package io.sloeber.junit;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/*
 * these junit tests need to be run as a junit plugin
 * 
 * 
 * TxtWorkAroundRegression.class is not included as it needs a special setup to run
 * The special setup is a arduinoplugin folder with "old" sloeber.txt files so the differences can be spotted
 */

@RunWith(Suite.class)
@SuiteClasses({ TestPlatformWorkAround.class, TestSerialPlotterFilter.class, TestTxtFile.class, TestWorkAround.class,
        TestVersionCompare.class })
public class AllJUnitTests {
    //no need for code here
}
