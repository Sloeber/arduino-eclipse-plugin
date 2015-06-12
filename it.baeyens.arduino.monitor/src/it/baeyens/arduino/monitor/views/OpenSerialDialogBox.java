package it.baeyens.arduino.monitor.views;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class OpenSerialDialogBox extends Dialog {
    @Override
    protected void okPressed() {
	// I need to save these values in local variables as the GUI stuff is
	// deleted after he close
	SelectedRate = Integer.parseInt(BaudRates.getCombo().getText());
	SelectedPort = SerialPorts.getCombo().getText();
	ArduinoInstancePreferences.setGlobalValue(ArduinoConst.KEY_SERIAlRATE, BaudRates.getCombo().getText());
	ArduinoInstancePreferences.setGlobalValue(ArduinoConst.KEY_SERIAlPORT, SelectedPort);
	super.okPressed();
    }

    private ComboViewer SerialPorts;
    private ComboViewer BaudRates;
    // private ComboViewer LineEndings;
    private String SelectedPort;
    private int SelectedRate;

    protected OpenSerialDialogBox(Shell parentShell) {
	super(parentShell);

    }

    @Override
    protected Control createDialogArea(Composite parent) {
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	// layout.horizontalAlignment = GridData.FILL;
	parent.setLayout(layout);

	// The text fields will grow with the size of the dialog
	GridData gridData = new GridData();
	gridData.grabExcessHorizontalSpace = true;
	gridData.horizontalAlignment = GridData.FILL;

	// Create the serial port combo box to allow to select a serial port
	Label label1 = new Label(parent, SWT.NONE);
	label1.setText("Serial port to connect to:");

	// If there are no comports allow to provide one
	String[] comPorts = Common.listComPorts();
	if (comPorts.length == 0) {
	    SerialPorts = new ComboViewer(parent, SWT.DROP_DOWN);
	} else {
	    SerialPorts = new ComboViewer(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
	}
	SerialPorts.getControl().setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false));
	SerialPorts.setContentProvider(new ArrayContentProvider());
	SerialPorts.setLabelProvider(new LabelProvider());
	SerialPorts.setInput(comPorts);

	// Create baud rate selection combo box to select the baud rate
	Label label2 = new Label(parent, SWT.NONE);
	label2.setText("Select the baudrate:");
	BaudRates = new ComboViewer(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
	BaudRates.getControl().setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false));
	BaudRates.setContentProvider(new ArrayContentProvider());
	BaudRates.setLabelProvider(new LabelProvider());
	BaudRates.setInput(Common.listBaudRates());

	BaudRates.getCombo().setText(ArduinoInstancePreferences.getGlobalString(ArduinoConst.KEY_SERIAlRATE, ""));
	SerialPorts.getCombo().setText(ArduinoInstancePreferences.getGlobalString(ArduinoConst.KEY_SERIAlPORT, ""));
	return parent;

    }

    public String GetComPort() {
	return SelectedPort;
    }

    public int GetBaudRate() {
	return SelectedRate;
    }

}
