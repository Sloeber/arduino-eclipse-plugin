package io.sloeber.core.tools;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.runtime.IPath;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.Defaults;

public class Programmers extends TxtFile {
	private static final String programmersFileName1 = "programmers.txt";//$NON-NLS-1$
	private static final String programmersFileName2 = "externalprogrammers.txt";//$NON-NLS-1$

	Programmers(String programmersFileName) {
		super(new File(programmersFileName));
	}

	Programmers(File programmersFile) {
		super(programmersFile);
	}

	private static Programmers[] fromBoards(IPath referencingPlatformPath, IPath referencedPlatformPath) {
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

		Programmers ret[] = new Programmers[BoardsFiles.size()];
		int i = 0;
		for (File file : BoardsFiles) {
			ret[i++] = new Programmers(file);
		}

		return ret;

	}

	public static String[] getUploadProtocols(BoardDescriptor boardsDescriptor) {
		String[] ret = new String[1];
		ret[0] = Defaults.getDefaultUploadProtocol();
		Programmers allProgrammers[] = fromBoards(boardsDescriptor.getreferencingPlatformPath(),
				boardsDescriptor.getReferencedUploadPlatformPath());
		for (Programmers curprogrammer : allProgrammers) {
			ret = curprogrammer.getAllNames(ret);
		}
		return ret;

	}

	public static Programmers[] fromBoards(BoardDescriptor boardsDescriptor) {
		return fromBoards(boardsDescriptor.getreferencingPlatformPath(),
				boardsDescriptor.getReferencedUploadPlatformPath());
	}

}
