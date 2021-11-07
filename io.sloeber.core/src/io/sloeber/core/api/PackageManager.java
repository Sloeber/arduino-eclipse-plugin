package io.sloeber.core.api;

import static io.sloeber.core.Messages.*;
import static io.sloeber.core.common.ConfigurationPreferences.*;
import static io.sloeber.core.common.Const.*;
import static java.nio.file.StandardCopyOption.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SystemUtils;
import org.eclipse.cdt.core.parser.util.StringUtil;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.google.gson.Gson;

import io.sloeber.core.Activator;
import io.sloeber.core.Messages;
import io.sloeber.core.api.Json.packages.ArduinoPackage;
import io.sloeber.core.api.Json.packages.ArduinoPlatform;
import io.sloeber.core.api.Json.packages.PackageIndex;
import io.sloeber.core.api.Json.packages.ToolDependency;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.Const;
import io.sloeber.core.common.InstancePreferences;
import io.sloeber.core.managers.InstallProgress;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.MyMultiStatus;
import io.sloeber.core.txt.BoardTxtFile;
import io.sloeber.core.txt.WorkAround;

/**
 * This class groups both boards installed by the hardware manager and boards
 * installed locally.
 *
 * @author jantje
 *
 */
public class PackageManager {
    private static String stringSplitter = "\n";//$NON-NLS-1$
    private static final String KEY_MANAGER_JSON_URLS_V3 = "Arduino Manager board Urls"; //$NON-NLS-1$
    private static final String KEY_MANAGER_ARDUINO_LIBRARY_JSON_URL = "https://downloads.arduino.cc/libraries/library_index.json"; //$NON-NLS-1$
    private static final String KEY_MANAGER_JSON_URLS = "Manager jsons"; //$NON-NLS-1$
    private static final String DEFAULT_JSON_URLS = "https://downloads.arduino.cc/packages/package_index.json\n" //$NON-NLS-1$
            + "https://raw.githubusercontent.com/jantje/hardware/master/package_jantje_index.json\n" //$NON-NLS-1$
            + "https://raw.githubusercontent.com/jantje/ArduinoLibraries/master/library_jantje_index.json\n" //$NON-NLS-1$
            + "https://arduino.esp8266.com/stable/package_esp8266com_index.json\n" //$NON-NLS-1$
            + KEY_MANAGER_ARDUINO_LIBRARY_JSON_URL;

    protected static List<PackageIndex> packageIndices;
    private static boolean myHasbeenLogged = false;
    private static boolean envVarsNeedUpdating = true;// reset global variables at startup
    private final static int MAX_HTTP_REDIRECTIONS = 5;
    private static HashMap<String, String> myWorkbenchEnvironmentVariables = new HashMap<>();

    /**
     * Gets the board description based on the information provided. If
     * jsonFileName="local" the board is assumed not to be installed by the boards
     * manager. Otherwise the boardsmanager is queried to find the board descriptor.
     * In this case the latest installed board will be returned
     *
     * @param jsonFileName
     *            equals to "local" or the name of the json file used by the boards
     *            manager to install the boards
     * @param packageName
     *            if jsonFileName equals "local" the filename of the boards.txt
     *            containing the boards. otherwise the name of the package
     *            containing the board
     * @param architectureName
     *            ignored if jsonFileName equals "local" otherwise the architecture
     *            name of the platform containing the board (this assumes the
     *            architecture is the unique id for the platform)
     * @param boardID
     *            the id of the board in the boards.txt file
     * @param options
     *            the options to specify the board (the menu named on the boards.txt
     *            file) or null for defaults
     * @return The class BoardDescriptor or null
     */
    static public BoardDescription getBoardDescription(String jsonFileName, String packageName, String architectureName,
            String boardID, Map<String, String> options) {
        if (LOCAL.equals(jsonFileName)) {
            return new BoardDescription(new File(packageName), boardID, options);
        }
        return getNewestBoardIDFromBoardsManager(jsonFileName, packageName, architectureName, boardID, options);
    }

    static private BoardDescription getNewestBoardIDFromBoardsManager(String jsonFileName, String packageName,
            String architectureName, String boardID, Map<String, String> options) {

        ArduinoPackage thePackage = getPackage(jsonFileName, packageName);
        if (thePackage == null) {
            // fail("failed to find package:" + this.mPackageName);
            return null;
        }
        ArduinoPlatform platform = thePackage.getLatestPlatform(architectureName, true);
        if (platform == null) {
            // fail("failed to find platform " + this.mPlatform + " in
            // package:" + this.mPackageName);
            return null;
        }
        java.io.File boardsFile = platform.getBoardsFile();
        BoardDescription boardid = new BoardDescription(boardsFile, boardID, options);

        return boardid;
    }

    public static void addPackageURLs(HashSet<String> packageUrlsToAdd, boolean forceDownload) {
        HashSet<String> originalJsonUrls = new HashSet<>(Arrays.asList(getJsonURLList()));
        packageUrlsToAdd.addAll(originalJsonUrls);

        setJsonURLs(packageUrlsToAdd);
        loadJsons(forceDownload);
    }

    public static void setPackageURLs(HashSet<String> packageUrls, boolean forceDownload) {
        if (!isReady()) {
            Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, BoardsManagerIsBussy, new Exception()));
            return;
        }
        setJsonURLs(packageUrls);
        loadJsons(forceDownload);
    }

    /**
     * installs a subset of the latest platforms It skips the first <fromIndex>
     * platforms And stops at <toIndex> platforms. To install the 5 first latest
     * platforms installsubsetOfLatestPlatforms(0,5)
     * 
     * @param fromIndex
     *            the platforms at the start to skip
     * @param toIndex
     *            the platforms after this platform are skipped
     */
    public static void installsubsetOfLatestPlatforms(int fromIndex, int toIndex) {
        if (!isReady()) {
            Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, BoardsManagerIsBussy, new Exception()));
            return;
        }
        envVarsNeedUpdating = true;
        int currPlatformIndex = 1;
        NullProgressMonitor monitor = new NullProgressMonitor();
        List<ArduinoPackage> allPackages = getPackages();
        for (ArduinoPackage curPackage : allPackages) {
            Collection<ArduinoPlatform> latestPlatforms = curPackage.getLatestPlatforms();
            for (ArduinoPlatform curPlatform : latestPlatforms) {
                if (currPlatformIndex > fromIndex) {
                    curPlatform.install(monitor);
                }
                if (currPlatformIndex++ > toIndex)
                    return;
            }
        }
    }

    /**
     * Install all the latest platforms Assumes there are less than 100000 platforms
     */
    public static void installAllLatestPlatforms() {
        installsubsetOfLatestPlatforms(0, 100000);
    }

    public static void installLatestPlatform(String JasonName, String packageName, String architectureName) {
        if (!isReady()) {
            Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, BoardsManagerIsBussy, new Exception()));
            return;
        }
        envVarsNeedUpdating = true;
        ArduinoPackage curPackage = getPackage(JasonName, packageName);
        if (curPackage != null) {
            ArduinoPlatform curPlatform = curPackage.getLatestPlatform(architectureName, false);
            if (curPlatform != null) {
                NullProgressMonitor monitor = new NullProgressMonitor();
                curPlatform.install(monitor);
                return;
            }
        }
        Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                "failed to find " + JasonName + " " + packageName + " " + architectureName)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    public static void addPrivateHardwarePath(String newHardwarePath) {
        if (newHardwarePath == null) {
            return;
        }
        String currentPaths[] = InstancePreferences.getPrivateHardwarePaths();
        String newPaths[] = new String[currentPaths.length + 1];
        for (int i = 0; i < currentPaths.length; i++) {
            if (currentPaths[i].equals(newHardwarePath)) {
                return;
            }
            newPaths[i] = currentPaths[i];
        }
        newPaths[currentPaths.length] = newHardwarePath;
        InstancePreferences.setPrivateHardwarePaths(newPaths);
    }

    public static String[] getBoardNames(String boardFile) {
        BoardTxtFile theBoardsFile = new BoardTxtFile(new File(boardFile));
        return theBoardsFile.getAllSectionNames();
    }

    /**
     * Searches for all boards.txt files from the hardware folders and the boards
     * manager
     *
     * @return all the boards.txt files with full path and in a case insensitive
     *         order
     */
    public static File[] getAllBoardsFiles() {
        String hardwareFolders[] = getHardwarePaths();

        TreeSet<File> boardFiles = new TreeSet<>();
        for (String CurFolder : hardwareFolders) {
            searchFiles(new File(CurFolder), boardFiles, Const.BOARDS_FILE_NAME, 6);
        }
        if (boardFiles.size() == 0) {
            Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                    Helpers_No_boards_txt_found.replace(FILE_TAG, String.join("\n", hardwareFolders)), null)); //$NON-NLS-1$
            return null;
        }
        return boardFiles.toArray(new File[boardFiles.size()]);
    }

    private static void searchFiles(File folder, TreeSet<File> Hardwarelists, String Filename, int depth) {
        if (depth > 0) {
            File[] a = folder.listFiles();
            if (a == null) {
                if (!myHasbeenLogged) {
                    Common.log(new Status(IStatus.INFO, Const.CORE_PLUGIN_ID,
                            Helpers_Error_The_folder_is_empty.replace(FOLDER_TAG, folder.toString()), null));
                    myHasbeenLogged = true;
                }
                return;
            }
            for (File f : a) {
                if (f.isDirectory()) {
                    searchFiles(f, Hardwarelists, Filename, depth - 1);
                } else if (f.getName().equals(Filename)) {
                    Hardwarelists.add(f);
                }
            }
        }
    }

    /**
     * Gets all the folders that can contain hardware
     *
     * @return a list of all the folder locations that can contain hardware
     */
    private static String[] getHardwarePaths() {
        return (InstancePreferences.getPrivateHardwarePathsString() + File.pathSeparator
                + ConfigurationPreferences.getInstallationPathPackages()).split(File.pathSeparator);
    }

    public static IStatus installRemovePlatforms(List<ArduinoPlatform> platformsToInstall,
            List<ArduinoPlatform> platformsToRemove, IProgressMonitor monitor, MultiStatus status) {
        if (!isReady()) {
            status.add(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, BoardsManagerIsBussy, null));
            return status;
        }
        //TODO updating the jsons after selecting what to install seems dangerous to me; check to delete
        if (!ConfigurationPreferences.getUpdateJasonFilesFlag()) {
            loadJsons(true);
        }
        envVarsNeedUpdating = true;
        try {
            myIsReady = false;
            for (ArduinoPlatform curPlatform : platformsToRemove) {
                status.add(curPlatform.remove(monitor));
            }
            for (ArduinoPlatform curPlatform : platformsToInstall) {
                status.add(curPlatform.install(monitor));
            }

        } catch (Exception e) {
            // do nothing
        }
        myIsReady = true;
        SloeberProject.reloadTxtFile();
        return status;
    }

    public static TreeMap<String, String> getAllmenus() {
        TreeMap<String, String> ret = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        File[] boardFiles = getAllBoardsFiles();
        for (File curBoardFile : boardFiles) {
            BoardTxtFile txtFile = new BoardTxtFile(curBoardFile);
            ret.putAll(txtFile.getMenus());
        }
        return ret;
    }

    public static void setPrivateHardwarePaths(String[] hardWarePaths) {
        if (!isReady()) {
            Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, BoardsManagerIsBussy, new Exception()));
            return;
        }
        InstancePreferences.setPrivateHardwarePaths(hardWarePaths);
    }

    public static String getPrivateHardwarePathsString() {
        return InstancePreferences.getPrivateHardwarePathsString();
    }

    protected static synchronized void loadJsons(boolean forceDownload) {
        packageIndices = new ArrayList<>();
        LibraryManager.flushIndices();

        String[] jsonUrls = getJsonURLList();
        for (String jsonUrl : jsonUrls) {
            if (!jsonUrl.trim().isEmpty()) // skip empty lines
                loadJson(jsonUrl, forceDownload);
        }
    }

    /**
     * This method takes a json boards file url and downloads it and parses it for
     * usage in the boards manager
     *
     * @param url
     *            the url of the file to download and load
     * @param forceDownload
     *            set true if you want to download the file even if it is already
     *            available locally
     */
    static private void loadJson(String url, boolean forceDownload) {
        File jsonFile = getLocalFileName(url, true);
        if (jsonFile == null) {
            return;
        }
        if (!jsonFile.exists() || forceDownload) {
            jsonFile.getParentFile().mkdirs();
            try {
                mySafeCopy(new URL(url.trim()), jsonFile, false);
            } catch (IOException e) {
                Common.log(new Status(IStatus.ERROR, Activator.getId(), "Unable to download " + url, e)); //$NON-NLS-1$
            }
        }
        if (jsonFile.exists()) {
            if (jsonFile.getName().toLowerCase().startsWith("package_")) { //$NON-NLS-1$
                loadPackage(jsonFile);
            } else if (jsonFile.getName().toLowerCase().startsWith("library_")) { //$NON-NLS-1$
                LibraryManager.loadJson(jsonFile);
            } else {
                Common.log(new Status(IStatus.ERROR, Activator.getId(),
                        "json files should start with \"package_\" or \"library_\" " + url + " is ignored")); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    static private void loadPackage(File jsonFile) {
        try (Reader reader = new FileReader(jsonFile)) {
            PackageIndex index = new Gson().fromJson(reader, PackageIndex.class);
            index.setPackageFile(jsonFile);
            packageIndices.add(index);
        } catch (Exception e) {
            Common.log(new Status(IStatus.ERROR, Activator.getId(),
                    Manager_Failed_to_parse.replace(FILE_TAG, jsonFile.getAbsolutePath()), e));
            jsonFile.delete();// Delete the file so it stops damaging
        }
    }

    /**
     * convert a web url to a local file name. The local file name is the cache of
     * the web
     *
     * @param url
     *            url of the file we want a local cache
     * @return the file that represents the file that is the local cache. the file
     *         itself may not exists. If the url is malformed return null;
     * @throws MalformedURLException
     */
    protected static File getLocalFileName(String url, boolean show_error) {
        URL packageUrl;
        try {
            packageUrl = new URL(url.trim());
        } catch (MalformedURLException e) {
            if (show_error) {
                Common.log(new Status(IStatus.ERROR, Activator.getId(), "Malformed url " + url, e)); //$NON-NLS-1$
            }
            return null;
        }
        if ("file".equals(packageUrl.getProtocol())) { //$NON-NLS-1$
            String tst = packageUrl.getFile();
            File file = new File(tst);
            String localFileName = file.getName();
            java.nio.file.Path packagePath = Paths
                    .get(ConfigurationPreferences.getInstallationPath().append(localFileName).toString());
            return packagePath.toFile();
        }
        String localFileName = Paths.get(packageUrl.getPath()).getFileName().toString();
        java.nio.file.Path packagePath = Paths
                .get(ConfigurationPreferences.getInstallationPath().append(localFileName).toString());
        return packagePath.toFile();
    }

    /**
     * copy a url locally taking into account redirections
     *
     * @param url
     * @param localFile
     * @throws IOException
     */
    protected static void myCopy(URL url, File localFile, boolean report_error) throws IOException {
        myCopy(url, localFile, report_error, 0);
    }

    @SuppressWarnings("nls")
    private static void myCopy(URL url, File localFile, boolean report_error, int redirectionCounter)
            throws IOException {
        if ("file".equals(url.getProtocol())) {
            FileUtils.copyFile(new File(url.getFile()), localFile);
            return;
        }
        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(30000);
            conn.addRequestProperty("Accept-Language", "en-US,en;q=0.8");
            conn.addRequestProperty("User-Agent", "Mozilla");
            conn.addRequestProperty("Referer", "google.com");

            // normally, 3xx is redirect
            int status = conn.getResponseCode();

            if (status == HttpURLConnection.HTTP_OK) {
                try (InputStream stream = url.openStream()) {
                    Files.copy(stream, localFile.toPath(), REPLACE_EXISTING);
                }
                return;
            }

            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER) {
                if (redirectionCounter >= MAX_HTTP_REDIRECTIONS) {
                    throw new IOException("Too many redirections while downloading file.");
                }
                myCopy(new URL(conn.getHeaderField("Location")), localFile, report_error, redirectionCounter + 1);
                return;
            }
            if (report_error) {
                Common.log(new Status(IStatus.WARNING, Activator.getId(),
                        "Failed to download url " + url + " error code is: " + status, null));
            }
            throw new IOException("Failed to download url " + url + " error code is: " + status);

        } catch (Exception e) {
            if (report_error) {
                Common.log(new Status(IStatus.WARNING, Activator.getId(), "Failed to download url " + url, e));
            }
            throw e;

        }
    }

    /**
     * copy a url locally taking into account redirections in such a way that if
     * there is already a file it does not get lost if the download fails
     *
     * @param url
     * @param localFile
     * @throws IOException
     */
    protected static void mySafeCopy(URL url, File localFile, boolean report_error) throws IOException {
        File savedFile = null;
        if (localFile.exists()) {
            savedFile = File.createTempFile(localFile.getName(), "Sloeber"); //$NON-NLS-1$
            Files.move(localFile.toPath(), savedFile.toPath(), REPLACE_EXISTING);
        }
        try {
            myCopy(url, localFile, report_error);
        } catch (Exception e) {
            if (null != savedFile) {
                Files.move(savedFile.toPath(), localFile.toPath(), REPLACE_EXISTING);
            }
            throw e;
        }
    }

    public static String getDefaultJsonURLs() {
        return DEFAULT_JSON_URLS;
    }

    public static String getJsonUrlsKey() {
        return KEY_MANAGER_JSON_URLS;
    }

    public static void setJsonURLs(String urls) {
        setString(KEY_MANAGER_JSON_URLS, urls);
    }

    private static void saveJsonURLs(String urls[]) {
        setString(KEY_MANAGER_JSON_URLS, StringUtil.join(urls, stringSplitter));
    }

    public static void setJsonURLs(HashSet<String> urls) {
        setString(KEY_MANAGER_JSON_URLS, StringUtil.join(urls, stringSplitter));
    }

    public static String[] getJsonURLList() {
        return getJsonURLs().replace("\r", new String()).split(stringSplitter); //$NON-NLS-1$
    }

    public static String getJsonURLs() {
        // I added some code here to get easier from V3 to V4
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

    /**
     * Completely replace the list with jsons with a new list
     *
     * @param newJsonUrls
     */
    public static void setJsonURLs(String[] newJsonUrls) {
        if (!isReady()) {
            Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, BoardsManagerIsBussy, new Exception()));
            return;
        }

        String curJsons[] = getJsonURLList();
        HashSet<String> origJsons = new HashSet<>(Arrays.asList(curJsons));
        HashSet<String> currentSelectedJsons = new HashSet<>(Arrays.asList(newJsonUrls));
        origJsons.removeAll(currentSelectedJsons);
        // remove the files from disk which were in the old lst but not in the
        // new one
        for (String curJson : origJsons) {
            try {
                File localFile = getLocalFileName(curJson, false);
                if (localFile.exists()) {
                    localFile.delete();
                }
            } catch (Exception e) {
                // ignore
            }
        }
        // save to configurationsettings before calling LoadIndices
        saveJsonURLs(newJsonUrls);
        // reload the indices (this will remove all potential remaining
        // references
        // existing files do not need to be refreshed as they have been
        // refreshed at startup
        // new files will be added
        loadJsons(false);
    }

    public static void removeAllInstalledPlatforms() {
        if (!isReady()) {
            Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, BoardsManagerIsBussy, new Exception()));
            return;
        }
        try {
            FileUtils.deleteDirectory(ConfigurationPreferences.getInstallationPathPackages().toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static IPath getInstallationPath() {
        return ConfigurationPreferences.getInstallationPath();
    }

    /**
     * If something has been installed or deinstalled update the global variables
     * with references to the installed stuff this to support platforms that do not
     * explicitly define tools or platform dependencies Like private hardware
     */
    public static Map<String, String> getEnvironmentVariables() {
        if (!envVarsNeedUpdating) {
            return myWorkbenchEnvironmentVariables;
        }
        myWorkbenchEnvironmentVariables.clear();
        ArduinoPlatform latestAvrPlatform = null;
        ArduinoPlatform latestSamdPlatform = null;
        ArduinoPlatform latestSamPlatform = null;
        for (ArduinoPlatform curPlatform : getInstalledPlatforms()) {
            ArduinoPackage pkg = curPlatform.getParent();
            if (pkg != null) {
                myWorkbenchEnvironmentVariables.putAll(Helpers.getEnvVarPlatformFileTools(curPlatform, false));
                if (Const.ARDUINO.equalsIgnoreCase(pkg.getMaintainer())) {
                    switch (curPlatform.getArchitecture()) {
                    case Const.AVR:
                        latestAvrPlatform = curPlatform;
                        break;
                    case Const.SAM:
                        latestSamPlatform = curPlatform;
                        break;
                    case Const.SAMD:
                        latestSamdPlatform = curPlatform;
                        break;
                    }
                }
            }
        }

        if (latestSamdPlatform != null) {
            myWorkbenchEnvironmentVariables.putAll(Helpers.getEnvVarPlatformFileTools(latestSamdPlatform, false));
        }
        if (latestSamPlatform != null) {
            myWorkbenchEnvironmentVariables.putAll(Helpers.getEnvVarPlatformFileTools(latestSamPlatform, false));
        }
        if (latestAvrPlatform != null) {
            myWorkbenchEnvironmentVariables.putAll(Helpers.getEnvVarPlatformFileTools(latestAvrPlatform, false));
        }
        envVarsNeedUpdating = false;
        return myWorkbenchEnvironmentVariables;
    }

    /**
     * Only the latest versions of the platforms.
     *
     * @return latest platforms
     */
    public static Collection<ArduinoPlatform> getLatestPlatforms() {
        Collection<ArduinoPlatform> allLatestPlatforms = new LinkedList<>();
        List<ArduinoPackage> allPackages = getPackages();
        for (ArduinoPackage curPackage : allPackages) {
            allLatestPlatforms.addAll(curPackage.getLatestPlatforms());
        }
        return allLatestPlatforms;
    }

    //Below is what used to be the internal package manager class
    private static final String FILE = Messages.FILE_TAG;
    private static final String FOLDER = Messages.FOLDER_TAG;
    private static boolean myIsReady = false;

    public static boolean isReady() {
        return myIsReady;
    }

    /**
     * Loads all stuff needed and if this is the first time downloads the avr boards
     * and needed tools
     *
     * @param monitor
     */
    public static synchronized void startup_Pluging(IProgressMonitor monitor) {
        loadJsons(ConfigurationPreferences.getUpdateJasonFilesFlag());

        if (!LibraryManager.libsAreInstalled()) {
            LibraryManager.InstallDefaultLibraries(monitor);
        }
        IPath examplesPath = ConfigurationPreferences.getInstallationPathExamples();
        if (!examplesPath.toFile().exists()) {// examples are not installed
            // Download arduino IDE example programs
            Common.log(
                    downloadAndInstall(Defaults.EXAMPLES_URL, Defaults.EXAMPLE_PACKAGE, examplesPath, false, monitor));
        }
        if (!areThereInstalledBoards()) {

            MyMultiStatus mstatus = new MyMultiStatus("Failed to configer Sloeber"); //$NON-NLS-1$

            if (mstatus.isOK()) {
                // if successfully installed the examples: add the boards
                ArduinoPlatform platform = null;
                Collection<ArduinoPlatform> latestsPlatforms = getLatestPlatforms();
                if (!latestsPlatforms.isEmpty()) {
                    platform = latestsPlatforms.iterator().next();
                    for (ArduinoPlatform curPlatform : latestsPlatforms) {

                        if (Defaults.DEFAULT_INSTALL_PLATFORM_NAME.equalsIgnoreCase(curPlatform.getName())
                                && Defaults.DEFAULT_INSTALL_ARCHITECTURE.equalsIgnoreCase(curPlatform.getArchitecture())
                                && Defaults.DEFAULT_INSTALL_MAINTAINER
                                        .equalsIgnoreCase(curPlatform.getParent().getMaintainer())) {
                            platform = curPlatform;
                        }
                    }
                }
                if (platform == null) {
                    mstatus.add(new Status(IStatus.ERROR, Activator.getId(), Messages.No_Platform_available));
                } else {
                    mstatus.addErrors(downloadAndInstall(platform, false, monitor));
                }

            }
            if (!mstatus.isOK()) {
                StatusManager stMan = StatusManager.getManager();
                stMan.handle(mstatus, StatusManager.LOG | StatusManager.SHOW | StatusManager.BLOCK);
            }

        }
        myIsReady = true;

    }

    /**
     * Given a platform description in a json file download and install all needed
     * stuff. All stuff is including all tools and core files and hardware specific
     * libraries. That is (on windows) inclusive the make.exe
     *
     * @param platform
     * @param monitor
     * @param object
     * @return
     */
    static public synchronized IStatus downloadAndInstall(ArduinoPlatform platform, boolean forceDownload,
            IProgressMonitor monitor) {

        MyMultiStatus mstatus = new MyMultiStatus("Failed to install " + platform.getName()); //$NON-NLS-1$
        mstatus.addErrors(downloadAndInstall(platform.getUrl(), platform.getArchiveFileName(),
                platform.getInstallPath(), forceDownload, monitor));
        if (!mstatus.isOK()) {
            // no use going on installing tools if the boards failed installing
            return mstatus;
        }
        File packageFile = platform.getParent().getParent().getJsonFile();
        File copyToFile = platform.getInstallPath().append(packageFile.getName()).toFile();
        try {
            Files.copy(packageFile.toPath(), copyToFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (platform.getToolsDependencies() != null) {
            for (ToolDependency toolDependency : platform.getToolsDependencies()) {
                monitor.setTaskName(InstallProgress.getRandomMessage());
                mstatus.addErrors(toolDependency.install(monitor));
            }
        }

        WorkAround.applyKnownWorkArounds(platform);
        return mstatus;

    }

    synchronized static public List<PackageIndex> getPackageIndices() {
        if (packageIndices == null) {
            loadJsons(false);
        }
        return packageIndices;
    }

    public static List<ArduinoPlatform> getPlatforms() {
        List<ArduinoPlatform> platforms = new ArrayList<>();
        for (PackageIndex index : getPackageIndices()) {
            for (ArduinoPackage pkg : index.getPackages()) {
                platforms.addAll(pkg.getPlatforms());
            }
        }
        return platforms;
    }

    public static IPath getPlatformInstallPath(String vendor, String architecture) {

        for (PackageIndex index : getPackageIndices()) {
            for (ArduinoPackage pkg : index.getPackages()) {
                for (ArduinoPlatform curPlatform : pkg.getLatestInstalledPlatforms()) {
                    if (architecture.equalsIgnoreCase(curPlatform.getArchitecture())
                            && (vendor.equalsIgnoreCase(pkg.getName()))) {
                        return new org.eclipse.core.runtime.Path(curPlatform.getInstallPath().toString());
                    }
                }
            }
        }
        return null;
    }

    public static IPath getPlatformInstallPath(String refVendor, String refArchitecture, VersionNumber refVersion) {
        for (PackageIndex index : getPackageIndices()) {
            for (ArduinoPackage pkg : index.getPackages()) {
                if (refVendor.equalsIgnoreCase(pkg.getName())) {
                    for (ArduinoPlatform curPlatform : pkg.getInstalledPlatforms()) {
                        if (refArchitecture.equalsIgnoreCase(curPlatform.getArchitecture())
                                && refVersion.compareTo(curPlatform.getVersion()) == 0) {
                            return new org.eclipse.core.runtime.Path(curPlatform.getInstallPath().toString());
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Given a platform.txt file find the platform in the platform manager
     *
     * @param platformTxt
     * @return the found platform otherwise null
     */
    public static ArduinoPlatform getPlatform(IPath platformPath) {
        for (PackageIndex index : getPackageIndices()) {
            for (ArduinoPackage pkg : index.getPackages()) {
                for (ArduinoPlatform curPlatform : pkg.getPlatforms()) {
                    if (curPlatform.getInstallPath().equals(platformPath)) {
                        return curPlatform;
                    }
                }
            }
        }
        return null;
    }

    static public List<ArduinoPlatform> getLatestInstalledPlatforms() {
        List<ArduinoPlatform> platforms = new ArrayList<>();
        for (PackageIndex index : getPackageIndices()) {
            for (ArduinoPackage pkg : index.getPackages()) {

                platforms.addAll(pkg.getLatestInstalledPlatforms());

            }
        }
        return platforms;
    }

    static public List<ArduinoPlatform> getInstalledPlatforms() {
        List<ArduinoPlatform> platforms = new ArrayList<>();
        for (PackageIndex index : getPackageIndices()) {
            for (ArduinoPackage pkg : index.getPackages()) {

                platforms.addAll(pkg.getInstalledPlatforms());

            }
        }
        return platforms;
    }

    static public boolean areThereInstalledBoards() {
        for (PackageIndex index : getPackageIndices()) {
            for (ArduinoPackage pkg : index.getPackages()) {
                if (pkg.isInstalled()) {
                    return true;
                }
            }
        }
        return false;
    }

    static public List<ArduinoPackage> getPackages() {
        List<ArduinoPackage> packages = new ArrayList<>();
        for (PackageIndex index : getPackageIndices()) {
            packages.addAll(index.getPackages());
        }
        return packages;
    }

    static public ArduinoPackage getPackage(String JasonName, String packageName) {
        for (PackageIndex index : getPackageIndices()) {
            if (index.getJsonFile().getName().equals(JasonName)) {
                return index.getPackage(packageName);
            }
        }
        return null;
    }

    static public ArduinoPackage getPackage(String packageName) {
        for (PackageIndex index : getPackageIndices()) {
            ArduinoPackage pkg = index.getPackage(packageName);
            if (pkg != null) {
                return pkg;
            }
        }
        return null;
    }

    /**
     * downloads an archive file from the internet and saves it in the download
     * folder under the name "pArchiveFileName" then extrats the file to
     * pInstallPath if pForceDownload is true the file will be downloaded even if
     * the download file already exists if pForceDownload is false the file will
     * only be downloaded if the download file does not exists The extraction is
     * done with processArchive so only files types supported by this method will be
     * properly extracted
     *
     * @param pURL
     *            the url of the file to download
     * @param pArchiveFileName
     *            the name of the file in the download folder
     * @param pInstallPath
     * @param pForceDownload
     * @param pMonitor
     * @return
     */
    public static IStatus downloadAndInstall(String pURL, String pArchiveFileName, IPath pInstallPath,
            boolean pForceDownload, IProgressMonitor pMonitor) {
        IPath dlDir = ConfigurationPreferences.getInstallationPathDownload();
        IPath archivePath = dlDir.append(pArchiveFileName);
        try {
            URL dl = new URL(pURL);
            dlDir.toFile().mkdir();
            if (!archivePath.toFile().exists() || pForceDownload) {
                pMonitor.subTask("Downloading " + pArchiveFileName + " .."); //$NON-NLS-1$ //$NON-NLS-2$
                myCopy(dl, archivePath.toFile(), true);
            }
        } catch (IOException e) {
            return new Status(IStatus.ERROR, Activator.getId(), Messages.Manager_Failed_to_download.replace(FILE, pURL),
                    e);
        }
        return processArchive(pArchiveFileName, pInstallPath, pForceDownload, archivePath.toString(), pMonitor);
    }

    private static IStatus processArchive(String pArchiveFileName, IPath pInstallPath, boolean pForceDownload,
            String pArchiveFullFileName, IProgressMonitor pMonitor) {
        // Create an ArchiveInputStream with the correct archiving algorithm
        String faileToExtractMessage = Messages.Manager_Failed_to_extract.replace(FILE, pArchiveFullFileName);
        if (pArchiveFileName.endsWith("tar.bz2")) { //$NON-NLS-1$
            try (ArchiveInputStream inStream = new TarArchiveInputStream(
                    new BZip2CompressorInputStream(new FileInputStream(pArchiveFullFileName)))) {
                return extract(inStream, pInstallPath.toFile(), 1, pForceDownload, pMonitor);
            } catch (IOException | InterruptedException e) {
                return new Status(IStatus.ERROR, Activator.getId(), faileToExtractMessage, e);
            }
        } else if (pArchiveFileName.endsWith("zip")) { //$NON-NLS-1$
            try (ArchiveInputStream in = new ZipArchiveInputStream(new FileInputStream(pArchiveFullFileName))) {
                return extract(in, pInstallPath.toFile(), 1, pForceDownload, pMonitor);
            } catch (IOException | InterruptedException e) {
                return new Status(IStatus.ERROR, Activator.getId(), faileToExtractMessage, e);
            }
        } else if (pArchiveFileName.endsWith("tar.gz")) { //$NON-NLS-1$
            try (ArchiveInputStream in = new TarArchiveInputStream(
                    new GzipCompressorInputStream(new FileInputStream(pArchiveFullFileName)))) {
                return extract(in, pInstallPath.toFile(), 1, pForceDownload, pMonitor);
            } catch (IOException | InterruptedException e) {
                return new Status(IStatus.ERROR, Activator.getId(), faileToExtractMessage, e);
            }
        } else if (pArchiveFileName.endsWith("tar")) { //$NON-NLS-1$
            try (ArchiveInputStream in = new TarArchiveInputStream(new FileInputStream(pArchiveFullFileName))) {
                return extract(in, pInstallPath.toFile(), 1, pForceDownload, pMonitor);
            } catch (IOException | InterruptedException e) {
                return new Status(IStatus.ERROR, Activator.getId(), faileToExtractMessage, e);
            }
        } else {
            return new Status(IStatus.ERROR, Activator.getId(), Messages.Manager_Format_not_supported);
        }
    }

    public static IStatus extract(ArchiveInputStream in, File destFolder, int stripPath, boolean overwrite,
            IProgressMonitor pMonitor) throws IOException, InterruptedException {

        // Folders timestamps must be set at the end of archive extraction
        // (because creating a file in a folder alters the folder's timestamp)
        Map<File, Long> foldersTimestamps = new HashMap<>();

        String pathPrefix = new String();

        Map<File, File> hardLinks = new HashMap<>();
        Map<File, Integer> hardLinksMode = new HashMap<>();
        Map<File, String> symLinks = new HashMap<>();
        Map<File, Long> symLinksModifiedTimes = new HashMap<>();

        // Cycle through all the archive entries
        while (true) {
            ArchiveEntry entry = in.getNextEntry();
            if (entry == null) {
                break;
            }

            // Extract entry info
            long size = entry.getSize();
            String name = entry.getName();
            boolean isDirectory = entry.isDirectory();
            boolean isLink = false;
            boolean isSymLink = false;
            String linkName = null;
            Integer mode = null;
            Long modifiedTime = Long.valueOf(entry.getLastModifiedDate().getTime());

            pMonitor.subTask("Processing " + name); //$NON-NLS-1$

            {
                // Skip MacOSX metadata
                // http://superuser.com/questions/61185/why-do-i-get-files-like-foo-in-my-tarball-on-os-x
                int slash = name.lastIndexOf('/');
                if (slash == -1) {
                    if (name.startsWith("._")) { //$NON-NLS-1$
                        continue;
                    }
                } else {
                    if (name.substring(slash + 1).startsWith("._")) { //$NON-NLS-1$
                        continue;
                    }
                }
            }

            // Skip git metadata
            // http://www.unix.com/unix-for-dummies-questions-and-answers/124958-file-pax_global_header-means-what.html
            if (name.contains("pax_global_header")) { //$NON-NLS-1$
                continue;
            }

            if (entry instanceof TarArchiveEntry) {
                TarArchiveEntry tarEntry = (TarArchiveEntry) entry;
                mode = Integer.valueOf(tarEntry.getMode());
                isLink = tarEntry.isLink();
                isSymLink = tarEntry.isSymbolicLink();
                linkName = tarEntry.getLinkName();
            }

            // On the first archive entry, if requested, detect the common path
            // prefix to be stripped from filenames
            int localstripPath = stripPath;
            if (localstripPath > 0 && pathPrefix.isEmpty()) {
                int slash = 0;
                while (localstripPath > 0) {
                    slash = name.indexOf("/", slash); //$NON-NLS-1$
                    if (slash == -1) {
                        throw new IOException(Messages.Manager_archiver_eror_single_root_folder_required);
                    }
                    slash++;
                    localstripPath--;
                }
                pathPrefix = name.substring(0, slash);
            }

            // Strip the common path prefix when requested
            if (!name.startsWith(pathPrefix)) {
                throw new IOException(Messages.Manager_archive_error_root_folder_name_mismatch.replace(FILE, name)
                        .replace(FOLDER, pathPrefix));

            }
            name = name.substring(pathPrefix.length());
            if (name.isEmpty()) {
                continue;
            }
            File outputFile = new File(destFolder, name);

            File outputLinkedFile = null;
            if (isLink && linkName != null) {
                if (!linkName.startsWith(pathPrefix)) {
                    throw new IOException(Messages.Manager_archive_error_root_folder_name_mismatch.replace(FILE, name)
                            .replace(FOLDER, pathPrefix));
                }
                linkName = linkName.substring(pathPrefix.length());
                outputLinkedFile = new File(destFolder, linkName);
            }
            if (isSymLink) {
                // Symbolic links are referenced with relative paths
                outputLinkedFile = new File(linkName);
                if (outputLinkedFile.isAbsolute()) {
                    System.err.println(Messages.Manager_archive_error_symbolic_link_to_absolute_path
                            .replace(FILE, outputFile.toString()).replace(FOLDER, outputLinkedFile.toString()));
                    System.err.println();
                }
            }

            // Safety check
            if (isDirectory) {
                if (outputFile.isFile() && !overwrite) {
                    throw new IOException(
                            Messages.Manager_Cant_create_folder_exists.replace(FILE, outputFile.getPath()));
                }
            } else {
                // - isLink
                // - isSymLink
                // - anything else
                if (outputFile.exists() && !overwrite) {
                    throw new IOException(Messages.Manager_Cant_extract_file_exist.replace(FILE, outputFile.getPath()));
                }
            }

            // Extract the entry
            if (isDirectory) {
                if (!outputFile.exists() && !outputFile.mkdirs()) {
                    throw new IOException(Messages.Manager_Cant_create_folder.replace(FILE, outputFile.getPath()));
                }
                foldersTimestamps.put(outputFile, modifiedTime);
            } else if (isLink) {
                hardLinks.put(outputFile, outputLinkedFile);
                hardLinksMode.put(outputFile, mode);
            } else if (isSymLink) {
                symLinks.put(outputFile, linkName);
                symLinksModifiedTimes.put(outputFile, modifiedTime);
            } else {
                // Create the containing folder if not exists
                if (!outputFile.getParentFile().isDirectory()) {
                    outputFile.getParentFile().mkdirs();
                }
                copyStreamToFile(in, size, outputFile);
                outputFile.setLastModified(modifiedTime.longValue());
            }

            // Set file/folder permission
            if (mode != null && !isSymLink && outputFile.exists()) {
                chmod(outputFile, mode.intValue());
            }
        }

        for (Map.Entry<File, File> entry : hardLinks.entrySet()) {
            if (entry.getKey().exists() && overwrite) {
                entry.getKey().delete();
            }
            link(entry.getValue(), entry.getKey());
            Integer mode = hardLinksMode.get(entry.getKey());
            if (mode != null) {
                chmod(entry.getKey(), mode.intValue());
            }
        }

        for (Map.Entry<File, String> entry : symLinks.entrySet()) {
            if (entry.getKey().exists() && overwrite) {
                entry.getKey().delete();
            }

            symlink(entry.getValue(), entry.getKey());
            entry.getKey().setLastModified(symLinksModifiedTimes.get(entry.getKey()).longValue());
        }

        // Set folders timestamps
        for (Map.Entry<File, Long> entry : foldersTimestamps.entrySet()) {
            entry.getKey().setLastModified(entry.getValue().longValue());
        }

        return Status.OK_STATUS;

    }

    private static void symlink(String from, File to) throws IOException, InterruptedException {
        if (Common.isWindows) {
            // needs special rights only one board seems to fail due to this
            // Process process = Runtime.getRuntime().exec(new String[] {
            // "mklink", from, to.getAbsolutePath() }, //$NON-NLS-1$
            // null, to.getParentFile());
            // process.waitFor();
        } else {
            Process process = Runtime.getRuntime().exec(new String[] { "ln", "-s", from, to.getAbsolutePath() }, //$NON-NLS-1$ //$NON-NLS-2$
                    null, to.getParentFile());
            process.waitFor();
        }

    }

    /*
     * create a link file at the level of the os using mklink /H on windows makes
     * that no admin rights are needed
     */
    @SuppressWarnings("nls")
    private static void link(File actualFile, File linkName) throws IOException, InterruptedException {
        String[] command = new String[] { "ln", actualFile.getAbsolutePath(), linkName.getAbsolutePath() };
        if (SystemUtils.IS_OS_WINDOWS) {
            command = new String[] { "cmd", "/c", "mklink", "/H", linkName.getAbsolutePath(),
                    actualFile.getAbsolutePath() };
        }
        Process process = Runtime.getRuntime().exec(command, null, null);
        process.waitFor();
    }

    private static void chmod(File file, int mode) throws IOException, InterruptedException {
        String octal = Integer.toOctalString(mode);
        if (Common.isWindows) {
            boolean ownerExecute = (((mode / (8 * 8)) & 1) == 1);
            boolean ownerRead = (((mode / (8 * 8)) & 4) == 4);
            boolean ownerWrite = (((mode / (8 * 8)) & 2) == 2);
            boolean everyoneExecute = (((mode / 8) & 1) == 1);
            boolean everyoneRead = (((mode / 8) & 4) == 4);
            boolean everyoneWrite = (((mode / 8) & 2) == 2);
            file.setWritable(true, false);
            file.setExecutable(ownerExecute, !everyoneExecute);
            file.setReadable(ownerRead, !everyoneRead);
            file.setWritable(ownerWrite, !everyoneWrite);
        } else {
            Process process = Runtime.getRuntime().exec(new String[] { "chmod", octal, file.getAbsolutePath() }, null, //$NON-NLS-1$
                    null);
            process.waitFor();
        }
    }

    private static void copyStreamToFile(InputStream in, long size, File outputFile) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {

            // if size is not available, copy until EOF...
            if (size == -1) {
                byte[] buffer = new byte[4096];
                int length;
                while ((length = in.read(buffer)) != -1) {
                    fos.write(buffer, 0, length);
                }
                return;
            }

            // ...else copy just the needed amount of bytes
            byte[] buffer = new byte[4096];
            long leftToWrite = size;
            while (leftToWrite > 0) {
                int length = in.read(buffer);
                if (length <= 0) {
                    throw new IOException(
                            Messages.Manager_Failed_to_extract.replace(FILE, outputFile.getAbsolutePath()));
                }
                fos.write(buffer, 0, length);
                leftToWrite -= length;
            }
        }
    }

    /**
     * This method removes the json files from disk and removes memory references to
     * these files or their content
     *
     * @param packageUrlsToRemove
     */
    public static void removePackageURLs(Set<String> packageUrlsToRemove) {
        if (!isReady()) {
            Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, BoardsManagerIsBussy, new Exception()));
            return;
        }
        // remove the files from memory
        Set<String> activeUrls = new HashSet<>(Arrays.asList(getJsonURLList()));

        activeUrls.removeAll(packageUrlsToRemove);

        setJsonURLs(activeUrls.toArray((String[]) null));

        // remove the files from disk
        for (String curJson : packageUrlsToRemove) {
            File localFile = getLocalFileName(curJson, true);
            if (localFile != null) {
                if (localFile.exists()) {
                    localFile.delete();
                }
            }
        }

        // reload the indices (this will remove all potential remaining
        // references
        // existing files do not need to be refreshed as they have been
        // refreshed at startup
        loadJsons(false);

    }

    /**
     * Remove all packages that have a more recent version
     */
    public static void onlyKeepLatestPlatforms() {
        if (!isReady()) {
            Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, BoardsManagerIsBussy, new Exception()));
            return;
        }
        List<ArduinoPackage> allPackages = getPackages();
        for (ArduinoPackage curPackage : allPackages) {
            curPackage.onlyKeepLatestPlatforms();
        }
    }

}
