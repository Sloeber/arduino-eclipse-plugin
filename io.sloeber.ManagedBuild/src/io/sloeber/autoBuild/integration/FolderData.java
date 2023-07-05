package io.sloeber.autoBuild.integration;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.settings.model.extension.CFolderData;
import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
import org.eclipse.cdt.core.settings.model.extension.impl.CDefaultFolderData;
import org.eclipse.cdt.core.settings.model.extension.impl.CDefaultLanguageData;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.content.IContentType;

import io.sloeber.schema.api.IInputType;

public class FolderData extends CDefaultFolderData {
    private AutoBuildConfigurationDescription myAutoBuildConf;

    private Set<CDefaultLanguageData> myLanguageDatas = new HashSet<>();

    /**
     * Constructor for project
     * Only here because I didn't know how to load the language data's
     * 
     * @param myProject Not really used but Currently I only use this for the project base folder
     * @param autoBuildConf
     */
    FolderData(IProject myProject, AutoBuildConfigurationDescription autoBuildConf) {
        super( CDataUtil.genId(autoBuildConf.getId()), Path.ROOT, null,autoBuildConf, null, false);
        myAutoBuildConf = autoBuildConf;
        resolve();
    }

//    FolderData(IFolder folder, AutoBuildConfigurationDescription autoBuildConf) {
//        super();
//        myAutoBuildConf = autoBuildConf;
//        resolve();
//
//    }

    private void resolve() {
        Map<String, Set<IInputType>> languageIDs = myAutoBuildConf.getConfiguration().getLanguageIDs(myAutoBuildConf);
        for (String languageID : languageIDs.keySet()) {
            Set<IContentType> contentType = new HashSet<>();
            Set<String> extensions = new HashSet<>();
            for (IInputType inputType : languageIDs.get(languageID)) {
                contentType.addAll(inputType.getSourceContentTypes());
                extensions.addAll(inputType.getSourceExtensionsAttribute());
            }

            if (contentType.size() > 0) {
                String id = CDataUtil.genId(languageID);
                Set<String> types = new HashSet<>();
                for (IContentType curContentType : contentType) {
                    types.add(curContentType.getId());
                }
                CDefaultLanguageData languageData = new CDefaultLanguageData(id, languageID,
                        types.toArray(new String[types.size()]), true);
                myLanguageDatas.add(languageData);
            }
            if (extensions.size() > 0) {
                String id = CDataUtil.genId(languageID);
                CDefaultLanguageData languageData = new CDefaultLanguageData(id, languageID,
                        extensions.toArray(new String[extensions.size()]), false);
                myLanguageDatas.add(languageData);
            }
        }

    }

 
    @Override
    public CLanguageData[] getLanguageDatas() {
        return myLanguageDatas.toArray(new CLanguageData[myLanguageDatas.size()]);
    }


}
