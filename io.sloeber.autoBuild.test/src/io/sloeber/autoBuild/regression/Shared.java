package io.sloeber.autoBuild.regression;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.cdt.core.model.ICModelMarker;
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

        doFail(project, "The build did not produce a file that is considered a targetfile");
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
    public static void BuildAndVerify(IProject theTestProject, String builderName, String configs) throws Exception {
        BuildAndVerify(theTestProject, builderName, configs, null);

    }

    public static void BuildAndVerify(IProject theTestProject, String builderName, String configs,
            Boolean shouldMakefileExists) throws Exception {
        build(theTestProject, builderName, configs);

        verifyConfig(theTestProject, IAutoBuildConfigurationDescription.getActiveConfig(theTestProject),
                shouldMakefileExists);

    }

    public static void build(IProject theTestProject, String builderName, String configs) throws Exception {
        NullProgressMonitor monitor = new NullProgressMonitor();

        waitForAllJobsToFinish(); // for the indexer
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
