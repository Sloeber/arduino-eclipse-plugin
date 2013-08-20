package it.baeyens.arduino.common;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

public class PleaseHelp extends Dialog {

	@Override
	protected void handleShellCloseEvent() {// JABA is not going to add code
	}

	public Browser browser = null;
	public PleaseHelp ph;

	@Override
	protected Control createContents(Composite parent) {
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 3;
		parent.setLayout(gridLayout);

		try {
			browser = new Browser(parent, SWT.NONE);
		} catch (SWTError e) {
			System.out.println("Could not instantiate Browser: " + e.getMessage());
		}
		GridData data = new GridData();
		data.horizontalAlignment = GridData.FILL;
		data.verticalAlignment = GridData.FILL;
		data.horizontalSpan = 3;
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		browser.setLayoutData(data);

		browser.addProgressListener(new ProgressListener() {
			@SuppressWarnings("synthetic-access")
			@Override
			public void changed(ProgressEvent event) {
				ph.getButton(IDialogConstants.OK_ID).setEnabled(false);
				ph.getButton(IDialogConstants.CANCEL_ID).setEnabled(false);
			}

			@Override
			public void completed(ProgressEvent event) {
				ph.setBlockOnOpen(false);
				ph.open();
				Job job = new Job("delayed Activate Buttons") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
							@SuppressWarnings("synthetic-access")
							@Override
							public void run() {
								Button curbutton = ph.getButton(IDialogConstants.OK_ID);
								if (curbutton != null) {
									curbutton.setEnabled(true);
								}
								curbutton = ph.getButton(IDialogConstants.CANCEL_ID);
								if (curbutton != null) {
									curbutton.setEnabled(true);
								}
							}
						});

						return Status.OK_STATUS;
					}
				};
				job.setPriority(Job.INTERACTIVE);
				job.schedule(30000);

			}
		});

		buttonBar = createButtonBar(parent);

		browser.setUrl("http://eclipse.baeyens.it/remind2_1.html?os=" + Platform.getOS() + ";arch=" + Platform.getOSArch());

		return parent;

	}

	/**
	 * Static helper function to popup the help jantje page
	 * 
	 */
	static void DoHelp() {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				Shell activeShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				new PleaseHelp(activeShell).create();
			}
		});
	}

	@Override
	protected Point getInitialSize() {
		return this.getShell().computeSize(SWT.MAX, SWT.MAX, true);
	}

	public PleaseHelp(Shell parent) {
		super(parent);
		ph = this;

	}

}
