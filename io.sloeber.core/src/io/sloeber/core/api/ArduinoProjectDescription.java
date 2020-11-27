package io.sloeber.core.api;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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

public class ArduinoProjectDescription extends Common {
    static private Map<String, ArduinoProjectDescription> ArduinoProjectList = new HashMap<>();
    private Map<String, BoardDescription> myBoardDescriptions = new HashMap<>();
    private Map<String, CompileDescription> myCompileDescriptions = new HashMap<>();
    private IProject myProject = null;
    private boolean isDirty = true;

    private static final String ENV_KEY_BUILD_SOURCE_PATH = ERASE_START + "build.source.path"; //$NON-NLS-1$
    private static final String ENV_KEY_BUILD_GENERIC_PATH = ERASE_START + "build.generic.path"; //$NON-NLS-1$
    private static final String ENV_KEY_COMPILER_PATH = ERASE_START + "compiler.path"; //$NON-NLS-1$
    private static final String ENV_KEY_JANTJE_MAKE_LOCATION = ENV_KEY_JANTJE_START + "make_location"; //$NON-NLS-1$

    private ArduinoProjectDescription(IProject project) {
        myProject = project;
        ICProjectDescription projDesc = CCorePlugin.getDefault().getProjectDescription(myProject);
        for (ICConfigurationDescription confDesc : projDesc.getConfigurations()) {
            readSloeberConfigFile(confDesc);
            setEnvVars(confDesc, getEnvVars(confDesc), true);
        }
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

                    ArduinoProjectDescription arduinoProjectDescriptor = getArduinoProjectDescription(newProjectHandle);

                    for (ICConfigurationDescription curConfig : prjCDesc.getConfigurations()) {
                        // Even though we use the same boardDescriptor for all configurations during
                        // project creation
                        // we need to add them config per config because of the include linking
                        Helpers.addArduinoCodeToProject(boardDescriptor, curConfig);

                        arduinoProjectDescriptor.setCompileDescription(curConfig, compileDescriptor);
                        arduinoProjectDescriptor.setBoardDescription(curConfig, boardDescriptor);

                        Libraries.addLibrariesToProject(newProjectHandle, curConfig, librariesToAdd);
                    }

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

    private void saveConfig(ICConfigurationDescription confDesc) {

        createSloeberConfigFile(confDesc);
        setEnvVars(confDesc, getEnvVars(confDesc), true);

    }

    private HashMap<String, String> getEnvVars(ICConfigurationDescription confDesc) {

        BoardDescription boardDescription = myBoardDescriptions.get(confDesc.getName());
        CompileDescription compileOptions = myCompileDescriptions.get(confDesc.getName());

        HashMap<String, String> allVars = new HashMap<>();

        allVars.put(ENV_KEY_BUILD_SOURCE_PATH, myProject.getLocation().toOSString());

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

    private void readSloeberConfigFile(ICConfigurationDescription confDesc) {
        String confDescName = confDesc.getName();
        IFile file = myProject.getFile("sloeber." + confDescName + ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
        if (file.exists()) {
            TxtFile configFile = new TxtFile(file.getLocation().toFile());
            BoardDescription boardDescription = new BoardDescription(configFile);
            CompileDescription compileDescription = new CompileDescription(configFile);
            myBoardDescriptions.put(confDescName, boardDescription);
            myCompileDescriptions.put(confDescName, compileDescription);
        }

    }

    private void createSloeberConfigFile(ICConfigurationDescription confDesc) {
        BoardDescription boardDescription = myBoardDescriptions.get(confDesc.getName());
        CompileDescription compileDescription = myCompileDescriptions.get(confDesc.getName());
        Map<String, String> configVars = new HashMap<>();
        if (boardDescription != null) {
            configVars = boardDescription.getEnvVarsConfig();
        }
        if (compileDescription == null) {
            compileDescription = new CompileDescription();
        }
        configVars.putAll(compileDescription.getEnvVarsConfig());
        String newFileContent = EMPTY;
        for (Entry<String, String> curLine : configVars.entrySet()) {
            newFileContent += curLine.getKey() + '=' + curLine.getValue() + '\n';
        }
        IFile file = myProject.getFile("sloeber." + confDesc.getName() + ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
        try {
            file.refreshLocal(IResource.DEPTH_INFINITE, null);
            if (file.exists()) {
                file.delete(true, null);
                file.refreshLocal(IResource.DEPTH_INFINITE, null);
            }

            if (!file.exists() && (!newFileContent.isBlank())) {
                ByteArrayInputStream stream = new ByteArrayInputStream(newFileContent.getBytes());
                file.create(stream, true, null);
            }
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
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
            boolean isRebuildNeeded = true;
            BoardDescription oldBoardDescription = myBoardDescriptions.get(confDesc.getName());
            if (oldBoardDescription != null) {
                isRebuildNeeded = oldBoardDescription.needsRebuild(boardDescription);
            }
            Helpers.addArduinoCodeToProject(boardDescription, confDesc);

            isRebuildNeeded = isRebuildNeeded || Helpers.removeInvalidIncludeFolders(confDesc);
            myBoardDescriptions.put(confDesc.getName(), boardDescription);
            saveConfig(confDesc);
            if (isRebuildNeeded) {
                Helpers.setDirtyFlag(myProject, confDesc);
            } else {
                Common.log(new Status(IStatus.INFO, io.sloeber.core.Activator.getId(),
                        "Ignoring project update; clean may be required: " + myProject.getName())); //$NON-NLS-1$
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
    public static ArduinoProjectDescription getArduinoProjectDescription(IProject project) {
        if (project.isOpen() && project.getLocation().toFile().exists()) {
            if (Sketch.isSketch(project)) {
                ArduinoProjectDescription ret = ArduinoProjectList.get(project.getName());
                if (null == ret) {
                    ret = new ArduinoProjectDescription(project);
                }
                ArduinoProjectList.put(project.getName(), ret);
                return ret;
            }
        }
        return null;
    }

    public void setCompileDescription(ICConfigurationDescription confDesc, CompileDescription compileDescription) {
        try {
            boolean isRebuildNeeded = true;
            CompileDescription oldCompileDescription = myCompileDescriptions.get(confDesc.getName());
            if (oldCompileDescription != null) {
                isRebuildNeeded = oldCompileDescription.needsRebuild(compileDescription);
            }

            myCompileDescriptions.put(confDesc.getName(), compileDescription);
            saveConfig(confDesc);
            if (isRebuildNeeded) {
                Helpers.setDirtyFlag(myProject, confDesc);
            } else {
                Common.log(new Status(IStatus.INFO, io.sloeber.core.Activator.getId(),
                        "Ignoring project update; clean may be required: " + myProject.getName())); //$NON-NLS-1$
            }
        } catch (Exception e) {
            e.printStackTrace();
            Common.log(new Status(IStatus.ERROR, io.sloeber.core.Activator.getId(), "failed to save the board settings", //$NON-NLS-1$
                    e));
        }
    }

    public BoardDescription getBoardDescription(ICConfigurationDescription confDesc) {
        return myBoardDescriptions.get(confDesc.getName());
    }

    public CompileDescription getCompileDescription(ICConfigurationDescription confDesc) {
        return myCompileDescriptions.get(confDesc.getName());
    }

    public String getDecoratedText(String text) {
        ICProjectDescription projDesc = CCorePlugin.getDefault().getProjectDescription(myProject);
        ICConfigurationDescription configurationDescription = projDesc.getActiveConfiguration();
        BoardDescription boardDescriptor = getBoardDescription(configurationDescription);
        if (boardDescriptor == null) {
            return text + " Project needs to be configured";
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
