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
 *     
 * $Id: SystemPathHelper.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.paths;

import it.baeyens.avreclipse.core.paths.posix.SystemPathsPosix;
import it.baeyens.avreclipse.core.paths.win32.SystemPathsWin32;
import it.baeyens.avreclipse.core.preferences.AVRPathsPreferences;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.preference.IPreferenceStore;


/**
 * Convenience class to get the current operating system dependent path for a given resource.
 * 
 * This class acts as a switch to the the operating system dependent </code>IPathProvider</code>s.
 * 
 * @author Thomas Holland
 * @since 2.1
 */
public final class SystemPathHelper {

	private final static String					CACHE_TAG	= "systempath/";

	private final static Map<AVRPath, IPath>	fPathCache	= new HashMap<AVRPath, IPath>(AVRPath
																	.values().length);

	/**
	 * Get the path to a resource, depending on the operating system.
	 * 
	 * @param avrpath
	 *            AVRPath for which to get the system path.
	 * @param force
	 *            If <code>true</code> reload the system path directly, without using any cached
	 *            values.
	 * 
	 * @return IPath with the current system path.
	 */
	public synchronized static IPath getPath(AVRPath avrpath, boolean force) {

		// if force flag not set check the caches first
		if (!force) {
			// Check if the path is already in the instance cache
			if (fPathCache.containsKey(avrpath)) {
				IPath cachedpath = fPathCache.get(avrpath);
				if (cachedpath != null && !cachedpath.isEmpty()) {
					return cachedpath;
				}
			}

			// Check if the path is in the persistent cache

			// If there is an entry in the preferencestore named "cache_..." and its value is a
			// valid directory path and it contains the test file, then we use it instead of
			// re-searching the system.
			String cachedpath = AVRPathsPreferences.getPreferenceStore().getString(
					CACHE_TAG + avrpath.name());
			if (cachedpath.length() > 0) {
				// Test if the path contains the required test file
				IPath testpath = new Path(cachedpath).append(avrpath.getTest());
				File file = testpath.toFile();
				if (file.canRead()) {
					IPath path = new Path(cachedpath);
					fPathCache.put(avrpath, path);
					return path;
				}
				// Test with ".exe" appended for Windows systems
				testpath = new Path(cachedpath).append(avrpath.getTest() + ".exe");
				file = testpath.toFile();
				if (file.canRead()) {
					IPath path = new Path(cachedpath);
					fPathCache.put(avrpath, path);
					return path;
				}
			}
		}

		// If either the force flag was set or the path was not found in either cache, then
		// search
		// for the path.

		IPath path = null;
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			path = SystemPathsWin32.getSystemPath(avrpath);
		} else {
			// posix path provider
			path = SystemPathsPosix.getSystemPath(avrpath);
		}

		// if a path was found then store it in both caches
		if (path != null) {
			// instance cache
			fPathCache.put(avrpath, path);

			// persistent cache
			AVRPathsPreferences.getPreferenceStore().putValue(CACHE_TAG + avrpath.name(),
					path.toOSString());
		}

		return path;

	}

	/**
	 * Clear both the instance and the persistent system path cache.
	 * <p>
	 * This method is currently not used in the plugin.
	 * </p>
	 */
	public synchronized static void clearCache() {

		// Clear the instance cache
		fPathCache.clear();

		// Clear the persistent cache
		IPreferenceStore prefs = AVRPathsPreferences.getPreferenceStore();
		for (AVRPath avrpath : AVRPath.values()) {
			if (prefs.contains(CACHE_TAG + avrpath.name())) {
				prefs.setToDefault(CACHE_TAG + avrpath.name());
			}
		}
	}

}
