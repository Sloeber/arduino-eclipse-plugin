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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.managedbuilder.core.IManagedOutputNameProvider;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
//import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.osgi.framework.Version;

import io.sloeber.schema.api.ISchemaObject;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.internal.legacy.OutputNameProviderCompatibilityClass;

public abstract class SchemaObject implements ISchemaObject {

    protected String myID;
    public String myName;

    protected IConfigurationElement myElement;
    private static final int MAX_SUPER_CLASS = 5;
    protected IConfigurationElement[] mySuperClassConfElement = new IConfigurationElement[MAX_SUPER_CLASS];
    protected String[] mySuperClassID = new String[MAX_SUPER_CLASS];

    public static int ORIGINAL = 0;
    public static int SUPER = 1;

    protected Version version = null;
    protected String managedBuildRevision = null;

    protected void loadNameAndID(IExtensionPoint root, IConfigurationElement element) {
        myElement = element;
        myID = myElement.getAttribute(ID);
        myName = myElement.getAttribute(NAME);

        int cur = 0;
        mySuperClassID[cur] = element.getAttribute(SUPERCLASS);
        while ((cur < MAX_SUPER_CLASS) && (mySuperClassID[cur] != null)) {
            mySuperClassConfElement[cur] = getSuperClassConfElement(element.getName(), mySuperClassID[cur], root);
            if (mySuperClassConfElement[cur] == null) {
                System.err.println("Failed to load superclass " + mySuperClassID[cur]); //$NON-NLS-1$
            } else {
                mySuperClassID[cur + 1] = mySuperClassConfElement[cur].getAttribute(SUPERCLASS);
            }
            cur++;
        }
        if (myID == null) {
            myID = getAttributes(ID)[SUPER];
        }
        if (myName == null) {
            myName = getAttributes(NAME)[SUPER];
        }
        if (myName.isBlank()) {
            myName = myID;
        }
    }

    protected String[] getAttributes(String attributeName) {
        String[] ret = new String[2];
        ret[SUPER] = ret[ORIGINAL] = myElement.getAttribute(attributeName);
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

    /**
     * Crreate the executable component given in the extension (of one of the
     * superclasses)
     * 
     * @param attributeName
     *            the attributename containing the class info
     * @param compatibilityType
     *            the compatibility type requested 0= no compatibility
     * @return
     */
    protected Object createExecutableExtension(String attributeName) {
        String className = myElement.getAttribute(attributeName);
        IConfigurationElement element = myElement;
        String error = "createExecutableExtension for " + attributeName + " for " + myName + BLANK + className + BLANK //$NON-NLS-1$//$NON-NLS-2$
                + "failed.";
        if (className == null) {
            int cur = 0;
            while ((cur < MAX_SUPER_CLASS) && (mySuperClassConfElement[cur] != null) && (className == null)) {
                element = mySuperClassConfElement[cur];
                className = element.getAttribute(attributeName);
                cur++;
            }
        }
        if (className != null) {
            Object ret = legacyCreateExecutableExtention(element, className, attributeName);
            if (ret == null) {
                System.err.println(error);
            }
            return ret;
        }

        return null;
    }

    /**
     * This method together with some legacy compatibility classes make it possible
     * to continue
     * using the old class names in the plugin.xml for ported classes
     * 
     * @param element
     * 
     * @param className
     *            the FQN classname (can not be null)
     * @param attributeName
     * @return the loaded class or null if failed
     */
    private Object legacyCreateExecutableExtention(IConfigurationElement element, String className,
            String attributeName) {
        Object ret = null;
        try {
            ret = element.createExecutableExtension(attributeName);
            if (ret != null) {
                return ret;
            }
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            //e.printStackTrace();
        }
        // The provided class does not conform with the current class
        //try older types of classes
        //Currently no known cases will actually work

        try {
            switch (className) {
            case "org.eclipse.cdt.managedbuilder.makegen.gnu.GnuLinkOutputNameProvider": //$NON-NLS-1$
                return new OutputNameProviderCompatibilityClass();
            }
        } catch (Exception e) {
            // if this fails it is likely a class name is in the plugin.xml that is not a class name
            //not dumping the stacktrace as the error is reported in calling method
            //e.printStackTrace();
        }

        return null;
    }

    List<IConfigurationElement> getAllChildren() {
        ArrayList<IConfigurationElement> ret = new ArrayList<>();
        ret.addAll(Arrays.asList(myElement.getChildren()));
        int cur = 0;
        while ((cur < MAX_SUPER_CLASS) && (mySuperClassConfElement[cur] != null)) {
            ret.addAll(Arrays.asList(mySuperClassConfElement[cur].getChildren()));
            cur++;
        }
        return ret;
    }

    List<IConfigurationElement> getAllChildren(String builderElementName) {
        ArrayList<IConfigurationElement> ret = new ArrayList<>();
        ret.addAll(Arrays.asList(myElement.getChildren(builderElementName)));
        int cur = 0;
        while ((cur < MAX_SUPER_CLASS) && (mySuperClassConfElement[cur] != null)) {
            ret.addAll(Arrays.asList(mySuperClassConfElement[cur].getChildren(builderElementName)));
            cur++;
        }
        return ret;
    }

    List<IConfigurationElement> getFirstChildren(String builderElementName) {
        ArrayList<IConfigurationElement> ret = new ArrayList<>();
        ret.addAll(Arrays.asList(myElement.getChildren(builderElementName)));
        int cur = 0;
        while ((cur < MAX_SUPER_CLASS) && (mySuperClassConfElement[cur] != null)) {
            ret.addAll(Arrays.asList(mySuperClassConfElement[cur].getChildren(builderElementName)));
            if (ret.size() > 0) {
                return ret;
            }
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

    protected static String resolvedState(Object field) {
        if (field == null) {
            return "UNRESOLVED"; //$NON-NLS-1$
        }
        return "RESOLVED"; //$NON-NLS-1$
    }

    //added for backward compatibility
    @Override
    public boolean hasAncestor(String id) {
        if (myID.equals(id))
            return true;
        for (int i = 0; i < MAX_SUPER_CLASS; i++) {
            if (id.equals(mySuperClassID[i])) {
                return true;
            }
        }
        return false;
    }

    protected Map<String, String> parseProperties(String inProperties) {
        Map<String, String> ret = new HashMap<>();
        String[] properties = inProperties.split(COMMA);
        for (String property : properties) {
            if (!property.isBlank()) {
                String[] parts = property.split(EQUAL);
                if (parts.length == 2) {
                    ret.put(parts[0], parts[1]);
                } else {
                    System.err.println("malformed property " + myID + BLANK + inProperties); //$NON-NLS-1$
                }
            }
        }
        return ret;
    }
}