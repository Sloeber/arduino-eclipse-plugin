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
 * $Id: Register.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.devicedescription.avrio;

import it.baeyens.avreclipse.devicedescription.IEntry;

/**
 * A Register description for the avr/io.h device model.
 * <p>
 * In addition to the data supported by {@link BaseEntry} this entry stores the
 * I/O Address of the register, its type ("IO" or "MEM") and width in bits.
 * </p>
 * 
 * @author Thomas Holland
 * 
 */
public class Register extends BaseEntry {

	private String fAddrType;
	private String fAddr;
	private String fBits;

	
	public Register(IEntry parent) {
		super(parent);
	}

	@Override
	public void setColumnData(int index, String info) {
		switch (index) {
		case RegisterCategory.IDX_NAME:
			setName(info);
			break;
		case RegisterCategory.IDX_DESCRIPTION:
			setDescription(info);
			break;
		case RegisterCategory.IDX_ADDRTYPE:
			setAddrType(info);
			break;
		case RegisterCategory.IDX_ADDR:
			setAddr(info);
			break;
		case RegisterCategory.IDX_BITS:
			setBits(info);
			break;
		}
	}

	@Override
	public String getColumnData(int index) {
		switch (index) {
		case RegisterCategory.IDX_NAME:
			return getName();
		case RegisterCategory.IDX_DESCRIPTION:
			return getDescription();
		case RegisterCategory.IDX_ADDRTYPE:
			return getAddrType();
		case RegisterCategory.IDX_ADDR:
			return getAddr();
		case RegisterCategory.IDX_BITS:
			return getBits();
		}
		return null; // index not supported
	}

	public int getColumnCount() {
		return 5;
	}

	
	/**
	 * Sets the type of the adress: "IO" or "MEM"
	 * 
	 * @param type String with the address type
	 */
	protected void setAddrType(String type) {
		fAddrType = type;
	}
	
	/**
	 * @return The type of the address or an empty String if the address has not been set.
	 */
	protected String getAddrType() {
		if (fAddrType != null) {
			return fAddrType;
		}
		return null;
	}

	/**
	 * Sets the address of the register.
	 * 
	 * @param addr
	 *            String with the address
	 */
	protected void setAddr(String addr) {
		fAddr = addr;
	}

	/**
	 * @return The address of the register or an empty String if the address has
	 *         not been set.
	 */
	protected String getAddr() {
		if (fAddr != null)
			return fAddr;
		return "";
	}

	/**
	 * Sets the number of bits of the register.
	 * 
	 * @param bits
	 *            String with the number of bits ("8" or "16")
	 */
	protected void setBits(String bits) {
		fBits = bits;
	}

	/**
	 * @return The number of bits of the register or an empty String if the
	 *         width has not been set.
	 */
	protected String getBits() {
		if (fBits != null)
			return fBits;
		return null;
	}
}
