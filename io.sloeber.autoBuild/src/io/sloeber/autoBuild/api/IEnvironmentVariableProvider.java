/*******************************************************************************
 * Copyright (c) 2005, 2012 Intel Corporation and others.
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
package io.sloeber.autoBuild.api;

import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;

/**
 * this interface represent the environment variable provider - the main
 * entry-point
 * to be used for querying the build environment
 *
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IEnvironmentVariableProvider {
    /**
     * @return the reference to the IBuildEnvironmentVariable interface representing
     *         the variable of a given name
     *
     * @param variableName
     *            environment variable name
     *            if environment variable names are case insensitive in the current
     *            OS,
     *            the environment variable provider will query the getVariable
     *            method of suppliers always
     *            passing it the uppercase variable name not depending on the case
     *            of the variableName
     *            passed to the IEnvironmentVariableProvider.getVariable() method.
     *            This will prevent the
     *            supplier from answering different values for the same variable
     *            given the names that differ
     *            only by case. E.g. if the current OS does not support case
     *            sensitive variables both of the
     *            calls below:
     *
     *            provider.getVariable("FOO",level,includeParentContexts);
     *            provider.getVariable("foo",level,includeParentContexts);
     *
     *            will result in asking suppliers for the "FOO" variable
     *
     * @param level
     *            could be one of the following:
     *            1. IConfiguration to represent the configuration
     *            3. IWorkspace to represent the workspace
     *            4. null to represent the system environment passed to eclipse
     *
     * @deprecated use
     *             {@link IEnvironmentVariableProvider#getVariable(String, IConfiguration, boolean)}
     *             instead
     */
    //    @Deprecated
    //    public IBuildEnvironmentVariable getVariable(String variableName, Object level, boolean includeParentLevels,
    //            boolean resolveMacros);

    /**
     * Get variable for the given configuration, normally including those defined in
     * project properties
     * and workspace preferences.
     *
     * See also
     * {@code CCorePlugin.getDefault().getBuildEnvironmentManager().getVariable(String name, ICConfigurationDescription cfg, boolean resolveMacros)}
     *
     * @param variableName
     *            - name of the variable (not including $ sign).
     * @param cfg
     *            - configuration or {@code null} for workspace preferences only.
     * @param resolveMacros
     *            - if {@code true} expand macros, {@code false} otherwise.
     * @return variable defined for the configuration or the workspace.
     *         Returns {@code null} if variable is not defined.
     */
    public IEnvironmentVariable getVariable(String variableName, ICConfigurationDescription cfg, boolean resolveMacros);

    /**
     * if environment variable names are case insensitive in the current OS,
     * the environment variable provider will remove the duplicates of the variables
     * if their names
     * differ only by case
     * 
     * @deprecated use
     *             {@link IEnvironmentVariableProvider#getVariables(IConfiguration, boolean)}
     *             instead
     *
     * @return the array of IBuildEnvironmentVariable that represents the
     *         environment variables
     */
    //    @Deprecated
    //    public IBuildEnvironmentVariable[] getVariables(Object level, boolean includeParentLevels, boolean resolveMacros);

    /**
     * Get variables for the given configuration, normally including those defined
     * in project properties
     * and workspace preferences.
     *
     * See also
     * {@code CCorePlugin.getDefault().getBuildEnvironmentManager().getVariables(ICConfigurationDescription cfg, boolean resolveMacros)}
     *
     * @param cfg
     *            - configuration or {@code null} for workspace preferences only.
     * @param resolveMacros
     *            - if {@code true} expand macros, {@code false} otherwise.
     * @return array of variables defined for the configuration or the workspace.
     *         Returns an empty array if no variables are defined.
     */
    public IEnvironmentVariable[] getVariables(ICConfigurationDescription cfg, boolean resolveMacros);

}
