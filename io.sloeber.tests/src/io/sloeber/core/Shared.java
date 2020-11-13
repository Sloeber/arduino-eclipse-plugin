package io.sloeber.core;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.lang.SystemUtils;
import org.eclipse.cdt.core.model.ICModelMarker;
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

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.CompileOptions;
import io.sloeber.core.api.PackageManager;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.providers.MCUBoard;

@SuppressWarnings("nls")
public class Shared {
    private static boolean deleteProjects = true;

    public static void setDeleteProjects(boolean deleteProjects) {
        Shared.deleteProjects = deleteProjects;
    }

    private static int myLocalBuildCounter;
    private static int myTestCounter;
    private static String myLastFailMessage = new String();
    private static boolean closeFailedProjects;

    public static boolean isCloseFailedProjects() {
        return closeFailedProjects;
    }

    public static void setCloseFailedProjects(boolean closeFailedProjects) {
        Shared.closeFailedProjects = closeFailedProjects;
    }

    public static boolean hasBuildErrors(IProject project) throws CoreException {
        IMarker[] markers = project.findMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
        for (IMarker marker : markers) {
            if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
                return true;
            }
        }
        IPath resultPath = project.getLocation().append("Release");
        String projName=project.getName() ;
        String[] validOutputss=  {projName+".elf",projName+".bin",projName+".hex",projName+".exe","application.axf"};
        for(String validOutput:validOutputss) {   
            File validFile = resultPath.append( validOutput).toFile();
            if (validFile.exists()) {
                return false;
            }
        }
        
        return true;
    }

    public static void waitForAllJobsToFinish() {
        try {
            Thread.sleep(1000);
            IJobManager jobMan = Job.getJobManager();
            while (!(jobMan.isIdle() && PackageManager.isReady())) {
                Thread.sleep(500);
                // If you do not get out of this loop it probably means you are
                // runnning the test in the gui thread
            }
            // As nothing is running now we can start installing
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("can not find installerjob");
        }
    }

    public static IPath getTemplateFolder(String templateName) {
        try {
            Bundle bundle = Platform.getBundle("io.sloeber.tests");
            Path path = new Path("src/templates/" + templateName);
            URL fileURL = FileLocator.find(bundle, path, null);
            URL resolvedFileURL = FileLocator.toFileURL(fileURL);
            return new Path(resolvedFileURL.toURI().getPath());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.err.println("Failed to find templates in io.sloeber.tests plugin.");
        return new Path(new String());
    }

    /**
     * Convenience method to call BuildAndVerify with default project name and
     * default compile options
     *
     * @param boardDescriptor
     * @param codeDescriptor
     * @return true if build is successful otherwise false
     */
    public static boolean BuildAndVerify(BoardDescriptor boardDescriptor, CodeDescriptor codeDescriptor) {
        return BuildAndVerify(boardDescriptor, codeDescriptor, null, -1);
    }

    /**
     * Convenience method to call BuildAndVerify with default project name and null
     * as compile options
     *
     * @param boardDescriptor
     * @param codeDescriptor
     * @param compileOptions
     *            can be null
     * @return true if build is successful otherwise false
     */
    public static boolean BuildAndVerify(BoardDescriptor boardDescriptor, CodeDescriptor codeDescriptor,
            CompileOptions compileOptions, int globalBuildCounter) {

        int projectCounter = myLocalBuildCounter;
        if (globalBuildCounter >= 0) {
            projectCounter = globalBuildCounter;
        }
        String projectName = String.format("%05d_%s", Integer.valueOf(projectCounter), boardDescriptor.getBoardID());
        if (codeDescriptor.getExampleName() != null) {
            if (codeDescriptor.getExamples().get(0).toString().toLowerCase().contains("libraries")) {
                projectName = String.format("%05d_Library_%s_%s", Integer.valueOf(projectCounter),
                        codeDescriptor.getLibraryName(), codeDescriptor.getExampleName());
            } else {
                projectName = String.format("%05d_%s", Integer.valueOf(projectCounter),
                        codeDescriptor.getExampleName());
            }
        }

        CompileOptions localCompileOptions = compileOptions;
        if (compileOptions == null) {
            localCompileOptions = new CompileOptions(null);
        }
        return BuildAndVerify(projectName, boardDescriptor, codeDescriptor, localCompileOptions);
    }

    public static boolean BuildAndVerify(String projectName, BoardDescriptor boardDescriptor,
            CodeDescriptor codeDescriptor, CompileOptions compileOptions) {
        IProject theTestProject = null;
        NullProgressMonitor monitor = new NullProgressMonitor();
        myLocalBuildCounter++;

        try {
            compileOptions.setEnableParallelBuild(true);
            theTestProject = boardDescriptor.createProject(projectName, null, codeDescriptor, compileOptions, monitor);
            waitForAllJobsToFinish(); // for the indexer
        } catch (Exception e) {
            e.printStackTrace();
            myLastFailMessage = "Failed to create the project:" + projectName;
            return false;
        }
        try {
            theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
            if (hasBuildErrors(theTestProject)) {
                waitForAllJobsToFinish(); // for the indexer
                Thread.sleep(2000);
                theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
                if (hasBuildErrors(theTestProject)) {
                    waitForAllJobsToFinish(); // for the indexer
                    Thread.sleep(2000);
                    theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
                    if (hasBuildErrors(theTestProject)) {
                        myLastFailMessage = "Failed to compile the project:" + projectName + " build errors";
                        if (closeFailedProjects) {
                            theTestProject.close(null);
                        }
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            myLastFailMessage = "Failed to compile the project:" + boardDescriptor.getBoardName() + " exception";
            return false;
        }
        try {
            if (deleteProjects) {
                theTestProject.delete(true, true, null);
            } else {
                theTestProject.close(null);
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return true;
    }

    /*
     * For some boards that do not run out of the box we know how to fix it. This
     * code fixes these things
     */
    public static void applyKnownWorkArounds() {

        java.nio.file.Path packageRoot = Paths.get(ConfigurationPreferences.getInstallationPathPackages().toString());

        /*
         * oak on linux comes with a esptool2 in a wrong folder. As it is only 1 file I
         * move the file
         *
         */
        if (SystemUtils.IS_OS_LINUX) {
            java.nio.file.Path esptool2root = packageRoot.resolve("digistump").resolve("tools").resolve("esptool2")
                    .resolve("0.9.1");
            java.nio.file.Path esptool2wrong = esptool2root.resolve("0.9.1").resolve("esptool2");
            java.nio.file.Path esptool2right = esptool2root.resolve("esptool2");
            if (esptool2wrong.toFile().exists()) {
                try {
                    Files.move(esptool2wrong, esptool2right);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        /*
         * Elector heeft core Platino maar de directory noemt platino. In windows geen
         * probleem maar in case sensitive linux dus wel
         */
        if (SystemUtils.IS_OS_LINUX) {
            java.nio.file.Path cores = packageRoot.resolve("Elektor").resolve("hardware").resolve("avr")
                    .resolve("1.0.0").resolve("cores");
            java.nio.file.Path coreWrong = cores.resolve("platino");
            java.nio.file.Path coreGood = cores.resolve("Platino");
            if (coreWrong.toFile().exists()) {
                coreWrong.toFile().renameTo(coreGood.toFile());
            }
        }
    }

    public static String getCounterName(String name) {
        String counterName = String.format("%05d_%s", Integer.valueOf(myTestCounter++), name);
        return counterName;
    }

    @SuppressWarnings("unused")
    public static String getProjectName(CodeDescriptor codeDescriptor, Examples example, MCUBoard board) {
        return String.format("%05d_%s_%s", Integer.valueOf(myTestCounter++), codeDescriptor.getExampleName(),
                board.getBoardDescriptor().getBoardID());
    }

    public static String getLastFailMessage() {
        // TODO Auto-generated method stub
        return myLastFailMessage;
    }
}
