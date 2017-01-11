package io.sloeber.core.api;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
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
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

import io.sloeber.common.Common;
import io.sloeber.common.Const;
import io.sloeber.core.Activator;
import io.sloeber.core.InternalBoardDescriptor;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.Programmers;
import io.sloeber.core.tools.ShouldHaveBeenInCDT;
import io.sloeber.core.tools.TxtFile;

public class BoardDescriptor {
	// preference nodes
	public static final String NODE_ARDUINO = Activator.NODE_ARDUINO;
	/**
	 *
	 */

	private String myUploadPort;
	private String myUploadProtocol;
	private String myBoardID;
	private Map<String, String> myOptions;
	private File myBoardsFile;
	protected TxtFile myTxtFile;
	private ChangeListener myChangeListeners = null;
	private static final IEclipsePreferences myStorageNode = InstanceScope.INSTANCE.getNode(NODE_ARDUINO);

	private static final String KEY_LAST_USED_BOARD = "Last used Board"; //$NON-NLS-1$
	private static final String KEY_LAST_USED_UPLOAD_PORT = "Last Used Upload port"; //$NON-NLS-1$
	private static final String KEY_LAST_USED_UPLOAD_PROTOCOL = "last Used upload Protocol"; //$NON-NLS-1$
	private static final String KEY_LAST_USED_BOARDS_FILE = "Last used Boards file"; //$NON-NLS-1$
	private static final String KEY_LAST_USED_BOARD_MENU_OPTIONS = "last used Board custom option selections"; //$NON-NLS-1$
	private static final String MENUSELECTION = Const.ENV_KEY_JANTJE_START + "MENU."; //$NON-NLS-1$

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

	@SuppressWarnings("nls")
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
			this.myUploadPort = Common.getBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_UPLOAD_PORT, "");
			this.myUploadProtocol = Common.getBuildEnvironmentVariable(confdesc,
					Common.get_Jantje_KEY_PROTOCOL(Const.ACTION_UPLOAD), "");
			this.myBoardsFile = new File(
					Common.getBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_BOARDS_FILE, ""));
			this.myBoardID = Common.getBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_BOARD_ID, "");
			this.myTxtFile = new TxtFile(this.myBoardsFile);

			this.myOptions = new HashMap<>();
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

	protected BoardDescriptor(File boardsFile, String boardID, Map<String, String> options) {
		this.myUploadPort = Const.EMPTY_STRING;
		this.myUploadProtocol = Defaults.getDefaultUploadProtocol();
		this.myBoardID = boardID;
		this.myOptions = options;
		this.myBoardsFile = boardsFile;
		this.myTxtFile = new TxtFile(this.myBoardsFile);
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
			// TODO Auto-generated catch block
			e.printStackTrace();
			Common.log(new Status(IStatus.ERROR, io.sloeber.core.Activator.getId(), "failed to save the board settings", //$NON-NLS-1$
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
		if (confdesc != null) {

			Common.setBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_PLATFORM_FILE, getPlatformFile());
			Common.setBuildEnvironmentVariable(confdesc, "JANTJE.SELECTED.PLATFORM", getPlatformPath().toString()); //$NON-NLS-1$
			Common.setBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_BOARD_NAME, getBoardName());
			Common.setBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_BOARDS_FILE, getBoardsFile());
			Common.setBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_BOARD_ID, this.myBoardID);
			Common.setBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_ARCITECTURE_ID, getArchitecture());
			Common.setBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_PACKAGE_ID, getPackage());
			Common.setBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_UPLOAD_PORT, this.myUploadPort);
			Common.setBuildEnvironmentVariable(confdesc, Common.get_Jantje_KEY_PROTOCOL(Const.ACTION_UPLOAD),
					this.myUploadProtocol);
			if (this.myOptions != null) {
				for (Map.Entry<String, String> curoption : this.myOptions.entrySet()) {
					Common.setBuildEnvironmentVariable(confdesc, MENUSELECTION + curoption.getKey(),
							curoption.getValue());
				}
			}
			IProject project = confdesc.getProjectDescription().getProject();

			Helpers.setTheEnvironmentVariables(project, confdesc, (InternalBoardDescriptor) this);

			Helpers.addArduinoCodeToProject(project, confdesc);

			Helpers.removeInvalidIncludeFolders(confdesc);
			Helpers.setDirtyFlag(project, confdesc);
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
		this.myOptions = options;

	}

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

	public String getMenuItemNamedFromMenuItemID(String menuItemID, String menuID) {
		return this.myTxtFile.getMenuItemNameFromMenuItemID(this.myBoardID, menuID, menuItemID);
	}

	private String menuOptionsToString() {
		String ret = new String();
		String concat = new String();
		if (this.myOptions != null) {
			for (Entry<String, String> curOption : this.myOptions.entrySet()) {
				ret += concat + curOption.getKey() + '=' + curOption.getValue();
				concat = "\n"; //$NON-NLS-1$
			}
		}
		return ret;
	}

	private void menuOptionsFromString(String options) {
		this.myOptions = new HashMap<>();
		if (options != null) {
			String[] lines = options.split("\n"); //$NON-NLS-1$
			for (String curLine : lines) {
				String[] values = curLine.split("=", 2); //$NON-NLS-1$
				if (values.length == 2) {
					this.myOptions.put(values[0], values[1]);
				}
			}
		}
	}

	public String getMenuItemIDFromMenuItemName(String menuItemName, String menuID) {
		return this.myTxtFile.getMenuItemIDFromMenuItemName(this.myBoardID, menuID, menuItemName);
	}

}
