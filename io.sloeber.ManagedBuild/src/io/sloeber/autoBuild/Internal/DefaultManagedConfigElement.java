/*******************************************************************************
 * Copyright (c) 2004, 2011 TimeSys Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * TimeSys Corporation - Initial API and implementation
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

//import org.eclipse.cdt.managedbuilder.core.IManagedConfigElement;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;

import io.sloeber.autoBuild.api.IManagedConfigElement;

/**
 * Implements the ManagedConfigElement by delegate all calls to an
 * IConfigurationElement instance. This is used to load configuration
 * information from the extension point.
 */
public class DefaultManagedConfigElement implements IManagedConfigElement {

    private IConfigurationElement element;
    private IExtension extension;

    public DefaultManagedConfigElement(IConfigurationElement element, IExtension extension) {
        this.element = element;
        this.extension = extension;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedConfigElement#getName()
     */
    @Override
    public String getName() {
        return element.getName();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedConfigElement#getAttribute(java.lang.String)
     */
    @Override
    public String getAttribute(String name) {
        return element.getAttribute(name);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedConfigElement#getChildren()
     */
    @Override
    public IManagedConfigElement[] getChildren() {
        return convertArray(element.getChildren(), extension);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedConfigElement#getChildren(java.lang.String)
     */
    @Override
    public IManagedConfigElement[] getChildren(String elementName) {
        return convertArray(element.getChildren(elementName), extension);
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedConfigElement#getExtension(java.lang.String)
     */
    public IExtension getExtension() {
        return extension;
    }

    public IConfigurationElement getConfigurationElement() {
        return element;
    }

    /**
     * Convenience method for converting an array of IConfigurationElements
     * into an array of IManagedConfigElements.
     */
    public static IManagedConfigElement[] convertArray(IConfigurationElement[] elements, IExtension extension) {

        IManagedConfigElement[] ret = new IManagedConfigElement[elements.length];
        for (int i = 0; i < elements.length; i++) {
            ret[i] = new DefaultManagedConfigElement(elements[i], extension);
        }
        return ret;
    }

}
