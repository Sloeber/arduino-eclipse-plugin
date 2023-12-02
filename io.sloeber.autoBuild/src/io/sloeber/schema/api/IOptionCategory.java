/*******************************************************************************
 * Copyright (c) 2003, 2011 IBM Corporation and others.
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
 *     Miwako Tokugawa (Intel Corporation) - bug 222817 (OptionCategoryApplicability)
 *******************************************************************************/
package io.sloeber.schema.api;

import java.net.URL;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IOptionCategory extends ISchemaObject {

    // Schema element names
    public static final String OWNER = "owner"; //$NON-NLS-1$
    // public static final String ICON = "icon"; //$NON-NLS-1$

    //	 Resource Filter type
    public static final int FILTER_ALL = 0;
    public static final String ALL = "all"; //$NON-NLS-1$
    public static final int FILTER_FILE = 1;
    public static final int FILTER_PROJECT = 2;
    public static final String PROJECT = "project"; //$NON-NLS-1$

    /**
     * Returns the category that owns this category, or null if this is the
     * top category for a tool.
     */
    public IOptionCategory getOwner();

    /**
     * Get the path name of an alternative icon for the option group.
     * Or null if no alternative icon was defined.
     *
     * @return URL
     * @since 3.0
     */
    public URL getIconPath();

}
