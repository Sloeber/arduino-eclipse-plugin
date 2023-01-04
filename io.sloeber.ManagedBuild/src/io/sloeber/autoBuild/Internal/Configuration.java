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
 * IBM - Initial API and implementation
 * James Blackburn (Broadcom Corp.)
 * Dmitry Kozlov (CodeSourcery) - Save build output preferences (bug 294106)
 * Andrew Gvozdev (Quoin Inc)   - Saving build output implemented in different way (bug 306222)
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import static io.sloeber.autoBuild.core.Messages.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.cdtvariables.CdtVariableException;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICExternalSetting;
import org.eclipse.cdt.core.settings.model.ICLibraryPathEntry;
import org.eclipse.cdt.core.settings.model.ICOutputEntry;
import org.eclipse.cdt.core.settings.model.ICSettingBase;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.core.settings.model.extension.CBuildData;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.core.settings.model.util.LanguageSettingEntriesSerializer;
import org.eclipse.cdt.core.settings.model.util.PathSettingsContainer;
import org.eclipse.cdt.utils.cdtvariables.SupplierBasedCdtVariableSubstitutor;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.util.NLS;
import org.osgi.framework.Version;

import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.autoBuild.api.BuildMacroException;
import io.sloeber.autoBuild.api.IBuildMacroProvider;
import io.sloeber.autoBuild.api.IBuildObject;
import io.sloeber.autoBuild.api.IBuilder;
import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.api.IEnvironmentVariableSupplier;
import io.sloeber.autoBuild.api.IFileInfo;
import io.sloeber.autoBuild.api.IFolderInfo;
import io.sloeber.autoBuild.api.IHoldsOptions;
import io.sloeber.autoBuild.api.IManagedCommandLineInfo;
import io.sloeber.autoBuild.api.IManagedConfigElement;
import io.sloeber.autoBuild.api.IManagedProject;
import io.sloeber.autoBuild.api.IOption;
import io.sloeber.autoBuild.api.IProjectType;
import io.sloeber.autoBuild.api.IResourceConfiguration;
import io.sloeber.autoBuild.api.IResourceInfo;
import io.sloeber.autoBuild.api.ITargetPlatform;
import io.sloeber.autoBuild.api.ITool;
import io.sloeber.autoBuild.api.IToolChain;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IConfigurationBuildMacroSupplier;
import io.sloeber.autoBuild.extensionPoint.IManagedCommandLineGenerator;
import io.sloeber.autoBuild.extensionPoint.IManagedOptionValueHandler;
import io.sloeber.autoBuild.extensionPoint.IOptionApplicability;
import io.sloeber.autoBuild.extensionPoint.IOptionCommandGenerator;
import io.sloeber.autoBuild.integration.BuildConfigurationData;
import io.sloeber.buildProperties.PropertyManager;

public class Configuration extends BuildObject implements IConfiguration {

    private static final String WHITE_SPACE = " "; //$NON-NLS-1$
    private static final String EMPTY_STRING = ""; //$NON-NLS-1$
    private static final String EMPTY_CFG_ID = "org.eclipse.cdt.build.core.emptycfg"; //$NON-NLS-1$
    private static final String LANGUAGE_SETTINGS_PROVIDER_DELIMITER = ";"; //$NON-NLS-1$
    private static final String LANGUAGE_SETTINGS_PROVIDER_NEGATION_SIGN = "-"; //$NON-NLS-1$
    private static final String $TOOLCHAIN = "${Toolchain}"; //$NON-NLS-1$

    //  Parent and children
    private IConfiguration parent;
    private ProjectType projectType;
    private ManagedProject managedProject;
    private String artifactName;
    private String cleanCommand;
    private String artifactExtension;
    private String errorParserIds;
    private String defaultLanguageSettingsProvidersAttribute;
    private String[] defaultLanguageSettingsProviderIds;
    private String prebuildStep;
    private String postbuildStep;
    private String preannouncebuildStep;
    private String postannouncebuildStep;
    private String description;
    private ICSourceEntry[] sourceEntries;
    private boolean isTest;
    private SupportedProperties supportedProperties;

    //  Miscellaneous
    private boolean isExtensionConfig = false;
    private boolean rebuildNeeded = false;
    private boolean isTemporary = false;

    private PathSettingsContainer pathSettings = PathSettingsContainer.createRootContainer();
    private ResourceInfoContainer rcInfos = new ResourceInfoContainer(pathSettings, true);
    private BooleanExpressionApplicabilityCalculator booleanExpressionCalculator;

    private FolderInfo rootFolderInfo;
    private BuildConfigurationData fCfgData;
    private ICConfigurationDescription fCfgDes;
    private ICfgScannerConfigBuilderInfo2Set cfgScannerInfo;
    private boolean isPreferenceConfig;
    private List<IPath> excludeList;

    //property name for holding the rebuild state
    private static final String REBUILD_STATE = "rebuildState"; //$NON-NLS-1$

    private static final String RC_CHANGE_STATE = "rcState"; //$NON-NLS-1$
    //resource change state
    private int resourceChangeState = -1;
	private String parentId;


    /**
     * Create an extension configuration from the project manifest file element.
     *
     * @param projectType
     *            The <code>ProjectType</code> the configuration will be added to.
     * @param element
     *            The element from the manifest that contains the configuration
     *            information.
     */
    public Configuration(ProjectType projectType, IConfigurationElement element, String managedBuildRevision) {
        this.projectType = projectType;
        isExtensionConfig = true;

        setManagedBuildRevision(managedBuildRevision);

        // Initialize from the XML attributes
        loadFromManifest(element);

        // Hook me up to the Managed Build Manager
    //    ManagedBuildManager.addExtensionConfiguration(this);

        // Hook me up to the ProjectType
        if (projectType != null) {
            projectType.addConfiguration(this);
        }


        // Load the children
        IConfigurationElement[] configElements = element.getChildren();
        List<IPath> srcPathList = new ArrayList<>();
        excludeList = new ArrayList<>();
        for (int l = 0; l < configElements.length; ++l) {
            IConfigurationElement configElement = configElements[l];
            if (configElement.getName().equals(IToolChain.TOOL_CHAIN_ELEMENT_NAME)) {
                rootFolderInfo = new FolderInfo(this, configElement, managedBuildRevision, false);
                addResourceConfiguration(rootFolderInfo);
            } else if (IFolderInfo.FOLDER_INFO_ELEMENT_NAME.equals(configElement.getName())) {
                FolderInfo resConfig = new FolderInfo(this, configElement, managedBuildRevision, true);
                addResourceConfiguration(resConfig);
            } else if (IFileInfo.FILE_INFO_ELEMENT_NAME.equals(configElement.getName())
                    || IResourceConfiguration.RESOURCE_CONFIGURATION_ELEMENT_NAME.equals(configElement.getName())) {
                ResourceConfiguration resConfig = new ResourceConfiguration(this, configElement, managedBuildRevision);
                addResourceConfiguration(resConfig);
            } else if (SourcePath.ELEMENT_NAME.equals(configElement.getName())) {
                SourcePath p = new SourcePath(configElement);
                if (p.getPath() != null)
                    srcPathList.add(p.getPath());
            } else if (configElement.getName().equals(SupportedProperties.SUPPORTED_PROPERTIES)) {
                loadProperties(configElement);
            } else if (SOURCE_ENTRIES.equals(configElement.getName())) {
                //                List<ICSettingEntry> seList = LanguageSettingEntriesSerializer
                //                        .loadEntriesList(new ManagedConfigStorageElement(configElement), ICSettingEntry.SOURCE_PATH);
                //                sourceEntries = seList.toArray(new ICSourceEntry[seList.size()]);
            }
        }

        sourceEntries = createSourceEntries(sourceEntries, srcPathList, excludeList);

        excludeList = null;

        if (rootFolderInfo == null)
            createRootFolderInfo();

        String props = element.getAttribute(BUILD_PROPERTIES);

        String optionalProps = element.getAttribute(OPTIONAL_BUILD_PROPERTIES);

        String artType = element.getAttribute(BUILD_ARTEFACT_TYPE);

    }

    private static ICSourceEntry[] createSourceEntries(ICSourceEntry[] curEntries, List<IPath> pathList,
            List<IPath> excludeList) {
        for (int i = 0; i < excludeList.size(); i++) {
            IPath path = excludeList.get(i);
            if (path.segmentCount() == 0)
                excludeList.remove(i);
        }
        if (pathList.size() == 0)
            pathList.add(Path.EMPTY);

        if (pathList.size() == 1 && pathList.get(0).equals(Path.EMPTY) && excludeList.size() == 0)
            return curEntries;

        int pathSize = pathList.size();
        Map<IPath, ICSourceEntry> map = new LinkedHashMap<>();

        for (int i = 0; i < pathSize; i++) {
            IPath path = pathList.get(i);
            ICSourceEntry entry = map.get(path);
            if (entry == null)
                entry = new CSourceEntry(path, null, ICSettingEntry.VALUE_WORKSPACE_PATH | ICSettingEntry.RESOLVED);

            entry = CDataUtil.addExcludePaths(entry, excludeList, true);
            if (entry != null)
                map.put(path, entry);
        }

        return map.values().toArray(new ICSourceEntry[map.size()]);
    }

    /**
     * Create a new extension configuration based on one already defined.
     *
     * @param projectType
     *            The <code>ProjectType</code> the configuration will be added to.
     * @param parentConfig
     *            The <code>IConfiguration</code> that is the parent configuration
     *            of this configuration
     * @param id
     *            A unique ID for the new configuration.
     */
    public Configuration(ProjectType projectType, IConfiguration parentConfig, String id) {
        setId(id);
        this.projectType = projectType;
        isExtensionConfig = true;


        if (parentConfig != null) {
            name = parentConfig.getName();
            // If this constructor is called to clone an existing
            // configuration, the parent of the parent should be stored.
            // As of 2.1, there is still one single level of inheritance to
            // worry about
            parent = parentConfig.getParent() == null ? parentConfig : parentConfig.getParent();
        }


        // Hook me up to the ProjectType
        if (projectType != null) {
            projectType.addConfiguration(this);
            // set managedBuildRevision
            setManagedBuildRevision(projectType.getManagedBuildRevision());
        }
    }

    /**
     * Create a new extension configuration and fill in the attributes and childen
     * later.
     *
     * @param projectType
     *            The <code>ProjectType</code> the configuration will be added to.
     * @param parentConfig
     *            The <code>IConfiguration</code> that is the parent configuration
     *            of this configuration
     * @param id
     *            A unique ID for the new configuration.
     * @param name
     *            A name for the new configuration.
     */
    public Configuration(ProjectType projectType, IConfiguration parentConfig, String id, String name) {
        setId(id);
        setName(name);
        this.projectType = projectType;
        parent = parentConfig;
        isExtensionConfig = true;

        // Hook me up to the ProjectType
        if (projectType != null) {
            projectType.addConfiguration(this);
            setManagedBuildRevision(projectType.getManagedBuildRevision());
        }
    }

    /**
     * Create a <code>Configuration</code> based on the specification stored in the
     * project file (.cdtbuild).
     *
     * @param managedProject
     *            The <code>ManagedProject</code> the configuration will be added
     *            to.
     * @param element
     *            The XML element that contains the configuration settings.
     *
     */
    public Configuration(ManagedProject managedProject, ICStorageElement element, String managedBuildRevision,
            boolean isPreference) {
        this.managedProject = managedProject;
        this.isPreferenceConfig = isPreference;
        isExtensionConfig = false;
        fCfgData = new BuildConfigurationData(this);

        setManagedBuildRevision(managedBuildRevision);

        // Initialize from the XML attributes
        loadFromProject(element);

        // Hook me up
        if (managedProject != null)
            managedProject.addConfiguration(this);

        ICStorageElement configElements[] = element.getChildren();
        List<IPath> srcPathList = new ArrayList<>();
        excludeList = new ArrayList<>();
        for (int i = 0; i < configElements.length; ++i) {
            ICStorageElement configElement = configElements[i];
            if (configElement.getName().equals(IToolChain.TOOL_CHAIN_ELEMENT_NAME)) {
                rootFolderInfo = new FolderInfo(this, configElement, managedBuildRevision, false);
                addResourceConfiguration(rootFolderInfo);
            } else if (IFolderInfo.FOLDER_INFO_ELEMENT_NAME.equals(configElement.getName())) {
                FolderInfo resConfig = new FolderInfo(this, configElement, managedBuildRevision, true);
                addResourceConfiguration(resConfig);
            } else if (IFileInfo.FILE_INFO_ELEMENT_NAME.equals(configElement.getName())
                    || IResourceConfiguration.RESOURCE_CONFIGURATION_ELEMENT_NAME.equals(configElement.getName())) {
                ResourceConfiguration resConfig = new ResourceConfiguration(this, configElement, managedBuildRevision);
                addResourceConfiguration(resConfig);
            } else if (SourcePath.ELEMENT_NAME.equals(configElement.getName())) {
                SourcePath p = new SourcePath(configElement);
                if (p.getPath() != null)
                    srcPathList.add(p.getPath());
            } else if (SOURCE_ENTRIES.equals(configElement.getName())) {
                List<ICSettingEntry> seList = LanguageSettingEntriesSerializer.loadEntriesList(configElement,
                        ICSettingEntry.SOURCE_PATH);
                sourceEntries = seList.toArray(new ICSourceEntry[seList.size()]);
            }
        }

        resolveProjectReferences(true);

        sourceEntries = createSourceEntries(sourceEntries, srcPathList, excludeList);

        excludeList = null;

        PropertyManager mngr = PropertyManager.getInstance();
        String rebuild = mngr.getProperty(this, REBUILD_STATE);
        if (rebuild == null || Boolean.valueOf(rebuild).booleanValue())
            rebuildNeeded = true;

        String rcChangeState = mngr.getProperty(this, RC_CHANGE_STATE);
        if (rcChangeState == null)
            resourceChangeState = ~0;
        else {
            try {
                resourceChangeState = Integer.parseInt(rcChangeState);
            } catch (NumberFormatException e) {
                resourceChangeState = ~0;
            }
        }

    }

    public Configuration(ManagedProject managedProject, IToolChain tCh, String id, String name) {
        setId(id);
        setName(name);

        //		this.description = cloneConfig.getDescription();
        this.managedProject = managedProject;
        isExtensionConfig = false;

        if (tCh == null) {
            //create configuration based upon the preference config
            IConfiguration cfg = ManagedBuildManager.getPreferenceConfiguration(false);
            if (cfg != null)
                copySettingsFrom((Configuration) cfg, true);
        } else {
            Configuration baseCfg = (Configuration) ManagedBuildManager.getExtensionConfiguration(EMPTY_CFG_ID);
            //		this.isTemporary = temporary;
            fCfgData = new BuildConfigurationData(this);

            // set managedBuildRevision
            setManagedBuildRevision(baseCfg.getManagedBuildRevision());

            //		if(!baseCfg.isExtensionConfig)
            //			cloneChildren = true;
            // If this constructor is called to clone an existing
            // configuration, the parent of the cloning config should be stored.
            parent = baseCfg.isExtensionConfig || baseCfg.getParent() == null ? baseCfg : baseCfg.getParent();

            //  Copy the remaining attributes
            projectType = baseCfg.projectType;
            artifactName = baseCfg.artifactName;
            cleanCommand = baseCfg.cleanCommand;
            artifactExtension = baseCfg.artifactExtension;
            errorParserIds = baseCfg.errorParserIds;
            prebuildStep = baseCfg.prebuildStep;
            postbuildStep = baseCfg.postbuildStep;
            preannouncebuildStep = baseCfg.preannouncebuildStep;
            postannouncebuildStep = baseCfg.postannouncebuildStep;

            if (baseCfg.sourceEntries != null)
                sourceEntries = baseCfg.sourceEntries.clone();

            defaultLanguageSettingsProvidersAttribute = baseCfg.defaultLanguageSettingsProvidersAttribute;
            if (baseCfg.defaultLanguageSettingsProviderIds != null) {
                defaultLanguageSettingsProviderIds = baseCfg.defaultLanguageSettingsProviderIds.clone();
            }

            //		enableInternalBuilder(baseCfg.isInternalBuilderEnabled());
            //		setInternalBuilderIgnoreErr(baseCfg.getInternalBuilderIgnoreErr());
            //		setInternalBuilderParallel(baseCfg.getInternalBuilderParallel());
            //		setParallelDef(baseCfg.getParallelDef());
            //		setParallelNumber(baseCfg.getParallelNumber());
            //		internalBuilderEnabled = cloneConfig.internalBuilderEnabled;
            //		internalBuilderIgnoreErr = cloneConfig.internalBuilderIgnoreErr;

            // Clone the configuration's children
            // Tool Chain

            String tcId = ManagedBuildManager.calculateChildId(tCh.getId(), null);

            IToolChain newChain = createToolChain(tCh, tcId, tCh.getName(), false);

            // For each option/option category child of the tool-chain that is
            // the child of the selected configuration element, create an option/
            // option category child of the cloned configuration's tool-chain element
            // that specifies the original tool element as its superClass.
            newChain.createOptions(tCh);

            // For each tool element child of the tool-chain that is the child of
            // the selected configuration element, create a tool element child of
            // the cloned configuration's tool-chain element that specifies the
            // original tool element as its superClass.
            String subId;
            ITool[] tools = tCh.getTools();
            for (int i = 0; i < tools.length; i++) {
                Tool toolChild = (Tool) tools[i];
                subId = ManagedBuildManager.calculateChildId(toolChild.getId(), null);
                newChain.createTool(toolChild, subId, toolChild.getName(), false);
            }

            ITargetPlatform tpBase = tCh.getTargetPlatform();
            ITargetPlatform extTp = tpBase;
            for (; extTp != null && !extTp.isExtensionElement(); extTp = extTp.getSuperClass()) {
            }

            TargetPlatform tp;
            if (extTp != null) {
                int nnn = ManagedBuildManager.getRandomNumber();
                subId = extTp.getId() + "." + nnn; //$NON-NLS-1$
                //				subName = tpBase.getName();
                tp = new TargetPlatform(newChain, subId, tpBase.getName(), (TargetPlatform) tpBase);
            } else {
                subId = ManagedBuildManager.calculateChildId(getId(), null);
                String subName = EMPTY_STRING;
                tp = new TargetPlatform((ToolChain) newChain, null, subId, subName, false);
            }

            ((ToolChain) newChain).setTargetPlatform(tp);

            //		if(cloneChildren){
            //copy expand build macros setting
            //			BuildMacroProvider macroProvider = (BuildMacroProvider)ManagedBuildManager.getBuildMacroProvider();
            //			macroProvider.expandMacrosInBuildfile(this,
            //						macroProvider.areMacrosExpandedInBuildfile(baseCfg));

            //copy user-defined build macros
            /*			UserDefinedMacroSupplier userMacros = BuildMacroProvider.fUserDefinedMacroSupplier;
            			userMacros.setMacros(
            					userMacros.getMacros(BuildMacroProvider.CONTEXT_CONFIGURATION,cloneConfig),
            					BuildMacroProvider.CONTEXT_CONFIGURATION,
            					this);
            */
            //copy user-defined environment
            //			UserDefinedEnvironmentSupplier userEnv = EnvironmentVariableProvider.fUserSupplier;
            //			userEnv.setVariables(
            //					userEnv.getVariables(cloneConfig), this);

            //		}

            // Hook me up
            managedProject.addConfiguration(this);

            IBuilder builder = getEditableBuilder();
            builder.setManagedBuildOn(false);

        }
        setRebuildState(true);
    }

    public Configuration(ManagedProject managedProject, Configuration cloneConfig, String id, boolean cloneChildren,
            boolean temporary) {
        this(managedProject, cloneConfig, id, cloneChildren, temporary, false);
    }

    /**
     * Create a new project, non-extension, configuration based on one already
     * defined.
     *
     * @param managedProject
     *            The <code>ManagedProject</code> the configuration will be added
     *            to.
     * @param cloneConfig
     *            The <code>IConfiguration</code> to copy the settings from.
     * @param id
     *            A unique ID for the new configuration.
     * @param cloneChildren
     *            If <code>true</code>, the configuration's tools are cloned
     */
    public Configuration(ManagedProject managedProject, Configuration cloneConfig, String id, boolean cloneChildren,
            boolean temporary, boolean isPreferenceConfig) {
        setId(id);
        setName(cloneConfig.getName());
        this.isPreferenceConfig = isPreferenceConfig;
        this.managedProject = managedProject;
        isExtensionConfig = false;
        this.isTemporary = temporary;

        copySettingsFrom(cloneConfig, cloneChildren);
    }

    private void copySettingsFrom(Configuration cloneConfig, boolean cloneChildren) {
        fCfgData = new BuildConfigurationData(this);


        this.description = cloneConfig.getDescription();

        // set managedBuildRevision
        setManagedBuildRevision(cloneConfig.getManagedBuildRevision());

        if (!cloneConfig.isExtensionConfig)
            cloneChildren = true;
        // If this constructor is called to clone an existing
        // configuration, the parent of the cloning config should be stored.
        parent = cloneConfig.isExtensionConfig || cloneConfig.getParent() == null ? cloneConfig
                : cloneConfig.getParent();
        parentId = parent.getId();

        //  Copy the remaining attributes
        projectType = cloneConfig.projectType;
        artifactName = cloneConfig.artifactName;
        cleanCommand = cloneConfig.cleanCommand;
        artifactExtension = cloneConfig.artifactExtension;
        errorParserIds = cloneConfig.errorParserIds;
        prebuildStep = cloneConfig.prebuildStep;
        postbuildStep = cloneConfig.postbuildStep;
        preannouncebuildStep = cloneConfig.preannouncebuildStep;
        postannouncebuildStep = cloneConfig.postannouncebuildStep;
        if (cloneConfig.sourceEntries != null) {
            sourceEntries = cloneConfig.sourceEntries.clone();
        }
        defaultLanguageSettingsProvidersAttribute = cloneConfig.defaultLanguageSettingsProvidersAttribute;
        if (cloneConfig.defaultLanguageSettingsProviderIds != null) {
            defaultLanguageSettingsProviderIds = cloneConfig.defaultLanguageSettingsProviderIds.clone();
        }

        //		enableInternalBuilder(cloneConfig.isInternalBuilderEnabled());
        //		setInternalBuilderIgnoreErr(cloneConfig.getInternalBuilderIgnoreErr());
        //		setInternalBuilderParallel(cloneConfig.getInternalBuilderParallel());
        //		setParallelDef(cloneConfig.getParallelDef());
        //		setParallelNumber(cloneConfig.getParallelNumber());
        //		internalBuilderEnabled = cloneConfig.internalBuilderEnabled;
        //		internalBuilderIgnoreErr = cloneConfig.internalBuilderIgnoreErr;

        // Clone the configuration's children
        // Tool Chain
        boolean copyIds = cloneConfig.getId().equals(id);
        String subId;
        //  Resource Configurations
        Map<IPath, Map<String, String>> toolIdMap = new HashMap<>();
        IResourceInfo infos[] = cloneConfig.rcInfos.getResourceInfos();
        for (int i = 0; i < infos.length; i++) {
            if (infos[i] instanceof FolderInfo) {
                FolderInfo folderInfo = (FolderInfo) infos[i];
                subId = copyIds ? folderInfo.getId()
                        : ManagedBuildManager.calculateChildId(getId(), folderInfo.getPath().toString());
                FolderInfo newFolderInfo = new FolderInfo(this, folderInfo, subId, toolIdMap, cloneChildren);
                addResourceConfiguration(newFolderInfo);
            } else {
                ResourceConfiguration fileInfo = (ResourceConfiguration) infos[i];
                subId = copyIds ? fileInfo.getId()
                        : ManagedBuildManager.calculateChildId(getId(), fileInfo.getPath().toString());
                ResourceConfiguration newResConfig = new ResourceConfiguration(this, fileInfo, subId, toolIdMap,
                        cloneChildren);
                addResourceConfiguration(newResConfig);

            }
        }

        resolveProjectReferences(false);

        if (cloneChildren) {
            //copy expand build macros setting
            BuildMacroProvider macroProvider = (BuildMacroProvider) ManagedBuildManager.getBuildMacroProvider();
            macroProvider.expandMacrosInBuildfile(this, macroProvider.areMacrosExpandedInBuildfile(cloneConfig));

            //copy user-defined build macros
            /*			UserDefinedMacroSupplier userMacros = BuildMacroProvider.fUserDefinedMacroSupplier;
            			userMacros.setMacros(
            					userMacros.getMacros(BuildMacroProvider.CONTEXT_CONFIGURATION,cloneConfig),
            					BuildMacroProvider.CONTEXT_CONFIGURATION,
            					this);
            */
            //copy user-defined environment
            //			UserDefinedEnvironmentSupplier userEnv = EnvironmentVariableProvider.fUserSupplier;
            //			userEnv.setVariables(
            //					userEnv.getVariables(cloneConfig), this);

        }

        // Hook me up
        if (managedProject != null) {
            managedProject.addConfiguration(this);
        }

        if (copyIds) {
            rebuildNeeded = cloneConfig.rebuildNeeded;
            resourceChangeState = cloneConfig.resourceChangeState;
        } else {
            if (cloneConfig.isExtensionConfig)
                exportArtifactInfo();
            setRebuildState(true);
        }

    }

    public void applyToManagedProject(ManagedProject mProj) {
        managedProject = mProj;
        isPreferenceConfig = false;
        isTemporary = false;
        managedProject.addConfiguration(this);
    }

    /*
     *  E L E M E N T   A T T R I B U T E   R E A D E R S   A N D   W R I T E R S
     */

    /**
     * Initialize the configuration information from an element in the
     * manifest file or provided by a dynamicElementProvider
     *
     * @param element
     *            An obejct implementing IManagedConfigElement
     */
    protected void loadFromManifest(IConfigurationElement element) {

        // id
        setId(element.getAttribute(IBuildObject.ID));

        // name
        name = element.getAttribute(IBuildObject.NAME);

        // description
        description = element.getAttribute(IConfiguration.DESCRIPTION);

        // parent
        parentId = element.getAttribute(IConfiguration.PARENT);

        //		if (parentID != null) {
        //			// Lookup the parent configuration by ID
        //			parent = ManagedBuildManager.getExtensionConfiguration(parentID);
        //		}

        // Get the name of the build artifact associated with configuration
        artifactName = element.getAttribute(ARTIFACT_NAME);

        // Get the semicolon separated list of IDs of the error parsers
        errorParserIds = element.getAttribute(ERROR_PARSERS);

        // Get the initial/default language settings providers IDs
        defaultLanguageSettingsProvidersAttribute = 
                element.getAttribute(LANGUAGE_SETTINGS_PROVIDERS);

        // Get the artifact extension
        artifactExtension = element.getAttribute(EXTENSION);

        // Get the clean command
        cleanCommand = element.getAttribute(CLEAN_COMMAND);

        // Get the pre-build and post-build commands
        prebuildStep = element.getAttribute(PREBUILD_STEP);
        postbuildStep = element.getAttribute(POSTBUILD_STEP);

        // Get the pre-build and post-build announcements
        preannouncebuildStep = element.getAttribute(PREANNOUNCEBUILD_STEP);
        postannouncebuildStep = element.getAttribute(POSTANNOUNCEBUILD_STEP);

        String tmp = element.getAttribute(IS_SYSTEM);
        if (tmp != null)
            isTest = Boolean.valueOf(tmp).booleanValue();
    }

    /**
     * Initialize the configuration information from the XML element
     * specified in the argument
     *
     * @param element
     *            An XML element containing the configuration information
     */
    protected void loadFromProject(ICStorageElement element) {

        // id
        // note: IDs are unique so no benefit to intern them
        setId(element.getAttribute(IBuildObject.ID));

        // name
        if (element.getAttribute(IBuildObject.NAME) != null)
            setName(element.getAttribute(IBuildObject.NAME));

        // description
        if (element.getAttribute(IConfiguration.DESCRIPTION) != null)
            description = element.getAttribute(IConfiguration.DESCRIPTION);

        String props = element.getAttribute(BUILD_PROPERTIES);

        String optionalProps = element.getAttribute(OPTIONAL_BUILD_PROPERTIES);

        String artType = element.getAttribute(BUILD_ARTEFACT_TYPE);


        if (element.getAttribute(IConfiguration.PARENT) != null) {
            // See if the parent belongs to the same project
            if (managedProject != null)
                parent = managedProject.getConfiguration(element.getAttribute(IConfiguration.PARENT));
            // If not, then try the extension configurations
            if (parent == null) {
                parent = ManagedBuildManager.getExtensionConfiguration(element.getAttribute(IConfiguration.PARENT));
                if (parent == null) {
                    String message = NLS.bind(Configuration_orphaned, getId(), //$NON-NLS-1$
                            element.getAttribute(IConfiguration.PARENT));
                    Activator.error(message);
                }
            }
        }

        // Get the name of the build artifact associated with target (usually
        // in the plugin specification).
        if (element.getAttribute(ARTIFACT_NAME) != null) {
            artifactName = element.getAttribute(ARTIFACT_NAME);
        }

        // Get the semicolon separated list of IDs of the error parsers
        if (element.getAttribute(ERROR_PARSERS) != null) {
            errorParserIds = element.getAttribute(ERROR_PARSERS);
        }

        // Get the artifact extension
        if (element.getAttribute(EXTENSION) != null) {
            artifactExtension = element.getAttribute(EXTENSION);
        }

        // Get the clean command
        if (element.getAttribute(CLEAN_COMMAND) != null) {
            cleanCommand = element.getAttribute(CLEAN_COMMAND);
        }

        // Get the pre-build and post-build commands
        if (element.getAttribute(PREBUILD_STEP) != null) {
            prebuildStep = element.getAttribute(PREBUILD_STEP);
        }

        if (element.getAttribute(POSTBUILD_STEP) != null) {
            postbuildStep = element.getAttribute(POSTBUILD_STEP);
        }

        // Get the pre-build and post-build announcements
        if (element.getAttribute(PREANNOUNCEBUILD_STEP) != null) {
            preannouncebuildStep = element.getAttribute(PREANNOUNCEBUILD_STEP);
        }

        if (element.getAttribute(POSTANNOUNCEBUILD_STEP) != null) {
            postannouncebuildStep = element.getAttribute(POSTANNOUNCEBUILD_STEP);
        }
    }

    /**
     * Persist this configuration to project file.
     */
    public void serialize(ICStorageElement element) {
        element.setAttribute(IBuildObject.ID, id);

        if (name != null)
            element.setAttribute(IBuildObject.NAME, name);

        if (description != null)
            element.setAttribute(IConfiguration.DESCRIPTION, description);


        if (parent != null)
            element.setAttribute(IConfiguration.PARENT, parent.getId());

        if (artifactName != null)
            element.setAttribute(ARTIFACT_NAME, artifactName);

        if (errorParserIds != null)
            element.setAttribute(ERROR_PARSERS, errorParserIds);

        if (artifactExtension != null)
            element.setAttribute(EXTENSION, artifactExtension);

        if (cleanCommand != null)
            element.setAttribute(CLEAN_COMMAND, cleanCommand);

        if (prebuildStep != null)
            element.setAttribute(PREBUILD_STEP, prebuildStep);

        if (postbuildStep != null)
            element.setAttribute(POSTBUILD_STEP, postbuildStep);

        if (preannouncebuildStep != null)
            element.setAttribute(PREANNOUNCEBUILD_STEP, preannouncebuildStep);

        if (postannouncebuildStep != null)
            element.setAttribute(POSTANNOUNCEBUILD_STEP, postannouncebuildStep);

        // Serialize my children
        IResourceInfo infos[] = rcInfos.getResourceInfos();
        for (int i = 0; i < infos.length; i++) {
            String elementName = infos[i].getKind() == ICSettingBase.SETTING_FILE ? IFileInfo.FILE_INFO_ELEMENT_NAME
                    : IFolderInfo.FOLDER_INFO_ELEMENT_NAME;

            ICStorageElement resElement = element.createChild(elementName);
            ((ResourceInfo) infos[i]).serialize(resElement);
        }

        PropertyManager.getInstance().serialize(this);

        if (sourceEntries != null && sourceEntries.length > 0) {
            ICStorageElement el = element.createChild(SOURCE_ENTRIES);
            LanguageSettingEntriesSerializer.serializeEntries(sourceEntries, el);
        }
    }

    /*
     *  P A R E N T   A N D   C H I L D   H A N D L I N G
     */

    @Override
    public IConfiguration getParent() {
        return parent;
    }

    @Override
    public IResource getOwner() {
        if (managedProject != null)
            return managedProject.getOwner();
        else {
            return null; // Extension configurations don't have an "owner"
        }
    }

    //@Override
    public IProjectType getProjectType() {
        return projectType;
    }

    @Override
    public IManagedProject getManagedProject() {
        return (IManagedProject) managedProject;
    }

    @Override
    public IToolChain createToolChain(IToolChain superClass, String Id, String name, boolean isExtensionElement) {
        if (rootFolderInfo == null) {
            createRootFolderInfo();
        }

        return rootFolderInfo.createToolChain(superClass, Id, name, isExtensionElement);
    }

    private IFolderInfo createRootFolderInfo() {
        String id = ManagedBuildManager.calculateChildId(this.id, null);
        String name = "/"; //$NON-NLS-1$

        rootFolderInfo = new FolderInfo(this, new Path(name), id, name, isExtensionConfig);
        addResourceConfiguration(rootFolderInfo);
        return rootFolderInfo;
    }

    /*
    	public IFolderInfo createFolderInfo(IPath path, IToolChain superClass, String Id, String name){
    
    	}
    
    	public IFolderInfo createFolderInfo(IPath path, IFolderInfo baseFolderInfo, String Id, String name){
    
    	}
    */
    @Override
    public IToolChain getToolChain() {
        return rootFolderInfo.getToolChain();
    }

    @Override
    public IResourceConfiguration[] getResourceConfigurations() {
        return (IResourceConfiguration[]) rcInfos.getResourceInfos(ICSettingBase.SETTING_FILE,
                IResourceConfiguration.class);
    }

    @Override
    public IResourceConfiguration getResourceConfiguration(String resPath) {
        return rcInfos.getFileInfo(new Path(resPath).removeFirstSegments(1));
    }

    @Override
    public ITool[] getFilteredTools() {
        return rootFolderInfo.getFilteredTools();
    }

    @Override
    public ITool[] getTools() {
        return rootFolderInfo.getTools();
    }

    @Override
    public ITool getTool(String id) {
        return rootFolderInfo.getTool(id);
    }

    @Override
    public ITool[] getToolsBySuperClassId(String id) {
        return rootFolderInfo.getToolsBySuperClassId(id);
    }

    @Override
    public ITool getTargetTool() {
        String[] targetToolIds = rootFolderInfo.getToolChain().getTargetToolList();
        if (targetToolIds == null || targetToolIds.length == 0)
            return null;

        //  For each target tool id, in list order,
        //  look for a tool with this ID, or a tool with a superclass with this id.
        //  Stop when we find a match
        ITool[] tools = getFilteredTools();
        for (int i = 0; i < targetToolIds.length; i++) {
            String targetToolId = targetToolIds[i];
            for (int j = 0; j < tools.length; j++) {
                ITool targetTool = tools[j];
                ITool tool = targetTool;
                do {
                    if (targetToolId.equals(tool.getId())) {
                        return targetTool;
                    }
                    tool = tool.getSuperClass();
                } while (tool != null);
            }
        }
        return null;
    }

    @Override
    public String getToolCommand(ITool tool) {
        // TODO:  Do we need to verify that the tool is part of the configuration?
        return tool.getToolCommand();
    }


    @Override
    public IOption setOption(IHoldsOptions holder, IOption option, boolean value) throws BuildException {
        return getRootFolderInfo().setOption(holder, option, value);
    }

    @Override
    public IOption setOption(IHoldsOptions holder, IOption option, String value) throws BuildException {
        return getRootFolderInfo().setOption(holder, option, value);
    }

    @Override
    public IOption setOption(IHoldsOptions holder, IOption option, String[] value) throws BuildException {
        return getRootFolderInfo().setOption(holder, option, value);
    }

    /**
     * Adds the Resource Configuration to the Resource Configuration list and map
     */
    void addResourceConfiguration(IResourceInfo resConfig) {
        if (resConfig.getPath().segmentCount() == 0)
            rootFolderInfo = (FolderInfo) resConfig;
        rcInfos.addResourceInfo(resConfig);
        //		rebuildNeeded = true;
    }

    //@Override
    public void removeResourceConfiguration(IResourceInfo resConfig) {
        ManagedBuildManager.performValueHandlerEvent(resConfig, IManagedOptionValueHandler.EVENT_CLOSE);
        ITool tools[] = resConfig.getTools();
        rcInfos.removeResourceInfo(resConfig.getPath());
        ((ResourceInfo) resConfig).removed();
        BuildSettingsUtil.disconnectDepentents(this, tools);
        rebuildNeeded = true;
    }
    /*
     *  M O D E L   A T T R I B U T E   A C C E S S O R S
     */

    @Override
    public String getName() {
        return (name == null && parent != null) ? parent.getName() : name;
    }

    @Override
    public String getArtifactExtension() {
        String ext = getArtifactExtensionAttribute(true);
        return ext != null ? ext : EMPTY_STRING;
    }

    public String getArtifactExtensionAttribute(boolean querySuperClass) {
        if (artifactExtension == null) {
            // Ask my parent first
            if (parent != null) {
                return parent.getArtifactExtension();
            }
            return null;
        }
        return artifactExtension;
    }

    @Override
    public String getArtifactName() {
        if (artifactName == null) {
            // If I have a parent, ask it
            if (parent != null) {
                return parent.getArtifactName();
            } else {
                // I'm it and this is not good!
                return EMPTY_STRING;
            }
        } else {
            return artifactName;
        }
    }

    @Override
    public String getBuildArguments() {
        IToolChain tc = getToolChain();
        IBuilder builder = tc.getBuilder();
        if (builder != null) {
            return builder.getArguments();
        }
        return "-k"; //$NON-NLS-1$
    }

    @Override
    public String getBuildCommand() {
        IToolChain tc = getToolChain();
        IBuilder builder = tc.getBuilder();
        if (builder != null) {
            return builder.getCommand();
        }
        return "make"; //$NON-NLS-1$
    }

    @Override
    public String getPrebuildStep() {
        if (prebuildStep == null) {
            // If I have a parent, ask it
            if (parent != null) {
                return parent.getPrebuildStep();
            } else {
                // I'm it
                return EMPTY_STRING;
            }
        } else {
            return prebuildStep;
        }
    }

    @Override
    public String getPostbuildStep() {
        if (postbuildStep == null) {
            // If I have a parent, ask it
            if (parent != null) {
                return parent.getPostbuildStep();
            } else {
                // I'm it
                return EMPTY_STRING;
            }
        } else {
            return postbuildStep;
        }
    }

    @Override
    public String getPreannouncebuildStep() {
        if (preannouncebuildStep == null) {
            // If I have a parent, ask it
            if (parent != null) {
                return parent.getPreannouncebuildStep();
            } else {
                // I'm it
                return EMPTY_STRING;
            }
        } else {
            return preannouncebuildStep;
        }
    }

    @Override
    public String getPostannouncebuildStep() {
        if (postannouncebuildStep == null) {
            // If I have a parent, ask it
            if (parent != null) {
                return parent.getPostannouncebuildStep();
            } else {
                // I'm it
                return EMPTY_STRING;
            }
        } else {
            return postannouncebuildStep;
        }
    }

    @Override
    public String getCleanCommand() {
        // Return the command used to remove files
        if (cleanCommand == null) {
            if (parent != null) {
                return parent.getCleanCommand();
            } else {
                // User forgot to specify it. Guess based on OS.
                if (Platform.getOS().equals(Platform.OS_WIN32)) {
                    return "del"; //$NON-NLS-1$
                } else {
                    return "rm"; //$NON-NLS-1$
                }
            }
        } else {
            // This was spec'd in the manifest
            return cleanCommand;
        }
    }

    @Override
    public String getDescription() {
        if (description == null) {
            // If I have a parent, ask it
            if (parent != null) {
                return parent.getDescription();
            } else {
                // I'm it
                return EMPTY_STRING;
            }
        } else {
            return description;
        }
    }

    @Override
    public String getErrorParserIds() {
        if (errorParserIds != null) {
            return errorParserIds;
        }
        // If I have a parent, ask it
        String errorParsers = null;
        if (parent != null) {
            errorParsers = parent.getErrorParserIds();
        }
        // If no error parsers are specified by the configuration, the default
        // is
        // the error parsers from the tool-chain
        //TODO
        if (errorParsers == null && rootFolderInfo != null) {
            errorParsers = rootFolderInfo.getErrorParserIds();
        }
        return errorParsers;
    }

    public String getErrorParserIdsAttribute() {
        if (errorParserIds != null) {
            return errorParserIds;
        }
        // If I have a parent, ask it
        String errorParsers = null;
        if (parent != null) {
            errorParsers = ((Configuration) parent).getErrorParserIdsAttribute();
        }

        return errorParsers;
    }

    @Override
    public String[] getErrorParserList() {
        Set<String> set = contributeErrorParsers(null, true);
        if (set != null) {
            String result[] = new String[set.size()];
            set.toArray(result);
            return result;
        }
        return ErrorParserManager.getErrorParserAvailableIdsInContext(ErrorParserManager.BUILD_CONTEXT);
    }

    public Set<String> contributeErrorParsers(Set<String> set, boolean includeChildren) {
        String parserIDs = getErrorParserIdsAttribute();
        if (parserIDs != null) {
            if (set == null)
                set = new LinkedHashSet<>();
            if (parserIDs.length() != 0) {
                StringTokenizer tok = new StringTokenizer(parserIDs, ";"); //$NON-NLS-1$
                while (tok.hasMoreElements()) {
                    set.add(tok.nextToken());
                }
            }
        }

        if (includeChildren) {
            IResourceInfo[] rcInfos = getResourceInfos();
            for (int i = 0; i < rcInfos.length; i++) {
                ResourceInfo rcInfo = (ResourceInfo) rcInfos[i];
                set = rcInfo.contributeErrorParsers(set);
            }
        }
        return set;
    }

    /**
     * Get value of attribute {@link IConfiguration#LANGUAGE_SETTINGS_PROVIDERS}
     * It not defined, it will try to pull the attribute from the parent
     * configuration.
     */
    private String getDefaultLanguageSettingsProvidersAttribute() {
        if (defaultLanguageSettingsProvidersAttribute == null && parent instanceof Configuration) {
            defaultLanguageSettingsProvidersAttribute = ((Configuration) parent)
                    .getDefaultLanguageSettingsProvidersAttribute();
        }

        return defaultLanguageSettingsProvidersAttribute;
    }

    /**
     * {@inheritDoc}
     *
     * This function will try to find default provider Ids specified in this
     * instance.
     * It none defined, it will try to pull Ids from the parent configuration.
     */
    @Override
    public String[] getDefaultLanguageSettingsProviderIds() {
        if (defaultLanguageSettingsProviderIds == null) {
            defaultLanguageSettingsProvidersAttribute = getDefaultLanguageSettingsProvidersAttribute();
            if (defaultLanguageSettingsProvidersAttribute != null) {
                List<String> ids = new ArrayList<>();
                String[] defaultIds = defaultLanguageSettingsProvidersAttribute
                        .split(LANGUAGE_SETTINGS_PROVIDER_DELIMITER);
                for (String id : defaultIds) {
                    if (id != null && !id.isEmpty()) {
                        if (id.startsWith(LANGUAGE_SETTINGS_PROVIDER_NEGATION_SIGN)) {
                            id = id.substring(1);
                            ids.remove(id);
                        } else if (!ids.contains(id)) {
                            if (id.contains($TOOLCHAIN)) {
                                IToolChain toolchain = getToolChain();
                                if (toolchain != null) {
                                    String toolchainProvidersIds = toolchain.getDefaultLanguageSettingsProviderIds();
                                    if (toolchainProvidersIds != null) {
                                        ids.addAll(Arrays.asList(
                                                toolchainProvidersIds.split(LANGUAGE_SETTINGS_PROVIDER_DELIMITER)));
                                    }
                                }
                            } else {
                                ids.add(id);
                            }
                        }
                    }

                }
                defaultLanguageSettingsProviderIds = ids.toArray(new String[ids.size()]);
            } else if (parent != null) {
                defaultLanguageSettingsProviderIds = parent.getDefaultLanguageSettingsProviderIds();
            }
        }

        return defaultLanguageSettingsProviderIds;
    }

    @Override
    public void setArtifactExtension(String extension) {
        if (extension == null && artifactExtension == null)
            return;
        if (artifactExtension == null || extension == null || !artifactExtension.equals(extension)) {
            artifactExtension = extension;
            //			rebuildNeeded = true;
            if (!isExtensionElement()) {
                ITool tool = calculateTargetTool();
                if (tool != null) {
                    tool.setRebuildState(true);
                } else {
                    setRebuildState(true);
                }
            }
        }
    }

    @Override
    public void setArtifactName(String name) {
    	//TOFIX JABA should be deleted
            artifactName = name;
         
    }

    @Override
    public void setErrorParserIds(String ids) {
        String currentIds = getErrorParserIds();
        if (ids == null && currentIds == null)
            return;
        if (currentIds == null || ids == null || !(currentIds.equals(ids))) {
            errorParserIds = ids;
        }
    }

    @Override
    public void setCleanCommand(String command) {
        if (command == null && cleanCommand == null)
            return;
        if (cleanCommand == null || command == null || !cleanCommand.equals(command)) {
            cleanCommand = command;
        }
    }

    @Override
    public void setDescription(String description) {
        if (description == null && this.description == null)
            return;
        if (this.description == null || description == null || !description.equals(this.description)) {
            this.description = description;
        }
    }

    @Override
    public void setBuildArguments(String makeArgs) {
        IToolChain tc = getToolChain();
        IBuilder builder = tc.getBuilder();
        if (makeArgs == null) { //resetting the build arguments
            if (!builder.isExtensionElement()) {
                builder.setArguments(makeArgs);
                //				rebuildNeeded = true;
            }
        } else if (!makeArgs.equals(builder.getArguments())) {
            if (builder.isExtensionElement()) {
                String subId = ManagedBuildManager.calculateChildId(builder.getId(), null);
                String builderName = builder.getName() + "." + getName(); //$NON-NLS-1$
                builder = getToolChain().createBuilder(builder, subId, builderName, false);
            }
            builder.setArguments(makeArgs);
            //			rebuildNeeded = true;
        }
    }

    @Override
    public void setBuildCommand(String command) {
        IToolChain tc = getToolChain();
        IBuilder builder = tc.getBuilder();
        if (command == null) { //resetting the build command
            if (!builder.isExtensionElement()) {
                builder.setCommand(command);
                //				rebuildNeeded = true;
            }
        } else if (!command.equals(builder.getCommand())) {
            if (builder.isExtensionElement()) {
                String subId = ManagedBuildManager.calculateChildId(builder.getId(), null);
                String builderName = builder.getName() + "." + getName(); //$NON-NLS-1$
                builder = getToolChain().createBuilder(builder, subId, builderName, false);
            }
            builder.setCommand(command);
            //			rebuildNeeded = true;
        }
    }

    @Override
    public void setPrebuildStep(String step) {
        String currentPrebuildStep = getPrebuildStep();
        if (step == null && prebuildStep == null)
            return;
        if (currentPrebuildStep == null || step == null || !currentPrebuildStep.equals(step)) {
            prebuildStep = step;
        }
    }

    @Override
    public void setPostbuildStep(String step) {
        String currentPostbuildStep = getPostbuildStep();
        if (step == null && postbuildStep == null)
            return;
        if (currentPostbuildStep == null || step == null || !currentPostbuildStep.equals(step)) {
            postbuildStep = step;
        }
    }

    @Override
    public void setPreannouncebuildStep(String announceStep) {
        String currentPreannouncebuildStep = getPreannouncebuildStep();
        if (announceStep == null && preannouncebuildStep == null)
            return;
        if (currentPreannouncebuildStep == null || announceStep == null
                || !currentPreannouncebuildStep.equals(announceStep)) {
            preannouncebuildStep = announceStep;
            //    		rebuildNeeded = true;
        }
    }

    @Override
    public void setPostannouncebuildStep(String announceStep) {
        String currentAnnounceStep = getPostannouncebuildStep();
        if (announceStep == null && postannouncebuildStep == null)
            return;
        if (currentAnnounceStep == null || announceStep == null || !currentAnnounceStep.equals(announceStep)) {
            postannouncebuildStep = announceStep;
        }
    }

    @Override
    public boolean isSupported() {
        IFolderInfo foInfo = getRootFolderInfo();
        if (foInfo != null)
            return foInfo.isSupported();
        return false;
    }

    @Override
    public boolean isHeaderFile(String ext) {
        return getRootFolderInfo().isHeaderFile(ext);
    }

    /*
     *  O B J E C T   S T A T E   M A I N T E N A N C E
     */

    @Override
    public boolean isExtensionElement() {
        return isExtensionConfig;
    }


    @Override
    public boolean needsRebuild() {
        return needsRebuild(true);
    }

    @Override
    public boolean needsFullRebuild() {
        return needsRebuild(false);
    }

    public boolean needsRebuild(boolean checkChildren) {
        boolean needRebuild = rebuildNeeded || resourceChangesRequireRebuild();

        if (needRebuild || !checkChildren)
            return needRebuild;

        IResourceInfo infos[] = rcInfos.getResourceInfos();

        for (int i = 0; i < infos.length; i++) {
            if (infos[i].needsRebuild())
                return true;
        }

        return false;
    }



    @Override
    public void setRebuildState(boolean rebuild) {
        if (isExtensionElement() && rebuild)
            return;

        if (rebuildNeeded != rebuild) {
            rebuildNeeded = rebuild;
            saveRebuildState();
        }

        if (!rebuildNeeded) {
            setResourceChangeState(0);

            IResourceInfo infos[] = rcInfos.getResourceInfos();

            for (int i = 0; i < infos.length; i++) {
                infos[i].setRebuildState(false);
            }
        }
    }

    @Override
    public boolean hasOverriddenBuildCommand() {
        IBuilder builder = getToolChain().getBuilder();
        if (builder != null) {
            IBuilder superB = builder.getSuperClass();
            if (superB != null) {
                String command = builder.getCommand();
                if (command != null) {
                    String superC = superB.getCommand();
                    if (superC != null) {
                        if (!command.equals(superC)) {
                            return true;
                        }
                    }
                }
                String args = builder.getArguments();
                if (args != null) {
                    String superA = superB.getArguments();
                    if (superA != null) {
                        if (!args.equals(superA)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void resolveReferences() {
        //        if (!resolved) {
        //            resolved = true;
        //
        //            // call resolve references on any children
        //            ResourceInfo infos[] = (ResourceInfo[]) rcInfos.getResourceInfos(ResourceInfo.class);
        //
        //            for (int i = 0; i < infos.length; i++) {
        //                infos[i].resolveReferences();
        //            }
        //
        //            if (parentId != null) {
        //                // Lookup the parent configuration by ID
        //                parent = ManagedBuildManager.getExtensionConfiguration(parentId);
        //            }
        //
        //        }
    }

    /**
     * Reset the configuration's, tools', options
     */
    public void reset() {
        ((FolderInfo) getRootFolderInfo()).resetOptionSettings();
    }

    /**
     * Create a resource configuration object for the passed-in file
     */
    @Override
    public IResourceConfiguration createResourceConfiguration(IFile file) {
        return createFileInfo(file.getFullPath().removeFirstSegments(1));

    }

    @Override
    public IFileInfo createFileInfo(IPath path) {
        String resourceName = path.lastSegment();
        String id = ManagedBuildManager.calculateChildId(getId(), path.toString());
        return createFileInfo(path, id, resourceName);
    }

    @Override
    public IFileInfo createFileInfo(IPath path, String id, String name) {
        IResourceInfo info = getResourceInfo(path, false);
        IFileInfo fileInfo = null;
        if (info instanceof IFileInfo) {
            fileInfo = (IFileInfo) info;
        } else if (info instanceof IFolderInfo) {
            IFolderInfo base = (IFolderInfo) info;
            fileInfo = createFileInfo(path, base, null, id, name);
        }
        return fileInfo;
    }

    @Override
    public IFileInfo createFileInfo(IPath path, IFolderInfo base, ITool baseTool, String id, String name) {
        if (base.getPath().equals(path))
            return null;

        IFileInfo fileInfo = new ResourceConfiguration((FolderInfo) base, baseTool, id, name, path);
        addResourceConfiguration(fileInfo);
        ManagedBuildManager.performValueHandlerEvent(fileInfo, IManagedOptionValueHandler.EVENT_OPEN);

        return fileInfo;
    }

    @Override
    public IFileInfo createFileInfo(IPath path, IFileInfo base, String id, String name) {
        if (base.getPath().equals(path))
            return null;

        IFileInfo fileInfo = new ResourceConfiguration((ResourceConfiguration) base, path, id, name);
        addResourceConfiguration(fileInfo);
        ManagedBuildManager.performValueHandlerEvent(fileInfo, IManagedOptionValueHandler.EVENT_OPEN);

        return fileInfo;
    }

    @Override
    public IEnvironmentVariableSupplier getEnvironmentVariableSupplier() {
        IToolChain toolChain = getToolChain();
        if (toolChain != null)
            return toolChain.getEnvironmentVariableSupplier();
        return null;
    }

    /**
     * @return Returns the version.
     */
    @Override
    public Version getVersion() {
        if (version == null) {
            if (rootFolderInfo.getToolChain() != null) {
                return rootFolderInfo.getToolChain().getVersion();
            }
        }
        return version;
    }

    @Override
    public void setVersion(Version version) {
        // Do nothing
    }

    @Override
    public IConfigurationBuildMacroSupplier getBuildMacroSupplier() {
        IToolChain toolChain = getToolChain();
        if (toolChain != null)
            return toolChain.getBuildMacroSupplier();
        return null;

    }

    @Override
    public boolean isTemporary() {
        return isTemporary;
    }

    @Override
    public void updateManagedBuildRevision(String revision) {
        super.updateManagedBuildRevision(revision);

        ResourceInfo infos[] = (ResourceInfo[]) rcInfos.getResourceInfos(ResourceInfo.class);

        for (int i = 0; i < infos.length; i++) {
            infos[i].updateManagedBuildRevision(revision);
        }
    }


    @Override
    public ITool calculateTargetTool() {
        ITool tool = getTargetTool();

        if (tool == null) {
            tool = getToolFromOutputExtension(getArtifactExtension());
        }

        if (tool == null) {
            IConfiguration extCfg;
            for (extCfg = this; extCfg != null && !extCfg.isExtensionElement(); extCfg = extCfg.getParent()) {
            }

            if (extCfg != null) {
                tool = getToolFromOutputExtension(extCfg.getArtifactExtension());
            }
        }

        return tool;
    }

    @Override
    public ITool getToolFromOutputExtension(String extension) {
        return getRootFolderInfo().getToolFromOutputExtension(extension);
    }

    @Override
    public ITool getToolFromInputExtension(String sourceExtension) {
        return getRootFolderInfo().getToolFromInputExtension(sourceExtension);
    }

    /**
     * The resource delta passed to the builder is not always up-to-date
     * for the given configuration because between two builds of the same
     * configuration
     * any number of other configuration builds may occur
     * that is why we need to keep some information regarding what happened
     * with the resource tree between the two configuration builds
     *
     * The trivial approach implemented currently is to hold
     * the general information of whether some resources were
     * removed,changed,etc. and detect whether the rebuild is needed
     * based upon this information
     *
     * This method adds the resource change state for the configuration
     * specifying the resource change type performed on the project
     * reported while building another configuration
     * The method is not exported to the public API since delta handling
     * mechanism will likely to be changed in the future
     *
     * In the future we might implement some more smart mechanism
     * for tracking delta, e.g calculate the pre-cinfiguration resource delta, etc.
     *
     */
    @Override
    public void addResourceChangeState(int state) {
        setResourceChangeState(state | resourceChangeState);
    }

    private void setResourceChangeState(int state) {
        if (resourceChangeState != state) {
            resourceChangeState = state;
            saveResourceChangeState();
        }
    }

    private boolean resourceChangesRequireRebuild() {
        return isInternalBuilderEnabled() ? resourceChangeState != 0
                : (resourceChangeState & IResourceDelta.REMOVED) == IResourceDelta.REMOVED;
    }

    private void saveRebuildState() {
        PropertyManager.getInstance().setProperty(this, REBUILD_STATE, Boolean.toString(rebuildNeeded));
    }

    private void saveResourceChangeState() {
        PropertyManager.getInstance().setProperty(this, RC_CHANGE_STATE, Integer.toString(resourceChangeState));
    }

    /*
     * Internal Builder state API
     * NOTE: this is a temporary API
     * In the future we are going present the Internal Builder
     * as a special Builder object of the tool-chain and implement the internal
     * builder enabling/disabling as the Builder substitution functionality
     *
     */

    /*	public void setInternalBuilderBoolean(boolean value, String pref) {
    		Preferences prefs = getPreferences(INTERNAL_BUILDER);
    		if(prefs != null){
    			prefs.putBoolean(pref, value);
    			try {
    				prefs.flush();
    			} catch (BackingStoreException e) {}
    		}
    	}
    */
    /*	public boolean getInternalBuilderBoolean(String pref, boolean defaultValue) {
    		Preferences prefs = getPreferences(INTERNAL_BUILDER);
    		return prefs != null ?
    				prefs.getBoolean(pref, false) : defaultValue;
    	}
    */
    /**
     * this method is used for enabling/disabling the internal builder
     * for the given configuration
     *
     * @param enable
     *            boolean
     */
    public void enableInternalBuilder(boolean enable) {
        if (enable == isInternalBuilderEnabled())
            return;

        IBuilder builder = getBuilderForInternalBuilderEnablement(enable, true);
        if (builder != null) {
            if (enable) {
                savePrevBuilderId(getBuilder());
            }

            changeBuilder(builder, ManagedBuildManager.calculateChildId(builder.getId(), null), builder.getName(),
                    true);

            if (enable) {
                try {
                    setManagedBuildOn(true);
                } catch (BuildException e) {
                }
            }
        }
    }

    public boolean canEnableInternalBuilder(boolean enable) {
        return getBuilderForInternalBuilderEnablement(enable, true) != null;
    }

    private IBuilder getBuilderForInternalBuilderEnablement(boolean enable, boolean checkCompatibility) {
        IBuilder newBuilder = null;
        if (enable) {
            if (supportsBuild(true, false)) {
                IBuilder b = ManagedBuildManager.getInternalBuilder();
                if (b != null) {
                    if (!checkCompatibility || isBuilderCompatible(b))
                        newBuilder = b;
                }
            }
        } else {
            String id = getPrevBuilderId();
            if (id != null) {
                IBuilder b = ManagedBuildManager.getExtensionBuilder(id);
                if (b != null) {
                    if (!checkCompatibility || isBuilderCompatible(b))
                        newBuilder = b;
                }
            }
            if (newBuilder == null) {
                for (IToolChain tc = getToolChain(); tc != null; tc = tc.getSuperClass()) {
                    IBuilder b = tc.getBuilder();
                    if (b.isInternalBuilder())
                        continue;

                    for (; b != null && !b.isExtensionElement(); b = b.getSuperClass()) {
                    }

                    if (b != null) {
                        if (!checkCompatibility || isBuilderCompatible(b)) {
                            newBuilder = b;
                            break;
                        }
                    }
                }
            }

            //			if(newBuilder == null){
            //				IBuilder builders[] = ManagedBuildManager.getRealBuilders();
            //				IBuilder tmpB = null;
            //				for(int i = 0; i < builders.length; i++){
            //					IBuilder b = builders[i];
            //					if(b.isInternalBuilder())
            //						continue;
            //
            //
            //					if(isBuilderCompatible(b)){
            //						newBuilder = b;
            //						break;
            //					} else if(!checkCompatibility){
            //						tmpB = b;
            //					}
            //				}
            //
            //				if(newBuilder == null){
            //					if(tmpB != null)
            //						newBuilder = tmpB;
            //				}
            //			}

        }

        return newBuilder;
    }

    private void savePrevBuilderId(IBuilder builder) {
        IBuilder b = builder;
        for (; b != null && !b.isExtensionElement(); b = b.getSuperClass()) {
        }

        if (b == null)
            b = builder;

        ToolChain tc = (ToolChain) getToolChain();
        if (tc != null)
            tc.setNonInternalBuilderId(b.getId());
    }

    private String getPrevBuilderId() {
        ToolChain tc = (ToolChain) getToolChain();
        if (tc != null)
            return tc.getNonInternalBuilderId();
        return null;
    }

    /**
     * returns whether the internal builder is enabled
     * 
     * @return boolean
     */
    public boolean isInternalBuilderEnabled() {
        return getBuilder().isInternalBuilder();
    }

    /**
     *
     * sets the Internal Builder mode
     *
     * @param ignore
     *            if true, internal builder will ignore
     *            build errors while building,
     *            otherwise it will stop at the first build error
     */
    public void setInternalBuilderIgnoreErr(boolean ignore) {
        try {
            getEditableBuilder().setStopOnError(!ignore);
        } catch (CoreException e) {
        }
    }

    /**
     * returns the Internal Builder mode
     * if true, internal builder will ignore build errors while building,
     * otherwise it will stop at the first build error
     *
     * @return boolean
     */
    public boolean getInternalBuilderIgnoreErr() {
        return !getBuilder().isStopOnError();
    }

    /**
     * sets the Internal Builder Parallel mode
     * 
     * @param parallel
     *            if true, internal builder will use parallel mode
     *
     * @deprecated since CDT 9.0. Use {@link #setParallelDef(boolean)}
     */
    @Deprecated
    public void setInternalBuilderParallel(boolean parallel) {
        setParallelDef(parallel);
    }

    /**
     * returns the Internal Builder parallel mode
     * if true, internal builder will work in parallel mode
     * otherwise it will use only one thread
     * 
     * @return boolean
     *
     * @deprecated since CDT 9.0. Use {@link #getParallelDef()}
     */
    @Deprecated
    public boolean getInternalBuilderParallel() {
        return getParallelDef();
    }

    /**
     * Set parallel execution mode for the configuration's builder.
     * 
     * @see Builder#setParallelBuildOn(boolean)
     *
     * @param parallel
     *            - the flag to enable or disable parallel mode.
     */
    public void setParallelDef(boolean parallel) {
        if (getParallelDef() == parallel)
            return;

        try {
            getEditableBuilder().setParallelBuildOn(parallel);
        } catch (CoreException e) {
            Activator.log(e);
        }
    }

    /**
     * Check if the configuration's builder is operating in parallel mode.
     * 
     * @return {@code true} if parallel mode is enabled, {@code false} otherwise.
     */
    public boolean getParallelDef() {
        return getBuilder().isParallelBuildOn();
    }

    /**
     * Sets maximum number of parallel threads/jobs to be used by builder.
     *
     * @param jobs
     *            - maximum number of jobs or threads. For details how
     *            the number is interpreted see
     *            {@link Builder#setParallelizationNum(int)}.
     */
    public void setParallelNumber(int jobs) {
        try {
            getEditableBuilder().setParallelizationNum(jobs);
        } catch (CoreException e) {
            Activator.log(e);
        }
    }

    /**
     * Returns maximum number of parallel threads/jobs used by the configuration's
     * builder.
     * 
     * @see #setParallelDef(boolean)
     *
     * @return - maximum number of parallel threads or jobs used by the builder.
     */
    public int getParallelNumber() {
        return getBuilder().getParallelizationNum();
    }

    //	private Preferences getPreferences(String name){
    //		if(isTemporary)
    //			return null;
    //
    //		IProject project = (IProject)getOwner();
    //
    //		if(project == null || !project.exists() || !project.isOpen())
    //			return null;
    //
    //		Preferences prefs = new ProjectScope(project).getNode(ManagedBuilderCorePlugin.getUniqueIdentifier());
    //		if(prefs != null){
    //			prefs = prefs.node(getId());
    //			if(prefs != null && name != null)
    //				prefs = prefs.node(name);
    //		}
    //		return prefs;
    //	}

    @Override
    public IResourceInfo[] getResourceInfos() {
        return rcInfos.getResourceInfos();
    }

    @Override
    public IResourceInfo getResourceInfo(IPath path, boolean exactPath) {
        return rcInfos.getResourceInfo(path, exactPath);
    }

    @Override
    public IResourceInfo getResourceInfoById(String id) {
        IResourceInfo infos[] = rcInfos.getResourceInfos();
        for (int i = 0; i < infos.length; i++) {
            if (id.equals(infos[i].getId()))
                return infos[i];
        }
        return null;
    }

    @Override
    public IFolderInfo getRootFolderInfo() {
        return rootFolderInfo;
    }

    ResourceInfoContainer getRcInfoContainer(IResourceInfo rcInfo) {
        PathSettingsContainer cr = pathSettings.getChildContainer(rcInfo.getPath(), true, true);
        return new ResourceInfoContainer(cr, false);
    }

    @Override
    public CConfigurationData getConfigurationData() {
        return fCfgData;
    }

    @Override
    public void removeResourceInfo(IPath path) {
        IResourceInfo info = getResourceInfo(path, true);
        if (info != null)
            removeResourceConfiguration(info);
    }

    @Override
    public IFolderInfo createFolderInfo(IPath path) {
        String resourceName = path.lastSegment();
        String id = ManagedBuildManager.calculateChildId(getId(), path.toString());
        return createFolderInfo(path, id, resourceName);
    }

    @Override
    public IFolderInfo createFolderInfo(IPath path, String id, String name) {
        IResourceInfo info = getResourceInfo(path, false);
        IFolderInfo folderInfo = null;
        if (info instanceof IFileInfo) {
            //			folderInfo = null;
        } else if (info instanceof IFolderInfo) {
            IFolderInfo base = (IFolderInfo) info;
            folderInfo = createFolderInfo(path, base, id, name);
        }
        return folderInfo;
    }

    @Override
    public IFolderInfo createFolderInfo(IPath path, IFolderInfo base, String id, String name) {
        if (base.getPath().equals(path))
            return null;

        FolderInfo folderInfo = new FolderInfo((FolderInfo) base, id, name, path);
        addResourceConfiguration(folderInfo);
        folderInfo.propertiesChanged();
        ManagedBuildManager.performValueHandlerEvent(folderInfo, IManagedOptionValueHandler.EVENT_OPEN);

        return folderInfo;
    }

    @Override
    public ICSourceEntry[] getSourceEntries() {
        if (sourceEntries == null || sourceEntries.length == 0) {
            if (parent != null && sourceEntries == null)
                return parent.getSourceEntries();
            return new ICSourceEntry[] {
                    new CSourceEntry(Path.EMPTY, null, ICSettingEntry.VALUE_WORKSPACE_PATH | ICSettingEntry.RESOLVED) };

        }
        return sourceEntries.clone();
    }

    @Override
    public void setSourceEntries(ICSourceEntry[] entries) {
        setSourceEntries(entries, true);
    }

    public void setSourceEntries(ICSourceEntry[] entries, boolean setRebuildState) {
        exportArtifactInfo();
        if (Arrays.equals(getSourceEntries(), entries))
            return;
        sourceEntries = entries != null ? (ICSourceEntry[]) entries.clone() : null;
        if (setRebuildState) {
            setRebuildState(true);
        }
    }

    public void setErrorParserAttribute(String[] ids) {
        if (ids == null) {
            setErrorParserIds(null);
        } else if (ids.length == 0) {
            setErrorParserIds(EMPTY_STRING);
        } else {
            StringBuilder buf = new StringBuilder();
            buf.append(ids[0]);
            for (int i = 1; i < ids.length; i++) {
                buf.append(";").append(ids[i]); //$NON-NLS-1$
            }
            setErrorParserIds(buf.toString());
        }
    }

    @Override
    public void setErrorParserList(String[] ids) {
        if (ids == null) {
            //reset
            resetErrorParsers();
        } else {
            setErrorParserAttribute(ids);
        }
    }

    public void resetErrorParsers() {
        errorParserIds = null;
        IResourceInfo rcInfos[] = getResourceInfos();
        for (int i = 0; i < rcInfos.length; i++) {
            ResourceInfo rcInfo = (ResourceInfo) rcInfos[i];
            rcInfo.resetErrorParsers();
        }
    }

    void removeErrorParsers(Set<String> set) {
        if (set != null && set.isEmpty()) {
            Set<String> oldSet = contributeErrorParsers(null, false);
            if (oldSet == null)
                oldSet = new LinkedHashSet<>();

            oldSet.removeAll(set);
            setErrorParserAttribute(oldSet.toArray(new String[oldSet.size()]));

            IResourceInfo rcInfos[] = getResourceInfos();
            for (int i = 0; i < rcInfos.length; i++) {
                ResourceInfo rcInfo = (ResourceInfo) rcInfos[i];
                rcInfo.removeErrorParsers(set);
            }
        }
    }

    @Override
    public CBuildData getBuildData() {
        return getEditableBuilder().getBuildData();
    }

    @Override
    public IBuilder getEditableBuilder() {
        IToolChain tc = getToolChain();
        IBuilder builder = tc.getBuilder();
        if (builder.isExtensionElement()) {
            String subId = ManagedBuildManager.calculateChildId(builder.getId(), null);
            String builderName = builder.getName() + "." + getName(); //$NON-NLS-1$
            builder = getToolChain().createBuilder(builder, subId, builderName, false);
        }
        return builder;
    }

    @Override
    public IBuilder getBuilder() {
        return getToolChain().getBuilder();
    }


    public ICConfigurationDescription getConfigurationDescription() {
        return fCfgDes;
    }

    public void setConfigurationDescription(ICConfigurationDescription cfgDes) {
        fCfgDes = cfgDes;
    }



    public BooleanExpressionApplicabilityCalculator getBooleanExpressionCalculator() {
        if (booleanExpressionCalculator == null) {
            if (parent != null) {
                return ((Configuration) parent).getBooleanExpressionCalculator();
            }
        }
        return booleanExpressionCalculator;
    }



    @Override
    public String getOutputFlag(String outputExt) {
        // Treat null extension as an empty string
        String ext = outputExt == null ? EMPTY_STRING : outputExt;

        // Get all the tools for the current config
        String flags = EMPTY_STRING;
        ITool[] tools = getFilteredTools();
        for (int index = 0; index < tools.length; index++) {
            ITool tool = tools[index];
            // It's OK
            if (tool.producesFileType(ext)) {
                flags = tool.getOutputFlag();
            }
        }
        return flags;
    }

    @Override
    public String[] getUserObjects(String extension) {
        Vector<String> objs = new Vector<>();
        ITool tool = calculateTargetTool();
        if (tool == null)
            tool = getToolFromOutputExtension(extension);

        if (tool != null) {
            IOption[] opts = tool.getOptions();
            // Look for the user object option type
            for (int i = 0; i < opts.length; i++) {
                IOption option = opts[i];
                try {
                    if (option.getValueType() == IOption.OBJECTS) {
                        // check to see if the option has an applicability calculator
                        IOptionApplicability applicabilityCalculator = option.getApplicabilityCalculator();

                        if (applicabilityCalculator == null
                                || applicabilityCalculator.isOptionUsedInCommandLine(this, tool, option)) {
                            boolean generateDefaultCommand = true;
                            IOptionCommandGenerator commandGenerator = option.getCommandGenerator();
                            if (commandGenerator != null) {
                                SupplierBasedCdtVariableSubstitutor macroSubstitutor = new BuildfileMacroSubstitutor(
                                        null, EMPTY_STRING, WHITE_SPACE);
                                IMacroContextInfoProvider provider = BuildMacroProvider.getDefault();
                                IMacroContextInfo info = provider.getMacroContextInfo(BuildMacroProvider.CONTEXT_OPTION,
                                        new OptionContextData(option, tool));
                                if (info != null) {
                                    macroSubstitutor.setMacroContextInfo(info);
                                    String command = commandGenerator.generateCommand(option, macroSubstitutor);
                                    if (command != null) {
                                        objs.add(command);
                                        generateDefaultCommand = false;
                                    }
                                }
                            }

                            if (generateDefaultCommand) {
                                String unresolved[] = option.getUserObjects();
                                if (unresolved != null && unresolved.length > 0) {
                                    for (int k = 0; k < unresolved.length; k++) {
                                        try {
                                            String resolved[] = ManagedBuildManager.getBuildMacroProvider()
                                                    .resolveStringListValueToMakefileFormat(unresolved[k], "", //$NON-NLS-1$
                                                            " ", //$NON-NLS-1$
                                                            IBuildMacroProvider.CONTEXT_OPTION,
                                                            new OptionContextData(option, tool));
                                            if (resolved != null && resolved.length > 0)
                                                objs.addAll(Arrays.asList(resolved));
                                        } catch (BuildMacroException e) {
                                            // TODO: report error
                                            continue;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (BuildException | CdtVariableException e) {
                    // TODO: report error
                    continue;
                }
            }
        }
        return objs.toArray(new String[objs.size()]);
    }

    @Override
    public String[] getLibs(String extension) {
        Vector<String> libs = new Vector<>();
        ITool tool = calculateTargetTool();
        if (tool == null)
            tool = getToolFromOutputExtension(extension);

        if (tool != null) {
            IOption[] opts = tool.getOptions();
            // Look for the lib option type
            for (int i = 0; i < opts.length; i++) {
                IOption option = opts[i];
                try {
                    if (option.getValueType() == IOption.LIBRARIES) {

                        // check to see if the option has an applicability calculator
                        IOptionApplicability applicabilityCalculator = option.getApplicabilityCalculator();

                        if (applicabilityCalculator == null
                                || applicabilityCalculator.isOptionUsedInCommandLine(this, tool, option)) {
                            boolean generateDefaultCommand = true;
                            IOptionCommandGenerator commandGenerator = option.getCommandGenerator();
                            if (commandGenerator != null) {
                                SupplierBasedCdtVariableSubstitutor macroSubstitutor = new BuildfileMacroSubstitutor(
                                        null, EMPTY_STRING, WHITE_SPACE);
                                IMacroContextInfoProvider provider = BuildMacroProvider.getDefault();
                                IMacroContextInfo info = provider.getMacroContextInfo(BuildMacroProvider.CONTEXT_OPTION,
                                        new OptionContextData(option, tool));
                                if (info != null) {
                                    macroSubstitutor.setMacroContextInfo(info);
                                    String command = commandGenerator.generateCommand(option, macroSubstitutor);
                                    if (command != null) {
                                        libs.add(command);
                                        generateDefaultCommand = false;
                                    }
                                }
                            }

                            if (generateDefaultCommand) {
                                String command = option.getCommand();
                                String[] allLibs = option.getLibraries();
                                for (int j = 0; j < allLibs.length; j++) {
                                    try {
                                        String resolved[] = ManagedBuildManager.getBuildMacroProvider()
                                                .resolveStringListValueToMakefileFormat(allLibs[j], " ", //$NON-NLS-1$
                                                        " ", //$NON-NLS-1$
                                                        IBuildMacroProvider.CONTEXT_OPTION,
                                                        new OptionContextData(option, tool));
                                        if (resolved != null && resolved.length > 0) {
                                            for (int k = 0; k < resolved.length; k++) {
                                                String string = resolved[k];
                                                if (string.length() > 0)
                                                    libs.add(command + string);
                                            }
                                        }
                                    } catch (BuildMacroException e) {
                                        // TODO: report error
                                        continue;
                                    }
                                }
                            }
                        }
                    }
                } catch (BuildException | CdtVariableException e) {
                    // TODO: report error
                    continue;
                }
            }
        }
        return libs.toArray(new String[libs.size()]);
    }


    /**
     * Responsible for contributing 'external' settings back to the core for use
     * by referenced projects.
     *
     * In this case it returns Include, Library path & Library File settings
     * to be used be references for linking the output of this library project
     */
    public void exportArtifactInfo() {

        ICConfigurationDescription des = ManagedBuildManager.getDescriptionForConfiguration(this);
        if (des != null && !des.isReadOnly()) {
            ICOutputEntry entries[] = getConfigurationData().getBuildData().getOutputDirectories();
            IPath path = getOwner().getFullPath();

            List<ICSettingEntry> list = new ArrayList<>(entries.length + 1);

            // Add project level include path
            list.add(CDataUtil.createCIncludePathEntry(path.toString(), ICSettingEntry.VALUE_WORKSPACE_PATH));

            // Add Build output path as an exported library path
            entries = CDataUtil.resolveEntries(entries, des);
            for (int i = 0; i < entries.length; i++) {
                ICOutputEntry out = entries[i];
                String value = out.getValue();

                IPath p = new Path(value);
                if (!p.isAbsolute())
                    value = getOwner().getFullPath().append(value).toString();
                ICLibraryPathEntry lib = CDataUtil.createCLibraryPathEntry(value,
                        out.getFlags() & (~ICSettingEntry.RESOLVED));
                list.add(lib);
            }

            // Add 'libs' artifact names themselves
            ICSettingEntry[] unresolved = new ICSettingEntry[] {
                    CDataUtil.createCLibraryFileEntry(getArtifactName(), 0) };
            ICSettingEntry[] libFiles = CDataUtil.resolveEntries(unresolved, des);
            list.add(libFiles[0]);

            // Contribute the settings back as 'exported'
            des.createExternalSetting(null, null, null, list.toArray(new ICSettingEntry[list.size()]));
        }
    }

    @Override
    public boolean supportsBuild(boolean managed) {
        return supportsBuild(managed, true);
    }

    public boolean supportsBuild(boolean managed, boolean checkBuilder) {
    	return false;
//        IResourceInfo[] rcs = getResourceInfos();
//        for (int i = 0; i < rcs.length; i++) {
//            if (!rcs[i].supportsBuild(managed))
//                return false;
//        }
//
//        if (checkBuilder) {
//            IBuilder builder = getBuilder();
//            if (builder != null && !builder.supportsBuild(managed))
//                return false;
//        }
//
//        return true;
    }





    private SupportedProperties findSupportedProperties() {
        if (supportedProperties == null) {
            if (parent != null) {
                return ((Configuration) parent).findSupportedProperties();
            }
        }
        return supportedProperties;
    }

    private void loadProperties(IManagedConfigElement el) {
        supportedProperties = new SupportedProperties(el);
    }


    @Override
    public boolean isManagedBuildOn() {
        return getBuilder().isManagedBuildOn();
    }

    @Override
    public void setManagedBuildOn(boolean on) throws BuildException {
        getEditableBuilder().setManagedBuildOn(on);
    }

    @Override
    public void changeBuilder(IBuilder newBuilder, String id, String name) {
        changeBuilder(newBuilder, id, name, false);
    }

    public void changeBuilder(IBuilder newBuilder, String id, String name, boolean allBuildSettings) {
        ToolChain tc = (ToolChain) getToolChain();
        Builder cur = (Builder) getEditableBuilder();
        Builder newCfgBuilder = null;
        if (newBuilder.getParent() == tc) {
            newCfgBuilder = (Builder) newBuilder;
        } else {
            IBuilder curReal = ManagedBuildManager.getRealBuilder(cur);
            IBuilder newReal = ManagedBuildManager.getRealBuilder(newBuilder);
            if (newReal != curReal) {
                IBuilder extBuilder = newBuilder;
                for (; extBuilder != null
                        && !extBuilder.isExtensionElement(); extBuilder = extBuilder.getSuperClass()) {
                }
                if (extBuilder == null)
                    extBuilder = newBuilder;

                newCfgBuilder = new Builder(tc, extBuilder, id, name, false);
                newCfgBuilder.copySettings(cur, allBuildSettings);
            }
        }

        if (newCfgBuilder != null) {
            tc.setBuilder(newCfgBuilder);
        }
    }

    @Override
    public boolean isBuilderCompatible(IBuilder builder) {
        return builder.supportsBuild(isManagedBuildOn());
    }

    ITool findToolById(String id) {
        IResourceInfo[] rcInfos = getResourceInfos();
        ITool tool = null;
        for (int i = 0; i < rcInfos.length; i++) {
            ResourceInfo info = (ResourceInfo) rcInfos[i];
            tool = info.getToolById(id);
            if (tool != null)
                break;
        }
        return tool;
    }

    void resolveProjectReferences(boolean onLoad) {
        IResourceInfo[] rcInfos = getResourceInfos();
        for (int i = 0; i < rcInfos.length; i++) {
            ResourceInfo info = (ResourceInfo) rcInfos[i];
            info.resolveProjectReferences(onLoad);
        }
    }

    public boolean isPerRcTypeDiscovery() {
        ToolChain tc = (ToolChain) getRootFolderInfo().getToolChain();
        return tc.isPerRcTypeDiscovery();
    }

    public void setPerRcTypeDiscovery(boolean on) {
        ToolChain tc = (ToolChain) getRootFolderInfo().getToolChain();
        tc.setPerRcTypeDiscovery(on);
    }

    //	public IScannerConfigBuilderInfo2 getScannerConfigInfo(){
    //		ToolChain tc = (ToolChain)getRootFolderInfo().getToolChain();
    //		return tc.getScannerConfigBuilderInfo();
    //	}

    //	public IScannerConfigBuilderInfo2 setScannerConfigInfo(IScannerConfigBuilderInfo2 info){
    //		ToolChain tc = (ToolChain)getRootFolderInfo().getToolChain();
    //		return tc.setScannerConfigBuilderInfo(info);
    //	}

    public PathInfoCache setDiscoveredPathInfo(PathInfoCache info) {
        ToolChain tc = (ToolChain) getRootFolderInfo().getToolChain();
        return tc.setDiscoveredPathInfo(info);
    }

    public PathInfoCache getDiscoveredPathInfo() {
        ToolChain tc = (ToolChain) getRootFolderInfo().getToolChain();
        return tc.getDiscoveredPathInfo();
    }

    public String getDiscoveryProfileId() {
        ToolChain tc = (ToolChain) getRootFolderInfo().getToolChain();
        return tc.getScannerConfigDiscoveryProfileId();
    }

    public PathInfoCache clearDiscoveredPathInfo() {
        ToolChain tc = (ToolChain) getRootFolderInfo().getToolChain();
        return tc.clearDiscoveredPathInfo();
    }

    public ICfgScannerConfigBuilderInfo2Set getCfgScannerConfigInfo() {
        return cfgScannerInfo;
    }

    public void setCfgScannerConfigInfo(ICfgScannerConfigBuilderInfo2Set info) {
        cfgScannerInfo = info;
    }

    public void clearCachedData() {
        cfgScannerInfo = null;
    }

    public boolean isPreference() {
        return isPreferenceConfig;
    }


    @Override
    public void setBuildArtefactType(String id) throws BuildException {
//        IBuildObjectProperties props = getBuildProperties();
//        try {
//            props.setProperty(ManagedBuildManager.BUILD_ARTEFACT_TYPE_PROPERTY_ID, id);
//        } catch (CoreException e) {
//            throw new BuildException(e.getLocalizedMessage());
//        }
//        // May need to update the exports paths & symbols after artifact type change
//        exportArtifactInfo();
    }

    boolean isExcluded(IPath path) {
        //		if(path.segmentCount() == 0)
        //			return false;
        ICSourceEntry[] entries = getSourceEntries();
        return CDataUtil.isExcluded(path, entries);
    }

    void setExcluded(IPath path, boolean isFolder, boolean excluded) {
        //		if(path.segmentCount() == 0)
        //			return;
        if (excludeList == null) {
            ICSourceEntry[] newEntries = getUpdatedEntries(path, isFolder, excluded);
            if (newEntries != null)
                setSourceEntries(newEntries, false);
        } else {
            if (excluded)
                excludeList.add(path);
        }
    }

    private ICSourceEntry[] getUpdatedEntries(IPath path, boolean isFolder, boolean excluded) {
        try {
            ICSourceEntry[] entries = getSourceEntries();
            return CDataUtil.setExcluded(path, isFolder, excluded, entries, false);
        } catch (CoreException e) {
            Activator.log(e);
        }
        return null;
    }

    boolean canExclude(IPath path, boolean isFolder, boolean excluded) {
        if (excludeList == null) {
            ICSourceEntry[] newEntries = getUpdatedEntries(path, isFolder, excluded);
            return newEntries != null;
        } else {
            if (excluded)
                excludeList.add(path);
            return true;
        }
    }

	@Override
	public void resolveFields() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void resolveSuperClass() throws Exception {
		// TODO Auto-generated method stub
		
	}



 


}
