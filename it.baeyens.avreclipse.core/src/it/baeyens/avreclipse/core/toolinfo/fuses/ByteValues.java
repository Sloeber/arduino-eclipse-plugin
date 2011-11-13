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
 * $Id: ByteValues.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.toolinfo.fuses;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.core.toolinfo.fuses.ConversionResults.ConversionStatus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;


/**
 * A container for byte values.
 * <p>
 * This class holds the actual byte values for either the Fuse bytes or a Lockbit byte. These byte
 * values are only valid for the current MCU type.
 * </p>
 * <p>
 * This class can notify registered {@link IByteValuesChangeListener}'s about changes to the value
 * of the stored bytes.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * @since 2.3 Change listeners
 * 
 */
public class ByteValues {

	/** The type of Bytes, FUSE oder LOCKBITS */
	private final FuseType						fType;

	/** The MCU for which the byte values are valid. */
	private String								fMCUId;

	/** The number of bytes in this object */
	private int									fByteCount;

	/** The actual byte values. */
	private int[]								fValues;

	/** Map of all bitfield descriptions mapped to their name for easy access */
	private Map<String, BitFieldDescription>	fBitFieldNames;

	/** A user provided comment for this ByteValue object. */
	private String								fComment;

	/**
	 * The results of the last conversion, if this <code>ByteValues</code> object has had its MCU
	 * changed.
	 */
	private ConversionResults					fConversionResults;

	/** List of all registered Listeners. */
	private ListenerList						fListenerList;

	/** Name of the {@link ByteValueChangeEvent} in case the MCU has been changed. */
	public final static String					MCU_CHANGE_EVENT		= "mcuChangeEvent";

	/** Name of the {@link ByteValueChangeEvent} in case the Comment has been changed. */
	public final static String					COMMENT_CHANGE_EVENT	= "commentChangeEvent";

	/**
	 * Create a new byte values container for a given MCU.
	 * <p>
	 * All values are cleared (set to <code>-1</code>).
	 * </p>
	 * 
	 * @param type
	 *            Either <code>FuseType.FUSE</code> or <code>FuseType.LOCKBITS</code>.
	 * @param mcuid
	 *            <code>String</code> with a MCU id value.
	 */
	public ByteValues(FuseType type, String mcuid) {
		Assert.isNotNull(mcuid);
		fType = type;
		fMCUId = mcuid;
		fByteCount = loadByteCount();
		fValues = new int[fByteCount];
		clearValues();
		fComment = null;
		fConversionResults = null;
	}

	/**
	 * Clone constructor.
	 * <p>
	 * Creates a new byte values container and copies all values (and the MCU id) from the source.
	 * </p>
	 * <p>
	 * The list of change listeners from the source object is <em>not</em> copied.
	 * </p>
	 * 
	 * @param source
	 *            <code>ByteValues</code> object to clone.
	 */
	public ByteValues(ByteValues source) {
		Assert.isNotNull(source);
		fType = source.fType;
		fMCUId = source.fMCUId;
		fByteCount = source.fByteCount;
		fValues = new int[fByteCount];
		System.arraycopy(source.fValues, 0, fValues, 0, fByteCount);
		fComment = source.fComment;
		fConversionResults = source.fConversionResults;
	}

	/**
	 * Get the Fuse type for these byte values.
	 * <p>
	 * Currently {@link FuseType#FUSE} and {@link FuseType#LOCKBITS} are the only supported types.
	 * </p>
	 * 
	 * @return
	 */
	public FuseType getType() {
		return fType;
	}

	/**
	 * Get the MCU associated with this ByteValues object and for which the byte values are valid.
	 * 
	 * @return <code>String</code> with a MCU id.
	 */
	public String getMCUId() {
		return fMCUId;
	}

	/**
	 * Change this <code>ByteValues</code> object to a new MCU.
	 * <p>
	 * 
	 * 
	 * @param mcuid
	 * @param convert
	 */
	public void setMCUId(String mcuid, boolean convert) {

		if (fMCUId.equals(mcuid)) {
			// do nothing if the mcu is the same
			return;
		}

		ByteValues conversioncopy = null;
		if (convert) {
			fConversionResults = new ConversionResults();
			conversioncopy = convertTo(mcuid, fConversionResults);
		} else {
			fConversionResults = null;
		}

		fMCUId = mcuid;
		fByteCount = loadByteCount();
		fValues = new int[fByteCount];
		fBitFieldNames = null;

		// First inform all listeners that we have a new MCU
		fireBitFieldChangedEvent(MCU_CHANGE_EVENT, 0, 0, 0);

		// and then set the new values (about which the listeners will be informed as well.
		if (conversioncopy != null) {
			setValues(conversioncopy.getValues());
		} else {
			clearValues();
		}

	}

	/**
	 * Get the actual number of bytes supported by the MCU.
	 * <p>
	 * Depending on the type of this object either the number of fuse bytes (0 up to 6) or the
	 * number of lockbits bytes (currently always 1) is returned.
	 * </p>
	 * <p>
	 * If the MCU is not supported <code>0</code> is returned.
	 * </p>
	 * 
	 * @return Number of bytes supported by the MCU. Between <code>0</code> and <code>6</code>.
	 */
	public int getByteCount() {
		return fByteCount;
	}

	/**
	 * Sets the byte at the given index to a value.
	 * <p>
	 * If this object has been converted before, then the status of all BitFields of this Byte are
	 * set to {@link ConversionStatus#MODIFIED}.
	 * </p>
	 * 
	 * @param index
	 *            The index of the byte to set. Must be between 0 and {@link #getByteCount()}-1.
	 * @param value
	 *            The new value. Must be a byte value (0-255) or -1 to unset the value.
	 * @throws IllegalArgumentException
	 *             if the index is out of bounds or the value is out of range.
	 */
	public void setValue(int index, int value) {

		checkIndex(index);

		if (value < -1 || 255 < value) {
			throw new IllegalArgumentException("Value [" + value + "] out of range (-1...255)");
		}

		fValues[index] = value;

		if (fConversionResults != null) {
			IMCUDescription desc = getDescription(fMCUId);
			IByteDescription bytedesc = desc.getByteDescription(fType, index);
			List<BitFieldDescription> allbfds = bytedesc.getBitFieldDescriptions();
			for (BitFieldDescription bfd : allbfds) {
				String name = bfd.getName();
				fConversionResults.setModified(name);
			}
		}

		fireByteChangedEvent(index, value);
	}

	/**
	 * Get the value of the byte with the given index.
	 * 
	 * @param index
	 *            The index of the byte to read. Must be between 0 and {@link #getByteCount()}-1.
	 * @return <code>int</code> with a byte value (0-255) or <code>-1</code> if the value is not
	 *         set.
	 * @throws IllegalArgumentException
	 *             if the index is out of bounds.
	 */
	public int getValue(int index) {
		checkIndex(index);

		return fValues[index];
	}

	/**
	 * Set all byte values.
	 * <p>
	 * Copies all values from the given array to the internal storage. If the given
	 * <code>int[]</code> has less entries than this class supports, then the remaining bytes are
	 * untouched. Only {@link #getByteCount()} bytes are copied. Any additional bytes in the source
	 * array are ignored.
	 * </p>
	 * <p>
	 * The values of the source are not checked.
	 * </p>
	 * 
	 * @param newvalues
	 *            Array of <code>int</code> with the new byte values (0 to 255 or -1).
	 */
	public void setValues(int[] newvalues) {
		int count = Math.min(newvalues.length, fValues.length);
		System.arraycopy(newvalues, 0, fValues, 0, count);
		for (int index = 0; index < count; index++) {
			fireByteChangedEvent(index, fValues[index]);
		}
	}

	/**
	 * Returns an array with all byte values.
	 * <p>
	 * The returned array is a copy of the internal structure and any changes to it will not be
	 * reflected.
	 * </p>
	 * 
	 * @return Array of <code>int</code> with the current byte values (0 to 255 or -1).
	 */
	public int[] getValues() {
		// make a copy and return it
		int[] copy = new int[fValues.length];
		System.arraycopy(fValues, 0, copy, 0, fValues.length);
		return copy;
	}

	/**
	 * Gets the value of the bitfield with the given name.
	 * <p>
	 * The result is the current value of the bitfield, already normalized (range 0 to maxValue).
	 * </p>
	 * 
	 * @param name
	 *            The name of the bitfield.
	 * @return The current value of the bitfield, or <code>-1</code> if the bitfield value is not
	 *         yet set.
	 * @throws IllegalArgumentException
	 *             if the name of the bitfield is not valid.
	 */
	public int getNamedValue(String name) {
		initBitFieldNames();
		BitFieldDescription desc = fBitFieldNames.get(name);
		if (desc == null) {
			throw new IllegalArgumentException("Bitfield name [" + name + "] is not known.");
		}
		int index = desc.getIndex();
		int value = fValues[index];
		if (value == -1)
			return value;
		return desc.byteToBitField(value);
	}

	/**
	 * Sets the value of the bitfield with the given name.
	 * 
	 * @param name
	 *            The name of the bitfield.
	 * @param value
	 *            The normalized new value for the bitfield (between 0 to maxValue)
	 * @throws IllegalArgumentException
	 *             if the name of the bitfield is not valid or the value is out of range (0 to
	 *             maxValue).
	 */
	public void setNamedValue(String name, int value) {
		initBitFieldNames();

		BitFieldDescription desc = fBitFieldNames.get(name);
		if (desc == null) {
			throw new IllegalArgumentException("Bitfield name [" + name + "] is not known.");
		}

		// clear the conversion results for this BitField (if there is one)
		if (fConversionResults != null) {
			fConversionResults.setModified(name);
		}

		// Test if the value is within bounds
		if (value < 0 || desc.getMaxValue() < value) {
			throw new IllegalArgumentException("Value [" + value + "] out of range (0..."
					+ desc.getMaxValue() + ")");
		}

		int index = desc.getIndex();

		// Now left-shift the value to the right place and insert it
		// into the current value.
		int bitfieldvalue = desc.bitFieldToByte(value);
		int oldvalue = fValues[index];

		boolean completeByte = false;
		if (oldvalue == -1) {
			oldvalue = 0xff;
			completeByte = true;
		}
		int newvalue = oldvalue & ~desc.getMask();
		newvalue |= bitfieldvalue;
		if (completeByte) {
			setValue(index, newvalue); // setValue will fire the notifications
		} else {
			fValues[index] = newvalue;
			fireBitFieldChangedEvent(name, value, index, newvalue);
		}
	}

	/**
	 * Get the descriptive text for the value of the named bitfield.
	 * <p>
	 * This method returns a human readable text for the value of the bitfield. This may be one of
	 * the enumerations from the part description file or some other meaningful text if no
	 * enumerations have been defined.
	 * </p>
	 * 
	 * @see IBitFieldDescription#getValueText(int)
	 * @param name
	 *            The name of the BitField.
	 * @return Human readable string
	 * 
	 */
	public String getNamedValueText(String name) {
		initBitFieldNames();
		BitFieldDescription desc = fBitFieldNames.get(name);
		int value = getNamedValue(name);
		if (value == -1) {
			return "undefined";
		}
		String valuetext = desc.getValueText(value);

		return valuetext;
	}

	/**
	 * Set the value of a BitField to the default.
	 * <p>
	 * The default value comes from the part description file. For some MCUs there exist no default
	 * fuse byte values and LockBits never have a default value. In these cases the method will set
	 * the value of the BitField to all <code>1</code>s.
	 * </p>
	 * 
	 * @param name
	 *            The name of the BitField.
	 */
	public void setNamedValueToDefault(String name) {
		initBitFieldNames();
		BitFieldDescription desc = fBitFieldNames.get(name);

		int defaultvalue = desc.getDefaultValue();
		if (defaultvalue != -1) {
			setNamedValue(name, defaultvalue);
		} else {
			setNamedValue(name, desc.getMaxValue());
		}
	}

	/**
	 * Set all byte values of this object to their default value.
	 * <p>
	 * The default values come from the part description file. If the part description file did not
	 * have any default values (like for the ATXmega series), then <code>-1</code> is set for the
	 * byte.
	 * </p>
	 * <p>
	 * The default value for Lockbits is always <code>0xff</code>, which means all locks diabled.
	 * </p>
	 */
	public void setDefaultValues() {
		IMCUDescription desc = getDescription(fMCUId);
		List<IByteDescription> allbytes = desc.getByteDescriptions(fType);
		for (IByteDescription bytedesc : allbytes) {
			int value = bytedesc.getDefaultValue();
			int index = bytedesc.getIndex();
			setValue(index, value);
		}
	}

	/**
	 * Gets the conversion status of a named BitField after a conversion.
	 * <p>
	 * Conversion is caused by a change of the MCU id for this object. If no conversion was
	 * performed {@link ConversionStatus#NO_CONVERSION} is returned.
	 * </p>
	 * 
	 * @see ConversionStatus
	 * 
	 * @param bitFieldName
	 *            The name of the BitField
	 * @return The status in regard to the last conversion applied.
	 */
	public ConversionStatus getConversionStatus(String bitFieldName) {
		if (fConversionResults != null) {
			return fConversionResults.getStatusForName(bitFieldName);
		}

		return ConversionStatus.NO_CONVERSION;
	}

	/**
	 * Gets the complete ConversionResults from the last conversion applied to this ByteValues
	 * object.
	 * 
	 * @return
	 */
	public ConversionResults getConversionResults() {
		return fConversionResults;
	}

	/**
	 * Clears all values.
	 * <p>
	 * This method will set the value of all bytes to <code>-1</code>
	 * </p>
	 */
	public void clearValues() {
		for (int i = 0; i < fValues.length; i++) {
			setValue(i, -1); // setValue() notififies the listeners
		}
	}

	/**
	 * Get a list of all BitField names.
	 * <p>
	 * The returned list is a copy of the internal list.
	 * </p>
	 * 
	 * @return <code>List&lt;String&gt;</code> with the names.
	 */
	public List<String> getBitfieldNames() {
		initBitFieldNames();
		return new ArrayList<String>(fBitFieldNames.keySet());
	}

	/**
	 * Get a list of all {@link BitFieldDescription} objects.
	 * <p>
	 * The returned list is a copy of the internal list.
	 * </p>
	 * 
	 * @return <code>List&lt;IBitFieldDescription&gt;</code>.
	 */
	public List<BitFieldDescription> getBitfieldDescriptions() {
		initBitFieldNames();
		return new ArrayList<BitFieldDescription>(fBitFieldNames.values());
	}

	/**
	 * Get a single named {@link BitFieldDescription}.
	 * 
	 * @param name
	 *            of the BitField
	 * @return The <code>BitFieldDescription</code> or <code>null</code> if no BitField with the
	 *         given name exists.
	 */
	public BitFieldDescription getBitFieldDescription(String name) {
		initBitFieldNames();
		return fBitFieldNames.get(name);
	}

	/**
	 * Get the name of the byte at the given index.
	 * 
	 * @param index
	 *            Between 0 and {@link #getByteCount()} - 1.
	 * @return Name of the byte from the part description file.
	 */
	public String getByteName(int index) {
		IMCUDescription fusesdesc = getDescription(fMCUId);
		IByteDescription bytedesc = fusesdesc.getByteDescription(fType, index);
		return bytedesc.getName();
	}

	/**
	 * Get the user supplied comment for this <code>ByteValues</code> object.
	 * <p>
	 * The returned String may be <code>null</code> if no comment has been set.
	 * </p>
	 * 
	 * @return The comment String.
	 */
	public String getComment() {
		return fComment;
	}

	/**
	 * Sets a user supplied comment for this <code>ByteValues</code> object.
	 * <p>
	 * This class does not do anything with the comment other than store it. It is up to subclasses
	 * or to the caller to handle the comment.
	 * </p>
	 * 
	 * @param comment
	 *            The new comment or <code>null</code> to clear the comment.
	 */
	public void setComment(String comment) {
		fComment = comment;
		fireBitFieldChangedEvent(COMMENT_CHANGE_EVENT, 0, 0, 0);
	}

	/**
	 * Checks if the index is valid for the subclass.
	 * 
	 * @param index
	 *            Index value to test.
	 * @throws IllegalArgumentException
	 *             if the index is not valid.
	 */
	private void checkIndex(int index) {
		if (!(0 <= index && index < getByteCount())) {
			throw new IllegalArgumentException("[" + index + "] is not a valid byte index.");
		}
	}

	/**
	 * Initialize the Map of Bitfield names to their corresponding description objects.
	 * 
	 */
	private void initBitFieldNames() {

		if (fBitFieldNames != null) {
			// return if the map has already been initialized
			return;
		}

		fBitFieldNames = new HashMap<String, BitFieldDescription>();
		IMCUDescription fusedescription = getDescription(fMCUId);
		if (fusedescription == null) {
			// If the fusedescription could not be read we leave the map empty.
			return;
		}

		// Get all byte descriptions, get the bitfield descriptions from them
		// and fill the map.
		List<IByteDescription> bytedesclist = fusedescription.getByteDescriptions(fType);

		for (IByteDescription bytedesc : bytedesclist) {
			List<BitFieldDescription> bitfieldlist = bytedesc.getBitFieldDescriptions();
			for (BitFieldDescription desc : bitfieldlist) {
				fBitFieldNames.put(desc.getName(), desc);
			}
		}
	}

	/**
	 * Determine the bytecount for the mcu from the description object.
	 * <p>
	 * In case of errors determining the actual bytecount <code>0</code> is returned.
	 * </p>
	 * 
	 * @return Number of bytes supported by the MCU. Between <code>0</code> and <code>6</code>.
	 */
	private int loadByteCount() {
		IMCUDescription fusedescription = getDescription(fMCUId);
		if (fusedescription == null) {
			return 0;
		}
		return fusedescription.getByteCount(fType);
	}

	/**
	 * Get the description object for the given mcu id.
	 * 
	 * @param mcuid
	 * @return <code>IFusesdescription</code> Object or <code>null</code> if the description
	 *         could not be loaded.
	 */
	private IMCUDescription getDescription(String mcuid) {

		// we used to cache the value but the Fuses class already caches all results it is not
		// necessary to do it again.

		IMCUDescription description = null;
		try {
			description = Fuses.getDefault().getDescription(mcuid);
		} catch (IOException e) {
			// Could not read the Description from the plugin
			// Log the error and return null (indicates no fuse bytes)
			IStatus status = new Status(IStatus.ERROR, ArduinoConst.CORE_PLUGIN_ID,
					"Could not read the description file from the filesystem", e);
			AVRPlugin.getDefault().log(status);
			return null;
		}
		return description;
	}

	/**
	 * Test if this Object is compatible with the given MCU.
	 * <p>
	 * The test will be successful iff all BitFields for this and the given MCU have the same name
	 * and the same mask. In this case we assume that the meaning of the BitFields is also the same
	 * or at least very close.
	 * </p>
	 * <p>
	 * If the two MCUs are compatible they can be converted to each other without significant loss
	 * of information.
	 * </p>
	 * 
	 * @return <code>true</code> if the current byte values are also valid for the given MCU id.
	 * 
	 */
	public boolean isCompatibleWith(String mcuid) {
		IMCUDescription ourdesc = getDescription(fMCUId);
		IMCUDescription targetdesc = getDescription(mcuid);
		if (ourdesc == null || targetdesc == null) {
			return false;
		}
		return ourdesc.isCompatibleWith(targetdesc, fType);
	}

	/**
	 * Set the Byte values from the given ByteValues object.
	 * <p>
	 * If the source values are for an incompatible MCU type then two cases are possible as
	 * determined by the <code>forceMCU</code> flag.
	 * <ul>
	 * <li><code>true</code>: The MCU of this ByteValues object is changed to the MCU of the
	 * source ByteValues and then the values from the source are copied 1:1.</li>
	 * <li><code>false</code>: The MCU of this ByteValues object remains the same and the source
	 * ByteValues are first converted to this MCU and then the values are copied.</li>
	 * </ul>
	 * Another difference is, that in only the second case a <code>ConversionResults</code> is
	 * generated.
	 * </p>
	 * 
	 * @param sourcevalues
	 *            An <code>ByteValues</code> object with the source values
	 * @param forceMCU
	 *            <code>true</code> to change this MCU to that of the source, <code>false</code>
	 *            to convert the source values to this MCU.
	 */
	public void setValues(ByteValues sourcevalues, boolean forceMCU) {

		if (forceMCU) {
			// Change our MCU to that of the source values
			setMCUId(sourcevalues.getMCUId(), false);
			setValues(sourcevalues.getValues());
			return;
		}

		if (isCompatibleWith(sourcevalues.getMCUId())) {
			// Compatible mcu -> just copy the values and we're done
			setValues(sourcevalues.getValues());
			return;
		}

		// Don't change our MCU -> convert the source first and then use the
		// resulting values as our values.
		ConversionResults results = new ConversionResults();
		ByteValues converted = sourcevalues.convertTo(getMCUId(), results);
		setValues(converted.getValues());
		fConversionResults = results;

	}

	/**
	 * Convert this ByteValue object to a different MCU.
	 * <p>
	 * This method works by getting a list of all BitField names for the new target MCU and compares
	 * them one by one with the BitField names of this object. If the names match and also the
	 * length of the mask match, then the single BitField value is copied from this object to a new
	 * ByteValues object.
	 * </p>
	 * <p>
	 * All BitFields of the newly created target <code>ByteValues</code>, which do not have a
	 * matching BitField in this <code>ByteValues</code> object, are set to their default value
	 * (if defined) or to all <code>1</code>s.
	 * </p>
	 * 
	 * @param mcuid
	 *            The MCU id for the new <code>ByteValues</code> object.
	 * @param results
	 *            A {@link ConversionResults} object which will maintain the lists of successful and
	 *            unsuccessful BitField conversions.
	 * @return A new <code>ByteValues</code> object valid for the given MCU and with those
	 *         BitFields filled that match this object. All other bits are set to <code>1</code>.
	 */
	public ByteValues convertTo(String mcuid, ConversionResults results) {

		initBitFieldNames();

		// Create a new ByteValues Object for the target mcu
		ByteValues target = new ByteValues(fType, mcuid);

		// Init the results object.
		// We use a copy of ourself as the source, because the setMCUId() method will copy the
		// results of this method to this object, effectively making the target the source.
		results.init(new ByteValues(this), target);

		List<String> targetbfdnames = target.getBitfieldNames();

		for (String name : targetbfdnames) {

			// Check if the name matches.
			if (fBitFieldNames.containsKey(name)) {
				// OK, we have a matching name. Now check if the size of the BitField matches
				BitFieldDescription targetbfd = target.getBitFieldDescription(name);
				BitFieldDescription ourbfd = fBitFieldNames.get(name);
				if (targetbfd.getMaxValue() == ourbfd.getMaxValue()) {
					// identical BitField sizes: now copy the value
					int ourvalue = getNamedValue(name);
					if (ourvalue == -1) {
						// If the value is undefined we do not copy
						results.addUnset(ourbfd);
						continue;
					}
					target.setNamedValue(name, ourvalue);
					// mark the BitField as success and skip over the no-match parts
					results.addSuccess(targetbfd);
					results.removeNotCopied(ourbfd);
					continue;
				}
			}
			// no match found. Set to default value and add to the list
			target.setNamedValueToDefault(name);
			results.addUnset(target.getBitFieldDescription(name));
		}

		results.setReady();

		return target;
	}

	/**
	 * Registers the change listener with the ByteValues object. After registration the
	 * <code>IByteValuesChangeListener</code> is informed about each change of the ByteValues. If
	 * the listener is already registered nothing happens.
	 * <p>
	 * 
	 * @since 2.3
	 * 
	 * @param listener
	 *            The listener to be registered.
	 */
	public void addChangeListener(IByteValuesChangeListener listener) {
		Assert.isNotNull(listener);
		if (fListenerList == null) {
			fListenerList = new ListenerList(ListenerList.IDENTITY);
		}
		fListenerList.add(listener);
	}

	/**
	 * Removes the listener from the <code>ByteValues</code> list of change listeners. If the
	 * listener is not registered with the <code>ByteValues</code> nothing happens.
	 * <p>
	 * 
	 * @since 2.3
	 * 
	 * @param listener
	 *            The listener to be removed.
	 */
	public void removeChangeListener(IByteValuesChangeListener listener) {
		if (fListenerList == null) {
			return;
		}

		fListenerList.remove(listener);
	}

	/**
	 * Fire a {@link ByteValueChangeEvent} event to inform all listeners that the value of a single
	 * BitField has changed.
	 * 
	 * @param name
	 *            Name of the BitField
	 * @param value
	 *            Normalized value of the BitField
	 * @param byteindex
	 *            Index of the byte containing the BitField
	 * @param bytevalue
	 *            Value of the byte containing the BitField
	 */
	private void fireBitFieldChangedEvent(String name, int value, int byteindex, int bytevalue) {
		if (fListenerList == null || fListenerList.size() == 0) {
			// quick exit if we have no listeners
			return;
		}

		ByteValueChangeEvent event = createEvent(name, value, byteindex, bytevalue);
		fireEvents(new ByteValueChangeEvent[] { event });
	}

	/**
	 * Fire an array of {@link ByteValueChangeEvent}'s event to inform all listeners that one
	 * complete byte of this ByteValues has changed.
	 * 
	 * @param name
	 *            Name of the BitField
	 * @param value
	 *            Normalized value of the BitField
	 * @param byteindex
	 *            Index of the byte containing the BitField
	 * @param bytevalue
	 *            Value of the byte containing the BitField
	 */
	private void fireByteChangedEvent(int byteindex, int bytevalue) {
		if (fListenerList == null || fListenerList.size() == 0) {
			// quick exit if we have no listeners
			return;
		}

		List<ByteValueChangeEvent> allevents = new ArrayList<ByteValueChangeEvent>();
		initBitFieldNames();
		for (BitFieldDescription bfd : fBitFieldNames.values()) {
			if (bfd.getIndex() == byteindex) {
				String name = bfd.getName();
				int value = bfd.byteToBitField(bytevalue);
				ByteValueChangeEvent event = createEvent(name, value, byteindex, bytevalue);
				allevents.add(event);
			}
		}

		fireEvents(allevents.toArray(new ByteValueChangeEvent[allevents.size()]));
	}

	/**
	 * Fire the given Events to all listeners.
	 * 
	 * @param events
	 *            Array of Events.
	 */
	private void fireEvents(ByteValueChangeEvent[] events) {
		Object[] listeners = fListenerList.getListeners();
		for (int i = 0; i < listeners.length; ++i) {
			((IByteValuesChangeListener) listeners[i]).byteValuesChanged(events);
		}
	}

	/**
	 * Create a new Event with the given parameters.
	 * 
	 * @param name
	 *            Name of the BitField
	 * @param value
	 *            Normalized value of the BitField
	 * @param byteindex
	 *            Index of the byte containing the BitField
	 * @param bytevalue
	 *            Value of the byte containing the BitField
	 * @return A new <code>ByteValueChangeEvent</code>
	 */
	private ByteValueChangeEvent createEvent(String name, int value, int byteindex, int bytevalue) {

		ByteValueChangeEvent event = new ByteValueChangeEvent();
		event.name = name;
		event.bitfieldvalue = value;
		event.byteindex = byteindex;
		event.bytevalue = bytevalue;
		event.source = this;

		return event;

	}
}
