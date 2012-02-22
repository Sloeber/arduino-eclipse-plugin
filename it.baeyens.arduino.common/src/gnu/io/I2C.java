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
import java.io.*;
import java.util.*;
import java.lang.Math;

/**
* @author Trent Jarvi
* @version,
* @since JDK1.0
*/


/**
  * I2C
  */
final class I2C  extends  I2CPort {

	static 
	{
		System.loadLibrary( "rxtxI2C" );
		Initialize();
	}


	/** Initialize the native library */
	private native static void Initialize();


	/** Actual I2CPort wrapper class */


	/** Open the named port */
	public I2C( String name ) throws PortInUseException {
		fd = open( name );
	}
	private native int open( String name ) throws PortInUseException;


	/** File descriptor */
	private int fd;

	/** DSR flag **/
	static boolean dsrFlag = false;

	/** Output stream */
	private final I2COutputStream out = new I2COutputStream();
	public OutputStream getOutputStream() { return out; }


	/** Input stream */
	private final I2CInputStream in = new I2CInputStream();
	public InputStream getInputStream() { return in; }




	/** Set the I2CPort parameters */
	public void setI2CPortParams( int b, int d, int s, int p )
		throws UnsupportedCommOperationException
	{
		nativeSetI2CPortParams( b, d, s, p );
		speed = b;
		dataBits = d;
		stopBits = s;
		parity = p;
	}

	/** Set the native I2C port parameters */
	private native void nativeSetI2CPortParams( int speed, int dataBits,
		int stopBits, int parity ) throws UnsupportedCommOperationException;

	/** Line speed in bits-per-second */
	private int speed=9600;
	public int getBaudRate() { return speed; }

	/** Data bits port parameter */
	private int dataBits=DATABITS_8;
	public int getDataBits() { return dataBits; }

	/** Stop bits port parameter */
	private int stopBits=I2CPort.STOPBITS_1;
	public int getStopBits() { return stopBits; }

	/** Parity port parameter */
	private int parity= I2CPort.PARITY_NONE;
	public int getParity() { return parity; }


	/** Flow control */
	private int flowmode = I2CPort.FLOWCONTROL_NONE;
	public void setFlowControlMode( int flowcontrol ) {
		try { setflowcontrol( flowcontrol ); }
		catch( IOException e ) {
			e.printStackTrace();
			return;
		}
		flowmode=flowcontrol;
	}
	public int getFlowControlMode() { return flowmode; }
	native void setflowcontrol( int flowcontrol ) throws IOException;


	/*
	linux/drivers/char/n_hdlc.c? FIXME
		taj@www.linux.org.uk
	*/
	/** Receive framing control 
	*/
	public void enableReceiveFraming( int f )
		throws UnsupportedCommOperationException
	{
		throw new UnsupportedCommOperationException( "Not supported" );
	}
	public void disableReceiveFraming() {}
	public boolean isReceiveFramingEnabled() { return false; }
	public int getReceiveFramingByte() { return 0; }


	/** Receive timeout control */
	private int timeout = 0;

	public native int NativegetReceiveTimeout();
	public native boolean NativeisReceiveTimeoutEnabled();
	public native void NativeEnableReceiveTimeoutThreshold(int time, int threshold,int InputBuffer);
	public void disableReceiveTimeout(){
		enableReceiveTimeout(0);
	}
	public void enableReceiveTimeout( int time ){
		if( time >= 0 )  {
			timeout = time;
			NativeEnableReceiveTimeoutThreshold( time , threshold, InputBuffer );
		}
		else {
			System.out.println("Invalid timeout");
		}
	}
	public boolean isReceiveTimeoutEnabled(){
		return(NativeisReceiveTimeoutEnabled());
	}
	public int getReceiveTimeout(){
		return(NativegetReceiveTimeout( ));
	}

	/** Receive threshold control */
	
	private int threshold = 0;
	
	public void enableReceiveThreshold( int thresh ){
		if(thresh >=0)
		{
			threshold=thresh;
			NativeEnableReceiveTimeoutThreshold(timeout, threshold, InputBuffer);
		}
		else /* invalid thresh */
		{
			System.out.println("Invalid Threshold");
		}
	}
	public void disableReceiveThreshold() { 
		enableReceiveThreshold(0);
	}
	public int getReceiveThreshold(){
		return threshold;
	}
	public boolean isReceiveThresholdEnabled(){
		return(threshold>0);
	}

	/** Input/output buffers */
	/** FIXME I think this refers to 
		FOPEN(3)/SETBUF(3)/FREAD(3)/FCLOSE(3) 
		taj@www.linux.org.uk

		These are native stubs...
	*/
	private int InputBuffer=0;
	private int OutputBuffer=0;
	public void setInputBufferSize( int size )
	{
		InputBuffer=size;
	}
	public int getInputBufferSize()
	{
		return(InputBuffer);
	}
	public void setOutputBufferSize( int size )
	{
		OutputBuffer=size;
	}
	public int getOutputBufferSize()
	{
		return(OutputBuffer);
	}

	/** Line status methods */
	public native boolean isDTR();
	public native void setDTR( boolean state );
	public native void setRTS( boolean state );
	private native void setDSR( boolean state );
	public native boolean isCTS();
	public native boolean isDSR();
	public native boolean isCD();
	public native boolean isRI();
	public native boolean isRTS();


	/** Write to the port */
	public native void sendBreak( int duration );
	private native void writeByte( int b ) throws IOException;
	private native void writeArray( byte b[], int off, int len )
		throws IOException;
	private native void drain() throws IOException;


	/** I2C read methods */
	private native int nativeavailable() throws IOException;
	private native int readByte() throws IOException;
	private native int readArray( byte b[], int off, int len ) 
		throws IOException;


	/** I2C Port Event listener */
	private I2CPortEventListener SPEventListener;

	/** Thread to monitor data */
	private MonitorThread monThread;

	/** Process I2CPortEvents */
	native void eventLoop();
	private int dataAvailable=0;
	public void sendEvent( int event, boolean state ) {
		switch( event ) {
			case I2CPortEvent.DATA_AVAILABLE:
				dataAvailable=1;
				if( monThread.Data ) break;
				return;
			case I2CPortEvent.OUTPUT_BUFFER_EMPTY:
				if( monThread.Output ) break;
				return;
/*
				if( monThread.DSR ) break;
				return;
				if (isDSR())
				{
					if (!dsrFlag) 
					{
						dsrFlag = true;
						I2CPortEvent e = new I2CPortEvent(this, I2CPortEvent.DSR, !dsrFlag, dsrFlag );
					}
				}
				else if (dsrFlag)
				{
					dsrFlag = false;
					I2CPortEvent e = new I2CPortEvent(this, I2CPortEvent.DSR, !dsrFlag, dsrFlag );
				}
*/
			case I2CPortEvent.CTS:
				if( monThread.CTS ) break;
				return;
			case I2CPortEvent.DSR:
				if( monThread.DSR ) break;
				return;
			case I2CPortEvent.RI:
				if( monThread.RI ) break;
				return;
			case I2CPortEvent.CD:
				if( monThread.CD ) break;
				return;
			case I2CPortEvent.OE:
				if( monThread.OE ) break;
				return;
			case I2CPortEvent.PE:
				if( monThread.PE ) break;
				return;
			case I2CPortEvent.FE:
				if( monThread.FE ) break;
				return;
			case I2CPortEvent.BI:
				if( monThread.BI ) break;
				return;
			default:
				System.err.println("unknown event:"+event);
				return;
		}
		I2CPortEvent e = new I2CPortEvent(this, event, !state, state );
		if( SPEventListener != null ) SPEventListener.I2CEvent( e );
	}

	/** Add an event listener */
	public void addEventListener( I2CPortEventListener lsnr )
		throws TooManyListenersException
	{
		if( SPEventListener != null ) throw new TooManyListenersException();
		SPEventListener = lsnr;
		monThread = new MonitorThread();
		monThread.start(); 
	}
	/** Remove the I2C port event listener */
	public void removeEventListener() {
		SPEventListener = null;
		if( monThread != null ) {
			monThread.interrupt();
			monThread = null;
		}
	}

	public void notifyOnDataAvailable( boolean enable ) { monThread.Data = enable; }

	public void notifyOnOutputEmpty( boolean enable ) { monThread.Output = enable; }

	public void notifyOnCTS( boolean enable ) { monThread.CTS = enable; }
	public void notifyOnDSR( boolean enable ) { monThread.DSR = enable; }
	public void notifyOnRingIndicator( boolean enable ) { monThread.RI = enable; }
	public void notifyOnCarrierDetect( boolean enable ) { monThread.CD = enable; }
	public void notifyOnOverrunError( boolean enable ) { monThread.OE = enable; }
	public void notifyOnParityError( boolean enable ) { monThread.PE = enable; }
	public void notifyOnFramingError( boolean enable ) { monThread.FE = enable; }
	public void notifyOnBreakInterrupt( boolean enable ) { monThread.BI = enable; }


	/** Close the port */
	private native void nativeClose();
	public void close() {
		setDTR(false);
		setDSR(false);
		nativeClose();
		super.close();
		fd = 0;
	}


	/** Finalize the port */
	protected void finalize() {
		if( fd > 0 ) close();
	}


        /** Inner class for I2COutputStream */
        class I2COutputStream extends OutputStream {
                public void write( int b ) throws IOException {
                        writeByte( b );
                }
                public void write( byte b[] ) throws IOException {
                        writeArray( b, 0, b.length );
                }
                public void write( byte b[], int off, int len ) throws IOException {
                        writeArray( b, off, len );
                }
                public void flush() throws IOException {
                        drain();
                }
        }

	/** Inner class for I2CInputStream */
	class I2CInputStream extends InputStream {
		public int read() throws IOException {
			dataAvailable=0;
			return readByte();
		}
		public int read( byte b[] ) throws IOException 
		{
			return read ( b, 0, b.length);
		}
		public int read( byte b[], int off, int len ) throws IOException 
		{
			dataAvailable=0;
			int i=0, Minimum=0;
			int intArray[] = 
			{
				b.length,
				InputBuffer, 
				len
			};
		/*
			find the lowest nonzero value
			timeout and threshold are handled on the native side
			see  NativeEnableReceiveTimeoutThreshold in
			I2CImp.c
		*/
			while(intArray[i]==0 && i < intArray.length) i++;
			Minimum=intArray[i];
			while( i < intArray.length )
			{
				if(intArray[i] > 0 )
				{
					Minimum=Math.min(Minimum,intArray[i]);
				}
				i++;
			}
			Minimum=Math.min(Minimum,threshold);
			if(Minimum == 0) Minimum=1;
			int Available=available();
			int Ret = readArray( b, off, Minimum);
			return Ret;
		}
		public int available() throws IOException {
			return nativeavailable();
		}
	}
	class MonitorThread extends Thread {
	/** Note: these have to be separate boolean flags because the
	   I2CPortEvent constants are NOT bit-flags, they are just
	   defined as integers from 1 to 10  -DPL */
		private boolean CTS=false;
		private boolean DSR=false;
		private boolean RI=false;
		private boolean CD=false;
		private boolean OE=false;
		private boolean PE=false;
		private boolean FE=false;
		private boolean BI=false;
		private boolean Data=false;
		private boolean Output=false;
		MonitorThread() { }
		public void run() {
			eventLoop();
		}
	}
}
