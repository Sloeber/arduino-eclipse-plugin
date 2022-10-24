/*****************************************************************
 * Copyright (c) 2010, 2011 Texas Instruments and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Texas Instruments - Initial API and implementation
 *****************************************************************/

package io.sloeber.managedBuild.api;

import org.eclipse.cdt.utils.cdtvariables.IVariableSubstitutor;

/**
 * This interface can be implemented by clients to contribute custom command-generator
 * for a build-option.
 *
 * The custom command-generator class should be referenced in the <option>/commandGenerator
 * attribute of the org.eclipse.cdt.managedbuilder.core.buildDefinitions extension-point.
 *
 * @since 8.0
 */
public interface IOptionCommandGenerator {
	/**
	 * Generate the command for the given option.
	 *
	 * @param option the underlying build-option
	 * @param macroSubstitutor to be used for expanding macros in option's value
	 * @return the generated build-option command. May return {@code null} to fall
	 * 			back to the default command generation logic.
	 */
	String generateCommand(IOption option, IVariableSubstitutor macroSubstitutor);

}
