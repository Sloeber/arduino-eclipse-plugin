package io.sloeber.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.PlatformUI;

import io.sloeber.core.api.Sketch;
import io.sloeber.ui.Activator;
import io.sloeber.ui.Messages;
import io.sloeber.ui.listeners.ProjectExplorerListener;

public class ReattachLibraries extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent arg0) throws ExecutionException {
		IProject SelectedProjects[] = ProjectExplorerListener.getSelectedProjects();
		switch (SelectedProjects.length) {
		case 0:
			Activator.log(new Status(IStatus.ERROR, Activator.getId(), Messages.no_project_found));
			break;
		default:
			PlatformUI.getWorkbench().saveAllEditors(false);
			for (int curProject = 0; curProject < SelectedProjects.length; curProject++) {
				Sketch.reAttachLibrariesToProject(SelectedProjects[curProject]);

			}
		}
		return null;

	}

}
