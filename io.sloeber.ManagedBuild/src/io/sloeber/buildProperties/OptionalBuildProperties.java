/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *   Red Hat Inc. - initial contribution
 *******************************************************************************/
package io.sloeber.buildProperties;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.cdt.internal.core.SafeStringInterner;

import io.sloeber.autoBuild.api.IOptionalBuildProperties;

//import org.eclipse.cdt.internal.core.SafeStringInterner;
//import org.eclipse.cdt.managedbuilder.buildproperties.IOptionalBuildProperties;

public class OptionalBuildProperties implements IOptionalBuildProperties {

    public static final String PROPERTY_VALUE_SEPARATOR = "="; //$NON-NLS-1$
    public static final String PROPERTIES_SEPARATOR = ","; //$NON-NLS-1$

    private Map<String, String> fProperties = new HashMap<>();

    public OptionalBuildProperties() {
    }

    public OptionalBuildProperties(String properties) {
        StringTokenizer t = new StringTokenizer(properties, PROPERTIES_SEPARATOR);
        while (t.hasMoreTokens()) {
            String property = t.nextToken();
            int index = property.indexOf(PROPERTY_VALUE_SEPARATOR);
            String id, value;
            if (index != -1) {
                id = SafeStringInterner.safeIntern(property.substring(0, index));
                value = SafeStringInterner.safeIntern(property.substring(index + 1));
            } else {
                id = SafeStringInterner.safeIntern(property);
                value = null;
            }
            fProperties.put(id, value);
        }
    }

    public OptionalBuildProperties(OptionalBuildProperties properties) {
        fProperties.putAll(properties.fProperties);
    }

    @Override
    public String getProperty(String id) {
        return fProperties.get(id);
    }

    @Override
    public void setProperty(String id, String value) {
        fProperties.put(id, value);
    }

    @Override
    public String[] getProperties() {
        return fProperties.values().toArray(new String[fProperties.size()]);
    }

    @Override
    public void removeProperty(String id) {
        fProperties.remove(id);
    }

    @Override
    public String toString() {
        int size = fProperties.size();
        Set<Entry<String, String>> entries = fProperties.entrySet();
        if (size == 0)
            return ""; //$NON-NLS-1$

        StringBuilder buf = new StringBuilder();
        Iterator<Entry<String, String>> iterator = entries.iterator();
        Entry<String, String> entry = iterator.next();
        buf.append(entry.getKey() + PROPERTY_VALUE_SEPARATOR + entry.getValue());

        while (iterator.hasNext()) {
            buf.append(PROPERTIES_SEPARATOR);
            entry = iterator.next();
            buf.append(entry.getKey() + PROPERTY_VALUE_SEPARATOR + entry.getValue());
        }
        return buf.toString();
    }

    @Override
    public Object clone() {
        return new OptionalBuildProperties(this);
    }

    @Override
    public void clear() {
        fProperties.clear();
    }

}
