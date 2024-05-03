package io.sloeber.core;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.eclipse.core.runtime.IPath;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.CodeDescription;
import io.sloeber.core.api.IExample;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.api.Preferences;
import io.sloeber.providers.Jantje;
import io.sloeber.providers.MCUBoard;

@SuppressWarnings({ "nls" })
@RunWith(Parameterized.class)
public class CreateAndCompileArduinoIDEExamplesonJantjesBoardsTest {
    private CodeDescription myCodeDescriptor;
    private static BoardDescription myBoard;
    private static int myBuildCounter = 0;
    private static int myTotalFails = 0;
    private static int maxFails = 200;
    private static int mySkipAtStart = 0;

    @SuppressWarnings("unused")
    public CreateAndCompileArduinoIDEExamplesonJantjesBoardsTest(String name, CodeDescription codeDescriptor,
            BoardDescription board) {

        myCodeDescriptor = codeDescriptor;
        myBoard = board;

    }

    @SuppressWarnings("rawtypes")
    @Parameters(name = "{0}")
    public static Collection examples() {
        Preferences.setUseBonjour(false);
        String[] packageUrlsToAdd = { Jantje.additionalJsonURL };
        BoardsManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), true);
        Jantje.installLatestLocalDebugBoards();
        Shared.waitForAllJobsToFinish();

        List<MCUBoard> allBoards = Jantje.getAllBoards();
        LinkedList<Object[]> examples = new LinkedList<>();

        Map<String, IExample> exampleFolders = LibraryManager.getExamplesLibrary(null);
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
                        Object[] theData = new Object[] { Shared.getCounterName(codeDescriptor.getExampleName()),
                                codeDescriptor, curboard.getBoardDescriptor() };
                        examples.add(theData);
                    }
                }
            }
        }

        return examples;

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

    @Test
    public void testExample() {
        Assume.assumeTrue("Skipping first " + mySkipAtStart + " tests", myBuildCounter++ >= mySkipAtStart);
        Assume.assumeTrue("To many fails. Stopping test", myTotalFails < maxFails);

        if (!Shared.BuildAndVerify(myBoard, myCodeDescriptor)) {
            myTotalFails++;
            fail(Shared.getLastFailMessage());
        }
    }

}
