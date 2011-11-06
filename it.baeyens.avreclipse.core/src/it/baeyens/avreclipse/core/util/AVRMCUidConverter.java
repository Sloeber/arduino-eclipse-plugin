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
 * $Id: AVRMCUidConverter.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.util;

/**
 * @author Thomas Holland
 * 
 */
public class AVRMCUidConverter {

	/**
	 * Change the lower case mcuid into the official Name.
	 * 
	 * @param mcuid
	 * @return String with UI name of the MCU or <code>null</code> if the given mcuid does not
	 *         match any of the supported name families.
	 */
	public static String id2name(String mcuid) {

		// check invalid mcu names
		if (mcuid == null) {
			return null;
		}
		if ("".equals(mcuid.trim())) {
			return null;
		}

		// AVR Specific
		if (mcuid.startsWith("atxmega")) {
			return "ATXmega" + mcuid.substring(7).toUpperCase();
		}
		if (mcuid.startsWith("atmega")) {
			return "ATmega" + mcuid.substring(6).toUpperCase();
		}
		if (mcuid.startsWith("attiny")) {
			return "ATtiny" + mcuid.substring(6).toUpperCase();
		}
		if (mcuid.startsWith("at")) {
			return mcuid.toUpperCase();
		}
		if (mcuid.startsWith("32")) {
			// AVRDude now supports some AVR32 processors
			// Even though the plugin does not we still accept the name
			return mcuid.toUpperCase();
		}
		if (mcuid.startsWith("avr")) {
			// don't include the generic family names
			return null;
		}

		return null;
	}

	public static String name2id(String mcuname) {
		// just convert to lowercase
		return mcuname.toLowerCase();

	}

}
