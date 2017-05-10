package io.sloeber.core.tools;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
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

import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.Const;
import io.sloeber.core.common.InstancePreferences;
import io.sloeber.core.managers.Library;

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
				new String());
		IPath LibraryFolder = new Path(platformFile).removeLastSegments(1).append(Const.LIBRARY_PATH_SUFFIX);
		Map<String, IPath> ret = findAllSubFolders(LibraryFolder);
		String platform = Common.getBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_CORE_REFERENCED_PLATFORM,
				null);
		if (platform != null) {
			LibraryFolder = new Path(platformFile).append(Const.LIBRARY_PATH_SUFFIX);
			ret.putAll(findAllSubFolders(LibraryFolder));
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
							Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
									Messages.EmptyLibFolder.replace("${LIB}", curLib))); //$NON-NLS-1$
							break;
						case 1:// There should only be 1
							ret.put(curLib, Lib_root.append(versions[0]));
							break;
						default:// multiple lib versions are installed take the
								// latest
							int highestVersion = Version.getHighestVersionn(versions);
							ret.put(curLib, Lib_root.append(versions[highestVersion]));
							Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
									Messages.MultipleVersionsOfLib.replace("${LIB}", curLib))); //$NON-NLS-1$
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
		Map<String, String> includeHeaderReplacement = getIncludeHeaderReplacement();
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
					for (Map.Entry<String, String> entry : includeHeaderReplacement.entrySet()) {
						if (UnresolvedIncludedHeaders.contains(entry.getKey())) {
							UnresolvedIncludedHeaders.add(entry.getValue());
						}
					}

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

	private static Map<String, String> myIncludeHeaderReplacement;

	private static Map<String, String> getIncludeHeaderReplacement() {
		if (myIncludeHeaderReplacement == null) {
			myIncludeHeaderReplacement = buildincludeHeaderReplacementMap();
		}
		return myIncludeHeaderReplacement;
	}

	/**
	 * Builds a map of includes->libraries foe all headers not mapping
	 * libraryname.h If a include is found more than once in the libraries it is
	 * not added to the list
	 *
	 * @return
	 */
	@SuppressWarnings("nls")
	private static Map<String, String> buildincludeHeaderReplacementMap() {

		Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		LinkedList<String> doubleHeaders = new LinkedList<>();
		TreeMap<String, IPath> libraries = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		libraries.putAll(findAllArduinoManagerLibraries());
		libraries.putAll(findAllPrivateLibraries());
		for (Entry<String, IPath> CurItem : libraries.entrySet()) {
			IPath sourcePath = CurItem.getValue();
			String curLibName = CurItem.getKey();
			if (sourcePath.append(Library.LIBRARY_SOURCE_FODER).toFile().exists()) {
				sourcePath = sourcePath.append(Library.LIBRARY_SOURCE_FODER);
			}
			for (String CurFile : sourcePath.toFile().list()) {
				// only look at header files
				if (CurFile.endsWith(".h")) {//$NON-NLS-1$
					String curInclude = CurFile.substring(0, CurFile.length() - 2);

					if (curInclude.equals(curLibName)) {
						// We found a one to one match make sure others do not
						// overrule
						doubleHeaders.add(curLibName);
						map.remove(curInclude);
					} else {
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
		}
		// return KeyValue.makeMap(
		// "AFMotor=Adafruit_Motor_Shield_library\nAdafruit_MotorShield=Adafruit_Motor_Shield_V2_Library\nAdafruit_Simple_AHRS=Adafruit_AHRS\nAdafruit_ADS1015=Adafruit_ADS1X15\nAdafruit_ADXL345_U=Adafruit_ADXL345\n\nAdafruit_LSM303_U=Adafruit_LSM303DLHC\nAdafruit_BMP085_U=Adafruit_BMP085_Unified\nAdafruit_BLE=Adafruit_BluefruitLE_nRF51");
		// //$NON-NLS-1$
		// add adrfruit sensor as this lib is highly used and the header is in
		// libs
		map.put("Adafruit_Sensor", "Adafruit_Unified_Sensor");
		// remove the common hardware libraries so they will never be redirected
		map.remove("SPI");
		map.remove("SoftwareSerial");
		map.remove("HID");
		map.remove("EEPROM");
		return map;
	}
}
