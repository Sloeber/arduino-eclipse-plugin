package io.sloeber.core.txt;

import static io.sloeber.core.common.Const.*;

import java.io.File;
import java.util.Map;

public class PlatformTxtFile extends TxtFile {

    public PlatformTxtFile(File boardsFile) {
        super(getActualTxtFile(boardsFile));
    }

    private static File getActualTxtFile(File platformFile) {
        if (PLATFORM_FILE_NAME.equals(platformFile.getName())) {
            return WorkAround.MakePlatformSloeberTXT(platformFile);
        }
        return platformFile;
    }


    public Map<String, String> getAllEnvironVars() {
        return getAllEnvironVars(EMPTY);
    }
}
