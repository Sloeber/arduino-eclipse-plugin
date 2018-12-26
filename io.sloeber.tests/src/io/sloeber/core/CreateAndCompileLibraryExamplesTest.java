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
import io.sloeber.providers.Adafruit;
import io.sloeber.providers.Arduino;
import io.sloeber.providers.ESP32;
import io.sloeber.providers.ESP8266;
import io.sloeber.providers.MCUBoard;
import io.sloeber.providers.Teensy;

@SuppressWarnings({"nls"})
@RunWith(Parameterized.class)
public class CreateAndCompileLibraryExamplesTest {
	private static final boolean reinstall_boards_and_examples = false;
	private static final  int maxFails = 100;
	private static final  int mySkipAtStart = 0;
	
	private static int myBuildCounter = 0;
	private static int myTotalFails = 0;
	private Examples myExample;
	private MCUBoard myBoard;
	
	

	public CreateAndCompileLibraryExamplesTest(String name, MCUBoard boardID, Examples example) {
		myBoard = boardID;
		myExample = example;
	}

	@SuppressWarnings("rawtypes")
	@Parameters(name = "{index}: {0}")
	public static Collection examples() {
		WaitForInstallerToFinish();
		Preferences.setUseBonjour(false);
		Preferences.setUseArduinoToolSelection(true);
		MCUBoard myBoards[] = { Arduino.leonardo(), Arduino.uno(), Arduino.esplora(),
				Adafruit.feather(),Adafruit.featherMO(), Arduino.adafruitnCirquitPlayground(),
				ESP8266.nodeMCU(), ESP8266.wemosD1(), ESP8266.ESPressoLite(), Teensy.Teensy3_6(),
				Arduino.zeroProgrammingPort(), Arduino.cirquitPlaygroundExpress(),Arduino.gemma() ,
				Adafruit.trinket8MH(),Arduino.yun(),Arduino.arduino_101(),Arduino.zeroProgrammingPort(),
				Arduino.ethernet()};

		LinkedList<Object[]> examples = new LinkedList<>();
		TreeMap<String, IPath> exampleFolders = LibraryManager.getAllExamples(null);
		for (Map.Entry<String, IPath> curexample : exampleFolders.entrySet()) {
			String fqn = curexample.getKey().trim();
			IPath examplePath = curexample.getValue();
			Examples example = new Examples(fqn,  examplePath);

			// with the current amount of examples only do one
			MCUBoard curBoard = Examples.pickBestBoard(example, myBoards);

			if (curBoard != null) {
				Object[] theData = new Object[] { example.getLibName() + ":" + fqn + ":" + curBoard.getID(), curBoard,
						example };
				examples.add(theData);
			}

		}

		return examples;

	}

	/*
	 * In new new installations (of the Sloeber development environment) the
	 * installer job will trigger downloads These mmust have finished before we can
	 * start testing
	 */

	public static void WaitForInstallerToFinish() {

		installAdditionalBoards();
		Shared.waitForAllJobsToFinish();
	}

	public static void installAdditionalBoards() {
		String[] packageUrlsToAdd = { ESP8266.packageURL, Adafruit.packageURL ,ESP32.packageURL};
		PackageManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), reinstall_boards_and_examples);
		if (reinstall_boards_and_examples) {
			PackageManager.installAllLatestPlatforms();
			PackageManager.onlyKeepLatestPlatforms();
			// deal with removal of json files or libs from json files
			LibraryManager.removeAllLibs();
			LibraryManager.installAllLatestLibraries();
			// LibraryManager.onlyKeepLatestPlatforms();
		}
		if (MySystem.getTeensyPlatform().isEmpty()) {
			System.err.println("ERROR: Teensy not installed/configured skipping tests!!!");
		} else {
			PackageManager.addPrivateHardwarePath(MySystem.getTeensyPlatform());
		}
		PackageManager.installAllLatestPlatforms();

	}

	@Test
	public void testExamples() {

		Assume.assumeTrue("Skipping first " + mySkipAtStart + " tests", myBuildCounter++ >= mySkipAtStart);
		Assume.assumeTrue("To many fails. Stopping test", myTotalFails < maxFails);
		if (!myBoard.isExampleSupported(myExample)) {
			fail("Trying to run a test on unsoprted board");
			myTotalFails++;
			return;
		}
		ArrayList<IPath> paths = new ArrayList<>();

		paths.add(myExample.getPath());
		CodeDescriptor codeDescriptor = CodeDescriptor.createExample(false, paths);

		Map<String, String> boardOptions = myBoard.getBoardOptions(myExample);
		BoardDescriptor boardDescriptor = myBoard.getBoardDescriptor();
		boardDescriptor.setOptions(boardOptions);
        if (!Shared.BuildAndVerify( boardDescriptor, codeDescriptor)) {
            myTotalFails++;
            fail(Shared.getLastFailMessage() );
        }

	}

}
