package it.baeyens.arduino.listeners;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ArduinoHelpers;
import it.baeyens.arduino.tools.ArduinoLibraries;

import org.eclipse.cdt.core.settings.model.CProjectDescriptionEvent;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class ConfigurationChangeListener implements ICProjectDescriptionListener {

    @Override
    public void handleEvent(CProjectDescriptionEvent event) {
	// we are only interested in about to apply
	// if (event.getEventType() != CProjectDescriptionEvent.ABOUT_TO_APPLY)
	// return;
	// if (true)
	// return;
	ICProjectDescription projDesc = event.getNewCProjectDescription();

	if (projDesc.getActiveConfiguration() != null) {

	    ArduinoHelpers.setTheEnvironmentVariables(projDesc.getProject(), projDesc.getActiveConfiguration(), false);
	    ArduinoHelpers.setProjectPathVariables(projDesc.getActiveConfiguration());
	    // try {
	    try {
		ArduinoHelpers.addArduinoCodeToProject(projDesc.getProject(), projDesc.getActiveConfiguration());
	    } catch (Exception e) {
		Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "failed to add include folder", e));
	    }
	    // } catch (CoreException e1) {
	    // Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Error adding the arduino code", e1));
	    // }
	    ArduinoLibraries.reAttachLibrariesToProject(projDesc.getActiveConfiguration());
	}
    }

}
