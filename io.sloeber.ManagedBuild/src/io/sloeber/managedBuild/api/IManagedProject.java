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
package io.sloeber.managedBuild.api;

import org.eclipse.core.resources.IResource;

/**
 * This class represents a project instance in the managed build system.
 * Project instances are stored in the .cdtbuild file.  Note that there
 * is no reason to define a project element in a manifest file - it
 * would never be used.
 * <p>
 * The following steps occur when a CDT user creates a new Managed Build
 * project:
 * 1. A new project element is created.  Its projectType attribute is set
 *    to the projectType that the user selected.  Its name attribute is
 *    set to the project name that the user entered.
 * 2. When the user adds a default configuration, a configuration
 *    element is created as a child of the project element created in
 *    step 1.
 * 3. Add a tool-chain element that specifies as its superClass the
 *    tool-chain that is the child of the selected configuration element.
 * 4. For each tool element child of the tool-chain that is the child of
 *    the selected configuration element, create a tool element child of
 *    the cloned configuration's tool-chain element that specifies the
 *    original tool element as its superClass.
 * This prepares the new project/configurations for modification by the user.
 *
 * @since 2.1
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IManagedProject
		extends IBuildObject, IBuildObjectPropertiesContainer, IOptionalBuildObjectPropertiesContainer {
	public static final String MANAGED_PROJECT_ELEMENT_NAME = "project"; //$NON-NLS-1$
	public static final String PROJECTTYPE = "projectType"; //$NON-NLS-1$
	public static final String BUILD_PROPERTIES = "buildProperties"; //$NON-NLS-1$
	/**
	 * @since 8.6
	 */
	public static final String OPTIONAL_BUILD_PROPERTIES = "optionalBuildProperties"; //$NON-NLS-1$
	public static final String BUILD_ARTEFACT_TYPE = "buildArtefactType"; //$NON-NLS-1$

	/**
	 * Creates a configuration for this project populated with the tools
	 * and options settings from the parent configuration.  As options and
	 * tools change in the parent, unoverridden values are updated in the
	 * child configuration as well.
	 * <p>
	 * This method performs steps 3 & 4 described above.
	 *
	 * @param parent The <code>IConfiguration</code> to use as a settings template
	 * @param id The unique id the new configuration will have
	 * @return IConfiguration of the new configuration
	 */
	public IConfiguration createConfiguration(IConfiguration parent, String id);

	/**
	 * Creates a configuration for this project populated with the tools
	 * and options settings from the parent configuration.  As opposed to the
	 * <code>createConfiguration</code> method, this method creates a configuration
	 * from an existing configuration in the project.
	 * <p>
	 * In this case, the new configuration is cloned from the existing configuration,
	 * and does not retain a pointer to the existing configuration.
	 *
	 * @param parent The <code>IConfiguration</code> to clone
	 * @param id The unique id the new configuration will have
	 * @return IConfiguration of the new configuration
	 */
	public IConfiguration createConfigurationClone(IConfiguration parent, String id);

	/**
	 * Removes the configuration with the ID specified in the argument.
	 *
	 * @param id The unique id of the configuration
	 */
	public void removeConfiguration(String id);

	/**
	 * Returns all of the configurations defined by this project-type.
	 *
	 * @return IConfiguration[]
	 */
	public IConfiguration[] getConfigurations();

	/**
	 * Returns the configuration with the given id, or <code>null</code> if not found.
	 *
	 * @param id The unique id of the configuration
	 * @return IConfiguration
	 */
	public IConfiguration getConfiguration(String id);

	/**
	 * Answers the <code>IProjectType</code> that is the superclass of this
	 * project-type, or <code>null</code> if the attribute was not specified.
	 *
	 * @return IProjectType
	 */
	//public IProjectType getProjectType();

	/**
	 * Returns the owner of the managed project (an IProject).
	 *
	 * @return IResource
	 */
	public IResource getOwner();

	/**
	 * Sets the owner of the managed project.
	 */
	public void updateOwner(IResource resource);

	/**
	 * Returns <code>true</code> if this project has changes that need to
	 * be saved in the project file, else <code>false</code>.
	 *
	 * @return boolean
	 */
	public boolean isDirty();

	/**
	 * Sets the element's "dirty" (have I been modified?) flag.
	 */
	public void setDirty(boolean isDirty);

	/**
	 * Returns <code>true</code> if this project is valid
	 * else <code>false</code>.
	 *
	 * @return boolean
	 */
	public boolean isValid();

	/**
	 * Sets the element's "Valid" flag.
	 */
	public void setValid(boolean isValid);

	//	/**
	//	 * Persist the managed project to the project file (.cdtbuild).
	//	 *
	//	 * @param doc
	//	 * @param element
	//	 */
	//	public void serialize(Document doc, Element element);

	/**
	 * Returns the default build artifact name for the project
	 *
	 * @return String
	 */
	public String getDefaultArtifactName();
}
