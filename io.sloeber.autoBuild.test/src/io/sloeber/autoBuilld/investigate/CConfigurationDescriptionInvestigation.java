package io.sloeber.autoBuilld.investigate;

import static io.sloeber.autoBuild.helpers.Defaults.*;
import static org.junit.Assert.*;

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
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;

import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.helpers.Shared;
import io.sloeber.buildTool.api.IBuildToolManager;
import io.sloeber.buildTool.api.IBuildTools;

@SuppressWarnings({ "restriction", "nls", "static-method" })
public class CConfigurationDescriptionInvestigation {
    static int testCounter = 1;
    static private String codeRootFolder="src";

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
        beforeAll();
        String projectName = "testConfigDescription";
        IBuildTools buildTools = IBuildToolManager.getDefault().getAnyInstalledBuildTools(defaultProjectType);
        IProject testProject = AutoBuildProject.createProject(projectName, defaultProjectType, defaultNatureID,codeRootFolder, cpp_exeCodeProvider, buildTools, false, null);
        ICProjectDescription projectDesc = CoreModel.getDefault().getProjectDescription(testProject, true);
        for (ICConfigurationDescription curConf : projectDesc.getConfigurations()) {
            assertFalse("conf is readOnly class instance", curConf instanceof CConfigurationDescriptionCache);
            assertTrue("conf is of a unknown class instance", curConf instanceof CConfigurationDescription);
            ICFolderDescription orgDescription = curConf.getRootFolderDescription();
            curConf.setDescription("A nice description");
            ICFolderDescription newDescription = curConf.getRootFolderDescription();
        }
    }

}
