package io.sloeber.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

/*
 * a class containing a label and a combobox in one. This makes it easier to make both visible together
 */
public class LabelCombo {
	private GridData myComboGriddata;
	private GridData myLabelGriddata;
	private String myID = new String();
	private Label myLabel;
	private Combo myCombo;
	private String myValue = ""; //$NON-NLS-1$
	private String myMenuName;
	private Listener myListener = null;

	/**
	 * Create a combo box with a label in front of it.
	 * @param composite
	 * @param menuName
	 * @param ID
	 * @param horSpan
	 * @param fixedList if true only items of the list can be selected. If false you can type any text you want
	 */
	public LabelCombo(Composite composite, String menuName, String ID, int horSpan, boolean fixedList) {
		myID = ID;
		myLabel = new Label(composite, SWT.NONE);
		myLabel.setText(menuName + " :"); //$NON-NLS-1$
		myLabelGriddata = new GridData();
		myLabelGriddata.horizontalSpan = 1;
		myLabelGriddata.horizontalAlignment = SWT.FILL;
		myLabel.setLayoutData(myLabelGriddata);
		if (fixedList) {
			myCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		} else {
			myCombo = new Combo(composite, SWT.BORDER);
		}
		myComboGriddata = new GridData();
		myComboGriddata.horizontalSpan = horSpan;
		myComboGriddata.horizontalAlignment = SWT.FILL;
		myCombo.setLayoutData(myComboGriddata);
		myMenuName = menuName;

	}

	public void addListener(Listener listener) {
		myCombo.addListener(SWT.Modify, listener);
		myListener = listener;
	}



	public String getValue() {
		myValue = myCombo.getText().trim();
		return myValue;
	}

	public String getMenuName() {
		return myMenuName.trim();
	}

	public void setValue(String value) {
		myValue = value;
		myCombo.setText(value);
	}

	public void setVisible(boolean visible) {
		boolean newvisible = visible && (myCombo.getItemCount() > 0);
		myLabel.setVisible(newvisible);
		myCombo.setVisible(newvisible);
		myComboGriddata.exclude = !newvisible;
		myLabelGriddata.exclude = !newvisible;
	}

	public boolean isValid() {
		return !myCombo.getText().isEmpty() || myCombo.getItemCount() == 0;
	}

	public void setEnabled(boolean enabled) {
		myCombo.setEnabled(enabled);
	}

	public void setItems(String[] items) {
		if (myListener != null)
			myCombo.removeListener(SWT.Modify, myListener);
		myCombo.setItems(items);
		myCombo.setText(myValue);
		if (myListener != null)
			myCombo.addListener(SWT.Modify, myListener);

	}

	public void add(String item) {
		myCombo.add(item);
	}

	public String getID() {
		return myID;
	}

	public boolean isVisible() {
		return (myCombo.getItemCount() > 0);
	}

	public void setLabel(String newLabel) {
		myLabel.setText(newLabel);

	}

	public int getSelectionIndex() {
		return myCombo.getSelectionIndex();
	}

	public void select(int ordinal) {
		myCombo.select(ordinal);
	}

	public String getText() {
		return myCombo.getText();
	}

	public void setText(String text) {
		myCombo.setText(text);
		
	}

	public void addListener(int event, Listener comboListener) {
		myCombo.addListener( event,  comboListener);
		
	}
}
