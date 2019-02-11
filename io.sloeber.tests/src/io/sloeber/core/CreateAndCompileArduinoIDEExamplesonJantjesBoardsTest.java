package io.sloeber.core;

import static org.junit.Assert.fail;

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
import io.sloeber.core.api.Preferences;
import io.sloeber.providers.Jantje;
import io.sloeber.providers.MCUBoard;

@SuppressWarnings({"nls"})
@RunWith(Parameterized.class)
public class CreateAndCompileArduinoIDEExamplesonJantjesBoardsTest {
	private CodeDescriptor myCodeDescriptor;
	private static BoardDescriptor myBoard;
    private static int myBuildCounter = 0;
    private static int myTotalFails = 0;
    private static int maxFails = 200;
    private static int mySkipAtStart = 0;

	public CreateAndCompileArduinoIDEExamplesonJantjesBoardsTest( String name,CodeDescriptor codeDescriptor,BoardDescriptor board) {

		myCodeDescriptor = codeDescriptor;
		myBoard=board;

	}

	@SuppressWarnings("rawtypes")
	@Parameters(name = "{0}")
	public static Collection examples() {
		Preferences.setUseBonjour(false);
		String[] packageUrlsToAdd = {Jantje.jsonURL };
		PackageManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), true);
		Jantje.installLatestLocalDebugBoards();
		Shared.waitForAllJobsToFinish();
		
		MCUBoard[] allBoards=Jantje.getAllBoards();
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
		Assume.assumeTrue("Skipping first " + mySkipAtStart + " tests", myBuildCounter++ >= mySkipAtStart);
		Assume.assumeTrue("To many fails. Stopping test", myTotalFails < maxFails);


        if (!Shared.BuildAndVerify( myBoard, myCodeDescriptor)) {
            myTotalFails++;
            fail(Shared.getLastFailMessage() );
        }
	}

}
