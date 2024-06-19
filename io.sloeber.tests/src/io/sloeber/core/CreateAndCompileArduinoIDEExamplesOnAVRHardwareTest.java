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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IPath;
import org.junit.Assume;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import io.sloeber.core.api.CodeDescription;
import io.sloeber.core.api.CompileDescription;
import io.sloeber.core.api.IExample;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.api.Preferences;
import io.sloeber.providers.Arduino;
import io.sloeber.providers.MCUBoard;

@SuppressWarnings({ "nls" ,"static-method"})
public class CreateAndCompileArduinoIDEExamplesOnAVRHardwareTest {
    private static int myTotalFails = 0;
    private static int maxFails = 50;
    private static int mySkipAtStart = 0;


    public static Stream<Arguments>  avrHardwareData() {
        Shared.waitForAllJobsToFinish();
        Preferences.setUseBonjour(false);


//        Example testExample = new Example("Firmata:AllInputsFirmata", new Path("C:/Users/jan/Documents/Arduino/libraries/Firmata/examples/AllInputsFirmata"));
//        MCUBoard robotControler= Arduino.robotControl();
//        boolean ret=testExample.worksOnBoard(robotControler);


		List<Arguments> ret = new LinkedList<>();
        List<MCUBoard> allBoards = Arduino.getAllAvrBoards();
        TreeMap<String, IExample> exampleFolders = LibraryManager.getExamplesLibrary(null);
        for (Map.Entry<String, IExample> curexample : exampleFolders.entrySet()) {
            String fqn = curexample.getKey().trim();
            IPath examplePath = curexample.getValue().getCodeLocation();
            Example example = new Example(fqn, examplePath);
            if (skipExample(example)) {
            	System.out.println("skipping example "+example.getName());
            	continue;
            }
            int numExamplesToStart=ret.size();
            Set<IExample> paths = new HashSet<>();

                paths.add( curexample.getValue());
                CodeDescription codeDescriptor = CodeDescription.createExample(false, paths);
                for (MCUBoard curboard : allBoards) {
                    if (example.worksOnBoard(curboard)) {
                        String projectName = Shared.getProjectName(codeDescriptor,  curboard);
                        ret.add(Arguments.of( projectName, codeDescriptor, curboard ));
                    }
                }
                if(numExamplesToStart==ret.size()) {
                	System.err.println("No boards found for example "+example.getName());
                }
        }

		return ret.stream();

    }

    private static boolean skipExample(Example example) {
        // skip Teensy stuff on Arduino hardware
        // Teensy is so mutch more advanced that most arduino avr hardware can not
        // handle it
        return example.getCodeLocation().toString().contains("Teensy");
    }

	@ParameterizedTest
	@MethodSource("avrHardwareData")
    public void testExample(String projectName, CodeDescription codeDescriptor,
            MCUBoard board) throws Exception {

        Assume.assumeTrue("Skipping first " + mySkipAtStart + " tests", Shared.buildCounter++ >= mySkipAtStart);
        Assume.assumeTrue("To many fails. Stopping test", myTotalFails < maxFails);

        myTotalFails++;
        assertNull (Shared.buildAndVerify(projectName, board.getBoardDescriptor(), codeDescriptor,
                new CompileDescription())) ;
         myTotalFails++;
    }

}
