package io.sloeber.autoBuild.api;

import java.util.Set;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IFolder;

public interface IAutoBuildConfigurationDescription {

    boolean useDefaultBuildCommand();

    void setUseDefaultBuildCommand(boolean useDefaultBuildCommand);

    boolean generateMakeFilesAUtomatically();

    void setGenerateMakeFilesAUtomatically(boolean generateMakeFilesAUtomatically);

    String getBuildCommand(boolean noArgs);

    boolean useStandardBuildArguments();

    void setUseStandardBuildArguments(boolean useStandardBuildArguments);

    boolean stopOnFirstBuildError();

    void setStopOnFirstBuildError(boolean stopOnFirstBuildError);

    boolean isParallelBuild();

    void setIsParallelBuild(boolean parallelBuild);

    int getParallelizationNum();

    void setParallelizationNum(int parallelizationNum);

    boolean isCleanBuildEnabled();

    void setCleanBuildEnable(boolean cleanBuildEnabled);

    boolean isIncrementalBuildEnabled();

    void setIncrementalBuildEnable(boolean incrementalBuildEnabled);

    int getOptimalParallelJobNum();

    String getCustomBuildCommand();

    void setCustomBuildCommand(String makeArgs);

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
    void setBuildFolderString(String buildFolder);

    /**
     * See setBuildFolderString for more info
     * This method is solely intended to be used by the gui
     * 
     * @return the build folder in a editable string representation
     */
    String getBuildFolderString();

    /**
     * Get build folder to do build actions
     * 
     * @return the build folder in IFolder format based on the resolved buildfolder
     *         string representation
     */
    IFolder getBuildFolder();

    IBuildRunner getBuildRunner();

    Set<IBuildRunner> getBuildRunners();

    void setBuildRunner(IBuildRunner buildRunner);

    /**
     * Is the eclipse autobuild functionality
     * (the build on safe) enabled for this CDT project configuration
     * For autobuild to happen both the eclipse workspace has to have autobuild on
     * and the cdt configuration.
     * 
     * @return true if the autobuild will build when eclipse workspace has autobuild
     *         on
     */
    boolean isAutoBuildEnabled();

    /**
     * see isAutoBuildEnabled
     * 
     * @param enabled
     */
    void setAutoBuildEnabled(boolean enabled);

    ICConfigurationDescription getCdtConfigurationDescription();

    void setCustomBuildArguments(String arguments);

    String getCustomBuildArguments();

    void setAutoMakeTarget(String target);

    String getAutoMakeTarget();

    void setIncrementalMakeTarget(String target);

    String getIncrementalMakeTarget();

    void setCleanMakeTarget(String target);

    String getCleanMakeTarget();
}
