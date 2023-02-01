/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     Dmitry Kozlov (CodeSourcery) - Save build output preferences (bug 294106)
 *     Andrew Gvozdev (Quoin Inc)   - Saving build output implemented in different way (bug 306222)
 *******************************************************************************/
package io.sloeber.schema.api;

import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.core.resources.IFolder;

/**
 * A tool-integrator defines default configurations as children of the project
 * type.
 * These provide a template for the configurations added to the user's project,
 * which are stored in the project's .cproject file.
 * <p>
 * The configuration contains one child of type tool-chain. This describes how
 * the
 * project's resources are transformed into the build artifact. The
 * configuration can
 * contain one or more children of type resourceConfiguration. These describe
 * build
 * settings of individual resources that are different from the configuration as
 * a whole.
 *
 * @since 2.1
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IConfiguration extends ISchemaObject {
    public static final String CONFIGURATION_ELEMENT_NAME = "configuration"; //$NON-NLS-1$

    public static final String ARTIFACT_NAME = "artifactName"; //$NON-NLS-1$
    public static final String ARTIFACT_EXTENSION = "artifactExtension"; //$NON-NLS-1$
    public static final String CLEAN_COMMAND = "cleanCommand"; //$NON-NLS-1$
    public static final String ERROR_PARSERS = "errorParsers"; //$NON-NLS-1$
    public static final String LANGUAGE_SETTINGS_PROVIDERS = "languageSettingsProviders"; //$NON-NLS-1$
    public static final String PREBUILD_STEP = "prebuildStep"; //$NON-NLS-1$
    public static final String POSTBUILD_STEP = "postbuildStep"; //$NON-NLS-1$
    public static final String PREANNOUNCEBUILD_STEP = "preannouncebuildStep"; //$NON-NLS-1$
    public static final String POSTANNOUNCEBUILD_STEP = "postannouncebuildStep"; //$NON-NLS-1$
    public static final String DESCRIPTION = "description"; //$NON-NLS-1$
    public static final String BUILD_PROPERTIES = "buildProperties"; //$NON-NLS-1$
    public static final String BUILD_ARTEFACT_TYPE = "buildArtefactType"; //$NON-NLS-1$

    /**
     * Returns the description of the configuration.
     *
     * @return String
     */
    public String getDescription();

    /**
     * 
     * 
     * /**
     * Returns the extension that should be applied to build artifacts created by
     * this configuration.
     *
     * @return String
     */
    public String getArtifactExtension();

    /**
     * Returns the name of the final build artifact.
     *
     * @return String
     */
    public String getArtifactName();

    //    /**
    //     * Returns the build arguments from this configuration's builder
    //     *
    //     * @return String
    //     */
    //    public String getBuildArguments();

    /**
     * Returns the build command from this configuration's builder
     *
     * @return String
     */
    //    public String getBuildCommand();

    /**
     * Returns the prebuild step command
     *
     * @return String
     */
    public String getPrebuildStep();

    /**
     * Returns the postbuild step command
     *
     * @return String
     */
    public String getPostbuildStep();

    /**
     * Returns the display string associated with the prebuild step
     *
     * @return String
     */
    public String getPreannouncebuildStep();

    /**
     * Returns the display string associated with the postbuild step
     *
     * @return String
     */
    public String getPostannouncebuildStep();

    /**
     * Answers the OS-specific command to remove files created by the build
     * of this configuration.
     *
     * @return String
     */
    public String getCleanCommand();

    /**
     * Answers the semicolon separated list of unique IDs of the error parsers
     * associated
     * with this configuration.
     *
     * @return String
     */
    public String getErrorParserIds();

    /**
     * Answers the ordered list of unique IDs of the error parsers associated
     * with this configuration.
     *
     * @return String[]
     */
    public String[] getErrorParserList();

    /**
     * Returns default language settings providers IDs specified for the
     * configuration.
     * 
     * @return default language settings providers IDs or {@code null}.
     *
     * @since 8.1
     */
    public List<String> getDefaultLanguageSettingsProviderIds();

    /**
     * Returns the project-type parent of this configuration, if this is an
     * extension configuration. Otherwise, returns <code>null</code>.
     *
     * @return IProjectType
     */
    public IProjectType getProjectType();

    //    /**
    //     * Returns the <code>ITool</code> in this configuration's tool-chain with
    //     * the same id as the argument, or <code>null</code>.
    //     *
    //     * @param id
    //     *            unique identifier to search for
    //     * @return ITool
    //     */
    //    public ITool getTool(String id);

    /**
     * Returns the <code>IToolChain</code> child of this configuration.
     *
     * @return IToolChain
     */
    public IToolChain getToolChain();

    //    /**
    //     * Returns the command-line invocation command for the specified tool.
    //     *
    //     * @param tool
    //     *            The tool that will have its command retrieved.
    //     * @return String The command
    //     */
    //    public String getToolCommand(ITool tool);

    //    /**
    //     * Returns the tools that are used in this configuration's tool-chain.
    //     *
    //     * @return ITool[]
    //     */
    //    public List<ITool> getTools();

    //    /**
    //     * Returns the tool in this configuration specified with
    //     * the toolChain#targetTool attribute that creates the build artifact
    //     *
    //     * NOTE: This method returns null in case the toolChain definition
    //     * does not have the targetTool attribute or if the attribute does not
    //     * refer to the appropriate tool.
    //     * For the target tool calculation the IConfiguration#calculateTargetTool()
    //     * method should be used
    //     *
    //     * @see IConfiguration#calculateTargetTool()
    //     *
    //     * @return ITool
    //     */
    //    public ITool getTargetTool();

    //    /**
    //     * Returns <code>true</code> if the extension matches one of the special
    //     * file extensions the tools for the configuration consider to be a header file.
    //     *
    //     * @param ext
    //     *            the file extension of the resource
    //     * @return boolean
    //     */
    //    public boolean isHeaderFile(String ext);

    /**
     * Returns <code>true</code> if the configuration's tool-chain is supported on
     * the system
     * otherwise returns <code>false</code>
     *
     * @return boolean
     */
    public boolean isSupported();

    //    /**
    //     * Calculates the configuration target tool.
    //     *
    //     * @return ITool or null if not found
    //     *
    //     * @since 3.1
    //     */
    //    public ITool calculateTargetTool();

    List<ICSourceEntry> getSourceEntries();

    IBuilder getBuilder();

    IFolder getBuildFolder(ICConfigurationDescription cfg);

    Map<String, String> getDefaultBuildProperties();

    //    boolean supportsBuild(boolean managed);

}
