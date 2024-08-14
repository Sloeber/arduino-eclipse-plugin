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
package io.sloeber.autoBuild.extensionPoint.providers;

import static io.sloeber.autoBuild.api.AutoBuildCommon.*;
import static io.sloeber.autoBuild.core.Messages.*;
import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.*;

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.cdt.core.CommandLauncher;
import org.eclipse.cdt.core.ErrorParserManager;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.core.IConsoleParser;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.resources.IConsole;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.api.IAutoBuildMakeRule;
import io.sloeber.autoBuild.api.IBuildRunner;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.core.Messages;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.autoBuild.internal.AutoBuildRunnerHelper;
import io.sloeber.autoBuild.schema.api.IConfiguration;

public class InternalBuildRunner implements IBuildRunner {
	static public final String ID = Messages.InternalBuilderName;
	private static final int PROGRESS_MONITOR_SCALE = 100;
	private static final int TICKS_STREAM_PROGRESS_MONITOR = 1 * PROGRESS_MONITOR_SCALE;
	private static final int TICKS_DELETE_MARKERS = 1 * PROGRESS_MONITOR_SCALE;
	private static final int TICKS_EXECUTE_COMMAND = 1 * PROGRESS_MONITOR_SCALE;
	private static final int TICKS_REFRESH_PROJECT = 1 * PROGRESS_MONITOR_SCALE;
	private boolean myHasBuildError = false;

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
	public boolean invokeClean(int kind, IAutoBuildConfigurationDescription autoData, IMarkerGenerator markerGenerator,
			IConsole console, IProgressMonitor monitor) throws CoreException {
		IFolder buildRoot = autoData.getBuildFolder();
		//Do not delete the build folder as it may be in use with other processes (like discovery)
		for(IResource curMember:buildRoot.members()) {
			curMember.delete(true, monitor);
		}
//		buildRoot.delete(true, monitor);
//		buildRoot.create(true, true, monitor);
		return false;
	}

	@Override
	public boolean invokeBuild(int kind, String targetName, IAutoBuildConfigurationDescription inAutoData,
			IMarkerGenerator markerGenerator, IConsole console, IProgressMonitor monitor) throws CoreException {

		AutoBuildConfigurationDescription autoData = (AutoBuildConfigurationDescription) inAutoData;
		SubMonitor parentMon = SubMonitor.convert(monitor);
		IProject project = autoData.getProject();
		IConfiguration configuration = autoData.getConfiguration();
		ICConfigurationDescription cfgDescription = autoData.getCdtConfigurationDescription();
		IFolder buildRoot = autoData.getBuildFolder();

		// Generate the make Rules
		AutoBuildMakeRules myMakeRules = new AutoBuildMakeRules(autoData);

		try (AutoBuildRunnerHelper buildRunnerHelper = new AutoBuildRunnerHelper(project);
				ErrorParserManager epm = new ErrorParserManager(project, buildRoot.getLocationURI(), markerGenerator,
						autoData.getErrorParserList());) {

			monitor.beginTask("", TICKS_STREAM_PROGRESS_MONITOR + TICKS_DELETE_MARKERS + TICKS_EXECUTE_COMMAND //$NON-NLS-1$
					+ TICKS_REFRESH_PROJECT);

			// Prepare launch parameters for BuildRunnerHelper
			String cfgName = cfgDescription.getName();
			String toolchainName = autoData.getProjectType().getToolChain().getName();
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
				buildRunnerHelper.printLine(toolchainName);
				buildRunnerHelper.toConsole(ManagedMakeBuilder_message_internal_builder_header_note);

				int parrallelNum = autoData.getParallelizationNum(true);
				epm.deferDeDuplication();
				int sequenceID = -1;
				boolean lastSequenceID = true;

				// Run preBuildStep if existing
				TreeMap<String,String> preBuildSteps = autoData.getPrebuildSteps();
				for (Entry<String, String> preBuildStep:preBuildSteps.entrySet()) {
					String announcement = preBuildStep.getKey();
					String command = preBuildStep.getValue();

					if (!announcement.isEmpty()) {
						buildRunnerHelper.toConsole(announcement);
					}
					buildRunnerHelper.toConsole(command);
					if (launchCommand(command, autoData, monitor, buildRunnerHelper) != 0) {
						if (autoData.stopOnFirstBuildError()) {
							return false;
						}
					}
				}

				myHasBuildError = false;
				do {
					sequenceID++;
					lastSequenceID = true;

					ExecutorService executor = null;
					if (parrallelNum > 1) {
						executor = Executors.newFixedThreadPool(parrallelNum);
					}

					for (IAutoBuildMakeRule curRule : myMakeRules) {
						if (curRule.getSequenceGroupID() != sequenceID) {
							continue;
						}
						if (myHasBuildError) {
							continue;
						}
						lastSequenceID = false;
						//buildRunnerHelper.toConsole("Adding to executor " + curRule.getAnnouncement());
						if (!curRule.needsExecuting(buildRoot)) {
							buildRunnerHelper.toConsole(Messages.InternalBuildRunner_NoNeedToRun + curRule.getAnnouncement());
							continue;
						}

						// make sure the target folders exists
						// can not move this into RuleRunner as code locks
						Set<IFile> targetFiles = curRule.getTargetFiles();
						try {
							for (IFile curFile : targetFiles) {
								IContainer curPath = curFile.getParent();
								if (curPath instanceof IFolder) {
									createFolder((IFolder) curPath, true, true, null);
								}
								// GNU g++ does not delete the output file if compilation fails
								if (curFile.exists()) {
									curFile.delete(true, monitor);
								}
							}
						} catch (Exception e) {
							// don bother
							e.printStackTrace();
						}

						Runnable worker = new RuleRunner(curRule, autoData, monitor, buildRunnerHelper);
						if (executor != null) {
							executor.execute(worker);
						} else {
							worker.run();
						}

					}
					if (executor != null) {
						// This will make the executor accept no new threads
						// and finish all existing threads in the queue
						executor.shutdown();
						// Wait until all threads are finish
						executor.awaitTermination(20, TimeUnit.MINUTES);
					}
					epm.deDuplicate();

					if (kind == IncrementalProjectBuilder.AUTO_BUILD
							&& autoData.getAutoMakeTarget().equals(TARGET_OBJECTS)) {
						lastSequenceID = true;
					}
				} while (!(lastSequenceID || myHasBuildError));
				// Run postBuildStep if existing

				TreeMap<String,String> postBuildSteps = autoData.getPostbuildSteps();
				for (Entry<String, String> step:postBuildSteps.entrySet()) {
					String announcement = step.getKey();
					String command = step.getValue();

					if (!announcement.isEmpty()) {
						buildRunnerHelper.toConsole(announcement);
					}
					buildRunnerHelper.toConsole(command);
					if (launchCommand(command, autoData, monitor, buildRunnerHelper) != 0) {
						if (autoData.stopOnFirstBuildError()) {
							return false;
						}
					}
				}


				String postBuildStep = autoData.getPostbuildStep();
				postBuildStep = resolve(postBuildStep, EMPTY_STRING, WHITESPACE, autoData);
				if (!postBuildStep.isEmpty()) {
					String announcement = autoData.getPostBuildAnouncement();
					if (!announcement.isEmpty()) {
						buildRunnerHelper.toConsole(announcement);
					}
					buildRunnerHelper.toConsole(postBuildStep);
					if (launchCommand(postBuildStep, autoData, monitor, buildRunnerHelper) != 0) {
						return false;
					}
				}
			}
			buildRunnerHelper.goodbye();
			buildRunnerHelper.refreshProject(cfgName, parentMon.newChild(5));
			buildRunnerHelper.close();
		} catch (Exception e) {
			e.printStackTrace();
			String msg = MessageFormat.format(ManagedMakeBuilder_message_error_build,
					new Object[] { project.getName(), configuration.getName() });
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, msg, e));
		}
		monitor.done();
		return false;
	}

	private class RuleRunner implements Runnable {
		private IAutoBuildMakeRule myRule;
		private AutoBuildConfigurationDescription myAutoData;
		private IProgressMonitor myMonitor;
		private AutoBuildRunnerHelper myBuildRunnerHelper;

		RuleRunner(IAutoBuildMakeRule curRule, AutoBuildConfigurationDescription autoData, IProgressMonitor monitor,
				AutoBuildRunnerHelper buildRunnerHelper) {
			myRule = curRule;
			myAutoData = autoData;
			myMonitor = monitor;
			myBuildRunnerHelper = buildRunnerHelper;
		}

		@Override
		public void run() {
			try {
				myBuildRunnerHelper.toConsole(myRule.getAnnouncement());

				// run the actual build commands -called recipes
				for (String curRecipe : myRule.getRecipes(myAutoData.getBuildFolder(), myAutoData)) {
					myBuildRunnerHelper.toConsole(curRecipe);
					if (launchCommand(curRecipe, myAutoData, myMonitor, myBuildRunnerHelper) != 0) {
						if (myAutoData.stopOnFirstBuildError()) {
							reportBuildError();
							break;
						}
					}

				}
			} catch (IOException e) {
				e.printStackTrace();
				reportBuildError();

			}
		}
	}

	private void reportBuildError() {
		myHasBuildError = true;
	}

	public static int launchCommand(String curRecipe, IAutoBuildConfigurationDescription autoData,
			IProgressMonitor monitor, AutoBuildRunnerHelper buildRunnerHelper) throws IOException {
		CommandLauncher launcher = new CommandLauncher();
		launcher.showCommand(false);
		String[] args = argumentsToArray(curRecipe);
		IPath commandPath = new Path(args[0]);
		String[] onlyArgs = Arrays.copyOfRange(args, 1, args.length);

		Process fProcess = null;
		try (OutputStream stdout = buildRunnerHelper.getOutputStream();
				OutputStream stderr = buildRunnerHelper.getErrorStream();) {
			try {
				fProcess = launcher.execute(commandPath, onlyArgs, autoData.getEnvironmentVariables(),
						autoData.getBuildFolder().getLocation(), monitor);
			} catch ( CoreException e1) {
				e1.printStackTrace();
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

	private static void printMessage(String msg, OutputStream os) {
		if (os != null) {
			try {
				os.write((msg + NEWLINE).getBytes());
				os.flush();
			} catch ( IOException e) {
				e.printStackTrace();
				// ignore;
			}
		}

	}

	public static String[] argumentsToArray(String line) {
		if (isWindows) {
			return argumentsToArrayWindowsStyle(line);
		}
		return argumentsToArrayUnixStyle(line);
	}

	/**
	 * Parsing arguments in a shell style. i.e.
	 *
	 * <pre>
	 * ["a b c" d] -&gt; [[a b c],[d]]
	 * [a   d] -&gt; [[a],[d]]
	 * ['"quoted"'] -&gt; [["quoted"]]
	 * [\\ \" \a] -&gt; [[\],["],[a]]
	 * ["str\\str\a"] -&gt; [[str\str\a]]
	 * </pre>
	 *
	 * @param line
	 * @return array of arguments, or empty array if line is null or empty
	 */
	@SuppressWarnings("incomplete-switch")
	public static String[] argumentsToArrayUnixStyle(String line) {
		final int INITIAL = 0;
		final int IN_DOUBLE_QUOTES = 1;
		final int IN_DOUBLE_QUOTES_ESCAPED = 2;
		final int ESCAPED = 3;
		final int IN_SINGLE_QUOTES = 4;
		final int IN_ARG = 5;

		if (line == null) {
			return new String[0];
		}

		char[] array = line.trim().toCharArray();
		ArrayList<String> aList = new ArrayList<>();
		StringBuilder buffer = new StringBuilder();
		int state = INITIAL;
		for (int i = 0; i < array.length; i++) {
			char c = array[i];

			switch (state) {
			case IN_ARG:
				// fall through
			case INITIAL:
				if (Character.isWhitespace(c)) {
					if (state == INITIAL)
						break; // ignore extra spaces
					// add argument
					state = INITIAL;
					String arg = buffer.toString();
					buffer = new StringBuilder();
					aList.add(arg);
				} else {
					switch (c) {
					case '\\':
						state = ESCAPED;
						break;
					case '\'':
						state = IN_SINGLE_QUOTES;
						break;
					case '\"':
						state = IN_DOUBLE_QUOTES;
						break;
					default:
						state = IN_ARG;
						buffer.append(c);
						break;
					}
				}
				break;
			case IN_DOUBLE_QUOTES:
				switch (c) {
				case '\\':
					state = IN_DOUBLE_QUOTES_ESCAPED;
					break;
				case '\"':
					state = IN_ARG;
					break;
				default:
					buffer.append(c);
					break;
				}
				break;
			case IN_SINGLE_QUOTES:
				switch (c) {
				case '\'':
					state = IN_ARG;
					break;
				default:
					buffer.append(c);
					break;
				}
				break;
			case IN_DOUBLE_QUOTES_ESCAPED:
				switch (c) {
				case '\"':
				case '\\':
					buffer.append(c);
					break;
				case 'n':
					buffer.append("\n"); //$NON-NLS-1$
					break;
				default:
					buffer.append('\\');
					buffer.append(c);
					break;
				}
				state = IN_DOUBLE_QUOTES;
				break;
			case ESCAPED:
				buffer.append(c);
				state = IN_ARG;
				break;
			}
		}

		if (state != INITIAL) { // this allow to process empty string as an
								// argument
			aList.add(buffer.toString());
		}
		return aList.toArray(new String[aList.size()]);
	}

	/**
	 * Parsing arguments in a cmd style. i.e.
	 *
	 * <pre>
	 * ["a b c" d] -&gt; [[a b c],[d]]
	 * [a   d] -&gt; [[a],[d]]
	 * ['"quoted"'] -&gt; [['quoted']]
	 * [\\ \" \a] -&gt; [[\\],["],[\a]]
	 * ["str\\str\a"] -&gt; [[str\\str\a]]
	 * </pre>
	 *
	 * @param line
	 * @return array of arguments, or empty array if line is null or empty
	 */
	@SuppressWarnings("incomplete-switch")
	public static String[] argumentsToArrayWindowsStyle(String line) {
		final int OUTSIDE_ARGS = 0;
		final int IN_DOUBLE_QUOTES = 1;
		final int IN_SINGLE_QUOTES =3;
		final int IN_ARG = 5;
		boolean ESCAPED = false;

		if (line == null || line.isBlank()) {
			return new String[0];
		}

		ArrayList<String> retList = new ArrayList<>();
		StringBuilder buffer = new StringBuilder();
		int state = OUTSIDE_ARGS;
		for (char curChar:  line.trim().toCharArray()) {
			if(ESCAPED) {
				ESCAPED=false;
				buffer.append(curChar);
				continue;
			}
			switch (state) {
			case IN_ARG:
				// fall through
			case OUTSIDE_ARGS:
				if (Character.isWhitespace(curChar)) {
					if (state == OUTSIDE_ARGS)
						break; // ignore extra spaces
					// add argument
					state = OUTSIDE_ARGS;
					retList.add(buffer.toString());
					buffer = new StringBuilder();
				} else {
					state = IN_ARG;
					switch (curChar) {
					case '\\':
						buffer.append(curChar);
						 ESCAPED=true;
						break;
					case '\"':
						state = IN_DOUBLE_QUOTES;
						buffer.append(curChar);
						break;
					case '\'':
						state = IN_SINGLE_QUOTES;
						break;
					default:
						buffer.append(curChar);
						break;
					}
				}
				break;
			case IN_DOUBLE_QUOTES:
				switch (curChar) {
				case '\\':
					ESCAPED=true;
					buffer.append(curChar);
					break;
				case '\"':
					buffer.append(curChar);
					state = IN_ARG;
					break;
				default:
					buffer.append(curChar);
					break;
				}
				break;
			case IN_SINGLE_QUOTES:
				switch (curChar) {
				case '\'':
					state = IN_ARG;
					break;
				default:
					buffer.append(curChar);
					break;
				}
				break;
			}
		}

		if (state != OUTSIDE_ARGS) { // this allow to process empty string as an
								// argument
			retList.add(buffer.toString());
		}
		return retList.toArray(new String[retList.size()]);
	}

}
