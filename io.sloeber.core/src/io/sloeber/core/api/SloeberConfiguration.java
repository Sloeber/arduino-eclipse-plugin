package io.sloeber.core.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static io.sloeber.core.common.Const.*;
import static io.sloeber.core.common.Common.*;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICBuildSetting;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.autoBuild.api.AutoBuildConfigurationExtensionDescription;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.core.Activator;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.ConfigurationPreferences;

public class SloeberConfiguration extends AutoBuildConfigurationExtensionDescription {

    //configuration data
    BoardDescription myBoardDescription;
    OtherDescription myOtherDesc;
    CompileDescription myCompileDescription;

    //operational data
    boolean myIsDirty = true;

    //derived data
    private Map<String, String> myEnvironmentVariables = new HashMap<>();
    private boolean myIsConfiguring;

    /**
     * copy constructor
     * This constructor must be implemented for each derived class of
     * AutoBuildConfigurationExtensionDescription
     * or you will get run time errors
     * 
     * @param owner
     * @param source
     * @throws Exception
     */
    public SloeberConfiguration(AutoBuildConfigurationDescription owner,
            AutoBuildConfigurationExtensionDescription source) {
        // the code below will throw an error in case source is not an instance of SloeberConfiguration
        // This may sound strange for you but this is exactly what I want JABA
        SloeberConfiguration src = (SloeberConfiguration) source;
        setAutoBuildDescription(owner);
        myBoardDescription = new BoardDescription(src.getBoardDescription());
        myOtherDesc = new OtherDescription(src.getOtherDescription());
        myCompileDescription = new CompileDescription(src.getCompileDescription());
    }

    SloeberConfiguration(BoardDescription boardDesc, OtherDescription otherDesc, CompileDescription compileDescriptor) {
        myBoardDescription = boardDesc;
        myOtherDesc = otherDesc;
        myCompileDescription = compileDescriptor;
    }

    public BoardDescription getBoardDescription() {
        return new BoardDescription(myBoardDescription);
    }

    public OtherDescription getOtherDescription() {
        return new OtherDescription(myOtherDesc);
    }

    public CompileDescription getCompileDescription() {
        return new CompileDescription(myCompileDescription);
    }

    @Override
    public void copyData(AutoBuildConfigurationExtensionDescription from) {
        // TODO Auto-generated method stub

    }

    @Override
    public void deserialize(IAutoBuildConfigurationDescription autoCfgDescription, String curConfigsText,
            String lineStart, String lineEnd) {
        // TODO Auto-generated method stub

    }

    @Override
    public StringBuffer serialize(String linePrefix, String lineEnd) {
        // TODO Auto-generated method stub
        return null;
    }

    public void addLibrariesToProject(IProject newProjectHandle, Map<String, IPath> librariesToAdd) {
        // TODO Auto-generated method stub

    }

    public IProject getProject() {
        return getAutoBuildDescription().getCdtConfigurationDescription().getProjectDescription().getProject();
    }

    public IFolder getArduinoCodeFolder() {
        String cdtConfDescName = getAutoBuildDescription().getCdtConfigurationDescription().getName();
        IProject project = getProject();
        return project.getFolder(SLOEBER_ARDUINO_FOLDER_NAME).getFolder(cdtConfDescName);
    }

    public IFolder getArduinoCoreFolder() {
        return getArduinoCodeFolder().getFolder(SLOEBER_CODE_FOLDER_NAME);
    }

    public IFolder getArduinoVariantFolder() {
        return getArduinoCodeFolder().getFolder(SLOEBER_VARIANT_FOLDER_NAME);
    }

    public IFolder getArduinoLibraryFolder() {
        return getArduinoCodeFolder().getFolder(SLOEBER_LIBRARY_FOLDER_NAME);
    }

    public static SloeberConfiguration getActiveConfig(IProject project) {
        CoreModel coreModel = CoreModel.getDefault();
        ICProjectDescription projectDescription = coreModel.getProjectDescription(project);
        ICConfigurationDescription activeCfg = projectDescription.getActiveConfiguration();
        AutoBuildConfigurationDescription autoCfg = (AutoBuildConfigurationDescription) activeCfg
                .getConfigurationData();
        return (SloeberConfiguration) autoCfg.getAutoBuildConfigurationExtensionDescription();
    }

    public static SloeberConfiguration getConfig(ICConfigurationDescription config) {
        CConfigurationData buildSettings = config.getConfigurationData();
        if (!(buildSettings instanceof AutoBuildConfigurationDescription)) {
            //this should not happen as we just created a autoBuild project
            Common.log(new Status(SLOEBER_STATUS_DEBUG, Activator.getId(),
                    "\"Auto build created a project that does not seem to be a autobuild project :-s : " //$NON-NLS-1$
                            + config.getProjectDescription().getName()));
            return null;
        }
        return getConfig((IAutoBuildConfigurationDescription) buildSettings);
    }

    public static SloeberConfiguration getConfig(IAutoBuildConfigurationDescription autoBuildConfig) {
        return (SloeberConfiguration) autoBuildConfig.getAutoBuildConfigurationExtensionDescription();
    }

    public Map<String, String> getEnvironmentVariables() {
        if (myIsDirty && !myIsConfiguring) {
            configure();
        }

        return myEnvironmentVariables;
    }

    public void configure() {
        if (getAutoBuildDescription() == null) {
            //We can not configure if the AutoBuildDescription is not known
            System.err.println("SloeberConfiguration can not be configured if the AutoBuildDescription is not known"); //$NON-NLS-1$
            return;
        }
        myIsConfiguring = true;
        getEnvVars();
        myIsConfiguring = false;
        myIsDirty = false;
        return;

    }

    private void getEnvVars() {
        IProject project = getProject();

        myEnvironmentVariables.clear();

        myEnvironmentVariables.put(ENV_KEY_BUILD_SOURCE_PATH, project.getLocation().toOSString());
        myEnvironmentVariables.put(ENV_KEY_BUILD_PATH,
                getAutoBuildDescription().getBuildFolder().getLocation().toOSString());

        if (myBoardDescription != null) {
            myEnvironmentVariables.putAll(myBoardDescription.getEnvVars());
        }
        if (myCompileDescription != null) {
            myEnvironmentVariables.putAll(myCompileDescription.getEnvVars());
        }
        if (myOtherDesc != null) {
            myEnvironmentVariables.putAll(myOtherDesc.getEnvVars());
        }
        // set the paths
        String pathDelimiter = makeEnvironmentVar("PathDelimiter"); //$NON-NLS-1$
        if (Common.isWindows) {
            myEnvironmentVariables.put(SLOEBER_MAKE_LOCATION,
                    ConfigurationPreferences.getMakePath().addTrailingSeparator().toOSString());
            myEnvironmentVariables.put(SLOEBER_AWK_LOCATION,
                    ConfigurationPreferences.getAwkPath().addTrailingSeparator().toOSString());

            String systemroot = makeEnvironmentVar("SystemRoot"); //$NON-NLS-1$
            myEnvironmentVariables.put("PATH", //$NON-NLS-1$
                    makeEnvironmentVar(ENV_KEY_COMPILER_PATH) + pathDelimiter
                            + makeEnvironmentVar(ENV_KEY_BUILD_GENERIC_PATH) + pathDelimiter + systemroot + "\\system32" //$NON-NLS-1$
                            + pathDelimiter + systemroot + pathDelimiter + systemroot + "\\system32\\Wbem" //$NON-NLS-1$
                            + pathDelimiter + makeEnvironmentVar("sloeber_path_extension")); //$NON-NLS-1$
        } else {
            myEnvironmentVariables.put("PATH", makeEnvironmentVar(ENV_KEY_COMPILER_PATH) + pathDelimiter //$NON-NLS-1$
                    + makeEnvironmentVar(ENV_KEY_BUILD_GENERIC_PATH) + pathDelimiter + makeEnvironmentVar("PATH")); //$NON-NLS-1$
        }
    }

}
