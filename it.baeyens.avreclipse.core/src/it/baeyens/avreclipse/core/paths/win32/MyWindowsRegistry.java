/*******************************************************************************
 * 
 * Copyright (c) 2009, 2010 Thomas Holland (thomas@innot.de) and others
 * 
 * This program and the accompanying materials are made
 * available under the terms of the GNU Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     Thomas Holland - initial API and implementation
 *     
 * $Id: MyWindowsRegistry.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.paths.win32;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.utils.WindowsRegistry;

/**
 * This is an extension to the CDT WindowRegistry class.
 * <p>
 * Currently the original CDT WindowsRegistry class has some problems on 64bit windows systems,
 * where it (sometimes?) fails to load its associated dll.
 * </p>
 * <p>
 * This class, which is a (partial, see below) drop in replacement for <code>WindowsRegistry</code>
 * and will, whenever <code>WindowsRegistry</code> fails, use an alternative method to access the
 * registry.
 * </p>
 * <p>
 * Instead of using the JNI it will start the Windows <em>'reg query'</em> command and parse its
 * output. In addition to that it will also automatically look in the '\Wow6432Node' subnode if a
 * key can not be found.
 * </p>
 * <p>
 * Currently only the methods {@link #getLocalMachineValue(String, String)} and
 * {@link #getLocalMachineValueName(String, int)} are implemented, because they are the only ones
 * used by the AVR Eclipse Plugin.
 * </p>
 * 
 * @see org.eclipse.cdt.utils.WindowsRegistry
 * @author Enrico Ehrich
 * @author Thomas Holland
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 * @since 2.3.2
 * 
 */
public class MyWindowsRegistry {

	/**
	 * Small container to return the results of a query.
	 */
	protected class RegistryKeyValue {
		public String	key;	// Key name
		public String	type;	// Registry type, e.g. REG_SZ or REG_EXPANDED_SZ
		public String	value;	// Key value
	}

	/**
	 * Small class that reads all incoming chars from the InputStream and stores them in a String.
	 */
	protected class StreamReader extends Thread {
		private InputStream		is;
		private StringBuilder	sb;

		protected StreamReader(InputStream is) {
			this.is = is;
			sb = new StringBuilder();
		}

		@Override
		public void run() {
			try {
				int c;
				while ((c = is.read()) != -1)
					sb.append((char) c);
			} catch (IOException e) {
				;
			}
		}

		/**
		 * Gets the content of the given InputStream.
		 * <p>
		 * As a convenience the content is split into separate lines.
		 * </p>
		 * 
		 * @return All lines from the InputStream
		 */
		protected String[] getResult() {
			String result = sb.toString();

			// This works only on Windows, but this class is Window specific anyway so we can get
			// away with this simplistic method.
			return result.split("\r\n");
		}
	}

	/** The Windows executable to query the Registry */
	private static final String			REGQUERY_UTIL		= "reg query";

	/** Start of the Registry value type. */
	private static final String			REGTYPE_TOKEN		= "REG_";

	
	private static MyWindowsRegistry	fInstance;
	private static WindowsRegistry		fCDTRegistryInstance;

	/** Flag to inhibit calls to the CDT WindowsRegistry class. Used for test purposes. */
	private boolean						fInhibitOriginal	= true;

	/**
	 * Get the singleton instance of this class.
	 * 
	 * @return <code>MyWindowsRegistry</code> instance.
	 */
	public static MyWindowsRegistry getRegistry() {
		if (fInstance == null) {
			fInstance = new MyWindowsRegistry();
		}
		if (fCDTRegistryInstance == null) {
			fCDTRegistryInstance = WindowsRegistry.getRegistry();
		}
		return fInstance;
	}

	/**
	 * Inhibit usage of the original CDT WindowsRegistry class, always use the fallback method.
	 * <p>
	 * This method is intended only for testing this class.
	 * </p>
	 * 
	 * @param inhibit
	 *            When <code>true</code> only the fallback method is used (external call to the
	 *            'reg' executable.
	 */
	protected void setInhibitOriginal(boolean inhibit) {
		fInhibitOriginal = inhibit;
	}

	/**
	 * @see WindowsRegistry#getLocalMachineValue(String, String)
	 */
	public String getKeyValue(String subkey, String name) {
		String result;

		// First try the CDT WindowsRegistry Class
		if (fCDTRegistryInstance != null && !fInhibitOriginal) {
			// remove HKLM / HKEY_LOCAL_MACHINE from key
			// this gets added by getLacalMachineValue
			String key  = subkey.replaceFirst("HKLM\\\\", "");
			key = key.replaceFirst("HKEY_LOCAL_MACHINE\\\\", "");
			result = fCDTRegistryInstance.getLocalMachineValue(key, name);
			if (result != null) {
				// Original WindowsRegistry class was successful
				return result;
			}
		}

		// Original CDT WindowsRegistry failed: Try the fallback
		RegistryKeyValue[] results;
		String[] testkeys = convertkeys(subkey);
		for (String k : testkeys) {
			String parameters = "\"" + k + "\" /v " + name;
			results = executeKeyValueCommand(parameters);
			if (results.length > 0) {
				return results[0].value;
			}
		}

		return null;
	}

	/**
	 * @see WindowsRegistry#getLocalMachineValueName(String, int)
	 */
	public String getKeyName(String subkey, int index) {
		String result;

		// First try the CDT WindowsRegistry Class
		if (fCDTRegistryInstance != null && !fInhibitOriginal) {
			// remove HKLM / HKEY_LOCAL_MACHINE from key
			// this gets added by getLacalMachineValue
			String key  = subkey.replaceFirst("HKLM\\\\", "");
			key = key.replaceFirst("HKEY_LOCAL_MACHINE\\\\", "");
			result = fCDTRegistryInstance.getLocalMachineValueName(key, index);
			if (result != null) {
				// Original WindowsRegistry class was successful
				return result;
			}
		}

		// Original WindowsRegistry failed: Try the fallback
		RegistryKeyValue[] results;
		String[] testkeys = convertkeys(subkey);
		for (String k : testkeys) {
			String parameters = "\"" + k + "\" /s";
			results = executeKeyValueCommand(parameters);
			if (results.length > index) {
				return results[index].key;
			}
		}

		return null;
	}

	public List<String> getSubkeys(String key) {
		List<String> subkeys = new ArrayList<String>();

		// First try the CDT WindowsRegistry Class
		if (fCDTRegistryInstance != null && !fInhibitOriginal) {
			// remove HKLM / HKEY_LOCAL_MACHINE from key
			// this gets added by getLacalMachineValue
			String subkey  = key.replaceFirst("HKLM\\\\", "");
			subkey = subkey.replaceFirst("HKEY_LOCAL_MACHINE\\\\", "");

			int i = 0;
			String nextkey = null;
			do {
				nextkey = fCDTRegistryInstance.getLocalMachineValueName(subkey, i);
				if (nextkey != null) {
					subkeys.add(nextkey);
					i++;
				}
			} while (nextkey != null);

			if (subkeys.size() != 0) {
				// Original WindowsRegistry class was successful
				return subkeys;
			}
		}

		// Original WindowsRegistry failed: Try the fallback
		subkeys = executeSubKeysCommand("\"" + key + "\"");
		if (subkeys.size() == 0) {
			// Try Win64 location
			String key32 = key.replaceFirst("SOFTWARE", "SOFTWARE\\\\Wow6432Node");
			subkeys = executeSubKeysCommand(key32);
		}

		return subkeys;
	}

	/**
	 * Executes "reg query" with the given parameter string, parses the output and returns an array
	 * of {@link RegistryKeyValue} objects. If the call fails in any way an empty array is returned.
	 * 
	 * @param parameter
	 *            for the "reg query" call
	 * @return array of Key/Value objects. The array may be empty, but never <code>null</code>.
	 */
	private RegistryKeyValue[] executeKeyValueCommand(String parameter) {
		List<RegistryKeyValue> results = new ArrayList<RegistryKeyValue>();

		String[] alllines = executeRegCommand(parameter);
		for (String line : alllines) {
			if (line.indexOf(REGTYPE_TOKEN) != -1) {
				// line contains "REG_"
				// split it into key, type, and value
				String[] items;
				
				// Problem: 'reg query' on win32 separtes key/type/value with tabs
				// on win64 they are separated by four spaces.
				String trimmedline = line.trim();
				if (trimmedline.contains("\t")) {
					items = trimmedline.split("\t");
				} else if (trimmedline.contains("    ")) {
					items = trimmedline.split("    ");
				} else {
					// not field separator found
					break;
				}
				if (items.length >= 3) {
					RegistryKeyValue keyvalue = new RegistryKeyValue();
					keyvalue.key = items[0].trim();
					keyvalue.type = items[1].trim();
					keyvalue.value = items[2].trim();
					results.add(keyvalue);
				}
			}
		}

		return results.toArray(new RegistryKeyValue[results.size()]);
	}

	/**
	 * Executes "reg query" with the given parameter string, parses the output and returns an array
	 * of subkey Strings. If the call fails in any way an empty array is returned.
	 * 
	 * @param parameter
	 *            for the "reg query" call
	 * @return array of String objects. The array may be empty, but never <code>null</code>.
	 */
	private List<String> executeSubKeysCommand(String parameter) {
		List<String> allkeys = new ArrayList<String>();
		String[] alllines = executeRegCommand(parameter);
		for (String line : alllines) {
			if (line.indexOf("HKEY_") != -1) {
				allkeys.add(line);
			}
		}
		return allkeys;
	}

	private String[] executeRegCommand(String parameter) {
		String command = REGQUERY_UTIL + " " + parameter;
		String[] result = {};

		try {
			Process process = Runtime.getRuntime().exec(command);
			StreamReader reader = new StreamReader(process.getInputStream());
			reader.start();
			process.waitFor();
			reader.join();
			result = reader.getResult();
		} catch (Exception e) {
			// In case of an exception we return what we have found so far (which may be nothing =
			// empty array)
		}

		return result;

	}

	private String[] convertkeys(String key) {
		String key32 = key.replaceFirst("SOFTWARE", "SOFTWARE\\\\Wow6432Node");

		String[] allkeys = new String[2];
		allkeys[0] = key;
		allkeys[1] = key32;
		return allkeys;

	}
}
