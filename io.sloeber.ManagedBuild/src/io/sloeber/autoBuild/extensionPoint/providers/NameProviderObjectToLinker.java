package io.sloeber.autoBuild.extensionPoint.providers;

import org.eclipse.core.resources.IFile;

import io.sloeber.autoBuild.extensionPoint.IOutputNameProvider;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationData;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOutputType;

public class NameProviderObjectToLinker implements IOutputNameProvider {
    public static String OBJECT_EXTENSION = ".o"; //$NON-NLS-1$

    public NameProviderObjectToLinker() {
        // nothing to do here
    }

    @Override
    public String getOutputFileName(IFile inputFile, AutoBuildConfigurationData autoData, IInputType inputType,
            IOutputType outputType) {
        if (NameProviderArchive.isArchiveInputFile(inputFile)) {
            return null;
        }
        return inputFile.getName() + OBJECT_EXTENSION;
    }

}
