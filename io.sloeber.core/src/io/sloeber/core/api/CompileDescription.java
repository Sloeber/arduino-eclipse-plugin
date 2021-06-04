package io.sloeber.core.api;

import static io.sloeber.core.common.Common.*;
import static io.sloeber.core.common.Const.*;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;

import io.sloeber.core.txt.KeyValueTree;
import io.sloeber.core.txt.TxtFile;

public class CompileDescription {
    public enum WarningLevels {

        NONE, MORE, ALL, CUSTOM;

        private String myCustomWarningLevel = EMPTY;

        /**
         * Set the custom command but only if the warning level is CUSTOM
         * 
         * @param customCommand
         *            the command that needs to be used
         */
        public void setCustomWarningLevel(String customWarningLevel) {
            if (this == CUSTOM) {
                myCustomWarningLevel = customWarningLevel;
            }
        }

        public void setCustomWarningLevel(String customWarningLevel, boolean force) {
            if (force) {
                myCustomWarningLevel = customWarningLevel;
            } else {
                setCustomWarningLevel(customWarningLevel);
            }

        }

        public String getCustomWarningLevel() {
            return myCustomWarningLevel;
        }

        /**
         * Get the string that should be put in the environment variable that places the
         * warning part in the command string This is a non expanded string for Arduino
         * IDE supported options This is what the user typed in the GUI in the CUSTOM
         * case
         * 
         * @return
         */
        public String getEnvValue() {
            switch (this) {
            case MORE:
                return "${compiler.warning_flags.more}"; //$NON-NLS-1$
            case ALL:
                return "${compiler.warning_flags.all}"; //$NON-NLS-1$
            case CUSTOM:
                return myCustomWarningLevel;
            case NONE:
                // this is default
            }
            return "${compiler.warning_flags.none}"; //$NON-NLS-1$
        }


    }

    private WarningLevels myWarningLevel = WarningLevels.NONE;

    private boolean myAlternativeSizeCommand = false;
    private boolean myEnableParallelBuild = false;
    private String my_CPP_CompileOptions = new String();
    private String my_C_CompileOptions = new String();
    private String my_C_andCPP_CompileOptions = new String();
    private String my_Assembly_CompileOptions = new String();
    private String my_Archive_CompileOptions = new String();
    private String my_Link_CompileOptions = new String();
    private String my_All_CompileOptions = new String();

    private static final String ENV_KEY_WARNING_LEVEL = "compiler.warning_flags"; //$NON-NLS-1$

    private static final String SLOEBER_ADDITIONAL_COMPILE_OPTIONS = ENV_KEY_SLOEBER_START
            + "extra.compile"; //$NON-NLS-1$
    private static final String SLOEBER_ADDITIONAL_C_COMPILE_OPTIONS = ENV_KEY_SLOEBER_START
            + "extra.c.compile"; //$NON-NLS-1$
    private static final String SLOEBER_ADDITIONAL_CPP_COMPILE_OPTIONS = ENV_KEY_SLOEBER_START
            + "extra.cpp.compile"; //$NON-NLS-1$
    private static final String SLOEBER_WARNING_LEVEL = ENV_KEY_SLOEBER_START + "warning_level"; //$NON-NLS-1$
    private static final String SLOEBER_WARNING_LEVEL_CUSTOM = SLOEBER_WARNING_LEVEL + DOT + "custom"; //$NON-NLS-1$
    private static final String SLOEBER_SIZE_COMMAND = ENV_KEY_SLOEBER_START + "alt_size_command"; //$NON-NLS-1$
    private static final String SLOEBER_SIZE_SWITCH = ENV_KEY_SLOEBER_START + "size.switch"; //$NON-NLS-1$
    private static final String SLOEBER_ASSEMBLY_COMPILE_OPTIONS = ENV_KEY_SLOEBER_START + "extra.assembly"; //$NON-NLS-1$
    private static final String SLOEBER_ARCHIVE_COMPILE_OPTIONS = ENV_KEY_SLOEBER_START + "extra.archive"; //$NON-NLS-1$
    private static final String SLOEBER_LINK_COMPILE_OPTIONS = ENV_KEY_SLOEBER_START + "extra.link"; //$NON-NLS-1$
    private static final String SLOEBER_ALL_COMPILE_OPTIONS = ENV_KEY_SLOEBER_START + "extra.all"; //$NON-NLS-1$

    public WarningLevels getWarningLevel() {
        return this.myWarningLevel;
    }

    public void setWarningLevel(WarningLevels myWarningLevel) {
        this.myWarningLevel = myWarningLevel;
    }

    public boolean isParallelBuildEnabled() {
        return myEnableParallelBuild;
    }

    public void setEnableParallelBuild(boolean parrallelBuild) {
        this.myEnableParallelBuild = parrallelBuild;
    }

    public boolean isAlternativeSizeCommand() {
        return this.myAlternativeSizeCommand;
    }

    public void setAlternativeSizeCommand(boolean alternativeSizeCommand) {
        this.myAlternativeSizeCommand = alternativeSizeCommand;
    }

    public String get_CPP_CompileOptions() {
        return this.my_CPP_CompileOptions;
    }

    public void set_CPP_CompileOptions(String new_CPP_CompileOptions) {
        this.my_CPP_CompileOptions = new_CPP_CompileOptions;
    }

    public String get_C_CompileOptions() {
        return this.my_C_CompileOptions;
    }

    public void set_C_CompileOptions(String new_C_CompileOptions) {
        this.my_C_CompileOptions = new_C_CompileOptions;
    }

    public String get_C_andCPP_CompileOptions() {
        return this.my_C_andCPP_CompileOptions;
    }

    public void set_C_andCPP_CompileOptions(String new_C_andCPP_CompileOptions) {
        this.my_C_andCPP_CompileOptions = new_C_andCPP_CompileOptions;
    }

    public String get_Assembly_CompileOptions() {
        return this.my_Assembly_CompileOptions;
    }

    public void set_Assembly_CompileOptions(String my_Assembly_CompileOptions) {
        this.my_Assembly_CompileOptions = my_Assembly_CompileOptions;
    }

    public String get_Archive_CompileOptions() {
        return this.my_Archive_CompileOptions;
    }

    public void set_Archive_CompileOptions(String my_Archive_CompileOptions) {
        this.my_Archive_CompileOptions = my_Archive_CompileOptions;
    }

    public String get_Link_CompileOptions() {
        return this.my_Link_CompileOptions;
    }

    public void set_Link_CompileOptions(String my_Link_CompileOptions) {
        this.my_Link_CompileOptions = my_Link_CompileOptions;
    }

    public String get_All_CompileOptions() {
        return this.my_All_CompileOptions;
    }

    public void set_All_CompileOptions(String my_All_CompileOptions) {
        this.my_All_CompileOptions = my_All_CompileOptions;
    }

    /**
     * save the compilation options in this configuration description.
     *
     * @param configuration
     *            must be a valid configuration description
     */
    public Map<String, String> getEnvVars() {
        Map<String, String> ret = new HashMap<>();
        ret.put(SLOEBER_ADDITIONAL_COMPILE_OPTIONS, this.my_C_andCPP_CompileOptions);
        ret.put(SLOEBER_ADDITIONAL_CPP_COMPILE_OPTIONS, this.my_CPP_CompileOptions);
        ret.put(SLOEBER_ADDITIONAL_C_COMPILE_OPTIONS, this.my_C_CompileOptions);
        ret.put(SLOEBER_ASSEMBLY_COMPILE_OPTIONS, this.my_Assembly_CompileOptions);
        ret.put(SLOEBER_ARCHIVE_COMPILE_OPTIONS, this.my_Archive_CompileOptions);
        ret.put(SLOEBER_LINK_COMPILE_OPTIONS, this.my_Link_CompileOptions);
        ret.put(SLOEBER_ALL_COMPILE_OPTIONS, this.my_All_CompileOptions);
        ret.put(ENV_KEY_WARNING_LEVEL, myWarningLevel.getEnvValue());
        if (this.myAlternativeSizeCommand) {
            ret.put(SLOEBER_SIZE_SWITCH, makeEnvironmentVar(SLOEBER_SIZE_COMMAND));
        } else {
            ret.put(SLOEBER_SIZE_SWITCH, makeEnvironmentVar(RECIPE_SIZE));
        }

        return ret;
    }

    /**
     * Given the compile options you currently have and the ones provided Is a
     * rebuild needed if you switch from one to another
     * 
     * @param curOptions
     * @return true if a rebuild is needed otherwise false
     */

    public boolean needsRebuild(CompileDescription curOptions) {
        // ignore myWarningLevel
        // ignore myAlternativeSizeCommand
        if (curOptions == null) {
            return true;
        }
        return !equalCompileOptions(curOptions);
    }

    /**
     * get the environment variables that need to be stored in the configuration
     * files configuration files are files needed to setup the sloeber environment
     * for instance when opening a project or after import of a project in the
     * workspace
     * 
     * @return the minimum list of environment variables to recreate the project
     */
    public Map<String, String> getEnvVarsConfig(String prefix) {
        Map<String, String> ret = new HashMap<>();
        ret.put(prefix + SLOEBER_ADDITIONAL_COMPILE_OPTIONS, this.my_C_andCPP_CompileOptions);
        ret.put(prefix + SLOEBER_ADDITIONAL_CPP_COMPILE_OPTIONS, this.my_CPP_CompileOptions);
        ret.put(prefix + SLOEBER_ADDITIONAL_C_COMPILE_OPTIONS, this.my_C_CompileOptions);
        ret.put(prefix + SLOEBER_ASSEMBLY_COMPILE_OPTIONS, this.my_Assembly_CompileOptions);
        ret.put(prefix + SLOEBER_ARCHIVE_COMPILE_OPTIONS, this.my_Archive_CompileOptions);
        ret.put(prefix + SLOEBER_LINK_COMPILE_OPTIONS, this.my_Link_CompileOptions);
        ret.put(prefix + SLOEBER_ALL_COMPILE_OPTIONS, this.my_All_CompileOptions);
        ret.put(prefix + SLOEBER_WARNING_LEVEL, myWarningLevel.toString());
        ret.put(prefix + SLOEBER_WARNING_LEVEL_CUSTOM, myWarningLevel.myCustomWarningLevel);
        ret.put(prefix + SLOEBER_SIZE_SWITCH, Boolean.valueOf(myAlternativeSizeCommand).toString());

        return ret;
    }

    public Map<String, String> getEnvVarsVersion(String prefix) {
        return getEnvVarsConfig(prefix);
    }

    public CompileDescription(TxtFile configFile, String prefix) {

        KeyValueTree tree = configFile.getData();
        KeyValueTree section = tree.getChild(prefix);

        my_C_andCPP_CompileOptions = section.getValue(SLOEBER_ADDITIONAL_COMPILE_OPTIONS);
        my_CPP_CompileOptions = section.getValue(SLOEBER_ADDITIONAL_CPP_COMPILE_OPTIONS);
        my_C_CompileOptions = section.getValue(SLOEBER_ADDITIONAL_C_COMPILE_OPTIONS);
        my_Assembly_CompileOptions = section.getValue(SLOEBER_ASSEMBLY_COMPILE_OPTIONS);
        my_Archive_CompileOptions = section.getValue(SLOEBER_ARCHIVE_COMPILE_OPTIONS);
        my_Link_CompileOptions = section.getValue(SLOEBER_LINK_COMPILE_OPTIONS);
        my_All_CompileOptions = section.getValue(SLOEBER_ALL_COMPILE_OPTIONS);
        myAlternativeSizeCommand = TRUE.equalsIgnoreCase(section.getValue(SLOEBER_SIZE_SWITCH));
        try {
            myWarningLevel = WarningLevels.valueOf(section.getValue(SLOEBER_WARNING_LEVEL));
            myWarningLevel.setCustomWarningLevel(section.getValue(SLOEBER_WARNING_LEVEL_CUSTOM));
        } catch (@SuppressWarnings("unused") Exception e) {
            // ignore as this will be default
        }

    }

    public CompileDescription() {
        // no need to do anything
        // this will create default compile options
        // note that the Parallel build option is implemented at the ui level
        // therefore this is not set here but in the ui before project creation
    }

    public CompileDescription(CompileDescription compileDescription) {
        myWarningLevel = compileDescription.myWarningLevel;
        myAlternativeSizeCommand = compileDescription.myAlternativeSizeCommand;
        myEnableParallelBuild = compileDescription.myEnableParallelBuild;
        my_CPP_CompileOptions = compileDescription.my_CPP_CompileOptions;
        my_C_CompileOptions = compileDescription.my_C_CompileOptions;
        my_C_andCPP_CompileOptions = compileDescription.my_C_andCPP_CompileOptions;
        my_Assembly_CompileOptions = compileDescription.my_Assembly_CompileOptions;
        my_Archive_CompileOptions = compileDescription.my_Archive_CompileOptions;
        my_Link_CompileOptions = compileDescription.my_Link_CompileOptions;
        my_All_CompileOptions = compileDescription.my_All_CompileOptions;
    }

    /**
     * Compares 2 compile descriptors
     * 
     * @param other
     * @return true if the 2 are equal else false
     */
    public boolean equals(CompileDescription other) {
        return (myWarningLevel == other.myWarningLevel) && (myAlternativeSizeCommand == other.myAlternativeSizeCommand)
                && equalCompileOptions(other);
    }

    /**
     * Compares the compile options of 2 compile descriptors
     * 
     * @param other
     * @return true if the 2 have equal compile options else false
     */
    private boolean equalCompileOptions(CompileDescription other) {
        return (my_CPP_CompileOptions.equals(other.my_CPP_CompileOptions))
                && (my_C_CompileOptions.equals(other.my_C_CompileOptions))
                && (my_C_andCPP_CompileOptions.equals(other.my_C_andCPP_CompileOptions))
                && (my_Assembly_CompileOptions.equals(other.my_Assembly_CompileOptions))
                && (my_Archive_CompileOptions.equals(other.my_Archive_CompileOptions))
                && (my_Link_CompileOptions.equals(other.my_Link_CompileOptions))
                && (my_All_CompileOptions.equals(other.my_All_CompileOptions));
    }

    @SuppressWarnings("nls")
    public static CompileDescription getFromCDT(ICConfigurationDescription confDesc) {
        CompileDescription ret = new CompileDescription();
        ret.my_C_andCPP_CompileOptions = getOldWayEnvVar(confDesc, "JANTJE.extra.compile");
        ret.my_CPP_CompileOptions = getOldWayEnvVar(confDesc, "JANTJE.extra.cpp.compile");
        ret.my_C_CompileOptions = getOldWayEnvVar(confDesc, "JANTJE.extra.c.compile");
        ret.my_Assembly_CompileOptions = getOldWayEnvVar(confDesc, "JANTJE.extra.assembly");
        ret.my_Archive_CompileOptions = getOldWayEnvVar(confDesc, "JANTJE.extra.archive");
        ret.my_Link_CompileOptions = getOldWayEnvVar(confDesc, "JANTJE.extra.link");
        ret.my_All_CompileOptions = getOldWayEnvVar(confDesc, "JANTJE.extra.all");
        ret.myWarningLevel = WarningLevels.NONE;
        if (TRUE.equalsIgnoreCase(getOldWayEnvVar(confDesc, "JANTJE.warning_level"))) {
            ret.myWarningLevel = WarningLevels.ALL;
        }
        ret.myAlternativeSizeCommand = TRUE.equalsIgnoreCase(getOldWayEnvVar(confDesc, "JANTJE.size.switch"));
        return ret;
    }
}
