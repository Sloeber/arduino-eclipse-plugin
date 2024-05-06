package io.sloeber.core;

import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.eclipse.core.runtime.IPath;
import org.junit.Assume;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.CodeDescription;
import io.sloeber.core.api.CompileDescription;
import io.sloeber.core.api.IExample;
import io.sloeber.core.api.LibraryManager;
import io.sloeber.core.api.Preferences;
import io.sloeber.providers.Arduino;
import io.sloeber.providers.MCUBoard;

@SuppressWarnings({ "nls" })
public class NightlyBoardPatronTest {

    private static int mySkipAtStart = 0;
    private  int myTotalFails = 0;
    private static int maxFails = 40;
    private static CompileDescription myCompileOptions;
    private static boolean deleteProjects =true; //delete the projects after trying to build them


    public static Stream<Arguments>  NightlyBoardPatronTestData() {
    	Shared.setDeleteProjects(deleteProjects );
        Preferences.setUseBonjour(false);

        Shared.waitForAllJobsToFinish();
        Arduino.installLatestSamDBoards();
        LibraryManager.installLibrary("RTCZero");
        Shared.waitForAllJobsToFinish();
        Preferences.setUseArduinoToolSelection(true);
        myCompileOptions = new CompileDescription();
        MCUBoard zeroBoard = Arduino.zeroProgrammingPort();

        List<Arguments> ret = new LinkedList<>();
        TreeMap<String, IExample> exampleFolders = LibraryManager.getExamplesLibrary(null);
        for (Map.Entry<String, IExample> curexample : exampleFolders.entrySet()) {
            String fqn = curexample.getKey().trim();
            IPath examplePath = curexample.getValue().getCodeLocation();
            //for patron Keith Willis. Thanks Keith
            if (fqn.contains("RTCZero")) {
                Example example = new Example(fqn,  examplePath);

                ret.add(Arguments.of(Shared.getCounterName( example.getLibName() + ":" + fqn + ":" + zeroBoard.getID()),
                        zeroBoard, example ));
            }
        }
		return ret.stream();
    }



	@ParameterizedTest
	@MethodSource("NightlyBoardPatronTestData")
    public void testExamples(@SuppressWarnings("unused") String name, MCUBoard boardID, Example example) {
    	Shared.buildCounter++ ;
        Assume.assumeTrue("Skipping first " + mySkipAtStart + " tests", Shared.buildCounter >= mySkipAtStart);
        Assume.assumeTrue("To many fails. Stopping test", myTotalFails < maxFails);

        Set<IExample> examples = new HashSet<>();

        examples.add(example);
        CodeDescription codeDescriptor = CodeDescription.createExample(false, examples);

        Map<String, String> boardOptions = boardID.getBoardOptions(example);
        BoardDescription boardDescriptor = boardID.getBoardDescriptor();
        boardDescriptor.setOptions(boardOptions);
        if(!Shared.BuildAndVerify(boardID.getBoardDescriptor(), codeDescriptor, myCompileOptions,Shared.buildCounter)) {
            myTotalFails++;
            fail(Shared.getLastFailMessage() );
        }

    }


}
