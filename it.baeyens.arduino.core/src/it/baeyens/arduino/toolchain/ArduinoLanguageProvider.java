package it.baeyens.arduino.toolchain;

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
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.common.Const;

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

    @SuppressWarnings("nls")
    @Override
    protected String getCompilerCommand(String languageId) {
	String compilerCommand = Const.EMPTY_STRING;

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
	if (languageId.equals("org.eclipse.cdt.core.gcc")) {
	    try {
		compilerCommand = envManager.getVariable(Const.get_ENV_KEY_RECIPE(Const.ACTION_C_to_O), confDesc, true)
			.getValue().replace(" -o ", " "); //$NON-NLS-2$
	    } catch (Exception e) {
		compilerCommand = Const.EMPTY_STRING;
	    }
	    IEnvironmentVariable op1 = envManager.getVariable(Const.ENV_KEY_JANTJE_ADDITIONAL_COMPILE_OPTIONS, confDesc,
		    true);
	    IEnvironmentVariable op2 = envManager.getVariable(Const.ENV_KEY_JANTJE_ADDITIONAL_C_COMPILE_OPTIONS,
		    confDesc, true);
	    if (op1 != null) {
		compilerCommand = compilerCommand + ' ' + op1.getValue();
	    }
	    if (op2 != null) {
		compilerCommand = compilerCommand + ' ' + op2.getValue();
	    }
	    compilerCommand = compilerCommand + " -D" + Const.DEFINE_IN_ECLIPSE + "=1";
	} else if (languageId.equals("org.eclipse.cdt.core.g++")) {
	    try {
		compilerCommand = envManager
			.getVariable(Const.get_ENV_KEY_RECIPE(Const.ACTION_CPP_to_O), confDesc, true).getValue()
			.replace(" -o ", " ");
	    } catch (Exception e) {
		compilerCommand = Const.EMPTY_STRING;
	    }
	    IEnvironmentVariable op1 = envManager.getVariable(Const.ENV_KEY_JANTJE_ADDITIONAL_COMPILE_OPTIONS, confDesc,
		    true);
	    IEnvironmentVariable op2 = envManager.getVariable(Const.ENV_KEY_JANTJE_ADDITIONAL_CPP_COMPILE_OPTIONS,
		    confDesc, true);
	    if (op1 != null) {
		compilerCommand = compilerCommand + ' ' + op1.getValue();
	    }
	    if (op2 != null) {
		compilerCommand = compilerCommand + ' ' + op2.getValue();
	    }
	    compilerCommand = compilerCommand + " -D" + Const.DEFINE_IN_ECLIPSE + "=1";
	} else {
	    ManagedBuilderCorePlugin.error(
		    "Unable to find compiler command for language " + languageId + " in toolchain=" + getToolchainId());
	}

	return adaptCompilerCommand(compilerCommand.replaceAll(" -MMD ", " "));
    }

    /*
     * due to the way arduino and cdt work some conversions are needed her.
     * replaceAll(" -MMD ", " ") CDT adds -MMD so we delete them
     * 
     * replaceAll("[^\\\\]\"\"", Const.EMPTY_STRING I can't recall what this one
     * is for but it removes "" except \""
     * 
     * For the os dependent stuff see
     * https://github.com/jantje/arduino-eclipse-plugin/issues/493
     * 
     * replaceAll("  ", " ") due to the above replacements there can be multiple
     * spaces. this cause(s/d) problems so I re^lace them with 1 space. note
     * that -with the current implementation- this means that is you define a
     * string to a define and the string has multiple spaces there will only be
     * one left. This one has to be the last replacement !!
     */
    @SuppressWarnings("nls")
    public static String adaptCompilerCommand(String environmentReceivedRecipe) {
	String ret = environmentReceivedRecipe.replaceAll("[^\\\\]\"\"", Const.EMPTY_STRING);

	String replaceString = " '-D$1=\"$2\"'"; // linux and mac
	if (Platform.getOS().equals(Platform.OS_WIN32)) {
	    replaceString = " \"-D$1=\\\\\"$2\\\\\"\""; // windows
	}

	ret = ret.replaceAll(" '?-D(\\S+)=\\\\?\"(.+?)\\\\?\"'?", replaceString);

	ret = ret.replaceAll("  ", " ");

	return ret;
    }

}