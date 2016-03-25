package it.baeyens.arduino.ui;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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

	private Tree tree;
	private CheckboxTreeViewer viewer;
	private TreeEditor editor;
	
	@Override
	public void init(IWorkbench workbench) {
		// nothing needed here
	}

	@Override
	protected void performDefaults() {
		viewer.setInput(new LibraryTree());
		editor.getEditor().dispose();
		super.performDefaults();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite control = new Composite(parent, SWT.NONE);
		control.setLayout(new GridLayout());

		Text desc = new Text(control, SWT.READ_ONLY);
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
		desc.setLayoutData(layoutData);
		desc.setBackground(parent.getBackground());
		desc.setText("Add/remove libraries or change their version.");
		this.createTree(control);

		return control;
	}

	@Override
	public boolean performOk() {
		final HashMap<String, String> libs = new HashMap<>();
		for (TreeItem category : tree.getItems()) {
			for (TreeItem  item : category.getItems()) {
				if (item.getData() instanceof LibraryTree.Library) {
					libs.put(item.getText(0), item.getText(1));
				}
			}
		}
		new Job(Messages.ui_Adopting_arduino_libraries) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				MultiStatus status = new MultiStatus(Activator.getId(), 0, Messages.ui_installing_arduino_libraries, null);
				for (Map.Entry<String, String> lib : libs.entrySet()) {
						Library toRemove = Manager.getLibraryIndex().getInstalledLibrary(lib.getKey());
						if (toRemove != null && !toRemove.getVersion().equals(lib.getValue())) {
							status.add(toRemove.remove(monitor));
						}
						Library toInstall = Manager.getLibraryIndex().getLibrary(lib.getKey(), lib.getValue());
						if (toInstall != null && !toInstall.isInstalled()) {
							status.add(toInstall.install(monitor));
						}
				}
				return status;
			}
		}.schedule();

		return true;
	}
	
	public void createTree(Composite parent) {
		// filtering applied to all columns
		PatternFilter filter = new PatternFilter() {
			protected boolean isLeafMatch(final Viewer viewer, final Object element) {
				TreeViewer treeViewer = (TreeViewer) viewer;
				int numberOfColumns = treeViewer.getTree().getColumnCount();
				ITableLabelProvider labelProvider = (ITableLabelProvider) treeViewer.getLabelProvider();
				boolean isMatch = false;
				for (int columnIndex = 0; columnIndex < numberOfColumns; columnIndex++) {
					String labelText = labelProvider.getColumnText(element, columnIndex);
					isMatch |= wordMatches(labelText);
				}
				return isMatch;
			}
		};
		
		tree = new FilteredTree(parent, SWT.CHECK | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION, filter, true).getViewer().getTree();

		// trick to replace the tree viewer
		viewer = new CheckboxTreeViewer(tree);
		viewer.setLabelProvider(new LibraryLabelProvider());
		viewer.setContentProvider(new LibraryContentProvider());
		viewer.setCheckStateProvider(new LibraryCheckProvider());
		viewer.setInput(new LibraryTree());

		TreeColumn name = new TreeColumn(viewer.getTree(), SWT.LEFT);
		name.setWidth(300);

		TreeColumn version = new TreeColumn(viewer.getTree(), SWT.LEFT);
		version.setWidth(100);

		// create the editor and set its attributes
		editor = new TreeEditor(viewer.getTree());
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;
		editor.setColumn(1);

		// this ensures the tree labels are displayed correctly
		viewer.refresh(true);

		// tree interactions listener
		viewer.getTree().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (editor.getEditor() != null) {
					editor.getEditor().dispose();
				}
				final TreeItem item = event.item instanceof TreeItem ? (TreeItem) event.item : null;
				if (item != null && event.detail == SWT.CHECK) {
					if (item.getItemCount() > 0) {
						item.setGrayed(false);
						for (TreeItem child : item.getItems()) {
							child.setChecked(item.getChecked());
							if (child.getChecked()) {
								child.setText(1, ((LibraryTree.Library) child.getData()).getLatest());
							} else {
								child.setText(1, "");
							}
						}
					} else {
						if (item.getChecked()) {
							item.setText(1, ((LibraryTree.Library) item.getData()).getLatest());
						} else {
							item.setText(1, "");
						}
						verifySubtreeCheckStatus(item.getParentItem());
					}
				}
				if (item != null && item.getItemCount() == 0 && item.getChecked()) {
					// Create the dropdown and add data to it
					final CCombo combo = new CCombo(viewer.getTree(), SWT.READ_ONLY);
					for (LibraryTree.Version version : ((LibraryTree.Library) item.getData()).getVersions()) {
						combo.add(version.toString());
					}

					// Select the previously selected item from the cell
					combo.select(combo.indexOf(item.getText(1)));

					// Compute the width for the editor
					// Also, compute the column width, so that the dropdown fits

					// Set the focus on the dropdown and set into the editor
					combo.setFocus();
					editor.setEditor(combo, item, 1);

					// Add a listener to set the selected item back into the
					// cell
					combo.addSelectionListener(new SelectionAdapter() {
						public void widgetSelected(SelectionEvent event) {
							item.setText(1, combo.getText());
							// Item selected: end the editing session
							combo.dispose();
						}
					});
				}
			}
		});
	}

	/**
	 * Ensures the correct checked/unchecked/greyed attributes are set
	 * on the category.
	 * 
	 * @param item the tree item representing the category
	 */
	private void verifySubtreeCheckStatus(TreeItem item) {
		boolean grayed = false;
		boolean checked = false;
		for (TreeItem child : item.getItems()) {
			if (child.getChecked()) {
				checked = true;
			} else {
				grayed = true;
			}
		}
		item.setChecked(checked);
		item.setGrayed(grayed);
	}

	/**
	 * 
	 * Displays the tree labels for both columns: name and version
	 *
	 */
	private static class LibraryLabelProvider implements ITableLabelProvider {

		@Override
		public void addListener(ILabelProviderListener arg0) {
		}

		@Override
		public void dispose() {
		}

		@Override
		public boolean isLabelProperty(Object arg0, String arg1) {
			return false;
		}

		@Override
		public void removeListener(ILabelProviderListener arg0) {
		}

		@Override
		public Image getColumnImage(Object element, int col) {
			return null;
		}

		@Override
		public String getColumnText(Object element, int col) {
			switch (col) {
			case 0:
				return ((LibraryTree.Node) element).getName();
			case 1:
				if (element instanceof LibraryTree.Library) {
					return ((LibraryTree.Library) element).getInstalled();
				} else {
					return "";
				}
			}
			return null;
		}
	}

	/**
	 * Provides the correct checked status for installed libraries
	 *
	 */
	private static class LibraryCheckProvider implements ICheckStateProvider {
		@Override
		public boolean isChecked(Object element) {
			if (element instanceof LibraryTree.Library) {
				return ((LibraryTree.Library) element).getInstalled() != null;
			} else if (element instanceof LibraryTree.Category) {
				for (LibraryTree.Library library : ((LibraryTree.Category) element).getLibraries()) {
					if (library.getInstalled() != null) {
						return true;
					}
				}
			}
			return false;
		}

		@Override
		public boolean isGrayed(Object element) {
			if (element instanceof LibraryTree.Category && isChecked(element)) {
				for (LibraryTree.Library library : ((LibraryTree.Category) element).getLibraries()) {
					if (library.getInstalled() == null) {
						return true;
					}
				}
			}
			return false;
		}
	}

	/**
	 * Provides the tree content data
	 *
	 */
	private static class LibraryContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object node) {
			return ((LibraryTree.Node) node).getChildren();
		}

		@Override
		public Object getParent(Object node) {
			if (node instanceof LibraryTree.Node) {
				return ((LibraryTree.Node) node).getParent();
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object node) {
			if (node instanceof LibraryTree) {
				return !((LibraryTree) node).getCategories().isEmpty();
			}
			return ((LibraryTree.Node) node).hasChildren();
		}

		@Override
		public Object[] getElements(Object node) {
			if (node instanceof LibraryTree) {
				return ((LibraryTree) node).getCategories().toArray();
			}
			return getChildren(node);
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
		}
	}
}
