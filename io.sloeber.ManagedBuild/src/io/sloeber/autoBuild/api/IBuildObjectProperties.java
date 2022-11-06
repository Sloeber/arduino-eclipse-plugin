/*******************************************************************************
 * Copyright (c) 2007, 2010 Intel Corporation and others.
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
package io.sloeber.autoBuild.api;

//import org.eclipse.cdt.managedbuilder.buildproperties.IBuildProperties;
//import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyType;
//import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyValue;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IBuildObjectProperties extends IBuildProperties, IBuildPropertiesRestriction {
    IBuildPropertyType[] getSupportedTypes();

    IBuildPropertyValue[] getSupportedValues(String typeId);
}
