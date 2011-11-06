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
 * $Id: SystemPathsPosix.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.core.paths.posix;

import it.baeyens.avreclipse.core.paths.AVRPath;
import it.baeyens.avreclipse.core.paths.SystemPathHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.cdt.utils.spawner.ProcessFactory;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;


/**
 * Gets the actual system paths to the AVR-GCC Toolchain and some config files.
 * 
 * As these path can be almost everywhere (or not exist at all), this class tries to get the
 * location with the following methods:
 * <ol>
 * <li><code>which</code> command to look in the current $PATH</li>
 * <li><code>find</code> command to search certain parts of the filesystem. Currently the
 * following paths are checked (in this order)
 * <ul>
 * <li><code>/usr/local/</code></li>
 * <li><code>/usr/</code></li>
 * <li><code>/opt/</code></li>
 * <li><code>/etc/</code></li>
 * <li><code>~/</code></li>
 * <li><code>/home/</code></li>
 * </ul>
 * </li>
 * </ol>
 * <p>
 * Finding a path can be quite expensive on large systems. Therefore the caller ({@link SystemPathHelper})
 * should cache any paths found.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.1
 */
public class SystemPathsPosix {

	private final static IPath		fEmptyPath		= new Path("");

	/** Paths to be searched in order. */
	// /etc/ was used to find the avrdude.conf file. While this is currently not
	// required I leave it in just in case we will be looking for some other
	// configuration file in a future version of the plugin.
	private final static String[]	fSearchPaths	= { "/usr/local/", "/usr/", "/opt/", "~/",
			"/home/", "/etc/"						};

	private SystemPathsPosix() {
		// prevent instantiation
	}

	/**
	 * Find the system path for the given {@link AVRPath} enum value.
	 * 
	 * @param avrpath
	 * @return a valid path or <code>null</code> if no path could be found.
	 */
	public static IPath getSystemPath(AVRPath avrpath) {

		IPath path = fEmptyPath;
		String test = avrpath.getTest();
		path = which(test);
		if (path.isEmpty()) {
			path = find("*/" + test);
		}
		if (!path.isEmpty()) {
			// remove the number of segments of the test from
			// the path. This makes a test like "avr/io.h" work
			path = path.removeLastSegments(new Path(test).segmentCount());
		}
		return path;
	}

	/**
	 * Use the posix 'which' command to find the given file.
	 * 
	 * @param file
	 *            Name of the file
	 * @return <code>IPath</code> to the file. May be an empty path if the file could not be found
	 *         with the 'which' command.
	 */
	private static IPath which(String file) {

		IPath path = executeCommand("which " + file);
		return path;
	}

	/**
	 * Use the posix 'find' command to find the given file.
	 * <p>
	 * This method will search the paths in the order given by the {@link #fSearchPaths} array of
	 * path names.
	 * </p>
	 * 
	 * @param file
	 *            Name of the file
	 * @return <code>IPath</code> to the file. May be an empty path if the file could not be found
	 *         with the 'find' command.
	 */
	private static IPath find(String file) {

		for (String findpath : fSearchPaths) {
			// TODO: use -ipath instead of -path to be case insensitive.
			// -ipath is a GNU extension to the Posix find, so this might not be as
			// compatible across all platforms. For the time we leave
			// -path until someone complains.
			IPath testpath = executeCommand("find " + findpath + " -path " + file);
			if (!testpath.isEmpty()) {
				return testpath;
			}
		}

		// nothing found: return an empty path
		return fEmptyPath;

	}

	/**
	 * Execute the given command and read its output until a line with a valid path is found, which
	 * is returned.
	 * 
	 * @param command
	 * @return A valid <code>IPath</code> or an empty path if the command did not return a valid
	 *         path.
	 */
	public static IPath executeCommand(String command) {

		IPath path = fEmptyPath;

		Process cmdproc = null;
		InputStream is = null;
		InputStreamReader isr = null;
		BufferedReader br = null;

		try {
			cmdproc = ProcessFactory.getFactory().exec(command);
			is = cmdproc.getInputStream();
			isr = new InputStreamReader(is);
			br = new BufferedReader(isr);

			String line;

			while ((line = br.readLine()) != null) {
				if (line.length() > 1) {
					// non-empty line should have the path + file
					if (path.isValidPath(line)) {
						path = new Path(line);
						break;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
				if (isr != null)
					isr.close();
				if (is != null)
					is.close();
			} catch (IOException e) {
				// can't do anything about it
			}
			try {
				if (cmdproc != null) {
					cmdproc.waitFor();
				}
			} catch (InterruptedException e) {
			}
		}

		return path;
	}

}
