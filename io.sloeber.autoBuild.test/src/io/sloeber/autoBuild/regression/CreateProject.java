package io.sloeber.autoBuild.regression;

import static org.junit.Assert.fail;
import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import io.sloeber.autoBuild.api.AutoBuild;
import io.sloeber.schema.api.IProjectType;
import io.sloeber.autoBuild.Internal.ManagedBuildManager;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
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
        Shared.setDeleteProjects(false);
        Shared.setCloseProjects(false);

        IProject testProject = AutoBuild.createProject(myProjectName, extensionID, extensionImpID, projectTypeID, null);
        ICProjectDescription cProjectDesc = CCorePlugin.getDefault().getProjectDescription(testProject, true);
        String errorMessage = new String();
        for (ICConfigurationDescription curConfig : cProjectDesc.getConfigurations()) {
            cProjectDesc.setActiveConfiguration(curConfig);
            CCorePlugin.getDefault().setProjectDescription(testProject, cProjectDesc);
            if (!Shared.BuildAndVerify(testProject)) {
                errorMessage += "\n\t" + curConfig.getName();
            }
        }
        if (!errorMessage.isBlank()) {
            fail("Project " + myProjectName + " Failed to build configs:" + errorMessage);
        }
    }

    static Stream<Arguments> projectCreationInfoProvider() {
        String extensionPointID = "io.sloeber.autoBuild.buildDefinitions";
        int testCounter = 1;
        List<Arguments> ret = new LinkedList<>();
        IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(extensionPointID);
        if (extensionPoint != null) {
            IExtension extensions[] = extensionPoint.getExtensions();
            for (IExtension extension : extensions) {
                String extensionID = extension.getUniqueIdentifier();
                IConfigurationElement[] elements = extension.getConfigurationElements();
                for (IConfigurationElement element : elements) {
                    if (element.getName().equals(IProjectType.PROJECTTYPE_ELEMENT_NAME)) {
                        String projectID = element.getAttribute(ID);
                        IProjectType projectType = ManagedBuildManager.getProjectType(extensionPointID, extensionID,
                                projectID, true);
                        if (projectType != null && projectType.isCompatibleWithLocalOS()) {

                            String projectName = String.format("%03d", testCounter) + "_" + projectType.getName() + "_"
                                    + extensionID;

                            testCounter++;
                            ret.add(Arguments.of(projectName, extensionPointID, extensionID, projectID));
                            //                        if (testCounter > 4) {
                            //                            return ret.stream();
                            //                        }
                        } else {
                            System.err.print("Skipping projectType " + extensionPointID + " " + projectID);
                            if (projectType == null)
                                System.err.println(" projectType is null");
                            else
                                System.err.println(" projectType is incompitble with OS");
                        }
                    }
                }
            }
        }
        return ret.stream();
    }
}
