/*******************************************************************************
 * Copyright (c) 2004, 2016 Intel Corporation and others.
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
 * IBM Corporation
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.api.IEnvironmentVariableSupplier;
import io.sloeber.autoBuild.api.IProjectType;
import io.sloeber.autoBuild.extensionPoint.IConfigurationNameProvider;
import io.sloeber.autoBuild.extensionPoint.IProjectBuildMacroSupplier;

public class ProjectType extends BuildObject
		implements IProjectType {

	private static int ORIGINAL = 0;
	private static int SUPER = 1;
	// read from model
	private String modelSuperClass;
	private String[] modelBuildProperties = new String[2];
	private String[] modelArtifactType = new String[2];
	private String[] modelIsAbstract = new String[2];
	private String[] modelIsTest = new String[2];
	private String[] modelConfigurationNameProvider = new String[2];
	private String[] modelEnvironmentVariableSupplier = new String[2];
	private String[] modelBuildMacroSupplier = new String[2];

	// Parent and children
	private Map<String, Configuration> myConfigMap = new HashMap<>();
	// Managed Build model attributes
	private Boolean myIisAbstract;
	private Boolean myIsTest;
	private ProjectType mySuperClass; // TOFIX JABA Should be deleted

	private IConfigurationElement myConfigurationNameProviderElement = null;
	private IConfigurationNameProvider myConfigurationNameProvider = null;

	private IConfigurationElement myEnvironmentVariableSupplierElement = null;
	private IEnvironmentVariableSupplier myEnvironmentVariableSupplier = null;
	private IConfigurationElement myBuildMacroSupplierElement = null;
	private IProjectBuildMacroSupplier myBuildMacroSupplier = null;

	IConfigurationElement myElement;

	/*
	 * C O N S T R U C T O R S
	 */

	/**
	 * This constructor is called to create a projectType defined by an extension
	 * point in a plugin manifest file.
	 */
	public ProjectType(IConfigurationElement element, String managedBuildRevision) {

		setManagedBuildRevision(managedBuildRevision);

//        ManagedBuildManager.putConfigElement(this, element);

		myElement = element;
		id = element.getAttribute(ID);
		name = element.getAttribute(NAME);
		modelSuperClass = element.getAttribute(SUPERCLASS);
		modelBuildProperties[ORIGINAL] = element.getAttribute(BUILD_PROPERTIES);
		modelArtifactType[ORIGINAL] = element.getAttribute(BUILD_ARTEFACT_TYPE);
		modelIsAbstract[ORIGINAL] = element.getAttribute(IS_ABSTRACT);
		modelIsTest[ORIGINAL] = element.getAttribute(IS_TEST);
		modelConfigurationNameProvider[ORIGINAL] = element.getAttribute(CONFIGURATION_NAME_PROVIDER);
		modelEnvironmentVariableSupplier[ORIGINAL] = element.getAttribute(PROJECT_ENVIRONMENT_SUPPLIER);
		modelBuildMacroSupplier[ORIGINAL] = element.getAttribute(PROJECT_MACRO_SUPPLIER);

		
		if (modelSuperClass != null && !modelSuperClass.isEmpty()) {
			// TOFIX JABA should not use this method
			mySuperClass = (ProjectType) ManagedBuildManager.getExtensionProjectType(modelSuperClass);
			if (mySuperClass == null) {
				// Report error
			}
		}
		
		// Load the configuration children
		IConfigurationElement[] configs = element.getChildren(IConfiguration.CONFIGURATION_ELEMENT_NAME);
		for (IConfigurationElement config : configs) {
			Configuration newConfig = new Configuration(this, config, managedBuildRevision);
			myConfigMap.put(newConfig.getName(), newConfig);
		}
		if (configs.length == 0) {
			// Add configurations from our superClass that are not overridden here
			if (mySuperClass != null) {
				configs = mySuperClass.myElement.getChildren(IConfiguration.CONFIGURATION_ELEMENT_NAME);
				for (IConfigurationElement config : configs) {
					Configuration newConfig = new Configuration(this, config, managedBuildRevision);
					myConfigMap.put(newConfig.getName(), newConfig);
				}
			}


		}

	}

	@Override
	public void resolveSuperClass()  throws Exception{
		modelBuildProperties[SUPER] = modelBuildProperties[ORIGINAL];
		modelArtifactType[SUPER] = modelArtifactType[ORIGINAL];
		modelIsAbstract[SUPER] = modelIsAbstract[ORIGINAL];
		modelIsTest[SUPER] = modelIsTest[ORIGINAL];
		modelConfigurationNameProvider[SUPER] = modelConfigurationNameProvider[ORIGINAL];
		modelEnvironmentVariableSupplier[SUPER] = modelEnvironmentVariableSupplier[ORIGINAL];
		modelBuildMacroSupplier[SUPER] = modelBuildMacroSupplier[ORIGINAL];

		if (mySuperClass != null) {

			if (modelBuildProperties[SUPER] != null)
				modelBuildProperties[SUPER] = mySuperClass.modelBuildProperties[ORIGINAL];
			if (modelArtifactType[SUPER] != null)
				modelArtifactType[SUPER] = mySuperClass.modelArtifactType[ORIGINAL];
			if (modelIsAbstract[SUPER] != null)
				modelIsAbstract[SUPER] = mySuperClass.modelIsAbstract[ORIGINAL];
			if (modelIsTest[SUPER] != null)
				modelIsTest[SUPER] = mySuperClass.modelIsTest[ORIGINAL];
			if (modelConfigurationNameProvider[SUPER] != null)
				modelConfigurationNameProvider[SUPER] = mySuperClass.modelConfigurationNameProvider[ORIGINAL];
			if (modelEnvironmentVariableSupplier[SUPER] != null)
				modelEnvironmentVariableSupplier[SUPER] = mySuperClass.modelEnvironmentVariableSupplier[ORIGINAL];
			if (modelBuildMacroSupplier[SUPER] != null)
				modelBuildMacroSupplier[SUPER] = mySuperClass.modelBuildMacroSupplier[ORIGINAL];

		}

		// Call resolve references on any children
		for (Configuration current : myConfigMap.values()) {
			 current.resolveSuperClass();
		}
	}

	@Override
	public void resolveFields() throws Exception {

		IConfigurationElement element = getEnvironmentVariableSupplierElement();
		if (myEnvironmentVariableSupplierElement != null) {
			try {
				if (myEnvironmentVariableSupplierElement.getAttribute(PROJECT_ENVIRONMENT_SUPPLIER) != null) {
					myEnvironmentVariableSupplier = (IEnvironmentVariableSupplier) myEnvironmentVariableSupplierElement
							.createExecutableExtension(PROJECT_ENVIRONMENT_SUPPLIER);
				}
			} catch (CoreException e) {
			}
		}

		if (modelIsAbstract != null) {
			myIisAbstract = Boolean.parseBoolean(modelIsAbstract[ORIGINAL]);
		}
		if (modelIsTest != null) {
			myIsTest = Boolean.parseBoolean(modelIsTest[ORIGINAL]);
		}

		if (modelConfigurationNameProvider != null && element instanceof DefaultManagedConfigElement) {
			myConfigurationNameProviderElement = ((DefaultManagedConfigElement) element).getConfigurationElement();
		}

		if (modelEnvironmentVariableSupplier != null && element instanceof DefaultManagedConfigElement) {
			myEnvironmentVariableSupplierElement = ((DefaultManagedConfigElement) element).getConfigurationElement();
		}

		if (modelBuildMacroSupplier != null && element instanceof DefaultManagedConfigElement) {
			myBuildMacroSupplierElement = ((DefaultManagedConfigElement) element).getConfigurationElement();
		}

		if (myConfigurationNameProviderElement != null) {
			if (myConfigurationNameProviderElement.getAttribute(CONFIGURATION_NAME_PROVIDER) != null) {
				myConfigurationNameProvider = (IConfigurationNameProvider) myConfigurationNameProviderElement
						.createExecutableExtension(CONFIGURATION_NAME_PROVIDER);
			}
		}
		
		// Call resolve references on any children
		for (Configuration current : myConfigMap.values()) {
			 current.resolveFields();
		}
	}

	/*
	 * P A R E N T A N D C H I L D H A N D L I N G
	 */

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.core.build.managed.IProjectType#getConfiguration()
	 */
	@Override
	public IConfiguration getConfiguration(String id) {
		return (IConfiguration)myConfigMap.get(id);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.managedbuilder.core.IProjectType#getConfigurations()
	 */
	@Override
	public IConfiguration[] getConfigurations() {
		return myConfigMap.values().toArray(new IConfiguration[myConfigMap.size()]);
	}

	/**
	 * Adds the Configuration to the Configuration list and map
	 */
	public void addConfiguration(Configuration configuration) {
		if (!configuration.isTemporary()) {
			myConfigMap.put(configuration.getId(), configuration);
		}
	}

	/*
	 * M O D E L A T T R I B U T E A C C E S S O R S
	 */

	@Override
	public IProjectType getSuperClass() {
		return mySuperClass;
	}

	@Override
	public boolean isAbstract() {
		return myIisAbstract;
	}

	@Override
	public boolean isTestProjectType() {
		return myIsTest;
	}

	/*
	 * O B J E C T S T A T E M A I N T E N A N C E
	 */

	/**
	 * Resolve the element IDs to interface references
	 */
	public void resolveReferences() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.core.build.managed.IProjectType#isSupported()
	 */
	@Override
	public boolean isSupported() {
		for (Configuration current : myConfigMap.values()) {
			if (!current.isSupported())
				return false;
		}
		return true;
	}

	public IConfigurationElement getConfigurationNameProviderElement() {
		return myConfigurationNameProviderElement;
	}

	public void setConfigurationNameProviderElement(IConfigurationElement configurationElement) {
		myConfigurationNameProviderElement = configurationElement;
	}

	@Override
	public IConfigurationNameProvider getConfigurationNameProvider() {
		return myConfigurationNameProvider;
	}

	/**
	 * Returns the plugin.xml element of the projectEnvironmentSupplier extension or
	 * <code>null</code> if none.
	 *
	 * @return IConfigurationElement
	 */
	public IConfigurationElement getEnvironmentVariableSupplierElement() {
		if (myEnvironmentVariableSupplierElement == null) {
			if (mySuperClass != null && mySuperClass instanceof ProjectType) {
				return ((ProjectType) mySuperClass).getEnvironmentVariableSupplierElement();
			}
		}
		return myEnvironmentVariableSupplierElement;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.core.build.managed.IProjectType#
	 * getEnvironmentVariableSupplier()
	 */
	@Override
	public IEnvironmentVariableSupplier getEnvironmentVariableSupplier() {
		if (myEnvironmentVariableSupplier != null) {
			return myEnvironmentVariableSupplier;
		}

		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.cdt.managedbuilder.core.IProjectType#getBuildMacroSupplier()
	 */
	@Override
	public IProjectBuildMacroSupplier getBuildMacroSupplier() {
		if (myBuildMacroSupplier != null) {
			return myBuildMacroSupplier;
		}
		if (myBuildMacroSupplierElement != null) {
			try {
				if (myBuildMacroSupplierElement.getAttribute(PROJECT_MACRO_SUPPLIER) != null) {
					myBuildMacroSupplier = (IProjectBuildMacroSupplier) myBuildMacroSupplierElement
							.createExecutableExtension(PROJECT_MACRO_SUPPLIER);
					return myBuildMacroSupplier;
				}
			} catch (CoreException e) {
			}
		}
		return null;
	}

	/*
	 * This function checks for migration support for the projectType while loading
	 * the project. If migration support is needed, looks for the available
	 * converters and adds them to the list.
	 */

	@Override
	public boolean checkForMigrationSupport() {
		return false;
	}

}
