/*******************************************************************************
 * Copyright (c) 2005, 2011 Intel Corporation and others.
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
 * Miwako Tokugawa (Intel Corporation) - bug 222817 (OptionCategoryApplicability)
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import org.eclipse.core.runtime.IConfigurationElement;

import io.sloeber.autoBuild.api.IHoldsOptions;
import io.sloeber.autoBuild.api.IOption;
import io.sloeber.autoBuild.api.IOptionCategory;
import io.sloeber.autoBuild.api.IResourceInfo;

public class AndExpression extends CompositeExpression {
    public static final String NAME = "and"; //$NON-NLS-1$

    public AndExpression(IConfigurationElement element) {
        super(element);
    }

    @Override
    public boolean evaluate(IResourceInfo rcInfo, IHoldsOptions holder, IOption option) {
        IBooleanExpression children[] = getChildren();
        for (int i = 0; i < children.length; i++) {
            if (!children[i].evaluate(rcInfo, holder, option))
                return false;
        }
        return true;
    }

    @Override
    public boolean evaluate(IResourceInfo rcInfo, IHoldsOptions holder, IOptionCategory category) {
        IBooleanExpression children[] = getChildren();
        for (int i = 0; i < children.length; i++) {
            if (!children[i].evaluate(rcInfo, holder, category))
                return false;
        }
        return true;
    }
}
