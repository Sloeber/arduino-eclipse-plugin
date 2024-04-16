package io.sloeber.autoBuild.regression;

import static io.sloeber.autoBuild.api.AutoBuildConstants.*;
import static io.sloeber.autoBuild.helpers.Defaults.*;
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
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.api.ICodeProvider;
import io.sloeber.autoBuild.helpers.Shared;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.autoBuild.internal.AutoBuildCommon;
import io.sloeber.buildTool.api.IBuildToolManager;
import io.sloeber.buildTool.api.IBuildTools;
import io.sloeber.schema.api.IProjectType;

@SuppressWarnings({ "nls" })
public class AutoBuildCreateBasicProjects {
	static int testCounter = 1;
	// below are test limiting options buildTypeActiveBuild=null and
	private boolean buildTypeActiveBuild = true;
	private boolean doTestDefaultBuilder = true;
	private boolean doTestInternalBuilder = true;
	private boolean doTestMakeBuilder = true;
	private static String codeRootFolder = "src";
	static Set<IBuildTools> buildTools = IBuildToolManager.getDefault().getAllInstalledTargetTools();

	@BeforeAll
	static void beforeAll() {
		Shared.setDeleteProjects(false);
		Shared.setCloseProjects(false);
		// turn off auto building to make sure autobuild does not start a build behind
		// our backs
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

	static void buildAllConfigsAsActive(String builderID, String projectName, String extensionPointID,
			String extensionID, String projectTypeID, String natureID, ICodeProvider codeProvider,
			IBuildTools targetTool, Boolean shouldMakefileExists) throws Exception {

		IProjectType projectType = AutoBuildManager.getProjectType(extensionPointID, extensionID, projectTypeID, true);
		IProject testProject = AutoBuildProject.createProject(
				String.format("%03d", Integer.valueOf(testCounter++)) + "_" + projectName, projectType, natureID,
				codeRootFolder, codeProvider, targetTool, false, null);
		ICProjectDescription cProjectDesc = CCorePlugin.getDefault().getProjectDescription(testProject, true);
		for (ICConfigurationDescription curConfig : cProjectDesc.getConfigurations()) {
			cProjectDesc.setActiveConfiguration(curConfig);
			CCorePlugin.getDefault().setProjectDescription(testProject, cProjectDesc);
			Shared.BuildAndVerifyActiveConfig(testProject, builderID, shouldMakefileExists);
		}
	}

	static void buildAllConfigs(String builderName, String projectName, String extensionPointID, String extensionID,
			String projectTypeID, String natureID, ICodeProvider codeProvider, IBuildTools targetTool,
			Boolean shouldMakefileExists) throws Exception {

		IProjectType projectType = AutoBuildManager.getProjectType(extensionPointID, extensionID, projectTypeID, true);
		IProject testProject = AutoBuildProject.createProject(
				String.format("%03d", Integer.valueOf(testCounter++)) + "_" + projectName, projectType, natureID,
				codeRootFolder, codeProvider, targetTool, false, null);
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

	private void doBuilds(String builderID, String projectName, String extensionPointID, String extensionID,
			String projectTypeID, String natureID, ICodeProvider codeProvider, IBuildTools targetTool,
			Boolean shouldMakefileExists) throws Exception {
		String shortProjectName = projectName;
		if (projectName.length() > 41) {
			shortProjectName = projectName.substring(0, 40);
		}
		if (buildTypeActiveBuild) {
			buildAllConfigsAsActive(builderID, shortProjectName, extensionPointID, extensionID, projectTypeID, natureID,
					codeProvider, targetTool, shouldMakefileExists);
		}
		if (!buildTypeActiveBuild) {
			buildAllConfigs(builderID, "all_" + shortProjectName, extensionPointID, extensionID, projectTypeID,
					natureID, codeProvider, targetTool, shouldMakefileExists);
		}

	}

	@ParameterizedTest
	@MethodSource("projectCreationInfoProvider")
	void testDefaultBuilder(String projectName, String extensionPointID, String extensionID, String projectTypeID,
			String natureID, ICodeProvider codeProvider, IBuildTools targetTool) throws Exception {
		beforeAll();
		if (doTestDefaultBuilder) {
			doBuilds(null, projectName, extensionPointID, extensionID, projectTypeID, natureID, codeProvider,
					targetTool, null);
		}
	}

	@ParameterizedTest
	@MethodSource("projectCreationInfoProvider")
	void testInternaltBuilder(String inProjectName, String extensionPointID, String extensionID, String projectTypeID,
			String natureID, ICodeProvider codeProvider, IBuildTools targetTool) throws Exception {
		beforeAll();
		if (doTestInternalBuilder) {
			String projectName = "Internal_" + inProjectName;

			doBuilds(AutoBuildProject.INTERNAL_BUILDER_ID, projectName, extensionPointID, extensionID, projectTypeID,
					natureID, codeProvider, targetTool, Boolean.FALSE);
		}
	}

	@ParameterizedTest
	@MethodSource("projectCreationInfoProvider")
	void testMakeBuilder(String inProjectName, String extensionPointID, String extensionID, String projectTypeID,
			String natureID, ICodeProvider codeProvider, IBuildTools targetTool) throws Exception {
		beforeAll();
		if (doTestMakeBuilder) {
			Assumptions.assumeFalse(targetTool.getProviderID().equals("io.sloeber.autoBuild.Path.BuildToolProvider"),"Ignoring as make is not assumed on the path") ;
			String projectName = "make_" + inProjectName;
			if (projectName.length() > 40) {
				// somethimes the build fails due to to long filenames
				// as the project name is part of the targetName
				projectName = projectName.substring(0, 40);
			}
			doBuilds(AutoBuildProject.MAKE_BUILDER_ID, projectName, extensionPointID, extensionID, projectTypeID,
					natureID, codeProvider, targetTool, Boolean.TRUE);
		}
	}

	static Stream<Arguments> projectCreationInfoProvider() {
		String extensionPointID = "io.sloeber.autoBuild.buildDefinitions";
		Map<String, String> testProjectTypeIds = new HashMap<>();
		testProjectTypeIds.put(PROJECT_TYPE_ID_COMPOUND_EXE, "io.sloeber.autoBuild");
		testProjectTypeIds.put(PROJECT_TYPE_ID_EXE, "io.sloeber.autoBuild");
		testProjectTypeIds.put(PROJECT_TYPE_ID_STATIC_LIB, "io.sloeber.autoBuild");
		testProjectTypeIds.put(PROJECT_TYPE_ID_DYNAMIC_LIB, "io.sloeber.autoBuild");

		List<Arguments> ret = new LinkedList<>();
		for (Entry<String, String> testProjectEntry : testProjectTypeIds.entrySet()) {
			String extensionID = testProjectEntry.getValue();
			String projectID = testProjectEntry.getKey();
			IProjectType projectType = AutoBuildManager.getProjectType(extensionPointID, extensionID, projectID, true);
			if (projectType == null) {
				System.err.println("Skipping " + extensionID + " " + projectID);
				continue;
			}
			ICodeProvider codeProvider_cpp = null;
			switch (projectID) {
			case PROJECT_TYPE_ID_EXE:
				codeProvider_cpp = cpp_exeCodeProvider;
				break;
			case PROJECT_TYPE_ID_STATIC_LIB:
			case PROJECT_TYPE_ID_DYNAMIC_LIB:
				codeProvider_cpp = cpp_LibProvider;
				break;
			case PROJECT_TYPE_ID_COMPOUND_EXE:
				codeProvider_cpp = compoundProvider;
				break;
			default:
				codeProvider_cpp = cpp_exeCodeProvider;
			}
			for (IBuildTools curBuildTools : buildTools) {
				if (curBuildTools.isProjectTypeSupported(projectType)) {
					String projectName = AutoBuildCommon.MakeNameCompileSafe(projectType.getName() + "_" + extensionID
							+ "_" + curBuildTools.getProviderID() + "_" + curBuildTools.getSelectionID());
					ret.add(Arguments.of(projectName, extensionPointID, extensionID, projectType.getId(),
							CCProjectNature.CC_NATURE_ID, codeProvider_cpp, curBuildTools));
				}
			}
		}
		return ret.stream();
	}
}