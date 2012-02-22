/*-------------------------------------------------------------------------
|   rxtx is a native interface to serial ports in java.
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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
* @author Trent Jarvi
* @version %I%, %G%
* @since JDK1.0
*/


/**
  * CommPort
  */
public abstract class CommPort extends Object {
	protected String name;
	private final static boolean debug = false;

	public abstract void enableReceiveFraming( int f ) 
		throws UnsupportedCommOperationException;
	public abstract void disableReceiveFraming();
	public abstract boolean isReceiveFramingEnabled();
	public abstract int getReceiveFramingByte();
	public abstract void disableReceiveTimeout();
	public abstract void enableReceiveTimeout( int time )
		throws UnsupportedCommOperationException;
	public abstract boolean isReceiveTimeoutEnabled();
	public abstract int getReceiveTimeout();
	public abstract void enableReceiveThreshold( int thresh )
		throws UnsupportedCommOperationException;
	public abstract void disableReceiveThreshold();
	public abstract int getReceiveThreshold();
	public abstract boolean isReceiveThresholdEnabled();
	public abstract void setInputBufferSize( int size );
	public abstract int getInputBufferSize();
	public abstract void setOutputBufferSize( int size );
	public abstract int getOutputBufferSize();
	public void close() 
	{
		if (debug) System.out.println("CommPort:close()");

		try
		{
			CommPortIdentifier cp = 
				CommPortIdentifier.getPortIdentifier(this);
			if ( cp != null )
				cp.getPortIdentifier(this).internalClosePort();
		}
		catch (NoSuchPortException e)
		{
		}
	};

	public abstract InputStream getInputStream() throws IOException;
	public abstract OutputStream getOutputStream() throws IOException;

	public String getName()
	{
		if (debug) System.out.println("CommPort:getName()");
		return( name );
	}
	public String toString()
	{
		if (debug) System.out.println("CommPort:toString()");
		return( name );
	}
}
