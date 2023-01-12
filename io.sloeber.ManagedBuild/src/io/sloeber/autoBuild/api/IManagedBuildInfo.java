/*******************************************************************************
 * Copyright (c) 2003, 2010 Rational Software Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Rational Software - Initial API and implementation
 *******************************************************************************/
package io.sloeber.autoBuild.api;

import java.util.List;
import java.util.Set;

//import org.eclipse.cdt.managedbuilder.internal.core.ManagedBuildInfo;
//import org.eclipse.cdt.managedbuilder.makegen.IManagedDependencyGeneratorType;
import org.eclipse.core.runtime.IPath;

import io.sloeber.autoBuild.Internal.ManagedBuildInfo;

/**
 * There is a ManagedBuildInfo per CDT managed build project. Here are
 * some notes on their usage:
 * o You can look up the managed build info associated with a CDT
 * project by using ManagedBuildManager.getBuildInfo(IProject).
 * o Given a ManagedBuildInfo, you can retrieve the associated CDT
 * managed build system project by using getManagedProject.
 * o The usage model of a ManagedBuildInfo is:
 * 1. Call setDefaultConfiguration to set the context
 * 2. Call other methods (e.g. getBuildArtifactName) which get
 * information from the default configuration, and the other managed
 * build system model elements that can be reached from the
 * configuration.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IManagedBuildInfo {
    public static final String DEFAULT_CONFIGURATION = "defaultConfig"; //$NON-NLS-1$
    public static final String DEFAULT_TARGET = "defaultTarget"; //$NON-NLS-1$

    /*
     * Note:  "Target" routines are only currently applicable when loading a CDT 2.0
     *        or earlier managed build project file (.cdtbuild)
     */


    /**
     * Answers a <code>String</code> containing the arguments to be passed to make.
     * For example, if the user has selected a build that keeps going on error, the
     * answer would contain {"-k"}.
     *
     * @return String
     */
    public String getBuildArguments();

    /**
     * Answers the file extension for the receivers build goal without a separator.
     *
     * @return the extension or an empty string if none is defined
     */
    public String getBuildArtifactExtension();

    /**
     * Returns the name of the artifact to build for the receiver.
     *
     * @return Name of the build artifact
     */
    public String getBuildArtifactName();

    /**
     * Answers a <code>String</code> containing the make command invocation
     * for the default configuration.
     * 
     * @return build command
     */
    public String getBuildCommand();

    /**
     * Answers the prebuild step for the default configuration
     *
     * @return String
     */
    public String getPrebuildStep();

    /**
     * Answers the postbuild step for the default configuration
     *
     * @return String
     */
    public String getPostbuildStep();

    /**
     * Answers the display string associated with the prebuild step for the default
     * configuration
     *
     * @return String
     */
    public String getPreannouncebuildStep();

    /**
     * Answers the display string associated with the postbuild step for the default
     * configuration
     *
     * @return String
     */
    public String getPostannouncebuildStep();

    /**
     * Answers the command needed to remove files on the build machine
     */
    public String getCleanCommand();

    /**
     * Answers the name of the default configuration, for example <code>Debug</code>
     * or <code>Release</code>.
     *
     * @return String name of default configuration
     */
    public String getConfigurationName();

    /**
     * Answers a <code>String</code> array containing the names of all the
     * configurations
     * defined for the project.
     *
     * @return String[] of configuration names
     */
    public Set<String> getConfigurationNames();

    /**
     * Get the default configuration associated with the receiver
     *
     * @return IConfiguration default
     */
    public IConfiguration getDefaultConfiguration();

    //	public IManagedDependencyGeneratorType getDependencyGenerator(String sourceExtension);


    /**
     * Returns the ManagedProject associated with this build info
     *
     * @return IManagedProject
     */
    public IManagedProject getManagedProject();



    /**
     * Answers the flag to be passed to the build tool to produce a specific output
     * or an empty <code>String</code> if there is no special flag. For example, the
     * GCC tools use the '-o' flag to produce a named output, for example
     * gcc -c foo.c -o foo.o
     */
    public String getOutputFlag(String outputExt);



    /**
     * Returns the currently selected configuration. This is used while the project
     * property pages are displayed
     *
     * @return IConfiguration
     */
    public IConfiguration getSelectedConfiguration();

    /**
     * Returns a <code>String</code> containing the command-line invocation
     * for the tool associated with the output extension.
     *
     * @param extension
     *            the file extension of the output file
     * @return a String containing the command line invocation for the tool
     */
    public String getToolForConfiguration(String extension);





    /**
     * Answers the version of the build information in the format
     * 
     * @return a <code>String</code> containing the build information
     *         version
     */
    public String getVersion();


    /**
     * Answers <code>true</code> if the extension matches one of the special
     * file extensions the tools for the configuration consider to be a header file.
     *
     * @param ext
     *            the file extension of the resource
     * @return boolean
     */
    public boolean isHeaderFile(String ext);


    /**
     * Gets the "valid" status of Managed Build Info. Managed Build Info is invalid
     * if the loading of, or conversion to, the Managed Build Info failed.
     *
     * @return <code>true</code> if Managed Build Info is valid,
     *         otherwise returns <code>false</code>
     */
    public boolean isValid();

    /**
     * Answers whether the receiver has been changed and requires the
     * project to be rebuilt. When a project is first created, it is
     * assumed that the user will need it to be fully rebuilt. However
     * only option and tool command changes will trigger the build
     * information for an existing project to require a rebuild.
     * <p>
     * Clients can reset the state to force or clear the rebuild status
     * using <code>setRebuildState()</code>
     * 
     * @see ManagedBuildInfo#setRebuildState(boolean)
     *
     * @return <code>true</code> if the resource managed by the
     *         receiver needs to be rebuilt
     */
    public boolean needsRebuild();

    /**
     * Set the primary configuration for the receiver.
     *
     * @param configuration
     *            The <code>IConfiguration</code> that will be used as the default
     *            for all building.
     */
    public void setDefaultConfiguration(IConfiguration configuration);

    /**
     * @return boolean indicating if setDefaultConfiguration was successful
     */
    public boolean setDefaultConfiguration(String configName);



    /**
     * Sets the valid flag for the build model to the value of the argument.
     */
    public void setValid(boolean isValid);

    /**
     * Sets the ManagedProject associated with this build info
     */
    public void setManagedProject(IManagedProject project);


    /**
     * Sets the rebuild state in the receiver to the value of the argument.
     * This is a potentially expensive option, so setting it to true should
     * only be done if a project resource or setting has been modified in a
     * way that would invalidate the previous build.
     *
     * @param rebuild
     *            <code>true</code> will force a rebuild the next time the project
     *            builds
     */
    public void setRebuildState(boolean rebuild);

    /**
     * Sets the currently selected configuration. This is used while the project
     * property pages are displayed
     *
     * @param configuration
     *            the user selection
     */
    public void setSelectedConfiguration(IConfiguration configuration);
}
