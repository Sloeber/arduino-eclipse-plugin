/* Non functional contact taj@www.linux.org.uk for details */
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

abstract class RawPort extends CommPort {
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
	public static final int  STOPBITS_1_5           =0; //wrong
	public static final int  STOPBITS_2             =2;
	public static final int  FLOWCONTROL_NONE       =0;
	public static final int  FLOWCONTROL_RTSCTS_IN  =1;
	public static final int  FLOWCONTROL_RTSCTS_OUT =2;
	public static final int  FLOWCONTROL_XONXOFF_IN =4;
	public static final int  FLOWCONTROL_XONXOFF_OUT=8;
	public static final int  WRITE_SIZE             =8;
	public static final int  IO_PORT                =0x378;

	public abstract void setRawPortParams( int b, int d, int s, int p ) throws UnsupportedCommOperationException;
	public abstract void addEventListener( RawPortEventListener lsnr ) throws TooManyListenersException;
	public abstract void removeEventListener();
}
