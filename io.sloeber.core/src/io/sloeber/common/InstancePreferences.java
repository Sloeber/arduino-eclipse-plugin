package io.sloeber.common;

import java.io.File;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

/**
 * ArduinoPreferences is a class containing only static methods that help
 * managing the preferences.
 * 
 * @author Jan Baeyens
 * 
 */
public class InstancePreferences extends Const {

    /**
     * Give back the user option if the libraries need to be added or not
     * 
     * @return true if libraries need to be added else false.
     */
    public static boolean getAutomaticallyImportLibraries() {
	return getGlobalBoolean(KEY_AUTO_IMPORT_LIBRARIES, true);
    }

    public static void setAutomaticallyImportLibraries(boolean value) {
	setGlobalValue(KEY_AUTO_IMPORT_LIBRARIES, value);
    }

    public static String getGlobalString(String key, String defaultValue) {
	IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
	return myScope.get(key, defaultValue);
    }

    protected static boolean getGlobalBoolean(String key, boolean def) {
	IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
	return myScope.getBoolean(key, def);
    }

    protected static int getGlobalInt(String key) {
	IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
	return myScope.getInt(key, 0);
    }

    protected static long getGlobalLong(String key) {
	IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
	return myScope.getLong(key, 0);
    }

    public static void setGlobalValue(String key, String value) {

	IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
	myScope.put(key, value);
	try {
	    myScope.flush();
	} catch (BackingStoreException e) {
	    Common.log(
		    new Status(IStatus.WARNING, CORE_PLUGIN_ID, "failed to set global variable of type string " + key)); //$NON-NLS-1$
	    e.printStackTrace();
	}
    }

    protected static void setGlobalValue(String key, int value) {
	IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
	myScope.putInt(key, value);
	try {
	    myScope.flush();
	} catch (BackingStoreException e) {
	    Common.log(new Status(IStatus.WARNING, CORE_PLUGIN_ID, "failed to set global variable of type int " + key)); //$NON-NLS-1$
	    e.printStackTrace();
	}
    }

    protected static void setGlobalValue(String key, boolean value) {
	IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
	myScope.putBoolean(key, value);
	try {
	    myScope.flush();
	} catch (BackingStoreException e) {
	    Common.log(new Status(IStatus.WARNING, CORE_PLUGIN_ID,
		    "failed to set global variable of type boolean " + key)); //$NON-NLS-1$
	    e.printStackTrace();
	}
    }

    protected static void setGlobalValue(String key, long value) {
	IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
	myScope.putLong(key, value);
	try {
	    myScope.flush();
	} catch (BackingStoreException e) {
	    Common.log(
		    new Status(IStatus.WARNING, CORE_PLUGIN_ID, "failed to set global variable of type long " + key)); //$NON-NLS-1$
	    e.printStackTrace();
	}
    }

    public static void setLastUsedBoardsFile(String boardsFile) {
	setGlobalValue(KEY_LAST_USED_BOARDS_FILE, boardsFile);

    }

    public static String[] getPrivateLibraryPaths() {
	return getGlobalString(KEY_PRIVATE_LIBRARY_PATHS, Defaults.getPrivateLibraryPath()).split(File.pathSeparator);
    }

    public static void setPrivateLibraryPaths(String[] folderName) {
	setGlobalValue(KEY_PRIVATE_LIBRARY_PATHS, String.join(File.pathSeparator, folderName));
    }

    public static String[] getPrivateHardwarePaths() {
	return getGlobalString(KEY_PRIVATE_HARDWARE_PATHS, Defaults.getPrivateHardwarePath()).split(File.pathSeparator);
    }

    public static String getPrivateHardwarePathsString() {
	return getGlobalString(KEY_PRIVATE_HARDWARE_PATHS, Defaults.getPrivateHardwarePath());
    }

    public static void setPrivateHardwarePaths(String[] folderName) {
	setGlobalValue(KEY_PRIVATE_HARDWARE_PATHS, String.join(File.pathSeparator, folderName));
    }

}
