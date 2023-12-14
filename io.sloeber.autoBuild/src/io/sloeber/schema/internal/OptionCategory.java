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
    private IOptionCategory myOwner = null;
    private boolean myIsOwnerKnown = false;
    private URL myIconPathURL;
    private int myWeight = 50;

    private String[] myModelOwner;
    private String[] myModelIcon;
    private String[] myModelWeight;

    /**
     * This constructor is called to create an option category defined by an
     * extension point in a plugin manifest file
     *
     * @param parent
     *            The parejnt in the manifest file
     * @param element
     *            The category definition from the manifest file
     */
    public OptionCategory(ISchemaObject parent, IExtensionPoint root, IConfigurationElement element) {

        loadNameAndID(root, element);

        myModelOwner = getAttributes(OWNER);
        myModelIcon = getAttributes(ICON);
        myModelWeight = getAttributes(WEIGHT);

        resolveFields();

    }

    private void resolveFields() {
        if (!myModelIcon[SUPER].isBlank()) {
            try {
                myIconPathURL = new URL(myModelIcon[SUPER]);
            } catch (@SuppressWarnings("unused") MalformedURLException e) {
                AutoBuildManager.outputIconError(myModelIcon[SUPER]);
                myIconPathURL = null;
            }
        }
        if (!myModelWeight[SUPER].isBlank()) {
            myWeight = Integer.valueOf(myModelWeight[SUPER]).intValue();
        }

    }

    @Override
    public IOptionCategory getOwner() {
        if (!myIsOwnerKnown) {
            // TOFIX did some test to see it this is needed. If this code is still here ... how did the test go?
            //the test was there because this method was only used in one single case ToolListContentProvider
            //currently I do not know a easy way to get to the owner (based on a ID
            //That is ... I'm sure I have this type of code somewhere
            myOwner = null;
            myIsOwnerKnown = true;
        }
        return myOwner;
    }

    @Override
    public URL getIconPath() {
        return myIconPathURL;
    }

    @Override
    public int getWeight() {
        return myWeight;
    }

}
