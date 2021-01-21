package io.sloeber.core.toolchain;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.envvar.IBuildEnvironmentVariable;
import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;
import org.eclipse.cdt.managedbuilder.envvar.IEnvironmentVariableProvider;
import org.eclipse.core.resources.IProject;

import io.sloeber.core.api.PackageManager;
import io.sloeber.core.api.SloeberProject;

public class SloeberConfigurationVariableSupplier implements IConfigurationEnvironmentVariableSupplier {
    // variables per configuration
    private Map<String, Map<String, String>> myConfigValues = new HashMap<>();

    @Override
    public IBuildEnvironmentVariable getVariable(String variableName, IConfiguration configuration,
            IEnvironmentVariableProvider provider) {
        initializeIfNotYetDone(configuration);
        Map<String, String> curConfigVars = myConfigValues.get(configuration.getName());
        if (null == curConfigVars) {
            return null;
            // This should only happen if a config is existing Sloeber does not know about
            // because we configured the sloeber project above
            // So this should not happen
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
        initializeIfNotYetDone(configuration);
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

    private void initializeIfNotYetDone(IConfiguration configuration) {
        if (!myConfigValues.isEmpty()) {
            // we have some data; asume it is correct
            return;
        }
        ICConfigurationDescription confDesc = ManagedBuildManager.getDescriptionForConfiguration(configuration);
        ICProjectDescription projDesc = confDesc.getProjectDescription();
        IProject project = projDesc.getProject();
        SloeberProject sloeberProject = SloeberProject.getSloeberProject(project, false);
        sloeberProject.configure();
    }
}
