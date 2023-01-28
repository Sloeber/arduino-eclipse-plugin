/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
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
package io.sloeber.schema.internal;

import static io.sloeber.autoBuild.integration.Const.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
//import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.osgi.framework.Version;

import io.sloeber.schema.api.IProjectType;

public abstract class SchemaObject implements ISchemaObject {

    protected String myID;
    public String myName;

    protected IConfigurationElement myConfigurationElement;
    //protected ICStorageElement myStorageElement;
    private static final int MAX_SUPER_CLASS = 5;
    //protected ICStorageElement[] myStorageSuperClassElement = new ICStorageElement[MAX_SUPER_CLASS];
    protected IConfigurationElement[] mySuperClassConfElement = new IConfigurationElement[MAX_SUPER_CLASS];
    protected String[] mySuperClassID = new String[MAX_SUPER_CLASS];

    public static int ORIGINAL = 0;
    public static int SUPER = 1;

    protected Version version = null;
    protected String managedBuildRevision = null;

    protected void loadNameAndID(IExtensionPoint root, IConfigurationElement element) {
        myConfigurationElement = element;
        myID = element.getAttribute(ID);
        myName = element.getAttribute(NAME);
        int cur = 0;
        mySuperClassID[cur] = element.getAttribute(SUPERCLASS);
        while ((cur < MAX_SUPER_CLASS) && (mySuperClassID[cur] != null)) {
            mySuperClassConfElement[cur] = getSuperClassConfElement(element.getName(), mySuperClassID[cur], root);
            if (mySuperClassConfElement[cur] == null) {
                System.err.println("Failed to load superclass " + mySuperClassID[cur]);
            } else {
                mySuperClassID[cur + 1] = mySuperClassConfElement[cur].getAttribute(SUPERCLASS);
            }
            cur++;
        }
    }

    protected String[] getAttributes(String attributeName) {
        String[] ret = new String[2];
        ret[SUPER] = ret[ORIGINAL] = myConfigurationElement.getAttribute(attributeName);
        int cur = 0;
        while ((cur < MAX_SUPER_CLASS) && (ret[SUPER] == null) && (mySuperClassConfElement[cur] != null)) {
            ret[SUPER] = mySuperClassConfElement[cur].getAttribute(attributeName);
            cur++;
        }
        if (ret[SUPER] == null) {
            ret[SUPER] = EMPTY_STRING;
        }
        if (ret[ORIGINAL] == null) {
            ret[ORIGINAL] = EMPTY_STRING;
        }
        return ret;
    }

    protected Object createExecutableExtension(String attributeName) {
        String error = "createExecutableExtension for " + attributeName + " for " + myName + BLANK + myID; //$NON-NLS-1$ //$NON-NLS-2$
        try {
            if (myConfigurationElement.getAttribute(attributeName) != null) {
                Object ret = myConfigurationElement.createExecutableExtension(attributeName);
                if (ret == null) {
                    System.err.println(error);
                }
                return ret;
            }
            int cur = 0;
            while ((cur < MAX_SUPER_CLASS) && (mySuperClassConfElement[cur] != null)) {
                if (mySuperClassConfElement[cur].getAttribute(attributeName) != null) {
                    Object ret = mySuperClassConfElement[cur].createExecutableExtension(attributeName);
                    if (ret == null) {
                        System.err.println(error);
                    }
                    return ret;
                }
                cur++;
            }

        } catch (CoreException e) {
            System.err.println("failed to resolve executable " + attributeName + " for " + myName + BLANK + myID); //$NON-NLS-1$ //$NON-NLS-2$
            e.printStackTrace();
        }
        return null;
    }

    List<IConfigurationElement> getAllChildren() {
        ArrayList<IConfigurationElement> ret = new ArrayList<>();
        ret.addAll(Arrays.asList(myConfigurationElement.getChildren()));
        int cur = 0;
        while ((cur < MAX_SUPER_CLASS) && (mySuperClassConfElement[cur] != null)) {
            ret.addAll(Arrays.asList(mySuperClassConfElement[cur].getChildren()));
            cur++;
        }
        return ret;
    }

    @Override
    public String getId() {
        return myID;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public String toString() {
        if (myName != null) {
            return myName;
        }
        return "id=" + myID; //$NON-NLS-1$
    }

    private IConfigurationElement getSuperClassConfElement(String elementName, String inID, IExtensionPoint root) {
        for (IConfigurationElement curElement : root.getConfigurationElements()) {
            IConfigurationElement foundElement = recursiveSearchElement(elementName, inID, curElement);
            if (foundElement != null) {
                return foundElement;
            }
        }

        return null;
    }

    private IConfigurationElement recursiveSearchElement(String elementName, String inID,
            IConfigurationElement element) {
        if (inID.equals(element.getAttribute(ID)) && elementName.equals(element.getName())) {
            return element;
        }
        for (IConfigurationElement curElement : element.getChildren()) {
            IConfigurationElement foundElement = recursiveSearchElement(elementName, inID, curElement);
            if (foundElement != null) {
                return foundElement;
            }
        }
        return null;
    }

}
