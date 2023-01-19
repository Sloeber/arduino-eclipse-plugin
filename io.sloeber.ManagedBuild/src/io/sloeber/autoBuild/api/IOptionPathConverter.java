/*******************************************************************************
 * Copyright (c) 2006, 2010 Siemens AG.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/

package io.sloeber.autoBuild.api;

import org.eclipse.core.runtime.IPath;

import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.ITool;

/**
 * An IOptionPathConverter converts between tool-specific paths
 * and their platform locations
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IOptionPathConverter {

	/**
	 * Convert from a tool specific path to a platform location, e.g.
	 * "/usr/include" for a Cygwin tool gets converted to
	 * "c:\\cygwin\\usr\\include"
	 * @param toolSpecificPath The string representation of the tool-specific path
	 * @param option TODO
	 * @param tool TODO
	 * @return A path which is a meaningful platform location
	 * or null, if the conversion fails.
	 */
	IPath convertToPlatformLocation(String toolSpecificPath, IOption option, ITool tool);

}
