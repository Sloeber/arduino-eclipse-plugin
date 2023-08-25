package io.sloeber.core.toolchain;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;

import io.sloeber.autoBuild.api.IEnvironmentVariableProvider;
import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.SloeberProject;

public class SloeberConfigurationVariableSupplier implements IEnvironmentVariableProvider {

    private static SloeberProject getSloeberProject(ICConfigurationDescription confDesc) {
        ICProjectDescription projDesc = confDesc.getProjectDescription();
        IProject project = projDesc.getProject();
        return SloeberProject.getSloeberProject(project);
    }

    @Override
    public IEnvironmentVariable getVariable(String variableName, ICConfigurationDescription configuration,
            boolean resolveMacros) {
        String ret = null;
        if (configuration == null) {
            return null;
        }
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
            ret = BoardsManager.getEnvironmentVariables().get(variableName);
        }
        if (ret == null) {
            return null;
        }
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
        SloeberProject sloeberProject = getSloeberProject(configuration);
        if (sloeberProject != null) {

            Map<String, String> boardEnvVars = sloeberProject.getEnvironmentVariables(configuration.getName());
            if (boardEnvVars != null) {
                retVars.putAll(boardEnvVars);
            }
        }

        EnvironmentVariable[] ret = new EnvironmentVariable[retVars.size()];
        int i = 0;
        for (Entry<String, String> curVar : retVars.entrySet()) {
            ret[i++] = new EnvironmentVariable(curVar.getKey(), curVar.getValue());
        }
        return ret;
    }

}
