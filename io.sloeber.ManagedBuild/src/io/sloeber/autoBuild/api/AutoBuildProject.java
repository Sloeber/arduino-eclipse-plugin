package io.sloeber.autoBuild.api;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.autoBuild.integration.AutoBuildProjectGenerator;

public class AutoBuildProject {
    /**
     * 
     * @param projectName
     *            The name of the project
     * @param extensionPointID
     *            The ID of the extention point that describes the project to be
     *            created
     * @param extensionID
     *            The ID of the extension defined by the extensionpoindID
     * @param projectTypeID
     *            the projectTypeID of type extension ID
     * @param natureID
     *            use CCProjectNature.CC_NATURE_ID for C++ project; all other values
     *            are currently ignored
     * @param codeProvider
     *            a provider that gives the code to add to the project
     * @param monitor
     * 
     * @return the created project
     */
    public static IProject createProject(String projectName, String extensionPointID, String extensionID,
            String projectTypeID, String natureID, ICodeProvider codeProvider, IProgressMonitor monitor) {
        AutoBuildProjectGenerator theGenerator = new AutoBuildProjectGenerator();
        try {
            IProgressMonitor internalMonitor = monitor;
            if (internalMonitor == null) {
                internalMonitor = new NullProgressMonitor();
            }
            theGenerator.setExtentionPointID(extensionPointID);
            theGenerator.setExtentionID(extensionID);
            theGenerator.setProjectTypeID(projectTypeID);
            theGenerator.setProjectName(projectName);
            theGenerator.setCodeProvider(codeProvider);
            theGenerator.setNatureID(natureID);
            theGenerator.generate(internalMonitor);
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return theGenerator.getProject();
    }

    public static AutoBuildConfigurationDescription getAutoBuildConfig(ICConfigurationDescription confDesc) {
        return AutoBuildConfigurationDescription.getFromConfig(confDesc);
    }
}
