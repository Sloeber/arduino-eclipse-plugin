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
 * IBM Corporation
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.internal.core.SafeStringInterner;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IContributor;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.InvalidRegistryObjectException;

public class SupportedProperties implements IConfigurationElement {
    public static final String SUPPORTED_PROPERTIES = "supportedProperties"; //$NON-NLS-1$
    public static final String PROPERTY = "property"; //$NON-NLS-1$
    public static final String PROPERTY_VALUE = "value"; //$NON-NLS-1$
    public static final String ID = "id"; //$NON-NLS-1$
    public static final String REQUIRED = "required"; //$NON-NLS-1$

    private HashMap<String, SupportedProperty> fSupportedProperties = new HashMap<>();

    private class SupportedProperty {
        private boolean fIsRequired;
        private Set<String> fValues = new HashSet<>();
        private String fId;

        SupportedProperty(String id) {
            fId = id;
        }

        void updateRequired(boolean required) {
            if (!fIsRequired)
                fIsRequired = required;
        }

        public String getId() {
            return fId;
        }

        /*		SupportedProperty(IManagedConfigElement el) {
        			fId = el.getAttribute(ID);
        
        //			IBuildPropertyType type = mngr.getPropertyType(id);
        //			if(type == null)
        //				continue;
        
        			fIsRequired = Boolean.valueOf(el.getAttribute(REQUIRED)).booleanValue();
        
        			fValues = new HashSet();
        
        			IManagedConfigElement values[] = el.getChildren();
        			for(int k = 0; k < values.length; k++){
        				IManagedConfigElement value = values[k];
        				if(PROPERTY_VALUE.equals(value.getName())){
        					String valueId = value.getAttribute(ID);
        					if(valueId == null && valueId.length() == 0)
        						continue;
        
        //					IBuildPropertyValue val = type.getSupportedValue(valueId);
        //					if(val != null)
        //						set.add(val.getId());
        					fValues.add(valueId);
        				}
        			}
        		}
        */
        //		public boolean isValid(){
        //			return fId != null && fValues.size() != 0;
        //		}

        public boolean isRequired() {
            return fIsRequired;
        }

        public void addValueIds(Set<String> ids) {
            fValues.addAll(ids);
        }

        public boolean supportsValue(String id) {
            return fValues.contains(id);
        }

        public String[] getSupportedValues() {
            return fValues.toArray(new String[fValues.size()]);
        }

    }

    public SupportedProperties(IConfigurationElement el) {
        //		IBuildPropertyManager mngr = BuildPropertyManager.getInstance();

        IConfigurationElement children[] = el.getChildren();
        for (int i = 0; i < children.length; i++) {
            IConfigurationElement child = children[i];
            if (PROPERTY.equals(child.getName())) {
                String id = SafeStringInterner.safeIntern(child.getAttribute(ID));
                if (id == null)
                    continue;

                boolean required = Boolean.valueOf(el.getAttribute(REQUIRED)).booleanValue();

                //				IBuildPropertyType type = mngr.getPropertyType(id);
                //				if(type == null)
                //					continue;

                Set<String> set = new HashSet<>();

                IConfigurationElement values[] = child.getChildren();
                for (int k = 0; k < values.length; k++) {
                    IConfigurationElement value = values[k];
                    if (PROPERTY_VALUE.equals(value.getName())) {
                        String valueId = SafeStringInterner.safeIntern(value.getAttribute(ID));
                        if (valueId == null || valueId.length() == 0)
                            continue;

                        //						IBuildPropertyValue val = type.getSupportedValue(valueId);
                        //						if(val != null)
                        //							set.add(val.getId());

                        set.add(valueId);
                    }
                }

                if (set.size() != 0) {
                    SupportedProperty stored = fSupportedProperties.get(id);
                    if (stored == null) {
                        stored = new SupportedProperty(id);
                        fSupportedProperties.put(id, stored);
                    }
                    stored.addValueIds(set);
                    stored.updateRequired(required);
                }
            }
        }

    }

    //	public boolean supportsType(IBuildPropertyType type) {
    //		return supportsType(type.getId());
    //	}

    public boolean supportsType(String type) {
        return fSupportedProperties.containsKey(type);
    }

    public boolean supportsValue(String type, String value) {
        boolean suports = false;
        SupportedProperty prop = fSupportedProperties.get(type);
        if (prop != null) {
            suports = prop.supportsValue(value);
        }
        return suports;
    }

    //	public boolean supportsValue(IBuildPropertyType type,
    //			IBuildPropertyValue value) {
    //		return supportsValue(type.getId(), value.getId());
    //	}

    public String[] getRequiredTypeIds() {
        List<String> list = new ArrayList<>(fSupportedProperties.size());
        Collection<SupportedProperty> values = fSupportedProperties.values();
        for (SupportedProperty prop : values) {
            if (prop.isRequired())
                list.add(prop.getId());
        }
        return list.toArray(new String[list.size()]);
    }

    public String[] getSupportedTypeIds() {
        String result[] = new String[fSupportedProperties.size()];
        fSupportedProperties.keySet().toArray(result);
        return result;
    }

    public String[] getSupportedValueIds(String typeId) {
        SupportedProperty prop = fSupportedProperties.get(typeId);
        if (prop != null)
            return prop.getSupportedValues();
        return new String[0];
    }

    public boolean requiresType(String typeId) {
        SupportedProperty prop = fSupportedProperties.get(typeId);
        if (prop != null)
            return prop.isRequired();
        return false;
    }

    @Override
    public Object createExecutableExtension(String propertyName) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getAttribute(String name) throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getAttribute(String attrName, String locale) throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getAttributeAsIs(String name) throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getAttributeNames() throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IConfigurationElement[] getChildren() throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IConfigurationElement[] getChildren(String name) throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IExtension getDeclaringExtension() throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getName() throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object getParent() throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getValue() throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getValue(String locale) throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getValueAsIs() throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNamespace() throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getNamespaceIdentifier() throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IContributor getContributor() throws InvalidRegistryObjectException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean isValid() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public int getHandleId() {
        // TODO Auto-generated method stub
        return 0;
    }

}
