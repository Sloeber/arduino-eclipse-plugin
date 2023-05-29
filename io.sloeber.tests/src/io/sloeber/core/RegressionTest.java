package io.sloeber.core;

import static io.sloeber.core.common.Const.*;
import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;

import org.apache.commons.io.FileUtils;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.junit.BeforeClass;
import org.junit.Test;

import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.CodeDescription;
import io.sloeber.core.api.CompileDescription;
import io.sloeber.core.api.CompileDescription.SizeCommands;
import io.sloeber.core.api.CompileDescription.WarningLevels;
import io.sloeber.core.api.OtherDescription;
import io.sloeber.core.api.Preferences;
import io.sloeber.core.api.SloeberProject;
import io.sloeber.providers.Arduino;
import io.sloeber.providers.ESP32;
import io.sloeber.providers.ESP8266;
import io.sloeber.providers.MCUBoard;
import io.sloeber.providers.Teensy;

@SuppressWarnings({ "nls", "static-method" })
public class RegressionTest {
    private static final boolean reinstall_boards_and_libraries = false;

    /*
     * In new new installations (of the Sloeber development environment) the
     * installer job will trigger downloads These mmust have finished before we can
     * start testing
     */
    @BeforeClass
    public static void WaitForInstallerToFinish() {
        Shared.waitForAllJobsToFinish();
        Preferences.setUseBonjour(false);
        installAdditionalBoards();
    }

    public static void installAdditionalBoards() {

        String[] packageUrlsToAdd = { ESP8266.packageURL, ESP32.packageURL };
        BoardsManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), true);
        if (reinstall_boards_and_libraries) {
            BoardsManager.removeAllInstalledPlatforms();
        }
        ;
        // make sure the needed boards are available
        ESP8266.installLatest();
        ESP32.installLatest();
        Arduino.installLatestAVRBoards();

        if (!MySystem.getTeensyPlatform().isEmpty()) {
            BoardsManager.addPrivateHardwarePath(MySystem.getTeensyPlatform());
        }
    }

    /**
     * make sure when switching between a board with variant file and without the
     * build still succeeds
     */
    @Test
    public void issue555() {
        if (MySystem.getTeensyPlatform().isEmpty()) {
            // skip test due to no teensy install folder provided
            // do not fail as this will always fail on travis
            System.out.println("skipping the test because teensy is not installed.");
            return;
        }
        System.out.println("Teensy is installed at " + MySystem.getTeensyPlatform());
        BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();
        BoardDescription teensyBoardid = Teensy.Teensy3_1().getBoardDescriptor();

        IProject theTestProject = null;
        CodeDescription codeDescriptor = CodeDescription.createDefaultIno();
        String projectName = "issue555";
        NullProgressMonitor monitor = new NullProgressMonitor();
        try {
            theTestProject = SloeberProject.createArduinoProject(projectName, null, unoBoardid, codeDescriptor,
                    new CompileDescription(), monitor);
            Shared.waitForAllJobsToFinish(); // for the indexer
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to create the project:" + projectName);
            return;
        }
        try {
            theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
            if (Shared.hasBuildErrors(theTestProject)) {
                fail("Failed to compile the project:" + projectName + " as uno  build errors");
            }
        } catch (CoreException e) {
            e.printStackTrace();
            fail("Failed to compile the project:" + unoBoardid.getBoardName() + " as uno exception");
        }
        SloeberProject arduinoProject = SloeberProject.getSloeberProject(theTestProject);
        ICProjectDescription cProjectDescription = CCorePlugin.getDefault().getProjectDescription(theTestProject);
        arduinoProject.setBoardDescription(cProjectDescription.getActiveConfiguration().getName(), teensyBoardid, true);

        Shared.waitForAllJobsToFinish();
        try {
            theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
            if (Shared.hasBuildErrors(theTestProject)) {
                Shared.waitForAllJobsToFinish();
                theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
                if (Shared.hasBuildErrors(theTestProject)) {
                    fail("Failed to compile the project:" + projectName + " as teensy");
                }
            }
        } catch (CoreException e) {
            e.printStackTrace();
            fail("Failed to compile the project:" + unoBoardid.getBoardName() + " as teensy exception");
        }
    }

    /**
     * support void loop{};
     * 
     * @throws Exception
     */
    @Test
    public void issue687() throws Exception {
        BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();

        IProject theTestProject = null;
        String projectName = "issue687";
        IPath templateFolder = Shared.getTemplateFolder(projectName);
        CodeDescription codeDescriptor = CodeDescription.createCustomTemplate(templateFolder);
        try {
            theTestProject = SloeberProject.createArduinoProject(projectName, null, unoBoardid, codeDescriptor,
                    new CompileDescription(), new NullProgressMonitor());
            Shared.waitForAllJobsToFinish(); // for the indexer
            theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
            if (Shared.hasBuildErrors(theTestProject)) {
                fail("Failed to compile the project:" + projectName + " issue687 is not fixed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to create the project:" + projectName);
            return;
        }

    }

    @Test
    public void create_CPP_based_Sloeber_Project() throws Exception {
        BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();

        IProject theTestProject = null;
        String projectName = "createCPPProject";

        CodeDescription codeDescriptor = CodeDescription.createDefaultCPP();
        try {
            theTestProject = SloeberProject.createArduinoProject(projectName, null, unoBoardid, codeDescriptor,
                    new CompileDescription(), new NullProgressMonitor());
            Shared.waitForAllJobsToFinish(); // for the indexer
            theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
            if (Shared.hasBuildErrors(theTestProject)) {
                fail("Failed to compile the project:" + projectName + " CPP project no longer work");
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to create the project:" + projectName);
            return;
        }

    }

    @Test
    public void createDefaultInoProject() throws Exception {
        BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();

        IProject theTestProject = null;
        String projectName = "createDefaultInoProject";

        CodeDescription codeDescriptor = CodeDescription.createDefaultIno();
        try {
            theTestProject = SloeberProject.createArduinoProject(projectName, null, unoBoardid, codeDescriptor,
                    new CompileDescription(), new NullProgressMonitor());
            Shared.waitForAllJobsToFinish(); // for the indexer
            theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
            if (Shared.hasBuildErrors(theTestProject)) {
                fail("Failed to compile the project:" + projectName + " INO project no longer work");
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to create the project:" + projectName);
            return;
        }

    }

    /**
     * support void loop{};
     * 
     * @throws Exception
     */
    @Test
    public void issue1047_Board_Names_Can_Be_used_as_Strings() throws Exception {
        MCUBoard unoBoard = ESP8266.nodeMCU();

        String projectName = "issue1047_Board_Names_Can_Be_used_as_Strings";
        IPath templateFolder = Shared.getTemplateFolder(projectName);
        CodeDescription codeDescriptor = CodeDescription.createCustomTemplate(templateFolder);
        try {
            IProject theTestProject = SloeberProject.createArduinoProject(projectName, null,
                    unoBoard.getBoardDescriptor(), codeDescriptor, new CompileDescription(), new NullProgressMonitor());
            Shared.waitForAllJobsToFinish(); // for the indexer
            theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, new NullProgressMonitor());
            if (Shared.hasBuildErrors(theTestProject)) {
                fail("Failed to compile the project:" + projectName + " issue1047 is not fixed");
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to create the project:" + projectName);
            return;
        }

    }

    /**
     * This test will fail if the arduino compile option are not taken into account
     * To do sa a bunch of defines are added to the command line and the code checks
     * whether these defines are set properly
     * 
     * @throws Exception
     */
    @Test
    public void are_jantjes_options_taken_into_account() throws Exception {
        BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();

        IProject theTestProject = null;
        String projectName = "are_defines_found";
        IPath templateFolder = Shared.getTemplateFolder(projectName);
        CodeDescription codeDescriptor = CodeDescription.createCustomTemplate(templateFolder);

        NullProgressMonitor monitor = new NullProgressMonitor();
        try {
            CompileDescription compileOptions = new CompileDescription();
            compileOptions.set_C_andCPP_CompileOptions("-DTEST_C_CPP");
            compileOptions.set_C_CompileOptions("-DTEST_C");
            compileOptions.set_CPP_CompileOptions("-DTEST_CPP");
            theTestProject = SloeberProject.createArduinoProject(projectName, null, unoBoardid, codeDescriptor,
                    compileOptions, new NullProgressMonitor());

            Shared.waitForAllJobsToFinish(); // for the indexer
            theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
            if (Shared.hasBuildErrors(theTestProject)) {
                fail("Failed to compile the project:" + projectName
                        + " The defines have not been taken into account properly");
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to create the project:" + projectName);
            return;
        }
    }

    /**
     * If a .ino file is including a include using extern C is this handled properly
     * by the ino to cpp parser
     * 
     * @throws Exception
     */
    @Test
    public void is_extern_C_taken_into_account() throws Exception {
        BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();

        IProject theTestProject = null;
        String projectName = "externc";
        IPath templateFolder = Shared.getTemplateFolder(projectName);
        CodeDescription codeDescriptor = CodeDescription.createCustomTemplate(templateFolder);

        NullProgressMonitor monitor = new NullProgressMonitor();
        try {
            theTestProject = SloeberProject.createArduinoProject(projectName, null, unoBoardid, codeDescriptor,
                    new CompileDescription(), new NullProgressMonitor());

            Shared.waitForAllJobsToFinish(); // for the indexer
            theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
            if (Shared.hasBuildErrors(theTestProject)) {
                fail("Failed to compile the project:" + projectName
                        + " extern \"C\" has not been taken into account properly.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            fail("Failed to create the project:" + projectName);
            return;
        }
    }

    /**
     * If a .ino file is defining defines before including a include this should be
     * handled properly by the ino to cpp parser
     * 
     * @throws Exception
     */
    @Test
    public void are_defines_before_includes_taken_into_account() throws Exception {
        BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();

        IProject theTestProject = null;
        String projectName = "defines_and_includes";
        IPath templateFolder = Shared.getTemplateFolder(projectName);
        CodeDescription codeDescriptor = CodeDescription.createCustomTemplate(templateFolder);

        NullProgressMonitor monitor = new NullProgressMonitor();
        theTestProject = SloeberProject.createArduinoProject(projectName, null, unoBoardid, codeDescriptor,
                new CompileDescription(), new NullProgressMonitor());

        Shared.waitForAllJobsToFinish(); // for the indexer
        theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
        if (Shared.hasBuildErrors(theTestProject)) {
            fail("Failed to compile the project:" + projectName
                    + " defines have not been taken into account properly.");
        }

    }

    /**
     * Does Sloeber still compile after a configuration renamen
     * 
     * @throws Exception
     */
    @Test
    public void rename_Configuration() throws Exception {
        BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();

        IProject theTestProject = null;
        String projectName = "rename_Configuration";

        CodeDescription codeDescriptor = CodeDescription.createDefaultIno();

        NullProgressMonitor monitor = new NullProgressMonitor();
        theTestProject = SloeberProject.createArduinoProject(projectName, null, unoBoardid, codeDescriptor,
                new CompileDescription(), new NullProgressMonitor());

        Shared.waitForAllJobsToFinish(); // for the indexer
        theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
        if (Shared.hasBuildErrors(theTestProject)) {
            fail("Failed to compile the project before config rename");
        }

        CCorePlugin cCorePlugin = CCorePlugin.getDefault();
        ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(theTestProject);
        ICConfigurationDescription activeConfig = prjCDesc.getActiveConfiguration();
        activeConfig.setName("renamedConfig");
        cCorePlugin.setProjectDescription(theTestProject, prjCDesc);

        Shared.waitForAllJobsToFinish(); // for the indexer
        theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
        if (Shared.hasBuildErrors(theTestProject)) {
            fail("Failed to compile the project after config rename");
        }
    }

    //    /**
    //     * Does Sloeber still compile after a project rename
    //     * 
    //     * @throws Exception
    //     */
    //    @Test
    //    public void rename_Project() throws Exception {
    //        BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();
    //
    //        IProject theTestProject = null;
    //        String projectName = "rename_project";
    //        String projectNameRenamed = "renamed_project";
    //
    //        CodeDescription codeDescriptor = CodeDescription.createDefaultIno();
    //
    //        NullProgressMonitor monitor = new NullProgressMonitor();
    //        theTestProject = SloeberProject.createArduinoProject(projectName, null, unoBoardid, codeDescriptor,
    //                new CompileDescription(), new NullProgressMonitor());
    //
    //        Shared.waitForAllJobsToFinish(); // for the indexer
    //        theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    //        if (Shared.hasBuildErrors(theTestProject)) {
    //            fail("Failed to compile the project before project rename");
    //        }
    //        theTestProject.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
    //
    //        CCorePlugin cCorePlugin = CCorePlugin.getDefault();
    //        ICProjectDescription prjCDesc = cCorePlugin.getProjectDescription(theTestProject);
    //
    //        IProjectDescription descr = theTestProject.getDescription();
    //        descr.ren(projectNameRenamed);
    //        theTestProject.setDescription(descr, null);
    //
    //        Shared.waitForAllJobsToFinish(); // for the indexer
    //        theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
    //        if (Shared.hasBuildErrors(theTestProject)) {
    //            fail("Failed to compile the project after project rename");
    //        }
    //    }

    /**
     * open and close a project should keep the compileDescription and
     * BoardDescriotion
     * 
     * @throws Exception
     */
    @Test
    public void openAndClosePreservesSettings() throws Exception {
        BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();

        IProject theTestProject = null;
        String projectName = "openAndClose";
        CodeDescription codeDescriptor = new CodeDescription(CodeDescription.CodeTypes.defaultCPP);
        CompileDescription inCompileDescription = getBunkersCompileDescription();

        theTestProject = SloeberProject.createArduinoProject(projectName, null, unoBoardid, codeDescriptor,
                inCompileDescription, new NullProgressMonitor());

        // Read the data we want to test
        Shared.waitForAllJobsToFinish(); // for the indexer
        SloeberProject sloeberDesc = SloeberProject.getSloeberProject(theTestProject);
        ICProjectDescription projDesc = CoreModel.getDefault().getProjectDescription(theTestProject);
        ICConfigurationDescription confDesc = projDesc.getActiveConfiguration();
        BoardDescription createdBoardDesc = sloeberDesc.getBoardDescription(confDesc.getName(), false);
        CompileDescription createdCompileDesc = sloeberDesc.getCompileDescription(confDesc.getName(), false);

        // close and reopen the project
        theTestProject.close(null);
        // just wait a while
        Thread.sleep(1000);
        Shared.waitForAllJobsToFinish();
        theTestProject.open(null);
        Shared.waitForAllJobsToFinish();

        // read the data we want to test
        sloeberDesc = SloeberProject.getSloeberProject(theTestProject);
        projDesc = CoreModel.getDefault().getProjectDescription(theTestProject);
        confDesc = projDesc.getActiveConfiguration();
        BoardDescription reopenedBoardDesc = sloeberDesc.getBoardDescription(confDesc.getName(), false);
        CompileDescription reopenedCompileDesc = sloeberDesc.getCompileDescription(confDesc.getName(), false);

        // check the data is equal
        boolean createBoardsDiff = !unoBoardid.equals(createdBoardDesc);
        boolean createCompileDiff = !inCompileDescription.equals(createdCompileDesc);
        boolean openBoardsDiff = !reopenedBoardDesc.equals(createdBoardDesc);
        boolean openCompileDiff = !reopenedCompileDesc.equals(createdCompileDesc);

        if (createBoardsDiff || createCompileDiff) {
            fail("Created project does not match creation parameters.");
        }
        if (openBoardsDiff || openCompileDiff) {
            fail("Opened project does not match closed project parameters.");
        }

        // also do a build
        Shared.waitForAllJobsToFinish(); // for the indexer
        theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, null);
        if (Shared.hasBuildErrors(theTestProject)) {
            fail("Failed to compile the project:" + projectName);
        }

    }

    @Test
    public void closeProjectRemovesPropertiesSloeber() throws Exception {
        String DummyData = "a object";
        String projectName = "closeProjectRemovesPropertiesSloeber";
        QualifiedName qualifiedName = new QualifiedName("io.sloebertest", projectName);

        BoardDescription unoBoardid = Arduino.uno().getBoardDescriptor();

        CodeDescription codeDescriptor = new CodeDescription(CodeDescription.CodeTypes.defaultCPP);
        CompileDescription inCompileDescription = getBunkersCompileDescription();

        IProject project = SloeberProject.createArduinoProject(projectName, null, unoBoardid, codeDescriptor,
                inCompileDescription, new NullProgressMonitor());

        // Read the data we want to test
        Shared.waitForAllJobsToFinish(); // for the indexer

        // project.open(null);
        project.setSessionProperty(qualifiedName, DummyData);
        project.close(null);
        project.open(null);
        Object projData = project.getSessionProperty(qualifiedName);
        if (projData != null) {
            fail("non persistent projectdescription properties behave persistent during project close open in Sloeber");
        }
    }

    /**
     * open and close a project should keep the compileDescription and
     * BoardDescriotion
     * 
     * @throws Exception
     */
    @Test
    public void openAndCloseUsesSavedSettings() throws Exception {
        CodeDescription codeDesc = new CodeDescription(CodeDescription.CodeTypes.defaultCPP);

        String proj1Name = "openModAndClose1";
        BoardDescription proj1BoardDesc = Arduino.uno().getBoardDescriptor();
        OtherDescription otherDesc = new OtherDescription();
        otherDesc.setVersionControlled(true);
        CompileDescription proj1CompileDesc = getBunkersCompileDescription();
        IProject proj1 = SloeberProject.createArduinoProject(proj1Name, null, proj1BoardDesc, codeDesc,
                proj1CompileDesc, otherDesc, new NullProgressMonitor());

        String proj2Name = "openModAndClose2";
        BoardDescription proj2BoardDesc = Arduino.mega2560Board().getBoardDescriptor();
        CompileDescription proj2CompileDesc = new CompileDescription();
        IProject proj2 = SloeberProject.createArduinoProject(proj2Name, null, proj2BoardDesc, codeDesc,
                proj2CompileDesc, new NullProgressMonitor());

        // Read the data we want to test
        Shared.waitForAllJobsToFinish(); // for the indexer
        SloeberProject proj1SloeberDesc = SloeberProject.getSloeberProject(proj1);
        ICProjectDescription proj1Desc = CoreModel.getDefault().getProjectDescription(proj1);
        ICConfigurationDescription proj1ConfDesc = proj1Desc.getActiveConfiguration();
        BoardDescription proj1CreatedBoardDesc = proj1SloeberDesc.getBoardDescription(proj1ConfDesc.getName(), false);
        CompileDescription proj1CreatedCompileDesc = proj1SloeberDesc.getCompileDescription(proj1ConfDesc.getName(),
                false);

        SloeberProject proj2SloeberDesc = SloeberProject.getSloeberProject(proj2);
        ICProjectDescription proj2Desc = CoreModel.getDefault().getProjectDescription(proj2);
        ICConfigurationDescription proj2ConfDesc = proj2Desc.getActiveConfiguration();
        BoardDescription proj2CreatedBoardDesc = proj2SloeberDesc.getBoardDescription(proj2ConfDesc.getName(), false);
        CompileDescription proj2CreatedCompileDesc = proj2SloeberDesc.getCompileDescription(proj2ConfDesc.getName(),
                false);

        // get the filenames to copy
        IFile file = proj1.getFile(SLOEBER_CFG);
        File proj1SloeberFile = file.getLocation().toFile();

        file = proj2.getFile(SLOEBER_CFG);
        File proj2SloeberFile = file.getLocation().toFile();

        // close and reopen the project
        proj2.close(null);
        // just wait a while
        Thread.sleep(1000);
        Shared.waitForAllJobsToFinish();

        // copy from proj1 to proj2
        FileUtils.copyFile(proj1SloeberFile, proj2SloeberFile);

        // reopen the project
        proj2.open(null);
        Thread.sleep(1000);
        Shared.waitForAllJobsToFinish();

        // reread project 2
        proj2SloeberDesc = SloeberProject.getSloeberProject(proj2);
        proj2Desc = CoreModel.getDefault().getProjectDescription(proj2);
        proj2ConfDesc = proj2Desc.getActiveConfiguration();
        BoardDescription proj2OpenedBoardDesc = proj2SloeberDesc.getBoardDescription(proj2ConfDesc.getName(), false);
        CompileDescription proj2OpenedCompileDesc = proj2SloeberDesc.getCompileDescription(proj2ConfDesc.getName(),
                false);

        // check the setup was done correctly
        if (!proj1BoardDesc.equals(proj1CreatedBoardDesc)) {
            fail("Project 1 not created properly.");
        }
        if (!proj2BoardDesc.equals(proj2CreatedBoardDesc)) {
            fail("Project 2 not created properly.");
        }
        if (!proj1CompileDesc.equals(proj1CreatedCompileDesc)) {
            fail("Project 1 not created properly.");
        }
        if (!proj2CompileDesc.equals(proj2CreatedCompileDesc)) {
            fail("Project 2 not created properly.");
        }

        // check wether the file modification was taken into account
        if (!proj1BoardDesc.equals(proj2OpenedBoardDesc)) {
            fail("Project 2 not created properly.");
        }
        if (!proj1CompileDesc.equals(proj2OpenedCompileDesc)) {
            fail("Project 2 not created properly.");
        }

    }

    static CompileDescription getBunkersCompileDescription() {
        CompileDescription inCompileDescription = new CompileDescription();

        inCompileDescription.set_All_CompileOptions("-Deen=1");
        inCompileDescription.set_Archive_CompileOptions("-Dtwee=2");
        inCompileDescription.set_Assembly_CompileOptions("-Drie=3");
        inCompileDescription.set_C_andCPP_CompileOptions("-Dvier=4");
        inCompileDescription.set_C_CompileOptions("-Dvijf=5");
        inCompileDescription.set_Link_CompileOptions("-Dzes=6");
        inCompileDescription.set_CPP_CompileOptions("-Dzeven=7");
        inCompileDescription.setSizeCommand(SizeCommands.ARDUINO_WAY);
        inCompileDescription.setEnableParallelBuild(true);
        inCompileDescription.setWarningLevel(WarningLevels.NONE);
        return inCompileDescription;
    }

    /**
     * check to see whether upload recipe key is correct for a couple of boards that
     * have failed in the past
     * 
     * @throws Exception
     */
    @Test
    public void uploadPattern() throws Exception {
        BoardDescription boardDescriptor = Arduino.uno().getBoardDescriptor();
        String recipeKey = boardDescriptor.getUploadPatternKey();
        assertEquals("uno upload recipe key is wrong", "tools.avrdude.upload.pattern", recipeKey);
        boardDescriptor = ESP32.esp32().getBoardDescriptor();
        boardDescriptor.setUploadPort("host 10.10.10.10");
        recipeKey = boardDescriptor.getUploadPatternKey();
        assertEquals("ESP OTA upload recipe key is wrong", "tools.esptool_py.upload.network_pattern", recipeKey);

    }
}
