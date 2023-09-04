package io.sloeber.managedBuild.Internal;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

import io.sloeber.autoBuild.api.AutoBuildConfigurationExtensionDescription;
import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.extensionPoint.IOutputNameProvider;
import io.sloeber.core.api.SloeberConfiguration;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOutputType;

public class ArchiveNameProvider implements IOutputNameProvider {

    @Override
    public String getOutputFileName(IFile inputFile, IAutoBuildConfigurationDescription autoData, IInputType inputType,
            IOutputType outputType) {
        ICConfigurationDescription confdesc = autoData.getCdtConfigurationDescription();
        IProject project = inputFile.getProject();
        String fileExt = inputFile.getFileExtension();
        boolean bUseArchiver = Common
                .getBuildEnvironmentVariable(project, confdesc.getName(), Const.ENV_KEY_USE_ARCHIVER, Const.TRUE)
                .equalsIgnoreCase(Const.TRUE);
        if (!bUseArchiver) {
            // we don't use archiving so we ignore all files
            return null;
        }
        AutoBuildConfigurationExtensionDescription extDesc = autoData.getAutoBuildConfigurationExtensionDescription();
        if (extDesc != null && extDesc instanceof SloeberConfiguration) {
            SloeberConfiguration sloeberCfg = (SloeberConfiguration) extDesc;
            IPath coreFolder = sloeberCfg.getArduinoCoreFolder().getProjectRelativePath();
            if (coreFolder.isPrefixOf(inputFile.getProjectRelativePath()) && (!"cxx".equals(fileExt))) { //$NON-NLS-1$
                return inputFile.getName() + 'o';
            }
        }
        return null;
    }

}
