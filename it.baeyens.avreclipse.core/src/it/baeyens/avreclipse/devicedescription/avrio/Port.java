/*******************************************************************************
 * 
 * Copyright (c) 2007, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: Port.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.devicedescription.avrio;

import it.baeyens.avreclipse.devicedescription.IEntry;

/**
 * A I/O Port description for the avr/io.h device model.
 * <p>
 * This extends {@link Register}. The only difference is, that port are always
 * in I/O address space.
 * </p>
 * 
 * @author Thomas Holland
 * 
 */
public class Port extends Register {

	public Port(IEntry parent) {
		super(parent);
	}

	@Override
	public String getAddrType() {
		// ports are always in io space
		return "IO";
	}

}
