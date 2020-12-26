package io.sloeber.core.toolchain;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.envvar.IBuildEnvironmentVariable;
import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;
import org.eclipse.cdt.managedbuilder.envvar.IEnvironmentVariableProvider;

import io.sloeber.core.api.PackageManager;

public class SloeberConfigurationVariableSupplier implements IConfigurationEnvironmentVariableSupplier {
    // variables per configuration
    private Map<String, Map<String, String>> myConfigValues = new HashMap<>();

    @Override
    public IBuildEnvironmentVariable getVariable(String variableName, IConfiguration configuration,
            IEnvironmentVariableProvider provider) {
        Map<String, String> curConfigVars = myConfigValues.get(configuration.getName());
        if (null == curConfigVars) {
            return null;
            // maybe the project is not yet loaded by Sloeber.
            // try to load and retry
            // ICConfigurationDescription confDesc =
            // ManagedBuildManager.getDescriptionForConfiguration(configuration);
            // IProject project = confDesc.getProjectDescription().getProject();
            // ArduinoProjectDescription.getArduinoProjectDescription(project);
            // curConfigVars = myValues.get(configuration.getName());
            // if (null == curConfigVars) {
            // return null;
            // }
        }
        String ret = curConfigVars.get(variableName);
        if (ret == null) {
            // when the configuration doesn't hold the env var maybe the workbench does
            ret = PackageManager.getEnvironmentVariables().get(variableName);
        }
        if (ret == null) {
            return null;
        }
        return new BuildEnvironmentVariable(variableName, ret);
    }

    @Override
    public IBuildEnvironmentVariable[] getVariables(IConfiguration configuration,
            IEnvironmentVariableProvider provider) {
        Map<String, String> retVars = new HashMap<>();
        Map<String, String> workbenchVars = PackageManager.getEnvironmentVariables();
        if (workbenchVars != null) {
            retVars.putAll(workbenchVars);
        }

        Map<String, String> curConfigVars = myConfigValues.get(configuration.getName());
        if (curConfigVars != null) {
            retVars.putAll(curConfigVars);
        }

        IBuildEnvironmentVariable[] ret = new BuildEnvironmentVariable[retVars.size()];
        int i = 0;
        for (Entry<String, String> curVar : retVars.entrySet()) {
            ret[i++] = new BuildEnvironmentVariable(curVar.getKey(), curVar.getValue());
        }
        return ret;
    }

    public void setEnvVars(IConfiguration configuration, Map<String, String> values) {
        myConfigValues.put(configuration.getName(), values);
    }
}
