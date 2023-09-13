package io.sloeber.autoBuild.regression;

import static org.junit.Assert.*;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

    public static boolean hasBuildErrors(IProject project) throws CoreException {
        IMarker[] markers = project.findMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
        for (IMarker marker : markers) {
            if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
                return true;
            }
        }
        IAutoBuildConfigurationDescription autoBuild = IAutoBuildConfigurationDescription.getActiveConfig(project);

        IFolder buildRootFolder = autoBuild.getBuildFolder();
        String projName = project.getName();

        String[] validOutputs = { projName + ".elf", projName + ".bin", projName + ".hex", projName + ".exe",
                "lib_" + projName + ".so", projName + ".so", "lib_" + projName + ".a", projName + ".dll",
                "application.axf" };
        for (String validOutput : validOutputs) {
            if (buildRootFolder.getFile(validOutput).exists()) {
                return false;
            }
        }

        return true;
    }

    public static void waitForAllJobsToFinish() {
        try {
            Thread.sleep(1000);
            IJobManager jobMan = Job.getJobManager();
            int counter = 0;
            while ((!jobMan.isIdle()) && (counter++ < 120)) {
                Thread.sleep(500);
                // If you have to wait for counter it may mean you are
                // runnning the test in the gui thread
            }
            // As nothing is running now we can start installing
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("can not find installerjob");
        }
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
    public static boolean BuildAndVerify(IProject theTestProject, String builderName, String configs) throws Exception {

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
        if (hasBuildErrors(theTestProject)) {
            if (closeFailedProjects) {
                theTestProject.close(null);
            }
            return false;
        }
        try {
            if (deleteProjects) {
                theTestProject.delete(true, true, null);
            } else {
                if (closeProjects) {
                    theTestProject.close(null);
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
        }
        return true;
    }

    // copied from
    // https://www.codejava.net/java-se/file-io/programmatically-extract-a-zip-file-using-java

    // /**
    // * This utility extracts files and directories of a standard zip file to
    // * a destination directory.
    // * @author www.codejava.net
    // *
    // */
    // public class UnzipUtility {
    /**
     * Size of the buffer to read/write data
     */
    private static final int BUFFER_SIZE = 4096;

    /**
     * Extracts a zip file specified by the zipFilePath to a directory specified by
     * destDirectory (will be created if does not exists)
     * 
     * @param zipFilePath
     * @param destDirectory
     * @throws IOException
     */
    public static void unzip(String zipFilePath, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }
        ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                dir.mkdirs();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    /**
     * Extracts a zip entry (file entry)
     * 
     * @param zipIn
     * @param filePath
     * @throws IOException
     */
    private static void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
        byte[] bytesIn = new byte[BUFFER_SIZE];
        int read = 0;
        while ((read = zipIn.read(bytesIn)) != -1) {
            bos.write(bytesIn, 0, read);
        }
        bos.close();
    }
    // }
    // end copy from
    // https://www.codejava.net/java-se/file-io/programmatically-extract-a-zip-file-using-java
}
