package io.sloeber.core.communication;

import static io.sloeber.core.Messages.*;
import static io.sloeber.core.api.Common.*;
import static io.sloeber.core.api.Const.*;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.console.MessageConsoleStream;

import io.sloeber.arduinoFramework.api.BoardDescription;
import io.sloeber.core.Activator;
import io.sloeber.core.api.ISloeberConfiguration;
import io.sloeber.core.api.Serial;

public class ArduinoSerial {

    private ArduinoSerial() {
    }

    /**
     * This method resets arduino based on setting the baud rate. Used for due,
     * Leonardo and others
     *
     * @param comPort
     *            The port to set the baud rate
     * @param bautrate
     *            The baud rate to set
     * @param openTime
     *            Time to wait after the port has been closed again
     *
     * @return true is successful otherwise false
     */

    public static boolean reset_Arduino_by_baud_rate(String comPort, int baudRate, long openTime) {
        Serial serialPort;
        try {
            serialPort = new Serial(comPort, baudRate);
            serialPort.connect();
            serialPort.setDTR(false);
            serialPort.dispose();
            Thread.sleep(openTime);
        } catch (Exception e) {
            Activator.log(new Status(IStatus.WARNING, CORE_PLUGIN_ID,
                    ArduinoSerial_unable_to_open_serial_port.replace(PORT_TAG, comPort), e));
            return false;
        }
        return true;
    }

    /**
     * Waits for a unknown serial port to appear. It is assumed that the default
     * comport is not available on the system
     *
     * @param originalPorts
     *            The ports available on the system
     * @param defaultComPort
     *            The port to return if no new com port is found
     * @return the new comport if found else the defaultComPort
     */
    private static String wait_for_com_Port_to_appear(MessageConsoleStream console, List<String> originalPorts,
            String defaultComPort) {

        List<String> newPorts;
        List<String> newPortsCopy;

        // wait for port to disappear and appear
        int numTries = 0;
        int maxTries = 40; // wait for max 10 seconds as arduino does
        int delayMs = 250;
        int prefNewPortsCopySize = originalPorts.size();
        do {

            newPorts = Serial.list();

            newPortsCopy = new ArrayList<>(newPorts);
            for (int i = 0; i < originalPorts.size(); i++) {
                newPortsCopy.remove(originalPorts.get(i));
            }

            /* dump the serial ports to the console */
            console.print("PORTS {"); //$NON-NLS-1$
            for (int i = 0; i < originalPorts.size(); i++) {
                console.print(' ' + originalPorts.get(i) + ',');
            }
            console.print("} / {"); //$NON-NLS-1$
            for (int i = 0; i < newPorts.size(); i++) {
                console.print(' ' + newPorts.get(i) + ',');
            }
            console.print("} => {"); //$NON-NLS-1$
            for (int i = 0; i < newPortsCopy.size(); i++) {
                console.print(' ' + newPortsCopy.get(i) + ',');
            }
            console.println("}"); //$NON-NLS-1$
            /* end of dump to the console */

            // code to capture the case: the com port reappears with a name that
            // was in the original list
            int newPortsCopySize = newPorts.size();
            if ((newPortsCopy.isEmpty()) && (newPortsCopySize > prefNewPortsCopySize)) {
                console.println(ArduinoSerial_Comport_Appeared_and_disappeared);
                console.println(ArduinoSerial_Comport_reset_took.replace(MS_TAG, Integer.toString(numTries * delayMs)));
                return defaultComPort;
            }
            prefNewPortsCopySize = newPortsCopySize;

            if (numTries++ > maxTries) {
                console.println(ArduinoSerial_Comport_is_not_behaving_as_expected);
                return defaultComPort;
            }

            if (newPortsCopy.isEmpty()) // wait a while before we do the next
                                        // try
            {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException e) {
                	e.printStackTrace();
                }
            }
        } while (newPortsCopy.isEmpty());

        console.println(ArduinoSerial_Comport_reset_took.replace(MS_TAG, Integer.toString(numTries * delayMs)));
        return newPortsCopy.get(0);

    }

    /**
     * reset the arduino
     *
     * This method takes into account all the setting to be able to reset all
     * different types of arduino If RXTXDisabled is set the method only return the
     * parameter Comport
     *
     * @param project
     *            The project related to the com port to reset
     * @param comPort
     *            The name of the com port to reset
     * @return The com port to upload to
     */
    public static String makeArduinoUploadready(MessageConsoleStream console, ISloeberConfiguration sloeberConf) {

        BoardDescription boardDescriptor = sloeberConf.getBoardDescription();
        boolean use_1200bps_touch = getBuildEnvironmentVariable(sloeberConf, ENV_KEY_UPLOAD_USE_1200BPS_TOUCH, FALSE)
                .equalsIgnoreCase(TRUE);
        boolean bWaitForUploadPort = getBuildEnvironmentVariable(sloeberConf, ENV_KEY_WAIT_FOR_UPLOAD_PORT, FALSE)
                .equalsIgnoreCase(TRUE);
        String comPort = boardDescriptor.getActualUploadPort();

        if (!use_1200bps_touch) {
            return comPort;
        }
        /*
         * if the com port can not be found and no specific com port reset method is
         * specified assume it is a network port and do not try to reset
         */
        List<String> originalPorts = Serial.list();
        if (!originalPorts.contains(comPort) && !use_1200bps_touch && !bWaitForUploadPort) {
            console.println(ArduinoSerial_comport_not_found + ' ' + comPort);
            return comPort;
        }
        console.println(ArduinoSerial_Using_1200bps_touch.replace(PORT_TAG, comPort));

        if (!reset_Arduino_by_baud_rate(comPort, 1200, 500)) {
            console.println(ArduinoSerial_reset_failed);

        } else {

            if (bWaitForUploadPort) {
                String newComport = wait_for_com_Port_to_appear(console, originalPorts, comPort);
                console.println(ArduinoSerial_Using_comport.replace(PORT_TAG, newComport));
                console.println(ArduinoSerial_Ending_reset);
                return newComport;
            }
        }
        console.println(ArduinoSerial_Continuing_to_use.replace(PORT_TAG, comPort));
        console.println(ArduinoSerial_Ending_reset);
        return comPort;
    }
}
