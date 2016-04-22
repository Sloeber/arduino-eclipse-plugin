package it.baeyens.arduino.monitor.views;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import it.baeyens.arduino.common.Const;
import it.baeyens.arduino.common.InstancePreferences;
import it.baeyens.arduino.common.Common;

public class OpenSerialDialogBox extends Dialog {
    private ComboViewer serialPorts;
    private ComboViewer baudRates;
    private Button dtrCheckbox;
    private String selectedPort;
    private int selectedRate;
    private boolean selectedDtr;

    protected OpenSerialDialogBox(Shell parentShell) {
	super(parentShell);
    }

    @Override
    protected void okPressed() {
	// I need to save these values in local variables as the GUI stuff is
	// deleted after he close
	this.selectedRate = Integer.parseInt(this.baudRates.getCombo().getText());
	this.selectedPort = this.serialPorts.getCombo().getText();
	this.selectedDtr = this.dtrCheckbox.getSelection();
	InstancePreferences.setGlobalValue(Const.KEY_SERIAL_RATE, this.baudRates.getCombo().getText());
	InstancePreferences.setGlobalValue(Const.KEY_SERIAL_PORT, this.selectedPort);
	super.okPressed();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	parent.setLayout(layout);

	// The text fields will grow with the size of the dialog
	GridData gridData = new GridData();
	gridData.grabExcessHorizontalSpace = true;
	gridData.horizontalAlignment = GridData.FILL;

	// Create the serial port combo box to allow to select a serial port
	Label label1 = new Label(parent, SWT.NONE);
	label1.setText(Messages.openSerialDialogBoxSerialPortToConnectTo);

	// If there are no comports allow to provide one
	String[] comPorts = Common.listComPorts();
	if (comPorts.length == 0) {
	    this.serialPorts = new ComboViewer(parent, SWT.DROP_DOWN);
	} else {
	    this.serialPorts = new ComboViewer(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
	}
	this.serialPorts.getControl().setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false));
	this.serialPorts.setContentProvider(new ArrayContentProvider());
	this.serialPorts.setLabelProvider(new LabelProvider());
	this.serialPorts.setInput(comPorts);

	// Create baud rate selection combo box to select the baud rate
	Label label2 = new Label(parent, SWT.NONE);
	label2.setText(Messages.openSerialDialogBoxSelectTheBautRate);
	this.baudRates = new ComboViewer(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
	this.baudRates.getControl().setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false));
	this.baudRates.setContentProvider(new ArrayContentProvider());
	this.baudRates.setLabelProvider(new LabelProvider());
	this.baudRates.setInput(Common.listBaudRates());

	this.baudRates.getCombo().setText(InstancePreferences.getGlobalString(Const.KEY_SERIAL_RATE, Const.EMPTY_STRING));
	this.serialPorts.getCombo().setText(InstancePreferences.getGlobalString(Const.KEY_SERIAL_PORT, Const.EMPTY_STRING));
	
	this.dtrCheckbox = new Button(parent, SWT.CHECK);
	this.dtrCheckbox.setText(Messages.openSerialDialogBoxDtr);
	this.dtrCheckbox.setSelection(true);
	
	return parent;

    }

    public String GetComPort() {
	return this.selectedPort;
    }

    public int GetBaudRate() {
	return this.selectedRate;
    }

	public boolean GetDtr() {
	return this.selectedDtr;
	}

}
