/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     Miwako Tokugawa (Intel Corporation) - bug 222817 (OptionCategoryApplicability)
 *******************************************************************************/
package io.sloeber.schema.internal;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;
import java.net.MalformedURLException;
import java.net.URL;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;

import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.schema.api.IOptionCategory;
import io.sloeber.schema.api.ISchemaObject;

public class OptionCategory extends SchemaObject implements IOptionCategory {
    private IOptionCategory owner; // The logical Option Category parent
    private URL iconPathURL;

    private String[] modelOwner;
    private String[] modelIcon;

    /**
     * This constructor is called to create an option category defined by an
     * extension point in a plugin manifest file, or returned by a dynamic element
     * provider
     *
     * @param parent
     *            The IHoldsOptions parent of this category, or
     *            <code>null</code> if defined at the top level
     * @param element
     *            The category definition from the manifest file or a dynamic
     *            element provider
     */
    public OptionCategory(ISchemaObject parent, IExtensionPoint root, IConfigurationElement element) {

        loadNameAndID(root, element);

        modelOwner = getAttributes(OWNER);
        modelIcon = getAttributes(ICON);

        //		myOptionEnablementExpressions.clear();
        //		IConfigurationElement enablements[] = element.getChildren(OptionEnablementExpression.NAME);
        //		for (IConfigurationElement curEnablement : enablements) {
        //			myOptionEnablementExpressions.add(new OptionEnablementExpression(curEnablement));
        //		}
        //
        //		booleanExpressionCalculator = new BooleanExpressionApplicabilityCalculator(myOptionEnablementExpressions);

        if (!modelIcon[SUPER].isBlank()) {
            try {
                iconPathURL = new URL(modelIcon[SUPER]);
            } catch (@SuppressWarnings("unused") MalformedURLException e) {
                AutoBuildManager.outputIconError(modelIcon[SUPER]);
                iconPathURL = null;
            }
        }
        resolveFields();

    }

    private void resolveFields() {
        //TOFIX JABA need to find out what this holder is all about
        //		if (!modelOwner[SUPER].isBlank()) {
        //			owner = holder.getOptionCategory(modelOwner[SUPER]);
        //			if (owner == null) {
        //				if (holder instanceof IOptionCategory) {
        //					// Report error, only if the parent is a tool and thus also
        //					// an option category.
        //					ManagedBuildManager.outputResolveError("owner", //$NON-NLS-1$
        //							modelOwner[SUPER], "optionCategory", //$NON-NLS-1$
        //							getId());
        //				} else if (false == holder.getId().equals(modelOwner[SUPER])) {
        //					// Report error, if the holder ID does not match the owner's ID.
        //					ManagedBuildManager.outputResolveError("owner", //$NON-NLS-1$
        //							modelOwner[SUPER], "optionCategory", //$NON-NLS-1$
        //							getId());
        //				}
        //			}
        //		}
        //		if (owner == null) {
        //			owner = getNullOptionCategory();
        //		}

    }

    @Override
    public IOptionCategory getOwner() {
        return owner;
    }

    @Override
    public URL getIconPath() {
        return iconPathURL;
    }

}
