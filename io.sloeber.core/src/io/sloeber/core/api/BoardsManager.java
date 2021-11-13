package io.sloeber.core.api;

import static io.sloeber.core.Messages.*;
import static io.sloeber.core.common.ConfigurationPreferences.*;
import static io.sloeber.core.common.Const.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.FileUtils;
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
import io.sloeber.core.api.Json.ArduinoInstallable;
import io.sloeber.core.api.Json.ArduinoPackage;
import io.sloeber.core.api.Json.ArduinoPlatform;
import io.sloeber.core.api.Json.ArduinoPlatformPackageIndex;
import io.sloeber.core.api.Json.ArduinoPlatformTool;
import io.sloeber.core.api.Json.ArduinoPlatformTooldDependency;
import io.sloeber.core.api.Json.ArduinoPlatformVersion;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.Const;
import io.sloeber.core.common.InstancePreferences;
import io.sloeber.core.managers.InstallProgress;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.MyMultiStatus;
import io.sloeber.core.tools.PackageManager;
import io.sloeber.core.txt.BoardTxtFile;
import io.sloeber.core.txt.WorkAround;

/**
 * This class groups both boards installed by the hardware manager and boards
 * installed locally.
 *
 * @author jantje
 *
 */
public class BoardsManager {
    private static String stringSplitter = "\n";//$NON-NLS-1$
    private static final String KEY_MANAGER_JSON_URLS_V3 = "Arduino Manager board Urls"; //$NON-NLS-1$
    private static final String KEY_MANAGER_ARDUINO_LIBRARY_JSON_URL = "https://downloads.arduino.cc/libraries/library_index.json"; //$NON-NLS-1$
    private static final String KEY_MANAGER_JSON_URLS = "Manager jsons"; //$NON-NLS-1$
    private static final String DEFAULT_JSON_URLS = "https://downloads.arduino.cc/packages/package_index.json\n" //$NON-NLS-1$
            + "https://raw.githubusercontent.com/jantje/hardware/master/package_jantje_index.json\n" //$NON-NLS-1$
            + "https://raw.githubusercontent.com/jantje/ArduinoLibraries/master/library_jantje_index.json\n" //$NON-NLS-1$
            + "https://arduino.esp8266.com/stable/package_esp8266com_index.json\n" //$NON-NLS-1$
            + KEY_MANAGER_ARDUINO_LIBRARY_JSON_URL;

    protected static List<ArduinoPlatformPackageIndex> packageIndices;
    private static boolean myHasbeenLogged = false;
    private static boolean envVarsNeedUpdating = true;// reset global variables at startup

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
        ArduinoPlatform platform = thePackage.getPlatform(architectureName);
        if (platform == null) {
            // fail("failed to find platform " + this.mPlatform + " in
            // package:" + this.mPackageName);
            return null;
        }
        ArduinoPlatformVersion platformVersion = platform.getNewestVersion();
        java.io.File boardsFile = platformVersion.getBoardsFile();
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
            Collection<ArduinoPlatform> latestPlatforms = curPackage.getPlatforms();
            for (ArduinoPlatform curPlatform : latestPlatforms) {
                if (currPlatformIndex > fromIndex) {
                    install(curPlatform.getNewestVersion(), monitor);
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
            ArduinoPlatform curPlatform = curPackage.getPlatform(architectureName);
            if (curPlatform != null) {
                ArduinoPlatformVersion curPlatformVersion = curPlatform.getNewestVersion();
                if (curPlatformVersion != null) {
                    NullProgressMonitor monitor = new NullProgressMonitor();
                    install(curPlatformVersion, monitor);
                    return;
                }
            }
        }
        Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                "failed to find " + JasonName + " " + packageName + " " + architectureName)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @SuppressWarnings("nls")
    private static IStatus install(ArduinoPlatformVersion platformVersion, IProgressMonitor monitor) {
        String name = platformVersion.getName();
        String architecture = platformVersion.getArchitecture();
        String version = platformVersion.getVersion().toString();
        // Check if we're installed already
        if (platformVersion.isInstalled()) {
            System.out.println("reusing platform " + name + " " + architecture + "(" + version + ")");
            return Status.OK_STATUS;
        }

        // Download platform archive
        System.out.println("start installing platform " + name + " " + architecture + "(" + version + ")");
        IStatus ret = BoardsManager.downloadAndInstall(platformVersion, false, monitor);
        System.out.println("done installing platform " + name + " " + architecture + "(" + version + ")");
        return ret;
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

    public static IStatus updatePlatforms(List<ArduinoPlatformVersion> platformsToInstall,
            List<ArduinoPlatformVersion> platformsToRemove, IProgressMonitor monitor, MultiStatus status) {
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
            for (ArduinoPlatformVersion curPlatform : platformsToRemove) {
                status.add(uninstall(curPlatform, monitor));
            }
            for (ArduinoPlatformVersion curPlatform : platformsToInstall) {
                status.add(install(curPlatform, monitor));
            }

        } catch (Exception e) {
            // do nothing
        }
        myIsReady = true;
        SloeberProject.reloadTxtFile();
        return status;
    }

    public static IStatus uninstall(ArduinoPlatformVersion curPlatform, IProgressMonitor monitor) {
        return curPlatform.remove(monitor);
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
                PackageManager.mySafeCopy(new URL(url.trim()), jsonFile, false);
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
            ArduinoPlatformPackageIndex index = new Gson().fromJson(reader, ArduinoPlatformPackageIndex.class);
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
        ArduinoPlatformVersion latestAvrPlatform = null;
        ArduinoPlatformVersion latestSamdPlatform = null;
        ArduinoPlatformVersion latestSamPlatform = null;
        for (ArduinoPlatformVersion curPlatform : getInstalledPlatforms()) {
            ArduinoPackage pkg = curPlatform.getParent().getParent();
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

    //Below is what used to be the internal package manager class

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
            Common.log(PackageManager.downloadAndInstall(Defaults.EXAMPLES_URL, Defaults.EXAMPLE_PACKAGE, examplesPath,
                    false, monitor));
        }
        if (!areThereInstalledBoards()) {

            IStatus status = null;

            // if successfully installed the examples: add the boards
            ArduinoPlatform platform = getPlatform(Defaults.DEFAULT_INSTALL_MAINTAINER,
                    Defaults.DEFAULT_INSTALL_ARCHITECTURE);
            //we failed to find arduino avr platform. Take the fiorst one
            if (platform == null) {
                try {
                    platform = getPlatforms().get(0);
                } catch (@SuppressWarnings("unused") Exception e) {
                    //no need to do anything
                }
            }
            if (platform == null) {
                status = new Status(IStatus.ERROR, Activator.getId(), Messages.No_Platform_available);
            } else {
                status = downloadAndInstall(platform.getNewestVersion(), false, monitor);
            }

            if (!status.isOK()) {
                StatusManager stMan = StatusManager.getManager();
                stMan.handle(status, StatusManager.LOG | StatusManager.SHOW | StatusManager.BLOCK);
            }

        }
        myIsReady = true;

    }

    private static IStatus downloadAndInstall(ArduinoPlatformVersion platformVersion, boolean forceDownload,
            IProgressMonitor monitor) {
        MyMultiStatus mstatus = new MyMultiStatus("Failed to install " + platformVersion.getName()); //$NON-NLS-1$
        mstatus.addErrors(PackageManager.downloadAndInstall(platformVersion, forceDownload, monitor));
        if (!mstatus.isOK()) {
            // no use going on installing tools if the boards failed installing
            return mstatus;
        }

        //keep a copy of the json file used at install
        File packageFile = platformVersion.getParent().getParent().getPackageIndex().getJsonFile();
        File copyToFile = platformVersion.getInstallPath().append(packageFile.getName()).toFile();
        try {
            Files.copy(packageFile.toPath(), copyToFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (platformVersion.getToolsDependencies() != null) {
            ArduinoPackage pkg = platformVersion.getParent().getParent();
            if (pkg == null) {
                return null;
            }
            for (ArduinoPlatformTooldDependency toolDependency : platformVersion.getToolsDependencies()) {
                ArduinoPlatformTool tool = pkg.getTool(toolDependency.getName(), toolDependency.getVersion());
                if (tool == null) {
                    mstatus.add(new Status(IStatus.ERROR, Activator.getId(),
                            Messages.Tool_no_valid_system.replace(Messages.KEY_TAG, toolDependency.getName())));
                } else if (!tool.isInstalled()) {
                    ArduinoInstallable installable = tool.getInstallable();
                    if (installable != null) {
                        monitor.setTaskName(InstallProgress.getRandomMessage());
                        mstatus.addErrors(PackageManager.downloadAndInstall(installable, forceDownload, monitor));
                    }
                }
            }
        }

        WorkAround.applyKnownWorkArounds(platformVersion);
        return mstatus;
    }

    synchronized static public List<ArduinoPlatformPackageIndex> getPackageIndices() {
        if (packageIndices == null) {
            loadJsons(false);
        }
        return packageIndices;
    }

    public static List<ArduinoPlatform> getPlatforms() {
        List<ArduinoPlatform> platforms = new ArrayList<>();
        for (ArduinoPlatformPackageIndex index : getPackageIndices()) {
            for (ArduinoPackage pkg : index.getPackages()) {
                platforms.addAll(pkg.getPlatforms());
            }
        }
        return platforms;
    }

    public static ArduinoPlatform getPlatform(String vendor, String architecture) {

        for (ArduinoPlatformPackageIndex index : getPackageIndices()) {
            ArduinoPackage pkg = index.getPackage(vendor);
            if (pkg != null) {
                ArduinoPlatform platform = pkg.getPlatform(architecture);
                if (platform != null) {
                    return platform;
                }
            }
        }
        return null;
    }

    public static IPath getPlatformInstallPath(String vendor, String architecture) {

        ArduinoPlatform platform = getPlatform(vendor, architecture);
        if (platform != null) {
            return new org.eclipse.core.runtime.Path(platform.getInstallPath().toString());
        }
        return null;
    }

    public static IPath getPlatformInstallPath(String refVendor, String refArchitecture, VersionNumber refVersion) {
        ArduinoPlatformVersion platformVersion = BoardsManager.getPlatform(refVendor, refArchitecture, refVersion);
        if (platformVersion != null) {
            return platformVersion.getInstallPath();
        }
        return null;
    }

    /**
     * Given a platform.txt file find the platform in the platform manager
     *
     * @param platformTxt
     * @return the found platform otherwise null
     */
    public static ArduinoPlatformVersion getPlatform(IPath platformPath) {
        for (ArduinoPlatformPackageIndex index : getPackageIndices()) {
            for (ArduinoPackage pkg : index.getPackages()) {
                for (ArduinoPlatform curPlatform : pkg.getPlatforms()) {
                    if (platformPath
                            .matchingFirstSegments(curPlatform.getInstallPath()) > (platformPath.segmentCount() - 2))

                        for (ArduinoPlatformVersion curPlatformVersion : curPlatform.getVersions()) {
                            if (curPlatformVersion.getInstallPath().equals(platformPath)) {
                                return curPlatformVersion;
                            }
                        }
                }
            }
        }
        return null;
    }

    static public List<ArduinoPlatformVersion> getInstalledPlatforms() {
        List<ArduinoPlatformVersion> platforms = new ArrayList<>();
        for (ArduinoPlatformPackageIndex index : getPackageIndices()) {
            for (ArduinoPackage pkg : index.getPackages()) {

                platforms.addAll(pkg.getInstalledPlatforms());

            }
        }
        return platforms;
    }

    static public boolean areThereInstalledBoards() {
        for (ArduinoPlatformPackageIndex index : getPackageIndices()) {
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
        for (ArduinoPlatformPackageIndex index : getPackageIndices()) {
            packages.addAll(index.getPackages());
        }
        return packages;
    }

    static public ArduinoPackage getPackage(String JasonName, String packageName) {
        for (ArduinoPlatformPackageIndex index : getPackageIndices()) {
            if (index.getJsonFile().getName().equals(JasonName)) {
                return index.getPackage(packageName);
            }
        }
        return null;
    }

    static public ArduinoPackage getPackage(String packageName) {
        for (ArduinoPlatformPackageIndex index : getPackageIndices()) {
            ArduinoPackage pkg = index.getPackage(packageName);
            if (pkg != null) {
                return pkg;
            }
        }
        return null;
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

    public static ArduinoPlatformVersion getPlatform(String vendor, String architecture, VersionNumber refVersion) {
        ArduinoPlatform platform = getPlatform(vendor, architecture);
        if (platform != null) {
            return platform.getVersion(refVersion);
        }
        return null;
    }

}
