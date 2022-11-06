/*******************************************************************************
 * Copyright (c) 2007 Intel Corporation and others.
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

import io.sloeber.autoBuild.api.IBuildPropertyValue;

//import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyValue;

public class BuildPropertyValue extends PropertyBase implements IBuildPropertyValue {
    BuildPropertyValue(String id, String name) {
        super(id, name);
    }
}
