package it.baeyens.arduino.ui;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * The ArduinoSettingsPage class is linked to page in the import wizard. It wraps around the ArduinPropertyPage
 * 
 * @author Jan Baeyens
 * 
 */
public class NewArduinoSketchWizardBoardPage extends WizardPage {

    final Shell shell = new Shell();

    protected ArduinoSelectionPage mPageLayout = new ArduinoSelectionPage();

    public void setListener(Listener BoardSelectionChangedListener) {
	mPageLayout.setListener(BoardSelectionChangedListener);
    }

    private Listener completeListener = new Listener() {
	@Override
	public void handleEvent(Event e) {
	    setPageComplete(mPageLayout.isPageComplete());
	}
    };

    public NewArduinoSketchWizardBoardPage(String pageName) {
	super(pageName);
	setPageComplete(false);
    }

    public NewArduinoSketchWizardBoardPage(String pageName, String title, ImageDescriptor titleImage) {
	super(pageName, title, titleImage);
	setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
	Composite composite = new Composite(parent, SWT.NULL);
	mPageLayout.draw(composite);
	setControl(composite);
	mPageLayout.mFeedbackControl.addListener(SWT.Modify, completeListener);
	setPageComplete(mPageLayout.isPageComplete());
    }

    public void saveAllSelections(ICConfigurationDescription confdesc) {
	mPageLayout.saveAllSelections(confdesc);
    }

    public IPath getPlatformFolder() {
	return mPageLayout.getPlatformFolder();
    }

    public String getPackage() {
	return mPageLayout.getPackage();
    }

    public String getArchitecture() {
	return mPageLayout.getArchitecture();
    }

    public String getBoardID() {
	return mPageLayout.getBoardID();
    }

}
