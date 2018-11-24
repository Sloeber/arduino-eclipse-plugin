package io.sloeber.core.common;

import java.io.File;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

import io.sloeber.core.Activator;
import io.sloeber.core.api.Defaults;

/**
 * ArduinoPreferences is a class containing only static methods that help
 * managing the preferences.
 *
 * @author Jan Baeyens
 *
 */
public class InstancePreferences {
	// preference keys
	private static final String KEY_PRIVATE_LIBRARY_PATHS = "Private Library Path"; //$NON-NLS-1$
	private static final String KEY_PRIVATE_HARDWARE_PATHS = "Private hardware Path"; //$NON-NLS-1$
	public static final String KEY_AUTO_IMPORT_LIBRARIES = "Automatically import libraries"; //$NON-NLS-1$
	private static final String KEY_PRAGMA_ONCE_HEADER = "add pragma once to headers"; //$NON-NLS-1$
	private static final String KEY_USE_ARDUINO_TOOLS_SELECTION_ALGORITHM="Use the algoritm to find the toolchain like Arduino IDE"; //$NON-NLS-1$
	private static final String KEY_USE_BONJOUR="use bonjour service to find devices"; //$NON-NLS-1$
	// preference nodes
	public static final String NODE_ARDUINO = Activator.NODE_ARDUINO;

	/**
	 * Give back the user option if the libraries need to be added or not
	 *
	 * @return true if libraries need to be added else false.
	 */
	public static boolean getAutomaticallyImportLibraries() {
		return getBoolean(KEY_AUTO_IMPORT_LIBRARIES, true);
	}

	public static void setAutomaticallyImportLibraries(boolean value) {
		setValue(KEY_AUTO_IMPORT_LIBRARIES, value);
	}

	public static String getString(String key, String defaultValue) {
		IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
		return myScope.get(key, defaultValue);
	}

	private static boolean getBoolean(String key, boolean def) {
		IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
		return myScope.getBoolean(key, def);
	}


	public static void setGlobalValue(String key, String value) {

		IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
		myScope.put(key, value);
		try {
			myScope.flush();
		} catch (BackingStoreException e) {
			Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
					"failed to set global variable of type string " + key)); //$NON-NLS-1$
			e.printStackTrace();
		}
	}



	private static void setValue(String key, boolean value) {
		IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
		myScope.putBoolean(key, value);
		try {
			myScope.flush();
		} catch (BackingStoreException e) {
			Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
					"failed to set global variable of type boolean " + key)); //$NON-NLS-1$
			e.printStackTrace();
		}
	}


	public static String[] getPrivateLibraryPaths() {
		return getPrivateLibraryPathsString().split(File.pathSeparator);
	}

	public static String getPrivateLibraryPathsString() {
		return getString(KEY_PRIVATE_LIBRARY_PATHS, Defaults.getPrivateLibraryPath());
	}

	public static void setPrivateLibraryPaths(String[] folderName) {
		setGlobalValue(KEY_PRIVATE_LIBRARY_PATHS, String.join(File.pathSeparator, folderName));
	}

	public static String[] getPrivateHardwarePaths() {
		return getPrivateHardwarePathsString().split(File.pathSeparator);
	}

	public static String getPrivateHardwarePathsString() {
		return getString(KEY_PRIVATE_HARDWARE_PATHS, Defaults.getPrivateHardwarePath());
	}

	public static void setPrivateHardwarePaths(String[] folderName) {
		setGlobalValue(KEY_PRIVATE_HARDWARE_PATHS, String.join(File.pathSeparator, folderName));
	}

	public static void setPragmaOnceHeaders(boolean booleanValue) {
		setValue(KEY_PRAGMA_ONCE_HEADER, booleanValue);
	}

	public static boolean getPragmaOnceHeaders() {
		return getBoolean(KEY_PRAGMA_ONCE_HEADER, true);
	}

	public static void setUseArduinoToolSelection(boolean booleanValue) {
		setValue(KEY_USE_ARDUINO_TOOLS_SELECTION_ALGORITHM, booleanValue);

	}

	public static boolean getUseArduinoToolSelection() {
		return getBoolean(KEY_USE_ARDUINO_TOOLS_SELECTION_ALGORITHM, Defaults.useArduinoToolSelection);
	}
	/**
	 * Setting to see wether user wants bonjour service to be run on
	 * its's system.
	 * Bonjour is a mac protocol that allows you to do network discovery 
	 * for yun, esp8266 and some other boards
	 * If not enabled you will need another way to identify the boards
	 * Note that this service doesn't work properly on window 10
	 * default is enabled
	 * 
	 * @return true if network search for bonjour is requested by the user
	 */
	public static boolean useBonjour() {
		return getBoolean(KEY_USE_BONJOUR, Defaults.useBonjour);
	}

	/**
	 * Set the user preference to search for bonjour devices or not
	 * @param newFlag true is search for bonjour
	 */
	public static void setUseBonjour(boolean newFlag) {
		setValue(KEY_USE_BONJOUR, newFlag);
	}
}
