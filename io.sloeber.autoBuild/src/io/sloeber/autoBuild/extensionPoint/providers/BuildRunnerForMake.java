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

import static io.sloeber.autoBuild.core.Messages.*;
import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.*;

import java.io.IOException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.cdt.core.CommandLauncherManager;
import org.eclipse.cdt.core.ConsoleOutputStream;
import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.core.IConsoleParser;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.ProblemMarkerInfo;
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
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.autoBuild.api.AutoBuildCommon;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.api.IBuildRunner;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.autoBuild.internal.AutoBuildRunnerHelper;
import io.sloeber.autoBuild.schema.api.IBuilder;
import io.sloeber.autoBuild.schema.api.IConfiguration;

public class BuildRunnerForMake implements IBuildRunner {

	@Override
	public void invokeClean(int kind, IAutoBuildConfigurationDescription autoData,
			IMarkerGenerator markerGenerator, IConsole console, IProgressMonitor monitor) throws CoreException {
		autoData.deleteBuildFolder(monitor);

		//buildRoot.create(true, true, monitor);
//		for now I do not run make clean due to the fact rm -f is probably not on the system path
		//return invokeBuild(IncrementalProjectBuilder.CLEAN_BUILD, envp, autoData, markerGenerator, console, monitor);
	}

	@Override
	public boolean invokeBuild(int kind, String targetName,  IAutoBuildConfigurationDescription inAutoData,
			IMarkerGenerator markerGenerator, IConsole console, IProgressMonitor monitor) throws CoreException {

		AutoBuildConfigurationDescription autoData=(AutoBuildConfigurationDescription) inAutoData;
		IProject project = autoData.getProject();
		IBuilder builder = autoData.getBuilder();
		IConfiguration configuration = autoData.getConfiguration();
		ICConfigurationDescription confDesc = autoData.getCdtConfigurationDescription();

		if (autoData.generateMakeFilesAUtomatically()) {
			if (!generateMakeFiles(kind, autoData, console, monitor)) {
				return false;
			}
		}

		// get the make target
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
		default:
		case IncrementalProjectBuilder.INCREMENTAL_BUILD:
		case IncrementalProjectBuilder.FULL_BUILD: {
			defaultTarget = builder.getIncrementalBuildTarget();
			customTarget = autoData.getIncrementalMakeTarget();
			break;
		}
		}
		defaultTarget = AutoBuildCommon.resolve(defaultTarget, autoData);
		customTarget = AutoBuildCommon.resolve(customTarget, autoData);
		if (customTarget.isBlank()) {
			customTarget = defaultTarget;
		}
		if(targetName!=null) {
			customTarget=targetName;
		}
		for (String curArg : customTarget.split("\\s+")) { //$NON-NLS-1$
			args.add(curArg);
		}
		if (autoData.getCustomBuildArguments().isBlank()) {
			String builderArguments = builder.getArguments( autoData.getParallelizationNum(true),
					autoData.stopOnFirstBuildError());
			String resolvedBuilderArguments = AutoBuildCommon.resolve(builderArguments, autoData);
			args.addAll(Arrays.asList(CommandLineUtil.argumentsToArray(resolvedBuilderArguments)));
		} else {
			args.addAll(Arrays.asList(CommandLineUtil.argumentsToArray(autoData.getCustomBuildArguments())));
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
			ICommandLauncher launcher = CommandLauncherManager.getInstance().getCommandLauncher();
			IFolder buildFolder = autoData.getBuildFolder();
			URI buildFolderURI = buildFolder.getLocationURI();

			try (ErrorParserManager epm = new ErrorParserManager(project, buildFolderURI, markerGenerator,
					autoData.getErrorParserList());) {

				List<IConsoleParser> parsers = new ArrayList<>();
				if (!isClean) {
					AutoBuildManager.collectLanguageSettingsConsoleParsers(confDesc, epm, parsers);
				}

				buildRunnerHelper.setLaunchParameters(launcher, new Path(buildCommand),
						args.toArray(new String[args.size()]), buildFolderURI, autoData.getEnvironmentVariables());
				buildRunnerHelper.prepareStreams(epm, parsers, console, monitor);
				buildRunnerHelper.removeOldMarkers(project, monitor);
				buildRunnerHelper.greeting(kind, cfgName, configuration.getProjectType().getToolChain().getName(),
						configuration.isSupported());

				epm.deferDeDuplication();
				try {
					if (buildRunnerHelper.build(monitor) != 0) {
						epm.addProblemMarker(new ProblemMarkerInfo(project, 1, "Build Failed", //$NON-NLS-1$
								IMarkerGenerator.SEVERITY_ERROR_BUILD, null));

					}
				} finally {
					epm.deDuplicate();
				}
			}
			buildRunnerHelper.close();
			buildRunnerHelper.goodbye();

			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);

		} catch (Exception e) {
			String msg = MessageFormat.format(ManagedMakeBuilder_message_error_build, project.getName(),
					configuration.getName());
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e));
		}
		monitor.done();
		return isClean;
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
	private static boolean generateMakeFiles(int kind, AutoBuildConfigurationDescription autoData, IConsole console,
			IProgressMonitor monitor) throws CoreException {
		boolean canContinueBuilding = true;
		IProject project = autoData.getProject();
		MakefileGenerator generator =  new MakefileGenerator();
		generator.initialize(autoData);

		checkCancel(monitor);
		monitor.subTask(MessageFormat.format(ManagedMakeBuilder_message_update_makefiles, project.getName()));

		MultiStatus result = null;
		result = generator.localgenerateMakefiles(monitor);

		if (result.getCode() == IStatus.WARNING || result.getCode() == IStatus.INFO) {
			IStatus[] kids = result.getChildren();
			for (int index = 0; index < kids.length; ++index) {
				// One possibility is that there is nothing to build
				IStatus status = kids[index];
				if (status.getCode() == MakefileGenerator.NO_SOURCE_FOLDERS) {
					// performBuild = false;
					emitMessage(console, createNoSourceMessage(kind, status, autoData));
					canContinueBuilding = false;
					// break;

				} else {
					// Stick this in the list of stuff to warn the user about

					// TODO: messages.add(status);
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

		return canContinueBuilding;
	}

	/**
	 * Check whether the build has been canceled.
	 */
	private static void checkCancel(IProgressMonitor monitor) {
		if (monitor != null && monitor.isCanceled())
			throw new OperationCanceledException();
	}

	/*
	 * (non-javadoc) Emits a message to the console indicating that there were no
	 * source files to build
	 *
	 * @param buildType
	 *
	 * @param status
	 *
	 * @param configName
	 */
	private static String createNoSourceMessage(int buildType, IStatus status,
			AutoBuildConfigurationDescription autoData) {
		StringBuilder buf = new StringBuilder();
		String[] consoleHeader = new String[3];
		String configName = autoData.getCdtConfigurationDescription().getName();
		String projName = autoData.getProject().getName();
		if (buildType == IncrementalProjectBuilder.FULL_BUILD
				|| buildType == IncrementalProjectBuilder.INCREMENTAL_BUILD) {
			consoleHeader[0] = ManagedMakeBuider_type_incremental;
		} else {
			consoleHeader[0] = ""; //$NON-NLS-1$
			// outputError(projName, "The given build type is not supported in this
			// context"); //$NON-NLS-1$
		}
		consoleHeader[1] = configName;
		consoleHeader[2] = projName;
		buf.append(System.getProperty("line.separator", "\n")); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append(MessageFormat.format(ManagedMakeBuilder_message_console_header, consoleHeader[0], consoleHeader[1],
				consoleHeader[2]));
		buf.append(System.getProperty("line.separator", "\n")); //$NON-NLS-1$ //$NON-NLS-2$
		buf.append(System.getProperty("line.separator", "\n")); //$NON-NLS-1$ //$NON-NLS-2$
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
			e.printStackTrace();
			// Throw the exception back to the builder
			throw e;
		} catch (IOException io) { // Ignore console failures...
			io.printStackTrace();
			throw new CoreException(new Status(IStatus.ERROR, Activator.getId(), io.getLocalizedMessage(), io));
		}
	}

	@Override
	public String getName() {
		return ID;
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

//private static List<String> getMakeArguments(IBuilder builder, AutoBuildConfigurationDescription autoData) {
//String builderArguments = builder.getArguments(autoData.isParallelBuild(), autoData.getParallelizationNum(),
//      autoData.stopOnFirstBuildError());
//String resolvedBuilderArguments = AutoBuildCommon.resolve(builderArguments, autoData);
//return Arrays.asList(CommandLineUtil.argumentsToArray(resolvedBuilderArguments));
//}