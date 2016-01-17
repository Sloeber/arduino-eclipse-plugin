package it.baeyens.arduino.common;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.browser.ProgressListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
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
    private static String myhelpLocation;

    @Override
    protected Control createContents(Composite parent) {
	GridLayout gridLayout = new GridLayout();
	gridLayout.numColumns = 3;
	parent.setLayout(gridLayout);

	try {
	    this.browser = new Browser(parent, SWT.NONE);
	} catch (SWTError e) {
	    System.out.println("Could not instantiate Browser: " + e.getMessage()); //$NON-NLS-1$
	}
	GridData data = new GridData();
	data.horizontalAlignment = GridData.FILL;
	data.verticalAlignment = GridData.FILL;
	data.horizontalSpan = 3;
	data.grabExcessHorizontalSpace = true;
	data.grabExcessVerticalSpace = true;
	this.browser.setLayoutData(data);

	this.browser.addProgressListener(new ProgressListener() {
	    // @SuppressWarnings("synthetic-access")
	    @Override
	    public void changed(ProgressEvent event) {
		// ph.getButton(IDialogConstants.OK_ID).setEnabled(false);
		// ph.getButton(IDialogConstants.CANCEL_ID).setEnabled(false);
	    }

	    @Override
	    public void completed(ProgressEvent event) {
		PleaseHelp.this.ph.setBlockOnOpen(false);
		PleaseHelp.this.ph.open();
	    }
	});

	this.buttonBar = createButtonBar(parent);

	this.browser.setUrl(myhelpLocation + "?os=" + Platform.getOS() + ";arch=" + Platform.getOSArch()); //$NON-NLS-1$ //$NON-NLS-2$

	return parent;

    }

    /**
     * Static helper function to popup the help jantje page
     * 
     */
    static void DoHelp(String helpLocation) {
	myhelpLocation = helpLocation;
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
	this.ph = this;

    }

}
