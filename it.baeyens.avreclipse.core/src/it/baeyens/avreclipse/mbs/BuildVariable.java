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
 * $Id: BuildVariable.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.mbs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.envvar.IBuildEnvironmentVariable;

/**
 * Implementation of the {@link IBuildEnvironmentVariable} interface.
 * <p>
 * Each instance of this class represents a single environment variable within
 * the CDT managed build system.
 * </p>
 * <p>
 * This class is mostly a container for the {@link BuildVariableValues} Enum,
 * which handles the actual variable value.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.2
 */
public class BuildVariable implements IBuildEnvironmentVariable {

	/** The variable value handler of this variable */
	private BuildVariableValues fValueHandler;

	/** The configuration for this variable */
	private IConfiguration fConfiguration;

	/** System default Path Separator. On Windows ";", on Posix ":" */
	private final static String PATH_SEPARATOR = System.getProperty("path.separator");

	/**
	 * Constructs a new environment variable for the given build configuration.
	 * <p>
	 * The name of the variable must be one of those returned by the
	 * {@link #getVariableNames()} method, otherwise an unchecked Exception is
	 * thrown.
	 * </p>
	 * 
	 * @see BuildVariableValues
	 * 
	 * @param name
	 *            <code>String</code> with the Variable name.
	 * @param buildcfg
	 *            <code>IConfiguration</code> scope for the variable.
	 * @throws <code>IllegalArgumentException</code> if the name is not valid.
	 */
	public BuildVariable(String name, IConfiguration buildcfg) {
		fValueHandler = BuildVariableValues.valueOf(name);
		fConfiguration = buildcfg;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.managedbuilder.envvar.IBuildEnvironmentVariable#getName()
	 */
	public String getName() {
		return fValueHandler.name();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.managedbuilder.envvar.IBuildEnvironmentVariable#getDelimiter()
	 */
	public String getDelimiter() {
		// return Delimiter according to the Platform
		return PATH_SEPARATOR;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.managedbuilder.envvar.IBuildEnvironmentVariable#getOperation()
	 */
	public int getOperation() {
		return fValueHandler.getOperation();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.managedbuilder.envvar.IBuildEnvironmentVariable#getValue()
	 */
	public String getValue() {
		return fValueHandler.getValue(fConfiguration);
	}

	/**
	 * @return a <code>List&lt;String&gt;</code> of all supported variable
	 *         names.
	 */
	public static List<String> getVariableNames() {

		List<String> allvarnames = new ArrayList<String>();
		BuildVariableValues[] allnames = BuildVariableValues.values();
		for (BuildVariableValues mvar : allnames) {
			if (mvar.isVariable()) {
				allvarnames.add(mvar.name());
			}
		}
		return allvarnames;
	}

}
