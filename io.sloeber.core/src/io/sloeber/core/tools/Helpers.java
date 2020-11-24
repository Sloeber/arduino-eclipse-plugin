package io.sloeber.core.tools;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.parser.util.StringUtil;
import org.eclipse.cdt.core.settings.model.CIncludePathEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSetting;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;

import io.sloeber.core.InternalBoardDescriptor;
import io.sloeber.core.Messages;
import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.CompileOptions;
import io.sloeber.core.api.Defaults;
import io.sloeber.core.api.PackageManager;
import io.sloeber.core.api.Preferences;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.Const;
import io.sloeber.core.managers.ArduinoPlatform;
import io.sloeber.core.managers.InternalPackageManager;
import io.sloeber.core.managers.Library;
import io.sloeber.core.managers.Tool;
import io.sloeber.core.managers.ToolDependency;
import io.sloeber.core.txt.BoardTxtFile;
import io.sloeber.core.txt.PlatformTxtFile;
import io.sloeber.core.txt.Programmers;
import io.sloeber.core.txt.TxtFile;

/**
 * ArduinoHelpers is a static class containing general purpose functions
 *
 * @author Jan Baeyens
 *
 */
public class Helpers extends Common {

	private static final String ENV_KEY_BUILD_ARCH = ERASE_START + "build.arch"; //$NON-NLS-1$
    private static final String ENV_KEY_BUILD_SOURCE_PATH = ERASE_START + "build.source.path"; //$NON-NLS-1$
	private static final String ENV_KEY_BUILD_GENERIC_PATH = ERASE_START + "build.generic.path"; //$NON-NLS-1$
	private static final String ENV_KEY_HARDWARE_PATH = ERASE_START + "runtime.hardware.path"; //$NON-NLS-1$
	private static final String ENV_KEY_PLATFORM_PATH = ERASE_START + "runtime.platform.path"; //$NON-NLS-1$
	private static final String ENV_KEY_COMPILER_PATH = ERASE_START + "compiler.path"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_MAKE_LOCATION = ENV_KEY_JANTJE_START + "make_location"; //$NON-NLS-1$


    private static final String FILE = Messages.FILE;
	private static final String KEY = Messages.KEY;
	private static final String FOLDER = Messages.FOLDER;

	private static boolean myHasBeenLogged = false;


	/**
	 * This method is the internal working class that adds the provided include path
	 * to all configurations and languages.
	 *
	 * @param configurationDescription The configuration description of the project
	 *                                 to add it to
	 * @param IncludePath              The path to add to the include folders
	 * @see addLibraryDependency {@link #addLibraryDependency(IProject, IProject)}
	 */
    public static void addIncludeFolder(ICConfigurationDescription configurationDescription, IPath IncludePath,
            boolean isWorkspacePath) {
        ICLanguageSetting[] languageSettings = configurationDescription.getRootFolderDescription()
                .getLanguageSettings();
		int pathSetting = ICSettingEntry.VALUE_WORKSPACE_PATH;
		if (!isWorkspacePath) {
			pathSetting = 0;
		}

		// Add include path to all languages
		for (int idx = 0; idx < languageSettings.length; idx++) {
			ICLanguageSetting lang = languageSettings[idx];
			String LangID = lang.getLanguageId();
			if (LangID != null) {
				if (LangID.startsWith("org.eclipse.cdt.")) { //$NON-NLS-1$
					ICLanguageSettingEntry[] OrgIncludeEntries = lang.getSettingEntries(ICSettingEntry.INCLUDE_PATH);
					ICLanguageSettingEntry[] IncludeEntries = new ICLanguageSettingEntry[OrgIncludeEntries.length + 1];
					System.arraycopy(OrgIncludeEntries, 0, IncludeEntries, 0, OrgIncludeEntries.length);
					IncludeEntries[OrgIncludeEntries.length] = new CIncludePathEntry(IncludePath, pathSetting);
					lang.setSettingEntries(ICSettingEntry.INCLUDE_PATH, IncludeEntries);
				}
			}
		}
	}

	/**
	 * Removes include folders that are not valid. This method does not save the
	 * configurationDescription description
	 *
	 * @param configurationDescription the configuration that is checked
	 * @return true is a include path has been removed. False if the include path
	 *         remains unchanged.
	 */
	public static boolean removeInvalidIncludeFolders(ICConfigurationDescription configurationDescription) {
		// find all languages
		ICFolderDescription folderDescription = configurationDescription.getRootFolderDescription();
		ICLanguageSetting[] languageSettings = folderDescription.getLanguageSettings();
		boolean hasChange = false;
		// Add include path to all languages
		for (int idx = 0; idx < languageSettings.length; idx++) {
			ICLanguageSetting lang = languageSettings[idx];
			String LangID = lang.getLanguageId();
			if (LangID != null) {
				if (LangID.startsWith("org.eclipse.cdt.")) { //$NON-NLS-1$
					ICLanguageSettingEntry[] OrgIncludeEntries = lang.getSettingEntries(ICSettingEntry.INCLUDE_PATH);
					ICLanguageSettingEntry[] OrgIncludeEntriesFull = lang
							.getResolvedSettingEntries(ICSettingEntry.INCLUDE_PATH);
					int copiedEntry = 0;
					for (int curEntry = 0; curEntry < OrgIncludeEntries.length; curEntry++) {
						IPath cusPath = ((CIncludePathEntry) OrgIncludeEntriesFull[curEntry]).getFullPath();
						if ((ResourcesPlugin.getWorkspace().getRoot().exists(cusPath))
								|| (((CIncludePathEntry) OrgIncludeEntries[curEntry]).isBuiltIn())) {
							OrgIncludeEntries[copiedEntry++] = OrgIncludeEntries[curEntry];
						} else {
							Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
									"Removed invalid include path" + cusPath, null)); //$NON-NLS-1$
						}
					}
					if (copiedEntry != OrgIncludeEntries.length) // do not save
					// if nothing
					// has changed
					{
						ICLanguageSettingEntry[] IncludeEntries = new ICLanguageSettingEntry[copiedEntry];
						System.arraycopy(OrgIncludeEntries, 0, IncludeEntries, 0, copiedEntry);
						lang.setSettingEntries(ICSettingEntry.INCLUDE_PATH, IncludeEntries);
						hasChange = true;

					}
				}
			}
		}
		return hasChange;
	}

	/**
	 * Creates a folder and links the folder to an existing folder Parent folders of
	 * the target folder are created if needed. In case this method fails an error
	 * is logged.
	 *
	 * @param project the project the newly created folder will belong to
	 * @param target  the folder name relative to the project
	 * @param source  the fully qualified name of the folder to link to
	 */
	public static void LinkFolderToFolder(IProject project, IPath source, IPath target) {

		// create target parent folder and grandparents
		IPath ParentFolders = new Path(target.toString()).removeLastSegments(1);
		for (int curfolder = ParentFolders.segmentCount() - 1; curfolder >= 0; curfolder--) {
			try {
				createNewFolder(project, ParentFolders.removeLastSegments(curfolder).toString(), null);
			} catch (@SuppressWarnings("unused") CoreException e) {// ignore this error as the parent
				// folders may have been created yet
			}
		}

		// create the actual link
		try {
			createNewFolder(project, target.toString(), source);
		} catch (CoreException e) {
			Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
					Messages.Helpers_Create_folder_failed.replace(FOLDER, target.toString()), e));
		}
	}

	/**
	 * This method creates a link folder in the project and add the folder as a
	 * source path to the project it also adds the path to the include folder if the
	 * include path parameter points to a path that contains a subfolder named
	 * "utility" this subfolder will be added to the include path as well <br/>
	 * Forget about this. Arduino made this all so complicated I don't know anymore
	 * what needs to be added to what<br/>
	 * <br/>
	 *
	 * note Arduino has these subfolders in the libraries that need to be
	 * include.<br/>
	 * <br/>
	 *
	 * note that in the current eclipse version, there is no need to add the
	 * subfolder as a code folder. This may change in the future as it looks like a
	 * bug to me.<br/>
	 *
	 * @param project
	 * @param Path
	 * @throws CoreException
	 *
	 * @see addLibraryDependency {@link #addLibraryDependency(IProject, IProject)}
	 */
	public static void addCodeFolder(IProject project, IPath toLinkFolder, String LinkName,
			ICConfigurationDescription configurationDescription, boolean forceRoot) throws CoreException {
		IFolder link = project.getFolder(LinkName);		

		LinkFolderToFolder(project, toLinkFolder, new Path(LinkName));

		// Now the folder has been created we need to make sure the special
		// folders are added to the path

		String possibleIncludeFolder = "utility"; //$NON-NLS-1$
		File file = toLinkFolder.append(possibleIncludeFolder).toFile();
		if (file.exists()) {
            addIncludeFolder(configurationDescription, link.getFullPath().append(possibleIncludeFolder), true);
		}

		if (forceRoot) {
            addIncludeFolder(configurationDescription, link.getFullPath(), true);
		} else {
			// add src or root give priority to src
			possibleIncludeFolder = Library.LIBRARY_SOURCE_FODER;
			file = toLinkFolder.append(possibleIncludeFolder).toFile();
			if (file.exists()) {
                addIncludeFolder(configurationDescription, link.getFullPath().append(possibleIncludeFolder), true);
			} else {
                addIncludeFolder(configurationDescription, link.getFullPath(), true);
			}
		}
        // TOFIX removed this code as part of libraries not included in project after
        // creation,
        // Should run a lib test to see how this works without this code
        // if this is needed I should create a include with a environment var so I do
        // not need to get the boardDescriptor

        // possibleIncludeFolder = "arch"; //$NON-NLS-1$
        // file = toLinkFolder.append(possibleIncludeFolder).toFile();
        // if (file.exists()) {
        // InternalBoardDescriptor boardDescriptor = new
        // InternalBoardDescriptor(configurationDescription);
        // addIncludeFolder(rootFolderDescr,
        // link.getFullPath().append(possibleIncludeFolder).append(boardDescriptor.getArchitecture()));
        // }
	}

	public static void removeCodeFolder(IProject project, String LinkName) throws CoreException {
		IFolder link = project.getFolder(LinkName);
		if (link.exists()) {
			link.delete(true, null);
		}
	}

	/**
     * This method creates a link folder in the project descriptor and adds the
     * folder as a source path to the projectdescriptor it also adds the path to the
     * include folder if the include path parameter points to a path that contains a
     * subfolder named "utility" this subfolder will be added to the include path as
     * well <br/>
     * <br/>
     *
     * note Arduino has these subfolders in the libraries that need to be
     * include.<br/>
     * <br/>
     *
     * note that in the current eclipse version, there is no need to add the
     * subfolder as a code folder. This may change in the future as it looks like a
     * bug to me.<br/>
     *
     * @param project
     * @param Path
     * @throws CoreException
     *
     * @see addLibraryDependency {@link #addLibraryDependency(IProject, IProject)}
     */
	public static void addCodeFolder(IProject project, Path Path, ICConfigurationDescription configurationDescription,
			boolean forceRoot) throws CoreException {
            String NiceName = Path.lastSegment();
		addCodeFolder(project, Path, NiceName, configurationDescription, forceRoot);
	}



	/**
	 * This method adds the content of a content stream to a file If the file
	 * already exist the file remains untouched
	 *
	 * @param container     used as a reference to the file
	 * @param path          The path to the file relative from the container
	 * @param contentStream The stream to put in the file
	 * @param monitor       A monitor to show progress
	 * @throws CoreException
	 */
	public static IFile addFileToProject(IContainer container, Path path, InputStream contentStream,
			IProgressMonitor monitor, boolean overwrite) throws CoreException {
		IFile file = container.getFile(path);
		file.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		if (overwrite && file.exists()) {
			file.delete(true, null);
			file.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}

		if (!file.exists() && (contentStream != null)) {
			file.create(contentStream, true, monitor);
		}
		return file;
	}

	public static MessageConsole findConsole(String name) {
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		for (int i = 0; i < existing.length; i++)
			if (name.equals(existing[i].getName()))
				return (MessageConsole) existing[i];
		// no console found, so create a new one
		MessageConsole myConsole = new MessageConsole(name, null);
		conMan.addConsoles(new IConsole[] { myConsole });
		return myConsole;
	}

	/**
	 * This method adds the Arduino code in a subfolder named Arduino. 2 linked
	 * subfolders named core and variant link to the real Arduino code note
	 *
	 * @param project                  The project to add the arduino code to
	 * @param configurationDescription The configuration description that will
	 *                                 contain the change
	 * @throws CoreException
	 */
	public static void addArduinoCodeToProject(BoardDescriptor boardDescriptor, IProject project,
			ICConfigurationDescription configurationDescription) throws CoreException {

		IPath corePath = boardDescriptor.getActualCoreCodePath();
		if (corePath != null) {
			addCodeFolder(project, corePath, ARDUINO_CODE_FOLDER_PATH, configurationDescription, true);
			IPath variantPath = boardDescriptor.getActualVariantPath();
			if (variantPath == null) {
				// remove the existing link
				Helpers.removeCodeFolder(project, ARDUINO_VARIANT_FOLDER_PATH);
			} else {
				IPath redirectVariantPath = boardDescriptor.getActualVariantPath();
				Helpers.addCodeFolder(project, redirectVariantPath, ARDUINO_VARIANT_FOLDER_PATH,
						configurationDescription, false);
			}
		}

	}

	/**
	 * Creates a new folder resource as a link or local
	 *
	 * @param Project       the project the folder is added to
	 * @param newFolderName the new folder to create (can contain subfolders)
	 * @param linklocation  if null a local folder is created using newFolderName if
	 *                      not null a link folder is created with the name
	 *                      newFolderName and pointing to linklocation
	 *
	 * @return nothing
	 * @throws CoreException
	 */
	public static void createNewFolder(IProject Project, String newFolderName, IPath linklocation)
			throws CoreException {
		final IFolder newFolderHandle = Project.getFolder(newFolderName);
		if (linklocation != null) {
			URI relativeLinklocation = Project.getPathVariableManager().convertToRelative(URIUtil.toURI(linklocation),
					false, null);
			newFolderHandle.createLink(relativeLinklocation, IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, null);
		} else {
			newFolderHandle.create(0, true, null);
		}

	}

	/**
	 * Remove all the arduino environment variables.
	 *
	 * @param contribEnv
	 * @param confDesc
	 */
	private static void removeAllEraseEnvironmentVariables(IContributedEnvironment contribEnv,
			ICConfigurationDescription confDesc) {

		IEnvironmentVariable[] CurVariables = contribEnv.getVariables(confDesc);
		for (int i = (CurVariables.length - 1); i > 0; i--) {
			if (CurVariables[i].getName().startsWith(Const.ERASE_START)) {
				contribEnv.removeVariable(CurVariables[i].getName(), confDesc);
			}
		}
	}

	/**
	 * Sets the default values. Basically some settings are not set in the
	 * platform.txt file. Here I set these values. This method should be called as
	 * first. This way the values in platform.txt and boards.txt will take
	 * precedence of the default values declared here
	 *
	 * @param projectName
	 *
	 * @param contribEnv
	 * @param confDesc
	 * @param platformFile Used to define the hardware as different settings are
	 *                     needed for avr and sam
	 */

    private static Map<String,String> getEnvVarsDefault(IProject project, BoardDescriptor boardDescriptor) {
		// Set some default values because the platform.txt does not contain
		// them
		String architecture = boardDescriptor.getArchitecture();
		HashMap<String,String> vars=new HashMap<>();
		vars.put(ENV_KEY_BUILD_ARCH, architecture.toUpperCase());
        vars.put(ENV_KEY_HARDWARE_PATH,  boardDescriptor.getreferencedHardwarePath().toOSString());
        vars.put(ENV_KEY_PLATFORM_PATH,  boardDescriptor.getreferencingPlatformPath().toOSString());

        vars.put(ENV_KEY_BUILD_SOURCE_PATH, project.getLocation().toOSString());

        if (Common.isWindows) {
            vars.put(ENV_KEY_JANTJE_MAKE_LOCATION, ConfigurationPreferences.getMakePath().addTrailingSeparator().toOSString());
		}


        return vars;

	}




    public static Map<String, String> getEnvVarPlatformFileTools(ArduinoPlatform platform, boolean reportToolNotFound) {
        HashMap<String, String> emptyHashMap = new HashMap<>();
		if (platform == null) {
            return emptyHashMap;
		}
		if (platform.getToolsDependencies() == null) {
            return emptyHashMap;
		}
        return addDependentTools(platform.getToolsDependencies(), reportToolNotFound);

	}

    private static Map<String, String> addDependentTools(Iterable<ToolDependency> tools, boolean reportToolNotFound) {
        HashMap<String, String> vars = new HashMap<>();
        String RUNTIME_TOOLS = ERASE_START + RUNTIME + DOT + TOOLS + DOT;
        String DOT_PATH = DOT + PATH;
		for (ToolDependency tool : tools) {
            String keyString = RUNTIME_TOOLS + tool.getName() + DOT_PATH;
			Tool theTool = tool.getTool();
			if (theTool == null) {
				if (reportToolNotFound) {
					Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
							"Error adding platformFileTools while processing tool " + tool.getName() + " version " //$NON-NLS-1$ //$NON-NLS-2$
									+ tool.getVersion() + " Installpath is null")); //$NON-NLS-1$
				}
			} else {
                IPath installPath = theTool.getInstallPath();
                vars.put(keyString, installPath.toOSString());
                keyString = RUNTIME_TOOLS + tool.getName() + tool.getVersion() + DOT_PATH;
                vars.put(keyString, installPath.toOSString());
                keyString = RUNTIME_TOOLS + tool.getName() + '-' + tool.getVersion() + DOT_PATH;
                vars.put(keyString, installPath.toOSString());
			}
		}
        return vars;
	}

    private static Map<String, String> getEnvVarsAll(IProject project, InternalBoardDescriptor boardsDescriptor) {
        // first get all the data we need
        Programmers localProgrammers[] = Programmers.fromBoards(boardsDescriptor);
        String boardid = boardsDescriptor.getBoardID();

        InternalBoardDescriptor pluginPreProcessingBoardsTxt = new InternalBoardDescriptor(
                new BoardTxtFile(ConfigurationPreferences.getPreProcessingBoardsFile()), boardid);
        InternalBoardDescriptor pluginPostProcessingBoardsTxt = new InternalBoardDescriptor(
                new BoardTxtFile(ConfigurationPreferences.getPostProcessingBoardsFile()), boardid);
        TxtFile pluginPreProcessingPlatformTxt = new TxtFile(ConfigurationPreferences.getPreProcessingPlatformFile());
        TxtFile pluginPostProcessingPlatformTxt = new TxtFile(ConfigurationPreferences.getPostProcessingPlatformFile());

        //get all the environment variables
        Map<String, String> allVars= getEnvVarsDefault(project, boardsDescriptor);
        allVars.putAll( pluginPreProcessingPlatformTxt.getAllEnvironVars(EMPTY));
        allVars.putAll(pluginPreProcessingBoardsTxt.getEnvVarsTxt());
        PlatformTxtFile referencedPlatfromFile = boardsDescriptor.getreferencedPlatformFile();
        // process the platform file referenced by the boards.txt
        if (referencedPlatfromFile != null) {
            allVars.putAll( referencedPlatfromFile.getAllEnvironVars());
        }
        PlatformTxtFile referencingPlatfromFile = boardsDescriptor.getReferencingPlatformFile();
        // process the platform file next to the selected boards.txt
        if (referencingPlatfromFile != null) {
            allVars.putAll(referencingPlatfromFile.getAllEnvironVars());
        }
        allVars.putAll(getEnVarPlatformInfo(boardsDescriptor));

        // add the boards file
        allVars.putAll(boardsDescriptor.getEnvVarsAll());

        String programmer = boardsDescriptor.getProgrammer();
        for (Programmers curProgrammer : localProgrammers) {
            String programmerID = curProgrammer.getIDFromNiceName(programmer);
            if (programmerID != null) {
                allVars.putAll(curProgrammer.getAllEnvironVars( programmerID));
            }
        }

        // add the stuff that comes with the plugin that is marked as post
        allVars.putAll(pluginPostProcessingPlatformTxt.getAllEnvironVars(EMPTY));
        allVars.putAll(pluginPostProcessingBoardsTxt.getEnvVarsTxt());


        // Do some coded post processing
        allVars.putAll(getEnvVarsPostProcessing(project, allVars, boardsDescriptor));
        return allVars;

    }

    /**
     * This method creates environment variables based on the platform.txt and
     * boards.txt. platform.txt is processed first and then boards.txt. This way
     * boards.txt settings can overwrite common settings in platform.txt The
     * environment variables are only valid for the project given as parameter The
     * project properties are used to identify the boards.txt and platform.txt as
     * well as the board id to select the settings in the board.txt file At the end
     * also the path variable is set
     *
     *
     * To be able to quickly fix boards.txt and platform.txt problems I also added a
     * pre and post platform and boards files that are processed before and after
     * the arduino delivered boards.txt file.
     *
     * @param project
     *            the project for which the environment variables are set
     * @param arduinoProperties
     *            the info of the selected board to set the variables for
     */

    public static void setTheEnvironmentVariables(IProject project, ICConfigurationDescription confDesc,
            InternalBoardDescriptor boardsDescriptor) {
        IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
        IContributedEnvironment contribEnv = envManager.getContributedEnvironment();

        Map<String, String> allVars = getEnvVarsAll(project, boardsDescriptor);

		// remove all Arduino Variables so there is no memory effect
		removeAllEraseEnvironmentVariables(contribEnv, confDesc);
		
		//set the paths
        String pathDelimiter = makeEnvironmentVar("PathDelimiter"); //$NON-NLS-1$
        if (Common.isWindows) {
            String systemroot = makeEnvironmentVar("SystemRoot"); //$NON-NLS-1$
            allVars.put("PATH", //$NON-NLS-1$
                    makeEnvironmentVar(ENV_KEY_COMPILER_PATH) + pathDelimiter
                            + makeEnvironmentVar(ENV_KEY_BUILD_GENERIC_PATH) + pathDelimiter + systemroot + "\\system32" //$NON-NLS-1$
                            + pathDelimiter + systemroot + pathDelimiter + systemroot + "\\system32\\Wbem" //$NON-NLS-1$
                            + pathDelimiter + makeEnvironmentVar("sloeber_path_extension")); //$NON-NLS-1$
        } else {
            allVars.put("PATH", //$NON-NLS-1$
                    makeEnvironmentVar(ENV_KEY_COMPILER_PATH) + pathDelimiter
                            + makeEnvironmentVar(ENV_KEY_BUILD_GENERIC_PATH) + pathDelimiter
                            + makeEnvironmentVar("PATH")); //$NON-NLS-1$
        }

        // a save will overwrite the warning settings set by arduino
        // should find a better way though
        CompileOptions compileOptions = new CompileOptions(confDesc);
        compileOptions.save(confDesc);
        
        for (Entry<String, String> curVariable : allVars.entrySet()) {
            String name = curVariable.getKey();
            String value = curVariable.getValue();
            IEnvironmentVariable var = new EnvironmentVariable(name, makePathEnvironmentString(value));
            contribEnv.addVariable(var, confDesc);
        }

 
	}

	/**
	 * Following post processing is done
	 *
	 * CDT uses different keys to identify the input and output files then the
	 * arduino recipes. Therefore I split the arduino recipes into parts (based on
	 * the arduino keys) and connect them again in the plugin.xml using the CDT
	 * keys. This code assumes that the command is in following order ${first part}
	 * ${files} ${second part} [${ARCHIVE_FILE} ${third part}] with [optional]
	 *
	 * Secondly The handling of the upload variables is done differently in arduino
	 * than here. This is taken care of here. for example the output of this input
	 * tools.avrdude.upload.pattern="{cmd.path}" "-C{config.path}" {upload.verbose}
	 * is changed as if it were the output of this input
	 * tools.avrdude.upload.pattern="{tools.avrdude.cmd.path}"
	 * "-C{tools.avrdude.config.path}" {tools.avrdude.upload.verbose}
	 *
	 * thirdly if a programmer is selected different from default some extra actions
	 * are done here so no special code is needed to handle programmers
	 *
	 * Fourthly The build path for the core is {BUILD.PATH}/core/core in sloeber
	 * where it is {BUILD.PATH}/core/ in arduino world and used to be {BUILD.PATH}/
	 * This only gives problems in the link command as sometimes there are hardcoded
	 * links to some sys files so ${A.BUILD.PATH}/core/sys* ${A.BUILD.PATH}/sys* is
	 * replaced with ${A.BUILD.PATH}/core/core/sys*
	 *
	 * @param contribEnv
	 * @param confDesc
	 * @param boardsDescriptor
	 */
    private static Map<String, String> getEnvVarsPostProcessing(IProject project, Map<String, String> vars,
            InternalBoardDescriptor boardsDescriptor) {
        Map<String, String> extraVars=new HashMap<>();

		// split the recipes so we can add the input and output markers as cdt needs
		// them
		String recipeKeys[] = { RECIPE_C_to_O, RECIPE_CPP_to_O, RECIPE_S_to_O, RECIPE_SIZE, RECIPE_AR,
				RECIPE_C_COMBINE };
		for (String recipeKey : recipeKeys) {
            String recipe = vars.get(recipeKey);
            if (null == recipe) {
                continue;
            }

			// Sloeber should split o, -o {output} but to be safe that needs a regex so I
			// simply delete the -o
			if (!RECIPE_C_COMBINE.equals(recipeKey)) {
				recipe = recipe.replace(" -o ", " "); //$NON-NLS-1$ //$NON-NLS-2$
			}
			String recipeParts[] = recipe.split(
					"(\"\\$\\{A.object_file}\")|(\\$\\{A.object_files})|(\"\\$\\{A.source_file}\")|(\"[^\"]*\\$\\{A.archive_file}\")|(\"[^\"]*\\$\\{A.archive_file_path}\")", //$NON-NLS-1$
					3);

			switch (recipeParts.length) {
			case 0:
			    extraVars.put(recipeKey + DOT + '1', "echo no command for \"{KEY}\".".replace(KEY, recipeKey)); //$NON-NLS-1$
				break;
			case 1:
			    extraVars.put(recipeKey + DOT + '1', recipeParts[0]);
				break;
			case 2:
			    extraVars.put(recipeKey + DOT + '1', recipeParts[0]);
			    extraVars.put(recipeKey + DOT + '2', recipeParts[1]);
				break;
			case 3:
			    extraVars.put(recipeKey + DOT + '1', recipeParts[0]);
			    extraVars.put(recipeKey + DOT + '2', recipeParts[1]);
			    extraVars.put(recipeKey + DOT + '3', recipeParts[2]);

				break;
			default:
				// this should never happen as the split is limited to 3
			}
		}

		// report that the upload comport is not set
		String programmer = boardsDescriptor.getProgrammer();
		if (programmer.equalsIgnoreCase(Defaults.getDefaultUploadProtocol())) {
			String MComPort = boardsDescriptor.getUploadPort();
			if (MComPort.isEmpty()) {
				Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
						"Upload will fail due to missing upload port for project: "+project.getName())); //$NON-NLS-1$
			}
		}

		ArrayList<String> objcopyCommand = new ArrayList<>();
			for (Entry<String, String> curVariable : vars.entrySet()) {
			    String name=curVariable.getKey();
			    String value=curVariable.getValue();
				if (name.startsWith(Const.RECIPE_OBJCOPY) && name.endsWith(".pattern") && !value.isEmpty()) { //$NON-NLS-1$
					objcopyCommand.add(makeEnvironmentVar(name));
				}
			}
		Collections.sort(objcopyCommand);
		extraVars.put(Const.JANTJE_OBJCOPY, StringUtil.join(objcopyCommand, "\n\t")); //$NON-NLS-1$

		// handle the hooks
        extraVars.putAll(getEnvVarsHookBuild(vars, "A.JANTJE.pre.link", //$NON-NLS-1$
				"A.recipe.hooks.linking.prelink.XX.pattern", false)); //$NON-NLS-1$
        extraVars.putAll(getEnvVarsHookBuild(vars, "A.JANTJE.post.link", //$NON-NLS-1$
				"A.recipe.hooks.linking.postlink.XX.pattern", true)); //$NON-NLS-1$
        extraVars
                .putAll(getEnvVarsHookBuild(vars, "A.JANTJE.prebuild", "A.recipe.hooks.prebuild.XX.pattern", //$NON-NLS-1$ //$NON-NLS-2$
                        false));
        extraVars.putAll(getEnvVarsHookBuild(vars, "A.JANTJE.sketch.prebuild", //$NON-NLS-1$
				"A.recipe.hooks.sketch.prebuild.XX.pattern", false)); //$NON-NLS-1$
        extraVars.putAll(getEnvVarsHookBuild(vars, "A.JANTJE.sketch.postbuild", //$NON-NLS-1$
				"A.recipe.hooks.sketch.postbuild.XX.pattern", false)); //$NON-NLS-1$

		// add -relax for mega boards; the arduino ide way
        String buildMCU = vars.get(Const.ENV_KEY_BUILD_MCU);
		if ("atmega2560".equalsIgnoreCase(buildMCU)) { //$NON-NLS-1$
            String c_elf_flags = vars.get(Const.ENV_KEY_BUILD_COMPILER_C_ELF_FLAGS);
            extraVars.put(Const.ENV_KEY_BUILD_COMPILER_C_ELF_FLAGS, c_elf_flags + ",--relax"); //$NON-NLS-1$
		}
        return extraVars;
	}

    private static Map<String, String> getEnvVarsHookBuild(Map<String, String> vars, String varName,
            String hookName, boolean post) {
        Map<String, String> extraVars = new HashMap<>();
		String envVarString = new String();
        String searchString = "XX"; //$NON-NLS-1$
		String postSeparator = "}\n\t"; //$NON-NLS-1$
		String preSeparator = "${"; //$NON-NLS-1$
		if (post) {
			postSeparator = "${"; //$NON-NLS-1$
			preSeparator = "}\n\t"; //$NON-NLS-1$
		}
		for (int numDigits = 1; numDigits <= 2; numDigits++) {
            String formatter = "%0" + Integer.toString(numDigits) + "d"; //$NON-NLS-1$ //$NON-NLS-2$
			int counter = 1;
            String hookVarName = hookName.replace(searchString, String.format(formatter, Integer.valueOf(counter)));
            while (null != vars.get(hookVarName)) { // $NON-NLS-1$
				envVarString = envVarString + preSeparator + hookVarName + postSeparator;
                hookVarName = hookName.replace(searchString, String.format(formatter, Integer.valueOf(++counter)));
			}
		}
		if (!envVarString.isEmpty()) {
            extraVars.put(varName, envVarString);
		}
        return extraVars;
	}


	/**
	 * Set the project to force a rebuild. This method is called after the arduino
	 * settings have been updated. Note the only way I found I could get this to
	 * work is by deleting the build folder Still then the "indexer needs to recheck
	 * his includes from the language provider which still is not working
	 *
	 * @param project
	 */
	public static void setDirtyFlag(IProject project, ICConfigurationDescription cfgDescription) {
		IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(project);
		if (buildInfo == null) {
			return; // Project is not a managed build project
		}

		IFolder buildFolder = project.getFolder(cfgDescription.getName());
		if (buildFolder.exists()) {
			try {
				buildFolder.delete(true, null);
			} catch (CoreException e) {
				Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
						Messages.Helpers_delete_folder_failed.replace(FOLDER, cfgDescription.getName()), e));
			}
		}
	}

	/**
	 * Given a source file calculates the base of the output file. this method may
	 * not be needed if I can used the eclipse default behavior. However the eclipse
	 * default behavior is different from the arduino default behavior. So I keep it
	 * for now and we'll see how it goes The eclipse default behavior is (starting
	 * from the project folder [configuration]/Source The Arduino default behavior
	 * is all in 1 location (so no subfolders)
	 *
	 * @param Source The source file to find the
	 * @return The base file name for the ouput if Source is "file.cpp" the output
	 *         is "file.cpp"
	 */
	public static IPath GetOutputName(IPath Source) {
		return Source;
	}

	/**
	 * creates links to the root files and folders of the source location
	 *
	 * @param source the location where the files are that need to be linked to
	 * @param target the location where the links are to be created
	 */
	public static void linkDirectory(IProject project, IPath source, IPath target) {

		File[] a = source.toFile().listFiles();
		if (a == null) {
			if (!myHasBeenLogged) {
				Common.log(new Status(IStatus.INFO, Const.CORE_PLUGIN_ID,
						Messages.Helpers_error_link_folder_is_empty.replace(FILE, source.toOSString()), null));
				myHasBeenLogged = true;
			}
			return;
		}
		for (File f : a) {
			if (f.isDirectory()) {
				LinkFolderToFolder(project, source.append(f.getName()), target.append(f.getName()));
			} else {
				final IFile newFileHandle = project.getFile(target.append(f.getName()));
				try {
					newFileHandle.createLink(source.append(f.getName()),
							IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, null);
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		}

	}

    private static Map<String, String> getEnVarPlatformInfo(BoardDescriptor boardDescriptor) {

        // update the gobal variables if needed
        PackageManager.updateGlobalEnvironmentVariables();
        File referencingPlatformFile = boardDescriptor.getReferencingPlatformFile().getTxtFile();
        File referencedPlatformFile = boardDescriptor.getreferencedPlatformFile().getTxtFile();
        ArduinoPlatform referencingPlatform = InternalPackageManager.getPlatform(referencingPlatformFile);
        ArduinoPlatform referencedPlatform = InternalPackageManager.getPlatform(referencedPlatformFile);

        boolean jsonBasedPlatformManagement = !Preferences.getUseArduinoToolSelection();
        if (jsonBasedPlatformManagement) {
            // overrule the Arduino IDE way of working and use the json refereced tools
            Map<String, String> ret = getEnvVarPlatformFileTools(referencedPlatform, true);
            ret.putAll(getEnvVarPlatformFileTools(referencingPlatform, false));
            return ret;
        }
            // standard arduino IDE way
        Map<String, String> ret = getEnvVarPlatformFileTools(referencingPlatform, false);
        ret.putAll(getEnvVarPlatformFileTools(referencedPlatform, true));
        return ret;

    }
}
