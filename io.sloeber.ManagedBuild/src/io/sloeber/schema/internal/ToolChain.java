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
package io.sloeber.schema.internal;

import static io.sloeber.autoBuild.integration.Const.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import io.sloeber.autoBuild.Internal.ManagedBuildManager;
import io.sloeber.autoBuild.Internal.PathInfoCache;
import io.sloeber.autoBuild.api.IEnvironmentVariableSupplier;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IConfigurationBuildMacroSupplier;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IFolderInfo;
import io.sloeber.schema.api.IOptions;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ITargetPlatform;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.api.IToolChain;

public class ToolChain extends Options implements IToolChain {

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
    private TargetPlatform myTargetPlatform = null;
    private Builder myBuilder;
    // Managed Build model attributes
    private List<String> myOsList = new ArrayList<>();
    private List<String> myArchList = new ArrayList<>();
    private IEnvironmentVariableSupplier myEnvironmentVariableSupplier = null;
    private IConfigurationBuildMacroSupplier myBuildMacroSupplier = null;

    private Configuration myConfiguration;
    private List<OptionCategory> myCategories = new ArrayList<>();

    /**
     * This constructor is called to create a tool-chain defined by an extension
     * point in a plugin manifest file, or returned by a dynamic element provider
     *
     * @param parentFldInfo
     *            The {@link IFolderInfo} parent of this
     *            tool-chain, or {@code null} if defined at the top
     *            level
     * @param element
     *            The tool-chain definition from the manifest file
     *            or a dynamic element provider
     * @param managedBuildRevision
     *            the fileVersion of Managed Build System
     */
    public ToolChain(Configuration parent, IExtensionPoint root, IConfigurationElement element) {
        this.myConfiguration = parent;
        loadNameAndID(root, element);
        modelIsAbstract = getAttributes(IS_ABSTRACT);
        modelOsList = getAttributes(OS_LIST);
        modelArchList = getAttributes(ARCH_LIST);
        modelErrorParsers = getAttributes(ERROR_PARSERS);
        modelLanguageSettingsProviders = getAttributes(LANGUAGE_SETTINGS_PROVIDERS);
        modelScannerConfigDiscoveryProfileID = getAttributes(SCANNER_CONFIG_PROFILE_ID);
        modelTargetTool = getAttributes(TARGET_TOOL);
        modelSecondaryOutputs = getAttributes(SECONDARY_OUTPUTS);
        modelIsSupportedByOS = getAttributes(IS_TOOL_CHAIN_SUPPORTED);
        modelEnvironmentSupplier = getAttributes(CONFIGURATION_ENVIRONMENT_SUPPLIER);
        modelBuildMacroSuplier = getAttributes(CONFIGURATION_MACRO_SUPPLIER);
        modelIsSytem = getAttributes(IS_SYSTEM);

        IConfigurationElement[] targetPlatforms = element.getChildren(ITargetPlatform.TARGET_PLATFORM_ELEMENT_NAME);
        if (targetPlatforms.length == 1) {
            myTargetPlatform = new TargetPlatform(this, root, targetPlatforms[0]);
        } else {
            System.err.println("Targetplatforms of toolchain " + parent.myID + DOT + parent.myName + BLANK + myID + DOT //$NON-NLS-1$
                    + myName + " has wrong cardinality " + String.valueOf(targetPlatforms.length) + DOT); //$NON-NLS-1$
        }

        // Load the Builder child
        List<IConfigurationElement> builders = getFirstChildren(IBuilder.BUILDER_ELEMENT_NAME);
        if (builders.size() == 1) {
            myBuilder = new Builder(this, root, builders.get(0));
        } else {
            System.err.println("builders of toolchain " + myName + " has wrong cardinality " //$NON-NLS-1$//$NON-NLS-2$
                    + String.valueOf(builders.size()) + DOT);
        }

        List<IConfigurationElement> toolChainElements = getAllChildren(ITool.TOOL_ELEMENT_NAME);
        for (IConfigurationElement toolChainElement : toolChainElements) {
            Tool toolChild = new Tool(this, root, toolChainElement);
            myToolMap.put(toolChild.myID, toolChild);
        }
        if (myToolMap.size() == 0) {
            System.err.println("There are no tools in toolchain " + myName + DOT); //$NON-NLS-1$
        }

        List<IConfigurationElement> optionElements = getAllChildren(IOptions.OPTION);
        for (IConfigurationElement optionElement : optionElements) {
            Option newOption = new Option(this, root, optionElement);
            myOptionMap.put(newOption.getName(), newOption);
        }

        List<IConfigurationElement> categoryElements = getAllChildren(IOptions.OPTION_CAT);
        for (IConfigurationElement categoryElement : categoryElements) {
            myCategories.add(new OptionCategory(this, root, categoryElement));
        }

        //        IConfigurationElement enablements[] = element.getChildren(OptionEnablementExpression.NAME);
        //        for (IConfigurationElement curEnablement : enablements) {
        //            myEnablements.add(new OptionEnablementExpression(curEnablement));
        //        }

        resolveFields();
    }

    private void resolveFields() {

        if (modelOsList[SUPER].isBlank()) {
            myOsList.add(ALL);
        } else {
            for (String token : modelOsList[SUPER].split(COMMA)) {
                myOsList.add(token.trim());
            }
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
        myEnvironmentVariableSupplier = (IEnvironmentVariableSupplier) createExecutableExtension(
                CONFIGURATION_ENVIRONMENT_SUPPLIER);

    }

    // 
    //  
    /*
     * E L E M E N T A T T R I B U T E R E A D E R S A N D W R I T E R S
     */

    /*
     * P A R E N T A N D C H I L D H A N D L I N G
     */

    @Override
    public IConfiguration getParent() {
        return myConfiguration;
    }

    @Override
    public ITargetPlatform getTargetPlatform() {
        return myTargetPlatform;
    }

    @Override
    public IBuilder getBuilder() {
        return myBuilder;
    }

    @Override
    public List<ITool> getTools() {
        return new LinkedList<>(getAllTools(false));
        //        ITool tools[] = getAllTools(false);
        //        if (!isExtensionToolChain) {
        //            for (int i = 0; i < tools.length; i++) {
        //                if (tools[i].isExtensionElement()) {
        //                    String subId = ManagedBuildManager.calculateChildId(tools[i].getId(), null);
        //                    tools[i] = createTool(tools[i], subId, tools[i].getName(), false);
        //                }
        //            }
        //        }

        //        return tools;
    }

    public List<Tool> getAllTools(boolean includeCurrentUnused) {
        return new LinkedList<>(myToolMap.values()); // TOFIX jaba maybe the stuff below will need to be in the resolve
                                                     // fields stuff
                                                     //		// Merge our tools with our superclass' tools
                                                     //		if (getSuperClass() != null) {
                                                     //			tools = ((ToolChain) getSuperClass()).getAllTools(false);
                                                     //		}
                                                     //		// Our tools take precedence
                                                     //		if (tools != null) {
                                                     //			for (Tool tool : getToolList()) {
                                                     //				int j = 0;
                                                     //				for (; j < tools.length; j++) {
                                                     //					ITool superTool = tool.getSuperClass();
                                                     //					if (superTool != null) {
                                                     //						superTool = null;// TOFIX JABA ManagedBuildManager.getExtensionTool(superTool);
                                                     //						if (superTool != null && superTool.getId().equals(tools[j].getId())) {
                                                     //							tools[j] = tool;
                                                     //							break;
                                                     //						}
                                                     //					}
                                                     //				}
                                                     //				// No Match? Insert it (may be re-ordered)
                                                     //				if (j == tools.length) {
                                                     //					Tool[] newTools = new Tool[tools.length + 1];
                                                     //					for (int k = 0; k < tools.length; k++) {
                                                     //						newTools[k] = tools[k];
                                                     //					}
                                                     //					newTools[j] = tool;
                                                     //					tools = newTools;
                                                     //				}
                                                     //			}
                                                     //		} else {
                                                     //			tools = new Tool[getToolList().size()];
                                                     //			int i = 0;
                                                     //			for (Tool tool : getToolList()) {
                                                     //				tools[i++] = tool;
                                                     //			}
                                                     //		}
                                                     //		if (includeCurrentUnused)
                                                     //			return tools;
                                                     //		return filterUsedTools(tools, true);
    }

    //	private Tool[] filterUsedTools(Tool tools[], boolean used) {
    //		return used ? tools : new Tool[0];
    //	}

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
    public String getErrorParserIds() {
        return modelErrorParsers[SUPER];
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
            if (toolIDs.contains(curTool.getId())) {
                ret.add(curTool);
            }
        }
        return ret;
    }

    @Override
    public String getErrorParserIds(IConfiguration config) {
        return modelErrorParsers[SUPER];
        // TOFIX JABA code below is wierd
        //        String ids = errorParserIds;
        //        if (ids == null) {
        //            // If I have a superClass, ask it
        //            if (getSuperClass() != null) {
        //                ids = getSuperClass().getErrorParserIds(config);
        //            }
        //        }
        //        if (ids == null) {
        //            // Collect the error parsers from my children
        //            if (builder != null) {
        //                ids = builder.getErrorParserIds();
        //            }
        //            ITool[] tools = config.getFilteredTools();
        //            for (int i = 0; i < tools.length; i++) {
        //                ITool tool = tools[i];
        //                String toolIds = tool.getErrorParserIds();
        //                if (toolIds != null && toolIds.length() > 0) {
        //                    if (ids != null) {
        //                        ids += ";"; //$NON-NLS-1$
        //                        ids += toolIds;
        //                    } else {
        //                        ids = toolIds;
        //                    }
        //                }
        //            }
        //        }
        //        return ids;
    }

    @Override
    public List<String> getErrorParserList() {
        String parserIDs = getErrorParserIds();
        List<String> errorParsers = new LinkedList<>();
        if (!parserIDs.isBlank()) {
            StringTokenizer tok = new StringTokenizer(parserIDs, ";"); //$NON-NLS-1$
            while (tok.hasMoreElements()) {
                errorParsers.add(tok.nextToken());
            }
        }
        return errorParsers;
    }

    @Override
    public List<String> getArchList() {
        return new LinkedList<>(myArchList);
    }

    @Override
    public List<String> getOSList() {
        return new LinkedList<>(myOsList);
    }

    @Override
    public String getDefaultLanguageSettingsProviderIds() {
        return null;
        //		if (defaultLanguageSettingsProviderIds == null && superClass instanceof IToolChain) {
        //			defaultLanguageSettingsProviderIds = ((IToolChain) superClass).getDefaultLanguageSettingsProviderIds();
        //		}
        //		return defaultLanguageSettingsProviderIds;
    }

    @Override
    public String getScannerConfigDiscoveryProfileId() {
        return modelScannerConfigDiscoveryProfileID[SUPER];
    }

    @Override
    public IEnvironmentVariableSupplier getEnvironmentVariableSupplier() {
        return myEnvironmentVariableSupplier;
    }

    @Override
    public IConfigurationBuildMacroSupplier getBuildMacroSupplier() {
        return myBuildMacroSupplier;
    }

    public StringBuffer dump(int leadingChars) {
        StringBuffer ret = new StringBuffer();
        String prepend = StringUtils.repeat(DUMPLEAD, leadingChars);
        ret.append(prepend + TOOL_CHAIN_ELEMENT_NAME + NEWLINE);
        ret.append(prepend + NAME + EQUAL + myName + NEWLINE);
        ret.append(prepend + ID + EQUAL + myID + NEWLINE);
        ret.append(prepend + IS_ABSTRACT + EQUAL + modelIsAbstract[ORIGINAL] + NEWLINE);
        ret.append(prepend + OS_LIST + EQUAL + modelOsList[SUPER] + NEWLINE);
        ret.append(prepend + ARCH_LIST + EQUAL + modelArchList[SUPER] + NEWLINE);
        ret.append(prepend + ERROR_PARSERS + EQUAL + modelErrorParsers[SUPER] + NEWLINE);
        ret.append(prepend + LANGUAGE_SETTINGS_PROVIDERS + EQUAL + modelLanguageSettingsProviders[SUPER] + NEWLINE);

        ret.append(prepend + SCANNER_CONFIG_PROFILE_ID + EQUAL + modelScannerConfigDiscoveryProfileID[SUPER] + NEWLINE);
        ret.append(prepend + TARGET_TOOL + EQUAL + modelTargetTool[SUPER] + NEWLINE);
        ret.append(prepend + SECONDARY_OUTPUTS + EQUAL + modelSecondaryOutputs[SUPER] + NEWLINE);
        ret.append(prepend + IS_TOOL_CHAIN_SUPPORTED + EQUAL + modelIsSupportedByOS[SUPER] + NEWLINE);
        ret.append(prepend + CONFIGURATION_ENVIRONMENT_SUPPLIER + EQUAL + modelEnvironmentSupplier[SUPER] + NEWLINE);
        ret.append(prepend + CONFIGURATION_MACRO_SUPPLIER + EQUAL + modelBuildMacroSuplier[SUPER] + NEWLINE);
        ret.append(prepend + IS_SYSTEM + EQUAL + modelIsSytem[SUPER] + NEWLINE);

        ret.append(prepend + BEGIN_OF_CHILDREN + ITool.TOOL_ELEMENT_NAME + NEWLINE);
        ret.append(prepend + "Number of tools " + String.valueOf(myToolMap.size()));
        leadingChars++;
        for (Tool curTool : myToolMap.values()) {
            ret.append(curTool.dump(leadingChars));
        }
        ret.append(prepend + END_OF_CHILDREN + ITool.TOOL_ELEMENT_NAME + NEWLINE);

        return ret;
    }

}

///**
//* Check if legacy scanner discovery profiles should be used.
//*/
//private boolean useLegacyScannerDiscoveryProfiles() {
// boolean useLegacy = true;
// if (getDefaultLanguageSettingsProviderIds() != null) {
//     IConfiguration cfg = getParent();
//     if (cfg != null && cfg.getDefaultLanguageSettingsProviderIds() != null) {
//         IResource rc = cfg.getOwner();
//         if (rc != null) {
//             IProject project = rc.getProject();
//             useLegacy = !ScannerDiscoveryLegacySupport.isLanguageSettingsProvidersFunctionalityEnabled(project);
//         }
//     }
// }
// return useLegacy;
//}

//  public boolean hasCustomSettings(ToolChain tCh) {
//      if (superClass == null)
//          return true;
//
//      IToolChain realTc = ManagedBuildManager.getRealToolChain(this);
//      IToolChain otherRealTc = ManagedBuildManager.getRealToolChain(tCh);
//      if (realTc != otherRealTc)
//          return true;
//
//      if (hasCustomSettings())
//          return true;
//
//      List<ITool> tools = getTools();
//      List<ITool> otherTools = tCh.getTools();
//      if (tools.size() != otherTools.size())
//          return true;
//
//      for (int i = 0; i < tools.size(); i++) {
//          Tool tool = (Tool) tools[i];
//          Tool otherTool = (Tool) otherTools[i];
//          if (tool.hasCustomSettings(otherTool))
//              return true;
//      }
//      return false;
//  }

//@Override
//public CTargetPlatformData getTargetPlatformData() {
//  return targetPlatform.getTargetPlatformData();
//}

//    public BooleanExpressionApplicabilityCalculator getBooleanExpressionCalculator() {
//        if (booleanExpressionCalculator == null) {
//            if (superClass != null) {
//                return ((ToolChain) superClass).getBooleanExpressionCalculator();
//            }
//        }
//        return booleanExpressionCalculator;
//    }

//    @Override
//    protected IResourceInfo getParentResourceInfo() {
//        //return getParentFolderInfo();
//        return null;
//    }

/**
 * //* Create a {@link ToolChain} based upon an existing tool chain.
 * //*
 * //* @param parentFldInfo
 * //* The {@link IConfiguration} the tool-chain will be added
 * //* to.
 * //* @param Id
 * //* ID of the new tool-chain
 * //* @param name
 * //* name of the new tool-chain
 * //* @param toolChain
 * //* The existing tool-chain to clone.
 * //
 */
//public ToolChain(IFolderInfo parentFldInfo, String Id, String name, Map<IPath, Map<String, String>> superIdMap,
//      ToolChain toolChain) {
//  // this.config = parentFldInfo.getParent();
//  // this.parentFolderInfo = parentFldInfo;
//  // setSuperClassInternal(toolChain.getSuperClass());
//  // if (getSuperClass() != null) {
//  // if (toolChain.superClassId != null) {
//  // superClassId = toolChain.superClassId;
//  // }
//  // }
//  // setId(Id);
//  // setName(name);
//  //
//  // // Set the managedBuildRevision and the version
//  // setManagedBuildRevision(toolChain.getManagedBuildRevision());
//  // setVersion(getVersionFromId());
//  //
//  // isExtensionToolChain = false;
//  //
//  // // Copy the remaining attributes
//  // if (toolChain.versionsSupported != null) {
//  // versionsSupported = toolChain.versionsSupported;
//  // }
//  // if (toolChain.convertToId != null) {
//  // convertToId = toolChain.convertToId;
//  // }
//  //
//  // if (toolChain.errorParserIds != null) {
//  // errorParserIds = toolChain.errorParserIds;
//  // }
//  // if (toolChain.osList != null) {
//  // osList = new ArrayList<>(toolChain.osList);
//  // }
//  // if (toolChain.archList != null) {
//  // archList = new ArrayList<>(toolChain.archList);
//  // }
//  // if (toolChain.targetToolIds != null) {
//  // targetToolIds = toolChain.targetToolIds;
//  // }
//  // if (toolChain.secondaryOutputIds != null) {
//  // secondaryOutputIds = toolChain.secondaryOutputIds;
//  // }
//  // if (toolChain.isAbstract != null) {
//  // isAbstract = toolChain.isAbstract;
//  // }
//  // if (toolChain.scannerConfigDiscoveryProfileId != null) {
//  // scannerConfigDiscoveryProfileId = toolChain.scannerConfigDiscoveryProfileId;
//  // }
//  //
//  // isRcTypeBasedDiscovery = toolChain.isRcTypeBasedDiscovery;
//  //
//  // supportsManagedBuild = toolChain.supportsManagedBuild;
//  //
//  // managedIsToolChainSupportedElement =
//  // toolChain.managedIsToolChainSupportedElement;
//  // managedIsToolChainSupported = toolChain.managedIsToolChainSupported;
//  //
//  // environmentVariableSupplierElement =
//  // toolChain.environmentVariableSupplierElement;
//  // environmentVariableSupplier = toolChain.environmentVariableSupplier;
//  //
//  // buildMacroSupplierElement = toolChain.buildMacroSupplierElement;
//  // buildMacroSupplier = toolChain.buildMacroSupplier;
//  //
//  // pathconverterElement = toolChain.pathconverterElement;
//  // optionPathConverter = toolChain.optionPathConverter;
//  //
//  // nonInternalBuilderId = toolChain.nonInternalBuilderId;
//  //
//  // discoveredInfo = toolChain.discoveredInfo;
//  //
//  // userDefinedMacros = toolChain.userDefinedMacros;
//  //
//  // // Clone the children in superclass
//  // boolean copyIds = toolChain.getId().equals(id);
//  // super.copyChildren(toolChain);
//  // // Clone the children
//  // if (toolChain.builder != null) {
//  // String subId;
//  // String subName;
//  //
//  // if (toolChain.builder.getSuperClass() != null) {
//  // subId = copyIds ? toolChain.builder.getId()
//  // :
//  // ManagedBuildManager.calculateChildId(toolChain.builder.getSuperClass().getId(),
//  // null);
//  // subName = toolChain.builder.getSuperClass().getName();
//  // } else {
//  // subId = copyIds ? toolChain.builder.getId()
//  // : ManagedBuildManager.calculateChildId(toolChain.builder.getId(), null);
//  // subName = toolChain.builder.getName();
//  // }
//  //
//  // builder = new Builder(this, subId, subName, toolChain.builder);
//  // }
//  // // if (toolChain.targetPlatform != null)
//  // {
//  // ITargetPlatform tpBase = toolChain.getTargetPlatform();
//  // if (tpBase != null) {
//  // ITargetPlatform extTp = tpBase;
//  // for (; extTp != null && !extTp.isExtensionElement(); extTp =
//  // extTp.getSuperClass()) {
//  // // empty body, the loop is to find extension element
//  // }
//  //
//  // String subId;
//  // if (copyIds) {
//  // subId = tpBase.getId();
//  // } else {
//  // subId = extTp != null ? ManagedBuildManager.calculateChildId(extTp.getId(),
//  // null)
//  // : ManagedBuildManager.calculateChildId(getId(), null);
//  // }
//  // String subName = tpBase.getName();
//  //
//  // // if (toolChain.targetPlatform.getSuperClass() != null) {
//  // // subId = toolChain.targetPlatform.getSuperClass().getId() + "." + nnn;
//  // //$NON-NLS-1$
//  // // subName = toolChain.targetPlatform.getSuperClass().getName();
//  // // } else {
//  // // subId = toolChain.targetPlatform.getId() + "." + nnn; //$NON-NLS-1$
//  // // subName = toolChain.targetPlatform.getName();
//  // // }
//  // targetPlatform = new TargetPlatform(this, subId, subName, (TargetPlatform)
//  // tpBase);
//  // }
//  // }
//  //
//  // IConfiguration cfg = parentFolderInfo.getParent();
//  // if (toolChain.toolList != null) {
//  // for (Tool toolChild : toolChain.getToolList()) {
//  // String subId = null;
//  // // String tmpId;
//  // String subName;
//  // // String version;
//  // ITool extTool = ManagedBuildManager.getExtensionTool(toolChild);
//  // Map<String, String> curIdMap = superIdMap.get(parentFldInfo.getPath());
//  // if (curIdMap != null) {
//  // if (extTool != null)
//  // subId = curIdMap.get(extTool.getId());
//  // }
//  //
//  // subName = toolChild.getName();
//  //
//  // if (subId == null) {
//  // if (extTool != null) {
//  // subId = copyIds ? toolChild.getId()
//  // : ManagedBuildManager.calculateChildId(extTool.getId(), null);
//  // // subName = toolChild.getSuperClass().getName();
//  // } else {
//  // subId = copyIds ? toolChild.getId()
//  // : ManagedBuildManager.calculateChildId(toolChild.getId(), null);
//  // // subName = toolChild.getName();
//  // }
//  // }
//  // // version = ManagedBuildManager.getVersionFromIdAndVersion(tmpId);
//  // // if ( version != null) { // If the 'tmpId' contains version information
//  // // subId = ManagedBuildManager.getIdFromIdAndVersion(tmpId) + "." + nnn + "_"
//  // + version; //$NON-NLS-1$ //$NON-NLS-2$
//  // // } else {
//  // // subId = tmpId + "." + nnn; //$NON-NLS-1$
//  // // }
//  //
//  // // The superclass for the cloned tool is not the same as the one from the
//  // tool being cloned.
//  // // The superclasses reside in different configurations.
//  // ITool toolSuperClass = null;
//  // String superId = null;
//  // // Search for the tool in this configuration that has the same
//  // grand-superClass as the
//  // // tool being cloned
//  // ITool otherSuperTool = toolChild.getSuperClass();
//  // if (otherSuperTool != null) {
//  // if (otherSuperTool.isExtensionElement()) {
//  // toolSuperClass = otherSuperTool;
//  // } else {
//  // IResourceInfo otherRcInfo = otherSuperTool.getParentResourceInfo();
//  // IResourceInfo thisRcInfo = cfg.getResourceInfo(otherRcInfo.getPath(), true);
//  // ITool otherExtTool = ManagedBuildManager.getExtensionTool(otherSuperTool);
//  // if (otherExtTool != null) {
//  // if (thisRcInfo != null) {
//  // ITool tools[] = thisRcInfo.getTools();
//  // for (int i = 0; i < tools.length; i++) {
//  // ITool thisExtTool = ManagedBuildManager.getExtensionTool(tools[i]);
//  // if (otherExtTool.equals(thisExtTool)) {
//  // toolSuperClass = tools[i];
//  // superId = toolSuperClass.getId();
//  // break;
//  // }
//  // }
//  // } else {
//  // superId = copyIds ? otherSuperTool.getId()
//  // : ManagedBuildManager.calculateChildId(otherExtTool.getId(), null);
//  // Map<String, String> idMap = superIdMap.get(otherRcInfo.getPath());
//  // if (idMap == null) {
//  // idMap = new HashMap<>();
//  // superIdMap.put(otherRcInfo.getPath(), idMap);
//  // }
//  // idMap.put(otherExtTool.getId(), superId);
//  // }
//  // }
//  // }
//  // }
//  // // Tool newTool = new Tool(this, (Tool)null, subId, subName, toolChild);
//  // // addTool(newTool);
//  //
//  // Tool newTool = null;
//  // if (toolSuperClass != null)
//  // newTool = new Tool(this, toolSuperClass, subId, subName, toolChild);
//  // else if (superId != null)
//  // newTool = new Tool(this, superId, subId, subName, toolChild);
//  // else {
//  // //TODO: Error
//  // }
//  // if (newTool != null)
//  // addTool(newTool);
//  //
//  // }
//  // }
//  //
//  // if (copyIds) {
//  // rebuildState = toolChain.rebuildState;
//  // isDirty = toolChain.isDirty;
//  // } else {
//  // setRebuildState(true);
//  // }
//}
//public void setPerRcTypeDiscovery(boolean on) {
//isRcTypeBasedDiscovery = Boolean.valueOf(on);
//}
//
//public PathInfoCache setDiscoveredPathInfo(PathInfoCache info) {
//PathInfoCache oldInfo = discoveredInfo;
//discoveredInfo = info;
//return oldInfo;
//}
//
///**
//* Initialize the tool-chain information from the XML element specified in the
//* argument
//*
//* @param element
//*            An XML element containing the tool-chain information
//*/
//protected void loadFromProject(ICStorageElement element) {
//  //
//  //        loadNameAndID(element);
//  //
//  //        // version
//  //        setVersion(getVersionFromId());
//  //
//  //        // superClass
//  //        superClassId = SafeStringInterner.safeIntern(element.getAttribute(IProjectType.SUPERCLASS));
//  //        if (superClassId != null && superClassId.length() > 0) {
//  //            setSuperClassInternal(null);//TOFIX JABA ManagedBuildManager.getExtensionToolChain(superClassId));
//  //            // Check for migration support
//  //            //       checkForMigrationSupport();
//  //        }
//  //
//  //        // isAbstract
//  //        if (element.getAttribute(IS_ABSTRACT) != null) {
//  //            String isAbs = element.getAttribute(IS_ABSTRACT);
//  //            if (isAbs != null) {
//  //                isAbstract = Boolean.parseBoolean(isAbs);
//  //            }
//  //        }
//  //
//  //        // Get the semicolon separated list of IDs of the error parsers
//  //        if (element.getAttribute(ERROR_PARSERS) != null) {
//  //            errorParserIds = SafeStringInterner.safeIntern(element.getAttribute(ERROR_PARSERS));
//  //        }
//  //
//  //        // Get the semicolon separated list of IDs of the secondary outputs
//  //        if (element.getAttribute(SECONDARY_OUTPUTS) != null) {
//  //            secondaryOutputIds = SafeStringInterner.safeIntern(element.getAttribute(SECONDARY_OUTPUTS));
//  //        }
//  //
//  //        // Get the target tool id
//  //        if (element.getAttribute(TARGET_TOOL) != null) {
//  //            targetToolIds = SafeStringInterner.safeIntern(element.getAttribute(TARGET_TOOL));
//  //        }
//  //
//  //        // Get the scanner config discovery profile id
//  //        if (element.getAttribute(SCANNER_CONFIG_PROFILE_ID) != null) {
//  //            scannerConfigDiscoveryProfileId = SafeStringInterner
//  //                    .safeIntern(element.getAttribute(SCANNER_CONFIG_PROFILE_ID));
//  //        }
//  //
//  //        // Get the 'versionSupported' attribute
//  //        if (element.getAttribute(VERSIONS_SUPPORTED) != null) {
//  //            versionsSupported = SafeStringInterner.safeIntern(element.getAttribute(VERSIONS_SUPPORTED));
//  //        }
//  //
//  //        // Get the 'convertToId' id
//  //        if (element.getAttribute(CONVERT_TO_ID) != null) {
//  //            convertToId = SafeStringInterner.safeIntern(element.getAttribute(CONVERT_TO_ID));
//  //        }
//  //
//  //        // Get the comma-separated list of valid OS
//  //        if (element.getAttribute(OS_LIST) != null) {
//  //            String os = element.getAttribute(OS_LIST);
//  //            if (os != null) {
//  //                osList = new ArrayList<>();
//  //                String[] osTokens = os.split(","); //$NON-NLS-1$
//  //                for (int i = 0; i < osTokens.length; ++i) {
//  //                    osList.add(SafeStringInterner.safeIntern(osTokens[i].trim()));
//  //                }
//  //            }
//  //        }
//  //
//  //        // Get the comma-separated list of valid Architectures
//  //        if (element.getAttribute(ARCH_LIST) != null) {
//  //            String arch = element.getAttribute(ARCH_LIST);
//  //            if (arch != null) {
//  //                archList = new ArrayList<>();
//  //                String[] archTokens = arch.split(","); //$NON-NLS-1$
//  //                for (int j = 0; j < archTokens.length; ++j) {
//  //                    archList.add(SafeStringInterner.safeIntern(archTokens[j].trim()));
//  //                }
//  //            }
//  //        }
//  //
//  //        // Note: optionPathConverter cannot be specified in a project file because
//  //        //       an IConfigurationElement is needed to load it!
//  //        if (pathconverterElement != null) {
//  //            //  TODO:  issue warning?
//  //        }
//  //
//  //        // Get the scanner config discovery profile id
//  //        scannerConfigDiscoveryProfileId = element.getAttribute(SCANNER_CONFIG_PROFILE_ID);
//  //        String tmp = element.getAttribute(RESOURCE_TYPE_BASED_DISCOVERY);
//  //        if (tmp != null)
//  //            isRcTypeBasedDiscovery = Boolean.valueOf(tmp);
//  //
//  //        nonInternalBuilderId = SafeStringInterner.safeIntern(element.getAttribute(NON_INTERNAL_BUILDER_ID));
//  //
//  //        //      String tmp = element.getAttribute(name)
//}
//
//void setTargetPlatform(TargetPlatform tp) {
//  targetPlatform = tp;
//}
//@Override
//public IFolderInfo getParentFolderInfo() {
//  return parentFolderInfo;
//}
//
//@Override
//public boolean supportsBuild(boolean managed) {
//
//  //        IBuilder builder = getBuilder();
//  //        if (builder != null && !builder.supportsBuild(managed))
//  //            return false;
//  //
//  //        List<ITool> tools = getTools();
//  //        for (ITool tool : tools) {
//  //            if (!tool.supportsBuild(managed))
//  //                return false;
//  //        }
//
//  return true;
//}
//  void resolveProjectReferences(boolean onLoad) {
//      for (Tool tool : getToolList()) {
//          tool.resolveProjectReferences(onLoad);
//      }
//  }

//@Override
//public String getUniqueRealName() {
//  if (myName == null) {
//      myName = getId();
//  } else {
//      String idVersion = ManagedBuildManager.getVersionFromIdAndVersion(getId());
//      if (idVersion != null) {
//          StringBuilder buf = new StringBuilder();
//          buf.append(myName);
//          buf.append(" (v").append(idVersion).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
//          myName = buf.toString();
//      }
//  }
//  return myName;
//}
//@Override
//public boolean isSupported() {
//  if (managedIsToolChainSupported == null) {
//      IConfigurationElement element = getIsToolChainSupportedElement();
//      if (element != null) {
//          try {
//              if (element.getAttribute(IS_TOOL_CHAIN_SUPPORTED) != null) {
//                  managedIsToolChainSupported = (IManagedIsToolChainSupported) element
//                          .createExecutableExtension(IS_TOOL_CHAIN_SUPPORTED);
//              }
//          } catch (CoreException e) {
//              Activator.log(e);
//          }
//      }
//  }
//
//  // if (managedIsToolChainSupported != null) {
//  // try {
//  // return managedIsToolChainSupported.isSupported(this, null, null);
//  // } catch (Throwable e) {
//  // Activator.log(new Status(IStatus.ERROR, Activator.getId(),
//  // "Exception in toolchain [" + getName() + "], id=" + getId(), e));
//  // //$NON-NLS-1$ //$NON-NLS-2$
//  // return false;
//  // }
//  // }
//  return true;
//}
///*
//* O B J E C T S T A T E M A I N T E N A N C E
//*/
//
//private IConfigurationElement getIsToolChainSupportedElement() {
// if (managedIsToolChainSupportedElement == null) {
//     if (superClass != null && superClass instanceof ToolChain) {
//         return ((ToolChain) superClass).getIsToolChainSupportedElement();
//     }
// }
// return managedIsToolChainSupportedElement;
//}

//public Set<String> contributeErrorParsers(FolderInfo info, Set<String> set, boolean includeChildren) {
//String parserIDs = getErrorParserIdsAttribute();
//if (parserIDs != null) {
//  if (set == null)
//      set = new HashSet<>();
//  if (parserIDs.length() != 0) {
//      StringTokenizer tok = new StringTokenizer(parserIDs, ";"); //$NON-NLS-1$
//      while (tok.hasMoreElements()) {
//          set.add(tok.nextToken());
//      }
//  }
//}
//
//if (includeChildren) {
//  //List<ITool> tools = info.getFilteredTools();
//  // set = info.contributeErrorParsers(tools, set);
//
//  //            if (info.isRoot()) {
//  //                Builder builder = (Builder) getBuilder();
//  //                set = builder.contributeErrorParsers(set);
//  //            }
//}
//return set;
//}

///**
//* Get list of scanner discovery profiles supported by previous version.
//* 
//* @see ScannerDiscoveryLegacySupport#getDeprecatedLegacyProfiles(String)
//*
//* @noreference This method is not intended to be referenced by clients.
//*/
//public String getLegacyScannerConfigDiscoveryProfileId() {
// if (modelScannerConfigDiscoveryProfileID[SUPER].isBlank()) {
//     String profileId = ScannerDiscoveryLegacySupport.getDeprecatedLegacyProfiles(myID);
//     if (profileId != null) {
//         return profileId;
//     }
// }
// return modelScannerConfigDiscoveryProfileID[SUPER];
//}

/**
 * //* This constructor is called to create a ToolChain whose attributes and
 * //* children will be added by separate calls.
 * //*
 * //* @param parentFldInfo
 * //* The parent of the tool chain, if any
 * //* @param superClass
 * //* The superClass, if any
 * //* @param Id
 * //* The ID for the new tool chain
 * //* @param name
 * //* The name for the new tool chain
 * //* @param isExtensionElement
 * //* Indicates whether this is an extension element or a
 * //* managed project element
 * //
 */
//public ToolChain(IFolderInfo parentFldInfo, IToolChain superClass, String Id, String name,
//      boolean isExtensionElement) {
//  // super(resolvedDefault);
//  // this.config = parentFldInfo.getParent();
//  // parentFolderInfo = parentFldInfo;
//  //
//  // setSuperClassInternal(superClass);
//  // setManagedBuildRevision(config.getManagedBuildRevision());
//  //
//  // if (getSuperClass() != null) {
//  // superClassId = getSuperClass().getId();
//  // }
//  // setId(Id);
//  // setName(name);
//  // setVersion(getVersionFromId());
//  //
//  // // isExtensionToolChain = isExtensionElement;
//  // // if (isExtensionElement) {
//  // // // Hook me up to the Managed Build Manager
//  // // ManagedBuildManager.addExtensionToolChain(this);
//  // // } else {
//  // // setRebuildState(true);
//  // // }
//}
//
///**
//* Create a {@link ToolChain} based on the specification stored in the project
//* file (.cproject).
//*
//* @param parentFldInfo
//*            The {@link IFolderInfo} the tool-chain will be
//*            added to.
//* @param element
//*            The XML element that contains the tool-chain
//*            settings.
//* @param managedBuildRevision
//*            the fileVersion of Managed Build System
//*/
//public ToolChain(IFolderInfo parentFldInfo, ICStorageElement element) {
//  // this.config = parentFldInfo.getParent();
//  // this.parentFolderInfo = parentFldInfo;
//  //
//  // this.isExtensionToolChain = false;
//  //
//  //
//  // // Initialize from the XML attributes
//  // loadFromProject(element);
//  //
//  // // Load children
//  // ICStorageElement configElements[] = element.getChildren();
//  // for (int i = 0; i < configElements.length; ++i) {
//  // ICStorageElement configElement = configElements[i];
//  // if (loadChild(configElement)) {
//  // // do nothing
//  // } else if (configElement.getName().equals(ITool.TOOL_ELEMENT_NAME)) {
//  // Tool tool = new Tool(this, configElement, managedBuildRevision);
//  // addTool(tool);
//  // } else if
//  // (configElement.getName().equals(ITargetPlatform.TARGET_PLATFORM_ELEMENT_NAME))
//  // {
//  // if (targetPlatform != null) {
//  // // TODO: report error
//  // }
//  // targetPlatform = new TargetPlatform(this, configElement,
//  // managedBuildRevision);
//  // } else if (configElement.getName().equals(IBuilder.BUILDER_ELEMENT_NAME)) {
//  // if (builder != null) {
//  // // TODO: report error
//  // }
//  // builder = new Builder(this, configElement, managedBuildRevision);
//  // }
//  // }
//  //
//  // String rebuild = PropertyManager.getInstance().getProperty(this,
//  // REBUILD_STATE);
//  // if (rebuild == null || Boolean.valueOf(rebuild).booleanValue())
//  // rebuildState = true;
//
//}
//@Override
//public boolean isSystemObject() {
//  return isTest;
//}
//
///**
//* @return the pathconverterElement
//*/
//public IConfigurationElement getPathconverterElement() {
//  return pathconverterElement;
//}
//    @Override
//    public IOptionPathConverter getOptionPathConverter() {
//        if (optionPathConverter != null) {
//            return optionPathConverter;
//        }
//        IConfigurationElement element = getPathconverterElement();
//        if (element != null) {
//            try {
//                if (element.getAttribute(ITool.OPTIONPATHCONVERTER) != null) {
//                    optionPathConverter = (IOptionPathConverter) element
//                            .createExecutableExtension(ITool.OPTIONPATHCONVERTER);
//                    return optionPathConverter;
//                }
//            } catch (CoreException e) {
//            }
//        }
//        return null;
//    }

// 
///**
// * Returns the plugin.xml element of the configurationEnvironmentSupplier
// * extension or <code>null</code> if none.
// *
// * @return IConfigurationElement
// */
//public IConfigurationElement getEnvironmentVariableSupplierElement() {
//    return ;
//}
//public String getNameAndVersion() {
//String idVersion = ManagedBuildManager.getVersionFromIdAndVersion(getId());
//if (idVersion != null && idVersion.length() != 0) {
//  return new StringBuilder().append(myName).append(" (").append(idVersion).append("").toString(); //$NON-NLS-1$ //$NON-NLS-2$
//}
//return myName;
//}
//public PathInfoCache getDiscoveredPathInfo() {
//return discoveredInfo;
//}
//
//public PathInfoCache clearDiscoveredPathInfo() {
//PathInfoCache oldInfo = discoveredInfo;
//discoveredInfo = null;
//return oldInfo;
//}

// /*
// * this method is called by the UserDefinedMacroSupplier to obtain
// user-defined
// * macros available for this tool-chain
// */
// public StorableMacros getUserDefinedMacros(){
// if(isExtensionToolChain)
// return null;
//
// if(userDefinedMacros == null)
// userDefinedMacros = new StorableMacros();
// return userDefinedMacros;
// }

// public StorableEnvironment getUserDefinedEnvironment(){
// if(isExtensionToolChain)
// return null;
//
// return userDefinedEnvironment;
// }

// public void setUserDefinedEnvironment(StorableEnvironment env){
// if(!isExtensionToolChain)
// userDefinedEnvironment = env;
// }
//public boolean hasScannerConfigSettings() {
//
//  if (getScannerConfigDiscoveryProfileId() != null)
//      return true;
//
//  return false;
//}