/*******************************************************************************
 * Copyright (c) 2004, 2018 Intel Corporation and others.
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
 *******************************************************************************/
package io.sloeber.schema.api;

import java.util.List;

import org.eclipse.cdt.core.settings.model.extension.CTargetPlatformData;
//import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;
//import org.eclipse.cdt.managedbuilder.macros.IConfigurationBuildMacroSupplier;

import io.sloeber.autoBuild.api.IEnvironmentVariableSupplier;
import io.sloeber.autoBuild.api.IOptionPathConverter;
import io.sloeber.autoBuild.extensionPoint.IConfigurationBuildMacroSupplier;

/**
 * This interface represents a tool-integrator-defined, ordered set of tools
 * that transform the project's input into the project's outputs. A
 * tool-chain can be defined as part of a configuration, or as an
 * independent specification that is referenced in a separate configuration
 * via the toolChain superclass attribute.
 * <p>
 * The toolChain contains one or more children of type tool. These define
 * the tools used in the tool-chain. The toolChain contains one child of
 * type targetPlatform. This defines the architecture/os combination where
 * the outputs of the project can be deployed. The toolChain contains one
 * child of type builder. This defines the "build" or "make" utility that
 * is used to drive the transformation of the inputs into outputs.
 *
 * @since 2.1
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IToolChain extends IHoldsOptions {
    public static final String TOOL_CHAIN_ELEMENT_NAME = "toolChain"; //$NON-NLS-1$
    public static final String OS_LIST = "osList"; //$NON-NLS-1$
    public static final String ARCH_LIST = "archList"; //$NON-NLS-1$
    public static final String ERROR_PARSERS = "errorParsers"; //$NON-NLS-1$
    public static final String LANGUAGE_SETTINGS_PROVIDERS = "languageSettingsProviders"; //$NON-NLS-1$
    public static final String SCANNER_CONFIG_PROFILE_ID = "scannerConfigDiscoveryProfileId"; //$NON-NLS-1$
    public static final String TARGET_TOOL = "targetTool"; //$NON-NLS-1$
    public static final String SECONDARY_OUTPUTS = "secondaryOutputs"; //$NON-NLS-1$
    public static final String IS_TOOL_CHAIN_SUPPORTED = "isToolChainSupported"; //$NON-NLS-1$
    public static final String CONFIGURATION_ENVIRONMENT_SUPPLIER = "environmentSupplier"; //$NON-NLS-1$
    public static final String CONFIGURATION_MACRO_SUPPLIER = "buildMacroSupplier"; //$NON-NLS-1$

    /**
     * Returns the configuration that is the parent of this tool-chain.
     *
     * @return IConfiguration
     */
    public IConfiguration getParent();

    /**
     * Returns the target-platform child of this tool-chain
     *
     * @return ITargetPlatform
     */
    public ITargetPlatform getTargetPlatform();

    /**
     * Returns the builder child of this tool-chain.
     *
     * @return IBuilder
     */
    public IBuilder getBuilder();

    /**
     * Returns an array of tool children of this tool-chain
     *
     * @return ITool[]
     */
    public List<ITool> getTools();

    /**
     * Returns the tool in this tool-chain with the ID specified in the argument,
     * or <code>null</code>
     *
     * @param id
     *            The ID of the requested tool
     * @return ITool
     */
    public ITool getTool(String id);

    /**
     * Returns whether this element is abstract. Returns <code>false</code>
     * if the attribute was not specified.
     * 
     * @return boolean
     */
    public boolean isAbstract();

    /**
     * Returns an array of operating systems the tool-chain outputs can run on.
     *
     * @return String[]
     */
    public List<String> getOSList();

    /**
     * Returns an array of architectures the tool-chain outputs can run on.
     *
     * @return String[]
     */
    public List<String> getArchList();

    /**
     * Returns the semicolon separated list of unique IDs of the error parsers
     * associated
     * with the tool-chain.
     *
     * @return String
     */
    public String getErrorParserIds();

    /**
     * Returns the semicolon separated list of unique IDs of the error parsers
     * associated
     * with the tool-chain, filtered for the specified configuration.
     */
    public String getErrorParserIds(IConfiguration config);

    /**
     * Returns the ordered list of unique IDs of the error parsers associated with
     * the
     * tool-chain.
     *
     * @return String[]
     */
    public List<String> getErrorParserList();

    /**
     * Returns the default language settings providers IDs.
     *
     * @return the default language settings providers IDs separated by semicolon or
     *         {@code null} if none.
     *
     * @since 8.1
     */
    public String getDefaultLanguageSettingsProviderIds();

    /**
     * Returns the scanner config discovery profile id or <code>null</code> if none.
     *
     * @return String
     */
    public String getScannerConfigDiscoveryProfileId();

    /**
     * Returns the sem-colon separated list of Tool ids containing each
     * tool that can create the final build artifact (the end target of
     * the build). MBS will use the first ID in the list that matches
     * a Tool in the ToolChain. One reason for specifying a list, is
     * that different versions of a tool can be selected based upon the
     * project nature (e.g. different tool definitions for a linker for C vs. C++).
     *
     * @return String
     */
    public String getTargetToolIds();

    /**
     * Returns the list of Tool ids containing each
     * tool that can create the final build artifact (the end target of
     * the build). MBS will use the first ID in the list that matches
     * a Tool in the ToolChain. One reason for specifying a list, is
     * that different versions of a tool can be selected based upon the
     * project nature (e.g. different tool definitions for a linker for C vs. C++).
     *
     * @return String[]
     */
    public String[] getTargetToolList();

    /**
     * Returns the OutputTypes in this tool-chain, besides the primary
     * output of the targetTool, that are also considered to be build
     * artifacts.
     *
     * @return IOutputType[]
     */
    public List<IOutputType> getSecondaryOutputs();

    /**
     * Returns <code>true</code> if the tool-chain support is installed on the
     * system
     * otherwise returns <code>false</code>
     *
     * @return boolean
     */
    public boolean isSupported();

    /**
     * Returns the tool-integrator provided implementation of the configuration
     * environment variable supplier
     * or <code>null</code> if none.
     *
     * @return IConfigurationEnvironmentVariableSupplier
     */
    public IEnvironmentVariableSupplier getEnvironmentVariableSupplier();

    /**
     * Returns the tool-integrator provided implementation of the configuration
     * build macro supplier
     * or <code>null</code> if none.
     *
     * @return IConfigurationBuildMacroSupplier
     */
    public IConfigurationBuildMacroSupplier getBuildMacroSupplier();

    /**
     * Returns an IOptionPathConverter implementation for this toolchain
     * or null, if no conversion is required
     */
    //    public IOptionPathConverter getOptionPathConverter();

    IFolderInfo getParentFolderInfo();

    CTargetPlatformData getTargetPlatformData();

    boolean supportsBuild(boolean managed);

    boolean isSystemObject();

    String getUniqueRealName();
}