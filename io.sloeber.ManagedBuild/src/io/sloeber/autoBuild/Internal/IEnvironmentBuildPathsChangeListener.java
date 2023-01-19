/*******************************************************************************
 * Copyright (c) 2005 Intel Corporation and others.
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
package io.sloeber.autoBuild.Internal;

import io.sloeber.schema.api.IConfiguration;

//import org.eclipse.cdt.managedbuilder.core.IConfiguration;

/**
 *
 * listeners of the environment build path changes should implement this
 * interface
 *
 * @since 3.0
 */
public interface IEnvironmentBuildPathsChangeListener {

    /**
     *
     * @param configuration
     *            represent the configuration for which the paths were changed
     * @param buildPathType
     *            set to one of
     *            the IEnvVarBuildPath.BUILDPATH_xxx
     *            (the IEnvVarBuildPath will represent the build environment
     *            variables, see also
     *            the "Specifying the Includes and Library paths environment
     *            variables",
     *            the "envVarBuildPath schema" and the "Expected CDT/MBS code
     *            changes" sections)
     */
    void buildPathsChanged(IConfiguration configuration, int buildPathType);
}
