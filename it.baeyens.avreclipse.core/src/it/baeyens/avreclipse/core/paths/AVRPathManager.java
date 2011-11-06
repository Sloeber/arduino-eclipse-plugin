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
 * $Id: AVRPathManager.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.paths;

import it.baeyens.avreclipse.core.preferences.AVRPathsPreferences;

import java.io.File;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.IPreferenceStore;


public class AVRPathManager implements IPathProvider {

	public enum SourceType {
		Bundled, System, Custom
	}

	private IPreferenceStore	fPrefs;
	private final AVRPath		fAvrPath;

	private String				fPrefsValue	= null;

	/**
	 * Creates a PathProvider for the instance Preference Store and AVRPath.
	 * 
	 */
	public AVRPathManager(AVRPath avrpath) {
		this(AVRPathsPreferences.getPreferenceStore(), avrpath);
	}

	/**
	 * Creates a PathProvider for the given Preference Store and AVRPath.
	 * 
	 */
	public AVRPathManager(IPreferenceStore store, AVRPath avrpath) {
		fPrefs = store;
		fAvrPath = avrpath;
	}

	/**
	 * Creates a copy of the given AVRPathManager.
	 * 
	 * @param pathmanager
	 */
	public AVRPathManager(AVRPathManager pathmanager) {
		this(pathmanager.fPrefs, pathmanager.fAvrPath);
		fPrefsValue = pathmanager.fPrefsValue;
	}

	/**
	 * Gets the UI name of the underlying AVRPath.
	 * 
	 * @return String with the name
	 */
	public String getName() {
		return fAvrPath.toString();
	}

	/**
	 * Gets a description from the underlying AVRPath.
	 * 
	 * @return String with the description of the path
	 */
	public String getDescription() {
		return fAvrPath.getDescription();
	}

	/**
	 * Gets the current path.
	 * 
	 * This is different from IPathProvider.getPath() because the returned path is cached internally
	 * and can be modified with the setPath() method.
	 * 
	 * 
	 * @return <code>IPath</code>
	 */
	public IPath getPath() {
		// get the path from the preferences store and returns its value,
		// depending on the selected path source

		if (fPrefsValue == null) {
			fPrefsValue = fPrefs.getString(fAvrPath.name());
		}

		if (fPrefsValue.equals(AVRPathManager.SourceType.System.name())) {
			// System path
			return getSystemPath(false);
		}

		if (fPrefsValue.startsWith(AVRPathManager.SourceType.Bundled.name())) {
			// Bundle path
			String bundleid = fPrefsValue.substring(fPrefsValue.indexOf(':') + 1);
			return getBundlePath(bundleid);
		}
		// else: a custom path
		IPath path = new Path(fPrefsValue);
		return path;
	}

	/**
	 * Gets the default path.
	 * 
	 * @return <code>IPath</code> to the default source directory
	 */
	public IPath getDefaultPath() {
		// Don't want to duplicate the parsing done in getPath() so
		// just set the current value to the default, call getPath and
		// restore the current value afterward.
		String defaultvalue = fPrefs.getDefaultString(fAvrPath.name());
		String oldPrefsValue = fPrefsValue;
		fPrefsValue = defaultvalue;
		IPath defaultpath = getPath();
		fPrefsValue = oldPrefsValue;
		return defaultpath;
	}

	/**
	 * Gets the system path.
	 * 
	 * This is the path as determined by system path / windows registry.
	 * 
	 * @param force
	 *            If <code>true</code> reload the system path directly, without using any cached
	 *            values.
	 * 
	 * @return <code>IPath</code> to the system dependent source directory
	 */
	public IPath getSystemPath(boolean force) {
		return SystemPathHelper.getPath(fAvrPath, force);
	}

	/**
	 * Gets the path from the Eclipse bundle with the given id.
	 * 
	 * @param bundleid
	 *            ID of the source bundle
	 * @return <code>IPath</code> to the source directory within the bundle.
	 */
	public IPath getBundlePath(String bundleid) {
		return BundlePathHelper.getPath(fAvrPath, bundleid);
	}

	/**
	 * Sets the path in the preference store.
	 * 
	 * @param newpath
	 * @param context
	 */
	public void setPath(String newpath, SourceType source) {
		String newvalue = null;
		switch (source) {
			case System:
				newvalue = source.name();
				break;
			case Bundled:
				newvalue = source.name() + ":" + newpath;
				break;
			case Custom:
				newvalue = newpath;
		}
		fPrefsValue = newvalue;
	}

	/**
	 * Sets the path back to the default value.
	 */
	public void setToDefault() {
		fPrefsValue = fPrefs.getDefaultString(fAvrPath.name());
	}

	/**
	 * Gets the source of this path.
	 * 
	 * This can be one of the {@link SourceType} values
	 * <ul>
	 * <li><code>Bundled</code> if the path points to a bundled avr-gcc toolchain.</li>
	 * <li><code>System</code> if the system default path is used.</li>
	 * <li><code>Custom</code> if the path is selected by the user.</li>
	 * </ul>
	 * 
	 * @return
	 */
	public AVRPathManager.SourceType getSourceType() {
		if (fPrefsValue == null) {
			// get the path source from the preferences store
			fPrefsValue = fPrefs.getString(fAvrPath.name());
		}
		if (fPrefsValue.equals(AVRPathManager.SourceType.System.name())) {
			return AVRPathManager.SourceType.System;
		}
		if (fPrefsValue.startsWith(AVRPathManager.SourceType.Bundled.name())) {
			return AVRPathManager.SourceType.Bundled;
		}
		// else: a custom path
		return AVRPathManager.SourceType.Custom;
	}

	/**
	 * Checks if the path managed by this manager is optional. 
	 * 
	 * @return <code>true</code> if path is not required for basic plugin operation.
	 */
	public boolean isOptional() {
		return fAvrPath.isOptional();
	}
	
	/**
	 * Checks if the current path is valid.
	 * <p>
	 * Some paths are required, some are optional.
	 * </p>
	 * <p>
	 * For required paths this method returns <code>true</code> if a internally defined testfile
	 * exists in the given path.
	 * </p>
	 * <p>
	 * For optional paths this method also returns true if - and only if - the path is empty ("").
	 * </p>
	 * 
	 * @return <code>true</code> if the path points to a valid source folder.
	 */
	public boolean isValid() {
		IPath path = getPath();
		// Test if the file is optional. If optional,
		// then an empty Path is also valid
		if (fAvrPath.isOptional()) {
			if (path.isEmpty()) {
				return true;
			}
		}

		// Test if the testfile exists in the given folder
		IPath testpath = path.append(fAvrPath.getTest());
		File file = testpath.toFile();
		if (file.canRead()) {
			return true;
		}

		// try with ".exe" appended, as otherwise on Windows
		// file.canRead() will fail
		testpath = path.append(fAvrPath.getTest() + ".exe");
		file = testpath.toFile();
		if (file.canRead()) {
			return true;
		}

		return false;
	}

	/**
	 * Sets the PreferenceStore the PathManager should work on.
	 * 
	 * By default the PathManager will work on the Instance Preference store.
	 * 
	 * @param store
	 */
	public void setPreferenceStore(IPreferenceStore store) {
		fPrefs = store;
	}

	/**
	 * Stores the path in the PreferenceStore.
	 * 
	 * Until <code>store()</code> is called, all modifications to the path are only internal to
	 * this IPathManager and not visible outside.
	 */
	public void store() {
		if (fPrefsValue != null) {
			fPrefs.setValue(fAvrPath.name(), fPrefsValue);
		}
	}
}
