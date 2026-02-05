package io.sloeber.arduinoFramework.api;

import static io.sloeber.core.Messages.*;
import static io.sloeber.core.api.Common.*;
import static io.sloeber.core.api.Const.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import com.google.gson.Gson;

import io.sloeber.arduinoFramework.internal.ArduinoLibraryIndex;
import io.sloeber.arduinoFramework.internal.ArduinoLibraryVersion;
import io.sloeber.core.Activator;
import io.sloeber.core.api.ConfigurationPreferences;
import io.sloeber.core.api.Defaults;
import io.sloeber.core.api.IInstallLibraryHandler;
import io.sloeber.core.common.InstancePreferences;
import io.sloeber.core.core.DefaultInstallHandler;
import io.sloeber.core.internal.ArduinoHardwareLibrary;
import io.sloeber.core.internal.ArduinoPrivateHardwareLibraryVersion;
import io.sloeber.core.internal.Example;
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
	private static final Set<String> EXAMPLE_INDICATION_EXTENSIONS = new HashSet<>(
			Arrays.asList("ino", "pde", "cpp", "c")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	private static final Set<String> LIBRARY_INDICATION_FILES = new HashSet<>(
			Arrays.asList(SRC_FODER, EXAMPLES_FOLDER, eXAMPLES_FODER, LIBRARY_PROPERTIES));
	private static final Set<String> IGNORE_FILES = new HashSet<>(Arrays.asList(DOT, DOT + DOT));
	private static final Set<String> CODE_EXTENSIONS = new HashSet<>(Arrays.asList("h", "cpp")); //$NON-NLS-1$ //$NON-NLS-2$

	static private List<IArduinoLibraryIndex> libraryIndices;
	private static IInstallLibraryHandler myInstallLibraryHandler = new DefaultInstallHandler();

	static public List<IArduinoLibraryIndex> getLibraryIndices() {
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

	public static String[] getPrivateLibraryPaths() {
		return InstancePreferences.getPrivateLibraryPaths();
	}

	public static void setPrivateLibraryPaths(String[] libraryPaths) {
		InstancePreferences.setPrivateLibraryPaths(libraryPaths);

	}

	public static void InstallDefaultLibraries(IProgressMonitor monitor) {
		for (IArduinoLibraryIndex libindex : libraryIndices) {

			for (String libraryName : Defaults.DEFAULT_INSTALLED_LIBRARIES) {
				IArduinoLibrary toInstalLib = libindex.getLibrary(libraryName);
				if (toInstalLib != null) {
					IArduinoLibraryVersion toInstalLibVersion = toInstalLib.getNewestVersion();
					IArduinoLibraryVersion instaledLibVersion = toInstalLib.getInstalledVersion();
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
	}

	static public void loadJson(File jsonFile) {
		try (Reader reader = new FileReader(jsonFile)) {
			IArduinoLibraryIndex index = new Gson().fromJson(reader, ArduinoLibraryIndex.class);
			((ArduinoLibraryIndex)index).setJsonFile(jsonFile);
			libraryIndices.add(index);
		} catch (Exception e) {
			Activator.log(new Status(IStatus.ERROR, Activator.getId(),
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
		Map<String, IArduinoLibraryVersion> libsToInstall = LibraryManager
				.getLatestInstallableLibraries(libNamesToInstall);
		if (!libsToInstall.isEmpty()) {
			for (IArduinoLibraryVersion curLib : libsToInstall.values()) {
				install(curLib, new NullProgressMonitor());
			}
		}
	}


	public static IStatus install(IArduinoLibraryVersion inLib, IProgressMonitor monitor) {
		if (!(inLib instanceof ArduinoLibraryVersion)) {
			return Status.error("Trying to install a library that is not a installable library" + inLib.getName()); //$NON-NLS-1$
		}
		ArduinoLibraryVersion lib = (ArduinoLibraryVersion) inLib;
		monitor.setTaskName("Downloading and installing " + lib.getNodeName() + " library."); //$NON-NLS-1$ //$NON-NLS-2$
		if (lib.isInstalled()) {
			return Status.OK_STATUS;
		}
		IStatus ret = PackageManager.downloadAndInstall(lib.getInstallable(), monitor);

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
	public static IStatus unInstall(IArduinoLibraryVersion lib, IProgressMonitor monitor) {
		if (!lib.isInstalled()) {
			return Status.OK_STATUS;
		}

		try {
			deleteDirectory(lib.getInstallPath().removeLastSegments(1));
		} catch (IOException e) {
			return new Status(IStatus.ERROR, Activator.getId(),
					"Failed to remove folder" + lib.getInstallPath().toOSString(), //$NON-NLS-1$
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
		List<IArduinoLibraryIndex> libraryIndices1 = getLibraryIndices();
		for (IArduinoLibraryIndex libraryIndex : libraryIndices1) {
			for (IArduinoLibrary curLib : libraryIndex.getLibraries()) {
				String curLibName = curLib.getNodeName();
				String[] skipArray = { "Base64", "Add others if needed" }; //$NON-NLS-1$ //$NON-NLS-2$
				List<String> skipList = Arrays.asList(skipArray);
				if (!skipList.contains(curLibName)) {
					IArduinoLibraryVersion latestLibVersion = curLib.getNewestVersion();
					if (!latestLibVersion.isInstalled()) {
						IArduinoLibraryVersion installedLibVersion = curLib.getInstalledVersion();
						if (installedLibVersion != null) {
							unInstall(installedLibVersion, null);
						}
						install(latestLibVersion, new NullProgressMonitor());
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
			deleteDirectory(ConfigurationPreferences.getInstallationPathLibraries());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static Map<String, IArduinoLibraryVersion> getLatestInstallableLibraries(Set<String> libnames) {
		Set<String> remainingLibNames = new TreeSet<>(libnames);
		Map<String, IArduinoLibraryVersion> ret = new HashMap<>();
		for (IArduinoLibraryIndex libraryIndex : libraryIndices) {
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
	 * libraries delivered with hardware nor the examples
	 *
	 * @return
	 */
	public static TreeMap<String, IExample> getExamplesLibrary(BoardDescription boardDescriptor) {
		TreeMap<String, IExample> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		Map<String, IArduinoLibraryVersion> installedLibs = getLibrariesAll(boardDescriptor);
		for (IArduinoLibraryVersion curLib : installedLibs.values()) {
			examples.putAll(getExamplesFromFolder(curLib, curLib.getExamplePath().toFile(), 3));
		}

		return examples;
	}

	/**
	 * This method adds a folder recursively examples. Leaves containing ino files
	 * are assumed to be examples
	 *
	 * @param File
	 */
	private static Map<String, IExample> getExamplesFromFolder(IArduinoLibraryVersion lib, File location,
			int maxDepth) {
		Map<String, IExample> examples = new HashMap<>();
		File[] children = location.listFiles();
		if ((children == null) || (maxDepth <= 0)) {
			// Either dir does not exist or is not a directory or we reached the depth
			return examples;
		}
		for (File exampleFolder : children) {
			String extension = getFileExtension(exampleFolder.getName());
			if (exampleFolder.isDirectory()) {
				examples.putAll(getExamplesFromFolder(lib, exampleFolder, maxDepth - 1));
			} else if (EXAMPLE_INDICATION_EXTENSIONS.contains(extension)) {
				IExample newExample = new Example(lib, new Path(location.toString()));
				examples.put(newExample.getID(), newExample);
			}
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
	public static TreeMap<String, IExample> getExamplesAll(BoardDescription boardDescriptor) {
		// Get the examples arduinoPlugin/examples
		TreeMap<String, IExample> examples =getExamplesFromIDE();
		// get the examples related to libraries and boards
		examples.putAll(getExamplesLibrary(boardDescriptor));
		return examples;
	}

	public static TreeMap<String, IExample> getExamplesFromIDE() {
		TreeMap<String, IExample> examples = new TreeMap<>();
		// Get the examples arduinoPlugin/examples
		IPath exampleFolder = ConfigurationPreferences.getInstallationPathExamples();
		examples.putAll(getExamplesFromFolder(null, exampleFolder.toFile(), 5));
		return examples;
	}

	public static IStatus updateLibraries(Set<IArduinoLibraryVersion> toUnInstallLibs,
			Set<IArduinoLibraryVersion> toInstallLibs, IProgressMonitor monitor, MultiStatus status) {
		for (IArduinoLibraryVersion curLib : toUnInstallLibs) {
			status.add(unInstall(curLib, monitor));
		}
		for (IArduinoLibraryVersion curLib : toInstallLibs) {
			status.add(install(curLib, monitor));
		}
		return status;
	}

	/**
	 * A convenience (and downward compatibility method of
	 * getLibrariesAll(BoardDescription boardDescriptor, true) {
	 *
	 * @param confDesc can be null
	 * @return A map of FQN IArduinoLibraryVersion
	 */
	public static TreeMap<String, IArduinoLibraryVersion> getLibrariesAll(BoardDescription boardDescriptor) {
		return getLibrariesAll( boardDescriptor, true);
	}

	/**
	 * Given a sloeber configuration provide all the libraries that can be used by
	 * this sketch This boils down to all libraries maintained by the Library
	 * manager plus all the libraries provided by the core plus all the libraries
	 * provided by the personal libraries
	 *
	 * @param confDesc can be null
	 * @return if keyIsFQN is true: A map of FQN IArduinoLibraryVersion
	 * if keyIsFQN is false: A map of location IArduinoLibraryVersion
	 */
	public static TreeMap<String, IArduinoLibraryVersion> getLibrariesAll(BoardDescription boardDescriptor, boolean keyIsFQN) {
		TreeMap<String, IArduinoLibraryVersion> libraries = new TreeMap<>();
		libraries.putAll(getLibrariesdManaged(keyIsFQN));
		libraries.putAll(getLibrariesPrivate(keyIsFQN));
		if (boardDescriptor != null) {
			libraries.putAll(getLibrariesHarware(boardDescriptor,keyIsFQN));
		}
		return libraries;
	}

	private static Map<String, IArduinoLibraryVersion> getLibrariesdManaged(boolean keyIsFQN) {
		Map<String, IArduinoLibraryVersion> ret = new HashMap<>();
		for (IArduinoLibraryIndex libindex : libraryIndices) {
			for (IArduinoLibrary curLib : libindex.getLibraries()) {
				IArduinoLibraryVersion instVersion = curLib.getInstalledVersion();
				if (instVersion != null) {
					if (keyIsFQN) {
						ret.put(instVersion.getFQN().toPortableString(), instVersion);
					} else {
						ret.put(instVersion.getInstallPath().toPortableString(), instVersion);
					}
				}
			}
		}
		return ret;
	}

	private static Map<String, IArduinoLibraryVersion> getLibrariesPrivate(boolean keyIsFQN) {
		Map<String, IArduinoLibraryVersion> ret = new HashMap<>();
		String privateLibPaths[] = InstancePreferences.getPrivateLibraryPaths();
		for (String curLibPath : privateLibPaths) {
			ret.putAll(getLibrariesFromFolder(new Path(curLibPath), 2, false,true,keyIsFQN));
		}
		return ret;

	}

	/**
	 * for a given folder return all subfolders
	 *
	 * @param ipath the folder you want the subfolders off
	 * @param keyIsFQN
	 * @return The subfolders of the ipath folder. May contain empty values. This
	 *         method returns a key value pair of key equals foldername and value
	 *         equals full path.
	 */
	private static Map<String, IArduinoLibraryVersion> getLibrariesFromFolder(IPath ipath, int depth,
			boolean isHardwareLib,boolean isPrivate, boolean keyIsFQN) {
		if (ConfigurationPreferences.getInstallationPathLibraries().isPrefixOf(ipath)) {
			System.err.println("The method findAllPrivateLibs should not be called on Library manager installed libs"); //$NON-NLS-1$
		}
		String[] children = ipath.toFile().list();
		Map<String, IArduinoLibraryVersion> ret = new HashMap<>();
		if (children == null) {
			// Either dir does not exist or is not a directory
			return ret;
		}

		// if the folder contains a *.h or *.cpp file it is considered a
		// library itself
		for (String curChild : children) {
			if (IGNORE_FILES.contains(curChild)) {
				continue;
			}
			String fileExt = (new Path(curChild)).getFileExtension();
			if (LIBRARY_INDICATION_FILES.contains(curChild) || CODE_EXTENSIONS.contains(fileExt)) {
				IArduinoLibraryVersion retVersion = isHardwareLib?new ArduinoHardwareLibrary(ipath):new ArduinoPrivateHardwareLibraryVersion(ipath);
				String key=keyIsFQN?retVersion.getFQN().toPortableString():retVersion.getInstallPath().toPortableString();
				ret.put(key, retVersion);

				return ret;
			}
		}
		if (depth <= 0) {
			return ret;
		}
		// otherwise assume all subfolders are libraries
		for (String curFolder : children) {
			// Get filename of file or directory
			IPath LibPath = ipath.append(curFolder);
			File LibPathFile = LibPath.toFile();
			if (LibPathFile.isDirectory() && !LibPathFile.isHidden()) {
				ret.putAll(getLibrariesFromFolder(LibPath, depth - 1, isHardwareLib,isPrivate,keyIsFQN));
			}
		}
		return ret;
	}

	/**
	 * Searches all the hardware dependent libraries of a project. If this is a
	 * board referencing a core then the libraries of the referenced core are added
	 * as well
	 * @param keyIsFQN
	 *
	 * @param project the project to find all hardware libraries for
	 * @return all the library folder names. May contain empty values.
	 */
	public static Map<String, IArduinoLibraryVersion> getLibrariesHarware(BoardDescription boardDescriptor, boolean keyIsFQN) {
		Map<String, IArduinoLibraryVersion> ret = new HashMap<>();
		// first add the referenced
		IPath libPath = boardDescriptor.getReferencedCoreLibraryPath();
		if (libPath != null) {
			ret.putAll(getLibrariesFromFolder(libPath, 1, true,boardDescriptor.isPrivate(),keyIsFQN));
		}
		// then add the referencing
		libPath = boardDescriptor.getReferencingLibraryPath();
		if (libPath != null) {
			ret.putAll(getLibrariesFromFolder(libPath, 1, true,boardDescriptor.isPrivate(),keyIsFQN));
		}
		return ret;
	}

	public static IArduinoLibraryVersion getLibraryVersionFromLocation(IFolder libFolder,BoardDescription boardDescriptor) {
		if (boardDescriptor != null) {
			IPath libPath=boardDescriptor.getReferencedCoreLibraryPath();
			if(libPath!=null && libPath.isPrefixOf(libFolder.getLocation())) {
				return getLibrariesHarware(boardDescriptor,false).get(libFolder.getLocation().toPortableString());
			}
		}

		if(ConfigurationPreferences.getInstallationPathLibraries().isPrefixOf(libFolder.getLocation())) {
			return getLibrariesdManaged(false).get(libFolder.getLocation().toPortableString());
		}

		return getLibrariesPrivate(false).get(libFolder.getLocation().toPortableString());
	}

	public static IArduinoLibraryVersion getLibraryVersionFromFQN(String FQNLibName, BoardDescription boardDescriptor) {
		String[] fqnParts = FQNLibName.split(SLACH);
		if (fqnParts.length < 3) {
			return null;
		}
		if (!SLOEBER_LIBRARY_FQN.equals(fqnParts[0])) {
			// this is not a library
			return null;
		}
		if (MANAGED.equals(fqnParts[1])) {
			if (BOARD.equals(fqnParts[2])) {
				if (boardDescriptor == null) {
					return null;
				}
				return getLibrariesHarware(boardDescriptor,true).get(FQNLibName);
			}
			return getLibrariesdManaged(true).get(FQNLibName);
		}
		if (PRIVATE.equals(fqnParts[1])) {
			return getLibrariesPrivate(true).get(FQNLibName);
		}
		return null;
	}

	/**
	 * Remove a lib based on the name of the lib
	 *
	 * @param libName the name of the lib
	 * @return true if the lib has been removed or was not found
	 *          false if the lib was found and the removal failed.
	 */
	public static boolean uninstallLibrary(String libName) {
		Map<String, IArduinoLibraryVersion> installedLibs=getLibrariesAll(null);
		IPath libFQN=ArduinoLibraryVersion.calculateFQN(libName);
		IArduinoLibraryVersion libVersion = installedLibs.get(libFQN.toString());
		if(libVersion==null) {
			return true;

		}
		return unInstall(libVersion, new NullProgressMonitor()).isOK();
	}

}