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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.sloeber.autoBuild.api.IBuildObject;
import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.api.IFileInfo;
import io.sloeber.autoBuild.api.IFolderInfo;
import io.sloeber.autoBuild.api.IHoldsOptions;
import io.sloeber.autoBuild.api.IInputType;
import io.sloeber.autoBuild.api.IManagedConfigElement;
import io.sloeber.autoBuild.api.IOption;
import io.sloeber.autoBuild.api.IOptionCategory;
import io.sloeber.autoBuild.api.IOutputType;
import io.sloeber.autoBuild.api.IResourceInfo;
import io.sloeber.autoBuild.api.ITool;
import io.sloeber.autoBuild.api.IToolChain;
import io.sloeber.autoBuild.extensionPoint.IOptionApplicability;
import io.sloeber.autoBuild.extensionPoint.IOptionCategoryApplicability;

//import org.eclipse.cdt.managedbuilder.core.IBuildObject;
//import org.eclipse.cdt.managedbuilder.core.IConfiguration;
//import org.eclipse.cdt.managedbuilder.core.IFileInfo;
//import org.eclipse.cdt.managedbuilder.core.IFolderInfo;
//import org.eclipse.cdt.managedbuilder.core.IHoldsOptions;
//import org.eclipse.cdt.managedbuilder.core.IInputType;
//import org.eclipse.cdt.managedbuilder.core.IManagedConfigElement;
//import org.eclipse.cdt.managedbuilder.core.IOption;
//import org.eclipse.cdt.managedbuilder.core.IOptionApplicability;
//import org.eclipse.cdt.managedbuilder.core.IOptionCategory;
//import org.eclipse.cdt.managedbuilder.core.IOptionCategoryApplicability;
//import org.eclipse.cdt.managedbuilder.core.IOutputType;
//import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
//import org.eclipse.cdt.managedbuilder.core.ITool;
//import org.eclipse.cdt.managedbuilder.core.IToolChain;
//import org.eclipse.cdt.managedbuilder.internal.enablement.AdjustmentContext;
//import org.eclipse.cdt.managedbuilder.internal.enablement.OptionEnablementExpression;

public class BooleanExpressionApplicabilityCalculator implements IOptionApplicability, IOptionCategoryApplicability {
    private OptionEnablementExpression fExpressions[];

    private Map<String, Set<String>> fRefPropsMap;

    public BooleanExpressionApplicabilityCalculator(IManagedConfigElement optionElement) {
        this(optionElement.getChildren(OptionEnablementExpression.NAME));
    }

    public BooleanExpressionApplicabilityCalculator(IManagedConfigElement enablementElements[]) {
        fExpressions = new OptionEnablementExpression[enablementElements.length];

        for (int i = 0; i < enablementElements.length; i++) {
            fExpressions[i] = new OptionEnablementExpression(enablementElements[i]);
        }
    }

    @Override
    public boolean isOptionVisible(IBuildObject configuration, IHoldsOptions holder, IOption option) {
        IResourceInfo rcInfo = rcInfoFromConfiguration(configuration);
        if (rcInfo != null)
            return evaluate(rcInfo, holder, option, OptionEnablementExpression.FLAG_UI_VISIBILITY);
        return true;
    }

    public static IResourceInfo rcInfoFromConfiguration(IBuildObject configuration) {
        if (configuration instanceof IFolderInfo)
            return (IFolderInfo) configuration;
        if (configuration instanceof IFileInfo)
            return (IFileInfo) configuration;
        if (configuration instanceof IConfiguration)
            return ((IConfiguration) configuration).getRootFolderInfo();
        return null;
    }

    public boolean isInputTypeEnabled(ITool tool, IInputType type) {
        return evaluate(tool.getParentResourceInfo(), tool, null, OptionEnablementExpression.FLAG_CMD_USAGE);
    }

    public boolean isOutputTypeEnabled(ITool tool, IOutputType type) {
        return evaluate(tool.getParentResourceInfo(), tool, null, OptionEnablementExpression.FLAG_CMD_USAGE);
    }

    public boolean isToolUsedInCommandLine(IResourceInfo rcInfo, ITool tool) {
        return evaluate(rcInfo, tool, null, OptionEnablementExpression.FLAG_CMD_USAGE);
    }

    @Override
    public boolean isOptionEnabled(IBuildObject configuration, IHoldsOptions holder, IOption option) {
        IResourceInfo rcInfo = rcInfoFromConfiguration(configuration);
        if (rcInfo != null)
            return evaluate(rcInfo, holder, option, OptionEnablementExpression.FLAG_UI_ENABLEMENT);
        return true;
    }

    @Override
    public boolean isOptionUsedInCommandLine(IBuildObject configuration, IHoldsOptions holder, IOption option) {
        IResourceInfo rcInfo = rcInfoFromConfiguration(configuration);
        if (rcInfo != null)
            return evaluate(rcInfo, holder, option, OptionEnablementExpression.FLAG_CMD_USAGE);
        return true;
    }

    public boolean evaluate(IResourceInfo rcInfo, IHoldsOptions holder, IOption option, int flags) {
        for (int i = 0; i < fExpressions.length; i++) {
            if (!fExpressions[i].evaluate(rcInfo, holder, option, flags))
                return false;
        }
        return true;
    }

    /*	public boolean performAdjustment(IBuildObject configuration,
    			IHoldsOptions holder, IOption option, boolean extensionAdjustment){
    		boolean adjusted = false;
    		for(int i = 0; i < fExpressions.length; i++){
    			if(fExpressions[i].performAdjustment(configuration, holder, option, extensionAdjustment))
    				adjusted = true;
    		}
    		return adjusted;
    	}
    */
    public boolean adjustOption(IResourceInfo rcInfo, IHoldsOptions holder, IOption option,
            boolean extensionAdjustment) {
        boolean adjusted = false;
        AdjustmentContext context = extensionAdjustment ? null : new AdjustmentContext();
        for (int i = 0; i < fExpressions.length; i++) {
            if (fExpressions[i].adjustOption(rcInfo, holder, option, context, extensionAdjustment))
                adjusted = true;
        }

        if (context != null) {
            String unadjusted[] = context.getUnadjusted();
            for (int i = 0; i < unadjusted.length; i++) {
                OptionEnablementExpression.adjustOption(rcInfo, holder, option, unadjusted[i], null,
                        extensionAdjustment);
            }
        }
        return adjusted;
    }

    public boolean adjustToolChain(IFolderInfo info, IToolChain tChain, boolean extensionAdjustment) {
        boolean adjusted = false;
        AdjustmentContext context = extensionAdjustment ? null : new AdjustmentContext();
        for (int i = 0; i < fExpressions.length; i++) {
            if (fExpressions[i].adjustToolChain(info, tChain, context, extensionAdjustment))
                adjusted = true;
        }

        if (context != null) {
            String unadjusted[] = context.getUnadjusted();
            for (int i = 0; i < unadjusted.length; i++) {
                OptionEnablementExpression.adjustToolChain(info, tChain, unadjusted[i], null, extensionAdjustment);
            }
        }

        return adjusted;
    }

    public boolean adjustTool(IResourceInfo info, ITool tool, boolean extensionAdjustment) {
        boolean adjusted = false;
        AdjustmentContext context = extensionAdjustment ? null : new AdjustmentContext();
        for (int i = 0; i < fExpressions.length; i++) {
            if (fExpressions[i].adjustTool(info, tool, context, extensionAdjustment))
                adjusted = true;
        }

        if (context != null) {
            String unadjusted[] = context.getUnadjusted();
            for (int i = 0; i < unadjusted.length; i++) {
                OptionEnablementExpression.adjustTool(info, tool, unadjusted[i], null, extensionAdjustment);
            }
        }

        return adjusted;
    }

    public boolean adjustConfiguration(IConfiguration cfg, boolean extensionAdjustment) {
        boolean adjusted = false;
        AdjustmentContext context = extensionAdjustment ? null : new AdjustmentContext();
        for (int i = 0; i < fExpressions.length; i++) {
            if (fExpressions[i].adjustConfiguration(cfg, context, extensionAdjustment))
                adjusted = true;
        }

        if (context != null) {
            String unadjusted[] = context.getUnadjusted();
            for (int i = 0; i < unadjusted.length; i++) {
                OptionEnablementExpression.adjustConfiguration(cfg, unadjusted[i], null, extensionAdjustment);
            }
        }

        return adjusted;
    }

    private Map<String, Set<String>> getReferencedProperties() {
        if (fRefPropsMap == null) {
            fRefPropsMap = new HashMap<>();

            for (int i = 0; i < fExpressions.length; i++) {
                fExpressions[i].getReferencedProperties(fRefPropsMap);
            }
        }
        return fRefPropsMap;
    }

    public boolean referesProperty(String id) {
        Map<String, Set<String>> map = getReferencedProperties();

        return map.containsKey(id);
    }

    public boolean referesPropertyValue(String propertyId, String valueId) {
        Map<String, Set<String>> map = getReferencedProperties();
        Set<String> set = map.get(propertyId);
        if (set != null)
            return set.contains(valueId);
        return false;
    }

    public String[] getReferencedPropertyIds() {
        Map<String, Set<String>> map = getReferencedProperties();
        return map.keySet().toArray(new String[map.size()]);
    }

    public String[] getReferencedValueIds(String propertyId) {
        Map<String, Set<String>> map = getReferencedProperties();
        Set<String> set = map.get(propertyId);
        return set.toArray(new String[set.size()]);
    }

    @Override
    public boolean isOptionCategoryVisible(IBuildObject configuration, IHoldsOptions optHolder,
            IOptionCategory category) {
        return evaluateCategory(rcInfoFromConfiguration(configuration), optHolder, category);
    }

    private boolean evaluateCategory(IResourceInfo rcInfo, IHoldsOptions holder, IOptionCategory category) {
        for (int i = 0; i < fExpressions.length; i++) {
            if (!fExpressions[i].evaluate(rcInfo, holder, category))
                return false;
        }
        return true;
    }

}
