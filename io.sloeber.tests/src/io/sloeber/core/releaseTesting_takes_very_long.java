package io.sloeber.core;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.sloeber.core.api.SerialManager;

@RunWith(Suite.class)
@SuiteClasses({ 
        CreateAndCompileDefaultInoOnAllBoardsTest.class,
        NightlyBoardPatronTest.class, 
        RegressionTest.class,
        RegressionTestFailingOnTravis.class,
        CreateAndCompileArduinoIDEExamplesOnAVRHardwareTest.class,
        CreateAndCompileArduinoIDEExamplesOnTeensyTest.class,
        CreateAndCompileExamplesTest.class,
        CreateAndCompileJantjesBoardsTest.class,
        CreateAndCompileLibraryExamplesTest.class
        })
public class releaseTesting_takes_very_long {
    @BeforeClass
    public static void setUp() {
        SerialManager.stopNetworkScanning();
        Shared.deleteProjects=true;
    }
}
