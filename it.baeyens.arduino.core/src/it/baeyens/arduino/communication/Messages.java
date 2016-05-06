package it.baeyens.arduino.communication;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "it.baeyens.arduino.communication.messages"; //$NON-NLS-1$
    public static String ArduinoSerial_23;
    public static String ArduinoSerial_Comport_Appeared_and_disappeared;
    public static String ArduinoSerial_Comport_is_not_behaving_as_expected;
    public static String ArduinoSerial_comport_not_found;
    public static String ArduinoSerial_Comport_reset_took;
    public static String ArduinoSerial_Continuing_to_use;
    public static String ArduinoSerial_Ending_reset;
    public static String ArduinoSerial_exception_while_opening_seral_port;
    public static String ArduinoSerial_Flushing_buffer;
    public static String ArduinoSerial_From_Now_Onwards;
    public static String ArduinoSerial_miliseconds;
    public static String ArduinoSerial_reset_dtr_toggle;
    public static String ArduinoSerial_reset_failed;
    public static String ArduinoSerial_Unable_To_Open_Port;
    public static String ArduinoSerial_unable_to_open_serial_port;
    public static String ArduinoSerial_Using_12000bps_touch;
    public static String ArduinoSerial_Using_comport;

    static {
	// initialize resource bundle
	NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
