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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Activator;
import io.sloeber.core.api.BoardDescription;
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
		
		IProject activeProject = event.getProject();
		// only handle arduino nature projects
		try {
			if (!activeProject.hasNature(Const.ARDUINO_NATURE_ID)) {
					return;
			}
		} catch (Exception e) {
			// don't care don't update
			return;
		}
		if(IndexerController.isPosponed(activeProject)) {
            Common.log(new Status(Const.SLOEBER_STATUS_DEBUG, Activator.getId(),
                    "Ignoring configuration change during project creation " + activeProject.getName())); //$NON-NLS-1$
		    return;
		}
		ICProjectDescription projDesc = event.getNewCProjectDescription();
        ICConfigurationDescription activeConf = projDesc.getActiveConfiguration();
		ICProjectDescription oldprojDesc = event.getOldCProjectDescription();
        ICConfigurationDescription oldActiveConf = oldprojDesc.getActiveConfiguration();
        String boardsFile = BoardDescription.getBoardsFile(activeConf);
        String oldBoardsFile = BoardDescription.getBoardsFile(oldActiveConf);
        String variant = BoardDescription.getVariant(activeConf);
        String oldVariant = BoardDescription.getVariant(oldActiveConf);
        // only if the boardsFile or variant Changed we need to reattach libraries and
        // cores
        if (boardsFile.equals(oldBoardsFile) && variant.equals(oldVariant)) {
            return;
        }
        BoardDescription newBoardDescriptor = new BoardDescription(activeConf);
        try {
            Helpers.setDirtyFlag(activeProject, activeConf);
            Helpers.addArduinoCodeToProject(newBoardDescriptor, activeConf);
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (!boardsFile.equals(oldBoardsFile)) {
            Libraries.reAttachLibrariesToProject(activeConf);
        }
		
	}

}
