/*******************************************************************************
 * 
 * Copyright (c) 2008, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: MCUNames.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.toolinfo;

import it.baeyens.avreclipse.core.IMCUProvider;
import it.baeyens.avreclipse.core.util.AVRMCUidConverter;

import java.util.HashSet;
import java.util.Set;


/**
 * This class handles the conversion of known MCU ids to MCU Names.
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public class MCUNames implements IMCUProvider {

	private static MCUNames fInstance = null;

	/**
	 * Get the default instance of the Signatures class
	 */
	public static MCUNames getDefault() {
		if (fInstance == null)
			fInstance = new MCUNames();
		return fInstance;
	}

	// private constructor to prevent instantiation
	private MCUNames() {
	}

	/**
	 * Get the Name for the given MCU id.
	 * 
	 * @param mcuid
	 *            String with a MCU id
	 * @return String with the MCU Name.
	 */
	public String getName(String mcuid) {
		return AVRMCUidConverter.id2name(mcuid);
	}

	/**
	 * Get the MCU id for the given Name.
	 * 
	 * @param mcuname
	 *            String with an MCU name
	 * @return String with the corresponding MCU id or <code>null</code> if
	 *         the given id is invalid.
	 */
	public String getID(String mcuname) {
		return AVRMCUidConverter.name2id(mcuname);
	}

	//
	// Methods of the IMCUProvider Interface
	//

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.core.IMCUProvider#getMCUInfo(java.lang.String)
	 */
	public String getMCUInfo(String mcuid) {
		return getName(mcuid);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.core.IMCUProvider#getMCUList()
	 */
	public Set<String> getMCUList() {
		return new HashSet<String>(0);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.core.IMCUProvider#hasMCU(java.lang.String)
	 */
	public boolean hasMCU(String mcuid) {
		if (getName(mcuid)!= null) {
			return true;
		}
		return false;
	}

}
