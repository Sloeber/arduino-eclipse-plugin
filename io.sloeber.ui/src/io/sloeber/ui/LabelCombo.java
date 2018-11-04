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
		this.myID = ID;
		this.mLabel = new Label(composite, SWT.NONE);
		this.mLabel.setText(menuName + " :"); //$NON-NLS-1$
		this.mLabelGriddata = new GridData();
		this.mLabelGriddata.horizontalSpan = 1;// (ncol - 1);
		this.mLabelGriddata.horizontalAlignment = SWT.FILL;
		this.mLabel.setLayoutData(this.mLabelGriddata);
		if (fixedList) {
			this.mCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		} else {
			this.mCombo = new Combo(composite, SWT.BORDER);
		}
		this.mComboGriddata = new GridData();
		this.mComboGriddata.horizontalSpan = horSpan;// (ncol - 1);
		this.mComboGriddata.horizontalAlignment = SWT.FILL;
		this.mCombo.setLayoutData(this.mComboGriddata);
		this.mMenuName = menuName;

	}

	public void addListener(Listener listener) {
		this.mCombo.addListener(SWT.Modify, listener);
		this.myListener = listener;
	}

	private Label mLabel;
	public Combo mCombo;
	private String myValue = ""; //$NON-NLS-1$
	private String mMenuName;
	private Listener myListener = null;

	public String getValue() {
		this.myValue = this.mCombo.getText().trim();
		return this.myValue;
	}

	public String getMenuName() {
		return this.mMenuName.trim();
	}

	public void setValue(String value) {
		this.myValue = value;
		this.mCombo.setText(value);
	}

	public void setVisible(boolean visible) {
		boolean newvisible = visible && (this.mCombo.getItemCount() > 0);
		this.mLabel.setVisible(newvisible);
		this.mCombo.setVisible(newvisible);
		this.mComboGriddata.exclude = !newvisible;
		this.mLabelGriddata.exclude = !newvisible;
	}

	public boolean isValid() {
		return !this.mCombo.getText().isEmpty() || this.mCombo.getItemCount() == 0;
	}

	public void setEnabled(boolean enabled) {
		this.mCombo.setEnabled(enabled);
	}

	public void setItems(String[] items) {
		if (this.myListener != null)
			this.mCombo.removeListener(SWT.Modify, this.myListener);
		this.mCombo.setItems(items);
		this.mCombo.setText(this.myValue);
		if (this.myListener != null)
			this.mCombo.addListener(SWT.Modify, this.myListener);

	}

	public void add(String item) {
		this.mCombo.add(item);
	}

	public String getID() {
		return this.myID;
	}

	public boolean isVisible() {
		return (this.mCombo.getItemCount() > 0);
	}

	public void setLabel(String newLabel) {
		this.mLabel.setText(newLabel);

	}
}
