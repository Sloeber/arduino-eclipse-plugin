package io.sloeber.core.common;

import org.eclipse.core.runtime.Platform;

/**
 * ArduinoConst only contains global strings used in SLOEBER.
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
	public static final String COLON = ":";

	// arduino txt basic keys
	public static final String VARIANT = "variant";
	public static final String CORE = "core";
	public static final String CORES = "cores";
	public static final String UPLOAD = "upload";
	public static final String PROGRAM = "program";
	public static final String TOOL = "tool";
	public static final String TOOLS = "tools";
	public static final String MENU = "menu";
	public static final String STEP = "step";
	public static final String PATTERN = "pattern";
	public static final String NAME = "name";
	public static final String HARDWARE = "hardware";
	public static final String NETWORK = "network";
	public static final String PORT = "port";
	public static final String AUTH = "auth";
	public static final String RECIPE = "recipe";
	public static final String BUILD = "build";
	public static final String COM_PORT = "com_port";
	public static final String ARDUINO = "arduino";
	public static final String PATH = "path";
	public static final String PROTOCOL = "protocol";

	// arduino txt pre and suffix
	public static final String NETWORK_PREFIX = "network_";
	public static final String REMOTE_SUFFIX = "_remote";

	// General stuff
	public static final String CORE_PLUGIN_ID = "io.sloeber.arduino.core";
	public static final String ARDUINO_NATURE_ID = "io.sloeber.arduinonature";
	public static final String KEY_LAST_USED_EXAMPLES = "Last used Examples";
	public static final String ECLIPSE_HOME = "eclipse_home";

	// Folder and file Information
	public static final String ARDUINO_HARDWARE_FOLDER_NAME = HARDWARE;
	public static final String ARDUINO_CODE_FOLDER_NAME = CORE;
	public static final String BOARDS_FILE_NAME = "boards.txt";
	public static final String PLATFORM_FILE_NAME = "platform.txt";
	public static final String VARIANTS_FOLDER_NAME = "variants";
	public static final String LIBRARY_PATH_SUFFIX = "libraries";
	public static final String ARDUINO_VARIANT_FOLDER_PATH = ARDUINO_CODE_FOLDER_NAME + SLACH + VARIANT;
	public static final String ARDUINO_CODE_FOLDER_PATH = ARDUINO_CODE_FOLDER_NAME + SLACH + CORE;

	// Environment variable stuff
	public static final String ENV_KEY_JANTJE_START = "JANTJE" + DOT;
	public static final String ERASE_START = "A" + DOT;
	public static final String A_TOOLS = ERASE_START + TOOLS + DOT;

	public static final String ENV_KEY_UPLOAD_USE_1200BPS_TOUCH = ERASE_START + UPLOAD + DOT + "use_1200bps_touch";
	public static final String ENV_KEY_WAIT_FOR_UPLOAD_PORT = ERASE_START + UPLOAD + DOT + "wait_for_upload_port";
	public static final String ENV_KEY_RESET_BEFORE_UPLOAD = ERASE_START + UPLOAD + DOT + "force_reset_before_upload";
	public static final String ENV_KEY_NETWORK_PORT = ERASE_START + NETWORK + DOT + PORT;
	public static final String ENV_KEY_NETWORK_AUTH = ERASE_START + NETWORK + DOT + AUTH;
	public static final String ENV_KEY_NETWORK_PASSWORD = ERASE_START + NETWORK + DOT + "password";
	public static final String ENV_KEY_UPLOAD_VERBOSE = ERASE_START + UPLOAD + DOT + "verbose";

	public static final String ENV_KEY_USE_ARCHIVER = ERASE_START + BUILD + DOT + "use_archiver";
	public static final String ENV_KEY_BUILD_MCU = ERASE_START + "build.mcu";
	public static final String ENV_KEY_BUILD_COMPILER_C_ELF_FLAGS = ERASE_START + "compiler.c.elf.flags";
	public static final String PROGRAM_TOOL = ERASE_START + PROGRAM + DOT + TOOL;
	public static final String UPLOAD_TOOL = ERASE_START + UPLOAD + DOT + TOOL;


	// link time variables
	public static final String EXTRA_TIME_UTC = "A.extra.time.UTC";
	public static final String EXTRA_TIME_LOCAL = "A.extra.time.local";
	public static final String EXTRA_TIME_ZONE = "A.extra.time.zone";
	public static final String EXTRA_TIME_DTS = "A.extra.time.DTS";

	// Actions
	public static final String RECIPE_C_to_O = ERASE_START + RECIPE + DOT + "c.o" + DOT + PATTERN;
	public static final String RECIPE_CPP_to_O = ERASE_START + RECIPE + DOT + "cpp.o" + DOT + PATTERN;
	public static final String RECIPE_S_to_O = ERASE_START + RECIPE + DOT + "S.o" + DOT + PATTERN;
	public static final String RECIPE_OBJCOPY = ERASE_START + RECIPE + DOT + "objcopy";
//	public static final String RECIPE_OBJCOPY_to_HEX = RECIPE_OBJCOPY + ".hex" + DOT + PATTERN;
//	public static final String RECIPE_OBJCOPY_to_EEP = RECIPE_OBJCOPY + ".eep" + DOT + PATTERN;
	public static final String RECIPE_SIZE = ERASE_START + RECIPE + DOT + "size" + DOT + PATTERN;
	public static final String RECIPE_AR = ERASE_START + RECIPE + DOT + "ar" + DOT + PATTERN;
	public static final String RECIPE_C_COMBINE = ERASE_START + RECIPE + DOT + "c.combine" + DOT + PATTERN;

	public static final String JANTJE_OBJCOPY = ENV_KEY_JANTJE_START + "objcopy";
	// public static final String UPLOAD_CLASS = "UPLOAD_CLASS";
	// public static final String UPLOAD_CLASS_DEFAULT = "arduinoUploader";

	public static final boolean isWindows = Platform.getOS().equals(Platform.OS_WIN32);
	public static final boolean isLinux = Platform.getOS().equals(Platform.OS_LINUX);
	public static final boolean isMac = Platform.getOS().equals(Platform.OS_MACOSX);

	public enum OS {
		WINDOWS, LINUX, MAC, UNSUPPORTED
	}

	public static final OS os = getOS();

	private static OS getOS() {
		switch (Platform.getOS()) {
		case Platform.OS_WIN32:
			return OS.WINDOWS;
		case Platform.OS_MACOSX:
			return OS.MAC;
		case Platform.OS_LINUX:
			return OS.LINUX;

		}
		return OS.UNSUPPORTED;
	}
}
