package io.sloeber.autoBuild.api;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import io.sloeber.autoBuild.core.Messages;
import io.sloeber.autoBuild.extensionPoint.providers.BuildRunnerForMake;
import io.sloeber.autoBuild.extensionPoint.providers.InternalBuildRunner;
import io.sloeber.autoBuild.integration.AutoBuildProjectGenerator;

public class AutoBuildProject {
    public static final String BUILDER_ID = "io.sloeber.autoBuild.AutoMakeBuilder"; //$NON-NLS-1$
    public static final String ARGS_BUILDER_KEY = "The key to specify the value is a builder key"; //$NON-NLS-1$
    public static final String ARGS_TARGET_KEY = "The key to specify the value is the target to build"; //$NON-NLS-1$
    public static final String ARGS_INTERNAL_BUILDER_KEY = InternalBuildRunner.RUNNER_NAME;
    public static final String ARGS_MAKE_BUILDER_KEY = BuildRunnerForMake.RUNNER_NAME;
    public static final String ARGS_CONFIGS_KEY = "The names of the configurations to build"; //$NON-NLS-1$

    /**
     * 
     * @param projectName
     *            The name of the project
     * @param extensionPointID
     *            The ID of the extension point that describes the project to be
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
     * @param needsMoreWork
     *            if true the projectDescription will be marked as created
     *            if false you will need to call setCdtProjectCreated and
     *            setProjectDescription
     * @param monitor
     * 
     * @return the created project
     */
    public static IProject createProject(String projectName, String extensionPointID, String extensionID,
            String projectTypeID, String natureID, ICodeProvider codeProvider, boolean needsMoreWork,
            IProgressMonitor monitor) {
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
            theGenerator.setNeedsMoreWork(needsMoreWork);
            theGenerator.generate(internalMonitor);
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return theGenerator.getProject();
    }

}
