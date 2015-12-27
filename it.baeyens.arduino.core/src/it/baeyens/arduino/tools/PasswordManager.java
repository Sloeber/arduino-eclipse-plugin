package it.baeyens.arduino.tools;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.ui.PasswordDialog;

public class PasswordManager {
    private String myPassword;
    private String myLogin = null;
    private String myhost = null;

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

    public boolean setHost(String host) {
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
	    if (this.myPassword == null) {
		PasswordDialog dialog = new PasswordDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
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
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	    return false;
	}

	return true;
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
	    // ignore this error
	    e.printStackTrace();
	}

    }

    private static String ConvertHostToNodeName(String host) {

	return "ssh/" + host.replace(ArduinoConst.DOT, "/"); //$NON-NLS-1$ //$NON-NLS-2$
    }

}
