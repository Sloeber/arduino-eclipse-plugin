package io.sloeber.core.api;

import static io.sloeber.core.Messages.*;
import static io.sloeber.core.common.Const.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import com.google.gson.Gson;

import io.sloeber.core.Activator;
import io.sloeber.core.api.Json.ArduinoLibrary;
import io.sloeber.core.api.Json.ArduinoLibraryIndex;
import io.sloeber.core.api.Json.ArduinoLibraryVersion;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.InstancePreferences;
import io.sloeber.core.core.DefaultInstallHandler;
import io.sloeber.core.tools.FileModifiers;
import io.sloeber.core.tools.PackageManager;

/**
 * This class is the main entry point for libraries. It handles private
 * libraries hardware libraries installed libraries
 *
 * @author jan
 *
 */
public class LibraryManager {
    private static final String INO = "ino"; //$NON-NLS-1$
    private static final String PDE = "pde";//$NON-NLS-1$
    private static final String CPP = "cpp";//$NON-NLS-1$
    private static final String C = "c";//$NON-NLS-1$
    private static final String LIBRARY_DESCRIPTOR_PREFIX = "Library"; //$NON-NLS-1$
    private static final String EXAMPLE_DESCRIPTOR_PREFIX = "Example"; //$NON-NLS-1$

    static private List<ArduinoLibraryIndex> libraryIndices;
    private static IInstallLibraryHandler myInstallLibraryHandler = new DefaultInstallHandler();

    static public List<ArduinoLibraryIndex> getLibraryIndices() {
        if (libraryIndices == null) {
            BoardsManager.getPackageIndices();
        }
        return libraryIndices;
    }

    public static IStatus install(List<ArduinoLibraryVersion> removeLibs, List<ArduinoLibraryVersion> addLibs,
            IProgressMonitor monitor, MultiStatus status) {
        for (ArduinoLibraryVersion lib : removeLibs) {
            status.add(unInstall(lib, monitor));
            if (monitor.isCanceled())
                return Status.CANCEL_STATUS;
        }
        for (ArduinoLibraryVersion lib : addLibs) {
            status.add(install(lib, monitor));
            if (monitor.isCanceled())
                return Status.CANCEL_STATUS;
        }
        return status;
    }

    public static String getPrivateLibraryPathsString() {
        return InstancePreferences.getPrivateLibraryPathsString();
    }

    public static void setPrivateLibraryPaths(String[] libraryPaths) {
        InstancePreferences.setPrivateLibraryPaths(libraryPaths);

    }

    public static void InstallDefaultLibraries(IProgressMonitor monitor) {
        for (ArduinoLibraryIndex libindex : libraryIndices) {

            for (String libraryName : Defaults.DEFAULT_INSTALLED_LIBRARIES) {
                ArduinoLibrary toInstalLib = libindex.getLibrary(libraryName);
                ArduinoLibraryVersion toInstalLibVersion = toInstalLib.getNewestVersion();
                ArduinoLibraryVersion instaledLibVersion = toInstalLib.getInstalledVersion();
                if (toInstalLibVersion != null) {
                    if (toInstalLibVersion != instaledLibVersion) {
                        if (instaledLibVersion != null) {
                            unInstall(instaledLibVersion, monitor);
                        }
                        install(toInstalLibVersion, monitor);
                    }
                }
            }
        }
    }

    static public void loadJson(File jsonFile) {
        try (Reader reader = new FileReader(jsonFile)) {
            ArduinoLibraryIndex index = new Gson().fromJson(reader, ArduinoLibraryIndex.class);
            index.setJsonFile(jsonFile);
            libraryIndices.add(index);
        } catch (Exception e) {
            Common.log(new Status(IStatus.ERROR, Activator.getId(),
                    Manager_Failed_to_parse.replace(FILE_TAG, jsonFile.getAbsolutePath()), e));
            jsonFile.delete();// Delete the file so it stops damaging
        }
    }

    /**
     * install 1 single library based on the library name
     * 
     * @param libName
     */
    public static void installLibrary(String libName) {
        Set<String> libNamesToInstall = new TreeSet<>();
        libNamesToInstall.add(libName);
        Map<String, ArduinoLibraryVersion> libsToInstall = LibraryManager
                .getLatestInstallableLibraries(libNamesToInstall);
        if (!libsToInstall.isEmpty()) {
            for (ArduinoLibraryVersion curLib : libsToInstall.values()) {
                install(curLib, new NullProgressMonitor());
            }
        }
    }

    public static IStatus install(ArduinoLibraryVersion lib, IProgressMonitor monitor) {
        monitor.setTaskName("Downloading and installing " + lib.getName() + " library."); //$NON-NLS-1$ //$NON-NLS-2$
        if (lib.isInstalled()) {
            return Status.OK_STATUS;
        }
        IStatus ret = PackageManager.downloadAndInstall(lib.getUrl(), lib.getArchiveFileName(), lib.getInstallPath(),
                false, monitor);
        FileModifiers.addPragmaOnce(lib.getInstallPath());
        return ret;
    }

    /**
     * delete the library This will delete all installed versions of the library.
     * Normally only 1 version can be installed so deleting all versions should be
     * delete 1 version
     *
     * @param monitor
     * @return Status.OK_STATUS if delete is successful otherwise IStatus.ERROR
     */
    public static IStatus unInstall(ArduinoLibraryVersion lib, IProgressMonitor monitor) {
        if (!lib.isInstalled()) {
            return Status.OK_STATUS;
        }

        try {
            FileUtils.deleteDirectory(lib.getInstallPath().toFile().getParentFile());
        } catch (IOException e) {
            return new Status(IStatus.ERROR, Activator.getId(),
                    "Failed to remove folder" + lib.getInstallPath().toString(), //$NON-NLS-1$
                    e);
        }

        return Status.OK_STATUS;
    }

    /**
     * Install the latest version of all the libraries belonging to this category If
     * a earlier version is installed this version will be removed before
     * installation of the newer version
     *
     * @param category
     */
    public static void installAllLatestLibraries() {
        List<ArduinoLibraryIndex> libraryIndices1 = getLibraryIndices();
        for (ArduinoLibraryIndex libraryIndex : libraryIndices1) {
            for (ArduinoLibrary curLib : libraryIndex.getLibraries()) {
                String curLibName = curLib.getName();
                String[] skipArray = { "Base64", "Add others if needed" }; //$NON-NLS-1$ //$NON-NLS-2$
                List<String> skipList = Arrays.asList(skipArray);
                if (!skipList.contains(curLibName)) {
                    ArduinoLibraryVersion latestLibVersion = curLib.getNewestVersion();
                    if (!latestLibVersion.isInstalled()) {
                        ArduinoLibraryVersion installedLibVersion = curLib.getInstalledVersion();
                        if (installedLibVersion != null) {
                            unInstall(installedLibVersion, null);
                        }
                        install(latestLibVersion, null);
                    }
                }
            }
        }
    }

    public static void flushIndices() {
        libraryIndices = new ArrayList<>();
    }

    public static void unInstallAllLibs() {
        try {
            FileUtils.deleteDirectory(ConfigurationPreferences.getInstallationPathLibraries().toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, ArduinoLibraryVersion> getLatestInstallableLibraries(Set<String> libnames) {
        Set<String> remainingLibNames = new TreeSet<>(libnames);
        Map<String, ArduinoLibraryVersion> ret = new HashMap<>();
        for (ArduinoLibraryIndex libraryIndex : libraryIndices) {
            ret.putAll(libraryIndex.getLatestInstallableLibraries(remainingLibNames));
            remainingLibNames.removeAll(ret.keySet());
        }

        return ret;
    }

    public static void registerInstallLibraryHandler(IInstallLibraryHandler installLibraryHandler) {
        myInstallLibraryHandler = installLibraryHandler;
    }

    public static IInstallLibraryHandler getInstallLibraryHandler() {
        return myInstallLibraryHandler;
    }

    /**
     * Check wether a library is installed. The check looks at the library
     * installation place at the disk.
     *
     * @return true if at least one library is installed
     */
    public static boolean libsAreInstalled() {
        if (ConfigurationPreferences.getInstallationPathLibraries().toFile().exists()) {
            return ConfigurationPreferences.getInstallationPathLibraries().toFile().list().length != 0;
        }
        return false;
    }

    /**
     * find all examples that are delivered with a library This does not include the
     * libraries delivered with hardware
     *
     * @return
     */
    public static TreeMap<String, IPath> getAllLibraryExamples() {
        TreeMap<String, IPath> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        String libLocations[] = InstancePreferences.getPrivateLibraryPaths();

        File CommonLibLocation = ConfigurationPreferences.getInstallationPathLibraries().toFile();
        if (CommonLibLocation.exists()) {
            examples.putAll(getLibExampleFolders(CommonLibLocation));
        }

        // get the examples from the user provide library locations
        if (libLocations != null) {
            for (String curLibLocation : libLocations) {
                File curFile = new File(curLibLocation);
                if (curFile.exists()) {
                    examples.putAll(getLibExampleFolders(curFile));
                }
            }
        }
        return examples;
    }

    /***
     * finds all the example folders for both the version including and without
     * version libraries
     *
     * @param location
     *            The parent folder of the libraries
     */
    private static TreeMap<String, IPath> getLibExampleFolders(File LibRoot) {
        TreeMap<String, IPath> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        if (LibRoot == null) {
            return examples;
        }
        String[] Libs = LibRoot.list();
        if (Libs == null) {
            // Either dir does not exist or is not a directory
            return examples;
        }
        for (String curLib : Libs) {
            String libID = LIBRARY_DESCRIPTOR_PREFIX + '/' + curLib;
            File Lib_examples = LibRoot.toPath().resolve(curLib).resolve("examples").toFile();//$NON-NLS-1$
            File Lib_Examples = LibRoot.toPath().resolve(curLib).resolve("Examples").toFile();//$NON-NLS-1$
            if (Lib_examples.isDirectory()) {
                examples.putAll(getExamplesFromFolder(libID, Lib_examples, 2));
            } else if (Lib_Examples.isDirectory()) {
                examples.putAll(getExamplesFromFolder(libID, Lib_Examples, 2));
            } else // nothing found directly so maybe this is a version
                   // based lib
            {
                String[] versions = LibRoot.toPath().resolve(curLib).toFile().list();
                if (versions != null) {
                    if (versions.length == 1) {// There can only be 1
                        // version of a lib
                        Lib_examples = LibRoot.toPath().resolve(curLib).resolve(versions[0]).resolve("examples") //$NON-NLS-1$
                                .toFile();
                        Lib_Examples = LibRoot.toPath().resolve(curLib).resolve(versions[0]).resolve("Examples") //$NON-NLS-1$
                                .toFile();
                        if (Lib_examples.isDirectory()) {
                            examples.putAll(getExamplesFromFolder(libID, Lib_examples, 2));
                        } else if (Lib_Examples.isDirectory()) {
                            examples.putAll(getExamplesFromFolder(libID, Lib_Examples, 2));
                        }
                    }
                }
            }
        }

        return examples;
    }

    /**
     * This method adds a folder recursively examples. Leaves containing ino files
     * are assumed to be examples
     *
     * @param File
     */
    private static TreeMap<String, IPath> getExamplesFromFolder(String prefix, File location, int maxDepth) {
        TreeMap<String, IPath> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        File[] children = location.listFiles();
        if ((children == null) || (maxDepth <= 0)) {
            // Either dir does not exist or is not a directory
            return examples;
        }
        int newmaxDepth = maxDepth - 1;
        for (File exampleFolder : children) {
            String extension = FilenameUtils.getExtension(exampleFolder.toString()).toLowerCase();
            if (exampleFolder.isDirectory()) {
                examples.putAll(
                        getExamplesFromFolder(prefix + '/' + exampleFolder.getName(), exampleFolder, newmaxDepth));
            } else if (INO.equals(extension) || PDE.equals(extension) || CPP.equals(extension) || C.equals(extension)) {
                examples.put(prefix, new Path(location.toString()));
            }
        }
        return examples;
    }

    /*
     * Get the examples of the libraries from the selected hardware These may be
     * referenced libraries
     */
    private static TreeMap<String, IPath> getAllHardwareLibraryExamples(BoardDescription boardDescriptor) {
        TreeMap<String, IPath> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (boardDescriptor != null) {
            IPath platformPath = boardDescriptor.getreferencingPlatformPath();
            if (platformPath.toFile().exists()) {
                examples.putAll(getLibExampleFolders(platformPath.append(LIBRARY_PATH_SUFFIX).toFile()));
            }
        }
        return examples;
    }

    /**
     * find all examples that are delivered with the Arduino IDE
     *
     * @return
     */
    public static TreeMap<String, IPath> getAllArduinoIDEExamples() {
        TreeMap<String, IPath> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        File exampleLocation = ConfigurationPreferences.getInstallationPathExamples().toFile();

        if (exampleLocation.exists()) {
            examples.putAll(getExamplesFromFolder(EXAMPLE_DESCRIPTOR_PREFIX, exampleLocation, 100));
        }
        return examples;
    }

    /**
     * find all examples for this type of board. That is the examples provided by
     * Arduino The examples provided by the common libraries The examples provided
     * by the private libraries The examples provided by the platform the board
     * belongs to
     *
     * If the boardID is null there will be no platform examples
     *
     * @param boardDescriptor
     * @return
     */
    public static TreeMap<String, IPath> getAllExamples(BoardDescription boardDescriptor) {
        TreeMap<String, IPath> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        // Get the examples of the library manager installed libraries

        examples.putAll(getAllLibraryExamples());
        examples.putAll(getAllArduinoIDEExamples());
        // This one should be the last as hasmap overwrites doubles. This way
        // hardware libraries are preferred to others
        examples.putAll(getAllHardwareLibraryExamples(boardDescriptor));

        return examples;
    }

    public static IStatus updateLibraries(Set<ArduinoLibraryVersion> toUnInstallLibs,
            Set<ArduinoLibraryVersion> toInstallLibs, IProgressMonitor monitor, MultiStatus status) {
        for (ArduinoLibraryVersion curLib : toUnInstallLibs) {
            status.add(unInstall(curLib, monitor));
        }
        for (ArduinoLibraryVersion curLib : toInstallLibs) {
            status.add(install(curLib, monitor));
        }
        return status;
    }

}