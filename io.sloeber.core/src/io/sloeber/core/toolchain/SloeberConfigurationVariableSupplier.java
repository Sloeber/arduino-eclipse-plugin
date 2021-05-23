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

    private static SloeberProject getSloeberProject(IConfiguration configuration) {
        ICConfigurationDescription confDesc = ManagedBuildManager.getDescriptionForConfiguration(configuration);
        ICProjectDescription projDesc = confDesc.getProjectDescription();
        IProject project = projDesc.getProject();
        return SloeberProject.getSloeberProject(project, false);
    }

    @Override
    public IBuildEnvironmentVariable getVariable(String variableName, IConfiguration configuration,
            IEnvironmentVariableProvider provider) {
        String ret = null;
        SloeberProject sloeberProject = getSloeberProject(configuration);
        if (sloeberProject == null) {
            return null;
        }
        Map<String, String> boardEnvVars = sloeberProject.getEnvironmentVariables(configuration.getName());
        if (null != boardEnvVars) {
            ret = boardEnvVars.get(variableName);
        }
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
        SloeberProject sloeberProject = getSloeberProject(configuration);
        if (sloeberProject != null) {

            Map<String, String> boardEnvVars = sloeberProject.getEnvironmentVariables(configuration.getName());
            if (boardEnvVars != null) {
                retVars.putAll(boardEnvVars);
            }
        }

        IBuildEnvironmentVariable[] ret = new BuildEnvironmentVariable[retVars.size()];
        int i = 0;
        for (Entry<String, String> curVar : retVars.entrySet()) {
            ret[i++] = new BuildEnvironmentVariable(curVar.getKey(), curVar.getValue());
        }
        return ret;
    }

}
