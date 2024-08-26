package io.sloeber.core.toolchain;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;

import io.sloeber.arduinoFramework.api.BoardsManager;
import io.sloeber.autoBuild.api.IEnvironmentVariableProvider;
import io.sloeber.core.api.ISloeberConfiguration;

public class SloeberConfigurationVariableSupplier implements IEnvironmentVariableProvider {

    @Override
    public IEnvironmentVariable getVariable(String variableName, ICConfigurationDescription configuration,
            boolean resolveMacros) {
        String ret = null;
        if (configuration == null) {
            return null;
        }
        ISloeberConfiguration sloeberCfg = ISloeberConfiguration.getConfig(configuration);
        if (sloeberCfg != null) {
            Map<String, String> sloeberCfgEnvVars = sloeberCfg.getEnvironmentVariables();
            if (null != sloeberCfgEnvVars) {
                ret = sloeberCfgEnvVars.get(variableName);
            }
        }
        if (ret == null) {
            // when the configuration doesn't hold the env var maybe the workbench does
            ret = BoardsManager.getEnvironmentVariables().get(variableName);
        }
        if (ret == null) {
            return null;
        }
        //TOFIX I should take the resolveMacros parameter into account
        return new EnvironmentVariable(variableName, ret);
    }

    @Override
    public IEnvironmentVariable[] getVariables(ICConfigurationDescription configuration, boolean resolveMacros) {
        if (configuration == null) {
            return new IEnvironmentVariable[0];
        }

        Map<String, String> retVars = new HashMap<>();
        Map<String, String> workbenchVars = BoardsManager.getEnvironmentVariables();
        if (workbenchVars != null) {
            retVars.putAll(workbenchVars);
        }

        ISloeberConfiguration sloeberCfg = ISloeberConfiguration.getConfig(configuration);
        if (sloeberCfg != null) {
            Map<String, String> sloeberCfgEnvVars = sloeberCfg.getEnvironmentVariables();
            if (sloeberCfgEnvVars != null) {
                retVars.putAll(sloeberCfgEnvVars);
            }
        }

        EnvironmentVariable[] ret = new EnvironmentVariable[retVars.size()];
        int i = 0;
        for (Entry<String, String> curVar : retVars.entrySet()) {
            //TOFIX Take resolveMacros into account here
            ret[i++] = new EnvironmentVariable(curVar.getKey(), curVar.getValue());
        }
        return ret;
    }

}
