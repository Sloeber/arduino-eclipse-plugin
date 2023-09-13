package io.sloeber.autoBuild.regression;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.api.ICodeProvider;
import io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon;
import io.sloeber.autoBuild.helpers.TemplateTestCodeProvider;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.schema.api.IProjectType;

@SuppressWarnings({ "boxing", "nls" })
public class CreateBasicProjects {
    static int testCounter = 1;

    @BeforeAll
    static void beforeAll() {
        Shared.setDeleteProjects(false);
        Shared.setCloseProjects(false);
    }

    static void buildAllConfigsAsActive(String builderName, String projectName, String extensionID,
            String extensionImpID, String projectTypeID, String natureID, ICodeProvider codeProvider,
            Boolean shouldMakefileExists) throws Exception {

        IProject testProject = AutoBuildProject.createProject(String.format("%03d", testCounter++) + "_" + projectName,
                extensionID, extensionImpID, projectTypeID, natureID, codeProvider, false, null);
        ICProjectDescription cProjectDesc = CCorePlugin.getDefault().getProjectDescription(testProject, true);
        for (ICConfigurationDescription curConfig : cProjectDesc.getConfigurations()) {
            cProjectDesc.setActiveConfiguration(curConfig);
            CCorePlugin.getDefault().setProjectDescription(testProject, cProjectDesc);
            Shared.BuildAndVerify(testProject, builderName, null, shouldMakefileExists);
        }
    }

    static void buildAllConfigs(String builderName, String projectName, String extensionID, String extensionImpID,
            String projectTypeID, String natureID, ICodeProvider codeProvider, Boolean shouldMakefileExists)
            throws Exception {

        IProject testProject = AutoBuildProject.createProject(String.format("%03d", testCounter++) + "_" + projectName,
                extensionID, extensionImpID, projectTypeID, natureID, codeProvider, false, null);
        ICProjectDescription cProjectDesc = CCorePlugin.getDefault().getProjectDescription(testProject, true);
        Set<String> configs = new HashSet<>();

        for (ICConfigurationDescription curConfig : cProjectDesc.getConfigurations()) {
            configs.add(curConfig.getName());
        }
        Shared.build(testProject, builderName, AutoBuildProject.encode(configs));
        for (ICConfigurationDescription curConfig : cProjectDesc.getConfigurations()) {

            Shared.verifyConfig(testProject, IAutoBuildConfigurationDescription.getConfig(curConfig),
                    shouldMakefileExists);
        }
    }

    @ParameterizedTest
    @MethodSource("projectCreationInfoProvider")
    void testDefaultBuilder(String projectName, String extensionID, String extensionImpID, String projectTypeID,
            String natureID, ICodeProvider codeProvider) throws Exception {
        buildAllConfigsAsActive(null, projectName, extensionID, extensionImpID, projectTypeID, natureID, codeProvider,
                null);
        buildAllConfigs(null, "all_" + projectName, extensionID, extensionImpID, projectTypeID, natureID, codeProvider,
                null);

    }

    @ParameterizedTest
    @MethodSource("projectCreationInfoProvider")
    void testInternaltBuilder(String inProjectName, String extensionID, String extensionImpID, String projectTypeID,
            String natureID, ICodeProvider codeProvider) throws Exception {
        String projectName = "Internal_build_" + inProjectName;
        buildAllConfigsAsActive(AutoBuildProject.ARGS_INTERNAL_BUILDER_KEY, projectName, extensionID, extensionImpID,
                projectTypeID, natureID, codeProvider, Boolean.FALSE);
        buildAllConfigs(AutoBuildProject.ARGS_INTERNAL_BUILDER_KEY, "all_" + projectName, extensionID, extensionImpID,
                projectTypeID, natureID, codeProvider, Boolean.FALSE);
    }

    @ParameterizedTest
    @MethodSource("projectCreationInfoProvider")
    void testExternalBuilder(String inProjectName, String extensionID, String extensionImpID, String projectTypeID,
            String natureID, ICodeProvider codeProvider) throws Exception {
        String projectName = "make_build_" + inProjectName;
        buildAllConfigsAsActive(AutoBuildProject.ARGS_MAKE_BUILDER_KEY, projectName, extensionID, extensionImpID,
                projectTypeID, natureID, codeProvider, Boolean.TRUE);
        buildAllConfigs(AutoBuildProject.ARGS_MAKE_BUILDER_KEY, "all_" + projectName, extensionID, extensionImpID,
                projectTypeID, natureID, codeProvider, Boolean.TRUE);
    }

    static Stream<Arguments> projectCreationInfoProvider() {
        String extensionPointID = "io.sloeber.autoBuild.buildDefinitions";
        Map<String, String> testProjectTypeIds = new HashMap<>();
        testProjectTypeIds.put("io.sloeber.autoBuild.projectType.exe", "io.sloeber.autoBuild");
        testProjectTypeIds.put("cdt.managedbuild.target.gnu.cross.exe", "cdt.cross.gnu");
        testProjectTypeIds.put("cdt.managedbuild.target.gnu.cross.so", "cdt.cross.gnu");
        testProjectTypeIds.put("cdt.managedbuild.target.gnu.cross.lib", "cdt.cross.gnu");

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
            String projectName = AutoBuildCommon.MakeNameCompileSafe(projectType.getName() + "_" + extensionID);
            ret.add(Arguments.of(projectName, extensionPointID, extensionID, projectType.getId(),
                    CCProjectNature.CC_NATURE_ID, codeProvider_cpp));
        }
        return ret.stream();
    }
}
