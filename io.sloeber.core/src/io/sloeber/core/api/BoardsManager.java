package io.sloeber.core.api;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.common.Common;
import io.sloeber.common.ConfigurationPreferences;
import io.sloeber.common.Const;
import io.sloeber.common.InstancePreferences;
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

    public static String getUpdateJasonFilesKey() {
	return ConfigurationPreferences.getUpdateJasonFilesKey();
    }

    /**
     * Gets the board id based on the information provided. If
     * jsonFileName="local" the board is assumend not to be installed by the
     * boards manager. Otherwise the boardsmanager is queried to find the board
     * ID. In this case the latest installed board will be returned
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
    static public BoardDescriptor getBoardID(String jsonFileName, String packageName, String platformName,
	    String boardID, Map<String, String> options) {
	if (jsonFileName.equals("local")) { //$NON-NLS-1$
	    return new BoardDescriptor(new File(packageName), boardID, options);
	}
	return getNewestBoardIDFromBoardsManager(jsonFileName, packageName, platformName, boardID, options);
    }

    static private BoardDescriptor getNewestBoardIDFromBoardsManager(String jsonFileName, String packageName,
	    String platformName, String boardID, Map<String, String> options) {

	List<Board> boards = null;
	try {
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
	    boards = platform.getBoards();
	} catch (CoreException e1) {
	    e1.printStackTrace();
	}
	if (boards == null) {
	    // fail("No boards found");
	    return null;
	}
	for (Board curBoard : boards) {
	    if (curBoard.getId().equals(boardID)) {
		java.io.File boardsFile = curBoard.getPlatform().getBoardsFile();
		System.out.println("Testing board: " + curBoard.getName()); //$NON-NLS-1$
		BoardDescriptor boardid = new BoardDescriptor(boardsFile, curBoard.getId(), options);

		return boardid;
	    }

	}
	return null;
    }

    public static void addPackageURLs(HashSet<String> packageUrlsToAdd, boolean forceDownload) {
	Manager.addPackageURLs(packageUrlsToAdd, forceDownload);
    }

    public static void removePackageURLs(Set<String> packageUrlsToRemove) {
	Manager.removeBoardsPackageURLs(packageUrlsToRemove);

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
	// TODO Auto-generated method stub

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

    public static String[] getBoardsPackageURLList() {
	return Manager.getBoardsPackageURLList();
    }

    public static void setBoardsPackageURL(String[] newBoardJsonUrls) {
	Manager.setBoardsPackageURL(newBoardJsonUrls);
    }

    public static String getBoardsPackageURLs() {
	return Manager.getBoardsPackageURLs();
    }

    public static boolean isReady() {
	return Manager.isReady();
    }

    public static TreeMap<String, String> getAllExamples(BoardDescriptor boardID) {
	TreeMap<String, String> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	// Get the examples of the library manager installed libraries
	String libLocations[] = InstancePreferences.getPrivateLibraryPaths();
	File exampleLocation = new File(ConfigurationPreferences.getInstallationPathExamples().toString());

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

	// Get the examples from the example locations

	if (exampleLocation.exists()) {
	    examples.putAll(getExamplesFromFolder("", exampleLocation)); //$NON-NLS-1$
	}

	// Get the examples of the libraries from the selected hardware
	// This one should be the last as hasmap overwrites doubles. This way
	// hardware libraries are preferred to others
	if (boardID != null) {
	    IPath platformPath = boardID.getPlatformPath();
	    if (platformPath.toFile().exists()) {
		examples.putAll(getLibExampleFolders(platformPath.append(Const.LIBRARY_PATH_SUFFIX)));
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
    private static TreeMap<String, String> getLibExampleFolders(IPath LibRoot) {
	TreeMap<String, String> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	String[] Libs = LibRoot.toFile().list();
	if (Libs == null) {
	    // Either dir does not exist or is not a directory
	} else {
	    for (String curLib : Libs) {
		IPath Lib_examples = LibRoot.append(curLib).append("examples");//$NON-NLS-1$
		IPath Lib_Examples = LibRoot.append(curLib).append("Examples");//$NON-NLS-1$
		if (Lib_examples.toFile().isDirectory()) {
		    examples.putAll(getExampleFolders(curLib, Lib_examples.toFile()));
		} else if (Lib_Examples.toFile().isDirectory()) {
		    examples.putAll(getExampleFolders(curLib, Lib_Examples.toFile()));
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
				examples.putAll(getExampleFolders(curLib, Lib_examples.toFile()));
			    } else if (Lib_Examples.toFile().isDirectory()) {
				examples.putAll(getExampleFolders(curLib, Lib_Examples.toFile()));
			    }
			}
		    }
		}
	    }
	}
	return examples;
    }

    /**
     * This method adds a folder of examples. There is no search. The provided
     * folder is assumed to be a tree where the parents of the leaves are
     * assumed examples
     * 
     * @param iPath
     * @param pathVarName
     */
    private static TreeMap<String, String> getExampleFolders(String libname, File location) {
	String[] children = location.list();
	TreeMap<String, String> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	if (children == null) {
	    // Either dir does not exist or is not a directory
	} else {
	    for (String curFolder : children) {
		IPath LibFolder = new Path(location.toString()).append(curFolder);
		if (LibFolder.toFile().isDirectory()) {
		    examples.put(libname + '-' + curFolder, LibFolder.toString());
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
    private static TreeMap<String, String> getExamplesFromFolder(String prefix, File location) {
	TreeMap<String, String> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	File[] children = location.listFiles();
	if (children == null) {
	    // Either dir does not exist or is not a directory
	} else {
	    for (File exampleFolder : children) {
		Path pt = new Path(exampleFolder.toString());
		String extension = pt.getFileExtension();
		if (exampleFolder.isDirectory()) {
		    examples.putAll(getExamplesFromFolder(prefix + location.getName() + '-', exampleFolder));
		} else if (INO.equalsIgnoreCase(extension) || PDE.equalsIgnoreCase(extension)) {
		    examples.put(prefix + location.getName(), location.toString());
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
		    try {
			Hardwarelists.add(new Path(f.getCanonicalPath()).toString());
		    } catch (IOException e) {
			// e.printStackTrace();
		    }
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
		+ ConfigurationPreferences.getInstallationPath()).split(File.pathSeparator);
    }

    public static void setPrivateHardwarePaths(String[] hardWarePaths) {
	InstancePreferences.setPrivateHardwarePaths(hardWarePaths);

    }

    public static void setPrivateLibraryPaths(String[] libraryPaths) {
	InstancePreferences.setPrivateLibraryPaths(libraryPaths);

    }

    public static class PlatformTree {

	private TreeMap<String, IndexFile> IndexFiles = new TreeMap<>();

	public class Platform implements Comparable<Platform> {
	    private String name;
	    private String architecture;
	    protected TreeSet<VersionNumber> versions = new TreeSet<>();
	    protected TreeSet<VersionNumber> installedVersions = new TreeSet<>();
	    protected String boards;

	    public Platform(String architecture, String name) {
		this.architecture = architecture;
		this.name = name;
	    }

	    public Platform(ArduinoPlatform internalPlatformm) {
		this.name = internalPlatformm.getName();
		this.architecture = internalPlatformm.getArchitecture();
		this.versions.add(new VersionNumber(internalPlatformm.getVersion()));
		if (internalPlatformm.isInstalled()) {
		    this.installedVersions.add(new VersionNumber(internalPlatformm.getVersion()));
		}
		this.boards = String.join(", ", internalPlatformm.getBoardNames()); //$NON-NLS-1$
	    }

	    public void addVersion(ArduinoPlatform internalPlatformm) {
		this.versions.add(new VersionNumber(internalPlatformm.getVersion()));
		if (internalPlatformm.isInstalled()) {
		    this.installedVersions.add(new VersionNumber(internalPlatformm.getVersion()));
		}
	    }

	    public Collection<VersionNumber> getVersions() {
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
		return this.name.compareTo(other.name);
	    }

	    public boolean isInstalled(VersionNumber version) {
		return this.installedVersions.contains(version);
	    }

	}

	/**
	 * This class represents the json file on disk
	 */
	public class IndexFile implements Comparable<IndexFile> {
	    File jsonFile;
	    protected TreeMap<String, Package> packages = new TreeMap<>();

	    public IndexFile(File jsonFile) {
		this.jsonFile = jsonFile;
	    }

	    @Override
	    public int compareTo(IndexFile other) {
		return this.jsonFile.compareTo(other.jsonFile);
	    }

	    public Collection<Package> getPackages() {
		return this.packages.values();
	    }

	}

	public class Package implements Comparable<Package> {
	    private String name;
	    private String maintainer;
	    private URL websiteURL;
	    private String email;

	    protected TreeMap<String, Platform> platforms = new TreeMap<>();

	    public Package(String name) {
		this.name = name;
	    }

	    public Package(io.sloeber.core.managers.Package pack) {
		this.name = pack.getName();
		this.maintainer = pack.getMaintainer();
		try {
		    this.websiteURL = new URL(pack.getWebsiteURL());
		} catch (MalformedURLException e) {
		    // ignore this error
		}
		this.email = pack.getEmail();
	    }

	    public String getName() {
		return this.name;
	    }

	    public Collection<Platform> getPlatforms() {
		return this.platforms.values();
	    }

	    @Override
	    public int compareTo(Package other) {
		return this.name.compareTo(other.name);
	    }

	}

	public PlatformTree() {
	    List<PackageIndex> packageIndexes = Manager.getPackageIndices();
	    for (PackageIndex curPackageIndex : packageIndexes) {
		IndexFile curIndexFile = new IndexFile(curPackageIndex.getJsonFile());
		this.IndexFiles.put(curPackageIndex.getJsonFileName(), curIndexFile);
		for (io.sloeber.core.managers.Package curInternalPackage : curPackageIndex.getPackages()) {
		    Package curPackage = new Package(curInternalPackage);
		    curIndexFile.packages.put(curPackage.getName(), curPackage);
		    for (ArduinoPlatform curInternalPlatform : curInternalPackage.getPlatforms()) {
			Platform curPlatform = new Platform(curInternalPlatform);
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

	public Collection<IndexFile> getJsonFiles() {
	    return this.IndexFiles.values();
	}

	public Set<Package> getAllPackages() {
	    Set<Package> all = new HashSet<>();
	    for (IndexFile indexFile : this.IndexFiles.values()) {
		all.addAll(indexFile.getPackages());
	    }
	    return all;
	}

	public Collection<Platform> getAllPlatforms() {
	    Set<Platform> all = new HashSet<>();
	    for (IndexFile curIndexFile : this.IndexFiles.values()) {
		for (Package curPackage : curIndexFile.getPackages()) {
		    all.addAll(curPackage.getPlatforms());
		}
	    }
	    return all;
	}

    }

    public static IStatus setPlatformTree(PlatformTree platformTree, IProgressMonitor monitor, MultiStatus status) {
	for (PlatformTree.Platform platform : platformTree.getAllPlatforms()) {
	    for (VersionNumber curVersion : platform.versions) {
		if (platform.installedVersions.contains(curVersion)) {
		    // TODO make sure it is installed
		} else {
		    // TODO make sure it is not installed
		}
	    }

	}
	return status;
    }

}
