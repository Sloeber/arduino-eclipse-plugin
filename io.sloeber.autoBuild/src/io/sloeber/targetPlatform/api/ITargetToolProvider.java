package io.sloeber.targetPlatform.api;

import java.util.Set;

import io.sloeber.targetPlatform.api.ITargetToolManager.ToolFlavour;

public interface ITargetToolProvider {

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
     * Get the TargetToolsID's this targetTool provider can provide
     * 
     * @return
     */
    Set<String> getTargetToolIDs();
    
    /**
     * Get the targetToo for the given ID
     * @param targetToolID
     * @return
     */
    ITargetTool getTargetTool(String targetToolID);

    ToolFlavour getToolFlavour();

    /**
     * typical implementation
     * this.getClass().getName()
     * 
     * @return
     */
    String getID();

    /**
     * Just give a targetTool that holdsallTools
     * if none exists return null
     * @return
     */
	ITargetTool getAnyInstalledTargetTool();
	
	Set<ITargetTool> getAllInstalledTargetTools();

}
