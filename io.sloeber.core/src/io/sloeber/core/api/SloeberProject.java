package io.sloeber.core.api;

import static io.sloeber.core.api.Const.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.buildTools.api.IBuildTools;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager;
import io.sloeber.autoBuild.helpers.api.AutoBuildConstants;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.autoBuild.schema.api.IProjectType;
import io.sloeber.core.Activator;
import io.sloeber.core.internal.SloeberConfiguration;
import io.sloeber.core.natures.SloeberNature;

public class SloeberProject extends Common {
    protected static final String CONFIG_NAME_VARIABLE = AutoBuildConstants.CONFIG_NAME_VARIABLE;
	public static String LATEST_EXTENSION_POINT_ID = "io.sloeber.autoBuild.buildDefinitions"; //$NON-NLS-1$
    public static String LATEST_EXTENSION_ID = "io.sloeber.builddef"; //$NON-NLS-1$
    public static String PROJECT_ID = "io.sloeber.core.sketch"; //$NON-NLS-1$
    private static String SLOEBER_BUILD_TOOL_PROVIDER_ID = "io.sloeber.core.arduino.ToolProvider"; //$NON-NLS-1$


    /**
     * convenient method to create project
     *
     * @param proj1Name
     * @param object
     * @param proj1BoardDesc
     * @param codeDesc
     * @param proj1CompileDesc
     * @param otherDesc
     * @param nullProgressMonitor
     * @return
     */
    public static void convertToArduinoProject(IProject project, IProgressMonitor monitor) {
        //        if (project == null) {
        //            return;
        //        }
        //        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        //        IWorkspaceRoot root = workspace.getRoot();
        //        ICoreRunnable runnable = new ICoreRunnable() {
        //            @Override
        //            public void run(IProgressMonitor internalMonitor) throws CoreException {
        //                IndexerController.doNotIndex(project);
        //
        //                try {
        //
        //                    // create a sloeber project
        //                    SloeberProject sloeberProject = new SloeberProject(project);
        //                    CCorePlugin cCorePlugin = CCorePlugin.getDefault();
        //                    ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(project, true);
        //                    upgradeArduinoProject(sloeberProject, prjCDesc);
        //                    if (!sloeberProject.readConfigFromFiles()) {
        //                        sloeberProject.setBoardDescription(RELEASE, new BoardDescription(), false);
        //                        sloeberProject.setCompileDescription(RELEASE, new CompileDescription());
        //                        sloeberProject.setOtherDescription(RELEASE, new OtherDescription());
        //                        // we failed to read from disk so we set up some values
        //                        // faking the stuff is in memory
        //
        //                    }
        //                    String configName = sloeberProject.myBoardDescriptions.keySet().iterator().next();
        //                    BoardDescription boardDescriptor = sloeberProject.getBoardDescription(configName, true);
        //
        //                    // Add the arduino code folders
        //                    List<IPath> addToIncludePath = Helpers.addArduinoCodeToProject(project, boardDescriptor);
        //
        //                    // make the eclipse project a cdt project
        //                    CCorePlugin.getDefault().createCProject(null, project, new NullProgressMonitor(),
        //                            ManagedBuilderCorePlugin.MANAGED_MAKE_PROJECT_ID);
        //
        //                    // add the required natures
        //                    AutoBuildNature.addNature(project, internalMonitor);
        //
        //                    // make the cdt project a managed build project
        //                    IProjectType sloeberProjType = AutoBuildManager.getProjectType("io.sloeber.core.sketch"); //$NON-NLS-1$
        //                    ManagedBuildManager.createBuildInfo(project);
        //                    IManagedProject newProject = ManagedBuildManager.createManagedProject(project, sloeberProjType);
        //                    ManagedBuildManager.setNewProjectVersion(project);
        //                    // Copy over the Sloeber configs
        //                    IConfiguration defaultConfig = null;
        //                    IConfiguration[] configs = sloeberProjType.getConfigurations();
        //                    for (int i = 0; i < configs.length; ++i) {
        //                        IConfiguration curConfig = newProject.createConfiguration(configs[i],
        //                                sloeberProjType.getId() + "." + i); //$NON-NLS-1$
        //                        curConfig.setArtifactName(newProject.getDefaultArtifactName());
        //                        // Make the first configuration the default
        //                        if (i == 0) {
        //                            defaultConfig = curConfig;
        //                        }
        //                    }
        //
        //                    ManagedBuildManager.setDefaultConfiguration(project, defaultConfig);
        //
        //                    ICConfigurationDescription activeConfig = prjCDesc.getActiveConfiguration();
        //
        //                    for (String curConfigName : sloeberProject.myBoardDescriptions.keySet()) {
        //                        ICConfigurationDescription curConfigDesc = prjCDesc.getConfigurationByName(curConfigName);
        //                        if (curConfigDesc == null) {
        //                            String id = CDataUtil.genId(null);
        //                            curConfigDesc = prjCDesc.createConfiguration(id, curConfigName, activeConfig);
        //                        }
        //                        Helpers.addIncludeFolder(curConfigDesc, addToIncludePath, true);
        //
        //                        String curConfigKey = getConfigKey(curConfigDesc);
        //                        sloeberProject.myEnvironmentVariables.put(curConfigKey,
        //                                sloeberProject.getEnvVars(curConfigKey));
        //
        //                    }
        //                    sloeberProject.createSloeberConfigFiles();
        //                    SubMonitor refreshMonitor = SubMonitor.convert(internalMonitor, 3);
        //                    project.refreshLocal(IResource.DEPTH_INFINITE, refreshMonitor);
        //                    cCorePlugin.setProjectDescription(project, prjCDesc, true, null);
        //
        //                } catch (Exception e) {
        //                    Common.log(new Status(IStatus.INFO, io.sloeber.core.Activator.getId(),
        //                            "Project conversion failed: ", e)); //$NON-NLS-1$
        //                }
        //
        //                IndexerController.index(project);
        //            }
        //
        //        };
        //
        //        try {
        //            workspace.run(runnable, root, IWorkspace.AVOID_UPDATE, monitor);
        //        } catch (Exception e) {
        //            Common.log(new Status(IStatus.INFO, io.sloeber.core.Activator.getId(), "Project conversion failed: ", e)); //$NON-NLS-1$
        //        }

    }

    /**
     * convenient method to create project
     *
     * @param proj1Name
     * @param object
     * @param proj1BoardDesc
     * @param codeDesc
     * @param proj1CompileDesc
     * @param otherDesc
     * @param nullProgressMonitor
     * @return
     */
    public static IProject createArduinoProject(String projectName, URI projectURI, BoardDescription boardDescriptor,
            CodeDescription codeDesc, CompileDescription compileDescriptor, IProgressMonitor monitor) {
        return createArduinoProject(projectName, projectURI, boardDescriptor, codeDesc, compileDescriptor,
                new OtherDescription(), monitor);
    }

    /*
     * Method to create a project based on the board
     */
	public static IProject createArduinoProject(String projectName, URI projectURI, BoardDescription boardDescriptor,
			CodeDescription codeDesc, CompileDescription compileDescriptor, OtherDescription otherDesc,
			IProgressMonitor monitor) {

		String realProjectName = makeNameCompileSafe(projectName);

		final IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();
		ICoreRunnable runnable = new ICoreRunnable() {
			@Override
			public void run(IProgressMonitor internalMonitor) throws CoreException {
				IProject newProjectHandle = root.getProject(realProjectName);
				// IndexerController.doNotIndex(newProjectHandle);

				IBuildTools buildTools = IBuildToolsManager.getDefault().getBuildTools(SLOEBER_BUILD_TOOL_PROVIDER_ID,
						realProjectName);
				IProjectType projectType = AutoBuildManager.getProjectType(LATEST_EXTENSION_POINT_ID,
						LATEST_EXTENSION_ID, PROJECT_ID, true);
				newProjectHandle = AutoBuildProject.createProject(realProjectName,projectURI, projectType,
						CCProjectNature.CC_NATURE_ID, codeDesc, buildTools, true, internalMonitor);

				// Add the sketch code
				Set<IArduinoLibraryVersion> librariesToAdd = codeDesc.getNeededLibraries();

				SloeberNature.addNature(newProjectHandle, internalMonitor);

				CCorePlugin cCorePlugin = CCorePlugin.getDefault();
				ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(newProjectHandle, true);

				for (ICConfigurationDescription curConfig : prjCDesc.getConfigurations()) {
					ICSourceEntry[] orgSourceEntries = curConfig.getSourceEntries();
					ICSourceEntry[] newSourceEntries = new ICSourceEntry[orgSourceEntries.length + 1];
					for (int index = 0; index < orgSourceEntries.length; index++) {
						newSourceEntries[index+1] = orgSourceEntries[index];
					}
					IPath excludes[] = new IPath[8];
					excludes[0] = IPath.fromOSString("**/*.ino"); //$NON-NLS-1$
					excludes[1] = IPath.fromOSString("libraries/?*/**/doc*/**"); //$NON-NLS-1$
					excludes[2] = IPath.fromOSString("libraries/?*/**/?xamples/**"); //$NON-NLS-1$
					excludes[3] = IPath.fromOSString("libraries/?*/**/?xtras/**"); //$NON-NLS-1$
					excludes[4] = IPath.fromOSString("libraries/?*/**/test*/**"); //$NON-NLS-1$
					excludes[5] = IPath.fromOSString("libraries/?*/**/third-party/**"); //$NON-NLS-1$
					excludes[6] = IPath.fromOSString("libraries/**/._*"); //$NON-NLS-1$
					excludes[7] = IPath.fromOSString("libraries/?*/utility/*/*"); //$NON-NLS-1$

					//IPath arduinoRoot = IPath.fromOSString(SLOEBER_ARDUINO_FOLDER_NAME).append(CONFIG_NAME_VARIABLE);
					IPath arduinoRoot = IPath.fromOSString(SLOEBER_ARDUINO_FOLDER_NAME).append(curConfig.getName());
					newSourceEntries[0] = new CSourceEntry(arduinoRoot, excludes, ICSettingEntry.NONE);
					curConfig.setSourceEntries(newSourceEntries);
					IAutoBuildConfigurationDescription iAutoBuildConfig = IAutoBuildConfigurationDescription
							.getConfig(curConfig);
					if (!(iAutoBuildConfig instanceof AutoBuildConfigurationDescription)) {
						// this should not happen as we just created a autoBuild project
						Common.log(new Status(SLOEBER_STATUS_DEBUG, Activator.getId(),
								"\"Auto build created a project that does not seem to be a autobuild project :-s : " //$NON-NLS-1$
										+ realProjectName));
						continue;
					}
					AutoBuildConfigurationDescription autoBuildConfig = (AutoBuildConfigurationDescription) iAutoBuildConfig;
					autoBuildConfig.setIsParallelBuild(compileDescriptor.isParallelBuildEnabled());
					SloeberConfiguration sloeberConfiguration = new SloeberConfiguration(boardDescriptor, otherDesc,
							compileDescriptor);
					autoBuildConfig.setAutoBuildConfigurationExtensionDescription(sloeberConfiguration);
					sloeberConfiguration.addLibraries(librariesToAdd);
				}

				SubMonitor refreshMonitor = SubMonitor.convert(internalMonitor, 3);
				newProjectHandle.open(refreshMonitor);
				newProjectHandle.refreshLocal(IResource.DEPTH_INFINITE, refreshMonitor);
				prjCDesc.setCdtProjectCreated();
				cCorePlugin.setProjectDescription(newProjectHandle, prjCDesc, true,
						SubMonitor.convert(internalMonitor, 1));

				Common.log(new Status(SLOEBER_STATUS_DEBUG, Activator.getId(),
						"internal creation of project is done: " + realProjectName)); //$NON-NLS-1$
				// IndexerController.index(newProjectHandle);
			}
		};

		try

		{
			workspace.run(runnable, root, IWorkspace.AVOID_UPDATE, monitor);
		} catch (Exception e) {
			Common.log(new Status(IStatus.INFO, io.sloeber.core.Activator.getId(),
					"Project creation failed: " + realProjectName, e)); //$NON-NLS-1$
		}
		monitor.done();
		return root.getProject(realProjectName);
	}

    //    private HashMap<String, String> getEnvVars(String configKey) {
    //        BoardDescription boardDescription = myBoardDescriptions.get(configKey);
    //        CompileDescription compileOptions = myCompileDescriptions.get(configKey);
    //        OtherDescription otherOptions = myOtherDescriptions.get(configKey);
    //
    //        HashMap<String, String> allVars = new HashMap<>();
    //
    //        allVars.put(ENV_KEY_BUILD_SOURCE_PATH, myProject.getLocation().toOSString());
    //        allVars.put(ENV_KEY_BUILD_PATH, myProject.getLocation().append(configKey).toOSString());
    //
    //        if (boardDescription != null) {
    //            allVars.putAll(boardDescription.getEnvVars());
    //        }
    //        if (compileOptions != null) {
    //            allVars.putAll(compileOptions.getEnvVars());
    //        }
    //        if (otherOptions != null) {
    //            allVars.putAll(otherOptions.getEnvVars());
    //        }
    //        // set the paths
    //        String pathDelimiter = makeEnvironmentVar("PathDelimiter"); //$NON-NLS-1$
    //        if (Common.isWindows) {
    //            allVars.put(SLOEBER_MAKE_LOCATION,
    //                    ConfigurationPreferences.getMakePath().addTrailingSeparator().toOSString());
    //            allVars.put(SLOEBER_AWK_LOCATION,
    //                    ConfigurationPreferences.getAwkPath().addTrailingSeparator().toOSString());
    //
    //            String systemroot = makeEnvironmentVar("SystemRoot"); //$NON-NLS-1$
    //            allVars.put("PATH", //$NON-NLS-1$
    //                    makeEnvironmentVar(ENV_KEY_COMPILER_PATH) + pathDelimiter
    //                            + makeEnvironmentVar(ENV_KEY_BUILD_GENERIC_PATH) + pathDelimiter + systemroot + "\\system32" //$NON-NLS-1$
    //                            + pathDelimiter + systemroot + pathDelimiter + systemroot + "\\system32\\Wbem" //$NON-NLS-1$
    //                            + pathDelimiter + makeEnvironmentVar("sloeber_path_extension")); //$NON-NLS-1$
    //        } else {
    //            allVars.put("PATH", makeEnvironmentVar(ENV_KEY_COMPILER_PATH) + pathDelimiter //$NON-NLS-1$
    //                    + makeEnvironmentVar(ENV_KEY_BUILD_GENERIC_PATH) + pathDelimiter + makeEnvironmentVar("PATH")); //$NON-NLS-1$
    //        }
    //
    //        return allVars;
    //    }

    /**
     * Read the sloeber configuration file and setup the project
     *
     * @return true if the files exist
     */
    //    private boolean readConfigFromFiles() {
    //        IFile file = getConfigLocalFile();
    //        IFile versionFile = getConfigVersionFile();
    //        if (!(file.exists() || versionFile.exists())) {
    //            // no sloeber files found
    //            return false;
    //        }
    //        if (file.exists()) {
    //            myCfgFile = new TxtFile(file.getLocation().toFile());
    //            if (versionFile.exists()) {
    //                myCfgFile.mergeFile(versionFile.getLocation().toFile());
    //            }
    //        } else {
    //            myCfgFile = new TxtFile(versionFile.getLocation().toFile());
    //        }
    //
    //        KeyValueTree allFileConfigs = myCfgFile.getData().getChild(CONFIG);
    //        for (Entry<String, KeyValueTree> curChild : allFileConfigs.getChildren().entrySet()) {
    //            String curConfName = curChild.getKey();
    //            BoardDescription boardDesc = new BoardDescription(myCfgFile, getBoardPrefix(curConfName));
    //            CompileDescription compileDescription = new CompileDescription(myCfgFile, getCompilePrefix(curConfName));
    //            OtherDescription otherDesc = new OtherDescription(myCfgFile, getOtherPrefix(curConfName));
    //            String curConfKey = curConfName;
    //            myBoardDescriptions.put(curConfKey, boardDesc);
    //            myCompileDescriptions.put(curConfKey, compileDescription);
    //            myOtherDescriptions.put(curConfKey, otherDesc);
    //        }
    //        return true;
    //    }

    /**
     * return the list of configNames known by Sloeber not known by CDT
     *
     * @param prjCDesc
     * @return a list of sloeber known configurationNames unknown to CDT
     */
    //    private List<String> newConfigsNeededInCDT(ICProjectDescription prjCDesc) {
    //        List<String> ret = new LinkedList<>();
    //        for (String curConfName : myBoardDescriptions.keySet()) {
    //            ICConfigurationDescription curConfDesc = prjCDesc.getConfigurationByName(curConfName);
    //            if (curConfDesc == null) {
    //                ret.add(curConfName);
    //            }
    //        }
    //        return ret;
    //
    //    }

//    /**
//     * create the cdt configurations from the list
//     *
//     * @param configs
//     * @param prjCDesc
//     * @return true if at least one config was created (basically only false if
//     *         configs is empty oe error conditions)
//     */
//    private static boolean createNeededCDTConfigs(List<String> configs, ICProjectDescription prjCDesc) {
//        boolean ret = false;
//        for (String curConfName : configs) {
//            try {
//                String id = CDataUtil.genId(null);
//                prjCDesc.createConfiguration(id, curConfName, prjCDesc.getActiveConfiguration());
//                ret = true;
//            } catch (Exception e) {
//                // ignore as we will try again later
//                e.printStackTrace();
//            }
//        }
//        return ret;
//    }

    /**
     * This methods creates/updates 2 files in the workspace. Together these files
     * contain the Sloeber project configuration info The info is split into 2 files
     * because you probably do not want to add all the info to a version control
     * tool.
     *
     * sloeber.cfg is the file you can add to a version control .sproject is the
     * file with settings you do not want to add to version control
     *
     * @param project
     *            the project to store the data for
     */
    //    private void createSloeberConfigFiles() {
    //
    //        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
    //        if (workspace.isTreeLocked()) {
    //            // we cant save now do it later
    //            return;
    //        }
    //
    //        Map<String, String> configVars = new TreeMap<>();
    //        Map<String, String> versionVars = new TreeMap<>();
    //
    //        for (String configKey : myBoardDescriptions.keySet()) {
    //            BoardDescription boardDescription = myBoardDescriptions.get(configKey);
    //            CompileDescription compileDescription = myCompileDescriptions.get(configKey);
    //            OtherDescription otherDescription = myOtherDescriptions.get(configKey);
    //
    //            String boardPrefix = getBoardPrefix(configKey);
    //            String compPrefix = getCompilePrefix(configKey);
    //            String otherPrefix = getOtherPrefix(configKey);
    //
    //            configVars.putAll(boardDescription.getEnvVarsConfig(boardPrefix));
    //            configVars.putAll(compileDescription.getEnvVarsConfig(compPrefix));
    //            configVars.putAll(otherDescription.getEnvVarsConfig(otherPrefix));
    //
    //            if (otherDescription.IsVersionControlled()) {
    //                versionVars.putAll(boardDescription.getEnvVarsVersion(boardPrefix));
    //                versionVars.putAll(compileDescription.getEnvVarsVersion(compPrefix));
    //                versionVars.putAll(otherDescription.getEnvVarsVersion(otherPrefix));
    //            }
    //        }
    //
    //        try {
    //            storeConfigurationFile(getConfigVersionFile(), versionVars);
    //            storeConfigurationFile(getConfigLocalFile(), configVars);
    //        } catch (Exception e) {
    //            Common.log(new Status(IStatus.ERROR, io.sloeber.core.Activator.getId(),
    //                    "failed to save the sloeber config files", e)); //$NON-NLS-1$
    //        }
    //
    //    }

    private static void storeConfigurationFile(IFile file, Map<String, String> vars) throws Exception {
        String content = EMPTY;
        for (Entry<String, String> curLine : vars.entrySet()) {
            content += curLine.getKey() + '=' + curLine.getValue() + '\n';
        }

        if (file.exists()) {
            // if the filecontent hasn't changed=>do nothing
            try {
                Path filePath = Path.of(file.getLocation().toOSString());
                String fileContent = Files.readString(filePath);
                if (content.equals(fileContent)) {
                    return;
                }
            } catch (IOException e) {
                // Don't care as a optimization didn't work
                e.printStackTrace();
            }
            file.delete(true, null);
        }

        if (!file.exists() && (!content.isBlank())) {
            ByteArrayInputStream stream = new ByteArrayInputStream(content.getBytes());
            file.create(stream, true, null);
        }

    }

    //    /**
    //     * get the Arduino project description based on a project description
    //     *
    //     * @param project
    //     * @return the sloeber project or null if this is not a sloeber project
    //     */
    //    public static SloeberProject getSloeberProject(IProject project) {
    //
    //        if (project.isOpen() && project.getLocation().toFile().exists()) {
    //            if (Sketch.isSketch(project)) {
    //                Object sessionProperty = null;
    //                try {
    //                    sessionProperty = project.getSessionProperty(sloeberQualifiedName);
    //                    if (null != sessionProperty) {
    //                        SloeberProject sloeberProject = (SloeberProject) sessionProperty;
    //                        if (sloeberProject.isInMemory()) {
    //                            IndexerController.index(project);
    //                        }
    //                        return sloeberProject;
    //                    }
    //                } catch (CoreException e) {
    //                    e.printStackTrace();
    //                }
    //                return new SloeberProject(project);
    //            }
    //        }
    //        return null;
    //    }

//    private static String getBoardPrefix(String confDescName) {
//        return CONFIG_DOT + confDescName + DOT + "board."; //$NON-NLS-1$
//    }
//
//    private static String getCompilePrefix(String confDescName) {
//        return CONFIG_DOT + confDescName + DOT + "compile."; //$NON-NLS-1$
//    }
//
//    private static String getOtherPrefix(String confDescName) {
//        return CONFIG_DOT + confDescName + DOT + "other."; //$NON-NLS-1$
//    }
//
//    /*
//     * Get the file that Sloeber maintains and that is meant to be stored in version control
//     */
//    private IFile getConfigVersionFile() {
//        return Sketch.getConfigVersionFile(myProject);
//    }
//
//    /*
//     * Get the sloeber configuration file
//     */
//    private IFile getConfigLocalFile() {
//        return Sketch.getConfigLocalFile(myProject);
//    }

    public void configChangeAboutToApply(ICProjectDescription newProjDesc, ICProjectDescription oldProjDesc) {
        //        ICConfigurationDescription newActiveConfig = newProjDesc.getActiveConfiguration();
        //        ICConfigurationDescription oldActiveConfig = oldProjDesc.getActiveConfiguration();
        //
        //        //handle configuration name changes and new configs
        //        for (ICConfigurationDescription curConfig : newProjDesc.getConfigurations()) {
        //            String curConfigKey = getConfigKey(curConfig);
        //            ICConfigurationDescription oldConfig = oldProjDesc.getConfigurationById(curConfig.getId());
        //            if (oldConfig == null) {
        //                //this is a new config
        //                myIsDirty = true;
        //
        //                BoardDescription brdDesc = myBoardDescriptions.get(curConfigKey);
        //                if (brdDesc == null) {
        //                    // This new configuration has not been created in project properties
        //                    // I don't know how to get the "copied from" configuration
        //                    // trying to get something somewhere
        //                    // read: only copy configurations in project properties
        //                    String copyConfKey = getConfigKey(oldActiveConfig);
        //                    brdDesc = myBoardDescriptions.get(copyConfKey);
        //                    if (brdDesc == null) {
        //                        copyConfKey = getConfigKey(oldActiveConfig);
        //                        brdDesc = myBoardDescriptions.get(copyConfKey);
        //                    }
        //                    BoardDescription boardDescription = new BoardDescription(myBoardDescriptions.get(copyConfKey));
        //                    myBoardDescriptions.put(curConfigKey, boardDescription);
        //
        //                    CompileDescription compileDescription = new CompileDescription(
        //                            myCompileDescriptions.get(copyConfKey));
        //                    myCompileDescriptions.put(curConfigKey, compileDescription);
        //
        //                    OtherDescription otherDescription = new OtherDescription(myOtherDescriptions.get(copyConfKey));
        //                    myOtherDescriptions.put(curConfigKey, otherDescription);
        //                }
        //
        //            } else {
        //
        //                String oldConfigKey = getConfigKey(oldConfig);
        //                if (!oldConfigKey.equals(curConfigKey)) {
        //                    //this is a rename
        //                    myIsDirty = true;
        //                    Helpers.deleteBuildFolder(myProject, oldConfig.getName());
        //                    BoardDescription boardDesc = myBoardDescriptions.get(oldConfigKey);
        //                    myBoardDescriptions.remove(oldConfigKey);
        //                    myBoardDescriptions.put(curConfigKey, boardDesc);
        //
        //                    CompileDescription compDesc = myCompileDescriptions.get(oldConfigKey);
        //                    myCompileDescriptions.remove(oldConfigKey);
        //                    myCompileDescriptions.put(curConfigKey, compDesc);
        //
        //                    OtherDescription otherDesc = myOtherDescriptions.get(oldConfigKey);
        //                    myOtherDescriptions.remove(oldConfigKey);
        //                    myOtherDescriptions.put(curConfigKey, otherDesc);
        //
        //                }
        //
        //            }
        //        }
        //
        //        //delete all the deleted configs
        //        for (ICConfigurationDescription curConfig : oldProjDesc.getConfigurations()) {
        //            String curConfigKey = getConfigKey(curConfig);
        //            ICConfigurationDescription newConfig = newProjDesc.getConfigurationById(curConfig.getId());
        //            if (newConfig == null) {
        //                //this is a deleted config
        //                myIsDirty = true;
        //                myBoardDescriptions.remove(curConfigKey);
        //                myCompileDescriptions.remove(curConfigKey);
        //                myOtherDescriptions.remove(curConfigKey);
        //            }
        //        }
        //
        //        configure(newProjDesc, true);

    }

    /**
     * Call this method when the sloeber.cfg file changed
     *
     */
    public void sloeberCfgChanged() {
        //        //CCorePlugin cCorePlugin = CCorePlugin.getDefault();
        //        //ICProjectDescription projDesc = cCorePlugin.getProjectDescription(myProject, true);
        //        ///ICConfigurationDescription activeConfig = projDesc.getActiveConfiguration();
        //        configure();
        //        //all configs may have changed so only deleting the active config does not make sense
        //        //Helpers.deleteBuildFolder(myProject, activeConfig.getName());
        //
        //        //This code is only triggered when sloeber.cfg changed so no need to set the active config
        //        //projDescNeedsSaving = projDescNeedsSaving || setActiveConfig(activeConfig);
        //        //        if (projDescNeedsSaving) {
        //        //            try {
        //        //                cCorePlugin.setProjectDescription(myProject, projDesc);
        //        //            } catch (CoreException e) {
        //        //                e.printStackTrace();
        //        //            }
        //        //        }

    }

    /**
     * When a board has been installed it may be that a boardDescription needs to
     * reload the txt file
     */
    public static void reloadTxtFile() {
        //        final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        //        for (IProject curProject : workspaceRoot.getProjects()) {
        //            if (curProject.isOpen()) {
        //                SloeberProject sloeberProject = new SloeberProject(curProject);//getSloeberProject(curProject);
        //                if (sloeberProject != null) {
        //                    sloeberProject.internalReloadTxtFile();
        //                }
        //            }
        //        }
    }

    /**
     * When a board has been installed it may be that a boardDescription needs to
     * reload the txt file
     */
    //    private void internalReloadTxtFile() {
    //        for (BoardDescription curBoardDescription : myBoardDescriptions.values()) {
    //            curBoardDescription.reloadTxtFile();
    //        }
    //
    //    }

}
