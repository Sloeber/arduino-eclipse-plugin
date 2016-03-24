package it.baeyens.arduino.ui;

import java.util.HashMap;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ComboBoxViewerCellEditor;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

import it.baeyens.arduino.managers.Library;
import it.baeyens.arduino.managers.LibraryTree;
import it.baeyens.arduino.managers.Manager;

public class LibraryPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	protected HashMap<String, String> ModdedLibraries = null;
	private Table table;
	ModifyListener tt = new ModifyListener() {

		@Override
		public void modifyText(ModifyEvent e) {
			// TODO Auto-generated method stub
			CCombo theCombo = (CCombo) e.getSource();
			String libname = (String) theCombo.getData();
			String version = theCombo.getText();
			LibraryPreferencePage.this.ModdedLibraries.put(libname, version);
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
		createTree(control);

		// Composite tableComp = new Composite(control, SWT.NONE);
		// tableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
		// true));

		// this.table = new Table(tableComp, SWT.SINGLE | SWT.BORDER |
		// SWT.V_SCROLL | SWT.FULL_SELECTION);
		// this.table.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
		// true));
		// this.table.setHeaderVisible(true);
		// this.table.setLinesVisible(true);
		//
		// TableColumn categoryColumn = new TableColumn(this.table, SWT.LEAD);
		// categoryColumn.setText(Messages.ui_category);
		//
		// TableColumn LibNameColumn = new TableColumn(this.table, SWT.LEAD);
		// LibNameColumn.setText(Messages.ui_name);
		//
		// TableColumn VersionsColumn = new TableColumn(this.table, SWT.LEAD);
		// VersionsColumn.setText(Messages.ui_version);
		//
		// TableColumnLayout tableLayout = new TableColumnLayout();
		// tableLayout.setColumnData(categoryColumn, new ColumnWeightData(5,
		// 150, true));
		// tableLayout.setColumnData(LibNameColumn, new ColumnWeightData(5, 150,
		// true));
		// tableLayout.setColumnData(VersionsColumn, new ColumnWeightData(2, 75,
		// true));
		// tableComp.setLayout(tableLayout);
		//
		// updateTable();

		return control;
	}

	protected IStatus updateInstallation(IProgressMonitor monitor) {
		MultiStatus status = new MultiStatus(Activator.getId(), 0, Messages.ui_installing_arduino_libraries, null);

		if (this.ModdedLibraries != null) {
			if (this.ModdedLibraries.size() > 0) {

				for (Entry<String, String> curTableItem : this.ModdedLibraries.entrySet()) {
					String Version = curTableItem.getValue();
					String libName = curTableItem.getKey();
					Library removeLib = Manager.getLibraryIndex().getInstalledLibrary(libName);
					if (removeLib != null) {
						if (!(removeLib.getVersion().equals(Version))) {
							status.add(removeLib.remove(monitor));
						}

					}
					Library curLib = Manager.getLibraryIndex().getLibrary(libName, Version);
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

	private LibraryTree tree;
	private ComboBoxViewerCellEditor cellEditor;

	public void createTree(Composite parent) {
//		PatternFilter filter = new PatternFilter();
//		FilteredTree tree = new FilteredTree(parent, SWT.MULTI | SWT.H_SCROLL
//				| SWT.V_SCROLL, filter, true);
//
//		TreeViewer viewer = tree.getViewer();
//		viewer.setContentProvider(new LibraryContentProvider());
//		viewer.setLabelProvider(new LibraryLabelProvider());
//		viewer.setInput(new LibraryTree());
		
		
		
		Tree tree = new Tree(parent, SWT.CHECK | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION);
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		TreeColumn column1 = new TreeColumn(tree, SWT.LEFT);
		column1.setText("Column 1");
		column1.setWidth(200);
		TreeColumn column2 = new TreeColumn(tree, SWT.LEFT);
		column2.setText("Column 2");
		column2.setWidth(200);

//		for (int i = 0; i < 3; i++) {
//			TreeItem item = new TreeItem(tree, SWT.NONE);
//			item.setText("Category " + i);
//			for (int j = 0; j < 3; j++) {
//				TreeItem subItem = new TreeItem(item, SWT.CHECK);
//				subItem.setText(new String[] { "Library " + j, (j == 1 ? "1.0.3" : "") });
//				subItem.setChecked(j == 1);
//			}
//		}

		// Create the editor and set its attributes
		final TreeEditor editor = new TreeEditor(tree);
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;
		editor.setColumn(1);
		String[] options = new String[] { "1.0.0", "1.0.1", "1.0.2", "1.0.3" };

		tree.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (editor.getEditor() != null) {
					editor.getEditor().dispose();
				}
				final TreeItem item = event.item instanceof TreeItem ? (TreeItem)event.item : null;
				if (item != null && event.detail == SWT.CHECK) {
					if (item.getItemCount() > 0) {
						for (TreeItem child : item.getItems()) {
							child.setChecked(item.getChecked());
							if (item.getChecked()) {
								child.setText(1, ((LibraryTree.Library)child.getData()).getLatest());
							} else {
								child.setText(1, "");
							}
						}
					} else {
						if (item.getChecked()) {
							item.setText(1, ((LibraryTree.Library)item.getData()).getLatest());
						} else {
							item.setText(1, "");
							if (item.getParentItem().getChecked()) {
								item.getParentItem().setGrayed(true);
							}
						}
					}
				}
				if (item != null && item.getItemCount() == 0 && item.getChecked()) {
					// Create the dropdown and add data to it
					
					final CCombo combo = new CCombo(tree, SWT.READ_ONLY);
					for (LibraryTree.Version version : ((LibraryTree.Library)item.getData()).getVersions()) {
						combo.add(version.toString());
					}
					
					// Select the previously selected item from the cell
					combo.select(combo.indexOf(item.getText(1)));

					// Compute the width for the editor
					// Also, compute the column width, so that the dropdown fits
					// editor.minimumWidth = combo.computeSize(SWT.DEFAULT,
					// SWT.DEFAULT).x;
					// column2.setWidth(editor.minimumWidth);

					// Set the focus on the dropdown and set into the editor
					combo.setFocus();
					editor.setEditor(combo, item, 1);

					// Add a listener to set the selected item back into the
					// cell
					combo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent event) {
							item.setText(1, combo.getText());

							// They selected an item; end the editing session
							combo.dispose();
						}
					});
				}
			}
		});
		
		TreeViewer viewer = new TreeViewer(tree);
		viewer.setContentProvider(new LibraryContentProvider());
		viewer.setLabelProvider(new LibraryLabelProvider());
		viewer.setInput(new LibraryTree());
		viewer.addFilter(new PatternFilter());
	}
	
	static class LibraryLabelProvider implements ITableLabelProvider {

		@Override
		public void addListener(ILabelProviderListener arg0) { }

		@Override
		public void dispose() {	}

		@Override
		public boolean isLabelProperty(Object arg0, String arg1) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener arg0) { }

		@Override
		public Image getColumnImage(Object element, int col) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int col) {
			switch (col) {
				case 0:
					return ((LibraryTree.Node)element).getName();
				case 1:
					if (element instanceof LibraryTree.Library) {
						return ((LibraryTree.Library)element).getInstalled();
					} else {
						return "";
					}
			}
			return null;
		}}

	static class LibraryContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object node) {
			return ((LibraryTree.Node) node).getChildren();
		}

		@Override
		public Object getParent(Object node) {
			return ((LibraryTree.Library) node).getParent();
		}

		@Override
		public boolean hasChildren(Object node) {
			if (node instanceof LibraryTree) {
				return !((LibraryTree)node).getCategories().isEmpty();
			}
			return ((LibraryTree.Node) node).hasChildren();
		}

		@Override
		public Object[] getElements(Object node) {
			if (node instanceof LibraryTree) {
				return ((LibraryTree)node).getCategories().toArray();
			}
			return getChildren(node);
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
		}

		public Object[] getValue(Object node) {
			if (node instanceof LibraryTree.Library) {
				return ((LibraryTree.Library) node).getVersions().toArray();
			} else {
				return null;
			}
		}
	}
}
