package io.sloeber.core.api;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;

import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;

public class CompileOptions {

	private Boolean myWarningLevel = new Boolean(true);
	private boolean myAlternativeSizeCommand = false;
	private String myAditional_CPP_CompileOptions = ""; //$NON-NLS-1$
	private String myAditional_C_CompileOptions = ""; //$NON-NLS-1$
	private String myAditional_C_andCPP_CompileOptions = ""; //$NON-NLS-1$
	private static final String ENV_KEY_WARNING_LEVEL_OFF = "A.COMPILER.WARNING_FLAGS"; //$NON-NLS-1$
	private static final String ENV_KEY_WARNING_LEVEL_ON = "${A.COMPILER.WARNING_FLAGS.ALL}"; //$NON-NLS-1$
	public static final String ENV_KEY_JANTJE_ADDITIONAL_COMPILE_OPTIONS = Const.ENV_KEY_JANTJE_START + "EXTRA.COMPILE"; //$NON-NLS-1$
	public static final String ENV_KEY_JANTJE_ADDITIONAL_C_COMPILE_OPTIONS = Const.ENV_KEY_JANTJE_START
			+ "EXTRA.C.COMPILE"; //$NON-NLS-1$
	public static final String ENV_KEY_JANTJE_ADDITIONAL_CPP_COMPILE_OPTIONS = Const.ENV_KEY_JANTJE_START
			+ "EXTRA.CPP.COMPILE"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_WARNING_LEVEL = Const.ENV_KEY_JANTJE_START + "WARNING_LEVEL"; //$NON-NLS-1$
	public static final String ENV_KEY_JANTJE_SIZE_COMMAND = Const.ERASE_START + "ALT_SIZE_COMMAND"; //$NON-NLS-1$
	public static final String ENV_KEY_JANTJE_SIZE_SWITCH = Const.ENV_KEY_JANTJE_START + "SIZE.SWITCH"; //$NON-NLS-1$

	/**
	 * gets the compile options stored in this configuration description. if the
	 * configuration description is null the default compile options are
	 * returned.
	 *
	 * @param confDesc
	 *            null for default or the configuration description you want the
	 *            compile options for
	 */
	public CompileOptions(ICConfigurationDescription confDesc) {
		if (confDesc != null) {

			IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
			IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
			IEnvironmentVariable var = contribEnv.getVariable(ENV_KEY_JANTJE_WARNING_LEVEL, confDesc);
			if (var != null)
				this.myWarningLevel = Boolean.valueOf(var.getValue());
			var = contribEnv.getVariable(ENV_KEY_JANTJE_SIZE_SWITCH, confDesc);
			if (var != null)
				this.myAlternativeSizeCommand = var.getValue().contains(ENV_KEY_JANTJE_SIZE_COMMAND);
			var = contribEnv.getVariable(ENV_KEY_JANTJE_ADDITIONAL_COMPILE_OPTIONS, confDesc);
			if (var != null)
				this.myAditional_C_andCPP_CompileOptions = var.getValue();
			var = contribEnv.getVariable(ENV_KEY_JANTJE_ADDITIONAL_C_COMPILE_OPTIONS, confDesc);
			if (var != null)
				this.myAditional_C_CompileOptions = var.getValue();
			var = contribEnv.getVariable(ENV_KEY_JANTJE_ADDITIONAL_CPP_COMPILE_OPTIONS, confDesc);
			if (var != null)
				this.myAditional_CPP_CompileOptions = var.getValue();

		}
	}

	public boolean isMyWarningLevel() {
		return this.myWarningLevel.booleanValue();
	}

	public void setMyWarningLevel(boolean myWarningLevel) {
		this.myWarningLevel = new Boolean(myWarningLevel);
	}

	public boolean isMyAlternativeSizeCommand() {
		return this.myAlternativeSizeCommand;
	}

	public void setMyAlternativeSizeCommand(boolean alternativeSizeCommand) {
		this.myAlternativeSizeCommand = alternativeSizeCommand;
	}

	public String getMyAditional_CPP_CompileOptions() {
		return this.myAditional_CPP_CompileOptions;
	}

	public void setMyAditional_CPP_CompileOptions(String aditional_CPP_CompileOptions) {
		this.myAditional_CPP_CompileOptions = aditional_CPP_CompileOptions;
	}

	public String getMyAditional_C_CompileOptions() {
		return this.myAditional_C_CompileOptions;
	}

	public void setMyAditional_C_CompileOptions(String aditional_C_CompileOptions) {
		this.myAditional_C_CompileOptions = aditional_C_CompileOptions;
	}

	public String getMyAditional_C_andCPP_CompileOptions() {
		return this.myAditional_C_andCPP_CompileOptions;
	}

	public void setMyAditional_C_andCPP_CompileOptions(String myAditional_C_andCPP_CompileOptions) {
		this.myAditional_C_andCPP_CompileOptions = myAditional_C_andCPP_CompileOptions;
	}

	/**
	 * save the compilation options in this configuration description.
	 *
	 * @param configuration
	 *            must be a valid configuration description
	 */
	public void save(ICConfigurationDescription configuration) {
		IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
		IEnvironmentVariable var = new EnvironmentVariable(ENV_KEY_JANTJE_WARNING_LEVEL,
				this.myWarningLevel.toString());
		contribEnv.addVariable(var, configuration);

		if (this.isMyWarningLevel()) {
			var = new EnvironmentVariable(ENV_KEY_WARNING_LEVEL_OFF, ENV_KEY_WARNING_LEVEL_ON);
			contribEnv.addVariable(var, configuration);
		}
		if (this.myAlternativeSizeCommand) {
			var = new EnvironmentVariable(ENV_KEY_JANTJE_SIZE_SWITCH, "${" //$NON-NLS-1$
					+ ENV_KEY_JANTJE_SIZE_COMMAND + "}"); //$NON-NLS-1$
			contribEnv.addVariable(var, configuration);
		} else {
			var = new EnvironmentVariable(ENV_KEY_JANTJE_SIZE_SWITCH, "${" //$NON-NLS-1$
					+ Common.get_ENV_KEY_RECIPE(Const.ACTION_SIZE) + "}"); //$NON-NLS-1$
			contribEnv.addVariable(var, configuration);
		}
		var = new EnvironmentVariable(ENV_KEY_JANTJE_ADDITIONAL_COMPILE_OPTIONS,
				this.myAditional_C_andCPP_CompileOptions);
		contribEnv.addVariable(var, configuration);
		var = new EnvironmentVariable(ENV_KEY_JANTJE_ADDITIONAL_CPP_COMPILE_OPTIONS,
				this.myAditional_CPP_CompileOptions);
		contribEnv.addVariable(var, configuration);
		var = new EnvironmentVariable(ENV_KEY_JANTJE_ADDITIONAL_C_COMPILE_OPTIONS, this.myAditional_C_CompileOptions);
		contribEnv.addVariable(var, configuration);

	}

}
