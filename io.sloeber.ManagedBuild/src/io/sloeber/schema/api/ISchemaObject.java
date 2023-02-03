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

import io.sloeber.autoBuild.integration.AutoBuildConfigurationData;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ISchemaObject {
    // Schema element names
    public static final String ID = "id"; //$NON-NLS-1$
    public static final String NAME = "name"; //$NON-NLS-1$
    public static final String SUPERCLASS = "superClass"; //$NON-NLS-1$
    public static final String IS_ABSTRACT = "isAbstract"; //$NON-NLS-1$
    public static final String IS_SYSTEM = "isSystem"; //$NON-NLS-1$
    public static final String ICON = "icon"; //$NON-NLS-1$

    public String getId();

    public String getName();

    boolean hasAncestor(String id);

	boolean isEnabled(AutoBuildConfigurationData autoBuildConfData);

}
