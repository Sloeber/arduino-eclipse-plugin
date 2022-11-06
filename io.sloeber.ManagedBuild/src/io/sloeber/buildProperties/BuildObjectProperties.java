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
import java.util.List;

//import org.eclipse.cdt.managedbuilder.buildproperties.IBuildProperty;
//import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyType;
//import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyValue;
//import org.eclipse.cdt.managedbuilder.core.IBuildObjectProperties;
//import org.eclipse.cdt.managedbuilder.core.IBuildPropertiesRestriction;
//import org.eclipse.cdt.managedbuilder.internal.buildproperties.BuildProperties;
//import org.eclipse.cdt.managedbuilder.internal.buildproperties.BuildPropertyManager;
import org.eclipse.core.runtime.CoreException;

import io.sloeber.autoBuild.Internal.BuildProperties;
import io.sloeber.autoBuild.Internal.IBuildPropertyChangeListener;
import io.sloeber.autoBuild.api.IBuildObjectProperties;
import io.sloeber.autoBuild.api.IBuildPropertiesRestriction;
import io.sloeber.autoBuild.api.IBuildProperty;
import io.sloeber.autoBuild.api.IBuildPropertyType;
import io.sloeber.autoBuild.api.IBuildPropertyValue;

public class BuildObjectProperties extends BuildProperties implements IBuildObjectProperties {
    private IBuildPropertiesRestriction fRestriction;
    private IBuildPropertyChangeListener fListener;

    public BuildObjectProperties(IBuildPropertiesRestriction restriction, IBuildPropertyChangeListener listener) {
        super();
        fRestriction = restriction;
        fListener = listener;
    }

    public BuildObjectProperties(BuildObjectProperties properties, IBuildPropertiesRestriction restriction,
            IBuildPropertyChangeListener listener) {
        super(properties);
        fRestriction = restriction;
        fListener = listener;
    }

    public BuildObjectProperties(String properties, IBuildPropertiesRestriction restriction,
            IBuildPropertyChangeListener listener) {
        super(properties);
        fRestriction = restriction;
        fListener = listener;
    }

    @Override
    public IBuildPropertyType[] getSupportedTypes() {
        IBuildPropertyType types[] = BuildPropertyManager.getInstance().getPropertyTypes();

        if (fRestriction != null && types.length != 0) {
            List<IBuildPropertyType> list = new ArrayList<>(types.length);
            for (IBuildPropertyType type : types) {
                if (fRestriction.supportsType(type.getId()))
                    list.add(type);
            }

            types = list.toArray(new IBuildPropertyType[list.size()]);
        }

        return types;
    }

    @Override
    public IBuildPropertyValue[] getSupportedValues(String typeId) {
        IBuildPropertyType type = BuildPropertyManager.getInstance().getPropertyType(typeId);
        if (type != null) {
            IBuildPropertyValue values[] = type.getSupportedValues();
            if (fRestriction != null && values.length != 0) {
                List<IBuildPropertyValue> list = new ArrayList<>(values.length);
                for (IBuildPropertyValue value : values) {
                    if (fRestriction.supportsValue(type.getId(), value.getId()))
                        list.add(value);
                }

                return list.toArray(new IBuildPropertyValue[list.size()]);
            }
        }
        return new IBuildPropertyValue[0];
    }

    @Override
    public boolean supportsType(String id) {
        return fRestriction.supportsType(id);
        //		IBuildPropertyType type = BuildPropertyManager.getInstance().getPropertyType(id);
        //		if(type != null){
        //			if(fRestriction != null){
        //				return fRestriction.supportsType(type.getId());
        //			}
        //			return true;
        //		}
        //		return false;
    }

    @Override
    public boolean supportsValue(String typeId, String valueId) {
        return fRestriction.supportsValue(typeId, valueId);
        //		IBuildPropertyType type = BuildPropertyManager.getInstance().getPropertyType(typeId);
        //		if(type != null){
        //			IBuildPropertyValue value = type.getSupportedValue(valueId);
        //			if(value != null){
        //				if(fRestriction != null){
        //					return fRestriction.supportsValue(type.getId(), value.getId());
        //				}
        //				return true;
        //			}
        //		}
        //		return false;
    }

    @Override
    public void clear() {
        super.clear();
        fListener.propertiesChanged();
    }

    @Override
    public IBuildProperty removeProperty(String id) {
        IBuildProperty property = super.removeProperty(id);
        if (property != null)
            fListener.propertiesChanged();
        return property;
    }

    public IBuildProperty internalSetProperty(String propertyId, String propertyValue) throws CoreException {
        return super.setProperty(propertyId, propertyValue);
    }

    @Override
    public IBuildProperty setProperty(String propertyId, String propertyValue) throws CoreException {
        //		if(!supportsType(propertyId))
        //			throw new CoreException(new Status(IStatus.ERROR,
        //					ManagedBuilderCorePlugin.getUniqueIdentifier(),
        //					"property type is not supported"));
        //		if(!supportsValue(propertyId, propertyValue))
        //			throw new CoreException(new Status(IStatus.ERROR,
        //					ManagedBuilderCorePlugin.getUniqueIdentifier(),
        //					"property value is not supported"));

        IBuildProperty property = super.setProperty(propertyId, propertyValue);
        fListener.propertiesChanged();
        return property;
    }

    @Override
    public String[] getRequiredTypeIds() {
        return fRestriction.getRequiredTypeIds();
    }

    @Override
    public boolean requiresType(String typeId) {
        return fRestriction.requiresType(typeId);
    }

    @Override
    public String[] getSupportedTypeIds() {
        return fRestriction.getSupportedTypeIds();
    }

    @Override
    public String[] getSupportedValueIds(String typeId) {
        return fRestriction.getSupportedValueIds(typeId);
    }
}
