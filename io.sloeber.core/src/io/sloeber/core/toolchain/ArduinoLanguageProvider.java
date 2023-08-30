package io.sloeber.core.toolchain;

import static io.sloeber.core.common.Const.*;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsStorage;
import org.eclipse.cdt.core.settings.model.CIncludePathEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

public class ArduinoLanguageProvider implements ILanguageSettingsProvider {

    @Override
    public String getId() {
        return "io.sloeber.languageSettingsProvider"; //$NON-NLS-1$
    }

    @Override
    public String getName() {
        return "ArduinoLanguageProvider"; //$NON-NLS-1$
    }

    @Override
    public List<ICLanguageSettingEntry> getSettingEntries(ICConfigurationDescription cfgDescription, IResource rc,
            String languageId) {
        if (languageId == null || languageId.isBlank()) {
            return null;
        }
        List<ICLanguageSettingEntry> ret = new LinkedList<>();
        IProject project = rc.getProject();
        //JABA TOFIX : I hard coded this to make things work to see this is the way to go
        IFolder coreFolder = project.getFolder("core/core");
        IFolder variansFolder = project.getFolder("core/variant");
        int flags = ICSettingEntry.READONLY | ICSettingEntry.VALUE_WORKSPACE_PATH;
        ret.add(CDataUtil.getPooledEntry(new CIncludePathEntry(coreFolder, flags)));
        ret.add(CDataUtil.getPooledEntry(new CIncludePathEntry(variansFolder, flags)));
        /*
         * TOFIX
         * @return the list of setting entries or {@code null} if no settings defined.
                 *    The list needs to be a pooled list created by {@link LanguageSettingsStorage#getPooledList(List)}
                 *    to save memory and avoid deep equality comparisons.
                 */
        return ret;
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