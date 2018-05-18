package io.sloeber.core.toolchain;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
import org.eclipse.cdt.managedbuilder.language.settings.providers.GCCBuiltinSpecsDetector;

import io.sloeber.core.api.CompileOptions;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;

@SuppressWarnings({"nls","unused"})
public class ArduinoLanguageProvider extends GCCBuiltinSpecsDetector{

	

	@Override
	protected String getCompilerCommand(String languageId) {
		String compilerCommand = new String();

		ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(currentProject);
		if (prjDesc == null)
			return compilerCommand;

		IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		ICConfigurationDescription confDesc = prjDesc.getActiveConfiguration();

		String recipeKey = new String();
		String extraOptions = new String();
		CompileOptions compileOptions = new CompileOptions(confDesc);
		if (languageId.equals("org.eclipse.cdt.core.gcc")) {
			recipeKey = Common.get_ENV_KEY_RECIPE(Const.ACTION_C_to_O);
			extraOptions = compileOptions.get_C_CompileOptions();
		} else if (languageId.equals("org.eclipse.cdt.core.g++")) {
			recipeKey = Common.get_ENV_KEY_RECIPE(Const.ACTION_CPP_to_O);
			extraOptions = compileOptions.get_CPP_CompileOptions();
		} else {
			ManagedBuilderCorePlugin.error(
					"Unable to find compiler command for language " + languageId + " in toolchain=" + getToolchainId());
		}
		extraOptions = extraOptions + " " + compileOptions.get_C_andCPP_CompileOptions() + " "
				+ compileOptions.get_All_CompileOptions();
		try {
			compilerCommand = envManager.getVariable(recipeKey + Const.DOT + "1", confDesc, false).getValue();
			compilerCommand = compilerCommand
					+ envManager.getVariable(recipeKey + Const.DOT + "2", confDesc, false).getValue();
			compilerCommand = compilerCommand
					+ envManager.getVariable(recipeKey + Const.DOT + "3", confDesc, false).getValue();
		} catch (Exception e) {
			compilerCommand = new String();
		}

		compilerCommand = compilerCommand + ' ' + extraOptions;

		return compilerCommand.replaceAll(" -o ", " ");
	}

}