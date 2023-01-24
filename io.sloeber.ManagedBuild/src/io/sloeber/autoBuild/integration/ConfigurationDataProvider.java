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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import io.sloeber.autoBuild.Internal.BuilderFactory;
import io.sloeber.autoBuild.Internal.ManagedBuildInfo;
import io.sloeber.autoBuild.Internal.ManagedBuildManager;
import io.sloeber.autoBuild.api.IManagedBuildInfo;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IManagedOptionValueHandler;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IFolderInfo;
import io.sloeber.schema.api.IManagedProject;
import io.sloeber.schema.api.IToolChain;
import io.sloeber.schema.internal.Builder;
import io.sloeber.schema.internal.Configuration;
import io.sloeber.schema.internal.ManagedProject;

/**
 * The main hook ManagedBuild uses to connect to cdt.core's project model.
 * Provides & Persists Build configuration data in the project model storage.
 */
public class ConfigurationDataProvider extends CConfigurationDataProvider {// implements ISettingsChangeListener {
    public static final String CFG_DATA_PROVIDER_ID = Activator.PLUGIN_ID + ".ConfigurationDataProvider"; //$NON-NLS-1$
    // private static final String PREF_TC_ID = "org.eclipse.cdt.build.core.prefbase.toolchain"; //$NON-NLS-1$

    public ConfigurationDataProvider() {
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

    @Override
    public CConfigurationData applyConfiguration(ICConfigurationDescription cfgDescription,
            ICConfigurationDescription baseCfgDescription, CConfigurationData baseData, IModificationContext context,
            IProgressMonitor monitor) throws CoreException {

        //TOFIX JABA need to add storage here
        AutoBuildConfigurationData autoBuildBaseData = (AutoBuildConfigurationData) baseData;

        IConfiguration baseCfg = autoBuildBaseData.getConfiguration();

        if (context.isBaseDataCached()) {// JABA Assume not dirty && !baseCfg.isDirty()
            context.setConfigurationSettingsFlags(IModificationContext.CFG_DATA_STORAGE_UNMODIFIED
                    | IModificationContext.CFG_DATA_SETTINGS_UNMODIFIED);
        } else {

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

                    if (baseCfg != null) {
                        defaultIds = baseCfg.getDefaultLanguageSettingsProviderIds().toArray(new String[0]);
                    }
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
        }

        if (cfgDescription.isActive()) {
            IBuilder builder = baseCfg.getBuilder();
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

        return baseData;
    }

    @Override
    public CConfigurationData createConfiguration(ICConfigurationDescription cfgDescription,
            ICConfigurationDescription baseCfgDescription, CConfigurationData base, boolean clone,
            IProgressMonitor monitor) throws CoreException {
        AutoBuildConfigurationData autoBuildConfigBase = (AutoBuildConfigurationData) base;
        return new AutoBuildConfigurationData(cfgDescription, autoBuildConfigBase);

        //        if (cfgDescription.isPreferenceConfiguration())
        //            return new AutoBuildConfigurationData(autoBuildConfigBase);
        //
        //        Configuration cfg = (Configuration) ((BuildConfigurationData) base).getConfiguration();
        //        Configuration newCfg = copyCfg(cfg, cfgDescription);
        //
        //        if (!newCfg.getId().equals(cfg.getId())) {
        //            // Bug 335001: Remove existing exported settings as they point at this
        //            // configuration
        //            for (ICExternalSetting extSetting : newCfg.getConfigurationDescription().getExternalSettings())
        //                newCfg.getConfigurationDescription().removeExternalSetting(extSetting);
        //            // Now export the new settings
        //            newCfg.exportArtifactInfo();
        //        }
        //
        //        setPersistedFlag(cfgDescription);
        //
        //        return newCfg.getConfigurationData();
        //        return null;
    }

    @Override
    public CConfigurationData loadConfiguration(ICConfigurationDescription cfgDescription, IProgressMonitor monitor)
            throws CoreException {
        //		if (cfgDescription.isPreferenceConfiguration()) {
        //			return loadPreferences(cfgDescription);
        //		}
        //
        //		IManagedBuildInfo info = getBuildInfo(cfgDescription);
        //		Configuration cfg = load(cfgDescription, (ManagedProject) info.getManagedProject(), false);
        //
        //		if (cfg != null) {
        //			IProject project = cfgDescription.getProjectDescription().getProject();
        //			cfg.setConfigurationDescription(cfgDescription);
        //			info.setValid(true);
        //			setPersistedFlag(cfgDescription);
        //			cacheNaturesIdsUsedOnCache(cfgDescription);
        //			// Update the ManagedBuildInfo in the ManagedBuildManager map. Doing this
        //			// creates a barrier for subsequent
        //			// ManagedBuildManager#getBuildInfo(...) see Bug 305146 for more
        //			ManagedBuildManager.setLoaddedBuildInfo(project, info);
        //
        //			if (cfgDescription instanceof ILanguageSettingsProvidersKeeper) {
        //				List<String> defaultIds = cfg.getDefaultLanguageSettingsProviderIds();
        //				if (defaultIds != null) {
        //					((ILanguageSettingsProvidersKeeper) cfgDescription)
        //							.setDefaultLanguageSettingsProvidersIds(defaultIds.toArray(new String[defaultIds.size()]));
        //				}
        //			}
        //
        //			return cfg.getConfigurationData();
        //		}
        return null;
    }

    @Override
    public void removeConfiguration(ICConfigurationDescription cfgDescription, CConfigurationData data,
            IProgressMonitor monitor) {
        //		IConfiguration cfg = ((BuildConfigurationData) data).getConfiguration();
        //		ManagedBuildManager.performValueHandlerEvent(cfg, IManagedOptionValueHandler.EVENT_CLOSE);
        //		IManagedBuildInfo info = getBuildInfo(cfgDescription);
        //		IManagedProject mProj = info.getManagedProject();
        //		mProj.removeConfiguration(cfg.getId());
        return;
    }

    @Override
    public void dataCached(ICConfigurationDescription cfgDescription, CConfigurationData data,
            IProgressMonitor monitor) {
        //		AutoBuildConfigurationDescription cfgData = (BuildConfigurationData) data;
        //		((Configuration) cfgData.getConfiguration()).setConfigurationDescription(cfgDescription);
        //		cfgData.clearCachedData();
        return;
    }

    //        private CConfigurationData applyPreferences(ICConfigurationDescription cfgDescription, CConfigurationData baseData)
    //                throws CoreException {
    //            AutoBuildConfigurationDescription appliedCfg = writeConfiguration(cfgDescription,
    //                    (BuildConfigurationData) baseData);
    //    
    //            IConfiguration cfg = ((BuildConfigurationData) baseData).getConfiguration();
    //            try {
    //                CfgScannerConfigInfoFactory2.savePreference(cfg);
    //            } catch (CoreException e) {
    //                ManagedBuilderCorePlugin.log(e);
    //            }
    //    
    //            return appliedCfg;
    //        }
    //
    //    private CConfigurationData createPreferences(ICConfigurationDescription cfgDescription,
    //            AutoBuildConfigurationData baseData) throws CoreException {
    //        Configuration cfg = (Configuration) baseData.getConfiguration();
    //        Configuration newCfg = new Configuration((ManagedProject) cfg.getManagedProject(), cfg, cfgDescription.getId(),
    //                true, true, true);
    //        newCfg.setConfigurationDescription(cfgDescription);
    //        newCfg.setName(cfgDescription.getName());
    //        //      if(!newCfg.getId().equals(cfg.getId())){
    //        //          newCfg.exportArtifactInfo();
    //        //      }
    //
    //        return newCfg.getConfigurationData();
    //    }
}
