package io.sloeber.autoBuild.extensionPoint.providers;

import java.util.Arrays;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IFile;

import io.sloeber.autoBuild.extensionPoint.IOutputNameProvider;
import io.sloeber.autoBuild.integration.AutoBuildConfigurationData;
import io.sloeber.schema.api.IInputType;
import io.sloeber.schema.api.IOutputType;

public class NameProviderArchive implements IOutputNameProvider {
    static String myArchiveFolders[] = { "Libraries", "libraries", "Archives", "archives" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    static String myFixedNameArchiveFolders[] = { "core", "Core" }; //$NON-NLS-1$ //$NON-NLS-2$
    static private String ARCHIVE_EXTENSION = ".ar"; //$NON-NLS-1$

    public NameProviderArchive() {
        // nothing to do here
    }

    public static boolean isArchiveInputFile(IFile inputFile) {
        return getArchiveFileName(inputFile, true) != null;
    }

    private static String getArchiveFileName(IFile inputFile, boolean findOnly) {
        String segments[] = inputFile.getProjectRelativePath().segments();
        for (String cur : myFixedNameArchiveFolders) {
            if (Arrays.stream(segments).anyMatch(cur::contains)) {
                return cur + ARCHIVE_EXTENSION;
            }
        }
        for (String cur : myArchiveFolders) {
            if (Arrays.stream(segments).anyMatch(cur::contains)) {
                if (findOnly)
                    return cur;
                for (int segmentIndex = 0; segmentIndex < segments.length; segmentIndex++) {
                    if (Arrays.stream(myArchiveFolders).anyMatch(segments[segmentIndex]::contains)) {
                        return segments[segmentIndex + 1] + ARCHIVE_EXTENSION;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String getOutputFileName(IFile inputFile, AutoBuildConfigurationData autoData, IInputType inputType,
            IOutputType outputType) {
        String archiveFileName = getArchiveFileName(inputFile, false);
        return archiveFileName;
    }

}
