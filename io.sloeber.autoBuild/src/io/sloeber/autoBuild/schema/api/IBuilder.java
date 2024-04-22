/*******************************************************************************
 * Copyright (c) 2004, 2011 Intel Corporation and others.
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
 * James Blackburn (Broadcom Corp.)
 *******************************************************************************/
package io.sloeber.autoBuild.schema.api;

import java.util.Set;

import io.sloeber.autoBuild.api.IBuildRunner;
import io.sloeber.autoBuild.core.Activator;

/**
 * This class represents the utility that drives the build process
 * (typically, but not necessarily, a variant of "make"). It defines
 * the command needed to invoke the build utility in the command attribute.
 * Any special flags that need to be passed to the builder are defined
 * in the arguments attribute. The builder can specify the error parser(s)
 * to be used to parse its output. The builder also specifies a Java class
 * that generates the build file.
 *
 * @since 2.1
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IBuilder extends ISchemaObject {
    public static final String BUILDER_ELEMENT_NAME = "builder"; //$NON-NLS-1$

    public static final String COMMAND = "command"; //$NON-NLS-1$
    public static final String ARGUMENTS = "arguments"; //$NON-NLS-1$
    public static final String ERROR_PARSERS = IToolChain.ERROR_PARSERS;
    public static final String ATTRIBUTE_TARGET_AUTO = "autoBuildTarget"; //$NON-NLS-1$
    public static final String ATTRIBUTE_TARGET_INCREMENTAL = "incrementalBuildTarget"; //$NON-NLS-1$
    public static final String ATTRIBUTE_TARGET_CLEAN = "cleanBuildTarget"; //$NON-NLS-1$
    public static final String ATTRIBUTE_IGNORE_ERR_CMD = "ignoreErrCmd"; //$NON-NLS-1$
    public static final String ATTRIBUTE_PARALLEL_BUILD_CMD = "parallelBuildCmd"; //$NON-NLS-1$
    public static final String ATTRIBUTE_BUILD_RUNNER = "buildRunner"; //$NON-NLS-1$

    public final static String ARGS_PREFIX = Activator.getId();

    public final static String BUILD_LOCATION = ARGS_PREFIX + ".build.location"; //$NON-NLS-1$
    public final static String BUILD_COMMAND = ARGS_PREFIX + ".build.command"; //$NON-NLS-1$
    public final static String BUILD_ARGUMENTS = ARGS_PREFIX + ".build.arguments"; //$NON-NLS-1$
    public final static String BUILD_TARGET_INCREMENTAL = ARGS_PREFIX + ".build.target.inc"; //$NON-NLS-1$
    public final static String BUILD_TARGET_AUTO = ARGS_PREFIX + ".build.target.auto"; //$NON-NLS-1$
    public final static String BUILD_TARGET_CLEAN = ARGS_PREFIX + ".build.target.clean"; //$NON-NLS-1$

    /**
     * Returns the command line arguments to pass to the build/make utility used
     * to build a configuration.
     *
     * @return String
     */
    public String getArguments( int numParallel, boolean stopOnError);


    /**
     * Returns the name of the build/make utility for the configuration.
     *
     * @return String
     */
    public String getCommand();

    //    /**
    //     * Returns the semicolon separated list of unique IDs of the error parsers
    //     * associated
    //     * with the builder.
    //     *
    //     * @return String
    //     */
    //    public String getErrorParserIds();

    /**
     * Returns the ordered list of unique IDs of the error parsers associated with
     * the
     * builder.
     *
     * @return String[]
     */
    public Set<String> getErrorParserList();



    public IBuildRunner getBuildRunner() ;

    boolean supportsStopOnError();

    boolean supportsParallelBuild();

    String getCleanBuildTarget();

    String getIncrementalBuildTarget();

    boolean isAutoBuildEnable();

    String getAutoBuildTarget();

    boolean supportsBuild(boolean managed);

}
