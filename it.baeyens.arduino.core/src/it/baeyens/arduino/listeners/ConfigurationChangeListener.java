package it.baeyens.arduino.listeners;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ArduinoHelpers;
import it.baeyens.arduino.tools.ArduinoLibraries;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.settings.model.CProjectDescriptionEvent;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionListener;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

public class ConfigurationChangeListener implements ICProjectDescriptionListener {

    @Override
    public void handleEvent(CProjectDescriptionEvent event) {
	// we are only interested in about to apply
	if (event.getEventType() != CProjectDescriptionEvent.ABOUT_TO_APPLY)
	    return;
	ICProjectDescription projDesc = event.getNewCProjectDescription();
	IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
	if (projDesc.getActiveConfiguration() != null) {
	    IEnvironmentVariable var = contribEnv.getVariable(ArduinoConst.ENV_KEY_JANTJE_PLATFORM_FILE, projDesc.getActiveConfiguration());
	    if (var != null) {
		IPath platformPath = new Path(var.getValue());
		ArduinoHelpers.setProjectPathVariables(projDesc.getProject(), platformPath.removeLastSegments(1));
		ArduinoHelpers.setTheEnvironmentVariables(projDesc.getProject(), projDesc.getActiveConfiguration(), false);
		try {
		    ArduinoHelpers.addArduinoCodeToProject(projDesc.getProject(), projDesc.getActiveConfiguration());
		} catch (CoreException e1) {
		    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Error adding the arduino code", e1));
		}
		ArduinoLibraries.reAttachLibrariesToProject(projDesc.getActiveConfiguration());
	    }
	}
    }

}
