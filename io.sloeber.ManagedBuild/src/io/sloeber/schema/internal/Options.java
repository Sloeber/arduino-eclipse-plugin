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
package io.sloeber.schema.internal;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import io.sloeber.schema.api.IOptions;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.IOptionCategory;

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

    protected Map<String, IOptionCategory> categoryMap = new HashMap<>();
    protected Map<String, Option> myOptionMap = new HashMap<>();

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

    /* (non-Javadoc)
     * org.eclipse.cdt.managedbuilder.core.IHoldsOptions#getOptionCategory()
     */
    @Override
    public IOptionCategory getOptionCategory(String id) {
        return categoryMap.get(id);
    }

    public void add(OptionCategory optionCategory) {
        categoryMap.put(optionCategory.getId(), optionCategory);

    }

    public void add(Option option) {
        myOptionMap.put(option.getId(), option);

    }

    public StringBuffer dump(int leadingChars) {
        StringBuffer ret = new StringBuffer();
        String prepend = StringUtils.repeat(DUMPLEAD, leadingChars);
        ret.append(prepend + IOption.ELEMENT_NAME + BLANK + myOptionMap.size() + NEWLINE);
        for (Option curOption : myOptionMap.values()) {
            ret.append(curOption.dump(leadingChars));
            ret.append(NEWLINE);
        }

        return ret;
    }

}
