package io.sloeber.core.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.PlatformUI;

import io.sloeber.common.Common;
import io.sloeber.common.Const;
import io.sloeber.core.listeners.ProjectExplorerListener;
import io.sloeber.core.tools.Libraries;

public class ReattachLibraries extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent arg0) throws ExecutionException {
	IProject SelectedProjects[] = ProjectExplorerListener.getSelectedProjects();
	switch (SelectedProjects.length) {
	case 0:
	    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, Messages.ReattachLibraries_no_project_found));
	    break;
	default:
	    PlatformUI.getWorkbench().saveAllEditors(false);
	    for (int curProject = 0; curProject < SelectedProjects.length; curProject++) {
		Libraries.reAttachLibrariesToProject(SelectedProjects[curProject]);
	    }
	}
	return null;

    }

}
