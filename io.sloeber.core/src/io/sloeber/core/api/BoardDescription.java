package io.sloeber.core.api;

import static io.sloeber.core.common.Common.*;
import static io.sloeber.core.common.Const.*;

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
import io.sloeber.core.managers.ArduinoPlatform;
import io.sloeber.core.managers.InternalPackageManager;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.KeyValue;
import io.sloeber.core.txt.BoardTxtFile;
import io.sloeber.core.txt.KeyValueTree;
import io.sloeber.core.txt.PlatformTxtFile;
import io.sloeber.core.txt.Programmers;
import io.sloeber.core.txt.TxtFile;

public class BoardDescription {
    // Important constants to avoid having to add the class
    private static final String TOOL_ID = Messages.TOOL;
    private static final String BOARD_ID = Messages.BOARD;
    private static final String FILE_ID = Messages.FILE;
    private static final String VendorArduino = ARDUINO;

    /*
     * Some constants
     */
    private static final String REFERENCED = "referenced"; //$NON-NLS-1$
    private static final String KEY_LAST_USED_BOARD = "Last used Board"; //$NON-NLS-1$
    private static final String KEY_LAST_USED_UPLOAD_PORT = "Last Used Upload port"; //$NON-NLS-1$
    private static final String KEY_LAST_USED_UPLOAD_PROTOCOL = "last Used upload Protocol"; //$NON-NLS-1$
    private static final String KEY_LAST_USED_BOARDS_FILE = "Last used Boards file"; //$NON-NLS-1$
    private static final String KEY_LAST_USED_BOARD_MENU_OPTIONS = "last used Board custom option selections"; //$NON-NLS-1$
    private static final String ENV_KEY_SERIAL_PORT = "serial_port"; //$NON-NLS-1$
    private static final String ENV_KEY_SERIAL_DOT_PORT = "serial.port"; //$NON-NLS-1$
    private static final String ENV_KEY_SERIAL_PORT_FILE = "serial.port.file"; //$NON-NLS-1$
    private static final String ENV_KEY_BUILD_VARIANT_PATH = BUILD + DOT + VARIANT + DOT + PATH;
    private static final String ENV_KEY_BUILD_ACTUAL_CORE_PATH = BUILD + DOT + CORE + DOT + PATH;
    private static final String ENV_KEY_BUILD_ARCH = BUILD + DOT + "arch"; //$NON-NLS-1$
    private static final String ENV_KEY_HARDWARE_PATH = RUNTIME + DOT + HARDWARE + DOT + PATH;
    private static final String ENV_KEY_PLATFORM_PATH = RUNTIME + DOT + PLATFORM + DOT + PATH;
    private static final String ENV_KEY_REFERENCED_CORE_PLATFORM_PATH = REFERENCED + DOT + CORE + DOT + PATH;
    private static final String ENV_KEY_REFERENCED_VARIANT_PLATFORM_PATH = REFERENCED + DOT + VARIANT + DOT + PATH;
    private static final String ENV_KEY_REFERENCED_UPLOAD_PLATFORM_PATH = REFERENCED + DOT + UPLOAD + PATH;

    // preference nodes
    private static final String NODE_ARDUINO = Activator.NODE_ARDUINO;
    private static final IEclipsePreferences myStorageNode = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);
    private static final TxtFile pluginPreProcessingPlatformTxt = new TxtFile(
            ConfigurationPreferences.getPreProcessingPlatformFile());
    private static final TxtFile pluginPostProcessingPlatformTxt = new TxtFile(
            ConfigurationPreferences.getPostProcessingPlatformFile());

    /*
     * This is the basic info contained in the descriptor
     */
    private String myUploadPort = EMPTY;
    private String myProgrammer = Defaults.getDefaultUploadProtocol();
    private String myBoardID = EMPTY;
    private Map<String, String> myOptions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);


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
    private final String KEY_SLOEBER_PROGRAMMER = "PROGRAMMER.NAME"; //$NON-NLS-1$
    private final String KEY_SLOEBER_BOARD_TXT = "BOARD.TXT"; //$NON-NLS-1$
    private final String KEY_SLOEBER_BOARD_ID = "BOARD.ID"; //$NON-NLS-1$
    private final String KEY_SLOEBER_UPLOAD_PORT = "UPLOAD.PORT"; //$NON-NLS-1$
    private final String KEY_SLOEBER_MENU_SELECTION = "BOARD.MENU"; //$NON-NLS-1$


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
        if (otherBoardDescriptor == null) {
            return false;
        }
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
        if (otherBoardDescriptor == null) {
            return true;
        }
        if (!this.getBoardID().equals(otherBoardDescriptor.getBoardID())) {
            return true;
        }
        String moddedReferencingBoardsFile = makePathEnvironmentString(getReferencingBoardsFile());
        String moddedOtherReferencingBoardsFile = makePathEnvironmentString(
                otherBoardDescriptor.getReferencingBoardsFile());
        if (!moddedReferencingBoardsFile.equals(moddedOtherReferencingBoardsFile)) {
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
                    Common.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
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
                    Common.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
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
                    Common.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
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
                    Common.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
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
                    Common.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
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
                    Common.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
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

    /**
     * make a board descriptor for each board in the board.txt file with the default
     * options
     *
     * @param boardFile
     * @return a list of board descriptors
     */
    public static List<BoardDescription> makeBoardDescriptors(File boardFile, Map<String, String> options) {
        BoardTxtFile txtFile = new BoardTxtFile(resolvePathEnvironmentString(boardFile));
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
        this.myBoardID = boardID;
        this.myreferencingBoardsFile = resolvePathEnvironmentString(boardsFile);
        this.myTxtFile = new BoardTxtFile(this.myreferencingBoardsFile);
        setDefaultOptions();
        if (options != null) {
            this.myOptions.putAll(options);
        }
    }

    public BoardDescription() {
        myreferencingBoardsFile = resolvePathEnvironmentString(
                new File(myStorageNode.get(KEY_LAST_USED_BOARDS_FILE, EMPTY)));
        myTxtFile = new BoardTxtFile(this.myreferencingBoardsFile);
        myBoardID = myStorageNode.get(KEY_LAST_USED_BOARD, EMPTY);
        myUploadPort = myStorageNode.get(KEY_LAST_USED_UPLOAD_PORT, EMPTY);
        myProgrammer = myStorageNode.get(KEY_LAST_USED_UPLOAD_PROTOCOL, Defaults.getDefaultUploadProtocol());
        myOptions = KeyValue.makeMap(myStorageNode.get(KEY_LAST_USED_BOARD_MENU_OPTIONS, EMPTY));
    }

    public BoardDescription(BoardDescription srcObject) {
        myreferencingBoardsFile = srcObject.myreferencingBoardsFile;
        myTxtFile = srcObject.myTxtFile;
        myBoardID = srcObject.myBoardID;
        myUploadPort = srcObject.myUploadPort;
        myProgrammer = srcObject.myProgrammer;
        myOptions = srcObject.myOptions;
    }



    public String getuploadTool() {
        return this.myUploadTool;
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
        myStorageNode.put(KEY_LAST_USED_BOARDS_FILE, makePathEnvironmentString(getReferencingBoardsFile()));
        myStorageNode.put(KEY_LAST_USED_BOARD, this.myBoardID);
        myStorageNode.put(KEY_LAST_USED_UPLOAD_PORT, this.myUploadPort);
        myStorageNode.put(KEY_LAST_USED_UPLOAD_PROTOCOL, this.myProgrammer);
        myStorageNode.put(KEY_LAST_USED_BOARD_MENU_OPTIONS, KeyValue.makeString(this.myOptions));
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

    public void setProgrammer(String newUploadProtocol) {
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
        if (this.myreferencingBoardsFile.equals(resolvePathEnvironmentString(boardsFile))) {
            return;
        }

        this.myreferencingBoardsFile = resolvePathEnvironmentString(boardsFile);
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
        return retPath.append(VARIANTS_FOLDER_NAME).append(getBoardVariant());
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
        return retPath.append(CORES).append(this.myBoardsCore);
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
        File platformFile = getreferencingPlatformPath().append(PLATFORM_FILE_NAME).toFile();
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
        File platformFile = myReferencedCorePlatformPath.append(PLATFORM_FILE_NAME).toFile();
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
        String upLoadTool = getActualUploadTool();
        String action = UPLOAD;
        if (usesProgrammer()) {
            action = PROGRAM;
        }
        String networkPrefix = EMPTY;
        if (isNetworkUpload()) {
            networkPrefix = DOT + NETWORK_PREFIX;
        }
        String key = TOOLS + DOT + upLoadTool + DOT + action + networkPrefix + DOT + PATTERN;
        String ret = Common.getBuildEnvironmentVariable(confdesc, key, EMPTY);
        if (ret.isEmpty()) {
            Common.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID, key + " : not found in the platform.txt file")); //$NON-NLS-1$
        }
        return ret;
    }

    public String getActualUploadTool() {
        updateWhenDirty();
        // if (confdesc == null) {
        // Common.log(new Status(IStatus.ERROR, .CORE_PLUGIN_ID, "Confdesc null is
        // not alowed here")); //$NON-NLS-1$
        // return this.myUploadTool;
        // }
        // if (usesProgrammer()) {
        // return Common.getBuildEnvironmentVariable(confdesc, PROGRAM_TOOL,
        // "Program tool not properly configured"); //$NON-NLS-1$
        // }
        //
        // if (this.myUploadTool == null) {
        // return Common.getBuildEnvironmentVariable(confdesc, UPLOAD_TOOL,
        // "upload tool not properly configured"); //$NON-NLS-1$
        // }
        return myUploadTool;

    }

    public boolean usesProgrammer() {
        updateWhenDirty();
        return !myProgrammer.equals(Defaults.getDefaultUploadProtocol());
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

    protected BoardDescription(File txtFile, String boardID) {
        this.myBoardID = boardID;
        this.myreferencingBoardsFile = txtFile;
        this.myTxtFile = new BoardTxtFile(txtFile);
        setDefaultOptions();
        calculateDerivedFields();
    }

    BoardDescription(TxtFile configFile, String prefix) {

        KeyValueTree tree = configFile.getData();
        KeyValueTree section = tree.getChild(prefix);
        this.myProgrammer = section.getValue(KEY_SLOEBER_PROGRAMMER);
        this.myBoardID = section.getValue(KEY_SLOEBER_BOARD_ID);
        String board_txt = section.getValue(KEY_SLOEBER_BOARD_TXT);
        this.myUploadPort = section.getValue(KEY_SLOEBER_UPLOAD_PORT);
        KeyValueTree optionsTree = section.getChild(KEY_SLOEBER_MENU_SELECTION);
        Map<String, String> options = optionsTree.toKeyValues(EMPTY, false);

        myreferencingBoardsFile = resolvePathEnvironmentString(new File(board_txt));
        this.myTxtFile = new BoardTxtFile(this.myreferencingBoardsFile);
        setDefaultOptions();
        if (options != null) {
            // Only add the valid options for this board to our options
            myOptions.putAll(onlyKeepValidOptions(options));
        }
    }


    private Map<String, String> onlyKeepValidOptions(Map<String, String> options) {
        Map<String, String> ret = new HashMap<>();

        KeyValueTree tree = myTxtFile.getData();
        KeyValueTree boardMenuSection = tree.getChild(myBoardID + DOT + MENU);
        if (boardMenuSection != null) {
            for (Entry<String, String> curoption : options.entrySet()) {
                String key = curoption.getKey();
                if (boardMenuSection.getChild(key).getKey() != null) {
                    ret.put(key, curoption.getValue());
                }
            }
        }
        return ret;
    }

    /**
     * get the environment variables that need to be stored in the configuration
     * files configuration files are files needed to setup the sloeber environment
     * for instance when openiung a project or after import of a project in the
     * workspace
     * 
     * @return the minimum list of environment variables to recreate the project
     */
    public Map<String, String> getEnvVarsConfig(String prefix) {
        Map<String, String> allVars = new TreeMap<>();
        String board_txt = makePathVersionString(getReferencingBoardsFile());

        allVars.put(prefix + KEY_SLOEBER_PROGRAMMER, myProgrammer);
        allVars.put(prefix + KEY_SLOEBER_BOARD_ID, myBoardID);
        allVars.put(prefix + KEY_SLOEBER_BOARD_TXT, board_txt);
        allVars.put(prefix + KEY_SLOEBER_UPLOAD_PORT, myUploadPort);

        for (Entry<String, String> curOption : myOptions.entrySet()) {
            allVars.put(prefix + KEY_SLOEBER_MENU_SELECTION + DOT + curOption.getKey(), curOption.getValue());
        }
        return allVars;
    }

    /**
     * get the environment variables that need to be stored in version control
     * 
     * @return the minimum list of environment variables to recreate the project
     *         from version control
     */
    public Map<String, String> getEnvVarsVersion(String prefix) {
        Map<String, String> allVars = new TreeMap<>();
        String board_txt = makePathVersionString(getReferencingBoardsFile());

        allVars.put(prefix + KEY_SLOEBER_BOARD_ID, myBoardID);
        allVars.put(prefix + KEY_SLOEBER_BOARD_TXT, board_txt);

        for (Entry<String, String> curOption : myOptions.entrySet()) {
            allVars.put(prefix + KEY_SLOEBER_MENU_SELECTION + DOT + curOption.getKey(), curOption.getValue());
        }
        return allVars;
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


        allVars.put(ENV_KEY_SERIAL_PORT, getActualUploadPort());
        allVars.put(ENV_KEY_SERIAL_DOT_PORT, getActualUploadPort());

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

        // put in the installed tools info
        allVars.putAll(getEnVarPlatformInfo());



        Programmers localProgrammers[] = Programmers.fromBoards(this);
        String programmer = getProgrammer();
        for (Programmers curProgrammer : localProgrammers) {
            String programmerID = curProgrammer.getIDFromNiceName(programmer);
            if (programmerID != null) {
                allVars.putAll(curProgrammer.getAllEnvironVars(programmerID));
            }
        }

        // boards settings not coming from menu selections
        allVars.putAll(myTxtFile.getAllBoardEnvironVars(getBoardID()));

        // board settings from menu selections
        Map<String, String> options = getOptions();
        KeyValueTree rootData = myTxtFile.getData();
        KeyValueTree menuData = rootData.getChild(getBoardID() + DOT + MENU);
        for (Entry<String, String> curOption : options.entrySet()) {
            String menuID = curOption.getKey();
            String SelectedMenuItemID = curOption.getValue();
            KeyValueTree curSelectedMenuItem = menuData.getChild(menuID + DOT + SelectedMenuItemID);
            allVars.putAll(curSelectedMenuItem.toKeyValues(EMPTY, false));
        }

        // add the stuff that comes with the plugin that is marked as post
        allVars.putAll(pluginPostProcessingPlatformTxt.getAllEnvironVars(EMPTY));
        allVars.putAll(pluginPostProcessingBoardsTxt.getEnvVarsTxt());

        // Do some coded post processing
        allVars.putAll(getEnvVarsPostProcessing(allVars));
        return allVars;

    }

    private Map<String, String> getEnVarPlatformInfo() {
        IPath referencingPlatformPath = getreferencingPlatformPath();
        IPath referencedPlatformPath = getReferencedCorePlatformPath();

        if ((referencingPlatformPath == null) || (referencedPlatformPath == null)) {
            // something is seriously wrong -->shoot
            return new HashMap<>();
        }

        ArduinoPlatform referencingPlatform = InternalPackageManager.getPlatform(referencingPlatformPath);
        ArduinoPlatform referencedPlatform = InternalPackageManager.getPlatform(referencedPlatformPath);

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
     * ${files} ${second part} [${archive_file} ${third part}] with [optional]
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
     * where it is {build.path}/core/ in arduino world and used to be {build.path}/
     * This only gives problems in the link command as sometimes there are hardcoded
     * links to some sys files so ${build.path}/core/sys* ${build.path}/sys* is
     * replaced with ${build.path}/core/core/sys*
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
                    "(\"\\$\\{object_file}\")|(\\$\\{object_files})|(\"\\$\\{source_file}\")|(\"[^\"]*\\$\\{archive_file}\")|(\"[^\"]*\\$\\{archive_file_path}\")", //$NON-NLS-1$
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
            if (name.startsWith(RECIPE_OBJCOPY) && name.endsWith(".pattern") && !value.isEmpty()) { //$NON-NLS-1$
                objcopyCommand.add(makeEnvironmentVar(name));
            }
        }
        Collections.sort(objcopyCommand);
        extraVars.put(SLOEBER_OBJCOPY, StringUtil.join(objcopyCommand, "\n\t")); //$NON-NLS-1$

        // handle the hooks
        extraVars.putAll(getEnvVarsHookBuild(vars, "sloeber.pre.link", //$NON-NLS-1$
                "recipe.hooks.linking.prelink.XX.pattern", false)); //$NON-NLS-1$
        extraVars.putAll(getEnvVarsHookBuild(vars, "sloeber.post.link", //$NON-NLS-1$
                "recipe.hooks.linking.postlink.XX.pattern", true)); //$NON-NLS-1$
        extraVars.putAll(getEnvVarsHookBuild(vars, "sloeber.prebuild", "recipe.hooks.prebuild.XX.pattern", //$NON-NLS-1$ //$NON-NLS-2$
                false));
        extraVars.putAll(getEnvVarsHookBuild(vars, "sloeber.sketch.prebuild", //$NON-NLS-1$
                "recipe.hooks.sketch.prebuild.XX.pattern", false)); //$NON-NLS-1$
        extraVars.putAll(getEnvVarsHookBuild(vars, "sloeber.sketch.postbuild", //$NON-NLS-1$
                "recipe.hooks.sketch.postbuild.XX.pattern", false)); //$NON-NLS-1$

        // add -relax for mega boards; the arduino ide way
        String buildMCU = vars.get(ENV_KEY_BUILD_MCU);
        if ("atmega2560".equalsIgnoreCase(buildMCU)) { //$NON-NLS-1$
            String c_elf_flags = vars.get(ENV_KEY_BUILD_COMPILER_C_ELF_FLAGS);
            extraVars.put(ENV_KEY_BUILD_COMPILER_C_ELF_FLAGS, c_elf_flags + ",--relax"); //$NON-NLS-1$
        }
        return extraVars;
    }

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

    /**
     * method to get the configuration info from the old way Sloeber stored data
     * 
     * @param confDesc
     * @return
     */
    @SuppressWarnings("nls")
    public static BoardDescription getFromCDT(ICConfigurationDescription confDesc) {
        BoardDescription ret = new BoardDescription();
        ret.myUploadPort = getOldWayEnvVar(confDesc, "JANTJE.com_port");
        ret.myProgrammer = getOldWayEnvVar(confDesc, "JANTJE.upload");
        ret.myBoardID = getOldWayEnvVar(confDesc, "JANTJE.board_ID");
        String optinconcat = getOldWayEnvVar(confDesc, "JANTJE.menu");
        ret.myOptions = KeyValue.makeMap(optinconcat);
        
        String referencingBoardsFile = getOldWayEnvVar(confDesc, "JANTJE.boards_file");
        int packagesIndex=referencingBoardsFile.indexOf( "\\arduinoPlugin\\packages\\");
        if(packagesIndex==-1) {
            packagesIndex=referencingBoardsFile.indexOf( "/arduinoPlugin/packages/");
        }
        if(packagesIndex!=-1) {
            referencingBoardsFile = sloeberHomePath.append(referencingBoardsFile.substring(packagesIndex)).toString();
        }
        ret.myreferencingBoardsFile = resolvePathEnvironmentString(new File(referencingBoardsFile));
        ret.myTxtFile = new BoardTxtFile(ret.myreferencingBoardsFile);
        
        return ret;
    }

    public boolean isValid() {
        if (myreferencingBoardsFile == null) {
            return false;
        }
        return myreferencingBoardsFile.exists();
    }

    public void reloadTxtFile() {
        myTxtFile.reloadTxtFile();

    }

}
