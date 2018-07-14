package io.sloeber.ui.listeners;
/**
 * this listener listens for changes in the project explorer
 * This is so we can know which project(s) you are currently working
 * on
 */

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import io.sloeber.ui.Activator;
import io.sloeber.ui.Messages;

public class ProjectExplorerListener implements ISelectionListener {
	static IProject projects[] = new IProject[0];

	/**
	 * give the selected project if there is only 1 project selected
	 * in all other cases show a error
	 * @return the selected project if there is only 1 else null
	 */
	public static IProject getSelectedProject() {
	    switch (projects.length) {
	    case 0:
	        Activator.log(new Status(IStatus.ERROR, Activator.getId(),
	            Messages.no_project_found));
	        break;
	    case 1:
	        return projects[0];
	    default:
	        Activator.log(new Status(IStatus.ERROR, Activator.getId(),
	            Messages.arduino_upload_project_handler_multiple_projects_found.
	            replace(Messages.NUMBER, Integer.toString(projects.length) ).
	            replace(Messages.PROJECT_LIST,  projects.toString()) ));

	    }
	    return null;}

	public static IProject[] getSelectedProjects() {
		return projects;
	}

	public static void registerListener() {
		IWorkbench wb = PlatformUI.getWorkbench();
		IWorkbenchWindow awbw = wb.getActiveWorkbenchWindow();
		ISelectionService ss = awbw.getSelectionService();

		ProjectExplorerListener selectionListener = new ProjectExplorerListener();
		ss.addPostSelectionListener(IPageLayout.ID_PROJECT_EXPLORER, selectionListener);

	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection newSelection) {
		if (!newSelection.isEmpty()) {
			if (newSelection instanceof IStructuredSelection) {

				List<IProject> allSelectedprojects = new ArrayList<>();
				for (Object element : ((IStructuredSelection) newSelection).toList()) {
					if (element instanceof IAdaptable) {
						@SuppressWarnings("cast") // this is needed for the
													// oracle
						// sdk as it needs the cast and
						// otherwise I have a warning
						IResource resource = (IResource) ((IAdaptable) element).getAdapter(IResource.class);
						if (resource != null) {
							allSelectedprojects.add(resource.getProject());
						}

					}
				}
				if (allSelectedprojects.size() > 0) {
					projects = new IProject[allSelectedprojects.size()];
					allSelectedprojects.toArray(projects);
				}
				return;
			}
		}
	}

}