package it.baeyens.arduino.monitor.views;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "it.baeyens.arduino.monitor.views.messages"; //$NON-NLS-1$
    public static String OpenSerialDialogBox_Select_the_baut_rate;
    public static String OpenSerialDialogBox_Serial_port_to_connect_to;
    public static String ScopeListener_buffer_overflow;
    public static String ScopeView_channel;
    public static String ScopeView_connected_to;
    public static String ScopeView_disconnected_from;
    public static String ScopeView_serial_message_missed;
    public static String SerialListener_error_input_part_1;
    public static String SerialListener_error_input_part_2;
    public static String SerialListener_scope_skipping_data;
    public static String SerialMonitor_Add_connection_to_seral_monitor;
    public static String SerialMonitor_at;
    public static String SerialMonitor_autoscrol;
    public static String SerialMonitor_clear;
    public static String SerialMonitor_connected_to;
    public static String SerialMonitor_connectedt_to;
    public static String SerialMonitor_disconnected_from;
    public static String SerialMonitor_filter_scope;
    public static String SerialMonitor_no_input;
    public static String SerialMonitor_no_more_serial_ports_supported;
    public static String SerialMonitor_remove_serial_port_from_monitor;
    public static String SerialMonitor_reset;
    public static String SerialMonitor_send;

    static {
	// initialize resource bundle
	NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
