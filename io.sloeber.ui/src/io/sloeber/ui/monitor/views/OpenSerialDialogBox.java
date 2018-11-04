package io.sloeber.ui.monitor.views;

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

import io.sloeber.core.api.SerialManager;
import io.sloeber.ui.Messages;
import io.sloeber.ui.helpers.MyPreferences;

public class OpenSerialDialogBox extends Dialog {
    private ComboViewer mySerialPorts;
    private ComboViewer myBaudRates;
    private Button myDtrCheckbox;
    private String mySelectedPort;
    private int mySelectedRate;
    private boolean mySelectedDtr;

    protected OpenSerialDialogBox(Shell parentShell) {
	super(parentShell);
    }

    @Override
    protected void okPressed() {
	// I need to save these values in local variables as the GUI stuff is
	// deleted after he close
	mySelectedRate = Integer.parseInt(myBaudRates.getCombo().getText());
	mySelectedPort = mySerialPorts.getCombo().getText();
	mySelectedDtr = myDtrCheckbox.getSelection();
	MyPreferences.setLastUsedBaudRate(myBaudRates.getCombo().getText());
	MyPreferences.setLastUsedPort(mySelectedPort);
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


	// Always allow to provide your com port name https://github.com/Sloeber/arduino-eclipse-plugin/issues/1034
	mySerialPorts = new ComboViewer(parent, SWT.DROP_DOWN);
	mySerialPorts.getControl().setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false));
	mySerialPorts.setContentProvider(new ArrayContentProvider());
	mySerialPorts.setLabelProvider(new LabelProvider());
	mySerialPorts.setInput(SerialManager.listComPorts());

	// Create baud rate selection combo box to select the baud rate
	Label label2 = new Label(parent, SWT.NONE);
	label2.setText(Messages.openSerialDialogBoxSelectTheBautRate);
	myBaudRates = new ComboViewer(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
	myBaudRates.getControl().setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false));
	myBaudRates.setContentProvider(new ArrayContentProvider());
	myBaudRates.setLabelProvider(new LabelProvider());
	myBaudRates.setInput(SerialManager.listBaudRates());

	myBaudRates.getCombo().setText(MyPreferences.getLastUsedRate());
	mySerialPorts.getCombo().setText(MyPreferences.getLastUsedPort());

	myDtrCheckbox = new Button(parent, SWT.CHECK);
	myDtrCheckbox.setText(Messages.openSerialDialogBoxDtr);
	myDtrCheckbox.setSelection(true);

	return parent;

    }

    public String GetComPort() {
	return mySelectedPort;
    }

    public int GetBaudRate() {
	return mySelectedRate;
    }

    public boolean GetDtr() {
	return mySelectedDtr;
    }

}
