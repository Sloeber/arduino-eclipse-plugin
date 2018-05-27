package io.sloeber.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.PlatformUI;

import io.sloeber.core.api.Sketch;
import io.sloeber.ui.Activator;
import io.sloeber.ui.Messages;
import io.sloeber.ui.listeners.ProjectExplorerListener;

/**
 * This id a handler to connect the plugin.xml to the code for building the code
 * This method forces a save all before building
 *
 * @author jan
 *
 */
class BuildJobHandler extends Job {
	IProject myBuildProject = null;

	public BuildJobHandler(String name) {
		super(name);
	}

	public BuildJobHandler(IProject buildProject) {
		super(Messages.buildHandler_build_code_of_project.replace(Messages.PROJECT, buildProject.getName()));
		this.myBuildProject = buildProject;
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		Sketch.verify(this.myBuildProject, monitor);
		return Status.OK_STATUS;
	}
}

public class BuildHandler extends AbstractHandler {
	private Job mBuildJob = null;

	public Job getJob() {
		return this.mBuildJob;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IProject SelectedProjects[] = ProjectExplorerListener.getSelectedProjects();
		switch (SelectedProjects.length) {
		case 0:
			Activator.log(new Status(IStatus.ERROR, Activator.getId(), Messages.no_project_found));
			break;
		default:
			PlatformUI.getWorkbench().saveAllEditors(false);
			for (int curProject = 0; curProject < SelectedProjects.length; curProject++) {
				this.mBuildJob = new BuildJobHandler(SelectedProjects[curProject]);
				this.mBuildJob.setPriority(Job.INTERACTIVE);
				this.mBuildJob.schedule();
			}
		}
		return null;
	}

}
