/*******************************************************************************
 * Copyright (c) 2003, 2016 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM - Initial API and implementation
 * James Blackburn (Broadcom Corp.)
 * Dmitry Kozlov (CodeSourcery) - Save build output preferences (bug 294106)
 * Andrew Gvozdev (Quoin Inc)   - Saving build output implemented in different way (bug 306222)
 *******************************************************************************/
package io.sloeber.autoBuild.schema.internal;

import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.autoBuild.schema.api.IConfiguration;
import io.sloeber.autoBuild.schema.api.IInputType;
import io.sloeber.autoBuild.schema.api.IProjectType;
import io.sloeber.autoBuild.schema.api.ITool;
import io.sloeber.autoBuild.schema.api.IToolChain;
import io.sloeber.autoBuild.schema.internal.enablement.MBSEnablementExpression;

public class Configuration extends SchemaObject implements IConfiguration {

    private static final String LANGUAGE_SETTINGS_PROVIDER_DELIMITER = ";"; //$NON-NLS-1$
    private static final String LANGUAGE_SETTINGS_PROVIDER_NEGATION_SIGN = "-"; //$NON-NLS-1$
    private static final String $TOOLCHAIN = "${Toolchain}"; //$NON-NLS-1$

    private String[] modelartifactName;
    private String[] modelartifactExtension;
    private String[] modelerrorParsers;
    private String[] modellanguageSettingsProviders;
    private String[] modeldescription;
    private String[] modelbuildProperties;
    private String[] modelbuildArtefactType;

    private Map<String, String> myBuildProperties = new HashMap<>();

    // Parent and children
    private ProjectType myProjectType;
    private List<String> defaultLanguageSettingsProviderIds = new ArrayList<>();

    private boolean isPreferenceConfig;
    private Set<String> myErrorParserIDs = new HashSet<>();

    /**
     * Create an configuration from the project manifest file element.
     *
     * @param projectType
     *            The <code>ProjectType</code> the configuration will be
     *            added to.
     * @param element
     *            The element from the manifest that contains the
     *            configuration information.
     */
    public Configuration(ProjectType projectType, IExtensionPoint root, IConfigurationElement element) {
        this.myProjectType = projectType;

        loadNameAndID(root, element);

        modelartifactName = getAttributes(ARTIFACT_NAME);
        modelartifactExtension = getAttributes(ARTIFACT_EXTENSION);
        modelerrorParsers = getAttributes(ERROR_PARSERS);
        modellanguageSettingsProviders = getAttributes(LANGUAGE_SETTINGS_PROVIDERS);
        modeldescription = getAttributes(DESCRIPTION);
        modelbuildProperties = getAttributes(BUILD_PROPERTIES);
        modelbuildArtefactType = getAttributes(BUILD_ARTEFACT_TYPE);

        resolveFields();
    }

    private void resolveFields() {
        String errorParserIDs[] = modelerrorParsers[SUPER].split(SEMICOLON);
        Set<String> builderErroParserIds = getToolChain().getErrorParserList();
        myErrorParserIDs.addAll(Arrays.asList(errorParserIDs));
        myErrorParserIDs.addAll(builderErroParserIds);
        myErrorParserIDs.remove(EMPTY_STRING);

        if (modellanguageSettingsProviders[SUPER].isBlank()) {
            modellanguageSettingsProviders[SUPER] = $TOOLCHAIN;
        }

        String[] defaultIds = modellanguageSettingsProviders[SUPER].split(LANGUAGE_SETTINGS_PROVIDER_DELIMITER);
        for (String defaultID : defaultIds) {
            if (defaultID != null && !defaultID.isEmpty()) {
                if (defaultID.startsWith(LANGUAGE_SETTINGS_PROVIDER_NEGATION_SIGN)) {
                    defaultID = defaultID.substring(1);
                    defaultLanguageSettingsProviderIds.remove(defaultID);
                } else if (!defaultLanguageSettingsProviderIds.contains(defaultID)) {
                    if (defaultID.contains($TOOLCHAIN)) {
                        if (getToolChain() != null) {
                            String toolchainProvidersIds = getToolChain().getDefaultLanguageSettingsProviderIds();
                            if (toolchainProvidersIds != null) {
                                defaultLanguageSettingsProviderIds.addAll(Arrays
                                        .asList(toolchainProvidersIds.split(LANGUAGE_SETTINGS_PROVIDER_DELIMITER)));
                            }
                        }
                    } else {
                        defaultLanguageSettingsProviderIds.add(defaultID);
                    }
                }
            }
        }
        myBuildProperties = parseProperties(modelbuildProperties[SUPER]);
        if (!modelartifactName[SUPER].isBlank()) {
            myBuildProperties.put(BUILD_ARTEFACT_TYPE_PROPERTY_ID, modelartifactName[SUPER]);
        }

    }

    private IToolChain getToolChain() {
		return myProjectType.getToolChain();
	}

	@Override
    public IProjectType getProjectType() {
        return myProjectType;
    }

    /*
     * M O D E L A T T R I B U T E A C C E S S O R S
     */

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public String getArtifactExtension() {
        return modelartifactExtension[SUPER];
    }

    @Override
    public String getArtifactName() {
        return modelartifactName[SUPER];
    }

    @Override
    public String getDescription() {
        return modeldescription[SUPER];
    }

    @Override
    public String[] getErrorParserList() {
        return myErrorParserIDs.toArray(new String[myErrorParserIDs.size()]);
    }

    /**
     * {@inheritDoc}
     *
     * This function will try to find default provider Ids specified in this
     * instance. It none defined, it will try to pull Ids from the parent
     * configuration.
     */
    @Override
    public List<String> getDefaultLanguageSettingsProviderIds() {
        return defaultLanguageSettingsProviderIds;
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    /*
     * O B J E C T S T A T E M A I N T E N A N C E
     */

    @Override
    public List<ICSourceEntry> getSourceEntries() {
        return new LinkedList<>();
        //                if (sourceEntries == null || sourceEntries.length == 0) {
        //                    if (parent != null && sourceEntries == null)
        //                        return parent.getSourceEntries();
        //                    return new ICSourceEntry[] {
        //                            new CSourceEntry(Path.EMPTY, null, ICSettingEntry.VALUE_WORKSPACE_PATH | ICSettingEntry.RESOLVED) };
        //        
        //                }
        //                return sourceEntries.clone();
    }



    public boolean isPreference() {
        return isPreferenceConfig;
    }

    @Override
    public Map<String, String> getDefaultBuildProperties() {
        return myBuildProperties;
    }

    public StringBuffer dump(int leadingChars) {
        StringBuffer ret = new StringBuffer();
        String prepend = DUMPLEAD.repeat(leadingChars);
        ret.append(prepend + CONFIGURATION_ELEMENT_NAME + NEWLINE);
        ret.append(prepend + NAME + EQUAL + myName + NEWLINE);
        ret.append(prepend + ID + EQUAL + myID + NEWLINE);
        ret.append(prepend + ARTIFACT_NAME + EQUAL + modelartifactName[SUPER] + NEWLINE);
        ret.append(prepend + ARTIFACT_EXTENSION + EQUAL + modelartifactExtension[SUPER] + NEWLINE);
        ret.append(prepend + ERROR_PARSERS + EQUAL + modelerrorParsers[SUPER] + NEWLINE);
        ret.append(prepend + LANGUAGE_SETTINGS_PROVIDERS + EQUAL + modellanguageSettingsProviders[SUPER] + NEWLINE);

        ret.append(prepend + DESCRIPTION + EQUAL + modeldescription[SUPER] + NEWLINE);
        ret.append(prepend + BUILD_PROPERTIES + EQUAL + modelbuildProperties[SUPER] + NEWLINE);
        ret.append(prepend + BUILD_ARTEFACT_TYPE + EQUAL + modelbuildArtefactType[SUPER] + NEWLINE);

        return ret;
    }

    //    @Override
    //    public Map<IResource, Map<String, String>> getDefaultProjectOptions(
    //            AutoBuildConfigurationDescription autoBuildConfigurationData) {
    //        //Get the default options on the level of the project
    //        Map<String, String> retOptions = getDefaultOptions(autoBuildConfigurationData.getProject(),
    //                autoBuildConfigurationData);
    //        //Get the default options on the level of the toolchain and tools
    //        retOptions.putAll(((ToolChain) getToolChain()).getDefaultOptions(autoBuildConfigurationData.getProject(),
    //                autoBuildConfigurationData));
    //        Map<IResource, Map<String, String>> ret = new LinkedHashMap<>();
    //        ret.put(autoBuildConfigurationData.getProject(), retOptions);
    //        return ret;
    //    }

    @Override
    public Map<String, Set<IInputType>> getLanguageIDs(AutoBuildConfigurationDescription autoBuildConfData) {
        IProject project = autoBuildConfData.getProject();
        Map<String, Set<IInputType>> ret = new HashMap<>();
        for (ITool curTool : getToolChain().getTools()) {
            for (IInputType curInputType : curTool.getInputTypes()) {
                if (!curInputType.isEnabled(MBSEnablementExpression.ENABLEMENT_TYPE_CMD, project, autoBuildConfData)) {
                    continue;
                }
                String languageID = curInputType.getLanguageID();
                if (languageID.isEmpty()) {
                    continue;
                }
                Set<IInputType> inputTypes = ret.get(languageID);
                if (inputTypes == null) {
                    inputTypes = new HashSet<>();
                    inputTypes.add(curInputType);
                    ret.put(languageID, inputTypes);
                } else {
                    inputTypes.add(curInputType);
                }
            }
        }
        return ret;
    }


}
