package io.sloeber.core.api;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.event.ChangeListener;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICExclusionPatternPathEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import io.sloeber.core.Activator;
import io.sloeber.core.InternalBoardDescriptor;
import io.sloeber.core.Messages;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.Const;
import io.sloeber.core.managers.InternalPackageManager;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.KeyValue;
import io.sloeber.core.tools.Libraries;
import io.sloeber.core.tools.Programmers;
import io.sloeber.core.tools.ShouldHaveBeenInCDT;
import io.sloeber.core.tools.TxtFile;

@SuppressWarnings({ "nls" })
public class BoardDescriptor {

    /*
     * Some constants
     */
    private static final String KEY_LAST_USED_BOARD = "Last used Board";
    private static final String KEY_LAST_USED_UPLOAD_PORT = "Last Used Upload port";
    private static final String KEY_LAST_USED_UPLOAD_PROTOCOL = "last Used upload Protocol";
    private static final String KEY_LAST_USED_BOARDS_FILE = "Last used Boards file";
    private static final String KEY_LAST_USED_BOARD_MENU_OPTIONS = "last used Board custom option selections";
    private static final String ENV_KEY_JANTJE_MENU_SELECTION = Const.ENV_KEY_JANTJE_START + "MENU";
    private static final String ENV_KEY_JANTJE_UPLOAD_PORT = Const.ENV_KEY_JANTJE_START + "COM_PORT";
    private static final String ENV_KEY_JANTJE_BOARD_NAME = Const.ENV_KEY_JANTJE_START + "BOARD_NAME";
    private static final String ENV_KEY_JANTJE_PROJECT_NAME = Const.ENV_KEY_JANTJE_START + "PROJECT_NAME";
    private static final String ENV_KEY_JANTJE_OS = Const.ENV_KEY_JANTJE_START + "OS_NAME";
    private static final String ENV_KEY_JANTJE_WORKSPACE_LOCATION = Const.ENV_KEY_JANTJE_START + "WORKSPACE_LOCATION";
    private static final String ENV_KEY_JANTJE_ECLIPSE_LOCATION = Const.ENV_KEY_JANTJE_START + "ECLIPSE_LOCATION";

    public static final String ENV_KEY_JANTJE_BOARDS_FILE = Const.ENV_KEY_JANTJE_START + "BOARDS_FILE";

    public static final String ENV_KEY_JANTJE_PACKAGE_ID = Const.ENV_KEY_JANTJE_START + "PACKAGE_ID";
    public static final String ENV_KEY_JANTJE_ARCITECTURE_ID = Const.ENV_KEY_JANTJE_START + "ARCHITECTURE_ID";
    public static final String ENV_KEY_JANTJE_BOARD_ID = Const.ENV_KEY_JANTJE_START + "BOARD_ID";
    public static final String ENV_KEY_SERIAL_PORT = Const.ERASE_START + "SERIAL.PORT";
    public static final String ENV_KEY_SERIAL_PORT_FILE = Const.ERASE_START + "SERIAL.PORT.FILE";
    private static final String ENV_KEY_BUILD_VARIANT_PATH = Const.ERASE_START + "BUILD.VARIANT.PATH";
    private static final String ENV_KEY_BUILD_ACTUAL_CORE_PATH = Const.ERASE_START + "BUILD.CORE.PATH";

    private static final String ENV_KEY_REFERENCED_CORE_PLATFORM_PATH = Const.ERASE_START + "REFERENCED.CORE.PATH";
    private static final String ENV_KEY_REFERENCED_VARIANT_PLATFORM_PATH = Const.ERASE_START
            + "REFERENCED.VARIANT.PATH";
    private static final String ENV_KEY_REFERENCED_UPLOAD_PLATFORM_PATH = Const.ERASE_START + "REFERENCED.UPLOAD.PATH";

    // preference nodes
    private static final String NODE_ARDUINO = Activator.NODE_ARDUINO;
    private static final String PLATFORM_FILE_NAME = "platform.txt";
    private static final String LIBRARY_PATH_SUFFIX = "libraries";
    private static final String JANTJE_ACTION_UPLOAD = "JANTJE.UPLOAD"; // this is actually the programmer
    private static final IEclipsePreferences myStorageNode = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
    private static final String TOOL = Messages.TOOL;
    private static final String BOARD = Messages.BOARD;
    private static final String FILE = Messages.FILE;

    /*
     * This is the basic info contained in the descriptor
     */
    private String myUploadPort;
    private String myProgrammer;
    private String myBoardID;
    private Map<String, String> myOptions;

    /*
     * the following data is stored to detect changes that will make the equal fail
     * so os changes, workspace changes, eclipse install changes will force a update
     * on the stored data
     */
    private String myProjectName = new String();
    private String myOSName = Platform.getOS();
    private String myWorkSpaceLocation = Common.getWorkspaceRoot().toString();
    private String myWorkEclipseLocation = ConfigurationPreferences.getEclipseHome().toString();

    /*
     * Stuff to make things work
     */
    private File myreferencingBoardsFile;
    protected TxtFile myTxtFile;
    private ChangeListener myChangeListener = null;

    private String myBoardsVariant;
    private IPath myReferencedBoardVariantPlatformPath;
    private String myBoardsCore;
    private IPath myReferencedCorePlatformPath;
    private IPath myReferencedUploadToolPlatformPath;
    private String myUploadTool;

    @Override
    public String toString() {
        return getReferencingBoardsFile() + " \"" + getBoardName() + "\" " + getUploadPort(); //$NON-NLS-2$
    }

    /**
     * Compare 2 descriptors and return true is they are equal. This method detects
     * - OS changes - project name changes - moves of workspace - changed runtine
     * eclipse install
     *
     * @param obj
     * @return true if equal otherwise false
     */
    public boolean equals(BoardDescriptor otherBoardDescriptor) {
        if (!this.getUploadPort().equals(otherBoardDescriptor.getUploadPort())) {
            return false;
        }
        if (!this.getProgrammer().equals(otherBoardDescriptor.getProgrammer())) {
            return false;
        }
        return !needsSettingDirty(otherBoardDescriptor);
    }

    /**
     * compare 2 board descriptors and return true if replacing one board descriptor
     * with the other implies that a rebuild is needed
     *
     * @param otherBoardDescriptor
     * @return
     */
    public boolean needsSettingDirty(BoardDescriptor otherBoardDescriptor) {

        if (!this.getBoardID().equals(otherBoardDescriptor.getBoardID())) {
            return true;
        }
        if (!this.getReferencingBoardsFile().equals(otherBoardDescriptor.getReferencingBoardsFile())) {
            return true;
        }
        if (!this.getOptions().equals(otherBoardDescriptor.getOptions())) {
            return true;
        }
        if (!this.getProjectName().equals(otherBoardDescriptor.getProjectName())) {
            return true;
        }
        if (!this.getMyOSName().equals(otherBoardDescriptor.getMyOSName())) {
            return true;
        }
        if (!this.getMyWorkEclipseLocation().equals(otherBoardDescriptor.getMyWorkEclipseLocation())) {
            return true;
        }
        if (!this.getMyWorkSpaceLocation().equals(otherBoardDescriptor.getMyWorkSpaceLocation())) {
            return true;
        }
        return false;
    }

    /*
     * Create a sketchProject. This class does not really create a sketch object.
     * Nor does it look for existing (mapping) sketch projects This class represents
     * the data passed between the UI and the core This class does contain a create
     * to create the project When confdesc is null the data will be taken from the
     * "last used " otherwise the data is taken from the project the confdesc
     * belongs to
     *
     */
    public static BoardDescriptor makeBoardDescriptor(ICConfigurationDescription confdesc) {
        return new InternalBoardDescriptor(confdesc);
    }

    /**
     * after construction data needs to be derived from other data. This method
     * derives all other data and puts it in fields
     */
    private void calculateDerivedFields() {

        myReferencedCorePlatformPath = getreferencingPlatformPath();
        myBoardsCore = null;
        myReferencedBoardVariantPlatformPath = myReferencedCorePlatformPath;
        myBoardsVariant = null;
        myReferencedUploadToolPlatformPath = myReferencedCorePlatformPath;
        myUploadTool = null;
        setDefaultOptions();
        // search in the board info
        Map<String, String> boardInfo = myTxtFile.getSection(getBoardID());
        ParseSection(boardInfo);

    }

    private void ParseSection(Map<String, String> boardInfo) {
        final String COLON = ":";
        if (boardInfo == null) {
            return; // there is a problem with the board ID
        }
        String core = boardInfo.get("build.core");
        String variant = boardInfo.get("build.variant");
        String upload = boardInfo.get("upload.tool");
        // also search the options
        for (Entry<String, String> curOption : this.myOptions.entrySet()) {
            String keyPrefix = "menu." + curOption.getKey() + '.' + curOption.getValue();
            String coreOption = boardInfo.get(keyPrefix + ".build.core");
            String variantOption = boardInfo.get(keyPrefix + ".build.variant");
            String uploadOption = boardInfo.get(keyPrefix + ".upload.tool");
            if (coreOption != null) {
                core = coreOption;
            }
            if (variantOption != null) {
                variant = variantOption;
            }
            if (uploadOption != null) {
                upload = uploadOption;
            }
        }
        String architecture = getArchitecture();
        if (core != null) {
            String valueSplit[] = core.split(":");
            if (valueSplit.length == 2) {
                String refVendor = valueSplit[0];
                String actualValue = valueSplit[1];
                myBoardsCore = actualValue;
                myReferencedCorePlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor, architecture);
                if (this.myReferencedCorePlatformPath == null) {
                    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                            Messages.Helpers_tool_reference_missing.replaceAll(TOOL, core)
                                    .replaceAll(FILE, getReferencingBoardsFile().toString())
                                    .replaceAll(BOARD, getBoardID())));
                    return;
                }
            } else if (valueSplit.length == 4) {
                String refVendor = valueSplit[0];
                String refArchitecture = valueSplit[1];
                String refVersion = valueSplit[2];
                String actualValue = valueSplit[3];
                myBoardsCore = actualValue;
                myReferencedCorePlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor,refArchitecture, refVersion);
                if (this.myReferencedCorePlatformPath == null) {
                    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                            Messages.Helpers_tool_reference_missing.replaceAll(TOOL, core)
                                    .replaceAll(FILE, getReferencingBoardsFile().toString())
                                    .replaceAll(BOARD, getBoardID())));
                    return;
                }
            } else {
                this.myBoardsCore = core;
            }
        }
        if (variant != null) {
            String valueSplit[] = variant.split(COLON);
            if (valueSplit.length == 2) {
                String refVendor = valueSplit[0];
                String actualValue = valueSplit[1];
                this.myBoardsVariant = actualValue;
                this.myReferencedBoardVariantPlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor, architecture);
                if (this.myReferencedBoardVariantPlatformPath == null) {
                    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                            Messages.Helpers_tool_reference_missing.replaceAll(TOOL, variant)
                                    .replaceAll(FILE, getReferencingBoardsFile().toString())
                                    .replaceAll(BOARD, getBoardID())));
                    return;
                }
            } else if (valueSplit.length == 4) {
                String refVendor = valueSplit[0];
                String refArchitecture = valueSplit[1];
                String refVersion = valueSplit[2];
                String actualValue = valueSplit[3];
                this.myBoardsVariant = actualValue;
                if ("*".equals(refVersion)) {
                    this.myReferencedBoardVariantPlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor, refArchitecture);
                } else {
                    this.myReferencedBoardVariantPlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor, refArchitecture,
                            refVersion);
                }
                if (this.myReferencedBoardVariantPlatformPath == null) {
                    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                            Messages.Helpers_tool_reference_missing.replaceAll(TOOL, variant)
                                    .replaceAll(FILE, getReferencingBoardsFile().toString())
                                    .replaceAll(BOARD, getBoardID())));
                    return;
                }
            } else {
                myBoardsVariant = variant;
            }
        }
        if (upload != null) {
            String valueSplit[] = upload.split(COLON);
            if (valueSplit.length == 2) {
                String refVendor = valueSplit[0];
                String actualValue = valueSplit[1];
                this.myUploadTool = actualValue;
                this.myReferencedUploadToolPlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor, architecture);
                if (this.myReferencedUploadToolPlatformPath == null) {
                    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                            Messages.Helpers_tool_reference_missing.replaceAll(TOOL, upload)
                                    .replaceAll(FILE, getReferencingBoardsFile().toString())
                                    .replaceAll(BOARD, getBoardID())));
                    return;
                }
            } else if (valueSplit.length == 4) {
                String refVendor = valueSplit[0];
                String refArchitecture = valueSplit[1];
                String refVersion = valueSplit[2];
                String actualValue = valueSplit[3];
                this.myUploadTool = actualValue;
                this.myReferencedUploadToolPlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor, refArchitecture,
                        refVersion);
                if (this.myReferencedUploadToolPlatformPath == null) {
                    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                            Messages.Helpers_tool_reference_missing.replaceAll(TOOL, upload)
                                    .replaceAll(FILE, getReferencingBoardsFile().toString())
                                    .replaceAll(BOARD, getBoardID())));
                    return;
                }
            } else {
                myUploadTool = upload;
            }
        }

    }

    protected BoardDescriptor(ICConfigurationDescription confdesc) {
        if (confdesc == null) {
            this.myreferencingBoardsFile = new File(myStorageNode.get(KEY_LAST_USED_BOARDS_FILE, ""));
            this.myTxtFile = new TxtFile(this.myreferencingBoardsFile);
            this.myBoardID = myStorageNode.get(KEY_LAST_USED_BOARD, "");
            this.myUploadPort = myStorageNode.get(KEY_LAST_USED_UPLOAD_PORT, "");
            this.myProgrammer = myStorageNode.get(KEY_LAST_USED_UPLOAD_PROTOCOL, Defaults.getDefaultUploadProtocol());
            this.myOptions = KeyValue.makeMap(myStorageNode.get(KEY_LAST_USED_BOARD_MENU_OPTIONS, new String()));

        } else {
            this.myUploadPort = Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_UPLOAD_PORT, "");
            this.myProgrammer = Common.getBuildEnvironmentVariable(confdesc, JANTJE_ACTION_UPLOAD, "");
            this.myreferencingBoardsFile = new File(
                    Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_BOARDS_FILE, ""));
            this.myBoardID = Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_BOARD_ID, "");
            this.myProjectName = Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_PROJECT_NAME, "");
            this.myTxtFile = new TxtFile(this.myreferencingBoardsFile);
            this.myOSName = Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_OS, "");
            this.myWorkSpaceLocation = Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_WORKSPACE_LOCATION,
                    "");
            this.myWorkEclipseLocation = Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_ECLIPSE_LOCATION,
                    "");
            String optinconcat = Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_MENU_SELECTION, "");
            this.myOptions = KeyValue.makeMap(optinconcat);
        }
        calculateDerivedFields();
    }

    public static BoardDescriptor makeBoardDescriptor(File boardsFile, String boardID, Map<String, String> options) {
        return new InternalBoardDescriptor(boardsFile, boardID, options);
    }

    /**
     * make a board descriptor for each board in the board.txt file with the default
     * options
     *
     * @param boardFile
     * @return a list of board descriptors
     */
    public static List<BoardDescriptor> makeBoardDescriptors(File boardFile, Map<String, String> options) {
        TxtFile txtFile = new TxtFile(boardFile);
        List<BoardDescriptor> boards = new ArrayList<>();
        for (String curboardName : txtFile.getAllNames()) {
            Map<String, String> boardSection = txtFile.getSection(txtFile.getBoardIDFromBoardName(curboardName));
            if (boardSection != null) {
                if (!"true".equalsIgnoreCase(boardSection.get("hide"))) {
                    boards.add(makeBoardDescriptor(boardFile, txtFile.getBoardIDFromBoardName(curboardName), options));
                }
            }
        }
        return boards;
    }

    /**
     * create a board descriptor
     *
     * @param boardsFile
     * @param boardID
     * @param options
     *            if null default options are taken
     */
    protected BoardDescriptor(File boardsFile, String boardID, Map<String, String> options) {
        this.myUploadPort = new String();
        this.myProgrammer = Defaults.getDefaultUploadProtocol();
        this.myBoardID = boardID;
        this.myOptions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.myreferencingBoardsFile = boardsFile;
        this.myTxtFile = new TxtFile(this.myreferencingBoardsFile);
        setDefaultOptions();
        if (options != null) {
            this.myOptions.putAll(options);
        }
        calculateDerivedFields();

    }

    protected BoardDescriptor(TxtFile txtFile, String boardID) {
        this.myUploadPort = new String();
        this.myProgrammer = Defaults.getDefaultUploadProtocol();
        this.myBoardID = boardID;
        this.myOptions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.myreferencingBoardsFile = txtFile.getTxtFile();
        this.myTxtFile = txtFile;
        setDefaultOptions();

        calculateDerivedFields();
    }

    public static BoardDescriptor makeBoardDescriptor(BoardDescriptor sourceBoardDescriptor) {
        return new InternalBoardDescriptor(sourceBoardDescriptor);
    }

    protected BoardDescriptor(BoardDescriptor sourceBoardDescriptor) {

        this.myUploadPort = sourceBoardDescriptor.getUploadPort();
        this.myProgrammer = sourceBoardDescriptor.getProgrammer();
        this.myBoardID = sourceBoardDescriptor.getBoardID();
        this.myOptions = sourceBoardDescriptor.getOptions();
        this.myProjectName = sourceBoardDescriptor.getProjectName();
        this.myreferencingBoardsFile = sourceBoardDescriptor.getReferencingBoardsFile();
        this.myTxtFile = sourceBoardDescriptor.myTxtFile;
        this.myChangeListener = null;

        this.myBoardsVariant = sourceBoardDescriptor.getBoardVariant();
        this.myReferencedBoardVariantPlatformPath = sourceBoardDescriptor.getReferencedVariantPlatformPath();
        this.myBoardsCore = sourceBoardDescriptor.getBoardsCore();
        this.myReferencedCorePlatformPath = sourceBoardDescriptor.getReferencedCorePlatformPath();
        this.myReferencedUploadToolPlatformPath = sourceBoardDescriptor.getReferencedUploadPlatformPath();
        this.myUploadTool = sourceBoardDescriptor.getuploadTool();
    }

    public String getuploadTool() {
        return this.myUploadTool;
    }

    private String getBoardsCore() {
        return this.myBoardsCore;
    }

    /*
	 * Sets default options as follows
	 * If no option is specified take the first one
     * if a option is specified but the value is invalid take the first one
     *
	 * this is so because I want to provide a list of options
	 * but if the options are incomplete or invalid
	 * this method still returns a complete and valid set.
     */
    private void setDefaultOptions() {

        TreeMap<String, String> allMenuIDs = this.myTxtFile.getMenus();
        for (Map.Entry<String, String> curMenuID : allMenuIDs.entrySet()) {
            String providedMenuValue = this.myOptions.get(curMenuID.getKey());
            String[] menuOptions = this.myTxtFile.getMenuItemIDsFromMenuID(curMenuID.getKey(), getBoardID());
            if (providedMenuValue == null) {
                if (menuOptions.length > 0) {
                    this.myOptions.put(curMenuID.getKey(), menuOptions[0]);
                }
            } else if (!Arrays.asList(menuOptions).contains(providedMenuValue)) {
                if (menuOptions.length > 0) {
                    this.myOptions.put(curMenuID.getKey(), menuOptions[0]);
                }
            }
        }
    }

    /**
     * tries to set the project to the boarddescriptor
     *
     * @param project
     * @param monitor
     * @return true if success false if failed
     */
    public boolean configureProject(IProject project, IProgressMonitor monitor) {
        ICProjectDescription prjCDesc = CoreModel.getDefault().getProjectDescription(project);
        ICConfigurationDescription configurationDescription = prjCDesc.getActiveConfiguration();
        try {
            save(configurationDescription);
            // prjCDesc.setActiveConfiguration(configurationDescription);
            CoreModel.getDefault().getProjectDescriptionManager().setProjectDescription(project, prjCDesc, true, null);
        } catch (Exception e) {
            e.printStackTrace();
            Common.log(new Status(IStatus.ERROR, io.sloeber.core.Activator.getId(), "failed to save the board settings",
                    e));
            return false;
        }
        return true;
    }

    /*
     * Method to create a project based on the board
     */
    public IProject createProject(String projectName, URI projectURI,
            ArrayList<ConfigurationDescriptor> cfgNamesAndTCIds, CodeDescriptor codeDescription,
            CompileOptions compileOptions, IProgressMonitor monitor) throws Exception {
        IProject projectHandle;
        projectHandle = ResourcesPlugin.getWorkspace().getRoot().getProject(Common.MakeNameCompileSafe(projectName));

        // try {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        final IProjectDescription desc = workspace.newProjectDescription(projectHandle.getName());
        desc.setLocationURI(projectURI);

        projectHandle.create(desc, monitor);

        if (monitor.isCanceled()) {
            throw new OperationCanceledException();
        }

        projectHandle.open(IResource.BACKGROUND_REFRESH, monitor);

        // Creates the .cproject file with the configurations
        ICProjectDescription prjCDesc = ShouldHaveBeenInCDT.setCProjectDescription(projectHandle, cfgNamesAndTCIds,
                true, compileOptions.isParallelBuildEnabled(), monitor);

        // Add the C C++ AVR and other needed Natures to the project
        Helpers.addTheNatures(desc);

        // Add the Arduino folder

        Helpers.createNewFolder(projectHandle, Const.ARDUINO_CODE_FOLDER_NAME, null);

        for (ConfigurationDescriptor curConfig : cfgNamesAndTCIds) {
            ICConfigurationDescription configurationDescription = prjCDesc.getConfigurationByName(curConfig.configName);
            compileOptions.save(configurationDescription);
            save(configurationDescription);

        }

        // Set the path variables
        // ArduinoHelpers.setProjectPathVariables(prjCDesc.getActiveConfiguration());

        // Intermediately save or the adding code will fail
        // Release is the active config (as that is the "IDE" Arduino
        // type....)
        ICConfigurationDescription defaultConfigDescription = prjCDesc
                .getConfigurationByName(cfgNamesAndTCIds.get(0).configName);

        ICResourceDescription cfgd = defaultConfigDescription.getResourceDescription(new Path(new String()), true);
        ICExclusionPatternPathEntry[] entries = cfgd.getConfiguration().getSourceEntries();
        if (entries.length == 1) {
            Path exclusionPath[] = new Path[6];
            exclusionPath[0] = new Path(LIBRARY_PATH_SUFFIX + "/?*/**/?xamples/**");
            exclusionPath[1] = new Path(LIBRARY_PATH_SUFFIX + "/?*/**/?xtras/**");
            exclusionPath[2] = new Path(LIBRARY_PATH_SUFFIX + "/?*/**/test*/**");
            exclusionPath[3] = new Path(LIBRARY_PATH_SUFFIX + "/?*/**/third-party/**");
            exclusionPath[4] = new Path(LIBRARY_PATH_SUFFIX + "/**/._*");
            exclusionPath[5] = new Path(LIBRARY_PATH_SUFFIX + "/?*/utility/*/*");

            ICExclusionPatternPathEntry newSourceEntry = new CSourceEntry(entries[0].getFullPath(), exclusionPath,
                    ICSettingEntry.VALUE_WORKSPACE_PATH);
            ICSourceEntry[] out = null;
            out = new ICSourceEntry[1];
            out[0] = (ICSourceEntry) newSourceEntry;
            try {
                cfgd.getConfiguration().setSourceEntries(out);
            } catch (@SuppressWarnings("unused") CoreException e) {
                // ignore
            }

        } else {
            // this should not happen
        }
        Set<String> librariesToInstall = codeDescription.createFiles(projectHandle, monitor);
        IPath linkedPath = codeDescription.getLinkedExamplePath();
        if (linkedPath != null) {
            Helpers.addIncludeFolder(defaultConfigDescription, linkedPath, false);
        }
        Libraries.addLibrariesToProject(projectHandle, defaultConfigDescription, librariesToInstall);
        prjCDesc.setActiveConfiguration(defaultConfigDescription);
        prjCDesc.setCdtProjectCreated();
        CoreModel.getDefault().getProjectDescriptionManager().setProjectDescription(projectHandle, prjCDesc, true,
                null);
        projectHandle.setDescription(desc, new NullProgressMonitor());
        projectHandle.refreshLocal(IResource.DEPTH_INFINITE, null);
        monitor.done();
        return projectHandle;
    }

    public void save(ICConfigurationDescription confdesc) throws Exception {
        boolean needsSettingDirty = saveConfiguration(confdesc, null);

        if (confdesc != null) {
            IProject project = confdesc.getProjectDescription().getProject();

            Helpers.setTheEnvironmentVariables(project, confdesc, (InternalBoardDescriptor) this);

            Helpers.addArduinoCodeToProject(this, project, confdesc);

            needsSettingDirty = needsSettingDirty || Helpers.removeInvalidIncludeFolders(confdesc);
            if (needsSettingDirty) {
                Helpers.setDirtyFlag(project, confdesc);
            } else {
                Common.log(new Status(IStatus.INFO, io.sloeber.core.Activator.getId(),
                        "Ignoring update; clean may be required: " + project.getName()));
            }
        }
    }

    public void saveConfiguration() {
        saveConfiguration(null, null);
    }

    public boolean saveConfiguration(ICConfigurationDescription confDesc, IContributedEnvironment contribEnvIn) {
        boolean needsSettingDirty = false;
        if (confDesc != null) {
            if (getActualCoreCodePath() == null) {
                // don't change stuff if the setup is wonky
                return false;
            }
            BoardDescriptor curBoardDesCriptor = makeBoardDescriptor(confDesc);
            needsSettingDirty = curBoardDesCriptor.needsSettingDirty(this);
            IContributedEnvironment contribEnv = contribEnvIn;
            if (contribEnv == null) {
                IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
                contribEnv = envManager.getContributedEnvironment();
            }
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, "JANTJE.SELECTED.PLATFORM",
                    getreferencingPlatformPath().toString());
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_BOARD_NAME, getBoardName());
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_BOARDS_FILE,
                    getReferencingBoardsFile().toString());
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_BOARD_ID, this.myBoardID);
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_ARCITECTURE_ID, getArchitecture());
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_PACKAGE_ID, getPackage());
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_UPLOAD_PORT, this.myUploadPort);
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_PROJECT_NAME,
                    confDesc.getProjectDescription().getProject().getName());
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_OS, this.myOSName);
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_WORKSPACE_LOCATION,
                    this.myWorkSpaceLocation);
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_ECLIPSE_LOCATION,
                    this.myWorkEclipseLocation);
            Common.setBuildEnvironmentVariable(confDesc, JANTJE_ACTION_UPLOAD, this.myProgrammer);
            String value = KeyValue.makeString(this.myOptions);
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_MENU_SELECTION, value);

            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_SERIAL_PORT, getActualUploadPort());
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_SERIAL_PORT_FILE,
                    getActualUploadPort().replace("/dev/", new String()));
			Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_BUILD_ACTUAL_CORE_PATH, getActualCoreCodePath().toOSString());
            IPath variantPath = getActualVariantPath();
            if (variantPath != null) {
                Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_BUILD_VARIANT_PATH,
                        variantPath.toOSString());
            } else {// teensy does not use variants
                Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_BUILD_VARIANT_PATH, new String());
            }

            // the entries below are only saved for special platforms that heavily rely on
            // referencing such as jantjes hardware

            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_REFERENCED_CORE_PLATFORM_PATH,
                    getReferencedCorePlatformPath().toOSString());
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_REFERENCED_VARIANT_PLATFORM_PATH,
                    getReferencedVariantPlatformPath().toOSString());
            Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_REFERENCED_UPLOAD_PLATFORM_PATH,
                    getReferencedUploadPlatformPath().toOSString());

        }

        // Also save last used values
        myStorageNode.put(KEY_LAST_USED_BOARDS_FILE, getReferencingBoardsFile().toString());
        myStorageNode.put(KEY_LAST_USED_BOARD, this.myBoardID);
        myStorageNode.put(KEY_LAST_USED_UPLOAD_PORT, this.myUploadPort);
        myStorageNode.put(KEY_LAST_USED_UPLOAD_PROTOCOL, this.myProgrammer);
        myStorageNode.put(KEY_LAST_USED_BOARD_MENU_OPTIONS, KeyValue.makeString(this.myOptions));
        return needsSettingDirty;
    }

    public String getPackage() {
        return this.myTxtFile.getPackage();
    }

    public String getArchitecture() {
        return this.myTxtFile.getArchitecture();
    }

    public File getReferencingBoardsFile() {
        return this.myreferencingBoardsFile;
    }

    public String getBoardName() {
        return this.myTxtFile.getNameFromID(this.myBoardID);
    }

    public String getUploadPort() {
        return this.myUploadPort;
    }

    /**
	 * return the actual adress en,coded in the upload port
	 * example
	 * uploadport com4 returns com4
	 * uploadport = arduino.local at 199.25.25.1 returns arduino.local
     * 
     * @return
     */
    public String getActualUploadPort() {
        return myUploadPort.split(" ")[0];
    }

    public String getProgrammer() {
        return this.myProgrammer;
    }

    /**
	 * Set the upload port like in the gui.
	 * The upload port can be a comport or a networkadress space and something else
	 * note that getuploadport returns the before space part of this method
     * @param newUploadPort
     */
    public void setUploadPort(String newUploadPort) {
        this.myUploadPort = newUploadPort;
    }

    public void setUploadProtocol(String newUploadProtocol) {
        this.myProgrammer = newUploadProtocol;

    }

    public void setBoardID(String boardID) {
        if (!boardID.equals(this.myBoardID)) {
            this.myBoardID = boardID;
            informChangeListeners();
        }
    }

    public void setBoardName(String boardName) {
        String newBoardID = this.myTxtFile.getBoardIDFromBoardName(boardName);
        if ((newBoardID == null || this.myBoardID.equals(newBoardID))) {
            return;
        }

        this.myBoardID = newBoardID;
        informChangeListeners();

    }

    public void setreferencingBoardsFile(File boardsFile) {
        if (boardsFile == null) {
            return;// ignore
        }
        if (this.myreferencingBoardsFile.equals(boardsFile)) {
            return;
        }

        this.myreferencingBoardsFile = boardsFile;
        this.myTxtFile = new TxtFile(this.myreferencingBoardsFile);
        informChangeListeners();
    }

    public void setOptions(Map<String, String> options) {
        if (options == null) {
            return;
        }
        this.myOptions.putAll(options);
        calculateDerivedFields();
    }

    /**
     * Returns the options for this board This reflects the options selected through
     * the menu functionality in the boards.txt
     *
     * @return a map of case insensitive ordered key value pairs
     */
    public Map<String, String> getOptions() {
        return this.myOptions;
    }

    public String getBoardID() {
        return this.myBoardID;
    }

    public String[] getCompatibleBoards() {
        return this.myTxtFile.getAllNames();
    }

    public String[] getUploadProtocols() {

        return Programmers.getUploadProtocols(this);

    }

    public String[] getMenuItemNamesFromMenuID(String menuID) {
        return this.myTxtFile.getMenuItemNamesFromMenuID(menuID, this.myBoardID);
    }

    public Set<String> getAllMenuNames() {
        return this.myTxtFile.getMenuNames();
    }

    public TreeMap<String, IPath> getAllExamples() {
        return LibraryManager.getAllExamples(this);
    }

    public void addChangeListener(ChangeListener l) {
        this.myChangeListener = l;
    }

    public void removeChangeListener() {
        this.myChangeListener = null;
    }

    private void informChangeListeners() {
        calculateDerivedFields();
        if (this.myChangeListener != null) {
            this.myChangeListener.stateChanged(null);
        }
    }

    public String getMenuIdFromMenuName(String menuName) {
        return this.myTxtFile.getMenuIDFromMenuName(menuName);
    }

    public String getMenuNameFromMenuID(String id) {
        return this.myTxtFile.getMenuNameFromID(id);
    }

    public String getMenuItemNamedFromMenuItemID(String menuItemID, String menuID) {
        return this.myTxtFile.getMenuItemNameFromMenuItemID(this.myBoardID, menuID, menuItemID);
    }

    public String getMenuItemIDFromMenuItemName(String menuItemName, String menuID) {
        return this.myTxtFile.getMenuItemIDFromMenuItemName(this.myBoardID, menuID, menuItemName);
    }

    public static String getUploadPort(IProject project) {
        return Common.getBuildEnvironmentVariable(project, ENV_KEY_JANTJE_UPLOAD_PORT, new String());
    }

    private String getMyOSName() {
        return this.myOSName;
    }

    private String getMyWorkSpaceLocation() {
        return this.myWorkSpaceLocation;
    }

    private String getMyWorkEclipseLocation() {
        return this.myWorkEclipseLocation;
    }

    public String getProjectName() {
        return this.myProjectName;
    }

    /**
     * provide the actual path to the variant. Use this method if you want to know
     * where the variant is
     *
     * @return the path to the variant; null if no variant is needed
     */
    public IPath getActualVariantPath() {
        if (getBoardVariant() == null) {
            return null;
        }
        IPath retPath = getReferencedVariantPlatformPath();
        if (retPath == null) {
            retPath = getreferencingPlatformPath();
        }
        return retPath.append(Const.VARIANTS_FOLDER_NAME).append(getBoardVariant());
    }

    private String getBoardVariant() {
        return this.myBoardsVariant;
    }

    public IPath getActualCoreCodePath() {
        IPath retPath = getReferencedCorePlatformPath();
        if (retPath == null) {
            retPath = getreferencingPlatformPath();
        }
        if (this.myBoardsCore == null) {
            return null;
        }
        return retPath.append("cores").append(this.myBoardsCore);
    }

    /**
	 * provide the actual path to the variant. Use this method if you want to know
     * where the variant is
     *
	 * @return the path to the variant
     */
    public IPath getReferencedCorePlatformPath() {
        if (myReferencedCorePlatformPath != null) {
            return myReferencedCorePlatformPath;
        }
        return getreferencingPlatformPath();
    }

    public IPath getReferencedUploadPlatformPath() {
        if (myReferencedUploadToolPlatformPath != null) {
            return myReferencedUploadToolPlatformPath;
        }
        return getreferencingPlatformPath();
    }

    public IPath getReferencedVariantPlatformPath() {
        if (myReferencedBoardVariantPlatformPath != null) {
            return myReferencedBoardVariantPlatformPath;
        }
        return getreferencingPlatformPath();
    }

    public File getReferencingPlatformFile() {
        return getreferencingPlatformPath().append(PLATFORM_FILE_NAME).toFile();
    }

    public Path getreferencingPlatformPath() {
        try {
            return new Path(this.myreferencingBoardsFile.getParent());
        } catch (@SuppressWarnings("unused") Exception e) {
            return new Path(new String());
        }
    }

    public File getreferencedPlatformFile() {
        if (this.myReferencedCorePlatformPath == null) {
            return null;
        }
        return this.myReferencedCorePlatformPath.append(PLATFORM_FILE_NAME).toFile();
    }

    public IPath getReferencedLibraryPath() {
        if (this.myReferencedCorePlatformPath == null) {
            return null;
        }
        return this.myReferencedCorePlatformPath.append(LIBRARY_PATH_SUFFIX);
    }

    public IPath getReferencingLibraryPath() {
        return this.getreferencingPlatformPath().append(LIBRARY_PATH_SUFFIX);
    }

    public String getUploadCommand(ICConfigurationDescription confdesc) {
        String upLoadTool = getActualUploadTool(confdesc);
        String action = "UPLOAD";
        if (usesProgrammer()) {
            action = "PROGRAM";
        }
        String networkPrefix = "";
        if (isNetworkUpload()) {
            networkPrefix = "NETWORK_";
        }
		String key = "A.TOOLS." + upLoadTool.toUpperCase() + "."
				+ action + "." + networkPrefix+"PATTERN";
        String ret = Common.getBuildEnvironmentVariable(confdesc, key, "");
        if (ret.isEmpty()) {
			Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
					key + " : not found in the platform.txt file"));
        }
        return ret;
    }

    public String getActualUploadTool(ICConfigurationDescription confdesc) {
        if (confdesc == null) {
            Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "Confdesc null is not alowed here"));
            return this.myUploadTool;
        }
        if (usesProgrammer()) {
            return Common.getBuildEnvironmentVariable(confdesc, "A.PROGRAM.TOOL",
                    "Program tool not properly configured");
        }

        if (this.myUploadTool == null) {
            return Common.getBuildEnvironmentVariable(confdesc, "A.UPLOAD.TOOL", "upload tool not properly configured");
        }

        return this.myUploadTool;
    }

    public boolean usesProgrammer() {
        return !this.myProgrammer.equals(Defaults.getDefaultUploadProtocol());
    }

    public IPath getreferencedHardwarePath() {
        IPath platformPath = getReferencedCorePlatformPath();
        return platformPath.removeLastSegments(1);
    }

    /*
	 * get the latest installed arduino platform with the same architecture.
	 * This is the platform to use the programmers.txt if no other programmers.txt
	 * are found.
     */
    public IPath getArduinoPlatformPath() {
        return InternalPackageManager.getPlatformInstallPath("arduino", getArchitecture());
    }

    /**
     * If the upload port contains a space everything before the first space is
     * considered to be a dns name or ip adress.
     *
     * @param mComPort
	 * @return null if no space in uploadport
	 *         return dns name op ipadress
     */
    public String getHost() {
        String host = myUploadPort.split(" ")[0];
        if (host.equals(myUploadPort))
            return null;
        return host;
    }

    /**
     * true if this board needs a networkUpload else false
     * 
     * @return
     */
    public boolean isNetworkUpload() {
        return getHost() != null;
    }

}
