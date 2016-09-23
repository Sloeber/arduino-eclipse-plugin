package io.sloeber.common;

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

/**
 * Items on the Configuration level are linked to the ConfigurationScope
 * (=eclipse install base).
 * 
 * @author jan
 *
 */
public class ConfigurationPreferences {

    private static String stringSplitter = "\n";//$NON-NLS-1$
    private static final String KEY_MANAGER_DOWNLOAD_LOCATION = "arduino Manager downloadlocation"; //$NON-NLS-1$
    private static final String DOWNLOADS_FOLDER = "downloads"; //$NON-NLS-1$
    private static final String PRE_PROCESSING_PLATFORM_TXT = "pre_processing_platform.txt"; //$NON-NLS-1$
    private static final String POST_PROCESSING_PLATFORM_TXT = "post_processing_platform.txt"; //$NON-NLS-1$
    private static final String PRE_PROCESSING_BOARDS_TXT = "pre_processing_boards.txt"; //$NON-NLS-1$
    private static final String POST_PROCESSING_BOARDS_TXT = "post_processing_boards.txt"; //$NON-NLS-1$
    private static final String KEY_UPDATE_JASONS = "Update jsons files"; //$NON-NLS-1$
    private static final String KEY_MANAGER_JSON_URLS = "Arduino Manager board Urls"; //$NON-NLS-1$
    private static final String DEFAULT_JSON_URLS = "http://downloads.arduino.cc/packages/package_index.json" //$NON-NLS-1$
	    + System.lineSeparator() + "http://arduino.esp8266.com/stable/package_esp8266com_index.json"; //$NON-NLS-1$

    private ConfigurationPreferences() {
    }

    private static String getString(String key, String defaultValue) {
	IEclipsePreferences myScope = ConfigurationScope.INSTANCE.getNode(Const.NODE_ARDUINO);
	return myScope.get(key, defaultValue);
    }

    private static boolean getBoolean(String key, boolean defaultValue) {
	IEclipsePreferences myScope = ConfigurationScope.INSTANCE.getNode(Const.NODE_ARDUINO);
	return myScope.getBoolean(key, defaultValue);
    }

    private static void setString(String key, String value) {
	IEclipsePreferences myScope = ConfigurationScope.INSTANCE.getNode(Const.NODE_ARDUINO);
	myScope.put(key, value);
	try {
	    myScope.flush();
	} catch (BackingStoreException e) {
	    e.printStackTrace();
	}
    }

    public static Path getInstallationPath() {
	final String argStart = "-manager_path:"; //$NON-NLS-1$
	String[] args = Platform.getApplicationArgs();
	for (String arg : args) {
	    if (arg.startsWith(argStart)) {
		String pathName = arg.substring(argStart.length());
		return new Path(pathName);
	    }
	}
	String storedValue = getString(KEY_MANAGER_DOWNLOAD_LOCATION, Const.EMPTY_STRING);
	if (storedValue.isEmpty()) {
	    try {
		URL resolvedUrl = Platform.getInstallLocation().getURL();
		URI resolvedUri = new URI(resolvedUrl.getProtocol(), resolvedUrl.getPath(), null);
		String defaulDownloadLocation = Paths.get(resolvedUri).resolve("arduinoPlugin").toString(); //$NON-NLS-1$
		return new Path(defaulDownloadLocation);
	    } catch (URISyntaxException e) {
		// this should not happen
		// but it seems a space in the path makes it happen
		Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
			"Eclipse fails to provide its own installation folder :-(. \nThis is know to happen when you have a space ! # or other wierd characters in your eclipse installation path", //$NON-NLS-1$
			e));
	    }
	}
	return new Path(storedValue);
    }

    public static IPath getInstallationPathLibraries() {
	return getInstallationPath().append(Const.LIBRARY_PATH_SUFFIX);
    }

    public static IPath getInstallationPathExamples() {
	return getInstallationPath().append(Const.EXAMPLE_FOLDER_NAME);
    }

    public static IPath getInstallationPathDownload() {
	return getInstallationPath().append(DOWNLOADS_FOLDER);
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

    public static String getBoardsPackageURLs() {
	return getString(KEY_MANAGER_JSON_URLS, DEFAULT_JSON_URLS);
    }

    public static String getDefaultBoardsPackageURLs() {
	return DEFAULT_JSON_URLS;
    }

    public static String[] getBoardsPackageURLList() {
	return getBoardsPackageURLs().replaceAll(Const.RETURN, Const.EMPTY_STRING).split(stringSplitter);
    }

    public static String getBoardsPackageKey() {
	return KEY_MANAGER_JSON_URLS;
    }

    public static void setBoardsPackageURLs(String urls) {
	setString(KEY_MANAGER_JSON_URLS, urls);
    }

    public static void setBoardsPackageURLs(String urls[]) {
	setString(KEY_MANAGER_JSON_URLS, StringUtil.join(urls, stringSplitter));
    }

    public static void setBoardsPackageURLs(HashSet<String> urls) {
	setString(KEY_MANAGER_JSON_URLS, StringUtil.join(urls, stringSplitter));
    }

    public static Path getPathExtensionPath() {
	return new Path(getInstallationPath().append("tools/make").toString()); //$NON-NLS-1$

    }

    public static String getUpdateJasonFilesKey() {
	return KEY_UPDATE_JASONS;
    }

    public static boolean getUpdateJasonFilesValue() {
	return getBoolean(KEY_UPDATE_JASONS, false);
    }

    private static String systemHash = null;

    /**
     * Make a unique hashKey based on system parameters so we can identify users
     * To make the key the mac adresses of the network cards are used
     * 
     * @return a unique key identyfying the system
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
