package io.sloeber.core.ui;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.cdt.ui.newui.ICPropertyProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import io.sloeber.common.Const;

public class CompileProperties extends AbstractCPropertyTab {
    Button myWarningLevel;
    Button mySizeCommand;
    public Text myCAndCppCommand;
    public Text myCppCommand;
    public Text myCCommand;

    private static void createLine(Composite parent, int ncol) {
	Label line = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.BOLD);
	GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
	gridData.horizontalSpan = ncol;
	line.setLayoutData(gridData);
    }

    @Override
    public void createControls(Composite parent, ICPropertyProvider provider) {
	super.createControls(parent, provider);
	GridLayout theGridLayout = new GridLayout();
	theGridLayout.numColumns = 2;
	this.usercomp.setLayout(theGridLayout);

	// checkbox show all warnings => Set WARNING_LEVEL=wall else
	// WARNING_LEVEL=$ARDUINO_WARNING_LEVEL
	this.myWarningLevel = new Button(this.usercomp, SWT.CHECK);
	this.myWarningLevel.setText(Messages.ui_show_all_warnings);
	this.myWarningLevel.setEnabled(true);
	this.myWarningLevel.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1));
	this.myWarningLevel.addListener(UPDATE, new Listener() {
	    @Override
	    public void handleEvent(Event e) {
		if (getResDesc().getConfiguration() != null) {
		    IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		    IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
		    if (CompileProperties.this.myWarningLevel.getSelection() == true) {
			IEnvironmentVariable var = new EnvironmentVariable(Const.ENV_KEY_JANTJE_WARNING_LEVEL, Const.ENV_KEY_WARNING_LEVEL_ON);
			contribEnv.addVariable(var, getResDesc().getConfiguration());
		    } else {
			IEnvironmentVariable var = new EnvironmentVariable(Const.ENV_KEY_JANTJE_WARNING_LEVEL, Const.ENV_KEY_WARNING_LEVEL_OFF);
			contribEnv.addVariable(var, getResDesc().getConfiguration());
		    }
		}
	    }
	});

	// checkbox show alternative size
	this.mySizeCommand = new Button(this.usercomp, SWT.CHECK);
	this.mySizeCommand.setText(Messages.ui_Alternative_size);
	this.mySizeCommand.setEnabled(true);
	this.mySizeCommand.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1));
	this.mySizeCommand.addListener(UPDATE, new Listener() {
	    @Override
	    public void handleEvent(Event e) {
		if (getResDesc().getConfiguration() != null) {
		    IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		    IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
		    if (CompileProperties.this.mySizeCommand.getSelection() == true) {
			IEnvironmentVariable var = new EnvironmentVariable(Const.ENV_KEY_JANTJE_SIZE_SWITCH, "${" //$NON-NLS-1$
				+ Const.ENV_KEY_JANTJE_SIZE_COMMAND + "}"); //$NON-NLS-1$
			contribEnv.addVariable(var, getResDesc().getConfiguration());
		    } else {
			IEnvironmentVariable var = new EnvironmentVariable(Const.ENV_KEY_JANTJE_SIZE_SWITCH, "${" //$NON-NLS-1$
				+ Const.get_ENV_KEY_RECIPE(Const.ACTION_SIZE) + "}"); //$NON-NLS-1$
			contribEnv.addVariable(var, getResDesc().getConfiguration());
		    }
		}
	    }
	});

	createLine(this.usercomp, 2);
	// edit field add to C & C++ command line
	Label label = new Label(this.usercomp, SWT.LEFT);
	label.setText(Messages.ui_Apend_c_cpp);
	label.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 2));
	this.myCAndCppCommand = new Text(this.usercomp, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
	this.myCAndCppCommand.setText(Const.EMPTY_STRING);
	this.myCAndCppCommand.setToolTipText(Messages.ui_append_c_cpp_text);
	this.myCAndCppCommand.setEnabled(true);

	GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
	gridData.horizontalSpan = 1;
	gridData.verticalSpan = 2;
	this.myCAndCppCommand.setLayoutData(gridData);
	this.myCAndCppCommand.addModifyListener(new ModifyListener() {

	    @Override
	    public void modifyText(ModifyEvent e) {
		if (getResDesc().getConfiguration() != null) {
		    IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		    IContributedEnvironment contribEnv = envManager.getContributedEnvironment();

		    IEnvironmentVariable var = new EnvironmentVariable(Const.ENV_KEY_JANTJE_ADDITIONAL_COMPILE_OPTIONS,
			    CompileProperties.this.myCAndCppCommand.getText());
		    contribEnv.addVariable(var, getResDesc().getConfiguration());
		}

	    }
	});

	// edit field add to C++ command line
	label = new Label(this.usercomp, SWT.LEFT);
	label.setText(Messages.ui_append_cpp);
	label.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 2));
	this.myCppCommand = new Text(this.usercomp, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
	this.myCppCommand.setText(Const.EMPTY_STRING);
	this.myCppCommand.setToolTipText(Messages.ui_append_cpp_text);
	this.myCppCommand.setEnabled(true);
	this.myCppCommand.setLayoutData(gridData);
	this.myCppCommand.addModifyListener(new ModifyListener() {

	    @Override
	    public void modifyText(ModifyEvent e) {
		if (getResDesc().getConfiguration() != null) {
		    IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		    IContributedEnvironment contribEnv = envManager.getContributedEnvironment();

		    IEnvironmentVariable var = new EnvironmentVariable(Const.ENV_KEY_JANTJE_ADDITIONAL_CPP_COMPILE_OPTIONS,
			    CompileProperties.this.myCppCommand.getText());
		    contribEnv.addVariable(var, getResDesc().getConfiguration());
		}

	    }
	});

	// edit field add to C command line
	label = new Label(this.usercomp, SWT.LEFT);
	label.setText(Messages.ui_append_c);
	label.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 2));
	this.myCCommand = new Text(this.usercomp, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.BORDER);
	this.myCCommand.setText(Const.EMPTY_STRING);
	this.myCCommand.setToolTipText(Messages.ui_append_c_text);
	this.myCCommand.setEnabled(true);
	this.myCCommand.setLayoutData(gridData);
	this.myCCommand.addModifyListener(new ModifyListener() {

	    @Override
	    public void modifyText(ModifyEvent e) {
		if (getResDesc().getConfiguration() != null) {
		    IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		    IContributedEnvironment contribEnv = envManager.getContributedEnvironment();

		    IEnvironmentVariable var = new EnvironmentVariable(Const.ENV_KEY_JANTJE_ADDITIONAL_C_COMPILE_OPTIONS,
			    CompileProperties.this.myCCommand.getText());
		    contribEnv.addVariable(var, getResDesc().getConfiguration());
		}

	    }
	});

	theGridLayout = new GridLayout();
	theGridLayout.numColumns = 2;
	this.usercomp.setLayout(theGridLayout);
	setValues(getResDesc().getConfiguration());
	setVisible(true);
    }

    private void setValues(ICConfigurationDescription confDesc) {
	IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
	IEnvironmentVariable var = contribEnv.getVariable(Const.ENV_KEY_JANTJE_WARNING_LEVEL, confDesc);
	if (var == null)
	    this.myWarningLevel.setSelection(false);
	else
	    this.myWarningLevel.setSelection((var.getValue().equalsIgnoreCase(Const.ENV_KEY_WARNING_LEVEL_ON)));
	var = contribEnv.getVariable(Const.ENV_KEY_JANTJE_SIZE_SWITCH, confDesc);
	if (var == null)
	    this.mySizeCommand.setSelection(false);
	else
	    this.mySizeCommand.setSelection((var.getValue().contains(Const.ENV_KEY_JANTJE_SIZE_COMMAND)));
	var = contribEnv.getVariable(Const.ENV_KEY_JANTJE_ADDITIONAL_COMPILE_OPTIONS, confDesc);
	if (var == null)
	    this.myCAndCppCommand.setText(Const.EMPTY_STRING);
	else
	    this.myCAndCppCommand.setText(var.getValue());
	var = contribEnv.getVariable(Const.ENV_KEY_JANTJE_ADDITIONAL_C_COMPILE_OPTIONS, confDesc);
	if (var == null)
	    this.myCCommand.setText(Const.EMPTY_STRING);
	else
	    this.myCCommand.setText(var.getValue());
	var = contribEnv.getVariable(Const.ENV_KEY_JANTJE_ADDITIONAL_CPP_COMPILE_OPTIONS, confDesc);
	if (var == null)
	    this.myCppCommand.setText(Const.EMPTY_STRING);
	else
	    this.myCppCommand.setText(var.getValue());
    }

    @Override
    protected void updateData(ICResourceDescription cfg) {
	setValues(cfg.getConfiguration());
    }

    @Override
    public boolean canBeVisible() {
	return true;
    }

    @Override
    protected void updateButtons() {
	// nothing to do here

    }

    @Override
    protected void performApply(ICResourceDescription src, ICResourceDescription dst) {
	// nothing to do here
    }

    @Override
    protected void performDefaults() {
	this.myWarningLevel.setSelection(true);
	this.mySizeCommand.setSelection(false);
	this.myCAndCppCommand.setText(Const.EMPTY_STRING);
	this.myCCommand.setText(Const.EMPTY_STRING);
	this.myCppCommand.setText(Const.EMPTY_STRING);
    }
}
