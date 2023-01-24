/*******************************************************************************
 * Copyright (c) 2006, 2015 Intel Corporation and others.
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
 * Baltasar Belyavsky (Texas Instruments) - [405744] PropertyManager causes many unnecessary file-writes into workspace metadata
 *******************************************************************************/
package io.sloeber.buildProperties;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
//import org.eclipse.cdt.managedbuilder.core.IBuildObject;
//import org.eclipse.cdt.managedbuilder.core.IBuilder;
//import org.eclipse.cdt.managedbuilder.core.IConfiguration;
//import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
//import org.eclipse.cdt.managedbuilder.core.IManagedProject;
//import org.eclipse.cdt.managedbuilder.core.IResourceConfiguration;
//import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
//import org.eclipse.cdt.managedbuilder.core.ITool;
//import org.eclipse.cdt.managedbuilder.core.IToolChain;
//import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
//import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import io.sloeber.autoBuild.Internal.ManagedBuildManager;
import io.sloeber.autoBuild.api.IManagedBuildInfo;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationData;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IManagedProject;
import io.sloeber.schema.api.IResourceInfo;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.api.IToolChain;
import io.sloeber.schema.internal.Configuration;
import io.sloeber.schema.internal.IBuildObject;
import io.sloeber.schema.internal.ManagedProject;

/**
 * This class allows specifying BuildObject-specific persisted properties
 *
 */
public class PropertyManager {
    //	private static final String PROPS_PROPERTY = "properties";	//$NON-NLS-1$
    //	private static final QualifiedName propsSessionProperty = new QualifiedName(ManagedBuilderCorePlugin.getUniqueIdentifier(), PROPS_PROPERTY);

    private static final String NODE_NAME = "properties"; //$NON-NLS-1$

    private static PropertyManager fInstance;

    private LoaddedInfo fLoaddedInfo;

    private static class LoaddedInfo {
        private final IProject fProject;
        private final ICConfigurationDescription fConfigDesc;
        private final String fCfgId;
        private final IConfiguration config;
        // one of Map<String, String> or Map<String, Map<String, Properties>>
        private final Map<String, Object> fCfgPropertyMap;

        LoaddedInfo(ICConfigurationDescription ConfigDesc, Map<String, Object> cfgPropertyMap) {
            fConfigDesc = ConfigDesc;
            fProject = ConfigDesc.getProjectDescription().getProject();
            fCfgPropertyMap = cfgPropertyMap;
            AutoBuildConfigurationData autoConfig = (AutoBuildConfigurationData) fConfigDesc.getConfigurationData();
            config = autoConfig.getConfiguration();
            fCfgId = config.getId();
        }

        public IConfiguration getConfiguration() {
            return config;
        }

        public ICConfigurationDescription getConfigurationDescription() {
            return fConfigDesc;
        }

        public IProject getProject() {
            return fProject;
        }

        public String getConfigurationId() {
            return fCfgId;
        }

        public Map<String, Object> getProperties() {
            return fCfgPropertyMap;
        }

        public boolean cfgMatch(ICConfigurationDescription cfg) {
            if (fCfgId == null || fProject == null)
                return false;

            if (!fCfgId.equals(cfg.getId()))
                return false;

            if (!fProject.equals(cfg.getProjectDescription().getProject()))
                return false;

            return true;
        }
    }

    private PropertyManager() {
    }

    public static PropertyManager getInstance() {
        if (fInstance == null)
            fInstance = new PropertyManager();
        return fInstance;
    }

    protected void setProperty(ICConfigurationDescription cfg, IBuildObject bo, String prop, String value) {
        if (((Configuration) cfg).isPreference())
            return;
        Properties props = getProperties(cfg, bo);
        if (props != null) {
            props.setProperty(prop, value);
        }
    }

    protected String getProperty(ICConfigurationDescription cfg, IBuildObject bo, String prop) {
        if (((Configuration) cfg).isPreference())
            return null;
        Properties props = getProperties(cfg, bo);
        if (props != null)
            return props.getProperty(prop);
        return null;
    }

    protected Properties getProperties(ICConfigurationDescription cfg, IBuildObject bo) {
        return loadProperties(cfg, bo);
    }

    private LoaddedInfo getLoaddedInfo() {
        return fLoaddedInfo;
    }

    private synchronized void setLoaddedInfo(LoaddedInfo info) {
        fLoaddedInfo = info;
    }

    protected Map<String, Object> getLoaddedData(ICConfigurationDescription cfg) {
        LoaddedInfo info = getLoaddedInfo();
        if (info == null)
            return null;

        if (!info.cfgMatch(cfg))
            return null;

        return info.getProperties();
        //		Map map = null;
        //		IProject proj = null;
        //		try {
        //			if(!((Configuration)cfg).isPreference()){
        //				proj = cfg.getOwner().getProject();
        //				map = (Map)proj.getSessionProperty(propsSessionProperty);
        //			}
        //			if(map == null){
        //				map = new HashMap();
        //				if(proj != null){
        //					proj.setSessionProperty(propsSessionProperty, map);
        //				}
        //			}
        //			map = (Map)map.get(cfg.getId());
        //		} catch (CoreException e) {
        //		}
        //		return map;
    }

    protected synchronized void clearLoaddedData(ICConfigurationDescription cfg) {
        if (((Configuration) cfg).isPreference())
            return;

        LoaddedInfo info = getLoaddedInfo();
        if (info == null)
            return;

        if (info.cfgMatch(cfg))
            setLoaddedInfo(null);
        //		IProject proj = cfg.getOwner().getProject();
        //		try {
        //			proj.setSessionProperty(propsSessionProperty, null);
        //		} catch (CoreException e) {
        //		}
    }

    //    private static IProject getProject(IConfiguration cfg) {
    //        IResource rc = cfg.getOwner();
    //        return rc != null ? rc.getProject() : null;
    //    }

    protected Properties loadProperties(ICConfigurationDescription cfg, IBuildObject bo) {
        Map<String, Object> map = getData(cfg);

        return getPropsFromData(map, bo);
    }

    protected Properties getPropsFromData(Map<String, Object> data, IBuildObject bo) {
        synchronized (data) {
            Object oVal = data.get(bo.getId());
            Properties props = null;
            if (oVal instanceof String) {
                props = stringToProps((String) oVal);
                data.put(bo.getId(), props);
            } else if (oVal instanceof Properties) {
                props = (Properties) oVal;
            }

            if (props == null) {
                props = new Properties();
                data.put(bo.getId(), props);
            }

            return props;
        }
    }

    protected void storeData(ICConfigurationDescription cfg) {
        Map<String, Object> map = getLoaddedData(cfg);

        if (map != null)
            storeData(cfg, map);
    }

    protected Properties mapToProps(Map<String, Object> map) {
        Properties props = null;
        if (map != null) {
            synchronized (map) {
                if (map.size() > 0) {
                    props = new Properties();
                    Set<Entry<String, Object>> entrySet = map.entrySet();
                    for (Entry<String, Object> entry : entrySet) {
                        String key = entry.getKey();
                        String value = null;
                        Object oVal = entry.getValue();
                        if (oVal instanceof Properties) {
                            value = propsToString((Properties) oVal);
                        } else if (oVal instanceof String) {
                            value = (String) oVal;
                        }

                        if (key != null && value != null)
                            props.setProperty(key, value);
                    }
                }
            }
        }

        return props;
    }

    protected String propsToString(Properties props) {
        if (props == null || props.size() == 0)
            return null;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            props.store(stream, null);
        } catch (IOException e1) {
        }

        byte[] bytes = stream.toByteArray();

        String value = null;
        try {
            value = new String(bytes, "UTF-8"); //$NON-NLS-1$
        } catch (UnsupportedEncodingException e) {
            value = new String(bytes);
        }

        /* FIX for Bug 405744: Properties.store() always starts the serialzed string with
         * a timestamp comment. That constantly changing comment causes the preference-store
         * to perform many unnecessary file-writes into the workspace metadata, even when
         * the properties don't change. The comment is ignored by Properties.load(), so
         * just remove it here.
         */
        String sep = System.getProperty("line.separator"); //$NON-NLS-1$
        while (value.charAt(0) == '#') {
            value = value.substring(value.indexOf(sep) + sep.length());
        }

        return value;
    }

    protected Properties stringToProps(String str) {
        Properties props = null;
        if (str != null) {
            props = new Properties();
            byte[] bytes;
            try {
                bytes = str.getBytes("UTF-8"); //$NON-NLS-1$
            } catch (UnsupportedEncodingException e) {
                bytes = str.getBytes();
            }

            ByteArrayInputStream stream = new ByteArrayInputStream(bytes);
            try {
                props.load(stream);
            } catch (IOException e) {
                props = null;
            }
        }
        return props;
    }

    protected void storeData(ICConfigurationDescription cfg, Map<String, Object> map) {
        String str = null;
        Properties props = mapToProps(map);

        str = propsToString(props);

        storeString(cfg, str);
    }

    private Preferences getPreferences() {
        IProject project = fLoaddedInfo.getProject();
        return getNode(CCorePlugin.getDefault().getProjectDescription(project));
    }

    protected void storeString(ICConfigurationDescription cfg, String str) {
        Preferences prefs = getPreferences();
        if (prefs != null) {
            if (str != null)
                prefs.put(cfg.getId(), str);
            else
                prefs.remove(cfg.getId());
            try {
                prefs.flush();
            } catch (BackingStoreException e) {
                Activator.log(e);
            }
        }
    }

    protected String loadString(ICConfigurationDescription cfg) {
        Preferences prefs = getPreferences();
        String str = null;
        if (prefs != null)
            str = prefs.get(cfg.getId(), null);
        return str;
    }

    @SuppressWarnings("static-method")
    protected Preferences getNode(ICProjectDescription projectDescription) {
        Preferences prefs = InstanceScope.INSTANCE.getNode(Activator.getId());
        if (prefs != null) {
            prefs = prefs.node(NODE_NAME);
            if (prefs != null)
                prefs = prefs.node(projectDescription.getId());
        }
        return prefs;
    }

    @SuppressWarnings("static-method")
    protected Preferences getProjNode(IManagedProject mProject) {
        IProject project = mProject.getOwner().getProject();
        if (project == null || !project.exists() || !project.isOpen())
            return null;

        Preferences prefs = new ProjectScope(project).getNode(Activator.getId());
        if (prefs != null)
            return prefs.node(NODE_NAME);
        return null;
    }

    protected Map<String, Object> getData(ICConfigurationDescription cfg) {
        Map<String, Object> map = getLoaddedData(cfg);

        if (map == null) {
            map = loadData(cfg);

            setLoaddedData(cfg, map);
        }

        return map;
    }

    protected Map<String, Object> loadData(ICConfigurationDescription cfg) {
        Map<String, Object> map = null;
        String str = loadString(cfg);

        Properties props = stringToProps(str);

        map = propsToMap(props);

        if (map == null)
            map = new LinkedHashMap<>();

        return map;
    }

    @SuppressWarnings("static-method")
    protected Map<String, Object> propsToMap(Properties props) {
        if (props != null) {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Map<String, Object> map = new LinkedHashMap(props);
            return map;
        }
        return null;
    }

    private static IConfiguration getConfigurationFromId(IProject project, String id) {
        if (project == null || id == null)
            return null;
        IManagedBuildInfo bInfo = ManagedBuildManager.getBuildInfo(project, false);
        IConfiguration cfg = null;
        if (bInfo != null) {
            IManagedProject mProj = bInfo.getManagedProject();
            if (mProj != null) {
                cfg = mProj.getConfiguration(id);
            }
        }

        return cfg;
    }

    protected void setLoaddedData(ICConfigurationDescription cfg, Map<String, Object> data) {
        LoaddedInfo info = getLoaddedInfo();

        if (info != null) {
            if (info.cfgMatch(cfg)) {
                info = new LoaddedInfo(info.getConfigurationDescription(), data);
                setLoaddedInfo(info);
                return;
            }

            ICConfigurationDescription oldCfg = info.getConfigurationDescription();
            if (oldCfg != null) {
                storeData(oldCfg, info.getProperties());
            }
        }

        IProject proj = cfg.getProjectDescription().getProject();
        info = new LoaddedInfo(cfg, data);
        setLoaddedInfo(info);
    }

    //    public void setProperty(ITool tool, String key, String value) {
    //        Configuration cfg = (Configuration) getConfiguration(tool);
    //        if (cfg.isPreference())
    //            return;
    //        setProperty(cfg, tool, key, value);
    //    }

    public void clearProperties(ICProjectDescription projectDescription) {
        if (projectDescription == null)
            return;

        ICConfigurationDescription[] cfgs = projectDescription.getConfigurations();
        for (int i = 0; i < cfgs.length; i++)
            clearLoaddedData(cfgs[i]);

        Preferences prefs = getNode(projectDescription);
        if (prefs != null) {
            try {
                Preferences parent = prefs.parent();
                prefs.removeNode();
                if (parent != null)
                    parent.flush();
            } catch (BackingStoreException e) {
                Activator.log(e);
            }
        }
    }

    public void clearProperties(ICConfigurationDescription cfg) {
        clearLoaddedData(cfg);
        storeData(cfg, null);
    }

    @SuppressWarnings("static-method")
    private IConfiguration getConfiguration(IBuilder builder) {
        IToolChain tc = builder.getParent();
        if (tc != null)
            return tc.getParent();
        return null;
    }

    //    private IConfiguration getConfiguration(ITool tool) {
    //        IBuildObject p = tool.getParent();
    //        IConfiguration cfg = null;
    //        if (p instanceof IToolChain) {
    //            cfg = ((IToolChain) p).getParent();
    //        } else if (p instanceof IResourceConfiguration) {
    //            cfg = ((IResourceConfiguration) p).getParent();
    //        }
    //        return cfg;
    //    }

    public void serialize(ICConfigurationDescription cfg) {
        storeData(cfg);
    }

    public void serialize() {
        LoaddedInfo info = getLoaddedInfo();
        ICConfigurationDescription cfg = info.getConfigurationDescription();
        if (cfg != null) {
            serialize(cfg);

            clearLoaddedData(cfg);
        }
        //		IProject projects[] = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        //		for(int i = 0; i < projects.length; i++){
        //			IManagedBuildInfo info = ManagedBuildManager.getBuildInfo(projects[i], false);
        //			if(info != null && info.isValid() && info.getManagedProject() != null){
        //				IConfiguration cfgs[] = info.getManagedProject().getConfigurations();
        //				for(int j = 0; j < cfgs.length; j++){
        //					serialize(cfgs[j]);
        //				}
        //			}
        //		}
    }

}
