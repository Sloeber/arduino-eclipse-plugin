package io.sloeber.ui.monitor.views;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import io.sloeber.core.api.SerialManager;
import io.sloeber.ui.LabelCombo;
import io.sloeber.ui.Messages;
import io.sloeber.ui.helpers.MyPreferences;

public class OpenSerialDialogBox extends Dialog {
    private LabelCombo mySerialPorts;
    private LabelCombo myBaudRates;
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
	mySelectedRate = Integer.parseInt(myBaudRates.getText());
	mySelectedPort = mySerialPorts.getText();
	mySelectedDtr = myDtrCheckbox.getSelection();
	MyPreferences.setLastUsedBaudRate(myBaudRates.getText());
	MyPreferences.setLastUsedPort(mySelectedPort);
	super.okPressed();
    }

    @Override
    protected Control createDialogArea(Composite parent) {
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	parent.setLayout(layout);
	mySerialPorts = new LabelCombo(parent, Messages.openSerialDialogBoxSerialPortToConnectTo, null, 1, false);
	mySerialPorts.setItems(SerialManager.listComPorts());
	mySerialPorts.setText(MyPreferences.getLastUsedPort());

	myBaudRates = new LabelCombo(parent, Messages.openSerialDialogBoxSelectTheBautRate, null, 1, false);
	myBaudRates.setItems(SerialManager.listBaudRates());
	myBaudRates.setText(MyPreferences.getLastUsedRate());

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
