package io.sloeber.core.tools;

import static io.sloeber.core.Messages.*;
import static io.sloeber.core.common.Const.*;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang.ArrayUtils;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexFile;
import org.eclipse.cdt.core.index.IIndexInclude;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.IInstallLibraryHandler;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.api.SloeberProject;
import io.sloeber.core.api.VersionNumber;
import io.sloeber.core.api.Json.library.LibraryJson;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.InstancePreferences;

public class Libraries {
    public static final String WORKSPACE_LIB_FOLDER = "libraries/"; //$NON-NLS-1$
    public static String INCLUDE = "INCLUDE"; //$NON-NLS-1$
    public static String REMOVE = "REMOVE"; //$NON-NLS-1$

    /**
     * for a given folder return all subfolders
     *
     * @param ipath
     *            the folder you want the subfolders off
     * @return The subfolders of the ipath folder. May contain empty values. This
     *         method returns a key value pair of key equals foldername and value
     *         equals full path.
     */
    private static Map<String, IPath> findAllSubFolders(IPath ipath) {
        String[] children = ipath.toFile().list();
        Map<String, IPath> ret = new HashMap<>();
        if (children == null) {
            // Either dir does not exist or is not a directory
        } else {
            // if the folder contains any of the following it is considered a
            // library itself
            // src library.properties or examples
            if (ArrayUtils.contains(children, "src") //$NON-NLS-1$
                    || ArrayUtils.contains(children, "library.properties") //$NON-NLS-1$
                    || ArrayUtils.contains(children, "examples")) { //$NON-NLS-1$
                ret.put(ipath.lastSegment(), ipath);
                return ret;
            }
            // if the folder contains a *.h or *.cpp file it is considered a
            // library itself
            for (String curFolder : children) {
                // Get filename of file or directory
                IPath LibPath = ipath.append(curFolder);
                File LibPathFile = LibPath.toFile();
                if (LibPathFile.isFile() && (!LibPathFile.getName().startsWith(".")) //$NON-NLS-1$
                        && ("cpp".equalsIgnoreCase(LibPath.getFileExtension()) //$NON-NLS-1$
                                || "h".equalsIgnoreCase( //$NON-NLS-1$
                                        LibPath.getFileExtension()))) {
                    ret.put(ipath.lastSegment(), ipath);
                    return ret;
                }
            }
            // otherwise assume all subfolders are libraries
            for (String curFolder : children) {
                // Get filename of file or directory
                IPath LibPath = ipath.append(curFolder);
                File LibPathFile = LibPath.toFile();
                if (LibPathFile.isDirectory() && !LibPathFile.isHidden()) {
                    ret.put(curFolder, LibPath);
                }
            }
        }
        return ret;
    }

    /**
     * Searches all the hardware dependent libraries of a project. If this is a
     * board referencing a core then the libraries of the referenced core are added
     * as well
     *
     * @param project
     *            the project to find all hardware libraries for
     * @return all the library folder names. May contain empty values.
     */
    private static Map<String, IPath> findAllHarwareLibraries(ICConfigurationDescription confDesc) {
        Map<String, IPath> ret = new HashMap<>();
        IProject project = confDesc.getProjectDescription().getProject();
        SloeberProject sProject = SloeberProject.getSloeberProject(project, false);
        BoardDescription boardDescriptor = sProject.getBoardDescription(confDesc.getName(), false);
        // first add the referenced
        IPath libPath = boardDescriptor.getReferencedLibraryPath();
        if (libPath != null) {
            ret.putAll(findAllSubFolders(libPath));
        }
        // then add the referencing
        libPath = boardDescriptor.getReferencingLibraryPath();
        if (libPath != null) {
            ret.putAll(findAllSubFolders(libPath));
        }
        return ret;
    }

    public static Map<String, IPath> findAllPrivateLibraries() {
        Map<String, IPath> ret = new HashMap<>();
        String privateLibPaths[] = InstancePreferences.getPrivateLibraryPaths();
        for (String curLibPath : privateLibPaths) {
            ret.putAll(findAllSubFolders(new Path(curLibPath)));
        }
        return ret;

    }

    public static Map<String, IPath> findAllArduinoManagerLibraries() {
        Map<String, IPath> ret = new HashMap<>();
        IPath CommonLibLocation = ConfigurationPreferences.getInstallationPathLibraries();
        if (CommonLibLocation.toFile().exists()) {

            String[] Libs = CommonLibLocation.toFile().list();
            if (Libs == null) {
                // Either dir does not exist or is not a directory
            } else {
                java.util.Arrays.sort(Libs, String.CASE_INSENSITIVE_ORDER);
                for (String curLib : Libs) {
                    IPath Lib_root = CommonLibLocation.append(curLib);

                    String[] versions = Lib_root.toFile().list();
                    if (versions != null) {
                        switch (versions.length) {
                        case 0:// A empty lib folder is hanging around
                            Common.log(new Status(IStatus.WARNING, CORE_PLUGIN_ID,
                                    EmptyLibFolder.replace(LIB_TAG, curLib)));
                            Lib_root.toFile().delete();
                            break;
                        case 1:// There should only be 1
                            ret.put(curLib, Lib_root.append(versions[0]));

                            break;
                        default:// multiple lib versions are installed take
                                // the
                                // latest
                            int highestVersion = getHighestVersion(versions);
                            ret.put(curLib, Lib_root.append(versions[highestVersion]));
                            Common.log(new Status(IStatus.WARNING, CORE_PLUGIN_ID,
                                    MultipleVersionsOfLib.replace(LIB_TAG, curLib)));

                        }
                    }
                }
            }
        }
        return ret;

    }

    /**
     * Given a list of version strings returns the index of the highest version
     * If the highest version is multiple times in the list the result will
     * point to one of those but the result may be different for each call
     *
     * @param versions
     *            a string list of version numbers
     *
     * @return the index to the highest version or 0 in case of an empty
     *         versions
     */
    private static int getHighestVersion(String[] versions) {
        int returnIndex = 0;
        for (int curVersion = 1; curVersion < versions.length; curVersion++) {
            if (new VersionNumber(versions[returnIndex]).compareTo(versions[curVersion]) == -1) {
                returnIndex = curVersion;
            }

        }
        return returnIndex;
    }

    /**
     * Removes a set of libraries from a project
     *
     * @param project
     *            the project from which to remove libraries
     * @param confdesc
     *            the configuration from which to remove libraries
     * @param libraries
     *            set of libraries to remove
     */
    public static boolean removeLibrariesFromProject(IProject project, ICConfigurationDescription confdesc,
            Set<String> libraries) {
        for (String CurItem : libraries) {
            try {
                final IFolder folderHandle = project.getFolder(WORKSPACE_LIB_FOLDER + CurItem);
                folderHandle.delete(true, null);
            } catch (CoreException e) {
                Common.log(
                        new Status(IStatus.ERROR, CORE_PLUGIN_ID, failed_to_remove_lib.replace(LIB_TAG, CurItem), e));
            }
        }
        return Helpers.removeInvalidIncludeFolders(confdesc);
    }

    public static Map<String, IPath> getAllInstalledLibraries(ICConfigurationDescription confdesc) {
        TreeMap<String, IPath> libraries = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        // lib folder name
        libraries.putAll(findAllArduinoManagerLibraries());
        libraries.putAll(findAllPrivateLibraries());
        libraries.putAll(findAllHarwareLibraries(confdesc));
        return libraries;
    }

    /**
     * 
     * @param project
     *            the project the libratries need to be added to
     * @param confdesc
     *            a writable confdesc
     * @param librariesToAdd
     *            the libraries to add
     * @return return true if the projdesc needs to be set
     */
    public static Map<String, List<IPath>> addLibrariesToProject(IProject project, ICConfigurationDescription confdesc,
            Set<String> librariesToAdd) {
        Map<String, IPath> libraries = getAllInstalledLibraries(confdesc);
        libraries.keySet().retainAll(librariesToAdd);
        if (libraries.isEmpty()) {
            return new HashMap<>();
        }
        return addLibrariesToProject(project, libraries);
    }

    /**
     * Adds one or more libraries to a project in a configuration
     *
     * @param project
     *            the project to add the libraries to
     * @param confdesc
     *            the confdesc of the project
     * @param libraries
     *            the list of libraries to add
     * @return true if the configuration description has changed
     */
    public static Map<String, List<IPath>> addLibrariesToProject(IProject project, Map<String, IPath> libraries) {

        List<IPath> foldersToRemoveFromBuildPath = new LinkedList<>();
        List<IPath> foldersToAddToIncludes = new LinkedList<>();
        for (Entry<String, IPath> CurItem : libraries.entrySet()) {
            foldersToAddToIncludes.addAll(
                    Helpers.addCodeFolder(project, CurItem.getValue(), WORKSPACE_LIB_FOLDER + CurItem.getKey(), false));
            // Check the libraries to see if there are "unwanted subfolders"
            File subFolders[] = CurItem.getValue().toFile().listFiles();
            for (File file : subFolders) {
                if (file.isDirectory() && !"src".equals(file.getName()) //$NON-NLS-1$
                        && !"utility".equals(file.getName()) //$NON-NLS-1$
                        && !"examples".equalsIgnoreCase(file.getName())) { //$NON-NLS-1$
                    IPath excludePath = new Path("/" + project.getName()) //$NON-NLS-1$
                            .append(WORKSPACE_LIB_FOLDER).append(CurItem.getKey()).append(file.getName());
                    foldersToRemoveFromBuildPath.add(excludePath);

                }
            }
        }
        Map<String, List<IPath>> codePathChanges = new HashMap<>();
        codePathChanges.put(INCLUDE, foldersToAddToIncludes);
        codePathChanges.put(REMOVE, foldersToRemoveFromBuildPath);
        return codePathChanges;

    }

    // public static void removeLibrariesFromProject(Set<String> libraries) {
    //
    // }

    public static Set<String> getAllLibrariesFromProject(IProject project) {
        IFolder link = project.getFolder(WORKSPACE_LIB_FOLDER);
        Set<String> ret = new TreeSet<>();
        try {
            if (link.exists()) {
                for (IResource curResource : link.members()) {
                    ret.add(curResource.getName());
                }
            }
        } catch (CoreException e) {
            // ignore
            e.printStackTrace();
        }

        return ret;
    }

    public static void reAttachLibrariesToProject(IProject project) {
        boolean descNeedsSet = false;
        Set<String> AllLibrariesOriginallyUsed = getAllLibrariesFromProject(project);
        ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
        ICProjectDescription projDesc = mngr.getProjectDescription(project, true);
        ICConfigurationDescription configurationDescriptions[] = projDesc.getConfigurations();
        for (ICConfigurationDescription curconfDesc : configurationDescriptions) {
            Map<String, List<IPath>> foldersToChange = addLibrariesToProject(project, curconfDesc,
                    AllLibrariesOriginallyUsed);
            descNeedsSet = descNeedsSet || adjustProjectDescription(curconfDesc, foldersToChange);
        }
        if (descNeedsSet) {
            try {
                mngr.setProjectDescription(project, projDesc, true, null);
            } catch (CoreException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Removes a set of libraries from a project in each project configuration
     *
     * @param project
     *            the project from which to remove libraries
     * @param libraries
     *            set of libraries to remove
     */
    public static boolean removeLibrariesFromProject(IProject project, ICProjectDescription projectDescription,
            Set<String> selectedLibraries) {
        boolean descMustBeSet = false;
        ICConfigurationDescription configurationDescriptions[] = projectDescription.getConfigurations();
        for (ICConfigurationDescription CurItem : configurationDescriptions) {
            boolean curDescNeedsSet = removeLibrariesFromProject(project, CurItem, selectedLibraries);
            descMustBeSet = descMustBeSet || curDescNeedsSet;
        }
        return descMustBeSet;

    }

    private static Set<String> getUnresolvedProjectIncludes(IProject iProject) {
        Set<String> ret = new TreeSet<>();
        ICProject tt = CoreModel.getDefault().create(iProject);
        IIndex index = null;

        try {
            index = CCorePlugin.getIndexManager().getIndex(tt);
            index.acquireReadLock();
            try {

                IIndexFile allFiles[] = index.getFilesWithUnresolvedIncludes();
                for (IIndexFile curUnesolvedIncludeFile : allFiles) {
                    IIndexInclude includes[] = curUnesolvedIncludeFile.getIncludes();
                    for (IIndexInclude curinclude : includes) {
                        if (curinclude.isActive() && !curinclude.isResolved()) {
                            ret.add(new Path(curinclude.getName()).removeFileExtension().toString());
                        }
                    }
                }
            } finally {
                index.releaseReadLock();
            }
        } catch (CoreException e1) {
            // ignore
            e1.printStackTrace();
        } catch (InterruptedException e) {
            // ignore
            e.printStackTrace();
        }
        return ret;
    }

    public static void checkLibraries(IProject affectedProject) {
        Map<String, String> includeHeaderReplacement = getIncludeHeaderReplacement();
        ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
        if (mngr != null) {
            ICProjectDescription projectDescription = mngr.getProjectDescription(affectedProject, true);
            if (projectDescription != null) {
                ICConfigurationDescription confDesc = projectDescription.getActiveConfiguration();
                if (confDesc != null) {

                    Set<String> UnresolvedIncludedHeaders = getUnresolvedProjectIncludes(affectedProject);
                    Set<String> alreadyAddedLibs = getAllLibrariesFromProject(affectedProject);
                    // remove pgmspace as it gives a problem
                    UnresolvedIncludedHeaders.remove("pgmspace"); //$NON-NLS-1$
                    if (UnresolvedIncludedHeaders.isEmpty()) {
                        return;
                    }

                    for (Map.Entry<String, String> entry : includeHeaderReplacement.entrySet()) {
                        if (UnresolvedIncludedHeaders.contains(entry.getKey())) {
                            UnresolvedIncludedHeaders.remove(entry.getKey());
                            UnresolvedIncludedHeaders.add(entry.getValue());
                        }
                    }
                    UnresolvedIncludedHeaders.removeAll(alreadyAddedLibs);

                    IInstallLibraryHandler installHandler = LibraryManager.getInstallLibraryHandler();
                    if (installHandler.autoInstall()) {
                        // Check if there are libraries that are not found in
                        // the installed libraries
                        Map<String, IPath> installedLibs = getAllInstalledLibraries(confDesc);
                        Set<String> uninstalledIncludedHeaders = new TreeSet<>(UnresolvedIncludedHeaders);
                        uninstalledIncludedHeaders.removeAll(installedLibs.keySet());
                        if (!uninstalledIncludedHeaders.isEmpty()) {
                            // some libraries may need to be installed

                            Map<String, LibraryJson> availableLibs = LibraryManager
                                    .getLatestInstallableLibraries(uninstalledIncludedHeaders);

                            if (!availableLibs.isEmpty()) {
                                // We now know which libraries to install
                                // TODO for now I just install but there should
                                // be some user
                                // interaction
                                availableLibs = installHandler.selectLibrariesToInstall(availableLibs);
                                for (Entry<String, LibraryJson> curLib : availableLibs.entrySet()) {
                                    curLib.getValue().install(new NullProgressMonitor());
                                }
                            }
                        }
                    }

                    Map<String, IPath> installedLibs = getAllInstalledLibraries(confDesc);
                    installedLibs.keySet().retainAll(UnresolvedIncludedHeaders);
                    if (!installedLibs.isEmpty()) {
                        // there are possible libraries to add
                        Common.log(new Status(IStatus.INFO, CORE_PLUGIN_ID, "list of libraries to add to project " //$NON-NLS-1$
                                + affectedProject.getName() + ": " //$NON-NLS-1$
                                + installedLibs.keySet().toString()));
                        Map<String, List<IPath>> foldersToChange = addLibrariesToProject(affectedProject,
                                installedLibs);

                        if (adjustProjectDescription(confDesc, foldersToChange)) {
                            try {
                                mngr.setProjectDescription(affectedProject, projectDescription, true, null);

                            } catch (CoreException e) {
                                // this can fail because the project may already
                                // be
                                // deleted
                            }
                        }

                    }
                }
            }
        }
    }

    public static boolean adjustProjectDescription(ICConfigurationDescription confdesc,
            Map<String, List<IPath>> foldersToInclude) {
        List<IPath> foldersToAddToInclude = foldersToInclude.get(INCLUDE);
        boolean descriptionMustBeSet = Helpers.addIncludeFolder(confdesc, foldersToAddToInclude, true);
        List<IPath> foldersToRemoveFromBuildPath = foldersToInclude.get(REMOVE);
        if ((foldersToRemoveFromBuildPath != null) && (!foldersToRemoveFromBuildPath.isEmpty())) {
            ICSourceEntry[] sourceEntries = confdesc.getSourceEntries();
            for (IPath curFile : foldersToRemoveFromBuildPath) {
                try {
                    if (!CDataUtil.isExcluded(curFile, sourceEntries)) {
                        sourceEntries = CDataUtil.setExcluded(curFile, true, true, sourceEntries);
                        descriptionMustBeSet = true;
                    }

                } catch (CoreException e1) {
                    // ignore
                }
            }
            try {
                confdesc.setSourceEntries(sourceEntries);
            } catch (Exception e) {
                // ignore
            }

        }
        return descriptionMustBeSet;
    }

    private static Map<String, String> myIncludeHeaderReplacement;

    private static Map<String, String> getIncludeHeaderReplacement() {
        if (myIncludeHeaderReplacement == null) {
            myIncludeHeaderReplacement = buildincludeHeaderReplacementMap();
        }
        return myIncludeHeaderReplacement;
    }

    /**
     * Builds a map of includes->libraries for all headers not mapping libraryname.h
     * If a include is found more than once in the libraries it is not added to the
     * list If a library has to many includes it is ignored
     *
     * @return
     */
    private static Map<String, String> buildincludeHeaderReplacementMap() {

        Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        LinkedList<String> doubleHeaders = new LinkedList<>();
        TreeMap<String, IPath> libraries = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        libraries.putAll(findAllArduinoManagerLibraries());
        libraries.putAll(findAllPrivateLibraries());
        for (Entry<String, IPath> CurItem : libraries.entrySet()) {
            IPath sourcePath = CurItem.getValue();
            String curLibName = CurItem.getKey();
            if (sourcePath.append(LibraryJson.LIBRARY_SOURCE_FODER).toFile().exists()) {
                sourcePath = sourcePath.append(LibraryJson.LIBRARY_SOURCE_FODER);
            }
            File[] allHeaderFiles = sourcePath.toFile().listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.toLowerCase().endsWith(".h"); //$NON-NLS-1$
                }
            });
            if (ArrayUtils.contains(allHeaderFiles, new File(curLibName + ".h"))) { //$NON-NLS-1$
                // We found a one to one match make sure others do not
                // overrule
                doubleHeaders.add(curLibName);
                map.remove(curLibName + ".h"); //$NON-NLS-1$
            } else if (allHeaderFiles.length <= 10) { // Ignore libraries with
                                                      // to many headers
                for (File CurFile : allHeaderFiles) {
                    String curInclude = CurFile.getName().substring(0, CurFile.getName().length() - 2);

                    // here we have a lib using includes that do not map the
                    // folder name
                    if ((map.get(curInclude) == null) && (!doubleHeaders.contains(curInclude))) {
                        map.put(curInclude, curLibName);
                    } else {
                        doubleHeaders.add(curInclude);
                        map.remove(curInclude);
                    }
                }
            }
        }
        // return KeyValue.makeMap(
        // "AFMotor=Adafruit_Motor_Shield_library\nAdafruit_MotorShield=Adafruit_Motor_Shield_V2_Library\nAdafruit_Simple_AHRS=Adafruit_AHRS\nAdafruit_ADS1015=Adafruit_ADS1X15\nAdafruit_ADXL345_U=Adafruit_ADXL345\n\nAdafruit_LSM303_U=Adafruit_LSM303DLHC\nAdafruit_BMP085_U=Adafruit_BMP085_Unified\nAdafruit_BLE=Adafruit_BluefruitLE_nRF51");
        // //$NON-NLS-1$
        // add adrfruit sensor as this lib is highly used and the header is in
        // libs
        map.put("Adafruit_Sensor", "Adafruit_Unified_Sensor"); //$NON-NLS-1$ //$NON-NLS-2$
        // remove the common hardware libraries so they will never be redirected
        map.remove("SPI"); //$NON-NLS-1$
        map.remove("SoftwareSerial"); //$NON-NLS-1$
        map.remove("HID"); //$NON-NLS-1$
        map.remove("EEPROM"); //$NON-NLS-1$
        return map;
    }

    /**
     * based on a folder inside the library get the folder that starts the library
     * if that path is not found will return path
     * 
     * @param path
     *            path somewhere inside the library
     * @return path to the source code
     */
    public static IPath getLibraryCodeFolder(IPath path) {
        // is it a Sloeber managed Library
        IPath libraryInstallPath = ConfigurationPreferences.getInstallationPathLibraries();
        if (libraryInstallPath.matchingFirstSegments(path) == libraryInstallPath.segmentCount()) {
            return path.uptoSegment(libraryInstallPath.segmentCount() + 2);
        }
        // is it a library of the hardware
        IPath hardwareInstallPath = ConfigurationPreferences.getInstallationPathPackages();
        if (hardwareInstallPath.matchingFirstSegments(path) == hardwareInstallPath.segmentCount()) {
            return path.uptoSegment(hardwareInstallPath.segmentCount() + 6);
        }
        return path;
    }

}
