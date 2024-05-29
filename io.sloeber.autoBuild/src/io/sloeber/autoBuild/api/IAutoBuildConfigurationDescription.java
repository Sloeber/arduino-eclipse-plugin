package io.sloeber.autoBuild.api;

import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.CONFIG_NAME_VARIABLE;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSourceEntry;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

import io.sloeber.autoBuild.buildTools.api.IBuildTools;
import io.sloeber.autoBuild.buildTools.api.IBuildToolsManager.ToolFlavour;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.autoBuild.schema.api.IBuilder;
import io.sloeber.autoBuild.schema.api.IConfiguration;
import io.sloeber.autoBuild.schema.api.IOption;
import io.sloeber.autoBuild.schema.api.IProjectType;
import io.sloeber.autoBuild.schema.api.ITool;

public interface IAutoBuildConfigurationDescription {

    public static IAutoBuildConfigurationDescription getActiveConfig(IProject project, boolean write) {
        CoreModel coreModel = CoreModel.getDefault();
        ICProjectDescription projectDescription = coreModel.getProjectDescription(project, write);
        return getActiveConfig(projectDescription);
    }

    public static IAutoBuildConfigurationDescription getActiveConfig(ICProjectDescription projectDescription) {
    	if(projectDescription==null) {
    		return null;
    	}
        return getConfig(projectDescription.getActiveConfiguration());
    }

    public static IAutoBuildConfigurationDescription getConfig(ICConfigurationDescription confDesc) {
        if (confDesc == null)
            return null;
        if(!(confDesc.getConfigurationData() instanceof AutoBuildConfigurationDescription)) {
        	return null;
        }
        AutoBuildConfigurationDescription ret = (AutoBuildConfigurationDescription) confDesc.getConfigurationData();
        ret.setWritable(true);
        return ret;
        //      Note:
        //      The code above always returns a readable configdesc
        //        eventhough the method below exists it is not defined in ICConfigurationDescription
        //        and as sutch not usable
        //        boolean writable =!confDesc.isReadOnly();
        //        return (AutoBuildConfigurationDescription) confDesc.getConfigurationData(writable);

    }

	/**
	 * Get the source entries and
	 * resolve the ${ConfigName} in the name
	 * The filters are not changed
	 *
	 * @param autoBuildConfData
	 * @return
	 */
	public static ICSourceEntry[] getResolvedSourceEntries(IAutoBuildConfigurationDescription cfgDesc) {
		AutoBuildConfigurationDescription autoBuildConfData=(AutoBuildConfigurationDescription) cfgDesc;
		String configName = autoBuildConfData.getName();
		List<ICSourceEntry> ret = new ArrayList<>();
		for (ICSourceEntry curSourceEntry : autoBuildConfData.getSourceEntries()) {
			String key = curSourceEntry.getName();
			if ((curSourceEntry.getFlags() & ICSettingEntry.RESOLVED) != ICSettingEntry.RESOLVED) {
				// The code only support configname
				key = key.replace(CONFIG_NAME_VARIABLE, configName);
			}

			curSourceEntry = new CSourceEntry(key, curSourceEntry.getExclusionPatterns().clone(),
					curSourceEntry.getFlags()| ICSettingEntry.RESOLVED);
			ret.add(curSourceEntry);
		}
		return ret.toArray(new CSourceEntry[ret.size()]);
	}

    public boolean useDefaultBuildCommand();

    public void setUseDefaultBuildCommand(boolean useDefaultBuildCommand);

    public boolean generateMakeFilesAUtomatically();

    public void setGenerateMakeFilesAUtomatically(boolean generateMakeFilesAUtomatically);

    public String getBuildCommand(boolean noArgs);

    public boolean useStandardBuildArguments();

    public void setUseStandardBuildArguments(boolean useStandardBuildArguments);

    public boolean stopOnFirstBuildError();

    public void setStopOnFirstBuildError(boolean stopOnFirstBuildError);

    /**
     * is the build supposed to be run with multiple threads or not.
     *
     * @return true if multiple threads should be used else false.
     */
    public boolean isParallelBuild();

    public void setIsParallelBuild(boolean parallelBuild);

    /**
     * Return the number of threads to use during the build
     * if actualNumber is false this method can returns the value set with setParallelizationNum
     * if actualNumber is true the returned number is no longer "encoded" and is safe to use
     * as the number of threads.
     * This method takes isParallelBuild into account. if isParallelBuild is false this method returns 1
     *
     * @param actualNumber if true this will return the actual number of threads that should be used
     * during the build
     * If false returns what has been set in setParallelizationNum
     * @return
     */
    public int getParallelizationNum(boolean actualNumber);

    /**
     * setParallelizationNum sets the number of threads to use during a parallel build
     * valid values are PARRALLEL_BUILD_UNLIMITED_JOBS PARRALLEL_BUILD_OPTIMAL_JOBS any number >0
     * When the number >0 this is the actual threads to use.
     * PARRALLEL_BUILD_UNLIMITED_JOBS PARRALLEL_BUILD_OPTIMAL_JOBS are <0
     *
     * @param parallelizationNum
     */
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
     * For instance the default build folder is ${ProjDir}/bin/${ConfigName}
     * Though this m�ay be settable in a IFolder the GUI works with a text field so
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

    public IBuilder getBuilder();


    public void setBuilder(IBuilder builder);

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

    /**
     * Get the options (for a file) selected by the user combined with the default
     * options.
     *
     * File specific values overrule folder specific values
     * which overrule project specific values Only the parentfolder options are
     * taken into account
     *
     * @param resource
     *            the resource you want the selected options for
     * @param tool
     *            The tool you want the options for
     *
     * @return a TreeMap of <IOption,optionValue> ordered on optionID
     */
    public TreeMap<IOption, String> getSelectedOptions(IResource resource, ITool tool);

    //    public Map<String, String> getSelectedOptionNames(IResource resource, ITool tool);

    /**
     * Get the options (for a set of files) selected by the user combined with the
     * default options.
     *
     * File specific values overrule folder specific values
     * which overrule project specific values Only the parentfolder options are
     * taken into account
     *
     * Note:
     * There may be conflicting options.
     * For example the list may contain 2 cpp files where one states it should
     * compile without debug info
     * and the other with debug info
     * This method should log an error in the error log if this happens and take one
     * of the 2 options
     *
     * @param resources
     *            the resources you want the selected options for
     * @param tool
     *            The tool you want the options for
     *
     * @return a Map of <IOption,optionValue>
     */
    public Map<IOption, String> getSelectedOptions(Set<? extends IResource> resources, ITool tool);

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

    public String getToolPattern(ITool tool, IResource resource);

    public void setOptionValue(IResource resource, ITool tool, IOption option, String valueID);

    /**
     * return the value of the option for this option for this tool
     * as provided by the user for this resource.
     * In other words "nothing smart gets done"
     * It only returns a value if the user has set a value for this
     * option/tool/resource combinations using
     * setOptionValue
     *
     * The only use I can think of is to maintain user set option values
     *
     * @param resource
     *            only show option values explicitly set for this resource
     * @param tool
     *            only show option values explicitly set for this tool
     * @param option
     *            only show the option value explicitly set for this option
     * @return the value set by the user or a empty string (should never return
     *         null)
     */
    public String getOptionValue(IResource resource, ITool tool, IOption option);

    public String getExtensionPointID();

    public String getExtensionID();

    public IProjectType getProjectType();

    public IConfiguration getAutoBuildConfiguration();

    public void setModelConfiguration(IConfiguration newConfiguration);

    /**
     * Get the AutoBuildConfigurationExtensionDescription for this
     * IAutoBuildConfigurationDescription
     *
     * see setAutoBuildConfigurationExtensionDescription for more explenation on
     * IAutoBuildConfigurationExtensionDescription
     *
     * @return null if no extension is given
     */
    public AutoBuildConfigurationExtensionDescription getAutoBuildConfigurationExtensionDescription();

    /**
     * set the IAutoBuildConfigurationExtensionDescription for this
     * IAutoBuildConfigurationDescription
     * The IAutoBuildConfigurationExtensionDescription allows java developers to
     * extend autoBuild with custom implementations
     * The main problem IAutoBuildConfigurationExtensionDescription focuses on is
     * the synchronization of the classes between
     * CDT AutoBuild and the extension.
     * The creation; request to save or load all happen in one place triggered
     * sequentional
     * This makes it easier to handle configuration additions/name changes and
     * startup code. (lets hope)
     * Current thinking is that you should only set the
     * IAutoBuildConfigurationExtensionDescription once
     * at configuration creation and that the clone load save methods should deal
     * with changes
     *
     * @param newExtension
     */
    public void setAutoBuildConfigurationExtensionDescription(AutoBuildConfigurationExtensionDescription newExtension);

    public IBuildTools getBuildTools();

    public void setBuildTools(IBuildTools buildTools);

	/**
	 * provide all the options for the resource taking into account setting on the
	 * project level; folder level and file level. The result are the options that
	 * should be used to build commands for the file suppose the file
	 * src/folder1/folder2/sourceFile.cpp and the option A
	 *
	 * src folder1 folder2 sourceFile.cpp value 1 2 3 4 4 1 2 3 3 1 2 2 1 1
	 *
	 */
    public TreeMap<IOption, String> getSelectedOptions(IResource file);

	public Set<IBuilder> getAvailableBuilders();

	/**
	 * Get the buildrunner with the specified id
	 *
	 * @param buildRunnerID
	 * @return the buildrunner with the id. If the buildrunner is not found
	 *         returns the default buildrunner
	 */
	public IBuilder getBuilder(String builderID);

	public ToolFlavour getBuildToolsFlavour();

	public IProject getProject();

	/**
	 * call this method if a clean is required before a build is started
	 */
	public void forceCleanBeforeBuild();

	public String getDiscoveryCommand(String languageId);

	public String[] getEnvironmentVariables();

	public boolean isTeamShared();
	public void setTeamShared(boolean isTeamShared);

}
