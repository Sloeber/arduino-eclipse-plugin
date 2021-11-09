package io.sloeber.ui.preferences;

import static io.sloeber.ui.Activator.*;

import java.util.Collection;
import java.util.Collections;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ICheckStateProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
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

import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.api.VersionNumber;
import io.sloeber.core.api.Json.library.LibraryIndexJson;
import io.sloeber.core.api.Json.library.LibraryJson;
import io.sloeber.ui.Messages;

public class LibrarySelectionPage extends PreferencePage implements IWorkbenchPreferencePage {

	private FilteredTree tree;
	private boolean isJobRunning = false;
	protected TreeViewer viewer;
	protected TreeEditor editor;
	protected LibraryTree libs = new LibraryTree();
	final static String emptyString = ""; //$NON-NLS-1$
	final static String blankLine = "\n\n";//$NON-NLS-1$
	final static String canUpdateLabel = " (update)";

	public interface Node {
		boolean hasChildren();

		Object[] getChildren();

		Object getParent();

		String getName();
	}

	public static class LibraryTree implements Node {

		private TreeMap<String, Category> categories = new TreeMap<>();

		public class Category implements Comparable<Category>, Node {
			private String name;
			private TreeMap<String, Library> libraries = new TreeMap<>();

			public Category(String name) {
				this.name = name;
			}

			@Override
			public String getName() {
				return this.name;
			}

			public Collection<Library> getLibraries() {
				return this.libraries.values();
			}

			@Override
			public int compareTo(Category other) {
				return this.name.compareTo(other.name);
			}

			@Override
			public boolean hasChildren() {
				return !this.libraries.isEmpty();
			}

			@Override
			public Object[] getChildren() {
				return this.libraries.values().toArray();
			}

			@Override
			public Object getParent() {
				return LibraryTree.this;
			}
		}

		public class Library implements Comparable<Library>, Node {
			private String myName;
			private Category myParent;
			protected TreeSet<LibraryJson> myVersions = new TreeSet<>(Collections.reverseOrder());
			protected LibraryJson myInstalledVersion;

			public Library(Category category, String name) {
				myParent = category;
				myName = name;
			}

			public Collection<LibraryJson> getVersions() {
				return myVersions;
			}

			@Override
			public String getName() {
				if (canUpdate()) {
					return myName + canUpdateLabel;
				}
				return myName;
			}

			public boolean canUpdate() {
				return myInstalledVersion == null ? false : getLatest().compareTo(myInstalledVersion) != 0;
			}

			private String myTooltip = null;

			public String getTooltip() {
				if (myTooltip == null) {
					LibraryJson library = getLatest();
					myTooltip = "Architectures:" + library.getArchitectures().toString() + blankLine
							+ library.getSentence() + blankLine + "provided by: " + library.getParent().getName();
				}
				return myTooltip;
			}

			public LibraryJson getLatest() {
				return myVersions.first();
			}

			public LibraryJson getVersion() {
				return myInstalledVersion;
			}

			public void setVersion(LibraryJson version) {
				myInstalledVersion = version;
			}

			public void setVersion(VersionNumber versionNumber) {
				if (versionNumber == null) {
					myInstalledVersion = null;
				}
				for (LibraryJson curLib : myVersions) {
					if (curLib.getVersion().compareTo(versionNumber) == 0) {
						myInstalledVersion = curLib;
						return;
					}
				}
				// this should not happen
				myInstalledVersion = null;
			}

			@Override
			public int compareTo(Library other) {
				return myName.compareTo(other.myName);
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
				return myParent;
			}

			public String getVersionString() {
				if (myInstalledVersion == null) {
					return emptyString;
				}
				return myInstalledVersion.getVersion().toString();
			}

		}

		public LibraryTree() {
			for (LibraryIndexJson libraryIndex : LibraryManager.getLibraryIndices()) {
				for (String categoryName : libraryIndex.getCategories()) {
					Category category = this.categories.get(categoryName);
					if (category == null) {
						category = new Category(categoryName);
						this.categories.put(category.getName(), category);
					}
					for (LibraryJson library : libraryIndex.getLibraries(categoryName)) {
						Library lib = category.libraries.get(library.getName());
						if (lib == null) {
							lib = new Library(category, library.getName());
							category.libraries.put(library.getName(), lib);
						}
						lib.myVersions.add(library);
						if (library.isInstalled()) {
							lib.myInstalledVersion = library;
						}
					}
				}
			}
		}

		private Collection<Category> getCategories() {
			return this.categories.values();
		}

		@Override
		public boolean hasChildren() {
			return getCategories().isEmpty();
		}

		@Override
		public Object[] getChildren() {
			return getCategories().toArray();
		}

		@Override
		public Object getParent() {
			return null;
		}

		@Override
		public String getName() {
			return null;
		}
	}

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
		desc.setLayoutData(layoutData);
		desc.setBackground(parent.getBackground());
		desc.setText(Messages.library_preference_page_add_remove);
		this.createTree(control);

		return control;
	}

	@Override
	public boolean performOk() {
		if (this.isJobRunning == true) {
			MessageDialog.openInformation(getShell(), "Library Manager", //$NON-NLS-1$
					"Library Manager is busy. Please wait some time..."); //$NON-NLS-1$
			return false;
		}
		this.isJobRunning = true;
		Job job = new Job(Messages.ui_Adopting_arduino_libraries) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				MultiStatus status = new MultiStatus(PLUGIN_ID, 0, Messages.ui_installing_arduino_libraries, null);
				// return LibraryManager.setLibraryTree(LibrarySelectionPage.this.libs, monitor,
				// status);
				return null;
			}
		};
		job.addJobChangeListener(new JobChangeAdapter() {

			@Override
			public void done(IJobChangeEvent event) {
				LibrarySelectionPage.this.isJobRunning = false;
			}

		});
		job.setUser(true);
		job.schedule();
		return true;
	}

	public void createTree(Composite parent) {
		// filtering applied to all columns
		PatternFilter filter = new PatternFilter() {
			@Override
			protected boolean isLeafMatch(final Viewer viewer1, final Object element) {

				int numberOfColumns = ((TreeViewer) viewer1).getTree().getColumnCount();
				boolean isMatch = false;
				for (int columnIndex = 0; columnIndex < numberOfColumns; columnIndex++) {
					String labelText = LibraryLabelProvider.getColumnText(element, columnIndex);
					isMatch |= wordMatches(labelText);
				}
				return isMatch;
			}
		};

		this.tree = new FilteredTree(parent, SWT.CHECK | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION, filter, true,
				true) {
			@Override
			protected TreeViewer doCreateTreeViewer(Composite composite, int style) {
				CheckboxTreeViewer viewer1 = new CheckboxTreeViewer(composite);
				viewer1.setCheckStateProvider(new LibraryCheckProvider());
				viewer1.setLabelProvider(new LibraryLabelProvider());
				viewer1.setContentProvider(new LibraryContentProvider());
				return viewer1;
			}

		};
		this.viewer = this.tree.getViewer();
		this.viewer.setInput(this.libs);

		TreeColumn name = new TreeColumn(this.viewer.getTree(), SWT.LEFT);
		name.setWidth(400);

		TreeColumn version = new TreeColumn(this.viewer.getTree(), SWT.LEFT);
		version.setWidth(100);

		// create the editor and set its attributes
		this.editor = new TreeEditor(this.viewer.getTree());
		this.editor.horizontalAlignment = SWT.LEFT;
		this.editor.grabHorizontal = true;
		this.editor.setColumn(1);

		// this ensures the tree labels are displayed correctly
		this.viewer.refresh(true);

		// enable tooltips
		ColumnViewerToolTipSupport.enableFor(this.viewer);

		// tree interactions listener
		this.viewer.getTree().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (LibrarySelectionPage.this.editor.getEditor() != null) {
					LibrarySelectionPage.this.editor.getEditor().dispose();
				}
				final TreeItem item = event.item instanceof TreeItem ? (TreeItem) event.item : null;
				if (item != null && event.detail == SWT.CHECK) {
					if (item.getData() instanceof LibraryTree.Category) {
						item.setGrayed(false);
						for (LibraryTree.Library child : ((LibraryTree.Category) item.getData()).getLibraries()) {
							if (item.getChecked()) {
								child.setVersion(child.getLatest());
							} else {
								child.setVersion((LibraryJson) null);
							}
						}
						for (TreeItem child : item.getItems()) {
							child.setChecked(item.getChecked());
							if (item.getChecked()) {
								child.setText(1, ((LibraryTree.Library) child.getData()).getLatest().toString());
							} else {
								child.setText(1, emptyString);
							}
						}
					} else {
						if (item.getChecked()) {
							((LibraryTree.Library) item.getData())
									.setVersion(((LibraryTree.Library) item.getData()).getLatest());
							item.setText(1, ((LibraryTree.Library) item.getData()).getLatest().toString());
						} else {
							((LibraryTree.Library) item.getData()).setVersion((LibraryJson) null);
							item.setText(1, emptyString);
						}
						verifySubtreeCheckStatus(item.getParentItem());
					}
				}
				if (item != null && item.getItemCount() == 0 && item.getChecked()) {
					// Create the dropdown and add data to it
					final CCombo combo = new CCombo(LibrarySelectionPage.this.viewer.getTree(), SWT.READ_ONLY);
					for (LibraryJson version1 : ((LibraryTree.Library) item.getData()).getVersions()) {
						combo.add(version1.getVersion().toString());
					}

					// Select the previously selected item from the cell
					combo.select(combo.indexOf(item.getText(1)));

					// Compute the width for the editor
					// Also, compute the column width, so that the dropdown fits

					// Set the focus on the dropdown and set into the editor
					combo.setFocus();
					LibrarySelectionPage.this.editor.setEditor(combo, item, 1);

					// Add a listener to set the selected item back into the
					// cell
					combo.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent event1) {
							LibraryTree.Library lib = (LibraryTree.Library) item.getData();
							lib.setVersion(new VersionNumber(combo.getText()));
							item.setText(1, combo.getText());
							item.setText(0, lib.getName());
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
	 * @param item the tree item representing the category
	 */
	protected static void verifySubtreeCheckStatus(TreeItem item) {
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
				return ((LibraryTree.Library) element).getTooltip();
			}
			return null;
		}

		public static String getColumnText(Object element, int col) {
			switch (col) {
			case 0:
				if (element instanceof LibraryTree.Library) {
					return ((LibraryTree.Library) element).getName();
				}
				return emptyString;
			case 1:
				if (element instanceof LibraryTree.Library) {
					return ((LibraryTree.Library) element).getVersionString();
				}
				return emptyString;
			default:
				break;
			}
			return null;
		}

		@Override
		public Point getToolTipShift(Object object) {
			return new Point(5, 5);
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
				cell.setText(((Node) cell.getElement()).getName());
			} else if (cell.getElement() instanceof LibraryTree.Library) {
				cell.setText(((LibraryTree.Library) cell.getElement()).getVersionString());
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
			return ((Node) node).getChildren();
		}

		@Override
		public Object getParent(Object node) {
			return ((Node) node).getParent();
		}

		@Override
		public boolean hasChildren(Object node) {
			return ((Node) node).hasChildren();
		}

		@Override
		public Object[] getElements(Object node) {
			return getChildren(node);
		}

		@Override
		public void dispose() {
			// no code needed here
		}

		@Override
		public void inputChanged(Viewer arg0, Object arg1, Object arg2) {
			// no code needed here
		}
	}

}
