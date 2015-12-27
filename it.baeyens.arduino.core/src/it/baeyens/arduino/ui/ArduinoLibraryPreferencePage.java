package it.baeyens.arduino.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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

    protected HashMap<String, String> ModdedLibraries = null;
    private Table table;
    ModifyListener tt = new ModifyListener() {

	@Override
	public void modifyText(ModifyEvent e) {
	    // TODO Auto-generated method stub
	    CCombo theCombo = (CCombo) e.getSource();
	    String libname = (String) theCombo.getData();
	    String version = theCombo.getText();
	    ArduinoLibraryPreferencePage.this.ModdedLibraries.put(libname, version);
	}
    };

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
	categoryColumn.setText(Messages.ui_category);

	TableColumn LibNameColumn = new TableColumn(this.table, SWT.LEAD);
	LibNameColumn.setText(Messages.ui_name);

	TableColumn VersionsColumn = new TableColumn(this.table, SWT.LEAD);
	VersionsColumn.setText(Messages.ui_version);

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

	this.ModdedLibraries = new HashMap<>();
	this.table.removeAll();
	LibraryIndex libraryIndex = ArduinoManager.getLibraryIndex();
	Set<String> categories = libraryIndex.getCategories();

	for (String curCategory : categories) {
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
		    libraryItem.setText(0, curCategory);
		    libraryItem.setText(1, curLibrary.getName());

		    TableEditor editor = new TableEditor(this.table);
		    if (combo != null) {
			combo.addModifyListener(this.tt);
		    }
		    combo = new CCombo(this.table, SWT.BORDER | SWT.READ_ONLY);
		    combo.add(Messages.ui_remove);
		    editor.grabHorizontal = true;
		    editor.setEditor(combo, libraryItem, 2);
		    combo.setData(curLibrary.getName());

		}
		if ((libraryItem != null) && (combo != null)) {
		    combo.add(curLibrary.getVersion());
		    if (curLibrary.isInstalled()) {
			libraryItem.setText(3, curLibrary.getName());
			combo.setText(curLibrary.getVersion());

		    }
		}
	    }
	    if (combo != null) {
		combo.addModifyListener(this.tt);
	    }
	}
    }

    protected IStatus updateInstallation(IProgressMonitor monitor) {
	MultiStatus status = new MultiStatus(Activator.getId(), 0, Messages.ui_installing_arduino_libraries, null);

	if (this.ModdedLibraries != null) {
	    if (this.ModdedLibraries.size() > 0) {

		for (Entry<String, String> curTableItem : this.ModdedLibraries.entrySet()) {
		    String Version = curTableItem.getValue();
		    String libName = curTableItem.getKey();
		    ArduinoLibrary removeLib = ArduinoManager.getLibraryIndex().getInstalledLibrary(libName);
		    if (removeLib != null) {
			if (!(removeLib.getVersion().equals(Version))) {
			    status.add(removeLib.remove(monitor));
			}

		    }
		    ArduinoLibrary curLib = ArduinoManager.getLibraryIndex().getLibrary(libName, Version);
		    if (curLib != null) {
			if (!curLib.isInstalled()) {
			    status.add(curLib.install(monitor));
			}
		    }
		}
	    }
	}
	return status;
    }

    @Override
    public boolean performOk() {
	new Job(Messages.ui_Adopting_arduino_libraries) {

	    @Override
	    protected IStatus run(IProgressMonitor monitor) {

		return updateInstallation(monitor);

	    }
	}.schedule();

	return true;
    }

}
