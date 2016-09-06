package io.sloeber.core.ui;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import io.sloeber.common.ConfigurationPreferences;
import io.sloeber.common.Const;
import io.sloeber.common.InstancePreferences;

public class SampleSelector {
    private static final String EXAMPLEPATH = "examplePath"; //$NON-NLS-1$
    private static final String LIBNAME = "libName"; //$NON-NLS-1$
    private static final String INO = "ino"; //$NON-NLS-1$
    private static final String PDE = "pde";//$NON-NLS-1$
    protected Tree sampleTree;
    protected Label myLabel;
    TreeMap<String, String> examples = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    protected Listener mylistener;
    protected int numSelected = 0;
    protected Label numSelectedLabel;

    public SampleSelector(Composite composite, int style, String label, int ncols) {

	this.myLabel = new Label(composite, SWT.NONE);
	this.myLabel.setText(label);
	GridData theGriddata = new GridData(SWT.LEFT, SWT.TOP, true, false);
	theGriddata.horizontalSpan = ncols;
	this.myLabel.setLayoutData(theGriddata);

	this.sampleTree = new Tree(composite, SWT.CHECK | SWT.BORDER);
	theGriddata = new GridData(SWT.FILL, SWT.FILL, true, true);
	theGriddata.horizontalSpan = ncols;
	this.sampleTree.setLayoutData(theGriddata);
	// Get the data in the tree
	this.sampleTree.setRedraw(false);

	this.sampleTree.addListener(SWT.Selection, new Listener() {
	    @Override
	    public void handleEvent(Event event) {

		if (event.detail == SWT.CHECK) {
		    TreeItem thechangeItem = (TreeItem) event.item;
		    if (thechangeItem.getItemCount() > 0) {
			event.detail = SWT.NONE;
			event.type = SWT.None;
			event.doit = false;
			thechangeItem.setChecked(!thechangeItem.getChecked());
		    } else {
			if (thechangeItem.getChecked()) {
			    SampleSelector.this.numSelected += 1;
			    SampleSelector.this.numSelectedLabel
				    .setText(Integer.toString(SampleSelector.this.numSelected));
			} else {
			    SampleSelector.this.numSelected -= 1;
			    SampleSelector.this.numSelectedLabel
				    .setText(Integer.toString(SampleSelector.this.numSelected));
			}
			if (SampleSelector.this.mylistener != null) {
			    SampleSelector.this.mylistener.handleEvent(null);
			}

		    }
		}
	    }

	});
	Label label1 = new Label(composite, SWT.NONE);
	label1.setText(Messages.SampleSelector_num_selected);
	this.numSelectedLabel = new Label(composite, SWT.NONE);
	this.numSelectedLabel.setText(Integer.toString(this.numSelected));
	theGriddata = new GridData(SWT.LEFT, SWT.TOP, true, false);
	theGriddata.horizontalSpan = ncols - 2;
	this.numSelectedLabel.setLayoutData(theGriddata);

	this.sampleTree.setRedraw(true);

    }

    /**
     * This method adds all examples to the selection listbox All examples
     * already in the listbox are removed first.
     * 
     * @param paths
     * 
     * @param arduinoExample
     *            The folder with the arduino samples
     * @param privateLibrary
     *            The folder with the private libraries
     * @param hardwareLibrary
     *            The folder with the hardware libraries
     * @param mPlatformPathPath
     */

    public void AddAllExamples(String selectedPlatformLocation, Path[] paths) {
	this.numSelected = 0;

	// Get the examples of the library manager installed libraries
	String libLocations[] = InstancePreferences.getPrivateLibraryPaths();
	File exampleLocation = new File(ConfigurationPreferences.getInstallationPathExamples().toString());

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

	if (exampleLocation.exists()) {
	    getExamplesFromFolder(Const.EMPTY_STRING, exampleLocation);
	}

	// Get the examples of the libraries from the selected hardware
	// This one should be the last as hasmap overwrites doubles. This way
	// hardware libraries are preferred to others
	if (selectedPlatformLocation != null) {
	    if (new File(selectedPlatformLocation).exists()) {
		getLibExampleFolders(new Path(selectedPlatformLocation).append(Const.LIBRARY_PATH_SUFFIX));
	    }
	}

	this.sampleTree.removeAll();

	// Add the examples to the tree
	for (Map.Entry<String, String> entry : this.examples.entrySet()) {
	    String keys[] = entry.getKey().split("-"); //$NON-NLS-1$
	    TreeItem curItems[] = this.sampleTree.getItems();
	    TreeItem curItem = findItem(curItems, keys[0]);
	    if (curItem == null) {
		curItem = new TreeItem(this.sampleTree, SWT.NONE);
		curItem.setText(keys[0]);
	    }
	    curItems = this.sampleTree.getItems();
	    TreeItem prefItem = curItem;
	    for (String curKey : keys) {
		curItem = findItem(curItems, curKey);
		if (curItem == null) {

		    curItem = new TreeItem(prefItem, SWT.NONE);
		    curItem.setText(curKey);
		}
		prefItem = curItem;
		curItems = curItem.getItems();
	    }

	    curItem.setData(EXAMPLEPATH, entry.getValue());
	    curItem.setData(LIBNAME, keys[keys.length - 2]);

	}
	// Mark the examples selected
	setLastUsedExamples(paths);
    }

    private static TreeItem findItem(TreeItem items[], String text) {
	for (TreeItem curitem : items) {
	    if (text.equals(curitem.getText())) {
		return curitem;
	    }
	}
	return null;
    }

    /**
     * This method adds a folder of examples. There is no search. The provided
     * folder is assumed to be a tree where the parents of the leaves are
     * assumed examples
     * 
     * @param iPath
     * @param pathVarName
     */
    private void getExampleFolders(String libname, File location) {
	String[] children = location.list();
	if (children == null) {
	    // Either dir does not exist or is not a directory
	} else {
	    java.util.Arrays.sort(children, String.CASE_INSENSITIVE_ORDER);
	    for (String curFolder : children) {
		IPath LibFolder = new Path(location.toString()).append(curFolder);
		if (LibFolder.toFile().isDirectory()) {
		    this.examples.put(libname + '-' + curFolder, LibFolder.toString());
		}
	    }
	}
    }

    /**
     * This method adds a folder recursively examples. Leaves containing ino
     * files are assumed to be examples
     * 
     * @param File
     */
    private void getExamplesFromFolder(String prefix, File location) {
	File[] children = location.listFiles();
	if (children == null) {
	    // Either dir does not exist or is not a directory
	} else {
	    // java.util.Arrays.sort(children, String.CASE_INSENSITIVE_ORDER);
	    for (File exampleFolder : children) {
		// File exampleFolder = (new
		// Path(location.toString()).append(curFolder)).toFile();
		Path pt = new Path(exampleFolder.toString());
		String extension = pt.getFileExtension();
		if (exampleFolder.isDirectory()) {
		    getExamplesFromFolder(prefix + location.getName() + '-', exampleFolder);
		} else if (INO.equalsIgnoreCase(extension) || PDE.equalsIgnoreCase(extension)) {
		    this.examples.put(prefix + location.getName(), location.toString());
		}
	    }
	}
    }

    /***
     * finds all the example folders for both the version including and without
     * version libraries
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
		    getExampleFolders(curLib, Lib_examples.toFile());
		} else if (Lib_Examples.toFile().isDirectory()) {
		    getExampleFolders(curLib, Lib_Examples.toFile());
		} else // nothing found directly so maybe this is a version
		       // based lib
		{
		    String[] versions = LibRoot.append(curLib).toFile().list();
		    if (versions != null) {
			if (versions.length == 1) {// There can only be 1
						   // version of a lib
			    Lib_examples = LibRoot.append(curLib).append(versions[0]).append("examples");//$NON-NLS-1$
			    Lib_Examples = LibRoot.append(curLib).append(versions[0]).append("Examples");//$NON-NLS-1$
			    if (Lib_examples.toFile().isDirectory()) {
				getExampleFolders(curLib, Lib_examples.toFile());
			    } else if (Lib_Examples.toFile().isDirectory()) {
				getExampleFolders(curLib, Lib_Examples.toFile());
			    }
			}
		    }
		}
	    }
	}
    }

    public void setEnabled(boolean enable) {
	this.sampleTree.setEnabled(enable);
	this.myLabel.setEnabled(enable);
    }

    /**
     * is at least 1 sample selected in this tree
     * 
     * @return true if at least one sample is selected. else false
     */
    public boolean isSampleSelected() {
	return this.numSelected > 0;
    }

    /**
     * you can only set 1 listener. The listener is triggered each time a item
     * is selected or deselected
     * 
     * @param listener
     */
    public void addchangeListener(Listener listener) {
	this.mylistener = listener;
    }

    /**
     * Marks the previous selected example(s) as selected and expands the items
     * plus all parent items
     * 
     * @param paths
     */
    private void setLastUsedExamples(Path[] paths) {

	TreeItem[] startIems = this.sampleTree.getItems();
	for (TreeItem curItem : startIems) {
	    recursiveSetExamples(curItem, paths);
	}
	this.numSelectedLabel.setText(Integer.toString(this.numSelected));
    }

    private void recursiveSetExamples(TreeItem curTreeItem, Path[] lastUsedExamples) {
	for (TreeItem curchildTreeItem : curTreeItem.getItems()) {
	    if (curchildTreeItem.getItems().length == 0) {
		for (Path curLastUsedExample : lastUsedExamples) {
		    Path ss = new Path((String) curchildTreeItem.getData(EXAMPLEPATH));
		    if (curLastUsedExample.equals(ss)) {
			curchildTreeItem.setChecked(true);
			curchildTreeItem.setExpanded(true);
			TreeItem parentTreeItem = curTreeItem;
			while (parentTreeItem != null) {
			    parentTreeItem.setExpanded(true);
			    parentTreeItem.setChecked(true);
			    parentTreeItem = parentTreeItem.getParentItem();
			}
			this.numSelected += 1;
		    }
		}
	    } else {
		recursiveSetExamples(curchildTreeItem, lastUsedExamples);
	    }
	}
    }

    public Path[] GetSampleFolders() {
	this.sampleTree.getItems();
	List<Path> ret = new ArrayList<>();
	for (TreeItem curTreeItem : this.sampleTree.getItems()) {
	    ret.addAll(recursiveGetSelectedExamples(curTreeItem));
	}
	return ret.toArray(new Path[0]);
    }

    private List<Path> recursiveGetSelectedExamples(TreeItem TreeItem) {
	List<Path> ret = new ArrayList<>();
	for (TreeItem curchildTreeItem : TreeItem.getItems()) {
	    if (curchildTreeItem.getChecked() && (curchildTreeItem.getData(EXAMPLEPATH) != null)) {
		String location = (String) curchildTreeItem.getData(EXAMPLEPATH);
		ret.add(new Path(location));

	    }
	    ret.addAll(recursiveGetSelectedExamples(curchildTreeItem));
	}
	return ret;
    }
}
