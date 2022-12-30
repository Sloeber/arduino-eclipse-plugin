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
package io.sloeber.autoBuild.api;

import java.net.URL;

import io.sloeber.autoBuild.extensionPoint.IOptionCategoryApplicability;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IOptionCategory extends IBuildObject {

    // Schema element names
    public static final String OWNER = "owner"; //$NON-NLS-1$
    public static final String ICON = "icon"; //$NON-NLS-1$

    //	 Resource Filter type
    public static final int FILTER_ALL = 0;
    public static final String ALL = "all"; //$NON-NLS-1$
    public static final int FILTER_FILE = 1;
    public static final int FILTER_PROJECT = 2;
    public static final String PROJECT = "project"; //$NON-NLS-1$

    /**
     * Returns the list of children of this node in the option category tree
     */
    public IOptionCategory[] getChildCategories();

    /**
     * Returns an array of ITool/IOption pairs for the options in this category
     * for a given configuration.
     *
     * @since 3.1
     */
    public Object[][] getOptions(IConfiguration configuration, IHoldsOptions optHolder);

    //	/**
    //	 * Returns an array of ITool/IOption pairs for the options in this category
    //	 * for a given resource configuration.
    //	 *
    //	 * @since 3.1
    //	 */
    //public Object[][] getOptions(IResourceConfiguration resConfig, IHoldsOptions optHolder);

    /**
     * Returns an array of ITool/IOption pairs for the options in this category
     * for a given resource configuration.
     *
     * @since 3.1
     */
    public Object[][] getOptions(IResourceInfo resInfo, IHoldsOptions optHolder);

    /**
     * Returns the category that owns this category, or null if this is the
     * top category for a tool.
     */
    public IOptionCategory getOwner();

    /**
     * Returns the holder (parent) of this category. This may be an object
     * implementing ITool or IToolChain, which both extend IHoldsOptions.
     * The call can return null, for example the top option category of a tool
     * will return null.
     *
     * Note that the name getOptionHolder() has been choosen, because Tool
     * implements
     * both ITool and IOptionCategory and ITool.getParent() exists already.
     *
     * @return IHoldsOptions
     * @since 3.0
     */
    public IHoldsOptions getOptionHolder();

    /**
     * Get the path name of an alternative icon for the option group.
     * Or null if no alternative icon was defined.
     *
     * @return URL
     * @since 3.0
     */
    public URL getIconPath();

    /**
     * Returns <code>true</code> if this element has changes that need to
     * be saved in the project file, else <code>false</code>.
     *
     * @return boolean
     */
    public boolean isDirty();

    /**
     * Sets the element's "dirty" (have I been modified?) flag.
     */
    public void setDirty(boolean isDirty);

    /**
     * @return an instance of the class that calculates whether the option category
     *         is visible.
     * @since 8.0
     */
    public IOptionCategoryApplicability getApplicabilityCalculator();
}
