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
 * $Id: TargetConfigurationManager.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.core.targets;

import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.core.avrdude.ProgrammerConfig;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;


/**
 * Manages the list of {@link TargetConfiguration} objects in the preferences.
 * <p>
 * This manager has methods to
 * <ul>
 * <li>get a configuration from the preferences: {@link #getConfig(String)},</li>
 * <li>get a working copy configuration: {@link #getWorkingCopy(String)},</li>
 * <li>create a new configuration: {@link #createNewConfig()},</li>
 * <li>save a configuration: {@link #saveConfig(ProgrammerConfig)} and</li>
 * <li>delete a configuration: {@link #deleteConfig(ProgrammerConfig)}</li>
 * </p>
 * <p>
 * The manager also has methods to get a list of all available programmers and their names:
 * {@link #getAllConfigIDs()} and {@link #getAllConfigNames()}.
 * </p>
 * <p>
 * To improve access times all retrieved configurations are stored in an internal cache.
 * </p>
 * <p>
 * This class implements the singleton pattern and can be accessed with the static
 * {@link #getDefault()} method.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.4
 * 
 */
public class TargetConfigurationManager {

	/** Static singleton instance */
	private static TargetConfigurationManager		fInstance		= null;

	private IPath									fStorageFolder;

	/**
	 * The prefix for programmer configuration id values. This is appended with a running number to
	 * get the real id.
	 */
	private final static String						CONFIG_PREFIX	= "config.";

	/** Cache of all Configs that have been used in this session */
	private final Map<String, TargetConfiguration>	fConfigsCache;

	/**
	 * Gets the session <code>TargetConfigurationManager</code>.
	 * 
	 * @return <code>TargetConfigurationManager</code> for the current Eclipse session.
	 */
	public static TargetConfigurationManager getDefault() {
		if (fInstance == null) {
			fInstance = new TargetConfigurationManager();
		}
		return fInstance;
	}

	// Private to prevent instantiation of this class.
	private TargetConfigurationManager() {
		fConfigsCache = new HashMap<String, TargetConfiguration>();
	}

	/**
	 * Create a new TargetConfiguration.
	 * <p>
	 * The returned TargetConfiguration is filled with some default values. It is immediately
	 * created in the preference store.
	 * </p>
	 * 
	 * @return A new <code>ProgrammerConfig</code>
	 * @throws IOException
	 *             An <code>IOException</code> is thrown when the new config file can not be
	 *             created.
	 */
	public ITargetConfiguration createNewConfig() throws IOException {

		// The id has the form "targetconfig.#" where # is a running
		// number.

		// Get all files in the folder and find the highest ID.

		IPath folder = getConfigFolder();
		File[] files = folder.toFile().listFiles();

		int maxid = 0;
		for (File file : files) {
			if (file.isFile()) {
				String name = file.getName();
				if (name.startsWith(CONFIG_PREFIX)) {
					int dot = name.lastIndexOf('.');
					if (dot >= 0) {
						String idstring = name.substring(dot + 1);
						int id = Integer.parseInt(idstring);
						if (id >= maxid) {
							maxid = id + 1;
						}
					}
				}
			}
		}

		String filename = CONFIG_PREFIX + Integer.toString(maxid);

		IPath newfile = new Path(folder.toString()).append(filename);

		TargetConfiguration newconfig = new TargetConfiguration(newfile);
		fConfigsCache.put(newconfig.getId(), newconfig);

		return newconfig;

	}

	/**
	 * Deletes the given configuration.
	 * 
	 * @param id
	 *            The id of the target configuration to delete.
	 */
	public void deleteConfig(String id) throws IOException {

		// If the config is in the cache, remove it from the cache
		if (fConfigsCache.containsKey(id)) {
			// First clear any listeners so that we don't have dangling references
			TargetConfiguration tc = fConfigsCache.get(id);
			tc.dispose();
			fConfigsCache.remove(id);
		}

		// Delete the config file
		File file = getConfigFolder().append(id).toFile();
		if (file.exists()) {
			if (!file.delete()) {
				throw new IOException("Could not delete hardware config file '" + file.toString()
						+ "'");
			}
		}
	}

	/**
	 * Get the {@link TargetConfiguration} with the given ID.
	 * <p>
	 * If the config has been requested before, a reference to the config in the internal cache is
	 * returned. All modifications to the returned config will affect the config in the cache.
	 * </p>
	 * <p>
	 * While these changes are only persisted when saveConfig() is called, it is usually better to
	 * use the {@link #getWorkingCopy(String)} call to get a safely modifiable config.
	 * </p>
	 * 
	 * @see #getWorkingCopy(String)
	 * 
	 * @param id
	 *            <code>String</code> with an ID value.
	 * @return The requested <code>TargetConfiguration</code> or <code>null</code> if no config with
	 *         the given ID exists.
	 * @throws IOException
	 *             if a config file exists in the storage area, but could not be read.
	 */
	public ITargetConfiguration getConfig(String id) throws IOException {

		return internalGetConfig(id);
	}

	private TargetConfiguration internalGetConfig(String id) throws IOException {
		// Test for empty / null id
		if (id == null || id.length() == 0) {
			return null;
		}

		// Test if the config is already in the cache
		if (fConfigsCache.containsKey(id)) {
			return fConfigsCache.get(id);
		}

		// The config was not in the cache

		// The file must exist, otherwise return null
		IPath file = getConfigFolder().append(id);
		if (!file.toFile().exists()) {
			return null;
		}

		// Load the Config from the File
		TargetConfiguration config = new TargetConfiguration(file);

		fConfigsCache.put(id, config);

		return config;
	}

/**
	 * Get a working copy of the {@link TargetConfiguration} with the given Id.
	 * <p>
	 * The returned config is not backed by the cache, so any modifications will not be visible
	 * until the {@link #saveConfig(ProgrammerConfig) method is called with the returned config.<p>
	 * </p>
	 * 
	 * @param sourceconfig
	 *            Source <code>TargetConfiguration</code> to clone.
	 * @return New working copy of an existing configuration, or <code>null</code> if no config with the given id exists.
	 * @throws IOException when the source config exists, but can not be loaded from the storage area.
	 */
	public ITargetConfigurationWorkingCopy getWorkingCopy(String id) throws IOException {

		// Clone the source config
		TargetConfiguration sourceconfig = internalGetConfig(id);
		if (sourceconfig == null) {
			return null;
		}
		ITargetConfigurationWorkingCopy cloneconfig = new TargetConfiguration(sourceconfig);

		return cloneconfig;
	}

	/**
	 * Checks if a target configuration with the given id exists.
	 * 
	 * @param id
	 *            A target configuration id string
	 * @return <code>true</code> if the configuration exists.
	 */
	public boolean exists(String id) {
		// Test for empty / null id
		if (id == null)
			return false;
		if (id.length() == 0)
			return false;

		// Test if the config is already in the cache
		if (fConfigsCache.containsKey(id)) {
			return true;
		}

		// The config was not in the cache

		// The file must exist, otherwise return null
		try {
			File file = getConfigFolder().append(id).toFile();
			return file.exists();
		} catch (IOException ioe) {
			return false;
		}
	}

	/**
	 * Get a list of all available target configuration id's.
	 * 
	 * @return List of all id strings
	 */
	public List<String> getConfigurationIDs() {
		final List<String> confignames = new ArrayList<String>();

		try {
			File folder = getConfigFolder().toFile();
			File[] allfiles = folder.listFiles();

			for (File file : allfiles) {
				String filename = file.getName();
				if (filename.startsWith(CONFIG_PREFIX)) {
					confignames.add(filename);
				}
			}
		} catch (IOException ioe) {
			// In case of errors return what is already in the list
		}

		return confignames;
	}

	private IPath getConfigFolder() throws IOException {
		if (fStorageFolder == null) {
			IPath location = AVRPlugin.getDefault().getStateLocation().append("hardwareconfigs");
			File folder = location.toFile();
			if (!folder.exists()) {
				if (!folder.mkdirs()) {
					throw new IOException("Could not create hardware config storage folder '"
							+ folder.toString() + "'");
				}
			}
			fStorageFolder = location;
		}
		return fStorageFolder;

	}

}
