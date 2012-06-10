package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ArduinoHelpers;

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
/**Import_Arduino_Library class is llinked to the gui related to the arduino library import
 * It creates one page. All important action is in the performFinish
 * 
 * @author Jan Baeyens
 * @see performFinish
 *
 */
public class Import_Source_Folder implements IImportWizard {

	private Import_Source_Folder_Page mFolderSelectionPage;
	private IWizardPage[] mPages;
	private IWizardContainer mWizardContainer=null;
	private static String mPageName ="Select";
	private static String mPageTitle = "Select the folder containing the arduino library";
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {
		mFolderSelectionPage = new Import_Source_Folder_Page(mPageName,StructuredSelection.EMPTY);
		mFolderSelectionPage.setWizard(this);
		mPages= new IWizardPage[1];
		mPages[0]=mFolderSelectionPage;
		mFolderSelectionPage.setImportProject(Common.getProject(selection));
	}

	@Override
	public void addPages() {
	}

	@Override
	public boolean canFinish() {
		return mFolderSelectionPage.canFinish();
	}

	@Override
	public void createPageControls(Composite pageContainer) {
	}

	@Override
	public void dispose() {
	}

	@Override
	public IWizardContainer getContainer() {
		return mWizardContainer;
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
		if (mFolderSelectionPage.getName().equals(pageName)) return mFolderSelectionPage;
		return null;
	}

	@Override
	public int getPageCount() {
		return mPages.length;
	}

	@Override
	public IWizardPage[] getPages() {
		return mPages;
	}

	@Override
	public IWizardPage getPreviousPage(IWizardPage page) {
		return null;
	}

	@Override
	public IWizardPage getStartingPage() {
		return mPages[0];
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

	/**performFinish creates the library and set the environment so that it can be compiled.
	 * 
	 * @author Jan Baeyens
	 */
	@Override
	public boolean performFinish() {
		try {
			ArduinoHelpers.addCodeFolder( mFolderSelectionPage.GetProject(), new Path(mFolderSelectionPage.GetLibraryFolder()));
		} catch (CoreException e) {
			e.printStackTrace();
			IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,	"Failed to import library " , e);
			Common.log(status);
			return false;
		}
		return true;
	}

	@Override
	public void setContainer(IWizardContainer wizardContainer) {
		mWizardContainer = wizardContainer;
	}

}

