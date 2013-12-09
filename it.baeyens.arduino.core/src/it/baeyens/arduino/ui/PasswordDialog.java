/*
 * Started from http://www.vogella.com/articles/EclipseDialogs/article.html#tutorial_passworddialog
 * adapted by Jantje
 */
package it.baeyens.arduino.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class PasswordDialog extends Dialog {
    private Text txtUser;
    private Text txtPassword;
    private String myUser = "";
    private String myPassword = "";
    private String myHost = "No host name provided";

    public PasswordDialog(Shell parentShell) {
	super(parentShell);
    }

    @Override
    protected Control createDialogArea(Composite parent) {
	parent.getShell().setText("enter login and pasword for " + myHost);
	Composite container = (Composite) super.createDialogArea(parent);
	GridLayout layout = new GridLayout(2, false);
	layout.marginRight = 5;
	layout.marginLeft = 10;
	container.setLayout(layout);

	Label lblUser = new Label(container, SWT.NONE);
	lblUser.setText("User:");

	txtUser = new Text(container, SWT.BORDER);
	txtUser.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
	txtUser.setText(myUser);

	Label lblPassword = new Label(container, SWT.NONE);
	GridData gd_lblNewLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
	gd_lblNewLabel.horizontalIndent = 1;
	lblPassword.setLayoutData(gd_lblNewLabel);
	lblPassword.setText("Password:");

	txtPassword = new Text(container, SWT.BORDER);
	txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
	txtPassword.setText(myPassword);

	return container;
    }

    // override method to use "Login" as label for the OK button
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
	createButton(parent, IDialogConstants.OK_ID, "Login", true);
	createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
    }

    @Override
    protected Point getInitialSize() {
	return new Point(450, 300);
    }

    @Override
    protected void okPressed() {
	// Copy data from SWT widgets into fields on button press.
	// Reading data from the widgets later will cause an SWT
	// widget disposed exception.
	myUser = txtUser.getText();
	myPassword = txtPassword.getText();
	super.okPressed();
    }

    public String getUser() {
	return myUser;
    }

    public void setUser(String user) {
	this.myUser = user;
    }

    public String getPassword() {
	return myPassword;
    }

    public void setPassword(String password) {
	this.myPassword = password;
    }

    public void sethost(String host) {
	this.myHost = host;
    }
}