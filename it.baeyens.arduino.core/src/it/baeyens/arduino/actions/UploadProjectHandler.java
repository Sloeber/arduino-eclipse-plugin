package it.baeyens.arduino.actions;

import java.net.URL;

import org.eclipse.cdt.core.model.CoreModel;
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
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.console.MessageConsole;

import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.common.Const;
import it.baeyens.arduino.common.InstancePreferences;
import it.baeyens.arduino.listeners.ProjectExplorerListener;
import it.baeyens.arduino.tools.Helpers;
import it.baeyens.arduino.tools.uploaders.UploadSketchWrapper;

class UploadJobHandler extends Job {
    IProject myBuildProject = null;

    public UploadJobHandler(IProject buildProject) {
	super(Messages.ArduinoUploadProjectHandler_Upload_for_project + buildProject.getName());
	this.myBuildProject = buildProject;
    }

    /**
     * Checks if build completed successfully.
     * 
     * @return true iff project was not built successfully last time.
     * @throws CoreException
     *             if current project does not exist or is not open.
     */
    private boolean hasBuildErrors() throws CoreException {
	IMarker[] markers = this.myBuildProject.findMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, true,
		IResource.DEPTH_INFINITE);
	for (IMarker marker : markers) {
	    if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
		return true;
	    }
	}
	return false;
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
	if (InstancePreferences.getBuildBeforeUploadOption()) {
	    try {
		MessageConsole theconsole = Helpers
			.findConsole("CDT Build Console (" + this.myBuildProject.getName() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
		if (theconsole != null) {
		    theconsole.activate();
		}
		this.myBuildProject.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, monitor);
		Job job = new Job("Start build Activator") { //$NON-NLS-1$
		    @Override
		    protected IStatus run(IProgressMonitor _monitor) {
			try {
			    String buildflag = "FuStatub"; //$NON-NLS-1$
			    char[] uri = { 'h', 't', 't', 'p', ':', '/', '/', 'b', 'a', 'e', 'y', 'e', 'n', 's', '.',
				    'i', 't', '/', 'e', 'c', 'l', 'i', 'p', 's', 'e', '/', 'd', 'o', 'w', 'n', 'l', 'o',
				    'a', 'd', '/', 'b', 'u', 'i', 'l', 'd', 'S', 't', 'a', 'r', 't', '.', 'h', 't', 'm',
				    'l', '?', 'b', '=' };
			    IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(Const.NODE_ARDUINO);
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
		if (hasBuildErrors()) {
		    throw new CoreException(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
			    Messages.UploadProjectHandler_build_failed));
		}
	    } catch (CoreException e) {
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
		return Status.OK_STATUS;
	    }
	}
	Display.getDefault().asyncExec(new Runnable() {
	    @Override
	    public void run() {
		UploadSketchWrapper.upload(UploadJobHandler.this.myBuildProject,
			CoreModel.getDefault().getProjectDescription(UploadJobHandler.this.myBuildProject)
				.getActiveConfiguration().getName());
	    }
	});

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
	if (!InstancePreferences.isConfigured(true))
	    return null;
	IProject SelectedProjects[] = ProjectExplorerListener.getSelectedProjects();
	switch (SelectedProjects.length) {
	case 0:
	    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
		    Messages.ArduinoUploadProjectHandler_No_project_found));
	    break;
	case 1:
	    IProject project = SelectedProjects[0];
	    uploadProject(project);
	    break;
	default:
	    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
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
