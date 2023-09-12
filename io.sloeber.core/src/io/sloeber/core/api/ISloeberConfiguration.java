package io.sloeber.core.api;

import static io.sloeber.core.common.Const.*;

import java.util.Map;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.core.Activator;
import io.sloeber.core.common.Common;

public interface ISloeberConfiguration {
    public static ISloeberConfiguration getActiveConfig(IProject project) {
        CoreModel coreModel = CoreModel.getDefault();
        ICProjectDescription projectDescription = coreModel.getProjectDescription(project);
        return getActiveConfig(projectDescription);
    }

    public static ISloeberConfiguration getActiveConfig(ICProjectDescription projectDescription) {
        ICConfigurationDescription activeCfg = projectDescription.getActiveConfiguration();
        AutoBuildConfigurationDescription autoCfg = (AutoBuildConfigurationDescription) activeCfg
                .getConfigurationData();
        return (ISloeberConfiguration) autoCfg.getAutoBuildConfigurationExtensionDescription();
    }

    public static ISloeberConfiguration getConfig(ICConfigurationDescription config) {
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

    public static ISloeberConfiguration getConfig(IAutoBuildConfigurationDescription autoBuildConfig) {
        return (ISloeberConfiguration) autoBuildConfig.getAutoBuildConfigurationExtensionDescription();
    }

    IFolder getArduinoCodeFolder();

    IFolder getArduinoCoreFolder();

    IFolder getArduinoVariantFolder();

    IFolder getArduinoLibraryFolder();

    BoardDescription getBoardDescription();

    CompileDescription getCompileDescription();

    OtherDescription getOtherDescription();

    IAutoBuildConfigurationDescription getAutoBuildDesc();

    IProject getProject();

    Map<String, String> getEnvironmentVariables();

    /**
     * get the text for the decorator
     * 
     * @param text
     * @return
     */
    String getDecoratedText(String text);

    /**
     * Synchronous upload of the sketch to the board returning the status.
     *
     * @param project
     * @return the status of the upload. Status.OK means upload is OK
     */
    IStatus upload();

    IStatus upLoadUsingProgrammer();

    IStatus burnBootloader();

    /*
     * Is the sloeber configuration rready to be indexed?
     */
    boolean canBeIndexed();

    void setBoardDescription(BoardDescription boardDescription);

    IFile getTargetFile();

}
