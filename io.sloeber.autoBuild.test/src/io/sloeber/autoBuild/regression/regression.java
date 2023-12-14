package io.sloeber.autoBuild.regression;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.sloeber.autoBuild.api.AutoBuildProject;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.api.ICodeProvider;
import io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon;
import io.sloeber.autoBuild.extensionPoint.providers.MakeRule;
import io.sloeber.autoBuild.extensionPoint.providers.MakeRules;
import io.sloeber.autoBuild.helpers.Shared;
import io.sloeber.autoBuild.helpers.TemplateTestCodeProvider;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.schema.api.IOption;
import io.sloeber.schema.api.IProjectType;
import io.sloeber.schema.api.ITool;

@SuppressWarnings({ "static-method", "nls", "boxing" })
public class regression {
    static private String extensionPointID = "io.sloeber.autoBuild.buildDefinitions";
    static int testCounter = 1;

    @BeforeAll
    public static void beforeAll() {
        Shared.setDeleteProjects(false);
        Shared.setCloseProjects(false);
        // turn off auto building to make sure autobuild does not start a build behind our backs
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

    /*
     * Create a project build it
     * clean it
     * close it
     * open it
     * build it
     * 
     */
    @Test
    public void createCloseOpenProject() throws Exception {
        beforeAll();// for one reason or another the beforeall is not called
        String projectName = "createCloseOpenProject";

        IProject testProject = AutoBuildProject.createProject(projectName, extensionPointID, "cdt.cross.gnu",
                "cdt.managedbuild.target.gnu.cross.exe", CCProjectNature.CC_NATURE_ID,
                new TemplateTestCodeProvider("exe"), false, null);

        //Build all the configurations and verify proper building
        Shared.buildAndVerifyProjectUsingActivConfig(testProject, null);
        //clean all configurations and verify clean has been done properly
        Shared.cleanProject(testProject);

        //close the project
        testProject.close(new NullProgressMonitor());
        //wait a while
        Thread.sleep(5000);
        //open the project 
        testProject.open(new NullProgressMonitor());
        //Build all the configurations and verify proper building
        Shared.buildAndVerifyProjectUsingActivConfig(testProject, null);
    }

    /*
     * Create a project build it
     * check whether there is a makefile
     * if so set internal builder else set external builder
     * clean it
     * build
     * check for makefile existence
     * clean it
     * close it
     * open it
     * build it
     * check for makefile existence
     * 
     */

    @Test
    public void setBuilder() throws Exception {
        beforeAll();// for one reason or another the before all is not called
        String projectName = "setBuilder";

        IProject testProject = AutoBuildProject.createProject(projectName, extensionPointID, "cdt.cross.gnu",
                "cdt.managedbuild.target.gnu.cross.exe", CCProjectNature.CC_NATURE_ID,
                new TemplateTestCodeProvider("exe"), false, null);

        //Build the active configuration and verify proper building
        Shared.BuildAndVerifyActiveConfig(testProject);

        ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
        ICProjectDescription projectDescription = mngr.getProjectDescription(testProject, true);
        IAutoBuildConfigurationDescription activeConfig = IAutoBuildConfigurationDescription
                .getActiveConfig(projectDescription);

        IFile makeFile = activeConfig.getBuildFolder().getFile("makefile");
        boolean hasMakefile = makeFile.exists();
        if (hasMakefile) {
            activeConfig.setBuildRunner(AutoBuildProject.ARGS_INTERNAL_BUILDER_KEY);
        } else {
            activeConfig.setBuildRunner(AutoBuildProject.ARGS_MAKE_BUILDER_KEY);
        }
        //clean all configurations and verify clean has been done properly
        Shared.cleanConfiguration(activeConfig);
        //do the clean before the builderswitch otherwise the makefile in the buildroot will make the test fail
        mngr.setProjectDescription(testProject, projectDescription, true, new NullProgressMonitor());

        //Build all the configurations and verify proper building
        Shared.BuildAndVerifyActiveConfig(testProject);

        assertNotEquals("Builder changes have not been taken into account", makeFile.exists(), hasMakefile);

        //clean activeConfig and verify clean has been done properly
        Shared.cleanConfiguration(activeConfig);
        activeConfig.getBuildFolder().delete(true, new NullProgressMonitor());

        assertFalse("Clean did not remove makefile", makeFile.exists());

        //close the project
        testProject.close(new NullProgressMonitor());
        //wait a while
        Thread.sleep(5000);
        //open the project 
        testProject.open(new NullProgressMonitor());
        //Build all the configurations and verify proper building
        Shared.BuildAndVerifyActiveConfig(testProject);

        assertNotEquals("Builder changes have been lost in open close projects", makeFile.exists(), hasMakefile);
    }

    static String savePreviousCommand = null;
    static String savePreviousOptionCommand = null;
    static String savepreviousOptionID = null;

    @ParameterizedTest
    @MethodSource("OptionIDValueCmd")
    void testOptions(String optionID, String optionValue, String commandContribution) throws CoreException {
        //get the project; create it if nessesary
        String projectName = "testOptionsProject";
        IProject testProject = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        if (testProject == null || !testProject.exists()) {
            testProject = AutoBuildProject.createProject(projectName, extensionPointID, "io.sloeber.autoBuild.test",
                    "io.sloeber.autoBuild.projectType.test.options", CCProjectNature.CC_NATURE_ID,
                    new TemplateTestCodeProvider("exe"), false, null);
        }

        //get the project and autobuild configurations
        CoreModel coreModel = CoreModel.getDefault();
        ICProjectDescription projectDescription = coreModel.getProjectDescription(testProject, true);
        AutoBuildConfigurationDescription autoConf = (AutoBuildConfigurationDescription) IAutoBuildConfigurationDescription
                .getActiveConfig(projectDescription);

        //get the tool and option
        ITool tool = autoConf.getAutoBuildConfiguration().getToolChain().getTools().get(0);
        IOption iOption = tool.getOption(optionID);

        //If the statics are null initialize them
        if (savePreviousCommand == null) {
            savePreviousCommand = getTheCompileCommand(autoConf, tool).trim();
            savePreviousOptionCommand = savePreviousCommand;
            savepreviousOptionID = optionID;
        }

        if (!savepreviousOptionID.equals(optionID)) {
            savePreviousOptionCommand = savePreviousCommand;
        }

        autoConf.setOptionValue(testProject, tool, iOption, optionValue);
        coreModel.setProjectDescription(testProject, projectDescription);

        String CurrentCommand = getTheCompileCommand(autoConf, tool).trim();
        String previousCommand = savePreviousOptionCommand;
        savePreviousCommand = CurrentCommand;
        if (commandContribution.isBlank()) {
            assertTrue("option is not consistent", previousCommand.equals(CurrentCommand));
        } else {
            String[] parts = CurrentCommand.split(Pattern.quote(commandContribution));
            assertTrue("option contribution did not appear " + CurrentCommand, parts.length == 2);
            assertTrue("option contribution appeard multiple times in command " + CurrentCommand, parts.length < 3);
            String reGlued = parts[0].trim() + " " + parts[1].trim();
            if (!previousCommand.equals(reGlued)) {
                fail("option is not consistent " + CurrentCommand + " " + previousCommand);
            }
        }

    }

    private String getTheCompileCommand(AutoBuildConfigurationDescription autoData, ITool iTool) throws CoreException {
        IFolder buildRoot = autoData.getBuildFolder();

        //Generate the make Rules
        MakeRules myMakeRules = new MakeRules(autoData, buildRoot, new HashSet<>());
        for (MakeRule curRule : myMakeRules) {
            if (curRule.getTool().equals(iTool)) {
                if (curRule.getTargetFiles().size() == 1) {
                    return curRule.getRecipes(buildRoot, autoData)[0];
                }
            }
        }
        return null;
    }

    static Stream<Arguments> OptionIDValueCmd() {
        List<Arguments> ret = new LinkedList<>();
        ret.add(Arguments.of("gnu.cpp.compiler.option.dialect.std", "gnu.cpp.compiler.dialect.default", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.dialect.std", "gnu.cpp.compiler.dialect.c++98", "-std=c++98"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.dialect.std", "gnu.cpp.compiler.dialect.c++11", "-std=c++0x"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.dialect.std", "gnu.cpp.compiler.dialect.c++1y", "-std=c++1y"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.dialect.std", "gnu.cpp.compiler.dialect.c++17", "-std=c++17"));

        ret.add(Arguments.of("gnu.cpp.compiler.option.dialect.flags", "a flag", "a flag"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.preprocessor.nostdinc", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.preprocessor.nostdinc", "true", "-nostdinc"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.preprocessor.preprocess", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.preprocessor.preprocess", "true", "-E"));

        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "gnucppcompilerdialect", "dddtddddddddddddd7"));

        return ret.stream();
    }

}
