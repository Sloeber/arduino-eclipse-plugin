package io.sloeber.core.txt;

import static io.sloeber.core.Messages.*;
import static io.sloeber.core.api.Const.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.autoBuild.helpers.api.KeyValueTree;
import io.sloeber.core.api.Common;
import io.sloeber.core.api.Const;

/**
 * TxtFile is a class that hides the Arduino *.txt file processing <br/>
 * The is based on the code of Trump at
 * https://github.com/Trump211/ArduinoEclipsePlugin and later renamed from
 * Boards to TxtFile and adapted as needed.
 *
 * This class is at the root of processing the boards.txt platform.txt and
 * programmers.txt from the Arduino eco system As this feature is available most
 * other configuration stuff is put in files with the same setup and processed
 * by this class
 *
 * @author Jan Baeyens and trump
 *
 */
public class TxtFile {
    private File mLoadedTxtFile = null;

    protected KeyValueTree myData = KeyValueTree.createRoot();

    public TxtFile(File boardsFileName) {

        mLoadedTxtFile = boardsFileName;
        mergeFile(boardsFileName);

    }

    /**
     * given a TXT file add the data from another txt file
     *
     * @param boardsFileName
     * @return
     */
    public void mergeFile(File boardsFileName) {
        if (!boardsFileName.exists()) {
            return;
        }

        try {
            String[] lines = readLines(boardsFileName.getPath());
            for (String line : lines) {
                if ((line.length() == 0) || (line.charAt(0) == '#'))
                    continue;

                String[] lineParts = line.split("=", 2); //$NON-NLS-1$
                if (lineParts.length == 2) {
                    String key = lineParts[0].trim();
                    String value = lineParts[1].trim();
                    myData.addValue(key, value);
                }
            }
        } catch (Exception e) {
            Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
                    Boards_Failed_to_read_boards.replace(FILE_TAG, boardsFileName.getName()), e));
        }
    }

    /**
     * This method returns the full section so custom processing can be done.
     * without MENU submenu!!
     *
     * @param SectionKey
     *            the first name on the line before the .
     * @return all entries that match the filter
     */
    public Map<String, String> getSection(String SectionKey) {
        if (null == SectionKey) {
            return new HashMap<>();
        }
        return this.myData.getChild(SectionKey).toKeyValues(false);
    }

    /**
     * from
     * https://stackoverflow.com/questions/285712/java-reading-a-file-into-an-array#285745
     * reads a file to a string array
     *
     * @param filename
     * @return
     * @throws IOException
     */
    private static String[] readLines(String filename) throws IOException {
        try (FileReader fileReader = new FileReader(filename)) {
            try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {
                List<String> lines = new ArrayList<>();
                String line = null;
                while ((line = bufferedReader.readLine()) != null) {
                    lines.add(line);
                }
                bufferedReader.close();
                return lines.toArray(new String[lines.size()]);
            }
        }
    }

    public String getNiceNameFromID(String myBoardID) {
        return myData.getValue(myBoardID + DOT + NAME);
    }

    /*
     * Returns the architecture based on the platform file name Caters for the
     * packages (with version number and for the old way if the boards file does not
     * exists returns avr
     */
    public String getArchitecture() {

        IPath platformFile = new Path(this.mLoadedTxtFile.toString().trim());
        String architecture = platformFile.removeLastSegments(1).lastSegment();
        if (architecture == null) {// for error conditions
            architecture = AVR;
        }
        if (architecture.contains(DOT)) { // This is a version number so
            // package
            architecture = platformFile.removeLastSegments(2).lastSegment();
        }
        return architecture;
    }

    /**
     * Given a nice name look for the ID The assumption is that the txt file
     * contains a line like ID.name=[nice name] Given this this method returns ID
     * when given [nice name]
     */
    public String getIDFromNiceName(String name) {
        if ((name == null) || name.isEmpty()) {
            return null;
        }
        for (String curID : myData.getChildren().keySet()) {
            if (name.equals(myData.getValue(curID + DOT + NAME))) {
                return curID;
            }
        }
        return null;
    }

    /**
     * Get all the key value pairs that need to be added to the environment
     * variables
     *
     * prefix something to add at the beginning of each key name
     */
    public Map<String, String> getAllEnvironVars(String prefix) {
        return myData.toKeyValues(prefix, true);
    }

    public KeyValueTree getData() {
        return myData;
    }

    public void reloadTxtFile() {
        myData = KeyValueTree.createRoot();
        mergeFile(mLoadedTxtFile);

    }

    public File getLoadedFile() {
        return mLoadedTxtFile;
    }
}
