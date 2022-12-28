
package io.sloeber.autoBuild.api;

import java.net.URI;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tools.templates.core.IGenerator;

import io.sloeber.autoBuild.Internal.Configuration;
import io.sloeber.autoBuild.Internal.ManagedBuildInfo;
import io.sloeber.autoBuild.Internal.ManagedBuildManager;
import io.sloeber.autoBuild.Internal.ManagedProject;
import io.sloeber.autoBuild.core.Activator;

public class AutoBuildProjectGenerator implements IGenerator {
    private URI myProjectURI = null;
    private String myProjectName = null;

    public AutoBuildProjectGenerator() {

    }

    @Override
    public void generate(IProgressMonitor monitor) throws CoreException {
        IGenerator.super.generate(monitor);
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        ICoreRunnable runnable = new ICoreRunnable() {
            @Override
            public void run(IProgressMonitor internalMonitor) throws CoreException {
                IProjectDescription description = workspace.newProjectDescription(myProjectName);
                if (myProjectURI != null) {
                    description.setLocationURI(myProjectURI);
                }
                IProject project = root.getProject(myProjectName);
                project.create(description, monitor);
                project.open(monitor);
                project = CCorePlugin.getDefault().createCDTProject(description, project, monitor);

                ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
                ICProjectDescription des = mngr.createProjectDescription(project, false, true);
                ManagedBuildInfo info = ManagedBuildManager.createBuildInfo(project);

                IProjectType sloeberProjType = ManagedBuildManager.getProjectType("io.sloeber.core.sketch"); //$NON-NLS-1$
                IConfiguration[] configs = sloeberProjType.getConfigurations();
                Configuration cf = (Configuration) configs[0];
                ManagedProject mProj = new ManagedProject(project, cf.getProjectType());
                info.setManagedProject(mProj);

                for (IConfiguration cfinter : configs) {
                    //        for (CfgHolder cfg : cfgs) {
                    cf = (Configuration) cfinter;
                    String id = ManagedBuildManager.calculateChildId(cf.getId(), null);
                    Configuration config = new Configuration(mProj, cf, id, false, true);
                    CConfigurationData data = config.getConfigurationData();
                    ICConfigurationDescription cfgDes = des
                            .createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);
                    config.setConfigurationDescription(cfgDes);
                    config.exportArtifactInfo();

                    IBuilder bld = config.getEditableBuilder();
                    if (bld != null) {
                        bld.setManagedBuildOn(true);
                    }

                    config.setName(cf.getName());
                    config.setArtifactName(mProj.getDefaultArtifactName());

                }
                mngr.setProjectDescription(project, des);
                //                IProject newProject = root.getProject(myProjectName);
                //                // IndexerController.doNotIndex(newProjectHandle);
                //
                //                // create a eclipse project
                //                IProjectDescription description = workspace.newProjectDescription(myProjectName);
                //                if (myProjectURI != null) {
                //                    description.setLocationURI(myProjectURI);
                //                }
                //                newProject.create(description, monitor);
                //                newProject.open(monitor);
                //
                //                //TOFIX Start of stupid code creation 
                //                IFolder srcFolder = newProject.getFolder("src");
                //                srcFolder.create(true, true, monitor);
                //                IFile mainFile = srcFolder.getFile("main.cpp");
                //                InputStream stream = new ByteArrayInputStream("void main(){}".getBytes(StandardCharsets.UTF_8));
                //                mainFile.create(stream, true, monitor);
                //                //End of stupid code creation
                //
                //                // make the eclipse project a cdt project
                //                CCorePlugin.getDefault().createCProject(description, newProject, new NullProgressMonitor(),
                //                        "org.eclipse.cdt.managedbuilder.core.managedMake");
                //                //                        "org.eclipse.cdt.core.cbuilder");
                //                //null);
                //                CCorePlugin.getDefault().createCDTProject(description, newProject,
                //                        "io.sloeber.autoBuild.ConfigurationDataProvider", new NullProgressMonitor());
                //                // ManagedBuilderCorePlugin.MANAGED_MAKE_PROJECT_ID
                //                // "org.eclipse.cdt.managedbuilder.core.managedMake");
                //                // "io.sloeber.autoBuild.Project.ID"
                //
                //                // add the required natures
                //                ManagedCProjectNature.addManagedBuilder(newProject, monitor);
                //                ManagedCProjectNature.addNature(newProject, "org.eclipse.cdt.core.ccnature", monitor); //$NON-NLS-1$
                //                ManagedCProjectNature.addNature(newProject, "io.sloeber.autoBuildNature", monitor); //$NON-NLS-1$
                //
                //                try {
                //                    IProjectType sloeberProjType = ManagedBuildManager.getProjectType("io.sloeber.core.sketch"); //$NON-NLS-1$
                //                    //                    ManagedBuildManager.createBuildInfo(newProject);
                //                    //                    IManagedProject newManagedProject = ManagedBuildManager.createManagedProject(newProject,
                //                    //                            sloeberProjType);
                //                    //
                //                    //                    ManagedBuildManager.setNewProjectVersion(newProject);
                //
                //                    CCorePlugin cCorePlugin = CCorePlugin.getDefault();
                //                    ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(newProject, true);
                //                    // Copy over the Sloeber configs
                //                    IConfiguration[] configs = sloeberProjType.getConfigurations();
                //                    for (IConfiguration config : configs) {
                //                        //TOFIX add correct ID
                //                        ICConfigurationDescription curConfig = prjCDesc.createConfiguration(
                //                                "io.sloeber.autoBuild.ConfigurationDataProvider", config.getConfigurationData());
                //                        //                        curConfig.setName(config.getName());
                //                        //                        curConfig.setArtifactName(newManagedProject.getDefaultArtifactName());
                //                    }
                //                    //ManagedBuildManager.setDefaultConfiguration(newProject, defaultConfig);
                //                    cCorePlugin.setProjectDescription(newProject, prjCDesc, true, null);
                //                } catch (Exception e) {
                //                    // TODO Auto-generated catch block
                //                    e.printStackTrace();
                //                }
                //
                //                // Set<String> configKeys = GetConfigKeysFromProjectDescription(prjCDesc);
                //                //
                //                // for (String curConfigKey : configKeys) {
                //                //
                //                //// arduinoProjDesc.myCompileDescriptions.put(curConfigKey, compileDescriptor);
                //                //// arduinoProjDesc.myBoardDescriptions.put(curConfigKey, boardDescriptor);
                //                //// arduinoProjDesc.myOtherDescriptions.put(curConfigKey, otherDesc);
                //                //// ICConfigurationDescription curConfigDesc =
                //                // prjCDesc.getConfigurationByName(curConfigKey);
                //                //
                //                // }
                //
            }
        };
        try {
            workspace.run(runnable, root, IWorkspace.AVOID_UPDATE, monitor);
        } catch (Exception e) {
            Activator.log(e);
        }
        monitor.done();
    }

    /* code copied from MBSWizardHandler*/

    @Override
    public IFile[] getFilesToOpen() {
        // TODO Auto-generated method stub
        return IGenerator.super.getFilesToOpen();
    }

    public void setProjectName(String projectName) {
        myProjectName = projectName;

    }

    public void setLocationURI(URI locationURI) {
        // TODO Auto-generated method stub
        myProjectURI = locationURI;
    }

}
