package io.sloeber.ui.wizard;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import io.sloeber.core.api.SloeberProject;

public class ConvertToSloeber extends Wizard implements INewWizard {
	private IProject myProject = null;

	public ConvertToSloeber() {
		// TODO Auto-generated constructor stub
		System.out.println("ConvertToSloeber"); //$NON-NLS-1$
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// TODO Auto-generated method stub
		Object firstElement = selection.getFirstElement();
		myProject = (IProject) firstElement;
		System.out.println("init"); //$NON-NLS-1$

	}

	@Override
	public boolean performFinish() {
		SloeberProject.convertToArduinoProject(myProject, null);
		return true;
	}

}
