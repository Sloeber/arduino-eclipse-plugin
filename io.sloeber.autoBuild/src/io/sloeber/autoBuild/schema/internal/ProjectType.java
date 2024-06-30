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
package io.sloeber.autoBuild.schema.internal;

import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;

import io.sloeber.autoBuild.api.AutoBuildBuilderExtension;
import io.sloeber.autoBuild.api.IEnvironmentVariableProvider;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsProvider;
import io.sloeber.autoBuild.extensionPoint.IConfigurationNameProvider;
import io.sloeber.autoBuild.extensionPoint.IProjectBuildMacroSupplier;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.autoBuild.schema.api.IBuilder;
import io.sloeber.autoBuild.schema.api.IConfiguration;
import io.sloeber.autoBuild.schema.api.IOption;
import io.sloeber.autoBuild.schema.api.IProjectType;
import io.sloeber.autoBuild.schema.api.IToolChain;

public class ProjectType extends SchemaObject implements IProjectType {

	// read from model

	private String[] modelIsTest;
	private String[] modelEnvironmentVariableSupplier;
	private String[] modelBuildMacroSupplier;
	private String[] modelConfigurationNameProvider;
	private String[] modelBuildProperties;
	private String[] modelBuildArtifactType;
	private String[] modelBuilders;
	private String[] modelSupportedToolProviders;
	//private String[] modelBuilderExtension; not needed

	private Map<String, Configuration> myConfigMap = new HashMap<>();
	private Map<String, String> myProperties = new HashMap<>();
	private boolean myIsTest;

	private IConfigurationNameProvider myConfigurationNameProvider = null;
	private IEnvironmentVariableProvider myEnvironmentVariableProvider = null;
	private IProjectBuildMacroSupplier myBuildMacroSupplier = null;
	private String myExtensionPointID;
	private String myExtensionID;
	private ToolChain myToolchain;
	private Map<String, IBuilder> myBuilders = new HashMap<>();
	private IBuilder myDefaultBuilder;
	private AutoBuildBuilderExtension myBuilderExtension;
	private Set<String> mySupportedToolProviders=new HashSet<>();

	/*
	 * C O N S T R U C T O R S
	 */

	/**
	 * This constructor is called to create a projectType defined by an extension
	 * point in a plugin manifest file.
	 */
	public ProjectType(String extensionPointID, String extensionID, IExtensionPoint root,
			IConfigurationElement element) {
		myExtensionPointID = extensionPointID;
		myExtensionID = extensionID;
		loadNameAndID(root, element);
		modelBuildProperties = getAttributes(BUILD_PROPERTIES);
		modelBuildArtifactType = getAttributes(BUILD_ARTEFACT_TYPE);
		modelIsTest = getAttributes(IS_TEST);
		modelConfigurationNameProvider = getAttributes(CONFIGURATION_NAME_PROVIDER);
		modelEnvironmentVariableSupplier = getAttributes(PROJECT_ENVIRONMENT_SUPPLIER);
		modelBuildMacroSupplier = getAttributes(PROJECT_BUILD_MACRO_SUPPLIER);
		modelBuilders = getAttributes(PROJECT_BUILDERS);
		modelSupportedToolProviders= getAttributes(MODEL_TOOL_PROVIDERS);

		if(!modelSupportedToolProviders[SUPER].isBlank()) {
			mySupportedToolProviders.addAll(Arrays.asList(  modelSupportedToolProviders[SUPER].split(SEMICOLON)));
		}


		myIsTest=Boolean.valueOf(modelIsTest[SUPER]).booleanValue();

		myEnvironmentVariableProvider = (IEnvironmentVariableProvider) createExecutableExtension(
				PROJECT_ENVIRONMENT_SUPPLIER);
		myConfigurationNameProvider = (IConfigurationNameProvider) createExecutableExtension(
				CONFIGURATION_NAME_PROVIDER);
		myBuildMacroSupplier = (IProjectBuildMacroSupplier) createExecutableExtension(PROJECT_BUILD_MACRO_SUPPLIER);

		myBuilderExtension = (AutoBuildBuilderExtension) createExecutableExtension(BUILDER_EXTENSION);
		if(myBuilderExtension==null) {
			myBuilderExtension=new AutoBuildBuilderExtension();
		}

		// Load the toolchains first
		IConfigurationElement[] toolChainElements = element.getChildren(IToolChain.TOOL_CHAIN_ELEMENT_NAME);
		for (IConfigurationElement configElement : toolChainElements) {
			myToolchain = new ToolChain(this, root, configElement);
		}
		if (toolChainElements.length != 1) {
			System.err.println("ProjectType " + getName() + " should have exactly 1 toolchain but has " //$NON-NLS-1$//$NON-NLS-2$
					+ toolChainElements.length);
		}

		// Load the configuration children
		IConfigurationElement[] configs = element.getChildren(IConfiguration.CONFIGURATION_ELEMENT_NAME);
		for (IConfigurationElement config : configs) {
			Configuration newConfig = new Configuration(this, root, config);
			myConfigMap.put(newConfig.getName(), newConfig);
		}

		myProperties = parseProperties(modelBuildProperties[SUPER]);
		if (!modelBuildArtifactType[SUPER].isBlank()) {
			myProperties.put(BUILD_ARTEFACT_TYPE_PROPERTY_ID, modelBuildArtifactType[SUPER]);
		}

		if(modelBuilders[SUPER].isBlank()) {
			myBuilders=AutoBuildManager.getBuilders();
			myDefaultBuilder=AutoBuildManager.getDefaultBuilder();
		}else {
		for (String curBuilderID : modelBuilders[SUPER].split(SEMICOLON)) {
			IBuilder curBuilder = AutoBuildManager.getBuilder(curBuilderID);

			if (curBuilder == null) {
				System.err.println("Failed to find builder with ID " + curBuilder); //$NON-NLS-1$
			} else {
				if (myDefaultBuilder==null) {
					myDefaultBuilder=curBuilder;
				}
				myBuilders.put(curBuilder.getId(), curBuilder);
			}
		}
		}

	}


	@Override
	public IConfiguration getConfiguration(String id2) {
		return myConfigMap.get(id2);
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
		myConfigMap.put(configuration.getId(), configuration);
	}


	@Override
	public boolean isTest() {
		return myIsTest;
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

	@Override
	public IConfigurationNameProvider getConfigurationNameProvider() {
		return myConfigurationNameProvider;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.cdt.core.build.managed.IProjectType#
	 * getEnvironmentVariableSupplier()
	 */
	@Override
	public IEnvironmentVariableProvider getEnvironmentVariableProvider() {
		return myEnvironmentVariableProvider;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.cdt.managedbuilder.core.IProjectType#getBuildMacroSupplier()
	 */
	@Override
	public IProjectBuildMacroSupplier getBuildMacroSupplier() {
		return myBuildMacroSupplier;
	}

	public StringBuffer dump(int leadingChars) {
		StringBuffer ret = new StringBuffer();
		String prepend = DUMPLEAD.repeat(leadingChars);
		ret.append(prepend + PROJECTTYPE_ELEMENT_NAME + NEWLINE);
		ret.append(prepend + NAME + EQUAL + myName + NEWLINE);
		ret.append(prepend + ID + EQUAL + myID + NEWLINE);
		ret.append(prepend + BUILD_PROPERTIES + EQUAL + modelBuildProperties[SUPER] + NEWLINE);
		ret.append(prepend + BUILD_ARTEFACT_TYPE + EQUAL + modelBuildArtifactType[SUPER] + NEWLINE);
		ret.append(prepend + IS_TEST + EQUAL + modelIsTest[SUPER] + NEWLINE);
		ret.append(prepend + CONFIGURATION_NAME_PROVIDER + EQUAL + modelConfigurationNameProvider[SUPER]
				+ resolvedState(myConfigurationNameProvider) + NEWLINE);
		ret.append(prepend + PROJECT_ENVIRONMENT_SUPPLIER + EQUAL + modelEnvironmentVariableSupplier[SUPER]
				+ resolvedState(myEnvironmentVariableProvider) + NEWLINE);
		ret.append(prepend + PROJECT_BUILD_MACRO_SUPPLIER + EQUAL + modelBuildMacroSupplier[SUPER]
				+ resolvedState(myBuildMacroSupplier) + NEWLINE);
		ret.append(prepend + BEGIN_OF_CHILDREN + myConfigMap.size() + BLANK + IConfiguration.CONFIGURATION_ELEMENT_NAME
				+ NEWLINE);
		for (Configuration curConfig : myConfigMap.values()) {
			ret.append(curConfig.dump(leadingChars + 1));
			ret.append(NEWLINE);
		}
		ret.append(myToolchain.dump(leadingChars + 1));
		ret.append(prepend + END_OF_CHILDREN + BLANK + IConfiguration.CONFIGURATION_ELEMENT_NAME + NEWLINE);
		return ret;
	}

	@Override
	public Map<String, String> getDefaultBuildProperties() {
		return myProperties;
	}

	@Override
	public String getExtensionPointID() {
		return myExtensionPointID;
	}

	@Override
	public String getExtensionID() {
		return myExtensionID;
	}

	@Override
	public String getBuildArtifactType() {
		return modelBuildArtifactType[SUPER];
	}

	@Override
	public IToolChain getToolChain() {
		return myToolchain;
	}

	@Override
	public Map<String, IBuilder> getBuilders() {
		return myBuilders;
	}


	@Override
	public IBuilder getdefaultBuilder() {
		return myDefaultBuilder;
	}


	@Override
	public IOption getOption(String optionID) {
		return getToolChain().getOption( optionID);
	}

	@Override
	public AutoBuildBuilderExtension getBuilderExtension() {
		return myBuilderExtension;
	}


	@Override
	public boolean supportsToolProvider(IBuildToolsProvider buildToolsProvider) {
		if(mySupportedToolProviders.size()==0) {
			return true;
		}
		return mySupportedToolProviders.contains(buildToolsProvider.getID());
	}

	@Override
    public boolean equals (IProjectType other) {
    	return myID.equals(((ProjectType)other).myID);
    }
}
