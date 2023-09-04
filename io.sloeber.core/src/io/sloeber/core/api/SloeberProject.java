package io.sloeber.core.api;

import static io.sloeber.core.common.Const.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICBuildSetting;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;
import org.eclipse.cdt.make.core.IMakeTarget;
import org.eclipse.cdt.make.core.IMakeTargetManager;
import org.eclipse.cdt.make.core.MakeCorePlugin;
import org.eclipse.cdt.make.internal.core.MakeTarget;
import org.eclipse.core.resources.IContainer;
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
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;

import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.ICodeProvider;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.autoBuild.integration.AutoBuildNature;
import io.sloeber.core.Activator;
import io.sloeber.core.Messages;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.listeners.IndexerController;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.Libraries;
import io.sloeber.core.tools.uploaders.UploadSketchWrapper;
import io.sloeber.core.txt.KeyValueTree;
import io.sloeber.core.txt.TxtFile;

public class SloeberProject extends Common {
    public static String LATEST_EXTENSION_POINT_ID = "io.sloeber.autoBuild.buildDefinitions"; //$NON-NLS-1$
    public static String LATEST_EXTENSION_ID = "io.sloeber.builddef"; //$NON-NLS-1$
    public static String PROJECT_ID = "io.sloeber.core.sketch"; //$NON-NLS-1$
    private static QualifiedName sloeberQualifiedName = new QualifiedName(NODE_ARDUINO, "SloeberProject"); //$NON-NLS-1$
    private Map<String, BoardDescription> myBoardDescriptions = new HashMap<>();
    private Map<String, CompileDescription> myCompileDescriptions = new HashMap<>();
    private Map<String, OtherDescription> myOtherDescriptions = new HashMap<>();
    private Map<String, Map<String, String>> myEnvironmentVariables = new HashMap<>();
    private TxtFile myCfgFile = null;
    private IProject myProject = null;
    private boolean myIsInMemory = false;
    private boolean myIsDirty = false; // if anything has changed
    // Do we need to write data to disk
    //this only happens when we were not able to write the data to disk
    //due to a locked workspace
    private boolean myNeedToPersist = false;

    private SloeberProject(IProject project) {
        myProject = project;
        try {
            project.setSessionProperty(sloeberQualifiedName, this);
        } catch (CoreException e) {
            e.printStackTrace();
        }
    }

    public IProject getProject() {
        return myProject;
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

        String realProjectName = Common.MakeNameCompileSafe(projectName);

        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        ICoreRunnable runnable = new ICoreRunnable() {
            @Override
            public void run(IProgressMonitor internalMonitor) throws CoreException {
                IProject newProjectHandle = root.getProject(realProjectName);
                //IndexerController.doNotIndex(newProjectHandle);

                //                String extensionPointID = AutoBuildManager.supportedExtensionPointIDs()[0];
                //                String extensionID = AutoBuildManager.getSupportedExtensionIDs(extensionPointID)[0];
                //                String projectTypeID = AutoBuildManager.getProjectIDs(extensionPointID, extensionID).keySet()
                //                        .toArray(new String[10])[0];
                //                String natureID = CCProjectNature.CC_NATURE_ID;
                //                ICodeProvider codeProvider = null;
                //
                //                AutoBuildProject.createProject(projectName, extensionPointID, extensionID, projectTypeID, natureID,
                //                        codeProvider, internalMonitor);

                //                newProjectHandle = AutoBuildProject.createProject(realProjectName,
                //                        "io.sloeber.autoBuild.buildDefinitions", "cdt.cross.gnu",
                //                        "cdt.managedbuild.target.gnu.cross.exe", CCProjectNature.CC_NATURE_ID, null, internalMonitor);

                newProjectHandle = AutoBuildProject.createProject(realProjectName, LATEST_EXTENSION_POINT_ID,
                        LATEST_EXTENSION_ID, PROJECT_ID, CCProjectNature.CC_NATURE_ID, null, false, internalMonitor);

                // Add the sketch code
                Map<String, IPath> librariesToAdd = codeDesc.createFiles(newProjectHandle, internalMonitor);

                AutoBuildNature.addNature(newProjectHandle, internalMonitor);

                // create a sloeber project
                //SloeberProject arduinoProjDesc = new SloeberProject(newProjectHandle);
                //the line below will trigger environment var requests causing loops if called to early
                //                ManagedBuildManager.setDefaultConfiguration(newProjectHandle, defaultConfig);

                CCorePlugin cCorePlugin = CCorePlugin.getDefault();
                ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(newProjectHandle, true);

                for (ICConfigurationDescription curConfig : prjCDesc.getConfigurations()) {
                    CConfigurationData buildSettings = curConfig.getConfigurationData();
                    if (!(buildSettings instanceof AutoBuildConfigurationDescription)) {
                        //this should not happen as we just created a autoBuild project
                        Common.log(new Status(SLOEBER_STATUS_DEBUG, Activator.getId(),
                                "\"Auto build created a project that does not seem to be a autobuild project :-s : " //$NON-NLS-1$
                                        + realProjectName));
                    }
                    AutoBuildConfigurationDescription autoBuildConfig = (AutoBuildConfigurationDescription) buildSettings;

                    SloeberConfiguration sloeberConfiguration = new SloeberConfiguration(boardDescriptor, otherDesc,
                            compileDescriptor);
                    autoBuildConfig.setAutoBuildConfigurationExtensionDescription(sloeberConfiguration);
                    sloeberConfiguration.addLibrariesToProject(newProjectHandle, librariesToAdd);
                    Map<String, List<IPath>> pathMods = Libraries.addLibrariesForConfiguration(sloeberConfiguration,
                            librariesToAdd);
                    // Add the arduino code folders
                    Helpers.addArduinoCodeForConfig(sloeberConfiguration, boardDescriptor);

                    //                    Libraries.adjustProjectDescription(curConfigDesc, pathMods);
                    //                    Helpers.addIncludeFolder(curConfigDesc, addToIncludePath, true);
                    //TOFIX pretty sure the line below can be deleted because of the setAllEnvironmentVars below.
                    //                        arduinoProjDesc.myEnvironmentVariables.put(curConfigKey,
                    //                                arduinoProjDesc.getEnvVars(curConfigKey));

                }

                //                arduinoProjDesc.createSloeberConfigFiles();
                //                arduinoProjDesc.setAllEnvironmentVars();

                SubMonitor refreshMonitor = SubMonitor.convert(internalMonitor, 3);
                newProjectHandle.open(refreshMonitor);
                newProjectHandle.refreshLocal(IResource.DEPTH_INFINITE, refreshMonitor);
                cCorePlugin.setProjectDescription(newProjectHandle, prjCDesc, true,
                        SubMonitor.convert(internalMonitor, 1));

                Common.log(new Status(SLOEBER_STATUS_DEBUG, Activator.getId(),
                        "internal creation of project is done: " + realProjectName)); //$NON-NLS-1$
                //                IndexerController.index(newProjectHandle);
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

    /**
     * This method asks for a Sloeber project in an old form and creates a sloeber
     * project in a new for
     * currently only the conversion storage in CDT to .sProject is implemented
     * 
     * returns true if the projectdescription needs to be saved
     */

    private static boolean upgradeArduinoProject(SloeberProject project, ICProjectDescription prjCDesc) {
        boolean saveProjDesc = false;

        if (prjCDesc == null) {
            //CDT project description is not found or is not writable
            return false;
        }

        if (project.getConfigLocalFile().exists()) {
            //if the .sproject file exists check for old data and clean old data if found
            saveProjDesc = removeCDTEnvironmentVars(prjCDesc);
        } else {
            //No sloeber project file try to migrate from old CDT storage
            if (project.readConfigFromCDT(prjCDesc)) {
                project.createSloeberConfigFiles();
                project.setAllEnvironmentVars();
                saveProjDesc = removeCDTEnvironmentVars(prjCDesc);
            }
        }
        return saveProjDesc;
    }

    private HashMap<String, String> getEnvVars(String configKey) {
        BoardDescription boardDescription = myBoardDescriptions.get(configKey);
        CompileDescription compileOptions = myCompileDescriptions.get(configKey);
        OtherDescription otherOptions = myOtherDescriptions.get(configKey);

        HashMap<String, String> allVars = new HashMap<>();

        allVars.put(ENV_KEY_BUILD_SOURCE_PATH, myProject.getLocation().toOSString());
        allVars.put(ENV_KEY_BUILD_PATH, myProject.getLocation().append(configKey).toOSString());

        if (boardDescription != null) {
            allVars.putAll(boardDescription.getEnvVars());
        }
        if (compileOptions != null) {
            allVars.putAll(compileOptions.getEnvVars());
        }
        if (otherOptions != null) {
            allVars.putAll(otherOptions.getEnvVars());
        }
        // set the paths
        String pathDelimiter = makeEnvironmentVar("PathDelimiter"); //$NON-NLS-1$
        if (Common.isWindows) {
            allVars.put(SLOEBER_MAKE_LOCATION,
                    ConfigurationPreferences.getMakePath().addTrailingSeparator().toOSString());
            allVars.put(SLOEBER_AWK_LOCATION,
                    ConfigurationPreferences.getAwkPath().addTrailingSeparator().toOSString());

            String systemroot = makeEnvironmentVar("SystemRoot"); //$NON-NLS-1$
            allVars.put("PATH", //$NON-NLS-1$
                    makeEnvironmentVar(ENV_KEY_COMPILER_PATH) + pathDelimiter
                            + makeEnvironmentVar(ENV_KEY_BUILD_GENERIC_PATH) + pathDelimiter + systemroot + "\\system32" //$NON-NLS-1$
                            + pathDelimiter + systemroot + pathDelimiter + systemroot + "\\system32\\Wbem" //$NON-NLS-1$
                            + pathDelimiter + makeEnvironmentVar("sloeber_path_extension")); //$NON-NLS-1$
        } else {
            allVars.put("PATH", makeEnvironmentVar(ENV_KEY_COMPILER_PATH) + pathDelimiter //$NON-NLS-1$
                    + makeEnvironmentVar(ENV_KEY_BUILD_GENERIC_PATH) + pathDelimiter + makeEnvironmentVar("PATH")); //$NON-NLS-1$
        }

        return allVars;
    }

    public void configure() {

        CCorePlugin cCorePlugin = CCorePlugin.getDefault();
        ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(myProject, false);
        configure(prjCDesc, false);
    }

    /**
     * 
     * @param prjCDesc
     * @param prjDescWritable
     * @return true if the projectDesc needs to be saved
     */

    public synchronized boolean configure(ICProjectDescription prjCDesc, boolean prjDescWritable) {
        boolean saveProjDesc = false;

        if (isInMemory()) {
            //Everything is in memory so start from there
            if (myIsDirty || myNeedToPersist) {
                createSloeberConfigFiles();
            }

        } else {
            //we need to read the stuff from disk or CDT
            if (!getConfigLocalFile().exists()) {
                //There is no sloeber configuration file so error your way out
                Common.log(new Status(IStatus.ERROR, io.sloeber.core.Activator.getId(),
                        "Sloeber did not find the sloeber config files and could not configure the project")); //$NON-NLS-1$
            } else {
                //Configure from the sloeber config file
                readConfigFromFiles();

            }
        }

        setAllEnvironmentVars();

        List<String> newConfigs = newConfigsNeededInCDT(prjCDesc);
        if (prjDescWritable) {
            if (newConfigs.size() > 0) {
                saveProjDesc = saveProjDesc || createNeededCDTConfigs(newConfigs, prjCDesc);
            }
        } else {
            saveProjDesc = saveProjDesc || (newConfigs.size() > 0);
            if (saveProjDesc) {
                final IWorkspace workspace = ResourcesPlugin.getWorkspace();
                try {
                    workspace.run(new ICoreRunnable() {

                        @Override
                        public void run(IProgressMonitor monitor) throws CoreException {
                            CCorePlugin cCorePlugin = CCorePlugin.getDefault();
                            ICProjectDescription prjCDescSave = cCorePlugin.getProjectDescription(myProject, true);
                            if (configure(prjCDescSave, true)) {
                                cCorePlugin.setProjectDescription(myProject, prjCDescSave, false, null);
                            }

                        }
                    }, null, IWorkspace.AVOID_UPDATE, null);
                } catch (Exception e) {
                    Common.log(new Status(IStatus.INFO, io.sloeber.core.Activator.getId(),
                            "Failed to do posponed confdesc saving for project " + myProject.getName(), e)); //$NON-NLS-1$
                }
            }
        }

        myIsDirty = false;
        return saveProjDesc;

    }

    /**
     * When the project is in memory the project data has been read from disk and
     * the project is configured.
     * This means:
     * it is available for all activity
     * memory is considered to be the master
     * it may not be persistent with what is on disk (in this case the isDirty flag
     * is set)
     * 
     * @return true if in memory
     */
    public boolean isInMemory() {
        // if We are in memory we are configured
        return myIsInMemory;
    }

    private void setAllEnvironmentVars() {
        // set all the environment variables
        myEnvironmentVariables.clear();
        for (String curConfigKey : myBoardDescriptions.keySet()) {
            myEnvironmentVariables.put(curConfigKey, getEnvVars(curConfigKey));
        }
    }

    /**
     * remove environment variables from CDT
     * that were created the old sloeber way
     * 
     * @param prjCDesc
     *            a writable project description
     * @return true if at least one environment var was removed
     */
    private static boolean removeCDTEnvironmentVars(ICProjectDescription prjCDesc) {
        boolean needsClean = false;
        IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
        IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
        for (ICConfigurationDescription confDesc : prjCDesc.getConfigurations()) {
            IEnvironmentVariable[] CurVariables = contribEnv.getVariables(confDesc);
            for (int i = (CurVariables.length - 1); i > 0; i--) {
                if (CurVariables[i].getName().startsWith("A.")) { //$NON-NLS-1$
                    contribEnv.removeVariable(CurVariables[i].getName(), confDesc);
                    needsClean = true;
                }
                if (CurVariables[i].getName().startsWith("JANTJE.")) { //$NON-NLS-1$
                    contribEnv.removeVariable(CurVariables[i].getName(), confDesc);
                    needsClean = true;
                }
            }
        }
        return needsClean;
    }

    /**
     * Read the sloeber configuration file and setup the project
     * 
     * @return true if the files exist
     */
    private boolean readConfigFromFiles() {
        IFile file = getConfigLocalFile();
        IFile versionFile = getConfigVersionFile();
        if (!(file.exists() || versionFile.exists())) {
            // no sloeber files found
            return false;
        }
        if (file.exists()) {
            myCfgFile = new TxtFile(file.getLocation().toFile());
            if (versionFile.exists()) {
                myCfgFile.mergeFile(versionFile.getLocation().toFile());
            }
        } else {
            myCfgFile = new TxtFile(versionFile.getLocation().toFile());
        }

        KeyValueTree allFileConfigs = myCfgFile.getData().getChild(CONFIG);
        for (Entry<String, KeyValueTree> curChild : allFileConfigs.getChildren().entrySet()) {
            String curConfName = curChild.getKey();
            BoardDescription boardDesc = new BoardDescription(myCfgFile, getBoardPrefix(curConfName));
            CompileDescription compileDescription = new CompileDescription(myCfgFile, getCompilePrefix(curConfName));
            OtherDescription otherDesc = new OtherDescription(myCfgFile, getOtherPrefix(curConfName));
            String curConfKey = curConfName;
            myBoardDescriptions.put(curConfKey, boardDesc);
            myCompileDescriptions.put(curConfKey, compileDescription);
            myOtherDescriptions.put(curConfKey, otherDesc);
        }
        return true;
    }

    /**
     * Read the configuration the old Sloeber CDT environment
     * variable way
     * Do not use this method except for upgrading projects based on user request
     * 
     * @param confDesc
     *            returns true if the config was found
     */
    private boolean readConfigFromCDT(ICProjectDescription prjCDesc) {
        boolean foundAValidConfig = false;
        // Check if this is a old Sloeber project with the data in the eclipse build
        // environment variables
        for (ICConfigurationDescription confDesc : prjCDesc.getConfigurations()) {

            BoardDescription boardDesc = BoardDescription.getFromCDT(confDesc);
            CompileDescription compileDescription = CompileDescription.getFromCDT(confDesc);
            OtherDescription otherDesc = OtherDescription.getFromCDT(confDesc);
            if (boardDesc.getReferencingBoardsFile() != null) {
                if (!boardDesc.getReferencingBoardsFile().toString().isBlank()) {
                    foundAValidConfig = true;
                    myBoardDescriptions.put(getConfigKey(confDesc), boardDesc);
                    myCompileDescriptions.put(getConfigKey(confDesc), compileDescription);
                    myOtherDescriptions.put(getConfigKey(confDesc), otherDesc);
                }
            }
        }
        return foundAValidConfig;
    }

    /**
     * return the list of configNames known by Sloeber not known by CDT
     * 
     * @param prjCDesc
     * @return a list of sloeber known configurationNames unknown to CDT
     */
    private List<String> newConfigsNeededInCDT(ICProjectDescription prjCDesc) {
        List<String> ret = new LinkedList<>();
        for (String curConfName : myBoardDescriptions.keySet()) {
            ICConfigurationDescription curConfDesc = prjCDesc.getConfigurationByName(curConfName);
            if (curConfDesc == null) {
                ret.add(curConfName);
            }
        }
        return ret;

    }

    /**
     * create the cdt configurations from the list
     * 
     * @param configs
     * @param prjCDesc
     * @return true if at least one config was created (basically only false if
     *         configs is empty oe error conditions)
     */
    private static boolean createNeededCDTConfigs(List<String> configs, ICProjectDescription prjCDesc) {
        boolean ret = false;
        for (String curConfName : configs) {
            try {
                String id = CDataUtil.genId(null);
                prjCDesc.createConfiguration(id, curConfName, prjCDesc.getActiveConfiguration());
                ret = true;
            } catch (Exception e) {
                // ignore as we will try again later
                e.printStackTrace();
            }
        }
        return ret;
    }

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
    private void createSloeberConfigFiles() {

        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        if (workspace.isTreeLocked()) {
            // we cant save now do it later
            myNeedToPersist = true;
            return;
        }

        Map<String, String> configVars = new TreeMap<>();
        Map<String, String> versionVars = new TreeMap<>();

        for (String configKey : myBoardDescriptions.keySet()) {
            BoardDescription boardDescription = myBoardDescriptions.get(configKey);
            CompileDescription compileDescription = myCompileDescriptions.get(configKey);
            OtherDescription otherDescription = myOtherDescriptions.get(configKey);

            String boardPrefix = getBoardPrefix(configKey);
            String compPrefix = getCompilePrefix(configKey);
            String otherPrefix = getOtherPrefix(configKey);

            configVars.putAll(boardDescription.getEnvVarsConfig(boardPrefix));
            configVars.putAll(compileDescription.getEnvVarsConfig(compPrefix));
            configVars.putAll(otherDescription.getEnvVarsConfig(otherPrefix));

            if (otherDescription.IsVersionControlled()) {
                versionVars.putAll(boardDescription.getEnvVarsVersion(boardPrefix));
                versionVars.putAll(compileDescription.getEnvVarsVersion(compPrefix));
                versionVars.putAll(otherDescription.getEnvVarsVersion(otherPrefix));
            }
        }

        try {
            storeConfigurationFile(getConfigVersionFile(), versionVars);
            storeConfigurationFile(getConfigLocalFile(), configVars);
            myNeedToPersist = false;
        } catch (Exception e) {
            Common.log(new Status(IStatus.ERROR, io.sloeber.core.Activator.getId(),
                    "failed to save the sloeber config files", e)); //$NON-NLS-1$
            myNeedToPersist = true;
        }

    }

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

    /**
     * method to switch the board in a given configuration This method assumes the
     * configuration description is a valid arduino confiuguration description and
     * only the board descriptor changed
     * 
     * @param confDesc
     * @param boardDescription
     */
    public void setBoardDescription(String confDescName, BoardDescription boardDescription, boolean force) {
        BoardDescription oldBoardDescription = myBoardDescriptions.get(confDescName);
        if (!force) {
            if (boardDescription.equals(oldBoardDescription)) {
                return;
            }
        }
        if (boardDescription.needsRebuild(oldBoardDescription)) {
            Helpers.deleteBuildFolder(myProject, confDescName);
        }
        myBoardDescriptions.put(confDescName, boardDescription);
        myIsDirty = true;
    }

    /**
     * get the Arduino project description based on a project description
     * 
     * @param project
     * @return the sloeber project or null if this is not a sloeber project
     */
    public static SloeberProject getSloeberProject(IProject project) {

        if (project.isOpen() && project.getLocation().toFile().exists()) {
            if (Sketch.isSketch(project)) {
                Object sessionProperty = null;
                try {
                    sessionProperty = project.getSessionProperty(sloeberQualifiedName);
                    if (null != sessionProperty) {
                        SloeberProject sloeberProject = (SloeberProject) sessionProperty;
                        if (sloeberProject.isInMemory()) {
                            IndexerController.index(project);
                        }
                        return sloeberProject;
                    }
                } catch (CoreException e) {
                    e.printStackTrace();
                }
                return new SloeberProject(project);
            }
        }
        return null;
    }

    public void setCompileDescription(String confDescName, CompileDescription compileDescription) {

        CompileDescription oldCompileDescription = myCompileDescriptions.get(confDescName);
        if (compileDescription.needsRebuild(oldCompileDescription)) {
            Helpers.deleteBuildFolder(myProject, confDescName);
        }
        myCompileDescriptions.put(confDescName, compileDescription);
        myIsDirty = true;
    }

    public void setOtherDescription(String confDescName, OtherDescription otherDesc) {
        try {
            myOtherDescriptions.put(confDescName, otherDesc);
            myIsDirty = true;
        } catch (Exception e) {
            e.printStackTrace();
            Common.log(new Status(IStatus.ERROR, io.sloeber.core.Activator.getId(), "failed to save the board settings", //$NON-NLS-1$
                    e));
        }

    }

    /**
     * Method that tries to give you the boardDescription settings for this
     * project/configuration This method tries folowing things 1)memory (after 2 or
     * 3) 2)configuration files in the project (at project creation) 3)CDT
     * environment variables (to update projects created by previous versions of
     * Sloeber)
     * 
     * @param confDesc
     * @return
     */
    public BoardDescription getBoardDescription(String confDescName, boolean allowNull) {
        if (!allowNull) {
            configure();
        }
        return myBoardDescriptions.get(confDescName);
    }

    public CompileDescription getCompileDescription(String confDescName, boolean allowNull) {
        if (!allowNull) {
            configure();
        }
        return myCompileDescriptions.get(confDescName);
    }

    public OtherDescription getOtherDescription(String confDescName, boolean allowNull) {
        if (!allowNull) {
            configure();
        }
        return myOtherDescriptions.get(confDescName);
    }

    /**
     * get the text for the decorator
     * 
     * @param text
     * @return
     */
    public String getDecoratedText(String text) {
        if (!isInMemory()) {
            configure();
        }
        ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(myProject);
        if (prjDesc != null) {
            ICConfigurationDescription confDesc = prjDesc.getActiveConfiguration();
            if (confDesc != null) {
                // do not use getBoardDescriptor below as this will cause a infinite loop at
                // project creation
                BoardDescription boardDescriptor = myBoardDescriptions.get(getConfigKey(confDesc));
                if (boardDescriptor == null) {
                    return text + " Project not configured"; //$NON-NLS-1$
                }
                String boardName = boardDescriptor.getBoardName();
                String portName = boardDescriptor.getActualUploadPort();
                if (portName.isEmpty()) {
                    portName = Messages.decorator_no_port;
                }
                if (boardName.isEmpty()) {
                    boardName = Messages.decorator_no_platform;
                }

                return text + ' ' + boardName + ' ' + ':' + portName;
            }
        }
        return text;
    }

    private static String getBoardPrefix(String confDescName) {
        return CONFIG_DOT + confDescName + DOT + "board."; //$NON-NLS-1$
    }

    private static String getCompilePrefix(String confDescName) {
        return CONFIG_DOT + confDescName + DOT + "compile."; //$NON-NLS-1$
    }

    private static String getOtherPrefix(String confDescName) {
        return CONFIG_DOT + confDescName + DOT + "other."; //$NON-NLS-1$
    }

    /*
     * Get the file that Sloeber maintains and that is meant to be stored in version control
     */
    private IFile getConfigVersionFile() {
        return Sketch.getConfigVersionFile(myProject);
    }

    /*
     * Get the sloeber configuration file
     */
    private IFile getConfigLocalFile() {
        return Sketch.getConfigLocalFile(myProject);
    }

    public void configChangeAboutToApply(ICProjectDescription newProjDesc, ICProjectDescription oldProjDesc) {
        ICConfigurationDescription newActiveConfig = newProjDesc.getActiveConfiguration();
        ICConfigurationDescription oldActiveConfig = oldProjDesc.getActiveConfiguration();

        //handle configuration name changes and new configs
        for (ICConfigurationDescription curConfig : newProjDesc.getConfigurations()) {
            String curConfigKey = getConfigKey(curConfig);
            ICConfigurationDescription oldConfig = oldProjDesc.getConfigurationById(curConfig.getId());
            if (oldConfig == null) {
                //this is a new config
                myIsDirty = true;

                BoardDescription brdDesc = myBoardDescriptions.get(curConfigKey);
                if (brdDesc == null) {
                    // This new configuration has not been created in project properties
                    // I don't know how to get the "copied from" configuration
                    // trying to get something somewhere
                    // read: only copy configurations in project properties
                    String copyConfKey = getConfigKey(oldActiveConfig);
                    brdDesc = myBoardDescriptions.get(copyConfKey);
                    if (brdDesc == null) {
                        copyConfKey = getConfigKey(oldActiveConfig);
                        brdDesc = myBoardDescriptions.get(copyConfKey);
                    }
                    BoardDescription boardDescription = new BoardDescription(myBoardDescriptions.get(copyConfKey));
                    myBoardDescriptions.put(curConfigKey, boardDescription);

                    CompileDescription compileDescription = new CompileDescription(
                            myCompileDescriptions.get(copyConfKey));
                    myCompileDescriptions.put(curConfigKey, compileDescription);

                    OtherDescription otherDescription = new OtherDescription(myOtherDescriptions.get(copyConfKey));
                    myOtherDescriptions.put(curConfigKey, otherDescription);
                }

            } else {

                String oldConfigKey = getConfigKey(oldConfig);
                if (!oldConfigKey.equals(curConfigKey)) {
                    //this is a rename
                    myIsDirty = true;
                    Helpers.deleteBuildFolder(myProject, oldConfig.getName());
                    BoardDescription boardDesc = myBoardDescriptions.get(oldConfigKey);
                    myBoardDescriptions.remove(oldConfigKey);
                    myBoardDescriptions.put(curConfigKey, boardDesc);

                    CompileDescription compDesc = myCompileDescriptions.get(oldConfigKey);
                    myCompileDescriptions.remove(oldConfigKey);
                    myCompileDescriptions.put(curConfigKey, compDesc);

                    OtherDescription otherDesc = myOtherDescriptions.get(oldConfigKey);
                    myOtherDescriptions.remove(oldConfigKey);
                    myOtherDescriptions.put(curConfigKey, otherDesc);

                }

            }
        }

        //delete all the deleted configs
        for (ICConfigurationDescription curConfig : oldProjDesc.getConfigurations()) {
            String curConfigKey = getConfigKey(curConfig);
            ICConfigurationDescription newConfig = newProjDesc.getConfigurationById(curConfig.getId());
            if (newConfig == null) {
                //this is a deleted config
                myIsDirty = true;
                myBoardDescriptions.remove(curConfigKey);
                myCompileDescriptions.remove(curConfigKey);
                myOtherDescriptions.remove(curConfigKey);
            }
        }

        configure(newProjDesc, true);

    }

    /**
     * Call this method when the sloeber.cfg file changed
     * 
     */
    public void sloeberCfgChanged() {
        //CCorePlugin cCorePlugin = CCorePlugin.getDefault();
        //ICProjectDescription projDesc = cCorePlugin.getProjectDescription(myProject, true);
        ///ICConfigurationDescription activeConfig = projDesc.getActiveConfiguration();
        configure();
        //all configs may have changed so only deleting the active config does not make sense
        //Helpers.deleteBuildFolder(myProject, activeConfig.getName());

        //This code is only triggered when sloeber.cfg changed so no need to set the active config
        //projDescNeedsSaving = projDescNeedsSaving || setActiveConfig(activeConfig);
        //        if (projDescNeedsSaving) {
        //            try {
        //                cCorePlugin.setProjectDescription(myProject, projDesc);
        //            } catch (CoreException e) {
        //                e.printStackTrace();
        //            }
        //        }

    }

    /**
     * When a board has been installed it may be that a boardDescription needs to
     * reload the txt file
     */
    public static void reloadTxtFile() {
        final IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
        for (IProject curProject : workspaceRoot.getProjects()) {
            if (curProject.isOpen()) {
                SloeberProject sloeberProject = getSloeberProject(curProject);
                if (sloeberProject != null) {
                    sloeberProject.internalReloadTxtFile();
                }
            }
        }
    }

    /**
     * When a board has been installed it may be that a boardDescription needs to
     * reload the txt file
     */
    private void internalReloadTxtFile() {
        for (BoardDescription curBoardDescription : myBoardDescriptions.values()) {
            curBoardDescription.reloadTxtFile();
        }

    }

    /**
     * This method returns the key used to identify the configuraation One can use
     * the id or the name. Both with advantages and disadvantages
     * 
     * I doubted between the 2 and finally set on the name. To be able to switch
     * between the 2 options during investigation I created this method So basically
     * now this can be replaced by configDesc.getName();
     * 
     * @param configDesc
     * @return
     */
    private static String getConfigKey(ICConfigurationDescription configDesc) {
        return configDesc.getName();
    }

    public Map<String, String> getEnvironmentVariables(String configKey) {
        if (!isInMemory()) {
            configure();
        }

        return myEnvironmentVariables.get(configKey);
    }

    public IStatus upLoadUsingProgrammer() {
        return BuildTarget("uploadWithProgrammerWithoutBuild"); //$NON-NLS-1$
    }

    public IStatus burnBootloader() {
        return BuildTarget("BurnBootLoader"); //$NON-NLS-1$
    }

    private IStatus BuildTarget(String targetName) {

        try {
            IMakeTargetManager targetManager = MakeCorePlugin.getDefault().getTargetManager();
            IContainer targetResource = myProject.getFolder(RELEASE);
            IMakeTarget itarget = targetManager.findTarget(targetResource, targetName);
            if (itarget == null) {
                itarget = targetManager.createTarget(myProject, targetName, "org.eclipse.cdt.build.MakeTargetBuilder"); //$NON-NLS-1$
                if (itarget instanceof MakeTarget) {
                    ((MakeTarget) itarget).setBuildTarget(targetName);
                    targetManager.addTarget(targetResource, itarget);
                }
            }
            if (itarget != null) {
                itarget.build(new NullProgressMonitor());
            }
        } catch (CoreException e) {
            return new Status(IStatus.ERROR, CORE_PLUGIN_ID, e.getMessage(), e);
        }
        return Status.OK_STATUS;
    }

    /**
     * Synchronous upload of the sketch to the board returning the status.
     *
     * @param project
     * @return the status of the upload. Status.OK means upload is OK
     */
    public IStatus upload() {

        Job upLoadJob = UploadSketchWrapper.upload(this,
                CoreModel.getDefault().getProjectDescription(myProject).getActiveConfiguration());

        if (upLoadJob == null)
            return new Status(IStatus.ERROR, CORE_PLUGIN_ID, Messages.Upload_failed, null);
        try {
            upLoadJob.join();
            return upLoadJob.getResult();
        } catch (InterruptedException e) {
            // not sure if this is needed
            return new Status(IStatus.ERROR, CORE_PLUGIN_ID, Messages.Upload_failed, e);
        }
    }

    public IFile getTargetFile() {
        // I assume the extension is .hex as the Arduino Framework does not provide the
        // extension nor a key for the uploadable sketch (=build target)
        // as currently this method is only used for network upload via yun this is ok
        // for now
        CCorePlugin cCorePlugin = CCorePlugin.getDefault();
        ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(myProject, false);
        String activeConfig = prjCDesc.getActiveConfiguration().getName();
        return myProject.getFolder(activeConfig).getFile(myProject.getName() + ".hex"); //$NON-NLS-1$
    }

}
