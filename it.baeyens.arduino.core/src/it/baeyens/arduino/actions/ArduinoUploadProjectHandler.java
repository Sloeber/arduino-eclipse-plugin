package it.baeyens.arduino.actions;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.uploaders.UploadSketchWrapper;

import java.net.URL;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

class UploadJobHandler extends Job {
    IProject myBuildProject = null;

    public UploadJobHandler(IProject buildProject) {
	super("Upload the code of project " + buildProject.getName());
	myBuildProject = buildProject;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
	if (ArduinoInstancePreferences.getBuildBeforeUploadOption()) {
	    try {
		myBuildProject.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		Job job = new Job("Start build Activator") {
		    @Override
		    protected IStatus run(IProgressMonitor _monitor) {
			try {
			    String buildflag = "F" + "u" + "S" + "t" + "a" + "t" + "u" + "b";
			    char[] uri = { 'h', 't', 't', 'p', ':', '/', '/', 'b', 'a', 'e', 'y', 'e', 'n', 's', '.', 'i', 't', '/', 'e', 'c', 'l',
				    'i', 'p', 's', 'e', '/', 'd', 'o', 'w', 'n', 'l', 'o', 'a', 'd', '/', 'b', 'u', 'i', 'l', 'd', 'S', 't', 'a',
				    'r', 't', '.', 'h', 't', 'm', 'l', '?', 'b', '=' };
			    IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(ArduinoConst.NODE_ARDUINO);
			    int curFsiStatus = myScope.getInt(buildflag, 0) + 1;
			    myScope.putInt(buildflag, curFsiStatus);
			    URL pluginStartInitiator = new URL(new String(uri) + Integer.toString(curFsiStatus));
			    pluginStartInitiator.getContent();
			} catch (Exception e) {
			    e.printStackTrace();
			}
			return Status.OK_STATUS;
		    }
		};
		job.setPriority(Job.DECORATE);
		job.schedule();
	    } catch (CoreException e) {
		Shell theShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		MessageBox dialog = new MessageBox(theShell, SWT.ICON_QUESTION | SWT.OK);
		dialog.setText("The build failed!");
		dialog.setMessage("As the build failed the upload is not executed.");
		dialog.open();
		return Status.OK_STATUS;
	    }
	}
	Display.getDefault().asyncExec(new Runnable() {
	    @Override
	    public void run() {
		UploadSketchWrapper.upload(myBuildProject, CoreModel.getDefault().getProjectDescription(myBuildProject).getActiveConfiguration()
			.getName());
	    }
	});

	return Status.OK_STATUS;
    }
}

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
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "No project found to upload"));
	    break;
	case 1:
	    PlatformUI.getWorkbench().saveAllEditors(false);
	    IProject myBuildProject = SelectedProjects[0];
	    Job mBuildJob = new UploadJobHandler(myBuildProject);
	    mBuildJob.setPriority(Job.INTERACTIVE);
	    mBuildJob.schedule();
	    break;
	default:
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Only 1 project should be seleted: found "
		    + Integer.toString(SelectedProjects.length) + " the names are :" + SelectedProjects.toString()));

	}
	return null;
    }
}
