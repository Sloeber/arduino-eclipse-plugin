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
import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;

public class ArduinoSerial {
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
	    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, Messages.ArduinoSerial_Unable_To_Open_Port + ComPort, e));
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
     * @param OriginalPorts
     *            The ports available on the system
     * @param defaultComPort
     *            The port to return if no new com port is found
     * @return the new comport if found else the defaultComPort
     */
    public static String wait_for_com_Port_to_appear(MessageConsoleStream console, Vector<String> OriginalPorts, String defaultComPort) {

	Vector<String> NewPorts;
	Vector<String> NewPortsCopy;

	// wait for port to disappear and appear
	int NumTries = 0;
	int MaxTries = 40; // wait for max 10 seconds as arduino does
	int delayMs = 250;
	int PrefNewPortsCopySize = -10;
	do {

	    NewPorts = Serial.list();

	    NewPortsCopy = new Vector<>(NewPorts);
	    for (int i = 0; i < OriginalPorts.size(); i++) {
		NewPortsCopy.remove(OriginalPorts.get(i));
	    }

	    /* dump the serial ports to the console */
	    console.print("PORTS {"); //$NON-NLS-1$
	    for (int i = 0; i < OriginalPorts.size(); i++) {
		console.print(' ' + OriginalPorts.get(i) + ',');
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

	    // code to capture the case: the com port reappears with a name that was in the original list
	    int NewPortsCopySize = NewPorts.size();
	    if ((NewPortsCopy.size() == 0) && (NewPortsCopySize == PrefNewPortsCopySize + 1)) {
		console.println(Messages.ArduinoSerial_Comport_Appeared_and_disappeared);
		return defaultComPort;
	    }
	    PrefNewPortsCopySize = NewPortsCopySize;

	    if (NumTries++ > MaxTries) {
		console.println(Messages.ArduinoSerial_Comport_is_not_behaving_as_expected);
		return defaultComPort;
	    }
	    if (NewPortsCopy.size() == 0) // wait a while before we do the next try
	    {
		try {
		    Thread.sleep(delayMs);
		} catch (InterruptedException e) {// Jaba is not going to write this
		    // code
		}
	    }
	} while (NewPortsCopy.size() == 0);

	console.println(Messages.ArduinoSerial_Comport_reset_took + (NumTries * delayMs) + Messages.ArduinoSerial_miliseconds);
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
     * flush the serial buffer throw everything away which is in there
     * 
     * @param serialPort
     *            The port to clean the serial buffer
     */
    public static void flushSerialBuffer(Serial serialPort) {
	while (serialPort.available() > 0) {
	    serialPort.readBytes();
	    try {
		Thread.sleep(100); // TOFIX I think this is bad; not to bad as
				   // readBytes reads all info but
				   // if the boards sends data at a speed higher
				   // than 1 ever 100ms we will never get out of
				   // this loop
	    } catch (InterruptedException e) { // we can safely ignore all
					       // errors here as we are throwing
					       // everything away anyway
	    }
	}
    }

    /**
     * reset the arduino
     * 
     * This method takes into account all the setting to be able to reset all different types of arduino If RXTXDisabled is set the method only return
     * the parameter Comport
     * 
     * @param project
     *            The project related to the com port to reset
     * @param ComPort
     *            The name of the com port to reset
     * @return The com port to upload to
     */
    public static String makeArduinoUploadready(MessageConsoleStream console, IProject project, String configName, String ComPort) {
	// ArduinoProperties arduinoProperties = new ArduinoProperties(project);
	boolean use_1200bps_touch = Common
		.getBuildEnvironmentVariable(project, configName, ArduinoConst.ENV_KEY_upload_use_1200bps_touch, ArduinoConst.FALSE)
		.equalsIgnoreCase(ArduinoConst.TRUE);
	boolean bDisableFlushing = Common
		.getBuildEnvironmentVariable(project, configName, ArduinoConst.ENV_KEY_upload_disable_flushing, ArduinoConst.FALSE)
		.equalsIgnoreCase(ArduinoConst.TRUE);
	boolean bwait_for_upload_port = Common
		.getBuildEnvironmentVariable(project, configName, ArduinoConst.ENV_KEY_wait_for_upload_port, ArduinoConst.FALSE)
		.equalsIgnoreCase(ArduinoConst.TRUE);
	String boardName = Common.getBuildEnvironmentVariable(project, configName, ArduinoConst.ENV_KEY_JANTJE_BOARD_NAME, ArduinoConst.EMPTY_STRING);
	String upload_protocol = Common.getBuildEnvironmentVariable(project, configName, ArduinoConst.ENV_KEY_UPLOAD_PROTOCOL,
		ArduinoConst.EMPTY_STRING);
	/* Teensy uses halfkay protocol and does not require a reset */
	if (upload_protocol.equalsIgnoreCase("halfkay")) { //$NON-NLS-1$
	    return ComPort;
	}
	/* end of Teensy and halfkay */
	if (use_1200bps_touch) {
	    // Get the list of the current com serial ports
	    console.println(Messages.ArduinoSerial_Using_12000bps_touch);
	    Vector<String> OriginalPorts = Serial.list();

	    if (!reset_Arduino_by_baud_rate(ComPort, 1200, 300) /* || */) {
		console.println(Messages.ArduinoSerial_reset_failed);

	    } else {
		if (boardName.startsWith("Digistump DigiX")) { //$NON-NLS-1$
		    // Give the DUE/DigiX Atmel SAM-BA bootloader time to switch-in after the reset
		    try {
			Thread.sleep(2000);
		    } catch (InterruptedException ex) {
			// ignore error
		    }
		}
		if (bwait_for_upload_port) {
		    String NewComport = wait_for_com_Port_to_appear(console, OriginalPorts, ComPort);
		    console.println(Messages.ArduinoSerial_Using_comport + NewComport + Messages.ArduinoSerial_From_Now_Onwards);
		    console.println(Messages.ArduinoSerial_Ending_reset);
		    return NewComport;
		}
	    }
	    console.println(Messages.ArduinoSerial_Continuing_to_use + ComPort);
	    console.println(Messages.ArduinoSerial_Ending_reset);
	    return ComPort;
	}

	// connect to the serial port
	console.println(Messages.ArduinoSerial_reset_dtr_toggle);
	Serial serialPort;
	try {
	    serialPort = new Serial(ComPort, 9600);
	} catch (Exception e) {
	    e.printStackTrace();
	    Common.log(
		    new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, Messages.ArduinoSerial_exception_while_opening_seral_port + ComPort, e));
	    console.println(Messages.ArduinoSerial_exception_while_opening_seral_port + ComPort);
	    console.println(Messages.ArduinoSerial_Continuing_to_use + ComPort);
	    console.println(Messages.ArduinoSerial_Ending_reset);
	    return ComPort;
	    // throw new RunnerException(e.getMessage());
	}
	if (!serialPort.IsConnected()) {
	    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, Messages.ArduinoSerial_unable_to_open_serial_port + ComPort, null));
	    console.println(Messages.ArduinoSerial_exception_while_opening_seral_port + ComPort);
	    console.println(Messages.ArduinoSerial_Continuing_to_use + ComPort);
	    console.println(Messages.ArduinoSerial_Ending_reset);
	    return ComPort;
	}

	if (!bDisableFlushing) {
	    // Cleanup the serial buffer
	    console.println(Messages.ArduinoSerial_Flushing_buffer);
	    flushSerialBuffer(serialPort);// I wonder is this code on the right
					  // place (I mean before the reset?;
					  // shouldn't it be after?)
	}
	// reset arduino
	console.println(Messages.ArduinoSerial_23);
	ToggleDTR(serialPort, 100);

	serialPort.dispose();
	console.println(Messages.ArduinoSerial_Continuing_to_use + ComPort);
	console.println(Messages.ArduinoSerial_Ending_reset);
	return ComPort;

    }
}
