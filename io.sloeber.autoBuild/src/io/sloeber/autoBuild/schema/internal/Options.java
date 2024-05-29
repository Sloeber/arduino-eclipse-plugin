/*******************************************************************************
 * Copyright (c) 2005, 2016 Symbian Ltd and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Symbian Ltd - Initial API and implementation
 * Baltasar Belyavsky (Texas Instruments) - [405643] HoldsOptions performance improvements
 *******************************************************************************/
package io.sloeber.autoBuild.schema.internal;

import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.*;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IResource;

import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.autoBuild.schema.api.IOption;
import io.sloeber.autoBuild.schema.api.IOptionCategory;
import io.sloeber.autoBuild.schema.api.IOptions;
import io.sloeber.autoBuild.schema.internal.enablement.MBSEnablementExpression;

/**
 * Implements the functionality that is needed to hold options and option
 * categories. In CDT 3.0, the functionality has been moved from ITool and
 * Tool to this class.
 *
 * This class is intended to be used as base class for all MBS grammar
 * elements that can hold Options and Option Categories. These are currently
 * Tool and ToolChain.
 *
 * Note that the member <code>superClass</code> must be shared with the
 * derived class. This requires to wrap this member by access functions
 * in the derived class or frequent casts, because the type of
 * <code>superClass</code>
 * in <code>HoldsOptions</code> must be <code>IHoldOptions</code>. Further
 * note that the member <code>resolved</code> must inherit the value of its
 * derived class. This achieved through the constructor.
 *
 * @since 3.0
 */
public class Options implements IOptions {

    protected Map<String, IOptionCategory> categoryMap = new LinkedHashMap<>();
    protected Map<String, Option> myOptionMap = new LinkedHashMap<>();

    public Options() {
        // nothing to do here
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IHoldsOptions#getOptions()
     */
    @Override
    public List<IOption> getOptions() {
        return List.copyOf(myOptionMap.values());
    }

    // 
    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IHoldsOptions#getOptionById(java.lang.String)
     */
    @Override
    public IOption getOptionById(String id) {
        return myOptionMap.get(id);
    }

    public void add(OptionCategory optionCategory) {
        categoryMap.put(optionCategory.getId(), optionCategory);

    }

    public void add(Option option) {
        myOptionMap.put(option.getId(), option);

    }

    public StringBuffer dump(int leadingChars) {
        StringBuffer ret = new StringBuffer();
        String prepend = DUMPLEAD.repeat(leadingChars);
        ret.append(prepend + IOption.ELEMENT_NAME + BLANK + myOptionMap.size() + NEWLINE);
        for (Option curOption : myOptionMap.values()) {
            ret.append(curOption.dump(leadingChars));
            ret.append(NEWLINE);
        }

        return ret;
    }

    /**
     * Get all the categories that are applicable for this resource
     * That means: all the categories that are enabled for this resource and
     * contain at least one enabled option
     * 
     * @param resource
     *            The resource we are querying for
     * @param autoBuildConf
     *            the autobuild configuration we are dealing with
     * @return a list of enabled categories that contain at least one enabled option
     */
    public Set<IOptionCategory> getCategories(IResource resource, AutoBuildConfigurationDescription autoBuildConf) {
        Set<IOptionCategory> ret = new HashSet<>();
        Set<IOptionCategory> retOrdered = new LinkedHashSet<>();
        for (Option curOption : myOptionMap.values()) {
            if (curOption.isEnabled(MBSEnablementExpression.ENABLEMENT_GUI_VISIBLE, resource, autoBuildConf)) {
                IOptionCategory cat = categoryMap.get(curOption.getCategoryID());
                if (cat != null
                        && cat.isEnabled(MBSEnablementExpression.ENABLEMENT_GUI_VISIBLE, resource, autoBuildConf)) {
                    ret.add(cat);
                }
            }
        }
        ret.remove(null);
        //make sure the order is the same as in the plugin.xml
        for (IOptionCategory curCat : categoryMap.values()) {
            if (ret.contains(curCat)) {
                retOrdered.add(curCat);
            }
        }
        return retOrdered;
    }

    /**
     * Get all the categories that are applicable for this resource
     * That means: all the categories that are enabled for this resource and
     * contain at least one enabled option
     * 
     * @param resource
     *            The resource we are querying for
     * @param autoBuildConf
     *            the autobuild configuration we are dealing with
     * @return a list of enabled categories that contain at least one enabled option
     */
    public Set<IOption> getOptionsOfCategory(IOptionCategory cat, IResource resource,
            AutoBuildConfigurationDescription autoBuildConf) {
        Set<IOption> ret = new LinkedHashSet<>();
        if (!cat.isEnabled(MBSEnablementExpression.ENABLEMENT_GUI_VISIBLE, resource, autoBuildConf)) {
            return ret;
        }
        String catID = cat.getId();

        for (Option curOption : myOptionMap.values()) {
            if (curOption.isEnabled(MBSEnablementExpression.ENABLEMENT_GUI_VISIBLE, resource, autoBuildConf)
                    && catID.equals(curOption.getCategoryID())) {
                ret.add(curOption);
            }
        }
        ret.remove(null);
        return ret;
    }
}
