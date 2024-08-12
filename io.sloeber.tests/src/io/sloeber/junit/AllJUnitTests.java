package io.sloeber.junit;

import org.junit.platform.suite.api.SelectClasses;

import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

/*
 * these junit tests need to be run as a junit plugin
 *
 *
 * TxtWorkAroundRegression.class is not included as it needs a special setup to run
 * The special setup is a arduinoplugin folder with "old" sloeber.txt files so the differences can be spotted
 */


@SuiteDisplayName("Sloeber Nightly suite")
@SelectClasses ({ TestPlatformWorkAround.class, TestSerialPlotterFilter.class, TestTxtFile.class, TestWorkAround.class,
    TestVersionCompare.class })
@Suite
public class AllJUnitTests {
//nothing needs to be done here
}
