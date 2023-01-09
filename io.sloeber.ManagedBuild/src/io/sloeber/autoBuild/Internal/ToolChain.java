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

    private static final String EMPTY_STRING = ""; //$NON-NLS-1$

    private static final String REBUILD_STATE = "rebuildState"; //$NON-NLS-1$

    private static final boolean resolvedDefault = true;

    //  Superclass
    //  Note that superClass itself is defined in the base and that the methods
    //  getSuperClass() and setSuperClassInternal(), defined in ToolChain must be used
    //  to access it. This avoids widespread casts from IHoldsOptions to IToolChain.
    private String superClassId;
    //  Parent and children
    private IConfiguration config;
    private List<Tool> toolList;
    private Map<String, Tool> toolMap;
    private TargetPlatform targetPlatform;
    private Builder builder;
    //  Managed Build model attributes
    private String errorParserIds;
    private List<String> osList;
    private List<String> archList;
    private String targetToolIds;
    private String secondaryOutputIds;
    private Boolean isAbstract;
    private String defaultLanguageSettingsProviderIds;
    private String scannerConfigDiscoveryProfileId;
    private String versionsSupported;
    private String convertToId;
    private IConfigurationElement managedIsToolChainSupportedElement = null;
    private IManagedIsToolChainSupported managedIsToolChainSupported = null;
    private IConfigurationElement environmentVariableSupplierElement = null;
    private IEnvironmentVariableSupplier environmentVariableSupplier = null;
    private IConfigurationElement buildMacroSupplierElement = null;
    private IConfigurationBuildMacroSupplier buildMacroSupplier = null;
    private IConfigurationElement pathconverterElement = null;
    private IOptionPathConverter optionPathConverter = null;
    private Boolean supportsManagedBuild;
    private boolean isTest;
    private SupportedProperties supportedProperties;
    private String nonInternalBuilderId;

    //  Miscellaneous
    private boolean isExtensionToolChain = false;
    private boolean isDirty = false;
    private boolean resolved = resolvedDefault;

    //used for loadding pre-4.0 projects only
    private StorableCdtVariables userDefinedMacros;
    //holds user-defined macros
    //	private StorableEnvironment userDefinedEnvironment;

    private IConfigurationElement previousMbsVersionConversionElement = null;
    private IConfigurationElement currentMbsVersionConversionElement = null;
    private boolean rebuildState;
    private BooleanExpressionApplicabilityCalculator booleanExpressionCalculator;

    private List<ToolChain> identicalList;
    private Set<String> unusedChildrenSet;

    private IFolderInfo parentFolderInfo;

    private PathInfoCache discoveredInfo;
    private Boolean isRcTypeBasedDiscovery;

    private List<OptionEnablementExpression> myEnablements = new ArrayList<>();;

    /**
     * This constructor is called to create a tool-chain defined by an extension
     * point in
     * a plugin manifest file, or returned by a dynamic element provider
     *
     * @param parentFldInfo
     *            The {@link IFolderInfo} parent of this tool-chain, or {@code null}
     *            if
     *            defined at the top level
     * @param element
     *            The tool-chain definition from the manifest file or a dynamic
     *            element
     *            provider
     * @param managedBuildRevision
     *            the fileVersion of Managed Build System
     */
    public ToolChain(IFolderInfo parentFldInfo, IExtensionPoint root, IConfigurationElement element) {
        // setup for resolving
        super(false);
        resolved = false;

        if (parentFldInfo != null) {
            this.config = parentFldInfo.getParent();
            parentFolderInfo = parentFldInfo;
        }

        isExtensionToolChain = true;

        // Set the managedBuildRevision
        //       setManagedBuildRevision(managedBuildRevision);

        booleanExpressionCalculator = new BooleanExpressionApplicabilityCalculator(myEnablements);

        //       ManagedBuildManager.putConfigElement(this, element);

        loadNameAndID(root, element);

        // version
        setVersion(getVersionFromId());

        // superClass
        superClassId = SafeStringInterner.safeIntern(element.getAttribute(IProjectType.SUPERCLASS));

        // isAbstract
        String isAbs = element.getAttribute(IS_ABSTRACT);
        if (isAbs != null) {
            isAbstract = Boolean.parseBoolean(isAbs);
        }

        // Get the semicolon separated list of IDs of the error parsers
        errorParserIds = SafeStringInterner.safeIntern(element.getAttribute(ERROR_PARSERS));

        // Get the semicolon separated list of IDs of the secondary outputs
        secondaryOutputIds = SafeStringInterner.safeIntern(element.getAttribute(SECONDARY_OUTPUTS));

        // Get the target tool id
        targetToolIds = SafeStringInterner.safeIntern(element.getAttribute(TARGET_TOOL));

        // Get the initial/default language settings providers IDs
        defaultLanguageSettingsProviderIds = element.getAttribute(LANGUAGE_SETTINGS_PROVIDERS);

        // Get the scanner config discovery profile id
        scannerConfigDiscoveryProfileId = SafeStringInterner
                .safeIntern(element.getAttribute(SCANNER_CONFIG_PROFILE_ID));
        String tmp = element.getAttribute(RESOURCE_TYPE_BASED_DISCOVERY);
        if (tmp != null)
            isRcTypeBasedDiscovery = Boolean.valueOf(tmp);

        // Get the 'versionsSupported' attribute
        versionsSupported = SafeStringInterner.safeIntern(element.getAttribute(VERSIONS_SUPPORTED));

        // Get the 'convertToId' attribute
        convertToId = SafeStringInterner.safeIntern(element.getAttribute(CONVERT_TO_ID));

        tmp = element.getAttribute(SUPPORTS_MANAGED_BUILD);
        if (tmp != null)
            supportsManagedBuild = Boolean.valueOf(tmp);

        tmp = element.getAttribute(IS_SYSTEM);
        if (tmp != null)
            isTest = Boolean.valueOf(tmp).booleanValue();

        // Get the comma-separated list of valid OS
        String os = element.getAttribute(OS_LIST);
        if (os != null) {
            osList = new ArrayList<>();
            String[] osTokens = os.split(","); //$NON-NLS-1$
            for (int i = 0; i < osTokens.length; ++i) {
                osList.add(SafeStringInterner.safeIntern(osTokens[i].trim()));
            }
        }

        // Get the comma-separated list of valid Architectures
        String arch = element.getAttribute(ARCH_LIST);
        if (arch != null) {
            archList = new ArrayList<>();
            String[] archTokens = arch.split(","); //$NON-NLS-1$
            for (int j = 0; j < archTokens.length; ++j) {
                archList.add(SafeStringInterner.safeIntern(archTokens[j].trim()));
            }
        }

        nonInternalBuilderId = SafeStringInterner.safeIntern(element.getAttribute(NON_INTERNAL_BUILDER_ID));

        // Hook me up to the Managed Build Manager
        //      ManagedBuildManager.addExtensionToolChain(this);

        // Load the TargetPlatform child
        IConfigurationElement[] targetPlatforms = element.getChildren(ITargetPlatform.TARGET_PLATFORM_ELEMENT_NAME);
        if (targetPlatforms.length < 1 || targetPlatforms.length > 1) {
            // TODO: Report error
        }
        if (targetPlatforms.length > 0) {
            targetPlatform = new TargetPlatform(this, root, targetPlatforms[0]);
        }

        // Load the Builder child
        IConfigurationElement[] builders = element.getChildren(IBuilder.BUILDER_ELEMENT_NAME);
        if (builders.length < 1 || builders.length > 1) {
            // TODO: Report error
        }
        if (builders.length > 0) {
            builder = new Builder(this, root, builders[0]);
        }

        // Load children
        IConfigurationElement[] toolChainElements = element.getChildren();
        for (int l = 0; l < toolChainElements.length; ++l) {
            IConfigurationElement toolChainElement = toolChainElements[l];
            if (loadChild(root, toolChainElement)) {
                // do nothing
            } else if (toolChainElement.getName().equals(ITool.TOOL_ELEMENT_NAME)) {
                Tool toolChild = new Tool(this, root, toolChainElement);
                addTool(toolChild);
            } else if (toolChainElement.getName().equals(SupportedProperties.SUPPORTED_PROPERTIES)) {
                loadProperties(toolChainElement);
            }
        }
        IConfigurationElement enablements[] = element.getChildren(OptionEnablementExpression.NAME);
        for (IConfigurationElement curEnablement : enablements) {
            myEnablements.add(new OptionEnablementExpression(curEnablement));
        }
    }

    /**
     * This constructor is called to create a ToolChain whose attributes and
     * children will be
     * added by separate calls.
     *
     * @param parentFldInfo
     *            The parent of the tool chain, if any
     * @param superClass
     *            The superClass, if any
     * @param Id
     *            The ID for the new tool chain
     * @param name
     *            The name for the new tool chain
     * @param isExtensionElement
     *            Indicates whether this is an extension element or a managed
     *            project element
     */
    public ToolChain(IFolderInfo parentFldInfo, IToolChain superClass, String Id, String name,
            boolean isExtensionElement) {
        super(resolvedDefault);
        //        this.config = parentFldInfo.getParent();
        //        parentFolderInfo = parentFldInfo;
        //
        //        setSuperClassInternal(superClass);
        //        setManagedBuildRevision(config.getManagedBuildRevision());
        //
        //        if (getSuperClass() != null) {
        //            superClassId = getSuperClass().getId();
        //        }
        //        setId(Id);
        //        setName(name);
        //        setVersion(getVersionFromId());
        //
        //        //        isExtensionToolChain = isExtensionElement;
        //        //        if (isExtensionElement) {
        //        //            // Hook me up to the Managed Build Manager
        //        //            ManagedBuildManager.addExtensionToolChain(this);
        //        //        } else {
        //        //            setRebuildState(true);
        //        //        }
    }

    /**
     * Create a {@link ToolChain} based on the specification stored in the
     * project file (.cproject).
     *
     * @param parentFldInfo
     *            The {@link IFolderInfo} the tool-chain will be added to.
     * @param element
     *            The XML element that contains the tool-chain settings.
     * @param managedBuildRevision
     *            the fileVersion of Managed Build System
     */
    public ToolChain(IFolderInfo parentFldInfo, ICStorageElement element) {
        super(resolvedDefault);
        //        this.config = parentFldInfo.getParent();
        //        this.parentFolderInfo = parentFldInfo;
        //
        //        this.isExtensionToolChain = false;
        //
        //    
        //        // Initialize from the XML attributes
        //        loadFromProject(element);
        //
        //        // Load children
        //        ICStorageElement configElements[] = element.getChildren();
        //        for (int i = 0; i < configElements.length; ++i) {
        //            ICStorageElement configElement = configElements[i];
        //            if (loadChild(configElement)) {
        //                // do nothing
        //            } else if (configElement.getName().equals(ITool.TOOL_ELEMENT_NAME)) {
        //                Tool tool = new Tool(this, configElement, managedBuildRevision);
        //                addTool(tool);
        //            } else if (configElement.getName().equals(ITargetPlatform.TARGET_PLATFORM_ELEMENT_NAME)) {
        //                if (targetPlatform != null) {
        //                    // TODO: report error
        //                }
        //                targetPlatform = new TargetPlatform(this, configElement, managedBuildRevision);
        //            } else if (configElement.getName().equals(IBuilder.BUILDER_ELEMENT_NAME)) {
        //                if (builder != null) {
        //                    // TODO: report error
        //                }
        //                builder = new Builder(this, configElement, managedBuildRevision);
        //            }
        //        }
        //
        //        String rebuild = PropertyManager.getInstance().getProperty(this, REBUILD_STATE);
        //        if (rebuild == null || Boolean.valueOf(rebuild).booleanValue())
        //            rebuildState = true;

    }

    /**
     * Create a {@link ToolChain} based upon an existing tool chain.
     *
     * @param parentFldInfo
     *            The {@link IConfiguration} the tool-chain will be added to.
     * @param Id
     *            ID of the new tool-chain
     * @param name
     *            name of the new tool-chain
     * @param toolChain
     *            The existing tool-chain to clone.
     */
    public ToolChain(IFolderInfo parentFldInfo, String Id, String name, Map<IPath, Map<String, String>> superIdMap,
            ToolChain toolChain) {
        super(resolvedDefault);
        //        this.config = parentFldInfo.getParent();
        //        this.parentFolderInfo = parentFldInfo;
        //        setSuperClassInternal(toolChain.getSuperClass());
        //        if (getSuperClass() != null) {
        //            if (toolChain.superClassId != null) {
        //                superClassId = toolChain.superClassId;
        //            }
        //        }
        //        setId(Id);
        //        setName(name);
        //
        //        // Set the managedBuildRevision and the version
        //        setManagedBuildRevision(toolChain.getManagedBuildRevision());
        //        setVersion(getVersionFromId());
        //
        //        isExtensionToolChain = false;
        //
        //        //  Copy the remaining attributes
        //        if (toolChain.versionsSupported != null) {
        //            versionsSupported = toolChain.versionsSupported;
        //        }
        //        if (toolChain.convertToId != null) {
        //            convertToId = toolChain.convertToId;
        //        }
        //
        //        if (toolChain.errorParserIds != null) {
        //            errorParserIds = toolChain.errorParserIds;
        //        }
        //        if (toolChain.osList != null) {
        //            osList = new ArrayList<>(toolChain.osList);
        //        }
        //        if (toolChain.archList != null) {
        //            archList = new ArrayList<>(toolChain.archList);
        //        }
        //        if (toolChain.targetToolIds != null) {
        //            targetToolIds = toolChain.targetToolIds;
        //        }
        //        if (toolChain.secondaryOutputIds != null) {
        //            secondaryOutputIds = toolChain.secondaryOutputIds;
        //        }
        //        if (toolChain.isAbstract != null) {
        //            isAbstract = toolChain.isAbstract;
        //        }
        //        if (toolChain.scannerConfigDiscoveryProfileId != null) {
        //            scannerConfigDiscoveryProfileId = toolChain.scannerConfigDiscoveryProfileId;
        //        }
        //
        //        isRcTypeBasedDiscovery = toolChain.isRcTypeBasedDiscovery;
        //
        //        supportsManagedBuild = toolChain.supportsManagedBuild;
        //
        //        managedIsToolChainSupportedElement = toolChain.managedIsToolChainSupportedElement;
        //        managedIsToolChainSupported = toolChain.managedIsToolChainSupported;
        //
        //        environmentVariableSupplierElement = toolChain.environmentVariableSupplierElement;
        //        environmentVariableSupplier = toolChain.environmentVariableSupplier;
        //
        //        buildMacroSupplierElement = toolChain.buildMacroSupplierElement;
        //        buildMacroSupplier = toolChain.buildMacroSupplier;
        //
        //        pathconverterElement = toolChain.pathconverterElement;
        //        optionPathConverter = toolChain.optionPathConverter;
        //
        //        nonInternalBuilderId = toolChain.nonInternalBuilderId;
        //
        //        discoveredInfo = toolChain.discoveredInfo;
        //
        //        userDefinedMacros = toolChain.userDefinedMacros;
        //
        //        //  Clone the children in superclass
        //        boolean copyIds = toolChain.getId().equals(id);
        //        super.copyChildren(toolChain);
        //        //  Clone the children
        //        if (toolChain.builder != null) {
        //            String subId;
        //            String subName;
        //
        //            if (toolChain.builder.getSuperClass() != null) {
        //                subId = copyIds ? toolChain.builder.getId()
        //                        : ManagedBuildManager.calculateChildId(toolChain.builder.getSuperClass().getId(), null);
        //                subName = toolChain.builder.getSuperClass().getName();
        //            } else {
        //                subId = copyIds ? toolChain.builder.getId()
        //                        : ManagedBuildManager.calculateChildId(toolChain.builder.getId(), null);
        //                subName = toolChain.builder.getName();
        //            }
        //
        //            builder = new Builder(this, subId, subName, toolChain.builder);
        //        }
        //        //		if (toolChain.targetPlatform != null)
        //        {
        //            ITargetPlatform tpBase = toolChain.getTargetPlatform();
        //            if (tpBase != null) {
        //                ITargetPlatform extTp = tpBase;
        //                for (; extTp != null && !extTp.isExtensionElement(); extTp = extTp.getSuperClass()) {
        //                    // empty body, the loop is to find extension element
        //                }
        //
        //                String subId;
        //                if (copyIds) {
        //                    subId = tpBase.getId();
        //                } else {
        //                    subId = extTp != null ? ManagedBuildManager.calculateChildId(extTp.getId(), null)
        //                            : ManagedBuildManager.calculateChildId(getId(), null);
        //                }
        //                String subName = tpBase.getName();
        //
        //                //				if (toolChain.targetPlatform.getSuperClass() != null) {
        //                //					subId = toolChain.targetPlatform.getSuperClass().getId() + "." + nnn;		//$NON-NLS-1$
        //                //					subName = toolChain.targetPlatform.getSuperClass().getName();
        //                //				} else {
        //                //					subId = toolChain.targetPlatform.getId() + "." + nnn;		//$NON-NLS-1$
        //                //					subName = toolChain.targetPlatform.getName();
        //                //				}
        //                targetPlatform = new TargetPlatform(this, subId, subName, (TargetPlatform) tpBase);
        //            }
        //        }
        //
        //        IConfiguration cfg = parentFolderInfo.getParent();
        //        if (toolChain.toolList != null) {
        //            for (Tool toolChild : toolChain.getToolList()) {
        //                String subId = null;
        //                //				String tmpId;
        //                String subName;
        //                //				String version;
        //                ITool extTool = ManagedBuildManager.getExtensionTool(toolChild);
        //                Map<String, String> curIdMap = superIdMap.get(parentFldInfo.getPath());
        //                if (curIdMap != null) {
        //                    if (extTool != null)
        //                        subId = curIdMap.get(extTool.getId());
        //                }
        //
        //                subName = toolChild.getName();
        //
        //                if (subId == null) {
        //                    if (extTool != null) {
        //                        subId = copyIds ? toolChild.getId()
        //                                : ManagedBuildManager.calculateChildId(extTool.getId(), null);
        //                        //						subName = toolChild.getSuperClass().getName();
        //                    } else {
        //                        subId = copyIds ? toolChild.getId()
        //                                : ManagedBuildManager.calculateChildId(toolChild.getId(), null);
        //                        //						subName = toolChild.getName();
        //                    }
        //                }
        //                //				version = ManagedBuildManager.getVersionFromIdAndVersion(tmpId);
        //                //				if ( version != null) {		// If the 'tmpId' contains version information
        //                //					subId = ManagedBuildManager.getIdFromIdAndVersion(tmpId) + "." + nnn + "_" + version;		//$NON-NLS-1$ //$NON-NLS-2$
        //                //				} else {
        //                //					subId = tmpId + "." + nnn;		//$NON-NLS-1$
        //                //				}
        //
        //                //  The superclass for the cloned tool is not the same as the one from the tool being cloned.
        //                //  The superclasses reside in different configurations.
        //                ITool toolSuperClass = null;
        //                String superId = null;
        //                //  Search for the tool in this configuration that has the same grand-superClass as the
        //                //  tool being cloned
        //                ITool otherSuperTool = toolChild.getSuperClass();
        //                if (otherSuperTool != null) {
        //                    if (otherSuperTool.isExtensionElement()) {
        //                        toolSuperClass = otherSuperTool;
        //                    } else {
        //                        IResourceInfo otherRcInfo = otherSuperTool.getParentResourceInfo();
        //                        IResourceInfo thisRcInfo = cfg.getResourceInfo(otherRcInfo.getPath(), true);
        //                        ITool otherExtTool = ManagedBuildManager.getExtensionTool(otherSuperTool);
        //                        if (otherExtTool != null) {
        //                            if (thisRcInfo != null) {
        //                                ITool tools[] = thisRcInfo.getTools();
        //                                for (int i = 0; i < tools.length; i++) {
        //                                    ITool thisExtTool = ManagedBuildManager.getExtensionTool(tools[i]);
        //                                    if (otherExtTool.equals(thisExtTool)) {
        //                                        toolSuperClass = tools[i];
        //                                        superId = toolSuperClass.getId();
        //                                        break;
        //                                    }
        //                                }
        //                            } else {
        //                                superId = copyIds ? otherSuperTool.getId()
        //                                        : ManagedBuildManager.calculateChildId(otherExtTool.getId(), null);
        //                                Map<String, String> idMap = superIdMap.get(otherRcInfo.getPath());
        //                                if (idMap == null) {
        //                                    idMap = new HashMap<>();
        //                                    superIdMap.put(otherRcInfo.getPath(), idMap);
        //                                }
        //                                idMap.put(otherExtTool.getId(), superId);
        //                            }
        //                        }
        //                    }
        //                }
        //                //				Tool newTool = new Tool(this, (Tool)null, subId, subName, toolChild);
        //                //				addTool(newTool);
        //
        //                Tool newTool = null;
        //                if (toolSuperClass != null)
        //                    newTool = new Tool(this, toolSuperClass, subId, subName, toolChild);
        //                else if (superId != null)
        //                    newTool = new Tool(this, superId, subId, subName, toolChild);
        //                else {
        //                    //TODO: Error
        //                }
        //                if (newTool != null)
        //                    addTool(newTool);
        //
        //            }
        //        }
        //
        //        if (copyIds) {
        //            rebuildState = toolChain.rebuildState;
        //            isDirty = toolChain.isDirty;
        //        } else {
        //            setRebuildState(true);
        //        }
    }

    /*
     *  E L E M E N T   A T T R I B U T E   R E A D E R S   A N D   W R I T E R S
     */

    /**
     * Initialize the tool-chain information from the XML element
     * specified in the argument
     *
     * @param element
     *            An XML element containing the tool-chain information
     */
    protected void loadFromProject(ICStorageElement element) {

        loadNameAndID(element);

        // version
        setVersion(getVersionFromId());

        // superClass
        superClassId = SafeStringInterner.safeIntern(element.getAttribute(IProjectType.SUPERCLASS));
        if (superClassId != null && superClassId.length() > 0) {
            setSuperClassInternal(null);//TOFIX JABA ManagedBuildManager.getExtensionToolChain(superClassId));
            // Check for migration support
            //       checkForMigrationSupport();
        }

        // isAbstract
        if (element.getAttribute(IS_ABSTRACT) != null) {
            String isAbs = element.getAttribute(IS_ABSTRACT);
            if (isAbs != null) {
                isAbstract = Boolean.parseBoolean(isAbs);
            }
        }

        // Get the semicolon separated list of IDs of the error parsers
        if (element.getAttribute(ERROR_PARSERS) != null) {
            errorParserIds = SafeStringInterner.safeIntern(element.getAttribute(ERROR_PARSERS));
        }

        // Get the semicolon separated list of IDs of the secondary outputs
        if (element.getAttribute(SECONDARY_OUTPUTS) != null) {
            secondaryOutputIds = SafeStringInterner.safeIntern(element.getAttribute(SECONDARY_OUTPUTS));
        }

        // Get the target tool id
        if (element.getAttribute(TARGET_TOOL) != null) {
            targetToolIds = SafeStringInterner.safeIntern(element.getAttribute(TARGET_TOOL));
        }

        // Get the scanner config discovery profile id
        if (element.getAttribute(SCANNER_CONFIG_PROFILE_ID) != null) {
            scannerConfigDiscoveryProfileId = SafeStringInterner
                    .safeIntern(element.getAttribute(SCANNER_CONFIG_PROFILE_ID));
        }

        // Get the 'versionSupported' attribute
        if (element.getAttribute(VERSIONS_SUPPORTED) != null) {
            versionsSupported = SafeStringInterner.safeIntern(element.getAttribute(VERSIONS_SUPPORTED));
        }

        // Get the 'convertToId' id
        if (element.getAttribute(CONVERT_TO_ID) != null) {
            convertToId = SafeStringInterner.safeIntern(element.getAttribute(CONVERT_TO_ID));
        }

        // Get the comma-separated list of valid OS
        if (element.getAttribute(OS_LIST) != null) {
            String os = element.getAttribute(OS_LIST);
            if (os != null) {
                osList = new ArrayList<>();
                String[] osTokens = os.split(","); //$NON-NLS-1$
                for (int i = 0; i < osTokens.length; ++i) {
                    osList.add(SafeStringInterner.safeIntern(osTokens[i].trim()));
                }
            }
        }

        // Get the comma-separated list of valid Architectures
        if (element.getAttribute(ARCH_LIST) != null) {
            String arch = element.getAttribute(ARCH_LIST);
            if (arch != null) {
                archList = new ArrayList<>();
                String[] archTokens = arch.split(","); //$NON-NLS-1$
                for (int j = 0; j < archTokens.length; ++j) {
                    archList.add(SafeStringInterner.safeIntern(archTokens[j].trim()));
                }
            }
        }

        // Note: optionPathConverter cannot be specified in a project file because
        //       an IConfigurationElement is needed to load it!
        if (pathconverterElement != null) {
            //  TODO:  issue warning?
        }

        // Get the scanner config discovery profile id
        scannerConfigDiscoveryProfileId = element.getAttribute(SCANNER_CONFIG_PROFILE_ID);
        String tmp = element.getAttribute(RESOURCE_TYPE_BASED_DISCOVERY);
        if (tmp != null)
            isRcTypeBasedDiscovery = Boolean.valueOf(tmp);

        nonInternalBuilderId = SafeStringInterner.safeIntern(element.getAttribute(NON_INTERNAL_BUILDER_ID));

        //		String tmp = element.getAttribute(name)
    }

    /*
     *  P A R E N T   A N D   C H I L D   H A N D L I N G
     */

    @Override
    public IConfiguration getParent() {
        return config;
    }

    @Override
    public ITargetPlatform getTargetPlatform() {
        if (targetPlatform == null) {
            if (getSuperClass() != null) {
                return getSuperClass().getTargetPlatform();
            }
        }
        return targetPlatform;
    }

    @Override
    public void removeLocalTargetPlatform() {
        if (targetPlatform == null)
            return;
        targetPlatform = null;
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
        if (builder == null) {
            if (getSuperClass() != null) {
                return getSuperClass().getBuilder();
            }
        }
        return builder;
    }

    @Override
    public void removeLocalBuilder() {
        if (builder == null)
            return;
        builder = null;
    }

    @Override
    public ITool createTool(ITool superClass, String id, String name, boolean isExtensionElement) {
        Tool tool = new Tool(this, superClass, id, name, isExtensionElement);
        addTool(tool);
        return tool;
    }

    @Override
    public ITool[] getTools() {
        ITool tools[] = getAllTools(false);
        if (!isExtensionToolChain) {
            for (int i = 0; i < tools.length; i++) {
                if (tools[i].isExtensionElement()) {
                    String subId = ManagedBuildManager.calculateChildId(tools[i].getId(), null);
                    tools[i] = createTool(tools[i], subId, tools[i].getName(), false);
                }
            }
        }

        return tools;
    }

    public Tool[] getAllTools(boolean includeCurrentUnused) {
        Tool[] tools = null;
        //  Merge our tools with our superclass' tools
        if (getSuperClass() != null) {
            tools = ((ToolChain) getSuperClass()).getAllTools(false);
        }
        //  Our tools take precedence
        if (tools != null) {
            for (Tool tool : getToolList()) {
                int j = 0;
                for (; j < tools.length; j++) {
                    ITool superTool = tool.getSuperClass();
                    if (superTool != null) {
                        superTool = null;//TOFIX JABA ManagedBuildManager.getExtensionTool(superTool);
                        if (superTool != null && superTool.getId().equals(tools[j].getId())) {
                            tools[j] = tool;
                            break;
                        }
                    }
                }
                //  No Match?  Insert it (may be re-ordered)
                if (j == tools.length) {
                    Tool[] newTools = new Tool[tools.length + 1];
                    for (int k = 0; k < tools.length; k++) {
                        newTools[k] = tools[k];
                    }
                    newTools[j] = tool;
                    tools = newTools;
                }
            }
        } else {
            tools = new Tool[getToolList().size()];
            int i = 0;
            for (Tool tool : getToolList()) {
                tools[i++] = tool;
            }
        }
        if (includeCurrentUnused)
            return tools;
        return filterUsedTools(tools, true);
    }

    private Tool[] filterUsedTools(Tool tools[], boolean used) {
        return used ? tools : new Tool[0];
    }

    @Override
    public ITool getTool(String id) {
        Tool tool = getToolMap().get(id);
        return tool;
    }

    @Override
    public ITool[] getToolsBySuperClassId(String id) {
        List<ITool> retTools = new ArrayList<>();
        if (id != null) {
            //  Look for a tool with this ID, or the tool(s) with a superclass with this id
            ITool[] tools = getTools();
            for (ITool targetTool : tools) {
                ITool tool = targetTool;
                do {
                    if (id.equals(tool.getId())) {
                        retTools.add(targetTool);
                        break;
                    }
                    tool = tool.getSuperClass();
                } while (tool != null);
            }
        }
        return retTools.toArray(new ITool[retTools.size()]);
    }

    /**
     * Safe accessor for the list of tools.
     *
     * @return List containing the tools
     */
    public List<Tool> getToolList() {
        if (toolList == null) {
            toolList = new ArrayList<>();
        }
        return toolList;
    }

    /**
     * Safe accessor for the map of tool ids to tools
     */
    private Map<String, Tool> getToolMap() {
        if (toolMap == null) {
            toolMap = new HashMap<>();
        }
        return toolMap;
    }

    /**
     * Adds the Tool to the tool-chain list and map
     *
     * @param tool
     *            - tool to add
     */
    public void addTool(Tool tool) {
        getToolList().add(tool);
        getToolMap().put(tool.getId(), tool);
    }

    void setToolsInternal(ITool[] tools) {
        List<Tool> list = getToolList();
        Map<String, Tool> map = getToolMap();

        list.clear();
        map.clear();

        for (ITool t : tools) {
            list.add((Tool) t);
            map.put(t.getId(), (Tool) t);
        }
    }

    /*
     *  M O D E L   A T T R I B U T E   A C C E S S O R S
     */

    @Override
    public IToolChain getSuperClass() {
        return (IToolChain) superClass;
    }

    /**
     * Access function to set the superclass element that is defined in
     * the base class.
     */
    private void setSuperClassInternal(IToolChain superClass) {
        this.superClass = superClass;
    }

    public void setSuperClass(IToolChain superClass) {
        if (this.superClass != superClass) {
            this.superClass = superClass;
            if (this.superClass == null) {
                superClassId = null;
            } else {
                superClassId = this.superClass.getId();
            }

        }
    }

    @Override
    public String getName() {
        return (name == null && getSuperClass() != null) ? getSuperClass().getName() : name;
    }

    @Override
    public boolean isAbstract() {
        if (isAbstract != null) {
            return isAbstract.booleanValue();
        }
        return false; // Note: no inheritance from superClass
    }

    @Override
    public String getErrorParserIds() {
        String ids = errorParserIds;
        if (ids == null) {
            // If I have a superClass, ask it
            if (getSuperClass() != null) {
                ids = getSuperClass().getErrorParserIds();
            }
        }
        if (ids == null) {
            // Collect the error parsers from my children
            ids = builder.getErrorParserIds();
            ITool[] tools = getTools();
            for (int i = 0; i < tools.length; i++) {
                ITool tool = tools[i];
                String toolIds = tool.getErrorParserIds();
                if (toolIds != null && toolIds.length() > 0) {
                    if (ids != null) {
                        ids += ";"; //$NON-NLS-1$
                        ids += toolIds;
                    } else {
                        ids = toolIds;
                    }
                }
            }
        }
        return ids;
    }

    public String getErrorParserIdsAttribute() {
        String ids = errorParserIds;
        if (ids == null) {
            // If I have a superClass, ask it
            if (getSuperClass() != null) {
                ids = ((ToolChain) getSuperClass()).getErrorParserIdsAttribute();
            }
        }
        return ids;
    }

    @Override
    public IOutputType[] getSecondaryOutputs() {
        IOutputType[] types = null;
        String ids = secondaryOutputIds;
        if (ids == null) {
            if (getSuperClass() != null) {
                return getSuperClass().getSecondaryOutputs();
            } else {
                return new IOutputType[0];
            }
        }
        StringTokenizer tok = new StringTokenizer(ids, ";"); //$NON-NLS-1$
        types = new IOutputType[tok.countTokens()];
        ITool[] tools = getTools();
        int i = 0;
        while (tok.hasMoreElements()) {
            String id = tok.nextToken();
            for (int j = 0; j < tools.length; j++) {
                IOutputType type;
                type = tools[j].getOutputTypeById(id);
                if (type != null) {
                    types[i++] = type;
                    break;
                }
            }
        }
        return types;
    }

    @Override
    public String getTargetToolIds() {
        if (targetToolIds == null) {
            // Ask superClass for its list
            if (getSuperClass() != null) {
                return getSuperClass().getTargetToolIds();
            } else {
                return null;
            }
        }
        return targetToolIds;
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
        String ids = errorParserIds;
        if (ids == null) {
            // If I have a superClass, ask it
            if (getSuperClass() != null) {
                ids = getSuperClass().getErrorParserIds(config);
            }
        }
        if (ids == null) {
            // Collect the error parsers from my children
            if (builder != null) {
                ids = builder.getErrorParserIds();
            }
            ITool[] tools = config.getFilteredTools();
            for (int i = 0; i < tools.length; i++) {
                ITool tool = tools[i];
                String toolIds = tool.getErrorParserIds();
                if (toolIds != null && toolIds.length() > 0) {
                    if (ids != null) {
                        ids += ";"; //$NON-NLS-1$
                        ids += toolIds;
                    } else {
                        ids = toolIds;
                    }
                }
            }
        }
        return ids;
    }

    @Override
    public String[] getErrorParserList() {
        String parserIDs = getErrorParserIds();
        String[] errorParsers;
        if (parserIDs != null) {
            // Check for an empty string
            if (parserIDs.length() == 0) {
                errorParsers = new String[0];
            } else {
                StringTokenizer tok = new StringTokenizer(parserIDs, ";"); //$NON-NLS-1$
                List<String> list = new ArrayList<>(tok.countTokens());
                while (tok.hasMoreElements()) {
                    list.add(tok.nextToken());
                }
                String[] strArr = { "" }; //$NON-NLS-1$
                errorParsers = list.toArray(strArr);
            }
        } else {
            errorParsers = new String[0];
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
            ITool tools[] = info.getFilteredTools();
            set = info.contributeErrorParsers(tools, set);

            if (info.isRoot()) {
                Builder builder = (Builder) getBuilder();
                set = builder.contributeErrorParsers(set);
            }
        }
        return set;
    }

    @Override
    public String[] getArchList() {
        if (archList == null) {
            // Ask superClass for its list
            if (getSuperClass() != null) {
                return getSuperClass().getArchList();
            } else {
                // I have no superClass and no defined list
                return new String[] { "all" }; //$NON-NLS-1$
            }
        }
        return archList.toArray(new String[archList.size()]);
    }

    @Override
    public String[] getOSList() {
        if (osList == null) {
            // Ask superClass for its list
            if (getSuperClass() != null) {
                return getSuperClass().getOSList();
            }
            // I have no superClass and no defined filter list
            return new String[] { "all" }; //$NON-NLS-1$
        }
        return osList.toArray(new String[osList.size()]);
    }

    @Override
    public void setErrorParserIds(String ids) {
        String currentIds = getErrorParserIds();
        if (ids == null && currentIds == null)
            return;
        if (currentIds == null || ids == null || !(currentIds.equals(ids))) {
            errorParserIds = ids;
            isDirty = true;
        }
    }

    @Override
    public void setSecondaryOutputs(String newIds) {
        if (secondaryOutputIds == null && newIds == null)
            return;
        if (secondaryOutputIds == null || newIds == null || !newIds.equals(secondaryOutputIds)) {
            secondaryOutputIds = newIds;
            isDirty = true;
        }
    }

    @Override
    public void setTargetToolIds(String newIds) {
        if (targetToolIds == null && newIds == null)
            return;
        if (targetToolIds == null || newIds == null || !newIds.equals(targetToolIds)) {
            targetToolIds = newIds;
            isDirty = true;
        }
    }

    @Override
    public void setOSList(String[] OSs) {
        if (osList == null) {
            osList = new ArrayList<>();
        } else {
            osList.clear();
        }
        for (int i = 0; i < OSs.length; i++) {
            osList.add(OSs[i]);
        }
    }

    @Override
    public void setArchList(String[] archs) {
        if (archList == null) {
            archList = new ArrayList<>();
        } else {
            archList.clear();
        }
        for (int i = 0; i < archs.length; i++) {
            archList.add(archs[i]);
        }
    }

    @Override
    public String getDefaultLanguageSettingsProviderIds() {
        if (defaultLanguageSettingsProviderIds == null && superClass instanceof IToolChain) {
            defaultLanguageSettingsProviderIds = ((IToolChain) superClass).getDefaultLanguageSettingsProviderIds();
        }
        return defaultLanguageSettingsProviderIds;
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
        String profileId = scannerConfigDiscoveryProfileId;
        if (profileId == null) {
            profileId = ScannerDiscoveryLegacySupport.getDeprecatedLegacyProfiles(id);
            if (profileId == null) {
                IToolChain superClass = getSuperClass();
                if (superClass instanceof ToolChain) {
                    profileId = ((ToolChain) superClass).getLegacyScannerConfigDiscoveryProfileId();
                }
            }
        }
        return profileId;
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
        if (scannerConfigDiscoveryProfileId == null && superClass instanceof ToolChain) {
            return ((ToolChain) getSuperClass()).getScannerConfigDiscoveryProfileIdInternal();
        }
        return scannerConfigDiscoveryProfileId;
    }

    @Override
    public void setScannerConfigDiscoveryProfileId(String profileId) {
        if (scannerConfigDiscoveryProfileId == null && profileId == null)
            return;
        if (scannerConfigDiscoveryProfileId == null || !scannerConfigDiscoveryProfileId.equals(profileId)) {
            scannerConfigDiscoveryProfileId = profileId;
        }
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
        } else {
            if (getSuperClass() != null) {
                IToolChain superTool = getSuperClass();
                return superTool.getOptionPathConverter();
            }
        }
        return null;
    }

    /*
     *  O B J E C T   S T A T E   M A I N T E N A N C E
     */

    @Override
    public boolean isExtensionElement() {
        return isExtensionToolChain;
    }

    /**
     * Normalize the list of output extensions,for all tools in the toolchain by
     * populating the list
     * with an empty string for those tools which have no explicit output extension
     * (as defined in the
     * manifest file. In a post 2.1 manifest, all tools must have a specifed output
     * extension, even
     * if it is "")
     */
    public void normalizeOutputExtensions() {
        ITool[] tools = getTools();
        if (tools != null) {
            for (int i = 0; i < tools.length; i++) {
                ITool tool = tools[i];
                String[] extensions = tool.getOutputsAttribute();
                if (extensions == null) {
                    tool.setOutputsAttribute(""); //$NON-NLS-1$
                    continue;
                }
                if (extensions.length == 0) {
                    tool.setOutputsAttribute(""); //$NON-NLS-1$
                    continue;
                }
            }
        }
    }

    @Override
    public String getConvertToId() {
        if (convertToId == null) {
            // If I have a superClass, ask it
            if (getSuperClass() != null) {
                return getSuperClass().getConvertToId();
            } else {
                return EMPTY_STRING;
            }
        }
        return convertToId;
    }

    @Override
    public void setConvertToId(String convertToId) {
        if (convertToId == null && this.convertToId == null)
            return;
        if (convertToId == null || this.convertToId == null || !convertToId.equals(this.convertToId)) {
            this.convertToId = convertToId;
        }
        return;
    }

    @Override
    public String getVersionsSupported() {
        if (versionsSupported == null) {
            // If I have a superClass, ask it
            if (getSuperClass() != null) {
                return getSuperClass().getVersionsSupported();
            } else {
                return EMPTY_STRING;
            }
        }
        return versionsSupported;
    }

    @Override
    public void setVersionsSupported(String versionsSupported) {
        if (versionsSupported == null && this.versionsSupported == null)
            return;
        if (versionsSupported == null || this.versionsSupported == null
                || !versionsSupported.equals(this.versionsSupported)) {
            this.versionsSupported = versionsSupported;
        }
        return;
    }

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

        //        if (managedIsToolChainSupported != null) {
        //            try {
        //                return managedIsToolChainSupported.isSupported(this, null, null);
        //            } catch (Throwable e) {
        //                Activator.log(new Status(IStatus.ERROR, Activator.getId(),
        //                        "Exception in toolchain [" + getName() + "], id=" + getId(), e)); //$NON-NLS-1$ //$NON-NLS-2$
        //                return false;
        //            }
        //        }
        return true;
    }

    /**
     * Returns the plugin.xml element of the configurationEnvironmentSupplier
     * extension or <code>null</code> if none.
     *
     * @return IConfigurationElement
     */
    public IConfigurationElement getEnvironmentVariableSupplierElement() {
        if (environmentVariableSupplierElement == null) {
            if (getSuperClass() != null && getSuperClass() instanceof ToolChain) {
                return ((ToolChain) getSuperClass()).getEnvironmentVariableSupplierElement();
            }
        }
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

    //	/*
    //	 * this method is called by the UserDefinedMacroSupplier to obtain user-defined
    //	 * macros available for this tool-chain
    //	 */
    //	public StorableMacros getUserDefinedMacros(){
    //		if(isExtensionToolChain)
    //			return null;
    //
    //		if(userDefinedMacros == null)
    //			userDefinedMacros = new StorableMacros();
    //		return userDefinedMacros;
    //	}

    //	public StorableEnvironment getUserDefinedEnvironment(){
    //		if(isExtensionToolChain)
    //			return null;
    //
    //		return userDefinedEnvironment;
    //	}

    //	public void setUserDefinedEnvironment(StorableEnvironment env){
    //		if(!isExtensionToolChain)
    //			userDefinedEnvironment = env;
    //	}

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

    private void getConverter(String convertToId) {

        String fromId = null;
        String toId = null;

        // Get the Converter Extension Point
        IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(
                "org.eclipse.cdt.managedbuilder.core", //$NON-NLS-1$
                "projectConverter"); //$NON-NLS-1$
        if (extensionPoint != null) {
            // Get the extensions
            IExtension[] extensions = extensionPoint.getExtensions();
            for (int i = 0; i < extensions.length; i++) {
                // Get the configuration elements of each extension
                IConfigurationElement[] configElements = extensions[i].getConfigurationElements();
                for (int j = 0; j < configElements.length; j++) {

                    IConfigurationElement element = configElements[j];

                    if (element.getName().equals("converter")) { //$NON-NLS-1$

                        fromId = element.getAttribute("fromId"); //$NON-NLS-1$
                        toId = element.getAttribute("toId"); //$NON-NLS-1$
                        // Check whether the current converter can be used for
                        // the selected toolchain

                        if (fromId.equals(getSuperClass().getId()) && toId.equals(convertToId)) {
                            // If it matches
                            String mbsVersion = element.getAttribute("mbsVersion"); //$NON-NLS-1$
                            Version currentMbsVersion = ManagedBuildManager.getBuildInfoVersion();

                            // set the converter element based on the MbsVersion
                            if (currentMbsVersion.compareTo(new Version(mbsVersion)) > 0) {
                                previousMbsVersionConversionElement = element;
                            } else {
                                currentMbsVersionConversionElement = element;
                            }
                            return;
                        }
                    }
                }
            }
        }

        // If control comes here, it means 'Tool Integrator' specified
        // 'convertToId' attribute in toolchain definition file, but
        // has not provided any converter.
        // So, make the project is invalid

        IConfiguration parentConfig = getParent();
        IManagedProject managedProject = parentConfig.getManagedProject();
        if (managedProject != null) {
            managedProject.setValid(false);
        }
    }

    public IConfigurationElement getPreviousMbsVersionConversionElement() {
        return previousMbsVersionConversionElement;
    }

    public IConfigurationElement getCurrentMbsVersionConversionElement() {
        return currentMbsVersionConversionElement;
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
        if (isExtensionToolChain)
            return null;
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
        return getParentFolderInfo();
    }

    @Override
    public boolean matches(IToolChain tc) {
        if (tc == this)
            return true;

        IToolChain rTc = ManagedBuildManager.getRealToolChain(this);
        if (rTc == null)
            return false;

        return rTc == ManagedBuildManager.getRealToolChain(tc);
    }

    @Override
    public boolean supportsBuild(boolean managed) {
        if (!getSupportsManagedBuildAttribute())
            return !managed;

        IBuilder builder = getBuilder();
        if (builder != null && !builder.supportsBuild(managed))
            return false;

        ITool tools[] = getTools();
        for (int i = 0; i < tools.length; i++) {
            if (!tools[i].supportsBuild(managed))
                return false;
        }

        return true;
    }

    public boolean getSupportsManagedBuildAttribute() {
        if (supportsManagedBuild == null) {
            if (superClass != null) {
                return ((ToolChain) superClass).getSupportsManagedBuildAttribute();
            }
            return true;
        }
        return supportsManagedBuild.booleanValue();
    }

    @Override
    public boolean isSystemObject() {
        if (isTest)
            return true;

        if (getConvertToId().length() != 0)
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

    public IConfigurationElement getConverterModificationElement(IToolChain tc) {
        Map<String, IConfigurationElement> map = ManagedBuildManager.getConversionElements(this);
        IConfigurationElement element = null;
        if (!map.isEmpty()) {
            for (IConfigurationElement el : map.values()) {
                String toId = el.getAttribute("toId"); //$NON-NLS-1$
                IToolChain toTc = tc;
                if (toId != null) {
                    for (; toTc != null; toTc = toTc.getSuperClass()) {
                        if (toId.equals(toTc.getId()))
                            break;
                    }
                }
                if (toTc != null) {
                    element = el;
                    break;
                }
            }
        }
        return element;
    }

    public IConfigurationElement getConverterModificationElement(ITool fromTool, ITool toTool) {
        return ((Tool) fromTool).getConverterModificationElement(toTool);
    }

    void updateParentFolderInfo(FolderInfo info) {
        parentFolderInfo = info;
        config = parentFolderInfo.getParent();
    }

    private SupportedProperties findSupportedProperties() {
        if (supportedProperties == null) {
            if (superClass != null) {
                return ((ToolChain) superClass).findSupportedProperties();
            }
        }
        return supportedProperties;
    }

    private void loadProperties(IConfigurationElement el) {
        supportedProperties = new SupportedProperties(el);
    }

    void setNonInternalBuilderId(String id) {
        nonInternalBuilderId = id;
    }

    String getNonInternalBuilderId() {
        if (nonInternalBuilderId == null) {
            if (superClass != null) {
                return ((ToolChain) superClass).getNonInternalBuilderId();
            }
            return null;
        }
        return nonInternalBuilderId;
    }

    public void resetErrorParsers(FolderInfo info) {
        errorParserIds = null;
        info.resetErrorParsers(info.getFilteredTools());

        if (info.isRoot()) {
            if (builder != null) {
                builder.resetErrorParsers();
            }
        }
    }

    void removeErrorParsers(FolderInfo info, Set<String> set) {
        if (set != null && !set.isEmpty()) {
            Set<String> oldSet = contributeErrorParsers(info, null, false);
            if (oldSet == null)
                oldSet = new HashSet<>();

            oldSet.removeAll(set);
            setErrorParserList(oldSet.toArray(new String[oldSet.size()]));

            info.removeErrorParsers(info.getFilteredTools(), set);

            if (info.isRoot()) {
                Builder builder = (Builder) info.getParent().getEditableBuilder();
                builder.removeErrorParsers(set);
            }
        }
    }

    public void setErrorParserList(String[] ids) {
        if (ids == null) {
            errorParserIds = null;
        } else if (ids.length == 0) {
            errorParserIds = EMPTY_STRING;
        } else {
            StringBuilder buf = new StringBuilder();
            buf.append(ids[0]);
            for (int i = 1; i < ids.length; i++) {
                buf.append(";").append(ids[i]); //$NON-NLS-1$
            }
            errorParserIds = buf.toString();
        }
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

    void resolveProjectReferences(boolean onLoad) {
        for (Tool tool : getToolList()) {
            tool.resolveProjectReferences(onLoad);
        }
    }

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
        //        IToolChain tch = ManagedBuildManager.getRealToolChain(this);
        //        return tch != null && tch.getId().equals(ConfigurationDataProvider.PREF_TC_ID);
    }

    public boolean hasCustomSettings(ToolChain tCh) {
        if (superClass == null)
            return true;

        IToolChain realTc = ManagedBuildManager.getRealToolChain(this);
        IToolChain otherRealTc = ManagedBuildManager.getRealToolChain(tCh);
        if (realTc != otherRealTc)
            return true;

        if (hasCustomSettings())
            return true;

        ITool[] tools = getTools();
        ITool[] otherTools = tCh.getTools();
        if (tools.length != otherTools.length)
            return true;

        for (int i = 0; i < tools.length; i++) {
            Tool tool = (Tool) tools[i];
            Tool otherTool = (Tool) otherTools[i];
            if (tool.hasCustomSettings(otherTool))
                return true;
        }
        return false;
    }

    private int getSuperClassNum() {
        int num = 0;
        for (IToolChain superTool = getSuperClass(); superTool != null; superTool = superTool.getSuperClass()) {
            num++;
        }
        return num;
    }

    private String translateUnusedIdSetToString(Set<String> set) {
        return CDataUtil.arrayToString(set.toArray(), ";"); //$NON-NLS-1$
    }

}
