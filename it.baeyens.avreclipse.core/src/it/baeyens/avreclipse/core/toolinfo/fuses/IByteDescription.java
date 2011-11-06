/*******************************************************************************
 * Copyright (c) 2010 Thomas Holland (thomas@innot.de) and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 *
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: IByteDescription.java 851 2010-08-07 19:37:00Z innot $
 *******************************************************************************/
package it.baeyens.avreclipse.core.toolinfo.fuses;

import java.util.List;

public interface IByteDescription {

	/**
	 * Get the list of <code>BitFieldDescription</code> objects for this byte.
	 * <p>
	 * The returned list is a copy of the actual list. Any modifications to the returned list do not
	 * apply to the original list of this byte description object.
	 * </p>
	 * 
	 * @return <code>List&lt;BitFieldDescription&gt;</code>
	 */
	public List<BitFieldDescription> getBitFieldDescriptions();

	/**
	 * Get the name of this fuse byte object.
	 * <p>
	 * This is the name as defined in the part description file. Currently the name may be one of
	 * the following:
	 * <ul>
	 * <li><code>LOW</code>, <code>HIGH</code> or <code>EXTENDED</code> for the fuse bytes
	 * of pre-ATXmega MCUs.</li>
	 * <li><code>FUSEBYTE0</code>, <code>FUSEBYTE1</code>, ..., <code>FUSEBYTE5</code> for
	 * the fuse bytes of ATXmega MCUs.</li>
	 * <li><code>LOCKBITS</code> for the lockbits byte.</li>
	 * </ul>
	 * </p>
	 * <p>
	 * <strong>Note:</strong> AVRDude uses different names to access the fuse bytes. It is up to
	 * the caller to map the names as required.
	 * </p>
	 * 
	 * @return The name of the byte.
	 */
	public String getName();

	/**
	 * Get the index of this byte.
	 * <p>
	 * The index is the address of this byte within the Fuses memory block. It is between
	 * <code>0</code> for the first byte (usually called "low") up to the maximum number of btes
	 * supported by the MCU.
	 * </p>
	 * 
	 * @return The byte index.
	 */
	public int getIndex();

	/**
	 * Get the default value of this byte.
	 * <p>
	 * The part description files have only default settings for some MCUs. In these MCUs the return
	 * value will by a byte value (0-255).<br>
	 * For fuse bytes without default value <code>-1</code> is returned.<br>
	 * For lockbit bytes the default value of <code>0xFF</code> is returned (= no locks).
	 * </p>
	 * 
	 * @return The default value or <code>-1</code> if no default available.
	 */
	public int getDefaultValue();

	/**
	 * Checks if the target IByteDescription is compatible with this IByteDescription.
	 * <p>
	 * They are compatible if all BitFields have the same name and the same mask. The meaning of the
	 * BitFields are not checked since we assume that they are reasonably close or identical (this
	 * assumption has not yet been verified).
	 * </p>
	 * 
	 * @param target
	 *            The <code>IByteDescription</code> to check against.
	 * @return <code>true</code> if the given description is (reasonable) compatible.
	 */
	public boolean isCompatibleWith(IByteDescription target);
}