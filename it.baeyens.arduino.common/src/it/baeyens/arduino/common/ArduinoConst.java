package it.baeyens.arduino.common;

/**
 * ArduinoConst only contains global strings used in the eclipse plugin.
 * 
 * @author Jan Baeyens
 * 
 */
public class ArduinoConst {
    // General stuff
    public static final String PluginStart = "it.baeyens.";
    public static final String CORE_PLUGIN_ID = PluginStart + "core";

    // prefix to be added to the arduino environment
    protected static final String UploadPortPrefix_WIN = "-P\\\\.\\";
    protected static final String UploadPortPrefix_LINUX = "-P";
    protected static final String UploadPortPrefix_MAC = "-P";

    // natures
    public static final String Cnatureid = "org.eclipse.cdt.core.cnature";
    public static final String CCnatureid = "org.eclipse.cdt.core.ccnature";
    public static final String Buildnatureid = "org.eclipse.cdt.managedbuilder.core.managedBuildNature";
    public static final String Scannernatureid = "org.eclipse.cdt.managedbuilder.core.ScannerConfigNature";
    public static final String ArduinoNatureID = PluginStart + "arduinonature";

    // preference nodes
    public static final String NODE_ARDUINO = PluginStart + "arduino";

    // preference keys
    public static final String KEY_ARDUINO_IDE_VERSION = "Arduino IDE Version";
    public static final String KEY_ARDUINOPATH = "Arduino Path";
    public static final String KEY_PRIVATE_LIBRARY_PATH = "Private Library Path";
    public static final String KEY_PRIVATE_HARDWARE_PATH = "Private hardware Path";

    // properties keys
    public static final String KEY_LAST_USED_ARDUINOBOARD = "Arduino Board";
    public static final String KEY_LAST_USED_COM_PORT = "Arduino Port";
    public static final String KEY_LAST_USED_PROGRAMMER = "Arduino Programmer";
    public static final String KEY_LAST_USED_ARDUINO_BOARDS_FILE = "Arduino boards file";
    public static final String KEY_LAST_USED_ARDUINO_MENU_OPTIONS = "Arduino Custom Option Selections";
    public static final String KEY_LAST_USED_SCOPE_FILTER_MENU_OPTION = "Arduino scope filter on off";

    // Serial monitor keys
    public static final String KEY_SERIAlRATE = "Serial monitor last selected rate";
    public static final String KEY_SERIAlPORT = "Serial monitor last selected Port";
    public static final String KEY_RXTX_LAST_USED_LINE_INDES = "Serial Monitor Last Used Line Ending index";
    public static final String KEY_RXTX_LAST_USED_AUTOSCROLL = "Serial Monitor Last Used auto scroll setting";
    // Folder Information
    public static final String LIBRARY_PATH_SUFFIX = "libraries";
    public static final String ARDUINO_HARDWARE_FOLDER_NAME = "hardware";
    public static final String ARDUINO_CORE_FOLDER_NAME = "cores";
    public static final String DEFAULT = "Default";
    public static final String BOARDS_FILE_NAME = "boards.txt";
    public static final String PLATFORM_FILE_NAME = "platform.txt";
    public static final String LIB_VERSION_FILE = "lib/version.txt";
    public static final String VARIANTS_FOLDER = "variants";

    public static final String WORKSPACE_PATH_VARIABLE_NAME_ARDUINO_LIB = "ArduinoLibPath";
    public static final String WORKSPACE_PATH_VARIABLE_NAME_ARDUINO = "ArduinoPath";
    public static final String WORKSPACE_PATH_VARIABLE_NAME_PRIVATE_LIB = "ArduinoPivateLibPath";
    public static final String WORKSPACE_PATH_VARIABLE_NAME_HARDWARE_LIB = "ArduinoHardwareLibPath";
    public static final String PATH_VARIABLE_NAME_ARDUINO_PINS = "ArduinoPinPath";
    public static final String PATH_VARIABLE_NAME_ARDUINO_PLATFORM = "ArduinoPlatformPath";

    // tags to interpret the arduino input files
    public static final String BoardNameKeyTAG = "name";
    public static final String UploadToolTeensy = "teensy_reboot";
    public static final String Upload_ssh = "ssh upload";

    public static final String KEY_BUILD_BEFORE_UPLOAD_OPTION = "Build before upload option";

    public static final String ENV_KEY_ARDUINO_START = "A.";
    public static final String ENV_KEY_ARDUINO_PATH = ENV_KEY_ARDUINO_START + "RUNTIME.IDE.PATH";
    public static final String ENV_KEY_ARDUINO_UPLOAD_PROTOCOL = ENV_KEY_ARDUINO_START + "UPLOAD.PROTOCOL";
    public static final String WORKSPACE_LIB_FOLDER = "Libraries/";
    public static final String ARDUINO_IDE_VERSION = "ArduinoIDEVersion";
    public static final String ENV_KEY_recipe_c_o_pattern = ENV_KEY_ARDUINO_START + "RECIPE.C.O.PATTERN";
    public static final String ENV_KEY_recipe_cpp_o_pattern = ENV_KEY_ARDUINO_START + "RECIPE.CPP.O.PATTERN";
    public static final String ENV_KEY_recipe_S_o_pattern = ENV_KEY_ARDUINO_START + "RECIPE.S.O.PATTERN";
    public static final String ENV_KEY_recipe_objcopy_hex_pattern = ENV_KEY_ARDUINO_START + "RECIPE.OBJCOPY.HEX.PATTERN";
    public static final String ENV_KEY_recipe_objcopy_eep_pattern = ENV_KEY_ARDUINO_START + "RECIPE.OBJCOPY.EEP.PATTERN";
    public static final String ENV_KEY_recipe_size_pattern = ENV_KEY_ARDUINO_START + "RECIPE.SIZE.PATTERN";
    public static final String ENV_KEY_recipe_AR_pattern = ENV_KEY_ARDUINO_START + "RECIPE.AR.PATTERN";
    public static final String ENV_KEY_recipe_c_combine_pattern = ENV_KEY_ARDUINO_START + "RECIPE.C.COMBINE.PATTERN";

    public static final String ENV_KEY_build_variant = ENV_KEY_ARDUINO_START + "BUILD.VARIANT";
    public static final String ENV_KEY_compiler_path = ENV_KEY_ARDUINO_START + "COMPILER.PATH";
    public static final String ENV_KEY_build_system_path = ENV_KEY_ARDUINO_START + "BUILD.SYSTEM.PATH";
    public static final String ENV_KEY_build_generic_path = ENV_KEY_ARDUINO_START + "BUILD.GENERIC.PATH";
    public static final String ENV_KEY_SOFTWARE = ENV_KEY_ARDUINO_START + "SOFTWARE";
    public static final String ENV_KEY_ARCHITECTURE = ENV_KEY_ARDUINO_START + "ARCHITECTURE";
    public static final String ENV_KEY_BUILD_ARCH = ENV_KEY_ARDUINO_START + "BUILD.ARCH";
    public static final String ENV_KEY_HARDWARE_PATH = ENV_KEY_ARDUINO_START + "RUNTIME.HARDWARE.PATH";
    public static final String ENV_KEY_PLATFORM_PATH = ENV_KEY_ARDUINO_START + "RUNTIME.PLATFORM.PATH";

    public static final String ENV_KEY_runtime_ide_version = ENV_KEY_ARDUINO_START + "RUNTIME.IDE.VERSION";
    public static final String ENV_KEY_build_path = ENV_KEY_ARDUINO_START + "BUILD.PATH";
    public static final String ENV_KEY_build_project_name = ENV_KEY_ARDUINO_START + "BUILD.PROJECT_NAME";
    public static final String ENV_KEY_build_variant_path = ENV_KEY_ARDUINO_START + "BUILD.VARIANT.PATH";
    public static final String ENV_KEY_archive_file = ENV_KEY_ARDUINO_START + "ARCHIVE_FILE";
    public static final String ENV_KEY_upload_use_1200bps_touch = ENV_KEY_ARDUINO_START + "UPLOAD.USE_1200BPS_TOUCH";
    public static final String ENV_KEY_upload_disable_flushing = ENV_KEY_ARDUINO_START + "UPLOAD.DISABLE_FLUSHING";
    public static final String ENV_KEY_wait_for_upload_port = ENV_KEY_ARDUINO_START + "UPLOAD.WAIT_FOR_UPLOAD_PORT";
    public static final String ENV_KEY_upload_tool = ENV_KEY_ARDUINO_START + "UPLOAD.TOOL";
    public static final String ENV_KEY_UPLOAD_PROTOCOL = ENV_KEY_ARDUINO_START + "UPLOAD.PROTOCOL";
    public static final String ENV_KEY_build_core_folder = ENV_KEY_ARDUINO_START + "BUILD.CORE";
    public static final String ENV_KEY_build_core_path = ENV_KEY_ARDUINO_START + "BUILD.CORE.PATH";
    public static final String ENV_KEY_use_archiver = ENV_KEY_ARDUINO_START + "BUILD.USE_ARCHIVER";
    public static final String ENV_KEY_SERIAL_PORT = ENV_KEY_ARDUINO_START + "SERIAL.PORT";
    public static final String ENV_KEY_SERIAL_PORT_FILE = ENV_KEY_ARDUINO_START + "SERIAL.PORT.FILE";

    public static final String ArduinoIdeSuffix_WIN[] = { "" };
    public static final String ArduinoIdeSuffix_LINUX[] = { "" };
    public static final String ArduinoIdeSuffix_MAC[] = { "Contents/Resources/Java", "Contents/Java" };

    public static final String ENV_KEY_JANTJE_START = "JANTJE.";
    public static final String ENV_KEY_JANTJE_WARNING_LEVEL = ENV_KEY_JANTJE_START + "WARNING_LEVEL";
    public static final String ENV_KEY_JANTJE_SIZE_COMMAND = ENV_KEY_JANTJE_START + "SIZE_COMMAND";
    public static final String ENV_KEY_JANTJE_SIZE_SWITCH = ENV_KEY_JANTJE_START + "SIZE.SWITCH";
    public static final String ENV_KEY_JANTJE_BOARDS_FILE = ENV_KEY_JANTJE_START + "BOARDS_FILE";
    public static final String ENV_KEY_JANTJE_PLATFORM_FILE = ENV_KEY_JANTJE_START + "PLATFORM_FILE";
    public static final String ENV_KEY_JANTJE_COM_PORT = ENV_KEY_JANTJE_START + "COM_PORT";
    public static final String ENV_KEY_JANTJE_COM_PROG = ENV_KEY_JANTJE_START + "COM_PROGMR";
    public static final String ENV_KEY_JANTJE_BOARD_NAME = ENV_KEY_JANTJE_START + "BOARD_NAME";

    public static final String ENV_KEY_JANTJE_ADDITIONAL_COMPILE_OPTIONS = ENV_KEY_JANTJE_START + "EXTRA.COMPILE";
    public static final String ENV_KEY_JANTJE_ADDITIONAL_C_COMPILE_OPTIONS = ENV_KEY_JANTJE_START + "EXTRA.C.COMPILE";
    public static final String ENV_KEY_JANTJE_ADDITIONAL_CPP_COMPILE_OPTIONS = ENV_KEY_JANTJE_START + "EXTRA.CPP.COMPILE";
    //
    // template Sketch information

    public static final String ENV_KEY_JANTJE_SKETCH_TEMPLATE_FOLDER = ENV_KEY_JANTJE_START + "TEMPLATE_FOLDER";
    public static final String ENV_KEY_JANTJE_SKETCH_TEMPLATE_USE_DEFAULT = ENV_KEY_JANTJE_START + "TEMPLATE_USE_DEFAULT";

    public static final String JANTJE_SIZE_COMMAND = "\"${A.COMPILER.PATH}${A.COMPILER.SIZE.CMD}\" --format=avr --mcu=${A.BUILD.MCU} \"${A.BUILD.PATH}/${A.BUILD.PROJECT_NAME}.elf\"";

    public static final String ENV_KEY_WARNING_LEVEL_OFF = " -w ";
    public static final String ENV_KEY_WARNING_LEVEL_ON = " -Wall ";

    public static final String ENV_KEY_GNU_SERIAL_PORTS = "gnu.io.rxtx.SerialPorts";
    public static final String ENV_VALUE_GNU_SERIAL_PORTS_LINUX = "/dev/ttyACM0:/dev/ttyACM1:/dev/ttyACM2:/dev/ttyACM3:/dev/ttyUSB0::/dev/ttyUSB1::/dev/ttyUSB2::/dev/ttyUSB3::/dev/ttyUSB4";
    // scope stuff
    public static final short SCOPE_START_DATA = (short) 0xCDAB;// This is the flag that indicates scope data is following
    // least significant first 0xCDAB;
    public static final String ARDUINO_EXAMPLE_FOLDER_NAME = "examples";

}
