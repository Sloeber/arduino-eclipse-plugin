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
 * $Id: TargetConfiguration.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.core.targets;

import it.baeyens.avreclipse.core.avrdude.AVRDudeException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.ListenerList;


/**
 * Implementation of the ITargetConfiguration API.
 * <p>
 * This class implements both the {@link ITargetConfiguration} and
 * {@link ITargetConfigurationWorkingCopy} interfaces, so it acts as both.
 * </p>
 * <p>
 * This class may not be instantiated directly by clients. Instances are created and managed by the
 * {@link TargetConfigurationManager}.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public class TargetConfiguration implements ITargetConfiguration, ITargetConfigurationWorkingCopy,
		ITargetConfigConstants {

	private final static String						EMPTY_STRING	= "";

	private File									fPropertiesFile;

	private String									fId;

	private boolean									fDirty;

	/** Flag to indicate that the config has been disposed. */
	private boolean									fIsDisposed		= false;

	/** The Properties container for all attributes. */
	private Properties								fAttributes		= new Properties();

	/** Map of all attributes to their default values. */
	private Map<String, String>						fDefaults		= new HashMap<String, String>();

	/**
	 * List of registered listeners (element type: <code>ITargetConfigChangeListener</code>). These
	 * listeners are to be informed when the current value of an attribute changes.
	 */
	protected ListenerList							fListeners		= new ListenerList();

	/** The source target configuration if this is a working copy */
	private TargetConfiguration						fOriginal;

	/** The current programmer tool for this target configuration. */
	private IProgrammerTool							fProgrammerTool;

	/** The current gdbserver tool for this target configuration. */
	private IGDBServerTool							fGDBServerTool;

	/** Map of the owners for attributes not handled by the this hardware configuration itself. */
	private Map<String, ITargetConfigurationTool>	fAttributeOwner	= new HashMap<String, ITargetConfigurationTool>();

	private TargetConfiguration() {
		initDefaults();
	}

	/**
	 * Instantiate a new target configuration from a given file.
	 * <p>
	 * If the file already exists, then it is loaded. Otherwise the standard attributes are set to
	 * the default values.
	 * </p>
	 * 
	 * @param path
	 *            handle to the file containing the hardware configuration attributes
	 * @throws IOException
	 *             thrown if the file exists, but can not be read.
	 */
	protected TargetConfiguration(IPath path) throws IOException {
		this();
		fPropertiesFile = path.toFile();
		fId = path.lastSegment();

		if (fPropertiesFile.exists()) {

			// If the hardware configuration file already exists we just load it
			load(fPropertiesFile);
		} else {
			// This is a brand new hardware configuration.
			// We set all attributes to their defaults and then save them.

			// need to pull in the defaults from the currently selected tools first.
			getProgrammerTool();
			getGDBServerTool();

			// Now we can set all defaults.
			restoreDefaults();

			// Immediately save the file to create the file
			save(fPropertiesFile, true);

		}
	}

	/**
	 * Make a Working copy of the given <code>TargetConfiguration</code>.
	 * <p>
	 * The copy can be safely modified without affecting the source until the {@link #doSave()}
	 * method is called, which will then copy all changes to the source configuration.
	 * </p>
	 * 
	 * @param config
	 */
	protected TargetConfiguration(TargetConfiguration config) {
		this();
		fOriginal = config;
		fId = config.fId;
		fPropertiesFile = config.fPropertiesFile;
		loadFromConfig(config);
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfiguration#getId()
	 */
	public String getId() {
		return fId;
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfiguration#getName()
	 */
	public String getName() {
		return getAttribute(ATTR_NAME);
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.core.targets.ITargetConfigurationWorkingCopy#setName(java.lang.String)
	 */
	public void setName(String name) {
		setAttribute(ATTR_NAME, name);
	}

	/**
	 * Get the user supplied description of the target configuration.
	 * 
	 * @return the Name
	 */
	public String getDescription() {
		return getAttribute(ATTR_DESCRIPTION);
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.core.targets.ITargetConfigurationWorkingCopy#setDescription(java.lang
	 * .String)
	 */
	public void setDescription(String description) {
		setAttribute(ATTR_DESCRIPTION, description);
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfiguration#getMCU()
	 */
	public String getMCU() {
		return getAttribute(ATTR_MCU);
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.core.targets.ITargetConfigurationWorkingCopy#setMCU(java.lang.String)
	 */
	public void setMCU(String mcuid) {
		setAttribute(ATTR_MCU, mcuid);
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfiguration#getFCPU()
	 */
	public int getFCPU() {
		String fcpu = getAttribute(ATTR_FCPU);
		return Integer.parseInt(fcpu);
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfigurationWorkingCopy#setFCPU(int)
	 */
	public void setFCPU(int fcpu) {
		String value = Integer.toString(fcpu);
		setAttribute(ATTR_FCPU, value);
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfiguration#getSupportedMCUs(boolean)
	 */
	public Set<String> getSupportedMCUs(boolean filtered) {
		IProgrammerTool progtool = getProgrammerTool();
		IGDBServerTool gdbserver = getGDBServerTool();

		Set<String> allmcus = new HashSet<String>();
		Set<String> progtoolmcus = null;
		Set<String> gdbservermcus = null;

		try {
			progtoolmcus = progtool.getMCUs();
		} catch (AVRDudeException e) {
			// in case of an exception we just leave the Set at null
			// so it won't be used
		}
		try {
			gdbservermcus = gdbserver.getMCUs();
		} catch (AVRDudeException e) {
			// in case of an exception we just leave the Set at null
			// so it won't be used
		}

		if (progtoolmcus != null) {
			allmcus.addAll(progtoolmcus);
		}

		if (gdbservermcus != null) {

			if (filtered && progtoolmcus != null) {
				allmcus.retainAll(gdbservermcus);
			} else {
				allmcus.addAll(gdbservermcus);
			}
		}

		return allmcus;
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfiguration#getSupportedProgrammers(boolean)
	 */
	public Set<String> getAllProgrammers(boolean supported) {
		IProgrammerTool progtool = getProgrammerTool();
		IGDBServerTool gdbserver = getGDBServerTool();

		Set<String> allprogrammers = new HashSet<String>();
		Set<String> progtoolprogrammers = null;
		Set<String> gdbserverprogrammers = null;

		try {
			progtoolprogrammers = progtool.getProgrammers();
		} catch (AVRDudeException e) {
			// in case of an exception we just leave the Set at null
			// so it won't be used
		}
		try {
			gdbserverprogrammers = gdbserver.getProgrammers();
		} catch (AVRDudeException e) {
			// in case of an exception we just leave the Set at null
			// so it won't be used
		}

		if (progtoolprogrammers != null) {
			allprogrammers.addAll(progtoolprogrammers);
		}

		if (gdbserverprogrammers != null) {

			if (supported && progtoolprogrammers != null) {
				allprogrammers.retainAll(gdbserverprogrammers);
			} else {
				allprogrammers.addAll(gdbserverprogrammers);
			}
		}
		return allprogrammers;
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfiguration#getProgrammer(java.lang.String)
	 */
	public IProgrammer getProgrammer(String programmerid) {

		// first check if the currently selected programmer tool knows the id
		// As this will usually be avrdude the chances are high that it knows
		// the programmer.
		try {
			IProgrammer progger = getProgrammerTool().getProgrammer(programmerid);
			if (progger != null) {
				return progger;
			}
		} catch (AVRDudeException ade) {
			// continue with the gdbserver
		}

		// The programmer tool didn't know the id. Maybe the gdbserver knows it.
		try {
			IProgrammer progger = getGDBServerTool().getProgrammer(programmerid);
			if (progger != null) {
				return progger;
			}
		} catch (AVRDudeException ade) {
			// continue with the other tools
		}

		// Nope. Lets go through all known tools to find one that knows this id.
		List<String> alltools = ToolManager.getDefault().getAllTools(null);
		for (String toolid : alltools) {
			try {
				ITargetConfigurationTool tool = ToolManager.getDefault().getTool(this, toolid);
				IProgrammer progger = tool.getProgrammer(programmerid);
				if (progger != null) {
					return progger;
				}
			} catch (AVRDudeException ade) {
				// just continue with the next tool
			}
		}

		// Nothing found
		// TODO: Maybe return a special "unknown" programmer.
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfiguration#getProgrammerTool()
	 */
	public IProgrammerTool getProgrammerTool() {

		if (fProgrammerTool == null) {
			// create the programmer tool if it has not yet been done.
			String id = getAttribute(ATTR_PROGRAMMER_TOOL_ID);
			ITargetConfigurationTool tool = ToolManager.getDefault().getTool(this, id);
			if (tool instanceof IProgrammerTool) {
				fProgrammerTool = (IProgrammerTool) tool;
			} else {
				// TODO throw an exception
				fProgrammerTool = null;
			}

			registerTool(fProgrammerTool);
		}

		return fProgrammerTool;
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.core.targets.ITargetConfigurationWorkingCopy#setProgrammerTool(java.lang
	 * .String)
	 */
	public void setProgrammerTool(String toolid) {
		// nothing to do if the tool is not changed
		if (fProgrammerTool != null && (fProgrammerTool.getId().equals(toolid))) {
			return;
		}

		// Check if the id is a valid programmer tool id
		List<String> programmerids = ToolManager.getDefault().getAllTools(
				ToolManager.AVRPROGRAMMERTOOL);
		if (programmerids.contains(toolid)) {
			// yes, id is valid. Get the actual tool and register its attributes
			ITargetConfigurationTool tool = ToolManager.getDefault().getTool(this, toolid);
			registerTool(tool);
			fProgrammerTool = (IProgrammerTool) tool;
			setAttribute(ATTR_PROGRAMMER_TOOL_ID, tool.getId());
		} else {
			throw new IllegalArgumentException("Invalid tool id '" + toolid + "'");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfiguration#getGDBServerTool()
	 */
	public IGDBServerTool getGDBServerTool() {

		if (fGDBServerTool == null) {
			// create the gdbserver tool if it has not yet been done.
			String id = getAttribute(ATTR_GDBSERVER_ID);
			ITargetConfigurationTool tool = ToolManager.getDefault().getTool(this, id);
			if (tool instanceof IGDBServerTool) {
				fGDBServerTool = (IGDBServerTool) tool;
				registerTool(fGDBServerTool);
			} else {
				// TODO throw an exception
				fGDBServerTool = null;
			}
		}

		return fGDBServerTool;
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.core.targets.ITargetConfigurationWorkingCopy#setGDBServerTool(java.lang
	 * .String)
	 */
	public void setGDBServerTool(String toolid) {
		// nothing to do if the tool is not changed
		if (fGDBServerTool != null && (fGDBServerTool.getId().equals(toolid))) {
			return;
		}

		// Check if the id is a valid gdbserver tool id
		List<String> programmerids = ToolManager.getDefault().getAllTools(ToolManager.AVRGDBSERVER);
		if (programmerids.contains(toolid)) {
			// yes, id is valid. Get the actual tool and register its attributes
			ITargetConfigurationTool tool = ToolManager.getDefault().getTool(this, toolid);
			registerTool(tool);
			fGDBServerTool = (IGDBServerTool) tool;
			setAttribute(ATTR_PROGRAMMER_TOOL_ID, tool.getId());
		} else {
			throw new IllegalArgumentException("Invalid tool id '" + toolid + "'");
		}
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfiguration#getAttribute(java.lang.String,
	 * java.lang.String)
	 */
	public String getAttribute(String attributeName) {
		Assert.isNotNull(attributeName);
		String value = fAttributes.getProperty(attributeName);
		if (value == null) {
			value = fDefaults.get(attributeName);
			if (value == null) {
				value = EMPTY_STRING;
			}

		}
		return value;
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.core.targets.ITargetConfigurationWorkingCopy#setAttribute(java.lang.String
	 * , java.lang.String)
	 */
	public void setAttribute(String attributeName, String newvalue) {
		Assert.isNotNull(newvalue);
		Assert.isNotNull(attributeName);
		String oldvalue = fAttributes.getProperty(attributeName);
		if (oldvalue == null || !oldvalue.equals(newvalue)) {
			// only change attribute & fire event if the value is actually changed
			fAttributes.setProperty(attributeName, newvalue);
			fireAttributeChangeEvent(attributeName, oldvalue, newvalue);
			fDirty = true;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.core.targets.ITargetConfiguration#getBooleanAttribute(java.lang.String)
	 */
	public boolean getBooleanAttribute(String attribute) {
		Assert.isNotNull(attribute);
		String value = getAttribute(attribute);
		boolean boolvalue = Boolean.parseBoolean(value);
		return boolvalue;
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.core.targets.ITargetConfigurationWorkingCopy#setBooleanAttribute(java
	 * .lang.String, boolean)
	 */
	public void setBooleanAttribute(String attribute, boolean value) {
		String valuestring = Boolean.toString(value);
		setAttribute(attribute, valuestring);
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.core.targets.ITargetConfiguration#getBooleanAttribute(java.lang.String)
	 */
	public int getIntegerAttribute(String attribute) {
		Assert.isNotNull(attribute);
		String value = getAttribute(attribute);
		if (value.length() == 0) {
			return -1;
		}
		try {
			return Integer.parseInt(value);
		} catch (NumberFormatException nfe) {
			return 0;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.core.targets.ITargetConfigurationWorkingCopy#setIntegerAttribute(java
	 * .lang.String, int)
	 */
	public void setIntegerAttribute(String attribute, int value) {
		String valuestring = Integer.toString(value);
		setAttribute(attribute, valuestring);
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfiguration#getAttributes()
	 */
	public Map<String, String> getAttributes() {
		HashMap<String, String> map = new HashMap<String, String>();
		for (Object obj : fAttributes.keySet()) {
			String key = (String) obj;
			String value = fAttributes.getProperty(key);
			map.put(key, value);
		}
		return map;
	}

	private void registerTool(ITargetConfigurationTool tool) {

		// Safety check
		if (tool == null) {
			return;
		}

		// Get all attributes supported by the tool, and add their default values to our own
		// internal list. Also remember that this tool is a handler for all its attributes.
		String[] toolattrs = tool.getAttributes();
		for (String attr : toolattrs) {
			fDefaults.put(attr, tool.getDefaultValue(attr));
			fAttributeOwner.put(attr, tool);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfigurationWorkingCopy#isDirty()
	 */
	public boolean isDirty() {
		return fDirty;
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfigurationWorkingCopy#doSave()
	 */
	public synchronized void doSave() throws IOException {

		save(fPropertiesFile, false);
	}

	private void save(File file, boolean force) throws IOException {

		// Saving a disposed config is not allowed, as it could overwrite a new config with the same
		// id.
		if (fIsDisposed) {
			throw new IllegalStateException("Config is disposed");
		}

		if (fDirty || force) {

			FileWriter reader = new FileWriter(file);
			fAttributes.store(reader, "Hardware Configuration File");
			reader.close();

			fDirty = false;

			if (fOriginal != null) {
				// Copy the changes to the original
				fOriginal.loadFromConfig(this);
			}
		}

	}

	private void load(File file) throws IOException {

		FileReader reader = new FileReader(file);
		fAttributes.load(reader);
		reader.close();

	}

	/**
	 * Load the values of this Configuration from the given <code>TargetConfiguration</code>.
	 * 
	 * @param prefs
	 *            Source <code>TargetConfiguration</code>.
	 */
	private void loadFromConfig(TargetConfiguration config) {
		fAttributes.clear();
		for (Object obj : config.fAttributes.keySet()) {
			String key = (String) obj;
			setAttribute(key, config.getAttribute(key));
		}
		fDirty = config.fDirty;
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfigurationWorkingCopy#setDefaults()
	 */
	public void restoreDefaults() {
		// Set the defaults. If
		for (String key : fDefaults.keySet()) {
			String defvalue = fDefaults.get(key);
			setAttribute(key, defvalue);
		}
	}

	/**
	 * Put all default values into the default values map.
	 */
	private void initDefaults() {
		fDefaults.put(ATTR_NAME, DEF_NAME);
		fDefaults.put(ATTR_DESCRIPTION, DEF_DESCRIPTION);
		fDefaults.put(ATTR_MCU, DEF_MCU);
		fDefaults.put(ATTR_FCPU, Integer.toString(DEF_FCPU));
		fDefaults.put(ATTR_PROGRAMMER_ID, DEF_PROGRAMMER_ID);
		fDefaults.put(ATTR_HOSTINTERFACE, DEF_HOSTINTERFACE);
		fDefaults.put(ATTR_PROGRAMMER_PORT, DEF_PROGRAMMER_PORT);
		fDefaults.put(ATTR_PROGRAMMER_BAUD, DEF_PROGRAMMER_BAUD);
		fDefaults.put(ATTR_BITBANGDELAY, DEF_BITBANGDELAY);
		fDefaults.put(ATTR_PAR_EXITSPEC, DEF_PAR_EXITSPEC);
		fDefaults.put(ATTR_USB_DELAY, DEF_USB_DELAY);
		fDefaults.put(ATTR_JTAG_CLOCK, DEF_JTAG_CLOCK);
		fDefaults.put(ATTR_DAISYCHAIN_ENABLE, DEF_DAISYCHAIN_ENABLE);
		fDefaults.put(ATTR_DAISYCHAIN_UB, DEF_DAISYCHAIN_UB);
		fDefaults.put(ATTR_DAISYCHAIN_UA, DEF_DAISYCHAIN_UA);
		fDefaults.put(ATTR_DAISYCHAIN_BB, DEF_DAISYCHAIN_BB);
		fDefaults.put(ATTR_DAISYCHAIN_BA, DEF_DAISYCHAIN_BA);
		fDefaults.put(ATTR_PROGRAMMER_TOOL_ID, DEF_PROGRAMMER_TOOL_ID);
		fDefaults.put(ATTR_GDBSERVER_ID, DEF_GDBSERVER_ID);
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfiguration#isDebugCapable()
	 */
	public boolean isDebugCapable() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfiguration#isImageLoaderCapable()
	 */
	public boolean isImageLoaderCapable() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfiguration#dispose()
	 */
	public void dispose() {
		fListeners.clear();
		fIsDisposed = true;
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.core.targets.ITargetConfiguration#addPropertyChangeListener(it.baeyens.
	 * avreclipse.core.targets.TargetConfiguration.ITargetConfigChangeListener)
	 */
	public void addPropertyChangeListener(ITargetConfigChangeListener listener) {
		fListeners.add(listener);
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.core.targets.ITargetConfiguration#removePropertyChangeListener(it.baeyens
	 * .avreclipse.core.targets.TargetConfiguration.ITargetConfigChangeListener)
	 */
	public void removePropertyChangeListener(ITargetConfigChangeListener listener) {
		fListeners.remove(listener);
	}

	/**
	 * Informs all registered listeners that an attribute has changed.
	 * 
	 * @param name
	 *            the name of the changed attribute
	 * @param oldValue
	 *            the old value, or <code>null</code> if not known or not relevant
	 * @param newValue
	 *            the new value, or <code>null</code> if not known or not relevant
	 */
	protected void fireAttributeChangeEvent(String name, String oldValue, String newValue) {
		if (name == null)
			throw new IllegalArgumentException();

		Object[] allListeners = fListeners.getListeners();

		// Don't fire anything if there are no listeners
		if (allListeners.length == 0) {
			return;
		}

		for (Object changeListener : allListeners) {
			ITargetConfigChangeListener listener = (ITargetConfigChangeListener) changeListener;
			listener.attributeChange(TargetConfiguration.this, name, oldValue, newValue);
		}
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.core.targets.ITargetConfiguration#validateAttribute(java.lang.String)
	 */
	public ValidationResult validateAttribute(String attr) {

		// First check if one of the registered tools handles the attribute
		ITargetConfigurationTool tool = fAttributeOwner.get(attr);
		if (tool != null) {
			return tool.validate(attr);
		}

		// The tools know nothing. Now go through all attributes that can be validated.
		// But first Check if the attribute is actually know.
		String value = fAttributes.getProperty(attr);
		if (value == null) {
			return new ValidationResult(Result.UNKNOWN_ATTRIBUTE, "");
		}

		if (ATTR_MCU.equals(attr)) {
			return AVRHardwareConfigValidator.checkMCU(this);

		} else if (ATTR_PROGRAMMER_ID.equals(attr)) {
			return AVRHardwareConfigValidator.checkProgrammer(this);

		} else if (ATTR_JTAG_CLOCK.equals(attr)) {
			return AVRHardwareConfigValidator.checkJTAGClock(this);

		} else if (ATTR_DAISYCHAIN_BB.equals(attr)) {
			return AVRHardwareConfigValidator.checkJTAGDaisyChainBitsBefore(this);

		} else if (ATTR_DAISYCHAIN_BA.equals(attr)) {
			return AVRHardwareConfigValidator.checkJTAGDaisyChainBitsAfter(this);

		} else if (ATTR_DAISYCHAIN_UB.equals(attr)) {
			return AVRHardwareConfigValidator.checkJTAGDaisyChainUnitsBefore(this);

		} else if (ATTR_DAISYCHAIN_UA.equals(attr)) {
			return AVRHardwareConfigValidator.checkJTAGDaisyChainUnitsAfter(this);

		}

		// TODO Auto-generated method stub
		return new ValidationResult(Result.OK, "");
	}

}
