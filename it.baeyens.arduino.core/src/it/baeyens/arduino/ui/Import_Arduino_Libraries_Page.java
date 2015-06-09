package it.baeyens.arduino.ui;

import it.baeyens.arduino.tools.ArduinoLibraries;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.WizardResourceImportPage;

public class Import_Arduino_Libraries_Page extends WizardResourceImportPage {

    protected Tree myLibrarySelector;

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

	// find the items to add to the list
	Set<String> allLibraries = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
	allLibraries.addAll(ArduinoLibraries.findAllHarwareLibraries(myProject));
	allLibraries.addAll(ArduinoLibraries.findAllUserLibraries(myProject));
	allLibraries.addAll(ArduinoLibraries.findAllArduinoLibraries(myProject));

	// Get the data in the tree
	Set<String> allLibrariesAlreadyUsed = ArduinoLibraries.getAllLibrariesFromProject(myProject);
	myLibrarySelector.setRedraw(false);
	Iterator<String> iterator = allLibraries.iterator();
	while (iterator.hasNext()) {
	    TreeItem child = new TreeItem(myLibrarySelector, SWT.NONE);
	    String nextLibrary = iterator.next();
	    child.setText(nextLibrary);
	    if (allLibrariesAlreadyUsed.contains(nextLibrary))
		child.setChecked(true);
	}

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

    public boolean PerformFinish() {
	TreeItem selectedTreeItems[] = myLibrarySelector.getItems();
	Set<String> selectedLibraries = new TreeSet<String>();
	Set<String> unselectedLibraries = new TreeSet<String>();
	for (TreeItem CurItem : selectedTreeItems) {
	    if (CurItem.getChecked())
		selectedLibraries.add(CurItem.getText());
	    else
		unselectedLibraries.add(CurItem.getText());
	}
	ArduinoLibraries.removeLibrariesFromProject(myProject, unselectedLibraries);
	ArduinoLibraries.addLibrariesToProject(myProject, selectedLibraries);

	return true;
    }

}
