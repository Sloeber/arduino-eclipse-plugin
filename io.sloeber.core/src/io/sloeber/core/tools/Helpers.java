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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
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
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.parser.util.StringUtil;
import org.eclipse.cdt.core.settings.model.CIncludePathEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSetting;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.language.settings.providers.AbstractBuiltinSpecsDetector;
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

import cc.arduino.packages.discoverers.NetworkDiscovery;
import io.sloeber.common.Common;
import io.sloeber.common.ConfigurationPreferences;
import io.sloeber.common.Const;
import io.sloeber.core.InternalBoardDescriptor;
import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.Defaults;
import io.sloeber.core.managers.ArduinoPlatform;
import io.sloeber.core.managers.Manager;
import io.sloeber.core.managers.ToolDependency;

/**
 * ArduinoHelpers is a static class containing general purpose functions
 *
 * @author Jan Baeyens
 *
 */
public class Helpers extends Common {
	private static final String ARDUINO_CORE_FOLDER_NAME = "cores"; //$NON-NLS-1$
	private static final String ARDUINO_CORE_BUILD_FOLDER_NAME = "core"; //$NON-NLS-1$
	private static final String BUILD_PATH_SYSCALLS_SAM3 = "\"{build.path}/syscalls_sam3.c.o\""; //$NON-NLS-1$
	private static final String BUILD_PATH_ARDUINO_SYSCALLS_SAM3 = "\"{build.path}/" + ARDUINO_CORE_BUILD_FOLDER_NAME //$NON-NLS-1$
			+ "/syscalls_sam3.c.o\""; //$NON-NLS-1$
	private static final String BUILD_PATH_SYSCALLS_MTK = "\"{build.path}/syscalls_mtk.c.o\""; //$NON-NLS-1$
	private static final String BUILD_PATH_ARDUINO_SYSCALLS_MTK = "\"{build.path}/" + ARDUINO_CORE_BUILD_FOLDER_NAME //$NON-NLS-1$
			+ "/syscalls_mtk.c.o\""; //$NON-NLS-1$
	private static final String minusG = "-g "; //$NON-NLS-1$
	private static final String minusG2 = "-g2 "; //$NON-NLS-1$
	private static final String ENV_KEY_ARCHITECTURE = ERASE_START + "ARCHITECTURE"; //$NON-NLS-1$
	private static final String ENV_KEY_BUILD_VARIANT = ERASE_START + "BUILD.VARIANT"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_BUILD_VARIANT = ERASE_START + "JANTJE.BUILD_VARIANT"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_UPLOAD_TOOL = ERASE_START + "JANTJE.UPLOAD.TOOL"; //$NON-NLS-1$

	private static final String ACTION_PROGRAM = "PROGRAM"; //$NON-NLS-1$

	private static final String ENV_KEY_BUILD_ARCH = ERASE_START + "BUILD.ARCH"; //$NON-NLS-1$
	private static final String ENV_KEY_BUILD_CORE = ERASE_START + "BUILD.CORE"; //$NON-NLS-1$
	private static final String ENV_KEY_BUILD_GENERIC_PATH = ERASE_START + "BUILD.GENERIC.PATH"; //$NON-NLS-1$
	private static final String ENV_KEY_HARDWARE_PATH = ERASE_START + "RUNTIME.HARDWARE.PATH"; //$NON-NLS-1$
	private static final String ENV_KEY_PLATFORM_PATH = ERASE_START + "RUNTIME.PLATFORM.PATH"; //$NON-NLS-1$

	private static final String ENV_KEY_BUILD_PATH = ERASE_START + "BUILD.PATH"; //$NON-NLS-1$
	private static final String ENV_KEY_BUILD_PROJECT_NAME = ERASE_START + "BUILD.PROJECT_NAME"; //$NON-NLS-1$
	private static final String ENV_KEY_COMPILER_PATH = ERASE_START + "COMPILER.PATH"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_CORE_REFERENCED_PLATFORM_FILE = ERASE_START
			+ "JANTJE.REFERENCE.CORE.PLATFORM_FILE"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_VARIANT_REFERENCED_PLATFORM_FILE = ERASE_START
			+ "JANTJE.REFERENCE.VARIANT.PLATFORM_FILE"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_UPLOAD_REFERENCED_PLATFORM_FILE = ERASE_START
			+ "JANTJE.REFERENCE.UPLOAD.PLATFORM_FILE"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_REFERENCED_CORE = ERASE_START + "JANTJE.REFERENCED.CORE.FILE"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_REFERENCED_VARIANT_PATH = ERASE_START + "JANTJE.BUILD.VARIANT.PATH"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_BUILD_CORE = ERASE_START + "JANTJE.BUILD_CORE"; //$NON-NLS-1$
	// private static final String ENV_KEY_JANTJE_PACKAGE_NAME =
	// ENV_KEY_JANTJE_START + "PACKAGE.NAME"; //$NON-NLS-1$
	private static final String ENV_KEY_JANTJE_MAKE_LOCATION = ENV_KEY_JANTJE_START + "MAKE_LOCATION"; //$NON-NLS-1$
	private static final String TOOL_KEY = "\\$\\{TOOL}"; //$NON-NLS-1$
	private static final String FILE_KEY = "\\$\\{FILE}"; //$NON-NLS-1$
	private static final String BOARD_KEY = "\\$\\{BOARD}"; //$NON-NLS-1$

	/**
	 * This method is the internal working class that adds the provided include
	 * path to all configurations and languages.
	 *
	 * @param configurationDescription
	 *            The configuration description of the project to add it to
	 * @param IncludePath
	 *            The path to add to the include folders
	 * @see addLibraryDependency
	 *      {@link #addLibraryDependency(IProject, IProject)}
	 */
	private static void addIncludeFolder(ICConfigurationDescription configurationDescription, IPath IncludePath) {
		// find all languages
		ICFolderDescription folderDescription = configurationDescription.getRootFolderDescription();
		ICLanguageSetting[] languageSettings = folderDescription.getLanguageSettings();

		// Add include path to all languages
		for (int idx = 0; idx < languageSettings.length; idx++) {
			ICLanguageSetting lang = languageSettings[idx];
			String LangID = lang.getLanguageId();
			if (LangID != null) {
				if (LangID.startsWith("org.eclipse.cdt.")) { //$NON-NLS-1$
					ICLanguageSettingEntry[] OrgIncludeEntries = lang.getSettingEntries(ICSettingEntry.INCLUDE_PATH);
					ICLanguageSettingEntry[] IncludeEntries = new ICLanguageSettingEntry[OrgIncludeEntries.length + 1];
					System.arraycopy(OrgIncludeEntries, 0, IncludeEntries, 0, OrgIncludeEntries.length);
					IncludeEntries[OrgIncludeEntries.length] = new CIncludePathEntry(IncludePath,
							ICSettingEntry.VALUE_WORKSPACE_PATH); // (location.toString());

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
	 * @return true is a include path has been removed. False if the include
	 *         path remains unchanged.
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
	 * This method adds the provided path to the include path of all
	 * configurations and languages.
	 *
	 * @param project
	 *            The project to add it to
	 * @param IncludePath
	 *            The path to add to the include folders
	 * @see addLibraryDependency
	 *      {@link #addLibraryDependency(IProject, IProject)}
	 */
	public static void addIncludeFolder(IProject project, IPath IncludePath) {
		// find all languages
		ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
		ICProjectDescription projectDescription = mngr.getProjectDescription(project, true);
		ICConfigurationDescription configurationDescription = projectDescription.getDefaultSettingConfiguration();
		addIncludeFolder(configurationDescription, IncludePath);

		projectDescription.setActiveConfiguration(configurationDescription);
		projectDescription.setCdtProjectCreated();
		try {
			mngr.setProjectDescription(project, projectDescription, true, null);
		} catch (CoreException e) {
			Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, Messages.Helpers_Could_not_add_folder
					+ IncludePath.toOSString() + Messages.Helpers_To_include_path + project.getName(), e));
		}

	}

	public static void addCodeFolder(IProject project, Path toLinkFolder, String LinkName,
			ICConfigurationDescription configurationDescriptions[]) throws CoreException {
		for (ICConfigurationDescription curConfig : configurationDescriptions) {
			Helpers.addCodeFolder(project, toLinkFolder, LinkName, curConfig);
		}

	}

	/**
	 * Creates a folder and links the folder to an existing folder Parent
	 * folders of the target folder are created if needed. In case this method
	 * fails an error is logged.
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
			createNewFolder(project, target.toString(), URIUtil.toURI(source));
		} catch (CoreException e) {
			Common.log(
					new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, Messages.Helpers_Create_folder_failed + target, e));
		}
	}

	/**
	 * This method creates a link folder in the project and add the folder as a
	 * source path to the project it also adds the path to the include folder if
	 * the include path parameter points to a path that contains a subfolder
	 * named "utility" this subfolder will be added to the include path as well
	 * <br/>
	 * Forget about this. Arduino made this all so complicated I don't know
	 * anymore what needs to be added to what<br/>
	 * <br/>
	 *
	 * note Arduino has these subfolders in the libraries that need to be
	 * include.<br/>
	 * <br/>
	 *
	 * note that in the current eclipse version, there is no need to add the
	 * subfolder as a code folder. This may change in the future as it looks
	 * like a bug to me.<br/>
	 *
	 * @param project
	 * @param Path
	 * @throws CoreException
	 *
	 * @see addLibraryDependency
	 *      {@link #addLibraryDependency(IProject, IProject)}
	 */
	public static void addCodeFolder(IProject project, IPath toLinkFolder, String LinkName,
			ICConfigurationDescription configurationDescription) throws CoreException {
		IFolder link = project.getFolder(LinkName);

		LinkFolderToFolder(project, toLinkFolder, new Path(LinkName));

		// Now the folder has been created we need to make sure the special
		// folders are added to the path
		addIncludeFolder(configurationDescription, link.getFullPath());

		String possibleIncludeFolder = "utility"; //$NON-NLS-1$
		File file = toLinkFolder.append(possibleIncludeFolder).toFile();
		if (file.exists()) {
			addIncludeFolder(configurationDescription, link.getFullPath().append(possibleIncludeFolder));
		}

		possibleIncludeFolder = "src"; //$NON-NLS-1$
		file = toLinkFolder.append(possibleIncludeFolder).toFile();
		if (file.exists()) {
			addIncludeFolder(configurationDescription, link.getFullPath().append(possibleIncludeFolder));
		}

		possibleIncludeFolder = "arch"; //$NON-NLS-1$
		file = toLinkFolder.append(possibleIncludeFolder).toFile();
		if (file.exists()) {
			addIncludeFolder(configurationDescription,
					link.getFullPath().append(possibleIncludeFolder).append(makeEnvironmentVar(ENV_KEY_ARCHITECTURE)));
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
	 * source path to the project it also adds the path to the include folder if
	 * the include path parameter points to a path that contains a subfolder
	 * named "utility" this subfolder will be added to the include path as well
	 * <br/>
	 * <br/>
	 *
	 * note Arduino has these subfolders in the libraries that need to be
	 * include.<br/>
	 * <br/>
	 *
	 * note that in the current eclipse version, there is no need to add the
	 * subfolder as a code folder. This may change in the future as it looks
	 * like a bug to me.<br/>
	 *
	 * @param project
	 * @param Path
	 * @throws CoreException
	 *
	 * @see addLibraryDependency
	 *      {@link #addLibraryDependency(IProject, IProject)}
	 */
	public static void addCodeFolder(IProject project, Path Path, ICConfigurationDescription configurationDescription)
			throws CoreException {

		String NiceName = Path.lastSegment();
		addCodeFolder(project, Path, NiceName, configurationDescription);
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
		newnatures[0] = "org.eclipse.cdt.core.cnature"; //$NON-NLS-1$
		newnatures[1] = "org.eclipse.cdt.core.ccnature"; //$NON-NLS-1$
		newnatures[2] = "org.eclipse.cdt.managedbuilder.core.managedBuildNature"; //$NON-NLS-1$
		newnatures[3] = "org.eclipse.cdt.managedbuilder.core.ScannerConfigNature"; //$NON-NLS-1$
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
	public static void addFileToProject(IContainer container, Path path, InputStream contentStream,
			IProgressMonitor monitor, boolean overwrite) throws CoreException {
		final IFile file = container.getFile(path);
		file.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		if (overwrite && file.exists()) {
			file.delete(true, null);
			file.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}

		if (!file.exists() && (contentStream != null)) {
			file.create(contentStream, true, monitor);
		}
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
	public static void addArduinoCodeToProject(IProject project, ICConfigurationDescription configurationDescription)
			throws CoreException {

		String boardVariant = getBuildEnvironmentVariable(configurationDescription, ENV_KEY_BUILD_VARIANT,
				EMPTY_STRING); // $NON-NLS-1$
		String buildCoreFolder = getBuildEnvironmentVariable(configurationDescription, ENV_KEY_BUILD_CORE,
				EMPTY_STRING);
		String platformFile = getBuildEnvironmentVariable(configurationDescription, ENV_KEY_JANTJE_PLATFORM_FILE,
				EMPTY_STRING);
		String redirectCorePlatformFile = getBuildEnvironmentVariable(configurationDescription,
				ENV_KEY_JANTJE_CORE_REFERENCED_PLATFORM_FILE, platformFile);
		IPath corePath = new Path(redirectCorePlatformFile).removeLastSegments(1).append("cores") //$NON-NLS-1$
				.append(buildCoreFolder);

		addCodeFolder(project, corePath, ARDUINO_CODE_FOLDER_NAME + '/' + ARDUINO_CORE_BUILD_FOLDER_NAME,
				configurationDescription);
		if (boardVariant.isEmpty()) {
			// remove the existing link
			Helpers.removeCodeFolder(project, ARDUINO_CODE_FOLDER_NAME + "/variant"); //$NON-NLS-1$
		} else {
			String redirectVariantPath = getBuildEnvironmentVariable(configurationDescription,
					ENV_KEY_JANTJE_REFERENCED_VARIANT_PATH, EMPTY_STRING);
			IPath VariantFile;
			if (redirectVariantPath.isEmpty()) {
				VariantFile = new Path(platformFile).removeLastSegments(1).append(VARIANTS_FOLDER_NAME)
						.append(boardVariant);
			} else {
				VariantFile = new Path(redirectVariantPath).append(boardVariant);
			}
			Helpers.addCodeFolder(project, VariantFile, ARDUINO_CODE_FOLDER_NAME + "/variant", //$NON-NLS-1$
					configurationDescription);
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
	 *            if null a local folder is created using newFolderName if not
	 *            null a link folder is created with the name newFolderName and
	 *            pointing to linklocation
	 *
	 * @return nothing
	 * @throws CoreException
	 */
	public static void createNewFolder(IProject Project, String newFolderName, URI linklocation) throws CoreException {
		// IPath newFolderPath = Project.getFullPath().append(newFolderName);
		final IFolder newFolderHandle = Project.getFolder(newFolderName);
		if (linklocation != null) {
			newFolderHandle.createLink(linklocation, IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, null);
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
	 * platform.txt file. Here I set these values. This method should be called
	 * as first. This way the values in platform.txt and boards.txt will take
	 * precedence of the default values declared here
	 *
	 * @param contribEnv
	 * @param confDesc
	 * @param platformFile
	 *            Used to define the hardware as different settings are needed
	 *            for avr and sam
	 */
	private static void setTheEnvironmentVariablesSetTheDefaults(IContributedEnvironment contribEnv,
			ICConfigurationDescription confDesc, BoardDescriptor boardDescriptor) {
		// Set some default values because the platform.txt does not contain
		// them
		Path platformPath = new Path(boardDescriptor.getPlatformPath().toString());
		String architecture = boardDescriptor.getArchitecture();
		// String packagename = boardDescriptor.getPackage();
		int numSegmentsToSubtractForHardwarePath = 1;
		if (architecture.contains(DOT)) { // in case there is a version in the
			// path ignore the version
			numSegmentsToSubtractForHardwarePath = 3;
			architecture = platformPath.removeLastSegments(2).lastSegment();
			// packagename = platformPath.removeLastSegments(4).lastSegment();
		}

		setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_ARCHITECTURE, architecture.toUpperCase());
		setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_BUILD_ARCH, architecture.toUpperCase());
		setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_HARDWARE_PATH,
				platformPath.removeLastSegments(numSegmentsToSubtractForHardwarePath).toString());
		setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_PLATFORM_PATH, platformPath.toString());
		setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_SERIAL_PORT,
				makeEnvironmentVar(Const.ENV_KEY_JANTJE_UPLOAD_PORT));
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_MAKE_LOCATION,
					ConfigurationPreferences.getPathExtensionPath().toString() + '/');
		}

		// Build Time
		Date d = new Date();
		GregorianCalendar cal = new GregorianCalendar();
		long current = d.getTime() / 1000;
		long timezone = cal.get(Calendar.ZONE_OFFSET) / 1000;
		long daylight = cal.get(Calendar.DST_OFFSET) / 1000;
		// p.put("extra.time.utc", Long.toString(current));
		setBuildEnvironmentVariable(contribEnv, confDesc, "A.EXTRA.TIME.UTC", Long.toString(current)); //$NON-NLS-1$
		setBuildEnvironmentVariable(contribEnv, confDesc, "A.EXTRA.TIME.LOCAL", //$NON-NLS-1$
				Long.toString(current + timezone + daylight));
		setBuildEnvironmentVariable(contribEnv, confDesc, "A.EXTRA.TIME.ZONE", Long.toString(timezone)); //$NON-NLS-1$
		setBuildEnvironmentVariable(contribEnv, confDesc, "A.EXTRA.TIME.DTS", Long.toString(daylight)); //$NON-NLS-1$
		// End of Teensy specific settings

		// some glue to make it work
		String pathDelimiter = makeEnvironmentVar("PathDelimiter"); //$NON-NLS-1$
		setBuildEnvironmentVariable(contribEnv, confDesc, "PATH", //$NON-NLS-1$
				makeEnvironmentVar(ENV_KEY_COMPILER_PATH) + pathDelimiter
						+ makeEnvironmentVar(ENV_KEY_BUILD_GENERIC_PATH) + pathDelimiter + makeEnvironmentVar("PATH")); //$NON-NLS-1$

		setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_BUILD_PATH,
				makeEnvironmentVar("ProjDirPath") + '/' + makeEnvironmentVar("ConfigName")); //$NON-NLS-1$ //$NON-NLS-2$

		setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_BUILD_PROJECT_NAME, makeEnvironmentVar("ProjName")); //$NON-NLS-1$

		// if (firstTime)
		String sizeSwitch = getBuildEnvironmentVariable(confDesc, ENV_KEY_JANTJE_SIZE_SWITCH, EMPTY_STRING, false);
		if (sizeSwitch.isEmpty()) {
			setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_SIZE_SWITCH,
					makeEnvironmentVar(get_ENV_KEY_RECIPE(ACTION_SIZE)));
		} else {
			sizeSwitch.toString();
		}

		// Set the warning level default off like arduino does
		if (getBuildEnvironmentVariable(confDesc, ENV_KEY_JANTJE_WARNING_LEVEL, EMPTY_STRING).isEmpty()) {
			setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_WARNING_LEVEL, ENV_KEY_WARNING_LEVEL_OFF);
		}

		// Save some info so we can find the tool paths
		// setBuildEnvironmentVariable(contribEnv, confDesc,
		// ENV_KEY_JANTJE_PACKAGE_NAME, packagename);

	}

	private static void setTheEnvironmentVariablesAddAFile(IContributedEnvironment contribEnv,
			ICConfigurationDescription confDesc, File envVarFile) {
		setTheEnvironmentVariablesAddAFile(ERASE_START, contribEnv, confDesc, envVarFile);
	}

	/**
	 * This method parses a file with environment variables like the
	 * platform.txt file for values to be added to the environment variables
	 *
	 * @param contribEnv
	 * @param confDesc
	 * @param envVarFile
	 *            The file to parse
	 */
	private static void setTheEnvironmentVariablesAddAFile(String prefix, IContributedEnvironment contribEnv,
			ICConfigurationDescription confDesc, File envVarFile) {
		try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(envVarFile));
				BufferedReader br = new BufferedReader(new InputStreamReader(dataInputStream));) {
			String strLine;

			// Read File Line By Line
			while ((strLine = br.readLine()) != null) {
				String realData[] = strLine.split("#");// Ignore //$NON-NLS-1$
				// everything after
				// first #
				if (realData.length > 0) {
					String var[] = realData[0].split("=", 2); // look //$NON-NLS-1$
					// for assignment
					if (var.length == 2) {
						String value = var[1];
						if (value.contains(BUILD_PATH_SYSCALLS_SAM3)) {
							value = value.replace(BUILD_PATH_SYSCALLS_SAM3, BUILD_PATH_ARDUINO_SYSCALLS_SAM3);
						} else if (value.contains(BUILD_PATH_SYSCALLS_MTK)) {
							value = value.replace(BUILD_PATH_SYSCALLS_MTK, BUILD_PATH_ARDUINO_SYSCALLS_MTK);
						}
						setBuildEnvironmentVariable(contribEnv, confDesc, MakeKeyString(prefix, var[0]),
								MakeEnvironmentString(value, Const.ERASE_START));
					}
				}
			}
		} catch (FileNotFoundException e) {
			Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
					Messages.Helpers_Error_parsing + envVarFile.toString() + Messages.Helpers_File_does_not_exists, e));
		} catch (IOException e) {
			Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
					Messages.Helpers_Error_parsing + envVarFile.toString() + Messages.Helpers_IO_exception, e));
		}
	}

	/**
	 * This method parses the boards.txt file for values to be added to the
	 * environment variables First it adds all the variables based on the board
	 * name [boardID].[key]=[value] results in [key]=[value] (taking in account
	 * the modifiers) Then it parses for the menu variables
	 * menu.[menuID].[boardID].[selectionID].[key]=[value] results in
	 * [key]=[value] (taking in account the modifiers)
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
				Common.log(new Status(IStatus.INFO, Const.CORE_PLUGIN_ID,
						Messages.Helpers_The_project + confDesc.getProjectDescription().getProject().getName()
								+ Messages.Helpers_Invalid_boards_config + confDesc.getName()
								+ Messages.Helpers_boards_file + boardsFile.getTxtFile().toString()
								+ Messages.Helpers_Boards_id + boardID));
			}
			return;
		}
		for (Entry<String, String> currentPair : boardSectionMap.entrySet()) {
			// if it is not a menu item add it
			if (!currentPair.getKey().startsWith(Messages.Helpers_menu)) {
				String keyString = MakeKeyString(currentPair.getKey());
				String valueString = MakeEnvironmentString(currentPair.getValue(), Const.ERASE_START);
				contribEnv.addVariable(new EnvironmentVariable(keyString, valueString), confDesc);
			}
		}
		for (Entry<String, String> currentPair : boardSectionMap.entrySet()) {
			// if it is not a menu item add it
			if (currentPair.getKey().startsWith(Messages.Helpers_menu)) {

				String[] keySplit = currentPair.getKey().split("\\."); //$NON-NLS-1$
				String menuID = keySplit[1];
				String menuItemID = keySplit[2];

				if (menuItemID.equalsIgnoreCase(options.get(menuID.toUpperCase()))) {
					// we also need to skip the name
					String StartValue = MENU + DOT + menuID + DOT + menuItemID + DOT; // $NON-NLS-1$
					try {
						String keyString = MakeKeyString(currentPair.getKey().substring(StartValue.length()));
						String valueString = MakeEnvironmentString(currentPair.getValue(), Const.ERASE_START);
						contribEnv.addVariable(new EnvironmentVariable(keyString, valueString), confDesc);
					} catch (StringIndexOutOfBoundsException e) {
						// ignore as this is the case when the menu name is
						// processed
					}

				}
			}
		}

	}

	private static void addPlatformFileTools(ArduinoPlatform platform, IContributedEnvironment contribEnv,
			ICConfigurationDescription confDesc) {
		if (platform.getToolsDependencies() != null) {
			for (ToolDependency tool : platform.getToolsDependencies()) {
				String keyString = MakeKeyString("runtime.tools." + tool.getName() + ".path"); //$NON-NLS-1$ //$NON-NLS-2$
				String valueString = new Path(tool.getTool().getInstallPath().toString()).toString();
				setBuildEnvironmentVariable(contribEnv, confDesc, keyString, valueString);
				keyString = MakeKeyString("runtime.tools." + tool.getName() + tool.getVersion() + ".path"); //$NON-NLS-1$ //$NON-NLS-2$
				setBuildEnvironmentVariable(contribEnv, confDesc, keyString, valueString);
				keyString = MakeKeyString("runtime.tools." + tool.getName() + '-' + tool.getVersion() + ".path"); //$NON-NLS-1$ //$NON-NLS-2$
				setBuildEnvironmentVariable(contribEnv, confDesc, keyString, valueString);
			}
		}
	}

	private static void setTheEnvironmentVariablesAddThePlatformInfo(IContributedEnvironment contribEnv,
			ICConfigurationDescription confDesc) {
		String platformFileName = getBuildEnvironmentVariable(confDesc, Const.ENV_KEY_JANTJE_PLATFORM_FILE,
				Const.EMPTY_STRING);
		String referenceCoredPlatformFileName = getBuildEnvironmentVariable(confDesc,
				ENV_KEY_JANTJE_CORE_REFERENCED_PLATFORM_FILE, Const.EMPTY_STRING);

		ArduinoPlatform platform = null;
		String curversion = null;
		for (ArduinoPlatform curPlatform : Manager.getInstalledPlatforms()) {
			addPlatformFileTools(curPlatform, contribEnv, confDesc);
			if (curPlatform.isInstalled() && "avr".equalsIgnoreCase(curPlatform.getArchitecture()) //$NON-NLS-1$
					&& "arduino".equalsIgnoreCase(curPlatform.getPackage().getMaintainer())) { //$NON-NLS-1$
				if (Manager.compareVersions(curPlatform.getVersion(), curversion) > 0) {
					curversion = curPlatform.getVersion();
					platform = curPlatform;
				}
			}
		}
		// add the newest arduino avr platform again for the idiots wanting to
		// reference arduino without referencing it
		if (platform != null) {
			addPlatformFileTools(platform, contribEnv, confDesc);
		}

		// by adding the referencenced platform after the real platform
		platform = Manager.getPlatform(new File(referenceCoredPlatformFileName));
		if (platform != null) {
			addPlatformFileTools(platform, contribEnv, confDesc);
		}
		// and the real platform
		platform = Manager.getPlatform(new File(platformFileName));
		if (platform != null) {
			// skip if this platform has no platform.txt. This is to fix
			// problem with arduboy that provide tooldependencies but no
			// platform.txt
			if (platform.getPlatformFile().exists()) {
				addPlatformFileTools(platform, contribEnv, confDesc);
			}
		}
	}

	/**
	 * This method creates environment variables based on the platform.txt and
	 * boards.txt. platform.txt is processed first and then boards.txt. This way
	 * boards.txt settings can overwrite common settings in platform.txt The
	 * environment variables are only valid for the project given as parameter
	 * The project properties are used to identify the boards.txt and
	 * platform.txt as well as the board id to select the settings in the
	 * board.txt file At the end also the path variable is set
	 *
	 * from arduino IDE 1.6.5 an additional file generated by the arduino ide is
	 * processed. This is the first file processed.
	 *
	 * To be able to quickly fix boards.txt and platform.txt problems I also
	 * added a pre and post platform and boards files that are processed before
	 * and after the arduino delivered boards.txt file.
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

		// first remove all Arduino Variables so there is no memory effect
		removeAllEraseEnvironmentVariables(contribEnv, confDesc);

		setTheEnvironmentVariablesSetTheDefaults(contribEnv, confDesc, boardsDescriptor);

		// add the stuff that comes with the plugin that are marked as pre
		setTheEnvironmentVariablesAddAFile(contribEnv, confDesc, pluginPreProcessingPlatformTxt);
		setTheEnvironmentVariablesAddtheBoardsTxt(contribEnv, confDesc, pluginPreProcessingBoardsTxt, false);

		// // Then add the programmers file
		// setTheEnvironmentVariablesAddAFile(ENV_KEY_PROGRAMMERS_START,
		// contribEnv, confDesc, localProgrammers.getTxtFile());

		// Do some magic for the arduino:arduino stuff
		setTheEnvironmentVariablesRedirectToOtherVendors(contribEnv, confDesc, boardsDescriptor);

		// process the platform file that is referenced in the build.core of the
		// boards.txt file
		File coreReferencedPlatformFilename = new File(Common.getBuildEnvironmentVariable(confDesc,
				ENV_KEY_JANTJE_CORE_REFERENCED_PLATFORM_FILE, EMPTY_STRING));
		File upLoadreferencedPlatformFilename = new File(Common.getBuildEnvironmentVariable(confDesc,
				ENV_KEY_JANTJE_UPLOAD_REFERENCED_PLATFORM_FILE, EMPTY_STRING));
		File variantReferencedPlatformFilename = new File(Common.getBuildEnvironmentVariable(confDesc,
				ENV_KEY_JANTJE_VARIANT_REFERENCED_PLATFORM_FILE, EMPTY_STRING));
		if (upLoadreferencedPlatformFilename.exists()) {
			setTheEnvironmentVariablesAddAFile(contribEnv, confDesc, upLoadreferencedPlatformFilename);
		}
		if (variantReferencedPlatformFilename.exists()
				&& !variantReferencedPlatformFilename.equals(upLoadreferencedPlatformFilename)) {
			setTheEnvironmentVariablesAddAFile(contribEnv, confDesc, variantReferencedPlatformFilename);
		}
		if (coreReferencedPlatformFilename.exists()
				&& !coreReferencedPlatformFilename.equals(variantReferencedPlatformFilename)) {
			setTheEnvironmentVariablesAddAFile(contribEnv, confDesc, coreReferencedPlatformFilename);
		}
		File localPlatfrmFilename = new File(boardsDescriptor.getPlatformFile());
		// process the platform file next to the selected boards.txt
		if (localPlatfrmFilename.exists()) {
			setTheEnvironmentVariablesAddAFile(contribEnv, confDesc, localPlatfrmFilename);
		}

		setTheEnvironmentVariablesAddThePlatformInfo(contribEnv, confDesc);

		// add the boards file
		setTheEnvironmentVariablesAddtheBoardsTxt(contribEnv, confDesc, boardsDescriptor, true);

		// Then add the programmers file
		// TOFIX this code is important for the programmers but due to the
		// changes needs some work
		String programmer = contribEnv.getVariable(get_Jantje_KEY_PROTOCOL(ACTION_UPLOAD), confDesc).getValue();
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
		setTheEnvironmentVariablesPostProcessing(contribEnv, confDesc);

	}

	/**
	 * This method is to support the [vendor]:[value] as described in
	 * https://github.com/arduino/Arduino/wiki/Arduino-IDE-1.5-3rd-party-
	 * Hardware-specification This method parses the boards.txt file for
	 * myboard.build.core myboard.build.variant currently not supported
	 * myboard.upload.tool myboard.bootloader.tool
	 *
	 * in case myboard.build.core is of type [vendor]:[value]
	 * PATH_VARIABLE_NAME_ARDUINO_PLATFORM is changed to the correct value in
	 * case myboard.build.variant is of type [vendor]:[value]
	 * PATH_VARIABLE_NAME_ARDUINO_PINS is changed to the correct value
	 *
	 * this method also sets ENV_KEY_JANTJE_BUILD_CORE and
	 * ENV_KEY_JANTJE_BUILD_VARIANT to [value] of respectively
	 * myboard.build.core and myboard.build.variant
	 *
	 * This method relies on the post processing to set
	 * A.BUILD.CORE=${ENV_KEY_JANTJE_BUILD_CORE}
	 * A.BUILD.VARIANT=${ENV_KEY_JANTJE_BUILD_VARIANT}
	 *
	 * @param contribEnv
	 * @param confDesc
	 * @param boardsFile
	 * @param boardID
	 */
	private static void setTheEnvironmentVariablesRedirectToOtherVendors(IContributedEnvironment contribEnv,
			ICConfigurationDescription confDesc, InternalBoardDescriptor boardsDescriptor) {
		Map<String, String> boardInfo = boardsDescriptor.getTxtFile().getSection(boardsDescriptor.getBoardID());
		if (boardInfo == null) {
			return; // there is a problem with the board ID
		}
		String core = boardInfo.get("build.core"); //$NON-NLS-1$
		String variant = boardInfo.get("build.variant"); //$NON-NLS-1$
		String upload = boardInfo.get("upload.tool"); //$NON-NLS-1$
		if (core != null) {
			String valueSplit[] = core.split(COLON);
			if (valueSplit.length == 2) {
				String refVendor = valueSplit[0];
				String actualValue = valueSplit[1];
				Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_BUILD_CORE, actualValue);
				IPath referencdPlatformFile = findReferencedPlatformFile(refVendor, boardsDescriptor.getArchitecture());
				if (referencdPlatformFile == null) {
					Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
							Messages.Helpers_tool_reference_missing.replaceAll(TOOL_KEY, core)
									.replaceAll(FILE_KEY, boardsDescriptor.getBoardsFile())
									.replaceAll(BOARD_KEY, boardsDescriptor.getBoardID())));
				} else {
					setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_REFERENCED_CORE,
							referencdPlatformFile.removeLastSegments(1).append(ARDUINO_CORE_FOLDER_NAME)
									.append(actualValue).toString());
					setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_CORE_REFERENCED_PLATFORM_FILE,
							referencdPlatformFile.toString());
				}
			} else {
				setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_BUILD_CORE, core);
			}
		}
		if (variant != null) {
			String valueSplit[] = variant.split(COLON);
			if (valueSplit.length == 2) {
				String refVendor = valueSplit[0];
				String actualValue = valueSplit[1];
				Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_BUILD_VARIANT, actualValue);
				IPath referencdPlatformFile = findReferencedPlatformFile(refVendor, boardsDescriptor.getArchitecture());
				if (referencdPlatformFile == null) {
					Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
							Messages.Helpers_tool_reference_missing.replaceAll(TOOL_KEY, variant)
									.replaceAll(FILE_KEY, boardsDescriptor.getBoardsFile())
									.replaceAll(BOARD_KEY, boardsDescriptor.getBoardID())));
				} else {
					IPath referencedVariant = referencdPlatformFile.removeLastSegments(1).append(VARIANTS_FOLDER_NAME);
					Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_REFERENCED_VARIANT_PATH,
							referencedVariant.toString());
					setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_VARIANT_REFERENCED_PLATFORM_FILE,
							referencdPlatformFile.toString());
				}
			} else {
				setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_BUILD_VARIANT, variant);
			}
		}
		if (upload != null) {
			String valueSplit[] = upload.split(COLON);
			if (valueSplit.length == 2) {
				String refVendor = valueSplit[0];
				String actualValue = valueSplit[1];
				Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_UPLOAD_TOOL, actualValue);
				IPath referencdPlatformFile = findReferencedPlatformFile(refVendor, boardsDescriptor.getArchitecture());
				if (referencdPlatformFile == null) {
					Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
							Messages.Helpers_tool_reference_missing.replaceAll(TOOL_KEY, upload)
									.replaceAll(FILE_KEY, boardsDescriptor.getBoardsFile())
									.replaceAll(BOARD_KEY, boardsDescriptor.getBoardID())));
				} else {
					Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_UPLOAD_TOOL, actualValue);
					setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_UPLOAD_REFERENCED_PLATFORM_FILE,
							referencdPlatformFile.toString());
				}
			} else {
				setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_UPLOAD_TOOL, upload);
			}
		}

	}

	/**
	 * This method looks for a referenced platformFile. Ask the boards manager
	 * to find the latest installed vendor/architecture platform file
	 *
	 * If this is not found there is still sme old code that probably can be
	 * deleted.
	 *
	 * @param vendor
	 * @param architecture
	 * @return
	 */
	private static IPath findReferencedPlatformFile(String vendor, String architecture) {
		// ask the boardsmanager for the platform file
		IPath ret = Manager.getPlatformFile(vendor, architecture);
		return ret;

	}

	/**
	 * Following post processing is done
	 *
	 * the macro expansion resolves the "file tag" Therefore I split the
	 * "recipe" patterns in 2 parts (before and after the "file tag") the
	 * pattern in the toolchain is then ${first part} ${files} ${second part}
	 *
	 * The handling of the upload variables is done differently in arduino than
	 * here. This is taken care of here. for example the output of this input
	 * tools.avrdude.upload.pattern="{cmd.path}" "-C{config.path}"
	 * {upload.verbose} is changed as if it were the output of this input
	 * tools.avrdude.upload.pattern="{tools.avrdude.cmd.path}"
	 * "-C{tools.avrdude.config.path}" {tools.avrdude.upload.verbose}
	 *
	 * if a programmer is selected different from default some extra actions are
	 * done here so no special code is needed to handle programmers
	 *
	 * @param contribEnv
	 * @param confDesc
	 */
	private static void setTheEnvironmentVariablesPostProcessing(IContributedEnvironment contribEnv,
			ICConfigurationDescription confDesc) {

		String actions[] = { ACTION_C_to_O, ACTION_CPP_to_O, ACTION_S_to_O, ACTION_OBJCOPY_to_HEX,
				ACTION_OBJCOPY_to_EEP, ACTION_SIZE, ACTION_AR, ACTION_C_COMBINE };
		for (String action : actions) {
			String recipeKey = get_ENV_KEY_RECIPE(action);
			String recipe = getBuildEnvironmentVariable(confDesc, recipeKey, EMPTY_STRING, false);

			String recipeParts[] = recipe.split(
					"(\"\\$\\{A.OBJECT_FILE}\")|(\\$\\{A.OBJECT_FILES})|(\"\\$\\{A.SOURCE_FILE}\")|(\"[^\"]*\\$\\{A.ARCHIVE_FILE}\")|(\"[^\"]*\\$\\{A.ARCHIVE_FILE_PATH}\")", //$NON-NLS-1$
					3);
			switch (recipeParts.length) {
			case 0:
				Common.setBuildEnvironmentVariable(contribEnv, confDesc, recipeKey + DOT + '1',
						Messages.Helpers_No_command_for + recipeKey);
				break;
			case 1:
				Common.setBuildEnvironmentVariable(contribEnv, confDesc, recipeKey + DOT + '1', recipeParts[0]);
				break;
			case 2:
				Common.setBuildEnvironmentVariable(contribEnv, confDesc, recipeKey + DOT + '1', recipeParts[0]);
				Common.setBuildEnvironmentVariable(contribEnv, confDesc, recipeKey + DOT + '2', recipeParts[1]);
				break;
			case 3:
				Common.setBuildEnvironmentVariable(contribEnv, confDesc, recipeKey + DOT + '1', recipeParts[0]);
				Common.setBuildEnvironmentVariable(contribEnv, confDesc, recipeKey + DOT + '2', recipeParts[1]);
				Common.setBuildEnvironmentVariable(contribEnv, confDesc, recipeKey + DOT + '3', recipeParts[2]);
				break;
			default:
				// this should never happen as the split is limited to 3
			}
		}

		String programmer = contribEnv.getVariable(get_Jantje_KEY_PROTOCOL(ACTION_UPLOAD), confDesc).getValue();
		if (programmer.equalsIgnoreCase(Defaults.getDefaultUploadProtocol())) {
			IEnvironmentVariable uploadToolVar = contribEnv.getVariable(ENV_KEY_JANTJE_UPLOAD_TOOL, confDesc);
			IEnvironmentVariable comportVar = contribEnv.getVariable(Const.ENV_KEY_JANTJE_UPLOAD_PORT, confDesc);
			if ((uploadToolVar == null) || (comportVar == null)) {
				Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
						"Upload will fail due to missing upload parameters")); //$NON-NLS-1$
			} else {
				String uploadTool = uploadToolVar.getValue();

				String MComPort = comportVar.getValue();
				String host = getHostFromComPort(MComPort);
				if (host != null) {
					String platform = contribEnv.getVariable(Const.ENV_KEY_JANTJE_ARCITECTURE_ID, confDesc).getValue();
					setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_NETWORK_PORT,
							NetworkDiscovery.getPort(host));
					setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_NETWORK_AUTH,
							NetworkDiscovery.hasAuth(host) ? TRUE : FALSE);
					setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_SERIAL_PORT, host);

					try {
						String key = ERASE_START + platform.toUpperCase() + DOT + "NETWORK" + DOT //$NON-NLS-1$
								+ ACTION_UPLOAD.toUpperCase() + DOT + ENV_TOOL;
						String networkUploadTool = contribEnv.getVariable(key, confDesc).getValue();
						if (!networkUploadTool.isEmpty()) {
							uploadTool = networkUploadTool;
							setBuildEnvironmentVariable(contribEnv, confDesc, get_ENV_KEY_TOOL(UPLOAD_CLASS),
									UPLOAD_CLASS_DEFAULT);
							setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_RESET_BEFORE_UPLOAD, FALSE);
						}
					} catch (Exception e) {
						// simply ignore
					}
				}
				setBuildEnvironmentVariable(contribEnv, confDesc, get_Jantje_KEY_RECIPE(ACTION_UPLOAD),
						makeEnvironmentVar(get_ENV_KEY_RECIPE(uploadTool, ACTION_UPLOAD)));
				setBuildEnvironmentVariable(contribEnv, confDesc, get_ENV_KEY_TOOL(ACTION_PROGRAM),
						makeEnvironmentVar(get_ENV_KEY_TOOL(ACTION_UPLOAD)));
			}
		} else {
			String uploadTool = new String();
			try {
				uploadTool = contribEnv.getVariable("A.PROGRAM.TOOL", confDesc).getValue(); //$NON-NLS-1$
			} catch (Exception e) {
				Common.log(
						new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, Messages.Helpers_ProblemInProgrammerFie, e));
			}
			setBuildEnvironmentVariable(contribEnv, confDesc, get_Jantje_KEY_RECIPE(ACTION_UPLOAD),
					makeEnvironmentVar(get_ENV_KEY_RECIPE(uploadTool, ACTION_PROGRAM)));
			setBuildEnvironmentVariable(contribEnv, confDesc, get_ENV_KEY_TOOL(ACTION_PROGRAM), uploadTool);

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
				if (name.startsWith("A.TOOLS.")) { //$NON-NLS-1$
					String toolID = curVariable.getName().split("\\.")[2]; //$NON-NLS-1$
					String recipe = curVariable.getValue();
					int indexOfVar = recipe.indexOf("${A."); //$NON-NLS-1$
					while (indexOfVar != -1) {
						int endIndexOfVar = recipe.indexOf('}', indexOfVar);
						if (endIndexOfVar != -1) {
							String foundSuffix = recipe.substring(indexOfVar + 3, endIndexOfVar);
							String foundVar = "A" + foundSuffix; //$NON-NLS-1$
							String replaceVar = "A.TOOLS." + toolID.toUpperCase() + foundSuffix; //$NON-NLS-1$
							if (contribEnv.getVariable(foundVar, confDesc) == null) {// $NON-NLS-1$
								recipe = recipe.replaceAll(foundVar, replaceVar);
							}
						}
						indexOfVar = recipe.indexOf("${A.", indexOfVar + 4); //$NON-NLS-1$

					}
					setBuildEnvironmentVariable(contribEnv, confDesc, name, recipe);
				}
				if (name.startsWith("A.RECIPE.OBJCOPY.") && name.endsWith(".PATTERN") //$NON-NLS-1$ //$NON-NLS-2$
						&& !curVariable.getValue().isEmpty()) {
					objcopyCommand.add(makeEnvironmentVar(name));

				}
			}

		} catch (Exception e) {
			Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, "parsing of upload recipe failed", e)); //$NON-NLS-1$
		}

		Collections.sort(objcopyCommand);
		setBuildEnvironmentVariable(contribEnv, confDesc, "JANTJE.OBJCOPY", StringUtil.join(objcopyCommand, "\n\t")); //$NON-NLS-1$ //$NON-NLS-2$

		// if we have a variant defined in a menu option we need to
		// grab the value in ENV_KEY_BUILD_VARIANT and put it in
		// ENV_KEY_JANTJE_BUILD_VARIANT
		// because ENV_KEY_JANTJE_BUILD_VARIANT is empty
		String variant = getBuildEnvironmentVariable(confDesc, ENV_KEY_JANTJE_BUILD_VARIANT, "", true); //$NON-NLS-1$
		if (variant.isEmpty()) {
			variant = getBuildEnvironmentVariable(confDesc, ENV_KEY_BUILD_VARIANT, "", true); //$NON-NLS-1$
			setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_BUILD_VARIANT, variant);
		}

		// link build.core to jantje.build.core
		setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_BUILD_CORE,
				makeEnvironmentVar(ENV_KEY_JANTJE_BUILD_CORE));
		// link build.variant to jantje.build.variant
		setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_BUILD_VARIANT,
				makeEnvironmentVar(ENV_KEY_JANTJE_BUILD_VARIANT));
	}

	/**
	 * Converts the CPP and C compiler flags to not optimize for space/size and
	 * to leave symbols in. These changes allow step through debugging with JTAG
	 * and Dragon AVR
	 *
	 * @param confDesc
	 * @param envManager
	 * @param contribEnv
	 */
	// TODO reimplement this debug code one way or another

	private static void setTheEnvironmentVariablesModifyDebugCompilerSettings(ICConfigurationDescription confDesc,
			IEnvironmentVariableManager envManager, IContributedEnvironment contribEnv) {

		// Modify the compiler flags for the debug configuration
		// Replace "-g" with "-g2"
		// Replace "-Os" with ""
		// TODO: This should move to another location eventually -- a bit hacky
		// here (considering other env vars come from other -- a little bit
		// magical -- places).
		// I couldn't easily determine where that magic happened :(
		IEnvironmentVariable original = null;
		IEnvironmentVariable replacement = null;

		original = envManager.getVariable(ERASE_START + "COMPILER.C.FLAGS", confDesc, true); //$NON-NLS-1$
		if (original != null) {
			replacement = new EnvironmentVariable(original.getName(),
					original.getValue().replace(minusG, minusG2).replaceFirst("-O.? ", SPACE), //$NON-NLS-1$
					original.getOperation(), original.getDelimiter());
			contribEnv.addVariable(replacement, confDesc);
		}

		original = envManager.getVariable(ERASE_START + "COMPILER.CPP.FLAGS", confDesc, true); //$NON-NLS-1$
		if (original != null) {
			replacement = new EnvironmentVariable(original.getName(),
					original.getValue().replace(minusG, minusG2).replaceFirst("-O.? ", SPACE), //$NON-NLS-1$
					original.getOperation(), original.getDelimiter());
			contribEnv.addVariable(replacement, confDesc);
		}
	}

	/**
	 * When parsing boards.txt and platform.txt some processing needs to be done
	 * to get "acceptable environment variable values" This method does the
	 * parsing
	 *
	 * @param inputString
	 *            the value string as read from the file
	 * @return the string to be stored as value for the environment variable
	 */
	public static String MakeEnvironmentString(String inputString, String keyPrefix) {
		// String ret = inputString.replaceAll("-o \"\\{object_file}\"",
		// "").replaceAll("\"\\{object_file}\"",
		// "").replaceAll("\"\\{source_file}\"", "")
		// .replaceAll("\\{", "\\${" + ArduinoConst.ENV_KEY_START);
		String ret = inputString.replaceAll("\\{(?!\\{)", "\\${" + keyPrefix); //$NON-NLS-1$ //$NON-NLS-2$
		StringBuilder sb = new StringBuilder(ret);
		String regex = "\\{[^}]*\\}"; //$NON-NLS-1$
		Pattern p = Pattern.compile(regex); // Create the pattern.
		Matcher matcher = p.matcher(sb); // Create the matcher.
		while (matcher.find()) {
			String buf = sb.substring(matcher.start(), matcher.end()).toUpperCase();
			sb.replace(matcher.start(), matcher.end(), buf);
		}
		return sb.toString();
	}

	/**
	 * When parsing boards.txt and platform.txt some processing needs to be done
	 * to get "acceptable environment variable keys" This method does the
	 * parsing some examples on windows "test.windows" becomes "A.TEST"
	 * "test.linux" becomes "A.TEST.LINUX"
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
		String osString = "\\.\\."; //$NON-NLS-1$
		if (Platform.getOS().equals(Platform.OS_LINUX)) {
			osString = "\\.LINUX"; //$NON-NLS-1$
		} else if (Platform.getOS().equals(Platform.OS_WIN32)) {
			osString = "\\.WINDOWS"; //$NON-NLS-1$
		}
		return prefix + string.toUpperCase().replaceAll(osString, EMPTY_STRING);
	}

	/**
	 * Set the project to force a rebuild. This method is called after the
	 * arduino settings have been updated. Note the only way I found I could get
	 * this to work is by deleting the build folder Still then the "indexer
	 * needs to recheck his includes from the language provider which still is
	 * not working
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
						Messages.Helpers_delete_folder_failed + cfgDescription.getName(), e));
			}
		}

		List<ILanguageSettingsProvider> providers;
		if (cfgDescription instanceof ILanguageSettingsProvidersKeeper) {
			providers = new ArrayList<>(
					((ILanguageSettingsProvidersKeeper) cfgDescription).getLanguageSettingProviders());
			for (ILanguageSettingsProvider provider : providers) {
				if ((provider instanceof AbstractBuiltinSpecsDetector)) { // basically
					// check
					// for
					// working
					// copy
					// clear and reset isExecuted flag
					((AbstractBuiltinSpecsDetector) provider).clear();
				}
			}
		}
	}

	/**
	 * Given a source file calculates the base of the output file. this method
	 * may not be needed if I can used the eclipse default behavior. However the
	 * eclipse default behavior is different from the arduino default behavior.
	 * So I keep it for now and we'll see how it goes The eclipse default
	 * behavior is (starting from the project folder [configuration]/Source The
	 * Arduino default behavior is all in 1 location (so no subfolders)
	 *
	 * @param Source
	 *            The source file to find the
	 * @return The base file name for the ouput if Source is "file.cpp" the
	 *         output is "file.cpp"
	 */
	public static IPath GetOutputName(IPath Source) {
		IPath outputName;
		if (Source.toString().startsWith(Const.ARDUINO_CODE_FOLDER_NAME)) {
			outputName = new Path(Const.ARDUINO_CODE_FOLDER_NAME).append(Source.lastSegment());
		} else {
			outputName = Source;
		}
		return outputName;
	}

	/**
	 * Converts a name to a tagged environment variable if variableName ="this"
	 * the output is "${this}"
	 *
	 * @param variableName
	 * @return
	 */
	private static String makeEnvironmentVar(String variableName) {
		return "${" + variableName + '}'; //$NON-NLS-1$
	}

	/**
	 * Give the string entered in the com port try to extract a host. If no host
	 * is found return null yun.local at xxx.yyy.zzz (arduino yun) returns
	 * yun.local
	 *
	 * @param mComPort
	 * @return
	 */
	public static String getHostFromComPort(String mComPort) {
		String host = mComPort.split(Const.SPACE)[0];
		if (host.equals(mComPort))
			return null;
		return host;
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
			Common.log(new Status(IStatus.INFO, Const.CORE_PLUGIN_ID,
					Messages.Helpers_link_folder + source + Messages.Helpers_is_empty, null));
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

	/**
	 * given a action and a tool return the environment key that matches it's
	 * recipe
	 *
	 * @param action
	 * @return he environment variable key to find the recipe
	 */
	private static String get_ENV_KEY_RECIPE(String tool, String action) {
		return ERASE_START + "TOOLS" + DOT + tool.toUpperCase() + DOT + action.toUpperCase() + DOT + ENV_PATTERN; //$NON-NLS-1$
	}

}
