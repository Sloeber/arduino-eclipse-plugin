package io.sloeber.autoBuild.integration;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.api.IBuildRunner;
import io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon;
import io.sloeber.autoBuild.extensionPoint.providers.BuildRunnerForMake;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IProjectType;
import io.sloeber.schema.internal.Configuration;

public class AutoBuildConfigurationDescription extends CConfigurationData
        implements IAutoBuildConfigurationDescription {
    private static final String MODEL = "Model"; //$NON-NLS-1$
    private static final String CONFIGURATION = "configuration"; //$NON-NLS-1$
    private static final String EQUALS = "="; //$NON-NLS-1$
    private static final String PROJECT_TYPE = "projectType"; //$NON-NLS-1$
    private static final String EXTENSION_ID = "extensionID"; //$NON-NLS-1$
    private static final String EXTENSION_POINT_ID = "extensionPointID"; //$NON-NLS-1$
    private static final String PROPERTY = "property"; //$NON-NLS-1$
    private static final String BUILDFOLDER = "buildFolder"; //$NON-NLS-1$
    private static final String USE_DEFAULT_BUILD_COMMAND = "useDefaultBuildCommand"; //$NON-NLS-1$
    private static final String GENERATE_MAKE_FILES_AUTOMATICALLY = "generateBuildFilesAutomatically"; //$NON-NLS-1$
    private static final String USE_STANDARD_BUILD_ARGUMENTS = "useStandardBuildArguments"; //$NON-NLS-1$
    private static final String IS_PARRALLEL_BUILD = "isParralelBuild"; //$NON-NLS-1$
    private static final String IS_CLEAN_BUILD_ENABLED = "isCleanEnabled"; //$NON-NLS-1$
    private static final String NUM_PARRALEL_BUILDS = "numberOfParralelBuilds"; //$NON-NLS-1$
    private static final String CUSTOM_BUILD_COMMAND = "customBuildCommand"; //$NON-NLS-1$
    private static final String STOP_ON_FIRST_ERROR = "stopOnFirstError"; //$NON-NLS-1$
    private static final String IS_INCREMENTAL_BUILD_ENABLED = "isIncrementalBuildEnabled"; //$NON-NLS-1$
    private static final String IS_AUTO_BUILD_ENABLED = "isAutoBuildEnabled";//$NON-NLS-1$
    private static final String KEY = "key"; //$NON-NLS-1$
    private static final String VALUE = "value"; //$NON-NLS-1$
    private static final String RESOURCE = "resource";//$NON-NLS-1$
    private static final String BUILD_RUNNER_NAME = "buildRunnerName";//$NON-NLS-1$
    private static final String AUTO_MAKE_TARGET = "make.target.auto";//$NON-NLS-1$
    private static final String INCREMENTAL_MAKE_TARGET = "make.target.incremental";//$NON-NLS-1$
    private static final String CLEAN_MAKE_TARGET = "make.target.clean";//$NON-NLS-1$

    private IConfiguration myAutoBuildConfiguration;
    private IProject myProject;
    private ICConfigurationDescription myCdtConfigurationDescription;
    private BuildTargetPlatformData myTargetPlatformData;
    private BuildBuildData myBuildBuildData;
    private boolean isValid = false;
    private String myName = EMPTY_STRING;
    private String myDescription;
    private Map<String, String> myProperties = new HashMap<>();
    // resource OptionID Value
    private Map<IResource, Map<String, String>> mySelectedOptions = new HashMap<>();
    private String[] myRequiredErrorParserList;

    private boolean myGenerateMakeFilesAUtomatically = true;
    private boolean myStopOnFirstBuildError = true;

    private boolean myIsParallelBuild = false;

    private IBuildRunner myBuildRunner = staticMakeBuildRunner;
    private boolean myIsCleanBuildEnabled = myBuildRunner.supportsCleanBuild();
    private boolean myIsIncrementalBuildEnabled = myBuildRunner.supportsIncrementalBuild();
    private boolean myIsAutoBuildEnabled = myBuildRunner.supportsAutoBuild();
    private String myCustomBuildArguments = EMPTY_STRING;

    private boolean myUseDefaultBuildCommand = true;
    private boolean myUseStandardBuildArguments = true;
    private String myCustomBuildCommand = EMPTY_STRING;
    private int myParallelizationNum = PARRALLEL_BUILD_OPTIMAL_JOBS;
    private String myBuildFolderString = CONFIG_NAME_VARIABLE;

    private String myAutoMakeTarget = DEFAULT_AUTO_MAKE_TARGET;
    private String myIncrementalMakeTarget = DEFAULT_INCREMENTAL_MAKE_TARGET;
    private String myCleanMakeTarget = DEFAULT_CLEAN_MAKE_TARGET;
    private Set<IBuildRunner> myBuildRunners = createBuildRunners();
    private String myId = CDataUtil.genId("io.sloeber.autoBuild.configurationDescrtion."); //$NON-NLS-1$

    private static IBuildRunner staticMakeBuildRunner = new BuildRunnerForMake();
    private static IBuildRunner staticInternalBuildRunner = new InternalBuildRunner();

    public AutoBuildConfigurationDescription(Configuration config, IProject project) {
        myCdtConfigurationDescription = null;
        myAutoBuildConfiguration = config;
        myProject = project;
        myTargetPlatformData = new BuildTargetPlatformData(myAutoBuildConfiguration.getToolChain().getTargetPlatform());
        myName = myAutoBuildConfiguration.getName();
        myDescription = myAutoBuildConfiguration.getDescription();
        myRequiredErrorParserList = myAutoBuildConfiguration.getErrorParserList();

    }

    private static Set<IBuildRunner> createBuildRunners() {
        Set<IBuildRunner> ret = new HashSet<>();
        ret.add(staticMakeBuildRunner);
        ret.add(staticInternalBuildRunner);
        return ret;
    }

    // Copy constructor
    public AutoBuildConfigurationDescription(ICConfigurationDescription cfgDescription,
            AutoBuildConfigurationDescription autoBuildConfigBase) {

        myAutoBuildConfiguration = autoBuildConfigBase.myAutoBuildConfiguration;
        myProject = autoBuildConfigBase.myProject;
        myCdtConfigurationDescription = cfgDescription;
        myTargetPlatformData = new BuildTargetPlatformData(myAutoBuildConfiguration.getToolChain().getTargetPlatform());
        myBuildBuildData = new BuildBuildData(this);
        isValid = autoBuildConfigBase.isValid;
        myName = myCdtConfigurationDescription.getName();
        myDescription = autoBuildConfigBase.myDescription;
        myProperties.clear();
        myProperties.putAll(autoBuildConfigBase.myProperties);
        mySelectedOptions.clear();
        mySelectedOptions.putAll(autoBuildConfigBase.mySelectedOptions);
        myRequiredErrorParserList = myAutoBuildConfiguration.getErrorParserList();
        myGenerateMakeFilesAUtomatically = autoBuildConfigBase.myGenerateMakeFilesAUtomatically;
        myStopOnFirstBuildError = autoBuildConfigBase.myStopOnFirstBuildError;
        myIsParallelBuild = autoBuildConfigBase.myIsParallelBuild;
        myIsCleanBuildEnabled = autoBuildConfigBase.myIsCleanBuildEnabled;
        myIsIncrementalBuildEnabled = autoBuildConfigBase.myIsIncrementalBuildEnabled;
        myCustomBuildArguments = autoBuildConfigBase.myCustomBuildArguments;
        myUseDefaultBuildCommand = autoBuildConfigBase.myUseDefaultBuildCommand;
        myUseStandardBuildArguments = autoBuildConfigBase.myUseStandardBuildArguments;
        myCustomBuildCommand = autoBuildConfigBase.myCustomBuildCommand;
        myParallelizationNum = autoBuildConfigBase.myParallelizationNum;
        myBuildFolderString = autoBuildConfigBase.myBuildFolderString;
        myBuildRunner = autoBuildConfigBase.myBuildRunner;
        myIsAutoBuildEnabled = autoBuildConfigBase.myIsAutoBuildEnabled;

        myAutoMakeTarget = autoBuildConfigBase.myAutoMakeTarget;
        myIncrementalMakeTarget = autoBuildConfigBase.myIncrementalMakeTarget;
        myCleanMakeTarget = autoBuildConfigBase.myCleanMakeTarget;

    }

    public AutoBuildConfigurationDescription(ICConfigurationDescription cfgDescription, String curConfigsText,
            String lineStart, String lineEnd) {
        String extensionPointID = null;
        String extensionID = null;
        String projectTypeID = null;
        String confName = null;
        myCdtConfigurationDescription = cfgDescription;
        myProject = cfgDescription.getProjectDescription().getProject();
        String[] lines = curConfigsText.split(lineEnd);
        Map<String, String> optionKeyMap = new HashMap<>();
        Map<String, String> optionValueMap = new HashMap<>();
        Map<String, String> optionResourceMap = new HashMap<>();

        for (String curLine : lines) {
            if (!curLine.startsWith(lineStart)) {
                continue;
            }
            String field[] = curLine.split(EQUALS, 2);
            String key = field[0].substring(lineStart.length());
            String value = field[1];
            switch (key) {
            case MODEL + DOT + PROJECT_TYPE + DOT + EXTENSION_POINT_ID:
                extensionPointID = value;
                break;
            case MODEL + DOT + PROJECT_TYPE + DOT + EXTENSION_ID:
                extensionID = value;
                break;
            case MODEL + DOT + PROJECT_TYPE + DOT + ID:
                projectTypeID = value;
                break;
            case MODEL + DOT + CONFIGURATION + DOT + NAME:
                confName = value;
                break;
            case NAME:
                myName = value;
                break;
            case DESCRIPTION:
                myDescription = value;
                break;
            case ID:
                myId = value;
                break;
            case BUILDFOLDER:
                myBuildFolderString = value;
                break;
            case USE_DEFAULT_BUILD_COMMAND:
                myUseDefaultBuildCommand = Boolean.parseBoolean(value);
                break;
            case GENERATE_MAKE_FILES_AUTOMATICALLY:
                myGenerateMakeFilesAUtomatically = Boolean.parseBoolean(value);
                break;
            case USE_STANDARD_BUILD_ARGUMENTS:
                myUseStandardBuildArguments = Boolean.parseBoolean(value);
                break;
            case STOP_ON_FIRST_ERROR:
                myStopOnFirstBuildError = Boolean.parseBoolean(value);
                break;
            case IS_PARRALLEL_BUILD:
                myIsParallelBuild = Boolean.parseBoolean(value);
                break;
            case IS_CLEAN_BUILD_ENABLED:
                myIsCleanBuildEnabled = Boolean.parseBoolean(value);
                break;
            case IS_INCREMENTAL_BUILD_ENABLED:
                myIsIncrementalBuildEnabled = Boolean.parseBoolean(value);
                break;
            case IS_AUTO_BUILD_ENABLED:
                myIsAutoBuildEnabled = Boolean.parseBoolean(value);
                break;
            case NUM_PARRALEL_BUILDS:
                myParallelizationNum = Integer.parseInt(value);
                break;
            case CUSTOM_BUILD_COMMAND:
                myCustomBuildCommand = value;
                break;
            case BUILD_RUNNER_NAME:
                for (IBuildRunner buildRunner : getBuildRunners()) {
                    if (myBuildRunner == null || buildRunner.getName().equals(value)) {
                        myBuildRunner = buildRunner;
                    }
                }
                break;
            case AUTO_MAKE_TARGET:
                myAutoMakeTarget = value;
                break;
            case INCREMENTAL_MAKE_TARGET:
                myIncrementalMakeTarget = value;
                break;
            case CLEAN_MAKE_TARGET:
                myCleanMakeTarget = value;
                break;

            default:
                boolean found = false;
                if (key.startsWith(PROPERTY + DOT)) {
                    String propKey = key.substring(PROPERTY.length() + DOT.length());
                    myProperties.put(propKey, value);
                    found = true;
                }
                if (key.startsWith(OPTION + DOT + KEY + DOT)) {
                    String optionIndex = key.substring(OPTION.length() + KEY.length() + DOT.length() * 2);
                    optionKeyMap.put(optionIndex, value);
                    found = true;
                }

                if (key.startsWith(OPTION + DOT + VALUE + DOT)) {
                    String optionIndex = key.substring(OPTION.length() + VALUE.length() + DOT.length() * 2);
                    optionValueMap.put(optionIndex, value);
                    found = true;
                }
                if (key.startsWith(OPTION + DOT + RESOURCE + DOT)) {
                    String optionIndex = key.substring(OPTION.length() + RESOURCE.length() + DOT.length() * 2);
                    optionResourceMap.put(optionIndex, value);
                    found = true;
                }
                if (!found) {
                    System.err.println("Following autobuild configuration line is ignored " + curLine); //$NON-NLS-1$
                }
            }
        }
        IProjectType projectType = AutoBuildManager.getProjectType(extensionPointID, extensionID, projectTypeID, true);
        myAutoBuildConfiguration = projectType.getConfiguration(confName);
        myTargetPlatformData = new BuildTargetPlatformData(myAutoBuildConfiguration.getToolChain().getTargetPlatform());
        myBuildBuildData = new BuildBuildData(this);
        myRequiredErrorParserList = myAutoBuildConfiguration.getErrorParserList();

        for (Entry<String, String> curOptionIndex : optionKeyMap.entrySet()) {
            String value = optionValueMap.get(curOptionIndex.getKey());
            String resourceString = optionResourceMap.get(curOptionIndex.getKey());
            if (value == null || resourceString == null) {
                //This Should not happen
            } else {
                IResource resource = myProject;
                if (!resourceString.isBlank()) {
                    resource = myProject.getFile(resourceString);
                }
                Map<String, String> options = mySelectedOptions.get(resource);
                if (options == null) {
                    options = new HashMap<>();
                    mySelectedOptions.put(resource, options);
                }
                options.put(curOptionIndex.getValue(), value);
            }
        }
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
        myBuildBuildData = new BuildBuildData(this);
        mySelectedOptions = myAutoBuildConfiguration.getDefaultProjectOptions(this);
        doLegacyChanges();
        isValid = true;
    }

    @Override
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

    CFolderData retTest = null;//TOFIX JABA this should not be here

    @Override
    public CFolderData getRootFolderData() {
        int option = 3;
        if (retTest != null) {
            //option 0,2,3 same behaviour
            return retTest;
        }
        if (option == 0) {
            //when opening projectproperties->somethintg with configurations
            //java.lang.IllegalStateException
            // at org.eclipse.cdt.core.settings.model.util.PathSettingsContainer.getValue(PathSettingsContainer.java:608)
            CDataFactory factory = CDataFactory.getDefault();
            CFolderData foData = factory.createFolderData(null, null, getId(), false, new Path(SLACH));
            retTest = foData;
        }
        if (option == 1) {
            CDataFactory factory = CDataFactory.getDefault();
            CFolderData foData = factory.createFolderData(this, null, getId(), false, new Path(SLACH));
            factory.link(this, foData); //This fails as it assumes CDefaultFolderData and not CFolderData
            retTest = foData;
        }
        if (option == 2) {
            //project creation fails
            //java.lang.NullPointerException: Cannot invoke "org.eclipse.cdt.core.settings.model.extension.CFolderData.getPath()" because "baseRootFolderData" is null
            try {
                retTest = createFolderData(new Path(""), null); //$NON-NLS-1$
            } catch (CoreException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        if (option == 3) {
            //When opening project properties->language settings
            //java.lang.NullPointerException: Cannot invoke "org.eclipse.cdt.core.settings.model.ICResourceDescription.getConfiguration()" because the return value of "org.eclipse.cdt.internal.ui.language.settings.providers.LanguageSettingsEntriesTab.getResDesc()" is null
            //at org.eclipse.cdt.internal.ui.language.settings.providers.LanguageSettingsEntriesTab.getConfigurationDescription(LanguageSettingsEntriesTab.java:256)
            retTest = new FolderData(myProject, this);
        }
        if (option == 4) {
            //"src" didn't work "" is project creation failure
            retTest = new FolderData(myProject.getFolder("src"), this); //$NON-NLS-1$
        }
        //project creation fails
        //java.lang.NullPointerException: Cannot invoke "org.eclipse.cdt.core.settings.model.extension.CFolderData.getPath()" because "baseRootFolderData" is null
        //at org.eclipse.cdt.core.settings.model.extension.impl.CDefaultConfigurationData.copySettingsFrom(CDefaultConfigurationData.java:117)
        return retTest;
    }

    //    protected void addRcData(CResourceData data) {
    //        IPath path = standardizePath(data.getPath());
    //        if (path.segmentCount() == 0) {
    //            if (data.getType() == ICSettingBase.SETTING_FOLDER)
    //                fRootFolderData = (CFolderData) data;
    //            else
    //                return;
    //        }
    //        fResourceDataMap.put(path, data);
    //    }

    @Override
    public CResourceData[] getResourceDatas() {
        //        CResourceData datas[] = new CResourceData[1];
        //        datas[0] = new FolderData(myProject, this);
        CResourceData datas[] = new CResourceData[0];
        return datas;

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
        return;
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
        CSourceEntry[] ret = new CSourceEntry[1];
        ret[0] = new CSourceEntry(myProject.getFolder("src"), null, 0); //$NON-NLS-1$
        return ret;
    }

    @Override
    public void setSourceEntries(ICSourceEntry[] entries) {
        // TODO Auto-generated method stub
        return;
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
    public void setBuildFolderString(String buildFolder) {
        myBuildFolderString = buildFolder;
    }

    @Override
    public String getBuildFolderString() {
        return myBuildFolderString;
    }

    @Override
    public IFolder getBuildFolder() {
        String resolved = AutoBuildCommon.resolve(myBuildFolderString, this);
        if (resolved.isBlank()) {
            resolved = myCdtConfigurationDescription.getName();
        }
        return myProject.getFolder(resolved);
    }

    @Override
    public String getBuildCommand(boolean includeArgs) {
        IBuilder bldr = myAutoBuildConfiguration.getBuilder();
        String command = new String();
        if (myUseDefaultBuildCommand) {
            command = bldr.getCommand();
        } else {
            command = getCustomBuildCommand();
        }

        if (includeArgs) {
            String args = bldr.getArguments(myIsParallelBuild, myParallelizationNum, myStopOnFirstBuildError);
            if (args.isBlank()) {
                return command;
            }
            return command + BLANK + args;
        }
        return command;
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
    public int getOptimalParallelJobNum() {
        return AutoBuildCommon.getOptimalParallelJobNum();
    }

    @Override
    public String getCustomBuildCommand() {
        return myCustomBuildCommand;
    }

    @Override
    public void setCustomBuildCommand(String makeArgs) {
        myCustomBuildCommand = makeArgs;
    }

    public StringBuffer ToText(String linePrefix, String lineEnd) {
        StringBuffer ret = new StringBuffer();
        IProjectType projectType = myAutoBuildConfiguration.getProjectType();
        ret.append(linePrefix + MODEL + DOT + PROJECT_TYPE + DOT + EXTENSION_POINT_ID + EQUALS
                + projectType.getExtensionPointID() + lineEnd);
        ret.append(linePrefix + MODEL + DOT + PROJECT_TYPE + DOT + EXTENSION_ID + EQUALS + projectType.getExtensionID()
                + lineEnd);
        ret.append(linePrefix + MODEL + DOT + PROJECT_TYPE + DOT + ID + EQUALS + projectType.getId() + lineEnd);
        ret.append(linePrefix + MODEL + DOT + CONFIGURATION + DOT + NAME + EQUALS + myAutoBuildConfiguration.getName()
                + lineEnd);

        ret.append(linePrefix + NAME + EQUALS + myName + lineEnd);
        ret.append(linePrefix + DESCRIPTION + EQUALS);
        ret.append(myDescription);
        ret.append(lineEnd);

        ret.append(linePrefix + ID + EQUALS + myId + lineEnd);

        for (Entry<String, String> curProp : myProperties.entrySet()) {
            ret.append(linePrefix + PROPERTY + DOT + curProp.getKey() + EQUALS + curProp.getValue() + lineEnd);
        }
        int counter = 0;
        for (Entry<IResource, Map<String, String>> curOption : mySelectedOptions.entrySet()) {
            IResource res = curOption.getKey();
            String resourceID = res.getProjectRelativePath().toString();
            for (Entry<String, String> resourceOptions : curOption.getValue().entrySet()) {
                ret.append(linePrefix + OPTION + DOT + KEY + DOT + String.valueOf(counter) + EQUALS
                        + resourceOptions.getKey() + lineEnd);
                ret.append(linePrefix + OPTION + DOT + VALUE + DOT + String.valueOf(counter) + EQUALS
                        + resourceOptions.getValue() + lineEnd);
                ret.append(linePrefix + OPTION + DOT + RESOURCE + DOT + String.valueOf(counter) + EQUALS + resourceID
                        + lineEnd);
                counter++;
            }
        }

        ret.append(linePrefix + BUILDFOLDER + EQUALS + myBuildFolderString + lineEnd);
        ret.append(
                linePrefix + USE_DEFAULT_BUILD_COMMAND + EQUALS + String.valueOf(myUseDefaultBuildCommand) + lineEnd);
        ret.append(linePrefix + GENERATE_MAKE_FILES_AUTOMATICALLY + EQUALS
                + String.valueOf(myGenerateMakeFilesAUtomatically) + lineEnd);
        ret.append(linePrefix + USE_STANDARD_BUILD_ARGUMENTS + EQUALS + String.valueOf(myUseStandardBuildArguments)
                + lineEnd);
        ret.append(linePrefix + STOP_ON_FIRST_ERROR + EQUALS + String.valueOf(myStopOnFirstBuildError) + lineEnd);
        ret.append(linePrefix + IS_PARRALLEL_BUILD + EQUALS + String.valueOf(myIsParallelBuild) + lineEnd);
        ret.append(linePrefix + IS_CLEAN_BUILD_ENABLED + EQUALS + String.valueOf(myIsCleanBuildEnabled) + lineEnd);
        ret.append(linePrefix + IS_INCREMENTAL_BUILD_ENABLED + EQUALS + String.valueOf(myIsIncrementalBuildEnabled)
                + lineEnd);
        ret.append(linePrefix + NUM_PARRALEL_BUILDS + EQUALS + String.valueOf(myParallelizationNum) + lineEnd);
        ret.append(linePrefix + CUSTOM_BUILD_COMMAND + EQUALS + myCustomBuildCommand + lineEnd);
        ret.append(linePrefix + BUILD_RUNNER_NAME + EQUALS + myBuildRunner.getName() + lineEnd);
        ret.append(linePrefix + AUTO_MAKE_TARGET + EQUALS + myAutoMakeTarget + lineEnd);
        ret.append(linePrefix + INCREMENTAL_MAKE_TARGET + EQUALS + myIncrementalMakeTarget + lineEnd);
        ret.append(linePrefix + CLEAN_MAKE_TARGET + EQUALS + myCleanMakeTarget + lineEnd);

        return ret;

    }

    @Override
    public IBuildRunner getBuildRunner() {
        return myBuildRunner;
    }

    @Override
    public Set<IBuildRunner> getBuildRunners() {
        return myBuildRunners;
    }

    @Override
    public void setBuildRunner(IBuildRunner buildRunner) {
        myBuildRunner = buildRunner;
    }

    @Override
    public boolean isAutoBuildEnabled() {
        return myIsAutoBuildEnabled;
    }

    @Override
    public void setAutoBuildEnabled(boolean enabled) {
        myIsAutoBuildEnabled = enabled;
    }

    @Override
    public void setCustomBuildArguments(String arguments) {
        myCustomBuildArguments = arguments;
    }

    @Override
    public String getCustomBuildArguments() {
        return myCustomBuildArguments;
    }

    @Override
    public void setAutoMakeTarget(String target) {
        myAutoMakeTarget = target;
    }

    @Override
    public String getAutoMakeTarget() {
        return myAutoMakeTarget;
    }

    @Override
    public void setIncrementalMakeTarget(String target) {
        myIncrementalMakeTarget = target;
    }

    @Override
    public String getIncrementalMakeTarget() {
        return myIncrementalMakeTarget;
    }

    @Override
    public void setCleanMakeTarget(String target) {
        myCleanMakeTarget = target;

    }

    @Override
    public String getCleanMakeTarget() {
        return myCleanMakeTarget;
    }

}
