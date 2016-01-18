package it.baeyens.arduino.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.index.IIndexFile;
import org.eclipse.cdt.core.index.IIndexInclude;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.common.ConfigurationPreferences;

public class ArduinoLibraries {
    /**
     * for a given folder return all subfolders
     * 
     * @param ipath
     *            the folder you want the subfolders off
     * @return The subfolders of the ipath folder. May contain empty values.
     *         This method returns a key value pair of key equals foldername and
     *         value equals full path.
     */
    private static Map<String, IPath> findAllSubFolders(IPath ipath) {
	String[] children = ipath.toFile().list();
	Map<String, IPath> ret = new HashMap<>();
	if (children == null) {
	    // Either dir does not exist or is not a directory
	} else {
	    for (String curFolder : children) {
		// Get filename of file or directory
		IPath LibPath = ipath.append(curFolder);
		if (LibPath.toFile().isDirectory()) {
		    ret.put(curFolder, LibPath);
		}
	    }
	}
	return ret;
    }

    /**
     * Searches all the hardware dependent libraries of a project.
     * 
     * @param project
     *            the project to find all hardware libraries for
     * @return all the library folder names. May contain empty values. This
     *         method does not return the full path only the leaves.
     */
    public static Map<String, IPath> findAllHarwareLibraries(ICConfigurationDescription confdesc) {
	Path platformFile = new Path(Common.getBuildEnvironmentVariable(confdesc,
		ArduinoConst.ENV_KEY_JANTJE_PLATFORM_FILE, ArduinoConst.EMPTY_STRING));
	return findAllSubFolders(platformFile.removeLastSegments(1).append(ArduinoConst.LIBRARY_PATH_SUFFIX));
    }

    public static Map<String, IPath> findAllPrivateLibraries() {
	Map<String, IPath> ret = new HashMap<>();
	String privateLibPaths[] = ArduinoInstancePreferences.getPrivateLibraryPaths();
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
			if (versions.length == 1) {// There can only be 1
						   // version of a lib
			    ret.put(curLib, Lib_root.append(versions[0]));
			}
		    }
		}
	    }
	}
	return ret;

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
    public static void removeLibrariesFromProject(IProject project, ICConfigurationDescription confdesc,
	    Set<String> libraries) {
	for (String CurItem : libraries) {
	    try {
		final IFolder folderHandle = project.getFolder(ArduinoConst.WORKSPACE_LIB_FOLDER + CurItem);
		folderHandle.delete(true, null);
	    } catch (CoreException e) {
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, Messages.failed_to_remove_lib, e));
	    }
	}
	ArduinoHelpers.removeInvalidIncludeFolders(confdesc);
    }

    private static HashMap<String, IPath> getAllInstalledLibraries(ICConfigurationDescription confdesc) {
	HashMap<String, IPath> libraries = new HashMap<>(); // a hashmap libname
							    // lib folder name
	libraries.putAll(findAllArduinoManagerLibraries());
	libraries.putAll(findAllPrivateLibraries());
	libraries.putAll(findAllHarwareLibraries(confdesc));
	return libraries;
    }

    public static void addLibrariesToProject(IProject project, ICConfigurationDescription confdesc,
	    Set<String> librariesToAdd) {
	HashMap<String, IPath> libraries = getAllInstalledLibraries(confdesc);
	libraries.keySet().retainAll(librariesToAdd);
	addLibrariesToProject(project, confdesc, libraries);
    }

    private static void addLibrariesToProject(IProject project, ICConfigurationDescription confdesc,
	    HashMap<String, IPath> libraries) {

	for (Entry<String, IPath> CurItem : libraries.entrySet()) {
	    try {

		ArduinoHelpers.addCodeFolder(project, CurItem.getValue(),
			ArduinoConst.WORKSPACE_LIB_FOLDER + CurItem.getKey(), confdesc);
	    } catch (CoreException e) {
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, Messages.import_lib_failed, e));
	    }
	}
    }

    // public static void removeLibrariesFromProject(Set<String> libraries) {
    //
    // }

    public static Set<String> getAllLibrariesFromProject(IProject project) {
	IFolder link = project.getFolder(ArduinoConst.WORKSPACE_LIB_FOLDER);
	Set<String> ret = new TreeSet<>();
	try {
	    if (link.exists()) {
		for (IResource curResource : link.members()) {
		    ret.add(curResource.getName());
		}
	    }
	} catch (CoreException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}

	return ret;
    }

    public static void reAttachLibrariesToProject(IProject project) {
	Set<String> AllLibrariesOriginallyUsed = getAllLibrariesFromProject(project);
	ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
	ICProjectDescription projectDescription = mngr.getProjectDescription(project, true);
	ICConfigurationDescription configurationDescriptions[] = projectDescription.getConfigurations();
	for (ICConfigurationDescription CurItem : configurationDescriptions) {
	    addLibrariesToProject(project, CurItem, AllLibrariesOriginallyUsed);
	}
	try {
	    mngr.setProjectDescription(project, projectDescription, true, null);
	} catch (CoreException e) {
	    e.printStackTrace();
	}
    }

    public static void reAttachLibrariesToProject(ICConfigurationDescription confdesc) {
	Set<String> AllLibrariesOriginallyUsed = getAllLibrariesFromProject(
		confdesc.getProjectDescription().getProject());
	addLibrariesToProject(confdesc.getProjectDescription().getProject(), confdesc, AllLibrariesOriginallyUsed);
    }

    public static void addLibrariesToProject(IProject project, Set<String> selectedLibraries) {
	ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
	ICProjectDescription projectDescription = mngr.getProjectDescription(project, true);
	ICConfigurationDescription configurationDescription = projectDescription.getActiveConfiguration();
	addLibrariesToProject(project, configurationDescription, selectedLibraries);
	try {
	    mngr.setProjectDescription(project, projectDescription, true, null);
	} catch (CoreException e) {
	    e.printStackTrace();
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
    public static void removeLibrariesFromProject(IProject project, Set<String> selectedLibraries) {
	ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
	ICProjectDescription projectDescription = mngr.getProjectDescription(project, true);
	ICConfigurationDescription configurationDescriptions[] = projectDescription.getConfigurations();
	for (ICConfigurationDescription CurItem : configurationDescriptions) {
	    removeLibrariesFromProject(project, CurItem, selectedLibraries);
	}
	try {
	    mngr.setProjectDescription(project, projectDescription, true, null);
	} catch (CoreException e) {
	    e.printStackTrace();
	}

    }

    private static Set<String> getUnresolvedProjectIncludes(IProject iProject) {
	Set<String> ret = new TreeSet<>();
	ICProject tt = CoreModel.getDefault().create(iProject);

	try {
	    IIndex index = CCorePlugin.getIndexManager().getIndex(tt);
	    index.acquireReadLock();

	    IIndexFile allFiles[] = index.getFilesWithUnresolvedIncludes();
	    for (IIndexFile curUnesolvedIncludeFile : allFiles) {
		IIndexInclude includes[] = curUnesolvedIncludeFile.getIncludes();
		for (IIndexInclude curinclude : includes) {
		    if (curinclude.isActive() && !curinclude.isResolved()) {
			ret.add(new Path(curinclude.getName()).removeFileExtension().toString());
		    }
		}
	    }
	    index.releaseReadLock();
	} catch (CoreException e1) {
	    // TODO Auto-generated catch block
	    e1.printStackTrace();
	} catch (InterruptedException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	return ret;
    }

    public static void checkLibraries(IProject affectedProject) {
	ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
	ICProjectDescription projectDescription = mngr.getProjectDescription(affectedProject, true);
	ICConfigurationDescription configurationDescription = projectDescription.getActiveConfiguration();

	Set<String> UnresolvedIncludedHeaders = getUnresolvedProjectIncludes(affectedProject);
	Set<String> alreadyAddedLibs = getAllLibrariesFromProject(affectedProject);
	HashMap<String, IPath> availableLibs = getAllInstalledLibraries(configurationDescription);
	UnresolvedIncludedHeaders.removeAll(alreadyAddedLibs);
	availableLibs.keySet().retainAll(UnresolvedIncludedHeaders);
	if (!availableLibs.isEmpty()) {
	    // there are possible libraries to add
	    Common.log(new Status(IStatus.INFO, Common.CORE_PLUGIN_ID, "list of libraries to add to project "
		    + affectedProject.getName() + ": " + availableLibs.keySet().toString()));
	    addLibrariesToProject(affectedProject, configurationDescription, availableLibs);
	    try {
		mngr.setProjectDescription(affectedProject, projectDescription, true, null);
	    } catch (CoreException e) {
		e.printStackTrace();
	    }
	}
    }

}
