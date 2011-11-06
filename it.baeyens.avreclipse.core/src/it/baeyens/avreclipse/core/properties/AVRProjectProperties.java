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
 * $Id: AVRProjectProperties.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.properties;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Container for all AVR Plugin specific properties of a project.
 * <p>
 * Upon instantiation, the properties are loaded from the given preference
 * store. All changes are local to the object until the {@link #save()} method
 * is called.
 * </p>
 * <p>
 * AVRConfigurationProperties objects do not reflect changes made to other
 * AVRConfigurationProperties for the same Project/Configuration, so they should
 * not be held on to and be reloaded every time the current values are required.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 */
public class AVRProjectProperties {

	private static final String NODE_AVRDUDE = "avrdude";

	private static final String KEY_MCUTYPE = "MCUType";
	private static final String DEFAULT_MCUTYPE = "atmega16";
	private String fMCUid;

	private static final String KEY_FCPU = "ClockFrequency";
	private static final int DEFAULT_FCPU = 1000000;
	private int fFCPU;

	private static final String KEY_USE_EXT_RAM = "ExtendedRAM";
	private static final boolean DEFAULT_USE_EXT_RAM = false;
	private boolean fUseExtRAM;

	private static final String KEY_EXT_RAM_SIZE = "ExtRAMSize";
	private int fExtRAMSize;

	private static final String KEY_USE_EXT_RAM_HEAP = "UseExtendedRAMforHeap";
	private boolean fUseExtRAMforHeap;

	private static final String KEY_USE_EEPROM = "UseEEPROM";
	private static final boolean DEFAULT_USE_EEPROM = false;
	private boolean fUseEEPROM;

	private AVRDudeProperties fAVRDudeProperties;

	/**
	 * The source/target Preferences for the properties or <code>null</code>
	 * if default properties are represented.
	 */
	private IEclipsePreferences fPrefs;

	/** Flag if any properties have been changed */
	private boolean fDirty;

	/**
	 * Load the AVR project properties from the given Preferences.
	 * 
	 * @param prefs
	 *            <code>IEclipsePreferences</code>
	 */
	public AVRProjectProperties(IEclipsePreferences prefs) {
		fPrefs = prefs;
		loadData();
	}

	/**
	 * Load the AVR Project properties from the given
	 * <code>AVRConfigurationProperties</code> object.
	 * 
	 * @param source
	 */
	public AVRProjectProperties(IEclipsePreferences prefs, AVRProjectProperties source) {
		fPrefs = prefs;
		fMCUid = source.fMCUid;
		fFCPU = source.fFCPU;

		fUseExtRAM = source.fUseExtRAM;
		fExtRAMSize = source.fExtRAMSize;
		fUseExtRAMforHeap = source.fUseExtRAMforHeap;
		fUseEEPROM = source.fUseEEPROM;

		fAVRDudeProperties = new AVRDudeProperties(prefs.node(NODE_AVRDUDE), this,
		        source.fAVRDudeProperties);

		fDirty = source.fDirty;
	}

	public String getMCUId() {
		return fMCUid;
	}

	public void setMCUId(String mcuid) {
		if (!fMCUid.equals(mcuid)) {
			fMCUid = mcuid;
			fDirty = true;
		}
	}

	public String getFCPU() {
		return Integer.toString(fFCPU);
	}

	public void setFCPU(String fcpu) {
		int newvalue = Integer.parseInt(fcpu);
		if (fFCPU != newvalue) {
			fFCPU = newvalue;
			fDirty = true;
		}
	}

	public AVRDudeProperties getAVRDudeProperties() {
		return fAVRDudeProperties;
	}

	/**
	 * Load all options from the preferences.
	 */
	protected void loadData() {
		fMCUid = fPrefs.get(KEY_MCUTYPE, DEFAULT_MCUTYPE);
		fFCPU = fPrefs.getInt(KEY_FCPU, DEFAULT_FCPU);

		fUseExtRAM = fPrefs.getBoolean(KEY_USE_EXT_RAM, DEFAULT_USE_EXT_RAM);
		fExtRAMSize = fPrefs.getInt(KEY_EXT_RAM_SIZE, 0);
		fUseExtRAMforHeap = fPrefs.getBoolean(KEY_USE_EXT_RAM_HEAP, true);
		fUseEEPROM = fPrefs.getBoolean(KEY_USE_EEPROM, DEFAULT_USE_EEPROM);

		fAVRDudeProperties = new AVRDudeProperties(fPrefs.node(NODE_AVRDUDE), this);

		fDirty = false;
	}

	/**
	 * Save the modified properties to the persistent storage.
	 * 
	 * @throws BackingStoreException
	 */
	public void save() throws BackingStoreException {

		try {
			if (fDirty) {
				// Save the properties of this class
				fDirty = false;
				fPrefs.put(KEY_MCUTYPE, fMCUid);
				fPrefs.putInt(KEY_FCPU, fFCPU);

				fPrefs.putBoolean(KEY_USE_EXT_RAM, fUseExtRAM);
				fPrefs.putInt(KEY_EXT_RAM_SIZE, fExtRAMSize);
				fPrefs.putBoolean(KEY_USE_EXT_RAM_HEAP, fUseExtRAMforHeap);
				fPrefs.putBoolean(KEY_USE_EEPROM, fUseEEPROM);

				fPrefs.flush();
			}
			// Save the associated AVRDude properties
			fAVRDudeProperties.save();

		} catch (IllegalStateException ise) {
			// This should not happen, but just in case we ignore this unchecked
			// exception
			ise.printStackTrace();
		}
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(fDirty ? "*" : " ");
		sb.append("[");
		sb.append("mcuid=" + fMCUid);
		sb.append(", fcpu=" + fFCPU);
		sb.append("]");
		return sb.toString();
	}

}
