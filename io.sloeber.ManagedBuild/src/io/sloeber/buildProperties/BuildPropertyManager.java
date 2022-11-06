/*******************************************************************************
 * Copyright (c) 2007, 2011 Intel Corporation and others.
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
package io.sloeber.buildProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import io.sloeber.autoBuild.Internal.BuildProperties;
import io.sloeber.autoBuild.api.IBuildProperty;
import io.sloeber.autoBuild.api.IBuildPropertyType;
import io.sloeber.autoBuild.api.IBuildPropertyValue;
import io.sloeber.autoBuild.core.Activator;

public class BuildPropertyManager implements IBuildPropertyManager {
    public static final String PROPERTIES_EXT_POINT_ID = "org.eclipse.cdt.managedbuilder.core.buildProperties"; //$NON-NLS-1$
    public static final String PROPERTY_VALUE_SEPARATOR = "="; //$NON-NLS-1$
    public static final String PROPERTIES_SEPARATOR = ","; //$NON-NLS-1$
    public static final String ELEMENT_PROPERTY_TYPE = "propertyType"; //$NON-NLS-1$
    public static final String ELEMENT_PROPERTY_VALUE = "propertyValue"; //$NON-NLS-1$
    public static final String ATTRIBUTE_PROPERTY = "property"; //$NON-NLS-1$
    public static final String ATTRIBUTE_NAME = "name"; //$NON-NLS-1$
    public static final String ATTRIBUTE_ID = "id"; //$NON-NLS-1$

    private static BuildPropertyManager fInstance;

    private List<IConfigurationElement> fTypeCfgElements;
    private List<IConfigurationElement> fValueCfgElements;

    private BuildPropertyManager() {
        loadExtensions();
    }

    public static BuildPropertyManager getInstance() {
        if (fInstance == null)
            fInstance = new BuildPropertyManager();
        return fInstance;
    }

    public BuildProperties loadPropertiesFromString(String properties) {
        return new BuildProperties(properties);
    }

    public String savePropertiesToString(BuildProperties properties) {
        return properties.toString();
    }

    private Map<String, IBuildPropertyType> fPropertyTypeMap = new HashMap<>();

    @Override
    public IBuildPropertyType getPropertyType(String id) {
        return fPropertyTypeMap.get(id);
    }

    public IBuildPropertyType createPropertyType(String id, String name) throws CoreException {
        IBuildPropertyType type = getPropertyType(id);
        if (type != null) {
            if (!name.equals(type.getName()))
                throw new CoreException(new Status(IStatus.ERROR, Activator.getId(),
                        "BuildPropertiesMessages.getString(\"BuildPropertyManager.8\")")); //$NON-NLS-1$
        } else {
            type = new BuildPropertyType(id, name);
            fPropertyTypeMap.put(id, type);
        }
        return type;
    }

    public IBuildPropertyValue createPropertyValue(String typeId, String id, String name) throws CoreException {
        IBuildPropertyType type = getPropertyType(typeId);
        if (type == null)
            throw new CoreException(new Status(IStatus.ERROR, Activator.getId(),
                    "BuildPropertiesMessages.getString(\"BuildPropertyManager.9\")")); //$NON-NLS-1$

        return createPropertyValue(type, id, name);
    }

    public IBuildPropertyValue createPropertyValue(IBuildPropertyType type, String id, String name)
            throws CoreException {
        BuildPropertyValue value = (BuildPropertyValue) type.getSupportedValue(id);
        if (value != null) {
            if (!name.equals(value.getName()))
                throw new CoreException(new Status(IStatus.ERROR, Activator.getId(),
                        "BuildPropertiesMessages.getString(\"BuildPropertyManager.10\")")); //$NON-NLS-1$
        } else {
            value = new BuildPropertyValue(id, name);
            ((BuildPropertyType) type).addSupportedValue(value);
        }

        return value;
    }

    @Override
    public IBuildPropertyType[] getPropertyTypes() {
        return fPropertyTypeMap.values().toArray(new BuildPropertyType[fPropertyTypeMap.size()]);
    }

    public IBuildProperty createProperty(String id, String value) throws CoreException {
        IBuildPropertyType type = getPropertyType(id);
        if (type == null)
            throw new CoreException(new Status(IStatus.ERROR, Activator.getId(),
                    "BuildPropertiesMessages.getString(\"BuildPropertyManager.11\")")); //$NON-NLS-1$

        BuildProperty property = new BuildProperty(type, value);
        return property;
    }

    private boolean addConfigElement(IConfigurationElement el) {
        if (ELEMENT_PROPERTY_TYPE.equals(el.getName())) {
            getTypeElList(true).add(el);
            return true;
        } else if (ELEMENT_PROPERTY_VALUE.equals(el.getName())) {
            getValueElList(true).add(el);
            return true;
        }
        return false;
    }

    private List<IConfigurationElement> getTypeElList(boolean create) {
        if (fTypeCfgElements == null && create)
            fTypeCfgElements = new ArrayList<>();
        return fTypeCfgElements;
    }

    private List<IConfigurationElement> getValueElList(boolean create) {
        if (fValueCfgElements == null && create)
            fValueCfgElements = new ArrayList<>();
        return fValueCfgElements;
    }

    private void loadExtensions() {
        IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(PROPERTIES_EXT_POINT_ID);
        if (extensionPoint != null) {
            IExtension[] extensions = extensionPoint.getExtensions();
            for (int i = 0; i < extensions.length; ++i) {
                IExtension extension = extensions[i];
                IConfigurationElement els[] = extension.getConfigurationElements();
                for (int k = 0; k < els.length; k++) {
                    addConfigElement(els[k]);
                }
            }

            resolveConfigElements();
        }
    }

    private void resolveConfigElements() {
        List<IConfigurationElement> typeEls = getTypeElList(false);
        if (typeEls != null) {
            for (IConfigurationElement el : typeEls) {
                try {
                    createPropertyType(el);
                } catch (CoreException e) {
                }
            }
        }

        List<IConfigurationElement> valEls = getValueElList(false);
        if (valEls != null) {
            for (IConfigurationElement el : valEls) {
                try {
                    createPropertyValue(el);
                } catch (CoreException e) {
                }
            }
        }

    }

    private IBuildPropertyType createPropertyType(IConfigurationElement el) throws CoreException {
        String id = el.getAttribute(ATTRIBUTE_ID);
        if (id == null)
            throw new CoreException(new Status(IStatus.ERROR, Activator.getId(),
                    "BuildPropertiesMessages.getString(\"BuildPropertyManager.12\")")); //$NON-NLS-1$
        String name = el.getAttribute(ATTRIBUTE_NAME);
        if (name == null)
            throw new CoreException(new Status(IStatus.ERROR, Activator.getId(),
                    "BuildPropertiesMessages.getString(\"BuildPropertyManager.13\")")); //$NON-NLS-1$

        return createPropertyType(id, name);
    }

    private IBuildPropertyValue createPropertyValue(IConfigurationElement el) throws CoreException {
        String id = el.getAttribute(ATTRIBUTE_ID);
        if (id == null)
            throw new CoreException(new Status(IStatus.ERROR, Activator.getId(),
                    "BuildPropertiesMessages.getString(\"BuildPropertyManager.14\")")); //$NON-NLS-1$
        String name = el.getAttribute(ATTRIBUTE_NAME);
        if (name == null)
            throw new CoreException(new Status(IStatus.ERROR, Activator.getId(),
                    "BuildPropertiesMessages.getString(\"BuildPropertyManager.15\")")); //$NON-NLS-1$
        String property = el.getAttribute(ATTRIBUTE_PROPERTY);
        if (property == null)
            throw new CoreException(new Status(IStatus.ERROR, Activator.getId(),
                    "BuildPropertiesMessages.getString(\"BuildPropertyManager.16\")")); //$NON-NLS-1$

        return createPropertyValue(property, id, name);
    }
}
