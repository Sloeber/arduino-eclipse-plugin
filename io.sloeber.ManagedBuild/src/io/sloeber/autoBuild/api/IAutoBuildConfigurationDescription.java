package io.sloeber.autoBuild.api;

import java.util.Map;
import java.util.Set;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

import io.sloeber.schema.api.ITool;

public interface IAutoBuildConfigurationDescription {

    public boolean useDefaultBuildCommand();

    public void setUseDefaultBuildCommand(boolean useDefaultBuildCommand);

    public boolean generateMakeFilesAUtomatically();

    public void setGenerateMakeFilesAUtomatically(boolean generateMakeFilesAUtomatically);

    public String getBuildCommand(boolean noArgs);

    public boolean useStandardBuildArguments();

    public void setUseStandardBuildArguments(boolean useStandardBuildArguments);

    public boolean stopOnFirstBuildError();

    public void setStopOnFirstBuildError(boolean stopOnFirstBuildError);

    public boolean isParallelBuild();

    public void setIsParallelBuild(boolean parallelBuild);

    public int getParallelizationNum();

    public void setParallelizationNum(int parallelizationNum);

    public boolean isCleanBuildEnabled();

    public void setCleanBuildEnable(boolean cleanBuildEnabled);

    public boolean isIncrementalBuildEnabled();

    public void setIncrementalBuildEnable(boolean incrementalBuildEnabled);

    public int getOptimalParallelJobNum();

    public String getCustomBuildCommand();

    public void setCustomBuildCommand(String makeArgs);

    /**
     * Set a buildfolder to be the same as a string.
     * The reason this is not set as a iFolder is that the string can be a variable
     * that need to be resolved
     * For instance the default build folder is ${ProjDir}/${ConfigName}
     * Though this mùay be settable in a IFolder the GUI works with a text field so
     * I opted to
     * make the interface work with text fields
     * 
     * @param buildFolder
     */
    public void setBuildFolderString(String buildFolder);

    /**
     * See setBuildFolderString for more info
     * This method is solely intended to be used by the gui
     * 
     * @return the build folder in a editable string representation
     */
    public String getBuildFolderString();

    /**
     * Get build folder to do build actions
     * 
     * @return the build folder in IFolder format based on the resolved buildfolder
     *         string representation
     */
    public IFolder getBuildFolder();

    public IBuildRunner getBuildRunner();

    public Set<IBuildRunner> getBuildRunners();

    public void setBuildRunner(IBuildRunner buildRunner);

    /**
     * Is the eclipse autobuild functionality
     * (the build on safe) enabled for this CDT project configuration
     * For autobuild to happen both the eclipse workspace has to have autobuild on
     * and the cdt configuration.
     * 
     * @return true if the autobuild will build when eclipse workspace has autobuild
     *         on
     */
    public boolean isAutoBuildEnabled();

    /**
     * see isAutoBuildEnabled
     * 
     * @param enabled
     */
    public void setAutoBuildEnabled(boolean enabled);

    public ICConfigurationDescription getCdtConfigurationDescription();

    public void setCustomBuildArguments(String arguments);

    public String getCustomBuildArguments();

    public void setAutoMakeTarget(String target);

    public String getAutoMakeTarget();

    public void setIncrementalMakeTarget(String target);

    public String getIncrementalMakeTarget();

    public void setCleanMakeTarget(String target);

    public String getCleanMakeTarget();

    /**
     * Get the command to run at the start of the build
     * No environment var or build var extension is done
     * 
     * @return The actual command to run as provided by setPrebuildStep
     */
    public String getPrebuildStep();

    /**
     * Set the command to run at the start of the build
     */
    public void setPrebuildStep(String text);

    public String getPreBuildAnouncement();

    public void setPreBuildAnouncement(String AnoounceMent);

    public String getPostbuildStep();

    public void setPostbuildStep(String text);

    public String getPostBuildAnouncement();

    public void setPostBuildAnouncement(String text);

    public Map<String, String> getSelectedOptions(IResource resource);

    public String getProperty(String propertyName);

    /**
     * Sets or removes a custom command for a tool for a specific resource
     * Note that there is no get custom command because getCommand will return
     * the custom command if appropriate
     * Use null as custom command to remove the custom command.
     * use null as resource to remove all custom commands for this tool
     * 
     * @param tool
     *            the tool the custom command is provided for
     * @param resource
     *            the resource the custom command is to be used for
     * @param customCommand
     *            the custom command that replaces the original command or null to
     *            replace the current customisation
     */
    public void setCustomToolCommand(ITool tool, IResource resource, String customCommand);

    public String getToolCommand(ITool tool, IResource resource);

    public void setCustomToolPattern(ITool tool, IResource resource, String pattern);

    String getToolPattern(ITool tool, IResource resource);
}
