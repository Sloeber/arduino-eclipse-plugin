package io.sloeber.autoBuild.regression;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.osgi.framework.Bundle;

import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;

@SuppressWarnings("nls")
public class Shared {
    private static boolean deleteProjects = true;
    private static boolean closeFailedProjects = false;
    private static boolean closeProjects = true;

    public static void setDeleteProjects(boolean deleteProjects) {
        Shared.deleteProjects = deleteProjects;
    }

    public static void setCloseProjects(boolean close) {
        Shared.closeProjects = close;
    }

    public static void setCloseFailedProjects(boolean closeFailedProjects) {
        Shared.closeFailedProjects = closeFailedProjects;
    }

    public static boolean isCloseFailedProjects() {
        return closeFailedProjects;
    }

    private static void doFail(IProject project, String error) {
        if (closeFailedProjects) {
            try {
                project.close(null);
            } catch (CoreException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        fail(error);
    }

    public static void hasProjectBuildErrors(IProject project) throws CoreException {
        IMarker[] markers = project.findMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
        for (IMarker marker : markers) {
            if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
                doFail(project, "The project contains error");
            }
        }
    }

    public static void checkIfConfigurationBuildCorrectly(IProject project,
            IAutoBuildConfigurationDescription autoBuild, Boolean shouldMakefileExists) {
        IFolder buildRootFolder = autoBuild.getBuildFolder();
        String projName = project.getName();

        //check to see if the makefile is present or not
        if (shouldMakefileExists != null) {
            if (shouldMakefileExists.booleanValue() && (!buildRootFolder.getFile("makefile").exists())) {
                doFail(project, "makefile " + buildRootFolder.getFile("makefile") + " should exist but does not ");
            }
            if ((!shouldMakefileExists.booleanValue()) && buildRootFolder.getFile("makefile").exists()) {
                doFail(project, "makefile " + buildRootFolder.getFile("makefile") + " should not exist but does");
            }
        }

        //check to see if any of the expected outputfiles exists
        String[] validOutputs = { projName + ".elf", projName + ".bin", projName + ".hex", projName + ".exe",
                "lib_" + projName + ".so", projName + ".so", "lib_" + projName + ".a", projName + ".dll",
                "application.axf" };
        for (String validOutput : validOutputs) {
            if (buildRootFolder.getFile(validOutput).exists()) {
                return;
            }
        }

        doFail(project, "The build did not produce a file that is considered a targetfile in build folder "
                + buildRootFolder.toString());
    }

    public static void waitForAllJobsToFinish() throws InterruptedException {
        Thread.sleep(1000);
        IJobManager jobMan = Job.getJobManager();
        int counter = 0;
        while ((!jobMan.isIdle()) && (counter++ < 120)) {
            Thread.sleep(500);
            // If you have to wait for counter it may mean you are
            // runnning the test in the gui thread
        }
        // As nothing is running now we can start installing
    }

    public static IPath getTemplateFolder(String templateName) throws Exception {
        Bundle bundle = Platform.getBundle("io.sloeber.tests");
        Path path = new Path("src/templates/" + templateName);
        URL fileURL = FileLocator.find(bundle, path, null);
        URL resolvedFileURL = FileLocator.toFileURL(fileURL);
        return new Path(resolvedFileURL.toURI().getPath());
    }

    public static IPath getprojectZip(String zipFileName) throws Exception {
        Bundle bundle = Platform.getBundle("io.sloeber.tests");
        Path path = new Path("src/projects/" + zipFileName);
        URL fileURL = FileLocator.find(bundle, path, null);
        URL resolvedFileURL = FileLocator.toFileURL(fileURL);
        return new Path(resolvedFileURL.toURI().getPath());
    }

    public static void buildProjectUsingActivConfig(IProject iProject, String builderName) throws Exception {
        ICProjectDescription cProjectDesc = CCorePlugin.getDefault().getProjectDescription(iProject, true);
        for (ICConfigurationDescription curConfig : cProjectDesc.getConfigurations()) {
            cProjectDesc.setActiveConfiguration(curConfig);
            CCorePlugin.getDefault().setProjectDescription(iProject, cProjectDesc);
            build(iProject, builderName, null);
        }
    }

    public static void buildAndVerifyProjectUsingActivConfig(IProject project, Boolean shouldMakefileExists)
            throws Exception {
        //buildAndVerifyProjectUsingActivConfigWorking(project, shouldMakefileExists);
        buildAndVerifyProjectUsingActivConfigNotWorking(project, shouldMakefileExists);
    }

    private static void buildAndVerifyProjectUsingActivConfigWorking(IProject project, Boolean shouldMakefileExists)
            throws Exception {
        ICProjectDescription cProjectDescread = CCorePlugin.getDefault().getProjectDescription(project, false);
        for (ICConfigurationDescription curConfig : cProjectDescread.getConfigurations()) {
            ICProjectDescription cProjectDesc = CCorePlugin.getDefault().getProjectDescription(project, true);
            cProjectDesc.setActiveConfiguration(cProjectDesc.getConfigurationByName(curConfig.getName()));
            CCorePlugin.getDefault().setProjectDescription(project, cProjectDesc);
            project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
            verifyConfig(project, IAutoBuildConfigurationDescription.getConfig(curConfig), shouldMakefileExists);
        }
    }

    private static void buildAndVerifyProjectUsingActivConfigNotWorking(IProject project, Boolean shouldMakefileExists)
            throws Exception {
        ICProjectDescription cProjectDesc = CCorePlugin.getDefault().getProjectDescription(project, true);
        for (ICConfigurationDescription curConfig : cProjectDesc.getConfigurations()) {
            cProjectDesc.setActiveConfiguration(curConfig);
            CCorePlugin.getDefault().setProjectDescription(project, cProjectDesc);
            project.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
            verifyConfig(project, IAutoBuildConfigurationDescription.getConfig(curConfig), shouldMakefileExists);
        }
    }

    /**
     * clean all the configuration by looping over all the configurations and
     * setting them active
     * and then call a clean build
     * 
     * @param iProject
     * @throws CoreException
     */
    public static void cleanProject(IProject iProject) throws CoreException {
        ICProjectDescription cProjectDesc = CCorePlugin.getDefault().getProjectDescription(iProject, true);
        //clean all configurations and verify clean has been done properly
        for (ICConfigurationDescription curConfig : cProjectDesc.getConfigurations()) {
            cProjectDesc.setActiveConfiguration(curConfig);
            CCorePlugin.getDefault().setProjectDescription(iProject, cProjectDesc);

            IAutoBuildConfigurationDescription autoConf = IAutoBuildConfigurationDescription.getConfig(curConfig);
            cleanConfiguration(autoConf);
        }
    }

    /**
     * clean all the configuration by looping over all the configurations and
     * setting them active
     * and then call a clean build
     * 
     * @param iProject
     * @throws CoreException
     */
    public static void cleanConfiguration(IAutoBuildConfigurationDescription autoConf) throws CoreException {
        IFolder buildRoot = autoConf.getBuildFolder();
        IProject iProject = buildRoot.getProject();
        int membersBeforeClean = buildRoot.members().length;

        iProject.build(IncrementalProjectBuilder.CLEAN_BUILD, new NullProgressMonitor());

        int membersAfterClean = buildRoot.members().length;
        assertTrue("clean did not remove files", membersAfterClean < membersBeforeClean);
        int a = 0;
    }

    /**
     * Convenience method to call BuildAndVerify for the active config of a project
     * and a builder
     *
     * @param theTestProject
     *            The project to build
     * @param builderName
     *            if null use the default builder else use the builder with the
     *            provided name
     * @return true if build is successful otherwise false
     * @throws Exception
     */
    public static void BuildAndVerifyActiveConfig(IProject theTestProject) throws Exception {
        BuildAndVerifyActiveConfig(theTestProject, null, null);
    }

    public static void BuildAndVerifyActiveConfig(IProject theTestProject, String builderName,
            Boolean shouldMakefileExists) throws Exception {
        build(theTestProject, builderName, null);

        verifyConfig(theTestProject, IAutoBuildConfigurationDescription.getActiveConfig(theTestProject, false),
                shouldMakefileExists);

    }

    public static void build(IProject theTestProject, String builderName, String configs) throws Exception {
        NullProgressMonitor monitor = new NullProgressMonitor();

        waitForAllJobsToFinish();
        if (builderName == null && configs == null) {
            theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
        } else {
            Map<String, String> args = new HashMap<>();
            if (builderName != null) {
                args.put(AutoBuildProject.ARGS_BUILDER_KEY, builderName);
            }
            if (configs != null) {
                args.put(AutoBuildProject.ARGS_CONFIGS_KEY, configs);
            }
            theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, AutoBuildProject.BUILDER_ID, args, monitor);
        }
        waitForAllJobsToFinish();
    }

    public static void verifyConfig(IProject theTestProject, IAutoBuildConfigurationDescription autoBuild,
            Boolean shouldMakefileExists) throws Exception {

        hasProjectBuildErrors(theTestProject);
        checkIfConfigurationBuildCorrectly(theTestProject, autoBuild, shouldMakefileExists);

        if (deleteProjects) {
            theTestProject.delete(true, true, null);
        } else {
            if (closeProjects) {
                theTestProject.close(null);
            }
        }
    }
}
