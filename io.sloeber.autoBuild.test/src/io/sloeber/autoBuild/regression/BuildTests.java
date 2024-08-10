package io.sloeber.autoBuild.regression;

import org.junit.platform.suite.api.SelectClasses;

import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;


@SuiteDisplayName("AutoBuild Nightly suite")
@SelectClasses ({AutoBuildCreateBasicProjects.class,AutoBuildCreateProject.class,AutoBuildRegression.class})
@Suite
public class BuildTests {
//nothing needs to be done here
}
