package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.ide.connector.ArduinoGetPreferences;
import it.baeyens.arduino.tools.ArduinoHelpers;
import it.baeyens.arduino.tools.ExternalCommandLauncher;
import it.baeyens.arduino.tools.MyDirectoryFieldEditor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
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

    private StringFieldEditor mArduinoIdeVersion;
    private MyDirectoryFieldEditor mArduinoIdePath;
    private DirectoryFieldEditor mArduinoPrivateLibPath;
    private DirectoryFieldEditor mArduinoPrivateHardwarePath;
    private ComboFieldEditor mArduinoBuildBeforeUploadOption;
    private boolean mIsDirty = false;
    private IPath mPrefBoardFile = null;
    private org.eclipse.swt.graphics.Color redColor = null;
    private org.eclipse.swt.graphics.Color greenColor = null;
    private Label myArduinoVersionOKText;
    private Label myMakeOKText;
    private String myAdvisedArduinIDEVersion = "1.6.5 -newer versions will not work-";
    boolean myIsMakeInstalled;
    private String myInfoMessage = "";

    /**
     * PropertyChange set the flag mIsDirty to false. <br/>
     * This is needed because the default PerformOK saves all fields in the object store. Therefore I set the mIsDirty flag to true as soon as a field
     * gets change. Then I use this flag in the PerformOK to decide to call the super performOK or not.
     * 
     * @author Jan Baeyens
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
	mIsDirty = true;
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
	String FullDialogMessage = myInfoMessage + "\nAre you sure about this setup";
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
	if (!mIsDirty) {
	    return true;
	}
	if (!testStatus()) {
	    return false;
	}

	isIDESupported();

	if (myInfoMessage.length() > 1) {
	    if (!showError()) {
		return false;
	    }
	}

	super.performOk();
	mArduinoIdeVersion.store();
	makeOurOwnCustomBoards_txt();
	ArduinoGetPreferences.updateArduinoEnvironmentVariablesForAllProjectsIfNeeded();
	setWorkSpacePathVariables();

	// reset the previous selected values
	ArduinoInstancePreferences.SetLastUsedArduinoBoard("");
	ArduinoInstancePreferences.SetLastUsedUploadPort("");
	ArduinoInstancePreferences.setLastUsedBoardsFile("");
	ArduinoInstancePreferences.setLastUsedMenuOption("");
	return true;
    }

    /**
     * To be capable of overwriting the boards.txt and platform.txt file settings the plugin contains its own settings. The settings are arduino IDE
     * version specific and it seems to be relatively difficult to read a boards.txt located in the plugin itself (so outside of the workspace)
     * Therefore I copy the file during plugin configuration to the workspace root. The file is arduino IDE specific. If no specific file is found the
     * default is used. There are actually 4 txt files. 2 are for pre-processing 2 are for post processing. each time 1 board.txt an platform.txt I
     * probably do not need all of them but as I'm setting up this framework it seems best to add all possible combinations.
     * 
     */
    private void makeOurOwnCustomBoards_txt() {
	// TODO think about the A. and JANTJE. Maybe we do not want to add them
	// automatically
	// Actually I can not cleanup JANTJE properly so I can't do this
	IPath workspacePath = new Path(Common.getWorkspaceRoot().getAbsolutePath());
	makeOurOwnCustomBoard_txt("config/pre_processing_boards_-.txt", workspacePath.append(ArduinoConst.PRE_PROCESSING_BOARDS_TXT));
	makeOurOwnCustomBoard_txt("config/post_processing_boards_-.txt", workspacePath.append(ArduinoConst.POST_PROCESSING_BOARDS_TXT));
	makeOurOwnCustomBoard_txt("config/pre_processing_platform_-.txt", workspacePath.append(ArduinoConst.PRE_PROCESSING_PLATFORM_TXT));
	makeOurOwnCustomBoard_txt("config/post_processing_platform_-.txt", workspacePath.append(ArduinoConst.POST_PROCESSING_PLATFORM_TXT));
    }

    /**
     * This method creates a file in the root of the workspace based on a file delivered with the plugin The file can be arduino IDE version specific.
     * If no specific version is found the default is used.
     * 
     * @param inRegEx
     *            a string used to search for the version specific file. The $ is replaced by the arduino version or default
     * @param outFile
     *            the name of the file that will be created in the root of the workspace
     */
    private void makeOurOwnCustomBoard_txt(String inRegEx, IPath outFile) {
	String VersionSpecificFile = inRegEx.replaceFirst("-", mArduinoIdeVersion.getStringValue());
	String DefaultFile = inRegEx.replaceFirst("-", "default");
	/*
	 * Finding the file in the plugin as described here :http://blog.vogella.com/2010/07/06/reading-resources-from-plugin/
	 */

	byte[] buffer = new byte[4096]; // To hold file contents
	int bytes_read; // How many bytes in buffer

	try (FileOutputStream to = new FileOutputStream(outFile.toString());) {
	    try {
		URL specificUrl = new URL("platform:/plugin/it.baeyens.arduino.core/" + VersionSpecificFile);
		URL defaultUrl = new URL("platform:/plugin/it.baeyens.arduino.core/" + DefaultFile);

		try (InputStream inputStreamSpecific = specificUrl.openConnection().getInputStream();) {
		    while ((bytes_read = inputStreamSpecific.read(buffer)) != -1) {
			to.write(buffer, 0, bytes_read); // write
		    }
		} catch (IOException e) {
		    try (InputStream inputStreamDefault = defaultUrl.openConnection().getInputStream();) {
			while ((bytes_read = inputStreamDefault.read(buffer)) != -1) {
			    to.write(buffer, 0, bytes_read); // write
			}
		    } catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
			return;
		    }
		    return;
		}
	    } catch (MalformedURLException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	    }
	} catch (IOException e2) {
	    // TODO Auto-generated catch block
	    e2.printStackTrace();
	} // Create output stream

    }

    /**
     * This method sets the eclipse path variables to contain the important Arduino folders (code wise that is)
     * 
     * 
     * The arduino library location in the root folder (used when importing arduino libraries) The Private library path (used when importing private
     * libraries) The Arduino IDE root folder
     * 
     * 
     */
    private void setWorkSpacePathVariables() {

	IWorkspace workspace = ResourcesPlugin.getWorkspace();
	IPathVariableManager pathMan = workspace.getPathVariableManager();

	try {
	    IPath ArduinoIDEPath = Common.getArduinoIDEPathFromUserSelection(mArduinoIdePath.getStringValue());
	    pathMan.setURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_ARDUINO_LIB,
		    URIUtil.toURI(ArduinoIDEPath.append(ArduinoConst.LIBRARY_PATH_SUFFIX).toString()));
	    pathMan.setURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_PRIVATE_LIB, URIUtil.toURI(mArduinoPrivateLibPath.getStringValue()));
	    // the line below is added because eclipse seems to keep on using the old value.
	    pathMan.setURIValue("ArduinoPivateLibPath", URIUtil.toURI(mArduinoPrivateLibPath.getStringValue()));
	    pathMan.setURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_ARDUINO, URIUtil.toURI(ArduinoIDEPath));
	} catch (CoreException e) {
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
		    "Failed to create the workspace path variables. The setup will not work properly", e));
	    e.printStackTrace();
	}
    }

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

	mArduinoIdePath = new MyDirectoryFieldEditor(ArduinoConst.KEY_ARDUINOPATH, "Arduino IDE path", parent);

	addField(mArduinoIdePath.getfield());

	String LibPath = ArduinoInstancePreferences.getPrivateLibraryPath();
	if (LibPath.isEmpty()) {
	    String libraryPath = Common.getDefaultPrivateLibraryPath();
	    new File(libraryPath).mkdirs();
	    ArduinoInstancePreferences.setPrivateLibraryPath(libraryPath);
	}
	mArduinoPrivateLibPath = new DirectoryFieldEditor(ArduinoConst.KEY_PRIVATE_LIBRARY_PATH, "Private Library path", parent);
	addField(mArduinoPrivateLibPath);

	LibPath = ArduinoInstancePreferences.getPrivateHardwarePath();
	if (LibPath.isEmpty()) {
	    String hardwarePath = Common.getDefaultPrivateHardwarePath();
	    new File(hardwarePath).mkdirs();
	    ArduinoInstancePreferences.setPrivateHardwarePath(hardwarePath);
	}
	mArduinoPrivateHardwarePath = new DirectoryFieldEditor(ArduinoConst.KEY_PRIVATE_HARDWARE_PATH, "Private hardware path", parent);
	addField(mArduinoPrivateHardwarePath);

	Dialog.applyDialogFont(parent);

	mArduinoIdeVersion = new StringFieldEditor(ArduinoConst.KEY_ARDUINO_IDE_VERSION, "Arduino IDE Version", parent);
	addField(mArduinoIdeVersion);
	mArduinoIdeVersion.setEnabled(false, parent);

	String[][] buildBeforeUploadOptions = new String[][] { { "Ask every upload", "ASK" }, { "Yes", "YES" }, { "No", "NO" } };
	mArduinoBuildBeforeUploadOption = new ComboFieldEditor(ArduinoConst.KEY_BUILD_BEFORE_UPLOAD_OPTION, "Build before upload?",
		buildBeforeUploadOptions, parent);
	addField(mArduinoBuildBeforeUploadOption);
	createLine(parent, 4);

	myArduinoVersionOKText = new Label(parent, SWT.LEFT | SWT.BOLD);
	myArduinoVersionOKText.setText("Advised Arduino IDE version : " + myAdvisedArduinIDEVersion);
	myArduinoVersionOKText.setEnabled(true);
	myArduinoVersionOKText.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 4, 1));

	myMakeOKText = new Label(parent, SWT.LEFT);
	myMakeOKText.setText("Looking for make");
	myMakeOKText.setEnabled(true);
	myMakeOKText.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false, 4, 1));

	// myHelpMakeButton=new;
	Button myHelpMakeButton = new Button(parent, SWT.BUTTON1);
	myHelpMakeButton.setText("What is this make thing?");
	myHelpMakeButton.addSelectionListener(new SelectionListener() {

	    @Override
	    public void widgetSelected(SelectionEvent e) {
		switch (Platform.getOS()) {
		case Platform.OS_MACOSX:
		    Program.launch("http://eclipse.baeyens.it/make_mac.php");
		    break;
		case Platform.OS_WIN32:
		    Program.launch("https://www.youtube.com/watch?v=cspLbTqBi7k&feature=youtu.be");
		    break;
		default:
		    Program.launch("http://lmgtfy.com/?q=install+make+on+linux");
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

		if (myIsMakeInstalled) {
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

	redColor = parent.getDisplay().getSystemColor(SWT.COLOR_RED);
	greenColor = parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN);
    }

    /**
     * validates the version number of the arduino IDE and if the version number requires some extra info the info message contains that info
     * 
     * @param infoMessage
     *            extra information about the compatibility
     * @return
     */
    private boolean isIDESupported() {
	final String makeMissing = "\n!!!You need to install make!!!";
	myInfoMessage = "";
	if (mArduinoIdeVersion.getStringValue().compareTo("1.5.0") < 0) {
	    myInfoMessage = "This plugin is for Arduino IDE 1.5.x. \nPlease use V1 of the plugin for earlier versions.";
	    return false;
	}
	switch (mArduinoIdeVersion.getStringValue()) {
	case "1.5.0":
	case "1.5.1":
	case "1.5.2":
	case "1.5.3":
	case "1.5.4":
	case "1.5.5":
	case "1.5.6":
	case "1.5.6-r2":
	    myInfoMessage = "Arduino IDE " + mArduinoIdeVersion.getStringValue() + " is not supported.";
	    return false;
	case "1.5.7":
	case "1.5.8":
	case "1.6.0":
	    if (Platform.getOS().equals(Platform.OS_WIN32)) {
		myInfoMessage = "Arduino IDE " + mArduinoIdeVersion.getStringValue() + " Has serious issues on windows. THIS IS NOT SUPPORTED!!!!";
		if (!myIsMakeInstalled) {
		    myInfoMessage += makeMissing;
		}
		return false;
	    } else {
		return true;
	    }
	case "1.6.1":
	    return true;
	case "1.6.2":
	case "1.6.3":
	case "1.6.4":
	    myInfoMessage = "Arduino IDE " + mArduinoIdeVersion.getStringValue() + " only works with Teensy.";
	    if (!myIsMakeInstalled) {
		myInfoMessage += makeMissing;
		return false;
	    }
	    return true;
	case "1.6.5":
	case "1.6.5-r1":
	case "1.6.5-r2":
	case "1.6.5-r3":
	case "1.6.5-r4":
	case "1.6.5-r5":

	    return true;
	default:
	    myInfoMessage = "You are using a version of the Arduino IDE that is unknow or newer than available at the release of this plugin.";
	    myInfoMessage += "\nIf it is a newer released version please feed back usage results to Jantje.";
	    if (!myIsMakeInstalled) {
		myInfoMessage += makeMissing;
	    }
	    return false;
	}
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

	// Validate the arduino path
	IPath arduinoFolder = Common.getArduinoIDEPathFromUserSelection(mArduinoIdePath.getStringValue());
	File arduinoBoardFile = arduinoFolder.append(ArduinoConst.LIB_VERSION_FILE).toFile();
	boolean isArduinoFolderValid = arduinoBoardFile.canRead();
	if (isArduinoFolderValid) {
	    IPath BoardFile = Common.getArduinoIDEPathFromUserSelection(mArduinoIdePath.getStringValue());
	    if (!BoardFile.equals(mPrefBoardFile)) {
		mPrefBoardFile = BoardFile;
		mArduinoIdeVersion.setStringValue(ArduinoHelpers.GetIDEVersion(BoardFile));
	    }
	    if (isIDESupported()) {
		myArduinoVersionOKText.setForeground(greenColor);
		myArduinoVersionOKText.setText("ide is supported. Advised Arduino IDE version : " + myAdvisedArduinIDEVersion);

	    } else {
		myArduinoVersionOKText.setForeground(redColor);
		myArduinoVersionOKText.setText("ide not supported. Advised Arduino IDE version : " + myAdvisedArduinIDEVersion);

	    }

	} else {
	    ErrorMessage += Seperator + "Arduino folder is not correct!";
	    Seperator = "/n";
	    myArduinoVersionOKText.setText("Arduino folder is not correct! Advised Arduino IDE version : " + myAdvisedArduinIDEVersion);
	    myArduinoVersionOKText.setForeground(redColor);
	}
	myMakeOKText.setForeground(myIsMakeInstalled ? greenColor : redColor);
	myMakeOKText.setText(myIsMakeInstalled ? "Make is found on your system." : "Make is not found on your system");

	// Validate the private lib path
	Path PrivateLibFolder = new Path(mArduinoPrivateLibPath.getStringValue());
	boolean isArduinoPrivateLibFolderValid = PrivateLibFolder.toFile().canRead();
	if (!isArduinoPrivateLibFolderValid) {
	    ErrorMessage += Seperator + "Private library folder is not correct!";
	    Seperator = "/n";
	}

	// report status
	if (isArduinoFolderValid && isArduinoPrivateLibFolderValid) {
	    setErrorMessage(null);
	    setValid(true);
	    return true;
	}
	setErrorMessage(ErrorMessage);
	setValid(false);
	return false;
    }

    @Override
    protected void performApply() {
	mIsDirty = true;
	super.performApply();
    }

    private static void createLine(Composite parent, int ncol) {
	Label line = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.BOLD);
	GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
	gridData.horizontalSpan = ncol;
	line.setLayoutData(gridData);
    }

    void test_make_exe() {
	String command = "make -v";
	myIsMakeInstalled = false;
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
	myIsMakeInstalled = true;
    }

}
