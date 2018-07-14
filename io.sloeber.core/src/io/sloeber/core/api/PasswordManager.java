package io.sloeber.core.api;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;

import io.sloeber.core.Messages;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;

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

	/**
	 * Sets the host. If there is no Login,password for this host the method
	 * returns false. If a login/password for the host is found the method
	 * returns true
	 *
	 * @param host
	 * @return
	 */
	public boolean setHost(String host) {
		this.myhost = host;
		this.myPassword = null;
		this.myLogin = null;

		String nodename = ConvertHostToNodeName(this.myhost);
		ISecurePreferences root = SecurePreferencesFactory.getDefault();

		try {
			if (root.nodeExists(nodename)) {

				ISecurePreferences node = root.node(nodename);
				this.myPassword = node.get(Messages.security_password, null);
				this.myLogin = node.get(Messages.security_login, null);
				return true;
			}

		} catch (StorageException e) {
			Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
					"Set a password on the project properties->arduino page", e)); //$NON-NLS-1$
		}

		return false;
	}

	/**
	 * Sets the login and password for a host
	 *
	 * @param host
	 * @param login
	 * @param password
	 * @throws Exception
	 */
	public void setLoginData(String host, String login, String password) throws Exception {
		this.myhost = host;
		this.myLogin = login;
		this.myPassword = password;
		String nodename = ConvertHostToNodeName(this.myhost);
		ISecurePreferences root = SecurePreferencesFactory.getDefault();
		ISecurePreferences node = root.node(nodename);

		node.put(Messages.security_login, this.myLogin, false);
		node.put(Messages.security_password, this.myPassword, true);
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

		if (root.nodeExists(nodename)) {
			ISecurePreferences node = root.node(nodename);
			node.removeNode();// (Messages.security_password, null, true);
		}

	}

	private static String ConvertHostToNodeName(String host) {

		return "ssh/" + host.replace(Const.DOT, Const.SLACH); //$NON-NLS-1$
	}

}
