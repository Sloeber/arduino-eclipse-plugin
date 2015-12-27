package it.baeyens.arduino.ui;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FileUtils;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
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

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.common.ConfigurationPreferences;
import it.baeyens.arduino.tools.ArduinoHelpers;

public class ArduinoSampleSelector extends Composite {
    protected Tree sampleTree;
    protected Label myLabel;
    TreeMap<String, String> examples = new TreeMap<>(); // contains the items in a hashmap so we can sort and all that things before we make the
							// listtree

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

	this.myLabel = new Label(composite, SWT.NONE);
	this.myLabel.setText(label);
	theGriddata = new GridData(SWT.LEFT, SWT.TOP, false, false);
	theGriddata.horizontalSpan = 1;
	this.myLabel.setLayoutData(theGriddata);

	theGriddata = new GridData(SWT.LEFT, SWT.TOP, false, false);
	theGriddata.horizontalSpan = 2;
	this.sampleTree = new Tree(composite, SWT.CHECK | SWT.BORDER);
	this.sampleTree.setLayoutData(theGriddata);
	// Get the data in the tree
	this.sampleTree.setRedraw(false);

	this.sampleTree.addListener(SWT.Selection, new Listener() {
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
	this.sampleTree.setLayoutData(theGriddata);
	this.sampleTree.setRedraw(true);

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

    public void AddAllExamples(String exampleLocations[], String libLocations[], String selectedPlatformLocation) {

	// Get the examples of the library manager installed libraries
	IPath CommonLibLocation = ConfigurationPreferences.getInstallationPathLibraries();
	if (CommonLibLocation.toFile().exists()) {
	    getLibExampleFolders(CommonLibLocation);
	}

	// get the examples from the user provide library locations
	if (libLocations != null) {
	    for (String curLibLocation : libLocations) {
		if (new File(curLibLocation).exists()) {
		    getLibExampleFolders(new Path(curLibLocation));
		}
	    }
	}

	// Get the examples from the example locations
	if (exampleLocations != null) {
	    for (String curExampleLocation : exampleLocations) {
		Path Folder = new Path(curExampleLocation);
		if (Folder.toFile().exists()) {
		    getExampleFolders(new File(curExampleLocation).getName(), Folder);
		}
	    }
	}

	// Get the examples of the libraries from the selected hardware
	// This one should be the last as hasmap overwrites doubles. This way hardware libraries are preferred to others
	if (selectedPlatformLocation != null) {
	    if (new File(selectedPlatformLocation).exists()) {
		getLibExampleFolders(new Path(selectedPlatformLocation).append(ArduinoConst.LIBRARY_PATH_SUFFIX));
	    }
	}

	this.sampleTree.removeAll();
	String curLibName = null;
	TreeItem curlib = null;
	for (Map.Entry<String, String> entry : this.examples.entrySet()) {
	    String keys[] = entry.getKey().split("-"); //$NON-NLS-1$
	    if (!keys[0].equals(curLibName)) {
		curlib = new TreeItem(this.sampleTree, SWT.NONE);
		curLibName = keys[0];
		curlib.setText(keys[0]);
	    }
	    TreeItem curExample = new TreeItem(curlib, SWT.NONE);
	    curExample.setText(keys[1]);
	    curExample.setData("examplePath", entry.getValue()); //$NON-NLS-1$
	    curExample.setData("libName", keys[0]); //$NON-NLS-1$
	    IPath libPath = new Path(entry.getValue()).removeLastSegments(1);
	    if (libPath.lastSegment().equalsIgnoreCase("examples")) //$NON-NLS-1$
	    {
		curExample.setData("libPath", libPath.removeLastSegments(1).toString()); //$NON-NLS-1$
	    }
	}

    }

    /**
     * This method adds a folder of examples. There is no search. The provided folder is assumed to be a tree where the parents of the leaves are
     * assumed examples
     * 
     * @param iPath
     * @param pathVarName
     */
    private void getExampleFolders(String libname, IPath location) {
	String[] children = location.toFile().list();
	if (children == null) {
	    // Either dir does not exist or is not a directory
	} else {
	    java.util.Arrays.sort(children, String.CASE_INSENSITIVE_ORDER);
	    for (String curFolder : children) {
		IPath LibFolder = location.append(curFolder);
		if (LibFolder.toFile().isDirectory()) {
		    this.examples.put(libname + '-' + curFolder, LibFolder.toString());
		}
	    }
	}
    }

    /***
     * finds all the example folders for both the version including and without version libraries
     * 
     * @param location
     *            The parent folder of the libraries
     */
    private void getLibExampleFolders(IPath LibRoot) {

	String[] Libs = LibRoot.toFile().list();
	if (Libs == null) {
	    // Either dir does not exist or is not a directory
	} else {
	    java.util.Arrays.sort(Libs, String.CASE_INSENSITIVE_ORDER);
	    for (String curLib : Libs) {
		IPath Lib_examples = LibRoot.append(curLib).append("examples");//$NON-NLS-1$
		IPath Lib_Examples = LibRoot.append(curLib).append("Examples");//$NON-NLS-1$
		if (Lib_examples.toFile().isDirectory()) {
		    getExampleFolders(curLib, Lib_examples);
		} else if (Lib_Examples.toFile().isDirectory()) {
		    getExampleFolders(curLib, Lib_Examples);
		} else // nothing found directly so maybe this is a version based lib
		{
		    String[] versions = LibRoot.append(curLib).toFile().list();
		    if (versions != null) {
			if (versions.length == 1) {// There can only be 1 version of a lib
			    Lib_examples = LibRoot.append(curLib).append(versions[0]).append("examples");//$NON-NLS-1$
			    Lib_Examples = LibRoot.append(curLib).append(versions[0]).append("Examples");//$NON-NLS-1$
			    if (Lib_examples.toFile().isDirectory()) {
				getExampleFolders(curLib, Lib_examples);
			    } else if (Lib_Examples.toFile().isDirectory()) {
				getExampleFolders(curLib, Lib_Examples);
			    }
			}
		    }
		}
	    }
	}
    }

    @Override
    public void setEnabled(boolean enable) {
	this.sampleTree.setEnabled(enable);
	this.myLabel.setEnabled(enable);
    }

    private void recursiveCopySelectedExamples(IProject project, IPath target, TreeItem TreeItem, boolean link) throws IOException {
	for (TreeItem curchildTreeItem : TreeItem.getItems()) {
	    if (curchildTreeItem.getChecked() && (curchildTreeItem.getData("examplePath") != null)) { //$NON-NLS-1$
		String location = (String) curchildTreeItem.getData("examplePath"); //$NON-NLS-1$
		Path locationPath = new Path(location);
		if (link) {
		    ArduinoHelpers.linkDirectory(project, locationPath, target);
		} else {
		    FileUtils.copyDirectory(locationPath.toFile(), project.getLocation().toFile());
		}
	    }
	    recursiveCopySelectedExamples(project, target, curchildTreeItem, link);
	}
    }

    public void CopySelectedExamples(IProject project, IPath target, boolean link) throws IOException {
	this.sampleTree.getItems();
	for (TreeItem curTreeItem : this.sampleTree.getItems()) {
	    recursiveCopySelectedExamples(project, target, curTreeItem, link);
	}
    }

    private void recursiveImportSelectedLibraries(IProject project, ICConfigurationDescription configurationDescriptions[], TreeItem curTreeItem) {
	for (TreeItem curchildTreeItem : curTreeItem.getItems()) {
	    if (curchildTreeItem.getChecked() && (curchildTreeItem.getData("libPath") != null)) { //$NON-NLS-1$
		String location = (String) curchildTreeItem.getData("libPath"); //$NON-NLS-1$
		String LibName = (String) curchildTreeItem.getData("libName"); //$NON-NLS-1$

		try {
		    ArduinoHelpers.addCodeFolder(project, new Path(location), ArduinoConst.WORKSPACE_LIB_FOLDER + LibName, configurationDescriptions);
		} catch (CoreException e) {
		    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, Messages.error_failed_to_import_library_in_project, e));
		}
		break;
	    }
	    recursiveImportSelectedLibraries(project, configurationDescriptions, curchildTreeItem);
	}

    }

    public void importSelectedLibraries(IProject project, ICConfigurationDescription configurationDescriptions[]) {
	this.sampleTree.getItems();
	for (TreeItem curTreeItem : this.sampleTree.getItems()) {
	    recursiveImportSelectedLibraries(project, configurationDescriptions, curTreeItem);
	}

    }
}
