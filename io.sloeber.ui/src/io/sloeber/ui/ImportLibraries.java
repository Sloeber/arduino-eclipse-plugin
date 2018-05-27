package io.sloeber.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardContainer;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;

import io.sloeber.ui.listeners.ProjectExplorerListener;

/**
 * Import_Arduino_Libraries class is the class linked to the GUI related to the
 * Arduino library import. It creates one page. There is an important processing
 * when opened and on the perform finish. This import wizard uses 3 folders to
 * import: The arduino library folder Your 3rd party library folder and the
 * Arduino library folder of your hardware
 *
 * @author Jan Baeyens
 * @see performFinish
 *
 */
public class ImportLibraries implements IImportWizard {

	private Import_Libraries_Page mProjectSelectionPage;
	private IWizardPage[] mPages;
	private IWizardContainer mWizardContainer = null;

	private static String mPageName = Messages.ui_select;
	private static String mPageTitle = Messages.ui_select_Arduino_libraries;

	@Override
	public void init(IWorkbench arg0, IStructuredSelection selection) {
		// Entry point is here when right-click project and import -- as opposed
		// to AddLibraryAction.execute() when done via Arduino menu
	}

	@Override
	public void addPages() {

		// Always create the pages like this at the last minute

		IProject theProject = null;
		IProject SelectedProjects[] = ProjectExplorerListener.getSelectedProjects();

		if (SelectedProjects.length > 0) {
			theProject = SelectedProjects[0];
			this.mProjectSelectionPage = new Import_Libraries_Page(theProject, mPageName, StructuredSelection.EMPTY);
			this.mProjectSelectionPage.setWizard(this);
			this.mPages = new IWizardPage[1];
			this.mPages[0] = this.mProjectSelectionPage;
		} else {

			Activator.log(new Status(IStatus.ERROR, Activator.getId(), Messages.no_project_found));
		}
	}

	@Override
	public boolean canFinish() {
		return true;
	}

	@Override
	public void createPageControls(Composite pageContainer) {// no code needed

	}

	@Override
	public void dispose() {// no code needed
	}

	@Override
	public IWizardContainer getContainer() {
		return this.mWizardContainer;
	}

	@Override
	public Image getDefaultPageImage() {
		return null;
	}

	@Override
	public IDialogSettings getDialogSettings() {
		return null;
	}

	@Override
	public IWizardPage getNextPage(IWizardPage arg0) {
		for (int i = 0; i < (this.mPages.length - 1); i++) {
			if (arg0 == this.mPages[i]) {
				return this.mPages[i + 1];
			}
		}
		return null;
	}

	@Override
	public IWizardPage getPage(String pageName) {
		for (int i = 0; i < this.mPages.length; i++) {
			if (pageName == this.mPages[i].getName())
				return this.mPages[i];
		}
		return null;
	}

	@Override
	public int getPageCount() {
		return this.mPages.length;
	}

	@Override
	public IWizardPage[] getPages() {
		return this.mPages;
	}

	@Override
	public IWizardPage getPreviousPage(IWizardPage arg0) {
		for (int i = 1; i < this.mPages.length; i++) {
			if (arg0 == this.mPages[i])
				return this.mPages[i - 1];
		}
		return null;
	}

	@Override
	public IWizardPage getStartingPage() {
		return this.mPages[0];
	}

	@Override
	public RGB getTitleBarColor() {
		return null;
	}

	@Override
	public String getWindowTitle() {
		return mPageTitle;
	}

	@Override
	public boolean isHelpAvailable() {
		return false;
	}

	@Override
	public boolean needsPreviousAndNextButtons() {
		return false;
	}

	@Override
	public boolean needsProgressMonitor() {
		return false;
	}

	@Override
	public boolean performCancel() {
		return true;
	}

	@Override
	public boolean performFinish() {
		return this.mProjectSelectionPage.PerformFinish();
	}

	@Override
	public void setContainer(IWizardContainer wizardContainer) {
		this.mWizardContainer = wizardContainer;
	}

}
