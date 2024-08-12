package io.sloeber.autoBuilld.investigate;

import static io.sloeber.autoBuild.helpers.Defaults.*;
import static org.junit.jupiter.api.Assertions.*;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICFolderDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.internal.core.settings.model.CConfigurationDescription;
import org.eclipse.cdt.internal.core.settings.model.CConfigurationDescriptionCache;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.buildTools.api.IBuildTools;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager;
import io.sloeber.autoBuild.helpers.Shared;

@SuppressWarnings({ "restriction", "nls", "static-method" })
public class CConfigurationDescriptionInvestigation {
    static int testCounter = 1;

    @BeforeAll
    static void beforeAll() {
        Shared.setDeleteProjects(false);
        Shared.setCloseProjects(false);
        // turn off auto building to make sure autobuild does not start a build behind our backs
        final IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceDescription workspaceDesc = workspace.getDescription();
        workspaceDesc.setAutoBuilding(false);
    }

    @Test
    public void testConfigDescription() {
        String projectName = "testConfigDescription";
        IBuildTools buildTools = IBuildToolsManager.getDefault().getAnyInstalledBuildTools(defaultProjectType);
        IProject testProject = AutoBuildProject.createProject(projectName, defaultProjectType, defaultNatureID, cpp_exeCodeProvider, buildTools, false, null);
        ICProjectDescription projectDesc = CoreModel.getDefault().getProjectDescription(testProject, true);
        for (ICConfigurationDescription curConf : projectDesc.getConfigurations()) {
            assertFalse( curConf instanceof CConfigurationDescriptionCache,"conf is readOnly class instance");
            assertTrue( curConf instanceof CConfigurationDescription,"conf is of a unknown class instance");
            ICFolderDescription orgDescription = curConf.getRootFolderDescription();
            curConf.setDescription("A nice description");
            ICFolderDescription newDescription = curConf.getRootFolderDescription();
        }
    }

}
