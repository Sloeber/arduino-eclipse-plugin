package io.sloeber.autoBuild.extensionPoint.providers;

import java.util.Arrays;
import java.util.LinkedList;

import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.core.resources.IFile;

import io.sloeber.autoBuild.extensionPoint.IOutputNameProvider;
import io.sloeber.schema.api.IInputType;

public class ArchiveNameProvider implements IOutputNameProvider {
    static String myArchiveFolders[] = { "Libraries", "libraries", "Archives", "archives" };
    static String myFixedNameArchiveFolders[] = { "core", "Core" };
    static private String ARCHIVE_EXTENSION = ".ar";

    public ArchiveNameProvider() {
        // nothing to do here
    }

    @Override
    public String getOutputFileName(IFile inputFile, ICConfigurationDescription config, IInputType inputType) {
        String archiveFileName = getArchiveFileName(inputFile, false);
        return archiveFileName;
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

}
