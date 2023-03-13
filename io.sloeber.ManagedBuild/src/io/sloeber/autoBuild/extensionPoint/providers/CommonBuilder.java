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

import static io.sloeber.autoBuild.core.Messages.*;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ConsoleOutputStream;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.resources.ACBuilder;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IIncrementalProjectBuilder2;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;

import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IMakefileGenerator;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.schema.api.IBuilder;

public class CommonBuilder extends ACBuilder implements IIncrementalProjectBuilder2 {

    public final static String BUILDER_ID = "io.sloeber.autoBuild.integration.CommonBuilder"; //$NON-NLS-1$
    private static final String ERROR_HEADER = "automakefileBuilder error ["; //$NON-NLS-1$
    private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$
    private static final String TRACE_FOOTER = "]: "; //$NON-NLS-1$
    private static final String TRACE_HEADER = "automakefileBuilder trace ["; //$NON-NLS-1$
    public static boolean VERBOSE = false;

    private Set<String> builtRefConfigIds = new HashSet<>();
    private Set<String> scheduledConfigIds = new HashSet<>();

    public CommonBuilder() {
    }

    private static void outputTrace(String resourceName, String message) {
        if (VERBOSE) {
            System.out.println(TRACE_HEADER + resourceName + TRACE_FOOTER + message + NEWLINE);
        }
    }

    private static void outputError(String resourceName, String message) {
        if (VERBOSE) {
            System.err.println(ERROR_HEADER + resourceName + TRACE_FOOTER + message + NEWLINE);
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
    protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map argsMap, IProgressMonitor monitor)
            throws CoreException {
        @SuppressWarnings("unchecked")
        Map<String, String> args = argsMap;
        if (DEBUG_EVENTS)
            printEvent(kind, args);

        builtRefConfigIds.clear();
        scheduledConfigIds.clear();

        IProject project = getProject();

        if (!isCdtProjectCreated(project)) {
            System.err.println("The build is cancelled as the project has not yet been created."); //$NON-NLS-1$
            return project.getReferencedProjects();
        }

        outputTrace(project.getName(), ">>build requested, type = " + kind); //$NON-NLS-1$

        ICProjectDescription cdtProjectDescription = CCorePlugin.getDefault().getProjectDescription(project, false);

        Set<IProject> buildProjects = new HashSet<>();
        if (needAllConfigBuild()) {
            ICConfigurationDescription[] cfgs = cdtProjectDescription.getConfigurations();
            for (ICConfigurationDescription cfg : cfgs) {
                AutoBuildConfigurationDescription autoBuildConfData = AutoBuildConfigurationDescription.getFromConfig(cfg);
                buildProjects.addAll(buildProjectAndReferences(kind, autoBuildConfData, monitor));
            }
        } else {
            ICConfigurationDescription cdtConfDesc = cdtProjectDescription.getActiveConfiguration();
            AutoBuildConfigurationDescription autoData = AutoBuildConfigurationDescription.getFromConfig(cdtConfDesc);
            buildProjects.addAll(buildProjectAndReferences(kind, autoData, monitor));
        }

        outputTrace(project.getName(), "<<done build requested, type = " + kind); //$NON-NLS-1$

        return buildProjects.toArray(new IProject[buildProjects.size()]);
    }

    private Set<IProject> buildProjectAndReferences(int kind, AutoBuildConfigurationDescription autoData,
            IProgressMonitor monitor) throws CoreException {

        IBuilder builder = autoData.getConfiguration().getBuilder();
        ICConfigurationDescription cdtConfigurationDescription = autoData.getCdtConfigurationDescription();
        IProject project = autoData.getProject();
        Set<IProject> refProjects = new HashSet<>();
        refProjects.addAll(Arrays.asList(project.getReferencedProjects()));

        if (!isCdtProjectCreated(project)) {
            return refProjects;
        }

        scheduledConfigIds.add(cdtConfigurationDescription.getId());
        ICConfigurationDescription[] rcfgs = getReferencedConfigurations(cdtConfigurationDescription);
        int numberOfReferencedConfigs = rcfgs.length;

        SubMonitor subMonitor = SubMonitor.convert(monitor, "Building", 1 + numberOfReferencedConfigs); //$NON-NLS-1$

        if (numberOfReferencedConfigs != 0) {
            refProjects.addAll(buildReferencedConfigs(rcfgs, subMonitor.split(numberOfReferencedConfigs)));// = getProjectsSet(cfgs);
        }

        buildProject(kind, autoData, builder, subMonitor.split(1));

        scheduledConfigIds.remove(cdtConfigurationDescription.getId());

        monitor.done();
        return refProjects;
    }

    private Set<IProject> buildReferencedConfigs(ICConfigurationDescription[] cfgs, IProgressMonitor monitor) {
        Set<IProject> buildProjects = new HashSet<>();
        ICConfigurationDescription[] filteredCfgs = filterConfigsToBuild(cfgs);

        if (filteredCfgs.length == 0) {
            return buildProjects;
        }
        monitor.beginTask(CommonBuilder_22, filteredCfgs.length);
        for (ICConfigurationDescription cfg : filteredCfgs) {
            if (builtRefConfigIds.contains(cfg.getId())) {
                continue;
            }

            AutoBuildConfigurationDescription autoBuildConfData = AutoBuildConfigurationDescription.getFromConfig(cfg);
            IProject project = autoBuildConfData.getProject();
            try {

                outputTrace(project.getName(), ">>>>building reference cfg " + cfg.getName()); //$NON-NLS-1$

                buildProjects.addAll(buildProjectAndReferences(INCREMENTAL_BUILD, autoBuildConfData, monitor));

                outputTrace(project.getName(), "<<<<done building reference cfg " + cfg.getName()); //$NON-NLS-1$

            } catch (CoreException e) {
                Activator.log(e);
            } finally {
                builtRefConfigIds.add(cfg.getId());
            }
        }

        return buildProjects;
    }

    private ICConfigurationDescription[] filterConfigsToBuild(ICConfigurationDescription[] cfgs) {
        List<ICConfigurationDescription> cfgList = new ArrayList<>(cfgs.length);
        for (ICConfigurationDescription cfg : cfgs) {
            IProject project = cfg.getProjectDescription().getProject();

            if (scheduledConfigIds.contains(cfg.getId())) {
                Activator.log(new Status(IStatus.WARNING, Activator.getId(),
                        MessageFormat.format(CommonBuilder_circular_dependency, project.getName(), cfg.getName())));
                continue;
            }

            if (!builtRefConfigIds.contains(cfg.getId())) {
                outputTrace(project.getName(), "set: adding cfg " + cfg.getName() + " ( id=" + cfg.getId() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                outputTrace(project.getName(),
                        "filtering regs: adding cfg " + cfg.getName() + " ( id=" + cfg.getId() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

                cfgList.add(cfg);
            } else {
                outputTrace(project.getName(),
                        "filtering regs: excluding cfg " + cfg.getName() + " ( id=" + cfg.getId() + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            }

        }
        return cfgList.toArray(new ICConfigurationDescription[cfgList.size()]);
    }

    @Override
    protected void startupOnInitialize() {
        super.startupOnInitialize();

    }

    /**
     * Returns the configurations referenced by this configuration. Returns an empty
     * array if there are no referenced configurations.
     *
     * @see CoreModelUtil#getReferencedConfigurationDescriptions(ICConfigurationDescription,
     *      boolean)
     * @return an array of IConfiguration objects referenced by this IConfiguration
     */
    private static ICConfigurationDescription[] getReferencedConfigurations(ICConfigurationDescription cfgDes) {
        if (cfgDes != null) {
            return CoreModelUtil.getReferencedConfigurationDescriptions(cfgDes, false);
        }
        return new ICConfigurationDescription[0];
    }

    private void buildProject(int kind, AutoBuildConfigurationDescription autoData, IBuilder builder, IProgressMonitor monitor)
            throws CoreException {
        ICConfigurationDescription cConfDesc = autoData.getCdtConfigurationDescription();
        String configName = cConfDesc.getName();
        IProject project = autoData.getProject();
        IConsole console = CCorePlugin.getDefault().getConsole();
        console.start(project);
        outputTrace(project.getName(), "building cfg " + configName //$NON-NLS-1$
                + " with builder " + builder.getName()); //$NON-NLS-1$

        if (!performPrebuildGeneration(kind, autoData, builder, console, monitor)) {
            return;
        }

        try {
            // Set the current project for markers creation
            setCurrentProject(project);
            if (builder.getBuildRunner().invokeBuild(kind, autoData, builder, this, this, console, monitor)) {
                forgetLastBuiltState();
            }
        } catch (CoreException e) {
            forgetLastBuiltState();
            Activator.log(e);
            throw e;
        }

        checkCancel(monitor);
    }

    /* (non-javadoc)
     * Emits a message to the console indicating that there were no source files to build
     * @param buildType
     * @param status
     * @param configName
     */
    private static String createNoSourceMessage(int buildType, IStatus status, AutoBuildConfigurationDescription autoData) {
        StringBuilder buf = new StringBuilder();
        String[] consoleHeader = new String[3];
        String configName = autoData.getCdtConfigurationDescription().getName();
        String projName = autoData.getProject().getName();
        if (buildType == FULL_BUILD || buildType == INCREMENTAL_BUILD) {
            consoleHeader[0] = ManagedMakeBuider_type_incremental;
        } else {
            consoleHeader[0] = ""; //$NON-NLS-1$
            outputError(projName, "The given build type is not supported in this context"); //$NON-NLS-1$
        }
        consoleHeader[1] = configName;
        consoleHeader[2] = projName;
        buf.append(System.getProperty("line.separator", "\n")); //$NON-NLS-1$	//$NON-NLS-2$
        buf.append(MessageFormat.format(ManagedMakeBuilder_message_console_header, consoleHeader));
        buf.append(System.getProperty("line.separator", "\n")); //$NON-NLS-1$	//$NON-NLS-2$
        buf.append(System.getProperty("line.separator", "\n")); //$NON-NLS-1$	//$NON-NLS-2$
        buf.append(status.getMessage());
        buf.append(System.getProperty("line.separator", "\n")); //$NON-NLS-1$//$NON-NLS-2$
        return buf.toString();
    }

    private static void emitMessage(IConsole console, String msg) throws CoreException {
        try (ConsoleOutputStream consoleOutStream = console.getOutputStream();) {
            // Report a successful clean
            consoleOutStream.write(msg);
            consoleOutStream.write(NEWLINE);
            consoleOutStream.flush();
            consoleOutStream.close();
        } catch (CoreException e) {
            // Throw the exception back to the builder
            throw e;
        } catch (IOException io) { //  Ignore console failures...
            throw new CoreException(new Status(IStatus.ERROR, Activator.getId(), io.getLocalizedMessage(), io));
        }
    }

    /**
     * 
     * @param kind
     * @param autoData
     * @param builder
     * @param generator
     * @param monitor
     * @return true if build can continue
     * @throws CoreException
     */
    private boolean performPrebuildGeneration(int kind, AutoBuildConfigurationDescription autoData, IBuilder builder,
            IConsole console, IProgressMonitor monitor) throws CoreException {
        boolean canContinueBuilding = true;
        IProject project = autoData.getProject();
        //performCleanning(kind, autoData, buildStatus, monitor);
        IMakefileGenerator generator = builder.getBuildFileGenerator();
        if (generator == null) {
            generator = new MakefileGenerator();
        }
        generator.initialize(kind, autoData);

        checkCancel(monitor);
        monitor.subTask(MessageFormat.format(ManagedMakeBuilder_message_update_makefiles, project.getName()));

        MultiStatus result = null;
        if (isCleanBuild(kind)) {
            result = generator.regenerateMakefiles(monitor);
        } else {
            result = generator.generateMakefiles(getDelta(project), monitor);
        }

        if (result.getCode() == IStatus.WARNING || result.getCode() == IStatus.INFO) {
            IStatus[] kids = result.getChildren();
            for (int index = 0; index < kids.length; ++index) {
                // One possibility is that there is nothing to build
                IStatus status = kids[index];
                //					if(messages == null){
                //						messages = new MultiStatus(
                //								Activator.getId(),
                //								IStatus.INFO,
                //								"",
                //								null);
                //
                //					}
                if (status.getCode() == IMakefileGenerator.NO_SOURCE_FOLDERS) {
                    //						performBuild = false;
                    emitMessage(console, createNoSourceMessage(kind, status, autoData));
                    canContinueBuilding = false;
                    //						break;

                } else {
                    // Stick this in the list of stuff to warn the user about

                    //TODO:		messages.add(status);
                }
            }
        } else if (result.getCode() == IStatus.ERROR) {
            StringBuilder buf = new StringBuilder();
            buf.append(CommonBuilder_23).append(NEWLINE);
            String message = result.getMessage();
            if (message != null && message.length() != 0) {
                buf.append(message).append(NEWLINE);
            }

            buf.append(CommonBuilder_24).append(NEWLINE);
            emitMessage(console, buf.toString());
            canContinueBuilding = false;
        }

        checkCancel(monitor);

        //			if(result.getSeverity() != IStatus.OK)
        //				throw new CoreException(result);
        return canContinueBuilding;
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

        IProject curProject = getProject();

        if (!isCdtProjectCreated(curProject))
            return;
        ICProjectDescription cdtProjectDescription = CCorePlugin.getDefault().getProjectDescription(curProject, false);
        ICConfigurationDescription cdtConfigurationDescription = cdtProjectDescription.getActiveConfiguration();
        AutoBuildConfigurationDescription autoData = AutoBuildConfigurationDescription.getFromConfig(cdtConfigurationDescription);
        performExternalClean(autoData, false, monitor);
    }

    private void performExternalClean(AutoBuildConfigurationDescription autoData, boolean separateJob,
            IProgressMonitor monitor) throws CoreException {
        IProject project = autoData.getProject();
        IResourceRuleFactory ruleFactory = ResourcesPlugin.getWorkspace().getRuleFactory();
        final ISchedulingRule rule = ruleFactory.modifyRule(project);
        IBuilder builder = autoData.getConfiguration().getBuilder();
        IConsole console = CCorePlugin.getDefault().getConsole();
        console.start(project);

        if (separateJob) {
            Job backgroundJob = new Job("CDT Common Builder") { //$NON-NLS-1$
                /* (non-Javadoc)
                 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
                 */
                @Override
                protected IStatus run(IProgressMonitor monitor2) {
                    try {
                        ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {

                            @Override
                            public void run(IProgressMonitor monitor3) throws CoreException {
                                // Set the current project for markers creation

                                setCurrentProject(project);
                                builder.getBuildRunner().invokeBuild(CLEAN_BUILD, autoData, builder, CommonBuilder.this,
                                        CommonBuilder.this, console, monitor3);
                            }
                        }, rule, IWorkspace.AVOID_UPDATE, monitor2);
                    } catch (CoreException e) {
                        return e.getStatus();
                    }
                    IStatus returnStatus = Status.OK_STATUS;
                    return returnStatus;
                }

            };

            backgroundJob.setRule(rule);
            backgroundJob.schedule();
        } else {
            // Set the current project for markers creation
            setCurrentProject(project);
            builder.getBuildRunner().invokeBuild(CLEAN_BUILD, autoData, builder, this, this, console, monitor);
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

    /**
     * Only lock the workspace is this is a ManagedBuild, or this project references
     * others.
     */
    @Override
    public ISchedulingRule getRule(int trigger, Map args) {
        IResource WR_rule = ResourcesPlugin.getWorkspace().getRoot();
        if (needAllConfigBuild() || !isCdtProjectCreated(getProject()))
            return WR_rule;

        IProject buildProject = getProject();
        ICProjectDescription cdtProjectDescription = CCorePlugin.getDefault().getProjectDescription(buildProject,
                false);
        ICConfigurationDescription cdtConfigurationDescription = cdtProjectDescription.getActiveConfiguration();
        // Get the builders to run
        //IBuilder builders[] = createBuilders(buildProject, cdtConfigurationDescription, args);
        // Be pessimistic if we referenced other configs
        if (getReferencedConfigurations(cdtConfigurationDescription).length > 0)
            return WR_rule;
        //        // If any builder isManaged => pessimistic
        //        for (IBuilder builder : builders) {
        //            if (builder.isManagedBuildOn())
        //                return WR_rule;
        //        }

        // Success!
        return null;
    }

    private static boolean isCleanBuild(int kind) {
        switch (kind) {
        case IncrementalProjectBuilder.AUTO_BUILD:
        case IncrementalProjectBuilder.INCREMENTAL_BUILD:
            return false;
        case IncrementalProjectBuilder.CLEAN_BUILD:
        case IncrementalProjectBuilder.FULL_BUILD:
            return true;
        }
        return true;
    }
}

//private IProject[] build(int kind, IProject project, IBuilder[] builders, boolean isForeground,
//IProgressMonitor monitor) throws CoreException {
//return build(kind, project, builders, isForeground, monitor, new MyBoolean(false));
//}

//private MultiStatus performMakefileGeneration(AutoBuildConfigurationData autoData, IMakefileGenerator generator,
//BuildStatus buildStatus, IProgressMonitor inMonitor) throws CoreException {
//// Need to report status to the user
//IProject curProject = autoData.getProject();
//IProgressMonitor monitor=inMonitor;
//if (monitor == null) {
//monitor = new NullProgressMonitor();
//}
//
//// Ask the makefile generator to generate any makefiles needed to build delta
//checkCancel(monitor);
//String statusMsg = MessageFormat.format(ManagedMakeBuilder_message_update_makefiles, curProject.getName());
//monitor.subTask(statusMsg);
//
//MultiStatus result;
//if (buildStatus.isRebuild()) {
//result = generator.regenerateMakefiles(monitor);
//} else {
//result = generator.generateMakefiles(getDelta(curProject), monitor);
//}
//
//return result;
//}

//  private MultiStatus createMultiStatus(int severity){
//      return new MultiStatus(
//              Activator.getId(),
//              severity,
//              "", //$NON-NLS-1$
//              null);
//  }

//private static void performPostbuildGeneration(int kind, IMakefileGenerator makeFileGenerator,
//IProgressMonitor monitor) throws CoreException {
//
//boolean isRebuild = true;
//if (isRebuild) {
//makeFileGenerator.regenerateDependencies(false, monitor);
//} else {
//makeFileGenerator.generateDependencies(monitor);
//}
//
//}

//    @Override
//    public void addMarker(IResource file, int lineNumber, String errorDesc, int severity, String errorVar) {
//        super.addMarker(file, lineNumber, errorDesc, severity, errorVar);
//    }
//
//    @Override
//    public void addMarker(ProblemMarkerInfo problemMarkerInfo) {
//        super.addMarker(problemMarkerInfo);
//    }

//private static String concatMessages(List<String> msgs) {
//int size = msgs.size();
//if (size == 0) {
//  return ""; //$NON-NLS-1$
//} else if (size == 1) {
//  return msgs.get(0);
//}
//
//StringBuilder buf = new StringBuilder();
//buf.append(msgs.get(0));
//for (int i = 1; i < size; i++) {
//  buf.append(System.getProperty("line.separator", "\n")); //$NON-NLS-1$ //$NON-NLS-2$
//  buf.append(msgs.get(i));
//}
//return buf.toString();
//}

//private class MyBoolean {
//private boolean value;
//
//public MyBoolean(boolean value) {
//  this.value = value;
//}
//
//public boolean getValue() {
//  return value;
//}
//
//public void setValue(boolean value) {
//  this.value = value;
//}
//
//}

//private boolean performCleanning(int kind, AutoBuildConfigurationData autoData, IProgressMonitor monitor)
//throws CoreException {
//return true;
////        status.setRebuild();
////        return status;
////TOFIX decide what to do with this mess
////                IConfiguration cfg = bInfo.getConfiguration();
////                IProject curProject = bInfo.getProject();
////                //      IBuilder builder = bInfo.getBuilder();
////        
////                boolean makefileRegenerationNeeded = false;
////                //perform necessary cleaning and build type calculation
////                if (cfg.needsFullRebuild()) {
////                    //configuration rebuild state is set to true,
////                    //full rebuild is needed in any case
////                    //clean first, then make a full build
////                    outputTrace(curProject.getName(), "config rebuild state is set to true, making a full rebuild"); //$NON-NLS-1$
////                    clean(bInfo, new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
////                    makefileRegenerationNeeded = true;
////                } else {
////                    makefileRegenerationNeeded = cfg.needsRebuild();
////                    IBuildDescription des = null;
////        
////                    IResourceDelta delta = kind == FULL_BUILD ? null : getDelta(curProject);
////                    if (delta == null)
////                        makefileRegenerationNeeded = true;
////                    if (cfg.needsRebuild() || delta != null) {
////                        //use a build desacription model to calculate the resources to be cleaned
////                        //only in case there are some changes to the project sources or build information
////                        try {
////                            int flags = BuildDescriptionManager.REBUILD | BuildDescriptionManager.DEPFILES
////                                    | BuildDescriptionManager.DEPS;
////                            if (delta != null)
////                                flags |= BuildDescriptionManager.REMOVED;
////        
////                            outputTrace(curProject.getName(), "using a build description.."); //$NON-NLS-1$
////        
////                            des = BuildDescriptionManager.createBuildDescription(cfg, getDelta(curProject), flags);
////        
////                            BuildDescriptionManager.cleanGeneratedRebuildResources(des);
////                        } catch (Throwable e) {
////                            //TODO: log error
////                            outputError(curProject.getName(),
////                                    "error occured while build description calculation: " + e.getLocalizedMessage()); //$NON-NLS-1$
////                            //in case an error occured, make it behave in the old stile:
////                            if (cfg.needsRebuild()) {
////                                //make a full clean if an info needs a rebuild
////                                clean((Map<String, String>) null, new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
////                                makefileRegenerationNeeded = true;
////                            } else if (delta != null && !makefileRegenerationNeeded) {
////                                // Create a delta visitor to detect the build type
////                                ResourceDeltaVisitor visitor = new ResourceDeltaVisitor(cfg,
////                                        bInfo.getBuildInfo().getManagedProject().getConfigurations());
////                                delta.accept(visitor);
////                                if (visitor.shouldBuildFull()) {
////                                    clean((Map<String, String>) null,
////                                            new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
////                                    makefileRegenerationNeeded = true;
////                                }
////                            }
////                        }
////                    }
////                }
////        
////                if (makefileRegenerationNeeded) {
////                    status.setRebuild();
////                }
////                return status;
//}

//private static Set<IProject> getProjectsSet(ICConfigurationDescription[] cfgs) {
//if (cfgs.length == 0)
//  return new HashSet<>(0);
//
//Set<IProject> set = new HashSet<>();
//for (ICConfigurationDescription cfg : cfgs) {
//  set.add(cfg.getProjectDescription().getProject());
//}
//
//return set;
//}