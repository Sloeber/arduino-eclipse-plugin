package io.sloeber.core.toolchain;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsEditableProvider;
import org.eclipse.cdt.core.language.settings.providers.IWorkingDirectoryTracker;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
import org.eclipse.cdt.managedbuilder.language.settings.providers.ToolchainBuiltinSpecsDetector;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.common.Common;
import io.sloeber.common.Const;

@SuppressWarnings("nls")
public class ArduinoLanguageProvider extends ToolchainBuiltinSpecsDetector
		implements ILanguageSettingsEditableProvider {
	// ID must match the tool-chain definition in
	// org.eclipse.cdt.managedbuilder.core.buildDefinitions extension point
	private static final String GCC_TOOLCHAIN_ID = "cdt.managedbuild.toolchain.gnu.base"; //$NON-NLS-1$

	private enum State {
		NONE, EXPECTING_LOCAL_INCLUDE, EXPECTING_SYSTEM_INCLUDE, EXPECTING_FRAMEWORKS
	}

	private State state = State.NONE;

	private static final AbstractOptionParser[] optionParsers = {
			new IncludePathOptionParser("#include \"(\\S.*)\"", "$1", //$NON-NLS-1$ //$NON-NLS-2$
					ICSettingEntry.BUILTIN | ICSettingEntry.READONLY | ICSettingEntry.LOCAL),
			new IncludePathOptionParser("#include <(\\S.*)>", "$1", ICSettingEntry.BUILTIN | ICSettingEntry.READONLY), //$NON-NLS-1$ //$NON-NLS-2$
			new IncludePathOptionParser("#framework <(\\S.*)>", "$1", //$NON-NLS-1$ //$NON-NLS-2$
					ICSettingEntry.BUILTIN | ICSettingEntry.READONLY | ICSettingEntry.FRAMEWORKS_MAC),
			new MacroOptionParser("#define\\s+(\\S*\\(.*?\\))\\s*(.*)", "$1", "$2", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					ICSettingEntry.BUILTIN | ICSettingEntry.READONLY),
			new MacroOptionParser("#define\\s+(\\S*)\\s*(\\S*)", "$1", "$2", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					ICSettingEntry.BUILTIN | ICSettingEntry.READONLY), };

	@Override
	public String getToolchainId() {
		return GCC_TOOLCHAIN_ID;
	}

	@Override
	protected AbstractOptionParser[] getOptionParsers() {
		return optionParsers;
	}

	/**
	 * Create a list from one item.
	 */
	private static List<String> makeList(String line) {
		List<String> list = new ArrayList<>();
		list.add(line);
		return list;
	}

	@Override
	protected List<String> parseOptions(String lineIn) {
		String line = lineIn.trim();

		// contribution of -dD option
		if (line.startsWith("#define")) { //$NON-NLS-1$
			return makeList(line);
		}

		// contribution of includes
		if (line.equals("#include \"...\" search starts here:")) { //$NON-NLS-1$
			this.state = State.EXPECTING_LOCAL_INCLUDE;
		} else if (line.equals("#include <...> search starts here:")) { //$NON-NLS-1$
			this.state = State.EXPECTING_SYSTEM_INCLUDE;
		} else if (line.startsWith("End of search list.")) { //$NON-NLS-1$
			this.state = State.NONE;
		} else if (line.equals("Framework search starts here:")) { //$NON-NLS-1$
			this.state = State.EXPECTING_FRAMEWORKS;
		} else if (line.startsWith("End of framework search list.")) { //$NON-NLS-1$
			this.state = State.NONE;
		} else if (this.state == State.EXPECTING_LOCAL_INCLUDE) {
			// making that up for the parser to figure out
			line = "#include \"" + line + "\""; //$NON-NLS-1$ //$NON-NLS-2$
			return makeList(line);
		} else {
			String frameworkIndicator = "(framework directory)"; //$NON-NLS-1$
			if (this.state == State.EXPECTING_SYSTEM_INCLUDE) {
				// making that up for the parser to figure out
				if (line.contains(frameworkIndicator)) {
					line = "#framework <" + line.replace(frameworkIndicator, "").trim() + ">"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				} else {
					line = "#include <" + line + ">"; //$NON-NLS-1$ //$NON-NLS-2$
				}
				return makeList(line);
			} else if (this.state == State.EXPECTING_FRAMEWORKS) {
				// making that up for the parser to figure out
				line = "#framework <" + line.replace(frameworkIndicator, "").trim() + ">"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				return makeList(line);
			}
		}

		return null;
	}

	@Override
	public void startup(ICConfigurationDescription cfgDescription, IWorkingDirectoryTracker cwdTracker1)
			throws CoreException {
		super.startup(cfgDescription, cwdTracker1);

		this.state = State.NONE;
	}

	@Override
	public void shutdown() {
		this.state = State.NONE;

		super.shutdown();
	}

	@Override
	public ArduinoLanguageProvider cloneShallow() throws CloneNotSupportedException {
		return (ArduinoLanguageProvider) super.cloneShallow();
	}

	@Override
	public ArduinoLanguageProvider clone() throws CloneNotSupportedException {
		return (ArduinoLanguageProvider) super.clone();
	}

	@Override
	protected String getCompilerCommand(String languageId) {
		String compilerCommand = new String();

		ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(this.currentProject);
		if (prjDesc == null)
			return compilerCommand;

		IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		ICConfigurationDescription confDesc = prjDesc.getActiveConfiguration();

		// Bug fix for CDT 8.1 fixed in 8.2
		IFolder buildFolder = this.currentProject.getFolder(confDesc.getName());
		if (!buildFolder.exists()) {
			try {
				buildFolder.create(true, true, null);
			} catch (CoreException e) {
				Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
						"failed to create folder " + confDesc.getName(), e));
			}
		}
		// End of Bug fix for CDT 8.1 fixed in 8.2
		String recipeKey = new String();
		String OptionKey = new String();
		if (languageId.equals("org.eclipse.cdt.core.gcc")) {
			recipeKey = Common.get_ENV_KEY_RECIPE(Const.ACTION_C_to_O);
			OptionKey = Const.ENV_KEY_JANTJE_ADDITIONAL_C_COMPILE_OPTIONS;
		} else if (languageId.equals("org.eclipse.cdt.core.g++")) {
			recipeKey = Common.get_ENV_KEY_RECIPE(Const.ACTION_CPP_to_O);
			OptionKey = Const.ENV_KEY_JANTJE_ADDITIONAL_CPP_COMPILE_OPTIONS;
		} else {
			ManagedBuilderCorePlugin.error(
					"Unable to find compiler command for language " + languageId + " in toolchain=" + getToolchainId());
		}

		try {
			compilerCommand = envManager.getVariable(recipeKey + Const.DOT + "1", confDesc, true).getValue();
			compilerCommand = compilerCommand
					+ envManager.getVariable(recipeKey + Const.DOT + "2", confDesc, true).getValue();
			compilerCommand = compilerCommand
					+ envManager.getVariable(recipeKey + Const.DOT + "3", confDesc, true).getValue();
		} catch (Exception e) {
			compilerCommand = new String();
		}
		IEnvironmentVariable op1 = envManager.getVariable(Const.ENV_KEY_JANTJE_ADDITIONAL_COMPILE_OPTIONS, confDesc,
				true);
		IEnvironmentVariable op2 = envManager.getVariable(OptionKey, confDesc, true);
		if (op1 != null) {
			compilerCommand = compilerCommand + ' ' + op1.getValue();
		}
		if (op2 != null) {
			compilerCommand = compilerCommand + ' ' + op2.getValue();
		}

		return compilerCommand.replaceAll(" -o ", " ");
	}

}