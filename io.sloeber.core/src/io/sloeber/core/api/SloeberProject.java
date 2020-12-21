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
import org.eclipse.core.resources.IFolder;
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
    private Map<String, OtherDescription> myOtherDescriptions = new HashMap<>();
    private TxtFile myCfgFile = null;
    private IProject myProject = null;
    private boolean isConfigured = false;
    private boolean isDirty = false; // if anything has changed

    private static final String ENV_KEY_BUILD_SOURCE_PATH = ERASE_START + "build.source.path"; //$NON-NLS-1$
    private static final String ENV_KEY_BUILD_GENERIC_PATH = ERASE_START + "build.generic.path"; //$NON-NLS-1$
    private static final String ENV_KEY_COMPILER_PATH = ERASE_START + "compiler.path"; //$NON-NLS-1$
    private static final String ENV_KEY_JANTJE_MAKE_LOCATION = ENV_KEY_JANTJE_START + "make_location"; //$NON-NLS-1$
    private static final String CONFIG_DOT = "Config.";//$NON-NLS-1$

    private SloeberProject(IProject project, boolean isProjectCreating) {
        myProject = project;
        try {
            project.setSessionProperty(sloeberQualifiedName, this);
        } catch (CoreException e) {
            e.printStackTrace();
        }
        if (isProjectCreating) {
            return;
        }
        configureUpdateProject();

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
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        ICoreRunnable runnable = new ICoreRunnable() {
            @Override
            public void run(IProgressMonitor internalMonitor) throws CoreException {
                IProject newProjectHandle = root.getProject(realProjectName);
                IndexerController.doNotIndex(newProjectHandle);
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
                    Map<String, IPath> librariesToAdd = codeDesc.createFiles(newProjectHandle,
                            new NullProgressMonitor());

                    CCorePlugin cCorePlugin = CCorePlugin.getDefault();
                    ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(newProjectHandle);

                    SloeberProject arduinoProjDesc = new SloeberProject(newProjectHandle, true);
                    for (ICConfigurationDescription curConfig : prjCDesc.getConfigurations()) {
                        // Even though we use the same boardDescriptor for all configurations during
                        // project creation
                        // we need to add them config per config because of the include linking
                        Helpers.addArduinoCodeToProject(boardDescriptor, curConfig);

                        arduinoProjDesc.myCompileDescriptions.put(curConfig.getId(), compileDescriptor);
                        arduinoProjDesc.myBoardDescriptions.put(curConfig.getId(), boardDescriptor);
                        arduinoProjDesc.myOtherDescriptions.put(curConfig.getId(), otherDesc);

                        setEnvVars(curConfig, arduinoProjDesc.getEnvVars(curConfig));
                        Libraries.addLibrariesToProject(newProjectHandle, curConfig, librariesToAdd);
                    }

                    arduinoProjDesc.createSloeberConfigFiles(prjCDesc);
                    ;
                    SubMonitor refreshMonitor = SubMonitor.convert(internalMonitor, 3);
                    newProjectHandle.open(refreshMonitor);
                    newProjectHandle.refreshLocal(IResource.DEPTH_INFINITE, refreshMonitor);
                    cCorePlugin.setProjectDescription(newProjectHandle, prjCDesc, true, null);

                } catch (Exception e) {
                    Common.log(new Status(IStatus.INFO, io.sloeber.core.Activator.getId(),
                            "Project creation failed: " + realProjectName, e)); //$NON-NLS-1$
                }
                Common.log(new Status(Const.SLOEBER_STATUS_DEBUG, Activator.getId(),
                        "internal creation of project is done: " + realProjectName)); //$NON-NLS-1$
                IndexerController.Index(newProjectHandle);
            }
        };

        try {
            workspace.run(runnable, root, IWorkspace.AVOID_UPDATE, monitor);
        } catch (Exception e) {
            Common.log(new Status(IStatus.INFO, io.sloeber.core.Activator.getId(),
                    "Project creation failed: " + realProjectName, e)); //$NON-NLS-1$
        }
        monitor.done();
        return root.getProject(realProjectName);
    }


    private HashMap<String, String> getEnvVars(ICConfigurationDescription confDesc) {
        IProject project = confDesc.getProjectDescription().getProject();

        BoardDescription boardDescription = myBoardDescriptions.get(confDesc.getId());
        CompileDescription compileOptions = myCompileDescriptions.get(confDesc.getId());
        OtherDescription otherOptions = myOtherDescriptions.get(confDesc.getId());

        HashMap<String, String> allVars = new HashMap<>();

        allVars.put(ENV_KEY_BUILD_SOURCE_PATH, project.getLocation().toOSString());

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

    private void configureUpdateProject() {

        CCorePlugin cCorePlugin = CCorePlugin.getDefault();
        ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(myProject);
        if (configureUpdateProject(prjCDesc)) {
            try {
                cCorePlugin.setProjectDescription(myProject, prjCDesc, true, null);
            } catch (CoreException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private boolean configureUpdateProject(ICProjectDescription prjCDesc) {
        boolean prjDescChanged = false;
        if (isConfigured) {
            if (isDirty) {
                createSloeberConfigFiles(prjCDesc);

                setActiveConfig(prjCDesc.getActiveConfiguration());
                prjDescChanged = true;
                isDirty = false;
            }
            return prjDescChanged;
        }
        readSloeberConfig(prjCDesc);
        if (isDirty) {
            createSloeberConfigFiles(prjCDesc);
            isDirty = false;
        }
        isConfigured = true;
        return true;
    }

    /**
     * Read the configuration needed to setup the project First try the
     * configuration files If they do not exist try the old Sloeber CDT environment
     * variable way
     * 
     * @param confDesc
     *            returns true if the config needs saving otherwise false
     */
    private void readSloeberConfig(ICProjectDescription prjCDesc) {
        IFile file = getConfigLocalFile();
        if (file.exists()) {
            myCfgFile = new TxtFile(file.getLocation().toFile());
            IFile versionFile = getConfigVersionFile();
            if (versionFile.exists()) {
                myCfgFile.mergeFile(versionFile.getLocation().toFile());
            }
            for (ICConfigurationDescription confDesc : prjCDesc.getConfigurations()) {
                BoardDescription boardDesc = new BoardDescription(myCfgFile, getBoardPrefix(confDesc));
                CompileDescription compileDescription = new CompileDescription(myCfgFile, getCompilePrefix(confDesc));
                OtherDescription otherDesc = new OtherDescription(myCfgFile, getOtherPrefix(confDesc));
                myBoardDescriptions.put(confDesc.getId(), boardDesc);
                myCompileDescriptions.put(confDesc.getId(), compileDescription);
                myOtherDescriptions.put(confDesc.getId(), otherDesc);
            }

        } else {
            // no config files exist. try to save the day
            if (IndexerController.isPosponed(myProject)) {
                Common.log(new Status(IStatus.ERROR, io.sloeber.core.Activator.getId(),
                        "Trying to get the configuration during project creation? This should not happen!!")); //$NON-NLS-1$
            } else {
                // Maybe this is a old Sloeber project with the data in the eclipse build
                // environment variables
                isDirty = true;
                for (ICConfigurationDescription confDesc : prjCDesc.getConfigurations()) {

                    BoardDescription boardDesc = BoardDescription.getFromCDT(confDesc);
                    CompileDescription compileDescription = CompileDescription.getFromCDT(confDesc);
                    OtherDescription otherDesc = OtherDescription.getFromCDT(confDesc);
                    myBoardDescriptions.put(confDesc.getId(), boardDesc);
                    myCompileDescriptions.put(confDesc.getId(), compileDescription);
                    myOtherDescriptions.put(confDesc.getId(), otherDesc);
                }
                // remove environment variables from the old sloeber way
                IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
                IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
                for (ICConfigurationDescription confDesc : prjCDesc.getConfigurations()) {
                    IEnvironmentVariable[] CurVariables = contribEnv.getVariables(confDesc);
                    for (int i = (CurVariables.length - 1); i > 0; i--) {
                        if (CurVariables[i].getName().startsWith(Const.ERASE_START)) {
                            contribEnv.removeVariable(CurVariables[i].getName(), confDesc);
                        }
                        if (CurVariables[i].getName().startsWith(Const.ENV_KEY_JANTJE_START)) {
                            contribEnv.removeVariable(CurVariables[i].getName(), confDesc);
                        }
                    }
                }
            }
        }
        setActiveConfig(prjCDesc.getActiveConfiguration());
    }

    private void setActiveConfig(ICConfigurationDescription confDesc) {
        try {
            BoardDescription boardDescription = myBoardDescriptions.get(confDesc.getId());

            Helpers.addArduinoCodeToProject(boardDescription, confDesc);

            boolean isRebuildNeeded = Helpers.removeInvalidIncludeFolders(confDesc);

            if (isRebuildNeeded) {
                Helpers.deleteBuildFolder(myProject, confDesc);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Common.log(new Status(IStatus.ERROR, io.sloeber.core.Activator.getId(), "failed to save the board settings", //$NON-NLS-1$
                    e));
        }
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
    private void createSloeberConfigFiles(ICProjectDescription prjCDesc) {

        Map<String, String> configVars = new TreeMap<>();
        Map<String, String> versionVars = new TreeMap<>();

        for (ICConfigurationDescription confDesc : prjCDesc.getConfigurations()) {
            BoardDescription boardDescription = myBoardDescriptions.get(confDesc.getId());
            CompileDescription compileDescription = myCompileDescriptions.get(confDesc.getId());
            OtherDescription otherDescription = myOtherDescriptions.get(confDesc.getId());
            String boardPrefix = getBoardPrefix(confDesc);
            String compPrefix = getCompilePrefix(confDesc);
            String otherPrefix = getOtherPrefix(confDesc);

            configVars.putAll(boardDescription.getEnvVarsConfig(boardPrefix));
            configVars.putAll(compileDescription.getEnvVarsConfig(compPrefix));
            configVars.putAll(otherDescription.getEnvVarsConfig(otherPrefix));

            if (otherDescription.IsVersionControlled()) {
                versionVars.putAll(boardDescription.getEnvVarsConfig(boardPrefix));
                versionVars.putAll(compileDescription.getEnvVarsConfig(compPrefix));
                versionVars.putAll(otherDescription.getEnvVarsConfig(otherPrefix));
            }
            setEnvVars(confDesc, getEnvVars(confDesc));
        }

        try {
            storeConfigurationFile(getConfigVersionFile(), versionVars);
            storeConfigurationFile(getConfigLocalFile(), configVars);
        } catch (CoreException e) {
            Common.log(new Status(IStatus.ERROR, io.sloeber.core.Activator.getId(),
                    "failed to save the sloeber config files", e)); //$NON-NLS-1$
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
     * method to switch the board in a given configuration This method assumes the
     * configuration description is a valid arduino confiuguration description and
     * only the board descriptor changed
     * 
     * @param confDesc
     * @param boardDescription
     */
    public void setBoardDescription(ICConfigurationDescription confDesc, BoardDescription boardDescription,
            boolean force) {
        BoardDescription oldBoardDescription = myBoardDescriptions.get(confDesc.getId());
        if (!force) {
            if (boardDescription.equals(oldBoardDescription)) {
                return;
            }
        }
        if (boardDescription.needsRebuild(oldBoardDescription)) {
            Helpers.deleteBuildFolder(myProject, confDesc);
        }
        myBoardDescriptions.put(confDesc.getId(), boardDescription);
        isDirty = true;
    }

    private static void setEnvVars(ICConfigurationDescription confDesc, Map<String, String> envVars) {
        IConfiguration configuration = ManagedBuildManager.getConfigurationForDescription(confDesc);
        if (configuration != null) {
            SloeberConfigurationVariableSupplier.setEnvVars(configuration, envVars);
        }
    }

    /**
     * get the Arduino project description based on a project description
     * Convenience method for getSloeberProject(project, false);
     * 
     * @param project
     * @return
     */
    public static synchronized SloeberProject getSloeberProject(IProject project) {
        return getSloeberProject(project, false);
    }

    /**
     * get the Arduino project description based on a project description
     * 
     * @param project
     * @param allowNull
     *            set true if a null response is ok
     * @return
     */
    public static synchronized SloeberProject getSloeberProject(IProject project, boolean allowNull) {

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
                if (!allowNull) {
                    SloeberProject ret = new SloeberProject(project, false);
                    return ret;
                }
            }
        }
        return null;
    }

    public void setCompileDescription(ICConfigurationDescription confDesc, CompileDescription compileDescription) {
        IProject project = confDesc.getProjectDescription().getProject();
        IFolder buildFolder = project.getFolder(confDesc.getName());

        if (buildFolder.exists()) {
            CompileDescription oldCompileDescription = myCompileDescriptions.get(confDesc.getId());
            if (compileDescription.needsRebuild(oldCompileDescription)) {
                Helpers.deleteBuildFolder(project, confDesc);
            }
        }
        myCompileDescriptions.put(confDesc.getId(), compileDescription);
        isDirty = true;
    }

    public void setOtherDescription(ICConfigurationDescription confDesc, OtherDescription otherDesc) {
        try {
            myOtherDescriptions.put(confDesc.getId(), otherDesc);
            isDirty = true;
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
    public BoardDescription getBoardDescription(ICConfigurationDescription confDesc, boolean allowNull) {
        if (!allowNull) {
        configureUpdateProject();
        }
        return myBoardDescriptions.get(confDesc.getId());
    }

    public CompileDescription getCompileDescription(ICConfigurationDescription confDesc, boolean allowNull) {
        if (!allowNull) {
        configureUpdateProject();
        }
        return myCompileDescriptions.get(confDesc.getId());
    }


    public OtherDescription getOtherDescription(ICConfigurationDescription confDesc, boolean allowNull) {
        if (!allowNull) {
        configureUpdateProject();
        }
        return myOtherDescriptions.get(confDesc.getId());
    }

    public String getDecoratedText(ICConfigurationDescription confDesc, String text) {
        // do not use getBoardDescriptor below as this will cause a infinite loop at
        // project creation
        BoardDescription boardDescriptor = myBoardDescriptions.get(confDesc.getId());
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

    private static String getBoardPrefix(ICConfigurationDescription confDesc) {
        return CONFIG_DOT + confDesc.getName() + DOT + "board."; //$NON-NLS-1$
    }

    private static String getCompilePrefix(ICConfigurationDescription confDesc) {
        return CONFIG_DOT + confDesc.getName() + DOT + "compile."; //$NON-NLS-1$
    }

    private static String getOtherPrefix(ICConfigurationDescription confDesc) {
        return CONFIG_DOT + confDesc.getName() + DOT + "other."; //$NON-NLS-1$
    }

    private IFile getConfigVersionFile() {
        return myProject.getFile("sloeber.cfg"); //$NON-NLS-1$
    }

    private IFile getConfigLocalFile() {
        return myProject.getFile(".sproject"); //$NON-NLS-1$
    }

    public void configChangeAboutToApply(ICProjectDescription newProjDesc, ICProjectDescription oldProjDesc) {
        ICConfigurationDescription newActiveConfig = newProjDesc.getActiveConfiguration();
        ICConfigurationDescription oldActiveConfig = oldProjDesc.getActiveConfiguration();
        if (!newActiveConfig.getName().equals(oldActiveConfig.getName())) {
            configureUpdateProject(newProjDesc);
            setActiveConfig(newActiveConfig);
        } else {
            configureUpdateProject(newProjDesc);
        }

    }

}
