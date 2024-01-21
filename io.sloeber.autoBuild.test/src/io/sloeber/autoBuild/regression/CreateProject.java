package io.sloeber.autoBuild.regression;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.ICodeProvider;
import io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon;
import io.sloeber.autoBuild.helpers.Shared;
import io.sloeber.autoBuild.helpers.TemplateTestCodeProvider;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.buildTool.api.IBuildToolManager;
import io.sloeber.buildTool.api.IBuildTools;
import io.sloeber.schema.api.IProjectType;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;

@SuppressWarnings("nls")
class CreateProject {
    static IBuildTools targetTool = IBuildToolManager.getDefault().getAnyInstalledTargetTool();
    private static String codeRootFolder="src";
    @BeforeAll
    static void beforeAll() {
        Shared.setDeleteProjects(false);
        Shared.setCloseProjects(false);
    }

	@SuppressWarnings("static-method")
    @ParameterizedTest
    @MethodSource("projectCreationInfoProvider")
    void testExample(String myProjectName, String extensionID, String extensionImpID, String projectTypeID,
            String natureID, ICodeProvider codeProvider) throws Exception {

        IProject testProject = AutoBuildProject.createProject(myProjectName, extensionID, extensionImpID, projectTypeID,
                natureID, codeRootFolder,codeProvider, targetTool, false, null);
        ICProjectDescription cProjectDesc = CCorePlugin.getDefault().getProjectDescription(testProject, true);
        for (ICConfigurationDescription curConfig : cProjectDesc.getConfigurations()) {
            cProjectDesc.setActiveConfiguration(curConfig);
            CCorePlugin.getDefault().setProjectDescription(testProject, cProjectDesc);
            Shared.BuildAndVerifyActiveConfig(testProject);
        }
    }

    static Stream<Arguments> projectCreationInfoProvider() {
        int testCounter = 1;
        List<Arguments> ret = new LinkedList<>();
        for (String extensionPointID : AutoBuildManager.supportedExtensionPointIDs()) {
            for (String extensionID : AutoBuildManager.getSupportedExtensionIDs(extensionPointID)) {
                for (IProjectType projectType : AutoBuildManager.getProjectTypes(extensionPointID, extensionID)) {
                	if(projectType.isTest()) {
                		continue;
                	}
                    String projectID = projectType.getId();


                        ICodeProvider codeProvider_cpp = null;
                        ICodeProvider codeProvider_c = null;
                        switch (projectID) {
                        case "io.sloeber.autoBuild.projectType.exe":
                            codeProvider_cpp = new TemplateTestCodeProvider("exe");
                            codeProvider_c = new TemplateTestCodeProvider("c_exe");
                            break;
                        case "io.sloeber.autoBuild.projectType.static.lib":
                        case "io.sloeber.autoBuild.projectType.dynamic.lib":
                            codeProvider_cpp = new TemplateTestCodeProvider("lib");
                            codeProvider_c = new TemplateTestCodeProvider("c_lib");
                            break;
                        case "io.sloeber.autoBuild.projectType.compound.exe":
                            codeProvider_cpp = new TemplateTestCodeProvider("compound");
                            break;
                        default:
                            codeProvider_cpp = new TemplateTestCodeProvider("exe");
                        }
                        String projectName = AutoBuildCommon
                                .MakeNameCompileSafe(String.format("%03d", Integer.valueOf(testCounter)) + "_CPP_"
                                        + projectType.getName() + "_" + extensionID);
                        testCounter++;
                        ret.add(Arguments.of(projectName, extensionPointID, extensionID, projectID,
                                CCProjectNature.CC_NATURE_ID, codeProvider_cpp));
                        if (codeProvider_c != null) {
                            projectName = AutoBuildCommon
                                    .MakeNameCompileSafe(String.format("%03d", Integer.valueOf(testCounter)) + "_C_"
                                            + projectType.getName() + "_" + extensionID);
                            testCounter++;
                            ret.add(Arguments.of(projectName, extensionPointID, extensionID, projectID,
                                    CProjectNature.C_NATURE_ID, codeProvider_c));
                        }
                }
            }
        }
        return ret.stream();
    }
}
