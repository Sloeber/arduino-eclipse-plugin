package it.baeyens.arduino.listeners;

import org.eclipse.cdt.core.settings.model.CProjectDescriptionEvent;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionListener;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ArduinoHelpers;
import it.baeyens.arduino.tools.ArduinoLibraries;

public class ConfigurationChangeListener implements ICProjectDescriptionListener {

    @Override
    public void handleEvent(CProjectDescriptionEvent event) {
	if (event.getEventType() != CProjectDescriptionEvent.ABOUT_TO_APPLY) {
	    return;
	}

	// only handle arduino nature projects
	try {
	    if (!event.getProject().hasNature(ArduinoConst.ArduinoNatureID)) {
		return;
	    }
	} catch (Exception e) {
	    // don't care
	}

	// We have a arduino project so we are safe.
	ICProjectDescription projDesc = event.getNewCProjectDescription();
	if (projDesc.getActiveConfiguration() != null) {

	    ArduinoHelpers.setTheEnvironmentVariables(projDesc.getProject(), projDesc.getActiveConfiguration(), false);
	    try {
		ArduinoHelpers.addArduinoCodeToProject(projDesc.getProject(), projDesc.getActiveConfiguration());
	    } catch (Exception e) {
		Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "failed to add include folder", e)); //$NON-NLS-1$
	    }
	    ArduinoLibraries.reAttachLibrariesToProject(projDesc.getActiveConfiguration());
	}
    }

}
