package io.sloeber.core;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.sloeber.core.api.Preferences;

@RunWith(Suite.class)
@SuiteClasses({
        RegressionTest.class,
        //UpgradeTest.class, TODO JABA:Need to decide what to do sith these tests as 1 fails and they are kind of obsolete
        CompileAndUpload.class,
        CreateAndCompileArduinoIDEExamplesOnAVRHardwareTest.class,
        //CreateAndCompileArduinoIDEExamplesOnTeensyTest.class, no longer needed as teensy is now imported with the boardsmanager
        CreateAndCompileDefaultInoOnAllBoardsTest.class,
        CreateAndCompileExamplesTest.class,
        //CreateAndCompileArduinoIDEExamplesonJantjesBoardsTest.class, I should deprecate this
        CreateAndCompileLibraryExamplesTest.class,
        })
public class releaseTesting_takes_very_long {
    @BeforeClass
    public static void setUp() {
    	Preferences.setUseBonjour(false);
        Shared.setDeleteProjects(true);
    }
}
