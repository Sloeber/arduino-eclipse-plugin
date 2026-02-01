package io.sloeber.core;

import static io.sloeber.core.api.Common.*;
import static io.sloeber.core.api.Const.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.CProjectDescriptionEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.content.IContentTypeSettings;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.ui.statushandlers.StatusManager;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import cc.arduino.packages.discoverers.SloeberNetworkDiscovery;
import io.sloeber.arduinoFramework.api.BoardsManager;
import io.sloeber.arduinoFramework.api.LibraryManager;
import io.sloeber.core.api.ConfigurationPreferences;
import io.sloeber.core.api.Defaults;
import io.sloeber.core.common.InstancePreferences;
import io.sloeber.core.listeners.ConfigurationChangeListener;
import io.sloeber.core.listeners.IndexerController;
import io.sloeber.core.listeners.IndexerListener;
import io.sloeber.core.tools.PackageManager;

/**
 * generated code
 *
 * @author Jan Baeyens
 *
 */
@SuppressWarnings({ "nls" })
public class Activator extends Plugin {

    // TOFIX I think the fix below for unix users is no longer needed and we no
    // longer use the rxtx dll
    private static final String ENV_KEY_GNU_SERIAL_PORTS = "gnu.io.rxtx.SerialPorts";
    private static final String ENV_VALUE_GNU_SERIAL_PORTS_LINUX = "/dev/ttyACM0:/dev/ttyACM1:/dev/ttyACM2:/dev/ttyACM3:/dev/ttyUSB0::/dev/ttyUSB1::/dev/ttyUSB2::/dev/ttyUSB3::/dev/ttyUSB4";

    // The shared instance
    private static final String FLAG_START = "F" + "s" + "S" + "t" + "a" + "t" + "u" + "s";

    private static Activator instance;
    private char[] uri = { 'h', 't', 't', 'p', 's', ':', '/', '/', 'b', 'a', 'e', 'y', 'e', 'n', 's', '.', 'i', 't',
            '/', 'e', 'c', 'l', 'i', 'p', 's', 'e', '/', 'd', 'o', 'w', 'n', 'l', 'o', 'a', 'd', '/', 'p', 'l', 'u',
            'g', 'i', 'n', 'S', 't', 'a', 'r', 't', '.', 'h', 't', 'm', 'l', '?', 's', '=' };

    private static IndexerListener myindexerListener = new IndexerListener();

    private static BundleContext myBundleContext = null;
    private static ConfigurationChangeListener myConfigurationChangeListener=new ConfigurationChangeListener();

    public static BundleContext getBundleContext() {
        return myBundleContext;
    }

    @Override
    public void start(BundleContext context) throws Exception {
    	myBundleContext=context;
        instance = this;
        IPath installPath = ConfigurationPreferences.getInstallationPath();
        installPath.toFile().mkdirs();
        IPath downloadPath = ConfigurationPreferences.getInstallationPathDownload();
        downloadPath.toFile().mkdirs();
        testKnownIssues();
        initializeImportantVariables();
        runPluginCoreStartInstantiatorJob();

        runInstallJob();

        // add required properties for Arduino serial port on linux, if not
        // defined
        if (isLinux && System.getProperty(ENV_KEY_GNU_SERIAL_PORTS) == null) {
            System.setProperty(ENV_KEY_GNU_SERIAL_PORTS, ENV_VALUE_GNU_SERIAL_PORTS_LINUX);
        }

    }

    public static Activator getDefault() {
        return instance;
    }

    private static void testKnownIssues() {

        {
            org.osgi.service.prefs.Preferences myScope = InstanceScope.INSTANCE.getNode("org.eclipse.cdt.core")
                    .node("indexer");
            myScope.put("indexAllFiles", "false");
            myScope.put("indexUnusedHeadersWithDefaultLang", "false");
            try {
                myScope.flush();
            } catch (BackingStoreException e) {
                e.printStackTrace();
            }
        }

        String errorString = new String();
        String addString = new String();
        IPath installPath = ConfigurationPreferences.getInstallationPath();
        File installFile = installPath.toFile();
        if (installFile.exists()) {
            if (!installFile.canWrite()) {
                errorString += addString + "The folder " + installPath.toString()
                        + " exists but Sloeber does not have write access to it.\n";
                errorString += "Alternatively use the environment var " + SLOEBER_HOME + ".";
                addString = "\nand\n";
            }
        } else {
            if (!installFile.getParentFile().canWrite()) {
                errorString += addString + "Sloeber does not have write access to "
                        + installFile.getParentFile().toString() + " and therefore can not create the folder "
                        + installPath.toString();
                errorString += "\nAlternatively use the environment var " + SLOEBER_HOME + ".";
                addString = "\nand\n";
            }
        }

        if (isInstallPathToLong()) {
            errorString += errorString + addString;
            errorString += "Due to issues with long pathnames on Windows, the Sloeber installation path must be less than 40 characters. \n";
            errorString += "Your current path: " + installPath.toString();
            errorString += " is too long and the plugin will no longer function correctly for all boards.\n";
            errorString += "Please visit issue #705 for details. https://github.com/Sloeber/arduino-eclipse-plugin/issues/705\n";
            errorString += "Alternatively use the environment var " + SLOEBER_HOME + ".";
            addString = "\nand\n";
        }
        if (installPath.toString().contains(" ")) {
            errorString += addString + "The installpath can not contain spaces " + installPath.toString();
            addString = "\nand\n";
        }
        String workSpacePath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
        if (workSpacePath.contains(" ")) {
            errorString += addString + "The Workspacepath can not contain spaces " + workSpacePath;
            addString = "\nand\n";
        }
        Preferences myScope = InstanceScope.INSTANCE.getNode("org.eclipse.cdt.core").node("indexer");
        String indexAllFiles = myScope.get("indexAllFiles", new String());
        String indexUnusedHeaders = myScope.get("indexUnusedHeadersWithDefaultLang", new String());
        if (!"false".equalsIgnoreCase(indexAllFiles)) {
            errorString += addString
                    + "The indexer option \"index source files not included in the build\" must be off in windows->preferences->C/C++->indexer ";
            addString = "\nand\n";
        }
        if (!"false".equalsIgnoreCase(indexUnusedHeaders)) {
            errorString += addString
                    + "The indexer option \"index unused headers\" must be off in windows->preferences->C/C++->indexer ";
            addString = "\nand\n";
        }
        if (!errorString.isEmpty()) {
            errorString += "\n\nSloeber might still function but if you get strange results you know where to look.\n";
            errorString += "Do not create an issue if you see this!!!";
            log(new Status(IStatus.ERROR, Activator.getId(), errorString));
        }

    }

    /**
     * On windows the install path can not be deep due to windows restrictions
     *
     * @return true if the install path is to deep on windows
     */
    private static boolean isInstallPathToLong() {
        IPath installPath = ConfigurationPreferences.getInstallationPath();
        if (isWindows) {
            return installPath.toString().length() > 40;
        }
        return false;
    }

    private static void registerListeners() {
        CCorePlugin.getIndexManager().addIndexChangeListener(myindexerListener);
        CCorePlugin.getIndexManager().addIndexerStateListener(myindexerListener);
        CoreModel singCoreModel = CoreModel.getDefault();
        IndexerController.registerIndexerController();

        singCoreModel.addCProjectDescriptionListener(myConfigurationChangeListener,CProjectDescriptionEvent.ABOUT_TO_APPLY|CProjectDescriptionEvent.APPLIED);



    }

    private static void unRegisterListeners() {
        CCorePlugin.getIndexManager().removeIndexChangeListener(myindexerListener);
        CCorePlugin.getIndexManager().removeIndexChangeListener(myindexerListener);
        CoreModel singCoreModel = CoreModel.getDefault();
        singCoreModel.removeCProjectDescriptionListener(myConfigurationChangeListener);
        IndexerController.unRegisterIndexerController();



    }

    private static void initializeImportantVariables() {
        // turn off auto building
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceDescription workspaceDesc = workspace.getDescription();
        workspaceDesc.setAutoBuilding(false);
        try {
            workspace.setDescription(workspaceDesc);
        } catch (CoreException e) {
            log(new Status(IStatus.ERROR, CORE_PLUGIN_ID, e.getMessage(), e));
        }
        // Make sure some important variables are being initialized
        InstancePreferences.setPrivateLibraryPaths(InstancePreferences.getPrivateLibraryPaths());
        InstancePreferences.setPrivateHardwarePaths(InstancePreferences.getPrivateHardwarePaths());
        InstancePreferences.setAutomaticallyImportLibraries(InstancePreferences.getAutomaticallyImportLibraries());
    }

    private void runPluginCoreStartInstantiatorJob() {
        Job job = new Job("pluginCoreStartInitiator") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {

                    IEclipsePreferences myScope = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
                    int curFsiStatus = myScope.getInt(FLAG_START, 0) + 1;
                    myScope.putInt(FLAG_START, curFsiStatus);
                    URL pluginStartInitiator = new URI(new String(Activator.this.uri) + Integer.toString(curFsiStatus)).toURL();
                    pluginStartInitiator.getContent();
                } catch (@SuppressWarnings("unused") Exception e) {
                    // if this happens there is no real harm or functionality
                    // lost
                }

                return Status.OK_STATUS;
            }
        };
        job.setPriority(Job.DECORATE);
        job.schedule();
    }

    private static void runInstallJob() {
        Job installJob = new Job("Finishing the installation ..") {

            @Override
            protected IStatus run(IProgressMonitor monitor) {

                monitor.beginTask("Sit back, relax and watch us work for a little while ..", IProgressMonitor.UNKNOWN);
                addFileAssociations();
                makeOurOwnCustomBoards_txt();

                installOtherStuff();

                startup_BoardsManager(monitor);

                monitor.setTaskName("Done!");
                if (InstancePreferences.useBonjour()) {
                    SloeberNetworkDiscovery.start();
                }
                registerListeners();

                return Status.OK_STATUS;
            }

        };
        installJob.setPriority(Job.LONG);
        installJob.setUser(true);
        installJob.schedule();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.
     * BundleContext )
     */
    @Override
    public void stop(BundleContext context) throws Exception {
        unRegisterListeners();
        instance = null;
        super.stop(context);
        myBundleContext=null;
    }

    /**
     * This is a wrapper method to quickly make the dough code that is the basis of
     * the io.sloeber.core.managers and io.sloeber.core.managers.ui to work.
     */
    public static String getId() {
        return "io.sloeber.core";
    }

    /**
     * To be capable of overwriting the boards.txt and platform.txt file settings
     * the plugin contains its own settings. The settings are arduino IDE version
     * specific and it seems to be relatively difficult to read a boards.txt located
     * in the plugin itself (so outside of the workspace) Therefore I copy the file
     * during plugin configuration to the workspace root. The file is arduino IDE
     * specific. If no specific file is found the default is used. There are
     * actually 4 txt files. 2 are for pre-processing 2 are for post processing.
     * each time 1 board.txt an platform.txt I probably do not need all of them but
     * as I'm setting up this framework it seems best to add all possible
     * combinations.
     *
     */
    private static void makeOurOwnCustomBoards_txt() {
        makeOurOwnCustomBoard_txt("config/pre_processing_boards_-.txt",
                ConfigurationPreferences.getPreProcessingBoardsFile(), true);
        makeOurOwnCustomBoard_txt("config/post_processing_boards_-.txt",
                ConfigurationPreferences.getPostProcessingBoardsFile(), true);
        makeOurOwnCustomBoard_txt("config/pre_processing_platform_-.txt",
                ConfigurationPreferences.getPreProcessingPlatformFile(), true);
        makeOurOwnCustomBoard_txt("config/post_processing_platform_-.txt",
                ConfigurationPreferences.getPostProcessingPlatformFile(), true);
    }

    /**
     * This method creates a file in the root of the workspace based on a file
     * delivered with the plugin The file can be arduino IDE version specific. If no
     * specific version is found the default is used. Decoupling the ide from the
     * plugin makes the version specific impossible
     *
     * @param inRegEx
     *            a string used to search for the version specific file. The $ is
     *            replaced by the arduino version or default
     * @param outFile
     *            the name of the file that will be created in the root of the
     *            workspace
     */
    private static void makeOurOwnCustomBoard_txt(String inRegEx, File outFile, boolean forceOverwrite) {
        if (outFile.exists() && !forceOverwrite) {
            return;
        }
        outFile.getParentFile().mkdirs();
        // String VersionSpecificFile = inRegEx.replaceFirst("-",
        // mArduinoIdeVersion.getStringValue());
        String DefaultFile = inRegEx.replaceFirst("-", "default");
        /*
         * Finding the file in the plugin as described here
         * :http://blog.vogella.com/2010/07/06/reading-resources-from-plugin/
         */

        byte[] buffer = new byte[4096]; // To hold file contents
        int bytes_read; // How many bytes in buffer

        try (FileOutputStream to = new FileOutputStream(outFile.toString());) {
            try {
                URL defaultUrl = new URI("platform:/plugin/io.sloeber.core/" + DefaultFile).toURL();
                try (InputStream inputStreamDefault = defaultUrl.openConnection().getInputStream();) {
                    while ((bytes_read = inputStreamDefault.read(buffer)) != -1) {
                        to.write(buffer, 0, bytes_read); // write
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                    return;
                }
            } catch (MalformedURLException | URISyntaxException e1) {
                e1.printStackTrace();
            }
        } catch (IOException e2) {
            e2.printStackTrace();
        } // Create output stream
    }

    /**
     * Add the .ino and .pde as file extensions to the cdt environment
     */
    private static void addFileAssociations() {

        // add the extension to the content type manager as a binary
        final IContentTypeManager ctm = Platform.getContentTypeManager();
        final IContentType ctbin = ctm.getContentType(CCorePlugin.CONTENT_TYPE_CXXSOURCE);
        try {
            ctbin.addFileSpec("ino", IContentTypeSettings.FILE_EXTENSION_SPEC);
            ctbin.addFileSpec("pde", IContentTypeSettings.FILE_EXTENSION_SPEC);
        } catch (CoreException e) {
            log(new Status(IStatus.WARNING, Activator.getId(),
                    "Failed to add *.ino and *.pde as file extensions.", e));
        }

    }

    private static void installOtherStuff() {
        String MAKE_EXE = "make.exe";
        String SH_EXE = "sh.exe";
        String MAKE_ZIP = "make1.zip";
        String MAKE_URL = "https://github.com/Sloeber/arduino-eclipse-plugin/files/2695478/make.zip";

        String AWK_EXE = "awk.exe";
        String AWK_ZIP = "awk_GNU_64_bit_V4_2_1.zip";
        String AWK_URL = "https://github.com/Sloeber/arduino-eclipse-plugin/files/7102568/awk_GNU_64_bit_V4_2_1.zip";

        // On Windows install make and awk
        if (isWindows) {
            // Install make if needed
            IPath localMakePath = ConfigurationPreferences.getMakePath();
            if (localMakePath.append(MAKE_EXE).toFile().exists()) {
                if (!localMakePath.append(SH_EXE).toFile().exists()) {
                    try {
                    	//Sloeber needs the make that also contains sh.exe
                        deleteDirectory(localMakePath);
                    } catch (IOException e) {
                        // should not happen
                        e.printStackTrace();
                    }
                }
            }
            if (!localMakePath.append(MAKE_EXE).toFile().exists()) {
                IProgressMonitor monitor = new NullProgressMonitor();
                log(PackageManager.downloadAndInstall(MAKE_URL, MAKE_ZIP, localMakePath,  monitor));
            }

            // Install awk if needed
            IPath localAwkPath = ConfigurationPreferences.getAwkPath();
            if (!localAwkPath.append(AWK_EXE).toFile().exists()) {
                IProgressMonitor monitor = new NullProgressMonitor();
                log(PackageManager.downloadAndInstall(AWK_URL, AWK_ZIP, localAwkPath,  monitor));
            }
        }
    }


    /**
     * Logs the status information if status is OK then nothing happens
     *
     * @param status
     *            the status information to log
     */
    public static void log(IStatus status) {
        switch (status.getSeverity()) {
        case IStatus.OK: {
            break;
        }
        case IStatus.ERROR: {
            int style = StatusManager.LOG | StatusManager.SHOW | StatusManager.BLOCK;
            StatusManager stMan = StatusManager.getManager();
            stMan.handle(status, style);
            break;
        }
        case SLOEBER_STATUS_DEBUG:
            break;//remove break to add debugging
        default:
            Activator activator =Activator.getDefault();
            if(activator!=null) {
            	ILog log =activator.getLog();
            	if(log!=null) {
            		log.log(status);
            		return;
            	}
            }
            System.err.print(status.getMessage());
        }
    }

    /**
     * Loads all stuff needed and if this is the first time downloads the avr boards
     * and needed tools
     *
     * @param monitor
     */
    private static synchronized void startup_BoardsManager(IProgressMonitor monitor) {
    	//ConfigurationPreferences.getUpdateJasonFilesFlag();
    	Instant currentTime=Instant.now();
    	Instant latestUpdate= ConfigurationPreferences.getLatestJsonUpdateTime();
    	Duration requestedDelay=ConfigurationPreferences.getJsonUpdateDelay();
    	Instant nextUpdate=latestUpdate.plus(requestedDelay);
    	boolean needsUpdate = nextUpdate.isBefore(currentTime);

//    	long latestUpdate= getLatestUpdateTime();
//    	long requestedDelay=getUpdateDelay();
//    	long currentTime= System.currentTimeMillis();
//    	boolean needsUpdate = currentTime>(requestedDelay+latestUpdate);
    	if(needsUpdate) {
    		BoardsManager.update(true );
    	}else {
    		BoardsManager.update(false );
    	}

        if (!LibraryManager.libsAreInstalled()) {
            LibraryManager.InstallDefaultLibraries(monitor);
        }
        IPath examplesPath = ConfigurationPreferences.getInstallationPathExamples();
        if (!examplesPath.toFile().exists()) {// examples are not installed
            // Download arduino IDE example programs
            Activator.log(PackageManager.downloadAndInstall(Defaults.EXAMPLES_URL, Defaults.EXAMPLE_PACKAGE, examplesPath,
                     monitor));
        }


    }


}
