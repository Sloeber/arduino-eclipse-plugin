/*******************************************************************************
 * 
 * Copyright (c) 2008, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: FusesWizardPage.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.wizards;

import it.baeyens.avreclipse.PluginIDs;
import it.baeyens.avreclipse.core.avrdude.AVRDudeException;
import it.baeyens.avreclipse.core.avrdude.AVRDudeSchedulingRule;
import it.baeyens.avreclipse.core.avrdude.ProgrammerConfig;
import it.baeyens.avreclipse.core.properties.AVRProjectProperties;
import it.baeyens.avreclipse.core.properties.ProjectPropertyManager;
import it.baeyens.avreclipse.core.toolinfo.AVRDude;
import it.baeyens.avreclipse.core.toolinfo.fuses.ByteValues;
import it.baeyens.avreclipse.core.toolinfo.fuses.FuseType;
import it.baeyens.avreclipse.core.toolinfo.fuses.Fuses;
import it.baeyens.avreclipse.core.util.AVRMCUidConverter;
import it.baeyens.avreclipse.ui.dialogs.AVRDudeErrorDialogJob;
import it.baeyens.avreclipse.ui.dialogs.SelectProgrammerDialog;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ContainerSelectionDialog;
import org.eclipse.ui.progress.UIJob;


/**
 * The "New Fuses File" / "New Lockbits File" Wizard page.
 * <p>
 * This is the UI part of the "Fuses File" Wizard. It is used for both FUSES and LOCKBITS. This is
 * determined during instantiation by passing a {@link FuseType} to the constructor.
 * </p>
 * <p>
 * On this page the following properties for the new fuses file can be edited:
 * <ol>
 * <li>The parent container for the new file</li>
 * <li>The MCU type</li>
 * <li>The name of the fuses file, either without an extension or with a ".fuses" / ".locks"
 * extension</li>
 * <li>The initial content of the file: Atmel defaults or loaded from an MCU.</li>
 * </ol>
 * </p>
 * <p>
 * The selected properties are accessible through the
 * <ul>
 * <li>{@link #getContainer()}</li>
 * <li>{@link #getFileName()}</li>
 * <li>{@link #getByteValues()}</li>
 * </ul>
 * methods.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.3
 */

public class FusesWizardPage extends WizardPage {

	// Texts for the Load from MCU Button
	private static final String	TEXT_LOADBUTTON			= "Load from MCU";
	private static final String	TEXT_LOADBUTTON_BUSY	= "Loading...";

	// The Page title and Description
	private final static String	TITLE					= "New AVR{0} file";
	private final static String	DESCRIPTION				= "This wizard creates a new AVR {0} file.";

	private Text				fContainerText;

	private Text				fFileText;

	private Combo				fMCUCombo;
	private Button				fLoadButton;

	private Button				fDefaultsButton;
	private Button				fLoadedValuesButton;

	private ByteValues			fLoadedValues;

	private final ISelection	fSelection;

	private final FuseType		fType;

	/**
	 * Constructor for FusesWizardPage.
	 * 
	 * @param selection
	 *            The resource selected when the Wizard was called. Will be used to determine and
	 *            set the parent container for the new fuses file.
	 */
	public FusesWizardPage(ISelection selection, FuseType type) {
		super("wizardPage");

		String title = MessageFormat.format(TITLE, type.toString());
		setTitle(title);

		String description = MessageFormat.format(DESCRIPTION, type.toString());
		setDescription(description);

		fSelection = selection;
		fType = type;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;

		addParentFolderRow(container);
		addMCUComboRow(container);
		addFilenameRow(container);
		addInitialContentRow(container);

		initialize();
		validateDialog();
		setControl(parent);
	}

	private void addParentFolderRow(Composite parent) {

		Label label = new Label(parent, SWT.NULL);
		label.setText("&Folder:");

		fContainerText = new Text(parent, SWT.BORDER | SWT.SINGLE);
		fContainerText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fContainerText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validateDialog();
			}
		});

		Button button = new Button(parent, SWT.PUSH);
		button.setText("Browse...");
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleBrowse();
			}
		});

	}

	private void addFilenameRow(Composite parent) {

		// Second Row: The File name
		Label label = new Label(parent, SWT.NULL);
		label.setText("&File name:");

		fFileText = new Text(parent, SWT.BORDER | SWT.SINGLE);
		fFileText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
		fFileText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				validateDialog();
			}
		});

	}

	private void addMCUComboRow(Composite parent) {
		// Third Row: The MCU selection combo
		Label label = new Label(parent, SWT.NULL);
		label.setText("Target &MCU");

		fMCUCombo = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		fMCUCombo.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fMCUCombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				validateDialog();
				// Check if the selected MCU is still valid for any loaded values
				if (fLoadedValues != null && fLoadedValues.isCompatibleWith(getMCUId())) {
					fLoadedValuesButton.setEnabled(true);
				} else {
					fLoadedValuesButton.setEnabled(false);
					fDefaultsButton.setSelection(true);
				}
			}
		});

		// Load from Device Button
		fLoadButton = new Button(parent, SWT.NONE);
		fLoadButton.setText(TEXT_LOADBUTTON);
		fLoadButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		fLoadButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				loadComboFromDevice();
			}
		});
	}

	private void addInitialContentRow(Composite parent) {
		Group contentgroup = new Group(parent, SWT.NONE);
		contentgroup.setText("Initial content");
		contentgroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		contentgroup.setLayout(new GridLayout());

		fDefaultsButton = new Button(contentgroup, SWT.RADIO);
		fDefaultsButton.setText("Use default values (if available)");
		fDefaultsButton
				.setToolTipText("Use the default values defined in the Atmel part description files. Missing for some MCUs.");
		fDefaultsButton.setSelection(true);

		fLoadedValuesButton = new Button(contentgroup, SWT.RADIO);
		fLoadedValuesButton.setText("Use the values loaded from the MCU");
		fLoadedValuesButton.setEnabled(false);
	}

	/**
	 * Tests if the current workbench selection is a suitable container to use. Also determine the
	 * project of the selection and, if it is an AVR project, set the MCU and the filename according
	 * to the active target MCU.
	 */
	private void initialize() {

		String mcuid = "atmega16"; // The default mcu
		String filename = "new"; // The default filename

		Object item = getSelectedItem(fSelection);

		// Set the container value
		IContainer container = null;
		if (item instanceof IResource) {
			if (item instanceof IContainer)
				container = (IContainer) item;
			else
				container = ((IResource) item).getParent();
			fContainerText.setText(container.getFullPath().toString());
		}

		IProject project = getProject(item);

		// Get the MCU

		if (project != null) {
			filename = project.getName();
			ProjectPropertyManager propsmanager = getProjectPropertiesManager(project);
			if (propsmanager.isPerConfig()) {
				// Get the name of the active configuration
				// Get the active build configuration
				IManagedBuildInfo bi = ManagedBuildManager.getBuildInfo(project);
				IConfiguration activecfg = bi.getDefaultConfiguration();
				filename = filename + "_" + activecfg.getName();
			}

		}

		// Check if the file already exists. If yes, then we use an incremental counter
		// to get a new filename
		String fullname = filename + "." + fType.getExtension();
		if (container != null) {
			IFile file = container.getFile(new Path(fullname));
			int i = 2;
			while (file.exists()) {
				fullname = filename + "_" + i++ + "." + fType.getExtension();
				file = container.getFile(new Path(fullname));
			}
		}
		// Set the filename
		fFileText.setText(fullname);

		// Build the list of MCUs with fuses for the MCU selection combo
		// and select the active mcu
		Set<String> allmcus = Fuses.getDefault().getMCUList();
		List<String> mculist = new ArrayList<String>(allmcus);
		Collections.sort(mculist);
		for (String mcu : mculist) {
			fMCUCombo.add(AVRMCUidConverter.id2name(mcu));
		}
		fMCUCombo.setVisibleItemCount(Math.min(mculist.size(), 20));
		fMCUCombo.select(fMCUCombo.indexOf(AVRMCUidConverter.id2name(mcuid)));

	}

	/**
	 * Extract the first selection element of a structured selection.
	 * 
	 * @param selection
	 * @return First element or <code>null</code> if the element could not be extracted.
	 */
	private Object getSelectedItem(ISelection selection) {
		if (fSelection != null && fSelection.isEmpty() == false
				&& fSelection instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) fSelection;
			if (ssel.size() == 1)
				return ssel.getFirstElement();
		}
		return null;
	}

	/**
	 * Extract a project from a selection item.
	 * 
	 * @param item
	 *            Either a <code>IProject</code> or an <code>IAdaptable</code>
	 * @return An <code>IProject</code> or <code>null</code> if the selection does not contain a
	 *         project.
	 */
	private IProject getProject(Object item) {
		IProject project = null;

		// See if the given is an IProject (directly or via IAdaptable
		if (item instanceof IProject) {
			project = (IProject) item;
		} else if (item instanceof IAdaptable) {
			IAdaptable adaptable = (IAdaptable) item;
			project = (IProject) adaptable.getAdapter(IProject.class);
		}
		return project;
	}

	/**
	 * Get the {@link ProjectPropertyManager} for the given project.
	 * <p>
	 * This is a convenience method that adds some error checking to the
	 * <code>ProjectPropertyManager.getPropertyManager(project)</code> call at its core.
	 * </p>
	 * 
	 * @param project
	 *            An AVR project
	 * @return The <code>ProjectPropertyManager</code> for the given project, or <code>null</code>
	 *         if no ProjectPropertyManager exists.
	 */
	private ProjectPropertyManager getProjectPropertiesManager(IProject project) {
		try {
			IProjectNature nature = project.getNature(PluginIDs.NATURE_ID);
			if (nature != null) {
				// This is an AVR Project
				// Get the AVR properties for the active build
				// configuration
				return ProjectPropertyManager.getPropertyManager(project);
			}
		} catch (CoreException e) {
			// Ignore the exception and continue with the default MCU;
		}
		return null;
	}

	/**
	 * Uses the standard container selection dialog to choose the new value for the parent folder
	 * field.
	 */
	private void handleBrowse() {
		ContainerSelectionDialog dialog = new ContainerSelectionDialog(getShell(), ResourcesPlugin
				.getWorkspace().getRoot(), false, "Select parent folder");
		if (dialog.open() == ContainerSelectionDialog.OK) {
			Object[] result = dialog.getResult();
			if (result.length == 1) {
				fContainerText.setText(((Path) result[0]).toString());
			}
		}
	}

	/**
	 * Validate all changes in the dialog.
	 * 
	 */
	private void validateDialog() {
		IContainer container = (IContainer) ResourcesPlugin.getWorkspace().getRoot().findMember(
				new Path(getContainerName()));
		String fileName = getFileName();

		if (getContainerName().length() == 0) {
			updateStatus("Folder must be specified");
			return;
		}
		if (container == null
				|| (container.getType() & (IResource.PROJECT | IResource.FOLDER)) == 0) {
			updateStatus("Folder must exist");
			return;
		}
		if (!container.isAccessible()) {
			updateStatus("Project must be writable");
			return;
		}
		if (fileName.length() == 0) {
			updateStatus("File name must be specified");
			return;
		}
		if (fileName.replace('\\', '/').indexOf('/', 1) > 0) {
			updateStatus("File name must be valid");
			return;
		}
		if (!fileName.endsWith("." + fType.getExtension())) {
			String message = MessageFormat.format("File extension must be \"{0}\"", fType
					.getExtension());
			updateStatus(message);
			return;
		}

		IFile file = container.getFile(new Path(fileName));
		if (file.exists()) {
			updateStatus("File already exists");
			return;
		}

		updateStatus(null);

	}

	/**
	 * Update the dialog page status.
	 * 
	 * @param message
	 *            Error message to display or <code>null</code> if the dialog does not have any
	 *            errors.
	 */
	private void updateStatus(String message) {
		setErrorMessage(message);
		setPageComplete(message == null);
	}

	/**
	 * Get the selected parent folder name.
	 * 
	 * @return String with parent folder name
	 */
	public String getContainerName() {
		return fContainerText.getText();
	}

	/**
	 * Get the selected filename.
	 * 
	 * @return String with filename
	 */
	public String getFileName() {
		return fFileText.getText();
	}

	/**
	 * Get the mcu id value of the selected MCU.
	 * 
	 * @return String with mcu id
	 */
	private String getMCUId() {
		String mcuname = fMCUCombo.getItem(fMCUCombo.getSelectionIndex());
		String mcuid = AVRMCUidConverter.name2id(mcuname);

		return mcuid;
	}

	/**
	 * Get a new <code>ByteValues</code> object set up to the user selection.
	 * <p>
	 * The object has the correct MCU and either - depending on user choice - the default byte
	 * values or the values loaded from an attached MCU.
	 * 
	 * @return New <code>ByteValues</code> object.
	 */
	public ByteValues getNewByteValues() {
		String mcuid = getMCUId();
		if (fLoadedValues != null && fLoadedValues.isCompatibleWith(mcuid)) {
			fLoadedValues.setMCUId(mcuid, false);
			return fLoadedValues;
		}

		// Create new ByteValues object with the default values
		ByteValues newvalues = new ByteValues(fType, mcuid);
		newvalues.setDefaultValues();

		return newvalues;
	}

	/**
	 * Load the actual MCU from the currently selected Programmer and set the MCU combo accordingly.
	 * <p>
	 * This method will start a new Job to load the values and return immediately.
	 * </p>
	 */
	private void loadComboFromDevice() {

		ProgrammerConfig tmpcfg = getProgrammerConfig(fSelection);
		if (tmpcfg == null) {
			SelectProgrammerDialog dialog = new SelectProgrammerDialog(this.getShell(), null);
			if (dialog.open() != IDialogConstants.OK_ID) {
				return;
			}
			tmpcfg = dialog.getResult();
			if (tmpcfg == null) {
				return;
			}
		}
		final ProgrammerConfig progcfg = tmpcfg;

		// Disable the Load Button. It is re-enabled by the load job when the job finishes.
		fLoadButton.setEnabled(false);
		fLoadButton.setText(TEXT_LOADBUTTON_BUSY);

		// The Job that does the actual loading.
		Job readJob = new Job("Reading MCU Signature") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {

				try {
					monitor.beginTask("Starting AVRDude", 100);

					// Get the current Values from the attached MCU
					AVRDude avrdude = AVRDude.getDefault();
					switch (fType) {
						case FUSE:
							fLoadedValues = avrdude.getFuseBytes(progcfg, new SubProgressMonitor(
									monitor, 95));
							break;
						case LOCKBITS:
							fLoadedValues = avrdude.getLockbits(progcfg, new SubProgressMonitor(
									monitor, 95));
							break;
					}

					if (fLoadedValues == null) {
						return Status.CANCEL_STATUS;
					}

					// and update the user interface:
					// Select the MCU in the combo and enable the "use loaded values" button.
					if (!fLoadButton.isDisposed()) {
						fLoadButton.getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								String mcuid = fLoadedValues.getMCUId();
								fMCUCombo.select(fMCUCombo
										.indexOf(AVRMCUidConverter.id2name(mcuid)));
								fLoadedValuesButton.setEnabled(true);
							}
						});
					}
					monitor.worked(5);
				} catch (AVRDudeException ade) {
					// Show an Error message and exit
					if (!fLoadButton.isDisposed()) {
						UIJob messagejob = new AVRDudeErrorDialogJob(fLoadButton.getDisplay(), ade,
								progcfg.getId());
						messagejob.setPriority(Job.INTERACTIVE);
						messagejob.schedule();
						try {
							messagejob.join(); // block until the dialog is closed.
						} catch (InterruptedException e) {
							// Don't care if the dialog is interrupted from outside.
						}
					}
				} catch (SWTException swte) {
					// The display has been disposed, so the user is not
					// interested in the results from this job
					return Status.CANCEL_STATUS;
				} finally {
					monitor.done();
					// Enable the Load from MCU Button
					if (!fLoadButton.isDisposed()) {
						fLoadButton.getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								// Re-Enable the Button
								fLoadButton.setEnabled(true);
								fLoadButton.setText(TEXT_LOADBUTTON);
							}
						});
					}
				}

				return Status.OK_STATUS;
			}
		};

		// now set the Job properties and start it
		readJob.setRule(new AVRDudeSchedulingRule(progcfg));
		readJob.setPriority(Job.SHORT);
		readJob.setUser(true);
		readJob.schedule();
	}

	/**
	 * Get the {@link ProgrammerConfig} for a selection.
	 * <p>
	 * This methods will try to get a project from the selection and, if it is an AVR project, get
	 * the active programmer.
	 * </p>
	 * 
	 * @param selection
	 *            A selection containing an <code>IProject</code>.
	 * @return Active <code>ProgrammerConfig</code> or <code>null</code> if no ProgrammerConfig
	 *         could be retrieved from the selection.
	 */
	private ProgrammerConfig getProgrammerConfig(ISelection selection) {
		Object item = getSelectedItem(selection);
		if (item == null)
			return null;

		IProject project = getProject(item);
		if (project == null)
			return null;

		ProjectPropertyManager propmanager = getProjectPropertiesManager(project);
		if (propmanager == null)
			return null;

		AVRProjectProperties props = propmanager.getActiveProperties();
		ProgrammerConfig progcfg = props.getAVRDudeProperties().getProgrammer();

		return progcfg;
	}

}