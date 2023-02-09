/*******************************************************************************
 * Copyright (c) 2007, 2016 Intel Corporation and others.
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
 * cartu38 opendev (STMicroelectronics) - [514385] Custom defaultValue-generator support
 *******************************************************************************/
package io.sloeber.schema.internal;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IFolderInfo;

public class FolderInfo extends SchemaObject implements IFolderInfo {
    private String id;
    private String name;
    private Configuration myParent;
    private boolean myIsExcluded;

    String[] modelResourcePath;
    String[] modelExclude;

    public FolderInfo(IConfiguration parent, IExtensionPoint root, IConfigurationElement element) {

        myParent = (Configuration) parent;

        loadNameAndID(root, element);

        modelResourcePath = getAttributes(RESOURCEPATH);
        modelExclude = getAttributes(EXCLUDE);

        //		IConfigurationElement tcEl = null;
        //		if (!hasBody) {
        //			// setPath(Path.ROOT);
        //			id = (ManagedBuildManager.calculateChildId(parent.getId(), null));
        //			name = ("/"); //$NON-NLS-1$
        //			tcEl = element;
        //		} else {
        //			IConfigurationElement children[] = element.getChildren(IToolChain.TOOL_CHAIN_ELEMENT_NAME);
        //			if (children.length > 0)
        //				tcEl = children[0];
        //		}

        //        if (tcEl != null)
        //            toolChain = new ToolChain(this, root, tcEl);

    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isExcluded(IFile file) {
        return false;
    }

    @Override
    public Configuration getConfiguration() {
        return myParent;
    }

    @Override
    public ToolChain getToolChain() {
        return (ToolChain) myParent.getToolChain();
    }

    @Override
    public IPath getPath() {
        // TODO Auto-generated method stub
        return null;
    }


}
