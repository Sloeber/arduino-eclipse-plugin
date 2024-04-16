package io.sloeber.autoBuild.regression;

import static io.sloeber.autoBuild.api.AutoBuildConstants.*;
import static io.sloeber.autoBuild.helpers.Defaults.*;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.ICodeProvider;
import io.sloeber.autoBuild.buildTools.api.IBuildTools;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager;
import io.sloeber.autoBuild.helpers.Shared;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.autoBuild.internal.AutoBuildCommon;
import io.sloeber.autoBuild.schema.api.IProjectType;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;

@SuppressWarnings("nls")
class AutoBuildCreateProject {

	private static String codeRootFolder = "src";

	@BeforeAll
	static void beforeAll() {
		Shared.setDeleteProjects(false);
		Shared.setCloseProjects(false);
	}

	@SuppressWarnings("static-method")
	@ParameterizedTest
	@MethodSource("projectCreationInfoProvider")
	void testExample(String myProjectName, IProjectType projectType, IBuildTools buildTools,
			String natureID, ICodeProvider codeProvider) throws Exception {

		IProject testProject = AutoBuildProject.createProject(myProjectName, projectType, natureID, codeRootFolder,
				codeProvider, buildTools, false, null);
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
					if (projectType.isTest()) {
						continue;
					}
					String projectID = projectType.getId();
					IBuildTools buildTools = IBuildToolsManager.getDefault().getAnyInstalledBuildTools(projectType);
					if(buildTools==null) {
						continue;
					}

					ICodeProvider codeProvider_cpp = null;
					ICodeProvider codeProvider_c = null;
					switch (projectID) {
					case PROJECT_TYPE_ID_EXE:
						codeProvider_cpp = cpp_exeCodeProvider;
						codeProvider_c = c_exeCodeProvider;
						break;
					case "io.sloeber.autoBuild.projectType.static.lib":
					case PROJECT_TYPE_ID_DYNAMIC_LIB:
						codeProvider_cpp = cpp_LibProvider;
						codeProvider_c = c_LibProvider;
						break;
					case PROJECT_TYPE_ID_COMPOUND_EXE:
						codeProvider_cpp = compoundProvider;
						break;
					default:
						codeProvider_cpp = cpp_exeCodeProvider;
					}
					String projectName = AutoBuildCommon
							.MakeNameCompileSafe(String.format("%03d", Integer.valueOf(testCounter)) + "_CPP_"
									+ projectType.getName() + "_" + extensionID);
					testCounter++;
					ret.add(Arguments.of(projectName, projectType, buildTools,
							CCProjectNature.CC_NATURE_ID, codeProvider_cpp));
					if (codeProvider_c != null) {
						projectName = AutoBuildCommon
								.MakeNameCompileSafe(String.format("%03d", Integer.valueOf(testCounter)) + "_C_"
										+ projectType.getName() + "_" + extensionID);
						testCounter++;
						ret.add(Arguments.of(projectName, projectType, buildTools,
								CProjectNature.C_NATURE_ID, codeProvider_c));
					}
				}
			}
		}
		return ret.stream();
	}
}
