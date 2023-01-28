/*******************************************************************************
 * Copyright (c) 2004, 2011 Intel Corporation and others.
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
package io.sloeber.autoBuild.Internal;

import io.sloeber.schema.internal.SchemaObject;

//import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;

public class MatchKey<T extends SchemaObject> {
    private T buildObject;

    public MatchKey(T builder) {
        this.buildObject = builder;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof MatchKey))
            return false;
        @SuppressWarnings("unchecked")
        MatchKey<T> other = (MatchKey<T>) obj;
        return performMatchComparison(other.buildObject);
    }

    @Override
    public int hashCode() {
        String name = buildObject.getName();
        if (name == null)
            name = buildObject.getId();
        int code = name.hashCode();
        String version = ManagedBuildManager.getVersionFromIdAndVersion(buildObject.getId());
        if (version != null)
            code += version.hashCode();
        return code;
    }

    boolean performMatchComparison(T bo) {
        if (bo == null)
            return false;

        if (bo == buildObject)
            return true;

        //		if(bo.isReal() && buildObject.isReal())
        //			return false;
        //		if(!bo.getToolCommand().equals(buildObject.getToolCommand()))
        //			return false;

        if (!bo.getName().equals(buildObject.getName()))
            return false;

        String thisVersion = ManagedBuildManager.getVersionFromIdAndVersion(buildObject.getId());
        String otherVersion = ManagedBuildManager.getVersionFromIdAndVersion(bo.getId());
        if (thisVersion == null || thisVersion.length() == 0) {
            if (otherVersion != null && otherVersion.length() != 0)
                return false;
        } else {
            if (!thisVersion.equals(otherVersion))
                return false;
        }

        return true;
        /*		IOption options[] = buildObject.getOptions();
        		IOption otherOptions[] = bo.getOptions();
        
        		if(!ListComparator.match(options,
        				otherOptions,
        				new Comparator(){
        			public boolean equal(Object o1, Object o2){
        				return ((Option)o1).matches((Option)o2);
        			}
        		}))
        			return false;
        
        		return true;
        		*/
    }

}
