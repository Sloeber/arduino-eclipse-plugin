package it.baeyens.arduino.ui;

import java.util.Collection;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

import it.baeyens.arduino.managers.Library;
import it.baeyens.arduino.managers.LibraryIndex;
import it.baeyens.arduino.managers.Manager;

public class LibraryPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	private FilteredTree tree;
	private TreeViewer viewer;
	private TreeEditor editor;
	private LibraryTree libs = new LibraryTree();

	@Override
	public void init(IWorkbench workbench) {
		// nothing needed here
	}

	@Override
	protected void performDefaults() {
		libs.reset();
		viewer.refresh();
		if (editor != null && editor.getEditor() != null) {
			editor.getEditor().dispose();
		}
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
		new Job(Messages.ui_Adopting_arduino_libraries) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				MultiStatus status = new MultiStatus(Activator.getId(), 0, Messages.ui_installing_arduino_libraries,
						null);
				for (LibraryTree.Library lib : libs.getAllLibraries()) {
					Library toRemove = Manager.getLibraryIndex().getInstalledLibrary(lib.getName());
					if (toRemove != null && !toRemove.getVersion().equals(lib.getVersion())) {
						status.add(toRemove.remove(monitor));
					}
					Library toInstall = Manager.getLibraryIndex().getLibrary(lib.getName(), lib.getVersion());
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

				int numberOfColumns = ((TreeViewer) viewer).getTree().getColumnCount();
				LibraryLabelProvider labelProvider = (LibraryLabelProvider) ((TreeViewer) viewer).getLabelProvider();
				boolean isMatch = false;
				for (int columnIndex = 0; columnIndex < numberOfColumns; columnIndex++) {
					String labelText = labelProvider.getColumnText(element, columnIndex);
					isMatch |= wordMatches(labelText);
				}
				return isMatch;
			}
		};

		tree = new FilteredTree(parent, SWT.CHECK | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION, filter, true) {
			protected TreeViewer doCreateTreeViewer(Composite composite, int style) {
				CheckboxTreeViewer viewer = new CheckboxTreeViewer(composite);
				viewer.setCheckStateProvider(new LibraryCheckProvider());
				viewer.setLabelProvider(new LibraryLabelProvider());
				viewer.setContentProvider(new LibraryContentProvider());
				return viewer;
			}

		};
		viewer = tree.getViewer();
		viewer.setInput(libs);

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
		
		// enable tooltips
		ColumnViewerToolTipSupport.enableFor(viewer);
		
		// tree interactions listener
		viewer.getTree().addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				if (editor.getEditor() != null) {
					editor.getEditor().dispose();
				}
				final TreeItem item = event.item instanceof TreeItem ? (TreeItem) event.item : null;
				if (item != null && event.detail == SWT.CHECK) {
					if (item.getData() instanceof LibraryTree.Category) {
						item.setGrayed(false);
						for (LibraryTree.Library child : ((LibraryTree.Category) item.getData()).getLibraries()) {
							if (item.getChecked()) {
								child.setVersion(child.getLatest());
							} else {
								child.setVersion(null);
							}
						}
						for (TreeItem child : item.getItems()) {
							child.setChecked(item.getChecked());
							if (item.getChecked()) {
								child.setText(1, ((LibraryTree.Library) child.getData()).getLatest());
							} else {
								child.setText(1, "");
							}
						}
					} else {
						if (item.getChecked()) {
							((LibraryTree.Library) item.getData())
									.setVersion(((LibraryTree.Library) item.getData()).getLatest());
							item.setText(1, ((LibraryTree.Library) item.getData()).getLatest());
						} else {
							((LibraryTree.Library) item.getData()).setVersion(null);
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
							((LibraryTree.Library) item.getData()).setVersion(combo.getText());
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
	 * Ensures the correct checked/unchecked/greyed attributes are set on the
	 * category.
	 * 
	 * @param item
	 *            the tree item representing the category
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
	private static class LibraryLabelProvider extends CellLabelProvider {

		@Override
		public String getToolTipText(Object element) {
			if (element instanceof LibraryTree.Library) {
				return ((LibraryTree.Library)element).getTooltip();
			}
			return null;
		}
		
		public String getColumnText(Object element, int col) {
			switch (col) {
			case 0:
				return ((LibraryTree.Node) element).getName();
			case 1:
				if (element instanceof LibraryTree.Library) {
					return ((LibraryTree.Library) element).getVersion();
				} else {
					return "";
				}
			}
			return null;
		}

		@Override
		public Point getToolTipShift(Object object) {
			return new Point(5,5);
		}

		@Override
		public int getToolTipDisplayDelayTime(Object object) {
			return 500;
		}

		@Override
		public int getToolTipTimeDisplayed(Object object) {
			return 0;
		}

		@Override
		public void update(ViewerCell cell) {
			if (cell.getColumnIndex() == 0) {
				cell.setText(((LibraryTree.Node)cell.getElement()).getName());
			} else if (cell.getElement() instanceof LibraryTree.Library) {
				cell.setText(((LibraryTree.Library)cell.getElement()).getVersion());
			} else {
				cell.setText(null);
			}
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
				return ((LibraryTree.Library) element).getVersion() != null;
			} else if (element instanceof LibraryTree.Category) {
				for (LibraryTree.Library library : ((LibraryTree.Category) element).getLibraries()) {
					if (library.getVersion() != null) {
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
					if (library.getVersion() == null) {
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

	private static class LibraryTree {
		private TreeMap<String, Category> categories = new TreeMap<>();

		public static interface Node {
			boolean hasChildren();

			Object[] getChildren();

			Object getParent();

			String getName();
		}

		public class Category implements Comparable<Category>, Node {
			private String name;
			private TreeMap<String, Library> libraries = new TreeMap<>();

			public Category(String name) {
				this.name = name;
			}

			public String getName() {
				return name;
			}

			public Collection<Library> getLibraries() {
				return libraries.values();
			}

			@Override
			public int compareTo(Category other) {
				return name.compareTo(other.name);
			}

			@Override
			public boolean hasChildren() {
				return !libraries.isEmpty();
			}

			@Override
			public Object[] getChildren() {
				return libraries.values().toArray();
			}

			@Override
			public Object getParent() {
				return LibraryTree.this;
			}
		}

		public class Library implements Comparable<Library>, Node {
			private String name;
			private Category category;
			private TreeSet<Version> versions = new TreeSet<>();
			private String version;
			private String tooltip;

			public Library(Category category, String name, String tooltip) {
				this.category = category;
				this.name = name;
				this.tooltip = tooltip;
			}

			public Collection<Version> getVersions() {
				return versions;
			}

			public String getName() {
				return name;
			}

			public String getTooltip() {
				return tooltip;
			}

			public String getLatest() {
				return versions.last().toString();
			}

			public String getVersion() {
				return version;
			}

			public void setVersion(String version) {
				this.version = version;
			}

			@Override
			public int compareTo(Library other) {
				return name.compareTo(other.name);
			}

			@Override
			public boolean hasChildren() {
				return false;
			}

			@Override
			public Object[] getChildren() {
				return null;
			}

			@Override
			public Object getParent() {
				return category;
			}
		}

		public class Version implements Comparable<Object> {
			private String[] parts;

			public Version(String version) {
				parts = version.split("\\.");
			}

			@Override
			public int compareTo(Object other) {
				if (other instanceof String) {
					return this.compareTo(new Version((String) other));
				} else if (other instanceof Version) {
					return this.compareParts(((Version) other).parts, 0);
				} else {
					throw new UnsupportedOperationException();
				}
			}

			private int compareParts(String[] other, int level) {
				if (parts.length > level && other.length > level) {
					if (parts[level].compareTo(other[level]) == 0) {
						return this.compareParts(other, ++level);
					} else {
						try {
							return new Integer(parts[level]).compareTo(Integer.parseInt(other[level]));
						} catch (Exception e) {
							return parts[level].compareTo(other[level]);
						}
					}
				} else {
					return parts.length > other.length ? 1 : -1;
				}
			}

			public String toString() {
				return String.join(".", parts);
			}
		}

		public LibraryTree() {
			LibraryIndex libraryIndex = Manager.getLibraryIndex();

			for (String categoryName : libraryIndex.getCategories()) {
				Category category = new Category(categoryName);
				for (it.baeyens.arduino.managers.Library library : libraryIndex.getLibraries(categoryName)) {
					Library lib = category.libraries.get(library.getName());
					if (lib == null) {
						StringBuilder builder = new StringBuilder("Architectures: ")
								.append(library.getArchitectures().toString())
								.append("\n\n")
								.append(library.getSentence());
						lib = new Library(category, library.getName(), builder.toString());
						category.libraries.put(lib.getName(), lib);
					}
					lib.versions.add(new Version(library.getVersion()));
					if (library.isInstalled()) {
						lib.version = library.getVersion();
					}
				}

				categories.put(category.getName(), category);
			}
		}

		public Collection<Category> getCategories() {
			return categories.values();
		}

		public Collection<Library> getAllLibraries() {
			Set<Library> all = new TreeSet<>();
			for (Category category : categories.values()) {
				all.addAll(category.getLibraries());
			}
			return all;
		}

		public void reset() {
			LibraryIndex libraryIndex = Manager.getLibraryIndex();
			for (Library library : this.getAllLibraries()) {
				it.baeyens.arduino.managers.Library installed = libraryIndex.getInstalledLibrary(library.getName());
				library.setVersion(installed != null ? installed.getVersion() : null);
			}
		}
	}
}
