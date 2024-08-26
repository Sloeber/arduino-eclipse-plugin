package io.sloeber.core.txt;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IPath;

import io.sloeber.arduinoFramework.api.BoardDescription;

public class Programmers extends BoardTxtFile {
	private static final String programmersFileName1 = "programmers.txt";//$NON-NLS-1$
	private static final String programmersFileName2 = "externalprogrammers.txt";//$NON-NLS-1$



    public Programmers(File programmersFile) {
        super(getActualTxtFile(programmersFile));
    }

    private static File getActualTxtFile(File programmersFile) {
        String txtFileNamer = programmersFile.getName();
        if (programmersFileName1.equals(txtFileNamer) || programmersFileName2.equals(txtFileNamer)) {
            return WorkAround.MakeProgrammersSloeberTXT(programmersFile);
        }
        return programmersFile;
    }

	private static Programmers[] fromBoards(IPath referencingPlatformPath, IPath referencedPlatformPath,
			IPath arduinoPlatformPath) {
		HashSet<File> BoardsFiles = new HashSet<>();
		BoardsFiles.add(referencingPlatformPath.append(programmersFileName1).toFile());
		BoardsFiles.add(referencingPlatformPath.append(programmersFileName2).toFile());
		if (referencedPlatformPath != null) {
			BoardsFiles.add(referencedPlatformPath.append(programmersFileName1).toFile());
			BoardsFiles.add(referencedPlatformPath.append(programmersFileName2).toFile());
		}
		for (Iterator<File> i = BoardsFiles.iterator(); i.hasNext();) {
			File file = i.next();
			if (!file.exists()) {
				i.remove();
			}
		}
		if ((BoardsFiles.size() == 0) && (referencedPlatformPath == null) && (arduinoPlatformPath != null)) {
			if (arduinoPlatformPath.append(programmersFileName1).toFile().exists()) {
				BoardsFiles.add(arduinoPlatformPath.append(programmersFileName1).toFile());
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
