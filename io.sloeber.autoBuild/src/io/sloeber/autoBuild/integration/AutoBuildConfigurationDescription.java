package io.sloeber.autoBuild.integration;

import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.cdtvariables.ICdtVariablesContributor;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.extension.CBuildData;
import org.eclipse.cdt.core.settings.model.extension.CTargetPlatformData;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

import io.sloeber.autoBuild.api.AutoBuildConfigurationExtensionDescription;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.api.IBuildRunner;
import io.sloeber.autoBuild.buildTools.api.IBuildTools;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager.ToolFlavour;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager.ToolType;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.helpers.api.KeyValueTree;
import io.sloeber.autoBuild.internal.AutoBuildCommon;
import io.sloeber.autoBuild.schema.api.IBuilder;
import io.sloeber.autoBuild.schema.api.IConfiguration;
import io.sloeber.autoBuild.schema.api.IOption;
import io.sloeber.autoBuild.schema.api.IProjectType;
import io.sloeber.autoBuild.schema.api.ITool;
import io.sloeber.autoBuild.schema.api.IToolChain;
import io.sloeber.autoBuild.schema.internal.Configuration;
import io.sloeber.autoBuild.schema.internal.Tool;

public class AutoBuildConfigurationDescription extends AutoBuildResourceData
		implements IAutoBuildConfigurationDescription {
	private static final String KEY_MODEL = "Model"; //$NON-NLS-1$
	private static final String KEY_CONFIGURATION = "configuration"; //$NON-NLS-1$
	private static final String KEY_TEAM= "team"; //$NON-NLS-1$
	private static final String KEY_IS_SHARED= "is shared"; //$NON-NLS-1$
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
	private static final String KEY_RESOURCE_TYPE = "resource type";//$NON-NLS-1$
	private static final String KEY_FOLDER="folder";//$NON-NLS-1$
	private static final String KEY_FILE="file";//$NON-NLS-1$
	private static final String KEY_PROJECT="project";//$NON-NLS-1$
	private static final String KEY_BUILDER_ID = "builderID";//$NON-NLS-1$
	private static final String KEY_AUTO_MAKE_TARGET = "make.target.auto";//$NON-NLS-1$
	private static final String KEY_INCREMENTAL_MAKE_TARGET = "make.target.incremental";//$NON-NLS-1$
	private static final String KEY_CLEAN_MAKE_TARGET = "make.target.clean";//$NON-NLS-1$
	private static final String KEY_EXTENSION = "extension"; //$NON-NLS-1$

	private static final String KEY_PRE_BUILD_STEP = "Build pre step"; //$NON-NLS-1$
	private static final String KEY_PRE_BUILD_ANNOUNCEMENT = "Build pre announcement"; //$NON-NLS-1$
	private static final String KEY_POST_BUILD_STEP = "Build post step"; //$NON-NLS-1$
	private static final String KEY_POST_BUILD_ANNOUNCEMENT = "Build post announcement"; //$NON-NLS-1$
	private static final String KEY_AUTOBUILD_EXTENSION_CLASS = "Extension class name"; //$NON-NLS-1$
	private static final String KEY_AUTOBUILD_EXTENSION_BUNDEL = "Extension bundel name"; //$NON-NLS-1$
	private static final String KEY_PROVIDER_ID= "provider ID"; //$NON-NLS-1$
	private static final String KEY_SELECTION_ID= "Selection"; //$NON-NLS-1$

	// Start of fields that need to be copied/made persistent
	private IConfiguration myAutoBuildConfiguration;
	private IProjectType myProjectType;

	private ICConfigurationDescription myCdtConfigurationDescription;
	private BuildTargetPlatformData myTargetPlatformData;
	private IBuildTools myBuildTools;
	private BuildBuildData myBuildBuildData;
	private boolean myIsValid = false;
	private boolean myIsTeamShared = false;
	private String myName = EMPTY_STRING;
	private String myDescription;
	private Map<String, String> myProperties = new HashMap<>();
	/*
	 * the mySelectedOptions works as follows: The Map<String, String> is the
	 * optionID, valueID. In other words the selected value for a option
	 * Map<IResource, Map<String, String>> adds the resource. The resource can not
	 * be null. In most cases the resource will be a IProject (in other words valid
	 * for all resources in the project) Map<ITool, Map<IResource, Map<String,
	 * String>>> adds the tool. tool can be null. These are the options at the level
	 * of the toolchain/configuration .... When the tool is null I would expect the
	 * IResource to be a IProject When the tool is not null this option is only
	 * valid when we deal with this tool
	 *
	 */
	private Map<IResource, Map<IOption, String>> myDefaultOptions = new HashMap<>();
	private Map<IResource, Map<IOption, String>> mySelectedOptions = new HashMap<>();
	private Map<IResource, Map<IOption, String>> myCombinedOptions = new HashMap<>();
	private String[] myRequiredErrorParserList;

	private boolean myGenerateMakeFilesAUtomatically = true;
	private boolean myStopOnFirstBuildError = true;

	private boolean myIsParallelBuild = false;

	private IBuilder myBuilder = null;
	private Map<String, IBuilder> myBuilders = new HashMap<>();
	private boolean myIsCleanBuildEnabled = false;
	private boolean myIsIncrementalBuildEnabled = false;
	private boolean myIsAutoBuildEnabled = false;
	private String myCustomBuildArguments = EMPTY_STRING;

	private boolean myUseDefaultBuildCommand = true;
	private boolean myUseStandardBuildArguments = true;
	private String myCustomBuildCommand = EMPTY_STRING;
	private int myParallelizationNum = PARRALLEL_BUILD_OPTIMAL_JOBS;
	private String myBuildFolderString = BIN_FOLDER + SLACH + CONFIG_NAME_VARIABLE;

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
	// End of fields that need to be copied/made persistent

	private String myId = CDataUtil.genId("io.sloeber.autoBuild.configurationDescription"); //$NON-NLS-1$
	private boolean myIsWritable = false;
	private boolean myForceCleanBeforeBuild = false;

	public AutoBuildConfigurationDescription(Configuration config, IProject project, IBuildTools buildTools,
			String rootCodeFolder) {
		initializeResourceData(project, rootCodeFolder, myBuildFolderString);
		myBuildTools = buildTools;
		myIsWritable = true;
		myCdtConfigurationDescription = null;
		myAutoBuildConfiguration = config;
		myProjectType = myAutoBuildConfiguration.getProjectType();
		myTargetPlatformData = new BuildTargetPlatformData();
		myName = myAutoBuildConfiguration.getName();
		myDescription = myAutoBuildConfiguration.getDescription();
		myRequiredErrorParserList = myAutoBuildConfiguration.getErrorParserList();
		myBuilders = myProjectType.getBuilders();
		myBuilder = myProjectType.getdefaultBuilder();
		if (myBuilder == null) {
			System.err.println("project " + project.getName() + " has no default builder"); //$NON-NLS-1$ //$NON-NLS-2$
			myBuilder = AutoBuildManager.getDefaultBuilder();
		}
		myIsCleanBuildEnabled = myBuilder.getBuildRunner().supportsCleanBuild();
		myIsIncrementalBuildEnabled = myBuilder.getBuildRunner().supportsIncrementalBuild();
		myIsAutoBuildEnabled = myBuilder.getBuildRunner().supportsAutoBuild();

	}

	// Copy constructor
	public AutoBuildConfigurationDescription(ICConfigurationDescription cfgDescription,
			AutoBuildConfigurationDescription base, boolean clone) {

		myIsWritable = !cfgDescription.isReadOnly();
		if (clone) {
			myId = base.getId();
		}
		myBuildTools = base.getBuildTools();
		myAutoBuildConfiguration = base.myAutoBuildConfiguration;
		myProjectType = myAutoBuildConfiguration.getProjectType();
		myCdtConfigurationDescription = cfgDescription;
		myBuilder = base.myBuilder;
		myBuilders = base.myBuilders;
		myTargetPlatformData = new BuildTargetPlatformData(base.myTargetPlatformData, clone);
		myBuildBuildData = new BuildBuildData(this, base.myBuildBuildData, clone);
		myIsValid = base.myIsValid;
		myName = myCdtConfigurationDescription.getName();
		myDescription = base.myDescription;
		myIsTeamShared=base.myIsTeamShared;
		myProperties.clear();
		myProperties.putAll(base.myProperties);
		options_copy(base.mySelectedOptions, mySelectedOptions);
		options_copy(base.myDefaultOptions, myDefaultOptions);
		options_copy(base.myCombinedOptions, myCombinedOptions);
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

		myIsAutoBuildEnabled = base.myIsAutoBuildEnabled;

		myAutoMakeTarget = base.myAutoMakeTarget;
		myIncrementalMakeTarget = base.myIncrementalMakeTarget;
		myCleanMakeTarget = base.myCleanMakeTarget;

		myPreBuildStep = base.myPreBuildStep;
		myPreBuildAnnouncement = base.myPreBuildAnnouncement;
		myPostBuildStep = base.myPostBuildStep;
		myPostBuildStepAnouncement = base.myPostBuildStepAnouncement;
		myBuilders = base.myBuilders;
		myForceCleanBeforeBuild = base.myForceCleanBeforeBuild;
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
		clone(this, base, clone);
		if (base.getAutoBuildConfigurationExtensionDescription() != null) {
			AutoBuildConfigurationExtensionDescription baseExtensionDesc = base
					.getAutoBuildConfigurationExtensionDescription();

			try {
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
			} catch (Exception e) {
				System.err.println(
						"ERROR: Failed to create class derived from AutoBuildConfigurationExtensionDescription"); //$NON-NLS-1$
				e.printStackTrace();
			}
		}
	}

	@Override
	public IBuildTools getBuildTools() {
		if (myBuildTools == null) {
			// TODO add real error warning
			System.err.println("AutoBuildConfigurationDescription.myBuildTools should never be null"); //$NON-NLS-1$
			myBuildTools = IBuildToolsManager.getDefault().getAnyInstalledBuildTools(myProjectType);
		}
		return myBuildTools;
	}

	@Override
	public void setBuildTools(IBuildTools buildTools) {
		myBuildTools = buildTools;
		forceCleanBeforeBuild();
	}

	/**
	 * Create a configuration based on persisted content. The persisted content can
	 * contain multiple configurations and as sutch a filtering is needed The
	 * lineStart and lineEnd are used to filter content only applicable to this
	 * configuration
	 *
	 * @param cfgDescription the CDT configuration this object will belong to
	 * @param curConfigsText the persistent content
	 * @param lineStart      only consider lines that start with this string
	 * @param lineEnd        only consider lines that end with this string
	 */
	public AutoBuildConfigurationDescription(ICConfigurationDescription cfgDescription, KeyValueTree keyValues) {
		super(cfgDescription, keyValues.getChild(KEY_SOURCE_ENTRY));
		myId = cfgDescription.getId();
		myIsWritable = !cfgDescription.isReadOnly();
		String extensionPointID = null;
		String extensionID = null;
		String projectTypeID = null;
		String confName = null;
		myCdtConfigurationDescription = cfgDescription;


		KeyValueTree modelKeyValues =keyValues.getChild(KEY_MODEL);
		KeyValueTree buildToolsKeyValues =keyValues.getChild(KEY_BUILDTOOLS);
		KeyValueTree projectTypeKeyValues =modelKeyValues.getChild(KEY_PROJECT_TYPE);
		KeyValueTree configKeyValues =modelKeyValues.getChild(KEY_CONFIGURATION);
		KeyValueTree teamKeyValues =keyValues.getChild(KEY_TEAM);
		myIsTeamShared=Boolean.parseBoolean(teamKeyValues.getValue(KEY_IS_SHARED));

		myName=keyValues.getValue(NAME);
		myDescription=keyValues.getValue(DESCRIPTION);
		myBuildFolderString=keyValues.getValue(KEY_BUILDFOLDER);
		myCustomBuildCommand=keyValues.getValue(KEY_CUSTOM_BUILD_COMMAND);
		String builderID=keyValues.getValue(KEY_BUILDER_ID);
		myAutoMakeTarget=keyValues.getValue(KEY_AUTO_MAKE_TARGET);
		myIncrementalMakeTarget=keyValues.getValue(KEY_INCREMENTAL_MAKE_TARGET);
		myCleanMakeTarget=keyValues.getValue(KEY_CLEAN_MAKE_TARGET);
		myPreBuildStep=keyValues.getValue(KEY_PRE_BUILD_STEP);
		myPreBuildAnnouncement=keyValues.getValue(KEY_PRE_BUILD_ANNOUNCEMENT);
		myPostBuildStep=keyValues.getValue(KEY_POST_BUILD_STEP);
		myPostBuildStepAnouncement=keyValues.getValue(KEY_POST_BUILD_ANNOUNCEMENT);
		String autoCfgExtentionDesc=keyValues.getValue(KEY_AUTOBUILD_EXTENSION_CLASS);
		myName=keyValues.getValue(NAME);
		String autoCfgExtentionBundel=keyValues.getValue( KEY_AUTOBUILD_EXTENSION_BUNDEL);
		myUseDefaultBuildCommand= Boolean.parseBoolean(keyValues.getValue(KEY_USE_DEFAULT_BUILD_COMMAND));
		myGenerateMakeFilesAUtomatically= Boolean.parseBoolean(keyValues.getValue(KEY_GENERATE_MAKE_FILES_AUTOMATICALLY));
		myUseStandardBuildArguments= Boolean.parseBoolean(keyValues.getValue(KEY_USE_STANDARD_BUILD_ARGUMENTS));
		myStopOnFirstBuildError= Boolean.parseBoolean(keyValues.getValue(KEY_STOP_ON_FIRST_ERROR));
		myIsParallelBuild= Boolean.parseBoolean(keyValues.getValue(KEY_IS_PARRALLEL_BUILD));
		myIsCleanBuildEnabled= Boolean.parseBoolean(keyValues.getValue(KEY_IS_CLEAN_BUILD_ENABLED));
		myIsIncrementalBuildEnabled= Boolean.parseBoolean(keyValues.getValue(KEY_IS_INCREMENTAL_BUILD_ENABLED));
		myIsAutoBuildEnabled= Boolean.parseBoolean(keyValues.getValue(KEY_IS_AUTO_BUILD_ENABLED));
		myParallelizationNum=Integer.parseInt(keyValues.getValue(KEY_NUM_PARRALEL_BUILDS));


		String providerID =buildToolsKeyValues.getValue(	KEY_PROVIDER_ID);
		String selectionID =buildToolsKeyValues.getValue(	KEY_PROVIDER_ID);
		myBuildTools = IBuildToolsManager.getDefault().getBuildTools(providerID, selectionID);


		extensionPointID=projectTypeKeyValues.getValue(KEY_EXTENSION_POINT_ID);
		extensionID=projectTypeKeyValues.getValue(KEY_EXTENSION_ID);
		projectTypeID=projectTypeKeyValues.getValue(ID);
		confName=configKeyValues.getValue(NAME);

		myProjectType = AutoBuildManager.getProjectType(extensionPointID, extensionID, projectTypeID, true);
		if (myBuildTools == null) {
			// TODO add real error warning
			System.err.println("unable to identify build Tools "); //$NON-NLS-1$
			myBuildTools = IBuildToolsManager.getDefault().getAnyInstalledBuildTools(myProjectType);
		}
		myAutoBuildConfiguration = myProjectType.getConfiguration(confName);
		for (IBuilder buildRunner : getAvailableBuilders()) {
			if (myBuilder == null || buildRunner.getId().equals(builderID)) {
				myBuilder = buildRunner;
			}
		}
		myTargetPlatformData = new BuildTargetPlatformData();
		myBuildBuildData = new BuildBuildData(this);
		myRequiredErrorParserList = myAutoBuildConfiguration.getErrorParserList();
		options_updateDefault();

		// Now we have reconstructed the environment and read out the persisted content
		// it is time to
		// reconstruct the more complicated fields

		IProject project = getProject();
		options_combine();


		KeyValueTree propertiesKeyValues =keyValues.getChild(KEY_PROPERTY);
		for (KeyValueTree curProp : propertiesKeyValues.getChildren().values()) {
			myProperties.put(curProp.getKey(), curProp.getValue());
		}


		KeyValueTree optionsKeyValue=keyValues.getChild( OPTION );
		for (KeyValueTree curResourceProp : optionsKeyValue.getChildren().values()) {
			IResource resource = getResource(project, curResourceProp);
			Map<IOption, String> resourceOptions=new HashMap<>();
			for (KeyValueTree curOption : curResourceProp.getChildren().values()) {
				String value=curOption.getValue(  KEY_VALUE );
				IOption option = myProjectType.getOption( curOption.getValue(  KEY  ));
				resourceOptions.put(option, value);
			}
			mySelectedOptions.put(resource, resourceOptions);
		}









		KeyValueTree toolCommandKeyValue=keyValues.getChild( KEY_CUSTOM_TOOL_COMMAND );
		for (KeyValueTree curToolCommand : toolCommandKeyValue.getChildren().values()) {
			String toolID=curToolCommand.getValue(KEY);
			ITool tool = myProjectType.getToolChain().getTool(toolID);
			Map<IResource, String> resourceOptions=new HashMap<>();
			for (KeyValueTree curOption : curToolCommand.getChildren().values()) {
				IResource resource = getResource(project, curOption);
				resourceOptions.put(resource, curOption.getValue(  KEY_VALUE ));
			}
			myCustomToolCommands.put(tool, resourceOptions);
		}




		KeyValueTree toolPatternKeyValue=keyValues.getChild( KEY_CUSTOM_TOOL_PATTERN );
		for (KeyValueTree curToolCommand : toolPatternKeyValue.getChildren().values()) {
			String toolID=curToolCommand.getValue(KEY);
			ITool tool = myProjectType.getToolChain().getTool(toolID);
			Map<IResource, String> resourceOptions=new HashMap<>();
			for (KeyValueTree curOption : curToolCommand.getChildren().values()) {
				IResource resource = getResource(project, curOption);
				resourceOptions.put(resource, curOption.getValue(  KEY_VALUE ));
			}
			myCustomToolPattern.put(tool, resourceOptions);
		}


		// load the auto Build configuration extension description
		if (autoCfgExtentionDesc != null && autoCfgExtentionBundel != null && (!autoCfgExtentionDesc.isBlank())
				&& (!autoCfgExtentionBundel.isBlank())) {
			try {
				Bundle contributingBundle = getBundle(autoCfgExtentionBundel);
				Class<?> autoCfgExtentionDescClass = contributingBundle.loadClass(autoCfgExtentionDesc);
				Constructor<?> ctor = autoCfgExtentionDescClass.getDeclaredConstructor(
						IAutoBuildConfigurationDescription.class, KeyValueTree.class);
				ctor.setAccessible(true);

				myAutoBuildCfgExtDes = (AutoBuildConfigurationExtensionDescription) ctor.newInstance(this,
						keyValues.getChild( KEY_EXTENSION ));
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


	private static IResource getResource(IProject project,KeyValueTree keyValueTree) {
		String resourceID =keyValueTree.getValue(  KEY_RESOURCE );
		String resourceType =keyValueTree.getValue(  KEY_RESOURCE_TYPE );
		switch(resourceType) {
		case KEY_FILE:
			 return project.getFile(resourceID);
	case KEY_FOLDER:
		 return project.getFolder(resourceID);
		 default:
case KEY_PROJECT:
	 return project;
}
	}

	private static Bundle getBundle(String symbolicName) {
		return Platform.getBundle(symbolicName);
	}

	/**
	 * get the default options and update the combined options
	 */
	private void options_updateDefault() {
		myDefaultOptions.clear();
		IProject project = getProject();
		Map<IOption, String> defaultOptions = myAutoBuildConfiguration.getDefaultOptions(project, this);
		IToolChain toolchain = myAutoBuildConfiguration.getProjectType().getToolChain();
		defaultOptions.putAll(toolchain.getDefaultOptions(project, this));

		for (ITool curITool : toolchain.getTools()) {
			Tool curTool = (Tool) curITool;
			if (!curTool.isEnabled(project, this)) {
				continue;
			}
			// Map<IResource, Map<String, String>> resourceOptions = new HashMap<>();
			defaultOptions.putAll(curTool.getDefaultOptions(project, this));

		}
		myDefaultOptions.put(null, defaultOptions);
		options_combine();
	}

	/*
	 * take myDefaultOptions and mySelected options and combine them in
	 * myCombinedOptions From now onwards the combined options are to be used as
	 * they take the default and the user selected option into account in the
	 * desired way
	 */
	private void options_combine() {
		myCombinedOptions.clear();
		options_copy(myDefaultOptions, myCombinedOptions);
		options_copy(mySelectedOptions, myCombinedOptions);

	}

	/*
	 * take options and make a copy
	 */
	private static void options_copy(Map<IResource, Map<IOption, String>> from,
			Map<IResource, Map<IOption, String>> to) {
		for (Entry<IResource, Map<IOption, String>> fromResourceSet : from.entrySet()) {
			IResource curResource = fromResourceSet.getKey();
			Map<IOption, String> curFromOptions = fromResourceSet.getValue();

			Map<IOption, String> curToOptions = to.get(curResource);
			if (curToOptions == null) {
				curToOptions = new HashMap<>();
				to.put(curResource, curToOptions);
			}
			curToOptions.putAll(curFromOptions);
		}
	}

	public void setCdtConfigurationDescription(ICConfigurationDescription cfgDescription) {
		checkIfWeCanWrite();
		myCdtConfigurationDescription = cfgDescription;
		myBuildBuildData = new BuildBuildData(this);
		options_updateDefault();

		myIsValid = true;
	}

	@Override
	public ICConfigurationDescription getCdtConfigurationDescription() {
		return myCdtConfigurationDescription;
	}

	@Override
	public IProject getProject() {
		return myCdtConfigurationDescription.getProjectDescription().getProject();
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
		return myIsValid;
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

	private static TreeMap<IOption, String> getSortedOptionMap() {
		TreeMap<IOption, String> ret = new TreeMap<>(new java.util.Comparator<>() {

			@Override
			public int compare(IOption o1, IOption o2) {
				if (o1 == null || o2 == null) {
					return 0;
				}
				return o1.getId().compareTo(o2.getId());
			}
		});
		return ret;
	}

	@Override
	public TreeMap<IOption, String> getSelectedOptions(Set<? extends IResource> file, ITool tool) {
		TreeMap<IOption, String> ret = getSortedOptionMap();
		for (IResource curFile : file) {
			Map<IOption, String> fileOptions = getSelectedOptions(curFile, tool);
			for (Entry<IOption, String> curResourceOption : fileOptions.entrySet()) {
				IOption curKey = curResourceOption.getKey();
				String curValue = curResourceOption.getValue();
				if (ret.containsKey(curKey)) {
					if (!ret.get(curKey).equals(curValue)) {
						// TOFIX log error
					}
				}
				ret.put(curKey, curValue);
			}
		}
		return ret;
	}

	/**
	 * provide all the options for the resource taking into account setting on the
	 * project level; folder level and file level. The result are the options that
	 * should be used to build commands for the file suppose the file
	 * src/folder1/folder2/sourceFile.cpp and the option A
	 *
	 * src folder1 folder2 sourceFile.cpp value 1 2 3 4 4 1 2 3 3 1 2 2 1 1
	 *
	 */
	@Override
	public TreeMap<IOption, String> getSelectedOptions(IResource file) {

		Map<IOption, String> retProject = new HashMap<>();
		Map<Integer, Map<IOption, String>> retFolder = new HashMap<>();
		Map<IOption, String> retFile = new HashMap<>();
		for (Entry<IResource, Map<IOption, String>> curResourceOptions : myCombinedOptions.entrySet()) {
			IResource curResource = curResourceOptions.getKey();
			if (curResource == null || curResource instanceof IProject) {
				// null means project level and as sutch is valid for all resources
				retProject.putAll(curResourceOptions.getValue());
				continue;
			}
			if (curResource instanceof IFolder) {
				if (curResource.getProjectRelativePath().isPrefixOf(file.getProjectRelativePath())) {

					retFolder.put(Integer.valueOf(curResource.getProjectRelativePath().segmentCount()),
							curResourceOptions.getValue());

				}
				continue;
			}
			if ((curResource instanceof IFile) && (curResource.equals(file))) {
				retFile.putAll(curResourceOptions.getValue());
				continue;
			}
		}
		TreeMap<IOption, String> ret = getSortedOptionMap();
		ret.putAll(retProject);
		TreeSet<Integer> segments = new TreeSet<>(retFolder.keySet());
		for (Integer curSegment : segments) {
			ret.putAll(retFolder.get(curSegment));
		}
		ret.putAll(retFile);
		return ret;
	}

	@Override
	public TreeMap<IOption, String> getSelectedOptions(IResource file, ITool tool) {
		TreeMap<IOption, String> ret = getSelectedOptions(file);

		// remove all options not known to the tool
		List<IOption> toolOptions = tool.getOptions().getOptions();// TOFIX : this should be get Enabled Options
		for (Iterator<IOption> iterator = ret.keySet().iterator(); iterator.hasNext();) {
			IOption curOption = iterator.next();
			if (!toolOptions.contains(curOption)) {
				iterator.remove();
			}
		}
		return ret;
	}

	public String[] getErrorParserList() {
		// TODO JABA I for now return the required but maybe this should be the selected
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
		IFolder oldBuildFolder = getBuildFolder();
		if (oldBuildFolder != null && oldBuildFolder.exists()) {
			try {
				oldBuildFolder.delete(true, new NullProgressMonitor());
			} catch (CoreException e) {
				e.printStackTrace();
			}
		}
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
		return getProject().getFolder(resolved);
	}

	@Override
	public String getBuildCommand(boolean includeArgs) {
		String command = null;
		if (myUseDefaultBuildCommand) {
			command = getBuildTools().getBuildCommand();
			if (command == null || command.isBlank()) {
				command = myBuilder.getCommand();
			}
		} else {
			command = getCustomBuildCommand();
		}

		if (includeArgs) {
			String args = myBuilder.getArguments( myParallelizationNum, myStopOnFirstBuildError);
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
	public int getParallelizationNum(boolean actualNumber) {
		if (!actualNumber) {
			return myParallelizationNum;
		}
		if (!isParallelBuild()) {
			return 1;
		}
		switch (myParallelizationNum) {
		case PARRALLEL_BUILD_UNLIMITED_JOBS:
			return 999;
		case PARRALLEL_BUILD_OPTIMAL_JOBS:
			return AutoBuildCommon.getOptimalParallelJobNum();
		default:
			return myParallelizationNum;
		}

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


	@Override
	public void serialize(KeyValueTree keyValuePairs) {
		super.serialize(keyValuePairs.addChild(KEY_SOURCE_ENTRY ));
		final int counterStart = 0;
		KeyValueTree modelKeyValue=keyValuePairs.addChild(KEY_MODEL );
		KeyValueTree projectTypeKeyValue=modelKeyValue.addChild(KEY_PROJECT_TYPE );

		KeyValueTree teamKeyValues =keyValuePairs.addChild(KEY_TEAM);
		teamKeyValues.addChild(KEY_IS_SHARED,String.valueOf(myIsTeamShared));

		projectTypeKeyValue.addChild(  KEY_EXTENSION_POINT_ID , getExtensionPointID());
		projectTypeKeyValue.addChild( KEY_EXTENSION_ID , getExtensionID() );
		projectTypeKeyValue.addChild(  ID, myProjectType.getId() );

		KeyValueTree configurationKeyValue=modelKeyValue.addChild(  KEY_CONFIGURATION);
		configurationKeyValue.addChild(  NAME , myAutoBuildConfiguration.getName() );

		keyValuePairs.addChild( NAME , myName );
		keyValuePairs.addChild( DESCRIPTION ,myDescription);


		KeyValueTree buildToolsKeyValue=keyValuePairs.addChild( KEY_BUILDTOOLS );
		buildToolsKeyValue.addChild( KEY_PROVIDER_ID,getBuildTools().getProviderID());
		buildToolsKeyValue.addChild( KEY_SELECTION_ID, getBuildTools().getSelectionID() );

		keyValuePairs.addChild( KEY_BUILDFOLDER , myBuildFolderString );
		keyValuePairs.addChild( KEY_USE_DEFAULT_BUILD_COMMAND , String.valueOf(myUseDefaultBuildCommand)
				);
		keyValuePairs.addChild( KEY_GENERATE_MAKE_FILES_AUTOMATICALLY ,String.valueOf(myGenerateMakeFilesAUtomatically) );
		keyValuePairs.addChild( KEY_USE_STANDARD_BUILD_ARGUMENTS , String.valueOf(myUseStandardBuildArguments)
				);
		keyValuePairs.addChild( KEY_STOP_ON_FIRST_ERROR , String.valueOf(myStopOnFirstBuildError) );
		keyValuePairs.addChild( KEY_IS_PARRALLEL_BUILD , String.valueOf(myIsParallelBuild) );
		keyValuePairs.addChild( KEY_IS_CLEAN_BUILD_ENABLED , String.valueOf(myIsCleanBuildEnabled) );
		keyValuePairs.addChild( KEY_IS_INCREMENTAL_BUILD_ENABLED , String.valueOf(myIsIncrementalBuildEnabled)
				);
		keyValuePairs.addChild( KEY_NUM_PARRALEL_BUILDS , String.valueOf(myParallelizationNum) );
		keyValuePairs.addChild( KEY_CUSTOM_BUILD_COMMAND , myCustomBuildCommand );
		keyValuePairs.addChild( KEY_BUILDER_ID , myBuilder.getId() );
		keyValuePairs.addChild( KEY_AUTO_MAKE_TARGET , myAutoMakeTarget );
		keyValuePairs.addChild( KEY_INCREMENTAL_MAKE_TARGET , myIncrementalMakeTarget );
		keyValuePairs.addChild( KEY_CLEAN_MAKE_TARGET , myCleanMakeTarget );

		keyValuePairs.addChild( KEY_PRE_BUILD_STEP , myPreBuildStep );
		keyValuePairs.addChild( KEY_PRE_BUILD_ANNOUNCEMENT , myPreBuildAnnouncement );
		keyValuePairs.addChild( KEY_POST_BUILD_STEP , myPostBuildStep );
		keyValuePairs.addChild( KEY_POST_BUILD_ANNOUNCEMENT , myPostBuildStepAnouncement );


		KeyValueTree propertiesKeyValue=keyValuePairs.addChild( KEY_PROPERTY );
		for (Entry<String, String> curProp : myProperties.entrySet()) {
			propertiesKeyValue.addChild(  curProp.getKey(), curProp.getValue() );
		}

		int counter = counterStart;
		KeyValueTree optionsKeyValue=keyValuePairs.addChild( OPTION );
		for (Entry<IResource, Map<IOption, String>> curOption : mySelectedOptions.entrySet()) {
			IResource resource= curOption.getKey();
			String resourceID = resource.getProjectRelativePath().toString();
			KeyValueTree curResourceKeyValue=optionsKeyValue.addChild(  String.valueOf(counter) );
			curResourceKeyValue.addChild(  KEY_RESOURCE , resourceID);
			if(resource instanceof IFolder) {
				curResourceKeyValue.addChild(  KEY_RESOURCE_TYPE , KEY_FOLDER);
			}
			if(resource instanceof IFile) {
				curResourceKeyValue.addChild(  KEY_RESOURCE_TYPE , KEY_FILE);
			}
			if(resource instanceof IProject) {
				curResourceKeyValue.addChild(  KEY_RESOURCE_TYPE , KEY_PROJECT);
			}
			counter++;
			int counter2 = counterStart;
			for (Entry<IOption, String> resourceOptions : curOption.getValue().entrySet()) {
				KeyValueTree curOptionKeyValue=curResourceKeyValue.addChild(  String.valueOf(counter2) );
				curOptionKeyValue.addChild(  KEY  , resourceOptions.getKey().getId() );
				curOptionKeyValue.addChild(  KEY_VALUE  , resourceOptions.getValue() );
				counter2++;
			}
		}

		counter = counterStart;
		KeyValueTree customToolKeyValue=keyValuePairs.addChild( KEY_CUSTOM_TOOL_COMMAND );
		for (Entry<ITool, Map<IResource, String>> curCustomToolCommands : myCustomToolCommands.entrySet()) {
			ITool tool = curCustomToolCommands.getKey();
			customToolKeyValue.addChild( KEY ,tool.getId());

			for (Entry<IResource, String> curResourceCommand : curCustomToolCommands.getValue().entrySet()) {
				IResource res = curResourceCommand.getKey();
				KeyValueTree curOptionKeyValue=customToolKeyValue.addChild(  String.valueOf(counter) );
				String resourceID = res.getProjectRelativePath().toString();
				curOptionKeyValue.addChild( KEY_VALUE , curResourceCommand.getValue() );
				curOptionKeyValue.addChild(  KEY_RESOURCE , resourceID );
				counter++;
			}
		}

		counter = counterStart;
		KeyValueTree customToolPatternKeyValue=keyValuePairs.addChild( KEY_CUSTOM_TOOL_PATTERN );
		for (Entry<ITool, Map<IResource, String>> curCustomToolCommands : myCustomToolPattern.entrySet()) {
			ITool tool = curCustomToolCommands.getKey();
			customToolPatternKeyValue.addChild( KEY ,tool.getId());
			for (Entry<IResource, String> curResourceCommand : curCustomToolCommands.getValue().entrySet()) {
				IResource res = curResourceCommand.getKey();
				String resourceID = res.getProjectRelativePath().toString();
				KeyValueTree curOptionKeyValue=customToolPatternKeyValue.addChild(  String.valueOf(counter) );
				curOptionKeyValue.addChild(  KEY_VALUE , curResourceCommand.getValue() );
				curOptionKeyValue.addChild(  KEY_RESOURCE , resourceID );
				counter++;
			}
		}

		if (myAutoBuildCfgExtDes != null) {
			Class<? extends AutoBuildConfigurationExtensionDescription> referencedClass = myAutoBuildCfgExtDes
					.getClass();

			keyValuePairs.addChild( KEY_AUTOBUILD_EXTENSION_BUNDEL , myAutoBuildCfgExtDes.getBundelName()
					);
			keyValuePairs.addChild( KEY_AUTOBUILD_EXTENSION_CLASS , referencedClass.getName() );
			myAutoBuildCfgExtDes.serialize(keyValuePairs.addChild(KEY_EXTENSION));
		}
	}

	@Override
	public IBuilder getBuilder() {
		return myBuilder;
	}

	@Override
	public Set<IBuilder> getAvailableBuilders() {
		if (myBuilders.size() == 0) {
			myBuilders = myProjectType.getBuilders();
		}
		return new HashSet<>(myBuilders.values());
	}

	@Override
	public void setBuilder(IBuilder builder) {
		checkIfWeCanWrite();
		myBuilder = builder;
		IBuildRunner buildRunner = myBuilder.getBuildRunner();
		myIsCleanBuildEnabled = myIsCleanBuildEnabled && buildRunner.supportsCleanBuild();
		myIsIncrementalBuildEnabled = myIsIncrementalBuildEnabled && buildRunner.supportsIncrementalBuild();
		myIsAutoBuildEnabled = myIsAutoBuildEnabled && buildRunner.supportsAutoBuild();
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
			// TOFIX need to throw exception one way or another
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
			// This should not happen; ignore
			return;
		}
		if (resource == null) {
			// request to remove all custom commands for this tool
			myCustomToolCommands.remove(tool);
			return;
		}
		Map<IResource, String> customCommands = myCustomToolCommands.get(tool);
		if (customCommands == null && customCommand == null) {
			// Request to remove something that doesn't exists => nothing to do
			return;
		}
		if (customCommands == null) {
			customCommands = new HashMap<>();
			myCustomToolCommands.put(tool, customCommands);
		}
		customCommands.put(resource, customCommand);
		forceCleanBeforeBuild();
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
			// This should not happen; ignore
			return;
		}
		if (resource == null) {
			// request to remove all custom patterns for this tool
			myCustomToolPattern.remove(tool);
			return;
		}
		Map<IResource, String> customCommands = myCustomToolPattern.get(tool);
		if (customCommands == null && pattern == null) {
			// Request to remove something that doesn't exists => nothing to do
			return;
		}
		if (customCommands == null) {
			customCommands = new HashMap<>();
			myCustomToolPattern.put(tool, customCommands);
		}
		customCommands.put(resource, pattern);
		forceCleanBeforeBuild();

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
		setOptionValueInternal(resource, option, valueID);
		options_combine();
	}

	private void setOptionValueInternal(IResource resource, IOption option, String value) {
		// Map<IResource, Map<String, String>> resourceOptions =
		// mySelectedOptions.get(tool);
		// if (resourceOptions == null) {
		// if (valueID == null || valueID.isBlank()) {
		// //as it does not exist and we want to erase do nothing
		// return;
		// }
		// resourceOptions = new HashMap<>();
		// mySelectedOptions.put(tool, resourceOptions);
		// }

		Map<IOption, String> options = mySelectedOptions.get(resource);
		if (options == null) {
			if (value == null || value.isBlank()) {
				// as it does not exist and we want to erase do nothing
				return;
			}
			options = new HashMap<>();
			mySelectedOptions.put(resource, options);
		}
		if (value == null || value.isBlank()) {
			options.remove(option);
		} else {
			options.put(option, value);
		}

	}

	@Override
	public String getOptionValue(IResource resource, ITool tool, IOption option) {
		if (tool.getOption(option.getId()) == null) {
			// the tool does not know this option
			return EMPTY_STRING;
		}

		if (myCombinedOptions == null) {
			// there are no options selected by the user
			return EMPTY_STRING;
		}

		Map<IOption, String> resourceOptions = myCombinedOptions.get(resource);
		if (resourceOptions == null) {
			// there are no options selected by the user for this resource
			return EMPTY_STRING;
		}
		String ret = resourceOptions.get(option);
		if (ret == null) {
			// there are no options selected by the user for this resource/option
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
		myTargetPlatformData = new BuildTargetPlatformData();
		// myName = myAutoBuildConfiguration.getName();
		// myDescription = myAutoBuildConfiguration.getDescription();
		myRequiredErrorParserList = myAutoBuildConfiguration.getErrorParserList();
		options_updateDefault();
		forceCleanBeforeBuild();
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
	 * Get the buildrunner with the specified id
	 *
	 * @param buildRunnerID
	 * @return the buildrunner with the id. If the buildrunner is not found returns
	 *         the default buildrunner
	 */
	@Override
	public IBuilder getBuilder(String builderID) {
		IBuilder ret = myBuilders.get(builderID);
		if (ret != null) {
			return ret;
		}
		return getBuilder();
	}

	public void setWritable(boolean write) {
		myIsWritable = write;

	}

	@Override
	public ToolFlavour getBuildToolsFlavour() {
		return getBuildTools().getToolFlavour();
	}

	@Override
	public void forceCleanBeforeBuild() {
		myForceCleanBeforeBuild = true;
	}

	public void forceFullBuildIfNeeded(IProgressMonitor monitor) throws CoreException {
		if (myForceCleanBeforeBuild) {
			myForceCleanBeforeBuild = false;
			IFolder buildFolder = getBuildFolder();
			if (buildFolder != null && buildFolder.exists()) {
				buildFolder.delete(true, monitor);
			}

		}

	}

	private static String getSpecFile(String languageId) {
		String ext = EXTENSION_CPP;
		if (LANGUAGEID_C.equals(languageId)) {
			ext = EXTENSION_C;
		}
		String specFileName = SPEC_BASE + DOT + ext;
		IPath ret = Activator.getInstance().getStateLocation().append(specFileName);
		File specFile = new java.io.File(ret.toOSString());
		if (!specFile.exists()) {
			try {
				specFile.createNewFile();
			} catch (IOException e) {
				Activator.log(e);
			}
		}
		return ret.toString();
	}

	@Override
	public String getDiscoveryCommand(String languageId) {
		String specInFile = getSpecFile(languageId);
		String basicCommand = internalgetDiscoveryCommand(languageId);
		if (basicCommand == null || basicCommand.isBlank()) {
			return null;
		}
		if (!basicCommand.contains(BLANK)) {
			// No blanks so this may be a environment var that needs 1 level of expansion
			basicCommand = AutoBuildCommon.getVariableValue(basicCommand, basicCommand, false, this);
		}
		basicCommand = basicCommand.replace(INPUTS_VARIABLE, specInFile);
		return AutoBuildCommon.resolve(basicCommand, this);
	}

	private String internalgetDiscoveryCommand(String languageId) {
		if (myAutoBuildCfgExtDes != null) {
			String ret = myAutoBuildCfgExtDes.getDiscoveryCommand(languageId);
			if (ret != null) {
				return ret;
			}
		}
		List<ITool> tools = myProjectType.getToolChain().getTools();
		switch (languageId) {
		case LANGUAGEID_CPP:
		case LANGUAGEID_C:
			for (ITool curTool : tools) {
				if (curTool.isForLanguage(languageId)) {
					ToolType toolType = curTool.getToolType();
					if (toolType == null) {
						continue;
					}
					String command = myBuildTools.getDiscoveryCommand(toolType);
					if (command != null) {
						return command;
					}
				}
			}
			break;
		case LANGUAGEID_ASSEMBLY:
			// We do not do discovery for assembly
			break;
		default:
			System.err.println("getDiscoveryCommand got unsupported language id " + languageId); //$NON-NLS-1$
			break;
		}
		return null;
	}

	@Override
	public String[] getEnvironmentVariables() {
		// Get the environment variables
		IEnvironmentVariableManager mngr = CCorePlugin.getDefault().getBuildEnvironmentManager();
		IEnvironmentVariable[] vars = mngr.getVariables(getCdtConfigurationDescription(), true);
		Map<String, String> envMap = new HashMap<>();
		for (IEnvironmentVariable var : vars) {
			envMap.put(var.getName(), var.getValue());
		}

		IBuildTools buildTools = getBuildTools();
		if (buildTools != null) {
			if (buildTools.getEnvironmentVariables() != null) {
				for (Entry<String, String> curEnv : buildTools.getEnvironmentVariables().entrySet()) {
					envMap.put(curEnv.getKey(), curEnv.getValue());
				}
			}
			if (buildTools.getPathExtension() != null) {
				String systemPath = envMap.get(ENV_VAR_PATH);
				if (systemPath == null) {
					envMap.put(ENV_VAR_PATH, buildTools.getPathExtension());
				} else {
					envMap.put(ENV_VAR_PATH, buildTools.getPathExtension() + File.pathSeparator + systemPath);
				}
			}
		}

		Set<String> envSet = new HashSet<>();
		for (Entry<String, String> curEnv : envMap.entrySet()) {
			envSet.add(curEnv.getKey() + EQUAL + curEnv.getValue());
		}
		return envSet.toArray(new String[envSet.size()]);
	}

	@Override
	public boolean isTeamShared() {
		return myIsTeamShared;
	}

	@Override
	public void setTeamShared(boolean isTeamShared) {
		myIsTeamShared= isTeamShared;
	}

}
