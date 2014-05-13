package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ArduinoHelpers;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

public class ArduinoSampleSelector extends Composite {
    protected Tree myTreeSelector;
    protected Label myLabel;

    public ArduinoSampleSelector(Composite parent, int style, String label) {
	super(parent, style);
	Composite composite = new Composite(parent, SWT.FILL);
	GridLayout theGridLayout = new GridLayout();
	theGridLayout.numColumns = 1;
	composite.setLayout(theGridLayout);
	GridData theGriddata = new GridData(SWT.FILL, SWT.FILL, true, true);
	theGriddata.horizontalSpan = 1;
	// theGriddata.
	composite.setLayoutData(theGriddata);
	composite.setFont(parent.getFont());

	myLabel = new Label(composite, SWT.NONE);
	myLabel.setText(label);
	theGriddata = new GridData(SWT.LEFT, SWT.TOP, false, false);
	theGriddata.horizontalSpan = 1;
	myLabel.setLayoutData(theGriddata);

	myTreeSelector = new Tree(composite, SWT.CHECK | SWT.BORDER);

	myTreeSelector.setLayoutData(theGriddata);
	// Get the data in the tree
	myTreeSelector.setRedraw(false);

	myTreeSelector.addListener(SWT.Selection, new Listener() {
	    @Override
	    public void handleEvent(Event event) {

		if (event.detail == SWT.CHECK) {
		    TreeItem thechangeItem = (TreeItem) event.item;
		    if ((thechangeItem.getParentItem() == null)) {
			event.detail = SWT.NONE;
			event.type = SWT.None;
			event.doit = false;
			thechangeItem.setChecked(false);
		    }
		}
	    }

	});
	// Turn drawing back on!
	theGriddata = new GridData(SWT.FILL, SWT.FILL, true, true);
	theGriddata.horizontalSpan = 1;
	myTreeSelector.setLayoutData(theGriddata);
	myTreeSelector.setRedraw(true);

    }

    public void AddAllExamples(IPath arduinoExample, IPath privateLibrary, IPath hardwareLibrary) {
	// IWorkspace workspace = ResourcesPlugin.getWorkspace();
	removeExamples();

	if (hardwareLibrary.toFile().exists()) {
	    addLibExamples(hardwareLibrary, ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_HARDWARE_LIB);
	}

	if (arduinoExample.toFile().exists()) {
	    // Create Arduino Item
	    // TreeItem myArduinoExampleItem = new TreeItem(myTreeSelector, SWT.NONE);
	    // myArduinoExampleItem.setText("Arduino examples");
	    // myArduinoExampleItem.setData(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_ARDUINO_LIB);
	    // // Add the Arduino Libs
	    // addExamples(myArduinoExampleItem, arduinoExample);
	    addExamples(arduinoExample, ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_ARDUINO_LIB);
	}

	if (privateLibrary.toFile().exists()) {
	    addLibExamples(privateLibrary, ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_PRIVATE_LIB);
	}

    }

    private void addExamples(IPath iPath, String pathVarName) {
	File LibRoot = iPath.toFile();
	IPath LibFolder;
	String[] children = LibRoot.list();
	if (children == null) {
	    // Either dir does not exist or is not a directory
	} else {
	    java.util.Arrays.sort(children, String.CASE_INSENSITIVE_ORDER);
	    for (int i = 0; i < children.length; i++) {
		TreeItem libItem = new TreeItem(myTreeSelector, SWT.NONE);
		libItem.setText(children[i]);
		libItem.setData(pathVarName);
		LibFolder = iPath.append(children[i]);
		if (LibFolder.toFile().isDirectory()) {
		    addExamples(libItem, LibFolder);
		}
	    }
	}
    }

    private void addLibExamples(IPath iPath, String pathVarName) {
	File LibRoot = iPath.toFile();
	IPath LibFolder;
	String[] children = LibRoot.list();
	if (children == null) {
	    // Either dir does not exist or is not a directory
	} else {
	    java.util.Arrays.sort(children, String.CASE_INSENSITIVE_ORDER);
	    for (int i = 0; i < children.length; i++) {
		TreeItem libItem = new TreeItem(myTreeSelector, SWT.NONE);
		libItem.setText(children[i]);
		libItem.setData(pathVarName);
		LibFolder = iPath.append(children[i]).append("examples");
		if (LibFolder.toFile().isDirectory()) {
		    addExamples(libItem, LibFolder);
		}
	    }
	}
    }

    private static void addExamples(TreeItem LibItem, IPath iPath) {
	File LibRoot = iPath.toFile();
	IPath LibFolder;
	String[] children = LibRoot.list();
	if (children == null) {
	    // Either dir does not exist or is not a directory
	} else {
	    java.util.Arrays.sort(children, String.CASE_INSENSITIVE_ORDER);
	    for (int i = 0; i < children.length; i++) {
		// Get filename of file or directory
		LibFolder = iPath.append(children[i]);
		if (LibFolder.toFile().isDirectory()) {
		    TreeItem child = new TreeItem(LibItem, SWT.NONE);
		    child.setText(children[i]);
		    child.setData(LibFolder.toFile());
		}
	    }
	}
    }

    public void removeExamples() {
	myTreeSelector.removeAll();

    }

    @Override
    public void setEnabled(boolean enable) {
	myTreeSelector.setEnabled(enable);
	myLabel.setEnabled(enable);
    }

    public void CopySelectedExamples(File target) throws IOException {

	myTreeSelector.getItems();
	for (TreeItem curTreeItem : myTreeSelector.getItems()) {
	    for (TreeItem curchildTreeItem : curTreeItem.getItems()) {
		if (curchildTreeItem.getChecked() && (curchildTreeItem.getData() != null)) {
		    FileUtils.copyDirectory((File) curchildTreeItem.getData(), target);
		}
	    }
	}
    }

    public void importSelectedLibraries(IProject project, ICConfigurationDescription configurationDescriptions[]) {
	// ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
	// ICProjectDescription projectDescription = mngr.getProjectDescription(project, true);
	// ICConfigurationDescription configurationDescriptions[] = projectDescription.getConfigurations();
	myTreeSelector.getItems();
	for (TreeItem curTreeItem : myTreeSelector.getItems()) {
	    String PathVarName = (String) curTreeItem.getData();
	    for (TreeItem curchildTreeItem : curTreeItem.getItems()) {
		if (curchildTreeItem.getChecked() && (curchildTreeItem.getData() != null)) {
		    try {
			ArduinoHelpers.addCodeFolder(project, PathVarName, curTreeItem.getText(),
				ArduinoConst.WORKSPACE_LIB_FOLDER + curTreeItem.getText(), configurationDescriptions);
		    } catch (CoreException e) {
			Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to import library ", e));
		    }
		    break;
		}
	    }
	}
	// try {
	// // projectDescription.(configurationDescription);
	// mngr.setProjectDescription(project, projectDescription, true, null);
	// } catch (CoreException e) {
	// e.printStackTrace();
	// }

    }
}
