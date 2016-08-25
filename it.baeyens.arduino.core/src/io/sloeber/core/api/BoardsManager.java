package io.sloeber.core.api;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import it.baeyens.arduino.common.InstancePreferences;
import it.baeyens.arduino.managers.ArduinoPlatform;
import it.baeyens.arduino.managers.Board;
import it.baeyens.arduino.managers.Manager;
import it.baeyens.arduino.managers.Package;
import it.baeyens.arduino.tools.TxtFile;

/**
 * This class groups both boards installed by the hardware manager and boards
 * installed locally.
 * 
 * @author jan
 *
 */
public class BoardsManager {
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

    public static void addPackageURLs(String[] packageUrlsToAdd) {
	Manager.addPackageURLs(packageUrlsToAdd);

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

}
