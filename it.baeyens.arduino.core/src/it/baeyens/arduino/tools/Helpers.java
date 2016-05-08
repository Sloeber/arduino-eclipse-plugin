package it.baeyens.arduino.tools;

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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
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
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.common.ConfigurationPreferences;
import it.baeyens.arduino.common.Const;
import it.baeyens.arduino.common.InstancePreferences;
import it.baeyens.arduino.managers.ArduinoPlatform;
import it.baeyens.arduino.managers.Manager;
import it.baeyens.arduino.managers.ToolDependency;

/**
 * ArduinoHelpers is a static class containing general purpose functions
 * 
 * @author Jan Baeyens
 * 
 */
public class Helpers extends Common {

    private static final String BUILD_PATH_SYSCALLS_SAM3 = "\"{build.path}/syscalls_sam3.c.o\""; //$NON-NLS-1$
    private static final String BUILD_PATH_ARDUINO_SYSCALLS_SAM3 = "\"{build.path}/arduino/syscalls_sam3.c.o\""; //$NON-NLS-1$
    private static final String BUILD_PATH_SYSCALLS_MTK = "\"{build.path}/syscalls_mtk.c.o\""; //$NON-NLS-1$
    private static final String BUILD_PATH_ARDUINO_SYSCALLS_MTK = "\"{build.path}/arduino/syscalls_mtk.c.o\""; //$NON-NLS-1$
    private static final String minusG = "-g "; //$NON-NLS-1$
    private static final String minusG2 = "-g2 "; //$NON-NLS-1$

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
	    Common.log(
		    new Status(
			    IStatus.ERROR, Const.CORE_PLUGIN_ID, Messages.Helpers_Could_not_add_folder
				    + IncludePath.toOSString() + Messages.Helpers_To_include_path + project.getName(),
			    e));
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
	newnatures[0] = Const.CNATURE_ID;
	newnatures[1] = Const.CCNATURE_ID;
	newnatures[2] = Const.BUILD_NATURE_ID;
	newnatures[3] = Const.SCANNER_NATURE_ID;
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

	if (!file.exists()) {
	    file.create(contentStream, true, monitor);
	}
    }

    private static void searchFiles(File folder, HashSet<String> Hardwarelists, String Filename, int depth) {
	if (depth > 0) {
	    File[] a = folder.listFiles();
	    if (a == null) {
		Common.log(new Status(IStatus.INFO, Const.CORE_PLUGIN_ID,
			Messages.Helpers_The_folder + folder + Messages.Helpers_is_empty, null));
		return;
	    }
	    for (File f : a) {
		if (f.isDirectory()) {
		    searchFiles(f, Hardwarelists, Filename, depth - 1);
		} else if (f.getName().equals(Filename)) {
		    try {
			Hardwarelists.add(f.getCanonicalPath());
		    } catch (IOException e) {
			// e.printStackTrace();
		    }
		}
	    }
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
	String rootFolder = getBuildEnvironmentVariable(configurationDescription, ENV_KEY_JANTJE_PLATFORM_FILE,
		EMPTY_STRING);
	IPath rootPath = new Path(rootFolder).removeLastSegments(1);
	if (buildCoreFolder.contains(COLON)) {
	    String sections[] = buildCoreFolder.split(COLON);
	    if (sections.length != 2) {
		Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, Messages.Helpers_Value_for_key
			+ ENV_KEY_BUILD_CORE + Messages.Helpers__in_boards_invalid + buildCoreFolder, null));
	    } else {
		String architecture = getBuildEnvironmentVariable(configurationDescription, ENV_KEY_ARCHITECTURE,
			EMPTY_STRING);
		addCodeFolder(project,
			new Path(ARDUINO_HARDWARE_FOLDER_NAME).append(sections[1]).append(architecture)
				.append(ARDUINO_CORE_FOLDER_NAME).append(sections[1]),
			ARDUINO_CODE_FOLDER_NAME + "/core", configurationDescription); //$NON-NLS-1$
	    }
	} else {
	    addCodeFolder(project, rootPath.append("cores").append(buildCoreFolder), ARDUINO_CODE_FOLDER_NAME + "/core", //$NON-NLS-1$ //$NON-NLS-2$
		    configurationDescription);
	    // //$NON-NLS-3$
	}
	if (!boardVariant.isEmpty()) {
	    Helpers.addCodeFolder(project, rootPath.append("variants").append(boardVariant), //$NON-NLS-1$
		    ARDUINO_CODE_FOLDER_NAME + "/variant", //$NON-NLS-1$
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
    private static void RemoveAllArduinoEnvironmentVariables(IContributedEnvironment contribEnv,
	    ICConfigurationDescription confDesc) {

	IEnvironmentVariable[] CurVariables = contribEnv.getVariables(confDesc);
	for (int i = (CurVariables.length - 1); i > 0; i--) {
	    if (CurVariables[i].getName().startsWith(Const.ENV_KEY_BOARD_START)) {
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
	    ICConfigurationDescription confDesc, File platformFile) {
	// Set some default values because the platform.txt does not contain
	// them
	Path platformPath = new Path(platformFile.getAbsolutePath());
	String architecture = platformPath.removeLastSegments(1).lastSegment();
	String packagename = platformPath.removeLastSegments(3).lastSegment();
	int numSegmentsToSubtractForHardwarePath = 2;
	if (architecture.contains(DOT)) { // in case there is a version in the
					  // path ignore the version
	    numSegmentsToSubtractForHardwarePath += 1;
	    architecture = platformPath.removeLastSegments(2).lastSegment();
	    packagename = platformPath.removeLastSegments(4).lastSegment();
	}
	// String buildVariantPath = makeEnvironmentVar(ENV_KEY_PLATFORM_PATH) +
	// "/variants/";

	setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_ARCHITECTURE, architecture.toUpperCase());
	setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_BUILD_ARCH, architecture.toUpperCase());
	setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_HARDWARE_PATH,
		platformPath.removeLastSegments(numSegmentsToSubtractForHardwarePath).toString());
	setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_PLATFORM_PATH,
		platformPath.removeLastSegments(1).toString());
	setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_SERIAL_PORT,
		makeEnvironmentVar(Const.ENV_KEY_JANTJE_COM_PORT));
	if (Platform.getOS().equals(Platform.OS_WIN32)) {
	    setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_MAKE_LOCATION,
		    ConfigurationPreferences.getPathExtensionPath().toOSString() + '/');
	}

	// Teensy uses build.core.path
	// setBuildEnvironmentVariable(contribEnv, confDesc,
	// ENV_KEY_build_core_path, makeEnvironmentVar(ENV_KEY_PLATFORM_PATH) +
	// "/cores/"
	// + makeEnvironmentVar(ENV_KEY_build_core));
	// setBuildEnvironmentVariable(contribEnv, confDesc,
	// ENV_KEY_build_variant_path, buildVariantPath +
	// makeEnvironmentVar(ENV_KEY_build_variant));

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
	setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_PACKAGE_NAME, packagename);

    }

    private static void setTheEnvironmentVariablesAddAFile(IContributedEnvironment contribEnv,
	    ICConfigurationDescription confDesc, File envVarFile) {
	setTheEnvironmentVariablesAddAFile(ENV_KEY_BOARD_START, contribEnv, confDesc, envVarFile);
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
			IEnvironmentVariable envVar = new EnvironmentVariable(MakeKeyString(prefix, var[0]),
				MakeEnvironmentString(value, Const.ENV_KEY_BOARD_START));
			contribEnv.addVariable(envVar, confDesc);
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
	    ICConfigurationDescription confDesc, TxtFile boardsFile, String boardID, boolean warn) {

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
		String valueString = MakeEnvironmentString(currentPair.getValue(), Const.ENV_KEY_BOARD_START);
		contribEnv.addVariable(new EnvironmentVariable(keyString, valueString), confDesc);
	    }
	}
	for (Entry<String, String> currentPair : boardSectionMap.entrySet()) {
	    // if it is not a menu item add it
	    if (currentPair.getKey().startsWith(Messages.Helpers_menu)) {

		String[] keySplit = currentPair.getKey().split("\\."); //$NON-NLS-1$
		String menuID = keySplit[1];
		String menuItemID = keySplit[2];
		if (isThisMenuItemSelected(boardsFile, confDesc, boardID, menuID, menuItemID)) {
		    // we also need to skip the name
		    String StartValue = MENU + DOT + menuID + DOT + menuItemID + DOT; // $NON-NLS-1$
		    if (currentPair.getKey().startsWith(StartValue)) {
			String keyString = MakeKeyString(currentPair.getKey().substring(StartValue.length()));
			String valueString = MakeEnvironmentString(currentPair.getValue(), Const.ENV_KEY_BOARD_START);
			contribEnv.addVariable(new EnvironmentVariable(keyString, valueString), confDesc);
		    }
		}
	    }
	}

	Map<String, String> menuSectionMap = boardsFile.getSection(MENU); // $NON-NLS-1$
	String[] optionNames = boardsFile.getMenuNames();
	for (int currentOption = 0; currentOption < optionNames.length; currentOption++) {
	    String optionName = optionNames[currentOption];
	    String optionValue = getBuildEnvironmentVariable(confDesc, Const.ENV_KEY_JANTJE_START + optionName,
		    EMPTY_STRING);
	    if (!optionValue.isEmpty()) {
		String optionValueID = null;
		String optionID = null;
		// Look for the option ID
		for (Entry<String, String> curOption : menuSectionMap.entrySet()) {
		    if (curOption.getValue().equals(optionName)) {
			String[] keySplit = curOption.getKey().split("\\."); //$NON-NLS-1$
			if (keySplit.length == 1)
			    optionID = keySplit[0];
		    }
		}
		if (optionID != null) { // we have the option ID lets look for
					// the option value ID
		    for (Entry<String, String> curOption : menuSectionMap.entrySet()) {
			if (curOption.getValue().equals(optionValue)) {
			    String[] keySplit = curOption.getKey().split("\\."); //$NON-NLS-1$
			    if (keySplit.length == 3 && keySplit[0].equals(optionID) && keySplit[1].equals(boardID))
				optionValueID = keySplit[2];
			}
		    }
		}
		if (optionValueID != null) // now we have all the info to find
					   // the key value pairs for the
					   // environment vars
		{
		    // The arduino menu way
		    String keyStartsWithValue = optionID + DOT + boardID + DOT + optionValueID + DOT;
		    for (Entry<String, String> curOption : menuSectionMap.entrySet()) {
			if (curOption.getKey().startsWith(keyStartsWithValue)) {
			    String key = curOption.getKey().substring(keyStartsWithValue.length());
			    contribEnv.addVariable(
				    new EnvironmentVariable(MakeKeyString(key),
					    MakeEnvironmentString(curOption.getValue(), Const.ENV_KEY_BOARD_START)),
				    confDesc);
			}
		    }

		}
	    }

	}
    }

    private static boolean isThisMenuItemSelected(TxtFile boardsFile, ICConfigurationDescription confDesc,
	    String boardID, String menuID, String menuItemID) {

	String MenuName = boardsFile.getMenuNameFromID(menuID);
	String MenuItemName = boardsFile.getMenuItemNameFromID(boardID, menuID, menuItemID);

	String SelectedMenuItemName = getBuildEnvironmentVariable(confDesc, Const.ENV_KEY_JANTJE_START + MenuName,
		EMPTY_STRING);
	if (SelectedMenuItemName.isEmpty()) {
	    return false; // This menu item has not been selected
	    // this should not happen
	}
	if (MenuItemName.equalsIgnoreCase(SelectedMenuItemName))
	    return true;
	return false;
    }

    private static void setTheEnvironmentVariablesAddThePlatformInfo(IContributedEnvironment contribEnv,
	    ICConfigurationDescription confDesc) {
	String platformFileName = getBuildEnvironmentVariable(confDesc, Const.ENV_KEY_JANTJE_PLATFORM_FILE,
		Const.EMPTY_STRING);

	ArduinoPlatform platform = Manager.getPlatform(platformFileName);
	if (platform == null) {
	    return;
	}
	for (ToolDependency tool : platform.getToolsDependencies()) {
	    String keyString = MakeKeyString("runtime.tools." + tool.getName() + ".path"); //$NON-NLS-1$ //$NON-NLS-2$
	    String valueString = tool.getTool().getInstallPath().toString();
	    contribEnv.addVariable(new EnvironmentVariable(keyString, valueString), confDesc);
	    keyString = MakeKeyString("runtime.tools." + tool.getName() + tool.getVersion() + ".path"); //$NON-NLS-1$ //$NON-NLS-2$
	    contribEnv.addVariable(new EnvironmentVariable(keyString, valueString), confDesc);
	    keyString = MakeKeyString("runtime.tools." + tool.getName() + '-' + tool.getVersion() + ".path"); //$NON-NLS-1$ //$NON-NLS-2$
	    contribEnv.addVariable(new EnvironmentVariable(keyString, valueString), confDesc); // writer.println("runtime.tools."
											       // +
											       // tool.getName()
											       // +
											       // ".path="
											       // +
	    // tool.getTool().getInstallPath());//$NON-NLS-1$ //$NON-NLS-2$
	    // writer.println("runtime.tools." + tool.getName() +
	    // tool.getVersion() + ".path=" //$NON-NLS-1$ //$NON-NLS-2$
	    // + tool.getTool().getInstallPath());
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
	    boolean debugCompilerSettings) {

	// first get all the data we need
	IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	IContributedEnvironment contribEnv = envManager.getContributedEnvironment();

	File boardFileName = new Path(Common.getBuildEnvironmentVariable(confDesc, Const.ENV_KEY_JANTJE_BOARDS_FILE,
		InstancePreferences.getLastUsedBoardsFile())).toFile();
	File localPlatformFilename = new Path(
		Common.getBuildEnvironmentVariable(confDesc, Const.ENV_KEY_JANTJE_PLATFORM_FILE, EMPTY_STRING))
			.toFile();
	Programmers localProgrammers = Programmers.fromBoards(boardFileName);

	String boardID = Common.getBuildEnvironmentVariable(confDesc, Const.ENV_KEY_JANTJE_BOARD_ID, EMPTY_STRING);
	String architecture = Common.getBuildEnvironmentVariable(confDesc, Const.ENV_KEY_JANTJE_ARCITECTURE_ID,
		EMPTY_STRING);

	architecture = architecture.toUpperCase();

	TxtFile pluginPreProcessingBoardsTxt = new TxtFile(ConfigurationPreferences.getPreProcessingBoardsFile());
	TxtFile pluginPostProcessingBoardsTxt = new TxtFile(ConfigurationPreferences.getPostProcessingBoardsFile());
	File pluginPreProcessingPlatformTxt = ConfigurationPreferences.getPreProcessingPlatformFile();
	File pluginPostProcessingPlatformTxt = ConfigurationPreferences.getPostProcessingPlatformFile();
	TxtFile boardsFile = new TxtFile(boardFileName);

	// Now we have all info we can start processing

	// first remove all Arduino Variables so there is no memory effect
	RemoveAllArduinoEnvironmentVariables(contribEnv, confDesc);

	setTheEnvironmentVariablesSetTheDefaults(contribEnv, confDesc, localPlatformFilename);

	// add the stuff that comes with the plugin that are marked as pre
	setTheEnvironmentVariablesAddAFile(contribEnv, confDesc, pluginPreProcessingPlatformTxt);
	setTheEnvironmentVariablesAddtheBoardsTxt(contribEnv, confDesc, pluginPreProcessingBoardsTxt, boardID, false);

	// // Then add the programmers file
	// setTheEnvironmentVariablesAddAFile(ENV_KEY_PROGRAMMERS_START,
	// contribEnv, confDesc, localProgrammers.getTxtFile());

	// Do some magic for the arduino:arduino stuff
	setTheEnvironmentVariablesRedirectToOtherVendors(contribEnv, confDesc, boardsFile, boardID,
		architecture.toLowerCase());// TOFIX again some
					    // dirty thing

	// process the platform file that is referenced in the build.core of the
	// boards.txt file
	File referencedPlatformFilename = new File(Common.getBuildEnvironmentVariable(confDesc,
		Const.ENV_KEY_JANTJE_REFERENCED_PLATFORM_FILE, EMPTY_STRING));
	if (referencedPlatformFilename.exists()) {
	    setTheEnvironmentVariablesAddAFile(contribEnv, confDesc, referencedPlatformFilename);
	}

	// process the platform file next to the selected boards.txt
	if (localPlatformFilename.exists()) {
	    setTheEnvironmentVariablesAddAFile(contribEnv, confDesc, localPlatformFilename);
	}

	setTheEnvironmentVariablesAddThePlatformInfo(contribEnv, confDesc);

	// add the boards file
	setTheEnvironmentVariablesAddtheBoardsTxt(contribEnv, confDesc, boardsFile, boardID, true);

	// Then add the programmers file
	String programmer = contribEnv.getVariable(get_Jantje_KEY_PROTOCOL(ACTION_UPLOAD), confDesc).getValue();
	setTheEnvironmentVariablesAddtheBoardsTxt(contribEnv, confDesc, localProgrammers,
		localProgrammers.getIDFromName(programmer), false);

	// add the stuff that comes with the plugin that is marked as post
	setTheEnvironmentVariablesAddAFile(contribEnv, confDesc, pluginPostProcessingPlatformTxt);
	setTheEnvironmentVariablesAddtheBoardsTxt(contribEnv, confDesc, pluginPostProcessingBoardsTxt, boardID, false);

	// Do some coded post processing
	setTheEnvironmentVariablesPostProcessing(contribEnv, confDesc);

	// If this is a debug config we modify the environment variables for
	// compilation
	if (debugCompilerSettings) {
	    setTheEnvironmentVariablesModifyDebugCompilerSettings(confDesc, envManager, contribEnv);
	}

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
	    ICConfigurationDescription confDesc, TxtFile boardsFile, String boardID, String architecture) {
	Map<String, String> boardInfo = boardsFile.getSection(boardID);
	if (boardInfo == null) {
	    return; // there is a problem with the board ID
	}
	String core = boardInfo.get("build.core"); //$NON-NLS-1$
	String variant = boardInfo.get("build.variant"); //$NON-NLS-1$
	if (core != null) {
	    String coreSplit[] = core.split(COLON);
	    if (coreSplit.length == 2) {
		String vendor = coreSplit[0];
		Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_BUILD_CORE, coreSplit[1]);
		IPath coreReference = findReferencedFolder(vendor, architecture.toLowerCase());// TODO
											       // fix
											       // this
											       // quickfix
											       // to
											       // lower
											       // which
											       // is
											       // really
											       // dirty
		if (coreReference == null) {
		    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
			    Messages.Helpers_Core_refference_missing + core));
		} else {
		    setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_BUILD_CORE_PATH,
			    coreReference.append(ARDUINO_CORE_FOLDER_NAME).append(coreSplit[1]).toString());
		    setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_REFERENCED_PLATFORM_FILE,
			    coreReference.append(PLATFORM_FILE_NAME).toString());
		}
	    } else {
		setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_BUILD_CORE, core);
		setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_REFERENCED_PLATFORM_FILE,
			EMPTY_STRING);
	    }
	}
	if (variant != null) {
	    String variantSplit[] = variant.split(COLON);
	    if (variantSplit.length == 2) {
		String vendor = variantSplit[0];
		Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_BUILD_VARIANT, variantSplit[1]);
		IPath variantReference = findReferencedFolder(vendor, architecture);
		if (variantReference == null) {
		    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
			    Messages.Helpers_Variant_reference_missing + variant));
		} else {
		    Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_BUILD_VARIANT_PATH,
			    variantReference.append(VARIANTS_FOLDER_NAME).append(variantSplit[1]).toString());
		}
	    } else {
		Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_BUILD_VARIANT, variant);
	    }
	}
    }

    /**
     * This method looks for a referenced path. The search goes as follows it
     * only looks in manager installed stuff
     * 
     * @param vendor
     * @param architecture
     * @return
     */
    private static IPath findReferencedFolder(String vendor, String architecture) {

	// TODO look in all hardware paths
	Path privateHardwareFolder = new Path(getPrivateHardwarePaths()[0]);
	IPath boardsManagerPackagesFolder = ConfigurationPreferences.getInstallationPath();
	if (privateHardwareFolder.append(vendor).append(architecture).toFile().exists()) {
	    return privateHardwareFolder.append(vendor).append(architecture);
	}
	if (boardsManagerPackagesFolder.append(vendor).append(ARDUINO_HARDWARE_FOLDER_NAME).append(architecture)
		.toFile().exists()) {
	    // need to add version
	    IPath foundPath = boardsManagerPackagesFolder.append(vendor).append(ARDUINO_HARDWARE_FOLDER_NAME)
		    .append(architecture);
	    String[] versions = foundPath.toFile().list();
	    switch (versions.length) {
	    case 0:
		break;
	    case 1:
		return foundPath.append(versions[0]);
	    default:
		Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
			"Multiple versions found in: " + foundPath.toString() + " taking " + versions[0])); //$NON-NLS-1$ //$NON-NLS-2$
		return foundPath.append(versions[0]);
	    }
	}

	return null;
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
		    "(\"\\$\\{A.OBJECT_FILE}\")|(\\$\\{A.OBJECT_FILES})|(\"\\$\\{A.SOURCE_FILE}\")|(\"[^\"]*\\$\\{A.ARCHIVE_FILE}\")", //$NON-NLS-1$
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
	if (programmer.equalsIgnoreCase(Const.DEFAULT)) {
	    String uploadTool = contribEnv.getVariable(get_ENV_KEY_TOOL(ACTION_UPLOAD), confDesc).getValue();
	    String MComPort = contribEnv.getVariable(Const.ENV_KEY_JANTJE_COM_PORT, confDesc).getValue();
	    String host = getHostFromComPort(MComPort);
	    if (host != null) {
		String platform = contribEnv.getVariable(Const.ENV_KEY_JANTJE_ARCITECTURE_ID, confDesc).getValue();
		setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_NETWORK_PORT, NetworkDiscovery.getPort(host));
		setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_NETWORK_AUTH,
			NetworkDiscovery.hasAuth(host) ? TRUE : FALSE);
		setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_SERIAL_PORT, host);

		try {
		    String key = ENV_KEY_BOARD_START + platform.toUpperCase() + DOT + NETWORK + DOT
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
	} else {
	    String uploadTool = contribEnv.getVariable("A.PROGRAM.TOOL", confDesc).getValue(); //$NON-NLS-1$
	    setBuildEnvironmentVariable(contribEnv, confDesc, get_Jantje_KEY_RECIPE(ACTION_UPLOAD),
		    makeEnvironmentVar(get_ENV_KEY_RECIPE(uploadTool, ACTION_PROGRAM)));
	    setBuildEnvironmentVariable(contribEnv, confDesc, get_ENV_KEY_TOOL(ACTION_PROGRAM), uploadTool);
	}

	// Arduino uses the board approach for the tools.
	// as I'm not, therefore I mod the tools in the command to be FQN
	try {
	    IEnvironmentVariable[] curVariables = contribEnv.getVariables(confDesc);
	    for (IEnvironmentVariable curVariable : curVariables) {
		String name = curVariable.getName();
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
	    }

	} catch (Exception e) {
	    Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, "parsing of upload recipe failed", e)); //$NON-NLS-1$
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

	original = envManager.getVariable(ENV_KEY_BOARD_START + "COMPILER.C.FLAGS", confDesc, true); //$NON-NLS-1$
	if (original != null) {
	    replacement = new EnvironmentVariable(original.getName(),
		    original.getValue().replace(minusG, minusG2).replaceFirst("-O.? ", SPACE), //$NON-NLS-1$
		    original.getOperation(), original.getDelimiter());
	    contribEnv.addVariable(replacement, confDesc);
	}

	original = envManager.getVariable(ENV_KEY_BOARD_START + "COMPILER.CPP.FLAGS", confDesc, true); //$NON-NLS-1$
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

	return MakeKeyString(ENV_KEY_BOARD_START, string);
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
     * Searches for all boards.txt files from the arduino hardware folder
     * 
     * @return all the boards.txt files with full path
     */
    public static String[] getBoardsFiles() {
	String hardwareFolders[] = getHardwarePaths();

	HashSet<String> boardFiles = new HashSet<>();
	for (String CurFolder : hardwareFolders) {
	    searchFiles(new File(CurFolder), boardFiles, Const.BOARDS_FILE_NAME, 6);
	}
	if (boardFiles.size() == 0) {
	    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
		    Messages.Helpers_No_boards_txt_found + String.join("\n", hardwareFolders), null)); //$NON-NLS-1$
	    return null;
	}
	return boardFiles.toArray(new String[boardFiles.size()]);

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
}
