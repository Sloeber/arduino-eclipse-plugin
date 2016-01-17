package it.baeyens.arduino.ui;

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

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;

/*
 * a class containing a label and a combobox in one. This makes it easier to make both visible together
 */
class LabelCombo {
    public LabelCombo(Composite composite, String menuName, int horSpan, String envName, boolean readOnly) {
	this.mLabel = new Label(composite, SWT.NONE);
	this.mLabel.setText(menuName + " :"); //$NON-NLS-1$
	if (readOnly) {
	    this.mCombo = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
	} else {
	    this.mCombo = new Combo(composite, SWT.BORDER);
	}
	GridData theGriddata = new GridData();
	theGriddata.horizontalSpan = horSpan;// (ncol - 1);
	theGriddata.horizontalAlignment = SWT.FILL;
	this.mCombo.setLayoutData(theGriddata);
	this.myEnvName = envName;
	this.mMenuName = menuName;

    }

    public void addListener(Listener listener) {
	this.mCombo.addListener(SWT.Modify, listener);
	this.myListener = listener;
    }

    private Label mLabel;
    public Combo mCombo;
    private String myValue = ArduinoConst.EMPTY_STRING;
    private String myEnvName;
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

    public void getStoredValue(ICConfigurationDescription confdesc) {
	String optionValue = Common.getBuildEnvironmentVariable(confdesc, this.myEnvName, this.myValue, true);
	setValue(optionValue);
    }

    public void StoreValue(ICConfigurationDescription confdesc) {
	IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
	this.myValue = this.mCombo.getText();
	IEnvironmentVariable var = new EnvironmentVariable(this.myEnvName, this.myValue);
	contribEnv.addVariable(var, confdesc);
    }

    public void setVisible(boolean visible) {
	boolean newvisible = visible && (this.mCombo.getItemCount() > 0);
	this.mLabel.setVisible(newvisible);
	this.mCombo.setVisible(newvisible);
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
}
