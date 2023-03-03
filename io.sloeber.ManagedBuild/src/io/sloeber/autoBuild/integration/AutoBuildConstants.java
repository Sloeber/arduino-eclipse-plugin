package io.sloeber.autoBuild.integration;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

/**
 * ArduinoConst only contains global strings used in sloeber.
 *
 * @author Jan Baeyens
 *
 */
@SuppressWarnings("nls")
public class AutoBuildConstants {

    public static final boolean isWindows = Platform.getOS().equals(Platform.OS_WIN32);
    public static final boolean isLinux = Platform.getOS().equals(Platform.OS_LINUX);
    public static final boolean isMac = Platform.getOS().equals(Platform.OS_MACOSX);

    public static final String PROJECT_NAME_VARIABLE = "${ProjName}";

    // preference nodes
    public static final String NODE_ARDUINO = "io.sloeber.arduino";

    //for debug messages
    public static final int SLOEBER_STATUS_DEBUG = IStatus.CANCEL;

    // java stuff so I do not have to add all the time $NON-NLS-1$
    public static final String DOT = ".";
    public static final String AT_SYMBOL = "@";
    public static final String PROCENT = "%";
    public static final String SLACH = "/";
    public static final String FALSE = "FALSE";
    public static final String TRUE = "TRUE";
    public static final String COLON = ":";
    public static final String SEMICOLON = ";";
    public static final String COMMA = ",";
    public static final String EMPTY_STRING = "";
    public static final String NEWLINE = "\n";
    public static final String EQUAL = "=";
    public static final String BLANK = " ";
    public static final String ALL = "all";
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
    public static final String HARDWARE = "hardware";
    public static final String PLATFORM = "platform";
    public static final String TXT = "txt";
    public static final String SOURCE = "source";
    public static final String COMPILER = "compiler";
    public static final String END_OF_CHILDREN = "end of children ";
    public static final String BEGIN_OF_CHILDREN = "Begin of children ";
    public static final String DUMPLEAD = " ";

    public static final String STATIC_LIB_EXTENSION = "a";
    public static final String DYNAMIC_LIB_EXTENSION = isWindows ? "dll" : "so";
    public static final String EXE_NAME = isWindows ? "${ProjName}.exe" : "${ProjName}";
    public static final String NETWORK = "network";
    public static final String PORT = "port";
    public static final String AUTH = "auth";
    public static final String RECIPE = "recipe";
    public static final String BUILD = "build";
    public static final String SYSTEM = "system";
    public static final String COM_PORT = "com_port";
    public static final String ARDUINO = "arduino";
    public static final String PATH = "path";
    public static final String PROTOCOL = "protocol";
    public static final String VendorArduino = "arduino";

    // arduino txt pre and suffix
    public static final String NETWORK_PREFIX = "network_";
    public static final String REMOTE_SUFFIX = "_remote";

    // General stuff
    public static final String PLUGIN_ID = "io.sloeber.core";
    public static final String CORE_PLUGIN_ID = "io.sloeber.arduino.core";
    public static final String ARDUINO_NATURE_ID = "io.sloeber.arduinonature";
    public static final String KEY_LAST_USED_EXAMPLES = "Last used Examples";
    public static final String SLOEBER_HOME = "SLOEBER_HOME";
    public static final String LOCAL = "local";

    // Folder and file Information
    public static final String ARDUINO_HARDWARE_FOLDER_NAME = HARDWARE;
    public static final String ARDUINO_CODE_FOLDER_NAME = CORE;
    public static final String BOARDS_FILE_NAME = "boards" + DOT + TXT;
    public static final String PLATFORM_FILE_NAME = PLATFORM + DOT + TXT;
    public static final String VARIANTS_FOLDER_NAME = "variants";
    public static final String LIBRARY_PATH_SUFFIX = "libraries";
    public static final String ARDUINO_VARIANT_FOLDER_PATH = ARDUINO_CODE_FOLDER_NAME + SLACH + VARIANT;
    public static final String ARDUINO_CODE_FOLDER_PATH = ARDUINO_CODE_FOLDER_NAME + SLACH + CORE;
    public static final String SLOEBER_CFG = "sloeber.cfg";

    // Environment variable stuff
    public static final String ENV_KEY_SLOEBER_START = "sloeber" + DOT;

    public static final String ENV_KEY_UPLOAD_USE_1200BPS_TOUCH = UPLOAD + DOT + "use_1200bps_touch";
    public static final String ENV_KEY_WAIT_FOR_UPLOAD_PORT = UPLOAD + DOT + "wait_for_upload_port";
    public static final String ENV_KEY_NETWORK_PORT = NETWORK + DOT + PORT;
    public static final String ENV_KEY_NETWORK_AUTH = NETWORK + DOT + AUTH;
    public static final String ENV_KEY_NETWORK_PASSWORD = NETWORK + DOT + "password";
    public static final String ENV_KEY_UPLOAD_VERBOSE = UPLOAD + DOT + "verbose";

    public static final String ENV_KEY_USE_ARCHIVER = BUILD + DOT + "use_archiver";
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

    public static final String JSSC_SERIAL_FILTER_PATTERN_KEY = "jssc_serial_filter_pattern";
    public static final String JSSC_MAC_DEFAULT_FILTER_PATTERN = "^cu\\..*(UART|serial|usb).*";

    public static final String AT = "@";
    public static final int COLS_PER_LINE = 80;
    public static final String COMMENT_SYMBOL = "#";
    public static final String COMMENT_START = "# ";
    public static final String DOLLAR_SYMBOL = "$";
    public static final String DEP_EXT = "d";
    public static final String DEPFILE_NAME = "subdir.dep";
    public static final String DASH = "-";
    public static final String ECHO = "echo";
    public static final String IN_MACRO = "$<";
    public static final String LINEBREAK = "\\\n";
    public static final String LOGICAL_AND = "&&";
    public static final String MAKEFILE_DEFS = "makefile.defs";
    public static final String MAKEFILE_INIT = "makefile.init";
    public static final String MAKEFILE_NAME = "makefile";
    public static final String MAKEFILE_TARGETS = "makefile.targets";
    public static final String MAKE = "$(MAKE)";
    public static final String NO_PRINT_DIR = "--no-print-directory";

    public static final String MODFILE_NAME = "subdir.mk";
    public static final String OBJECTS_MAKFILE = "objects.mk";
    public static final String OUT_MACRO = "$@";
    public static final String ROOT = "..";
    public static final String SEPARATOR = "/";
    public static final String SINGLE_QUOTE = "'";
    public static final String SRCSFILE_NAME = "sources.mk";
    public static final String TAB = "\t";
    public static final String WHITESPACE = " ";
    public static final String WILDCARD = "%";

    // String constants for makefile contents and messages
    public static final String COMMENT = "MakefileGenerator_comment";
    public static final String HEADER = COMMENT + ".header";

    public static final String BUILD_TOP = COMMENT + ".build.toprules";
    public static final String BUILD_TARGETS = COMMENT + ".build.toptargets";
    public static final String[] EMPTY_STRING_ARRAY = new String[0];
    public static final String OBJS_MACRO = "OBJS";
    public static final String MACRO_ADDITION_ADDPREFIX_HEADER = "${addprefix ";
    public static final String MACRO_ADDITION_ADDPREFIX_SUFFIX = "," + WHITESPACE + LINEBREAK;
    public static final String MAKE_ADDITION = " +=";
    public static final String MAKE_EQUAL = " :=";
    public static final String MACRO_ADDITION_PREFIX_SUFFIX = MAKE_ADDITION + LINEBREAK;
    public static final String PREBUILD = "pre-build";
    public static final String MAINBUILD = "main-build";
    public static final String POSTBUILD = "post-build";
    public static final String SECONDARY_OUTPUTS = "secondary-outputs";

    public static final IPath DOT_SLASH_PATH = new Path("./");
    public static final String FILE_SEPARATOR = File.separator;
    // Enumerations
    public static final int PROJECT_RELATIVE = 1, PROJECT_SUBDIR_RELATIVE = 2, ABSOLUTE = 3;

    public static final String DEFAULT_PATTERN = "${COMMAND} ${FLAGS} ${OUTPUT_FLAG} ${OUTPUT_PREFIX}${OUTPUT} ${INPUTS}";
    public static final String DOUBLE_QUOTE = "\"";

    public static final String CMD_LINE_PRM_NAME = "COMMAND";
    public static final String FLAGS_PRM_NAME = "FLAGS";
    public static final String OUTPUT_FLAG_PRM_NAME = "OUTPUT_FLAG";
    public static final String OUTPUT_PREFIX_PRM_NAME = "OUTPUT_PREFIX";
    public static final String OUTPUT_PRM_NAME = "OUTPUT";
    public static final String INPUTS_PRM_NAME = "INPUTS";
    public static final String VARIABLE_PREFIX = "${";
    public static final String VARIABLE_SUFFIX = "}";
    public static final String DEPENDENCY_SUFFIX = "_DEPS";

    //    public static final String MESSAGE_FINISH_BUILD = ManagedMakeMessages
    //            .getResourceString("MakefileGenerator.message.finish.build");
    //    public static final String MESSAGE_FINISH_FILE = ManagedMakeMessages
    //            .getResourceString("MakefileGenerator.message.finish.file");
    //    public static final String MESSAGE_START_BUILD = ManagedMakeMessages
    //            .getResourceString("MakefileGenerator.message.start.build");
    //    public static final String MESSAGE_START_FILE = ManagedMakeMessages
    //            .getResourceString("MakefileGenerator.message.start.file");
    //    public static final String MESSAGE_START_DEPENDENCY = ManagedMakeMessages
    //            .getResourceString("MakefileGenerator.message.start.dependency");
    //    public static final String MESSAGE_NO_TARGET_TOOL = ManagedMakeMessages
    //            .getResourceString("MakefileGenerator.message.no.target");
    //    public static final String MESSAGE_MOD_VARS = MakefileGenerator_comment_module_variables;
    //    public static final String MESSAGE_MOD_RULES = MakefileGenerator_comment_build_rule;
    //    public static final String MOD_LIST_MESSAGE = MakefileGenerator_comment_module_list;
    //    public static final String MESSAGE_MAINBUILD_TARGET = ManagedMakeMessages
    //            .getResourceString(MakefileGenerator_comment_build_mainbuildtarget");
    //        public static final String MESSAGE_ALL_TARGET = ManagedMakeMessages.getResourceString(MakefileGenerator_comment_build_alltarget");
    //    public static final String MESSAGE_SRC_LISTS = ManagedMakeMessages.getResourceString(COMMENT + ".source.list");
    //    public static final String MESSAGE_HEADER = ManagedMakeMessages.getResourceString(HEADER);

    // Schema element names
    public static final String ID = "id"; //$NON-NLS-1$
    public static final String NAME = "name"; //$NON-NLS-1$
    public static final String SUPERCLASS = "superClass"; //$NON-NLS-1$
    public static final String IS_ABSTRACT = "isAbstract"; //$NON-NLS-1$
    public static final String IS_SYSTEM = "isSystem"; //$NON-NLS-1$
    public static final String ICON = "icon"; //$NON-NLS-1$
    public static final String INPUT_TYPE_ELEMENT_NAME = "inputType"; //$NON-NLS-1$
    public static final String SOURCE_CONTENT_TYPE = "sourceContentType"; //$NON-NLS-1$
    public static final String EXTENSIONS = "extensions"; //$NON-NLS-1$
    public static final String OUTPUT_TYPE_ID = "outputTypeID"; //$NON-NLS-1$
    public static final String OPTION = "option"; //$NON-NLS-1$
    public static final String ASSIGN_TO_OPTION = "assignToOption"; //$NON-NLS-1$	
    public static final String DEPENDENCY_CONTENT_TYPE = "dependencyContentType"; //$NON-NLS-1$
    public static final String DEPENDENCY_EXTENSIONS = "dependencyExtensions"; //$NON-NLS-1$
    public static final String SCANNER_CONFIG_PROFILE_ID = "scannerConfigDiscoveryProfileId"; //$NON-NLS-1$
    public static final String LANGUAGE_ID = "languageId"; //$NON-NLS-1$
    public static final String LANGUAGE_INFO_CALCULATOR = "languageInfoCalculator"; //$NON-NLS-1$
    public static final String OUTPUT_TYPE_ELEMENT_NAME = "outputType"; //$NON-NLS-1$

    public static final String OUTPUT_CONTENT_TYPE = "outputContentType"; //$NON-NLS-1$
    public static final String OUTPUT_PREFIX = "outputPrefix"; //$NON-NLS-1$
    public static final String OUTPUT_EXTENSION = "outputExtension"; //$NON-NLS-1$
    public static final String OUTPUT_NAME = "outputName"; //$NON-NLS-1$
    public static final String NAME_PATTERN = "namePattern"; //$NON-NLS-1$
    public static final String NAME_PROVIDER = "nameProvider"; //$NON-NLS-1$
    public static final String BUILD_VARIABLE = "buildVariable"; //$NON-NLS-1$

}
