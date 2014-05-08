package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.ArduinoInstancePreferences;
import it.baeyens.arduino.tools.ArduinoHelpers;
import it.baeyens.arduino.tools.Stream;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

public class NewArduinoSketchWizardCodeSelectionPage extends WizardPage {

    final Shell shell = new Shell();
    private final int ncol = 4;
    String[] codeOptions = { "Default ino file", "Default cpp file", "Custom template", "Sample sketch" };
    private final int defaultIno = 0;
    private final int defaultCPP = 1;
    private final int CustomTemplate = 2;
    private final int sample = 3;
    Composite mParentComposite = null;

    protected LabelCombo mCodeSourceOptionsCombo; // ComboBox Containing all the sketch creation options

    protected DirectoryFieldEditor mTemplateFolderEditor;
    protected ArduinoSampleSelector mExampleEditor = null;
    protected Button mCheckBoxUseCurrentSettingsAsDefault; // checkbox whether to use the current settings as default
    private IPath mArduinoExamplePath = null;
    private IPath mPrivateLibraryPath = null;
    private IPath mPlatformPathPath = null;

    public NewArduinoSketchWizardCodeSelectionPage(String pageName) {
	super(pageName);
	setPageComplete(true);
    }

    public NewArduinoSketchWizardCodeSelectionPage(String pageName, String title, ImageDescriptor titleImage) {
	super(pageName, title, titleImage);
	setPageComplete(true);
    }

    @Override
    public void createControl(Composite parent) {

	Composite composite = new Composite(parent, SWT.NULL);
	mParentComposite = composite;
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
	Listener comboListener = new Listener() {

	    @Override
	    public void handleEvent(Event event) {
		SetControls();
		validatePage();

	    }
	};
	mCodeSourceOptionsCombo = new LabelCombo(composite, "select code", ncol, comboListener);

	mCodeSourceOptionsCombo.mCombo.setItems(codeOptions);

	mTemplateFolderEditor = new DirectoryFieldEditor("temp1", "Custom Template Location:", composite);
	mExampleEditor = new ArduinoSampleSelector(composite, SWT.NONE, "Select Example code.");
	// GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
	// gd.horizontalSpan = ncol;
	// mExampleEditor.setLayoutData(gd);

	mTemplateFolderEditor.getTextControl(composite).addListener(SWT.Modify, comboListener);
	// mSampleFolderEditor.getTextControl(composite).addListener(SWT.Modify, comboListener);

	mCheckBoxUseCurrentSettingsAsDefault = new Button(composite, SWT.CHECK);
	mCheckBoxUseCurrentSettingsAsDefault.setText("Use current settings as default");
	theGriddata = new GridData();
	theGriddata.horizontalSpan = ncol;
	theGriddata.horizontalAlignment = SWT.LEAD;
	theGriddata.grabExcessHorizontalSpace = false;
	mCheckBoxUseCurrentSettingsAsDefault.setLayoutData(theGriddata);
	//

	//
	// End of special controls
	//

	restoreAllSelections();// load the default settings
	SetControls();// set the controls according to the setting
	if (mArduinoExamplePath != null) {
	    mExampleEditor.AddAllExamples(mArduinoExamplePath, mPrivateLibraryPath, mPlatformPathPath);
	}

	validatePage();// validate the page

	setControl(composite);
    }

    /**
     * @name SetControls() Enables or disables the controls based on the Checkbox settings
     */
    protected void SetControls() {
	switch (mCodeSourceOptionsCombo.mCombo.getSelectionIndex()) {
	case defaultIno:
	    mTemplateFolderEditor.setEnabled(false, mParentComposite);
	    mExampleEditor.setEnabled(false);
	    break;
	case defaultCPP:
	    mTemplateFolderEditor.setEnabled(false, mParentComposite);
	    mExampleEditor.setEnabled(false);
	    break;
	case CustomTemplate:
	    mTemplateFolderEditor.setEnabled(true, mParentComposite);
	    mExampleEditor.setEnabled(false);
	    break;
	case sample:
	    mTemplateFolderEditor.setEnabled(false, mParentComposite);
	    mExampleEditor.setEnabled(true);
	    break;
	default:
	    break;
	}
    }

    /**
     * @name validatePage() Check if the user has provided all the info to create the project. If so enable the finisch button.
     */
    protected void validatePage() {
	switch (mCodeSourceOptionsCombo.mCombo.getSelectionIndex()) {
	case defaultIno:
	case defaultCPP:
	    setPageComplete(true);// default always works
	    break;
	case CustomTemplate:
	    IPath templateFolder = new Path(mTemplateFolderEditor.getStringValue());
	    File cppFile = templateFolder.append("sketch.cpp").toFile();
	    File headerFile = templateFolder.append("sketch.h").toFile();
	    File inoFile = templateFolder.append("sketch.ino").toFile();
	    boolean existFile = inoFile.isFile() || (cppFile.isFile() && headerFile.isFile());
	    setPageComplete(existFile);
	    break;
	case sample:
	    // setPageComplete(new Path(mSampleFolderEditor.getStringValue()).toFile().exists());
	    setPageComplete(true);
	    break;
	default:
	    setPageComplete(false);
	    break;
	}
    }

    /**
     * @name restoreAllSelections() Restore all necessary variables into the respective controls
     */
    private void restoreAllSelections() {
	//
	// get the settings for the Use Default checkbox and foldername from the environment settings
	// settings are saved when the files are created and the use this as default flag is set
	//
	mTemplateFolderEditor.setStringValue(ArduinoInstancePreferences.getLastTemplateFolderName());
	mCodeSourceOptionsCombo.mCombo.select(ArduinoInstancePreferences.getLastUsedDefaultSketchSelection());
    }

    public void createFiles(IProject project, IProgressMonitor monitor) throws CoreException {
	if (mCheckBoxUseCurrentSettingsAsDefault.getSelection()) {
	    ArduinoInstancePreferences.setLastTemplateFolderName(mTemplateFolderEditor.getStringValue());
	    ArduinoInstancePreferences.setLastUsedDefaultSketchSelection(mCodeSourceOptionsCombo.mCombo.getSelectionIndex());
	}

	// first determine type of include due to Arduino version. Since version 1.0 we use Arduino.h
	//
	String Include = "WProgram.h";
	if (ArduinoInstancePreferences.isArduinoIdeOne()) // Arduino v1.0+
	{
	    Include = "Arduino.h";
	}
	//
	// Create the source files (sketch.cpp and sketch.h)
	//
	switch (mCodeSourceOptionsCombo.mCombo.getSelectionIndex()) {
	case defaultIno:
	    ArduinoHelpers.addFileToProject(project, new Path(project.getName() + ".ino"),
		    Stream.openContentStream(project.getName(), Include, "templates/sketch.ino", false), monitor);
	    break;
	case defaultCPP:
	    ArduinoHelpers.addFileToProject(project, new Path(project.getName() + ".cpp"),
		    Stream.openContentStream(project.getName(), Include, "templates/sketch.cpp", false), monitor);
	    ArduinoHelpers.addFileToProject(project, new Path(project.getName() + ".h"),
		    Stream.openContentStream(project.getName(), Include, "templates/sketch.h", false), monitor);
	    break;
	case CustomTemplate:
	    Path folderName = new Path(mTemplateFolderEditor.getStringValue());
	    File cppTemplateFile = folderName.append("sketch.cpp").toFile();
	    File hTemplateFile = folderName.append("sketch.h").toFile();
	    File inoFile = folderName.append("sketch.ino").toFile();
	    if (inoFile.exists()) {
		ArduinoHelpers.addFileToProject(project, new Path(project.getName() + ".ino"),
			Stream.openContentStream(project.getName(), Include, inoFile.toString(), true), monitor);
	    } else {
		ArduinoHelpers.addFileToProject(project, new Path(project.getName() + ".cpp"),
			Stream.openContentStream(project.getName(), Include, cppTemplateFile.toString(), true), monitor);
		ArduinoHelpers.addFileToProject(project, new Path(project.getName() + ".h"),
			Stream.openContentStream(project.getName(), Include, hTemplateFile.toString(), true), monitor);
	    }
	    break;
	case sample:
	    try {
		java.io.File target = project.getLocation().toFile();
		File exampleFolders[] = mExampleEditor.getSelectedExamples();
		for (File exampleFolder : exampleFolders) {
		    FileUtils.copyDirectory(exampleFolder, target);

		}
	    } catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	    }
	    break;
	default:

	    break;
	}
    }

    public void removeExamples() {
	if (mExampleEditor != null)
	    mExampleEditor.removeExamples();

    }

    public void AddAllExamples(IPath arduinoExample, IPath privateLibrary, IPath platformPath) {
	if (mExampleEditor != null) {
	    mExampleEditor.AddAllExamples(arduinoExample, privateLibrary, platformPath);
	} else {
	    mArduinoExamplePath = arduinoExample;
	    mPrivateLibraryPath = privateLibrary;
	    mPlatformPathPath = platformPath;
	}

    }

}
