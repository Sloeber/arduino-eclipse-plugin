package io.sloeber.autoBuild.integration;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.settings.model.extension.CFolderData;
import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
import org.eclipse.cdt.core.settings.model.extension.impl.CDefaultLanguageData;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.content.IContentType;

import io.sloeber.schema.api.IInputType;

public class FolderData extends CFolderData {
    private AutoBuildConfigurationDescription myAutoBuildConf;
    private String myID;

    private Set<CDefaultLanguageData> myLanguageDatas = new HashSet<>();
    private IPath myPath;

    FolderData(IProject myProject, AutoBuildConfigurationDescription autoBuildConf) {
        super();
        myPath = myProject.getFullPath();
        myAutoBuildConf = autoBuildConf;
        resolve();
    }

    FolderData(IFolder folder, AutoBuildConfigurationDescription autoBuildConf) {
        super();
        myPath = folder.getFullPath();
        myAutoBuildConf = autoBuildConf;
        resolve();

    }

    private void resolve() {
        myID = CDataUtil.genId(myAutoBuildConf.getId());
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
    public IPath getPath() {
        return myPath;
    }

    @Override
    public void setPath(IPath path) {
        // DO NOT implement
        return;
    }

    @Override
    public boolean hasCustomSettings() {
        return false;
    }

    @Override
    public String getId() {
        return myID;
    }

    @Override
    public String getName() {
        return myID;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public CLanguageData[] getLanguageDatas() {
        return myLanguageDatas.toArray(new CLanguageData[myLanguageDatas.size()]);
    }

    @Override
    public CLanguageData createLanguageDataForContentTypes(String languageId, String[] cTypesIds) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CLanguageData createLanguageDataForExtensions(String languageId, String[] extensions) {
        // TODO Auto-generated method stub
        return null;
    }

}
