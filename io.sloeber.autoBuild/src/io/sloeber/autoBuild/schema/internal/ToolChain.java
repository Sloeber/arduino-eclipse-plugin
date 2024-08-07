/*******************************************************************************
 * Copyright (c) 2004, 2020 Intel Corporation and others.
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
 * IBM Corporation
 *******************************************************************************/
package io.sloeber.autoBuild.schema.internal;

import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

import io.sloeber.autoBuild.api.IEnvironmentVariableProvider;
import io.sloeber.autoBuild.extensionPoint.IConfigurationBuildMacroSupplier;
import io.sloeber.autoBuild.schema.api.IOption;
import io.sloeber.autoBuild.schema.api.IOutputType;
import io.sloeber.autoBuild.schema.api.IProjectType;
import io.sloeber.autoBuild.schema.api.ITool;
import io.sloeber.autoBuild.schema.api.IToolChain;

public class ToolChain extends SchemaObject implements IToolChain {

    String[] modelIsAbstract;
    String[] modelOsList;
    String[] modelArchList;
    String[] modelErrorParsers;
    String[] modelLanguageSettingsProviders;
    String[] modelScannerConfigDiscoveryProfileID;
    String[] modelTargetTool;
    String[] modelSecondaryOutputs;
    String[] modelIsSupportedByOS;
    String[] modelEnvironmentSupplier;
    String[] modelBuildMacroSuplier;
    String[] modelIsSytem;

    private Map<String, Tool> myToolMap = new HashMap<>();
    Set<String> myErrorParsersIDs = new HashSet<>();
    // Managed Build model attributes
    private List<String> myArchList = new ArrayList<>();
    private IEnvironmentVariableProvider myEnvironmentVariableProvider = null;
    private IConfigurationBuildMacroSupplier myBuildMacroSupplier = null;

    private IProjectType myProjectType;
    private boolean myIsCompatibleWithLocalOS = false;

    /**
     * This constructor is called to create a tool-chain defined by an extension
     * point in a plugin manifest file, or returned by a dynamic element provider
     *
     */
    public ToolChain(IProjectType parent, IExtensionPoint root, IConfigurationElement element) {
        myProjectType = parent;
        loadNameAndID(root, element);
        modelIsAbstract = getAttributes(IS_ABSTRACT);
        modelOsList = getAttributes(OS_LIST);
        modelArchList = getAttributes(ARCH_LIST);
        modelErrorParsers = getAttributes(ERROR_PARSERS);
        modelLanguageSettingsProviders = getAttributes(LANGUAGE_SETTINGS_PROVIDERS);
        modelScannerConfigDiscoveryProfileID = getAttributes(SCANNER_CONFIG_PROFILE_ID);
        modelTargetTool = getAttributes(TARGET_TOOLS);
        modelSecondaryOutputs = getAttributes(SECONDARY_OUTPUTS);
        modelIsSupportedByOS = getAttributes(IS_TOOL_CHAIN_SUPPORTED);
        modelEnvironmentSupplier = getAttributes(CONFIGURATION_ENVIRONMENT_SUPPLIER);
        modelBuildMacroSuplier = getAttributes(CONFIGURATION_MACRO_SUPPLIER);
        modelIsSytem = getAttributes(IS_SYSTEM);


        List<IConfigurationElement> toolChainElements = getFirstChildren(ITool.TOOL_ELEMENT_NAME);
        for (IConfigurationElement toolChainElement : toolChainElements) {
            Tool toolChild = new Tool(this, root, toolChainElement);
            myToolMap.put(toolChild.myID, toolChild);
        }
        if (myToolMap.size() == 0) {
            System.err.println("There are no tools in toolchain " + myName + DOT); //$NON-NLS-1$
        }

        resolveFields();
    }

    private void resolveFields() {
        String osName = Platform.getOS();
        if (modelOsList[SUPER].isBlank() || ALL.equals(modelOsList[SUPER])) {
            myIsCompatibleWithLocalOS = true;
        } else {
            for (String token : modelOsList[SUPER].split(COMMA)) {
                if (osName.equals(token)) {
                    myIsCompatibleWithLocalOS = true;
                }
            }
        }
        if (!myIsCompatibleWithLocalOS) {
            System.err.println(myName + BLANK + myID + BLANK + modelOsList[SUPER]);
        }

        if (modelArchList[SUPER].isBlank()) {
            myArchList.add(ALL);
        } else {
            for (String token : modelArchList[SUPER].split(COMMA)) {
                myArchList.add(token.trim());
            }
        }

        myBuildMacroSupplier = (IConfigurationBuildMacroSupplier) createExecutableExtension(
                CONFIGURATION_MACRO_SUPPLIER);
        myEnvironmentVariableProvider = (IEnvironmentVariableProvider) createExecutableExtension(
                CONFIGURATION_ENVIRONMENT_SUPPLIER);

        //collect all the error parser ID's
        String localErrorIDs[] = modelErrorParsers[SUPER].split(Pattern.quote(SEMICOLON));
        myErrorParsersIDs.addAll(Arrays.asList(localErrorIDs));
        for (Tool curTool : myToolMap.values()) {
            String toolErrorIDs[] = curTool.getErrorParserList();
            myErrorParsersIDs.addAll(Arrays.asList(toolErrorIDs));
        }
        myErrorParsersIDs.remove(EMPTY_STRING);
    }

    @Override
    public IProjectType getParent() {
        return myProjectType;
    }


    @Override
    public List<ITool> getTools() {
        return new LinkedList<>(myToolMap.values());
    }

    @Override
    public ITool getTool(String toolID) {
        Tool tool = myToolMap.get(toolID);
        return tool;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public List<IOutputType> getSecondaryOutputs() {
        List<IOutputType> types = new LinkedList<>();
        String ids = modelSecondaryOutputs[SUPER];
        StringTokenizer tok = new StringTokenizer(ids, ";"); //$NON-NLS-1$
        List<ITool> tools = getTools();
        while (tok.hasMoreElements()) {
            String tokenID = tok.nextToken();
            for (ITool tool : tools) {
                IOutputType type = tool.getOutputTypeById(tokenID);
                if (type != null) {
                    types.add(type);
                    break;
                }
            }
        }
        return types;
    }

    @Override
    public Set<ITool> getTargetTools() {
        Set<ITool> ret = new HashSet<>();
        Set<String> toolIDs = new HashSet<>();
        toolIDs = Set.of(modelTargetTool[SUPER].split(SEMICOLON));
        for (Tool curTool : myToolMap.values()) {
            if (curTool.matchID(toolIDs)) {
                ret.add(curTool);
            }
        }
        return ret;
    }

    //
    @Override
    public Set<String> getErrorParserList() {
        return myErrorParsersIDs;
    }

    @Override
    public List<String> getArchList() {
        return new LinkedList<>(myArchList);
    }

    //    @Override
    //    public List<String> getOSList() {
    //        return new LinkedList<>(myOsList);
    //    }

    @Override
    public String getDefaultLanguageSettingsProviderIds() {
        return modelLanguageSettingsProviders[SUPER];
    }

    @Override
    public String getScannerConfigDiscoveryProfileId() {
        return modelScannerConfigDiscoveryProfileID[SUPER];
    }

    @Override
    public IEnvironmentVariableProvider getEnvironmentVariableProvider() {
        return myEnvironmentVariableProvider;
    }

    @Override
    public IConfigurationBuildMacroSupplier getBuildMacroSupplier() {
        return myBuildMacroSupplier;
    }

    public StringBuffer dump(int leadingChars) {
        StringBuffer ret = new StringBuffer();
        String prepend = DUMPLEAD.repeat(leadingChars);
        ret.append(prepend + TOOL_CHAIN_ELEMENT_NAME + NEWLINE);
        ret.append(prepend + NAME + EQUAL + myName + NEWLINE);
        ret.append(prepend + ID + EQUAL + myID + NEWLINE);
        ret.append(prepend + IS_ABSTRACT + EQUAL + modelIsAbstract[ORIGINAL] + NEWLINE);
        ret.append(prepend + OS_LIST + EQUAL + modelOsList[SUPER] + NEWLINE);
        ret.append(prepend + ARCH_LIST + EQUAL + modelArchList[SUPER] + NEWLINE);
        ret.append(prepend + ERROR_PARSERS + EQUAL + modelErrorParsers[SUPER] + NEWLINE);
        ret.append(prepend + LANGUAGE_SETTINGS_PROVIDERS + EQUAL + modelLanguageSettingsProviders[SUPER] + NEWLINE);

        ret.append(prepend + SCANNER_CONFIG_PROFILE_ID + EQUAL + modelScannerConfigDiscoveryProfileID[SUPER] + NEWLINE);
        ret.append(prepend + TARGET_TOOLS + EQUAL + modelTargetTool[SUPER] + NEWLINE);
        ret.append(prepend + SECONDARY_OUTPUTS + EQUAL + modelSecondaryOutputs[SUPER] + NEWLINE);
        ret.append(prepend + IS_TOOL_CHAIN_SUPPORTED + EQUAL + modelIsSupportedByOS[SUPER] + NEWLINE);
        ret.append(prepend + CONFIGURATION_ENVIRONMENT_SUPPLIER + EQUAL + modelEnvironmentSupplier[SUPER] + NEWLINE);
        ret.append(prepend + CONFIGURATION_MACRO_SUPPLIER + EQUAL + modelBuildMacroSuplier[SUPER] + NEWLINE);
        ret.append(prepend + IS_SYSTEM + EQUAL + modelIsSytem[SUPER] + NEWLINE);

        ret.append(prepend + BEGIN_OF_CHILDREN + ITool.TOOL_ELEMENT_NAME + NEWLINE);
        ret.append(prepend + "Number of tools " + String.valueOf(myToolMap.size())); //$NON-NLS-1$
        for (Tool curTool : myToolMap.values()) {
            ret.append(curTool.dump(leadingChars + 1));
        }
        ret.append(prepend + END_OF_CHILDREN + ITool.TOOL_ELEMENT_NAME + NEWLINE);

        return ret;
    }

	@Override
	public IOption getOption(String optionID) {
		for(Tool curTool:myToolMap.values()) {
			IOption option = curTool.getOption(optionID);
			if (option!=null){
				return option;
			}
		}
		return null;
	}

	@Override
	public ITool getToolFromOptionID(String optionID) {
		for(Tool curTool:myToolMap.values()) {
			IOption option = curTool.getOption(optionID);
			if (option!=null){
				return curTool;
			}
		}
		return null;
	}

}
