package io.sloeber.junit;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ TestPlatformWorkAround.class, TestSerialPlotterFilter.class, TestTxtFile.class, TestWorkAround.class,
        TxtWorkAroundRegression.class, TestVersionCompare.class })
public class AllJUnitTests {

}
