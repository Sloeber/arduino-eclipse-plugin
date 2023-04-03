/*******************************************************************************
 * Copyright (c) 2010, 2018 Wind River Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Wind River Systems - Initial API and implementation
 *   James Blackburn (Broadcom Corp.)
 *   Andrew Gvozdev
 *   IBM Corporation
 *******************************************************************************/
package io.sloeber.autoBuild.extensionPoint.providers;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;
import static io.sloeber.autoBuild.core.Messages.*;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.core.IConsoleParser;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.ProblemMarkerInfo;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.utils.CommandLineUtil;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.autoBuild.Internal.AutoBuildRunnerHelper;
import io.sloeber.autoBuild.api.IBuildRunner;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.core.Messages;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;

public class BuildRunnerForMake extends IBuildRunner {

    @Override
    public boolean invokeBuild(int kind, AutoBuildConfigurationDescription autoData, IBuilder builder,
            IMarkerGenerator markerGenerator, IncrementalProjectBuilder projectBuilder, IConsole console,
            IProgressMonitor inMonitor) throws CoreException {
        IProgressMonitor monitor = inMonitor;
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        IProject project = autoData.getProject();
        IConfiguration configuration = autoData.getConfiguration();
        ICConfigurationDescription confDesc = autoData.getCdtConfigurationDescription();

        //get the make target 
        List<String> args = new LinkedList<>();
        boolean isClean = false;
        String defaultTarget = EMPTY_STRING;
        String customTarget = EMPTY_STRING;
        switch (kind) {
        case IncrementalProjectBuilder.AUTO_BUILD: {
            defaultTarget = builder.getAutoBuildTarget();
            customTarget = autoData.getAutoMakeTarget();
            break;
        }
        case IncrementalProjectBuilder.INCREMENTAL_BUILD:
        case IncrementalProjectBuilder.FULL_BUILD: {
            defaultTarget = builder.getIncrementalBuildTarget();
            customTarget = autoData.getIncrementalMakeTarget();
            break;
        }
        case IncrementalProjectBuilder.CLEAN_BUILD: {
            defaultTarget = builder.getCleanBuildTarget();
            customTarget = autoData.getCleanMakeTarget();
            isClean = true;
            break;
        }
        }
        defaultTarget = AutoBuildCommon.resolve(defaultTarget, autoData);
        customTarget = AutoBuildCommon.resolve(customTarget, autoData);
        if (customTarget.isBlank()) {
            args.add(defaultTarget);
        } else {
            args.add(customTarget);
        }

        // get the make command
        String buildCommand = AutoBuildCommon.resolve(autoData.getBuildCommand(false), autoData);
        if (buildCommand.isBlank()) {
            String msg = MessageFormat.format(ManagedMakeBuilder_message_undefined_build_command, builder.getId());
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, new Exception()));
        }

        try (AutoBuildRunnerHelper buildRunnerHelper = new AutoBuildRunnerHelper(project);) {

            monitor.beginTask(MakeBuilder_Invoking_Make_Builder + project.getName(), 4);

            String cfgName = confDesc.getName();
            ICommandLauncher launcher = builder.getCommandLauncher();
            args.addAll(getMakeArguments(kind, builder, autoData));
            IFolder buildFolder = autoData.getBuildFolder();
            URI buildFolderURI = buildFolder.getLocationURI();

            String[] envp = AutoBuildRunnerHelper.envMapToEnvp(getEnvironment(confDesc, builder));

            String[] errorParsers = autoData.getErrorParserList();
            ErrorParserManager epm = new ErrorParserManager(project, buildFolderURI, markerGenerator, errorParsers);

            List<IConsoleParser> parsers = new ArrayList<>();
            if (!isClean) {
                AutoBuildManager.collectLanguageSettingsConsoleParsers(confDesc, epm, parsers);
            }

            buildRunnerHelper.setLaunchParameters(launcher, new Path(buildCommand),
                    args.toArray(new String[args.size()]), buildFolderURI, envp);
            buildRunnerHelper.prepareStreams(epm, parsers, console, monitor);
            buildRunnerHelper.removeOldMarkers(project, monitor);
            buildRunnerHelper.greeting(kind, cfgName, configuration.getToolChain().getName(),
                    configuration.isSupported());

            int state;
            epm.deferDeDuplication();
            try {
                state = buildRunnerHelper.build(monitor);
                if (state != 0) {
                    epm.addProblemMarker(new ProblemMarkerInfo(project, 1, "Build Failed", //$NON-NLS-1$
                            IMarkerGenerator.SEVERITY_ERROR_BUILD, null));

                }
            } finally {
                epm.deDuplicate();
            }
            buildRunnerHelper.close();
            buildRunnerHelper.goodbye();

            project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

        } catch (Exception e) {
            String msg = MessageFormat.format(ManagedMakeBuilder_message_error_build,
                    new String[] { project.getName(), configuration.getName() });
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e));
        }
        monitor.done();
        return isClean;
    }

    private static List<String> getMakeArguments(int kind, IBuilder builder,
            AutoBuildConfigurationDescription autoData) {
        String builderArguments = builder.getArguments(autoData.isParallelBuild(), autoData.getParallelizationNum(),
                autoData.stopOnFirstBuildError());
        String resolvedBuilderArguments = AutoBuildCommon.resolve(builderArguments, autoData);
        return Arrays.asList(CommandLineUtil.argumentsToArray(resolvedBuilderArguments));
    }

    private static Map<String, String> getEnvironment(ICConfigurationDescription cfgDes, IBuilder builder) {
        Map<String, String> envMap = new HashMap<>();
        if (builder.appendEnvironment()) {
            IEnvironmentVariableManager mngr = CCorePlugin.getDefault().getBuildEnvironmentManager();
            IEnvironmentVariable[] vars = mngr.getVariables(cfgDes, true);
            for (IEnvironmentVariable var : vars) {
                envMap.put(var.getName(), var.getValue());
            }
        }

        return envMap;
    }

    @Override
    public String getName() {
        return Messages.ExternalBuilderName;
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
        return true;
    }

    @Override
    public boolean supportsMakeFiles() {
        return true;
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

}
