/*******************************************************************************
 * Copyright (c) 2004, 2016 Intel Corporation and others.
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
 * IBM Corporation
 *******************************************************************************/
package io.sloeber.schema.internal;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import io.sloeber.schema.api.ITargetPlatform;
import io.sloeber.schema.api.IToolChain;

public class TargetPlatform extends SchemaObject implements ITargetPlatform {

    String[] modelIsAbstract;
    String[] modelOsList;
    String[] modelArchList;
    String[] modelBinaryParser;

    private IToolChain parent;
    private Set<String> osList = new HashSet<>();
    private Set<String> archList = new HashSet<>();
    private Set<String> binaryParserList = new HashSet<>();
    //  Miscellaneous

    /*
     *  C O N S T R U C T O R S
     */

    /**
     * This constructor is called to create a TargetPlatform defined by an
     * extension point in a plugin manifest file, or returned by a dynamic element
     * provider
     *
     * @param parent
     *            The IToolChain parent of this TargetPlatform, or <code>null</code>
     *            if
     *            defined at the top level
     * @param element
     *            The TargetPlatform definition from the manifest file or a dynamic
     *            element
     *            provider
     * @param managedBuildRevision
     *            the fileVersion of Managed Build System
     */
    public TargetPlatform(IToolChain parent, IExtensionPoint root, IConfigurationElement element) {
        this.parent = parent;

        loadNameAndID(root, element);

        modelIsAbstract = getAttributes(IS_ABSTRACT);
        modelOsList = getAttributes(OS_LIST);
        modelArchList = getAttributes(ARCH_LIST);
        modelBinaryParser = getAttributes(BINARY_PARSER);

        // Get the comma-separated list of valid OS
        if (!modelOsList[SUPER].isBlank()) {
            String[] osTokens = modelOsList[SUPER].split(","); //$NON-NLS-1$
            for (String token : osTokens) {
                osList.add(token.trim());
            }
        }

        // Get the comma-separated list of valid Architectures
        String arch = element.getAttribute(ARCH_LIST);
        if (arch != null) {
            String[] archTokens = arch.split(","); //$NON-NLS-1$
            for (int j = 0; j < archTokens.length; ++j) {
                archList.add(archTokens[j].trim());
            }
        }

        // Get the IDs of the binary parsers from a semi-colon-separated list.
        String bpars = element.getAttribute(BINARY_PARSER);
        if (bpars != null) {
            String[] bparsTokens = CDataUtil.stringToArray(bpars, ";"); //$NON-NLS-1$
            for (int j = 0; j < bparsTokens.length; ++j) {
                binaryParserList.add(bparsTokens[j].trim());
            }
        }

    }

    @Override
    public IToolChain getParent() {
        return parent;
    }

    @Override
    public String getName() {
        return myName;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.ITargetPlatform#getBinaryParserList()
     */
    @Override
    public Set<String> getBinaryParserList() {
        return binaryParserList;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITargetPlatform#getArchList()
     */
    @Override
    public Set<String> getArchList() {
        return archList;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.ITargetPlatform#getOSList()
     */
    @Override
    public Set<String> getOSList() {
        return osList;
    }

}
