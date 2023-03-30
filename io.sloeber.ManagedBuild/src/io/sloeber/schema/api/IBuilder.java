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
package io.sloeber.schema.api;

import java.util.Set;

import org.eclipse.cdt.core.ICommandLauncher;
//import org.eclipse.cdt.managedbuilder.macros.IFileContextBuildMacroValues;
//import org.eclipse.cdt.managedbuilder.macros.IReservedMacroNameSupplier;
//import org.eclipse.cdt.newmake.core.IMakeBuilderInfo;
import org.eclipse.core.runtime.CoreException;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IBuildRunner;
import io.sloeber.autoBuild.extensionPoint.IMakefileGenerator;
import io.sloeber.autoBuild.extensionPoint.IReservedMacroNameSupplier;

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
    public static final String MAKEGEN_ID = "makefileGenerator"; //$NON-NLS-1$
    public static final String ERROR_PARSERS = IToolChain.ERROR_PARSERS;
    public static final String VARIABLE_FORMAT = "variableFormat"; //$NON-NLS-1$
    public static final String RESERVED_MACRO_NAMES = "reservedMacroNames"; //$NON-NLS-1$
    public static final String RESERVED_MACRO_NAME_SUPPLIER = "reservedMacroNameSupplier"; //$NON-NLS-1$
    public static final String ATTRIBUTE_TARGET_AUTO = "autoBuildTarget"; //$NON-NLS-1$
    public static final String ATTRIBUTE_TARGET_INCREMENTAL = "incrementalBuildTarget"; //$NON-NLS-1$
    public static final String ATTRIBUTE_TARGET_CLEAN = "cleanBuildTarget"; //$NON-NLS-1$
    public static final String ATTRIBUTE_IGNORE_ERR_CMD = "ignoreErrCmd"; //$NON-NLS-1$
    public static final String ATTRIBUTE_PARALLEL_BUILD_CMD = "parallelBuildCmd"; //$NON-NLS-1$
    public static final String ATTRIBUTE_COMMAND_LAUNCHER = "commandLauncher"; //$NON-NLS-1$
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
    public String getArguments(boolean parallel, int numParallel, boolean stopOnError);

    /**
     * Returns the BuildfileGenerator used to generate buildfiles for this builder
     *
     * @return IManagedBuilderMakefileGenerator
     */
    IMakefileGenerator getBuildFileGenerator();

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

    /**
     * Returns the tool-chain that is the parent of this builder.
     *
     * @return IToolChain
     */
    public IToolChain getParent();

    /**
     * Returns an array of Strings representing the patterns of the
     * builder/buildfile-generator
     * reserved variables
     *
     * @return String[]
     */
    public String[] getReservedMacroNames();

    public IReservedMacroNameSupplier getReservedMacroNameSupplier();

    public ICommandLauncher getCommandLauncher();

    public IBuildRunner getBuildRunner() throws CoreException;

    /**
     * If this returns true the environment variables are added to the command line
     * environment
     * If set false the environment variables are ignored when running a command.
     * In other words: If you define the PATH environment variable in eclipse and
     * this
     * method returns false the PATH will be the PATH that eclipse started with not
     * the PATH set in eclipse
     * In JABA's opinion this method should always return true
     * 
     * @return
     */
    boolean appendEnvironment();

    boolean supportsStopOnError();

    boolean supportsParallelBuild();

    String getCleanBuildTarget();

    String getIncrementalBuildTarget();

    boolean isAutoBuildEnable();

    String getAutoBuildTarget();

    boolean supportsBuild(boolean managed);

    public String getBuilderVariablePattern();

}
