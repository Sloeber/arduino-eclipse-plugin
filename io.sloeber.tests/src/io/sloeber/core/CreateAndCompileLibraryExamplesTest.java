package io.sloeber.core;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IPath;
import org.junit.Assume;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.CodeDescription;
import io.sloeber.core.api.IExample;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.Preferences;
import io.sloeber.providers.Adafruit;
import io.sloeber.providers.Arduino;
import io.sloeber.providers.ESP32;
import io.sloeber.providers.ESP8266;
import io.sloeber.providers.MCUBoard;
import io.sloeber.providers.Teensy;

@SuppressWarnings({ "nls" })
public class CreateAndCompileLibraryExamplesTest {
    private static final boolean reinstall_boards_and_examples = false;
    private static final int maxFails = 100;
    private static final int mySkipAtStart = 0;

    private int myTotalFails = 0;

    public static Stream<Arguments> CreateAndCompileLibraryExamplesTestData() {
        Preferences.setUseBonjour(false);
        Preferences.setUseArduinoToolSelection(true);
        Shared.waitForAllJobsToFinish();
        installMyStuff();

        MCUBoard myBoards[] = { Arduino.uno(), Arduino.leonardo(), Arduino.esplora(), Adafruit.feather(),
                Adafruit.featherMO(), Arduino.adafruitnCirquitPlayground(), ESP8266.nodeMCU(), ESP8266.wemosD1(),
                ESP8266.ESPressoLite(), Teensy.Teensy3_6(), Arduino.zeroProgrammingPort(),
                Arduino.cirquitPlaygroundExpress(), Arduino.gemma(), Adafruit.trinket8MH(), Arduino.yun(),
                Arduino.arduino_101(), Arduino.zeroProgrammingPort(), Arduino.ethernet() };

        List<Arguments> ret = new LinkedList<>();
        Map<String, IExample> exampleFolders = LibraryManager.getExamplesAll(null);
        for (Map.Entry<String, IExample> curexample : exampleFolders.entrySet()) {
            String fqn = curexample.getKey().trim();
            IPath examplePath = curexample.getValue().getCodeLocation();
            Example example = new Example(fqn, examplePath);

            // with the current amount of examples only do one
            MCUBoard curBoard = Example.pickBestBoard(example, myBoards);

            if (curBoard != null) {
            	ret.add(Arguments.of( example.getLibName() + ":" + fqn + ":" + curBoard.getID(), curBoard,
                        example ));
            }
        }
		return ret.stream();
    }

    /*
     * In new new installations (of the Sloeber development environment) the
     * installer job will trigger downloads These mmust have finished before we can
     * start testing
     */

    public static void installMyStuff() {

        installAdditionalBoards();
        Shared.waitForAllJobsToFinish();
    }

    public static void installAdditionalBoards() {
        String[] packageUrlsToAdd = { ESP8266.packageURL, Adafruit.packageURL, ESP32.packageURL };
        BoardsManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), reinstall_boards_and_examples);
        if (reinstall_boards_and_examples) {
            BoardsManager.installAllLatestPlatforms();
            BoardsManager.onlyKeepLatestPlatforms();
            // deal with removal of json files or libs from json files
            LibraryManager.unInstallAllLibs();
            LibraryManager.installAllLatestLibraries();
            // LibraryManager.onlyKeepLatestPlatforms();
        }
        BoardsManager.installAllLatestPlatforms();

    }

	@ParameterizedTest
	@MethodSource("CreateAndCompileLibraryExamplesTestData")
    public void testExamples(String name, MCUBoard boardID, Example example) throws Exception {

        Assume.assumeTrue("Skipping first " + mySkipAtStart + " tests", Shared.buildCounter++ >= mySkipAtStart);
        Assume.assumeTrue("To many fails. Stopping test", myTotalFails < maxFails);
        if (! example.worksOnBoard(boardID)) {
            fail("Trying to run a test on unsoprted board");
            myTotalFails++;
            return;
        }
        Set<IExample> paths = new HashSet<>();

        paths.add(example);
        CodeDescription codeDescriptor = CodeDescription.createExample(false, paths);

        Map<String, String> boardOptions = boardID.getBoardOptions(example);
        BoardDescription boardDescriptor = boardID.getBoardDescriptor();
        boardDescriptor.setOptions(boardOptions);
        String error=Shared.buildAndVerify(boardDescriptor, codeDescriptor);
        if (error!=null) {
            myTotalFails++;
            fail(error);
        }

    }

}
