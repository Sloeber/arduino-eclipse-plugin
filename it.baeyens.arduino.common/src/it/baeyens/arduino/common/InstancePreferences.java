package it.baeyens.arduino.common;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.prefs.BackingStoreException;

/**
 * ArduinoPreferences is a class containing only static methods that help
 * managing the preferences.
 * 
 * @author jan Baeyens
 * 
 */
public class InstancePreferences extends Const {

    public static boolean getOpenSerialWithMonitor() {
	return getGlobalBoolean(KEY_OPEN_SERIAL_WITH_MONITOR, Defaults.OPEN_SERIAL_WITH_MONITOR);
    }

    public static void setOpenSerialWithMonitor(boolean value) {
	setGlobalValue(KEY_OPEN_SERIAL_WITH_MONITOR, value);
    }

    /**
     * Give back the user option if the libraries need to be added or not
     * 
     * @return true if libraries need to be added else false.
     */
    public static boolean getAutomaticallyIncludeLibraries() {
	return getGlobalBoolean(KEY_AUTO_IMPORT_LIBRARIES, Defaults.AUTO_IMPORT_LIBRARIES);
    }

    public static void setAutomaticallyIncludeLibraries(boolean value) {
	setGlobalValue(KEY_AUTO_IMPORT_LIBRARIES, value);
    }

    /***
     * get the stored option whether a build before the upload is wanted or not.
     * If nothing is stored the option is ask and this method will pop up a
     * dialogbox
     * 
     * @return true if a build is wanted before upload false if no build is
     *         wanted before upload
     */
    public static boolean getBuildBeforeUploadOption() {

	switch (getGlobalString(KEY_BUILD_BEFORE_UPLOAD_OPTION, "ASK")) { //$NON-NLS-1$
	case Const.TRUE:
	    return true;
	case Const.FALSE:
	    return false;
	default:
	    break;
	}
	class TheDialog implements Runnable {
	    boolean ret = false;

	    boolean getAnswer() {
		return this.ret;
	    }

	    @Override
	    public void run() {

		MessageDialog dialog = new MessageDialog(null, Messages.Build_before_upload, null,
			Messages.do_you_want_to_build_before_upload, MessageDialog.QUESTION,
			new String[] { "Yes", "No", "Always", "Never" }, 0); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		switch (dialog.open()) {
		case 0:
		    this.ret = true;
		    break;
		case 1:
		    this.ret = false;
		    break;
		case 2:
		    setGlobalValue(KEY_BUILD_BEFORE_UPLOAD_OPTION, Const.TRUE);
		    this.ret = true;
		    break;
		case 3:
		    setGlobalValue(KEY_BUILD_BEFORE_UPLOAD_OPTION, Const.FALSE);
		    this.ret = false;
		    break;
		default:
		    this.ret = false;
		    break;
		}
	    }
	}
	TheDialog theDialog = new TheDialog();
	Display.getDefault().syncExec(theDialog);
	return theDialog.getAnswer();
    }

    /**
     * This method reads the name of the last used arduino board from the
     * instance preferences
     * 
     * @return the Arduino Board name
     * @author Jan Baeyens
     */
    public static String getLastUsedArduinoBoardName() {
	return getGlobalString(KEY_LAST_USED_BOARD, ""); //$NON-NLS-1$
    }

    /**
     * This method reads the arduino upload port from the configuration memory
     * 
     * @return the upload port
     * @author Jan Baeyens
     */
    public static String getLastUsedUploadPort() {
	return getGlobalString(KEY_LAST_USED_COM_PORT, ""); //$NON-NLS-1$
    }

    /**
     * saves the last used arduino upload port
     * 
     * @param UploadPort
     *            The port to use to upload to save
     * 
     * @author Jan Baeyens
     */
    public static void setLastUsedUploadPort(String UploadPort) {
	setGlobalValue(KEY_LAST_USED_COM_PORT, UploadPort);

    }

    public static String getLastUsedUploadProtocol() {
	return getGlobalString(KEY_LAST_USED_UPLOAD_PROTOCOL, DEFAULT);
    }

    public static void setLastUsedUploadProtocol(String uploadProtocol) {
	setGlobalValue(KEY_LAST_USED_UPLOAD_PROTOCOL, uploadProtocol);

    }

    /**
     * saves the last used arduino board name
     * 
     * @param ArduinoBoardName
     *            The arduino board name to save
     * 
     * @author Jan Baeyens
     */
    public static void setLastUsedArduinoBoard(String ArduinoBoardName) {
	setGlobalValue(KEY_LAST_USED_BOARD, ArduinoBoardName);
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

    public static void setGlobalValue(String key, String Value) {

	IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
	myScope.put(key, Value);
	try {
	    myScope.flush();
	} catch (BackingStoreException e) {
	    Common.log(
		    new Status(IStatus.WARNING, CORE_PLUGIN_ID, "failed to set global variable of type string " + key)); //$NON-NLS-1$
	    e.printStackTrace();
	}
    }

    protected static void setGlobalValue(String key, int Value) {
	IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
	myScope.putInt(key, Value);
	try {
	    myScope.flush();
	} catch (BackingStoreException e) {
	    Common.log(new Status(IStatus.WARNING, CORE_PLUGIN_ID, "failed to set global variable of type int " + key)); //$NON-NLS-1$
	    e.printStackTrace();
	}
    }

    protected static void setGlobalValue(String key, boolean Value) {
	IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
	myScope.putBoolean(key, Value);
	try {
	    myScope.flush();
	} catch (BackingStoreException e) {
	    Common.log(new Status(IStatus.WARNING, CORE_PLUGIN_ID,
		    "failed to set global variable of type boolean " + key)); //$NON-NLS-1$
	    e.printStackTrace();
	}
    }

    protected static void setGlobalValue(String key, long Value) {
	IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
	myScope.putLong(key, Value);
	try {
	    myScope.flush();
	} catch (BackingStoreException e) {
	    Common.log(
		    new Status(IStatus.WARNING, CORE_PLUGIN_ID, "failed to set global variable of type long " + key)); //$NON-NLS-1$
	    e.printStackTrace();
	}
    }

    /**
     * This method returns the index of the last used line ending options are CR
     * LF CR+LF none
     * 
     * @return the index of the last used setting
     */
    public static int getLastUsedSerialLineEnd() {
	return getGlobalInt(KEY_RXTX_LAST_USED_LINE_INDES);
    }

    /**
     * This method returns the index of the last used line ending options are CR
     * LF CR+LF none
     * 
     * @return the index of the last used setting
     */
    public static void setLastUsedSerialLineEnd(int index) {
	setGlobalValue(KEY_RXTX_LAST_USED_LINE_INDES, index);
    }

    public static boolean getLastUsedAutoScroll() {
	return getGlobalBoolean(KEY_RXTX_LAST_USED_AUTOSCROLL, false);
    }

    public static void setLastUsedAutoScroll(boolean autoScroll) {
	setGlobalValue(KEY_RXTX_LAST_USED_AUTOSCROLL, autoScroll);

    }

    public static String getLastUsedBoardsFile() {
	return getGlobalString(KEY_LAST_USED_BOARDS_FILE, ""); //$NON-NLS-1$
    }

    public static void setLastUsedBoardsFile(String boardsFile) {
	setGlobalValue(KEY_LAST_USED_BOARDS_FILE, boardsFile);

    }

    public static void setLastUsedMenuOption(Map<String, String> menuOptions) {
	String store = ""; //$NON-NLS-1$
	String concat = ""; //$NON-NLS-1$
	for (Entry<String, String> curOption : menuOptions.entrySet()) {
	    store = store + concat + curOption.getKey() + '=' + curOption.getValue();
	    concat = "\n"; //$NON-NLS-1$
	}
	setGlobalValue(KEY_LAST_USED_BOARD_MENU_OPTIONS, store);

    }

    public static Map<String, String> getLastUsedMenuOption() {
	Map<String, String> options = new HashMap<>();
	String storedValue = getGlobalString(KEY_LAST_USED_BOARD_MENU_OPTIONS, ""); //$NON-NLS-1$
	String lines[] = storedValue.split("\n"); //$NON-NLS-1$
	for (String curLine : lines) {
	    String values[] = curLine.split("=", 2); //$NON-NLS-1$
	    if (values.length == 2) {
		options.put(values[0], values[1]);
	    }
	}
	return options;
    }

    private static boolean mIsConfigured = false;

    public static void setConfigured() {
	mIsConfigured = true;
    }

    /**
     * This method returns boolean whether the plugin is properly configured The
     * plugin is configured properly if a board has been installed
     * 
     * @return
     */
    public static boolean isConfigured(boolean showError) {
	if (mIsConfigured)
	    return true;
	if (showError) {
	    // If not then we bail out with an error.
	    // And no pages are presented (with no option to FINISH).
	    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, Messages.Please_wait_for_installer_job, null));
	}
	return false;
    }

    public static boolean getLastUsedScopeFilter() {
	return getGlobalBoolean(KEY_LAST_USED_SCOPE_FILTER_MENU_OPTION, false);

    }

    public static void setLastUsedScopeFilter(boolean newFilter) {
	setGlobalValue(KEY_LAST_USED_SCOPE_FILTER_MENU_OPTION, newFilter);

    }

    //
    // get/set last used "use default sketch location"
    //
    public static int getLastUsedDefaultSketchSelection() {
	return getGlobalInt(ENV_KEY_JANTJE_SKETCH_TEMPLATE_USE_DEFAULT);
    }

    public static void setLastUsedDefaultSketchSelection(int newFilter) {
	setGlobalValue(ENV_KEY_JANTJE_SKETCH_TEMPLATE_USE_DEFAULT, newFilter);
    }

    //
    // get/set last used sketch template folder parameters
    //
    public static String getLastTemplateFolderName() {
	return getGlobalString(ENV_KEY_JANTJE_SKETCH_TEMPLATE_FOLDER, ""); //$NON-NLS-1$
    }

    public static void setLastTemplateFolderName(String folderName) {
	setGlobalValue(ENV_KEY_JANTJE_SKETCH_TEMPLATE_FOLDER, folderName);

    }

    public static String[] getLastUsedExamples() {
	return getGlobalString(KEY_LAST_USED_EXAMPLES, Defaults.getPrivateLibraryPath()).split("\n"); //$NON-NLS-1$
    }

    public static void setLastUsedExamples(String[] exampleNames) {
	setGlobalValue(KEY_LAST_USED_EXAMPLES, String.join("\n", exampleNames)); //$NON-NLS-1$
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

    public static void setPrivateHardwarePaths(String[] folderName) {
	setGlobalValue(KEY_PRIVATE_HARDWARE_PATHS, String.join(File.pathSeparator, folderName));
    }

    /**
     * Gets all the folders that can contain hardware
     * 
     * @return a list of all the folder locations that can contain hardware
     */
    public static String[] getHardwarePaths() {
	return (getGlobalString(KEY_PRIVATE_HARDWARE_PATHS, EMPTY_STRING) + File.pathSeparator
		+ ConfigurationPreferences.getInstallationPath()).split(File.pathSeparator);
    }

}
