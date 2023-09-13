package io.sloeber.autoBuild.regression;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.provider.Arguments;
import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.ICodeProvider;
import io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon;
import io.sloeber.autoBuild.helpers.TemplateTestCodeProvider;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.schema.api.IProjectType;

@SuppressWarnings({ "static-method", "nls" })
public class regression {
    static private String extensionPointID = "io.sloeber.autoBuild.buildDefinitions";

    @BeforeAll
    void beforeAll() {
        Shared.setDeleteProjects(false);
        Shared.setCloseProjects(false);
    }

    @Test
    void closeOpenProjectSavesConfig() throws Exception {
        String projectName = "closeOpenProjectSavesConfig";

        IProject testProject = AutoBuildProject.createProject(extensionPointID, projectName,
                "cdt.managedbuild.target.gnu.cross.exe", "cdt.cross.gnu", CCProjectNature.CC_NATURE_ID,
                new TemplateTestCodeProvider("exe"), false, null);

        ICProjectDescription cProjectDesc = CCorePlugin.getDefault().getProjectDescription(testProject, true);
        for (ICConfigurationDescription curConfig : cProjectDesc.getConfigurations()) {
            cProjectDesc.setActiveConfiguration(curConfig);
            CCorePlugin.getDefault().setProjectDescription(testProject, cProjectDesc);
            Shared.BuildAndVerify(testProject, null, null);
        }
    }

    static Stream<Arguments> projectCreationInfoProvider() {

        Map<String, String> testProjectTypeIds = new HashMap<>();
        testProjectTypeIds.put("io.sloeber.autoBuild.projectType.exe", "io.sloeber.autoBuild");
        testProjectTypeIds.put("cdt.managedbuild.target.gnu.cross.exe", "cdt.cross.gnu");
        testProjectTypeIds.put("cdt.managedbuild.target.gnu.cross.so", "cdt.cross.gnu");
        testProjectTypeIds.put("cdt.managedbuild.target.gnu.cross.lib", "cdt.cross.gnu");
        int testCounter = 1;
        List<Arguments> ret = new LinkedList<>();
        for (Entry<String, String> testProjectEntry : testProjectTypeIds.entrySet()) {
            String extensionID = testProjectEntry.getValue();
            String projectID = testProjectEntry.getKey();
            IProjectType projectType = AutoBuildManager.getProjectType(extensionPointID, extensionID, projectID, true);
            if (projectType == null || !projectType.isCompatibleWithLocalOS() || projectType.isAbstract()) {
                System.err.println("Skipping " + extensionID + " " + projectID);
                continue;
            }
            String buildArtifactType = projectType.getBuildArtifactType();
            ICodeProvider codeProvider_cpp = null;
            switch (buildArtifactType) {
            case "org.eclipse.cdt.build.core.buildArtefactType.exe":
                codeProvider_cpp = new TemplateTestCodeProvider("exe");
                break;
            case "org.eclipse.cdt.build.core.buildArtefactType.staticLib":
            case "org.eclipse.cdt.build.core.buildArtefactType.sharedLib":
                codeProvider_cpp = new TemplateTestCodeProvider("lib");
                break;
            case "org.eclipse.cdt.build.core.buildArtefactType.compound":
                codeProvider_cpp = new TemplateTestCodeProvider("compound");
                break;
            default:
                codeProvider_cpp = new TemplateTestCodeProvider("exe");
            }
            String projectName = AutoBuildCommon.MakeNameCompileSafe(String.format("%03d", Integer.valueOf(testCounter))
                    + "_" + projectType.getName() + "_" + extensionID);
            testCounter++;
            ret.add(Arguments.of(projectName, extensionPointID, extensionID, projectType.getId(),
                    CCProjectNature.CC_NATURE_ID, codeProvider_cpp));
        }
        return ret.stream();
    }
}
