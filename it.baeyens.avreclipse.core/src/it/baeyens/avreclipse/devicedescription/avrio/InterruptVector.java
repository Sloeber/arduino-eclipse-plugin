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
 * $Id: InterruptVector.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.devicedescription.avrio;

import it.baeyens.avreclipse.devicedescription.IEntry;

/**
 * A IRQ Vector description for the avr/io.h device model.
 * <p>
 * In addition to the data supported by {@link BaseEntry} this entry
 * stores the vector number and the old ("SIG_*) format of the IRQ.
 * </p>
 * 
 * @author Thomas Holland
 * 
 */
public class InterruptVector extends BaseEntry {

	private String fVector;
	private String fSIGName;

	public InterruptVector(IEntry parent) {
		super(parent);
	}

	@Override
	public String getColumnData(int index) {
		switch (index) {
		case IVecsCategory.IDX_NAME:
			return getName();
		case IVecsCategory.IDX_DESCRIPTION:
			return getDescription();
		case IVecsCategory.IDX_VECTOR:
			return getVector();
		case IVecsCategory.IDX_SIGNAME:
			return getSIGName();
		}
		return null;
	}

	@Override
	public void setColumnData(int index, String info) {
		switch (index) {
		case IVecsCategory.IDX_NAME:
			setName(info);
			break;
		case IVecsCategory.IDX_DESCRIPTION:
			setDescription(info);
			break;
		case IVecsCategory.IDX_VECTOR:
			fVector = info;
			break;
		case IVecsCategory.IDX_SIGNAME:
			fSIGName = info;
			break;
		}
	}

	public int getColumnCount() {
		return 5;
	}

	/**
	 * Set the IRQ vector number of this Interrupt Vector.
	 * 
	 * @param vector
	 *            String with the IVec number
	 */
	protected void setVector(String vector) {
		fVector = vector;
	}

	/**
	 * @return String with the Vector number or an empty String if no vector has
	 *         been set.
	 */
	protected String getVector() {
		if (fVector != null)
			return fVector;
		return "";
	}

	/**
	 * Sets the old and deprecated name of the interrupt vector ("SIG_*"). Some
	 * io*.h files only define this old name.
	 * 
	 * @param signame
	 *            String with the SIG_* name of the IRQ
	 */
	protected void setSIGName(String signame) {
		fSIGName = signame;
	}

	/**
	 * @return String with the old "SIG_*" name or empty String if no signame
	 *         has been set.
	 */
	protected String getSIGName() {
		if (fSIGName != null)
			return fSIGName;
		return "";
	}

}
