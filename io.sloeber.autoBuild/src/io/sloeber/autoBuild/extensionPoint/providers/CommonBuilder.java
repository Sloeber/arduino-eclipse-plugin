/*******************************************************************************
 * Copyright (c) 2007, 2021 Intel Corporation and others.
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
 * Dmitry Kozlov (CodeSourcery) - Build error highlighting and navigation
 *                                Save build output (bug 294106)
 * Andrew Gvozdev (Quoin Inc)   - Saving build output implemented in different way (bug 306222)
 * Umair Sair (Mentor Graphics) - Project dependencies are not built in the correct order (bug 546407)
 * Umair Sair (Mentor Graphics) - Setting current project for markers creation (bug 545976)
 * Torbjörn Svensson (STMicroelectronics) - bug #571134
 *******************************************************************************/
package io.sloeber.autoBuild.extensionPoint.providers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.resources.ACBuilder;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IIncrementalProjectBuilder2;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;

import io.sloeber.autoBuild.api.AutoBuildBuilderExtension;
import io.sloeber.autoBuild.api.AutoBuildCommon;
import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.autoBuild.schema.api.IBuilder;

public class CommonBuilder extends ACBuilder implements IIncrementalProjectBuilder2 {

    public final static String BUILDER_ID = "io.sloeber.autoBuild.integration.CommonBuilder"; //$NON-NLS-1$

    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$
    private static final String TRACE_FOOTER = "]: "; //$NON-NLS-1$
    private static final String TRACE_HEADER = "automakefileBuilder trace ["; //$NON-NLS-1$
    private static final String PREFIX = "org.eclipse.cdt.make.core"; //$NON-NLS-1$
    static private  final String CONTENTS = PREFIX + ".contents"; //$NON-NLS-1$
    static private  final String CONTENTS_CONFIGURATION_IDS = PREFIX + ".configurationIds"; //$NON-NLS-1$

    //	static final String IDS = PREFIX + ".ids"; //$NON-NLS-1$
    public static final String CONFIGURATION_IDS = PREFIX + ".configurationIds"; //$NON-NLS-1$

    public static boolean VERBOSE = false;

    static private final Set<IProject> projectsThatAreBuilding = new HashSet<>();

    public CommonBuilder() {
    }

    private static void outputTrace(String resourceName, String message) {
        if (VERBOSE) {
            System.out.println(TRACE_HEADER + resourceName + TRACE_FOOTER + message + NEWLINE);
        }
    }

    private static boolean isCdtProjectCreated(IProject project) {
        ICProjectDescription des = CoreModel.getDefault().getProjectDescription(project, false);
        return des != null && !des.isCdtProjectCreating();
    }

    /**
     * @see IncrementalProjectBuilder#build
     */
    @Override
    protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
        if (DEBUG_EVENTS)
            printEvent(kind, args);

        IProject project = getProject();
        if (!isCdtProjectCreated(project)) {
            System.err.println("The build is cancelled as the project has not yet been created."); //$NON-NLS-1$
            return null;
        }
        outputTrace(project.getName(), ">>build requested, type = " + kind); //$NON-NLS-1$

        //Mark the project as building to avoid it to be build when being build
        synchronized (projectsThatAreBuilding) {
            if (projectsThatAreBuilding.contains(project)) {
                // this project is already building so do not try to build it again.
                // this caters for A->depends on B->depends on A
                return null;
            }
            projectsThatAreBuilding.add(project);
        }

        try {
        	project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
            invokeBuild(project,kind, args, monitor);
        } finally {
            synchronized (projectsThatAreBuilding) {
                projectsThatAreBuilding.remove(project);
            }
        }

        outputTrace(project.getName(), "<<done build requested, type = " + kind); //$NON-NLS-1$
        return null;
    }

    /**
     * build this project after building all the projects this one depends on
     * parses the args to find configurations that are requested
     *
     * @param kind
     * @param args
     * @param monitor
     * @throws CoreException
     */
    private void invokeBuild(IProject project,int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
    	project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
        Set<AutoBuildConfigurationDescription> cfgsToBuild = getConfigsToBuild(project, kind, args);

        //For the configurations to build: get the cdt referenced configurations
        Set<ICConfigurationDescription> referencedCfgs = new HashSet<>();
        for (AutoBuildConfigurationDescription curConfig : cfgsToBuild) {
            referencedCfgs.addAll(Arrays.asList(CoreModelUtil
                    .getReferencedConfigurationDescriptions(curConfig.getCdtConfigurationDescription(), false)));
        }

        //build the cdt referenced configurations
        Set<IProject> cdtReferencedProjects = new HashSet<>();
        for (ICConfigurationDescription curConfig : referencedCfgs) {
            IProject curProject = curConfig.getProjectDescription().getProject();
            if (cdtReferencedProjects.contains(curProject)) {
                continue;
            }
            Set<ICConfigurationDescription> toBuildCfgs = new HashSet<>();
            for (ICConfigurationDescription searchConfig : referencedCfgs) {
                IProject searchProject = searchConfig.getProjectDescription().getProject();
                if (curProject == searchProject) {
                    toBuildCfgs.add(searchConfig);
                }
                cdtReferencedProjects.add(curProject);
                //Ask eclipse to build these configs of this project
                buildProjectConfigs(curProject, toBuildCfgs, kind, monitor);
            }
        }

        //Build the projects this project references in the eclipse way (without configuration)
        //that have not already been handled before
        Set<IProject> eclipseReferencedProjects = new HashSet<>();
        eclipseReferencedProjects.addAll(Arrays.asList(project.getReferencedProjects()));
        eclipseReferencedProjects.removeAll(cdtReferencedProjects);
        for (IProject curProject : eclipseReferencedProjects) {
            //build the referenced project
            curProject.build(kind, monitor);
        }

        //now that all referenced projects and configs are build.
        //build the configs requested to build
        String BuildRunnerID = null;
        String targetName = null;
        if (args != null) {
        	BuildRunnerID = args.get(AutoBuildProject.ARGS_BUILDER_KEY);
        	targetName = args.get(AutoBuildProject.ARGS_TARGET_KEY);
        }

        for (AutoBuildConfigurationDescription curAutoConfig : cfgsToBuild) {
        	IBuilder builder = curAutoConfig.getBuilder(BuildRunnerID);
        	curAutoConfig.forceFullBuildIfNeeded(monitor);
            buildProjectConfiguration(kind,targetName, builder, curAutoConfig, monitor);
        }

    }



	private static void buildProjectConfigs(IProject project, Set<ICConfigurationDescription> toBuildCfgs, int kind,
            IProgressMonitor localmonitor) {
        Map<String, String> cfgIdArgs = createBuildArgs(toBuildCfgs);
        try {
            ICommand[] commands = project.getDescription().getBuildSpec();
            for (ICommand command : commands) {
                Map<String, String> args = command.getArguments();
                if (args == null) {
                    args = new HashMap<>(cfgIdArgs);
                } else {
                    args.putAll(cfgIdArgs);
                }

                if (localmonitor.isCanceled()) {
                    throw new OperationCanceledException();
                }

                project.build(kind, command.getBuilderName(), args, localmonitor);

            }
        } catch (CoreException e) {
            e.printStackTrace();
        }

    }

    private static Map<String, String> createBuildArgs(Set<ICConfigurationDescription> cfgs) {
        Map<String, String> map = new HashMap<>();
        map.put(CONFIGURATION_IDS, AutoBuildProject.encode(getCfgIds(cfgs)));
        map.put(CONTENTS, CONTENTS_CONFIGURATION_IDS);
        return map;
    }

    private static Set<String> getCfgIds(Set<ICConfigurationDescription> cfgs) {
        Set<String> ids = new HashSet<>();
        for (ICConfigurationDescription cfg : cfgs) {
            ids.add(cfg.getId());
        }
        return ids;
    }

    @Override
    protected void startupOnInitialize() {
        super.startupOnInitialize();

    }

    /**
     * Get the configurations that are requested to build and that should be build
     * This takes into account the arguments that can reference multiple
     * configurations and the
     * configuration settings in regards to the build type
     *
     * @param cdtProjectDescription
     *            the project configuration description of the project to build
     * @param args
     *            the args passed to the build command
     * @param kind
     *            the kind of build as provided by the build method
     * @return a list of configurations to build. In case no configurations to build
     *         an empty list is returned.
     *         This method does not return null.
     */
    private static Set<AutoBuildConfigurationDescription> getConfigsToBuild(IProject project, int kind,
            Map<String, String> args) {
        // get the configurations that need to be build
        Set<AutoBuildConfigurationDescription> cfgToBuild = new HashSet<>();
        if ((args != null) && (args.size() > 0)) {
            ICProjectDescription cdtProjectDescription = CCorePlugin.getDefault().getProjectDescription(project, false);
            String configIDs = args.get(CONTENTS_CONFIGURATION_IDS);
            if (configIDs != null) {
                for (String curConfigId : AutoBuildProject.decodeList(configIDs)) {
                    // JABA: Should I log that a config is not found?
                    ICConfigurationDescription config = cdtProjectDescription.getConfigurationById(curConfigId);
                    cfgToBuild.add(
                            (AutoBuildConfigurationDescription) IAutoBuildConfigurationDescription.getConfig(config));
                }
            }
            String configs = args.get(AutoBuildProject.ARGS_CONFIGS_KEY);
            if (configs != null) {
                for (String curConfigName : AutoBuildProject.decodeList(configs)) {
                    // JABA: Should I log that a config is not found?
                    ICConfigurationDescription config = cdtProjectDescription.getConfigurationByName(curConfigName);
                    cfgToBuild.add(
                            (AutoBuildConfigurationDescription) IAutoBuildConfigurationDescription.getConfig(config));
                }
            }
        }

        //remove null configurations that may have been added
        cfgToBuild.remove(null);

        //check whether all the configs to build should actually be build for
        // this kind of build
        String projectName = project.getName();
        Set<AutoBuildConfigurationDescription> cfgToIgnore = new HashSet<>();
        for (AutoBuildConfigurationDescription curAutoConf : cfgToBuild) {
            switch (kind) {
            case INCREMENTAL_BUILD:
                //TOFIX JABA this does no longer make sense. As you can specify the buider and
                // it is the builder that decides on the incremental
                if (!curAutoConf.isIncrementalBuildEnabled()) {
                    outputTrace(projectName,
                            curAutoConf.getName() + " >>The config is setup to ignore incremental builds "); //$NON-NLS-1$
                    cfgToIgnore.add(curAutoConf);
                }
                break;
			default:
            case AUTO_BUILD:
                if (!curAutoConf.isAutoBuildEnabled()) {
                    outputTrace(projectName, curAutoConf.getName() + ">>The config is setup to ignore auto builds "); //$NON-NLS-1$
                    cfgToIgnore.add(curAutoConf);
                }
                break;
            }
        }
        cfgToBuild.removeAll(cfgToIgnore);

        //if no configs found add active config
        if (cfgToBuild.size() == 0) {
            cfgToBuild.add((AutoBuildConfigurationDescription) IAutoBuildConfigurationDescription
                    .getActiveConfig(project, false));
        }
        //remove null configurations that may have been added
        cfgToBuild.remove(null);
        return cfgToBuild;

    }

    private void buildProjectConfiguration(int kind, String targetName, IBuilder builder,
            AutoBuildConfigurationDescription autoData, IProgressMonitor monitor) throws CoreException {
        ICConfigurationDescription cConfDesc = autoData.getCdtConfigurationDescription();
        String configName = cConfDesc.getName();
        IProject project = autoData.getProject();

        IConsole console = CCorePlugin.getDefault().getConsole();
        console.start(project);
        outputTrace(project.getName(), "building cfg " + configName //$NON-NLS-1$
                + " with builder " + builder.getName()); //$NON-NLS-1$

        try {
            // Set the current project for markers creation
            setCurrentProject(project);
            AutoBuildCommon.createFolder(autoData.getBuildFolder());
            AutoBuildBuilderExtension builderExt=autoData.getProjectType().getBuilderExtension();
            if(builderExt.invokeBuild(builder,kind,targetName, autoData, this,  console, monitor)) {
                forgetLastBuiltState();
            }
        } catch (CoreException e) {
            forgetLastBuiltState();
            Activator.log(e);
            throw e;
        }

        checkCancel(monitor);
    }

    @Override
    protected final void clean(IProgressMonitor monitor) throws CoreException {
        throw new IllegalStateException(
                "Unexpected/incorrect call to old clean method. Client code must call clean(Map,IProgressMonitor)"); //$NON-NLS-1$
    }

    @Override
    public void clean(Map<String, String> args, IProgressMonitor monitor) throws CoreException {
        if (DEBUG_EVENTS)
            printEvent(IncrementalProjectBuilder.CLEAN_BUILD, args);

        IProject project = getProject();
        if (!isCdtProjectCreated(project)) {
            System.err.println("The clean is cancelled as the project has not yet been created."); //$NON-NLS-1$
            return ;
        }

        Set<AutoBuildConfigurationDescription> cfgsToBuild = getConfigsToBuild(project, IncrementalProjectBuilder.CLEAN_BUILD, args);
        String BuildRunnerID = null;
        if (args != null) {
        	BuildRunnerID = args.get(AutoBuildProject.ARGS_BUILDER_KEY);
        }

        for (AutoBuildConfigurationDescription curAutoConfig : cfgsToBuild) {
        	IBuilder builder = curAutoConfig.getBuilder(BuildRunnerID);
            IConsole console = CCorePlugin.getDefault().getConsole();
            console.start(project);
            AutoBuildBuilderExtension builderExt=curAutoConfig.getProjectType().getBuilderExtension();
            builderExt.invokeClean(builder,IncrementalProjectBuilder.CLEAN_BUILD, curAutoConfig, this,  console,
                    monitor);
        }
    }


    //
    /**
     * Check whether the build has been canceled.
     */
    private static void checkCancel(IProgressMonitor monitor) {
        if (monitor != null && monitor.isCanceled())
            throw new OperationCanceledException();
    }

//    /**
//     * Only lock the workspace is this is a ManagedBuild, or this project references
//     * others.
//     */
//    @Override
//    public ISchedulingRule getRule(int trigger, Map args) {
//        IResource WR_rule = ResourcesPlugin.getWorkspace().getRoot();
//        if (needAllConfigBuild() || !isCdtProjectCreated(getProject()))
//            return WR_rule;
//
//        IProject buildProject = getProject();
//        ICProjectDescription cdtProjectDescription = CCorePlugin.getDefault().getProjectDescription(buildProject,
//                false);
//        ICConfigurationDescription cdtConfigurationDescription = cdtProjectDescription.getActiveConfiguration();
//        // Get the builders to run
//        // IBuilder builders[] = createBuilders(buildProject,
//        // cdtConfigurationDescription, args);
//        // Be pessimistic if we referenced other configs
//        if (CoreModelUtil.getReferencedConfigurationDescriptions(cdtConfigurationDescription, false).length > 0)
//            return WR_rule;
//        // // If any builder isManaged => pessimistic
//        // for (IBuilder builder : builders) {
//        // if (builder.isManagedBuildOn())
//        // return WR_rule;
//        // }
//
//        // Success!
//        return null;
//    }

}
