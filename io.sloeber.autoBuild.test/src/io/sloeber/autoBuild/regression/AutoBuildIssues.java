package io.sloeber.autoBuild.regression;

import static io.sloeber.autoBuild.helpers.Defaults.*;
import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.buildTools.api.IBuildTools;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager;
import io.sloeber.autoBuild.helpers.Shared;

@SuppressWarnings({ "nls", "static-method" })
public class AutoBuildIssues {

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

    @Test
    public void cxx_associates_with_c() {
        IContentTypeManager manager = Platform.getContentTypeManager();
        IContentType contentType = manager.getContentType("org.eclipse.cdt.core.cxxSource");
        if (contentType.isAssociatedWith("test.c")) {
            fail("org.eclipse.cdt.core.cxxSource should not associate with c files");

        }
    }

    @Test
    public void cxx_associates_with_cpp() {
        IContentTypeManager manager = Platform.getContentTypeManager();
        IContentType contentType = manager.getContentType("org.eclipse.cdt.core.cxxSource");
        if (!contentType.isAssociatedWith("test.cpp")) {
            fail("org.eclipse.cdt.core.cxxSource should associate with cpp files");

        }
    }

    @Test
    public void cxx_associates_with_C() {
        IContentTypeManager manager = Platform.getContentTypeManager();
        IContentType contentType = manager.getContentType("org.eclipse.cdt.core.cxxSource");
        if (!contentType.isAssociatedWith("test.C")) {
            fail("org.eclipse.cdt.core.cxxSource should associate with C files");

        }
    }

    @Test
    public void cxx_associates_with_CPP() {
        IContentTypeManager manager = Platform.getContentTypeManager();
        IContentType contentType = manager.getContentType("org.eclipse.cdt.core.cxxSource");
        if (contentType.isAssociatedWith("test.CPP")) {
            fail("org.eclipse.cdt.core.cxxSource should not associate with CPP files");

        }
    }

    @Test
    public void c_associates_with_C() {
        IContentTypeManager manager = Platform.getContentTypeManager();
        IContentType contentType = manager.getContentType("org.eclipse.cdt.core.cSource");
        if (contentType.isAssociatedWith("test.C")) {
            fail("org.eclipse.cdt.core.cxxSource should not associate with CPP files");

        }
    }

    @Test
    public void setDescriptionDoesNotSetDescription() throws Exception {
        String projectName = "setDescription";
        CoreModel coreModel = CoreModel.getDefault();

        IBuildTools buildTools = IBuildToolsManager.getDefault().getAnyInstalledBuildTools(defaultProjectType);
        IProject testProject = AutoBuildProject.createProject(projectName, defaultProjectType, defaultNatureID,cpp_exeCodeProvider,
        		buildTools, false, null);

        ICProjectDescription cProjectDesc = CCorePlugin.getDefault().getProjectDescription(testProject, true);
        for (ICConfigurationDescription curConfig : cProjectDesc.getConfigurations()) {
            //Set the active configuration
            cProjectDesc.setActiveConfiguration(curConfig);
            coreModel.setProjectDescription(testProject, cProjectDesc);

            //get the active configuration from the project
            ICProjectDescription readProjectDescription = coreModel.getProjectDescription(testProject, false);
            ICConfigurationDescription actualActiveConfig = readProjectDescription.getActiveConfiguration();
            //Make sure the active config has been set
            assertEquals("After project create", curConfig.getName(), actualActiveConfig.getName());

        }

        //open and close the project and try again
        Thread.sleep(5000);
        //close the project
        testProject.close(new NullProgressMonitor());
        //wait a while
        Thread.sleep(5000);
        //open the project
        testProject.open(new NullProgressMonitor());

        ICProjectDescription cProjectDescOpen = CCorePlugin.getDefault().getProjectDescription(testProject, true);
        for (ICConfigurationDescription curConfig : cProjectDescOpen.getConfigurations()) {
            //Set the active configuration
            cProjectDescOpen.setActiveConfiguration(curConfig);
            coreModel.setProjectDescription(testProject, cProjectDescOpen);

            //get the active configuration from the project
            ICProjectDescription readProjectDescription = coreModel.getProjectDescription(testProject, false);
            ICConfigurationDescription actualActiveConfig = readProjectDescription.getActiveConfiguration();
            //Make sure the active config has been set
            assertEquals("After close open", curConfig.getName(), actualActiveConfig.getName());

        }

    }

}
