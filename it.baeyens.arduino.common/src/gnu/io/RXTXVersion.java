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

import it.baeyens.arduino.common.Common;

/**
 * A class to keep the current version in
 */

public class RXTXVersion {
    /*------------------------------------------------------------------------------
     RXTXVersion  
     accept:       -
     perform:      Set Version.
     return:       -
     exceptions:   Throwable
     comments:     
     See INSTALL for details.
     ------------------------------------------------------------------------------*/
    private static String Version;

    static {
	// System.loadLibrary( Common.SerialDllName() );
	// System.load( Common.GetSerialFullDllName() );
	Common.LoadRXTX();
	Version = "RXTX-2.1-7";
    }

    /**
     * static method to return the current version of RXTX unique to RXTX.
     * 
     * @returns a string representing the version "RXTX-1.4-9"
     */
    public static String getVersion() {
	return (Version);
    }

    public static native String nativeGetVersion();
}
