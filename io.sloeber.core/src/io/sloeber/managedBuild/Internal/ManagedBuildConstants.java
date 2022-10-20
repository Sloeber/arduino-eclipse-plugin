package io.sloeber.managedBuild.Internal;

import java.io.File;

import org.eclipse.cdt.managedbuilder.internal.core.ManagedMakeMessages;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

@SuppressWarnings("nls")
public class ManagedBuildConstants {
    public static final String AT = "@";
    public static final String COLON = ":";
    public static final int COLS_PER_LINE = 80;
    public static final String COMMENT_SYMBOL = "#";
    public static final String DOLLAR_SYMBOL = "$";
    public static final String DEP_EXT = "d";
    public static final String DEPFILE_NAME = "subdir.dep";
    public static final String DOT = ".";
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
    public static final String NEWLINE = System.getProperty("line.separator");
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
    public static final String COMMENT = "MakefileGenerator.comment";
    public static final String HEADER = COMMENT + ".header";
    public static final String MESSAGE_FINISH_BUILD = ManagedMakeMessages
            .getResourceString("MakefileGenerator.message.finish.build");
    public static final String MESSAGE_FINISH_FILE = ManagedMakeMessages
            .getResourceString("MakefileGenerator.message.finish.file");
    public static final String MESSAGE_START_BUILD = ManagedMakeMessages
            .getResourceString("MakefileGenerator.message.start.build");
    public static final String MESSAGE_START_FILE = ManagedMakeMessages
            .getResourceString("MakefileGenerator.message.start.file");
    public static final String MESSAGE_START_DEPENDENCY = ManagedMakeMessages
            .getResourceString("MakefileGenerator.message.start.dependency");
    public static final String MESSAGE_NO_TARGET_TOOL = ManagedMakeMessages
            .getResourceString("MakefileGenerator.message.no.target");

    public static final String MOD_LIST = COMMENT + ".module.list";
    public static final String MOD_LIST_MESSAGE = ManagedMakeMessages.getResourceString(MOD_LIST);
    public static final String MOD_VARS = COMMENT + ".module.variables";
    public static final String MOD_RULES = COMMENT + ".build.rule";
    public static final String BUILD_TOP = COMMENT + ".build.toprules";
    public static final String ALL_TARGET = COMMENT + ".build.alltarget";
    public static final String MAINBUILD_TARGET = COMMENT + ".build.mainbuildtarget";
    public static final String BUILD_TARGETS = COMMENT + ".build.toptargets";
    public static final String SRC_LISTS = COMMENT + ".source.list";
    public static final String EMPTY_STRING = "";
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

}
