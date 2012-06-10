package it.baeyens.arduino.ui;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.tools.ArduinoHelpers;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * ArduinoPreferencePage is the class that is behind the preference page of
 * arduino that allows you to select the arduino path and the option to use the
 * arduino IDE settings <br/>
 * Note that this class uses 2 technologies to change values (the flag and the
 * path). This should be changed some day to only using the field editor.<br/>
 * The arduinoPath and the useIDETools are set at configuration level.<br/>
 * The paths are set in the avr-eclipse preference store<br/>
 * 
 * @author Jan Baeyens
 * 
 */
public class ArduinoPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private BooleanFieldEditor mUseArduinoIdeTools;
	private StringFieldEditor mArduinoIdeVersion;
	private DirectoryFieldEditor mArduinoIdePath;
	private boolean mIsDirty = false;
	private IPath mPrefBoardFile = null;


	/**
	 * PropertyChange set the flag mIsDirty to false. <br/>
	 * This is needed because the default PerformOK saves all fields in the
	 * object store. As I use the same objectstore as the avreclipse I need to
	 * make sure the performOK will not overwrite settings it should not.
	 * Therefore I set the mIsDirty flag to true as soon as a field gets change.
	 * Then I use this flag in the PerformOK to decide to call the super
	 * performOK or not.
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

	/**
	 * PerformOK is done when the end users presses OK on a preference page. The
	 * order of the execution of the performOK is undefined. This method saves
	 * the Avrdude preferences when the use arduino IDE tools flag is set.<br/>
	 * This code also saves the Arduino path
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
		super.performOk();
		if (mUseArduinoIdeTools.getBooleanValue()) {
			ArduinoHelpers.ConfigureToUseArduinoIDETools();
		}
		ArduinoHelpers.SetPathVariables();
		return true;
	}

	@Override
	public void init(IWorkbench workbench) {
		// nothing to do
	}

	/**
	 * createFieldEditors creates the fields to edit. <br/>
	 * Due to lack of knowledge of the author the path and brows button are
	 * "hard coded" therefore the perform OK needs to save the path.
	 * 
	 * @author Jan Baeyens
	 */
	@Override
	protected void createFieldEditors() {
		final Composite parent = getFieldEditorParent();

		mArduinoIdePath=new DirectoryFieldEditor(ArduinoConst.KEY_ARDUINOPATH, "Arduino IDE path", parent);
		addField(mArduinoIdePath );
		addField( new DirectoryFieldEditor(ArduinoConst.KEY_PRIVATE_LIBRARY_PATH, "Private Library path", parent));
		
		Dialog.applyDialogFont(parent);

		mUseArduinoIdeTools = new BooleanFieldEditor(ArduinoConst.KEY_USE_ARDUINO_IDE_TOOLS, "Use Arduino IDE tools in eclipse", parent);
		addField(mUseArduinoIdeTools);
		addField(new BooleanFieldEditor(ArduinoConst.KEY_RXTXDISABLED, "Disable RXTX (disables Arduino reset during upload and the serial monitor)", parent));
		mArduinoIdeVersion = new StringFieldEditor(ArduinoConst.KEY_ARDUINO_IDE_VERSION, "Arduino IDE Version", parent);
		addField(mArduinoIdeVersion);
		mArduinoIdeVersion.setEnabled(false, parent);

	}

	/**
	 * testStatus test whether the provided information is OK. Here the code
	 * checks whether there is a hardware\arduino\board.txt file under the
	 * provide path.
	 * 
	 * @return true if the provided info is OK; False if the provided info is
	 *         not OK
	 * 
	 * @author Jan Baeyens
	 * 
	 */
	private boolean testStatus() {
		// Path BoardFile = new Path(fPathText.getText());
		Path BoardFile = new Path(mArduinoIdePath.getStringValue());

		File file = BoardFile.append(ArduinoConst.BOARDS_FILE_SUFFIX).toFile();
		boolean IsValid = file.canRead();
		if (IsValid) {
			mArduinoIdeVersion.setStringValue(GetIDEVersion());
			setErrorMessage(null);
		} else {
			setErrorMessage("Folder is not correct");
		}
		setValid(IsValid);
		return IsValid;
	}

	/**
	 * Reads the version number from the lib/version.txt file
	 * 
	 * @return the version number if found if no version number found the error returned by the file read method
	 */
	private String GetIDEVersion() {
		Path BoardFile = new Path(mArduinoIdePath.getStringValue());
		if (BoardFile.equals(mPrefBoardFile)) {
			return mArduinoIdeVersion.getStringValue();
		}
		mPrefBoardFile = BoardFile;

		File file = BoardFile.append(ArduinoConst.LIB_FILE_SUFFIX).toFile();
		try {
			// Open the file that is the first
			// command line parameter
			FileInputStream fstream = new FileInputStream(file);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine = br.readLine();
			in.close();
			return strLine;
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
			return e.getMessage();
		}
	}

}
