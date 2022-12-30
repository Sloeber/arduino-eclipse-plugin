/*******************************************************************************
 * Copyright (c) 2010, 2017 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Wind River Systems - Initial API and implementation
 * James Blackburn (Broadcom Corp.)
 * IBM Corporation
 * Samuel Hultgren (STMicroelectronics) - bug #217674
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import static io.sloeber.autoBuild.core.Messages.*;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.IConsoleParser;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.internal.core.BuildRunnerHelper;
//import org.eclipse.cdt.internal.core.BuildRunnerHelper;
//import org.eclipse.cdt.managedbuilder.buildmodel.BuildDescriptionManager;
//import org.eclipse.cdt.managedbuilder.buildmodel.IBuildDescription;
//import org.eclipse.cdt.managedbuilder.internal.buildmodel.BuildStateManager;
//import org.eclipse.cdt.managedbuilder.internal.buildmodel.DescriptionBuilder;
//import org.eclipse.cdt.managedbuilder.internal.buildmodel.IConfigurationBuildState;
//import org.eclipse.cdt.managedbuilder.internal.buildmodel.IProjectBuildState;
//import org.eclipse.cdt.managedbuilder.internal.buildmodel.ParallelBuilder;
//import org.eclipse.cdt.managedbuilder.internal.core.ManagedMakeMessages;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import io.sloeber.autoBuild.api.IBuilder;
import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IBuildRunner;

/**
 * The build runner for the internal builder.
 *
 * @author dschaefer
 * @since 8.0
 */
public class InternalBuildRunner extends IBuildRunner {
    private static final int PROGRESS_MONITOR_SCALE = 100;
    private static final int TICKS_STREAM_PROGRESS_MONITOR = 1 * PROGRESS_MONITOR_SCALE;
    private static final int TICKS_DELETE_MARKERS = 1 * PROGRESS_MONITOR_SCALE;
    private static final int TICKS_EXECUTE_COMMAND = 1 * PROGRESS_MONITOR_SCALE;
    private static final int TICKS_REFRESH_PROJECT = 1 * PROGRESS_MONITOR_SCALE;

    @Override
    public boolean invokeBuild(int kind, IProject project, IConfiguration configuration, IBuilder builder,
            IConsole console, IMarkerGenerator markerGenerator, IncrementalProjectBuilder projectBuilder,
            IProgressMonitor monitor) throws CoreException {

        SubMonitor parentMon = SubMonitor.convert(monitor);
        BuildRunnerHelper buildRunnerHelper = new BuildRunnerHelper(project);

        try {
            if (monitor == null) {
                monitor = new NullProgressMonitor();
            }
            monitor.beginTask("", TICKS_STREAM_PROGRESS_MONITOR + TICKS_DELETE_MARKERS + TICKS_EXECUTE_COMMAND //$NON-NLS-1$
                    + TICKS_REFRESH_PROJECT);

            boolean isParallel = builder.getParallelizationNum() > 1;
            boolean resumeOnErr = !builder.isStopOnError();

            int flags = 0;
            IResourceDelta delta = projectBuilder.getDelta(project);
            //            BuildStateManager bsMngr = BuildStateManager.getInstance();
            //            IProjectBuildState pBS = bsMngr.getProjectBuildState(project);
            //            IConfigurationBuildState cBS = pBS.getConfigurationBuildState(configuration.getId(), true);
            //
            //            //			if(delta != null){
            //            flags = BuildDescriptionManager.REBUILD | BuildDescriptionManager.REMOVED | BuildDescriptionManager.DEPS;
            //				delta = getDelta(currentProject);
            //			}
            boolean buildIncrementaly = delta != null;

            ICConfigurationDescription cfgDescription = ManagedBuildManager
                    .getDescriptionForConfiguration(configuration);

            // Prepare launch parameters for BuildRunnerHelper
            String cfgName = configuration.getName();
            String toolchainName = configuration.getToolChain().getName();
            boolean isConfigurationSupported = configuration.isSupported();

            URI workingDirectoryURI = ManagedBuildManager.getBuildLocationURI(configuration, builder);

            String[] errorParsers = builder.getErrorParsers();
            ErrorParserManager epm = new ErrorParserManager(project, workingDirectoryURI, markerGenerator,
                    errorParsers);

            List<IConsoleParser> parsers = new ArrayList<>();
            ManagedBuildManager.collectLanguageSettingsConsoleParsers(cfgDescription, epm, parsers);

            buildRunnerHelper.prepareStreams(epm, parsers, console, parentMon.newChild(5));

            //            IBuildDescription des = BuildDescriptionManager.createBuildDescription(configuration, cBS, delta, flags);
            //            DescriptionBuilder dBuilder = null;
            //            if (!isParallel) {
            //                dBuilder = new DescriptionBuilder(des, buildIncrementaly, resumeOnErr, cBS);
            //                if (dBuilder.getNumCommands() <= 0) {
            //                    buildRunnerHelper
            //                            .printLine(MessageFormat.format(ManagedMakeBuilder_message_no_build, project.getName())); //$NON-NLS-1$
            //                    return false;
            //                }
            //            }

            buildRunnerHelper.removeOldMarkers(project, parentMon.newChild(5));

            if (buildIncrementaly) {
                buildRunnerHelper.greeting(IncrementalProjectBuilder.INCREMENTAL_BUILD, cfgName, toolchainName,
                        isConfigurationSupported);
            } else {
                buildRunnerHelper.greeting(ManagedMakeBuider_type_rebuild, cfgName, toolchainName,
                        isConfigurationSupported);
            }
            buildRunnerHelper.printLine(ManagedMakeBuilder_message_internal_builder_header_note);

            OutputStream stdout = buildRunnerHelper.getOutputStream();
            OutputStream stderr = buildRunnerHelper.getErrorStream();

            int status;
            epm.deferDeDuplication();
            try {

                //                if (dBuilder != null) {
                //                    status = dBuilder.build(stdout, stderr, parentMon.newChild(5));
                //                } else {
                //                    status = ParallelBuilder.build(des, null, null, stdout, stderr, parentMon.newChild(5), resumeOnErr,
                //                            buildIncrementaly, cBS);
                //                    // Bug 403670:
                //                    // Make sure the build configuration's rebuild status is updated with the result of
                //                    // this successful build.  In the non-parallel case this happens within dBuilder.build
                //                    // (the cBS is passed as an instance of IResourceRebuildStateContainer).
                //                    if (status == ParallelBuilder.STATUS_OK)
                //                        cBS.setState(0);
                //                    buildRunnerHelper.printLine(
                //                            MessageFormat.format(CommonBuilder_7, Integer.toString(ParallelBuilder.lastThreadsUsed)));
                //                }
            } finally {
                epm.deDuplicate();
            }

            // bsMngr.setProjectBuildState(project, pBS);
            buildRunnerHelper.close();
            buildRunnerHelper.goodbye();

            // if (status != ICommandLauncher.ILLEGAL_COMMAND) {
            buildRunnerHelper.refreshProject(cfgName, parentMon.newChild(5));
            //}

        } catch (Exception e) {
            projectBuilder.forgetLastBuiltState();

            String msg = MessageFormat.format(ManagedMakeBuilder_message_error_build,
                    new String[] { project.getName(), configuration.getName() });
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e));
        } finally {
            try {
                buildRunnerHelper.close();
            } catch (IOException e) {
                Activator.log(e);
            }
            monitor.done();
        }

        return false;
    }
}
