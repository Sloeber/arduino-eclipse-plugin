package io.sloeber.monitor;

import io.sloeber.monitor.views.SerialMonitor;

public class SerialConnection {
    private SerialConnection() {}

    public static void add(String comPort, int baudrate) {
    SerialMonitor.getSerialMonitor().connectSerial(comPort, baudrate);
    }

    public static void remove(String comPort) {
    SerialMonitor.getSerialMonitor().disConnectSerialPort(comPort);
    }

}
