package io.sloeber.autoBuild.extensionPoint.providers;

import io.sloeber.autoBuild.api.IAutoBuildConfigurationDescription;
import io.sloeber.autoBuild.extensionPoint.IOutputNameProvider;
import io.sloeber.autoBuild.schema.api.IInputType;
import io.sloeber.autoBuild.schema.api.IOutputType;
import io.sloeber.autoBuild.schema.api.ITool;

import static io.sloeber.autoBuild.helpers.api.AutoBuildConstants.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;

public class NameProviderAutoBuild implements IOutputNameProvider {
    public static final String OBJECT_EXTENSION = ".o"; //$NON-NLS-1$
    public static final String STATIC_LIB = "staticLib"; //$NON-NLS-1$
    public static final String SHARED_LIB = "sharedLib"; //$NON-NLS-1$
    public static final String EXECUTABLE = "exe"; //$NON-NLS-1$
    private static final int STATIC_LIB_TYPE = 1;
    private static final int SHARED_LIB_TYPE = 2;
    private static final int EXE_TYPE = 3;
    private static final int TYPE_ERROR = 4;

    public NameProviderAutoBuild() {
        // nothing to do here
    }

    @Override
    public String getOutputFileName(IFile inputFile, IAutoBuildConfigurationDescription autoData, IInputType inputType,
            IOutputType outputType) {
        ITool tool = inputType.getTool();
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
            return outputType.getOutputNameWithoutNameProvider(inputFile);
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
            return SHARED_LIB_TYPE;
        }
        return STATIC_LIB_TYPE;
    }

    /**
     * Is this outputType for staticlib, shared lib or exe
     * This is based on the outputType id last segment
     *
     * @param outputType
     * @return
     */
    private static int getOutputTypeType(IOutputType outputType) {
        String outputID = outputType.getId();
        String outputTypeID = outputID.substring(outputID.lastIndexOf(DOT) + 1);
        switch (outputTypeID) {
        case STATIC_LIB:
            return STATIC_LIB_TYPE;
        case SHARED_LIB:
            return SHARED_LIB_TYPE;
        case EXECUTABLE:
            return EXE_TYPE;
		default:
			break;
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
        case SHARED_LIB_TYPE:
            return libName + DOT + DYNAMIC_LIB_EXTENSION;
        case EXE_TYPE:
            return EXE_NAME;
		default:
			break;

        }
        return null;
    }

}
