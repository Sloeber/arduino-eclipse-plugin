/*******************************************************************************
 * Copyright (c) 2005, 2019 Intel Corporation and others.
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
 * EclipseSource
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.internal.core.SafeStringInterner;

import io.sloeber.autoBuild.api.IInputOrder;
import io.sloeber.autoBuild.api.IInputType;
import io.sloeber.autoBuild.api.IManagedConfigElement;

public class InputOrder implements IInputOrder {
    //  Superclass
    //  Parent and children
    private IInputType fParent;
    //  Managed Build model attributes
    private String fPath;
    private String fOrder;
    private Boolean fExcluded;
    //  Miscellaneous
    private boolean fIsExtensionInputOrder = false;
    private boolean fIsDirty = false;
    private boolean fResolved = true;
    private boolean fRebuildState;

    /*
     *  C O N S T R U C T O R S
     */

    /**
     * This constructor is called to create an InputOrder defined by an extension
     * point in
     * a plugin manifest file, or returned by a dynamic element provider
     *
     * @param parent
     *            The IInputType parent of this InputOrder
     * @param element
     *            The InputOrder definition from the manifest file or a dynamic
     *            element
     *            provider
     */
    public InputOrder(IInputType parent, IManagedConfigElement element) {
        this.fParent = parent;
        fIsExtensionInputOrder = true;

        // setup for resolving
        fResolved = false;

        loadFromManifest(element);
    }

    /**
     * This constructor is called to create an InputOrder whose attributes and
     * children will be
     * added by separate calls.
     *
     * @param parent
     *            The parent of the an InputOrder
     * @param isExtensionElement
     *            Indicates whether this is an extension element or a managed
     *            project element
     */
    public InputOrder(InputType parent, boolean isExtensionElement) {
        this.fParent = parent;
        fIsExtensionInputOrder = isExtensionElement;
        if (!isExtensionElement) {
            setDirty(true);
        }
    }

    /**
     * Create an <code>InputOrder</code> based on the specification stored in the
     * project file (.cdtbuild).
     *
     * @param parent
     *            The <code>ITool</code> the InputOrder will be added to.
     * @param element
     *            The XML element that contains the InputOrder settings.
     */
    public InputOrder(IInputType parent, ICStorageElement element) {
        this.fParent = parent;
        fIsExtensionInputOrder = false;

        // Initialize from the XML attributes
        loadFromProject(element);
    }

    /**
     * Create an <code>InputOrder</code> based upon an existing InputOrder.
     *
     * @param parent
     *            The <code>IInputType</code> the InputOrder will be added to.
     * @param inputOrder
     *            The existing InputOrder to clone.
     */
    public InputOrder(IInputType parent, InputOrder inputOrder) {
        this(parent, inputOrder, false);
    }

    /**
     * Create an <code>InputOrder</code> based upon an existing InputOrder.
     *
     * @param parent
     *            The <code>IInputType</code> the InputOrder will be added to.
     * @param inputOrder
     *            The existing InputOrder to clone.
     * @param retainRebuildState
     *            Whether or not to retain the <code>rebuildState</code> and
     *            <code>dirty</code> state of <code>inputOrder</code>.
     */
    public InputOrder(IInputType parent, InputOrder inputOrder, boolean retainRebuildState) {
        this.fParent = parent;
        fIsExtensionInputOrder = false;

        //  Copy the remaining attributes
        if (inputOrder.fPath != null) {
            fPath = inputOrder.fPath;
        }

        if (inputOrder.fOrder != null) {
            fOrder = inputOrder.fOrder;
        }

        if (inputOrder.fExcluded != null) {
            fExcluded = inputOrder.fExcluded;
        }

        if (retainRebuildState) {
            setDirty(inputOrder.fIsDirty);
            setRebuildState(inputOrder.fRebuildState);
        } else {
            setDirty(true);
            setRebuildState(true);
        }
    }

    /*
     *  E L E M E N T   A T T R I B U T E   R E A D E R S   A N D   W R I T E R S
     */

    /* (non-Javadoc)
     * Loads the InputOrder information from the ManagedConfigElement specified in the
     * argument.
     *
     * @param element Contains the InputOrder information
     */
    protected void loadFromManifest(IManagedConfigElement element) {

        // path
        fPath = SafeStringInterner.safeIntern(element.getAttribute(IInputOrder.PATH));

        // order
        fOrder = SafeStringInterner.safeIntern(element.getAttribute(IInputOrder.ORDER));

        // excluded
        String isEx = element.getAttribute(IInputOrder.EXCLUDED);
        if (isEx != null) {
            fExcluded = Boolean.parseBoolean(isEx);
        }
    }

    /* (non-Javadoc)
     * Initialize the InputOrder information from the XML element
     * specified in the argument
     *
     * @param element An XML element containing the InputOrder information
     */
    protected void loadFromProject(ICStorageElement element) {
        // path
        if (element.getAttribute(IInputOrder.PATH) != null) {
            fPath = SafeStringInterner.safeIntern(element.getAttribute(IInputOrder.PATH));
        }

        // order
        if (element.getAttribute(IInputOrder.ORDER) != null) {
            fOrder = SafeStringInterner.safeIntern(element.getAttribute(IInputOrder.ORDER));
        }

        // excluded
        if (element.getAttribute(IInputOrder.EXCLUDED) != null) {
            String isEx = element.getAttribute(IInputOrder.EXCLUDED);
            if (isEx != null) {
                fExcluded = Boolean.parseBoolean(isEx);
            }
        }
    }

    /**
     * Persist the InputOrder to the project file.
     */
    public void serialize(ICStorageElement element) {
        if (fPath != null) {
            element.setAttribute(IInputOrder.PATH, fPath);
        }

        if (fOrder != null) {
            element.setAttribute(IInputOrder.ORDER, fOrder);
        }

        if (fExcluded != null) {
            element.setAttribute(IInputOrder.EXCLUDED, fExcluded.toString());
        }

        // I am clean now
        fIsDirty = false;
    }

    /*
     *  P A R E N T   A N D   C H I L D   H A N D L I N G
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputOrder#getParent()
     */
    @Override
    public IInputType getParent() {
        return fParent;
    }

    /*
     *  M O D E L   A T T R I B U T E   A C C E S S O R S
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputOrder#getPsth()
     */
    @Override
    public String getPath() {
        return fPath;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputOrder#setPath()
     */
    @Override
    public void setPath(String newPath) {
        if (fPath == null && newPath == null)
            return;
        if (fPath == null || newPath == null || !(fPath.equals(newPath))) {
            fPath = newPath;
            fIsDirty = true;
            setRebuildState(true);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputOrder#getOrder()
     */
    @Override
    public String getOrder() {
        return fOrder;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputOrder#setOrder()
     */
    @Override
    public void setOrder(String newOrder) {
        if (fOrder == null && newOrder == null)
            return;
        if (fOrder == null || newOrder == null || !(fOrder.equals(newOrder))) {
            fOrder = newOrder;
            fIsDirty = true;
            setRebuildState(true);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputOrder#getExcluded()
     */
    @Override
    public boolean getExcluded() {
        return fExcluded.booleanValue();
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IInputOrder#setExcluded()
     */
    @Override
    public void setExcluded(boolean b) {
        if (fExcluded == null || !(b == fExcluded.booleanValue())) {
            fExcluded = b;
            setDirty(true);
            setRebuildState(true);
        }
    }

    /*
     *  O B J E C T   S T A T E   M A I N T E N A N C E
     */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IInputOrder#isExtensionElement()
     */
    public boolean isExtensionElement() {
        return fIsExtensionInputOrder;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IInputOrder#isDirty()
     */
    @Override
    public boolean isDirty() {
        // This shouldn't be called for an extension InputOrder
        if (fIsExtensionInputOrder)
            return false;
        return fIsDirty;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IInputOrder#setDirty(boolean)
     */
    @Override
    public void setDirty(boolean isDirty) {
        this.fIsDirty = isDirty;
    }

    /* (non-Javadoc)
     *  Resolve the element IDs to interface references
     */
    public void resolveReferences() {
        if (!fResolved) {
            fResolved = true;
        }
    }

    public boolean needsRebuild() {
        return fRebuildState;
    }

    public void setRebuildState(boolean rebuild) {
        if (isExtensionElement() && rebuild)
            return;

        fRebuildState = rebuild;
    }
}
