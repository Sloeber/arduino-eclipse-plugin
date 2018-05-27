package io.sloeber.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
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

import io.sloeber.core.api.Sketch;
import io.sloeber.ui.listeners.ProjectExplorerListener;

/**
 * Import_Arduino_Library class is linked to the GUI related to the Arduino
 * source folder import. It creates one page. All important action is in the
 * performFinish
 *
 * @author Jan Baeyens
 * @see performFinish
 *
 */
public class Import_Source_Folder implements IImportWizard {

	private Import_Source_Folder_Page mFolderSelectionPage;
	private IWizardPage[] mPages;
	private IWizardContainer mWizardContainer = null;
	private static String mPageName = Messages.ui_select;
	private static String mPageTitle = Messages.ui_select_folder;

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		// Entry point is here when right-click project and import -- as opposed
		// to AddSourceFolderAction.execute() when done via Arduino menu
	}

	@Override
	public void addPages() {

		// Always create the pages like this at the last minute

		IProject theProject = null;
		IProject SelectedProjects[] = ProjectExplorerListener.getSelectedProjects();

		if (SelectedProjects.length > 0) {
			theProject = SelectedProjects[0];
			this.mFolderSelectionPage = new Import_Source_Folder_Page(theProject, mPageName, StructuredSelection.EMPTY);
			this.mFolderSelectionPage.setWizard(this);
			this.mPages = new IWizardPage[1];
			this.mPages[0] = this.mFolderSelectionPage;
			this.mFolderSelectionPage.setImportProject(SelectedProjects[0]);
		} else {
			Activator.log(new Status(IStatus.ERROR, Activator.getId(), Messages.no_project_found));
		}
	}

	@Override
	public boolean canFinish() {
		return this.mFolderSelectionPage.canFinish();
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
	public IWizardPage getNextPage(IWizardPage page) {
		return null;
	}

	@Override
	public IWizardPage getPage(String pageName) {
		if (this.mFolderSelectionPage.getName().equals(pageName))
			return this.mFolderSelectionPage;
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
	public IWizardPage getPreviousPage(IWizardPage page) {
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

	/**
	 * performFinish creates the library and set the environment so that it can
	 * be compiled.
	 *
	 * @author Jan Baeyens
	 */
	@Override
	public boolean performFinish() {
		IProject project = this.mFolderSelectionPage.getProject();
		try {
			return Sketch.addCodeFolder(project, new Path(this.mFolderSelectionPage.GetLibraryFolder()));
		} catch (CoreException e) {
			e.printStackTrace();
			IStatus status = new Status(IStatus.ERROR, Activator.getId(),
					Messages.error_failed_to_import_library_in_project, e);
			Activator.log(status);

		}
		return false;
	}

	@Override
	public void setContainer(IWizardContainer wizardContainer) {
		this.mWizardContainer = wizardContainer;
	}

}
