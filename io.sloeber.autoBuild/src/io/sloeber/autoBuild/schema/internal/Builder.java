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
package io.sloeber.autoBuild.schema.internal;

import static io.sloeber.autoBuild.api.AutoBuildConstants.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;

import io.sloeber.autoBuild.api.IBuildRunner;
import io.sloeber.autoBuild.extensionPoint.providers.BuildRunnerForMake;
import io.sloeber.autoBuild.internal.AutoBuildCommon;
import io.sloeber.autoBuild.schema.api.IBuilder;

public class Builder extends SchemaObject implements IBuilder {
    private static final String DEFAULT_TARGET_CLEAN = "clean"; //$NON-NLS-1$
    private static final String DEFAULT_TARGET_INCREMENTAL = "all"; //$NON-NLS-1$

    private String[] modelcommand;
    private String[] modelarguments;
    private String[] modelErrorParsers;
    private String[] modelautoBuildTarget;
    private String[] modelincrementalBuildTarget;
    private String[] modelcleanBuildTarget;
    private String[] modelignoreErrCmd;
    private String[] modelparallelBuildCmd;
   // private String[] modelbuildRunner;

    //  Parent and children
    private IBuildRunner fBuildRunner = null;
    Set<String> myErrorParsers = new HashSet<>();


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
    public Builder( IExtensionPoint root, IConfigurationElement element) {
        loadNameAndID(root, element);

        modelcommand = getAttributes(COMMAND);
        modelarguments = getAttributes(ARGUMENTS);
        modelErrorParsers = getAttributes(ERROR_PARSERS);

        modelautoBuildTarget = getAttributes(ATTRIBUTE_TARGET_AUTO);
        modelincrementalBuildTarget = getAttributes(ATTRIBUTE_TARGET_INCREMENTAL);
        modelcleanBuildTarget = getAttributes(ATTRIBUTE_TARGET_CLEAN);
        modelignoreErrCmd = getAttributes(ATTRIBUTE_IGNORE_ERR_CMD);
        modelparallelBuildCmd = getAttributes(ATTRIBUTE_PARALLEL_BUILD_CMD);

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
    public IBuildRunner getBuildRunner()  {
        return fBuildRunner;
    }


}

