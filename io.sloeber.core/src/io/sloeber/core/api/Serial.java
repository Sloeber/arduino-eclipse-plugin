/* -*- mode: jde; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
 PSerial - class for serial port goodness
 Part of the Processing project - http://processing.org

 Copyright (c) 2004 Ben Fry & Casey Reas

 This library is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 2.1 of the License, or (at your option) any later version.

 This library is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General
 Public License along with this library; if not, write to the
 Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 Boston, MA  02111-1307  USA
 */

/*
 * For documentation on jssc see  http://java-simple-serial-connector.googlecode.com/svn/trunk/additional_content/javadoc/0.8/index.html
 */

package io.sloeber.core.api;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;
@SuppressWarnings("unused")
public class Serial implements SerialPortEventListener {

    // PApplet parent;

    // properties can be passed in for default values
    // otherwise defaults to 9600 N81

    // these could be made static, which might be a solution
    // for the classloading problem.. because if code ran again,
    // the static class would have an object that could be closed

    SerialPort port = null;
    int rate;
    int parity;
    int databits;

    // read buffer and streams

    int stopbits;
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

	this.parity = SerialPort.PARITY_NONE;
	if (iparity == 'E')
	    this.parity = SerialPort.PARITY_EVEN;
	if (iparity == 'O')
	    this.parity = SerialPort.PARITY_ODD;

	this.databits = idatabits;

	this.stopbits = SerialPort.STOPBITS_1;
	if (istopbits == 1.5f)
	    this.stopbits = SerialPort.STOPBITS_1_5;
	if (istopbits == 2)
	    this.stopbits = SerialPort.STOPBITS_2;
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
     * If this just hangs and never completes on Windows, it may be because the
     * DLL doesn't have its exec bit set. Why the hell that'd be the case, who
     * knows.
     */
    public static List<String> list() {
	try {
	    String[] portNames;
	    String os = System.getProperty("os.name").toLowerCase(); //$NON-NLS-1$
	    if (os.indexOf("mac") >= 0) { //$NON-NLS-1$
		portNames = SerialPortList.getPortNames("/dev/", Pattern.compile("^cu\\..*(UART|serial|usb).*")); //$NON-NLS-1$ //$NON-NLS-2$
	    } else {
		portNames = SerialPortList.getPortNames();
	    }
	    return new ArrayList<>(Arrays.asList(portNames));
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

    public void connect() {
	connect(1);
    }

    public void connect(int maxTries) {
	if (this.port == null) {
	    int count = 0;
	    while (true) {
		try {
		    this.port = new SerialPort(this.portName);
		    this.port.openPort();
		    this.port.setParams(this.rate, this.databits, this.stopbits, this.parity, this.dtr, this.dtr);

		    int eventMask = SerialPort.MASK_RXCHAR | SerialPort.MASK_BREAK;
		    this.port.addEventListener(this, eventMask);
		    return;
		} catch (SerialPortException e) {
		    // handle exception
		    if (++count == maxTries) {
			if (SerialPortException.TYPE_PORT_BUSY.equals(e.getExceptionType())) {
			    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
				    "Serial port " + this.portName //$NON-NLS-1$
					    + " already in use. Try quiting any programs that may be using it", //$NON-NLS-1$
				    e));
			} else if (SerialPortException.TYPE_PORT_NOT_FOUND.equals(e.getExceptionType())) {
			    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID, "Serial port " //$NON-NLS-1$
				    + this.portName
				    + " not found. Did you select the right one from the project properties -> Arduino -> Arduino?", //$NON-NLS-1$
				    e));
			} else {
			    Common.log(new Status(IStatus.ERROR, Const.CORE_PLUGIN_ID,
				    "Error opening serial port " + this.portName, e)); //$NON-NLS-1$
			}
			return;
		    }
		    try {
			Thread.sleep(200);
		    } catch (InterruptedException e1) {
			Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, "Sleep failed", e1)); //$NON-NLS-1$
		    }
		}
		// If an exception was thrown, delete port variable
		this.port = null;
	    }
	}
    }

	public void disconnect() {
		if (this.port != null) {

			if (this.port.isOpened()) {
				try {
					this.port.closePort();
				} catch (SerialPortException e) {
					// e.printStackTrace();
				}
			}
			this.port = null;
		}
	}

    public void dispose() {
	notifyConsumersOfEvent("Disconnect of port " + this.port.getPortName() + " executed"); //$NON-NLS-1$ //$NON-NLS-2$
	disconnect();

	if (this.fServiceRegistration != null) {
	    this.fServiceRegistration.unregister();
	}
    }

    public boolean IsConnected() {
	return this.port != null && this.port.isOpened();
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

    public void reset() {
	setDTR(false);
	setRTS(false);

	try {
	    Thread.sleep(100);
	} catch (InterruptedException e) {// JABA is not going to add code
	}
	setDTR(true);
	setRTS(true);

    }

    @Override
    public synchronized void serialEvent(SerialPortEvent serialEvent) {
	switch (serialEvent.getEventType()) {
	case SerialPortEvent.RXCHAR:
	    int bytesCount = serialEvent.getEventValue();

	    if (IsConnected() && bytesCount > 0) {
		try {
		    notifyConsumersOfData(this.port.readBytes(bytesCount));
		} catch (SerialPortException e) {
		    errorMessage("serialEvent", e); //$NON-NLS-1$
		}
	    }
	    break;
	case SerialPortEvent.BREAK:
	    errorMessage("Break detected", new Exception()); //$NON-NLS-1$
	    break;
	default:
	    break;
	}
    }

    public void setDTR(boolean state) {
	if (IsConnected()) {
	    try {
		this.port.setDTR(state);
	    } catch (SerialPortException e) {
		e.printStackTrace();
	    }
	}
    }

    public void setRTS(boolean state) {
	if (IsConnected()) {
	    try {
		this.port.setRTS(state);
	    } catch (SerialPortException e) {
		e.printStackTrace();
	    }
	}
    }

    public void setup() {// JABA is not going to add code
    }

    // needed to fill viewers in jfases
    @Override
    public String toString() {
	return this.portName;
    }

    public void write(byte[] bytes) {
	if (this.port != null) {
	    try {
		this.port.writeBytes(bytes);
	    } catch (SerialPortException e) {
		errorMessage("write", e); //$NON-NLS-1$
	    }
	}
    }

    /**
     * This will handle both ints, bytes and chars transparently.
     */
    public void write(int what) { // will also cover char
	if (this.port != null) {
	    try {
		this.port.writeByte((byte) (what & 0xFF));
	    } catch (SerialPortException e) {
		errorMessage("write", e); //$NON-NLS-1$
	    }
	}
    }

    /**
     * Write a String to the output. Note that this doesn't account for Unicode
     * (two bytes per char), nor will it send UTF8 characters.. It assumes that
     * you mean to send a byte buffer (most often the case for networking and
     * serial i/o) and will only use the bottom 8 bits of each char in the
     * string. (Meaning that internally it uses String.getBytes)
     * 
     * If you want to move Unicode data, you can first convert the String to a
     * byte stream in the representation of your choice (i.e. UTF8 or two-byte
     * Unicode data), and send it as a byte array.
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
}
