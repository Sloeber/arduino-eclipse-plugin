package io.sloeber.core;

import org.junit.platform.suite.api.SelectClasses;

import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;


@SuiteDisplayName("Sloeber Nightly suite")
@SelectClasses ({RegressionTest.class})
@Suite
public class BuildTests {
//nothing needs to be done here
}
