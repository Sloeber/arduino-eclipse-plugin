package it.baeyens.arduino.listeners;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

public class ProjectExplorerListener implements ISelectionListener {
    static IProject projects[] = new IProject[0];

    // public static IProject getActiveProject() {
    // return project;
    // }

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
	if (newSelection instanceof IStructuredSelection) {

	    List<IProject> allSelectedprojects = new ArrayList<>();
	    for (Object element : ((IStructuredSelection) newSelection).toList()) {
		if (element instanceof IAdaptable) {
		    @SuppressWarnings("cast") // this is needed for the oracle
					      // sdk
		    IResource resource = (IResource) ((IAdaptable) element).getAdapter(IResource.class);
		    if (resource != null) {
			allSelectedprojects.add(resource.getProject());
		    }

		}
	    }
	    projects = new IProject[allSelectedprojects.size()];
	    allSelectedprojects.toArray(projects);
	    return;
	}

    }

}