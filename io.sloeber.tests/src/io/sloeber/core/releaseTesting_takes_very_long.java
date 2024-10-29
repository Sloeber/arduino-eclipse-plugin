package io.sloeber.core;



import org.junit.jupiter.api.BeforeAll;
import org.junit.platform.suite.api.SelectClasses;

import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

import io.sloeber.core.api.ConfigurationPreferences;


@SuiteDisplayName("Sloeber Nightly suite")
@SelectClasses ({
	BuildTests.class,
    //UpgradeTest.class, TODO JABA:Need to decide what to do sith these tests as 1 fails and they are kind of obsolete
    CompileAndUpload.class,
    CreateAndCompileArduinoIDEExamplesOnAVRHardwareTest.class,
    //CreateAndCompileArduinoIDEExamplesOnTeensyTest.class, no longer needed as teensy is now imported with the boardsmanager
    CreateAndCompileDefaultInoOnAllBoardsTest.class,
    CreateAndCompileExamplesTest.class,
    //CreateAndCompileArduinoIDEExamplesonJantjesBoardsTest.class, I should deprecate this
    CreateAndCompileLibraryExamplesTest.class,
    })
@Suite
public class releaseTesting_takes_very_long {
    @BeforeAll
    public static void setUp() {
    	ConfigurationPreferences.setUseBonjour(false);
        Shared.setDeleteProjects(true);
    }
}
