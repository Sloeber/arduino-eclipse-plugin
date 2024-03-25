/*******************************************************************************
 * This class is the entry point for the environment variables provided by this plugin
 * The plugin= autoBuild
 *
 * With entry point I mean: this class is registered in/called/used by CDT
 *
 * This class should know all environment variable classes defined
 * in the extension point and directly call them
 *******************************************************************************/
package io.sloeber.autoBuild.integration;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.envvar.IEnvironmentContributor;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import io.sloeber.autoBuild.api.IEnvironmentVariableProvider;
import io.sloeber.schema.api.IProjectType;

public class BuildEnvironmentContributor implements IEnvironmentContributor {
    IEnvironmentVariableProvider myProjectEnvironmentVariableProvider = null;
    IEnvironmentVariableProvider myConfigurationEnvironmentVariableProvider = null;
    AutoBuildConfigurationDescription myAutoData;

    public BuildEnvironmentContributor(AutoBuildConfigurationDescription autoData) {
        myAutoData = autoData;
        IProjectType pType = myAutoData.getProjectType();
        if (pType != null) {
            myProjectEnvironmentVariableProvider = pType.getEnvironmentVariableProvider();
            myConfigurationEnvironmentVariableProvider = pType.getToolChain()
                    .getEnvironmentVariableProvider();
        }

    }

    @Override
    public IEnvironmentVariable getVariable(String name, IEnvironmentVariableManager provider) {

        IEnvironmentVariable environmentVariable = null;
        if (myConfigurationEnvironmentVariableProvider != null) {
            environmentVariable = myConfigurationEnvironmentVariableProvider.getVariable(name,
                    myAutoData.getCdtConfigurationDescription(), false);
            if (environmentVariable == null && myProjectEnvironmentVariableProvider != null) {
                environmentVariable = myProjectEnvironmentVariableProvider.getVariable(name,
                        myAutoData.getCdtConfigurationDescription(), false);
            }
        }

        return environmentVariable;
    }

    @Override
    public IEnvironmentVariable[] getVariables(IEnvironmentVariableManager provider) {
        Set<IEnvironmentVariable> allVars = new HashSet<>();
        if (myProjectEnvironmentVariableProvider != null) {
            Collections.addAll(allVars, myProjectEnvironmentVariableProvider
                    .getVariables(myAutoData.getCdtConfigurationDescription(), false));
        }
        if (myConfigurationEnvironmentVariableProvider != null) {
            Collections.addAll(allVars, myConfigurationEnvironmentVariableProvider
                    .getVariables(myAutoData.getCdtConfigurationDescription(), false));
        }

        return allVars.toArray(new IEnvironmentVariable[allVars.size()]);
    }

}
