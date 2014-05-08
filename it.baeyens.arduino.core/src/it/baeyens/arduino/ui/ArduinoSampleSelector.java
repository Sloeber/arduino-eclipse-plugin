package it.baeyens.arduino.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IPath;
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
	    addExamples(hardwareLibrary);
	}

	if (arduinoExample.toFile().exists()) {
	    // Create Arduino Item
	    TreeItem myArduinoExampleItem = new TreeItem(myTreeSelector, SWT.NONE);
	    myArduinoExampleItem.setText("Arduino examples");
	    // Add the Arduino Libs
	    addExamples(myArduinoExampleItem, arduinoExample);
	}

	if (privateLibrary.toFile().exists()) {
	    addExamples(privateLibrary);
	}

    }

    private void addExamples(IPath iPath) {
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

    public File[] getSelectedExamples() {
	List<File> filelist = new ArrayList<File>();
	myTreeSelector.getItems();
	for (TreeItem curTreeItem : myTreeSelector.getItems()) {
	    for (TreeItem curchildTreeItem : curTreeItem.getItems()) {
		if (curchildTreeItem.getChecked() && (curchildTreeItem.getData() != null)) {
		    filelist.add((File) curchildTreeItem.getData());
		}
	    }
	}
	File ret[] = new File[filelist.size()];

	return filelist.toArray(ret);
    }
}
