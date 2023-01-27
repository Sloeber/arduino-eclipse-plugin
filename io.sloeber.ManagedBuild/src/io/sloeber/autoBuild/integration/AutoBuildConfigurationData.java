package io.sloeber.autoBuild.integration;

import static io.sloeber.autoBuild.integration.Const.*;
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

import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.internal.Configuration;

public class AutoBuildConfigurationData extends CConfigurationData {

    IConfiguration myAutoBuildConfiguration;
    IProject myProject;
    CTargetPlatformData myTargetPlatformData;
    BuildBuildData myBuildBuildData;
    ICConfigurationDescription myCdtConfigurationDescription;
    private boolean isValid = false;
    String myName=EMPTY_STRING;

    public AutoBuildConfigurationData(Configuration config, IProject project) {
        myCdtConfigurationDescription = null;
        myAutoBuildConfiguration = config;
        myProject = project;
        myTargetPlatformData = myAutoBuildConfiguration.getToolChain().getTargetPlatformData();
        myName=config.getName();
    }

    //Copy constructor
    public AutoBuildConfigurationData(ICConfigurationDescription cfgDescription,
            AutoBuildConfigurationData autoBuildConfigBase) {
        myCdtConfigurationDescription = cfgDescription;
        myAutoBuildConfiguration = autoBuildConfigBase.getConfiguration();
        myProject = autoBuildConfigBase.getProject();
        myTargetPlatformData = myAutoBuildConfiguration.getToolChain().getTargetPlatformData();
        myBuildBuildData = new BuildBuildData(myAutoBuildConfiguration.getToolChain().getBuilder(),
                myCdtConfigurationDescription);
        myName=autoBuildConfigBase.getName();
        isValid = true;
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
        // TODO Auto-generated method stub
        return "io.sloeber.autoBuild.configurationDescription";
    }

    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return myName;
    }

    @Override
    public CFolderData getRootFolderData() {
        // TODO Auto-generated method stub
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
        // TODO Auto-generated method stub
        return "AutoBuildConfigurationData.getDescription()";
    }

    @Override
    public void setDescription(String description) {
        // TODO Auto-generated method stub

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
    	myName=name;

    }

    @Override
    public boolean isValid() {
        return isValid;
    }

}
