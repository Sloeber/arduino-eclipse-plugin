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
package io.sloeber.autoBuild.Internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;

import org.eclipse.cdt.core.language.settings.providers.ScannerDiscoveryLegacySupport;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.core.settings.model.extension.CTargetPlatformData;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.internal.core.SafeStringInterner;
import org.eclipse.cdt.internal.core.cdtvariables.StorableCdtVariables;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Version;

import io.sloeber.autoBuild.api.IBuildObject;
import io.sloeber.autoBuild.api.IBuilder;
import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.api.IEnvironmentVariableSupplier;
import io.sloeber.autoBuild.api.IFolderInfo;
import io.sloeber.autoBuild.api.IHoldsOptions;
import io.sloeber.autoBuild.api.IManagedProject;
import io.sloeber.autoBuild.api.IOptionPathConverter;
import io.sloeber.autoBuild.api.IOutputType;
import io.sloeber.autoBuild.api.IProjectType;
import io.sloeber.autoBuild.api.IResourceInfo;
import io.sloeber.autoBuild.api.ITargetPlatform;
import io.sloeber.autoBuild.api.ITool;
import io.sloeber.autoBuild.api.IToolChain;
import io.sloeber.autoBuild.extensionPoint.IConfigurationBuildMacroSupplier;
import io.sloeber.buildProperties.PropertyManager;

public class ToolChain extends HoldsOptions implements IToolChain {

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

	private List<Tool> toolList = new ArrayList<>();
	private Map<String, Tool> toolMap = new HashMap<>();
	private TargetPlatform targetPlatform;
	private Builder builder;
	// Managed Build model attributes
	private List<String> osList = new ArrayList<>();
	private List<String> archList = new ArrayList<>();
	private boolean isAbstract;
	private IConfigurationElement managedIsToolChainSupportedElement = null;
	private IManagedIsToolChainSupported managedIsToolChainSupported = null;
	private IConfigurationElement environmentVariableSupplierElement = null;
	private IEnvironmentVariableSupplier environmentVariableSupplier = null;
	private IConfigurationElement buildMacroSupplierElement = null;
	private IConfigurationBuildMacroSupplier buildMacroSupplier = null;
	private IConfigurationElement pathconverterElement = null;
	private IOptionPathConverter optionPathConverter = null;
	private boolean isTest;
	private SupportedProperties supportedProperties;

	private BooleanExpressionApplicabilityCalculator booleanExpressionCalculator;

//	private List<ToolChain> identicalList;

	private PathInfoCache discoveredInfo;
	private Boolean isRcTypeBasedDiscovery;

	private List<OptionEnablementExpression> myEnablements = new ArrayList<>();
	private IFolderInfo parent;
	private IFolderInfo parentFolderInfo = null;
	private List<OptionCategory> myCategories = new ArrayList<>();

	/**
	 * This constructor is called to create a tool-chain defined by an extension
	 * point in a plugin manifest file, or returned by a dynamic element provider
	 *
	 * @param parentFldInfo        The {@link IFolderInfo} parent of this
	 *                             tool-chain, or {@code null} if defined at the top
	 *                             level
	 * @param element              The tool-chain definition from the manifest file
	 *                             or a dynamic element provider
	 * @param managedBuildRevision the fileVersion of Managed Build System
	 */
	public ToolChain(IFolderInfo parentFldInfo, IExtensionPoint root, IConfigurationElement element) {
		this.parent = parentFldInfo;
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

		booleanExpressionCalculator = new BooleanExpressionApplicabilityCalculator(myEnablements);

		IConfigurationElement[] targetPlatforms = element.getChildren(ITargetPlatform.TARGET_PLATFORM_ELEMENT_NAME);
		if (targetPlatforms.length == 1) {
			targetPlatform = new TargetPlatform(this, root, targetPlatforms[0]);
		} else {
			System.err.println("Targetplatforms of toolchain " + name + " has wrong cardinality");
		}

		// Load the Builder child
		IConfigurationElement[] builders = element.getChildren(IBuilder.BUILDER_ELEMENT_NAME);
		if (builders.length == 1) {
			builder = new Builder(this, root, builders[0]);
		} else {
			System.err.println("builders of toolchain " + name + " has wrong cardinality");
		}

		IConfigurationElement[] supportedPropertiesElement = element
				.getChildren(SupportedProperties.SUPPORTED_PROPERTIES);
		if (supportedPropertiesElement.length == 1) {
			supportedProperties = new SupportedProperties(supportedPropertiesElement[0]);
		} else {
			System.err.println("supportedProperties of toolchain " + name + " has wrong cardinality");
		}

		IConfigurationElement[] toolChainElements = element.getChildren(ITool.TOOL_ELEMENT_NAME);
		for (IConfigurationElement toolChainElement : toolChainElements) {
			Tool toolChild = new Tool(this, root, toolChainElement);
			toolMap.put(toolChild.id, toolChild);
		}

		IConfigurationElement[] optionElements = element.getChildren(IHoldsOptions.OPTION);
		for (IConfigurationElement optionElement : optionElements) {
			Option newOption = new Option(this, root, optionElement);
			myOptionMap.put(newOption.getName(), newOption);
		}

		IConfigurationElement[] categoryElements = element.getChildren(IHoldsOptions.OPTION_CAT);
		for (IConfigurationElement categoryElement : categoryElements) {
			myCategories.add(new OptionCategory(this, root, categoryElement));
		}

		IConfigurationElement enablements[] = element.getChildren(OptionEnablementExpression.NAME);
		for (IConfigurationElement curEnablement : enablements) {
			myEnablements.add(new OptionEnablementExpression(curEnablement));
		}

		resolveFields();
	}

	private void resolveFields() {

		// Note no inheritanceof super class
		isAbstract = Boolean.parseBoolean(modelIsAbstract[ORIGINAL]);

		isTest = Boolean.valueOf(modelIsSytem[SUPER]).booleanValue();

		if (modelOsList[SUPER].isBlank()) {
			osList.add("all");
		} else {
			for (String token : modelOsList[SUPER].split(",")) {
				osList.add(token.trim());
			}
		}

		if (modelArchList[SUPER].isBlank()) {
			archList.add("all");
		} else {
			for (String token : modelArchList[SUPER].split(",")) {
				archList.add(token.trim());
			}
		}

	}

	/**
	 * This constructor is called to create a ToolChain whose attributes and
	 * children will be added by separate calls.
	 *
	 * @param parentFldInfo      The parent of the tool chain, if any
	 * @param superClass         The superClass, if any
	 * @param Id                 The ID for the new tool chain
	 * @param name               The name for the new tool chain
	 * @param isExtensionElement Indicates whether this is an extension element or a
	 *                           managed project element
	 */
	public ToolChain(IFolderInfo parentFldInfo, IToolChain superClass, String Id, String name,
			boolean isExtensionElement) {
		// super(resolvedDefault);
		// this.config = parentFldInfo.getParent();
		// parentFolderInfo = parentFldInfo;
		//
		// setSuperClassInternal(superClass);
		// setManagedBuildRevision(config.getManagedBuildRevision());
		//
		// if (getSuperClass() != null) {
		// superClassId = getSuperClass().getId();
		// }
		// setId(Id);
		// setName(name);
		// setVersion(getVersionFromId());
		//
		// // isExtensionToolChain = isExtensionElement;
		// // if (isExtensionElement) {
		// // // Hook me up to the Managed Build Manager
		// // ManagedBuildManager.addExtensionToolChain(this);
		// // } else {
		// // setRebuildState(true);
		// // }
	}

	/**
	 * Create a {@link ToolChain} based on the specification stored in the project
	 * file (.cproject).
	 *
	 * @param parentFldInfo        The {@link IFolderInfo} the tool-chain will be
	 *                             added to.
	 * @param element              The XML element that contains the tool-chain
	 *                             settings.
	 * @param managedBuildRevision the fileVersion of Managed Build System
	 */
	public ToolChain(IFolderInfo parentFldInfo, ICStorageElement element) {
		// this.config = parentFldInfo.getParent();
		// this.parentFolderInfo = parentFldInfo;
		//
		// this.isExtensionToolChain = false;
		//
		//
		// // Initialize from the XML attributes
		// loadFromProject(element);
		//
		// // Load children
		// ICStorageElement configElements[] = element.getChildren();
		// for (int i = 0; i < configElements.length; ++i) {
		// ICStorageElement configElement = configElements[i];
		// if (loadChild(configElement)) {
		// // do nothing
		// } else if (configElement.getName().equals(ITool.TOOL_ELEMENT_NAME)) {
		// Tool tool = new Tool(this, configElement, managedBuildRevision);
		// addTool(tool);
		// } else if
		// (configElement.getName().equals(ITargetPlatform.TARGET_PLATFORM_ELEMENT_NAME))
		// {
		// if (targetPlatform != null) {
		// // TODO: report error
		// }
		// targetPlatform = new TargetPlatform(this, configElement,
		// managedBuildRevision);
		// } else if (configElement.getName().equals(IBuilder.BUILDER_ELEMENT_NAME)) {
		// if (builder != null) {
		// // TODO: report error
		// }
		// builder = new Builder(this, configElement, managedBuildRevision);
		// }
		// }
		//
		// String rebuild = PropertyManager.getInstance().getProperty(this,
		// REBUILD_STATE);
		// if (rebuild == null || Boolean.valueOf(rebuild).booleanValue())
		// rebuildState = true;

	}

	/**
	 * Create a {@link ToolChain} based upon an existing tool chain.
	 *
	 * @param parentFldInfo The {@link IConfiguration} the tool-chain will be added
	 *                      to.
	 * @param Id            ID of the new tool-chain
	 * @param name          name of the new tool-chain
	 * @param toolChain     The existing tool-chain to clone.
	 */
	public ToolChain(IFolderInfo parentFldInfo, String Id, String name, Map<IPath, Map<String, String>> superIdMap,
			ToolChain toolChain) {
		// this.config = parentFldInfo.getParent();
		// this.parentFolderInfo = parentFldInfo;
		// setSuperClassInternal(toolChain.getSuperClass());
		// if (getSuperClass() != null) {
		// if (toolChain.superClassId != null) {
		// superClassId = toolChain.superClassId;
		// }
		// }
		// setId(Id);
		// setName(name);
		//
		// // Set the managedBuildRevision and the version
		// setManagedBuildRevision(toolChain.getManagedBuildRevision());
		// setVersion(getVersionFromId());
		//
		// isExtensionToolChain = false;
		//
		// // Copy the remaining attributes
		// if (toolChain.versionsSupported != null) {
		// versionsSupported = toolChain.versionsSupported;
		// }
		// if (toolChain.convertToId != null) {
		// convertToId = toolChain.convertToId;
		// }
		//
		// if (toolChain.errorParserIds != null) {
		// errorParserIds = toolChain.errorParserIds;
		// }
		// if (toolChain.osList != null) {
		// osList = new ArrayList<>(toolChain.osList);
		// }
		// if (toolChain.archList != null) {
		// archList = new ArrayList<>(toolChain.archList);
		// }
		// if (toolChain.targetToolIds != null) {
		// targetToolIds = toolChain.targetToolIds;
		// }
		// if (toolChain.secondaryOutputIds != null) {
		// secondaryOutputIds = toolChain.secondaryOutputIds;
		// }
		// if (toolChain.isAbstract != null) {
		// isAbstract = toolChain.isAbstract;
		// }
		// if (toolChain.scannerConfigDiscoveryProfileId != null) {
		// scannerConfigDiscoveryProfileId = toolChain.scannerConfigDiscoveryProfileId;
		// }
		//
		// isRcTypeBasedDiscovery = toolChain.isRcTypeBasedDiscovery;
		//
		// supportsManagedBuild = toolChain.supportsManagedBuild;
		//
		// managedIsToolChainSupportedElement =
		// toolChain.managedIsToolChainSupportedElement;
		// managedIsToolChainSupported = toolChain.managedIsToolChainSupported;
		//
		// environmentVariableSupplierElement =
		// toolChain.environmentVariableSupplierElement;
		// environmentVariableSupplier = toolChain.environmentVariableSupplier;
		//
		// buildMacroSupplierElement = toolChain.buildMacroSupplierElement;
		// buildMacroSupplier = toolChain.buildMacroSupplier;
		//
		// pathconverterElement = toolChain.pathconverterElement;
		// optionPathConverter = toolChain.optionPathConverter;
		//
		// nonInternalBuilderId = toolChain.nonInternalBuilderId;
		//
		// discoveredInfo = toolChain.discoveredInfo;
		//
		// userDefinedMacros = toolChain.userDefinedMacros;
		//
		// // Clone the children in superclass
		// boolean copyIds = toolChain.getId().equals(id);
		// super.copyChildren(toolChain);
		// // Clone the children
		// if (toolChain.builder != null) {
		// String subId;
		// String subName;
		//
		// if (toolChain.builder.getSuperClass() != null) {
		// subId = copyIds ? toolChain.builder.getId()
		// :
		// ManagedBuildManager.calculateChildId(toolChain.builder.getSuperClass().getId(),
		// null);
		// subName = toolChain.builder.getSuperClass().getName();
		// } else {
		// subId = copyIds ? toolChain.builder.getId()
		// : ManagedBuildManager.calculateChildId(toolChain.builder.getId(), null);
		// subName = toolChain.builder.getName();
		// }
		//
		// builder = new Builder(this, subId, subName, toolChain.builder);
		// }
		// // if (toolChain.targetPlatform != null)
		// {
		// ITargetPlatform tpBase = toolChain.getTargetPlatform();
		// if (tpBase != null) {
		// ITargetPlatform extTp = tpBase;
		// for (; extTp != null && !extTp.isExtensionElement(); extTp =
		// extTp.getSuperClass()) {
		// // empty body, the loop is to find extension element
		// }
		//
		// String subId;
		// if (copyIds) {
		// subId = tpBase.getId();
		// } else {
		// subId = extTp != null ? ManagedBuildManager.calculateChildId(extTp.getId(),
		// null)
		// : ManagedBuildManager.calculateChildId(getId(), null);
		// }
		// String subName = tpBase.getName();
		//
		// // if (toolChain.targetPlatform.getSuperClass() != null) {
		// // subId = toolChain.targetPlatform.getSuperClass().getId() + "." + nnn;
		// //$NON-NLS-1$
		// // subName = toolChain.targetPlatform.getSuperClass().getName();
		// // } else {
		// // subId = toolChain.targetPlatform.getId() + "." + nnn; //$NON-NLS-1$
		// // subName = toolChain.targetPlatform.getName();
		// // }
		// targetPlatform = new TargetPlatform(this, subId, subName, (TargetPlatform)
		// tpBase);
		// }
		// }
		//
		// IConfiguration cfg = parentFolderInfo.getParent();
		// if (toolChain.toolList != null) {
		// for (Tool toolChild : toolChain.getToolList()) {
		// String subId = null;
		// // String tmpId;
		// String subName;
		// // String version;
		// ITool extTool = ManagedBuildManager.getExtensionTool(toolChild);
		// Map<String, String> curIdMap = superIdMap.get(parentFldInfo.getPath());
		// if (curIdMap != null) {
		// if (extTool != null)
		// subId = curIdMap.get(extTool.getId());
		// }
		//
		// subName = toolChild.getName();
		//
		// if (subId == null) {
		// if (extTool != null) {
		// subId = copyIds ? toolChild.getId()
		// : ManagedBuildManager.calculateChildId(extTool.getId(), null);
		// // subName = toolChild.getSuperClass().getName();
		// } else {
		// subId = copyIds ? toolChild.getId()
		// : ManagedBuildManager.calculateChildId(toolChild.getId(), null);
		// // subName = toolChild.getName();
		// }
		// }
		// // version = ManagedBuildManager.getVersionFromIdAndVersion(tmpId);
		// // if ( version != null) { // If the 'tmpId' contains version information
		// // subId = ManagedBuildManager.getIdFromIdAndVersion(tmpId) + "." + nnn + "_"
		// + version; //$NON-NLS-1$ //$NON-NLS-2$
		// // } else {
		// // subId = tmpId + "." + nnn; //$NON-NLS-1$
		// // }
		//
		// // The superclass for the cloned tool is not the same as the one from the
		// tool being cloned.
		// // The superclasses reside in different configurations.
		// ITool toolSuperClass = null;
		// String superId = null;
		// // Search for the tool in this configuration that has the same
		// grand-superClass as the
		// // tool being cloned
		// ITool otherSuperTool = toolChild.getSuperClass();
		// if (otherSuperTool != null) {
		// if (otherSuperTool.isExtensionElement()) {
		// toolSuperClass = otherSuperTool;
		// } else {
		// IResourceInfo otherRcInfo = otherSuperTool.getParentResourceInfo();
		// IResourceInfo thisRcInfo = cfg.getResourceInfo(otherRcInfo.getPath(), true);
		// ITool otherExtTool = ManagedBuildManager.getExtensionTool(otherSuperTool);
		// if (otherExtTool != null) {
		// if (thisRcInfo != null) {
		// ITool tools[] = thisRcInfo.getTools();
		// for (int i = 0; i < tools.length; i++) {
		// ITool thisExtTool = ManagedBuildManager.getExtensionTool(tools[i]);
		// if (otherExtTool.equals(thisExtTool)) {
		// toolSuperClass = tools[i];
		// superId = toolSuperClass.getId();
		// break;
		// }
		// }
		// } else {
		// superId = copyIds ? otherSuperTool.getId()
		// : ManagedBuildManager.calculateChildId(otherExtTool.getId(), null);
		// Map<String, String> idMap = superIdMap.get(otherRcInfo.getPath());
		// if (idMap == null) {
		// idMap = new HashMap<>();
		// superIdMap.put(otherRcInfo.getPath(), idMap);
		// }
		// idMap.put(otherExtTool.getId(), superId);
		// }
		// }
		// }
		// }
		// // Tool newTool = new Tool(this, (Tool)null, subId, subName, toolChild);
		// // addTool(newTool);
		//
		// Tool newTool = null;
		// if (toolSuperClass != null)
		// newTool = new Tool(this, toolSuperClass, subId, subName, toolChild);
		// else if (superId != null)
		// newTool = new Tool(this, superId, subId, subName, toolChild);
		// else {
		// //TODO: Error
		// }
		// if (newTool != null)
		// addTool(newTool);
		//
		// }
		// }
		//
		// if (copyIds) {
		// rebuildState = toolChain.rebuildState;
		// isDirty = toolChain.isDirty;
		// } else {
		// setRebuildState(true);
		// }
	}

	/*
	 * E L E M E N T A T T R I B U T E R E A D E R S A N D W R I T E R S
	 */

	/**
	 * Initialize the tool-chain information from the XML element specified in the
	 * argument
	 *
	 * @param element An XML element containing the tool-chain information
	 */
	protected void loadFromProject(ICStorageElement element) {
//
//        loadNameAndID(element);
//
//        // version
//        setVersion(getVersionFromId());
//
//        // superClass
//        superClassId = SafeStringInterner.safeIntern(element.getAttribute(IProjectType.SUPERCLASS));
//        if (superClassId != null && superClassId.length() > 0) {
//            setSuperClassInternal(null);//TOFIX JABA ManagedBuildManager.getExtensionToolChain(superClassId));
//            // Check for migration support
//            //       checkForMigrationSupport();
//        }
//
//        // isAbstract
//        if (element.getAttribute(IS_ABSTRACT) != null) {
//            String isAbs = element.getAttribute(IS_ABSTRACT);
//            if (isAbs != null) {
//                isAbstract = Boolean.parseBoolean(isAbs);
//            }
//        }
//
//        // Get the semicolon separated list of IDs of the error parsers
//        if (element.getAttribute(ERROR_PARSERS) != null) {
//            errorParserIds = SafeStringInterner.safeIntern(element.getAttribute(ERROR_PARSERS));
//        }
//
//        // Get the semicolon separated list of IDs of the secondary outputs
//        if (element.getAttribute(SECONDARY_OUTPUTS) != null) {
//            secondaryOutputIds = SafeStringInterner.safeIntern(element.getAttribute(SECONDARY_OUTPUTS));
//        }
//
//        // Get the target tool id
//        if (element.getAttribute(TARGET_TOOL) != null) {
//            targetToolIds = SafeStringInterner.safeIntern(element.getAttribute(TARGET_TOOL));
//        }
//
//        // Get the scanner config discovery profile id
//        if (element.getAttribute(SCANNER_CONFIG_PROFILE_ID) != null) {
//            scannerConfigDiscoveryProfileId = SafeStringInterner
//                    .safeIntern(element.getAttribute(SCANNER_CONFIG_PROFILE_ID));
//        }
//
//        // Get the 'versionSupported' attribute
//        if (element.getAttribute(VERSIONS_SUPPORTED) != null) {
//            versionsSupported = SafeStringInterner.safeIntern(element.getAttribute(VERSIONS_SUPPORTED));
//        }
//
//        // Get the 'convertToId' id
//        if (element.getAttribute(CONVERT_TO_ID) != null) {
//            convertToId = SafeStringInterner.safeIntern(element.getAttribute(CONVERT_TO_ID));
//        }
//
//        // Get the comma-separated list of valid OS
//        if (element.getAttribute(OS_LIST) != null) {
//            String os = element.getAttribute(OS_LIST);
//            if (os != null) {
//                osList = new ArrayList<>();
//                String[] osTokens = os.split(","); //$NON-NLS-1$
//                for (int i = 0; i < osTokens.length; ++i) {
//                    osList.add(SafeStringInterner.safeIntern(osTokens[i].trim()));
//                }
//            }
//        }
//
//        // Get the comma-separated list of valid Architectures
//        if (element.getAttribute(ARCH_LIST) != null) {
//            String arch = element.getAttribute(ARCH_LIST);
//            if (arch != null) {
//                archList = new ArrayList<>();
//                String[] archTokens = arch.split(","); //$NON-NLS-1$
//                for (int j = 0; j < archTokens.length; ++j) {
//                    archList.add(SafeStringInterner.safeIntern(archTokens[j].trim()));
//                }
//            }
//        }
//
//        // Note: optionPathConverter cannot be specified in a project file because
//        //       an IConfigurationElement is needed to load it!
//        if (pathconverterElement != null) {
//            //  TODO:  issue warning?
//        }
//
//        // Get the scanner config discovery profile id
//        scannerConfigDiscoveryProfileId = element.getAttribute(SCANNER_CONFIG_PROFILE_ID);
//        String tmp = element.getAttribute(RESOURCE_TYPE_BASED_DISCOVERY);
//        if (tmp != null)
//            isRcTypeBasedDiscovery = Boolean.valueOf(tmp);
//
//        nonInternalBuilderId = SafeStringInterner.safeIntern(element.getAttribute(NON_INTERNAL_BUILDER_ID));
//
//        //		String tmp = element.getAttribute(name)
	}

	/*
	 * P A R E N T A N D C H I L D H A N D L I N G
	 */

	@Override
	public IConfiguration getParent() {
		return null;//parent.getParent();
	}

	@Override
	public ITargetPlatform getTargetPlatform() {
		return targetPlatform;
	}

	@Override
	public IBuilder createBuilder(IBuilder superClass, String id, String name, boolean isExtensionElement) {
		builder = new Builder(this, superClass, id, name, isExtensionElement);
		return builder;
	}

	public void setBuilder(Builder builder) {
		this.builder = builder;
	}

	@Override
	public IBuilder getBuilder() {
		return builder;
	}

	@Override
	public List<ITool> getTools() {
		return new LinkedList<ITool>(getAllTools(false));
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
		return new LinkedList<>(toolMap.values()); // TOFIX jaba maybe the stuff below will need to be in the resolve
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

	private Tool[] filterUsedTools(Tool tools[], boolean used) {
		return used ? tools : new Tool[0];
	}

	@Override
	public ITool getTool(String id) {
		Tool tool = getToolMap().get(id);
		return tool;
	}

	/**
	 * Safe accessor for the list of tools.
	 *
	 * @return List containing the tools
	 */
	public List<Tool> getToolList() {
		return toolList;
	}

	/**
	 * Safe accessor for the map of tool ids to tools
	 */
	private Map<String, Tool> getToolMap() {
		return toolMap;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isAbstract() {
		return isAbstract;
	}

	@Override
	public String getErrorParserIds() {
		return modelErrorParsers[SUPER];
	}

	public String getErrorParserIdsAttribute() {
		return modelErrorParsers[SUPER];
		// TOFIX code below is wierd
//        String ids = errorParserIds;
//        if (ids == null) {
//            // If I have a superClass, ask it
//            if (getSuperClass() != null) {
//                ids = ((ToolChain) getSuperClass()).getErrorParserIdsAttribute();
//            }
//        }
//        return ids;
	}

	@Override
	public List<IOutputType> getSecondaryOutputs() {
		List<IOutputType> types = new LinkedList<>();
		String ids = modelSecondaryOutputs[SUPER];
		StringTokenizer tok = new StringTokenizer(ids, ";"); //$NON-NLS-1$
		List<ITool> tools = getTools();
		int i = 0;
		while (tok.hasMoreElements()) {
			String id = tok.nextToken();
			for (ITool tool : tools) {
				IOutputType type = tool.getOutputTypeById(id);
				if (type != null) {
					types.add(type);
					break;
				}
			}
		}
		return types;
	}

	@Override
	public String getTargetToolIds() {
		return modelTargetTool[SUPER];
	}

	@Override
	public String[] getTargetToolList() {
		String IDs = getTargetToolIds();
		String[] targetTools;
		if (IDs != null) {
			// Check for an empty string
			if (IDs.length() == 0) {
				targetTools = new String[0];
			} else {
				StringTokenizer tok = new StringTokenizer(IDs, ";"); //$NON-NLS-1$
				List<String> list = new ArrayList<>(tok.countTokens());
				while (tok.hasMoreElements()) {
					list.add(tok.nextToken());
				}
				String[] strArr = { "" }; //$NON-NLS-1$
				targetTools = list.toArray(strArr);
			}
		} else {
			targetTools = new String[0];
		}
		return targetTools;
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
		;
		if (!parserIDs.isBlank()) {
			StringTokenizer tok = new StringTokenizer(parserIDs, ";"); //$NON-NLS-1$
			while (tok.hasMoreElements()) {
				errorParsers.add(tok.nextToken());
			}
		}
		return errorParsers;
	}

	public Set<String> contributeErrorParsers(FolderInfo info, Set<String> set, boolean includeChildren) {
		String parserIDs = getErrorParserIdsAttribute();
		if (parserIDs != null) {
			if (set == null)
				set = new HashSet<>();
			if (parserIDs.length() != 0) {
				StringTokenizer tok = new StringTokenizer(parserIDs, ";"); //$NON-NLS-1$
				while (tok.hasMoreElements()) {
					set.add(tok.nextToken());
				}
			}
		}

		if (includeChildren) {
			//List<ITool> tools = info.getFilteredTools();
			// set = info.contributeErrorParsers(tools, set);

//			if (info.isRoot()) {
//				Builder builder = (Builder) getBuilder();
//				set = builder.contributeErrorParsers(set);
//			}
		}
		return set;
	}

	@Override
	public List<String> getArchList() {
		return new LinkedList<>(archList);
	}

	@Override
	public List<String> getOSList() {
		return new LinkedList<>(osList);
	}

	@Override
	public String getDefaultLanguageSettingsProviderIds() {
		return null;
//		if (defaultLanguageSettingsProviderIds == null && superClass instanceof IToolChain) {
//			defaultLanguageSettingsProviderIds = ((IToolChain) superClass).getDefaultLanguageSettingsProviderIds();
//		}
//		return defaultLanguageSettingsProviderIds;
	}

	/**
	 * Check if legacy scanner discovery profiles should be used.
	 */
	private boolean useLegacyScannerDiscoveryProfiles() {
		boolean useLegacy = true;
		if (getDefaultLanguageSettingsProviderIds() != null) {
			IConfiguration cfg = getParent();
			if (cfg != null && cfg.getDefaultLanguageSettingsProviderIds() != null) {
				IResource rc = cfg.getOwner();
				if (rc != null) {
					IProject project = rc.getProject();
					useLegacy = !ScannerDiscoveryLegacySupport.isLanguageSettingsProvidersFunctionalityEnabled(project);
				}
			}
		}
		return useLegacy;
	}

	/**
	 * Get list of scanner discovery profiles supported by previous version.
	 * 
	 * @see ScannerDiscoveryLegacySupport#getDeprecatedLegacyProfiles(String)
	 *
	 * @noreference This method is not intended to be referenced by clients.
	 */
	public String getLegacyScannerConfigDiscoveryProfileId() {
		if (modelScannerConfigDiscoveryProfileID[SUPER].isBlank()) {
			String profileId = ScannerDiscoveryLegacySupport.getDeprecatedLegacyProfiles(id);
			if (profileId != null) {
				return profileId;
			}
		}
		return modelScannerConfigDiscoveryProfileID[SUPER];
	}

	@Override
	public String getScannerConfigDiscoveryProfileId() {
		String discoveryProfileId = getScannerConfigDiscoveryProfileIdInternal();
		if (discoveryProfileId == null && useLegacyScannerDiscoveryProfiles()) {
			discoveryProfileId = getLegacyScannerConfigDiscoveryProfileId();
		}

		return discoveryProfileId;
	}

	/**
	 * Do not inline! This method needs to call itself recursively.
	 */
	private String getScannerConfigDiscoveryProfileIdInternal() {
		return modelScannerConfigDiscoveryProfileID[SUPER];
	}

	/**
	 * @return the pathconverterElement
	 */
	public IConfigurationElement getPathconverterElement() {
		return pathconverterElement;
	}

	@Override
	public IOptionPathConverter getOptionPathConverter() {
		if (optionPathConverter != null) {
			return optionPathConverter;
		}
		IConfigurationElement element = getPathconverterElement();
		if (element != null) {
			try {
				if (element.getAttribute(ITool.OPTIONPATHCONVERTER) != null) {
					optionPathConverter = (IOptionPathConverter) element
							.createExecutableExtension(ITool.OPTIONPATHCONVERTER);
					return optionPathConverter;
				}
			} catch (CoreException e) {
			}
		}
		return null;
	}

	/*
	 * O B J E C T S T A T E M A I N T E N A N C E
	 */



	private IConfigurationElement getIsToolChainSupportedElement() {
		if (managedIsToolChainSupportedElement == null) {
			if (superClass != null && superClass instanceof ToolChain) {
				return ((ToolChain) superClass).getIsToolChainSupportedElement();
			}
		}
		return managedIsToolChainSupportedElement;
	}

	@Override
	public boolean isSupported() {
		if (managedIsToolChainSupported == null) {
			IConfigurationElement element = getIsToolChainSupportedElement();
			if (element != null) {
				try {
					if (element.getAttribute(IS_TOOL_CHAIN_SUPPORTED) != null) {
						managedIsToolChainSupported = (IManagedIsToolChainSupported) element
								.createExecutableExtension(IS_TOOL_CHAIN_SUPPORTED);
					}
				} catch (CoreException e) {
				}
			}
		}

		// if (managedIsToolChainSupported != null) {
		// try {
		// return managedIsToolChainSupported.isSupported(this, null, null);
		// } catch (Throwable e) {
		// Activator.log(new Status(IStatus.ERROR, Activator.getId(),
		// "Exception in toolchain [" + getName() + "], id=" + getId(), e));
		// //$NON-NLS-1$ //$NON-NLS-2$
		// return false;
		// }
		// }
		return true;
	}

	/**
	 * Returns the plugin.xml element of the configurationEnvironmentSupplier
	 * extension or <code>null</code> if none.
	 *
	 * @return IConfigurationElement
	 */
	public IConfigurationElement getEnvironmentVariableSupplierElement() {
//		if (environmentVariableSupplierElement == null) {
//			if (getSuperClass() != null && getSuperClass() instanceof ToolChain) {
//				return ((ToolChain) getSuperClass()).getEnvironmentVariableSupplierElement();
//			}
//		}
		return environmentVariableSupplierElement;
	}

	@Override
	public IEnvironmentVariableSupplier getEnvironmentVariableSupplier() {
		if (environmentVariableSupplier != null) {
			return environmentVariableSupplier;
		}
		IConfigurationElement element = getEnvironmentVariableSupplierElement();
		if (element != null) {
			try {
				if (element.getAttribute(CONFIGURATION_ENVIRONMENT_SUPPLIER) != null) {
					environmentVariableSupplier = (IEnvironmentVariableSupplier) element
							.createExecutableExtension(CONFIGURATION_ENVIRONMENT_SUPPLIER);
					return environmentVariableSupplier;
				}
			} catch (CoreException e) {
			}
		}
		return null;
	}

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

	/**
	 * Returns the plugin.xml element of the configurationMacroSupplier extension or
	 * <code>null</code> if none.
	 *
	 * @return IConfigurationElement
	 */
	public IConfigurationElement getBuildMacroSupplierElement() {
		if (buildMacroSupplierElement == null) {
			if (superClass != null && superClass instanceof ToolChain) {
				return ((ToolChain) superClass).getBuildMacroSupplierElement();
			}
		}
		return buildMacroSupplierElement;
	}

	@Override
	public IConfigurationBuildMacroSupplier getBuildMacroSupplier() {
		if (buildMacroSupplier != null) {
			return buildMacroSupplier;
		}
		IConfigurationElement element = getBuildMacroSupplierElement();
		if (element != null) {
			try {
				if (element.getAttribute(CONFIGURATION_MACRO_SUPPLIER) != null) {
					buildMacroSupplier = (IConfigurationBuildMacroSupplier) element
							.createExecutableExtension(CONFIGURATION_MACRO_SUPPLIER);
					return buildMacroSupplier;
				}
			} catch (CoreException e) {
			}
		}
		return null;
	}

	@Override
	public IFolderInfo getParentFolderInfo() {
		return parentFolderInfo;
	}

	void setTargetPlatform(TargetPlatform tp) {
		targetPlatform = tp;
	}

	@Override
	public CTargetPlatformData getTargetPlatformData() {
		if (targetPlatform == null) {
			ITargetPlatform platform = getTargetPlatform();
			if (platform != null) {
				ITargetPlatform extPlatform = platform;
				for (; extPlatform != null
						&& !extPlatform.isExtensionElement(); extPlatform = extPlatform.getSuperClass()) {
					// No body, this loop is to find extension element
				}
				String subId;
				if (extPlatform != null)
					subId = ManagedBuildManager.calculateChildId(extPlatform.getId(), null);
				else
					subId = ManagedBuildManager.calculateChildId(getId(), null);

				targetPlatform = new TargetPlatform(this, subId, platform.getName(), (TargetPlatform) extPlatform);
			} else {
				String subId = ManagedBuildManager.calculateChildId(getId(), null);
				targetPlatform = new TargetPlatform(this, null, subId, "", false); //$NON-NLS-1$
			}
		}

		return targetPlatform.getTargetPlatformData();
	}

	public BooleanExpressionApplicabilityCalculator getBooleanExpressionCalculator() {
		if (booleanExpressionCalculator == null) {
			if (superClass != null) {
				return ((ToolChain) superClass).getBooleanExpressionCalculator();
			}
		}
		return booleanExpressionCalculator;
	}

	@Override
	protected IResourceInfo getParentResourceInfo() {
		//return getParentFolderInfo();
		return null;
	}

	@Override
	public boolean supportsBuild(boolean managed) {

		IBuilder builder = getBuilder();
		if (builder != null && !builder.supportsBuild(managed))
			return false;

		List<ITool> tools = getTools();
		for (ITool tool : tools) {
			if (!tool.supportsBuild(managed))
				return false;
		}

		return true;
	}

	@Override
	public boolean isSystemObject() {
		if (isTest)
			return true;

		return false;
	}

	public String getNameAndVersion() {
		String name = getName();
		String version = ManagedBuildManager.getVersionFromIdAndVersion(getId());
		if (version != null && version.length() != 0) {
			return new StringBuilder().append(name).append(" (").append(version).append("").toString(); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return name;
	}

	@Override
	public String getUniqueRealName() {
		String name = getName();
		if (name == null) {
			name = getId();
		} else {
			String version = ManagedBuildManager.getVersionFromIdAndVersion(getId());
			if (version != null) {
				StringBuilder buf = new StringBuilder();
				buf.append(name);
				buf.append(" (v").append(version).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
				name = buf.toString();
			}
		}
		return name;
	}

//	void resolveProjectReferences(boolean onLoad) {
//		for (Tool tool : getToolList()) {
//			tool.resolveProjectReferences(onLoad);
//		}
//	}

	public boolean hasScannerConfigSettings() {

		if (getScannerConfigDiscoveryProfileId() != null)
			return true;

		return false;
	}

	public boolean isPerRcTypeDiscovery() {
		if (isRcTypeBasedDiscovery == null) {
			if (superClass != null) {
				return ((ToolChain) superClass).isPerRcTypeDiscovery();
			}
			return true;
		}
		return isRcTypeBasedDiscovery.booleanValue();
	}

	public void setPerRcTypeDiscovery(boolean on) {
		isRcTypeBasedDiscovery = Boolean.valueOf(on);
	}

	public PathInfoCache setDiscoveredPathInfo(PathInfoCache info) {
		PathInfoCache oldInfo = discoveredInfo;
		discoveredInfo = info;
		return oldInfo;
	}

	public PathInfoCache getDiscoveredPathInfo() {
		return discoveredInfo;
	}

	public PathInfoCache clearDiscoveredPathInfo() {
		PathInfoCache oldInfo = discoveredInfo;
		discoveredInfo = null;
		return oldInfo;
	}

	public boolean isPreferenceToolChain() {
		return false;
		// IToolChain tch = ManagedBuildManager.getRealToolChain(this);
		// return tch != null &&
		// tch.getId().equals(ConfigurationDataProvider.PREF_TC_ID);
	}

//	public boolean hasCustomSettings(ToolChain tCh) {
//		if (superClass == null)
//			return true;
//
//		IToolChain realTc = ManagedBuildManager.getRealToolChain(this);
//		IToolChain otherRealTc = ManagedBuildManager.getRealToolChain(tCh);
//		if (realTc != otherRealTc)
//			return true;
//
//		if (hasCustomSettings())
//			return true;
//
//		List<ITool> tools = getTools();
//		List<ITool> otherTools = tCh.getTools();
//		if (tools.size() != otherTools.size())
//			return true;
//
//		for (int i = 0; i < tools.size(); i++) {
//			Tool tool = (Tool) tools[i];
//			Tool otherTool = (Tool) otherTools[i];
//			if (tool.hasCustomSettings(otherTool))
//				return true;
//		}
//		return false;
//	}

}
