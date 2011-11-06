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
 * $Id: LockbitBytesProperties.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.avrdude;

import it.baeyens.avreclipse.core.properties.AVRDudeProperties;
import it.baeyens.avreclipse.core.toolinfo.fuses.FuseType;

import java.util.ArrayList;
import java.util.List;

import org.osgi.service.prefs.Preferences;


/**
 * Storage independent container for the Lockbit values.
 * <p>
 * This class has two modes. Depending on the {@link #fUseFile} flag, it will either read the fuse
 * values from a supplied file or immediate values stored in a {@link LockbitsValue} object. The
 * mode is selected by the user in the Properties user interface.
 * </p>
 * <p>
 * This class can be used either standalone or as part of the AVRProjectProperties structure.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public class LockbitBytesProperties extends BaseBytesProperties {

	/**
	 * Create a new LockbitBytesProperties object and load the properties from the Preferences.
	 * <p>
	 * If the given Preferences has no saved properties yet, the default values are used.
	 * </p>
	 * 
	 * @param prefs
	 *            <code>Preferences</code> to read the properties from.
	 * @param parent
	 *            Reference to the <code>AVRDudeProperties</code> parent object.
	 */
	public LockbitBytesProperties(Preferences prefs, AVRDudeProperties parent) {
		super(prefs, parent);
	}

	/**
	 * Create a new LockbitBytesProperties object and copy from the given LockbitBytesProperties
	 * object.
	 * <p>
	 * All values from the source are copied, except for the source Preferences and the Parent.
	 * </p>
	 * 
	 * @param prefs
	 *            <code>Preferences</code> to read the properties from.
	 * @param parent
	 *            Reference to the <code>AVRDudeProperties</code> parent object.
	 * @param source
	 *            <code>FuseBytesProperties</code> object to copy.
	 */
	public LockbitBytesProperties(Preferences prefs, AVRDudeProperties parent,
			BaseBytesProperties source) {
		super(prefs, parent, source);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.core.avrdude.BaseBytesProperties#getType()
	 */
	@Override
	protected FuseType getType() {
		return FuseType.LOCKBITS;
	}

	/**
	 * Get the list of avrdude arguments required to write the lockbit byte.
	 * 
	 * @return <code>List&lt;String&gt;</code> with avrdude action options.
	 */
	@Override
	public List<String> getArguments(String mcuid) {
		List<String> args = new ArrayList<String>();

		if (!isCompatibleWith(mcuid)) {
			// If the fuse bytes are not valid (mismatch between read and
			// assigned mcu id) return an empty list,
			return args;
		}

		if (getUseFile()) {
			// Use a lockbit file
			// Read the locks from the file.

			// TODO Not implemented yet
		}

		// The action factory will take of generating just the right number of
		// actions for the MCU.
		List<AVRDudeAction> allactions = AVRDudeActionFactory.writeLockbitBytes(getMCUId(),
				getValues());

		for (AVRDudeAction action : allactions) {
			args.add(action.getArgument());
		}

		return args;
	}
}
