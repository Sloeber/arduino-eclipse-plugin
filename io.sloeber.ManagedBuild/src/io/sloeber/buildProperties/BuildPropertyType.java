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

import java.util.HashMap;
import java.util.Map;

import io.sloeber.autoBuild.api.PropertyBase;

//import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyType;
//import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyValue;

public class BuildPropertyType extends PropertyBase implements IBuildPropertyType {
    private Map<String, BuildPropertyValue> fValuesMap = new HashMap<>();

    BuildPropertyType(String id, String name) {
        super(id, name);
    }

    void addSupportedValue(BuildPropertyValue value) {
        fValuesMap.put(value.getId(), value);
    }

    @Override
    public IBuildPropertyValue[] getSupportedValues() {
        return fValuesMap.values().toArray(new BuildPropertyValue[fValuesMap.size()]);
    }

    @Override
    public IBuildPropertyValue getSupportedValue(String id) {
        return fValuesMap.get(id);
    }
}
