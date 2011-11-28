package it.baeyens.arduino.tools;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.core.preferences.AVRDudePreferences;
import it.baeyens.avreclipse.core.preferences.AVRPathsPreferences;

import java.io.IOException;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.service.prefs.BackingStoreException;

/**
 * ArduinoPreferences is a class containing only static methods that help
 * managing the preferences.
 * 
 * @author jan Baeyens
 * 
 */
public class ArduinoPreferences {
	private static IPreferenceStore mInstancePreferenceStore = null;
	private static IPreferenceStore mConfigurationPreferenceStore = null;

	/**
	 * StoreGlobalStuff saves the arduino path; the arduino board name and the
	 * upload port in configuration memory
	 * 
	 * @param ArduinoPath
	 *            The Arduino path to save
	 * @param ArduinoBoardName
	 *            The arduino board name to save
	 * @param UploadPort
	 *            The port to use to upload to save
	 * 
	 * @author Jan Baeyens
	 */
	public static void StoreGlobalStuff(IPath ArduinoPath, String ArduinoBoardName, String UploadPort) {
		setArduinoPath(ArduinoPath);
		ArduinoPreferences.setGlobalValue(ArduinoConst.KEY_ARDUINOBOARD, ArduinoBoardName);
		ArduinoPreferences.setGlobalValue(ArduinoConst.KEY_ARDUINOPORT, UploadPort);

	}

	// public static void ReadGlobalStuff(IPath ArduinoPath , String
	// ArduinoBoardName, String UploadPort) {
	// ArduinoPath = getArduinoPath();
	// ArduinoBoardName = getArduinoBoardName();
	// UploadPort= getArduinoBoardName();
	//
	// }

	/**
	 * This method reads the arduino path from the configuration memory
	 * 
	 * @return the arduino path
	 * @author Jan Baeyens
	 */
	public static Path getArduinoPath() {
		return new Path(ArduinoPreferences.getGlobalValue(ArduinoConst.KEY_ARDUINOPATH));
	}

	/**
	 * This method reads the arduino board from the configuration memory
	 * 
	 * @return the Arduino Board name
	 * @author Jan Baeyens
	 */
	public static String getArduinoBoardName() {
		return ArduinoPreferences.getGlobalValue(ArduinoConst.KEY_ARDUINOBOARD);
	}

	/**
	 * This method reads the arduino upload port from the configuration memory
	 * 
	 * @return the upload port
	 * @author Jan Baeyens
	 */
	public static String getUploadPort() {
		return ArduinoPreferences.getGlobalValue(ArduinoConst.KEY_ARDUINOPORT);
	}

	/**
	 * getConfigurationPreferenceStore returns the configuration
	 * prefferenceStore. <br/>
	 * You are probably better of using one of the more high level functions but
	 * it is needed for the preference pages
	 * 
	 * @return the configuration preference store related to the arduino plugin
	 * @author Jan Baeyens
	 */
	public static IPreferenceStore getConfigurationPreferenceStore() {

		if (mConfigurationPreferenceStore == null) {
			mConfigurationPreferenceStore = new ScopedPreferenceStore(ConfigurationScope.INSTANCE, ArduinoConst.NODE_ARDUINO);
		}
		return mConfigurationPreferenceStore;
	}

	/**
	 * getInstancePreferenceStore returns the configuration prefferenceStore. <br/>
	 * You are probably better of using one of the more high level functions but
	 * it is needed for the preference pages
	 * 
	 * @return the Instance preference store related to the arduino plugin
	 * @author Jan Baeyens
	 */
	public static IPreferenceStore getInstancePreferenceStore() {
		if (mInstancePreferenceStore == null) {
			mInstancePreferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, ArduinoConst.NODE_ARDUINO); // AVRPathsPreferences.getPreferenceStore();
																														// //
		}
		return mInstancePreferenceStore;
	}

	private static String getGlobalValue(String key) {
		IEclipsePreferences myScope = ConfigurationScope.INSTANCE.getNode(ArduinoConst.NODE_ARDUINO);
		return myScope.get(key, "");
	}

	private static void setGlobalValue(String key, String Value) {

		IEclipsePreferences myScope = ConfigurationScope.INSTANCE.getNode(ArduinoConst.NODE_ARDUINO);
		myScope.put(key, Value);
		try {
			myScope.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	/**
	 * ConfigureToUseArduinoIDETools set the preferences such that eclipse is
	 * configured to use the Arduino IDE delivered tools. Therefore it needs to
	 * set the avrdud to use a custom config Give avr dude the custom config
	 * file from Arduino IDE Set The paths correctly
	 * 
	 * @author Jan Baeyens
	 * 
	 */
	public static void ConfigureToUseArduinoIDETools() {
		IPath mArduinoPath = getArduinoPath();
		IPreferenceStore avrdudestore = AVRDudePreferences.getPreferenceStore();
		avrdudestore.setValue(AVRDudePreferences.KEY_USECUSTOMCONFIG, true);
		avrdudestore.setValue(AVRDudePreferences.KEY_CONFIGFILE, mArduinoPath.append(ArduinoConst.DUDE_CONFIG_SUFFIX()).toOSString());
		try {
			AVRDudePreferences.savePreferences(avrdudestore);
		} catch (IOException e) {
			IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to save AVRDude settings", e);
			AVRPlugin.getDefault().log(status);
			e.printStackTrace();
		}
		IPreferenceStore pathPrefs = AVRPathsPreferences.getPreferenceStore();
		pathPrefs.setValue(ArduinoConst.KEY_AVRDUDE_PATH, mArduinoPath.append(ArduinoConst.AVRDUDE_PATH_SUFFIX()).toOSString());
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			pathPrefs.setValue(ArduinoConst.KEY_GCC_PATH, mArduinoPath.append(ArduinoConst.GCC_PATH_SUFFIX).toOSString());
			pathPrefs.setValue(ArduinoConst.KEY_GNU_PATH, mArduinoPath.append(ArduinoConst.GNU_PATH_SUFFIX).toOSString());
			pathPrefs.setValue(ArduinoConst.KEY_HEADER_PATH, mArduinoPath.append(ArduinoConst.HEADER_PATH_SUFFIX).toOSString());
			pathPrefs.setValue(ArduinoConst.KEY_NO_SCAN_AT_STARTUP, "true");
		}
		if (Platform.getOS().equals(Platform.OS_LINUX)) {
			pathPrefs.setValue(ArduinoConst.KEY_GCC_PATH, ArduinoConst.KEY_SYSTEM);
			pathPrefs.setValue(ArduinoConst.KEY_GNU_PATH, ArduinoConst.KEY_SYSTEM);
			pathPrefs.setValue(ArduinoConst.KEY_HEADER_PATH, ArduinoConst.KEY_SYSTEM);
		
//			pathPrefs.setValue(ArduinoConst.KEY_GCC_SYSTEM_PATH, "");
//			pathPrefs.setValue(ArduinoConst.KEY_GNU_SYSTEM_PATH, ""toOSString());
//			pathPrefs.setValue(ArduinoConst.KEY_HEADER_SYSTEM_PATH, mArduinoPath.append(ArduinoConst.HEADER_PATH_SUFFIX).toOSString());
			pathPrefs.setValue(ArduinoConst.KEY_NO_SCAN_AT_STARTUP, "false");
		}
		try {
			AVRPathsPreferences.savePreferences(pathPrefs);
		} catch (IOException e) {
			IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to save AVR path settings settings", e);
			AVRPlugin.getDefault().log(status);
			e.printStackTrace();
		}
	}

	/**
	 * setArduinoPath stores the arduino path in the preferences
	 * 
	 * @param ArduinoPath
	 *            the path to be stores
	 * 
	 * @author Jan Baeyens
	 */
	public static void setArduinoPath(IPath ArduinoPath) {
		setGlobalValue(ArduinoConst.KEY_ARDUINOPATH, ArduinoPath.toOSString());
	}

	/**
	 * setUseIDESettings stores the flag in the preferences
	 * 
	 * @param booleanValue
	 *            the use ide settings flag to store
	 * @author Jan Baeyens
	 */
	public static void setUseIDESettings(boolean booleanValue) {
		setGlobalValue(ArduinoConst.KEY_USE_ARDUINO_IDE_TOOLS, booleanValue ? "true" : "false");
	}

	/**
	 * getUseIDESettings get the UseIDESettings flag value from the preference
	 * store
	 * 
	 * @return the value in the preference store representing the UseIDESettings
	 *         flag
	 * @author Jan Baeyens
	 */
	public static boolean getUseIDESettings() {
		return (getGlobalValue(ArduinoConst.KEY_USE_ARDUINO_IDE_TOOLS) == "true");
	}

}
