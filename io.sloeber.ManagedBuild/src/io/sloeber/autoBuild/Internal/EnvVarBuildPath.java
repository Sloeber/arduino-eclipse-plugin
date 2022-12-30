/*******************************************************************************
 * Copyright (c) 2005, 2018 Intel Corporation and others.
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

import org.eclipse.cdt.internal.core.SafeStringInterner;
//import org.eclipse.cdt.managedbuilder
//import org.eclipse.cdt.managedbuilder.core.IEnvVarBuildPath;
//import org.eclipse.cdt.managedbuilder.core.IManagedConfigElement;
//import org.eclipse.cdt.managedbuilder.core.ITool;
//import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;

import io.sloeber.autoBuild.api.IEnvVarBuildPath;
import io.sloeber.autoBuild.api.IManagedConfigElement;
import io.sloeber.autoBuild.api.ITool;
import io.sloeber.autoBuild.extensionPoint.IBuildPathResolver;

public class EnvVarBuildPath implements IEnvVarBuildPath {

    private int fType;
    private String fVariableNames[];
    private String fPathDelimiter;
    private IBuildPathResolver fBuildPathResolver;
    private IConfigurationElement fBuildPathResolverElement;

    /**
     * Constructor to create an EnvVarBuildPath based on an element from the plugin
     * manifest.
     *
     * @param element
     *            The element containing the information about the tool.
     */
    public EnvVarBuildPath(ITool tool, IManagedConfigElement element) {
        loadFromManifest(element);
    }

    /* (non-Javadoc)
     * Load the EnvVarBuildPath information from the XML element specified in the
     * argument
     * @param element An XML element containing the tool information
     */
    protected void loadFromManifest(IManagedConfigElement element) {

        setType(convertPathTypeToInt(element.getAttribute(TYPE)));

        setVariableNames(SafeStringInterner.safeIntern(element.getAttribute(LIST)));

        setPathDelimiter(SafeStringInterner.safeIntern(element.getAttribute(PATH_DELIMITER)));

        // Store the configuration element IFF there is a build path resolver defined
        String buildPathResolver = element.getAttribute(BUILD_PATH_RESOLVER);
        if (buildPathResolver != null && element instanceof DefaultManagedConfigElement) {
            fBuildPathResolverElement = ((DefaultManagedConfigElement) element).getConfigurationElement();
        }
    }

    @Override
    public int getType() {
        return fType;
    }

    public void setType(int type) {
        this.fType = type;
    }

    @Override
    public String[] getVariableNames() {
        return fVariableNames;
    }

    public void setVariableNames(String names[]) {
        fVariableNames = names;
        fVariableNames = SafeStringInterner.safeIntern(fVariableNames);
    }

    public void setVariableNames(String names) {
        setVariableNames(getNamesFromString(names));
    }

    public String[] getNamesFromString(String names) {
        if (names == null)
            return null;
        return names.split(NAME_SEPARATOR);
    }

    @Override
    public String getPathDelimiter() {
        return fPathDelimiter;
    }

    public void setPathDelimiter(String delimiter) {
        //        if (delimiter == null)
        //            delimiter = ManagedBuildManager.getEnvironmentVariableProvider().getDefaultDelimiter();
        fPathDelimiter = SafeStringInterner.safeIntern(delimiter);
    }

    private int convertPathTypeToInt(String pathType) {
        if (pathType != null && TYPE_LIBRARY.equals(pathType))
            return BUILDPATH_LIBRARY;
        return BUILDPATH_INCLUDE;
    }

    @Override
    public IBuildPathResolver getBuildPathResolver() {
        if (fBuildPathResolver == null && fBuildPathResolverElement != null) {
            try {
                if (fBuildPathResolverElement.getAttribute(BUILD_PATH_RESOLVER) != null) {
                    fBuildPathResolver = (IBuildPathResolver) fBuildPathResolverElement
                            .createExecutableExtension(BUILD_PATH_RESOLVER);
                }
            } catch (CoreException e) {
            }
        }
        return fBuildPathResolver;
    }

}
