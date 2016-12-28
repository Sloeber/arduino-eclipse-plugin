package io.sloeber.ui;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.WizardResourceImportPage;

import io.sloeber.core.api.Sketch;

public class Import_Libraries_Page extends WizardResourceImportPage {

    protected Tree myLibrarySelector;

    private IProject myProject = null;

    protected Import_Libraries_Page(IProject project, String name, IStructuredSelection selection) {

	super(name, selection);
	setTitle(Messages.ui_import_arduino_libraries_in_project);
	if (project != null) {
	    this.myProject = project;
	    setContainerFieldValue(project.getName());
	    setDescription(Messages.ui_import_arduino_libraries_in_project_help + this.myProject.getName());
	} else {
	    setDescription(Messages.ui_error_select_arduino_project);
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
	if (this.myProject == null)
	    return;
	Composite composite = new Composite(parent, SWT.NONE);
	GridLayout theGridLayout = new GridLayout();
	theGridLayout.numColumns = 1;
	composite.setLayout(theGridLayout);
	composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	composite.setFont(parent.getFont());

	GridData theGriddata;

	this.myLibrarySelector = new Tree(composite, SWT.CHECK | SWT.BORDER);
	theGriddata = new GridData(SWT.FILL, SWT.FILL, true, true);
	theGriddata.horizontalSpan = 1;
	this.myLibrarySelector.setLayoutData(theGriddata);

	// find the items to add to the list
	Map<String, IPath> allLibraries = Sketch.getAllAvailableLibraries(this.myProject);

	// Get the data in the tree
	Set<String> allLibrariesAlreadyUsed = Sketch.getAllImportedLibraries(this.myProject);
	this.myLibrarySelector.setRedraw(false);
	for (Entry<String, IPath> curlib : allLibraries.entrySet()) {
	    TreeItem child = new TreeItem(this.myLibrarySelector, SWT.NONE);
	    child.setText(curlib.getKey());
	    if (allLibrariesAlreadyUsed.contains(curlib.getKey()))
		child.setChecked(true);
	}

	this.myLibrarySelector.setRedraw(true);

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
	// check if there is a incompatibility in the library folder name
	// windows only
	if (Platform.getOS().equals(Platform.OS_WIN32)) {
	    // TODO libraries should not be hardcoded here
	    IFolder folder = this.myProject.getFolder("libraries"); //$NON-NLS-1$
	    if (!folder.exists()) {
		try {
		    folder.create(false, true, null);
		} catch (CoreException e) {
		    Activator.log(new Status(IStatus.ERROR, Activator.getId(),
			    "Failed to create \"libraries\" folder.\nThis is probably a windows case insensetivity problem", //$NON-NLS-1$
			    e));
		    return true;
		}

	    }
	}
	TreeItem selectedTreeItems[] = this.myLibrarySelector.getItems();
	Set<String> selectedLibraries = new TreeSet<>();
	Set<String> unselectedLibraries = new TreeSet<>();
	for (TreeItem CurItem : selectedTreeItems) {
	    if (CurItem.getChecked())
		selectedLibraries.add(CurItem.getText());
	    else
		unselectedLibraries.add(CurItem.getText());
	}
	Sketch.removeLibrariesFromProject(this.myProject, unselectedLibraries);
	Sketch.addLibrariesToProject(this.myProject, selectedLibraries);

	return true;
    }

}
