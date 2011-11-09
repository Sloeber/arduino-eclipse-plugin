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
 * $Id: TabTargetHardware.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.propertypages;

import it.baeyens.arduino.ArduinoConst;
import it.baeyens.avreclipse.core.avrdude.AVRDudeException;
import it.baeyens.avreclipse.core.avrdude.AVRDudeSchedulingRule;
import it.baeyens.avreclipse.core.properties.AVRDudeProperties;
import it.baeyens.avreclipse.core.properties.AVRProjectProperties;
import it.baeyens.avreclipse.core.toolinfo.AVRDude;
import it.baeyens.avreclipse.core.toolinfo.GCC;
import it.baeyens.avreclipse.core.util.AVRMCUidConverter;
import it.baeyens.avreclipse.ui.dialogs.AVRDudeErrorDialogJob;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.UIJob;


/**
 * This tab handles setting of all target hardware related properties.
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public class TabTargetHardware extends AbstractAVRPropertyTab {

	private static final String		LABEL_MCUTYPE			= "MCU Type";
	private static final String		LABEL_FCPU				= "MCU Clock Frequency";
	private static final String		TEXT_LOADBUTTON			= "Load from MCU";
	private static final String		TEXT_LOADBUTTON_BUSY	= "Loading...";

	private final static String		TITLE_FUSEBYTEWARNING	= "{0} Conflict";
	private final static String		TEXT_FUSEBYTEWARNING	= "Selected MCU is not compatible with the currently set {0}.\n"
																	+ "Please check the {0} settings on the AVRDude {1}.";
	private final static String[]	TITLEINSERT				= new String[] { "", "Fuse Byte",
			"Lockbits", "Fuse Byte and Lockbits"			};
	private final static String[]	TEXTINSERT				= new String[] { "", "fuse byte",
			"lockbits", "fuse byte and lockbits"			};
	private final static String[]	TABNAMEINSERT			= new String[] { "", "Fuse tab",
			"Lockbits tab", "Fuse and Lockbit tabs"		};

	/** List of common MCU frequencies (taken from mfile) */
	private static final String[]	FCPU_VALUES				= { "1000000", "1843200", "2000000",
			"3686400", "4000000", "7372800", "8000000", "11059200", "14745600", "16000000",
			"18432000", "20000000"							};

	/** The Properties that this page works with */
	private AVRProjectProperties	fTargetProps;

	private Combo					fMCUcombo;
	private Button					fLoadButton;
	private Composite				fMCUWarningComposite;

	private Combo					fFCPUcombo;

	private Set<String>				fMCUids;
	private String[]				fMCUNames;

	private String					fOldMCUid;
	private String					fOldFCPU;

	private static final Image		IMG_WARN				= PlatformUI
																	.getWorkbench()
																	.getSharedImages()
																	.getImage(
																			ISharedImages.IMG_OBJS_WARN_TSK);

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.cdt.ui.newui.AbstractCPropertyTab#createControls(org.eclipse.swt.widgets.Composite
	 * )
	 */
	@Override
	public void createControls(Composite parent) {
		super.createControls(parent);
		usercomp.setLayout(new GridLayout(4, false));

		// Get the list of supported MCU id's from the compiler
		// The list is then converted into an array of MCU names
		//
		// If we ever implement per project paths this needs to be moved to the
		// updataData() method to reload the list of supported mcus every time
		// the paths change. The list is added to the combo in addMCUSection().
		if (fMCUids == null) {
			try {
				fMCUids = GCC.getDefault().getMCUList();
			} catch (IOException e) {
				// Could not start avr-gcc. Pop an Error Dialog and continue with an empty list
				IStatus status = new Status(
						IStatus.ERROR,
						ArduinoConst.CORE_PLUGIN_ID,
						"Could not execute avr-gcc. Please check the AVR paths in the preferences.",
						e);
				ErrorDialog.openError(usercomp.getShell(), "AVR-GCC Execution fault", null, status);
				fMCUids = new HashSet<String>();
			}
			String[] allmcuids = fMCUids.toArray(new String[fMCUids.size()]);
			fMCUNames = new String[fMCUids.size()];
			for (int i = 0; i < allmcuids.length; i++) {
				fMCUNames[i] = AVRMCUidConverter.id2name(allmcuids[i]);
			}
			Arrays.sort(fMCUNames);
		}

		addMCUSection(usercomp);
		addFCPUSection(usercomp);
		addSeparator(usercomp);

	}

	private void addMCUSection(Composite parent) {

		GridData gd = new GridData(SWT.FILL, SWT.FILL, false, false, 1, 1);
		FontMetrics fm = getFontMetrics(parent);
		gd.widthHint = Dialog.convertWidthInCharsToPixels(fm, 20);

		// MCU Selection Combo
		setupLabel(parent, LABEL_MCUTYPE, 1, SWT.NONE);
		// Label label = new Label(parent, SWT.NONE);
		// label.setText(LABEL_MCUTYPE);
		// label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		fMCUcombo = new Combo(parent, SWT.READ_ONLY | SWT.DROP_DOWN);
		fMCUcombo.setLayoutData(gd);
		fMCUcombo.setItems(fMCUNames);
		fMCUcombo.setVisibleItemCount(Math.min(fMCUNames.length, 20));

		fMCUcombo.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String mcuname = fMCUcombo.getItem(fMCUcombo.getSelectionIndex());
				String mcuid = AVRMCUidConverter.name2id(mcuname);
				fTargetProps.setMCUId(mcuid);

				// Check if supported by avrdude and set the errorpane as
				// required
				checkAVRDude(mcuid);

				// Check fuse byte settings and pop a message if the settings
				// are not compatible
				checkFuseBytes(mcuid);

				// Set the rebuild flag for the configuration
				getCfg().setRebuildState(true);
			}
		});

		// Load from Device Button
		fLoadButton = setupButton(parent, TEXT_LOADBUTTON, 1, SWT.NONE);
		fLoadButton.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		fLoadButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				loadComboFromDevice();
			}
		});

		// Dummy Label for Padding
		Label label = new Label(parent, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		// The Warning Composite
		fMCUWarningComposite = new Composite(parent, SWT.NONE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 4;
		fMCUWarningComposite.setLayoutData(gd);
		GridLayout gl = new GridLayout(2, false);
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		fMCUWarningComposite.setLayout(gl);

		Label warnicon = new Label(fMCUWarningComposite, SWT.LEFT);
		warnicon.setLayoutData(new GridData(GridData.BEGINNING));
		warnicon.setImage(IMG_WARN);

		Label warnmessage = new Label(fMCUWarningComposite, SWT.LEFT);
		warnmessage.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		warnmessage.setText("This MCU is not supported by AVRDude");

		fMCUWarningComposite.setVisible(false);
	}

	private void addFCPUSection(Composite parent) {

		GridData gd = new GridData();
		FontMetrics fm = getFontMetrics(parent);
		gd.widthHint = Dialog.convertWidthInCharsToPixels(fm, 14);

		setupLabel(parent, LABEL_FCPU, 1, SWT.NONE);

		fFCPUcombo = new Combo(parent, SWT.DROP_DOWN);
		fFCPUcombo.setLayoutData(gd);
		fFCPUcombo.setTextLimit(8); // max. 99 MHz
		fFCPUcombo.setToolTipText("Target Hardware Clock Frequency in Hz");
		fFCPUcombo.setVisibleItemCount(FCPU_VALUES.length);
		fFCPUcombo.setItems(FCPU_VALUES);

		fFCPUcombo.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (fTargetProps != null) {
					fTargetProps.setFCPU(fFCPUcombo.getText());
				}
			}
		});

		// Ensure that only integer values are entered
		fFCPUcombo.addVerifyListener(new VerifyListener() {
			@Override
			public void verifyText(VerifyEvent event) {
				String text = event.text;
				if (!text.matches("[0-9]*")) {
					event.doit = false;
				}
			}
		});
	}


	@Override
	protected void performApply(AVRProjectProperties dst) {

		if (fTargetProps == null) {
			// Do nothing if the Target properties do not exist.
			return;
		}
		String newMCUid = fTargetProps.getMCUId();
		String newFCPU = fTargetProps.getFCPU();

		dst.setMCUId(newMCUid);
		dst.setFCPU(newFCPU);

		// Check if a rebuild is required
		checkRebuildRequired();

		fOldMCUid = newMCUid;
		fOldFCPU = newFCPU;

		// Now we need to invalidate all discovered Symbols, because they still contain infos about
		// the previous MCU.

	}


	@Override
	protected void performCopy(AVRProjectProperties defaults) {
		fTargetProps.setMCUId(defaults.getMCUId());
		fTargetProps.setFCPU(defaults.getFCPU());
		updateData(fTargetProps);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.cdt.ui.newui.AbstractCPropertyTab#performOK()
	 */
	@Override
	protected void performOK() {
		// We override this to set the rebuild state as required
		checkRebuildRequired();
		super.performOK();
	}


	@Override
	protected void updateData(AVRProjectProperties cfg) {

		fTargetProps = cfg;

		String mcuid = cfg.getMCUId();
		fMCUcombo.select(fMCUcombo.indexOf(AVRMCUidConverter.id2name(mcuid)));
		checkAVRDude(mcuid);

		String fcpu = cfg.getFCPU();
		fFCPUcombo.setText(fcpu);

		// Save the original values, so we can set the rebuild flag when any
		// changes are applied.
		fOldMCUid = mcuid;
		fOldFCPU = fcpu;

	}

	/**
	 * Check if the given MCU is supported by avrdude and set visibility of the MCU Warning Message
	 * accordingly.
	 * 
	 * @param mcuid
	 *            The MCU id value to test
	 */
	private void checkAVRDude(String mcuid) {
		if (AVRDude.getDefault().hasMCU(mcuid)) {
			fMCUWarningComposite.setVisible(false);
		} else {
			fMCUWarningComposite.setVisible(true);
		}
	}

	/**
	 * Check if the FuseBytesProperties and Lockbits in the current properties are compatible with
	 * the selected mcu. If not, a warning dialog is shown.
	 */
	private void checkFuseBytes(String mcuid) {
		AVRDudeProperties avrdudeprops = fTargetProps.getAVRDudeProperties();

		// State:
		// 0x00 = neither fuses nor lockbits are written
		// 0x01 = fuses not compatible
		// 0x02 = lockbits not compatible
		// 0x03 = both not compatible
		// The state is used as an index to the String arrays with the texts.
		int state = 0x00;

		// Check fuse bytes
		boolean fusewrite = avrdudeprops.getFuseBytes(getCfg()).getWrite();
		if (fusewrite) {
			boolean fusecompatible = avrdudeprops.getFuseBytes(getCfg()).isCompatibleWith(mcuid);
			if (!fusecompatible) {
				state |= 0x01;
			}
		}

		// check lockbits
		boolean lockwrite = avrdudeprops.getLockbitBytes(getCfg()).getWrite();
		if (lockwrite) {
			boolean lockcompatible = avrdudeprops.getLockbitBytes(getCfg()).isCompatibleWith(mcuid);
			if (!lockcompatible) {
				state |= 0x02;
			}
		}

		if (!fusewrite && !lockwrite) {
			// Neither Fuses nor Lockbits are written, so no need for a warning.
			// The fuses tab respective lockbits tab will show a warning once the write flag is
			// changed.
			return;
		}

		if (state == 0) {
			// both fuses and lockbits are compatible, so no need for a warning.
			return;
		}

		// Now show the warning.
		String title = MessageFormat.format(TITLE_FUSEBYTEWARNING, TITLEINSERT[state]);
		String text = MessageFormat.format(TEXT_FUSEBYTEWARNING, TEXTINSERT[state],
				TABNAMEINSERT[state]);
		MessageDialog.openWarning(fMCUcombo.getShell(), title, text);
	}

	/**
	 * Checks if the current target values are different from the original ones and set the rebuild
	 * flag for the configuration / project if yes.
	 */
	private void checkRebuildRequired() {
		if (fOldMCUid != null) {
			if (!(fTargetProps.getMCUId().equals(fOldMCUid))
					|| !(fTargetProps.getFCPU().equals(fOldFCPU))) {
				setRebuildState(true);
			}
		}
	}

	/**
	 * Load the actual MCU from the currently selected Programmer and set the MCU combo accordingly.
	 * <p>
	 * This method will start a new Job to load the values and return immediately.
	 * </p>
	 */
	private void loadComboFromDevice() {

		// Disable the Load Button. It is re-enabled by the load job when it finishes.
		fLoadButton.setEnabled(false);
		fLoadButton.setText(TEXT_LOADBUTTON_BUSY);

		// The Job that does the actual loading.
		Job readJob = new Job("Reading MCU Signature") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {

				try {
					monitor.beginTask("Starting AVRDude", 100);

					final String mcuid = AVRDude.getDefault().getAttachedMCU(
							fTargetProps.getAVRDudeProperties().getProgrammer(),
							new SubProgressMonitor(monitor, 95));

					fTargetProps.setMCUId(mcuid);

					// and update the user interface
					if (!fLoadButton.isDisposed()) {
						fLoadButton.getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								updateData(fTargetProps);

								// Check if supported by avrdude and set the errorpane as
								// required
								checkAVRDude(mcuid);

								// Check fuse byte settings and pop a message if the settings
								// are not compatible
								checkFuseBytes(mcuid);

								// Set the rebuild flag for the configuration
								getCfg().setRebuildState(true);

							}
						});
					}
					monitor.worked(5);
				} catch (AVRDudeException ade) {
					// Show an Error message and exit
					if (!fLoadButton.isDisposed()) {
						UIJob messagejob = new AVRDudeErrorDialogJob(fLoadButton.getDisplay(), ade,
								fTargetProps.getAVRDudeProperties().getProgrammerId());
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
		readJob.setRule(new AVRDudeSchedulingRule(fTargetProps.getAVRDudeProperties()
				.getProgrammer()));
		readJob.setPriority(Job.SHORT);
		readJob.setUser(true);
		readJob.schedule();
	}
}
