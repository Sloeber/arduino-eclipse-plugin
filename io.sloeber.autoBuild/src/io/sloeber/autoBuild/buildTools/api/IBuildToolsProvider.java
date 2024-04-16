package io.sloeber.autoBuild.buildTools.api;

import java.util.Set;

import io.sloeber.autoBuild.schema.api.IProjectType;

public interface IBuildToolsProvider {

    /**
     * did this target tool provider find fully functional buildTools on the local disk
     * This method does not tell you how many buildTools were found
     * It only tells you there is at least one location on disk that contains a buildTools with all
     * the tools
     * expected for the toolflavour.
     *
     *
     * @return true if this buildToolsProvider can provide at least 1 buildTools that returns true for holdsAllTools
     *         else false
     */
    boolean holdsAllTools();

    /**
     * Get the targetToo for the given ID
     * @param buildToolsID
     * @return
     */
    IBuildTools getBuildTools(String buildToolsID);

    /**
     * typical implementation
     * this.getClass().getName()
     *
     * @return
     */
    String getID();

    String getName();

    /**
     * Just give a buildTools that holdsallTools
     * if none exists return null
     * @param projectType
     * @return
     */
	IBuildTools getAnyInstalledBuildTools();

	Set<IBuildTools> getAllInstalledBuildTools();

	public void refreshToolchains();

	boolean supports(IProjectType projectType);

	String getDescription();

	boolean isTest();

}
