/*--------------------------------------------------------------------------
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
import java.util.*;

/**
* @author Trent Jarvi
* @version %I%, %G%
* @since JDK1.0
*/

public interface CommPortOwnershipListener extends EventListener
{
	public static final int PORT_OWNED               =1;
	public static final int PORT_UNOWNED             =2;
	public static final int PORT_OWNERSHIP_REQUESTED =3;

	public abstract void ownershipChange( int type );
}
