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
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionListener;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.common.Common;
import io.sloeber.common.Const;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.Libraries;

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
		if (event.getProject().hasNature("it.baeyens.arduinonature")) {
		    // this is the old nature so make nessesary changes
		    IProjectDescription desc = projDesc.getProject().getDescription();
		    // set the correct natures
		    Helpers.addTheNatures(desc);
		    // TODO set the correct toolchain
		    // IToolChainModificationManager tcmmgr =
		    // ManagedBuildManager.getToolChainModificationManager();
		    // IFolderInfoModification tt1;
		    // org.eclipse.cdt.managedbuilder.core.IToolChain tc;
		    // tt1.setToolChain(tc);
		    // tcmmgr.IToolListModification ff;
		    // org.eclipse.cdt.managedbuilder.ui.properties.Page_ToolChain
		    // ff;
		    // set toolchain
		    // for( ICConfigurationDescription
		    // curconf:projDesc.getConfigurations()){
		    // curconf.getConfiguration().
		    // }
		    // projDesc.getProject().setDescription(desc, null);
		} else {
		    return;
		}
	    }
	} catch (Exception e) {
	    // don't care
	}

	// We have a arduino project so we are safe.

	if (projDesc.getActiveConfiguration() != null) {

	    Helpers.setTheEnvironmentVariables(projDesc.getProject(), projDesc.getActiveConfiguration(), false);
	    try {

		Helpers.addArduinoCodeToProject(projDesc.getProject(), projDesc.getActiveConfiguration());
	    } catch (Exception e) {
		Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, "failed to add include folder", e)); //$NON-NLS-1$
	    }
	    Libraries.reAttachLibrariesToProject(projDesc.getActiveConfiguration());
	}
    }

}
