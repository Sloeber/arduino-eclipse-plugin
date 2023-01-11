/*******************************************************************************
 * Copyright (c) 2005, 2016 Intel Corporation and others.
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.settings.model.ICSettingBase;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.core.settings.model.extension.CFileData;
import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
import org.eclipse.cdt.internal.core.SafeStringInterner;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.osgi.framework.Version;

import io.sloeber.autoBuild.api.IBuildObject;
import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.api.IFileInfo;
import io.sloeber.autoBuild.api.IFolderInfo;
import io.sloeber.autoBuild.api.IOption;
import io.sloeber.autoBuild.api.IResourceConfiguration;
import io.sloeber.autoBuild.api.IResourceInfo;
import io.sloeber.autoBuild.api.ITool;
import io.sloeber.autoBuild.api.IToolChain;
import io.sloeber.buildProperties.PropertyManager;

public class ResourceConfiguration extends ResourceInfo implements IFileInfo {

    // property name for holding the rebuild state
    private static final String REBUILD_STATE = "rebuildState"; //$NON-NLS-1$

    // Parent and children
    private List<ITool> toolList;
    private Map<String, ITool> toolMap;
    // Managed Build model attributes
    private Integer rcbsApplicability;
    private String toolsToInvoke;
    // Miscellaneous
    private boolean resolved = true;

    /*
     * C O N S T R U C T O R S
     */

    /**
     * This constructor is called to create a resource configuration defined by an
     * extension point in a plugin manifest file, or returned by a dynamic element
     * provider
     *
     * @param parent
     *            The IConfiguration parent of this resource configuration
     * @param element
     *            The resource configuration definition from the manifest file
     *            or a dynamic element provider
     */
    public ResourceConfiguration(IConfiguration parent, IExtensionPoint root, IConfigurationElement element) {
        super(parent, element, true);

        // setup for resolving
        resolved = false;

        setManagedBuildRevision(managedBuildRevision);
        loadFromManifest(element);

        // Hook me up to the Managed Build Manager
        //ManagedBuildManager.addExtensionResourceConfiguration(this);

        // Load the tool children
        IConfigurationElement[] tools = element.getChildren(ITool.TOOL_ELEMENT_NAME);
        for (int n = 0; n < tools.length; ++n) {
            Tool toolChild = new Tool(this, root, tools[n]);
            getToolList().add(toolChild);
            getToolMap().put(toolChild.getId(), toolChild);
        }

    }

    /**
     * Create a <code>ResourceConfiguration</code> based on the specification stored
     * in the project file (.cdtbuild).
     *
     * @param parent
     *            The <code>IConfiguration</code> the resource configuration
     *            will be added to.
     * @param element
     *            The XML element that contains the resource configuration
     *            settings.
     */
    public ResourceConfiguration(IConfiguration parent, ICStorageElement element, String managedBuildRevision) {
        super(parent, element, true);
        setResourceData(new BuildFileData(this));

        setManagedBuildRevision(managedBuildRevision);
        // Initialize from the XML attributes
        loadFromProject(element);

        // Load children
        ICStorageElement configElements[] = element.getChildren();
        for (int i = 0; i < configElements.length; ++i) {
            ICStorageElement configElement = configElements[i];
            if (configElement.getName().equals(ITool.TOOL_ELEMENT_NAME)) {
                Tool tool = new Tool(this, configElement, getManagedBuildRevision());
                addTool(tool);
            }
        }

    }

    public ResourceConfiguration(FolderInfo folderInfo, ITool baseTool, String id, String resourceName, IPath path) {
        super(folderInfo, path, id, resourceName);
        // setParentFolder(folderInfo);
        // setParentFolderId(folderInfo.getId());

        //        isExtensionResourceConfig = folderInfo.isExtensionElement();
        //        if (!isExtensionResourceConfig)
        IFile theFile = null; //TOFIX jaba how to convert the path to a file
        setResourceData(new BuildFileData(this));

        if (folderInfo.getParent() != null)
            setManagedBuildRevision(folderInfo.getParent().getManagedBuildRevision());

        toolsToInvoke = ""; //$NON-NLS-1$
        rcbsApplicability = KIND_DISABLE_RCBS_TOOL;

        // Get file extension.
        if (baseTool != null) {
            if (baseTool.getParentResourceInfo() != folderInfo)
                baseTool = null;
        }
        // Add the resource specific tools to this resource.
        List<ITool> tools = folderInfo.getFilteredTools();
        String subId = ""; //$NON-NLS-1$
        for (ITool tool: tools) {
            if (tool.buildsFileType(theFile)) {
                baseTool = tool;
                break;
            }
        }

        if (baseTool != null) {
            subId = ManagedBuildManager.calculateChildId(baseTool.getId(), null);
            createTool(baseTool, subId, baseTool.getName(), false);
        }
    }

    /**
     * Create a new resource configuration based on one already defined.
     *
     * @param cfg
     *            The <code>IConfiguration</code> the resource configuration
     *            will be added to.
     * @param cloneConfig
     *            The <code>ResourceConfiguration</code> to copy the
     *            settings from.
     * @param id
     *            A unique ID for the new resource configuration.
     */
    public ResourceConfiguration(IConfiguration cfg, ResourceConfiguration cloneConfig, String id,
            Map<IPath, Map<String, String>> superClassIdMap, boolean cloneChildren) {
        super(cfg, cloneConfig, id);

        //        isExtensionResourceConfig = cfg.isExtensionElement();
        //        if (!cloneConfig.isExtensionResourceConfig)
        cloneChildren = true;

        //        if (!isExtensionResourceConfig)
        setResourceData(new BuildFileData(this));

        setManagedBuildRevision(cloneConfig.getManagedBuildRevision());

        // Copy the remaining attributes
        if (cloneConfig.toolsToInvoke != null) {
            toolsToInvoke = cloneConfig.toolsToInvoke;
        }
        if (cloneConfig.rcbsApplicability != null) {
            rcbsApplicability = cloneConfig.rcbsApplicability;
        }

        boolean copyIds = cloneChildren && id.equals(cloneConfig.id);
        // Clone the resource configuration's tool children
        if (cloneConfig.toolList != null) {
            for (ITool toolChild : cloneConfig.getToolList()) {
                String subId = null;
                String subName;

                Map<String, String> curIdMap = superClassIdMap.get(cloneConfig.getPath());
                ITool extTool = null; //JAVA toFix ManagedBuildManager.getExtensionTool(toolChild);
                if (curIdMap != null) {
                    if (extTool != null) {
                        subId = curIdMap.get(extTool.getId());
                    }
                }

                subName = toolChild.getName();

                if (subId == null) {
                    if (extTool != null) {
                        subId = copyIds ? toolChild.getId()
                                : ManagedBuildManager.calculateChildId(extTool.getId(), null);
                        // subName = toolChild.getSuperClass().getName();
                    } else {
                        subId = copyIds ? toolChild.getId()
                                : ManagedBuildManager.calculateChildId(toolChild.getId(), null);
                        // subName = toolChild.getName();
                    }
                }

                // The superclass for the cloned tool is not the same as the one from the tool
                // being cloned.
                // The superclasses reside in different configurations.
                ITool toolSuperClass = null;
                String superId = null;
                // Search for the tool in this configuration that has the same grand-superClass
                // as the
                // tool being cloned
                ITool otherSuperTool = toolChild.getSuperClass();
                if (otherSuperTool != null) {
                    if (otherSuperTool.isExtensionElement()) {
                        toolSuperClass = otherSuperTool;
                    } else {
                        IResourceInfo otherRcInfo = otherSuperTool.getParentResourceInfo();
                        IResourceInfo thisRcInfo = cfg.getResourceInfo(otherRcInfo.getPath(), true);
                        ITool otherExtTool = null;//JABA TOFIX  ManagedBuildManager.getExtensionTool(otherSuperTool);
                        if (otherExtTool != null) {
                            if (thisRcInfo != null) {
                                List<ITool> tools = thisRcInfo.getTools();
                                for (ITool tool: tools) {
                                    ITool thisExtTool = null;//TOFIX JABA  ManagedBuildManager.getExtensionTool(tool);
                                    if (otherExtTool.equals(thisExtTool)) {
                                        toolSuperClass =tool;
                                        superId = toolSuperClass.getId();
                                        break;
                                    }
                                }
                            } else {
                                superId = copyIds ? otherSuperTool.getId()
                                        : ManagedBuildManager.calculateChildId(otherExtTool.getId(), null);
                                Map<String, String> idMap = superClassIdMap.get(otherRcInfo.getPath());
                                if (idMap == null) {
                                    idMap = new HashMap<>();
                                    superClassIdMap.put(otherRcInfo.getPath(), idMap);
                                }
                                idMap.put(otherExtTool.getId(), superId);
                            }
                        }
                    }
                }
                // IToolChain tCh = cloneConfig.getBaseToolChain();
                // if(tCh != null){
                // if(!tCh.isExtensionElement()){
                // IFolderInfo fo = tCh.getParentFolderInfo();
                // IPath path = fo.getPath();
                // IResourceInfo baseFo = cfg.getResourceInfo(path, false);
                // if(baseFo instanceof IFileInfo)
                // baseFo = cfg.getResourceInfo(path.removeLastSegments(1), false);
                // tCh = ((IFolderInfo)baseFo).getToolChain();
                //
                // }
                // ITool[] tools = tCh.getTools();
                // for (int i=0; i<tools.length; i++) {
                // ITool configTool = tools[i];
                // if (toolChild.getSuperClass() != null
                // && configTool.getSuperClass() == toolChild.getSuperClass().getSuperClass())
                // {
                // toolSuperClass = configTool;
                // break;
                // }
                // }
                // } else {
                // //TODO:
                // }

                Tool newTool = null;
                if (toolSuperClass != null)
                    newTool = new Tool(this, toolSuperClass, subId, subName, (Tool) toolChild);
                else
                    newTool = new Tool(this, superId, subId, subName, (Tool) toolChild);

                addTool(newTool);
            }
        }

        if (copyIds) {
            isDirty = cloneConfig.isDirty;
            needsRebuild = cloneConfig.needsRebuild;
        }
    }

    public ResourceConfiguration(ResourceConfiguration baseInfo, IPath path, String id, String name) {
        super(baseInfo, path, id, name);

        setResourceData(new BuildFileData(this));

        setManagedBuildRevision(baseInfo.getManagedBuildRevision());

        // Copy the remaining attributes
        toolsToInvoke = baseInfo.toolsToInvoke;

        rcbsApplicability = KIND_DISABLE_RCBS_TOOL;

        // Clone the resource configuration's tool children
        if (baseInfo.toolList != null) {
            for (ITool toolChild : baseInfo.getToolList()) {
                ITool superTool = toolChild.getSuperClass();
                String baseId = superTool != null ? superTool.getId() : toolChild.getId();
                String subId = ManagedBuildManager.calculateChildId(baseId, null);
                String subName = toolChild.getName();

                Tool newTool = new Tool(this, superTool, subId, subName, (Tool) toolChild);
                addTool(newTool);
            }
        }

    }

    /*
     * E L E M E N T A T T R I B U T E R E A D E R S A N D W R I T E R S
     */

    /*
     * (non-Javadoc) Loads the resource configuration information from the
     * ManagedConfigElement specified in the argument.
     *
     * @param element Contains the resource configuration information
     */
    protected void loadFromManifest(IConfigurationElement element) {
        //  ManagedBuildManager.putConfigElement(this, element);

        // toolsToInvoke
        toolsToInvoke = SafeStringInterner.safeIntern(element.getAttribute(IResourceConfiguration.TOOLS_TO_INVOKE));

        // rcbsApplicability
        String rcbsApplicabilityStr = element.getAttribute(IResourceConfiguration.RCBS_APPLICABILITY);
        if (rcbsApplicabilityStr == null || rcbsApplicabilityStr.equals(DISABLE_RCBS_TOOL)) {
            rcbsApplicability = KIND_DISABLE_RCBS_TOOL;
        } else if (rcbsApplicabilityStr.equals(APPLY_RCBS_TOOL_BEFORE)) {
            rcbsApplicability = KIND_APPLY_RCBS_TOOL_BEFORE;
        } else if (rcbsApplicabilityStr.equals(APPLY_RCBS_TOOL_AFTER)) {
            rcbsApplicability = KIND_APPLY_RCBS_TOOL_AFTER;
        } else if (rcbsApplicabilityStr.equals(APPLY_RCBS_TOOL_AS_OVERRIDE)) {
            rcbsApplicability = KIND_APPLY_RCBS_TOOL_AS_OVERRIDE;
        }
    }

    /*
     * (non-Javadoc) Initialize the resource configuration information from the XML
     * element specified in the argument
     *
     * @param element An XML element containing the resource configuration
     * information
     */
    protected void loadFromProject(ICStorageElement element) {
        // toolsToInvoke
        if (element.getAttribute(IResourceConfiguration.TOOLS_TO_INVOKE) != null) {
            toolsToInvoke = SafeStringInterner.safeIntern(element.getAttribute(IResourceConfiguration.TOOLS_TO_INVOKE));
        }

        // rcbsApplicability
        if (element.getAttribute(IResourceConfiguration.RCBS_APPLICABILITY) != null) {
            String rcbsApplicabilityStr = element.getAttribute(IResourceConfiguration.RCBS_APPLICABILITY);
            if (rcbsApplicabilityStr == null || rcbsApplicabilityStr.equals(DISABLE_RCBS_TOOL)) {
                rcbsApplicability = KIND_DISABLE_RCBS_TOOL;
            } else if (rcbsApplicabilityStr.equals(APPLY_RCBS_TOOL_BEFORE)) {
                rcbsApplicability = KIND_APPLY_RCBS_TOOL_BEFORE;
            } else if (rcbsApplicabilityStr.equals(APPLY_RCBS_TOOL_AFTER)) {
                rcbsApplicability = KIND_APPLY_RCBS_TOOL_AFTER;
            } else if (rcbsApplicabilityStr.equals(APPLY_RCBS_TOOL_AS_OVERRIDE)) {
                rcbsApplicability = KIND_APPLY_RCBS_TOOL_AS_OVERRIDE;
            }
        }
    }

    /*
     * P A R E N T A N D C H I L D H A N D L I N G
     */

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.core.build.managed.IResourceConfiguration#getTools()
     */
    @Override
    public List<ITool> getTools() {
        return  getToolList();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.cdt.managedbuilder.core.IResourceConfiguration#getTool(java.lang.
     * String)
     */
    @Override
    public ITool getTool(String id) {
        Tool tool = (Tool) getToolMap().get(id);
        return tool;
    }

    /*
     * (non-Javadoc) Safe accessor for the list of tools.
     *
     * @return List containing the tools
     */
    private List<ITool> getToolList() {
        if (toolList == null) {
            toolList = new ArrayList<>();
        }
        return toolList;
    }

    /*
     * (non-Javadoc) Safe accessor for the map of tool ids to tools
     *
     * @return
     */
    private Map<String, ITool> getToolMap() {
        if (toolMap == null) {
            toolMap = new HashMap<>();
        }
        return toolMap;
    }

    /*
     * (non-Javadoc) Adds the Tool to the Tool list and map
     *
     * @param Tool
     */
    public void addTool(Tool tool) {
        getToolList().add(tool);
        getToolMap().put(tool.getId(), tool);
    }

    /*
     * (non-Javadoc) Removes the Tool from the Tool list and map
     *
     * @param Tool
     */
    @Override
    public void removeTool(ITool tool) {
        getToolList().remove(tool);
        getToolMap().remove(tool);
    }

    /*
     * M O D E L A T T R I B U T E A C C E S S O R S
     */

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.cdt.managedbuilder.core.IResourceConfiguration#getResourcePath()
     */
    @Override
    public String getResourcePath() {
        IPath path = getParent().getOwner().getProject().getFullPath();
        path = path.append(getPath());
        return path.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.managedbuilder.core.IResourceConfiguration#
     * getRcbsApplicability()
     */
    @Override
    public int getRcbsApplicability() {
        /*
         * rcbsApplicability is an integer constant that represents how the user wants
         * to order the application of a resource custom build step tool. Defaults to
         * disable rcbs tool. Choices are before, after, or override other tools, or
         * disable rcbs tool.
         */
        if (rcbsApplicability == null) {
            return KIND_DISABLE_RCBS_TOOL;
        }
        return rcbsApplicability.intValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.cdt.managedbuilder.core.IResourceConfiguration#getToolsToInvoke()
     */
    @Override
    public List<ITool> getToolsToInvoke() {
        /*
         * toolsToInvoke is an ordered list of tool ids for the currently defined tools
         * in the resource configuration. Defaults to all tools in the order found.
         * Modified by the presence of an rcbs tool and the currently assigned
         * applicability of that tool. The attribute is implemented as a String of a
         * semicolon separated list of tool ids. An empty string implies treat as if no
         * resource configuration, i.e., use project level tool. This getter routine
         * returns an ITool[] to consumers (i.e., the makefile generator).
         */
        String t_ToolsToInvoke = ""; //$NON-NLS-1$
        List<ITool> resConfigTools;
        List<ITool> tools=new LinkedList<>();

        resConfigTools = getTools();

        /*
         * Evaluate the tools currently defined in the resource configuration. Update
         * the current state of the toolsToInvoke attribute. Build and return an ITool[]
         * for consumers.
         */

        /*
         * If no tools are currently defined, return a zero lengh array of ITool.
         */
        if (resConfigTools.size() == 0) {
        	toolsToInvoke="";
            return resConfigTools;
        }

        /*
         * See if there is an rcbs tool defined. There should only be one at most.
         */
        String rcbsToolId = null;
        ITool rcbsToolIdx = null;
        for (ITool resConfigTool: resConfigTools) {
            if (resConfigTool.getCustomBuildStep() && !resConfigTool.isExtensionElement()) {
                rcbsToolId = resConfigTool.getId();
                rcbsToolIdx = resConfigTool;
                break;
            }
        }
        if (rcbsToolId!=null) {
            /*
             * Here if an rcbs tool is defined. Apply the tools according to the current
             * rcbsApplicability setting.
             */
            switch (rcbsApplicability.intValue()) {
            case KIND_APPLY_RCBS_TOOL_AS_OVERRIDE:
                toolsToInvoke = rcbsToolId;
                tools.add(rcbsToolIdx);
                return tools;
            case KIND_APPLY_RCBS_TOOL_AFTER:
                for (ITool curtool: resConfigTools) {
                    if (curtool.getId() != rcbsToolId) {
                        t_ToolsToInvoke += curtool.getId() + ";"; //$NON-NLS-1$
                        tools.add(curtool);
                    }
                }
                t_ToolsToInvoke += rcbsToolId;
                tools.add(rcbsToolIdx);
                toolsToInvoke = t_ToolsToInvoke;
                return tools;
            case KIND_APPLY_RCBS_TOOL_BEFORE:
                t_ToolsToInvoke = rcbsToolId ;
                tools.add(rcbsToolIdx);
                for (ITool curtool: resConfigTools) {
                    if (curtool.getId() != rcbsToolId) {
                        t_ToolsToInvoke +=  ";"+curtool.getId() ; //$NON-NLS-1$
                        tools.add(curtool);
                    }
                }
                toolsToInvoke = t_ToolsToInvoke;
                return tools;
            case KIND_DISABLE_RCBS_TOOL:
                /*
                 * If the rcbs tool is the only tool and the user has disabled it, there are no
                 * tools to invoke in the resource configuration.
                 */
                if (resConfigTools.size() == 1) {
                    toolsToInvoke = ""; //$NON-NLS-1$
                    
                }else {
                String prepend="";
                for (ITool curtool: resConfigTools) {
                	if (curtool.getId() != rcbsToolId) {
                        t_ToolsToInvoke += prepend +curtool.getId() ; //$NON-NLS-1$
                        prepend=";";
                        tools.add(curtool);
                    }
                }
                toolsToInvoke = t_ToolsToInvoke;}
                return tools;
            default:
                /*
                 * If we get an unexpected value, apply all tools in the order found.
                 */
                
            }
        } 
        String prepend="";
        for (ITool curtool: resConfigTools) {
        	if (curtool.getId() != rcbsToolId) {
                t_ToolsToInvoke += prepend +curtool.getId() ; //$NON-NLS-1$
                prepend=";";
                tools.add(curtool);
            }
        }
        toolsToInvoke = t_ToolsToInvoke;
        return tools;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.cdt.managedbuilder.core.IResourceConfiguration#
     * getRcbsApplicability()
     */
    @Override
    public void setRcbsApplicability(int newValue) {
        /*
         * rcbsApplicability is an integer constant that represents how the user wants
         * to order the application of a resource custom build step tool. Defaults to
         * override all other tools. Choices are before, after, or override other tools,
         * or disable rcbs tool.
         */
        if (rcbsApplicability == null || !(rcbsApplicability.intValue() == newValue)) {
            rcbsApplicability = newValue;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.cdt.managedbuilder.core.IResourceConfiguration#setResourcePath()
     */
    @Override
    public void setResourcePath(String path) {
        if (path == null)
            return;
        IPath p = new Path(path).removeFirstSegments(1);
  //      setPath(p);
    }

    @Override
    public ITool createTool(ITool superClass, String id, String name, boolean isExtensionElement) {
        Tool tool = new Tool(this, superClass, id, name, isExtensionElement);
        addTool(tool);
        return tool;
    }



    // private IBuildObject getHoldersParent(IOption option) {
    // IHoldsOptions holder = option.getOptionHolder();
    // if (holder instanceof ITool) {
    // return ((ITool)holder).getParent();
    // } else if (holder instanceof IToolChain) {
    // return ((IToolChain)holder).getParent();
    // }
    // return null;
    // }

    @Override
    public IResource getOwner() {
        return getParent().getOwner();
    }

    /**
     * @return Returns the version.
     */
    @Override
    public Version getVersion() {
        if (version == null) {
            if (getParent() != null) {
                return getParent().getVersion();
            }
        }
        return version;
    }

    @Override
    public void setVersion(Version version) {
        // Do nothing
    }

    @Override
    public final int getKind() {
        return ICSettingBase.SETTING_FILE;
    }

    @Override
    public CFileData getFileData() {
        return (CFileData) getResourceData();
    }

    @Override
    public CLanguageData[] getCLanguageDatas() {
        List<ITool> tools= getTools/* ToInvoke */();
        List<CLanguageData> list = new ArrayList<>();
        for (ITool tool : tools) {
            CLanguageData datas[] = tool.getCLanguageDatas();
            for (int j = 0; j < datas.length; j++) {
                list.add(datas[j]);
            }
        }
        return list.toArray(new BuildLanguageData[list.size()]);
    }

    @Override
    public IToolChain getBaseToolChain() {
    	List<ITool> tools = getToolsToInvoke();
        ITool baseTool = null;
        for (ITool tool:tools) {
            ITool superTool = tool.getSuperClass();
            if (superTool != null) {
                baseTool = superTool;
                if (!superTool.isExtensionElement()) {
                    break;
                }
            }
        }

        IToolChain baseTc = null;
        if (baseTool != null) {
            IBuildObject parent = baseTool.getParent();
            if (parent instanceof IToolChain) {
                baseTc = (IToolChain) parent;
            } else if (parent instanceof ResourceConfiguration) {
                baseTc = ((ResourceConfiguration) parent).getBaseToolChain();
            }
        }

        return baseTc;
    }

    @Override
    public boolean supportsBuild(boolean managed) {
    	List<ITool> tools = getToolsToInvoke();
        for (ITool tool: tools) {
            if (!tool.supportsBuild(managed))
                return false;
        }

        return true;
    }

    @Override
    void resolveProjectReferences(boolean onLoad) {
        for (ITool tool : getToolList()) {
            ((Tool) tool).resolveProjectReferences(onLoad);
        }
    }

    @Override
    public boolean hasCustomSettings() {
    	return true;
//        IResourceInfo parentRc = getParentResourceInfo();
//        if (parentRc instanceof FolderInfo) {
//            IPath path = getPath();
//            String ext = path.getFileExtension();
//            if (ext == null)
//                ext = ""; //$NON-NLS-1$
//            ITool otherTool = ((FolderInfo) parentRc).getToolFromInputExtension(ext);
//            if (otherTool == null)
//                return true;
//
//            List<ITool> tti = getToolsToInvoke();
//            if (tti.size() != 1)
//                return true;
//
//            return ((Tool) tti[0]).hasCustomSettings((Tool) otherTool);
//        }
//        List<ITool> tools = getTools();
//        List<ITool>  otherTools = ((IFileInfo) parentRc).getTools();
//        if (tools.size() != otherTools.size())
//            return true;
//
////        for (int i = 0; i < tools.length; i++) {
////            Tool tool = (Tool) tools[i];
////            Tool otherTool = (Tool) otherTools[i];
////            if (tool.hasCustomSettings(otherTool))
//                return true;
////        }
//
//        return false;
    }

    @Override
    public void setTools(List<ITool> tools) {
        ToolListModificationInfo info = getToolListModificationInfo(tools);
        info.apply();
    }

    @Override
    public boolean isFolderInfo() {
        return false;
    }

    @Override
    public boolean isSupported() {
        IFolderInfo foInfo = getParentFolderInfo();
        if (foInfo == null) {
            IConfiguration cfg = getParent();
            if (cfg != null) {
                foInfo = cfg.getRootFolderInfo();
            }
        }

        if (foInfo != null)
            return foInfo.isSupported();
        return false;
    }

}
