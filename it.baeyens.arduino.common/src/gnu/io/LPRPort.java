/*-------------------------------------------------------------------------
|   rxtx is a native interface to communication ports in java.
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
 * LPRPort
 */

final class LPRPort extends ParallelPort {

	static {
		System.loadLibrary("rxtxParallel");
		Initialize();
	}

	/** Initialize the native library */
	private native static void Initialize();

	private final static boolean debug = false;

	/** Open the named port */
	public LPRPort(String name) throws PortInUseException {
		if (debug)
			System.out.println("LPRPort:LPRPort(" + name + ")");
		/*
		 * commapi/javadocs/API_users_guide.html specifies that whenever an
		 * application tries to open a port in use by another application the
		 * PortInUseException will be thrown
		 * 
		 * I know some didnt like it this way but I'm not sure how to avoid it.
		 * We will just be writing to a bogus fd if we catch the exeption
		 * 
		 * Trent
		 */
		// try {
		fd = open(name);
		this.name = name;
		// } catch ( PortInUseException e ){}
		if (debug)
			System.out.println("LPRPort:LPRPort(" + name + ") fd = " + fd);
	}

	private synchronized native int open(String name) throws PortInUseException;

	/** File descriptor */
	int fd;

	/** Output stream */
	private final ParallelOutputStream out = new ParallelOutputStream();

	@Override
	public OutputStream getOutputStream() {
		return out;
	}

	/** Input stream */
	private final ParallelInputStream in = new ParallelInputStream();

	@Override
	public InputStream getInputStream() {
		return in;
	}

	/**
	 * return current mode LPT_MODE_SPP, LPT_MODE_PS2, LPT_MODE_EPP, or
	 * LPT_MODE_ECP
	 */
	private int lprmode = LPT_MODE_ANY;

	@Override
	public int getMode() {
		return lprmode;
	}

	@Override
	public int setMode(int mode) throws UnsupportedCommOperationException {
		try {
			setLPRMode(mode);
		} catch (UnsupportedCommOperationException e) {
			e.printStackTrace();
			return -1;
		}
		lprmode = mode;
		return (0);
	}

	@Override
	public void restart() {
		System.out.println("restart() is not implemented");
	}

	@Override
	public void suspend() {
		System.out.println("suspend() is not implemented");
	}

	public native boolean setLPRMode(int mode) throws UnsupportedCommOperationException;

	@Override
	public native boolean isPaperOut();

	@Override
	public native boolean isPrinterBusy();

	@Override
	public native boolean isPrinterError();

	@Override
	public native boolean isPrinterSelected();

	@Override
	public native boolean isPrinterTimedOut();

	/** Close the port */
	private native void nativeClose();

	@Override
	public synchronized void close() {
		if (fd < 0)
			return;
		nativeClose();
		super.close();
		removeEventListener();

		fd = 0;
		Runtime.getRuntime().gc();
	}

	/** Receive framing control */
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

	@Override
	public void enableReceiveTimeout(int t) {
		if (t > 0)
			timeout = t;
		else
			timeout = 0;
	}

	@Override
	public void disableReceiveTimeout() {
		timeout = 0;
	}

	@Override
	public boolean isReceiveTimeoutEnabled() {
		return timeout > 0;
	}

	@Override
	public int getReceiveTimeout() {
		return timeout;
	}

	/** Receive threshold control */
	private int threshold = 1;

	@Override
	public void enableReceiveThreshold(int t) {
		if (t > 1)
			threshold = t;
		else
			threshold = 1;
	}

	@Override
	public void disableReceiveThreshold() {
		threshold = 1;
	}

	@Override
	public int getReceiveThreshold() {
		return threshold;
	}

	@Override
	public boolean isReceiveThresholdEnabled() {
		return threshold > 1;
	}

	/**
	 * Input/output buffers These are native stubs...
	 */

	@Override
	public native void setInputBufferSize(int size);

	@Override
	public native int getInputBufferSize();

	@Override
	public native void setOutputBufferSize(int size);

	@Override
	public native int getOutputBufferSize();

	@Override
	public native int getOutputBufferFree();

	/** Write to the port */
	protected native void writeByte(int b) throws IOException;

	protected native void writeArray(byte b[], int off, int len) throws IOException;

	protected native void drain() throws IOException;

	/** LPRPort read methods */
	protected native int nativeavailable() throws IOException;

	protected native int readByte() throws IOException;

	protected native int readArray(byte b[], int off, int len) throws IOException;

	/** Parallel Port Event listener */
	private ParallelPortEventListener PPEventListener;

	/** Thread to monitor data */
	private MonitorThread monThread;

	/** Process ParallelPortEvents */
	native void eventLoop();

	public boolean checkMonitorThread() {
		if (monThread != null)
			return monThread.isInterrupted();
		return (true);
	}

	public synchronized boolean sendEvent(int event, boolean state) {
		/* Let the native side know its time to die */

		if (fd == 0 || PPEventListener == null || monThread == null) {
			return (true);
		}

		switch (event) {
		case ParallelPortEvent.PAR_EV_BUFFER:
			if (monThread.monBuffer)
				break;
			return (false);
		case ParallelPortEvent.PAR_EV_ERROR:
			if (monThread.monError)
				break;
			return (false);
		default:
			System.err.println("unknown event:" + event);
			return (false);
		}
		ParallelPortEvent e = new ParallelPortEvent(this, event, !state, state);
		if (PPEventListener != null)
			PPEventListener.parallelEvent(e);
		if (fd == 0 || PPEventListener == null || monThread == null) {
			return (true);
		}
		try {
			Thread.sleep(50);
		} catch (Exception exc) {// JABA is not going to add code
		}
		return (false);

	}

	/** Add an event listener */
	@Override
	public synchronized void addEventListener(ParallelPortEventListener lsnr) throws TooManyListenersException {
		if (PPEventListener != null)
			throw new TooManyListenersException();
		PPEventListener = lsnr;
		monThread = new MonitorThread();
		monThread.start();
	}

	/** Remove the parallel port event listener */
	@Override
	public synchronized void removeEventListener() {
		PPEventListener = null;
		if (monThread != null) {
			monThread.interrupt();
			monThread = null;
		}
	}

	/**
	 * Note: these have to be separate boolean flags because the
	 * ParallelPortEvent constants are NOT bit-flags, they are just defined as
	 * integers from 1 to 10 -DPL
	 */
	@Override
	public synchronized void notifyOnError(boolean enable) {
		System.out.println("notifyOnError is not implemented yet");
		monThread.monError = enable;
	}

	@Override
	public synchronized void notifyOnBuffer(boolean enable) {
		System.out.println("notifyOnBuffer is not implemented yet");
		monThread.monBuffer = enable;
	}

	/** Finalize the port */
	@Override
	protected void finalize() {
		if (fd > 0)
			close();
	}

	/** Inner class for ParallelOutputStream */
	class ParallelOutputStream extends OutputStream {
		@Override
		public synchronized void write(int b) throws IOException {
			if (fd == 0)
				throw new IOException();
			writeByte(b);
		}

		@Override
		public synchronized void write(byte b[]) throws IOException {
			if (fd == 0)
				throw new IOException();
			writeArray(b, 0, b.length);
		}

		@Override
		public synchronized void write(byte b[], int off, int len) throws IOException {
			if (fd == 0)
				throw new IOException();
			writeArray(b, off, len);
		}

		@Override
		public synchronized void flush() throws IOException {
			if (fd == 0)
				throw new IOException();
			// drain();
		}
	}

	/** Inner class for ParallelInputStream */
	class ParallelInputStream extends InputStream {
		@Override
		public int read() throws IOException {
			if (fd == 0)
				throw new IOException();
			return readByte();
		}

		@Override
		public int read(byte b[]) throws IOException {
			if (fd == 0)
				throw new IOException();
			return readArray(b, 0, b.length);
		}

		@Override
		public int read(byte b[], int off, int len) throws IOException {
			if (fd == 0)
				throw new IOException();
			return readArray(b, off, len);
		}

		@Override
		public int available() throws IOException {
			if (fd == 0)
				throw new IOException();
			return nativeavailable();
		}
	}

	class MonitorThread extends Thread {
		boolean monError = false;
		boolean monBuffer = false;

		MonitorThread() {
		}

		@Override
		public void run() {
			eventLoop();
			yield();
		}
	}
}
