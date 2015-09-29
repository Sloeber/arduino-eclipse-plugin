package it.baeyens.arduino.tools;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;

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
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
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

/**
 * ArduinoHelpers is a static class containing general purpose functions
 * 
 * @author Jan Baeyens
 * 
 */
public class ArduinoHelpers extends Common {

    private static final String BUILD_PATH_SYSCALLS_SAM3 = "\"{build.path}/syscalls_sam3.c.o\"";
    private static final String BUILD_PATH_ARDUINO_SYSCALLS_SAM3 = "\"{build.path}/arduino/syscalls_sam3.c.o\"";
    private static final String BUILD_PATH_SYSCALLS_MTK = "\"{build.path}/syscalls_mtk.c.o\"";
    private static final String BUILD_PATH_ARDUINO_SYSCALLS_MTK = "\"{build.path}/arduino/syscalls_mtk.c.o\"";

    /**
     * This method is the internal working class that adds the provided includepath to all configurations and languages.
     * 
     * @param configurationDescription
     *            The configuration description of the project to add it to
     * @param IncludePath
     *            The path to add to the include folders
     * @see addLibraryDependency {@link #addLibraryDependency(IProject, IProject)}
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
		    IncludeEntries[OrgIncludeEntries.length] = new CIncludePathEntry(IncludePath, ICSettingEntry.VALUE_WORKSPACE_PATH); // (location.toString());

		    lang.setSettingEntries(ICSettingEntry.INCLUDE_PATH, IncludeEntries);

		}
	    }
	}
    }

    /**
     * Removes include folders that are not valid. This method does not save the configurationDescription description
     * 
     * @param configurationDescription
     *            the configuration that is checked
     * @return true is a include path has been removed. False if the include path remains unchanged.
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
		    ICLanguageSettingEntry[] OrgIncludeEntriesFull = lang.getResolvedSettingEntries(ICSettingEntry.INCLUDE_PATH);
		    int copiedEntry = 0;
		    for (int curEntry = 0; curEntry < OrgIncludeEntries.length; curEntry++) {
			IPath cusPath = ((CIncludePathEntry) OrgIncludeEntriesFull[curEntry]).getFullPath();
			if ((ResourcesPlugin.getWorkspace().getRoot().exists(cusPath))
				|| (((CIncludePathEntry) OrgIncludeEntries[curEntry]).isBuiltIn())) {
			    OrgIncludeEntries[copiedEntry++] = OrgIncludeEntries[curEntry];
			} else {
			    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Removed invalid include path" + cusPath, null));
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
     * This method adds the provided path to the include path of all configurations and languages.
     * 
     * @param project
     *            The project to add it to
     * @param IncludePath
     *            The path to add to the include folders
     * @see addLibraryDependency {@link #addLibraryDependency(IProject, IProject)}
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
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Could not add folder " + IncludePath.toOSString()
		    + " to includepoth in project" + project.getName(), e));
	}

    }

    public static void addCodeFolder(IProject project, String PathVarName, String SubFolder, String LinkName,
	    ICConfigurationDescription configurationDescriptions[]) throws CoreException {
	for (ICConfigurationDescription curConfig : configurationDescriptions) {
	    ArduinoHelpers.addCodeFolder(project, PathVarName, SubFolder, LinkName, curConfig);
	}

    }

    /**
     * Creates a folder and links the folder to an existing folder Parent folders of the target folder are created if needed. In case this method
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
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Could not create folder " + target, e));
	}
    }

    /**
     * This method creates a link folder in the project and add the folder as a source path to the project it also adds the path to the include folder
     * if the includepath parameter points to a path that contains a subfolder named "utility" this subfolder will be added to the include path as
     * well <br/>
     * Forget about this. Arduino made this all so complicated I don't know anymore what needs to be added to what<br/>
     * <br/>
     * 
     * note Arduino has these subfolders in the libraries that need to be include.<br/>
     * <br/>
     * 
     * note that in the current eclipse version, there is no need to add the subfolder as a code folder. This may change in the future as it looks
     * like a bug to me.<br/>
     * 
     * @param project
     * @param Path
     * @throws CoreException
     * 
     * @see addLibraryDependency {@link #addLibraryDependency(IProject, IProject)}
     */
    public static void addCodeFolder(IProject project, String PathVarName, String SubFolder, String LinkName,
	    ICConfigurationDescription configurationDescription) throws CoreException {
	IFolder link = project.getFolder(LinkName);

	LinkFolderToFolder(project, new Path(PathVarName).append(SubFolder), new Path(LinkName));

	// Now the folder has been created we need to make sure the special folders are added to the path
	addIncludeFolder(configurationDescription, link.getFullPath());

	IPathVariableManager pathMan = project.getPathVariableManager();

	String possibleIncludeFolder = "utility";
	File file = new File(new Path(pathMan.resolveURI(pathMan.getURIValue(PathVarName)).getPath()).append(SubFolder).append(possibleIncludeFolder)
		.toString());
	if (file.exists()) {
	    addIncludeFolder(configurationDescription, link.getFullPath().append(possibleIncludeFolder));
	}

	possibleIncludeFolder = "src";
	file = new File(new Path(pathMan.resolveURI(pathMan.getURIValue(PathVarName)).getPath()).append(SubFolder).append(possibleIncludeFolder)
		.toString());
	if (file.exists()) {
	    addIncludeFolder(configurationDescription, link.getFullPath().append(possibleIncludeFolder));
	}

	possibleIncludeFolder = "arch";
	file = new File(new Path(pathMan.resolveURI(pathMan.getURIValue(PathVarName)).getPath()).append(SubFolder).append(possibleIncludeFolder)
		.toString());
	if (file.exists()) {
	    addIncludeFolder(configurationDescription,
		    link.getFullPath().append(possibleIncludeFolder).append(makeEnvironmentVar(ENV_KEY_ARCHITECTURE)));
	}
    }

    /**
     * This method creates a link folder in the project and adds the folder as a source path to the project it also adds the path to the include
     * folder if the includepath parameter points to a path that contains a subfolder named "utility" this subfolder will be added to the include path
     * as well <br/>
     * <br/>
     * 
     * note Arduino has these subfolders in the libraries that need to be include.<br/>
     * <br/>
     * 
     * note that in the current eclipse version, there is no need to add the subfolder as a code folder. This may change in the future as it looks
     * like a bug to me.<br/>
     * 
     * @param project
     * @param Path
     * @throws CoreException
     * 
     * @see addLibraryDependency {@link #addLibraryDependency(IProject, IProject)}
     */
    public static void addCodeFolder(IProject project, IPath Path, ICConfigurationDescription configurationDescription) throws CoreException {

	// create a link to the path
	String NiceName = Path.lastSegment();
	String PathName = project.getName() + NiceName;
	URI ShortPath = URIUtil.toURI(Path.removeTrailingSeparator().removeLastSegments(1));

	IWorkspace workspace = project.getWorkspace();
	IPathVariableManager pathMan = workspace.getPathVariableManager();

	pathMan.setURIValue(PathName, ShortPath);

	addCodeFolder(project, PathName, NiceName, NiceName, configurationDescription);

    }

    /**
     * addTheNatures replaces all existing natures by the natures needed for a arduino project
     * 
     * @param project
     *            The project where the natures need to be added to
     * @throws CoreException
     */
    public static void addTheNatures(IProjectDescription description) throws CoreException {
	String[] newnatures = new String[5];
	newnatures[0] = ArduinoConst.Cnatureid;
	newnatures[1] = ArduinoConst.CCnatureid;
	newnatures[2] = ArduinoConst.Buildnatureid;
	newnatures[3] = ArduinoConst.Scannernatureid;
	newnatures[4] = ArduinoConst.ArduinoNatureID;
	description.setNatureIds(newnatures);

    }

    /**
     * This method adds the content of a content stream to a file
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
    public static void addFileToProject(IContainer container, Path path, InputStream contentStream, IProgressMonitor monitor) throws CoreException {
	final IFile file = container.getFile(path);
	if (file.exists()) {
	    file.setContents(contentStream, true, true, monitor);
	} else {
	    file.create(contentStream, true, monitor);
	}

    }

    /**
     * This method sets the eclipse path variables to contain the 3 important Arduino hardware folders (code wise that is)
     * 
     * Core path (used when referencing Arduino Code) The Arduino Pin Path (used from Arduino 1.0 to reference the arduino pin variants) The libraries
     * path (used to find libraries)
     * 
     * Paths are given relative to the arduino folder to avoid conflict when a version control system is being used (these values are in the .project
     * file) As the arduino folder location is in the workspace all values in the .project file become relative avoiding conflict.
     * 
     * If core or variant are of the type [vendor ID]:[core ID] then we reroute
     * 
     * @param project
     */
    public static void setProjectPathVariables(ICConfigurationDescription configurationDescription) {
	IPath variantPath = new Path(Common.getBuildEnvironmentVariable(configurationDescription, ArduinoConst.ENV_KEY_build_variant_path, ""));
	IPath corePath = new Path(Common.getBuildEnvironmentVariable(configurationDescription, ArduinoConst.ENV_KEY_build_core_path, ""));
	IPath platformPath = new Path(Common.getBuildEnvironmentVariable(configurationDescription, ArduinoConst.ENV_KEY_PLATFORM_PATH, ""));

	IPath arduinoHardwareLibraryPath = platformPath.append(ArduinoConst.LIBRARY_PATH_SUFFIX);
	IPathVariableManager pathMan = configurationDescription.getProjectDescription().getProject().getPathVariableManager();
	try {
	    pathMan.setURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_HARDWARE_LIB, URIUtil.toURI(arduinoHardwareLibraryPath));
	    pathMan.setURIValue(ArduinoConst.PATH_VARIABLE_NAME_ARDUINO_PLATFORM, URIUtil.toURI(corePath.removeLastSegments(1)));
	    pathMan.setURIValue(ArduinoConst.PATH_VARIABLE_NAME_ARDUINO_PINS, URIUtil.toURI(variantPath.removeLastSegments(1)));
	    // pathMan.setURIValue("ArduinoPivateLibPath", URIUtil.toURI("${" + ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_PRIVATE_LIB + "}"));

	} catch (CoreException e) {
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
		    "Failed to create the path variable variables. The setup will not work properly", e));
	    e.printStackTrace();
	}
    }

    private static void searchFiles(File folder, HashSet<String> Hardwarelists, String Filename, int depth) {
	if (depth > 0) {
	    File[] a = folder.listFiles();
	    if (a == null) {
		Common.log(new Status(IStatus.INFO, ArduinoConst.CORE_PLUGIN_ID, "The folder " + folder + " does not contain any files.", null));
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
     * This method adds the Arduino code in a subfolder named Arduino. 2 linked subfolders named core and variant link to the real Arduino code note
     * if your arduino ide version is from before 1.0 only 1 folder is created
     * 
     * @param project
     *            The project to add the arduino code to
     * @param ProjectProperties
     *            The properties to use to add the core folder
     * @throws CoreException
     */
    public static void addArduinoCodeToProject(IProject project, ICConfigurationDescription configurationDescription) throws CoreException {

	String boardVariant = getBuildEnvironmentVariable(configurationDescription, ENV_KEY_build_variant, "");
	String buildCoreFolder = getBuildEnvironmentVariable(configurationDescription, ENV_KEY_build_core, "");
	if (buildCoreFolder.contains(":")) {
	    String sections[] = buildCoreFolder.split(":");
	    if (sections.length != 2) {
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "the value for key " + ENV_KEY_build_core
			+ " in boards.txt is invalid:" + buildCoreFolder, null));
	    } else {
		String architecture = getBuildEnvironmentVariable(configurationDescription, ENV_KEY_ARCHITECTURE, "");
		addCodeFolder(project, WORKSPACE_PATH_VARIABLE_NAME_ARDUINO, ARDUINO_HARDWARE_FOLDER_NAME + "/" + sections[1] + "/" + architecture
			+ "/" + ARDUINO_CORE_FOLDER_NAME + "/" + sections[1], "arduino/core", configurationDescription);
	    }
	} else {
	    addCodeFolder(project, PATH_VARIABLE_NAME_ARDUINO_PLATFORM, buildCoreFolder, "arduino/core", configurationDescription);
	}
	if (!boardVariant.equals("")) {
	    ArduinoHelpers.addCodeFolder(project, PATH_VARIABLE_NAME_ARDUINO_PINS, boardVariant, "arduino/variant", configurationDescription);
	} else {// this is Arduino version 1.0
	    IFolder variantFolder = project.getFolder("arduino/variant");
	    if (variantFolder.exists()) {
		try {
		    variantFolder.delete(true, null);
		} catch (CoreException e) {
		    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "failed to delete the variant folder", e));
		}
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
     *            if null a local folder is created using newFolderName if not null a link folder is created with the name newFolderName and pointing
     *            to linklocation
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
    private static void RemoveAllArduinoEnvironmentVariables(IContributedEnvironment contribEnv, ICConfigurationDescription confDesc) {

	IEnvironmentVariable[] CurVariables = contribEnv.getVariables(confDesc);
	for (int i = (CurVariables.length - 1); i > 0; i--) {
	    if (CurVariables[i].getName().startsWith(ArduinoConst.ENV_KEY_ARDUINO_START)) {
		contribEnv.removeVariable(CurVariables[i].getName(), confDesc);
	    }
	}
    }

    /**
     * Sets the default values. Basically some settings are not set in the platform.txt file. Here I set these values. This method should be called as
     * first. This way the values in platform.txt and boards.txt will take precedence of the default values declared here
     * 
     * @param contribEnv
     * @param confDesc
     * @param platformFile
     *            Used to define the hardware as different settings are needed for avr and sam
     */
    private static void setTheEnvironmentVariablesSetTheDefaults(IContributedEnvironment contribEnv, ICConfigurationDescription confDesc,
	    IPath platformFile) {
	// Set some default values because the platform.txt does not contain
	// them
	setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_ARDUINO_PATH, getArduinoPath().toString());

	String architecture = platformFile.removeLastSegments(1).lastSegment();
	if (architecture.contains(".")) {
	    architecture = platformFile.removeLastSegments(2).lastSegment();
	}
	String buildVariantPath = makeEnvironmentVar(ENV_KEY_PLATFORM_PATH) + "/variants/";

	setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_ARCHITECTURE, architecture.toUpperCase());
	setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_BUILD_ARCH, architecture.toUpperCase());
	// from 1.6.2 the hardware path can also contain a version number
	// TOFIX test with boardmanager and without board manager
	setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_HARDWARE_PATH, platformFile.removeLastSegments(3).toString());

	// from 1.5.8 onward 1 more environment variable is needed
	setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_PLATFORM_PATH, platformFile.removeLastSegments(1).toString());
	// Teensy uses build.core.path
	setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_build_core_path, makeEnvironmentVar(ENV_KEY_PLATFORM_PATH) + "/cores/"
		+ makeEnvironmentVar(ENV_KEY_build_core));
	setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_build_variant_path, buildVariantPath + makeEnvironmentVar(ENV_KEY_build_variant));

	setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_SOFTWARE, "ARDUINO");
	setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_runtime_ide_version, GetArduinoDefineValue());
	// for the due from arduino IDE 1.6.1 onwards link the due bin builder to the hex binder
	setBuildEnvironmentVariable(contribEnv, confDesc, "A.RECIPE.OBJCOPY.HEX.PATTERN", "${A.RECIPE.OBJCOPY.BIN.PATTERN}");

	// For Teensy I added a flag that allows to compile everything in one
	// project not using the archiving functionality
	// I set the default value to: use the archiver
	setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_use_archiver, "true");

	// Build Time
	Date d = new Date();
	GregorianCalendar cal = new GregorianCalendar();
	long current = d.getTime() / 1000;
	long timezone = cal.get(Calendar.ZONE_OFFSET) / 1000;
	long daylight = cal.get(Calendar.DST_OFFSET) / 1000;
	// p.put("extra.time.utc", Long.toString(current));
	setBuildEnvironmentVariable(contribEnv, confDesc, "A.EXTRA.TIME.UTC", Long.toString(current));
	setBuildEnvironmentVariable(contribEnv, confDesc, "A.EXTRA.TIME.LOCAL", Long.toString(current + timezone + daylight));
	setBuildEnvironmentVariable(contribEnv, confDesc, "A.EXTRA.TIME.ZONE", Long.toString(timezone));
	setBuildEnvironmentVariable(contribEnv, confDesc, "A.EXTRA.TIME.DTS", Long.toString(daylight));
	// End of Teensy specific settings

	if (architecture.equals("avr")) {
	    setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_compiler_path, makeEnvironmentVar(ENV_KEY_HARDWARE_PATH) + "/tools/avr/bin/");
	} else if (architecture.equals("sam") || architecture.equals("mtk")) {
	    setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_build_system_path, makeEnvironmentVar(ENV_KEY_PLATFORM_PATH) + "/system");
	    setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_build_generic_path, makeEnvironmentVar(ENV_KEY_HARDWARE_PATH)
		    + "/tools/g++_arm_none_eabi/arm-none-eabi/bin");
	}

	// some glue to make it work
	String extraPathForOS = "";
	if (Platform.getWS().equals(Platform.WS_WIN32)) {
	    extraPathForOS = makeEnvironmentVar("PathDelimiter") + makeEnvironmentVar(ENV_KEY_ARDUINO_PATH) + "/hardware/tools/avr/utils/bin"
		    + makeEnvironmentVar("PathDelimiter") + makeEnvironmentVar(ENV_KEY_ARDUINO_PATH);
	}
	setBuildEnvironmentVariable(contribEnv, confDesc, "PATH", makeEnvironmentVar(ENV_KEY_compiler_path) + makeEnvironmentVar("PathDelimiter")
		+ makeEnvironmentVar(ENV_KEY_build_generic_path) + extraPathForOS + makeEnvironmentVar("PathDelimiter") + makeEnvironmentVar("PATH"));

	setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_build_path, makeEnvironmentVar("ProjDirPath") + "/"
		+ makeEnvironmentVar("ConfigName"));

	setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_build_project_name, makeEnvironmentVar("ProjName"));

	// if (firstTime) {
	if (getBuildEnvironmentVariable(confDesc, ENV_KEY_JANTJE_SIZE_SWITCH, "").isEmpty()) {
	    setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_SIZE_SWITCH, makeEnvironmentVar(ENV_KEY_recipe_size_pattern));
	}
	if (getBuildEnvironmentVariable(confDesc, ENV_KEY_JANTJE_SIZE_COMMAND, "").isEmpty()) {
	    setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_SIZE_COMMAND, JANTJE_SIZE_COMMAND);
	}

	// Set the warning level default off like arduino does
	if (getBuildEnvironmentVariable(confDesc, ENV_KEY_JANTJE_WARNING_LEVEL, "").isEmpty()) {
	    setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_WARNING_LEVEL, ENV_KEY_WARNING_LEVEL_OFF);
	}
	setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_archive_file, "arduino.ar");

    }

    /**
     * This method parses a file with environment variables like the platform.txt file for values to be added to the environment variables
     * 
     * @param contribEnv
     * @param confDesc
     * @param envVarFile
     *            The file to parse
     */
    private static void setTheEnvironmentVariablesAddAFile(IContributedEnvironment contribEnv, ICConfigurationDescription confDesc, IPath envVarFile) {
	try (DataInputStream dataInputStream = new DataInputStream(new FileInputStream(envVarFile.toOSString()));
		BufferedReader br = new BufferedReader(new InputStreamReader(dataInputStream));) {
	    String strLine;

	    // Read File Line By Line
	    while ((strLine = br.readLine()) != null) {
		String realData[] = strLine.split("#");// Ignore everything after
						       // first #
		if (realData.length > 0) {
		    String var[] = realData[0].split("=", 2); // look for assignment
		    if (var.length == 2) {
			String value = var[1];
			if (value.contains(BUILD_PATH_SYSCALLS_SAM3)) {
			    value = value.replace(BUILD_PATH_SYSCALLS_SAM3, BUILD_PATH_ARDUINO_SYSCALLS_SAM3);
			} else if (value.contains(BUILD_PATH_SYSCALLS_MTK)) {
			    value = value.replace(BUILD_PATH_SYSCALLS_MTK, BUILD_PATH_ARDUINO_SYSCALLS_MTK);
			}
			IEnvironmentVariable envVar = new EnvironmentVariable(MakeKeyString(var[0]), MakeEnvironmentString(value,
				ArduinoConst.ENV_KEY_ARDUINO_START));
			contribEnv.addVariable(envVar, confDesc);
		    }
		}
	    }
	} catch (FileNotFoundException e) {
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Error parsing " + envVarFile.toString() + " file does not exist. ", e));
	} catch (IOException e) {
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Error parsing " + envVarFile.toString() + " I/O exception. ", e));
	}
    }

    /**
     * This method parses the boards.txt file for values to be added to the environment variables First it adds all the variables based on the board
     * name [boardID].[key]=[value] results in [key]=[value] (taking in account the modifiers) Then it parses for the menu variables
     * menu.[menuID].[boardID].[selectionID].[key]=[value] results in [key]=[value] (taking in account the modifiers)
     * 
     * @param contribEnv
     * @param confDesc
     * @param platformFilename
     *            The file to parse
     */
    private static void setTheEnvironmentVariablesAddtheBoardsTxt(IContributedEnvironment contribEnv, ICConfigurationDescription confDesc,
	    ArduinoBoards boardsFile, String boardID, boolean warn) {

	// Get the boards section and add all entries to the environment variables
	Map<String, String> boardSectionMap = boardsFile.getSection(boardID);
	if (boardSectionMap == null) {
	    if (warn) {
		Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "The project "
			+ confDesc.getProjectDescription().getProject().getName() + " Has an invalid arduino board configuration."));

	    }
	    return;
	}
	for (Entry<String, String> currentPair : boardSectionMap.entrySet()) {
	    // if it is not a menu item add it
	    if (!currentPair.getKey().startsWith("menu.")) {
		String keyString = MakeKeyString(currentPair.getKey());
		String valueString = MakeEnvironmentString(currentPair.getValue(), ArduinoConst.ENV_KEY_ARDUINO_START);
		contribEnv.addVariable(new EnvironmentVariable(keyString, valueString), confDesc);
	    } else {

		String[] keySplit = currentPair.getKey().split("\\.");
		String menuID = keySplit[1];
		String menuItemID = keySplit[2];
		if (isThisMenuItemSelected(boardsFile, confDesc, boardID, menuID, menuItemID)) {
		    // we also need to skip the name
		    String StartValue = "menu." + menuID + "." + menuItemID + ".";
		    if (currentPair.getKey().startsWith(StartValue)) {
			String keyString = MakeKeyString(currentPair.getKey().substring(StartValue.length()));
			String valueString = MakeEnvironmentString(currentPair.getValue(), ArduinoConst.ENV_KEY_ARDUINO_START);
			contribEnv.addVariable(new EnvironmentVariable(keyString, valueString), confDesc);
		    }
		}
	    }
	}

	Map<String, String> menuSectionMap = boardsFile.getSection("menu");
	String[] optionNames = boardsFile.getMenuNames();
	for (int currentOption = 0; currentOption < optionNames.length; currentOption++) {
	    String optionName = optionNames[currentOption];
	    String optionValue = getBuildEnvironmentVariable(confDesc, ArduinoConst.ENV_KEY_JANTJE_START + optionName, "");
	    if (!optionValue.isEmpty()) {
		String optionValueID = null;
		String optionID = null;
		// Look for the option ID
		for (Entry<String, String> curOption : menuSectionMap.entrySet()) {
		    if (curOption.getValue().equals(optionName)) {
			String[] keySplit = curOption.getKey().split("\\.");
			if (keySplit.length == 1)
			    optionID = keySplit[0];
		    }
		}
		if (optionID != null) { // we have the option ID lets look for
					// the option value ID
		    for (Entry<String, String> curOption : menuSectionMap.entrySet()) {
			if (curOption.getValue().equals(optionValue)) {
			    String[] keySplit = curOption.getKey().split("\\.");
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
		    String keyStartsWithValue = optionID + "." + boardID + "." + optionValueID + ".";
		    for (Entry<String, String> curOption : menuSectionMap.entrySet()) {
			if (curOption.getKey().startsWith(keyStartsWithValue)) {
			    String key = curOption.getKey().substring(keyStartsWithValue.length());
			    contribEnv.addVariable(
				    new EnvironmentVariable(MakeKeyString(key), MakeEnvironmentString(curOption.getValue(),
					    ArduinoConst.ENV_KEY_ARDUINO_START)), confDesc);
			}
		    }

		}
	    }

	}
    }

    private static boolean isThisMenuItemSelected(ArduinoBoards boardsFile, ICConfigurationDescription confDesc, String boardID, String menuID,
	    String menuItemID) {

	String MenuName = boardsFile.getMenuNameFromID(menuID);
	String MenuItemName = boardsFile.getMenuItemNameFromID(boardID, menuID, menuItemID);

	String SelectedMenuItemName = getBuildEnvironmentVariable(confDesc, ArduinoConst.ENV_KEY_JANTJE_START + MenuName, "");
	if (SelectedMenuItemName.isEmpty()) {
	    return false; // This menu item has not been selected
	    // this should not happen
	}
	if (MenuItemName.equalsIgnoreCase(SelectedMenuItemName))
	    return true;
	return false;
    }

    /**
     * This method creates environment variables based on the platform.txt and boards.txt. platform.txt is processed first and then boards.txt. This
     * way boards.txt settings can overwrite common settings in platform.txt The environment variables are only valid for the project given as
     * parameter The project properties are used to identify the boards.txt and platform.txt as well as the board id to select the settings in the
     * board.txt file At the end also the path variable is set
     * 
     * from arduino IDE 1.6.5 an additional file generated by the arduino ide is processed. This is the first file processed.
     * 
     * To be able to quickly fix boards.txt and pmatform.txt problems I also added a arduino_eclipse_plugin.txt that is processed as a boards.txt file
     * and is processed after the arduino delivered boards.txt file.
     * 
     * @param project
     *            the project for which the environment variables are set
     * @param arduinoProperties
     *            the info of the selected board to set the variables for
     */

    public static void setTheEnvironmentVariables(IProject project, ICConfigurationDescription confDesc, boolean debugCompilerSettings) {

	// first get all the data we need
	IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	IContributedEnvironment contribEnv = envManager.getContributedEnvironment();

	IPath boardFileName = new Path(Common.getBuildEnvironmentVariable(confDesc, ArduinoConst.ENV_KEY_JANTJE_BOARDS_FILE,
		ArduinoInstancePreferences.getLastUsedBoardsFile()));
	IPath localPlatformFilename = new Path(Common.getBuildEnvironmentVariable(confDesc, ArduinoConst.ENV_KEY_JANTJE_PLATFORM_FILE, ""));

	String boardID = Common.getBuildEnvironmentVariable(confDesc, ArduinoConst.ENV_KEY_JANTJE_BOARD_ID, "");
	String architecture = Common.getBuildEnvironmentVariable(confDesc, ArduinoConst.ENV_KEY_JANTJE_ARCITECTURE_ID, "");
	String packageName = Common.getBuildEnvironmentVariable(confDesc, ArduinoConst.ENV_KEY_JANTJE_PACKAGE_ID, "");
	File anduinoIDEEnvNamesFile = Common.getArduinoIdeDumpName(packageName, architecture, boardID);
	IPath anduinoIDEEnvNamesPath = new Path(anduinoIDEEnvNamesFile.toString());
	architecture = architecture.toUpperCase();
	IPath workspacePath = new Path(Common.getWorkspaceRoot().getAbsolutePath());
	ArduinoBoards pluginPreProcessingBoardsTxt = new ArduinoBoards(workspacePath.append(ArduinoConst.PRE_PROCESSING_BOARDS_TXT).toString());
	ArduinoBoards pluginPostProcessingBoardsTxt = new ArduinoBoards(workspacePath.append(ArduinoConst.POST_PROCESSING_BOARDS_TXT).toString());
	IPath pluginPreProcessingPlatformTxt = new Path(workspacePath.append(ArduinoConst.PRE_PROCESSING_PLATFORM_TXT).toString());
	IPath pluginPostProcessingPlatformTxt = new Path(workspacePath.append(ArduinoConst.POST_PROCESSING_PLATFORM_TXT).toString());
	ArduinoBoards boardsFile = new ArduinoBoards(boardFileName.toOSString());
	if (!(pluginPreProcessingBoardsTxt.exists() && pluginPostProcessingBoardsTxt.exists())) {
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Plugin is not properly configured. Please reconfigure plugin."));
	    return;
	}

	// Now we have all info we can start processing

	// first remove all Arduino Variables so there is no memory effect
	RemoveAllArduinoEnvironmentVariables(contribEnv, confDesc);

	setTheEnvironmentVariablesSetTheDefaults(contribEnv, confDesc, localPlatformFilename);

	setTheEnvironmentVariablesAddAFile(contribEnv, confDesc, pluginPreProcessingPlatformTxt);
	setTheEnvironmentVariablesAddtheBoardsTxt(contribEnv, confDesc, pluginPreProcessingBoardsTxt, boardID, true);

	// Do some magic for the arduino:arduino stuff
	setTheEnvironmentVariablesRedirectToOtherVendors(contribEnv, confDesc, boardsFile, boardID, architecture.toLowerCase());// TOFIX again some
																// dirty thing
	// process the dump file from the arduino IDE
	if (anduinoIDEEnvNamesFile.exists()) {
	    setTheEnvironmentVariablesAddAFile(contribEnv, confDesc, anduinoIDEEnvNamesPath);
	}

	// process the platform file that is referenced in the build.core of the boards.txt file
	IPath referencedPlatformFilename = new Path(Common.getBuildEnvironmentVariable(confDesc,
		ArduinoConst.ENV_KEY_JANTJE_REFERENCED_PLATFORM_FILE, ""));
	if (referencedPlatformFilename.toFile().exists()) {
	    setTheEnvironmentVariablesAddAFile(contribEnv, confDesc, referencedPlatformFilename);
	}

	// process the platform file next to the selected boards.txt
	if (localPlatformFilename.toFile().exists()) {
	    setTheEnvironmentVariablesAddAFile(contribEnv, confDesc, localPlatformFilename);
	}
	// now process the boards file
	setTheEnvironmentVariablesAddtheBoardsTxt(contribEnv, confDesc, boardsFile, boardID, true);

	setTheEnvironmentVariablesAddAFile(contribEnv, confDesc, pluginPostProcessingPlatformTxt);
	setTheEnvironmentVariablesAddtheBoardsTxt(contribEnv, confDesc, pluginPostProcessingBoardsTxt, boardID, true);

	// Do some coded post processing
	setTheEnvironmentVariablesPostProcessing(contribEnv, confDesc);

	// If this is a debug config we modify the environment variables for compilation
	if (debugCompilerSettings) {
	    setTheEnvironmentVariablesModifyDebugCompilerSettings(confDesc, envManager, contribEnv);
	}

    }

    /**
     * This method is to support the [vendor]:[value] as described in
     * https://github.com/arduino/Arduino/wiki/Arduino-IDE-1.5-3rd-party-Hardware-specification This method parses the boards.txt file for
     * myboard.build.core myboard.build.variant currently not supported myboard.upload.tool myboard.bootloader.tool
     * 
     * in case myboard.build.core is of type [vendor]:[value] PATH_VARIABLE_NAME_ARDUINO_PLATFORM is changed to the correct value in case
     * myboard.build.variant is of type [vendor]:[value] PATH_VARIABLE_NAME_ARDUINO_PINS is changed to the correct value
     * 
     * this method also sets ENV_KEY_JANTJE_BUILD_CORE and ENV_KEY_JANTJE_BUILD_VARIANT to [value] of respectively myboard.build.core and
     * myboard.build.variant
     * 
     * This method relies on the post processing to set A.BUILD.CORE=${ENV_KEY_JANTJE_BUILD_CORE} A.BUILD.VARIANT=${ENV_KEY_JANTJE_BUILD_VARIANT}
     * 
     * @param contribEnv
     * @param confDesc
     * @param boardsFile
     * @param boardID
     */
    private static void setTheEnvironmentVariablesRedirectToOtherVendors(IContributedEnvironment contribEnv, ICConfigurationDescription confDesc,
	    ArduinoBoards boardsFile, String boardID, String architecture) {
	Map<String, String> boardInfo = boardsFile.getSection(boardID);
	if (boardInfo == null) {
	    return; // there is a problem with the board ID
	}
	String core = boardInfo.get("build.core");
	String variant = boardInfo.get("build.variant");
	if (core != null) {
	    String coreSplit[] = core.split(":");
	    if (coreSplit.length == 2) {
		String vendor = coreSplit[0];
		Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_BUILD_CORE, coreSplit[1]);
		IPath coreReference = findReferencedFolder(vendor, architecture.toLowerCase());// TODO fix this quickfix to lower which is really
											       // dirty
		if (coreReference == null) {
		    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "failed to find core reference: " + core));
		} else {
		    Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_build_core_path, coreReference.append(ARDUINO_CORE_FOLDER_NAME)
			    .append(coreSplit[1]).toString());
		    Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_REFERENCED_PLATFORM_FILE,
			    coreReference.append(PLATFORM_FILE_NAME).toString());
		}
	    } else {
		Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_BUILD_CORE, core);
		Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_REFERENCED_PLATFORM_FILE, "");
	    }
	}
	if (variant != null) {
	    String variantSplit[] = variant.split(":");
	    if (variantSplit.length == 2) {
		String vendor = variantSplit[0];
		Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_BUILD_VARIANT, variantSplit[1]);
		IPath variantReference = findReferencedFolder(vendor, architecture);
		if (variantReference == null) {
		    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "failed to find variant reference: " + variant));
		} else {
		    Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_build_variant_path,
			    variantReference.append(ARDUINO_VARIANTS_FOLDER_NAME).append(variantSplit[1]).toString());
		}
	    } else {
		Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_BUILD_VARIANT, variant);
	    }
	}
    }

    /**
     * This method looks for a referenced path. The search goes as follows ->private hardware folder ->boardsmanager folder ->installed arduino
     * hardware folder
     * 
     * The first matching folder is returned if no folder is found null is returned
     * 
     * @param vendor
     * @param architecture
     * @return
     */
    private static IPath findReferencedFolder(String vendor, String architecture) {

	Path privateHardwareFolder = new Path(getPrivateHardwarePath());
	IPath ideHardwareFolder = getArduinoIdeHardwarePath();
	IPath boardsManagerPackagesFolder = getArduinoBoardsManagerPackagesPath();
	if (privateHardwareFolder.append(vendor).append(architecture).toFile().exists()) {
	    return privateHardwareFolder.append(vendor).append(architecture);
	}
	if (boardsManagerPackagesFolder.append(vendor).append(ARDUINO_HARDWARE_FOLDER_NAME).append(architecture).toFile().exists()) {
	    // need to add version
	    IPath foundPath = boardsManagerPackagesFolder.append(vendor).append(ARDUINO_HARDWARE_FOLDER_NAME).append(architecture);
	    String[] versions = foundPath.toFile().list();
	    switch (versions.length) {
	    case 0:
		break;
	    case 1:
		return foundPath.append(versions[0]);
	    default:
		Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Multiple versions found in: " + foundPath.toString()
			+ " taking " + versions[0]));
		return foundPath.append(versions[0]);
	    }
	}
	if (ideHardwareFolder.append(vendor).append(architecture).toFile().exists()) {
	    return ideHardwareFolder.append(vendor).append(architecture);
	}
	return null;
    }

    /**
     * Some post processing is needed because the macro expansion resolves the "file tag" Therefore I split the "recipe" patterns in 2 parts (before
     * and after the "file tag") the pattern in the toolchain is then ${first part} ${files} ${second part}
     * 
     * @param contribEnv
     * @param confDesc
     */
    private static void setTheEnvironmentVariablesPostProcessing(IContributedEnvironment contribEnv, ICConfigurationDescription confDesc) {

	String recipes[] = { ENV_KEY_recipe_c_o_pattern, ENV_KEY_recipe_cpp_o_pattern, ENV_KEY_recipe_S_o_pattern,
		ENV_KEY_recipe_objcopy_hex_pattern, ENV_KEY_recipe_objcopy_eep_pattern, ENV_KEY_recipe_size_pattern, ENV_KEY_recipe_AR_pattern,
		ENV_KEY_recipe_c_combine_pattern };
	for (int curRecipe = 0; curRecipe < recipes.length; curRecipe++) {
	    String recipe = getBuildEnvironmentVariable(confDesc, recipes[curRecipe], "", false);

	    String recipeParts[] = recipe.split("(\"\\$\\{A.OBJECT_FILE}\")|(\\$\\{A.OBJECT_FILES})|(\"\\$\\{A.SOURCE_FILE}\")", 3);
	    switch (recipeParts.length) {
	    case 0:
		Common.setBuildEnvironmentVariable(contribEnv, confDesc, recipes[curRecipe] + ".1", "echo no command for " + recipes[curRecipe]);
		break;
	    case 1:
		Common.setBuildEnvironmentVariable(contribEnv, confDesc, recipes[curRecipe] + ".1", recipeParts[0]);
		break;
	    case 2:
		Common.setBuildEnvironmentVariable(contribEnv, confDesc, recipes[curRecipe] + ".1", recipeParts[0]);
		Common.setBuildEnvironmentVariable(contribEnv, confDesc, recipes[curRecipe] + ".2", recipeParts[1]);
		break;
	    case 3:
		Common.setBuildEnvironmentVariable(contribEnv, confDesc, recipes[curRecipe] + ".1", recipeParts[0]);
		Common.setBuildEnvironmentVariable(contribEnv, confDesc, recipes[curRecipe] + ".2", recipeParts[1]);
		Common.setBuildEnvironmentVariable(contribEnv, confDesc, recipes[curRecipe] + ".3", recipeParts[2]);
		break;
	    default:
		// this should never happen as the split is limited to 2
	    }
	}
	Common.setBuildEnvironmentVariable(contribEnv, confDesc, ArduinoConst.ENV_KEY_SOFTWARE, "ARDUINO");

	String uploadProg = ArduinoInstancePreferences.getLastUsedUploadProgrammer();
	// If the user selected a different upload protocol replace the protocol with the selected one
	if (!uploadProg.equals(ArduinoConst.DEFAULT)) {
	    Common.setBuildEnvironmentVariable(contribEnv, confDesc, ArduinoConst.ENV_KEY_ARDUINO_UPLOAD_PROTOCOL, uploadProg);
	}

	// Arduino uses the board approach for the upload tool.
	// as I'm not I create some special entries to work around it
	try {
	    String uploadTool = contribEnv.getVariable(ArduinoConst.ENV_KEY_upload_tool, confDesc).getValue().toUpperCase();
	    Common.setBuildEnvironmentVariable(contribEnv, confDesc, "A.CMD", makeEnvironmentVar("A.TOOLS." + uploadTool + ".CMD"));
	    Common.setBuildEnvironmentVariable(contribEnv, confDesc, "A.PATH", makeEnvironmentVar("A.TOOLS." + uploadTool + ".PATH"));
	    Common.setBuildEnvironmentVariable(contribEnv, confDesc, "A.CMD.PATH", makeEnvironmentVar("A.TOOLS." + uploadTool + ".CMD.PATH"));
	    Common.setBuildEnvironmentVariable(contribEnv, confDesc, "A.CONFIG.PATH", makeEnvironmentVar("A.TOOLS." + uploadTool + ".CONFIG.PATH"));
	} catch (Exception e) {
	    // ignore this exception as there is no upload tool defined.
	}

	// link build.core to jantje.build.core
	Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_build_core, makeEnvironmentVar(ENV_KEY_JANTJE_BUILD_CORE));
	// link build.variant to jantje.build.variant
	Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_build_variant, makeEnvironmentVar(ENV_KEY_JANTJE_BUILD_VARIANT));

    }

    /**
     * Converts the CPP and C compiler flags to not optimise for space/size and to leave symbols in. These changes allow step through debugging with
     * JTAG and Dragon AVR
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
	// TODO: This should move to another location eventually -- a bit hacky here (considering other env vars come from other -- a little bit
	// magical -- places).
	// I couldn't easily determine where that magic happened :(
	IEnvironmentVariable original = null;
	IEnvironmentVariable replacement = null;

	original = envManager.getVariable(ENV_KEY_ARDUINO_START + "COMPILER.C.FLAGS", confDesc, true);
	if (original != null) {
	    replacement = new EnvironmentVariable(original.getName(), original.getValue().replace("-g ", "-g2 ").replaceFirst("-O.? ", " "),
		    original.getOperation(), original.getDelimiter());
	    contribEnv.addVariable(replacement, confDesc);
	}

	original = envManager.getVariable(ENV_KEY_ARDUINO_START + "COMPILER.CPP.FLAGS", confDesc, true);
	if (original != null) {
	    replacement = new EnvironmentVariable(original.getName(), original.getValue().replace("-g", "-g2").replaceFirst("-O.? ", " "),
		    original.getOperation(), original.getDelimiter());
	    contribEnv.addVariable(replacement, confDesc);
	}
    }

    /**
     * When parsing boards.txt and platform.txt some processing needs to be done to get "acceptable environment variable values" This method does the
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
	String ret = inputString.replaceAll("\\{(?!\\{)", "\\${" + keyPrefix);
	StringBuilder sb = new StringBuilder(ret);
	String regex = "\\{[^}]*\\}";
	Pattern p = Pattern.compile(regex); // Create the pattern.
	Matcher matcher = p.matcher(sb); // Create the matcher.
	while (matcher.find()) {
	    String buf = sb.substring(matcher.start(), matcher.end()).toUpperCase();
	    sb.replace(matcher.start(), matcher.end(), buf);
	}
	return sb.toString();
    }

    /**
     * When parsing boards.txt and platform.txt some processing needs to be done to get "acceptable environment variable keys" This method does the
     * parsing
     * 
     * @param inputString
     *            the key string as read from the file
     * @return the string to be used as key for the environment variable
     */
    static String osString = null;

    private static String MakeKeyString(String string) {
	if (osString == null) {
	    if (Platform.getOS().equals(Platform.OS_LINUX)) {
		osString = "\\.LINUX";
	    } else if (Platform.getOS().equals(Platform.OS_WIN32)) {
		osString = "\\.WINDOWS";
	    } else {
		osString = "\\.\\.";
	    }
	}
	return ArduinoConst.ENV_KEY_ARDUINO_START + string.toUpperCase().replaceAll(osString, "");
    }

    /**
     * Set the project to force a rebuild. This method is called after the arduino settings have been updated. Note the only way I found I could get
     * this to work is by deleting the build folder Still then the "indexer needs to recheck his includes from the language provider which still is
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
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "failed to delete the folder " + cfgDescription.getName(), e));
	    }
	}

	List<ILanguageSettingsProvider> providers;
	if (cfgDescription instanceof ILanguageSettingsProvidersKeeper) {
	    providers = new ArrayList<ILanguageSettingsProvider>(((ILanguageSettingsProvidersKeeper) cfgDescription).getLanguageSettingProviders());
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
     * Given a source file calculates the base of the output file. this method may not be needed if I can used the eclipse default behavior. However
     * the eclipse default behavior is different from the arduino default behavior. So I keep it for now and we'll see how it goes The eclipse default
     * behavior is (starting from the project folder [configuration]/Source The Arduino default behavior is all in 1 location (so no subfolders)
     * 
     * @param Source
     *            The source file to find the
     * @return The base file name for the ouput if Source is "file.cpp" the output is "file.cpp"
     */
    public static IPath GetOutputName(IPath Source) {
	IPath outputName;
	if (Source.toString().startsWith("arduino")) {
	    outputName = new Path("arduino").append(Source.lastSegment());
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
	File privateHardwareFolder = new File(getPrivateHardwarePath());
	File HardwareFolder = getArduinoIdeHardwarePath().toFile();
	File boardsManagerPackagesFolder = getArduinoBoardsManagerPackagesPath().toFile();

	HashSet<String> boardFiles = new HashSet<String>();
	searchFiles(HardwareFolder, boardFiles, ArduinoConst.BOARDS_FILE_NAME, 3);
	searchFiles(boardsManagerPackagesFolder, boardFiles, ArduinoConst.BOARDS_FILE_NAME, 5);
	searchFiles(privateHardwareFolder, boardFiles, ArduinoConst.BOARDS_FILE_NAME, 3);
	if (boardFiles.size() == 0) {
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
		    "No boards.txt files found in the arduino hardware folders. I looked in:\nPrivate Hardware folder = \"" + privateHardwareFolder
			    + "\"\nHardwareFolder = \"" + HardwareFolder + "\"\nboardsManagerPackagesFolder = \"" + boardsManagerPackagesFolder
			    + "\"", null));
	    return null;
	}
	return boardFiles.toArray(new String[boardFiles.size()]);

    }

    /**
     * Reads the version number from the lib/version.txt file
     * 
     * @return the version number if found if no version number found the error returned by the file read method
     */
    static public String GetIDEVersion(IPath arduinoIDEPath) {

	File file = arduinoIDEPath.append(ArduinoConst.LIB_VERSION_FILE).toFile();
	try {
	    // Open the file that is the first
	    // command line parameter
	    FileInputStream fstream = new FileInputStream(file);
	    // Get the object of DataInputStream
	    try (DataInputStream in = new DataInputStream(fstream); BufferedReader br = new BufferedReader(new InputStreamReader(in));) {

		String strLine = br.readLine();
		in.close();
		return strLine;
	    }
	} catch (Exception e) {// Catch exception if any
	    System.err.println("Error: " + e.getMessage());
	    return e.getMessage();
	}
    }

    private static String makeEnvironmentVar(String string) {
	return "${" + string + "}";
    }

    /**
     * Give the string entered in the com port try to extract a host. If no host is found return null yun at xxx.yyy.zzz (arduino yun) returns
     * yun.local
     * 
     * @param mComPort
     * @return
     */
    public static String getHostFromComPort(String mComPort) {
	String host = mComPort.split(" ")[0];
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
	    Common.log(new Status(IStatus.INFO, ArduinoConst.CORE_PLUGIN_ID, "The folder you want to link to '" + source
		    + "' does not contain any files.", null));
	    return;
	}
	for (File f : a) {
	    if (f.isDirectory()) {
		LinkFolderToFolder(project, source.append(f.getName()), target.append(f.getName()));
	    } else {
		final IFile newFileHandle = project.getFile(target.append(f.getName()));
		try {
		    newFileHandle.createLink(source.append(f.getName()), IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, null);
		} catch (CoreException e) {
		    // TODO Auto-generated catch block
		    e.printStackTrace();
		}
	    }
	}

    }
}
