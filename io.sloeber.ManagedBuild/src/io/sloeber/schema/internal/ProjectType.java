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
package io.sloeber.schema.internal;

import static io.sloeber.autoBuild.integration.Const.*;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;

import io.sloeber.autoBuild.api.IEnvironmentVariableSupplier;
import io.sloeber.autoBuild.extensionPoint.IConfigurationNameProvider;
import io.sloeber.autoBuild.extensionPoint.IProjectBuildMacroSupplier;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IProjectType;

public class ProjectType extends SchemaObject implements IProjectType {

    // read from model
    private String[] modelBuildProperties;
    private String[] modelArtifactType;
    private String[] modelIsAbstract;
    private String[] modelIsTest;
    private String[] modelConfigurationNameProvider;
    private String[] modelEnvironmentVariableSupplier;
    private String[] modelBuildMacroSupplier;

    // Parent and children
    private Map<String, Configuration> myConfigMap = new HashMap<>();
    // Managed Build model attributes
    private boolean myIsAbstract;
    private boolean myIsTest;

    private IConfigurationNameProvider myConfigurationNameProvider = null;
    private IEnvironmentVariableSupplier myEnvironmentVariableSupplier = null;
    private IProjectBuildMacroSupplier myBuildMacroSupplier = null;

    /*
     * C O N S T R U C T O R S
     */

    /**
     * This constructor is called to create a projectType defined by an extension
     * point in a plugin manifest file.
     */
    public ProjectType(IExtensionPoint root, IConfigurationElement element) {

        loadNameAndID(root, element);
        modelBuildProperties = getAttributes(BUILD_PROPERTIES);
        modelArtifactType = getAttributes(BUILD_ARTEFACT_TYPE);
        modelIsAbstract = getAttributes(IS_ABSTRACT);
        modelIsTest = getAttributes(IS_TEST);
        modelConfigurationNameProvider = getAttributes(CONFIGURATION_NAME_PROVIDER);
        modelEnvironmentVariableSupplier = getAttributes(PROJECT_ENVIRONMENT_SUPPLIER);
        modelBuildMacroSupplier = getAttributes(PROJECT_BUILD_MACRO_SUPPLIER);

        myEnvironmentVariableSupplier = (IEnvironmentVariableSupplier) createExecutableExtension(
                PROJECT_ENVIRONMENT_SUPPLIER);
        myConfigurationNameProvider = (IConfigurationNameProvider) createExecutableExtension(
                CONFIGURATION_NAME_PROVIDER);
        myBuildMacroSupplier = (IProjectBuildMacroSupplier) createExecutableExtension(PROJECT_BUILD_MACRO_SUPPLIER);

        // Load the configuration children
        IConfigurationElement[] configs = element.getChildren(IConfiguration.CONFIGURATION_ELEMENT_NAME);
        for (IConfigurationElement config : configs) {
            Configuration newConfig = new Configuration(this, root, config);
            myConfigMap.put(newConfig.getName(), newConfig);
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
    public boolean isAbstract() {
        return myIsAbstract;
    }

    @Override
    public boolean isTestProjectType() {
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
    public IEnvironmentVariableSupplier getEnvironmentVariableSupplier() {
        return myEnvironmentVariableSupplier;
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
        String prepend = StringUtils.repeat(DUMPLEAD, leadingChars);
        ret.append(prepend + PROJECTTYPE_ELEMENT_NAME + NEWLINE);
        ret.append(prepend + NAME + EQUAL + myName + NEWLINE);
        ret.append(prepend + ID + EQUAL + myID + NEWLINE);
        ret.append(prepend + BUILD_PROPERTIES + EQUAL + modelBuildProperties[SUPER] + NEWLINE);
        ret.append(prepend + BUILD_ARTEFACT_TYPE + EQUAL + modelArtifactType[SUPER] + NEWLINE);
        ret.append(prepend + IS_ABSTRACT + EQUAL + modelIsAbstract[SUPER] + NEWLINE);
        ret.append(prepend + IS_TEST + EQUAL + modelIsTest[SUPER] + NEWLINE);
        ret.append(prepend + CONFIGURATION_NAME_PROVIDER + EQUAL + modelConfigurationNameProvider[SUPER]
                + resolvedState(myConfigurationNameProvider) + NEWLINE);
        ret.append(prepend + PROJECT_ENVIRONMENT_SUPPLIER + EQUAL + modelEnvironmentVariableSupplier[SUPER]
                + resolvedState(myEnvironmentVariableSupplier) + NEWLINE);
        ret.append(prepend + PROJECT_BUILD_MACRO_SUPPLIER + EQUAL + modelBuildMacroSupplier[SUPER]
                + resolvedState(myBuildMacroSupplier) + NEWLINE);
        ret.append(prepend + BEGIN_OF_CHILDREN + myConfigMap.size() + BLANK + IConfiguration.CONFIGURATION_ELEMENT_NAME
                + NEWLINE);
        for (Configuration curConfig : myConfigMap.values()) {
            ret.append(curConfig.dump(leadingChars + 1));
            ret.append(NEWLINE);
        }
        ret.append(prepend + END_OF_CHILDREN + BLANK + IConfiguration.CONFIGURATION_ELEMENT_NAME + NEWLINE);
        return ret;
    }

    @Override
    public Map<String, String> getDefaultBuildProperties() {
        return parseProperties(modelBuildProperties[ORIGINAL]);
    }
}
