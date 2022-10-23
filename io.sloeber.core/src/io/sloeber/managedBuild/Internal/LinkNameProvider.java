package io.sloeber.managedBuild.Internal;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.core.tools.Helpers;
import io.sloeber.managedBuild.api.INewManagedOutputNameProvider;

public class LinkNameProvider implements INewManagedOutputNameProvider {

    @Override
    public IPath getOutputName(IProject project, IConfiguration cConf, ITool tool, IPath inputName) {
        boolean bUseArchiver = Common
                .getBuildEnvironmentVariable(project, cConf.getName(), Const.ENV_KEY_USE_ARCHIVER, Const.TRUE)
                .equalsIgnoreCase(Const.TRUE);
        if (inputName.toString().startsWith(Const.ARDUINO_CODE_FOLDER_PATH) && (bUseArchiver)) {
            return null;
        }
        if (inputName.toString().endsWith(".ino")) { //$NON-NLS-1$
            return null;
        }
        if (inputName.toString().endsWith(".pde")) { //$NON-NLS-1$
            return null;
        }
        if (inputName.toString().endsWith(".cxx")) { //$NON-NLS-1$
            return null;
        }
        return Helpers.GetOutputName(inputName).addFileExtension("o"); //$NON-NLS-1$
    }

}
