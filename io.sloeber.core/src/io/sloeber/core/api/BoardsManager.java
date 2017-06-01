package io.sloeber.core.api;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.core.api.BoardsManager.PlatformTree.IndexFile;
import io.sloeber.core.api.BoardsManager.PlatformTree.InstallableVersion;
import io.sloeber.core.api.BoardsManager.PlatformTree.Platform;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.Const;
import io.sloeber.core.common.InstancePreferences;
import io.sloeber.core.managers.ArduinoPlatform;
import io.sloeber.core.managers.Board;
import io.sloeber.core.managers.Manager;
import io.sloeber.core.managers.Package;
import io.sloeber.core.managers.PackageIndex;
import io.sloeber.core.tools.Messages;
import io.sloeber.core.tools.TxtFile;

/**
 * This class groups both boards installed by the hardware manager and boards
 * installed locally.
 *
 * @author jantje
 *
 */
public class BoardsManager {
	private static final String INO = "ino"; //$NON-NLS-1$
	private static final String PDE = "pde";//$NON-NLS-1$
	private static final String CPP = "cpp";//$NON-NLS-1$
	private static final String C = "c";//$NON-NLS-1$
	private static final String LIBRARY_PATH_SUFFIX = "libraries"; //$NON-NLS-1$

	/**
	 * Gets the board descriptor based on the information provided. If
	 * jsonFileName="local" the board is assumed not to be installed by the
	 * boards manager. Otherwise the boardsmanager is queried to find the board
	 * descriptor. In this case the latest installed board will be returned
	 *
	 * @param jsonFileName
	 *            equals to "local" or the name of the json file used by the
	 *            boards manager to install the boards
	 * @param packageName
	 *            if jsonFileName equals "local" the filename of the boards.txt
	 *            containing the boards. otherwise the name of the package
	 *            containing the board
	 * @param platformName
	 *            ignored if jsonFileName equals "local" otherwise the name of
	 *            the platform containing the board
	 * @param boardID
	 *            the id of the board in the boards.txt file
	 * @param options
	 *            the options to specify the board (the menu named on the
	 *            boards.txt file)
	 * @return The class BoardDescriptor or null
	 */
	static public BoardDescriptor getBoardDescriptor(String jsonFileName, String packageName, String platformName,
			String boardID, Map<String, String> options) {
		if (jsonFileName.equals("local")) { //$NON-NLS-1$
			return BoardDescriptor.makeBoardDescriptor(new File(packageName), boardID, options);
		}
		return getNewestBoardIDFromBoardsManager(jsonFileName, packageName, platformName, boardID, options);
	}

	static private BoardDescriptor getNewestBoardIDFromBoardsManager(String jsonFileName, String packageName,
			String platformName, String boardID, Map<String, String> options) {

		Package thePackage = Manager.getPackage(jsonFileName, packageName);
		if (thePackage == null) {
			// fail("failed to find package:" + this.mPackageName);
			return null;
		}
		ArduinoPlatform platform = thePackage.getLatestPlatform(platformName);
		if (platform == null) {
			// fail("failed to find platform " + this.mPlatform + " in
			// package:" + this.mPackageName);
			return null;
		}
		List<Board> boards = platform.getBoards();
		if (boards == null) {
			// fail("No boards found");
			return null;
		}
		for (Board curBoard : boards) {
			if (curBoard.getId().equals(boardID)) {
				java.io.File boardsFile = curBoard.getPlatform().getBoardsFile();
				BoardDescriptor boardid = BoardDescriptor.makeBoardDescriptor(boardsFile, curBoard.getId(), options);

				return boardid;
			}

		}
		return null;
	}

	public static void addPackageURLs(HashSet<String> packageUrlsToAdd, boolean forceDownload) {
		Manager.addJsonURLs(packageUrlsToAdd, forceDownload);
	}

	public static void removePackageURLs(Set<String> packageUrlsToRemove) {
		Manager.removePackageURLs(packageUrlsToRemove);

	}

	public static void installAllLatestPlatforms() {
		NullProgressMonitor monitor = new NullProgressMonitor();
		List<Package> allPackages = Manager.getPackages();
		for (Package curPackage : allPackages) {
			Collection<ArduinoPlatform> latestPlatforms = curPackage.getLatestPlatforms();
			for (ArduinoPlatform curPlatform : latestPlatforms) {
				curPlatform.install(monitor);
			}
		}
	}

	public static void referenceLocallInstallation(String newHardwarePath) {

		String currentPaths[] = InstancePreferences.getPrivateHardwarePaths();
		String newPaths[] = new String[currentPaths.length + 1];
		for (int i = 0; i < currentPaths.length; i++) {
			if (currentPaths[i].equals(newHardwarePath)) {
				return;
			}
			newPaths[i] = currentPaths[i];
		}
		newPaths[currentPaths.length] = newHardwarePath;
		InstancePreferences.setPrivateHardwarePaths(newPaths);
	}

	public static boolean isReady() {
		return Manager.isReady();
	}

	/**
	 * find all examples for this type of board. That is the examples provided
	 * by Arduino The examples provided by the common libraries The examples
	 * provided by the private libraries The examples provided by the platform
	 * the board belongs to
	 *
	 * If the boardID is null there will be no platform examples
	 *
	 * @param boardDescriptor
	 * @return
	 */
	public static TreeMap<String, IPath> getAllExamples(BoardDescriptor boardDescriptor) {
		TreeMap<String, IPath> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		// Get the examples of the library manager installed libraries

		examples.putAll(getAllLibraryExamples());
		examples.putAll(getAllArduinoIDEExamples());
		// This one should be the last as hasmap overwrites doubles. This way
		// hardware libraries are preferred to others
		examples.putAll(getAllHardwareLibraryExamples(boardDescriptor));

		return examples;
	}

	/*
	 * Get the examples of the libraries from the selected hardware These may be
	 * referenced libraries
	 */
	private static TreeMap<String, IPath> getAllHardwareLibraryExamples(BoardDescriptor boardDescriptor) {
		TreeMap<String, IPath> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		if (boardDescriptor != null) {
			IPath platformPath = boardDescriptor.getreferencingPlatformPath();
			if (platformPath.toFile().exists()) {
				examples.putAll(getLibExampleFolders(platformPath.append(LIBRARY_PATH_SUFFIX)));
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
		Path exampleLocation = new Path(ConfigurationPreferences.getInstallationPathExamples().toString());

		if (exampleLocation.toFile().exists()) {
			examples.putAll(getExamplesFromFolder(new String(), exampleLocation));
		}
		return examples;
	}

	/**
	 * find all examples that are delivered with a library This does not include
	 * the libraries delivered with hardware
	 *
	 * @return
	 */
	public static TreeMap<String, IPath> getAllLibraryExamples() {
		TreeMap<String, IPath> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		String libLocations[] = InstancePreferences.getPrivateLibraryPaths();

		IPath CommonLibLocation = ConfigurationPreferences.getInstallationPathLibraries();
		if (CommonLibLocation.toFile().exists()) {
			examples.putAll(getLibExampleFolders(CommonLibLocation));
		}

		// get the examples from the user provide library locations
		if (libLocations != null) {
			for (String curLibLocation : libLocations) {
				if (new File(curLibLocation).exists()) {
					examples.putAll(getLibExampleFolders(new Path(curLibLocation)));
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
	private static TreeMap<String, IPath> getLibExampleFolders(IPath LibRoot) {
		TreeMap<String, IPath> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		String[] Libs = LibRoot.toFile().list();
		if (Libs == null) {
			// Either dir does not exist or is not a directory
		} else {
			for (String curLib : Libs) {
				IPath Lib_examples = LibRoot.append(curLib).append("examples");//$NON-NLS-1$
				IPath Lib_Examples = LibRoot.append(curLib).append("Examples");//$NON-NLS-1$
				if (Lib_examples.toFile().isDirectory()) {
					examples.putAll(getExamplesFromFolder(curLib, Lib_examples));
				} else if (Lib_Examples.toFile().isDirectory()) {
					examples.putAll(getExamplesFromFolder(curLib, Lib_Examples));
				} else // nothing found directly so maybe this is a version
						// based lib
				{
					String[] versions = LibRoot.append(curLib).toFile().list();
					if (versions != null) {
						if (versions.length == 1) {// There can only be 1
							// version of a lib
							Lib_examples = LibRoot.append(curLib).append(versions[0]).append("examples");//$NON-NLS-1$
							Lib_Examples = LibRoot.append(curLib).append(versions[0]).append("Examples");//$NON-NLS-1$
							if (Lib_examples.toFile().isDirectory()) {
								examples.putAll(getExamplesFromFolder(curLib, Lib_examples));
							} else if (Lib_Examples.toFile().isDirectory()) {
								examples.putAll(getExamplesFromFolder(curLib, Lib_Examples));
							}
						}
					}
				}
			}
		}
		return examples;
	}

	/**
	 * This method adds a folder recursively examples. Leaves containing ino
	 * files are assumed to be examples
	 *
	 * @param File
	 */
	private static TreeMap<String, IPath> getExamplesFromFolder(String prefix, IPath location) {
		TreeMap<String, IPath> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		File[] children = location.toFile().listFiles();
		if (children == null) {
			// Either dir does not exist or is not a directory
		} else {
			for (File exampleFolder : children) {
				Path pt = new Path(exampleFolder.toString());
				String extension = pt.getFileExtension();
				if (exampleFolder.isDirectory()) {
					examples.putAll(getExamplesFromFolder(prefix + ' ' + location.lastSegment() + '?',
							new Path(exampleFolder.toString())));
				} else if (INO.equalsIgnoreCase(extension) || PDE.equalsIgnoreCase(extension)
						|| CPP.equalsIgnoreCase(extension) || C.equalsIgnoreCase(extension)) {
					examples.put(prefix + location.lastSegment(), location);
				}
			}
		}
		return examples;
	}

	public static void setAutoImportLibraries(boolean booleanValue) {
		InstancePreferences.setAutomaticallyImportLibraries(booleanValue);

	}

	public static boolean getAutoImportLibraries(boolean booleanValue) {
		return InstancePreferences.getAutomaticallyImportLibraries();

	}

	public static String[] getBoardNames(String boardFile) {
		TxtFile theBoardsFile = new TxtFile(new File(boardFile));
		return theBoardsFile.getAllNames();
	}

	/**
	 * Searches for all boards.txt files from the hardware folders and the
	 * boards manager
	 *
	 * @return all the boards.txt files with full path and in a case insensitive
	 *         order
	 */
	public static String[] getAllBoardsFiles() {
		String hardwareFolders[] = getHardwarePaths();

		TreeSet<String> boardFiles = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		for (String CurFolder : hardwareFolders) {
			searchFiles(new File(CurFolder), boardFiles, Const.BOARDS_FILE_NAME, 6);
		}
		if (boardFiles.size() == 0) {
			Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
					Messages.Helpers_No_boards_txt_found + String.join("\n", hardwareFolders), null)); //$NON-NLS-1$
			return null;
		}
		return boardFiles.toArray(new String[boardFiles.size()]);
	}

	private static void searchFiles(File folder, TreeSet<String> Hardwarelists, String Filename, int depth) {
		if (depth > 0) {
			File[] a = folder.listFiles();
			if (a == null) {
				Common.log(new Status(IStatus.INFO, Const.CORE_PLUGIN_ID,
						Messages.Helpers_The_folder + folder + Messages.Helpers_is_empty, null));
				return;
			}
			for (File f : a) {
				if (f.isDirectory()) {
					searchFiles(f, Hardwarelists, Filename, depth - 1);
				} else if (f.getName().equals(Filename)) {
					Hardwarelists.add(new Path(f.toString()).toString());
				}
			}
		}
	}

	/**
	 * Gets all the folders that can contain hardware
	 *
	 * @return a list of all the folder locations that can contain hardware
	 */
	private static String[] getHardwarePaths() {
		return (InstancePreferences.getPrivateHardwarePathsString() + File.pathSeparator
				+ ConfigurationPreferences.getInstallationPathPackages()).split(File.pathSeparator);
	}

	public static void setPrivateHardwarePaths(String[] hardWarePaths) {
		InstancePreferences.setPrivateHardwarePaths(hardWarePaths);

	}

	public static void setPrivateLibraryPaths(String[] libraryPaths) {
		InstancePreferences.setPrivateLibraryPaths(libraryPaths);

	}

	public static class PlatformTree {
		private TreeMap<String, IndexFile> IndexFiles = new TreeMap<>();

		public class InstallableVersion implements Comparable<InstallableVersion> {
			private Platform platform;
			private ArduinoPlatform myInternalPlatformm;
			private boolean isInstalled;

			public InstallableVersion(ArduinoPlatform internalPlatformm, Platform platform) {
				this.platform = platform;
				this.myInternalPlatformm = internalPlatformm;
				this.isInstalled = this.myInternalPlatformm.isInstalled();
			}

			public VersionNumber getVersion() {
				return new VersionNumber(this.myInternalPlatformm.getVersion());
			}

			public boolean isInstalled() {
				return this.isInstalled;
			}

			public void setInstalled(boolean isInstalled) {
				this.isInstalled = isInstalled;
			}

			@Override
			public int compareTo(InstallableVersion o) {
				return getVersion().compareTo(o.getVersion());
			}

			public Platform getPlatform() {
				return this.platform;
			}

			public ArduinoPlatform getInternalPlatform() {
				return this.myInternalPlatformm;
			}

		}

		public class Platform implements Comparable<Platform> {

			private String name;
			private String architecture;
			protected TreeSet<InstallableVersion> versions = new TreeSet<>();
			protected String boards;
			private Package pac;

			public String getArchitecture() {
				return this.architecture;
			}

			public String getBoards() {
				return this.boards;
			}

			public Platform(ArduinoPlatform internalPlatformm, Package pac) {
				this.name = internalPlatformm.getName();
				this.architecture = internalPlatformm.getArchitecture();
				this.versions.add(new InstallableVersion(internalPlatformm, this));
				this.boards = String.join("\n", internalPlatformm.getBoardNames()); //$NON-NLS-1$
				this.pac = pac;
			}

			public void addVersion(ArduinoPlatform internalPlatformm) {
				this.versions.add(new InstallableVersion(internalPlatformm, this));

			}

			public Collection<InstallableVersion> getVersions() {
				return this.versions;
			}

			public String getName() {
				return this.name;
			}

			public String getLatest() {
				return this.versions.last().toString();
			}

			@Override
			public int compareTo(Platform other) {
				return this.name.compareToIgnoreCase(other.name);
			}

			public InstallableVersion getVersion(VersionNumber versionNumber) {
				for (InstallableVersion curVersion : this.versions) {
					if (curVersion.getVersion().compareTo(versionNumber) == 0) {
						return curVersion;
					}
				}
				return null;
			}

			public boolean isInstalled() {
				for (InstallableVersion curVersion : this.versions) {
					if (curVersion.isInstalled()) {
						return true;
					}
				}
				return false;
			}

			public Package getPackage() {
				return this.pac;
			}
		}

		/**
		 * This class represents the json file on disk
		 */
		public class IndexFile implements Comparable<IndexFile> {
			protected File jsonFile;
			protected TreeMap<String, Package> packages = new TreeMap<>();

			public IndexFile(File jsonFile) {
				this.jsonFile = jsonFile;
			}

			@Override
			public int compareTo(IndexFile other) {
				return this.jsonFile.compareTo(other.jsonFile);
			}

			public Collection<Package> getAllPackages() {
				return this.packages.values();
			}

			public String getNiceName() {
				return this.jsonFile.getName();
			}

			public String getFullName() {
				return this.jsonFile.getPath();
			}

			/**
			 * is one ore more packages of this index file installed
			 *
			 * @return returns true if at least one version of one child
			 *         platform of a child packageis installed
			 */

			public boolean isInstalled() {
				for (Package curpackage : this.packages.values()) {
					if (curpackage.isInstalled()) {
						return true;

					}
				}
				return false;
			}
		}

		public class Package implements Comparable<Package> {
			public String getMaintainer() {
				return this.maintainer;
			}

			public URL getWebsiteURL() {
				return this.websiteURL;
			}

			public String getEmail() {
				return this.email;
			}

			private String name;
			private String maintainer;
			private URL websiteURL;
			private String email;

			protected TreeMap<String, Platform> platforms = new TreeMap<>();
			private IndexFile indexFile;

			public Package(String name) {
				this.name = name;
			}

			public Package(io.sloeber.core.managers.Package pack, IndexFile indexFile) {
				this.name = pack.getName();
				this.maintainer = pack.getMaintainer();
				try {
					this.websiteURL = new URL(pack.getWebsiteURL());
				} catch (MalformedURLException e) {
					// ignore this error
				}
				this.email = pack.getEmail();
				this.indexFile = indexFile;
			}

			public String getName() {
				return this.name;
			}

			public Collection<Platform> getPlatforms() {
				return this.platforms.values();
			}

			@Override
			public int compareTo(Package other) {
				return this.name.compareToIgnoreCase(other.name);
			}

			/**
			 * is one ore more platforms of this package installed
			 *
			 * @return returns true if at least one version of one child
			 *         platform is installed
			 */
			public boolean isInstalled() {
				for (Platform curplatform : this.platforms.values()) {
					if (curplatform.isInstalled()) {
						return true;

					}
				}
				return false;
			}

			public IndexFile getIndexFile() {
				return this.indexFile;
			}

		}

		public PlatformTree() {
			List<PackageIndex> packageIndexes = Manager.getPackageIndices();
			for (PackageIndex curPackageIndex : packageIndexes) {
				IndexFile curIndexFile = new IndexFile(curPackageIndex.getJsonFile());
				this.IndexFiles.put(curPackageIndex.getJsonFileName(), curIndexFile);
				for (io.sloeber.core.managers.Package curInternalPackage : curPackageIndex.getPackages()) {
					Package curPackage = new Package(curInternalPackage, curIndexFile);
					curIndexFile.packages.put(curPackage.getName(), curPackage);
					for (ArduinoPlatform curInternalPlatform : curInternalPackage.getPlatforms()) {
						Platform curPlatform = new Platform(curInternalPlatform, curPackage);
						Platform foundPlatform = curPackage.platforms.get(curPlatform.getName());
						if (foundPlatform == null) {
							curPackage.platforms.put(curPlatform.getName(), curPlatform);
						} else {
							foundPlatform.addVersion(curInternalPlatform);
						}
					}
				}
			}
		}

		public Collection<IndexFile> getAllIndexFiles() {
			return this.IndexFiles.values();
		}

		public Set<Package> getAllPackages() {
			Set<Package> all = new TreeSet<>();
			for (IndexFile indexFile : this.IndexFiles.values()) {
				all.addAll(indexFile.getAllPackages());
			}
			return all;
		}

		public Collection<Platform> getAllPlatforms() {
			Set<Platform> all = new TreeSet<>();
			for (IndexFile curIndexFile : this.IndexFiles.values()) {
				for (Package curPackage : curIndexFile.getAllPackages()) {
					all.addAll(curPackage.getPlatforms());
				}
			}
			return all;
		}

		public IndexFile getIndexFile(PackageIndex packageIndex) {
			return this.IndexFiles.get(packageIndex.getJsonFileName());
		}

		@SuppressWarnings("static-method")
		public Package getPackage(IndexFile indexFile, io.sloeber.core.managers.Package curInternalPackage) {
			return indexFile.packages.get(curInternalPackage.getName());
		}

		@SuppressWarnings("static-method")
		public Platform getPlatform(Package curPackage, ArduinoPlatform curInternalPlatform) {
			return curPackage.platforms.get(curInternalPlatform.getName());
		}

	}

	public static IStatus setPlatformTree(PlatformTree platformTree, IProgressMonitor monitor, MultiStatus status) {
		if (!Manager.isReady()) {
			status.add(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "BoardsManager is still Bussy", null)); //$NON-NLS-1$
			return status;
		}
		try {
			Manager.setReady(false);

			for (IndexFile curIndexFile : platformTree.getAllIndexFiles()) {
				for (io.sloeber.core.api.BoardsManager.PlatformTree.Package curPackage : curIndexFile
						.getAllPackages()) {
					for (Platform curPlatform : curPackage.getPlatforms()) {
						for (InstallableVersion curVersion : curPlatform.getVersions()) {
							if (curVersion.isInstalled()) {
								status.add(curVersion.getInternalPlatform().install(monitor));
							} else {

								status.add(curVersion.getInternalPlatform().remove(monitor));
							}
						}
					}
				}

			}
		} catch (Exception e) {
			// do nothing
		}
		Manager.setReady(true);
		return status;
	}

	public static boolean getAutoImportLibraries() {
		return InstancePreferences.getAutomaticallyImportLibraries();
	}

	public static String getPrivateHardwarePathsString() {
		return InstancePreferences.getPrivateHardwarePathsString();
	}

	/**
	 * returns all the menu names for all installed platforms. The return is
	 * sorted and unique
	 *
	 * @return
	 */
	public static Set<String> getAllMenuNames() {
		Set<String> ret = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		String[] boardFiles = getAllBoardsFiles();
		for (String curBoardFile : boardFiles) {
			TxtFile txtFile = new TxtFile(new File(curBoardFile));
			ret.addAll(txtFile.getMenuNames());
		}
		return ret;
	}

	public static TreeMap<String, String> getAllmenus() {
		TreeMap<String, String> ret = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		String[] boardFiles = getAllBoardsFiles();
		for (String curBoardFile : boardFiles) {
			TxtFile txtFile = new TxtFile(new File(curBoardFile));
			ret.putAll(txtFile.getMenus());
		}
		return ret;
	}

	/**
	 * Remove all packages that have a more recent version
	 */
	public static void onlyKeepLatestPlatforms() {
		Manager.onlyKeepLatestPlatforms();

	}

	public static void setPragmaOnceHeaders(boolean booleanValue) {
		InstancePreferences.setPragmaOnceHeaders(booleanValue);

	}

	public static boolean getPragmaOnceHeaders() {
		return InstancePreferences.getPragmaOnceHeaders();
	}

}
