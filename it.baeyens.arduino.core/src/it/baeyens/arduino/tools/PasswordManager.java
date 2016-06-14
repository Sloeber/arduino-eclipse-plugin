package it.baeyens.arduino.tools;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.common.Const;
import it.baeyens.arduino.ui.PasswordDialog;

public class PasswordManager {
    private String myPassword;
    private String myLogin = null;
    private String myhost = null;
    boolean ret = false;

    public PasswordManager() {
	// no constructor needed
    }

    public String getPassword() {
	return this.myPassword;
    }

    public String getLogin() {
	return this.myLogin;
    }

    public String getHost() {
	return this.myhost;
    }

    public boolean setHost(String host, boolean askPwdIfNotFound) {
	if (!askPwdIfNotFound) { // if we do not ask the pwd if it is not found
				 // we do not need to be on a gui thread
	    return internalSetHost(host, askPwdIfNotFound);
	}

	Runnable myRunnable = new Runnable() {

	    @Override
	    public void run() {
		try {
		    PasswordManager.this.ret = internalSetHost(host, askPwdIfNotFound);
		} catch (Exception e) {// ignore as we get errors when closing
				       // down
		}
	    }

	};
	Display.getDefault().syncExec(myRunnable);
	return this.ret;
    }

    protected boolean internalSetHost(String host, boolean askPwdIfNotFound) {
	this.myhost = host;
	this.myPassword = null;
	this.myLogin = null;

	String nodename = ConvertHostToNodeName(this.myhost);
	ISecurePreferences root = SecurePreferencesFactory.getDefault();
	ISecurePreferences node = root.node(nodename);

	try {
	    if (root.nodeExists(nodename)) {
		this.myPassword = node.get(Messages.security_password, null);
		this.myLogin = node.get(Messages.security_login, null);
	    }
	    if (!askPwdIfNotFound) {
		return false;
	    }
	    if (this.myPassword == null) {
		PasswordDialog dialog = new PasswordDialog(
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		if (this.myLogin != null)
		    dialog.setUser(this.myLogin);
		dialog.sethost(host);
		// get the new values from the dialog
		if (dialog.open() == Window.OK) {
		    this.myLogin = dialog.getUser();
		    this.myPassword = dialog.getPassword();
		    node.put(Messages.security_login, this.myLogin, false);
		    node.put(Messages.security_password, this.myPassword, true);
		} else {
		    return false;
		}
	    }
	} catch (StorageException e) {
	    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "failed to set login info", e)); //$NON-NLS-1$
	    return false;
	}

	return true;
    }

    static public void setPwd(String host, String login, String pwd) {

	String nodename = ConvertHostToNodeName(host);
	ISecurePreferences root = SecurePreferencesFactory.getDefault();
	ISecurePreferences node = root.node(nodename);

	try {
	    node.put(Messages.security_login, login, false);
	    node.put(Messages.security_password, pwd, false);
	} catch (StorageException e) {

	    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "failed to set login info", e)); //$NON-NLS-1$
	}

    }

    public static void ErasePassword(String host) {
	String nodename = ConvertHostToNodeName(host);
	ISecurePreferences root = SecurePreferencesFactory.getDefault();
	ISecurePreferences node = root.node(nodename);
	try {
	    if (root.nodeExists(nodename)) {
		node.put(Messages.security_password, null, true);
	    }

	} catch (StorageException e) {
	    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "failed to erase login info", e)); //$NON-NLS-1$
	}

    }

    private static String ConvertHostToNodeName(String host) {

	return "ssh/" + host.replace(Const.DOT, Const.SLACH); //$NON-NLS-1$
    }

}
