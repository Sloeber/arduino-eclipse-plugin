package io.sloeber.autoBuild.integration;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.core.cdtvariables.ICdtVariablesContributor;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsManager;
import org.eclipse.cdt.core.language.settings.providers.ScannerDiscoveryLegacySupport;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
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

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IProjectType;
import io.sloeber.schema.internal.Configuration;

public class AutoBuildConfigurationDescription extends CConfigurationData
        implements IAutoBuildConfigurationDescription {
    private IConfiguration myAutoBuildConfiguration;
    private IProject myProject;
    private ICConfigurationDescription myCdtConfigurationDescription;
    private BuildTargetPlatformData myTargetPlatformData;
    private BuildBuildData myBuildBuildData;
    private boolean isValid = false;
    private boolean myIsApplied = false;//have all the settings been applied to the project
    private String myName = EMPTY_STRING;
    private String myDescription;
    private Map<String, String> myProperties = new HashMap<>();
    // resource OptionID Value
    private Map<IResource, Map<String, String>> mySelectedOptions = new HashMap<>();
    private String[] myRequiredErrorParserList;
    private IFolder myBuildFolder;
    private boolean myUseDefaultBuildCommand = true;
    private boolean myGenerateMakeFilesAUtomatically = true;
    private boolean myUseStandardBuildArguments = true;
    private boolean myUseCustomBuildArguments = false;
    private boolean myStopOnFirstBuildError = true;
    private boolean myIsParallelBuild = false;
    private int myParallelizationNum = 0;
    private boolean myIsAutoBuildEnable = true;
    private boolean myIsCleanBuildEnabled = true;
    private boolean myIsIncrementalBuildEnabled = true;
    private boolean myIsInternalBuilderEnabled = true;
    private String myMakeArguments = EMPTY_STRING;
    private String myId = "io.sloeber.autoBuild.configurationDescrtion." + AutoBuildCommon.getRandomNumber();

    public AutoBuildConfigurationDescription(Configuration config, IProject project) {
        myCdtConfigurationDescription = null;
        myAutoBuildConfiguration = config;
        myProject = project;
        myTargetPlatformData = new BuildTargetPlatformData(myAutoBuildConfiguration.getToolChain().getTargetPlatform());
        myName = myAutoBuildConfiguration.getName();
        myDescription = myAutoBuildConfiguration.getDescription();
        myRequiredErrorParserList = myAutoBuildConfiguration.getErrorParserList();

    }

    // Copy constructor
    public AutoBuildConfigurationDescription(ICConfigurationDescription cfgDescription,
            AutoBuildConfigurationDescription autoBuildConfigBase) {

        myAutoBuildConfiguration = autoBuildConfigBase.myAutoBuildConfiguration;
        myProject = autoBuildConfigBase.myProject;
        myCdtConfigurationDescription = cfgDescription;
        myTargetPlatformData = new BuildTargetPlatformData(myAutoBuildConfiguration.getToolChain().getTargetPlatform());
        myBuildBuildData = new BuildBuildData(myAutoBuildConfiguration.getToolChain().getBuilder(),
                myCdtConfigurationDescription);

        isValid = autoBuildConfigBase.isValid;
        myName = myCdtConfigurationDescription.getName();
        myDescription = autoBuildConfigBase.myDescription;
        myRequiredErrorParserList = autoBuildConfigBase.myRequiredErrorParserList;
        myBuildFolder = autoBuildConfigBase.myBuildFolder;
        myProperties.clear();
        myProperties.putAll(autoBuildConfigBase.myProperties);
        mySelectedOptions.clear();
        mySelectedOptions.putAll(autoBuildConfigBase.mySelectedOptions);
        myRequiredErrorParserList = myAutoBuildConfiguration.getErrorParserList();
        myBuildFolder = autoBuildConfigBase.myBuildFolder;

        myUseDefaultBuildCommand = autoBuildConfigBase.myUseDefaultBuildCommand;
        myGenerateMakeFilesAUtomatically = autoBuildConfigBase.myGenerateMakeFilesAUtomatically;
        myUseStandardBuildArguments = autoBuildConfigBase.myUseStandardBuildArguments;
        myUseCustomBuildArguments = autoBuildConfigBase.myUseCustomBuildArguments;
        myStopOnFirstBuildError = autoBuildConfigBase.myStopOnFirstBuildError;
        myIsParallelBuild = autoBuildConfigBase.myIsParallelBuild;
        myParallelizationNum = autoBuildConfigBase.myParallelizationNum;
        myIsAutoBuildEnable = autoBuildConfigBase.myIsAutoBuildEnable;
        myIsCleanBuildEnabled = autoBuildConfigBase.myIsCleanBuildEnabled;
        myIsIncrementalBuildEnabled = autoBuildConfigBase.myIsIncrementalBuildEnabled;
        myIsInternalBuilderEnabled = autoBuildConfigBase.myIsInternalBuilderEnabled;
        myMakeArguments = autoBuildConfigBase.myMakeArguments;
    }

    private void doLegacyChanges() {
        IProjectType projectType = myAutoBuildConfiguration.getProjectType();
        if ("io.sloeber.autoBuild.buildDefinitions".equals(projectType.getExtensionPointID())) { //$NON-NLS-1$
            if ("cdt.cross.gnu".equals(projectType.getExtensionID())) { //$NON-NLS-1$
                switch (projectType.getId()) {
                case "cdt.managedbuild.target.gnu.cross.exe": { //$NON-NLS-1$
                    Map<String, String> options = mySelectedOptions.get(myProject);
                    options.put("gnu.c.link.option.shared", FALSE); //$NON-NLS-1$
                    options.put("gnu.cpp.link.option.shared", FALSE); //$NON-NLS-1$
                    // mySelectedOptions.put(myProject, options);
                    break;
                }
                case "cdt.managedbuild.target.gnu.cross.so": { //$NON-NLS-1$
                    Map<String, String> options = mySelectedOptions.get(myProject);
                    options.put("gnu.c.link.option.shared", TRUE.toLowerCase()); //$NON-NLS-1$
                    options.put("gnu.cpp.link.option.shared", TRUE.toLowerCase()); //$NON-NLS-1$
                    options.put("gnu.cpp.link.option.soname", PROJECT_NAME_VARIABLE + ".so"); //$NON-NLS-1$ //$NON-NLS-2$

                    // mySelectedOptions.put(myProject, options);
                    break;
                }
                }
            }
        }

    }

    public static AutoBuildConfigurationDescription getFromConfig(ICConfigurationDescription confDesc) {
        // TOFIX JABA as CDT does a create on the configuration data in this call
        // I will need to make a lookup table to avoid doing this call
        // I currently keep this with side effects
        return (AutoBuildConfigurationDescription) confDesc.getConfigurationData();
    }

    public void setCdtConfigurationDescription(ICConfigurationDescription cfgDescription) {
        myCdtConfigurationDescription = cfgDescription;
        myBuildBuildData = new BuildBuildData(myAutoBuildConfiguration.getToolChain().getBuilder(),
                myCdtConfigurationDescription);
        mySelectedOptions = myAutoBuildConfiguration.getDefaultProjectOptions(this);
        myBuildFolder = myAutoBuildConfiguration.getBuildFolder(myCdtConfigurationDescription);
        doLegacyChanges();
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
        return myBuildBuildData;
    }

    @Override
    public String getId() {
        return myId;
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

    /**
     * Get the options selected by the user. At project creation time the options
     * are set to the defaults. File specific values overrule folder specific values
     * which overrule project specific values Only the parentfolder options are
     * taken into account
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
                // null means project level and as sutch is valid for all resources
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

    public String[] getErrorParserList() {
        // TODO JABA I for now return the required but this should be the selected
        return myRequiredErrorParserList;
    }

    @Override
    public boolean useDefaultBuildCommand() {
        return myUseDefaultBuildCommand;
    }

    @Override
    public void setUseDefaultBuildCommand(boolean useDefaultBuildCommand) {
        myUseDefaultBuildCommand = useDefaultBuildCommand;
    }

    @Override
    public boolean generateMakeFilesAUtomatically() {
        return myGenerateMakeFilesAUtomatically;
    }

    @Override
    public void setGenerateMakeFilesAUtomatically(boolean generateMakeFilesAUtomatically) {
        myGenerateMakeFilesAUtomatically = generateMakeFilesAUtomatically;
    }

    @Override
    public void setBuildFolder(IFolder buildFolder) {
        myBuildFolder = buildFolder;
    }

    @Override
    public IFolder getBuildFolder() {
        return myBuildFolder;
    }

    @Override
    public String getBuildCommand() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean useStandardBuildArguments() {
        return myUseStandardBuildArguments;
    }

    @Override
    public void setUseStandardBuildArguments(boolean useStandardBuildArguments) {
        myUseStandardBuildArguments = useStandardBuildArguments;
    }

    @Override
    public boolean useCustomBuildArguments() {
        return myUseCustomBuildArguments;
    }

    @Override
    public void setUseCustomBuildArguments(boolean useCustomBuildArguments) {
        myUseCustomBuildArguments = useCustomBuildArguments;
    }

    @Override
    public boolean stopOnFirstBuildError() {
        return myStopOnFirstBuildError;
    }

    @Override
    public void setStopOnFirstBuildError(boolean stopOnFirstBuildError) {
        myStopOnFirstBuildError = stopOnFirstBuildError;
    }

    @Override
    public boolean isParallelBuild() {
        return myIsParallelBuild;
    }

    @Override
    public void setIsParallelBuild(boolean parallelBuild) {
        myIsParallelBuild = parallelBuild;
    }

    @Override
    public int getParallelizationNum() {
        return myParallelizationNum;
    }

    @Override
    public void setParallelizationNum(int parallelizationNum) {
        myParallelizationNum = parallelizationNum;
    }

    @Override
    public boolean isAutoBuildEnable() {
        return myIsAutoBuildEnable;
    }

    @Override
    public void setAutoBuildEnable(boolean autoBuildEnable) {
        myIsAutoBuildEnable = autoBuildEnable;

    }

    @Override
    public boolean isCleanBuildEnabled() {
        return myIsCleanBuildEnabled;
    }

    @Override
    public void setCleanBuildEnable(boolean cleanBuildEnabled) {
        myIsCleanBuildEnabled = cleanBuildEnabled;
    }

    @Override
    public boolean isIncrementalBuildEnabled() {
        return myIsIncrementalBuildEnabled;
    }

    @Override
    public void setIncrementalBuildEnable(boolean incrementalBuildEnabled) {
        myIsIncrementalBuildEnabled = incrementalBuildEnabled;

    }

    @Override
    public boolean isInternalBuilderEnabled() {
        return myIsInternalBuilderEnabled;
    }

    @Override
    public void enableInternalBuilder(boolean internalBuilderEnabled) {
        myIsInternalBuilderEnabled = internalBuilderEnabled;
    }

    @Override
    public boolean isManagedBuildOn() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void setIsManagedBuildOn(boolean isManagedBuildOn) {
        // TOFIX JABA this should not exists

    }

    @Override
    public boolean supportsStopOnError(boolean b) {
        // TOFIX JABA should this exist?
        return true;
    }

    @Override
    public boolean canKeepEnvironmentVariablesInBuildfile() {
        // TOFIX JABA should this exist?
        return false;
    }

    @Override
    public boolean keepEnvironmentVariablesInBuildfile() {
        // TOFIX JABA should this exist?
        return false;
    }

    @Override
    public boolean supportsParallelBuild() {
        // TOFIX JABA should this exist?
        return true;
    }

    @Override
    public int getOptimalParallelJobNum() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String getMakeArguments() {
        return myMakeArguments;
    }

    @Override
    public void setMakeArguments(String makeArgs) {
        myMakeArguments = makeArgs;
    }

    public void applyToConfiguration(ICConfigurationDescription baseCfgDescription) {
        if (myIsApplied) {
            return;
        }
        //        if (baseCfgDescription instanceof ILanguageSettingsProvidersKeeper) {
        //            String[] defaultIds = ((ILanguageSettingsProvidersKeeper) baseCfgDescription)
        //                    .getDefaultLanguageSettingsProvidersIds();
        //            List<ILanguageSettingsProvider> providers;
        //            if (defaultIds == null) {
        //                ICProjectDescription prjDescription = baseCfgDescription.getProjectDescription();
        //                if (prjDescription != null) {
        //                    IProject project = prjDescription.getProject();
        //                    // propagate the preference to project properties
        //                    ScannerDiscoveryLegacySupport.defineLanguageSettingsEnablement(project);
        //                }
        //
        //                if (myAutoBuildConfiguration != null) {
        //                    defaultIds = myAutoBuildConfiguration.getDefaultLanguageSettingsProviderIds()
        //                            .toArray(new String[0]);
        //                }
        //                int a = 0;
        //                if (defaultIds == null) {
        //                    defaultIds = ScannerDiscoveryLegacySupport.getDefaultProviderIdsLegacy(baseCfgDescription);
        //                }
        //                providers = LanguageSettingsManager.createLanguageSettingsProviders(defaultIds);
        //            } else {
        //                providers = ((ILanguageSettingsProvidersKeeper) baseCfgDescription).getLanguageSettingProviders();
        //            }
        //            if (myCdtConfigurationDescription instanceof ILanguageSettingsProvidersKeeper) {
        //                ((ILanguageSettingsProvidersKeeper) myCdtConfigurationDescription)
        //                        .setDefaultLanguageSettingsProvidersIds(defaultIds);
        //                ((ILanguageSettingsProvidersKeeper) myCdtConfigurationDescription)
        //                        .setLanguageSettingProviders(providers);
        //            }
        //        }
        myIsApplied = true;
    }

}
