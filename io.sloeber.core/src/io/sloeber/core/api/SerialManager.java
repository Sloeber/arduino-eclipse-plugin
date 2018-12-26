package io.sloeber.core.api;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;

import cc.arduino.packages.discoverers.SloeberNetworkDiscovery;

public class SerialManager {
	static ISerialUser otherSerialUser = null; // If someone else uses the
	// serial port he can register
	// here so we can
	// request him to disconnect
	// when we need the serial port

	static Set<IProject> fProjects = new HashSet<>();

	/**
	 * This method is used to register a serial user. A serial user is alerted
	 * when the serial port will be disconnected (for instance for a upload) The
	 * serial user is requested to act appropriately Only 1 serial user can be
	 * registered at a given time. No check is done.
	 *
	 * @param serialUser
	 */
	public static void registerSerialUser(ISerialUser serialUser) {
		otherSerialUser = serialUser;
	}

	/**
	 * This method is to unregister a serial user.
	 */
	public static void UnRegisterSerialUser() {
		otherSerialUser = null;
	}

	public static boolean StopSerialMonitor(String mComPort) {
		if (otherSerialUser != null) {
			return otherSerialUser.PauzePort(mComPort);
		}
		return false;
	}

	public static void StartSerialMonitor(String mComPort) {
		if (otherSerialUser != null) {
			otherSerialUser.ResumePort(mComPort);
		}

	}

	public static String[] listComPorts() {
		List<String> serialList = Serial.list();
		String[] outgoing = new String[serialList.size()];
		serialList.toArray(outgoing);
		return outgoing;
	}

	@SuppressWarnings("nls")
	public static String[] listBaudRates() {

		String[] outgoing = { "921600", "460800", "230400", "115200", "76800", "57600", "38400", "31250", "28800",
				"19200", "14400", "9600", "4800", "2400", "1200", "300" };
		return outgoing;
	}

	@SuppressWarnings("nls")
	public static String[] listLineEndings() {
		String[] outgoing = { "none", "CR", "NL", "CR/NL" };
		return outgoing;
	}

	@SuppressWarnings("nls")
	public static String getLineEnding(int selectionIndex) {
		switch (selectionIndex) {
		default:
		case 0:
			return "";
		case 1:
			return "\r";
		case 2:
			return "\n";
		case 3:
			return "\r\n";
		}
	}

	public static String[] listNetworkPorts() {
		return SloeberNetworkDiscovery.getList();
	}

}
