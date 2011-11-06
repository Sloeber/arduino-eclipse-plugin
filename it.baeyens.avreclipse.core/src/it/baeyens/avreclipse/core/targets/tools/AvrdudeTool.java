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
 * $Id: AvrdudeTool.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/

package it.baeyens.avreclipse.core.targets.tools;

import it.baeyens.avreclipse.core.avrdude.AVRDudeException;
import it.baeyens.avreclipse.core.targets.IProgrammer;
import it.baeyens.avreclipse.core.targets.IProgrammerTool;
import it.baeyens.avreclipse.core.targets.ITargetConfiguration;
import it.baeyens.avreclipse.core.targets.ITargetConfiguration.Result;
import it.baeyens.avreclipse.core.targets.ITargetConfiguration.ValidationResult;
import it.baeyens.avreclipse.core.toolinfo.AVRDude;
import it.baeyens.avreclipse.core.toolinfo.ICommandOutputListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author Thomas Holland
 * @since
 * 
 */
public class AvrdudeTool extends AbstractTool implements IProgrammerTool {

	public final static String			ID					= "avreclipse.avrdude";

	public final static String			NAME				= "AVRDude";

	private final static AVRDude		fAVRDude			= AVRDude.getDefault();

	public final static String			ATTR_CMD_NAME		= ID + ".command";
	public final static String			DEF_CMD_NAME		= "avrdude";

	public final static String			ATTR_USE_CONSOLE	= ID + ".useconsole";
	public final static String			DEF_USE_CONSOLE		= Boolean.toString(true);

	public final static String			ATTR_VERBOSITY		= ID + ".verbosity";
	public final static String			DEF_VERBOSITY		= Integer.toString(0);

	private final static String[]		ALL_ATTRS			= new String[] { ATTR_CMD_NAME,
			ATTR_USE_CONSOLE, ATTR_VERBOSITY				};

	private final static String[]		VERBOSITY_LEVELS	= new String[] { "", "-v", "-vv",
			"-vvvv"										};

	/** Cache of all Name/Version strings, mapped to their respective command name. */
	private Map<String, String>			fNameVersionMap		= new HashMap<String, String>();

	/** Cache of all MCU Sets, mapped to their respective command name */
	private Map<String, Set<String>>	fMCUMap				= new HashMap<String, Set<String>>();

	/** Mapping of mcu id values to their AVRDude format counterparts. */
	private Map<String, String>			fMCUAVRudeFormatMap	= new HashMap<String, String>();

	private ICommandOutputListener		fOutputListener		= new AvrdudeOutputListener();

	public AvrdudeTool(ITargetConfiguration hc) {
		super(hc);
		// tell the hardware configuration about the avrddue attributes and their default values.
		// String[][] avariceattrs = new String[][] { { ATTR_CMD_NAME, DEF_CMD_NAME },
		// { ATTR_USE_CONSOLE, DEF_USE_CONSOLE }, { ATTR_VERBOSITY, DEF_VERBOSITY } };
		// getHardwareConfig().addAttributes(this, avariceattrs);

	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfigurationTool#getId()
	 */
	@Override
	public String getId() {
		return ID;
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfigurationTool#getName()
	 */
	@Override
	public String getName() {
		return NAME;
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.IAttributeProvider#getAttributes()
	 */
	public String[] getAttributes() {
		return ALL_ATTRS;
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.IAttributeProvider#getDefaultValue(java.lang.String)
	 */
	public String getDefaultValue(String attribute) {
		String defaultvalue = null;
		if (ATTR_CMD_NAME.equals(attribute)) {
			defaultvalue = DEF_CMD_NAME;
		} else if (ATTR_USE_CONSOLE.equals(attribute)) {
			defaultvalue = DEF_USE_CONSOLE;
		} else if (ATTR_VERBOSITY.equals(attribute)) {
			defaultvalue = DEF_VERBOSITY;
		}

		return defaultvalue;
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.core.targets.tools.AbstractTool#getCommand(it.baeyens.avreclipse.core.targets
	 * .ITargetConfiguration)
	 */
	@Override
	public String getCommand() {
		String command = getHardwareConfig().getAttribute(ATTR_CMD_NAME);
		if (command == null) {
			command = DEF_CMD_NAME;
		}
		return command;
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.tools.AbstractTool#getOutputListener()
	 */
	@Override
	protected ICommandOutputListener getOutputListener() {
		return fOutputListener;
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.core.targets.ITargetConfigurationTool#getVersion(it.baeyens.avreclipse.
	 * core.targets.ITargetConfiguration)
	 */
	public String getVersion() throws AVRDudeException {

		String cmd = getCommand();

		// Check if we already have the version in the cache
		if (fNameVersionMap.containsKey(cmd)) {
			return fNameVersionMap.get(cmd);
		}

		// Execute avrdude in verbose mode with a dummy programmer (to silence the warning that
		// would cause an AVRDudeException by the output listener)
		// The name / version are in the first full line of the output in the format
		// "avrdude: Version 5.6cvs, compiled on Nov 10 2008 at 17:15:38"
		String name = null;
		List<String> stdout = runCommand("-v");

		if (stdout != null) {
			// look for a line matching "*Version TheVersionNumber *"
			Pattern mcuPat = Pattern.compile(".*Version\\s+([\\w\\.]+).*");
			Matcher m;
			for (String line : stdout) {
				m = mcuPat.matcher(line);
				if (!m.matches()) {
					continue;
				}
				name = getName() + " " + m.group(1);
				break;
			}
		}
		if (name == null) {
			// could not read the version from the output, probably the regex has a
			// mistake. Return a reasonable default.
			return getName() + " ?.?";
		}

		fNameVersionMap.put(cmd, name);
		return name;
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfigurationTool#getMCUs()
	 */
	public Set<String> getMCUs() throws AVRDudeException {

		// Check if we already have the list in the cache
		String cmd = getCommand();

		if (fMCUMap.containsKey(cmd)) {
			return fMCUMap.get(cmd);
		}
		// Execute avrdude with the "-p?" to get a list of all supported mcus.
		// The parse the all output for lines matching
		// avrdudeid = mcuid [otherstuff]
		Set<String> allmcus = new HashSet<String>();
		List<String> stdout;

		stdout = runCommand("-p?");

		if (stdout != null) {
			Pattern mcuPat = Pattern.compile("\\s*(\\w+)\\s*=\\s*(\\w+).*");
			Matcher m;
			for (String line : stdout) {
				m = mcuPat.matcher(line);
				if (!m.matches()) {
					continue;
				}
				String avrdudeid = m.group(1);
				String mcuid = m.group(2).toLowerCase();
				fMCUAVRudeFormatMap.put(mcuid, avrdudeid);
				allmcus.add(mcuid);
			}
		}

		// Save the set in the cache
		fMCUMap.put(cmd, allmcus);
		return allmcus;
	}

	/*
	 * (non-Javadoc)
	 * @see it.baeyens.avreclipse.core.targets.ITargetConfigurationTool#getProgrammers()
	 */
	public Set<String> getProgrammers() throws AVRDudeException {
		// FIXME: Change the AVRDude.class API to take the Target configuration into account.
		return fAVRDude.getProgrammerIDs();
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.core.targets.ITargetConfigurationTool#getProgrammer(it.baeyens.avreclipse
	 * .core.targets.ITargetConfiguration, java.lang.String)
	 */
	public IProgrammer getProgrammer(String id) throws AVRDudeException {
		// FIXME: Change the AVRDude.class API to take the Target configuration into account.
		return fAVRDude.getProgrammer(id);
	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.core.targets.ITargetConfigurationTool#validate(it.baeyens.avreclipse.core
	 * .targets.ITargetConfiguration, java.lang.String)
	 */
	public ValidationResult validate(String attr) {
		if (ATTR_CMD_NAME.equals(attr)) {
			try {
				List<String> stdout = runCommand("-?");
				for (String line : stdout) {
					if (line.contains("avrdude")) {
						return ValidationResult.OK_RESULT;
					}
				}
			} catch (AVRDudeException e) {
				// Could not execute avrdude with the given command name
			}

			return new ValidationResult(Result.ERROR, "Invalid Command for AVRDude");
		} else if (ATTR_USE_CONSOLE.equals(attr)) {
			return ValidationResult.OK_RESULT;
		} else if (ATTR_VERBOSITY.equals(attr)) {
			return ValidationResult.OK_RESULT;
		} else {
			return new ValidationResult(Result.UNKNOWN_ATTRIBUTE, attr);
		}

	}

	/*
	 * (non-Javadoc)
	 * @see  it.baeyens.avreclipse.core.targets.tools.AbstractTool#runCommand(it.baeyens.avreclipse.core.targets
	 * .ITargetConfiguration, java.lang.String[])
	 */
	@Override
	public List<String> runCommand(String... args) throws AVRDudeException {
		String[] newargs;
		String verbosity = getVerbosityArgument();
		if (verbosity.length() != 0) {
			// Not the default verbosity level. Add the verbosity
			// argument to the array of arguments.
			newargs = new String[args.length + 1];
			newargs[0] = verbosity;
			System.arraycopy(args, 0, newargs, 1, args.length);
		} else {
			newargs = args;
		}
		return super.runCommand(newargs);
	}

	private String getVerbosityArgument() {
		int verbosity = getHardwareConfig().getIntegerAttribute(ATTR_VERBOSITY);

		// little sanity check
		if (verbosity < 0 || verbosity >= VERBOSITY_LEVELS.length) {
			verbosity = 2;
		}

		return VERBOSITY_LEVELS[verbosity];
	}
}
