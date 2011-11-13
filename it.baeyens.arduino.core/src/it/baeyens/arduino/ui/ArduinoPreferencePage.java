package it.baeyens.arduino.ui;

import java.io.File;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.tools.ArduinoPreferences;

import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.util.PropertyChangeEvent;

/**    
 * ArduinoPreferencePage is the class that is behind the preference page of arduino that allows you to select 
 * the arduino path and the option to use the arduino IDE settings <br/>
 * Note that this class uses 2 technologies to change values (the flag and the path). 
 * This should be changed some day to only using the field editor.<br/>
 * The arduinoPath and the useIDETools are set at configuration level.<br/>
 * The paths are set in the avr-eclipse preference store<br/>
 * 
 * @author Jan Baeyens 
 *
 */
public class ArduinoPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {
	//private boolean mIsValid =false;
	//private FileFieldEditor fFileEditor;
	private Text fPathText;
	private Button fFolderButton;
	private BooleanFieldEditor mUseArduinoIdeTools;
	private boolean mIsDirty = false;
	

	/**
	 * PropertyChange set the flag mIsDirty to false. <br/>
	 * This is needed because the default PerformOK saves all fields in the object store. As I use the same objectstore as the avreclipse
	 * I need to make sure the performOK will not overwrite settings it should not. Therefore I set the mIsDirty flag to true as soon as
	 * a field gets change. Then I use this flag in the PerformOK to decide to call the super performOK or not.
	 * 
	 * @author Jan Baeyens
	 */
	@Override
	public void propertyChange(PropertyChangeEvent event) {
		mIsDirty=true;
		testStatus();
	}
	
	public ArduinoPreferencePage() {
		super( org.eclipse.jface.preference.FieldEditorPreferencePage.GRID);
		setDescription("Arduino Global Settings");
		setPreferenceStore(ArduinoPreferences.getConfigurationPreferenceStore());
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
 * PerformOK is done when the end users presses OK on a preference page. The order of the execution of the performOK is undefined.
 * This method saves the Avrdude preferences when the use arduino IDE tools flag is set.<br/>
 * This code also saves the Arduino path
 * @see propertyChange
 * 
 * @see createFieldEditors
 * 
 * @author Jan Baeyens
 * 
 */
	@Override
	public boolean performOk() {
		if (!testStatus()) 	return false;
		if (mIsDirty)
			{
			super.performOk();
			}
		ArduinoPreferences.setArduinoPath(new Path(fPathText.getText()));
		boolean bUseArduinoIdeTools = mUseArduinoIdeTools.getBooleanValue();
		if (bUseArduinoIdeTools)
		{
			ArduinoPreferences.ConfigureToUseArduinoIDETools();
		}
		return true; // super.performOk();
	}




	@Override
	public void init(IWorkbench workbench) {
		// nothing to do
	}

	/**createFieldEditors creates the fields to edit. <br/>
	 * Due to lack of knowledge of the author the path and brows button are "hard coded" therefore the perform OK needs to save the path.
	 * 
	 * @author Jan Baeyens
	 */
	@Override
	protected void createFieldEditors() {
		final Composite parent = getFieldEditorParent();
		GridLayout theGridLayout = new GridLayout();
		GridData theGriddata;
		theGridLayout.numColumns = 2;
		parent.setLayout(theGridLayout);
		
		

		fPathText = new Text(parent, SWT.SINGLE | SWT.BORDER);
		fPathText.setText(ArduinoPreferences.getArduinoPath().toOSString());
		fPathText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fPathText.addListener(SWT.Modify, new Listener() {
			@Override
			public void handleEvent(Event event) {
				testStatus();
			}
		});
		theGriddata = new GridData();
		theGriddata.horizontalAlignment = SWT.FILL;
		theGriddata.horizontalSpan = 1;
		theGriddata.grabExcessHorizontalSpace = true;
		fPathText.setLayoutData(theGriddata);
		fPathText.setEnabled(true);

		fFolderButton = new Button(parent, SWT.NONE);
		fFolderButton.setText("Browse...");
		fFolderButton.addSelectionListener( new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				DirectoryDialog fileDialog = new DirectoryDialog(getShell(), SWT.OPEN);
				if (fPathText.getText() != null) fileDialog.setFilterPath(fPathText.getText());
				String dir = fileDialog.open();
				if (dir != null) fPathText.setText(dir.trim());
				testStatus();
			}
		});
		theGriddata = new GridData();
		theGriddata.horizontalAlignment = SWT.LEAD;
		theGriddata.horizontalSpan = 1;
		fFolderButton.setLayoutData(theGriddata);
		fFolderButton.setEnabled(true);
		
	
		fFolderButton.setVisible(true);
		fPathText.setVisible(true);
		
		Dialog.applyDialogFont(parent);
		
		mUseArduinoIdeTools = new BooleanFieldEditor(	ArduinoConst.KEY_USE_ARDUINO_IDE_TOOLS,  "Use Arduino IDE tools in eclipse", parent);
		addField(mUseArduinoIdeTools);		
	}
	
	/**
	 * testStatus test whether the provided information is OK.
	 * Here the code checks whether there is a hardware\arduino\board.txt file under the provide path.
	 * 
	 * @return true if the provided info is OK; False if the provided info is not OK
	 * 
	 * @author Jan Baeyens
	 * 
	 */
	private boolean testStatus() {
		Path BoardFile = new Path(fPathText.getText());
		File file = BoardFile.append(ArduinoConst.BOARDS_FILE_SUFFIX).toFile();
		return file.canRead();
	}

}
