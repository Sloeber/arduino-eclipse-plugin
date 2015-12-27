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

    protected ArduinoBoardSelectionPage mPageLayout = new ArduinoBoardSelectionPage();

    public void setListener(Listener BoardSelectionChangedListener) {
	this.mPageLayout.setListener(BoardSelectionChangedListener);
    }

    private Listener completeListener = new Listener() {
	@Override
	public void handleEvent(Event e) {
	    setPageComplete(NewArduinoSketchWizardBoardPage.this.mPageLayout.isPageComplete());
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
	this.mPageLayout.draw(composite);
	setControl(composite);
	this.mPageLayout.mFeedbackControl.addListener(SWT.Modify, this.completeListener);
	setPageComplete(this.mPageLayout.isPageComplete());
    }

    public void saveAllSelections(ICConfigurationDescription confdesc) {
	this.mPageLayout.saveAllSelections(confdesc);
    }

    public IPath getPlatformFolder() {
	return this.mPageLayout.getPlatformFolder();
    }

    public String getPackage() {
	return this.mPageLayout.getPackage();
    }

    public String getArchitecture() {
	return this.mPageLayout.getArchitecture();
    }

    public String getBoardID() {
	return this.mPageLayout.getBoardID();
    }

}
