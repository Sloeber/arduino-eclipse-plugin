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
import org.eclipse.core.runtime.IExtensionPoint;

import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.api.IEnvironmentVariableSupplier;
import io.sloeber.autoBuild.api.IProjectType;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IConfigurationNameProvider;
import io.sloeber.autoBuild.extensionPoint.IProjectBuildMacroSupplier;

public class ProjectType extends BuildObject implements IProjectType {

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
    private boolean myIisAbstract;
    private boolean myIsTest;

    private IConfigurationElement myConfigurationNameProviderElement = null;
    private IConfigurationNameProvider myConfigurationNameProvider = null;

    private IConfigurationElement myEnvironmentVariableSupplierElement = null;
    private IEnvironmentVariableSupplier myEnvironmentVariableSupplier = null;
    private IConfigurationElement myBuildMacroSupplierElement = null;
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
        modelBuildMacroSupplier = getAttributes(PROJECT_MACRO_SUPPLIER);

        // Load the configuration children
        IConfigurationElement[] configs = element.getChildren(IConfiguration.CONFIGURATION_ELEMENT_NAME);
        for (IConfigurationElement config : configs) {
            Configuration newConfig = new Configuration(this, root, config);
            myConfigMap.put(newConfig.getName(), newConfig);
        }
        //TOFIX JABA super class confiigs are not handled
        //        if (configs.length == 0) {
        //            // Add configurations from our superClass that are not overridden here
        //            if (myConfigurationSuperClassElement != null) {
        //                configs = myConfigurationSuperClassElement.myConfigurationElement.getChildren(IConfiguration.CONFIGURATION_ELEMENT_NAME);
        //                for (IConfigurationElement config : configs) {
        //                    Configuration newConfig = new Configuration(this, root, config);
        //                    myConfigMap.put(newConfig.getName(), newConfig);
        //                }
        //            }
        //
        //        }
        try {
            resolveFields();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void resolveFields() throws Exception {

        myIisAbstract = Boolean.parseBoolean(modelIsAbstract[SUPER]);
        myIsTest = Boolean.parseBoolean(modelIsTest[SUPER]);

        if (modelEnvironmentVariableSupplier[SUPER] != null) {
            try {
                //TOFIX JABA something is not right here
                if (myEnvironmentVariableSupplierElement.getAttribute(PROJECT_ENVIRONMENT_SUPPLIER) != null) {
                    myEnvironmentVariableSupplier = (IEnvironmentVariableSupplier) myEnvironmentVariableSupplierElement
                            .createExecutableExtension(PROJECT_ENVIRONMENT_SUPPLIER);
                }
            } catch (CoreException e) {
                Activator.log(e);
            }
        }

        if (!modelConfigurationNameProvider[SUPER].isEmpty()) {
            //TOFIX JABA something is not right here
            if (myConfigurationNameProviderElement.getAttribute(CONFIGURATION_NAME_PROVIDER) != null) {
                myConfigurationNameProvider = (IConfigurationNameProvider) myConfigurationNameProviderElement
                        .createExecutableExtension(CONFIGURATION_NAME_PROVIDER);
            }
        }
        if (myBuildMacroSupplierElement.getAttribute(PROJECT_MACRO_SUPPLIER) != null) {
            myBuildMacroSupplier = (IProjectBuildMacroSupplier) myBuildMacroSupplierElement
                    .createExecutableExtension(PROJECT_MACRO_SUPPLIER);
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
        if (!configuration.isTemporary()) {
            myConfigMap.put(configuration.getId(), configuration);
        }
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

}
