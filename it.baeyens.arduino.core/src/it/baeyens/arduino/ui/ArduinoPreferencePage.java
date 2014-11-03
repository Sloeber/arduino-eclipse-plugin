package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ArduinoHelpers;
import it.baeyens.arduino.tools.MyDirectoryFieldEditor;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
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
    private boolean mIsDirty = false;
    private IPath mPrefBoardFile = null;

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

    private boolean showError(String dialogMessage) {
	String FullDialogMessage = dialogMessage + "\nPlease see <http://eclipse.baeyens.it/installAdvice.shtml> for more info.\n";
	FullDialogMessage = FullDialogMessage
		+ "Yes continue and ignore the warning\nNo do not continue and open install advice in browser\ncancel do not continue";
	MessageBox dialog = new MessageBox(getShell(), SWT.ICON_QUESTION | SWT.YES | SWT.CANCEL | SWT.NO);
	dialog.setText("Considerations about Arduino IDE compatibility");
	dialog.setMessage(FullDialogMessage);
	int ret = dialog.open();
	if (ret == SWT.CANCEL)
	    return false;
	if (ret == SWT.NO) {
	    boolean openedDialog = false;
	    try {
		URI uri = new URI("http://eclipse.baeyens.it/installAdvice.shtml");

		Desktop desktop = null;
		if (Desktop.isDesktopSupported()) {
		    desktop = Desktop.getDesktop();
		    desktop.browse(uri);
		    openedDialog = true;
		}
	    } catch (URISyntaxException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	    if (!openedDialog) {
		dialog = new MessageBox(getShell(), SWT.ICON_ERROR | SWT.OK);
		dialog.setText("A error occured!");
		dialog.setMessage("Failed to open browser!");
		dialog.open();
	    }
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
	if (!testStatus())
	    return false;
	if (!mIsDirty)
	    return true;

	if (mArduinoIdeVersion.getStringValue().compareTo("1.5.0") < 0) {
	    showError("This plugin is for Arduino IDE 1.5.x. \nPlease use V1 of the plugin for earlier versions.");
	    return false;
	}
	if (mArduinoIdeVersion.getStringValue().equals("1.5.3") || mArduinoIdeVersion.getStringValue().equals("1.5.4")) {
	    if (!showError("Arduino IDE 1.5.3 and 1.5.4 are not supported."))
		return false;
	}
	if (mArduinoIdeVersion.getStringValue().equals("1.5.5")) {
	    if (!showError("Arduino IDE 1.5.5 works but you need to adapt some libraries."))
		return false;
	}
	if (mArduinoIdeVersion.getStringValue().compareTo("1.5.5") > 0) {
	    if (!showError("You are using a version of the Arduino IDE that is newer than available at the release of this plugin."))
		return false;
	}

	super.performOk();
	setWorkSpacePathVariables();
	// reset the previous selected values
	ArduinoInstancePreferences.SetLastUsedArduinoBoard("");
	ArduinoInstancePreferences.SetLastUsedUploadPort("");
	ArduinoInstancePreferences.setLastUsedBoardsFile("");
	ArduinoInstancePreferences.setLastUsedMenuOption("");
	return true;
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

	    pathMan.setURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_ARDUINO_LIB,
		    URIUtil.toURI(new Path(mArduinoIdePath.getStringValue()).append(ArduinoConst.LIBRARY_PATH_SUFFIX).toString()));
	    pathMan.setURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_PRIVATE_LIB, URIUtil.toURI(mArduinoPrivateLibPath.getStringValue()));
	    pathMan.setURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_ARDUINO, URIUtil.toURI(mArduinoIdePath.getStringValue()));
	} catch (CoreException e) {
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
		    "Failed to create the workspace path variables. The setup will not work properly", e));
	    e.printStackTrace();
	}
    }

    @Override
    public void init(IWorkbench workbench) {
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

	mArduinoIdePath = new MyDirectoryFieldEditor(ArduinoConst.KEY_ARDUINOPATH, "Arduino IDE path", parent, Common.getArduinoIdeSuffix());

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

	addField(new BooleanFieldEditor(ArduinoConst.KEY_RXTXDISABLED, "Disable RXTX (disables Arduino reset during upload and the serial monitor)",
		parent));
	mArduinoIdeVersion = new StringFieldEditor(ArduinoConst.KEY_ARDUINO_IDE_VERSION, "Arduino IDE Version", parent);
	addField(mArduinoIdeVersion);
	mArduinoIdeVersion.setEnabled(false, parent);
	Button TestButton = new Button(parent, SWT.BUTTON1);
	TestButton.setText("test RXTX");
	TestButton.addSelectionListener(new SelectionListener() {

	    @Override
	    public void widgetSelected(SelectionEvent e) {
		Common.LoadRXTX(getShell());
	    }

	    @Override
	    public void widgetDefaultSelected(SelectionEvent e) {
		// Needs to be implemented but I don't use it
	    }
	});

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
    private boolean testStatus() {
	String ErrorMessage = "";
	String Seperator = "";

	// Validate the arduino path
	Path arduinoFolder = new Path(mArduinoIdePath.getStringValue());
	File arduinoBoardFile = arduinoFolder.append(ArduinoConst.LIB_VERSION_FILE).toFile();
	boolean isArduinoFolderValid = arduinoBoardFile.canRead();
	if (isArduinoFolderValid) {
	    Path BoardFile = new Path(mArduinoIdePath.getStringValue());
	    if (!BoardFile.equals(mPrefBoardFile)) {
		mPrefBoardFile = BoardFile;
		mArduinoIdeVersion.setStringValue(ArduinoHelpers.GetIDEVersion(BoardFile));
	    }
	} else {
	    ErrorMessage += Seperator + "Arduino folder is not correct!";
	    Seperator = "/n";
	}

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

}
