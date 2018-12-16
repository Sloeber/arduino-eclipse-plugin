package io.sloeber.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
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
import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.api.PackageManager;
import io.sloeber.core.api.SerialManager;
import io.sloeber.providers.Jantje;
import io.sloeber.providers.MCUBoard;

@SuppressWarnings({"nls"})
@RunWith(Parameterized.class)
public class CreateAndCompileJantjesBoardsTest {
	private CodeDescriptor myCodeDescriptor;
	private static BoardDescriptor myBoard;
    private static int myBuildCounter = 0;
    private static int myTotalFails = 0;
    private static int maxFails = 200;
    private static int mySkipAtStart = 0;

	public CreateAndCompileJantjesBoardsTest( String name,CodeDescriptor codeDescriptor,BoardDescriptor board) {

		myCodeDescriptor = codeDescriptor;
		myBoard=board;

	}

	@SuppressWarnings("rawtypes")
	@Parameters(name = "{0}")
	public static Collection examples() {
		String[] packageUrlsToAdd = {Jantje.jsonURL };
		MCUBoard[] allBoards=Jantje.getAllBoards();
		PackageManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), true);
		Jantje.installLatestLocalDebugBoards();
		SerialManager.stopNetworkScanning();


		Shared.waitForAllJobsToFinish();
		LinkedList<Object[]> examples = new LinkedList<>();

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
						Object[] theData = new Object[] {Shared.getCounterName(codeDescriptor.getExampleName()), codeDescriptor ,curboard.getBoardDescriptor()};
						examples.add(theData);
			        }
				}
			}
		}

		return examples;

	}

	private static boolean skipExample(Examples example) {
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
		}
		return false;
	}
	@Test
	public void testExample() {
        // Stop after X fails because
        // the fails stays open in eclipse and it becomes really slow
        // There are only a number of issues you can handle
        // best is to focus on the first ones and then rerun starting with the
        // failures
        Assume.assumeTrue("Skipping first " + mySkipAtStart + " tests", (myBuildCounter++ >= mySkipAtStart)?Shared.increaseBuildCounter():false);
        Assume.assumeTrue("To many fails. Stopping test", (myTotalFails < maxFails)?Shared.increaseBuildCounter():false);
        //because we run all examples on all boards we need to filter incompatible combinations
        //like serial examples on gemma

        if (!Shared.BuildAndVerify( myBoard, myCodeDescriptor, null)) {
            myTotalFails++;
        }
	}

}
