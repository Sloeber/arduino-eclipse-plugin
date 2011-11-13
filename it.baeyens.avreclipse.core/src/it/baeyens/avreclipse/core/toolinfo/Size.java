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
 * $Id: Size.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.core.toolinfo;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.avreclipse.AVRPlugin;
import it.baeyens.avreclipse.PluginIDs;
import it.baeyens.avreclipse.core.paths.AVRPath;
import it.baeyens.avreclipse.core.paths.AVRPathProvider;
import it.baeyens.avreclipse.core.paths.IPathProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;


/**
 * This class provides some information about the used size tool in the toolchain.
 * 
 * It can return a list of all supported format options.
 * 
 * @author Thomas Holland
 * @since 2.1
 * 
 */
public class Size extends BaseToolInfo {

	private static final String	TOOL_ID			= PluginIDs.PLUGIN_TOOLCHAIN_TOOL_SIZE;

	private Map<String, String>	fOptionsMap		= null;

	private static Size			instance		= null;

	private final IPathProvider	fPathProvider	= new AVRPathProvider(AVRPath.AVRGCC);

	/**
	 * Get an instance of this Tool.
	 */
	public static Size getDefault() {
		if (instance == null)
			instance = new Size();
		return instance;
	}

	private Size() {
		// Let the superclass get the command name
		super(TOOL_ID);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.baeyens.avreclipse.core.toolinfo.IToolInfo#getToolPath()
	 */
	@Override
	public IPath getToolPath() {
		IPath path = fPathProvider.getPath();
		return path.append(getCommandName());
	}

	/**
	 * @return true if this size tool supports the -format=avr option.
	 */
	public boolean hasAVROption() {

		return getSizeOptions().containsValue("avr");
	}

	/**
	 * @return Map &lt;UI-name, option-name&gt; with all supported size options.
	 */
	public Map<String, String> getSizeOptions() {

		if (fOptionsMap != null) {
			return fOptionsMap;
		}

		fOptionsMap = new HashMap<String, String>();

		// Execute avr-gcc with the "--target-help" option and parse the
		// output
		String command = getToolPath().toOSString();
		List<String> argument = new ArrayList<String>(1);
		argument.add("-h");
		ExternalCommandLauncher size = new ExternalCommandLauncher(command, argument);

		// At least in winAVR avr-size -h will print to the error stream!
		size.redirectErrorStream(true);
		try {
			size.launch();
		} catch (IOException e) {
			// Something didn't work while running the external command
			IStatus status = new Status(Status.ERROR, ArduinoConst.CORE_PLUGIN_ID, "Could not start "
					+ command, e);
			AVRPlugin.getDefault().log(status);
			return fOptionsMap;
		}

		List<String> stdout = size.getStdOut();

		for (String line : stdout) {
			if (line.contains("--format=")) {
				// this is the line we are looking for
				// extract the format options
				int start = line.indexOf('{');
				int end = line.lastIndexOf('}');
				String options = line.substring(start + 1, end);
				// next line does not work and i am no regex expert
				// to know how to split at a "|"
				// String[] allopts = options.split("|");
				int splitter = 0;
				while ((splitter = options.indexOf('|')) != -1) {
					String opt = options.substring(0, splitter);
					fOptionsMap.put(convertOption(opt), opt);
					options = options.substring(splitter + 1);
				}
				fOptionsMap.put(convertOption(options), options);
				break;
			}
		}

		return fOptionsMap;
	}

	/**
	 * Get a better name for known format options.
	 * 
	 * @param option
	 * @return String with the UI name of the Option
	 */
	private static String convertOption(String option) {
		if ("avr".equals(option)) {
			return "AVR Specific Format";
		}
		if ("berkeley".equals(option)) {
			return "Berkeley Format";
		}
		if ("sysv".equals(option)) {
			return "SysV Format";
		}

		// unknown option
		// log a message telling the user to report this new option for inclusion into the list
		// above (as if anyone would actually read the log)
		IStatus status = new Status(
				IStatus.INFO,
				ArduinoConst.CORE_PLUGIN_ID,
				"Size encountered an unknown option for avr-size ["
						+ option
						+ "]. Please report this to the AVR Eclipse plugin maintainer to include this option in future versions of the plugin.",
				null);
		AVRPlugin.getDefault().log(status);

		return option;
	}
}
