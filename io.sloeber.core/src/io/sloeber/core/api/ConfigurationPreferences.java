package io.sloeber.core.api;

import static io.sloeber.core.api.Const.*;

import java.io.File;
import java.time.Duration;
import java.time.Instant;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

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

	public static Instant getLatestUpdateTime() {
		return getInstant(KEY_LATEST_JSON_UPDATE_TIME, Instant.now());
	}

	public static void setLatestUpdateTime(Instant currentTime) {
		setInstant(KEY_LATEST_JSON_UPDATE_TIME,currentTime);

	}

	public static Duration getUpdateDelay() {
		// TODO Auto-generated method stub
		return Duration.ofDays(10);
	}

}
