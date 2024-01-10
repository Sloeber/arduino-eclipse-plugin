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
import io.sloeber.schema.api.IProjectType;
import io.sloeber.targetPlatform.api.ITargetTool;
import io.sloeber.targetPlatform.api.ITargetToolManager;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;

@SuppressWarnings("nls")
class CreateProject {
    static ITargetTool targetTool = ITargetToolManager.getDefault().getAnyInstalledTargetTool();

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
                natureID, codeProvider, targetTool, false, null);
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


                        String buildArtifactType = projectType.getBuildArtifactType();
                        ICodeProvider codeProvider_cpp = null;
                        ICodeProvider codeProvider_c = null;
                        switch (buildArtifactType) {
                        case "org.eclipse.cdt.build.core.buildArtefactType.exe":
                            codeProvider_cpp = new TemplateTestCodeProvider("exe");
                            codeProvider_c = new TemplateTestCodeProvider("c_exe");
                            break;
                        case "org.eclipse.cdt.build.core.buildArtefactType.staticLib":
                        case "org.eclipse.cdt.build.core.buildArtefactType.sharedLib":
                            codeProvider_cpp = new TemplateTestCodeProvider("lib");
                            codeProvider_c = new TemplateTestCodeProvider("c_lib");
                            break;
                        case "org.eclipse.cdt.build.core.buildArtefactType.compound":
                            codeProvider_cpp = new TemplateTestCodeProvider("compound");
                            break;
                        default:
                            codeProvider_cpp = new TemplateTestCodeProvider("exe");
                        }
                        String projectName = AutoBuildCommon
                                .MakeNameCompileSafe(String.format("%03d", Integer.valueOf(testCounter)) + "_"
                                        + projectType.getName() + "_" + extensionID);
                        testCounter++;
                        ret.add(Arguments.of(projectName, extensionPointID, extensionID, projectID,
                                CCProjectNature.CC_NATURE_ID, codeProvider_cpp));
                        if (codeProvider_c != null) {
                            projectName = AutoBuildCommon
                                    .MakeNameCompileSafe(String.format("%03d", Integer.valueOf(testCounter)) + "_"
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
