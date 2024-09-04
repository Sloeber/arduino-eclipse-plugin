package io.sloeber.core.toolchain;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsStorage;
import org.eclipse.cdt.core.settings.model.CIncludePathEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.core.api.ISloeberConfiguration;

public class ArduinoLanguageProvider implements ILanguageSettingsProvider {

    @Override
    public String getId() {
        return "io.sloeber.languageSettingsProvider"; //$NON-NLS-1$
    }

    @Override
    public String getName() {
        return "Sloeber language Settings provider"; //$NON-NLS-1$
    }

    @Override
    public List<ICLanguageSettingEntry> getSettingEntries(ICConfigurationDescription cfgDescription, IResource rc,
            String languageId) {
        if (languageId == null || languageId.isBlank()) {
            return null;
        }
        List<ICLanguageSettingEntry> ret = new LinkedList<>();
        IAutoBuildConfigurationDescription autoBuildConfData=IAutoBuildConfigurationDescription.getConfig(cfgDescription);
        if(autoBuildConfData==null) {
        	return null;
        }
        ISloeberConfiguration autoConfDesc= ISloeberConfiguration.getConfig(autoBuildConfData);
        if(autoConfDesc==null) {
        	return null;
        }
        ICSourceEntry[] mySrcEntries = IAutoBuildConfigurationDescription.getResolvedSourceEntries(autoBuildConfData);
        Set<IFolder> includeFolders = autoConfDesc.getIncludeFolders();
        int flags = ICSettingEntry.READONLY | ICSettingEntry.VALUE_WORKSPACE_PATH | ICSettingEntry.RESOLVED;
        for(IFolder curFolder:includeFolders) {
            boolean isExcluded = mySrcEntries == null ? false
                    : CDataUtil.isExcluded(curFolder.getProjectRelativePath(), mySrcEntries);
            if (isExcluded) {
                 continue;
            }
        	ret.add(CDataUtil.getPooledEntry(new CIncludePathEntry(curFolder, flags)));
        }
        return LanguageSettingsStorage.getPooledList(ret);
    }

    //    @Override
    //    protected String getCompilerCommand(String languageId) {
    //
    //        if (languageId.equals("org.eclipse.cdt.core.gcc")) {
    //            return "${" + CODAN_C_to_O + "}";
    //        } else if (languageId.equals("org.eclipse.cdt.core.g++")) {
    //            return "${" + CODAN_CPP_to_O + "}";
    //        } else {
    //            ManagedBuilderCorePlugin.error(
    //                    "Unable to find compiler command for language " + languageId + " in toolchain=" + getToolchainId());
    //        }
    //
    //        return null;
    //    }

}