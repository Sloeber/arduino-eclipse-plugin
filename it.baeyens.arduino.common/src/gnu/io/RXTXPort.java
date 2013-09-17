/*-------------------------------------------------------------------------
|   rxtx is a native interface to serial ports in java.
|   Copyright 1997-2006 by Trent Jarvi taj@www.linux.org.uk.
|
|   This library is free software; you can redistribute it and/or
|   modify it under the terms of the GNU Library General Public
|   License as published by the Free Software Foundation; either
|   version 2 of the License, or (at your option) any later version.
|
|   This library is distributed in the hope that it will be useful,
|   but WITHOUT ANY WARRANTY; without even the implied warranty of
|   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
|   Library General Public License for more details.
|
|   You should have received a copy of the GNU Library General Public
|   License along with this library; if not, write to the Free
|   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
--------------------------------------------------------------------------*/
package gnu.io;

import it.baeyens.arduino.common.Common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

/**
 * An extension of gnu.io.SerialPort
 * 
 * @see gnu.io.SerialPort
 */

final public class RXTXPort extends SerialPort {
    /*
     * I had a report that some JRE's complain when MonitorThread tries to access private variables
     */

    protected final static boolean debug = false;
    protected final static boolean debug_read = false;
    protected final static boolean debug_read_results = false;
    protected final static boolean debug_write = false;
    protected final static boolean debug_events = false;
    protected final static boolean debug_verbose = false;

    static Zystem z;

    static {
	try {
	    z = new Zystem();
	} catch (Exception e) {// JABA is not going to write this code
	}

	if (debug)
	    z.reportln("RXTXPort {}");
	// System.loadLibrary( Common.SerialDllName() );
	// System.load( Common.GetSerialFullDllName() );
	Common.LoadRXTX();
	Initialize();
    }

    /** Initialize the native library */
    private native static void Initialize();

    boolean MonitorThreadAlive = false;

    /**
     * Open the named port
     * 
     * @param name
     *            the name of the device to open
     * @throws PortInUseException
     * @see gnu.io.SerialPort
     */
    public RXTXPort(String name) throws PortInUseException {
	if (debug)
	    z.reportln("RXTXPort:RXTXPort(" + name + ") called");
	/*
	 * commapi/javadocs/API_users_guide.html specifies that whenever an application tries to open a port in use by another application the
	 * PortInUseException will be thrown
	 * 
	 * I know some didnt like it this way but I'm not sure how to avoid it. We will just be writing to a bogus fd if we catch the exeption
	 * 
	 * Trent
	 */
	// try {
	fd = open(name);
	this.name = name;

	MonitorThreadLock = true;
	monThread = new MonitorThread();
	monThread.start();
	waitForTheNativeCodeSilly();
	MonitorThreadAlive = true;
	// } catch ( PortInUseException e ){}
	timeout = -1; /* default disabled timeout */
	if (debug)
	    z.reportln("RXTXPort:RXTXPort(" + name + ") returns with fd = " + fd);
    }

    @SuppressWarnings("hiding")
    private native synchronized int open(String name) throws PortInUseException;

    /* dont close the file while accessing the fd */
    int IOLocked = 0;

    /** File descriptor */
    int fd = 0;

    /**
     * a pointer to the event info structure used to share information between threads so write threads can send output buffer empty from a pthread if
     * need be.
     * 
     * long for 64 bit pointers.
     */
    long eis = 0;
    /** pid for lock files */
    int pid = 0;

    /** DSR flag **/
    static boolean dsrFlag = false;

    /** Output stream */
    private final SerialOutputStream out = new SerialOutputStream();

    /**
     * get the OutputStream
     * 
     * @return OutputStream
     */
    @Override
    public OutputStream getOutputStream() {
	if (debug)
	    z.reportln("RXTXPort:getOutputStream() called and returning");
	return out;
    }

    /** Input stream */
    private final SerialInputStream in = new SerialInputStream();

    /**
     * get the InputStream
     * 
     * @return InputStream
     * @see java.io.InputStream
     */
    @Override
    public InputStream getInputStream() {
	if (debug)
	    z.reportln("RXTXPort:getInputStream() called and returning");
	return in;
    }

    /**
     * Set the SerialPort parameters 1.5 stop bits requires 5 databits
     * 
     * @param b
     *            baudrate
     * @param d
     *            databits
     * @param s
     *            stopbits
     * @param p
     *            parity
     * @throws UnsupportedCommOperationException
     * @see gnu.io.UnsupportedCommOperationException
     * 
     *      If speed is not a predifined speed it is assumed to be the actual speed desired.
     */
    @SuppressWarnings("hiding")
    private native int nativeGetParity(int fd);

    @SuppressWarnings("hiding")
    private native int nativeGetFlowControlMode(int fd);

    @Override
    public synchronized void setSerialPortParams(int b, int d, int s, int p) throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:setSerialPortParams(" + b + " " + d + " " + s + " " + p + ") called");
	if (nativeSetSerialPortParams(b, d, s, p))
	    throw new UnsupportedCommOperationException("Invalid Parameter");
	speed = b;
	if (s == STOPBITS_1_5)
	    dataBits = DATABITS_5;
	else
	    dataBits = d;
	stopBits = s;
	parity = p;
	z.reportln("RXTXPort:setSerialPortParams(" + b + " " + d + " " + s + " " + p + ") returning");
    }

    /**
     * Set the native serial port parameters If speed is not a predifined speed it is assumed to be the actual speed desired.
     */
    @SuppressWarnings("hiding")
    private native boolean nativeSetSerialPortParams(int speed, int dataBits, int stopBits, int parity) throws UnsupportedCommOperationException;

    /** Line speed in bits-per-second */
    int speed = 9600;

    /**
     * @return int representing the baudrate This will not behave as expected with custom speeds
     */
    @Override
    public int getBaudRate() {
	if (debug)
	    z.reportln("RXTXPort:getBaudRate() called and returning " + speed);
	return speed;
    }

    /** Data bits port parameter */
    private int dataBits = DATABITS_8;

    /**
     * @return int representing the databits
     */
    @Override
    public int getDataBits() {
	if (debug)
	    z.reportln("RXTXPort:getDataBits() called and returning " + dataBits);
	return dataBits;
    }

    /** Stop bits port parameter */
    private int stopBits = SerialPort.STOPBITS_1;

    /**
     * @return int representing the stopbits
     */
    @Override
    public int getStopBits() {
	if (debug)
	    z.reportln("RXTXPort:getStopBits() called and returning " + stopBits);
	return stopBits;
    }

    /** Parity port parameter */
    private int parity = SerialPort.PARITY_NONE;

    /**
     * @return int representing the parity
     */
    @Override
    public int getParity() {
	if (debug)
	    z.reportln("RXTXPort:getParity() called and returning " + parity);
	return parity;
    }

    /** Flow control */
    private int flowmode = SerialPort.FLOWCONTROL_NONE;

    /**
     * @param flowcontrol
     *            FLOWCONTROL_NONE is default
     * @see gnu.io.SerialPort#FLOWCONTROL_NONE
     */
    @Override
    public void setFlowControlMode(int flowcontrol) {
	if (debug)
	    z.reportln("RXTXPort:setFlowControlMode( " + flowcontrol + " ) called");
	if (monThreadisInterrupted) {
	    if (debug_events)
		z.reportln("RXTXPort:setFlowControlMode MonThread is Interrupeted returning");
	    return;
	}
	try {
	    setflowcontrol(flowcontrol);
	} catch (IOException e) {
	    e.printStackTrace();
	    return;
	}
	flowmode = flowcontrol;
	if (debug)
	    z.reportln("RXTXPort:setFlowControlMode( " + flowcontrol + " ) returning");
    }

    /**
     * @return int representing the flowmode
     */
    @Override
    public int getFlowControlMode() {
	if (debug)
	    z.reportln("RXTXPort:getFlowControlMode() returning " + flowmode);
	return flowmode;
    }

    native void setflowcontrol(int flowcontrol) throws IOException;

    /*
     * linux/drivers/char/n_hdlc.c? FIX ME taj@www.linux.org.uk
     */
    /**
     * Receive framing control
     * 
     * @param f
     *            framming
     * @throws UnsupportedCommOperationException
     */
    @Override
    public void enableReceiveFraming(int f) throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:enableReceiveFramming() throwing exception");
	throw new UnsupportedCommOperationException("Not supported");
    }

    /** 
	*/
    @Override
    public void disableReceiveFraming() {
	if (debug)
	    z.reportln("RXTXPort:disableReceiveFramming() called and returning (noop)");
    }

    /**
     * @returns true if framing is enabled
     */
    @Override
    public boolean isReceiveFramingEnabled() {
	if (debug)
	    z.reportln("RXTXPort:isReceiveFrammingEnabled() called and returning " + false);
	return false;
    }

    /**
     * @return int representing the framing byte
     */
    @Override
    public int getReceiveFramingByte() {
	if (debug)
	    z.reportln("RXTXPort:getReceiveFrammingByte() called and returning " + 0);
	return 0;
    }

    /** Receive timeout control */
    private int timeout;

    /**
     * @return int the timeout
     */
    public native int NativegetReceiveTimeout();

    /**
     * @return bloolean true if recieve timeout is enabled
     */
    private native boolean NativeisReceiveTimeoutEnabled();

    /**
     * @param time
     * @param threshold
     * @param InputBuffer
     */
    @SuppressWarnings("hiding")
    private native void NativeEnableReceiveTimeoutThreshold(int time, int threshold, int InputBuffer);

    /** 
	*/
    @Override
    public void disableReceiveTimeout() {
	if (debug)
	    z.reportln("RXTXPort:disableReceiveTimeout() called");
	timeout = -1;
	NativeEnableReceiveTimeoutThreshold(timeout, threshold, InputBuffer);
	if (debug)
	    z.reportln("RXTXPort:disableReceiveTimeout() returning");
    }

    /**
     * @param time
     */
    @Override
    public void enableReceiveTimeout(int time) {
	if (debug)
	    z.reportln("RXTXPort:enableReceiveTimeout() called");
	if (time >= 0) {
	    timeout = time;
	    NativeEnableReceiveTimeoutThreshold(time, threshold, InputBuffer);
	} else {
	    throw new IllegalArgumentException("Unexpected negative timeout value");
	}
	if (debug)
	    z.reportln("RXTXPort:enableReceiveTimeout() returning");
    }

    /**
     * @return boolean true if recieve timeout is enabled
     */
    @Override
    public boolean isReceiveTimeoutEnabled() {
	if (debug)
	    z.reportln("RXTXPort:isReceiveTimeoutEnabled() called and returning " + NativeisReceiveTimeoutEnabled());
	return (NativeisReceiveTimeoutEnabled());
    }

    /**
     * @return int the timeout
     */
    @Override
    public int getReceiveTimeout() {
	if (debug)
	    z.reportln("RXTXPort:getReceiveTimeout() called and returning " + NativegetReceiveTimeout());
	return (NativegetReceiveTimeout());
    }

    /** Receive threshold control */

    int threshold = 0;

    /**
     * @param thresh
     *            threshold
     */
    @Override
    public void enableReceiveThreshold(int thresh) {
	if (debug)
	    z.reportln("RXTXPort:enableReceiveThreshold( " + thresh + " ) called");
	if (thresh >= 0) {
	    threshold = thresh;
	    NativeEnableReceiveTimeoutThreshold(timeout, threshold, InputBuffer);
	} else /* invalid thresh */
	{
	    throw new IllegalArgumentException("Unexpected negative threshold value");
	}
	if (debug)
	    z.reportln("RXTXPort:enableReceiveThreshold( " + thresh + " ) returned");
    }

    /** 
	*/
    @Override
    public void disableReceiveThreshold() {
	if (debug)
	    z.reportln("RXTXPort:disableReceiveThreshold() called and returning");
	enableReceiveThreshold(0);
    }

    /**
     * @return int the recieve threshold
     */
    @Override
    public int getReceiveThreshold() {
	if (debug)
	    z.reportln("RXTXPort:getReceiveThreshold() called and returning " + threshold);
	return threshold;
    }

    /**
     * @return boolean true if receive threshold is enabled
     */
    @Override
    public boolean isReceiveThresholdEnabled() {
	if (debug)
	    z.reportln("RXTXPort:isReceiveThresholdEnable() called and returning" + (threshold > 0));
	return (threshold > 0);
    }

    /** Input/output buffers */
    /**
     * FIX ME I think this refers to FOPEN(3)/SETBUF(3)/FREAD(3)/FCLOSE(3) taj@www.linux.org.uk
     * 
     * These are native stubs...
     */
    private int InputBuffer = 0;
    private int OutputBuffer = 0;

    /**
     * @param size
     */
    @Override
    public void setInputBufferSize(int size) {
	if (debug)
	    z.reportln("RXTXPort:setInputBufferSize( " + size + ") called");
	if (size < 0)
	    throw new IllegalArgumentException("Unexpected negative buffer size value");
	InputBuffer = size;
	if (debug)
	    z.reportln("RXTXPort:setInputBufferSize( " + size + ") returning");
    }

    /** 
	*/
    @Override
    public int getInputBufferSize() {
	if (debug)
	    z.reportln("RXTXPort:getInputBufferSize() called and returning " + InputBuffer);
	return (InputBuffer);
    }

    /**
     * @param size
     */
    @Override
    public void setOutputBufferSize(int size) {
	if (debug)
	    z.reportln("RXTXPort:setOutputBufferSize( " + size + ") called");
	if (size < 0)
	    throw new IllegalArgumentException("Unexpected negative buffer size value");
	OutputBuffer = size;
	if (debug)
	    z.reportln("RXTXPort:setOutputBufferSize( " + size + ") returned");

    }

    /**
     * @return in the output buffer size
     */
    @Override
    public int getOutputBufferSize() {
	if (debug)
	    z.reportln("RXTXPort:getOutputBufferSize() called and returning " + OutputBuffer);
	return (OutputBuffer);
    }

    /* =================== cleaned messages to here */

    /**
     * Line status methods
     */
    /**
     * @returns true if DTR is set
     */
    @Override
    public native boolean isDTR();

    /**
     * @param state
     */
    @Override
    public native void setDTR(boolean state);

    /**
     * @param state
     */
    @Override
    public native void setRTS(boolean state);

    private native void setDSR(boolean state);

    /**
     * @return boolean true if CTS is set
     */
    @Override
    public native boolean isCTS();

    /**
     * @return boolean true if DSR is set
     */
    @Override
    public native boolean isDSR();

    /**
     * @return boolean true if CD is set
     */
    @Override
    public native boolean isCD();

    /**
     * @return boolean true if RI is set
     */
    @Override
    public native boolean isRI();

    /**
     * @return boolean true if RTS is set
     */
    @Override
    public native boolean isRTS();

    /**
     * Write to the port
     * 
     * @param duration
     */
    @Override
    public native void sendBreak(int duration);

    protected native void writeByte(int b, boolean i) throws IOException;

    protected native void writeArray(byte b[], int off, int len, boolean i) throws IOException;

    protected native boolean nativeDrain(boolean i) throws IOException;

    /** RXTXPort read methods */
    protected native int nativeavailable() throws IOException;

    protected native int readByte() throws IOException;

    protected native int readArray(byte b[], int off, int len) throws IOException;

    protected native int readTerminatedArray(byte b[], int off, int len, byte t[]) throws IOException;

    /** Serial Port Event listener */
    private SerialPortEventListener SPEventListener;

    /** Thread to monitor data */
    private MonitorThread monThread;

    /** Process SerialPortEvents */
    native void eventLoop();

    /**
     * @return boolean true if monitor thread is interrupted
     */
    boolean monThreadisInterrupted = true;

    private native void interruptEventLoop();

    public boolean checkMonitorThread() {
	if (debug)
	    z.reportln("RXTXPort:checkMonitorThread()");
	if (monThread != null) {
	    if (debug)
		z.reportln("monThreadisInterrupted = " + monThreadisInterrupted);
	    return monThreadisInterrupted;
	}
	if (debug)
	    z.reportln("monThread is null ");
	return (true);
    }

    /**
     * @param event
     * @param state
     * @return boolean true if the port is closing
     */
    @SuppressWarnings("unused")
    public boolean sendEvent(int event, boolean state) {
	if (debug_events)
	    z.report("RXTXPort:sendEvent(");
	/* Let the native side know its time to die */

	if (fd == 0 || SPEventListener == null || monThread == null) {
	    return (true);
	}

	switch (event) {
	case SerialPortEvent.DATA_AVAILABLE:
	    if (debug_events)
		z.reportln("DATA_AVAILABLE " + monThread.Data + ")");
	    break;
	case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
	    if (debug_events)
		z.reportln("OUTPUT_BUFFER_EMPTY " + monThread.Output + ")");
	    break;
	case SerialPortEvent.CTS:
	    if (debug_events)
		z.reportln("CTS " + monThread.CTS + ")");
	    break;
	case SerialPortEvent.DSR:
	    if (debug_events)
		z.reportln("DSR " + monThread.Output + ")");
	    break;
	case SerialPortEvent.RI:
	    if (debug_events)
		z.reportln("RI " + monThread.RI + ")");
	    break;
	case SerialPortEvent.CD:
	    if (debug_events)
		z.reportln("CD " + monThread.CD + ")");
	    break;
	case SerialPortEvent.OE:
	    if (debug_events)
		z.reportln("OE " + monThread.OE + ")");
	    break;
	case SerialPortEvent.PE:
	    if (debug_events)
		z.reportln("PE " + monThread.PE + ")");
	    break;
	case SerialPortEvent.FE:
	    if (debug_events)
		z.reportln("FE " + monThread.FE + ")");
	    break;
	case SerialPortEvent.BI:
	    if (debug_events)
		z.reportln("BI " + monThread.BI + ")");
	    break;
	default:
	    if (debug_events)
		z.reportln("XXXXXXXXXXXXXX " + event + ")");
	    break;
	}
	if (debug_events && debug_verbose)
	    z.reportln("	checking flags ");

	switch (event) {
	case SerialPortEvent.DATA_AVAILABLE:
	    if (monThread.Data)
		break;
	    return (false);
	case SerialPortEvent.OUTPUT_BUFFER_EMPTY:
	    if (monThread.Output)
		break;
	    return (false);
	case SerialPortEvent.CTS:
	    if (monThread.CTS)
		break;
	    return (false);
	case SerialPortEvent.DSR:
	    if (monThread.DSR)
		break;
	    return (false);
	case SerialPortEvent.RI:
	    if (monThread.RI)
		break;
	    return (false);
	case SerialPortEvent.CD:
	    if (monThread.CD)
		break;
	    return (false);
	case SerialPortEvent.OE:
	    if (monThread.OE)
		break;
	    return (false);
	case SerialPortEvent.PE:
	    if (monThread.PE)
		break;
	    return (false);
	case SerialPortEvent.FE:
	    if (monThread.FE)
		break;
	    return (false);
	case SerialPortEvent.BI:
	    if (monThread.BI)
		break;
	    return (false);
	default:
	    System.err.println("unknown event: " + event);
	    return (false);
	}
	if (debug_events && debug_verbose)
	    z.reportln("	getting event");
	SerialPortEvent e = new SerialPortEvent(this, event, !state, state);
	if (debug_events && debug_verbose)
	    z.reportln("	sending event");
	if (monThreadisInterrupted) {
	    if (debug_events)
		z.reportln("	sendEvent return");
	    return (true);
	}
	if (SPEventListener != null) {
	    SPEventListener.serialEvent(e);
	}

	if (debug_events && debug_verbose)
	    z.reportln("	sendEvent return");

	if (fd == 0 || SPEventListener == null || monThread == null) {
	    return (true);
	}
	return (false);
    }

    /**
     * Add an event listener
     * 
     * @param lsnr
     *            SerialPortEventListener
     * @throws TooManyListenersException
     */

    boolean MonitorThreadLock = true;

    @Override
    public void addEventListener(SerialPortEventListener lsnr) throws TooManyListenersException {
	/*
	 * Don't let and notification requests happen until the Eventloop is ready
	 */

	if (debug)
	    z.reportln("RXTXPort:addEventListener()");
	if (SPEventListener != null) {
	    throw new TooManyListenersException();
	}
	SPEventListener = lsnr;
	if (!MonitorThreadAlive) {
	    MonitorThreadLock = true;
	    monThread = new MonitorThread();
	    monThread.start();
	    waitForTheNativeCodeSilly();
	    MonitorThreadAlive = true;
	}
	if (debug)
	    z.reportln("RXTXPort:Interrupt=false");
    }

    /**
     * Remove the serial port event listener
     */
    @Override
    public void removeEventListener() {
	if (debug)
	    z.reportln("RXTXPort:removeEventListener() called");
	waitForTheNativeCodeSilly();
	// if( monThread != null && monThread.isAlive() )
	if (monThreadisInterrupted == true) {
	    z.reportln("	RXTXPort:removeEventListener() already interrupted");
	    monThread = null;
	    SPEventListener = null;
	    return;
	} else if (monThread != null && monThread.isAlive()) {
	    if (debug)
		z.reportln("	RXTXPort:Interrupt=true");
	    monThreadisInterrupted = true;
	    /*
	     * Notify all threads in this PID that something is up They will call back to see if its their thread using isInterrupted().
	     */
	    if (debug)
		z.reportln("	RXTXPort:calling interruptEventLoop");
	    interruptEventLoop();

	    if (debug)
		z.reportln("	RXTXPort:calling monThread.join()");
	    try {
		monThread.join(1000);
	    } catch (Exception ex) {
		/* yikes */
		ex.printStackTrace();
	    }
	    if (debug)
		z.reportln("	RXTXPort:waiting on isAlive()");
	    while (monThread.isAlive()) {
		if (debug)
		    z.reportln("	MonThread is still alive!");
		try {
		    monThread.join(1000);
		    Thread.sleep(1000);
		} catch (Exception e) {// JABA is not going to write this code
		}
		// monThread.stop();
	    }

	}
	if (debug)
	    z.reportln("	RXTXPort:calling gc()");
	monThread = null;
	SPEventListener = null;
	MonitorThreadLock = false;
	MonitorThreadAlive = false;
	monThreadisInterrupted = true;
	z.reportln("RXTXPort:removeEventListener() returning");
    }

    /**
     * Give the native code a chance to start listening to the hardware or should we say give the native code control of the issue.
     * 
     * This is important for applications that flicker the Monitor thread while keeping the port open. In worst case test cases this loops once or
     * twice every time.
     */

    protected void waitForTheNativeCodeSilly() {
	while (MonitorThreadLock) {
	    try {
		Thread.sleep(5);
	    } catch (Exception e) {// JABA is not going to write this code
	    }
	}
    }

    /**
     * @param enable
     */
    @SuppressWarnings("hiding")
    private native void nativeSetEventFlag(int fd, int event, boolean flag);

    @Override
    public void notifyOnDataAvailable(boolean enable) {
	if (debug)
	    z.reportln("RXTXPort:notifyOnDataAvailable( " + enable + " )");

	waitForTheNativeCodeSilly();

	MonitorThreadLock = true;
	nativeSetEventFlag(fd, SerialPortEvent.DATA_AVAILABLE, enable);
	monThread.Data = enable;
	MonitorThreadLock = false;
    }

    /**
     * @param enable
     */
    @Override
    public void notifyOnOutputEmpty(boolean enable) {
	if (debug)
	    z.reportln("RXTXPort:notifyOnOutputEmpty( " + enable + " )");
	waitForTheNativeCodeSilly();
	MonitorThreadLock = true;
	nativeSetEventFlag(fd, SerialPortEvent.OUTPUT_BUFFER_EMPTY, enable);
	monThread.Output = enable;
	MonitorThreadLock = false;
    }

    /**
     * @param enable
     */
    @Override
    public void notifyOnCTS(boolean enable) {
	if (debug)
	    z.reportln("RXTXPort:notifyOnCTS( " + enable + " )");
	waitForTheNativeCodeSilly();
	MonitorThreadLock = true;
	nativeSetEventFlag(fd, SerialPortEvent.CTS, enable);
	monThread.CTS = enable;
	MonitorThreadLock = false;
    }

    /**
     * @param enable
     */
    @Override
    public void notifyOnDSR(boolean enable) {
	if (debug)
	    z.reportln("RXTXPort:notifyOnDSR( " + enable + " )");
	waitForTheNativeCodeSilly();
	MonitorThreadLock = true;
	nativeSetEventFlag(fd, SerialPortEvent.DSR, enable);
	monThread.DSR = enable;
	MonitorThreadLock = false;
    }

    /**
     * @param enable
     */
    @Override
    public void notifyOnRingIndicator(boolean enable) {
	if (debug)
	    z.reportln("RXTXPort:notifyOnRingIndicator( " + enable + " )");
	waitForTheNativeCodeSilly();
	MonitorThreadLock = true;
	nativeSetEventFlag(fd, SerialPortEvent.RI, enable);
	monThread.RI = enable;
	MonitorThreadLock = false;
    }

    /**
     * @param enable
     */
    @Override
    public void notifyOnCarrierDetect(boolean enable) {
	if (debug)
	    z.reportln("RXTXPort:notifyOnCarrierDetect( " + enable + " )");
	waitForTheNativeCodeSilly();
	MonitorThreadLock = true;
	nativeSetEventFlag(fd, SerialPortEvent.CD, enable);
	monThread.CD = enable;
	MonitorThreadLock = false;
    }

    /**
     * @param enable
     */
    @Override
    public void notifyOnOverrunError(boolean enable) {
	if (debug)
	    z.reportln("RXTXPort:notifyOnOverrunError( " + enable + " )");
	waitForTheNativeCodeSilly();
	MonitorThreadLock = true;
	nativeSetEventFlag(fd, SerialPortEvent.OE, enable);
	monThread.OE = enable;
	MonitorThreadLock = false;
    }

    /**
     * @param enable
     */
    @Override
    public void notifyOnParityError(boolean enable) {
	if (debug)
	    z.reportln("RXTXPort:notifyOnParityError( " + enable + " )");
	waitForTheNativeCodeSilly();
	MonitorThreadLock = true;
	nativeSetEventFlag(fd, SerialPortEvent.PE, enable);
	monThread.PE = enable;
	MonitorThreadLock = false;
    }

    /**
     * @param enable
     */
    @Override
    public void notifyOnFramingError(boolean enable) {
	if (debug)
	    z.reportln("RXTXPort:notifyOnFramingError( " + enable + " )");
	waitForTheNativeCodeSilly();
	MonitorThreadLock = true;
	nativeSetEventFlag(fd, SerialPortEvent.FE, enable);
	monThread.FE = enable;
	MonitorThreadLock = false;
    }

    /**
     * @param enable
     */
    @Override
    public void notifyOnBreakInterrupt(boolean enable) {
	if (debug)
	    z.reportln("RXTXPort:notifyOnBreakInterrupt( " + enable + " )");
	waitForTheNativeCodeSilly();
	MonitorThreadLock = true;
	nativeSetEventFlag(fd, SerialPortEvent.BI, enable);
	monThread.BI = enable;
	MonitorThreadLock = false;
    }

    /** Close the port */
    @SuppressWarnings("hiding")
    private native void nativeClose(String name);

    /**
	*/
    boolean closeLock = false;

    @Override
    public synchronized void close() {
	if (debug)
	    z.reportln("RXTXPort:close( " + this.name + " )");
	if (closeLock)
	    return;
	closeLock = true;
	while (IOLocked > 0) {
	    if (debug)
		z.reportln("IO is locked " + IOLocked);
	    try {
		Thread.sleep(500);
	    } catch (Exception e) {// JABA is not going to write this code
	    }
	}
	if (fd <= 0) {
	    z.reportln("RXTXPort:close detected bad File Descriptor");
	    return;
	}
	setDTR(false);
	setDSR(false);
	if (debug)
	    z.reportln("RXTXPort:close( " + this.name + " ) setting monThreadisInterrupted");
	if (!monThreadisInterrupted) {
	    removeEventListener();
	}
	if (debug)
	    z.reportln("RXTXPort:close( " + this.name + " ) calling nativeClose");
	nativeClose(this.name);
	if (debug)
	    z.reportln("RXTXPort:close( " + this.name + " ) calling super.close");
	super.close();
	fd = 0;
	closeLock = false;
	if (debug)
	    z.reportln("RXTXPort:close( " + this.name + " ) leaving");
    }

    /** Finalize the port */
    @Override
    protected void finalize() {
	if (debug)
	    z.reportln("RXTXPort:finalize()");
	if (fd > 0) {
	    if (debug)
		z.reportln("RXTXPort:calling close()");
	    close();
	}
	z.finalize();
    }

    /** Inner class for SerialOutputStream */
    class SerialOutputStream extends OutputStream {
	/**
	 * @param b
	 * @throws IOException
	 */
	@Override
	public void write(int b) throws IOException {
	    if (debug_write)
		z.reportln("RXTXPort:SerialOutputStream:write(int)");
	    if (speed == 0)
		return;
	    if (monThreadisInterrupted == true) {
		return;
	    }
	    IOLocked++;
	    waitForTheNativeCodeSilly();
	    if (fd == 0) {
		IOLocked--;
		throw new IOException();
	    }
	    try {
		writeByte(b, monThreadisInterrupted);
		if (debug_write)
		    z.reportln("Leaving RXTXPort:SerialOutputStream:write( int )");
	    } catch (IOException e) {
		IOLocked--;
		throw e;
	    }
	    IOLocked--;
	}

	/**
	 * @param b
	 *            []
	 * @throws IOException
	 */
	@Override
	public void write(byte b[]) throws IOException {
	    if (debug_write) {
		z.reportln("Entering RXTXPort:SerialOutputStream:write(" + b.length + ") "/* + new String(b) */);
	    }
	    if (speed == 0)
		return;
	    if (monThreadisInterrupted == true) {
		return;
	    }
	    if (fd == 0)
		throw new IOException();
	    IOLocked++;
	    waitForTheNativeCodeSilly();
	    try {
		writeArray(b, 0, b.length, monThreadisInterrupted);
		if (debug_write)
		    z.reportln("Leaving RXTXPort:SerialOutputStream:write(" + b.length + ")");
	    } catch (IOException e) {
		IOLocked--;
		throw e;
	    }
	    IOLocked--;

	}

	/**
	 * @param b
	 *            []
	 * @param off
	 * @param len
	 * @throws IOException
	 */
	@Override
	public void write(byte b[], int off, int len) throws IOException {
	    if (speed == 0)
		return;
	    if (off + len > b.length) {
		throw new IndexOutOfBoundsException("Invalid offset/length passed to read");
	    }

	    byte send[] = new byte[len];
	    System.arraycopy(b, off, send, 0, len);
	    if (debug_write) {
		z.reportln("Entering RXTXPort:SerialOutputStream:write(" + send.length + " " + off + " " + len + " " + ") " /*
															     * + new String ( send )
															     */);
	    }
	    if (fd == 0)
		throw new IOException();
	    if (monThreadisInterrupted == true) {
		return;
	    }
	    IOLocked++;
	    waitForTheNativeCodeSilly();
	    try {
		writeArray(send, 0, len, monThreadisInterrupted);
		if (debug_write)
		    z.reportln("Leaving RXTXPort:SerialOutputStream:write(" + send.length + " " + off + " " + len + " " + ") " /*
															        * + new String ( send
															        * )
															        */);
	    } catch (IOException e) {
		IOLocked--;
		throw e;
	    }
	    IOLocked--;
	}

	/**
	*/
	@Override
	public void flush() throws IOException {
	    if (debug)
		z.reportln("RXTXPort:SerialOutputStream:flush() enter");
	    if (speed == 0)
		return;
	    if (fd == 0)
		throw new IOException();
	    if (monThreadisInterrupted == true) {
		if (debug)
		    z.reportln("RXTXPort:SerialOutputStream:flush() Leaving Interrupted");
		return;
	    }
	    IOLocked++;
	    waitForTheNativeCodeSilly();
	    /*
	     * this is probably good on all OS's but for now just sendEvent from java on Sol
	     */
	    try {
		if (nativeDrain(monThreadisInterrupted))
		    sendEvent(SerialPortEvent.OUTPUT_BUFFER_EMPTY, true);
		if (debug)
		    z.reportln("RXTXPort:SerialOutputStream:flush() leave");
	    } catch (IOException e) {
		IOLocked--;
		throw e;
	    }
	    IOLocked--;
	}
    }

    /** Inner class for SerialInputStream */
    class SerialInputStream extends InputStream {
	/**
	 * @return int the int read
	 * @throws IOException
	 * @see java.io.InputStream
	 * 
	 *      timeout threshold Behavior -------------------------------------- ---------------------------------- 0 0 blocks until 1 byte is
	 *      available timeout > 0, threshold = 0, blocks until timeout occurs, returns -1 on timeout >0 >0 blocks until timeout, returns - 1 on
	 *      timeout, magnitude of threshold doesn't play a role. 0 >0 Blocks until 1 byte, magnitude of threshold doesn't play a role
	 */
	@Override
	public synchronized int read() throws IOException {
	    if (debug_read)
		z.reportln("RXTXPort:SerialInputStream:read() called");
	    if (fd == 0)
		throw new IOException();
	    if (monThreadisInterrupted) {
		z.reportln("+++++++++ read() monThreadisInterrupted");
	    }
	    IOLocked++;
	    if (debug_read_results)
		z.reportln("RXTXPort:SerialInputStream:read() L");
	    waitForTheNativeCodeSilly();
	    if (debug_read_results)
		z.reportln("RXTXPort:SerialInputStream:read() N");
	    try {
		int result = readByte();
		if (debug_read_results)
		    // z.reportln(
		    // "RXTXPort:SerialInputStream:read() returns byte = " +
		    // result );
		    z.reportln("RXTXPort:SerialInputStream:read() returns");
		return (result);
	    } finally {
		IOLocked--;
	    }
	}

	/**
	 * @param b
	 *            []
	 * @return int number of bytes read
	 * @throws IOException
	 * 
	 *             timeout threshold Behavior -------------------------------- ---------------------------------------- 0 0 blocks until 1 byte is
	 *             available >0 0 blocks until timeout occurs, returns 0 on timeout >0 >0 blocks until timeout or reads threshold bytes, returns 0
	 *             on timeout 0 >0 blocks until reads threshold bytes
	 */
	@Override
	public synchronized int read(byte b[]) throws IOException {
	    int result;
	    if (debug_read)
		z.reportln("RXTXPort:SerialInputStream:read(" + b.length + ") called");
	    if (monThreadisInterrupted == true) {
		return (0);
	    }
	    IOLocked++;
	    waitForTheNativeCodeSilly();
	    try {
		result = read(b, 0, b.length);
		if (debug_read_results)
		    z.reportln("RXTXPort:SerialInputStream:read() returned " + result + " bytes");
		return (result);
	    } finally {
		IOLocked--;
	    }
	}

	/*
	 * read(byte b[], int, int) Documentation is at http://java.sun.com/products /jdk/1.2/docs/api/java/io/InputStream.html#read(byte[], int, int)
	 */
	/**
	 * @param b
	 *            []
	 * @param off
	 * @param len
	 * @return int number of bytes read
	 * @throws IOException
	 * 
	 *             timeout threshold Behavior -------------------------------- ---------------------------------------- 0 0 blocks until 1 byte is
	 *             available >0 0 blocks until timeout occurs, returns 0 on timeout >0 >0 blocks until timeout or reads threshold bytes, returns 0
	 *             on timeout 0 >0 blocks until either threshold # of bytes or len bytes, whichever was lower.
	 */
	@Override
	public synchronized int read(byte b[], int off, int len) throws IOException {
	    if (debug_read)
		z.reportln("RXTXPort:SerialInputStream:read(" + b.length + " " + off + " " + len + ") called" /* + new String(b) */);
	    int result;
	    /*
	     * Some sanity checks
	     */
	    if (fd == 0) {
		if (debug_read)
		    z.reportln("RXTXPort:SerialInputStream:read() fd == 0");
		z.reportln("+++++++ IOException()\n");
		throw new IOException();
	    }

	    if (b == null) {
		z.reportln("+++++++ NullPointerException()\n");
		if (debug_read)
		    z.reportln("RXTXPort:SerialInputStream:read() b == 0");
		throw new NullPointerException();
	    }

	    if ((off < 0) || (len < 0) || (off + len > b.length)) {
		z.reportln("+++++++ IndexOutOfBoundsException()\n");
		if (debug_read)
		    z.reportln("RXTXPort:SerialInputStream:read() off < 0 ..");
		throw new IndexOutOfBoundsException();
	    }

	    /*
	     * Return immediately if len==0
	     */
	    if (len == 0) {
		if (debug_read)
		    z.reportln("RXTXPort:SerialInputStream:read() off < 0 ..");
		return 0;
	    }
	    /*
	     * See how many bytes we should read
	     */
	    int Minimum = len;

	    if (threshold == 0) {
		/*
		 * If threshold is disabled, read should return as soon as data are available (up to the amount of available bytes in order to avoid
		 * blocking) Read may return earlier depending of the receive time out.
		 */
		int a = nativeavailable();
		if (a == 0)
		    Minimum = 1;
		else
		    Minimum = Math.min(Minimum, a);
	    } else {
		/*
		 * Threshold is enabled. Read should return when 'threshold' bytes have been received (or when the receive timeout expired)
		 */
		Minimum = Math.min(Minimum, threshold);
	    }
	    if (monThreadisInterrupted == true) {
		if (debug_read)
		    z.reportln("RXTXPort:SerialInputStream:read() Interrupted");
		return (0);
	    }
	    IOLocked++;
	    waitForTheNativeCodeSilly();
	    try {
		result = readArray(b, off, Minimum);
		if (debug_read_results)
		    z.reportln("RXTXPort:SerialInputStream:read(" + b.length + " " + off + " " + len + ") returned " + result + " bytes" /*
																	  * + new
																	  * String(b)
																	  */);
		return (result);
	    } finally {
		IOLocked--;
	    }
	}

	/**
	 * @param b
	 *            []
	 * @param off
	 * @param len
	 * @param t
	 *            []
	 * @return int number of bytes read
	 * @throws IOException
	 * 
	 *             We are trying to catch the terminator in the native code Right now it is assumed that t[] is an array of 2 bytes.
	 * 
	 *             if the read encounters the two bytes, it will return and the array will contain the terminator. Otherwise read behavior should
	 *             be the same as read( b[], off, len ). Timeouts have not been well tested.
	 */

	public synchronized int read(byte b[], int off, int len, byte t[]) throws IOException {
	    if (debug_read)
		z.reportln("RXTXPort:SerialInputStream:read(" + b.length + " " + off + " " + len + ") called" /* + new String(b) */);
	    int result;
	    /*
	     * Some sanity checks
	     */
	    if (fd == 0) {
		if (debug_read)
		    z.reportln("RXTXPort:SerialInputStream:read() fd == 0");
		z.reportln("+++++++ IOException()\n");
		throw new IOException();
	    }

	    if (b == null) {
		z.reportln("+++++++ NullPointerException()\n");
		if (debug_read)
		    z.reportln("RXTXPort:SerialInputStream:read() b == 0");
		throw new NullPointerException();
	    }

	    if ((off < 0) || (len < 0) || (off + len > b.length)) {
		z.reportln("+++++++ IndexOutOfBoundsException()\n");
		if (debug_read)
		    z.reportln("RXTXPort:SerialInputStream:read() off < 0 ..");
		throw new IndexOutOfBoundsException();
	    }

	    /*
	     * Return immediately if len==0
	     */
	    if (len == 0) {
		if (debug_read)
		    z.reportln("RXTXPort:SerialInputStream:read() off < 0 ..");
		return 0;
	    }
	    /*
	     * See how many bytes we should read
	     */
	    int Minimum = len;

	    if (threshold == 0) {
		/*
		 * If threshold is disabled, read should return as soon as data are available (up to the amount of available bytes in order to avoid
		 * blocking) Read may return earlier depending of the receive time out.
		 */
		int a = nativeavailable();
		if (a == 0)
		    Minimum = 1;
		else
		    Minimum = Math.min(Minimum, a);
	    } else {
		/*
		 * Threshold is enabled. Read should return when 'threshold' bytes have been received (or when the receive timeout expired)
		 */
		Minimum = Math.min(Minimum, threshold);
	    }
	    if (monThreadisInterrupted == true) {
		if (debug_read)
		    z.reportln("RXTXPort:SerialInputStream:read() Interrupted");
		return (0);
	    }
	    IOLocked++;
	    waitForTheNativeCodeSilly();
	    try {
		result = readTerminatedArray(b, off, Minimum, t);
		if (debug_read_results)
		    z.reportln("RXTXPort:SerialInputStream:read(" + b.length + " " + off + " " + len + ") returned " + result + " bytes" /*
																	  * + new
																	  * String(b)
																	  */);
		return (result);
	    } finally {
		IOLocked--;
	    }
	}

	/**
	 * @return int bytes available
	 * @throws IOException
	 */
	@Override
	public synchronized int available() throws IOException {
	    if (monThreadisInterrupted == true) {
		return (0);
	    }
	    if (debug_verbose)
		z.reportln("RXTXPort:available() called");
	    IOLocked++;
	    try {
		int r = nativeavailable();
		if (debug_verbose)
		    z.reportln("RXTXPort:available() returning " + r);
		return r;
	    } finally {
		IOLocked--;
	    }
	}
    }

    /**
	*/
    class MonitorThread extends Thread {
	/**
	 * Note: these have to be separate boolean flags because the SerialPortEvent constants are NOT bit-flags, they are just defined as integers
	 * from 1 to 10 -DPL
	 */
	volatile boolean CTS = false;
	volatile boolean DSR = false;
	volatile boolean RI = false;
	volatile boolean CD = false;
	volatile boolean OE = false;
	volatile boolean PE = false;
	volatile boolean FE = false;
	volatile boolean BI = false;
	volatile boolean Data = false;
	volatile boolean Output = false;

	MonitorThread() {
	    if (debug)
		z.reportln("RXTXPort:MontitorThread:MonitorThread()");
	}

	/**
	 * run the thread and call the event loop.
	 */
	@Override
	public void run() {
	    if (debug)
		z.reportln("RXTXPort:MontitorThread:run()");
	    monThreadisInterrupted = false;
	    eventLoop();
	    if (debug)
		z.reportln("eventLoop() returned");
	}

	@Override
	protected void finalize() throws Throwable {
	    if (debug)
		z.reportln("RXTXPort:MonitorThread exiting");
	}
    }

    /**
     * A dummy method added so RXTX compiles on Kaffee
     * 
     * @deprecated deprecated but used in Kaffe
     */
    @SuppressWarnings("unused")
    @Deprecated
    public void setRcvFifoTrigger(int trigger) {// JABA is not going to write
						// this code
    }

    /*------------------------  END OF CommAPI -----------------------------*/

    private native static void nativeStaticSetSerialPortParams(String f, int b, int d, int s, int p) throws UnsupportedCommOperationException;

    private native static boolean nativeStaticSetDSR(String port, boolean flag) throws UnsupportedCommOperationException;

    private native static boolean nativeStaticSetDTR(String port, boolean flag) throws UnsupportedCommOperationException;

    private native static boolean nativeStaticSetRTS(String port, boolean flag) throws UnsupportedCommOperationException;

    private native static boolean nativeStaticIsDSR(String port) throws UnsupportedCommOperationException;

    private native static boolean nativeStaticIsDTR(String port) throws UnsupportedCommOperationException;

    private native static boolean nativeStaticIsRTS(String port) throws UnsupportedCommOperationException;

    private native static boolean nativeStaticIsCTS(String port) throws UnsupportedCommOperationException;

    private native static boolean nativeStaticIsCD(String port) throws UnsupportedCommOperationException;

    private native static boolean nativeStaticIsRI(String port) throws UnsupportedCommOperationException;

    private native static int nativeStaticGetBaudRate(String port) throws UnsupportedCommOperationException;

    private native static int nativeStaticGetDataBits(String port) throws UnsupportedCommOperationException;

    private native static int nativeStaticGetParity(String port) throws UnsupportedCommOperationException;

    private native static int nativeStaticGetStopBits(String port) throws UnsupportedCommOperationException;

    private native byte nativeGetParityErrorChar() throws UnsupportedCommOperationException;

    private native boolean nativeSetParityErrorChar(byte b) throws UnsupportedCommOperationException;

    private native byte nativeGetEndOfInputChar() throws UnsupportedCommOperationException;

    private native boolean nativeSetEndOfInputChar(byte b) throws UnsupportedCommOperationException;

    private native boolean nativeSetUartType(String type, boolean test) throws UnsupportedCommOperationException;

    native String nativeGetUartType() throws UnsupportedCommOperationException;

    private native boolean nativeSetBaudBase(int BaudBase) throws UnsupportedCommOperationException;

    private native int nativeGetBaudBase() throws UnsupportedCommOperationException;

    private native boolean nativeSetDivisor(int Divisor) throws UnsupportedCommOperationException;

    private native int nativeGetDivisor() throws UnsupportedCommOperationException;

    private native boolean nativeSetLowLatency() throws UnsupportedCommOperationException;

    private native boolean nativeGetLowLatency() throws UnsupportedCommOperationException;

    private native boolean nativeSetCallOutHangup(boolean NoHup) throws UnsupportedCommOperationException;

    private native boolean nativeGetCallOutHangup() throws UnsupportedCommOperationException;

    private native boolean nativeClearCommInput() throws UnsupportedCommOperationException;

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be supported on all operating systems.
     * 
     * This is only accurate up to 38600 baud currently.
     * 
     * @param port
     *            the name of the port thats been preopened
     * @return BaudRate on success
     * @throws UnsupportedCommOperationException
     *             ; This will not behave as expected with custom speeds
     * 
     */
    public static int staticGetBaudRate(String port) throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:staticGetBaudRate( " + port + " )");
	return (nativeStaticGetBaudRate(port));
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be supported on all operating systems.
     * 
     * @param port
     *            the name of the port thats been preopened
     * @return DataBits on success
     * @throws UnsupportedCommOperationException
     *             ;
     * 
     */
    public static int staticGetDataBits(String port) throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:staticGetDataBits( " + port + " )");
	return (nativeStaticGetDataBits(port));
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be supported on all operating systems.
     * 
     * @param port
     *            the name of the port thats been preopened
     * @return Parity on success
     * @throws UnsupportedCommOperationException
     *             ;
     * 
     */
    public static int staticGetParity(String port) throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:staticGetParity( " + port + " )");
	return (nativeStaticGetParity(port));
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be supported on all operating systems.
     * 
     * @param port
     *            the name of the port thats been preopened
     * @return StopBits on success
     * @throws UnsupportedCommOperationException
     *             ;
     * 
     */
    public static int staticGetStopBits(String port) throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:staticGetStopBits( " + port + " )");
	return (nativeStaticGetStopBits(port));
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be supported on all operating systems.
     * 
     * Set the SerialPort parameters 1.5 stop bits requires 5 databits
     * 
     * @param f
     *            filename
     * @param b
     *            baudrate
     * @param d
     *            databits
     * @param s
     *            stopbits
     * @param p
     *            parity
     * 
     * @throws UnsupportedCommOperationException
     * @see gnu.io.UnsupportedCommOperationException
     */

    public static void staticSetSerialPortParams(String f, int b, int d, int s, int p) throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:staticSetSerialPortParams( " + f + " " + b + " " + d + " " + s + " " + p);
	nativeStaticSetSerialPortParams(f, b, d, s, p);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be supported on all operating systems.
     * 
     * Open the port and set DSR. remove lockfile and do not close This is so some software can appear to set the DSR before 'opening' the port a
     * second time later on.
     * 
     * @return true on success
     * @throws UnsupportedCommOperationException
     *             ;
     * 
     */

    public static boolean staticSetDSR(String port, boolean flag) throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:staticSetDSR( " + port + " " + flag);
	return (nativeStaticSetDSR(port, flag));
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be supported on all operating systems.
     * 
     * Open the port and set DTR. remove lockfile and do not close This is so some software can appear to set the DTR before 'opening' the port a
     * second time later on.
     * 
     * @return true on success
     * @throws UnsupportedCommOperationException
     *             ;
     * 
     */

    public static boolean staticSetDTR(String port, boolean flag) throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:staticSetDTR( " + port + " " + flag);
	return (nativeStaticSetDTR(port, flag));
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be supported on all operating systems.
     * 
     * Open the port and set RTS. remove lockfile and do not close This is so some software can appear to set the RTS before 'opening' the port a
     * second time later on.
     * 
     * @return none
     * @throws UnsupportedCommOperationException
     *             ;
     * 
     */

    public static boolean staticSetRTS(String port, boolean flag) throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:staticSetRTS( " + port + " " + flag);
	return (nativeStaticSetRTS(port, flag));
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be supported on all operating systems.
     * 
     * find the fd and return RTS without using a Java open() call
     * 
     * @param String
     *            port
     * @return boolean true if asserted
     * @throws UnsupportedCommOperationException
     *             ;
     * 
     */

    public static boolean staticIsRTS(String port) throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:staticIsRTS( " + port + " )");
	return (nativeStaticIsRTS(port));
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be supported on all operating systems.
     * 
     * find the fd and return CD without using a Java open() call
     * 
     * @param String
     *            port
     * @return boolean true if asserted
     * @throws UnsupportedCommOperationException
     *             ;
     * 
     */

    public static boolean staticIsCD(String port) throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:staticIsCD( " + port + " )");
	return (nativeStaticIsCD(port));
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be supported on all operating systems.
     * 
     * find the fd and return CTS without using a Java open() call
     * 
     * @param String
     *            port
     * @return boolean true if asserted
     * @throws UnsupportedCommOperationException
     *             ;
     * 
     */

    public static boolean staticIsCTS(String port) throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:staticIsCTS( " + port + " )");
	return (nativeStaticIsCTS(port));
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be supported on all operating systems.
     * 
     * find the fd and return DSR without using a Java open() call
     * 
     * @param String
     *            port
     * @return boolean true if asserted
     * @throws UnsupportedCommOperationException
     *             ;
     * 
     */

    public static boolean staticIsDSR(String port) throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:staticIsDSR( " + port + " )");
	return (nativeStaticIsDSR(port));
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be supported on all operating systems.
     * 
     * find the fd and return DTR without using a Java open() call
     * 
     * @param String
     *            port
     * @return boolean true if asserted
     * @throws UnsupportedCommOperationException
     *             ;
     * 
     */

    public static boolean staticIsDTR(String port) throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:staticIsDTR( " + port + " )");
	return (nativeStaticIsDTR(port));
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be supported on all operating systems.
     * 
     * find the fd and return RI without using a Java open() call
     * 
     * @param String
     *            port
     * @return boolean true if asserted
     * @throws UnsupportedCommOperationException
     *             ;
     * 
     */

    public static boolean staticIsRI(String port) throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:staticIsRI( " + port + " )");
	return (nativeStaticIsRI(port));
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be supported on all operating systems.
     * 
     * @return int the Parity Error Character
     * @throws UnsupportedCommOperationException
     *             ;
     * 
     *             Anyone know how to do this in Unix?
     */

    @Override
    public byte getParityErrorChar() throws UnsupportedCommOperationException {
	byte ret;
	if (debug)
	    z.reportln("getParityErrorChar()");
	ret = nativeGetParityErrorChar();
	if (debug)
	    z.reportln("getParityErrorChar() returns " + ret);
	return (ret);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be supported on all operating systems.
     * 
     * @param b
     *            Parity Error Character
     * @return boolean true on success
     * @throws UnsupportedCommOperationException
     *             ;
     * 
     *             Anyone know how to do this in Unix?
     */

    @Override
    public boolean setParityErrorChar(byte b) throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("setParityErrorChar(" + b + ")");
	return (nativeSetParityErrorChar(b));
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be supported on all operating systems.
     * 
     * @return int the End of Input Character
     * @throws UnsupportedCommOperationException
     *             ;
     * 
     *             Anyone know how to do this in Unix?
     */

    @Override
    public byte getEndOfInputChar() throws UnsupportedCommOperationException {
	byte ret;
	if (debug)
	    z.reportln("getEndOfInputChar()");
	ret = nativeGetEndOfInputChar();
	if (debug)
	    z.reportln("getEndOfInputChar() returns " + ret);
	return (ret);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be supported on all operating systems.
     * 
     * @param b
     *            End Of Input Character
     * @return boolean true on success
     * @throws UnsupportedCommOperationException
     *             ;
     */

    @Override
    public boolean setEndOfInputChar(byte b) throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("setEndOfInputChar(" + b + ")");
	return (nativeSetEndOfInputChar(b));
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be supported on all operating systems.
     * 
     * @param type
     *            String representation of the UART type which mayb be "none", "8250", "16450", "16550", "16550A", "16650", "16550V2" or "16750".
     * @param test
     *            boolean flag to determin if the UART should be tested.
     * @return boolean true on success
     * @throws UnsupportedCommOperationException
     *             ;
     */
    @Override
    public boolean setUARTType(String type, boolean test) throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:setUARTType()");
	return nativeSetUartType(type, test);
    }

    /**
     * Extension to CommAPI This is an extension to CommAPI. It may not be supported on all operating systems.
     * 
     * @return type String representation of the UART type which mayb be "none", "8250", "16450", "16550", "16550A", "16650", "16550V2" or "16750".
     * @throws UnsupportedCommOperationException
     *             ;
     */
    @Override
    public String getUARTType() throws UnsupportedCommOperationException {
	return nativeGetUartType();
    }

    /**
     * Extension to CommAPI. Set Baud Base to 38600 on Linux and W32 before using.
     * 
     * @param int BaudBase The clock frequency divided by 16. Default BaudBase is 115200.
     * @return boolean true on success
     * @throws UnsupportedCommOperationException
     *             , IOException
     */

    @Override
    public boolean setBaudBase(int BaudBase) throws UnsupportedCommOperationException, IOException {
	if (debug)
	    z.reportln("RXTXPort:setBaudBase()");
	return nativeSetBaudBase(BaudBase);
    }

    /**
     * Extension to CommAPI
     * 
     * @return int BaudBase
     * @throws UnsupportedCommOperationException
     *             , IOException
     */

    @Override
    public int getBaudBase() throws UnsupportedCommOperationException, IOException {
	if (debug)
	    z.reportln("RXTXPort:getBaudBase()");
	return nativeGetBaudBase();
    }

    /**
     * Extension to CommAPI. Set Baud Base to 38600 on Linux and W32 before using.
     * 
     * @param int Divisor;
     * @throws UnsupportedCommOperationException
     *             , IOException
     */

    @Override
    public boolean setDivisor(int Divisor) throws UnsupportedCommOperationException, IOException {
	if (debug)
	    z.reportln("RXTXPort:setDivisor()");
	return nativeSetDivisor(Divisor);
    }

    /**
     * Extension to CommAPI
     * 
     * @returns int Divisor;
     * @throws UnsupportedCommOperationException
     *             , IOException
     */

    @Override
    public int getDivisor() throws UnsupportedCommOperationException, IOException {
	if (debug)
	    z.reportln("RXTXPort:getDivisor()");
	return nativeGetDivisor();
    }

    /**
     * Extension to CommAPI returns boolean true on success
     * 
     * @throws UnsupportedCommOperationException
     */

    @Override
    public boolean setLowLatency() throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:setLowLatency()");
	return nativeSetLowLatency();
    }

    /**
     * Extension to CommAPI returns boolean true on success
     * 
     * @throws UnsupportedCommOperationException
     */

    @Override
    public boolean getLowLatency() throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:getLowLatency()");
	return nativeGetLowLatency();
    }

    /**
     * Extension to CommAPI returns boolean true on success
     * 
     * @throws UnsupportedCommOperationException
     */

    @Override
    public boolean setCallOutHangup(boolean NoHup) throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:setCallOutHangup()");
	return nativeSetCallOutHangup(NoHup);
    }

    /**
     * Extension to CommAPI returns boolean true on success
     * 
     * @throws UnsupportedCommOperationException
     */

    @Override
    public boolean getCallOutHangup() throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:getCallOutHangup()");
	return nativeGetCallOutHangup();
    }

    /**
     * Extension to CommAPI returns boolean true on success
     * 
     * @throws UnsupportedCommOperationException
     */

    public boolean clearCommInput() throws UnsupportedCommOperationException {
	if (debug)
	    z.reportln("RXTXPort:clearCommInput()");
	return nativeClearCommInput();
    }

    /*------------------------  END OF CommAPI Extensions -----------------------*/
}
