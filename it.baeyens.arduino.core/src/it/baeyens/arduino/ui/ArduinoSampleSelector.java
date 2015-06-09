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

    /**
     * This method adds all examples to the selection listbox All examples already in the listbox are removed first.
     * 
     * @param arduinoExample
     *            The folder with the arduino samples
     * @param privateLibrary
     *            The folder with the private libraries
     * @param hardwareLibrary
     *            The folder with the hardware libraries
     * @param mPlatformPathPath
     */
    public void AddAllExamples(IPath arduinoExample, IPath arduinoLibPath, IPath privateLibrary, IPath hardwareLibrary) {
	removeExamples();

	if (arduinoExample.toFile().exists()) {
	    // Create Arduino Item
	    // TreeItem myArduinoExampleItem = new TreeItem(myTreeSelector, SWT.NONE);
	    // myArduinoExampleItem.setText("Arduino examples");
	    // myArduinoExampleItem.setData(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_ARDUINO_LIB);
	    // // Add the Arduino Libs
	    // addExamples(myArduinoExampleItem, arduinoExample);
	    addExamplesFolder(arduinoExample);
	}

	if (arduinoLibPath.toFile().exists()) {
	    addLibExamples(arduinoLibPath, ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_ARDUINO_LIB);
	}

	if (hardwareLibrary.toFile().exists()) {
	    addLibExamples(hardwareLibrary, ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_HARDWARE_LIB);
	}

	if (privateLibrary.toFile().exists()) {
	    addLibExamples(privateLibrary, ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_PRIVATE_LIB);
	}
    }

    /**
     * This method adds a folder of examples. There is no search. The provided folder is assumed to be a tree where the parents of the leaves are
     * assumed examples
     * 
     * @param iPath
     * @param pathVarName
     */
    private void addExamplesFolder(IPath iPath) {
	File LibRoot = iPath.toFile();
	IPath LibFolder;
	String[] children = LibRoot.list();
	if (children == null) {
	    // Either dir does not exist or is not a directory
	} else {
	    java.util.Arrays.sort(children, String.CASE_INSENSITIVE_ORDER);
	    for (int i = 0; i < children.length; i++) {
		LibFolder = iPath.append(children[i]);
		if (LibFolder.toFile().isDirectory()) {
		    TreeItem libItem = new TreeItem(myTreeSelector, SWT.NONE);
		    libItem.setText(children[i]);
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
		if ((LibFolder = iPath.append(children[i]).append("examples")).toFile().isDirectory()) {
		    TreeItem libItem = new TreeItem(myTreeSelector, SWT.NONE);
		    libItem.setText(children[i]);
		    libItem.setData(pathVarName);
		    addExamples(libItem, LibFolder);
		} else if ((LibFolder = iPath.append(children[i]).append("Examples")).toFile().isDirectory()) {
		    TreeItem libItem = new TreeItem(myTreeSelector, SWT.NONE);
		    libItem.setText(children[i]);
		    libItem.setData(pathVarName);
		    addExamples(libItem, LibFolder);
		}
	    }
	}
    }

    /**
     * This method does the actual adding of the examples to the listbox.
     * 
     * This method is recursive so we can go deeper in the folder structure on disk. It stops when a .ino file is found
     * 
     * @param LibItem
     *            the parent to add the new examples to
     * @param iPath
     *            The path where the examples are located.
     */
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
		    child.setData(LibFolder);
		    addExamples(child, LibFolder);
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

    private void recursiveCopySelectedExamples(IProject project, IPath target, TreeItem TreeItem, boolean link) throws IOException {
	for (TreeItem curchildTreeItem : TreeItem.getItems()) {
	    if (curchildTreeItem.getChecked() && (curchildTreeItem.getData() != null)) {
		if (link) {
		    ArduinoHelpers.linkDirectory(project, (IPath) curchildTreeItem.getData(), target);
		} else {
		    FileUtils.copyDirectory(((IPath) curchildTreeItem.getData()).toFile(), project.getLocation().toFile());
		}
	    }
	    recursiveCopySelectedExamples(project, target, curchildTreeItem, link);
	}
    }

    public void CopySelectedExamples(IProject project, IPath target, boolean link) throws IOException {
	myTreeSelector.getItems();
	for (TreeItem curTreeItem : myTreeSelector.getItems()) {
	    recursiveCopySelectedExamples(project, target, curTreeItem, link);
	}
    }

    private void recursiveImportSelectedLibraries(IProject project, ICConfigurationDescription configurationDescriptions[], String PathVarName,
	    TreeItem curTreeItem, String LibName) {
	for (TreeItem curchildTreeItem : curTreeItem.getItems()) {
	    if (curchildTreeItem.getChecked() && (curchildTreeItem.getData() != null)) {
		try {
		    ArduinoHelpers.addCodeFolder(project, PathVarName, LibName, ArduinoConst.WORKSPACE_LIB_FOLDER + LibName,
			    configurationDescriptions);
		} catch (CoreException e) {
		    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to import library ", e));
		}
		break;
	    }
	    recursiveImportSelectedLibraries(project, configurationDescriptions, PathVarName, curchildTreeItem, LibName);
	}

    }

    public void importSelectedLibraries(IProject project, ICConfigurationDescription configurationDescriptions[]) {
	myTreeSelector.getItems();
	for (TreeItem curTreeItem : myTreeSelector.getItems()) {
	    String PathVarName = (String) curTreeItem.getData();
	    if (PathVarName != null) {
		recursiveImportSelectedLibraries(project, configurationDescriptions, PathVarName, curTreeItem, curTreeItem.getText());
	    }
	}

    }
}
