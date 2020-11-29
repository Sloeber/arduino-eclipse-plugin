package io.sloeber.core.api;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.cdt.core.parser.util.StringUtil;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import io.sloeber.core.Activator;
import io.sloeber.core.Messages;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.Const;
import io.sloeber.core.managers.ArduinoPlatform;
import io.sloeber.core.managers.InternalPackageManager;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.KeyValue;
import io.sloeber.core.txt.BoardTxtFile;
import io.sloeber.core.txt.KeyValueTree;
import io.sloeber.core.txt.PlatformTxtFile;
import io.sloeber.core.txt.Programmers;
import io.sloeber.core.txt.TxtFile;

public class BoardDescription extends Common {
    // Important constants to avoid having to add the class
    private static final String TOOL_ID = Messages.TOOL;
    private static final String BOARD_ID = Messages.BOARD;
    private static final String FILE_ID = Messages.FILE;
    private static final String VendorArduino = Const.ARDUINO;

    /*
     * Some constants
     */
    private static final String REFERENCED = "referenced"; //$NON-NLS-1$
    private static final String KEY_LAST_USED_BOARD = "Last used Board"; //$NON-NLS-1$
    private static final String KEY_LAST_USED_UPLOAD_PORT = "Last Used Upload port"; //$NON-NLS-1$
    private static final String KEY_LAST_USED_UPLOAD_PROTOCOL = "last Used upload Protocol"; //$NON-NLS-1$
    private static final String KEY_LAST_USED_BOARDS_FILE = "Last used Boards file"; //$NON-NLS-1$
    private static final String KEY_LAST_USED_BOARD_MENU_OPTIONS = "last used Board custom option selections"; //$NON-NLS-1$
    private static final String ENV_KEY_JANTJE_MENU_SELECTION = ENV_KEY_JANTJE_START + Const.MENU;
    private static final String ENV_KEY_JANTJE_UPLOAD_PORT = ENV_KEY_JANTJE_START + Const.COM_PORT;
    private static final String ENV_KEY_JANTJE_BOARD_NAME = ENV_KEY_JANTJE_START + "board_name"; //$NON-NLS-1$
    private static final String ENV_KEY_JANTJE_PROJECT_NAME = ENV_KEY_JANTJE_START + "project_name"; //$NON-NLS-1$
    private static final String ENV_KEY_JANTJE_OS = ENV_KEY_JANTJE_START + "os_name"; //$NON-NLS-1$
    private static final String ENV_KEY_JANTJE_WORKSPACE_LOCATION = ENV_KEY_JANTJE_START + "workspace_location"; //$NON-NLS-1$
    private static final String ENV_KEY_JANTJE_ECLIPSE_LOCATION = ENV_KEY_JANTJE_START + "eclipse_location"; //$NON-NLS-1$
    private static final String ENV_KEY_JANTJE_BOARDS_FILE = ENV_KEY_JANTJE_START + "boards_file"; //$NON-NLS-1$
    private static final String ENV_KEY_JANTJE_PACKAGE_ID = ENV_KEY_JANTJE_START + "package_ID"; //$NON-NLS-1$
    private static final String ENV_KEY_JANTJE_ARCITECTURE_ID = ENV_KEY_JANTJE_START + "architecture_ID"; //$NON-NLS-1$
    private static final String ENV_KEY_JANTJE_BOARD_ID = ENV_KEY_JANTJE_START + "board_ID"; //$NON-NLS-1$
    private static final String ENV_KEY_SERIAL_PORT = ERASE_START + "serial_port"; //$NON-NLS-1$
    private static final String ENV_KEY_SERIAL_PORT_FILE = ERASE_START + "serial.port.file"; //$NON-NLS-1$
    private static final String ENV_KEY_BUILD_VARIANT_PATH = ERASE_START + BUILD + DOT + VARIANT + DOT + PATH;
    private static final String ENV_KEY_BUILD_ACTUAL_CORE_PATH = ERASE_START + BUILD + DOT + CORE + DOT + PATH;
    private static final String ENV_KEY_BUILD_ARCH = ERASE_START + "build.arch"; //$NON-NLS-1$
    private static final String ENV_KEY_HARDWARE_PATH = ERASE_START + "runtime.hardware.path"; //$NON-NLS-1$
    private static final String ENV_KEY_PLATFORM_PATH = ERASE_START + "runtime.platform.path"; //$NON-NLS-1$
    private static final String ENV_KEY_REFERENCED_CORE_PLATFORM_PATH = ERASE_START + REFERENCED + DOT + CORE + DOT
            + PATH;
    private static final String ENV_KEY_REFERENCED_VARIANT_PLATFORM_PATH = ERASE_START + REFERENCED + DOT + VARIANT
            + DOT + PATH;
    private static final String ENV_KEY_REFERENCED_UPLOAD_PLATFORM_PATH = ERASE_START + REFERENCED + DOT + UPLOAD
            + PATH;

    // preference nodes
    private static final String NODE_ARDUINO = Activator.NODE_ARDUINO;
    private static final String JANTJE_ACTION_UPLOAD = ENV_KEY_JANTJE_START + UPLOAD; // this is actually the programmer
    private static final IEclipsePreferences myStorageNode = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
    private static final TxtFile pluginPreProcessingPlatformTxt = new TxtFile(
            ConfigurationPreferences.getPreProcessingPlatformFile());
    private static final TxtFile pluginPostProcessingPlatformTxt = new TxtFile(
            ConfigurationPreferences.getPostProcessingPlatformFile());

    /*
     * This is the basic info contained in the descriptor
     */
    private String myUploadPort;
    private String myProgrammer;
    private String myBoardID;
    private Map<String, String> myOptions;

    /*
     * Stuff to make things work
     */
    private File myreferencingBoardsFile;
    protected BoardTxtFile myTxtFile;

    private String myBoardsVariant;
    private IPath myReferencedBoardVariantPlatformPath;
    private String myBoardsCore;
    private IPath myReferencedCorePlatformPath;
    private IPath myReferencedUploadToolPlatformPath;
    private String myUploadTool;
    private boolean isDirty = true;
    private final String ENV_KEY_JANTJE_PROGRAMMER = "SLOEBER.PROGRAMMER.NAME"; //$NON-NLS-1$
    private final String ENV_KEY_JANTJE_BOARD_TXT = "SLOEBER.PRODUCT.NAME"; //$NON-NLS-1$


    @Override
    public String toString() {
        return getReferencingBoardsFile() + " \"" + getBoardName() + "\" " + getUploadPort(); //$NON-NLS-1$//$NON-NLS-2$
    }

    /**
     * Compare 2 descriptors and return true is they are equal. This method detects
     * - OS changes - project name changes - moves of workspace - changed runtine
     * eclipse install
     *
     * @param obj
     * @return true if equal otherwise false
     */
    public boolean equals(BoardDescription otherBoardDescriptor) {
        if (!this.getUploadPort().equals(otherBoardDescriptor.getUploadPort())) {
            return false;
        }
        if (!this.getProgrammer().equals(otherBoardDescriptor.getProgrammer())) {
            return false;
        }
        return !needsRebuild(otherBoardDescriptor);
    }

    /**
     * compare 2 board descriptors and return true if replacing one board descriptor
     * with the other implies that a rebuild is needed
     *
     * @param otherBoardDescriptor
     * @return
     */
    public boolean needsRebuild(BoardDescription otherBoardDescriptor) {

        if (!this.getBoardID().equals(otherBoardDescriptor.getBoardID())) {
            return true;
        }
        if (!this.getReferencingBoardsFile().equals(otherBoardDescriptor.getReferencingBoardsFile())) {
            return true;
        }
        if (!this.getOptions().equals(otherBoardDescriptor.getOptions())) {
            return true;
        }
        return false;
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
        ParseSection();

    }

    private void ParseSection() {
        KeyValueTree rootData = myTxtFile.getData();
        String boardID = getBoardID();
        KeyValueTree boardData = rootData.getChild(boardID);

        String core = boardData.getValue(BUILD + DOT + CORE);
        String variant = boardData.getValue(BUILD + DOT + VARIANT);
        String upload = boardData.getValue(UPLOAD + DOT + TOOL);
        // also search the options
        for (Entry<String, String> curOption : this.myOptions.entrySet()) {
            KeyValueTree curMenuData = boardData.getChild(MENU + DOT + curOption.getKey() + DOT + curOption.getValue());
            String coreOption = curMenuData.getValue(BUILD + DOT + CORE);
            String variantOption = curMenuData.getValue(BUILD + DOT + VARIANT);
            String uploadOption = curMenuData.getValue(UPLOAD + DOT + TOOL);
            if (!coreOption.isEmpty()) {
                core = coreOption;
            }
            if (!variantOption.isEmpty()) {
                variant = variantOption;
            }
            if (!uploadOption.isEmpty()) {
                upload = uploadOption;
            }
        }
        String architecture = getArchitecture();
        if (core != null) {
            String valueSplit[] = core.split(COLON);
            if (valueSplit.length == 2) {
                String refVendor = valueSplit[0];
                String actualValue = valueSplit[1];
                myBoardsCore = actualValue;
                myReferencedCorePlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor, architecture);
                if (this.myReferencedCorePlatformPath == null) {
                    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                            Messages.Helpers_tool_reference_missing.replace(TOOL_ID, core)
                                    .replace(FILE_ID, getReferencingBoardsFile().toString())
                                    .replace(BOARD_ID, getBoardID())));
                    return;
                }
            } else if (valueSplit.length == 4) {
                String refVendor = valueSplit[0];
                String refArchitecture = valueSplit[1];
                String refVersion = valueSplit[2];
                String actualValue = valueSplit[3];
                myBoardsCore = actualValue;
                myReferencedCorePlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor, refArchitecture,
                        refVersion);
                if (this.myReferencedCorePlatformPath == null) {
                    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                            Messages.Helpers_tool_reference_missing.replace(TOOL_ID, core)
                                    .replace(FILE_ID, getReferencingBoardsFile().toString())
                                    .replace(BOARD_ID, getBoardID())));
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
                this.myReferencedBoardVariantPlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor,
                        architecture);
                if (this.myReferencedBoardVariantPlatformPath == null) {
                    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                            Messages.Helpers_tool_reference_missing.replace(TOOL_ID, variant)
                                    .replace(FILE_ID, getReferencingBoardsFile().toString())
                                    .replace(BOARD_ID, getBoardID())));
                    return;
                }
            } else if (valueSplit.length == 4) {
                String refVendor = valueSplit[0];
                String refArchitecture = valueSplit[1];
                String refVersion = valueSplit[2];
                String actualValue = valueSplit[3];
                this.myBoardsVariant = actualValue;
                if ("*".equals(refVersion)) { //$NON-NLS-1$
                    this.myReferencedBoardVariantPlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor,
                            refArchitecture);
                } else {
                    this.myReferencedBoardVariantPlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor,
                            refArchitecture, refVersion);
                }
                if (this.myReferencedBoardVariantPlatformPath == null) {
                    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                            Messages.Helpers_tool_reference_missing.replace(TOOL_ID, variant)
                                    .replace(FILE_ID, getReferencingBoardsFile().toString())
                                    .replace(BOARD_ID, getBoardID())));
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
                this.myReferencedUploadToolPlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor,
                        architecture);
                if (this.myReferencedUploadToolPlatformPath == null) {
                    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                            Messages.Helpers_tool_reference_missing.replace(TOOL_ID, upload)
                                    .replace(FILE_ID, getReferencingBoardsFile().toString())
                                    .replace(BOARD_ID, getBoardID())));
                    return;
                }
            } else if (valueSplit.length == 4) {
                String refVendor = valueSplit[0];
                String refArchitecture = valueSplit[1];
                String refVersion = valueSplit[2];
                String actualValue = valueSplit[3];
                this.myUploadTool = actualValue;
                this.myReferencedUploadToolPlatformPath = InternalPackageManager.getPlatformInstallPath(refVendor,
                        refArchitecture, refVersion);
                if (this.myReferencedUploadToolPlatformPath == null) {
                    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
                            Messages.Helpers_tool_reference_missing.replace(TOOL_ID, upload)
                                    .replace(FILE_ID, getReferencingBoardsFile().toString())
                                    .replace(BOARD_ID, getBoardID())));
                    return;
                }
            } else {
                myUploadTool = upload;
            }
        }
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
    public BoardDescription(ICConfigurationDescription confdesc) {
        if (confdesc == null) {
            myreferencingBoardsFile = new File(myStorageNode.get(KEY_LAST_USED_BOARDS_FILE, EMPTY));
            myTxtFile = new BoardTxtFile(this.myreferencingBoardsFile);
            myBoardID = myStorageNode.get(KEY_LAST_USED_BOARD, EMPTY);
            myUploadPort = myStorageNode.get(KEY_LAST_USED_UPLOAD_PORT, EMPTY);
            myProgrammer = myStorageNode.get(KEY_LAST_USED_UPLOAD_PROTOCOL, Defaults.getDefaultUploadProtocol());
            myOptions = KeyValue.makeMap(myStorageNode.get(KEY_LAST_USED_BOARD_MENU_OPTIONS, EMPTY));

        } else {
            myUploadPort = getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_UPLOAD_PORT, EMPTY);
            myProgrammer = getBuildEnvironmentVariable(confdesc, JANTJE_ACTION_UPLOAD, EMPTY);
            myreferencingBoardsFile = new File(
                    getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_BOARDS_FILE, EMPTY));
            myBoardID = getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_BOARD_ID, EMPTY);
            myTxtFile = new BoardTxtFile(this.myreferencingBoardsFile);
            String optinconcat = getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_MENU_SELECTION, EMPTY);
            myOptions = KeyValue.makeMap(optinconcat);
        }
        setDirty();
    }


    /**
     * make a board descriptor for each board in the board.txt file with the default
     * options
     *
     * @param boardFile
     * @return a list of board descriptors
     */
    public static List<BoardDescription> makeBoardDescriptors(File boardFile, Map<String, String> options) {
        BoardTxtFile txtFile = new BoardTxtFile(boardFile);
        List<BoardDescription> boards = new ArrayList<>();
        String[] allSectionNames = txtFile.getAllSectionNames();
        for (String curboardName : allSectionNames) {
            Map<String, String> boardSection = txtFile.getSection(txtFile.getIDFromNiceName(curboardName));
            if (boardSection != null) {
                if (!"true".equalsIgnoreCase(boardSection.get("hide"))) { //$NON-NLS-1$ //$NON-NLS-2$
                    boards.add(new BoardDescription(boardFile, txtFile.getIDFromNiceName(curboardName), options));
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
    BoardDescription(File boardsFile, String boardID, Map<String, String> options) {
        this.myUploadPort = EMPTY;
        this.myProgrammer = Defaults.getDefaultUploadProtocol();
        this.myBoardID = boardID;
        this.myOptions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.myreferencingBoardsFile = boardsFile;
        this.myTxtFile = new BoardTxtFile(this.myreferencingBoardsFile);
        setDefaultOptions();
        if (options != null) {
            this.myOptions.putAll(options);
        }
    }

    protected BoardDescription(BoardTxtFile txtFile, String boardID) {
        this.myUploadPort = EMPTY;
        this.myProgrammer = Defaults.getDefaultUploadProtocol();
        this.myBoardID = boardID;
        this.myOptions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        this.myreferencingBoardsFile = txtFile.getTxtFile();
        this.myTxtFile = txtFile;
        setDefaultOptions();
        calculateDerivedFields();
    }

    protected BoardDescription(BoardDescription sourceBoardDescriptor) {

        this.myUploadPort = sourceBoardDescriptor.getUploadPort();
        this.myProgrammer = sourceBoardDescriptor.getProgrammer();
        this.myBoardID = sourceBoardDescriptor.getBoardID();
        this.myOptions = sourceBoardDescriptor.getOptions();
        this.myreferencingBoardsFile = sourceBoardDescriptor.getReferencingBoardsFile();
        this.myTxtFile = sourceBoardDescriptor.myTxtFile;
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
        updateWhenDirty();
        return this.myBoardsCore;
    }

    /*
     * Sets default options as follows If no option is specified take the first one
     * if a option is specified but the value is invalid take the first one
     *
     * this is so because I want to provide a list of options but if the options are
     * incomplete or invalid this method still returns a complete and valid set.
     */
    private void setDefaultOptions() {
        Map<String, String> allMenuIDs = this.myTxtFile.getMenus();
        for (Map.Entry<String, String> curMenuID : allMenuIDs.entrySet()) {
            String providedMenuValue = this.myOptions.get(curMenuID.getKey());
            ArrayList<String> menuOptions = this.myTxtFile.getMenuItemIDsFromMenuID(curMenuID.getKey(), getBoardID());
            if (menuOptions.size() > 0) {
                if (providedMenuValue == null) {

                    this.myOptions.put(curMenuID.getKey(), menuOptions.get(0));
                } else if (!menuOptions.contains(providedMenuValue)) {
                    this.myOptions.put(curMenuID.getKey(), menuOptions.get(0));
                }
            }
        }
    }




    /**
     * Store the selections the user made so we can reuse them when creating a new
     * project
     */
    public void saveUserSelection() {
        myStorageNode.put(KEY_LAST_USED_BOARDS_FILE, getReferencingBoardsFile().toString());
        myStorageNode.put(KEY_LAST_USED_BOARD, this.myBoardID);
        myStorageNode.put(KEY_LAST_USED_UPLOAD_PORT, this.myUploadPort);
        myStorageNode.put(KEY_LAST_USED_UPLOAD_PROTOCOL, this.myProgrammer);
        myStorageNode.put(KEY_LAST_USED_BOARD_MENU_OPTIONS, KeyValue.makeString(this.myOptions));
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
        return this.myTxtFile.getNiceNameFromID(this.myBoardID);
    }

    public String getUploadPort() {
        return this.myUploadPort;
    }

    /**
     * return the actual adress en,coded in the upload port example uploadport com4
     * returns com4 uploadport = arduino.local at 199.25.25.1 returns arduino.local
     * 
     * @return
     */
    public String getActualUploadPort() {
        return myUploadPort.split(" ")[0]; //$NON-NLS-1$
    }

    public String getProgrammer() {
        return this.myProgrammer;
    }

    /**
     * Set the upload port like in the gui. The upload port can be a comport or a
     * networkadress space and something else note that getuploadport returns the
     * before space part of this method
     * 
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
            setDirty();
        }
    }

    private void setDirty() {
        isDirty = true;

    }

    public void setBoardName(String boardName) {
        String newBoardID = this.myTxtFile.getIDFromNiceName(boardName);
        if ((newBoardID == null || this.myBoardID.equals(newBoardID))) {
            return;
        }
        this.myBoardID = newBoardID;
        setDirty();

    }

    public void setreferencingBoardsFile(File boardsFile) {
        if (boardsFile == null) {
            return;// ignore
        }
        if (this.myreferencingBoardsFile.equals(boardsFile)) {
            return;
        }

        this.myreferencingBoardsFile = boardsFile;
        this.myTxtFile = new BoardTxtFile(this.myreferencingBoardsFile);
        setDirty();
    }

    public void setOptions(Map<String, String> options) {
        if (options == null) {
            return;
        }
        this.myOptions.putAll(options);
        setDirty();
    }

    /**
     * Returns the options for this board This reflects the options selected through
     * the menu functionality in the boards.txt
     *
     * @return a map of case insensitive ordered key value pairs
     */
    public Map<String, String> getOptions() {
        // no update is needed as this field is only modified at construction or set
        return this.myOptions;
    }

    private void updateWhenDirty() {
        if (isDirty) {
            calculateDerivedFields();
            isDirty = false;
        }
    }

    public String getBoardID() {
        // no update is needed as this field is only modified at construction or set
        return this.myBoardID;
    }

    public String[] getCompatibleBoards() {
        return this.myTxtFile.getAllSectionNames();
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
        updateWhenDirty();
        return LibraryManager.getAllExamples(this);
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
        return Common.getBuildEnvironmentVariable(project, ENV_KEY_JANTJE_UPLOAD_PORT, EMPTY);
    }


    /**
     * provide the actual path to the variant. Use this method if you want to know
     * where the variant is
     *
     * @return the path to the variant; null if no variant is needed
     */
    public IPath getActualVariantPath() {
        updateWhenDirty();
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
        updateWhenDirty();
        return this.myBoardsVariant;
    }

    public IPath getActualCoreCodePath() {
        updateWhenDirty();
        IPath retPath = getReferencedCorePlatformPath();
        if (retPath == null) {
            retPath = getreferencingPlatformPath();
        }
        if (this.myBoardsCore == null) {
            return null;
        }
        return retPath.append(Const.CORES).append(this.myBoardsCore);
    }

    /**
     * provide the actual path to the variant. Use this method if you want to know
     * where the variant is
     *
     * @return the path to the variant
     */
    public IPath getReferencedCorePlatformPath() {
        updateWhenDirty();
        if (myReferencedCorePlatformPath != null) {
            return myReferencedCorePlatformPath;
        }
        return getreferencingPlatformPath();
    }

    public IPath getReferencedUploadPlatformPath() {
        updateWhenDirty();
        if (myReferencedUploadToolPlatformPath != null) {
            return myReferencedUploadToolPlatformPath;
        }
        return getreferencingPlatformPath();
    }

    public IPath getReferencedVariantPlatformPath() {
        updateWhenDirty();
        if (myReferencedBoardVariantPlatformPath != null) {
            return myReferencedBoardVariantPlatformPath;
        }
        return getreferencingPlatformPath();
    }

    public PlatformTxtFile getReferencingPlatformFile() {
        updateWhenDirty();
        File platformFile = getreferencingPlatformPath().append(Const.PLATFORM_FILE_NAME).toFile();
        if (platformFile != null && platformFile.exists()) {
            return new PlatformTxtFile(platformFile);
        }
        return null;
    }

    public Path getreferencingPlatformPath() {
        try {
            return new Path(this.myreferencingBoardsFile.getParent());
        } catch (@SuppressWarnings("unused") Exception e) {
            return new Path(EMPTY);
        }
    }

    public PlatformTxtFile getreferencedPlatformFile() {
        updateWhenDirty();
        if (this.myReferencedCorePlatformPath == null) {
            return null;
        }
        File platformFile = myReferencedCorePlatformPath.append(Const.PLATFORM_FILE_NAME).toFile();
        if (platformFile != null && platformFile.exists()) {
            return new PlatformTxtFile(platformFile);
        }
        return null;
    }

    public IPath getReferencedLibraryPath() {
        updateWhenDirty();
        if (this.myReferencedCorePlatformPath == null) {
            return null;
        }
        return this.myReferencedCorePlatformPath.append(LIBRARY_PATH_SUFFIX);
    }

    public IPath getReferencingLibraryPath() {
        updateWhenDirty();
        return this.getreferencingPlatformPath().append(LIBRARY_PATH_SUFFIX);
    }

    public String getUploadCommand(ICConfigurationDescription confdesc) {
        updateWhenDirty();
        String upLoadTool = getActualUploadTool(confdesc);
        String action = Const.UPLOAD;
        if (usesProgrammer()) {
            action = Const.PROGRAM;
        }
        String networkPrefix = EMPTY;
        if (isNetworkUpload()) {
            networkPrefix = DOT + Const.NETWORK_PREFIX;
        }
        String key = Const.A_TOOLS + upLoadTool + DOT + action + networkPrefix + DOT + Const.PATTERN;
        String ret = Common.getBuildEnvironmentVariable(confdesc, key, EMPTY);
        if (ret.isEmpty()) {
            Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, key + " : not found in the platform.txt file")); //$NON-NLS-1$
        }
        return ret;
    }

    public String getActualUploadTool(ICConfigurationDescription confdesc) {
        updateWhenDirty();
        if (confdesc == null) {
            Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "Confdesc null is not alowed here")); //$NON-NLS-1$
            return this.myUploadTool;
        }
        if (usesProgrammer()) {
            return Common.getBuildEnvironmentVariable(confdesc, Const.PROGRAM_TOOL,
                    "Program tool not properly configured"); //$NON-NLS-1$
        }

        if (this.myUploadTool == null) {
            return Common.getBuildEnvironmentVariable(confdesc, Const.UPLOAD_TOOL,
                    "upload tool not properly configured"); //$NON-NLS-1$
        }
        return this.myUploadTool;

    }

    public boolean usesProgrammer() {
        updateWhenDirty();
        return !this.myProgrammer.equals(Defaults.getDefaultUploadProtocol());
    }

    public IPath getreferencedHardwarePath() {
        updateWhenDirty();
        IPath platformPath = getReferencedCorePlatformPath();
        return platformPath.removeLastSegments(1);
    }

    /*
     * get the latest installed arduino platform with the same architecture. This is
     * the platform to use the programmers.txt if no other programmers.txt are
     * found.
     */
    public IPath getArduinoPlatformPath() {
        updateWhenDirty();
        return InternalPackageManager.getPlatformInstallPath(VendorArduino, getArchitecture());
    }

    /**
     * If the upload port contains a space everything before the first space is
     * considered to be a dns name or ip adress.
     *
     * @param mComPort
     * @return null if no space in uploadport return dns name op ipadress
     */
    public String getHost() {
        String host = myUploadPort.split(" ")[0]; //$NON-NLS-1$
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

    private Map<String, String> getEnvVarsTxt() {
        return myTxtFile.getAllBoardEnvironVars(getBoardID());
    }

    BoardDescription(TxtFile configFile) {
        KeyValueTree tree = configFile.getData();
        this.myUploadPort = EMPTY;
        this.myProgrammer = tree.getValue(ENV_KEY_JANTJE_PROGRAMMER);
        this.myBoardID = tree.getValue(ENV_KEY_JANTJE_BOARD_ID);
        myreferencingBoardsFile = new File(tree.getValue(ENV_KEY_JANTJE_BOARD_TXT));
        this.myTxtFile = new BoardTxtFile(this.myreferencingBoardsFile);

        KeyValueTree optionsTree = tree.getChild(ENV_KEY_JANTJE_MENU_SELECTION);
        Map<String, String> options = optionsTree.toKeyValues(EMPTY, false);
        this.myOptions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        setDefaultOptions();
        if (options != null) {
            this.myOptions.putAll(options);
        }
    }
    public Map<String, String> getEnvVarsConfig() {
        Map<String, String> allVars = new TreeMap<>();
        allVars.put(ENV_KEY_JANTJE_PROGRAMMER, this.myProgrammer);
        allVars.put(ENV_KEY_JANTJE_BOARD_ID, this.myBoardID);
        allVars.put(ENV_KEY_JANTJE_BOARD_TXT, myreferencingBoardsFile.toString());
        // allVars.put(ENV_KEY_JANTJE_PRODUCT_VERSION, EMPTY);

        for (Entry<String, String> curOption : this.myOptions.entrySet()) {
            allVars.put(ENV_KEY_JANTJE_MENU_SELECTION + DOT + curOption.getKey(), curOption.getValue());
        }
        return allVars;
    }

    public BoardDescription() {
        // TODO Auto-generated constructor stub
    }

    public Map<String, String> getEnvVars() {
        updateWhenDirty();

        BoardDescription pluginPreProcessingBoardsTxt = new BoardDescription(
                new BoardTxtFile(ConfigurationPreferences.getPreProcessingBoardsFile()), getBoardID());
        BoardDescription pluginPostProcessingBoardsTxt = new BoardDescription(
                new BoardTxtFile(ConfigurationPreferences.getPostProcessingBoardsFile()), getBoardID());

        Map<String, String> allVars = pluginPreProcessingPlatformTxt.getAllEnvironVars(EMPTY);
        allVars.putAll(pluginPreProcessingBoardsTxt.getEnvVarsTxt());


        String architecture = getArchitecture();
        allVars.put(ENV_KEY_BUILD_ARCH, architecture.toUpperCase());
        allVars.put(ENV_KEY_HARDWARE_PATH, getreferencedHardwarePath().toOSString());
        allVars.put(ENV_KEY_PLATFORM_PATH, getreferencingPlatformPath().toOSString());




        allVars.putAll(getEnvVarsConfig());
        allVars.put(ENV_KEY_JANTJE_BOARD_NAME, getBoardName());
        allVars.put(ENV_KEY_JANTJE_BOARDS_FILE, getReferencingBoardsFile().toString());
        allVars.put(ENV_KEY_JANTJE_ARCITECTURE_ID, getArchitecture());
        allVars.put(ENV_KEY_JANTJE_PACKAGE_ID, getPackage());
        allVars.put(ENV_KEY_JANTJE_UPLOAD_PORT, this.myUploadPort);
        allVars.put(JANTJE_ACTION_UPLOAD, this.myProgrammer);

        allVars.put(ENV_KEY_SERIAL_PORT, getActualUploadPort());
        allVars.put(ENV_KEY_SERIAL_PORT_FILE, getActualUploadPort().replace("/dev/", EMPTY)); //$NON-NLS-1$
        // if actual core path is osstring regression test issue555 willl fail teensy
        // stuff
        allVars.put(ENV_KEY_BUILD_ACTUAL_CORE_PATH, getActualCoreCodePath().toString());
        IPath variantPath = getActualVariantPath();
        if (variantPath != null) {
            allVars.put(ENV_KEY_BUILD_VARIANT_PATH, variantPath.toOSString());
        } else {// teensy does not use variants
            allVars.put(ENV_KEY_BUILD_VARIANT_PATH, EMPTY);
        }

        // the entries below are only saved for special platforms that heavily rely on
        // referencing such as jantjes hardware

        allVars.put(ENV_KEY_REFERENCED_CORE_PLATFORM_PATH, getReferencedCorePlatformPath().toOSString());
        allVars.put(ENV_KEY_REFERENCED_VARIANT_PLATFORM_PATH, getReferencedVariantPlatformPath().toOSString());
        allVars.put(ENV_KEY_REFERENCED_UPLOAD_PLATFORM_PATH, getReferencedUploadPlatformPath().toOSString());

        PlatformTxtFile referencedPlatfromFile = getreferencedPlatformFile();
        // process the platform file referenced by the boards.txt
        if (referencedPlatfromFile != null) {
            allVars.putAll(referencedPlatfromFile.getAllEnvironVars());
        }
        PlatformTxtFile referencingPlatfromFile = getReferencingPlatformFile();
        // process the platform file next to the selected boards.txt
        if (referencingPlatfromFile != null) {
            allVars.putAll(referencingPlatfromFile.getAllEnvironVars());
        }

        allVars.putAll(getEnVarPlatformInfo());

        Programmers localProgrammers[] = Programmers.fromBoards(this);
        String programmer = getProgrammer();
        for (Programmers curProgrammer : localProgrammers) {
            String programmerID = curProgrammer.getIDFromNiceName(programmer);
            if (programmerID != null) {
                allVars.putAll(curProgrammer.getAllEnvironVars(programmerID));
            }
        }

        // boards setiings not comming from menu selections
        allVars.putAll(myTxtFile.getAllBoardEnvironVars(getBoardID()));

        // board settings from menu selections
        Map<String, String> options = getOptions();
        KeyValueTree rootData = myTxtFile.getData();
        KeyValueTree menuData = rootData.getChild(getBoardID() + DOT + MENU);
        for (Entry<String, String> curOption : options.entrySet()) {
            String menuID = curOption.getKey();
            String SelectedMenuItemID = curOption.getValue();
            KeyValueTree curSelectedMenuItem = menuData.getChild(menuID + DOT + SelectedMenuItemID);
            allVars.putAll(curSelectedMenuItem.toKeyValues(ERASE_START, false));
        }

        // add the stuff that comes with the plugin that is marked as post
        allVars.putAll(pluginPostProcessingPlatformTxt.getAllEnvironVars(EMPTY));
        allVars.putAll(pluginPostProcessingBoardsTxt.getEnvVarsTxt());

        // Do some coded post processing
        allVars.putAll(getEnvVarsPostProcessing(allVars));
        return allVars;

    }

    public static String getBoardsFile(ICConfigurationDescription confDesc) {
        return getBuildEnvironmentVariable(confDesc, ENV_KEY_JANTJE_BOARDS_FILE, EMPTY);
    }

    public static String getVariant(ICConfigurationDescription confDesc) {
        return getBuildEnvironmentVariable(confDesc, ENV_KEY_BUILD_VARIANT_PATH, EMPTY);
    }

    private Map<String, String> getEnVarPlatformInfo() {

        // update the gobal variables if needed
        PackageManager.updateGlobalEnvironmentVariables();
        if ((getReferencingPlatformFile() == null) || (getreferencedPlatformFile() == null)) {
            // something is seriously wrong -->shoot
            return new HashMap<>();
        }
        File referencingPlatformFile = getReferencingPlatformFile().getTxtFile();
        ArduinoPlatform referencingPlatform = InternalPackageManager.getPlatform(referencingPlatformFile);
        File referencedPlatformFile = getreferencedPlatformFile().getTxtFile();

        ArduinoPlatform referencedPlatform = InternalPackageManager.getPlatform(referencedPlatformFile);

        boolean jsonBasedPlatformManagement = !Preferences.getUseArduinoToolSelection();
        if (jsonBasedPlatformManagement) {
            // overrule the Arduino IDE way of working and use the json refereced tools
            Map<String, String> ret = Helpers.getEnvVarPlatformFileTools(referencedPlatform, true);
            ret.putAll(Helpers.getEnvVarPlatformFileTools(referencingPlatform, false));
            return ret;
        }
        // standard arduino IDE way
        Map<String, String> ret = Helpers.getEnvVarPlatformFileTools(referencingPlatform, false);
        ret.putAll(Helpers.getEnvVarPlatformFileTools(referencedPlatform, true));
        return ret;

    }

    /**
     * Following post processing is done
     *
     * CDT uses different keys to identify the input and output files then the
     * arduino recipes. Therefore I split the arduino recipes into parts (based on
     * the arduino keys) and connect them again in the plugin.xml using the CDT
     * keys. This code assumes that the command is in following order ${first part}
     * ${files} ${second part} [${ARCHIVE_FILE} ${third part}] with [optional]
     *
     * Secondly The handling of the upload variables is done differently in arduino
     * than here. This is taken care of here. for example the output of this input
     * tools.avrdude.upload.pattern="{cmd.path}" "-C{config.path}" {upload.verbose}
     * is changed as if it were the output of this input
     * tools.avrdude.upload.pattern="{tools.avrdude.cmd.path}"
     * "-C{tools.avrdude.config.path}" {tools.avrdude.upload.verbose}
     *
     * thirdly if a programmer is selected different from default some extra actions
     * are done here so no special code is needed to handle programmers
     *
     * Fourthly The build path for the core is {BUILD.PATH}/core/core in sloeber
     * where it is {BUILD.PATH}/core/ in arduino world and used to be {BUILD.PATH}/
     * This only gives problems in the link command as sometimes there are hardcoded
     * links to some sys files so ${A.BUILD.PATH}/core/sys* ${A.BUILD.PATH}/sys* is
     * replaced with ${A.BUILD.PATH}/core/core/sys*
     *
     * @param contribEnv
     * @param confDesc
     * @param boardsDescriptor
     */
    private Map<String, String> getEnvVarsPostProcessing(Map<String, String> vars) {

        Map<String, String> extraVars = new HashMap<>();

        // split the recipes so we can add the input and output markers as cdt needs
        // them
        String recipeKeys[] = { RECIPE_C_to_O, RECIPE_CPP_to_O, RECIPE_S_to_O, RECIPE_SIZE, RECIPE_AR,
                RECIPE_C_COMBINE };
        for (String recipeKey : recipeKeys) {
            String recipe = vars.get(recipeKey);
            if (null == recipe) {
                continue;
            }

            // Sloeber should split o, -o {output} but to be safe that needs a regex so I
            // simply delete the -o
            if (!RECIPE_C_COMBINE.equals(recipeKey)) {
                recipe = recipe.replace(" -o ", " "); //$NON-NLS-1$ //$NON-NLS-2$
            }
            String recipeParts[] = recipe.split(
                    "(\"\\$\\{A.object_file}\")|(\\$\\{A.object_files})|(\"\\$\\{A.source_file}\")|(\"[^\"]*\\$\\{A.archive_file}\")|(\"[^\"]*\\$\\{A.archive_file_path}\")", //$NON-NLS-1$
                    3);

            switch (recipeParts.length) {
            case 0:
                extraVars.put(recipeKey + DOT + '1', "echo no command for \"{KEY}\".".replace(Messages.KEY, recipeKey)); //$NON-NLS-1$
                break;
            case 1:
                extraVars.put(recipeKey + DOT + '1', recipeParts[0]);
                break;
            case 2:
                extraVars.put(recipeKey + DOT + '1', recipeParts[0]);
                extraVars.put(recipeKey + DOT + '2', recipeParts[1]);
                break;
            case 3:
                extraVars.put(recipeKey + DOT + '1', recipeParts[0]);
                extraVars.put(recipeKey + DOT + '2', recipeParts[1]);
                extraVars.put(recipeKey + DOT + '3', recipeParts[2]);

                break;
            default:
                // this should never happen as the split is limited to 3
            }
        }

        ArrayList<String> objcopyCommand = new ArrayList<>();
        for (Entry<String, String> curVariable : vars.entrySet()) {
            String name = curVariable.getKey();
            String value = curVariable.getValue();
            if (name.startsWith(Const.RECIPE_OBJCOPY) && name.endsWith(".pattern") && !value.isEmpty()) { //$NON-NLS-1$
                objcopyCommand.add(makeEnvironmentVar(name));
            }
        }
        Collections.sort(objcopyCommand);
        extraVars.put(Const.JANTJE_OBJCOPY, StringUtil.join(objcopyCommand, "\n\t")); //$NON-NLS-1$

        // handle the hooks
        extraVars.putAll(getEnvVarsHookBuild(vars, "A.JANTJE.pre.link", //$NON-NLS-1$
                "A.recipe.hooks.linking.prelink.XX.pattern", false)); //$NON-NLS-1$
        extraVars.putAll(getEnvVarsHookBuild(vars, "A.JANTJE.post.link", //$NON-NLS-1$
                "A.recipe.hooks.linking.postlink.XX.pattern", true)); //$NON-NLS-1$
        extraVars.putAll(getEnvVarsHookBuild(vars, "A.JANTJE.prebuild", "A.recipe.hooks.prebuild.XX.pattern", //$NON-NLS-1$ //$NON-NLS-2$
                false));
        extraVars.putAll(getEnvVarsHookBuild(vars, "A.JANTJE.sketch.prebuild", //$NON-NLS-1$
                "A.recipe.hooks.sketch.prebuild.XX.pattern", false)); //$NON-NLS-1$
        extraVars.putAll(getEnvVarsHookBuild(vars, "A.JANTJE.sketch.postbuild", //$NON-NLS-1$
                "A.recipe.hooks.sketch.postbuild.XX.pattern", false)); //$NON-NLS-1$

        // add -relax for mega boards; the arduino ide way
        String buildMCU = vars.get(Const.ENV_KEY_BUILD_MCU);
        if ("atmega2560".equalsIgnoreCase(buildMCU)) { //$NON-NLS-1$
            String c_elf_flags = vars.get(Const.ENV_KEY_BUILD_COMPILER_C_ELF_FLAGS);
            extraVars.put(Const.ENV_KEY_BUILD_COMPILER_C_ELF_FLAGS, c_elf_flags + ",--relax"); //$NON-NLS-1$
        }
        return extraVars;
    }

    /**
     * This method creates environment variables based on the platform.txt and
     * boards.txt. platform.txt is processed first and then boards.txt. This way
     * boards.txt settings can overwrite common settings in platform.txt The
     * environment variables are only valid for the project given as parameter The
     * project properties are used to identify the boards.txt and platform.txt as
     * well as the board id to select the settings in the board.txt file At the end
     * also the path variable is set
     *
     *
     * To be able to quickly fix boards.txt and platform.txt problems I also added a
     * pre and post platform and boards files that are processed before and after
     * the arduino delivered boards.txt file.
     *
     * @param project
     *            the project for which the environment variables are set
     * @param arduinoProperties
     *            the info of the selected board to set the variables for
     */

    private static Map<String, String> getEnvVarsHookBuild(Map<String, String> vars, String varName, String hookName,
            boolean post) {
        Map<String, String> extraVars = new HashMap<>();
        String envVarString = new String();
        String searchString = "XX"; //$NON-NLS-1$
        String postSeparator = "}\n\t"; //$NON-NLS-1$
        String preSeparator = "${"; //$NON-NLS-1$
        if (post) {
            postSeparator = "${"; //$NON-NLS-1$
            preSeparator = "}\n\t"; //$NON-NLS-1$
        }
        for (int numDigits = 1; numDigits <= 2; numDigits++) {
            String formatter = "%0" + Integer.toString(numDigits) + "d"; //$NON-NLS-1$ //$NON-NLS-2$
            int counter = 1;
            String hookVarName = hookName.replace(searchString, String.format(formatter, Integer.valueOf(counter)));
            while (null != vars.get(hookVarName)) { // $NON-NLS-1$
                envVarString = envVarString + preSeparator + hookVarName + postSeparator;
                hookVarName = hookName.replace(searchString, String.format(formatter, Integer.valueOf(++counter)));
            }
        }
        if (!envVarString.isEmpty()) {
            extraVars.put(varName, envVarString);
        }
        return extraVars;
    }


}
