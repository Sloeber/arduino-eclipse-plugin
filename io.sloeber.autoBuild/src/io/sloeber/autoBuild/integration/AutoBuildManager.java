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
package io.sloeber.autoBuild.integration;

import static io.sloeber.autoBuild.core.Messages.*;
import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.AbstractCExtension;
import org.eclipse.cdt.core.IConsoleParser;
import org.eclipse.cdt.core.language.settings.providers.ICBuildOutputParser;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.language.settings.providers.IWorkingDirectoryTracker;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;

import io.sloeber.autoBuild.core.Activator;
import io.sloeber.schema.api.IBuilder;
import io.sloeber.schema.api.IProjectType;
import io.sloeber.schema.internal.Builder;
import io.sloeber.schema.internal.ProjectType;

/**
 * This is the main entry point for getting at the build information for the
 * managed build system.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class AutoBuildManager extends AbstractCExtension {

	private static boolean VERBOSE = false;

	// The loaded extensions
	private static Map<String, Map<String, IProjectType>> myLoadedExtensions = new HashMap<>();
	private static Map<String, IBuilder> myBuilders = new HashMap<>();
	// List of extension point ID's the autoBuild Supports
	// currently only 1
	private static List<String> supportedExtensionPointIDs = new ArrayList<>();

	private static Builder myDefaultBuilder;

	static {
		supportedExtensionPointIDs.add("io.sloeber.autoBuild.buildDefinitions"); //$NON-NLS-1$
	}

	public static String[] supportedExtensionPointIDs() {
		return supportedExtensionPointIDs.toArray(new String[supportedExtensionPointIDs.size()]);
	}

	public static IBuilder getBuilder(String builderID) {
		if (myBuilders.size() == 0) {
			LoadBuilders();
		}
		return myBuilders.get(builderID);
	}

	private static void LoadBuilders() {
		for (String extensionPointID : supportedExtensionPointIDs) {
			IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(extensionPointID);
			if (extensionPoint != null) {
				for (IExtension extension : extensionPoint.getExtensions()) {
					for (IConfigurationElement curElement : extension.getConfigurationElements()) {
						if (IBuilder.BUILDER_ELEMENT_NAME.equals(curElement.getName())) {
							Builder newBuilder = new Builder(extensionPoint, curElement);
							myBuilders.put(newBuilder.getId(), newBuilder);
							myDefaultBuilder=newBuilder;
						}
					}
				}
			}
		}
	}

	@SuppressWarnings("nls")
	public static IProjectType getProjectType(String extensionPointID, String extensionID, String projectTypeID,
			boolean loadIfNeeded) {
		// verify if it is a valid set of ID's
		if (!supportedExtensionPointIDs.contains(extensionPointID)) {
			System.err.println("extensionpoint support for " + extensionPointID + " is not supported");
		}

		// Try to find the project type
		IProjectType ret = findLoadedProject(extensionPointID, extensionID, projectTypeID);
		if (ret != null || loadIfNeeded == false) {
			return ret;
		}
		loadExtension(extensionPointID, extensionID, projectTypeID);

		// This projectTypeID project is not yet loaded.
		ret = findLoadedProject(extensionPointID, extensionID, projectTypeID);
		if (ret == null) {
			// Error Can not LOAD project extensionPointID extensionID projectTypeID
			System.err.println("Could not find the project with ID " + projectTypeID + " in extension with "
					+ extensionID + " based on extention point with ID " + extensionPointID);
		}
		return ret;
	}

	private static IProjectType findLoadedProject(String extensionPointID, String extensionID, String projectTypeID) {
		String key = makeKey(extensionPointID, extensionID);
		Map<String, IProjectType> projectTypes = myLoadedExtensions.get(key);
		if (projectTypes == null) {
			return null;
		}

		return projectTypes.get(projectTypeID);
	}

	private static String makeKey(String extensionPointID, String extensionID) {
		return extensionPointID + ';' + extensionID;
	}

	private static void loadExtension(String extensionPointID, String extensionID, String projectTypeID) {
		try {

			String key = makeKey(extensionPointID, extensionID);
			// Get the extensions that use the current CDT managed build model
			IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(extensionPointID);
			if (extensionPoint != null) {
				IExtension extension = extensionPoint.getExtension(extensionID);
				if (extension != null) {
					IConfigurationElement[] elements = extension.getConfigurationElements();
					for (IConfigurationElement curElement : elements) {
						try {
							if (PROJECTTYPE_ELEMENT_NAME.equals(curElement.getName())) {
								if (projectTypeID.equals(curElement.getAttribute(ID))) {
									ProjectType newProjectType = new ProjectType(extensionPointID, extensionID,
											extensionPoint, curElement);

									Map<String, IProjectType> objects = myLoadedExtensions.get(key);
									if (objects == null) {
										objects = new HashMap<>();
										myLoadedExtensions.put(key, objects);
									}
									objects.put(projectTypeID, newProjectType);
									if (VERBOSE) {
										System.out.print(newProjectType.dump(0));
									}
									return;
								}
							}
						} catch (Exception ex) {
							ex.printStackTrace();
						}
					}

				}
			}
		} catch (Exception e) {
			Activator.log(e);
		}
	}

	public static void outputManifestError(String message) {
		System.err.println(ManagedBuildManager_error_manifest_header + message + NEWLINE);
	}

	public static void outputIconError(String iconLocation) {
		Object[] msgs = new String[1];
		msgs[0] = iconLocation;
		AutoBuildManager.outputManifestError(MessageFormat.format(ManagedBuildManager_error_manifest_icon, msgs));
	}

	public static void collectLanguageSettingsConsoleParsers(ICConfigurationDescription cfgDescription,
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

	public static String[] getSupportedExtensionIDs(String extensionPointID) {
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(extensionPointID);
		if (extensionPoint == null) {
			return new String[0];
		}
		Set<String> ret = new HashSet<>();
		IExtension extensions[] = extensionPoint.getExtensions();
		for (IExtension extension : extensions) {
			ret.add(extension.getUniqueIdentifier());
		}
		return ret.toArray(new String[ret.size()]);
	}

	public static Map<String, String> getProjectIDs(String extensionPointID, String extensionID) {
		Map<String, String> ret = new HashMap<>();
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(extensionPointID);
		if (extensionPoint == null) {
			return ret;
		}
		IExtension extension = extensionPoint.getExtension(extensionID);
		if (extension == null) {
			return ret;
		}
		IConfigurationElement[] elements = extension.getConfigurationElements();
		if (elements == null) {
			return ret;
		}
		for (IConfigurationElement element : elements) {
			if (element.getName().equals(PROJECTTYPE_ELEMENT_NAME)) {
				ret.put(element.getAttribute(ID), element.getAttribute(NAME));
			}

		}
		return ret;
	}

	public static Set<IProjectType> getProjectTypes(String extensionPointID, String extensionID) {
		Set<IProjectType> ret = new HashSet<>();
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(extensionPointID);
		if (extensionPoint == null) {
			return ret;
		}
		IExtension extension = extensionPoint.getExtension(extensionID);
		if (extension == null) {
			return ret;
		}
		IConfigurationElement[] elements = extension.getConfigurationElements();
		if (elements == null) {
			return ret;
		}
		for (IConfigurationElement element : elements) {
			if (element.getName().equals(PROJECTTYPE_ELEMENT_NAME)) {
				IProjectType projectType = AutoBuildManager.getProjectType(extensionPointID, extensionID,
						element.getAttribute(ID), true);
				if (projectType == null) {
					System.err.println("project not found: extensionPoint ID " + extensionPointID + " and extension ID "
							+ extensionID + " and " + ID);
				} else {
					ret.add(projectType);
				}
			}

		}
		return ret;
	}

	public static IBuilder getDefaultBuilder() {
		if (myBuilders.size() == 0) {
			LoadBuilders();
		}
		return myDefaultBuilder;
		}

	public static Map<String, IBuilder> getBuilders() {
		if (myBuilders.size() == 0) {
			LoadBuilders();
		}
		return myBuilders;
	}
}