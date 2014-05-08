package it.baeyens.arduino.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;

/*
 * a class containing a label and a combobox in one. This makes it easier to make both visible together
 */
class LabelCombo {
    public LabelCombo(Composite composite, String label, int horSpan, Listener listener) {
	mLabel = new Label(composite, SWT.NONE);
	mLabel.setText(label + " :");
	mCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
	GridData theGriddata = new GridData();
	theGriddata.horizontalSpan = horSpan;// (ncol - 1);
	theGriddata.horizontalAlignment = SWT.FILL;
	mCombo.setLayoutData(theGriddata);
	mCombo.setData("Menu", label);
	mCombo.addListener(SWT.Modify, listener);// ValidationListener);

    }

    Label mLabel;
    Combo mCombo;

    public void setVisible(boolean visible) {
	mLabel.setVisible(visible);
	mCombo.setVisible(visible);
    }
}
