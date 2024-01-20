package io.sloeber.buildTool.api;

import java.util.Set;

public interface IBuildToolProvider {

    /**
     * did this target tool provider find fully functional targetTool on the local disk
     * This method does not tell you how many targetTools were found
     * It only tells you there is at least one location on disk that contains a targetTool with all
     * the tools
     * expected for the toolflavour.
     * 
     * 
     * @return true if this targetToolProvider can provide at least 1 TargetTool that returns true for holdsAllTools
     *         else false
     */
    boolean holdsAllTools();
    
    /**
     * Get the targetToo for the given ID
     * @param targetToolID
     * @return
     */
    IBuildTools getTargetTool(String targetToolID);

    /**
     * typical implementation
     * this.getClass().getName()
     * 
     * @return
     */
    String getID();
    
    String getName();

    /**
     * Just give a targetTool that holdsallTools
     * if none exists return null
     * @return
     */
	IBuildTools getAnyInstalledTargetTool();
	
	Set<IBuildTools> getAllInstalledBuildTools();
	
	public void refreshToolchains();

}
