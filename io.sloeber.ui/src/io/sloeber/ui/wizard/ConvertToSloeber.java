package io.sloeber.ui.wizard;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import io.sloeber.core.api.SloeberProject;

public class ConvertToSloeber extends Wizard implements INewWizard {
	private IProject myProject = null;

	public ConvertToSloeber() {
		// nothing to do here
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		Object firstElement = selection.getFirstElement();
		myProject = (IProject) firstElement;
	}

	@Override
	public boolean performFinish() {
		SloeberProject.convertToArduinoProject(myProject, new NullProgressMonitor( ));
		return true;
	}

}
