package io.sloeber.autoBuild.integration;

import static io.sloeber.autoBuild.integration.Const.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.core.cdtvariables.ICdtVariablesContributor;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.cdt.core.settings.model.extension.CBuildData;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.core.settings.model.extension.CFileData;
import org.eclipse.cdt.core.settings.model.extension.CFolderData;
import org.eclipse.cdt.core.settings.model.extension.CLanguageData;
import org.eclipse.cdt.core.settings.model.extension.CResourceData;
import org.eclipse.cdt.core.settings.model.extension.CTargetPlatformData;
import org.eclipse.cdt.core.settings.model.extension.impl.CDataFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.autoBuild.Internal.BuildTargetPlatformData;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.IOptions;
import io.sloeber.schema.internal.Configuration;
import io.sloeber.schema.internal.Options;

public class AutoBuildConfigurationData extends CConfigurationData {
    private IConfiguration myAutoBuildConfiguration;
    private IProject myProject;
    private BuildTargetPlatformData myTargetPlatformData;
    private BuildBuildData myBuildBuildData;
    private ICConfigurationDescription myCdtConfigurationDescription;
    private boolean isValid = false;
    private String myName = EMPTY_STRING;
    private String myDescription;
    private Map<String, String> myProperties = new HashMap<>();
    //resource OptionID Value
    private Map<IResource, Map<String, String>> mySelectedOptions = new HashMap<>();

    public AutoBuildConfigurationData(Configuration config, IProject project) {
        myCdtConfigurationDescription = null;
        myAutoBuildConfiguration = config;
        myProject = project;
        myTargetPlatformData = new BuildTargetPlatformData(myAutoBuildConfiguration.getToolChain().getTargetPlatform());
        myName = myAutoBuildConfiguration.getName();
        myDescription = myAutoBuildConfiguration.getDescription();

    }

    //Copy constructor
    public AutoBuildConfigurationData(ICConfigurationDescription cfgDescription,
            AutoBuildConfigurationData autoBuildConfigBase) {
        myCdtConfigurationDescription = cfgDescription;
        myAutoBuildConfiguration = autoBuildConfigBase.getConfiguration();
        myProject = autoBuildConfigBase.getProject();
        myTargetPlatformData = null;// myAutoBuildConfiguration.getToolChain().getTargetPlatformData();
        myBuildBuildData = new BuildBuildData(myAutoBuildConfiguration.getToolChain().getBuilder(),
                myCdtConfigurationDescription);
        myName = autoBuildConfigBase.getName();
        myDescription = autoBuildConfigBase.getDescription();
        mySelectedOptions = myAutoBuildConfiguration.getDefaultProjectOptions(this);
        isValid = true;
    }

    public static AutoBuildConfigurationData getFromConfig(ICConfigurationDescription confDesc) {
        return (AutoBuildConfigurationData) confDesc.getConfigurationData();
    }

    public void setCdtConfigurationDescription(ICConfigurationDescription cfgDescription) {
        myCdtConfigurationDescription = cfgDescription;
        myBuildBuildData = new BuildBuildData(myAutoBuildConfiguration.getToolChain().getBuilder(),
                myCdtConfigurationDescription);
        mySelectedOptions = myAutoBuildConfiguration.getDefaultProjectOptions(this);
        isValid = true;
    }

    public ICConfigurationDescription getCdtConfigurationDescription() {
        return myCdtConfigurationDescription;
    }

    public IProject getProject() {
        return myProject;
    }

    public IConfiguration getConfiguration() {
        return myAutoBuildConfiguration;
    }

    @Override
    public CTargetPlatformData getTargetPlatformData() {
        return myTargetPlatformData;
    }

    @Override
    public CBuildData getBuildData() {
        // TODO Auto-generated method stub
        return myBuildBuildData;
    }

    @Override
    public String getId() {
        return myAutoBuildConfiguration.getId();
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public CFolderData getRootFolderData() {
        CDataFactory factory = CDataFactory.getDefault();
        return factory.createFolderData(null, null, getId(), false, new Path(SLACH));
    }

    @Override
    public CResourceData[] getResourceDatas() {
        // TODO Auto-generated method stub
        return new CResourceData[0];
    }

    @Override
    public String getDescription() {
        return myDescription;
    }

    @Override
    public void setDescription(String description) {
        myDescription = description;
    }

    @Override
    public void removeResourceData(CResourceData data) throws CoreException {
        // TODO Auto-generated method stub

    }

    @Override
    public CFolderData createFolderData(IPath path, CFolderData base) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CFileData createFileData(IPath path, CFileData base) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public CFileData createFileData(IPath path, CFolderData base, CLanguageData langData) throws CoreException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ICSourceEntry[] getSourceEntries() {
        // TODO Auto-generated method stub
        return new CSourceEntry[0];
    }

    @Override
    public void setSourceEntries(ICSourceEntry[] entries) {
        // TODO Auto-generated method stub

    }

    @Override
    public ICdtVariablesContributor getBuildVariablesContributor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setName(String name) {
        myName = name;

    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    public String getProperty(String propertyName) {
        String ret = myProperties.get(propertyName);
        if (ret == null) {
            ret = myAutoBuildConfiguration.getDefaultBuildProperties().get(propertyName);
            if (ret == null) {
                ret = myAutoBuildConfiguration.getProjectType().getDefaultBuildProperties().get(propertyName);
            }
        }
        return ret;
    }

    public String setProperty(String key, String value) {
        return myProperties.put(key, value);
    }

    public IFolder getBuildFolder() {
        return myAutoBuildConfiguration.getBuildFolder(myCdtConfigurationDescription);
    }

    /**
     * Get the options selected by the user.
     * At project creation time the options are set to the defaults.
     * File specific values overrule folder specific values which overrule project
     * specific values
     * Only the parentfolder options are taken into account
     * 
     * @param resource
     *            the resource you want the selected options for
     * @return a Map of <optionID,Selectedvalue>
     */
    public Map<String, String> getSelectedOptions(IResource resource) {
        Map<String, String> retProject = new HashMap<>();
        Map<String, String> retFolder = new HashMap<>();
        Map<String, String> retFile = new HashMap<>();
        for (Entry<IResource, Map<String, String>> curResourceOptions : mySelectedOptions.entrySet()) {
            IResource curResource = curResourceOptions.getKey();
            if (curResource instanceof IProject) {
                //null means project level and as sutch is valid for all resources
                retProject.putAll(curResourceOptions.getValue());
                continue;
            }
            if ((curResource instanceof IFolder)
                    && (curResource.getProjectRelativePath().equals(resource.getParent().getProjectRelativePath()))) {
                retFolder.putAll(curResourceOptions.getValue());
                continue;
            }
            if ((curResource instanceof IFile) && (curResource.equals(resource))) {
                retFile.putAll(curResourceOptions.getValue());
                continue;
            }
        }
        Map<String, String> ret = new HashMap<>();
        ret.putAll(retProject);
        ret.putAll(retFolder);
        ret.putAll(retFile);
        return ret;
    }

}
