package io.sloeber.monitor.views;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "io.sloeber.monitor.views.messages"; //$NON-NLS-1$
    public static String openSerialDialogBoxSelectTheBautRate;
    public static String openSerialDialogBoxSerialPortToConnectTo;
    public static String openSerialDialogBoxDtr;
    public static String plotterListenerBufferOverflow;
    public static String plotterViewChannel;
    public static String plotterViewConnectedTo;
    public static String plotterViewDisconnectedFrom;
    public static String plotterViewSerialMessageMissed;
    public static String serialListenerErrorInputPart1;
    public static String serialListenerErrorInputPart2;
    public static String serialListenerPlotterSkippingData;
    public static String serialMonitorAddConnectionToSeralMonitor;
    public static String serialMonitorAt;
    public static String serialMonitorScrollLock;
    public static String serialMonitorClear;
    public static String serialMonitorConnectedTo;
    public static String serialMonitorDisconnectedFrom;
    public static String serialMonitorFilterPloter;
    public static String serialMonitorNoInput;
    public static String serialMonitorNoMoreSerialPortsSupported;
    public static String serialMonitorRemoveSerialPortFromMonitor;
    public static String serialMonitorReset;
    public static String serialMonitorSend;

    static {
	// initialize resource bundle
	NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {

    }
}
