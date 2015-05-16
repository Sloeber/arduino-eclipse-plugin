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

package it.baeyens.arduino.arduino;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortList;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

public class Serial implements SerialPortEventListener {

    // PApplet parent;

    // properties can be passed in for default values
    // otherwise defaults to 9600 N81

    // these could be made static, which might be a solution
    // for the classloading problem.. because if code ran again,
    // the static class would have an object that could be closed

    /**
     * General error reporting, all correlated here just in case I think of something slightly more intelligent to do.
     */
    static public void errorMessage(String where, Throwable e) {
	Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Error inside Serial. " + where, e));

    }

    /**
     * If this just hangs and never completes on Windows, it may be because the DLL doesn't have its exec bit set. Why the hell that'd be the case,
     * who knows.
     */
    public static Vector<String> list() {
	try {
	    String[] portNames = SerialPortList.getPortNames();
	    return new Vector<String>(Arrays.asList(portNames));
	} catch (Error e) {
	    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
		    "There is a config problem on your system.\nFor more detail see https://github.com/jantje/arduino-eclipse-plugin/issues/252", e));
	    Vector<String> ret = new Vector<String>();
	    ret.add("config error:");
	    ret.add("see https://github.com/jantje/arduino-eclipse-plugin/issues/252");
	    return ret;
	}
    }

    SerialPort port = null;
    int rate;
    int parity;
    int databits;

    // read buffer and streams

    int stopbits;
    boolean monitor = false;

    byte buffer[] = new byte[32768];
    int bufferIndex;

    int bufferLast;

    String PortName;

    private ServiceRegistration<Serial> fServiceRegistration;

    private List<MessageConsumer> fConsumers;

    public Serial(String iname, int irate) {
	this(iname, irate, 'N', 8, 1.0f);
    }

    public Serial(String iname, int irate, char iparity, int idatabits, float istopbits) {
	PortName = iname;
	this.rate = irate;

	parity = SerialPort.PARITY_NONE;
	if (iparity == 'E')
	    parity = SerialPort.PARITY_EVEN;
	if (iparity == 'O')
	    parity = SerialPort.PARITY_ODD;

	this.databits = idatabits;

	stopbits = SerialPort.STOPBITS_1;
	if (istopbits == 1.5f)
	    stopbits = SerialPort.STOPBITS_1_5;
	if (istopbits == 2)
	    stopbits = SerialPort.STOPBITS_2;
	connect();

    }

    public void addListener(MessageConsumer consumer) {
	if (fConsumers == null) {
	    fConsumers = new ArrayList<MessageConsumer>();
	}
	fConsumers.add(consumer);
    }

    public void removeListener(MessageConsumer consumer) {
	if (fConsumers == null)
	    return;
	fConsumers.remove(consumer);
    }

    /**
     * Returns the number of bytes that have been read from serial and are waiting to be dealt with by the user.
     */
    public int available() {
	return (bufferLast - bufferIndex);
    }

    /**
     * Ignore all the bytes read so far and empty the buffer.
     */
    public void clear() {
	bufferLast = 0;
	bufferIndex = 0;
    }

    public void connect() {
	connect(1);
    }

    public void connect(int maxTries) {
	if (port == null) {
	    int count = 0;
	    while (true) {
		try {
		    port = new SerialPort(PortName);
		    port.openPort();
		    port.setParams(rate, databits, stopbits, parity);

		    int eventMask = SerialPort.MASK_RXCHAR | SerialPort.MASK_BREAK;
		    port.addEventListener(this, eventMask);
		    return;
		} catch (SerialPortException e) {
		    // handle exception
		    if (++count == maxTries) {
			if (SerialPortException.TYPE_PORT_BUSY.equals(e.getExceptionType())) {
			    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Serial port " + PortName
				    + " already in use. Try quiting any programs that may be using it", e));
			} else if (SerialPortException.TYPE_PORT_NOT_FOUND.equals(e.getExceptionType())) {
			    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Serial port " + PortName
				    + " not found. Did you select the right one from the project properties -> Arduino -> Arduino?", null));
			} else {
			    Common.log(new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Error opening serial port " + PortName, e));
			}
		    } else {
			try {
			    Thread.sleep(200);
			} catch (InterruptedException e1) {
			    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Sleep failed", e1));
			}
		    }
		}
		// If an exception was thrown, delete port variable
		port = null;
	    }
	}
    }

    public void disconnect() {
	if (port != null) {
	    try {
		port.closePort();
	    } catch (SerialPortException e) {
		e.printStackTrace();
	    }
	    port = null;
	}
    }

    public void dispose() {
	notifyConsumersOfEvent("Disconnect of port " + port.getPortName() + " executed");
	disconnect();

	if (fServiceRegistration != null) {
	    fServiceRegistration.unregister();
	}
    }

    public boolean IsConnected() {
	return (port != null && port.isOpened());
    }

    private void notifyConsumersOfData(byte[] message) {
	if (fConsumers != null) {
	    for (MessageConsumer consumer : fConsumers) {
		consumer.message(message);
	    }
	}
    }

    private void notifyConsumersOfEvent(String message) {
	if (fConsumers != null) {
	    for (MessageConsumer consumer : fConsumers) {
		consumer.event(message);
	    }
	}
    }

    /**
     * Returns a number between 0 and 255 for the next byte that's waiting in the buffer. Returns -1 if there was no byte (although the user should
     * first check available() to see if things are ready to avoid this)
     */
    public int read() {
	if (bufferIndex == bufferLast)
	    return -1;

	synchronized (buffer) {
	    int outgoing = buffer[bufferIndex++] & 0xff;
	    if (bufferIndex == bufferLast) { // rewind
		bufferIndex = 0;
		bufferLast = 0;
	    }
	    return outgoing;
	}
    }

    /**
     * Return a byte array of anything that's in the serial buffer. Not particularly memory/speed efficient, because it creates a byte array on each
     * read, but it's easier to use than readBytes(byte b[]) (see below).
     */
    public byte[] readBytes() {
	if (bufferIndex == bufferLast)
	    return null;

	synchronized (buffer) {
	    int length = bufferLast - bufferIndex;
	    byte outgoing[] = new byte[length];
	    System.arraycopy(buffer, bufferIndex, outgoing, 0, length);

	    bufferIndex = 0; // rewind
	    bufferLast = 0;
	    return outgoing;
	}
    }

    /**
     * Grab whatever is in the serial buffer, and stuff it into a byte buffer passed in by the user. This is more memory/time efficient than
     * readBytes() returning a byte[] array.
     * 
     * Returns an int for how many bytes were read. If more bytes are available than can fit into the byte array, only those that will fit are read.
     */
    public int readBytes(byte outgoing[]) {
	if (bufferIndex == bufferLast)
	    return 0;

	synchronized (buffer) {
	    int length = bufferLast - bufferIndex;
	    if (length > outgoing.length)
		length = outgoing.length;
	    System.arraycopy(buffer, bufferIndex, outgoing, 0, length);

	    bufferIndex += length;
	    if (bufferIndex == bufferLast) {
		bufferIndex = 0; // rewind
		bufferLast = 0;
	    }
	    return length;
	}
    }

    /**
     * Reads from the serial port into a buffer of bytes up to and including a particular character. If the character isn't in the serial buffer, then
     * 'null' is returned.
     */
    public byte[] readBytesUntil(int interesting) {
	if (bufferIndex == bufferLast)
	    return null;
	byte what = (byte) interesting;

	synchronized (buffer) {
	    int found = -1;
	    for (int k = bufferIndex; k < bufferLast; k++) {
		if (buffer[k] == what) {
		    found = k;
		    break;
		}
	    }
	    if (found == -1)
		return null;

	    int length = found - bufferIndex + 1;
	    byte outgoing[] = new byte[length];
	    System.arraycopy(buffer, bufferIndex, outgoing, 0, length);

	    bufferIndex = 0; // rewind
	    bufferLast = 0;
	    return outgoing;
	}
    }

    /**
     * Reads from the serial port into a buffer of bytes until a particular character. If the character isn't in the serial buffer, then 'null' is
     * returned.
     * 
     * If outgoing[] is not big enough, then -1 is returned, and an error message is printed on the console. If nothing is in the buffer, zero is
     * returned. If 'interesting' byte is not in the buffer, then 0 is returned.
     */
    public int readBytesUntil(int interesting, byte outgoing[]) {
	if (bufferIndex == bufferLast)
	    return 0;
	byte what = (byte) interesting;

	synchronized (buffer) {
	    int found = -1;
	    for (int k = bufferIndex; k < bufferLast; k++) {
		if (buffer[k] == what) {
		    found = k;
		    break;
		}
	    }
	    if (found == -1)
		return 0;

	    int length = found - bufferIndex + 1;
	    if (length > outgoing.length) {
		Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "readBytesUntil() byte buffer is too small for the " + length
			+ " bytes up to and including char " + interesting, null));
		return -1;
	    }
	    // byte outgoing[] = new byte[length];
	    System.arraycopy(buffer, bufferIndex, outgoing, 0, length);

	    bufferIndex += length;
	    if (bufferIndex == bufferLast) {
		bufferIndex = 0; // rewind
		bufferLast = 0;
	    }
	    return length;
	}
    }

    /**
     * Returns the next byte in the buffer as a char. Returns -1, or 0xffff, if nothing is there.
     */
    public char readChar() {
	if (bufferIndex == bufferLast)
	    return (char) (-1);
	return (char) read();
    }

    /**
     * Return whatever has been read from the serial port so far as a String. It assumes that the incoming characters are ASCII.
     * 
     * If you want to move Unicode data, you can first convert the String to a byte stream in the representation of your choice (i.e. UTF8 or two-byte
     * Unicode data), and send it as a byte array.
     */
    public String readString() {
	if (bufferIndex == bufferLast)
	    return null;
	return new String(readBytes());
    }

    /**
     * Combination of readBytesUntil and readString. See caveats in each function. Returns null if it still hasn't found what you're looking for.
     * 
     * If you want to move Unicode data, you can first convert the String to a byte stream in the representation of your choice (i.e. UTF8 or two-byte
     * Unicode data), and send it as a byte array.
     */
    public String readStringUntil(int interesting) {
	byte b[] = readBytesUntil(interesting);
	if (b == null)
	    return null;
	return new String(b);
    }

    public void registerService() {
	fServiceRegistration = FrameworkUtil.getBundle(getClass()).getBundleContext().registerService(Serial.class, this, null);
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
    synchronized public void serialEvent(SerialPortEvent serialEvent) {
	switch (serialEvent.getEventType()) {
	case SerialPortEvent.RXCHAR:
	    int bytesCount = serialEvent.getEventValue();

	    if (IsConnected() && bytesCount > 0) {
		synchronized (buffer) {
		    try {
			byte[] readBytes = port.readBytes(bytesCount);

			// Check there is enough buffer to store new bytes
			if (readBytes.length > buffer.length) {
			    int newLength = buffer.length;
			    while (readBytes.length > newLength) {
				newLength *= 2;
			    }

			    byte temp[] = new byte[newLength];
			    System.arraycopy(buffer, 0, temp, 0, buffer.length);
			    buffer = temp;
			}

			// Append read bytes to buffer tail
			System.arraycopy(readBytes, 0, buffer, bufferLast, readBytes.length);
			if (monitor == true) {
			    System.out.print(new String(readBytes));
			}
			bufferLast += readBytes.length;

			notifyConsumersOfData(readBytes());
		    } catch (SerialPortException e) {
			errorMessage("serialEvent", e);
		    }
		}
	    }

	    break;
	case SerialPortEvent.BREAK:
	    errorMessage("Break detected", new Exception());
	    break;
	default:
	    break;
	}
    }

    public void setDTR(boolean state) {
	if (IsConnected()) {
	    try {
		port.setDTR(state);
	    } catch (SerialPortException e) {
		e.printStackTrace();
	    }
	}
    }

    public void setRTS(boolean state) {
	if (IsConnected()) {
	    try {
		port.setRTS(state);
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
	return PortName;
    }

    public void write(byte bytes[]) {
	if (port != null) {
	    try {
		port.writeBytes(bytes);
	    } catch (SerialPortException e) {
		errorMessage("write", e);
	    }
	}
    }

    /**
     * This will handle both ints, bytes and chars transparently.
     */
    public void write(int what) { // will also cover char
	if (port != null) {
	    try {
		port.writeByte((byte) (what & 0xFF));
	    } catch (SerialPortException e) {
		errorMessage("write", e);
	    }
	}
    }

    /**
     * Write a String to the output. Note that this doesn't account for Unicode (two bytes per char), nor will it send UTF8 characters.. It assumes
     * that you mean to send a byte buffer (most often the case for networking and serial i/o) and will only use the bottom 8 bits of each char in the
     * string. (Meaning that internally it uses String.getBytes)
     * 
     * If you want to move Unicode data, you can first convert the String to a byte stream in the representation of your choice (i.e. UTF8 or two-byte
     * Unicode data), and send it as a byte array.
     */
    public void write(String what) {
	write(what.getBytes());
    }

    public void write(String what, String LineEnd) {
	notifyConsumersOfEvent(System.getProperty("line.separator") + ">>Send to " + PortName + ": \"" + what + "\"<<"
		+ System.getProperty("line.separator"));
	write(what.getBytes());
	if (LineEnd.length() > 0) {
	    write(LineEnd.getBytes());
	}
    }
}
