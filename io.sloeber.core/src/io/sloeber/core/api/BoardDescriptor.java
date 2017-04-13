package io.sloeber.core.api;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.event.ChangeListener;

import org.eclipse.cdt.core.CCorePlugin;
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
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import io.sloeber.core.Activator;
import io.sloeber.core.InternalBoardDescriptor;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.Const;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.Programmers;
import io.sloeber.core.tools.ShouldHaveBeenInCDT;
import io.sloeber.core.tools.TxtFile;

@SuppressWarnings("nls")
public class BoardDescriptor {

	@Override
	public String toString() {
		return getBoardsFile() + " \"" + getBoardName() + "\" " + getUploadPort(); //$NON-NLS-2$
	}

	// preference nodes
	public static final String NODE_ARDUINO = Activator.NODE_ARDUINO;

	/*
	 * This is the basic info contained in the descriptor
	 */
	private String myUploadPort;
	private String myUploadProtocol;
	private String myBoardID;
	private Map<String, String> myOptions;

	/*
	 * the following data is stored to detect changes that will make the equal
	 * fail so os changes, workspace changes, eclipse install changes will force
	 * a update on the stored data
	 */
	private String myProjectName = new String();
	private String myOSName = Platform.getOS();
	private String myWorkSpaceLocation = Common.getWorkspaceRoot().toString();
	private String myWorkEclipseLocation = ConfigurationPreferences.getEclipseHome().toString();

	/*
	 * Stuff to make things work
	 */
	private File myBoardsFile;
	protected TxtFile myTxtFile;
	private ChangeListener myChangeListeners = null;
	private static final IEclipsePreferences myStorageNode = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);

	/*
	 * Some constants
	 */
	private static final String KEY_LAST_USED_BOARD = "Last used Board";
	private static final String KEY_LAST_USED_UPLOAD_PORT = "Last Used Upload port";
	private static final String KEY_LAST_USED_UPLOAD_PROTOCOL = "last Used upload Protocol";
	private static final String KEY_LAST_USED_BOARDS_FILE = "Last used Boards file";
	private static final String KEY_LAST_USED_BOARD_MENU_OPTIONS = "last used Board custom option selections";
	private static final String MENUSELECTION = Const.ENV_KEY_JANTJE_START + "MENU.";
	private static final String ENV_KEY_JANTJE_UPLOAD_PORT = Const.ENV_KEY_JANTJE_START + "COM_PORT";
	private static final String ENV_KEY_JANTJE_BOARD_NAME = Const.ENV_KEY_JANTJE_START + "BOARD_NAME";
	private static final String ENV_KEY_JANTJE_PROJECT_NAME = Const.ENV_KEY_JANTJE_START + "PROJECT_NAME";
	private static final String ENV_KEY_JANTJE_OS = Const.ENV_KEY_JANTJE_START + "OS_NAME";
	private static final String ENV_KEY_JANTJE_WORKSPACE_LOCATION = Const.ENV_KEY_JANTJE_START + "WORKSPACE_LOCATION";
	private static final String ENV_KEY_JANTJE_ECLIPSE_LOCATION = Const.ENV_KEY_JANTJE_START + "ECLIPSE_LOCATION";

	/**
	 * Compare 2 descriptors and return true is they are equal. This method
	 * detects - OS changes - project name changes - moves of workspace -
	 * changed runtine eclipse install
	 *
	 * @param obj
	 * @return true if equal otherwise false
	 */
	public boolean equals(BoardDescriptor obj) {
		if (!this.getUploadPort().equals(obj.getUploadPort())) {
			return false;
		}
		if (!this.getUploadProtocol().equals(obj.getUploadProtocol())) {
			return false;
		}
		if (!this.getBoardID().equals(obj.getBoardID())) {
			return false;
		}
		if (!this.getBoardsFile().equals(obj.getBoardsFile())) {
			return false;
		}
		if (!this.getOptions().equals(obj.getOptions())) {
			return false;
		}
		if (!this.getProjectName().equals(obj.getProjectName())) {
			return false;
		}
		if (!this.getMyOSName().equals(obj.getMyOSName())) {
			return false;
		}
		if (!this.getMyWorkEclipseLocation().equals(obj.getMyWorkEclipseLocation())) {
			return false;
		}
		if (!this.getMyWorkSpaceLocation().equals(obj.getMyWorkSpaceLocation())) {
			return false;
		}
		return true;
	}

	/*
	 * Create a sketchProject. This class does not really create a sketch
	 * object. Nor does it look for existing (mapping) sketch projects This
	 * class represents the data passed between the UI and the core This class
	 * does contain a create to create the project When confdesc is null the
	 * data will be taken from the "last used " otherwise the data is taken from
	 * the project the confdesc belongs to
	 *
	 */
	public static BoardDescriptor makeBoardDescriptor(ICConfigurationDescription confdesc) {
		return new InternalBoardDescriptor(confdesc);
	}

	protected BoardDescriptor() {

	}

	protected BoardDescriptor(ICConfigurationDescription confdesc) {
		if (confdesc == null) {
			this.myBoardsFile = new File(myStorageNode.get(KEY_LAST_USED_BOARDS_FILE, ""));
			this.myTxtFile = new TxtFile(this.myBoardsFile);
			this.myBoardID = myStorageNode.get(KEY_LAST_USED_BOARD, "");
			this.myUploadPort = myStorageNode.get(KEY_LAST_USED_UPLOAD_PORT, "");
			this.myUploadProtocol = myStorageNode.get(KEY_LAST_USED_UPLOAD_PROTOCOL,
					Defaults.getDefaultUploadProtocol());
			menuOptionsFromString(myStorageNode.get(KEY_LAST_USED_BOARD_MENU_OPTIONS, new String()));

		} else {
			this.myUploadPort = Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_UPLOAD_PORT, "");
			this.myUploadProtocol = Common.getBuildEnvironmentVariable(confdesc,
					Common.get_Jantje_KEY_PROTOCOL(Const.ACTION_UPLOAD), "");
			this.myBoardsFile = new File(
					Common.getBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_BOARDS_FILE, ""));
			this.myBoardID = Common.getBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_BOARD_ID, "");
			this.myProjectName = Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_PROJECT_NAME, "");
			this.myTxtFile = new TxtFile(this.myBoardsFile);
			this.myOSName = Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_OS, "");
			this.myWorkSpaceLocation = Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_WORKSPACE_LOCATION,
					"");
			this.myWorkEclipseLocation = Common.getBuildEnvironmentVariable(confdesc, ENV_KEY_JANTJE_ECLIPSE_LOCATION,
					"");

			this.myOptions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
			IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
			IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
			IEnvironmentVariable[] curVariables = contribEnv.getVariables(confdesc);
			for (IEnvironmentVariable curVariable : curVariables) {
				if (curVariable.getName().startsWith(MENUSELECTION)) {
					this.myOptions.put(curVariable.getName().substring(MENUSELECTION.length()), curVariable.getValue());
				}
			}
		}
	}

	public static BoardDescriptor makeBoardDescriptor(File boardsFile, String boardID, Map<String, String> options) {
		return new InternalBoardDescriptor(boardsFile, boardID, options);
	}

	/**
	 * make a board descriptor for each board in the board.txt file with the
	 * default options
	 *
	 * @param boardFile
	 * @return a list of board descriptors
	 */
	public static List<BoardDescriptor> makeBoardDescriptors(File boardFile) {
		TxtFile txtFile = new TxtFile(boardFile);
		List<BoardDescriptor> boards = new ArrayList<>();
		for (String curboardName : txtFile.getAllNames()) {
			Map<String, String> boardSection = txtFile.getSection(curboardName);
			if (!"true".equalsIgnoreCase(boardSection.get("hide"))) {
				boards.add(makeBoardDescriptor(boardFile, txtFile.getBoardIDFromBoardName(curboardName), null));
			}
		}
		return boards;
	}

	/**
	 * create a board descriptor
	 *
	 * @param boardsFile
	 * @param boardID
	 * @param options
	 *            if null default options are taken
	 */
	protected BoardDescriptor(File boardsFile, String boardID, Map<String, String> options) {
		this.myUploadPort = new String();
		this.myUploadProtocol = Defaults.getDefaultUploadProtocol();
		this.myBoardID = boardID;
		this.myOptions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		this.myBoardsFile = boardsFile;
		this.myTxtFile = new TxtFile(this.myBoardsFile);
		if (options != null) {
			this.myOptions.putAll(options);
		} else {
			TreeMap<String, String> allOptions = this.myTxtFile.getMenus();
			for (Map.Entry<String, String> curoption : allOptions.entrySet()) {
				if (!this.myOptions.containsKey(curoption.getKey())) {
					String[] menuOptions = this.myTxtFile.getMenuItemIDsFromMenuID(curoption.getKey(), boardID);
					if (menuOptions.length > 0) {
						this.myOptions.put(curoption.getKey(), menuOptions[0]);
					}
				}
			}
		}
	}

	/**
	 * tries to set the project to the boarddescriptor
	 *
	 * @param project
	 * @param monitor
	 * @return true if success false if failed
	 */
	public boolean configureProject(IProject project, IProgressMonitor monitor) {
		ICProjectDescription prjCDesc = CoreModel.getDefault().getProjectDescription(project);
		ICConfigurationDescription configurationDescription = prjCDesc.getActiveConfiguration();
		try {
			save(configurationDescription);
			// prjCDesc.setActiveConfiguration(configurationDescription);
			CoreModel.getDefault().getProjectDescriptionManager().setProjectDescription(project, prjCDesc, true, null);
		} catch (Exception e) {
			e.printStackTrace();
			Common.log(new Status(IStatus.ERROR, io.sloeber.core.Activator.getId(), "failed to save the board settings",
					e));
			return false;
		}
		return true;
	}

	/*
	 * Method to create a project based on the board
	 */
	public IProject createProject(String projectName, URI projectURI,
			ArrayList<ConfigurationDescriptor> cfgNamesAndTCIds, CodeDescriptor codeDescription,
			CompileOptions compileOptions, IProgressMonitor monitor) throws Exception {
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
			ICConfigurationDescription configurationDescription = prjCDesc.getConfigurationByName(curConfig.configName);
			compileOptions.save(configurationDescription);
			save(configurationDescription);

		}

		// Set the path variables
		// ArduinoHelpers.setProjectPathVariables(prjCDesc.getActiveConfiguration());

		// Intermediately save or the adding code will fail
		// Release is the active config (as that is the "IDE" Arduino
		// type....)
		ICConfigurationDescription defaultConfigDescription = prjCDesc
				.getConfigurationByName(cfgNamesAndTCIds.get(0).configName);

		ICResourceDescription cfgd = defaultConfigDescription.getResourceDescription(new Path(new String()), true);
		ICExclusionPatternPathEntry[] entries = cfgd.getConfiguration().getSourceEntries();
		if (entries.length == 1) {
			Path exclusionPath[] = new Path[8];
			exclusionPath[0] = new Path(Const.LIBRARY_PATH_SUFFIX + "/?*/**/?xamples/**");
			exclusionPath[1] = new Path(Const.LIBRARY_PATH_SUFFIX + "/?*/**/?xtras/**");
			exclusionPath[2] = new Path(Const.LIBRARY_PATH_SUFFIX + "/?*/**/test*/**");
			exclusionPath[3] = new Path(Const.LIBRARY_PATH_SUFFIX + "/?*/**/third-party/**");
			exclusionPath[4] = new Path(Const.LIBRARY_PATH_SUFFIX + "**/._*");
			exclusionPath[5] = new Path(Const.LIBRARY_PATH_SUFFIX + "/?*/c*/?*");
			exclusionPath[6] = new Path(Const.LIBRARY_PATH_SUFFIX + "/?*/d*/?*");
			exclusionPath[7] = new Path(Const.LIBRARY_PATH_SUFFIX + "/?*/D*/?*");

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
		codeDescription.createFiles(projectHandle, monitor);
		prjCDesc.setActiveConfiguration(defaultConfigDescription);
		prjCDesc.setCdtProjectCreated();
		CoreModel.getDefault().getProjectDescriptionManager().setProjectDescription(projectHandle, prjCDesc, true,
				null);
		projectHandle.setDescription(desc, new NullProgressMonitor());
		projectHandle.refreshLocal(IResource.DEPTH_INFINITE, null);
		monitor.done();
		return projectHandle;
	}

	public void save(ICConfigurationDescription confdesc) throws Exception {
		saveConfiguration(confdesc, null);
		if (confdesc != null) {
			IProject project = confdesc.getProjectDescription().getProject();

			Helpers.setTheEnvironmentVariables(project, confdesc, (InternalBoardDescriptor) this);

			Helpers.addArduinoCodeToProject(project, confdesc);

			Helpers.removeInvalidIncludeFolders(confdesc);
			Helpers.setDirtyFlag(project, confdesc);
		}
	}

	public void saveConfiguration() {
		saveConfiguration(null, null);
	}

	public void saveConfiguration(ICConfigurationDescription confDesc, IContributedEnvironment contribEnvIn) {
		if (confDesc != null) {
			IContributedEnvironment contribEnv = contribEnvIn;
			if (contribEnv == null) {
				IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
				contribEnv = envManager.getContributedEnvironment();
			}
			Common.setBuildEnvironmentVariable(contribEnv, confDesc, Const.ENV_KEY_JANTJE_PLATFORM_FILE,
					getPlatformFile());
			Common.setBuildEnvironmentVariable(contribEnv, confDesc, "JANTJE.SELECTED.PLATFORM",
					getPlatformPath().toString());
			Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_BOARD_NAME, getBoardName());
			Common.setBuildEnvironmentVariable(contribEnv, confDesc, Const.ENV_KEY_JANTJE_BOARDS_FILE, getBoardsFile());
			Common.setBuildEnvironmentVariable(contribEnv, confDesc, Const.ENV_KEY_JANTJE_BOARD_ID, this.myBoardID);
			Common.setBuildEnvironmentVariable(contribEnv, confDesc, Const.ENV_KEY_JANTJE_ARCITECTURE_ID,
					getArchitecture());
			Common.setBuildEnvironmentVariable(contribEnv, confDesc, Const.ENV_KEY_JANTJE_PACKAGE_ID, getPackage());
			Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_UPLOAD_PORT, this.myUploadPort);
			Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_PROJECT_NAME, getProjectName());
			Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_OS, this.myOSName);
			Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_WORKSPACE_LOCATION,
					this.myWorkSpaceLocation);
			Common.setBuildEnvironmentVariable(contribEnv, confDesc, ENV_KEY_JANTJE_ECLIPSE_LOCATION,
					this.myWorkEclipseLocation);
			Common.setBuildEnvironmentVariable(confDesc, Common.get_Jantje_KEY_PROTOCOL(Const.ACTION_UPLOAD),
					this.myUploadProtocol);
			if (this.myOptions != null) {
				for (Map.Entry<String, String> curoption : this.myOptions.entrySet()) {
					Common.setBuildEnvironmentVariable(contribEnv, confDesc, MENUSELECTION + curoption.getKey(),
							curoption.getValue());
				}
			}
		}

		// Also save last used values
		myStorageNode.put(KEY_LAST_USED_BOARDS_FILE, getBoardsFile());
		myStorageNode.put(KEY_LAST_USED_BOARD, this.myBoardID);
		myStorageNode.put(KEY_LAST_USED_UPLOAD_PORT, this.myUploadPort);
		myStorageNode.put(KEY_LAST_USED_UPLOAD_PROTOCOL, this.myUploadProtocol);
		myStorageNode.put(KEY_LAST_USED_BOARD_MENU_OPTIONS, menuOptionsToString());
	}

	public String getPackage() {
		return this.myTxtFile.getPackage();
	}

	public String getArchitecture() {
		return this.myTxtFile.getArchitecture();
	}

	public String getBoardsFile() {
		return new Path(this.myBoardsFile.toString()).toString();
	}

	public String getBoardName() {
		return this.myTxtFile.getNameFromID(this.myBoardID);
	}

	public String getUploadPort() {
		return this.myUploadPort;
	}

	public String getUploadProtocol() {
		return this.myUploadProtocol;
	}

	public IPath getPlatformPath() {
		try {
			return new Path(this.myBoardsFile.getParent());
		} catch (Exception e) {
			return new Path(new String());
		}
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
		if (!boardID.equals(this.myBoardID)) {
			this.myBoardID = boardID;
			informChangeListeners();
		}
	}

	public void setBoardName(String boardName) {
		String newBoardID = this.myTxtFile.getBoardIDFromBoardName(boardName);
		if ((newBoardID == null || this.myBoardID.equals(newBoardID))) {
			return;
		}

		this.myBoardID = newBoardID;
		informChangeListeners();

	}

	public void setBoardsFile(File boardsFile) {
		if (boardsFile == null) {
			return;// ignore
		}
		if (this.myBoardsFile.equals(boardsFile)) {
			return;
		}

		this.myBoardsFile = boardsFile;
		this.myTxtFile = new TxtFile(this.myBoardsFile);
		informChangeListeners();
	}

	public void setOptions(Map<String, String> options) {
		this.myOptions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		this.myOptions.putAll(options);
	}

	/**
	 * Returns the options for this board This reflects the options selected
	 * through the menu functionality in the boards.txt
	 *
	 * @return a map of case insensitive ordered key value pairs
	 */
	public Map<String, String> getOptions() {
		return this.myOptions;
	}

	// private void setMenuOptions(ICConfigurationDescription confdesc) {
	// String store =menuOptionsToString();
	// try {
	// confdesc.getProjectDescription().getProject().setPersistentProperty(this.optionsStorageQualifiedName,
	// store);
	// } catch (CoreException e) {
	// e.printStackTrace();
	// }
	//
	// }

	public String getBoardID() {
		return this.myBoardID;
	}

	public String[] getCompatibleBoards() {
		return this.myTxtFile.getAllNames();
	}

	public String[] getUploadProtocols() {
		if (this.myBoardsFile.exists()) {
			return Programmers.getUploadProtocols(this.myBoardsFile.toString());
		}
		return new String[0];
	}

	public String[] getMenuItemNamesFromMenuID(String menuID) {
		return this.myTxtFile.getMenuItemNamesFromMenuID(menuID, this.myBoardID);
	}

	public Set<String> getAllMenuNames() {
		return this.myTxtFile.getMenuNames();
	}

	public TreeMap<String, IPath> getAllExamples() {
		return BoardsManager.getAllExamples(this);
	}

	public void addChangeListener(ChangeListener l) {
		this.myChangeListeners = l;
	}

	public void removeChangeListener() {
		this.myChangeListeners = null;
	}

	private void informChangeListeners() {
		if (this.myChangeListeners != null) {
			this.myChangeListeners.stateChanged(null);
		}
	}

	public String getMenuIdFromMenuName(String menuName) {
		return this.myTxtFile.getMenuIDFromMenuName(menuName);
	}

	public String getMenuNameFromMenuID(String id) {
		return this.myTxtFile.getMenuNameFromID(id);
	}

	public String getMenuItemNamedFromMenuItemID(String menuItemID, String menuID) {
		return this.myTxtFile.getMenuItemNameFromMenuItemID(this.myBoardID, menuID, menuItemID);
	}

	/**
	 * convert the options to a string so it can be stored
	 *
	 * @return a string representation of the options
	 */
	private String menuOptionsToString() {
		String ret = new String();
		String concat = new String();
		if (this.myOptions != null) {
			for (Entry<String, String> curOption : this.myOptions.entrySet()) {
				ret += concat + curOption.getKey() + '=' + curOption.getValue();
				concat = "\n";
			}
		}
		return ret;
	}

	/**
	 * convert a string to a options so it can be read from a string based
	 * storage
	 *
	 * @param options
	 */
	private void menuOptionsFromString(String options) {
		this.myOptions = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		if (options != null) {
			String[] lines = options.split("\n");
			for (String curLine : lines) {
				String[] values = curLine.split("=", 2);
				if (values.length == 2) {
					this.myOptions.put(values[0], values[1]);
				}
			}
		}
	}

	public String getMenuItemIDFromMenuItemName(String menuItemName, String menuID) {
		return this.myTxtFile.getMenuItemIDFromMenuItemName(this.myBoardID, menuID, menuItemName);
	}

	public static String getUploadPort(IProject project) {
		return Common.getBuildEnvironmentVariable(project, ENV_KEY_JANTJE_UPLOAD_PORT, new String());
	}

	public static void storeUploadPort(IProject project, String uploadPort) {
		Common.setBuildEnvironmentVariable(project, ENV_KEY_JANTJE_UPLOAD_PORT, uploadPort);
	}

	public String getMyOSName() {
		return this.myOSName;
	}

	public String getMyWorkSpaceLocation() {
		return this.myWorkSpaceLocation;
	}

	public String getMyWorkEclipseLocation() {
		return this.myWorkEclipseLocation;
	}

	public String getProjectName() {
		return this.myProjectName;
	}

}
