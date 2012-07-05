package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ArduinoBoards;
import it.baeyens.arduino.tools.ArduinoProperties;


import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;


/**
 * The ArduinoPageLayout class is used in the new wizard and the project
 * properties. This class controls the gui and the data underneath the gui. This
 * class allows to select the arduino board and the port name
 * 
 * @author Jan Baeyens
 * @see ArduinoProperties ArduinoSettingsPage
 * 
 */
public class ArduinoPageLayout {
	// global stuff to allow to communicate outside this class
	public Text feedbackControl;

	// GUI elements
	private Text controlArduinoPath;
	private Combo controlArduinoBoardName;
	private Text controlMCUName;
	private Text controlMCUFrequency;
	private Text controlUploadBaudRate;
	//private Text controlUploadPort;
	private Combo controlUploadPort;
	private Text controlBoardVariant;
	private Text controlUploadProtocol;
	private Text controlBuildCoreFolder;
	private Text controlCppCompileOptions;
	private Text controlCCompileOptions;
	private Text ControlLinkOptions;
	private Button controlDisableFlushing;

	// the properties to modify
	private ArduinoProperties mArduinoProperties = new ArduinoProperties();

	// Arduino input data
	ArduinoBoards mArduinoBoards = new ArduinoBoards();

	private boolean mArduinoPathIsValid = false;
	private boolean mValidAndComplete;

	/**
	 * fieldModifyListener triggers the validity check as soon as something is
	 * changed in a text field. In other words when the port has been modified
	 * 
	 * @author Jan Baeyens
	 * 
	 */
	private Listener fieldModifyListener = new Listener() {
		@Override
		public void handleEvent(Event e) {
			validatePage();
		}
	};

	/**
	 * pathModifyListener triggers the validity check as soon as something is
	 * changed in the path field. In other words when the Arduino Path has been
	 * modified
	 * This method loads the board and updates the board combo box.
	 * 
	 * @author Jan Baeyens
	 * 
	 */
	private Listener pathModifyListener = new Listener() {
		@Override
		public void handleEvent(Event e) {
			mArduinoPathIsValid = mArduinoBoards.Load(new Path(controlArduinoPath.getText().trim()));
			controlArduinoBoardName.setItems(mArduinoBoards.GetArduinoBoards());
			EnableControls();
			validatePage();
		}
	};

	/**BoardModifyListener trigges the validate when the board gets changed
	 * 
	 * @author Jan Baeyens
	 */
	private Listener BoardModifyListener = new Listener() {
		@Override
		public void handleEvent(Event e) {
			EnableControls();
			showBoardSetting();
			validatePage();
		}
	};



	

	private void createLabel(Composite parent, int ncol, String t) {
		Label line = new Label(parent, SWT.HORIZONTAL | SWT.BOLD);
		line.setText(t);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = ncol;
		line.setLayoutData(gridData);
	}

	private void createLine(Composite parent, int ncol) {
		Label line = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL | SWT.BOLD);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = ncol;
		line.setLayoutData(gridData);
	}

	public void draw(Composite composite, IProject project) {
		mArduinoProperties.read(project);
		draw(composite);
	}

	public void draw(Composite composite) {
		int ncol = 4;
		mArduinoPathIsValid = mArduinoBoards.Load(ArduinoInstancePreferences.getArduinoPath());

		// create the desired layout for this wizard page
		GridLayout theGridLayout = new GridLayout();
		GridData theGriddata;
		theGridLayout.numColumns = ncol;
		composite.setLayout(theGridLayout);

		createLabel(composite, ncol, "Arduino Environment Settings"); //$NON-NLS-1$
		new Label(composite, SWT.NONE).setText("Arduino Location"); //$NON-NLS-1$
		controlArduinoPath = new Text(composite, SWT.BORDER);
		theGriddata = new GridData();
		theGriddata.horizontalAlignment = SWT.FILL;
		theGriddata.horizontalSpan = (ncol - 2);
		theGriddata.grabExcessHorizontalSpace = true;
		controlArduinoPath.setLayoutData(theGriddata);
	
		createLine(composite, ncol);
		createLabel(composite, ncol, "Your Arduino board specifications"); //$NON-NLS-1$
		new Label(composite, SWT.NONE).setText("Board:"); //$NON-NLS-1$
		controlArduinoBoardName = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		theGriddata = new GridData();
		theGriddata.horizontalAlignment = SWT.FILL;
		theGriddata.horizontalSpan = (ncol - 1);
		controlArduinoBoardName.setLayoutData(theGriddata);
		controlArduinoBoardName.setEnabled(false);
		controlArduinoBoardName.setItems(mArduinoBoards.GetArduinoBoards());

		new Label(composite, SWT.None).setText("Port: ");
		//controlUploadPort = new Text(composite, SWT.BORDER);
		controlUploadPort = new Combo(composite, SWT.BORDER);// | SWT.READ_ONLY);
		theGriddata = new GridData();
		theGriddata.horizontalSpan = (ncol - 1);
		theGriddata.horizontalAlignment = SWT.FILL;
		controlUploadPort.setLayoutData(theGriddata);
		controlUploadPort.setEnabled(false);
		controlUploadPort.setItems(Common.listComPorts());

		createLine(composite, ncol);
		createLabel(composite, ncol, "The used settings"); //$NON-NLS-1$

		new Label(composite, SWT.NONE).setText("Processor:"); //$NON-NLS-1$
		controlMCUName = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
		theGriddata = new GridData();
		theGriddata.horizontalAlignment = SWT.FILL;
		theGriddata.grabExcessHorizontalSpace = true;
		controlMCUName.setLayoutData(theGriddata);
		controlMCUName.setEnabled(false);

		new Label(composite, SWT.NONE).setText("Processor Frequency (Hz):"); //$NON-NLS-1$
		controlMCUFrequency = new Text(composite, SWT.BORDER);
		theGriddata = new GridData();
		theGriddata.horizontalAlignment = SWT.FILL;
		theGriddata.grabExcessHorizontalSpace = true;
		controlMCUFrequency.setLayoutData(theGriddata);
		controlMCUFrequency.setEnabled(false);

		new Label(composite, SWT.NONE).setText("Baud:"); //$NON-NLS-1$
		controlUploadBaudRate = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
		theGriddata = new GridData();
		theGriddata.horizontalAlignment = SWT.FILL;
		controlUploadBaudRate.setLayoutData(theGriddata);
		controlUploadBaudRate.setEnabled(false);

		new Label(composite, SWT.NONE).setText("Board Variant:"); //$NON-NLS-1$
		controlBoardVariant = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
		theGriddata = new GridData();
		theGriddata.horizontalAlignment = SWT.FILL;
		controlBoardVariant.setLayoutData(theGriddata);
		controlBoardVariant.setEnabled(false);		
		
		new Label(composite, SWT.NONE).setText("UpLoadProtocol:"); //$NON-NLS-1$
		controlUploadProtocol = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
		theGriddata = new GridData();
		theGriddata.horizontalAlignment = SWT.FILL;
		controlUploadProtocol.setLayoutData(theGriddata);
		controlUploadProtocol.setEnabled(false);
		
		new Label(composite, SWT.NONE).setText("Disable Flushing:"); //$NON-NLS-1$
		controlDisableFlushing = new Button(composite, SWT.CHECK);
		controlDisableFlushing.setEnabled(false);
		
		new Label(composite, SWT.NONE).setText("Library folder:"); //$NON-NLS-1$
		controlBuildCoreFolder = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
		theGriddata = new GridData();
		theGriddata.horizontalAlignment = SWT.FILL;
		controlBuildCoreFolder.setLayoutData(theGriddata);
		controlBuildCoreFolder.setEnabled(false);
	

		new Label(composite, SWT.NONE).setText("Cpp compile options:"); //$NON-NLS-1$
		controlCppCompileOptions = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
		theGriddata = new GridData();
		theGriddata.horizontalAlignment = SWT.FILL;
		controlCppCompileOptions.setLayoutData(theGriddata);
		controlCppCompileOptions.setEnabled(false);
		
		new Label(composite, SWT.NONE).setText("C compile options:"); //$NON-NLS-1$
		controlCCompileOptions = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
		theGriddata = new GridData();
		theGriddata.horizontalAlignment = SWT.FILL;
		controlCCompileOptions.setLayoutData(theGriddata);
		controlCCompileOptions.setEnabled(false);
		
		new Label(composite, SWT.NONE).setText("Link options:"); //$NON-NLS-1$
		ControlLinkOptions = new Text(composite, SWT.BORDER | SWT.READ_ONLY);
		theGriddata = new GridData();
		theGriddata.horizontalAlignment = SWT.FILL;
		ControlLinkOptions.setLayoutData(theGriddata);
		ControlLinkOptions.setEnabled(false);
		

		// Create the control to alert parents of changes
		feedbackControl = new Text(composite, SWT.None);
		feedbackControl.setVisible(false);
		feedbackControl.setEnabled(false);
		theGriddata = new GridData();
		theGriddata.horizontalSpan = 0;
		feedbackControl.setLayoutData(theGriddata);

		// set the values before the listener to avoid the listeners changing
		// values
		controlArduinoPath.setText(ArduinoInstancePreferences.getArduinoPath().toOSString());
		controlArduinoBoardName.setText(mArduinoProperties.getArduinoBoardName());
		controlUploadPort.setText(mArduinoProperties.getUploadPort());
		//controlBuildCoreFolder.setText(mArduinoProperties.getBuildCoreFolder());
		showBoardSetting();

		// Set the listeners
		controlUploadPort.addListener(SWT.Modify, fieldModifyListener);
		controlArduinoPath.addListener(SWT.Modify, pathModifyListener);
		controlArduinoBoardName.addListener(SWT.Selection, BoardModifyListener);
//		controlMCUFrequency.addListener(SWT.Modify, fieldModifyListener);
//		controlMCUFrequency.addListener(SWT.Verify, DigitsOnlyListener); // only allow digits

		
		
		// sets which fields can be edited
		EnableControls();
		validatePage();
		Dialog.applyDialogFont(composite);
	}

	public boolean isPageComplete() {
		return mValidAndComplete;
	}

	private void EnableControls() {
		controlArduinoBoardName.setEnabled(mArduinoPathIsValid);
		controlUploadPort.setEnabled(mArduinoPathIsValid);
		controlArduinoPath.setEnabled(false);
	}

	private void showBoardSetting() {
		String BoardName = controlArduinoBoardName.getText();
		controlMCUName.setText(mArduinoBoards.getMCUName(BoardName));
		controlMCUFrequency.setText(mArduinoBoards.getMCUFrequency(BoardName));
		controlUploadBaudRate.setText(mArduinoBoards.getUploadBaudRate(BoardName));
		controlDisableFlushing.setSelection(mArduinoBoards.getDisableFlushing(BoardName));
		controlBoardVariant.setText(mArduinoBoards.getBoardVariant(BoardName));
		controlUploadProtocol.setText(mArduinoBoards.getUploadProtocol(BoardName));
		controlBuildCoreFolder.setText(mArduinoBoards.getBuildCoreFolder(BoardName));
		controlCppCompileOptions.setText(mArduinoBoards.getCppCompileOptions(BoardName));
		controlCCompileOptions.setText(mArduinoBoards.getCCompileOptions(BoardName));
		ControlLinkOptions.setText(mArduinoBoards.getLinkOptions(BoardName));
	
	}

	public void setToDefaults() {
	}

	private void validatePage() {
		mArduinoProperties.setArduinoBoardName(controlArduinoBoardName.getText().trim());
		mArduinoProperties.setMCUFrequency(Common.ToInt(controlMCUFrequency.getText().trim()));
		mArduinoProperties.setMCUName(controlMCUName.getText().trim());
		mArduinoProperties.setUploadBaudrate(controlUploadBaudRate.getText().trim());
		mArduinoProperties.setUploadPort(controlUploadPort.getText().trim());
		mArduinoProperties.setDisabledFlushing(controlDisableFlushing.getSelection());
		mArduinoProperties.setBoardVariant(controlBoardVariant.getText().trim());
		mArduinoProperties.setUploadProtocol(controlUploadProtocol.getText().trim());
		mArduinoProperties.setBuildCoreFolder(controlBuildCoreFolder.getText().trim());
		mArduinoProperties.setCppCompileOptions( controlCppCompileOptions.getText());
		mArduinoProperties.setCCompileOptions(controlCCompileOptions.getText());
		mArduinoProperties.setLinkOptions(ControlLinkOptions.getText());

		mValidAndComplete = mArduinoPathIsValid && !controlArduinoBoardName.getText().trim().equals("") && !controlUploadPort.getText().trim().equals("");
		feedbackControl.setText(mValidAndComplete ? "true" : "false");
	}

	/** save saves the arduino properties visible on this page
	 * 
	 * @param project
	 * @author Jan Baeyens
	 */
	public void save(IProject project) {
		mArduinoProperties.save(project);
	}

//	/**getArduinoSourceCodeLocation returns the source code location
//	 * 
//	 * @return the source code location of arduino. That is the full path
//	 */
//	public IPath getArduinoSourceCodeLocation() {
//
//		return mArduinoProperties.getArduinoSourceCodeLocation();
//	}
	/**getMCUName returns the MCU name
	 * 
	 * @return the MCU name
	 */
	public String getMCUName() {
		return mArduinoProperties.getMCUName();
	}
	/**getArduinoBoardName returns the Arduino Board name
	 * 
	 * @return the Arduino Board name
	 */
	public String getArduinoBoardName() {
		return mArduinoProperties.getArduinoBoardName();
	}

	/**getProperties returns the Arduino properties
	 * 
	 * @return the Arduino properties linked to the page
	 */
	public ArduinoProperties getProperties() {
		return mArduinoProperties;
	}

};
