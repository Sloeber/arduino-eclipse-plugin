package io.sloeber.core.toolchain;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.envvar.IBuildEnvironmentVariable;
import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;
import org.eclipse.cdt.managedbuilder.envvar.IEnvironmentVariableProvider;

public class SloeberConfigurationVariableSupplier implements IConfigurationEnvironmentVariableSupplier {
    private static Map<String, Map<String, BuildEnvironmentVariable>> myValues = new HashMap<>();

    @Override
    public IBuildEnvironmentVariable getVariable(String variableName, IConfiguration configuration,
            IEnvironmentVariableProvider provider) {
        Map<String, BuildEnvironmentVariable> curConfigVars = myValues.get(configuration.getName());
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
        return curConfigVars.get(variableName);
    }

    @Override
    public IBuildEnvironmentVariable[] getVariables(IConfiguration configuration,
            IEnvironmentVariableProvider provider) {
        Map<String, BuildEnvironmentVariable> curConfigVars = myValues.get(configuration.getName());
        if (null == curConfigVars) {
            return null;
        }
        return curConfigVars.values().toArray(new BuildEnvironmentVariable[curConfigVars.size()]);
    }


    public static void setEnvVars(IConfiguration configuration, Map<String, String> values) {
        Map<String, BuildEnvironmentVariable> curConfigVars = myValues.get(configuration.getName());
        if (null == curConfigVars) {
            curConfigVars = new HashMap<>();
        }
        else {
            curConfigVars.clear();
        }
        for (Entry<String, String> curVar : values.entrySet()) {
            curConfigVars.put(curVar.getKey(), new BuildEnvironmentVariable(curVar.getKey(), curVar.getValue()));
        }
        myValues.put(configuration.getName(), curConfigVars);
    }
}
