package io.sloeber.core.txt;

import static io.sloeber.core.api.Const.*;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IPath;

import io.sloeber.arduinoFramework.api.BoardDescription;

public class Programmers extends BoardTxtFile {



    public Programmers(File programmersFile) {
        super(getActualTxtFile(programmersFile));
    }

    private static File getActualTxtFile(File programmersFile) {
        String txtFileNamer = programmersFile.getName();
        if (PROGRAMMER_TXT_FILE_NAME.equals(txtFileNamer) || EXTERNAL_PROGRAMMERS_TXT_FILE_NAME.equals(txtFileNamer)) {
            return WorkAround.MakeProgrammersSloeberTXT(programmersFile);
        }
        return programmersFile;
    }

	private static Programmers[] fromBoards(IPath referencingPlatformPath, IPath referencedPlatformPath,
			IPath arduinoPlatformPath) {
		HashSet<File> BoardsFiles = new HashSet<>();
		BoardsFiles.add(referencingPlatformPath.append(PROGRAMMER_TXT_FILE_NAME).toFile());
		BoardsFiles.add(referencingPlatformPath.append(EXTERNAL_PROGRAMMERS_TXT_FILE_NAME).toFile());
		if (referencedPlatformPath != null) {
			BoardsFiles.add(referencedPlatformPath.append(PROGRAMMER_TXT_FILE_NAME).toFile());
			BoardsFiles.add(referencedPlatformPath.append(EXTERNAL_PROGRAMMERS_TXT_FILE_NAME).toFile());
		}
		for (Iterator<File> i = BoardsFiles.iterator(); i.hasNext();) {
			File file = i.next();
			if (!file.exists()) {
				i.remove();
			}
		}
		if ((BoardsFiles.size() == 0) && (referencedPlatformPath == null) && (arduinoPlatformPath != null)) {
			if (arduinoPlatformPath.append(PROGRAMMER_TXT_FILE_NAME).toFile().exists()) {
				BoardsFiles.add(arduinoPlatformPath.append(PROGRAMMER_TXT_FILE_NAME).toFile());
			}
		}
		Programmers ret[] = new Programmers[BoardsFiles.size()];
		int i = 0;
		for (File file : BoardsFiles) {
			ret[i++] = new Programmers(file);
		}

		return ret;

	}

	public static String[] getUploadProtocols(BoardDescription boardsDescriptor) {
        String[] ret = new String[0];
		Programmers allProgrammers[] = fromBoards(boardsDescriptor);
		for (Programmers curprogrammer : allProgrammers) {
            ret = curprogrammer.getAllSectionNames(ret);
		}
		return ret;

	}

	public static Programmers[] fromBoards(BoardDescription boardsDescriptor) {
		return fromBoards(boardsDescriptor.getreferencingPlatformPath(),
				boardsDescriptor.getReferencedUploadPlatformPath(), boardsDescriptor.getArduinoPlatformPath());
	}

    @Override
    public Map<String, String> getAllEnvironVars(String programmerID) {
        return getSection(programmerID);

    }

}
