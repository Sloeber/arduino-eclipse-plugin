package io.sloeber.managedBuild.Internal;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.core.tools.Helpers;
import io.sloeber.managedBuild.api.IManagedOutputNameProviderJaba;

public class ArchiveNameProvider implements IManagedOutputNameProviderJaba {

    @Override
    public IPath getOutputName(IProject project, IConfiguration cConf, ITool tool, IPath primaryInput) {

        boolean bUseArchiver = Common
                .getBuildEnvironmentVariable(project, cConf.getName(), Const.ENV_KEY_USE_ARCHIVER, Const.TRUE)
                .equalsIgnoreCase(Const.TRUE);
        if (!bUseArchiver) {
            // we don't use archiving so we ignore all files
            return null;
        }

        if (primaryInput.toString().startsWith(Const.ARDUINO_CODE_FOLDER_PATH)
                && (!"cxx".equals(primaryInput.getFileExtension()))) { //$NON-NLS-1$
            return Helpers.GetOutputName(primaryInput).addFileExtension("o"); //$NON-NLS-1$
        }
        return null;
    }

}
