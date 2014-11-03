package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.ArduinoInstancePreferences;

import java.io.File;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.ui.newui.AbstractCPropertyTab;
import org.eclipse.cdt.ui.newui.ICPropertyProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
//import java.io.File;
//import org.eclipse.swt.widgets.Combo;
//import org.eclipse.swt.widgets.Event;
//import org.eclipse.swt.widgets.Listener;
//import org.eclipse.swt.widgets.Widget;

/**
 * The SketchSelection class is used in the new wizard and the selecting the folder with the sketch templates This class controls the gui and the data
 * underneath the gui.
 * 
 * @author Nico Verduin
 * @date 25-11-2013
 * 
 */
public class SketchSelectionPage extends AbstractCPropertyTab {
    // global stuff to allow to communicate outside this class
    public Text feedbackControl;
    //
    // Gui Elements used
    //
    protected Label mLabel; // "template folder"
    protected Text mFolderName; // input folder name
    protected Button mBrowseButton; // browse button to find correct folder
    protected Button mCheckBox; // checkbox wheter to use default template

    private final int ncol = 4; // we have 6 columns in the dialog box

    private boolean mValidAndComplete; // Is the form valid and completely
				       // filled in?

    @Override
    public void createControls(Composite parent, ICPropertyProvider provider) {
	super.createControls(parent, provider);
	draw(parent);
    }

    /**
     * @function draw
     * @param composite
     *            builds the layout and displays it
     */
    public void draw(Composite composite) {
	//
	// local variables
	//
	GridLayout theGridLayout; // references the layout
	GridData theGriddata; // references a grid

	//
	// create the grid layout and add it to the composite
	//
	theGridLayout = new GridLayout();
	theGridLayout.numColumns = ncol; // 4 columns
	composite.setLayout(theGridLayout);
	//
	// check box Use default
	//
	mCheckBox = new Button(composite, SWT.CHECK);
	mCheckBox.setText("Use default");
	theGriddata = new GridData();
	theGriddata.horizontalSpan = ncol;
	theGriddata.horizontalAlignment = SWT.LEAD;
	theGriddata.grabExcessHorizontalSpace = false;
	mCheckBox.setLayoutData(theGriddata);
	//
	// Check box event listener
	//
	mCheckBox.addSelectionListener(new SelectionAdapter() {
	    @SuppressWarnings("synthetic-access")
	    @Override
	    //
	    // check if check box is checked
	    //
	    public void widgetSelected(SelectionEvent event) {
		//
		// determine the control settings based on the checkbox setting
		//
		SetControls();
		//
		// check if the page is ready for processing
		//
		validatePage();
		//
		// let the host know of the status so it can set the Next and Finish buttons
		//
		feedbackControl.setText(mValidAndComplete ? "true" : "false");
	    }
	});

	//
	// label field
	//
	mLabel = new Label(composite, SWT.NONE);
	mLabel.setText("Template Folder Location:");
	theGriddata = new GridData();
	theGriddata.horizontalAlignment = SWT.LEFT;
	theGriddata.horizontalSpan = 4;
	theGriddata.grabExcessHorizontalSpace = false;
	mLabel.setLayoutData(theGriddata);
	//
	// folder name field
	//
	mFolderName = new Text(composite, SWT.SINGLE | SWT.BORDER);
	mFolderName.setText(ArduinoInstancePreferences.getLastTemplateFolderName());
	theGriddata = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
	theGriddata.horizontalSpan = 1;
	mFolderName.setLayoutData(theGriddata);
	//
	// Folder change event listener if the user modifies the field directly
	//
	mFolderName.addModifyListener(new ModifyListener() {
	    @SuppressWarnings("synthetic-access")
	    @Override
	    public void modifyText(ModifyEvent event) {
		//
		// if the user changes the field directly, validate the page
		//
		validatePage();
		feedbackControl.setText(mValidAndComplete ? "true" : "false");
	    }
	});
	//
	// Browse button
	//
	mBrowseButton = new Button(composite, SWT.NONE);
	mBrowseButton.setText("Browse..."); //$NON-NLS-1$
	theGriddata = new GridData();
	theGriddata.horizontalSpan = 1;
	theGriddata.horizontalAlignment = SWT.LEAD;
	theGriddata.grabExcessHorizontalSpace = false;
	mBrowseButton.setLayoutData(theGriddata);
	//
	// button click event listener
	//
	mBrowseButton.addSelectionListener(new SelectionAdapter() {
	    @SuppressWarnings("synthetic-access")
	    @Override
	    public void widgetSelected(SelectionEvent event) {
		//
		// create the browse dialog box in a new shell
		//
		final Shell shell = new Shell();
		//
		// create the dialog box and set the parameters
		//
		DirectoryDialog theDialog = new DirectoryDialog(shell);
		//
		// we have to change this
		//
		if ((mFolderName.getText() == null) || (mFolderName.getText() == "")) {
		    theDialog.setFilterPath(ArduinoInstancePreferences.getArduinoPath().append(ArduinoConst.LIBRARY_PATH_SUFFIX).toString());
		} else {
		    theDialog.setFilterPath(mFolderName.getText());
		}
		//
		// get a selected folder
		//
		String Path = theDialog.open();
		if (Path != null) {
		    mFolderName.setText(Path);
		}
		//
		// whether a folder was selected. In any case validate the page and inform
		// the parent
		//
		validatePage();
		feedbackControl.setText(mValidAndComplete ? "true" : "false");
	    }
	});

	//
	// Create the control to alert parents of changes
	//
	feedbackControl = new Text(composite, SWT.None);
	feedbackControl.setVisible(false);
	feedbackControl.setEnabled(false);
	theGriddata = new GridData();
	theGriddata.horizontalSpan = 0;
	feedbackControl.setLayoutData(theGriddata);
	//
	// End of special controls
	//
	// load the environment settings
	//
	restoreAllSelections();
	//
	// set the controls according to the setting
	//
	SetControls();
	//
	// validate the page
	//
	validatePage();
	//
	// inform the parent
	//
	feedbackControl.setText(mValidAndComplete ? "true" : "false");
    }

    /**
     * @name SetControls() Enables or disables the controls based on the mCheckbox settings
     */
    private void SetControls() {
	if (mCheckBox.getSelection() == true) {
	    //
	    // we are using default settings so disable
	    // setting of alternative folder for template files
	    //
	    mLabel.setEnabled(false);
	    mFolderName.setEnabled(false);
	    mBrowseButton.setEnabled(false);
	    mValidAndComplete = true;
	} else {
	    //
	    // we are using our own settings
	    //
	    mLabel.setEnabled(true);
	    mFolderName.setEnabled(true);
	    mBrowseButton.setEnabled(true);
	    mValidAndComplete = false;
	}
    }

    /**
     * @name validatePage() Check if the user has decided to use another folder for the Sketch.cpp and Sketch.h template files. If so, validate the
     *       folder actually contains both files. If the validation meets the criteria, set mValidAndComplete to true. Ohterwise false.
     */
    private void validatePage() {
	//
	// check if the default is selected
	//
	if (mCheckBox.getSelection() == false) {
	    //
	    // check if the folder contains a Sketch.cpp and a Sketch.h
	    //
	    String pathName = mFolderName.getText();
	    boolean existFile = new File(pathName + "\\sketch.cpp").isFile();
	    mValidAndComplete = false;
	    if (existFile == true) {
		//
		// sketch.cpp exists now check if sketch.h exists
		//
		existFile = new File(pathName + "\\sketch.h").isFile();
		if (existFile == true) {
		    //
		    // they both exist so this is a valid folder
		    //
		    mValidAndComplete = true;
		}
	    }
	} else {
	    //
	    // we are using a default setting
	    //
	    mValidAndComplete = true;
	}
	if (mValidAndComplete == true) {
	    //
	    // this page is ok so save the data
	    //
	    saveAllSelections();
	}

    }

    public boolean isPageComplete() {
	return mValidAndComplete;
    }

    @Override
    public boolean canBeVisible() {
	return true;
    }

    @Override
    protected void performApply(ICResourceDescription src, ICResourceDescription dst) {
	saveAllSelections();
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
	    saveAllSelections(getResDesc().getConfiguration());
	}
    }

    /**
     * @name saveAllSelections
     * @param confdesc
     *            Save the folder name and the check Box whether to use the default location for the sketch template files
     */
    public void saveAllSelections(ICConfigurationDescription confdesc) {
	//
	// define and get the values from the screen
	//
	IEnvironmentVariable var;
	String folderName = mFolderName.getText();
	boolean defaultChecked = mCheckBox.getSelection();
	if (confdesc != null) {
	    IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
	    IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
	    var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_SKETCH_TEMPLATE_FOLDER, folderName);
	    contribEnv.addVariable(var, confdesc);
	    var = new EnvironmentVariable(ArduinoConst.ENV_KEY_JANTJE_SKETCH_TEMPLATE_USE_DEFAULT, defaultChecked ? "true" : "false");
	    contribEnv.addVariable(var, confdesc);
	}
	ArduinoInstancePreferences.setLastTemplateFolderName(folderName);
	ArduinoInstancePreferences.setLastUsedDefaultSketchSelection(defaultChecked);
    }

    /**
     * @name restoreAllSelections() Restore all necessary variables into the respective controls
     */
    private void restoreAllSelections() {
	//
	// get the settings for the Use Default checkbox and foldername from the environment settings
	//
	mFolderName.setText(ArduinoInstancePreferences.getLastTemplateFolderName());
	mCheckBox.setSelection(ArduinoInstancePreferences.getLastUsedDefaultSketchSelection());
    }

}
