package io.sloeber.core;
/*
 * This test compiles all examples on all Teensy hardware
 * For this test to be able to run you need to specify the
 * Teensy install folder of your system in MySystem.java
 *
 * Warning!! new teensy boards must be added manually!!!
 *
 * At the time of writing no examples are excluded
 * At the time of writing 1798 examples are compiled
 * 2 internal segmentation faults and 1 lto wrapper error
 * only the private static method skipExample allows to skip examples
 */


import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.sloeber.arduinoFramework.api.BoardDescription;
import io.sloeber.arduinoFramework.api.IExample;
import io.sloeber.arduinoFramework.api.LibraryManager;
import io.sloeber.core.api.CodeDescription;
import io.sloeber.core.api.CompileDescription;
import io.sloeber.core.api.Preferences;
import io.sloeber.providers.MCUBoard;
import io.sloeber.providers.Teensy;

@SuppressWarnings({ "nls" })
public class CreateAndCompileArduinoIDEExamplesOnTeensyTest {
    private int myBuildCounter = 0;
    private int myTotalFails = 0;
    private static int maxFails = 50;
    private static int mySkipAtStart = 0;

    @BeforeAll
    public static void setup() throws Exception {
    	Shared.waitForBoardsManager();
    	Teensy.installLatest();

    	Shared.setUseParralBuildProjects(Boolean.TRUE);

    	//Shared.setDefaultBuilder(AutoBuildProject.MAKE_BUILDER_ID);
        Shared.waitForAllJobsToFinish();
        Preferences.setUseBonjour(false);
    }


    public static Stream<Arguments> teensyHardwareData() throws Exception {

        List<Arguments> ret = new LinkedList<>();
        List<MCUBoard> allBoards = Teensy.getAllBoards();

        TreeMap<String, IExample> exampleFolders = LibraryManager.getExamplesFromIDE();
        for (Map.Entry<String, IExample> curexample : exampleFolders.entrySet()) {
            String fqn = curexample.getKey().trim();
            IPath examplePath = curexample.getValue().getCodeLocation();
            Example example = new Example(fqn, examplePath);
            if (!skipExample(example)) {
                Set<IExample> paths = new HashSet<>();
                paths.add(curexample.getValue());
                CodeDescription codeDescriptor = CodeDescription.createExample(false, paths);

                for (MCUBoard curBoard : allBoards) {
                    if (example.worksOnBoard(curBoard)) {
                        String projectName = Shared.getProjectName(codeDescriptor,  curBoard);
                        Map<String, String> boardOptions = curBoard.getBoardOptions(example);
                        BoardDescription boardDescriptor = curBoard.getBoardDescriptor();
                        boardDescriptor.setOptions(boardOptions);
                        ret.add(Arguments.of( projectName, codeDescriptor, boardDescriptor));
                    }
                }
            }
        }
		return ret.stream();
    }

    @SuppressWarnings("unused")
    private static boolean skipExample(Example example) {
        // no need to skip examples in this test
        return false;
    }


	@ParameterizedTest
	@MethodSource("teensyHardwareData")
    public void testArduinoIDEExamplesOnTeensy(String testName, CodeDescription codeDescriptor,
            BoardDescription board) throws Exception {
        assumeTrue( myBuildCounter++ >= mySkipAtStart,"Skipping first " + mySkipAtStart + " tests");
        assumeTrue( myTotalFails < maxFails,"To many fails. Stopping test");
        myTotalFails++;
        assertNull (Shared.buildAndVerify(testName, board, codeDescriptor, new CompileDescription()));
        myTotalFails--;
    }

}
