package io.sloeber.autoBuild.regression;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.helpers.TemplateTestCodeProvider;

@SuppressWarnings({ "static-method", "nls" })
public class regression {
    static private String extensionPointID = "io.sloeber.autoBuild.buildDefinitions";
    static int testCounter = 1;

    @BeforeAll
    public static void beforeAll() throws CoreException {
        Shared.setDeleteProjects(false);
        Shared.setCloseProjects(false);
        // turn off auto building to make sure autobuild does not start a build behind our backs
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceDescription workspaceDesc = workspace.getDescription();
        workspaceDesc.setAutoBuilding(false);
        workspace.setDescription(workspaceDesc);
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

}
