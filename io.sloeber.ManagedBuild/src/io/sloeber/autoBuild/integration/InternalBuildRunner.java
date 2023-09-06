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
package io.sloeber.autoBuild.integration;

import static io.sloeber.autoBuild.core.Messages.*;
import static io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon.*;
import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.CommandLauncher;
import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.core.IConsoleParser;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.utils.CommandLineUtil;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import io.sloeber.autoBuild.Internal.AutoBuildRunnerHelper;
import io.sloeber.autoBuild.api.IBuildRunner;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.core.Messages;
import io.sloeber.autoBuild.extensionPoint.providers.BuildRunnerForMake;
import io.sloeber.autoBuild.extensionPoint.providers.MakeRule;
import io.sloeber.autoBuild.extensionPoint.providers.MakeRules;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;

public class InternalBuildRunner extends IBuildRunner {
    private static final int PROGRESS_MONITOR_SCALE = 100;
    private static final int TICKS_STREAM_PROGRESS_MONITOR = 1 * PROGRESS_MONITOR_SCALE;
    private static final int TICKS_DELETE_MARKERS = 1 * PROGRESS_MONITOR_SCALE;
    private static final int TICKS_EXECUTE_COMMAND = 1 * PROGRESS_MONITOR_SCALE;
    private static final int TICKS_REFRESH_PROJECT = 1 * PROGRESS_MONITOR_SCALE;

    private static void createFolder(IFolder folder, boolean force, boolean local, IProgressMonitor monitor)
            throws CoreException {
        if (!folder.exists()) {
            IContainer parent = folder.getParent();
            if (parent instanceof IFolder) {
                createFolder((IFolder) parent, force, local, null);
            }
            folder.create(force, local, monitor);
        }
    }

    @Override
    public boolean invokeBuild(int kind, AutoBuildConfigurationDescription autoData, IMarkerGenerator markerGenerator,
            IncrementalProjectBuilder projectBuilder, IConsole console, IProgressMonitor monitor) throws CoreException {

        SubMonitor parentMon = SubMonitor.convert(monitor);
        IBuilder builder = autoData.getConfiguration().getBuilder();
        IProject project = autoData.getProject();
        IConfiguration configuration = autoData.getConfiguration();
        ICConfigurationDescription cfgDescription = autoData.getCdtConfigurationDescription();
        IFolder buildRoot = autoData.getBuildFolder();

        //Generate the make Rules
        MakeRules myMakeRules = new MakeRules(autoData, buildRoot, new HashSet<>());

        try (AutoBuildRunnerHelper buildRunnerHelper = new AutoBuildRunnerHelper(project);
                ErrorParserManager epm = new ErrorParserManager(project, buildRoot.getLocationURI(), markerGenerator,
                        builder.getErrorParserList().toArray(new String[0]));) {

            monitor.beginTask("", TICKS_STREAM_PROGRESS_MONITOR + TICKS_DELETE_MARKERS + TICKS_EXECUTE_COMMAND //$NON-NLS-1$
                    + TICKS_REFRESH_PROJECT);

            // Prepare launch parameters for BuildRunnerHelper
            String cfgName = cfgDescription.getName();
            String toolchainName = configuration.getToolChain().getName();
            boolean isConfigurationSupported = configuration.isSupported();

            List<IConsoleParser> parsers = new ArrayList<>();
            AutoBuildManager.collectLanguageSettingsConsoleParsers(cfgDescription, epm, parsers);

            buildRunnerHelper.prepareStreams(epm, parsers, console, parentMon.newChild(5));
            buildRunnerHelper.greeting(kind, cfgName, toolchainName, isConfigurationSupported);
            if (kind == IncrementalProjectBuilder.CLEAN_BUILD) {
                for (IFile curFile : myMakeRules.getBuildFiles()) {
                    curFile.delete(true, false, monitor);
                }
            } else {

                buildRunnerHelper.removeOldMarkers(project, parentMon.newChild(5));

                buildRunnerHelper.printLine(ManagedMakeBuilder_message_internal_builder_header_note);

                boolean isParallel = autoData.isParallelBuild();
                int parrallelNum = autoData.getParallelizationNum();
                epm.deferDeDuplication();
                int sequenceID = -1;
                boolean lastSequenceID = true;
                boolean isError = false;
                //Run preBuildStep if existing
                String preBuildStep = autoData.getPrebuildStep();
                preBuildStep = resolve(preBuildStep, EMPTY_STRING, WHITESPACE, autoData);
                if (!preBuildStep.isEmpty()) {
                    String announcement = autoData.getPreBuildAnouncement();
                    if (!announcement.isEmpty()) {
                        announcement = announcement + NEWLINE;
                        buildRunnerHelper.getOutputStream().write(announcement.getBytes());
                    }
                    if (launchCommand(preBuildStep, autoData, monitor, buildRunnerHelper) != 0) {
                        if (autoData.stopOnFirstBuildError()) {
                            return false;
                        }
                    }
                }
                do {
                    sequenceID++;
                    lastSequenceID = true;
                    for (MakeRule curRule : myMakeRules) {
                        if (curRule.getSequenceGroupID() != sequenceID) {
                            continue;
                        }
                        lastSequenceID = false;
                        if (!curRule.needsExecuting(buildRoot)) {
                            continue;
                        }

                        //make sure the target folders exists
                        Set<IFile> targetFiles = curRule.getTargetFiles();
                        for (IFile curFile : targetFiles) {
                            IContainer curPath = curFile.getParent();
                            if (curPath instanceof IFolder) {
                                createFolder((IFolder) curPath, true, true, null);
                            }
                            //GNU g++ does not delete the output file if compilation fails
                            if (curFile.exists()) {
                                curFile.delete(true, monitor);
                            }
                        }
                        String announcement = curRule.getTool().getAnnouncement();
                        if (!announcement.isEmpty()) {
                            announcement = announcement + NEWLINE;
                            buildRunnerHelper.getOutputStream().write(announcement.getBytes());
                        }
                        for (String curRecipe : curRule.getRecipes(buildRoot, autoData)) {
                            try {
                                if (launchCommand(curRecipe, autoData, monitor, buildRunnerHelper) != 0) {
                                    if (autoData.stopOnFirstBuildError()) {
                                        isError = true;
                                        break;
                                    }
                                }

                            } catch (@SuppressWarnings("unused") Exception e) {
                                isError = autoData.stopOnFirstBuildError();
                            }
                            epm.deDuplicate();

                            // bsMngr.setProjectBuildState(project, pBS);

                        }
                        if (isError) {
                            break;
                        }
                    }
                    if (kind == IncrementalProjectBuilder.AUTO_BUILD
                            && autoData.getAutoMakeTarget().equals(TARGET_OBJECTS)) {
                        lastSequenceID = true;
                    }
                } while (!(lastSequenceID || isError));
                //Run postBuildStep if existing
                String postBuildStep = autoData.getPostbuildStep();
                postBuildStep = resolve(postBuildStep, EMPTY_STRING, WHITESPACE, autoData);
                if (!postBuildStep.isEmpty()) {
                    String announcement = autoData.getPostBuildAnouncement();
                    if (!announcement.isEmpty()) {
                        announcement = announcement + NEWLINE;
                        buildRunnerHelper.getOutputStream().write(announcement.getBytes());
                    }
                    if (launchCommand(postBuildStep, autoData, monitor, buildRunnerHelper) != 0) {
                        return false;
                    }
                }
            }
            buildRunnerHelper.goodbye();

            // if (status != ICommandLauncher.ILLEGAL_COMMAND) {
            buildRunnerHelper.refreshProject(cfgName, parentMon.newChild(5));
            buildRunnerHelper.close();
        } catch (Exception e) {
            projectBuilder.forgetLastBuiltState();

            String msg = MessageFormat.format(ManagedMakeBuilder_message_error_build,
                    new String[] { project.getName(), configuration.getName() });
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e));
        }
        monitor.done();
        return false;
    }

    private int launchCommand(String curRecipe, AutoBuildConfigurationDescription autoData, IProgressMonitor monitor,
            AutoBuildRunnerHelper buildRunnerHelper) throws IOException {
        CommandLauncher launcher = new CommandLauncher();
        launcher.showCommand(true);
        String[] args = CommandLineUtil.argumentsToArray(curRecipe);
        IPath commandPath = new Path(args[0]);
        String[] onlyArgs = Arrays.copyOfRange(args, 1, args.length);

        String[] envp = BuildRunnerForMake.getEnvironment(autoData.getCdtConfigurationDescription(),
                autoData.getConfiguration().getBuilder().appendEnvironment());
        Process fProcess = null;
        try (OutputStream stdout = buildRunnerHelper.getOutputStream();
                OutputStream stderr = buildRunnerHelper.getErrorStream();) {
            try {
                fProcess = launcher.execute(commandPath, onlyArgs, envp, autoData.getBuildFolder().getLocation(),
                        monitor);
            } catch (CoreException e1) {
                // ignore and handle null case
            }
            if (fProcess == null) {
                String error = "Failed to execute" + NEWLINE + curRecipe + NEWLINE; //$NON-NLS-1$
                stdout.write(error.getBytes());
                return -999;
            }

            if (ICommandLauncher.OK != launcher.waitAndRead(stdout, stderr, monitor)) {
                if (autoData.stopOnFirstBuildError()) {
                    return -999;
                }
            }
            String fErrMsg = launcher.getErrorMessage();
            if (fErrMsg != null && !fErrMsg.isEmpty()) {
                printMessage(fErrMsg, stderr);
            }
        }
        return fProcess.exitValue();
    }

    @Override
    public String getName() {
        return Messages.InternalBuilderName;
    }

    @Override
    public boolean supportsParallelBuild() {
        return true;
    }

    @Override
    public boolean supportsStopOnError() {
        return true;
    }

    @Override
    public boolean supportsCustomCommand() {
        return false;
    }

    @Override
    public boolean supportsMakeFiles() {
        return false;
    }

    @Override
    public boolean supportsAutoBuild() {
        return true;
    }

    @Override
    public boolean supportsIncrementalBuild() {
        return true;
    }

    @Override
    public boolean supportsCleanBuild() {
        return true;
    }

    private void printMessage(String msg, OutputStream os) {
        if (os != null) {
            try {
                os.write((msg + NEWLINE).getBytes());
                os.flush();
            } catch (IOException e) {
                // ignore;
            }
        }

    }

}
