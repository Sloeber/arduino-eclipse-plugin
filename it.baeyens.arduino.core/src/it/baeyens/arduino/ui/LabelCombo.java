package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.Common;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
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
    public LabelCombo(Composite composite, String menuName, int horSpan, String envName, boolean readOnly) {
	mLabel = new Label(composite, SWT.NONE);
	mLabel.setText(menuName + " :");
	if (readOnly) {
	    mCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
	} else {
	    mCombo = new Combo(composite, SWT.BORDER);
	}
	GridData theGriddata = new GridData();
	theGriddata.horizontalSpan = horSpan;// (ncol - 1);
	theGriddata.horizontalAlignment = SWT.FILL;
	mCombo.setLayoutData(theGriddata);
	myEnvName = envName;
	mMenuName = menuName;

    }

    public void addListener(Listener listener) {
	mCombo.addListener(SWT.Modify, listener);
	myListener = listener;
    }

    private Label mLabel;
    public Combo mCombo;
    private String myValue = "";
    private String myEnvName;
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

    public void getStoredValue(ICConfigurationDescription confdesc) {
	String optionValue = Common.getBuildEnvironmentVariable(confdesc, myEnvName, myValue, true);
	setValue(optionValue);
    }

    public void StoreValue(ICConfigurationDescription confdesc) {
	IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
	myValue = mCombo.getText();
	IEnvironmentVariable var = new EnvironmentVariable(myEnvName, myValue);
	contribEnv.addVariable(var, confdesc);
    }

    public void setVisible(boolean visible) {
	boolean newvisible = visible && (mCombo.getItemCount() > 0);
	mLabel.setVisible(newvisible);
	mCombo.setVisible(newvisible);
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
}
