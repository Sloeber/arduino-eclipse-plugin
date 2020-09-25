package io.sloeber.core;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.sloeber.core.api.Preferences;

@RunWith(Suite.class)
// removed NightlyBoardPatronTest due to issue #1204
//this should be reenabled after #1204 is fixed
//@SuiteClasses({ NightlyBoardPatronTest.class, RegressionTest.class,
@SuiteClasses({  RegressionTest.class,
        RegressionTestFailingOnTravis.class })
public class NightlyJenkins {
    @BeforeClass
    public static void setUp() {
    	Preferences.setUseBonjour(false);
        Shared.deleteProjects=true;
    }
}
