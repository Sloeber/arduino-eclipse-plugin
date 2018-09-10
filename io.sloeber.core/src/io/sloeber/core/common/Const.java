package io.sloeber.core.common;

/**
 * ArduinoConst only contains global strings used in the eclipse plugin.
 *
 * @author Jan Baeyens
 *
 */
@SuppressWarnings("nls")
public class Const {
	// java stuff so I do not have to add all the time $NON-NLS-1$
	public static final String DOT = ".";
	public static final String SLACH = "/";
	public static final String FALSE = "FALSE";
	public static final String TRUE = "TRUE";
	public static final String MENU = "menu";

	// General stuff
	public static final String CORE_PLUGIN_ID = "io.sloeber.arduino.core";

	// Actions
	public static final String ACTION_C_to_O = "RECIPE.C.O";
	public static final String ACTION_CPP_to_O = "RECIPE.CPP.O";
	public static final String ACTION_S_to_O = "RECIPE.S.O";
	public static final String ACTION_OBJCOPY_to_HEX = "RECIPE.OBJCOPY.HEX";
	public static final String ACTION_OBJCOPY_to_EEP = "RECIPE.OBJCOPY.EEP";
	public static final String ACTION_SIZE = "RECIPE.SIZE";
	public static final String ACTION_AR = "RECIPE.AR";
	public static final String ACTION_C_COMBINE = "RECIPE.C.COMBINE";
	//public static final String UPLOAD_CLASS = "UPLOAD_CLASS";
	//public static final String UPLOAD_CLASS_DEFAULT = "arduinoUploader";

	// properties keys

	public static final String KEY_LAST_USED_EXAMPLES = "Last used Examples";

	// Folder Information

	public static final String ARDUINO_HARDWARE_FOLDER_NAME = "hardware";
	public static final String ARDUINO_CODE_FOLDER_NAME = "core";

	public static final String ARDUINO_CODE_FOLDER_PATH =ARDUINO_CODE_FOLDER_NAME+ "/core";

	public static final String BOARDS_FILE_NAME = "boards.txt";

	public static final String VARIANTS_FOLDER_NAME = "variants";
	public static final String ARDUINO_VARIANT_FOLDER_PATH =ARDUINO_CODE_FOLDER_NAME+ "/variant";

	// tags to interpret the arduino txt config files

	public static final String ERASE_START = "A" + DOT;

	public static final String ENV_KEY_UPLOAD_USE_1200BPS_TOUCH = ERASE_START + "UPLOAD.USE_1200BPS_TOUCH";

	public static final String ENV_KEY_WAIT_FOR_UPLOAD_PORT = ERASE_START + "UPLOAD.WAIT_FOR_UPLOAD_PORT";
	public static final String ENV_KEY_RESET_BEFORE_UPLOAD = ERASE_START + "UPLOAD.FORCE_RESET_BEFORE_UPLOAD";
	public static final String ENV_KEY_NETWORK_PORT = ERASE_START + "NETWORK.PORT";
	public static final String ENV_KEY_NETWORK_AUTH = ERASE_START + "NETWORK.AUTH";

	public static final String ENV_KEY_USE_ARCHIVER = ERASE_START + "BUILD.USE_ARCHIVER";

	public static final String ENV_KEY_JANTJE_START = "JANTJE.";

	public static final String ARDUINO_NATURE_ID = "io.sloeber.arduinonature";

}
