package io.sloeber.core.tools;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
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

import io.sloeber.common.Common;
import io.sloeber.common.ConfigurationPreferences;
import io.sloeber.common.Const;
import io.sloeber.common.InstancePreferences;

public class Libraries {
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
	 * board referencing a core then the libraries of the referenced core are
	 * added as well
	 *
	 * @param project
	 *            the project to find all hardware libraries for
	 * @return all the library folder names. May contain empty values. This
	 *         method does not return the full path only the leaves.
	 */
	private static Map<String, IPath> findAllHarwareLibraries(ICConfigurationDescription confdesc) {
		String platformFile = Common.getBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_PLATFORM_FILE,
				Const.EMPTY_STRING);
		IPath platformFolder = new Path(platformFile).removeLastSegments(1);
		Map<String, IPath> ret = findAllSubFolders(platformFolder.append(Const.LIBRARY_PATH_SUFFIX));
		platformFile = Common.getBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_CORE_REFERENCED_PLATFORM_FILE,
				null);
		if (platformFile != null) {
			platformFolder = new Path(platformFile).removeLastSegments(1);
			ret.putAll(findAllSubFolders(platformFolder.append(Const.LIBRARY_PATH_SUFFIX)));
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
				final IFolder folderHandle = project.getFolder(Const.WORKSPACE_LIB_FOLDER + CurItem);
				folderHandle.delete(true, null);
			} catch (CoreException e) {
				Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, Messages.failed_to_remove_lib, e));
			}
		}
		Helpers.removeInvalidIncludeFolders(confdesc);
	}

	public static Map<String, IPath> getAllInstalledLibraries(ICConfigurationDescription confdesc) {
		TreeMap<String, IPath> libraries = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		// lib folder name
		libraries.putAll(findAllArduinoManagerLibraries());
		libraries.putAll(findAllPrivateLibraries());
		libraries.putAll(findAllHarwareLibraries(confdesc));
		return libraries;
	}

	public static void addLibrariesToProject(IProject project, ICConfigurationDescription confdesc,
			Set<String> librariesToAdd) {
		Map<String, IPath> libraries = getAllInstalledLibraries(confdesc);
		libraries.keySet().retainAll(librariesToAdd);
		addLibrariesToProject(project, confdesc, libraries);
	}

	private static void addLibrariesToProject(IProject project, ICConfigurationDescription confdesc,
			Map<String, IPath> libraries) {

		for (Entry<String, IPath> CurItem : libraries.entrySet()) {
			try {

				Helpers.addCodeFolder(project, CurItem.getValue(), Const.WORKSPACE_LIB_FOLDER + CurItem.getKey(),
						confdesc);
			} catch (CoreException e) {
				Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, Messages.import_lib_failed, e));
			}
		}
	}

	// public static void removeLibrariesFromProject(Set<String> libraries) {
	//
	// }

	public static Set<String> getAllLibrariesFromProject(IProject project) {
		IFolder link = project.getFolder(Const.WORKSPACE_LIB_FOLDER);
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
		ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
		if (mngr != null) {
			ICProjectDescription projectDescription = mngr.getProjectDescription(affectedProject, true);
			if (projectDescription != null) {
				ICConfigurationDescription configurationDescription = projectDescription.getActiveConfiguration();
				if (configurationDescription != null) {

					Set<String> UnresolvedIncludedHeaders = getUnresolvedProjectIncludes(affectedProject);
					Set<String> alreadyAddedLibs = getAllLibrariesFromProject(affectedProject);
					Map<String, IPath> availableLibs = getAllInstalledLibraries(configurationDescription);
					UnresolvedIncludedHeaders.removeAll(alreadyAddedLibs);
					availableLibs.keySet().retainAll(UnresolvedIncludedHeaders);
					if (!availableLibs.isEmpty()) {
						// there are possible libraries to add
						Common.log(new Status(IStatus.INFO, Const.CORE_PLUGIN_ID, "list of libraries to add to project " //$NON-NLS-1$
								+ affectedProject.getName() + ": " + availableLibs.keySet().toString())); //$NON-NLS-1$
						addLibrariesToProject(affectedProject, configurationDescription, availableLibs);
						try {
							mngr.setProjectDescription(affectedProject, projectDescription, true, null);
						} catch (CoreException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

}
