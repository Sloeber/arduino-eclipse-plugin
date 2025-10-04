package io.sloeber.ui.helpers;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;

import io.sloeber.arduinoFramework.api.IArduinoLibrary;
import io.sloeber.arduinoFramework.api.IArduinoLibraryIndex;
import io.sloeber.arduinoFramework.api.IArduinoLibraryVersion;
import io.sloeber.arduinoFramework.api.LibraryManager;
import io.sloeber.core.api.VersionNumber;

public class ArduinoLibraryTree implements ITreeContentProvider {

	final private static String canUpdateLabel = " (can update)"; //$NON-NLS-1$
	final private static String blankLine = "\n\n";//$NON-NLS-1$
	final private static String emptyString = ""; //$NON-NLS-1$

	private TreeMap<String, Branch> myBranches = new TreeMap<>();

	public class Branch implements Comparable<Branch>, ITreeContentProvider {
		private Object myParent;
		private String myName;
		private TreeMap<String, Library> myLibraries = new TreeMap<>();
		private TreeMap<String, Branch> mySubBranches = new TreeMap<>();

		public Branch(Object parent, String newName) {
			myName = newName;
			myParent = parent;
		}

		public String getName() {
			return myName;
		}

		public Collection<Library> getLibraries() {
			return myLibraries.values();
		}

		@Override
		public int compareTo(Branch other) {
			return myName.compareTo(other.myName);
		}

		public boolean isChecked() {
			for (Library curLibrary : getLibraries()) {
				if (curLibrary.isChecked()) {
					return true;
				}
			}
			for (Branch curSubBranch : mySubBranches.values()) {
				if (curSubBranch.isChecked()) {
					return true;
				}
			}
			return false;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return new Object[0];
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (!myLibraries.isEmpty()) {
				return myLibraries.values().toArray();
			}
			return mySubBranches.values().toArray();
		}

		@Override
		public Object getParent(Object element) {
			return myParent;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (!myLibraries.isEmpty()) {
				return true;
			}
			return !mySubBranches.isEmpty();
		}
	}

	public class Library implements Comparable<Library>, ITreeContentProvider {
		private IArduinoLibraryVersion myLibVersion = null;
		private IArduinoLibrary myLib = null;
		private boolean myIsSelected = false;
		private String myTooltip = null;
		private IArduinoLibraryVersion myToInstallVersion = null;
		private Branch myCategory;

		public boolean isChecked() {
			return myIsSelected;
		}

		public Library(Branch category, IArduinoLibrary lib) {
			myCategory = category;
			myLib = lib;
			myToInstallVersion = myLib.getInstalledVersion();
			myIsSelected = myToInstallVersion != null;
		}

		public Library(Branch category, IArduinoLibraryVersion lib, boolean addToProject) {
			myCategory = category;
			myLibVersion = lib;
			myIsSelected = addToProject;
			if (myIsSelected) {
				myToInstallVersion = myLibVersion;
			}
		}

		public IArduinoLibraryVersion getInstalledVersion() {
			return myLib.getInstalledVersion();
		}

		public Collection<IArduinoLibraryVersion> getVersions() {
			Set<IArduinoLibraryVersion> ret = new TreeSet<>();
			if (myLibVersion != null) {
				ret.add(myLibVersion);
				return ret;
			}
			if (myLib == null) {
				if (myToInstallVersion != null) {
					ret.add(myToInstallVersion);
				}
				return ret;
			}
			ret.addAll(myLib.getVersions());
			return ret;
		}

		public String getName() {
			if (myLib != null) {
				if (canUpdate()) {
					return myLib.getID() + canUpdateLabel;
				}
				return myLib.getID();
			}
			if (myLibVersion != null) {
				if (canUpdate()) {
					return myLibVersion.getName() + canUpdateLabel;
				}
				return myLibVersion.getName();
			}
			if (myToInstallVersion != null) {
				if (canUpdate()) {
					return myToInstallVersion.getName() + canUpdateLabel;
				}
				return myToInstallVersion.getName();
			}

			return "ERROR: There is no lib."; //$NON-NLS-1$
		}

		private boolean canUpdate() {
			if (myLibVersion != null) {
				if (!myLibVersion.isInstalled()) {
					return false;
				}
				IArduinoLibrary lib = myLibVersion.getLibrary();
				if (lib == null) {
					return false;
				}
			}
			if (myLib != null) {
				return myLib.canUpdate();
			}
			return false;
		}

		public String getTooltip() {
			if (myTooltip == null) {
				IArduinoLibraryVersion libVersion = null;
				if (myLibVersion != null) {
					libVersion = myLibVersion;
				} else {
					if (myLib != null) {
						libVersion = myLib.getNewestVersion();
					}
				}
				if (libVersion == null) {
					return "No tooltip found."; //$NON-NLS-1$
				}
				// IArduinoLibraryVersion libVers = getLatest();
				List<String> architectures = libVersion.getArchitectures();
				String architecturePart = emptyString;
				if (architectures != null) {
					architecturePart = "Architectures:" + libVersion.getArchitectures().toString() + blankLine; //$NON-NLS-1$
				}
				myTooltip = architecturePart + libVersion.getSentence() + blankLine + libVersion.getParagraph()
						+ blankLine + "Author: " //$NON-NLS-1$
						+ libVersion.getAuthor() + blankLine + "Maintainer: " + libVersion.getMaintainer(); //$NON-NLS-1$
			}
			return myTooltip;
		}

		public IArduinoLibraryVersion getLatest() {
			if (myLib != null) {
				return myLib.getNewestVersion();
			}
			if (myLibVersion != null) {
				return myLibVersion;
			}
			return null;
		}

		public IArduinoLibraryVersion getVersion() {
			if (myLibVersion != null) {
				return myLibVersion;
			}
			return myToInstallVersion;
		}

		public void setVersion(IArduinoLibraryVersion version) {
			myToInstallVersion = version;
			myIsSelected = myToInstallVersion != null;
		}

		public void setVersion(VersionNumber versionNumber) {
			if (versionNumber == null) {
				myToInstallVersion = null;
				myIsSelected = false;
			}
			myToInstallVersion = myLib.getVersion(versionNumber);
			myIsSelected = myToInstallVersion != null;
		}

		@Override
		public int compareTo(Library other) {
			return myLib.compareTo(other.getArduinoLibrary());
		}

		private IArduinoLibrary getArduinoLibrary() {
			return myLib;
		}

		public String getVersionString() {
			if (myToInstallVersion == null) {
				return emptyString;
			}
			VersionNumber version = myToInstallVersion.getVersion();
			if (version == null) {
				return null;
			}
			return version.toString();
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return new Object[0];
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			return getCategory();
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}

		public Branch getCategory() {
			return myCategory;
		}

	}

	/**
	 * Ensures the correct checked/unchecked/greyed attributes are set on the
	 * category.
	 *
	 * @param item the tree item representing the category
	 */
	private static void verifySubtreeCheckStatus(TreeItem item) {
		if (item == null) {
			return;
		}
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
		verifySubtreeCheckStatus(item.getParentItem());
	}

	/**
	 * Provides the correct checked status for installed libraries
	 *
	 */
	private static class LibraryCheckProvider implements ICheckStateProvider {
		@Override
		public boolean isChecked(Object element) {
			if (element instanceof ArduinoLibraryTree.Library) {
				return ((ArduinoLibraryTree.Library) element).isChecked();
			} else if (element instanceof ArduinoLibraryTree.Branch) {
				return ((ArduinoLibraryTree.Branch) element).isChecked();

			}
			return false;
		}

		@Override
		public boolean isGrayed(Object element) {
			if (element instanceof ArduinoLibraryTree.Branch && isChecked(element)) {
				return true;
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
			return ((ITreeContentProvider) node).getChildren(node);
		}

		@Override
		public Object getParent(Object node) {
			return ((ITreeContentProvider) node).getParent(node);
		}

		@Override
		public boolean hasChildren(Object node) {
			return ((ITreeContentProvider) node).hasChildren(node);
		}

		@Override
		public Object[] getElements(Object node) {
			return ((ITreeContentProvider) node).getElements(node);
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

	/**
	 *
	 * Displays the tree labels for both columns: name and version
	 *
	 */
	public class LibraryLabelProvider extends CellLabelProvider {

		@Override
		public String getToolTipText(Object element) {
			if (element instanceof ArduinoLibraryTree.Library) {
				return ((ArduinoLibraryTree.Library) element).getTooltip();
			}
			return null;
		}

		public static String getColumnText(Object element, int col) {
			if (element instanceof Library) {
				Library theLibrary = (Library) element;
				switch (col) {
				case 0:
					return theLibrary.getName();
				case 1:
					return theLibrary.getVersionString();
				default:
					break;
				}
			}
			if (element instanceof Branch) {
				Branch theCategory = (Branch) element;
				switch (col) {
				case 0:
					return theCategory.getName();
				default:
					break;
				}
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
			Object element = cell.getElement();
			cell.setText(LibraryLabelProvider.getColumnText(element, cell.getColumnIndex()));
		}
	}

	public ArduinoLibraryTree() {

	}

	public Set<Library> getLibs() {
		Set<Library> ret = new TreeSet<>();
		for (Branch category : myBranches.values()) {
			ret.addAll(category.getLibraries());
		}
		return ret;
	}

	public FilteredTree createTree(Composite composite, Map<String, IArduinoLibraryVersion> allLibraries,
			Collection<IArduinoLibraryVersion> addedLibs) {
		for (IArduinoLibraryVersion curLibVersion : allLibraries.values()) {
			String breadCrumbs[] = curLibVersion.getBreadCrumbs();
			if (breadCrumbs.length > 1) {
				Branch curBranch1 = myBranches.get(breadCrumbs[1]);
				IArduinoLibrary curLib = curLibVersion.getLibrary();
				if (curBranch1 == null) {
					curBranch1 = new Branch(this, breadCrumbs[1]);
					myBranches.put(curBranch1.getName(), curBranch1);
				}
				if (curLib == null) {
					curBranch1.myLibraries.put(curLibVersion.getName(),
							new Library(curBranch1, curLibVersion, addedLibs.contains(curLibVersion)));
				} else {
					String curCategory = curLib.getCategory();
					if (curCategory == null || curCategory.isBlank()) {
						curBranch1.myLibraries.put(curLibVersion.getName(),
								new Library(curBranch1, curLibVersion, addedLibs.contains(curLibVersion)));

					} else {
						Branch curBranch2 = curBranch1.mySubBranches.get(curCategory);
						if (curBranch2 == null) {
							curBranch2 = new Branch(curBranch1, curCategory);
							curBranch1.mySubBranches.put(curBranch2.getName(), curBranch2);
						}
						curBranch2.myLibraries.put(curLibVersion.getName(),
								new Library(curBranch1, curLibVersion, addedLibs.contains(curLibVersion)));
					}

				}

			}
		}
		return internalCreateTree(composite);
	}

	public FilteredTree createTree(Composite composite) {
		for (IArduinoLibraryIndex libraryIndex : LibraryManager.getLibraryIndices()) {
			for (IArduinoLibrary arduinoLibrary : libraryIndex.getLibraries()) {
				String categoryName = arduinoLibrary.getCategory();
				Branch category = this.myBranches.get(categoryName);
				if (category == null) {
					category = new Branch(this, categoryName);
					this.myBranches.put(category.getName(), category);
				}
				category.myLibraries.put(arduinoLibrary.getNodeName(), new Library(category, arduinoLibrary));
			}
		}
		return internalCreateTree(composite);
	}

	private FilteredTree internalCreateTree(Composite parent) {
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

		FilteredTree tree = new FilteredTree(parent, SWT.CHECK | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION, filter,
				true, true) {
			@Override
			protected TreeViewer doCreateTreeViewer(Composite composite, int style) {
				CheckboxTreeViewer viewer1 = new CheckboxTreeViewer(composite);
				viewer1.setCheckStateProvider(new LibraryCheckProvider());
				viewer1.setLabelProvider(new LibraryLabelProvider());
				viewer1.setContentProvider(new LibraryContentProvider());
				return viewer1;
			}

		};
		TreeViewer viewer = tree.getViewer();
		viewer.setInput(this);

		TreeColumn name = new TreeColumn(viewer.getTree(), SWT.LEFT);
		name.setWidth(400);

		TreeColumn version = new TreeColumn(viewer.getTree(), SWT.LEFT);
		version.setWidth(100);

		// create the editor and set its attributes
		TreeEditor editor = new TreeEditor(viewer.getTree());
		editor.horizontalAlignment = SWT.LEFT;
		editor.grabHorizontal = true;
		editor.setColumn(1);

		// this ensures the tree labels are displayed correctly
		viewer.refresh(true);

		// enable tooltips
		ColumnViewerToolTipSupport.enableFor(viewer);

		// tree interactions listener
		viewer.getTree().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				final TreeItem item = event.item instanceof TreeItem ? (TreeItem) event.item : null;
				if (item == null) {
					return;
				}
				if (event.detail == SWT.CHECK) {
					if (item.getData() instanceof ArduinoLibraryTree.Branch) {
						item.setChecked(!item.getChecked());
					} else {
						if (item.getData() instanceof ArduinoLibraryTree.Library) {
							Library lib = ((ArduinoLibraryTree.Library) item.getData());
							if (item.getChecked()) {
								lib.setVersion(lib.getLatest());
							} else {
								lib.setVersion((IArduinoLibraryVersion) null);

							}
							verifySubtreeCheckStatus(item.getParentItem());
						}
					}
				}
				if (item.getItemCount() == 0 && item.getChecked()) {
					if (item.getData() instanceof ArduinoLibraryTree.Library) {
						Library selectedLib = ((ArduinoLibraryTree.Library) item.getData());
						if (selectedLib.getVersions().size() == 1) {
							// only one version so no need for make a combo box.
							// only update the version field
							VersionNumber versionNumber = selectedLib.getVersion().getVersion();
							if (versionNumber == null) {
								item.setText(1, "n/a");
							} else {
								item.setText(1, versionNumber.toString());
							}
						} else {
							// Create the dropdown and add data to it
							final CCombo combo = new CCombo(viewer.getTree(), SWT.READ_ONLY);
							for (IArduinoLibraryVersion curVersion : selectedLib.getVersions()) {
								VersionNumber versionNumber = curVersion.getVersion();
								if (versionNumber != null) {
									combo.add(versionNumber.toString());
								}
							}

							IArduinoLibraryVersion displayVersion = selectedLib.getVersion();
							if (displayVersion == null) {
								displayVersion = selectedLib.getLatest();
								selectedLib.setVersion(displayVersion);
								// item.setText(0, selectedLib.getName());
							}
							if (displayVersion.getVersion() != null) {
								combo.select(combo.indexOf(displayVersion.getVersion().toString()));
							}

							// Compute the width for the editor
							// Also, compute the column width, so that the dropdown fits

							// Set the focus on the dropdown and set into the editor
							combo.setFocus();
							editor.setEditor(combo, item, 1);

							// Add a listener to set the selected item back into the
							// cell
							combo.addSelectionListener(new SelectionAdapter() {
								@Override
								public void widgetSelected(SelectionEvent event1) {
									ArduinoLibraryTree.Library lib = (ArduinoLibraryTree.Library) item.getData();
									lib.setVersion(new VersionNumber(combo.getText()));
									// Item selected: end the editing session
									item.setText(1, combo.getText());
									combo.dispose();
								}
							});
						}
					}
				}
			}
		});
		return tree;
	}

	public Set<IArduinoLibraryVersion> getSelectedLibraries() {
		Set<IArduinoLibraryVersion> ret = new TreeSet<>();
		for (Branch curBranch : myBranches.values()) {
			ret.addAll(getSelectedLibraries(curBranch));
		}
		return ret;
	}

	private static Set<IArduinoLibraryVersion> getSelectedLibraries(Branch branch) {
		Set<IArduinoLibraryVersion> ret = new TreeSet<>();
		for (Library curLib : branch.getLibraries()) {
			if (curLib.isChecked()) {
				ret.add(curLib.getVersion());
			}
		}
		for (Branch curBranch : branch.mySubBranches.values()) {
			ret.addAll(getSelectedLibraries(curBranch));
		}
		return ret;
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return myBranches.values().toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		return new Object[0];// myBranches.values().toArray();
	}

	@Override
	public Object getParent(Object element) {
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		return !myBranches.isEmpty();
	}

}
