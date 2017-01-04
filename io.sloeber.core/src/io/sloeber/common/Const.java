package io.sloeber.common;

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
	public static final String MENU = "menu";

	// General stuff
	public static final String CORE_PLUGIN_ID = "io.sloeber.arduino.core";

	// Actions
	public static final String ACTION_UPLOAD = "UPLOAD";

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

	// properties keys

	public static final String KEY_LAST_USED_EXAMPLES = "Last used Examples";

	// Folder Information
	public static final String LIBRARY_PATH_SUFFIX = "libraries";

	public static final String ARDUINO_HARDWARE_FOLDER_NAME = "hardware";
	public static final String ARDUINO_CODE_FOLDER_NAME = "core";

	public static final String BOARDS_FILE_NAME = "boards.txt";
	public static final String PLATFORM_FILE_NAME = "platform.txt";
	public static final String VARIANTS_FOLDER_NAME = "variants";
	public static final String PACKAGES_FOLDER_NAME = "packages";

	// tags to interpret the arduino txt config files

	public static final String UPLOAD_TOOL_TEENSY = "teensy_reboot";
	public static final String UPLOAD_SSH = "ssh upload";

	public static final String ERASE_START = "A" + DOT;

	public static final String WORKSPACE_LIB_FOLDER = LIBRARY_PATH_SUFFIX + "/";

	public static final String ENV_KEY_UPLOAD_USE_1200BPS_TOUCH = ERASE_START + "UPLOAD.USE_1200BPS_TOUCH";

	public static final String ENV_KEY_WAIT_FOR_UPLOAD_PORT = ERASE_START + "UPLOAD.WAIT_FOR_UPLOAD_PORT";
	public static final String ENV_KEY_RESET_BEFORE_UPLOAD = ERASE_START + "UPLOAD.FORCE_RESET_BEFORE_UPLOAD";
	public static final String ENV_KEY_NETWORK_PORT = ERASE_START + "NETWORK.PORT";
	public static final String ENV_KEY_NETWORK_AUTH = ERASE_START + "NETWORK.AUTH";

	public static final String ENV_KEY_USE_ARCHIVER = ERASE_START + "BUILD.USE_ARCHIVER";
	public static final String ENV_KEY_SERIAL_PORT = ERASE_START + "SERIAL.PORT";
	public static final String ENV_KEY_SERIAL_PORT_FILE = ERASE_START + "SERIAL.PORT.FILE";

	public static final String ENV_KEY_JANTJE_START = "JANTJE.";

	public static final String ENV_KEY_JANTJE_SIZE_COMMAND = ERASE_START + "ALT_SIZE_COMMAND";
	public static final String ENV_KEY_JANTJE_SIZE_SWITCH = ENV_KEY_JANTJE_START + "SIZE.SWITCH";
	public static final String ENV_KEY_JANTJE_BOARDS_FILE = ENV_KEY_JANTJE_START + "BOARDS_FILE";
	public static final String ENV_KEY_JANTJE_PLATFORM_FILE = ENV_KEY_JANTJE_START + "PLATFORM_FILE";
	public static final String ENV_KEY_JANTJE_CORE_REFERENCED_PLATFORM = ERASE_START + ENV_KEY_JANTJE_START
			+ "CORE.REFERENCED.PLATFORM"; //$NON-NLS-1$
	public static final String ENV_KEY_JANTJE_UPLOAD_PORT = ENV_KEY_JANTJE_START + "COM_PORT";
	public static final String ENV_KEY_JANTJE_BOARD_NAME = ENV_KEY_JANTJE_START + "BOARD_NAME";

	public static final String ENV_KEY_JANTJE_ADDITIONAL_COMPILE_OPTIONS = ENV_KEY_JANTJE_START + "EXTRA.COMPILE";
	public static final String ENV_KEY_JANTJE_ADDITIONAL_C_COMPILE_OPTIONS = ENV_KEY_JANTJE_START + "EXTRA.C.COMPILE";
	public static final String ENV_KEY_JANTJE_ADDITIONAL_CPP_COMPILE_OPTIONS = ENV_KEY_JANTJE_START
			+ "EXTRA.CPP.COMPILE";
	public static final String ENV_KEY_JANTJE_PACKAGE_ID = ENV_KEY_JANTJE_START + "PACKAGE_ID";
	public static final String ENV_KEY_JANTJE_ARCITECTURE_ID = ENV_KEY_JANTJE_START + "ARCHITECTURE_ID";
	public static final String ENV_KEY_JANTJE_BOARD_ID = ENV_KEY_JANTJE_START + "BOARD_ID";

	//
	// template Sketch information

	public static final String ENV_KEY_JANTJE_SKETCH_TEMPLATE_FOLDER = ENV_KEY_JANTJE_START + "TEMPLATE_FOLDER";
	public static final String ENV_KEY_JANTJE_SKETCH_TEMPLATE_USE_DEFAULT = ENV_KEY_JANTJE_START
			+ "TEMPLATE_USE_DEFAULT";

	// TOFIX I think the fix below for unix users is no longer needed and we no
	// longer use the rxtx dll
	public static final String ENV_KEY_GNU_SERIAL_PORTS = "gnu.io.rxtx.SerialPorts";
	public static final String ENV_VALUE_GNU_SERIAL_PORTS_LINUX = "/dev/ttyACM0:/dev/ttyACM1:/dev/ttyACM2:/dev/ttyACM3:/dev/ttyUSB0::/dev/ttyUSB1::/dev/ttyUSB2::/dev/ttyUSB3::/dev/ttyUSB4";

	static final String EXAMPLE_FOLDER_NAME = "examples";

	public static final String ARDUINO_NATURE_ID = "io.sloeber.arduinonature";

}
