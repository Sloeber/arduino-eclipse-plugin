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

import io.sloeber.core.api.PackageManager;
import io.sloeber.core.api.Sketch;
import io.sloeber.ui.Activator;
import io.sloeber.ui.Messages;
import io.sloeber.ui.helpers.MyPreferences;
import io.sloeber.ui.listeners.ProjectExplorerListener;

class UploadJobHandler extends Job {
    IProject myBuildProject = null;

    public UploadJobHandler(IProject buildProject) {
	super(Messages.arduino_upload_projecthandler_upload_for_project.replace(Messages.PROJECT, buildProject.getName()));
	this.myBuildProject = buildProject;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
    boolean canUpload=true;
	if (MyPreferences.getBuildBeforeUploadOption()) {

	    canUpload = Sketch.verify(this.myBuildProject, monitor);
	    if (!canUpload) {

		Display.getDefault().syncExec(new Runnable() {
		    @Override
		    public void run() {
			Shell theShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			MessageBox dialog = new MessageBox(theShell, SWT.ICON_QUESTION | SWT.OK);
			dialog.setText(Messages.arduino_upload_project_handler_build_failed);
			dialog.setMessage(Messages.arduino_upload_project_handler_build_failed_so_no_upload);
			dialog.open();
		    }
		});
	    }
	}
	if(canUpload) {
		Sketch.upload(UploadJobHandler.this.myBuildProject);
	}
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
	if (!PackageManager.isReady()) {
	    Activator.log(new Status(IStatus.ERROR, Activator.getId(), Messages.pleaseWaitForInstallerJob, null));
	    return null;
	}
	IProject selectedProject = ProjectExplorerListener.getSelectedProject();
	if (selectedProject!=null) {
	    uploadProject(selectedProject);
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
