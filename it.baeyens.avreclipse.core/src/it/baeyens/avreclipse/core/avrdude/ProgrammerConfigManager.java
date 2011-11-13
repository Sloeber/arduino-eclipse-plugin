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
 * $Id: ProgrammerConfigManager.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.avrdude;


import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.core.preferences.AVRDudePreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;


/**
 * Manages the list of {@link ProgrammerConfig} objects in the preferences.
 * <p>
 * This manager has methods to
 * <ul>
 * <li>get configs from the preferences: {@link #getConfig(String)},</li>
 * <li>create new configs: {@link #createNewConfig()},</li>
 * <li>save configs: {@link #saveConfig(ProgrammerConfig)} and</li>
 * <li>delete configs: {@link #deleteConfig(ProgrammerConfig)}</li>
 * </p>
 * <p>
 * The manager also has methods to get a list of all available programmers and their names:
 * {@link #getAllConfigIDs()} and {@link #getAllConfigNames()}.
 * </p>
 * <p>
 * To improve access times all retreived configs are stored in an internal cache.
 * </p>
 * <p>
 * This class implements the singleton pattern and can be accessed with the static
 * {@link #getDefault()} method.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 * 
 */
public class ProgrammerConfigManager {

	/**
	 * The prefix for programmer configuration id values. This is appended with a running number to
	 * get the real id.
	 */
	private final static String					CONFIG_PREFIX	= "programmerconfig.";

	/** Static singleton instance */
	private static ProgrammerConfigManager		fInstance		= null;

	/** Cache of all Configs that have been used in this session */
	private final Map<String, ProgrammerConfig>	fConfigsCache;

	/**
	 * List of all IDs that have been given out for new configs, but have not yet been saved. Used
	 * to avoid duplicate IDs, even when {@link #createNewConfig()} gets called multiple times.
	 */
	private final List<String>					fPendingIds;

	/**
	 * The preferences this manager works on.
	 * 
	 * @see AVRDudePreferences#getConfigPreferences()
	 */
	private final IEclipsePreferences			fPreferences;

	/**
	 * Gets the session <code>ProgrammerConfigManager</code>.
	 * 
	 * @return <code>ProgrammerConfigManager</code> for the current Eclipse session.
	 */
	public static ProgrammerConfigManager getDefault() {
		if (fInstance == null) {
			fInstance = new ProgrammerConfigManager();
		}
		return fInstance;
	}

	// Private to prevent instantiation of this class.
	private ProgrammerConfigManager() {
		// Set up Preferences and the internal lists
		fPreferences = AVRDudePreferences.getConfigPreferences();
		fConfigsCache = new HashMap<String, ProgrammerConfig>();
		fPendingIds = new ArrayList<String>();
	}

	/**
	 * Create a new ProgrammerConfig.
	 * <p>
	 * The returned ProgrammerConfig is filled with some default values. It is not created in the
	 * preference store.
	 * </p>
	 * <p>
	 * Call {@link #saveConfig(ProgrammerConfig)} with the returned config to persist any
	 * modifications and to add the newly created config to the list of all existing configs.
	 * </p>
	 * 
	 * @return A new <code>ProgrammerConfig</code>
	 */
	public ProgrammerConfig createNewConfig() {
		// The id has the form "programmerconfig.#" where # is a running
		// number.

		// Find the first free id.
		// Check both the list of existing config nodes in the preferences and
		// the list of pending config ids (ids that have been assigned, but not
		// yet saved).
		String newid = null;
		int i = 1;

		try {
			do {
				newid = CONFIG_PREFIX + i++;
			} while (fPreferences.nodeExists(newid) || fPendingIds.contains(newid));
		} catch (BackingStoreException bse) {
			// TODO What shall we do if we can't access the Preferences?
			// For now log an error and return null.
			logException(bse);
			return null;
		}
		ProgrammerConfig newconfig = new ProgrammerConfig(newid);

		fPendingIds.add(newid);

		return newconfig;
	}

	/**
	 * Get the {@link ProgrammerConfig} with the given ID.
	 * <p>
	 * If the config has been requested before, a reference to the config in the internal cache is
	 * returned. All modifications to the returned config will affect the config in the cache.
	 * </p>
	 * <p>
	 * While these changes are only persisted when saveConfig() is called, it is usually better to
	 * use the {@link #getConfigEditable(ProgrammerConfig)} call to get a safely modifiable config.
	 * </p>
	 * 
	 * @see #getConfigEditable(ProgrammerConfig)
	 * 
	 * @param id
	 *            <code>String</code> with an ID value.
	 * @return The requested <code>ProgrammerConfig</code> or <code>null</code> if no config
	 *         with the given ID exists.
	 */
	public ProgrammerConfig getConfig(String id) {

		// Test for empty / null id
		if (id == null)
			return null;
		if (id.length() == 0)
			return null;

		// Test if the config is already in the cache
		if (fConfigsCache.containsKey(id)) {
			return fConfigsCache.get(id);
		}

		// The config was not in the cache

		// The node must exist, otherwise return null
		try {

			if (!fPreferences.nodeExists(id)) {
				return null;
			}
		} catch (BackingStoreException bse) {
			// TODO What shall we do if we can't access the Preferences?
			// For now log an error and return null.
			logException(bse);
			return null;
		}

		// Load the Config from the Preferences
		Preferences cfgprefs = fPreferences.node(id);
		ProgrammerConfig config = new ProgrammerConfig(id, cfgprefs);

		fConfigsCache.put(id, config);

		return config;
	}

	/**
	 * Return an safely modifiable copy of the given config.
	 * <p>
	 * The returned config is not backed by the cache, so any modifications will not be visible
	 * until the {@link #saveConfig(ProgrammerConfig) method is called with the returned config.<p>
	 * </p>
	 * 
	 * @param sourceconfig
	 *            Source <code>ProgrammerConfig</code> to clone.
	 * @return New <code>ProgrammerConfig</code> with the same properties as the source config.
	 */
	public ProgrammerConfig getConfigEditable(ProgrammerConfig sourceconfig) {

		// Clone the source config
		ProgrammerConfig cloneconfig = new ProgrammerConfig(sourceconfig);

		return cloneconfig;
	}

	/**
	 * Test if the given ID is valid, i.e. a <code>ProgrammerConfig</code> with the given ID
	 * exists in the Preferences.
	 * 
	 * @param id
	 *            <code>String</code> with the ID value to test
	 * @return <code>true</code> if a config with the given ID exists in the Preferences.
	 */
	public boolean isValidId(String id) {
		// Test the cache first (quicker)
		if (fConfigsCache.containsKey(id)) {
			return true;
		}

		// Not in the cache, try the preferences
		try {
			if (fPreferences.nodeExists(id)) {
				return true;
			}
		} catch (BackingStoreException bse) {
			// TODO What shall we do if we can't access the Preferences?
			// For now log an error and return false.
			logException(bse);
		}

		// Id not found anywhere
		return false;
	}

	/**
	 * Deletes the given configuration from the preference storage area.
	 * <p>
	 * Note: This Object is still valid and further calls to {@link #saveConfig(ProgrammerConfig)}
	 * will add this configuration back to the preference storage.
	 * </p>
	 * 
	 * @throws BackingStoreException
	 */
	public void deleteConfig(ProgrammerConfig config) throws BackingStoreException {

		String id = config.getId();

		// If the config is in the cache, remove it from the cache
		if (fConfigsCache.containsKey(id)) {
			fConfigsCache.remove(id);
		}

		// Remove the Preference node for the config and flush the preferences
		// If the node does not exist do nothing - no need to create the node
		// just to remove it again
		if (fPreferences.nodeExists(id)) {
			Preferences cfgnode = fPreferences.node(id);
			cfgnode.removeNode();
			fPreferences.flush();
		}
	}

	/**
	 * Save the given Configuration to the persistent storage.
	 * <p>
	 * Only {@link ProgrammerConfig} objects obtained with the {@link #getConfigEditable(String)}
	 * method should be passed to this method, however this is currently not enforced.
	 * </p>
	 * 
	 * @param config
	 *            <code>ProgrammerConfig</code> to be saved.
	 * @throws BackingStoreException
	 *             If this configuration cannot be written to the preference storage area.
	 */
	public void saveConfig(ProgrammerConfig config) throws BackingStoreException {

		// save the config
		config.save(getConfigPreferences(config));

		// If the config is already in the cache update the cached config to the
		// new values
		if (fConfigsCache.containsKey(config.getId())) {
			ProgrammerConfig oldconfig = fConfigsCache.get(config.getId());
			oldconfig.loadFromConfig(config);
		} else {
			// Add the new config to the cache
			fConfigsCache.put(config.getId(), config);
		}

		// Remove from the pending id list (if it is a newly created config)
		fPendingIds.remove(config.getId());
	}

	/**
	 * Gets a list of all Programmer configuration ID values currently in the preferences.
	 * 
	 * @return <code>Set&lt;String&gt;</code> with all configuration id values.
	 */
	public Set<String> getAllConfigIDs() {
		// All Programmer Configurations are children of the rootnode in the
		// preferences. So fetch all children and add them to a Set.
		Set<String> allconfigs = new HashSet<String>();
		String[] confignames;

		try {
			confignames = fPreferences.childrenNames();
		} catch (BackingStoreException bse) {
			// TODO What shall we do if we can't access the Preferences?
			// For now log an error and return an empty list
			logException(bse);
			return allconfigs;
		}

		for (String conf : confignames) {
			allconfigs.add(conf);
		}

		return allconfigs;
	}

	/**
	 * Get a list of all programmer configuration names currently in the preferences.
	 * <p>
	 * The list is returned as a mapping of id values to config names.
	 * </p>
	 * 
	 * @return <code>Map&lt;String id, String name&gt;</code>
	 */
	public Map<String, String> getAllConfigNames() {
		// Get all configs ids, then load all configs and
		// add their names to a Map
		Set<String> allids = getAllConfigIDs();

		Map<String, String> idNameMap = new HashMap<String, String>(allids.size());

		for (String id : allids) {
			ProgrammerConfig cfg = getConfig(id);
			idNameMap.put(id, cfg.getName());
		}
		return idNameMap;

	}

	
	/**
	 * Get a config based on a name.
	 * <p>
	 * The first item in the config list with a matching name is returned.
	 * in all other cases returns null
	 * </p>
	 * 
	 * @return <code>ProgrammerConfig;</code>
	 */
	public ProgrammerConfig getConfigByName(String ConfigName) {
		// Get all configs ids, then load all configs and
		// add their names to a Map
		Set<String> allids = getAllConfigIDs();

		for (String id : allids) {
			ProgrammerConfig cfg = getConfig(id);
			if (cfg.getName().equals( ConfigName)) return cfg;
		}
		return null;
	}
	
	/**
	 * Get the preference node for the given configuration
	 * 
	 * @param config
	 * @return
	 */
	private Preferences getConfigPreferences(ProgrammerConfig config) {
		String id = config.getId();
		return fPreferences.node(id);

	}

	/**
	 * Log an BackingStoreException.
	 * 
	 * @param bse
	 *            <code>BackingStoreException</code> to log.
	 */
	private void logException(BackingStoreException bse) {
		// TODO Check if we really should do this here or if we just throw the
		// Exception all the way up to the GUI code to show an error dialog,
		// like in the saveConfig() and deleteConfig() methods (where this
		// Exception is more likely to happen as something is actually written
		// to the Preferences)
		// 
		Status status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID,
				"Can't access the list of avrdude configuration preferences", bse);
		AVRPlugin.getDefault().log(status);
	}
}
