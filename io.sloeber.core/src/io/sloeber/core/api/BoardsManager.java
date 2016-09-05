package io.sloeber.core.api;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import io.sloeber.common.ConfigurationPreferences;
import io.sloeber.common.InstancePreferences;
import io.sloeber.core.managers.ArduinoPlatform;
import io.sloeber.core.managers.Board;
import io.sloeber.core.managers.Manager;
import io.sloeber.core.managers.Package;
import io.sloeber.core.tools.TxtFile;

/**
 * This class groups both boards installed by the hardware manager and boards
 * installed locally.
 * 
 * @author jan
 *
 */
public class BoardsManager {

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
	    return new BoardDescriptor(new TxtFile(new File(packageName)), boardID, options);
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
		TxtFile boardsTxtFile = new TxtFile(boardsFile);
		System.out.println("Testing board: " + curBoard.getName()); //$NON-NLS-1$
		BoardDescriptor boardid = new BoardDescriptor(boardsTxtFile, curBoard.getId(), options);

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

}
