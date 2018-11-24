package io.sloeber.ui.helpers;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.prefs.BackingStoreException;

import io.sloeber.core.api.Defaults;
import io.sloeber.ui.Activator;
import io.sloeber.ui.Messages;

/**
 * ArduinoPreferences is a class containing only static methods that help
 * managing the preferences.
 * 
 * @author Jan Baeyens
 * 
 */
public class MyPreferences {

	private static final String FALSE = "FALSE"; //$NON-NLS-1$
	private static final String TRUE = "TRUE"; //$NON-NLS-1$
	public static final String KEY_BUILD_BEFORE_UPLOAD_OPTION = "Build before upload option"; //$NON-NLS-1$
	public static final String NODE_ARDUINO = "io.sloeber.core.ui"; //$NON-NLS-1$
	public static final String KEY_OPEN_SERIAL_WITH_MONITOR = "Open serial connections with the monitor"; //$NON-NLS-1$
	public static final String KEY_CLEAN_MONITOR_AFTER_UPLOAD = "Clean Serial Monitor after upload"; //$NON-NLS-1$
	public static final String KEY_ENABLE_PARALLEL_BUILD_FOR_NEW_PROJECTS = "Enable parallel build for new projects"; //$NON-NLS-1$
	public static final String KEY_AUTO_INSTALL_LIBRARIES = "Gui entry for install libraries"; //$NON-NLS-1$
	
	public static final boolean DEFAULT_OPEN_SERIAL_WITH_MONITOR = true;
	// Serial monitor keys
	private static final String KEY_SERIAL_RATE = "Serial monitor last selected rate"; //$NON-NLS-1$
	private static final String KEY_SERIAL_PORT = "Serial monitor last selected Port"; //$NON-NLS-1$
	private static final String KEY_RXTX_LAST_USED_LINE_INDES = "Serial Monitor Last Used Line Ending index"; //$NON-NLS-1$
	private static final String KEY_RXTX_LAST_USED_AUTOSCROLL = "Serial Monitor Last Used auto scroll setting"; //$NON-NLS-1$
	private static final String KEY_LAST_USED_PLOTTER_FILTER_MENU_OPTION = "Board plotter filter on off"; //$NON-NLS-1$
	private static final String KEY_HIDE_JSON_FILES = "Hide json files in preferences platform selection page"; //$NON-NLS-1$
	

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
		case TRUE:
			return true;
		case FALSE:
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

				MessageDialog dialog = new MessageDialog(null, Messages.build_before_upload, null,
						Messages.do_you_want_to_build_before_upload, MessageDialog.QUESTION,
						new String[] { Messages.yes, Messages.no, Messages.always, Messages.never }, 0);

				switch (dialog.open()) {
				case 0:
					this.ret = true;
					break;
				case 1:
					this.ret = false;
					break;
				case 2:
					setGlobalValue(KEY_BUILD_BEFORE_UPLOAD_OPTION, TRUE);
					this.ret = true;
					break;
				case 3:
					setGlobalValue(KEY_BUILD_BEFORE_UPLOAD_OPTION, FALSE);
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

	public static boolean getOpenSerialWithMonitor() {
		return getGlobalBoolean(KEY_OPEN_SERIAL_WITH_MONITOR, DEFAULT_OPEN_SERIAL_WITH_MONITOR);
	}

	public static void setOpenSerialWithMonitor(boolean value) {
		setGlobalValue(KEY_OPEN_SERIAL_WITH_MONITOR, value);
	}

	private static String getGlobalString(String key, String defaultValue) {
		IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
		return myScope.get(key, defaultValue);
	}

	private static boolean getGlobalBoolean(String key, boolean def) {
		IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
		return myScope.getBoolean(key, def);
	}

	private static int getGlobalInt(String key) {
		IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
		return myScope.getInt(key, 0);
	}

	@SuppressWarnings("unused")
	private static long getGlobalLong(String key) {
		IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
		return myScope.getLong(key, 0);
	}

	static void setGlobalValue(String key, String value) {

		IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
		myScope.put(key, value);
		try {
			myScope.flush();
		} catch (BackingStoreException e) {
			Activator.log(new Status(IStatus.WARNING, Activator.getId(),
					"failed to set global variable of type string " + key)); //$NON-NLS-1$
			e.printStackTrace();
		}
	}

	private static void setGlobalValue(String key, int value) {
		IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
		myScope.putInt(key, value);
		try {
			myScope.flush();
		} catch (BackingStoreException e) {
			Activator.log(
					new Status(IStatus.WARNING, Activator.getId(), "failed to set global variable of type int " + key)); //$NON-NLS-1$
			e.printStackTrace();
		}
	}

	private static void setGlobalValue(String key, boolean value) {
		IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
		myScope.putBoolean(key, value);
		try {
			myScope.flush();
		} catch (BackingStoreException e) {
			Activator.log(new Status(IStatus.WARNING, Activator.getId(),
					"failed to set global variable of type boolean " + key)); //$NON-NLS-1$
			e.printStackTrace();
		}
	}

	@SuppressWarnings("unused")
	private static void setGlobalValue(String key, long value) {
		IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
		myScope.putLong(key, value);
		try {
			myScope.flush();
		} catch (BackingStoreException e) {
			Activator.log(new Status(IStatus.WARNING, Activator.getId(),
					"failed to set global variable of type long " + key)); //$NON-NLS-1$
			e.printStackTrace();
		}
	}

	public static boolean getCleanSerialMonitorAfterUpload() {
		return getGlobalBoolean(KEY_CLEAN_MONITOR_AFTER_UPLOAD, false);

	}

	public static boolean getEnableParallelBuildForNewProjects() {
		return getGlobalBoolean(KEY_ENABLE_PARALLEL_BUILD_FOR_NEW_PROJECTS, false);

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

	public static void setCleanSerialMonitorAfterUpload(boolean newFilter) {
		setGlobalValue(KEY_CLEAN_MONITOR_AFTER_UPLOAD, newFilter);
	}


	public static void setEnableParallelBuildForNewProjects(boolean newSetting) {
		setGlobalValue(KEY_ENABLE_PARALLEL_BUILD_FOR_NEW_PROJECTS, newSetting);

	}

	public static boolean getLastUsedPlotterFilter() {
		return getGlobalBoolean(KEY_LAST_USED_PLOTTER_FILTER_MENU_OPTION, false);

	}

	public static void setLastUsedPlotterFilter(boolean newFilter) {
		setGlobalValue(KEY_LAST_USED_PLOTTER_FILTER_MENU_OPTION, newFilter);

	}

	public static void setLastUsedBaudRate(String text) {
		setGlobalValue(KEY_SERIAL_RATE, text);
	}

	public static void setLastUsedPort(String selectedPort) {
		setGlobalValue(KEY_SERIAL_PORT, selectedPort);

	}

	public static String getLastUsedRate() {
		return getGlobalString(KEY_SERIAL_RATE, ""); //$NON-NLS-1$
	}

	public static String getLastUsedPort() {
		return getGlobalString(KEY_SERIAL_PORT, ""); //$NON-NLS-1$
	}

	public static boolean getHideJson() {
		return getGlobalBoolean(KEY_HIDE_JSON_FILES, true);
	}

	public static void setHideJson(boolean state) {
		setGlobalValue(KEY_HIDE_JSON_FILES, state);
	}
	
	public static boolean getAutomaticallyInstallLibrariesOption() {
		return getGlobalBoolean(KEY_AUTO_INSTALL_LIBRARIES,Defaults.autoInstallLibraries);
	}
}
