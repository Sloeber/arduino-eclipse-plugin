package it.baeyens.arduino.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.cdt.core.model.CoreModel;
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
     * @return The subfolders of the ipath folder. May contain empty values. This method returns a key value pair of key equals foldername and value
     *         equals full path.
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
     * @return all the library folder names. May contain empty values. This method does not return the full path only the leaves.
     */
    public static Map<String, IPath> findAllHarwareLibraries(ICConfigurationDescription confdesc) {
	Path platformFile = new Path(
		Common.getBuildEnvironmentVariable(confdesc, ArduinoConst.ENV_KEY_JANTJE_PLATFORM_FILE, ArduinoConst.EMPTY_STRING));
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
			if (versions.length == 1) {// There can only be 1 version of a lib
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
    public static void removeLibrariesFromProject(IProject project, ICConfigurationDescription confdesc, Set<String> libraries) {
	for (String CurItem : libraries) {
	    try {
		final IFolder folderHandle = project.getFolder(ArduinoConst.WORKSPACE_LIB_FOLDER + CurItem);
		folderHandle.delete(true, null);
	    } catch (CoreException e) {
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to remove library ", e));
	    }
	}
	ArduinoHelpers.removeInvalidIncludeFolders(confdesc);
    }

    public static void addLibrariesToProject(IProject project, ICConfigurationDescription confdesc, Set<String> librariesToAdd) {
	HashMap<String, IPath> libraries = new HashMap<>(); // a hashmap libname lib folder name
	libraries.putAll(findAllArduinoManagerLibraries());
	libraries.putAll(findAllPrivateLibraries());
	libraries.putAll(findAllHarwareLibraries(confdesc));

	for (String CurItem : librariesToAdd) {
	    try {
		if (libraries.containsKey(CurItem)) {
		    ArduinoHelpers.addCodeFolder(project, libraries.get(CurItem), ArduinoConst.WORKSPACE_LIB_FOLDER + CurItem, confdesc);
		} else {
		    // TODO add check whether this is actually a library
		    // in case of code added via samples the plugin thinks a library needs to be added. However this is not a library but just a
		    // folder
		    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "The library " + CurItem + " is not valid for the project "
			    + project.getName() + " and configuration " + confdesc.getName()));
		}
	    } catch (CoreException e) {
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to import library ", e));
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
	Set<String> AllLibrariesOriginallyUsed = getAllLibrariesFromProject(confdesc.getProjectDescription().getProject());
	addLibrariesToProject(confdesc.getProjectDescription().getProject(), confdesc, AllLibrariesOriginallyUsed);
    }

    public static void addLibrariesToProject(IProject project, Set<String> selectedLibraries) {
	ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
	ICProjectDescription projectDescription = mngr.getProjectDescription(project, true);
	ICConfigurationDescription configurationDescriptions[] = projectDescription.getConfigurations();
	for (ICConfigurationDescription CurItem : configurationDescriptions) {
	    addLibrariesToProject(project, CurItem, selectedLibraries);
	}
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

}
