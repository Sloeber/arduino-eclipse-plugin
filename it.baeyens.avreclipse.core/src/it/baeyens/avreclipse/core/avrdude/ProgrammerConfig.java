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
 * $Id: ProgrammerConfig.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.avrdude;

import it.baeyens.arduino.common.ArduinoConst;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * Container class for all Programmer specific options of AVRDude.
 * <p>
 * This class also acts as an Interface to the preference store. It knows how to save and delete
 * configurations.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * @since 2.3 added invocation delay option
 * 
 */
public class ProgrammerConfig {

	/** The unique identifier for this configuration */
	private final String		fId;
	public final static String	KEY_ID						= "id";

	/** The unique name of this configuration */
	private String				fName;
	public final static String	KEY_NAME					= "name";

	/** A custom description of this configuration */
	private String				fDescription;
	public final static String	KEY_DESCRIPTION				= "description";
	public final static String	DEFAULT_DESCRIPTION			= "Default AVRDude Programmer Configuration. Modify as required for your setup.";

	/** The avrdude id of the programmer for this configuration */
	private String				fProgrammer;
	public final static String	KEY_PROGRAMMER				= "programmer";
	public final static String	DEFAULT_PROGRAMMER			= "stk500v2";

	/**
	 * The port for this configuration. If empty it will not be included in the command line
	 * arguments.
	 */
	private String				fPort;
	public final static String	KEY_PORT					= "port";

	/**
	 * The baudrate for this configuration. If empty it will not be included in the command line
	 * arguments.
	 */
	private String				fBaudrate;
	public final static String	KEY_BAUDRATE				= "baudrate";

	/**
	 * The Exitspec for the resetline. If empty it will not be included in the command line
	 * arguments.
	 * <p>
	 * Valid values are "reset", "noreset" and ""
	 * </p>
	 */
	private String				fExitReset;
	public final static String	KEY_EXITSPEC_RESET			= "ppresetline";

	/**
	 * The Exitspec for the Vcc lines. If empty or <code>null</code> it will not be included in
	 * the command line arguments.
	 * <p>
	 * Valid values are "vcc", "novcc" and ""
	 * </p>
	 */
	private String				fExitVcc;
	public final static String	KEY_EXITSPEC_VCC			= "ppvccline";

	/**
	 * The optional delay in milliseconds to delay multiple successive calls to avrdude.
	 * <p>
	 * To be used if avrdude does not release its output port fast enough, causing "port blocked"
	 * failures.
	 * </p>
	 * <p>
	 * May be <code>null</code> or empty. If not empty it must contain an integer number.
	 * </p>
	 * <p>
	 * Unlike the other parameters in this class this is not used on the avrdude command line. But
	 * all classes that make successive calls to avrdude must respect this delay.
	 * </p>
	 * 
	 */
	private String				fPostAVRDudeDelay;
	public final static String	KEY_POSTAVRDUDE_DELAY_MS	= "postAvrdudeDelayMs";

	/** Flag to mark modifications of this config */
	private boolean				fDirty;

	/**
	 * Constructs a ProgrammerConfig with the given id and set the default values.
	 * 
	 * @param id
	 *            Unique id of the configuration.
	 */
	protected ProgrammerConfig(String id) {
		fId = id;
		fDirty = false;
		defaults();
	}

	/**
	 * Constructs a ProgrammerConfig with the given id and load its values from the given
	 * <code>Preferences</code>.
	 * 
	 * @param id
	 *            Unique id of the configuration.
	 * @param prefs
	 *            <code>Preferences</code> node from which to load.
	 */
	protected ProgrammerConfig(String id, Preferences prefs) {
		fId = id;
		fDirty = false;
		loadFromPrefs(prefs);
	}

	/**
	 * Make a copy of the given <code>ProgrammerConfig</code>.
	 * <p>
	 * The copy does not reflect any changes of the original or vv.
	 * </p>
	 * Note: This copy can be saved, even when the given original has been deleted.
	 * </p>
	 * 
	 * @param config
	 */
	protected ProgrammerConfig(ProgrammerConfig config) {
		fId = config.fId;
		loadFromConfig(config);
	}

	/**
	 * Persist this configuration to the preference storage.
	 * <p>
	 * This will not do anything if the configuration has not been modified.
	 * </p>
	 * 
	 * @throws BackingStoreException
	 *             If this configuration cannot be written to the preference storage area.
	 */
	protected synchronized void save(Preferences prefs) throws BackingStoreException {

		if (fDirty) {
			// write all values to the preferences
			prefs.put(KEY_NAME, fName);
			prefs.put(KEY_DESCRIPTION, fDescription);
			prefs.put(KEY_PROGRAMMER, fProgrammer);
			prefs.put(KEY_PORT, fPort);
			prefs.put(KEY_BAUDRATE, fBaudrate);
			prefs.put(KEY_EXITSPEC_RESET, fExitReset);
			prefs.put(KEY_EXITSPEC_VCC, fExitVcc);
			prefs.put(KEY_POSTAVRDUDE_DELAY_MS, fPostAVRDudeDelay);

			// flush the Preferences to the persistent storage
			prefs.flush();
		}
	}

	/**
	 * @return A <code>List&lt;Strings&gt;</code> with all avrdude options as defined by this
	 *         configuration
	 */
	public List<String> getArguments() {

		List<String> args = new ArrayList<String>();

		args.add("-c" + fProgrammer);

		if (fPort.length() > 0) {
			args.add( ArduinoConst.UploadPortPrefix() + fPort);
		}

		if (fBaudrate.length() > 0) {
			args.add("-b" + fBaudrate);
		}

		StringBuffer exitspec = new StringBuffer();
		if (fExitReset.length() > 0) {
			exitspec.append(fExitReset);
		}
		if (fExitVcc.length() > 0) {
			if (fExitReset.length() > 0) {
				exitspec.append(",");
			}
			exitspec.append(fExitVcc);
		}
		if (exitspec.length() > 0) {
			args.add("-E" + exitspec.toString());
		}
		return args;
	}

	/**
	 * Gets the ID of this configuration.
	 * 
	 * @return <code>String</code> with the ID.
	 */
	public String getId() {
		return fId;
	}

	/**
	 * Sets the name of this configuration.
	 * <p>
	 * The name must not contain any slashes ('/'), as this would cause problems with the preference
	 * store.
	 * </p>
	 * 
	 * @param name
	 *            <code>String</code> with the new name.
	 */
	public void setName(String name) {
		Assert.isTrue(!name.contains("/"));
		fName = name;
		fDirty = true;
	}

	/**
	 * @return The current name of this configuration.
	 */
	public String getName() {
		return fName;
	}

	/**
	 * Sets the description of this configuration.
	 * 
	 * @param name
	 *            <code>String</code> with the new description.
	 */
	public void setDescription(String description) {
		fDescription = description;
		fDirty = true;
	}

	/**
	 * @return The current description of this configuration.
	 */
	public String getDescription() {
		return fDescription;
	}

	/**
	 * Sets the avrdude programmer id of this configuration.
	 * <p>
	 * The programmer id is not checked for validity. It is up to the caller to ensure that the
	 * given id is valid.
	 * </p>
	 * 
	 * @param name
	 *            <code>String</code> with the new programmer id.
	 */
	public void setProgrammer(String programmer) {
		fProgrammer = programmer;
		fDirty = true;
	}

	/**
	 * @return The current avrdude programmer id of this configuration.
	 */
	public String getProgrammer() {
		return fProgrammer;
	}

	/**
	 * Sets the port of this configuration.
	 * <p>
	 * The port name is not checked for validity. It is up to the caller to ensure that the port
	 * name is valid.
	 * </p>
	 * 
	 * @param name
	 *            <code>String</code> with the new port, may be an empty String to use the avrdude
	 *            default port.
	 */
	public void setPort(String port) {
		fPort = port;
		fDirty = true;
	}

	/**
	 * @return The current port of this configuration, empty if default is to be used.
	 */
	public String getPort() {
		return fPort;
	}

	/**
	 * Sets the baudrate of this configuration.
	 * <p>
	 * The baudrate is not checked for validity. It is up to the caller to ensure that the baudrate
	 * is a valid integer (or empty).
	 * </p>
	 * 
	 * @param name
	 *            <code>String</code> with the new baudrate, may be an empty String to use the
	 *            avrdude default baudrate.
	 */
	public void setBaudrate(String baudrate) {
		fBaudrate = baudrate;
		fDirty = true;
	}

	/**
	 * @return The current baudrate of this configuration, empty if default is to be used.
	 */
	public String getBaudrate() {
		return fBaudrate;
	}

	/**
	 * Sets the reset line ExitSpec of this configuration.
	 * <p>
	 * Only the values "reset", "noreset" and "" (empty String) are valid. It is up to the caller to
	 * ensure that the given value is valid.
	 * </p>
	 * 
	 * @param name
	 *            <code>String</code> with the resetline ExitSpec, may be an empty String to use
	 *            the avrdude default.
	 */
	public void setExitspecResetline(String resetline) {
		fExitReset = resetline;
		fDirty = true;
	}

	/**
	 * @return The current reset line ExitSpec of this configuration, empty if default is to be
	 *         used.
	 */
	public String getExitspecResetline() {
		return fExitReset;
	}

	/**
	 * Sets the Vcc lines ExitSpec of this configuration.
	 * <p>
	 * Only the values "vcc", "novcc" and "" (empty String) are valid.It is up to the caller to
	 * ensure that the given value is valid.
	 * </p>
	 * 
	 * @param name
	 *            <code>String</code> with the resetline ExitSpec, may be an empty String to use
	 *            the avrdude default.
	 */
	public void setExitspecVCCline(String vccline) {
		fExitVcc = vccline;
		fDirty = true;
	}

	/**
	 * @return The current Vcc lines ExitSpec of this configuration, empty if default is to be used.
	 */
	public String getExitspecVCCline() {
		return fExitVcc;
	}

	/**
	 * Sets the post avrdude delay value in milliseconds.
	 * <p>
	 * The delay value is not checked for validity. It is up to the caller to ensure that the value
	 * is a valid integer (or empty).
	 * </p>
	 * 
	 * @param delay
	 *            String with integer value.
	 */
	public void setPostAvrdudeDelay(String delay) {
		fPostAVRDudeDelay = delay;
		fDirty = true;
	}

	/**
	 * Get the selected post avrdude delay value in milliseconds.
	 * 
	 * @return Selected delay value or an empty string if no delay is required.
	 */
	public String getPostAvrdudeDelay() {
		return fPostAVRDudeDelay;
	}

	/**
	 * Load the values of this Configuration from the preference storage area.
	 * 
	 * @param prefs
	 *            <code>Preferences</code> node for this configuration
	 */
	private void loadFromPrefs(Preferences prefs) {
		fName = prefs.get(KEY_NAME, "");
		fDescription = prefs.get(KEY_DESCRIPTION, "");
		fProgrammer = prefs.get(KEY_PROGRAMMER, "");
		fPort = prefs.get(KEY_PORT, "");
		fBaudrate = prefs.get(KEY_BAUDRATE, "");
		fExitReset = prefs.get(KEY_EXITSPEC_RESET, "");
		fExitVcc = prefs.get(KEY_EXITSPEC_VCC, "");
		fPostAVRDudeDelay = prefs.get(KEY_POSTAVRDUDE_DELAY_MS, "");
	}

	/**
	 * Load the values of this Configuration from the given <code>ProgrammerConfig</code>.
	 * 
	 * @param prefs
	 *            Source <code>ProgrammerConfig</code>.
	 */
	protected void loadFromConfig(ProgrammerConfig config) {
		fName = config.fName;
		fDescription = config.fDescription;
		fProgrammer = config.fProgrammer;
		fPort = config.fPort;
		fBaudrate = config.fBaudrate;
		fExitReset = config.fExitReset;
		fExitVcc = config.fExitVcc;
		fDirty = config.fDirty;
		fPostAVRDudeDelay = config.fPostAVRDudeDelay;
	}

	/**
	 * Reset this Configuration to the default values.
	 * <p>
	 * The ID and the Name of this Configuration are not changed.
	 * </p>
	 */
	public void defaults() {
		// Set the defaults
		fDescription = DEFAULT_DESCRIPTION;
		fProgrammer = DEFAULT_PROGRAMMER;
		fPort = "";
		fBaudrate = "";
		fExitReset = "";
		fExitVcc = "";
		fPostAVRDudeDelay = "";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		// for the debugger
		return fName + " (" + fDescription + "): " + getArguments();
	}

}
