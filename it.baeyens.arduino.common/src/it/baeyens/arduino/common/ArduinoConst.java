package it.baeyens.arduino.common;

/**
 * ArduinoConst only contains global strings used in the eclipse plugin.
 * 
 * @author Jan Baeyens
 * 
 */
public class ArduinoConst {
    // General stuff
    public static final String PluginStart = "it.baeyens."; //$NON-NLS-1$
    public static final String CORE_PLUGIN_ID = PluginStart + "arduino.core"; //$NON-NLS-1$

    // prefix to be added to the arduino environment
    protected static final String UploadPortPrefix_WIN = "-P\\\\.\\"; //$NON-NLS-1$
    protected static final String UploadPortPrefix_LINUX = "-P"; //$NON-NLS-1$
    protected static final String UploadPortPrefix_MAC = "-P"; //$NON-NLS-1$

    // natures
    public static final String Cnatureid = "org.eclipse.cdt.core.cnature"; //$NON-NLS-1$
    public static final String CCnatureid = "org.eclipse.cdt.core.ccnature"; //$NON-NLS-1$
    public static final String Buildnatureid = "org.eclipse.cdt.managedbuilder.core.managedBuildNature";//$NON-NLS-1$
    public static final String Scannernatureid = "org.eclipse.cdt.managedbuilder.core.ScannerConfigNature";//$NON-NLS-1$
    public static final String ArduinoNatureID = PluginStart + "arduinonature";//$NON-NLS-1$

    // preference nodes
    public static final String NODE_ARDUINO = PluginStart + "arduino";//$NON-NLS-1$

    // preference keys
    public static final String KEY_ARDUINO_IDE_VERSION = "Arduino IDE Version";//$NON-NLS-1$
    public static final String KEY_ARDUINOPATH = "Arduino Path";//$NON-NLS-1$
    public static final String KEY_PRIVATE_LIBRARY_PATH = "Private Library Path";//$NON-NLS-1$
    public static final String KEY_PRIVATE_HARDWARE_PATH = "Private hardware Path";//$NON-NLS-1$
    public static final String KEY_PREFERENCE_MODIFICATION_STAMP = "Arduino IDE preference.txt time stamp";//$NON-NLS-1$

    // properties keys
    public static final String KEY_LAST_USED_ARDUINOBOARD = "Arduino Board";//$NON-NLS-1$
    public static final String KEY_LAST_USED_COM_PORT = "Arduino Port";//$NON-NLS-1$
    public static final String KEY_LAST_USED_PROGRAMMER = "Arduino Programmer";//$NON-NLS-1$
    public static final String KEY_LAST_USED_ARDUINO_BOARDS_FILE = "Arduino boards file";//$NON-NLS-1$
    public static final String KEY_LAST_USED_ARDUINO_MENU_OPTIONS = "Arduino Custom Option Selections";//$NON-NLS-1$
    public static final String KEY_LAST_USED_SCOPE_FILTER_MENU_OPTION = "Arduino scope filter on off";//$NON-NLS-1$

    // Serial monitor keys
    public static final String KEY_SERIAlRATE = "Serial monitor last selected rate";//$NON-NLS-1$
    public static final String KEY_SERIAlPORT = "Serial monitor last selected Port";//$NON-NLS-1$
    public static final String KEY_RXTX_LAST_USED_LINE_INDES = "Serial Monitor Last Used Line Ending index";//$NON-NLS-1$
    public static final String KEY_RXTX_LAST_USED_AUTOSCROLL = "Serial Monitor Last Used auto scroll setting";//$NON-NLS-1$
    // Folder Information
    public static final String LIBRARY_PATH_SUFFIX = "libraries";//$NON-NLS-1$
    public static final String ARDUINO_HARDWARE_FOLDER_NAME = "hardware";//$NON-NLS-1$
    public static final String ARDUINO_CORE_FOLDER_NAME = "cores";//$NON-NLS-1$
    public static final String DEFAULT = "Default";//$NON-NLS-1$
    public static final String BOARDS_FILE_NAME = "boards.txt";//$NON-NLS-1$
    public static final String PLATFORM_FILE_NAME = "platform.txt";//$NON-NLS-1$
    public static final String LIB_VERSION_FILE = "lib/version.txt";//$NON-NLS-1$
    public static final String ARDUINO_VARIANTS_FOLDER_NAME = "variants";//$NON-NLS-1$

    public static final String WORKSPACE_PATH_VARIABLE_NAME_ARDUINO_LIB = "ArduinoLibPath";//$NON-NLS-1$
    public static final String WORKSPACE_PATH_VARIABLE_NAME_ARDUINO = "ArduinoPath";//$NON-NLS-1$
    public static final String WORKSPACE_PATH_VARIABLE_NAME_PRIVATE_LIB = "ArduinoPrivateLibPath";//$NON-NLS-1$
    public static final String WORKSPACE_PATH_VARIABLE_NAME_HARDWARE_LIB = "ArduinoHardwareLibPath";//$NON-NLS-1$
    public static final String PATH_VARIABLE_NAME_ARDUINO_PINS = "ArduinoPinPath";//$NON-NLS-1$
    public static final String PATH_VARIABLE_NAME_ARDUINO_PLATFORM = "ArduinoPlatformPath";//$NON-NLS-1$

    // tags to interpret the arduino input files
    public static final String BoardNameKeyTAG = "name";//$NON-NLS-1$
    public static final String UploadToolTeensy = "teensy_reboot";//$NON-NLS-1$
    public static final String Upload_ssh = "ssh upload";//$NON-NLS-1$

    public static final String KEY_BUILD_BEFORE_UPLOAD_OPTION = "Build before upload option";//$NON-NLS-1$

    public static final String ENV_KEY_ARDUINO_START = "A.";//$NON-NLS-1$
    public static final String ENV_KEY_ARDUINO_PATH = ENV_KEY_ARDUINO_START + "RUNTIME.IDE.PATH";//$NON-NLS-1$
    public static final String ENV_KEY_ARDUINO_UPLOAD_PROTOCOL = ENV_KEY_ARDUINO_START + "UPLOAD.PROTOCOL";//$NON-NLS-1$
    public static final String WORKSPACE_LIB_FOLDER = "Libraries/";//$NON-NLS-1$
    public static final String ARDUINO_IDE_VERSION = "ArduinoIDEVersion";//$NON-NLS-1$
    public static final String ENV_KEY_recipe_c_o_pattern = ENV_KEY_ARDUINO_START + "RECIPE.C.O.PATTERN";//$NON-NLS-1$
    public static final String ENV_KEY_recipe_cpp_o_pattern = ENV_KEY_ARDUINO_START + "RECIPE.CPP.O.PATTERN";//$NON-NLS-1$
    public static final String ENV_KEY_recipe_S_o_pattern = ENV_KEY_ARDUINO_START + "RECIPE.S.O.PATTERN";//$NON-NLS-1$
    public static final String ENV_KEY_recipe_objcopy_hex_pattern = ENV_KEY_ARDUINO_START + "RECIPE.OBJCOPY.HEX.PATTERN";//$NON-NLS-1$
    public static final String ENV_KEY_recipe_objcopy_eep_pattern = ENV_KEY_ARDUINO_START + "RECIPE.OBJCOPY.EEP.PATTERN";//$NON-NLS-1$
    public static final String ENV_KEY_recipe_size_pattern = ENV_KEY_ARDUINO_START + "RECIPE.SIZE.PATTERN";//$NON-NLS-1$
    public static final String ENV_KEY_recipe_AR_pattern = ENV_KEY_ARDUINO_START + "RECIPE.AR.PATTERN";//$NON-NLS-1$//$NON-NLS-1$
    public static final String ENV_KEY_recipe_c_combine_pattern = ENV_KEY_ARDUINO_START + "RECIPE.C.COMBINE.PATTERN";

    public static final String ENV_KEY_build_variant = ENV_KEY_ARDUINO_START + "BUILD.VARIANT";//$NON-NLS-1$
    public static final String ENV_KEY_compiler_path = ENV_KEY_ARDUINO_START + "COMPILER.PATH";//$NON-NLS-1$
    public static final String ENV_KEY_build_system_path = ENV_KEY_ARDUINO_START + "BUILD.SYSTEM.PATH";//$NON-NLS-1$
    public static final String ENV_KEY_build_generic_path = ENV_KEY_ARDUINO_START + "BUILD.GENERIC.PATH";//$NON-NLS-1$
    public static final String ENV_KEY_SOFTWARE = ENV_KEY_ARDUINO_START + "SOFTWARE";//$NON-NLS-1$
    public static final String ENV_KEY_ARCHITECTURE = ENV_KEY_ARDUINO_START + "ARCHITECTURE";//$NON-NLS-1$
    public static final String ENV_KEY_BUILD_ARCH = ENV_KEY_ARDUINO_START + "BUILD.ARCH";//$NON-NLS-1$
    public static final String ENV_KEY_HARDWARE_PATH = ENV_KEY_ARDUINO_START + "RUNTIME.HARDWARE.PATH";//$NON-NLS-1$
    public static final String ENV_KEY_PLATFORM_PATH = ENV_KEY_ARDUINO_START + "RUNTIME.PLATFORM.PATH";//$NON-NLS-1$

    public static final String ENV_KEY_runtime_ide_version = ENV_KEY_ARDUINO_START + "RUNTIME.IDE.VERSION";//$NON-NLS-1$
    public static final String ENV_KEY_build_path = ENV_KEY_ARDUINO_START + "BUILD.PATH";//$NON-NLS-1$
    public static final String ENV_KEY_build_project_name = ENV_KEY_ARDUINO_START + "BUILD.PROJECT_NAME";//$NON-NLS-1$
    public static final String ENV_KEY_build_variant_path = ENV_KEY_ARDUINO_START + "BUILD.VARIANT.PATH";//$NON-NLS-1$
    public static final String ENV_KEY_archive_file = ENV_KEY_ARDUINO_START + "ARCHIVE_FILE";//$NON-NLS-1$
    public static final String ENV_KEY_upload_use_1200bps_touch = ENV_KEY_ARDUINO_START + "UPLOAD.USE_1200BPS_TOUCH";//$NON-NLS-1$
    public static final String ENV_KEY_upload_disable_flushing = ENV_KEY_ARDUINO_START + "UPLOAD.DISABLE_FLUSHING";//$NON-NLS-1$
    public static final String ENV_KEY_wait_for_upload_port = ENV_KEY_ARDUINO_START + "UPLOAD.WAIT_FOR_UPLOAD_PORT";//$NON-NLS-1$
    public static final String ENV_KEY_upload_tool = ENV_KEY_ARDUINO_START + "UPLOAD.TOOL";//$NON-NLS-1$
    public static final String ENV_KEY_UPLOAD_PROTOCOL = ENV_KEY_ARDUINO_START + "UPLOAD.PROTOCOL";//$NON-NLS-1$
    public static final String ENV_KEY_build_core = ENV_KEY_ARDUINO_START + "BUILD.CORE";//$NON-NLS-1$
    public static final String ENV_KEY_build_core_path = ENV_KEY_ARDUINO_START + "BUILD.CORE.PATH";//$NON-NLS-1$
    public static final String ENV_KEY_use_archiver = ENV_KEY_ARDUINO_START + "BUILD.USE_ARCHIVER";//$NON-NLS-1$
    public static final String ENV_KEY_SERIAL_PORT = ENV_KEY_ARDUINO_START + "SERIAL.PORT";//$NON-NLS-1$
    public static final String ENV_KEY_SERIAL_PORT_FILE = ENV_KEY_ARDUINO_START + "SERIAL.PORT.FILE";//$NON-NLS-1$

    public static final String ArduinoIdeSuffix_WIN[] = { "" };//$NON-NLS-1$
    public static final String ArduinoIdeSuffix_LINUX[] = { "" };//$NON-NLS-1$
    public static final String ArduinoIdeSuffix_MAC[] = { "Contents/Resources/Java", "Contents/Java" };//$NON-NLS-1$

    public static final String ENV_KEY_JANTJE_START = "JANTJE.";//$NON-NLS-1$
    public static final String ENV_KEY_JANTJE_WARNING_LEVEL = ENV_KEY_JANTJE_START + "WARNING_LEVEL";//$NON-NLS-1$
    public static final String ENV_KEY_JANTJE_SIZE_COMMAND = ENV_KEY_JANTJE_START + "SIZE_COMMAND";//$NON-NLS-1$
    public static final String ENV_KEY_JANTJE_SIZE_SWITCH = ENV_KEY_JANTJE_START + "SIZE.SWITCH";//$NON-NLS-1$
    public static final String ENV_KEY_JANTJE_BOARDS_FILE = ENV_KEY_JANTJE_START + "BOARDS_FILE";//$NON-NLS-1$
    public static final String ENV_KEY_JANTJE_PLATFORM_FILE = ENV_KEY_JANTJE_START + "PLATFORM_FILE";//$NON-NLS-1$
    public static final String ENV_KEY_JANTJE_REFERENCED_PLATFORM_FILE = ENV_KEY_JANTJE_START + "REFERENCED_PLATFORM_FILE";//$NON-NLS-1$
    public static final String ENV_KEY_JANTJE_COM_PORT = ENV_KEY_JANTJE_START + "COM_PORT";//$NON-NLS-1$
    public static final String ENV_KEY_JANTJE_COM_PROG = ENV_KEY_JANTJE_START + "COM_PROGMR";//$NON-NLS-1$
    public static final String ENV_KEY_JANTJE_BOARD_NAME = ENV_KEY_JANTJE_START + "BOARD_NAME";//$NON-NLS-1$

    public static final String ENV_KEY_JANTJE_ADDITIONAL_COMPILE_OPTIONS = ENV_KEY_JANTJE_START + "EXTRA.COMPILE";//$NON-NLS-1$
    public static final String ENV_KEY_JANTJE_ADDITIONAL_C_COMPILE_OPTIONS = ENV_KEY_JANTJE_START + "EXTRA.C.COMPILE";//$NON-NLS-1$
    public static final String ENV_KEY_JANTJE_ADDITIONAL_CPP_COMPILE_OPTIONS = ENV_KEY_JANTJE_START + "EXTRA.CPP.COMPILE";//$NON-NLS-1$
    public static final String ENV_KEY_JANTJE_PACKAGE_ID = ENV_KEY_JANTJE_START + "PACKAGE_ID";//$NON-NLS-1$
    public static final String ENV_KEY_JANTJE_ARCITECTURE_ID = ENV_KEY_JANTJE_START + "ARCHITECTURE_ID";//$NON-NLS-1$
    public static final String ENV_KEY_JANTJE_BOARD_ID = ENV_KEY_JANTJE_START + "BOARD_ID";//$NON-NLS-1$
    public static final String ENV_KEY_JANTJE_BUILD_CORE = ENV_KEY_JANTJE_START + "BUILD_CORE";//$NON-NLS-1$
    public static final String ENV_KEY_JANTJE_BUILD_VARIANT = ENV_KEY_JANTJE_START + "BUILD_VARIANT";//$NON-NLS-1$

    //
    // template Sketch information

    public static final String ENV_KEY_JANTJE_SKETCH_TEMPLATE_FOLDER = ENV_KEY_JANTJE_START + "TEMPLATE_FOLDER";//$NON-NLS-1$
    public static final String ENV_KEY_JANTJE_SKETCH_TEMPLATE_USE_DEFAULT = ENV_KEY_JANTJE_START + "TEMPLATE_USE_DEFAULT";//$NON-NLS-1$

    public static final String JANTJE_SIZE_COMMAND = "\"${A.COMPILER.PATH}${A.COMPILER.SIZE.CMD}\" --format=avr --mcu=${A.BUILD.MCU} \"${A.BUILD.PATH}/${A.BUILD.PROJECT_NAME}.elf\"";//$NON-NLS-1$

    public static final String ENV_KEY_WARNING_LEVEL_OFF = " -w ";//$NON-NLS-1$
    public static final String ENV_KEY_WARNING_LEVEL_ON = " -Wall ";//$NON-NLS-1$

    public static final String ENV_KEY_GNU_SERIAL_PORTS = "gnu.io.rxtx.SerialPorts";//$NON-NLS-1$
    public static final String ENV_VALUE_GNU_SERIAL_PORTS_LINUX = "/dev/ttyACM0:/dev/ttyACM1:/dev/ttyACM2:/dev/ttyACM3:/dev/ttyUSB0::/dev/ttyUSB1::/dev/ttyUSB2::/dev/ttyUSB3::/dev/ttyUSB4";//$NON-NLS-1$
    // scope stuff
    public static final short SCOPE_START_DATA = (short) 0xCDAB;// This is the flag that indicates scope data is following
    // least significant first 0xCDAB;
    public static final String ARDUINO_EXAMPLE_FOLDER_NAME = "examples";//$NON-NLS-1$

    public static final String ARDUINO_IDE_DUMP__FILE_NAME_TRAILER = "ArduinoIDE.tmp";
    public static final String ARDUINO_IDE_DUMP__FILE_NAME_PREFIX = "DUMP_";
    public static final String PRE_PROCESSING_BOARDS_TXT = "pre_processing_boards.txt";
    public static final String POST_PROCESSING_BOARDS_TXT = "post_processing_boards.txt";
    public static final String PRE_PROCESSING_PLATFORM_TXT = "pre_processing_platform.txt";
    public static final String POST_PROCESSING_PLATFORM_TXT = "post_processing_platform.txt";

}
