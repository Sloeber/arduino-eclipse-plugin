package it.baeyens.arduino.ui;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

/**
 * The SketchTemplatePage class is linked to page in the import wizard. It wraps around the SketchSelectionPage
 * 
 * @author Nico Verduin
 * 
 */
public class SketchTemplatePage extends WizardPage {

    final Shell shell = new Shell();

    protected SketchSelectionPage mPageLayout = new SketchSelectionPage();

    private Listener completeListener = new Listener() {
	@Override
	public void handleEvent(Event e) {
	    setPageComplete(mPageLayout.isPageComplete());
	}
    };

    public SketchTemplatePage(String pageName) {
	super(pageName);
	setPageComplete(false);
    }

    public SketchTemplatePage(String pageName, String title, ImageDescriptor titleImage) {
	super(pageName, title, titleImage);
	setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
	Composite composite = new Composite(parent, SWT.NULL);
	mPageLayout.draw(composite);
	setControl(composite);
	mPageLayout.feedbackControl.addListener(SWT.Modify, completeListener);
	setPageComplete(mPageLayout.isPageComplete());
    }

    public void saveAllSelections(ICConfigurationDescription confdesc) {
	mPageLayout.saveAllSelections(confdesc);
    }

}
