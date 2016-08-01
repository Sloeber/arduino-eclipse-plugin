package it.baeyens.arduino.ui;

import java.io.File;
import java.io.IOException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import it.baeyens.arduino.common.Const;
import it.baeyens.arduino.common.InstancePreferences;
import it.baeyens.arduino.tools.Helpers;
import it.baeyens.arduino.tools.Stream;

public class NewSketchWizardCodeSelectionPage extends WizardPage {

    final Shell shell = new Shell();
    String[] codeOptions = { Messages.ui_new_sketch_default_ino, Messages.ui_new_sketch_default_cpp,
	    Messages.ui_new_sketch_custom_template, Messages.ui_new_sketch_sample_sketch };
    private static final int defaultIno = 0;
    private static final int defaultCPP = 1;
    private static final int CustomTemplate = 2;
    private static final int sample = 3;
    Composite mParentComposite = null;

    protected LabelCombo mCodeSourceOptionsCombo; // ComboBox Containing all the
						  // sketch creation options

    protected DirectoryFieldEditor mTemplateFolderEditor;
    protected SampleSelector mExampleEditor = null;
    protected Button mCheckBoxUseCurrentLinkSample;
    private String platformPath = null;

    public void setPlatformPath(String newPlatformPath) {
	if (newPlatformPath.equals(this.platformPath))
	    return; // this is needed as setting the examples will remove the
		    // selection
	this.platformPath = newPlatformPath;
	AddAllExamples();
	validatePage();
    }

    public NewSketchWizardCodeSelectionPage(String pageName) {
	super(pageName);
	setPageComplete(true);
    }

    public NewSketchWizardCodeSelectionPage(String pageName, String title, ImageDescriptor titleImage) {
	super(pageName, title, titleImage);
	setPageComplete(true);
    }

    @Override
    public void createControl(Composite parent) {

	Composite composite = new Composite(parent, SWT.NULL);
	this.mParentComposite = composite;

	GridLayout theGridLayout = new GridLayout();
	theGridLayout.numColumns = 4;
	composite.setLayout(theGridLayout);

	Listener comboListener = new Listener() {

	    @Override
	    public void handleEvent(Event event) {
		SetControls();
		validatePage();

	    }
	};
	this.mCodeSourceOptionsCombo = new LabelCombo(composite, Messages.ui_new_sketch_selecy_code, 4,
		Const.EMPTY_STRING, true);
	this.mCodeSourceOptionsCombo.addListener(comboListener);

	this.mCodeSourceOptionsCombo.setItems(this.codeOptions);

	this.mTemplateFolderEditor = new DirectoryFieldEditor("temp1", Messages.ui_new_sketch_custom_template_location, //$NON-NLS-1$
		composite);

	this.mExampleEditor = new SampleSelector(composite, SWT.FILL, Messages.ui_new_sketch_select_example_code, 4);
	// GridData theGriddata = new GridData();
	// theGriddata.horizontalSpan = 4;// (ncol - 1);
	// theGriddata.horizontalAlignment = SWT.FILL;
	// this.mExampleEditor.setLayoutData(theGriddata);

	this.mExampleEditor.addchangeListener(new Listener() {

	    @Override
	    public void handleEvent(Event event) {
		validatePage();

	    }

	});

	this.mTemplateFolderEditor.getTextControl(composite).addListener(SWT.Modify, comboListener);

	this.mCheckBoxUseCurrentLinkSample = new Button(composite, SWT.CHECK);
	this.mCheckBoxUseCurrentLinkSample.setText(Messages.ui_new_sketch_link_to_sample_code);

	//
	// End of special controls
	//

	restoreAllSelections();// load the default settings
	SetControls();// set the controls according to the setting

	validatePage();// validate the page

	setControl(composite);

    }

    /**
     * @name SetControls() Enables or disables the controls based on the
     *       Checkbox settings
     */
    protected void SetControls() {
	switch (this.mCodeSourceOptionsCombo.mCombo.getSelectionIndex()) {
	case defaultIno:
	    this.mTemplateFolderEditor.setEnabled(false, this.mParentComposite);
	    this.mExampleEditor.setEnabled(false);
	    this.mCheckBoxUseCurrentLinkSample.setEnabled(false);
	    break;
	case defaultCPP:
	    this.mTemplateFolderEditor.setEnabled(false, this.mParentComposite);
	    this.mExampleEditor.setEnabled(false);
	    this.mCheckBoxUseCurrentLinkSample.setEnabled(false);
	    break;
	case CustomTemplate:
	    this.mTemplateFolderEditor.setEnabled(true, this.mParentComposite);
	    this.mExampleEditor.setEnabled(false);
	    this.mCheckBoxUseCurrentLinkSample.setEnabled(false);
	    break;
	case sample:
	    this.mTemplateFolderEditor.setEnabled(false, this.mParentComposite);
	    this.mExampleEditor.setEnabled(true);
	    this.mCheckBoxUseCurrentLinkSample.setEnabled(true);
	    break;
	default:
	    break;
	}
    }

    /**
     * @name validatePage() Check if the user has provided all the info to
     *       create the project. If so enable the finish button.
     */
    protected void validatePage() {
	switch (this.mCodeSourceOptionsCombo.mCombo.getSelectionIndex()) {
	case defaultIno:
	case defaultCPP:
	    setPageComplete(true);// default always works
	    break;
	case CustomTemplate:
	    IPath templateFolder = new Path(this.mTemplateFolderEditor.getStringValue());
	    File cppFile = templateFolder.append("sketch.cpp").toFile(); //$NON-NLS-1$
	    File headerFile = templateFolder.append("sketch.h").toFile(); //$NON-NLS-1$
	    File inoFile = templateFolder.append("sketch.ino").toFile(); //$NON-NLS-1$
	    boolean existFile = inoFile.isFile() || (cppFile.isFile() && headerFile.isFile());
	    setPageComplete(existFile);
	    break;
	case sample:
	    setPageComplete(this.mExampleEditor.isSampleSelected());
	    break;
	default:
	    setPageComplete(false);
	    break;
	}
    }

    /**
     * @name restoreAllSelections() Restore all necessary variables into the
     *       respective controls
     */
    private void restoreAllSelections() {
	//
	// get the settings for the Use Default checkbox and foldername from the
	// environment settings
	// settings are saved when the files are created and the use this as
	// default flag is set
	//
	this.mTemplateFolderEditor.setStringValue(InstancePreferences.getLastTemplateFolderName());
	this.mCodeSourceOptionsCombo.mCombo.select(InstancePreferences.getLastUsedDefaultSketchSelection());
    }

    public void createFiles(IProject project, IProgressMonitor monitor) throws CoreException {

	InstancePreferences.setLastTemplateFolderName(this.mTemplateFolderEditor.getStringValue());
	InstancePreferences.setLastUsedDefaultSketchSelection(this.mCodeSourceOptionsCombo.mCombo.getSelectionIndex());
	this.mExampleEditor.saveLastUsedExamples();

	String Include = "Arduino.h"; //$NON-NLS-1$

	//
	// Create the source files (sketch.cpp and sketch.h)
	//
	switch (this.mCodeSourceOptionsCombo.mCombo.getSelectionIndex()) {
	case defaultIno:
	    Helpers.addFileToProject(project, new Path(project.getName() + ".ino"), //$NON-NLS-1$
		    Stream.openContentStream(project.getName(), Include, "templates/sketch.ino", false), monitor, //$NON-NLS-1$
		    false);
	    break;
	case defaultCPP:
	    Helpers.addFileToProject(project, new Path(project.getName() + ".cpp"), //$NON-NLS-1$
		    Stream.openContentStream(project.getName(), Include, "templates/sketch.cpp", false), monitor, //$NON-NLS-1$
		    false);
	    Helpers.addFileToProject(project, new Path(project.getName() + ".h"), //$NON-NLS-1$
		    Stream.openContentStream(project.getName(), Include, "templates/sketch.h", false), monitor, false); //$NON-NLS-1$
	    break;
	case CustomTemplate:
	    Path folderName = new Path(this.mTemplateFolderEditor.getStringValue());
	    File cppTemplateFile = folderName.append("sketch.cpp").toFile(); //$NON-NLS-1$
	    File hTemplateFile = folderName.append("sketch.h").toFile(); //$NON-NLS-1$
	    File inoFile = folderName.append("sketch.ino").toFile(); //$NON-NLS-1$
	    if (inoFile.exists()) {
		Helpers.addFileToProject(project, new Path(project.getName() + ".ino"), //$NON-NLS-1$
			Stream.openContentStream(project.getName(), Include, inoFile.toString(), true), monitor, false);
	    } else {
		Helpers.addFileToProject(project, new Path(project.getName() + ".cpp"), //$NON-NLS-1$
			Stream.openContentStream(project.getName(), Include, cppTemplateFile.toString(), true), monitor,
			false);
		Helpers.addFileToProject(project, new Path(project.getName() + ".h"), //$NON-NLS-1$
			Stream.openContentStream(project.getName(), Include, hTemplateFile.toString(), true), monitor,
			false);
	    }
	    break;
	case sample:
	    try {
		boolean MakeLinks = this.mCheckBoxUseCurrentLinkSample.getSelection();
		this.mExampleEditor.CopySelectedExamples(project, new Path("/"), MakeLinks); //$NON-NLS-1$
	    } catch (IOException e) {
		e.printStackTrace();
	    }
	    break;
	default:

	    break;
	}
    }

    public void AddAllExamples() {
	if (this.mExampleEditor != null) {
	    this.mExampleEditor.AddAllExamples(this.platformPath);
	}

    }

}
