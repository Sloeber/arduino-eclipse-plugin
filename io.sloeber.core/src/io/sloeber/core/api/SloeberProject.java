package io.sloeber.core.api;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IContributedEnvironment;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.envvar.IEnvironmentVariableManager;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.core.IProjectType;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
import org.eclipse.cdt.managedbuilder.core.ManagedCProjectNature;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
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

import io.sloeber.core.Activator;
import io.sloeber.core.Messages;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.Const;
import io.sloeber.core.listeners.IndexerController;
import io.sloeber.core.toolchain.SloeberConfigurationVariableSupplier;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.Libraries;
import io.sloeber.core.txt.TxtFile;

public class SloeberProject extends Common {
    private static QualifiedName sloeberQualifiedName = new QualifiedName(Activator.NODE_ARDUINO, "SloeberProject"); //$NON-NLS-1$
    private Map<String, BoardDescription> myBoardDescriptions = new HashMap<>();
    private Map<String, CompileDescription> myCompileDescriptions = new HashMap<>();
    private TxtFile myCfgFile = null;

    private static final String ENV_KEY_BUILD_SOURCE_PATH = ERASE_START + "build.source.path"; //$NON-NLS-1$
    private static final String ENV_KEY_BUILD_GENERIC_PATH = ERASE_START + "build.generic.path"; //$NON-NLS-1$
    private static final String ENV_KEY_COMPILER_PATH = ERASE_START + "compiler.path"; //$NON-NLS-1$
    private static final String ENV_KEY_JANTJE_MAKE_LOCATION = ENV_KEY_JANTJE_START + "make_location"; //$NON-NLS-1$

    private SloeberProject(IProject project, boolean skipChildren) {
        try {
            project.setSessionProperty(sloeberQualifiedName, this);
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (skipChildren) {
            return;
        }
        configureProject(project);

    }

    /*
     * Method to create a project based on the board
     */
    public static IProject createArduinoProject(String projectName, URI projectURI, BoardDescription boardDescriptor,
            CodeDescription codeDescription, CompileDescription compileDescriptor, IProgressMonitor monitor) {

        String realProjectName = Common.MakeNameCompileSafe(projectName);

        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        final IProject newProjectHandle = root.getProject(realProjectName);
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        ICoreRunnable runnable = new ICoreRunnable() {
            @Override
            public void run(IProgressMonitor internalMonitor) throws CoreException {
                try {
                    IWorkspaceDescription workspaceDesc = workspace.getDescription();
                    workspaceDesc.setAutoBuilding(false);
                    workspace.setDescription(workspaceDesc);
                    IProjectType sloeberProjType = ManagedBuildManager.getProjectType("io.sloeber.core.sketch"); //$NON-NLS-1$

                    // create a eclipse project
                    IProjectDescription description = workspace.newProjectDescription(newProjectHandle.getName());
                    if (projectURI != null) {
                        description.setLocationURI(projectURI);
                    }

                    // make the eclipse project a cdt project
                    CCorePlugin.getDefault().createCProject(description, newProjectHandle, new NullProgressMonitor(),
                            ManagedBuilderCorePlugin.MANAGED_MAKE_PROJECT_ID);

                    // add the required natures
                    ManagedCProjectNature.addManagedNature(newProjectHandle, internalMonitor);
                    ManagedCProjectNature.addManagedBuilder(newProjectHandle, internalMonitor);
                    ManagedCProjectNature.addNature(newProjectHandle, "org.eclipse.cdt.core.ccnature", internalMonitor); //$NON-NLS-1$
                    ManagedCProjectNature.addNature(newProjectHandle, Const.ARDUINO_NATURE_ID, internalMonitor);

                    // make the cdt project a managed build project
                    ManagedBuildManager.createBuildInfo(newProjectHandle);
                    IManagedProject newProject = ManagedBuildManager.createManagedProject(newProjectHandle,
                            sloeberProjType);
                    ManagedBuildManager.setNewProjectVersion(newProjectHandle);
                    // Copy over the Sloeber configs
                    IConfiguration defaultConfig = null;
                    IConfiguration[] configs = sloeberProjType.getConfigurations();
                    for (int i = 0; i < configs.length; ++i) {
                        IConfiguration curConfig = newProject.createConfiguration(configs[i],
                                sloeberProjType.getId() + "." + i); //$NON-NLS-1$
                        curConfig.setArtifactName(newProject.getDefaultArtifactName());
                        curConfig.getEditableBuilder().setParallelBuildOn(compileDescriptor.isParallelBuildEnabled());
                        // Make the first configuration the default
                        if (i == 0) {
                            defaultConfig = curConfig;
                        }
                    }

                    ManagedBuildManager.setDefaultConfiguration(newProjectHandle, defaultConfig);
                    Map<String, IPath> librariesToAdd = codeDescription.createFiles(newProjectHandle,
                            new NullProgressMonitor());

                    CCorePlugin cCorePlugin = CCorePlugin.getDefault();
                    ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(newProjectHandle);

                    SloeberProject arduinoProjDesc = new SloeberProject(newProjectHandle, true);
                    for (ICConfigurationDescription curConfig : prjCDesc.getConfigurations()) {
                        // Even though we use the same boardDescriptor for all configurations during
                        // project creation
                        // we need to add them config per config because of the include linking
                        Helpers.addArduinoCodeToProject(boardDescriptor, curConfig);

                        arduinoProjDesc.putCompileDescription(curConfig, compileDescriptor);
                        arduinoProjDesc.putBoardDescription(curConfig, boardDescriptor);

                        setEnvVars(curConfig, arduinoProjDesc.getEnvVars(curConfig), true);
                        Libraries.addLibrariesToProject(newProjectHandle, curConfig, librariesToAdd);
                    }

                    arduinoProjDesc.saveConfig(newProjectHandle);
                    SubMonitor refreshMonitor = SubMonitor.convert(internalMonitor, 3);
                    newProjectHandle.open(refreshMonitor);
                    newProjectHandle.refreshLocal(IResource.DEPTH_INFINITE, refreshMonitor);
                    cCorePlugin.setProjectDescription(newProjectHandle, prjCDesc, true, null);

                } catch (Exception e) {
                    Common.log(new Status(IStatus.INFO, io.sloeber.core.Activator.getId(),
                            "Project creation failed: " + newProjectHandle.getName(), e)); //$NON-NLS-1$
                }
                Common.log(new Status(Const.SLOEBER_STATUS_DEBUG, Activator.getId(),
                        "internal creation of project is done: " + newProjectHandle.getName())); //$NON-NLS-1$
            }
        };

        try {
            IndexerController.doNotIndex(newProjectHandle);
            workspace.run(runnable, root, IWorkspace.AVOID_UPDATE, monitor);
        } catch (Exception e) {
            Common.log(new Status(IStatus.INFO, io.sloeber.core.Activator.getId(),
                    "Project creation failed: " + newProjectHandle.getName(), e)); //$NON-NLS-1$
        }

        monitor.done();
        IndexerController.Index(newProjectHandle);
        return newProjectHandle;
    }

    private void saveConfig(IProject project) {
        createSloeberConfigFiles(project);
    }

    private HashMap<String, String> getEnvVars(ICConfigurationDescription confDesc) {
        IProject project = confDesc.getProjectDescription().getProject();

        BoardDescription boardDescription = getBoardDescription(confDesc);
        CompileDescription compileOptions = getCompileDescription(confDesc);

        HashMap<String, String> allVars = new HashMap<>();

        allVars.put(ENV_KEY_BUILD_SOURCE_PATH, project.getLocation().toOSString());

        if (boardDescription != null) {
            allVars.putAll(boardDescription.getEnvVars());
        }
        if (compileOptions != null) {
            allVars.putAll(compileOptions.getEnvVars());
        }
        // set the paths
        String pathDelimiter = makeEnvironmentVar("PathDelimiter"); //$NON-NLS-1$
        if (Common.isWindows) {
            allVars.put(ENV_KEY_JANTJE_MAKE_LOCATION,
                    ConfigurationPreferences.getMakePath().addTrailingSeparator().toOSString());
            String systemroot = makeEnvironmentVar("SystemRoot"); //$NON-NLS-1$
            allVars.put("PATH", //$NON-NLS-1$
                    makeEnvironmentVar(ENV_KEY_COMPILER_PATH) + pathDelimiter
                            + makeEnvironmentVar(ENV_KEY_BUILD_GENERIC_PATH) + pathDelimiter + systemroot + "\\system32" //$NON-NLS-1$
                            + pathDelimiter + systemroot + pathDelimiter + systemroot + "\\system32\\Wbem" //$NON-NLS-1$
                            + pathDelimiter + makeEnvironmentVar("sloeber_path_extension")); //$NON-NLS-1$
        } else {
            allVars.put("PATH", //$NON-NLS-1$
                    makeEnvironmentVar(ENV_KEY_COMPILER_PATH) + pathDelimiter
                            + makeEnvironmentVar(ENV_KEY_BUILD_GENERIC_PATH) + pathDelimiter
                            + makeEnvironmentVar("PATH")); //$NON-NLS-1$
        }

        return allVars;
    }

    private void configureProject(IProject project) {
        if (readSloeberConfig(project)) {
            saveConfig(project);
        }
        ICProjectDescription projDesc = CCorePlugin.getDefault().getProjectDescription(project);
        for (ICConfigurationDescription confDesc : projDesc.getConfigurations()) {
            setEnvVars(confDesc, getEnvVars(confDesc), true);
        }
    }

    /**
     * Read the configuration needed to setup the project First try the
     * configuration files If they do not exist try the old Sloeber CDT environment
     * variable way
     * 
     * @param confDesc
     *            returns true if the config needs saving otherwise false
     */
    private boolean readSloeberConfig(IProject project) {
        boolean needsSave = false;
        CCorePlugin cCorePlugin = CCorePlugin.getDefault();
        ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(project);

        IFile file = getConfigLocalFile(project);
        if (file.exists()) {
            myCfgFile = new TxtFile(file.getLocation().toFile());
            IFile versionFile = getConfigVersionFile(project);
            if (versionFile.exists()) {
                myCfgFile.mergeFile(versionFile.getLocation().toFile());
            }
            // For all the configs known in the project
            // try to get the sloeber data
            for (ICConfigurationDescription confDesc : prjCDesc.getConfigurations()) {
                BoardDescription boardDesc = new BoardDescription(myCfgFile, getBoardPrefix(confDesc));
                CompileDescription compileDescription = new CompileDescription(myCfgFile, getCompilePrefix(confDesc));
                putBoardDescription(confDesc, boardDesc);
                putCompileDescription(confDesc, compileDescription);
            }
            // For all the configs known in sloeber data
            // check wether a config exists
            // and if not create it
        } else {

            // TODO error handling configfiles not ok
            if (!IndexerController.isPosponed(project)) {
                needsSave = true;
                for (ICConfigurationDescription confDesc : prjCDesc.getConfigurations()) {
                    BoardDescription boardDescription = BoardDescription.getFromCDT(confDesc);
                    CompileDescription compileDescription = CompileDescription.getFromCDTEnvVars(confDesc);
                    putBoardDescription(confDesc, boardDescription);
                    putCompileDescription(confDesc, compileDescription);
                }
                Common.log(new Status(IStatus.ERROR, io.sloeber.core.Activator.getId(),
                        "failed to read file: " + file.getName()));
            }
        }
        return needsSave;
    }



    /**
     * This methods creates/updates 2 files in the workspace. Together these files
     * contain the Sloeber project configuration info The info is split into 2 files
     * because you probably do not want to add all the info to a version control
     * tool.
     * 
     * .sproject is the file you can add to a version control sloeber.cfg is the
     * file with settings you do not want to add to version control
     * 
     * @param project
     *            the project to store the data for
     */
    private void createSloeberConfigFiles(IProject project) {
        CCorePlugin cCorePlugin = CCorePlugin.getDefault();
        ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(project);

        Map<String, String> configVars = new TreeMap<>();
        Map<String, String> versionVars = new TreeMap<>();

        for (ICConfigurationDescription confDesc : prjCDesc.getConfigurations()) {
            BoardDescription boardDescription = getBoardDescription(confDesc);
            CompileDescription compileDescription = getCompileDescription(confDesc);
            String boardPrefix = getBoardPrefix(confDesc);
            String compPrefix = getCompilePrefix(confDesc);

            configVars.putAll(boardDescription.getEnvVarsConfig(boardPrefix));
            configVars.putAll(compileDescription.getEnvVarsConfig(compPrefix));
            boolean StoreConfigInversion = true;
            if (StoreConfigInversion) {
                versionVars.putAll(boardDescription.getEnvVarsConfig(boardPrefix));
                versionVars.putAll(compileDescription.getEnvVarsConfig(compPrefix));
            }
        }

        try {
            project.refreshLocal(IResource.DEPTH_INFINITE, null);
            storeConfigurationFile(getConfigVersionFile(project), versionVars);
            storeConfigurationFile(getConfigLocalFile(project), configVars);

        } catch (CoreException e) {
            Common.log(new Status(IStatus.ERROR, io.sloeber.core.Activator.getId(),
                    "failed to save the sloeber config files", e));
        }

    }

    private static void storeConfigurationFile(IFile file, Map<String, String> vars) throws CoreException {
        String content = EMPTY;
        for (Entry<String, String> curLine : vars.entrySet()) {
            content += curLine.getKey() + '=' + curLine.getValue() + '\n';
        }

        if (file.exists()) {
            file.delete(true, null);
        }

        if (!file.exists() && (!content.isBlank())) {
            ByteArrayInputStream stream = new ByteArrayInputStream(content.getBytes());
            file.create(stream, true, null);
        }

    }

    /**
     * Remove all the arduino environment variables.
     *
     * @param contribEnv
     * @param confDesc
     */
    private static void removeAllEraseEnvironmentVariables(IContributedEnvironment contribEnv,
            ICConfigurationDescription confDesc) {

        IEnvironmentVariable[] CurVariables = contribEnv.getVariables(confDesc);
        for (int i = (CurVariables.length - 1); i > 0; i--) {
            if (CurVariables[i].getName().startsWith(Const.ERASE_START)) {
                contribEnv.removeVariable(CurVariables[i].getName(), confDesc);
            }
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
    public void setBoardDescription(ICConfigurationDescription confDesc, BoardDescription boardDescription) {
        try {
            IProject project = confDesc.getProjectDescription().getProject();
            boolean isRebuildNeeded = true;
            BoardDescription oldBoardDescription = getBoardDescription(confDesc);
            if (oldBoardDescription != null) {
                isRebuildNeeded = oldBoardDescription.needsRebuild(boardDescription);
            }
            Helpers.addArduinoCodeToProject(boardDescription, confDesc);

            isRebuildNeeded = isRebuildNeeded || Helpers.removeInvalidIncludeFolders(confDesc);
            putBoardDescription(confDesc, boardDescription);
            saveConfig(project);
            setEnvVars(confDesc, getEnvVars(confDesc), true);
            if (isRebuildNeeded) {
                Helpers.setDirtyFlag(project, confDesc);
            } else {
                Common.log(new Status(IStatus.INFO, io.sloeber.core.Activator.getId(),
                        "Ignoring project update; clean may be required: " + project.getName())); //$NON-NLS-1$
            }
        } catch (Exception e) {
            e.printStackTrace();
            Common.log(new Status(IStatus.ERROR, io.sloeber.core.Activator.getId(), "failed to save the board settings", //$NON-NLS-1$
                    e));
        }
    }

    private static void setEnvVars(ICConfigurationDescription confDesc, Map<String, String> envVars, boolean replace) {
        IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
        IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
        if (replace) {
            // remove all Arduino Variables so there is no memory effect
            removeAllEraseEnvironmentVariables(contribEnv, confDesc);
        }

        IConfiguration configuration = ManagedBuildManager.getConfigurationForDescription(confDesc);
        if (configuration != null) {
            SloeberConfigurationVariableSupplier.setEnvVars(configuration, envVars, replace);
        }

        // for (Entry<String, String> curVariable : envVars.entrySet()) {
        // String name = curVariable.getKey();
        // String value = curVariable.getValue();
        // IEnvironmentVariable var = new EnvironmentVariable(name,
        // makePathEnvironmentString(value));
        // contribEnv.addVariable(var, confDesc);
        // }

    }

    /**
     * get the Arduino project description based on a project description
     * 
     * @param project
     * @return
     */
    public static synchronized SloeberProject getSloeberProject(IProject project) {

        if (project.isOpen() && project.getLocation().toFile().exists()) {
            if (Sketch.isSketch(project)) {
                Object sessionProperty = null;
                try {
                    sessionProperty = project.getSessionProperty(sloeberQualifiedName);
                    if (null != sessionProperty) {
                        SloeberProject ret = (SloeberProject) sessionProperty;
                        return ret;
                    }
                } catch (CoreException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                SloeberProject ret = new SloeberProject(project, false);
                return ret;
            }
        }
        return null;
    }

    public void setCompileDescription(ICConfigurationDescription confDesc, CompileDescription compileDescription) {
        try {
            IProject project = confDesc.getProjectDescription().getProject();
            boolean isRebuildNeeded = true;
            CompileDescription oldCompileDescription = getCompileDescription(confDesc);
            if (oldCompileDescription != null) {
                isRebuildNeeded = oldCompileDescription.needsRebuild(compileDescription);
            }

            putCompileDescription(confDesc, compileDescription);
            saveConfig(project);
            setEnvVars(confDesc, getEnvVars(confDesc), true);
            if (isRebuildNeeded) {
                Helpers.setDirtyFlag(project, confDesc);
            } else {
                Common.log(new Status(IStatus.INFO, io.sloeber.core.Activator.getId(),
                        "Ignoring project update; clean may be required: " + project.getName())); //$NON-NLS-1$
            }
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
    public BoardDescription getBoardDescription(ICConfigurationDescription confDesc) {
        BoardDescription ret = myBoardDescriptions.get(confDesc.getId());
        if (ret == null) {
            IProject project = confDesc.getProjectDescription().getProject();
            configureProject(project);
            ret = myBoardDescriptions.get(confDesc.getId());
        }
        return ret;
    }

    public CompileDescription getCompileDescription(ICConfigurationDescription confDesc) {
        CompileDescription ret = myCompileDescriptions.get(confDesc.getId());
        if (ret == null) {
            IProject project = confDesc.getProjectDescription().getProject();
            configureProject(project);
            ret = myCompileDescriptions.get(confDesc.getId());
        }
        return ret;
    }

    public String getDecoratedText(ICConfigurationDescription confDesc, String text) {
        BoardDescription boardDescriptor = getBoardDescription(confDesc);
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

    private void putBoardDescription(ICConfigurationDescription confDesc, BoardDescription boardDesc) {
        myBoardDescriptions.put(confDesc.getId(), boardDesc);
    }

    private void putCompileDescription(ICConfigurationDescription confDesc, CompileDescription compDesc) {
        myCompileDescriptions.put(confDesc.getId(), compDesc);
    }

    private static String getBoardPrefix(ICConfigurationDescription confDesc) {
        return "Config." + confDesc.getName() + DOT + "board."; //$NON-NLS-1$
    }

    private static String getCompilePrefix(ICConfigurationDescription confDesc) {
        return "Config." + confDesc.getName() + DOT + "compile."; //$NON-NLS-1$
    }

    private static IFile getConfigVersionFile(IProject project) {
        return project.getFile("sloeber.cfg"); //$NON-NLS-1$
    }

    private static IFile getConfigLocalFile(IProject project) {
        return project.getFile(".sproject"); //$NON-NLS-1$
    }
}
