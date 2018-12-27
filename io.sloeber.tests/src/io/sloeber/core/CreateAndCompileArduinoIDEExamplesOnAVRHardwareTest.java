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
import io.sloeber.core.api.Preferences;
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

	public CreateAndCompileArduinoIDEExamplesOnAVRHardwareTest(String projectName, CodeDescriptor codeDescriptor,
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
		MCUBoard[] allBoards = Arduino.getAllBoards();

		TreeMap<String, IPath> exampleFolders = LibraryManager.getAllArduinoIDEExamples();
		for (Map.Entry<String, IPath> curexample : exampleFolders.entrySet()) {
			String fqn = curexample.getKey().trim();
			IPath examplePath = curexample.getValue();
			Examples example = new Examples(fqn, examplePath);
			if (!skipExample(example)) {
				ArrayList<IPath> paths = new ArrayList<>();

				paths.add(examplePath);
				CodeDescriptor codeDescriptor = CodeDescriptor.createExample(false, paths);
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

	private static boolean skipExample(Examples example) {
		// skip Teensy stuff on Arduino hardware
		// Teensy is so mutch more advanced that most arduino avr hardware can not
		// handle it
		return example.getPath().toString().contains("Teensy");
	}

	@Test
	public void testExample() {

		Assume.assumeTrue("Skipping first " + mySkipAtStart + " tests", myBuildCounter++ >= mySkipAtStart);
		Assume.assumeTrue("To many fails. Stopping test", myTotalFails < maxFails);

		if (!Shared.BuildAndVerify(myProjectName, myBoard.getBoardDescriptor(), myCodeDescriptor,
				new CompileOptions(null))) {
			myTotalFails++;
			fail(Shared.getLastFailMessage());
		}
	}

}
