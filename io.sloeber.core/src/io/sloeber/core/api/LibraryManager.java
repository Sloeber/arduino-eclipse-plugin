package io.sloeber.core.api;

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

import io.sloeber.core.Activator;
import io.sloeber.core.api.Json.ArduinoLibrary;
import io.sloeber.core.api.Json.ArduinoLibraryIndex;
import io.sloeber.core.api.Json.ArduinoLibraryVersion;
import io.sloeber.core.common.InstancePreferences;
import io.sloeber.core.core.DefaultInstallHandler;
import io.sloeber.core.internal.ArduinoHardwareLibrary;
import io.sloeber.core.internal.ArduinoPrivateLibraryVersion;
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
				if (toInstalLib != null) {
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
			deleteDirectory(lib.getInstallPath().removeLastSegments(1));
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
				String curLibName = curLib.getNodeName();
				String[] skipArray = { "Base64", "Add others if needed" }; //$NON-NLS-1$ //$NON-NLS-2$
				List<String> skipList = Arrays.asList(skipArray);
				if (!skipList.contains(curLibName)) {
					ArduinoLibraryVersion latestLibVersion = curLib.getNewestVersion();
					if (!latestLibVersion.isInstalled()) {
						ArduinoLibraryVersion installedLibVersion = curLib.getInstalledVersion();
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
	 * libraries delivered with hardware nor the examples
	 *
	 * @return
	 */
	public static TreeMap<String, IExample> getExamplesLibrary(BoardDescription boardDescriptor) {
		TreeMap<String, IExample> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

		Map<String, IArduinoLibraryVersion> installedLibs = getLibrariesAll(boardDescriptor);
		for (IArduinoLibraryVersion curLib : installedLibs.values()) {
			examples.putAll(getExamplesFromFolder(curLib, curLib.getExamplePath().toFile(), 2));
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

	/**
	 * Given a sloeber configuration provide all the libraries that can be used by
	 * this sketch This boils down to all libraries maintained by the Library
	 * manager plus all the libraries provided by the core plus all the libraries
	 * provided by the personal libraries
	 *
	 * @param confDesc can be null
	 * @return
	 */
	public static TreeMap<String, IArduinoLibraryVersion> getLibrariesAll(BoardDescription boardDescriptor) {
		TreeMap<String, IArduinoLibraryVersion> libraries = new TreeMap<>();
		libraries.putAll(getLibrariesdManaged());
		libraries.putAll(getLibrariesPrivate());
		if (boardDescriptor != null) {
			libraries.putAll(getLibrariesHarware(boardDescriptor));
		}
		return libraries;
	}

	private static Map<String, IArduinoLibraryVersion> getLibrariesdManaged() {
		Map<String, IArduinoLibraryVersion> ret = new HashMap<>();
		for (ArduinoLibraryIndex libindex : libraryIndices) {
			for (ArduinoLibrary curLib : libindex.getLibraries()) {
				IArduinoLibraryVersion instVersion = curLib.getInstalledVersion();
				if (instVersion != null) {
					ret.put(instVersion.getFQN().toPortableString(), instVersion);
				}
			}
		}
		return ret;
	}

	private static Map<String, IArduinoLibraryVersion> getLibrariesPrivate() {
		Map<String, IArduinoLibraryVersion> ret = new HashMap<>();
		String privateLibPaths[] = InstancePreferences.getPrivateLibraryPaths();
		for (String curLibPath : privateLibPaths) {
			ret.putAll(getLibrariesFromFolder(new Path(curLibPath), 2, false,true));
		}
		return ret;

	}

	/**
	 * for a given folder return all subfolders
	 *
	 * @param ipath the folder you want the subfolders off
	 * @return The subfolders of the ipath folder. May contain empty values. This
	 *         method returns a key value pair of key equals foldername and value
	 *         equals full path.
	 */
	private static Map<String, IArduinoLibraryVersion> getLibrariesFromFolder(IPath ipath, int depth,
			boolean isHardwareLib,boolean isPrivate) {
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
				if (isHardwareLib) {
					IArduinoLibraryVersion retVersion = new ArduinoHardwareLibrary(ipath);
					ret.put(retVersion.getFQN().toPortableString(), retVersion);
				} else {
					IArduinoLibraryVersion retVersion = new ArduinoPrivateLibraryVersion(ipath);
					ret.put(retVersion.getFQN().toPortableString(), retVersion);
				}

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
				ret.putAll(getLibrariesFromFolder(LibPath, depth - 1, isHardwareLib,isPrivate));
			}
		}
		return ret;
	}

	/**
	 * Searches all the hardware dependent libraries of a project. If this is a
	 * board referencing a core then the libraries of the referenced core are added
	 * as well
	 *
	 * @param project the project to find all hardware libraries for
	 * @return all the library folder names. May contain empty values.
	 */
	private static Map<String, IArduinoLibraryVersion> getLibrariesHarware(BoardDescription boardDescriptor) {
		Map<String, IArduinoLibraryVersion> ret = new HashMap<>();
		// first add the referenced
		IPath libPath = boardDescriptor.getReferencedCoreLibraryPath();
		if (libPath != null) {
			ret.putAll(getLibrariesFromFolder(libPath, 1, true,boardDescriptor.isPrivate()));
		}
		// then add the referencing
		libPath = boardDescriptor.getReferencingLibraryPath();
		if (libPath != null) {
			ret.putAll(getLibrariesFromFolder(libPath, 1, true,boardDescriptor.isPrivate()));
		}
		return ret;
	}

	public static IArduinoLibraryVersion getLibraryVersionFromLocation(IFolder libFolder,BoardDescription boardDescriptor) {
		// TODO Auto-generated method stub

		if (boardDescriptor != null) {
			IPath libPath=boardDescriptor.getReferencedCoreLibraryPath();
			if(libPath!=null && libPath.isPrefixOf(libFolder.getLocation())) {
				return getLibrariesHarware(boardDescriptor).get(libFolder.getName());
			}
		}

		if(ConfigurationPreferences.getInstallationPathLibraries().isPrefixOf(libFolder.getLocation())) {
			return getLibrariesdManaged().get(libFolder.getName());
		}

		return getLibrariesPrivate().get(libFolder.getName());
	}

}