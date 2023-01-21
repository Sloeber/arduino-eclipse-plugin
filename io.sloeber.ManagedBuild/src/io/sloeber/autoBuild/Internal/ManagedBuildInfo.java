/*******************************************************************************
 * Copyright (c) 2002, 2016 IBM Software Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Rational Software - Initial API and implementation
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IContainerEntry;
import org.eclipse.cdt.core.model.IIncludeEntry;
import org.eclipse.cdt.core.model.IMacroEntry;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.cdt.core.model.IPathEntryContainer;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.core.settings.model.WriteAccessException;
//import org.eclipse.cdt.managedbuilder.core.BuildException;
//import org.eclipse.cdt.managedbuilder.core.IBuildObject;
//import org.eclipse.cdt.managedbuilder.core.IBuilder;
//import org.eclipse.cdt.managedbuilder.core.IConfiguration;
//import org.eclipse.cdt.managedbuilder.core.IEnvVarBuildPath;
//import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
//import org.eclipse.cdt.managedbuilder.core.IManagedCommandLineInfo;
//import org.eclipse.cdt.managedbuilder.core.IManagedProject;
//import org.eclipse.cdt.managedbuilder.core.IOption;
//import org.eclipse.cdt.managedbuilder.core.IOptionApplicability;
//import org.eclipse.cdt.managedbuilder.core.IOptionPathConverter;
//import org.eclipse.cdt.managedbuilder.core.IResourceConfiguration;
//import org.eclipse.cdt.managedbuilder.core.ITarget;
//import org.eclipse.cdt.managedbuilder.core.ITool;
//import org.eclipse.cdt.managedbuilder.core.IToolChain;
//import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
//import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
//import org.eclipse.cdt.managedbuilder.envvar.IEnvironmentVariableProvider;
//import org.eclipse.cdt.managedbuilder.internal.macros.OptionContextData;
//import org.eclipse.cdt.managedbuilder.internal.scannerconfig.ManagedBuildCPathEntryContainer;
//import org.eclipse.cdt.managedbuilder.macros.BuildMacroException;
//import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
//import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGeneratorType;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.autoBuild.api.BuildMacroException;
import io.sloeber.autoBuild.api.IBuildMacroProvider;
import io.sloeber.autoBuild.api.IManagedBuildInfo;
import io.sloeber.autoBuild.api.IOptionPathConverter;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IOptionApplicability;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IManagedProject;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.IResourceConfiguration;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.api.IToolChain;
import io.sloeber.schema.internal.IBuildObject;
import io.sloeber.schema.internal.ManagedProject;

/**
 * Concrete IManagedBuildInfo storing runtime ManagedProject metadata with
 * utility settings for accessing
 * some attributes in the default configuration
 */
public class ManagedBuildInfo implements IManagedBuildInfo, IScannerInfo {
    // The path container used for all managed projects
    public static final IContainerEntry containerEntry = CoreModel
            .newContainerEntry(new Path("org.eclipse.cdt.managedbuilder.MANAGED_CONTAINER")); //$NON-NLS-1$
    //	private static final QualifiedName defaultConfigProperty = new QualifiedName(ManagedBuilderCorePlugin.getUniqueIdentifier(), DEFAULT_CONFIGURATION);
    //private static final QualifiedName defaultTargetProperty = new QualifiedName(ManagedBuilderCorePlugin.getUniqueIdentifier(), DEFAULT_TARGET);

    private volatile ManagedProject managedProject;
    private volatile ICProject cProject;
    private volatile boolean isValid = false;
    private volatile IResource owner;
    private volatile boolean rebuildNeeded;
    private volatile String version;
    private volatile IConfiguration selectedConfig;

    private volatile boolean bIsContainerInited = false;

    /**
     * Basic constructor used when the project is brand new.
     */
    public ManagedBuildInfo(IResource owner) {
        this.owner = owner;
        cProject = CoreModel.getDefault().create(owner.getProject());

        // Does not need a save but should be rebuilt
        rebuildNeeded = true;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IManagedBuildInfo#setManagedProject(IManagedProject)
     */
    @Override
    public void setManagedProject(ManagedProject managedProject) {
        this.managedProject = managedProject;
        //setDirty(true);  - It is primarily up to the ManagedProject to maintain the dirty state
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IManagedBuildInfo#getManagedProject()
     */
    @Override
    public ManagedProject getManagedProject() {
        return managedProject;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo#getBuildArtifactExtension()
     */
    @Override
    public String getBuildArtifactExtension() {
        String ext = ""; //$NON-NLS-1$
        IConfiguration config = getDefaultConfiguration();
        if (config != null) {
            ext = config.getArtifactExtension();
        }
        return ext;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IManagedBuildInfo#getBuildArtifactName()
     */
    @Override
    public String getBuildArtifactName() {
        // Get the default configuration and use its value
        String name = ""; //$NON-NLS-1$
        IConfiguration config = getDefaultConfiguration();
        if (config != null) {
            name = config.getArtifactName();
        }
        return name;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IManagedBuildInfo#getCleanCommand()
     */
    @Override
    public String getCleanCommand() {
        // Get from the model
        String command = ""; //$NON-NLS-1$
        IConfiguration config = getDefaultConfiguration();
        if (config != null) {
            command = config.getCleanCommand();
        }
        return command;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IManagedBuildInfo#getConfigurationName()
     */
    @Override
    public String getConfigurationName() {
        // Return the human-readable name of the default configuration
        IConfiguration config = getDefaultConfiguration();
        return config == null ? "" : config.getName(); //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IManagedBuildInfo#getConfigurationNames()
     */
    @Override
    public Set<String> getConfigurationNames() {
        Set<String> configNames = new HashSet<>();
        IConfiguration[] configs = managedProject.getConfigurations();
        for (int i = 0; i < configs.length; i++) {
            IConfiguration configuration = configs[i];
            configNames.add(configuration.getName());
        }
        return configNames;
    }

    public ICProject getCProject() {
        return cProject;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IManagedBuildInfo#getDefaultConfiguration()
     */
    @Override
    public IConfiguration getDefaultConfiguration() {
        // Get the default config associated with the project
        /*		if (defaultConfig == null) {
        			if (managedProject != null) {
        				if (defaultConfigId != null) {
        					defaultConfig = managedProject.getConfiguration(defaultConfigId);
        				}
        				if (defaultConfig == null) {
        					IConfiguration[] configs = managedProject.getConfigurations();
        					for (int i = 0; i < configs.length; i++){
        						if (configs[i].isSupported()){
        							defaultConfig = configs[i];
        							defaultConfigId = defaultConfig.getId();
        							break;
        						}
        					}
        					if (defaultConfig == null && configs.length > 0) {
        						defaultConfig = configs[0];
        						defaultConfigId = defaultConfig.getId();
        					}
        				}
        			}
        		}
        		return defaultConfig;
        */
        IConfiguration activeCfg = findExistingDefaultConfiguration(null);

        if (activeCfg == null) {
            IConfiguration cfgs[] = managedProject.getConfigurations();
            if (cfgs.length != 0)
                activeCfg = cfgs[0];
        }

        return activeCfg;

    }

    private IConfiguration findExistingDefaultConfiguration(ICProjectDescription in_des) {
        ICProjectDescription des = in_des;
        if (des == null)
            des = CoreModel.getDefault().getProjectDescription(getOwner().getProject(), false);
        IConfiguration activeCfg = null;
        if (des != null) {
            ICConfigurationDescription cfgDes = des.getActiveConfiguration();
            activeCfg = managedProject.getProjectType().getConfiguration(cfgDes.getName());
            //activeCfg = managedProject.getConfiguration(cfgDes.getName());
        }

        return activeCfg;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IScannerInfo#getDefinedSymbols()
     */
    @Override
    public Map<String, String> getDefinedSymbols() {
        // Return the defined symbols for the default configuration
        HashMap<String, String> symbols = getMacroPathEntries();
        return symbols;
    }

    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo#getDependencyGenerator(java.lang.String)
    //     */
    //    @Override
    //    public IManagedDependencyGeneratorType getDependencyGenerator(String sourceExtension) {
    //        // Find the tool and ask the Managed Build Manager for its dep generator
    //        try {
    //            if (getDefaultConfiguration() != null) {
    //                ITool[] tools = getDefaultConfiguration().getFilteredTools();
    //                for (int index = 0; index < tools.length; ++index) {
    //                    if (tools[index].buildsFileType(sourceExtension)) {
    //                        return tools[index].getDependencyGeneratorForExtension(sourceExtension);
    //                    }
    //                }
    //            }
    //        } catch (NullPointerException e) {
    //            return null;
    //        }
    //
    //        return null;
    //    }

    /* (non-Javadoc)
     * Helper method to extract a list of valid tools that are filtered by the
     * project nature.
     *
     * @return
     */
    private List<ITool> getFilteredTools() {
        // Get all the tools for the current config filtered by the project nature
        IConfiguration config = getDefaultConfiguration();
        return config.getFilteredTools();
    }

    private ArrayList<String> getIncludePathEntries() {
        // Extract the resolved paths from the project (if any)
        ArrayList<String> paths = new ArrayList<>();
        if (cProject != null) {
            try {
                IPathEntry[] entries = cProject.getResolvedPathEntries();
                for (int index = 0; index < entries.length; ++index) {
                    int kind = entries[index].getEntryKind();
                    if (kind == IPathEntry.CDT_INCLUDE) {
                        IIncludeEntry include = (IIncludeEntry) entries[index];
                        if (include.isSystemInclude()) {
                            IPath entryPath = include.getFullIncludePath();
                            paths.add(entryPath.toString());
                        }
                    }
                }
            } catch (CModelException e) {
                // Just return an empty array
                paths.clear();
                return paths;
            }
        }
        return paths;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IScannerInfo#getIncludePaths()
     */
    @Override
    public String[] getIncludePaths() {
        // Return the include paths for the default configuration
        ArrayList<String> paths = getIncludePathEntries();
        return paths.toArray(new String[paths.size()]);
    }

    private HashMap<String, String> getMacroPathEntries() {
        HashMap<String, String> macros = new HashMap<>();
        if (cProject != null) {
            try {
                IPathEntry[] entries = cProject.getResolvedPathEntries();
                for (int index = 0; index < entries.length; ++index) {
                    if (entries[index].getEntryKind() == IPathEntry.CDT_MACRO) {
                        IMacroEntry macro = (IMacroEntry) entries[index];
                        macros.put(macro.getMacroName(), macro.getMacroValue());
                    }
                }
            } catch (CModelException e) {
                // return an empty map
                macros.clear();
                return macros;
            }

        }
        return macros;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IManagedBuildInfo#getMakeArguments()
     */
    @Override
    public String getBuildArguments() {
        if (getDefaultConfiguration() != null) {
            IToolChain toolChain = getDefaultConfiguration().getToolChain();
            IBuilder builder = toolChain.getBuilder();
            if (builder != null) {
                return builder.getArguments();
            }
        }
        return "-k"; //$NON-NLS-1$
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IManagedBuildInfo#getMakeCommand()
     */
    @Override
    public String getBuildCommand() {
        if (getDefaultConfiguration() != null) {
            IToolChain toolChain = getDefaultConfiguration().getToolChain();
            IBuilder builder = toolChain.getBuilder();
            if (builder != null) {
                return builder.getCommand();
            }
        }
        return "make"; //$NON-NLS-1$
    }

    @Override
    public String getPrebuildStep() {
        // Get the default configuration and use its value
        String name = ""; //$NON-NLS-1$
        IConfiguration config = getDefaultConfiguration();
        if (config != null) {
            name = config.getPrebuildStep();
        }
        return name;
    }

    @Override
    public String getPostbuildStep() {
        // Get the default configuration and use its value
        String name = ""; //$NON-NLS-1$
        IConfiguration config = getDefaultConfiguration();
        if (config != null) {
            name = config.getPostbuildStep();
        }
        return name;
    }

    @Override
    public String getPreannouncebuildStep() {
        // Get the default configuration and use its value
        String name = ""; //$NON-NLS-1$
        IConfiguration config = getDefaultConfiguration();
        if (config != null) {
            name = config.getPreannouncebuildStep();
        }
        return name;
    }

    @Override
    public String getPostannouncebuildStep() {
        // Get the default configuration and use its value
        String name = ""; //$NON-NLS-1$
        IConfiguration config = getDefaultConfiguration();
        if (config != null) {
            name = config.getPostannouncebuildStep();
        }
        return name;
    }

    /**
     * @return IResource owner
     */
    public IResource getOwner() {
        return owner;
    }

    //    /* (non-Javadoc)
    //     * @see org.eclipse.cdt.core.build.managed.IManagedBuildInfo#getToolForConfiguration(java.lang.String)
    //     */
    //    @Override
    //    public String getToolForConfiguration(String extension) {
    //        // Treat a null argument as an empty string
    //        String ext = extension == null ? "" : extension; //$NON-NLS-1$
    //        // Get all the tools for the current config
    //        List<ITool> tools = getFilteredTools();
    //        for (ITool tool : tools) {
    //            if (tool.producesFileType(ext)) {
    //                return tool.getToolCommand();
    //            }
    //        }
    //        return null;
    //    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo#getVersion()
     */
    @Override
    public String getVersion() {
        return version;
    }

    /* (non-Javadoc)
     *
     */
    public void initializePathEntries() {
        if (!isValid())
            return;
        try {
            IPathEntryContainer container = new ManagedBuildCPathEntryContainer(getOwner().getProject());
            CoreModel.setPathEntryContainer(new ICProject[] { cProject }, container, new NullProgressMonitor());
        } catch (CModelException e) {
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo#isValid()
     */
    @Override
    public boolean isValid() {
        // If the info has been flagged as valid, answer true
        return isValid;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo#isHeaderFile(java.lang.String)
     */
    @Override
    public boolean isHeaderFile(String ext) {
        // Check to see if there is a rule to build a file with this extension
        IConfiguration config = getDefaultConfiguration();
        return config.isHeaderFile(ext);
    }

    /**
     *
     * @return boolean
     */
    public boolean isContainerInited() {
        return bIsContainerInited;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo#needsRebuild()
     */
    @Override
    public boolean needsRebuild() {
        if (rebuildNeeded)
            return true;

        //        if (getDefaultConfiguration() != null) {
        //            return getDefaultConfiguration().needsRebuild();
        //        }
        return false;
    }

    /* (non-Javadoc)
     *
     */
    /*	private void persistDefaultConfiguration() {
    		// Persist the default configuration
    		IProject project = owner.getProject();
    		try {
    			if(defaultConfigId != null)
    				project.setPersistentProperty(defaultConfigProperty, defaultConfigId.toString().trim());
    		} catch (CoreException e) {
    			// Too bad
    		}
    	}
    */

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IManagedBuildInfo#setDefaultConfiguration(org.eclipse.cdt.core.build.managed.IConfiguration)
     */
    @Override
    public void setDefaultConfiguration(IConfiguration configuration) {
        // TODO:  This is probably wrong.  I'll bet we don't handle the case where all configs are deleted...
        //        But, at least, our UI does not allow the last config to be deleted.
        // Sanity
        if (configuration == null)
            return;

        ICProjectDescription des = null;
        try {
            des = BuildSettingsUtil.checkSynchBuildInfo(getOwner().getProject());
        } catch (CoreException e1) {
            Activator.log(e1);
        }

        if (!configuration.equals(findExistingDefaultConfiguration(des))) {
            IProject project = owner.getProject();
            if (des == null)
                des = CoreModel.getDefault().getProjectDescription(project);
            if (des != null) {
                ICConfigurationDescription activeCfgDes = des.getConfigurationById(configuration.getId());
                if (activeCfgDes == null) {
                    try {
                        activeCfgDes = des.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID,
                                configuration.getConfigurationData());
                    } catch (WriteAccessException e) {
                    } catch (CoreException e) {
                    }
                }

                if (activeCfgDes != null) {
                    des.setActiveConfiguration(activeCfgDes);
                } else {
                    des = null;
                }
            }
        }

        if (des != null) {
            try {
                BuildSettingsUtil.checkApplyDescription(owner.getProject(), des);
            } catch (CoreException e) {
                Activator.log(e);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo#setDefaultConfiguration(java.lang.String)
     */
    @Override
    public boolean setDefaultConfiguration(String configName) {
        if (configName != null) {
            // Look for the configuration with the same name as the argument
            IConfiguration[] configs = managedProject.getConfigurations();
            for (int index = configs.length - 1; index >= 0; --index) {
                IConfiguration config = configs[index];
                if (configName.equalsIgnoreCase(config.getName())) {
                    setDefaultConfiguration(config);
                    return true;
                }
            }
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo#setValid(boolean)
     */
    @Override
    public void setValid(boolean isValid) {
        // Reset the valid status
        this.isValid = isValid;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo#setRebuildState(boolean)
     */
    @Override
    public void setRebuildState(boolean rebuild) {
        // TODO:  Is the appropriate?  Should the rebuild state be stored in the project file?
        // and in the managed project
        //        if (getDefaultConfiguration() != null) {
        //            getDefaultConfiguration().setRebuildState(rebuild);
        //        }
        // Reset the status here
        rebuildNeeded = rebuild;
    }

    public void setContainerInited(boolean bInited) {
        bIsContainerInited = bInited;
    }

    @Override
    public String toString() {
        // Just print out the name of the project
        return "Managed build information for " + owner.getName(); //$NON-NLS-1$
    }

    /**
     * Sets the owner of the receiver to be the <code>IResource</code> specified
     * in the argument.
     */
    public void updateOwner(IResource resource) {
        // Check to see if the owner is the same as the argument
        if (resource != null) {
            if (!owner.equals(resource)) {
                // Update owner on the managed project
                if (managedProject != null)
                    managedProject.updateOwner(resource);
                // And finally update the cModelElement
                cProject = CoreModel.getDefault().create(resource.getProject());

                // Save everything
                setRebuildState(true);
                // Finally update this managedbuild info's owner
                owner = resource;
            }
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IManagedBuildInfo#getSelectedConfiguration()
     */
    @Override
    public IConfiguration getSelectedConfiguration() {
        return selectedConfig;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.build.managed.IManagedBuildInfo#setSelectedConfiguration(org.eclipse.cdt.core.build.managed.IConfiguration)
     */
    @Override
    public void setSelectedConfiguration(IConfiguration config) {
        selectedConfig = config;
    }

    /*
     * Note:  "Target" routines are only currently applicable when loading a CDT 2.0
     *        or earlier managed build project file (.cdtbuild)
     */

    private String getCWD() {
        String cwd = ""; //$NON-NLS-1$
        //        IEnvironmentVariable cwdvar = ManagedBuildManager.getEnvironmentVariableProvider().getVariable("CWD", //$NON-NLS-1$
        //                getDefaultConfiguration(), true);
        //        if (cwdvar != null) {
        //            cwd = cwdvar.getValue().replace('\\', '/');
        //        }
        return cwd;
    }

    /**
     */
    private List<String> processPath(List<String> list, String path, int context, Object obj) {
        final String EMPTY = ""; //$NON-NLS-1$
        if (path != null) {
            if (context != 0) {
                try {
                    String paths[] = ManagedBuildManager.getBuildMacroProvider().resolveStringListValue(path, EMPTY,
                            " ", context, obj); //$NON-NLS-1$
                    if (paths != null) {
                        for (int i = 0; i < paths.length; i++) {
                            // Check for registered path converter
                            if (obj instanceof OptionContextData) {
                                OptionContextData optionContext = (OptionContextData) obj;
                                IBuildObject buildObject = optionContext.getParent();
                                //                                IOptionPathConverter optionPathConverter = getPathConverter(buildObject);
                                //                                if (null != optionPathConverter) {
                                //                                    IPath platformPath = optionPathConverter.convertToPlatformLocation(paths[i], null,
                                //                                            null);
                                //                                    paths[i] = platformPath.toOSString();
                                //                                }
                            }
                            list.add(checkPath(paths[i]));
                        }
                    }
                } catch (BuildMacroException e) {
                }
            } else {
                list.add(checkPath(path));
            }
        }
        return list;
    }

    //    private IOptionPathConverter getPathConverter(IBuildObject buildObject) {
    //        IOptionPathConverter converter = null;
    //        if (buildObject instanceof ITool) {
    //            ITool tool = (ITool) buildObject;
    //            converter = tool.getOptionPathConverter();
    //        }
    //        return converter;
    //    }

    private String checkPath(String p) {
        final String QUOTE = "\""; //$NON-NLS-1$
        final String EMPTY = ""; //$NON-NLS-1$

        if (p == null)
            return EMPTY;

        if (p.length() > 1 && p.startsWith(QUOTE) && p.endsWith(QUOTE)) {
            p = p.substring(1, p.length() - 1);
        }

        if (".".equals(p)) { //$NON-NLS-1$
            String cwd = getCWD();
            if (cwd.length() > 0) {
                p = cwd;
            }
        }
        if (!(new Path(p)).isAbsolute()) {
            String cwd = getCWD();
            if (cwd.length() > 0) {
                p = cwd + "/" + p; //$NON-NLS-1$
            }
        }
        return p;

    }

    /**
     * Obtain all possible Managed build values
     * 
     * @return IPathEntry[]
     */
    public IPathEntry[] getManagedBuildValues() {
        return null;
        //        List<IPathEntry> entries = new ArrayList<>();
        //        int i = 0;
        //        IPathEntry[] a = getManagedBuildValues(IPathEntry.CDT_INCLUDE);
        //        if (a != null) {
        //            for (i = 0; i < a.length; i++)
        //                entries.add(a[i]);
        //        }
        //        a = getManagedBuildValues(IPathEntry.CDT_LIBRARY);
        //        if (a != null) {
        //            for (i = 0; i < a.length; i++)
        //                entries.add(a[i]);
        //        }
        //        a = getManagedBuildValues(IPathEntry.CDT_MACRO);
        //        if (a != null) {
        //            for (i = 0; i < a.length; i++)
        //                entries.add(a[i]);
        //        }
        //        return entries.toArray(new IPathEntry[entries.size()]);
    }

    /**
     * Obtain all possible Managed build built-ins
     * 
     * @return IPathEntry[]
     */
    public IPathEntry[] getManagedBuildBuiltIns() {
        List<IPathEntry> entries = new ArrayList<>();
        int i = 0;
        IPathEntry[] a = getManagedBuildBuiltIns(IPathEntry.CDT_INCLUDE);
        if (a != null) {
            for (i = 0; i < a.length; i++)
                entries.add(a[i]);
        }
        a = getManagedBuildBuiltIns(IPathEntry.CDT_LIBRARY);
        if (a != null) {
            for (i = 0; i < a.length; i++)
                entries.add(a[i]);
        }
        a = getManagedBuildBuiltIns(IPathEntry.CDT_MACRO);
        if (a != null) {
            for (i = 0; i < a.length; i++)
                entries.add(a[i]);
        }
        return entries.toArray(new IPathEntry[entries.size()]);
    }

    public IPathEntry[] getManagedBuildValues(int entryType) {
        return null;
        //        // obtain option values
        //        List<IPathEntry> entries = getOptionValues(entryType, false);
        //
        //        // for includes, get env variables values; useless for other entry types
        //        if (entryType == IPathEntry.CDT_INCLUDE) {
        //            IEnvironmentVariableProvider env = ManagedBuildManager.getEnvironmentVariableProvider();
        //            entries = addIncludes(entries,
        //                    env.getBuildPaths(getDefaultConfiguration(), IEnvVarBuildPath.BUILDPATH_INCLUDE), Path.EMPTY, 0,
        //                    null);
        //        }
        //        return entries.toArray(new IPathEntry[entries.size()]);
    }

    public IPathEntry[] getManagedBuildBuiltIns(int entryType) {
        List<IPathEntry> entries = getOptionValues(entryType, true);
        return entries.toArray(new IPathEntry[entries.size()]);
    }

    /**
     *
     * @param entryType
     *            - data type to be scanned for
     * @param builtIns
     *            - return either values or built-in's
     * @return list of strings which contains all found values
     */
    private List<IPathEntry> getOptionValues(int entryType, boolean builtIns) {
        List<IPathEntry> entries = new ArrayList<>();
        IConfiguration cfg = getDefaultConfiguration();

        // process config toolchain's options
        entries = readToolsOptions(entryType, entries, builtIns, cfg);

        // code below (obtaining of resource config values)
        // is now commented because resource-related include
        // paths are displayed by UI together with config-
        // related includes, so paths are duplicated in
        // project's "includes" folder.
        //
        // Uncomment following code after UI problem fix.
        /*
        		// process resource configurations
        IResourceConfiguration[] rescfgs = cfg.getResourceConfigurations();
        		if (rescfgs != null) {
        			for (int i=0; i<rescfgs.length; i++) {
        				entries = readToolsOptions(
        							entryType,
        							entries,
        							builtIns,
        							rescfgs[i]);
        			}
        		}
        */
        return entries;
    }

    /**
     *
     * @param entryType
     *            - data type: include | library | symbols
     * @param entries
     *            - list to be affected
     * @param builtIns
     *            - whether get actual values or builtins
     * @param obj
     *            - object to be processed (ResCfg | Cfg)
     */
    private List<IPathEntry> readToolsOptions(int entryType, List<IPathEntry> entries, boolean builtIns,
            IBuildObject obj) {
        List<ITool> t = null;
        IPath resPath = Path.EMPTY;

        // check that entryType is correct
        if (entryType != IPathEntry.CDT_INCLUDE &&
        //TODO: we need to implement the proper CDT_LIBRARY handling
        //calculating the CDT_LIBRARY entries from the managed build
        //options is disabled for now, we need to define a new option type
        //that will represent library paths
        //see bug# 100844
        //			entryType != IPathEntry.CDT_LIBRARY &&
                entryType != IPathEntry.CDT_MACRO) {
            return entries;
        }

        // calculate parameters depending of object type
        if (obj instanceof IResourceConfiguration) {
            resPath = new Path(((IResourceConfiguration) obj).getResourcePath()).removeFirstSegments(1);
            t = ((IResourceConfiguration) obj).getToolsToInvoke();
        } else if (obj instanceof IConfiguration) {
            t = ((IConfiguration) obj).getFilteredTools();
        } else {
            return entries;
        } // wrong object passed
        if (t == null) {
            return entries;
        }

        // process all tools and all their options
        for (ITool i : t) {
            List<IOption> options = i.getOptions();
            for (IOption op : options) {

                // check to see if the option has an applicability calculator
                IOptionApplicability applicabilityCalculator = op.getApplicabilityCalculator();
                if (applicabilityCalculator != null && !applicabilityCalculator.isOptionUsedInCommandLine(obj, i, op))
                    continue;

                try {
                    if (entryType == IPathEntry.CDT_INCLUDE && op.getValueType() == IOption.INCLUDE_PATH) {
                        OptionContextData ocd = new OptionContextData(op, i);
                        addIncludes(entries, builtIns ? op.getBuiltIns() : op.getIncludePaths(), resPath,
                                IBuildMacroProvider.CONTEXT_OPTION, ocd);
                    } else if (entryType == IPathEntry.CDT_LIBRARY && op.getValueType() == IOption.LIBRARIES) {
                        OptionContextData ocd = new OptionContextData(op, i);
                        addLibraries(entries, builtIns ? op.getBuiltIns() : op.getLibraries(), resPath,
                                IBuildMacroProvider.CONTEXT_OPTION, ocd);
                    } else if (entryType == IPathEntry.CDT_MACRO && op.getValueType() == IOption.PREPROCESSOR_SYMBOLS) {
                        OptionContextData ocd = new OptionContextData(op, i);
                        addSymbols(entries, builtIns ? op.getBuiltIns() : op.getDefinedSymbols(), resPath,
                                IBuildMacroProvider.CONTEXT_OPTION, ocd);
                    } else {
                        continue;
                    }
                } catch (BuildException e) {
                }
            }
        }
        return entries;
    }

    protected List<IPathEntry> addIncludes(List<IPathEntry> entries, String[] values, IPath resPath, int context,
            Object obj) {
        return addPaths(entries, values, resPath, context, obj, IPathEntry.CDT_INCLUDE);
    }

    protected List<IPathEntry> addPaths(List<IPathEntry> entries, String[] values, IPath resPath, int context,
            Object obj, int type) {
        if (values != null && values.length > 0) {
            List<String> list = new ArrayList<>();
            for (int k = 0; k < values.length; k++) {
                processPath(list, values[k], context, obj);
            }

            Iterator<String> iter = list.iterator();
            while (iter.hasNext()) {
                IPathEntry entry = null;
                switch (type) {
                case IPathEntry.CDT_INCLUDE:
                    entry = CoreModel.newIncludeEntry(resPath, Path.EMPTY, new Path(iter.next()), true);
                    break;
                case IPathEntry.CDT_LIBRARY:
                    entry = CoreModel.newLibraryEntry(resPath, Path.EMPTY, new Path(iter.next()), null, null, null,
                            true);
                    break;
                }
                if (entry != null && !entries.contains(entry)) {
                    entries.add(entry);
                }
            }
        }
        return entries;
    }

    protected List<IPathEntry> addLibraries(List<IPathEntry> entries, String[] values, IPath resPath, int context,
            Object obj) {
        return addPaths(entries, values, resPath, context, obj, IPathEntry.CDT_LIBRARY);
    }

    protected List<IPathEntry> addSymbols(List<IPathEntry> entries, String[] values, IPath resPath, int context,
            Object obj) {
        if (values == null)
            return entries;
        for (int i = 0; i < values.length; i++) {
            try {
                String res[] = ManagedBuildManager.getBuildMacroProvider().resolveStringListValue(values[i], "", " ", //$NON-NLS-1$//$NON-NLS-2$
                        context, obj);
                if (res != null) {
                    for (int k = 0; k < res.length; k++)
                        createMacroEntry(entries, res[k], resPath);
                }
            } catch (BuildMacroException e) {
            }
        }
        return entries;
    }

    private List<IPathEntry> createMacroEntry(List<IPathEntry> entries, String val, IPath resPath) {
        if (val != null && val.length() != 0) {

            String[] tokens = val.split("="); //$NON-NLS-1$
            String key = tokens[0].trim();
            String value = (tokens.length > 1) ? tokens[1].trim() : ""; //$NON-NLS-1$
            // Make sure the current entries do not contain a duplicate
            boolean add = true;
            Iterator<IPathEntry> entryIter = entries.listIterator();
            while (entryIter.hasNext()) {
                IPathEntry entry = entryIter.next();
                if (entry.getEntryKind() == IPathEntry.CDT_MACRO) {
                    if (((IMacroEntry) entry).getMacroName().equals(key)
                            && ((IMacroEntry) entry).getMacroValue().equals(value)) {
                        add = false;
                        break;
                    }
                }
            }
            if (add) {
                entries.add(CoreModel.newMacroEntry(resPath, key, value));
            }
        }
        return entries;
    }

}
