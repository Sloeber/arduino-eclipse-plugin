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
import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.eclipse.cdt.core.AbstractCExtension;
import org.eclipse.cdt.core.IConsoleParser;
import org.eclipse.cdt.core.language.settings.providers.ICBuildOutputParser;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsProvidersKeeper;
import org.eclipse.cdt.core.language.settings.providers.IWorkingDirectoryTracker;
import org.eclipse.cdt.core.language.settings.providers.LanguageSettingsManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import io.sloeber.autoBuild.api.IManagedBuildInfo;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.schema.api.IProjectType;
import io.sloeber.schema.api.ISchemaObject;
import io.sloeber.schema.internal.ProjectType;

/**
 * This is the main entry point for getting at the build information for the
 * managed build system.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ManagedBuildManager extends AbstractCExtension {

    // Error ID's for OptionValidError()
//    private static final int ERROR_CATEGORY = 0;
//    private static final int ERROR_FILTER = 1;
//
//    private static final String BUILD_TYPE_PROPERTY_ID = "org.eclipse.cdt.build.core.buildType"; //$NON-NLS-1$
//    private static final String BUILD_ARTEFACT_TYPE_PROPERTY_ID = "org.eclipse.cdt.build.core.buildArtefactType"; //$NON-NLS-1$
//
//    private static final String BUILD_TYPE_PROPERTY_DEBUG = "org.eclipse.cdt.build.core.buildType.debug"; //$NON-NLS-1$
//    private static final String BUILD_TYPE_PROPERTY_RELEASE = "org.eclipse.cdt.build.core.buildType.release"; //$NON-NLS-1$
//    private static final String BUILD_ARTEFACT_TYPE_PROPERTY_EXE = "org.eclipse.cdt.build.core.buildArtefactType.exe"; //$NON-NLS-1$
//    private static final String BUILD_ARTEFACT_TYPE_PROPERTY_STATICLIB = "org.eclipse.cdt.build.core.buildArtefactType.staticLib"; //$NON-NLS-1$
//    private static final String BUILD_ARTEFACT_TYPE_PROPERTY_SHAREDLIB = "org.eclipse.cdt.build.core.buildArtefactType.sharedLib"; //$NON-NLS-1$
//
//    private static final String INTERNAL_BUILDER_ID = "org.eclipse.cdt.build.core.internal.builder"; //$NON-NLS-1$
    private static boolean VERBOSE = false;

    // Random number for derived object model elements
//    private static Random randomNumber;

    private static Map<IProject, IManagedBuildInfo> fInfoMap = new HashMap<>();

    // The loaded extensions
    private static Map<String, List<ISchemaObject>> myLoadedExtensions = new HashMap<>();
    // List of extension point ID's the autoBuild Supports
    // currently only 1
    private static List<String> supportedExtensionPointIDs = new ArrayList<>();

    static {
        supportedExtensionPointIDs.add("io.sloeber.autoBuild.buildDefinitions"); //$NON-NLS-1$
    }



    @SuppressWarnings("nls")
    public static IProjectType getProjectType(String extensionPointID, String extensionID, String projectTypeID,
            boolean loadIfNeeded) {
        String key = makeKey(extensionPointID, extensionID);
        // verify if it is a valid set of ID's
        if (!supportedExtensionPointIDs.contains(extensionPointID)) {
            System.err.println("extensionpoint support for " + extensionPointID + " is not yet implemented");
        }

        // Try to find the project type
        IProjectType ret = findLoadedProject(key, projectTypeID);
        if (ret == null && loadIfNeeded == false) {
            return ret;
        }
        loadExtension(extensionPointID, extensionID);

        // This projectTypeID project is not yet loaded.
        ret = findLoadedProject(key, projectTypeID);
        if (ret == null) {
            // Error Can not LOAD project extensionPointID extensionID projectTypeID
            System.err.println("Could not find the project with ID " + projectTypeID + " in extension with "
                    + extensionID + " based on extention point with ID " + extensionPointID);
        }
        return ret;
    }

    private static IProjectType findLoadedProject(String key, String projectTypeID) {
        List<ISchemaObject> buildObjects = myLoadedExtensions.get(key);
        if (buildObjects == null) {
            return null;
        }
        for (ISchemaObject curBuildObject : buildObjects) {
            if (curBuildObject instanceof IProjectType) {
                IProjectType curProject = (IProjectType) curBuildObject;
                if (curProject.getId().equals(projectTypeID)) {
                    return curProject;
                }
            }
        }
        return null;
    }

    private static String makeKey(String extensionPointID, String extensionID) {
        return extensionPointID + ';' + extensionID;
    }

    private static void loadExtension(String extensionPointID, String extensionID) {
        try {

            String key = makeKey(extensionPointID, extensionID);
            // Get the extensions that use the current CDT managed build model
            IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(extensionPointID);
            if (extensionPoint != null) {
                IExtension extension = extensionPoint.getExtension(extensionID);
                if (extension != null) {
                    List<ISchemaObject> objects = new LinkedList<>();
                    IConfigurationElement[] elements = extension.getConfigurationElements();
                    for (IConfigurationElement element : elements) {
                        try {
                            if (element.getName().equals(IProjectType.PROJECTTYPE_ELEMENT_NAME)) {
                                ProjectType newProjectType = new ProjectType(extensionPoint, element);
                                objects.add(newProjectType);
                                if (VERBOSE) {
                                    System.out.print(newProjectType.dump(0));
                                }
                            }

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    myLoadedExtensions.put(key, objects);
                }
            }
        } catch (Exception e) {
            Activator.log(e);
        }
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
            Activator.log(e);
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
                    Activator.log(e);
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
                            Activator.log(e);
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
     * If the build info is not currently loaded and "forceLoad" argument is set to
     * true, loads the build info from the .cdtbuild file In case "forceLoad" is
     * false, does not load the build info and returns null in case it is not loaded
     *
     * @param resource
     *            The resource to search for managed build information on.
     * @param forceLoad
     *            specifies whether the build info should be loaded in case it
     *            is not loaded currently.
     * @return IManagedBuildInfo The build information object for the resource.
     */
    public static ManagedBuildInfo getBuildInfo(IResource resource, boolean forceLoad) {
        return findBuildInfo(resource.getProject(), forceLoad);
    }



    public static void outputManifestError(String message) {
        System.err.println(ManagedBuildManager_error_manifest_header + message + NEWLINE);
    }

    public static void outputIconError(String iconLocation) {
        String[] msgs = new String[1];
        msgs[0] = iconLocation;
        ManagedBuildManager.outputManifestError(MessageFormat.format(ManagedBuildManager_error_manifest_icon, msgs));
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

}