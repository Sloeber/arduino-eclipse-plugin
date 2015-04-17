package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.ArduinoConst;

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

public class ArduinoCompileProperties extends AbstractCPropertyTab {
    Button myWarningLevel;
    Button mySizeCommand;
    public Text myCCppCommand;
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
	usercomp.setLayout(theGridLayout);

	// checkbox show all warnings => Set WARNING_LEVEL=wall else
	// WARNING_LEVEL=$ARDUINO_WARNING_LEVEL
	myWarningLevel = new Button(usercomp, SWT.CHECK);
	myWarningLevel.setText("show all warnings?");
	myWarningLevel.setEnabled(true);
	myWarningLevel.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1));
	myWarningLevel.addListener(UPDATE, new Listener() {
	    @Override
	    public void handleEvent(Event e) {
		if (getResDesc().getConfiguration() != null) {
		    IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		    IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
		    if (myWarningLevel.getSelection() == true) {
			IEnvironmentVariable var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_WARNING_LEVEL,
				ArduinoConst.ENV_KEY_WARNING_LEVEL_ON);
			contribEnv.addVariable(var, getResDesc().getConfiguration());
		    } else {
			IEnvironmentVariable var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_WARNING_LEVEL,
				ArduinoConst.ENV_KEY_WARNING_LEVEL_OFF);
			contribEnv.addVariable(var, getResDesc().getConfiguration());
		    }
		}
	    }
	});

	// checkbox show alternative size
	mySizeCommand = new Button(usercomp, SWT.CHECK);
	mySizeCommand.setText("Use alternative size command?");
	mySizeCommand.setEnabled(true);
	mySizeCommand.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 2, 1));
	mySizeCommand.addListener(UPDATE, new Listener() {
	    @Override
	    public void handleEvent(Event e) {
		if (getResDesc().getConfiguration() != null) {
		    IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		    IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
		    if (mySizeCommand.getSelection() == true) {
			IEnvironmentVariable var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_SIZE_SWITCH, "${"
				+ ArduinoConst.ENV_KEY_JANTJE_SIZE_COMMAND + "}");
			contribEnv.addVariable(var, getResDesc().getConfiguration());
		    } else {
			IEnvironmentVariable var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_SIZE_SWITCH, "${"
				+ ArduinoConst.ENV_KEY_recipe_size_pattern + "}");
			contribEnv.addVariable(var, getResDesc().getConfiguration());
		    }
		}
	    }
	});

	createLine(usercomp, 2);
	// edit field add to C & C++ command line
	Label label = new Label(usercomp, SWT.LEFT);
	label.setText("append to C and C++ ");
	label.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 2));
	myCCppCommand = new Text(usercomp, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
	myCCppCommand.setText("");
	myCCppCommand.setToolTipText("This command is added to the C and C++ compile command.");
	myCCppCommand.setEnabled(true);

	GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
	gridData.horizontalSpan = 1;
	gridData.verticalSpan = 2;
	myCCppCommand.setLayoutData(gridData);
	myCCppCommand.addModifyListener(new ModifyListener() {

	    @Override
	    public void modifyText(ModifyEvent e) {
		if (getResDesc().getConfiguration() != null) {
		    IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		    IContributedEnvironment contribEnv = envManager.getContributedEnvironment();

		    IEnvironmentVariable var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_ADDITIONAL_COMPILE_OPTIONS, myCCppCommand
			    .getText());
		    contribEnv.addVariable(var, getResDesc().getConfiguration());
		}

	    }
	});

	// edit field add to C++ command line
	label = new Label(usercomp, SWT.LEFT);
	label.setText("append to C++ ");
	label.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 2));
	myCppCommand = new Text(usercomp, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
	myCppCommand.setText("");
	myCppCommand.setToolTipText("This command is added to the C++ compile command.");
	myCppCommand.setEnabled(true);
	myCppCommand.setLayoutData(gridData);
	myCppCommand.addModifyListener(new ModifyListener() {

	    @Override
	    public void modifyText(ModifyEvent e) {
		if (getResDesc().getConfiguration() != null) {
		    IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		    IContributedEnvironment contribEnv = envManager.getContributedEnvironment();

		    IEnvironmentVariable var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_ADDITIONAL_CPP_COMPILE_OPTIONS, myCppCommand
			    .getText());
		    contribEnv.addVariable(var, getResDesc().getConfiguration());
		}

	    }
	});

	// edit field add to C command line
	label = new Label(usercomp, SWT.LEFT);
	label.setText("append to C ");
	label.setLayoutData(new GridData(SWT.BEGINNING, SWT.BEGINNING, false, false, 1, 2));
	myCCommand = new Text(usercomp, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
	myCCommand.setText("");
	myCCommand.setToolTipText("This command is added to the C compile command.");
	myCCommand.setEnabled(true);
	myCCommand.setLayoutData(gridData);
	myCCommand.addModifyListener(new ModifyListener() {

	    @Override
	    public void modifyText(ModifyEvent e) {
		if (getResDesc().getConfiguration() != null) {
		    IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		    IContributedEnvironment contribEnv = envManager.getContributedEnvironment();

		    IEnvironmentVariable var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_ADDITIONAL_C_COMPILE_OPTIONS, myCCommand.getText());
		    contribEnv.addVariable(var, getResDesc().getConfiguration());
		}

	    }
	});

	theGridLayout = new GridLayout();
	theGridLayout.numColumns = 2;
	usercomp.setLayout(theGridLayout);
	setValues(getResDesc().getConfiguration());
	setVisible(true);
    }

    private void setValues(ICConfigurationDescription confDesc) {
	IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
	IEnvironmentVariable var = contribEnv.getVariable(ArduinoConst.ENV_KEY_JANTJE_WARNING_LEVEL, confDesc);
	if (var == null)
	    myWarningLevel.setSelection(false);
	else
	    myWarningLevel.setSelection((var.getValue().equalsIgnoreCase(ArduinoConst.ENV_KEY_WARNING_LEVEL_ON)));
	var = contribEnv.getVariable(ArduinoConst.ENV_KEY_JANTJE_SIZE_SWITCH, confDesc);
	if (var == null)
	    mySizeCommand.setSelection(false);
	else
	    mySizeCommand.setSelection((var.getValue().contains(ArduinoConst.ENV_KEY_JANTJE_SIZE_COMMAND)));
	var = contribEnv.getVariable(ArduinoConst.ENV_KEY_JANTJE_ADDITIONAL_COMPILE_OPTIONS, confDesc);
	if (var == null)
	    myCCppCommand.setText("");
	else
	    myCCppCommand.setText(var.getValue());
	var = contribEnv.getVariable(ArduinoConst.ENV_KEY_JANTJE_ADDITIONAL_C_COMPILE_OPTIONS, confDesc);
	if (var == null)
	    myCCommand.setText("");
	else
	    myCCommand.setText(var.getValue());
	var = contribEnv.getVariable(ArduinoConst.ENV_KEY_JANTJE_ADDITIONAL_CPP_COMPILE_OPTIONS, confDesc);
	if (var == null)
	    myCppCommand.setText("");
	else
	    myCppCommand.setText(var.getValue());
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
	myWarningLevel.setSelection(true);
	mySizeCommand.setSelection(false);
	myCCppCommand.setText("");
	myCCommand.setText("");
	myCppCommand.setText("");
    }
}
