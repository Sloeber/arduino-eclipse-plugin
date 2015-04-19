package it.baeyens.arduino.ui;

import org.eclipse.cdt.ui.newui.AbstractPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * ArduinoPropertyPage is a wrapper class for ArduinoSelectionPage. It wraps this class for the project properties
 * 
 * @author Jan Baeyens
 * 
 */
public class ArduinoPropertyPage extends AbstractPage {
    ArduinoSelectionPage page = null;

    @Override
    protected boolean isSingle() {
	return false;
    }

    @Override
    public void createControl(Composite parent) {
	// TODO Auto-generated method stub
	super.createControl(parent);
	page = (ArduinoSelectionPage) this.currentTab;
	page.mFeedbackControl.addListener(SWT.Modify, new Listener() {

	    @Override
	    public void handleEvent(Event event) {
		// TODO Auto-generated method stub
		setValid(page.isPageComplete());
	    }
	});
    }

}
