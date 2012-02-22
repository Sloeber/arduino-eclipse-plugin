/*--------------------------------------------------------------------------
|   Zystem is a native interface for message reporting in java.
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
* Exception thrown when a method does not support the requested functionality.
* @author Trent Jarvi
* @version %I%, %G%
* @since JDK1.0
*/

public class UnSupportedLoggerException extends Exception
{
/**
* create an instances with no message about why the Exception was thrown.
* @since JDK1.0
*/
	public UnSupportedLoggerException()
	{
		super();
	}
/**
* create an instance with a message about why the Exception was thrown.
* @param str	A detailed message explaining the reason for the Exception.
* @since JDK1.0
*/
	public UnSupportedLoggerException( String str )
	{
		super( str );
	}
}
