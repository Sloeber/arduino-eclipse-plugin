package it.baeyens.arduino.common;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

/**
 * ArduinoPreferences is a class containing only static methods that help managing the preferences.
 * 
 * @author jan Baeyens
 * 
 */
public class ArduinoInstancePreferences extends ArduinoConst {

    /**
     * This method reads the name of the last used arduino board from the instance preferences
     * 
     * @return the Arduino Board name
     * @author Jan Baeyens
     */
    public static String getLastUsedArduinoBoardName() {
	return getGlobalValue(KEY_LAST_USED_ARDUINOBOARD);
    }

    /**
     * This method reads the arduino upload port from the configuration memory
     * 
     * @return the upload port
     * @author Jan Baeyens
     */
    public static String getLastUsedUploadPort() {
	return getGlobalValue(KEY_LAST_USED_COM_PORT);
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
	setGlobalValue(KEY_LAST_USED_COM_PORT, UploadPort);

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
	setGlobalValue(KEY_LAST_USED_ARDUINOBOARD, ArduinoBoardName);
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
	    Common.log(new Status(IStatus.WARNING, CORE_PLUGIN_ID, "failed to set global variable of type string " + key));
	    e.printStackTrace();
	}
    }

    protected static void setGlobalInt(String key, int Value) {
	IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
	myScope.putInt(key, Value);
	try {
	    myScope.flush();
	} catch (BackingStoreException e) {
	    Common.log(new Status(IStatus.WARNING, CORE_PLUGIN_ID, "failed to set global variable of type int " + key));
	    e.printStackTrace();
	}
    }

    protected static void setGlobalBoolean(String key, boolean Value) {
	IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
	myScope.putBoolean(key, Value);
	try {
	    myScope.flush();
	} catch (BackingStoreException e) {
	    Common.log(new Status(IStatus.WARNING, CORE_PLUGIN_ID, "failed to set global variable of type boolean " + key));
	    e.printStackTrace();
	}
    }

    /**
     * getUseIDESettings get the UseIDESettings flag value from the preference store
     * 
     * @return the value in the preference store representing the UseIDESettings flag
     * @author Jan Baeyens
     */
    public static String getIDEVersion() {
	return (getGlobalValue(KEY_ARDUINO_IDE_VERSION));
    }

    /**
     * Returns whether the arduino path in preferences is for arduino 1.0 IDE and later or before
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
    public static String GetARDUINODefineValue() {
	String Ret;
	Ret = getIDEVersion().trim();
	if (Ret.contains(".")) {
	    Ret = Ret.replace(".", "");
	    if (Ret.length() == 2) {
		Ret = Ret.concat("0");
	    }
	}
	return Ret;
    }

    /**
     * This method returns the index of the last used line ending options are CR LF CR+LF none
     * 
     * @return the index of the last used setting
     */
    public static int GetLastUsedSerialLineEnd() {
	return getGlobalInt(KEY_RXTX_LAST_USED_LINE_INDES);
    }

    /**
     * This method returns the index of the last used line ending options are CR LF CR+LF none
     * 
     * @return the index of the last used setting
     */
    public static void SetLastUsedSerialLineEnd(int LastUsedIndex) {
	setGlobalInt(KEY_RXTX_LAST_USED_LINE_INDES, LastUsedIndex);
    }

    public static boolean getLastUsedAutoScroll() {
	return getGlobalBoolean(KEY_RXTX_LAST_USED_AUTOSCROLL);
    }

    public static void setLastUsedAutoScroll(boolean autoScroll) {
	setGlobalBoolean(KEY_RXTX_LAST_USED_AUTOSCROLL, autoScroll);

    }

    public static String getLastUsedBoardsFile() {
	return getGlobalValue(KEY_LAST_USED_ARDUINO_BOARDS_FILE);
    }

    public static void setLastUsedBoardsFile(String boardsFile) {
	setGlobalValue(KEY_LAST_USED_ARDUINO_BOARDS_FILE, boardsFile);

    }

    public static void setLastUsedMenuOption(String menuOptions) {
	setGlobalValue(KEY_LAST_USED_ARDUINO_MENU_OPTIONS, menuOptions);

    }

    public static String getLastUsedMenuOption() {
	return getGlobalValue(KEY_LAST_USED_ARDUINO_MENU_OPTIONS);
    }

    public static IPath getArduinoPath() {
	return new Path(getGlobalValue(KEY_ARDUINOPATH)).append(Common.getArduinoIdeSuffix());

    }

    /**
     * This method returns boolean whether the arduino IDE has been pointed to or not. If you use the flag error = true a error will be shown to the
     * end user in case Arduino is not configured.
     * 
     * @return
     */
    public static boolean isConfigured(boolean showError) {
	if (ArduinoInstancePreferences.getArduinoPath().toFile().exists())
	    return true;
	if (showError) {
	    // If not then we bail out with an error.
	    // And no pages are presented (with no option to FINISH).
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Arduino IDE path does not exist. Check Window>Preferences>Arduino",
		    null));
	}
	return false;
    }

    public static boolean getLastUsedScopeFilter() {
	return getGlobalBoolean(KEY_LAST_USED_SCOPE_FILTER_MENU_OPTION);

    }

    public static void setLastUsedScopeFilter(boolean newFilter) {
	setGlobalBoolean(KEY_LAST_USED_SCOPE_FILTER_MENU_OPTION, newFilter);

    }
    //
    // get/set last used "use default sketch location"
    //
    public static boolean getLastUsedDefaultSketchSelection(){
	return getGlobalBoolean(ENV_KEY_SKETCH_TEMPLATE_USE_DEFAULT);
    }

    public static void setLastUsedDefaultSketchSelection(boolean newFilter) {
	setGlobalBoolean(ENV_KEY_SKETCH_TEMPLATE_USE_DEFAULT, newFilter);
    }
    //
    // get/set last used sketch template folder parameters
    //
    public static String getLastTemplateFolderName() {
	return getGlobalValue(ENV_KEY_SKETCH_TEMPLATE_FOLDER);
    }
    public static void setLastTemplateFolderName(String folderName) {
	setGlobalValue(ENV_KEY_SKETCH_TEMPLATE_FOLDER, folderName);

    }
}
