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
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;

import io.sloeber.autoBuild.api.IBuildRunner;
import io.sloeber.autoBuild.extensionPoint.IMakefileGenerator;
import io.sloeber.autoBuild.extensionPoint.IReservedMacroNameSupplier;
import io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon;
import io.sloeber.autoBuild.extensionPoint.providers.BuildRunnerForMake;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IOptions;
import io.sloeber.schema.api.IToolChain;

public class Builder extends SchemaObject implements IBuilder {
    private static final String DEFAULT_TARGET_CLEAN = "clean"; //$NON-NLS-1$
    private static final String DEFAULT_TARGET_INCREMENTAL = "all"; //$NON-NLS-1$

    private String[] modelcommand;
    private String[] modelarguments;
    private String[] modelbuildfileGenerator;
    private String[] modelErrorParsers;
    private String[] modelvariableFormat;
    private String[] modelreservedMacroNames;
    private String[] modelReservedMacroNameSupplier;
    private String[] modelautoBuildTarget;
    private String[] modelincrementalBuildTarget;
    private String[] modelcleanBuildTarget;
    private String[] modelignoreErrCmd;
    private String[] modelparallelBuildCmd;
    private String[] modelisSystem;
    private String[] modelcommandLauncher;
    private String[] modelbuildRunner;

    //  Parent and children
    private IToolChain parent;
    private String[] reservedMacroNames;
    private ICommandLauncher fCommandLauncher;
    private IReservedMacroNameSupplier reservedMacroNameSupplier;
    private IBuildRunner fBuildRunner = null;
    private IMakefileGenerator myMakeFileGenerator;
    Set<String> myErrorParsers = new HashSet<>();

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
            fBuildRunner = new BuildRunnerForMake();
        }

        resolveFields();

    }

    private void resolveFields() {
        if (myName.isBlank()) {
            myName = getId();
        }
        reservedMacroNames = modelreservedMacroNames[SUPER].split(","); //$NON-NLS-1$

        if (modelcommand[SUPER].isBlank()) {
            modelcommand[SUPER] = "make"; //$NON-NLS-1$
        }
        if (modelcleanBuildTarget[SUPER].isBlank()) {
            modelcleanBuildTarget[SUPER] = DEFAULT_TARGET_CLEAN;
        }
        if (modelincrementalBuildTarget[SUPER].isBlank()) {
            modelincrementalBuildTarget[SUPER] = DEFAULT_TARGET_INCREMENTAL;
        }

        String parseArray[] = modelErrorParsers[SUPER].split(Pattern.quote(SEMICOLON));
        myErrorParsers.addAll(Arrays.asList(parseArray));

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
    public String getCommand() {
        return modelcommand[SUPER];
    }

    @Override
    public String getArguments(boolean parallel, int numParallel, boolean stopOnError) {
        String ret = modelarguments[SUPER];
        String separator = EMPTY_STRING;
        if (!ret.isBlank()) {
            separator = BLANK;
        }
        if (!stopOnError) {
            ret = ret + separator + modelignoreErrCmd[SUPER];
            separator = BLANK;
        }
        if (parallel) {
            int actualParallel = numParallel;
            if (numParallel == PARRALLEL_BUILD_OPTIMAL_JOBS) {
                actualParallel = AutoBuildCommon.getOptimalParallelJobNum();
            }
            if (numParallel == PARRALLEL_BUILD_UNLIMITED_JOBS) {
                actualParallel = 999;
            }
            String parallelParameter = modelparallelBuildCmd[SUPER].replace(ASTERISK, Integer.toString(actualParallel));
            ret = ret + separator + parallelParameter;
            separator = BLANK;
        }
        return ret;
    }

    @Override
    public Set<String> getErrorParserList() {

        return myErrorParsers;

    }

    @Override
    public IMakefileGenerator getBuildFileGenerator() {
        return myMakeFileGenerator;
    }

    //TODO JABA get rid of this replace with "isMacroName"
    @Override
    public String[] getReservedMacroNames() {
        return reservedMacroNames;
    }

    //TODO JABA get rid of this replace with "isMacroName"
    @Override
    public IReservedMacroNameSupplier getReservedMacroNameSupplier() {
        return reservedMacroNameSupplier;
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

    @Override
    public boolean supportsBuild(boolean managed) {
        return false;
    }

    @Override
    public boolean supportsParallelBuild() {
        return !modelparallelBuildCmd[SUPER].isBlank();
    }

    @Override
    public boolean supportsStopOnError() {
        return !modelarguments[SUPER].isBlank();
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
    public String getBuilderVariablePattern() {
        return modelvariableFormat[SUPER];
    }

}

