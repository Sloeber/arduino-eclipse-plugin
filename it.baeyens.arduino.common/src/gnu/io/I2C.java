/*-------------------------------------------------------------------------
|   rxtx is a native interface to I2C ports in java.
|   Copyright 1997-2004 by Trent Jarvi taj@www.linux.org.uk
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.TooManyListenersException;

/**
 * @author Trent Jarvi
 * @version,
 * @since JDK1.0
 */

/**
 * I2C
 */
final class I2C extends I2CPort {

    static {
	System.loadLibrary("rxtxI2C");
	Initialize();
    }

    /** Initialize the native library */
    private native static void Initialize();

    /** Actual I2CPort wrapper class */

    /** Open the named port */
    public I2C(String name) throws PortInUseException {
	fd = open(name);
    }

    @SuppressWarnings("hiding")
    private native int open(String name) throws PortInUseException;

    /** File descriptor */
    private int fd;

    /** DSR flag **/
    static boolean dsrFlag = false;

    /** Output stream */
    private final I2COutputStream out = new I2COutputStream();

    @Override
    public OutputStream getOutputStream() {
	return out;
    }

    /** Input stream */
    private final I2CInputStream in = new I2CInputStream();

    @Override
    public InputStream getInputStream() {
	return in;
    }

    /** Set the I2CPort parameters */
    @Override
    public void setI2CPortParams(int b, int d, int s, int p) throws UnsupportedCommOperationException {
	nativeSetI2CPortParams(b, d, s, p);
	speed = b;
	dataBits = d;
	stopBits = s;
	parity = p;
    }

    /** Set the native I2C port parameters */
    @SuppressWarnings("hiding")
    private native void nativeSetI2CPortParams(int speed, int dataBits, int stopBits, int parity) throws UnsupportedCommOperationException;

    /** Line speed in bits-per-second */
    private int speed = 9600;

    @Override
    public int getBaudRate() {
	return speed;
    }

    /** Data bits port parameter */
    private int dataBits = DATABITS_8;

    @Override
    public int getDataBits() {
	return dataBits;
    }

    /** Stop bits port parameter */
    private int stopBits = I2CPort.STOPBITS_1;

    @Override
    public int getStopBits() {
	return stopBits;
    }

    /** Parity port parameter */
    private int parity = I2CPort.PARITY_NONE;

    @Override
    public int getParity() {
	return parity;
    }

    /** Flow control */
    private int flowmode = I2CPort.FLOWCONTROL_NONE;

    @Override
    public void setFlowControlMode(int flowcontrol) {
	try {
	    setflowcontrol(flowcontrol);
	} catch (IOException e) {
	    e.printStackTrace();
	    return;
	}
	flowmode = flowcontrol;
    }

    @Override
    public int getFlowControlMode() {
	return flowmode;
    }

    native void setflowcontrol(int flowcontrol) throws IOException;

    /*
     * linux/drivers/char/n_hdlc.c? FIX ME taj@www.linux.org.uk
     */
    /**
     * Receive framing control
     */
    @Override
    public void enableReceiveFraming(int f) throws UnsupportedCommOperationException {
	throw new UnsupportedCommOperationException("Not supported");
    }

    @Override
    public void disableReceiveFraming() {// JABA is not going to add code
    }

    @Override
    public boolean isReceiveFramingEnabled() {
	return false;
    }

    @Override
    public int getReceiveFramingByte() {
	return 0;
    }

    /** Receive timeout control */
    private int timeout = 0;

    public native int NativegetReceiveTimeout();

    public native boolean NativeisReceiveTimeoutEnabled();

    @SuppressWarnings("hiding")
    public native void NativeEnableReceiveTimeoutThreshold(int time, int threshold, int InputBuffer);

    @Override
    public void disableReceiveTimeout() {
	enableReceiveTimeout(0);
    }

    @Override
    public void enableReceiveTimeout(int time) {
	if (time >= 0) {
	    timeout = time;
	    NativeEnableReceiveTimeoutThreshold(time, threshold, InputBuffer);
	} else {
	    System.out.println("Invalid timeout");
	}
    }

    @Override
    public boolean isReceiveTimeoutEnabled() {
	return (NativeisReceiveTimeoutEnabled());
    }

    @Override
    public int getReceiveTimeout() {
	return (NativegetReceiveTimeout());
    }

    /** Receive threshold control */

    int threshold = 0;

    @Override
    public void enableReceiveThreshold(int thresh) {
	if (thresh >= 0) {
	    threshold = thresh;
	    NativeEnableReceiveTimeoutThreshold(timeout, threshold, InputBuffer);
	} else /* invalid thresh */
	{
	    System.out.println("Invalid Threshold");
	}
    }

    @Override
    public void disableReceiveThreshold() {
	enableReceiveThreshold(0);
    }

    @Override
    public int getReceiveThreshold() {
	return threshold;
    }

    @Override
    public boolean isReceiveThresholdEnabled() {
	return (threshold > 0);
    }

    /** Input/output buffers */
    /**
     * FIX ME I think this refers to FOPEN(3)/SETBUF(3)/FREAD(3)/FCLOSE(3) taj@www.linux.org.uk
     * 
     * These are native stubs...
     */
    int InputBuffer = 0;
    private int OutputBuffer = 0;

    @Override
    public void setInputBufferSize(int size) {
	InputBuffer = size;
    }

    @Override
    public int getInputBufferSize() {
	return (InputBuffer);
    }

    @Override
    public void setOutputBufferSize(int size) {
	OutputBuffer = size;
    }

    @Override
    public int getOutputBufferSize() {
	return (OutputBuffer);
    }

    /** Line status methods */
    @Override
    public native boolean isDTR();

    @Override
    public native void setDTR(boolean state);

    @Override
    public native void setRTS(boolean state);

    private native void setDSR(boolean state);

    @Override
    public native boolean isCTS();

    @Override
    public native boolean isDSR();

    @Override
    public native boolean isCD();

    @Override
    public native boolean isRI();

    @Override
    public native boolean isRTS();

    /** Write to the port */
    @Override
    public native void sendBreak(int duration);

    native void writeByte(int b) throws IOException;

    native void writeArray(byte b[], int off, int len) throws IOException;

    native void drain() throws IOException;

    /** I2C read methods */
    native int nativeavailable() throws IOException;

    native int readByte() throws IOException;

    native int readArray(byte b[], int off, int len) throws IOException;

    /** I2C Port Event listener */
    private I2CPortEventListener SPEventListener;

    /** Thread to monitor data */
    private MonitorThread monThread;

    /** Process I2CPortEvents */
    native void eventLoop();

    int dataAvailable = 0;

    public void sendEvent(int event, boolean state) {
	switch (event) {
	case I2CPortEvent.DATA_AVAILABLE:
	    dataAvailable = 1;
	    if (monThread.Data)
		break;
	    return;
	case I2CPortEvent.OUTPUT_BUFFER_EMPTY:
	    if (monThread.Output)
		break;
	    return;
	    /*
	     * if( monThread.DSR ) break; return; if (isDSR()) { if (!dsrFlag) { dsrFlag = true; I2CPortEvent e = new I2CPortEvent(this,
	     * I2CPortEvent.DSR, !dsrFlag, dsrFlag ); } } else if (dsrFlag) { dsrFlag = false; I2CPortEvent e = new I2CPortEvent(this,
	     * I2CPortEvent.DSR, !dsrFlag, dsrFlag ); }
	     */
	case I2CPortEvent.CTS:
	    if (monThread.CTS)
		break;
	    return;
	case I2CPortEvent.DSR:
	    if (monThread.DSR)
		break;
	    return;
	case I2CPortEvent.RI:
	    if (monThread.RI)
		break;
	    return;
	case I2CPortEvent.CD:
	    if (monThread.CD)
		break;
	    return;
	case I2CPortEvent.OE:
	    if (monThread.OE)
		break;
	    return;
	case I2CPortEvent.PE:
	    if (monThread.PE)
		break;
	    return;
	case I2CPortEvent.FE:
	    if (monThread.FE)
		break;
	    return;
	case I2CPortEvent.BI:
	    if (monThread.BI)
		break;
	    return;
	default:
	    System.err.println("unknown event:" + event);
	    return;
	}
	I2CPortEvent e = new I2CPortEvent(this, event, !state, state);
	if (SPEventListener != null)
	    SPEventListener.I2CEvent(e);
    }

    /** Add an event listener */
    @Override
    public void addEventListener(I2CPortEventListener lsnr) throws TooManyListenersException {
	if (SPEventListener != null)
	    throw new TooManyListenersException();
	SPEventListener = lsnr;
	monThread = new MonitorThread();
	monThread.start();
    }

    /** Remove the I2C port event listener */
    @Override
    public void removeEventListener() {
	SPEventListener = null;
	if (monThread != null) {
	    monThread.interrupt();
	    monThread = null;
	}
    }

    @Override
    public void notifyOnDataAvailable(boolean enable) {
	monThread.Data = enable;
    }

    @Override
    public void notifyOnOutputEmpty(boolean enable) {
	monThread.Output = enable;
    }

    @Override
    public void notifyOnCTS(boolean enable) {
	monThread.CTS = enable;
    }

    @Override
    public void notifyOnDSR(boolean enable) {
	monThread.DSR = enable;
    }

    @Override
    public void notifyOnRingIndicator(boolean enable) {
	monThread.RI = enable;
    }

    @Override
    public void notifyOnCarrierDetect(boolean enable) {
	monThread.CD = enable;
    }

    @Override
    public void notifyOnOverrunError(boolean enable) {
	monThread.OE = enable;
    }

    @Override
    public void notifyOnParityError(boolean enable) {
	monThread.PE = enable;
    }

    @Override
    public void notifyOnFramingError(boolean enable) {
	monThread.FE = enable;
    }

    @Override
    public void notifyOnBreakInterrupt(boolean enable) {
	monThread.BI = enable;
    }

    /** Close the port */
    private native void nativeClose();

    @Override
    public void close() {
	setDTR(false);
	setDSR(false);
	nativeClose();
	super.close();
	fd = 0;
    }

    /** Finalize the port */
    @Override
    protected void finalize() {
	if (fd > 0)
	    close();
    }

    /** Inner class for I2COutputStream */
    class I2COutputStream extends OutputStream {
	@Override
	public void write(int b) throws IOException {
	    writeByte(b);
	}

	@Override
	public void write(byte b[]) throws IOException {
	    writeArray(b, 0, b.length);
	}

	@Override
	public void write(byte b[], int off, int len) throws IOException {
	    writeArray(b, off, len);
	}

	@Override
	public void flush() throws IOException {
	    drain();
	}
    }

    /** Inner class for I2CInputStream */
    class I2CInputStream extends InputStream {
	@Override
	public int read() throws IOException {
	    dataAvailable = 0;
	    return readByte();
	}

	@Override
	public int read(byte b[]) throws IOException {
	    return read(b, 0, b.length);
	}

	@Override
	public int read(byte b[], int off, int len) throws IOException {
	    dataAvailable = 0;
	    int i = 0, Minimum = 0;
	    int intArray[] = { b.length, InputBuffer, len };
	    /*
	     * find the lowest nonzero value timeout and threshold are handled on the native side see NativeEnableReceiveTimeoutThreshold in I2CImp.c
	     */
	    while (intArray[i] == 0 && i < intArray.length)
		i++;
	    Minimum = intArray[i];
	    while (i < intArray.length) {
		if (intArray[i] > 0) {
		    Minimum = Math.min(Minimum, intArray[i]);
		}
		i++;
	    }
	    Minimum = Math.min(Minimum, threshold);
	    if (Minimum == 0)
		Minimum = 1;
	    @SuppressWarnings("unused")
	    int Available = available();
	    int Ret = readArray(b, off, Minimum);
	    return Ret;
	}

	@Override
	public int available() throws IOException {
	    return nativeavailable();
	}
    }

    class MonitorThread extends Thread {
	/**
	 * Note: these have to be separate boolean flags because the I2CPortEvent constants are NOT bit-flags, they are just defined as integers from
	 * 1 to 10 -DPL
	 */
	boolean CTS = false;
	boolean DSR = false;
	boolean RI = false;
	boolean CD = false;
	boolean OE = false;
	boolean PE = false;
	boolean FE = false;
	boolean BI = false;
	boolean Data = false;
	boolean Output = false;

	MonitorThread() {
	}

	@Override
	public void run() {
	    eventLoop();
	}
    }
}
