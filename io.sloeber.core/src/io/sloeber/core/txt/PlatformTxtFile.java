package io.sloeber.core.txt;

import java.io.File;
import java.util.Map;

public class PlatformTxtFile extends TxtFile {
    private File providedTxtFile = null;

    public PlatformTxtFile(File boardsFile) {
        super(getActualTxtFile(boardsFile));
        providedTxtFile = boardsFile;
    }

    private static File getActualTxtFile(File platformFile) {
        if (PLATFORM_FILE_NAME.equals(platformFile.getName())) {
            return WorkAround.MakePlatformSloeberTXT(platformFile);
        }
        return platformFile;
    }

    @Override
    public File getTxtFile() {
        return providedTxtFile;
    }


    public Map<String, String> getAllEnvironVars() {
        return getAllEnvironVars(ERASE_START);
    }
}
