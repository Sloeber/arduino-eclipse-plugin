/*******************************************************************************
 * Copyright (c) 2002, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Timesys - Initial API and implementation
 * IBM Rational Software
 * Miwako Tokugawa (Intel Corporation) - bug 222817 (OptionCategoryApplicability)
 * Torbjörn Svensson (STMicroelectronics) - bug #533473
 *******************************************************************************/
package io.sloeber.autoBuild.ui.tabs;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.schema.api.IOptionCategory;
import io.sloeber.schema.api.ITool;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ToolListContentProvider implements ITreeContentProvider {
    private IResource myResource;
    private IAutoBuildConfigurationDescription myAutoBuildConf;

    /**
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    @Override
    public void dispose() {
        //Nothing to do here
    }

    public ToolListContentProvider(IResource resource, IAutoBuildConfigurationDescription myAutoConfDesc) {
        myResource = resource;
        myAutoBuildConf = myAutoConfDesc;
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
     */
    @Override
    public Object[] getChildren(Object parentElement) {
        //        if (parentElement instanceof IConfiguration ) {
        //            List<ITool> tools= ((IConfiguration)parentElement).getToolChain().getTools();
        //            return tools.toArray();
        //        }
        if (parentElement instanceof ITool) {
            Set<IOptionCategory> categories = ((ITool) parentElement).getCategories(myAutoBuildConf, myResource);
            return categories.toArray();
        }
        return new Object[0];
    }

    /**
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    @Override
    public Object[] getElements(Object inputElement) {
        if (inputElement instanceof AutoBuildConfigurationDescription) {
            AutoBuildConfigurationDescription autoDesc = (AutoBuildConfigurationDescription) inputElement;
            List<ITool> tools = autoDesc.getProjectType().getToolChain().getTools();
            //only keep the enabled tools
            List<ITool> enabledTools = new LinkedList<>();
            for (ITool curTool : tools) {
                if (curTool.isEnabled(myResource, autoDesc)) {
                    enabledTools.add(curTool);
                }
            }
            //Sort the tools alphabetically 
            enabledTools.sort(new Comparator<ITool>() {
                @Override
                public int compare(ITool o1, ITool o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            return enabledTools.toArray();
        }
        return new Object[0];
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
     */
    @Override
    public Object getParent(Object element) {
        if (element instanceof IOptionCategory) {
            return ((IOptionCategory) element).getOwner();
        }
        return null;
    }

    /**
     * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
     */
    @Override
    public boolean hasChildren(Object element) {
        return getChildren(element).length > 0;
    }

    /**
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer,
     *      java.lang.Object, java.lang.Object)
     */
    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        return;
        //        if (newInput == null)
        //            return;
        //        fInfo = (IResourceInfo) newInput;
        //        if (myResource == PROJECT)
        //            elements = createElements(fInfo.getParent());
        //        else
        //            elements = createElements(fInfo);

    }
}
