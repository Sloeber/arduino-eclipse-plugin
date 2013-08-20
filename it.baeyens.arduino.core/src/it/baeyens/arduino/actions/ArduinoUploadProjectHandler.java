package it.baeyens.arduino.actions;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.UploadArduinoSketch;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * This is a handler to connect the plugin.xml to the code for uploading code to
 * arduino teensy ..
 * 
 * @author jan
 * 
 */
public class ArduinoUploadProjectHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IProject SelectedProjects[] = Common.getSelectedProjects();
		switch (SelectedProjects.length) {
		case 0:
			Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "No project found to upload"));
			break;
		case 1:
			UploadArduinoSketch.Do(SelectedProjects[0], CoreModel.getDefault().getProjectDescription(SelectedProjects[0]).getActiveConfiguration()
					.getName());
			break;
		default:
			Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Only 1 project should be seleted: found "
					+ Integer.toString(SelectedProjects.length) + " the names are :" + SelectedProjects.toString()));

		}
		return null;
	}

}
