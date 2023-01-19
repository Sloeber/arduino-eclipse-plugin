/*******************************************************************************
 * Copyright (c) 2007, 2018 Intel Corporation and others.
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

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.core.language.settings.providers.ScannerDiscoveryLegacySupport;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.make.core.MakeCorePlugin;
import org.eclipse.cdt.make.core.scannerconfig.IScannerConfigBuilderInfo2;
import org.eclipse.cdt.make.core.scannerconfig.IScannerConfigBuilderInfo2Set;
import org.eclipse.cdt.make.core.scannerconfig.InfoContext;
import org.eclipse.cdt.make.internal.core.scannerconfig2.ScannerConfigProfileManager;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.QualifiedName;

import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.api.IFileInfo;
import io.sloeber.autoBuild.api.IFolderInfo;
import io.sloeber.autoBuild.api.IInputType;
import io.sloeber.autoBuild.api.IResourceInfo;
import io.sloeber.autoBuild.api.ITool;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.integration.BuildConfigurationData;

public class CfgScannerConfigInfoFactory2 {
    private static final QualifiedName CONTAINER_INFO_PROPERTY = new QualifiedName(Activator.getId(),
            "ScannerConfigBuilderInfo2Container"); //$NON-NLS-1$

    private static class ContainerInfo {
        int fCode;
        IScannerConfigBuilderInfo2Set fContainer;

        ContainerInfo(ICProjectDescription des, IScannerConfigBuilderInfo2Set container) {
            this.fCode = des.hashCode();
            this.fContainer = container;
        }

        public boolean matches(ICProjectDescription des) {
            return des.hashCode() == fCode;
        }
    }

    private static class CfgInfo implements ICfgScannerConfigBuilderInfo2Set {
        private Configuration cfg;
        private SoftReference<IScannerConfigBuilderInfo2Set> fContainer;
        //		private HashMap map;

        CfgInfo(Configuration cfg) {
            this.cfg = cfg;
            //			init();
        }



        @Override
        public IScannerConfigBuilderInfo2 getInfo(CfgInfoContext context) {
            return createMap().get(context);
            //			IScannerConfigBuilderInfo2 info = null;
            //			if(!isPerRcTypeDiscovery()){
            //				info = cfg.getScannerConfigInfo();
            //				if(info == null){
            //					info = ScannerConfigInfoFactory2.create(cfg, ManagedBuilderCorePlugin.getDefault().getPluginPreferences());
            //				}
            //			} else {
            //				Tool tool = (Tool)context.getTool();
            //				if(tool != null)
            //					info = tool.getScannerConfigInfo(context.getInputType());
            ////				else
            ////					info = getDefaultInfo();
            //			}
            //			return info;
        }



        private IScannerConfigBuilderInfo2Set getContainer() throws CoreException {
            IScannerConfigBuilderInfo2Set container = fContainer != null ? fContainer.get() : null;
            if (container == null) {
                if (!cfg.isPreference()) {
                    ICConfigurationDescription cfgDes = ManagedBuildManager.getDescriptionForConfiguration(cfg);
                    if (cfgDes != null) {
                        ICProjectDescription projDes = cfgDes.getProjectDescription();
                        if (projDes != null) {
                            ContainerInfo cInfo = (ContainerInfo) projDes.getSessionProperty(CONTAINER_INFO_PROPERTY);
                            if (cInfo != null && cInfo.matches(projDes)) {
                                container = cInfo.fContainer;
                            } else {
                                container = ScannerConfigProfileManager
                                        .createScannerConfigBuildInfo2Set(cfg.getOwner().getProject());
                                cInfo = new ContainerInfo(projDes, container);
                                projDes.setSessionProperty(CONTAINER_INFO_PROPERTY, cInfo);
                            }
                        }
                    }

                    if (container == null) {
                        container = ScannerConfigProfileManager
                                .createScannerConfigBuildInfo2Set(cfg.getOwner().getProject());
                    }
                } else {
                    Preferences prefs = MakeCorePlugin.getDefault().getPluginPreferences();
                    container = ScannerConfigProfileManager.createScannerConfigBuildInfo2Set(prefs, false);
                }
            }

            if (fContainer == null) {
                fContainer = new SoftReference<>(container);
            }
            return container;
        }

        private Map<CfgInfoContext, IScannerConfigBuilderInfo2> createMap() {
            HashMap<CfgInfoContext, IScannerConfigBuilderInfo2> map = new HashMap<>();
            try {
                IScannerConfigBuilderInfo2Set container = getContainer();

                boolean isPerRcType = cfg.isPerRcTypeDiscovery();
                Map<InfoContext, IScannerConfigBuilderInfo2> baseMap = container.getInfoMap();
                if (!isPerRcType) {
                    // Discovery profile scope = configuration wide

                    CfgInfoContext c = new CfgInfoContext(cfg);
                    InfoContext baseContext = c.toInfoContext();
                    IScannerConfigBuilderInfo2 info = container.getInfo(baseContext);

                    if (info == null) {
                        String id = cfg.getDiscoveryProfileId();
                        if (id == null)
                            id = CfgScannerConfigUtil.getFirstProfileId(cfg.getFilteredTools());

                        IScannerConfigBuilderInfo2 prefInfo = null;
                        if (!cfg.isPreference()) {
//                            IConfiguration prefCfg = ManagedBuildManager.getPreferenceConfiguration(false);
//                            ICfgScannerConfigBuilderInfo2Set prefContainer = create(prefCfg);
//                            prefInfo = prefContainer.getInfo(new CfgInfoContext(prefCfg));
                        }
                        if (prefInfo == null) {
                            if (id != null)
                                info = container.createInfo(baseContext, id);
                            else
                                info = container.createInfo(baseContext);
                        } else {
                            if (id != null)
                                info = container.createInfo(baseContext, prefInfo, id);
                            else
                                info = container.createInfo(baseContext, prefInfo, prefInfo.getSelectedProfileId());
                        }
                    }
                    map.put(new CfgInfoContext(cfg), info);
                } else {
                    // Discovery profile scope = per language

                    Map<CfgInfoContext, IScannerConfigBuilderInfo2> configMap = getConfigInfoMap(baseMap);

//                    List<IResourceInfo> rcInfos = cfg.getResourceInfos();
//                    for (IResourceInfo rcInfo : rcInfos) {
//                        List<ITool> tools;
//                        if (rcInfo instanceof IFolderInfo) {
//                            tools = ((IFolderInfo) rcInfo).getFilteredTools();
//                        } else {
//                            tools = ((IFileInfo) rcInfo).getToolsToInvoke();
//                        }
//                        for (ITool tool : tools) {
//                            IInputType types[] = tool.getInputTypes();
//                            if (types.length != 0) {
//                                for (IInputType inputType : types) {
//                                    CfgInfoContext context = new CfgInfoContext(rcInfo, tool, inputType);
//                                    context = CfgScannerConfigUtil.adjustPerRcTypeContext(context);
//                                    if (context != null && context.getResourceInfo() != null) {
//                                        IScannerConfigBuilderInfo2 info = configMap.get(context);
//                                        if (info == null //&& !inputType.isExtensionElement()
//                                                && inputType.getSuperClass() != null) {
//                                            CfgInfoContext superContext = new CfgInfoContext(rcInfo, tool,
//                                                    inputType.getSuperClass());
//                                            superContext = CfgScannerConfigUtil.adjustPerRcTypeContext(superContext);
//                                            if (superContext != null && superContext.getResourceInfo() != null) {
//                                                info = configMap.get(superContext);
//                                            }
//
//                                            // Scanner discovery options aren't settable on a file-per-file basis. Thus
//                                            // files with custom properties don't have a persisted entry in the config
//                                            // info map; we create an ephemeral entry instead. We need to assign that file
//                                            // the scanner profile that's used for non-custom files of the same
//                                            // inputType/tool (and configuration, of course). Unfortunately, identifying
//                                            // a match is inefficient, but in practice, projects don't have tons of
//                                            // customized files. See Bug 354194
//                                            String id = null;
//                                            for (Entry<CfgInfoContext, IScannerConfigBuilderInfo2> entry : configMap
//                                                    .entrySet()) {
//                                                CfgInfoContext cfgInfoCxt = entry.getKey();
//                                                if (match(cfgInfoCxt.getInputType(), context.getInputType())
//                                                        && match(cfgInfoCxt.getTool(),
//                                                                context.getTool().getSuperClass())
//                                                        && cfgInfoCxt.getConfiguration()
//                                                                .equals(context.getConfiguration())) {
//                                                    id = entry.getValue().getSelectedProfileId();
//                                                }
//                                            }
//                                            if (id == null) {
//                                                // Language Settings Providers are meant to replace legacy scanner discovery
//                                                // so do not try to find default profile
//                                                ICConfigurationDescription cfgDescription = ManagedBuildManager
//                                                        .getDescriptionForConfiguration(cfg);
//                                                if (ScannerDiscoveryLegacySupport
//                                                        .isLegacyScannerDiscoveryOn(cfgDescription)) {
//                                                    id = CfgScannerConfigUtil.getDefaultProfileId(context, true);
//                                                }
//                                            }
//
//                                            InfoContext baseContext = context.toInfoContext();
//                                            if (info == null) {
//                                                if (id != null) {
//                                                    info = container.createInfo(baseContext, id);
//                                                } else {
//                                                    info = container.createInfo(baseContext);
//                                                }
//                                            } else {
//                                                if (id != null) {
//                                                    info = container.createInfo(baseContext, info, id);
//                                                } else {
//                                                    info = container.createInfo(baseContext, info);
//                                                }
//                                            }
//                                            // Make sure to remove the ephemeral info map entry from the
//                                            // container, otherwise it will not be ephemeral but rather a
//                                            // permanent and stagnant part of the project description. It was
//                                            // added to the container only so we could obtain an
//                                            // IScannerConfigBuilderInfo2. Now that we have the info object,
//                                            // revert the container. See Bug 354194. Note that the permanent
//                                            // entry for the project's root folder resource info gets created
//                                            // by us shortly after project creation; thus we have to make an
//                                            // exception for that rcinfo.
//                                            if (!(rcInfo instanceof IFolderInfo && rcInfo.getPath().isEmpty())) {
//                                                container.removeInfo(context.toInfoContext());
//                                            }
//                                        }
//                                        if (info != null) {
//                                            map.put(context, info);
//                                        }
//                                    }
//                                }
//                            } else {
//                                if (cfg.isPreference())
//                                    continue;
//                                CfgInfoContext context = new CfgInfoContext(rcInfo, tool, null);
//                                context = CfgScannerConfigUtil.adjustPerRcTypeContext(context);
//                                if (context != null && context.getResourceInfo() != null) {
//                                    IScannerConfigBuilderInfo2 info = configMap.get(context);
//                                    if (info == null) {
//                                        String id = CfgScannerConfigUtil.getDefaultProfileId(context, true);
//                                        InfoContext baseContext = context.toInfoContext();
//                                        if (id != null) {
//                                            info = container.createInfo(baseContext, id);
//                                        } else {
//                                            info = container.createInfo(baseContext);
//                                        }
//                                    }
//                                    if (info != null) {
//                                        map.put(context, info);
//                                    }
//                                }
//                            }
//                        }
//                    }

                    if (!configMap.isEmpty()) {
                        for (Entry<CfgInfoContext, IScannerConfigBuilderInfo2> entry : configMap.entrySet()) {
                            if (map.containsKey(entry.getKey()))
                                continue;
                            CfgInfoContext c = entry.getKey();
                            if (c.getResourceInfo() != null || c.getTool() != null || c.getInputType() != null) {
                                InfoContext baseC = c.toInfoContext();
                                if (!baseC.isDefaultContext())
                                    container.removeInfo(baseC);
                            }
                        }
                    }
                }
            } catch (CoreException e) {
                Activator.log(e);
            }

            return map;
        }

        private Map<CfgInfoContext, IScannerConfigBuilderInfo2> getConfigInfoMap(
                Map<InfoContext, IScannerConfigBuilderInfo2> baseMap) {
            Map<CfgInfoContext, IScannerConfigBuilderInfo2> map = new HashMap<>();

            for (Entry<InfoContext, IScannerConfigBuilderInfo2> entry : baseMap.entrySet()) {
                InfoContext baseContext = entry.getKey();
                CfgInfoContext c = CfgInfoContext.fromInfoContext(cfg, baseContext);
                if (c != null) {
                    IScannerConfigBuilderInfo2 info = entry.getValue();
                    map.put(c, info);
                }
            }

            return map;
        }

        @Override
        public Map<CfgInfoContext, IScannerConfigBuilderInfo2> getInfoMap() {
            return createMap();
        }



        @Override
        public IScannerConfigBuilderInfo2 applyInfo(CfgInfoContext context, IScannerConfigBuilderInfo2 base) {
            try {
                IScannerConfigBuilderInfo2 newInfo;
                IScannerConfigBuilderInfo2Set container = getContainer();
                InfoContext baseContext = context.toInfoContext();
                if (base != null) {
                    newInfo = container.createInfo(baseContext, base);
                } else {
                    if (!baseContext.isDefaultContext())
                        container.removeInfo(baseContext);
                    newInfo = getInfo(context);
                }

                return newInfo;
            } catch (CoreException e) {
                Activator.log(e);
            }
            return null;
        }

        @Override
        public IConfiguration getConfiguration() {
            return cfg;
        }


    }

//    public static ICfgScannerConfigBuilderInfo2Set create(IConfiguration cfg) {
//        Configuration c = (Configuration) cfg;
//        ICfgScannerConfigBuilderInfo2Set container = c.getCfgScannerConfigInfo();
//        if (container == null) {
//            container = new CfgInfo(c);
//            c.setCfgScannerConfigInfo(container);
//        }
//        return container;
//    }

    public static void save(BuildConfigurationData data, ICProjectDescription des, ICProjectDescription baseDescription,
            boolean force) throws CoreException {
//        ContainerInfo info = (ContainerInfo) des.getSessionProperty(CONTAINER_INFO_PROPERTY);
//        if (info != null) {
//            if (info.matches(baseDescription)) {
//                IScannerConfigBuilderInfo2Set baseContainer = info.fContainer;
//                baseContainer.save();
//            }
//            des.setSessionProperty(CONTAINER_INFO_PROPERTY, null);
//        } else if (force) {
//            Configuration cfg = (Configuration) data.getConfiguration();
//            CfgInfo cfgInfo = new CfgInfo(cfg);
//            cfg.setCfgScannerConfigInfo(cfgInfo);
//            cfgInfo.getInfoMap();
//            cfgInfo.getContainer().save();
//            des.setSessionProperty(CONTAINER_INFO_PROPERTY, null);
//        }
    }

    public static void savePreference(IConfiguration cfg) throws CoreException {
//        ICfgScannerConfigBuilderInfo2Set container = ((Configuration) cfg).getCfgScannerConfigInfo();
//        if (container != null) {
//            IScannerConfigBuilderInfo2Set baseContainer = ((CfgInfo) container).getContainer();
//            if (baseContainer != null) {
//                baseContainer.save();
//            }
//        }
    }


}
