package it.baeyens.arduino.ide.connector;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ExternalCommandLauncher;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class ArduinoGetPreferences {
    /**
     * checks whether the arduino IDE contains the dump file functionality (1.6.5 and later) Note that I assume "nightly" versions to be from after
     * 1.6.5
     * 
     * @return true if dump file functionality is available false if the functionality is not available or unknown (god knows what will happen with
     *         the version numbers)
     */
    private static boolean arduinoIdeSupportsDumpFiles() {
	String IDEVersion = ArduinoInstancePreferences.getArduinoIDEVersion();
	if (IDEVersion.trim().split("\\.").length != 3)
	    return false;
	if (IDEVersion.compareTo("1.6.4") > 0) {
	    return true;
	}
	return false;
    }

    /**
     * Checks whether the boards file has updated. Based on discussions with the core team this is done based on the timestamp of the preferences.txt
     * file (https://github.com/arduino/Arduino/issues/2982)
     * 
     * @return true if the boards have been updated otherwise false
     */
    private static boolean newerBoardsAvailable() {

	File preferenceFile = Common.getPreferenceFile();
	long storedPreferenceModificatonStamp = ArduinoInstancePreferences.getStoredPreferenceModificatonStamp();
	return (preferenceFile.lastModified() != storedPreferenceModificatonStamp);
    }

    private static void storePreferenceModificationStamp() {

	File preferenceFile = Common.getPreferenceFile();
	ArduinoInstancePreferences.setStoredPreferenceModificatonStamp(preferenceFile.lastModified());
    }

    private static void emptyPreferenceModificationStamp() {
	ArduinoInstancePreferences.setStoredPreferenceModificatonStamp(-1);
    }

    /**
     * loops through all the projects and finds all board ID's then for each boardID it will create a arduino ide preference dump file
     */
    private static void generateDumpFiles(IProgressMonitor monitor) {
	IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
	Set<String> boardsSet = new HashSet<String>();
	for (IProject project : projects) {
	    if (project.isOpen()) {
		try {
		    if (project.hasNature(ArduinoConst.ArduinoNatureID)) {
			String boardName = Common.getBuildEnvironmentVariable(project, ArduinoConst.ENV_KEY_JANTJE_BOARD_ID, "");
			String PackageName = Common.getBuildEnvironmentVariable(project, ArduinoConst.ENV_KEY_JANTJE_PACKAGE_ID, "");
			String ArchitectureName = Common.getBuildEnvironmentVariable(project, ArduinoConst.ENV_KEY_JANTJE_ARCITECTURE_ID, "");
			if (boardName.isEmpty() || PackageName.isEmpty() || ArchitectureName.isEmpty()) {
			    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Skipping project \"" + project.getName()
				    + "\" due to missing configuration."));
			} else {
			    boardsSet.add(PackageName + ":" + ArchitectureName + ":" + boardName);
			}
		    }
		} catch (CoreException e) {
		    e.printStackTrace();
		}
	    }
	}
	for (String board : boardsSet) {
	    String names[] = board.split(":");
	    if (names.length == 3) {
		generateDumpFileForBoard(names[0], names[1], names[2], monitor);
	    } else {
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
			"This should never happen. concatinated 3 strings and splitting gives different number", null));
	    }
	}

    }

    /**
     * 
     */
    private static boolean generateDumpFileForBoard(String packageName, String architecture, String boardID, IProgressMonitor monitor) {
	File arduinoIDEenvVars = Common.getArduinoIdeDumpName(packageName, architecture, boardID);

	String command = ArduinoInstancePreferences.getArduinoIdeProgram() + " --board " + packageName + ":" + architecture + ":" + boardID
		+ " --get-pref";
	ExternalCommandLauncher commandLauncher = new ExternalCommandLauncher(command);
	try {

	    if (commandLauncher.launch(monitor) != 0) {
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
			"Failed to extract environment info from arduino ide 'arduino --get-pref'. The setup will not work properly.\n" + command,
			null));
	    }
	} catch (IOException e) {
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
		    "Failed to extract environment info from arduino ide 'arduino --get-pref'. The setup will not work properly\n" + command, e));
	}
	// List<String> err = commandLauncher.getStdErr();
	List<String> out = commandLauncher.getStdOut();
	try (BufferedWriter output = new BufferedWriter(new FileWriter(arduinoIDEenvVars));) {
	    output.write("#This file has been generated automatically.");
	    output.newLine();
	    output.write("#Please do not change.");
	    output.newLine();
	    for (String item : out) {
		String upperItem = item.trim().toUpperCase();
		if (upperItem.endsWith("=TRUE") || upperItem.endsWith("=FALSE")) {
		    output.write("#Ignoring this line : ");
		}
		output.write(item);
		output.newLine();
	    }
	    output.close();
	} catch (IOException e) {
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
		    "Failed to extract environment info from arduino ide 'arduino --get-pref'. The setup will not work properly", e));

	}
	return true;
    }

    /**
     * this method checks whether an update of the dump files is needed. if the arduino ide version is from before 1.6.4 all existing dump files are
     * deleted if no update is needed no update is done if an update is needed an update is done as follows
     * 
     * returns true if an update has been done false if no update was needed
     */
    public static boolean updateArduinoEnvironmentVariablesForAllProjectsIfNeeded() {
	if (arduinoIdeSupportsDumpFiles()) {
	    if (newerBoardsAvailable()) {
		deleteAllDumpFiles();
		generateDumpFiles(null);
		storePreferenceModificationStamp();
		return true;
	    }
	    return false;
	}
	emptyPreferenceModificationStamp();
	return deleteAllDumpFiles();

    }

    public static boolean generateDumpFileForBoardIfNeeded(String packageName, String architecture, String boardID, IProgressMonitor monitor) {
	if (arduinoIdeSupportsDumpFiles()) {
	    if (Common.getArduinoIdeDumpName(packageName, architecture, boardID).exists()) {
		if (newerBoardsAvailable()) {
		    deleteAllDumpFiles();
		    generateDumpFiles(monitor);
		    return true;
		}
	    } else {
		return generateDumpFileForBoard(packageName, architecture, boardID, monitor);
	    }
	}
	return false;
    }

    /**
     * deletes all the dump files available in the current workspace. dump files are recognized as follows: no directories and starts with suffix and
     * ends with trailer
     * 
     * @return true if actual files have been deleted false if no files have been deleted (in other words there were no files to delete)
     */
    private static boolean deleteAllDumpFiles() {
	boolean ret = false;
	File[] workspaceRootFiles = Common.getWorkspaceRoot().listFiles();
	if (workspaceRootFiles == null) {
	    return false;
	}
	for (File workspaceRootFile : workspaceRootFiles) {
	    if (!workspaceRootFile.isDirectory()) {
		if (workspaceRootFile.getName().startsWith(ArduinoConst.ARDUINO_IDE_DUMP__FILE_NAME_PREFIX)) {
		    if (workspaceRootFile.getName().endsWith(ArduinoConst.ARDUINO_IDE_DUMP__FILE_NAME_TRAILER)) {
			workspaceRootFile.delete();
			ret = true;
		    }
		}
	    }
	}
	return ret;
    }
}