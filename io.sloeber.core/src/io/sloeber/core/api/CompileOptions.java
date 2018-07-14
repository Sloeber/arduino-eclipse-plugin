package io.sloeber.core.api;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IProject;

import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.core.tools.Helpers;

public class CompileOptions {


	private Boolean myWarningLevel = new Boolean(true);
	private boolean myAlternativeSizeCommand = false;
	private String my_CPP_CompileOptions = new String();
	private String my_C_CompileOptions = new String();
	private String my_C_andCPP_CompileOptions = new String();
	private String my_Assembly_CompileOptions = new String();
	private String my_Archive_CompileOptions = new String();
	private String my_Link_CompileOptions = new String();
	private String my_All_CompileOptions = new String();

	private boolean myEnableParallelBuild;


	private static final String ENV_KEY_WARNING_LEVEL_OFF = "A.COMPILER.WARNING_FLAGS"; //$NON-NLS-1$
	private static final String ENV_KEY_WARNING_LEVEL_ON = "${A.COMPILER.WARNING_FLAGS.ALL}"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_ADDITIONAL_COMPILE_OPTIONS = Const.ENV_KEY_JANTJE_START + "EXTRA.COMPILE"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_ADDITIONAL_C_COMPILE_OPTIONS = Const.ENV_KEY_JANTJE_START
			+ "EXTRA.C.COMPILE"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_ADDITIONAL_CPP_COMPILE_OPTIONS = Const.ENV_KEY_JANTJE_START
			+ "EXTRA.CPP.COMPILE"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_WARNING_LEVEL = Const.ENV_KEY_JANTJE_START + "WARNING_LEVEL"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_SIZE_COMMAND = Const.ERASE_START + "ALT_SIZE_COMMAND"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_SIZE_SWITCH = Const.ENV_KEY_JANTJE_START + "SIZE.SWITCH"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_ASSEMBLY_COMPILE_OPTIONS = Const.ENV_KEY_JANTJE_START + "EXTRA.ASSEMBLY"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_ARCHIVE_COMPILE_OPTIONS = Const.ENV_KEY_JANTJE_START + "EXTRA.ARCHIVE"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_LINK_COMPILE_OPTIONS = Const.ENV_KEY_JANTJE_START + "EXTRA.LINK"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_ALL_COMPILE_OPTIONS = Const.ENV_KEY_JANTJE_START + "EXTRA.ALL"; //$NON-NLS-1$

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
				this.my_C_andCPP_CompileOptions = var.getValue();
			var = contribEnv.getVariable(ENV_KEY_JANTJE_ADDITIONAL_C_COMPILE_OPTIONS, confDesc);
			if (var != null)
				this.my_C_CompileOptions = var.getValue();
			var = contribEnv.getVariable(ENV_KEY_JANTJE_ADDITIONAL_CPP_COMPILE_OPTIONS, confDesc);
			if (var != null)
				this.my_CPP_CompileOptions = var.getValue();
			var = contribEnv.getVariable(ENV_KEY_JANTJE_ASSEMBLY_COMPILE_OPTIONS, confDesc);
			if (var != null)
				this.my_Assembly_CompileOptions = var.getValue();
			var = contribEnv.getVariable(ENV_KEY_JANTJE_ARCHIVE_COMPILE_OPTIONS, confDesc);
			if (var != null)
				this.my_Archive_CompileOptions = var.getValue();
			var = contribEnv.getVariable(ENV_KEY_JANTJE_LINK_COMPILE_OPTIONS, confDesc);
			if (var != null)
				this.my_Link_CompileOptions = var.getValue();
			var = contribEnv.getVariable(ENV_KEY_JANTJE_ALL_COMPILE_OPTIONS, confDesc);
			if (var != null)
				this.my_All_CompileOptions = var.getValue();

		}
	}

	public boolean isWarningLevel() {
		return this.myWarningLevel.booleanValue();
	}

	public void setWarningLevel(boolean myWarningLevel) {
		this.myWarningLevel = new Boolean(myWarningLevel);
	}
	
	public boolean isParallelBuildEnabled() {
		return myEnableParallelBuild;
	}
	
	public void setEnableParallelBuild(boolean parrallelBuild) {
		this.myEnableParallelBuild = parrallelBuild;
	}

	public boolean isAlternativeSizeCommand() {
		return this.myAlternativeSizeCommand;
	}

	public void setAlternativeSizeCommand(boolean alternativeSizeCommand) {
		this.myAlternativeSizeCommand = alternativeSizeCommand;
	}

	public String get_CPP_CompileOptions() {
		return this.my_CPP_CompileOptions;
	}

	public void set_CPP_CompileOptions(String new_CPP_CompileOptions) {
		this.my_CPP_CompileOptions = new_CPP_CompileOptions;
	}

	public String get_C_CompileOptions() {
		return this.my_C_CompileOptions;
	}

	public void set_C_CompileOptions(String new_C_CompileOptions) {
		this.my_C_CompileOptions = new_C_CompileOptions;
	}

	public String get_C_andCPP_CompileOptions() {
		return this.my_C_andCPP_CompileOptions;
	}

	public void set_C_andCPP_CompileOptions(String new_C_andCPP_CompileOptions) {
		this.my_C_andCPP_CompileOptions = new_C_andCPP_CompileOptions;
	}
	public String get_Assembly_CompileOptions() {
		return this.my_Assembly_CompileOptions;
	}

	public void set_Assembly_CompileOptions(String my_Assembly_CompileOptions) {
		this.my_Assembly_CompileOptions = my_Assembly_CompileOptions;
	}

	public String get_Archive_CompileOptions() {
		return this.my_Archive_CompileOptions;
	}

	public void set_Archive_CompileOptions(String my_Archive_CompileOptions) {
		this.my_Archive_CompileOptions = my_Archive_CompileOptions;
	}

	public String get_Link_CompileOptions() {
		return this.my_Link_CompileOptions;
	}

	public void set_Link_CompileOptions(String my_Link_CompileOptions) {
		this.my_Link_CompileOptions = my_Link_CompileOptions;
	}

	public String get_All_CompileOptions() {
		return this.my_All_CompileOptions;
	}

	public void set_All_CompileOptions(String my_All_CompileOptions) {
		this.my_All_CompileOptions = my_All_CompileOptions;
	}

	/**
	 * save the compilation options in this configuration description.
	 *
	 * @param configuration
	 *            must be a valid configuration description
	 */
	public void save(ICConfigurationDescription configuration) {
		CompileOptions curOptions=new CompileOptions(configuration);
		if(needsDirtyFlag(curOptions)) {
			IProject project = configuration.getProjectDescription().getProject();
			Helpers.setDirtyFlag(project, configuration);
		}
		IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
		IEnvironmentVariable var = new EnvironmentVariable(ENV_KEY_JANTJE_WARNING_LEVEL,
				this.myWarningLevel.toString());
		contribEnv.addVariable(var, configuration);

		if (this.isWarningLevel()) {
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
				this.my_C_andCPP_CompileOptions);
		contribEnv.addVariable(var, configuration);
		var = new EnvironmentVariable(ENV_KEY_JANTJE_ADDITIONAL_CPP_COMPILE_OPTIONS,
				this.my_CPP_CompileOptions);
		contribEnv.addVariable(var, configuration);
		var = new EnvironmentVariable(ENV_KEY_JANTJE_ADDITIONAL_C_COMPILE_OPTIONS, this.my_C_CompileOptions);
		contribEnv.addVariable(var, configuration);


		var = new EnvironmentVariable(ENV_KEY_JANTJE_ASSEMBLY_COMPILE_OPTIONS,
				this.my_Assembly_CompileOptions);
		contribEnv.addVariable(var, configuration);
		var = new EnvironmentVariable(ENV_KEY_JANTJE_ARCHIVE_COMPILE_OPTIONS,
				this.my_Archive_CompileOptions);
		contribEnv.addVariable(var, configuration);
		var = new EnvironmentVariable(ENV_KEY_JANTJE_LINK_COMPILE_OPTIONS,
				this.my_Link_CompileOptions);
		contribEnv.addVariable(var, configuration);
		var = new EnvironmentVariable(ENV_KEY_JANTJE_ALL_COMPILE_OPTIONS,
				this.my_All_CompileOptions);
		contribEnv.addVariable(var, configuration);



	}

	private boolean needsDirtyFlag(CompileOptions curOptions) {
		// ignore myWarningLevel
		//ignore myAlternativeSizeCommand
		if( !this.my_CPP_CompileOptions.equals(curOptions.get_CPP_CompileOptions())){
			return true;
		}
		if( !this.my_C_CompileOptions.equals(curOptions.get_C_CompileOptions())){
			return true;
		}
		if( !this.my_C_andCPP_CompileOptions.equals(curOptions.get_C_andCPP_CompileOptions())){
			return true;
		}
		if( !this.my_Assembly_CompileOptions.equals(curOptions.get_Assembly_CompileOptions())){
			return true;
		}
		if( !this.my_Archive_CompileOptions.equals(curOptions.get_Archive_CompileOptions())){
			return true;
		}
		if( !this.my_Link_CompileOptions.equals(curOptions.get_Link_CompileOptions())){
			return true;
		}
		if( !this.my_All_CompileOptions.equals(curOptions.get_All_CompileOptions())){
			return true;
		}

		return false;
	}

}
