package io.sloeber.core.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.cdt.serial.ByteSize;
import org.eclipse.cdt.serial.Parity;
import org.eclipse.cdt.serial.SerialPort;
import org.eclipse.cdt.serial.StopBits;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;

@SuppressWarnings("unused")
public class Serial {

	// PApplet parent;

	// properties can be passed in for default values
	// otherwise defaults to 9600 N81

	// these could be made static, which might be a solution
	// for the classloading problem.. because if code ran again,
	// the static class would have an object that could be closed

	SerialPort port = null;
	int rate;
	Parity parity;
	ByteSize databits;

	// read buffer and streams

	StopBits stopbits;
	boolean monitor = false;

	// initial state of RTS&DTR line(ON/OFF)
	// This is needed as Some boards reset when the serial port is opened with
	// RTS and DTR low.
	boolean dtr = true;

	String portName;

	private ServiceRegistration<Serial> fServiceRegistration;

	private List<MessageConsumer> fConsumers;

	public Serial(String iname, int irate) {
		this(iname, irate, 'N', 8, 1.0f, true);
	}

	public Serial(String iname, int irate, boolean dtr) {
		this(iname, irate, 'N', 8, 1.0f, dtr);
	}

	public Serial(String iname, int irate, char iparity, int idatabits, float istopbits, boolean dtr) {
		this.portName = iname;
		this.rate = irate;
		this.dtr = dtr;

		this.parity = Parity.None;
		if (iparity == 'E')
			this.parity = Parity.Even;
		if (iparity == 'O')
			this.parity = Parity.Odd;

		if (idatabits == 8)
			this.databits = ByteSize.B8;
		else if (idatabits == 7)
			this.databits = ByteSize.B7;
		else if (idatabits == 6)
			this.databits = ByteSize.B6;
		else if (idatabits == 5)
			this.databits = ByteSize.B5;

		this.stopbits = StopBits.S1;
		if (istopbits == 2)
			this.stopbits = StopBits.S2;
		connect();

	}

	/**
	 * General error reporting, all correlated here just in case I think of
	 * something slightly more intelligent to do.
	 */
	public static void errorMessage(String where, Throwable e) {
		Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, "Error inside Serial. " + where, e)); //$NON-NLS-1$
	}

	/**
	 * Lists all the output ports.
	 */
	public static List<String> list() {
		try {
			return new ArrayList<>(Arrays.asList(SerialPort.list()));
		} catch (Exception e) {
			Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
					"There is a config problem on your system.\nFor more detail see https://github.com/jantje/arduino-eclipse-plugin/issues/252", //$NON-NLS-1$
					e));
			List<String> ret = new ArrayList<>();
			ret.add("config error:"); //$NON-NLS-1$
			ret.add("see https://github.com/jantje/arduino-eclipse-plugin/issues/252"); //$NON-NLS-1$
			return ret;
		}
	}

	public void addListener(MessageConsumer consumer) {
		if (this.fConsumers == null) {
			this.fConsumers = new ArrayList<>();
		}
		this.fConsumers.add(consumer);
	}

	public void removeListener(MessageConsumer consumer) {
		if (this.fConsumers == null)
			return;
		this.fConsumers.remove(consumer);
	}

	/**
	 * Only connects if the port is not currently connected.
	 * 
	 * @return true if the port was not connected/open and is now connected/open.
	 * @see #IsConnected()
	 */
	public boolean connect() {
		if (IsConnected()) {
			return false;
		}
		int count = 0;
		try {
			this.port = new SerialPort(this.portName);
			this.port.setBaudRateValue(this.rate);
			this.port.setParity(this.parity);
			this.port.setStopBits(this.stopbits);
			this.port.setByteSize(this.databits);
			this.port.open();
			startMonitor();
			return true;
		} catch (IOException e) {
			Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "Error opening serial port " + this.portName, //$NON-NLS-1$
					e));
			this.port = null;
			return false;
		}
	}

	/**
	 * @return true if the system was connected and is now disconnected.
	 */
	public boolean disconnect() {
		if (IsConnected()) {
			try {
				this.port.close();
				this.port = null;
				return true;
			} catch (Exception e) {
				Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, "Serial port close failed", e)); //$NON-NLS-1$
				return false;
			}
		}
		return false;
	}

	public void dispose() {
		disconnect();
		notifyConsumersOfEvent("Disconnect of port " + this.port.getPortName() + " executed"); //$NON-NLS-1$ //$NON-NLS-2$
		if (this.fServiceRegistration != null) {
			this.fServiceRegistration.unregister();
		}
	}

	/**
	 * @return true if the port is connected/open.
	 */
	public boolean IsConnected() {
		return (this.port != null && this.port.isOpen());
	}

	private void notifyConsumersOfData(byte[] message) {
		if (this.fConsumers != null) {
			for (MessageConsumer consumer : this.fConsumers) {
				consumer.message(message);
			}
		}
	}

	private void notifyConsumersOfEvent(String message) {
		if (this.fConsumers != null) {
			for (MessageConsumer consumer : this.fConsumers) {
				consumer.event(message);
			}
		}
	}

	public void registerService() {
		this.fServiceRegistration = FrameworkUtil.getBundle(getClass()).getBundleContext().registerService(Serial.class,
				this, null);
	}

	public boolean reset() {
		try {
			disconnect();
			connect();
		} catch (Exception e) {
			Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "Serial port reset failed", e)); //$NON-NLS-1$
			return false;
		}
		return true;
	}

	private synchronized void checkForData() {
		try {
			if (IsConnected()) {
				int bytesCount = port.getInputStream().available();
				if (bytesCount > 0) {
					byte[] bytes = new byte[bytesCount];
					port.getInputStream().read(bytes, 0, bytesCount);
					notifyConsumersOfData(bytes);
				}
			}
		} catch (Exception e) {
			errorMessage("serialEvent", e); //$NON-NLS-1$
		}
	}

	@Override
	public String toString() {
		return this.portName;
	}

	public void write(byte[] bytes) {
		if (this.port != null) {
			try {
				this.port.getOutputStream().write(bytes);
			} catch (Exception e) {
				errorMessage("write", e); //$NON-NLS-1$
			}
		}
	}

	private void startMonitor() {
		Runnable runner = new Runnable() {

			@Override
			public void run() {
				while (IsConnected()) {
					checkForData();
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
						return;
					}
				}
			}
		};
		Thread thread = new Thread(runner, "Serial port data monitor");
		thread.start();
	}

	/**
	 * This will handle both ints, bytes and chars transparently.
	 */
	public void write(int what) { // will also cover char
		if (this.port != null) {
			try {
				this.port.getOutputStream().write((byte) (what & 0xFF));
			} catch (Exception e) {
				errorMessage("write", e); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Write a String to the output. Note that this doesn't account for Unicode (two
	 * bytes per char), nor will it send UTF8 characters.. It assumes that you mean
	 * to send a byte buffer (most often the case for networking and serial i/o) and
	 * will only use the bottom 8 bits of each char in the string. (Meaning that
	 * internally it uses String.getBytes)
	 * 
	 * If you want to move Unicode data, you can first convert the String to a byte
	 * stream in the representation of your choice (i.e. UTF8 or two-byte Unicode
	 * data), and send it as a byte array.
	 */
	public void write(String what) {
		write(what.getBytes());
	}

	public void write(String what, String lineEnd) {
		notifyConsumersOfEvent(
				System.getProperty("line.separator") + ">>Send to " + this.portName + ": \"" + what + "\"<<" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						+ System.getProperty("line.separator")); //$NON-NLS-1$
		write(what.getBytes());
		if (lineEnd.length() > 0) {
			write(lineEnd.getBytes());
		}
	}

	/**
	 * Pauses the port.
	 * 
	 * @return true if the port was paused, false otherwise
	 */
	public boolean pause() {
		if (IsConnected()) {
			try {
				port.pause();
				return true;
			} catch (IOException e) {
				Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, "Serial port pause failed", e)); //$NON-NLS-1$
			}
		}
		return false;
	}

	/**
	 * Resumes the port.
	 * 
	 * @return true if the port was resumed, false otherwise
	 */
	public boolean resume() {
		if (IsConnected()) {
			try {
				port.resume();
				return true;
			} catch (IOException e) {
				Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, "Serial port resume failed", e)); //$NON-NLS-1$
			}
		}
		return false;
	}
}
