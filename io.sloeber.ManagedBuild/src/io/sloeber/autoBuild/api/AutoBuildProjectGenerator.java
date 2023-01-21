
package io.sloeber.autoBuild.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tools.templates.core.IGenerator;

import io.sloeber.autoBuild.Internal.ManagedBuildInfo;
import io.sloeber.autoBuild.Internal.ManagedBuildManager;
import io.sloeber.autoBuild.core.Activator;
import io.sloeber.autoBuild.integration.AutoBuildNature;
import io.sloeber.autoBuild.integration.BuildConfigurationData;
import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.IManagedProject;
import io.sloeber.schema.api.IProjectType;
import io.sloeber.schema.internal.Configuration;
import io.sloeber.schema.internal.ManagedProject;

public class AutoBuildProjectGenerator implements IGenerator {
    private URI myProjectURI = null;
    private String myProjectName = null;
    private IProject myProject = null;

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
                myProject = root.getProject(myProjectName);
                myProject.create(description, monitor);
                myProject.open(monitor);
                CProjectNature.addCNature(myProject, monitor);
                CCProjectNature.addCCNature(myProject, monitor);
                AutoBuildNature.addNature(myProject, monitor);

                //TOFIX Start of stupid code creation 
                IFolder srcFolder = myProject.getFolder("src");
                srcFolder.create(true, true, monitor);
                IFile mainFile = srcFolder.getFile("main.cpp");
                InputStream stream = new ByteArrayInputStream("void main(){}".getBytes(StandardCharsets.UTF_8));
                mainFile.create(stream, true, monitor);
                //End of stupid code creation

                myProject = CCorePlugin.getDefault().createCDTProject(description, myProject, monitor);

                ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
                ICProjectDescription des = mngr.createProjectDescription(myProject, false, true);
                ManagedBuildInfo info = ManagedBuildManager.createBuildInfo(myProject);

                IProjectType sloeberProjType = ManagedBuildManager.getProjectType(
                        "io.sloeber.autoBuild.buildDefinitions", "io.sloeber.builddef", "io.sloeber.core.sketch", true); //$NON-NLS-1$
                IConfiguration[] modelConfigs = sloeberProjType.getConfigurations();
                Configuration cf = (Configuration) modelConfigs[0];
                ManagedProject mProj = new ManagedProject(myProject, cf.getProjectType());
                info.setManagedProject( mProj);
                for (IConfiguration iConfig : modelConfigs) {
                    //        for (CfgHolder cfg : cfgs) {
                	Configuration config = (Configuration) iConfig;
                    //String id = ManagedBuildManager.calculateChildId(config.getId(), null);
                   // Configuration config = new Configuration(mProj, cf, id, false, true);
                    CConfigurationData data =  new BuildConfigurationData(config,myProject);
                    assert(data!=null);
                    //ICConfigurationDescription cfgDes = 
                    		des.createConfiguration(ManagedBuildManager.CFG_DATA_PROVIDER_ID, data);
//                    config.setConfigurationDescription(cfgDes);
//                    config.exportArtifactInfo();

//                    IBuilder bld = config.getBuilder();
//                    if (bld != null) {
//                        bld.setManagedBuildOn(true);
//                    }


                }
                des.setCdtProjectCreated();
                mngr.setProjectDescription(myProject, des);
            }
        };
        try {
            workspace.run(runnable, root, IWorkspace.AVOID_UPDATE, monitor);
        } catch (Exception e) {
            Activator.log(e);
        }
        monitor.done();
    }

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

    public IProject getProject() {
        return myProject;
    }

}