package io.sloeber.core.eclipseIntegrations;

import static io.sloeber.core.api.Const.*;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IDynamicVariableResolver;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;

import io.sloeber.core.Messages;

public class CDT_EnvironmentVariableResolver implements IDynamicVariableResolver {

    @Override
    public String resolveValue(IDynamicVariable variable, String varName) throws CoreException {
        try {
            ICConfigurationDescription confDesc = getConfigurationDescription();
            return getBuildEnvironmentVariable(confDesc, varName);

        } catch (@SuppressWarnings("unused") Exception dontCare) {
            Status iStatus = new Status(IStatus.ERROR, PLUGIN_ID, Messages.projectNotFoundInGUI);
            throw new CoreException(iStatus);
        }
    }

    /**
     * Find the active configuration of the project selected in the project manager
     * 
     * @return The configuration description or null if not found
     */
    private static ICConfigurationDescription getConfigurationDescription() {
        IProject project = getGUISelectedProject();

        if (project != null && project.exists() && project.isOpen()) {
            CCorePlugin cCorePlugin = CCorePlugin.getDefault();
            ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(project);
            return prjCDesc.getActiveConfiguration();
        }
        return null;
    }

    static private String getBuildEnvironmentVariable(ICConfigurationDescription configurationDescription,
            String envName) {
        try {
            IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
            return envManager.getVariable(envName, configurationDescription, true).getValue();
        } catch (@SuppressWarnings("unused") Exception e) {// ignore all errors and return the default value
        }
        return new String();
    }

    /**
     * Returns the project selected GUI. Uses the ${selected_resource_path} variable
     * to determine the selected resource. This variable is provided by the debug.ui
     * plug-in. Selected resource resolution is only available when the debug.ui
     * plug-in is present.
     *
     * @return project related to selected resource in the gui if no project is
     *         found null is returned
     * 
     */
    private static IProject getGUISelectedProject() {
        try {
            IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
            IWorkspaceRoot workSpaceRoot = ResourcesPlugin.getWorkspace().getRoot();
            String pathString;

            pathString = manager.performStringSubstitution("${selected_resource_path}"); //$NON-NLS-1$

            return workSpaceRoot.findMember(new Path(pathString)).getProject();
        } catch (@SuppressWarnings("unused") CoreException e) {
            return null;
        }

    }

}
