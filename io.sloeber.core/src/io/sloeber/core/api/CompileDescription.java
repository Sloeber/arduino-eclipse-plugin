package io.sloeber.core.api;

import static io.sloeber.core.api.Const.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.autoBuild.helpers.api.KeyValueTree;
import io.sloeber.core.Activator;
import io.sloeber.core.Messages;
import io.sloeber.core.txt.TxtFile;

public class CompileDescription {

	public enum DebugLevels {
		OPTIMIZED_FOR_DEBUG, OPTIMIZED_FOR_RELEASE, CUSTOM;

		@Override
		public String toString() {
			switch (this) {
			case OPTIMIZED_FOR_DEBUG:
				return Messages.CompileDescription_OptimizedForDebug;
			case OPTIMIZED_FOR_RELEASE:
				return Messages.CompileDescription_OptimizedForRelease;
			case CUSTOM:
				return Messages.CompileDescription_CustomDebugLevel;
			default:
				break;
			}
			return super.toString();
		}

		private String myCustomDebugLevel = EMPTY;

		/**
		 * Set the custom command but only if the warning level is CUSTOM
		 *
         * @param customCommand
         *            the command that needs to be used
		 */
		public void setCustomDebugLevel(String customDebugLevel) {
			if (this == CUSTOM) {
				myCustomDebugLevel = customDebugLevel;
			}
		}

		public void setCustomDebugLevel(String customDebugLevel, boolean force) {
			if (force) {
				myCustomDebugLevel = customDebugLevel;
			} else {
				setCustomDebugLevel(customDebugLevel);
			}

		}

		public String getCustomDebugLevel() {
			return myCustomDebugLevel;
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
			case OPTIMIZED_FOR_DEBUG:
				return "${compiler.optimization_flags.debug}"; //$NON-NLS-1$
			case OPTIMIZED_FOR_RELEASE:
				return "${compiler.optimization_flags.release}"; //$NON-NLS-1$
			case CUSTOM:
				return myCustomDebugLevel;
			default:
				return "${compiler.optimization_flags.release}"; //$NON-NLS-1$
			}

		}

	}

	public enum WarningLevels {
		@SuppressWarnings("hiding")
		ALL, MORE, DEFAULT, NONE, CUSTOM;

		@Override
		public String toString() {
			switch (this) {
			case MORE:
				return Messages.CompileDescription_WarningsMore;
			case ALL:
				return Messages.CompileDescription_WarningsAll;
			case CUSTOM:
				return Messages.CompileDescription_WarningsCustom;
			case DEFAULT:
				return Messages.CompileDescription_WarningsDefault;
			case NONE:
				return Messages.CompileDescription_WarningsNone;
			default:
				break;
			}
			return super.toString();
		}

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
			case DEFAULT:
				return "${compiler.warning_flags.default}";//$NON-NLS-1$
			case NONE:
				return "${compiler.warning_flags.none}"; //$NON-NLS-1$
			default:
				break;
			}
			return "${compiler.warning_flags.all}"; //$NON-NLS-1$
		}

	}

	public enum SizeCommands {

		ARDUINO_WAY, AVR_ALTERNATIVE, RAW_RESULT, CUSTOM;





		@Override
		public String toString() {
			return toString (this);
		}

		public static String toString(SizeCommands value) {

			switch (value) {
			case ARDUINO_WAY:
				return "Arduino Way"; //$NON-NLS-1$
			case AVR_ALTERNATIVE:
				return "AVR Alternative"; //$NON-NLS-1$
			case RAW_RESULT:
				return "Raw result"; //$NON-NLS-1$
			case CUSTOM:
				return "Custom"; //$NON-NLS-1$
			default:
				break;
			}
			try {
				return value.toString();
			} catch (@SuppressWarnings("unused") Exception e) {
				// ignore exception
			}
			return "Arduino Way"; //$NON-NLS-1$
		}

		public static SizeCommands valueOf(String name, SizeCommands defaultValue) {
			if (name.equals(toString(ARDUINO_WAY))) {
				return ARDUINO_WAY;
			}
			if (name.equals(toString(AVR_ALTERNATIVE))) {
				return AVR_ALTERNATIVE;
			}
			if (name.equals(toString(RAW_RESULT))) {
				return RAW_RESULT;
			}
			if (name.equals(toString(CUSTOM))) {
				return CUSTOM;
			}
			return defaultValue;
		}



		private String myCustomSizeCommand = EMPTY;

		/**
		 * Set the custom command but only if the warning level is CUSTOM
		 *
         * @param customCommand
         *            the command that needs to be used
		 */
		public void setCustomSizeCommand(String customWarningLevel) {
			if (this == CUSTOM) {
				myCustomSizeCommand = customWarningLevel;
			}
		}

		public void setCustomSizeCommand(String customWarningLevel, boolean force) {
			if (force) {
				myCustomSizeCommand = customWarningLevel;
			} else {
				setCustomSizeCommand(customWarningLevel);
			}

		}

		public String getCustomSizeCommand() {
			return myCustomSizeCommand;
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
			case ARDUINO_WAY:
				return Common.makeEnvironmentVar(ENV_KEY_BUILD_PATH) + SLACH + ARDUINO_SIZE;
			case AVR_ALTERNATIVE:
				return "${sloeber.size_command.avr}"; //$NON-NLS-1$
			case RAW_RESULT:
				return "${recipe.size.pattern}"; //$NON-NLS-1$
			case CUSTOM:
				return myCustomSizeCommand;
			default:
				break;
			}
			return "${recipe.size.pattern}"; //$NON-NLS-1$
		}
	}

	private WarningLevels myWarningLevel = WarningLevels.ALL;
	private SizeCommands mySizeCommand = SizeCommands.RAW_RESULT;
	private DebugLevels myDebugLevel = DebugLevels.OPTIMIZED_FOR_RELEASE;

	private boolean myEnableParallelBuild = false;
	private String my_CPP_CompileOptions = new String();
	private String my_C_CompileOptions = new String();
	private String my_C_andCPP_CompileOptions = new String();
	private String my_Assembly_CompileOptions = new String();
	private String my_Archive_CompileOptions = new String();
	private String my_Link_CompileOptions = new String();
	private String my_All_CompileOptions = new String();

	private static final String ENV_KEY_WARNING_LEVEL = "compiler.warning_flags"; //$NON-NLS-1$
	private static final String ENV_KEY_DEBUG_LEVEL = "compiler.optimization_flags"; //$NON-NLS-1$

	private static final String SLOEBER_ADDITIONAL_COMPILE_OPTIONS = ENV_KEY_SLOEBER_START + "extra.compile"; //$NON-NLS-1$
	private static final String SLOEBER_ADDITIONAL_C_COMPILE_OPTIONS = ENV_KEY_SLOEBER_START + "extra.c.compile"; //$NON-NLS-1$
	private static final String SLOEBER_ADDITIONAL_CPP_COMPILE_OPTIONS = ENV_KEY_SLOEBER_START + "extra.cpp.compile"; //$NON-NLS-1$
	private static final String SLOEBER_WARNING_LEVEL = ENV_KEY_SLOEBER_START + "warning_level"; //$NON-NLS-1$
	private static final String SLOEBER_SIZE_TYPE = ENV_KEY_SLOEBER_START + "size.type"; //$NON-NLS-1$
	private static final String SLOEBER_SIZE_CUSTOM = ENV_KEY_SLOEBER_START + "size.custom"; //$NON-NLS-1$

	private static final String SLOEBER_WARNING_LEVEL_CUSTOM = SLOEBER_WARNING_LEVEL + DOT + "custom"; //$NON-NLS-1$
	private static final String SLOEBER_DEBUG_LEVEL = ENV_KEY_SLOEBER_START + "debug_level"; //$NON-NLS-1$
	private static final String SLOEBER_DEBUG_LEVEL_CUSTOM = SLOEBER_DEBUG_LEVEL + DOT + "custom"; //$NON-NLS-1$

	// private static final String SLOEBER_SIZE_COMMAND = ENV_KEY_SLOEBER_START +
	// "alt_size_command"; //$NON-NLS-1$
	private static final String SLOEBER_SIZE_SWITCH = ENV_KEY_SLOEBER_START + "size.switch"; //$NON-NLS-1$
	private static final String SLOEBER_ASSEMBLY_COMPILE_OPTIONS = ENV_KEY_SLOEBER_START + "extra.assembly"; //$NON-NLS-1$
	private static final String SLOEBER_ARCHIVE_COMPILE_OPTIONS = ENV_KEY_SLOEBER_START + "extra.archive"; //$NON-NLS-1$
	private static final String SLOEBER_LINK_COMPILE_OPTIONS = ENV_KEY_SLOEBER_START + "extra.link"; //$NON-NLS-1$
	private static final String SLOEBER_ALL_COMPILE_OPTIONS = ENV_KEY_SLOEBER_START + "extra.all"; //$NON-NLS-1$

	public WarningLevels getWarningLevel() {
		return myWarningLevel;
	}

	public void setWarningLevel(WarningLevels warningLevel) {
		myWarningLevel = warningLevel;
	}

	public DebugLevels getDebugLevel() {
		return myDebugLevel;
	}

	public void setDebugLevel(DebugLevels debugLevel) {
		myDebugLevel = debugLevel;
	}

	public boolean isParallelBuildEnabled() {
		return myEnableParallelBuild;
	}

	public void setEnableParallelBuild(boolean parrallelBuild) {
		this.myEnableParallelBuild = parrallelBuild;
	}

	public void setSizeCommand(SizeCommands sizeCommand) {
		this.mySizeCommand = sizeCommand;
	}

	public SizeCommands getSizeCommand() {
		return mySizeCommand;
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
		ret.put(ENV_KEY_DEBUG_LEVEL, myDebugLevel.getEnvValue());
		ret.put(SLOEBER_SIZE_SWITCH, mySizeCommand.getEnvValue());

		return ret;
	}

	/**
	 * Given the compile options you currently have and the ones provided Is a
	 * rebuild needed if you switch from one to another
	 *
	 * @param otherOptions
	 * @return true if a rebuild is needed otherwise false
	 */

	public boolean needsRebuild(CompileDescription otherOptions) {
		// ignore myWarningLevel (as changing the warning level does not hurt
		// ignore myAlternativeSizeCommand (as this is run anyways)
		if (otherOptions == null) {
			return true;
		}
		// When the debuglevel is changed it is best to do a full rebuild
		if (!myDebugLevel.getEnvValue().equals(otherOptions.myDebugLevel.getEnvValue())) {
			return true;
		}
		return !equalCompileOptions(otherOptions);
	}

	/**
	 * get the environment variables that need to be stored in the configuration
	 * files configuration files are files needed to setup the sloeber environment
	 * for instance when opening a project or after import of a project in the
	 * workspace
	 *
	 * @return the minimum list of environment variables to recreate the project
	 */
	public void serialize(KeyValueTree keyValueTree) {
		Map<String, String> ret = getEnvVarsVersion();
		for (Entry<String, String> curvalue : ret.entrySet()) {
			keyValueTree.addChild(curvalue.getKey(), curvalue.getValue());
		}
	}

	/**
	 * Recreate the compile options based on the configuration environment variables
	 * given
	 *
	 * @param envVars
	 */
	public CompileDescription(KeyValueTree keyValues) {
		my_C_andCPP_CompileOptions = keyValues.getValue(SLOEBER_ADDITIONAL_COMPILE_OPTIONS);
		my_CPP_CompileOptions = keyValues.getValue(SLOEBER_ADDITIONAL_CPP_COMPILE_OPTIONS);
		my_C_CompileOptions = keyValues.getValue(SLOEBER_ADDITIONAL_C_COMPILE_OPTIONS);
		my_Assembly_CompileOptions = keyValues.getValue(SLOEBER_ASSEMBLY_COMPILE_OPTIONS);
		my_Archive_CompileOptions = keyValues.getValue(SLOEBER_ARCHIVE_COMPILE_OPTIONS);
		my_Link_CompileOptions = keyValues.getValue(SLOEBER_LINK_COMPILE_OPTIONS);
		my_All_CompileOptions = keyValues.getValue(SLOEBER_ALL_COMPILE_OPTIONS);
		String warningLevel = keyValues.getValue(SLOEBER_WARNING_LEVEL);
		String customWarningLevel = keyValues.getValue(SLOEBER_WARNING_LEVEL_CUSTOM);
		String debugLevel = keyValues.getValue(SLOEBER_DEBUG_LEVEL);
		String customDebugLevel = keyValues.getValue(SLOEBER_DEBUG_LEVEL_CUSTOM);
		String sizeCommand = keyValues.getValue(SLOEBER_SIZE_TYPE);
		String customSizeCommand = keyValues.getValue(SLOEBER_SIZE_CUSTOM);

		try {
			myWarningLevel = WarningLevels.valueOf(warningLevel);
			myWarningLevel.setCustomWarningLevel(customWarningLevel, true);

			myDebugLevel = DebugLevels.valueOf(debugLevel);
			myDebugLevel.setCustomDebugLevel(customDebugLevel, true);

			mySizeCommand = SizeCommands.valueOf(sizeCommand);
			mySizeCommand.setCustomSizeCommand(customSizeCommand, true);
		} catch (Exception e) {
			Activator.log(new Status(IStatus.WARNING, Activator.getId(), "Deserialisation error", e)); //$NON-NLS-1$
		}
	}

	public Map<String, String> getEnvVarsVersion() {
		Map<String, String> ret = new HashMap<>();
		ret.put(SLOEBER_ADDITIONAL_COMPILE_OPTIONS, this.my_C_andCPP_CompileOptions);
		ret.put(SLOEBER_ADDITIONAL_CPP_COMPILE_OPTIONS, this.my_CPP_CompileOptions);
		ret.put(SLOEBER_ADDITIONAL_C_COMPILE_OPTIONS, this.my_C_CompileOptions);
		ret.put(SLOEBER_ASSEMBLY_COMPILE_OPTIONS, this.my_Assembly_CompileOptions);
		ret.put(SLOEBER_ARCHIVE_COMPILE_OPTIONS, this.my_Archive_CompileOptions);
		ret.put(SLOEBER_LINK_COMPILE_OPTIONS, this.my_Link_CompileOptions);
		ret.put(SLOEBER_ALL_COMPILE_OPTIONS, this.my_All_CompileOptions);
		ret.put(SLOEBER_WARNING_LEVEL, myWarningLevel.name());
		ret.put(SLOEBER_WARNING_LEVEL_CUSTOM, myWarningLevel.myCustomWarningLevel);
		ret.put(SLOEBER_DEBUG_LEVEL, myDebugLevel.name());
		ret.put(SLOEBER_DEBUG_LEVEL_CUSTOM, myDebugLevel.myCustomDebugLevel);
		ret.put(SLOEBER_SIZE_TYPE, mySizeCommand.name());
		ret.put(SLOEBER_SIZE_CUSTOM, mySizeCommand.myCustomSizeCommand);

		return ret;
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

		try {
			myWarningLevel = WarningLevels.valueOf(section.getValue(SLOEBER_WARNING_LEVEL));
			myWarningLevel.setCustomWarningLevel(section.getValue(SLOEBER_WARNING_LEVEL_CUSTOM));
		} catch (@SuppressWarnings("unused") Exception e) {
			// ignore as this will be default
		}
		try {
			myDebugLevel = DebugLevels.valueOf(section.getValue(SLOEBER_DEBUG_LEVEL));
			myDebugLevel.setCustomDebugLevel(section.getValue(SLOEBER_DEBUG_LEVEL_CUSTOM));
		} catch (@SuppressWarnings("unused") Exception e) {
			// ignore as this will be default
		}
		try {
			mySizeCommand = SizeCommands.valueOf(section.getValue(SLOEBER_SIZE_TYPE));
			mySizeCommand.setCustomSizeCommand(section.getValue(SLOEBER_SIZE_CUSTOM));
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
		myDebugLevel = compileDescription.myDebugLevel;
		mySizeCommand = compileDescription.mySizeCommand;
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
		return myWarningLevel.getEnvValue().equals(other.myWarningLevel.getEnvValue())
				&& myDebugLevel.getEnvValue().equals(other.myDebugLevel.getEnvValue())
        		&& mySizeCommand.getEnvValue().equals(other.mySizeCommand.getEnvValue())
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

}
