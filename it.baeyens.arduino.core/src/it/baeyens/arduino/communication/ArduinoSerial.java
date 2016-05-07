package it.baeyens.arduino.communication;

import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.MessageConsoleStream;

/**
 * This file contains serial communication at the arduino level.
 * Lower level methods are in the common package
 * 
 */
import it.baeyens.arduino.arduino.Serial;
import it.baeyens.arduino.common.Common;
import it.baeyens.arduino.common.Const;

public class ArduinoSerial {

    private ArduinoSerial() {
    }

    /**
     * This method resets arduino based on setting the baud rate. Used for due, Leonardo and others
     * 
     * @param ComPort
     *            The port to set the baud rate
     * @param bautrate
     *            The baud rate to set
     * @return true is successful otherwise false
     */
    public static boolean reset_Arduino_by_baud_rate(String ComPort, int baudrate, long openTime) {
	Serial serialPort;
	try {
	    serialPort = new Serial(ComPort, baudrate);
	} catch (Exception e) {
	    e.printStackTrace();
	    Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, Messages.ArduinoSerial_Unable_To_Open_Port + ComPort, e));
	    return false;
	}
	if (!Platform.getOS().equals(Platform.OS_MACOSX)) {
	    try {
		Thread.sleep(openTime);
	    } catch (InterruptedException e) {// Jaba is not going to write this
					      // code
	    }
	}
	serialPort.dispose();
	return true;
    }

    /**
     * Waits for a serial port to appear. It is assumed that the default comport is not available on the system
     * 
     * @param originalPorts
     *            The ports available on the system
     * @param defaultComPort
     *            The port to return if no new com port is found
     * @return the new comport if found else the defaultComPort
     */
    public static String wait_for_com_Port_to_appear(MessageConsoleStream console, Vector<String> originalPorts, String defaultComPort) {

	Vector<String> NewPorts;
	Vector<String> NewPortsCopy;

	// wait for port to disappear and appear
	int numTries = 0;
	int maxTries = 40; // wait for max 10 seconds as arduino does
	int delayMs = 250;
	int prefNewPortsCopySize = -10;
	do {

	    NewPorts = Serial.list();

	    NewPortsCopy = new Vector<>(NewPorts);
	    for (int i = 0; i < originalPorts.size(); i++) {
		NewPortsCopy.remove(originalPorts.get(i));
	    }

	    /* dump the serial ports to the console */
	    console.print("PORTS {"); //$NON-NLS-1$
	    for (int i = 0; i < originalPorts.size(); i++) {
		console.print(' ' + originalPorts.get(i) + ',');
	    }
	    console.print("} / {"); //$NON-NLS-1$
	    for (int i = 0; i < NewPorts.size(); i++) {
		console.print(' ' + NewPorts.get(i) + ',');
	    }
	    console.print("} => {"); //$NON-NLS-1$
	    for (int i = 0; i < NewPortsCopy.size(); i++) {
		console.print(' ' + NewPortsCopy.get(i) + ',');
	    }
	    console.println("}"); //$NON-NLS-1$
	    /* end of dump to the console */

	    // code to capture the case: the com port reappears with a name that
	    // was in the original list
	    int newPortsCopySize = NewPorts.size();
	    if ((NewPortsCopy.isEmpty()) && (newPortsCopySize == prefNewPortsCopySize + 1)) {
		console.println(Messages.ArduinoSerial_Comport_Appeared_and_disappeared);
		return defaultComPort;
	    }
	    prefNewPortsCopySize = newPortsCopySize;

	    if (numTries++ > maxTries) {
		console.println(Messages.ArduinoSerial_Comport_is_not_behaving_as_expected);
		return defaultComPort;
	    }
	    if (NewPortsCopy.isEmpty()) // wait a while before we do the next
					// try
	    {
		try {
		    Thread.sleep(delayMs);
		} catch (InterruptedException e) {// Jaba is not going to write
						  // this
		    // code
		}
	    }
	} while (NewPortsCopy.isEmpty());

	console.println(Messages.ArduinoSerial_Comport_reset_took + (numTries * delayMs) + Messages.ArduinoSerial_miliseconds);
	return NewPortsCopy.get(0);
    }

    /**
     * Toggle DTR this is a way to reset an arduino board
     * 
     * @param Port
     *            the port to toggle
     * @param delay
     *            the time to wait between the 2 toggle commands
     * @return true is successful otherwise false
     */
    public static boolean ToggleDTR(Serial serialPort, long delay) {
	serialPort.setDTR(false);
	serialPort.setRTS(false);

	try {
	    Thread.sleep(delay);
	} catch (InterruptedException e) {// Jaba is not going to write this
					  // code
	}

	serialPort.setDTR(true);
	serialPort.setRTS(true);
	return true;
    }

    /**
     * reset the arduino
     * 
     * This method takes into account all the setting to be able to reset all different types of arduino If RXTXDisabled is set the method only return the parameter Comport
     * 
     * @param project
     *            The project related to the com port to reset
     * @param comPort
     *            The name of the com port to reset
     * @return The com port to upload to
     */
    public static String makeArduinoUploadready(MessageConsoleStream console, IProject project, String configName, String comPort) {
	boolean use_1200bps_touch = Common.getBuildEnvironmentVariable(project, configName, Const.ENV_KEY_UPLOAD_USE_1200BPS_TOUCH, Const.FALSE).equalsIgnoreCase(Const.TRUE);
	boolean bWaitForUploadPort = Common.getBuildEnvironmentVariable(project, configName, Const.ENV_KEY_WAIT_FOR_UPLOAD_PORT, Const.FALSE).equalsIgnoreCase(Const.TRUE);
	String boardName = Common.getBuildEnvironmentVariable(project, configName, Const.ENV_KEY_JANTJE_BOARD_NAME, Const.EMPTY_STRING);
	String uploadProtocol = Common.getBuildEnvironmentVariable(project, configName, Const.get_ENV_KEY_PROTOCOL(Const.ACTION_UPLOAD), Const.EMPTY_STRING);

	boolean bResetPortForUpload = Common.getBuildEnvironmentVariable(project, configName, Const.ENV_KEY_RESET_BEFORE_UPLOAD, Const.TRUE).equalsIgnoreCase(Const.TRUE);

	/*
	 * Teensy uses halfkay protocol and does not require a reset in boards.txt use Const.ENV_KEY_RESET_BEFORE_UPLOAD=FALSE to disable a reset
	 */
	if (!bResetPortForUpload || uploadProtocol.equalsIgnoreCase("halfkay")) { //$NON-NLS-1$
	    return comPort;
	}
	/*
	 * if the com port can not be found and no specific com port reset method is specified assume it is a network port and do not try to reset
	 */
	Vector<String> originalPorts = Serial.list();
	if (!originalPorts.contains(comPort) && !use_1200bps_touch && !bWaitForUploadPort) {
	    console.println(Messages.ArduinoSerial_comport_not_found);
	    return comPort;
	}
	if (use_1200bps_touch) {
	    // Get the list of the current com serial ports
	    console.println(Messages.ArduinoSerial_Using_12000bps_touch);

	    if (!reset_Arduino_by_baud_rate(comPort, 1200, 300) /* || */) {
		console.println(Messages.ArduinoSerial_reset_failed);

	    } else {
		if (boardName.startsWith("Digistump DigiX")) { //$NON-NLS-1$
		    // Give the DUE/DigiX Atmel SAM-BA bootloader time to
		    // switch-in after the reset
		    try {
			Thread.sleep(2000);
		    } catch (InterruptedException ex) {
			// ignore error
		    }
		}
		if (bWaitForUploadPort) {
		    String newComport = wait_for_com_Port_to_appear(console, originalPorts, comPort);
		    console.println(Messages.ArduinoSerial_Using_comport + newComport + Messages.ArduinoSerial_From_Now_Onwards);
		    console.println(Messages.ArduinoSerial_Ending_reset);
		    return newComport;
		}
	    }
	    console.println(Messages.ArduinoSerial_Continuing_to_use + comPort);
	    console.println(Messages.ArduinoSerial_Ending_reset);
	    return comPort;
	}

	// connect to the serial port
	console.println(Messages.ArduinoSerial_reset_dtr_toggle);
	Serial serialPort;
	try {
	    serialPort = new Serial(comPort, 9600);
	} catch (Exception e) {
	    e.printStackTrace();
	    Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, Messages.ArduinoSerial_exception_while_opening_seral_port + comPort, e));
	    console.println(Messages.ArduinoSerial_exception_while_opening_seral_port + comPort);
	    console.println(Messages.ArduinoSerial_Continuing_to_use + comPort);
	    console.println(Messages.ArduinoSerial_Ending_reset);
	    return comPort;
	}
	if (!serialPort.IsConnected()) {
	    Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, Messages.ArduinoSerial_unable_to_open_serial_port + comPort, null));
	    console.println(Messages.ArduinoSerial_exception_while_opening_seral_port + comPort);
	    console.println(Messages.ArduinoSerial_Continuing_to_use + comPort);
	    console.println(Messages.ArduinoSerial_Ending_reset);
	    return comPort;
	}

	console.println(Messages.ArduinoSerial_23);
	ToggleDTR(serialPort, 100);

	serialPort.dispose();
	console.println(Messages.ArduinoSerial_Continuing_to_use + comPort);
	console.println(Messages.ArduinoSerial_Ending_reset);
	return comPort;

    }
}
