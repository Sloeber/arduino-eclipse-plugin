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
 * $Id: IMCUDescription.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.toolinfo.fuses;

import java.util.List;

/**
 * Describes all fuse and lockbits bytes of a single MCU type. An Object that holds
 * {@link BitFieldDescription} objects for a number of bytes.
 * 
 * @author Thomas Holland
 * @since 2.2
 */
public interface IMCUDescription {

	/** <code>int</code> value that represents a non-existing or illegal value. */
	public final static int	NO_VALUE	= -1;

	/**
	 * Get the MCU id value for which the bitfield descriptions are valid.
	 * 
	 * @return <code>String</code> with the MCU id.
	 */
	public String getMCUId();

	/**
	 * Get the release status of the description.
	 * <p>
	 * This is the value of the &lt;RELEASE_STATUS&gt; element from the atmel part description file.
	 * </p>
	 * <p>
	 * As of now the this field has two possible values:
	 * <ul>
	 * <li>RELEASED</li>
	 * <li>INTERNAL</li>
	 * </ul>
	 * </p>
	 * 
	 * @return The value of the release status element.
	 */
	public String getStatus();

	/**
	 * Get the build number of the description.
	 * <p>
	 * This is the value of the &lt;BUILD&gt; element from the atmel part description file.
	 * </p>
	 * <p>
	 * It is used to by the plugin to determine if a fuse/lockbit description file is newer than the
	 * one supplied by the plugin.
	 * </p>
	 * 
	 * @return The value of the build element.
	 */
	public int getVersion();

	/**
	 * Get the number of fuse or lockbits bytes that are defined in this description.
	 * 
	 * @param type
	 *            the type of of fuse memory for which the count is wanted. Either
	 *            {@link FuseType#FUSE} or {@link FuseType#LOCKBITS}.
	 * 
	 * @return <code>int</code> with the number of bytes.
	 */
	public int getByteCount(FuseType type);

	/**
	 * Get the {@link IByteDescription} for a single byte with the given name.
	 * 
	 * @param name
	 *            The name of the byte as used in the part description file.
	 * @return The description for the selected byte, or <code>null</code> if no byte with the
	 *         name exists.
	 */
	public IByteDescription getByteDescription(String name);

	/**
	 * Get the {@link IByteDescription} for a single fuse or lockbits byte.
	 * 
	 * @param type
	 *            the type of bytedescription required. Either {@link FuseType#FUSE} or
	 *            {@link FuseType#LOCKBITS}.
	 * @param index
	 *            The fuse byte for which to get the descriptions. Between 0 and 5, depending on the
	 *            MCU.
	 * @return The description for the selected byte
	 * @throws ArrayIndexOutOfBoundsException
	 *             when the index is invalid.
	 */
	public IByteDescription getByteDescription(FuseType type, int index);

	/**
	 * Get the list of {@link IByteDescription}s for all fuse or lockbits bytes.
	 * <p>
	 * The returned list is a copy of the internal list and can be modified without affecting the
	 * internal list.
	 * </p>
	 * 
	 * @param type
	 *            the type of bytedescriptions required. Either {@link FuseType#FUSE} or
	 *            {@link FuseType#LOCKBITS}.
	 * @return
	 */
	public List<IByteDescription> getByteDescriptions(FuseType type);

	/**
	 * Checks if the target IMCUDescription is compatible with this IMCUDescription.
	 * <p>
	 * They are compatible iff they have the same number of bytes and all BitFields have the same
	 * name and the same mask. The meaning of the BitFields are not checked since we assume that
	 * they are reasonably close or identical (this assumption has not yet been verified).
	 * </p>
	 * <p>
	 * Both the Fuses and the LockBits are compared with this call. As the LockBits seem to be
	 * identical for all AVR processors this should not be a problem.
	 * </p>
	 * 
	 * @param target
	 *            The <code>IMCUDescription</code> to check against.
	 * @param type
	 *            The type of descriptions to compare. Either {@link FuseType#FUSE} or
	 *            {@link FuseType#LOCKBITS}.
	 * @return <code>true</code> if the given description is (reasonable) compatible.
	 */
	public boolean isCompatibleWith(IMCUDescription target, FuseType type);
}