package it.baeyens.arduino.tools;

import it.baeyens.arduino.ui.PasswordDialog;

import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.PlatformUI;

public class PasswordManager {
    private String myPassword;
    private String myLogin = null;
    private String myhost = null;

    public PasswordManager() {
	// no constructor needed
    }

    public String getPassword() {
	return myPassword;
    }

    public String getLogin() {
	return myLogin;
    }

    public String getHost() {
	return myhost;
    }

    public boolean setHost(String host) {
	myhost = host;
	myPassword = null;
	myLogin = null;

	String nodename = ConvertHostToNodeName(myhost);
	ISecurePreferences root = SecurePreferencesFactory.getDefault();
	ISecurePreferences node = root.node(nodename);

	try {
	    if (root.nodeExists(nodename)) {
		myPassword = node.get("password", null);
		myLogin = node.get("login", null);
	    }
	    if (myPassword == null) {
		PasswordDialog dialog = new PasswordDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell());
		if (myLogin != null)
		    dialog.setUser(myLogin);
		dialog.sethost(host);
		// get the new values from the dialog
		if (dialog.open() == Window.OK) {
		    myLogin = dialog.getUser();
		    myPassword = dialog.getPassword();
		    node.put("login", myLogin, false);
		    node.put("password", myPassword, true);
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
		node.put("password", null, true);
	    }

	} catch (StorageException e) {
	    // ignore this error
	    e.printStackTrace();
	}

    }

    private static String ConvertHostToNodeName(String host) {

	return "ssh/" + host.replace(".", "/");
    }

}
