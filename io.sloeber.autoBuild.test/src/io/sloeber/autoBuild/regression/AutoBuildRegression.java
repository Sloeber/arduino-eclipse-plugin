package io.sloeber.autoBuild.regression;

import static org.junit.Assert.*;
import static io.sloeber.autoBuild.helpers.Defaults.*;

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.model.CoreModel;
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
import io.sloeber.autoBuild.api.IAutoBuildMakeRule;
import io.sloeber.autoBuild.api.IAutoBuildMakeRules;
import io.sloeber.autoBuild.buildTools.api.IBuildTools;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager;
import io.sloeber.autoBuild.extensionPoint.providers.AutoBuildMakeRules;
import io.sloeber.autoBuild.helpers.Shared;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.autoBuild.integration.AutoBuildManager;
import io.sloeber.autoBuild.schema.api.IBuilder;
import io.sloeber.autoBuild.schema.api.IOption;
import io.sloeber.autoBuild.schema.api.IProjectType;
import io.sloeber.autoBuild.schema.api.ITool;

@SuppressWarnings({ "static-method", "nls", "boxing" })
public class AutoBuildRegression {
    static private String extensionPointID = "io.sloeber.autoBuild.buildDefinitions";
    static private String codeRootFolder="src";
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

        IProjectType projectType= AutoBuildManager.getProjectType( extensionPointID, defaultExtensionID, defaultProjectTypeID, true);
        IBuildTools buildTools = IBuildToolsManager.getDefault().getAnyInstalledBuildTools(projectType);
        IProject testProject = AutoBuildProject.createProject(projectName, projectType, CCProjectNature.CC_NATURE_ID,codeRootFolder,
        		cpp_exeCodeProvider, buildTools, false, null); 

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

        IProjectType projectType= AutoBuildManager.getProjectType( extensionPointID, defaultExtensionID, defaultProjectTypeID, true);
        IBuildTools buildTools = IBuildToolsManager.getDefault().getAnyInstalledBuildTools(projectType);
        IProject testProject = AutoBuildProject.createProject(projectName, projectType, CCProjectNature.CC_NATURE_ID,codeRootFolder,
        		cpp_exeCodeProvider, buildTools, false, null);

        //Build the active configuration and verify proper building
        Shared.BuildAndVerifyActiveConfig(testProject);

        ICProjectDescriptionManager mngr = CoreModel.getDefault().getProjectDescriptionManager();
        ICProjectDescription projectDescription = mngr.getProjectDescription(testProject, true);
        IAutoBuildConfigurationDescription activeConfig = IAutoBuildConfigurationDescription
                .getActiveConfig(projectDescription);

        IFile makeFile = activeConfig.getBuildFolder().getFile("makefile");
        boolean hasMakefile = makeFile.exists();
        IBuilder builder=null; 
        if (hasMakefile) {
        	builder=activeConfig.getBuilder(AutoBuildProject.INTERNAL_BUILDER_ID);
        } else {
        	builder=activeConfig.getBuilder(AutoBuildProject.MAKE_BUILDER_ID);
        }
        activeConfig.setBuilder(builder);
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
        	IProjectType projectType= AutoBuildManager.getProjectType( extensionPointID, "io.sloeber.autoBuild.test",  "io.sloeber.autoBuild.projectType.test.options", true);
        	IBuildTools buildTools = IBuildToolsManager.getDefault().getAnyInstalledBuildTools(projectType);
            testProject = AutoBuildProject.createProject(projectName, projectType, 
                    CCProjectNature.CC_NATURE_ID,codeRootFolder,
                    cpp_exeCodeProvider, buildTools, false, null);
        }

        //get the project and autobuild configurations
        CoreModel coreModel = CoreModel.getDefault();
        ICProjectDescription projectDescription = coreModel.getProjectDescription(testProject, true);
        AutoBuildConfigurationDescription autoConf = (AutoBuildConfigurationDescription) IAutoBuildConfigurationDescription
                .getActiveConfig(projectDescription);

        //get the tool and option
        ITool tool = autoConf.getProjectType().getToolChain().getTools().get(0);
        IOption iOption = tool.getOption(optionID);

        //If the statics are null initialize them
        if (savePreviousCommand == null) {
            savePreviousCommand = getTheCompileCommand(autoConf, tool).trim();
            savePreviousOptionCommand = savePreviousCommand;
            savepreviousOptionID = optionID;
        }

        if (!savepreviousOptionID.equals(optionID)) {
            savePreviousOptionCommand = savePreviousCommand;
            savepreviousOptionID = optionID;
        }

        autoConf.setOptionValue(testProject, tool, iOption, optionValue);
        coreModel.setProjectDescription(testProject, projectDescription);

        String CurrentCommand = getTheCompileCommand(autoConf, tool).trim();
        String previousCommand = savePreviousOptionCommand;
        savePreviousCommand = CurrentCommand;
        if (commandContribution.isBlank()) {
            if (previousCommand.equals(CurrentCommand) == false) {
                getTheCompileCommand(autoConf, tool);
            }
            assertTrue("option is not consistent " + CurrentCommand + " " + previousCommand,
                    previousCommand.equals(CurrentCommand));
        } else {
            String[] parts = CurrentCommand.split(Pattern.quote(" " + commandContribution + " "));
            if (parts.length != 2) {
                getTheCompileCommand(autoConf, tool);
            }
            assertTrue("option contribution did not appear " + CurrentCommand, parts.length == 2);
            assertTrue("option contribution appeard multiple times in command " + CurrentCommand, parts.length < 3);
            String reGlued = parts[0] + " " + parts[1];
            if (!previousCommand.equals(reGlued)) {
                fail("option is not consistent " + CurrentCommand + " " + previousCommand);
            }
        }

    }

    private String getTheCompileCommand(AutoBuildConfigurationDescription autoData, ITool iTool) throws CoreException {
        IFolder buildRoot = autoData.getBuildFolder();

        //Generate the make Rules
        IAutoBuildMakeRules myMakeRules = new AutoBuildMakeRules(autoData);
        for (IAutoBuildMakeRule curRule : myMakeRules) {
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

        ret.add(Arguments.of("gnu.cpp.compiler.option.preprocessor.def", "Define1;Define2", "-DDefine1 -DDefine2"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.preprocessor.undef", "UnDefine1;UnDefine2",
                "-UUnDefine1 -UUnDefine2"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.include.paths", "c:\\includePath1;/root/includePath2",
                "-Ic:\\includePath1 -I/root/includePath2"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.include.files", "c:\\includeFile1;/root/includeFile2",
                "-includec:\\includeFile1 -include/root/includeFile2"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.optimization.level", "gnu.cpp.compiler.optimization.level.none",
                "-O0"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.optimization.level",
                "gnu.cpp.compiler.optimization.level.optimize", "-O1"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.optimization.level", "gnu.cpp.compiler.optimization.level.more",
                "-O2"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.optimization.level", "gnu.cpp.compiler.optimization.level.most",
                "-O3"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.optimization.level", "gnu.cpp.compiler.optimization.level.size",
                "-Os"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.optimization.flags", "optimize flags", "optimize flags"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.level", "gnu.cpp.compiler.debugging.level.none", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.level", "gnu.cpp.compiler.debugging.level.minimal",
                "-g1"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.level", "gnu.cpp.compiler.debugging.level.default",
                "-g"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.level", "gnu.cpp.compiler.debugging.level.max", "-g3"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.other", "debug other", "debug other"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.prof", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.prof", "true", "-p"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.gprof", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.gprof", "true", "-pg"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.codecov", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.codecov", "true", "-ftest-coverage -fprofile-arcs"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.sanitaddress", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.sanitaddress", "true", "-fsanitize=address"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.sanitpointers", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.sanitpointers", "true",
                "-fsanitize=pointer-compare -fsanitize=pointer-subtract"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.sanitthread", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.sanitthread", "true", "-fsanitize=thread"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.sanitleak", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.sanitleak", "true", "-fsanitize=leak"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.sanitundef", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.debugging.sanitundef", "true", "-fsanitize=undefined"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.syntax", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.syntax", "true", "-fsyntax-only"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.pedantic", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.pedantic", "true", "-pedantic"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.pedantic.error", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.pedantic.error", "true", "-pedantic-errors"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.nowarn", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.nowarn", "true", "-w"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.allwarn", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.allwarn", "true", "-Wall"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.extrawarn", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.extrawarn", "true", "-Wextra"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.toerrors", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.toerrors", "true", "-Werror"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wconversion", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wconversion", "true", "-Wconversion"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wcastalign", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wcastalign", "true", "-Wcast-align"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wcastqual", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wcastqual", "true", "-Wcast-qual"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wctordtorprivacy", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wctordtorprivacy", "true", "-Wctor-dtor-privacy"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wdisabledopt", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wdisabledopt", "true", "-Wdisabled-optimization"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wlogicalop", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wlogicalop", "true", "-Wlogical-op"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wmissingdecl", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wmissingdecl", "true", "-Wmissing-declarations"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wmissingincdir", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wmissingincdir", "true", "-Wmissing-include-dirs"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wnoexccept", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wnoexccept", "true", "-Wnoexcept"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.woldstylecast", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.woldstylecast", "true", "-Wold-style-cast"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.woverloadedvirtual", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.woverloadedvirtual", "true", "-Woverloaded-virtual"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wredundantdecl", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wredundantdecl", "true", "-Wredundant-decls"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wshadow", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wshadow", "true", "-Wshadow"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wsignconv", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wsignconv", "true", "-Wsign-conversion"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wsignpromo", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wsignpromo", "true", "-Wsign-promo"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wstrictnullsent", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wstrictnullsent", "true", "-Wstrict-null-sentinel"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wswitchdef", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wswitchdef", "true", "-Wswitch-default"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wundef", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wundef", "true", "-Wundef"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.weffcpp", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.weffcpp", "true", "-Weffc++"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wfloatequal", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.warnings.wfloatequal", "true", "-Wfloat-equal"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.other.verbose", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.other.verbose", "true", "-v"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.other.pic", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.other.pic", "true", "-fPIC"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.misc.hardening", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.misc.hardening", "true",
                "-fstack-protector-all -Wformat=2 -Wformat-security -Wstrict-overflow"));
        ret.add(Arguments.of("gnu.cpp.compiler.option.misc.randomization", "false", ""));
        ret.add(Arguments.of("gnu.cpp.compiler.option.misc.randomization", "true", "-fPIE"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "true", ""));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "false", "dddtddddddddddddd7"));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "true", ""));
        //        ret.add(Arguments.of("gddddddddddddddddnu", "false", "dddtddddddddddddd7"));

        return ret.stream();
    }

}
