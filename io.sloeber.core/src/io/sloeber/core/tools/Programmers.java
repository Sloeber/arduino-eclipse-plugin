package io.sloeber.core.tools;

import java.io.File;

import org.eclipse.core.runtime.Path;

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

	public static Programmers[] fromBoards(String boardsFileName) {
		return fromBoards(new File(boardsFileName));
	}

	public static Programmers[] fromBoards(File boardsFile) {
		File BoardsFile1 = new Path(boardsFile.getParentFile().toString()).append(programmersFileName1).toFile();

		File BoardsFile2 = new Path(boardsFile.getParentFile().toString()).append(programmersFileName2).toFile();
		if (BoardsFile1.exists() & BoardsFile2.exists()) {
			Programmers ret[] = new Programmers[2];

			ret[0] = new Programmers(BoardsFile1);
			ret[1] = new Programmers(BoardsFile2);
			return ret;
		}
		if (BoardsFile1.exists()) {
			Programmers ret[] = new Programmers[1];

			ret[0] = new Programmers(BoardsFile1);
			return ret;
		}
		if (BoardsFile2.exists()) {
			Programmers ret[] = new Programmers[1];

			ret[0] = new Programmers(BoardsFile2);
			return ret;

		}
		return new Programmers[0];

	}

	public static String[] getUploadProtocols(String boardsFileName) {
		String[] ret = new String[1];
		ret[0] = Defaults.getDefaultUploadProtocol();
		Programmers allProgrammers[] = fromBoards(new File(boardsFileName));
		for (Programmers curprogrammer : allProgrammers) {
			ret = curprogrammer.getAllNames(ret);
		}
		return ret;

	}

	public static Programmers[] fromBoards(BoardDescriptor boardsDescriptor) {
		return fromBoards(boardsDescriptor.getReferencingBoardsFile());
	}

}
