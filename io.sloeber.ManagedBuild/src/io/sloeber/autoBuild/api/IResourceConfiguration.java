/*******************************************************************************
 * Copyright (c) 2004, 2011 Intel Corporation and others.
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
package io.sloeber.autoBuild.api;

import java.util.List;

import org.eclipse.core.resources.IResource;

/**
 * This class is a place to define build attributes of individual
 * resources that are different from the configuration as a whole. The
 * resourceConfiguration element can have multiple tool children. They
 * define the tool(s) to be used to build the specified resource. The
 * tool(s) can execute before, after, or instead of the default tool for
 * the resources (see the toolOrder attribute in the tool element).
 *
 * @since 2.1
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IResourceConfiguration extends IResourceInfo {
    public static final String RESOURCE_CONFIGURATION_ELEMENT_NAME = "resourceConfiguration"; //$NON-NLS-1$
    public static final String RCBS_APPLICABILITY = "rcbsApplicability"; //$NON-NLS-1$
    public static final String TOOLS_TO_INVOKE = "toolsToInvoke"; //$NON-NLS-1$
    public static final String APPLY_RCBS_TOOL_AS_OVERRIDE = "override"; //$NON-NLS-1$
    public static final int KIND_APPLY_RCBS_TOOL_AS_OVERRIDE = 1;
    public static final String APPLY_RCBS_TOOL_BEFORE = "before"; //$NON-NLS-1$
    public static final int KIND_APPLY_RCBS_TOOL_BEFORE = 2;
    public static final String APPLY_RCBS_TOOL_AFTER = "after"; //$NON-NLS-1$
    public static final int KIND_APPLY_RCBS_TOOL_AFTER = 3;
    public static final String DISABLE_RCBS_TOOL = "disable"; //$NON-NLS-1$
    public static final int KIND_DISABLE_RCBS_TOOL = 4;

    //TODO:  Set name and ID in the constructors to be
    //       configuration-name#resource-path

    /**
     * Returns the configuration that is the parent of this resource configuration.
     *
     * @return IConfiguration
     */
    @Override
    public IConfiguration getParent();

    /**
     * Returns whether the resource referenced by this element should be excluded
     * from builds of the parent configuration.
     * Returns <code>false</code> if the attribute was not specified.
     *
     * @return boolean
     */
    @Override
    public boolean isExcluded();

    /**
     * Returns the path of the project resource that this element references.
     * TODO: What is the format of the path? Absolute? Relative? Canonical?
     *
     * @return String
     */
    public String getResourcePath();

    /**
     * Returns an integer constant representing the users desire for ordering the
     * application of
     * a resource custom build step tool.
     *
     * @return int
     */
    public int getRcbsApplicability();

    /**
     * Returns the list of tools currently defined for the project resource that
     * this element references. Updates the String attribute toolsToInvoke.
     *
     * @return String
     */
    public List<ITool> getToolsToInvoke();

    /**
     * Sets the new value representing the users desire for ordering the application
     * of
     * a resource custom build step tool.
     */
    public void setRcbsApplicability(int value);


    /**
     * Sets the resource path to which this resource configuration applies.
     */
    public void setResourcePath(String path);

    /**
     * Returns the list of tools associated with this resource configuration.
     *
     * @return ITool[]
     */
    @Override
    public List<ITool> getTools();

    /**
     * Returns the tool in this resource configuration with the ID specified
     * in the argument, or <code>null</code>
     *
     * @param id
     *            The ID of the requested tool
     * @return ITool
     */
    public ITool getTool(String id);

    /**
     * Removes the Tool from the Tool list and map
     */
    public void removeTool(ITool tool);

    /**
     * Creates a <code>Tool</code> child for this resource configuration.
     *
     * @param superClass
     *            The superClass, if any
     * @param Id
     *            The id for the new tool chain
     * @param name
     *            The name for the new tool chain
     * @param isExtensionElement
     *            Indicates whether this is an extension element or a managed
     *            project element
     *
     * @return ITool
     */
//    public ITool createTool(ITool superClass, String Id, String name, boolean isExtensionElement);


    /**
     * Returns the Eclipse project that owns the resource configuration.
     *
     * @return IResource
     */
    public IResource getOwner();

    void setTools(List<ITool> tools);

    /**
     * @since 9.2
     */
    IToolChain getBaseToolChain();
}
