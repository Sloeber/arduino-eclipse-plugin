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
	private GridData mComboGriddata;
	private GridData mLabelGriddata;
	private String myID = new String();

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
		mLabel = new Label(composite, SWT.NONE);
		mLabel.setText(menuName + " :"); //$NON-NLS-1$
		mLabelGriddata = new GridData();
		mLabelGriddata.horizontalSpan = 1;// (ncol - 1);
		mLabelGriddata.horizontalAlignment = SWT.FILL;
		mLabel.setLayoutData(mLabelGriddata);
		if (fixedList) {
			mCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		} else {
			mCombo = new Combo(composite, SWT.BORDER);
		}
		mComboGriddata = new GridData();
		mComboGriddata.horizontalSpan = horSpan;// (ncol - 1);
		mComboGriddata.horizontalAlignment = SWT.FILL;
		mCombo.setLayoutData(mComboGriddata);
		mMenuName = menuName;

	}

	public void addListener(Listener listener) {
		mCombo.addListener(SWT.Modify, listener);
		myListener = listener;
	}

	private Label mLabel;
	public Combo mCombo;
	private String myValue = ""; //$NON-NLS-1$
	private String mMenuName;
	private Listener myListener = null;

	public String getValue() {
		myValue = mCombo.getText().trim();
		return myValue;
	}

	public String getMenuName() {
		return mMenuName.trim();
	}

	public void setValue(String value) {
		myValue = value;
		mCombo.setText(value);
	}

	public void setVisible(boolean visible) {
		boolean newvisible = visible && (mCombo.getItemCount() > 0);
		mLabel.setVisible(newvisible);
		mCombo.setVisible(newvisible);
		mComboGriddata.exclude = !newvisible;
		mLabelGriddata.exclude = !newvisible;
	}

	public boolean isValid() {
		return !mCombo.getText().isEmpty() || mCombo.getItemCount() == 0;
	}

	public void setEnabled(boolean enabled) {
		mCombo.setEnabled(enabled);
	}

	public void setItems(String[] items) {
		if (myListener != null)
			mCombo.removeListener(SWT.Modify, myListener);
		mCombo.setItems(items);
		mCombo.setText(myValue);
		if (myListener != null)
			mCombo.addListener(SWT.Modify, myListener);

	}

	public void add(String item) {
		mCombo.add(item);
	}

	public String getID() {
		return myID;
	}

	public boolean isVisible() {
		return (mCombo.getItemCount() > 0);
	}

	public void setLabel(String newLabel) {
		mLabel.setText(newLabel);

	}
}
