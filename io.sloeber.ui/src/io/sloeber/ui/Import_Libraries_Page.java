package io.sloeber.ui;

import java.util.Map;
import java.util.Set;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TreeEditor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.WizardResourceImportPage;

import io.sloeber.arduinoFramework.api.IArduinoLibraryVersion;
import io.sloeber.arduinoFramework.api.LibraryManager;
import io.sloeber.core.api.ISloeberConfiguration;
import io.sloeber.ui.helpers.ArduinoLibraryTree;

public class Import_Libraries_Page extends WizardResourceImportPage {

	protected TreeViewer viewer;
	protected TreeEditor editor;
	protected ArduinoLibraryTree libs = new ArduinoLibraryTree();
	final static String emptyString = ""; //$NON-NLS-1$


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




	public boolean PerformFinish() {
		Set<IArduinoLibraryVersion> selectedLibraries = libs.getSelectedLibraries();
		CoreModel coreModel = CoreModel.getDefault();
		ICProjectDescription projDesc = coreModel.getProjectDescription(myProject, true);
		ISloeberConfiguration sloeberCfg = ISloeberConfiguration.getActiveConfig(projDesc);
		sloeberCfg.setLibraries(selectedLibraries);
		try {
			coreModel.setProjectDescription(myProject, projDesc, true, null);
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return true;
	}

	@Override
	protected ITreeContentProvider getFileProvider() {
		return null;
	}

	@Override
	protected ITreeContentProvider getFolderProvider() {
		return null;
	}
























































//	protected Tree myLibrarySelector;



//	@Override
//	public void createControl(Composite parent) {
//
//		initializeDialogUnits(parent);
//
//		Composite composite = new Composite(parent, SWT.NULL);
//		composite.setLayout(new GridLayout());
//		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
//		composite.setSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
//		composite.setFont(parent.getFont());
//
//		createSourceGroup(composite);
//
//		setPageComplete(true);
//		setErrorMessage(null); // should not initially have error message
//
//		setControl(composite);
//	}
//
//	class ItemSorter {
//		private TreeMap<String, ItemSorter> myItems = new TreeMap<>();
//		private IArduinoLibraryVersion myLib = null;
//		private static Map<IPath, IArduinoLibraryVersion> myCurrentInstalledLibs =null;
//
//		ItemSorter() {
//		}
//
//		static void SetSloeberConfiguration(ISloeberConfiguration sloeberCfg) {
//				myCurrentInstalledLibs = sloeberCfg.getUsedLibraries();
//		}
//
//		public void createChildren(TreeItem curItem) {
//			for (Entry<String, ItemSorter> curentry : myItems.entrySet()) {
//				String key = curentry.getKey();
//				ItemSorter curSorter = curentry.getValue();
//				TreeItem newItem = new TreeItem(curItem, SWT.NONE);
//				newItem.setText(key);
//				curSorter.createChildren(newItem);
//			}
//			if (myLib == null) {
//				curItem.setGrayed(true);
//			}else {
//				boolean isSelected = myCurrentInstalledLibs.get(myLib.getFQN()) != null;
//				curItem.setChecked(isSelected);
//				curItem.setData(myLib);
//				if (isSelected) {
//					// expand all parents
//					TreeItem parentTreeItem = curItem;
//					while (parentTreeItem != null) {
//						parentTreeItem.setExpanded(true);
//						parentTreeItem.setChecked(true);
//						parentTreeItem.setGrayed(true);
//						parentTreeItem = parentTreeItem.getParentItem();
//					}
//				}
//			}
//
//		}
//	}

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

	//	GridData theGriddata;
		ISloeberConfiguration sloeberCfg = ISloeberConfiguration.getActiveConfig(myProject);
		// find the items to add to the list
		Map<String, IArduinoLibraryVersion> allLibraries = LibraryManager
				.getLibrariesAll(sloeberCfg.getBoardDescription());
//		Map<IPath, IArduinoLibraryVersion> usedLibs=sloeberCfg.getUsedLibraries();
//		 Map<Boolean, IArduinoLibraryVersion> libSelection =new HashMap<>();
//		for(IArduinoLibraryVersion curVersion:allLibraries.values()) {
//			if(curVersion.isInstalled()) {
//				libSelection.put(usedLibs.containsKey(curVersion),curVersion);
//			}
//		}
		libs.createTree(composite,allLibraries,sloeberCfg.getUsedLibraries().values());

		//myLibrarySelector = new Tree(composite, SWT.CHECK | SWT.BORDER);
//		theGriddata = new GridData(SWT.FILL, SWT.FILL, true, true);
//		theGriddata.horizontalSpan = 1;
		//myLibrarySelector.setLayoutData(theGriddata);
		//myLibrarySelector.setSortDirection(SWT.UP);



//		// sort the items
//		ItemSorter sortedItems = new ItemSorter();
//		ItemSorter.SetSloeberConfiguration(sloeberCfg);
//
//		for (IArduinoLibraryVersion curlib : allLibraries.values()) {
//			String keys[] = curlib.getBreadCrumbs();
//			ItemSorter curParent = sortedItems;
//			for (String curKey : keys) {
//				ItemSorter curSorter = curParent.myItems.get(curKey);
//				if (curSorter == null) {
//					curSorter = new ItemSorter();
//					curParent.myItems.put(curKey, curSorter);
//				}
//				curParent = curSorter;
//			}
//			curParent.myLib = curlib;
//		}
//		myLibrarySelector.setRedraw(false);

//		for (Entry<String, ItemSorter> curentry : sortedItems.myItems.entrySet()) {
//			String key = curentry.getKey();
//			ItemSorter curSorter = curentry.getValue();
//
//			TreeItem curItem = new TreeItem(myLibrarySelector, SWT.NONE);
//			curItem.setText(key);
//			curSorter.createChildren(curItem);
//		}
//		myLibrarySelector.setRedraw(true);

	}

//	@Override
//	public void createControl(Composite parent) {
//
//		initializeDialogUnits(parent);
//
//		Composite composite = new Composite(parent, SWT.NULL);
//		composite.setLayout(new GridLayout());
//		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
//		composite.setSize(composite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
//		composite.setFont(parent.getFont());
//
//		createSourceGroup(composite);
//
//		setPageComplete(true);
//		setErrorMessage(null); // should not initially have error message
//
//		setControl(composite);
//	}
//





//	@Override
//	protected Control createContents(Composite parent) {
//		Composite control = new Composite(parent, SWT.NONE);
//		control.setLayout(new GridLayout());
//
//		Text desc = new Text(control, SWT.READ_ONLY);
//		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false);
//		desc.setLayoutData(layoutData);
//		desc.setBackground(parent.getBackground());
//		desc.setText(Messages.library_preference_page_add_remove);
//		libs.createTree(control);
//
//		return control;
//	}


//	public Set<IArduinoLibraryVersion> getSelectedLibraries() {
//		Set<IArduinoLibraryVersion> ret= new HashSet<>();
//		for(Library curLib : libs.getLibs()) {
////			if(curLib.isSelected()) {
//				ret.add(curLib.getInstalledVersion());
//	//		}
//		}
//
////		Set<IArduinoLibraryVersion> ret = new HashSet<>();
////		for (TreeItem curTreeItem : myLibrarySelector.getItems()) {
////			ret.addAll(internalGetSelectedLibraries(curTreeItem));
////		}
//		return ret;
//	}
//
//	private List<IArduinoLibraryVersion> internalGetSelectedLibraries(TreeItem TreeItem) {
//		List<IArduinoLibraryVersion> ret = new ArrayList<>();
//		for (TreeItem curchildTreeItem : TreeItem.getItems()) {
//			if (curchildTreeItem.getChecked() && (curchildTreeItem.getData() != null)) {
//				ret.add((IArduinoLibraryVersion) curchildTreeItem.getData());
//			}
//			ret.addAll(internalGetSelectedLibraries(curchildTreeItem));
//		}
//		return ret;
//	}

}
