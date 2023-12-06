/*******************************************************************************
 * Copyright (c) 2009, 2013 Andrew Gvozdev and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Andrew Gvozdev - initial API and implementation
 *******************************************************************************/

package io.sloeber.autoBuild.Internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.AbstractExecutableExtensionBase;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.cdtvariables.CdtVariableException;
import org.eclipse.cdt.core.cdtvariables.ICdtVariableManager;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsBroadcastingProvider;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsStorage;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFileDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSetting;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICPathEntry;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.core.settings.model.ICSettingBase;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;

/**
 * Implementation of language settings provider for CDT Managed Build System.
 */
public class AutoBuildLanguageSettingsProvider extends AbstractExecutableExtensionBase
        implements ILanguageSettingsBroadcastingProvider {
    @Override
    public List<ICLanguageSettingEntry> getSettingEntries(ICConfigurationDescription cfgDescription, IResource rc,
            String languageId) {
        if (cfgDescription == null || rc == null) {
            return null;
        }

        IPath projectPath = rc.getProjectRelativePath();
        ICLanguageSetting[] languageSettings = null;

        if (rc instanceof IFile) {
            ICLanguageSetting ls = cfgDescription.getLanguageSettingForFile(projectPath, true);
            if (ls != null) {
                languageSettings = new ICLanguageSetting[] { ls };
            } else {
                return getSettingEntries(cfgDescription, rc.getParent(), languageId);
            }
        } else {
            ICResourceDescription rcDescription = cfgDescription.getResourceDescription(projectPath, false);
            languageSettings = getLanguageSettings(rcDescription);
        }

        // this list is allowed to contain duplicate entries, cannot be LinkedHashSet
        List<ICLanguageSettingEntry> list = new ArrayList<>();

        if (languageSettings != null) {
            for (ICLanguageSetting langSetting : languageSettings) {
                if (langSetting != null) {
                    String id = langSetting.getLanguageId();
                    if (id == languageId || (id != null && id.equals(languageId))) {
                        int kindsBits = langSetting.getSupportedEntryKinds();
                        for (int kind = 1; kind <= kindsBits; kind <<= 1) {
                            if ((kindsBits & kind) != 0) {
                                List<ICLanguageSettingEntry> additions = langSetting.getSettingEntriesList(kind);
                                for (ICLanguageSettingEntry entry : additions) {
                                    if (entry instanceof ICPathEntry) {
                                        // have to use getName() rather than getLocation() and not use IPath operations to avoid collapsing ".."
                                        String pathStr = ((ICPathEntry) entry).getName();
                                        if (!new Path(pathStr).isAbsolute()) {
                                            // We need to add project-rooted entry for relative path as MBS counts it this way in some UI
                                            // The relative entry below also should be added for indexer to resolve from source file locations

                                            ICdtVariableManager varManager = CCorePlugin.getDefault()
                                                    .getCdtVariableManager();
                                            try {
                                                // Substitute build/environment variables
                                                String location = varManager.resolveValue(pathStr, "", null, //$NON-NLS-1$
                                                        cfgDescription);
                                                if (!new Path(location).isAbsolute()) {
                                                    IStringVariableManager mngr = VariablesPlugin.getDefault()
                                                            .getStringVariableManager();
                                                    String projectRootedPath = mngr.generateVariableExpression(
                                                            "workspace_loc", rc.getProject().getName()) //$NON-NLS-1$
                                                            + IPath.SEPARATOR + pathStr;
                                                    // clear "RESOLVED" flag
                                                    int flags = entry.getFlags() & ~(ICSettingEntry.RESOLVED
                                                            | ICSettingEntry.VALUE_WORKSPACE_PATH);
                                                    ICLanguageSettingEntry projectRootedEntry = (ICLanguageSettingEntry) CDataUtil
                                                            .createEntry(kind, projectRootedPath, projectRootedPath,
                                                                    null, flags);
                                                    if (!list.contains(projectRootedEntry)) {
                                                        list.add(projectRootedEntry);
                                                    }
                                                }
                                            } catch (CdtVariableException e) {
                                                // Swallow exceptions but also log them
                                                //ManagedBuilderCorePlugin.log(e);
                                                e.printStackTrace();
                                            }

                                        }
                                    }
                                    if (!list.contains(entry)) {
                                        list.add(entry);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return LanguageSettingsStorage.getPooledList(list);
    }

    /**
     * Get language settings for resource description.
     */
    private ICLanguageSetting[] getLanguageSettings(ICResourceDescription rcDescription) {
        ICLanguageSetting[] array = null;
        switch (rcDescription.getType()) {
        case ICSettingBase.SETTING_PROJECT:
        case ICSettingBase.SETTING_CONFIGURATION:
        case ICSettingBase.SETTING_FOLDER:
            ICFolderDescription foDes = (ICFolderDescription) rcDescription;
            array = foDes.getLanguageSettings();
            break;
        case ICSettingBase.SETTING_FILE:
            ICFileDescription fiDes = (ICFileDescription) rcDescription;
            ICLanguageSetting ls = fiDes.getLanguageSetting();
            if (ls != null) {
                array = new ICLanguageSetting[] { ls };
            }
        }
        if (array == null) {
            array = new ICLanguageSetting[0];
        }
        return array;
    }

    @Override
    public LanguageSettingsStorage copyStorage() {
        class PretendStorage extends LanguageSettingsStorage {
            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public LanguageSettingsStorage clone() throws CloneNotSupportedException {
                return this;
            }

            @Override
            public boolean equals(Object obj) {
                // Note that this always triggers change event even if nothing changed in MBS
                return false;
            }
        }
        return new PretendStorage();
    }

}
