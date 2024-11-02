package io.sloeber.core.api;

import static io.sloeber.core.api.Const.*;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;

import org.eclipse.cdt.core.parser.util.StringUtil;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import cc.arduino.packages.discoverers.SloeberNetworkDiscovery;
import io.sloeber.core.common.InstancePreferences;

/**
 * Items on the Configuration level are linked to the ConfigurationScope
 * (=eclipse install base).
 *
 * @author jan
 *
 */
public class ConfigurationPreferences {

	private static final String EXAMPLE_FOLDER_NAME = "examples"; //$NON-NLS-1$
	private static final String DOWNLOADS_FOLDER = "downloads"; //$NON-NLS-1$
	private static final String PRE_PROCESSING_PLATFORM_TXT = "pre_processing_platform.txt"; //$NON-NLS-1$
	private static final String POST_PROCESSING_PLATFORM_TXT = "post_processing_platform.txt"; //$NON-NLS-1$
	private static final String PRE_PROCESSING_BOARDS_TXT = "pre_processing_boards.txt"; //$NON-NLS-1$
	private static final String POST_PROCESSING_BOARDS_TXT = "post_processing_boards.txt"; //$NON-NLS-1$

	private static final String KEY_LATEST_JSON_UPDATE_TIME ="latest time the json files were updated";//$NON-NLS-1$
	private static final String KEY_JSON_UPDATE_DELAY="Duration between json file updates";//$NON-NLS-1$

    private static String stringSplitter = "\n";//$NON-NLS-1$
    private static final String KEY_DISCONNECT_SERIAL_TAGETS = "Target names that require serial disconnect to run";//$NON-NLS-1$

	public static void setAutoImportLibraries(boolean booleanValue) {
		InstancePreferences.setAutomaticallyImportLibraries(booleanValue);

	}

	public static void setPragmaOnceHeaders(boolean booleanValue) {
		InstancePreferences.setPragmaOnceHeaders(booleanValue);

	}

	public static boolean getPragmaOnceHeaders() {
		return InstancePreferences.getPragmaOnceHeaders();
	}

	public static boolean getAutoImportLibraries() {
		return InstancePreferences.getAutomaticallyImportLibraries();
	}

	public static void setUseArduinoToolSelection(boolean booleanValue) {
		InstancePreferences.setUseArduinoToolSelection(booleanValue);

	}

	public static boolean getUseArduinoToolSelection() {
		return InstancePreferences.getUseArduinoToolSelection();
	}

//	public static void setUpdateJsonFiles(boolean flag) {
//		ConfigurationPreferences.setUpdateJasonFilesFlag(flag);
//	}
//	public static boolean getUpdateJsonFiles() {
//		return ConfigurationPreferences.getUpdateJasonFilesFlag();
//	}

	/**
	 *wrapper for ConfigurationPreferences.useBonjour();
	 */
	public static boolean useBonjour() {
		return InstancePreferences.useBonjour();
	}

	/**
	 *wrapper for ConfigurationPreferences.setUseBonjour(newFlag);
	 */
	public static void setUseBonjour(boolean newFlag) {
		InstancePreferences.setUseBonjour(newFlag);
		if(newFlag) {
			SloeberNetworkDiscovery.start();
		}else {
			SloeberNetworkDiscovery.stop();
		}
	}



    public static String[] getDisconnectSerialTargetsList() {
        return getDisconnectSerialTargets().split(stringSplitter);
    }

    public static String getDisconnectSerialTargets() {
        return getString(KEY_DISCONNECT_SERIAL_TAGETS, Defaults.getDefaultDisconnectSerialTargets()).replace("\r", EMPTY);//$NON-NLS-1$
    }

    public static void setDisconnectSerialTargets(String targets) {
        setString(KEY_DISCONNECT_SERIAL_TAGETS, targets);
    }

    public static void setDisconnectSerialTargets(String targets[]) {
        setString(KEY_DISCONNECT_SERIAL_TAGETS, StringUtil.join(targets, stringSplitter));
    }

    public static void setDisconnectSerialTargets(HashSet<String> targets) {
        setString(KEY_DISCONNECT_SERIAL_TAGETS, StringUtil.join(targets, stringSplitter));
    }

	public static void removeKey(String key) {
		IEclipsePreferences myScope = ConfigurationScope.INSTANCE.getNode(NODE_ARDUINO);
		myScope.remove(key);
	}

	public static String getString(String key, String defaultValue) {
		IEclipsePreferences myScope = ConfigurationScope.INSTANCE.getNode(NODE_ARDUINO);
		return myScope.get(key, defaultValue);
	}


	private static Instant getInstant(String key, Instant defaultValue) {
		IEclipsePreferences myScope = ConfigurationScope.INSTANCE.getNode(NODE_ARDUINO);
		long ret = myScope.getLong(key, 0);
		if(ret==0) {
			setInstant( key,  defaultValue);
			return defaultValue;
		}
		return Instant.ofEpochSecond(ret);
	}

	private static void setInstant(String key, Instant value) {
		IEclipsePreferences myScope = ConfigurationScope.INSTANCE.getNode(NODE_ARDUINO);
		myScope.putLong(key, value.getEpochSecond());
		try {
			myScope.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	public static void setString(String key, String value) {
		IEclipsePreferences myScope = ConfigurationScope.INSTANCE.getNode(NODE_ARDUINO);
		if (value == null) {
			myScope.remove(key);
		} else {
			myScope.put(key, value);
		}
		try {
			myScope.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	public static IPath getInstallationPath() {
		return Common.sloeberHomePath.append(SLOEBER_HOME_SUB_FOLDER);
	}

	public static IPath getInstallationPathLibraries() {
		return getInstallationPath().append(ARDUINO_LIBRARY_FOLDER_NAME);
	}

	public static IPath getInstallationPathExamples() {
		return getInstallationPath().append(EXAMPLE_FOLDER_NAME);
	}

	public static IPath getInstallationPathDownload() {
		return getInstallationPath().append(DOWNLOADS_FOLDER);
	}

	public static IPath getInstallationPathPackages() {
		return getInstallationPath().append(PACKAGES_FOLDER_NAME);
	}

	/**
	 * Get the file that contains the preprocessing platform content
	 *
	 * @return
	 */
	public static File getPreProcessingPlatformFile() {
		return getInstallationPath().append(PRE_PROCESSING_PLATFORM_TXT).toFile();
	}

	/**
	 * Get the file that contains the post processing platform content
	 *
	 * @return
	 */
	public static File getPostProcessingPlatformFile() {
		return getInstallationPath().append(POST_PROCESSING_PLATFORM_TXT).toFile();
	}

	public static File getPreProcessingBoardsFile() {
		return getInstallationPath().append(PRE_PROCESSING_BOARDS_TXT).toFile();
	}

	public static File getPostProcessingBoardsFile() {
		return getInstallationPath().append(POST_PROCESSING_BOARDS_TXT).toFile();
	}

	public static Path getMakePath() {
		return new Path(getInstallationPath().append("tools/make").toString()); //$NON-NLS-1$

	}

	public static IPath getAwkPath() {
		return new Path(getInstallationPath().append("tools/awk").toString()); //$NON-NLS-1$
	}

	public static Instant getLatestJsonUpdateTime() {
		return getInstant(KEY_LATEST_JSON_UPDATE_TIME, Instant.now());
	}

	public static void setLatestUpdateTime(Instant currentTime) {
		setInstant(KEY_LATEST_JSON_UPDATE_TIME,currentTime);

	}

	public static Duration getJsonUpdateDelay() {
		return getDuration(KEY_JSON_UPDATE_DELAY,Defaults.getJsonUpdateDuration());
	}

	public static void setJsonUpdateDelay(Duration jsunUpdateDuration) {
		setDuration(KEY_JSON_UPDATE_DELAY,jsunUpdateDuration);
	}

	private static void setDuration(String key, Duration value) {
		IEclipsePreferences myScope = ConfigurationScope.INSTANCE.getNode(NODE_ARDUINO);
		myScope.putLong(key, value.toDays());
		try {
			myScope.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	private static Duration getDuration(String key, Duration defaultValue) {
		IEclipsePreferences myScope = ConfigurationScope.INSTANCE.getNode(NODE_ARDUINO);
		long ret = myScope.getLong(key, 0);
		if(ret==0) {
			return defaultValue;
		}
		return Duration.ofDays(ret);
	}

}
