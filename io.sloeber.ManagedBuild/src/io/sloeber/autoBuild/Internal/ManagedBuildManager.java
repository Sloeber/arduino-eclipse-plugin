/*******************************************************************************
 *  Copyright (c) 2003, 2018 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM - Initial API and implementation
 *     Anna Dushistova (MontaVista) - [366771]Converter fails to convert a CDT makefile project
 *******************************************************************************/
package io.sloeber.autoBuild.Internal;

import static io.sloeber.autoBuild.core.Messages.*;

import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.eclipse.cdt.core.AbstractCExtension;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.IConsoleParser;
import org.eclipse.cdt.core.language.settings.providers.ICBuildOutputParser;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.language.settings.providers.IWorkingDirectoryTracker;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IScannerInfoChangeListener;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import io.sloeber.autoBuild.api.BuildException;
import io.sloeber.autoBuild.api.IBuildMacroProvider;
import io.sloeber.autoBuild.api.IBuildObject;
import io.sloeber.autoBuild.api.IBuilder;
import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.api.IEnvironmentVariableProvider;
import io.sloeber.autoBuild.api.IFileInfo;
import io.sloeber.autoBuild.api.IFolderInfo;
import io.sloeber.autoBuild.api.IHoldsOptions;
import io.sloeber.autoBuild.api.IInputType;
import io.sloeber.autoBuild.api.IManagedBuildInfo;
import io.sloeber.autoBuild.api.IManagedConfigElement;
import io.sloeber.autoBuild.api.IManagedProject;
import io.sloeber.autoBuild.api.IOption;
import io.sloeber.autoBuild.api.IOptionCategory;
import io.sloeber.autoBuild.api.IOutputType;
import io.sloeber.autoBuild.api.IProjectType;
import io.sloeber.autoBuild.api.IResourceConfiguration;
import io.sloeber.autoBuild.api.IResourceInfo;
import io.sloeber.autoBuild.api.ITargetPlatform;
import io.sloeber.autoBuild.api.ITool;
import io.sloeber.autoBuild.api.IToolChain;
import io.sloeber.autoBuild.api.OptionStringValue;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.extensionPoint.IMakefileGenerator;
import io.sloeber.autoBuild.extensionPoint.IManagedCommandLineGenerator;
import io.sloeber.autoBuild.extensionPoint.IManagedOptionValueHandler;
import io.sloeber.autoBuild.integration.CommonBuilder;
import io.sloeber.buildProperties.BuildPropertyManager;
import io.sloeber.buildProperties.IBuildPropertyManager;

/**
 * This is the main entry point for getting at the build information for the
 * managed build system.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ManagedBuildManager extends AbstractCExtension {
	private static final String REVISION_ELEMENT_NAME = "managedBuildRevision"; //$NON-NLS-1$
	private static final String VERSION_ELEMENT_NAME = "fileVersion"; //$NON-NLS-1$

	// Error ID's for OptionValidError()
	public static final int ERROR_CATEGORY = 0;
	public static final int ERROR_FILTER = 1;

	public static final String BUILD_TYPE_PROPERTY_ID = "org.eclipse.cdt.build.core.buildType"; //$NON-NLS-1$
	public static final String BUILD_ARTEFACT_TYPE_PROPERTY_ID = "org.eclipse.cdt.build.core.buildArtefactType"; //$NON-NLS-1$

	public static final String BUILD_TYPE_PROPERTY_DEBUG = "org.eclipse.cdt.build.core.buildType.debug"; //$NON-NLS-1$
	public static final String BUILD_TYPE_PROPERTY_RELEASE = "org.eclipse.cdt.build.core.buildType.release"; //$NON-NLS-1$
	public static final String BUILD_ARTEFACT_TYPE_PROPERTY_EXE = "org.eclipse.cdt.build.core.buildArtefactType.exe"; //$NON-NLS-1$
	public static final String BUILD_ARTEFACT_TYPE_PROPERTY_STATICLIB = "org.eclipse.cdt.build.core.buildArtefactType.staticLib"; //$NON-NLS-1$
	public static final String BUILD_ARTEFACT_TYPE_PROPERTY_SHAREDLIB = "org.eclipse.cdt.build.core.buildArtefactType.sharedLib"; //$NON-NLS-1$

	public static final String CFG_DATA_PROVIDER_ID = Activator.PLUGIN_ID + ".ConfigurationDataProvider"; //$NON-NLS-1$

	private static final String NEWLINE = System.getProperty("line.separator"); //$NON-NLS-1$

	public static final String INTERNAL_BUILDER_ID = "org.eclipse.cdt.build.core.internal.builder"; //$NON-NLS-1$

	// This is the version of the manifest and project files
	private static final Version buildInfoVersion = new Version(4, 0, 0);
	private static final Version version = new Version(4, 0, 0);
	private static boolean theProjectTypesLoaded = false;
	private static boolean theProjectTypesLoading = false;
	// Project types defined in the manifest files
	public static SortedMap<String, IProjectType> theProjectTypeMap;
	private static List<IProjectType> theProjectTypes = new ArrayList<>();
	// Configurations defined in the manifest files
	private static Map<String, IConfiguration> extensionConfigurationMap;
	// Resource configurations defined in the manifest files
	private static Map<String, IResourceConfiguration> extensionResourceConfigurationMap;
	// Tool-chains defined in the manifest files
	private static SortedMap<String, IToolChain> extensionToolChainMap;
	// Tools defined in the manifest files
	private static SortedMap<String, Tool> extensionToolMap;
	// Target Platforms defined in the manifest files
	private static Map<String, ITargetPlatform> extensionTargetPlatformMap;
	// Builders defined in the manifest files
	private static SortedMap<String, IBuilder> extensionBuilderMap;
	// Options defined in the manifest files
	private static Map<String, IOption> extensionOptionMap;
	// Option Categories defined in the manifest files
	private static Map<String, IOptionCategory> extensionOptionCategoryMap;
	// Input types defined in the manifest files
	private static Map<String, IInputType> extensionInputTypeMap;
	// Output types defined in the manifest files
	private static Map<String, IOutputType> extensionOutputTypeMap;
	// Targets defined in the manifest files (CDT V2.0 object model)

	// "Selected configuraton" elements defined in the manifest files.
	// These are configuration elements that map to objects in the internal
	// representation of the manifest files. For example, ListOptionValues
	// and enumeratedOptionValues are NOT included.
	// Note that these "configuration elements" are not related to the
	// managed build system "configurations".
	// From the PDE Guide:
	// A configuration element, with its attributes and children, directly
	// reflects the content and structure of the extension section within the
	// declaring plug-in's manifest (plugin.xml) file.
	// This map has a lifecycle corresponding to the build definitions extension
	// loading.
	private static Map<IBuildObject, IManagedConfigElement> theConfigElementMap;

	// private static List sortedToolChains;
	// private static Map builtTypeToToolChainListMap;
	// Listeners interested in build model changes
	private static Map<IResource, List<IScannerInfoChangeListener>> buildModelListeners;
	// Random number for derived object model elements
	private static Random randomNumber;

	private static HashSet<IToolChain> fSortedToolChains;
	private static HashSet<ITool> fSortedTools;
	private static HashSet<IBuilder> fSortedBuilders;

	private static Map<IProject, IManagedBuildInfo> fInfoMap = new HashMap<>();

	// List of extension point ID's the autoBuild Supports
	// currently only 1
	private static List<String> supportedExtensionPointIDs = new ArrayList<>();
	// The list of the loaded extensions
	private static Map<String, String> loadedExtensions = new HashMap<>();
	static {
		supportedExtensionPointIDs.add("io.sloeber.autoBuild.buildDefinitions");
	}

	/**
	 * @return the next random number as a positive integer.
	 */
	public static int getRandomNumber() {
		if (randomNumber == null) {
			// Set the random number seed
			randomNumber = new Random();
			randomNumber.setSeed(System.currentTimeMillis());
		}
		int i = randomNumber.nextInt();
		if (i < 0) {
			i *= -1;
		}
		return i;
	}

	/**
	 * @return the list of project types that are defined by this project, projects
	 *         referenced by this project, and by the extensions.
	 */
	public static IProjectType[] getDefinedProjectTypes() {
		loadExtensions();

		// Create the array and copy the elements over
		int size = theProjectTypes != null ? theProjectTypes.size() : 0;

		IProjectType[] types = new IProjectType[size];

		int n = 0;
		for (IProjectType type : theProjectTypes) {
			types[n++] = type;
		}

		return types;
	}

	/**
	 * @param id - id of the project type
	 * @return the project type with the passed in ID
	 */
	public static IProjectType getProjectType(String id) {
		loadExtensions();
		return getExtensionProjectTypeMap().get(id);
	}

	public static Version getVersion() {
		return version;
	}

	/**
	 * Safe accessor for the map of IDs to ProjectTypes
	 */
	public static SortedMap<String, IProjectType> getExtensionProjectTypeMap() {
		loadExtensions();
		if (theProjectTypeMap == null) {
			theProjectTypeMap = new TreeMap<>();
		}
		return theProjectTypeMap;
	}

	/**
	 * Safe accessor for the map of IDs to Configurations
	 */
	protected static Map<String, IConfiguration> getExtensionConfigurationMap() {
		if (extensionConfigurationMap == null) {
			extensionConfigurationMap = new HashMap<>();
		}
		return extensionConfigurationMap;
	}

	/**
	 * Safe accessor for the map of IDs to Resource Configurations
	 */
	protected static Map<String, IResourceConfiguration> getExtensionResourceConfigurationMap() {
		if (extensionResourceConfigurationMap == null) {
			extensionResourceConfigurationMap = new HashMap<>();
		}
		return extensionResourceConfigurationMap;
	}

	/**
	 * Safe internal accessor for the map of IDs to ToolChains
	 */
	private static SortedMap<String, IToolChain> getExtensionToolChainMapInternal() {
		loadExtensions();

		if (extensionToolChainMap == null) {
			extensionToolChainMap = new TreeMap<>();
		}
		return extensionToolChainMap;
	}

	/**
	 * Safe accessor for the map of IDs to ToolChains
	 */
	public static SortedMap<String, ? extends IToolChain> getExtensionToolChainMap() {
		return getExtensionToolChainMapInternal();
	}

	public static IToolChain[] getExtensionToolChains() {
		return getExtensionToolChainMapInternal().values().toArray(new ToolChain[extensionToolChainMap.size()]);
	}

	/**
	 * Safe internal accessor for the map of IDs to Tools
	 */
	private static SortedMap<String, Tool> getExtensionToolMapInternal() {
		loadExtensions();
		if (extensionToolMap == null) {
			extensionToolMap = new TreeMap<>();
		}
		return extensionToolMap;
	}

	/**
	 * Safe accessor for the map of IDs to Tools
	 */
	public static SortedMap<String, ? extends ITool> getExtensionToolMap() {
		return getExtensionToolMapInternal();
	}

	public static ITool[] getExtensionTools() {
		return getExtensionToolMapInternal().values().toArray(new Tool[extensionToolMap.size()]);
	}

	/**
	 * Safe accessor for the map of IDs to TargetPlatforms
	 */
	protected static Map<String, ITargetPlatform> getExtensionTargetPlatformMap() {
		if (extensionTargetPlatformMap == null) {
			extensionTargetPlatformMap = new HashMap<>();
		}
		return extensionTargetPlatformMap;
	}

	/**
	 * Safe internal accessor for the map of IDs to Builders
	 */
	private static SortedMap<String, IBuilder> getExtensionBuilderMapInternal() {
		loadExtensions();
		if (extensionBuilderMap == null) {
			extensionBuilderMap = new TreeMap<>();
		}
		return extensionBuilderMap;
	}

	/**
	 * Safe accessor for the map of IDs to Builders
	 */
	public static SortedMap<String, ? extends IBuilder> getExtensionBuilderMap() {
		return getExtensionBuilderMapInternal();
	}

	public static IBuilder[] getExtensionBuilders() {
		return getExtensionBuilderMapInternal().values().toArray(new Builder[extensionBuilderMap.size()]);
	}

	/**
	 * Safe accessor for the map of IDs to Options
	 */
	protected static Map<String, IOption> getExtensionOptionMap() {
		if (extensionOptionMap == null) {
			extensionOptionMap = new HashMap<>();
		}
		return extensionOptionMap;
	}

	/**
	 * Safe accessor for the map of IDs to Option Categories
	 */
	protected static Map<String, IOptionCategory> getExtensionOptionCategoryMap() {
		if (extensionOptionCategoryMap == null) {
			extensionOptionCategoryMap = new HashMap<>();
		}
		return extensionOptionCategoryMap;
	}

	/**
	 * Safe accessor for the map of IDs to InputTypes
	 */
	protected static Map<String, IInputType> getExtensionInputTypeMap() {
		if (extensionInputTypeMap == null) {
			extensionInputTypeMap = new HashMap<>();
		}
		return extensionInputTypeMap;
	}

	/**
	 * Safe accessor for the map of IDs to OutputTypes
	 */
	protected static Map<String, IOutputType> getExtensionOutputTypeMap() {
		if (extensionOutputTypeMap == null) {
			extensionOutputTypeMap = new HashMap<>();
		}
		return extensionOutputTypeMap;
	}

	/**
	 * @return the project type from the manifest with the ID specified in the
	 *         argument or {@code null}.
	 */
	public static IProjectType getExtensionProjectType(String id) {
		loadExtensions();

		return getExtensionProjectTypeMap().get(id);
	}

	/**
	 * @return the base extension configuration from the manifest (plugin.xml) or
	 *         {@code null} if not found.
	 *
	 * @since 8.0
	 */
	public static IConfiguration getExtensionConfiguration(IConfiguration cfg) {
		for (; cfg != null && !cfg.isExtensionElement(); cfg = cfg.getParent()) {
			// empty loop to find base configuration
		}
		return cfg;
	}

	/**
	 * @return the configuration from the manifest with the ID specified in the
	 *         argument or {@code null}.
	 */
	public static IConfiguration getExtensionConfiguration(String id) {
		loadExtensions();
		return getExtensionConfigurationMap().get(id);
	}

	/**
	 * @return the tool-chain from the manifest with the ID specified in the
	 *         argument or {@code null}.
	 */
	public static IToolChain getExtensionToolChain(String id) {
		return getExtensionToolChainMapInternal().get(id);
	}

	/**
	 * @return the tool from the manifest with the ID specified in the argument or
	 *         {@code null}.
	 */
	public static ITool getExtensionTool(String id) {
		return getExtensionToolMapInternal().get(id);
	}

	/**
	 * @return the target platform from the manifest with the ID specified in the
	 *         argument or {@code null}.
	 */
	public static ITargetPlatform getExtensionTargetPlatform(String id) {
		loadExtensions();
		return getExtensionTargetPlatformMap().get(id);
	}

	/**
	 * @return the builder from the manifest with the ID specified in the argument
	 *         or {@code null}.
	 */
	public static IBuilder getExtensionBuilder(String id) {
		return getExtensionBuilderMapInternal().get(id);
	}

	public static IBuilder getExtensionBuilder(IBuilder builder) {
		for (; builder != null && !builder.isExtensionElement(); builder = builder.getSuperClass()) {
			// empty loop to find parent builder
		}
		return builder;
	}

	/**
	 * @return the option from the manifest with the ID specified in the argument or
	 *         {@code null}.
	 */
	public static IOption getExtensionOption(String id) {
		loadExtensions();
		return getExtensionOptionMap().get(id);
	}

	/**
	 * @return the InputType from the manifest with the ID specified in the argument
	 *         or {@code null}.
	 */
	public static IInputType getExtensionInputType(String id) {
		loadExtensions();
		return getExtensionInputTypeMap().get(id);
	}

	/**
	 * @return the OutputType from the manifest with the ID specified in the
	 *         argument or {@code null}.
	 */
	public static IOutputType getExtensionOutputType(String id) {
		loadExtensions();
		return getExtensionOutputTypeMap().get(id);
	}

	/**
	 * @param resource to find the target
	 * @param id       - ID of the target
	 *
	 * @return the result of a best-effort search to find a target with the
	 *         specified ID, or {@code null} if one is not found.
	 */
	// public static ITarget getTarget(IResource resource, String id) {
	// ITarget target = null;
	// // Check if the target is spec'd in the build info for the resource
	// if (resource != null) {
	// IManagedBuildInfo buildInfo = getBuildInfo(resource);
	// if (buildInfo != null)
	// target = buildInfo.getTarget(id);
	// }
	// // OK, check the extension map
	// if (target == null) {
	// target = getExtensionTargetMap().get(id);
	// }
	// return target;
	// }

	/**
	 * Sets the default configuration for the project. Note that this will also
	 * update the default target if needed.
	 */
	public static void setDefaultConfiguration(IProject project, IConfiguration newDefault) {
		if (project == null || newDefault == null) {
			return;
		}
		// Set the default in build information for the project
		IManagedBuildInfo info = getBuildInfo(project);
		if (info != null) {
			info.setDefaultConfiguration(newDefault);
		}
	}

	/**
	 * Sets the currently selected configuration. This is used while the project
	 * property pages are displayed
	 */
	public static void setSelectedConfiguration(IProject project, IConfiguration config) {
		if (project == null) {
			return;
		}
		// Set the default in build information for the project
		IManagedBuildInfo info = getBuildInfo(project);
		if (info != null) {
			info.setSelectedConfiguration(config);
		}
	}

	public static IMakefileGenerator getBuildfileGenerator(IConfiguration config) {
		IToolChain toolChain = config.getToolChain();
		if (toolChain != null) {
			IBuilder builder = toolChain.getBuilder();
			if (builder != null)
				return builder.getBuildFileGenerator();
		}
		// If no generator is defined, return the default GNU generator
		// return new GnuMakefileGenerator();
		return null;
	}

	/**
	 * load tool provider defined or default (if not found) command line generator
	 * special for selected tool
	 * 
	 * @param toolId - id selected tool ID
	 */
	public static IManagedCommandLineGenerator getCommandLineGenerator(IConfiguration config, String toolId) {
		// ITool tool = config.getTool(toolId);
		// if (tool != null) {
		// return tool.getCommandLineGenerator();
		// }
		// return new ManagedCommandLineGenerator();
		return null;
	}

	/**
	 * Targets may have a scanner config discovery profile defined that knows how to
	 * discover built-in compiler defines and includes search paths. Find the
	 * profile for the target specified.
	 *
	 * @return scanner configuration discovery profile id
	 */
	public static String getScannerInfoProfileId(IConfiguration config) {
		IToolChain toolChain = config.getToolChain();
		return toolChain.getScannerConfigDiscoveryProfileId();
	}

	/**
	 * Gets the currently selected target. This is used while the project property
	 * pages are displayed.
	 *
	 * @return target configuration.
	 */
	public static IConfiguration getSelectedConfiguration(IProject project) {
		if (project == null) {
			return null;
		}
		// Set the default in build information for the project
		IManagedBuildInfo info = getBuildInfo(project);
		if (info != null) {
			return info.getSelectedConfiguration();
		}
		return null;
	}

	private static void notifyListeners(IResourceInfo resConfig, IOption option) {
		// Continue if change is something that effect the scanreser
		try {
			if (resConfig.getParent().isTemporary() || (option != null && option.getValueType() != IOption.INCLUDE_PATH
					&& option.getValueType() != IOption.PREPROCESSOR_SYMBOLS
					&& option.getValueType() != IOption.INCLUDE_FILES && option.getValueType() != IOption.LIBRARY_PATHS
					&& option.getValueType() != IOption.LIBRARY_FILES && option.getValueType() != IOption.MACRO_FILES
					&& option.getValueType() != IOption.UNDEF_INCLUDE_PATH
					&& option.getValueType() != IOption.UNDEF_PREPROCESSOR_SYMBOLS
					&& option.getValueType() != IOption.UNDEF_INCLUDE_FILES
					&& option.getValueType() != IOption.UNDEF_LIBRARY_PATHS
					&& option.getValueType() != IOption.UNDEF_LIBRARY_FILES
					&& option.getValueType() != IOption.UNDEF_MACRO_FILES && !option.isForScannerDiscovery())) {
				return;
			}
		} catch (BuildException e) {
			return;
		}

		// Figure out if there is a listener for this change
		IResource resource = resConfig.getParent().getOwner();
		List<IScannerInfoChangeListener> listeners = getBuildModelListeners().get(resource);
		if (listeners == null) {
			return;
		}
		ListIterator<IScannerInfoChangeListener> iter = listeners.listIterator();
		while (iter.hasNext()) {
			iter.next().changeNotification(resource, (IScannerInfo) getBuildInfo(resource));
		}
	}

	/**
	 * Set the boolean value for an option for a given config.
	 *
	 * @param config The configuration the option belongs to.
	 * @param holder The holder/parent of the option.
	 * @param option The option to set the value for.
	 * @param value  The boolean that the option should contain after the change.
	 *
	 * @return The modified option. This can be the same option or a newly created
	 *         option.
	 *
	 * @since 3.0 - The type and name of the <code>ITool tool</code> parameter has
	 *        changed to <code>IHoldsOptions holder</code>. Client code assuming
	 *        <code>ITool</code> as type, will continue to work unchanged.
	 */
	public static IOption setOption(IConfiguration config, IHoldsOptions holder, IOption option, boolean value) {
		IOption retOpt;
		try {
			// Request a value change and set dirty if real change results
			retOpt = config.setOption(holder, option, value);
			if (retOpt.getValueHandler().handleValue(config, holder, retOpt, retOpt.getValueHandlerExtraArgument(),
					IManagedOptionValueHandler.EVENT_APPLY)) {
				// TODO : Event is handled successfully and returned true.
				// May need to do something here say log a message.
			} else {
				// Event handling Failed.
			}
			// initializePathEntries(config,retOpt);
			// notifyListeners(config, retOpt);
		} catch (BuildException e) {
			return null;
		}
		return retOpt;
	}

	/**
	 * Set the boolean value for an option for a given config.
	 *
	 * @param resConfig The resource configuration the option belongs to.
	 * @param holder    The holder/parent of the option.
	 * @param option    The option to set the value for.
	 * @param value     The boolean that the option should contain after the change.
	 *
	 * @return The modified option. This can be the same option or a newly created
	 *         option.
	 *
	 * @since 3.0 - The type and name of the <code>ITool tool</code> parameter has
	 *        changed to <code>IHoldsOptions holder</code>. Client code assuming
	 *        <code>ITool</code> as type, will continue to work unchanged.
	 */
	public static IOption setOption(IResourceInfo resConfig, IHoldsOptions holder, IOption option, boolean value) {
		IOption retOpt;
		try {
			// Request a value change and set dirty if real change results
			retOpt = resConfig.setOption(holder, option, value);
			if (retOpt != null && retOpt.getValueHandler().handleValue(resConfig, holder, retOpt,
					retOpt.getValueHandlerExtraArgument(), IManagedOptionValueHandler.EVENT_APPLY)) {
				// TODO : Event is handled successfully and returned true.
				// May need to do something here say log a message.
			} else {
				// Event handling Failed.
			}
			// initializePathEntries(resConfig,retOpt);
			notifyListeners(resConfig, retOpt);
		} catch (BuildException e) {
			return null;
		}
		return retOpt;
	}

	/**
	 * Set the string value for an option for a given config.
	 *
	 * @param config The configuration the option belongs to.
	 * @param holder The holder/parent of the option.
	 * @param option The option to set the value for.
	 * @param value  The value that the option should contain after the change.
	 *
	 * @return The modified option. This can be the same option or a newly created
	 *         option.
	 *
	 * @since 3.0 - The type and name of the <code>ITool tool</code> parameter has
	 *        changed to <code>IHoldsOptions holder</code>. Client code assuming
	 *        <code>ITool</code> as type, will continue to work unchanged.
	 */
	public static IOption setOption(IConfiguration config, IHoldsOptions holder, IOption option, String value) {
		IOption retOpt;
		try {
			retOpt = config.setOption(holder, option, value);
			if (retOpt.getValueHandler().handleValue(config, holder, retOpt, retOpt.getValueHandlerExtraArgument(),
					IManagedOptionValueHandler.EVENT_APPLY)) {
				// TODO : Event is handled successfully and returned true.
				// May need to do something here say log a message.
			} else {
				// Event handling Failed.
			}
			// initializePathEntries(config,retOpt);
			// notifyListeners(config, retOpt);
		} catch (BuildException e) {
			return null;
		}
		return retOpt;
	}

	/**
	 * Set the string value for an option for a given resource config.
	 *
	 * @param resConfig The resource configuration the option belongs to.
	 * @param holder    The holder/parent of the option.
	 * @param option    The option to set the value for.
	 * @param value     The value that the option should contain after the change.
	 *
	 * @return The modified option. This can be the same option or a newly created
	 *         option.
	 *
	 * @since 3.0 - The type and name of the <code>ITool tool</code> parameter has
	 *        changed to <code>IHoldsOptions holder</code>. Client code assuming
	 *        <code>ITool</code> as type, will continue to work unchanged.
	 */
	public static IOption setOption(IResourceInfo resConfig, IHoldsOptions holder, IOption option, String value) {
		IOption retOpt;
		try {
			retOpt = resConfig.setOption(holder, option, value);
			if (retOpt.getValueHandler().handleValue(resConfig, holder, retOpt, retOpt.getValueHandlerExtraArgument(),
					IManagedOptionValueHandler.EVENT_APPLY)) {
				// TODO : Event is handled successfully and returned true.
				// May need to do something here say log a message.
			} else {
				// Event handling Failed.
			}
			// initializePathEntries(resConfig,retOpt);
			notifyListeners(resConfig, retOpt);
		} catch (BuildException e) {
			return null;
		}
		return retOpt;
	}

	/**
	 * Set the string array value for an option for a given config.
	 *
	 * @param config The configuration the option belongs to.
	 * @param holder The holder/parent of the option.
	 * @param option The option to set the value for.
	 * @param value  The values the option should contain after the change.
	 *
	 * @return The modified option. This can be the same option or a newly created
	 *         option.
	 *
	 * @since 3.0 - The type and name of the <code>ITool tool</code> parameter has
	 *        changed to <code>IHoldsOptions holder</code>. Client code assuming
	 *        <code>ITool</code> as type, will continue to work unchanged.
	 */
	public static IOption setOption(IConfiguration config, IHoldsOptions holder, IOption option, String[] value) {
		IOption retOpt;
		try {
			retOpt = config.setOption(holder, option, value);
			if (retOpt.getValueHandler().handleValue(config, holder, retOpt, retOpt.getValueHandlerExtraArgument(),
					IManagedOptionValueHandler.EVENT_APPLY)) {
				// TODO : Event is handled successfully and returned true.
				// May need to do something here say log a message.
			} else {
				// Event handling Failed.
			}
			// initializePathEntries(config,retOpt);
			// notifyListeners(config, retOpt);
		} catch (BuildException e) {
			return null;
		}
		return retOpt;
	}

	/**
	 * Set the string array value for an option for a given resource config.
	 *
	 * @param resConfig The resource configuration the option belongs to.
	 * @param holder    The holder/parent of the option.
	 * @param option    The option to set the value for.
	 * @param value     The values the option should contain after the change.
	 *
	 * @return The modified option. This can be the same option or a newly created
	 *         option.
	 *
	 * @since 3.0 - The type and name of the <code>ITool tool</code> parameter has
	 *        changed to <code>IHoldsOptions holder</code>. Client code assuming
	 *        <code>ITool</code> as type, will continue to work unchanged.
	 */
	public static IOption setOption(IResourceInfo resConfig, IHoldsOptions holder, IOption option, String[] value) {
		IOption retOpt;
		try {
			retOpt = resConfig.setOption(holder, option, value);
			if (retOpt.getValueHandler().handleValue(resConfig, holder, retOpt, retOpt.getValueHandlerExtraArgument(),
					IManagedOptionValueHandler.EVENT_APPLY)) {
				// TODO : Event is handled successfully and returned true.
				// May need to do something here say log a message.
			} else {
				// Event handling Failed.
			}
			// initializePathEntries(resConfig,retOpt);
			notifyListeners(resConfig, retOpt);
		} catch (BuildException e) {
			return null;
		}
		return retOpt;
	}

	public static IOption setOption(IResourceInfo resConfig, IHoldsOptions holder, IOption option,
			OptionStringValue[] value) {
		IOption retOpt;
		try {
			retOpt = resConfig.setOption(holder, option, value);
			if (retOpt.getValueHandler().handleValue(resConfig, holder, retOpt, retOpt.getValueHandlerExtraArgument(),
					IManagedOptionValueHandler.EVENT_APPLY)) {
				// TODO : Event is handled successfully and returned true.
				// May need to do something here say log a message.
			} else {
				// Event handling Failed.
			}
			// initializePathEntries(resConfig,retOpt);
			notifyListeners(resConfig, retOpt);
		} catch (BuildException e) {
			return null;
		}
		return retOpt;
	}

	public static void removeBuildInfo(IResource resource) {
		/*
		 * IManagedBuildInfo info = findBuildInfo(resource, false); if(info != null){
		 * IConfiguration[] configs = info.getManagedProject().getConfigurations(); //
		 * Send an event to each configuration and if they exist, its resource
		 * configurations for (int i=0; i < configs.length; i++) {
		 * ManagedBuildManager.performValueHandlerEvent(configs[i],
		 * IManagedOptionValueHandler.EVENT_CLOSE); }
		 * 
		 * info.setValid(false);
		 * 
		 * try { resource.setSessionProperty(buildInfoProperty, null); } catch
		 * (CoreException e) { } }
		 */
	}

	/**
	 * Resets the build information for the project and configuration specified in
	 * the arguments. The build information will contain the settings defined in the
	 * plugin manifest.
	 */
	public static void resetConfiguration(IProject project, IConfiguration configuration) {
		// // reset the configuration
		// if (configuration instanceof MultiConfiguration) {
		// IConfiguration[] cfs = (IConfiguration[]) ((MultiConfiguration)
		// configuration).getItems();
		// for (IConfiguration c : cfs) {
		// ((Configuration) c).reset();
		// performValueHandlerEvent(c, IManagedOptionValueHandler.EVENT_SETDEFAULT,
		// false);
		// }
		// } else {
		// ((Configuration) configuration).reset();
		// performValueHandlerEvent(configuration,
		// IManagedOptionValueHandler.EVENT_SETDEFAULT, false);
		// }
	}

	public static void resetResourceConfiguration(IProject project, IResourceConfiguration resConfig) {
		// reset the configuration
		((ResourceConfiguration) resConfig).reset();

		performValueHandlerEvent(resConfig, IManagedOptionValueHandler.EVENT_SETDEFAULT);

	}

	public static void resetOptionSettings(IResourceInfo rcInfo) {
		// if (rcInfo instanceof IFileInfo) {
		// IConfiguration c = rcInfo.getParent();
		// Configuration cfg = null;
		// IProject project = null;
		// if (c instanceof Configuration)
		// cfg = (Configuration) c;
		// else if (c instanceof MultiConfiguration) {
		// MultiConfiguration mc = (MultiConfiguration) c;
		// IConfiguration[] cfs = (IConfiguration[]) mc.getItems();
		// cfg = (Configuration) cfs[0];
		// }
		// if (!(cfg == null || cfg.isExtensionElement() || cfg.isPreference()))
		// project = cfg.getOwner().getProject();
		//
		// if (rcInfo instanceof MultiResourceInfo) {
		// for (IResourceInfo ri : (IResourceInfo[]) ((MultiResourceInfo)
		// rcInfo).getItems())
		// resetResourceConfiguration(project, (IFileInfo) ri);
		// } else
		// resetResourceConfiguration(project, (IFileInfo) rcInfo);
		// } else {
		// if (rcInfo instanceof MultiFolderInfo) {
		// for (IFolderInfo fi : (IFolderInfo[]) ((MultiFolderInfo) rcInfo).getItems())
		// ((FolderInfo) fi).resetOptionSettings();
		// } else {
		// FolderInfo fo = (FolderInfo) rcInfo;
		// fo.resetOptionSettings();
		// }
		// }
	}

	/**
	 * Adds a ProjectType that is is specified in the manifest to the build system.
	 * It is available to any element that has a reference to it as part of its
	 * description.
	 */
	private static void addExtensionProjectType(ProjectType projectType) {
		if (theProjectTypes == null) {
			theProjectTypes = new ArrayList<>();
		}

		theProjectTypes.add(projectType);
		IProjectType previous = getExtensionProjectTypeMap().put(projectType.getId(), projectType);
		if (previous != null) {
			// Report error
			ManagedBuildManager.outputDuplicateIdError("ProjectType", //$NON-NLS-1$
					projectType.getId());
		}
	}

	/**
	 * Adds a Configuration that is is specified in the manifest to the build
	 * system. It is available to any element that has a reference to it as part of
	 * its description.
	 */
	private static void addExtensionConfiguration(Configuration configuration) {
		IConfiguration previous = getExtensionConfigurationMap().put(configuration.getId(), configuration);
		if (previous != null) {
			// Report error
			ManagedBuildManager.outputDuplicateIdError("Configuration", //$NON-NLS-1$
					configuration.getId());
		}
	}

	/**
	 * Adds a Resource Configuration that is is specified in the manifest to the
	 * build system. It is available to any element that has a reference to it as
	 * part of its description.
	 */
	public static void addExtensionResourceConfiguration(ResourceConfiguration resourceConfiguration) {
		IResourceConfiguration previous = getExtensionResourceConfigurationMap().put(resourceConfiguration.getId(),
				resourceConfiguration);
		if (previous != null) {
			// Report error
			ManagedBuildManager.outputDuplicateIdError("ResourceConfiguration", //$NON-NLS-1$
					resourceConfiguration.getId());
		}
	}

	/**
	 * Adds a ToolChain that is is specified in the manifest to the build system. It
	 * is available to any element that has a reference to it as part of its
	 * description.
	 */
	private static void addExtensionToolChain(IToolChain toolChain) {
		IToolChain previous = getExtensionToolChainMapInternal().put(toolChain.getId(), toolChain);
		if (previous != null) {
			// Report error
			ManagedBuildManager.outputDuplicateIdError("ToolChain", //$NON-NLS-1$
					toolChain.getId());
		}
	}

	/**
	 * Adds a tool that is is specified in the manifest to the build system. This
	 * tool is available to any target that has a reference to it as part of its
	 * description. This permits a tool that is common to many targets to be defined
	 * only once.
	 */
	private static void addExtensionTool(Tool tool) {
		ITool previous = getExtensionToolMapInternal().put(tool.getId(), tool);
		if (previous != null) {
			// Report error
			ManagedBuildManager.outputDuplicateIdError("Tool", //$NON-NLS-1$
					tool.getId());
		}
	}

	/**
	 * Adds a TargetPlatform that is is specified in the manifest to the build
	 * system. It is available to any element that has a reference to it as part of
	 * its description.
	 */
	private static void addExtensionTargetPlatform(TargetPlatform targetPlatform) {
		ITargetPlatform previous = getExtensionTargetPlatformMap().put(targetPlatform.getId(), targetPlatform);
		if (previous != null) {
			// Report error
			ManagedBuildManager.outputDuplicateIdError("TargetPlatform", //$NON-NLS-1$
					targetPlatform.getId());
		}
	}

	/**
	 * Adds a Builder that is is specified in the manifest to the build system. It
	 * is available to any element that has a reference to it as part of its
	 * description.
	 */
	private static void addExtensionBuilder(Builder builder) {
		IBuilder previous = getExtensionBuilderMapInternal().put(builder.getId(), builder);
		if (previous != null) {
			// Report error
			ManagedBuildManager.outputDuplicateIdError("Builder", //$NON-NLS-1$
					builder.getId());
		}
	}

	/**
	 * Adds a Option that is is specified in the manifest to the build system. It is
	 * available to any element that has a reference to it as part of its
	 * description.
	 */
	public static void addExtensionOption(Option option) {
		IOption previous = getExtensionOptionMap().put(option.getId(), option);
		if (previous != null) {
			// Report error
			ManagedBuildManager.outputDuplicateIdError("Option", //$NON-NLS-1$
					option.getId());
		}
	}

	/**
	 * Adds a OptionCategory that is is specified in the manifest to the build
	 * system. It is available to any element that has a reference to it as part of
	 * its description.
	 */
	public static void addExtensionOptionCategory(OptionCategory optionCategory) {
		IOptionCategory previous = getExtensionOptionCategoryMap().put(optionCategory.getId(), optionCategory);
		if (previous != null) {
			// Report error
			ManagedBuildManager.outputDuplicateIdError("OptionCategory", //$NON-NLS-1$
					optionCategory.getId());
		}
	}

	/**
	 * Adds an InputType that is is specified in the manifest to the build system.
	 * It is available to any element that has a reference to it as part of its
	 * description.
	 */
	public static void addExtensionInputType(InputType inputType) {
		IInputType previous = getExtensionInputTypeMap().put(inputType.getId(), inputType);
		if (previous != null) {
			// Report error
			ManagedBuildManager.outputDuplicateIdError("InputType", //$NON-NLS-1$
					inputType.getId());
		}
	}

	/**
	 * Adds an OutputType that is is specified in the manifest to the build system.
	 * It is available to any element that has a reference to it as part of its
	 * description.
	 */
	public static void addExtensionOutputType(OutputType outputType) {
		IOutputType previous = getExtensionOutputTypeMap().put(outputType.getId(), outputType);
		if (previous != null) {
			// Report error
			ManagedBuildManager.outputDuplicateIdError("OutputType", //$NON-NLS-1$
					outputType.getId());
		}
	}

	/**
	 * Creates a new project instance for the resource based on the parent project
	 * type.
	 *
	 * @param parent - parent project type
	 * @return new <code>ITarget</code> with settings based on the parent passed in
	 *         the arguments
	 */
	public static IManagedProject createManagedProject(IResource resource, IProjectType parent) throws BuildException {
		return new ManagedProject(resource, parent);
	}

	public static IStatus initBuildInfoContainer(IResource resource) {
		return Status.OK_STATUS;
		/*
		 * ManagedBuildInfo buildInfo = null;
		 * 
		 * // Get the build info associated with this project for this session try {
		 * buildInfo = findBuildInfo(resource.getProject(), true);
		 * initBuildInfoContainer(buildInfo); } catch (CoreException e) { return new
		 * Status(IStatus.ERROR, Activator.getId(), IStatus.ERROR,
		 * e.getLocalizedMessage(), e); } return new Status(IStatus.OK,
		 * Activator.getId(), IStatus.OK,
		 * MessageFormat.format("ManagedBuildInfo.message.init.ok", resource.getName()),
		 * //$NON-NLS-1$ null);
		 */
	}

	/**
	 * This method loads all of the managed build system manifest files that have
	 * been installed with CDT. An internal hierarchy of objects is created that
	 * contains the information from the manifest files. The information is then
	 * accessed through the ManagedBuildManager.
	 *
	 * Since the class does not have a constructor but all public methods call this
	 * method first, it is effectively a startup method
	 */
	private static void loadExtensions() {
		if (theProjectTypesLoaded)
			return;
		try {
			loadExtensionsSynchronized();
		} catch (BuildException e) {
			e.printStackTrace();
		}

	}

	private synchronized static void loadExtensionsSynchronized() throws BuildException {
		String extensionPointID = supportedExtensionPointIDs.get(0);
		String extensionName = "io.sloeber.builddef";

		// Do this once
		if (theProjectTypesLoaded)
			return;

		// This routine gets called recursively. If so, just return
		if (theProjectTypesLoading)
			return;
		theProjectTypesLoading = true;

		try {

			// Get the extensions that use the current CDT managed build model
			IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(extensionPointID);
			if (extensionPoint != null) {
				IExtension extension = extensionPoint.getExtension(extensionName);
				if (extension != null) {
					IConfigurationElement[] elements = extension.getConfigurationElements();

					String revision=null; //TOFIX JABA to remove
					for (IConfigurationElement element : elements) {
						try {
							switch(element.getName()) {
							case IProjectType.PROJECTTYPE_ELEMENT_NAME:
								addExtensionProjectType(new ProjectType(element, revision));
								break;
								
							case IConfiguration.CONFIGURATION_ELEMENT_NAME:
								addExtensionConfiguration(new Configuration((ProjectType) null, element, revision));
								break;
							case IToolChain.TOOL_CHAIN_ELEMENT_NAME:
								addExtensionToolChain(new ToolChain((IFolderInfo) null, element, revision));
								break;
							case ITool.TOOL_ELEMENT_NAME:
								addExtensionTool(new Tool((ProjectType) null, element, revision));
								break;
							case ITargetPlatform.TARGET_PLATFORM_ELEMENT_NAME:
								addExtensionTargetPlatform(new TargetPlatform((ToolChain) null, element, revision));
								break;
							case IBuilder.BUILDER_ELEMENT_NAME:
								addExtensionBuilder( new Builder((ToolChain) null, element, revision));
								break;
								default:
									// TODO: Report an error (log?)
							}

						} catch (Exception ex) {
							// TODO: log
							ex.printStackTrace();
						}
					}

				}

				Collection<IProjectType> prjTypes = getExtensionProjectTypeMap().values();
				for (IProjectType projectType : prjTypes) {
					try {
						((ProjectType) projectType).resolveReferences();
					} catch (Exception ex) {
						// TODO: log
						ex.printStackTrace();
					}
				}
				Collection<IConfiguration> configurations = getExtensionConfigurationMap().values();
				for (IConfiguration configuration : configurations) {
					try {
						((Configuration) configuration).resolveReferences();
					} catch (Exception ex) {
						// TODO: log
						ex.printStackTrace();
					}
				}
				Collection<IResourceConfiguration> resConfigs = getExtensionResourceConfigurationMap().values();
				for (IResourceConfiguration resConfig : resConfigs) {
					try {
						((ResourceConfiguration) resConfig).resolveReferences();
					} catch (Exception ex) {
						// TODO: log
						ex.printStackTrace();
					}
				}
				Collection<IToolChain> toolChains = getExtensionToolChainMapInternal().values();
				for (IToolChain toolChain : toolChains) {
					try {
						((ToolChain) toolChain).resolveReferences();
					} catch (Exception ex) {
						// TODO: log
						ex.printStackTrace();
					}
				}
				Collection<Tool> tools = getExtensionToolMapInternal().values();
				for (Tool tool : tools) {
					try {
						tool.resolveReferences();
					} catch (Exception ex) {
						// TODO: log
						ex.printStackTrace();
					}
				}
				Collection<ITargetPlatform> targetPlatforms = getExtensionTargetPlatformMap().values();
				for (ITargetPlatform targetPlatform : targetPlatforms) {
					try {
						((TargetPlatform) targetPlatform).resolveReferences();
					} catch (Exception ex) {
						// TODO: log
						ex.printStackTrace();
					}
				}
				Collection<IBuilder> builders = getExtensionBuilderMapInternal().values();
				for (IBuilder builder : builders) {
					try {
						((Builder) builder).resolveReferences();
					} catch (Exception ex) {
						// TODO: log
						ex.printStackTrace();
					}
				}
				Collection<IOption> options = getExtensionOptionMap().values();
				for (IOption option : options) {
					try {
						((Option) option).resolveReferences();
					} catch (Exception ex) {
						// TODO: log
						ex.printStackTrace();
					}
				}
				Collection<IOptionCategory> optionCategories = getExtensionOptionCategoryMap().values();
				for (IOptionCategory optionCat : optionCategories) {
					try {
						((OptionCategory) optionCat).resolveReferences();
					} catch (Exception ex) {
						// TODO: log
						ex.printStackTrace();
					}
				}
			}

			performAdjustments();
			theProjectTypesLoaded = true;

		} catch (Exception e) {
			theProjectTypes = null;
			theConfigElementMap = null;
			theProjectTypeMap = null;
		}
		theProjectTypesLoading = false;
	}

	private static void performAdjustments() {
		IProjectType types[] = getDefinedProjectTypes();
		for (IProjectType type : types) {
			IConfiguration cfgs[] = type.getConfigurations();
			for (IConfiguration cfg : cfgs) {
				adjustConfig(cfg);
			}
		}

		for (IProjectType type : types) {
			IConfiguration cfgs[] = type.getConfigurations();
			for (IConfiguration cfg : cfgs) {
				performValueHandlerEvent(cfg, IManagedOptionValueHandler.EVENT_LOAD);
			}
		}

	}

	private static void adjustConfig(IConfiguration cfg) {
		// IResourceInfo rcInfos[] = cfg.getResourceInfos();
		// for (IResourceInfo rcInfo : rcInfos) {
		// if (rcInfo instanceof IFolderInfo) {
		// IFolderInfo info = (IFolderInfo) rcInfo;
		// IToolChain tc = info.getToolChain();
		// adjustHolder(info, tc);
		//
		// ITool tools[] = tc.getTools();
		// for (ITool tool : tools) {
		// adjustHolder(info, tool);
		// }
		// } else if (rcInfo instanceof IFileInfo) {
		// IFileInfo info = (IFileInfo) rcInfo;
		// ITool rcTools[] = info.getTools();
		// for (ITool rcTool : rcTools) {
		// adjustHolder(info, rcTool);
		// }
		//
		// }
		// }
		//
		// IResourceConfiguration rcCfgs[] = cfg.getResourceConfigurations();
		//
		// // for (IResourceConfiguration rcCfg : rcCfgs) {
		// // }

	}

	private static void adjustHolder(IResourceInfo rcInfo, IHoldsOptions holder) {
		IOption options[] = holder.getOptions();

		for (IOption opt : options) {
			Option option = (Option) opt;
			BooleanExpressionApplicabilityCalculator calc = option.getBooleanExpressionCalculator(true);

			if (calc != null)
				calc.adjustOption(rcInfo, holder, option, true);
		}
	}

	private static void loadConfigElements(IConfigurationElement elements) {
	}

	/*
	 * Creates a new build information object and associates it with the resource in
	 * the argument. Note that the information contains no build target or
	 * configuation information. It is the responsibility of the caller to populate
	 * it. It is also important to note that the caller is responsible for
	 * associating an IPathEntryContainer with the build information after it has
	 * been populated. <p> The typical sequence of calls to add a new build
	 * information object to a managed build project is <p><pre>
	 * ManagedBuildManager.createBuildInfo(project); &#047;&#047; Do whatever
	 * initialization you need here ManagedBuildManager.createTarget(project);
	 * ManagedBuildManager.initBuildInfoContainer(project);
	 *
	 * @param resource The resource the build information is associated with
	 */
	public static ManagedBuildInfo createBuildInfo(IResource resource) {
		IProject proj = resource.getProject();
		ManagedBuildInfo buildInfo = new ManagedBuildInfo(proj);
		try {
			setLoaddedBuildInfo(proj, buildInfo);
		} catch (CoreException e) {
			Activator.log(e);
			buildInfo = null;
		}
		return buildInfo;
	}

	public static void setLoaddedBuildInfo(IProject project, IManagedBuildInfo info) throws CoreException {
		// Associate the build info with the project for the duration of the session
		// project.setSessionProperty(buildInfoProperty, info);
		// IResourceRuleFactory rcRf = ResourcesPlugin.getWorkspace().getRuleFactory();
		// ISchedulingRule rule = rcRf.modifyRule(project);
		// IJobManager mngr = Job.getJobManager();

		// try {
		// mngr.beginRule(rule, null);
		doSetLoaddedInfo(project, info, true);
		// } catch (IllegalArgumentException e) {
		// // TODO: set anyway for now
		// doSetLoaddedInfo(project, info);
		// }finally {
		// mngr.endRule(rule);
		// }
	}

	private synchronized static void doSetLoaddedInfo(IProject project, IManagedBuildInfo info, boolean overwrite) {
		if (!overwrite && fInfoMap.get(project) != null)
			return;

		if (info != null) {
			fInfoMap.put(project, info);
			// if (BuildDbgUtil.DEBUG)
			// BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD,
			// "build info load: build info set for project " + project.getName());
			// //$NON-NLS-1$
		} else {
			fInfoMap.remove(project);
			// if (BuildDbgUtil.DEBUG)
			// BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD,
			// "build info load: build info CLEARED for project " + project.getName());
			// //$NON-NLS-1$
		}
	}

	// private static IManagedConfigElementProvider
	// createConfigProvider(DefaultManagedConfigElement element)
	// throws CoreException {
	//
	// return (IManagedConfigElementProvider) element.getConfigurationElement()
	// .createExecutableExtension(IManagedConfigElementProvider.CLASS_ATTRIBUTE);
	// }
	//
	// private static IManagedBuildDefinitionsStartup
	// createStartUpConfigLoader(DefaultManagedConfigElement element)
	// throws CoreException {
	//
	// return (IManagedBuildDefinitionsStartup) element.getConfigurationElement()
	// .createExecutableExtension(IManagedBuildDefinitionsStartup.CLASS_ATTRIBUTE);
	// }

	public static boolean manages(IResource resource) {
		ICProjectDescription des = CoreModel.getDefault().getProjectDescription(resource.getProject(), false);
		if (des == null) {
			return false;
		}

		ICConfigurationDescription cfgDes = des.getActiveConfiguration();
		IConfiguration cfg = ManagedBuildManager.getConfigurationForDescription(cfgDes);
		if (cfg != null)
			return true;
		return false;

		// // The managed build manager manages build information for the
		// // resource IFF it it is a project and has a build file with the proper
		// // root element
		// IProject project = null;
		// if (resource instanceof IProject){
		// project = (IProject)resource;
		// } else if (resource instanceof IFile) {
		// project = ((IFile)resource).getProject();
		// } else {
		// return false;
		// }
		// IFile file = project.getFile(SETTINGS_FILE_NAME);
		// if (file.exists()) {
		// try {
		// InputStream stream = file.getContents();
		// DocumentBuilder parser =
		// DocumentBuilderFactory.newInstance().newDocumentBuilder();
		// Document document = parser.parse(stream);
		// NodeList nodes = document.getElementsByTagName(ROOT_NODE_NAME);
		// return (nodes.getLength() > 0);
		// } catch (Exception e) {
		// return false;
		// }
		// }
		// return false;
	}

	/**
	 * Private helper method that first checks to see if a build information object
	 * has been associated with the project for the current workspace session. If
	 * one cannot be found, one is created from the project file associated with the
	 * argument. If there is no project file or the load fails for some reason, the
	 * method will return {@code null}.
	 */
	private static ManagedBuildInfo findBuildInfo(IResource rc, boolean forceLoad) {

		if (rc == null) {
			// if (BuildDbgUtil.DEBUG)
			// BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD, "build info
			// load: null resource"); //$NON-NLS-1$
			return null;
		}

		ManagedBuildInfo buildInfo = null;
		IProject proj = rc.getProject();

		if (!proj.exists()) {
			// if (BuildDbgUtil.DEBUG)
			// BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD,
			// "build info load: info is null, project does not exist"); //$NON-NLS-1$
			return null;
		}

		// if (BuildDbgUtil.DEBUG)
		// BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD,
		// "build info load: info is null, querying the update mngr"); //$NON-NLS-1$
		// buildInfo = UpdateManagedProjectManager.getConvertedManagedBuildInfo(proj);

		if (buildInfo != null)
			return buildInfo;

		// Check if there is any build info associated with this project for this
		// session
		try {
			buildInfo = getLoadedBuildInfo(proj);
		} catch (CoreException e) {
			// if (BuildDbgUtil.DEBUG)
			// BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD,
			// "build info load: core exception while getting the loaded info: " +
			// e.getLocalizedMessage()); //$NON-NLS-1$
			return null;
		}

		if (buildInfo == null /* && forceLoad */) {
			int flags = forceLoad ? 0 : ICProjectDescriptionManager.GET_IF_LOADDED;

			// if (BuildDbgUtil.DEBUG)
			// BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD,
			// "build info load: build info is NOT loaded" + (forceLoad ? " forceload" :
			// "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			ICProjectDescription projDes = CoreModel.getDefault().getProjectDescriptionManager()
					.getProjectDescription(proj, flags);
			if (projDes != null) {
				// if (BuildDbgUtil.DEBUG)
				// BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD,
				// "build info load: project description is obtained, qwerying the loaded build
				// info"); //$NON-NLS-1$
				try {
					buildInfo = getLoadedBuildInfo(proj);
				} catch (CoreException e) {
					// if (BuildDbgUtil.DEBUG)
					// BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD,
					// "build info load: core exception while getting the loaded info (2): "
					// //$NON-NLS-1$
					// + e.getLocalizedMessage());
					return null;
				}

				if (buildInfo == null) {
					// if (BuildDbgUtil.DEBUG)
					// BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD,
					// "build info load: info is null, trying the cfg data provider"); //$NON-NLS-1$

					// buildInfo = ConfigurationDataProvider.getLoaddedBuildInfo(projDes);
					if (buildInfo != null) {
						// if (BuildDbgUtil.DEBUG)
						// BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD,
						// "build info load: info found, setting as loaded"); //$NON-NLS-1$

						try {
							setLoaddedBuildInfo(proj, buildInfo);
						} catch (CoreException e) {
							// if (BuildDbgUtil.DEBUG)
							// BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD,
							// "build info load: core exception while setting loaded description, ignoring;
							// : " //$NON-NLS-1$
							// + e.getLocalizedMessage());
						}
					}

				}

				// } else if (BuildDbgUtil.DEBUG) {
				// BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD,
				// "build info load: project description in null"); //$NON-NLS-1$
			}

			// if(buildInfo == null){
			// if(BuildDbgUtil.DEBUG)
			// BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD, "build info
			// load: info is null, querying the update mngr"); //$NON-NLS-1$
			// buildInfo = UpdateManagedProjectManager.getConvertedManagedBuildInfo(proj);
			// }
		}
		// if (buildInfo == null && resource instanceof IProject)
		// buildInfo = findBuildInfoSynchronized((IProject)resource, forceLoad);
		/*
		 * // Nothing in session store, so see if we can load it from cdtbuild if
		 * (buildInfo == null && resource instanceof IProject) { try { buildInfo =
		 * loadBuildInfo((IProject)resource); } catch (Exception e) { // TODO: Issue
		 * error reagarding not being able to load the project file (.cdtbuild) }
		 * 
		 * try { // Check if the project needs its container initialized
		 * initBuildInfoContainer(buildInfo); } catch (CoreException e) { // We can live
		 * without a path entry container if the build information is valid } }
		 */
		if (buildInfo != null)
			buildInfo.updateOwner(proj);

		// if (BuildDbgUtil.DEBUG) {
		// if (buildInfo == null)
		// BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD, "build info
		// load: build info is null"); //$NON-NLS-1$
		// // else
		// // BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD, "build
		// info load: build info found");
		// }

		return buildInfo;
	}

	synchronized static ManagedBuildInfo getLoadedBuildInfo(IProject project) throws CoreException {
		// Check if there is any build info associated with this project for this
		// session
		ManagedBuildInfo buildInfo = (ManagedBuildInfo) fInfoMap.get(project);// project.getSessionProperty(buildInfoProperty);
		// Make sure that if a project has build info, that the info is not corrupted
		if (buildInfo != null) {
			buildInfo.updateOwner(project);
		}
		return buildInfo;
	}

	/**
	 * Finds, but does not create, the managed build information for the argument.
	 * Loads the build info in case it is not currently loaded Calling this method
	 * is the same as calling getBuildInfo(IResource resource, boolean forceLoad)
	 * with the "forceLoad" argument set to true
	 *
	 * @param resource The resource to search for managed build information on.
	 * @return IManagedBuildInfo The build information object for the resource, or
	 *         null if it doesn't exist
	 */
	public static IManagedBuildInfo getBuildInfo(IResource resource) {
		return getBuildInfo(resource, true);
	}

	/**
	 * Finds, but does not create, the managed build information for the argument.
	 * If the build info is not currently loaded and "forceLoad" argument is set to
	 * true, loads the build info from the .cdtbuild file In case "forceLoad" is
	 * false, does not load the build info and returns null in case it is not loaded
	 *
	 * @param resource  The resource to search for managed build information on.
	 * @param forceLoad specifies whether the build info should be loaded in case it
	 *                  is not loaded currently.
	 * @return IManagedBuildInfo The build information object for the resource.
	 */
	public static IManagedBuildInfo getBuildInfo(IResource resource, boolean forceLoad) {
		return findBuildInfo(resource.getProject(), forceLoad);
	}

	/**
	 * Answers the current version of the managed builder plugin.
	 *
	 * @return the current version of the managed builder plugin
	 * @since 8.0
	 */
	public static Version getBuildInfoVersion() {
		return buildInfoVersion;
	}

	/**
	 * Get the full URL for a path that is relative to the plug-in in which
	 * .buildDefinitions are defined
	 *
	 * @return the full URL for a path relative to the .buildDefinitions plugin
	 */
	public static URL getURLInBuildDefinitions(DefaultManagedConfigElement element, IPath path) {
		IExtension extension = element.getExtension();
		if (extension != null) {
			Bundle bundle = Platform.getBundle(extension.getContributor().getName());
			URL url = FileLocator.find(bundle, path);
			if (url != null) {
				try {
					return FileLocator.toFileURL(url);
				} catch (IOException e) {
					// Ignore the exception
				}
			}
		}

		// Print a warning
		outputIconError(path.toString());

		return null;
	}

	/*
	 * @return
	 */
	private static Map<IResource, List<IScannerInfoChangeListener>> getBuildModelListeners() {
		if (buildModelListeners == null) {
			buildModelListeners = new HashMap<>();
		}
		return buildModelListeners;
	}

	private static Map<IBuildObject, IManagedConfigElement> getConfigElementMap() {
		if (!theProjectTypesLoading)
			throw new IllegalStateException();

		if (theConfigElementMap == null) {
			theConfigElementMap = new HashMap<>();
		}
		return theConfigElementMap;
	}

	/**
	 * @noreference This method public for implementation reasons. Not intended for
	 *              use by clients.
	 *
	 */
	public static void putConfigElement(IBuildObject buildObj, IManagedConfigElement configElement) {
		getConfigElementMap().put(buildObj, configElement);
	}

	/**
	 * @noreference This method public for implementation reasons. Not intended for
	 *              use by clients.
	 */
	public static IManagedConfigElement getConfigElement(IBuildObject buildObj) {
		return getConfigElementMap().get(buildObj);
	}

	public static void optionValidError(int errorId, String id) {
		String[] msgs = new String[1];
		msgs[0] = id;
		switch (errorId) {
		case ERROR_CATEGORY:
			ManagedBuildManager.outputManifestError(
					MessageFormat.format(ManagedBuildManager_error_manifest_option_category, msgs));
			break;
		case ERROR_FILTER:
			ManagedBuildManager
					.outputManifestError(MessageFormat.format(ManagedBuildManager_error_manifest_option_filter, msgs));
			break;
		}
	}

	public static void optionValueHandlerError(String attribute, String id) {
		String[] msgs = new String[2];
		msgs[0] = attribute;
		msgs[1] = id;
		ManagedBuildManager.outputManifestError(
				MessageFormat.format(ManagedBuildManager_error_manifest_option_valuehandler, msgs));
	}

	public static void outputResolveError(String attribute, String lookupId, String type, String id) {
		String[] msgs = new String[4];
		msgs[0] = attribute;
		msgs[1] = lookupId;
		msgs[2] = type;
		msgs[3] = id;
		ManagedBuildManager
				.outputManifestError(MessageFormat.format(ManagedBuildManager_error_manifest_resolving, msgs));
	}

	public static void outputDuplicateIdError(String type, String id) {
		String[] msgs = new String[2];
		msgs[0] = type;
		msgs[1] = id;
		ManagedBuildManager
				.outputManifestError(MessageFormat.format(ManagedBuildManager_error_manifest_duplicate, msgs));
	}

	public static void outputManifestError(String message) {
		System.err.println(ManagedBuildManager_error_manifest_header + message + NEWLINE);
	}

	public static void outputIconError(String iconLocation) {
		String[] msgs = new String[1];
		msgs[0] = iconLocation;
		ManagedBuildManager.outputManifestError(MessageFormat.format(ManagedBuildManager_error_manifest_icon, msgs));
	}

	// /**
	// * @return the instance of the Environment Variable Provider
	// */
	public static IEnvironmentVariableProvider getEnvironmentVariableProvider() {
		return EnvironmentVariableProvider.getDefault();
	}

	/**
	 * @return the version, if 'id' contains a valid version or {@code null}
	 *         otherwise.
	 */

	public static String getVersionFromIdAndVersion(String idAndVersion) {

		// Get the index of the separator '_' in tool id.
		int index = idAndVersion.lastIndexOf('_');

		// Validate the version number if exists.
		if (index != -1) {
			// Get the version number from tool id.
			String version = idAndVersion.substring(index + 1);

			try {
				// If there is a valid version then return 'version'
				Version.parseVersion(version);
				return version;
			} catch (IllegalArgumentException e) {
				// ignore exception and return null
			}
		}
		// If there is no version information or not a valid version, return null
		return null;
	}

	/**
	 * @return If the input to this function contains 'id & a valid version', it
	 *         returns only the 'id' part Otherwise it returns the received input
	 *         back.
	 */
	public static String getIdFromIdAndVersion(String idAndVersion) {
		// If there is a valid version return only 'id' part
		if (getVersionFromIdAndVersion(idAndVersion) != null) {
			// Get the index of the separator '_' in tool id.
			int index = idAndVersion.lastIndexOf('_');
			return idAndVersion.substring(0, index);
		} else {
			// if there is no version or no valid version
			return idAndVersion;
		}
	}

	/**
	 * @return the instance of the Build Macro Provider
	 */
	// TODO JABA remove this method and call BuildMacroProvider.getDefault()
	public static IBuildMacroProvider getBuildMacroProvider() {
		return BuildMacroProvider.getDefault();
	}

	/**
	 * Send event to value handlers of relevant configuration including all its
	 * child resource configurations, if they exist.
	 *
	 * @param config configuration for which to send the event
	 * @param event  to be sent
	 *
	 * @since 3.0
	 */
	public static void performValueHandlerEvent(IConfiguration config, int event) {
		performValueHandlerEvent(config, event, true);
	}

	/**
	 * Send event to value handlers of relevant configuration.
	 *
	 * @param config     configuration for which to send the event
	 * @param event      to be sent
	 * @param doChildren - if true, also perform the event for all resource
	 *                   configurations that are children if this configuration.
	 *
	 * @since 3.0
	 */
	public static void performValueHandlerEvent(IConfiguration config, int event, boolean doChildren) {

		IToolChain toolChain = config.getToolChain();
		if (toolChain == null)
			return;

		IOption[] options = toolChain.getOptions();
		// Get global options directly under Toolchain (not associated with a particular
		// tool)
		// This has to be sent to all the Options associated with this configuration.
		for (IOption option : options) {
			// Ignore invalid options
			if (option.isValid()) {
				// Call the handler
				if (option.getValueHandler().handleValue(config, toolChain, option,
						option.getValueHandlerExtraArgument(), event)) {
					// TODO : Event is handled successfully and returned true.
					// May need to do something here say logging a message.
				} else {
					// Event handling Failed.
				}
			}
		}

		// Get options associated with tools under toolChain
		ITool[] tools = config.getFilteredTools();
		for (ITool tool : tools) {
			IOption[] toolOptions = tool.getOptions();
			for (IOption toolOption : toolOptions) {
				// Ignore invalid options
				if (toolOption.isValid()) {
					// Call the handler
					if (toolOption.getValueHandler().handleValue(config, tool, toolOption,
							toolOption.getValueHandlerExtraArgument(), event)) {
						// TODO : Event is handled successfully and returned true.
						// May need to do something here say logging a message.
					} else {
						// Event handling Failed.
					}
				}
			}
		}

		// Call backs for Resource Configurations associated with this config.
		if (doChildren == true) {
			IResourceConfiguration[] resConfigs = config.getResourceConfigurations();
			for (IResourceConfiguration resConfig : resConfigs) {
				ManagedBuildManager.performValueHandlerEvent(resConfig, event);
			}
		}
	}

	/**
	 * Send event to value handlers of relevant configuration.
	 *
	 * @param config configuration for which to send the event
	 * @param event  to be sent
	 *
	 * @since 3.0
	 */
	public static void performValueHandlerEvent(IResourceInfo config, int event) {

		// Note: Resource configurations have no toolchain options

		// Get options associated with the resource configuration
		ITool[] tools = config instanceof IFileInfo ? ((IFileInfo) config).getToolsToInvoke()
				: ((IFolderInfo) config).getFilteredTools();
		for (ITool tool : tools) {
			IOption[] toolOptions = tool.getOptions();
			for (IOption toolOption : toolOptions) {
				// Ignore invalid options
				if (toolOption.isValid()) {
					// Call the handler
					if (toolOption.getValueHandler().handleValue(config, tool, toolOption,
							toolOption.getValueHandlerExtraArgument(), event)) {
						// TODO : Event is handled successfully and returned true.
						// May need to do something here say logging a message.
					} else {
						// Event handling Failed.
					}
				}
			}
		}
	}

	private static IBuildObject invokeConverter(ManagedBuildInfo bi, IBuildObject buildObject,
			IConfigurationElement element) {
		//
		// if (element != null) {
		// IConvertManagedBuildObject convertBuildObject = null;
		// String toId = element.getAttribute("toId"); //$NON-NLS-1$
		// String fromId = element.getAttribute("fromId"); //$NON-NLS-1$
		//
		// try {
		// convertBuildObject = (IConvertManagedBuildObject)
		// element.createExecutableExtension("class"); //$NON-NLS-1$
		// } catch (CoreException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		//
		// if (convertBuildObject != null) {
		// // invoke the converter
		// IProject prj = null;
		// IBuildObject result = null;
		// try {
		// if (bi != null) {
		// prj = (IProject) bi.getManagedProject().getOwner();
		// UpdateManagedProjectManager.addInfo(prj, bi);
		// }
		// result = convertBuildObject.convert(buildObject, fromId, toId, false);
		// } finally {
		// if (bi != null)
		// UpdateManagedProjectManager.delInfo(prj);
		// }
		// return result;
		// }
		// }
		// // if control comes here, it means that either 'convertBuildObject' is null
		// or
		// // converter did not convert the object successfully
		return null;
	}


	/**
	 * Generic routine for checking the availability of converters for the given
	 * Build Object.
	 *
	 * @return true if there are converters for the given Build Object. Returns
	 *         false if there are no converters.
	 */
	public static boolean hasTargetConversionElements(IBuildObject buildObj) {

		// Get the Converter Extension Point
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(
				"org.eclipse.cdt.managedbuilder.core", //$NON-NLS-1$
				"projectConverter"); //$NON-NLS-1$
		if (extensionPoint != null) {
			// Get the extensions
			IExtension[] extensions = extensionPoint.getExtensions();
			for (IExtension extension : extensions) {
				// Get the configuration elements of each extension
				IConfigurationElement[] configElements = extension.getConfigurationElements();
				for (IConfigurationElement element : configElements) {
					if (element.getName().equals("converter") //$NON-NLS-1$
							&& (isBuildObjectApplicableForConversion(buildObj, element) == true))
						return true;
				}
			}
		}
		return false;
	}

	/*
	 * Generic function for getting the list of converters for the given Build
	 * Object
	 */

	public static Map<String, IConfigurationElement> getConversionElements(IBuildObject buildObj) {

		Map<String, IConfigurationElement> conversionTargets = new HashMap<>();

		// Get the Converter Extension Point
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(
				"org.eclipse.cdt.managedbuilder.core", //$NON-NLS-1$
				"projectConverter"); //$NON-NLS-1$
		if (extensionPoint != null) {
			// Get the extensions
			IExtension[] extensions = extensionPoint.getExtensions();
			for (IExtension extension : extensions) {
				// Get the configuration elements of each extension
				IConfigurationElement[] configElements = extension.getConfigurationElements();
				for (IConfigurationElement element : configElements) {
					if (element.getName().equals("converter") //$NON-NLS-1$
							&& (isBuildObjectApplicableForConversion(buildObj, element) == true)) {
						conversionTargets.put(element.getAttribute("name"), element); //$NON-NLS-1$
					}
				}
			}
		}
		return conversionTargets;
	}

	/*
	 * Generic function that checks whether the given conversion element can be used
	 * to convert the given build object. It returns true if the given build object
	 * is convertable, otherwise it returns false.
	 */

	private static boolean isBuildObjectApplicableForConversion(IBuildObject buildObj, IConfigurationElement element) {

		String id = null;
		String fromId = element.getAttribute("fromId"); //$NON-NLS-1$

		// Check whether the current converter can be used for conversion

		if (buildObj instanceof IProjectType) {
			IProjectType projType = (IProjectType) buildObj;

			// Check whether the converter's 'fromId' and the
			// given projType 'id' are equal
			while (projType != null) {
				id = projType.getId();

				if (fromId.equals(id)) {
					return true;
				}
				projType = projType.getSuperClass();
			}
		} else if (buildObj instanceof IToolChain) {
			IToolChain toolChain = (IToolChain) buildObj;

			// Check whether the converter's 'fromId' and the
			// given toolChain 'id' are equal
			while (toolChain != null) {
				id = toolChain.getId();

				if (fromId.equals(id)) {
					return true;
				}
				toolChain = toolChain.getSuperClass();
			}
		} else if (buildObj instanceof ITool) {
			ITool tool = (ITool) buildObj;

			// Check whether the converter's 'fromId' and the
			// given tool 'id' are equal
			while (tool != null) {
				id = tool.getId();

				if (fromId.equals(id)) {
					return true;
				}
				tool = tool.getSuperClass();
			}
		} else if (buildObj instanceof IBuilder) {
			IBuilder builder = (IBuilder) buildObj;

			// Check whether the converter's 'fromId' and the
			// given builder 'id' are equal
			while (builder != null) {
				id = builder.getId();

				if (fromId.equals(id)) {
					return true;
				}
				builder = builder.getSuperClass();
			}
		}
		return false;
	}

	/*
	 * if the suffix is null, then the random number will be appended to the superId
	 */
	static public String calculateChildId(String superId, String suffix) {
		if (suffix == null)
			suffix = Integer.toString(getRandomNumber());

		String version = getVersionFromIdAndVersion(superId);
		if (version != null)
			return ManagedBuildManager.getIdFromIdAndVersion(superId) + "." + suffix + "_" + version; //$NON-NLS-1$ //$NON-NLS-2$
		return superId + "." + suffix; //$NON-NLS-1$
	}

	private static int isInt(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/**
	 * @return base id when the given id was generated by
	 *         {@link #calculateChildId(String, String)}.
	 * @since 8.0
	 */
	public static String calculateBaseId(String id) {
		int index = id.lastIndexOf('.');
		if (index < 0)
			return id;

		String lastSeg = id.substring(index + 1, id.length());
		if (isInt(lastSeg) > 0) {
			String baseId = id.substring(0, index);
			return baseId;
		}
		return getIdFromIdAndVersion(id);
	}

	/**
	 * @return calculated relative path given the full path to a folder and a file
	 */
	public static IPath calculateRelativePath(IPath container, IPath contents) {
		IPath path = contents;
		if (container.isPrefixOf(contents)) {
			path = contents.setDevice(null).removeFirstSegments(container.segmentCount());
		} else {
			String file = null;
			container = container.addTrailingSeparator();
			if (!contents.hasTrailingSeparator()) {
				file = contents.lastSegment();
				contents = contents.removeLastSegments(1);
				contents = contents.addTrailingSeparator();
			}

			IPath prefix = contents;
			for (; prefix.segmentCount() > 0 && !prefix.isPrefixOf(container); prefix = prefix.removeLastSegments(1)) {
			}
			if (prefix.segmentCount() > 0) {
				int diff = container.segmentCount() - prefix.segmentCount();
				StringBuilder buff = new StringBuilder();
				while (diff-- > 0)
					buff.append("../"); //$NON-NLS-1$
				path = new Path(buff.toString()).append(contents.removeFirstSegments(prefix.segmentCount()));
				if (file != null)
					path = path.append(file);
			}
		}
		return path;
	}

	/*
	 * private static IBuildObject getBuildObjectFromDataObject(CDataObject data){
	 * if(data instanceof BuildConfigurationData) return
	 * ((BuildConfigurationData)data).getConfiguration(); else if(data instanceof
	 * BuildFolderData) return ((BuildFolderData)data).getFolderInfo(); else if(data
	 * instanceof BuildFileData) return ((BuildFileData)data).getFileInfo(); return
	 * null; }
	 */
	private static final boolean TEST_CONSISTENCE = false;

	public static IConfiguration getConfigurationForDescription(ICConfigurationDescription cfgDes) {
		return getConfigurationForDescription(cfgDes, TEST_CONSISTENCE);
	}

	private static IConfiguration getConfigurationForDescription(ICConfigurationDescription cfgDes,
			boolean checkConsistance) {
		if (cfgDes == null)
			return null;

		// if (cfgDes instanceof ICMultiConfigDescription) {
		// ICMultiConfigDescription mcd = (ICMultiConfigDescription) cfgDes;
		// ICConfigurationDescription[] cfds = (ICConfigurationDescription[])
		// mcd.getItems();
		// return new MultiConfiguration(cfds);
		// }

		// CConfigurationData cfgData = cfgDes.getConfigurationData();
		// if (cfgData instanceof BuildConfigurationData) {
		// IConfiguration cfg = ((BuildConfigurationData) cfgData).getConfiguration();
		// if (checkConsistance) {
		// if (cfgDes != getDescriptionForConfiguration(cfg, false)) {
		// throw new IllegalStateException();
		// }
		// }
		// return cfg;
		// }
		return null;
	}

	/**
	 * Convert the IOption integer type ID to the {@link ICSettingEntry#getKind()}
	 * type ID
	 * 
	 * @param type {@link IOption#getValueType()}
	 * @return ICSettingEntry type
	 */
	public static int optionTypeToEntryKind(int type) {
		switch (type) {
		case IOption.INCLUDE_PATH:
			return ICSettingEntry.INCLUDE_PATH;
		case IOption.PREPROCESSOR_SYMBOLS:
			return ICSettingEntry.MACRO;
		case IOption.INCLUDE_FILES:
			return ICSettingEntry.INCLUDE_FILE;
		case IOption.LIBRARY_PATHS:
			return ICSettingEntry.LIBRARY_PATH;
		case IOption.LIBRARIES:
		case IOption.LIBRARY_FILES:
			return ICSettingEntry.LIBRARY_FILE;
		case IOption.MACRO_FILES:
			return ICSettingEntry.MACRO_FILE;
		}
		return 0;
	}

	/**
	 * Convert the IOption integer type ID to the {@link ICSettingEntry#getKind()}
	 * type ID
	 * 
	 * @param type {@link IOption#getValueType()}
	 * @return ICSettingEntry type
	 */
	public static int optionUndefTypeToEntryKind(int type) {
		switch (type) {
		case IOption.UNDEF_INCLUDE_PATH:
			return ICSettingEntry.INCLUDE_PATH;
		case IOption.UNDEF_PREPROCESSOR_SYMBOLS:
			return ICSettingEntry.MACRO;
		case IOption.UNDEF_INCLUDE_FILES:
			return ICSettingEntry.INCLUDE_FILE;
		case IOption.UNDEF_LIBRARY_PATHS:
			return ICSettingEntry.LIBRARY_PATH;
		case IOption.UNDEF_LIBRARY_FILES:
			return ICSettingEntry.LIBRARY_FILE;
		case IOption.UNDEF_MACRO_FILES:
			return ICSettingEntry.MACRO_FILE;
		}
		return 0;
	}

	public static int entryKindToOptionType(int kind) {
		switch (kind) {
		case ICSettingEntry.INCLUDE_PATH:
			return IOption.INCLUDE_PATH;
		case ICSettingEntry.INCLUDE_FILE:
			return IOption.INCLUDE_FILES;
		case ICSettingEntry.MACRO:
			return IOption.PREPROCESSOR_SYMBOLS;
		case ICSettingEntry.MACRO_FILE:
			return IOption.MACRO_FILES;
		case ICSettingEntry.LIBRARY_PATH:
			return IOption.LIBRARY_PATHS;// TODO IOption.LIBRARIES;
		case ICSettingEntry.LIBRARY_FILE:
			return IOption.LIBRARY_FILES;
		}
		return 0;
	}

	public static int entryKindToUndefOptionType(int kind) {
		switch (kind) {
		case ICSettingEntry.INCLUDE_PATH:
			return IOption.UNDEF_INCLUDE_PATH;
		case ICSettingEntry.INCLUDE_FILE:
			return IOption.UNDEF_INCLUDE_FILES;
		case ICSettingEntry.MACRO:
			return IOption.UNDEF_PREPROCESSOR_SYMBOLS;
		case ICSettingEntry.MACRO_FILE:
			return IOption.UNDEF_MACRO_FILES;
		case ICSettingEntry.LIBRARY_PATH:
			return IOption.UNDEF_LIBRARY_PATHS;// TODO IOption.LIBRARIES;
		case ICSettingEntry.LIBRARY_FILE:
			return IOption.UNDEF_LIBRARY_FILES;
		}
		return 0;
	}

	public static ICConfigurationDescription getDescriptionForConfiguration(IConfiguration cfg) {
		return getDescriptionForConfiguration(cfg, TEST_CONSISTENCE);
	}

	private static ICConfigurationDescription getDescriptionForConfiguration(IConfiguration cfg,
			boolean checkConsistance) {
		if (cfg.isExtensionElement())
			return null;
		ICConfigurationDescription des = ((Configuration) cfg).getConfigurationDescription();
		if (des == null) {
			if (checkConsistance)
				throw new IllegalStateException();
			if (((Configuration) cfg).isPreference()) {
				try {
					des = CCorePlugin.getDefault().getPreferenceConfiguration(CFG_DATA_PROVIDER_ID);
				} catch (CoreException e) {
					Activator.log(e);
				}
			} else {
				IProject project = cfg.getOwner().getProject();
				ICProjectDescription projDes = CoreModel.getDefault().getProjectDescription(project, false);
				if (projDes != null) {
					des = projDes.getConfigurationById(cfg.getId());
				}
			}
		}
		if (checkConsistance) {
			if (cfg != getConfigurationForDescription(des, false)) {
				throw new IllegalStateException();
			}
		}
		return des;
	}

	/**
	 * Returns a string representing the workspace relative path with
	 * ${workspace_loc: stripped or null if the String path doesn't contain
	 * workspace_loc
	 * 
	 * @param path String path to have workspace_loc removed
	 * @return workspace path or null
	 *
	 * @deprecated as of CDT 8.3. This method is useless as API as it does something
	 *             very specfic to {@link BuildEntryStorage}. It was moved there as
	 *             private method {@link BuildEntryStorage#locationToFullPath}.
	 */
	@Deprecated
	public static String locationToFullPath(String path) {
		Assert.isLegal(false, "Do not use this method"); //$NON-NLS-1$
		return null;
	}

	public static String fullPathToLocation(String path) {
		StringBuilder buf = new StringBuilder();
		return buf.append("${").append("workspace_loc:").append(path).append("}").toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public static IFolder getBuildFolder(IConfiguration cfg, IBuilder builder) {
		if (cfg.getOwner() == null) {
			// This should never happen
			return null;
		}

		IProject project = cfg.getOwner().getProject();
		return project.getFolder(cfg.getName());
//        IPath buildDirectory = builder.getBuildLocation();
//        if (buildDirectory != null && !buildDirectory.isEmpty()) {
//            IResource res = project.getParent().findMember(buildDirectory);
//            if (res instanceof IContainer && res.exists()) {
//                buildDirectory = res.getLocation();
//            }
//        } else {
//            buildDirectory = getPathForResource(project);
//
//            if (buildDirectory != null) {
//                if (builder.isManagedBuildOn())
//                    buildDirectory = buildDirectory.append(cfg.getName());
//            }
//        }
//        return buildDirectory;
	}

	// public static IBuilder[] createBuilders(IProject project, Map<String, String>
	// args) {
	// return ManagedBuilderCorePlugin.createBuilders(project, args);
	// }
	//
	// public static IBuilder createCustomBuilder(IConfiguration cfg, String
	// builderId) throws CoreException {
	// return ManagedBuilderCorePlugin.createCustomBuilder(cfg, builderId);
	// }
	//
	// public static IBuilder createCustomBuilder(IConfiguration cfg, IBuilder base)
	// {
	// return ManagedBuilderCorePlugin.createCustomBuilder(cfg, base);
	// }
	//
	// public static IBuilder createBuilderForEclipseBuilder(IConfiguration cfg,
	// String eclipseBuilderID)
	// throws CoreException {
	// return ManagedBuilderCorePlugin.createBuilderForEclipseBuilder(cfg,
	// eclipseBuilderID);
	// }

	/*
	 * public static IToolChain[] getExtensionsToolChains(String propertyType,
	 * String propertyValue){ List all = getSortedToolChains(); List result = new
	 * ArrayList(); for(int i = 0; i < all.size(); i++){ List list =
	 * (List)all.get(i); IToolChain tc = findToolChain(list, propertyType,
	 * propertyValue); if(tc != null) result.add(tc); } return
	 * (IToolChain[])result.toArray(new ToolChain[result.size()]); }
	 */
	/*
	 * public static void resortToolChains(){ sortedToolChains = null;
	 * getSortedToolChains(); }
	 */
	/*
	 * private static List getSortedToolChains(){ if(sortedToolChains == null){
	 * sortedToolChains = new ArrayList(); SortedMap map =
	 * getExtensionToolChainMapInternal(); for(Iterator iter =
	 * map.values().iterator(); iter.hasNext();){ ToolChain tc =
	 * (ToolChain)iter.next(); if(tc.isAbstract()) continue; List list =
	 * searchIdentical(sortedToolChains, tc); if(list == null){ list = new
	 * ArrayList(); sortedToolChains.add(list); } list.add(tc);
	 * tc.setIdenticalList(list); } } return sortedToolChains; }
	 */
	// private static List findIdenticalToolChains(IToolChain tc){
	// ToolChain tCh = (ToolChain)tc;
	// List list = tCh.getIdenticalList();
	// if(list == null){
	// resortToolChains();
	// list = tCh.getIdenticalList();
	// if(list == null){
	// list = new ArrayList(0);
	// tCh.setIdenticalList(list);
	// }
	// }
	//
	// return ((ToolChain)tc).getIdenticalList();
	// }

	public static IToolChain[] getExtensionToolChains(IProjectType type) {
		return null;
		// List<IToolChain> result = new ArrayList<>();
		// IConfiguration cfgs[] = type.getConfigurations();
		//
		// for (IConfiguration cfg : cfgs) {
		// IToolChain tc = cfg.getToolChain();
		// if (tc == null)
		// continue;
		//
		// List<IToolChain> list = findIdenticalElements((ToolChain) tc,
		// fToolChainSorter);
		// int k = 0;
		// for (; k < result.size(); k++) {
		// if (findIdenticalElements((ToolChain) result.get(k), fToolChainSorter) ==
		// list)
		// break;
		// }
		//
		// if (k == result.size()) {
		// result.add(tc);
		// }
		// }
		// return result.toArray(new IToolChain[result.size()]);
	}

	public static IConfiguration[] getExtensionConfigurations(IToolChain tChain, IProjectType type) {
		return null;
		// List<IConfiguration> list = new ArrayList<>();
		// IConfiguration cfgs[] = type.getConfigurations();
		// for (IConfiguration cfg : cfgs) {
		// IToolChain cur = cfg.getToolChain();
		// if (cur != null && findIdenticalElements((ToolChain) cur,
		// fToolChainSorter) == findIdenticalElements((ToolChain) tChain,
		// fToolChainSorter)) {
		// list.add(cfg);
		// }
		// }
		// return list.toArray(new Configuration[list.size()]);
	}

	public static IConfiguration getFirstExtensionConfiguration(IToolChain tChain) {
		if (tChain.getParent() != null)
			return tChain.getParent();

		// List<ToolChain> list = findIdenticalElements((ToolChain) tChain,
		// fToolChainSorter);
		// if (list != null) {
		// for (int i = 0; i < list.size(); i++) {
		// ToolChain cur = list.get(i);
		// if (cur.getParent() != null)
		// return cur.getParent();
		// }
		// }

		return null;
	}

	public static IConfiguration[] getExtensionConfigurations(IToolChain tChain, String propertyType,
			String propertyValue) {
		// List all = getSortedToolChains();
		// List<ToolChain> list = findIdenticalElements((ToolChain) tChain,
		// fToolChainSorter);
		// LinkedHashSet<IConfiguration> result = new LinkedHashSet<>();
		// boolean tcFound = false;
		// if (list != null) {
		// for (int i = 0; i < list.size(); i++) {
		// ToolChain cur = list.get(i);
		// if (cur == tChain) {
		// tcFound = true;
		// }
		//
		// IConfiguration cfg = cur.getParent();
		// if (cfg != null) {
		// IBuildObjectProperties props = cfg.getBuildProperties();
		// if (props.containsValue(propertyType, propertyValue)) {
		// result.add(cfg);
		// }
		// }
		// }
		//
		// }
		//
		// if (!tcFound) {
		// IConfiguration cfg = tChain.getParent();
		// if (cfg != null) {
		// IBuildObjectProperties props = cfg.getBuildProperties();
		// if (props.containsValue(propertyType, propertyValue)) {
		// result.add(cfg);
		// }
		// }
		// }
		//
		// // if(result.size() == 0){
		// // if(((ToolChain)tChain).supportsValue(propertyType, propertyValue)){
		// // IConfiguration cfg = getFirstExtensionConfiguration(tChain);
		// // if(cfg != null){
		// // result.add(cfg);
		// // }
		// // }
		// // }
		// return result.toArray(new IConfiguration[result.size()]);
		return null;
	}

	/*
	 * public static IToolChain[] getRealToolChains(){ List all =
	 * getSortedToolChains(); IToolChain tcs[] = new ToolChain[all.size()]; for(int
	 * i = 0; i < tcs.length; i++){ List list = (List)all.get(i); tcs[i] =
	 * (ToolChain)list.get(0); } return tcs; }
	 */

	private static HashSet<IToolChain> getSortedToolChains() {
		if (fSortedToolChains == null) {
			Collection<IToolChain> toolChains = getExtensionToolChainMapInternal().values();
			fSortedToolChains = new HashSet<>(toolChains);
		}
		return fSortedToolChains;
	}

	private static HashSet<ITool> getSortedTools() {
		if (fSortedTools == null) {
			Collection<Tool> tools = getExtensionToolMapInternal().values();
			fSortedTools = new HashSet<>(tools);
		}
		return fSortedTools;
	}

	private static HashSet<IBuilder> getSortedBuilders() {
		if (fSortedBuilders == null) {
			Collection<IBuilder> builders = getExtensionBuilderMapInternal().values();
			fSortedBuilders = new HashSet<>(builders);
		}
		return fSortedBuilders;
	}

	// public static IToolChain[] getRealToolChains() {
	// HashMap<MatchKey<ToolChain>, List<ToolChain>> map = getSortedToolChains();
	// IToolChain tcs[] = new ToolChain[map.size()];
	// int i = 0;
	// for (List<ToolChain> list : map.values()) {
	// tcs[i++] = list.get(0);
	// }
	// return tcs;
	// }

	// public static ITool[] getRealTools() {
	// HashMap<MatchKey<Tool>, List<Tool>> map = getSortedTools();
	// Tool ts[] = new Tool[map.size()];
	// int i = 0;
	// for (List<Tool> list : map.values()) {
	// ts[i++] = list.get(0);
	// }
	// return ts;
	// }

	// public static IBuilder[] getRealBuilders() {
	// HashMap<MatchKey<Builder>, List<Builder>> map = getSortedBuilders();
	// IBuilder bs[] = new Builder[map.size()];
	// int i = 0;
	// for (List<Builder> list : map.values()) {
	// bs[i++] = list.get(0);
	// }
	// return bs;
	// }

	public static IBuilder getRealBuilder(IBuilder builder) {
		IBuilder extBuilder = builder;
		IBuilder realBuilder = null;
		for (; extBuilder != null && !extBuilder.isExtensionElement(); extBuilder = extBuilder.getSuperClass()) {
			// empty body
		}

		// if (extBuilder != null) {
		// List<Builder> list = findIdenticalElements((Builder) extBuilder,
		// fBuilderSorter);
		// if (list.size() == 0) {
		realBuilder = extBuilder;
		// } else {
		// for (IBuilder realBldr : getRealBuilders()) {
		// List<Builder> rList = findIdenticalElements((Builder) realBldr,
		// fBuilderSorter);
		// if (rList == list) {
		// realBuilder = realBldr;
		// break;
		// }
		// }
		// }
		// } else {
		// //TODO:
		// }
		return realBuilder;
	}

	public static ITool getRealTool(ITool tool) {
		if (tool == null)
			return null;
		ITool extTool = tool;
		ITool realTool = null;
		for (; extTool != null && !extTool.isExtensionElement(); extTool = extTool.getSuperClass()) {
			// empty body
		}
		//
		// if (extTool != null) {
		// List<Tool> list = findIdenticalElements((Tool) extTool, fToolSorter);
		// if (list.size() == 0) {
		realTool = extTool;
		// } else {
		// for (ITool realT : getRealTools()) {
		// List<Tool> rList = findIdenticalElements((Tool) realT, fToolSorter);
		// if (rList == list) {
		// realTool = realT;
		// break;
		// }
		// }
		// }
		// } else {
		// realTool = getExtensionTool(Tool.DEFAULT_TOOL_ID);
		// }
		return realTool;
	}

	public static IToolChain getExtensionToolChain(IToolChain tc) {
		IToolChain extTc = tc;
		for (; extTc != null && !extTc.isExtensionElement(); extTc = extTc.getSuperClass()) {
			// empty body
		}
		return extTc;
	}

	public static IToolChain getRealToolChain(IToolChain tc) {
		IToolChain extTc = tc;
		IToolChain realToolChain = null;
		for (; extTc != null && !extTc.isExtensionElement(); extTc = extTc.getSuperClass()) {
			// empty body
		}
		//
		// if (extTc != null) {
		// List<ToolChain> list = findIdenticalElements((ToolChain) extTc,
		// fToolChainSorter);
		// if (list.size() == 0) {
		realToolChain = extTc;
		// } else {
		// for (IToolChain realTc : getRealToolChains()) {
		// List<ToolChain> rList = findIdenticalElements((ToolChain) realTc,
		// fToolChainSorter);
		// if (rList == list) {
		// realToolChain = realTc;
		// break;
		// }
		// }
		// }
		// } else {
		// //TODO:
		// }
		return realToolChain;
	}

	public static void resortToolChains() {
		fSortedToolChains = null;
		getSortedToolChains();
	}

	public static void resortTools() {
		fSortedTools = null;
		getSortedTools();
	}

	public static void resortBuilders() {
		fSortedBuilders = null;
		getSortedBuilders();
	}

	public static IBuildPropertyManager getBuildPropertyManager() {
		return BuildPropertyManager.getInstance();
	}

	/**
	 * Returns the configurations referenced by this configuration. Returns an empty
	 * array if there are no referenced configurations.
	 *
	 * @see CoreModelUtil#getReferencedConfigurationDescriptions(ICConfigurationDescription,
	 *      boolean)
	 * @return an array of IConfiguration objects referenced by this IConfiguration
	 */
	public static IConfiguration[] getReferencedConfigurations(IConfiguration config) {
		ICConfigurationDescription cfgDes = getDescriptionForConfiguration(config);
		if (cfgDes != null) {
			ICConfigurationDescription[] descs = CoreModelUtil.getReferencedConfigurationDescriptions(cfgDes, false);
			List<IConfiguration> result = new ArrayList<>();
			for (ICConfigurationDescription desc : descs) {
				IConfiguration cfg = getConfigurationForDescription(desc);
				if (cfg != null) {
					result.add(cfg);
				}
			}
			return result.toArray(new IConfiguration[result.size()]);
		}

		return new Configuration[0];
	}

	/**
	 * Build the specified build configurations
	 * 
	 * @param configs - configurations to build
	 * @param monitor - progress monitor
	 */
	public static void buildConfigurations(IConfiguration[] configs, IProgressMonitor monitor) throws CoreException {
		buildConfigurations(configs, null, monitor);
	}

	/**
	 * Build the specified build configurations
	 * 
	 * @param configs - configurations to build
	 * @param builder - builder to retrieve build arguments
	 * @param monitor - progress monitor
	 */
	public static void buildConfigurations(IConfiguration[] configs, IBuilder builder, IProgressMonitor monitor)
			throws CoreException {
		buildConfigurations(configs, builder, monitor, true);
	}

	/**
	 * Build the specified build configurations.
	 *
	 * @param configs     - configurations to build
	 * @param builder     - builder to retrieve build arguments
	 * @param monitor     - progress monitor
	 * @param allBuilders - {@code true} if all builders need to be building or
	 *                    {@code false} to build with {@link CommonBuilder}
	 */
	public static void buildConfigurations(IConfiguration[] configs, IBuilder builder, IProgressMonitor monitor,
			boolean allBuilders) throws CoreException {
		buildConfigurations(configs, builder, monitor, allBuilders, IncrementalProjectBuilder.FULL_BUILD);
	}

	/**
	 * Build the specified build configurations.
	 *
	 * @param configs     - configurations to build
	 * @param builder     - builder to retrieve build arguments
	 * @param monitor     - progress monitor
	 * @param allBuilders - {@code true} if all builders need to be building or
	 *                    {@code false} to build with {@link CommonBuilder}
	 * @param buildKind   - one of
	 *                    <li>{@link IncrementalProjectBuilder#CLEAN_BUILD}</li>
	 *                    <li>{@link IncrementalProjectBuilder#INCREMENTAL_BUILD}</li>
	 *                    <li>{@link IncrementalProjectBuilder#FULL_BUILD}</li>
	 *
	 * @since 7.0
	 */
	public static void buildConfigurations(IConfiguration[] configs, IBuilder builder, IProgressMonitor monitor,
			boolean allBuilders, int buildKind) throws CoreException {

		Map<IProject, IConfiguration[]> map = sortConfigs(configs);
		for (Entry<IProject, IConfiguration[]> entry : map.entrySet()) {
			IProject proj = entry.getKey();
			IConfiguration[] cfgs = entry.getValue();
			buildConfigurations(proj, cfgs, builder, monitor, allBuilders, buildKind);
		}
	}

	private static Map<IProject, IConfiguration[]> sortConfigs(IConfiguration cfgs[]) {
		Map<IProject, Set<IConfiguration>> cfgSetMap = new HashMap<>();
		for (IConfiguration cfg : cfgs) {
			IProject proj = cfg.getOwner().getProject();
			Set<IConfiguration> set = cfgSetMap.get(proj);
			if (set == null) {
				set = new HashSet<>();
				cfgSetMap.put(proj, set);
			}
			set.add(cfg);
		}

		Map<IProject, IConfiguration[]> cfgArrayMap = new HashMap<>();
		if (cfgSetMap.size() != 0) {
			Set<Entry<IProject, Set<IConfiguration>>> entrySet = cfgSetMap.entrySet();
			for (Entry<IProject, Set<IConfiguration>> entry : entrySet) {
				IProject key = entry.getKey();
				Set<IConfiguration> set = entry.getValue();
				cfgArrayMap.put(key, set.toArray(new Configuration[set.size()]));
			}
		}

		return cfgArrayMap;
	}

	/**
	 * Build the specified build configurations for a given project.
	 *
	 * @param project     - project the configurations belong to
	 * @param configs     - configurations to build
	 * @param builder     - builder to retrieve build arguments
	 * @param monitor     - progress monitor
	 * @param allBuilders - {@code true} if all builders need to be building or
	 *                    {@code false} to build with {@link CommonBuilder}
	 * @param buildKind   - one of
	 *                    <li>{@link IncrementalProjectBuilder#CLEAN_BUILD}</li>
	 *                    <li>{@link IncrementalProjectBuilder#INCREMENTAL_BUILD}</li>
	 *                    <li>{@link IncrementalProjectBuilder#FULL_BUILD}</li>
	 *
	 * @throws CoreException
	 */
	private static void buildConfigurations(final IProject project, final IConfiguration[] configs,
			final IBuilder builder, final IProgressMonitor monitor, final boolean allBuilders, final int buildKind)
			throws CoreException {

		IWorkspaceRunnable op = new IWorkspaceRunnable() {
			/*
			 * (non-Javadoc)
			 *
			 * @see
			 * org.eclipse.core.resources.IWorkspaceRunnable#run(org.eclipse.core.runtime.
			 * IProgressMonitor)
			 */
			@Override
			public void run(IProgressMonitor monitor) throws CoreException {
				int ticks = 1;
				if (buildKind == IncrementalProjectBuilder.CLEAN_BUILD) {
					if (allBuilders) {
						ICommand[] commands = project.getDescription().getBuildSpec();
						ticks = commands.length;
					}
					ticks = ticks * configs.length;
				}
				monitor.beginTask(project.getName(), ticks);

				if (buildKind == IncrementalProjectBuilder.CLEAN_BUILD) {
					// It is not possible to pass arguments to clean() method of a builder
					// So we iterate setting active configuration
					IManagedBuildInfo buildInfo = ManagedBuildManager.getBuildInfo(project);
					IConfiguration savedCfg = buildInfo.getDefaultConfiguration();

					try {
						for (IConfiguration config : configs) {
							if (monitor.isCanceled())
								break;

							buildInfo.setDefaultConfiguration(config);
							buildProject(project, null, allBuilders, buildKind, monitor);
						}
					} finally {
						buildInfo.setDefaultConfiguration(savedCfg);
					}
					// } else {
					// // configuration IDs are passed in args to CDT builder
					// Map<String, String> args = builder != null ?
					// BuilderFactory.createBuildArgs(configs, builder)
					// : BuilderFactory.createBuildArgs(configs);
					// buildProject(project, args, allBuilders, buildKind, monitor);
				}

				monitor.done();
			}

			private void buildProject(IProject project, Map<String, String> args, boolean allBuilders, int buildKind,
					IProgressMonitor monitor) throws CoreException {

				if (allBuilders) {
					ICommand[] commands = project.getDescription().getBuildSpec();
					for (ICommand command : commands) {
						if (monitor.isCanceled())
							break;

						String builderName = command.getBuilderName();
						Map<String, String> newArgs = null;
						// if (buildKind != IncrementalProjectBuilder.CLEAN_BUILD) {
						// newArgs = new HashMap<>(args);
						// if (!builderName.equals(CommonBuilder.BUILDER_ID)) {
						// newArgs.putAll(command.getArguments());
						// }
						// }
						project.build(buildKind, builderName, newArgs, new SubProgressMonitor(monitor, 1));
					}
				} else {
					// project.build(buildKind, CommonBuilder.BUILDER_ID, args, new
					// SubProgressMonitor(monitor, 1));
				}
			}
		};

		try {
			ResourcesPlugin.getWorkspace().run(op, monitor);
		} finally {
			monitor.done();
		}
	}

	public static IBuilder getInternalBuilder() {
		return getExtensionBuilder(INTERNAL_BUILDER_ID);
	}

	public static ITool getExtensionTool(ITool tool) {
		ITool extTool = tool;
		for (; extTool != null && !extTool.isExtensionElement(); extTool = extTool.getSuperClass()) {
		}
		return extTool;
	}

	public static IInputType getExtensionInputType(IInputType inType) {
		IInputType extIT = inType;
		for (; extIT != null; extIT = extIT.getSuperClass()) {
		}
		return extIT;
	}

	public static IConfiguration getPreferenceConfiguration(boolean write) {
		try {
			ICConfigurationDescription des = CCorePlugin.getDefault().getPreferenceConfiguration(CFG_DATA_PROVIDER_ID,
					write);
			if (des != null)
				return getConfigurationForDescription(des);
		} catch (CoreException e) {
			Activator.log(e);
		}
		return null;
	}

	/* package */ public static void collectLanguageSettingsConsoleParsers(ICConfigurationDescription cfgDescription,
			IWorkingDirectoryTracker cwdTracker, List<IConsoleParser> parsers) {
		if (cfgDescription instanceof ILanguageSettingsProvidersKeeper) {
			List<ILanguageSettingsProvider> lsProviders = ((ILanguageSettingsProvidersKeeper) cfgDescription)
					.getLanguageSettingProviders();
			for (ILanguageSettingsProvider lsProvider : lsProviders) {
				ILanguageSettingsProvider rawProvider = LanguageSettingsManager.getRawProvider(lsProvider);
				if (rawProvider instanceof ICBuildOutputParser) {
					ICBuildOutputParser consoleParser = (ICBuildOutputParser) rawProvider;
					try {
						consoleParser.startup(cfgDescription, cwdTracker);
						parsers.add(consoleParser);
					} catch (CoreException e) {
						Activator.log(new Status(IStatus.ERROR, Activator.getId(),
								"Language Settings Provider failed to start up", e)); //$NON-NLS-1$
					}
				}
			}
		}
	}
}
