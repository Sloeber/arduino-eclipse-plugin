package io.sloeber.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.Sketch;
import io.sloeber.ui.Activator;
import io.sloeber.ui.Messages;
import io.sloeber.ui.helpers.MyPreferences;
import io.sloeber.ui.listeners.ProjectExplorerListener;

class UploadJobHandler extends Job {
    IProject myBuildProject = null;

    public UploadJobHandler(IProject buildProject) {
	super(Messages.ArduinoUploadProjectHandler_Upload_for_project + buildProject.getName());
	this.myBuildProject = buildProject;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
	if (MyPreferences.getBuildBeforeUploadOption()) {

	    boolean success = Sketch.verify(this.myBuildProject, monitor);
	    if (!success) {

		Display.getDefault().asyncExec(new Runnable() {
		    @Override
		    public void run() {
			Shell theShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			MessageBox dialog = new MessageBox(theShell, SWT.ICON_QUESTION | SWT.OK);
			dialog.setText(Messages.ArduinoUploadProjectHandler_Build_failed);
			dialog.setMessage(Messages.ArduinoUploadProjectHandler_Build_failed_so_no_upload);
			dialog.open();
		    }
		});
	    }
	}
	Sketch.upload(UploadJobHandler.this.myBuildProject);
	return Status.OK_STATUS;
    }
}

/**
 * This is a handler to connect the plugin.xml to the code for uploading code to
 * arduino teensy ..
 * 
 * @author jan
 * 
 */
public class UploadProjectHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
	if (!BoardsManager.isReady()) {
	    Activator.log(new Status(IStatus.ERROR, Activator.getId(), Messages.pleaseWaitForInstallerJob, null));
	    return null;
	}
	IProject SelectedProjects[] = ProjectExplorerListener.getSelectedProjects();
	switch (SelectedProjects.length) {
	case 0:
	    Activator.log(new Status(IStatus.ERROR, Activator.getId(),
		    Messages.Handler_No_project_found));
	    break;
	case 1:
	    IProject project = SelectedProjects[0];
	    uploadProject(project);
	    break;
	default:
	    Activator.log(new Status(IStatus.ERROR, Activator.getId(),
		    Messages.ArduinoUploadProjectHandler_Multiple_projects_found
			    + Integer.toString(SelectedProjects.length)
			    + Messages.ArduinoUploadProjectHandler_The_Names_Are + SelectedProjects.toString()));

	}
	return null;
    }

    public static void uploadProject(IProject project) {
	PlatformUI.getWorkbench().saveAllEditors(false);
	Job mBuildJob = new UploadJobHandler(project);
	mBuildJob.setPriority(Job.INTERACTIVE);
	mBuildJob.schedule();
    }
}
