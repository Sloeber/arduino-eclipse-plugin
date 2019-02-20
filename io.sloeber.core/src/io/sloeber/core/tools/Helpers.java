package io.sloeber.core.tools;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
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
import io.sloeber.core.api.Preferences;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.Const;
import io.sloeber.core.managers.ArduinoPlatform;
import io.sloeber.core.managers.InternalPackageManager;
import io.sloeber.core.managers.Library;
import io.sloeber.core.managers.Tool;
import io.sloeber.core.managers.ToolDependency;

@SuppressWarnings({"nls","unused"})
/**
 * ArduinoHelpers is a static class containing general purpose functions
 *
 * @author Jan Baeyens
 *
 */
public class Helpers extends Common {
	

	private static final String ENV_KEY_BUILD_ARCH = ERASE_START + "BUILD.ARCH";
	private static final String ENV_KEY_BUILD_GENERIC_PATH = ERASE_START + "BUILD.GENERIC.PATH";
	private static final String ENV_KEY_HARDWARE_PATH = ERASE_START + "RUNTIME.HARDWARE.PATH";
	private static final String ENV_KEY_PLATFORM_PATH = ERASE_START + "RUNTIME.PLATFORM.PATH";
	private static final String ENV_KEY_COMPILER_PATH = ERASE_START + "COMPILER.PATH";
	private static final String ENV_KEY_JANTJE_MAKE_LOCATION = ENV_KEY_JANTJE_START + "MAKE_LOCATION";

	private static final String MENU_KEY = "menu.";

	private static final String PROJECT = Messages.PROJECT;
	private static final String CONFIG = Messages.CONFIG;
	private static final String FILE = Messages.FILE;
	private static final String BOARDID = Messages.BOARDID;
	private static final String KEY = Messages.KEY;
	private static final String FOLDER = Messages.FOLDER;

	private static boolean myHasBeenLogged=false;

	/**
	 * conveniance method because java does not know default values as parameters
	 * default is isWorkSpace=true
	 * @param configurationDescription
	 * @param IncludePath
	 */
	public static void addIncludeFolder( ICConfigurationDescription configurationDescription,  IPath IncludePath) 
	{
		   addIncludeFolder( configurationDescription,  IncludePath,true) ;
	}
	/**
	 * This method is the internal working class that adds the provided include path
	 * to all configurations and languages.
	 *
	 * @param configurationDescription
	 *            The configuration description of the project to add it to
	 * @param IncludePath
	 *            The path to add to the include folders
	 * @see addLibraryDependency {@link #addLibraryDependency(IProject, IProject)}
	 */
	public static void addIncludeFolder(ICConfigurationDescription configurationDescription, IPath IncludePath,boolean isWorkspacePath) {
		// find all languages
		ICFolderDescription folderDescription = configurationDescription.getRootFolderDescription();
		ICLanguageSetting[] languageSettings = folderDescription.getLanguageSettings();
		int pathSetting=ICSettingEntry.VALUE_WORKSPACE_PATH;
		if(!isWorkspacePath)
		{
			pathSetting=0;
		}

		// Add include path to all languages
		for (int idx = 0; idx < languageSettings.length; idx++) {
			ICLanguageSetting lang = languageSettings[idx];
			String LangID = lang.getLanguageId();
			if (LangID != null) {
				if (LangID.startsWith("org.eclipse.cdt.")) {
					ICLanguageSettingEntry[] OrgIncludeEntries = lang.getSettingEntries(ICSettingEntry.INCLUDE_PATH);
					ICLanguageSettingEntry[] IncludeEntries = new ICLanguageSettingEntry[OrgIncludeEntries.length + 1];
					System.arraycopy(OrgIncludeEntries, 0, IncludeEntries, 0, OrgIncludeEntries.length);
					IncludeEntries[OrgIncludeEntries.length] = new CIncludePathEntry(IncludePath,pathSetting);
					lang.setSettingEntries(ICSettingEntry.INCLUDE_PATH, IncludeEntries);
				}
			}
		}
	}

	/**
	 * Removes include folders that are not valid. This method does not save the
	 * configurationDescription description
	 *
	 * @param configurationDescription
	 *            the configuration that is checked
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
				if (LangID.startsWith("org.eclipse.cdt.")) {
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
									"Removed invalid include path" + cusPath, null));
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
	 * @param project
	 *            the project the newly created folder will belong to
	 * @param target
	 *            the folder name relative to the project
	 * @param source
	 *            the fully qualified name of the folder to link to
	 */
	public static void LinkFolderToFolder(IProject project, IPath source, IPath target) {

		// create target parent folder and grandparents
		IPath ParentFolders = new Path(target.toString()).removeLastSegments(1);
		for (int curfolder = ParentFolders.segmentCount() - 1; curfolder >= 0; curfolder--) {
			try {
				createNewFolder(project, ParentFolders.removeLastSegments(curfolder).toString(), null);
			} catch (CoreException e) {// ignore this error as the parent
				// folders may have been created yet
			}
		}

		// create the actual link
		try {
			createNewFolder(project, target.toString(), source);
		} catch (CoreException e) {
			Common.log(
					new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, Messages.Helpers_Create_folder_failed.replace(FOLDER, target.toString()), e));
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
			ICConfigurationDescription configurationDescription,boolean forceRoot) throws CoreException {
		IFolder link = project.getFolder(LinkName);

		LinkFolderToFolder(project, toLinkFolder, new Path(LinkName));

		// Now the folder has been created we need to make sure the special
		// folders are added to the path

		String possibleIncludeFolder = "utility";
		File file = toLinkFolder.append(possibleIncludeFolder).toFile();
		if (file.exists()) {
			addIncludeFolder(configurationDescription, link.getFullPath().append(possibleIncludeFolder));
		}

		if (forceRoot) {
			addIncludeFolder(configurationDescription, link.getFullPath());
		} else {
			// add src or root give priority to src
			possibleIncludeFolder = Library.LIBRARY_SOURCE_FODER;
			file = toLinkFolder.append(possibleIncludeFolder).toFile();
			if (file.exists()) {
				addIncludeFolder(configurationDescription, link.getFullPath().append(possibleIncludeFolder));
			} else {
				addIncludeFolder(configurationDescription, link.getFullPath());
			}
		}

		possibleIncludeFolder = "arch";
		file = toLinkFolder.append(possibleIncludeFolder).toFile();
		if (file.exists()) {
			InternalBoardDescriptor boardDescriptor = new InternalBoardDescriptor(configurationDescription);
			addIncludeFolder(configurationDescription,
					link.getFullPath().append(possibleIncludeFolder).append(boardDescriptor.getArchitecture()));
		}
	}

	public static void removeCodeFolder(IProject project, String LinkName) throws CoreException {
		IFolder link = project.getFolder(LinkName);
		if (link.exists()) {
			link.delete(true, null);
		}
	}

	/**
	 * This method creates a link folder in the project and adds the folder as a
	 * source path to the project it also adds the path to the include folder if the
	 * include path parameter points to a path that contains a subfolder named
	 * "utility" this subfolder will be added to the include path as well <br/>
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
	public static void addCodeFolder(IProject project, Path Path, ICConfigurationDescription configurationDescription,boolean forceRoot)
			throws CoreException {

		String NiceName = Path.lastSegment();
		addCodeFolder(project, Path, NiceName, configurationDescription,forceRoot);
	}

	/**
	 * addTheNatures replaces all existing natures by the natures needed for a
	 * arduino project
	 *
	 * @param project
	 *            The project where the natures need to be added to
	 * @throws CoreException
	 */
	public static void addTheNatures(IProjectDescription description) throws CoreException {

		String[] newnatures = new String[5];
		newnatures[0] = "org.eclipse.cdt.core.cnature";
		newnatures[1] = "org.eclipse.cdt.core.ccnature";
		newnatures[2] = "org.eclipse.cdt.managedbuilder.core.managedBuildNature";
		newnatures[3] = "org.eclipse.cdt.managedbuilder.core.ScannerConfigNature";
		newnatures[4] = Const.ARDUINO_NATURE_ID;
		description.setNatureIds(newnatures);

	}

	/**
	 * This method adds the content of a content stream to a file If the file
	 * already exist the file remains untouched
	 *
	 * @param container
	 *            used as a reference to the file
	 * @param path
	 *            The path to the file relative from the container
	 * @param contentStream
	 *            The stream to put in the file
	 * @param monitor
	 *            A monitor to show progress
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
	 * @param project
	 *            The project to add the arduino code to
	 * @param configurationDescription
	 *            The configuration description that will contain the change
	 * @throws CoreException
	 */
	public static void addArduinoCodeToProject(BoardDescriptor boardDescriptor, IProject project,
			ICConfigurationDescription configurationDescription) throws CoreException {

		IPath corePath = boardDescriptor.getActualCoreCodePath();
		if(corePath!=null) {
		addCodeFolder(project, corePath, ARDUINO_CODE_FOLDER_PATH,
				configurationDescription,true);
		IPath variantPath = boardDescriptor.getActualVariantPath();
		if (variantPath == null) {
			// remove the existing link
			Helpers.removeCodeFolder(project, ARDUINO_VARIANT_FOLDER_PATH);
		} else {
			IPath redirectVariantPath = boardDescriptor.getActualVariantPath();
			Helpers.addCodeFolder(project, redirectVariantPath, ARDUINO_VARIANT_FOLDER_PATH,
					configurationDescription,false);
		}
		}

	}

	/**
	 * Creates a new folder resource as a link or local
	 *
	 * @param Project
	 *            the project the folder is added to
	 * @param newFolderName
	 *            the new folder to create (can contain subfolders)
	 * @param linklocation
	 *            if null a local folder is created using newFolderName if not null
	 *            a link folder is created with the name newFolderName and pointing
	 *            to linklocation
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
	 * @param platformFile
	 *            Used to define the hardware as different settings are needed for
	 *            avr and sam
	 */

	private static void setTheEnvironmentVariablesSetTheDefaults(String projectName, IContributedEnvironment contribEnv,
			ICConfigurationDescription confDesc, BoardDescriptor boardDescriptor) {
		// Set some default values because the platform.txt does not contain
		// them
		IPath platformPath = boardDescriptor.getreferencingPlatformPath();
		IPath hardwarePath = boardDescriptor.getreferencedHardwarePath();
		String architecture = boardDescriptor.getArchitecture();

		boardDescriptor.saveConfiguration(confDesc, contribEnv);
		setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_BUILD_ARCH, architecture.toUpperCase());
		setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_HARDWARE_PATH, hardwarePath.toString());
		setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_PLATFORM_PATH, platformPath.toString());


		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_MAKE_LOCATION,
					ConfigurationPreferences.getMakePath().toOSString() + File.separator);
		}



		// some glue to make it work
		String pathDelimiter = makeEnvironmentVar("PathDelimiter");
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			String systemroot = makeEnvironmentVar("SystemRoot");
			setBuildEnvironmentVariable(contribEnv, confDesc, "PATH",
					makeEnvironmentVar(ENV_KEY_COMPILER_PATH) + pathDelimiter
							+ makeEnvironmentVar(ENV_KEY_BUILD_GENERIC_PATH) + pathDelimiter + systemroot + "\\system32"
							+ pathDelimiter + systemroot + pathDelimiter + systemroot + "\\system32\\Wbem"
							+ pathDelimiter + makeEnvironmentVar("sloeber_path_extension"));
		} else {
			setBuildEnvironmentVariable(contribEnv, confDesc, "PATH",
					makeEnvironmentVar(ENV_KEY_COMPILER_PATH) + pathDelimiter
							+ makeEnvironmentVar(ENV_KEY_BUILD_GENERIC_PATH) + pathDelimiter
							+ makeEnvironmentVar("PATH"));
		}

	}

	private static void setTheEnvironmentVariablesAddAFile(IContributedEnvironment contribEnv,
			ICConfigurationDescription confDesc, File envVarFile) {
		setTheEnvironmentVariablesAddAFile(ERASE_START, contribEnv, confDesc, envVarFile, true);
	}

	/**
	 * This method parses a file with environment variables like the platform.txt
	 * file for values to be added to the environment variables
	 *
	 * @param contribEnv
	 * @param confDesc
	 * @param envVarFile
	 *            The file to parse
	 */
	private static void setTheEnvironmentVariablesAddAFile(String prefix, IContributedEnvironment contribEnv,
			ICConfigurationDescription confDesc, File envVarFile, boolean touppercase) {
		try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(envVarFile));
				BufferedReader br = new BufferedReader(new InputStreamReader(dataInputStream));) {
			String strLine;

			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				// Ignore everything after first #
				String realData[] = strLine.split("#");
				if (realData.length > 0) {
					String var[] = realData[0].split("=", 2);
					if (var.length == 2) {
						String value = var[1].trim();
						setBuildEnvironmentVariable(contribEnv, confDesc, MakeKeyString(prefix, var[0]),
								MakeEnvironmentString(value, prefix, touppercase));
					}
				}
			}
		} catch (FileNotFoundException e) {
			Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
					Messages.Helpers_Error_parsing_IO_exception.replace(FILE, envVarFile.toString()), e));
		} catch (IOException e) {
			Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
					Messages.Helpers_Error_File_does_not_exists.replace(FILE, envVarFile.toString()) , e));
		}
	}

	/**
	 * This method parses the boards.txt file for values to be added to the
	 * environment variables First it adds all the variables based on the board name
	 * [boardID].[key]=[value] results in [key]=[value] (taking in account the
	 * modifiers) Then it parses for the menu variables
	 * menu.[menuID].[boardID].[selectionID].[key]=[value] results in [key]=[value]
	 * (taking in account the modifiers)
	 *
	 * @param contribEnv
	 * @param confDesc
	 * @param platformFilename
	 *            The file to parse
	 */
	private static void setTheEnvironmentVariablesAddtheBoardsTxt(IContributedEnvironment contribEnv,
			ICConfigurationDescription confDesc, InternalBoardDescriptor boardDescriptor, boolean warn) {
		TxtFile boardsFile = boardDescriptor.getTxtFile();
		String boardID = boardDescriptor.getBoardID();
		Map<String, String> options = boardDescriptor.getOptions();
		// Get the boards section and add all entries to the environment
		// variables
		Map<String, String> boardSectionMap = boardsFile.getSection(boardID);
		if (boardSectionMap == null) {
			if (warn) {
				String error=Messages.Helpers_error_boards_TXT.replace(PROJECT,  confDesc.getProjectDescription().getProject().getName()).replaceAll(CONFIG, confDesc.getName())
						.replaceAll(FILE,  boardsFile.getTxtFile().toString()).replaceAll(BOARDID, boardID);
				Common.log(new Status(IStatus.INFO, Const.CORE_PLUGIN_ID,error));
			}
			return;
		}
		List<EnvironmentVariable> localVariables = new ArrayList<>();
		for (Entry<String, String> currentPair : boardSectionMap.entrySet()) {
			// if it is not a menu item add it
			if (!currentPair.getKey().startsWith(MENU_KEY)) {
				String keyString = MakeKeyString(currentPair.getKey());
				String valueString = MakeEnvironmentString(currentPair.getValue(), Const.ERASE_START, true);
				if (isLocalKey(currentPair.getKey())) {
					localVariables.add(new EnvironmentVariable(keyString, valueString));
				} else {
					contribEnv.addVariable(new EnvironmentVariable(keyString, valueString), confDesc);
				}
			}
		}
		for (EnvironmentVariable environmentVariable : localVariables) {
			contribEnv.addVariable(environmentVariable, confDesc);
		}
		for (Entry<String, String> currentPair : boardSectionMap.entrySet()) {
			// if it is a menu item add it
			if (currentPair.getKey().startsWith(MENU_KEY)) {

				String[] keySplit = currentPair.getKey().split("\\.");
				String menuID = keySplit[1];
				String menuItemID = keySplit[2];

				if (menuItemID.equalsIgnoreCase(options.get(menuID.toUpperCase()))) {
					// we also need to skip the name
					String StartValue = MENU + DOT + menuID + DOT + menuItemID + DOT; // $NON-NLS-1$
					try {
						String keyString = MakeKeyString(currentPair.getKey().substring(StartValue.length()));
						String valueString = MakeEnvironmentString(currentPair.getValue(), Const.ERASE_START, true);
						contribEnv.addVariable(new EnvironmentVariable(keyString, valueString), confDesc);
					} catch (StringIndexOutOfBoundsException e) {
						// ignore as this is the case when the menu name is
						// processed
					}

				}
			}
		}

	}

	private static boolean isLocalKey(String key) {
		String osString = "";
		if (Platform.getOS().equals(Platform.OS_LINUX)) {
			osString = ".LINUX";
		} else if (Platform.getOS().equals(Platform.OS_WIN32)) {
			osString = ".WINDOWS";
		} else if (Platform.getOS().equals(Platform.OS_MACOSX)) {
			osString = ".MACOSX";
		}
		return key.toUpperCase().endsWith(osString);

	}

	private static void addPlatformFileTools(ArduinoPlatform platform, IContributedEnvironment contribEnv,
			ICConfigurationDescription confDesc, boolean reportToolNotFound) {
		if (platform.getToolsDependencies() != null) {
			for (ToolDependency tool : platform.getToolsDependencies()) {
				String keyString = MakeKeyString("runtime.tools." + tool.getName() + ".path");
				Tool theTool = tool.getTool();
				if (theTool == null) {
					if (reportToolNotFound) {
						Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
								"Error adding platformFileTools while processing tool " + tool.getName() + " version "
										+ tool.getVersion() + " Installpath is null"));
					}
				} else {
					String valueString = theTool.getInstallPath().toOSString();
					setBuildEnvironmentVariable(contribEnv, confDesc, keyString, valueString);
					keyString = MakeKeyString("runtime.tools." + tool.getName() + tool.getVersion() + ".path");
					setBuildEnvironmentVariable(contribEnv, confDesc, keyString, valueString);
					keyString = MakeKeyString("runtime.tools." + tool.getName() + '-' + tool.getVersion() + ".path");
					setBuildEnvironmentVariable(contribEnv, confDesc, keyString, valueString);
				}
			}
		}
	}

	private static void setTheEnvironmentVariablesAddThePlatformInfo(BoardDescriptor boardDescriptor,
			IContributedEnvironment contribEnv, ICConfigurationDescription confDesc) {
		File referencingPlatformFile = boardDescriptor.getReferencingPlatformFile();
		File referencedPlatformFile = boardDescriptor.getreferencedPlatformFile();
		String architecture = boardDescriptor.getArchitecture();
		for (ArduinoPlatform curPlatform : InternalPackageManager.getInstalledPlatforms()) {
			addPlatformFileTools(curPlatform, contribEnv, confDesc, false);
		}
		ArduinoPlatform LatestArduinoPlatform = null;
		for (ArduinoPlatform curPlatform : InternalPackageManager.getLatestInstalledPlatforms()) {
			if (architecture.equalsIgnoreCase(curPlatform.getArchitecture())) {
				addPlatformFileTools(curPlatform, contribEnv, confDesc, false);
				if ("arduino".equalsIgnoreCase(curPlatform.getPackage().getMaintainer())) {
					LatestArduinoPlatform = curPlatform;
				}
			}
		}
		// add the newest arduino avr platform again for the idiots wanting to
		// reference arduino without referencing it
		if (LatestArduinoPlatform != null) {
			addPlatformFileTools(LatestArduinoPlatform, contribEnv, confDesc, true);
		}
		// todo implement this jsonBasedPlatformManagement trigger
		boolean jsonBasedPlatformManagement = !Preferences.getUseArduinoToolSelection();
		if (jsonBasedPlatformManagement) {
			// add the referenced platform before the real platform
			ArduinoPlatform referencedPlatform = InternalPackageManager.getPlatform(referencedPlatformFile);
			if ((referencedPlatform != null) && (referencedPlatform != LatestArduinoPlatform)) {
				addPlatformFileTools(referencedPlatform, contribEnv, confDesc, true);
			}
			// and the real platform
			ArduinoPlatform referencingPlatform = InternalPackageManager.getPlatform(referencingPlatformFile);
			if ((referencingPlatform != null) && (referencingPlatform != LatestArduinoPlatform)) {

				addPlatformFileTools(referencingPlatform, contribEnv, confDesc, false);
			}
		}
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

		// first get all the data we need
		IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IContributedEnvironment contribEnv = envManager.getContributedEnvironment();

		Programmers localProgrammers[] = Programmers.fromBoards(boardsDescriptor);
		String boardid = boardsDescriptor.getBoardID();

		InternalBoardDescriptor pluginPreProcessingBoardsTxt = new InternalBoardDescriptor(
				new TxtFile(ConfigurationPreferences.getPreProcessingBoardsFile()), boardid);
		InternalBoardDescriptor pluginPostProcessingBoardsTxt = new InternalBoardDescriptor(
				new TxtFile(ConfigurationPreferences.getPostProcessingBoardsFile()), boardid);
		File pluginPreProcessingPlatformTxt = ConfigurationPreferences.getPreProcessingPlatformFile();
		File pluginPostProcessingPlatformTxt = ConfigurationPreferences.getPostProcessingPlatformFile();

		// Now we have all info we can start processing

		//set the output folder as derive

		// first remove all Arduino Variables so there is no memory effect
		removeAllEraseEnvironmentVariables(contribEnv, confDesc);

		setTheEnvironmentVariablesSetTheDefaults(project.getName(), contribEnv, confDesc, boardsDescriptor);

		// add the stuff that comes with the plugin that are marked as pre
		setTheEnvironmentVariablesAddAFile(new String(), contribEnv, confDesc, pluginPreProcessingPlatformTxt, false);
		setTheEnvironmentVariablesAddtheBoardsTxt(contribEnv, confDesc, pluginPreProcessingBoardsTxt, false);

		File referencedPlatfromFile = boardsDescriptor.getreferencedPlatformFile();
		// process the platform file referenced by the boards.txt
		if (referencedPlatfromFile != null && referencedPlatfromFile.exists()) {
			setTheEnvironmentVariablesAddAFile(contribEnv, confDesc, referencedPlatfromFile);
		}
		File referencingPlatfromFile = boardsDescriptor.getReferencingPlatformFile();
		// process the platform file next to the selected boards.txt
		if (referencingPlatfromFile != null && referencingPlatfromFile.exists()) {
			setTheEnvironmentVariablesAddAFile(contribEnv, confDesc, referencingPlatfromFile);
		}
		setTheEnvironmentVariablesAddThePlatformInfo(boardsDescriptor, contribEnv, confDesc);

		// add the boards file
		setTheEnvironmentVariablesAddtheBoardsTxt(contribEnv, confDesc, boardsDescriptor, true);

		String programmer = boardsDescriptor.getProgrammer();
		for (Programmers curProgrammer : localProgrammers) {
			String programmerID = curProgrammer.getBoardIDFromBoardName(programmer);
			if (programmerID != null) {
				InternalBoardDescriptor progBoard = new InternalBoardDescriptor(curProgrammer, programmerID);
				setTheEnvironmentVariablesAddtheBoardsTxt(contribEnv, confDesc, progBoard, false);
			}
		}

		// add the stuff that comes with the plugin that is marked as post
		setTheEnvironmentVariablesAddAFile(contribEnv, confDesc, pluginPostProcessingPlatformTxt);
		setTheEnvironmentVariablesAddtheBoardsTxt(contribEnv, confDesc, pluginPostProcessingBoardsTxt, false);

		// Do some coded post processing
		setTheEnvironmentVariablesPostProcessing(contribEnv, confDesc, boardsDescriptor);

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
	private static void setTheEnvironmentVariablesPostProcessing(IContributedEnvironment contribEnv,
			ICConfigurationDescription confDesc, InternalBoardDescriptor boardsDescriptor) {

		CompileOptions compileOptions = new CompileOptions(confDesc);
		// a save will overwrite the warning settings set by arduino
		compileOptions.save(confDesc);
		String actions[] = { ACTION_C_to_O, ACTION_CPP_to_O, ACTION_S_to_O, ACTION_OBJCOPY_to_HEX,
				ACTION_OBJCOPY_to_EEP, ACTION_SIZE, ACTION_AR, ACTION_C_COMBINE };
		for (String action : actions) {
			String recipeKey = get_ENV_KEY_RECIPE(action);
			String recipe = getBuildEnvironmentVariable(confDesc, recipeKey, new String(), false);
			recipe=recipe.replace("-DARDUINO_BSP_VERSION=\"${A.VERSION}\"", "\"-DARDUINO_BSP_VERSION=\\\"${A.VERSION}\\\"\"");


			if (ACTION_C_COMBINE.equals(action)) {
				recipe = recipe.replace("${A.BUILD.PATH}/core/sys", "${A.BUILD.PATH}/core/core/sys");
				recipe = recipe.replace("${A.BUILD.PATH}/sys", "${A.BUILD.PATH}/core/core/sys");
				setBuildEnvironmentVariable(contribEnv,confDesc, recipeKey, recipe);
			}

			String recipeParts[] = recipe.split(
					"(\"\\$\\{A.OBJECT_FILE}\")|(\\$\\{A.OBJECT_FILES})|(\"\\$\\{A.SOURCE_FILE}\")|(\"[^\"]*\\$\\{A.ARCHIVE_FILE}\")|(\"[^\"]*\\$\\{A.ARCHIVE_FILE_PATH}\")",
					3);
			switch (recipeParts.length) {
			case 0:
				setBuildEnvironmentVariable(contribEnv, confDesc, recipeKey + DOT + '1',
						"echo no command for \"{KEY}\".".replace(KEY, recipeKey));
				break;
			case 1:
				setBuildEnvironmentVariable(contribEnv, confDesc, recipeKey + DOT + '1', recipeParts[0]);
				break;
			case 2:
				setBuildEnvironmentVariable(contribEnv, confDesc, recipeKey + DOT + '1', recipeParts[0]);
				setBuildEnvironmentVariable(contribEnv, confDesc, recipeKey + DOT + '2', recipeParts[1]);
				break;
			case 3:
				setBuildEnvironmentVariable(contribEnv, confDesc, recipeKey + DOT + '1', recipeParts[0]);
				setBuildEnvironmentVariable(contribEnv, confDesc, recipeKey + DOT + '2', recipeParts[1]);
				setBuildEnvironmentVariable(contribEnv, confDesc, recipeKey + DOT + '3', recipeParts[2]);
				break;
			default:
				// this should never happen as the split is limited to 3
			}
		}

		String programmer = boardsDescriptor.getProgrammer();
		if (programmer.equalsIgnoreCase(Defaults.getDefaultUploadProtocol())) {
			String MComPort = boardsDescriptor.getUploadPort();
			if (MComPort.isEmpty()) {
				Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
						"Upload will fail due to missing upload port"));
			}
		}

		ArrayList<String> objcopyCommand = new ArrayList<>();

		// I'm looping through the set of variables to fix some things up
		try {
			IEnvironmentVariable[] curVariables = contribEnv.getVariables(confDesc);
			for (IEnvironmentVariable curVariable : curVariables) {
				String name = curVariable.getName();
				// Arduino uses the board approach for the tools.
				// as I'm not, therefore I mod the tools in the command to be
				// FQN
				if (name.startsWith("A.TOOLS.")) {
					String skipVars[]={"A.NETWORK.PASSWORD","A.NETWORK.PORT"};
					List<String> skipVarslist=Arrays.asList(skipVars);
					String toolID = curVariable.getName().split("\\.")[2];
					String recipe = curVariable.getValue();
					int indexOfVar = recipe.indexOf("${A.");
					while (indexOfVar != -1) {
						int endIndexOfVar = recipe.indexOf('}', indexOfVar);
						if (endIndexOfVar != -1) {
							String foundSuffix = recipe.substring(indexOfVar + 3, endIndexOfVar);
							String foundVar = "A" + foundSuffix;
							String replaceVar = "A.TOOLS." + toolID.toUpperCase() + foundSuffix;
							if( !skipVarslist.contains(foundVar)) {
							if (contribEnv.getVariable(foundVar, confDesc) == null) {// $NON-NLS-1$
								recipe = recipe.replaceAll(foundVar, replaceVar);
							}
							}
						}
						indexOfVar = recipe.indexOf("${A.", indexOfVar + 4);

					}
					setBuildEnvironmentVariable(contribEnv, confDesc, name, recipe);
				}
				if (name.startsWith("A.RECIPE.OBJCOPY.") && name.endsWith(".PATTERN")
						&& !curVariable.getValue().isEmpty()) {
					objcopyCommand.add(makeEnvironmentVar(name));

				}
				//Handle spaces in defines for USB stuff in windows
				if (Platform.getOS().equals(Platform.OS_WIN32)){
					if("A.BUILD.USB_MANUFACTURER".equalsIgnoreCase(name)){
					String moddedValue=curVariable.getValue().replace("\"","\\\"");
					setBuildEnvironmentVariable(contribEnv, confDesc, name, moddedValue);
					}
					if("A.BUILD.USB_PRODUCT".equalsIgnoreCase(name)){
					String moddedValue=curVariable.getValue().replace("\"","\\\"");
					setBuildEnvironmentVariable(contribEnv, confDesc, name, moddedValue);
					}
					if("A.BUILD.USB_FLAGS".equalsIgnoreCase(name)){
					String moddedValue=curVariable.getValue().replace("'", "\"");
					setBuildEnvironmentVariable(contribEnv, confDesc, name, moddedValue);
					}
					if("A.BUILD.EXTRA_FLAGS".equalsIgnoreCase(name)){
						//for radino
						//radinoCC1101.build.extra_flags=-DUSB_VID={build.vid} -DUSB_PID={build.pid} '-DUSB_PRODUCT={build.usb_product}'
						String moddedValue=curVariable.getValue().replace("'-DUSB_PRODUCT=${A.BUILD.USB_PRODUCT}'","\"-DUSB_PRODUCT=${A.BUILD.USB_PRODUCT}\"");
						setBuildEnvironmentVariable(contribEnv, confDesc, name, moddedValue);
					}
				}

			}

		} catch (Exception e) {
			Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, "parsing of upload recipe failed", e));
		}

		Collections.sort(objcopyCommand);
		setBuildEnvironmentVariable(contribEnv, confDesc, "JANTJE.OBJCOPY", StringUtil.join(objcopyCommand, "\n\t"));

		//handle the hooks
		setHookBuildEnvironmentVariable(contribEnv, confDesc, "A.JANTJE.PRE.LINK","A.RECIPE.HOOKS.LINKING.PRELINK.XX.PATTERN",false);
		setHookBuildEnvironmentVariable(contribEnv, confDesc, "A.JANTJE.POST.LINK","A.RECIPE.HOOKS.LINKING.POSTLINK.XX.PATTERN",true);
		setHookBuildEnvironmentVariable(contribEnv, confDesc, "A.JANTJE.PREBUILD","A.RECIPE.HOOKS.PREBUILD.XX.PATTERN",false);
		setHookBuildEnvironmentVariable(contribEnv, confDesc, "A.JANTJE.SKETCH.PREBUILD","A.RECIPE.HOOKS.SKETCH.PREBUILD.XX.PATTERN",false);
		setHookBuildEnvironmentVariable(contribEnv, confDesc, "A.JANTJE.SKETCH.POSTBUILD","A.RECIPE.HOOKS.SKETCH.POSTBUILD.XX.PATTERN",false);
		

	}



    private static void setHookBuildEnvironmentVariable(IContributedEnvironment contribEnv,
            ICConfigurationDescription confDesc, String varName, String hookName, boolean post) {
        String envVarString = new String();
        String postSeparator = "}\n\t";
        String preSeparator = "${";
        if (post) {
            postSeparator = "${";
            preSeparator = "}\n\t";
        }
        for (int numDigits = 1; numDigits <= 2; numDigits++) {
            int counter = 1;
            String hookVarName = hookName.replace("XX",
                    String.format("%0" + Integer.toString(numDigits) + "d", new Integer(counter)));
            while (!getBuildEnvironmentVariable(confDesc, hookVarName, "", true).isEmpty()) {
                envVarString = envVarString + preSeparator + hookVarName + postSeparator;
                hookVarName = hookName.replace("XX",
                        String.format("%0" + Integer.toString(numDigits) + "d", new Integer(++counter)));
            }
            if (!envVarString.isEmpty()) {
                setBuildEnvironmentVariable(contribEnv, confDesc, varName, envVarString);
            }
        }
    }
    /**
	 * When parsing boards.txt and platform.txt some processing needs to be done to
	 * get "acceptable environment variable values" This method does the parsing
	 * {xx} is replaced with ${XX} if to uppercase is true {xx} is replaced with
	 * ${xx} if to uppercase is false
	 *
	 * @param inputString
	 *            the value string as read from the file
	 * @return the string to be stored as value for the environment variable
	 */
	public static String MakeEnvironmentString(String inputString, String keyPrefix, boolean touppercase) {
		try {
		String ret = inputString.replaceAll("\\{(?!\\{)", "\\${" + keyPrefix);
		if (!touppercase) {
			return ret;
		}
		StringBuilder sb = new StringBuilder(ret);
		String regex = "\\{[^}]*\\}";
		Pattern p = Pattern.compile(regex); // Create the pattern.
		Matcher matcher = p.matcher(sb); // Create the matcher.
		while (matcher.find()) {
			String buf = sb.substring(matcher.start(), matcher.end()).toUpperCase();
			sb.replace(matcher.start(), matcher.end(), buf);
		}
		return sb.toString();}
		catch (Exception e){
			Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
					"Failed to parse environment var "+inputString, e));
			return inputString;
		}
	}

	/**
	 * When parsing boards.txt and platform.txt some processing needs to be done to
	 * get "acceptable environment variable keys" This method does the parsing some
	 * examples on windows "test.windows" becomes "A.TEST" "test.linux" becomes
	 * "A.TEST.LINUX"
	 *
	 * on Linux "test.windows" becomes "A.TEST.WINDOWS" "test.linux" becomes
	 * "A.TEST"
	 *
	 *
	 * @param inputString
	 *            the key string as read from the file
	 * @return the string to be used as key for the environment variable
	 */
	private static String MakeKeyString(String string) {

		return MakeKeyString(ERASE_START, string);
	}

	private static String MakeKeyString(String prefix, String string) {
		String osString = "\\.\\.";
		if (Platform.getOS().equals(Platform.OS_LINUX)) {
			osString = "\\.LINUX";
		} else if (Platform.getOS().equals(Platform.OS_WIN32)) {
			osString = "\\.WINDOWS";
		}
		return prefix + string.toUpperCase().replaceAll(osString, new String());
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

		// List<ILanguageSettingsProvider> providers;
		// if (cfgDescription instanceof ILanguageSettingsProvidersKeeper) {
		// providers = new ArrayList<>(
		// ((ILanguageSettingsProvidersKeeper)
		// cfgDescription).getLanguageSettingProviders());
		// for (ILanguageSettingsProvider provider : providers) {
		// if ((provider instanceof AbstractBuiltinSpecsDetector)) { //
		// basically
		// // check
		// // for
		// // working
		// // copy
		// // clear and reset isExecuted flag
		// ((AbstractBuiltinSpecsDetector) provider).clear();
		// }
		// }
		// }
	}

	/**
	 * Given a source file calculates the base of the output file. this method may
	 * not be needed if I can used the eclipse default behavior. However the eclipse
	 * default behavior is different from the arduino default behavior. So I keep it
	 * for now and we'll see how it goes The eclipse default behavior is (starting
	 * from the project folder [configuration]/Source The Arduino default behavior
	 * is all in 1 location (so no subfolders)
	 *
	 * @param Source
	 *            The source file to find the
	 * @return The base file name for the ouput if Source is "file.cpp" the output
	 *         is "file.cpp"
	 */
	public static IPath GetOutputName(IPath Source) {
		return Source;
	}

	/**
	 * Converts a name to a tagged environment variable if variableName ="this" the
	 * output is "${this}"
	 *
	 * @param variableName
	 * @return
	 */
	private static String makeEnvironmentVar(String variableName) {
		return "${" + variableName + '}';
	}

	/**
	 * creates links to the root files and folders of the source location
	 *
	 * @param source
	 *            the location where the files are that need to be linked to
	 * @param target
	 *            the location where the links are to be created
	 */
	public static void linkDirectory(IProject project, IPath source, IPath target) {

		File[] a = source.toFile().listFiles();
		if (a == null) {
			if(!myHasBeenLogged) {
			Common.log(new Status(IStatus.INFO, Const.CORE_PLUGIN_ID,
					Messages.Helpers_error_link_folder_is_empty.replace(FILE, source.toOSString()), null));
			myHasBeenLogged=true;
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

}
