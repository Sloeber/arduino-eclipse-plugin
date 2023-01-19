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

import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.extension.CBuildData;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
//import org.eclipse.cdt.managedbuilder.core.IFileInfo;
//import org.eclipse.cdt.managedbuilder.core.IFolderInfo;
//import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;
//import org.eclipse.cdt.managedbuilder.macros.IConfigurationBuildMacroSupplier;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import io.sloeber.autoBuild.api.IEnvironmentVariableSupplier;
import io.sloeber.autoBuild.extensionPoint.IConfigurationBuildMacroSupplier;
import io.sloeber.schema.internal.IBuildObject;

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
public interface IConfiguration extends IBuildObject {
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


    /**
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

    /**
     * Returns the build arguments from this configuration's builder
     *
     * @return String
     */
    public String getBuildArguments();

    /**
     * Returns the build command from this configuration's builder
     *
     * @return String
     */
    public String getBuildCommand();

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
     * Projects have C or CC natures. Tools can specify a filter so they are not
     * misapplied to a project. This method allows the caller to retrieve a list
     * of tools from a project that are correct for a project's nature.
     *
     * @return an array of <code>ITools</code> that have compatible filters
     *         for this configuration.
     */
    List<ITool> getFilteredTools();

    /**
     * Returns the managed-project parent of this configuration, if this is a
     * project configuration. Otherwise, returns <code>null</code>.
     *
     * @return IManagedProject
     */
    public IManagedProject getManagedProject();

    /**
     * Returns the Eclipse project that owns the configuration.
     *
     * @return IResource
     */
    public IResource getOwner();



    /**
     * Returns the project-type parent of this configuration, if this is an
     * extension configuration. Otherwise, returns <code>null</code>.
     *
     * @return IProjectType
     */
    public IProjectType getProjectType();


    /**
     * Returns the <code>ITool</code> in this configuration's tool-chain with
     * the same id as the argument, or <code>null</code>.
     *
     * @param id
     *            unique identifier to search for
     * @return ITool
     */
    public ITool getTool(String id);


    /**
     * Returns the <code>IToolChain</code> child of this configuration.
     *
     * @return IToolChain
     */
    public IToolChain getToolChain();

    /**
     * Returns the command-line invocation command for the specified tool.
     *
     * @param tool
     *            The tool that will have its command retrieved.
     * @return String The command
     */
    public String getToolCommand(ITool tool);

    /**
     * Returns the tools that are used in this configuration's tool-chain.
     *
     * @return ITool[]
     */
    public List<ITool> getTools();

    /**
     * Returns the tool in this configuration specified with
     * the toolChain#targetTool attribute that creates the build artifact
     *
     * NOTE: This method returns null in case the toolChain definition
     * does not have the targetTool attribute or if the attribute does not
     * refer to the appropriate tool.
     * For the target tool calculation the IConfiguration#calculateTargetTool()
     * method should be used
     *
     * @see IConfiguration#calculateTargetTool()
     *
     * @return ITool
     */
    public ITool getTargetTool();

    /**
     * Returns <code>true</code> if this configuration has overridden the default
     * build
     * build command in this configuration, otherwise <code>false</code>.
     *
     * @return boolean
     */
    public boolean hasOverriddenBuildCommand();

    /**
     * Returns <code>true</code> if the extension matches one of the special
     * file extensions the tools for the configuration consider to be a header file.
     *
     * @param ext
     *            the file extension of the resource
     * @return boolean
     */
    public boolean isHeaderFile(String ext);



    /**
     * Returns <code>true</code> if the configuration's tool-chain is supported on
     * the system
     * otherwise returns <code>false</code>
     *
     * @return boolean
     */
    public boolean isSupported();

    /**
     * Returns the implementation of the IConfigurationEnvironmentVariableSupplier
     * provided
     * by the tool-integrator or <code>null</code> if none.
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
     * Calculates the configuration target tool.
     *
     * @return ITool or null if not found
     *
     * @since 3.1
     */
    public ITool calculateTargetTool();



//    IResourceInfo getResourceInfo(IPath path, boolean exactPath);

//    List<IResourceInfo> getResourceInfos();

//    IResourceInfo getResourceInfoById(String id);

//    IFolderInfo getRootFolderInfo();

    CConfigurationData getConfigurationData();

    List<ICSourceEntry> getSourceEntries();

    CBuildData getBuildData();

    IBuilder getBuilder();
    
    IFolder getBuildFolder(IProject project);


    boolean supportsBuild(boolean managed);


}
