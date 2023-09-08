package io.sloeber.autoBuild.integration;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;

import io.sloeber.schema.api.IToolChain;

/**
 * ArduinoConst only contains global strings used in this plugin.
 *
 * @author Jan Baeyens
 *
 */
@SuppressWarnings("nls")
public class AutoBuildConstants {

    public static final boolean isWindows = Platform.getOS().equals(Platform.OS_WIN32);
    public static final boolean isLinux = Platform.getOS().equals(Platform.OS_LINUX);
    public static final boolean isMac = Platform.getOS().equals(Platform.OS_MACOSX);

    public static final int PARRALLEL_BUILD_UNLIMITED_JOBS = -1;
    public static final int PARRALLEL_BUILD_OPTIMAL_JOBS = -2;

    public static final String PROJECT_NAME_VARIABLE = "${ProjName}";
    public static final String CONFIG_NAME_VARIABLE = "${ConfigName}";

    public static final String TARGET_OBJECTS = "objects";
    public static final String TARGET_ALL = "all";
    public static final String TARGET_CLEAN = "clean";
    public static final String DEFAULT_AUTO_MAKE_TARGET = TARGET_OBJECTS;
    public static final String DEFAULT_INCREMENTAL_MAKE_TARGET = TARGET_ALL;
    public static final String DEFAULT_CLEAN_MAKE_TARGET = TARGET_CLEAN;

    // java stuff so I do not have to add all the time $NON-NLS-1$
    public static final String DOT = ".";
    public static final String ASTERISK = "*";
    public static final String AT_SYMBOL = "@";
    public static final String PROCENT = "%";
    public static final String SLACH = "/";
    public static final String BACKSLACH = "\\";
    public static final String FALSE = "FALSE";
    public static final String TRUE = "TRUE";
    public static final String COLON = ":";
    public static final String SEMICOLON = ";";
    public static final String COMMA = ",";
    public static final String EMPTY_STRING = "";
    public static final String WINDOWS_NEWLINE = "\r\n";
    public static final String NEWLINE = "\n";
    public static final String EQUAL = "=";
    public static final String BLANK = " ";
    public static final String ALL = "all";

    public static final String DEFAULT_BUILDSTEP_ANNOUNCEMENT = "Building";

    public static final String COMPILER = "compiler";
    public static final String END_OF_CHILDREN = "end of children ";
    public static final String BEGIN_OF_CHILDREN = "Begin of children ";
    public static final String DUMPLEAD = " ";
    public static final String NULL = "null";

    public static final String STATIC_LIB_EXTENSION = "a";
    public static final String DYNAMIC_LIB_EXTENSION = isWindows ? "dll" : "so";
    public static final String EXE_NAME = isWindows ? PROJECT_NAME_VARIABLE + ".exe" : PROJECT_NAME_VARIABLE;

    public static final String LIBRARY_PATH_SUFFIX = "libraries";
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
    public static final String CLEAN = "clean";
    public static final String PHONY = ".PHONY";
    public static final String SECONDARY_OUTPUTS = "secondary-outputs";

    public static final IPath DOT_SLASH_PATH = new Path("./");
    public static final String FILE_SEPARATOR = File.separator;
    // Enumerations
    public static final int PROJECT_RELATIVE = 1, PROJECT_SUBDIR_RELATIVE = 2, ABSOLUTE = 3;

    public static final String DEFAULT_PATTERN = "${TOOL_PATH}${TOOL_PREFIX}${COMMAND}${TOOL_SUFFIX} ${FLAGS} ${OUTPUT_FLAG} ${OUTPUT_PREFIX}${OUTPUT} ${INPUTS}";
    public static final String DOUBLE_QUOTE = "\"";

    public static final String CMD_LINE_PRM_NAME = "COMMAND";
    public static final String CMD_LINE_INCLUDE_FILE = "-I";
    public static final String CMD_LINE_INCLUDE_FOLDER = "-I";
    public static final String CMD_LINE_DEFINE = "-D";
    public static final String FLAGS_PRM_NAME = "FLAGS";
    public static final String OUTPUT_FLAG_PRM_NAME = "OUTPUT_FLAG";
    public static final String OUTPUT_PREFIX_PRM_NAME = "OUTPUT_PREFIX";
    public static final String OUTPUT_PRM_NAME = "OUTPUT";
    public static final String INPUTS_PRM_NAME = "INPUTS";
    public static final String VARIABLE_PREFIX = "${";
    public static final String VARIABLE_SUFFIX = "}";
    public static final String DEPENDENCY_SUFFIX = "_DEPS";

    // Schema element names
    public static final String ID = "id";
    public static final String NAME = "name";
    public static final String SUPERCLASS = "superClass";
    public static final String IS_ABSTRACT = "isAbstract";
    public static final String IS_SYSTEM = "isSystem";
    public static final String ICON = "icon";
    public static final String INPUT_TYPE_ELEMENT_NAME = "inputType";
    public static final String SOURCE_CONTENT_TYPE = "sourceContentType";
    public static final String EXTENSIONS = "extensions";
    public static final String OUTPUT_TYPE_IDS = "outputTypeIDs";
    public static final String OPTION = "option";
    public static final String KEY_CUSTOM_TOOL_COMMAND = "custom.tool.command";
    public static final String KEY_CUSTOM_TOOL_PATTERN = "custom.tool.pattern";
    public static final String SCANNER_CONFIG_PROFILE_ID = "scannerConfigDiscoveryProfileId";
    public static final String LANGUAGE_ID = "languageId";
    public static final String LANGUAGE_INFO_CALCULATOR = "languageInfoCalculator";
    public static final String OUTPUT_TYPE_ELEMENT_NAME = "outputType";

    public static final String OUTPUT_CONTENT_TYPE = "outputContentType";
    public static final String OUTPUT_PREFIX = "outputPrefix";
    public static final String OUTPUT_EXTENSION = "outputExtension";
    public static final String OUTPUT_NAME = "outputName";
    public static final String NAME_PATTERN = "namePattern";
    public static final String NAME_PROVIDER = "nameProvider";
    public static final String BUILD_VARIABLE = "buildVariable";
    public static final String DYNAMIC_LIB_FILE = "dynamic";

    // Schema attribute names for option elements
    public static final String BROWSE_TYPE = "browseType";
    public static final String BROWSE_FILTER_PATH = "browseFilterPath";
    public static final String BROWSE_FILTER_EXTENSIONS = "browseFilterExtensions";
    public static final String CATEGORY = "category";
    public static final String ORDER = "order";
    public static final String COMMAND = "command";
    public static final String COMMAND_FALSE = "commandFalse";
    public static final String USE_BY_SCANNER_DISCOVERY = "useByScannerDiscovery";
    public static final String COMMAND_GENERATOR = "commandGenerator";
    public static final String TOOL_TIP = "tip";
    public static final String CONTEXT_ID = "contextId";
    public static final String DEFAULT_VALUE = "defaultValue";
    public static final String DEFAULTVALUE_GENERATOR = "defaultValueGenerator";
    public static final String ENUM_VALUE = "enumeratedOptionValue";
    public static final String TREE_ROOT = "treeOptionRoot";
    public static final String SELECT_LEAF_ONLY = "selectLeafOnly";
    public static final String TREE_VALUE = "treeOption";
    public static final String DESCRIPTION = "description";
    public static final String IS_DEFAULT = "isDefault";
    public static final String LIST_VALUE = "listOptionValue";
    public static final String RESOURCE_FILTER = "resourceFilter";
    public static final String APPLICABILITY_CALCULATOR = "applicabilityCalculator";
    public static final String TYPE_BOOL = "boolean";
    public static final String TYPE_ENUM = "enumerated";
    public static final String TYPE_INC_PATH = "includePath";
    public static final String TYPE_LIB = "libs";
    public static final String TYPE_STRING = "string";
    public static final String TYPE_STR_LIST = "stringList";
    public static final String TYPE_USER_OBJS = "userObjs";
    public static final String TYPE_DEFINED_SYMBOLS = "definedSymbols";
    public static final String TYPE_LIB_PATHS = "libPaths";
    public static final String TYPE_LIB_FILES = "libFiles";
    public static final String TYPE_INC_FILES = "includeFiles";
    public static final String TYPE_SYMBOL_FILES = "symbolFiles";
    public static final String TYPE_UNDEF_INC_PATH = "undefIncludePath";
    public static final String TYPE_UNDEF_DEFINED_SYMBOLS = "undefDefinedSymbols";
    public static final String TYPE_UNDEF_LIB_PATHS = "undefLibPaths";
    public static final String TYPE_UNDEF_LIB_FILES = "undefLibFiles";
    public static final String TYPE_UNDEF_INC_FILES = "undefIncludeFiles";
    public static final String TYPE_UNDEF_SYMBOL_FILES = "undefSymbolFiles";
    public static final String TYPE_TREE = "tree";
    public static final String VALUE_TYPE = "valueType";
    public static final String FIELD_EDITOR_ID = "fieldEditor";
    public static final String FIELD_EDITOR_EXTRA_ARGUMENT = "fieldEditorExtraArgument";
    public static final String LIST_ITEM_VALUE = "value";
    public static final String LIST_ITEM_BUILTIN = "builtIn";
    public static final String ASSIGN_TO_COMMAND_VARIABLE = "assignToCommandVarriable";
    public static final String OUTPUT_FLAG = "outputFlag";
    public static final String NATURE = "natureFilter";
    public static final String COMMAND_LINE_PATTERN = "commandLinePattern";
    public static final String COMMAND_LINE_GENERATOR = "commandLineGenerator";
    public static final String ERROR_PARSERS = IToolChain.ERROR_PARSERS;
    public static final String CUSTOM_BUILD_STEP = "customBuildStep";
    public static final String ANNOUNCEMENT = "announcement";
    public static final String IS_HIDDEN = "isHidden";
    public static final String DEPENDENCY_OUTPUT_PATTERN = "dependencyOutputPattern";
    public static final String DEPENDENCY_GENERATION_FLAG = "dependencyGenerationFlag";
}
