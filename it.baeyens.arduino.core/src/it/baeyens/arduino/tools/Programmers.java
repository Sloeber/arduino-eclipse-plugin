package it.baeyens.arduino.tools;

import java.io.File;

import org.eclipse.core.runtime.Path;

import it.baeyens.arduino.common.Const;

public class Programmers extends TxtFile {
    private static final String programmersFileName = "programmers.txt";//$NON-NLS-1$

    Programmers(String programmersFileName) {
	super(new File(programmersFileName));
    }

    Programmers(File programmersFile) {
	super(programmersFile);
    }

    public static Programmers fromBoards(String boardsFileName) {
	return fromBoards(new File(boardsFileName));
    }

    public static Programmers fromBoards(File boardsFile) {
	File BoardsFile = new Path(boardsFile.getParentFile().toString()).append(programmersFileName).toFile();
	return new Programmers(BoardsFile);
    }

    public String[] GetUploadProtocols() {
	String[] defaultValue = new String[1];
	defaultValue[0] = Const.DEFAULT;
	return getAllNames(defaultValue);

    }

}
