package io.sloeber.schema.internal.legacy;

import java.util.Map;
import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;
import org.eclipse.core.resources.IFile;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.extensionPoint.IOutputNameProvider;
import io.sloeber.autoBuild.extensionPoint.providers.AutoBuildCommon;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationDescription;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ISchemaObject;
import io.sloeber.schema.api.ITool;
import io.sloeber.schema.internal.OutputType;

/**
 * This class is a port of
 * 
 * @author jan
 *
 */
public class OutputNameProviderCompatibilityClass implements IOutputNameProvider {

    @Override
    public String getOutputFileName(IFile inputFile, IAutoBuildConfigurationDescription autoData, IInputType inputType,
            IOutputType outputType) {
        ITool tool = inputType.getParent();

        boolean isToolCLinker = tool.hasAncestor("cdt.managedbuild.tool.gnu.c.linker"); //$NON-NLS-1$
        boolean isToolCPPLinker = tool.hasAncestor("cdt.managedbuild.tool.gnu.cpp.linker"); //$NON-NLS-1$

        if (isToolCLinker || isToolCPPLinker) {
            // it is a linker tool
            Map<String, String> projectOptions = autoData.getSelectedOptions(inputFile);

            if (isToolCLinker) {
                // Are we building a dynamically shared C library?

                String optCSharedValue = projectOptions.get("gnu.c.link.option.shared"); //$NON-NLS-1$
                if (optCSharedValue != null) {
                    if (Boolean.parseBoolean(optCSharedValue)) {
                        String dynamicLibraryName = projectOptions.get("gnu.c.link.option.soname"); //$NON-NLS-1$
                        if (dynamicLibraryName != null && !dynamicLibraryName.isBlank()) {
                            return dynamicLibraryName;
                        }
                    }
                }
            }
            if (isToolCPPLinker) {
                // Are we building a dynamically shared CPP library?
                String optCPPSharedValue = projectOptions.get("gnu.cpp.link.option.shared"); //$NON-NLS-1$
                if (optCPPSharedValue != null) {
                    if (Boolean.parseBoolean(optCPPSharedValue)) {
                        String dynamicLibraryName = projectOptions.get("gnu.cpp.link.option.soname"); //$NON-NLS-1$
                        if (dynamicLibraryName != null && dynamicLibraryName.length() > 0) {
                            return dynamicLibraryName;
                        }
                    }
                }

                // This is not a dynamically shared library

                // is this a Executable?
                if ("org.eclipse.cdt.build.core.buildArtefactType.exe" //$NON-NLS-1$
                        .equals(autoData.getProperty(ISchemaObject.BUILD_ARTEFACT_TYPE_PROPERTY_ID))) {
                    if (isWindows)
                        return PROJECT_NAME_VARIABLE + ".exe"; //$NON-NLS-1$
                    return PROJECT_NAME_VARIABLE;
                }
                // This is not a Executable?
            }
        }
        // it is not a linking tool

        //Check if the outputType provided a outputName and if so use that
        if (!outputType.getOutputName().isBlank()) {
            return outputType.getOutputName();
        }

        // Determine a default name from the input file name
        String fileName = inputFile.getProjectRelativePath().removeFileExtension().lastSegment();
        if (fileName.startsWith("$(") && fileName.endsWith(")")) { //$NON-NLS-1$ //$NON-NLS-2$
            fileName = fileName.substring(2, fileName.length() - 1);
        }
        //Is this a archive project
        if ("org.eclipse.cdt.build.core.buildArtefactType.staticLib" //$NON-NLS-1$
                .equals(autoData.getProperty(ISchemaObject.BUILD_ARTEFACT_TYPE_PROPERTY_ID))) {
            fileName = PROJECT_NAME_VARIABLE;
        }
        // Add the primary output type extension
        String exts = outputType.getOutputExtension();
        if (!exts.isBlank()) {
            fileName += "." + exts; //$NON-NLS-1$
        }
        String outputPrefix = AutoBuildCommon.resolve(outputType.getOutputPrefix(), autoData);
        return outputPrefix + fileName;
    }

}
