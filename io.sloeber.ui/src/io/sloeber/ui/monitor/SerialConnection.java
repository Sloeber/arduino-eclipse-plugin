package io.sloeber.ui.monitor;

import java.util.List;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import io.sloeber.ui.monitor.views.SerialMonitor;

public class SerialConnection {
	private SerialConnection() {
	}

	/**
	 * add a serial connection to the serial monitor. Must be run from the ui
	 * thread
	 * 
	 * @param comPort
	 *            the comport name to connect to
	 * @param baudrate
	 *            the baud rate to use to connect
	 */
	public static void add(String comPort, int baudrate) {
		SerialMonitor.getSerialMonitor().connectSerial(comPort, baudrate);
	}

	/**
	 * removes a serial connection from the serial monitor Must be run from the
	 * ui thread
	 * 
	 * @param comPort
	 *            the comport name that needs to be removed
	 */
	public static void remove(String comPort) {
		SerialMonitor.getSerialMonitor().disConnectSerialPort(comPort);
	}

	/**
	 * get the full content of the serial monitor test window. Line by line Must
	 * be run from the ui thread
	 * 
	 * @return a string for each content line
	 */
	public static List<String> getContent() {
		return SerialMonitor.getMonitorContent();
	}

	/**
	 * Clear all the content of the serial monitor Must be run from the ui
	 * thread
	 */
	public static void clearMonitor() {

		SerialMonitor.clearMonitor();

	}
	/**
	 * conveniance class giving the display to get tothe ui thread which is not
	 * so easy in teste environments for jantje
	 * 
	 * @return
	 */
	public static Display getDisplay() {
		return PlatformUI.getWorkbench().getDisplay();
	}

	/**
	 * Show the serial monitor
	 */
	public static void show() {
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("io.sloeber.ui.monitor.views.SerialMonitor"); //$NON-NLS-1$
		} catch (PartInitException e) {
			// ignoring all errors
			e.printStackTrace();
		}
		
	}

}
