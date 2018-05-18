package io.sloeber.core.listeners;
/*** Message from jan baeyens
 * this listener makes sure that when you change from one configuration to another
 * the correct hardware libraries are attached to the project
 * for instance you can have a project with 2 configurations
 * one for teensy
 * one for arduino uno
 *
 *
 * when you use the spi library the library is a completely different library
 * this code takes care that you use the correct library when switching configuration
 *
 */

import org.eclipse.cdt.core.settings.model.CProjectDescriptionEvent;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionListener;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;

import io.sloeber.core.InternalBoardDescriptor;
import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.Libraries;
@SuppressWarnings({"unused"})
public class ConfigurationChangeListener implements ICProjectDescriptionListener {

	@Override
	public void handleEvent(CProjectDescriptionEvent event) {
		if (event.getEventType() != CProjectDescriptionEvent.ABOUT_TO_APPLY) {
			return;
		}
		ICProjectDescription projDesc = event.getNewCProjectDescription();

		// only handle arduino nature projects
		try {

			if (!event.getProject().hasNature(Const.ARDUINO_NATURE_ID)) {
				if (event.getProject().hasNature("it.baeyens.arduinonature")) { //$NON-NLS-1$
					// this is the old nature so make necessary changes
					IProjectDescription desc = projDesc.getProject().getDescription();
					// replace the ino to cpp tool
					ICommand[] buildSpec = desc.getBuildSpec();
					for (ICommand curCommand : buildSpec) {
						if (curCommand.getBuilderName().equals("it.baeyens.arduino.core.inoToCpp")) { //$NON-NLS-1$
							curCommand.setBuilderName("io.sloeber.arduino.core.inoToCpp"); //$NON-NLS-1$
						}
					}
					desc.setBuildSpec(buildSpec);

					// set the correct natures
					Helpers.addTheNatures(desc);

					projDesc.getProject().setDescription(desc, null);
					// set the correct toolchain
					// Setting the toolchain by code seems to be a issue so I
					// report the issue and have the user set the toolchain
					// manually.
					class TheDialog implements Runnable {

						@Override
						public void run() {

							MessageDialog dialog = new MessageDialog(null, "Your action is needed.", null, //$NON-NLS-1$
									"Your project/sketch has partially been migrated from baeyens.it to sloeber.io.\nYou still need to change the toolchain!", //$NON-NLS-1$
									MessageDialog.QUESTION,
									new String[] { "Show me datailed instructions (opens browser)", //$NON-NLS-1$
											"I know the routine" }, //$NON-NLS-1$
									0);

							if (dialog.open() == 0) {
								org.eclipse.swt.program.Program.launch("https://baeyens.it/eclipse/toolchainFix.php"); //$NON-NLS-1$
							}
						}
					}
					TheDialog theDialog = new TheDialog();
					Display.getDefault().syncExec(theDialog);
				} else {
					return;
				}
			}
		} catch (Exception e) {
			// don't care don't update
			return;
		}

		// TODO check here how this behaves when changing a config and select
		// ok.
		// It seems to me this is rerunning the config change which is not
		// nessesary in this case
		// We have a arduino project so we are safe.
		ICProjectDescription oldprojDesc = event.getOldCProjectDescription();
		ICConfigurationDescription activeConf = projDesc.getActiveConfiguration();
		IProject activeProject = projDesc.getProject();

		InternalBoardDescriptor oldBoardDescriptor = (InternalBoardDescriptor) BoardDescriptor
				.makeBoardDescriptor(oldprojDesc.getActiveConfiguration());
		InternalBoardDescriptor newBoardDescriptor = (InternalBoardDescriptor) BoardDescriptor
				.makeBoardDescriptor(activeConf);

		if (oldBoardDescriptor.equals(newBoardDescriptor)) {
			if (event.getProject().getName().equals(oldBoardDescriptor.getProjectName())) {
				if(oldprojDesc.getActiveConfiguration().getName().equals(projDesc.getActiveConfiguration().getName())) {
				// only act when there is change
				return;
				}
			}
		}
		Helpers.setTheEnvironmentVariables(activeProject, activeConf, newBoardDescriptor);
		try {
			Helpers.addArduinoCodeToProject(newBoardDescriptor, activeProject, activeConf);
		} catch (Exception e) {
			Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, "failed to add include folder", e)); //$NON-NLS-1$
		}
		Libraries.reAttachLibrariesToProject(activeConf);
		projDesc.setActiveConfiguration(activeConf);
	}

}
