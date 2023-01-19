/*******************************************************************************
 *  Copyright (c) 2002, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *  IBM Rational Software - Initial API and implementation
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import static io.sloeber.autoBuild.Internal.ManagebBuildCommon.*;
import static io.sloeber.autoBuild.core.Messages.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ConsoleOutputStream;
import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.cdt.core.resources.ACBuilder;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.core.resources.RefreshScopeManager;
import org.eclipse.cdt.internal.core.BuildRunnerHelper;
import org.eclipse.cdt.internal.errorparsers.FixitManager;
//import org.eclipse.cdt.managedbuilder.buildmodel.BuildDescriptionManager;
//import org.eclipse.cdt.managedbuilder.buildmodel.IBuildDescription;
//import org.eclipse.cdt.managedbuilder.buildmodel.IBuildIOType;
//import org.eclipse.cdt.managedbuilder.buildmodel.IBuildResource;
//import org.eclipse.cdt.managedbuilder.buildmodel.IBuildStep;
//import org.eclipse.cdt.managedbuilder.core.IBuilder;
//import org.eclipse.cdt.managedbuilder.core.IConfiguration;
//import org.eclipse.cdt.managedbuilder.core.IManagedBuildInfo;
//import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
//import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
//import org.eclipse.cdt.managedbuilder.internal.buildmodel.DescriptionBuilder;
//import org.eclipse.cdt.managedbuilder.internal.buildmodel.IBuildModelBuilder;
//import org.eclipse.cdt.managedbuilder.internal.buildmodel.ParallelBuilder;
//import org.eclipse.cdt.managedbuilder.internal.buildmodel.StepBuilder;
//import org.eclipse.cdt.managedbuilder.macros.BuildMacroException;
//import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
//import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator;
import org.eclipse.cdt.utils.EFSExtensionManager;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;

import io.sloeber.autoBuild.api.BuildMacroException;
import io.sloeber.autoBuild.api.IBuildMacroProvider;
import io.sloeber.autoBuild.api.IManagedBuildInfo;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IMakefileGenerator;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.internal.Configuration;

/**
 * This is the incremental builder associated with a managed build project. It
 * dynamically
 * decides the makefile generator it wants to use for a specific target.
 *
 * @since 1.2
 */
public class GeneratedMakefileBuilder extends ACBuilder {
    /**
     * @since 1.2
     */
    public class ResourceDeltaVisitor implements IResourceDeltaVisitor {
        private String buildGoalName;
        private IManagedBuildInfo buildInfo;
        private boolean incrBuildNeeded = false;
        private boolean fullBuildNeeded = false;
        private List<String> reservedNames;

        public ResourceDeltaVisitor(IManagedBuildInfo info) {
            buildInfo = info;
            String ext = buildInfo.getBuildArtifactExtension();
            //try to resolve build macros in the build artifact extension
            ext = resolveValueToMakefileFormat(ext, "", //$NON-NLS-1$
                    " ", //$NON-NLS-1$
                    IBuildMacroProvider.CONTEXT_CONFIGURATION, info.getDefaultConfiguration());

            String name = buildInfo.getBuildArtifactName();
            //try to resolve build macros in the build artifact name
            String resolved = resolveValueToMakefileFormat(name, "", //$NON-NLS-1$
                    " ", //$NON-NLS-1$
                    IBuildMacroProvider.CONTEXT_CONFIGURATION, info.getDefaultConfiguration());
            if ((resolved = resolved.trim()).length() > 0)
                name = resolved;

            buildGoalName = name;
            reservedNames = Arrays.asList(new String[] { ".cdtbuild", ".cdtproject", ".project" }); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }

        private boolean isGeneratedResource(IResource resource) {
            // Is this a generated directory ...
            IPath path = resource.getProjectRelativePath();
            Set<String> configNames = buildInfo.getConfigurationNames();
            for (String name: configNames) {
                IPath root = new Path(name);
                // It is if it is a root of the resource pathname
                if (root.isPrefixOf(path))
                    return true;
            }
            return false;
        }

        private boolean isProjectFile(IResource resource) {
            return reservedNames.contains(resource.getName());
        }

        public boolean shouldBuildIncr() {
            return incrBuildNeeded;
        }

        public boolean shouldBuildFull() {
            return fullBuildNeeded;
        }

        @Override
        public boolean visit(IResourceDelta delta) throws CoreException {
            IResource resource = delta.getResource();
            // If the project has changed, then a build is needed and we can stop
            if (resource != null && resource.getProject() == getProject()) {
                switch (resource.getType()) {
                case IResource.FILE:
                    String name = resource.getName();
                    if ((!name.equals(buildGoalName) &&
                    // TODO:  Also need to check for secondary outputs
                            (resource.isDerived() || (isProjectFile(resource)) || (isGeneratedResource(resource))))) {
                        // The resource that changed has attributes which make it uninteresting,
                        // so don't do anything
                    } else {
                        //  TODO:  Should we do extra checks here to determine if a build is really needed,
                        //         or do you just do exclusion checks like above?
                        //         We could check for:
                        //         o  The build goal name
                        //         o  A secondary output
                        //         o  An input file to a tool:
                        //            o  Has an extension of a source file used by a tool
                        //            o  Has an extension of a header file used by a tool
                        //            o  Has the name of an input file specified in an InputType via:
                        //               o  An Option
                        //               o  An AdditionalInput
                        //
                        //if (resourceName.equals(buildGoalName) ||
                        //	(buildInfo.buildsFileType(ext) || buildInfo.isHeaderFile(ext))) {

                        // We need to do an incremental build, at least
                        incrBuildNeeded = true;
                        if (delta.getKind() == IResourceDelta.REMOVED) {
                            // If a meaningful resource was removed, then force a full build
                            // This is required because an incremental build will trigger make to
                            // do nothing for a missing source, since the state after the file
                            // removal is uptodate, as far as make is concerned
                            // A full build will clean, and ultimately trigger a relink without
                            // the object generated from the deleted source, which is what we want
                            fullBuildNeeded = true;
                            // There is no point in checking anything else since we have
                            // decided to do a full build anyway
                            break;
                        }

                        //}
                    }

                    return false;
                }
            }
            return true;
        }
    }

    private static class OtherConfigVerifier implements IResourceDeltaVisitor {
        IConfiguration config;
        IConfiguration configs[];
        Set<Configuration> otherConfigs;
        int resourceChangeState;

        private static final IPath[] ignoreList = { new Path(".cdtproject"), //$NON-NLS-1$
                new Path(".cproject"), //$NON-NLS-1$
                new Path(".cdtbuild"), //$NON-NLS-1$
                new Path(".settings"), //$NON-NLS-1$
        };

        OtherConfigVerifier(IConfiguration cfg) {
            config = cfg;
            configs = cfg.getManagedProject().getConfigurations();
            otherConfigs = new HashSet<Configuration>();
            int counter = 0;
            for (IConfiguration curConfig : configs) {
                if (curConfig != config)
                    otherConfigs.add((Configuration) curConfig);
            }
        }

        @Override
        public boolean visit(IResourceDelta delta) throws CoreException {

            IResource rc = delta.getResource();
            if (rc.getType() == IResource.FILE) {
                if (isResourceValuable(rc))
                    resourceChangeState |= delta.getKind();
                return false;
            }
            return !isGeneratedForConfig(rc, config) && isResourceValuable(rc);
        }

        public void updateOtherConfigs(IResourceDelta delta) {
            if (delta == null)
                resourceChangeState = ~0;
            else {
                try {
                    delta.accept(this);
                } catch (CoreException e) {
                    resourceChangeState = ~0;
                }
            }

            setResourceChangeStateForOtherConfigs();
        }

        private void setResourceChangeStateForOtherConfigs() {
//            for (IConfiguration curConfig : otherConfigs) {
//                curConfig.addResourceChangeState(resourceChangeState);
//            }
        }

        private boolean isGeneratedForConfig(IResource resource, IConfiguration cfg) {
            // Is this a generated directory ...
            IPath path = resource.getProjectRelativePath();
            IPath root = new Path(cfg.getName());
            // It is if it is a root of the resource pathname
            if (root.isPrefixOf(path))
                return true;
            return false;
        }

        private boolean isResourceValuable(IResource rc) {
            IPath path = rc.getProjectRelativePath();
            for (int i = 0; i < ignoreList.length; i++) {
                if (ignoreList[i].equals(path))
                    return false;
            }
            return true;
        }
    }

    // String constants
    //private static final String BUILD_ERROR = "ManagedMakeBuilder_message_error"; //$NON-NLS-1$
    //private static final String BUILD_FINISHED = "ManagedMakeBuilder_message_finished"; //$NON-NLS-1$
    //private static final String CONSOLE_HEADER = "ManagedMakeBuilder_message_console_header"; //$NON-NLS-1$
    private static final String ERROR_HEADER = "GeneratedmakefileBuilder error ["; //$NON-NLS-1$
    //private static final String MAKE = "ManagedMakeBuilder.message.make"; //$NON-NLS-1$
    //    private static final String MARKERS = "ManagedMakeBuilder_message_creating_markers"; //$NON-NLS-1$
    private static final String NEWLINE = System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$
    //    private static final String NOTHING_BUILT = "ManagedMakeBuilder_message_no_build"; //$NON-NLS-1$
    //    private static final String REFRESH = "ManagedMakeBuilder.message.updating"; //$NON-NLS-1$
    //private static final String REFRESH_ERROR = "ManagedMakeBuilder_message_error_refresh"; //$NON-NLS-1$
    private static final String TRACE_FOOTER = "]: "; //$NON-NLS-1$
    private static final String TRACE_HEADER = "GeneratedmakefileBuilder trace ["; //$NON-NLS-1$
    //private static final String TYPE_CLEAN = "ManagedMakeBuilder.type.clean"; //$NON-NLS-1$
    //private static final String TYPE_INC = "ManagedMakeBuider.type.incremental"; //$NON-NLS-1$
    private static final String WARNING_UNSUPPORTED_CONFIGURATION = "ManagedMakeBuilder.warning.unsupported.configuration"; //$NON-NLS-1$
    //private static final String BUILD_CANCELLED = "ManagedMakeBuilder_message_cancelled"; //$NON-NLS-1$
    //private static final String BUILD_FINISHED_WITH_ERRS = "ManagedMakeBuilder_message_finished_with_errs"; //$NON-NLS-1$
    //    private static final String BUILD_FAILED_ERR = "ManagedMakeBuilder_message_internal_builder_error"; //$NON-NLS-1$
    //private static final String BUILD_STOPPED_ERR = "ManagedMakeBuilder_message_stopped_error"; //$NON-NLS-1$
    //    private static final String INTERNAL_BUILDER_HEADER_NOTE = "ManagedMakeBuilder_message_internal_builder_header_note"; //$NON-NLS-1$
    //private static final String TYPE_REBUILD = "ManagedMakeBuider_type_rebuild"; //$NON-NLS-1$
    //private static final String INTERNAL_BUILDER = "ManagedMakeBuilder_message_internal_builder"; //$NON-NLS-1$

    private static final int PROGRESS_MONITOR_SCALE = 100;
    private static final int TICKS_STREAM_PROGRESS_MONITOR = 1 * PROGRESS_MONITOR_SCALE;
    private static final int TICKS_DELETE_MARKERS = 1 * PROGRESS_MONITOR_SCALE;

    public static boolean VERBOSE = false;

    // Local variables
    protected Vector<IStatus> generationProblems;
    protected IProject[] referencedProjects;
    protected List<IResource> resourcesToBuild;
    private IConsole console;

    public static void outputTrace(String resourceName, String message) {
        if (VERBOSE) {
            System.out.println(TRACE_HEADER + resourceName + TRACE_FOOTER + message + NEWLINE);
        }
    }

    private static void outputError(String resourceName, String message) {
        if (VERBOSE) {
            System.err.println(ERROR_HEADER + resourceName + TRACE_FOOTER + message + NEWLINE);
        }
    }

    /**
     * Zero-argument constructor needed to fulfill the contract of an
     * incremental builder.
     */
    public GeneratedMakefileBuilder() {
    }

    private void addBuilderMarkers(ErrorParserManager epm) {
        IWorkspaceRoot root = CCorePlugin.getWorkspace().getRoot();
        Iterator<IStatus> iter = getGenerationProblems().iterator();
        while (iter.hasNext()) {
            IStatus stat = iter.next();
            IResource location = root.findMember(stat.getMessage());
            if (stat.getCode() == IMakefileGenerator.SPACES_IN_PATH) {
                epm.generateMarker(location, -1, MakefileGenerator_error_spaces, //$NON-NLS-1$
                        IMarkerGenerator.SEVERITY_WARNING, null);
            }
        }
    }

    /* (non-javadoc)
     * Emits a message to the console indicating that there were no source files to build
     * @param buildType
     * @param status
     * @param configName
     */
    private void emitNoSourceMessage(int buildType, IStatus status, String configName) throws CoreException {
        try {
            StringBuilder buf = new StringBuilder();
            IConsole console = CCorePlugin.getDefault().getConsole();
            console.start(getProject());
            ConsoleOutputStream consoleOutStream = console.getOutputStream();
            // Report a successful clean
            String[] consoleHeader = new String[3];
            if (buildType == FULL_BUILD || buildType == INCREMENTAL_BUILD) {
                consoleHeader[0] = ManagedMakeBuider_type_incremental;
            } else {
                consoleHeader[0] = ""; //$NON-NLS-1$
                outputError(getProject().getName(), "The given build type is not supported in this context"); //$NON-NLS-1$
            }
            consoleHeader[1] = configName;
            consoleHeader[2] = getProject().getName();
            buf.append(NEWLINE);
            buf.append(MessageFormat.format(ManagedMakeBuilder_message_console_header, consoleHeader)).append(NEWLINE);
            buf.append(NEWLINE);
            buf.append(status.getMessage()).append(NEWLINE);
            consoleOutStream.write(buf.toString().getBytes());
            consoleOutStream.flush();
            consoleOutStream.close();
        } catch (CoreException e) {
            // Throw the exception back to the builder
            throw e;
        } catch (IOException io) { //  Ignore console failures...
        }
    }

    /**
     *
     * This method has been created so that subclasses can override how the builder
     * obtains its
     * build info. The default implementation retrieves the info from the build
     * manager.
     *
     * @return An IManagedBuildInfo object representing the build info.
     */
    protected IManagedBuildInfo getBuildInfo() {
        return ManagedBuildManager.getBuildInfo(getProject());
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.internal.events.InternalBuilder#build(int, java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected IProject[] build(int kind, @SuppressWarnings("rawtypes") Map argsMap, IProgressMonitor monitor)
            throws CoreException {
        @SuppressWarnings("unchecked")
        Map<String, String> args = argsMap;
        if (DEBUG_EVENTS)
            printEvent(kind, args);

        // We should always tell the build system what projects we reference
        referencedProjects = getProject().getReferencedProjects();

        // Get the build information
        IManagedBuildInfo info = getBuildInfo();
        if (info == null) {
            outputError(getProject().getName(), "Build information was not found"); //$NON-NLS-1$
            return referencedProjects;
        }
        if (!info.isValid()) {
            outputError(getProject().getName(), "Build information is not valid"); //$NON-NLS-1$
            return referencedProjects;
        }

        IConfiguration[] cfgs = null;
        if (needAllConfigBuild()) {
            cfgs = info.getManagedProject().getConfigurations();
        } else {
            cfgs = new IConfiguration[] { info.getDefaultConfiguration() };
        }

        for (IConfiguration cfg : cfgs) {
            updateOtherConfigs(cfg, kind);

            // Create a makefile generator for the build
            IMakefileGenerator generator = ManagedBuildManager.getBuildfileGenerator(info.getDefaultConfiguration());
            generator.initialize(getProject(), info, monitor);

            //perform necessary cleaning and build type calculation
            boolean needsFullRebuild=true;//TOFIX JAB cfg.needsFullRebuild();
            if (needsFullRebuild) {
                //configuration rebuild state is set to true,
                //full rebuild is needed in any case
                //clean first, then make a full build
                outputTrace(getProject().getName(), "config rebuild state is set to true, making a full rebuild"); //$NON-NLS-1$
                clean(new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
                fullBuild(info, generator, monitor);
            } else {
                //                boolean fullBuildNeeded = info.needsRebuild();
                //                IBuildDescription des = null;
                //
                //                IResourceDelta delta = kind == FULL_BUILD ? null : getDelta(getProject());
                //                if (delta == null)
                //                    fullBuildNeeded = true;
                //                if (cfg.needsRebuild() || delta != null) {
                //                    //use a build description model to calculate the resources to be cleaned
                //                    //only in case there are some changes to the project sources or build information
                //                    try {
                //                        int flags = BuildDescriptionManager.REBUILD | BuildDescriptionManager.DEPFILES
                //                                | BuildDescriptionManager.DEPS;
                //                        if (delta != null)
                //                            flags |= BuildDescriptionManager.REMOVED;
                //
                //                        outputTrace(getProject().getName(), "using a build description.."); //$NON-NLS-1$
                //
                //                        des = BuildDescriptionManager.createBuildDescription(info.getDefaultConfiguration(),
                //                                getDelta(getProject()), flags);
                //
                //                        BuildDescriptionManager.cleanGeneratedRebuildResources(des);
                //                    } catch (Throwable e) {
                //                        //TODO: log error
                //                        outputError(getProject().getName(),
                //                                "error occured while build description calculation: " + e.getLocalizedMessage()); //$NON-NLS-1$
                //                        //in case an error occured, make it behave in the old stile:
                //                        if (info.needsRebuild()) {
                //                            //make a full clean if an info needs a rebuild
                //                            clean(new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
                //                            fullBuildNeeded = true;
                //                        } else if (delta != null && !fullBuildNeeded) {
                //                            // Create a delta visitor to detect the build type
                //                            ResourceDeltaVisitor visitor = new ResourceDeltaVisitor(info);
                //                            delta.accept(visitor);
                //                            if (visitor.shouldBuildFull()) {
                //                                clean(new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
                //                                fullBuildNeeded = true;
                //                            }
                //                        }
                //                    }
            }

            boolean fullBuildNeeded = true;
            if (fullBuildNeeded) {
                outputTrace(getProject().getName(), "performing a full build"); //$NON-NLS-1$
                fullBuild(info, generator, monitor);
            } else {
                outputTrace(getProject().getName(), "performing an incremental build"); //$NON-NLS-1$
                incrementalBuild(getDelta(getProject()), info, generator, monitor);
            }
        }
        /*
        // So let's figure out why we got called
        if (kind == FULL_BUILD) {
        	outputTrace(getProject().getName(), "Full build needed/requested");	//$NON-NLS-1$
        	fullBuild(info, generator, monitor);
        }
        else if (kind == AUTO_BUILD && info.needsRebuild()) {
        	outputTrace(getProject().getName(), "Autobuild requested, full build needed");	//$NON-NLS-1$
        	fullBuild(info, generator, monitor);
        }
        else {
        	// Create a delta visitor to make sure we should be rebuilding
        	ResourceDeltaVisitor visitor = new ResourceDeltaVisitor(info);
        	IResourceDelta delta = getDelta(getProject());
        	if (delta == null) {
        		outputTrace(getProject().getName(), "Incremental build requested, full build needed");	//$NON-NLS-1$
        		fullBuild(info, generator, monitor);
        	}
        	else {
        		delta.accept(visitor);
        		if (visitor.shouldBuildFull()) {
        			outputTrace(getProject().getName(), "Incremental build requested, full build needed");	//$NON-NLS-1$
        			fullBuild(info, generator, monitor);
        		} else if (visitor.shouldBuildIncr()) {
        			outputTrace(getProject().getName(), "Incremental build requested");	//$NON-NLS-1$
        			incrementalBuild(delta, info, generator, monitor);
        		}
        		else if (referencedProjects != null) {
        			//  Also check to see is any of the dependent projects changed
        			for (int i=0; i<referencedProjects.length; i++) {
        				IProject ref = referencedProjects[i];
        				IResourceDelta refDelta = getDelta(ref);
        				if (refDelta == null) {
        					outputTrace(getProject().getName(), "Incremental build because of changed referenced project");	//$NON-NLS-1$
        					incrementalBuild(delta, info, generator, monitor);
        					//  Should only build this project once, for this delta
        					break;
        				} else {
        					int refKind = refDelta.getKind();
        					if (refKind != IResourceDelta.NO_CHANGE) {
        						int refFlags = refDelta.getFlags();
        						if (!(refKind == IResourceDelta.CHANGED &&
        							  refFlags == IResourceDelta.OPEN)) {
        							outputTrace(getProject().getName(), "Incremental build because of changed referenced project");	//$NON-NLS-1$
        							incrementalBuild(delta, info, generator, monitor);
        							//  Should only build this project once, for this delta
        							break;
        						}
        					}
        				}
        			}
        		}
        	}
        }
         */
        // Scrub the build info the project
        info.setRebuildState(false);
        // Ask build mechanism to compute deltas for project dependencies next time
        return referencedProjects;

    }

    private void updateOtherConfigs(IConfiguration cfg, int buildKind) {
        new OtherConfigVerifier(cfg).updateOtherConfigs(buildKind == FULL_BUILD ? null : getDelta(getProject()));
    }

    /**
     * Check whether the build has been canceled. Cancellation requests
     * propagated to the caller by throwing <code>OperationCanceledException</code>.
     *
     * @see org.eclipse.core.runtime.OperationCanceledException#OperationCanceledException()
     */
    public void checkCancel(IProgressMonitor monitor) {
        if (monitor != null && monitor.isCanceled()) {
            outputTrace(getProject().getName(), "Build cancelled"); //$NON-NLS-1$
            forgetLastBuiltState();
            throw new OperationCanceledException();
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.core.resources.IncrementalProjectBuilder#clean(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    protected void clean(IProgressMonitor monitor) throws CoreException {
        if (DEBUG_EVENTS)
            printEvent(IncrementalProjectBuilder.CLEAN_BUILD, null);

        referencedProjects = getProject().getReferencedProjects();
        outputTrace(getProject().getName(), "Clean build requested"); //$NON-NLS-1$
        IManagedBuildInfo info = getBuildInfo();
        if (info == null) {
            outputError(getProject().getName(), "Build information was not found"); //$NON-NLS-1$
            return;
        }
        if (!info.isValid()) {
            outputError(getProject().getName(), "Build information is not valid"); //$NON-NLS-1$
            return;
        }
        IPath buildDirPath = getProject().getLocation().append(info.getConfigurationName());
        IWorkspace workspace = CCorePlugin.getWorkspace();
        IContainer buildDir = workspace.getRoot().getContainerForLocation(buildDirPath);
        if (buildDir == null || !buildDir.isAccessible()) {
            outputError(buildDir == null ? "null" : buildDir.getName(), "Could not delete the build directory"); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }
        String status;
        try {
            // try the brute force approach first
            status = MessageFormat.format(ManagedMakeBuilder_message_clean_deleting_output, buildDir.getName());
            monitor.subTask(status);
            workspace.delete(new IResource[] { buildDir }, true, monitor);
            StringBuilder buf = new StringBuilder();
            // write to the console
            IConsole console = CCorePlugin.getDefault().getConsole();
            console.start(getProject());
            ConsoleOutputStream consoleOutStream = console.getOutputStream();
            String[] consoleHeader = new String[3];
            consoleHeader[0] = ManagedMakeBuilder_type_clean;
            consoleHeader[1] = info.getConfigurationName();
            consoleHeader[2] = getProject().getName();
            buf.append(NEWLINE);
            buf.append(MessageFormat.format(ManagedMakeBuilder_message_console_header, consoleHeader)).append(NEWLINE);
            consoleOutStream.write(buf.toString().getBytes());
            consoleOutStream.flush();
            buf = new StringBuilder();
            // Report a successful clean
            String successMsg = MessageFormat.format(ManagedMakeBuilder_message_finished, getProject().getName());
            buf.append(successMsg).append(NEWLINE);
            consoleOutStream.write(buf.toString().getBytes());
            consoleOutStream.flush();
            consoleOutStream.close();
        } catch (CoreException e) {
            // Create a makefile generator for the build
            status = MessageFormat.format(ManagedMakeBuilder_message_clean_build_clean, buildDir.getName());
            monitor.subTask(status);
            IMakefileGenerator generator = ManagedBuildManager.getBuildfileGenerator(info.getDefaultConfiguration());
            generator.initialize(getProject(), info, monitor);
            cleanBuild(info, generator, monitor);
        } catch (IOException io) {
        } //  Ignore console failures...
    }

    /* (non-Javadoc)
     * @param info
     * @param generator
     * @param monitor
     */
    protected void cleanBuild(IManagedBuildInfo info, IMakefileGenerator generator, IProgressMonitor monitor) {
        // Make sure that there is a top level directory and a set of makefiles
        IPath buildDir = generator.getBuildWorkingDir();
        if (buildDir == null) {
            buildDir = new Path(info.getConfigurationName());
        }
        IPath makefilePath = getProject().getLocation().append(buildDir.append(generator.getMakefileName()));
        IWorkspaceRoot root = CCorePlugin.getWorkspace().getRoot();
        IFile makefile = root.getFileForLocation(makefilePath);

        if (makefile != null && makefile.isAccessible()) {
            // invoke make with the clean argument
            String statusMsg = MessageFormat.format(ManagedMakeBuilder_message_starting, getProject().getName());
            monitor.subTask(statusMsg);
            checkCancel(monitor);
            invokeMake(CLEAN_BUILD, buildDir, info, generator, monitor);
        }
    }

    /* (non-Javadoc)
     * @param info
     * @param generator
     * @param monitor
     */
    protected void fullBuild(IManagedBuildInfo info, IMakefileGenerator generator, IProgressMonitor monitor)
            throws CoreException {
        // Always need one of these bad boys
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        checkCancel(monitor);
        //If the previous builder invocation was cancelled, generated files might be corrupted
        //in case one or more of the generated makefiles (e.g. dep files) are corrupted,
        //the builder invocation might fail because of the possible syntax errors, so e.g. "make clean" will not work
        //we need to explicitly clean the generated directories
        //		clean(new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));

        // Regenerate the makefiles for this project
        checkCancel(monitor);
        String statusMsg = MessageFormat.format(ManagedMakeBuilder_message_rebuild_makefiles, getProject().getName());
        monitor.subTask(statusMsg);
        //generator = ManagedBuildManager.getBuildfileGenerator(info.getDefaultConfiguration());
        generator.initialize(getProject(), info, monitor);
        MultiStatus result = generator.regenerateMakefiles();
        if (result.getCode() == IStatus.WARNING || result.getCode() == IStatus.INFO) {
            IStatus[] kids = result.getChildren();
            for (int index = 0; index < kids.length; ++index) {
                // One possibility is that there is nothing to build
                IStatus status = kids[index];
                if (status.getCode() == IMakefileGenerator.NO_SOURCE_FOLDERS) {
                    // Inform the user, via the console, that there is nothing to build
                    // either because there are no buildable sources files or all potentially
                    // buildable files have been excluded from build
                    try {
                        emitNoSourceMessage(FULL_BUILD, status, info.getConfigurationName());
                    } catch (CoreException e) {
                        // Throw the exception back to the builder
                        throw e;
                    }
                    // Dude, we're done
                    return;
                } else {
                    // Stick this in the list of stuff to warn the user about
                    getGenerationProblems().add(status);
                }
            }
        }

        // Now call make
        checkCancel(monitor);
        statusMsg = MessageFormat.format(ManagedMakeBuilder_message_starting, getProject().getName());
        monitor.subTask(statusMsg);
        IPath topBuildDir = generator.getBuildWorkingDir();
        if (topBuildDir != null) {
            invokeMake(FULL_BUILD, topBuildDir, info, generator, monitor);
        } else {
            statusMsg = MessageFormat.format(ManagedMakeBuilder_message_no_build, getProject().getName());
            monitor.subTask(statusMsg);
            return;
        }

        // Now regenerate the dependencies
        checkCancel(monitor);
        statusMsg = MessageFormat.format(ManagedMakeBuilder_message_regen_deps, getProject().getName());
        monitor.subTask(statusMsg);
        try {
            generator.regenerateDependencies(false);
        } catch (CoreException e) {
            // Throw the exception back to the builder
            throw e;
        }

        //  Build finished message
        statusMsg = MessageFormat.format(ManagedMakeBuilder_message_finished, getProject().getName());
        monitor.subTask(statusMsg);
    }

    /* (non-Javadoc)
     *
     * @return
     */
    private Vector<IStatus> getGenerationProblems() {
        if (generationProblems == null) {
            generationProblems = new Vector<>();
        }
        return generationProblems;
    }

    /* (non-javadoc)
     * Answers an array of strings with the proper make targets
        * for a build with no custom prebuild/postbuild steps
     *
     * @param fullBuild
     * @return
     */
    protected String[] getMakeTargets(int buildType) {
        List<String> args = new ArrayList<>();
        switch (buildType) {
        case CLEAN_BUILD:
            args.add("clean"); //$NON-NLS-1$
            break;
        case FULL_BUILD:
        case INCREMENTAL_BUILD:
            args.add("all"); //$NON-NLS-1$
            break;
        }
        return args.toArray(new String[args.size()]);
    }

    protected List<IResource> getResourcesToBuild() {
        if (resourcesToBuild == null) {
            resourcesToBuild = new ArrayList<>();
        }
        return resourcesToBuild;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.core.resources.ACBuilder#getWorkingDirectory()
     */
    public IPath getWorkingDirectory() {
        return getProject().getLocation();
    }

    /* (non-Javadoc)
     * @param delta
     * @param info
     * @param monitor
     * @throws CoreException
     */
    protected void incrementalBuild(IResourceDelta delta, IManagedBuildInfo info, IMakefileGenerator generator,
            IProgressMonitor monitor) throws CoreException {
        // Need to report status to the user
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        // Ask the makefile generator to generate any makefiles needed to build delta
        checkCancel(monitor);
        String statusMsg = MessageFormat.format(ManagedMakeBuilder_message_update_makefiles, getProject().getName());
        monitor.subTask(statusMsg);
        MultiStatus result = generator.generateMakefiles(delta);
        if (result.getCode() == IStatus.WARNING || result.getCode() == IStatus.INFO) {
            IStatus[] kids = result.getChildren();
            for (int index = 0; index < kids.length; ++index) {
                // One possibility is that there is nothing to build
                IStatus status = kids[index];
                if (status.getCode() == IMakefileGenerator.NO_SOURCE_FOLDERS) {
                    // Inform the user, via the console, that there is nothing to build
                    // either because there are no buildable sources files or all potentially
                    // buildable files have been excluded from build
                    try {
                        emitNoSourceMessage(INCREMENTAL_BUILD, status, info.getConfigurationName());
                    } catch (CoreException e) {
                        // Throw the exception back to the builder
                        throw e;
                    }
                    // Dude, we're done
                    return;
                } else {
                    // Stick this in the list of stuff to warn the user about
                    getGenerationProblems().add(status);
                }
            }
        }

        // Run the build
        checkCancel(monitor);
        statusMsg = MessageFormat.format(ManagedMakeBuilder_message_starting, getProject().getName());
        monitor.subTask(statusMsg);
        IPath buildDir = generator.getBuildWorkingDir();
        if (buildDir != null) {
            invokeMake(INCREMENTAL_BUILD, buildDir, info, generator, monitor);
        } else {
            statusMsg = MessageFormat.format(ManagedMakeBuilder_message_no_build, getProject().getName());
            monitor.subTask(statusMsg);
            return;
        }

        // Generate the dependencies for all changes
        checkCancel(monitor);
        statusMsg = MessageFormat.format(ManagedMakeBuilder_message_updating_deps, getProject().getName());
        monitor.subTask(statusMsg);
        try {
            generator.generateDependencies();
        } catch (CoreException e) {
            throw e;
        }

        // Build finished message
        statusMsg = MessageFormat.format(ManagedMakeBuilder_message_finished, getProject().getName());
        monitor.subTask(statusMsg);
    }

    /* (non-Javadoc)
     * @param buildType
     * @param buildDir
     * @param info
     * @param generator
     * @param monitor
     */
    protected void invokeMake(int buildType, IPath buildDir, IManagedBuildInfo info, IMakefileGenerator generator,
            IProgressMonitor monitor) {
        // Get the project and make sure there's a monitor to cancel the build
        IProject project = getProject();
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        try {
            // Figure out the working directory for the build and make sure there is a makefile there
            final URI workingDirectoryURI = getProject().getFolder(buildDir).getLocationURI();
            final String pathFromURI = EFSExtensionManager.getDefault().getPathFromURI(workingDirectoryURI);
            if (pathFromURI == null) {
                throw new CoreException(
                        new Status(IStatus.ERROR, Activator.getId(), ManagedMakeBuilder_message_error, null));
            }

            IPath workingDirectory = new Path(pathFromURI);

            IWorkspace workspace = project.getWorkspace();
            if (workspace == null) {
                return;
            }
            IWorkspaceRoot root = workspace.getRoot();
            if (root == null) {
                return;
            }
            IPath makefile = workingDirectory.append(generator.getMakefileName());
            if (root.getFileForLocation(makefile) == null) {
                return;
            }

            // Flag to the user that make is about to be called
            String makeCmd = info.getBuildCommand();
            //try to resolve the build macros in the builder command
            try {
                String resolved = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(makeCmd, "", //$NON-NLS-1$
                        " ", //$NON-NLS-1$
                        IBuildMacroProvider.CONTEXT_CONFIGURATION, info.getDefaultConfiguration());
                if ((resolved = resolved.trim()).length() > 0)
                    makeCmd = resolved;
            } catch (BuildMacroException e) {
            }

            IPath makeCommand = new Path(makeCmd);
            String[] msgs = new String[2];
            msgs[0] = makeCommand.toString();
            msgs[1] = project.getName();
            monitor.subTask(MessageFormat.format(ManagedMakeBuilder_message_make, msgs));

            // Get a build console for the project
            StringBuilder buf = new StringBuilder();
            IConsole console = CCorePlugin.getDefault().getConsole();
            console.start(project);
            ConsoleOutputStream consoleOutStream = console.getOutputStream();
            String[] consoleHeader = new String[3];
            switch (buildType) {
            case FULL_BUILD:
            case INCREMENTAL_BUILD:
                consoleHeader[0] = ManagedMakeBuider_type_incremental;
                break;
            case CLEAN_BUILD:
                consoleHeader[0] = ManagedMakeBuilder_type_clean;
                break;
            }

            consoleHeader[1] = info.getConfigurationName();
            consoleHeader[2] = project.getName();
            buf.append(NEWLINE);
            buf.append(MessageFormat.format(ManagedMakeBuilder_message_console_header, consoleHeader)).append(NEWLINE);
            buf.append(NEWLINE);

            IConfiguration cfg = info.getDefaultConfiguration();
            if (!cfg.isSupported()) {
                String msg = MessageFormat.format(WARNING_UNSUPPORTED_CONFIGURATION,
                        new String[] { cfg.getName(), cfg.getToolChain().getName() });
                buf.append(msg).append(NEWLINE);
                buf.append(NEWLINE);
            }
            consoleOutStream.write(buf.toString().getBytes());
            consoleOutStream.flush();

            // Remove all markers for this project
            removeAllMarkers(project);

            // Get a launcher for the make command
            String errMsg = null;
            IBuilder builder = info.getDefaultConfiguration().getBuilder();
            ICommandLauncher launcher = builder.getCommandLauncher();
            launcher.setProject(project);
            launcher.showCommand(true);

            // Set the environmennt
            IEnvironmentVariable[] variables = ManagedBuildManager.getEnvironmentVariableProvider().getVariables(cfg,
                    true);
            String[] envp = null;
            ArrayList<String> envList = new ArrayList<>();
            if (variables != null) {
                for (int i = 0; i < variables.length; i++) {
                    envList.add(variables[i].getName() + "=" + variables[i].getValue()); //$NON-NLS-1$
                }
                envp = envList.toArray(new String[envList.size()]);
            }

            // Hook up an error parser manager
            String[] errorParsers = info.getDefaultConfiguration().getErrorParserList();
            ErrorParserManager epm = new ErrorParserManager(getProject(), workingDirectoryURI, this, errorParsers);
            epm.setOutputStream(consoleOutStream);
            // This variable is necessary to ensure that the EPM stream stay open
            // until we explicitly close it. See bug#123302.
            OutputStream epmOutputStream = epm.getOutputStream();

            // Get the arguments to be passed to make from build model
            ArrayList<String> makeArgs = new ArrayList<>();
            String arg = info.getBuildArguments();
            if (arg.length() > 0) {
                String[] args = arg.split("\\s"); //$NON-NLS-1$
                for (int i = 0; i < args.length; ++i) {
                    makeArgs.add(args[i]);
                }
            }

            String[] makeTargets;
            String prebuildStep = info.getPrebuildStep();
            //try to resolve the build macros in the prebuildStep
            try {
                prebuildStep = ManagedBuildManager.getBuildMacroProvider().resolveValueToMakefileFormat(prebuildStep,
                        "", //$NON-NLS-1$
                        " ", //$NON-NLS-1$
                        IBuildMacroProvider.CONTEXT_CONFIGURATION, cfg);
            } catch (BuildMacroException e) {
            }
            boolean prebuildStepPresent = (prebuildStep.length() > 0);
            Process proc = null;
            boolean isuptodate = false;

            if (prebuildStepPresent) {
                @SuppressWarnings("unchecked")
                ArrayList<String> premakeArgs = (ArrayList<String>) makeArgs.clone();
                String[] premakeTargets;
                switch (buildType) {
                case INCREMENTAL_BUILD: {
                    // For an incremental build with a prebuild step:
                    // Check the status of the main build with "make -q main-build"
                    // If up to date:
                    // then: don't invoke the prebuild step, which should be run only if
                    //       something needs to be built in the main build
                    // else: invoke the prebuild step and the main build step
                    premakeArgs.add("-q"); //$NON-NLS-1$
                    premakeArgs.add("main-build"); //$NON-NLS-1$
                    premakeTargets = premakeArgs.toArray(new String[premakeArgs.size()]);
                    proc = launcher.execute(makeCommand, premakeTargets, envp, workingDirectory, monitor);
                    if (proc != null) {
                        try {
                            // Close the input of the process since we will never write to it
                            proc.getOutputStream().close();
                        } catch (IOException e) {
                        }
                        if (launcher.waitAndRead(epm.getOutputStream(), epm.getOutputStream(),
                                new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN)) != ICommandLauncher.OK) {
                            errMsg = launcher.getErrorMessage();
                        }
                    } else {
                        errMsg = launcher.getErrorMessage();
                    }

                    if ((errMsg != null && errMsg.length() > 0) || proc == null) {
                        // Can't tell if the build is needed, so assume it is, and let any errors be triggered
                        // when the "real" build is invoked below
                        makeArgs.add("pre-build"); //$NON-NLS-1$
                        makeArgs.add("main-build"); //$NON-NLS-1$
                    } else {
                        // The "make -q" command launch was successful
                        if (proc.exitValue() == 0) {
                            // If the status value returned from "make -q" is 0, then the build state is up-to-date
                            isuptodate = true;
                            // Report that the build was up to date, and thus nothing needs to be built
                            String uptodateMsg = MessageFormat.format(ManagedMakeBuilder_message_no_build,
                                    project.getName());
                            buf = new StringBuilder();
                            buf.append(NEWLINE);
                            buf.append(uptodateMsg).append(NEWLINE);
                            // Write message on the console
                            consoleOutStream.write(buf.toString().getBytes());
                            consoleOutStream.flush();
                            epmOutputStream.close();
                            consoleOutStream.close();
                        } else {
                            // The status value was other than 0, so press on with the build process
                            makeArgs.add("pre-build"); //$NON-NLS-1$
                            makeArgs.add("main-build"); //$NON-NLS-1$
                        }
                    }
                    break;
                }
                case FULL_BUILD: {
                    //						makeArgs.add("clean"); //$NON-NLS-1$
                    makeArgs.add("pre-build"); //$NON-NLS-1$
                    makeArgs.add("main-build"); //$NON-NLS-1$
                    break;
                }
                case CLEAN_BUILD: {
                    makeArgs.add("clean"); //$NON-NLS-1$
                    break;
                }
                }

            } else {
                // No prebuild step
                //
                makeArgs.addAll(Arrays.asList(getMakeTargets(buildType)));
            }

            makeTargets = makeArgs.toArray(new String[makeArgs.size()]);

            // Launch make - main invocation
            if (!isuptodate) {
                proc = launcher.execute(makeCommand, makeTargets, envp, workingDirectory, monitor);
                if (proc != null) {
                    try {
                        // Close the input of the process since we will never write to it
                        proc.getOutputStream().close();
                    } catch (IOException e) {
                    }

                    int state = launcher.waitAndRead(epm.getOutputStream(), epm.getOutputStream(),
                            new SubProgressMonitor(monitor, IProgressMonitor.UNKNOWN));
                    if (state != ICommandLauncher.OK) {
                        errMsg = launcher.getErrorMessage();

                        if (state == ICommandLauncher.COMMAND_CANCELED) {
                            //TODO: the better way of handling cancel is needed
                            //currently the rebuild state is set to true forcing the full rebuild
                            //on the next builder invocation
                           // info.getDefaultConfiguration().setRebuildState(true);
                        }
                    }

                    // Force a resync of the projects without allowing the user to cancel.
                    // This is probably unkind, but short of this there is no way to insure
                    // the UI is up-to-date with the build results
                    monitor.subTask(ManagedMakeBuilder_message_updating);
                    try {
                        //currentProject.refreshLocal(IResource.DEPTH_INFINITE, null);

                        // use the refresh scope manager to refresh
                        RefreshScopeManager refreshManager = RefreshScopeManager.getInstance();
                        IWorkspaceRunnable runnable = refreshManager.getRefreshRunnable(project, cfg.getName());
                        ResourcesPlugin.getWorkspace().run(runnable, null, IWorkspace.AVOID_UPDATE, null);
                    } catch (CoreException e) {
                        monitor.subTask(ManagedMakeBuilder_message_error_refresh);
                    }
                } else {
                    errMsg = launcher.getErrorMessage();
                }

                // Report either the success or failure of our mission
                buf = new StringBuilder();
                if (errMsg != null && errMsg.length() > 0) {
                    buf.append(errMsg).append(NEWLINE);
                } else {
                    // Report a successful build
                    String successMsg = MessageFormat.format(ManagedMakeBuilder_message_finished, project.getName());
                    buf.append(successMsg).append(NEWLINE);
                }

                // Write message on the console
                consoleOutStream.write(buf.toString().getBytes());
                consoleOutStream.flush();
                epmOutputStream.close();

                // Generate any error markers that the build has discovered
                monitor.subTask(ManagedMakeBuilder_message_creating_markers);
                addBuilderMarkers(epm);
                consoleOutStream.close();
            }
        } catch (Exception e) {
            forgetLastBuiltState();
        } finally {
            getGenerationProblems().clear();
        }
    }

    /* (non-Javadoc)
     * Removes the IMarkers for the project specified in the argument if the
     * project exists, and is open.
     *
     * @param project
     */
    @SuppressWarnings("restriction")
    private void removeAllMarkers(IProject project) {
        if (project == null || !project.isAccessible())
            return;

        // Clear out the problem markers
        IWorkspace workspace = project.getWorkspace();
        IMarker[] markers;
        try {
            markers = project.findMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
        } catch (CoreException e) {
            // Handled just about every case in the sanity check
            return;
        }
        if (markers != null) {
            try {
                workspace.deleteMarkers(markers);
                FixitManager.getInstance().deleteMarkers(markers);
            } catch (CoreException e) {
                // The only situation that might cause this is some sort of resource change event
                return;
            }
        }
    }


    private Map<IProject, List<IFile>> arrangeFilesByProject(List<IFile> files) {
        Map<IProject, List<IFile>> projectMap = new HashMap<>();
        for (IFile file : files) {
            IProject project = file.getProject();
            List<IFile> filesInProject = projectMap.get(project);
            if (filesInProject == null) {
                filesInProject = new ArrayList<>();
                projectMap.put(project, filesInProject);
            }
            filesInProject.add(file);
        }
        return projectMap;
    }

    private void invokeInternalBuilderForOneProject(List<IFile> files, IProgressMonitor monitor) {
        IProject project = files.get(0).getProject();
        BuildRunnerHelper buildRunnerHelper = new BuildRunnerHelper(project);

        try {
            monitor.beginTask(MessageFormat.format(GeneratedMakefileBuilder_buildingProject, project.getName()) + ':',
                    TICKS_STREAM_PROGRESS_MONITOR + files.size() * PROGRESS_MONITOR_SCALE);

            // Get a build console for the project
            console = CCorePlugin.getDefault().getConsole();
            console.start(project);

            IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(project);
            IConfiguration configuration = buildInfo.getDefaultConfiguration();

            String cfgName = configuration.getName();
            String toolchainName = configuration.getToolChain().getName();
            IFile buildFolder = project.getFile(cfgName);

            boolean isSupported = configuration.isSupported();

            //IBuildDescription des = BuildDescriptionManager.createBuildDescription(configuration, null, 0);

            String[] errorParsers = configuration.getErrorParserList();
            ErrorParserManager epm = new ErrorParserManager(project, buildFolder.getLocationURI(), this, errorParsers);

            buildRunnerHelper.prepareStreams(epm, null, console,
                    new SubProgressMonitor(monitor, TICKS_STREAM_PROGRESS_MONITOR));
            OutputStream stdout = buildRunnerHelper.getOutputStream();
            OutputStream stderr = buildRunnerHelper.getErrorStream();

            buildRunnerHelper.greeting(GeneratedMakefileBuilder_buildingSelectedFiles, cfgName, toolchainName,
                    isSupported);
            buildRunnerHelper.printLine(ManagedMakeBuilder_message_internal_builder_header_note);

            // Build artifacts for each file in the project
            for (IFile file : files) {
                if (monitor.isCanceled()) {
                    break;
                }
                String filePath = file.getProjectRelativePath().toString();

                try {
                    //                    IBuildResource buildResource = des.getBuildResource(file);
                    //
                    //                    Set<IBuildStep> dependentSteps = new HashSet<>();
                    //                    IBuildIOType depTypes[] = buildResource.getDependentIOTypes();
                    //                    for (IBuildIOType btype : depTypes) {
                    //                        if (btype != null && btype.getStep() != null)
                    //                            dependentSteps.add(btype.getStep());
                    //                    }

                    SubProgressMonitor monitor2 = new SubProgressMonitor(monitor, 1 * PROGRESS_MONITOR_SCALE,
                            SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
                    try {
                        monitor2.beginTask("", TICKS_DELETE_MARKERS + PROGRESS_MONITOR_SCALE); //$NON-NLS-1$

                        // Remove problem markers for the file
                        monitor2.subTask(
                                MessageFormat.format(GeneratedMakefileBuilder_removingResourceMarkers, filePath));
                        buildRunnerHelper.removeOldMarkers(file, new SubProgressMonitor(monitor2, TICKS_DELETE_MARKERS,
                                SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));

                        // Build dependent steps
                        //                        for (IBuildStep step : dependentSteps) {
                        //                            if (monitor2.isCanceled()) {
                        //                                break;
                        //                            }
                        //
                        //                            monitor2.subTask(filePath);
                        //                            StepBuilder stepBuilder = new StepBuilder(step, null);
                        //                            stepBuilder.build(stdout, stderr, new SubProgressMonitor(monitor2,
                        //                                    1 * PROGRESS_MONITOR_SCALE, SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));
                        //
                        //                            monitor2.subTask(
                        //                                    MessageFormat.format(GeneratedMakefileBuilder_refreshingArtifacts, filePath));
                        //                            IBuildIOType[] outputIOTypes = step.getOutputIOTypes();
                        //                            for (IBuildIOType type : outputIOTypes) {
                        //                                for (IBuildResource outResource : type.getResources()) {
                        //                                    IFile outFile = project.getFile(outResource.getLocation());
                        //                                    // Refresh the output resource without allowing the user to cancel.
                        //                                    outFile.refreshLocal(IResource.DEPTH_INFINITE, null);
                        //                                }
                        //                            }
                        //                        }
                    } finally {
                        monitor2.done();
                    }

                } catch (Exception e) {
                    Activator
                            .log(new CoreException(new Status(IStatus.ERROR, Activator.getId(), "CDT Build Error", e))); //$NON-NLS-1$
                }

            }
            buildRunnerHelper.close();
            buildRunnerHelper.goodbye();

        } catch (Exception e) {
            Activator.log(new CoreException(new Status(IStatus.ERROR, Activator.getId(), "CDT Build Error", e))); //$NON-NLS-1$
            forgetLastBuiltState();
        } finally {
            getGenerationProblems().clear();
            try {
                buildRunnerHelper.close();
            } catch (IOException e) {
                Activator.log(new CoreException(new Status(IStatus.ERROR, Activator.getId(), "CDT Build Error", e))); //$NON-NLS-1$
            }
            monitor.done();
        }
    }

    public IStatus cleanFiles(List<IFile> files, IProgressMonitor monitor) {
        // Make sure there's a monitor to cancel the build
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        try {
            Map<IProject, List<IFile>> projectMap = arrangeFilesByProject(files);
            monitor.beginTask("", projectMap.size() * PROGRESS_MONITOR_SCALE); //$NON-NLS-1$

            for (List<IFile> filesInProject : projectMap.values()) {
                IProject project = filesInProject.get(0).getProject();
                monitor.subTask(MessageFormat.format(GeneratedMakefileBuilder_cleaningProject, project.getName()));
                cleanFilesForOneProject(filesInProject, new SubProgressMonitor(monitor, 1 * PROGRESS_MONITOR_SCALE,
                        SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK));
            }
        } finally {
            if (monitor.isCanceled()) {
                return Status.CANCEL_STATUS;
            }
            monitor.done();
        }
        return Status.OK_STATUS;
    }

    public void cleanFilesForOneProject(List<IFile> files, IProgressMonitor monitor) {
        IProject project = files.get(0).getProject();
        BuildRunnerHelper buildRunnerHelper = new BuildRunnerHelper(project);
        int countDeleted = 0;

        try {
            monitor.beginTask(MessageFormat.format(GeneratedMakefileBuilder_cleaningProject, project.getName()) + ':',
                    TICKS_STREAM_PROGRESS_MONITOR + files.size() * PROGRESS_MONITOR_SCALE);

            // Get a build console for the project
            console = CCorePlugin.getDefault().getConsole();
            console.start(project);

            IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(project);
            IConfiguration configuration = buildInfo.getDefaultConfiguration();

            String cfgName = configuration.getName();
            IFile buildLocation = project.getFile(cfgName);
            String toolchainName = configuration.getToolChain().getName();
            boolean isSupported = configuration.isSupported();

            //int flags = BuildDescriptionManager.REBUILD | BuildDescriptionManager.REMOVED;
            IResourceDelta delta = getDelta(project);

            //IBuildDescription des = BuildDescriptionManager.createBuildDescription(configuration, delta, flags);

            String[] errorParsers = configuration.getErrorParserList();
            ErrorParserManager epm = new ErrorParserManager(project, buildLocation.getLocationURI(), this,
                    errorParsers);
            buildRunnerHelper.prepareStreams(epm, null, console,
                    new SubProgressMonitor(monitor, TICKS_STREAM_PROGRESS_MONITOR));

            buildRunnerHelper.greeting(GeneratedMakefileBuilder_cleanSelectedFiles, cfgName, toolchainName,
                    isSupported);
            buildRunnerHelper.printLine(ManagedMakeBuilder_message_internal_builder_header_note);

            for (IFile file : files) {
                if (monitor.isCanceled()) {
                    break;
                }
                String filePath = file.getProjectRelativePath().toString();

                try {
                    //                    IBuildResource buildResource = des.getBuildResource(file);
                    //                    if (buildResource != null) {
                    //                        Set<IBuildStep> dependentSteps = new HashSet<>();
                    //                        IBuildIOType depTypes[] = buildResource.getDependentIOTypes();
                    //                        for (IBuildIOType btype : depTypes) {
                    //                            if (btype != null && btype.getStep() != null)
                    //                                dependentSteps.add(btype.getStep());
                    //                        }

                    SubProgressMonitor monitor2 = new SubProgressMonitor(monitor, 1 * PROGRESS_MONITOR_SCALE,
                            SubProgressMonitor.PREPEND_MAIN_LABEL_TO_SUBTASK);
                    try {
                        monitor2.beginTask("", //$NON-NLS-1$
                                TICKS_DELETE_MARKERS + PROGRESS_MONITOR_SCALE);

                        // Remove problem markers for the file
                        monitor2.subTask(
                                MessageFormat.format(GeneratedMakefileBuilder_removingResourceMarkers, filePath));
                        buildRunnerHelper.removeOldMarkers(file, new SubProgressMonitor(monitor2, TICKS_DELETE_MARKERS,
                                SubProgressMonitor.SUPPRESS_SUBTASK_LABEL));

                        // iterate through all build steps
                        //                            for (IBuildStep step : dependentSteps) {
                        //                                if (monitor2.isCanceled()) {
                        //                                    break;
                        //                                }
                        //
                        //                                monitor2.subTask(filePath);
                        //                                // Delete the output resources
                        //                                IBuildIOType[] outputIOTypes = step.getOutputIOTypes();
                        //
                        //                                for (IBuildIOType ioType : outputIOTypes) {
                        //                                    for (IBuildResource rc : ioType.getResources()) {
                        //                                        IResource outputFile = project
                        //                                                .findMember(rc.getFullPath().removeFirstSegments(1)); // strip project name
                        //                                        if (outputFile != null) {
                        //                                            outputFile.delete(true, null);
                        //                                            countDeleted++;
                        //                                            buildRunnerHelper.printLine(
                        //                                                    MessageFormat.format(GeneratedMakefileBuilder_fileDeleted,
                        //                                                            outputFile.getProjectRelativePath().toString()));
                        //                                        }
                        //                                    }
                        //                                }
                        //
                        //                                monitor2.worked(1 * PROGRESS_MONITOR_SCALE);
                        //                            }
                    } finally {
                        monitor2.done();
                    }
                    //                  }
                } catch (Exception e) {
                    Activator
                            .log(new CoreException(new Status(IStatus.ERROR, Activator.getId(), "CDT Build Error", e))); //$NON-NLS-1$
                }

            }
            if (countDeleted == 0) {
                buildRunnerHelper.printLine(GeneratedMakefileBuilder_nothingToClean);
            }
            buildRunnerHelper.close();
            buildRunnerHelper.goodbye();

        } catch (Exception e) {
            Activator.log(new CoreException(new Status(IStatus.ERROR, Activator.getId(), "CDT Build Error", e))); //$NON-NLS-1$
        } finally {
            try {
                buildRunnerHelper.close();
            } catch (IOException e) {
                Activator.log(new CoreException(new Status(IStatus.ERROR, Activator.getId(), "CDT Build Error", e))); //$NON-NLS-1$
            }
            monitor.done();
        }
    }
}
