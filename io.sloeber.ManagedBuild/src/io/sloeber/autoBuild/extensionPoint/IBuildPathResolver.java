/*******************************************************************************
 * Copyright (c) 2005, 2012 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 *******************************************************************************/
package io.sloeber.autoBuild.extensionPoint;

import io.sloeber.schema.api.IConfiguration;

/**
 * This interface is to be implemented by the tool-integrator to provide some specific
 * logic for resolving the build path variable values to the build paths.
 * <br/><br/>
 * See extension point {@code org.eclipse.cdt.managedbuilder.core.buildDefinitions},
 * element {@code envVarBuildPath} attribute {@code buildPathResolver}.
 *
 * @since 3.0
 */
public interface IBuildPathResolver {
	/**
	 * @param pathType one of the IEnvVarBuildPath.BUILDPATH _xxx
	 * @param variableName represents the name of the variable that holds the build paths
	 * @param variableValue represents the value of the value specified with the
	 *     variableName argument
	 * @param configuration represents configuration for which the build paths are requested
	 */
	String[] resolveBuildPaths(int pathType, String variableName, String variableValue, IConfiguration configuration);
}
