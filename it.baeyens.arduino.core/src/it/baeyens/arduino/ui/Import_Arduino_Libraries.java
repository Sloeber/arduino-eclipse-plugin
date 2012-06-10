package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.Common;
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

/**
 * Import_Arduino_Libraries class is the linked to the gui related to the
 * arduino library import It creates one page. There is an important processing
 * when opened and on the perform finish This import wizard uses 2 folders to
 * import. The arduino library folder Your 3rd party library folder
 * 
 * @author Jan Baeyens
 * @see performFinish
 * 
 */
public class Import_Arduino_Libraries implements IImportWizard
	{
		private Import_Arduino_Libraries_Page mFolderSelectionPage;
		private Wizard_Select_Project_Page mProjectSelectionPage;
		private IWizardPage[] mPages;
		private IWizardContainer mWizardContainer = null;
		private static String mPageName = "Select";
		private static String mPageTitle = "Select the arduino libraries";

		@Override
		public void init(IWorkbench arg0, IStructuredSelection selection)
			{
				mFolderSelectionPage = new Import_Arduino_Libraries_Page(mPageName, StructuredSelection.EMPTY);
				mFolderSelectionPage.setWizard(this);
				mProjectSelectionPage = new Wizard_Select_Project_Page("Select the project to import to", StructuredSelection.EMPTY);
				mProjectSelectionPage.setWizard(this);
				
				mPages = new IWizardPage[2];
				mPages[0] = mFolderSelectionPage;
				mPages[1] = mProjectSelectionPage;
				mProjectSelectionPage.setProject(Common.getProject(selection));
			}

		@Override
		public void addPages()
			{

			}

		@Override
		public boolean canFinish()
			{
				return mFolderSelectionPage.canFinish() & mProjectSelectionPage.canFinish();
			}

		@Override
		public void createPageControls(Composite pageContainer)
			{

			}

		@Override
		public void dispose()
			{

			}

		@Override
		public IWizardContainer getContainer()
			{
				return mWizardContainer;
			}

		@Override
		public Image getDefaultPageImage()
			{
				return null;
			}

		@Override
		public IDialogSettings getDialogSettings()
			{
				return null;
			}

		@Override
		public IWizardPage getNextPage(IWizardPage arg0)
			{
				for (int i=0 ; i < ( mPages.length -1 ) ; i++)
					{
						if (arg0 ==mPages[i] )return mPages[i+1];
					}
				return null;
			}

		@Override
		public IWizardPage getPage(String pageName)
			{
				for (int i=0 ; i < mPages.length; i++)
					{
						if (pageName ==mPages[i].getName() )return mPages[i];
					}
				return null;
			}

		@Override
		public int getPageCount()
			{
				return mPages.length;
			}

		@Override
		public IWizardPage[] getPages()
			{
				return mPages;
			}

		@Override
		public IWizardPage getPreviousPage(IWizardPage arg0)
			{
				for (int i=1 ; i < mPages.length; i++)
					{
						if (arg0 ==mPages[i] )return mPages[i-1];
					}
				return null;
			}

		@Override
		public IWizardPage getStartingPage()
			{
				return mPages[0];
			}

		@Override
		public RGB getTitleBarColor()
			{
				return null;
			}

		@Override
		public String getWindowTitle()
			{
				return mPageTitle;
			}

		@Override
		public boolean isHelpAvailable()
			{
				return false;
			}

		@Override
		public boolean needsPreviousAndNextButtons()
			{
				return true;
			}

		@Override
		public boolean needsProgressMonitor()
			{
				return false;
			}

		@Override
		public boolean performCancel()
			{
				return true;
			}

		@Override
		public boolean performFinish()
			{
				return mFolderSelectionPage.PerformFinish(mProjectSelectionPage.GetProject());
			}

		@Override
		public void setContainer(IWizardContainer wizardContainer)
			{
				mWizardContainer = wizardContainer;
			}

	}
