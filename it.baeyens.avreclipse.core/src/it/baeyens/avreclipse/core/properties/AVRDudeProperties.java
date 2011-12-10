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
 * $Id: AVRDudeProperties.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.properties;

import it.baeyens.avreclipse.core.avrdude.AVRDudeAction;
import it.baeyens.avreclipse.core.avrdude.AVRDudeActionFactory;
import it.baeyens.avreclipse.core.avrdude.BaseBytesProperties;
import it.baeyens.avreclipse.core.avrdude.FuseBytesProperties;
import it.baeyens.avreclipse.core.avrdude.LockbitBytesProperties;
import it.baeyens.avreclipse.core.avrdude.ProgrammerConfig;
import it.baeyens.avreclipse.core.avrdude.ProgrammerConfigManager;
import it.baeyens.avreclipse.core.toolinfo.fuses.FuseType;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;


/**
 * Container for all AVRDude specific properties of a project.
 * <p>
 * Upon instantiation, the properties are loaded from the given preference store. All changes are
 * local to the object until the {@link #save()} method is called.
 * </p>
 * <p>
 * <code>AVRDudeProperties</code> objects do not reflect changes made to other
 * <code>AVRDudeProperties</code> for the same Project/Configuration, so they should not be held
 * on to and be reloaded every time the current values are required.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 */
public class AVRDudeProperties {

	/** Reference to the parent properties */
	private final AVRProjectProperties	fParent;

	/** The currently selected <code>ProgrammerConfig</code> */
	private ProgrammerConfig			fProgrammer;

	/** ID of the currently selected <code>ProgrammerConfig</code> */
	private String						fProgrammerId;
	private static final String			KEY_PROGRAMMER				= "ProgrammerID";

	/** Current JTAG BitClock value. Contains a float value or be empty */
	private String						fBitclock;
	private static final String			KEY_BITCLOCK				= "Bitclock";
	private static final String			DEFAULT_BITCLOCK			= "";

	/** Current BitBanger bit change delay. Contains an int value or be empty */
	private String						fBitBangDelay;
	private static final String			KEY_BITBANGDELAY			= "BitBangDelay";
	private static final String			DEFAULT_BITBANGDELAY		= "";

	/** No Signature Check flag. <code>true</code> disables the signature check */
	private boolean						fNoSigCheck;
	private static final String			KEY_NOSIGCHECK				= "NoSigCheck";
	private static final boolean		DEFAULT_NOSIGCHECK			= false;

	/** No Verify flag. <code>true</code> disables the automatic verify */
	private boolean						fNoVerify;
	private static final String			KEY_NOVERIFY				= "NoVerify";
	private static final boolean		DEFAULT_NOVERIFY			= false;

	/** No Write Mode flag. <code>true</code> inhibits most write actions */
	private boolean						fNoWrite;
	private static final String			KEY_NOWRITE					= "NoWrite";
	private static final boolean		DEFAULT_NOWRITE				= false;

	/** No auto chip erase flag. <code>true</code> disables chip erase when writing flash memory. */
	private boolean						fNoChipErase;
	private static final String			KEY_NOCHIPERASE				= "NoChipErase";
	private static final boolean		DEFAULT_NOCHIPERASE			= false;

	/** Use Erase Cycle Counter flags. <code>true</code> enables the counter */
	private boolean						fUseCounter;
	private static final String			KEY_USECOUNTER				= "UseCounter";
	private static final boolean		DEFAULT_USECOUNTER			= false;

	/** Write Flash Image flag. <code>true</code> to upload an image file */
	private boolean						fWriteFlash;
	private static final String			KEY_WRITEFLASH				= "WriteFlash";
	private static final boolean		DEFAULT_WRITEFLASH			= true;

	/**
	 * Use Build Config Image flag. <code>true</code> to get the name of the flash image file from
	 * the current <code>IConfiguration</code>
	 */
	private boolean						fFlashFromConfig;
	private static final String			KEY_FLASHFROMCONFIG			= "FlashFromConfig";
	private static final boolean		DEFAULT_FLASHFROMCONFIG		= true;

	/**
	 * Name of the Flash image file. Only used when <code>fFlashFromConfig</code> is
	 * <code>false</code>
	 */
	private String						fFlashFile;
	private static final String			KEY_FLASHFILE				= "FlashFile";
	private static final String			DEFAULT_FLASHFILE			= "";

	/** Write EEPROM Image flag. <code>true</code> to upload an image file */
	private boolean						fWriteEEPROM;
	private static final String			KEY_WRITEEEPROM				= "WriteEEPROM";
	private static final boolean		DEFAULT_WRITEEEPROM			= false;

	/**
	 * Use Build Config Image flag. <code>true</code> to get the name of the eeprom image file
	 * from the current <code>IConfiguration</code>
	 */
	private boolean						fEEPROMFromConfig;
	private static final String			KEY_EEPROMFROMCONFIG		= "EEPROMFromConfig";
	private static final boolean		DEFAULT_EEPROMFROMCONFIG	= true;

	/**
	 * Name of the EEPROM image file. Only used when <code>fEEPROMFromConfig</code> is
	 * <code>false</code>
	 */
	private String						fEEPROMFile;
	private static final String			KEY_EEPROMFILE				= "EEPROMFile";
	private static final String			DEFAULT_EEPROMFILE			= "";

	/** The <code>FuseBytesProperties</code> with all fuse bytes related settings. */
	private FuseBytesProperties			fFuseBytes;
	private static final String			NODE_FUSES					= "Fuses";

	/** The <code>LockbitBytesProperties</code> with all lock byte related settings. */
	private LockbitBytesProperties		fLockbits;
	private static final String			NODE_LOCKS					= "Locks";

	/** Other avrdude options. Free text for avrdude options not directly supported by the plugin. */
	private String						fOtherOptions;
	private static final String			KEY_OTHEROPTIONS			= "OtherOptions";
	private static final String			DEFAULT_OTHEROPTIONS		= "";

	// Unused for now
	// private static final String NODE_CALIBRATION = "CalibrationBytes";
	// private CalibrationBytes fAVRDudeCalibration;

	/**
	 * The source/target Preferences for the properties or <code>null</code> if default properties
	 * are represented.
	 */
	private final Preferences			fPrefs;

	/** Flag if any properties have been changed */
	private boolean						fDirty;

	/**
	 * Create a new AVRDudeProperties object and load the properties from the Preferences.
	 * <p>
	 * If the given Preferences has no saved properties yet, the default values are used.
	 * </p>
	 * 
	 * @param prefs
	 *            <code>Preferences</code> to read the properties from.
	 * @param parent
	 *            Reference to the <code>AVRProjectProperties</code> parent object.
	 */
	public AVRDudeProperties(Preferences prefs, AVRProjectProperties parent) {
		fPrefs = prefs;
		fParent = parent;
		loadData();
	}

	/**
	 * Copy constructor.
	 * <p>
	 * Create a new AVRDudeProperties object and copy the values from the given AVRDudeProperties
	 * object.
	 * </p>
	 * <p>
	 * All values from the source are copied, except for the source Preferences and the Parent.
	 * </p>
	 * 
	 * @param prefs
	 *            <code>Preferences</code> to read the properties from.
	 * @param parent
	 *            Reference to the <code>AVRProjectProperties</code> parent object.
	 * @param source
	 *            <code>AVRDudeProperties</code> object to copy.
	 */
	public AVRDudeProperties(Preferences prefs, AVRProjectProperties parent,
			AVRDudeProperties source) {
		fParent = parent;
		fPrefs = prefs;

		fProgrammer = source.fProgrammer;
		fProgrammerId = source.fProgrammerId;
		fBitclock = source.fBitclock;
		fBitBangDelay = source.fBitBangDelay;
		fNoSigCheck = source.fNoSigCheck;
		fNoVerify = source.fNoVerify;
		fNoWrite = source.fNoWrite;
		fNoChipErase = source.fNoChipErase;
		fUseCounter = source.fUseCounter;

		fWriteFlash = source.fWriteFlash;
		fFlashFromConfig = source.fFlashFromConfig;
		fFlashFile = source.fFlashFile;

		fWriteEEPROM = source.fWriteEEPROM;
		fEEPROMFromConfig = source.fEEPROMFromConfig;
		fEEPROMFile = source.fEEPROMFile;

		fFuseBytes = new FuseBytesProperties(prefs.node(NODE_FUSES), this, source.fFuseBytes);

		fLockbits = new LockbitBytesProperties(prefs.node(NODE_LOCKS), this, source.fLockbits);

		fOtherOptions = source.fOtherOptions;

		// fAVRDudeCalibration = new
		// CalibrationBytes(source.fAVRDudeCalibration);

		fDirty = source.fDirty;
	}

	/**
	 * Get a reference to the parent properties.
	 * 
	 * @return <code>AVRProjectProperties</code>
	 */
	public AVRProjectProperties getParent() {
		return fParent;
	}

	public ProgrammerConfig getProgrammer() {
		if (fProgrammer == null) {
			return ProgrammerConfigManager.getDefault().getConfig(fProgrammerId);
		}
		return fProgrammer;

	}

	public void setProgrammer(ProgrammerConfig progcfg) {
		if (!progcfg.equals(fProgrammer)) {
			fProgrammer = progcfg;
			fProgrammerId = progcfg.getId();
			fDirty = true;
		}
	}

	public String getProgrammerId() {
		return fProgrammerId;
	}

	public void setProgrammerId(String programmerid) {
		if (!fProgrammerId.equals(programmerid)) {
			fProgrammerId = programmerid;
			fProgrammer = null;
			fDirty = true;
		}
	}

	public String getBitclock() {
		return fBitclock;
	}

	public void setBitclock(String bitclock) {
		if (!fBitclock.equals(bitclock)) {
			fBitclock = bitclock;
			fDirty = true;
		}
	}

	public String getBitBangDelay() {
		return fBitBangDelay;
	}

	public void setBitBangDelay(String bitbangdelay) {
		if (!fBitBangDelay.equals(bitbangdelay)) {
			fBitBangDelay = bitbangdelay;
			fDirty = true;
		}
	}

	public boolean getNoSigCheck() {
		return fNoSigCheck;
	}

	public void setNoSigCheck(boolean nosigcheck) {
		if (fNoSigCheck != nosigcheck) {
			fNoSigCheck = nosigcheck;
			fDirty = true;
		}
	}

	public boolean getNoVerify() {
		return fNoVerify;
	}

	public void setNoVerify(boolean noverify) {
		if (fNoVerify != noverify) {
			fNoVerify = noverify;
			fDirty = true;
		}
	}

	public boolean getNoWrite() {
		return fNoWrite;
	}

	public void setNoWrite(boolean nowrite) {
		if (fNoWrite != nowrite) {
			fNoWrite = nowrite;
			fDirty = true;
		}
	}

	public boolean getNoChipErase() {
		return fNoChipErase;
	}

	public void setNoChipErase(boolean nochiperase) {
		if (fNoChipErase != nochiperase) {
			fNoChipErase = nochiperase;
			fDirty = true;
		}
	}

	public boolean getUseCounter() {
		return fUseCounter;
	}

	public void setUseCounter(boolean usecounter) {
		if (fUseCounter != usecounter) {
			fUseCounter = usecounter;
			fDirty = true;
		}
	}

	public boolean getWriteFlash() {
		return fWriteFlash;
	}

	public void setWriteFlash(boolean enabled) {
		if (fWriteFlash != enabled) {
			fWriteFlash = enabled;
			fDirty = true;
		}
	}

	public boolean getFlashFromConfig() {
		return fFlashFromConfig;
	}

	public void setFlashFromConfig(boolean useconfig) {
		if (fFlashFromConfig != useconfig) {
			fFlashFromConfig = useconfig;
			fDirty = true;
		}
	}

	public String getFlashFile() {
		return fFlashFile;
	}

	public void setFlashFile(String filename) {
		if (!fFlashFile.equals(filename)) {
			fFlashFile = filename;
			fDirty = true;
		}
	}

	public boolean getWriteEEPROM() {
		return fWriteEEPROM;
	}

	public void setWriteEEPROM(boolean enabled) {
		if (fWriteEEPROM != enabled) {
			fWriteEEPROM = enabled;
			fDirty = true;
		}
	}

	public boolean getEEPROMFromConfig() {
		return fEEPROMFromConfig;
	}

	public void setEEPROMFromConfig(boolean useconfig) {
		if (fEEPROMFromConfig != useconfig) {
			fEEPROMFromConfig = useconfig;
			fDirty = true;
		}
	}

	public String getEEPROMFile() {
		return fEEPROMFile;
	}

	public void setEEPROMFile(String filename) {
		if (!fEEPROMFile.equals(filename)) {
			fEEPROMFile = filename;
			fDirty = true;
		}
	}

	/**
	 * Get the Fuse byte properties container object.
	 * <p>
	 * The <code>IConfiguration</code> parameter is only used to resolve filenames for optional
	 * fuses files. It can be <code>null</code>, but then the filename of a fuse file will be
	 * used as is and no macro expansion takes place.
	 * </p>
	 * 
	 * @param buildconfig
	 *            The current build configuration
	 * @return The <code>FuseByteProperties</code> container object.
	 */
	public FuseBytesProperties getFuseBytes(IConfiguration buildconfig) {
		// informing the FuseByteProperties about the current build configuration here is somewhat
		// kludgy, but this seems to be the only point where it can be set.
		fFuseBytes.setBuildConfig(buildconfig);
		return fFuseBytes;
	}

	/**
	 * Get the Lockbits byte properties container object.
	 * <p>
	 * The <code>IConfiguration</code> parameter is only used to resolve filenames for optional
	 * locks files. It can be <code>null</code>, but then the filename of a locks file will be
	 * used as is and no macro expansion takes place.
	 * </p>
	 * 
	 * @param buildconfig
	 *            The current build configuration
	 * @return The <code>FuseByteProperties</code> container object.
	 */
	public LockbitBytesProperties getLockbitBytes(IConfiguration buildconfig) {
		// informing the LockbitByteProperties about the current build configuration here is
		// somewhat
		// kludgy, but this seems to be the only point where it can be set.
		fLockbits.setBuildConfig(buildconfig);
		return fLockbits;
	}

	public BaseBytesProperties getBytesProperties(FuseType type, IConfiguration buildconfig) {
		switch (type) {
			case FUSE:
				return getFuseBytes(buildconfig);
			case LOCKBITS:
				return getLockbitBytes(buildconfig);
		}

		// there are no other FuseTypes than those two, so we never come here
		return null;
	}

	public String getOtherOptions() {
		return fOtherOptions;
	}

	public void setOtherOptions(String otheroptions) {
		if (!fOtherOptions.equals(otheroptions)) {
			fOtherOptions = otheroptions;
			fDirty = true;
		}
	}

	/**
	 * Gets the avrdude command arguments as defined by the properties.
	 * 
	 * @return <code>List&lt;String&gt;</code> with the avrdude options, one per list entry.
	 */
	public List<String> getArguments() {
		List<String> arguments = new ArrayList<String>();

		arguments.add("-p" + fParent.getMCUId());

		// Add the options from the programmer configuration
		ProgrammerConfig progcfg = getProgrammer();
		if (progcfg != null) {
			arguments.addAll(progcfg.getArguments());
		}

		// add the bitclock value
		if (fBitclock.length() != 0) {
			arguments.add("-B" + fBitclock);
		}

		// add the BitBang delay value
		if (fBitBangDelay.length() != 0) {
			arguments.add("-i" + fBitBangDelay);
		}

		// add the No Signature Check flag
		if (fNoSigCheck) {
			arguments.add("-F");
		}

		// add the Simulation / no-write flag
		if (fNoWrite) {
			arguments.add("-n");
			// Add the "no Verify" flag to suppress nuisance error messages
			// (if not already set)
			if (!fNoVerify)
				arguments.add("-V");
		}

		// add the No Verify flag
		if (fNoVerify) {
			arguments.add("-V");
		}

		// ad the No Chip Erase flag
		if (fNoChipErase) {
			arguments.add("-D");
		}

		// add the Use Erase Cycle Counter flag
		if (fUseCounter) {
			arguments.add("-y");
		}

		// Disable safe mode when Fuses are written, otherwise the fuses will be
		// restored after the write.
		// Safemode is *not* disabled in Simulation mode, because even with the
		// no-write flag, fuse bytes may change values accidentally and should
		// be restored.
		if (fFuseBytes.getWrite() && !fNoWrite) {
			arguments.add("-u");
		}

		// add the other options field
		if (fOtherOptions.length() > 0) {
			arguments.add(fOtherOptions);
		}

		return arguments;
	}

	/**
	 * Get the list of avrdude action options according to the current properties.
	 * <p>
	 * Currently the following actions are supported:
	 * <ul>
	 * <li>write flash image</li>
	 * <li>write eeprom image</li>
	 * <li>write fuse bytes</li>
	 * <li>write lockbits</li>
	 * </ul>
	 * Only for sections enabled with the <code>setWriteXXXX(true)</code> method will avrdude
	 * actions be created.
	 * </p>
	 * <p>
	 * Macros in the filenames for the flash and eeprom image files are not resolved. Use
	 * {@link #getActionArguments(IConfiguration, boolean)} to get the arguments with all macros
	 * resolved.
	 * </p>
	 * <p>
	 * This is a convenience method for <code>getArguments(buildcfg, true)</code>
	 * </p>
	 * 
	 * @return <code>List&lt;String&gt;</code> with avrdude action options.
	 */
	public List<String> getActionArguments(IConfiguration buildcfg) {
		return getActionArguments(buildcfg, false);
	}

	public List<String> getActionArguments(IConfiguration buildcfg, boolean resolve) {
		List<String> arguments = new ArrayList<String>();

		AVRDudeAction action = null;

		if (fWriteFlash) {
			if (fFlashFromConfig) {
				action = AVRDudeActionFactory.writeFlashAction(buildcfg);
			} else {
				action = AVRDudeActionFactory.writeFlashAction(fFlashFile);
			}
			if (action != null) {
				String argument;
				if (resolve) {
					argument = action.getArgument(buildcfg);
				} else {
					argument = action.getArgument();
				}
				arguments.add(argument);
			}
		}

		if (fWriteEEPROM) {
			if (fEEPROMFromConfig) {
				action = AVRDudeActionFactory.writeEEPROMAction(buildcfg);
			} else {
				action = AVRDudeActionFactory.writeEEPROMAction(fEEPROMFile);
			}
			if (action != null) {
				String argument;
				if (resolve) {
					argument = action.getArgument(buildcfg);
				} else {
					argument = action.getArgument();
				}
				arguments.add(argument);
			}
		}

		if (fFuseBytes.getWrite()) {
			arguments.addAll(fFuseBytes.getArguments(fParent.getMCUId()));
		}

		if (fLockbits.getWrite()) {
			arguments.addAll(fLockbits.getArguments(fParent.getMCUId()));
		}

		return arguments;
	}

	/**
	 * Load all options from the preferences.
	 */
	protected void loadData() {
		fProgrammerId = fPrefs.get(KEY_PROGRAMMER, "");
		fBitclock = fPrefs.get(KEY_BITCLOCK, DEFAULT_BITCLOCK);
		fBitBangDelay = fPrefs.get(KEY_BITBANGDELAY, DEFAULT_BITBANGDELAY);
		fNoSigCheck = fPrefs.getBoolean(KEY_NOSIGCHECK, DEFAULT_NOSIGCHECK);
		fNoVerify = fPrefs.getBoolean(KEY_NOVERIFY, DEFAULT_NOVERIFY);
		fNoWrite = fPrefs.getBoolean(KEY_NOWRITE, DEFAULT_NOWRITE);
		fNoChipErase = fPrefs.getBoolean(KEY_NOCHIPERASE, DEFAULT_NOCHIPERASE);
		fUseCounter = fPrefs.getBoolean(KEY_USECOUNTER, DEFAULT_USECOUNTER);

		fWriteFlash = fPrefs.getBoolean(KEY_WRITEFLASH, DEFAULT_WRITEFLASH);
		fFlashFromConfig = fPrefs.getBoolean(KEY_FLASHFROMCONFIG, DEFAULT_FLASHFROMCONFIG);
		fFlashFile = fPrefs.get(KEY_FLASHFILE, DEFAULT_FLASHFILE);

		fWriteEEPROM = fPrefs.getBoolean(KEY_WRITEEEPROM, DEFAULT_WRITEEEPROM);
		fEEPROMFromConfig = fPrefs.getBoolean(KEY_EEPROMFROMCONFIG, DEFAULT_EEPROMFROMCONFIG);
		fEEPROMFile = fPrefs.get(KEY_EEPROMFILE, DEFAULT_EEPROMFILE);

		fFuseBytes = new FuseBytesProperties(fPrefs.node(NODE_FUSES), this);

		fLockbits = new LockbitBytesProperties(fPrefs.node(NODE_LOCKS), this);

		fOtherOptions = fPrefs.get(KEY_OTHEROPTIONS, DEFAULT_OTHEROPTIONS);

		// fAVRDudeCalibration = new
		// CalibrationBytes(fPrefs.node(NODE_CALIBRATION));
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
				fDirty = false;

				fPrefs.put(KEY_PROGRAMMER, fProgrammerId);
				fPrefs.put(KEY_BITCLOCK, fBitclock);
				fPrefs.put(KEY_BITBANGDELAY, fBitBangDelay);
				fPrefs.putBoolean(KEY_NOSIGCHECK, fNoSigCheck);
				fPrefs.putBoolean(KEY_NOVERIFY, fNoVerify);
				fPrefs.putBoolean(KEY_NOWRITE, fNoWrite);
				fPrefs.putBoolean(KEY_NOCHIPERASE, fNoChipErase);
				fPrefs.putBoolean(KEY_USECOUNTER, fUseCounter);

				fPrefs.putBoolean(KEY_WRITEFLASH, fWriteFlash);
				fPrefs.putBoolean(KEY_FLASHFROMCONFIG, fFlashFromConfig);
				fPrefs.put(KEY_FLASHFILE, fFlashFile);

				fPrefs.putBoolean(KEY_WRITEEEPROM, fWriteEEPROM);
				fPrefs.putBoolean(KEY_EEPROMFROMCONFIG, fEEPROMFromConfig);
				fPrefs.put(KEY_EEPROMFILE, fEEPROMFile);

				fPrefs.put(KEY_OTHEROPTIONS, fOtherOptions);

				fPrefs.flush();

				if (fProgrammer != null) {
					ProgrammerConfigManager.getDefault().saveConfig(fProgrammer);
				}
			}
			fFuseBytes.save();
			fLockbits.save();
			// fAVRDudeCalibration.save();

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
		sb.append("ProgrammerID=" + fProgrammerId);
		sb.append("]");
		return sb.toString();
	}

}
