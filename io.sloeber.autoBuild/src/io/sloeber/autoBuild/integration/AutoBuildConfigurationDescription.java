package io.sloeber.autoBuild.integration;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

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
import org.eclipse.cdt.core.cdtvariables.ICdtVariablesContributor;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.extension.CBuildData;
import org.eclipse.cdt.core.settings.model.extension.CTargetPlatformData;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;
import io.sloeber.autoBuild.api.AutoBuildConfigurationExtensionDescription;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.api.IBuildRunner;
import io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon;
import io.sloeber.buildTool.api.IBuildTools;
import io.sloeber.buildTool.api.IBuildToolManager;
import io.sloeber.buildTool.api.IBuildToolManager.ToolFlavour;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.IProjectType;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.api.IToolChain;
import io.sloeber.schema.internal.Configuration;
import io.sloeber.schema.internal.Tool;

public class AutoBuildConfigurationDescription extends AutoBuildResourceData
		implements IAutoBuildConfigurationDescription {
	private static final String KEY_MODEL = "Model"; //$NON-NLS-1$
	private static final String KEY_CONFIGURATION = "configuration"; //$NON-NLS-1$
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
	private static final String KEY_BUILDER_ID = "builderID";//$NON-NLS-1$
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

	// Start of fields that need to be copied/made persistent
	private IConfiguration myAutoBuildConfiguration;
	private IProjectType myProjectType;

	private ICConfigurationDescription myCdtConfigurationDescription;
	private BuildTargetPlatformData myTargetPlatformData;
	private IBuildTools myBuildTools;
	private BuildBuildData myBuildBuildData;
	private boolean isValid = false;
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
	private String myBuildFolderString = BIN_FOLDER+SLACH+CONFIG_NAME_VARIABLE;

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

	public AutoBuildConfigurationDescription(Configuration config, IProject project, IBuildTools buildTools,String rootCodeFolder) {
		initializeResourceData(rootCodeFolder,myBuildFolderString);
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
		if(myBuilder==null) {
			System.err.println("project "+project.getName()+" has no default builder"); //$NON-NLS-1$ //$NON-NLS-2$
			myBuilder=AutoBuildManager.getDefaultBuilder();
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
		myCdtConfigurationDescription = cfgDescription;
		myBuilder = base.myBuilder;
		myBuilders = base.myBuilders;
		myTargetPlatformData = new BuildTargetPlatformData(base.myTargetPlatformData, clone);
		myBuildBuildData = new BuildBuildData(this, base.myBuildBuildData, clone);
		isValid = base.isValid;
		myName = myCdtConfigurationDescription.getName();
		myDescription = base.myDescription;
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
				// TOFIX JABA is this ever going to work?
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

	@Override
	public IBuildTools getBuildTools() {
		if (myBuildTools == null) {
			// TODO add real error warning
			System.err.println("AutoBuildConfigurationDescription.myBuildTools should never be null" ); //$NON-NLS-1$
			myBuildTools=IBuildToolManager.getDefault().getAnyInstalledBuildTools();
		}
		return myBuildTools;
	}

	@Override
	public void setBuildTools(IBuildTools buildTools) {
		myBuildTools = buildTools;
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
	public AutoBuildConfigurationDescription(ICConfigurationDescription cfgDescription, String curConfigsText,
			String lineStart, String lineEnd) {
		super(cfgDescription, curConfigsText, lineStart, lineEnd);
		myId = cfgDescription.getId();
		myIsWritable = !cfgDescription.isReadOnly();
		String extensionPointID = null;
		String extensionID = null;
		String projectTypeID = null;
		String confName = null;
		String builderID=null;
		String autoCfgExtentionDesc = null;
		String autoCfgExtentionBundel = null;
		myCdtConfigurationDescription = cfgDescription;
		String[] lines = curConfigsText.split(lineEnd);
		Map<String, String> optionKeyMap = new HashMap<>();
		Map<String, String> optionValueMap = new HashMap<>();
		Map<String, String> optionResourceMap = new HashMap<>();
		Map<String, String> customToolKeyMap = new HashMap<>();
		Map<String, String> customToolResourceMap = new HashMap<>();
		Map<String, String> customToolValueMap = new HashMap<>();
		Map<String, String> customToolPatternKeyMap = new HashMap<>();
		Map<String, String> customToolPatternResourceMap = new HashMap<>();
		Map<String, String> customToolPatternValueMap = new HashMap<>();

		// structure to handle the more complex fields
		Map<String, Map<String, String>> complexStructures = new HashMap<>();
		complexStructures.put(KEY_PROPERTY + DOT, myProperties);
		complexStructures.put(OPTION + DOT + KEY + DOT, optionKeyMap);
		complexStructures.put(OPTION + DOT + KEY_VALUE + DOT, optionValueMap);
		complexStructures.put(OPTION + DOT + KEY_RESOURCE + DOT, optionResourceMap);
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
			String field[] = curLine.split(EQUAL, 2);
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
			case KEY_BUILDER_ID:
				builderID=value;
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

				if (key.startsWith(KEY_BUILDTOOLS + DOT)) {
					found = true;
					String providerID = key.substring(KEY_BUILDTOOLS.length() + DOT.length());
					String selectionID = value;
					IBuildToolManager buildToolManager =IBuildToolManager.getDefault();
					myBuildTools = buildToolManager.getBuildTools(providerID, selectionID);
					if (myBuildTools == null) {
						// TODO add real error warning
						System.err.println("unable to identify build Tools from :" + curLine); //$NON-NLS-1$
						myBuildTools=buildToolManager.getAnyInstalledBuildTools();
					}
				}

				// gather the complex field data
				if (!found) {
					for (Entry<String, Map<String, String>> curComplex : complexStructures.entrySet()) {
						String searchKey = curComplex.getKey();
						if (key.startsWith(searchKey)) {
							String propKey = key.substring(searchKey.length());
							curComplex.getValue().put(propKey, value);
							found = true;
							break;
						}
					}
				}

				if (!found && !key.startsWith(KEY_EXTENSION) && !key.startsWith(KEY_SOURCE_ENTRY)) {
					System.err.println("Following autobuild configuration line is ignored " + curLine); //$NON-NLS-1$
				}
			}
		}
		myProjectType = AutoBuildManager.getProjectType(extensionPointID, extensionID, projectTypeID, true);
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

		IProject project =getProject(); 
		// reconstruct the selected options
		for (Entry<String, String> curOptionIndex : optionKeyMap.entrySet()) {
			String key = curOptionIndex.getKey();
			String optionID = curOptionIndex.getValue();
			String value = optionValueMap.get(key);
			String resourceString = optionResourceMap.get(key);
			if (value == null || resourceString == null) {
				// This Should not happen
				continue;
			}
			IResource resource = project;
			if (!resourceString.isBlank()) {
				resource = project.getFile(resourceString);
			}
			IOption option =myProjectType.getOption(optionID);

			if (resource==null || option == null) {
				// TODO log a error in error log
				System.err.println("failed to map option(" + optionID + ")/value(" + value + ")/resource("+resource+")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			}else {
			setOptionValueInternal(resource,  option, value);
			}

		}
		options_combine();

		// reconstruct the custom tool commands
		for (Entry<String, String> curOptionIndex : customToolKeyMap.entrySet()) {
			String cmd = customToolValueMap.get(curOptionIndex.getKey());
			String resourceString = customToolResourceMap.get(curOptionIndex.getKey());
			ITool tool = myAutoBuildConfiguration.getProjectType().getToolChain().getTool(curOptionIndex.getValue());
			if (cmd == null || resourceString == null || tool == null) {
				// This Should not happen
			} else {
				IResource resource = project;
				if (!resourceString.isBlank()) {
					resource = project.getFile(resourceString);
				}

				Map<IResource, String> resourceCmds = myCustomToolCommands.get(tool);
				if (resourceCmds == null) {
					resourceCmds = new HashMap<>();
					myCustomToolCommands.put(tool, resourceCmds);
				}
				resourceCmds.put(resource, cmd);
			}
		}

		// reconstruct the custom tool patterns
		for (Entry<String, String> curOptionIndex : customToolPatternKeyMap.entrySet()) {
			String cmd = customToolPatternValueMap.get(curOptionIndex.getKey());
			String resourceString = customToolPatternResourceMap.get(curOptionIndex.getKey());
			ITool tool = myAutoBuildConfiguration.getProjectType().getToolChain().getTool(curOptionIndex.getValue());
			if (cmd == null || resourceString == null || tool == null) {
				// This Should not happen
			} else {
				IResource resource = project;
				if (!resourceString.isBlank()) {
					resource = project.getFile(resourceString);
				}

				Map<IResource, String> resourceCmds = myCustomToolPattern.get(tool);
				if (resourceCmds == null) {
					resourceCmds = new HashMap<>();
					myCustomToolPattern.put(tool, resourceCmds);
				}
				resourceCmds.put(resource, cmd);
			}
		}

		// load the auto Build configuration extension description
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
		return Platform.getBundle(symbolicName);
		//return org.eclipse.core.internal.registry.osgi.OSGIUtils.getDefault().getBundle(symbolicName);
		// TOFIX Below is an alternative but as I could not test at the time ...
		// BundleContext bundleContext = Activator.getBundleContext();
		// if (bundleContext == null) {
		// System.err.println("Failed to get the bundleContext");
		// System.err.println("Could not load the plugin this project is created
		// with.");
		// return null;
		// }
		// Bundle result = null;
		// for (Bundle candidate : bundleContext.getBundles()) {
		// if (candidate.getSymbolicName().equals(symbolicName)) {
		// if (result == null || result.getVersion().compareTo(candidate.getVersion()) <
		// 0) {
		// result = candidate;
		// }
		// }
		// }
		// return result;
	}

	/**
	 * get the default options and update the combined options
	 */
	private void options_updateDefault() {
		myDefaultOptions.clear();
		IProject project=getProject();
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

		isValid = true;
	}

	@Override
	public ICConfigurationDescription getCdtConfigurationDescription() {
		return myCdtConfigurationDescription;
	}

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

	// @Override
	// public Map<IOption, String> getSelectedOptions(IResource file, ITool tool) {
	// return convertOptionIDToOption(getSelectedOptionNames(file, tool), tool);
	// }

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
			String args = myBuilder.getArguments(myIsParallelBuild, myParallelizationNum, myStopOnFirstBuildError);
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

	@Override
	public StringBuffer serialize(String linePrefix, String lineEnd) {
		int counterStart = 0;
		StringBuffer ret = super.serialize(linePrefix, lineEnd);
		IProjectType projectType = myAutoBuildConfiguration.getProjectType();
		ret.append(linePrefix + KEY_MODEL + DOT + KEY_PROJECT_TYPE + DOT + KEY_EXTENSION_POINT_ID + EQUAL
				+ projectType.getExtensionPointID() + lineEnd);
		ret.append(linePrefix + KEY_MODEL + DOT + KEY_PROJECT_TYPE + DOT + KEY_EXTENSION_ID + EQUAL
				+ projectType.getExtensionID() + lineEnd);
		ret.append(linePrefix + KEY_MODEL + DOT + KEY_PROJECT_TYPE + DOT + ID + EQUAL + projectType.getId() + lineEnd);
		ret.append(linePrefix + KEY_MODEL + DOT + KEY_CONFIGURATION + DOT + NAME + EQUAL
				+ myAutoBuildConfiguration.getName() + lineEnd);

		ret.append(linePrefix + NAME + EQUAL + myName + lineEnd);
		ret.append(linePrefix + DESCRIPTION + EQUAL);
		ret.append(myDescription);
		ret.append(lineEnd);

		ret.append(linePrefix + KEY_BUILDTOOLS + DOT + getBuildTools().getProviderID() + EQUAL + getBuildTools().getSelectionID()
				+ lineEnd);

		// ret.append(linePrefix + ID + EQUAL + myId + lineEnd);

		for (Entry<String, String> curProp : myProperties.entrySet()) {
			ret.append(linePrefix + KEY_PROPERTY + DOT + curProp.getKey() + EQUAL + curProp.getValue() + lineEnd);
		}
		int counter = counterStart;
		for (Entry<IResource, Map<IOption, String>> curOption : mySelectedOptions.entrySet()) {
			IResource resource = curOption.getKey();
			String resourceID = resource.getProjectRelativePath().toString();
			for (Entry<IOption, String> resourceOptions : curOption.getValue().entrySet()) {
				ret.append(linePrefix + OPTION + DOT + KEY + DOT + String.valueOf(counter) + EQUAL
						+ resourceOptions.getKey() + lineEnd);
				ret.append(linePrefix + OPTION + DOT + KEY_VALUE + DOT + String.valueOf(counter) + EQUAL
						+ resourceOptions.getValue() + lineEnd);
				ret.append(linePrefix + OPTION + DOT + KEY_RESOURCE + DOT + String.valueOf(counter) + EQUAL + resourceID
						+ lineEnd);
				counter++;
			}
		}

		ret.append(linePrefix + KEY_BUILDFOLDER + EQUAL + myBuildFolderString + lineEnd);
		ret.append(linePrefix + KEY_USE_DEFAULT_BUILD_COMMAND + EQUAL + String.valueOf(myUseDefaultBuildCommand)
				+ lineEnd);
		ret.append(linePrefix + KEY_GENERATE_MAKE_FILES_AUTOMATICALLY + EQUAL
				+ String.valueOf(myGenerateMakeFilesAUtomatically) + lineEnd);
		ret.append(linePrefix + KEY_USE_STANDARD_BUILD_ARGUMENTS + EQUAL + String.valueOf(myUseStandardBuildArguments)
				+ lineEnd);
		ret.append(linePrefix + KEY_STOP_ON_FIRST_ERROR + EQUAL + String.valueOf(myStopOnFirstBuildError) + lineEnd);
		ret.append(linePrefix + KEY_IS_PARRALLEL_BUILD + EQUAL + String.valueOf(myIsParallelBuild) + lineEnd);
		ret.append(linePrefix + KEY_IS_CLEAN_BUILD_ENABLED + EQUAL + String.valueOf(myIsCleanBuildEnabled) + lineEnd);
		ret.append(linePrefix + KEY_IS_INCREMENTAL_BUILD_ENABLED + EQUAL + String.valueOf(myIsIncrementalBuildEnabled)
				+ lineEnd);
		ret.append(linePrefix + KEY_NUM_PARRALEL_BUILDS + EQUAL + String.valueOf(myParallelizationNum) + lineEnd);
		ret.append(linePrefix + KEY_CUSTOM_BUILD_COMMAND + EQUAL + myCustomBuildCommand + lineEnd);
		ret.append(linePrefix + KEY_BUILDER_ID + EQUAL + myBuilder.getId() + lineEnd);
		ret.append(linePrefix + KEY_AUTO_MAKE_TARGET + EQUAL + myAutoMakeTarget + lineEnd);
		ret.append(linePrefix + KEY_INCREMENTAL_MAKE_TARGET + EQUAL + myIncrementalMakeTarget + lineEnd);
		ret.append(linePrefix + KEY_CLEAN_MAKE_TARGET + EQUAL + myCleanMakeTarget + lineEnd);

		ret.append(linePrefix + KEY_PRE_BUILD_STEP + EQUAL + myPreBuildStep + lineEnd);
		ret.append(linePrefix + KEY_PRE_BUILD_ANNOUNCEMENT + EQUAL + myPreBuildAnnouncement + lineEnd);
		ret.append(linePrefix + KEY_POST_BUILD_STEP + EQUAL + myPostBuildStep + lineEnd);
		ret.append(linePrefix + KEY_POST_BUILD_ANNOUNCEMENT + EQUAL + myPostBuildStepAnouncement + lineEnd);

		counter = counterStart;
		for (Entry<ITool, Map<IResource, String>> curCustomToolCommands : myCustomToolCommands.entrySet()) {
			ITool tool = curCustomToolCommands.getKey();

			for (Entry<IResource, String> curResourceCommand : curCustomToolCommands.getValue().entrySet()) {
				IResource res = curResourceCommand.getKey();
				String resourceID = res.getProjectRelativePath().toString();
				ret.append(linePrefix + KEY_CUSTOM_TOOL_COMMAND + DOT + KEY + DOT + String.valueOf(counter) + EQUAL
						+ tool.getId() + lineEnd);
				ret.append(linePrefix + KEY_CUSTOM_TOOL_COMMAND + DOT + KEY_VALUE + DOT + String.valueOf(counter)
						+ EQUAL + curResourceCommand.getValue() + lineEnd);
				ret.append(linePrefix + KEY_CUSTOM_TOOL_COMMAND + DOT + KEY_RESOURCE + DOT + String.valueOf(counter)
						+ EQUAL + resourceID + lineEnd);
				counter++;
			}
		}

		counter = counterStart;
		for (Entry<ITool, Map<IResource, String>> curCustomToolCommands : myCustomToolPattern.entrySet()) {
			ITool tool = curCustomToolCommands.getKey();

			for (Entry<IResource, String> curResourceCommand : curCustomToolCommands.getValue().entrySet()) {
				IResource res = curResourceCommand.getKey();
				String resourceID = res.getProjectRelativePath().toString();
				ret.append(linePrefix + KEY_CUSTOM_TOOL_PATTERN + DOT + KEY + DOT + String.valueOf(counter) + EQUAL
						+ tool.getId() + lineEnd);
				ret.append(linePrefix + KEY_CUSTOM_TOOL_PATTERN + DOT + KEY_VALUE + DOT + String.valueOf(counter)
						+ EQUAL + curResourceCommand.getValue() + lineEnd);
				ret.append(linePrefix + KEY_CUSTOM_TOOL_PATTERN + DOT + KEY_RESOURCE + DOT + String.valueOf(counter)
						+ EQUAL + resourceID + lineEnd);
				counter++;
			}
		}

		if (myAutoBuildCfgExtDes != null) {
			Class<? extends AutoBuildConfigurationExtensionDescription> referencedClass = myAutoBuildCfgExtDes
					.getClass();

			ret.append(linePrefix + KEY_AUTOBUILD_EXTENSION_BUNDEL + EQUAL + myAutoBuildCfgExtDes.getBundelName()
					+ lineEnd);
			ret.append(linePrefix + KEY_AUTOBUILD_EXTENSION_CLASS + EQUAL + referencedClass.getName() + lineEnd);
			ret.append(myAutoBuildCfgExtDes.serialize(linePrefix + KEY_EXTENSION + DOT, lineEnd));
		}

		return ret;

	}

	@Override
	public IBuilder getBuilder() {
		return myBuilder;
	}

	@Override
	public Set<IBuilder> getAvailableBuilders() {
		if(myBuilders.size()==0) {
			myBuilders=myProjectType.getBuilders();
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
		setOptionValueInternal(resource,  option, valueID);
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

}
