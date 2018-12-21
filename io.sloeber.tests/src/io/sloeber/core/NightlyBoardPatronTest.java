package io.sloeber.core;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.CompileOptions;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.api.Preferences;
import io.sloeber.providers.Arduino;
import io.sloeber.providers.MCUBoard;

@SuppressWarnings({ "nls" })
@RunWith(Parameterized.class)
public class NightlyBoardPatronTest {

    private static int myBuildCounter = 0;
    private Examples myExample;
    private MCUBoard myBoardID;
    private static int mySkipAtStart = 0;
    private static int myTotalFails = 0;
    private static int maxFails = 40;
    private static CompileOptions myCompileOptions;

    public NightlyBoardPatronTest(String name, MCUBoard boardID, Examples example) {
        myBoardID = boardID;
        myExample = example;
    }

    @SuppressWarnings("rawtypes")
    @Parameters(name = "{0}")
    public static Collection examples() {
        Shared.waitForAllJobsToFinish();
        Arduino.installLatestSamDBoards();
        LibraryManager.installLibrary("RTCZero");
        Shared.waitForAllJobsToFinish();
        Preferences.setUseArduinoToolSelection(true);
        myCompileOptions = new CompileOptions(null);
        MCUBoard zeroBoard = Arduino.zeroProgrammingPort();

        LinkedList<Object[]> examples = new LinkedList<>();
        TreeMap<String, IPath> exampleFolders = LibraryManager.getAllLibraryExamples();
        for (Map.Entry<String, IPath> curexample : exampleFolders.entrySet()) {
            String fqn = curexample.getKey().trim();
            IPath examplePath = curexample.getValue();
            //for patron Keith Willis. Thanks Keith
            if (fqn.contains("RTCZero")) {
                Examples example = new Examples(fqn,  examplePath);

                Object[] theData = new Object[] {Shared.getCounterName( example.getLibName() + ":" + fqn + ":" + zeroBoard.getID()),
                        zeroBoard, example };
                examples.add(theData);
            }

        }

        return examples;

    }




    @Test
    public void testExamples() {

        Assume.assumeTrue("Skipping first " + mySkipAtStart + " tests", myBuildCounter++ >= mySkipAtStart);
        Assume.assumeTrue("To many fails. Stopping test", myTotalFails < maxFails);
        
        ArrayList<IPath> paths = new ArrayList<>();

        paths.add(myExample.getPath());
        CodeDescriptor codeDescriptor = CodeDescriptor.createExample(false, paths);

        Map<String, String> boardOptions = myBoardID.getBoardOptions(myExample);
        BoardDescriptor boardDescriptor = myBoardID.getBoardDescriptor();
        boardDescriptor.setOptions(boardOptions);
        if(!Shared.BuildAndVerify(myBoardID.getBoardDescriptor(), codeDescriptor, myCompileOptions)) {
            myTotalFails++;
            fail(Shared.getLastFailMessage() );
        }

    }


}
