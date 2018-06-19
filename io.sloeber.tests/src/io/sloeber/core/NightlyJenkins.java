package io.sloeber.core;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ NightlyBoardPatronTest.class, RegressionTest.class,
        RegressionTestFailingOnTravis.class })
public class NightlyJenkins {
    @BeforeClass
    public static void setUp() {
        Shared.deleteProjects=true;
    }
}
