package io.sloeber.autoBuild.regression;

import static org.junit.Assert.*;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.helpers.TemplateTestCodeProvider;

@SuppressWarnings({ "static-method", "nls" })
public class regression {
    static private String extensionPointID = "io.sloeber.autoBuild.buildDefinitions";
    static int testCounter = 1;

    @BeforeAll
    public static void beforeAll() {
        Shared.setDeleteProjects(false);
        Shared.setCloseProjects(false);
        // turn off auto building to make sure autobuild does not start a build behind our backs
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceDescription workspaceDesc = workspace.getDescription();
        workspaceDesc.setAutoBuilding(false);
        try {
            workspace.setDescription(workspaceDesc);
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /*
     * Create a project build it
     * clean it
     * close it
     * open it
     * build it
     * 
     */
    @Test
    public void createCloseOpenProject() throws Exception {
        beforeAll();// for one reason or another the beforeall is not called
        String projectName = "createCloseOpenProject";

        IProject testProject = AutoBuildProject.createProject(projectName, extensionPointID, "cdt.cross.gnu",
                "cdt.managedbuild.target.gnu.cross.exe", CCProjectNature.CC_NATURE_ID,
                new TemplateTestCodeProvider("exe"), false, null);

        //Build all the configurations and verify proper building
        Shared.buildAndVerifyProjectUsingActivConfig(testProject, null, null);
        //clean all configurations and verify clean has been done properly
        Shared.cleanProject(testProject);

        //close the project
        testProject.close(new NullProgressMonitor());
        //wait a while
        Thread.sleep(5000);
        //open the project 
        testProject.open(new NullProgressMonitor());
        //Build all the configurations and verify proper building
        Shared.buildAndVerifyProjectUsingActivConfig(testProject, null, null);
    }

    /*
     * Create a project build it
     * check whether there is a makefile
     * if so set internal builder else set external builder
     * clean it
     * build
     * check for makefile existence
     * clean it
     * close it
     * open it
     * build it
     * check for makefile existence
     * 
     */
    @Test
    public void setBuilder() throws Exception {
        beforeAll();// for one reason or another the before all is not called
        String projectName = "setBuilder";

        IProject testProject = AutoBuildProject.createProject(projectName, extensionPointID, "cdt.cross.gnu",
                "cdt.managedbuild.target.gnu.cross.exe", CCProjectNature.CC_NATURE_ID,
                new TemplateTestCodeProvider("exe"), false, null);

        //Build the active configuration and verify proper building
        Shared.BuildAndVerifyActiveConfig(testProject);

        ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
        ICProjectDescription projectDescription = mngr.getProjectDescription(testProject, true);
        IAutoBuildConfigurationDescription activeConfig = IAutoBuildConfigurationDescription
                .getActiveConfig(projectDescription);

        IFile makeFile = activeConfig.getBuildFolder().getFile("makefile");
        boolean hasMakefile = makeFile.exists();
        if (hasMakefile) {
            activeConfig.setBuildRunner(AutoBuildProject.ARGS_INTERNAL_BUILDER_KEY);
        } else {
            activeConfig.setBuildRunner(AutoBuildProject.ARGS_MAKE_BUILDER_KEY);
        }
        //clean all configurations and verify clean has been done properly
        Shared.cleanConfiguration(activeConfig);
        //do the clean before the builderswitch otherwise the makefile in the buildroot will make the test fail
        mngr.setProjectDescription(testProject, projectDescription, true, new NullProgressMonitor());

        //Build all the configurations and verify proper building
        Shared.BuildAndVerifyActiveConfig(testProject);

        assertNotEquals("Builder changes have not been taken into account", makeFile.exists(), hasMakefile);

        //clean all configurations and verify clean has been done properly
        Shared.cleanConfiguration(activeConfig);

        //close the project
        testProject.close(new NullProgressMonitor());
        //wait a while
        Thread.sleep(5000);
        //open the project 
        testProject.open(new NullProgressMonitor());
        //Build all the configurations and verify proper building
        Shared.BuildAndVerifyActiveConfig(testProject);

        assertNotEquals("Builder changes have been lost in open close projects", makeFile.exists(), hasMakefile);
    }

}
