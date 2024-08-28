package io.sloeber.core.tools;

import static io.sloeber.core.Messages.*;
import static io.sloeber.core.api.Const.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.Activator;
import io.sloeber.core.api.ConfigurationPreferences;
import io.sloeber.core.api.VersionNumber;

public class Libraries {
    public static final String WORKSPACE_LIB_FOLDER = "libraries/"; //$NON-NLS-1$
    public static String INCLUDE = "INCLUDE"; //$NON-NLS-1$
    public static String REMOVE = "REMOVE"; //$NON-NLS-1$






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
                            Activator.log(new Status(IStatus.WARNING, CORE_PLUGIN_ID,
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
                            Activator.log(new Status(IStatus.WARNING, CORE_PLUGIN_ID,
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
                Activator.log(
                        new Status(IStatus.ERROR, CORE_PLUGIN_ID, failed_to_remove_lib.replace(LIB_TAG, CurItem), e));
            }
        }
        return Helpers.removeInvalidIncludeFolders(confdesc);
    }



//    public static boolean adjustProjectDescription(ICConfigurationDescription confdesc,
//            Map<String, List<IPath>> foldersToInclude) {
//        List<IPath> foldersToAddToInclude = foldersToInclude.get(INCLUDE);
//        boolean descriptionMustBeSet = Helpers.addIncludeFolder(confdesc, foldersToAddToInclude, true);
//        List<IPath> foldersToRemoveFromBuildPath = foldersToInclude.get(REMOVE);
//        if ((foldersToRemoveFromBuildPath != null) && (!foldersToRemoveFromBuildPath.isEmpty())) {
//            ICSourceEntry[] sourceEntries = confdesc.getSourceEntries();
//            for (IPath curFile : foldersToRemoveFromBuildPath) {
//                try {
//                    if (!CDataUtil.isExcluded(curFile, sourceEntries)) {
//                        sourceEntries = CDataUtil.setExcluded(curFile, true, true, sourceEntries);
//                        descriptionMustBeSet = true;
//                    }
//
//                } catch (CoreException e1) {
//                    // ignore
//                }
//            }
//            try {
//                confdesc.setSourceEntries(sourceEntries);
//            } catch (Exception e) {
//                // ignore
//            }
//
//        }
//        return descriptionMustBeSet;
//    }





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
        if (libraryInstallPath.isPrefixOf(path)) {
            return path.uptoSegment(libraryInstallPath.segmentCount() + 2);
        }
        // is it a library of the hardware
        IPath hardwareInstallPath = ConfigurationPreferences.getInstallationPathPackages();
        if (hardwareInstallPath.isPrefixOf(path)) {
            return path.uptoSegment(hardwareInstallPath.segmentCount() + 6);
        }
        return path;
    }

}
