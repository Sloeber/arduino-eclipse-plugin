package io.sloeber.targetPlatform.api;

import java.nio.file.Path;
import java.util.Map;

import org.eclipse.core.runtime.IPath;

import io.sloeber.targetPlatform.api.ITargetToolManager.ToolFlavour;
import io.sloeber.targetPlatform.api.ITargetToolManager.ToolType;

/**
 * This is a set of tools on the local disk to build a target
 * 
 */
public interface ITargetTool {

    /**
     * This tool provider find tools on the local disk
     * This method does not tell you how many locations of tools were found
     * It only tells you there is at least one location on disk that contains all
     * the tools
     * expected for the toolflavour.
     * For instance MVC needs a resource compiler tooltype where a ll other tools do
     * not need this
     * So if the tool type is GNU and not resource compiler is found but the others
     * are this method
     * should return true where MVC toolFlavour should return false
     * 
     * 
     * @return true if tools were found
     *         false if no tools were found
     */
    boolean holdsAllTools();

    /**
     * Get rovider may provide different versions of tools.
     * This can bthe ID that identifies the selection made in the tool provider
     * A tool pe V1.0 and V2.3 but also completely different tool set (like in
     * embedded world)
     * By providing the selectionID This selection can be made persistent.
     * 
     * @return a string that allows the tool provider to know what to return in its
     *         other methods;
     */
    String getSelectionID();


    /**
     * Some tools may require variables to be added to the environment before
     * starting the tools
     * For each of these variables the builder will run a set key=value command
     * If the tool needs to be on the path you can have {path;myPath;%path%} on windows
     * 
     * @return a set of variables or null
     */
    Map<String, String> getEnvironmentVariables();

    /**
     * Some tools may require variables to extend the tool commands
     * For example you may opt to add something to the command line by using the
     * FLAGS variable.
     * 
     * These variables will be added (using a space as delimiter) to the end of
     * existing variables with the same name
     * 
     * @return
     */
    Map<String, String> getToolVariables();

    /**
     * Gets the command (no path no extra's) that will be used to execute the given
     * toolType
     * The command should not contain spaces and should not contain variabnles
     * If the toolType is not supported null should be returned
     * The return value is visible in the ui.
     * 
     * @param toolType
     *            the tooltype for which the command is requested
     * @return the actual command to execute or null if this toolType is not
     *         supported.
     */
    String getCommand(ToolType toolType);

    /**
     * The location on disk where the provided tool of tooltype can be found and
     * executed.
     * If tooltype is not supported the return value is undefined.
     * 
     * @param toolType
     *            the tooltype for which the location is requested
     * @return a valid location of the tooltype when the tooltype is supported.
     *         null if the tool is on the path
     *         undefined when the tooltype is not supported.
     */
    IPath getToolLocation(ToolType toolType);

    ToolFlavour getToolFlavour();

	String getProviderID();

}
