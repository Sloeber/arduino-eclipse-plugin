/*******************************************************************************
 * Copyright (c) 2005, 2012 Intel Corporation and others.
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;

import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IFolderInfo;
import io.sloeber.schema.api.IHoldsOptions;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.IResourceInfo;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.api.IToolChain;

public class OptionEnablementExpression extends AndExpression {
    public static final String NAME = "enablement"; //$NON-NLS-1$
    public static final String TYPE = "type"; //$NON-NLS-1$

    public static final String TYPE_UI_VISIBILITY = "UI_VISIBILITY"; //$NON-NLS-1$
    public static final String TYPE_UI_ENABLEMENT = "UI_ENABLEMENT"; //$NON-NLS-1$
    public static final String TYPE_CMD_USAGE = "CMD_USAGE"; //$NON-NLS-1$
    public static final String TYPE_CONTAINER_ATTRIBUTE = "CONTAINER_ATTRIBUTE"; //$NON-NLS-1$
    public static final String TYPE_ALL = "ALL"; //$NON-NLS-1$

    public static final String FLAG_DELIMITER = "|"; //$NON-NLS-1$

    public static final String ATTRIBUTE = "attribute"; //$NON-NLS-1$
    public static final String VALUE = "value"; //$NON-NLS-1$

    public static final String EXTENSION_ADJUSTMENT = "extensionAdjustment"; //$NON-NLS-1$

    public static final String YES = "yes"; //$NON-NLS-1$
    public static final String TRUE = "true"; //$NON-NLS-1$

    private static final String fSupportedOptionAttributes[] = { IOption.COMMAND, IOption.COMMAND_FALSE, IOption.VALUE,
            IOption.DEFAULT_VALUE, };

    private static final String fSupportedToolChainAttributes[] = { IToolChain.TARGET_TOOL, };

    private static final String fSupportedConfigurationAttributes[] = { IConfiguration.ARTIFACT_EXTENSION,
            IConfiguration.ARTIFACT_NAME, };

    public static final int FLAG_UI_VISIBILITY = 0x01;
    public static final int FLAG_UI_ENABLEMENT = 0x02;
    public static final int FLAG_CMD_USAGE = 0x04;
    public static final int FLAG_CONTAINER_ATTRIBUTE = 0x08;
    public static final int FLAG_ALL = ~0;

    private int fEnablementFlags;
    private String fAttribute;
    private String fValue;
    private boolean fIsExtensionAdjustment;

    public OptionEnablementExpression(IConfigurationElement element) {
        super(element);

        fEnablementFlags = calculateFlags(element.getAttribute(TYPE));

        fAttribute = element.getAttribute(ATTRIBUTE);
        fValue = element.getAttribute(VALUE);
        String tmp = element.getAttribute(EXTENSION_ADJUSTMENT);

        adjustAttributeSupport();

        if (tmp != null) {
            fIsExtensionAdjustment = getBooleanValue(tmp);
        } else {
            fIsExtensionAdjustment = checkFlags(FLAG_CONTAINER_ATTRIBUTE) ? false : true;
        }
    }

    private void adjustAttributeSupport() {
        boolean cleanAttrFlag = true;
        if (fAttribute != null && fValue != null) {
            for (int i = 0; i < fSupportedOptionAttributes.length; i++) {
                if (fAttribute.equals(fSupportedOptionAttributes[i])) {
                    cleanAttrFlag = false;
                    break;
                }
            }

            if (cleanAttrFlag) {
                for (int i = 0; i < fSupportedToolChainAttributes.length; i++) {
                    if (fAttribute.equals(fSupportedToolChainAttributes[i])) {
                        cleanAttrFlag = false;
                        break;
                    }
                }
            }

            if (cleanAttrFlag) {
                for (int i = 0; i < fSupportedConfigurationAttributes.length; i++) {
                    if (fAttribute.equals(fSupportedConfigurationAttributes[i])) {
                        cleanAttrFlag = false;
                        break;
                    }
                }
            }

        }

        if (cleanAttrFlag) {
            fEnablementFlags &= ~FLAG_CONTAINER_ATTRIBUTE;
            fAttribute = null;
            fValue = null;
        }
    }

    public String[] convertToList(String value, String delimiter) {
        List<String> list = new ArrayList<>();
        int delLength = delimiter.length();
        int valLength = value.length();

        if (delLength == 0) {
            list.add(value);
        } else {
            int start = 0;
            int stop;
            while (start < valLength) {
                stop = value.indexOf(delimiter, start);
                if (stop == -1)
                    stop = valLength;
                String subst = value.substring(start, stop);
                list.add(subst);
                start = stop + delLength;
            }
        }

        return list.toArray(new String[list.size()]);
    }

    protected int calculateFlags(String flagsString) {
        int flags = 0;

        if (flagsString != null) {
            String strings[] = convertToList(flagsString, FLAG_DELIMITER);

            for (int i = 0; i < strings.length; i++) {
                String str = strings[i].trim();
                if (TYPE_UI_VISIBILITY.equals(str))
                    flags |= FLAG_UI_VISIBILITY;
                else if (TYPE_UI_ENABLEMENT.equals(str))
                    flags |= FLAG_UI_ENABLEMENT;
                else if (TYPE_CMD_USAGE.equals(str))
                    flags |= FLAG_CMD_USAGE;
                else if (TYPE_CONTAINER_ATTRIBUTE.equals(str))
                    flags |= FLAG_CONTAINER_ATTRIBUTE;
                else if (TYPE_ALL.equals(str))
                    flags |= FLAG_ALL;
            }
        }

        if (flags == 0)
            flags = FLAG_ALL;
        return flags;
    }

    public boolean evaluate(IResourceInfo rcInfo, IHoldsOptions holder, IOption option, int flags) {
        return evaluate(rcInfo, holder, option, flags, (FLAG_CONTAINER_ATTRIBUTE & flags) == 0);
    }

    public boolean evaluate(IResourceInfo rcInfo, IHoldsOptions holder, IOption option, int flags, boolean bDefault) {
        return checkFlags(flags) ? evaluate(rcInfo, holder, option) : bDefault;
    }

    public boolean checkFlags(int flags) {
        return (fEnablementFlags & flags) == flags;
    }

    public int getFlags() {
        return fEnablementFlags;
    }
    
    public boolean adjustmentNeeded(IResourceInfo rcInfo, IHoldsOptions holder, IOption option) {
        return evaluate(rcInfo, holder, option, FLAG_CONTAINER_ATTRIBUTE);
    }


    public boolean canPerformAdjustment(boolean extensionAdjustment) {
        return fIsExtensionAdjustment == extensionAdjustment && checkFlags(FLAG_CONTAINER_ATTRIBUTE);
    }

    public static boolean getBooleanValue(String value) {
        if (TRUE.equalsIgnoreCase(value))
            return true;
        else if (YES.equalsIgnoreCase(value))
            return true;
        return false;
    }


    /*	public boolean evaluate(IBuildObject configuration,
            IHoldsOptions holder,
            IOption option) {
    		if(getChildren().length == 0)
    			return false;
    		return super.evaluate(configuration,holder,option);
    	}
    */
    /*	public boolean performAdjustment(IBuildObject configuration,
            IHoldsOptions holder,
            IOption option,
            boolean extensionAdjustment){
    		boolean adjusted = false;
    		if(canPerformAdjustment(extensionAdjustment)){
    			if(evaluate(configuration,holder,option,FLAG_CONTAINER_ATTRIBUTE)){
    				if(option != null){
    					adjusted = adjustOption(configuration, holder, option, extensionAdjustment);
    				} else if (holder != null){
    					if(holder instanceof ITool){
    						adjusted = adjustTool(configuration, (ITool)holder, extensionAdjustment);
    					} else if(holder instanceof IToolChain){
    						adjusted = adjustToolChain(configuration, (IToolChain)holder, extensionAdjustment);
    					}
    				} else if (configuration != null){
    					if(configuration instanceof IConfiguration){
    						adjusted = adjustConfiguration((IConfiguration)holder, extensionAdjustment);
    					}
    				}
    			}
    		}
    		return adjusted;
    	}
    */
    //    private static IOption getOptionToSet(IHoldsOptions holder, IOption option, String value,
    //            boolean extensionAdjustment) {
    //        IOption optionToSet = null;
    //        if (value != null) {
    //            try {
    //                optionToSet = holder.getOptionToSet(option, extensionAdjustment);
    //            } catch (BuildException e) {
    //            }
    //        } else {
    //            if (!extensionAdjustment && option.getOptionHolder() == holder) {
    //                optionToSet = option;
    //            }
    //        }
    //        return optionToSet;
    //    }

    //    public static boolean adjustOption(IResourceInfo rcInfo, IHoldsOptions holder, IOption option, String attribute,
    //            String value, boolean extensionAdjustment) {
    //
    //        if (value == null)
    //            return false;
    //
    //        IOption setOption = getOptionToSet(holder, option, value, extensionAdjustment);
    //        if (setOption == null)
    //            return false;
    //
    //        boolean adjusted = true;
    //        try {
    //            if (IOption.COMMAND.equals(attribute)) {
    //                //				IOption setOption = holder.getOptionToSet(option, extensionAdjustment);
    //                setOption.setCommand(value);
    //            } else if (IOption.COMMAND_FALSE.equals(attribute)) {
    //                //				IOption setOption = holder.getOptionToSet(option, extensionAdjustment);
    //                setOption.setCommandFalse(value);
    //            } else if (IOption.VALUE.equals(attribute)) {
    //                //				IOption setOption = holder.getOptionToSet(option, extensionAdjustment);
    //                switch (setOption.getValueType()) {
    //                case IOption.BOOLEAN:
    //                    Boolean bValue = value != null ? Boolean.valueOf(value) : null;
    //                    if (extensionAdjustment)
    //                        setOption.setValue(bValue);
    //                    else {
    //                        if (bValue == null) {
    //                            IOption superOption = setOption.getSuperClass();
    //                            if (superOption != null) {
    //                                bValue = Boolean.valueOf(superOption.getBooleanValue());
    //                            }
    //                        }
    //
    //                        if (bValue != null)
    //                            ManagedBuildManager.setOption(rcInfo, holder, setOption, bValue.booleanValue());
    //                        else
    //                            setOption.setValue((Object) null);
    //                    }
    //                    break;
    //                case IOption.ENUMERATED:
    //                case IOption.TREE:
    //                case IOption.STRING:
    //                    if (extensionAdjustment)
    //                        setOption.setValue(value);
    //                    else
    //                        ManagedBuildManager.setOption(rcInfo, holder, setOption, value);
    //                    break;
    //                case IOption.STRING_LIST:
    //                case IOption.INCLUDE_PATH:
    //                case IOption.PREPROCESSOR_SYMBOLS:
    //                case IOption.LIBRARIES:
    //                case IOption.OBJECTS:
    //                case IOption.INCLUDE_FILES:
    //                case IOption.LIBRARY_PATHS:
    //                case IOption.LIBRARY_FILES:
    //                case IOption.MACRO_FILES:
    //                case IOption.UNDEF_INCLUDE_PATH:
    //                case IOption.UNDEF_PREPROCESSOR_SYMBOLS:
    //                case IOption.UNDEF_INCLUDE_FILES:
    //                case IOption.UNDEF_LIBRARY_PATHS:
    //                case IOption.UNDEF_LIBRARY_FILES:
    //                case IOption.UNDEF_MACRO_FILES:
    //                    //TODO: add String list value support
    //                    adjusted = false;
    //                    break;
    //                }
    //            } else if (IOption.DEFAULT_VALUE.equals(attribute)) {
    //                switch (setOption.getValueType()) {
    //                case IOption.BOOLEAN:
    //                    Boolean bValue = value != null ? Boolean.valueOf(value) : null;
    //                    setOption.setDefaultValue(bValue);
    //                    break;
    //                case IOption.ENUMERATED:
    //                case IOption.STRING:
    //                    setOption.setDefaultValue(value);
    //                    break;
    //                case IOption.STRING_LIST:
    //                case IOption.INCLUDE_PATH:
    //                case IOption.PREPROCESSOR_SYMBOLS:
    //                case IOption.LIBRARIES:
    //                case IOption.OBJECTS:
    //                    //TODO: add String list value support
    //                    adjusted = false;
    //                    break;
    //                }
    //            } else
    //                adjusted = false;
    //        } catch (BuildException e) {
    //            adjusted = false;
    //        }
    //        return adjusted;
    //    }

    //    public boolean adjustOption(IResourceInfo rcInfo, IHoldsOptions holder, IOption option, AdjustmentContext context,
    //            boolean extensionAdjustment) {
    //        if (!canPerformAdjustment(extensionAdjustment))
    //            return false;
    //
    //        boolean needed = adjustmentNeeded(rcInfo, holder, option);
    //
    //        if (context != null)
    //            context.addAdjustedState(fAttribute, needed);
    //
    //        if (needed)
    //            return adjustOption(rcInfo, holder, option, fAttribute, fValue, extensionAdjustment);
    //        return false;
    //    }


}
