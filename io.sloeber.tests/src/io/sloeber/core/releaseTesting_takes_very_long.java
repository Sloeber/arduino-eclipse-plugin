package io.sloeber.core;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.sloeber.core.api.Preferences;

@RunWith(Suite.class)
@SuiteClasses({

        NightlyBoardPatronTest.class,
        RegressionTest.class,
        RegressionTestFailingOnTravis.class,
        UpgradeTest.class, CompileAndUpload.class,
        CreateAndCompileArduinoIDEExamplesOnAVRHardwareTest.class,
        CreateAndCompileArduinoIDEExamplesOnTeensyTest.class,
        CreateAndCompileDefaultInoOnAllBoardsTest.class,
        CreateAndCompileExamplesTest.class,
        CreateAndCompileArduinoIDEExamplesonJantjesBoardsTest.class,
        CreateAndCompileLibraryExamplesTest.class,
        })
public class releaseTesting_takes_very_long {
    @BeforeClass
    public static void setUp() {
    	Preferences.setUseBonjour(false);
        Shared.setDeleteProjects(true);
    }
}
