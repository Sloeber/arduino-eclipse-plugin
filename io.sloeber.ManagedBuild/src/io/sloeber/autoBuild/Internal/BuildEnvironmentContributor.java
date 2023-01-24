/*******************************************************************************
 * This class is the entry point for the environment variables provided by this plugin
 * The plugin= autoBuild
 * 
 * With entry point I mean: this class is registered in/called/used by CDT
 * 
 * This class should know all environment variable classes defined 
 * in the extension point and directly call them
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentContributor;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IProject;

import io.sloeber.autoBuild.api.IEnvironmentVariableSupplier;
import io.sloeber.autoBuild.integration.BuildBuildData;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IManagedProject;
import io.sloeber.schema.api.IProjectType;

public class BuildEnvironmentContributor implements IEnvironmentContributor {
    private BuildBuildData fBuildData;
    private IConfiguration fCfg;
    private ICConfigurationDescription fCfgDes;
    IEnvironmentVariableSupplier myProjectEnvironmentVariableProvider = null;
    IEnvironmentVariableSupplier myConfigurationEnvironmentVariableProvider = null;

    public BuildEnvironmentContributor(BuildBuildData buildData) {
        fBuildData = buildData;

        fCfg = fBuildData.getConfiguration();
        fCfgDes = fBuildData.getCdtConfigurationDescription();

        IProjectType pType = fCfg.getProjectType();
        if (pType != null) {
            myProjectEnvironmentVariableProvider = pType.getEnvironmentVariableSupplier();
        }
        myConfigurationEnvironmentVariableProvider = fCfg.getEnvironmentVariableSupplier();
    }

    @Override
    public IEnvironmentVariable getVariable(String name, IEnvironmentVariableManager provider) {
        return internalGetVariables(provider).get(name);
    }

    @Override
    public IEnvironmentVariable[] getVariables(IEnvironmentVariableManager provider) {
        return internalGetVariables(provider).values().toArray(new EnvironmentVariable[0]);
    }

    private Map<String, IEnvironmentVariable> internalGetVariables(IEnvironmentVariableManager provider) {
        Map<String, IEnvironmentVariable> allVars = new HashMap<>();
        if (myProjectEnvironmentVariableProvider != null) {
            allVars.putAll(myProjectEnvironmentVariableProvider.getVariables(fCfg));
        }
        if (myConfigurationEnvironmentVariableProvider != null) {
            allVars.putAll(myConfigurationEnvironmentVariableProvider.getVariables(fCfg));
        }

        return allVars;
    }

}
