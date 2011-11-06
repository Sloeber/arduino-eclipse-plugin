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
 * $Id: AVRTargetBuildMacroSupplier.java 851 2010-08-07 19:37:00Z innot $
 *     
 *******************************************************************************/
package it.baeyens.avreclipse.mbs;

import java.util.List;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacro;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.cdt.managedbuilder.macros.IConfigurationBuildMacroSupplier;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

/**
 * BuildMacro Supplier.
 * <p>
 * This class implements the {@link IConfigurationBuildMacroSupplier} interface
 * and can be used for the <code>configurationMacroSupplier</code> attribute
 * of a <code>toolChain</code> element.
 * </p>
 * <p>
 * See {@link BuildVariableValues} for a list of variables actually supported.
 * </p>
 * 
 * @author Thomas Holland
 * @since 2.0
 */
public class AVRTargetBuildMacroSupplier implements IConfigurationBuildMacroSupplier {

	/** A list of all known macro names this supplier supports */
	private final static List<String> fAllMacroNames = BuildMacro.getMacroNames();

	/**
	 * Get the Macro with the given name.
	 * <p>
	 * If the passed macro name matches any of the macros handled by this
	 * plugin, it will return an <code>IBuildMacro</code> object which handles
	 * the Macro value dynamically.
	 * </p>
	 * <p>
	 * AVRTargetBuildMacros can have an optional parameter to retrive the value
	 * of the macro not from the current project, but from a different project.
	 * Examples:
	 * <ul>
	 * <li><code>${macroname:project}</code> gets the macro value from the
	 * active configuration of the named project.</li>
	 * <li><code>${macroname:project/configuration}</code> gets the macro
	 * value from the specified configuration of the project.</li>
	 * </ul>
	 * If either the project, or the specified configuration do not exist,
	 * <code>null</code> is returned and the macro is not resolved.
	 * </p>
	 * <p>
	 * If the target project is not an AVR project, some build macros will
	 * return an empty String.
	 * </p>
	 * 
	 * @param macroName
	 *            Name of the macro the build system wants a
	 *            <code>IBuildMacro</code> for.
	 * @param configuration
	 *            The current configuration. (e.g. "Debug" or "Release")
	 * @param provider
	 *            A buildMacro supplier to query already existing build macros.
	 *            Not used.
	 * @return An IBuildMacro object representing the value of the wanted macro
	 *         or <code>null</code> if <code>macroName</code> did not match
	 *         any of the implemented macro names or the optional macro
	 *         parameter is invalid.
	 */
	public IBuildMacro getMacro(String macroName, IConfiguration configuration,
	        IBuildMacroProvider provider) {

		if (macroName == null || macroName.length() == 0)
			return null;

		// Check if the macro has parameters
		int index = macroName.indexOf(':');
		String name = (index == -1 ? macroName : macroName.substring(0, index));
		String param = (index == -1 ? null : macroName.substring(index + 1));

		if (fAllMacroNames.contains(name)) {
			// The macro is one of ours. Resolve the parameters
			IConfiguration targetconfig = (param == null ? configuration : getConfiguration(param));
			if (targetconfig == null)
				return null;
			return new BuildMacro(name, targetconfig);
		}
		return null;
	}

	/**
	 * Returns an array of Macros supported by this supplier.
	 * 
	 * @param configuration
	 *            The current configuration.
	 * @param provider
	 *            A buildMacro supplier to query already existing build macros.
	 *            Not used.
	 * @return An array of IBuildMacros supported by this supplier.
	 * 
	 * @see #getMacro(String, IConfiguration, IBuildMacroProvider)
	 */
	public IBuildMacro[] getMacros(IConfiguration configuration, IBuildMacroProvider provider) {

		IBuildMacro[] macros = new BuildMacro[fAllMacroNames.size()];
		for (int i = 0; i < fAllMacroNames.size(); i++) {
			macros[i] = new BuildMacro(fAllMacroNames.get(i), configuration);
		}
		return macros;
	}

	/**
	 * Get the specified build configuration.
	 * <p>
	 * The parameter has the format <code>ProjectName[/ConfigName]</code>.
	 * Without the optional ConfigName, this method returns the default (=
	 * active) build configuration for the project.
	 * </p>
	 * <p>
	 * The project must be a Managed CDT Project, otherwise <code>null</code>
	 * is returned.
	 * </p>
	 * <p>
	 * If either the project or the configuration does not exist,
	 * <code>null</code> is returned.
	 * </p>
	 * 
	 * @param param
	 *            <code>String</code> with the format
	 *            <code>ProjectName[/ConfigName]</code>
	 * @return Requested build configuration or <code>null</code> if it does
	 *         not exist.
	 */
	private IConfiguration getConfiguration(String param) {

		// Check if the parameter has a config name
		int index = param.indexOf('/');
		String projectname = index == -1 ? param : param.substring(0, index);
		String configname = index == -1 ? null : param.substring(index + 1);

		// Get the Project
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectname);
		if (project != null) {
			// get the buildinfo for the project (which has the active
			// configuration)
			IManagedBuildInfo bi = ManagedBuildManager.getBuildInfo(project);
			if (bi != null) {
				if (configname == null) {
					// return the active configuration
					return bi.getDefaultConfiguration();
				}
				// user wants a specific configuration. A specific configuration
				// can be fetched from the ManagedProject.
				IManagedProject mp = bi.getManagedProject();
				if (mp != null) {
					IConfiguration[] allconfigs = mp.getConfigurations();
					for (IConfiguration config : allconfigs) {
						if (configname.equals(config.getName())) {
							return config;
						}
					}
				}
			}
		}
		// format of the parameter was wrong or either project or configuration
		// does not exist
		return null;
	}
}
