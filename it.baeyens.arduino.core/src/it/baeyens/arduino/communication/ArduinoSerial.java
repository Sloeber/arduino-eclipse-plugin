package it.baeyens.arduino.communication;

/**
 * This file contains serial communication at the arduino level.
 * Lower level methods are in the common package
 * 
 */
import it.baeyens.arduino.arduino.Serial;
import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;

import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class ArduinoSerial {
    /**
     * This method resets arduino based on setting the baud rate. Used for due, leonardo and others
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
	    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Unable to open Serial port " + ComPort, e));
	    return false;
	}

	try {
	    Thread.sleep(openTime);
	} catch (InterruptedException e) {// Jaba is not going to write this
					  // code
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
    public static String wait_for_com_Port_to_appear(Vector<String> OriginalPorts, String defaultComPort) {

	Vector<String> NewPorts;
	Vector<String> OriginalPortsCopy;

	// wait for port to disappear
	int NumTries = 0;
	do {
	    try {
		Thread.sleep(100);
	    } catch (InterruptedException e) {// Jaba is not going to write this
					      // code
	    }
	    OriginalPortsCopy = new Vector<String>(OriginalPorts);
	    if (NumTries++ > 70) {
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Leonardo upload port is not disappearing after reset"));
		return defaultComPort;
	    }
	    NewPorts = Serial.list();
	    for (int i = 0; i < NewPorts.size(); i++) {
		OriginalPortsCopy.remove(NewPorts.get(i));
	    }

	} while (OriginalPortsCopy.size() != 1);
	OriginalPorts.remove(OriginalPortsCopy.get(0));

	NumTries = 0;
	do {
	    if (NumTries++ > 70) {
		Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Leonardo upload port is not appearing after reset"));
		return defaultComPort;
	    }
	    NewPorts = Serial.list();
	    for (int i = 0; i < OriginalPorts.size(); i++) {
		NewPorts.remove(OriginalPorts.get(i));
	    }
	    try {
		Thread.sleep(100);
	    } catch (InterruptedException e) {// Jaba is not going to write this
					      // code
	    }
	} while (NewPorts.size() != 1);
	return NewPorts.get(0);
    }

    /**
     * Toggle DTR this is a way to reset an arduino board
     * 
     * @param Port
     *            the port to togle
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
     * the param Comport
     * 
     * @param project
     *            The project related to the com port to reset
     * @param ComPort
     *            The name of the com port to reset
     * @return The com port to upload to
     */
    public static String makeArduinoUploadready(IProject project, String configName, String ComPort) {
	if (Common.RXTXDisabled())
	    return ComPort;
	// ArduinoProperties arduinoProperties = new ArduinoProperties(project);
	boolean use_1200bps_touch = Common.getBuildEnvironmentVariableBoolean(project, configName, ArduinoConst.ENV_KEY_upload_use_1200bps_touch, false);
	boolean bDisableFlushing = Common.getBuildEnvironmentVariableBoolean(project, configName, ArduinoConst.ENV_KEY_upload_disable_flushing, false);
	boolean bwait_for_upload_port = Common.getBuildEnvironmentVariableBoolean(project, configName, ArduinoConst.ENV_KEY_wait_for_upload_port, false);
	String boardName = Common.getBuildEnvironmentVariable(project, configName, ArduinoConst.ENV_KEY_BOARD_NAME);

	if (boardName.equalsIgnoreCase("Arduino leonardo")
			|| boardName.equalsIgnoreCase("Arduino Micro")
			|| boardName.equalsIgnoreCase("Arduino Esplora")
			|| boardName.startsWith("Arduino Due")
			|| use_1200bps_touch)
	{
	    Vector<String> OriginalPorts = Serial.list();
	    // OriginalPorts.remove(ComPort);
	    if (!reset_Arduino_by_baud_rate(ComPort, 1200, 100) || boardName.startsWith("Arduino Due"))
		return ComPort;
	    if (boardName.equalsIgnoreCase("Arduino leonardo") || boardName.equalsIgnoreCase("Arduino Micro")
		    || boardName.equalsIgnoreCase("Arduino Esplora") || bwait_for_upload_port) {
		return wait_for_com_Port_to_appear(OriginalPorts, ComPort);
	    }
	}

	// connect to the serial port
	Serial serialPort;
	try {
	    serialPort = new Serial(ComPort, 9600);
	} catch (Exception e) {
	    e.printStackTrace();
	    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Unable to open Serial port " + ComPort, e));
	    return ComPort;
	    // throw new RunnerException(e.getMessage());
	}
	if (!serialPort.IsConnected()) {
	    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Unable to open Serial port " + ComPort, null));
	    return ComPort;
	}

	if (!bDisableFlushing) {
	    // Cleanup the serial buffer
	    flushSerialBuffer(serialPort);// I wonder is this code on the right
					  // place (I mean before the reset?;
					  // shouldn't it be after?)
	}
	// reset arduino
	ToggleDTR(serialPort, 100);

	serialPort.dispose();
	return ComPort;

    }
}
