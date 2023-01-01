package io.sloeber.autoBuild.regression;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import io.sloeber.autoBuild.api.AutoBuild;
import org.eclipse.core.resources.IProject;

class CreateProject {

	@ParameterizedTest
	@MethodSource("projectCreationInfoProvider")
	void testExample(String myProjectName, String extensionID, String extensionImpID, String projectTypeID)
			throws Exception {

		IProject testProject = AutoBuild.createProject(myProjectName, extensionID, extensionImpID, projectTypeID, null);
		Shared.BuildAndVerify(testProject);
	}

	static Stream<Arguments> projectCreationInfoProvider() {
		return Stream.of(
				Arguments.of("project1", "io.sloeber.autoBuild.buildDefinitions", "io.sloeber.builddef",
						"io.sloeber.core.sketch"),
				Arguments.of("project2", "io.sloeber.autoBuild.buildDefinitions", "cdt.autotools.core.managed.build.info",
						"org.eclipse.linuxtools.cdt.autotools.core.projectType"));
	}
}
