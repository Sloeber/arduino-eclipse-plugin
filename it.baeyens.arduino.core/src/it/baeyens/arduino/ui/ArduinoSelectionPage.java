package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.ide.connector.ArduinoGetPreferences;
import it.baeyens.arduino.tools.ArduinoBoards;
import it.baeyens.arduino.tools.ArduinoHelpers;

import java.io.File;
import java.util.Arrays;

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

/**
 * The ArduinoSelectionPage class is used in the new wizard and the project properties. This class controls the gui and the data underneath the gui.
 * This class allows to select the arduino board and the port name
 * 
 * @author Jan Baeyens
 * @see ArduinoProperties ArduinoSettingsPage
 * 
 */
public class ArduinoSelectionPage extends AbstractCPropertyTab {
    // global stuff to allow to communicate outside this class
    public Text mFeedbackControl;

    // GUI elements
    protected Combo mControlBoardsTxtFile;
    protected Combo mcontrolBoardName;
    protected LabelCombo mControlUploadPort;
    protected LabelCombo mControlUploadProtocol;
    protected LabelCombo[] mBoardOptionCombos = null;
    private final int ncol = 2;
    protected Listener mBoardSelectionChangedListener = null;

    // the properties to modify
    private String[] mAllBoardsFileNames; // contains the boards.txt file names found
					  // for the current arduino environment
    ArduinoBoards mAllBoardsFiles[] = null; // contains the boards.txt content found
					    // for the current arduino environment

    /**
     * Get the configuration we are currently working in. The configuration is null if we are in the create sketch wizard.
     * 
     * @return the configuration to save info into
     */
    public ICConfigurationDescription getConfdesc() {
	if (page != null) {
	    return getResDesc().getConfiguration();
	}
	return null;
    }

    /**
     * Listener for the child or leave fields. The listener saves the information in the configuration
     * 
     * @author jan
     *
     */
    final class childFieldListener implements Listener {

	@Override
	public void handleEvent(Event e) {
	    ICConfigurationDescription confdesc = getConfdesc();
	    if (confdesc != null) {
		myCombo.StoreValue(confdesc);
	    }
	    isPageComplete();
	}

	private LabelCombo myCombo;

	public void setInfo(LabelCombo Combo) {

	    myCombo = Combo;
	}
    }

    private Listener boardFileModifyListener = new Listener() {
	@Override
	public void handleEvent(Event e) {
	    IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	    IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
	    ICConfigurationDescription confdesc = getConfdesc();

	    int selectedBoardFile = mControlBoardsTxtFile.getSelectionIndex();
	    String boardFile = mControlBoardsTxtFile.getText().trim();
	    if (confdesc != null) {
		IEnvironmentVariable var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_BOARDS_FILE, boardFile);
		contribEnv.addVariable(var, confdesc);
		IPath platformPath = new Path(new File(boardFile).getParent()).append(ArduinoConst.PLATFORM_FILE_NAME);
		var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_PLATFORM_FILE, platformPath.toString());
		contribEnv.addVariable(var, confdesc);
	    }

	    /*
	     * Change the list of available boards
	     */
	    String CurrentBoard = mcontrolBoardName.getText();
	    mcontrolBoardName.removeAll();
	    mcontrolBoardName.setItems(mAllBoardsFiles[selectedBoardFile].GetArduinoBoards());
	    mcontrolBoardName.setText(CurrentBoard);

	    BoardModifyListener.handleEvent(null);
	}
    };

    protected Listener BoardModifyListener = new Listener() {
	@Override
	public void handleEvent(Event e) {

	    int selectedBoardFile = mControlBoardsTxtFile.getSelectionIndex();
	    String boardName = mcontrolBoardName.getText();

	    for (LabelCombo curLabelCombo : mBoardOptionCombos) {
		curLabelCombo.setItems(mAllBoardsFiles[selectedBoardFile].getMenuItemNames(curLabelCombo.getMenuName(), boardName));
	    }

	    IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	    IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
	    ICConfigurationDescription confdesc = getConfdesc();
	    if (confdesc != null) {
		IEnvironmentVariable var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_BOARD_NAME, boardName);
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
	mBoardSelectionChangedListener = BoardSelectionChangedListener;
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
	theGridLayout.numColumns = ncol;
	composite.setLayout(theGridLayout);

	GridData theGriddata;
	mAllBoardsFileNames = ArduinoHelpers.getBoardsFiles();
	Arrays.sort(mAllBoardsFileNames);
	mAllBoardsFiles = new ArduinoBoards[mAllBoardsFileNames.length];
	for (int currentBoardFile = 0; currentBoardFile < mAllBoardsFileNames.length; currentBoardFile++) {
	    mAllBoardsFiles[currentBoardFile] = new ArduinoBoards(mAllBoardsFileNames[currentBoardFile]);

	}

	switch (mAllBoardsFileNames.length) {
	case 0:
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "No platform files found: check your Arduino preferences ", null));
	    break;
	case 1: {
	    break;
	}
	default: {
	    // create a combo to select the boards
	    createLabel(composite, ncol, "The boards.txt file you want to use"); //$NON-NLS-1$
	    new Label(composite, SWT.NONE).setText("Boards.txt file:"); //$NON-NLS-1$
	}

	}

	mControlBoardsTxtFile = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
	theGriddata = new GridData();
	theGriddata.horizontalAlignment = SWT.FILL;
	theGriddata.horizontalSpan = (ncol - 1);
	mControlBoardsTxtFile.setLayoutData(theGriddata);
	mControlBoardsTxtFile.setEnabled(false);
	mControlBoardsTxtFile.setItems(mAllBoardsFileNames);

	createLine(composite, ncol);
	// -------

	// ------
	createLabel(composite, ncol, "Your Arduino board specifications"); //$NON-NLS-1$
	new Label(composite, SWT.NONE).setText("Board:"); //$NON-NLS-1$
	mcontrolBoardName = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
	theGriddata = new GridData();
	theGriddata.horizontalAlignment = SWT.FILL;
	theGriddata.horizontalSpan = (ncol - 1);
	mcontrolBoardName.setLayoutData(theGriddata);
	mcontrolBoardName.setEnabled(false);

	// ----

	mControlUploadProtocol = new LabelCombo(composite, "Uploading Protocol: ", ncol - 1, ArduinoConst.ENV_KEY_JANTJE_COM_PROG, false);

	mControlUploadProtocol.add(ArduinoConst.DEFAULT);

	// -----
	mControlUploadPort = new LabelCombo(composite, "Port: ", ncol - 1, ArduinoConst.ENV_KEY_JANTJE_COM_PORT, false);

	mControlUploadPort.setItems(ArrayUtil.addAll(activator.bonjourDiscovery.getList(), Common.listComPorts()));

	createLine(composite, ncol);

	String[] menuNames = new String[30];
	for (int curBoardsFile = 0; curBoardsFile < mAllBoardsFiles.length; curBoardsFile++) {
	    ArrayUtil.addAll(menuNames, mAllBoardsFiles[curBoardsFile].getMenuNames());
	}
	menuNames = ArrayUtil.removeDuplicates(menuNames);
	mBoardOptionCombos = new LabelCombo[menuNames.length];
	for (int currentOption = 0; currentOption < menuNames.length; currentOption++) {
	    String menuName = menuNames[currentOption];
	    mBoardOptionCombos[currentOption] = new LabelCombo(composite, menuName, ncol - 1, ArduinoConst.ENV_KEY_JANTJE_START + menuName, true);
	}

	// Create the control to alert parents of changes
	mFeedbackControl = new Text(composite, SWT.None);
	mFeedbackControl.setVisible(false);
	mFeedbackControl.setEnabled(false);
	theGriddata = new GridData();
	theGriddata.horizontalSpan = 0;
	mFeedbackControl.setLayoutData(theGriddata);
	// End of special controls

	setValues(confdesc);

	// enable the listeners
	childFieldListener controlUploadPortlistener = new childFieldListener();
	controlUploadPortlistener.setInfo(mControlUploadPort);
	mControlUploadPort.addListener(controlUploadPortlistener);
	childFieldListener controlUploadProtocollistener = new childFieldListener();
	controlUploadProtocollistener.setInfo(mControlUploadProtocol);
	mControlUploadProtocol.addListener(controlUploadProtocollistener);
	mcontrolBoardName.addListener(SWT.Modify, BoardModifyListener);
	mControlBoardsTxtFile.addListener(SWT.Modify, boardFileModifyListener);

	for (LabelCombo curLabelCombo : mBoardOptionCombos) {
	    childFieldListener comboboxModifyListener = new childFieldListener();
	    comboboxModifyListener.setInfo(curLabelCombo);
	    curLabelCombo.addListener(comboboxModifyListener);
	    // ComboboxModifyListener comboboxModifyListener = new ComboboxModifyListener();
	    // comboboxModifyListener.setLabelCombo(boardOptionCombos[curBoardsFile][currentOption]);
	    // boardOptionCombos[curBoardsFile][currentOption].addListener(comboboxModifyListener);
	}

	EnableControls();
	Dialog.applyDialogFont(composite);
    }

    public boolean isPageComplete() {

	boolean MenuOpionsValidAndComplete = true;
	boolean ret = true;
	int selectedBoardFile = mControlBoardsTxtFile.getSelectionIndex();
	if (selectedBoardFile == -1)
	    return false;

	for (LabelCombo curLabelCombo : mBoardOptionCombos) {
	    MenuOpionsValidAndComplete = MenuOpionsValidAndComplete && curLabelCombo.isValid();
	}

	ret = !mcontrolBoardName.getText().trim().isEmpty() && mControlUploadProtocol.isValid() && MenuOpionsValidAndComplete;
	if (!mFeedbackControl.getText().equals(ret ? "true" : "false")) {
	    mFeedbackControl.setText(ret ? "true" : "false");
	}
	if (ret) {
	    if (mBoardSelectionChangedListener != null) {
		mBoardSelectionChangedListener.handleEvent(new Event());
	    }
	}

	return ret;
    }

    protected void EnableControls() {
	mcontrolBoardName.setEnabled(true);
	mControlUploadPort.setEnabled(true);
	mControlUploadProtocol.setEnabled(true);
	mControlBoardsTxtFile.setEnabled((mAllBoardsFileNames.length > 1));
	mControlBoardsTxtFile.setVisible(mAllBoardsFileNames.length > 1);
	for (LabelCombo curLabelCombo : mBoardOptionCombos) {
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

    private void saveAllLastUseds() {
	//
	String boardFile = mControlBoardsTxtFile.getText().trim();
	String boardName = mcontrolBoardName.getText().trim();
	String uploadPort = mControlUploadPort.getValue();
	String uploadProg = mControlUploadProtocol.getValue();
	ArduinoInstancePreferences.setLastUsedBoardsFile(boardFile);
	ArduinoInstancePreferences.SetLastUsedArduinoBoard(boardName);
	ArduinoInstancePreferences.SetLastUsedUploadPort(uploadPort);
	ArduinoInstancePreferences.SetLastUsedUploadProgrammer(uploadProg);
	ArduinoInstancePreferences.setLastUsedMenuOption(""); // TOFIX implement
							      // the options
    }

    public void saveAllSelections(ICConfigurationDescription confdesc) {
	String boardFile = mControlBoardsTxtFile.getText().trim();
	String boardName = mcontrolBoardName.getText().trim();
	String uploadPort = mControlUploadPort.getValue();
	String uploadProg = mControlUploadProtocol.getValue();
	IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	IContributedEnvironment contribEnv = envManager.getContributedEnvironment();

	// Set the path variables
	IPath platformPath = new Path(new File(mControlBoardsTxtFile.getText().trim()).getParent()).append(ArduinoConst.PLATFORM_FILE_NAME);
	Common.setBuildEnvironmentVariable(contribEnv, confdesc, ArduinoConst.ENV_KEY_JANTJE_BOARDS_FILE, boardFile);
	Common.setBuildEnvironmentVariable(contribEnv, confdesc, ArduinoConst.ENV_KEY_JANTJE_PLATFORM_FILE, platformPath.toString());
	Common.setBuildEnvironmentVariable(contribEnv, confdesc, ArduinoConst.ENV_KEY_JANTJE_BOARD_NAME, boardName);
	Common.setBuildEnvironmentVariable(contribEnv, confdesc, ArduinoConst.ENV_KEY_JANTJE_COM_PORT, uploadPort);
	Common.setBuildEnvironmentVariable(contribEnv, confdesc, ArduinoConst.ENV_KEY_JANTJE_COM_PROG, uploadProg);

	Common.setBuildEnvironmentVariable(contribEnv, confdesc, ArduinoConst.ENV_KEY_JANTJE_PACKAGE_ID, getPackage());
	Common.setBuildEnvironmentVariable(contribEnv, confdesc, ArduinoConst.ENV_KEY_JANTJE_ARCITECTURE_ID, getArchitecture());
	Common.setBuildEnvironmentVariable(contribEnv, confdesc, ArduinoConst.ENV_KEY_JANTJE_BOARD_ID, getBoardID());

	for (LabelCombo curLabelCombo : mBoardOptionCombos) {
	    curLabelCombo.StoreValue(confdesc);
	}

    }

    private void setValues(ICConfigurationDescription confdesc) {
	String boardFile = ArduinoInstancePreferences.getLastUsedBoardsFile();
	String boardName = ArduinoInstancePreferences.getLastUsedArduinoBoardName();
	String uploadPort = ArduinoInstancePreferences.getLastUsedUploadPort();
	String uploadProtocol = ArduinoInstancePreferences.getLastUsedUploadProgrammer();
	if (confdesc != null) {
	    boardFile = Common.getBuildEnvironmentVariable(confdesc, ArduinoConst.ENV_KEY_JANTJE_BOARDS_FILE, boardFile);
	    boardName = Common.getBuildEnvironmentVariable(confdesc, ArduinoConst.ENV_KEY_JANTJE_BOARD_NAME, boardName);
	    uploadPort = Common.getBuildEnvironmentVariable(confdesc, ArduinoConst.ENV_KEY_JANTJE_COM_PORT, uploadPort);
	    uploadProtocol = Common.getBuildEnvironmentVariable(confdesc, ArduinoConst.ENV_KEY_JANTJE_COM_PROG, uploadProtocol);
	}
	mControlBoardsTxtFile.setText(boardFile);
	// if no boards file is selected select the first
	if (mControlBoardsTxtFile.getText().isEmpty()) {
	    mControlBoardsTxtFile.setText(mControlBoardsTxtFile.getItem(0));
	}
	int selectedBoardFile = mControlBoardsTxtFile.getSelectionIndex();
	mcontrolBoardName.setItems(mAllBoardsFiles[selectedBoardFile].GetArduinoBoards());
	mcontrolBoardName.setText(boardName);
	// BoardModifyListener.handleEvent(null);
	mControlUploadPort.setValue(uploadPort);
	mControlUploadProtocol.setValue(uploadProtocol);

	// set the options in the combo boxes before setting the value
	for (LabelCombo curLabelCombo : mBoardOptionCombos) {
	    curLabelCombo.setItems(mAllBoardsFiles[selectedBoardFile].getMenuItemNames(curLabelCombo.getMenuName(), boardName));
	    if (confdesc != null) {
		curLabelCombo.getStoredValue(confdesc);

	    }
	}

    }

    public IPath getPlatformFolder() {
	return new Path(new File(mControlBoardsTxtFile.getText().trim()).getParent());
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

	    ArduinoGetPreferences.generateDumpFileForBoardIfNeeded(getPackage(), getArchitecture(), getBoardID(), null);
	    ArduinoHelpers.setTheEnvironmentVariables(project, confdesc, false);
	    ArduinoHelpers.setProjectPathVariables(confdesc);

	    try {
		ArduinoHelpers.addArduinoCodeToProject(project, confdesc);
	    } catch (CoreException e1) {
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Error adding the arduino code", e1));
	    }
	    ArduinoHelpers.removeInvalidIncludeFolders(confdesc);
	    ArduinoHelpers.setDirtyFlag(project, confdesc);
	}
    }

    private class ArduinoSelectionPageListener implements Listener {
	private AbstractPage myPage;

	ArduinoSelectionPageListener(AbstractPage page) {
	    myPage = page;
	}

	@Override
	public void handleEvent(Event event) {
	    myPage.setValid(isPageComplete());
	}
    }

    @Override
    public void handleTabEvent(int kind, Object data) {
	if (kind == 222) {
	    mFeedbackControl.addListener(SWT.Modify, new ArduinoSelectionPageListener((AbstractPage) data));
	}
	super.handleTabEvent(kind, data);
    }

    public String getPackage() {
	IPath platformFile = new Path(mControlBoardsTxtFile.getText().trim());
	String architecture = platformFile.removeLastSegments(1).lastSegment();
	if (architecture.contains(".")) {
	    return platformFile.removeLastSegments(4).lastSegment();
	}
	return platformFile.removeLastSegments(2).lastSegment();
    }

    public String getArchitecture() {
	// TODO Auto-generated method stub
	IPath platformFile = new Path(mControlBoardsTxtFile.getText().trim());
	String architecture = platformFile.removeLastSegments(1).lastSegment();
	if (architecture.contains(".")) {
	    architecture = platformFile.removeLastSegments(2).lastSegment();
	}
	return architecture;
    }

    public String getBoardID() {
	int selectedBoardFile = mControlBoardsTxtFile.getSelectionIndex();
	String boardName = mcontrolBoardName.getText().trim();
	return mAllBoardsFiles[selectedBoardFile].getBoardIDFromName(boardName);
    }
}
