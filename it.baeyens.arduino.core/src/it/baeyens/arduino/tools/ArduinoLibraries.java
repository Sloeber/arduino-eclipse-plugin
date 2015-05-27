package it.baeyens.arduino.tools;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;

import java.io.File;
import java.net.URI;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class ArduinoLibraries {
    /**
     * for a given folder return all subfolders
     * 
     * @param ipath
     *            the folder you want the subfolders off
     * @return The subfolders of the ipath folder. May contain empty values. This method does not return the full path only the leaves.
     */
    private static Set<String> findAllSubFolders(IPath ipath) {
	File LibRoot = ipath.toFile();
	File LibFolder;
	String[] children = LibRoot.list();
	Set<String> ret = new TreeSet<String>();
	if (children == null) {
	    // Either dir does not exist or is not a directory
	} else {
	    for (int i = 0; i < children.length; i++) {
		// Get filename of file or directory
		LibFolder = ipath.append(children[i]).toFile();
		if (LibFolder.isDirectory()) {
		    ret.add(children[i]);
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
    public static Set<String> findAllHarwareLibraries(IProject project) {
	IPathVariableManager pathMan = project.getPathVariableManager();

	URI HardwareLibrarURI = pathMan.resolveURI(pathMan.getURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_HARDWARE_LIB));
	return findAllSubFolders(URIUtil.toPath(HardwareLibrarURI));

    }

    public static Set<String> findAllUserLibraries(IProject project) {
	IPathVariableManager pathMan = project.getPathVariableManager();
	URI PrivateLibraryURI = pathMan.resolveURI(pathMan.getURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_PRIVATE_LIB));
	return findAllSubFolders(URIUtil.toPath(PrivateLibraryURI));

    }

    public static Set<String> findAllArduinoLibraries(IProject project) {
	IPathVariableManager pathMan = project.getPathVariableManager();

	URI ArduinoLibraryURI = pathMan.resolveURI(pathMan.getURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_ARDUINO_LIB));
	return findAllSubFolders(URIUtil.toPath(ArduinoLibraryURI));

    }
    
    /**
     * Removes a set of libraries from a project
     * 
     * @param project	the project from which to remove libraries
     * @param confdesc	the configuration from which to remove libraries
     * @param libraries set of libraries to remove
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

    public static void addLibrariesToProject(IProject project, ICConfigurationDescription confdesc, Set<String> libraries) {
	Set<String> hardwareLibraries = findAllHarwareLibraries(project);
	Set<String> userLibraries = findAllUserLibraries(project);
	Set<String> arduinoLibraries = findAllArduinoLibraries(project);

	// ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
	// ICProjectDescription projectDescription = mngr.getProjectDescription(project, true);
	// ICConfigurationDescription configurationDescriptions[] = projectDescription.getConfigurations();

	for (String CurItem : libraries) {
	    try {
		if (hardwareLibraries.contains(CurItem))
		    ArduinoHelpers.addCodeFolder(project, ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_HARDWARE_LIB, CurItem,
			    ArduinoConst.WORKSPACE_LIB_FOLDER + CurItem, confdesc);
		else if (userLibraries.contains(CurItem)) {
		    ArduinoHelpers.addCodeFolder(project, ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_PRIVATE_LIB, CurItem,
			    ArduinoConst.WORKSPACE_LIB_FOLDER + CurItem, confdesc);
		} else if (arduinoLibraries.contains(CurItem)) {
		    ArduinoHelpers.addCodeFolder(project, ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_ARDUINO_LIB, CurItem,
			    ArduinoConst.WORKSPACE_LIB_FOLDER + CurItem, confdesc);
		} else {
		    // TODO add check whether this is actually a library
		    // in case of code added via samples the plugin thinks a library needs to be added. However this is not a library but just a
		    // folder
		    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "The library " + CurItem + " is not valid for this board."));
		}
	    } catch (CoreException e) {
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to import library ", e));
	    }
	}
	// try {
	// mngr.setProjectDescription(project, confdesc, true, null);
	// } catch (CoreException e) {
	// e.printStackTrace();
	// }
    }

    // public static void removeLibrariesFromProject(Set<String> libraries) {
    //
    // }

    public static Set<String> getAllLibrariesFromProject(IProject project) {
	IFolder link = project.getFolder(ArduinoConst.WORKSPACE_LIB_FOLDER);
	Set<String> ret = new TreeSet<String>();
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
     * @param project	the project from which to remove libraries
     * @param libraries set of libraries to remove
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
