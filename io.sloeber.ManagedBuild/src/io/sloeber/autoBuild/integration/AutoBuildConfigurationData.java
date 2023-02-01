package io.sloeber.autoBuild.integration;

import static io.sloeber.autoBuild.integration.Const.*;

import java.util.HashMap;
import java.util.Map;

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
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.autoBuild.Internal.BuildTargetPlatformData;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.internal.Configuration;

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
        isValid = true;
    }

    public static AutoBuildConfigurationData getFromConfig(ICConfigurationDescription confDesc) {
        return (AutoBuildConfigurationData) confDesc.getConfigurationData();
    }

    public void setCdtConfigurationDescription(ICConfigurationDescription cfgDescription) {
        myCdtConfigurationDescription = cfgDescription;
        myBuildBuildData = new BuildBuildData(myAutoBuildConfiguration.getToolChain().getBuilder(),
                myCdtConfigurationDescription);
        isValid = true;
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
        return factory.createFolderData(null, null, getId(), false, new Path("/"));
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

}
