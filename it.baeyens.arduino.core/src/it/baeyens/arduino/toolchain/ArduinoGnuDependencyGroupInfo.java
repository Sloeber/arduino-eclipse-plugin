package it.baeyens.arduino.toolchain;

/**
 * 
 * This class contains the description of a group of generated dependency files, e.g., .d files created by compilations
 * 
 */

public class ArduinoGnuDependencyGroupInfo {

    // Member Variables
    String groupBuildVar;
    boolean conditionallyInclude;

    // ArrayList groupFiles;

    // Constructor
    public ArduinoGnuDependencyGroupInfo(String groupName, boolean bConditionallyInclude) {
	this.groupBuildVar = groupName;
	this.conditionallyInclude = bConditionallyInclude;
	// Note: not yet needed
	// groupFiles = null;
    }

}
