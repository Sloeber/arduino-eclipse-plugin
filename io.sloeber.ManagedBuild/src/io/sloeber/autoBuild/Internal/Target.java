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
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import org.eclipse.core.resources.IResource;

import io.sloeber.autoBuild.api.IConfigurationV2;
import io.sloeber.autoBuild.api.ITarget;
import io.sloeber.autoBuild.api.ITool;

public class Target extends BuildObject implements ITarget {

    @Override
    public IConfigurationV2 createConfiguration(IConfigurationV2 parent, String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IConfigurationV2 createConfiguration(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getArtifactExtension() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getArtifactName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setDirty(boolean isDirty) {
        // TODO Auto-generated method stub

    }

    @Override
    public String getBinaryParserId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getErrorParserIds() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getErrorParserList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getCleanCommand() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IConfigurationV2[] getConfigurations() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getDefaultExtension() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMakeArguments() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getMakeCommand() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IConfigurationV2 getConfiguration(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IResource getOwner() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ITarget getParent() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getTargetOSList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String[] getTargetArchList() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ITool[] getTools() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ITool getTool(String id) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean hasOverridenMakeCommand() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAbstract() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isDirty() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isTestTarget() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean needsRebuild() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void removeConfiguration(String id) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setArtifactExtension(String extension) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setArtifactName(String name) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setMakeArguments(String makeArgs) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setMakeCommand(String command) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setErrorParserIds(String ids) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setRebuildState(boolean rebuild) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updateOwner(IResource resource) {
        // TODO Auto-generated method stub

    }

    @Override
    public void convertToProjectType(String managedBuildRevision) {
        // TODO Auto-generated method stub

    }

    @Override
    public ProjectType getCreatedProjectType() {
        // TODO Auto-generated method stub
        return null;
    }

    public void resolveReferences() {

    }
    //    private static final String EMPTY_STRING = ""; //$NON-NLS-1$
    //    private String artifactName;
    //    private String binaryParserId;
    //    private String cleanCommand;
    //    private List<IConfigurationV2> configList;
    //    private Map<String, IConfigurationV2> configMap;
    //    private String defaultExtension;
    //    //	private Map depCalculatorsMap;
    //    private String errorParserIds;
    //    private String extension;
    //    private boolean isAbstract = false;
    //    private boolean isDirty = false;
    //    private boolean isTest = false;
    //    private String makeArguments;
    //    private String makeCommand;
    //    private IResource owner;
    //    private ITarget parent;
    //    private boolean resolved = true;
    //    private List<String> targetArchList;
    //    private List<String> targetOSList;
    //    private List<ITool> toolList;
    //    private Map<String, ITool> toolMap;
    //    private List<ToolReference> toolReferences;
    //    private ProjectType createdProjectType;
    //    private String scannerInfoCollectorId;
    //
    //    /**
    //     * This constructor is called to create a target defined by an extension point
    //     * in
    //     * a plugin manifest file.
    //     *
    //     * @param managedBuildRevision
    //     *            the fileVersion of Managed Build System
    //     */
    //    public Target(IManagedConfigElement element, String managedBuildRevision) {
    //        // setup for resolving
    //        ManagedBuildManager.putConfigElement(this, element);
    //        resolved = false;
    //
    //        // id
    //        setId(SafeStringInterner.safeIntern(element.getAttribute(ID)));
    //
    //        // managedBuildRevision
    //        setManagedBuildRevision(SafeStringInterner.safeIntern(managedBuildRevision));
    //
    //        // hook me up
    //        ManagedBuildManager.addExtensionTarget(this);
    //
    //        // Get the target name
    //        setName(SafeStringInterner.safeIntern(element.getAttribute(NAME)));
    //
    //        // Get the name of the build artifact associated with target (usually
    //        // in the plugin specification).
    //        artifactName = SafeStringInterner.safeIntern(element.getAttribute(ARTIFACT_NAME));
    //
    //        // Get the ID of the binary parser
    //        binaryParserId = SafeStringInterner.safeIntern(element.getAttribute(BINARY_PARSER));
    //
    //        // Get the semicolon separated list of IDs of the error parsers
    //        errorParserIds = SafeStringInterner.safeIntern(element.getAttribute(ERROR_PARSERS));
    //
    //        // Get the default extension
    //        defaultExtension = SafeStringInterner.safeIntern(element.getAttribute(DEFAULT_EXTENSION));
    //
    //        // isAbstract
    //        isAbstract = Boolean.parseBoolean(element.getAttribute(IS_ABSTRACT));
    //
    //        // Is this a test target
    //        isTest = Boolean.parseBoolean(element.getAttribute(IS_TEST));
    //
    //        // Get the clean command
    //        cleanCommand = SafeStringInterner.safeIntern(element.getAttribute(CLEAN_COMMAND));
    //
    //        // Get the make command
    //        makeCommand = SafeStringInterner.safeIntern(element.getAttribute(MAKE_COMMAND));
    //
    //        // Get the make arguments
    //        makeArguments = SafeStringInterner.safeIntern(element.getAttribute(MAKE_ARGS));
    //
    //        // Get scannerInfoCollectorId
    //        scannerInfoCollectorId = SafeStringInterner.safeIntern(element.getAttribute(SCANNER_INFO_COLLECTOR_ID));
    //
    //        // Get the comma-separated list of valid OS
    //        String os = element.getAttribute(OS_LIST);
    //        if (os != null) {
    //            targetOSList = new ArrayList<>();
    //            String[] osTokens = os.split(","); //$NON-NLS-1$
    //            for (int i = 0; i < osTokens.length; ++i) {
    //                targetOSList.add(SafeStringInterner.safeIntern(osTokens[i].trim()));
    //            }
    //        }
    //
    //        // Get the comma-separated list of valid Architectures
    //        String arch = element.getAttribute(ARCH_LIST);
    //        if (arch != null) {
    //            targetArchList = new ArrayList<>();
    //            String[] archTokens = arch.split(","); //$NON-NLS-1$
    //            for (int j = 0; j < archTokens.length; ++j) {
    //                targetArchList.add(SafeStringInterner.safeIntern(archTokens[j].trim()));
    //            }
    //        }
    //
    //        // Load any tool references we might have
    //        IManagedConfigElement[] toolRefs = element.getChildren(IConfigurationV2.TOOLREF_ELEMENT_NAME);
    //        for (int k = 0; k < toolRefs.length; ++k) {
    //            new ToolReference(this, toolRefs[k]);
    //        }
    //        // Then load any tools defined for the target
    //        IManagedConfigElement[] tools = element.getChildren(ITool.TOOL_ELEMENT_NAME);
    //        for (int m = 0; m < tools.length; ++m) {
    //            ITool newTool = new Tool(this, tools[m], managedBuildRevision);
    //            // Add this tool to the target, as this is not done in the constructor
    //            this.addTool(newTool);
    //        }
    //        // Then load the configurations which may have tool references
    //        IManagedConfigElement[] configs = element.getChildren(IConfigurationV2.CONFIGURATION_ELEMENT_NAME);
    //        for (int n = 0; n < configs.length; ++n) {
    //            new ConfigurationV2(this, configs[n]);
    //        }
    //    }
    //
    //    /**
    //     * Set the resource that owns the target.
    //     */
    //    protected Target(IResource owner) {
    //        this.owner = owner;
    //    }
    //
    //    /**
    //     * Create a copy of the target specified in the argument,
    //     * that is owned by the owned by the specified resource.
    //     */
    //    public Target(IResource owner, ITarget parent) {
    //        // Make the owner of the target the project resource
    //        this(owner);
    //
    //        // Copy the parent's identity
    //        this.parent = parent;
    //        int id = ManagedBuildManager.getRandomNumber();
    //        setId(owner.getName() + "." + parent.getId() + "." + id); //$NON-NLS-1$ //$NON-NLS-2$
    //        setName(parent.getName());
    //
    //        setManagedBuildRevision(parent.getManagedBuildRevision());
    //
    //        setArtifactName(parent.getArtifactName());
    //        this.binaryParserId = parent.getBinaryParserId();
    //        this.errorParserIds = parent.getErrorParserIds();
    //        this.defaultExtension = parent.getArtifactExtension();
    //        this.isTest = parent.isTestTarget();
    //        this.cleanCommand = parent.getCleanCommand();
    //        this.scannerInfoCollectorId = ((Target) parent).scannerInfoCollectorId;
    //
    //        // Hook me up
    //        IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(owner);
    //        buildInfo.addTarget(this);
    //    }
    //
    //    /**
    //     * Create target from project file.
    //     */
    //    public Target(ManagedBuildInfo buildInfo, Element element) {
    //        this(buildInfo.getOwner());
    //
    //        // id
    //        setId(element.getAttribute(ID));
    //
    //        // hook me up
    //        buildInfo.addTarget(this);
    //
    //        // name
    //        setName(element.getAttribute(NAME));
    //
    //        // Get the name of the build artifact associated with target (should
    //        // contain what the user entered in the UI).
    //        artifactName = element.getAttribute(ARTIFACT_NAME);
    //
    //        // Get the overridden extension
    //        if (element.hasAttribute(EXTENSION)) {
    //            extension = element.getAttribute(EXTENSION);
    //        }
    //
    //        // parent
    //        String parentId = element.getAttribute(PARENT);
    //        if (parentId != null)
    //            parent = ManagedBuildManager.getTarget(null, parentId);
    //
    //        // isAbstract
    //        if (Boolean.parseBoolean(element.getAttribute(IS_ABSTRACT)))
    //            isAbstract = true;
    //
    //        // Is this a test target
    //        isTest = Boolean.parseBoolean(element.getAttribute(IS_TEST));
    //
    //        // Get the clean command
    //        if (element.hasAttribute(CLEAN_COMMAND)) {
    //            cleanCommand = element.getAttribute(CLEAN_COMMAND);
    //        }
    //
    //        // Get the semicolon separated list of IDs of the error parsers
    //        if (element.hasAttribute(ERROR_PARSERS)) {
    //            errorParserIds = element.getAttribute(ERROR_PARSERS);
    //        }
    //
    //        // Get the make command and arguments
    //        if (element.hasAttribute(MAKE_COMMAND)) {
    //            makeCommand = element.getAttribute(MAKE_COMMAND);
    //        }
    //        if (element.hasAttribute(MAKE_ARGS)) {
    //            makeArguments = element.getAttribute(MAKE_ARGS);
    //        }
    //
    //        Node child = element.getFirstChild();
    //        while (child != null) {
    //            if (child.getNodeName().equals(IConfigurationV2.CONFIGURATION_ELEMENT_NAME)) {
    //                new ConfigurationV2(this, (Element) child);
    //            }
    //            child = child.getNextSibling();
    //        }
    //    }
    //
    //    public void addConfiguration(IConfigurationV2 configuration) {
    //        getConfigurationList().add(configuration);
    //        getConfigurationMap().put(configuration.getId(), configuration);
    //    }
    //
    //    /**
    //     * Adds a tool specification to the receiver. This tool is defined
    //     * only for the receiver, and cannot be shared by other targets.
    //     */
    //    public void addTool(ITool tool) {
    //        getToolList().add(tool);
    //        getToolMap().put(tool.getId(), tool);
    //    }
    //
    //    /**
    //     * Adds a tool reference to the receiver.
    //     */
    //    public void addToolReference(ToolReference toolRef) {
    //        getLocalToolReferences().add(toolRef);
    //    }
    //
    //    /**
    //     * Tail-recursion method that creates a lits of tools and tool reference
    //     * walking the receiver's parent hierarchy.
    //     */
    //    private void addToolsToArray(Vector<ITool> toolArray) {
    //        if (parent != null) {
    //            ((Target) parent).addToolsToArray(toolArray);
    //        }
    //
    //        //	Add the tools from out own list
    //        toolArray.addAll(getToolList());
    //
    //        // Add local tool references
    //        toolArray.addAll(getLocalToolReferences());
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.core.build.managed.ITarget#createConfiguration(org.eclipse.cdt.core.build.managed.IConfigurationV2)
    //     */
    //    @Override
    //    public IConfigurationV2 createConfiguration(IConfigurationV2 parent, String id) {
    //        isDirty = true;
    //        return new ConfigurationV2(this, parent, id);
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.core.build.managed.ITarget#createConfiguration()
    //     */
    //    @Override
    //    public IConfigurationV2 createConfiguration(String id) {
    //        return new ConfigurationV2(this, id);
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#getArtifactExtension()
    //     */
    //    @Override
    //    public String getArtifactExtension() {
    //        // Has the user changed the extension for this target
    //        if (extension != null) {
    //            return extension;
    //        }
    //        // If not, then go through the default extension lookup
    //        if (defaultExtension == null) {
    //            // Ask my parent first
    //            if (parent != null) {
    //                return parent.getArtifactExtension();
    //            } else {
    //                return EMPTY_STRING;
    //            }
    //        } else {
    //            return defaultExtension;
    //        }
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.core.build.managed.ITarget#getArtifactName()
    //     */
    //    @Override
    //    public String getArtifactName() {
    //        if (artifactName == null) {
    //            // If I have a parent, ask it
    //            if (parent != null) {
    //                return parent.getArtifactName();
    //            } else {
    //                // I'm it and this is not good!
    //                return EMPTY_STRING;
    //            }
    //        } else {
    //            return artifactName;
    //        }
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#getBinaryParserId()
    //     */
    //    @Override
    //    public String getBinaryParserId() {
    //        if (binaryParserId == null) {
    //            // If I have a parent, ask it
    //            if (parent != null) {
    //                return parent.getBinaryParserId();
    //            } else {
    //                // I'm it and this is not good!
    //                return EMPTY_STRING;
    //            }
    //        }
    //        return binaryParserId;
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.core.build.managed.ITarget#getCleanCommand()
    //     */
    //    @Override
    //    public String getCleanCommand() {
    //        // Return the command used to remove files
    //        if (cleanCommand == null) {
    //            if (parent != null) {
    //                return parent.getCleanCommand();
    //            } else {
    //                // User forgot to specify it. Guess based on OS.
    //                if (Platform.getOS().equals(Platform.OS_WIN32)) {
    //                    return "del"; //$NON-NLS-1$
    //                } else {
    //                    return "rm"; //$NON-NLS-1$
    //                }
    //            }
    //        } else {
    //            // This was spec'd in the manifest
    //            return cleanCommand;
    //        }
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.core.build.managed.ITarget#getConfiguration()
    //     */
    //    @Override
    //    public IConfigurationV2 getConfiguration(String id) {
    //        return getConfigurationMap().get(id);
    //    }
    //
    //    /**
    //     * Safe accessor for the list of configurations.
    //     *
    //     * @return List containing the configurations
    //     */
    //    private List<IConfigurationV2> getConfigurationList() {
    //        if (configList == null) {
    //            configList = new ArrayList<>();
    //        }
    //        return configList;
    //    }
    //
    //    /**
    //     * Safe accessor for the map of configuration ids to configurations
    //     */
    //    private Map<String, IConfigurationV2> getConfigurationMap() {
    //        if (configMap == null) {
    //            configMap = new HashMap<>();
    //        }
    //        return configMap;
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#getConfigurations()
    //     */
    //    @Override
    //    public IConfigurationV2[] getConfigurations() {
    //        return getConfigurationList().toArray(new IConfigurationV2[getConfigurationList().size()]);
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#getDefaultExtension()
    //     */
    //    @Override
    //    public String getDefaultExtension() {
    //        return defaultExtension == null ? EMPTY_STRING : defaultExtension;
    //    }
    //
    //    //	private Map getDepCalcMap() {
    //    //		if (depCalculatorsMap == null) {
    //    //			depCalculatorsMap = new HashMap();
    //    //		}
    //    //		return depCalculatorsMap;
    //    //	}
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#getErrorParserIds()
    //     */
    //    @Override
    //    public String getErrorParserIds() {
    //        if (errorParserIds == null) {
    //            // If I have a parent, ask it
    //            if (parent != null) {
    //                return parent.getErrorParserIds();
    //            }
    //        }
    //        return errorParserIds;
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#getErrorParserList()
    //     */
    //    @Override
    //    public String[] getErrorParserList() {
    //        String parserIDs = getErrorParserIds();
    //        String[] errorParsers = null;
    //        if (parserIDs != null) {
    //            // Check for an empty string
    //            if (parserIDs.length() == 0) {
    //                errorParsers = new String[0];
    //            } else {
    //                StringTokenizer tok = new StringTokenizer(parserIDs, ";"); //$NON-NLS-1$
    //                List<String> list = new ArrayList<>(tok.countTokens());
    //                while (tok.hasMoreElements()) {
    //                    list.add(tok.nextToken());
    //                }
    //                String[] strArr = { "" }; //$NON-NLS-1$
    //                errorParsers = list.toArray(strArr);
    //            }
    //        } else {
    //            // If no error parsers are specified by the target, the default is
    //            // all error parsers
    //            errorParsers = ErrorParserManager.getErrorParserAvailableIdsInContext(ErrorParserManager.BUILD_CONTEXT);
    //        }
    //        return errorParsers;
    //    }
    //
    //    /**
    //     * A safe accesor method. It answers the tool reference list in the
    //     * receiver.
    //     *
    //     * @return List
    //     */
    //    protected List<ToolReference> getLocalToolReferences() {
    //        if (toolReferences == null) {
    //            toolReferences = new ArrayList<>();
    //        }
    //        return toolReferences;
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#getMakeArguments()
    //     */
    //    @Override
    //    public String getMakeArguments() {
    //        if (makeArguments == null) {
    //            // See if it is defined in my parent
    //            if (parent != null) {
    //                return parent.getMakeArguments();
    //            } else {
    //                // No parent and no user setting
    //                return ""; //$NON-NLS-1$
    //            }
    //        }
    //        return makeArguments;
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.core.build.managed.ITarget#getMakeCommand()
    //     */
    //    @Override
    //    public String getMakeCommand() {
    //        // Return the name of the make utility
    //        if (makeCommand == null) {
    //            // If I have a parent, ask it
    //            if (parent != null) {
    //                return parent.getMakeCommand();
    //            } else {
    //                // The user has forgotten to specify a command in the plugin manifest
    //                return "make"; //$NON-NLS-1$
    //            }
    //        } else {
    //            return makeCommand;
    //        }
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.IBuildObject#getName()
    //     */
    //    @Override
    //    public String getName() {
    //        // If I am unnamed, see if I can inherit one from my parent
    //        if (name == null) {
    //            if (parent != null) {
    //                return parent.getName();
    //            } else {
    //                return ""; //$NON-NLS-1$
    //            }
    //        } else {
    //            return name;
    //        }
    //    }
    //
    //    protected List<OptionReference> getOptionReferences(ITool tool) {
    //        List<OptionReference> references = new ArrayList<>();
    //
    //        // Get all the option references I add for this tool
    //        ToolReference toolRef = getToolReference(tool);
    //        if (toolRef != null) {
    //            references.addAll(toolRef.getOptionReferenceList());
    //        }
    //
    //        // See if there is anything that my parents add that I don't
    //        if (parent != null) {
    //            List<OptionReference> refs = ((Target) parent).getOptionReferences(tool);
    //            for (OptionReference ref : refs) {
    //                if (!references.contains(ref)) {
    //                    references.add(ref);
    //                }
    //            }
    //        }
    //
    //        return references;
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#getOwner()
    //     */
    //    @Override
    //    public IResource getOwner() {
    //        return owner;
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#getParent()
    //     */
    //    @Override
    //    public ITarget getParent() {
    //        return parent;
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#getTargetArchList()
    //     */
    //    @Override
    //    public String[] getTargetArchList() {
    //        if (targetArchList == null) {
    //            // Ask parent for its list
    //            if (parent != null) {
    //                return parent.getTargetArchList();
    //            } else {
    //                // I have no parent and no defined list
    //                return new String[] { "all" }; //$NON-NLS-1$
    //            }
    //        }
    //        return targetArchList.toArray(new String[targetArchList.size()]);
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#getTargetOSList()
    //     */
    //    @Override
    //    public String[] getTargetOSList() {
    //        if (targetOSList == null) {
    //            // Ask parent for its list
    //            if (parent != null) {
    //                return parent.getTargetOSList();
    //            } else {
    //                // I have no parent and no defined filter list
    //                return new String[] { "all" }; //$NON-NLS-1$
    //            }
    //        }
    //        return targetOSList.toArray(new String[targetOSList.size()]);
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#getTool(java.lang.String)
    //     */
    //    @Override
    //    public ITool getTool(String id) {
    //        ITool result = null;
    //
    //        // See if receiver has it in list
    //        result = getToolMap().get(id);
    //
    //        // If not, check if parent has it
    //        if (result == null && parent != null) {
    //            result = ((Target) parent).getTool(id);
    //        }
    //
    //        // If not defined in parents, check if defined at all
    //        if (result == null) {
    //            result = ManagedBuildManager.getExtensionTool(id);
    //        }
    //
    //        return result;
    //    }
    //
    //    /**
    //     * A safe accessor method for the list of tools maintained by the
    //     * target
    //     *
    //     */
    //    private List<ITool> getToolList() {
    //        if (toolList == null) {
    //            toolList = new ArrayList<>();
    //        }
    //        return toolList;
    //    }
    //
    //    /**
    //     * A safe accessor for the tool map
    //     *
    //     */
    //    private Map<String, ITool> getToolMap() {
    //        if (toolMap == null) {
    //            toolMap = new HashMap<>();
    //        }
    //        return toolMap;
    //    }
    //
    //    /**
    //     * Returns the reference for a given tool or <code>null</code> if one is not
    //     * found.
    //     */
    //    private ToolReference getToolReference(ITool tool) {
    //        // See if the receiver has a reference to the tool
    //        ToolReference ref = null;
    //        if (tool == null)
    //            return ref;
    //        List<ToolReference> localToolReferences = getLocalToolReferences();
    //        for (ToolReference temp : localToolReferences) {
    //            if (temp.references(tool)) {
    //                ref = temp;
    //                break;
    //            }
    //        }
    //        return ref;
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#getTools()
    //     */
    //    @Override
    //    public ITool[] getTools() {
    //        Vector<ITool> toolArray = new Vector<>();
    //        addToolsToArray(toolArray);
    //        return toolArray.toArray(new ITool[toolArray.size()]);
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#hasMakeCommandOverride()
    //     */
    //    @Override
    //    public boolean hasOverridenMakeCommand() {
    //        // We answer true if the make command or the flags are different
    //        return ((makeCommand != null && !makeCommand.equals(parent.getMakeCommand()))
    //                || (makeArguments != null && !makeArguments.equals(parent.getMakeArguments())));
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.core.build.managed.ITarget#isAbstract()
    //     */
    //    @Override
    //    public boolean isAbstract() {
    //        return isAbstract;
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#isDirty()
    //     */
    //    @Override
    //    public boolean isDirty() {
    //        // If I need saving, just say yes
    //        if (isDirty) {
    //            return true;
    //        }
    //
    //        // Iterate over the configurations and ask them if they need saving
    //        List<IConfigurationV2> configurationList = getConfigurationList();
    //        for (IConfigurationV2 cfgV2 : configurationList) {
    //            if (cfgV2.isDirty()) {
    //                return true;
    //            }
    //        }
    //
    //        return false;
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.core.build.managed.ITarget#isTestTarget()
    //     */
    //    @Override
    //    public boolean isTestTarget() {
    //        return isTest;
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#needsRebuild()
    //     */
    //    @Override
    //    public boolean needsRebuild() {
    //        // Iterate over the configurations and ask them if they need saving
    //        List<IConfigurationV2> configurationList = getConfigurationList();
    //        for (IConfigurationV2 cfgV2 : configurationList) {
    //            if (cfgV2.needsRebuild()) {
    //                return true;
    //            }
    //        }
    //        return false;
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#removeConfiguration(java.lang.String)
    //     */
    //    @Override
    //    public void removeConfiguration(String id) {
    //        // Remove the specified configuration from the list and map
    //        List<IConfigurationV2> configurationList = getConfigurationList();
    //        for (IConfigurationV2 config : configurationList) {
    //            if (config.getId().equals(id)) {
    //                configurationList.remove(config);
    //                getConfigurationMap().remove(id);
    //                isDirty = true;
    //                break;
    //            }
    //        }
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#resetMakeCommand()
    //     */
    //    public void resetMakeCommand() {
    //        // Flag target as dirty if the reset actually changes something
    //        if (makeCommand != null) {
    //            setDirty(true);
    //        }
    //        makeCommand = null;
    //        makeArguments = null;
    //    }
    //
    //    /**
    //     *
    //     */
    //    public void resolveReferences() {
    //        if (!resolved) {
    //            resolved = true;
    //            IManagedConfigElement element = ManagedBuildManager.getConfigElement(this);
    //            // parent
    //            String parentId = SafeStringInterner.safeIntern(element.getAttribute(PARENT));
    //            if (parentId != null) {
    //                parent = ManagedBuildManager.getTarget(null, parentId);
    //                // should resolve before calling methods on it
    //                ((Target) parent).resolveReferences();
    //                // copy over the parents configs
    //                IConfigurationV2[] parentConfigs = parent.getConfigurations();
    //                for (IConfigurationV2 cfgV2 : parentConfigs) {
    //                    addConfiguration(cfgV2);
    //                }
    //            }
    //
    //            // call resolve references on any children
    //            List<ITool> toolList = getToolList();
    //            for (ITool current : toolList) {
    //                ((Tool) current).resolveReferences();
    //            }
    //            List<ToolReference> localToolReferences = getLocalToolReferences();
    //            for (ToolReference current : localToolReferences) {
    //                current.resolveReferences();
    //            }
    //            List<IConfigurationV2> configurationList = getConfigurationList();
    //            for (IConfigurationV2 current : configurationList) {
    //                ((ConfigurationV2) current).resolveReferences();
    //            }
    //        }
    //    }
    //
    //    /**
    //     * Persist receiver to project file.
    //     */
    //    public void serialize(Document doc, Element element) {
    //        element.setAttribute(ID, getId());
    //        element.setAttribute(NAME, getName());
    //        if (parent != null)
    //            element.setAttribute(PARENT, parent.getId());
    //        element.setAttribute(IS_ABSTRACT, isAbstract ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
    //        element.setAttribute(ARTIFACT_NAME, getArtifactName());
    //        if (extension != null) {
    //            element.setAttribute(EXTENSION, extension);
    //        }
    //        element.setAttribute(IS_TEST, isTest ? "true" : "false"); //$NON-NLS-1$ //$NON-NLS-2$
    //
    //        if (makeCommand != null) {
    //            element.setAttribute(MAKE_COMMAND, makeCommand);
    //        } else {
    //            // Make sure we use the default
    //        }
    //
    //        if (makeArguments != null) {
    //            element.setAttribute(MAKE_ARGS, makeArguments);
    //        }
    //        if (errorParserIds != null) {
    //            element.setAttribute(ERROR_PARSERS, errorParserIds);
    //        }
    //
    //        // Serialize the configuration settings
    //        List<IConfigurationV2> configurationList = getConfigurationList();
    //        for (IConfigurationV2 config : configurationList) {
    //            Element configElement = doc.createElement(IConfigurationV2.CONFIGURATION_ELEMENT_NAME);
    //            element.appendChild(configElement);
    //            ((ConfigurationV2) config).serialize(doc, configElement);
    //        }
    //
    //        // I am clean now
    //        isDirty = false;
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#setArtifactExtension(java.lang.String)
    //     */
    //    @Override
    //    public void setArtifactExtension(String extension) {
    //        if (extension != null) {
    //            this.extension = extension;
    //            isDirty = true;
    //        }
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.core.build.managed.ITarget#setArtifactName(java.lang.String)
    //     */
    //    @Override
    //    public void setArtifactName(String name) {
    //        if (name != null) {
    //            artifactName = name;
    //            setRebuildState(true);
    //            isDirty = true;
    //        }
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#setDirty(boolean)
    //     */
    //    @Override
    //    public void setDirty(boolean isDirty) {
    //        // Override the dirty flag here
    //        this.isDirty = isDirty;
    //        // and in the configurations
    //        List<IConfigurationV2> configurationList = getConfigurationList();
    //        for (IConfigurationV2 config : configurationList) {
    //            config.setDirty(isDirty);
    //        }
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#setErrorParserIds()
    //     */
    //    @Override
    //    public void setErrorParserIds(String ids) {
    //        if (ids == null)
    //            return;
    //        String currentIds = getErrorParserIds();
    //        if (currentIds == null || !(currentIds.equals(ids))) {
    //            errorParserIds = ids;
    //            isDirty = true;
    //        }
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#setMakeArguments(java.lang.String)
    //     */
    //    @Override
    //    public void setMakeArguments(String makeArgs) {
    //        if (makeArgs != null && !getMakeArguments().equals(makeArgs)) {
    //            makeArguments = makeArgs;
    //            setRebuildState(true);
    //            isDirty = true;
    //        }
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#setMakeCommand(java.lang.String)
    //     */
    //    @Override
    //    public void setMakeCommand(String command) {
    //        if (command != null && !getMakeCommand().equals(command)) {
    //            makeCommand = command;
    //            setRebuildState(true);
    //            isDirty = true;
    //        }
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#setRebuildState(boolean)
    //     */
    //    @Override
    //    public void setRebuildState(boolean rebuild) {
    //        List<IConfigurationV2> configurationList = getConfigurationList();
    //        for (IConfigurationV2 config : configurationList) {
    //            config.setRebuildState(rebuild);
    //        }
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#updateOwner(org.eclipse.core.resources.IResource)
    //     */
    //    @Override
    //    public void updateOwner(IResource resource) {
    //        if (!resource.equals(owner)) {
    //            // Set the owner correctly
    //            owner = resource;
    //        }
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#convertToProjectType()
    //     */
    //    @Override
    //    public void convertToProjectType(String managedBuildRevision) {
    //        // Create a ProjectType + Configuration + Toolchain + Builder + TargetPlatform
    //        // from the Target
    //
    //        // The "parent" needs to have been converted already.
    //        // Do it now if necessary.
    //        ProjectType parentProj = null;
    //        if (parent != null) {
    //            parentProj = parent.getCreatedProjectType();
    //            if (parentProj == null) {
    //                parent.convertToProjectType(managedBuildRevision);
    //                parentProj = parent.getCreatedProjectType();
    //            }
    //        }
    //        ProjectType projectType = new ProjectType(parentProj, getId(), getName(), managedBuildRevision);
    //        createdProjectType = projectType;
    //        // Set the project type attributes
    //        projectType.setIsAbstract(isAbstract);
    //        projectType.setIsTest(isTest);
    //        // Add children
    //        // Add configurations  (Configuration -> ToolChain -> Builder -> TargetPlatform)
    //        List<IConfigurationV2> configurationList = getConfigurationList();
    //        for (IConfigurationV2 configV2 : configurationList) {
    //            if (configV2.getCreatedConfig() != null)
    //                continue;
    //            // The new config's superClass needs to be the
    //            // Configuration created from the ConfigurationV2 parent...
    //            IConfiguration configSuperClass = null;
    //            IConfigurationV2 parentV2 = configV2.getParent();
    //            if (parentV2 != null) {
    //                configSuperClass = parentV2.getCreatedConfig();
    //            }
    //            String id = configV2.getId();
    //            String name = configV2.getName();
    //            IConfiguration config = projectType.createConfiguration(configSuperClass, id, name);
    //            configV2.setCreatedConfig(config);
    //            // Set the configuration attributes
    //            config.setArtifactName(getArtifactName());
    //            config.setArtifactExtension(getArtifactExtension());
    //            config.setCleanCommand(getCleanCommand());
    //            config.setErrorParserIds(getErrorParserIds());
    //            // Create the Tool-chain
    //            String subId;
    //            String subName;
    //            subId = id + ".toolchain"; //$NON-NLS-1$
    //            subName = name + ".toolchain"; //$NON-NLS-1$
    //            IToolChain toolChain = config.createToolChain(null, subId, subName, true);
    //            // Set the tool chain attributes
    //            toolChain.setIsAbstract(isAbstract);
    //            toolChain.setOSList(getTargetOSList());
    //            toolChain.setArchList(getTargetArchList());
    //            // In target element had a scannerInfoCollector element here which
    //            // is now replaced with scanner config discovery profile id.
    //            // Using the default per project profile for managed make
    //            if (scannerInfoCollectorId != null && scannerInfoCollectorId
    //                    .equals("org.eclipse.cdt.managedbuilder.internal.scannerconfig.DefaultGCCScannerInfoCollector")) //$NON-NLS-1$
    //                toolChain
    //                        .setScannerConfigDiscoveryProfileId(ManagedBuildCPathEntryContainer.MM_PP_DISCOVERY_PROFILE_ID);
    //            // Create the Builder
    //            subId = id + ".builder"; //$NON-NLS-1$
    //            subName = name + ".builder"; //$NON-NLS-1$
    //            IBuilder builder = toolChain.createBuilder(null, subId, subName, true);
    //            // Set the builder attributes
    //            builder.setIsAbstract(isAbstract);
    //            builder.setCommand(getMakeCommand());
    //            builder.setArguments(getMakeArguments());
    //            IManagedConfigElement element = ManagedBuildManager.getConfigElement(this);
    //            if (element instanceof DefaultManagedConfigElement) {
    //                ((Builder) builder).setBuildFileGeneratorElement(
    //                        ((DefaultManagedConfigElement) element).getConfigurationElement());
    //            }
    //            // Create the TargetPlatform
    //            subId = id + ".targetplatform"; //$NON-NLS-1$
    //            subName = name + ".targetplatform"; //$NON-NLS-1$
    //            ITargetPlatform targetPlatform = toolChain.createTargetPlatform(null, subId, subName, true);
    //            // Set the target platform attributes
    //            targetPlatform.setIsAbstract(isAbstract);
    //            targetPlatform.setOSList(getTargetOSList());
    //            targetPlatform.setArchList(getTargetArchList());
    //            targetPlatform.setBinaryParserList(new String[] { getBinaryParserId() }); // Older projects will always have only one binary parser set.
    //
    //            // Handle ConfigurationV2 children (ToolReference)
    //            // The tools references fetched here are strictly local to the configuration,
    //            // so additional work is required to fetch the tool references from the target
    //            IToolReference[] configToolRefs = configV2.getToolReferences();
    //            // Add the "local" tool references (they are direct children of the target and
    //            //  its parent targets)
    //            Vector<IToolReference> targetToolRefs = new Vector<>();
    //            addTargetToolReferences(targetToolRefs);
    //            IToolReference[] toolRefs;
    //            if (targetToolRefs.size() > 0) {
    //                toolRefs = new IToolReference[targetToolRefs.size() + configToolRefs.length];
    //                int i;
    //                for (i = 0; i < configToolRefs.length; ++i) {
    //                    toolRefs[i] = configToolRefs[i];
    //                }
    //                for (IToolReference toolRef : targetToolRefs) {
    //                    toolRefs[i++] = toolRef;
    //                }
    //            } else {
    //                toolRefs = configToolRefs;
    //            }
    //            for (int i = 0; i < toolRefs.length; ++i) {
    //                IToolReference toolRef = toolRefs[i];
    //                subId = id + "." + toolRef.getId(); //$NON-NLS-1$
    //                // The ToolReference's Tool becomes the newTool's SuperClass
    //                ITool newTool = toolChain.createTool(toolRef.getTool(), subId, toolRef.getName(), true);
    //                // Set the tool attributes
    //                newTool.setToolCommand(toolRef.getRawToolCommand());
    //                newTool.setOutputPrefix(toolRef.getRawOutputPrefix());
    //                newTool.setOutputFlag(toolRef.getRawOutputFlag());
    //                newTool.setOutputsAttribute(toolRef.getRawOutputExtensions());
    //                // Handle ToolReference children (OptionReference)
    //                List<OptionReference> optionReferenceList = toolRef.getOptionReferenceList();
    //                for (OptionReference optRef : optionReferenceList) {
    //                    subId = id + "." + optRef.getId(); //$NON-NLS-1$
    //                    IOption newOption = newTool.createOption(optRef.getOption(), subId, optRef.getName(), true);
    //                    // Set the option attributes
    //                    newOption.setValue(optRef.getValue());
    //                    newOption.setValueType(optRef.getValueType());
    //                    ((Option) newOption).setWasOptRef(true);
    //                }
    //            }
    //
    //            // Process the tools in the configuration, adding them to the toolchain
    //            // Tools for a configuration are stored in the enclosing target, so getting
    //            // the tools for the configuration ultimately gets them from the enclosing target
    //            ITool[] configTools = configV2.getTools();
    //            for (int i = 0; i < configTools.length; ++i) {
    //                ITool tool = configTools[i];
    //                // If tool references encountered, they have already been processed, above,
    //                // so ignore them now
    //                if (!(tool instanceof ToolReference)) {
    //                    // See if the toolchain already has a tool with a SuperClass that has an id
    //                    // equal to the tool that we are considering adding to the toolchain; if so,
    //                    // don't add it
    //                    // This case arises when we have added a tool to the toolchain because
    //                    // we processed a ToolReference (above) that references this tool
    //                    // The original tool referenced in the ToolReference becomes the SuperClass
    //                    // of the tool that is created because of the ToolReference
    //                    boolean found = false;
    //                    ITool[] tools = toolChain.getTools();
    //                    ITool currentTool;
    //                    ITool supercurrentTool;
    //                    for (int j = 0; j < tools.length; ++j) {
    //                        currentTool = tools[j];
    //                        supercurrentTool = currentTool.getSuperClass();
    //                        if (supercurrentTool != null) {
    //                            if (supercurrentTool.getId() == tool.getId()) {
    //                                found = true;
    //                                // If this tool was already added to the toolchain because of a
    //                                // ToolReference, then we disconnent this redundant
    //                                // tool from the target by setting the parent to null
    //                                ((Tool) tool).setToolParent(null);
    //                                break;
    //                            }
    //                        }
    //                    }
    //
    //                    if (!found)
    //                        // This tool is not in the toolchain yet, so add it to the toolchain
    //                        ((ToolChain) toolChain).addTool((Tool) tool);
    //
    //                }
    //            }
    //            // Normalize the outputextensions list by adding an empty string for each tool
    //            // which did not have an explicit output file extension specified
    //            ((ToolChain) toolChain).normalizeOutputExtensions();
    //        }
    //    }
    //
    //    /*
    //     *  A target element may contain toolReference elements.  These get applied to all of the configurations
    //     *  of the target.  The method adds the list of this target's local tool references to the passed in vector.
    //     */
    //    public void addTargetToolReferences(Vector toolRefs) {
    //        toolRefs.addAll(getLocalToolReferences());
    //        if (parent != null) {
    //            Target targetParent = (Target) parent;
    //            targetParent.addTargetToolReferences(toolRefs);
    //        }
    //    }
    //
    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.ITarget#getCreatedProjectType()
    //     */
    //    @Override
    //    public ProjectType getCreatedProjectType() {
    //        return createdProjectType;
    //    }
    //
    //    /**
    //     * @return Returns the version.
    //     */
    //    @Override
    //    public Version getVersion() {
    //        if (version == null) {
    //            if (getParent() != null) {
    //                return getParent().getVersion();
    //            }
    //        }
    //        return version;
    //    }
    //
    //    @Override
    //    public void setVersion(Version version) {
    //        // Do nothing
    //    }

}
