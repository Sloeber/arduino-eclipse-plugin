/*******************************************************************************
 * Copyright (c) 2004, 2010 Intel Corporation and others.
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

import java.util.Map;

import io.sloeber.autoBuild.api.AutoBuildBuilderExtension;
import io.sloeber.autoBuild.api.IEnvironmentVariableProvider;
import io.sloeber.autoBuild.extensionPoint.IConfigurationNameProvider;
import io.sloeber.autoBuild.extensionPoint.IProjectBuildMacroSupplier;
import io.sloeber.buildTool.api.IBuildToolProvider;

/**
 * This class represents project-types in the Auto build system.
 * A project-type is a conceptual project (Like a library creating project) which
 * acts as a template for the projects that a user will create.
 * The project-type contains one or more children of type configuration.
 * These are the default configurations that the user can choose from.
 *
 * @since 2.1
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IProjectType extends ISchemaObject {

    /**
     * Returns all of the configurations defined by this project-type.
     *
     * @return IConfiguration[]
     */
    public IConfiguration[] getConfigurations();

    /**
     * Returns the configuration with the given id, or <code>null</code> if not
     * found.
     *
     * @param id
     *            The unique id of the configuration
     * @return IConfiguration
     */
    public IConfiguration getConfiguration(String id);


    /**
     * Returns <code>true</code> if the project-type is defined
     * for testing purposes only, else <code>false</code>. A test project-type will
     * not be shown in the UI but can still be manipulated programmatically.
     * Returns <code>false</code> if the attribute was not specified.
     *
     * @return boolean
     */
    public boolean isTest();

    /**
     * Returns <code>true</code> if at least one project-type contiguration is
     * supported on the system
     * otherwise returns <code>false</code>
     *
     * @return boolean
     */
    public boolean isSupported();

    /**
     * Returns the configurationNameProvider.
     *
     * @return IConfigurationNameProvider
     */
    public IConfigurationNameProvider getConfigurationNameProvider();

    /**
     * Returns the tool-integrator provided implementation of the project
     * environment variable supplier
     * or <code>null</code> if none.
     *
     * @return IProjectEnvironmentVariableSupplier
     */
    public IEnvironmentVariableProvider getEnvironmentVariableProvider();

    /**
     * Returns the tool-integrator provided implementation of the project build
     * macro supplier
     * or <code>null</code> if none.
     *
     * @return IProjectBuildMacroSupplier
     */
    public IProjectBuildMacroSupplier getBuildMacroSupplier();

    /**
     * return the extensionPointID that describes this XML model that describes this
     * configuration
     *
     * @return the extensionPointID of the XML
     */
    public String getExtensionPointID();

    /**
     * return the extensiontID that describes this configuration
     *
     * @return the extensionID of the model
     */
    public String getExtensionID();

    public Map<String, String> getDefaultBuildProperties();

    public String getBuildArtifactType();

    /**
     * Returns the <code>IToolChain</code> child of this project template.
     *
     * @return IToolChain
     */
    public IToolChain getToolChain();

	public Map<String, IBuilder> getBuilders();
	public boolean supportsToolProvider(IBuildToolProvider buildToolsProvider);
	public AutoBuildBuilderExtension getBuilderExtension();

	public IBuilder getdefaultBuilder();

	public IOption getOption(String optionID);

}
