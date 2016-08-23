package io.sloeber.core.api;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICExclusionPatternPathEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;

import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.common.Const;
import it.baeyens.arduino.common.InstancePreferences;
import it.baeyens.arduino.tools.Helpers;
import it.baeyens.arduino.tools.ShouldHaveBeenInCDT;
import it.baeyens.arduino.tools.TxtFile;

public class BoardID {

    private String myUploadPort;
    private String myUploadProtocol;
    private String myBoardID;
    private Map<String, String> myOptions;
    private TxtFile myBoardsFile;
    private QualifiedName optionsStorageQualifiedName = new QualifiedName(Const.CORE_PLUGIN_ID,
	    Const.KEY_LAST_USED_BOARD_MENU_OPTIONS);

    /*
     * Create a sketchProject. This class does not really create a sketch
     * object. Nor does it look for existing (mapping) sketch projects This
     * class represents the data passed between the UI and the core This class
     * does contain a create to create the project When confdesc is null the
     * data will be taken from the "last used " otherwise the data is taken from
     * the project the confdesc belongs to
     * 
     */
    @SuppressWarnings("nls")
    public BoardID(ICConfigurationDescription confdesc) {
	if (confdesc == null) {
	    String boardsFile = InstancePreferences.getGlobalString(Const.KEY_LAST_USED_BOARDS_FILE, "");
	    this.myBoardsFile = new TxtFile(new File(boardsFile));
	    this.myBoardID = InstancePreferences.getGlobalString(Const.KEY_LAST_USED_BOARD, "");
	    this.myUploadPort = InstancePreferences.getGlobalString(Const.KEY_LAST_USED_COM_PORT, "");
	    this.myUploadProtocol = InstancePreferences.getGlobalString(Const.KEY_LAST_USED_UPLOAD_PROTOCOL,
		    Const.DEFAULT);
	    getLastUsedMenuOption();
	} else {
	    this.myUploadPort = Common.getBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_UPLOAD_PORT, "");
	    this.myUploadProtocol = Common.getBuildEnvironmentVariable(confdesc,
		    Const.get_Jantje_KEY_PROTOCOL(Const.ACTION_UPLOAD), ""); //$NON-NLS-1$
	    String boardsfile = Common.getBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_BOARDS_FILE, "");
	    this.myBoardID = Common.getBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_BOARD_ID, "");
	    this.myBoardsFile = new TxtFile(new File(boardsfile));
	    getMenuOptions(confdesc);
	}
    }

    /*
     * Method to create a project based on the board
     */
    public IProject createProject(String projectName, URI projectURI,
	    ArrayList<ConfigurationDescriptor> cfgNamesAndTCIds, CodeDescriptor codeDescription,
	    IProgressMonitor monitor) throws Exception {
	IProject projectHandle;
	projectHandle = ResourcesPlugin.getWorkspace().getRoot().getProject(Common.MakeNameCompileSafe(projectName));

	// try {
	IWorkspace workspace = ResourcesPlugin.getWorkspace();
	final IProjectDescription desc = workspace.newProjectDescription(projectHandle.getName());
	desc.setLocationURI(projectURI);

	projectHandle.create(desc, monitor);

	if (monitor.isCanceled()) {
	    throw new OperationCanceledException();
	}

	projectHandle.open(IResource.BACKGROUND_REFRESH, monitor);

	// Creates the .cproject file with the configurations
	ICProjectDescription prjCDesc = ShouldHaveBeenInCDT.setCProjectDescription(projectHandle, cfgNamesAndTCIds,
		true, monitor);

	// Add the C C++ AVR and other needed Natures to the project
	Helpers.addTheNatures(desc);

	// Add the Arduino folder

	Helpers.createNewFolder(projectHandle, Const.ARDUINO_CODE_FOLDER_NAME, null);

	for (ConfigurationDescriptor curConfig : cfgNamesAndTCIds) {
	    ICConfigurationDescription configurationDescription = prjCDesc.getConfigurationByName(curConfig.Name);
	    save(configurationDescription);
	    Helpers.setTheEnvironmentVariables(projectHandle, configurationDescription,
		    curConfig.DebugCompilerSettings);
	}

	// Set the path variables
	// ArduinoHelpers.setProjectPathVariables(prjCDesc.getActiveConfiguration());

	// Intermediately save or the adding code will fail
	// Release is the active config (as that is the "IDE" Arduino
	// type....)
	ICConfigurationDescription defaultConfigDescription = prjCDesc
		.getConfigurationByName(cfgNamesAndTCIds.get(0).Name);
	prjCDesc.setActiveConfiguration(defaultConfigDescription);

	// Insert The Arduino Code
	// NOTE: Not duplicated for debug (the release reference is just to
	// get at some environment variables)
	Helpers.addArduinoCodeToProject(projectHandle, defaultConfigDescription);

	ICResourceDescription cfgd = defaultConfigDescription.getResourceDescription(new Path(Const.EMPTY_STRING),
		true);
	ICExclusionPatternPathEntry[] entries = cfgd.getConfiguration().getSourceEntries();
	if (entries.length == 1) {
	    Path exclusionPath[] = new Path[8];
	    exclusionPath[0] = new Path(Const.LIBRARY_PATH_SUFFIX + "/?*/**/?xamples/**"); //$NON-NLS-1$
	    exclusionPath[1] = new Path(Const.LIBRARY_PATH_SUFFIX + "/?*/**/?xtras/**"); //$NON-NLS-1$
	    exclusionPath[2] = new Path(Const.LIBRARY_PATH_SUFFIX + "/?*/**/test*/**"); //$NON-NLS-1$
	    exclusionPath[3] = new Path(Const.LIBRARY_PATH_SUFFIX + "/?*/**/third-party/**"); //$NON-NLS-1$
	    exclusionPath[4] = new Path(Const.LIBRARY_PATH_SUFFIX + "**/._*"); //$NON-NLS-1$
	    exclusionPath[5] = new Path(Const.LIBRARY_PATH_SUFFIX + "/?*/c*/?*"); //$NON-NLS-1$
	    exclusionPath[6] = new Path(Const.LIBRARY_PATH_SUFFIX + "/?*/d*/?*"); //$NON-NLS-1$
	    exclusionPath[7] = new Path(Const.LIBRARY_PATH_SUFFIX + "/?*/D*/?*"); //$NON-NLS-1$

	    ICExclusionPatternPathEntry newSourceEntry = new CSourceEntry(entries[0].getFullPath(), exclusionPath,
		    ICSettingEntry.VALUE_WORKSPACE_PATH);
	    ICSourceEntry[] out = null;
	    out = new ICSourceEntry[1];
	    out[0] = (ICSourceEntry) newSourceEntry;
	    try {
		cfgd.getConfiguration().setSourceEntries(out);
	    } catch (CoreException e) {
		// ignore
	    }

	} else {
	    // this should not happen
	}

	// set warning levels default on
	IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
	IEnvironmentVariable var = new EnvironmentVariable(Const.ENV_KEY_JANTJE_WARNING_LEVEL,
		Const.ENV_KEY_WARNING_LEVEL_ON);
	contribEnv.addVariable(var, cfgd.getConfiguration());

	prjCDesc.setActiveConfiguration(defaultConfigDescription);
	prjCDesc.setCdtProjectCreated();
	CoreModel.getDefault().getProjectDescriptionManager().setProjectDescription(projectHandle, prjCDesc, true,
		null);
	projectHandle.setDescription(desc, new NullProgressMonitor());
	codeDescription.createFiles(projectHandle, monitor);
	monitor.done();
	return projectHandle;
    }

    public void save(ICConfigurationDescription confdesc) {
	if (confdesc != null) {

	    Common.setBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_PLATFORM_FILE, getPlatformFile());
	    Common.setBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_BOARD_NAME, getBoardName());
	    Common.setBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_BOARDS_FILE, getBoardsFile());
	    Common.setBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_BOARD_ID, this.myBoardID);
	    Common.setBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_ARCITECTURE_ID, getArchitecture());
	    Common.setBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_PACKAGE_ID, getPackage());

	    Common.setBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_UPLOAD_PORT, this.myUploadPort);
	    Common.setBuildEnvironmentVariable(confdesc, Const.get_Jantje_KEY_PROTOCOL(Const.ACTION_UPLOAD),
		    this.myUploadProtocol);
	    setMenuOptions(confdesc);

	    for (Map.Entry<String, String> curoption : this.myOptions.entrySet()) {
		Common.setBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_START + curoption.getKey(),
			curoption.getValue());
	    }
	}

	// Also save last used values
	InstancePreferences.setLastUsedBoardsFile(getBoardsFile());

	InstancePreferences.setGlobalValue(Const.KEY_LAST_USED_BOARD, this.myBoardID);
	InstancePreferences.setGlobalValue(Const.KEY_LAST_USED_COM_PORT, this.myUploadPort);

	InstancePreferences.setGlobalValue(Const.KEY_LAST_USED_UPLOAD_PROTOCOL, this.myUploadProtocol);
	setLastUsedMenuOption();

    }

    public String getPackage() {
	return this.myBoardsFile.getPackage();
    }

    public String getArchitecture() {
	return this.myBoardsFile.getArchitecture();
    }

    public String getBoardsFile() {
	return new Path(this.myBoardsFile.getTxtFile().toString()).toString();
    }

    public String getBoardName() {
	return this.myBoardsFile.getNameFromID(this.myBoardID);
    }

    public String getUploadPort() {
	return this.myUploadPort;
    }

    public String getUploadProtocol() {
	return this.myUploadProtocol;
    }

    public IPath getPlatformPath() {
	return new Path(this.myBoardsFile.getTxtFile().getParent());
    }

    public String getPlatformFile() {
	return getPlatformPath().append(Const.PLATFORM_FILE_NAME).toString();
    }

    public void setUploadPort(String newUploadPort) {
	this.myUploadPort = newUploadPort;
    }

    public void setUploadProtocol(String newUploadProtocol) {
	this.myUploadProtocol = newUploadProtocol;

    }

    public void setBoardID(String boardID) {
	this.myBoardID = boardID;
    }

    public void setBoardName(String boardName) {
	this.myBoardID = this.myBoardsFile.getIDFromName(boardName);
    }

    public void setBoardsFile(TxtFile boardsFile) {
	this.myBoardsFile = boardsFile;
    }

    public void setOptions(Map<String, String> options) {
	this.myOptions = options;

    }

    public Map<String, String> getOptions() {
	return this.myOptions;
    }

    private void setLastUsedMenuOption() {
	String store = ""; //$NON-NLS-1$
	String concat = ""; //$NON-NLS-1$
	for (Entry<String, String> curOption : this.myOptions.entrySet()) {
	    store = store + concat + curOption.getKey() + '=' + curOption.getValue();
	    concat = "\n"; //$NON-NLS-1$
	}
	InstancePreferences.setGlobalValue(Const.KEY_LAST_USED_BOARD_MENU_OPTIONS, store);

    }

    private void getLastUsedMenuOption() {
	this.myOptions = new HashMap<>();
	String storedValue = InstancePreferences.getGlobalString(Const.KEY_LAST_USED_BOARD_MENU_OPTIONS, ""); //$NON-NLS-1$
	String[] lines = storedValue.split("\n"); //$NON-NLS-1$
	for (String curLine : lines) {
	    String[] values = curLine.split("=", 2); //$NON-NLS-1$
	    if (values.length == 2) {
		this.myOptions.put(values[0], values[1]);
	    }
	}
    }

    private void setMenuOptions(ICConfigurationDescription confdesc) {
	String store = ""; //$NON-NLS-1$
	String concat = ""; //$NON-NLS-1$
	for (Entry<String, String> curOption : this.myOptions.entrySet()) {
	    store = store + concat + curOption.getKey() + '=' + curOption.getValue();
	    concat = "\n"; //$NON-NLS-1$
	}
	try {
	    confdesc.getProjectDescription().getProject().setPersistentProperty(this.optionsStorageQualifiedName,
		    store);
	} catch (CoreException e) {
	    e.printStackTrace();
	}

    }

    private void getMenuOptions(ICConfigurationDescription confdesc) {
	this.myOptions = new HashMap<>();
	String storedValue;
	try {
	    storedValue = confdesc.getProjectDescription().getProject()
		    .getPersistentProperty(this.optionsStorageQualifiedName);
	} catch (CoreException e) {
	    e.printStackTrace();
	    return;
	}
	if (storedValue != null) {
	    String[] lines = storedValue.split("\n"); //$NON-NLS-1$
	    for (String curLine : lines) {
		String[] values = curLine.split("=", 2); //$NON-NLS-1$
		if (values.length == 2) {
		    this.myOptions.put(values[0], values[1]);
		}
	    }
	}

    }
}
