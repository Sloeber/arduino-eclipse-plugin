/*******************************************************************************
 * Copyright (c) 2004, 2010 Intel Corporation and others.
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
package io.sloeber.schema.api;

import java.util.Set;

/**
 * This class defines the os/architecture combination upon which the
 * outputs of a tool-chain can be deployed. The osList and archList
 * attributes contain the Eclipse names of the operating systems and
 * architectures described by this element.
 *
 * @since 2.1
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface ITargetPlatform extends ISchemaObject {
    public static final String TARGET_PLATFORM_ELEMENT_NAME = "targetPlatform"; //$NON-NLS-1$

    public static final String OS_LIST = "osList"; //$NON-NLS-1$
    public static final String ARCH_LIST = "archList"; //$NON-NLS-1$
    public static final String BINARY_PARSER = "binaryParser"; //$NON-NLS-1$

    /**
     * Returns the tool-chain that is the parent of this target platform.
     *
     * @return IToolChain
     */
    public IToolChain getParent();

    /**
     * Returns an array of operating systems this target platform represents.
     *
     * @return String[]
     */
    public Set<String> getOSList();

    /**
     * Returns an array of architectures this target platform represents.
     *
     * @return String[]
     */
    public Set<String> getArchList();

    /**
     * Returns the unique IDs of the binary parsers associated with the target
     * platform.
     *
     * @return String[]
     */
    public Set<String> getBinaryParserList();

}
