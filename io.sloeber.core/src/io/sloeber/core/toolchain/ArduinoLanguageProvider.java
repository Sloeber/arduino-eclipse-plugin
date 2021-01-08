package io.sloeber.core.toolchain;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
import org.eclipse.cdt.managedbuilder.language.settings.providers.GCCBuiltinSpecsDetector;

import io.sloeber.core.common.Const;

@SuppressWarnings({"nls","unused"})
public class ArduinoLanguageProvider extends GCCBuiltinSpecsDetector{

	

	@Override
	protected String getCompilerCommand(String languageId) {
        String ret = new String();

		ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(currentProject);
		if (prjDesc == null)
            return ret;
		ICConfigurationDescription confDesc = prjDesc.getActiveConfiguration();

        String codanVarName = new String();
		if (languageId.equals("org.eclipse.cdt.core.gcc")) {
            codanVarName = Const.CODAN_C_to_O;
		} else if (languageId.equals("org.eclipse.cdt.core.g++")) {
            codanVarName = Const.CODAN_CPP_to_O;
		} else {
			ManagedBuilderCorePlugin.error(
					"Unable to find compiler command for language " + languageId + " in toolchain=" + getToolchainId());
		}

		try {
            IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
            ret = envManager.getVariable(codanVarName, confDesc, false).getValue();
		} catch (Exception e) {
            ret = new String();
		}


        return ret;
	}

}