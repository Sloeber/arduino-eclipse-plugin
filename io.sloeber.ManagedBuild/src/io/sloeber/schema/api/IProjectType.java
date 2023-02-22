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

import io.sloeber.autoBuild.api.IEnvironmentVariableSupplier;
import io.sloeber.autoBuild.extensionPoint.IConfigurationNameProvider;
import io.sloeber.autoBuild.extensionPoint.IProjectBuildMacroSupplier;

/**
 * This class represents project-types in the managed build system.
 * A project-type is a tool-integrator defined class of project which
 * acts as a template for the projects that a user will create.
 * The project-type contains one or more children of type configuration.
 * These are the default configurations that the user can choose from.
 * Note that there is no reason to define a project-type element in a
 * .cdtbuild file. It would never be used since project-type elements
 * are used to primarily populate the "New Project" dialog boxes.
 * Project types can be arranged into hierarchies to promote the efficient
 * sharing of configurations. If you have defined a project type that
 * should not be selected by the user, but is a root for other project
 * types, it may be declared abstract by setting the isAbstract attribute
 * to 'true'. Abstract project types do not appear in the UI. You must
 * provide a unique identifier for the project type in the id attribute.
 * Children of the abstract project type will have the same configurations
 * that the abstract project type has, unless they are explicitly named
 * in the unusedChildren attribute of the child project. For these
 * children to function properly, their superClass attribute must contain
 * the unique identifier of the super class project type.
 * A concrete project type must have at least one configuration defined
 * for it. A project type must also define (or inherit) a set of tool-chain
 * definitions that work together to produce the build goal as an output.
 * You must also provide a meaningful name that will be displayed to the
 * user in the UI and New Project wizards.
 *
 * @since 2.1
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IProjectType extends ISchemaObject {
    public static final String PROJECTTYPE_ELEMENT_NAME = "projectType"; //$NON-NLS-1$

    public static final String IS_TEST = "isTest"; //$NON-NLS-1$
    public static final String PROJECT_ENVIRONMENT_SUPPLIER = "environmentSupplier"; //$NON-NLS-1$
    public static final String PROJECT_BUILD_MACRO_SUPPLIER = "buildMacroSupplier"; //$NON-NLS-1$
    public static final String CONFIGURATION_NAME_PROVIDER = "configurationNameProvider"; //$NON-NLS-1$
    public static final String BUILD_PROPERTIES = "buildProperties"; //$NON-NLS-1$
    public static final String BUILD_ARTEFACT_TYPE = "buildArtefactType"; //$NON-NLS-1$

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
     * Returns whether this element is abstract. Returns <code>false</code>
     * if the attribute was not specified.
     * 
     * @return boolean
     */
    public boolean isAbstract();

    /**
     * Returns <code>true</code> if the project-type is defined
     * for testing purposes only, else <code>false</code>. A test project-type will
     * not be shown in the UI but can still be manipulated programmatically.
     * Returns <code>false</code> if the attribute was not specified.
     *
     * @return boolean
     */
    public boolean isTestProjectType();

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
    public IEnvironmentVariableSupplier getEnvironmentVariableSupplier();

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

    Map<String, String> getDefaultBuildProperties();

}
