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
 * $Id: MCUReadActionPart.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.ui.editors;

import it.baeyens.avreclipse.core.avrdude.AVRDudeException;
import it.baeyens.avreclipse.core.avrdude.AVRDudeSchedulingRule;
import it.baeyens.avreclipse.core.avrdude.ProgrammerConfig;
import it.baeyens.avreclipse.core.toolinfo.AVRDude;
import it.baeyens.avreclipse.core.toolinfo.fuses.ByteValues;
import it.baeyens.avreclipse.ui.actions.ActionType;
import it.baeyens.avreclipse.ui.dialogs.AVRDudeErrorDialogJob;
import it.baeyens.avreclipse.ui.dialogs.FileMCUMismatchDialog;
import it.baeyens.avreclipse.ui.dialogs.SelectProgrammerDialog;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWTException;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.progress.UIJob;


/**
 * A <code>IFormPart</code> that adds an action to the form toolbar to read the values from a
 * programmer.
 * 
 * @see AbstractActionPart
 * 
 * @author Thomas Holland
 * @since 2.3
 * 
 */
public class MCUReadActionPart extends AbstractActionPart {


	@Override
	protected IAction[] getAction() {

		ActionType type = ActionType.READ;

		Action readAction = new Action() {

			@Override
			public void run() {

				// Open the "Change MCU" dialog
				SelectProgrammerDialog dialog = new SelectProgrammerDialog(getManagedForm()
						.getForm().getShell(), null);

				if (dialog.open() == IDialogConstants.OK_ID) {

					ProgrammerConfig progcfg = dialog.getResult();

					readFuseBytesFromDevice(progcfg);
				}
			}
		};
		type.setupAction(readAction);

		IAction[] allactions = new IAction[1];
		allactions[0] = readAction;

		return allactions;

	}

	/**
	 * Load the Bytes from the currently attached MCU.
	 */
	private void readFuseBytesFromDevice(final ProgrammerConfig progcfg) {

		// Set the form busy. It is restored from the job.
		getManagedForm().getForm().setBusy(true);

		// The Job that does the actual loading.
		Job readJob = new Job("Reading Fuse Bytes") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {

				final ScrolledForm form = getManagedForm().getForm();

				try {
					monitor.beginTask("Starting AVRDude", 100);

					// Read the values from the attached programmer.
					final ByteValues newvalues = readByteValues(progcfg, new SubProgressMonitor(
							monitor, 95));

					// update
					if (form.isDisposed()) {
						return Status.CANCEL_STATUS;
					}
					form.getDisplay().syncExec(new Runnable() {
						// Run in the UI thread in case we have to open a MCU mismatch dialog.
						@Override
						public void run() {
							ByteValues current = getByteValues();
							boolean forceMCU = false;

							// Check if the mcus are compatible
							String projectmcu = getByteValues().getMCUId();
							String newmcu = newvalues.getMCUId();

							if (current.isCompatibleWith(newmcu)) {
								// Compatible MCUs
								// Change the MCU type anyway to be consistent
								forceMCU = true;
							} else {
								// No, they are not compatible. Ask the user what to do
								// "Convert", "Change" or "Cancel"
								Dialog dialog = new FileMCUMismatchDialog(form.getShell(), newmcu,
										projectmcu, current.getType());
								int choice = dialog.open();
								switch (choice) {
									case FileMCUMismatchDialog.CANCEL:
										return;
									case FileMCUMismatchDialog.CHANGE:
										// Change project ByteValues to the new MCU and then copy
										// the values
										forceMCU = true;
										break;
									case FileMCUMismatchDialog.CONVERT:
										// Change the new ByteValues to our MCU and then copy
										// the values
										forceMCU = false;
										break;
								}
							}
							current.setValues(newvalues, forceMCU);
							markDirty();
							notifyForm();
						}
					});
					monitor.worked(5);
				} catch (AVRDudeException ade) {
					// Show an Error message and exit
					if (!form.isDisposed()) {
						UIJob messagejob = new AVRDudeErrorDialogJob(form.getDisplay(), ade,
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
					// remove the busy cursor
					if (!form.isDisposed()) {
						form.getDisplay().syncExec(new Runnable() {
							@Override
							public void run() {
								form.setBusy(false);
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
	 * Read the bytes values from the attached device.
	 * <p>
	 * Depending on the type of the current <code>ByteValues</code> either the FUSES or the
	 * LOCKBITS are loaded.
	 * </p>
	 * 
	 * @param progcfg
	 *            <code>ProgrammerConfig</code> to access the programmer.
	 * @param monitor
	 *            <code>IProgressMonitor</code> to cancel the operation.
	 * @return A new <code>ByteValues</code> object representing the state of the attached MCU.
	 * @throws AVRDudeException
	 *             for any errors during the read operation.
	 */
	private ByteValues readByteValues(ProgrammerConfig progcfg, IProgressMonitor monitor)
			throws AVRDudeException {
		switch (getByteValues().getType()) {
			case FUSE:
				return AVRDude.getDefault().getFuseBytes(progcfg, monitor);
			case LOCKBITS:
				return AVRDude.getDefault().getLockbits(progcfg, monitor);
			default:
				// Should not happen.
				return null;
		}

	}

}
