package io.sloeber.core.api;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.EnvironmentVariable;
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
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.Const;
import io.sloeber.core.listeners.IndexerController;
import io.sloeber.core.tools.Helpers;
import io.sloeber.core.tools.Libraries;

public class ArduinoProjectDescription extends Common {
    static private Map<String, ArduinoProjectDescription> ArduinoProjectList = new HashMap<>();
    private Map<ICConfigurationDescription, BoardDescription> myBoardDescriptions = new HashMap<>();
    private Map<ICConfigurationDescription, CompileDescription> myCompileDescriptions = new HashMap<>();
    private IProject myProject = null;

    private static final String ENV_KEY_BUILD_SOURCE_PATH = ERASE_START + "build.source.path"; //$NON-NLS-1$
    private static final String ENV_KEY_BUILD_GENERIC_PATH = ERASE_START + "build.generic.path"; //$NON-NLS-1$
    private static final String ENV_KEY_COMPILER_PATH = ERASE_START + "compiler.path"; //$NON-NLS-1$
    private static final String ENV_KEY_JANTJE_MAKE_LOCATION = ENV_KEY_JANTJE_START + "make_location"; //$NON-NLS-1$

    private ArduinoProjectDescription(IProject project) {
        myProject = project;
    }

    /*
     * Method to create a project based on the board
     */
    public static IProject createArduinoProject(String projectName, URI projectURI, BoardDescription boardDescriptor,
            CodeDescription codeDescription, CompileDescription compileDescriptor,
            IProgressMonitor monitor) {

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
                        curConfig.getEditableBuilder()
                                .setParallelBuildOn(compileDescriptor.isParallelBuildEnabled());
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
                        Helpers.addArduinoCodeToProject(boardDescriptor, newProjectHandle, curConfig);
                        arduinoProjectDescriptor.saveConfig(curConfig, boardDescriptor, compileDescriptor);
                        createSloeberConfigFile(curConfig, boardDescriptor);
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



    private void saveConfig(ICConfigurationDescription confDesc, BoardDescription boardDescription,
            CompileDescription compileOptions) {

        HashMap<String, String> allVars = new HashMap<>();

        allVars.put(ENV_KEY_BUILD_SOURCE_PATH, myProject.getLocation().toOSString());

        myBoardDescriptions.put(confDesc, boardDescription);
        myCompileDescriptions.put(confDesc, compileOptions);
        allVars.putAll(boardDescription.getEnvVars());
        allVars.putAll(compileOptions.getEnvVars(confDesc));


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


        setEnvVars(confDesc, allVars, true);

    }

    private static void createSloeberConfigFile(ICConfigurationDescription confDesc, BoardDescription boardDescription) {
        Map<String, String> configVars = boardDescription.getEnvVarsConfig();
        String newFileContent = EMPTY;
        for (Entry<String, String> curLine : configVars.entrySet()) {
            newFileContent += curLine.getKey() + '=' + curLine.getValue() + '\n';
        }
        IProject curProject = confDesc.getProjectDescription().getProject();
        IFile file = curProject.getFile("sloeber." + confDesc.getName() + ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
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
     * @param boardDescriptor
     */
    public void setBoardDescriptor(ICConfigurationDescription confDesc, BoardDescription boardDescriptor) {
        try {
            createSloeberConfigFile(confDesc, boardDescriptor);
            Map<String, String> envVars = boardDescriptor.getEnvVars();
            setEnvVars(confDesc, envVars, false);
            if (Helpers.removeInvalidIncludeFolders(confDesc)) {
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

    private static void setEnvVars(ICConfigurationDescription confDesc, Map<String, String> envVars,
            boolean eraseFirst) {
        IEnvironmentVariableManager envManager = CCorePlugin.getDefault().getBuildEnvironmentManager();
        IContributedEnvironment contribEnv = envManager.getContributedEnvironment();
        if (eraseFirst) {
            // remove all Arduino Variables so there is no memory effect
            removeAllEraseEnvironmentVariables(contribEnv, confDesc);
        }

        for (Entry<String, String> curVariable : envVars.entrySet()) {
            String name = curVariable.getKey();
            String value = curVariable.getValue();
            IEnvironmentVariable var = new EnvironmentVariable(name, makePathEnvironmentString(value));
            contribEnv.addVariable(var, confDesc);
        }

    }

    /**
     * get the Arduino project description based on a project description
     * 
     * @param project
     * @return
     */
    public static ArduinoProjectDescription getArduinoProjectDescription(IProject project) {

        ArduinoProjectDescription ret = ArduinoProjectList.get(project.getName());
        if (null == ret) {
            ret = new ArduinoProjectDescription(project);
        }
        ArduinoProjectList.put(project.getName(), ret);
        return ret;
    }


}
