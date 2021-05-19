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
	private Label myLabel;
	private Combo myCombo;
	private String myMenuName;

	/**
	 * Create a combo box with a label in front of it.
	 * @param composite
	 * @param menuName
	 * @param ID
	 * @param horSpan
	 * @param fixedList if true only items of the list can be selected. If false you can type any text you want
	 */
	public LabelCombo(Composite composite, String menuName, int horSpan, boolean fixedList) {
		myLabel = new Label(composite, SWT.NONE);
		myLabel.setText(menuName + " :"); //$NON-NLS-1$
		GridData myLabelGriddata = new GridData();
		myLabelGriddata.horizontalSpan = 1;
		myLabelGriddata.horizontalAlignment = SWT.FILL;
		myLabel.setLayoutData(myLabelGriddata);
		if (fixedList) {
			myCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		} else {
			myCombo = new Combo(composite, SWT.BORDER);
		}
		GridData myComboGriddata = new GridData();
		myComboGriddata.horizontalSpan = horSpan;
		myComboGriddata.horizontalAlignment = SWT.FILL;
		myCombo.setLayoutData(myComboGriddata);
		myMenuName = menuName;
		myCombo.layout();

	}

	public void addListener(Listener listener) {
		myCombo.addListener(SWT.Modify, listener);
	}

	public String getMenuName() {
		return myMenuName.trim();
	}

	public void dispose() {
		myLabel.dispose();
		myCombo.dispose();
	}

	public boolean isValid() {
		return !myCombo.getText().isEmpty() || myCombo.getItemCount() == 0;
	}

	public void setEnabled(boolean enabled) {
		myCombo.setEnabled(enabled);
	}

	public void setItems(String[] items) {
		Listener[] listeners = myCombo.getListeners(SWT.Modify);
		for (Listener curListener : listeners) {
			myCombo.removeListener(SWT.Modify, curListener);
		}
		String curValue = getText();
		myCombo.setItems(items);
		myCombo.setText(curValue);
		for (Listener curListener : listeners) {
			myCombo.addListener(SWT.Modify, curListener);
		}

	}

	public void add(String item) {
		myCombo.add(item);
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
		return myCombo.getText().trim();
	}

	public void setText(String text) {
		myCombo.setText(text);
		
	}

	public void addListener(int event, Listener comboListener) {
		myCombo.addListener( event,  comboListener);
		
	}

}
