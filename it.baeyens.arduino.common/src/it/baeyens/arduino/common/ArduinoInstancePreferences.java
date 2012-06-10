package it.baeyens.arduino.common;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

/**
 * ArduinoPreferences is a class containing only static methods that help
 * managing the preferences.
 * 
 * @author jan Baeyens
 * 
 */
public class ArduinoInstancePreferences extends ArduinoConst {

	/**
	 * This method reads the arduino path from the configuration memory
	 * 
	 * @return the arduino path
	 * @author Jan Baeyens
	 */
	public static Path getArduinoPath() {
		return new Path(getGlobalValue(KEY_ARDUINOPATH));
	}
	/**
	 * This method reads the private library path from the configuration memory
	 * 
	 * @return the path parent to the folders containing the private libraries 
	 * @author Jan Baeyens
	 */
	public static Path getPrivateLibraryPath() {
		return new Path(getGlobalValue(KEY_PRIVATE_LIBRARY_PATH));
	}
	
	
	public static IPath getArduinoLibraryPath()
		{
			return getArduinoPath().append(LIBRARY_PATH_SUFFIX);
		}
	/**
	 * This method reads the name of the last used arduino board from the
	 * instance preferences
	 * 
	 * @return the Arduino Board name
	 * @author Jan Baeyens
	 */
	public static String getLastUsedArduinoBoardName() {
		return getGlobalValue(KEY_ARDUINOBOARD);
	}

	/**
	 * This method reads the arduino upload port from the configuration memory
	 * 
	 * @return the upload port
	 * @author Jan Baeyens
	 */
	public static String getLastUsedUploadPort() {
		return getGlobalValue(KEY_ARDUINOPORT);
	}

	/**
	 * saves the last used arduino upload port
	 * 
	 * @param UploadPort
	 *            The port to use to upload to save
	 * 
	 * @author Jan Baeyens
	 */
	public static void SetLastUsedUploadPort(String UploadPort) {
		setGlobalValue(KEY_ARDUINOPORT, UploadPort);

	}

	/**
	 * saves the last used arduino board name
	 * 
	 * @param ArduinoBoardName
	 *            The arduino board name to save
	 * 
	 * @author Jan Baeyens
	 */
	public static void SetLastUsedArduinoBoard(String ArduinoBoardName) {
		setGlobalValue(KEY_ARDUINOBOARD, ArduinoBoardName);
	}

	public static String getGlobalValue(String key) {
		IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
		return myScope.get(key, "");
	}
	
	protected static boolean getGlobalBoolean(String key) {
		IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
		return myScope.getBoolean(key, false);
	}

	protected static int getGlobalInt(String key) {
		IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
		return myScope.getInt(key, 0);
	}
	
	public static void setGlobalValue(String key, String Value) {

		IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
		myScope.put(key, Value);
		try {
			myScope.flush();
		} catch (BackingStoreException e) {
			Common.log(new Status(Status.WARNING, CORE_PLUGIN_ID, "failed to set global variable of type string "+key));
			e.printStackTrace();
		}
	}
	
	protected static void setGlobalInt(String key, int Value) {
		IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
		myScope.putInt(key, Value);
		try {
			myScope.flush();
		} catch (BackingStoreException e) {
			Common.log(new Status(Status.WARNING, CORE_PLUGIN_ID, "failed to set global variable of type int "+key));
			e.printStackTrace();
		}
	}
	
	
	
	protected static void setGlobalBoolean(String key, boolean Value) {
		IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
		myScope.putBoolean(key, Value);
		try {
			myScope.flush();
		} catch (BackingStoreException e) {
			Common.log(new Status(Status.WARNING, CORE_PLUGIN_ID, "failed to set global variable of type boolean "+key));
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
		setGlobalValue(KEY_ARDUINOPATH, ArduinoPath.toOSString());
	}

	/**
	 * setUseIDESettings stores the flag in the preferences
	 * 
	 * @param booleanValue
	 *            the use ide settings flag to store
	 * @author Jan Baeyens
	 */
	public static void setUseIDESettings(boolean booleanValue) {
		setGlobalValue(KEY_USE_ARDUINO_IDE_TOOLS, booleanValue ? "true" : "false");
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
		return (getGlobalValue(KEY_USE_ARDUINO_IDE_TOOLS) == "true");
	}

	/**
	 * getUseIDESettings get the UseIDESettings flag value from the preference
	 * store
	 * 
	 * @return the value in the preference store representing the UseIDESettings
	 *         flag
	 * @author Jan Baeyens
	 */
	public static String getIDEVersion() {
		return (getGlobalValue(KEY_ARDUINO_IDE_VERSION));
	}

	/**
	 * Returns whether the arduino path in preferences is for arduino 1.0 IDE
	 * and later or before
	 * 
	 * @param project
	 *            the project to scan
	 * 
	 * @return true if it is arduino 1.0 or later; otherwise false
	 */
	public static boolean isArduinoIdeOne() {
		return !getIDEVersion().startsWith("00");
	}

	/*
	 * This method returns the define value for the define ARDUINO
	 */
	public static String GetARDUINODefineValue()
	{
		String Ret;
		Ret = getIDEVersion();
		if (Ret.contains("."))
		{
			String Suffix = Ret.substring(Ret.indexOf(".")+1);
			String Prefix=Ret.substring(0,Ret.indexOf("."));
			while (Suffix.length()<2)
					{
						Suffix=Suffix+"0";
					}
			Ret = Prefix + Suffix;
		}

		return Ret;
	}
	
	/**
	 * This method returns the index of the last used line ending options are CR LF CR+LF none
	 * 
	 * @return the index of the last used setting
	 */
	public static int GetLastUsedSerialLineEnd()
		{
			return getGlobalInt(KEY_RXTX_LAST_USED_LINE_INDES);
		}
	/**
	 * This method returns the index of the last used line ending options are CR LF CR+LF none
	 * 
	 * @return the index of the last used setting
	 */
	public static void SetLastUsedSerialLineEnd(int LastUsedIndex)
		{
			setGlobalInt(KEY_RXTX_LAST_USED_LINE_INDES,LastUsedIndex);
		}
	
	public static boolean GetLastUsedAutoScroll()
		{
			return getGlobalBoolean(KEY_RXTX_LAST_USED_AUTOSCROLL);
		}
	public static void SetLastUsedAutoScroll(boolean autoScroll)
		{
			setGlobalBoolean(KEY_RXTX_LAST_USED_AUTOSCROLL,autoScroll);
			
		}

}


