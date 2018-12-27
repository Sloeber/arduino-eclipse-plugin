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
import io.sloeber.core.api.PackageManager;
import io.sloeber.core.api.Preferences;
import io.sloeber.providers.MCUBoard;
import io.sloeber.providers.Teensy;

@SuppressWarnings({ "nls" })
@RunWith(Parameterized.class)
public class CreateAndCompileArduinoIDEExamplesOnTeensyTest {
	private CodeDescriptor myCodeDescriptor;

	private String myTestName;
	private BoardDescriptor myBoardDescriptor;
	private static int myBuildCounter = 0;
	private static int myTotalFails = 0;
	private static int maxFails = 50;
	private static int mySkipAtStart = 0;

	public CreateAndCompileArduinoIDEExamplesOnTeensyTest(String testName, CodeDescriptor codeDescriptor,
			BoardDescriptor board) {

		myCodeDescriptor = codeDescriptor;
		myTestName = testName;
		myBoardDescriptor = board;
	}

	@SuppressWarnings("rawtypes")
	@Parameters(name = "{0}")
	public static Collection examples() {
		installAdditionalBoards();

		Shared.waitForAllJobsToFinish();
		Preferences.setUseBonjour(false);
		LinkedList<Object[]> examples = new LinkedList<>();
		MCUBoard[] allBoards = Teensy.getAllBoards();

		TreeMap<String, IPath> exampleFolders = LibraryManager.getAllArduinoIDEExamples();
		for (Map.Entry<String, IPath> curexample : exampleFolders.entrySet()) {
			String fqn = curexample.getKey().trim();
			IPath examplePath = curexample.getValue();
			Examples example = new Examples(fqn, examplePath);
			if (!skipExample(example)) {
				ArrayList<IPath> paths = new ArrayList<>();
				paths.add(examplePath);
				CodeDescriptor codeDescriptor = CodeDescriptor.createExample(false, paths);

				for (MCUBoard curBoard : allBoards) {
					if (curBoard.isExampleSupported(example)) {
						String projectName = Shared.getProjectName(codeDescriptor, example, curBoard);
						Map<String, String> boardOptions = curBoard.getBoardOptions(example);
						BoardDescriptor boardDescriptor = curBoard.getBoardDescriptor();
						boardDescriptor.setOptions(boardOptions);
						Object[] theData = new Object[] { projectName, codeDescriptor, boardDescriptor };
						examples.add(theData);
					}
				}
			}
		}

		return examples;

	}

	private static boolean skipExample(Examples example) {
		// no need to skip examples in this test
		return false;
	}

	public static void installAdditionalBoards() {
		if (MySystem.getTeensyPlatform().isEmpty()) {
			System.err.println("ERROR: Teensy not installed/configured skipping tests!!!");
		} else {
			PackageManager.addPrivateHardwarePath(MySystem.getTeensyPlatform());
		}

	}

	@Test
	public void testArduinoIDEExamplesOnTeensy() {
		Assume.assumeTrue("Skipping first " + mySkipAtStart + " tests", myBuildCounter++ >= mySkipAtStart);
		Assume.assumeTrue("To many fails. Stopping test", myTotalFails < maxFails);
		if (!Shared.BuildAndVerify(myTestName, myBoardDescriptor, myCodeDescriptor, new CompileOptions(null))) {
			myTotalFails++;
			fail(Shared.getLastFailMessage());
		}
	}

}
