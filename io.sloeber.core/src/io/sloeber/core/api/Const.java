package io.sloeber.core.api;

import org.eclipse.core.runtime.IStatus;

/**
 * ArduinoConst only contains global strings used in sloeber.
 *
 * @author Jan Baeyens
 *
 */
@SuppressWarnings("nls")
public class Const {
    //Some private stuff as you should use other defines to get to this name
    private static final String ARDUINO = "arduino";

    // preference nodes
    public static final String NODE_ARDUINO = "io.sloeber.arduino";

    //for debug messages
    public static final int SLOEBER_STATUS_DEBUG = IStatus.CANCEL;

    // java stuff so I do not have to add all the time $NON-NLS-1$
    public static final String DOT = ".";
    public static final String SLACH = "/";
    public static final String FALSE = "FALSE";
    public static final String TRUE = "TRUE";
    public static final String COLON = ":";
    public static final String EMPTY = "";
    public static final String NEWLINE = "\n";
    public static final String EQUAL = "=";
    public static final String SPACE = " ";

    // arduino txt basic keys
    public static final String VARIANTS = "variants";
    public static final String VARIANT = "variant";
    public static final String CORE = "core";
    public static final String CORES = "cores";
    public static final String UPLOAD = "upload";
    public static final String PROGRAM = "program";
    public static final String TOOL = "tool";
    public static final String TOOLS = "tools";
    public static final String RUNTIME = "runtime";
    public static final String MENU = "menu";
    public static final String STEP = "step";
    public static final String PATTERN = "pattern";
    public static final String NAME = "name";
    public static final String HARDWARE = "hardware";
    public static final String PLATFORM = "platform";
    public static final String TXT = "txt";
    public static final String SOURCE = "source";
    public static final String COMPILER = "compiler";
    public static final String PRIVATE = "private";
    public static final String MANAGED = "Managed";

    public static final String NETWORK = "network";
    public static final String PORT = "port";
    public static final String AUTH = "auth";
    public static final String RECIPE = "recipe";
    public static final String BUILD = "build";
    public static final String SYSTEM = "system";
    public static final String COM_PORT = "com_port";

    public static final String PATH = "path";
    public static final String PROTOCOL = "protocol";
    public static final String VENDOR_ARDUINO = ARDUINO;

    // arduino txt pre and suffix
    public static final String NETWORK_PREFIX = "network_";
    public static final String REMOTE_SUFFIX = "_remote";

    // General stuff
    public static final String PLUGIN_ID = "io.sloeber.core";
    public static final String CORE_PLUGIN_ID = "io.sloeber.arduino.core";
    public static final String SLOEBER_NATURE_ID = "io.sloeber.arduinonature";
    public static final String KEY_LAST_USED_EXAMPLES = "Last used Examples";
    public static final String SLOEBER_HOME = "SLOEBER_HOME";
    public static final String RUNTIME_IDE_PATH = "runtime.ide.path";
    public static final String LOCAL = "local";

    // Folder and file Information
    public static final String ARDUINO_HARDWARE_FOLDER_NAME = HARDWARE;
    public static final String ARDUINO_CODE_FOLDER_NAME = CORE;
    public static final String ARDUINO_VARIANTS_FOLDER_NAME = VARIANTS;
    public static final String ARDUINO_LIBRARY_FOLDER_NAME = "libraries";
    public static final String SLOEBER_ARDUINO_FOLDER_NAME = ARDUINO;
    public static final String SLOEBER_VARIANT_FOLDER_NAME = VARIANT;
    public static final String SLOEBER_CODE_FOLDER_NAME = CORE;
    public static final String SLOEBER_LIBRARY_FOLDER_NAME = ARDUINO_LIBRARY_FOLDER_NAME;
    public static final String BOARDS_FILE_NAME = "boards" + DOT + TXT;
    public static final String PLATFORM_FILE_NAME = PLATFORM + DOT + TXT;

    public static final String SLOEBER_CFG = "sloeber.cfg";
    public static final String SLOEBER_PROJECT = ".sproject";
    public static final String LIBRARY_PROPERTIES = "library.properties";
    public static final String LIBRARY_DOT_A_LINKAGE = "dot_a_linkage";

    // Environment variable stuff
    public static final String ENV_KEY_SLOEBER_START = "sloeber" + DOT;

    public static final String ENV_KEY_UPLOAD_USE_1200BPS_TOUCH = UPLOAD + DOT + "use_1200bps_touch";
    public static final String ENV_KEY_WAIT_FOR_UPLOAD_PORT = UPLOAD + DOT + "wait_for_upload_port";
    public static final String ENV_KEY_NETWORK_PORT = NETWORK + DOT + PORT;
    public static final String ENV_KEY_NETWORK_AUTH = NETWORK + DOT + AUTH;
    public static final String ENV_KEY_NETWORK_PASSWORD = NETWORK + DOT + "password";
    public static final String ENV_KEY_UPLOAD_VERBOSE = UPLOAD + DOT + "verbose";
    public static final String ENV_KEY_BUILD_SOURCE_PATH = BUILD + DOT + SOURCE + DOT + PATH;
    public static final String ENV_KEY_BUILD_PATH = BUILD + DOT + PATH;
    public static final String ENV_KEY_BUILD_GENERIC_PATH = BUILD + DOT + "generic" + DOT + PATH;
    public static final String ENV_KEY_COMPILER_PATH = COMPILER + DOT + PATH;
    public static final String SLOEBER_MAKE_LOCATION = "MAKE_HOME";
    public static final String SLOEBER_AWK_LOCATION = ENV_KEY_SLOEBER_START + "awk.path";
    public static final String CONFIG = "Config";
    public static final String CONFIG_DOT = CONFIG + DOT;

    public static final String ENV_KEY_BUILD_MCU = BUILD + DOT + "mcu";
    public static final String ENV_KEY_BUILD_COMPILER_C_ELF_FLAGS = COMPILER + ".c.elf.flags";
    public static final String PROGRAM_TOOL = PROGRAM + DOT + TOOL;
    public static final String UPLOAD_TOOL = UPLOAD + DOT + TOOL;

    // link time variables
    public static final String EXTRA_TIME_UTC = "extra.time.UTC";
    public static final String EXTRA_TIME_LOCAL = "extra.time.local";
    public static final String EXTRA_TIME_ZONE = "extra.time.zone";
    public static final String EXTRA_TIME_DTS = "extra.time.DTS";

    // Actions
    public static final String RECIPE_C_to_O = RECIPE + DOT + "c.o" + DOT + PATTERN;
    public static final String RECIPE_CPP_to_O = RECIPE + DOT + "cpp.o" + DOT + PATTERN;
    public static final String RECIPE_S_to_O = RECIPE + DOT + "S.o" + DOT + PATTERN;
    public static final String RECIPE_OBJCOPY = RECIPE + DOT + "objcopy";
    public static final String RECIPE_SIZE = RECIPE + DOT + "size" + DOT + PATTERN;
    public static final String RECIPE_AR = RECIPE + DOT + "ar" + DOT + PATTERN;
    public static final String RECIPE_C_COMBINE = RECIPE + DOT + "c.combine" + DOT + PATTERN;

    public static final String CODAN = "CODAN";
    public static final String CODAN_C_to_O = RECIPE + DOT + "c.o" + DOT + CODAN;
    public static final String CODAN_CPP_to_O = RECIPE + DOT + "cpp.o" + DOT + CODAN;

    public static final String SLOEBER_OBJCOPY = ENV_KEY_SLOEBER_START + "objcopy";

    public static final String RUNTIME_TOOLS = RUNTIME + DOT + TOOLS + DOT;
    public static final String DOT_PATH = DOT + PATH;

    public static final String AVR = "avr";
    public static final String SAM = "sam";
    public static final String SAMD = "samd";
    public static final String UNO = "uno";

    public static final String SRC_FODER = "src";
    public static final String eXAMPLES_FODER = "examples";
    public static final String EXAMPLES_FODER = "Examples";

    public static final String JSSC_SERIAL_FILTER_PATTERN_KEY = "jssc_serial_filter_pattern";
    public static final String JSSC_MAC_DEFAULT_FILTER_PATTERN = "^cu\\..*(UART|serial|usb).*";

    public static final String RELEASE = "Release";

    public static final short PLOTTER_START_DATA = (short) 0xCDAB;// This is the
    // 205 171 or
    // -85 -51 flag
    // that
    // indicates
    // plotter data
    // is following
    // least
    // significant
    // first
    // 0xCDAB;
}
