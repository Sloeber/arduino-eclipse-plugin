package io.sloeber.managedBuild.Internal;

import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.extensionPoint.IOutputNameProvider;
import io.sloeber.core.api.ISloeberConfiguration;
import io.sloeber.core.common.Common;
import io.sloeber.core.common.Const;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOutputType;

public class CompileOutputNameProvider implements IOutputNameProvider {
    @SuppressWarnings("nls")
    private static Set<String> archiveOutputTypeIDs = Set.of("io.sloeber.compiler.cpp.ar.output",
            "io.sloeber.compiler.c.ar.output", "io.sloeber.compiler.S.ar.output");
    @SuppressWarnings("nls")
    private static Set<String> ignoreFileExtensions = Set.of("ino", "pde", "cxx");

    @Override
    public String getOutputFileName(IFile inputFile, IAutoBuildConfigurationDescription autoData, IInputType inputType,
            IOutputType outputType) {

        //ignore files with an extension we always need to ignore
        String fileExt = inputFile.getFileExtension();
        if (ignoreFileExtensions.contains(fileExt)) {
            return null;
        }

        ISloeberConfiguration sloeberConf = ISloeberConfiguration.getConfig(autoData);
        if (sloeberConf == null) {
            //This should not happen
            return null;
        }

        boolean bUseArchiver = Common.getBuildEnvironmentVariable(sloeberConf, Const.ENV_KEY_USE_ARCHIVER, Const.TRUE)
                .equalsIgnoreCase(Const.TRUE);
        boolean isArchiver = archiveOutputTypeIDs.contains(outputType.getId());
        if (!bUseArchiver) {
            // This is the simple case where we don't use archiving 
            // a name is requested for the archiver we return null
            // else return default name
            return isArchiver ? null : outputType.getOutputNameWithoutNameProvider(inputFile);
        }

        IPath coreFolder = sloeberConf.getArduinoCoreFolder().getProjectRelativePath();
        boolean isCoreCode = coreFolder.isPrefixOf(inputFile.getProjectRelativePath());
        if (isCoreCode) {
            return isArchiver ? outputType.getOutputNameWithoutNameProvider(inputFile) : null;
        }
        IPath libraryFolder = sloeberConf.getArduinoLibraryFolder().getProjectRelativePath();
        boolean isLibraryCode = libraryFolder.isPrefixOf(inputFile.getProjectRelativePath());
        if (!isLibraryCode) {
            return isArchiver ? null : outputType.getOutputNameWithoutNameProvider(inputFile);
        }
        //TOFIX add code to check whether library needs to go to archive

        return outputType.getOutputNameWithoutNameProvider(inputFile);
    }

}
