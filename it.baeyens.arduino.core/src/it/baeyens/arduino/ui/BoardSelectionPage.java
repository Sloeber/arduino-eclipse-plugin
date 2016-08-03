package it.baeyens.arduino.ui;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.parser.util.ArrayUtil;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.cdt.ui.newui.AbstractPage;
import org.eclipse.cdt.ui.newui.ICPropertyProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import cc.arduino.packages.discoverers.NetworkDiscovery;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.common.Const;
import it.baeyens.arduino.common.InstancePreferences;
import it.baeyens.arduino.tools.Helpers;
import it.baeyens.arduino.tools.Programmers;
import it.baeyens.arduino.tools.TxtFile;

/**
 * The ArduinoSelectionPage class is used in the new wizard and the project
 * properties. This class controls the gui and the data underneath the gui. This
 * class allows to select the arduino board and the port name
 * 
 * @author Jan Baeyens
 * @see ArduinoProperties ArduinoSettingsPage
 * 
 */
public class BoardSelectionPage extends AbstractCPropertyTab {
    // global stuff to allow to communicate outside this class
    public Text mFeedbackControl;

    // GUI elements
    protected Combo mControlBoardsTxtFile;
    protected Combo mcontrolBoardName;
    protected Combo mControlUploadProtocol;
    protected LabelCombo mControlUploadPort;
    protected LabelCombo[] mBoardOptionCombos = null;
    private final int ncol = 2;
    protected Listener mBoardSelectionChangedListener = null;

    // the properties to modify
    private String[] mAllBoardsFileNames; // contains the boards.txt file names
					  // found
					  // for the current arduino environment
    TxtFile mAllBoardsFiles[] = null; // contains the boards.txt content found
				      // for the current arduino environment

    /**
     * Get the configuration we are currently working in. The configuration is
     * null if we are in the create sketch wizard.
     * 
     * @return the configuration to save info into
     */
    public ICConfigurationDescription getConfdesc() {
	if (this.page != null) {
	    return getResDesc().getConfiguration();
	}
	return null;
    }

    /**
     * Listener for the child or leave fields. The listener saves the
     * information in the configuration
     * 
     * @author jan
     *
     */
    final class childFieldListener implements Listener {

	@Override
	public void handleEvent(Event e) {
	    ICConfigurationDescription confdesc = getConfdesc();
	    if (confdesc != null) {
		this.myCombo.StoreValue(confdesc);
	    }
	    isPageComplete();
	}

	private LabelCombo myCombo;

	public void setInfo(LabelCombo Combo) {

	    this.myCombo = Combo;
	}
    }

    private Listener boardFileModifyListener = new Listener() {
	@Override
	public void handleEvent(Event e) {
	    IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	    IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
	    ICConfigurationDescription confdesc = getConfdesc();

	    int selectedBoardFile = BoardSelectionPage.this.mControlBoardsTxtFile.getSelectionIndex();
	    String boardFile = BoardSelectionPage.this.mControlBoardsTxtFile.getText().trim();
	    if (confdesc != null) {
		IEnvironmentVariable var = new EnvironmentVariable(Const.ENV_KEY_JANTJE_BOARDS_FILE, boardFile);
		contribEnv.addVariable(var, confdesc);
		IPath platformPath = new Path(new File(boardFile).getParent()).append(Const.PLATFORM_FILE_NAME);
		var = new EnvironmentVariable(Const.ENV_KEY_JANTJE_PLATFORM_FILE, platformPath.toString());
		contribEnv.addVariable(var, confdesc);
	    }

	    /*
	     * Change the list of available boards
	     */
	    String CurrentBoard = BoardSelectionPage.this.mcontrolBoardName.getText();
	    BoardSelectionPage.this.mcontrolBoardName.removeAll();
	    BoardSelectionPage.this.mcontrolBoardName
		    .setItems(BoardSelectionPage.this.mAllBoardsFiles[selectedBoardFile].getAllNames());
	    BoardSelectionPage.this.mcontrolBoardName.setText(CurrentBoard);

	    /*
	     * Change the list of available upload protocols
	     */
	    String CurrentUploadProtocol = BoardSelectionPage.this.mControlUploadProtocol.getText();
	    BoardSelectionPage.this.mControlUploadProtocol.removeAll();
	    BoardSelectionPage.this.mControlUploadProtocol.setItems(Programmers.getUploadProtocols(boardFile));
	    BoardSelectionPage.this.mControlUploadProtocol.setText(CurrentUploadProtocol);

	    if (BoardSelectionPage.this.mControlUploadProtocol.getText().isEmpty()) {

		BoardSelectionPage.this.mControlUploadProtocol.setText(Const.DEFAULT);
	    }

	    BoardSelectionPage.this.BoardModifyListener.handleEvent(null);
	}
    };

    protected Listener BoardModifyListener = new Listener() {
	@Override
	public void handleEvent(Event e) {

	    int selectedBoardFile = BoardSelectionPage.this.mControlBoardsTxtFile.getSelectionIndex();
	    String boardName = BoardSelectionPage.this.mcontrolBoardName.getText();

	    for (LabelCombo curLabelCombo : BoardSelectionPage.this.mBoardOptionCombos) {
		curLabelCombo.setItems(BoardSelectionPage.this.mAllBoardsFiles[selectedBoardFile]
			.getMenuItemNames(curLabelCombo.getMenuName(), boardName));
	    }

	    IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	    IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
	    ICConfigurationDescription confdesc = getConfdesc();
	    if (confdesc != null) {
		IEnvironmentVariable var = new EnvironmentVariable(Const.ENV_KEY_JANTJE_BOARD_NAME, boardName);
		contribEnv.addVariable(var, confdesc);
	    }
	    isPageComplete();
	    EnableControls();
	}
    };

    @Override
    public void createControls(Composite parent, ICPropertyProvider provider) {
	super.createControls(parent, provider);
	draw(parent);
    }

    public void setListener(Listener BoardSelectionChangedListener) {
	this.mBoardSelectionChangedListener = BoardSelectionChangedListener;
    }

    private static void createLabel(Composite parent, int ncol, String t) {
	Label line = new Label(parent, SWT.HORIZONTAL | SWT.BOLD);
	line.setText(t);
	GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
	gridData.horizontalSpan = ncol;
	line.setLayoutData(gridData);
    }

    private static void createLine(Composite parent, int ncol) {
	Label line = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.BOLD);
	GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
	gridData.horizontalSpan = ncol;
	line.setLayoutData(gridData);
    }

    public void draw(Composite composite) {
	// create the desired layout for this wizard page
	ICConfigurationDescription confdesc = getConfdesc();
	GridLayout theGridLayout = new GridLayout();
	theGridLayout.numColumns = this.ncol;
	composite.setLayout(theGridLayout);

	GridData theGriddata;
	this.mAllBoardsFileNames = Helpers.getBoardsFiles();
	if (this.mAllBoardsFileNames == null) {
	    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
		    "ArduinoHelpers.getBoardsFiles() returns null.\nThis should not happen.\nIt looks like the download of the boards failed.")); //$NON-NLS-1$
	}
	Arrays.sort(this.mAllBoardsFileNames);
	this.mAllBoardsFiles = new TxtFile[this.mAllBoardsFileNames.length];
	for (int currentBoardFile = 0; currentBoardFile < this.mAllBoardsFileNames.length; currentBoardFile++) {
	    this.mAllBoardsFiles[currentBoardFile] = new TxtFile(new File(this.mAllBoardsFileNames[currentBoardFile]));

	}

	switch (this.mAllBoardsFileNames.length) {
	case 0:
	    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, Messages.error_no_platform_files_found, null));
	    break;
	case 1: {
	    break;
	}
	default: {
	    // create a combo to select the boards
	    createLabel(composite, this.ncol, "The boards.txt file you want to use"); //$NON-NLS-1$
	    new Label(composite, SWT.NONE).setText("Boards.txt file:"); //$NON-NLS-1$
	}

	}

	this.mControlBoardsTxtFile = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
	theGriddata = new GridData();
	theGriddata.horizontalAlignment = SWT.FILL;
	theGriddata.horizontalSpan = (this.ncol - 1);
	this.mControlBoardsTxtFile.setLayoutData(theGriddata);
	this.mControlBoardsTxtFile.setEnabled(false);
	this.mControlBoardsTxtFile.setItems(this.mAllBoardsFileNames);

	createLine(composite, this.ncol);
	// -------

	// ------
	createLabel(composite, this.ncol, "Your Arduino board specifications"); //$NON-NLS-1$
	new Label(composite, SWT.NONE).setText("Board:"); //$NON-NLS-1$
	this.mcontrolBoardName = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
	theGriddata = new GridData();
	theGriddata.horizontalAlignment = SWT.FILL;
	theGriddata.horizontalSpan = (this.ncol - 1);
	this.mcontrolBoardName.setLayoutData(theGriddata);
	this.mcontrolBoardName.setEnabled(false);

	// ------
	new Label(composite, SWT.NONE).setText("Upload Protocol:"); //$NON-NLS-1$
	this.mControlUploadProtocol = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
	theGriddata = new GridData();
	theGriddata.horizontalAlignment = SWT.FILL;
	theGriddata.horizontalSpan = (this.ncol - 1);
	this.mControlUploadProtocol.setLayoutData(theGriddata);
	this.mControlUploadProtocol.setEnabled(false);

	// ----
	this.mControlUploadPort = new LabelCombo(composite, Messages.ui_port, this.ncol - 1,
		Const.ENV_KEY_JANTJE_COM_PORT, false);

	this.mControlUploadPort.setItems(ArrayUtil.addAll(NetworkDiscovery.getList(), Common.listComPorts()));

	createLine(composite, this.ncol);

	String[] menuNames = new String[30];
	for (int curBoardsFile = 0; curBoardsFile < this.mAllBoardsFiles.length; curBoardsFile++) {
	    ArrayUtil.addAll(menuNames, this.mAllBoardsFiles[curBoardsFile].getMenuNames());
	}
	menuNames = ArrayUtil.removeDuplicates(menuNames);
	this.mBoardOptionCombos = new LabelCombo[menuNames.length];
	for (int currentOption = 0; currentOption < menuNames.length; currentOption++) {
	    String menuName = menuNames[currentOption];
	    this.mBoardOptionCombos[currentOption] = new LabelCombo(composite, menuName, this.ncol - 1,
		    Const.ENV_KEY_JANTJE_START + menuName, true);
	}

	// Create the control to alert parents of changes
	this.mFeedbackControl = new Text(composite, SWT.None);
	this.mFeedbackControl.setVisible(false);
	this.mFeedbackControl.setEnabled(false);
	theGriddata = new GridData();
	theGriddata.horizontalSpan = 0;
	this.mFeedbackControl.setLayoutData(theGriddata);
	// End of special controls

	setValues(confdesc);

	// enable the listeners
	childFieldListener controlUploadPortlistener = new childFieldListener();
	controlUploadPortlistener.setInfo(this.mControlUploadPort);
	this.mControlUploadPort.addListener(controlUploadPortlistener);

	this.mcontrolBoardName.addListener(SWT.Modify, this.BoardModifyListener);
	this.mControlBoardsTxtFile.addListener(SWT.Modify, this.boardFileModifyListener);

	for (LabelCombo curLabelCombo : this.mBoardOptionCombos) {
	    childFieldListener comboboxModifyListener = new childFieldListener();
	    comboboxModifyListener.setInfo(curLabelCombo);
	    curLabelCombo.addListener(comboboxModifyListener);
	}

	EnableControls();
	Dialog.applyDialogFont(composite);
    }

    public boolean isPageComplete() {

	boolean MenuOpionsValidAndComplete = true;
	boolean ret = true;
	int selectedBoardFile = this.mControlBoardsTxtFile.getSelectionIndex();
	if (selectedBoardFile == -1)
	    return false;

	for (LabelCombo curLabelCombo : this.mBoardOptionCombos) {
	    MenuOpionsValidAndComplete = MenuOpionsValidAndComplete && curLabelCombo.isValid();
	}

	ret = !this.mcontrolBoardName.getText().trim().isEmpty() && MenuOpionsValidAndComplete;
	if (!this.mFeedbackControl.getText().equals(ret ? Const.TRUE : Const.FALSE)) {
	    this.mFeedbackControl.setText(ret ? Const.TRUE : Const.FALSE);
	}
	if (ret) {
	    if (this.mBoardSelectionChangedListener != null) {
		this.mBoardSelectionChangedListener.handleEvent(new Event());
	    }
	}

	return ret;
    }

    protected void EnableControls() {
	this.mcontrolBoardName.setEnabled(true);
	this.mControlUploadPort.setEnabled(true);
	this.mControlUploadProtocol.setEnabled(true);
	this.mControlBoardsTxtFile.setEnabled((this.mAllBoardsFileNames.length > 1));
	this.mControlBoardsTxtFile.setVisible(this.mAllBoardsFileNames.length > 1);
	for (LabelCombo curLabelCombo : this.mBoardOptionCombos) {
	    curLabelCombo.setVisible(true);
	}
    }

    @Override
    public boolean canBeVisible() {
	return true;
    }

    @Override
    protected void performDefaults() {
	// nothing to do here

    }

    @Override
    protected void updateData(ICResourceDescription cfg) {
	setValues(cfg.getConfiguration());
    }

    @Override
    protected void updateButtons() {
	// nothing to do here

    }

    /**
     * Based on the current selection save the last used values$this to make
     * sure you can create the same sketch quickly again
     */
    private void saveAllLastUseds() {
	//
	String boardFile = this.mControlBoardsTxtFile.getText().trim();
	String boardName = this.mcontrolBoardName.getText().trim();
	String uploadPort = this.mControlUploadPort.getValue();
	String uploadProtocol = this.mControlUploadProtocol.getText().trim();
	InstancePreferences.setLastUsedBoardsFile(boardFile);
	InstancePreferences.setLastUsedArduinoBoard(boardName);
	InstancePreferences.setLastUsedUploadPort(uploadPort);
	InstancePreferences.setLastUsedUploadProtocol(uploadProtocol);

	Map<String, String> options = new HashMap<>();
	for (LabelCombo curLabelCombo : BoardSelectionPage.this.mBoardOptionCombos) {

	    options.put(curLabelCombo.getMenuName(), curLabelCombo.getValue());
	}
	InstancePreferences.setLastUsedMenuOption(options);
    }

    /**
     * Based on the selected board and parameters save all info needed to the
     * build environments
     * 
     * @param confdesc
     */
    public void saveAllSelections(ICConfigurationDescription confdesc) {
	String boardFile = this.mControlBoardsTxtFile.getText().trim();
	String boardName = this.mcontrolBoardName.getText().trim();
	String uploadPort = this.mControlUploadPort.getValue();
	String uploadProtocol = this.mControlUploadProtocol.getText();
	IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	IContributedEnvironment contribEnv = envManager.getContributedEnvironment();

	// Set the path variables
	IPath platformPath = new Path(new File(this.mControlBoardsTxtFile.getText().trim()).getParent())
		.append(Const.PLATFORM_FILE_NAME);
	Common.setBuildEnvironmentVariable(contribEnv, confdesc, Const.ENV_KEY_JANTJE_BOARDS_FILE, boardFile);
	Common.setBuildEnvironmentVariable(contribEnv, confdesc, Const.ENV_KEY_JANTJE_PLATFORM_FILE,
		platformPath.toString());
	Common.setBuildEnvironmentVariable(contribEnv, confdesc, Const.ENV_KEY_JANTJE_BOARD_NAME, boardName);
	Common.setBuildEnvironmentVariable(contribEnv, confdesc, Const.ENV_KEY_JANTJE_COM_PORT, uploadPort);
	Common.setBuildEnvironmentVariable(contribEnv, confdesc, Const.get_Jantje_KEY_PROTOCOL(Const.ACTION_UPLOAD),
		uploadProtocol);

	Common.setBuildEnvironmentVariable(contribEnv, confdesc, Const.ENV_KEY_JANTJE_PACKAGE_ID, getPackage());
	Common.setBuildEnvironmentVariable(contribEnv, confdesc, Const.ENV_KEY_JANTJE_ARCITECTURE_ID,
		getArchitecture());
	Common.setBuildEnvironmentVariable(contribEnv, confdesc, Const.ENV_KEY_JANTJE_BOARD_ID, getBoardID());

	for (LabelCombo curLabelCombo : this.mBoardOptionCombos) {
	    curLabelCombo.StoreValue(confdesc);
	}
	saveAllLastUseds();

    }

    private void setValues(ICConfigurationDescription confdesc) {
	String boardFile = InstancePreferences.getLastUsedBoardsFile();
	String boardName = InstancePreferences.getLastUsedArduinoBoardName();
	String uploadPort = InstancePreferences.getLastUsedUploadPort();
	String uploadProtocol = InstancePreferences.getLastUsedUploadProtocol();
	if (confdesc != null) {
	    boardFile = Common.getBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_BOARDS_FILE, boardFile);
	    boardName = Common.getBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_BOARD_NAME, boardName);
	    uploadPort = Common.getBuildEnvironmentVariable(confdesc, Const.ENV_KEY_JANTJE_COM_PORT, uploadPort);
	    uploadProtocol = Common.getBuildEnvironmentVariable(confdesc,
		    Const.get_Jantje_KEY_PROTOCOL(Const.ACTION_UPLOAD), uploadProtocol);
	}

	this.mControlBoardsTxtFile.setText(boardFile);
	// if no boards file is selected select the first
	if (this.mControlBoardsTxtFile.getText().isEmpty()) {
	    this.mControlBoardsTxtFile.setText(this.mControlBoardsTxtFile.getItem(0));
	    boardFile = this.mControlBoardsTxtFile.getText();
	}
	int selectedBoardFile = this.mControlBoardsTxtFile.getSelectionIndex();
	this.mcontrolBoardName.setItems(this.mAllBoardsFiles[selectedBoardFile].getAllNames());
	this.mcontrolBoardName.setText(boardName);

	String CurrentUploadProtocol = BoardSelectionPage.this.mControlUploadProtocol.getText();
	BoardSelectionPage.this.mControlUploadProtocol.removeAll();
	BoardSelectionPage.this.mControlUploadProtocol.setItems(Programmers.getUploadProtocols(boardFile));
	BoardSelectionPage.this.mControlUploadProtocol.setText(CurrentUploadProtocol);
	if (this.mControlUploadProtocol.getText().isEmpty()) {
	    this.mControlUploadProtocol.setText(uploadProtocol);
	    if (this.mControlUploadProtocol.getText().isEmpty()) {
		this.mControlUploadProtocol.setText(Const.DEFAULT);
	    }
	}

	this.mControlUploadPort.setValue(uploadPort);

	// set the options in the combo boxes before setting the value
	Map<String, String> options = InstancePreferences.getLastUsedMenuOption();
	for (LabelCombo curLabelCombo : this.mBoardOptionCombos) {
	    curLabelCombo.setItems(
		    this.mAllBoardsFiles[selectedBoardFile].getMenuItemNames(curLabelCombo.getMenuName(), boardName));
	    if (confdesc != null) {
		curLabelCombo.getStoredValue(confdesc);
	    } else {
		String value = options.get(curLabelCombo.getMenuName());
		if (value != null) {
		    curLabelCombo.setValue(value);
		}
	    }
	}
    }

    public IPath getPlatformFolder() {
	return new Path(new File(this.mControlBoardsTxtFile.getText().trim()).getParent());
    }

    @Override
    protected void performOK() {
	saveAllLastUseds();
	doOK();
	super.performOK();
    }

    @Override
    protected void performApply(ICResourceDescription src, ICResourceDescription dst) {
	saveAllLastUseds();
	doOK();
    }

    private void doOK() {
	ICConfigurationDescription confdesc = getConfdesc();
	if (confdesc != null) {
	    saveAllSelections(confdesc);
	    IProject project = confdesc.getProjectDescription().getProject();

	    Helpers.setTheEnvironmentVariables(project, confdesc, false);

	    try {
		Helpers.addArduinoCodeToProject(project, confdesc);
	    } catch (CoreException e1) {
		Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, Messages.error_adding_arduino_code, e1));
	    }
	    Helpers.removeInvalidIncludeFolders(confdesc);
	    Helpers.setDirtyFlag(project, confdesc);
	}
    }

    private class ArduinoSelectionPageListener implements Listener {
	private AbstractPage myPage;

	ArduinoSelectionPageListener(AbstractPage page) {
	    this.myPage = page;
	}

	@Override
	public void handleEvent(Event event) {
	    this.myPage.setValid(isPageComplete());
	}
    }

    @Override
    public void handleTabEvent(int kind, Object data) {
	if (kind == 222) {
	    this.mFeedbackControl.addListener(SWT.Modify, new ArduinoSelectionPageListener((AbstractPage) data));
	}
	super.handleTabEvent(kind, data);
    }

    /*
     * Returns the package name based on the platformfile name Caters for the
     * packages (with version number and for the old way
     */
    public String getPackage() {
	IPath platformFile = new Path(this.mControlBoardsTxtFile.getText().trim());
	String architecture = platformFile.removeLastSegments(1).lastSegment();
	if (architecture.contains(Const.DOT)) { // This is a version number so
						// package
	    return platformFile.removeLastSegments(4).lastSegment();
	}
	return platformFile.removeLastSegments(2).lastSegment();
    }

    /*
     * Returns the architecture based on the platfor file name Caters for the
     * packages (with version number and for the old way
     */
    public String getArchitecture() {
	IPath platformFile = new Path(this.mControlBoardsTxtFile.getText().trim());
	String architecture = platformFile.removeLastSegments(1).lastSegment();
	if (architecture.contains(Const.DOT)) { // This is a version number so
						// package
	    architecture = platformFile.removeLastSegments(2).lastSegment();
	}
	return architecture;
    }

    public String getBoardID() {
	int selectedBoardFile = this.mControlBoardsTxtFile.getSelectionIndex();
	String boardName = this.mcontrolBoardName.getText().trim();
	return this.mAllBoardsFiles[selectedBoardFile].getIDFromName(boardName);
    }
}
