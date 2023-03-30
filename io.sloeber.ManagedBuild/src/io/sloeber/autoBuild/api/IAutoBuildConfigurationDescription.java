package io.sloeber.autoBuild.api;

import org.eclipse.core.resources.IFolder;

public interface IAutoBuildConfigurationDescription {

    boolean useDefaultBuildCommand();

    void setUseDefaultBuildCommand(boolean useDefaultBuildCommand);

    boolean generateMakeFilesAUtomatically();

    void setGenerateMakeFilesAUtomatically(boolean generateMakeFilesAUtomatically);

    IFolder getBuildFolder();

    void setBuildFolder(IFolder buildFolder);

    String getBuildCommand(boolean noArgs);

    boolean useStandardBuildArguments();

    void setUseStandardBuildArguments(boolean useStandardBuildArguments);

    boolean useCustomBuildArguments();

    void setUseCustomBuildArguments(boolean useCustomBuildArguments);

    boolean stopOnFirstBuildError();

    void setStopOnFirstBuildError(boolean stopOnFirstBuildError);

    boolean isParallelBuild();

    void setIsParallelBuild(boolean parallelBuild);

    int getParallelizationNum();

    void setParallelizationNum(int parallelizationNum);

    boolean isAutoBuildEnable();

    void setAutoBuildEnable(boolean b);

    boolean isCleanBuildEnabled();

    void setCleanBuildEnable(boolean cleanBuildEnabled);

    boolean isIncrementalBuildEnabled();

    void setIncrementalBuildEnable(boolean incrementalBuildEnabled);

    boolean isInternalBuilderEnabled();

    void enableInternalBuilder(boolean internalBuilderEnabled);

    boolean isManagedBuildOn();

    void setIsManagedBuildOn(boolean isManagedBuildOn);

    boolean supportsStopOnError(boolean b);

    boolean canKeepEnvironmentVariablesInBuildfile();

    boolean keepEnvironmentVariablesInBuildfile();

    boolean supportsParallelBuild();

    int getOptimalParallelJobNum();

    String getCustomBuildCommand();

    void setCustomBuildCommand(String makeArgs);
}
