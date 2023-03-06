package io.sloeber.autoBuild.extensionPoint.providers;

import static io.sloeber.autoBuild.integration.AutoBuildConstants.*;
import io.sloeber.autoBuild.extensionPoint.IOutputNameProvider;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationData;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOutputType;
import io.sloeber.schema.api.ITool;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

public class NameProviderAutoBuild implements IOutputNameProvider {
    public static final String OBJECT_EXTENSION = ".o"; //$NON-NLS-1$
    public static final String STATIC_LIB = "staticLib"; //$NON-NLS-1$
    public static final String DYNAMIC_LIB = "sharedLib"; //$NON-NLS-1$
    public static final String EXECUTABLE = "exe"; //$NON-NLS-1$
    private static final int STATIC_LIB_TYPE = 1;
    private static final int DYNAMIC_LIB_TYPE = 2;
    private static final int EXE_TYPE = 3;
    private static final int TYPE_ERROR = 4;

    public NameProviderAutoBuild() {
        // nothing to do here
    }

    @Override
    public String getOutputFileName(IFile inputFile, AutoBuildConfigurationData autoData, IInputType inputType,
            IOutputType outputType) {
        ITool tool = inputType.getParent();
        if (tool.getId().endsWith(DOT + COMPILER)) {
            return getOutputFileNameForCompiler(inputFile, outputType);
        }
        //this is a linker
        return getOutputFileNameForLinker(inputFile, outputType);
    }

    private static String getOutputFileNameForCompiler(IFile inputFile, IOutputType outputType) {
        int outputTypeType = getOutputTypeType(outputType);
        int fileType = getFileType(inputFile);
        if (fileType == outputTypeType) {
            return inputFile.getName() + OBJECT_EXTENSION;
        }
        return null;
    }

    private static int getFileType(IFile inputFile) {
        String inputFileFQN = inputFile.toString().toLowerCase();
        if (!inputFileFQN.contains(LIBRARY_PATH_SUFFIX)) {
            return EXE_TYPE;
        }
        IResource LibFolder = inputFile;
        while (!LibFolder.getParent().getName().equals(LIBRARY_PATH_SUFFIX)) {
            LibFolder = LibFolder.getParent();
        }
        if (((IFolder) LibFolder).getFile(DYNAMIC_LIB_FILE).exists()) {
            return DYNAMIC_LIB_TYPE;
        }
        return STATIC_LIB_TYPE;
    }

    private static int getOutputTypeType(IOutputType outputType) {
        String outputID = outputType.getId();
        String outputTypeID = outputID.substring(outputID.lastIndexOf(DOT) + 1);
        switch (outputTypeID) {
        case STATIC_LIB:
            return STATIC_LIB_TYPE;
        case DYNAMIC_LIB:
            return DYNAMIC_LIB_TYPE;
        case EXECUTABLE:
            return EXE_TYPE;
        }
        return TYPE_ERROR;
    }

    private static String getOutputFileNameForLinker(IFile inputFile, IOutputType outputType) {
        String inputFileName = inputFile.toString();
        String inputFileNameLower = inputFileName.toLowerCase();

        String libNameStart = inputFileName.substring(
                inputFileNameLower.lastIndexOf(SLACH + LIBRARY_PATH_SUFFIX + SLACH) + 2 + LIBRARY_PATH_SUFFIX.length());
        String libName = libNameStart.substring(0, libNameStart.indexOf(SLACH));
        switch (getOutputTypeType(outputType)) {
        case STATIC_LIB_TYPE:
            return libName + DOT + STATIC_LIB_EXTENSION;
        case DYNAMIC_LIB_TYPE:
            return libName + DOT + DYNAMIC_LIB_EXTENSION;
        case EXE_TYPE:
            return EXE_NAME;

        }
        return null;
    }

}
