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
package io.sloeber.autoBuild.api;

import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.extension.CBuildData;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
//import org.eclipse.cdt.managedbuilder.core.IFileInfo;
//import org.eclipse.cdt.managedbuilder.core.IFolderInfo;
//import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;
//import org.eclipse.cdt.managedbuilder.macros.IConfigurationBuildMacroSupplier;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;

import io.sloeber.autoBuild.extensionPoint.IConfigurationBuildMacroSupplier;

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
public interface IConfiguration
        extends IBuildObject, IBuildObjectPropertiesContainer, IOptionalBuildObjectPropertiesContainer {
    public static final String ARTIFACT_NAME = "artifactName"; //$NON-NLS-1$
    public static final String CLEAN_COMMAND = "cleanCommand"; //$NON-NLS-1$
    public static final String PREBUILD_STEP = "prebuildStep"; //$NON-NLS-1$
    public static final String POSTBUILD_STEP = "postbuildStep"; //$NON-NLS-1$
    public static final String PREANNOUNCEBUILD_STEP = "preannouncebuildStep"; //$NON-NLS-1$
    public static final String POSTANNOUNCEBUILD_STEP = "postannouncebuildStep"; //$NON-NLS-1$
    // Schema element names
    public static final String CONFIGURATION_ELEMENT_NAME = "configuration"; //$NON-NLS-1$
    public static final String ERROR_PARSERS = "errorParsers"; //$NON-NLS-1$
    /** @since 8.1 */
    public static final String LANGUAGE_SETTINGS_PROVIDERS = "languageSettingsProviders"; //$NON-NLS-1$
    public static final String EXTENSION = "artifactExtension"; //$NON-NLS-1$
    public static final String PARENT = "parent"; //$NON-NLS-1$

    public static final String DESCRIPTION = "description"; //$NON-NLS-1$

    public static final String BUILD_PROPERTIES = "buildProperties"; //$NON-NLS-1$
    /**
     * @since 8.6
     */
    public static final String OPTIONAL_BUILD_PROPERTIES = "optionalBuildProperties"; //$NON-NLS-1$
    public static final String BUILD_ARTEFACT_TYPE = "buildArtefactType"; //$NON-NLS-1$
    public static final String IS_SYSTEM = "isSystem"; //$NON-NLS-1$

    public static final String SOURCE_ENTRIES = "sourceEntries"; //$NON-NLS-1$

    /**
     * Returns the description of the configuration.
     *
     * @return String
     */
    public String getDescription();

    /**
     * Sets the description of the receiver to the value specified in the argument
     */
    public void setDescription(String description);

    /**
     * Creates a child resource configuration corresponding to the passed in file.
     */
    public IResourceConfiguration createResourceConfiguration(IFile file);

    /**
     * Creates the <code>IToolChain</code> child of this configuration.
     *
     * @param superClass
     *            - The superClass, if any
     * @param Id
     *            - The id for the new tool chain
     * @param name
     *            - The name for the new tool chain
     * @param isExtensionElement
     *            - set {@code true} if the toolchain being created
     *            represents extension point toolchain
     *
     * @return IToolChain
     */
    public IToolChain createToolChain(IToolChain superClass, String Id, String name, boolean isExtensionElement);

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
    public String[] getDefaultLanguageSettingsProviderIds();

    /**
     * Projects have C or CC natures. Tools can specify a filter so they are not
     * misapplied to a project. This method allows the caller to retrieve a list
     * of tools from a project that are correct for a project's nature.
     *
     * @return an array of <code>ITools</code> that have compatible filters
     *         for this configuration.
     */
    ITool[] getFilteredTools();

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
     * Returns the configuration that this configuration is based on.
     *
     * @return IConfiguration
     */
    public IConfiguration getParent();

    /**
     * Returns the project-type parent of this configuration, if this is an
     * extension configuration. Otherwise, returns <code>null</code>.
     *
     * @return IProjectType
     */
    public IProjectType getProjectType();

    /**
     * @param path
     *            - path of the resource
     *
     * @return the resource configuration child of this configuration
     *         that is associated with the project resource, or <code>null</code> if
     *         none.
     */
    public IResourceConfiguration getResourceConfiguration(String path);

    /**
     * Returns the resource configuration children of this configuration.
     *
     * @return IResourceConfigurations[]
     */
    public IResourceConfiguration[] getResourceConfigurations();

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
     * Returns the <code>ITool</code> in this configuration's tool-chain with
     * the specified ID, or the tool(s) with a superclass with this id.
     *
     * <p>
     * If the tool-chain does not have a tool with that ID, the method
     * returns an empty array. It is the responsibility of the caller to
     * verify the return value.
     *
     * @param id
     *            unique identifier of the tool to search for
     * @return <code>ITool[]</code>
     * @since 3.0.2
     */
    public ITool[] getToolsBySuperClassId(String id);

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
    public ITool[] getTools();

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
     * Returns <code>true</code> if this configuration was loaded from a manifest
     * file,
     * and <code>false</code> if it was loaded from a project (.cdtbuild) file.
     *
     * @return boolean
     */
    public boolean isExtensionElement();

    /**
     * Returns whether this configuration has been changed and requires the
     * project to be rebuilt.
     *
     * @return <code>true</code> if the configuration contains a change
     *         that needs the project to be rebuilt.
     *         Should not be called for an extension configuration.
     */
    public boolean needsRebuild();

    /**
     * Removes a resource configuration from the configuration's list.
     *
     * @param resConfig
     *            - resource configuration to remove
     */
    //public void removeResourceConfiguration(IResourceInfo resConfig);

    public void removeResourceInfo(IPath path);

    /**
     * Set (override) the extension that should be appended to the build artifact
     * for the receiver.
     */
    public void setArtifactExtension(String extension);

    /**
     * Set the name of the artifact that will be produced when the receiver
     * is built.
     */
    public void setArtifactName(String name);

    /**
     * Sets the arguments to be passed to the build utility used by the
     * receiver to produce a build goal.
     */
    public void setBuildArguments(String makeArgs);

    /**
     * Sets the build command for the receiver to the value in the argument.
     */
    public void setBuildCommand(String command);

    /**
     * Sets the prebuild step for the receiver to the value in the argument.
     */
    public void setPrebuildStep(String step);

    /**
     * Sets the postbuild step for the receiver to the value in the argument.
     */
    public void setPostbuildStep(String step);

    /**
     * Sets the prebuild step display string for the receiver to the value in the
     * argument.
     */
    public void setPreannouncebuildStep(String announceStep);

    /**
     * Sets the postbuild step display string for the receiver to the value in the
     * argument.
     */
    public void setPostannouncebuildStep(String announceStep);

    /**
     * Sets the command used to clean the outputs of this configuration.
     * 
     * @param command
     *            - the command to clean outputs
     */
    public void setCleanCommand(String command);


    /**
     * Sets the semicolon separated list of error parser ids
     */
    public void setErrorParserIds(String ids);

    public void setErrorParserList(String ids[]);

    /**
     * Sets the name of the receiver to the value specified in the argument
     */
    public void setName(String name);

    /**
     * Sets the value of a boolean option for this configuration.
     *
     * @param parent
     *            The holder/parent of the option.
     * @param option
     *            The option to change.
     * @param value
     *            The value to apply to the option.
     *
     * @return IOption The modified option. This can be the same option or a newly
     *         created option.
     *
     * @since 3.0 - The type of parent has changed from ITool to IHoldsOptions.
     *        Code assuming ITool as type, will continue to work unchanged.
     */
    public IOption setOption(IHoldsOptions parent, IOption option, boolean value) throws BuildException;

    /**
     * Sets the value of a string option for this configuration.
     *
     * @param parent
     *            The holder/parent of the option.
     * @param option
     *            The option that will be effected by change.
     * @param value
     *            The value to apply to the option.
     *
     * @return IOption The modified option. This can be the same option or a newly
     *         created option.
     *
     * @since 3.0 - The type of parent has changed from ITool to IHoldsOptions.
     *        Code assuming ITool as type, will continue to work unchanged.
     */
    public IOption setOption(IHoldsOptions parent, IOption option, String value) throws BuildException;

    /**
     * Sets the value of a list option for this configuration.
     *
     * @param parent
     *            The holder/parent of the option.
     * @param option
     *            The option to change.
     * @param value
     *            The values to apply to the option.
     *
     * @return IOption The modified option. This can be the same option or a newly
     *         created option.
     *
     * @since 3.0 - The type of parent has changed from ITool to IHoldsOptions.
     *        Code assuming ITool as type, will continue to work unchanged.
     */
    public IOption setOption(IHoldsOptions parent, IOption option, String[] value) throws BuildException;

    /**
     * Sets the rebuild state in this configuration.
     *
     * @param rebuild
     *            <code>true</code> will force a rebuild the next time the project
     *            builds
     */
    void setRebuildState(boolean rebuild);


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
     * answers true if the configuration is temporary, otherwise - false
     * 
     * @return boolean
     */
    public boolean isTemporary();

    /**
     * Returns whether this configuration requires a full rebuild
     *
     * @return boolean
     */
    public boolean needsFullRebuild();

    /**
     * Calculates the configuration target tool.
     *
     * @return ITool or null if not found
     *
     * @since 3.1
     */
    public ITool calculateTargetTool();

    /**
     * Returns a <code>ITool</code> for the tool associated with the
     * output extension.
     *
     * @param extension
     *            the file extension of the output file
     * @return ITool
     *
     * @since 3.1
     */
    public ITool getToolFromOutputExtension(String extension);

    /**
     * Returns a <code>ITool</code> for the tool associated with the
     * input extension.
     *
     * @param sourceExtension
     *            the file extension of the input file
     * @return ITool
     *
     * @since 3.1
     */
    public ITool getToolFromInputExtension(String sourceExtension);

    IResourceInfo getResourceInfo(IPath path, boolean exactPath);

    IResourceInfo[] getResourceInfos();

    IResourceInfo getResourceInfoById(String id);

    IFolderInfo getRootFolderInfo();

    IFileInfo createFileInfo(IPath path);

    IFileInfo createFileInfo(IPath path, String id, String name);

    IFileInfo createFileInfo(IPath path, IFolderInfo base, ITool baseTool, String id, String name);

    IFileInfo createFileInfo(IPath path, IFileInfo base, String id, String name);

    IFolderInfo createFolderInfo(IPath path);

    IFolderInfo createFolderInfo(IPath path, String id, String name);

    IFolderInfo createFolderInfo(IPath path, IFolderInfo base, String id, String name);

    CConfigurationData getConfigurationData();

    ICSourceEntry[] getSourceEntries();

    void setSourceEntries(ICSourceEntry[] entries);

    CBuildData getBuildData();

    IBuilder getBuilder();

    IBuilder getEditableBuilder();

    boolean isSystemObject();


    String getOutputFlag(String outputExt);

    String[] getUserObjects(String extension);

    String[] getLibs(String extension);

    boolean supportsBuild(boolean managed);

    boolean isManagedBuildOn();

    void setManagedBuildOn(boolean on) throws BuildException;

    boolean isBuilderCompatible(IBuilder builder);

    void changeBuilder(IBuilder newBuilder, String id, String name);

    IBuildPropertyValue getBuildArtefactType();

    void setBuildArtefactType(String id) throws BuildException;

    public void addResourceChangeState(int state);

}
