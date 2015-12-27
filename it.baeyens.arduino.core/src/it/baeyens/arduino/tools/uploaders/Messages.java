package it.baeyens.arduino.tools.uploaders;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "it.baeyens.arduino.tools.uploaders.messages"; //$NON-NLS-1$
    public static String Upload_arduino;
    public static String Upload_auth_cancel;
    public static String Upload_auth_fail;
    public static String Upload_connection_refused;
    public static String Upload_console;
    public static String Upload_Done;
    public static String Upload_error_auth_fail;
    public static String Upload_Error_com_port;
    public static String Upload_Error_serial_monitor_restart;
    public static String Upload_error_connection_refused;
    public static String Upload_error_network;
    public static String Upload_failed_upload;
    public static String Upload_generic;
    public static String Upload_login_credentials_missing;
    public static String Upload_no_arduino_sketch;
    public static String Upload_PluginStartInitiator;
    public static String Upload_Project_nature_unaccesible;
    public static String Upload_sending_sketch;
    public static String Upload_sketch_on_yun;
    public static String Upload_ssh;
    public static String Upload_starting;
    public static String Upload_to;
    public static String Upload_uploading;

    static {
	// initialize resource bundle
	NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
