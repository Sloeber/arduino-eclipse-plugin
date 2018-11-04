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
	selectedRate = Integer.parseInt(baudRates.getCombo().getText());
	selectedPort = serialPorts.getCombo().getText();
	selectedDtr = dtrCheckbox.getSelection();
	MyPreferences.setLastUsedBaudRate(baudRates.getCombo().getText());
	MyPreferences.setLastUsedPort(selectedPort);
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
	serialPorts = new ComboViewer(parent, SWT.DROP_DOWN);
	serialPorts.getControl().setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false));
	serialPorts.setContentProvider(new ArrayContentProvider());
	serialPorts.setLabelProvider(new LabelProvider());
	serialPorts.setInput(SerialManager.listComPorts());

	// Create baud rate selection combo box to select the baud rate
	Label label2 = new Label(parent, SWT.NONE);
	label2.setText(Messages.openSerialDialogBoxSelectTheBautRate);
	baudRates = new ComboViewer(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
	baudRates.getControl().setLayoutData(new GridData(SWT.LEFT, SWT.NONE, false, false));
	baudRates.setContentProvider(new ArrayContentProvider());
	baudRates.setLabelProvider(new LabelProvider());
	baudRates.setInput(SerialManager.listBaudRates());

	baudRates.getCombo().setText(MyPreferences.getLastUsedRate());
	serialPorts.getCombo().setText(MyPreferences.getLastUsedPort());

	dtrCheckbox = new Button(parent, SWT.CHECK);
	dtrCheckbox.setText(Messages.openSerialDialogBoxDtr);
	dtrCheckbox.setSelection(true);

	return parent;

    }

    public String GetComPort() {
	return selectedPort;
    }

    public int GetBaudRate() {
	return selectedRate;
    }

    public boolean GetDtr() {
	return selectedDtr;
    }

}
