package io.sloeber.core;

/*
 * This test compiles all examples on all Arduino avr hardware
 * That is "compatible hardware as not all examples can be compiled for all hardware
 * for instance when serial2 is used a uno will not work but a mega will
 *
 *
 * At the time of writing 560 examples are compiled
 *
 */
import static org.junit.Assert.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.sloeber.core.api.CodeDescription;
import io.sloeber.core.api.CompileDescription;
import io.sloeber.core.api.IExample;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.api.Preferences;
import io.sloeber.providers.Arduino;
import io.sloeber.providers.MCUBoard;

@SuppressWarnings({ "nls" })
@RunWith(Parameterized.class)
public class CreateAndCompileArduinoIDEExamplesOnAVRHardwareTest {
    private CodeDescription myCodeDescriptor;
    private MCUBoard myBoard;
    private String myProjectName;
    private static int myTotalFails = 0;
    private static int maxFails = 50;
    private static int mySkipAtStart = 0;

    public CreateAndCompileArduinoIDEExamplesOnAVRHardwareTest(String projectName, CodeDescription codeDescriptor,
            MCUBoard board) {

        myCodeDescriptor = codeDescriptor;
        myBoard = board;
        myProjectName = projectName;
    }

    @SuppressWarnings("rawtypes")
    @Parameters(name = " {0}")
    public static Collection examples() {
        Shared.waitForAllJobsToFinish();
        Preferences.setUseBonjour(false);
        LinkedList<Object[]> examples = new LinkedList<>();
        List<MCUBoard> allBoards = Arduino.getAllBoards();

        TreeMap<String, IExample> exampleFolders = LibraryManager.getExamplesLibrary(null);
        for (Map.Entry<String, IExample> curexample : exampleFolders.entrySet()) {
            String fqn = curexample.getKey().trim();
            IPath examplePath = curexample.getValue().getCodeLocation();
            Example example = new Example(fqn, examplePath);
            if (!skipExample(example)) {
                Set<IExample> paths = new HashSet<>();

                paths.add( curexample.getValue());
                CodeDescription codeDescriptor = CodeDescription.createExample(false, paths);
                for (MCUBoard curboard : allBoards) {
                    if (curboard.isExampleSupported(example)) {
                        String projectName = Shared.getProjectName(codeDescriptor, example, curboard);
                        Object[] theData = new Object[] { projectName, codeDescriptor, curboard };
                        examples.add(theData);
                    }
                }
            }
        }

        return examples;

    }

    private static boolean skipExample(Example example) {
        // skip Teensy stuff on Arduino hardware
        // Teensy is so mutch more advanced that most arduino avr hardware can not
        // handle it
        return example.getCodeLocation().toString().contains("Teensy");
    }

    @Test
    public void testExample() {

        Assume.assumeTrue("Skipping first " + mySkipAtStart + " tests", Shared.buildCounter++ >= mySkipAtStart);
        Assume.assumeTrue("To many fails. Stopping test", myTotalFails < maxFails);

        if (!Shared.BuildAndVerify(myProjectName, myBoard.getBoardDescriptor(), myCodeDescriptor,
                new CompileDescription())) {
            myTotalFails++;
            fail(Shared.getLastFailMessage());
        }
    }

}
