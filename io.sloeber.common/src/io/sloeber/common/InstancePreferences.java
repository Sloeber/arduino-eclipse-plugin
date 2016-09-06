package io.sloeber.common;

import java.io.File;

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
 * @author Jan Baeyens
 * 
 */
public class InstancePreferences extends Const {

    private static boolean mIsConfigured = false;
    private static final String KEY_CLEAN_MONITOR_AFTER_UPLOAD = "Clean Serial Monitor after upload"; //$NON-NLS-1$
    private static final String KEY_LAST_USED_SCOPE_FILTER_MENU_OPTION = "Board scope filter on off"; //$NON-NLS-1$

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

		MessageDialog dialog = new MessageDialog(null, Messages.buildBeforeUpload, null,
			Messages.doYouWantToBuildBeforeUpload, MessageDialog.QUESTION,
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

    public static void setLastUsedBoardsFile(String boardsFile) {
	setGlobalValue(KEY_LAST_USED_BOARDS_FILE, boardsFile);

    }

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
	    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, Messages.pleaseWaitForInstallerJob, null));
	}
	return false;
    }

    public static boolean getLastUsedScopeFilter() {
	return getGlobalBoolean(KEY_LAST_USED_SCOPE_FILTER_MENU_OPTION, false);

    }

    public static void setLastUsedScopeFilter(boolean newFilter) {
	setGlobalValue(KEY_LAST_USED_SCOPE_FILTER_MENU_OPTION, newFilter);

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

    public static String getCleanSerialMonitorAfterUploadKey() {
	return KEY_CLEAN_MONITOR_AFTER_UPLOAD;
    }

    public static boolean getCleanSerialMonitorAfterUpload() {
	return getGlobalBoolean(KEY_CLEAN_MONITOR_AFTER_UPLOAD, false);

    }

    public static void setCleanSerialMonitorAfterUpload(boolean newFilter) {
	setGlobalValue(KEY_CLEAN_MONITOR_AFTER_UPLOAD, newFilter);

    }
}
