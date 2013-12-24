package it.baeyens.arduino.actions;

import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.uploaders.UploadSketchWrapper;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;

/**
 * This is a handler to connect the plugin.xml to the code for uploading code to arduino teensy ..
 * 
 * @author jan
 * 
 */
public class ArduinoUploadProjectHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
	if (!ArduinoInstancePreferences.isConfigured(true))
	    return null;
	IProject SelectedProjects[] = Common.getSelectedProjects();
	switch (SelectedProjects.length) {
	case 0:
	    Common.logError("No project found to upload");
	    break;
	case 1:
	    UploadSketchWrapper.upload(SelectedProjects[0], CoreModel.getDefault().getProjectDescription(SelectedProjects[0]).getActiveConfiguration()
		    .getName());
	    break;
	default:
	    Common.logError("Only 1 project should be seleted: found "
		    + Integer.toString(SelectedProjects.length)
		    + " the names are :" + SelectedProjects.toString());

	}
	return null;
    }

}
