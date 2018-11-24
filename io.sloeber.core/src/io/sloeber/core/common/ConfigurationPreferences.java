package io.sloeber.core.common;

import java.io.File;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.TreeSet;

import org.eclipse.cdt.core.parser.util.StringUtil;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

import io.sloeber.core.Activator;
import io.sloeber.core.api.Defaults;

/**
 * Items on the Configuration level are linked to the ConfigurationScope
 * (=eclipse install base).
 *
 * @author jan
 *
 */
public class ConfigurationPreferences {

	private static String stringSplitter = "\n";//$NON-NLS-1$
	private static final String EXAMPLE_FOLDER_NAME = "examples"; //$NON-NLS-1$
	private static final String DOWNLOADS_FOLDER = "downloads"; //$NON-NLS-1$
	private static final String PRE_PROCESSING_PLATFORM_TXT = "pre_processing_platform.txt"; //$NON-NLS-1$
	private static final String POST_PROCESSING_PLATFORM_TXT = "post_processing_platform.txt"; //$NON-NLS-1$
	private static final String PRE_PROCESSING_BOARDS_TXT = "pre_processing_boards.txt"; //$NON-NLS-1$
	private static final String POST_PROCESSING_BOARDS_TXT = "post_processing_boards.txt"; //$NON-NLS-1$
	private static final String KEY_UPDATE_JASONS = "Update jsons files"; //$NON-NLS-1$
	private static final String KEY_MANAGER_JSON_URLS_V3 = "Arduino Manager board Urls"; //$NON-NLS-1$
	private static final String KEY_MANAGER_ARDUINO_LIBRARY_JSON_URL = "http://downloads.arduino.cc/libraries/library_index.json"; //$NON-NLS-1$
	private static final String KEY_MANAGER_JSON_URLS = "Manager jsons"; //$NON-NLS-1$
	private static final String DEFAULT_JSON_URLS = "http://downloads.arduino.cc/packages/package_index.json\n" //$NON-NLS-1$
			+ "https://raw.githubusercontent.com/jantje/hardware/master/package_jantje_index.json\n" //$NON-NLS-1$
			+ "https://raw.githubusercontent.com/jantje/ArduinoLibraries/master/library_jantje_index.json\n" //$NON-NLS-1$
			+ "http://arduino.esp8266.com/stable/package_esp8266com_index.json\n" + KEY_MANAGER_ARDUINO_LIBRARY_JSON_URL; //$NON-NLS-1$
	// preference nodes
	private static final String NODE_ARDUINO = Activator.NODE_ARDUINO;
	private static final String LIBRARY_PATH_SUFFIX = "libraries"; //$NON-NLS-1$
	private static final String PACKAGES_FOLDER_NAME = "packages"; //$NON-NLS-1$

	private ConfigurationPreferences() {
	}

	private static void removeKey(String key) {
		IEclipsePreferences myScope = ConfigurationScope.INSTANCE.getNode(NODE_ARDUINO);
		myScope.remove(key);
	}

	private static String getString(String key, String defaultValue) {
		IEclipsePreferences myScope = ConfigurationScope.INSTANCE.getNode(NODE_ARDUINO);
		return myScope.get(key, defaultValue);
	}

	private static boolean getBoolean(String key, boolean defaultValue) {
		IEclipsePreferences myScope = ConfigurationScope.INSTANCE.getNode(NODE_ARDUINO);
		return myScope.getBoolean(key, defaultValue);
	}

	private static void setBoolean(String key, boolean value) {
		IEclipsePreferences myScope = ConfigurationScope.INSTANCE.getNode(NODE_ARDUINO);
		myScope.putBoolean(key, value);
		try {
			myScope.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	private static void setString(String key, String value) {
		IEclipsePreferences myScope = ConfigurationScope.INSTANCE.getNode(NODE_ARDUINO);
		myScope.put(key, value);
		try {
			myScope.flush();
		} catch (BackingStoreException e) {
			e.printStackTrace();
		}
	}

	private static Path myEclipseHome = null;

	public static Path getEclipseHome() {
		if (myEclipseHome == null) {

			try {
				URL resolvedUrl = Platform.getInstallLocation().getURL();
				URI resolvedUri = new URI(resolvedUrl.getProtocol(), resolvedUrl.getPath(), null);
				myEclipseHome = new Path(Paths.get(resolvedUri).toString());
			} catch (URISyntaxException e) {
				// this should not happen
				// but it seems a space in the path makes it happen
				Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
						"Eclipse fails to provide its own installation folder :-(. \nThis is known to happen when you have a space ! # or other wierd characters in your eclipse installation path", //$NON-NLS-1$
						e));
			}
		}
		return myEclipseHome;

	}

	public static IPath getInstallationPath() {
		return getEclipseHome().append("arduinoPlugin"); //$NON-NLS-1$
	}

	public static IPath getInstallationPathLibraries() {
		return getInstallationPath().append(LIBRARY_PATH_SUFFIX);
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

	public static String getJsonURLs() {
		// TODO I added some code here to get easier from V3 to V4
		// the library json url is now managed as the boards url's so it also
		// needs to be added to the json url's
		// this is doen in the default but people who have installed other
		// boards or do not move to the default (which is by default)
		// wil not see libraries
		// to fix this I changed the storage name and if the new storage name is
		// empty I read the ol one and add the lib
		String ret = getString(KEY_MANAGER_JSON_URLS, DEFAULT_JSON_URLS);
		if (DEFAULT_JSON_URLS.equals(ret)) {
			ret = getString(KEY_MANAGER_JSON_URLS_V3, DEFAULT_JSON_URLS);
			if (!DEFAULT_JSON_URLS.equals(ret)) {
				ret += System.lineSeparator() + KEY_MANAGER_ARDUINO_LIBRARY_JSON_URL;
				setString(KEY_MANAGER_JSON_URLS, ret);
				removeKey(KEY_MANAGER_JSON_URLS_V3);
			}
		}
		return ret;
	}

	public static String getDefaultJsonURLs() {
		return DEFAULT_JSON_URLS;
	}

	public static String[] getJsonURLList() {
		return getJsonURLs().replace("\r", new String()).split(stringSplitter); //$NON-NLS-1$
	}

	public static String getJsonUrlsKey() {
		return KEY_MANAGER_JSON_URLS;
	}

	public static void setJsonURLs(String urls) {
		setString(KEY_MANAGER_JSON_URLS, urls);
	}

	public static void setJsonURLs(String urls[]) {
		setString(KEY_MANAGER_JSON_URLS, StringUtil.join(urls, stringSplitter));
	}

	public static void setJsonURLs(HashSet<String> urls) {
		setString(KEY_MANAGER_JSON_URLS, StringUtil.join(urls, stringSplitter));
	}

	public static Path getMakePath() {
		return new Path(getInstallationPath().append("tools/make").toString()); //$NON-NLS-1$

	}

	public static boolean getUpdateJasonFilesFlag() {
		return getBoolean(KEY_UPDATE_JASONS, Defaults.updateJsonFiles);
	}

	public static void setUpdateJasonFilesFlag(boolean newFlag) {
		setBoolean(KEY_UPDATE_JASONS, newFlag);
	}

	private static String systemHash = null;

	/**
	 * Make a unique hashKey based on system parameters so we can identify users To
	 * make the key the mac addresses of the network cards are used
	 *
	 * @return a unique key identifying the system
	 */
	public static String getSystemHash() {
		if (systemHash != null) {
			return systemHash;
		}
		Collection<String> macs = new TreeSet<>();
		Enumeration<NetworkInterface> inters;
		try {
			inters = NetworkInterface.getNetworkInterfaces();

			while (inters.hasMoreElements()) {
				NetworkInterface inter = inters.nextElement();
				if (inter.getHardwareAddress() == null) {
					continue;
				}
				if (inter.isVirtual()) {
					continue;
				}
				byte curmac[] = inter.getHardwareAddress();
				StringBuilder b = new StringBuilder();
				for (byte curbyte : curmac) {
					b.append(String.format("%02X", new Byte(curbyte))); //$NON-NLS-1$
				}
				macs.add(b.toString());
			}
		} catch (SocketException e) {
			// ignore
		}
		Integer hascode = new Integer(macs.toString().hashCode());
		systemHash = hascode.toString();
		return systemHash;
	}


}
