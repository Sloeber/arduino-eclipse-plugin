/*-------------------------------------------------------------------------
|   rxtx is a native interface to serial ports in java.
|   Copyright 1997-2004 by Trent Jarvi taj@www.linux.org.uk.
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

/**
* @author Trent Jarvi
* @version %I%, %G%
* @since JDK1.0
*/

public abstract class SerialPort extends CommPort {
	public static final int  DATABITS_5             =5;
	public static final int  DATABITS_6             =6;
	public static final int  DATABITS_7             =7;
	public static final int  DATABITS_8             =8;
	public static final int  PARITY_NONE            =0;
	public static final int  PARITY_ODD             =1;
	public static final int  PARITY_EVEN            =2;
	public static final int  PARITY_MARK            =3;
	public static final int  PARITY_SPACE           =4;
	public static final int  STOPBITS_1             =1;
	public static final int  STOPBITS_2             =2;
	public static final int  STOPBITS_1_5           =3;
	public static final int  FLOWCONTROL_NONE       =0;
	public static final int  FLOWCONTROL_RTSCTS_IN  =1;
	public static final int  FLOWCONTROL_RTSCTS_OUT =2;
	public static final int  FLOWCONTROL_XONXOFF_IN =4;
	public static final int  FLOWCONTROL_XONXOFF_OUT=8;

	public abstract void setSerialPortParams( int b, int d, int s, int p )
		throws UnsupportedCommOperationException;
	public abstract int getBaudRate();
	public abstract int getDataBits();
	public abstract int getStopBits();
	public abstract int getParity();
	public abstract void setFlowControlMode( int flowcontrol )
		throws UnsupportedCommOperationException;
	public abstract int getFlowControlMode();
	public abstract boolean isDTR();
	public abstract void setDTR( boolean state );
	public abstract void setRTS( boolean state );
	public abstract boolean isCTS();
	public abstract boolean isDSR();
	public abstract boolean isCD();
	public abstract boolean isRI();
	public abstract boolean isRTS();
	public abstract void sendBreak( int duration );
	public abstract void addEventListener( SerialPortEventListener lsnr )
		throws TooManyListenersException;
	public abstract void removeEventListener();
	public abstract void notifyOnDataAvailable( boolean enable );
	public abstract void notifyOnOutputEmpty( boolean enable );
	public abstract void notifyOnCTS( boolean enable );
	public abstract void notifyOnDSR( boolean enable );
	public abstract void notifyOnRingIndicator( boolean enable );
	public abstract void notifyOnCarrierDetect( boolean enable );
	public abstract void notifyOnOverrunError( boolean enable );
	public abstract void notifyOnParityError( boolean enable );
	public abstract void notifyOnFramingError( boolean enable );
	public abstract void notifyOnBreakInterrupt( boolean enable );
/*
	public abstract void setRcvFifoTrigger(int trigger);
         deprecated
*/
/* ----------------------   end of commapi ------------------------ */

/*
	can't have static abstract?

	public abstract static boolean staticSetDTR( String port, boolean flag )
		throws UnsupportedCommOperationException;
	public abstract static boolean staticSetRTS( String port, boolean flag )
		throws UnsupportedCommOperationException;
*/
	public abstract byte getParityErrorChar( )
		throws UnsupportedCommOperationException;
	public abstract boolean setParityErrorChar( byte b )
		throws UnsupportedCommOperationException;
	public abstract byte getEndOfInputChar( )
		throws UnsupportedCommOperationException;
	public abstract boolean setEndOfInputChar( byte b )
		throws UnsupportedCommOperationException;
	public abstract boolean setUARTType(String type, boolean test)
		throws UnsupportedCommOperationException;
	public abstract String getUARTType()
		throws UnsupportedCommOperationException;
	public abstract boolean setBaudBase(int BaudBase)
		throws UnsupportedCommOperationException,
		IOException;
	public abstract int getBaudBase()
		throws UnsupportedCommOperationException,
		IOException;
	public abstract boolean setDivisor(int Divisor)
		throws UnsupportedCommOperationException,
		IOException;
	public abstract int getDivisor()
		throws UnsupportedCommOperationException,
		IOException;
	public abstract boolean setLowLatency()
		throws UnsupportedCommOperationException;
	public abstract boolean getLowLatency()
		throws UnsupportedCommOperationException;
	public abstract boolean setCallOutHangup(boolean NoHup)
		throws UnsupportedCommOperationException;
	public abstract boolean getCallOutHangup()
		throws UnsupportedCommOperationException;
}
