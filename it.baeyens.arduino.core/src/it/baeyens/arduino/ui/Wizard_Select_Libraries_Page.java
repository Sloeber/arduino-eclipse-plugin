package it.baeyens.arduino.ui;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.tools.ArduinoHelpers;

import java.io.File;
import java.net.URI;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.WizardResourceImportPage;

public class Wizard_Select_Libraries_Page extends WizardResourceImportPage {

	protected Tree LibrarySelector;
	TreeItem ArduinoLibItem = null;
	TreeItem PersonalLibItem = null;
	TreeItem ArduinoHardwareLibItem = null;

	private IProject mProject = null;

	protected Wizard_Select_Libraries_Page(IProject project, String name, IStructuredSelection selection) {
		super(name, selection);
		setProject(project);
		if (mProject == null) {
			setTitle("Error no project selected to import to");
			setDescription("As no project is selected it is not possible to import libraries");
		} else {
			setTitle("Import arduino libraries");
			setDescription("Use this page to select the libraries to import to project " + mProject.getName());
		}
	}

	@Override
	public void createControl(Composite parent) {

		// }
		// @Override
		// protected void createSourceGroup(Composite parent) {
		if (mProject == null)
			return;
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout theGridLayout = new GridLayout();
		GridData theGriddata;

		Label line = new Label(composite, SWT.HORIZONTAL | SWT.BOLD);
		line.setText("Arduino library to import to");
		theGriddata = new GridData(SWT.FILL, SWT.CENTER, true, false);
		theGriddata.horizontalSpan = 3;
		line.setLayoutData(theGriddata);

		theGridLayout.numColumns = 1;
		composite.setLayout(theGridLayout);
		composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
		composite.setFont(parent.getFont());

		LibrarySelector = new Tree(composite, SWT.CHECK | SWT.BORDER);
		theGriddata = new GridData(GridData.FILL_BOTH);
		LibrarySelector.setLayoutData(theGriddata);
		// Get the data in the tree
		LibrarySelector.setRedraw(false);

		// IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IPathVariableManager pathMan = mProject.getPathVariableManager();

		URI ArduinoLibraryURI = pathMan.getURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_ARDUINO_LIB);
		URI PrivateLibraryURI = pathMan.getURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_PRIVATE_LIB);
		URI HardwareLibrarURI = pathMan.getURIValue(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_HARDWARE_LIB);

		if (HardwareLibrarURI != null) {
			IPath HardwareLibraryPath = URIUtil.toPath(HardwareLibrarURI);

			if ((HardwareLibraryPath.toFile().exists()) && (HardwareLibraryPath.toFile().list().length > 0)) {
				// Create Arduino Item
				ArduinoHardwareLibItem = new TreeItem(LibrarySelector, SWT.NONE);
				ArduinoHardwareLibItem.setText("Arduino hardware Libs");
				// Add the arduino Libs
				AddLibs(ArduinoHardwareLibItem, HardwareLibraryPath);
			}
		}

		if (ArduinoLibraryURI != null) {
			IPath ArduinoLibraryPath = URIUtil.toPath(ArduinoLibraryURI);
			// Create Arduino Item
			ArduinoLibItem = new TreeItem(LibrarySelector, SWT.NONE);
			ArduinoLibItem.setText("Arduino Libs");
			// Add the arduino Libs
			AddLibs(ArduinoLibItem, ArduinoLibraryPath);
		}

		// Create Personal library Item
		if (PrivateLibraryURI != null) {
			IPath PrivateLibraryPath = URIUtil.toPath(PrivateLibraryURI);
			PersonalLibItem = new TreeItem(LibrarySelector, SWT.NONE);
			PersonalLibItem.setText("personal Libs");
			// Add the personal Libs
			AddLibs(PersonalLibItem, PrivateLibraryPath);
			ArduinoLibItem.setExpanded(true);
			PersonalLibItem.setExpanded(true);
		}

		LibrarySelector.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (event.detail == SWT.CHECK) {
					if ((event.item.equals(PersonalLibItem)) | (event.item.equals(ArduinoLibItem))) {
						event.detail = SWT.NONE;
						event.type = SWT.None;
						event.doit = false;
						try {
							LibrarySelector.setRedraw(false);
							if (PersonalLibItem != null)
								PersonalLibItem.setChecked(false);
							if (ArduinoLibItem != null)
								ArduinoLibItem.setChecked(false);
							if (ArduinoHardwareLibItem != null)
								ArduinoHardwareLibItem.setChecked(false);
						} finally {
							LibrarySelector.setRedraw(true);
						}
					}
				}
			}

		});
		// Turn drawing back on!
		LibrarySelector.setRedraw(true);
		// setControl(composite);
		super.createControl(parent);

	}

	@Override
	protected ITreeContentProvider getFileProvider() {
		return null;
	}

	@Override
	protected ITreeContentProvider getFolderProvider() {
		return null;
	}

	public void setProject(IProject project) {
		if (project != null) {
			mProject = project;
			setContainerFieldValue(project.getName());
		}

	}

	@SuppressWarnings("static-method")
	public boolean canFinish() {
		// if (ArduinoLibItem != null) {
		// TreeItem[] AllItems = ArduinoLibItem.getItems();
		// for (int CurItem = 0; CurItem < AllItems.length; CurItem++) {
		// if (AllItems[CurItem].getChecked())
		// return true;
		// }
		// }
		// if (PersonalLibItem != null) {
		// TreeItem[] AllItems = PersonalLibItem.getItems();// .get();//
		// .getItems();
		// for (int CurItem = 0; CurItem < AllItems.length; CurItem++) {
		// if (AllItems[CurItem].getChecked())
		// return true;
		// }
		// }
		// if (ArduinoHardwareLibItem != null) {
		// TreeItem[] AllItems = ArduinoHardwareLibItem.getItems();// .get();//
		// .getItems();
		// for (int CurItem = 0; CurItem < AllItems.length; CurItem++) {
		// if (AllItems[CurItem].getChecked())
		// return true;
		// }
		// }
		return true;// As no can finish is triggered when a item is selected
	}

	private static void AddLibs(TreeItem LibItem, IPath iPath) {
		File LibRoot = iPath.toFile();
		File LibFolder;
		String[] children = LibRoot.list();
		if (children == null) {
			// Either dir does not exist or is not a directory
		} else {
			java.util.Arrays.sort(children, String.CASE_INSENSITIVE_ORDER);
			for (int i = 0; i < children.length; i++) {
				// Get filename of file or directory
				LibFolder = iPath.append(children[i]).toFile();
				if (LibFolder.isDirectory()) {
					TreeItem child = new TreeItem(LibItem, SWT.NONE);
					child.setText(children[i]);
				}
			}
		}
	}

	public boolean PerformFinish() {
		if (ArduinoLibItem != null) {
			importLibs(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_ARDUINO_LIB, ArduinoLibItem.getItems());
		}
		if (PersonalLibItem != null) {
			importLibs(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_PRIVATE_LIB, PersonalLibItem.getItems());
		}
		if (ArduinoHardwareLibItem != null) {
			importLibs(ArduinoConst.WORKSPACE_PATH_VARIABLE_NAME_HARDWARE_LIB, ArduinoHardwareLibItem.getItems());
		}
		return true;
	}

	private void importLibs(String PathVarName, TreeItem[] AllItems) {
		ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
		ICProjectDescription projectDescription = mngr.getProjectDescription(mProject, true);
		ICConfigurationDescription configurationDescriptions[] = projectDescription.getConfigurations();
		for (int curConfig = 0; curConfig < configurationDescriptions.length; curConfig++) {
			for (int CurItem = 0; CurItem < AllItems.length; CurItem++) {
				if (AllItems[CurItem].getChecked()) {
					try {
						ArduinoHelpers.addCodeFolder(mProject, PathVarName, AllItems[CurItem].getText(), ArduinoConst.WORKSPACE_LIB_FOLDER
								+ AllItems[CurItem].getText(), configurationDescriptions[curConfig]);
					} catch (CoreException e) {
						Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Failed to import library ", e));
					}
				}
			}

			try {
				// projectDescription.(configurationDescription);
				mngr.setProjectDescription(mProject, projectDescription, true, null);
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void createSourceGroup(Composite parent) {
		// TODO Auto-generated method stub

	}

}
