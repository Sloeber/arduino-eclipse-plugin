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

public abstract class PropertyBase {
    private String fId;
    private String fName;

    protected PropertyBase(String id, String name) {
        fId = id;
        fName = name;
    }

    public String getId() {
        return fId;
    }

    public String getName() {
        return fName;
    }

    @Override
    public String toString() {
        return getId();
    }

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(getClass()))
            return false;

        return fId.equals(((PropertyBase) o).getId());
    }

    @Override
    public int hashCode() {
        return fId.hashCode();
    }
}
