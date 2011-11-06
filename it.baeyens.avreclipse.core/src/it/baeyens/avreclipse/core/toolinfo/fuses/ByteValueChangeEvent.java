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
 * $Id: ByteValueChangeEvent.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.toolinfo.fuses;

/**
 * Single BitField change event.
 * <p>
 * An array of these Events is sent to registered {@link IByteValuesChangeListener}s for any
 * changes in a {@link ByteValues} object.
 * </p>
 * <p>
 * The event has the name of the changed BitField and its new value. As a convenience it also has
 * the index and the value of the byte containing the BitField as well as a reference to the source
 * <code>ByteValues</code>.<br>
 * There are two special event defined. Instead of the name of a BitField they have the following
 * names:
 * <ul>
 * <li>{@link ByteValues#MCU_CHANGE_EVENT}: the MCU of the source ByteValues has changed.</li>
 * <li>{@link ByteValues#COMMENT_CHANGE_EVENT}: The comment property of the source ByteValues has
 * changed.</li>
 * </ul>
 * The receiver can get the actual values from the supplied <code>ByteValues</code>. In either
 * case all other parameters of the event are undefined.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.3
 * 
 */
public class ByteValueChangeEvent {

	/**
	 * The <code>ByteValues</code> that has been changed. This is for reference only and should be
	 * handled read only to avoid infinite loops.
	 */
	public ByteValues	source;

	/**
	 * The name of the field whose value has changed. Either the name of a BitField or one of the
	 * special names: {@link ByteValues#MCU_CHANGE_EVENT} or {@link ByteValues#COMMENT_CHANGE_EVENT}.
	 */
	public String		name;

	/**
	 * The new value of the changed BitField. May be <code>-1</code> if the parent byte has been
	 * cleared.
	 */
	public int			bitfieldvalue;

	// Index / Value of the parent byte.
	// They could be retrieved from the source ByteValues, but they
	// are available when the Event is created in the ByteValues class so
	// we can add them here as a freebie.

	/** Index of the byte containing the changed BitField. */
	public int			byteindex;

	/** New value of the byte containing the changed BitField. */
	public int			bytevalue;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString() For debugging pruposes
	 */
	@Override
	public String toString() {
		return "Event[" + name + "=>" + bitfieldvalue + "]";
	}
}
