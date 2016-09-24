package io.sloeber.core.api;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;

import io.sloeber.common.Const;

public class CompileOptions {

    private boolean myWarningLevel = false;
    private boolean myAlternativeSizeCommand = false;
    private String myAditional_CPP_CompileOptions = ""; //$NON-NLS-1$
    private String myAditional_C_CompileOptions = ""; //$NON-NLS-1$
    private String myAditional_C_andCPP_CompileOptions = ""; //$NON-NLS-1$

    public CompileOptions(ICConfigurationDescription confDesc) {

	IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
	IEnvironmentVariable var = contribEnv.getVariable(Const.ENV_KEY_JANTJE_WARNING_LEVEL, confDesc);
	if (var != null)
	    this.myWarningLevel = var.getValue().equalsIgnoreCase(Const.ENV_KEY_WARNING_LEVEL_ON);
	var = contribEnv.getVariable(Const.ENV_KEY_JANTJE_SIZE_SWITCH, confDesc);
	if (var != null)
	    this.myAlternativeSizeCommand = var.getValue().contains(Const.ENV_KEY_JANTJE_SIZE_COMMAND);
	var = contribEnv.getVariable(Const.ENV_KEY_JANTJE_ADDITIONAL_COMPILE_OPTIONS, confDesc);
	if (var != null)
	    this.myAditional_C_andCPP_CompileOptions = var.getValue();
	var = contribEnv.getVariable(Const.ENV_KEY_JANTJE_ADDITIONAL_C_COMPILE_OPTIONS, confDesc);
	if (var != null)
	    this.myAditional_C_CompileOptions = var.getValue();
	var = contribEnv.getVariable(Const.ENV_KEY_JANTJE_ADDITIONAL_CPP_COMPILE_OPTIONS, confDesc);
	if (var != null)
	    this.myAditional_CPP_CompileOptions = var.getValue();
    }

    public boolean isMyWarningLevel() {
	return this.myWarningLevel;
    }

    public void setMyWarningLevel(boolean myWarningLevel) {
	this.myWarningLevel = myWarningLevel;
    }

    public boolean isMyAlternativeSizeCommand() {
	return this.myAlternativeSizeCommand;
    }

    public void setMyAlternativeSizeCommand(boolean myAlternativeSizeCommand) {
	this.myAlternativeSizeCommand = myAlternativeSizeCommand;
    }

    public String getMyAditional_CPP_CompileOptions() {
	return this.myAditional_CPP_CompileOptions;
    }

    public void setMyAditional_CPP_CompileOptions(String myAditional_CPP_CompileOptions) {
	this.myAditional_CPP_CompileOptions = myAditional_CPP_CompileOptions;
    }

    public String getMyAditional_C_CompileOptions() {
	return this.myAditional_C_CompileOptions;
    }

    public void setMyAditional_C_CompileOptions(String myAditional_C_CompileOptions) {
	this.myAditional_C_CompileOptions = myAditional_C_CompileOptions;
    }

    public String getMyAditional_C_andCPP_CompileOptions() {
	return this.myAditional_C_andCPP_CompileOptions;
    }

    public void setMyAditional_C_andCPP_CompileOptions(String myAditional_C_andCPP_CompileOptions) {
	this.myAditional_C_andCPP_CompileOptions = myAditional_C_andCPP_CompileOptions;
    }

    public void save(ICConfigurationDescription configuration) {
	IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
	if (this.myWarningLevel) {
	    IEnvironmentVariable var = new EnvironmentVariable(Const.ENV_KEY_JANTJE_WARNING_LEVEL,
		    Const.ENV_KEY_WARNING_LEVEL_ON);
	    contribEnv.addVariable(var, configuration);
	} else {
	    IEnvironmentVariable var = new EnvironmentVariable(Const.ENV_KEY_JANTJE_WARNING_LEVEL,
		    Const.ENV_KEY_WARNING_LEVEL_OFF);
	    contribEnv.addVariable(var, configuration);
	}

	if (this.myAlternativeSizeCommand) {
	    IEnvironmentVariable var = new EnvironmentVariable(Const.ENV_KEY_JANTJE_SIZE_SWITCH, "${" //$NON-NLS-1$
		    + Const.ENV_KEY_JANTJE_SIZE_COMMAND + "}"); //$NON-NLS-1$
	    contribEnv.addVariable(var, configuration);
	} else {
	    IEnvironmentVariable var = new EnvironmentVariable(Const.ENV_KEY_JANTJE_SIZE_SWITCH, "${" //$NON-NLS-1$
		    + Const.get_ENV_KEY_RECIPE(Const.ACTION_SIZE) + "}"); //$NON-NLS-1$
	    contribEnv.addVariable(var, configuration);
	}
    }

}
