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
 *     Manuel Stahl - original idea to parse the <avr/io.h> file and the patterns
 *     
 * $Id: AVRiohDeviceDescriptionProvider.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.devicedescription.avrio;

import it.baeyens.avreclipse.core.paths.AVRPath;
import it.baeyens.avreclipse.core.paths.AVRPathProvider;
import it.baeyens.avreclipse.core.paths.IPathProvider;
import it.baeyens.avreclipse.core.preferences.AVRPathsPreferences;
import it.baeyens.avreclipse.core.util.AVRMCUidConverter;
import it.baeyens.avreclipse.devicedescription.ICategory;
import it.baeyens.avreclipse.devicedescription.IDeviceDescription;
import it.baeyens.avreclipse.devicedescription.IDeviceDescriptionProvider;
import it.baeyens.avreclipse.devicedescription.IEntry;
import it.baeyens.avreclipse.devicedescription.IProviderChangeListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;


/**
 * Provides DeviceDescription Objects based on parsing the <avr/io.h> file.
 * <p>
 * As the information in the include/avr folder is static, the class should be accessed with the
 * static method {@link #getDefault()}.
 * </p>
 * <p>
 * <b>Note:</b> The Registers defined in <avr/io.h>, namely the SREG and SP(L|H) are not included,
 * as parsing io.h would require an understanding of <code>#ifdef</code>, which the simple parser
 * in this class has not.
 * </p>
 * 
 * @author Thomas Holland
 * @author Manuel Stahl
 */
public class AVRiohDeviceDescriptionProvider implements IDeviceDescriptionProvider,
		IPropertyChangeListener {

	private static AVRiohDeviceDescriptionProvider	instance			= null;

	private Map<String, String>						fMCUNamesMap		= null;
	private Map<String, DeviceDescription>			fCache				= null;

	private String									fInternalErrorMsg	= null;

	private final List<IProviderChangeListener>		fChangeListeners	= new ArrayList<IProviderChangeListener>(
																				0);

	private final IPathProvider						fPathProvider		= new AVRPathProvider(
																				AVRPath.AVRINCLUDE);

	/**
	 * Get an instance of this DeviceModelProvider.
	 */
	public static AVRiohDeviceDescriptionProvider getDefault() {
		if (instance == null)
			instance = new AVRiohDeviceDescriptionProvider();
		return instance;
	}

	/**
	 * private default constructor, so the class can only be accessed via the singleton getDefault()
	 * method.
	 * 
	 * The Constructor will register a Preference change listener to be informed about any changes
	 * to the <avr/io.h> path preference value.
	 */
	private AVRiohDeviceDescriptionProvider() {
		// Add ourself as a listener to Path Preference change events
		IPreferenceStore store = AVRPathsPreferences.getPreferenceStore();
		store.addPropertyChangeListener(this);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.devicedescription.IDeviceDescriptionProvider#getDeviceList()
	 */
	public Set<String> getMCUList() {
		if (fMCUNamesMap == null) {
			try {
				loadDevices();
			} catch (IOException ioe) {
				return null;
			}
		}
		Set<String> devs = new HashSet<String>(fMCUNamesMap.keySet());
		return devs;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.core.IMCUProvider#hasMCU(java.lang.String)
	 */
	public boolean hasMCU(String mcuid) {
		Set<String> mcus = getMCUList();
		if (mcus != null) {
			return getMCUList().contains(mcuid);
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.devicedescription.IDeviceDescriptionProvider#getDevice(java.lang.String)
	 */
	public String getMCUInfo(String name) {
		IDeviceDescription dd = getDeviceDescription(name);
		if (dd == null) {
			return null;
		}
		List<String> headerfiles = dd.getSourcesList();
		if (headerfiles.size() > 0) {
			// TODO: maybe append the sub-header files
			return headerfiles.get(0);
		}
		return null;
	}

	/**
	 * Returns the IDeviceDescription for the given MCU id
	 * 
	 * @param name
	 *            String with a MCU id
	 * @return <code>IDeviceDescription</code> or <code>null</code> if the give MCU id is not
	 *         known or an error has occured reading the files.
	 */
	public IDeviceDescription getDeviceDescription(String name) {
		if (name == null)
			return null;

		if (fMCUNamesMap == null) {
			try {
				loadDevices();
			} catch (IOException ioe) {
				// return null on errors
				return null;
			}
		}

		// check if the name actually exists (and has a headerfile to load its
		// properties from)
		String headerfile = fMCUNamesMap.get(name);
		if (headerfile == null)
			return null;

		// Test if we already have this device in the cache
		if (fCache == null) {
			fCache = new HashMap<String, DeviceDescription>();
		}
		DeviceDescription currdev = fCache.get(name);
		if (currdev == null) {
			// No: create a new DeviceDescription
			currdev = new DeviceDescription(name);

			try {
				readDeviceHeader(currdev, headerfile);
			} catch (IOException e) {
				return null;
			}
			// Add the DeviceDescription to the cache
			fCache.put(name, currdev);
		}

		return currdev;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.devicedescription.IDeviceDescriptionProvider#getBasePath()
	 */
	public IPath getBasePath() {
		return new Path(getAVRIncludePath());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.devicedescription.IDeviceDescriptionProvider#getErrorMessage()
	 */
	public String getErrorMessage() {
		return fInternalErrorMsg + " Check the preferences for a correct path setting";
	}

	/**
	 * Initialize the list of fMCUNamesMap by opening the <avr/io.h> file and parsing it for all
	 * defined MCUs.
	 * 
	 * throws IOException if there was an error opening or reading the file.
	 */
	private void loadDevices() throws IOException {

		BufferedReader in = null;
		try {
			in = new BufferedReader(new FileReader(getAVRiohFile()));
		} catch (FileNotFoundException fnfe) {
			fInternalErrorMsg = "Cannot find <avr/io.h> (looked here: " + getAVRiohFile() + ").";
			throw fnfe;
		}

		fMCUNamesMap = new HashMap<String, String>();
		List<String> curDev = new ArrayList<String>();

		String line;
		Pattern defPat = Pattern.compile("__AVR_(.*?)__");
		Pattern incPat = Pattern.compile("^#  include <(.*)>");
		Matcher m;
		try {
			while ((line = in.readLine()) != null) {
				m = defPat.matcher(line);
				
				// There may be more than one "__AVR_xxxx__" entry per line
				while (m.find()) {
					curDev.add(m.group(1));
				}
				m = incPat.matcher(line);
				if (m.matches() && curDev.size() != 0) {
					for (String dev : curDev) {
						fMCUNamesMap.put(AVRMCUidConverter.name2id(dev), m.group(1));
					}
					curDev.clear();
				}
			}
		} catch (IOException ioe) {
			fInternalErrorMsg = "Cannot read <avr/io.h> (" + getAVRiohFile() + ").";
			throw ioe;
		}
		fInternalErrorMsg = null;
		in.close();
	}

	private void readDeviceHeader(DeviceDescription device, String headerfile) throws IOException {
		String line;

		device.addHeaderFile(headerfile);

		BufferedReader in;

		File hfile = new File(getAVRIncludePath() + "/" + headerfile);

		try {
			// Open a reader on the given header file and fail
			// if it can't be opened
			in = new BufferedReader(new FileReader(hfile));
		} catch (FileNotFoundException fnfe) {
			fInternalErrorMsg = "Cannot open source header file \"" + hfile.getAbsolutePath()
					+ "\".";
			throw fnfe;
		}

		Pattern incPat = Pattern.compile("^#include <(.+)>.*");
		Pattern descPat = Pattern.compile("/\\* (?:RegDef\\:  )?(.+) \\*/.*");

		Pattern ivecPatNew = Pattern.compile("^#define ([A-Z0-9_]+_vect)\\s+_VECTOR\\((\\d+)\\).*");
		Pattern ivecPatOld = Pattern.compile("^#define (SIG_[A-Z0-9_]+)\\s+_VECTOR\\((\\d+)\\).*");
		Pattern portPat = Pattern
				.compile("^#define ((?:PORT|PIN|DDR)[A-Z])\\s+_SFR_IO(\\d+)\\s*\\((0[xX].*)\\).*");
		Pattern regPat = Pattern
				.compile("^#define ([A-Z0-9]+)\\s+_SFR_(IO|MEM)(\\d+)\\s*\\((0[xX].*)\\).*");

		Matcher m;

		List<ICategory> categories = device.getCategories();
		ICategory regCategory = categories.get(0);
		ICategory portCategory = categories.get(1);
		ICategory ivecCategory = categories.get(2);

		// Stores the last comment in the source code
		String activeDesc = "";

		try {
			while ((line = in.readLine()) != null) {

				// Test if current line contains an #include directive
				m = incPat.matcher(line);
				if (m.matches()) {
					// Yes: Recurse into this file if its not <avr/sfr_defs.h>
					// or
					// outside the <avr/*> directory
					// (<avr/sfr_defs.h> because this file has some comments
					// that
					// are picked up as
					// register definitions)
					String incfilename = m.group(1);
					if (!("avr/sfr_defs.h").equals(incfilename)) {
						if (incfilename.startsWith("avr")) {
							readDeviceHeader(device, m.group(1));
						}
					}
					continue;
				}

				// Test if current line contains a descriptive comment
				m = descPat.matcher(line);
				if (m.matches()) {
					// Yes: remember it and add it as a description to all
					// following
					// items
					activeDesc = m.group(1);
					continue;
				}

				if (line.trim().equals("")) {
					// but don't carry activeDesc over empty lines
					activeDesc = "";
					continue;
				}

				// Test if current line defines a Interrupt vector (new style)
				m = ivecPatNew.matcher(line);
				if (m.matches()) {
					// Yes: Add it to the interrupt vectors list
					InterruptVector ivec = null;
					String name = m.group(1);
					String vector = m.group(2);
					// test if an ivec with the old style name has already been
					// created
					List<IEntry> children = ivecCategory.getChildren();
					if (children != null) {
						for (IEntry child : children) {
							if (child.getColumnData(IVecsCategory.IDX_VECTOR).equals(vector)) {
								ivec = (InterruptVector) child;
								break;
							}
						}
					}
					if (ivec == null) {
						ivec = new InterruptVector(ivecCategory);
					}
					ivec.setName(name);
					ivec.setDescription(activeDesc);
					ivec.setVector(vector);
					continue;
				}

				// Test if current line defines a Interrupt vector (old style)
				m = ivecPatOld.matcher(line);
				if (m.matches()) {
					// Yes: Add it to the interrupt vectors list (but only as
					// SIGname)
					InterruptVector ivec = null;
					String signame = m.group(1);
					String vector = m.group(2);
					// test if an ivec with the new style name has already been
					// created (same vector number)
					List<IEntry> children = ivecCategory.getChildren();
					if (children != null) {
						for (IEntry child : children) {
							if (child.getColumnData(IVecsCategory.IDX_VECTOR).equals(vector)) {
								ivec = (InterruptVector) child;
								break;
							}
						}
					}
					if (ivec == null) {
						ivec = new InterruptVector(ivecCategory);
					}
					ivec.setSIGName(signame);
					ivec.setDescription(activeDesc);
					ivec.setVector(vector);
					continue;
				}

				// Test if current line defines a Port Register
				m = portPat.matcher(line);
				if (m.matches()) {
					// Yes: Add it to the Ports list
					Port port = new Port(portCategory);
					port.setName(m.group(1));
					port.setDescription(activeDesc);
					port.setBits(m.group(2));
					port.setAddr(m.group(3));
					continue;
				}

				// Test if current line defines a Register
				m = regPat.matcher(line);
				if (m.matches()) {
					// Yes: Add it to the Register list
					Register register = new Register(regCategory);
					register.setName(m.group(1));
					register.setDescription(activeDesc);
					register.setAddrType(m.group(2));
					register.setBits(m.group(3));
					register.setAddr(m.group(4));
				}
			}
		} catch (IOException ioe) {
			fInternalErrorMsg = "Cannot read source header file \"" + hfile.getAbsolutePath()
					+ "\".";
			throw ioe;
		}
		in.close();
		fInternalErrorMsg = null;

	}

	/**
	 * Retrieves the path to the avr/io.h file from the plugin preferences.
	 * 
	 * @return String containing the path
	 */
	protected String getAVRiohFile() {
		IPath avriohpath = fPathProvider.getPath().append("avr").append("io.h");
		return avriohpath.toOSString();
	}

	/**
	 * Retrieves the path to the include/avr directory from the Plugin preferences.
	 * 
	 * @return String containing the path
	 */
	private String getAVRIncludePath() {

		IPath includepath = fPathProvider.getPath();
		return includepath.toOSString();

	}

	public void propertyChange(PropertyChangeEvent event) {
		// check if the path to the <avr/io.h> file has changed
		if (event.getProperty().equals(AVRPath.AVRINCLUDE.name())) {
			// Yes: reset the devicelist and fire an event to all of our
			// own listeners
			fMCUNamesMap = null;
			fireProviderChangeEvent();
		}
	}

	private void fireProviderChangeEvent() {
		for (IProviderChangeListener pcl : fChangeListeners) {
			if (pcl != null) {
				pcl.providerChange();
			}
		}
	}

	public void addProviderChangeListener(IProviderChangeListener pcl) {
		if (pcl != null && !(fChangeListeners.contains(pcl))) {
			fChangeListeners.add(pcl);
		}
	}

	public void removeProviderChangeListener(IProviderChangeListener pcl) {
		if (fChangeListeners.contains(pcl)) {
			fChangeListeners.remove(pcl);
		}
	}

}
