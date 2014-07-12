package it.baeyens.arduino.communication;

/**
 * This file contains serial communication at the arduino level.
 * Lower level methods are in the common package
 * 
 */
import it.baeyens.arduino.arduino.Serial;
import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;

import java.util.Arrays;
import java.util.Vector;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class ArduinoSerial {

    /**
     * This method resets Arduino based on setting the baud rate and disconnecting from the port.
     * Used for due, leonardo, esplora, and others.  Here is from Esplora documentation:
     *
     * 	   Rather than requiring a physical press of the reset button before an upload, the Esplora is
     * 	   designed in a way that allows it to be reset by software running on a connected computer.
     * 	   The reset is triggered when the Esplora's virtual (CDC) serial / COM port is opened at
     * 	   1200 baud and then closed. When this happens, the processor will reset, breaking the USB
     * 	   connection to the computer (meaning that the virtual serial / COM port will disappear).
     * 	   After the processor resets, the bootloader starts, remaining active for about 8 seconds
     * 
     * @param comPort
     *            The port to set the baud rate
     * @param baudRate
     *            The baud rate to set
     * @param keepOpenPeriod
     *            How long to keep the port open (ms)
     *
     * @return true is successful otherwise false
     */
    public static boolean resetArduinoByBaudRate (String comPort, int baudRate, long keepOpenPeriod) {
	Serial serialPort;

	try {
	    serialPort = new Serial(comPort, baudRate);
	} catch (Exception e) {
	    e.printStackTrace();
	    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Unable to open Serial port " +
			    comPort + " and baudRate " + baudRate, e));
	    return false;
	}

	waitFor(keepOpenPeriod);

	if (serialPort != null)
	    serialPort.dispose();

	return true;
    }

    /**
     * Waits for a serial port to appear. It is assumed that the default comport is not available on the system
     * 
     * @param originalPorts
     *            The ports available on the system
     * @param comPort
     *            The port to return if no new com port is found
     * @return the new comport if found else the defaultComPort
     */
    public static String waitForComPortsToReappear (Vector<String> originalPorts, String comPort, String boardName) {

	// wait for port to disappear
	int maxAttempts = 200;
	int pauseForMs = 5;
	int attemptsCount;

	Vector<String> disconnectedPorts = null;

	for (attemptsCount = 0; attemptsCount < maxAttempts; attemptsCount++) {
	    Vector<String> currentPorts = Serial.list();
	    if (currentPorts.size() < originalPorts.size()) {
		disconnectedPorts = new Vector<String>(originalPorts);
		disconnectedPorts.removeAll(currentPorts);
		Common.log(new Status(IStatus.INFO, ArduinoConst.CORE_PLUGIN_ID,
			boardName + " port(s) disconnected [" + disconnectedPorts.toString() + "] after " + attemptsCount * pauseForMs +"ms"));
		break;
	    }

	    if (attemptsCount > 0)
		waitFor(pauseForMs);
	}

	if (attemptsCount == maxAttempts && (disconnectedPorts == null || disconnectedPorts.isEmpty())) {
	    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, boardName + " upload port is not disappearing after reset and " + attemptsCount * pauseForMs + "ms"));
	    return comPort;
	}

	for (attemptsCount = 0; attemptsCount < maxAttempts; attemptsCount++) {
	    if (Serial.list().contains(comPort)) {
		Common.log(new Status(IStatus.INFO, ArduinoConst.CORE_PLUGIN_ID,
				boardName + " port " + comPort + " reconnected after " + attemptsCount * pauseForMs));
		return comPort;
	    }
	    if (attemptsCount > 0)
		waitFor(pauseForMs);
	}

	if (attemptsCount == maxAttempts) {
	    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, boardName + " upload port is not appearing after reset"));

	}

	return comPort;
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

	waitFor(delay);

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
	    // TOFIX I think this is bad; not to bad as
	    // readBytes reads all info but
	    // if the boards sends data at a speed higher
	    // than 1 ever 100ms we will never get out of
	    // this loop
	    waitFor(100);
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
     * @param configName
     *            Key into build environment variables hash
     * @param comPort
     *            The name of the com port to reset
     * @return The com port to upload to
     */
    public static String makeArduinoUploadReady(IProject project, String configName, String comPort) {

	if (Common.RXTXDisabled())
	    return comPort;

	boolean bUse1200bpsTouch = Common.getBuildEnvironmentVariable(project, configName, ArduinoConst.ENV_KEY_upload_use_1200bps_touch, "false")
			.equalsIgnoreCase("true");
	boolean bDisableFlushing = Common.getBuildEnvironmentVariable(project, configName, ArduinoConst.ENV_KEY_upload_disable_flushing, "false")
			.equalsIgnoreCase("true");
	boolean bWaitForUploadPort = Common.getBuildEnvironmentVariable(project, configName, ArduinoConst.ENV_KEY_wait_for_upload_port, "false")
			.equalsIgnoreCase("true");
	boolean bForceNoWaitForUploadPort = Common.getBuildEnvironmentVariable(project, configName, ArduinoConst.ENV_KEY_force_no_wait_for_upload_port, "false")
			.equalsIgnoreCase("true");

	String boardName = Common.getBuildEnvironmentVariable(project, configName, ArduinoConst.ENV_KEY_JANTJE_BOARD_NAME, "");

	if (boardRequiresBaudReset(boardName) || bUse1200bpsTouch) {
	    Vector<String> serialPorts = Serial.list();
	    if (!resetArduinoByBaudRate(comPort, 1200, 0) || boardRequiresResetPause(boardName)) {
		// Give the DUE/DigiX Atmel SAM-BA bootloader time to switch-in after the reset
		waitFor(2000);
		return comPort;
	    }
	    if (!bForceNoWaitForUploadPort) {  // allows completely skipping waiting for ports to speed things up
		if (boardNeedsToWaitForPorts(boardName) || bWaitForUploadPort) {
		    return waitForComPortsToReappear(serialPorts, comPort, boardName);
		}
	    }
	}

	reconnectAndFlushSerialPort(comPort, bDisableFlushing);

	return comPort;
    }


    // ____________________________________________________________________________
    //
    // private helpers


    private static void reconnectAndFlushSerialPort (String comPort, boolean bDisableFlushing) {
	// connect to the serial port
	Serial serialPort = null;
	try {
	    serialPort = new Serial(comPort, 9600);
	} catch (Exception e) {
	    e.printStackTrace();
	    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Unable to open Serial port " + comPort, e));
	}

	if (serialPort == null || !serialPort.IsConnected()) {
	    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Unable to open Serial port " + comPort, null));
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
    }

    private static void waitFor(long delayMs) {
	try {
	    Thread.sleep(delayMs);
	} catch (InterruptedException e) {
	}
    }

    private static final String[] BOARDS_RESET_BY_BAUD = {"arduino leonardo", "arduino micro", "arduino esplora", "arduino due"};
    private static final String[] BOARDS_NEEDING_RESET_PAUSE = {"arduino due", "digistump digix"};
    private static final String[] BOARDS_NEEDING_TO_WAIT_FOR_PORTS = {"arduino leonardo", "arduino micro", "arduino esplora"};

    private static boolean boardRequiresBaudReset(String board) {
	return Arrays.asList(BOARDS_RESET_BY_BAUD).contains(board.toLowerCase());
    }

    private static boolean boardRequiresResetPause(String board) {
	return Arrays.asList(BOARDS_NEEDING_RESET_PAUSE).contains(board.toLowerCase());
    }

    private static boolean boardNeedsToWaitForPorts(String board) {
	return Arrays.asList(BOARDS_NEEDING_TO_WAIT_FOR_PORTS).contains(board.toLowerCase());
    }



}
