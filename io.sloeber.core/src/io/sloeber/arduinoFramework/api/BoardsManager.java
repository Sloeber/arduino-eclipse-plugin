package io.sloeber.arduinoFramework.api;

import static io.sloeber.core.Messages.*;
import static io.sloeber.core.api.Common.*;
import static io.sloeber.core.api.ConfigurationPreferences.*;
import static io.sloeber.core.api.Const.*;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.statushandlers.StatusManager;

import com.google.gson.Gson;

import io.sloeber.arduinoFramework.internal.ArduinoPlatformPackageIndex;
import io.sloeber.arduinoFramework.internal.ArduinoPlatformTool;
import io.sloeber.arduinoFramework.internal.ArduinoPlatformToolVersion;
import io.sloeber.arduinoFramework.internal.ArduinoPlatformTooldDependency;
import io.sloeber.core.Activator;
import io.sloeber.core.Messages;
import io.sloeber.core.api.ConfigurationPreferences;
import io.sloeber.core.api.Defaults;
import io.sloeber.core.api.SloeberProject;
import io.sloeber.core.api.VersionNumber;
import io.sloeber.core.common.InstancePreferences;
import io.sloeber.core.managers.InstallProgress;
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
	private static final String THIRD_PARTY_URL_FILE="sloeber_third_party_url.txt"; //$NON-NLS-1$
    private static final String[] DEFAULT_JSON_URLS = {"https://downloads.arduino.cc/packages/package_index.json", //$NON-NLS-1$
             "https://raw.githubusercontent.com/jantje/hardware/master/package_jantje_index.json", //$NON-NLS-1$
             "https://raw.githubusercontent.com/jantje/ArduinoLibraries/master/library_jantje_index.json", //$NON-NLS-1$
             "https://arduino.esp8266.com/stable/package_esp8266com_index.json", //$NON-NLS-1$
             "https://www.pjrc.com/teensy/package_teensy_index.json", //$NON-NLS-1$
             "https://downloads.arduino.cc/libraries/library_index.json"};//$NON-NLS-1$

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
     * @param architectureID
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
    static public BoardDescription getBoardDescription(String jsonFileName, String packageName, String architectureID,
            String boardID, Map<String, String> options) {
        if (LOCAL.equals(jsonFileName)) {
            return new BoardDescription(new File(packageName), boardID, options);
        }
        return getNewestBoardIDFromBoardsManager(jsonFileName, packageName, architectureID, boardID, options);
    }

    static private BoardDescription getNewestBoardIDFromBoardsManager(String jsonFileName, String packageName,
            String architectureID, String boardID, Map<String, String> options) {

        IArduinoPackage thePackage = getPackage(jsonFileName, packageName);
        if (thePackage == null) {
            System.err.println("failed to find package:" + packageName); //$NON-NLS-1$
            return null;
        }
        IArduinoPlatform platform = thePackage.getPlatform(architectureID);
        if (platform == null) {
            System.err.println("failed to find architecture ID " + architectureID + " in package:" + packageName); //$NON-NLS-1$ //$NON-NLS-2$
            return null;
        }
        IArduinoPlatformVersion platformVersion = platform.getNewestVersion();
        java.io.File boardsFile = platformVersion.getBoardsFile();
        BoardDescription boardid = new BoardDescription(boardsFile, boardID, options);

        return boardid;
    }

    public static void addPackageURLs(Collection<String> packageUrlsToAdd, boolean forceDownload) {
        if (!isReady()) {
            Activator.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID, BoardsManagerIsBussy, new Exception()));
            return;
        }
        HashSet<String> originalJsonUrls = new HashSet<>(Arrays.asList(getJsonURLList()));
        packageUrlsToAdd.addAll(originalJsonUrls);

        setJsonURLs(packageUrlsToAdd);
        loadJsons(forceDownload);
    }

    public static void setPackageURLs(Collection<String> packageUrls, boolean forceDownload) {
        if (!isReady()) {
            Activator.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID, BoardsManagerIsBussy, new Exception()));
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
        String DEPRECATED = "DEPRECATED"; //$NON-NLS-1$
        if (!isReady()) {
            Activator.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID, BoardsManagerIsBussy, new Exception()));
            return;
        }
        envVarsNeedUpdating = true;
        int currPlatformIndex = 1;
        NullProgressMonitor monitor = new NullProgressMonitor();
        List<IArduinoPackage> allPackages = getPackages();
        for (IArduinoPackage curPackage : allPackages) {
            Collection<IArduinoPlatform> latestPlatforms = curPackage.getPlatforms();
            for (IArduinoPlatform curPlatform : latestPlatforms) {
                if (!curPlatform.getName().toUpperCase().contains(DEPRECATED)) {
                    if (currPlatformIndex > fromIndex) {
                        IArduinoPlatformVersion latestPlatformVersion = curPlatform.getNewestVersion();
                        if (!latestPlatformVersion.getName().toUpperCase().contains(DEPRECATED)) {
                            install(latestPlatformVersion, monitor);
                        } else {
                            System.out.println("skipping platform " + latestPlatformVersion.toString()); //$NON-NLS-1$
                        }
                    }
                    if (currPlatformIndex++ > toIndex) {
                        return;
                    }
                } else {
                    System.out.println("skipping platform " + curPlatform.toString()); //$NON-NLS-1$
                }
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
            Activator.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID, BoardsManagerIsBussy, new Exception()));
            return;
        }
        envVarsNeedUpdating = true;
        IArduinoPackage curPackage = getPackage(JasonName, packageName);
        if (curPackage != null) {
            IArduinoPlatform curPlatform = curPackage.getPlatform(architectureName);
            if (curPlatform != null) {
                IArduinoPlatformVersion curPlatformVersion = curPlatform.getNewestVersion();
                if (curPlatformVersion != null) {
                    NullProgressMonitor monitor = new NullProgressMonitor();
                    install(curPlatformVersion, monitor);
                    return;
                }
            }
        }
        Activator.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
                "failed to find " + JasonName + " " + packageName + " " + architectureName)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    private static IStatus install(IArduinoPlatformVersion platformVersion, IProgressMonitor monitor) {
        boolean forceDownload = false;
        //        String name = platformVersion.getName();
        //        String architecture = platformVersion.getArchitecture();
        //        String version = platformVersion.getVersion().toString();
        // Check if we're installed already
        if (platformVersion.isInstalled()) {
            System.out.println("reusing platform " + platformVersion.toString()); //$NON-NLS-1$
            return Status.OK_STATUS;
        }

        // Download platform archive
        System.out.println("start installing platform " + platformVersion.toString()); //$NON-NLS-1$

        MyMultiStatus mstatus = new MyMultiStatus("Failed to install " + platformVersion.getName()); //$NON-NLS-1$
        mstatus.addErrors(PackageManager.downloadAndInstall(platformVersion, forceDownload, monitor));
        if (!mstatus.isOK()) {
            // no use installing tools when the boards failed installing
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

        IArduinoPackage referencingPkg = platformVersion.getParent().getParent();
        for (ArduinoPlatformTooldDependency toolDependency : platformVersion.getToolsDependencies()) {
            ArduinoPlatformToolVersion tool = referencingPkg.getTool(toolDependency.getName(),
                    toolDependency.getVersion());
            if (tool == null) {
                //this is a tool provided by another platform
                //and the referencing platform does not specify the installable info
                //This means the package file of the referencing platform needs to be provided
                IArduinoPackage pkg = getPackageByProvider(toolDependency.getPackager());
                if (pkg != null) {
                    tool = pkg.getTool(toolDependency.getName(), toolDependency.getVersion());
                }
            }
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

        WorkAround.applyKnownWorkArounds(platformVersion);

        System.out.println("done installing platform " + platformVersion.toString()); //$NON-NLS-1$
        return mstatus;
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
            searchFiles(new File(CurFolder), boardFiles, BOARDS_FILE_NAME, 6);
        }
        if (boardFiles.size() == 0) {
            Activator.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
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
                    Activator.log(new Status(IStatus.INFO, CORE_PLUGIN_ID,
                            Helpers_Error_The_folder_is_empty.replace(FOLDER_TAG, folder.toString()), null));
                    myHasbeenLogged = true;
                }
                return;
            }
            for (File f : a) {
                if (f.isDirectory()) {
                	//ignore folders named tools
                	if(!f.getName().equals(TOOLS)) {
                		searchFiles(f, Hardwarelists, Filename, depth - 1);
                	}
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

    public static IStatus updatePlatforms(List<IArduinoPlatformVersion> platformsToInstall,
            List<IArduinoPlatformVersion> platformsToRemove, IProgressMonitor monitor, MultiStatus status) {
        if (!isReady()) {
            status.add(new Status(IStatus.ERROR, CORE_PLUGIN_ID, BoardsManagerIsBussy, null));
            return status;
        }
        //TODO updating the jsons after selecting what to install seems dangerous to me; check to delete
        if (!ConfigurationPreferences.getUpdateJasonFilesFlag()) {
            loadJsons(true);
        }
        envVarsNeedUpdating = true;
        try {
            myIsReady = false;
            for (IArduinoPlatformVersion curPlatform : platformsToRemove) {
                status.add(uninstall(curPlatform, monitor));
            }
            for (IArduinoPlatformVersion curPlatform : platformsToInstall) {
                status.add(install(curPlatform, monitor));
            }

        } catch (@SuppressWarnings("unused") Exception e) {
            // do nothing
        }
        myIsReady = true;
        SloeberProject.reloadTxtFile();
        return status;
    }

    public static IStatus uninstall(IArduinoPlatformVersion curPlatform, IProgressMonitor monitor) {
        if (!curPlatform.isInstalled()) {
            return Status.OK_STATUS;
        }

        IPath installFolder = curPlatform.getInstallPath();
        try {
            deleteDirectory(installFolder);
        } catch (IOException e) {
            return new Status(IStatus.ERROR, Activator.getId(), "Failed to remove folder" + installFolder.toString(), //$NON-NLS-1$
                    e);
        }

        return Status.OK_STATUS;
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
            Activator.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID, BoardsManagerIsBussy, new Exception()));
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
        //sorting here so things look good in the gui
        Collections.sort(packageIndices);
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
                Activator.log(new Status(IStatus.ERROR, Activator.getId(), "Unable to download " + url, e)); //$NON-NLS-1$
            }
        }
        if (jsonFile.exists()) {
            if (jsonFile.getName().toLowerCase().startsWith("package_")) { //$NON-NLS-1$
                loadPackage(url,jsonFile);
            } else if (jsonFile.getName().toLowerCase().startsWith("library_")) { //$NON-NLS-1$
                LibraryManager.loadJson(jsonFile);
            } else {
                Activator.log(new Status(IStatus.ERROR, Activator.getId(),
                        "json files should start with \"package_\" or \"library_\" " + url + " is ignored")); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    static private void loadPackage(String url, File jsonFile) {
        try (Reader reader = new FileReader(jsonFile)) {
            ArduinoPlatformPackageIndex index = new Gson().fromJson(reader, ArduinoPlatformPackageIndex.class);
            index.setPackageFile(jsonFile);
            index.setURL(url);
            packageIndices.add(index);
        } catch (Exception e) {
            Activator.log(new Status(IStatus.ERROR, Activator.getId(),
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
                Activator.log(new Status(IStatus.ERROR, Activator.getId(), "Malformed url " + url, e)); //$NON-NLS-1$
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

    public static String[] getDefaultJsonURLs() {
        return DEFAULT_JSON_URLS;
    }


    private static void setJsonURLs(Collection<String> urls) {
    	IPath myThirdPartyURLStoragePath=getThirdPartyURLStoragePath();
    	try {
    		if(myThirdPartyURLStoragePath!=null ) {
    			Files.write(myThirdPartyURLStoragePath.toPath(),urls, Charset.forName(StandardCharsets.UTF_8.name()));
    		}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public static String[] getJsonURLList() {
    	IPath myThirdPartyURLStoragePath=getThirdPartyURLStoragePath();
    	try {
    		if(myThirdPartyURLStoragePath!=null && myThirdPartyURLStoragePath.toFile().exists()) {
    			List<String>thirdPartyURLs = Files.readAllLines(myThirdPartyURLStoragePath.toPath(), Charset.forName(StandardCharsets.UTF_8.name()));
    			if(thirdPartyURLs.size()>0) {
    				return thirdPartyURLs.toArray(new String[thirdPartyURLs.size()]);
    			}
    		}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	//The new way of storing the thirdparty urls's failed
    	//try the Sloeber V3 way for downwards compatibility
    	String[] sloeberV4Storage= getString("Manager jsons", EMPTY_STRING).replace("\r", new String()).split("\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    	if(sloeberV4Storage.length>3) {
    		return sloeberV4Storage;
    	}
    	//Everything failed; This is probably a new install; return the defaults;
    	return DEFAULT_JSON_URLS;

    }

    private static IPath getThirdPartyURLStoragePath() {
    	return sloeberHomePath.append(SLOEBER_HOME_SUB_FOLDER).append(THIRD_PARTY_URL_FILE);
    }


    public static void removeAllInstalledPlatforms() {
        if (!isReady()) {
            Activator.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID, BoardsManagerIsBussy, new Exception()));
            return;
        }
        try {
            deleteDirectory(ConfigurationPreferences.getInstallationPathPackages());
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
        IArduinoPlatformVersion latestAvrPlatform = getNewestInstalledPlatform(VENDOR_ARDUINO, AVR);
        IArduinoPlatformVersion latestSamdPlatform = getNewestInstalledPlatform(VENDOR_ARDUINO, SAMD);
        IArduinoPlatformVersion latestSamPlatform = getNewestInstalledPlatform(VENDOR_ARDUINO, SAM);

        if (latestSamdPlatform != null) {
            myWorkbenchEnvironmentVariables.putAll(getEnvVarPlatformFileTools(latestSamdPlatform));
        }
        if (latestSamPlatform != null) {
            myWorkbenchEnvironmentVariables.putAll(getEnvVarPlatformFileTools(latestSamPlatform));
        }
        if (latestAvrPlatform != null) {
            myWorkbenchEnvironmentVariables.putAll(getEnvVarPlatformFileTools(latestAvrPlatform));
        }
        envVarsNeedUpdating = false;
        return myWorkbenchEnvironmentVariables;
    }

    private static Map<String, String> getEnvVarPlatformFileTools(IArduinoPlatformVersion platformVersion) {
        HashMap<String, String> vars = new HashMap<>();
        IArduinoPackage pkg = platformVersion.getParent().getParent();
        for (ArduinoPlatformTooldDependency tool : platformVersion.getToolsDependencies()) {
            ArduinoPlatformTool theTool = pkg.getTool(tool.getName());
            if (theTool == null) {
                System.err.println("Did not find " + tool.getName() + " in package with ID " + pkg.getID()); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                vars.putAll(theTool.getEnvVars(null));
            }
        }
        return vars;
    }

    /**
     * given a vendor and a architecture provide the newest installed platform
     * version
     *
     * @param vendor
     * @param architecture
     * @return the found platformVersion or null if none found
     */
    public static IArduinoPlatformVersion getNewestInstalledPlatform(String vendor, String architecture) {
        IArduinoPlatform platform = getPlatform(vendor, architecture);
        if (platform == null) {
            return null;
        }
        return platform.getNewestInstalled();
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
            Activator.log(PackageManager.downloadAndInstall(Defaults.EXAMPLES_URL, Defaults.EXAMPLE_PACKAGE, examplesPath,
                    false, monitor));
        }
        if (!areThereInstalledBoards()) {

            IStatus status = null;

            // if successfully installed the examples: add the boards
            IArduinoPlatform platform = getPlatform(Defaults.DEFAULT_INSTALL_MAINTAINER,
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
                status = install(platform.getNewestVersion(), monitor);
            }

            if (!status.isOK()) {
                StatusManager stMan = StatusManager.getManager();
                stMan.handle(status, StatusManager.LOG | StatusManager.SHOW | StatusManager.BLOCK);
            }

        }
        myIsReady = true;

    }

    synchronized static public List<IArduinoPlatformPackageIndex> getPackageIndices() {
        if (packageIndices == null) {
            loadJsons(false);
        }
        return new LinkedList<>(packageIndices);
    }

    public static List<IArduinoPlatform> getPlatforms() {
        List<IArduinoPlatform> platforms = new ArrayList<>();
        for (IArduinoPlatformPackageIndex index : getPackageIndices()) {
            for (IArduinoPackage pkg : index.getPackages()) {
                platforms.addAll(pkg.getPlatforms());
            }
        }
        return platforms;
    }

    public static IArduinoPlatform getPlatform(String vendor, String architecture) {

        for (IArduinoPlatformPackageIndex index : getPackageIndices()) {
            IArduinoPackage pkg = index.getPackage(vendor);
            if (pkg != null) {
                IArduinoPlatform platform = pkg.getPlatform(architecture);
                if (platform != null) {
                    return platform;
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
    public static IArduinoPlatformVersion getPlatform(IPath platformPath) {
        for (IArduinoPlatformPackageIndex index : getPackageIndices()) {
            for (IArduinoPackage pkg : index.getPackages()) {
                for (IArduinoPlatform curPlatform : pkg.getPlatforms()) {
                    if (platformPath
                            .matchingFirstSegments(curPlatform.getInstallPath()) > (platformPath.segmentCount() - 2))

                        for (IArduinoPlatformVersion curPlatformVersion : curPlatform.getVersions()) {
                            if (curPlatformVersion.getInstallPath().equals(platformPath)) {
                                return curPlatformVersion;
                            }
                        }
                }
            }
        }
        return null;
    }

    static public List<IArduinoPlatformVersion> getInstalledPlatforms() {
        List<IArduinoPlatformVersion> platforms = new ArrayList<>();
        for (IArduinoPlatformPackageIndex index : getPackageIndices()) {
            for (IArduinoPackage pkg : index.getPackages()) {

                platforms.addAll(pkg.getInstalledPlatforms());

            }
        }
        return platforms;
    }

    static public boolean areThereInstalledBoards() {
        for (IArduinoPlatformPackageIndex index : getPackageIndices()) {
            for (IArduinoPackage pkg : index.getPackages()) {
                if (pkg.isInstalled()) {
                    return true;
                }
            }
        }
        return false;
    }

    static public List<IArduinoPackage> getPackages() {
        List<IArduinoPackage> packages = new ArrayList<>();
        for (IArduinoPlatformPackageIndex index : getPackageIndices()) {
            packages.addAll(index.getPackages());
        }

        return packages;
    }

    static public IArduinoPackage getPackage(String JasonName, String packageName) {
        for (IArduinoPlatformPackageIndex index : getPackageIndices()) {
            if (index.getJsonFile().getName().equals(JasonName)) {
                return index.getPackage(packageName);
            }
        }
        return null;
    }

    static public IArduinoPackage getPackage(String packageName) {
        for (IArduinoPlatformPackageIndex index : getPackageIndices()) {
            IArduinoPackage pkg = index.getPackage(packageName);
            if (pkg != null) {
                return pkg;
            }
        }
        return null;
    }


    /**
     * Remove all packages that have a more recent version
     */
    public static void onlyKeepLatestPlatforms() {
        if (!isReady()) {
            Activator.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID, BoardsManagerIsBussy, new Exception()));
            return;
        }
        List<IArduinoPackage> allPackages = getPackages();
        for (IArduinoPackage curPackage : allPackages) {
            curPackage.onlyKeepLatestPlatforms();
        }
    }

    public static IArduinoPlatformVersion getPlatform(String vendor, String architecture, VersionNumber refVersion) {
        IArduinoPlatform platform = getPlatform(vendor, architecture);
        if (platform != null) {
            return platform.getVersion(refVersion);
        }
        return null;
    }

    public static IArduinoPackage getPackageByProvider(String packager) {
        for (IArduinoPlatformPackageIndex index : getPackageIndices()) {
            for (IArduinoPackage pkg : index.getPackages()) {
                if (packager.equals(pkg.getID())) {
                    return pkg;
                }
            }
        }
        return null;
    }

}
