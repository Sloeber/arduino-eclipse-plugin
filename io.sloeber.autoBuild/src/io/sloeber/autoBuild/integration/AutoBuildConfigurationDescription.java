package io.sloeber.autoBuild.integration;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.eclipse.cdt.core.cdtvariables.ICdtVariablesContributor;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.extension.CBuildData;
import org.eclipse.cdt.core.settings.model.extension.CTargetPlatformData;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.osgi.framework.Bundle;
import io.sloeber.autoBuild.api.AutoBuildConfigurationExtensionDescription;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.api.IBuildRunner;
import io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon;
import io.sloeber.autoBuild.extensionPoint.providers.BuildRunnerForMake;
import io.sloeber.autoBuild.extensionPoint.providers.InternalBuildRunner;
import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.IProjectType;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.api.IToolChain;
import io.sloeber.schema.internal.Configuration;
import io.sloeber.schema.internal.Tool;
//The following lines give warning. See TOFIX below on why
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import io.sloeber.autoBuild.core.Activator;

public class AutoBuildConfigurationDescription extends AutoBuildResourceData
        implements IAutoBuildConfigurationDescription {
    private static final String KEY_MODEL = "Model"; //$NON-NLS-1$
    private static final String KEY_CONFIGURATION = "configuration"; //$NON-NLS-1$
    private static final String KEY_EQUALS = "="; //$NON-NLS-1$
    private static final String KEY_PROJECT_TYPE = "projectType"; //$NON-NLS-1$
    private static final String KEY_EXTENSION_ID = "extensionID"; //$NON-NLS-1$
    private static final String KEY_EXTENSION_POINT_ID = "extensionPointID"; //$NON-NLS-1$
    private static final String KEY_PROPERTY = "property"; //$NON-NLS-1$
    private static final String KEY_BUILDFOLDER = "buildFolder"; //$NON-NLS-1$
    private static final String KEY_USE_DEFAULT_BUILD_COMMAND = "useDefaultBuildCommand"; //$NON-NLS-1$
    private static final String KEY_GENERATE_MAKE_FILES_AUTOMATICALLY = "generateBuildFilesAutomatically"; //$NON-NLS-1$
    private static final String KEY_USE_STANDARD_BUILD_ARGUMENTS = "useStandardBuildArguments"; //$NON-NLS-1$
    private static final String KEY_IS_PARRALLEL_BUILD = "isParralelBuild"; //$NON-NLS-1$
    private static final String KEY_IS_CLEAN_BUILD_ENABLED = "isCleanEnabled"; //$NON-NLS-1$
    private static final String KEY_NUM_PARRALEL_BUILDS = "numberOfParralelBuilds"; //$NON-NLS-1$
    private static final String KEY_CUSTOM_BUILD_COMMAND = "customBuildCommand"; //$NON-NLS-1$
    private static final String KEY_STOP_ON_FIRST_ERROR = "stopOnFirstError"; //$NON-NLS-1$
    private static final String KEY_IS_INCREMENTAL_BUILD_ENABLED = "isIncrementalBuildEnabled"; //$NON-NLS-1$
    private static final String KEY_IS_AUTO_BUILD_ENABLED = "isAutoBuildEnabled";//$NON-NLS-1$
    private static final String KEY = "key"; //$NON-NLS-1$
    private static final String KEY_VALUE = "value"; //$NON-NLS-1$
    private static final String KEY_RESOURCE = "resource";//$NON-NLS-1$
    private static final String KEY_TOOL = "tool";//$NON-NLS-1$
    private static final String KEY_BUILD_RUNNER_NAME = "buildRunnerName";//$NON-NLS-1$
    private static final String KEY_AUTO_MAKE_TARGET = "make.target.auto";//$NON-NLS-1$
    private static final String KEY_INCREMENTAL_MAKE_TARGET = "make.target.incremental";//$NON-NLS-1$
    private static final String KEY_CLEAN_MAKE_TARGET = "make.target.clean";//$NON-NLS-1$
    private static final String KEY_EXTENSION = "extension"; //$NON-NLS-1$

    private static final String KEY_PRE_BUILD_STEP = "Build.pre.step"; //$NON-NLS-1$
    private static final String KEY_PRE_BUILD_ANNOUNCEMENT = "Build.pre.announcement"; //$NON-NLS-1$
    private static final String KEY_POST_BUILD_STEP = "Build.post.step"; //$NON-NLS-1$
    private static final String KEY_POST_BUILD_ANNOUNCEMENT = "Build.post.announcement"; //$NON-NLS-1$
    private static final String KEY_AUTOBUILD_EXTENSION_CLASS = "Extension class name"; //$NON-NLS-1$
    private static final String KEY_AUTOBUILD_EXTENSION_BUNDEL = "Extension bundel name"; //$NON-NLS-1$

    //Start of fields that need to be copied/made persistent
    private IConfiguration myAutoBuildConfiguration;

    private ICConfigurationDescription myCdtConfigurationDescription;
    private BuildTargetPlatformData myTargetPlatformData;
    private BuildBuildData myBuildBuildData;
    private boolean isValid = false;
    private String myName = EMPTY_STRING;
    private String myDescription;
    private Map<String, String> myProperties = new HashMap<>();
    /* the mySelectedOptions works as follows:
     * The Map<String, String> is the optionID, valueID. In other words the selected value for a option
     * Map<IResource, Map<String, String>> adds the resource. The resource can not be null.
     * In most cases the resource will be a IProject (in other words valid for all resources in the project)
     * Map<ITool, Map<IResource, Map<String, String>>> adds the tool.
     * tool can be null. These are the options at the level of the toolchain/configuration ....
     * When the tool is null I would expect the IResource to be a IProject
     * When the tool is not null this option is only valid when we deal with this tool
     *  
     */
    private Map<ITool, Map<IResource, Map<String, String>>> myDefaultOptions = new HashMap<>();
    private Map<ITool, Map<IResource, Map<String, String>>> mySelectedOptions = new HashMap<>();
    private Map<ITool, Map<IResource, Map<String, String>>> myCombinedOptions = new HashMap<>();
    private String[] myRequiredErrorParserList;

    private boolean myGenerateMakeFilesAUtomatically = true;
    private boolean myStopOnFirstBuildError = true;

    private boolean myIsParallelBuild = false;

    private IBuildRunner myBuildRunner = staticInternalBuildRunner; //internal builder is default
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
    private String myPreBuildStep = EMPTY_STRING;
    private String myPreBuildAnnouncement = EMPTY_STRING;
    private String myPostBuildStep = EMPTY_STRING;
    private String myPostBuildStepAnouncement = EMPTY_STRING;
    private Map<ITool, Map<IResource, String>> myCustomToolCommands = new HashMap<>();
    private Map<ITool, Map<IResource, String>> myCustomToolPattern = new HashMap<>();
    private AutoBuildConfigurationExtensionDescription myAutoBuildCfgExtDes = null;
    //End of fields that need to be copied/made persistent

    private Set<IBuildRunner> myBuildRunners = createBuildRunners();
    private String myId = CDataUtil.genId("io.sloeber.autoBuild.configurationDescription"); //$NON-NLS-1$
    private boolean myIsWritable = false;

    private static IBuildRunner staticMakeBuildRunner = new BuildRunnerForMake();
    private static IBuildRunner staticInternalBuildRunner = new InternalBuildRunner();

    public AutoBuildConfigurationDescription(Configuration config, IProject project) {
        // myId = config.getId();
        myIsWritable = true;
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
            AutoBuildConfigurationDescription base, boolean clone) {

        myIsWritable = !cfgDescription.isReadOnly();
        if (clone) {
            myId = base.getId();
        }
        myAutoBuildConfiguration = base.myAutoBuildConfiguration;
        myProject = base.myProject;
        myCdtConfigurationDescription = cfgDescription;
        myTargetPlatformData = new BuildTargetPlatformData(base.myTargetPlatformData, clone);
        myBuildBuildData = new BuildBuildData(this, base.myBuildBuildData, clone);
        isValid = base.isValid;
        myName = myCdtConfigurationDescription.getName();
        myDescription = base.myDescription;
        myProperties.clear();
        myProperties.putAll(base.myProperties);
        mySelectedOptions.clear();
        options_copy(base.mySelectedOptions, mySelectedOptions);
        myRequiredErrorParserList = myAutoBuildConfiguration.getErrorParserList();
        myGenerateMakeFilesAUtomatically = base.myGenerateMakeFilesAUtomatically;
        myStopOnFirstBuildError = base.myStopOnFirstBuildError;
        myIsParallelBuild = base.myIsParallelBuild;
        myIsCleanBuildEnabled = base.myIsCleanBuildEnabled;
        myIsIncrementalBuildEnabled = base.myIsIncrementalBuildEnabled;
        myCustomBuildArguments = base.myCustomBuildArguments;
        myUseDefaultBuildCommand = base.myUseDefaultBuildCommand;
        myUseStandardBuildArguments = base.myUseStandardBuildArguments;
        myCustomBuildCommand = base.myCustomBuildCommand;
        myParallelizationNum = base.myParallelizationNum;
        myBuildFolderString = base.myBuildFolderString;
        myBuildRunner = base.myBuildRunner;
        myIsAutoBuildEnabled = base.myIsAutoBuildEnabled;

        myAutoMakeTarget = base.myAutoMakeTarget;
        myIncrementalMakeTarget = base.myIncrementalMakeTarget;
        myCleanMakeTarget = base.myCleanMakeTarget;

        myPreBuildStep = base.myPreBuildStep;
        myPreBuildAnnouncement = base.myPreBuildAnnouncement;
        myPostBuildStep = base.myPostBuildStep;
        myPostBuildStepAnouncement = base.myPostBuildStepAnouncement;
        myCustomToolCommands.clear();
        for (Entry<ITool, Map<IResource, String>> curCustomToolEntry : base.myCustomToolCommands.entrySet()) {
            Map<IResource, String> newMap = new HashMap<>(curCustomToolEntry.getValue());
            myCustomToolCommands.put(curCustomToolEntry.getKey(), newMap);
        }
        myCustomToolPattern.clear();
        for (Entry<ITool, Map<IResource, String>> curCustomToolEntry : base.myCustomToolPattern.entrySet()) {
            Map<IResource, String> newMap = new HashMap<>(curCustomToolEntry.getValue());
            myCustomToolPattern.put(curCustomToolEntry.getKey(), newMap);
        }
        options_updateDefault();
        clone(this, base, clone);
        if (base.getAutoBuildConfigurationExtensionDescription() != null) {
            AutoBuildConfigurationExtensionDescription baseExtensionDesc = base
                    .getAutoBuildConfigurationExtensionDescription();

            try {
                //TOFIX JABA is this ever going to work?
                @SuppressWarnings("rawtypes")
                Constructor ctor = baseExtensionDesc.getClass().getDeclaredConstructor(
                        AutoBuildConfigurationDescription.class, AutoBuildConfigurationExtensionDescription.class);

                ctor.setAccessible(true);

                myAutoBuildCfgExtDes = (AutoBuildConfigurationExtensionDescription) ctor.newInstance(this,
                        baseExtensionDesc);
            } catch (NoSuchMethodException e) {
                System.err.println(
                        "ERROR: Classed derived from AutoBuildConfigurationExtensionDescription need to implement a constructor with parameters AutoBuildConfigurationDescription and AutoBuildConfigurationExtensionDescription"); //$NON-NLS-1$
                e.printStackTrace();
            } catch (SecurityException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InstantiationException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Create a configuration based on persisted content.
     * The persisted content can contain multiple configurations and as sutch a
     * filtering is needed
     * The lineStart and lineEnd are used to filter content only applicable to this
     * configuration
     * 
     * @param cfgDescription
     *            the CDT configuration this object will belong to
     * @param curConfigsText
     *            the persistent content
     * @param lineStart
     *            only consider lines that start with this string
     * @param lineEnd
     *            only consider lines that end with this string
     */
    public AutoBuildConfigurationDescription(ICConfigurationDescription cfgDescription, String curConfigsText,
            String lineStart, String lineEnd) {
        super(cfgDescription, curConfigsText, lineStart, lineEnd);
        myId = cfgDescription.getId();
        myIsWritable = !cfgDescription.isReadOnly();
        String extensionPointID = null;
        String extensionID = null;
        String projectTypeID = null;
        String confName = null;
        String autoCfgExtentionDesc = null;
        String autoCfgExtentionBundel = null;
        myCdtConfigurationDescription = cfgDescription;
        myProject = cfgDescription.getProjectDescription().getProject();
        String[] lines = curConfigsText.split(lineEnd);
        Map<String, String> optionKeyMap = new HashMap<>();
        Map<String, String> optionValueMap = new HashMap<>();
        Map<String, String> optionResourceMap = new HashMap<>();
        Map<String, String> optionToolMap = new HashMap<>();
        Map<String, String> customToolKeyMap = new HashMap<>();
        Map<String, String> customToolResourceMap = new HashMap<>();
        Map<String, String> customToolValueMap = new HashMap<>();
        Map<String, String> customToolPatternKeyMap = new HashMap<>();
        Map<String, String> customToolPatternResourceMap = new HashMap<>();
        Map<String, String> customToolPatternValueMap = new HashMap<>();

        //structure to handle the more complex fields
        Map<String, Map<String, String>> complexStructures = new HashMap<>();
        complexStructures.put(KEY_PROPERTY + DOT, myProperties);
        complexStructures.put(OPTION + DOT + KEY + DOT, optionKeyMap);
        complexStructures.put(OPTION + DOT + KEY_VALUE + DOT, optionValueMap);
        complexStructures.put(OPTION + DOT + KEY_RESOURCE + DOT, optionResourceMap);
        complexStructures.put(OPTION + DOT + KEY_TOOL + DOT, optionToolMap);
        complexStructures.put(KEY_CUSTOM_TOOL_COMMAND + DOT + KEY + DOT, customToolKeyMap);
        complexStructures.put(KEY_CUSTOM_TOOL_COMMAND + DOT + KEY_VALUE + DOT, customToolValueMap);
        complexStructures.put(KEY_CUSTOM_TOOL_COMMAND + DOT + KEY_RESOURCE + DOT, customToolResourceMap);
        complexStructures.put(KEY_CUSTOM_TOOL_PATTERN + DOT + KEY + DOT, customToolPatternKeyMap);
        complexStructures.put(KEY_CUSTOM_TOOL_PATTERN + DOT + KEY_VALUE + DOT, customToolPatternValueMap);
        complexStructures.put(KEY_CUSTOM_TOOL_PATTERN + DOT + KEY_RESOURCE + DOT, customToolPatternResourceMap);

        for (String curLine : lines) {
            if (!curLine.startsWith(lineStart)) {
                continue;
            }
            String field[] = curLine.split(KEY_EQUALS, 2);
            String key = field[0].substring(lineStart.length());
            String value = field[1];
            switch (key) {
            case KEY_MODEL + DOT + KEY_PROJECT_TYPE + DOT + KEY_EXTENSION_POINT_ID:
                extensionPointID = value;
                break;
            case KEY_MODEL + DOT + KEY_PROJECT_TYPE + DOT + KEY_EXTENSION_ID:
                extensionID = value;
                break;
            case KEY_MODEL + DOT + KEY_PROJECT_TYPE + DOT + ID:
                projectTypeID = value;
                break;
            case KEY_MODEL + DOT + KEY_CONFIGURATION + DOT + NAME:
                confName = value;
                break;
            case NAME:
                myName = value;
                break;
            case DESCRIPTION:
                myDescription = value;
                break;
            //            case ID:
            //                myId = value;
            //                break;
            case KEY_BUILDFOLDER:
                myBuildFolderString = value;
                break;
            case KEY_USE_DEFAULT_BUILD_COMMAND:
                myUseDefaultBuildCommand = Boolean.parseBoolean(value);
                break;
            case KEY_GENERATE_MAKE_FILES_AUTOMATICALLY:
                myGenerateMakeFilesAUtomatically = Boolean.parseBoolean(value);
                break;
            case KEY_USE_STANDARD_BUILD_ARGUMENTS:
                myUseStandardBuildArguments = Boolean.parseBoolean(value);
                break;
            case KEY_STOP_ON_FIRST_ERROR:
                myStopOnFirstBuildError = Boolean.parseBoolean(value);
                break;
            case KEY_IS_PARRALLEL_BUILD:
                myIsParallelBuild = Boolean.parseBoolean(value);
                break;
            case KEY_IS_CLEAN_BUILD_ENABLED:
                myIsCleanBuildEnabled = Boolean.parseBoolean(value);
                break;
            case KEY_IS_INCREMENTAL_BUILD_ENABLED:
                myIsIncrementalBuildEnabled = Boolean.parseBoolean(value);
                break;
            case KEY_IS_AUTO_BUILD_ENABLED:
                myIsAutoBuildEnabled = Boolean.parseBoolean(value);
                break;
            case KEY_NUM_PARRALEL_BUILDS:
                myParallelizationNum = Integer.parseInt(value);
                break;
            case KEY_CUSTOM_BUILD_COMMAND:
                myCustomBuildCommand = value;
                break;
            case KEY_BUILD_RUNNER_NAME:
                for (IBuildRunner buildRunner : getCompatibleBuildRunners()) {
                    if (myBuildRunner == null || buildRunner.getName().equals(value)) {
                        myBuildRunner = buildRunner;
                    }
                }
                break;
            case KEY_AUTO_MAKE_TARGET:
                myAutoMakeTarget = value;
                break;
            case KEY_INCREMENTAL_MAKE_TARGET:
                myIncrementalMakeTarget = value;
                break;
            case KEY_CLEAN_MAKE_TARGET:
                myCleanMakeTarget = value;
                break;

            case KEY_PRE_BUILD_STEP:
                myPreBuildStep = value;
                break;
            case KEY_PRE_BUILD_ANNOUNCEMENT:
                myPreBuildAnnouncement = value;
                break;
            case KEY_POST_BUILD_STEP:
                myPostBuildStep = value;
                break;
            case KEY_POST_BUILD_ANNOUNCEMENT:
                myPostBuildStepAnouncement = value;
                break;
            case KEY_AUTOBUILD_EXTENSION_CLASS:
                autoCfgExtentionDesc = value;
                break;
            case KEY_AUTOBUILD_EXTENSION_BUNDEL:
                autoCfgExtentionBundel = value;
                break;

            default:
                boolean found = false;

                //gather the complex field data
                for (Entry<String, Map<String, String>> curComplex : complexStructures.entrySet()) {
                    String searchKey = curComplex.getKey();
                    if (key.startsWith(searchKey)) {
                        String propKey = key.substring(searchKey.length());
                        curComplex.getValue().put(propKey, value);
                        found = true;
                        break;
                    }
                }

                if (!found && !key.startsWith(KEY_EXTENSION)) {
                    System.err.println("Following autobuild configuration line is ignored " + curLine); //$NON-NLS-1$
                }
            }
        }
        IProjectType projectType = AutoBuildManager.getProjectType(extensionPointID, extensionID, projectTypeID, true);
        myAutoBuildConfiguration = projectType.getConfiguration(confName);
        myTargetPlatformData = new BuildTargetPlatformData(myAutoBuildConfiguration.getToolChain().getTargetPlatform());
        myBuildBuildData = new BuildBuildData(this);
        myRequiredErrorParserList = myAutoBuildConfiguration.getErrorParserList();
        options_updateDefault();

        //Now we have reconstructed the environment and read out the persisted content it is time to
        //reconstruct the more complicated fields

        //reconstruct the selected options
        for (Entry<String, String> curOptionIndex : optionKeyMap.entrySet()) {
            String key = curOptionIndex.getKey();
            String id = curOptionIndex.getValue();
            String value = optionValueMap.get(key);
            String resourceString = optionResourceMap.get(key);
            String toolString = optionToolMap.get(key);
            if (value == null || resourceString == null) {
                //This Should not happen
                continue;
            }
            IResource resource = myProject;
            if (!resourceString.isBlank()) {
                resource = myProject.getFile(resourceString);
            }
            ITool tool = null;
            if (toolString != null) {
                tool = myAutoBuildConfiguration.getToolChain().getTool(toolString);
            }
            setOptionValueInternal(resource, tool, id, value);

        }
        options_combine();

        //reconstruct the custom tool commands
        for (Entry<String, String> curOptionIndex : customToolKeyMap.entrySet()) {
            String cmd = customToolValueMap.get(curOptionIndex.getKey());
            String resourceString = customToolResourceMap.get(curOptionIndex.getKey());
            ITool tool = myAutoBuildConfiguration.getToolChain().getTool(curOptionIndex.getValue());
            if (cmd == null || resourceString == null || tool == null) {
                //This Should not happen
            } else {
                IResource resource = myProject;
                if (!resourceString.isBlank()) {
                    resource = myProject.getFile(resourceString);
                }

                Map<IResource, String> resourceCmds = myCustomToolCommands.get(tool);
                if (resourceCmds == null) {
                    resourceCmds = new HashMap<>();
                    myCustomToolCommands.put(tool, resourceCmds);
                }
                resourceCmds.put(resource, cmd);
            }
        }

        //reconstruct the custom tool patterns
        for (Entry<String, String> curOptionIndex : customToolPatternKeyMap.entrySet()) {
            String cmd = customToolPatternValueMap.get(curOptionIndex.getKey());
            String resourceString = customToolPatternResourceMap.get(curOptionIndex.getKey());
            ITool tool = myAutoBuildConfiguration.getToolChain().getTool(curOptionIndex.getValue());
            if (cmd == null || resourceString == null || tool == null) {
                //This Should not happen
            } else {
                IResource resource = myProject;
                if (!resourceString.isBlank()) {
                    resource = myProject.getFile(resourceString);
                }

                Map<IResource, String> resourceCmds = myCustomToolPattern.get(tool);
                if (resourceCmds == null) {
                    resourceCmds = new HashMap<>();
                    myCustomToolPattern.put(tool, resourceCmds);
                }
                resourceCmds.put(resource, cmd);
            }
        }

        //load the auto Build configuration extension description
        if (autoCfgExtentionDesc != null && autoCfgExtentionBundel != null && (!autoCfgExtentionDesc.isBlank())
                && (!autoCfgExtentionBundel.isBlank())) {
            try {
                Bundle contributingBundle = getBundle(autoCfgExtentionBundel);
                Class<?> autoCfgExtentionDescClass = contributingBundle.loadClass(autoCfgExtentionDesc);
                Constructor<?> ctor = autoCfgExtentionDescClass.getDeclaredConstructor(
                        IAutoBuildConfigurationDescription.class, String.class, String.class, String.class);
                ctor.setAccessible(true);

                myAutoBuildCfgExtDes = (AutoBuildConfigurationExtensionDescription) ctor.newInstance(this,
                        curConfigsText, lineStart + KEY_EXTENSION + DOT, lineEnd);
            } catch (NoSuchMethodException e) {
                System.err.println(
                        "Classes derived from AutoBuildConfigurationExtensionDescription need to implement constructor IAutoBuildConfigurationDescription, String, String, String"); //$NON-NLS-1$
                e.printStackTrace();
            } catch (SecurityException | ClassNotFoundException | InstantiationException

                    | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

    }

    private static Bundle getBundle(String symbolicName) {
        return org.eclipse.core.internal.registry.osgi.OSGIUtils.getDefault().getBundle(symbolicName);
        //TOFIX Below is an alternative but as I could not test at the time ...
        //        BundleContext bundleContext = Activator.getBundleContext();
        //        if (bundleContext == null) {
        //            System.err.println("Failed to get the bundleContext");
        //            System.err.println("Could not load the plugin this project is created with.");
        //            return null;
        //        }
        //        Bundle result = null;
        //        for (Bundle candidate : bundleContext.getBundles()) {
        //            if (candidate.getSymbolicName().equals(symbolicName)) {
        //                if (result == null || result.getVersion().compareTo(candidate.getVersion()) < 0) {
        //                    result = candidate;
        //                }
        //            }
        //        }
        //        return result;
    }

    /**
     * get the default options and update the combined options
     */
    private void options_updateDefault() {
        myDefaultOptions.clear();
        myDefaultOptions.put(null, myAutoBuildConfiguration.getDefaultProjectOptions(this));
        IToolChain toolchain = myAutoBuildConfiguration.getToolChain();

        for (ITool curITool : toolchain.getTools()) {
            Tool curTool = (Tool) curITool;
            if (!curTool.isEnabled(myProject, this)) {
                continue;
            }
            Map<IResource, Map<String, String>> resourceOptions = new HashMap<>();
            resourceOptions.put(myProject, curTool.getDefaultOptions(myProject, this));
            myDefaultOptions.put(curTool, resourceOptions);
        }
        options_combine();
    }

    /*
     * take myDefaultOptions and mySelected options and combine them in myCombinedOptions
     * From now onwards the combined options are to be used as they take the default and the 
     * user selected option into account in the desired way 
     */
    private void options_combine() {
        myCombinedOptions.clear();
        options_copy(myDefaultOptions, myCombinedOptions);
        options_copy(mySelectedOptions, myCombinedOptions);

    }

    /*
     * take options and make a copy
     */
    private static void options_copy(Map<ITool, Map<IResource, Map<String, String>>> from,
            Map<ITool, Map<IResource, Map<String, String>>> to) {
        for (Entry<ITool, Map<IResource, Map<String, String>>> toolEntrySet : from.entrySet()) {
            ITool curTool = toolEntrySet.getKey();
            Map<IResource, Map<String, String>> curFromResourceOptions = toolEntrySet.getValue();

            Map<IResource, Map<String, String>> curToResourceOptions = to.get(curTool);
            if (curToResourceOptions == null) {
                curToResourceOptions = new HashMap<>();
                to.put(curTool, curToResourceOptions);
            }
            for (Entry<IResource, Map<String, String>> fromResourceSet : curFromResourceOptions.entrySet()) {
                IResource curResource = fromResourceSet.getKey();
                Map<String, String> curFromOptions = fromResourceSet.getValue();

                Map<String, String> curToOptions = curToResourceOptions.get(curResource);
                if (curToOptions == null) {
                    curToOptions = new HashMap<>();
                    curToResourceOptions.put(curResource, curToOptions);
                }
                curToOptions.putAll(curFromOptions);
            }
        }
    }

    public void setCdtConfigurationDescription(ICConfigurationDescription cfgDescription) {
        checkIfWeCanWrite();
        myCdtConfigurationDescription = cfgDescription;
        myBuildBuildData = new BuildBuildData(this);
        options_updateDefault();

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
    public String getDescription() {
        return myDescription;
    }

    @Override
    public void setDescription(String description) {
        checkIfWeCanWrite();
        myDescription = description;
    }

    @Override
    public ICdtVariablesContributor getBuildVariablesContributor() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setName(String name) {
        checkIfWeCanWrite();
        myName = name;

    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
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
        checkIfWeCanWrite();
        return myProperties.put(key, value);
    }

    /**
     * Get the options selected by the user combined with the default options.
     * 
     * File specific values overrule folder specific values
     * which overrule project specific values Only the parentfolder options are
     * taken into account
     * 
     * @param resource
     *            the resource you want the selected options for
     * @param tool
     *            The tool you want the options for
     * 
     * @return a Map of <optionID,Selectedvalue>
     */
    @Override
    public Map<String, String> getSelectedOptions(IResource resource, ITool tool) {
        Map<String, String> retProject = new HashMap<>();
        Map<String, String> retFolder = new HashMap<>();
        Map<String, String> retFile = new HashMap<>();
        for (Entry<ITool, Map<IResource, Map<String, String>>> curToolOptions : myCombinedOptions.entrySet()) {
            ITool curTool = curToolOptions.getKey();
            if (curTool != null && curTool != tool) {
                continue;
            }
            for (Entry<IResource, Map<String, String>> curResourceOptions : curToolOptions.getValue().entrySet()) {
                IResource curResource = curResourceOptions.getKey();
                if (curResource instanceof IProject) {
                    // null means project level and as sutch is valid for all resources
                    retProject.putAll(curResourceOptions.getValue());
                    continue;
                }
                if ((curResource instanceof IFolder) && (curResource.getProjectRelativePath()
                        .equals(resource.getParent().getProjectRelativePath()))) {
                    retFolder.putAll(curResourceOptions.getValue());
                    continue;
                }
                if ((curResource instanceof IFile) && (curResource.equals(resource))) {
                    retFile.putAll(curResourceOptions.getValue());
                    continue;
                }
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
        checkIfWeCanWrite();
        myUseDefaultBuildCommand = useDefaultBuildCommand;
    }

    @Override
    public boolean generateMakeFilesAUtomatically() {
        return myGenerateMakeFilesAUtomatically;
    }

    @Override
    public void setGenerateMakeFilesAUtomatically(boolean generateMakeFilesAUtomatically) {
        checkIfWeCanWrite();
        myGenerateMakeFilesAUtomatically = generateMakeFilesAUtomatically;
    }

    @Override
    public void setBuildFolderString(String buildFolder) {
        checkIfWeCanWrite();
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
        checkIfWeCanWrite();
        myUseStandardBuildArguments = useStandardBuildArguments;
    }

    @Override
    public boolean stopOnFirstBuildError() {
        return myStopOnFirstBuildError;
    }

    @Override
    public void setStopOnFirstBuildError(boolean stopOnFirstBuildError) {
        checkIfWeCanWrite();
        myStopOnFirstBuildError = stopOnFirstBuildError;
    }

    @Override
    public boolean isParallelBuild() {
        return myIsParallelBuild;
    }

    @Override
    public void setIsParallelBuild(boolean parallelBuild) {
        checkIfWeCanWrite();
        myIsParallelBuild = parallelBuild;
    }

    @Override
    public int getParallelizationNum() {
        return myParallelizationNum;
    }

    @Override
    public void setParallelizationNum(int parallelizationNum) {
        checkIfWeCanWrite();
        myParallelizationNum = parallelizationNum;
    }

    @Override
    public boolean isCleanBuildEnabled() {
        return myIsCleanBuildEnabled;
    }

    @Override
    public void setCleanBuildEnable(boolean cleanBuildEnabled) {
        checkIfWeCanWrite();
        myIsCleanBuildEnabled = cleanBuildEnabled;
    }

    @Override
    public boolean isIncrementalBuildEnabled() {
        return myIsIncrementalBuildEnabled;
    }

    @Override
    public void setIncrementalBuildEnable(boolean incrementalBuildEnabled) {
        checkIfWeCanWrite();
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
        checkIfWeCanWrite();
        myCustomBuildCommand = makeArgs;
    }

    public StringBuffer serialize(String linePrefix, String lineEnd) {
        int counterStart = 0;
        StringBuffer ret = new StringBuffer();
        IProjectType projectType = myAutoBuildConfiguration.getProjectType();
        ret.append(linePrefix + KEY_MODEL + DOT + KEY_PROJECT_TYPE + DOT + KEY_EXTENSION_POINT_ID + KEY_EQUALS
                + projectType.getExtensionPointID() + lineEnd);
        ret.append(linePrefix + KEY_MODEL + DOT + KEY_PROJECT_TYPE + DOT + KEY_EXTENSION_ID + KEY_EQUALS
                + projectType.getExtensionID() + lineEnd);
        ret.append(linePrefix + KEY_MODEL + DOT + KEY_PROJECT_TYPE + DOT + ID + KEY_EQUALS + projectType.getId()
                + lineEnd);
        ret.append(linePrefix + KEY_MODEL + DOT + KEY_CONFIGURATION + DOT + NAME + KEY_EQUALS
                + myAutoBuildConfiguration.getName() + lineEnd);

        ret.append(linePrefix + NAME + KEY_EQUALS + myName + lineEnd);
        ret.append(linePrefix + DESCRIPTION + KEY_EQUALS);
        ret.append(myDescription);
        ret.append(lineEnd);

        //ret.append(linePrefix + ID + KEY_EQUALS + myId + lineEnd);

        for (Entry<String, String> curProp : myProperties.entrySet()) {
            ret.append(linePrefix + KEY_PROPERTY + DOT + curProp.getKey() + KEY_EQUALS + curProp.getValue() + lineEnd);
        }
        int counter = counterStart;
        for (Entry<ITool, Map<IResource, Map<String, String>>> curOption1 : mySelectedOptions.entrySet()) {
            ITool tool = curOption1.getKey();
            String toolID = NULL;
            if (tool != null) {
                toolID = tool.getId();
            }
            for (Entry<IResource, Map<String, String>> curOption : curOption1.getValue().entrySet()) {
                IResource resource = curOption.getKey();
                String resourceID = resource.getProjectRelativePath().toString();
                for (Entry<String, String> resourceOptions : curOption.getValue().entrySet()) {
                    ret.append(linePrefix + OPTION + DOT + KEY + DOT + String.valueOf(counter) + KEY_EQUALS
                            + resourceOptions.getKey() + lineEnd);
                    ret.append(linePrefix + OPTION + DOT + KEY_VALUE + DOT + String.valueOf(counter) + KEY_EQUALS
                            + resourceOptions.getValue() + lineEnd);
                    ret.append(linePrefix + OPTION + DOT + KEY_RESOURCE + DOT + String.valueOf(counter) + KEY_EQUALS
                            + resourceID + lineEnd);
                    ret.append(linePrefix + OPTION + DOT + KEY_TOOL + DOT + String.valueOf(counter) + KEY_EQUALS
                            + toolID + lineEnd);
                    counter++;
                }
            }
        }

        ret.append(linePrefix + KEY_BUILDFOLDER + KEY_EQUALS + myBuildFolderString + lineEnd);
        ret.append(linePrefix + KEY_USE_DEFAULT_BUILD_COMMAND + KEY_EQUALS + String.valueOf(myUseDefaultBuildCommand)
                + lineEnd);
        ret.append(linePrefix + KEY_GENERATE_MAKE_FILES_AUTOMATICALLY + KEY_EQUALS
                + String.valueOf(myGenerateMakeFilesAUtomatically) + lineEnd);
        ret.append(linePrefix + KEY_USE_STANDARD_BUILD_ARGUMENTS + KEY_EQUALS
                + String.valueOf(myUseStandardBuildArguments) + lineEnd);
        ret.append(
                linePrefix + KEY_STOP_ON_FIRST_ERROR + KEY_EQUALS + String.valueOf(myStopOnFirstBuildError) + lineEnd);
        ret.append(linePrefix + KEY_IS_PARRALLEL_BUILD + KEY_EQUALS + String.valueOf(myIsParallelBuild) + lineEnd);
        ret.append(
                linePrefix + KEY_IS_CLEAN_BUILD_ENABLED + KEY_EQUALS + String.valueOf(myIsCleanBuildEnabled) + lineEnd);
        ret.append(linePrefix + KEY_IS_INCREMENTAL_BUILD_ENABLED + KEY_EQUALS
                + String.valueOf(myIsIncrementalBuildEnabled) + lineEnd);
        ret.append(linePrefix + KEY_NUM_PARRALEL_BUILDS + KEY_EQUALS + String.valueOf(myParallelizationNum) + lineEnd);
        ret.append(linePrefix + KEY_CUSTOM_BUILD_COMMAND + KEY_EQUALS + myCustomBuildCommand + lineEnd);
        ret.append(linePrefix + KEY_BUILD_RUNNER_NAME + KEY_EQUALS + myBuildRunner.getName() + lineEnd);
        ret.append(linePrefix + KEY_AUTO_MAKE_TARGET + KEY_EQUALS + myAutoMakeTarget + lineEnd);
        ret.append(linePrefix + KEY_INCREMENTAL_MAKE_TARGET + KEY_EQUALS + myIncrementalMakeTarget + lineEnd);
        ret.append(linePrefix + KEY_CLEAN_MAKE_TARGET + KEY_EQUALS + myCleanMakeTarget + lineEnd);

        ret.append(linePrefix + KEY_PRE_BUILD_STEP + KEY_EQUALS + myPreBuildStep + lineEnd);
        ret.append(linePrefix + KEY_PRE_BUILD_ANNOUNCEMENT + KEY_EQUALS + myPreBuildAnnouncement + lineEnd);
        ret.append(linePrefix + KEY_POST_BUILD_STEP + KEY_EQUALS + myPostBuildStep + lineEnd);
        ret.append(linePrefix + KEY_POST_BUILD_ANNOUNCEMENT + KEY_EQUALS + myPostBuildStepAnouncement + lineEnd);

        counter = counterStart;
        for (Entry<ITool, Map<IResource, String>> curCustomToolCommands : myCustomToolCommands.entrySet()) {
            ITool tool = curCustomToolCommands.getKey();

            for (Entry<IResource, String> curResourceCommand : curCustomToolCommands.getValue().entrySet()) {
                IResource res = curResourceCommand.getKey();
                String resourceID = res.getProjectRelativePath().toString();
                ret.append(linePrefix + KEY_CUSTOM_TOOL_COMMAND + DOT + KEY + DOT + String.valueOf(counter) + KEY_EQUALS
                        + tool.getId() + lineEnd);
                ret.append(linePrefix + KEY_CUSTOM_TOOL_COMMAND + DOT + KEY_VALUE + DOT + String.valueOf(counter)
                        + KEY_EQUALS + curResourceCommand.getValue() + lineEnd);
                ret.append(linePrefix + KEY_CUSTOM_TOOL_COMMAND + DOT + KEY_RESOURCE + DOT + String.valueOf(counter)
                        + KEY_EQUALS + resourceID + lineEnd);
                counter++;
            }
        }

        counter = counterStart;
        for (Entry<ITool, Map<IResource, String>> curCustomToolCommands : myCustomToolPattern.entrySet()) {
            ITool tool = curCustomToolCommands.getKey();

            for (Entry<IResource, String> curResourceCommand : curCustomToolCommands.getValue().entrySet()) {
                IResource res = curResourceCommand.getKey();
                String resourceID = res.getProjectRelativePath().toString();
                ret.append(linePrefix + KEY_CUSTOM_TOOL_PATTERN + DOT + KEY + DOT + String.valueOf(counter) + KEY_EQUALS
                        + tool.getId() + lineEnd);
                ret.append(linePrefix + KEY_CUSTOM_TOOL_PATTERN + DOT + KEY_VALUE + DOT + String.valueOf(counter)
                        + KEY_EQUALS + curResourceCommand.getValue() + lineEnd);
                ret.append(linePrefix + KEY_CUSTOM_TOOL_PATTERN + DOT + KEY_RESOURCE + DOT + String.valueOf(counter)
                        + KEY_EQUALS + resourceID + lineEnd);
                counter++;
            }
        }

        if (myAutoBuildCfgExtDes != null) {
            Class<? extends AutoBuildConfigurationExtensionDescription> referencedClass = myAutoBuildCfgExtDes
                    .getClass();

            ret.append(linePrefix + KEY_AUTOBUILD_EXTENSION_BUNDEL + KEY_EQUALS + myAutoBuildCfgExtDes.getBundelName()
                    + lineEnd);
            ret.append(linePrefix + KEY_AUTOBUILD_EXTENSION_CLASS + KEY_EQUALS + referencedClass.getName() + lineEnd);
            ret.append(myAutoBuildCfgExtDes.serialize(linePrefix + KEY_EXTENSION + DOT, lineEnd));
        }

        return ret;

    }

    @Override
    public IBuildRunner getBuildRunner() {
        return myBuildRunner;
    }

    @Override
    public Set<IBuildRunner> getCompatibleBuildRunners() {
        return myBuildRunners;
    }

    @Override
    public void setBuildRunner(IBuildRunner buildRunner) {
        checkIfWeCanWrite();
        myBuildRunner = buildRunner;
        myIsCleanBuildEnabled = myIsCleanBuildEnabled && myBuildRunner.supportsCleanBuild();
        myIsIncrementalBuildEnabled = myIsIncrementalBuildEnabled && myBuildRunner.supportsIncrementalBuild();
        myIsAutoBuildEnabled = myIsAutoBuildEnabled && myBuildRunner.supportsAutoBuild();
    }

    @Override
    public void setBuildRunner(String buildRunnerName) {
        checkIfWeCanWrite();
        if (buildRunnerName == null) {
            return;
        }
        setBuildRunner(getBuildRunner(buildRunnerName));
    }

    @Override
    public boolean isAutoBuildEnabled() {
        return myIsAutoBuildEnabled;
    }

    @Override
    public void setAutoBuildEnabled(boolean enabled) {
        checkIfWeCanWrite();
        myIsAutoBuildEnabled = enabled;
    }

    @Override
    public void setCustomBuildArguments(String arguments) {
        checkIfWeCanWrite();
        myCustomBuildArguments = arguments;
    }

    @Override
    public String getCustomBuildArguments() {
        return myCustomBuildArguments;
    }

    @Override
    public void setAutoMakeTarget(String target) {
        checkIfWeCanWrite();
        myAutoMakeTarget = target;
    }

    @Override
    public String getAutoMakeTarget() {
        return myAutoMakeTarget;
    }

    @Override
    public void setIncrementalMakeTarget(String target) {
        checkIfWeCanWrite();
        myIncrementalMakeTarget = target;
    }

    @Override
    public String getIncrementalMakeTarget() {
        return myIncrementalMakeTarget;
    }

    @Override
    public void setCleanMakeTarget(String target) {
        checkIfWeCanWrite();
        myCleanMakeTarget = target;

    }

    @Override
    public String getCleanMakeTarget() {
        return myCleanMakeTarget;
    }

    @Override
    public String getPrebuildStep() {
        return myPreBuildStep;
    }

    @Override
    public void setPrebuildStep(String text) {
        checkIfWeCanWrite();
        myPreBuildStep = text;
    }

    @Override
    protected void checkIfWeCanWrite() {
        if (myCdtConfigurationDescription != null && myCdtConfigurationDescription.isReadOnly()) {
            myCdtConfigurationDescription.setDescription(null);
        }
        if (!myIsWritable) {
            //TOFIX need to throw exception one way or another
            int a = 0;
            a = a / a;
        }

    }

    @Override
    public String getPreBuildAnouncement() {
        return myPreBuildAnnouncement;
    }

    @Override
    public void setPreBuildAnouncement(String anouncement) {
        checkIfWeCanWrite();
        myPreBuildAnnouncement = anouncement;

    }

    @Override
    public String getPostbuildStep() {
        return myPostBuildStep;
    }

    @Override
    public void setPostbuildStep(String text) {
        checkIfWeCanWrite();
        myPostBuildStep = text;

    }

    @Override
    public String getPostBuildAnouncement() {
        return myPostBuildStepAnouncement;
    }

    @Override
    public void setPostBuildAnouncement(String text) {
        checkIfWeCanWrite();
        myPostBuildStepAnouncement = text;

    }

    @Override
    public void setCustomToolCommand(ITool tool, IResource resource, String customCommand) {
        checkIfWeCanWrite();
        if (tool == null) {
            //This should not happen; ignore
            return;
        }
        if (resource == null) {
            //request to remove all custom commands for this tool
            myCustomToolCommands.remove(tool);
            return;
        }
        Map<IResource, String> customCommands = myCustomToolCommands.get(tool);
        if (customCommands == null && customCommand == null) {
            //Request to remove something that doesn't exists => nothing to do
            return;
        }
        if (customCommands == null) {
            customCommands = new HashMap<>();
            myCustomToolCommands.put(tool, customCommands);
        }
        customCommands.put(resource, customCommand);
    }

    @Override
    public String getToolCommand(ITool tool, IResource resource) {
        String retProject = null;
        String retFolder = null;

        Map<IResource, String> customToolCommands = myCustomToolCommands.get(tool);
        if (customToolCommands == null) {
            return tool.getDefaultommandLineCommand();
        }
        for (Entry<IResource, String> curCustomCommand : customToolCommands.entrySet()) {
            IResource curResource = curCustomCommand.getKey();
            if (curResource instanceof IProject) {
                // null means project level and as sutch is valid for all resources
                retProject = curCustomCommand.getValue();
                continue;
            }
            if ((curResource instanceof IFolder)
                    && (curResource.getProjectRelativePath().equals(resource.getParent().getProjectRelativePath()))) {
                retFolder = curCustomCommand.getValue();
                continue;
            }
            if ((curResource instanceof IFile) && (curResource.equals(resource))) {
                return curCustomCommand.getValue();
            }
        }
        if (retFolder != null) {
            return retFolder;
        }
        if (retProject != null) {
            return retProject;
        }
        return tool.getDefaultommandLineCommand();
    }

    @Override
    public void setCustomToolPattern(ITool tool, IResource resource, String pattern) {
        checkIfWeCanWrite();
        if (tool == null) {
            //This should not happen; ignore
            return;
        }
        if (resource == null) {
            //request to remove all custom patterns for this tool
            myCustomToolPattern.remove(tool);
            return;
        }
        Map<IResource, String> customCommands = myCustomToolPattern.get(tool);
        if (customCommands == null && pattern == null) {
            //Request to remove something that doesn't exists => nothing to do
            return;
        }
        if (customCommands == null) {
            customCommands = new HashMap<>();
            myCustomToolPattern.put(tool, customCommands);
        }
        customCommands.put(resource, pattern);

    }

    @Override
    public String getToolPattern(ITool tool, IResource resource) {
        String retProject = null;
        String retFolder = null;

        Map<IResource, String> customToolCommands = myCustomToolPattern.get(tool);
        if (customToolCommands == null) {
            return tool.getDefaultCommandLinePattern();
        }
        for (Entry<IResource, String> curCustomCommand : customToolCommands.entrySet()) {
            IResource curResource = curCustomCommand.getKey();
            if (curResource instanceof IProject) {
                // null means project level and as sutch is valid for all resources
                retProject = curCustomCommand.getValue();
                continue;
            }
            if ((curResource instanceof IFolder)
                    && (curResource.getProjectRelativePath().equals(resource.getParent().getProjectRelativePath()))) {
                retFolder = curCustomCommand.getValue();
                continue;
            }
            if ((curResource instanceof IFile) && (curResource.equals(resource))) {
                return curCustomCommand.getValue();
            }
        }
        if (retFolder != null) {
            return retFolder;
        }
        if (retProject != null) {
            return retProject;
        }
        return tool.getDefaultCommandLinePattern();
    }

    @Override
    public void setOptionValue(IResource resource, ITool tool, IOption option, String valueID) {
        checkIfWeCanWrite();
        setOptionValueInternal(resource, tool, option.getId(), valueID);
        options_combine();
    }

    private void setOptionValueInternal(IResource resource, ITool tool, String optionID, String valueIDin) {
        String valueID = valueIDin;
        Map<IResource, Map<String, String>> resourceOptions = mySelectedOptions.get(tool);
        if (resourceOptions == null) {
            if (valueID == null || valueID.isBlank()) {
                //as it does not exist and we want to erase do nothing
                return;
            }
            resourceOptions = new HashMap<>();
            mySelectedOptions.put(tool, resourceOptions);
        }

        Map<String, String> options = resourceOptions.get(resource);
        if (options == null) {
            if (valueID == null || valueID.isBlank()) {
                //as it does not exist and we want to erase do nothing
                return;
            }
            options = new HashMap<>();
            resourceOptions.put(resource, options);
        }
        if (valueID == null) {
            options.remove(optionID);
        } else {
            options.put(optionID, valueID);
        }

    }

    @Override
    public String getOptionValue(IResource resource, ITool tool, IOption option) {
        Map<IResource, Map<String, String>> toolOptions = myCombinedOptions.get(tool);

        if (toolOptions == null) {
            return EMPTY_STRING;
        }
        Map<String, String> resourceOptions = toolOptions.get(resource);

        if (resourceOptions == null) {
            return EMPTY_STRING;
        }
        String ret = resourceOptions.get(option.getId());
        if (ret == null) {
            return EMPTY_STRING;
        }
        return ret;
    }

    @Override
    public String getExtensionPointID() {
        return myAutoBuildConfiguration.getProjectType().getExtensionPointID();
    }

    @Override
    public String getExtensionID() {
        return myAutoBuildConfiguration.getProjectType().getExtensionID();
    }

    @Override
    public IProjectType getProjectType() {
        return myAutoBuildConfiguration.getProjectType();
    }

    @Override
    public IConfiguration getAutoBuildConfiguration() {
        return myAutoBuildConfiguration;
    }

    @Override
    public void setModelConfiguration(IConfiguration newConfiguration) {
        checkIfWeCanWrite();
        myAutoBuildConfiguration = newConfiguration;
        myTargetPlatformData = new BuildTargetPlatformData(myAutoBuildConfiguration.getToolChain().getTargetPlatform());
        //   myName = myAutoBuildConfiguration.getName();
        //   myDescription = myAutoBuildConfiguration.getDescription();
        myRequiredErrorParserList = myAutoBuildConfiguration.getErrorParserList();
        options_updateDefault();
    }

    @Override
    public AutoBuildConfigurationExtensionDescription getAutoBuildConfigurationExtensionDescription() {
        return myAutoBuildCfgExtDes;
    }

    @Override
    public void setAutoBuildConfigurationExtensionDescription(AutoBuildConfigurationExtensionDescription newExtension) {
        checkIfWeCanWrite();
        newExtension.setAutoBuildDescription(this);
        myAutoBuildCfgExtDes = newExtension;

    }

    /**
     * Get the buildrunner with the specified name
     * 
     * @param buildRunnerName
     * @return the buildrunner with the name. If the buildrunner is not found
     *         returns the default buildrunner
     */

    public IBuildRunner getBuildRunner(String buildRunnerName) {
        if (AutoBuildProject.ARGS_INTERNAL_BUILDER_KEY.equals(buildRunnerName)) {
            return staticInternalBuildRunner;
        }
        if (AutoBuildProject.ARGS_MAKE_BUILDER_KEY.equals(buildRunnerName)) {
            return staticMakeBuildRunner;
        }
        return getBuildRunner();
    }

    public void setWritable(boolean write) {
        myIsWritable = write;

    }
}