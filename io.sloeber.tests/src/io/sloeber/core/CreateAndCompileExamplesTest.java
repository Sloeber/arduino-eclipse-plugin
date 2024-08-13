package io.sloeber.core;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.CodeDescription;
import io.sloeber.core.api.CompileDescription;
import io.sloeber.core.api.IExample;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.Preferences;
import io.sloeber.providers.Adafruit;
import io.sloeber.providers.Arduino;
import io.sloeber.providers.ESP8266;
import io.sloeber.providers.MCUBoard;

@SuppressWarnings("nls")
public class CreateAndCompileExamplesTest {
	private static final boolean reinstall_boards_and_examples = false;
    private  int myTotalFails = 0;
    private static int maxFails = 200;
    private static int mySkipAtStart = 0;


    public static Stream<Arguments> examples() throws Exception {
		installAdditionalBoards();
		Shared.waitForAllJobsToFinish();
		Preferences.setUseBonjour(false);

		MCUBoard myBoards[] = { Arduino.leonardo(),
				Arduino.uno(),
				Arduino.esplora(),
				Adafruit.feather(),
				Arduino.adafruitnCirquitPlayground(),
				ESP8266.nodeMCU(),
				Arduino.primo(),
				Arduino.mega2560Board(),
				Arduino.gemma(),
				Arduino.zeroProgrammingPort(),
				Arduino.mkrfox1200(),
				Arduino.due() };

		List<Arguments> ret = new LinkedList<>();
		TreeMap<String, IExample> exampleFolders = LibraryManager.getExamplesLibrary(null);
		for (Map.Entry<String, IExample> curexample : exampleFolders.entrySet()) {
			IExample example=curexample.getValue();

			Set<IExample> tmpExamples = new HashSet<>();
			tmpExamples.add(example);
			CodeDescription codeDescriptor = CodeDescription.createExample(false, tmpExamples);

			String fqn=curexample.getKey();
			Example newExample=new Example(fqn,example.getCodeLocation());
            // with the current amount of examples only do one
            MCUBoard board = Example.pickBestBoard(newExample, myBoards);
            if (board != null) {
                BoardDescription curBoard = board.getBoardDescriptor();
                if (curBoard != null) {
                	ret.add(Arguments.of(Shared.getCounterName(fqn.trim()), curBoard, codeDescriptor));
                }
            }
		}
		return ret.stream();

	}


	public static void installAdditionalBoards() {
		String[] packageUrlsToAdd = { ESP8266.packageURL, Adafruit.packageURL };
		BoardsManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), true);
		if (reinstall_boards_and_examples) {
			BoardsManager.installAllLatestPlatforms();
			BoardsManager.onlyKeepLatestPlatforms();
			// deal with removal of json files or libs from json files
			LibraryManager.unInstallAllLibs();
			LibraryManager.installAllLatestLibraries();
		}

	}

	@ParameterizedTest
	@MethodSource("examples")
	public void testExamples(String name, BoardDescription boardDescriptor, CodeDescription codeDescriptor) throws Exception {
        // Stop after X fails because
        // the fails stays open in eclipse and it becomes really slow
        // There are only a number of issues you can handle
        // best is to focus on the first ones and then rerun starting with the
        // failures
        assumeTrue( Shared.buildCounter++ >= mySkipAtStart,"Skipping first " + mySkipAtStart + " tests");
        assumeTrue( myTotalFails < maxFails,"To many fails. Stopping test");

        Shared.buildCounter++;
        myTotalFails++;
        assertNull(Shared.buildAndVerify(name, boardDescriptor, codeDescriptor, new CompileDescription()));
        myTotalFails--;

	}

}
