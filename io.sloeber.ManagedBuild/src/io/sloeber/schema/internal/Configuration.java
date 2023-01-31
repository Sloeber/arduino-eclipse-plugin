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
package io.sloeber.schema.internal;

import static io.sloeber.autoBuild.integration.Const.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IFolderInfo;
import io.sloeber.schema.api.IProjectType;
import io.sloeber.schema.api.IToolChain;

public class Configuration extends SchemaObject implements IConfiguration {

    private static final String LANGUAGE_SETTINGS_PROVIDER_DELIMITER = ";"; //$NON-NLS-1$
    private static final String LANGUAGE_SETTINGS_PROVIDER_NEGATION_SIGN = "-"; //$NON-NLS-1$
    private static final String $TOOLCHAIN = "${Toolchain}"; //$NON-NLS-1$

    String[] modelartifactName;
    String[] modelartifactExtension;
    String[] modelerrorParsers;
    String[] modellanguageSettingsProviders;
    String[] modeldescription;
    String[] modelbuildProperties;
    String[] modelbuildArtefactType;
    String[] modelprebuildStep;
    String[] modelpostbuildStep;
    String[] modelpreannouncebuildStep;
    String[] modelpostannouncebuildStep;
    String[] modelcleanCommand;

    ToolChain myToolchain;
    List<FolderInfo> myFolderInfo = new ArrayList<>();

    // Parent and children
    private ProjectType projectType;
    private List<String> defaultLanguageSettingsProviderIds = new ArrayList<>();

    private boolean isPreferenceConfig;
    private String[] myErrorParserIDs;

    /**
     * Create an configuration from the project manifest file element.
     *
     * @param projectType
     *            The <code>ProjectType</code> the configuration will be
     *            added to.
     * @param element
     *            The element from the manifest that contains the
     *            configuration information.
     */
    public Configuration(ProjectType projectType, IExtensionPoint root, IConfigurationElement element) {
        this.projectType = projectType;

        loadNameAndID(root, element);

        modelartifactName = getAttributes(ARTIFACT_NAME);
        modelartifactExtension = getAttributes(ARTIFACT_EXTENSION);
        modelcleanCommand = getAttributes(CLEAN_COMMAND);
        modelerrorParsers = getAttributes(ERROR_PARSERS);
        modellanguageSettingsProviders = getAttributes(LANGUAGE_SETTINGS_PROVIDERS);
        modelprebuildStep = getAttributes(PREBUILD_STEP);
        modelpostbuildStep = getAttributes(POSTBUILD_STEP);
        modelpreannouncebuildStep = getAttributes(PREANNOUNCEBUILD_STEP);
        modelpostannouncebuildStep = getAttributes(POSTANNOUNCEBUILD_STEP);
        modeldescription = getAttributes(DESCRIPTION);
        modelbuildProperties = getAttributes(BUILD_PROPERTIES);
        modelbuildArtefactType = getAttributes(BUILD_ARTEFACT_TYPE);

        // Load the children
        IConfigurationElement[] configElements = element.getChildren();
        for (IConfigurationElement configElement : configElements) {
            switch (configElement.getName()) {
            case IToolChain.TOOL_CHAIN_ELEMENT_NAME: {
                myToolchain = new ToolChain(this, root, configElement);
                break;
            }
            case IFolderInfo.FOLDER_INFO_ELEMENT_NAME: {
                myFolderInfo.add(new FolderInfo(this, root, configElement));
                break;
            }
            }
        }
        resolveFields();
    }

    private void resolveFields() {
        myErrorParserIDs = modelerrorParsers[SUPER].split(SEMICOLON);

        if (modelcleanCommand[SUPER].isBlank()) {
            if (Platform.getOS().equals(Platform.OS_WIN32)) {
                modelcleanCommand[SUPER] = "del"; //$NON-NLS-1$
            } else {
                modelcleanCommand[SUPER] = "rm"; //$NON-NLS-1$
            }
        }

        if (!modellanguageSettingsProviders[SUPER].isBlank()) {

            String[] defaultIds = modellanguageSettingsProviders[SUPER].split(LANGUAGE_SETTINGS_PROVIDER_DELIMITER);
            for (String defaultID : defaultIds) {
                if (defaultID != null && !defaultID.isEmpty()) {
                    if (defaultID.startsWith(LANGUAGE_SETTINGS_PROVIDER_NEGATION_SIGN)) {
                        defaultID = defaultID.substring(1);
                        defaultLanguageSettingsProviderIds.remove(defaultID);
                    } else if (!defaultLanguageSettingsProviderIds.contains(defaultID)) {
                        if (defaultID.contains($TOOLCHAIN)) {
                            if (myToolchain != null) {
                                String toolchainProvidersIds = myToolchain.getDefaultLanguageSettingsProviderIds();
                                if (toolchainProvidersIds != null) {
                                    defaultLanguageSettingsProviderIds.addAll(Arrays
                                            .asList(toolchainProvidersIds.split(LANGUAGE_SETTINGS_PROVIDER_DELIMITER)));
                                }
                            }
                        } else {
                            defaultLanguageSettingsProviderIds.add(defaultID);
                        }
                    }
                }

            }
        }

    }

    @Override
    public IProjectType getProjectType() {
        return projectType;
    }

    @Override
    public IToolChain getToolChain() {
        return myToolchain;
    }

    /*
     * M O D E L A T T R I B U T E A C C E S S O R S
     */

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public String getArtifactExtension() {
        return modelartifactExtension[SUPER];
    }

    @Override
    public String getArtifactName() {
        return modelartifactName[SUPER];
    }

    @Override
    public String getDescription() {
        return modeldescription[SUPER];
    }

    @Override
    public String getErrorParserIds() {
        return modelerrorParsers[SUPER];
    }

    @Override
    public String[] getErrorParserList() {
        return myErrorParserIDs;
    }

    @Override
    public String getCleanCommand() {
        return modelcleanCommand[SUPER];
    }

    /**
     * {@inheritDoc}
     *
     * This function will try to find default provider Ids specified in this
     * instance. It none defined, it will try to pull Ids from the parent
     * configuration.
     */
    @Override
    public List<String> getDefaultLanguageSettingsProviderIds() {
        return defaultLanguageSettingsProviderIds;
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    /*
     * O B J E C T S T A T E M A I N T E N A N C E
     */

    @Override
    public List<ICSourceEntry> getSourceEntries() {
        return new LinkedList<>();
        //                if (sourceEntries == null || sourceEntries.length == 0) {
        //                    if (parent != null && sourceEntries == null)
        //                        return parent.getSourceEntries();
        //                    return new ICSourceEntry[] {
        //                            new CSourceEntry(Path.EMPTY, null, ICSettingEntry.VALUE_WORKSPACE_PATH | ICSettingEntry.RESOLVED) };
        //        
        //                }
        //                return sourceEntries.clone();
    }

    @Override
    public IBuilder getBuilder() {
        return myToolchain.getBuilder();
    }

    public boolean isPreference() {
        return isPreferenceConfig;
    }

    @Override
    public String getPrebuildStep() {
        return modelprebuildStep[SUPER];
    }

    @Override
    public String getPostbuildStep() {
        return modelpostbuildStep[SUPER];
    }

    @Override
    public String getPreannouncebuildStep() {
        return modelpreannouncebuildStep[SUPER];
    }

    @Override
    public String getPostannouncebuildStep() {
        return modelpostannouncebuildStep[SUPER];
    }

    @Override
    public IFolder getBuildFolder(ICConfigurationDescription cfg) {
        return cfg.getProjectDescription().getProject().getFolder(cfg.getName());
    }

    public StringBuffer dump(int leadingChars) {
        StringBuffer ret = new StringBuffer();
        String prepend = StringUtils.repeat(DUMPLEAD, leadingChars);
        ret.append(prepend + CONFIGURATION_ELEMENT_NAME + NEWLINE);
        ret.append(prepend + NAME + EQUAL + myName + NEWLINE);
        ret.append(prepend + ID + EQUAL + myID + NEWLINE);
        ret.append(prepend + ARTIFACT_NAME + EQUAL + modelartifactName[SUPER] + NEWLINE);
        ret.append(prepend + ARTIFACT_EXTENSION + EQUAL + modelartifactExtension[SUPER] + NEWLINE);
        ret.append(prepend + CLEAN_COMMAND + EQUAL + modelcleanCommand[SUPER] + NEWLINE);
        ret.append(prepend + ERROR_PARSERS + EQUAL + modelerrorParsers[SUPER] + NEWLINE);
        ret.append(prepend + LANGUAGE_SETTINGS_PROVIDERS + EQUAL + modellanguageSettingsProviders[SUPER] + NEWLINE);

        ret.append(prepend + PREBUILD_STEP + EQUAL + modelprebuildStep[SUPER] + NEWLINE);
        ret.append(prepend + POSTBUILD_STEP + EQUAL + modelpostbuildStep[SUPER] + NEWLINE);
        ret.append(prepend + PREANNOUNCEBUILD_STEP + EQUAL + modelpreannouncebuildStep[SUPER] + NEWLINE);
        ret.append(prepend + POSTANNOUNCEBUILD_STEP + EQUAL + modelpostannouncebuildStep[SUPER] + NEWLINE);
        ret.append(prepend + DESCRIPTION + EQUAL + modeldescription[SUPER] + NEWLINE);
        ret.append(prepend + BUILD_PROPERTIES + EQUAL + modelbuildProperties[SUPER] + NEWLINE);
        ret.append(prepend + BUILD_ARTEFACT_TYPE + EQUAL + modelbuildArtefactType[SUPER] + NEWLINE);

        ret.append(prepend + BEGIN_OF_CHILDREN + IToolChain.TOOL_CHAIN_ELEMENT_NAME + NEWLINE);
        //  ret.append(myToolchain.dump(leadingChars+1));
        ret.append(prepend + END_OF_CHILDREN + IToolChain.TOOL_CHAIN_ELEMENT_NAME + NEWLINE);
        return ret;
    }

}

// private Preferences getPreferences(String name){
// if(isTemporary)
// return null;
//
// IProject project = (IProject)getOwner();
//
// if(project == null || !project.exists() || !project.isOpen())
// return null;
//
// Preferences prefs = new
// ProjectScope(project).getNode(ManagedBuilderCorePlugin.getUniqueIdentifier());
// if(prefs != null){
// prefs = prefs.node(getId());
// if(prefs != null && name != null)
// prefs = prefs.node(name);
// }
// return prefs;
// }

//@Override
//public List<IResourceInfo> getResourceInfos() {
//	return rcInfos.getResourceInfos();
//}
//
//@Override
//public IResourceInfo getResourceInfo(IPath path, boolean exactPath) {
//	return rcInfos.getResourceInfo(path, exactPath);
//}
//
//@Override
//public IResourceInfo getResourceInfoById(String id) {
//	List<IResourceInfo> infos = rcInfos.getResourceInfos();
//	for (IResourceInfo info : infos) {
//		if (id.equals(info.getId()))
//			return info;
//	}
//	return null;
//}

//public PathInfoCache clearDiscoveredPathInfo() {
//ToolChain tc = (ToolChain) getRootFolderInfo().getToolChain();
//return tc.clearDiscoveredPathInfo();
//}

//public void setPerRcTypeDiscovery(boolean on) {
//ToolChain tc = (ToolChain) getRootFolderInfo().getToolChain();
//tc.setPerRcTypeDiscovery(on);
//}

// public IScannerConfigBuilderInfo2 getScannerConfigInfo(){
// ToolChain tc = (ToolChain)getRootFolderInfo().getToolChain();
// return tc.getScannerConfigBuilderInfo();
// }

// public IScannerConfigBuilderInfo2
// setScannerConfigInfo(IScannerConfigBuilderInfo2 info){
// ToolChain tc = (ToolChain)getRootFolderInfo().getToolChain();
// return tc.setScannerConfigBuilderInfo(info);
// }

//public PathInfoCache setDiscoveredPathInfo(PathInfoCache info) {
//ToolChain tc = (ToolChain) getRootFolderInfo().getToolChain();
//return tc.setDiscoveredPathInfo(info);
//}

//public PathInfoCache getDiscoveredPathInfo() {
//ToolChain tc = (ToolChain) getRootFolderInfo().getToolChain();
//return tc.getDiscoveredPathInfo();
//}

//private SupportedProperties findSupportedProperties() {
//if (supportedProperties == null) {
//	if (parent != null) {
//		return ((Configuration) parent).findSupportedProperties();
//	}
//}
//return supportedProperties;
//}

//private void loadProperties(IConfigurationElement el) {
//supportedProperties = new SupportedProperties(el);
//}

//public void changeBuilder(IBuilder newBuilder, String id, String name, boolean allBuildSettings) {
//ToolChain tc = (ToolChain) getToolChain();
//Builder cur = (Builder) getBuilder();
//Builder newCfgBuilder = null;
//if (newBuilder.getParent() == tc) {
//	newCfgBuilder = (Builder) newBuilder;
//} else {
//	IBuilder curReal = ManagedBuildManager.getRealBuilder(cur);
//	IBuilder newReal = ManagedBuildManager.getRealBuilder(newBuilder);
//	if (newReal != curReal) {
//		IBuilder extBuilder = newBuilder;
//		for (; extBuilder != null/*
//				&& !extBuilder.isExtensionElement()*/; extBuilder = extBuilder.getSuperClass()) {
//		}
//		if (extBuilder == null)
//			extBuilder = newBuilder;
//
//		newCfgBuilder = new Builder(tc, extBuilder, id, name, false);
//		newCfgBuilder.copySettings(cur, allBuildSettings);
//	}
//}
//
//if (newCfgBuilder != null) {
//	tc.setBuilder(newCfgBuilder);
//}
//}

//ITool findToolById(String id) {
//List<IResourceInfo> rcInfos = getResourceInfos();
//ITool tool = null;
//for (IResourceInfo info : rcInfos) {
//	tool = ((ResourceInfo) info).getToolById(id);
//	if (tool != null)
//		break;
//}
//return tool;
//}

//void resolveProjectReferences(boolean onLoad) {
//List<IResourceInfo> rcInfos = getResourceInfos();
//for (IResourceInfo info : rcInfos) {
//	((ResourceInfo) info).resolveProjectReferences(onLoad);
//}
//}

//private IFolderInfo createRootFolderInfo() {
//String id = ManagedBuildManager.calculateChildId(this.id, null);
//String name = "/"; //$NON-NLS-1$
//
//rootFolderInfo = new FolderInfo(this, new Path(name), id, name, isExtensionConfig);
//addResourceConfiguration(rootFolderInfo);
//return rootFolderInfo;
//}

/*
* public IFolderInfo createFolderInfo(IPath path, IToolChain superClass, String
* Id, String name){
* 
* }
* 
* public IFolderInfo createFolderInfo(IPath path, IFolderInfo baseFolderInfo,
* String Id, String name){
* 
* }
*/
//    /**
//     * Responsible for contributing 'external' settings back to the core for use by
//     * referenced projects.
//     *
//     * In this case it returns Include, Library path & Library File settings to be
//     * used be references for linking the output of this library project
//     */
//    public void exportArtifactInfo() {
//
//        ICConfigurationDescription des = ManagedBuildManager.getDescriptionForConfiguration(this);
//        if (des != null && !des.isReadOnly()) {
//            ICOutputEntry entries[] = getConfigurationData().getBuildData().getOutputDirectories();
//            IPath path = getOwner().getFullPath();
//
//            List<ICSettingEntry> list = new ArrayList<>(entries.length + 1);
//
//            // Add project level include path
//            list.add(CDataUtil.createCIncludePathEntry(path.toString(), ICSettingEntry.VALUE_WORKSPACE_PATH));
//
//            // Add Build output path as an exported library path
//            entries = CDataUtil.resolveEntries(entries, des);
//            for (int i = 0; i < entries.length; i++) {
//                ICOutputEntry out = entries[i];
//                String value = out.getValue();
//
//                IPath p = new Path(value);
//                if (!p.isAbsolute())
//                    value = getOwner().getFullPath().append(value).toString();
//                ICLibraryPathEntry lib = CDataUtil.createCLibraryPathEntry(value,
//                        out.getFlags() & (~ICSettingEntry.RESOLVED));
//                list.add(lib);
//            }
//
//            // Add 'libs' artifact names themselves
//            ICSettingEntry[] unresolved = new ICSettingEntry[] {
//                    CDataUtil.createCLibraryFileEntry(getArtifactName(), 0) };
//            ICSettingEntry[] libFiles = CDataUtil.resolveEntries(unresolved, des);
//            list.add(libFiles[0]);
//
//            // Contribute the settings back as 'exported'
//            des.createExternalSetting(null, null, null, list.toArray(new ICSettingEntry[list.size()]));
//        }
//    }

//
//@Override
//public boolean hasOverriddenBuildCommand() {
//  IBuilder builder = getToolChain().getBuilder();
//  if (builder != null) {
//      IBuilder superB = builder.getSuperClass();
//      if (superB != null) {
//          String command = builder.getCommand();
//          if (command != null) {
//              String superC = superB.getCommand();
//              if (superC != null) {
//                  if (!command.equals(superC)) {
//                      return true;
//                  }
//              }
//          }
//          String args = builder.getArguments();
//          if (args != null) {
//              String superA = superB.getArguments();
//              if (superA != null) {
//                  if (!args.equals(superA)) {
//                      return true;
//                  }
//              }
//          }
//      }
//  }
//  return false;
//}

///**
//* Initialize the configuration information from the XML element specified in
//* the argument
//*
//* @param element
//*            An XML element containing the configuration information
//*/
//protected void loadFromProject(ICStorageElement element) {
//
// // // id
// // // note: IDs are unique so no benefit to intern them
// // setId(element.getAttribute(IBuildObject.ID));
// //
// // // name
// // if (element.getAttribute(IBuildObject.NAME) != null)
// // setName(element.getAttribute(IBuildObject.NAME));
// //
// // // description
// // if (element.getAttribute(IConfiguration.DESCRIPTION) != null)
// // description = element.getAttribute(IConfiguration.DESCRIPTION);
// //
// // String props = element.getAttribute(BUILD_PROPERTIES);
// //
// // String optionalProps = element.getAttribute(OPTIONAL_BUILD_PROPERTIES);
// //
// // String artType = element.getAttribute(BUILD_ARTEFACT_TYPE);
// //
// // if (element.getAttribute(IConfiguration.PARENT) != null) {
// // // See if the parent belongs to the same project
// // if (managedProject != null)
// // parent =
// // managedProject.getConfiguration(element.getAttribute(IConfiguration.PARENT));
// // // If not, then try the extension configurations
// // if (parent == null) {
// // parent =
// // ManagedBuildManager.getExtensionConfiguration(element.getAttribute(IConfiguration.PARENT));
// // if (parent == null) {
// // String message = NLS.bind(Configuration_orphaned, getId(), //$NON-NLS-1$
// // element.getAttribute(IConfiguration.PARENT));
// // Activator.error(message);
// // }
// // }
// // }
// //
// // // Get the name of the build artifact associated with target (usually
// // // in the plugin specification).
// // if (element.getAttribute(ARTIFACT_NAME) != null) {
// // artifactName = element.getAttribute(ARTIFACT_NAME);
// // }
// //
// // // Get the semicolon separated list of IDs of the error parsers
// // if (element.getAttribute(ERROR_PARSERS) != null) {
// // errorParserIds = element.getAttribute(ERROR_PARSERS);
// // }
// //
// // // Get the artifact extension
// // if (element.getAttribute(EXTENSION) != null) {
// // artifactExtension = element.getAttribute(EXTENSION);
// // }
// //
// // // Get the clean command
// // if (element.getAttribute(CLEAN_COMMAND) != null) {
// // cleanCommand = element.getAttribute(CLEAN_COMMAND);
// // }
// //
// // // Get the pre-build and post-build commands
// // if (element.getAttribute(PREBUILD_STEP) != null) {
// // prebuildStep = element.getAttribute(PREBUILD_STEP);
// // }
// //
// // if (element.getAttribute(POSTBUILD_STEP) != null) {
// // postbuildStep = element.getAttribute(POSTBUILD_STEP);
// // }
// //
// // // Get the pre-build and post-build announcements
// // if (element.getAttribute(PREANNOUNCEBUILD_STEP) != null) {
// // preannouncebuildStep = element.getAttribute(PREANNOUNCEBUILD_STEP);
// // }
// //
// // if (element.getAttribute(POSTANNOUNCEBUILD_STEP) != null) {
// // postannouncebuildStep = element.getAttribute(POSTANNOUNCEBUILD_STEP);
// // }
//}
//	private void copySettingsFrom(Configuration cloneConfig, boolean cloneChildren) {
//                fCfgData = new BuildConfigurationData(this);
//        
//                this.description = cloneConfig.getDescription();
//        
//                // set managedBuildRevision
//                setManagedBuildRevision(cloneConfig.getManagedBuildRevision());
//        
//                if (!cloneConfig.isExtensionConfig)
//                    cloneChildren = true;
//                // If this constructor is called to clone an existing
//                // configuration, the parent of the cloning config should be stored.
//                parent = cloneConfig.isExtensionConfig || cloneConfig.getParent() == null ? cloneConfig
//                        : cloneConfig.getParent();
//                parentId = parent.getId();
//        
//                //  Copy the remaining attributes
//                projectType = cloneConfig.projectType;
//                artifactName = cloneConfig.artifactName;
//                cleanCommand = cloneConfig.cleanCommand;
//                artifactExtension = cloneConfig.artifactExtension;
//                errorParserIds = cloneConfig.errorParserIds;
//                prebuildStep = cloneConfig.prebuildStep;
//                postbuildStep = cloneConfig.postbuildStep;
//                preannouncebuildStep = cloneConfig.preannouncebuildStep;
//                postannouncebuildStep = cloneConfig.postannouncebuildStep;
//                if (cloneConfig.sourceEntries != null) {
//                    sourceEntries = cloneConfig.sourceEntries.clone();
//                }
//                defaultLanguageSettingsProvidersAttribute = cloneConfig.defaultLanguageSettingsProvidersAttribute;
//                if (cloneConfig.defaultLanguageSettingsProviderIds != null) {
//                    defaultLanguageSettingsProviderIds = cloneConfig.defaultLanguageSettingsProviderIds.clone();
//                }
//        
//                //		enableInternalBuilder(cloneConfig.isInternalBuilderEnabled());
//                //		setInternalBuilderIgnoreErr(cloneConfig.getInternalBuilderIgnoreErr());
//                //		setInternalBuilderParallel(cloneConfig.getInternalBuilderParallel());
//                //		setParallelDef(cloneConfig.getParallelDef());
//                //		setParallelNumber(cloneConfig.getParallelNumber());
//                //		internalBuilderEnabled = cloneConfig.internalBuilderEnabled;
//                //		internalBuilderIgnoreErr = cloneConfig.internalBuilderIgnoreErr;
//        
//                // Clone the configuration's children
//                // Tool Chain
//                boolean copyIds = cloneConfig.getId().equals(id);
//                String subId;
//                //  Resource Configurations
//                Map<IPath, Map<String, String>> toolIdMap = new HashMap<>();
//                IResourceInfo infos[] = cloneConfig.rcInfos.getResourceInfos();
//                for (int i = 0; i < infos.length; i++) {
//                    if (infos[i] instanceof FolderInfo) {
//                        FolderInfo folderInfo = (FolderInfo) infos[i];
//                        subId = copyIds ? folderInfo.getId()
//                                : ManagedBuildManager.calculateChildId(getId(), folderInfo.getPath().toString());
//                        FolderInfo newFolderInfo = new FolderInfo(this, folderInfo, subId, toolIdMap, cloneChildren);
//                        addResourceConfiguration(newFolderInfo);
//                    } else {
//                        ResourceConfiguration fileInfo = (ResourceConfiguration) infos[i];
//                        subId = copyIds ? fileInfo.getId()
//                                : ManagedBuildManager.calculateChildId(getId(), fileInfo.getPath().toString());
//                        ResourceConfiguration newResConfig = new ResourceConfiguration(this, fileInfo, subId, toolIdMap,
//                                cloneChildren);
//                        addResourceConfiguration(newResConfig);
//        
//                    }
//                }
//        
//                resolveProjectReferences(false);
//        
//                if (cloneChildren) {
//                    //copy expand build macros setting
//                    BuildMacroProvider macroProvider = (BuildMacroProvider) ManagedBuildManager.getBuildMacroProvider();
//                   //TOFIX JABA  macroProvider.expandMacrosInBuildfile(this,macroProvider.areMacrosExpandedInBuildfile(cloneConfig));
//        
//                    //copy user-defined build macros
//                    /*			UserDefinedMacroSupplier userMacros = BuildMacroProvider.fUserDefinedMacroSupplier;
//                    			userMacros.setMacros(
//                    					userMacros.getMacros(BuildMacroProvider.CONTEXT_CONFIGURATION,cloneConfig),
//                    					BuildMacroProvider.CONTEXT_CONFIGURATION,
//                    					this);
//                    */
//                    //copy user-defined environment
//                    //			UserDefinedEnvironmentSupplier userEnv = EnvironmentVariableProvider.fUserSupplier;
//                    //			userEnv.setVariables(
//                    //					userEnv.getVariables(cloneConfig), this);
//        
//                }
//        
//                // Hook me up
//                if (managedProject != null) {
//                    managedProject.addConfiguration(this);
//                }
//        
//                if (copyIds) {
//                    rebuildNeeded = cloneConfig.rebuildNeeded;
//                    resourceChangeState = cloneConfig.resourceChangeState;
//                } else {
//                    if (cloneConfig.isExtensionConfig)
//                        exportArtifactInfo();
//                    setRebuildState(true);
//                }
//
//	}

//    public void applyToManagedProject(ManagedProject mProj) {
//        managedProject = mProj;
//        isPreferenceConfig = false;
//        isTemporary = false;
//        managedProject.addConfiguration(this);
//    }

/*
 * E L E M E N T A T T R I B U T E R E A D E R S A N D W R I T E R S
 */

/**
 * Create a new extension configuration and fill in the attributes and childen
 * later.
 *
 * @param projectType
 *            The <code>ProjectType</code> the configuration will be
 *            added to.
 * @param parentConfig
 *            The <code>IConfiguration</code> that is the parent
 *            configuration of this configuration
 * @param id
 *            A unique ID for the new configuration.
 * @param name
 *            A name for the new configuration.
 */
//public Configuration(ProjectType projectType, IConfiguration parentConfig, String newID, String newName) {
//    //        newID = (id);
//    //        newName = name;
//    //        this.projectType = projectType;
//    //        parent = parentConfig;
//    //        isExtensionConfig = true;
//    //
//    //        // Hook me up to the ProjectType
//    //        if (projectType != null) {
//    //            projectType.addConfiguration(this);
//    //            setManagedBuildRevision(projectType.getManagedBuildRevision());
//    //        }
//}
//public Configuration(ManagedProject managedProject, IToolChain tCh, String newID, String newName) {
////        id = newID;
////        name = newName;
////
////        //		this.description = cloneConfig.getDescription();
////        this.managedProject = managedProject;
////        isExtensionConfig = false;
////
////        if (tCh == null) {
////            //create configuration based upon the preference config
////            IConfiguration cfg = ManagedBuildManager.getPreferenceConfiguration(false);
////            if (cfg != null)
////                copySettingsFrom((Configuration) cfg, true);
////        } else {
////            Configuration baseCfg = null;//TOFIX JABA (Configuration) ManagedBuildManager.getExtensionConfiguration(EMPTY_CFG_ID);
////            //		this.isTemporary = temporary;
////            fCfgData = new BuildConfigurationData(this);
////
////            // set managedBuildRevision
////            setManagedBuildRevision(baseCfg.getManagedBuildRevision());
////
////            //		if(!baseCfg.isExtensionConfig)
////            //			cloneChildren = true;
////            // If this constructor is called to clone an existing
////            // configuration, the parent of the cloning config should be stored.
////            parent = baseCfg.isExtensionConfig || baseCfg.getParent() == null ? baseCfg : baseCfg.getParent();
////
////            //  Copy the remaining attributes
////            projectType = baseCfg.projectType;
////            artifactName = baseCfg.artifactName;
////            cleanCommand = baseCfg.cleanCommand;
////            artifactExtension = baseCfg.artifactExtension;
////            errorParserIds = baseCfg.errorParserIds;
////            prebuildStep = baseCfg.prebuildStep;
////            postbuildStep = baseCfg.postbuildStep;
////            preannouncebuildStep = baseCfg.preannouncebuildStep;
////            postannouncebuildStep = baseCfg.postannouncebuildStep;
////
////            if (baseCfg.sourceEntries != null)
////                sourceEntries = baseCfg.sourceEntries.clone();
////
////            defaultLanguageSettingsProvidersAttribute = baseCfg.defaultLanguageSettingsProvidersAttribute;
////            if (baseCfg.defaultLanguageSettingsProviderIds != null) {
////                defaultLanguageSettingsProviderIds = baseCfg.defaultLanguageSettingsProviderIds.clone();
////            }
////
////            //		enableInternalBuilder(baseCfg.isInternalBuilderEnabled());
////            //		setInternalBuilderIgnoreErr(baseCfg.getInternalBuilderIgnoreErr());
////            //		setInternalBuilderParallel(baseCfg.getInternalBuilderParallel());
////            //		setParallelDef(baseCfg.getParallelDef());
////            //		setParallelNumber(baseCfg.getParallelNumber());
////            //		internalBuilderEnabled = cloneConfig.internalBuilderEnabled;
////            //		internalBuilderIgnoreErr = cloneConfig.internalBuilderIgnoreErr;
////
////            // Clone the configuration's children
////            // Tool Chain
////
////            String tcId = ManagedBuildManager.calculateChildId(tCh.getId(), null);
////
////            IToolChain newChain = createToolChain(tCh, tcId, tCh.getName(), false);
////
//////            // For each option/option category child of the tool-chain that is
//////            // the child of the selected configuration element, create an option/
//////            // option category child of the cloned configuration's tool-chain element
//////            // that specifies the original tool element as its superClass.
//////            newChain.createOptions(tCh);
////
////            // For each tool element child of the tool-chain that is the child of
////            // the selected configuration element, create a tool element child of
////            // the cloned configuration's tool-chain element that specifies the
////            // original tool element as its superClass.
////            String subId;
////           List< ITool> tools = tCh.getTools();
////            for (ITool tool:tools) {
////                Tool toolChild = (Tool) tool;
////                subId = ManagedBuildManager.calculateChildId(toolChild.getId(), null);
////                newChain.createTool(toolChild, subId, toolChild.getName(), false);
////            }
////
////            ITargetPlatform tpBase = tCh.getTargetPlatform();
////            ITargetPlatform extTp = tpBase;
////            for (; extTp != null && !extTp.isExtensionElement(); extTp = extTp.getSuperClass()) {
////            }
////
////            TargetPlatform tp;
////            if (extTp != null) {
////                int nnn = ManagedBuildManager.getRandomNumber();
////                subId = extTp.getId() + "." + nnn; //$NON-NLS-1$
////                //				subName = tpBase.getName();
////                tp = new TargetPlatform(newChain, subId, tpBase.getName(), (TargetPlatform) tpBase);
////            } else {
////                subId = ManagedBuildManager.calculateChildId(getId(), null);
////                String subName = EMPTY_STRING;
////                tp = new TargetPlatform((ToolChain) newChain, null, subId, subName, false);
////            }
////
////            ((ToolChain) newChain).setTargetPlatform(tp);
////
////            //		if(cloneChildren){
////            //copy expand build macros setting
////            //			BuildMacroProvider macroProvider = (BuildMacroProvider)ManagedBuildManager.getBuildMacroProvider();
////            //			macroProvider.expandMacrosInBuildfile(this,
////            //						macroProvider.areMacrosExpandedInBuildfile(baseCfg));
////
////            //copy user-defined build macros
////            /*			UserDefinedMacroSupplier userMacros = BuildMacroProvider.fUserDefinedMacroSupplier;
////            			userMacros.setMacros(
////            					userMacros.getMacros(BuildMacroProvider.CONTEXT_CONFIGURATION,cloneConfig),
////            					BuildMacroProvider.CONTEXT_CONFIGURATION,
////            					this);
////            */
////            //copy user-defined environment
////            //			UserDefinedEnvironmentSupplier userEnv = EnvironmentVariableProvider.fUserSupplier;
////            //			userEnv.setVariables(
////            //					userEnv.getVariables(cloneConfig), this);
////
////            //		}
////
////            // Hook me up
////            managedProject.addConfiguration(this);
////
////            IBuilder builder = getEditableBuilder();
////            builder.setManagedBuildOn(false);
////
////        }
////        setRebuildState(true);
//}

//@Override
//public boolean supportsBuild(boolean managed) {
//  return supportsBuild(managed, true);
//}
//
//public boolean supportsBuild(boolean managed, boolean checkBuilder) {
//  return true;
//  // IResourceInfo[] rcs = getResourceInfos();
//  // for (int i = 0; i < rcs.length; i++) {
//  // if (!rcs[i].supportsBuild(managed))
//  // return false;
//  // }
//  //
//  // if (checkBuilder) {
//  // IBuilder builder = getBuilder();
//  // if (builder != null && !builder.supportsBuild(managed))
//  // return false;
//  // }
//  //
//  // return true;
//}
//@Override
//public CBuildData getBuildData() {
//  return getBuilder().getBuildData();
//}

///**
//* Create a new extension configuration based on one already defined.
//*
//* @param projectType
//*            The <code>ProjectType</code> the configuration will be
//*            added to.
//* @param parentConfig
//*            The <code>IConfiguration</code> that is the parent
//*            configuration of this configuration
//* @param id
//*            A unique ID for the new configuration.
//*/
//public Configuration(ProjectType projectType, IConfiguration parentConfig, String newID) {
//  //        id = newID;
//  //        this.projectType = projectType;
//  //        isExtensionConfig = true;
//  //
//  //        if (parentConfig != null) {
//  //            name = parentConfig.getName();
//  //            // If this constructor is called to clone an existing
//  //            // configuration, the parent of the parent should be stored.
//  //            // As of 2.1, there is still one single level of inheritance to
//  //            // worry about
//  //            parent = parentConfig.getParent() == null ? parentConfig : parentConfig.getParent();
//  //        }
//  //
//  //        // Hook me up to the ProjectType
//  //        if (projectType != null) {
//  //            projectType.addConfiguration(this);
//  //            // set managedBuildRevision
//  //            setManagedBuildRevision(projectType.getManagedBuildRevision());
//  //        }
//}
//

///**
//* Create a <code>Configuration</code> based on the specification stored in the
//* project file (.cdtbuild).
//*
//* @param managedProject
//*            The <code>ManagedProject</code> the configuration will
//*            be added to.
//* @param element
//*            The XML element that contains the configuration
//*            settings.
//*
//*/
//public Configuration(ManagedProject managedProject, ICStorageElement element, String managedBuildRevision,
//      boolean isPreference) {
//  //        this.managedProject = managedProject;
//  //        this.isPreferenceConfig = isPreference;
//  //        isExtensionConfig = false;
//  //        fCfgData = new BuildConfigurationData(this);
//  //
//  //        setManagedBuildRevision(managedBuildRevision);
//  //
//  //        // Initialize from the XML attributes
//  //        loadFromProject(element);
//  //
//  //        // Hook me up
//  //        if (managedProject != null)
//  //            managedProject.addConfiguration(this);
//  //
//  //        ICStorageElement configElements[] = element.getChildren();
//  //        List<IPath> srcPathList = new ArrayList<>();
//  //        excludeList = new ArrayList<>();
//  //        for (int i = 0; i < configElements.length; ++i) {
//  //            ICStorageElement configElement = configElements[i];
//  //            if (configElement.getName().equals(IToolChain.TOOL_CHAIN_ELEMENT_NAME)) {
//  //                rootFolderInfo = new FolderInfo(this, configElement, managedBuildRevision, false);
//  //                addResourceConfiguration(rootFolderInfo);
//  //            } else if (IFolderInfo.FOLDER_INFO_ELEMENT_NAME.equals(configElement.getName())) {
//  //                FolderInfo resConfig = new FolderInfo(this, configElement, managedBuildRevision, true);
//  //                addResourceConfiguration(resConfig);
//  //            } else if (IFileInfo.FILE_INFO_ELEMENT_NAME.equals(configElement.getName())
//  //                    || IResourceConfiguration.RESOURCE_CONFIGURATION_ELEMENT_NAME.equals(configElement.getName())) {
//  //                ResourceConfiguration resConfig = new ResourceConfiguration(this, configElement, managedBuildRevision);
//  //                addResourceConfiguration(resConfig);
//  //            } else if (SourcePath.ELEMENT_NAME.equals(configElement.getName())) {
//  //                SourcePath p = new SourcePath(configElement);
//  //                if (p.getPath() != null)
//  //                    srcPathList.add(p.getPath());
//  //            } else if (SOURCE_ENTRIES.equals(configElement.getName())) {
//  //                List<ICSettingEntry> seList = LanguageSettingEntriesSerializer.loadEntriesList(configElement,
//  //                        ICSettingEntry.SOURCE_PATH);
//  //                sourceEntries = seList.toArray(new ICSourceEntry[seList.size()]);
//  //            }
//  //        }
//  //
//  //        resolveProjectReferences(true);
//  //
//  //        sourceEntries = createSourceEntries(sourceEntries, srcPathList, excludeList);
//  //
//  //        excludeList = null;
//  //
//  //        PropertyManager mngr = PropertyManager.getInstance();
//  //        String rebuild = mngr.getProperty(this, REBUILD_STATE);
//  //        if (rebuild == null || Boolean.valueOf(rebuild).booleanValue())
//  //            rebuildNeeded = true;
//  //
//  //        String rcChangeState = mngr.getProperty(this, RC_CHANGE_STATE);
//  //        if (rcChangeState == null)
//  //            resourceChangeState = ~0;
//  //        else {
//  //            try {
//  //                resourceChangeState = Integer.parseInt(rcChangeState);
//  //            } catch (NumberFormatException e) {
//  //                resourceChangeState = ~0;
//  //            }
//  //        }
//
//}
//

//
//public Configuration(ManagedProject managedProject, Configuration cloneConfig, String id, boolean cloneChildren,
//      boolean temporary) {
//  //        this(managedProject, cloneConfig, id, cloneChildren, temporary, false);
//  // setArtifactName(managedProject.getDefaultArtifactName());
//}

///**
//* Create a new project, non-extension, configuration based on one already
//* defined.
//*
//* @param managedProject
//*            The <code>ManagedProject</code> the configuration will
//*            be added to.
//* @param cloneConfig
//*            The <code>IConfiguration</code> to copy the settings
//*            from.
//* @param id
//*            A unique ID for the new configuration.
//* @param cloneChildren
//*            If <code>true</code>, the configuration's tools are
//*            cloned
//*/
//public Configuration(ManagedProject managedProject, Configuration cloneConfig, String newID, boolean cloneChildren,
//      boolean temporary, boolean isPreferenceConfig) {
//  //        id = newID;
//  //        name = (cloneConfig.getName());
//  //        this.isPreferenceConfig = isPreferenceConfig;
//  //        this.managedProject = managedProject;
//  //        isExtensionConfig = false;
//  //        this.isTemporary = temporary;
//  //
//  //        copySettingsFrom(cloneConfig, cloneChildren);
//}
//@Override
//public String getBuildCommand() {
//  IToolChain tc = getToolChain();
//  IBuilder builder = tc.getBuilder();
//  if (builder != null) {
//      return builder.getCommand();
//  }
//  return "make"; //$NON-NLS-1$
//}

//@Override
//public List<ITool> getFilteredTools() {
//  //return rootFolderInfo.getFilteredTools();
//  return null;
//}
//@Override
//public ITool getTool(String id) {
//  //return rootFolderInfo.getTool(id);
//  return null;
//}
//@Override
//public List<ITool> getTools() {
//  return myToolchain.getTools();
//}
//
//@Override
//public String getToolCommand(ITool tool) {
//  return tool.getToolCommand();
//}
//@Override
//public String getBuildArguments() {
//  IToolChain tc = getToolChain();
//  IBuilder builder = tc.getBuilder();
//  if (builder != null) {
//      return builder.getArguments();
//  }
//  return "-k"; //$NON-NLS-1$
//}
//@Override
//public boolean isHeaderFile(String ext) {
//  return false;//getRootFolderInfo().isHeaderFile(ext);
//}
//@Override
//public ITool calculateTargetTool() {
//  ITool tool = getTargetTool();
//
//  //		if (tool == null) {
//  //			tool = getToolFromOutputExtension(getArtifactExtension());
//  //		}
//
//  //		if (tool == null) {
//  //			IConfiguration extCfg;
//  //			for (extCfg = this; extCfg != null; extCfg = extCfg.getParent()) {
//  //			}
//  //
//  //			if (extCfg != null) {
//  //				tool = getToolFromOutputExtension(extCfg.getArtifactExtension());
//  //			}
//  //		}
//
//  return tool;
//}

//    @Override
//    public CConfigurationData getConfigurationData() {
//        return fCfgData;
//    }
//@Override
//public ITool getTargetTool() {
////          String[] targetToolIds = rootFolderInfo.getToolChain().getTargetToolList();
////          if (targetToolIds == null || targetToolIds.length == 0)
////              return null;
////  
////          //  For each target tool id, in list order,
////          //  look for a tool with this ID, or a tool with a superclass with this id.
////          //  Stop when we find a match
////          List<ITool> tools = getFilteredTools();
////          for (int i = 0; i < targetToolIds.length; i++) {
////              String targetToolId = targetToolIds[i];
////              for (int j = 0; j < tools.length; j++) {
////                  ITool targetTool = tools[j];
////                  ITool tool = targetTool;
////                  do {
////                      if (targetToolId.equals(tool.getId())) {
////                          return tool;
////                      }
////                      tool = tool.getSuperClass();
////                  } while (tool != null);
////              }
////          }
//  return null;
//}