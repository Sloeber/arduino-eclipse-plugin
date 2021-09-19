package io.sloeber.ui.actions;

import static io.sloeber.ui.Activator.*;

import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import io.sloeber.core.api.PackageManager;
import io.sloeber.core.api.SloeberProject;
import io.sloeber.ui.Messages;
import io.sloeber.ui.helpers.MyPreferences;
import io.sloeber.ui.listeners.ProjectExplorerListener;

class UploadJobHandler extends Job {
	IProject myBuildProject = null;
	boolean myIsProgram = false;

	public UploadJobHandler(IProject buildProject, boolean isProgram) {
		super(Messages.arduino_upload_projecthandler_upload_for_project.replace(Messages.PROJECT,
				buildProject.getName()));
		this.myBuildProject = buildProject;
		myIsProgram = isProgram;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		boolean canUpload = true;
		IStatus retStatus = Status.OK_STATUS;
		if (MyPreferences.getBuildBeforeUploadOption()) {
			try {
				myBuildProject.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);

				canUpload = isBuildSuccessFull(myBuildProject);
			} catch (Exception e) {
				return new Status(IStatus.WARNING, PLUGIN_ID, Messages.Build_Error_Before_Upload, e);

			}

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
		if (canUpload) {
			SloeberProject sProject = SloeberProject.getSloeberProject(UploadJobHandler.this.myBuildProject, true);
			if (sProject != null) {
				if (myIsProgram) {
					retStatus = sProject.upLoadUsingProgrammer();
				} else {
					retStatus = sProject.upload();
				}
			}
			if (retStatus.isOK()) {
				if (MyPreferences.getSwitchToSerialMonitorAfterUpload()) {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							try {
								PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
										.showView("io.sloeber.ui.monitor.views.SerialMonitor"); //$NON-NLS-1$
							} catch (PartInitException e) {
								e.printStackTrace();
							}
						}
					});
				}
			}
		}
		return retStatus;
	}

	/**
	 * Checks if build completed successfully.
	 *
	 * @return true iff project was built successfully last time.
	 * @throws CoreException if current project does not exist or is not open.
	 */
	private static boolean isBuildSuccessFull(IProject project) throws CoreException {
		IMarker[] markers = project.findMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
		for (IMarker marker : markers) {
			if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
				return false;
			}
		}
		return true;
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
			log(new Status(IStatus.ERROR, PLUGIN_ID, Messages.pleaseWaitForInstallerJob, null));
			return null;
		}
		IProject selectedProject = ProjectExplorerListener.getSelectedProject();
		if (selectedProject != null) {
			PlatformUI.getWorkbench().saveAllEditors(false);
			Job mBuildJob = new UploadJobHandler(selectedProject, false);
			mBuildJob.setPriority(Job.INTERACTIVE);
			mBuildJob.schedule();
		}
		return null;
	}

}
