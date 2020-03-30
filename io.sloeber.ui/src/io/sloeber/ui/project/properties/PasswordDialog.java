/*
 * Started from http://www.vogella.com/articles/EclipseDialogs/article.html#tutorial_passworddialog
 * adapted by Jantje
 */
package io.sloeber.ui.project.properties;

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

import io.sloeber.core.api.PasswordManager;
import io.sloeber.ui.Messages;

public class PasswordDialog extends Dialog {
	private static final int DELETE = 10;

	@Override
	protected void buttonPressed(int buttonId) {
		switch (buttonId) {
		case IDialogConstants.OK_ID: {
			PasswordManager.setPwd(this.myPassWordmanager.getHost(), this.txtUser.getText(),
					this.txtPassword.getText());
			break;
		}
		case DELETE:
			PasswordManager.ErasePassword(this.myPassWordmanager.getHost());
			this.close();
			return;
		default:
			break;
		}
		super.buttonPressed(buttonId);
	}

	private Text txtUser;
	private Text txtPassword;
	private PasswordManager myPassWordmanager;

	public PasswordDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		parent.getShell().setText(Messages.ui_sec_login_and_password + this.myPassWordmanager.getHost());
		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout layout = new GridLayout(2, false);
		layout.marginRight = 5;
		layout.marginLeft = 10;
		container.setLayout(layout);

		Label lblUser = new Label(container, SWT.NONE);
		lblUser.setText(Messages.ui_sec_login + ':');

		this.txtUser = new Text(container, SWT.BORDER);
		this.txtUser.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		String login = this.myPassWordmanager.getLogin();
		if (login == null) {
			login = new String();
		}
		this.txtUser.setText(login);

		Label lblPassword = new Label(container, SWT.NONE);
		GridData gd_lblNewLabel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_lblNewLabel.horizontalIndent = 1;
		lblPassword.setLayoutData(gd_lblNewLabel);
		lblPassword.setText(Messages.ui_sec_password + ':');

		this.txtPassword = new Text(container, SWT.BORDER);
		this.txtPassword.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		String pwd = this.myPassWordmanager.getPassword();
		if (pwd == null) {
			pwd = new String();
		}
		this.txtPassword.setText(pwd);

		return container;
	}

	// override method to use "Login" as label for the OK button
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, DELETE, Messages.ui_sec_delete, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected Point getInitialSize() {
		return new Point(450, 300);
	}

	public void setPasswordManager(PasswordManager passWordmanager) {
		this.myPassWordmanager = passWordmanager;

	}
}