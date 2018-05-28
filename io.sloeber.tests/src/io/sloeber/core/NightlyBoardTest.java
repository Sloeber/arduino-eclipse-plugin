package io.sloeber.core;

/**
 * Test boards for board patrons and friends
 */
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.CompileOptions;
import io.sloeber.core.api.ConfigurationDescriptor;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.api.PackageManager;
import io.sloeber.core.api.Preferences;
import io.sloeber.providers.Arduino;
import io.sloeber.providers.MCUBoard;

@SuppressWarnings({ "nls", "unused" })
@RunWith(Parameterized.class)
public class NightlyBoardTest {
    private static final boolean reinstall_boards_and_examples = false;
    private static final boolean PARALLELBUILD = true;
    private static int myCounter = 0;
    private Examples myExample;
    private MCUBoard myBoardID;
    private static int skipAtStart = 0;
    private static int myTotalFails = 0;
    private static int maxFails = 40;
    private static CompileOptions myCompileOptions;

    public NightlyBoardTest(String name, MCUBoard boardID, Examples example) {
        myBoardID = boardID;
        myExample = example;
    }

    @SuppressWarnings("rawtypes")
    @Parameters(name = "{index}: {0}")
    public static Collection examples() {
        WaitForInstallerToFinish();
        Preferences.setUseArduinoToolSelection(true);
        myCompileOptions = new CompileOptions(null);
        myCompileOptions.setEnableParallelBuildForNewProject(PARALLELBUILD);
        MCUBoard zeroBoard = Arduino.zero();

        LinkedList<Object[]> examples = new LinkedList<>();
        TreeMap<String, IPath> exampleFolders = LibraryManager.getAllLibraryExamples();
        for (Map.Entry<String, IPath> curexample : exampleFolders.entrySet()) {
            String fqn = curexample.getKey().trim();
            IPath examplePath = curexample.getValue();
            //for patron Keith Willis. Thanks Keith
            if (fqn.contains("RTCZero")) {
                Examples example = new Examples(fqn, null, examplePath);

                Object[] theData = new Object[] { example.getLibName() + ":" + fqn + ":" + zeroBoard.getName(),
                        zeroBoard, example };
                examples.add(theData);
            }

        }

        return examples;

    }

    /*
     * In new new installations (of the Sloeber development environment) the
     * installer job will trigger downloads These mmust have finished before we can
     * start testing
     */

    public static void WaitForInstallerToFinish() {

        installAdditionalBoards();
        Shared.waitForAllJobsToFinish();
    }

    public static void installAdditionalBoards() {
        if (reinstall_boards_and_examples) {
            PackageManager.installAllLatestPlatforms();
            PackageManager.onlyKeepLatestPlatforms();
            // deal with removal of json files or libs from json files
            LibraryManager.removeAllLibs();
            LibraryManager.installAllLatestLibraries();
        }
    }

    @Test
    public void testExamples() {

        if (myTotalFails > maxFails) {
            // Stop after X fails because
            // the fails stays open in eclipse and it becomes really slow
            // There are only a number of issues you can handle
            // best is to focus on the first ones and then rerun
            // with a adapted skipAtStart
            // fail("To many fails. Stopping test");
            // failing is annoying when doing fixing
            return;
        }
        if (skipAtStart > myCounter++) {
            // skip these
            return;
        }
        if (!myBoardID.isExampleSupported(myExample)) {
            fail("Trying to run a test on unsoprted board");
            return;
        }
        ArrayList<IPath> paths = new ArrayList<>();

        paths.add(myExample.getPath());
        CodeDescriptor codeDescriptor = CodeDescriptor.createExample(false, paths);

        Map<String, String> boardOptions = myBoardID.getBoardOptions(myExample);
        BoardDescriptor boardDescriptor = myBoardID.getBoardDescriptor();
        boardDescriptor.setOptions(boardOptions);
        BuildAndVerify(myBoardID.getBoardDescriptor(), codeDescriptor, myCompileOptions);

    }

    public void BuildAndVerify(BoardDescriptor boardid, CodeDescriptor codeDescriptor, CompileOptions compileOptions) {

        IProject theTestProject = null;

        NullProgressMonitor monitor = new NullProgressMonitor();
        String projectName = String.format("%05d_%1.100s", new Integer(myCounter), myExample.getFQN());
        try {

            theTestProject = boardid.createProject(projectName, null, ConfigurationDescriptor.getDefaultDescriptors(),
                    codeDescriptor, compileOptions, monitor);
            Shared.waitForAllJobsToFinish(); // for the indexer
        } catch (Exception e) {
            e.printStackTrace();
            myTotalFails++;
            fail("Failed to create the project:" + projectName);
            return;
        }
        try {
            theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
            if (Shared.hasBuildErrors(theTestProject)) {
                // try again because the libraries may not yet been added
                Shared.waitForAllJobsToFinish(); // for the indexer
                try {
                    Thread.sleep(3000);// seen sometimes the libs were still not
                                       // added
                } catch (InterruptedException e) {
                    // ignore
                }
                theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
                if (Shared.hasBuildErrors(theTestProject)) {
                    // give up
                    myTotalFails++;
                    theTestProject.close(null);
                    fail("Failed to compile the project:" + projectName + " build errors");
                } else {
                //    theTestProject.delete(true, null);
                }
            } else {
              //  theTestProject.delete(true, null);
            }
        } catch (CoreException e) {
            e.printStackTrace();
            myTotalFails++;
            fail("Failed to compile the project:" + projectName + " exception");
        }
    }

}
