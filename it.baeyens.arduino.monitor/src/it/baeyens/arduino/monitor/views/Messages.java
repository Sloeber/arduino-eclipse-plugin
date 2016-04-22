package it.baeyens.arduino.monitor.views;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
    private static final String BUNDLE_NAME = "it.baeyens.arduino.monitor.views.messages"; //$NON-NLS-1$
    public static String openSerialDialogBoxSelectTheBautRate;
    public static String openSerialDialogBoxSerialPortToConnectTo;
    public static String openSerialDialogBoxDtr;
    public static String scopeListenerBufferOverflow;
    public static String scopeViewChannel;
    public static String scopeViewConnectedTo;
    public static String scopeViewDisconnectedFrom;
    public static String scopeViewSerialMessageMissed;
    public static String serialListenerErrorInputPart1;
    public static String serialListenerErrorInputPart2;
    public static String serialListenerScopeSkippingData;
    public static String serialMonitorAddConnectionToSeralMonitor;
    public static String serialMonitorAt;
    public static String serialMonitorScrollLock;
    public static String serialMonitorClear;
    public static String serialMonitorConnectedTo;
    public static String serialMonitorConnectedtTo;
    public static String serialMonitorDisconnectedFrom;
    public static String serialMonitorFilterScope;
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
