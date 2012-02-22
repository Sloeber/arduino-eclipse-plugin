/*--------------------------------------------------------------------------
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
import java.util.*;

/**
* The port requested is currently in use
* @author Trent Jarvi
* @version %I%, %G%
* @since JDK1.0
*/


public class PortInUseException extends Exception
{
/**
the owner of the port requested.
*/
	public String currentOwner;
/**
* create a instance of the Exception and store the current owner
*
* @param str	detailed information about the current owner
*/
	PortInUseException( String str )
	{
		super( str );
		currentOwner=str;
	}
	public PortInUseException()
	{
		super();
	}
}

