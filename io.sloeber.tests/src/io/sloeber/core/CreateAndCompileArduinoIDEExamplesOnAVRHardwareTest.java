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

import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.CompileOptions;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.api.SerialManager;
import io.sloeber.providers.Arduino;
import io.sloeber.providers.MCUBoard;

@SuppressWarnings({ "nls" })
@RunWith(Parameterized.class)
public class CreateAndCompileArduinoIDEExamplesOnAVRHardwareTest {
    private CodeDescriptor myCodeDescriptor;
    private MCUBoard myBoard;
    private String myProjectName;
    private static int myBuildCounter = 0;
    private static int myTotalFails = 0;
    private static int maxFails = 50;
    private static int mySkipAtStart = 0;

    public CreateAndCompileArduinoIDEExamplesOnAVRHardwareTest( String projectName,CodeDescriptor codeDescriptor,
    		MCUBoard board) {

        myCodeDescriptor = codeDescriptor;
        myBoard = board;
        myProjectName =projectName;
    }

    @SuppressWarnings("rawtypes")
    @Parameters(name = " {0}")
    public static Collection examples() {
        Shared.waitForAllJobsToFinish();
        SerialManager.stopNetworkScanning();
        LinkedList<Object[]> examples = new LinkedList<>();
        MCUBoard[] allBoards=Arduino.getAllBoards();

        TreeMap<String, IPath> exampleFolders = LibraryManager.getAllArduinoIDEExamples();
        for (Map.Entry<String, IPath> curexample : exampleFolders.entrySet()) {
            String fqn = curexample.getKey().trim();
            IPath examplePath = curexample.getValue();
            Examples example = new Examples(fqn,  examplePath);
            if (!skipExample(example)) {
                ArrayList<IPath> paths = new ArrayList<>();

                paths.add(examplePath);
                CodeDescriptor codeDescriptor = CodeDescriptor.createExample(false, paths);
				for (MCUBoard curboard : allBoards) {
			        if (curboard.isExampleSupported(example)) {
			        	String projectName=Shared.getProjectName(codeDescriptor, example, curboard);
                Object[] theData = new Object[] { projectName, codeDescriptor,  curboard};
                examples.add(theData);
			        }
				}
            }
        }

        return examples;

    }

    private static boolean skipExample(Examples example) {
        // skip Teensy stuff on Arduino hardware
        // Teensy is so mutch more advanced that most arduino avr hardware can not
        // handle
        // it
        return example.getPath().toString().contains("Teensy");
    }

    @Test
    public void testExample() {
        // Stop after X fails because
        // the fails stays open in eclipse and it becomes really slow
        // There are only a number of issues you can handle
        // best is to focus on the first ones and then rerun starting with the
        // failures
        Assume.assumeTrue("Skipping first " + mySkipAtStart + " tests", myBuildCounter++ >= mySkipAtStart);
        Assume.assumeTrue("To many fails. Stopping test", myTotalFails < maxFails);
        //because we run all examples on all boards we need to filter incompatible combinations
        //like serial examples on gemma

        if (!Shared.BuildAndVerify( myProjectName,myBoard.getBoardDescriptor(), myCodeDescriptor, new CompileOptions(null))) {
            myTotalFails++;
            fail(Shared.getLastFailMessage() );
        }
    }


}
