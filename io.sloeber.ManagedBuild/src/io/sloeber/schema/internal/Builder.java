/*******************************************************************************
 * Copyright (c) 2004, 2020 Intel Corporation and others.
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
 * James Blackburn (Broadcom Corp.)
 *******************************************************************************/
package io.sloeber.schema.internal;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.CommandLauncherManager;
import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.core.settings.model.COutputEntry;
import org.eclipse.cdt.core.settings.model.ICOutputEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IBuildRunner;
import io.sloeber.autoBuild.extensionPoint.IMakefileGenerator;
import io.sloeber.autoBuild.extensionPoint.IReservedMacroNameSupplier;
import io.sloeber.autoBuild.extensionPoint.providers.BuildRunner;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IOptions;
import io.sloeber.schema.api.IToolChain;

public class Builder extends SchemaObject implements IBuilder {
    public static final int UNLIMITED_JOBS = Integer.MAX_VALUE;
    private static final String EMPTY_STRING = ""; //$NON-NLS-1$

    static final String VALUE_UNLIMITED = "unlimited"; //$NON-NLS-1$
    static final String PARALLEL_PATTERN_NUM = "*"; //$NON-NLS-1$
    static final String PARALLEL_PATTERN_NUM_START = "["; //$NON-NLS-1$
    static final String PARALLEL_PATTERN_NUM_END = "]"; //$NON-NLS-1$
    static final String VALUE_OPTIMAL = "optimal"; //$NON-NLS-1$
    static final String DEFAULT_TARGET_CLEAN = "clean"; //$NON-NLS-1$
    static final String DEFAULT_TARGET_INCREMENTAL = "all"; //$NON-NLS-1$
    static final String DEFAULT_TARGET_AUTO = "all"; //$NON-NLS-1$

    String[] modelsAbstract;
    String[] modelcommand;
    String[] modelarguments;
    String[] modelbuildfileGenerator;
    String[] modelErrorParsers;
    String[] modelvariableFormat;
    String[] modelreservedMacroNames;
    String[] modelReservedMacroNameSupplier;
    String[] modelautoBuildTarget;
    String[] modelincrementalBuildTarget;
    String[] modelcleanBuildTarget;
    String[] modelignoreErrCmd;
    String[] modelparallelBuildCmd;
    String[] modelparallelBuildOn;
    String[] modelparallelizationNumber;
    String[] modelisSystem;
    String[] modelcommandLauncher;
    String[] modelbuildRunner;

    //  Parent and children
    private IToolChain parent;
    private boolean isAbstract;
    private boolean isParallelBuildEnabled;
    private String[] reservedMacroNames;
    private ICommandLauncher fCommandLauncher;
    private IReservedMacroNameSupplier reservedMacroNameSupplier;
    private IBuildRunner fBuildRunner = null;
    private IMakefileGenerator myMakeFileGenerator;
    private int parallelNumberAttribute; // negative number denotes "optimal" value, see getOptimalParallelJobNum()

    private Options myOptionMap = new Options();

    /**
     * This constructor is called to create a builder defined by an extension point
     * in
     * a plugin manifest file, or returned by a dynamic element provider
     *
     * @param parent
     *            The IToolChain parent of this builder, or <code>null</code> if
     *            defined at the top level
     * @param element
     *            The builder definition from the manifest file or a dynamic element
     *            provider
     * @param managedBuildRevision
     *            The fileVersion of Managed Buid System
     */
    public Builder(IToolChain parent, IExtensionPoint root, IConfigurationElement element) {
        this.parent = parent;
        loadNameAndID(root, element);

        modelsAbstract = getAttributes(IS_ABSTRACT);
        modelcommand = getAttributes(COMMAND);
        modelarguments = getAttributes(ARGUMENTS);
        modelbuildfileGenerator = getAttributes(MAKEGEN_ID);
        modelErrorParsers = getAttributes(ERROR_PARSERS);

        modelvariableFormat = getAttributes(VARIABLE_FORMAT);
        modelreservedMacroNames = getAttributes(RESERVED_MACRO_NAMES);
        modelReservedMacroNameSupplier = getAttributes(RESERVED_MACRO_NAME_SUPPLIER);
        modelautoBuildTarget = getAttributes(ATTRIBUTE_TARGET_AUTO);
        modelincrementalBuildTarget = getAttributes(ATTRIBUTE_TARGET_INCREMENTAL);
        modelcleanBuildTarget = getAttributes(ATTRIBUTE_TARGET_CLEAN);
        modelignoreErrCmd = getAttributes(ATTRIBUTE_IGNORE_ERR_CMD);
        modelparallelBuildCmd = getAttributes(ATTRIBUTE_PARALLEL_BUILD_CMD);
        modelparallelBuildOn = getAttributes(ATTRIBUTE_PARALLEL_BUILD_ON);
        modelparallelizationNumber = getAttributes(ATTRIBUTE_PARALLELIZATION_NUMBER);
        modelisSystem = getAttributes(IS_SYSTEM);
        modelcommandLauncher = getAttributes(ATTRIBUTE_COMMAND_LAUNCHER);
        modelbuildRunner = getAttributes(ATTRIBUTE_BUILD_RUNNER);

        IConfigurationElement[] optionElements = element.getChildren(IOptions.OPTION);
        for (IConfigurationElement optionElement : optionElements) {
            myOptionMap.add(new Option(this, root, optionElement));
        }

        myMakeFileGenerator = (IMakefileGenerator) createExecutableExtension(MAKEGEN_ID);
        reservedMacroNameSupplier = (IReservedMacroNameSupplier) createExecutableExtension(
                RESERVED_MACRO_NAME_SUPPLIER);

        fCommandLauncher = (ICommandLauncher) createExecutableExtension(ATTRIBUTE_COMMAND_LAUNCHER);
        if (fCommandLauncher == null) {
            fCommandLauncher = CommandLauncherManager.getInstance().getCommandLauncher();
        }
        fBuildRunner = (IBuildRunner) createExecutableExtension(ATTRIBUTE_BUILD_RUNNER);
        if (fBuildRunner == null) {
            fBuildRunner = new BuildRunner();
        }

        resolveFields();

    }

    private void resolveFields() {
        if (myName.isBlank()) {
            myName = getId();
        }
        reservedMacroNames = modelreservedMacroNames[SUPER].split(","); //$NON-NLS-1$
        isAbstract = Boolean.parseBoolean(modelsAbstract[ORIGINAL]);

        isParallelBuildEnabled = Boolean.parseBoolean(modelparallelBuildOn[SUPER]);
        if (isParallelBuildEnabled) {
            setParallelizationNumAttribute(modelparallelizationNumber[SUPER]);
        }
        //  isTest = Boolean.parseBoolean(modelisSystem[SUPER]);

        if (!modelarguments[SUPER].isBlank()) {
            String stopOnErrCmd = getStopOnErrCmd(isStopOnError());
            int parallelNum = getParallelizationNum();
            String parallelCmd = isParallelBuildOn() ? getParallelizationCmd(parallelNum) : EMPTY_STRING;

            String reversedStopOnErrCmd = getStopOnErrCmd(!isStopOnError());
            String reversedParallelBuildCmd = !isParallelBuildOn() ? getParallelizationCmd(parallelNum) : EMPTY_STRING;

            String args = removeCmd(modelarguments[SUPER], reversedStopOnErrCmd);
            args = removeCmd(args, reversedParallelBuildCmd);

            args = addCmd(args, stopOnErrCmd);
            args = addCmd(args, parallelCmd);
            modelarguments[SUPER] = args.trim();
        }

        if (modelcommand[SUPER].isBlank()) {
            modelcommand[SUPER] = "make"; //$NON-NLS-1$
        }
        if (modelcleanBuildTarget[SUPER].isBlank()) {
            modelcleanBuildTarget[SUPER] = DEFAULT_TARGET_CLEAN;
        }
        if (modelincrementalBuildTarget[SUPER].isBlank()) {
            modelincrementalBuildTarget[SUPER] = DEFAULT_TARGET_INCREMENTAL;
        }

    }

    //  
    /*
     *  P A R E N T   A N D   C H I L D   H A N D L I N G
     */

    @Override
    public IToolChain getParent() {
        return parent;
    }

    @Override
    public boolean isAbstract() {
        return isAbstract;
    }

    @Override
    public String getCommand() {
        return modelcommand[SUPER];
    }

    @Override
    public String getArguments() {
        return modelarguments[SUPER];
    }

    private static String addCmd(String args, String cmd) {
        if (cmd.isBlank())
            return args.trim();

        if (args.isBlank())
            return cmd.trim();

        if (args.contains(cmd)) {
            return args.trim();
        }
        return args.trim() + BLANK + cmd.trim();
    }

    private static String removeCmd(String args, String cmd) {
        String ret = args.replace(BLANK + cmd + BLANK, BLANK);
        ret = ret.replace(cmd + BLANK, EMPTY_STRING);
        ret = ret.replace(BLANK + cmd, EMPTY_STRING);
        return ret.trim();
    }

    public String getParallelizationCmd(int num) {
        String pattern = getParrallelBuildCmd();
        if (pattern.length() == 0 || num == 0) {
            return EMPTY_STRING;
        }
        // "unlimited" number of jobs results in not adding the number to parallelization cmd
        // that behavior corresponds that of "make" flag "-j".
        return processParallelPattern(pattern, num == UNLIMITED_JOBS, num);
    }

    /**
     * This method turns the supplied pattern to parallelization command
     *
     * It supports 2 kinds of pattern where "*" is replaced with number of jobs:
     * <li>Pattern 1 (supports "<b>-j*</b>"): "text*text" -> "text#text"</li>
     * <li>Pattern 2 (supports "<b>-[j*]</b>"): "text[text*text]text" ->
     * "texttext#texttext</li>
     * <br>
     * Where # is num or empty if {@code empty} is {@code true})
     */
    private static String processParallelPattern(String pattern, boolean empty, int num) {
        Assert.isTrue(num > 0);

        int start = pattern.indexOf(PARALLEL_PATTERN_NUM_START);
        int end = -1;
        boolean hasStartChar = false;
        String result;
        if (start != -1) {
            end = pattern.indexOf(PARALLEL_PATTERN_NUM_END);
            if (end != -1) {
                hasStartChar = true;
            } else {
                start = -1;
            }
        }
        if (start == -1) {
            start = pattern.indexOf(PARALLEL_PATTERN_NUM);
            if (start != -1) {
                end = start + PARALLEL_PATTERN_NUM.length();
            }
        }
        if (start == -1) {
            result = pattern;
        } else {
            String prefix;
            String suffix;
            String numStr;
            prefix = pattern.substring(0, start);
            suffix = pattern.substring(end);
            numStr = pattern.substring(start, end);
            if (empty) {
                result = prefix + suffix;
            } else {
                String resolvedNum;
                if (hasStartChar) {
                    String numPrefix, numSuffix;
                    numStr = numStr.substring(0, PARALLEL_PATTERN_NUM_START.length());
                    numStr = numStr.substring(numStr.length() - PARALLEL_PATTERN_NUM_END.length());
                    int numStart = pattern.indexOf(PARALLEL_PATTERN_NUM);
                    if (numStart != -1) {
                        int numEnd = numStart + PARALLEL_PATTERN_NUM.length();
                        numPrefix = numStr.substring(0, numStart);
                        numSuffix = numStr.substring(numEnd);
                        resolvedNum = numPrefix + Integer.toString(num) + numSuffix;
                    } else {
                        resolvedNum = EMPTY_STRING;
                    }
                } else {
                    resolvedNum = Integer.toString(num);
                }
                result = prefix + resolvedNum + suffix;
            }
        }
        return result;
    }

    @Override
    public Set<String> getErrorParserList() {
        Set<String> errorParsers = new HashSet<>();
        String parseArray[] = modelErrorParsers[SUPER].split(Pattern.quote(SEMICOLON));
        errorParsers.addAll(Arrays.asList(parseArray));
        return errorParsers;

    }

    @Override
    public IMakefileGenerator getBuildFileGenerator() {
        return myMakeFileGenerator;
        //return new GnuMakefileGenerator();
    }

    @Override
    public String[] getReservedMacroNames() {
        return reservedMacroNames;
    }

    @Override
    public IReservedMacroNameSupplier getReservedMacroNameSupplier() {
        return reservedMacroNameSupplier;
    }

    @Override
    public String[] getErrorParsers() {
        return new String[0];
    }

    @Override
    public boolean isStopOnError() {
        return true;
    }

    @Override
    public String getAutoBuildTarget() {
        return modelautoBuildTarget[SUPER];
    }

    @Override
    public String getCleanBuildTarget() {
        return modelcleanBuildTarget[SUPER];
    }

    @Override
    public String getIncrementalBuildTarget() {
        return modelincrementalBuildTarget[SUPER];
    }

    @Override
    public boolean isAutoBuildEnable() {
        return true;
    }

    public static String[] toBuildAttributes(String name) {

        //        if (ATTRIBUTE_TARGET_INCREMENTAL.equals(name)) {
        //            return new String[] { BUILD_TARGET_INCREMENTAL, BuilderFactory.BUILD_TARGET_INCREMENTAL, BUILD_TARGET_FULL,
        //                    BuilderFactory.BUILD_TARGET_FULL };
        //        } else if (ATTRIBUTE_TARGET_AUTO.equals(name)) {
        //            return new String[] { BUILD_TARGET_AUTO, BuilderFactory.BUILD_TARGET_AUTO };
        //        } else if (ATTRIBUTE_TARGET_CLEAN.equals(name)) {
        //            return new String[] { BUILD_TARGET_CLEAN, BuilderFactory.BUILD_TARGET_CLEAN };
        //        } else if (ATTRIBUTE_BUILD_PATH.equals(name)) {
        //            return new String[] { BUILD_LOCATION, BuilderFactory.BUILD_LOCATION };
        //        } else if (COMMAND.equals(name)) {
        //            return new String[] { BUILD_COMMAND, BuilderFactory.BUILD_COMMAND };
        //        } else if (ARGUMENTS.equals(name)) {
        //            return new String[] { BUILD_ARGUMENTS, BuilderFactory.BUILD_ARGUMENTS };
        //        } else if (ATTRIBUTE_STOP_ON_ERR.equals(name)) {
        //            return new String[] { BuilderFactory.STOP_ON_ERROR };
        //        } //TODO else if(BuilderFactory.USE_DEFAULT_BUILD_CMD.equals(name)){
        //          //	return getCommand();
        //          //}
        //        else if (ATTRIBUTE_INCREMENTAL_ENABLED.equals(name)) {
        //            return new String[] { BuilderFactory.BUILD_INCREMENTAL_ENABLED, BuilderFactory.BUILD_FULL_ENABLED };
        //        } else if (ATTRIBUTE_CLEAN_ENABLED.equals(name)) {
        //            return new String[] { BuilderFactory.BUILD_CLEAN_ENABLED };
        //        } else if (ATTRIBUTE_AUTO_ENABLED.equals(name)) {
        //            return new String[] { BuilderFactory.BUILD_AUTO_ENABLED };
        //        } else if (ATTRIBUTE_ENVIRONMENT.equals(name)) {
        //            return new String[] { BuilderFactory.ENVIRONMENT };
        //        } else if (ATTRIBUTE_APPEND_ENVIRONMENT.equals(name)) {
        //            return new String[] { BuilderFactory.BUILD_APPEND_ENVIRONMENT };
        //        } else if (ATTRIBUTE_CUSTOMIZED_ERROR_PARSERS.equals(name)) {
        //            return new String[] { ErrorParserManager.PREF_ERROR_PARSER };
        //        }

        return new String[0];
    }

    public static String toBuilderAttribute(String name) {

        //        if (BUILD_TARGET_INCREMENTAL.equals(name) || BuilderFactory.BUILD_TARGET_INCREMENTAL.equals(name)
        //                || BUILD_TARGET_FULL.equals(name) || BuilderFactory.BUILD_TARGET_FULL.equals(name)) {
        //            return ATTRIBUTE_TARGET_INCREMENTAL;
        //        } else if (BUILD_TARGET_AUTO.equals(name) || BuilderFactory.BUILD_TARGET_AUTO.equals(name)) {
        //            return ATTRIBUTE_TARGET_AUTO;
        //        } else if (BUILD_TARGET_CLEAN.equals(name) || BuilderFactory.BUILD_TARGET_CLEAN.equals(name)) {
        //            return ATTRIBUTE_TARGET_CLEAN;
        //        } else if (BUILD_LOCATION.equals(name) || BuilderFactory.BUILD_LOCATION.equals(name)) {
        //            return ATTRIBUTE_BUILD_PATH;
        //        } else if (BUILD_COMMAND.equals(name) || BuilderFactory.BUILD_COMMAND.equals(name)) {
        //            return COMMAND;
        //        } else if (BUILD_ARGUMENTS.equals(name) || BuilderFactory.BUILD_ARGUMENTS.equals(name)) {
        //            return ARGUMENTS;
        //        } else if (BuilderFactory.STOP_ON_ERROR.equals(name)) {
        //            return ATTRIBUTE_STOP_ON_ERR;
        //        } //TO DO else if(BuilderFactory.USE_DEFAULT_BUILD_CMD.equals(name)){
        //          //	return getCommand();
        //          //}
        //        else if (BuilderFactory.BUILD_INCREMENTAL_ENABLED.equals(name)
        //                || BuilderFactory.BUILD_FULL_ENABLED.equals(name)) {
        //            return ATTRIBUTE_INCREMENTAL_ENABLED;
        //        } else if (BuilderFactory.BUILD_CLEAN_ENABLED.equals(name)) {
        //            return ATTRIBUTE_CLEAN_ENABLED;
        //        } else if (BuilderFactory.BUILD_AUTO_ENABLED.equals(name)) {
        //            return ATTRIBUTE_AUTO_ENABLED;
        //        } else if (BuilderFactory.ENVIRONMENT.equals(name)) {
        //            return ATTRIBUTE_ENVIRONMENT;
        //        } else if (BuilderFactory.BUILD_APPEND_ENVIRONMENT.equals(name)) {
        //            return ATTRIBUTE_APPEND_ENVIRONMENT;
        //        } else if (ErrorParserManager.PREF_ERROR_PARSER.equals(name)) {
        //            return ATTRIBUTE_CUSTOMIZED_ERROR_PARSERS;
        //        }
        return null;
    }

    @Override
    public boolean isCustomBuilder() {
        if (getParent().getBuilder() != this)
            return true;
        return false;
    }

    public IConfiguration getConfguration() {
        if (getParent() != null)
            return getParent().getParent();
        return null;
    }

    @Override
    public boolean supportsBuild(boolean managed) {
        return false;
    }

    /**
     * Returns the optimal number of parallel jobs.
     * The number is the number of available processors on the machine.
     *
     * The function never returns number smaller than 1.
     */
    public int getOptimalParallelJobNum() {
        // Bug 398426: On my Mac running parallel builds at full tilt hangs the desktop.
        // Need to pull it back one.
        int j = Runtime.getRuntime().availableProcessors();
        if (j > 1 && Platform.getOS().equals(Platform.OS_MACOSX))
            return j - 1;
        return j;
    }

    /**
     * Returns the internal representation of maximum number of parallel jobs
     * to be used for a build.
     * Note that negative number represents "optimal" value.
     *
     * The value of the number is encoded as follows:
     * 
     * <pre>
     *  Status       Returns
     * No parallel      1
     * Optimal       -CPU# (negative number of processors)
     * Specific        >0  (positive number)
     * Unlimited    Builder.UNLIMITED_JOBS
     * </pre>
     */
    public int getParallelizationNumAttribute() {
        if (!isParallelBuildOn())
            return 1;
        return parallelNumberAttribute;
    }

    private void setParallelizationNumAttribute(String parallelNumberString) {
        int parallelNumber = -1;
        if (VALUE_OPTIMAL.equals(parallelNumberString)) {
            parallelNumber = -getOptimalParallelJobNum();
        } else if (VALUE_UNLIMITED.equals(parallelNumberString)) {
            parallelNumber = UNLIMITED_JOBS;
        } else {
            try {
                parallelNumber = Integer.parseInt(parallelNumberString);
                if (parallelNumber <= 0) {
                    // unlimited for External Builder
                    parallelNumber = UNLIMITED_JOBS;
                }
            } catch (NumberFormatException e) {
                Activator.log(e);
                // default to "optimal" if not recognized
                parallelNumber = -getOptimalParallelJobNum();
            }
        }

        isParallelBuildEnabled = (parallelNumber != 1);
        if (parallelNumber > 0) {
            parallelNumberAttribute = parallelNumber;
        } else {
            // "optimal"
            parallelNumberAttribute = -getOptimalParallelJobNum();
        }
    }

    @Override
    public int getParallelizationNum() {
        return Math.abs(getParallelizationNumAttribute());
    }

    @Override
    public boolean supportsParallelBuild() {
        return getParrallelBuildCmd().length() != 0;
    }

    @Override
    public boolean supportsStopOnError(boolean on) {

        if (!on)
            return getIgnoreErrCmdAttribute().length() != 0;
        return true;
    }

    public String getStopOnErrCmd(boolean stop) {
        if (!stop)
            return getIgnoreErrCmdAttribute();
        return EMPTY_STRING;
    }

    public String getIgnoreErrCmdAttribute() {
        return modelignoreErrCmd[SUPER];
    }

    public String getParrallelBuildCmd() {
        return modelparallelBuildCmd[SUPER];
    }

    @Override
    public boolean isParallelBuildOn() {
        return isParallelBuildEnabled;
    }

    public ICOutputEntry[] getOutputEntries(IProject project) {
        return getDefaultOutputSettings(project);
    }

    private ICOutputEntry[] getDefaultOutputSettings(IProject project) {
        Configuration cfg = (Configuration) getConfguration();
        if (cfg == null || cfg.isPreference()) {
            return new ICOutputEntry[] {
                    new COutputEntry(Path.EMPTY, null, ICSettingEntry.VALUE_WORKSPACE_PATH | ICSettingEntry.RESOLVED) };
        }

        //IFolder BuildFolder = ManagedBuildManager.getBuildFolder(cfg, this);
        IFolder BuildFolder = project.getFolder(cfg.getName());
        return new ICOutputEntry[] {
                new COutputEntry(BuildFolder, null, ICSettingEntry.VALUE_WORKSPACE_PATH | ICSettingEntry.RESOLVED) };
    }

    @Override
    public String toString() {

        //        String version1 = ManagedBuildManager.getVersionFromIdAndVersion(getId());
        //        if (version1 != null) {
        //            StringBuilder buf = new StringBuilder();
        //            buf.append(myName);
        //            buf.append(" (v").append(version1).append(")"); //$NON-NLS-1$ //$NON-NLS-2$
        //            return buf.toString();
        //        }
        return myName;
    }

    @Override
    public ICommandLauncher getCommandLauncher() {
        return fCommandLauncher;
    }

    @Override
    public IBuildRunner getBuildRunner() throws CoreException {
        return fBuildRunner;
    }

    @Override
    public boolean appendEnvironment() {
        return true;
    }

    @Override
    public String getBuilderVariablePattern() {
        return modelvariableFormat[SUPER];
    }

}
