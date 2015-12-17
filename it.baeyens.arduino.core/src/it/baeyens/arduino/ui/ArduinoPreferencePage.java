package it.baeyens.arduino.ui;

import java.io.IOException;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.PathEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ExternalCommandLauncher;

/**
 * ArduinoPreferencePage is the class that is behind the preference page of arduino that allows you to select the arduino path and the library path
 * and a option to use disable RXTX <br/>
 * Note that this class uses 2 technologies to change values (the flag and the path). <br/>
 * 
 * 
 * @author Jan Baeyens
 * 
 */
public class ArduinoPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private PathEditor arduinoPrivateLibPath;
    private PathEditor arduinoPrivateHardwarePath;
    private ComboFieldEditor mArduinoBuildBeforeUploadOption;
    private boolean mIsDirty = false;
    private org.eclipse.swt.graphics.Color redColor = null;
    private org.eclipse.swt.graphics.Color greenColor = null;
    private Label myMakeOKText;
    boolean myIsMakeInstalled;
    private String myInfoMessage = ""; //$NON-NLS-1$

    /**
     * PropertyChange set the flag mIsDirty to false. <br/>
     * This is needed because the default PerformOK saves all fields in the object store. Therefore I set the mIsDirty flag to true as soon as a field
     * gets change. Then I use this flag in the PerformOK to decide to call the super performOK or not.
     * 
     * @author Jan Baeyens
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
	this.mIsDirty = true;
	testStatus();
    }

    public ArduinoPreferencePage() {
	super(org.eclipse.jface.preference.FieldEditorPreferencePage.GRID);
	setDescription("Arduino Settings for this workspace");
	setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, ArduinoConst.NODE_ARDUINO));
    }

    @Override
    public boolean isValid() {
	return testStatus();
    }

    @Override
    public boolean okToLeave() {
	return testStatus();
    }

    private boolean showError() {
	String FullDialogMessage = this.myInfoMessage + "\nAre you sure about this setup";
	FullDialogMessage = FullDialogMessage + "\n\nYes:    continue and ignore the warning\nNo: do not continue";
	MessageBox dialog = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.NO);
	dialog.setText("Warnings about your setup");
	dialog.setMessage(FullDialogMessage);
	int ret = dialog.open();
	if (ret == SWT.NO) {
	    return false;
	}
	return true;
    }

    /**
     * PerformOK is done when the end users presses OK on a preference page. The order of the execution of the performOK is undefined. This method
     * saves the path variables based on the settings and removes the last used setting.<br/>
     * 
     * @see propertyChange
     * 
     * @see createFieldEditors
     * 
     * @author Jan Baeyens
     * 
     */
    @Override
    public boolean performOk() {
	if (!this.mIsDirty) {
	    return true;
	}
	if (!testStatus()) {
	    return false;
	}

	if (this.myInfoMessage.length() > 1) {
	    if (!showError()) {
		return false;
	    }
	}

	super.performOk();

	// reset the previous selected values
	String empty = "";//$NON-NLS-1$
	ArduinoInstancePreferences.SetLastUsedArduinoBoard(empty);
	ArduinoInstancePreferences.SetLastUsedUploadPort(empty);
	ArduinoInstancePreferences.setLastUsedBoardsFile(empty);
	ArduinoInstancePreferences.setLastUsedMenuOption(empty);
	return true;
    }

    // /**
    // * This method sets the eclipse path variables to contain the important Arduino folders (code wise that is)
    // *
    // *
    // * The arduino library location in the root folder (used when importing arduino libraries) The Private library path (used when importing private
    // * libraries) The Arduino IDE root folder
    // *
    // *
    // */
    // private void setWorkSpacePathVariables() {
    //
    // IWorkspace workspace = ResourcesPlugin.getWorkspace();
    // IPathVariableManager pathMan = workspace.getPathVariableManager();
    //
    // try {
    // pathMan.setURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_PRIVATE_LIB, URIUtil.toURI(mArduinoPrivateLibPath.getStringValue()));
    // } catch (CoreException e) {
    // Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
    // "Failed to create the workspace path variables. The setup will not work properly", e));
    // e.printStackTrace();
    // }
    // }

    @Override
    public void init(IWorkbench workbench) {
	test_make_exe();
	// nothing to do
    }

    /**
     * createFieldEditors creates the fields to edit. <br/>
     * 
     * @author Jan Baeyens
     */
    @Override
    protected void createFieldEditors() {
	final Composite parent = getFieldEditorParent();

	this.arduinoPrivateLibPath = new PathEditor(ArduinoConst.KEY_PRIVATE_LIBRARY_PATH, "Private Library path",
		"Select a folder containing libraries", parent);
	addField(this.arduinoPrivateLibPath);

	this.arduinoPrivateHardwarePath = new PathEditor(ArduinoConst.KEY_PRIVATE_HARDWARE_PATH, "Private hardware path",
		"Select a folder containing hardware", parent);
	addField(this.arduinoPrivateHardwarePath);

	Dialog.applyDialogFont(parent);

	String[][] buildBeforeUploadOptions = new String[][] { { "Ask every upload", "ASK" }, { "Yes", "YES" }, { "No", "NO" } }; //$NON-NLS-2$ //$NON-NLS-4$ //$NON-NLS-6$
	this.mArduinoBuildBeforeUploadOption = new ComboFieldEditor(ArduinoConst.KEY_BUILD_BEFORE_UPLOAD_OPTION, "Build before upload?",
		buildBeforeUploadOptions, parent);
	addField(this.mArduinoBuildBeforeUploadOption);
	createLine(parent, 4);

	this.myMakeOKText = new Label(parent, SWT.LEFT);
	this.myMakeOKText.setText("Looking for make");
	this.myMakeOKText.setEnabled(true);
	this.myMakeOKText.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 4, 1));

	// myHelpMakeButton=new;
	Button myHelpMakeButton = new Button(parent, SWT.BUTTON1);
	myHelpMakeButton.setText("What is this make thing?");
	myHelpMakeButton.addSelectionListener(new SelectionListener() {

	    @Override
	    public void widgetSelected(SelectionEvent e) {
		switch (Platform.getOS()) {
		case Platform.OS_MACOSX:
		    Program.launch("http://eclipse.baeyens.it/make_mac.php"); //$NON-NLS-1$
		    break;
		case Platform.OS_WIN32:
		    Program.launch("https://www.youtube.com/watch?v=cspLbTqBi7k&feature=youtu.be"); //$NON-NLS-1$
		    break;
		default:
		    Program.launch("http://lmgtfy.com/?q=install+make+on+linux"); //$NON-NLS-1$
		    break;
		}
	    }

	    @Override
	    public void widgetDefaultSelected(SelectionEvent e) {
		// Needs to be implemented but I don't use it
	    }
	});

	Button myTestMakeButton = new Button(parent, SWT.BUTTON1);
	myTestMakeButton.setText("test if make can be found");
	myTestMakeButton.addSelectionListener(new SelectionListener() {

	    @Override
	    public void widgetSelected(SelectionEvent e) {
		test_make_exe();
		MessageBox messageBox = new MessageBox(getShell(), SWT.ICON_WARNING | SWT.ABORT | SWT.RETRY | SWT.IGNORE);

		if (ArduinoPreferencePage.this.myIsMakeInstalled) {
		    messageBox.setText("Success");
		    messageBox.setMessage("Make was found.");
		} else {
		    messageBox.setText("Warning");
		    messageBox.setMessage("Make is still missing.");
		}
		messageBox.open();
		testStatus();
	    }

	    @Override
	    public void widgetDefaultSelected(SelectionEvent e) {
		// Needs to be implemented but I don't use it
	    }
	});

	this.redColor = parent.getDisplay().getSystemColor(SWT.COLOR_RED);
	this.greenColor = parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN);
    }

    /**
     * testStatus test whether the provided information is OK. Here the code checks whether there is a hardware\arduino\board.txt file under the
     * provide path.
     * 
     * @return true if the provided info is OK; False if the provided info is not OK
     * 
     * @author Jan Baeyens
     * 
     */
    boolean testStatus() {
	String ErrorMessage = "";
	String Seperator = "";

	this.myMakeOKText.setForeground(this.myIsMakeInstalled ? this.greenColor : this.redColor);
	this.myMakeOKText.setText(this.myIsMakeInstalled ? "Make is found on your system." : "Make is not found on your system");

	// Validate the private lib path
	// Path folder = new Path(mArduinoPrivateLibPath.getStringValue());
	// if (!folder.toFile().canRead()) {
	// ErrorMessage += Seperator + "Private library folder is not correct!";
	// Seperator = "/n";
	// }
	// folder = new Path(mArduinoPrivateHardwarePath.getStringValue());
	// if (!folder.toFile().canRead()) {
	// ErrorMessage += Seperator + "Hardware library folder is not correct!";
	// Seperator = "/n";
	// }

	setErrorMessage(ErrorMessage);
	setValid(true);
	return true;
    }

    @Override
    protected void performApply() {
	this.mIsDirty = true;
	super.performApply();
    }

    private static void createLine(Composite parent, int ncol) {
	Label line = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.BOLD);
	GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
	gridData.horizontalSpan = ncol;
	line.setLayoutData(gridData);
    }

    void test_make_exe() {
	String command = "make -v"; //$NON-NLS-1$
	this.myIsMakeInstalled = false;
	ExternalCommandLauncher commandLauncher = new ExternalCommandLauncher(command);
	try {

	    if (commandLauncher.launch(null) != 0) {
		Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Make not found.\n" + command, null));
		return;
	    }
	} catch (IOException e) {
	    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Failed to run make\n" + command, e));
	    return;
	}
	this.myIsMakeInstalled = true;
    }

}
