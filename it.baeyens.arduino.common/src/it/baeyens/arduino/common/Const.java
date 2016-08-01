package it.baeyens.arduino.common;

/**
 * ArduinoConst only contains global strings used in the eclipse plugin.
 * 
 * @author Jan Baeyens
 * 
 */
@SuppressWarnings("nls")
public class Const {
    // java stuff so I do not have to add all the time $NON-NLS-1$
    public static final String EMPTY_STRING = "";
    public static final String DOT = ".";
    public static final String SLACH = "/";
    public static final String COLON = ":";
    public static final String SPACE = " ";
    public static final String FALSE = "FALSE";
    public static final String TRUE = "TRUE";
    public static final String NEWLINE = "\n";
    public static final String RETURN = "\r";

    // General stuff
    public static final String PLUGIN_START = "it.baeyens.";
    public static final String CORE_PLUGIN_ID = PLUGIN_START + "arduino.core";
    public static final String NETWORK = "NETWORK";

    // prefix to be added to the arduino environment
    protected static final String UPLOAD_PORT_PREFIX_WIN = "-P\\\\.\\";
    protected static final String UPLOAD_PORT_PREFIX_LINUX = "-P";
    protected static final String UPLOAD_PORT_PREFIX_MAC = "-P";

    // natures
    public static final String CNATURE_ID = "org.eclipse.cdt.core.cnature";
    public static final String CCNATURE_ID = "org.eclipse.cdt.core.ccnature";
    public static final String BUILD_NATURE_ID = "org.eclipse.cdt.managedbuilder.core.managedBuildNature";
    public static final String SCANNER_NATURE_ID = "org.eclipse.cdt.managedbuilder.core.ScannerConfigNature";
    public static final String ARDUINO_NATURE_ID = PLUGIN_START + "arduinonature";

    // preference nodes
    public static final String NODE_ARDUINO = PLUGIN_START + "arduino";

    // Actions
    public static final String ACTION_UPLOAD = "UPLOAD";
    public static final String ACTION_PROGRAM = "PROGRAM";
    public static final String ACTION_C_to_O = "RECIPE.C.O";
    public static final String ACTION_CPP_to_O = "RECIPE.CPP.O";
    public static final String ACTION_S_to_O = "RECIPE.S.O";
    public static final String ACTION_OBJCOPY_to_HEX = "RECIPE.OBJCOPY.HEX";
    public static final String ACTION_OBJCOPY_to_EEP = "RECIPE.OBJCOPY.EEP";
    public static final String ACTION_SIZE = "RECIPE.SIZE";
    public static final String ACTION_AR = "RECIPE.AR";
    public static final String ACTION_C_COMBINE = "RECIPE.C.COMBINE";
    public static final String UPLOAD_CLASS = "UPLOAD_CLASS";
    public static final String UPLOAD_CLASS_DEFAULT = "arduinoUploader";

    // Describers
    public static final String ENV_PROTOCOL = "PROTOCOL";
    public static final String ENV_TOOL = "TOOL";
    public static final String ENV_PATTERN = "PATTERN";

    // preference keys
    public static final String KEY_PRIVATE_LIBRARY_PATHS = "Private Library Path";
    public static final String KEY_PRIVATE_HARDWARE_PATHS = "Private hardware Path";
    public static final String KEY_MANAGER_DOWNLOAD_LOCATION = "arduino Manager downloadlocation";
    public static final String KEY_MANAGER_BOARD_URLS = "Arduino Manager board Urls";

    // properties keys
    public static final String KEY_LAST_USED_BOARD = "Board";
    public static final String KEY_LAST_USED_COM_PORT = "Upload port";
    public static final String KEY_LAST_USED_UPLOAD_PROTOCOL = ACTION_UPLOAD + DOT + ENV_PROTOCOL;
    public static final String KEY_LAST_USED_BOARDS_FILE = "Boards file";
    public static final String KEY_LAST_USED_BOARD_MENU_OPTIONS = "Board custom option selections";
    public static final String KEY_LAST_USED_SCOPE_FILTER_MENU_OPTION = "Board scope filter on off";
    public static final String KEY_LAST_USED_EXAMPLES = "Last used Examples";
    public static final String KEY_UPDATE_JASONS = "Update jsons files";

    // Serial monitor keys
    public static final String KEY_SERIAL_RATE = "Serial monitor last selected rate";
    public static final String KEY_SERIAL_PORT = "Serial monitor last selected Port";
    public static final String KEY_RXTX_LAST_USED_LINE_INDES = "Serial Monitor Last Used Line Ending index";
    public static final String KEY_RXTX_LAST_USED_AUTOSCROLL = "Serial Monitor Last Used auto scroll setting";
    // Folder Information
    public static final String LIBRARY_PATH_SUFFIX = "libraries";
    public static final String DOWNLOADS_FOLDER = "downloads";
    public static final String ARDUINO_HARDWARE_FOLDER_NAME = "hardware";
    public static final String ARDUINO_CODE_FOLDER_NAME = "core";
    public static final String ARDUINO_CORE_FOLDER_NAME = "cores";
    public static final String DEFAULT = "Default";
    public static final String BOARDS_FILE_NAME = "boards.txt";
    public static final String PLATFORM_FILE_NAME = "platform.txt";
    public static final String VARIANTS_FOLDER_NAME = "variants";
    public static final String PACKAGES_FOLDER_NAME = "packages";

    // tags to interpret the arduino txt config files
    public static final String TXT_NAME_KEY_TAG = "name";
    public static final String UPLOAD_TOOL_TEENSY = "teensy_reboot";
    public static final String UPLOAD_SSH = "ssh upload";
    public static final String MENU = "menu";

    public static final String KEY_BUILD_BEFORE_UPLOAD_OPTION = "Build before upload option";
    public static final String KEY_OPEN_SERIAL_WITH_MONITOR = "Open serial connections with the monitor";
    public static final String KEY_AUTO_IMPORT_LIBRARIES = "Automatically import libraries";

    public static final String ERASE_START = "A" + DOT;

    public static final String ENV_KEY_PROGRAMMERS_START = ERASE_START + "PROGRAMMERS.";
    public static final String WORKSPACE_LIB_FOLDER = LIBRARY_PATH_SUFFIX + "/";
    public static final String ARDUINO_IDE_VERSION = "ArduinoIDEVersion";
    public static final String ENV_KEY_NAME = ERASE_START + "NAME";
    public static final String ENV_KEY_VERSION = ERASE_START + "VERSION";

    public static final String ENV_KEY_BUILD_VARIANT = ERASE_START + "BUILD.VARIANT";
    public static final String ENV_KEY_COMPILER_PATH = ERASE_START + "COMPILER.PATH";
    public static final String ENV_KEY_BUILD_SYSTEM_PATH = ERASE_START + "BUILD.SYSTEM.PATH";
    public static final String ENV_KEY_BUILD_GENERIC_PATH = ERASE_START + "BUILD.GENERIC.PATH";
    public static final String ENV_KEY_SOFTWARE = ERASE_START + "SOFTWARE";
    public static final String ENV_KEY_ARCHITECTURE = ERASE_START + "ARCHITECTURE";
    public static final String ENV_KEY_BUILD_ARCH = ERASE_START + "BUILD.ARCH";
    public static final String ENV_KEY_HARDWARE_PATH = ERASE_START + "RUNTIME.HARDWARE.PATH";
    public static final String ENV_KEY_PLATFORM_PATH = ERASE_START + "RUNTIME.PLATFORM.PATH";

    public static final String ENV_KEY_BUILD_PATH = ERASE_START + "BUILD.PATH";
    public static final String ENV_KEY_BUILD_PROJECT_NAME = ERASE_START + "BUILD.PROJECT_NAME";
    public static final String ENV_KEY_UPLOAD_USE_1200BPS_TOUCH = ERASE_START + "UPLOAD.USE_1200BPS_TOUCH";
    public static final String ENV_KEY_UPLOAD_DISABLE_FLUSHING = ERASE_START + "UPLOAD.DISABLE_FLUSHING";
    public static final String ENV_KEY_WAIT_FOR_UPLOAD_PORT = ERASE_START + "UPLOAD.WAIT_FOR_UPLOAD_PORT";
    public static final String ENV_KEY_RESET_BEFORE_UPLOAD = ERASE_START + "UPLOAD.FORCE_RESET_BEFORE_UPLOAD";
    public static final String ENV_KEY_NETWORK_PORT = ERASE_START + "NETWORK.PORT";
    public static final String ENV_KEY_NETWORK_AUTH = ERASE_START + "NETWORK.AUTH";

    public static final String ENV_KEY_BUILD_CORE = ERASE_START + "BUILD.CORE";

    public static final String ENV_KEY_USE_ARCHIVER = ERASE_START + "BUILD.USE_ARCHIVER";
    public static final String ENV_KEY_SERIAL_PORT = ERASE_START + "SERIAL.PORT";
    public static final String ENV_KEY_SERIAL_PORT_FILE = ERASE_START + "SERIAL.PORT.FILE";

    public static final String ENV_KEY_JANTJE_REFERENCED_PLATFORM_FILE = ERASE_START
	    + "JANTJE.REFERENCED_PLATFORM_FILE";
    public static final String ENV_KEY_JANTJE_REFERENCED_CORE = ERASE_START + "JANTJE.REFERENCED.CORE.FILE";
    public static final String ENV_KEY_JANTJE_REFERENCED_VARIANT_PATH = ERASE_START + "JANTJE.BUILD.VARIANT.PATH";
    public static final String ENV_KEY_JANTJE_BUILD_CORE = ERASE_START + "JANTJE.BUILD_CORE";
    public static final String ENV_KEY_JANTJE_BUILD_VARIANT = ERASE_START + "JANTJE.BUILD_VARIANT";

    public static final String ENV_KEY_JANTJE_START = "JANTJE.";
    public static final String ENV_KEY_JANTJE_WARNING_LEVEL = ENV_KEY_JANTJE_START + "WARNING_LEVEL";
    public static final String ENV_KEY_JANTJE_SIZE_COMMAND = ERASE_START + "ALT_SIZE_COMMAND";
    public static final String ENV_KEY_JANTJE_SIZE_SWITCH = ENV_KEY_JANTJE_START + "SIZE.SWITCH";
    public static final String ENV_KEY_JANTJE_BOARDS_FILE = ENV_KEY_JANTJE_START + "BOARDS_FILE";
    public static final String ENV_KEY_JANTJE_PLATFORM_FILE = ENV_KEY_JANTJE_START + "PLATFORM_FILE";
    public static final String ENV_KEY_JANTJE_COM_PORT = ENV_KEY_JANTJE_START + "COM_PORT";
    public static final String ENV_KEY_JANTJE_BOARD_NAME = ENV_KEY_JANTJE_START + "BOARD_NAME";

    public static final String ENV_KEY_JANTJE_ADDITIONAL_COMPILE_OPTIONS = ENV_KEY_JANTJE_START + "EXTRA.COMPILE";
    public static final String ENV_KEY_JANTJE_ADDITIONAL_C_COMPILE_OPTIONS = ENV_KEY_JANTJE_START + "EXTRA.C.COMPILE";
    public static final String ENV_KEY_JANTJE_ADDITIONAL_CPP_COMPILE_OPTIONS = ENV_KEY_JANTJE_START
	    + "EXTRA.CPP.COMPILE";
    public static final String ENV_KEY_JANTJE_PACKAGE_ID = ENV_KEY_JANTJE_START + "PACKAGE_ID";
    public static final String ENV_KEY_JANTJE_ARCITECTURE_ID = ENV_KEY_JANTJE_START + "ARCHITECTURE_ID";
    public static final String ENV_KEY_JANTJE_BOARD_ID = ENV_KEY_JANTJE_START + "BOARD_ID";
    public static final String ENV_KEY_JANTJE_PACKAGE_NAME = ENV_KEY_JANTJE_START + "PACKAGE.NAME";
    public static final String ENV_KEY_JANTJE_MAKE_LOCATION = ENV_KEY_JANTJE_START + "MAKE_LOCATION";
    //
    // template Sketch information

    public static final String ENV_KEY_JANTJE_SKETCH_TEMPLATE_FOLDER = ENV_KEY_JANTJE_START + "TEMPLATE_FOLDER";
    public static final String ENV_KEY_JANTJE_SKETCH_TEMPLATE_USE_DEFAULT = ENV_KEY_JANTJE_START
	    + "TEMPLATE_USE_DEFAULT";

    public static final String ENV_KEY_WARNING_LEVEL_OFF = " -w ";
    public static final String ENV_KEY_WARNING_LEVEL_ON = " -Wall ";

    public static final String ENV_KEY_GNU_SERIAL_PORTS = "gnu.io.rxtx.SerialPorts";
    public static final String ENV_VALUE_GNU_SERIAL_PORTS_LINUX = "/dev/ttyACM0:/dev/ttyACM1:/dev/ttyACM2:/dev/ttyACM3:/dev/ttyUSB0::/dev/ttyUSB1::/dev/ttyUSB2::/dev/ttyUSB3::/dev/ttyUSB4";
    // scope stuff
    public static final short SCOPE_START_DATA = (short) 0xCDAB;// This is the
								// 205 171 or
								// -85 -51 flag
								// that
								// indicates
								// scope data is
								// following
								// least
								// significant
								// first 0xCDAB;
    public static final String EXAMPLE_FOLDER_NAME = "examples";

    public static final String PRE_PROCESSING_BOARDS_TXT = "pre_processing_boards.txt";
    public static final String POST_PROCESSING_BOARDS_TXT = "post_processing_boards.txt";
    public static final String PRE_PROCESSING_PLATFORM_TXT = "pre_processing_platform.txt";
    public static final String POST_PROCESSING_PLATFORM_TXT = "post_processing_platform.txt";
    public static final String DEFINE_IN_ECLIPSE = "__IN_ECLIPSE__";

    /**
     * given a action return the environment key that matches it's protocol
     * 
     * @param action
     * @return the environment variable key to find the protocol
     */
    public static String get_ENV_KEY_PROTOCOL(String action) {
	return ERASE_START + action.toUpperCase() + DOT + ENV_PROTOCOL;
    }

    /**
     * given a action return the environment key that matches it's tool
     * 
     * @param action
     * @return the environment variable key to find the tool
     */
    public static String get_ENV_KEY_TOOL(String action) {
	return ERASE_START + action.toUpperCase() + DOT + ENV_TOOL;
    }

    /**
     * given a action return the environment key that matches it's recipe
     * 
     * @param action
     * @return he environment variable key to find the recipe
     */
    public static String get_ENV_KEY_RECIPE(String action) {
	return ERASE_START + action.toUpperCase() + DOT + ENV_PATTERN;
    }

    /**
     * given a action and a tool return the environment key that matches it's
     * recipe
     * 
     * @param action
     * @return he environment variable key to find the recipe
     */
    public static String get_ENV_KEY_RECIPE(String tool, String action) {
	return ERASE_START + "TOOLS" + DOT + tool.toUpperCase() + DOT + action.toUpperCase() + DOT + ENV_PATTERN;
    }

    public static String get_Jantje_KEY_PROTOCOL(String action) {
	return ENV_KEY_JANTJE_START + action.toUpperCase() + DOT + ENV_PROTOCOL;
    }

    public static String get_Jantje_KEY_RECIPE(String action) {
	return ENV_KEY_JANTJE_START + action.toUpperCase() + DOT + ENV_PATTERN;
    }

}
