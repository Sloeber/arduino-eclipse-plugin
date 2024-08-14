package io.sloeber.autoBuild.integration;

import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.*;
import static io.sloeber.autoBuild.api.AutoBuildCommon.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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

import io.sloeber.autoBuild.api.AutoBuildCommon;
import io.sloeber.autoBuild.api.AutoBuildConfigurationExtensionDescription;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.api.IBuildRunner;
import io.sloeber.autoBuild.buildTools.api.IBuildTools;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager.ToolFlavour;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager.ToolType;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.helpers.api.KeyValueTree;
import io.sloeber.autoBuild.schema.api.IBuilder;
import io.sloeber.autoBuild.schema.api.IConfiguration;
import io.sloeber.autoBuild.schema.api.IOption;
import io.sloeber.autoBuild.schema.api.IProjectType;
import io.sloeber.autoBuild.schema.api.ITool;
import io.sloeber.autoBuild.schema.internal.Configuration;

public class AutoBuildConfigurationDescription extends AutoBuildResourceData
		implements IAutoBuildConfigurationDescription {

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
	private AutoBuildOptions myOptions;
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
	private Set<String> myTeamExclusionKeys = null;
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
		myOptions = new AutoBuildOptions();

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
		myOptions = new AutoBuildOptions(base.myOptions);
		myIsValid = base.myIsValid;
		myName = myCdtConfigurationDescription.getName();
		myDescription = base.myDescription;
		myIsTeamShared = base.myIsTeamShared;
		myProperties.clear();
		myProperties.putAll(base.myProperties);

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
		if (base.myTeamExclusionKeys == null) {
			myTeamExclusionKeys = null;
		} else {
			myTeamExclusionKeys = new TreeSet<>(base.myTeamExclusionKeys);
		}

		myPreBuildStep = base.myPreBuildStep;
		myPreBuildAnnouncement = base.myPreBuildAnnouncement;
		myPostBuildStep = base.myPostBuildStep;
		myPostBuildStepAnouncement = base.myPostBuildStepAnouncement;
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

		KeyValueTree modelKeyValues = keyValues.getChild(KEY_MODEL);
		KeyValueTree buildToolsKeyValues = keyValues.getChild(KEY_BUILDTOOLS);
		KeyValueTree projectTypeKeyValues = modelKeyValues.getChild(KEY_PROJECT_TYPE);
		KeyValueTree configKeyValues = modelKeyValues.getChild(KEY_CONFIGURATION);
		KeyValueTree teamKeyValues = keyValues.getChild(KEY_TEAM);
		myIsTeamShared = Boolean.parseBoolean(teamKeyValues.getValue(KEY_IS_SHARED));
		String teamExclusionKeys = teamKeyValues.getValue(KEY_EXCLUSIONS);
		if (teamExclusionKeys.isBlank()) {
			myTeamExclusionKeys = null;
		} else {
			myTeamExclusionKeys = new TreeSet<>(Arrays.asList(teamExclusionKeys.split(SEMICOLON)));
		}

		myName = keyValues.getValue(NAME);
		myDescription = keyValues.getValue(DESCRIPTION);
		myBuildFolderString = keyValues.getValue(KEY_BUILDFOLDER);
		myCustomBuildCommand = keyValues.getValue(KEY_CUSTOM_BUILD_COMMAND);
		String builderID = keyValues.getValue(KEY_BUILDER_ID);
		myAutoMakeTarget = keyValues.getValue(KEY_AUTO_MAKE_TARGET);
		myIncrementalMakeTarget = keyValues.getValue(KEY_INCREMENTAL_MAKE_TARGET);
		myCleanMakeTarget = keyValues.getValue(KEY_CLEAN_MAKE_TARGET);
		myPreBuildStep = keyValues.getValue(KEY_PRE_BUILD_STEP);
		myPreBuildAnnouncement = keyValues.getValue(KEY_PRE_BUILD_ANNOUNCEMENT);
		myPostBuildStep = keyValues.getValue(KEY_POST_BUILD_STEP);
		myPostBuildStepAnouncement = keyValues.getValue(KEY_POST_BUILD_ANNOUNCEMENT);
		String autoCfgExtentionDesc = keyValues.getValue(KEY_AUTOBUILD_EXTENSION_CLASS);
		myName = keyValues.getValue(NAME);
		String autoCfgExtentionBundel = keyValues.getValue(KEY_AUTOBUILD_EXTENSION_BUNDEL);
		myUseDefaultBuildCommand = Boolean.parseBoolean(keyValues.getValue(KEY_USE_DEFAULT_BUILD_COMMAND));
		myGenerateMakeFilesAUtomatically = Boolean
				.parseBoolean(keyValues.getValue(KEY_GENERATE_MAKE_FILES_AUTOMATICALLY));
		myUseStandardBuildArguments = Boolean.parseBoolean(keyValues.getValue(KEY_USE_STANDARD_BUILD_ARGUMENTS));
		myStopOnFirstBuildError = Boolean.parseBoolean(keyValues.getValue(KEY_STOP_ON_FIRST_ERROR));
		myIsParallelBuild = Boolean.parseBoolean(keyValues.getValue(KEY_IS_PARRALLEL_BUILD));
		myIsCleanBuildEnabled = Boolean.parseBoolean(keyValues.getValue(KEY_IS_CLEAN_BUILD_ENABLED));
		myIsIncrementalBuildEnabled = Boolean.parseBoolean(keyValues.getValue(KEY_IS_INCREMENTAL_BUILD_ENABLED));
		myParallelizationNum = Integer.parseInt(keyValues.getValue(KEY_NUM_PARRALEL_BUILDS));

		String providerID = buildToolsKeyValues.getValue(KEY_PROVIDER_ID);
		String selectionID = buildToolsKeyValues.getValue(KEY_SELECTION_ID);

		extensionPointID = projectTypeKeyValues.getValue(KEY_EXTENSION_POINT_ID);
		extensionID = projectTypeKeyValues.getValue(KEY_EXTENSION_ID);
		projectTypeID = projectTypeKeyValues.getValue(ID);
		confName = configKeyValues.getValue(NAME);

		myProjectType = AutoBuildManager.getProjectType(extensionPointID, extensionID, projectTypeID, true);
		myBuildTools = IBuildToolsManager.getDefault().getBuildTools(providerID, selectionID);
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
		myIsAutoBuildEnabled = myBuilder.getBuildRunner().supportsAutoBuild();
		myTargetPlatformData = new BuildTargetPlatformData();
		myBuildBuildData = new BuildBuildData(this);
		myRequiredErrorParserList = myAutoBuildConfiguration.getErrorParserList();

		// Now we have reconstructed the environment and read out the persisted content
		// it is time to
		// reconstruct the more complicated fields

		IProject project = getProject();

		KeyValueTree propertiesKeyValues = keyValues.getChild(KEY_PROPERTY);
		for (KeyValueTree curProp : propertiesKeyValues.getChildren().values()) {
			myProperties.put(curProp.getKey(), curProp.getValue());
		}

		myOptions = new AutoBuildOptions(this, keyValues);
		myOptions.updateDefault(myAutoBuildConfiguration, this);

		KeyValueTree toolCommandKeyValue = keyValues.getChild(KEY_CUSTOM_TOOL_COMMAND);
		for (KeyValueTree curToolCommand : toolCommandKeyValue.getChildren().values()) {
			String toolID = curToolCommand.getValue(KEY);
			ITool tool = myProjectType.getToolChain().getTool(toolID);
			Map<IResource, String> resourceOptions = new HashMap<>();
			for (KeyValueTree curOption : curToolCommand.getChildren().values()) {
				IResource resource = curOption.getResource(project);
				resourceOptions.put(resource, curOption.getValue(KEY_VALUE));
			}
			myCustomToolCommands.put(tool, resourceOptions);
		}

		KeyValueTree toolPatternKeyValue = keyValues.getChild(KEY_CUSTOM_TOOL_PATTERN);
		for (KeyValueTree curToolCommand : toolPatternKeyValue.getChildren().values()) {
			String toolID = curToolCommand.getValue(KEY);
			ITool tool = myProjectType.getToolChain().getTool(toolID);
			Map<IResource, String> resourceOptions = new HashMap<>();
			for (KeyValueTree curOption : curToolCommand.getChildren().values()) {
				IResource resource = curOption.getResource(project);
				resourceOptions.put(resource, curOption.getValue(KEY_VALUE));
			}
			myCustomToolPattern.put(tool, resourceOptions);
		}

		// load the auto Build configuration extension description
		if (autoCfgExtentionDesc != null && autoCfgExtentionBundel != null && (!autoCfgExtentionDesc.isBlank())
				&& (!autoCfgExtentionBundel.isBlank())) {
			try {
				Bundle contributingBundle = getBundle(autoCfgExtentionBundel);
				Class<?> autoCfgExtentionDescClass = contributingBundle.loadClass(autoCfgExtentionDesc);
				Constructor<?> ctor = autoCfgExtentionDescClass
						.getDeclaredConstructor(IAutoBuildConfigurationDescription.class, KeyValueTree.class);
				ctor.setAccessible(true);

				myAutoBuildCfgExtDes = (AutoBuildConfigurationExtensionDescription) ctor.newInstance(this,
						keyValues.getChild(KEY_EXTENSION));
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
		return Platform.getBundle(symbolicName);
	}

	public void setCdtConfigurationDescription(ICConfigurationDescription cfgDescription) {
		checkIfWeCanWrite();
		myCdtConfigurationDescription = cfgDescription;
		myBuildBuildData = new BuildBuildData(this);
		myOptions.updateDefault(myAutoBuildConfiguration, this);

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

	@Override
	public TreeMap<IOption, String> getSelectedOptions(Set<? extends IResource> file, ITool tool) {
		return myOptions.getSelectedOptions(file, tool);
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
		return myOptions.getSelectedOptions(file);
	}

	@Override
	public TreeMap<IOption, String> getSelectedOptions(IResource file, ITool tool) {
		return myOptions.getSelectedOptions(file, tool);
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
			String args = myBuilder.getArguments(myParallelizationNum, myStopOnFirstBuildError);
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
		super.serialize(keyValuePairs.addChild(KEY_SOURCE_ENTRY));

		KeyValueTree modelKeyValue = keyValuePairs.addChild(KEY_MODEL);
		KeyValueTree projectTypeKeyValue = modelKeyValue.addChild(KEY_PROJECT_TYPE);

		KeyValueTree teamKeyValues = keyValuePairs.addChild(KEY_TEAM);
		teamKeyValues.addValue(KEY_IS_SHARED, String.valueOf(myIsTeamShared));
		if (myTeamExclusionKeys != null) {
			teamKeyValues.addValue(KEY_EXCLUSIONS, String.join(SEMICOLON, myTeamExclusionKeys));
		}

		projectTypeKeyValue.addValue(KEY_EXTENSION_POINT_ID, getExtensionPointID());
		projectTypeKeyValue.addValue(KEY_EXTENSION_ID, getExtensionID());
		projectTypeKeyValue.addValue(ID, myProjectType.getId());

		KeyValueTree configurationKeyValue = modelKeyValue.addChild(KEY_CONFIGURATION);
		configurationKeyValue.addValue(NAME, myAutoBuildConfiguration.getName());

		keyValuePairs.addValue(NAME, myName);
		keyValuePairs.addValue(DESCRIPTION, myDescription);

		KeyValueTree buildToolsKeyValue = keyValuePairs.addChild(KEY_BUILDTOOLS);
		buildToolsKeyValue.addValue(KEY_PROVIDER_ID, getBuildTools().getProviderID());
		buildToolsKeyValue.addValue(KEY_SELECTION_ID, getBuildTools().getSelectionID());

		keyValuePairs.addValue(KEY_BUILDFOLDER, myBuildFolderString);
		keyValuePairs.addValue(KEY_USE_DEFAULT_BUILD_COMMAND, String.valueOf(myUseDefaultBuildCommand));
		keyValuePairs.addValue(KEY_GENERATE_MAKE_FILES_AUTOMATICALLY, String.valueOf(myGenerateMakeFilesAUtomatically));
		keyValuePairs.addValue(KEY_USE_STANDARD_BUILD_ARGUMENTS, String.valueOf(myUseStandardBuildArguments));
		keyValuePairs.addValue(KEY_STOP_ON_FIRST_ERROR, String.valueOf(myStopOnFirstBuildError));
		keyValuePairs.addValue(KEY_IS_PARRALLEL_BUILD, String.valueOf(myIsParallelBuild));
		keyValuePairs.addValue(KEY_IS_CLEAN_BUILD_ENABLED, String.valueOf(myIsCleanBuildEnabled));
		keyValuePairs.addValue(KEY_IS_INCREMENTAL_BUILD_ENABLED, String.valueOf(myIsIncrementalBuildEnabled));
		keyValuePairs.addValue(KEY_NUM_PARRALEL_BUILDS, String.valueOf(myParallelizationNum));
		keyValuePairs.addValue(KEY_CUSTOM_BUILD_COMMAND, myCustomBuildCommand);
		keyValuePairs.addValue(KEY_BUILDER_ID, myBuilder.getId());
		keyValuePairs.addValue(KEY_AUTO_MAKE_TARGET, myAutoMakeTarget);
		keyValuePairs.addValue(KEY_INCREMENTAL_MAKE_TARGET, myIncrementalMakeTarget);
		keyValuePairs.addValue(KEY_CLEAN_MAKE_TARGET, myCleanMakeTarget);

		keyValuePairs.addValue(KEY_PRE_BUILD_STEP, myPreBuildStep);
		keyValuePairs.addValue(KEY_PRE_BUILD_ANNOUNCEMENT, myPreBuildAnnouncement);
		keyValuePairs.addValue(KEY_POST_BUILD_STEP, myPostBuildStep);
		keyValuePairs.addValue(KEY_POST_BUILD_ANNOUNCEMENT, myPostBuildStepAnouncement);

		KeyValueTree propertiesKeyValue = keyValuePairs.addChild(KEY_PROPERTY);
		for (Entry<String, String> curProp : myProperties.entrySet()) {
			propertiesKeyValue.addValue(curProp.getKey(), curProp.getValue());
		}

		myOptions.serialize(keyValuePairs);

		int counter = counterStart;
		KeyValueTree customToolKeyValue = keyValuePairs.addChild(KEY_CUSTOM_TOOL_COMMAND);
		for (Entry<ITool, Map<IResource, String>> curCustomToolCommands : myCustomToolCommands.entrySet()) {
			ITool tool = curCustomToolCommands.getKey();
			customToolKeyValue.addValue(KEY, tool.getId());

			for (Entry<IResource, String> curResourceCommand : curCustomToolCommands.getValue().entrySet()) {
				IResource res = curResourceCommand.getKey();
				KeyValueTree curOptionKeyValue = customToolKeyValue.addChild(String.valueOf(counter));
				String resourceID = res.getProjectRelativePath().toString();
				curOptionKeyValue.addValue(KEY_VALUE, curResourceCommand.getValue());
				curOptionKeyValue.addValue(KEY_RESOURCE, resourceID);
				counter++;
			}
		}

		counter = counterStart;
		KeyValueTree customToolPatternKeyValue = keyValuePairs.addChild(KEY_CUSTOM_TOOL_PATTERN);
		for (Entry<ITool, Map<IResource, String>> curCustomToolCommands : myCustomToolPattern.entrySet()) {
			ITool tool = curCustomToolCommands.getKey();
			customToolPatternKeyValue.addValue(KEY, tool.getId());
			for (Entry<IResource, String> curResourceCommand : curCustomToolCommands.getValue().entrySet()) {
				IResource res = curResourceCommand.getKey();
				String resourceID = res.getProjectRelativePath().toString();
				KeyValueTree curOptionKeyValue = customToolPatternKeyValue.addChild(String.valueOf(counter));
				curOptionKeyValue.addValue(KEY_VALUE, curResourceCommand.getValue());
				curOptionKeyValue.addValue(KEY_RESOURCE, resourceID);
				counter++;
			}
		}

		if (myAutoBuildCfgExtDes != null) {
			Class<? extends AutoBuildConfigurationExtensionDescription> referencedClass = myAutoBuildCfgExtDes
					.getClass();

			keyValuePairs.addValue(KEY_AUTOBUILD_EXTENSION_BUNDEL, myAutoBuildCfgExtDes.getBundelName());
			keyValuePairs.addValue(KEY_AUTOBUILD_EXTENSION_CLASS, referencedClass.getName());
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
	public void setBuilder(String builderID) {
		setBuilder(getBuilder(builderID));
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
	public void setOptionValue(IResource resource, IOption option, String valueID) {
		checkIfWeCanWrite();
		myOptions.setOptionValue(resource, option, valueID);
	}

	@Override
	public String getOptionValue(IResource resource, ITool tool, IOption option) {
		return myOptions.getOptionValue(resource, tool, option);
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
		myOptions.updateDefault(myAutoBuildConfiguration, this);
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
	public Map<String, String> getEnvironmentVariableMap() {
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
		return envMap;
	}

	@Override
	public String[] getEnvironmentVariables() {
		Map<String, String> envMap = getEnvironmentVariableMap();
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
		myIsTeamShared = isTeamShared;
	}

	@Override
	public Set<String> getTeamExclusionKeys() {
		if (!myIsTeamShared) {
			Set<String> ret = new TreeSet<>();
			ret.add(getName());
			return ret;
		}
		if (myTeamExclusionKeys != null) {
			return new TreeSet<>(myTeamExclusionKeys);
		}
		return getDefaultTeamExclusionKeys();
	}

	@Override
	public Set<String> getDefaultTeamExclusionKeys() {
		Set<String> ret = new TreeSet<>();
		ret.add(KEY_TEAM + DOT + KEY_IS_SHARED);
		ret.add(KEY_NUM_PARRALEL_BUILDS);
		ret.add(KEY_STOP_ON_FIRST_ERROR);
		if (myAutoBuildCfgExtDes != null) {
			ret.addAll(myAutoBuildCfgExtDes.getTeamDefaultExclusionKeys(KEY_EXTENSION));
		}
		return ret;
	}

	@Override
	public Set<String> getCustomTeamExclusionKeys() {
		if (myTeamExclusionKeys == null) {
			return null;
		}
		return new TreeSet<>(myTeamExclusionKeys);
	}

	@Override
	public void setCustomTeamExclusionKeys(Set<String> newExclusionKeys) {
		if (newExclusionKeys == null) {
			myTeamExclusionKeys = null;
			return;
		}
		if (newExclusionKeys.equals(myTeamExclusionKeys)) {
			myTeamExclusionKeys = null;
			return;
		}
		myTeamExclusionKeys = new TreeSet<>(newExclusionKeys);
	}

	@Override
	public boolean equals(IAutoBuildConfigurationDescription other) {
		AutoBuildConfigurationDescription localOther = (AutoBuildConfigurationDescription) other;
		if (myOptions.equals(localOther.myOptions)
				&& myBuildTools.equals(localOther.myBuildTools)
				&& myAutoBuildConfiguration.equals(localOther.myAutoBuildConfiguration)
				&& myProjectType.equals(localOther.myProjectType)
				&& myBuilder.equals(localOther.myBuilder)
				&& myBuildBuildData.equals(localOther.myBuildBuildData)
				&& myName.equals(localOther.myName)
				&& myDescription.equals(localOther.myDescription)
				// && myIsValid == localOther.myIsValid
				&& myIsTeamShared == localOther.myIsTeamShared
				&& myGenerateMakeFilesAUtomatically == localOther.myGenerateMakeFilesAUtomatically
				&& myStopOnFirstBuildError == localOther.myStopOnFirstBuildError
				&& myIsParallelBuild == localOther.myIsParallelBuild
				&& myIsCleanBuildEnabled == localOther.myIsCleanBuildEnabled
				&& myIsIncrementalBuildEnabled == localOther.myIsIncrementalBuildEnabled
				&& myUseDefaultBuildCommand == localOther.myUseDefaultBuildCommand
				&& myUseStandardBuildArguments == localOther.myUseStandardBuildArguments
				&& myIsAutoBuildEnabled == localOther.myIsAutoBuildEnabled
				&& myForceCleanBeforeBuild == localOther.myForceCleanBeforeBuild
				&& myParallelizationNum == localOther.myParallelizationNum
				&& myCustomBuildArguments.equals(localOther.myCustomBuildArguments)
				&& myCustomBuildCommand.equals(localOther.myCustomBuildCommand)
				&& myBuildFolderString.equals(localOther.myBuildFolderString)
				&& myAutoMakeTarget.equals(localOther.myAutoMakeTarget)
				&& myIncrementalMakeTarget.equals(localOther.myIncrementalMakeTarget)
				&& myCleanMakeTarget.equals(localOther.myCleanMakeTarget)
				&& myPreBuildStep.equals(localOther.myPreBuildStep)
				&& myPreBuildAnnouncement.equals(localOther.myPreBuildAnnouncement)
				&& myPostBuildStep.equals(localOther.myPostBuildStep)
				&& myPostBuildStepAnouncement.equals(localOther.myPostBuildStepAnouncement)
				&& myCustomToolCommands.equals(localOther.myCustomToolCommands)
				&& myCustomToolPattern.equals(localOther.myCustomToolPattern)
				&& myProperties.equals(localOther.myProperties)) {
			boolean ret = true;
			if (myAutoBuildCfgExtDes != null) {
				ret = ret && myAutoBuildCfgExtDes.equals(localOther.myAutoBuildCfgExtDes);
			}
			if (myTeamExclusionKeys != null) {
				ret = ret && myTeamExclusionKeys.equals(localOther.myTeamExclusionKeys);
			}
			return ret;
		}
		return false;
	}

	@Override
	public TreeMap<String, String> getPrebuildSteps() {
		TreeMap<String, String> ret=new TreeMap<>();

		String preBuildStep = resolve(getPrebuildStep(), EMPTY_STRING, WHITESPACE, this);
		if (!preBuildStep.isEmpty()) {
			String announcement = getPreBuildAnouncement();
			ret.put(announcement,preBuildStep);
		}
		if(myAutoBuildCfgExtDes!=null) {
			ret.putAll(myAutoBuildCfgExtDes.getPrebuildSteps());
		}
		return ret;
	}

	@Override
	public TreeMap<String, String> getPostbuildSteps() {
		TreeMap<String, String> ret=new TreeMap<>();

		if(myAutoBuildCfgExtDes!=null) {
			ret.putAll(myAutoBuildCfgExtDes.getPostbuildSteps());
		}

		String postBuildStep = resolve(getPostbuildStep(), EMPTY_STRING, WHITESPACE, this);
		if (!postBuildStep.isEmpty()) {
			String announcement = getPostBuildAnouncement();
			ret.put(announcement,postBuildStep);
		}
		return ret;
	}
}
