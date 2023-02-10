package io.sloeber.autoBuild.extensionPoint.providers;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IFile;

import io.sloeber.autoBuild.extensionPoint.IOutputNameProvider;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ITool;

public class NameProviderObjectForArchiver implements IOutputNameProvider {

    public NameProviderObjectForArchiver() {
        // nothing to do here
    }

    @Override
    public String getOutputFileName(IFile inputFile, ICConfigurationDescription config, IInputType inputType,
            IOutputType outputType) {
        if (!NameProviderArchive.isArchiveInputFile(inputFile)) {
            return null;
        }
        return inputFile.getName() + NameProviderObjectToLinker.OBJECT_EXTENSION;
    }

}
