package io.sloeber.core.api;

import static io.sloeber.core.Messages.*;
import static io.sloeber.core.common.Common.*;
import static io.sloeber.core.common.Const.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.cdt.core.parser.util.StringUtil;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import io.sloeber.core.api.Json.ArduinoPlatform;
import io.sloeber.core.api.Json.ArduinoPlatformTooldDependency;
import io.sloeber.core.api.Json.ArduinoPlatformVersion;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.Const;
import io.sloeber.core.tools.KeyValue;
import io.sloeber.core.txt.BoardTxtFile;
import io.sloeber.core.txt.KeyValueTree;
import io.sloeber.core.txt.PlatformTxtFile;
import io.sloeber.core.txt.Programmers;
import io.sloeber.core.txt.TxtFile;

public class BoardDescription {
    private static final String KEY_LAST_USED_BOARD = "Last used Board"; //$NON-NLS-1$
    private static final String KEY_LAST_USED_UPLOAD_PORT = "Last Used Upload port"; //$NON-NLS-1$
    private static final String KEY_LAST_USED_UPLOAD_PROTOCOL = "last Used upload Protocol"; //$NON-NLS-1$
    private static final String KEY_LAST_USED_BOARDS_FILE = "Last used Boards file"; //$NON-NLS-1$
    private static final String KEY_LAST_USED_BOARD_MENU_OPTIONS = "last used Board custom option selections"; //$NON-NLS-1$
    private static final String ENV_KEY_SERIAL_PORT = "serial_port"; //$NON-NLS-1$
    private static final String ENV_KEY_SERIAL_DOT_PORT = "serial.port"; //$NON-NLS-1$
    private static final String ENV_KEY_SERIAL_PORT_FILE = "serial.port.file"; //$NON-NLS-1$
    private static final String ENV_KEY_BUILD_VARIANT_PATH = BUILD + DOT + ARDUINO_VARIANTS_FOLDER_NAME + DOT + PATH;
    private static final String ENV_KEY_BUILD_SYSTEM_PATH = BUILD + DOT + SYSTEM + DOT + PATH;
    private static final String ENV_KEY_BUILD_ACTUAL_CORE_PATH = BUILD + DOT + CORE + DOT + PATH;
    private static final String ENV_KEY_BUILD_ARCH = BUILD + DOT + "arch"; //$NON-NLS-1$
    private static final String ENV_KEY_HARDWARE_PATH = RUNTIME + DOT + HARDWARE + DOT + PATH;
    private static final String ENV_KEY_PLATFORM_PATH = RUNTIME + DOT + PLATFORM + DOT + PATH;

    // stuff to store last used board
    private static final String KEY_SLOEBER_PROGRAMMER = "PROGRAMMER.NAME"; //$NON-NLS-1$
    private static final String KEY_SLOEBER_BOARD_TXT = "BOARD.TXT"; //$NON-NLS-1$
    private static final String KEY_SLOEBER_BOARD_ID = "BOARD.ID"; //$NON-NLS-1$
    private static final String KEY_SLOEBER_UPLOAD_PORT = "UPLOAD.PORT"; //$NON-NLS-1$
    private static final String KEY_SLOEBER_MENU_SELECTION = "BOARD.MENU"; //$NON-NLS-1$
    private static final IEclipsePreferences myStorageNode = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);

    /*
     * This is the basic info contained in the descriptor
     */
    private String myUploadPort = EMPTY;
    private String myProgrammer = EMPTY;
    private String myBoardID = EMPTY;
    private Map<String, String> myOptions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    private File myUserSelectedBoardsTxtFile; //this is the boards.txt file selected in the gui
    private BoardTxtFile mySloeberBoardTxtFile; // this is the actual used and loaded sloeber.boards.txt file

    private String myBoardsCore = null;
    private String myBoardsVariant = null;
    private String myUploadTool = null;

    private ArduinoPlatformVersion myReferencedPlatformVariant = null;
    private ArduinoPlatformVersion myReferencedPlatformCore = null;
    private ArduinoPlatformVersion myReferencedPlatformUpload = null;

    private boolean isDirty = true;

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
        String moddedReferencingBoardsFile = makePathVersionString(getReferencingBoardsFile());
        String moddedOtherReferencingBoardsFile = makePathVersionString(
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

        myReferencedPlatformCore = null;
        myBoardsCore = null;
        myReferencedPlatformVariant = null;
        myBoardsVariant = null;
        myReferencedPlatformUpload = null;
        myUploadTool = null;
        setDefaultOptions();
        // search in the board info
        ParseSection();

    }

    private void ParseSection() {
        KeyValueTree rootData = mySloeberBoardTxtFile.getData();
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
                myBoardsCore = valueSplit[1];
                myReferencedPlatformCore = BoardsManager.getNewestInstalledPlatform(refVendor, architecture);
                if (myReferencedPlatformCore == null) {
                    Common.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
                            Helpers_tool_reference_missing.replace(TOOL_TAG, core)
                                    .replace(FILE_TAG, getReferencingBoardsFile().toString())
                                    .replace(BOARD_TAG, getBoardID())));
                    return;
                }
            } else if (valueSplit.length == 4) {
                String refVendor = valueSplit[0];
                String refArchitecture = valueSplit[1];
                VersionNumber refVersion = new VersionNumber(valueSplit[2]);
                myBoardsCore = valueSplit[3];
                myReferencedPlatformCore = BoardsManager.getPlatform(refVendor, refArchitecture, refVersion);
                if (myReferencedPlatformCore == null) {
                    Common.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
                            Helpers_tool_reference_missing.replace(TOOL_TAG, core)
                                    .replace(FILE_TAG, getReferencingBoardsFile().toString())
                                    .replace(BOARD_TAG, getBoardID())));
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
                myBoardsVariant = valueSplit[1];
                myReferencedPlatformVariant = BoardsManager.getNewestInstalledPlatform(refVendor, architecture);
                if (myReferencedPlatformVariant == null) {
                    Common.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
                            Helpers_tool_reference_missing.replace(TOOL_TAG, variant)
                                    .replace(FILE_TAG, getReferencingBoardsFile().toString())
                                    .replace(BOARD_TAG, getBoardID())));
                    return;
                }
            } else if (valueSplit.length == 4) {
                String refVendor = valueSplit[0];
                String refArchitecture = valueSplit[1];
                VersionNumber refVersion = new VersionNumber(valueSplit[2]);
                myBoardsVariant = valueSplit[3];
                if ("*".equals(valueSplit[2])) { //$NON-NLS-1$
                    myReferencedPlatformVariant = BoardsManager.getNewestInstalledPlatform(refVendor, refArchitecture);
                } else {
                    myReferencedPlatformVariant = BoardsManager.getPlatform(refVendor, refArchitecture, refVersion);
                }
                if (myReferencedPlatformVariant == null) {
                    Common.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
                            Helpers_tool_reference_missing.replace(TOOL_TAG, variant)
                                    .replace(FILE_TAG, getReferencingBoardsFile().toString())
                                    .replace(BOARD_TAG, getBoardID())));
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
                myUploadTool = valueSplit[1];
                myReferencedPlatformUpload = BoardsManager.getNewestInstalledPlatform(refVendor, architecture);
                if (myReferencedPlatformUpload == null) {
                    Common.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
                            Helpers_tool_reference_missing.replace(TOOL_TAG, upload)
                                    .replace(FILE_TAG, getReferencingBoardsFile().toString())
                                    .replace(BOARD_TAG, getBoardID())));
                    return;
                }
            } else if (valueSplit.length == 4) {
                String refVendor = valueSplit[0];
                String refArchitecture = valueSplit[1];
                VersionNumber refVersion = new VersionNumber(valueSplit[2]);
                myUploadTool = valueSplit[3];
                myReferencedPlatformUpload = BoardsManager.getPlatform(refVendor, refArchitecture, refVersion);
                if (this.myReferencedPlatformUpload == null) {
                    Common.log(new Status(IStatus.ERROR, CORE_PLUGIN_ID,
                            Helpers_tool_reference_missing.replace(TOOL_TAG, upload)
                                    .replace(FILE_TAG, getReferencingBoardsFile().toString())
                                    .replace(BOARD_TAG, getBoardID())));
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
    public static List<BoardDescription> makeBoardDescriptors(File boardFile) {
        BoardTxtFile txtFile = new BoardTxtFile(resolvePathEnvironmentString(boardFile));
        List<BoardDescription> boards = new ArrayList<>();
        List<String> boardIDs = txtFile.getAllBoardIDs();
        for (String curboardID : boardIDs) {
            Map<String, String> boardSection = txtFile.getSection(curboardID);
            if (boardSection != null) {
                if (!"true".equalsIgnoreCase(boardSection.get("hide"))) { //$NON-NLS-1$ //$NON-NLS-2$
                    boards.add(new BoardDescription(boardFile, curboardID, null));
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
        myBoardID = boardID;
        myUserSelectedBoardsTxtFile = boardsFile;
        mySloeberBoardTxtFile = new BoardTxtFile(resolvePathEnvironmentString(myUserSelectedBoardsTxtFile));
        setDefaultOptions();
        if (options != null) {
            myOptions.putAll(options);
        }
    }

    public BoardDescription() {
        myUserSelectedBoardsTxtFile = new File(myStorageNode.get(KEY_LAST_USED_BOARDS_FILE, EMPTY));
        if (!myUserSelectedBoardsTxtFile.exists()) {
            List<ArduinoPlatformVersion> platforms = BoardsManager.getInstalledPlatforms();
            //If you crash on the next line no platform have been installed
            ArduinoPlatformVersion platform = platforms.get(0);
            myUserSelectedBoardsTxtFile = platform.getBoardsFile();
            mySloeberBoardTxtFile = new BoardTxtFile(myUserSelectedBoardsTxtFile);
            myBoardID = mySloeberBoardTxtFile.getAllBoardIDs().get(0);
        } else {
            mySloeberBoardTxtFile = new BoardTxtFile(myUserSelectedBoardsTxtFile);
            myBoardID = myStorageNode.get(KEY_LAST_USED_BOARD, EMPTY);
            myUploadPort = myStorageNode.get(KEY_LAST_USED_UPLOAD_PORT, EMPTY);
            myProgrammer = myStorageNode.get(KEY_LAST_USED_UPLOAD_PROTOCOL, EMPTY);
            myOptions = KeyValue.makeMap(myStorageNode.get(KEY_LAST_USED_BOARD_MENU_OPTIONS, EMPTY));
        }
    }

    public BoardDescription(BoardDescription srcObject) {
        myUserSelectedBoardsTxtFile = srcObject.myUserSelectedBoardsTxtFile;
        mySloeberBoardTxtFile = srcObject.mySloeberBoardTxtFile;
        myBoardID = srcObject.myBoardID;
        myUploadPort = srcObject.myUploadPort;
        myProgrammer = srcObject.myProgrammer;
        myOptions = new TreeMap<>(srcObject.myOptions);
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
        Map<String, String> allMenuIDs = this.mySloeberBoardTxtFile.getMenus();
        for (Map.Entry<String, String> curMenuID : allMenuIDs.entrySet()) {
            String providedMenuValue = this.myOptions.get(curMenuID.getKey());
            ArrayList<String> menuOptions = this.mySloeberBoardTxtFile.getMenuItemIDsFromMenuID(curMenuID.getKey(),
                    getBoardID());
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

    public String getArchitecture() {
        return mySloeberBoardTxtFile.getArchitecture();
    }

    public File getReferencingBoardsFile() {
        return myUserSelectedBoardsTxtFile;
    }

    public String getBoardName() {
        return this.mySloeberBoardTxtFile.getNiceNameFromID(this.myBoardID);
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
        String newBoardID = this.mySloeberBoardTxtFile.getIDFromNiceName(boardName);
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

        if (!myUserSelectedBoardsTxtFile.equals(boardsFile)) {
            myUserSelectedBoardsTxtFile = boardsFile;
            setDirty();
        }

        /* do not remove this for optimization as workaround changes will not be captured */
        mySloeberBoardTxtFile = new BoardTxtFile(resolvePathEnvironmentString(myUserSelectedBoardsTxtFile));

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
        return this.mySloeberBoardTxtFile.getAllSectionNames();
    }

    public String[] getUploadProtocols() {

        return Programmers.getUploadProtocols(this);

    }

    public String[] getMenuItemNamesFromMenuID(String menuID) {
        return this.mySloeberBoardTxtFile.getMenuItemNamesFromMenuID(menuID, this.myBoardID);
    }

    public TreeMap<String, IPath> getAllExamples() {
        updateWhenDirty();
        return LibraryManager.getAllExamples(this);
    }

    public String getMenuNameFromMenuID(String id) {
        return this.mySloeberBoardTxtFile.getMenuNameFromID(id);
    }

    public String getMenuItemNamedFromMenuItemID(String menuItemID, String menuID) {
        return this.mySloeberBoardTxtFile.getMenuItemNameFromMenuItemID(this.myBoardID, menuID, menuItemID);
    }

    public String getMenuItemIDFromMenuItemName(String menuItemName, String menuID) {
        return this.mySloeberBoardTxtFile.getMenuItemIDFromMenuItemName(this.myBoardID, menuID, menuItemName);
    }

    /**
     * provide the actual path to the variant. Use this method if you want to know
     * where the variant is
     *
     * @return the path to the variant; null if no variant is needed
     */
    public IPath getActualVariantPath() {
        updateWhenDirty();
        String boardVariant = getBoardVariant();
        if (boardVariant == null) {
            return null;
        }
        if (myReferencedPlatformVariant == null) {
            return new Path(myUserSelectedBoardsTxtFile.getParent().toString()).append(ARDUINO_VARIANTS_FOLDER_NAME)
                    .append(boardVariant);
        }
        return myReferencedPlatformVariant.getInstallPath().append(ARDUINO_VARIANTS_FOLDER_NAME).append(boardVariant);
    }

    private String getBoardVariant() {
        updateWhenDirty();
        return this.myBoardsVariant;
    }

    public IPath getActualCoreCodePath() {
        updateWhenDirty();
        if (myBoardsCore == null) {
            return null;
        }
        IPath retPath = null;
        if (myReferencedPlatformCore == null) {
            retPath = getreferencingPlatformPath();
        } else {
            retPath = myReferencedPlatformCore.getInstallPath();
        }
        return retPath.append(CORES).append(myBoardsCore);
    }

    public IPath getReferencedUploadPlatformPath() {
        updateWhenDirty();
        if (myReferencedPlatformUpload != null) {
            return myReferencedPlatformUpload.getInstallPath();
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
            return new Path(myUserSelectedBoardsTxtFile.getParent());
        } catch (@SuppressWarnings("unused") Exception e) {
            return new Path(EMPTY);
        }
    }

    public PlatformTxtFile getreferencedCorePlatformFile() {
        updateWhenDirty();
        if (myReferencedPlatformCore == null) {
            return null;
        }
        File platformFile = myReferencedPlatformCore.getInstallPath().append(PLATFORM_FILE_NAME).toFile();
        if (platformFile != null && platformFile.exists()) {
            return new PlatformTxtFile(platformFile);
        }
        return null;
    }

    public IPath getReferencedCoreLibraryPath() {
        updateWhenDirty();
        if (myReferencedPlatformCore == null) {
            return null;
        }
        return this.myReferencedPlatformCore.getInstallPath().append(ARDUINO_LIBRARY_FOLDER_NAME);
    }

    public IPath getReferencingLibraryPath() {
        updateWhenDirty();
        return this.getreferencingPlatformPath().append(ARDUINO_LIBRARY_FOLDER_NAME);
    }

    public String getUploadPatternKey() {
        updateWhenDirty();
        String upLoadTool = getuploadTool();
        String networkPrefix = EMPTY;
        if (isNetworkUpload()) {
            networkPrefix = NETWORK_PREFIX;
        }
        return TOOLS + DOT + upLoadTool + DOT + UPLOAD + DOT + networkPrefix + PATTERN;
    }

    public IPath getreferencedCoreHardwarePath() {
        updateWhenDirty();
        if (myReferencedPlatformCore == null) {
            return getreferencingPlatformPath();
        }
        return myReferencedPlatformCore.getInstallPath();
    }

    /*
     * get the latest installed arduino platform with the same architecture. This is
     * the platform to use the programmers.txt if no other programmers.txt are
     * found.
     */
    public IPath getArduinoPlatformPath() {
        updateWhenDirty();
        ArduinoPlatform platform = BoardsManager.getPlatform(VendorArduino, getArchitecture());
        if (platform == null) {
            return null;
        }
        ArduinoPlatformVersion platformVersion = platform.getNewestInstalled();
        if (platformVersion == null) {
            return null;
        }
        return platformVersion.getInstallPath();
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

    protected BoardDescription(File boardsFile, String boardID) {
        myBoardID = boardID;
        myUserSelectedBoardsTxtFile = boardsFile;
        mySloeberBoardTxtFile = new BoardTxtFile(myUserSelectedBoardsTxtFile);
        setDefaultOptions();
        calculateDerivedFields();
    }

    BoardDescription(TxtFile configFile, String prefix) {

        KeyValueTree tree = configFile.getData();
        KeyValueTree section = tree.getChild(prefix);
        myProgrammer = section.getValue(KEY_SLOEBER_PROGRAMMER);
        myBoardID = section.getValue(KEY_SLOEBER_BOARD_ID);
        String board_txt = section.getValue(KEY_SLOEBER_BOARD_TXT);
        myUploadPort = section.getValue(KEY_SLOEBER_UPLOAD_PORT);
        KeyValueTree optionsTree = section.getChild(KEY_SLOEBER_MENU_SELECTION);
        Map<String, String> options = optionsTree.toKeyValues(EMPTY, false);

        myUserSelectedBoardsTxtFile = resolvePathEnvironmentString(new File(board_txt));
        mySloeberBoardTxtFile = new BoardTxtFile(myUserSelectedBoardsTxtFile);
        setDefaultOptions();
        if (options != null) {
            // Only add the valid options for this board to our options
            myOptions.putAll(onlyKeepValidOptions(options));
        }
    }

    private Map<String, String> onlyKeepValidOptions(Map<String, String> options) {
        Map<String, String> ret = new HashMap<>();

        KeyValueTree tree = mySloeberBoardTxtFile.getData();
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
    public Map<String, String> getEnvVarsConfig() {
        Map<String, String> allVars = new TreeMap<>();
        String board_txt = makePathVersionString(getReferencingBoardsFile());

        allVars.put(KEY_SLOEBER_PROGRAMMER, myProgrammer);
        allVars.put(KEY_SLOEBER_BOARD_ID, myBoardID);
        allVars.put(KEY_SLOEBER_BOARD_TXT, board_txt);
        allVars.put(KEY_SLOEBER_UPLOAD_PORT, myUploadPort);

        for (Entry<String, String> curOption : myOptions.entrySet()) {
            allVars.put(KEY_SLOEBER_MENU_SELECTION + DOT + curOption.getKey(), curOption.getValue());
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

        TxtFile pluginPreProcessingPlatformTxt = new TxtFile(ConfigurationPreferences.getPreProcessingPlatformFile());
        TxtFile pluginPostProcessingPlatformTxt = new TxtFile(ConfigurationPreferences.getPostProcessingPlatformFile());
        BoardTxtFile pluginPreProcessingBoardsTxt = new BoardTxtFile(
                ConfigurationPreferences.getPreProcessingBoardsFile());
        BoardTxtFile pluginPostProcessingBoardsTxt = new BoardTxtFile(
                ConfigurationPreferences.getPostProcessingBoardsFile());

        Map<String, String> allVars = pluginPreProcessingPlatformTxt.getAllEnvironVars(EMPTY);
        allVars.putAll(pluginPreProcessingBoardsTxt.getBoardEnvironVars(getBoardID()));

        String architecture = getArchitecture();
        IPath coreHardwarePath = getreferencedCoreHardwarePath();
        allVars.put(ENV_KEY_BUILD_ARCH, architecture.toUpperCase());
        allVars.put(ENV_KEY_HARDWARE_PATH, getreferencingPlatformPath().removeLastSegments(1).toOSString());
        allVars.put(ENV_KEY_BUILD_SYSTEM_PATH, coreHardwarePath.append(SYSTEM).toOSString());
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

        PlatformTxtFile referencedPlatfromFile = getreferencedCorePlatformFile();
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

        // boards settings not coming from menu selections
        allVars.putAll(mySloeberBoardTxtFile.getBoardEnvironVars(getBoardID()));

        // board settings from menu selections
        Map<String, String> options = getOptions();
        KeyValueTree rootData = mySloeberBoardTxtFile.getData();
        KeyValueTree menuData = rootData.getChild(getBoardID() + DOT + MENU);
        for (Entry<String, String> curOption : options.entrySet()) {
            String menuID = curOption.getKey();
            String SelectedMenuItemID = curOption.getValue();
            KeyValueTree curSelectedMenuItem = menuData.getChild(menuID + DOT + SelectedMenuItemID);
            allVars.putAll(curSelectedMenuItem.toKeyValues(EMPTY, false));
        }

        //This moved last. See github issue 1410
        Programmers localProgrammers[] = Programmers.fromBoards(this);
        String programmer = getProgrammer();
        for (Programmers curProgrammer : localProgrammers) {
            String programmerID = curProgrammer.getIDFromNiceName(programmer);
            if (programmerID != null) {
                allVars.putAll(curProgrammer.getAllEnvironVars(programmerID));
            }
        }

        // add the stuff that comes with the plugin that is marked as post
        allVars.putAll(pluginPostProcessingPlatformTxt.getAllEnvironVars(EMPTY));
        allVars.putAll(pluginPostProcessingBoardsTxt.getBoardEnvironVars(getBoardID()));

        // Do some coded post processing
        allVars.putAll(getEnvVarsPostProcessing(allVars));
        return allVars;

    }

    private Map<String, String> getEnVarPlatformInfo() {
        Map<String, String> ret = new HashMap<>();

        if (myReferencedPlatformUpload != null) {
            ret.putAll(getEnvVarPlatformFileTools(myReferencedPlatformUpload));
        }
        if (myReferencedPlatformVariant != null) {
            ret.putAll(getEnvVarPlatformFileTools(myReferencedPlatformVariant));
        }

        if (myReferencedPlatformCore != null) {
            ret.putAll(getEnvVarPlatformFileTools(myReferencedPlatformCore));
        }

        IPath referencingPlatformPath = getreferencingPlatformPath();
        ArduinoPlatformVersion referencingPlatform = BoardsManager.getPlatform(referencingPlatformPath);

        if (referencingPlatform == null) {
            // This is the case for private hardware
            //there is no need to specify tool path as they do not use them
            return ret;
        }
        ArduinoPlatformVersion latestArduinoPlatform = BoardsManager.getNewestInstalledPlatform(Const.VENDOR_ARDUINO,
                referencingPlatform.getArchitecture());
        if (latestArduinoPlatform != null) {
            ret.putAll(getEnvVarPlatformFileTools(latestArduinoPlatform));
        }

        if (myReferencedPlatformCore == null) {
            //there is no referenced core so no need to do smart stuff
            ret.putAll(getEnvVarPlatformFileTools(referencingPlatform));
            return ret;
        }

        boolean jsonBasedPlatformManagement = !Preferences.getUseArduinoToolSelection();
        if (jsonBasedPlatformManagement) {
            // overrule the Arduino IDE way of working and use the json refereced tools
            ret.putAll(getEnvVarPlatformFileTools(referencingPlatform));
            return ret;
        }
        // standard arduino IDE way
        ret.putAll(getEnvVarPlatformFileTools(referencingPlatform));
        ret.putAll(getEnvVarPlatformFileTools(myReferencedPlatformCore));
        return ret;

    }

    /**
     * This method only returns environment variables with and without version
     * number
     * These are purely based on the tool dependencies
     * 
     * @param platformVersion
     * @return environment variables pointing to the tools used by the platform
     */
    private static Map<String, String> getEnvVarPlatformFileTools(ArduinoPlatformVersion platformVersion) {
        HashMap<String, String> vars = new HashMap<>();
        for (ArduinoPlatformTooldDependency tool : platformVersion.getToolsDependencies()) {
            IPath installPath = tool.getInstallPath();
            if (installPath.toFile().exists()) {
                String value = installPath.toOSString();
                String keyString = RUNTIME_TOOLS + tool.getName() + tool.getVersion() + DOT_PATH;
                vars.put(keyString, value);
                keyString = RUNTIME_TOOLS + tool.getName() + '-' + tool.getVersion() + DOT_PATH;
                vars.put(keyString, value);
                keyString = RUNTIME_TOOLS + tool.getName() + DOT_PATH;
                vars.put(keyString, value);
            }
        }
        return vars;
    }

    /**
     * Following post processing is done
     *
     *
     * The handling of the upload variables is done differently in arduino
     * than in Sloeber. This is taken care of here. for example the output of this
     * input
     * tools.avrdude.upload.pattern="{cmd.path}" "-C{config.path}" {upload.verbose}
     * is changed as if it were the output of this input
     * tools.avrdude.upload.pattern="{tools.avrdude.cmd.path}"
     * "-C{tools.avrdude.config.path}" {tools.avrdude.upload.verbose}
     *
     * if a programmer is selected different from default some extra actions
     * are done here so no special code is needed to handle programmers
     *
     * The build path for the core is {build.path}/core/core in sloeber
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
            int max = 10;
            for (int counter = 1; counter < max; counter++) {
                String hookVarName = hookName.replace(searchString, String.format(formatter, Integer.valueOf(counter)));
                if (null != vars.get(hookVarName)) { // $NON-NLS-1$
                    envVarString = envVarString + preSeparator + hookVarName + postSeparator;
                }
            }
            max = 100;
        }
        if (!envVarString.isEmpty()) {
            extraVars.put(varName, envVarString);
        }
        return extraVars;
    }

    public boolean isValid() {
        if (!myUserSelectedBoardsTxtFile.exists()) {
            return false;
        }
        File boardsFile = mySloeberBoardTxtFile.getLoadedFile();
        if (boardsFile == null) {
            return false;
        }
        return boardsFile.exists();
    }

    public void reloadTxtFile() {
        mySloeberBoardTxtFile.reloadTxtFile();

    }

    public Map<String, String> getAllMenus() {
        return mySloeberBoardTxtFile.getMenus();
    }

    public boolean isSSHUpload() {
        if (!isNetworkUpload()) {
            return false;
        }
        // This is a hardcoded fix. Not sure how to do better
        return myBoardID.equals("yun"); //$NON-NLS-1$
    }

}
