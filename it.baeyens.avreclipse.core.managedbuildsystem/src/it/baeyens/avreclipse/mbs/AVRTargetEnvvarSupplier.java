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
 * $Id: AVRTargetEnvvarSupplier.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.mbs;

import it.baeyens.avreclipse.mbs.BuildVariable;

import java.util.List;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.envvar.IBuildEnvironmentVariable;
import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;
import org.eclipse.cdt.managedbuilder.envvar.IEnvironmentVariableProvider;

/**
 * Environment variable supplier.
 * <p>
 * This class implements the {@link IConfigurationEnvironmentVariableSupplier}
 * interface and can be used for the
 * <code>configurationEnvironmentSupplier</code> attribute of a
 * <code>toolChain</code> element.
 * </p>
 * <p>
 * See {@link BuildVariableValues} for a list of variables actually supported.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.0
 */
public class AVRTargetEnvvarSupplier implements IConfigurationEnvironmentVariableSupplier {

	/** A list of all known variable names this supplier supports */
	private final static List<String> fAllVariableNames = BuildVariable.getVariableNames();

	/**
	 * Get the Build Environment Variable with the given name.
	 * <p>
	 * If the passed variable name matches any of the variables handled by this
	 * plugin, it will return an <code>IBuildEnvironmentVariable</code> object
	 * which handles the value dynamically.
	 * </p>
	 * 
	 * @param variableName
	 *            Name of the variable the build system wants a
	 *            <code>IBuidEnvironmentVariable</code> for.
	 * @param configuration
	 *            The current configuration. (e.g. "Debug" or "Release")
	 * @param provider
	 *            An envvar supplier to query already existing variables. Not
	 *            used.
	 * @return An <code>IBuildEnvironmentVariable</code> object representing
	 *         the value of the wanted macro or <code>null</code> if
	 *         <code>variableName</code> did not match any of the implemented
	 *         variable names.
	 */
	public IBuildEnvironmentVariable getVariable(String variableName, IConfiguration configuration,
	        IEnvironmentVariableProvider provider) {

		if (variableName == null)
			return null;

		if (fAllVariableNames.contains(variableName)) {
			return new BuildVariable(variableName, configuration);
		}
		return null;
	}

	/**
	 * Returns an array of Environment Variables supported by this supplier.
	 * 
	 * @param configuration
	 *            The current configuration.
	 * @param provider
	 *            An Environment Variable supplier to query already existing
	 *            envvars. Not used.
	 * @return An array of IBuildMacros supported by this supplier.
	 * 
	 * @see #getVariable(String, IConfiguration, IEnvironmentVariableProvider)
	 */
	public IBuildEnvironmentVariable[] getVariables(IConfiguration configuration,
	        IEnvironmentVariableProvider provider) {

		IBuildEnvironmentVariable[] envvars = new BuildVariable[fAllVariableNames.size()];
		for (int i = 0; i < fAllVariableNames.size(); i++) {
			envvars[i] = new BuildVariable(fAllVariableNames.get(i), configuration);
		}

		return envvars;
	}
}
