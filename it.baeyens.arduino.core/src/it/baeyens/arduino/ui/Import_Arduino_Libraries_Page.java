package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ArduinoHelpers;

import java.io.File;
import java.net.URI;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.WizardResourceImportPage;

public class Import_Arduino_Libraries_Page extends WizardResourceImportPage {

    protected Tree myLibrarySelector;
    TreeItem myArduinoLibItem = null;
    TreeItem myPersonalLibItem = null;
    TreeItem myArduinoHardwareLibItem = null;

    private IProject myProject = null;

    protected Import_Arduino_Libraries_Page(IProject project, String name, IStructuredSelection selection) {

	super(name, selection);
	setTitle("Import Arduino libraries");
	if (project != null) {
	    myProject = project;
	    setContainerFieldValue(project.getName());
	    setDescription("Use this page to select the libraries to import to project: " + myProject.getName());
	} else {
	    setDescription("As no project is selected it is not possible to import libraries");
	}

    }

    @Override
    public void createControl(Composite parent) {

	initializeDialogUnits(parent);

	Composite composite = new Composite(parent, SWT.NULL);
	composite.setLayout(new GridLayout());
	composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
	composite.setSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	composite.setFont(parent.getFont());

	createSourceGroup(composite);

	// createDestinationGroup(composite);

	// createOptionsGroup(composite);

	// restoreWidgetValues();
	// updateWidgetEnablements();
	setPageComplete(true);
	setErrorMessage(null); // should not initially have error message

	setControl(composite);
    }

    @Override
    protected void createSourceGroup(Composite parent) {
	if (myProject == null)
	    return;
	Composite composite = new Composite(parent, SWT.NONE);
	GridLayout theGridLayout = new GridLayout();
	theGridLayout.numColumns = 1;
	composite.setLayout(theGridLayout);
	composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	composite.setFont(parent.getFont());

	GridData theGriddata;

	myLibrarySelector = new Tree(composite, SWT.CHECK | SWT.BORDER);
	theGriddata = new GridData(SWT.FILL, SWT.FILL, true, true);
	theGriddata.horizontalSpan = 1;
	myLibrarySelector.setLayoutData(theGriddata);
	// Get the data in the tree
	myLibrarySelector.setRedraw(false);

	// IWorkspace workspace = ResourcesPlugin.getWorkspace();
	IPathVariableManager pathMan = myProject.getPathVariableManager();

	URI ArduinoLibraryURI = pathMan.getURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_ARDUINO_LIB);
	URI PrivateLibraryURI = pathMan.getURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_PRIVATE_LIB);
	URI HardwareLibrarURI = pathMan.getURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_HARDWARE_LIB);

	if (HardwareLibrarURI != null) {
	    IPath HardwareLibraryPath = URIUtil.toPath(HardwareLibrarURI);

	    if ((HardwareLibraryPath.toFile().exists()) && (HardwareLibraryPath.toFile().list().length > 0)) {
		// Create Arduino Item
		myArduinoHardwareLibItem = new TreeItem(myLibrarySelector, SWT.NONE);
		myArduinoHardwareLibItem.setText("Arduino Hardware Libraries");
		// Add the Arduino Libs
		AddLibs(myArduinoHardwareLibItem, HardwareLibraryPath);
	    }
	}

	if (ArduinoLibraryURI != null) {
	    IPath ArduinoLibraryPath = URIUtil.toPath(ArduinoLibraryURI);
	    // Create Arduino Item
	    myArduinoLibItem = new TreeItem(myLibrarySelector, SWT.NONE);
	    myArduinoLibItem.setText("Arduino Libraries");
	    // Add the Arduino Libs
	    AddLibs(myArduinoLibItem, ArduinoLibraryPath);
	    myArduinoLibItem.setExpanded(true);
	}

	// Create Personal library Item
	if (PrivateLibraryURI != null) {
	    IPath PrivateLibraryPath = URIUtil.toPath(PrivateLibraryURI);
	    myPersonalLibItem = new TreeItem(myLibrarySelector, SWT.NONE);
	    myPersonalLibItem.setText("Personal Libraries");
	    // Add the personal Libs
	    AddLibs(myPersonalLibItem, PrivateLibraryPath);
	    myLibrarySelector.setRedraw(true);
	    myPersonalLibItem.setExpanded(true);
	}

	myLibrarySelector.addListener(SWT.Selection, new Listener() {
	    @Override
	    public void handleEvent(Event event) {

		if (event.detail == SWT.CHECK) {
		    if ((event.item.equals(myPersonalLibItem)) || (event.item.equals(myArduinoHardwareLibItem))
			    || (event.item.equals(myArduinoLibItem))) {
			event.detail = SWT.NONE;
			event.type = SWT.None;
			event.doit = false;
			try {
			    myLibrarySelector.setRedraw(false);
			    if (myPersonalLibItem != null)
				myPersonalLibItem.setChecked(false);
			    if (myArduinoLibItem != null)
				myArduinoLibItem.setChecked(false);
			    if (myArduinoHardwareLibItem != null)
				myArduinoHardwareLibItem.setChecked(false);
			} finally {
			    myLibrarySelector.setRedraw(true);
			}
		    }
		}
	    }

	});
	// Turn drawing back on!
	myLibrarySelector.setRedraw(true);

    }

    @Override
    protected ITreeContentProvider getFileProvider() {
	return null;
    }

    @Override
    protected ITreeContentProvider getFolderProvider() {
	return null;
    }

    private static void AddLibs(TreeItem LibItem, IPath iPath) {
	File LibRoot = iPath.toFile();
	File LibFolder;
	String[] children = LibRoot.list();
	if (children == null) {
	    // Either dir does not exist or is not a directory
	} else {
	    java.util.Arrays.sort(children, String.CASE_INSENSITIVE_ORDER);
	    for (int i = 0; i < children.length; i++) {
		// Get filename of file or directory
		LibFolder = iPath.append(children[i]).toFile();
		if (LibFolder.isDirectory()) {
		    TreeItem child = new TreeItem(LibItem, SWT.NONE);
		    child.setText(children[i]);
		}
	    }
	}
    }

    public boolean PerformFinish() {
	if (myArduinoLibItem != null) {
	    importLibs(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_ARDUINO_LIB, myArduinoLibItem.getItems());
	}
	if (myPersonalLibItem != null) {
	    importLibs(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_PRIVATE_LIB, myPersonalLibItem.getItems());
	}
	if (myArduinoHardwareLibItem != null) {
	    importLibs(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_HARDWARE_LIB, myArduinoHardwareLibItem.getItems());
	}
	return true;
    }

    private void importLibs(String PathVarName, TreeItem[] AllItems) {
	ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
	ICProjectDescription projectDescription = mngr.getProjectDescription(myProject, true);
	ICConfigurationDescription configurationDescriptions[] = projectDescription.getConfigurations();
	for (int curConfig = 0; curConfig < configurationDescriptions.length; curConfig++) {
	    for (int CurItem = 0; CurItem < AllItems.length; CurItem++) {
		if (AllItems[CurItem].getChecked()) {
		    try {
			ArduinoHelpers.addCodeFolder(myProject, PathVarName, AllItems[CurItem].getText(), ArduinoConst.WORKSPACE_LIB_FOLDER
				+ AllItems[CurItem].getText(), configurationDescriptions[curConfig]);
		    } catch (CoreException e) {
			Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to import library ", e));
		    }
		}
	    }

	    try {
		// projectDescription.(configurationDescription);
		mngr.setProjectDescription(myProject, projectDescription, true, null);
	    } catch (CoreException e) {
		e.printStackTrace();
	    }
	}
    }

}
