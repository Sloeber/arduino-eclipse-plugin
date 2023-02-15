package io.sloeber.autoBuild.regression;

import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import io.sloeber.autoBuild.api.AutoBuild;
import io.sloeber.schema.api.IProjectType;
import io.sloeber.schema.api.ISchemaObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;

@SuppressWarnings("nls")
class CreateProject {

    @SuppressWarnings("static-method")
    @ParameterizedTest
    @MethodSource("projectCreationInfoProvider")
    void testExample(String myProjectName, String extensionID, String extensionImpID, String projectTypeID)
            throws Exception {

        IProject testProject = AutoBuild.createProject(myProjectName, extensionID, extensionImpID, projectTypeID, null);
        if (!Shared.BuildAndVerify(testProject)) {
            fail("Project " + myProjectName + " failed to build correctly");
        }
    }

    static Stream<Arguments> projectCreationInfoProvider() {
        String extensionPointID = "io.sloeber.autoBuild.buildDefinitions";
        int testCounter = 0;
        List<Arguments> ret = new LinkedList<>();
        IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(extensionPointID);
        if (extensionPoint != null) {
            IExtension extensions[] = extensionPoint.getExtensions();
            for (IExtension extension : extensions) {
                String extensionID = extension.getUniqueIdentifier();
                IConfigurationElement[] elements = extension.getConfigurationElements();
                for (IConfigurationElement element : elements) {
                    if (element.getName().equals(IProjectType.PROJECTTYPE_ELEMENT_NAME)) {

                        String projectName = extensionID + "_" + element.getAttribute(ISchemaObject.NAME) + "_"
                                + String.valueOf(testCounter);
                        String projectID = element.getAttribute(ISchemaObject.ID);
                        testCounter++;
                        if (extensionID == null) {
                            System.err.println("Skipping project " + projectName + " from extensionPointID projectID ("
                                    + extensionPointID + " " + projectID + " because extensionID is null");
                        } else {
                            ret.add(Arguments.of(projectName, extensionPointID, extensionID, projectID));
                        }
                        if (testCounter > 4) {
                            return ret.stream();
                        }
                    }

                    //		return Stream.of(
                    //				Arguments.of("project1", "io.sloeber.autoBuild.buildDefinitions", "io.sloeber.builddef",
                    //						"io.sloeber.core.sketch"),
                    //				Arguments.of("project2", "io.sloeber.autoBuild.buildDefinitions", "cdt.autotools.core.managed.build.info",
                    //						"org.eclipse.linuxtools.cdt.autotools.core.projectType"));
                }
            }
        }
        return ret.stream();
    }
}
