package io.sloeber.core;

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
import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.api.PackageManager;
import io.sloeber.providers.MCUBoard;
import io.sloeber.providers.Teensy;

@SuppressWarnings({"nls"})
@RunWith(Parameterized.class)
public class CreateAndCompileArduinoIDEExamplesOnTeensyTest {
	private CodeDescriptor myCodeDescriptor;
	private Examples myExample;
    private static int myBuildCounter = 0;
    private static int myTotalFails = 0;
    private static int maxFails = 50;
    private static int mySkipAtStart = 0;

	public CreateAndCompileArduinoIDEExamplesOnTeensyTest(CodeDescriptor codeDescriptor, Examples example) {

		myCodeDescriptor = codeDescriptor;
		myExample = example;
	}

	@SuppressWarnings("rawtypes")
	@Parameters(name = "{index}: {0}")
	public static Collection examples() {
		installAdditionalBoards();

		Shared.waitForAllJobsToFinish();
		LinkedList<Object[]> examples = new LinkedList<>();

		TreeMap<String, IPath> exampleFolders = LibraryManager.getAllArduinoIDEExamples();
		for (Map.Entry<String, IPath> curexample : exampleFolders.entrySet()) {
			String fqn = curexample.getKey().trim();
			IPath examplePath = curexample.getValue();
			Examples example = new Examples(fqn, null, examplePath);
			if (!skipExample(example)) {
				ArrayList<IPath> paths = new ArrayList<>();
				paths.add(examplePath);
				CodeDescriptor codeDescriptor = CodeDescriptor.createExample(false, paths);

				Object[] theData = new Object[] {  codeDescriptor, example };
				examples.add(theData);
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

	public void testExample(MCUBoard board) {
		// Stop after X fails because
		// the fails stays open in eclipse and it becomes really slow
		// There are only a number of issues you can handle
		// best is to focus on the first ones and then rerun starting with the
		// failures
        Assume.assumeTrue("Skipping first " + mySkipAtStart + " tests", myBuildCounter++ >= mySkipAtStart);
        Assume.assumeTrue("To many fails. Stopping test", myTotalFails < maxFails);
        Map<String,String> boardOptions=board.getBoardOptions(myExample);
        BoardDescriptor boardDescriptor=board.getBoardDescriptor();
        boardDescriptor.setOptions(boardOptions);
        if (!Shared.BuildAndVerify(myBuildCounter, boardDescriptor, myCodeDescriptor, null)) {
            myTotalFails++;
        }
	}

	@Test
	public void testArduinoIDEExamplesOnTeensy3_6() {
		if (!MySystem.getTeensyPlatform().isEmpty())
			testExample(Teensy.Teensy3_6());
	}

	@Test
	public void testArduinoIDEExamplesOnTeensy3_5() {
		if (!MySystem.getTeensyPlatform().isEmpty())
			testExample(Teensy.Teensy3_5());
	}

	@Test
	public void testArduinoIDEExamplesOnTeensy3_1() {
		if (!MySystem.getTeensyPlatform().isEmpty())
			testExample(Teensy.Teensy3_1());
	}

	@Test
	public void testArduinoIDEExamplesOnTeensy3_0() {
		if (!MySystem.getTeensyPlatform().isEmpty())
			testExample(Teensy.Teensy3_0());
	}

	@Test
	public void testArduinoIDEExamplesOnTeensyLC() {
		if (!MySystem.getTeensyPlatform().isEmpty())
			testExample(Teensy.Teensy_LC());
	}

	@Test
	public void testArduinoIDEExamplesOnTeensyPP2() {
		if (!MySystem.getTeensyPlatform().isEmpty())
			testExample(Teensy.teensypp2());
	}

	@Test
	public void testArduinoIDEExamplesOnTeensy2() {
		if (!MySystem.getTeensyPlatform().isEmpty())
			testExample(Teensy.teensy2());

	}

}
