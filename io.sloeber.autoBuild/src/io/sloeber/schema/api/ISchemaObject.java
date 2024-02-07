/*******************************************************************************
 * Copyright (c) 2003, 2010 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package io.sloeber.schema.api;

import java.util.Map;

import org.eclipse.core.resources.IResource;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ISchemaObject {
    //property ID's
    public static final String BUILD_ARTEFACT_TYPE_PROPERTY_ID = "org.eclipse.cdt.build.core.buildArtefactType"; //$NON-NLS-1$

    public String getId();

    public String getName();

    boolean hasAncestor(String id);

    boolean isEnabled(int enablementType, IResource resource, IAutoBuildConfigurationDescription autoBuildConfData);

    IOptions getOptions();

    //Get the default options for this schema object taking into account the enablements 
    public Map<IOption, String> getDefaultOptions(IResource resource, AutoBuildConfigurationDescription autoData);

}
