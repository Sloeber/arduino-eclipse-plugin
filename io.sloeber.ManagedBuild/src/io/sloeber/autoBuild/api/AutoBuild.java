package io.sloeber.autoBuild.api;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

public class AutoBuild {
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
}
