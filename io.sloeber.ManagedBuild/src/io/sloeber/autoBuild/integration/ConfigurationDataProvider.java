/*******************************************************************************
 * Copyright (c) 2007, 2013 Intel Corporation and others.
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
 * Baltasar Belyavsky (Texas Instruments) - bug 340219: Project metadata files are saved unnecessarily
 *******************************************************************************/
package io.sloeber.autoBuild.integration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsManager;
import org.eclipse.cdt.core.language.settings.providers.ScannerDiscoveryLegacySupport;
import org.eclipse.cdt.core.model.ILanguageDescriptor;
import org.eclipse.cdt.core.model.LanguageManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICExternalSetting;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.core.settings.model.IModificationContext;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationDataProvider;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentType;

import io.sloeber.autoBuild.Internal.Builder;
import io.sloeber.autoBuild.Internal.BuilderFactory;
import io.sloeber.autoBuild.Internal.CfgScannerConfigInfoFactory2;
import io.sloeber.autoBuild.Internal.Configuration;
import io.sloeber.autoBuild.Internal.InputType;
import io.sloeber.autoBuild.Internal.ManagedBuildInfo;
import io.sloeber.autoBuild.Internal.ManagedBuildManager;
import io.sloeber.autoBuild.Internal.ManagedProject;
import io.sloeber.autoBuild.Internal.Tool;
import io.sloeber.autoBuild.api.IBuilder;
import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.api.IFolderInfo;
import io.sloeber.autoBuild.api.IInputType;
import io.sloeber.autoBuild.api.IManagedBuildInfo;
import io.sloeber.autoBuild.api.IManagedProject;
import io.sloeber.autoBuild.api.IResourceInfo;
import io.sloeber.autoBuild.api.ITool;
import io.sloeber.autoBuild.api.IToolChain;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IManagedOptionValueHandler;

/**
 * The main hook ManagedBuild uses to connect to cdt.core's project model.
 * Provides & Persists Build configuration data in the project model storage.
 */
public class ConfigurationDataProvider extends CConfigurationDataProvider {// implements ISettingsChangeListener {
    private static final String BUILD_SYSTEM_DATA_MODULE_NAME = "cdtBuildSystem"; //$NON-NLS-1$
    private static final String VERSION_ATTRIBUTE = "version"; //$NON-NLS-1$
    private static final String PREF_CFG_ID = "org.eclipse.cdt.build.core.prefbase.cfg"; //$NON-NLS-1$
    public static final String PREF_TC_ID = "org.eclipse.cdt.build.core.prefbase.toolchain"; //$NON-NLS-1$
    private static final String PREF_TOOL_ID = "org.eclipse.cdt.build.core.settings.holder"; //$NON-NLS-1$
    private static final QualifiedName CFG_PERSISTED_PROPERTY = new QualifiedName(Activator.getId(), "configPersisted"); //$NON-NLS-1$
    private static final QualifiedName NATURES_USED_ON_CACHE_PROPERTY = new QualifiedName(Activator.getId(),
            "naturesUsedOnCache"); //$NON-NLS-1$
    private static final QualifiedName BUILD_INFO_PROPERTY = new QualifiedName(Activator.getId(), "buildInfo"); //$NON-NLS-1$

    private static boolean registered;

    public ConfigurationDataProvider() {
        if (!registered) {
            registered = true;
            //NotificationManager.getInstance().subscribe(this);
        }
    }

    private static class DesApplyRunnable implements IWorkspaceRunnable {
        IBuilder fBuilder;
        IProject fProject;

        DesApplyRunnable(IProject project, IBuilder builder) {
            fProject = project;
            fBuilder = builder;
        }

        @Override
        public void run(IProgressMonitor monitor) throws CoreException {
            try {
                IProjectDescription eDes = fProject.getDescription();
                if (BuilderFactory.applyBuilder(eDes, fBuilder) == BuilderFactory.CMD_CHANGED) {
                    fProject.setDescription(eDes, monitor);
                }
            } catch (Exception e) {
                Activator.log(e);
            }
        }

    }

    static BuildConfigurationData writeConfiguration(ICConfigurationDescription cfgDescription,
            BuildConfigurationData baseData) throws CoreException {

        BuildConfigurationData appliedCfg = baseData;
        ICStorageElement rootElement = cfgDescription.getStorage(BUILD_SYSTEM_DATA_MODULE_NAME, true);
        rootElement.clear();
        rootElement.setAttribute(VERSION_ATTRIBUTE, ManagedBuildManager.getVersion().toString());
        ICStorageElement cfgElemen = rootElement.createChild(IConfiguration.CONFIGURATION_ELEMENT_NAME);
        Configuration cfg = (Configuration) appliedCfg.getConfiguration();
        Builder b = (Builder) cfg.getEditableBuilder();
        // Need to ensure that build macro supplier can get the description for this configuration during the write...
        cfg.setConfigurationDescription(cfgDescription);
        
        
        
        
//       TOFIX JABA disabled this. This whole method doesn't make sense to me
//        I ùean 2 parameters are provided and one parameter is used to set the description of the other via a 
//        a lot of detours
//        if (b != null && b.isManagedBuildOn() && b.getBuildPathAttribute(false) == null) {
//            String bPath = b.getDefaultBuildPath();
//            b.setBuildPathAttribute(bPath);
//        }
//        //		cfg.setConfigurationDescription(des);
//        //		ManagedBuildManager.performValueHandlerEvent(cfg, IManagedOptionValueHandler.EVENT_APPLY);
//        
        
        
        
        cfg.serialize(cfgElemen);

        return appliedCfg;
    }

    protected CConfigurationData applyPreferences(ICConfigurationDescription cfgDescription,
            CConfigurationData baseData) throws CoreException {
        BuildConfigurationData appliedCfg = writeConfiguration(cfgDescription, (BuildConfigurationData) baseData);

        IConfiguration cfg = ((BuildConfigurationData) baseData).getConfiguration();
        try {
            CfgScannerConfigInfoFactory2.savePreference(cfg);
        } catch (CoreException e) {
            Activator.log(e);
        }

        return appliedCfg;
    }

    @Override
    public CConfigurationData applyConfiguration(ICConfigurationDescription cfgDescription,
            ICConfigurationDescription baseCfgDescription, CConfigurationData baseData, IModificationContext context,
            IProgressMonitor monitor) throws CoreException {

        if (cfgDescription.isPreferenceConfiguration()) {
            return applyPreferences(cfgDescription, baseData);
        }

        BuildConfigurationData baseCfgData = (BuildConfigurationData) baseData;
        IConfiguration baseCfg = baseCfgData.getConfiguration();
        BuildConfigurationData appliedCfgData;
        if (context.isBaseDataCached()) {//JABA Assume not dirty  && !baseCfg.isDirty()
            appliedCfgData = baseCfgData;
            context.setConfigurationSettingsFlags(IModificationContext.CFG_DATA_STORAGE_UNMODIFIED
                    | IModificationContext.CFG_DATA_SETTINGS_UNMODIFIED);
        } else {
            appliedCfgData = writeConfiguration(cfgDescription, baseCfgData);

            IManagedBuildInfo info = getBuildInfo(cfgDescription);
            ManagedProject mProj = (ManagedProject) info.getManagedProject();
            mProj.applyConfiguration((Configuration) appliedCfgData.getConfiguration());
            writeManagedProjectInfo(cfgDescription.getProjectDescription(), mProj);
            if (baseCfgDescription instanceof ILanguageSettingsProvidersKeeper) {
                String[] defaultIds = ((ILanguageSettingsProvidersKeeper) baseCfgDescription)
                        .getDefaultLanguageSettingsProvidersIds();
                List<ILanguageSettingsProvider> providers;
                if (defaultIds == null) {
                    ICProjectDescription prjDescription = baseCfgDescription.getProjectDescription();
                    if (prjDescription != null) {
                        IProject project = prjDescription.getProject();
                        // propagate the preference to project properties
                        ScannerDiscoveryLegacySupport.defineLanguageSettingsEnablement(project);
                    }

                    IConfiguration cfg = appliedCfgData.getConfiguration();
                    defaultIds = cfg != null ? cfg.getDefaultLanguageSettingsProviderIds() : null;
                    if (defaultIds == null) {
                        defaultIds = ScannerDiscoveryLegacySupport.getDefaultProviderIdsLegacy(baseCfgDescription);
                    }
                    providers = LanguageSettingsManager.createLanguageSettingsProviders(defaultIds);
                } else {
                    providers = ((ILanguageSettingsProvidersKeeper) baseCfgDescription).getLanguageSettingProviders();
                }
                if (cfgDescription instanceof ILanguageSettingsProvidersKeeper) {
                    ((ILanguageSettingsProvidersKeeper) cfgDescription)
                            .setDefaultLanguageSettingsProvidersIds(defaultIds);
                    ((ILanguageSettingsProvidersKeeper) cfgDescription).setLanguageSettingProviders(providers);
                }
            }

            try {
                CfgScannerConfigInfoFactory2.save(appliedCfgData, cfgDescription.getProjectDescription(),
                        baseCfgDescription.getProjectDescription(), !isPersistedCfg(cfgDescription));
            } catch (CoreException e) {
                Activator.log(e);
            }
            info.setValid(true);
            // Update the ManagedBuildInfo in the ManagedBuildManager map. Doing this creates a barrier for subsequent
            // ManagedBuildManager#getBuildInfo(...) see Bug 305146 for more
            ManagedBuildManager.setLoaddedBuildInfo(cfgDescription.getProjectDescription().getProject(), info);

            setPersistedFlag(cfgDescription);
            cacheNaturesIdsUsedOnCache(cfgDescription);
        }

        if (cfgDescription.isActive()) {
            IConfiguration cfg = appliedCfgData.getConfiguration();
            IBuilder builder = cfg.getEditableBuilder();
            IProject project = context.getProject();
            IProjectDescription eDes = context.getEclipseProjectDescription();
            switch (BuilderFactory.applyBuilder(eDes, builder)) {
            case BuilderFactory.CMD_UNDEFINED:
                IWorkspaceRunnable applyR = new DesApplyRunnable(project, builder);
                context.addWorkspaceRunnable(applyR);
                break;
            case BuilderFactory.CMD_CHANGED:
                context.setEclipseProjectDescription(eDes);
                break;
            }
        }

        return appliedCfgData;
    }

    private void setPersistedFlag(ICConfigurationDescription cfgDescription) {
        cfgDescription.setSessionProperty(CFG_PERSISTED_PROPERTY, Boolean.TRUE);
    }

    private static void writeManagedProjectInfo(ICProjectDescription prjDescription, ManagedProject mProj)
            throws CoreException {
        ICStorageElement rootElement = prjDescription.getStorage(BUILD_SYSTEM_DATA_MODULE_NAME, true);
        rootElement.clear();
        rootElement.setAttribute(VERSION_ATTRIBUTE, ManagedBuildManager.getVersion().toString());
        ICStorageElement mProjElem = rootElement.createChild(IManagedProject.MANAGED_PROJECT_ELEMENT_NAME);
        mProj.serializeProjectInfo(mProjElem);
    }

    protected CConfigurationData createPreferences(ICConfigurationDescription cfgDescription,
            CConfigurationData baseData) throws CoreException {
        Configuration cfg = (Configuration) ((BuildConfigurationData) baseData).getConfiguration();
        Configuration newCfg = new Configuration((ManagedProject) cfg.getManagedProject(), cfg, cfgDescription.getId(),
                true, true, true);
        newCfg.setConfigurationDescription(cfgDescription);
        newCfg.setName(cfgDescription.getName());
        //		if(!newCfg.getId().equals(cfg.getId())){
        //			newCfg.exportArtifactInfo();
        //		}

        return newCfg.getConfigurationData();
    }

    @Override
    public CConfigurationData createConfiguration(ICConfigurationDescription cfgDescription,
            ICConfigurationDescription baseCfgDescription, CConfigurationData base, boolean clone,
            IProgressMonitor monitor) throws CoreException {

        if (cfgDescription.isPreferenceConfiguration())
            return createPreferences(cfgDescription, base);

        Configuration cfg = (Configuration) ((BuildConfigurationData) base).getConfiguration();
        Configuration newCfg = copyCfg(cfg, cfgDescription);

        if (!newCfg.getId().equals(cfg.getId()) && newCfg.canExportedArtifactInfo()) {
            // Bug 335001: Remove existing exported settings as they point at this configuration
            for (ICExternalSetting extSetting : newCfg.getConfigurationDescription().getExternalSettings())
                newCfg.getConfigurationDescription().removeExternalSetting(extSetting);
            // Now export the new settings
            newCfg.exportArtifactInfo();
        }

        setPersistedFlag(cfgDescription);

        return newCfg.getConfigurationData();
    }

    public static Configuration copyCfg(Configuration cfg, ICConfigurationDescription cfgDescription) {
        IManagedBuildInfo info = getBuildInfo(cfgDescription);
        ManagedProject mProj = (ManagedProject) info.getManagedProject();

        Configuration newCfg = new Configuration(mProj, cfg, cfgDescription.getId(), true, true, false);
        newCfg.setConfigurationDescription(cfgDescription);
        newCfg.setName(cfgDescription.getName());

        cfgDescription.setConfigurationData(cfgDescription.getBuildSystemId(), newCfg.getConfigurationData());

        ManagedBuildManager.performValueHandlerEvent(newCfg, IManagedOptionValueHandler.EVENT_OPEN);

        //		if(!newCfg.getId().equals(cfg.getId())){
        //			newCfg.exportArtifactInfo();
        //		}

        return newCfg;
    }

    private static IManagedBuildInfo getBuildInfo(ICConfigurationDescription cfgDescription) {
        ICProjectDescription projDes = cfgDescription.getProjectDescription();
        IProject project = projDes.getProject();
        IManagedBuildInfo info = ManagedBuildManager.getBuildInfo(project, false);
        if (info == null)
            info = ManagedBuildManager.createBuildInfo(project);

        setLoaddedBuildInfo(projDes, info);

        getManagedProject(cfgDescription, info);

        return info;
    }

    private static void setLoaddedBuildInfo(ICProjectDescription prjDescription, IManagedBuildInfo info) {
        prjDescription.setSessionProperty(BUILD_INFO_PROPERTY, info);
    }

    public static ManagedBuildInfo getLoaddedBuildInfo(ICProjectDescription prjDescription) {
        return (ManagedBuildInfo) prjDescription.getSessionProperty(BUILD_INFO_PROPERTY);
    }

    private static IManagedProject getManagedProject(ICConfigurationDescription cfgDescription,
            IManagedBuildInfo info) {
        IManagedProject mProj = info.getManagedProject();
        if (mProj == null) {
            mProj = createManagedProject(info, cfgDescription.getProjectDescription());
        }
        return mProj;
    }

    private static IManagedProject createManagedProject(IManagedBuildInfo info, ICProjectDescription prjDescription) {
        IManagedProject mProj = null;
        try {
            ICStorageElement rootElem = prjDescription.getStorage(BUILD_SYSTEM_DATA_MODULE_NAME, false);
            if (rootElem != null) {
                String version = rootElem.getAttribute(VERSION_ATTRIBUTE);
                ICStorageElement children[] = rootElem.getChildren();
                for (int i = 0; i < children.length; i++) {
                    if (IManagedProject.MANAGED_PROJECT_ELEMENT_NAME.equals(children[i].getName())) {
                        mProj = new ManagedProject((ManagedBuildInfo) info, children[i], false, version);
                        break;
                    }
                }
            }
        } catch (CoreException e) {
        }

        if (mProj == null) {
            mProj = new ManagedProject(prjDescription);
            info.setManagedProject(mProj);
        }

        return mProj;
    }

    public static String[] getNaturesIdsUsedOnCache(IConfiguration cfg) {
        ICConfigurationDescription cfgDescription = ManagedBuildManager.getDescriptionForConfiguration(cfg);
        if (cfgDescription != null)
            return getNaturesIdsUsedOnCache(cfgDescription);
        return null;
    }

    public static String[] getNaturesIdsUsedOnCache(ICConfigurationDescription cfgDescription) {
        String[] strs = (String[]) cfgDescription.getSessionProperty(NATURES_USED_ON_CACHE_PROPERTY);
        return strs != null && strs.length != 0 ? (String[]) strs.clone() : strs;
    }

    public static void cacheNaturesIdsUsedOnCache(ICConfigurationDescription cfgDescription) {
        IProject project = cfgDescription.getProjectDescription().getProject();
        try {
            IProjectDescription eDes = project.getDescription();
            String[] natures = eDes.getNatureIds();
            setNaturesIdsUsedOnCache(cfgDescription, natures);
        } catch (CoreException e) {
            Activator.log(e);
        }
    }

    private static void setNaturesIdsUsedOnCache(ICConfigurationDescription cfgDescription, String ids[]) {
        ids = ids != null && ids.length != 0 ? (String[]) ids.clone() : ids;
        cfgDescription.setSessionProperty(NATURES_USED_ON_CACHE_PROPERTY, ids);
    }

    private Configuration load(ICConfigurationDescription cfgDescription, ManagedProject mProj, boolean isPreference)
            throws CoreException {
        ICStorageElement rootElement = cfgDescription.getStorage(BUILD_SYSTEM_DATA_MODULE_NAME, true);
        ICStorageElement children[] = rootElement.getChildren();
        String version = rootElement.getAttribute(VERSION_ATTRIBUTE);
        Configuration cfg = null;

        for (int i = 0; i < children.length; i++) {
            if (IConfiguration.CONFIGURATION_ELEMENT_NAME.equals(children[i].getName())) {
                cfg = new Configuration(mProj, children[i], version, isPreference);
                ManagedBuildManager.performValueHandlerEvent(cfg, IManagedOptionValueHandler.EVENT_OPEN);
                break;
            }
        }
        return cfg;
    }

    protected CConfigurationData loadPreferences(ICConfigurationDescription cfgDescription) throws CoreException {
        Configuration cfg = load(cfgDescription, null, true);
        cfg = updatePreferenceOnLoad(cfg, cfgDescription);
        cfg.setConfigurationDescription(cfgDescription);
        return cfg.getConfigurationData();
    }



    private static Configuration updatePreferenceOnLoad(Configuration cfg, ICConfigurationDescription cfgDescription) {
        if (cfg == null) {
            cfg = createEmptyPrefConfiguration(cfgDescription.getId(), cfgDescription.getName());
        }
        cfg = adjustPreferenceConfig(cfg);
        return cfg;
    }

    private static Configuration adjustPreferenceConfig(Configuration cfg) {
        LanguageManager mngr = LanguageManager.getInstance();
        ILanguageDescriptor dess[] = mngr.getLanguageDescriptors();
        Map<String, ILanguageDescriptor[]> map = mngr.getContentTypeIdToLanguageDescriptionsMap();

        IResourceInfo[] rcInfos = cfg.getResourceInfos();
        for (int i = 0; i < rcInfos.length; i++) {
            if (rcInfos[i] instanceof IFolderInfo) {
                adjustFolderInfo((IFolderInfo) rcInfos[i], dess, new HashMap<Object, ILanguageDescriptor[]>(map));
            }
        }

        return cfg;
    }

    private static void adjustFolderInfo(IFolderInfo info, ILanguageDescriptor dess[],
            HashMap<Object, ILanguageDescriptor[]> map) {
        IToolChain tch = info.getToolChain();
        Map<String, ILanguageDescriptor> langMap = new HashMap<>();
        for (int i = 0; i < dess.length; i++) {
            langMap.put(dess[i].getId(), dess[i]);
        }
        if (PREF_TC_ID.equals(tch.getSuperClass().getId())) {
            ITool[] tools = tch.getTools();
            for (int i = 0; i < tools.length; i++) {
                Tool tool = (Tool) tools[i];
                IInputType types[] = tool.getAllInputTypes();
                for (int k = 0; k < types.length; k++) {
                    InputType type = (InputType) types[k];
                    String langId = type.getLanguageId(tool);
                    if (langId != null) {
                        ILanguageDescriptor des = langMap.remove(langId);
                        if (des != null)
                            adjustInputType(tool, type, des);
                        continue;
                    } else {
                    	List<IContentType> cTypes = type.getSourceContentTypes();
                        for (IContentType cType: cTypes) {
                            ILanguageDescriptor[] langs = map.remove(cType.getId());
                            if (langs != null && langs.length != 0) {
                                for (int q = 0; q < langs.length; q++) {
                                    langMap.remove(langs[q].getId());
                                }

                                adjustInputType(tool, type, langs[0]);
                            }
                        }
                    }
                }
            }

            if (!langMap.isEmpty()) {
                addTools(tch, langMap, map);
            }
        }
    }

    private static InputType adjustInputType(Tool tool, InputType type, ILanguageDescriptor des) {
        String[] cTypeIds = des.getContentTypeIds();
        String srcIds[] = type.getSourceContentTypeIds();
        String hIds[] =null;// type.getHeaderContentTypeIds();

        Set<String> landTypes = new HashSet<>(Arrays.asList(cTypeIds));
        landTypes.removeAll(Arrays.asList(srcIds));
        landTypes.removeAll(Arrays.asList(hIds));

        if (landTypes.size() != 0) {
            List<String> srcList = new ArrayList<>();
            srcList.addAll(landTypes);
            type = (InputType) tool.getEditableInputType(type);
        }

        if (!des.getId().equals(type.getLanguageId(tool))) {
            type = (InputType) tool.getEditableInputType(type);
        }
        return type;
    }

    private static void addTools(IToolChain tc, Map<String, ILanguageDescriptor> langMap,
            Map<Object, ILanguageDescriptor[]> cTypeToLangMap) {
        ITool extTool = ManagedBuildManager.getExtensionTool(PREF_TOOL_ID);
        List<ILanguageDescriptor> list = new ArrayList<>(langMap.values());
        ILanguageDescriptor des;
        while (list.size() != 0) {
            des = list.remove(list.size() - 1);
            String[] ctypeIds = des.getContentTypeIds();
            boolean addLang = false;
            for (int i = 0; i < ctypeIds.length; i++) {
                ILanguageDescriptor[] langs = cTypeToLangMap.remove(ctypeIds[i]);
                if (langs != null && langs.length != 0) {
                    addLang = true;
                    for (int q = 0; q < langs.length; q++) {
                        list.remove(langs[q]);
                    }
                }
            }

            if (addLang) {
                String id = ManagedBuildManager.calculateChildId(extTool.getId(), null);
                String name = des.getName();
                Tool tool = (Tool) tc.createTool(extTool, id, name, false);
                InputType type = (InputType) tool.getInputTypes()[0];
                type = (InputType) tool.getEditableInputType(type);
                type.setName(des.getName());
            }
        }
    }

    private static Configuration createEmptyPrefConfiguration(String id, String name) {
        Configuration extCfg = (Configuration) ManagedBuildManager.getExtensionConfiguration(PREF_CFG_ID);
        Configuration emptyPrefCfg = null;
        if (extCfg != null) {
            if (id == null)
                id = ManagedBuildManager.calculateChildId(extCfg.getId(), null);
            if (name == null)
                name = extCfg.getName();
            emptyPrefCfg = new Configuration(null, extCfg, id, false, true, true);
            emptyPrefCfg.setName(name);
            emptyPrefCfg.setPerRcTypeDiscovery(false);
        }

        return emptyPrefCfg;
    }

    @Override
    public CConfigurationData loadConfiguration(ICConfigurationDescription cfgDescription, IProgressMonitor monitor)
            throws CoreException {
        if (cfgDescription.isPreferenceConfiguration()) {
            return loadPreferences(cfgDescription);
        }

        IManagedBuildInfo info = getBuildInfo(cfgDescription);
        Configuration cfg = load(cfgDescription, (ManagedProject) info.getManagedProject(), false);

        if (cfg != null) {
            IProject project = cfgDescription.getProjectDescription().getProject();
            cfg.setConfigurationDescription(cfgDescription);
            info.setValid(true);
            setPersistedFlag(cfgDescription);
            cacheNaturesIdsUsedOnCache(cfgDescription);
            // Update the ManagedBuildInfo in the ManagedBuildManager map. Doing this creates a barrier for subsequent
            // ManagedBuildManager#getBuildInfo(...) see Bug 305146 for more
            ManagedBuildManager.setLoaddedBuildInfo(project, info);

            if (cfgDescription instanceof ILanguageSettingsProvidersKeeper) {
                String[] defaultIds = cfg.getDefaultLanguageSettingsProviderIds();
                if (defaultIds != null) {
                    ((ILanguageSettingsProvidersKeeper) cfgDescription)
                            .setDefaultLanguageSettingsProvidersIds(defaultIds);
                }
            }

            return cfg.getConfigurationData();
        }
        return null;
    }

    private boolean isPersistedCfg(ICConfigurationDescription cfgDescription) {
        return cfgDescription.getSessionProperty(CFG_PERSISTED_PROPERTY) != null;
    }

    //    @Override
    //    public void settingsChanged(SettingsChangeEvent event) {
    //        BuildLanguageData datas[] = (BuildLanguageData[]) event.getRcInfo().getCLanguageDatas();
    //        IOption option = event.getOption();
    //
    //        try {
    //            int type = option.getValueType();
    //            for (int i = 0; i < datas.length; i++) {
    //                datas[i].optionsChanged(type);
    //            }
    //        } catch (BuildException e) {
    //            Activator.log(e);
    //        }
    //    }

    @Override
    public void removeConfiguration(ICConfigurationDescription cfgDescription, CConfigurationData data,
            IProgressMonitor monitor) {
        IConfiguration cfg = ((BuildConfigurationData) data).getConfiguration();
        ManagedBuildManager.performValueHandlerEvent(cfg, IManagedOptionValueHandler.EVENT_CLOSE);
        IManagedBuildInfo info = getBuildInfo(cfgDescription);
        IManagedProject mProj = info.getManagedProject();
        mProj.removeConfiguration(cfg.getId());
    }

    @Override
    public void dataCached(ICConfigurationDescription cfgDescription, CConfigurationData data,
            IProgressMonitor monitor) {
        BuildConfigurationData cfgData = (BuildConfigurationData) data;
        ((Configuration) cfgData.getConfiguration()).setConfigurationDescription(cfgDescription);
        cfgData.clearCachedData();
    }

}
