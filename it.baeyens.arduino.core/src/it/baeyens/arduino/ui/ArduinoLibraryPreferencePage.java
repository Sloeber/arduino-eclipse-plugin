package it.baeyens.arduino.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import it.baeyens.arduino.managers.ArduinoLibrary;
import it.baeyens.arduino.managers.ArduinoManager;
import it.baeyens.arduino.managers.LibraryIndex;

public class ArduinoLibraryPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    protected List<ArduinoLibrary> removeLibraries = null;
    protected List<ArduinoLibrary> addLibraries = null;
    private Table table;

    @Override
    public void init(IWorkbench workbench) {
	// nothing needed here
    }

    @Override
    protected Control createContents(Composite parent) {
	Composite control = new Composite(parent, SWT.NONE);
	control.setLayout(new GridLayout());

	Text desc = new Text(control, SWT.READ_ONLY);
	GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
	layoutData.widthHint = 500;
	desc.setLayoutData(layoutData);
	desc.setBackground(parent.getBackground());
	desc.setText("remove or add checkboxes to update your libraries."); //$NON-NLS-1$

	Composite tableComp = new Composite(control, SWT.NONE);
	tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

	this.table = new Table(tableComp, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.FULL_SELECTION);
	this.table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	this.table.setHeaderVisible(true);
	this.table.setLinesVisible(true);

	TableColumn categoryColumn = new TableColumn(this.table, SWT.LEAD);
	categoryColumn.setText("category");

	TableColumn LibNameColumn = new TableColumn(this.table, SWT.LEAD);
	LibNameColumn.setText("name");

	TableColumn VersionsColumn = new TableColumn(this.table, SWT.LEAD);
	VersionsColumn.setText("version");

	TableColumnLayout tableLayout = new TableColumnLayout();
	tableLayout.setColumnData(categoryColumn, new ColumnWeightData(5, 150, true));
	tableLayout.setColumnData(LibNameColumn, new ColumnWeightData(5, 150, true));
	tableLayout.setColumnData(VersionsColumn, new ColumnWeightData(2, 75, true));
	tableComp.setLayout(tableLayout);

	updateTable();

	return control;
    }

    private void updateTable() {
	if (this.table == null || this.table.isDisposed()) {
	    return;
	}

	this.table.removeAll();
	LibraryIndex libraryIndex = ArduinoManager.getLibraryIndex();
	Set<String> categories = libraryIndex.getCategories();

	for (String curCategory : categories) {
	    // TreeItem packageItem = new TreeItem(this.platformTree, SWT.NONE);
	    // packageItem.setText(curCategory);
	    Collection<ArduinoLibrary> libraries = libraryIndex.getLibraries(curCategory);
	    List<ArduinoLibrary> librarylist = new ArrayList<>(libraries);
	    Collections.sort(librarylist, new ArduinoLibrary());

	    String prefLibraryName = null;
	    TableItem libraryItem = null;
	    CCombo combo = null;

	    for (ArduinoLibrary curLibrary : librarylist) {

		if (!curLibrary.getName().equals(prefLibraryName)) {
		    libraryItem = new TableItem(this.table, SWT.NONE);
		    prefLibraryName = curLibrary.getName();
		    libraryItem.setData(curLibrary);
		    libraryItem.setText(0, curCategory);
		    libraryItem.setText(1, curLibrary.getName());

		    TableEditor editor = new TableEditor(this.table);
		    combo = new CCombo(this.table, SWT.BORDER | SWT.READ_ONLY);
		    combo.add("remove");
		    combo.add("");
		    combo.add(curLibrary.getVersion());
		    editor.grabHorizontal = true;
		    editor.setEditor(combo, libraryItem, 2);

		}
		if ((libraryItem != null) && (combo != null)) {
		    combo.add(curLibrary.getVersion());
		    if (curLibrary.isInstalled()) {
			libraryItem.setText(3, curLibrary.getName());
			combo.setText(curLibrary.getVersion());

		    }
		}
	    }
	}
    }

    protected IStatus updateInstallation(IProgressMonitor monitor) {
	MultiStatus status = new MultiStatus(Activator.getId(), 0, "Installing Arduino Board Platforms", null);

	for (ArduinoLibrary curLibrary : this.removeLibraries) {
	    status.add(curLibrary.remove(monitor));
	}
	for (ArduinoLibrary curLibrary : this.addLibraries) {
	    status.add(curLibrary.install(monitor));
	}

	return status;
    }

    private void markInstallationChanges() {
	this.removeLibraries = new ArrayList<>();
	this.addLibraries = new ArrayList<>();

	for (TableItem curTableItem : this.table.getItems()) {
	    ArduinoLibrary curLib = (ArduinoLibrary) curTableItem.getData();
	    String Version = curTableItem.getText(3);
	    if (curLib.isInstalled() && !(curLib.getVersion().equals(Version))) {
		this.removeLibraries.add(curLib);
	    } else if (!curLib.isInstalled() && (curLib.getVersion().equals(Version))) {
		this.addLibraries.add(curLib);
	    }
	}
    }

    @Override
    public boolean performOk() {
	markInstallationChanges();
	new Job("Adopting Arduino Board Platforms") {

	    @Override
	    protected IStatus run(IProgressMonitor monitor) {

		return updateInstallation(monitor);

	    }
	}.schedule();

	return true;
    }

}
