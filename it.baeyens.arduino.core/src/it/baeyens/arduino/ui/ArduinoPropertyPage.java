package it.baeyens.arduino.ui;

import org.eclipse.cdt.ui.newui.AbstractPage;

/**
 * ArduinoPropertyPage is a wrapper class for ArduinoSelectionPage. It wraps this class for the project properties
 * 
 * @author Jan Baeyens
 * 
 */
public class ArduinoPropertyPage extends AbstractPage {

    @Override
    protected boolean isSingle() {
	return false;
    }

}
