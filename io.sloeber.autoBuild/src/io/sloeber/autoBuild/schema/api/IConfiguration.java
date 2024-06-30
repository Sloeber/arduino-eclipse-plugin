/*******************************************************************************
 * Copyright (c) 2003, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *     Dmitry Kozlov (CodeSourcery) - Save build output preferences (bug 294106)
 *     Andrew Gvozdev (Quoin Inc)   - Saving build output implemented in different way (bug 306222)
 *******************************************************************************/
package io.sloeber.autoBuild.schema.api;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;

/**
 * A tool-integrator defines default configurations as children of the project
 * type.
 * These provide a template for the configurations added to the user's project,
 * which are stored in the project's .cproject file.
 * <p>
 * The configuration contains one child of type tool-chain. This describes how
 * the
 * project's resources are transformed into the build artifact. The
 * configuration can
 * contain one or more children of type resourceConfiguration. These describe
 * build
 * settings of individual resources that are different from the configuration as
 * a whole.
 *
 * @since 2.1
 * @noextend This class is not intended to be subclassed by clients.
 * @noimplement This interface is not intended to be implemented by clients.
 */
public interface IConfiguration extends ISchemaObject {
    public static final String CONFIGURATION_ELEMENT_NAME = "configuration"; //$NON-NLS-1$

    public static final String ARTIFACT_NAME = "artifactName"; //$NON-NLS-1$
    public static final String ARTIFACT_EXTENSION = "artifactExtension"; //$NON-NLS-1$
    public static final String CLEAN_COMMAND = "cleanCommand"; //$NON-NLS-1$
    public static final String ERROR_PARSERS = "errorParsers"; //$NON-NLS-1$
    public static final String LANGUAGE_SETTINGS_PROVIDERS = "languageSettingsProviders"; //$NON-NLS-1$
    public static final String DESCRIPTION = "description"; //$NON-NLS-1$
    public static final String BUILD_PROPERTIES = "buildProperties"; //$NON-NLS-1$

    /**
     * Returns the description of the configuration.
     *
     * @return String
     */
    public String getDescription();

    /**
     *
     *
     * /**
     * Returns the extension that should be applied to build artifacts created by
     * this configuration.
     *
     * @return String
     */
    public String getArtifactExtension();

    /**
     * Returns the name of the final build artifact.
     *
     * @return String
     */
    public String getArtifactName();


    /**
     * Returns default language settings providers IDs specified for the
     * configuration.
     *
     * @return default language settings providers IDs or {@code null}.
     *
     * @since 8.1
     */
    public List<String> getDefaultLanguageSettingsProviderIds();

    /**
     * Returns the project-type parent of this configuration, if this is an
     * extension configuration. Otherwise, returns <code>null</code>.
     *
     * @return IProjectType
     */
    public IProjectType getProjectType();

    /**
     * Returns <code>true</code> if the configuration's tool-chain is supported on
     * the system
     * otherwise returns <code>false</code>
     *
     * @return boolean
     */
    public boolean isSupported();

    public List<ICSourceEntry> getSourceEntries();

    public Map<String, String> getDefaultBuildProperties();

    /**
     * Answers the ordered list of unique IDs of the error parsers associated
     * with this configuration.
     *
     * @return String[]
     */
    public String[] getErrorParserList();

    Map<String, Set<IInputType>> getLanguageIDs(AutoBuildConfigurationDescription autoBuildConfData);

    public boolean equals(IConfiguration other);
}
