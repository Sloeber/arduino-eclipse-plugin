package it.baeyens.arduino.eclipse;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
//import it.baeyens.arduino.eclipse.*;



public class ArduinoPageLayout {
   //global stuff to allow to communicate outside this class
	public Text feedbackControl; 

    // GUI elements
	private Text controlArduinoPath;
	private Button controlBrowseButton;
	private Combo controlArduinoBoardName;
	private Text controlMCUName;
	private Text controlMCUFrequency;
	private Text controlUploadBaudRate;
	private Text controlUploadPort;
	private Button controlDisableFlushing;

	// the properties to modify
	private ArduinoProperties mArduinoProperties  = new ArduinoProperties();

	//Arduino input data
	ArduinoBoards mArduinoBoards = new ArduinoBoards();

	private boolean mArduinoPathIsValid=false;
	private boolean mValidAndComplete;
	
	

	private Listener fieldModifyListener = new Listener() {
		@Override
		public void handleEvent(Event e) {
			validatePage();
		}
	};

	private Listener pathModifyListener = new Listener() {
		@Override
		public void handleEvent(Event e) {
			mArduinoPathIsValid= mArduinoBoards.Load( controlArduinoPath.getText().trim());
			controlArduinoBoardName.setItems(mArduinoBoards.GetArduinoBoards()) ;
			EnableControls();
			validatePage();
		}
	};
	

	private Listener BoardModifyListener = new Listener() {
		@Override
		public void handleEvent(Event e) {
			EnableControls();
			showBoardSetting();
			validatePage();
		}
	};

	private Listener DigitsOnlyListener = new Listener() {
		@Override
		public void handleEvent (Event e) {
		String instring = e.text;
		String outString="";
		char [] chars = new char [instring.length ()];
		instring.getChars (0, chars.length, chars, 0);
		for (int i=0; i<chars.length; i++) {
			if (('0' <= chars [i] && chars [i] <= '9')) {
				outString = outString + chars [i];
			}
		}
		e.text=outString;
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
		Label line = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL| SWT.BOLD);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalSpan = ncol;
		line.setLayoutData(gridData);
	}

	public void draw(Composite composite, IProject  project){
		mArduinoProperties.read(project);
		draw( composite  );
	}
	public void draw(Composite composite  ) {
		int ncol = 4;
		mArduinoPathIsValid = mArduinoBoards.Load(mArduinoProperties.getArduinoPath());
		
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
		

		controlBrowseButton = new Button(composite, SWT.NONE);
		controlBrowseButton.setText("Browse..."); //$NON-NLS-1$
		theGriddata = new GridData();
		theGriddata.horizontalAlignment = SWT.LEAD;
		controlBrowseButton.setLayoutData(theGriddata);
		controlBrowseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				final Shell shell = new Shell();
				String Path = new DirectoryDialog(shell).open();
				controlArduinoPath.setText(Path);
			}
		});

		createLine(composite, ncol);
		createLabel(composite, ncol, "Your Arduino board specifications"); //$NON-NLS-1$
		new Label(composite, SWT.NONE).setText("Board:"); //$NON-NLS-1$
		controlArduinoBoardName = new Combo(composite, SWT.BORDER | SWT.READ_ONLY);
		theGriddata = new GridData();
		theGriddata.horizontalAlignment = SWT.FILL;
		theGriddata.horizontalSpan = (ncol - 1);
		controlArduinoBoardName.setLayoutData(theGriddata);
		controlArduinoBoardName.setEnabled(false);
		controlArduinoBoardName.setItems(mArduinoBoards.GetArduinoBoards()) ;



		new Label(composite, SWT.None).setText("Port: ");
		controlUploadPort = new Text(composite, SWT.BORDER);
		theGriddata = new GridData();
		theGriddata.horizontalSpan = (ncol - 1);
		theGriddata.horizontalAlignment = SWT.FILL;
		controlUploadPort.setLayoutData(theGriddata);
		controlUploadPort.setEnabled(false);
		
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
		
		
		new Label(composite, SWT.NONE).setText("Disable Flushing:"); //$NON-NLS-1$
		controlDisableFlushing = new Button(composite,  SWT.CHECK );
		controlDisableFlushing.setEnabled(false);
		


		//Create the control to alert parents of changes
		feedbackControl = new Text(composite,SWT.None);
		feedbackControl.setVisible(false);
		feedbackControl.setEnabled(false);
		theGriddata = new GridData();
		theGriddata.horizontalSpan = 0;
		feedbackControl.setLayoutData(theGriddata);
		
		

		
		//set the values before the listener to avoid the listeners changing values
		controlArduinoPath.setText(mArduinoProperties.getArduinoPath());
		controlArduinoBoardName.setText(mArduinoProperties.getArduinoBoardName());
		controlUploadPort.setText(mArduinoProperties.getUploadPort());
		showBoardSetting();

		//Set the listeners
		controlUploadPort.addListener(SWT.Modify, fieldModifyListener);
		controlArduinoPath.addListener(SWT.Modify, pathModifyListener);
		controlArduinoBoardName.addListener(SWT.Selection, BoardModifyListener);		
		controlMCUFrequency.addListener(SWT.Modify, fieldModifyListener);
		controlMCUFrequency.addListener(SWT.Verify, DigitsOnlyListener);  //onmy allow digits
		
		
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
	}


	private void showBoardSetting() {
		String BoardName = controlArduinoBoardName.getText();
		controlMCUName.setText(mArduinoBoards.getMCUName(BoardName));
		controlMCUFrequency.setText(mArduinoBoards.getMCUFrequency(BoardName));
		controlUploadBaudRate.setText(mArduinoBoards.getUploadBaudRate(BoardName));
		controlDisableFlushing.setSelection(mArduinoBoards.getDisableFlushing(BoardName));
	}


	public void setToDefaults() {
	}


	private void validatePage() {
		mArduinoProperties.setArduinoBoardName(controlArduinoBoardName.getText().trim());
		mArduinoProperties.setArduinoPath(controlArduinoPath.getText().trim());
		mArduinoProperties.setMCUFrequency(ArduinoHelpers.ToInt(controlMCUFrequency.getText().trim()));
		mArduinoProperties.setMCUName(controlMCUName.getText().trim());
		mArduinoProperties.setUploadBaudrate(controlUploadBaudRate.getText().trim());
		mArduinoProperties.setUploadPort(controlUploadPort.getText().trim());
		mArduinoProperties.setDisabledFlushing(controlDisableFlushing.getSelection());

		mValidAndComplete = mArduinoPathIsValid && !controlArduinoBoardName.getText().trim().equals("")
				&& !controlUploadPort.getText().trim().equals("");
		feedbackControl.setText(mValidAndComplete ? "true":"false");
	}


	public void save(IProject project) {
		mArduinoProperties.save(project);
	}

	public IPath getArduinoSourceCodeLocation(){

		return mArduinoProperties.getArduinoSourceCodeLocation();
	}
	
	public String getMCUName(){
		return mArduinoProperties.getMCUName();
	}

	public String getArduinoBoardName(){
		return mArduinoProperties.getArduinoBoardName();
	}
	
	public ArduinoProperties getProperties() {
		return mArduinoProperties;
	}

	
};
