package io.sloeber.core;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import io.sloeber.core.api.Preferences;

@RunWith(Suite.class)
@SuiteClasses({ NightlyBoardPatronTest.class, RegressionTest.class,
        RegressionTestFailingOnTravis.class })
public class NightlyJenkins {
    @BeforeClass
    public static void setUp() {
    	Preferences.setUseBonjour(false);
        Shared.deleteProjects=true;
    }
}
