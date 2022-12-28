/*******************************************************************************
 * Copyright (c) 2007, 2016 Intel Corporation and others.
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
 * cartu38 opendev (STMicroelectronics) - [514385] Custom defaultValue-generator support
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.settings.model.ICSettingBase;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.core.settings.model.extension.CFolderData;
import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
//import org.eclipse.cdt.managedb
//import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyType;
//import org.eclipse.cdt.managedbuilder.core.BuildException;
//import org.eclipse.cdt.managedbuilder.core.IBuildObject;
//import org.eclipse.cdt.managedbuilder.core.IBuildObjectProperties;
//import org.eclipse.cdt.managedbuilder.core.IBuildPropertiesRestriction;
//import org.eclipse.cdt.managedbuilder.core.IConfiguration;
//import org.eclipse.cdt.managedbuilder.core.IFolderInfo;
//import org.eclipse.cdt.managedbuilder.core.IHoldsOptions;
//import org.eclipse.cdt.managedbuilder.core.IInputType;
//import org.eclipse.cdt.managedbuilder.core.IManagedConfigElement;
//import org.eclipse.cdt.managedbuilder.core.IManagedProject;
//import org.eclipse.cdt.managedbuilder.core.IModificationStatus;
//import org.eclipse.cdt.managedbuilder.core.IOption;
//import org.eclipse.cdt.managedbuilder.core.IOutputType;
//import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
//import org.eclipse.cdt.managedbuilder.core.ITargetPlatform;
//import org.eclipse.cdt.managedbuilder.core.ITool;
//import org.eclipse.cdt.managedbuilder.core.IToolChain;
//import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
//import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
//import org.eclipse.cdt.managedbuilder.internal.buildproperties.BuildPropertyManager;
//import org.eclipse.cdt.managedbuilder.internal.dataprovider.BuildFolderData;
//import org.eclipse.cdt.managedbuilder.internal.dataprovider.BuildLanguageData;
//import org.eclipse.cdt.managedbuilder.internal.dataprovider.ConfigurationDataProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.autoBuild.api.IBuildObjectProperties;
import io.sloeber.autoBuild.api.IBuildPropertiesRestriction;
import io.sloeber.autoBuild.api.IBuildProperty;
import io.sloeber.autoBuild.api.IBuildPropertyType;
import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.api.IFolderInfo;
import io.sloeber.autoBuild.api.IHoldsOptions;
import io.sloeber.autoBuild.api.IInputType;
import io.sloeber.autoBuild.api.IManagedConfigElement;
import io.sloeber.autoBuild.api.IManagedProject;
import io.sloeber.autoBuild.api.IModificationStatus;
import io.sloeber.autoBuild.api.IOption;
import io.sloeber.autoBuild.api.IOutputType;
import io.sloeber.autoBuild.api.ITargetPlatform;
import io.sloeber.autoBuild.api.ITool;
import io.sloeber.autoBuild.api.IToolChain;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.buildProperties.BuildPropertyManager;

public class FolderInfo extends ResourceInfo implements IFolderInfo {
    private ToolChain toolChain;
    private boolean isExtensionElement;
    private boolean containsDiscoveredScannerInfo = true;

    public FolderInfo(FolderInfo folderInfo, String id, String resourceName, IPath path) {
        super(folderInfo, path, id, resourceName);

        isExtensionElement = folderInfo.isExtensionElement();
        if (!isExtensionElement)
            setResourceData(new BuildFolderData(this));

        if (folderInfo.getParent() != null)
            setManagedBuildRevision(folderInfo.getParent().getManagedBuildRevision());

        IToolChain parTc = folderInfo.getToolChain();
        IToolChain extTc = ManagedBuildManager.getExtensionToolChain(parTc);
        if (extTc == null)
            extTc = parTc;

        String tcId = ManagedBuildManager.calculateChildId(extTc.getId(), null);
        createToolChain(extTc, tcId, parTc.getName(), false);

        toolChain.createOptions(parTc);
        toolChain.setUnusedChildren(parTc.getUnusedChildren());

        ITool tools[] = parTc.getTools();
        String subId = ""; //$NON-NLS-1$
        for (ITool tool : tools) {
            ITool extTool = ManagedBuildManager.getExtensionTool(tool);
            if (extTool == null)
                extTool = tool;

            subId = ManagedBuildManager.calculateChildId(extTool.getId(), null);
            toolChain.createTool(tool, subId, tool.getName(), false);
        }
        setDirty(true);
        setRebuildState(true);
    }

    public FolderInfo(IConfiguration parent, IManagedConfigElement element, String managedBuildRevision,
            boolean hasBody) {
        super(parent, element, hasBody);

        isExtensionElement = true;
        IManagedConfigElement tcEl = null;
        if (!hasBody) {
            setPath(Path.ROOT);
            setId(ManagedBuildManager.calculateChildId(parent.getId(), null));
            setName("/"); //$NON-NLS-1$
            tcEl = element;
        } else {
            IManagedConfigElement children[] = element.getChildren(IToolChain.TOOL_CHAIN_ELEMENT_NAME);
            if (children.length > 0)
                tcEl = children[0];
        }

        if (tcEl != null)
            toolChain = new ToolChain(this, tcEl, managedBuildRevision);

    }

    public FolderInfo(IConfiguration parent, ICStorageElement element, String managedBuildRevision, boolean hasBody) {
        super(parent, element, hasBody);

        isExtensionElement = false;
        setResourceData(new BuildFolderData(this));
        ICStorageElement tcEl = null;
        if (!hasBody) {
            setPath(Path.ROOT);
            setId(ManagedBuildManager.calculateChildId(parent.getId(), null));
            setName("/"); //$NON-NLS-1$
            tcEl = element;
        } else {
            ICStorageElement nodes[] = element.getChildren();
            for (ICStorageElement node : nodes) {
                if (IToolChain.TOOL_CHAIN_ELEMENT_NAME.equals(node.getName()))
                    tcEl = node;
            }
        }

        if (tcEl != null)
            toolChain = new ToolChain(this, tcEl, managedBuildRevision);
    }

    /*TODO
    	public FolderInfo(FolderInfo base, IPath path, String id, String name) {
    		super(base, path, id, name);
    	}
    */
    public FolderInfo(IConfiguration parent, IPath path, String id, String name, boolean isExtensionElement) {
        super(parent, path, id, name);

        this.isExtensionElement = isExtensionElement;
        if (!isExtensionElement)
            setResourceData(new BuildFolderData(this));

    }

    public FolderInfo(IConfiguration cfg, FolderInfo cloneInfo, String id, Map<IPath, Map<String, String>> superIdMap,
            boolean cloneChildren) {
        super(cfg, cloneInfo, id);

        isExtensionElement = cfg.isExtensionElement();
        if (!isExtensionElement)
            setResourceData(new BuildFolderData(this));

        String subName;
        if (!cloneInfo.isExtensionElement)
            cloneChildren = true;

        boolean copyIds = cloneChildren && id.equals(cloneInfo.id);

        IToolChain cloneToolChain = cloneInfo.getToolChain();
        IToolChain extToolChain = ManagedBuildManager.getExtensionToolChain(cloneToolChain);
        if (extToolChain == null)
            extToolChain = cloneToolChain;

        subName = cloneToolChain.getName();

        if (cloneChildren) {
            String subId = copyIds ? cloneToolChain.getId()
                    : ManagedBuildManager.calculateChildId(extToolChain.getId(), null);
            toolChain = new ToolChain(this, subId, subName, superIdMap, (ToolChain) cloneToolChain);

        } else {
            // Add a tool-chain element that specifies as its superClass the
            // tool-chain that is the child of the configuration.
            String subId = ManagedBuildManager.calculateChildId(extToolChain.getId(), null);
            IToolChain newChain = createToolChain(extToolChain, subId, extToolChain.getName(), false);

            // For each option/option category child of the tool-chain that is
            // the child of the selected configuration element, create an option/
            // option category child of the cloned configuration's tool-chain element
            // that specifies the original tool element as its superClass.
            newChain.createOptions(extToolChain);

            // For each tool element child of the tool-chain that is the child of
            // the selected configuration element, create a tool element child of
            // the cloned configuration's tool-chain element that specifies the
            // original tool element as its superClass.
            ITool[] tools = extToolChain.getTools();
            for (ITool tool : tools) {
                Tool toolChild = (Tool) tool;
                subId = ManagedBuildManager.calculateChildId(toolChild.getId(), null);
                newChain.createTool(toolChild, subId, toolChild.getName(), false);
            }

            ITargetPlatform tpBase = cloneInfo.getToolChain().getTargetPlatform();
            ITargetPlatform extTp = tpBase;
            for (; extTp != null && !extTp.isExtensionElement(); extTp = extTp.getSuperClass()) {
                // empty body, loop is to find extension element only
            }

            TargetPlatform tp;
            if (extTp != null) {
                int nnn = ManagedBuildManager.getRandomNumber();
                subId = copyIds ? tpBase.getId() : extTp.getId() + "." + nnn; //$NON-NLS-1$
                tp = new TargetPlatform(newChain, subId, tpBase.getName(), (TargetPlatform) tpBase);
            } else {
                subId = copyIds ? tpBase.getId() : ManagedBuildManager.calculateChildId(getId(), null);
                subName = tpBase != null ? tpBase.getName() : ""; //$NON-NLS-1$
                tp = new TargetPlatform((ToolChain) newChain, null, subId, subName, false);
            }

            ((ToolChain) newChain).setTargetPlatform(tp);
        }

        if (isRoot())
            containsDiscoveredScannerInfo = cloneInfo.containsDiscoveredScannerInfo;

        if (copyIds) {
            isDirty = cloneInfo.isDirty;
            needsRebuild = cloneInfo.needsRebuild;
        } else {
            setDirty(true);
            setRebuildState(true);
        }

    }

    private boolean conflictsWithRootTools(ITool tool) {
        IFolderInfo rf = getParent().getRootFolderInfo();
        ITool[] rootTools = rf.getFilteredTools();
        ITool tt = getParent().getTargetTool();
        for (ITool rootTool : rootTools) {
            if (rootTool == tt || getMultipleOfType(rootTool) != null) {
                if (getConflictingInputExts(rootTool, tool).length != 0)
                    return true;
            }
        }
        return false;
    }

    private IInputType getMultipleOfType(ITool tool) {
        IInputType[] types = tool.getInputTypes();
        IInputType mType = null;
        boolean foundNonMultiplePrimary = false;
        for (IInputType type : types) {
            if (type.getMultipleOfType()) {
                if (type.getPrimaryInput() == true) {
                    foundNonMultiplePrimary = false;
                    mType = type;
                    break;
                } else if (mType == null) {
                    mType = type;
                }
            } else {
                if (type.getPrimaryInput() == true) {
                    foundNonMultiplePrimary = true;
                }
            }
        }

        return foundNonMultiplePrimary ? null : mType;
    }

    public ITool[] filterTools(ITool localTools[], IManagedProject manProj) {
        if (manProj == null) {
            //  If this is not associated with a project, then there is nothing to filter with
            return localTools;
        }
        IProject project = (IProject) manProj.getOwner();
        Vector<Tool> tools = new Vector<>(localTools.length);
        for (ITool t : localTools) {
            Tool tool = (Tool) t;
            if (!tool.isEnabled(this))
                continue;

            if (!isRoot() && conflictsWithRootTools(tool))
                continue;

            try {
                // Make sure the tool is right for the project
                switch (tool.getNatureFilter()) {
                case ITool.FILTER_C:
                    if (project.hasNature(CProjectNature.C_NATURE_ID)
                            && !project.hasNature(CCProjectNature.CC_NATURE_ID)) {
                        tools.add(tool);
                    }
                    break;
                case ITool.FILTER_CC:
                    if (project.hasNature(CCProjectNature.CC_NATURE_ID)) {
                        tools.add(tool);
                    }
                    break;
                case ITool.FILTER_BOTH:
                    tools.add(tool);
                    break;
                default:
                    break;
                }
            } catch (CoreException e) {
                continue;
            }
        }

        // Answer the filtered tools as an array
        return tools.toArray(new Tool[tools.size()]);
    }

    @Override
    public ITool[] getFilteredTools() {
        if (toolChain == null) {
            return new ITool[0];
        }
        ITool[] localTools = toolChain.getTools();
        IManagedProject manProj = getParent().getManagedProject();
        return filterTools(localTools, manProj);
    }

    @Override
    public final int getKind() {
        return ICSettingBase.SETTING_FOLDER;
    }

    @Override
    public boolean isDirty() {
        if (super.isDirty())
            return true;

        if (toolChain.isDirty())
            return true;

        return false;
    }

    @Override
    public boolean needsRebuild() {
        if (super.needsRebuild() || toolChain.needsRebuild())
            return true;
        else
            return false;
    }

    @Override
    public void setRebuildState(boolean rebuild) {
        super.setRebuildState(rebuild);

        if (!rebuild)
            toolChain.setRebuildState(false);
    }

    @Override
    public IToolChain getToolChain() {
        return toolChain;
    }

    @Override
    public ITool[] getTools() {
        return toolChain.getTools();
    }

    @Override
    public ITool getTool(String id) {
        return toolChain.getTool(id);
    }

    @Override
    public ITool[] getToolsBySuperClassId(String id) {
        return toolChain.getToolsBySuperClassId(id);
    }

    ToolChain createToolChain(IToolChain superClass, String Id, String name, boolean isExtensionElement) {
        toolChain = new ToolChain(this, superClass, Id, name, isExtensionElement);
        setDirty(true);
        return toolChain;
    }

    @Override
    void serialize(ICStorageElement element) {
        super.serialize(element);

        ICStorageElement toolChainElement = element.createChild(IToolChain.TOOL_CHAIN_ELEMENT_NAME);
        toolChain.serialize(toolChainElement);
    }

    @Override
    void resolveReferences() {
        if (toolChain != null)
            toolChain.resolveReferences();
    }

    @Override
    public void updateManagedBuildRevision(String revision) {
        super.updateManagedBuildRevision(revision);

        if (toolChain != null)
            toolChain.updateManagedBuildRevision(revision);
    }

    @Override
    public boolean isExtensionElement() {
        return isExtensionElement;
    }

    public String getErrorParserIds() {
        if (toolChain != null)
            return toolChain.getErrorParserIds(getParent());
        return null;
    }

    @Override
    public CFolderData getFolderData() {
        return (CFolderData) getResourceData();
    }

    @Override
    public CLanguageData[] getCLanguageDatas() {
        List<CLanguageData> list = new ArrayList<>();
        for (ITool t : getFilteredTools())
            for (CLanguageData d : t.getCLanguageDatas())
                list.add(d);
        return list.toArray(new BuildLanguageData[list.size()]);
    }

    @Override
    public ITool getToolFromOutputExtension(String extension) {
        // Treat a null argument as an empty string
        String ext = extension == null ? "" : extension; //$NON-NLS-1$
        // Get all the tools for the current config
        ITool[] tools = getFilteredTools();
        for (ITool tool : tools) {
            if (tool.producesFileType(ext)) {
                return tool;
            }
        }
        return null;
    }

    @Override
    public ITool getToolFromInputExtension(String sourceExtension) {
        // Get all the tools for the current config
        ITool[] tools = getFilteredTools();
        for (ITool tool : tools) {
            if (tool.buildsFileType(sourceExtension)) {
                return tool;
            }
        }
        return null;
    }

    @Override
    public void propertiesChanged() {
        if (isExtensionElement)
            return;
        toolChain.propertiesChanged();
        super.propertiesChanged();
    }

    @Override
    public void setDirty(boolean isDirty) {
        if (isExtensionElement && isDirty)
            return;

        super.setDirty(isDirty);

        // Propagate "false" to the children
        if (!isDirty) {
            if (toolChain != null)
                toolChain.setDirty(false);
        }
    }

    private Map<String, String> typeIdsToMap(String[] ids, IBuildObjectProperties props) {
        Map<String, String> map = new HashMap<>(ids.length);
        for (String id : ids) {
            IBuildProperty prop = props.getProperty(id);
            map.put(id, prop.getValue().getId());
        }
        return map;
    }

    private Map<String, String> propsToMap(IBuildProperty props[]) {
        Map<String, String> map = new HashMap<>(props.length);
        for (IBuildProperty p : props)
            map.put(p.getPropertyType().getId(), p.getValue().getId());
        return map;
    }

    private boolean checkPropertiesModificationCompatibility(IBuildPropertiesRestriction r,
            Map<String, String> unspecifiedRequiredProps, Map<String, String> unspecifiedProps,
            Set<String> undefinedSet) {
        IBuildObjectProperties props = null;
        IConfiguration cfg = getParent();
        if (cfg != null) {
            props = cfg.getBuildProperties();
        }

        unspecifiedProps.clear();
        unspecifiedRequiredProps.clear();

        if (props != null && props.getSupportedTypeIds().length != 0) {
            String[] requiredIds = props.getRequiredTypeIds();

            IBuildPropertyType[] supportedTypes = props.getSupportedTypes();
            if (supportedTypes.length != 0 || requiredIds.length != 0) {
                if (requiredIds.length == 0) {
                    if (props.getProperty(ManagedBuildManager.BUILD_ARTEFACT_TYPE_PROPERTY_ID) != null) {
                        requiredIds = new String[] { ManagedBuildManager.BUILD_ARTEFACT_TYPE_PROPERTY_ID };
                    }
                }

                Map<String, String> requiredMap = typeIdsToMap(requiredIds, props);
                getUnsupportedProperties(requiredMap, r, unspecifiedRequiredProps, undefinedSet);
                unspecifiedProps.putAll(unspecifiedRequiredProps);

                IBuildProperty[] ps = props.getProperties();
                Map<String, String> propsMap = propsToMap(ps);
                getUnsupportedProperties(propsMap, r, unspecifiedProps, undefinedSet);
            }
            return unspecifiedRequiredProps.size() == 0;
        }
        return false;
    }

    private void getUnsupportedProperties(Map<String, String> props, IBuildPropertiesRestriction restriction,
            Map<String, String> unsupported, Set<String> inexistent) {
        BuildPropertyManager mngr = BuildPropertyManager.getInstance();
        for (Map.Entry<String, String> entry : props.entrySet()) {
            String propId = entry.getKey();
            String valueId = entry.getValue();
            IBuildPropertyType type = mngr.getPropertyType(propId);
            if (type == null) {
                if (inexistent != null) {
                    inexistent.add(propId);
                }
            }

            if (!restriction.supportsType(propId)) {
                unsupported.put(propId, null);
            } else if (!restriction.supportsValue(propId, valueId)) {
                unsupported.put(propId, valueId);
            }
        }
    }

    public void checkPropertiesModificationCompatibility(final ITool tools[],
            Map<String, String> unspecifiedRequiredProps, Map<String, String> unspecifiedProps,
            Set<String> undefinedSet) {
        final ToolChain tc = (ToolChain) getToolChain();
        IBuildPropertiesRestriction r = new IBuildPropertiesRestriction() {
            @Override
            public boolean supportsType(String typeId) {
                if (tc.supportsType(typeId, false))
                    return true;

                for (ITool tool : tools) {
                    if (((Tool) tool).supportsType(typeId))
                        return true;
                }
                return false;
            }

            @Override
            public boolean supportsValue(String typeId, String valueId) {
                if (tc.supportsValue(typeId, valueId, false))
                    return true;

                for (ITool tool : tools) {
                    if (((Tool) tool).supportsValue(typeId, valueId))
                        return true;
                }
                return false;
            }

            @Override
            public String[] getRequiredTypeIds() {
                List<String> list = new ArrayList<>();

                list.addAll(Arrays.asList(tc.getRequiredTypeIds(false)));

                for (ITool tool : tools) {
                    list.addAll(Arrays.asList(((Tool) tool).getRequiredTypeIds()));
                }

                return list.toArray(new String[list.size()]);
            }

            @Override
            public String[] getSupportedTypeIds() {
                List<String> list = new ArrayList<>();

                list.addAll(Arrays.asList(tc.getSupportedTypeIds(false)));

                for (ITool tool : tools) {
                    list.addAll(Arrays.asList(((Tool) tool).getSupportedTypeIds()));
                }

                return list.toArray(new String[list.size()]);
            }

            @Override
            public String[] getSupportedValueIds(String typeId) {
                List<String> list = new ArrayList<>();

                list.addAll(Arrays.asList(tc.getSupportedValueIds(typeId, false)));

                for (ITool tool : tools) {
                    list.addAll(Arrays.asList(((Tool) tool).getSupportedValueIds(typeId)));
                }

                return list.toArray(new String[list.size()]);
            }

            @Override
            public boolean requiresType(String typeId) {
                if (tc.requiresType(typeId, false))
                    return true;

                for (ITool tool : tools) {
                    if (((Tool) tool).requiresType(typeId))
                        return true;
                }
                return false;
            }
        };

        checkPropertiesModificationCompatibility(r, unspecifiedRequiredProps, unspecifiedProps, undefinedSet);
    }

    public boolean checkPropertiesModificationCompatibility(IToolChain tc, Map<String, String> unspecifiedRequiredProps,
            Map<String, String> unspecifiedProps, Set<String> undefinedSet) {
        return checkPropertiesModificationCompatibility((IBuildPropertiesRestriction) tc, unspecifiedRequiredProps,
                unspecifiedProps, undefinedSet);
    }

    public boolean isPropertiesModificationCompatible(IToolChain tc) {
        Map<String, String> requiredMap = new HashMap<>();
        Map<String, String> unsupportedMap = new HashMap<>();
        Set<String> undefinedSet = new HashSet<>();
        if (!checkPropertiesModificationCompatibility(tc, requiredMap, unsupportedMap, undefinedSet))
            return false;
        return true;
    }

    private Set<String> getRequiredUnspecifiedProperties() {
        IBuildObjectProperties props = null;
        Set<String> set = new HashSet<>();
        IConfiguration cfg = getParent();

        if (cfg != null)
            props = cfg.getBuildProperties();

        if (props != null)
            for (String s : props.getRequiredTypeIds())
                if (props.getProperty(s) == null)
                    set.add(s);
        return set;
    }

    @Override
    public boolean isToolChainCompatible(IToolChain tCh) {
        return isToolChainCompatible(toolChain, tCh);
    }

    public boolean isToolChainCompatible(ToolChain fromTc, IToolChain tCh) {
        boolean compatible = false;
        if (tCh == fromTc)
            return true;

        //        if (tCh == null) {
        //            tCh = ManagedBuildManager.getExtensionToolChain(ConfigurationDataProvider.PREF_TC_ID);
        //        }

        if (tCh == null)
            return false;

        IToolChain curReal = ManagedBuildManager.getRealToolChain(fromTc);
        IToolChain newReal = ManagedBuildManager.getRealToolChain(tCh);

        if (curReal == newReal)
            return true;

        //        if (getToolChainConverterInfo(fromTc, tCh) != null)
        //            compatible = true;

        if (!compatible)
            compatible = isPropertiesModificationCompatible(tCh);

        return compatible;
    }

    @Override
    public IToolChain changeToolChain(IToolChain newSuperClass, String Id, String name) throws BuildException {
        boolean usePrefTc = false;
        //        if (newSuperClass == null) {
        //            newSuperClass = ManagedBuildManager.getExtensionToolChain(ConfigurationDataProvider.PREF_TC_ID);
        //            usePrefTc = true;
        //        }

        if (newSuperClass == null)
            return toolChain;

        IToolChain curReal = ManagedBuildManager.getRealToolChain(toolChain);
        IToolChain newReal = ManagedBuildManager.getRealToolChain(newSuperClass);

        if (Id == null) {
            Id = ManagedBuildManager.calculateChildId(newSuperClass.getId(), null);
        }

        if (name == null) {
            name = newSuperClass.getName();
        }

        if (newReal != curReal) {
            IToolChain extTc = ManagedBuildManager.getExtensionToolChain(newSuperClass);
            if (extTc != null)
                newSuperClass = extTc;
            ToolChain oldToolChain = toolChain;
            //            ConverterInfo cInfo = getToolChainConverterInfo(toolChain, newSuperClass);
            //            ITool oldTools[] = oldToolChain.getTools();
            //
            //            if (cInfo != null) {
            //                updateToolChainWithConverter(cInfo, Id, name);
            //            } else {
            //                updateToolChainWithProperties(usePrefTc ? null : newSuperClass, Id, name);
            //            }
            //            BuildSettingsUtil.disconnectDepentents(getParent(), oldTools);
        }
        return toolChain;
    }

    void updateToolChainWithProperties(IToolChain newSuperClass, String Id, String name) {
        ToolChain oldTc = (ToolChain) getToolChain();
        Configuration cfg = null;
        if (newSuperClass != null) {
            createToolChain(newSuperClass, Id, name, false);

            // For each option/option category child of the tool-chain that is
            // the child of the selected configuration element, create an option/
            // option category child of the cloned configuration's tool-chain element
            // that specifies the original tool element as its superClass.
            toolChain.createOptions(newSuperClass);

            // For each tool element child of the tool-chain that is the child of
            // the selected configuration element, create a tool element child of
            // the cloned configuration's tool-chain element that specifies the
            // original tool element as its superClass.
            String subId;
            ITool[] tools = newSuperClass.getTools();
            for (ITool tool : tools) {
                Tool toolChild = (Tool) tool;
                subId = ManagedBuildManager.calculateChildId(toolChild.getId(), null);
                toolChain.createTool(toolChild, subId, toolChild.getName(), false);
            }
        } else {
            //Configuration cfg = ConfigurationDataProvider.getClearPreference(null);
            ToolChain prefTch = (ToolChain) cfg.getRootFolderInfo().getToolChain();

            toolChain = new ToolChain(this, ManagedBuildManager.calculateChildId(prefTch.getSuperClass().getId(), null),
                    prefTch.getName(), new HashMap<IPath, Map<String, String>>(), prefTch);
        }

        if (isRoot()) {
            Builder oldBuilder = (Builder) oldTc.getBuilder();
            Builder newBuilder = (Builder) getParent().getEditableBuilder();
            newBuilder.copySettings(oldBuilder, false);
        }

        IManagedProject mProj = getParent().getManagedProject();
        ITool[] filteredTools = getFilteredTools();
        ITool[] oldFilteredTools = filterTools(oldTc.getTools(), mProj);

        copySettings(oldFilteredTools, filteredTools);

        toolChain.propertiesChanged();
    }

    private void copySettings(ITool[] fromTools, ITool[] toTools) {
        //        ITool[][] matches = getBestMatches(fromTools, toTools);
        //        for (ITool[] match : matches) {
        //            BuildSettingsUtil.copyCommonSettings(match[0], match[1]);
        //        }
    }

    private ITool[][] getBestMatches(ITool[] tools1, ITool[] tools2) {
        HashSet<ITool> set = new HashSet<>(Arrays.asList(tools2));
        List<ITool[]> list = new ArrayList<>(tools1.length);
        for (ITool tool1 : tools1) {
            ITool bestMatchTool = null;
            int num = 0;
            for (ITool tool2 : set) {
                int extsNum = getConflictingInputExts(tool1, tool2).length;
                if (extsNum > num) {
                    bestMatchTool = tool2;
                    num = extsNum;
                }
            }

            if (bestMatchTool != null) {
                list.add(new ITool[] { tool1, bestMatchTool });
                set.remove(bestMatchTool);
            }
        }
        return list.toArray(new ITool[list.size()][]);
    }

    //   void updateToolChainWithConverter(ConverterInfo cInfo, String Id, String name) throws BuildException {
    //        IBuildObject bo = cInfo.getConvertedFromObject();
    //        ToolChain updatedToolChain = null;
    //        if (bo instanceof Configuration) {
    //            Configuration cfg = (Configuration) bo;
    //            if (cfg != getParent()) {
    //                IResourceInfo rcInfo = cfg.getResourceInfo(getPath(), true);
    //                if (rcInfo instanceof FolderInfo) {
    //                    IToolChain tc = ((FolderInfo) rcInfo).getToolChain();
    //                    IToolChain realToToolChain = ManagedBuildManager.getRealToolChain((IToolChain) cInfo.getToObject());
    //                    if (ManagedBuildManager.getRealToolChain(tc) == realToToolChain) {
    //                        updatedToolChain = (ToolChain) tc;
    //                    }
    //                }
    //
    //                if (updatedToolChain == null) {
    //                    updatedToolChain = (ToolChain) cfg.getRootFolderInfo().getToolChain();
    //                }
    //            } else {
    //                updatedToolChain = toolChain;
    //            }
    //        } else if (bo instanceof ToolChain) {
    //            updatedToolChain = (ToolChain) bo;
    //        } else {
    //            throw new BuildException(ManagedMakeMessages.getResourceString("FolderInfo.4")); //$NON-NLS-1$
    //        }
    //
    //        if (updatedToolChain != null && toolChain != updatedToolChain) {
    //            setUpdatedToolChain(updatedToolChain);
    //        }
    //
    //        toolChain.setName(name);
    //    }

    void setUpdatedToolChain(ToolChain tch) {
        tch.copyNonoverriddenSettings(toolChain);
        toolChain = tch;
        tch.updateParentFolderInfo(this);
    }

    //    private ConverterInfo getToolChainConverterInfo(ToolChain fromTc, IToolChain toTc) {
    //        IConfigurationElement el = getToolChainConverterElement(fromTc, toTc);
    //        IToolChain foundToTc = toTc;
    //        if (el == null) {
    //            IToolChain[] tcs = ManagedBuildManager.findIdenticalToolChains(toTc);
    //            for (IToolChain tc : tcs) {
    //                foundToTc = tc;
    //                if (foundToTc == toTc)
    //                    continue;
    //
    //                el = getToolChainConverterElement(fromTc, foundToTc);
    //                if (el != null)
    //                    break;
    //            }
    //        }
    //
    //        if (el != null)
    //            return new ConverterInfo(this, getToolChain(), foundToTc, el);
    //        return null;
    //    }

    private IConfigurationElement getToolChainConverterElement(ToolChain fromTc, IToolChain tCh) {
        if (tCh == null)
            return null;

        if (fromTc != null)
            return fromTc.getConverterModificationElement(tCh);
        else
            return null;
    }

    @SuppressWarnings("unchecked")
    private ITool[][] checkDups(ITool[] removed, ITool[] added) {
        LinkedHashMap<Object, ITool> removedMap = createRealToExtToolMap(removed, false);
        LinkedHashMap<Object, ITool> addedMap = createRealToExtToolMap(added, true);
        LinkedHashMap<Object, ITool> rmCopy = (LinkedHashMap<Object, ITool>) removedMap.clone();

        removedMap.keySet().removeAll(addedMap.keySet());
        addedMap.keySet().removeAll(rmCopy.keySet());

        if (removedMap.size() != 0) {
            LinkedHashMap<Object, ITool> curMap = createRealToExtToolMap(getTools(), false);
            for (Iterator<Map.Entry<Object, ITool>> iter = removedMap.entrySet().iterator(); iter.hasNext();) {
                Map.Entry<Object, ITool> entry = iter.next();
                Object key = entry.getKey();
                Object curTool = curMap.get(key);
                if (curTool != null)
                    entry.setValue((ITool) curTool);
                else
                    iter.remove();
            }
        }
        ITool[][] result = new Tool[2][];
        result[0] = removedMap.values().toArray(new Tool[removedMap.size()]);
        result[1] = addedMap.values().toArray(new Tool[addedMap.size()]);
        return result;
    }

    private LinkedHashMap<Object, ITool> createRealToExtToolMap(ITool[] tools, boolean extValues) {
        LinkedHashMap<Object, ITool> map = new LinkedHashMap<>();
        for (ITool t : tools) {
            Tool realTool = (Tool) ManagedBuildManager.getRealTool(t);
            MatchKey<Tool> key = realTool.getMatchKey();
            ITool toolValue = extValues ? ManagedBuildManager.getExtensionTool(t) : t;
            if (toolValue != null)
                map.put(key, toolValue);
        }
        return map;
    }

    @Override
    public void modifyToolChain(ITool[] removed, ITool[] added) {
        //        ToolListModificationInfo info = ToolChainModificationHelper.getModificationInfo(this, getTools(), added,
        //                removed);
        //        info.apply();
        return;

        //		ITool[][] checked = checkDups(removed, added);
        //		removed = checked[0];
        //		added = checked[1];
        //		if(added.length == 0 && removed.length == 0)
        //			return;
        //
        //		List<ITool> remainingRemoved = new ArrayList<ITool>();
        //		List<ITool> remainingAdded = new ArrayList<ITool>();
        //		Map<ITool, ConverterInfo> converterMap =
        //			calculateConverterTools(removed, added, remainingRemoved, remainingAdded);
        //		invokeConverters(converterMap);
        //		List<Tool> newTools = new ArrayList<Tool>(added.length);
        //		for(ConverterInfo info : converterMap.values()){
        //			if(info.getConvertedFromObject() instanceof Tool){
        //				Tool newTool = (Tool)info.getConvertedFromObject();
        //				newTool.updateParent(getToolChain());
        //				newTools.add(newTool);
        //			} else {
        //				remainingAdded.add((ITool)info.getToObject());
        //			}
        //		}
        //
        //		for(ITool t : remainingAdded){
        //			newTools.add(
        //				new Tool(
        //					toolChain,
        //					t,
        //					ManagedBuildManager.calculateChildId(t.getId(), null),
        //					t.getName(),
        //					false)
        //				);
        //		}
        //
        //		performToolChainModification(removed, newTools.toArray(new Tool[newTools.size()]));
    }

    //	private void performToolChainModification(ITool removed[], ITool[] added){
    //		BuildSettingsUtil.disconnectDepentents(getParent(), removed);
    //
    //		for (ITool tool : removed) {
    //			toolChain.removeTool((Tool)tool);
    //		}
    //
    //		for (ITool tool : added) {
    //			toolChain.addTool((Tool)tool);
    //		}
    //
    //		adjustTargetTools(removed, added);
    //
    //		toolChain.propertiesChanged();
    //	}

    private void adjustTargetTools(ITool removed[], ITool added[]) {
        if (!isRoot())
            return;

        Set<String> set = new HashSet<>();
        String[] ids = toolChain.getTargetToolList();
        boolean targetToolsModified = false;
        set.addAll(Arrays.asList(ids));

        for (ITool tool : removed) {
            Object[] tInfo = getTargetTool(tool);

            if (tInfo == null)
                continue;

            ITool target = (ITool) tInfo[0];
            String tId = (String) tInfo[1];

            //            if (BuildSettingsUtil.calcDependentTools(added, target, null).size() != 0)
            //                continue;

            ITool newTargetTool = findCompatibleTargetTool(target, added);
            if (newTargetTool == null)
                continue;

            newTargetTool = ManagedBuildManager.getExtensionTool(newTargetTool);
            if (newTargetTool == null)
                continue;

            set.remove(tId);
            set.add(newTargetTool.getId());
            targetToolsModified = true;
        }

        if (targetToolsModified) {
            toolChain.setTargetToolIds(CDataUtil.arrayToString(set.toArray(new String[set.size()]), ";")); //$NON-NLS-1$
        }
    }

    private ITool findCompatibleTargetTool(ITool tool, ITool allTools[]) {
        IProject project = getParent().getOwner().getProject();
        String exts[] = ((Tool) tool).getAllOutputExtensions(project);
        Set<String> extsSet = new HashSet<>(Arrays.asList(exts));
        ITool compatibleTool = null;
        for (ITool t : allTools) {
            String otherExts[] = ((Tool) t).getAllOutputExtensions(project);
            for (String otherExt : otherExts) {
                if (extsSet.contains(otherExt)) {
                    compatibleTool = t;
                    break;
                }
            }
            if (compatibleTool != null)
                break;
        }

        if (compatibleTool == null) {
            //try to match build output variable
            Set<String> set = getToolOutputVars(tool);
            for (ITool t : allTools) {
                IOutputType types[] = t.getOutputTypes();
                for (IOutputType type : types) {
                    String var = type.getBuildVariable();
                    if (var != null && set.contains(var)) {
                        compatibleTool = t;
                        break;
                    }

                }

                if (compatibleTool != null)
                    break;
            }
        }

        return compatibleTool;
    }

    private Set<String> getToolOutputVars(ITool tool) {
        Set<String> set = new HashSet<>();

        IOutputType types[] = tool.getOutputTypes();
        for (IOutputType type : types) {
            String var = type.getBuildVariable();
            if (var != null)
                set.add(var);

        }

        return set;
    }

    private Object[] getTargetTool(ITool tool) {
        String[] ids = toolChain.getTargetToolList();

        for (String id : ids) {
            ITool target = tool;
            for (; target != null; target = target.getSuperClass()) {
                if (id.equals(target.getId()))
                    break;
            }
            if (target != null)
                return new Object[] { target, id };

        }
        return null;
    }

    //	private List<ConverterInfo> invokeConverters(Map<?, ConverterInfo> converterMap){
    //		List<ConverterInfo> failed = new ArrayList<ConverterInfo>();
    //		for(ConverterInfo info : converterMap.values()){
    //			IBuildObject converted = info.getConvertedFromObject();
    //			if(converted == null ||
    //			  !converted.getClass().equals(info.getFromObject().getClass())){
    //				failed.add(info);
    //			}
    //		}
    //		return failed;
    //	}

    //	private Map<ITool, ConverterInfo> calculateConverterTools(ITool[] removed, ITool[] added, List<ITool> remainingRemoved, List<ITool> remainingAdded){
    //		if(remainingAdded == null)
    //			remainingAdded = new ArrayList<ITool>(added.length);
    //		if(remainingRemoved == null)
    //			remainingRemoved = new ArrayList<ITool>(removed.length);
    //
    //		remainingAdded.clear();
    //		remainingRemoved.clear();
    //
    //		remainingAdded.addAll(Arrays.asList(added));
    //		remainingRemoved.addAll(Arrays.asList(removed));
    //
    //		Map<ITool, ConverterInfo> resultMap = new HashMap<ITool, ConverterInfo>();
    //
    //		for(Iterator<ITool> rIter = remainingRemoved.iterator(); rIter.hasNext();){
    //			ITool r = rIter.next();
    //
    //			if(r.getParentResourceInfo() != this)
    //				continue;
    //
    //			if(ManagedBuildManager.getConversionElements(r).size() == 0)
    //				continue;
    //
    //			for(Iterator<ITool> aIter = remainingAdded.iterator(); aIter.hasNext();){
    //				ITool a = aIter.next();
    //
    //				if(a.getParentResourceInfo() == this)
    //					continue;
    //
    //				IConfigurationElement el = getToolConverterElement(r, a);
    //				if(el != null){
    //					resultMap.put(r, new ConverterInfo(this, r, a, el));
    //					rIter.remove();
    //					aIter.remove();
    //					break;
    //				}
    //			}
    //		}
    //
    //		return resultMap;
    //	}

    private ITool[] calculateToolsArray(ITool[] removed, ITool[] added) {
        LinkedHashMap<Object, ITool> map = createRealToExtToolMap(getTools(), false);
        LinkedHashMap<Object, ITool> removedMap = createRealToExtToolMap(removed, false);

        map.keySet().removeAll(removedMap.keySet());
        map.putAll(createRealToExtToolMap(added, true));

        return map.values().toArray(new ITool[map.size()]);
    }

    @SuppressWarnings("unchecked")
    private ITool[][] calculateConflictingTools(ITool[] newTools) {
        HashSet<ITool> set = new HashSet<>();
        set.addAll(Arrays.asList(newTools));
        List<ITool[]> result = new ArrayList<>();
        for (Iterator<ITool> iter = set.iterator(); iter.hasNext();) {
            ITool t = iter.next();
            iter.remove();
            HashSet<ITool> tmp = (HashSet<ITool>) set.clone();
            List<ITool> list = new ArrayList<>();
            for (Iterator<ITool> tmpIt = tmp.iterator(); tmpIt.hasNext();) {
                ITool other = tmpIt.next();
                String conflicts[] = getConflictingInputExts(t, other);
                if (conflicts.length != 0) {
                    list.add(other);
                    tmpIt.remove();
                }
            }

            if (list.size() != 0) {
                list.add(t);
                result.add(list.toArray(new Tool[list.size()]));
            }
            set = tmp;
            iter = set.iterator();
        }

        return result.toArray(new ITool[result.size()][]);
    }

    private String[] getConflictingInputExts(ITool tool1, ITool tool2) {
        IProject project = getParent().getOwner().getProject();
        String ext1[] = ((Tool) tool1).getAllInputExtensions(project);
        String ext2[] = ((Tool) tool2).getAllInputExtensions(project);
        Set<String> set1 = new HashSet<>(Arrays.asList(ext1));
        Set<String> result = new HashSet<>();
        for (String e : ext2) {
            if (set1.remove(e))
                result.add(e);
        }
        return result.toArray(new String[result.size()]);
    }

    @Override
    public IModificationStatus getToolChainModificationStatus(ITool[] removed, ITool[] added) {
        return null;
        //        ITool[][] checked = checkDups(removed, added);
        //        removed = checked[0];
        //        added = checked[1];
        //        ITool newTools[] = calculateToolsArray(removed, added);
        //        ITool[][] conflicting = calculateConflictingTools(filterTools(newTools, getParent().getManagedProject()));
        //        Map<String, String> unspecifiedRequiredProps = new HashMap<>();
        //        Map<String, String> unspecifiedProps = new HashMap<>();
        //        Set<String> undefinedSet = new HashSet<>();
        //        IConfiguration cfg = getParent();
        //        ITool[] nonManagedTools = null;
        //        if (cfg.isManagedBuildOn() && cfg.supportsBuild(true)) {
        //            List<ITool> list = new ArrayList<>();
        //            for (ITool t : newTools)
        //                if (!t.supportsBuild(true))
        //                    list.add(t);
        //            if (list.size() != 0)
        //                nonManagedTools = list.toArray(new Tool[list.size()]);
        //        }
        //        return new ModificationStatus(unspecifiedRequiredProps, unspecifiedProps, undefinedSet, conflicting,
        //                nonManagedTools);
    }

    @Override
    public boolean supportsBuild(boolean managed) {
        if (getRequiredUnspecifiedProperties().size() != 0)
            return false;

        ToolChain tCh = (ToolChain) getToolChain();
        if (tCh == null || !tCh.getSupportsManagedBuildAttribute())
            return !managed;

        ITool tools[] = getFilteredTools();
        for (int i = 0; i < tools.length; i++) {
            if (!tools[i].supportsBuild(managed))
                return false;
        }

        return true;
    }

    @Override
    public boolean buildsFileType(String srcExt) {
        // Check to see if there is a rule to build a file with this extension
        ITool[] tools = getFilteredTools();
        for (ITool tool : tools) {
            if (tool != null && tool.buildsFileType(srcExt)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getOutputExtension(String resourceExtension) {
        String outputExtension = null;
        ITool[] tools = getFilteredTools();
        for (ITool tool : tools) {
            outputExtension = tool.getOutputExtension(resourceExtension);
            if (outputExtension != null) {
                return outputExtension;
            }
        }
        return null;
    }

    @Override
    public boolean isHeaderFile(String ext) {
        // Check to see if there is a rule to build a file with this extension
        IManagedProject manProj = getParent().getManagedProject();
        IProject project = null;
        if (manProj != null) {
            project = (IProject) manProj.getOwner();
        }
        ITool[] tools = getFilteredTools();
        for (ITool tool : tools) {
            try {
                if (project != null) {
                    // Make sure the tool is right for the project
                    switch (tool.getNatureFilter()) {
                    case ITool.FILTER_C:
                        if (project.hasNature(CProjectNature.C_NATURE_ID)
                                && !project.hasNature(CCProjectNature.CC_NATURE_ID)) {
                            return tool.isHeaderFile(ext);
                        }
                        break;
                    case ITool.FILTER_CC:
                        if (project.hasNature(CCProjectNature.CC_NATURE_ID)) {
                            return tool.isHeaderFile(ext);
                        }
                        break;
                    case ITool.FILTER_BOTH:
                        return tool.isHeaderFile(ext);
                    }
                } else {
                    return tool.isHeaderFile(ext);
                }
            } catch (CoreException e) {
                continue;
            }
        }
        return false;
    }

    @Override
    public Set<String> contributeErrorParsers(Set<String> set) {
        if (toolChain != null)
            set = toolChain.contributeErrorParsers(this, set, true);
        return set;
    }

    @Override
    public void resetErrorParsers() {
        if (toolChain != null)
            toolChain.resetErrorParsers(this);
    }

    @Override
    void removeErrorParsers(Set<String> set) {
        if (toolChain != null)
            toolChain.removeErrorParsers(this, set);
    }

    @Override
    public ITool getToolById(String id) {
        if (toolChain != null)
            return toolChain.getTool(id);
        return null;
    }

    @Override
    void resolveProjectReferences(boolean onLoad) {
        if (toolChain != null)
            toolChain.resolveProjectReferences(onLoad);
    }

    /**
     * Reset the options of the given holder to the default values
     */
    private void resetOptionSettings(IHoldsOptions holder) {
        IOption[] opts = holder.getOptions();
        for (IOption opt : opts) {
            Object val = opt.getDefaultValue();

            if (opt.getDefaultValueGenerator() != null) {
                val = opt.getDefaultValueGenerator().generateDefaultValue(opt);
            }

            if (val instanceof Boolean) {
                ManagedBuildManager.setOption(toolChain.getParent(), holder, opt, (Boolean) val);
            } else if (val instanceof String[]) {
                ManagedBuildManager.setOption(toolChain.getParent(), holder, opt, (String[]) val);
            } else if (val instanceof String) {
                ManagedBuildManager.setOption(toolChain.getParent(), holder, opt, (String) val);
            } else if (val == null) {
                holder.removeOption(opt);
            } else {
                Activator.error("Unknown type of value " + val + " for " + opt); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
    }

    public void resetOptionSettings() {
        // (Bug 438367) Removing all the options and relying on automatic creating when modifying/using the option
        // will result in problems in the following cases:
        // 		-	When changing an option affects values of other options.
        // 		-	When the option has a FieldEditor that holds an instance of the option, that
        //			will result on having multiple copies of the option in some cases.
        // Instead, will reset the value of each option to its default value.
        ITool[] tools = getTools();
        IToolChain toolChain = getToolChain();
        for (ITool tool : tools) {
            resetOptionSettings(tool);
        }
        resetOptionSettings(toolChain);

        // Send out the event to notify the options that they are about to be removed.
        // Do not do this for the child resource configurations as they are handled when
        // the configuration itself is destroyed.
        //		ManagedBuildManager.performValueHandlerEvent(this, IManagedOptionValueHandler.EVENT_CLOSE, false);
        //		rebuildNeeded = true;
    }

    @Override
    public boolean hasCustomSettings() {
        IFolderInfo parentFo = getParentFolderInfo();
        if (parentFo == null)
            return true;
        return toolChain.hasCustomSettings((ToolChain) parentFo.getToolChain());
    }

    public boolean containsDiscoveredScannerInfo() {
        if (!isRoot())
            return true;

        return containsDiscoveredScannerInfo;
    }

    public void setContainsDiscoveredScannerInfo(boolean contains) {
        containsDiscoveredScannerInfo = contains;
    }

    @Override
    public boolean isFolderInfo() {
        return true;
    }

    @Override
    void performPostModificationAdjustments(ToolListModificationInfo info) {
        adjustTargetTools(info.getRemovedTools(), info.getAddedTools(true));

        super.performPostModificationAdjustments(info);
    }

    @Override
    void applyToolsInternal(ITool[] resultingTools, ToolListModificationInfo info) {
        ITool[] removedTools = info.getRemovedTools();

        for (ITool tool : removedTools) {
            ITool extTool = ManagedBuildManager.getExtensionTool(tool);
            if (extTool.getParent() == toolChain.getSuperClass())
                toolChain.addUnusedChild(extTool);
        }

        toolChain.setToolsInternal(resultingTools);

        adjustTargetTools(removedTools, info.getAddedTools(true));

        setRebuildState(true);
    }

    @Override
    public boolean isSupported() {
        if (toolChain != null)
            return toolChain.isSupported();
        return false;
    }

    //	private IConfigurationElement getToolConverterElement(ITool fromTool, ITool toTool){
    //		ToolChain curTc = (ToolChain)getToolChain();
    //		if(curTc != null){
    //			return curTc.getConverterModificationElement(fromTool, toTool);
    //		}
    //		return null;
    //	}
}
