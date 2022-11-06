/*******************************************************************************
 * Copyright (c) 2005, 2010 Intel Corporation and others.
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

import java.util.Map;
import java.util.Set;

import io.sloeber.autoBuild.api.IManagedConfigElement;

//import org.eclipse.cdt.managedbuilder.core.IManagedConfigElement;

public abstract class CompositeExpression implements IBooleanExpression {
    private IBooleanExpression fChildren[];

    protected CompositeExpression(IManagedConfigElement element) {
        IManagedConfigElement childElement[] = element.getChildren();
        IBooleanExpression children[] = new IBooleanExpression[childElement.length];
        int num = 0;
        for (int i = 0; i < childElement.length; i++) {
            IBooleanExpression child = createExpression(childElement[i]);
            if (child != null)
                children[num++] = child;
        }

        if (num < children.length) {
            IBooleanExpression tmp[] = new IBooleanExpression[num];
            System.arraycopy(children, 0, tmp, 0, num);
            children = tmp;
        }
        fChildren = children;
    }

    protected IBooleanExpression createExpression(IManagedConfigElement element) {
        String name = element.getName();
        //TOFIX this code should not be removerd but treplaced
        //        if (AndExpression.NAME.equals(name))
        //            return new AndExpression(element);
        //        else if (OrExpression.NAME.equals(name))
        //            return new OrExpression(element);
        //        else if (NotExpression.NAME.equals(name))
        //            return new NotExpression(element);
        //        else if (CheckOptionExpression.NAME.equals(name))
        //            return new CheckOptionExpression(element);
        //        else if (CheckStringExpression.NAME.equals(name))
        //            return new CheckStringExpression(element);
        //        else if (FalseExpression.NAME.equals(name))
        //            return new FalseExpression(element);
        //        else if (CheckHolderExpression.NAME.equals(name))
        //            return new CheckHolderExpression(element);
        //        else if (CheckBuildPropertyExpression.NAME.equals(name))
        //            return new CheckBuildPropertyExpression(element);
        //        else if (HasNatureExpression.NAME.equals(name))
        //            return new HasNatureExpression(element);
        return null;
    }

    public IBooleanExpression[] getChildren() {
        return fChildren;
    }

    public Map<String, Set<String>> getReferencedProperties(Map<String, Set<String>> map) {
        //TOFIX implement something
        //        IBooleanExpression children[] = getChildren();
        //        if (map == null)
        //            map = new HashMap<>();
        //
        //        for (int i = 0; i < children.length; i++) {
        //            IBooleanExpression child = children[i];
        //            if (child instanceof CompositeExpression) {
        //                ((CompositeExpression) child).getReferencedProperties(map);
        //            } else if (child instanceof CheckBuildPropertyExpression) {
        //                CheckBuildPropertyExpression bp = (CheckBuildPropertyExpression) child;
        //                String prop = bp.getPropertyId();
        //                String val = bp.getValueId();
        //                if (prop != null && prop.length() != 0 && val != null && val.length() != 0) {
        //                    Set<String> set = map.get(prop);
        //                    if (set == null) {
        //                        set = new HashSet<>();
        //                        map.put(prop, set);
        //                    }
        //                    set.add(val);
        //                }
        //            }
        //        }
        return map;
    }
}
