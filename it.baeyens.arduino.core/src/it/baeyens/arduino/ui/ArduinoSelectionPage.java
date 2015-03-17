package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ArduinoBoards;
import it.baeyens.arduino.tools.ArduinoHelpers;
import it.baeyens.arduino.tools.ArduinoLibraries;

import java.io.File;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.parser.util.ArrayUtil;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICMultiConfigDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
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
    public Text feedbackControl;

    // GUI elements
    protected Combo mControlBoardsTxtFile;
    protected Combo mcontrolBoardName;
    protected Combo controlUploadPort;
    protected Combo controlUploadProtocol;
    protected LabelCombo[][] boardOptionCombos = null;
    private final int ncol = 2;
    private String mPreviousSelectedBoard = "";
    protected Listener mBoardSelectionChangedListener = null;

    // the properties to modify
    private String[] allBoardsFiles; // contains the boards.txt file names found
				     // for the current arduino environment
    ArduinoBoards boardsFiles[] = null; // contains the boards.txt content found
					// for the current arduino environment

    private boolean mValidAndComplete; // Is the form valid and completely
				       // filled in?

    @Override
    public void createControls(Composite parent, ICPropertyProvider provider) {
	super.createControls(parent, provider);
	draw(parent);
    }

    public void setListener(Listener BoardSelectionChangedListener) {
	mBoardSelectionChangedListener = BoardSelectionChangedListener;
    }

    /**
     * Changes the layout of the page to reflect the newly selected boards file
     * 
     * @author Jan Baeyens
     */
    private Listener boardTxtModifyListener = new Listener() {
	@Override
	public void handleEvent(Event e) {
	    int selectedBoardFile = mControlBoardsTxtFile.getSelectionIndex();

	    String CurrentBoard = mcontrolBoardName.getText();
	    mcontrolBoardName.removeAll();
	    mcontrolBoardName.setItems(boardsFiles[selectedBoardFile].GetArduinoBoards());
	    mcontrolBoardName.setText(CurrentBoard);
	    BoardModifyListener.handleEvent(null);
	}
    };

    /**
     * BoardModifyListener triggers the validate when the board gets changed
     * 
     * @author Jan Baeyens
     */
    protected Listener BoardModifyListener = new Listener() {
	@Override
	public void handleEvent(Event e) {
	    int selectedBoardFile = mControlBoardsTxtFile.getSelectionIndex();
	    String boardName = mcontrolBoardName.getText();
	    for (int curBoardFile = 0; curBoardFile < boardOptionCombos.length; curBoardFile++) {
		for (int curCombo = 0; curCombo < boardOptionCombos[curBoardFile].length; curCombo++) {
		    if (curBoardFile == selectedBoardFile) {
			String OptionName = (String) boardOptionCombos[curBoardFile][curCombo].mCombo.getData("Menu");
			String OldValue = boardOptionCombos[curBoardFile][curCombo].mCombo.getText();
			boardOptionCombos[curBoardFile][curCombo].mCombo.setItems(boardsFiles[curBoardFile].getMenuItemNames(OptionName, boardName));
			boardOptionCombos[curBoardFile][curCombo].mCombo.setText(OldValue);
		    } else {
			boardOptionCombos[curBoardFile][curCombo].mCombo.removeAll();
		    }
		}
	    }
	    EnableControls();
	    validatePage();
	}
    };

    /**
     * ValidationListener triggers the validate page only Use for all fields that have no children that are dependent on it
     * 
     * @author Jan Baeyens
     */
    protected Listener ValidationListener = new Listener() {
	@Override
	public void handleEvent(Event e) {
	    validatePage();
	}
    };

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
	GridLayout theGridLayout = new GridLayout();
	theGridLayout.numColumns = ncol;
	composite.setLayout(theGridLayout);

	GridData theGriddata;
	allBoardsFiles = ArduinoHelpers.getBoardsFiles();
	boardsFiles = new ArduinoBoards[allBoardsFiles.length];
	for (int currentBoardFile = 0; currentBoardFile < allBoardsFiles.length; currentBoardFile++) {
	    boardsFiles[currentBoardFile] = new ArduinoBoards(allBoardsFiles[currentBoardFile]);

	}

	switch (allBoardsFiles.length) {
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
	mControlBoardsTxtFile.setItems(allBoardsFiles);

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
	new Label(composite, SWT.None).setText("Uploading Protocol: ");
	controlUploadProtocol = new Combo(composite, SWT.BORDER);
	theGriddata = new GridData();
	theGriddata.horizontalAlignment = SWT.FILL;
	theGriddata.horizontalSpan = (ncol - 1);
	controlUploadProtocol.setLayoutData(theGriddata);
	controlUploadProtocol.setEnabled(false);

	controlUploadProtocol.add(ArduinoConst.DEFAULT);

	// -----
	new Label(composite, SWT.None).setText("Port: ");
	controlUploadPort = new Combo(composite, SWT.BORDER);
	theGriddata = new GridData();
	theGriddata.horizontalAlignment = SWT.FILL;
	theGriddata.horizontalSpan = (ncol - 1);
	controlUploadPort.setLayoutData(theGriddata);
	controlUploadPort.setEnabled(false);

	controlUploadPort.setItems(ArrayUtil.addAll(activator.bonjourDiscovery.getList(), Common.listComPorts()));

	createLine(composite, ncol);
	boardOptionCombos = new LabelCombo[boardsFiles.length][];
	for (int curBoardsFile = 0; curBoardsFile < boardsFiles.length; curBoardsFile++) {
	    String[] optionNames = boardsFiles[curBoardsFile].getMenuNames();
	    boardOptionCombos[curBoardsFile] = new LabelCombo[optionNames.length];
	    for (int currentOption = 0; currentOption < optionNames.length; currentOption++) {
		boardOptionCombos[curBoardsFile][currentOption] = new LabelCombo(composite, optionNames[currentOption], ncol - 1, ValidationListener);
	    }
	}

	// Create the control to alert parents of changes
	feedbackControl = new Text(composite, SWT.None);
	feedbackControl.setVisible(false);
	feedbackControl.setEnabled(false);
	theGriddata = new GridData();
	theGriddata.horizontalSpan = 0;
	feedbackControl.setLayoutData(theGriddata);
	// End of special controls

	controlUploadPort.addListener(SWT.Modify, ValidationListener);
	controlUploadProtocol.addListener(SWT.Modify, ValidationListener);
	mcontrolBoardName.addListener(SWT.Modify, BoardModifyListener);
	mControlBoardsTxtFile.addListener(SWT.Modify, boardTxtModifyListener);

	// Set all values as we know them
	restoreAllSelections();
	EnableControls();
	Dialog.applyDialogFont(composite);
    }

    public boolean isPageComplete() {
	return mValidAndComplete;
    }

    protected void EnableControls() {
	mcontrolBoardName.setEnabled(true);
	controlUploadPort.setEnabled(true);
	controlUploadProtocol.setEnabled(true);
	if (page == null) {
	    mControlBoardsTxtFile.setEnabled((allBoardsFiles.length > 1));
	} else {
	    mControlBoardsTxtFile.setEnabled((allBoardsFiles.length > 1)
	    /* && (getResDesc().getConfiguration().getProjectDescription().getConfigurations().length < 2) */);
	}
	mControlBoardsTxtFile.setVisible(allBoardsFiles.length > 1);

	int selectedBoardFile = mControlBoardsTxtFile.getSelectionIndex();
	for (int curBoardFile = 0; curBoardFile < allBoardsFiles.length; curBoardFile++)
	    for (int curMenuCombo = 0; curMenuCombo < boardOptionCombos[curBoardFile].length; curMenuCombo++) {
		boolean visible1 = (curBoardFile == selectedBoardFile) && (boardOptionCombos[curBoardFile][curMenuCombo].mCombo.getItemCount() > 0);
		boardOptionCombos[curBoardFile][curMenuCombo].setVisible(visible1);
	    }

    }

    /**
     * This method sets the mValidAndComplete flag to true when all data is provided and valid. in all other cases mValidAndComplete is set to false.
     */
    protected void validatePage() {

	boolean MenuOpionsValidAndComplete = true;
	int selectedBoardFile = mControlBoardsTxtFile.getSelectionIndex();
	if (selectedBoardFile == -1) {
	    mValidAndComplete = false;
	} else {

	    for (int curCombo = 0; curCombo < boardOptionCombos[selectedBoardFile].length; curCombo++) {
		MenuOpionsValidAndComplete &= (!boardOptionCombos[selectedBoardFile][curCombo].mCombo.getText().isEmpty() || boardOptionCombos[selectedBoardFile][curCombo].mCombo
			.getItemCount() == 0);
	    }

	    mValidAndComplete = !mcontrolBoardName.getText().trim().isEmpty() && !controlUploadPort.getText().trim().isEmpty()
		    && MenuOpionsValidAndComplete;
	    feedbackControl.setText(mValidAndComplete ? "true" : "false");
	    // if (mValidAndComplete)
	    // saveAllSelections();
	}
	// tell other pages who listen in that the board has changed
	if (mBoardSelectionChangedListener != null) {
	    if (mValidAndComplete) {

		if (!mPreviousSelectedBoard.equals(mcontrolBoardName.getText())) {
		    mPreviousSelectedBoard = mcontrolBoardName.getText();
		    mBoardSelectionChangedListener.handleEvent(new Event());
		}
	    } else {
		if (!mPreviousSelectedBoard.isEmpty()) {
		    mBoardSelectionChangedListener.handleEvent(null);
		    mPreviousSelectedBoard = "";
		}
	    }
	} else {
	    mPreviousSelectedBoard = "";
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
	// nothing to do here

    }

    @Override
    protected void updateButtons() {
	// nothing to do here

    }

    private void saveAllSelections() {
	if (page != null) {
	    ICConfigurationDescription confdesc = getResDesc().getConfiguration();
	    if (confdesc instanceof ICMultiConfigDescription) {
		ICMultiConfigDescription multiConfDesc = (ICMultiConfigDescription) confdesc;
		ICConfigurationDescription confdescs[] = (ICConfigurationDescription[]) multiConfDesc.getItems();
		for (int curdesc = 0; curdesc < confdescs.length; curdesc++) {
		    saveAllSelections(confdescs[curdesc]);
		    ArduinoLibraries.reAttachLibrariesToProject(confdescs[curdesc]);
		}
	    } else {
		saveAllSelections(confdesc);
	    }
	    // ArduinoLibraries.reAttachLibrariesToProject(confdesc);
	}
    }

    public void saveAllSelections(ICConfigurationDescription confdesc) {
	int selectedBoardFile = mControlBoardsTxtFile.getSelectionIndex();
	String boardFile = mControlBoardsTxtFile.getText().trim();
	String boardName = mcontrolBoardName.getText().trim();
	String uploadPort = controlUploadPort.getText().trim();
	String uploadProg = controlUploadProtocol.getText().trim();
	ArduinoInstancePreferences.setLastUsedBoardsFile(boardFile);
	ArduinoInstancePreferences.SetLastUsedArduinoBoard(boardName);
	ArduinoInstancePreferences.SetLastUsedUploadPort(uploadPort);
	ArduinoInstancePreferences.SetLastUsedUploadProgrammer(uploadProg);
	ArduinoInstancePreferences.setLastUsedMenuOption(""); // TOFIX implement
							      // the options
	if (confdesc != null) {
	    IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	    IContributedEnvironment contribEnv = envManager.getContributedEnvironment();

	    // Set the path variables
	    IProject project = confdesc.getProjectDescription().getProject();
	    IPath platformPath = new Path(new File(mControlBoardsTxtFile.getText().trim()).getParent()).append(ArduinoConst.PLATFORM_FILE_NAME);

	    IEnvironmentVariable var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_BOARDS_FILE, boardFile);
	    contribEnv.addVariable(var, confdesc);
	    var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_BOARD_NAME, boardName);
	    contribEnv.addVariable(var, confdesc);
	    var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_COM_PORT, uploadPort);
	    contribEnv.addVariable(var, confdesc);
	    var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_COM_PROG, uploadProg);
	    contribEnv.addVariable(var, confdesc);

	    for (int curBoardFile = 0; curBoardFile < allBoardsFiles.length; curBoardFile++) {
		for (int curCombo = 0; curCombo < boardOptionCombos[curBoardFile].length; curCombo++) {
		    String OptionName = (String) boardOptionCombos[curBoardFile][curCombo].mCombo.getData("Menu");
		    var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_START + OptionName, "");
		    contribEnv.addVariable(var, confdesc);
		}
	    }

	    for (int curCombo = 0; curCombo < boardOptionCombos[selectedBoardFile].length; curCombo++) {
		String OptionName = (String) boardOptionCombos[selectedBoardFile][curCombo].mCombo.getData("Menu");
		String optionValue = boardOptionCombos[selectedBoardFile][curCombo].mCombo.getText();
		var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_START + OptionName, optionValue);
		contribEnv.addVariable(var, confdesc);

	    }

	    // below are calculated values
	    var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_PLATFORM_FILE, platformPath.toString());
	    contribEnv.addVariable(var, confdesc);

	    ArduinoHelpers.setProjectPathVariables(project, platformPath.removeLastSegments(1));
	    ArduinoHelpers.setTheEnvironmentVariables(project, confdesc, false);

	    try {
		ArduinoHelpers.addArduinoCodeToProject(project, confdesc);
	    } catch (CoreException e1) {
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Error adding the arduino code", e1));
	    }
	    ArduinoHelpers.removeInvalidIncludeFolders(confdesc);

	    ArduinoHelpers.setDirtyFlag(project, confdesc);

	}

    }

    private void restoreAllSelections() {
	String boardFile = ArduinoInstancePreferences.getLastUsedBoardsFile();
	String boardName = ArduinoInstancePreferences.getLastUsedArduinoBoardName();
	String uploadPort = ArduinoInstancePreferences.getLastUsedUploadPort();
	String uploadProtocol = ArduinoInstancePreferences.getLastUsedUploadProgrammer();

	if (page != null) {

	    ICConfigurationDescription confdesc = getResDesc().getConfiguration();
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
	mcontrolBoardName.setItems(boardsFiles[selectedBoardFile].GetArduinoBoards());
	mcontrolBoardName.setText(boardName);
	BoardModifyListener.handleEvent(null);
	controlUploadPort.setText(uploadPort);
	controlUploadProtocol.setText(uploadProtocol);

	if (page != null) {
	    for (int curCombo = 0; curCombo < boardOptionCombos[selectedBoardFile].length; curCombo++) {
		String optionName = (String) boardOptionCombos[selectedBoardFile][curCombo].mCombo.getData("Menu");
		String optionValue = Common.getBuildEnvironmentVariable(getResDesc().getConfiguration(), ArduinoConst.ENV_KEY_JANTJE_START
			+ optionName, "", true);
		boardOptionCombos[selectedBoardFile][curCombo].mCombo.setText(optionValue);
	    }
	}

    }

    public IPath getPlatformFolder() {
	return new Path(new File(mControlBoardsTxtFile.getText().trim()).getParent());
    }

    @Override
    protected void performOK() {
	saveAllSelections();
	super.performOK();
    }

    @Override
    protected void performApply(ICResourceDescription src, ICResourceDescription dst) {
	saveAllSelections();
    }

}
