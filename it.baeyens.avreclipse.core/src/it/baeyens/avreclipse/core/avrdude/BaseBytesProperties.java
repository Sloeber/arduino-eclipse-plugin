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
 * $Id: BaseBytesProperties.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.avrdude;


import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.avreclipse.core.properties.AVRDudeProperties;
import it.baeyens.avreclipse.core.toolinfo.fuses.ByteValues;
import it.baeyens.avreclipse.core.toolinfo.fuses.ByteValuesFactory;
import it.baeyens.avreclipse.core.toolinfo.fuses.ConversionResults;
import it.baeyens.avreclipse.core.toolinfo.fuses.FuseType;
import it.baeyens.avreclipse.mbs.BuildMacro;

import java.io.FileNotFoundException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;


/**
 * Storage independent container for the Fuse and Lockbits Byte values.
 * <p>
 * This class is the bridge between the plugin property system and the {@link ByteValues} to
 * actually hold the data.
 * </p>
 * <p>
 * This class has two modes. Depending on the {@link #fUseFile} flag, it will either read the fuse
 * values from a supplied file or immediate values stored in a byte values object. The mode is
 * selected by the user in the Properties user interface.
 * </p>
 * <p>
 * This class can be used either standalone or as part of the AVRProjectProperties structure.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public abstract class BaseBytesProperties {

	public final static int			FILE_NOT_FOUND				= 200;
	public final static int			FILE_MCU_PROPERTY_MISSING	= 201;
	public final static int			FILE_WRONG_TYPE				= 202;
	public final static int			FILE_INVALID_FILENAME		= 203;
	public final static int			FILE_EMPTY_FILENAME			= 204;

	/** The MCU id for which the current fuse byte values are valid */
	private String					fMCUid;
	private static final String		KEY_MCUID					= "MCUid";

	/**
	 * Write flag
	 * <p>
	 * If <code>true</code>, the byte values are written to the target device when avrdude is
	 * executed.
	 * </p>
	 */
	private boolean					fWriteFlag;
	private final static String		KEY_WRITEFLAG				= "Write";
	private final static boolean	DEFAULT_WRITEFLAG			= false;

	/**
	 * Use file flag.
	 * <p>
	 * If <code>true</code>, the byte values are read from a file.
	 * </p>
	 * <p>
	 * If <code>false</code> the values from {@link #fByteValues} are used.
	 * </p>
	 */
	private boolean					fUseFile;
	private final static String		KEY_USEFILE					= "UseFile";
	private final static boolean	DEFAULT_USEFILE				= false;

	/**
	 * The name of the file.
	 * <p>
	 * This is used when the {@link #fUseFile} flag is <code>true</code>.
	 * </p>
	 * <p>
	 * The name can contain macros. They can be resolved by the caller or with the
	 * {@link #getFileNameResolved(IConfiguration)} method.
	 * </p>
	 */
	private String					fFileName;
	private final static String		KEY_FILENAME				= "FileName";
	private final static String		DEFAULT_FILENAME			= "";
	private ByteValues				fFileByteValues				= null;

	/**
	 * The current byte values.
	 * <p>
	 * This is used when the {@link #fUseFile} flag is <code>false</code>.
	 * </p>
	 */
	private ByteValues				fByteValues;
	private final static String		KEY_BYTEVALUES				= "ByteValues";
	private final static String		SEPARATOR					= ":";

	/**
	 * The <code>Preferences</code> used to read / save the current properties.
	 * 
	 */
	private final Preferences		fPrefs;

	/**
	 * The Parent <code>AVRDudeProperties</code>. Can be <code>null</code> if this class is used in
	 * stand alone mode.
	 * 
	 */
	private final AVRDudeProperties	fParent;

	/** Build configuration used to resolve filenames. */
	private IConfiguration			fBuildConfig				= null;

	/** <code>true</code> if the properties have been modified and need saving. */
	private boolean					fDirty						= false;

	/**
	 * Create a new FuseBytesProperties object and load the properties from the Preferences.
	 * <p>
	 * If the given Preferences has no saved properties yet, the default values are used.
	 * </p>
	 * 
	 * @param prefs
	 *            <code>Preferences</code> to read the properties from.
	 * @param parent
	 *            Reference to the <code>AVRDudeProperties</code> parent object.
	 */
	protected BaseBytesProperties(Preferences prefs, AVRDudeProperties parent) {
		fPrefs = prefs;
		fParent = parent;
		fByteValues = new ByteValues(getType(), parent.getParent().getMCUId());

		load();
	}

	/**
	 * Cloning constructor.
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
	public BaseBytesProperties(Preferences prefs, AVRDudeProperties parent,
			BaseBytesProperties source) {
		fPrefs = prefs;
		fParent = parent;

		fMCUid = source.fMCUid;

		fWriteFlag = source.fWriteFlag;
		fUseFile = source.fUseFile;
		fFileName = source.fFileName;
		fByteValues = new ByteValues(source.fByteValues);
	}

	/**
	 * Hook method for subclasses to supply the {@link FuseType} for the properties.
	 * 
	 * @return Either <code>FuseType.FUSE</code> or <code>FuseType.LOCKBITS</code>
	 */
	protected abstract FuseType getType();

	/**
	 * Get the MCU id value for which this object is valid.
	 * 
	 * @return <code>String</code> with an mcu id. May be <code>null</code> if a non-existing file
	 *         has been set as source.
	 */
	public String getMCUId() {

		if (fUseFile) {
			try {
				return getByteValuesFromFile().getMCUId();
			} catch (CoreException ce) {
				// if the file does not exist or can not be opened we can not return a valid MCU
				return null;
			}
		}

		return fMCUid;
	}

	/**
	 * Tells this class that the current byte values are valid for the given MCU.
	 * <p>
	 * Use this method with care, as there will be no checks if the current values actually make
	 * sense for the new MCU type.
	 * </p>
	 * <p>
	 * The new setting is only valid for the internally stored values. If a file is used it is not
	 * affected and a call to {@link #getMCUId()} will return the mcu from the file, not this one.
	 * </p>
	 * 
	 * @param mcuid
	 */
	public void setMCUId(String mcuid) {

		if (!fMCUid.equals(mcuid)) {
			fMCUid = mcuid;

			// copy the old byte values to a new ByteValues Object for the given MCU
			ByteValues newByteValues = new ByteValues(getType(), mcuid);
			newByteValues.setValues(fByteValues.getValues());
			fByteValues = newByteValues;

			fDirty = true;
		}

	}

	/**
	 * Get the "write to target MCU" flag.
	 * 
	 * @return <code>true</code> if the byte values should be written to the target device when
	 *         avrdude is executed.
	 */
	public boolean getWrite() {
		return fWriteFlag;
	}

	/**
	 * Set the "write to target MCU" flag.
	 * 
	 * @param enable
	 *            <code>true</code> to enable writing the bytes managed by this class when avrdude
	 *            is executed.
	 */
	public void setWrite(boolean enable) {
		if (fWriteFlag != enable) {
			fWriteFlag = enable;
			fDirty = true;
		}
	}

	/**
	 * Get the current value of the "Use File" flag.
	 * 
	 * @see #setFileName(String)
	 * @see #getValue(int)
	 * @see #getValues()
	 * 
	 * @return <code>true</code> if the byte values are taken from a file, <code>false</code> if the
	 *         values stored in this object are used.
	 */
	public boolean getUseFile() {
		return fUseFile;
	}

	/**
	 * Set the value of the "Use File" flag.
	 * 
	 * @see #setFileName(String)
	 * @see #getValue(int)
	 * @see #getValues()
	 * 
	 * 
	 * @param usefile
	 *            <code>true</code> if the fuse values should be read from the file,
	 *            <code>false</code> if the values stored in this object are used.
	 */
	public void setUseFile(boolean usefile) {
		if (fUseFile != usefile) {
			fUseFile = usefile;
			fDirty = true;
		}
	}

	/**
	 * Get the current name of the file with all macros resolved.
	 * <p>
	 * Note: The returned path may still be OS independent and needs to be converted to an OS
	 * specific path (e.g. with <code>new Path(resolvedname).toOSString()</code>
	 * </p>
	 * <p>
	 * To resolve any macros this method needs an <code>IConfiguration</code> for the macro context.
	 * This needs to be set with the {@link #setBuildConfig(IConfiguration)} method. If no build
	 * configuration has been set, this method will return the filename unresolved.
	 * </p>
	 * 
	 * @return <code>String</code> with the resolved filename. May be empty and may not point to an
	 *         actual or valid file.
	 */
	public String getFileNameResolved() {
		if (fBuildConfig != null) {
			return BuildMacro.resolveMacros(fBuildConfig, fFileName);
		}
		return fFileName;
	}

	/**
	 * Sets the current build configuration.
	 * <p>
	 * This is only used to expand macros in the filename of an optional fuse/locks file. If it is
	 * <code>null</code> filenames can not be resolved and used as is.
	 * </p>
	 * 
	 * @param config
	 *            The current build configuration.
	 */
	public void setBuildConfig(IConfiguration config) {
		fBuildConfig = config;
	}

	/**
	 * Get the current name of the file.
	 * <p>
	 * The returned string may still contain macros.
	 * </p>
	 * 
	 * @return <code>String</code> with the name of the file. May be empty and may not point to an
	 *         actual or valid file.
	 */
	public String getFileName() {
		return fFileName;
	}

	/**
	 * Set the name of the file.
	 * <p>
	 * The given filename is stored as-is. There are no checks if the file is valid or even exists.
	 * </p>
	 * 
	 * @param fusesfile
	 *            <code>String</code> with the name of a file.
	 */
	public void setFileName(String filename) {
		if (!fFileName.equals(filename)) {
			fFileName = filename;
			fFileByteValues = null;
			fDirty = true;
		}
	}

	/**
	 * Get all current byte values as a <code>ByteValues</code> object.
	 * <p>
	 * Get all bytes according to the current setting either from a file or from the object storage.
	 * <br>
	 * The returned <code>ByteValues</code> object is a copy of the internal values and any
	 * modifications are not reflected on the values in this object.
	 * </p>
	 * <p>
	 * To modify the values the use of the {@link #setValue(int, int)}, {@link #setValues(int[])}
	 * and {@link #setByteValues(ByteValues)} is required so this class can track any modifications
	 * and set the dirty flag as required.
	 * </p>
	 * <p>
	 * All values are either a valid bytes (0 - 255) or <code>-1</code> if no value was set.
	 * </p>
	 * <
	 * 
	 * @return
	 */
	public ByteValues getByteValues() {
		if (fUseFile) {
			try {
				return getByteValuesFromFile();
			} catch (CoreException ce) {
				return null;
			}
		}
		return new ByteValues(fByteValues);
	}

	public ByteValues getByteValuesFromFile() throws CoreException {

		if (fFileByteValues == null) {
			// First get the IFile of the file and check that it exists
			String rawfilename = getFileName();
			if (rawfilename == null || rawfilename.length() == 0) {
				String message = "Empty Filename";
				IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID, FILE_EMPTY_FILENAME,
						message, null);
				throw new CoreException(status);
			}
			String filename = getFileNameResolved();
			if (filename == null || filename.length() == 0) {
				String message = MessageFormat.format("Invalid filename [{0}]", getFileName());
				IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,
						FILE_INVALID_FILENAME, message, new FileNotFoundException(filename));
				throw new CoreException(status);
			}
			IPath location = new Path(filename);
			IFile file = getFileFromLocation(location);
			if (file == null || !file.exists()) {
				String message = MessageFormat
						.format("File not found [{0}]", location.toOSString());
				IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID, FILE_NOT_FOUND,
						message, new FileNotFoundException(file != null ? file.getFullPath()
								.toOSString() : null));
				throw new CoreException(status);
			}

			// then use the FuseFileDocumentProvider to get a ByteValues object for the file.
			// The input file is immediately disconnected, because this class does not have a
			// dispose method where the the disconnection could take place.
			// This means that any changes to the file are not synchronized. Therefore the
			// ByteValues object returned by this method should not be used for prolonged periods.
			fFileByteValues = ByteValuesFactory.createByteValues(file);

			// If the fFileByteValues are null, then the file could not be read.
			// probably the 'MCU' Property is missing.
			if (fFileByteValues == null) {
				String message = MessageFormat.format(
						"{0} is not a valid {1} file (probably the MCU=xxxx property is missing)",
						file.getFullPath(), getType());
				IStatus status = new Status(Status.ERROR,ArduinoConst.CORE_PLUGIN_ID,
						FILE_MCU_PROPERTY_MISSING, message, null);
				throw new CoreException(status);
			}

			// Check if the file actually has the right type.
			if (!getType().equals(fFileByteValues.getType())) {
				// No! Discard the object and throw an Exception
				String message = MessageFormat.format(
						"{0} is a {1} file, but expected a {2} file.", file.getFullPath(),
						fFileByteValues.getType(), getType());
				IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID, FILE_WRONG_TYPE,
						message, null);
				fFileByteValues = null;
				throw new CoreException(status);
			}
		}

		return new ByteValues(fFileByteValues);

	}

	/**
	 * Sets the current byte values.
	 * <p>
	 * This method copies the given <code>ByteValues</code>, so that it cannot be changed without
	 * going through the methods of this class.
	 * </p>
	 * <p>
	 * The MCU Id of this class is set to the one of the given <code>ByteValues</code>
	 * 
	 * @param newvalues
	 *            The ByteValues object to copy from.
	 * @throws IllegalArgumentException
	 *             if the type of the new byte values (FUSE or LOCKS) does not match the current
	 *             setting.
	 */
	public void setByteValues(ByteValues newvalues) {
		if (fUseFile) {
			// TODO: handle files
			return;
		}

		if (fByteValues.getType() != newvalues.getType()) {
			throw new IllegalArgumentException("Cannot set a " + newvalues.getType().toString()
					+ " ByteValues object");
		}

		fByteValues = new ByteValues(newvalues);
		fMCUid = newvalues.getMCUId();

		fDirty = true;
		return;
	}

	public void setDefaultValues() {
		fByteValues.setDefaultValues();
		fDirty = true;
	}

	/**
	 * Get all current byte values as an array of <code>int</code>.
	 * <p>
	 * Get all bytes according to the current setting either from a file or from the object storage.
	 * </p>
	 * <p>
	 * All values are either a valid bytes (0 - 255) or <code>-1</code> if no value was set.
	 * </p>
	 * 
	 * @return Array of <code>int</code> with all byte values.
	 */
	public int[] getValues() {
		if (fUseFile) {
			return getValuesFromFile();
		}
		return getValuesFromImmediate();
	}

	/**
	 * Get all current byte values from the file.
	 * <p>
	 * All values are either a valid bytes (0 - 255) or <code>-1</code> if no value was set.
	 * </p>
	 * 
	 * @return Array of <code>int</code> with all byte values.
	 */
	public int[] getValuesFromFile() {
		try {
			ByteValues fileByteValues = getByteValuesFromFile();
			return fileByteValues.getValues();
		} catch (CoreException e) {
			// File not found / not readable
			return new int[] {};
		}
	}

	/**
	 * Get all current byte values stored in the object.
	 * <p>
	 * All values are either a valid bytes (0 - 255) or <code>-1</code> if no value was set.
	 * </p>
	 * 
	 * @return Array of <code>int</code> with all byte values.
	 */
	public int[] getValuesFromImmediate() {
		return fByteValues.getValues();
	}

	/**
	 * Sets the values of all bytes in the object.
	 * <p>
	 * Even with the "Use File" flag set, this method will set the values stored in the object.
	 * </p>
	 * 
	 * @see #setByteValue(int, int)
	 * 
	 * @param values
	 *            Array of <code>int</code> with the new values.
	 * @throws IllegalArgumentException
	 *             if any value in the array is not a byte value or not <code>-1</code>
	 */
	public void setValues(int[] values) {
		// While values[].length should be equal to the length of the internal
		// field (and equal to MAX_FUSEBYTES), we use this to avoid any
		// OutOfBoundExceptions
		int min = Math.min(values.length, fByteValues.getByteCount());

		// Set all individual values. setFuseValue() will take care of setting
		// the dirty flag as needed.
		for (int i = 0; i < min; i++) {
			setValue(i, values[i]);
		}
	}

	/**
	 * Get a single byte value.
	 * <p>
	 * Get the byte according to the current setting either from a file or from the object storage.
	 * </p>
	 * 
	 * @param index
	 *            The byte to read. Must be between 0 and <code>getMaxBytes() - 1</code>
	 * @return <code>int</code> with the byte value or <code>-1</code> if the value was not set or
	 *         the index is out of bounds.
	 */
	public int getValue(int index) {
		if (!(0 <= index && index < fByteValues.getByteCount())) {
			return -1;
		}

		// getByteValues() will take care of the "use file" flag and
		// return the relevant byte values.
		int[] values = getValues();
		return values[index];
	}

	/**
	 * Set a single byte value.
	 * <p>
	 * The value is always written to the object storage, regardless of the "Use File" flag.
	 * </p>
	 * 
	 * @param index
	 *            The byte to set. Must be between 0 and <code>getByteCount() - 1</code>, otherwise
	 *            the value is ignored.
	 * @param value
	 *            <code>int</code> with the byte value (0-255) or <code>-1</code> to unset the
	 *            value.
	 * @throws IllegalArgumentException
	 *             if the the value is out of range (-1 to 255)
	 */
	public void setValue(int index, int value) {
		if (!(0 <= index && index < fByteValues.getByteCount())) {
			return;
		}
		if (!(-1 <= value && value <= 255)) {
			throw new IllegalArgumentException("invalid value:" + index
					+ " (must be between 0 and 255)");
		}

		if (fByteValues.getValue(index) != value) {
			fByteValues.setValue(index, value);
			fDirty = true;
		}
	}

	/**
	 * Clears all values.
	 * <p>
	 * This method will set the value of all bytes to <code>-1</code>
	 * </p>
	 */
	public void clearValues() {
		fByteValues.clearValues();
		fDirty = true;
	}

	/**
	 * Copies the byte values from the file to the object storage.
	 */
	public void syncFromFile() {
		try {
			ByteValues source = getByteValuesFromFile();
			setByteValues(source);
		} catch (CoreException ce) {
			// do nothing if the file does not exist.
		}
	}

	/**
	 * Get the list of avrdude arguments required to write all bytes.
	 * <p>
	 * Note: This method does <strong>not</strong> set the "-u" flag to disable the safemode. It is
	 * up to the caller to add this flag. If the "disable safemode" flag is not set, avrdude will
	 * restore the previous fusebyte values after the new values have been written.
	 * </p>
	 * 
	 * @return <code>List&lt;String&gt;</code> with avrdude action options.
	 */
	public abstract List<String> getArguments(String mcuid);

	/**
	 * Load the properties from the Preferences.
	 * <p>
	 * The <code>Preferences</code> object used is set in the constructor of this class.
	 * </p>
	 * 
	 */
	private void load() {

		// Get the MCU id of the parent TargetProperties
		// This is used as the default mcuid for this bytes object.
		String parentmcuid = fParent.getParent().getMCUId();

		fMCUid = fPrefs.get(KEY_MCUID, parentmcuid);
		fWriteFlag = fPrefs.getBoolean(KEY_WRITEFLAG, DEFAULT_WRITEFLAG);
		fUseFile = fPrefs.getBoolean(KEY_USEFILE, DEFAULT_USEFILE);
		fFileName = fPrefs.get(KEY_FILENAME, DEFAULT_FILENAME);

		String fusevaluestring = fPrefs.get(KEY_BYTEVALUES, "");

		// Check if the mcu id is different than the parent. In that case we
		// need a new ByteValues object
		if (!(fMCUid.equals(parentmcuid))) {
			// copy the old byte values to a new ByteValues Object for the given MCU
			fByteValues = new ByteValues(getType(), fMCUid);
		}

		// Clear the old values
		fByteValues.clearValues();

		// split the values
		String[] values = fusevaluestring.split(SEPARATOR);
		int count = Math.min(values.length, fByteValues.getByteCount());
		for (int i = 0; i < count; i++) {
			String value = values[i];
			if (value.length() != 0) {
				fByteValues.setValue(i, Integer.parseInt(values[i]));
			}
		}

		fDirty = false;

	}

	/**
	 * Save the current property values to the Preferences.
	 * <p>
	 * The <code>Preferences</code> object used is set in the constructor of this class.
	 * </p>
	 * 
	 * @throws BackingStoreException
	 */
	public void save() throws BackingStoreException {
		if (fDirty) {
			fPrefs.put(KEY_MCUID, fMCUid);
			fPrefs.putBoolean(KEY_WRITEFLAG, fWriteFlag);
			fPrefs.putBoolean(KEY_USEFILE, fUseFile);
			String filename = fFileName != null ? fFileName : "";
			fPrefs.put(KEY_FILENAME, filename);

			// convert the values to a single String
			StringBuilder sb = new StringBuilder(20);
			for (int i = 0; i < fByteValues.getByteCount(); i++) {
				if (i > 0)
					sb.append(SEPARATOR);
				sb.append(fByteValues.getValue(i));
			}
			fPrefs.put(KEY_BYTEVALUES, sb.toString());

			fPrefs.flush();
		}

		fDirty = false;
	}

	/**
	 * @return <code>true</code> if the object has unsaved changes
	 */
	public boolean isDirty() {
		return fDirty;
	}

	/**
	 * Test if this Object is valid for the given MCU.
	 * 
	 * @return <code>true</code> if the current byte values (either immediate or from a file) are
	 *         valid for the given MCU id.
	 * 
	 */
	public boolean isCompatibleWith(String mcuid) {
		if (fUseFile) {
			try {
				ByteValues fileByteValues = getByteValuesFromFile();
				return fileByteValues.isCompatibleWith(mcuid);
			} catch (CoreException e) {
				// Can't read the file
				return false;
			}

		}

		return fByteValues.isCompatibleWith(mcuid);
	}

	public void convertTo(String mcuid, ConversionResults results) {

		if (fUseFile) {
			// TODO: Check against the file
			return;
		}

		fByteValues = fByteValues.convertTo(mcuid, results);
		fMCUid = mcuid;

	}

	private IFile getFileFromLocation(IPath location) {

		// Convert the IPath to an URI (findFilesForLocation(IPath) is deprecated)
		URI locationAsURI = URIUtil.toURI(location);

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();

		try {
			IFile[] files = root.findFilesForLocationURI(locationAsURI);
			if (files.length != 0) {
				return files[0];
			}
		} catch (IllegalArgumentException iae) {
			// The caller will throw the appropriate FIleNotFound Exception
		}

		return null;

	}

}
