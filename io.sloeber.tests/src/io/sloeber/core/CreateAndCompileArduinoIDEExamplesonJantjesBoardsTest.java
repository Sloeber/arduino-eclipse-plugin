package io.sloeber.core;


import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IPath;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import io.sloeber.arduinoFramework.api.BoardDescription;
import io.sloeber.arduinoFramework.api.BoardsManager;
import io.sloeber.arduinoFramework.api.IExample;
import io.sloeber.arduinoFramework.api.LibraryManager;
import io.sloeber.core.api.CodeDescription;
import io.sloeber.core.api.Preferences;
import io.sloeber.providers.Jantje;
import io.sloeber.providers.MCUBoard;

@SuppressWarnings({ "nls","static-method" })
public class CreateAndCompileArduinoIDEExamplesonJantjesBoardsTest {
    private static int myBuildCounter = 0;
    private static int myTotalFails = 0;
    private static int maxFails = 200;
    private static int mySkipAtStart = 0;
    @BeforeAll
    public static void setup() throws Exception {
        Preferences.setUseBonjour(false);
        Shared.waitForAllJobsToFinish();
        String[] packageUrlsToAdd = { Jantje.additionalJsonURL };
        BoardsManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), true);
        Jantje.installLatestLocalDebugBoards();
        Shared.waitForAllJobsToFinish();

    	Shared.setUseParralBuildProjects(Boolean.TRUE);
    	//Shared.setDefaultBuilder(AutoBuildProject.MAKE_BUILDER_ID);
    }


    public static Stream<Arguments> jantjesHardwareData() throws Exception {
        List<MCUBoard> allBoards = Jantje.getAllBoards();
        List<Arguments> ret = new LinkedList<>();

        Map<String, IExample> exampleFolders = LibraryManager.getExamplesFromIDE();
        for (Map.Entry<String, IExample> curexample : exampleFolders.entrySet()) {
            String fqn = curexample.getKey().trim();
            IPath examplePath = curexample.getValue().getCodeLocation();
            Example example = new Example(fqn, examplePath);
            if (!skipExample(example)) {
                Set<IExample> paths = new HashSet<>();

                paths.add( curexample.getValue());
                CodeDescription codeDescriptor = CodeDescription.createExample(false, paths);
                for (MCUBoard curboard : allBoards) {
                    if (example.worksOnBoard(curboard) ) {
                    	ret.add(Arguments.of( codeDescriptor, curboard.getBoardDescriptor()));
                    }
                }
            }
        }
		return ret.stream();

    }

    private static boolean skipExample(Example example) {
        switch (example.getFQN()) {
        case "example/10.StarterKit/BasicKit/p13_TouchSensorLamp":
            return true;
        case "example/09.USB/KeyboardAndMouseControl":
            return true;
        case "example/09.USB/Mouse/JoystickMouseControl":
            return true;
        case "example/09.USB/Mouse/ButtonMouseControl":
            return true;
        case "example/09.USB/Keyboard/KeyboardSerial":
            return true;
        case "example/09.USB/Keyboard/KeyboardReprogram":
            return true;
        case "example/09.USB/Keyboard/KeyboardMessage":
            return true;
        case "example/09.USB/Keyboard/KeyboardLogout":
            return true;
        default:
            break;
        }
        return false;
    }

	@ParameterizedTest
	@MethodSource("jantjesHardwareData")
    public void testExample( CodeDescription codeDescriptor,
            BoardDescription board) throws Exception {
        assumeTrue( myBuildCounter++ >= mySkipAtStart,"Skipping first " + mySkipAtStart + " tests");
        assumeTrue( myTotalFails < maxFails,"To many fails. Stopping test");
        myTotalFails++;
        assertNull (Shared.buildAndVerify(board, codeDescriptor));
        myTotalFails--;
    }

}
