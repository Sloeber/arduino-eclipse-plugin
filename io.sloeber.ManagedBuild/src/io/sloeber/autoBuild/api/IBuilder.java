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
package io.sloeber.autoBuild.api;

import org.eclipse.cdt.core.ICommandLauncher;
import org.eclipse.cdt.core.settings.model.extension.CBuildData;
import org.eclipse.cdt.newmake.core.IMakeBuilderInfo;
import org.eclipse.core.resources.IFolder;
//import org.eclipse.cdt.managedbuilder.macros.IFileContextBuildMacroValues;
//import org.eclipse.cdt.managedbuilder.macros.IReservedMacroNameSupplier;
//import org.eclipse.cdt.newmake.core.IMakeBuilderInfo;
import org.eclipse.core.runtime.CoreException;

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
public interface IBuilder extends IMakeBuilderInfo, IHoldsOptions {
    public static final String BUILDER_ELEMENT_NAME = "builder"; //$NON-NLS-1$

    public static final String COMMAND = "command"; //$NON-NLS-1$
    public static final String ARGUMENTS = "arguments"; //$NON-NLS-1$
    public static final String BUILDFILEGEN_ID = "buildfileGenerator"; //$NON-NLS-1$
    //error parsers was missing
    public static final String VARIABLE_FORMAT = "variableFormat"; //$NON-NLS-1$
    public static final String RESERVED_MACRO_NAMES = "reservedMacroNames"; //$NON-NLS-1$
    public static final String RESERVED_MACRO_NAME_SUPPLIER = "reservedMacroNameSupplier"; //$NON-NLS-1$
    // macroInputFileNameValue was missing  
    //macroInputFileExtValue was missing
    //macroInputFileBaseNameValue was missing
    //macroInputFileRelPathValue was missing
    //macroInputDirRelPathValue was missing
    //macroOutputFileNameValue was missing
    //macroOutputFileExtValue was missing
    //macroOutputFileBaseNameValue was missing
    //macroOutputFileRelPathValue was missing 
    //macroOutputDirRelPathValue was missing
    public static final String ATTRIBUTE_SUPORTS_MANAGED_BUILD = "supportsManagedBuild"; //$NON-NLS-1$
    public static final String ATTRIBUTE_TARGET_AUTO = "autoBuildTarget"; //$NON-NLS-1$
    public static final String ATTRIBUTE_TARGET_INCREMENTAL = "incrementalBuildTarget"; //$NON-NLS-1$
    public static final String ATTRIBUTE_TARGET_CLEAN = "cleanBuildTarget"; //$NON-NLS-1$
    public static final String ATTRIBUTE_IGNORE_ERR_CMD = "ignoreErrCmd"; //$NON-NLS-1$
    public static final String ATTRIBUTE_PARALLEL_BUILD_CMD = "parallelBuildCmd"; //$NON-NLS-1$
    public static final String ATTRIBUTE_PARALLEL_BUILD_ON = "parallelBuildOn"; //$NON-NLS-1$
    public static final String ATTRIBUTE_PARALLELIZATION_NUMBER = "parallelizationNumber"; //$NON-NLS-1$
    //public static final String IS_SYSTEM = "isSystem"; this is a general field
    public static final String ATTRIBUTE_COMMAND_LAUNCHER = "commandLauncher"; //$NON-NLS-1$
    public static final String ATTRIBUTE_BUILD_RUNNER = "buildRunner"; //$NON-NLS-1$

    //   
    //    public static final String VERSIONS_SUPPORTED = "versionsSupported"; //$NON-NLS-1$
    //    public static final String CONVERT_TO_ID = "convertToId"; //$NON-NLS-1$
    //    static final String ATTRIBUTE_CLEAN_ENABLED = "enableCleanBuild"; //$NON-NLS-1$
    //    static final String ATTRIBUTE_INCREMENTAL_ENABLED = "enabledIncrementalBuild"; //$NON-NLS-1$
    //    static final String ATTRIBUTE_AUTO_ENABLED = "enableAutoBuild"; //$NON-NLS-1$
    //    static final String ATTRIBUTE_ENVIRONMENT = "environment"; //$NON-NLS-1$
    //    static final String ATTRIBUTE_APPEND_ENVIRONMENT = "appendEnvironment"; //$NON-NLS-1$
    //
    //    static final String ATTRIBUTE_MANAGED_BUILD_ON = "managedBuildOn"; //$NON-NLS-1$
    //    static final String ATTRIBUTE_KEEP_ENV = "keepEnvironmentInBuildfile"; //$NON-NLS-1$
    //    
    //
    //    static final String ATTRIBUTE_CUSTOMIZED_ERROR_PARSERS = "customizedErrorParsers"; //$NON-NLS-1$
    //    static final String ATTRIBUTE_CUSTOM_PROPS = "customBuilderProperties"; //$NON-NLS-1$
    //
    //    static final String ATTRIBUTE_STOP_ON_ERR = "stopOnErr"; //$NON-NLS-1$
    //
    //    /** @since 8.1 */
    //    static final String VALUE_OPTIMAL = "optimal"; //$NON-NLS-1$
    //    /** @since 8.1 */
    //    static final String VALUE_UNLIMITED = "unlimited"; //$NON-NLS-1$
    //    static final String PARALLEL_PATTERN_NUM = "*"; //$NON-NLS-1$
    //    static final String PARALLEL_PATTERN_NUM_START = "["; //$NON-NLS-1$
    //    static final String PARALLEL_PATTERN_NUM_END = "]"; //$NON-NLS-1$
    //
    //    static final String OUTPUT_ENTRIES = "outputEntries"; //$NON-NLS-1$
    //
    //    static final String DEFAULT_TARGET_INCREMENTAL = "all"; //$NON-NLS-1$
    //    static final String DEFAULT_TARGET_CLEAN = "clean"; //$NON-NLS-1$
    //    static final String DEFAULT_TARGET_AUTO = "all"; //$NON-NLS-1$

    /**
     * Returns the command line arguments to pass to the build/make utility used
     * to build a configuration.
     *
     * @return String
     */
    public String getArguments();

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

    /**
     * Returns the semicolon separated list of unique IDs of the error parsers
     * associated
     * with the builder.
     *
     * @return String
     */
    public String getErrorParserIds();

    /**
     * Returns the ordered list of unique IDs of the error parsers associated with
     * the
     * builder.
     *
     * @return String[]
     */
    public String[] getErrorParserList();

    /**
     * Returns the tool-chain that is the parent of this builder.
     *
     * @return IToolChain
     */
    public IToolChain getParent();

    /**
     * Returns the <code>IBuilder</code> that is the superclass of this
     * target platform, or <code>null</code> if the attribute was not specified.
     *
     * @return IBuilder
     */
    public IBuilder getSuperClass();

    /**
     * Returns whether this element is abstract. Returns <code>false</code>
     * if the attribute was not specified.
     *
     * @return boolean
     */
    public boolean isAbstract();

    /**
     * Returns <code>true</code> if this builder was loaded from a manifest file,
     * and <code>false</code> if it was loaded from a project (.cdtbuild) file.
     *
     * @return boolean
     */
    public boolean isExtensionElement();

    /**
     * 
     * /**
     * Returns the IFileContextBuildMacroValues interface reference that specifies
     * the file-context macro-values provided by the tool-integrator
     *
     * @return IFileContextBuildMacroValues
     */
    public IFileContextBuildMacroValues getFileContextBuildMacroValues();

    /**
     * Returns String representing the build variable pattern to be used while
     * makefile generation
     *
     * @return String
     */
    public String getBuilderVariablePattern();

    /**
     * Returns an array of Strings representing the patterns of the
     * builder/buildfile-generator
     * reserved variables
     *
     * @return String[]
     */
    public String[] getReservedMacroNames();

    /**
     * Returns the tool-integrator defined implementation of the
     * IReservedMacroNameSupplier
     * to be used for detecting the builder/buildfile-generator reserved variables
     * 
     * @return IReservedMacroNameSupplier
     */
    public IReservedMacroNameSupplier getReservedMacroNameSupplier();

    public CBuildData getBuildData();

    public boolean isCustomBuilder();

    public boolean supportsCustomizedBuild();

    boolean isInternalBuilder();

    boolean matches(IBuilder builder);

    boolean isSystemObject();

    String getUniqueRealName();

    /**
     * Returns the ICommandLauncher which should be used to launch the builder
     * command.
     *
     * @return ICommandLauncher
     * @since 6.0
     */
    public ICommandLauncher getCommandLauncher();

    /**
     * Returns the build runner for this builder.
     *
     * @return build runner
     * @since 8.0
     */
    public IBuildRunner getBuildRunner() throws CoreException;

    public void setManagedBuildOn(boolean b);

}
