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

import static io.sloeber.autoBuild.integration.Const.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.cdt.utils.cdtvariables.CdtVariableResolver;
import org.eclipse.cdt.utils.cdtvariables.IVariableSubstitutor;
import org.eclipse.cdt.utils.cdtvariables.SupplierBasedCdtVariableSubstitutor;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.core.IConsoleParser;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.cdtvariables.CdtVariableException;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.internal.core.BuildRunnerHelper;
import org.eclipse.cdt.internal.core.cdtvariables.DefaultVariableContextInfo;
import org.eclipse.cdt.utils.CommandLineUtil;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.autoBuild.Internal.ManagedBuildManager;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IBuildRunner;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationData;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;

/**
 * @author dschaefer
 * @since 8.0
 */
public class BuildRunner extends IBuildRunner {
    private static final int PROGRESS_MONITOR_SCALE = 100;
    private static final int TICKS_STREAM_PROGRESS_MONITOR = 1 * PROGRESS_MONITOR_SCALE;
    private static final int TICKS_DELETE_MARKERS = 1 * PROGRESS_MONITOR_SCALE;
    private static final int TICKS_EXECUTE_COMMAND = 1 * PROGRESS_MONITOR_SCALE;
    private static final int TICKS_REFRESH_PROJECT = 1 * PROGRESS_MONITOR_SCALE;

    @Override
    public boolean invokeBuild(int kind, IProject project, ICConfigurationDescription icConfigurationDescription,
            IBuilder builder, IConsole console, IMarkerGenerator markerGenerator,
            IncrementalProjectBuilder projectBuilder, IProgressMonitor inMonitor) throws CoreException {

        boolean isClean = (kind == IncrementalProjectBuilder.CLEAN_BUILD);
        AutoBuildConfigurationData autoData = (AutoBuildConfigurationData) icConfigurationDescription
                .getConfigurationData();
        IConfiguration configuration = autoData.getConfiguration();

        BuildRunnerHelper buildRunnerHelper = new BuildRunnerHelper(project);
        try {
            IProgressMonitor monitor = inMonitor;
            if (monitor == null) {
                monitor = new NullProgressMonitor();
            }
            monitor.beginTask(
                    //TOFIX ManagedMakeMessages.getResourceString("MakeBuilder.Invoking_Make_Builder") + project.getName(), //$NON-NLS-1$
                    "ManagedMakeMessages.getResourceString (MakeBuilder.Invoking_Make_Builder)" + project.getName(), //$NON-NLS-1$
                    TICKS_STREAM_PROGRESS_MONITOR + TICKS_DELETE_MARKERS + TICKS_EXECUTE_COMMAND
                            + TICKS_REFRESH_PROJECT);

            String buildCommand = resolve(builder.getCommand(), icConfigurationDescription);
            if (!buildCommand.isBlank()) {
                String cfgName = configuration.getName();
                String toolchainName = configuration.getToolChain().getName();
                boolean isSupported = configuration.isSupported();

                ICommandLauncher launcher = builder.getCommandLauncher();

                String[] targets = getTargets(kind, builder, icConfigurationDescription);

                String[] args = getCommandArguments(builder, targets, icConfigurationDescription);

                IFolder buildFolder = ManagedBuildManager.getBuildFolder(configuration, project);

                Map<String, String> envMap = getEnvironment(icConfigurationDescription, builder);
                String[] envp = BuildRunnerHelper.envMapToEnvp(envMap);

                String[] errorParsers = builder.getErrorParsers();
                ErrorParserManager epm = new ErrorParserManager(project, buildFolder.getLocationURI(), markerGenerator,
                        errorParsers);

                List<IConsoleParser> parsers = new ArrayList<>();
                //                if (!isOnlyClean) {
                //                    ICConfigurationDescription cfgDescription = ManagedBuildManager
                //                            .getDescriptionForConfiguration(configuration);
                //                    ManagedBuildManager.collectLanguageSettingsConsoleParsers(cfgDescription, epm, parsers);
                //                    if (ScannerDiscoveryLegacySupport.isLegacyScannerDiscoveryOn(cfgDescription)) {
                //                        collectScannerInfoConsoleParsers(project, configuration, workingDirectoryURI, markerGenerator,
                //                                parsers);
                //                    }
                //                }

                buildRunnerHelper.setLaunchParameters(launcher, new Path(buildCommand), args,
                        buildFolder.getLocationURI(), envp);
                buildRunnerHelper.prepareStreams(epm, parsers, console, monitor);

                buildRunnerHelper.removeOldMarkers(project, monitor);

                buildRunnerHelper.greeting(kind, cfgName, toolchainName, isSupported);

                int state;
                epm.deferDeDuplication();
                try {
                    state = buildRunnerHelper.build(monitor);
                } finally {
                    epm.deDuplicate();
                }
                buildRunnerHelper.close();
                buildRunnerHelper.goodbye();

                if (state != ICommandLauncher.ILLEGAL_COMMAND) {
                    buildRunnerHelper.refreshProject(cfgName, monitor);
                }
            } else {
                //TODO 
                String msg = "ManagedMakeMessages.getFormattedString(\"ManagedMakeBuilder.message.undefined.build.command"; //$NON-NLS-1$
                //builder.getId()); 
                throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, new Exception()));
            }
        } catch (Exception e) {
            //TODO
            String msg = "ManagedMakeMessages.getFormattedString(\"ManagedMakeBuilder.message.error.build\")"; //$NON-NLS-1$
            //new String[] { project.getName(), configuration.getName() });
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e));
        } finally {
            try {
                buildRunnerHelper.close();
            } catch (IOException e) {
                Activator.log(e);
            }
            inMonitor.done();
        }
        return isClean;
    }

    private static String[] getCommandArguments(IBuilder builder, String[] targets,
            ICConfigurationDescription icConfigurationDescription) {
        String builderArguments = resolve(builder.getArguments(), icConfigurationDescription);
        String[] builderArgs = CommandLineUtil.argumentsToArray(builderArguments);
        String[] args = new String[targets.length + builderArgs.length];
        System.arraycopy(builderArgs, 0, args, 0, builderArgs.length);
        System.arraycopy(targets, 0, args, builderArgs.length, targets.length);
        return args;
    }

    private static String resolve(String unresolved, ICConfigurationDescription icConfigurationDescription) {
        DefaultVariableContextInfo contextInfo = new DefaultVariableContextInfo(
                DefaultVariableContextInfo.CONTEXT_CONFIGURATION, icConfigurationDescription);
        IVariableSubstitutor varSubs = new SupplierBasedCdtVariableSubstitutor(contextInfo, EMPTY_STRING, EMPTY_STRING);
        try {
            return CdtVariableResolver.resolveToString(unresolved, varSubs);
        } catch (CdtVariableException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return EMPTY_STRING;
        //        IEnvironmentVariableManager buildEnvironmentManger = CCorePlugin.getDefault().getBuildEnvironmentManager();
        //        IEnvironmentVariable var = buildEnvironmentManger.getVariable(buildArguments, icConfigurationDescription, true);
        //        if (var == null) {
        //            return EMPTY_STRING;
        //        }
        //        return var.getValue();
    }

    private static String[] getTargets(int kind, IBuilder builder,
            ICConfigurationDescription icConfigurationDescription) {
        String targets = EMPTY_STRING;
        switch (kind) {
        case IncrementalProjectBuilder.AUTO_BUILD:
            targets = resolve(builder.getAutoBuildTarget(), icConfigurationDescription);
            break;
        case IncrementalProjectBuilder.INCREMENTAL_BUILD: // now treated as the same!
        case IncrementalProjectBuilder.FULL_BUILD:
            targets = resolve(builder.getIncrementalBuildTarget(), icConfigurationDescription);
            break;
        case IncrementalProjectBuilder.CLEAN_BUILD:
            targets = resolve(builder.getCleanBuildTarget(), icConfigurationDescription);
            break;
        }

        String targetsArray[] = CommandLineUtil.argumentsToArray(targets);

        return targetsArray;
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

}

//    @Deprecated
//    protected static String[] getEnvStrings(Map<String, String> env) {
//        // Convert into env strings
//        List<String> strings = new ArrayList<>(env.size());
//        for (Entry<String, String> entry : env.entrySet()) {
//            StringBuilder buffer = new StringBuilder(entry.getKey());
//            buffer.append('=').append(entry.getValue());
//            strings.add(buffer.toString());
//        }
//
//        return strings.toArray(new String[strings.size()]);
//    }

//    private static void collectScannerInfoConsoleParsers(IProject project, IConfiguration cfg, URI workingDirectoryURI,
//            IMarkerGenerator markerGenerator, List<IConsoleParser> parsers) {
//        ICfgScannerConfigBuilderInfo2Set container = CfgScannerConfigProfileManager.getCfgScannerConfigBuildInfo(cfg);
//        Map<CfgInfoContext, IScannerConfigBuilderInfo2> map = container.getInfoMap();
//
//        String pathFromURI = EFSExtensionManager.getDefault().getPathFromURI(workingDirectoryURI);
//        if (pathFromURI == null) {
//            // fallback to CWD
//            pathFromURI = System.getProperty("user.dir"); //$NON-NLS-1$
//        }
//        IPath workingDirectory = new Path(pathFromURI);
//
//        int oldSize = parsers.size();
//
//        if (container.isPerRcTypeDiscovery()) {
//            for (IResourceInfo rcInfo : cfg.getResourceInfos()) {
//                ITool tools[];
//                if (rcInfo instanceof IFileInfo) {
//                    tools = ((IFileInfo) rcInfo).getToolsToInvoke();
//                } else {
//                    tools = ((IFolderInfo) rcInfo).getFilteredTools();
//                }
//                for (ITool tool : tools) {
//                    IInputType[] types = tool.getInputTypes();
//
//                    if (types.length != 0) {
//                        for (IInputType type : types) {
//                            CfgInfoContext context = new CfgInfoContext(rcInfo, tool, type);
//                            IScannerInfoConsoleParser parser = getScannerInfoConsoleParser(project, map, context,
//                                    workingDirectory, markerGenerator);
//                            if (parser != null) {
//                                parsers.add(parser);
//                            }
//                        }
//                    } else {
//                        CfgInfoContext context = new CfgInfoContext(rcInfo, tool, null);
//                        IScannerInfoConsoleParser parser = getScannerInfoConsoleParser(project, map, context,
//                                workingDirectory, markerGenerator);
//                        if (parser != null) {
//                            parsers.add(parser);
//                        }
//                    }
//                }
//            }
//        }
//
//        if (parsers.size() == oldSize) {
//            CfgInfoContext context = new CfgInfoContext(cfg);
//            IScannerInfoConsoleParser parser = getScannerInfoConsoleParser(project, map, context, workingDirectory,
//                    markerGenerator);
//            if (parser != null) {
//                parsers.add(parser);
//            }
//        }
//    }

//    private static IScannerInfoConsoleParser getScannerInfoConsoleParser(IProject project,
//            Map<CfgInfoContext, IScannerConfigBuilderInfo2> map, CfgInfoContext context, IPath workingDirectory,
//            IMarkerGenerator markerGenerator) {
//        return ScannerInfoConsoleParserFactory.getScannerInfoConsoleParser(project, context.toInfoContext(),
//                workingDirectory, map.get(context), markerGenerator, null);
//    }
