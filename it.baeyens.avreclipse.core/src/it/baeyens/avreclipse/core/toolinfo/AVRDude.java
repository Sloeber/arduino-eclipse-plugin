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
 * $Id: AVRDude.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.toolinfo;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.core.IMCUProvider;
import it.baeyens.avreclipse.core.avrdude.AVRDudeAction;
import it.baeyens.avreclipse.core.avrdude.AVRDudeActionFactory;
import it.baeyens.avreclipse.core.avrdude.AVRDudeException;
import it.baeyens.avreclipse.core.avrdude.ProgrammerConfig;
import it.baeyens.avreclipse.core.avrdude.AVRDudeException.Reason;
import it.baeyens.avreclipse.core.paths.AVRPath;
import it.baeyens.avreclipse.core.paths.AVRPathProvider;
import it.baeyens.avreclipse.core.paths.IPathProvider;
import it.baeyens.avreclipse.core.preferences.AVRDudePreferences;
import it.baeyens.avreclipse.core.targets.ClockValuesGenerator;
import it.baeyens.avreclipse.core.targets.HostInterface;
import it.baeyens.avreclipse.core.targets.IProgrammer;
import it.baeyens.avreclipse.core.targets.TargetInterface;
import it.baeyens.avreclipse.core.targets.ClockValuesGenerator.ClockValuesType;
import it.baeyens.avreclipse.core.toolinfo.fuses.ByteValues;
import it.baeyens.avreclipse.core.toolinfo.fuses.FuseType;
import it.baeyens.avreclipse.core.util.AVRMCUidConverter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.console.MessageConsole;


/**
 * This class handles all interactions with the avrdude program.
 * <p>
 * It implements the {@link IMCUProvider} Interface to get a list of all MCUs supported by the
 * selected version of AVRDude. Additional methods are available to get a list of all supported
 * Programmers.
 * </p>
 * <p>
 * This class implements the Singleton pattern. Use the {@link #getDefault()} method to get the
 * instance of this class.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 */
public class AVRDude implements IMCUProvider {

	/** The singleton instance of this class */
	private static AVRDude					instance			= null;

	/** The preference store for AVRDude */
	private final IPreferenceStore			fPrefsStore;

	/**
	 * A list of all currently supported MCUs (with avrdude MCU id values), mapped to the
	 * ConfigEntry
	 */
	private Map<String, ConfigEntry>		fMCUList;

	/**
	 * A list of all currently supported Programmer devices, mapped to their ID.
	 */
	private Map<String, IProgrammer>		fProgrammerList;

	/**
	 * A List of all Programmer ConfigEntries to their respective id's.
	 */
	private Map<String, ConfigEntry>		fProgrammerConfigEntries;

	/**
	 * Mapping of the Plugin MCU Id values (as keys) to the avrdude mcu id values (as values)
	 */
	private Map<String, String>				fMCUIdMap			= null;

	/** The current path to the directory of the avrdude executable */
	private IPath							fCurrentPath		= null;

	/** The name of the avrdude executable */
	private final static String				fCommandName		= "avrdude";

	/** The Path provider for the avrdude executable */
	private final IPathProvider					fPathProvider		= new AVRPathProvider(
																			AVRPath.AVRDUDE);

	/** Bug 3023718: Remember the last MCU connected to a given programmer */
	private final Map<ProgrammerConfig, String>	fLastMCUtypeMap		= new HashMap<ProgrammerConfig, String>();

	private long							fLastAvrdudeFinish	= 0L;

	/**
	 * A cache of one or more avrdude config files. The config files are stored as
	 * List&lt;String&gt; with one entry per line
	 */
	private final Map<IPath, List<String>>	fConfigFileCache	= new HashMap<IPath, List<String>>();

	/**
	 * Get the singleton instance of the AVRDude class.
	 */
	public static AVRDude getDefault() {
		if (instance == null)
			instance = new AVRDude();
		return instance;
	}

	// Prevent Instantiation of the class
	private AVRDude() {
		fPrefsStore = AVRDudePreferences.getPreferenceStore();
	}

	/**
	 * Returns the name of the AVRDude executable.
	 * <p>
	 * On Windows Systems the ".exe" extension is not included and needs to be added for access to
	 * avrdude other than executing the programm.
	 * </p>
	 * 
	 * @return String with "avrdude"
	 */
	public String getCommandName() {
		return fCommandName;
	}

	/**
	 * Returns the full path to the AVRDude executable.
	 * <p>
	 * Note: On Windows Systems the returned path does not include the ".exe" extension.
	 * </p>
	 * 
	 * @return <code>IPath</code> to the avrdude executable
	 */
	public IPath getToolPath() {
		IPath path = fPathProvider.getPath();
		return path.append(getCommandName());
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.IMCUProvider#getMCUInfo(java.lang.String)
	 */
	public String getMCUInfo(String mcuid) {
		Map<String, String> internalmap;
		try {
			internalmap = loadMCUList();
		} catch (AVRDudeException e) {
			// Something went wrong when avrdude was called. The exception has
			// already been logged, so just return null
			return null;
		}
		return internalmap.get(mcuid);
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.IMCUProvider#getMCUList()
	 */
	public Set<String> getMCUList() throws IOException {
		Map<String, String> internalmap;
		try {
			internalmap = loadMCUList();
		} catch (AVRDudeException e) {
			// Something went wrong when avrdude was called. The exception has
			// already been logged, but we wrap the Exception in an IOException
			throw new IOException("Could not start avrdude");
		}
		Set<String> idset = internalmap.keySet();
		return new HashSet<String>(idset);
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.IMCUProvider#hasMCU(java.lang.String)
	 */
	public boolean hasMCU(String mcuid) {
		Map<String, String> internalmap;
		try {
			internalmap = loadMCUList();
		} catch (AVRDudeException e) {
			// Something went wrong when avrdude was called. The exception has
			// already been logged, so just return false.
			return false;
		}
		return internalmap.containsKey(mcuid);
	}

	/**
	 * Returns the {@link IProgrammer} for a given programmer id.
	 * 
	 * @param programmerid
	 *            A valid programmer id as used by avrdude.
	 * @return the programmer type object or <code>null</code> if the <code>programmerid</code> is
	 *         unknown.
	 * @throws AVRDudeException
	 */
	public IProgrammer getProgrammer(String programmerid) throws AVRDudeException {
		loadProgrammersList(); // update the internal list (if required)
		IProgrammer type = fProgrammerList.get(programmerid);
		return type;
	}

	/**
	 * Returns a List of all currently supported Programmer devices.
	 * 
	 * @return <code>Set&lt;String&gt</code> with the avrdude id values.
	 * @throws AVRDudeException
	 */
	public List<IProgrammer> getProgrammersList() throws AVRDudeException {
		Collection<IProgrammer> list = loadProgrammersList().values();

		// Return a copy of the original list
		return new ArrayList<IProgrammer>(list);
	}

	/**
	 * Returns a Set of all currently supported Programmer devices.
	 * 
	 * @return <code>Set&lt;String&gt</code> with the avrdude id values.
	 * @throws AVRDudeException
	 */
	public Set<String> getProgrammerIDs() throws AVRDudeException {
		Set<String> allids = loadProgrammersList().keySet();

		// Return a copy of the original list
		return new HashSet<String>(allids);
	}

	/**
	 * Returns the {@link ConfigEntry} for the given Programmer device.
	 * 
	 * @param programmerid
	 *            <code>String</code> with the avrdude id of the programmer
	 * @return <code>ConfigEntry</code> containing all known information extracted from the avrdude
	 *         executable
	 * @throws AVRDudeException
	 */
	public ConfigEntry getProgrammerInfo(String programmerid) throws AVRDudeException {
		loadProgrammersList(); // update the list (if required)

		return fProgrammerConfigEntries.get(programmerid);
	}

	/**
	 * Returns the section of the avrdude.conf configuration file describing the the given
	 * ConfigEntry.
	 * <p>
	 * The extract is returned as a multiline <code>String</code> that can be used directly in an
	 * Text Control in the GUI.
	 * </p>
	 * <p>
	 * Note: The first call to this method may take some time, as the complete avrdude.conf file is
	 * read and and split into lines (currently around 450 Kbyte). This method is Synchronized, so
	 * it is safe to call it multiple times.
	 * 
	 * @param entry
	 *            The <code>ConfigEntry</code> for which to get the avrdude.conf entry.
	 * @return A <code>String</code> with the relevant lines, separated with '\n'.
	 * @throws IOException
	 *             Any Exception reading the configuration file.
	 */
	public synchronized String getConfigDetailInfo(ConfigEntry entry) throws IOException {

		List<String> configcontent = null;
		// Test if we have already loaded the config file
		IPath configpath = entry.configfile;
		if (fConfigFileCache.containsKey(configpath)) {
			configcontent = fConfigFileCache.get(configpath);
		} else {
			// Load the config file
			configcontent = loadConfigFile(configpath);
			fConfigFileCache.put(configpath, configcontent);
		}

		// make a string, starting from the given line until the first line that
		// does not start with a whitespace
		StringBuffer result = new StringBuffer();

		// copy every line from the config file until we hit a single ';' in the first column

		int index = entry.linenumber;
		while (true) {
			String line = configcontent.get(index++);
			if (line.startsWith(";")) {
				break;
			}
			result.append(line.trim()).append('\n');
		}
		return result.toString();
	}

	/**
	 * Return the MCU id value of the device currently attached to the given Programmer.
	 * 
	 * @param config
	 *            <code>ProgrammerConfig</code> with the Programmer to query.
	 * @return <code>String</code> with the id of the attached MCU.
	 * @throws AVRDudeException
	 */
	public String getAttachedMCU(ProgrammerConfig config, IProgressMonitor monitor)
			throws AVRDudeException {

		try {
			monitor.beginTask("Getting attached MCU", 100);
			if (config == null)
				throw new AVRDudeException(Reason.NO_PROGRAMMER, "", null);

			// Bug 3023718: List of avrdude mcu id's to test. The first entry will be replaced
			// by the last muc used with this programmer.
			String[] testmcus = { "", "m16", "x128a3" };
			String lastMCU = fLastMCUtypeMap.get(config);
			if (lastMCU != null) {
				testmcus[0] = fMCUIdMap.get(lastMCU);
			}

			for (String testmcu : testmcus) {

				if ("".equals(testmcu)) {
					continue;
				}

				List<String> configoptions = config.getArguments();
				configoptions.add("-p" + testmcu);

				try {
					List<String> stdout = runCommand(configoptions, new SubProgressMonitor(monitor,
							30), false, null, config);
					if (stdout == null) {
						continue;
					}

					// Parse the output and look for a line "avrdude: Device signature =
					// 0x123456"
					Pattern mcuPat = Pattern.compile(".+signature.+(0x[\\da-fA-F]{6})");
					Matcher m;

					for (String line : stdout) {
						m = mcuPat.matcher(line);
						if (!m.matches()) {
							continue;
						}
						// pattern matched. Get the Signature and convert it to a mcu id
						String mcuid = Signatures.getDefault().getMCU(m.group(1));
						fLastMCUtypeMap.put(config, mcuid);
						return mcuid;
					}
				} catch (AVRDudeException ade) {
					if (ade.getReason().equals(Reason.INIT_FAIL)) {
						// if the init failed then (maybe) we tried the wrong MCU.
						// continue with the next one:
						continue;
					} else {
						throw ade;
					}
				}

				// did not find a signature. Try the next candidate.

			}
			// Signature not found. This probably means that our simple parser is
			// broken
			throw new AVRDudeException(Reason.PARSE_ERROR,
					"Could not find a valid Signature in the avrdude output", null);
		} finally {
			monitor.done();
		}
	}

	/**
	 * Return the Fuse Bytes of the device currently attached to the given Programmer.
	 * <p>
	 * The values are read by calling avdude with the "-U" option to read all available fusebytes
	 * and storing them in tempfiles in the system temp directory. These files are read to get the
	 * values and deleted afterwards.
	 * </p>
	 * 
	 * @param config
	 *            <code>ProgrammerConfig</code> with the Programmer to query.
	 * @param monitor
	 *            <code>IProgressMonitor to cancel the operation.
	 * @return <code>FuseByteValues</code> with the values and the MCU id of the attached MCU.
	 * @throws AVRDudeException
	 */
	public ByteValues getFuseBytes(ProgrammerConfig config, IProgressMonitor monitor)
			throws AVRDudeException {

		try {
			monitor.beginTask("Reading Fuse Bytes", 100);
			// First get the attached MCU
			String mcuid = getAttachedMCU(config, new SubProgressMonitor(monitor, 20));

			// Check if the found MCU is actually supported by avrdude
			if (!hasMCU(mcuid)) {
				throw new AVRDudeException(
						Reason.UNKNOWN_MCU,
						"Found "
								+ AVRMCUidConverter.id2name(mcuid)
								+ " MCU signature. This MCU is not supported by the selected version of avrdude.");
			}

			ByteValues values = new ByteValues(FuseType.FUSE, mcuid);

			int fusebytecount = values.getByteCount();
			List<String> args = new ArrayList<String>(config.getArguments());
			args.add("-p" + getMCUInfo(mcuid));

			IPath tempdir = getTempDir();

			List<Integer> fuseindices = new ArrayList<Integer>();

			for (int i = 0; i < fusebytecount; i++) {
				String tmpfilename = tempdir.append("fuse" + i + ".hex").toOSString();
				String bytename = values.getByteName(i);

				// Skip the fusebyte if its name is empty. This will prevent avrdude from failing to
				// read the undefined fusebyte 3 of XMega MCUs.
				if (bytename.length() != 0) {
					AVRDudeAction action = AVRDudeActionFactory.readFuseByte(mcuid, i, tmpfilename);
					args.add(action.getArgument());
					fuseindices.add(i);
				}
			}

			List<String> stdout = runCommand(args, new SubProgressMonitor(monitor, 80), false,
					null, config);
			if (stdout == null) {
				return null;
			}

			// get the temporary files, read and parse them and delete them afterwards

			for (int i : fuseindices) {
				File tmpfile = tempdir.append("fuse" + i + ".hex").toFile();

				BufferedReader in = null;
				try {
					in = new BufferedReader(new FileReader(tmpfile));
					String valueString = in.readLine();
					if (valueString == null) {
						throw new AVRDudeException(Reason.UNKNOWN,
								"Temporary file generated by avrdude is empty.");
					}
					int value = Integer.decode(valueString);
					values.setValue(i, value);
					in.close();
					// Delete the temporary file.
					if (!tmpfile.delete()) {
						// tmpfile deletion has failed (unlikely). Well, there is not much we
						// can do about it so we just log it. Especially Windows users are used to a
						// bazillion stale tempfiles in their temp directory anyway.
						IStatus status = new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID,
								"Could not delete temporary file [" + tmpfile.toString() + "]",
								null);
						AVRPlugin.getDefault().log(status);
					}
				} catch (FileNotFoundException fnfe) {
					throw new AVRDudeException(Reason.UNKNOWN, "Can't read temporary file", fnfe);
				} catch (IOException ioe) {
					throw new AVRDudeException(Reason.UNKNOWN, "Can't read temporary file", ioe);
				}
			}
			return values;
		} finally {
			monitor.done();
		}

	}

	/**
	 * Return the lockbits of the device currently attached to the given Programmer.
	 * <p>
	 * The values are read by calling avdude with the "-U" option to read all available locks
	 * (currently only one) and storing them in tempfiles in the system temp directory. These files
	 * are read to get the values and deleted afterwards.
	 * </p>
	 * 
	 * @param config
	 *            <code>ProgrammerConfig</code> with the Programmer to query.
	 * @return <code>LockbitsByteValues</code> with the values and the MCU id of the attached MCU.
	 * @throws AVRDudeException
	 */
	public ByteValues getLockbits(ProgrammerConfig config, IProgressMonitor monitor)
			throws AVRDudeException {

		try {
			monitor.beginTask("Reading Lockbits", 100);
			// First get the attached MCU
			String mcuid = getAttachedMCU(config, new SubProgressMonitor(monitor, 20));

			// Check if the found MCU is actually supported by avrdude
			if (!hasMCU(mcuid)) {
				throw new AVRDudeException(
						Reason.UNKNOWN_MCU,
						"Found "
								+ AVRMCUidConverter.id2name(mcuid)
								+ " MCU signature. This MCU is not supported by the selected version of avrdude.");
			}

			ByteValues values = new ByteValues(FuseType.LOCKBITS, mcuid);

			int locksbytecount = values.getByteCount();
			List<String> args = new ArrayList<String>(config.getArguments());
			args.add("-p" + getMCUInfo(mcuid));

			IPath tempdir = getTempDir();

			for (int i = 0; i < locksbytecount; i++) {
				String tmpfilename = tempdir.append("lock" + i + ".hex").toOSString();
				AVRDudeAction action = AVRDudeActionFactory.readLockbitByte(mcuid, i, tmpfilename);
				args.add(action.getArgument());
			}

			List<String> stdout = runCommand(args, monitor, false, null, config);
			if (stdout == null) {
				return null;
			}

			// get the temporary files, read and parse them and delete them afterwards

			for (int i = 0; i < locksbytecount; i++) {
				File tmpfile = tempdir.append("lock" + i + ".hex").toFile();

				BufferedReader in = null;
				try {
					in = new BufferedReader(new FileReader(tmpfile));
					String valueString = in.readLine();
					if (valueString == null) {
						throw new AVRDudeException(Reason.UNKNOWN,
								"Temporary file generated by avrdude is empty.");
					}
					int value = Integer.decode(valueString);
					values.setValue(i, value);
					in.close();
					// Delete the temporary file.
					if (!tmpfile.delete()) {
						// tmpfile deletion has failed (unlikely). Well, there is not much we
						// can do about it so we just log it. Especially Windows users are used to a
						// bazillion stale tempfiles in their temp directory anyway.
						IStatus status = new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID,
								"Could not delete temporary file [" + tmpfile.toString() + "]",
								null);
						AVRPlugin.getDefault().log(status);
					}
				} catch (FileNotFoundException fnfe) {
					throw new AVRDudeException(Reason.UNKNOWN, "Can't read temporary file", fnfe);
				} catch (IOException ioe) {
					throw new AVRDudeException(Reason.UNKNOWN, "Can't read temporary file", ioe);
				}
			}

			return values;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Return the current erase cycle counter of the device currently attached to the given
	 * Programmer.
	 * <p>
	 * The value is read by calling avdude with the "-y" option.
	 * </p>
	 * 
	 * @param config
	 *            <code>ProgrammerConfig</code> with the Programmer to query.
	 * @return <code>int</code> with the values or <code>-1</code> if the counter value is not set.
	 * @throws AVRDudeException
	 */
	public int getEraseCycleCounter(ProgrammerConfig config, IProgressMonitor monitor)
			throws AVRDudeException {

		try {
			monitor.beginTask("Read Erase Cycle Counter", 100);
			// First get the attached MCU
			String mcuid = getAttachedMCU(config, new SubProgressMonitor(monitor, 20));

			List<String> args = new ArrayList<String>(config.getArguments());
			args.add("-p" + getMCUInfo(mcuid));

			List<String> stdout = runCommand(args, new SubProgressMonitor(monitor, 80), false,
					null, config);
			if (stdout == null) {
				return -1;
			}

			// Parse the output and look for a line "avrdude: current erase-rewrite cycle count is
			// xx"
			Pattern mcuPat = Pattern.compile(".*erase-rewrite cycle count.*?([0-9]+).*");
			Matcher m;

			for (String line : stdout) {
				m = mcuPat.matcher(line);
				if (!m.matches()) {
					continue;
				}
				// pattern matched. Get the cycle count and return it as an int
				return Integer.parseInt(m.group(1));
			}
			// Cycle count not found. This probably means that no cycle count has been set yet
			return -1;
		} finally {
			monitor.done();
		}
	}

	/**
	 * Set the erase cycle counter of the device currently attached to the given Programmer.
	 * <p>
	 * The value is set by calling avdude with the "-Y xxxx" option. The method returns the new
	 * value as read from the MCU as a crosscheck that the value has been written.
	 * </p>
	 * 
	 * @param config
	 *            <code>ProgrammerConfig</code> with the Programmer to query.
	 * @return <code>int</code> with the values or <code>-1</code> if the counter value is not set.
	 * @throws AVRDudeException
	 */
	public int setEraseCycleCounter(ProgrammerConfig config, int newcounter,
			IProgressMonitor monitor) throws AVRDudeException {

		try {
			monitor.beginTask("Setting Erase Cylce Counter", 100);
			// First get the attached MCU
			String mcuid = getAttachedMCU(config, new SubProgressMonitor(monitor, 20));

			List<String> args = new ArrayList<String>(config.getArguments());
			args.add("-p" + getMCUInfo(mcuid));
			args.add("-Y" + (newcounter & 0xffff));

			runCommand(args, new SubProgressMonitor(monitor, 60), false, null, config);

			// return the current value of the device as a crosscheck.
			return getEraseCycleCounter(config, new SubProgressMonitor(monitor, 20));
		} finally {
			monitor.done();
		}
	}

	/**
	 * Tries to guess the target interface from the AVRDude config id and the driver type from
	 * avrdude.conf.
	 * <p>
	 * This has been tested with avrdude 5.6. Future versions of avrdude might return wrong results.
	 * </p>
	 * 
	 * @param avrdudeid
	 *            The id of the programmer as used by avrdude.
	 * @return The interface used by the programmer.
	 */
	public static TargetInterface getInterface(String avrdudeid, String type) {

		// Check the serial bootloader types
		if (type.equals("avr910") || type.equals("butterfly")) {
			return TargetInterface.BOOTLOADER;
		}

		// Check if this is one of the HVSP supporting devices
		if (avrdudeid.endsWith("hvsp")) {
			return TargetInterface.HVSP;
		}

		// Check if this is one of the PP supporting devices
		if (avrdudeid.endsWith("pp")) {
			return TargetInterface.PP;
		}

		// Check if this is one of the DebugWire supporting devices
		if (avrdudeid.endsWith("dw")) {
			return TargetInterface.DW;
		}

		// First check for ISP devices (to filter JTAG devices in ISP mode)
		if (avrdudeid.endsWith("isp")) {
			return TargetInterface.ISP;
		}

		// Check if this is JTAG device
		if (avrdudeid.contains("jtag")) {
			return TargetInterface.JTAG;
		}

		// I think we filtered everything out, so what is left is a normal ISP programmer.
		return TargetInterface.ISP;

	}

	final static HostInterface[]	SERIALBBPORT	= new HostInterface[] { HostInterface.SERIAL_BB };
	final static HostInterface[]	SERIALPORT		= new HostInterface[] { HostInterface.SERIAL };
	final static HostInterface[]	PARALLELPORT	= new HostInterface[] { HostInterface.PARALLEL };
	final static HostInterface[]	USBPORT			= new HostInterface[] { HostInterface.USB };
	final static HostInterface[]	SERIAL_USB_PORT	= new HostInterface[] { HostInterface.SERIAL,
			HostInterface.USB						};

	/**
	 * Tries to guess the host interface from the AVRDude programmer id and type.
	 * <p>
	 * This has been tested with avrdude 5.6. Future versions of avrdude might return wrong results.
	 * </p>
	 * 
	 * @param avrdudeid
	 *            The id of the programmer as used by avrdude.
	 * @return The interface used by the programmer.
	 */
	public static HostInterface[] getHostInterfaces(String avrdudeid, String avrdudetype) {

		String type = avrdudetype.toLowerCase(); // just in case
		if (type.equals("serbb")) {
			return SERIALBBPORT;
		}
		if (type.equals("par")) {
			return PARALLELPORT;
		}
		if (type.contains("usb")) {
			return USBPORT;
		}
		if (avrdudeid.startsWith("stk500") || type.equals("stk500")) {
			// The STK500 has only a serial port
			return SERIALPORT;
		}
		if (type.equals("stk500v2")) {
			// This can be either USB or SERIAL. We need to check the id
			if (avrdudeid.equals("avrispv2") || avrdudeid.equals("avrispmkII")
					|| avrdudeid.equals("avrisp2")) {
				// The AVR ISP MkII uses the STK500v2 protokoll, but over USB
				return USBPORT;
			}
			// All other STK500v2 versions / clones use the serial port
			return SERIALPORT;
		}
		if (type.startsWith("stk600")) {
			return USBPORT;
		}
		if (type.equals("avr910") || type.equals("butterfly")) {
			// Bootloader types
			return SERIALPORT;
		}
		if (type.startsWith("dragon")) {
			// AVR Dragon
			return USBPORT;
		}
		if (type.startsWith("jtag")) {
			// AVR ICE MkI normally uses the Serial port, but some clones have an USB port as well
			// AVR ICE MkII has both a serial and an usb port.
			return SERIAL_USB_PORT;
		}
		if (type.startsWith("arduino")) {
			// All newer Arduinos have an USB Port, but some old versions came with a serial port.
			return SERIAL_USB_PORT;
		}
		if (type.startsWith("buspirate")) {
			// some very old Buspirate Boards have only a serial port, but I think we can ignore them.
			return USBPORT;
		}

		// TODO remove when testing is finished
		throw new IllegalArgumentException("Can't determine host interface");
	}

	/**
	 * Internal method to read the config file with the given path and split it into lines.
	 * 
	 * @param path
	 *            <code>IPath</code> to a configuration file.
	 * @return A <code>List&lt;String&gt;</code> with all lines of the given configuration file
	 * @throws IOException
	 *             Any Exception reading the configuration file.
	 */
	private List<String> loadConfigFile(IPath path) throws IOException {

		// The default avrdude.conf file has some 12.000+ lines, however custom
		// avrdude.conf files might be much smaller, so we start with 100 lines
		// and let the ArrayList grow as required
		List<String> content = new ArrayList<String>(100);

		BufferedReader br = null;

		try {
			File configfile = path.toFile();
			br = new BufferedReader(new FileReader(configfile));

			String line;
			while ((line = br.readLine()) != null) {
				content.add(line);
			}

		} finally {
			if (br != null)
				br.close();
		}
		return content;
	}

	/**
	 * @return Map&lt;mcu id, avrdude id&gt; of all supported MCUs
	 * @throws AVRDudeException
	 */
	private Map<String, String> loadMCUList() throws AVRDudeException {

		if (!getToolPath().equals(fCurrentPath)) {
			// toolpath has changed, reload the list
			fMCUList = null;
			fMCUIdMap = null;
			fCurrentPath = getToolPath();
		}

		if (fMCUIdMap != null) {
			// return stored map
			return fMCUIdMap;
		}

		fMCUList = new HashMap<String, ConfigEntry>();
		// Execute avrdude with the "-p?" to get a list of all supported mcus.
		readAVRDudeConfigOutput(fMCUList, "-p?");

		// The returned list has avrdude mcu id values, which are not the same
		// as the ones used in this Plugin. Instead the returned name is
		// converted into an Pluin mcu id value.
		fMCUIdMap = new HashMap<String, String>(fMCUList.size());
		Collection<ConfigEntry> allentries = fMCUList.values();
		for (ConfigEntry entry : allentries) {
			String mcuid = AVRMCUidConverter.name2id(entry.description);
			fMCUIdMap.put(mcuid, entry.avrdudeid);
		}

		return fMCUIdMap;
	}

	/**
	 * @return Map&lt;mcu id, avrdude id&gt; of all supported Programmer devices.
	 * @throws AVRDudeException
	 */
	private Map<String, IProgrammer> loadProgrammersList() throws AVRDudeException {

		if (!getToolPath().equals(fCurrentPath)) {
			// toolpath has changed, reload the list
			fProgrammerList = null;
			fCurrentPath = getToolPath();
		}

		if (fProgrammerList == null) {
			fProgrammerConfigEntries = new HashMap<String, ConfigEntry>();
			// Execute avrdude with the "-c?" to get a list of all supported
			// programmers.
			readAVRDudeConfigOutput(fProgrammerConfigEntries, "-c?");

			// Convert the ConfigEntries to IProgrammerTypes
			fProgrammerList = new HashMap<String, IProgrammer>();
			for (String id : fProgrammerConfigEntries.keySet()) {
				IProgrammer type = new ProgrammerType(id);
				fProgrammerList.put(id, type);
			}
		}
		return fProgrammerList;
	}

	/**
	 * Internal method to execute avrdude and parse the output as ConfigEntries.
	 * 
	 * @see #loadMCUList()
	 * @see #loadProgrammersList()
	 * 
	 * @param resultmap
	 * @param arguments
	 * @throws AVRDudeException
	 */
	private void readAVRDudeConfigOutput(Map<String, ConfigEntry> resultmap, String... arguments)
			throws AVRDudeException {

		List<String> stdout = runCommand(arguments);
		if (stdout == null) {
			return;
		}

		// Avrdude output for configuration items looks like:
		// " id = description [pathtoavrdude.conf:line]"
		// The following pattern splits this into the four groups:
		// id / description / path / line
		Pattern mcuPat = Pattern.compile("\\s*(\\S+)\\s*=\\s*(.+?)\\s*\\[(.+):(\\d+)\\]\\.*");
		Matcher m;

		for (String line : stdout) {
			m = mcuPat.matcher(line);
			if (!m.matches()) {
				continue;
			}
			ConfigEntry entry = new ConfigEntry();
			entry.avrdudeid = m.group(1);
			entry.description = m.group(2);
			entry.configfile = new Path(m.group(3));
			entry.linenumber = Integer.valueOf(m.group(4));

			resultmap.put(entry.avrdudeid, entry);
		}
	}

	/**
	 * Get the command name and the current version of avrdude.
	 * <p>
	 * The name is defined in {@link #fCommandName}. The version is gathered by executing with the
	 * "-v" option and parsing the output.
	 * </p>
	 * 
	 * @return <code>String</code> with the command name and version
	 * @throws AVRDudeException
	 */
	public String getNameAndVersion() throws AVRDudeException {

		// Execute avrdude with the "-v" option and parse the
		// output.
		// Just "-v" will have avrdude complain about a missing programmer => AVRDudeException.
		// So we supply a dummy (but existing) programmer. AVRDude will now complain about the
		// missing part, but we can ignore this because we got what we wanted: the version number.
		List<String> stdout = runCommand("-v", "-cstk500v2");
		if (stdout == null) {
			// Return default name on failures
			return getCommandName() + " n/a";
		}

		// look for a line matching "*Version TheVersionNumber *"
		Pattern mcuPat = Pattern.compile(".*Version\\s+([\\d\\.]+).*");
		Matcher m;
		for (String line : stdout) {
			m = mcuPat.matcher(line);
			if (!m.matches()) {
				continue;
			}
			return getCommandName() + " " + m.group(1);
		}

		// could not read the version from the output, probably the regex has a
		// mistake. Return a reasonable default.
		return getCommandName() + " ?.?";
	}

	/**
	 * Runs avrdude with the given arguments.
	 * <p>
	 * The Output of stdout and stderr are merged and returned in a <code>List&lt;String&gt;</code>.
	 * </p>
	 * <p>
	 * If the command fails to execute an entry is written to the log and an
	 * {@link AVRDudeException} with the reason is thrown.
	 * </p>
	 * 
	 * @param arguments
	 *            Zero or more arguments for avrdude
	 * @return A list of all output lines, or <code>null</code> if the command could not be
	 *         launched.
	 * @throws AVRDudeException
	 *             when avrdude cannot be started or when avrdude returned an
	 */
	public List<String> runCommand(String... arguments) throws AVRDudeException {

		List<String> arglist = new ArrayList<String>(1);
		for (String arg : arguments) {
			arglist.add(arg);
		}

		return runCommand(arglist, new NullProgressMonitor(), false, null, null);
	}

	/**
	 * Runs avrdude with the given arguments.
	 * <p>
	 * The Output of stdout and stderr are merged and returned in a <code>List&lt;String&gt;</code>.
	 * If the "use Console" flag is set in the Preferences, the complete output is shown on a
	 * Console as well.
	 * </p>
	 * <p>
	 * If the command fails to execute an entry is written to the log and an
	 * {@link AVRDudeException} with the reason is thrown.
	 * </p>
	 * 
	 * @param arguments
	 *            <code>List&lt;String&gt;</code> with the arguments
	 * @param monitor
	 *            <code>IProgressMonitor</code> to cancel the running process.
	 * @param forceconsole
	 *            If <code>true</code> all output is copied to the console, regardless of the "use
	 *            console" flag.
	 * @param cwd
	 *            <code>IPath</code> with a current working directory or <code>null</code> to use
	 *            the default working directory (usually the one defined with the system property
	 *            <code>user.dir</code). May not be empty.
	 * @param programmerconfig
	 *            The AVRDude Programmer configuration currently is use. Required for the AVRDude
	 *            invocation delay value. If <code>null</code> no invocation delay will be done.
	 * @return A list of all output lines, or <code>null</code> if the command could not be
	 *         launched.
	 * @throws AVRDudeException
	 *             when avrdude cannot be started or when avrdude returned an error errors.
	 */
	public List<String> runCommand(List<String> arglist, IProgressMonitor monitor,
			boolean forceconsole, IPath cwd, ProgrammerConfig programmerconfig)
			throws AVRDudeException {

		try {
			monitor.beginTask("Running avrdude", 100);

			// Check if the CWD is valid
			if (cwd != null && cwd.isEmpty()) {
				throw new AVRDudeException(Reason.INVALID_CWD,
						"CWD does not point to a valid directory.");
			}

			String command = getToolPath().toOSString();

			// Check if the user has a custom configuration file
			IPreferenceStore avrdudeprefs = AVRDudePreferences.getPreferenceStore();
			boolean usecustomconfig = avrdudeprefs
					.getBoolean(AVRDudePreferences.KEY_USECUSTOMCONFIG);
			if (usecustomconfig) {
				String newconfigfile = avrdudeprefs.getString(AVRDudePreferences.KEY_CONFIGFILE);
				arglist.add("-C" + newconfigfile);
			}

			// Set up the External Command
			ExternalCommandLauncher avrdude = new ExternalCommandLauncher(command, arglist, cwd);
			avrdude.redirectErrorStream(true);

			MessageConsole console = null;
			// Set the Console (if requested by the user in the preferences)
			if (fPrefsStore.getBoolean(AVRDudePreferences.KEY_USECONSOLE) || forceconsole) {
				console = AVRPlugin.getDefault().getConsole("AVRDude");
				avrdude.setConsole(console);
			}

			ICommandOutputListener outputlistener = new OutputListener(monitor);
			avrdude.setCommandOutputListener(outputlistener);

			// This will delay the actual avrdude call if the previous call finished less than the
			// user provided time in milliseconds
			avrdudeInvocationDelay(programmerconfig, console, new SubProgressMonitor(monitor, 10));

			// Run avrdude
			try {
				fAbortReason = null;
				int result = avrdude.launch(new SubProgressMonitor(monitor, 80));

				// Test if avrdude was aborted
				if (fAbortReason != null) {
					throw new AVRDudeException(fAbortReason, fAbortLine);
				}

				if (result == -1) {
					throw new AVRDudeException(Reason.USER_CANCEL, "");
				}
			} catch (IOException e) {
				// Something didn't work while running the external command
				throw new AVRDudeException(Reason.NO_AVRDUDE_FOUND,
						"Cannot run AVRDude executable. Please check the AVR path preferences.", e);
			}

			// Everything was fine: get the ooutput from avrdude and return it
			// to the caller
			List<String> stdout = avrdude.getStdOut();

			monitor.worked(10);

			return stdout;
		} finally {
			monitor.done();
			fLastAvrdudeFinish = System.currentTimeMillis();
		}
	}

	/**
	 * Delay for the user specified invocation delay time.
	 * <p>
	 * This method will take the user supplied delay value from the given ProgrammerConfig, check
	 * how much time has passed since the last avrdude call finished and - if actually required -
	 * wait for the remaining milliseconds.
	 * </p>
	 * <p>
	 * While sleeping this method will wake up every 10 ms to check if the user has cancelled, in
	 * which case an {@link AVRDudeException} with {@link Reason#USER_CANCEL} is thrown.
	 * </p>
	 * 
	 * @param programmerconfig
	 *            contains the delay value. if <code>null</code> this method returns immediatly.
	 * @param console
	 *            If not <code>null</code>, then the start and end of the delay is logged on the
	 *            console.
	 * @param monitor
	 *            polled for user cancel event.
	 * @throws AVRDudeException
	 *             when the user cancelles the delay.
	 */
	private void avrdudeInvocationDelay(final ProgrammerConfig programmerconfig,
			MessageConsole console, IProgressMonitor monitor) throws AVRDudeException {

		// return if no ProgrammerConfig is available
		if (programmerconfig == null) {
			return;
		}

		// Get the optional avrdude invocation delay value
		String delayvalue = programmerconfig.getPostAvrdudeDelay();
		if (delayvalue == null || delayvalue.length() == 0) {
			return;
		}
		final int delay = Integer.decode(delayvalue);
		if (delay == 0) {
			return;
		}

		IOConsoleOutputStream ostream = null;
		if (console != null) {
			ostream = console.newOutputStream();
		}

		final long targetmillis = fLastAvrdudeFinish + delay;

		// Quick exit if the delay has already expired
		int targetdelay = (int) (targetmillis - System.currentTimeMillis());
		if (targetdelay < 1) {
			return;
		}

		try {
			monitor.beginTask("delay", targetdelay);

			writeOutput(ostream, "\n>>> avrdude invocation delay: " + targetdelay
					+ " milliseconds\n");

			// delay for specified amount of milliseconds
			// To allow user cancel during long delays we check the monitor every 10
			// milliseconds.
			// This is the fix for Bug 2071415
			while (System.currentTimeMillis() < targetmillis) {
				if (monitor.isCanceled()) {
					writeOutput(ostream, ">>> avrdude invocation delay: cancelled\n");
					throw new AVRDudeException(Reason.USER_CANCEL, "User cancelled");
				}
				Thread.sleep(10);
			}
			writeOutput(ostream, ">>> avrdude invocation delay: finished\n");

		} catch (InterruptedException e) {
			throw new AVRDudeException(Reason.USER_CANCEL, "System interrupt");
		} catch (IOException e) {
			// ignore exception
		} finally {
			if (ostream != null) {
				try {
					ostream.close();
				} catch (IOException e) {
					// ignore exception
				}
			}
			monitor.done();
		}

	}

	/**
	 * Convenience method to print a message to the given stream. This method checks that the stream
	 * exists.
	 * 
	 * @param ostream
	 * @param message
	 * @throws IOException
	 */
	private void writeOutput(IOConsoleOutputStream ostream, String message) throws IOException {
		if (ostream != null) {
			ostream.write(message);
		}
	}

	/**
	 * Get the path to the System temp directory.
	 * 
	 * @return <code>IPath</code>
	 */
	private IPath getTempDir() {

		String tmpdir = System.getProperty("java.io.tmpdir");
		return new Path(tmpdir);
	}

	/**
	 * The Reason code why avrdude was aborted (or <code>null</code> if avrdude finished normally)
	 */
	protected volatile Reason	fAbortReason;

	/** The line from the avrdude output that caused the abort */
	protected String			fAbortLine;

	/**
	 * Internal class to listen to the output of avrdude and cancel avrdude if the certain key
	 * Strings appears in the output.
	 * <p>
	 * They are:
	 * <ul>
	 * <li><code>timeout</code></li>
	 * <li><code>Can't open device</code></li>
	 * <li><code>can't open config file</code></li>
	 * <li><code>Can't find programmer id</code></li>
	 * <li><code>AVR Part ???? not found</code></li>
	 * </ul>
	 * </p>
	 * <p>
	 * Once any of these Strings is found in the output the associated Reason is set and avrdude is
	 * aborted via the ProgressMonitor.
	 * </p>
	 */
	private class OutputListener implements ICommandOutputListener {

		private final IProgressMonitor	fProgressMonitor;

		public OutputListener(IProgressMonitor monitor) {
			fProgressMonitor = monitor;
		}

		// private Reason fAbortReason;
		// private String fAbortLine;

		/*
		 * (non-Javadoc)
		 * @see  it.baeyens.avreclipse.core.toolinfo.ICommandOutputListener#init(org.eclipse.core.runtime
		 * .IProgressMonitor)
		 */
		public void init(IProgressMonitor monitor) {
			// fProgressMonitor = monitor;
			// fAbortLine = null;
			// fAbortReason = null;
		}

		public void handleLine(String line, StreamSource source) {

			boolean abort = false;

			if (line.contains("timeout")) {
				abort = true;
				fAbortReason = Reason.TIMEOUT;
			} else if (line.contains("can't open device")) {
				abort = true;
				fAbortReason = Reason.PORT_BLOCKED;
			} else if (line.contains("can't open config file")) {
				abort = true;
				fAbortReason = Reason.CONFIG_NOT_FOUND;
			} else if (line.contains("Can't find programmer id")) {
				abort = true;
				fAbortReason = Reason.UNKNOWN_PROGRAMMER;
			} else if (line.contains("no programmer has been specified")) {
				abort = true;
				fAbortReason = Reason.NO_PROGRAMMER;
			} else if (line.matches("AVR Part.+not found")) {
				abort = true;
				fAbortReason = Reason.UNKNOWN_MCU;
			} else if (line.endsWith("execution aborted")) {
				abort = true;
				fAbortReason = Reason.USER_CANCEL;
			} else if (line.contains("usbdev_open")) {
				abort = true;
				fAbortReason = Reason.NO_USB;
			} else if (line.contains("failed to sync with")) {
				abort = true;
				fAbortReason = Reason.SYNC_FAIL;
			} else if (line.contains("initialization failed")) {
				// don't set the abort flag so that the progress monitor does not get canceled. This
				// is a hack to make the #getAttachedMCU() method work.
				fAbortLine = line;
				fAbortReason = Reason.INIT_FAIL;
			} else if (line.contains("NO_TARGET_POWER")) {
				abort = true;
				fAbortReason = Reason.NO_TARGET_POWER;
			} else if (line.contains("can't set buffers for")) {
				abort = true;
				fAbortReason = Reason.INVALID_PORT;
			} else if (line.contains("error in USB receive")) {
				abort = true;
				fAbortReason = Reason.USB_RECEIVE_ERROR;
			}
			if (abort) {
				fProgressMonitor.setCanceled(true);
				fAbortLine = line;
			}
		}

		/*
		 * (non-Javadoc)
		 * @see it.baeyens.avreclipse.core.toolinfo.ICommandOutputListener#getAbortLine()
		 */
		public String getAbortLine() {
			return fAbortLine;
		}

		/*
		 * (non-Javadoc)
		 * @see it.baeyens.avreclipse.core.toolinfo.ICommandOutputListener#getAbortReason()
		 */
		public Reason getAbortReason() {
			return fAbortReason;
		}

	}

	/**
	 * Container class for AVRDude configuration entries.
	 * <p>
	 * This class is stores the four informations that avrdude supplies about a Programming device
	 * or a MCU part:
	 * </p>
	 * <ul>
	 * <li>{@link #avrdudeid} = AVRDude internal id</li>
	 * <li>{@link #description} = Human readable description</li>
	 * <li>{@link #configfile} = Path to the avrdude configuration file which declares this
	 * programmer or part</li>
	 * <li>{@link #linenumber} = Line number within the configuration file where the definition
	 * starts</li>
	 * </ul>
	 * 
	 */
	public static class ConfigEntry {
		/** AVRDude internal id for this entry */
		public String	avrdudeid;

		/** (Human readable) description of this entry */
		public String	description;

		/** Path to the configuration file which contains the definition */
		public IPath	configfile;

		/** line number of the start of the definition */
		public int		linenumber;
	}

	private class ProgrammerType implements IProgrammer {

		private String			fAvrdudeId;

		private String			fDescription;

		private HostInterface	fHostInterface[];

		private TargetInterface	fTargetInterface;

		private String			fType;

		private int[]			fClockFrequencies;

		protected ProgrammerType(String id) {
			fAvrdudeId = id;
		}

		/*
		 * (non-Javadoc)
		 * @see it.baeyens.avreclipse.core.targets.IProgrammerType#getId()
		 */
		public String getId() {
			return fAvrdudeId;
		}

		/*
		 * (non-Javadoc)
		 * @see it.baeyens.avreclipse.core.targets.IProgrammerType#getDescription()
		 */
		public String getDescription() {
			if (fDescription == null) {
				try {
					ConfigEntry entry = getProgrammerInfo(fAvrdudeId);
					fDescription = entry.description;
				} catch (AVRDudeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					fDescription = "";
				}
			}
			return fDescription;
		}

		/*
		 * (non-Javadoc)
		 * @see it.baeyens.avreclipse.core.targets.IProgrammer#getAdditionalInfo()
		 */
		public String getAdditionalInfo() {

			try {
				ConfigEntry entry = getProgrammerInfo(fAvrdudeId);
				String addinfo = getConfigDetailInfo(entry);
				return "avrdude.conf entry for this programmer:\n\n" + addinfo;
			} catch (AVRDudeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return "";
		}

		/*
		 * (non-Javadoc)
		 * @see it.baeyens.avreclipse.core.targets.IProgrammerType#getHostInterface()
		 */
		public HostInterface[] getHostInterfaces() {
			if (fHostInterface == null) {
				String type = getType();
				fHostInterface = AVRDude.getHostInterfaces(fAvrdudeId, type);
			}

			return fHostInterface;
		}

		/*
		 * (non-Javadoc)
		 * @see it.baeyens.avreclipse.core.targets.IProgrammerType#getTargetInterfaces()
		 */
		public TargetInterface getTargetInterface() {
			if (fTargetInterface == null) {
				String type = getType();
				fTargetInterface = getInterface(fAvrdudeId, type);
			}
			return fTargetInterface;
		}

		/*
		 * (non-Javadoc)
		 * @see it.baeyens.avreclipse.core.targets.IProgrammer#getTargetInterfaceClockFrequencies()
		 */
		public int[] getTargetInterfaceClockFrequencies() {
			if (fClockFrequencies == null) {
				String type = getType();
				TargetInterface tif = getTargetInterface();

				ClockValuesType protocol = null;

				if ("usbtiny".equals(type)) {
					// USBTiny pogrammer
					protocol = ClockValuesType.USBTINY;

				} else if ("jtagmki".equals(type)) {
					// AVR ICE MkI programmer
					protocol = ClockValuesType.JTAG1;

				} else if (type.startsWith("jtagmkii") || type.startsWith("dragon")) {
					// AVR ICE MkII or AVR Dragon
					switch (tif) {
						case JTAG:
						case DW:
							protocol = ClockValuesType.JTAG2;
							break;
						default:
							protocol = ClockValuesType.AVRISPMK2;
					}

				} else if (type.startsWith("stk500")) {
					// AVR ISP (mkII), STK500 or compatible
					protocol = ClockValuesType.STK500;

				} else if (type.startsWith("stk600")) {
					// STK600 board
					protocol = ClockValuesType.STK600;

				}

				if (protocol != null) {
					fClockFrequencies = ClockValuesGenerator.getValues(protocol);
				} else {
					fClockFrequencies = new int[] {};
				}
			}
			return fClockFrequencies;
		}

		/*
		 * (non-Javadoc)
		 * @see it.baeyens.avreclipse.core.targets.IProgrammer#isDaisyChainCapable()
		 */
		public boolean isDaisyChainCapable() {
			TargetInterface ti = getTargetInterface();
			if (!ti.equals(TargetInterface.JTAG)) {
				return false;
			}

			return true;
		}

		/**
		 * 
		 */
		private String getType() {
			if (fType == null) {
				try {
					// get the Detailed info for the programmer
					ConfigEntry entry = getProgrammerInfo(fAvrdudeId);
					String info = getConfigDetailInfo(entry);

					// find the type by looking for "type = xxx" in the info text
					Pattern typePat = Pattern.compile(".*type\\s*=\\s*(\\w*);.*", Pattern.DOTALL);
					Matcher m = typePat.matcher(info);
					if (m.matches()) {
						fType = m.group(1);
					} else {
						// could not find the 'type = xxx' pattern in the info.
						// This should not happen and means that the avrdude.conf
						// has errors.
						// TODO: log a message
						fType = "";
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (AVRDudeException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			return fType;
		}
	}
}
