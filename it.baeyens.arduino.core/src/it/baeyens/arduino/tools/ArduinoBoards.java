package it.baeyens.arduino.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import it.baeyens.arduino.common.ArduinoConst;
import it.baeyens.arduino.common.Common;

/**
 * ArduinoBoards is that class that hides the Arduino Boards.txt file <br/>
 * The is based on the code of Trump at https://github.com/Trump211/ArduinoEclipsePlugin and later adapted as needed.
 * 
 * @author Jan Baeyens and trump
 * 
 */
public class ArduinoBoards {
    private File mLastLoadedBoardsFile = null;
    private static final String DOT = ArduinoConst.DOT;
    private static final String MENU = "menu"; //$NON-NLS-1$
    Map<String, String> settings = null;
    // private String mLastLoadedBoard = "";
    private Map<String, Map<String, String>> mArduinoSupportedBoards = new LinkedHashMap<>(); // all the data

    public ArduinoBoards(File boardsFileName) {
	LoadBoardsFile(boardsFileName);
    }

    public ArduinoBoards() {
	// no constructor needed
    }

    /**
     * This method returns the full section so custom processing can be done.
     * 
     * @param SectionKey
     *            the first name on the line before the .
     * @return all entries that match the filter
     */
    public Map<String, String> getSection(String SectionKey) {
	return this.mArduinoSupportedBoards.get(SectionKey);
    }

    /**
     * Get all the options in the boards.txt file
     * 
     * @return a list of all the menu option name
     */
    public String[] getMenuNames() {
	HashSet<String> ret = new HashSet<>();
	for (Entry<String, Map<String, String>> entry : this.mArduinoSupportedBoards.entrySet()) {
	    if (entry.getKey().equals(MENU)) {
		for (Entry<String, String> e2 : entry.getValue().entrySet()) {
		    if (!e2.getKey().contains(ArduinoConst.DOT)) {
			if (!ret.contains(e2.getValue())) {
			    ret.add(e2.getValue());
			}
		    }
		}
	    }
	}
	return ret.toArray(new String[ret.size()]);
    }

    /**
     * Get all the acceptable values for a option for a board The outcome of this method can be used to fill a combobox
     * 
     * @param menu
     *            the name of a menu not the ide
     * @param boardName
     *            the name of a board not the ide
     * @return
     */
    public String[] getMenuItemNames(String menuLabel, String boardName) {
	String menuID = null;
	String boardID = getBoardIDFromName(boardName);
	HashSet<String> ret = new HashSet<>();
	Map<String, String> menuInfo = this.mArduinoSupportedBoards.get(MENU);
	if (menuInfo == null) {
	    return new String[0];
	}
	for (Entry<String, String> e2 : menuInfo.entrySet()) {
	    if (e2.getValue().equals(menuLabel))
		menuID = e2.getKey();
	}
	String SearchKey = menuID + DOT + boardID + DOT;
	for (Entry<String, String> e2 : menuInfo.entrySet()) {
	    int numsubkeys = e2.getKey().split("\\.").length; //$NON-NLS-1$
	    boolean startOk = e2.getKey().startsWith(SearchKey);
	    if ((numsubkeys == 3) && (startOk))
		ret.add(e2.getValue());
	}
	// from Arduino IDE 1.5.4 menu is subset of the board. The previous code will not return a result
	Map<String, String> boardInfo = this.mArduinoSupportedBoards.get(boardID);
	if (boardInfo != null) {
	    SearchKey = MENU + DOT + menuID + DOT;
	    for (Entry<String, String> e2 : boardInfo.entrySet()) {
		int numsubkeys = e2.getKey().split("\\.").length; //$NON-NLS-1$
		boolean startOk = e2.getKey().startsWith(SearchKey);
		if ((numsubkeys == 3) && (startOk))
		    ret.add(e2.getValue());
	    }
	}
	return ret.toArray(new String[ret.size()]);
    }

    /**
     * GetArduinoBoards returns all the boards that are in the currently loaded board.txt file.
     * 
     * @return an empty list if no board file is loaded. In all other cases it returns the list of oards found in the file
     * @author Trump
     * 
     */
    public String[] GetArduinoBoards() {
	if (this.mLastLoadedBoardsFile.equals(ArduinoConst.EMPTY_STRING)) {
	    String[] sBoards = new String[0];
	    return sBoards;
	}
	Set<String> mBoards = new HashSet<>();
	for (String s : this.mArduinoSupportedBoards.keySet()) {
	    if (s != null) {
		String theboardName = this.mArduinoSupportedBoards.get(s).get(ArduinoConst.BoardNameKeyTAG);
		if (theboardName != null) {
		    mBoards.add(theboardName);
		}
	    }
	}
	String[] sBoards = new String[mBoards.size()];
	mBoards.toArray(sBoards);
	Arrays.sort(sBoards);
	return sBoards;
    }

    /**
     * Load the board.txt file provided.
     * 
     * @param BoardsFile
     *            the full name to the boards.txt file
     * @return true when the action was successful. else false.
     * @author jan
     */
    public boolean LoadBoardsFile(File boardsFile) {

	if ((this.mLastLoadedBoardsFile != null) && (this.mLastLoadedBoardsFile.equals(boardsFile)))
	    return true; // do nothing when value didn't change
	this.mLastLoadedBoardsFile = boardsFile;
	return LoadBoardsFile();
    }

    public boolean exists() {
	return this.mLastLoadedBoardsFile.exists();
    }

    /**
     * Load loads the board.txt file based on the arduino path.
     * 
     * @param NewArduinoPath
     *            the full path to the file board.txt (including board.txt)
     * @return true when the action was successful. else false.
     */
    private boolean LoadBoardsFile() {
	// If the file doesn't exist ignore it.
	if (!this.mLastLoadedBoardsFile.exists())
	    return false;

	this.mArduinoSupportedBoards.clear();

	try {
	    Map<String, String> boardPreferences = new LinkedHashMap<>();
	    load(this.mLastLoadedBoardsFile, boardPreferences);
	    for (Object k : boardPreferences.keySet()) {
		String key = (String) k;
		String board = key.substring(0, key.indexOf('.'));
		if (!this.mArduinoSupportedBoards.containsKey(board))
		    this.mArduinoSupportedBoards.put(board, new HashMap<String, String>());
		(this.mArduinoSupportedBoards.get(board)).put(key.substring(key.indexOf('.') + 1), boardPreferences.get(key));
	    }

	} catch (Exception e) {
	    Common.log(new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID,
		    Messages.ArduinoBoards_Failed_to_read_boards + this.mLastLoadedBoardsFile.getName(), e));
	}
	return true;
    }

    /**
     * @author Trump
     */
    public String getBoardIDFromName(String boardName) {
	for (Entry<String, Map<String, String>> entry : this.mArduinoSupportedBoards.entrySet()) {
	    for (Entry<String, String> e2 : entry.getValue().entrySet()) {
		if (e2.getValue().equals(boardName))
		    return entry.getKey();
	    }

	}
	return null;
    }

    /**
     * Loads the input stream to a Map, ignoring any lines that start with a #
     * <p>
     * Taken from preferences.java in the arduino source
     * 
     * @param input
     *            the input stream to load
     * @param table
     *            the Map to load the values to
     * @throws IOException
     *             when something goes wrong??
     */
    static public void load(File inputFile, Map<String, String> table) throws IOException {
	try (FileInputStream input = new FileInputStream(inputFile);) {
	    String[] lines = loadStrings(input); // Reads as UTF-8
	    for (String line : lines) {
		if ((line.length() == 0) || (line.charAt(0) == '#'))
		    continue;

		// this won't properly handle = signs being in the text
		int equals = line.indexOf('=');
		if (equals != -1) {
		    String key = line.substring(0, equals).trim();
		    String value = line.substring(equals + 1).trim();
		    table.put(key, value);
		}
	    }
	    input.close();
	}
    }

    // Taken from PApplet.java
    /**
     * Loads an input stream into an array of strings representing each line of the input stream
     * 
     * @param input
     *            the input stream to load
     * @return the array of strings representing the inputStream
     */
    static public String[] loadStrings(InputStream input) {
	try {
	    String lines[] = new String[100];
	    int lineCount = 0;
	    try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));) { //$NON-NLS-1$

		String line = null;
		while ((line = reader.readLine()) != null) {
		    if (lineCount == lines.length) {
			String temp[] = new String[lineCount << 1];
			System.arraycopy(lines, 0, temp, 0, lineCount);
			lines = temp;
		    }
		    lines[lineCount++] = line;
		}
		reader.close();
	    }

	    if (lineCount == lines.length) {
		return lines;
	    }

	    // resize array to appropriate amount for these lines
	    String output[] = new String[lineCount];
	    System.arraycopy(lines, 0, output, 0, lineCount);
	    return output;

	} catch (IOException e) {
	    IStatus status = new Status(IStatus.WARNING, ArduinoConst.CORE_PLUGIN_ID, "Failed to read stream ", e); //$NON-NLS-1$
	    Common.log(status);
	}
	return null;
    }

    public String getBoardsTxtName() {
	return this.mLastLoadedBoardsFile.getAbsolutePath();
    }

    public String getMenuNameFromID(String menuID) {
	Map<String, String> menuSectionMap = getSection(MENU);
	for (Entry<String, String> curOption : menuSectionMap.entrySet()) {
	    if (curOption.getKey().equals(menuID)) {
		return curOption.getValue();
	    }
	}
	return MENU + " ID " + menuID + Messages.ArduinoBoards_not_found; //$NON-NLS-1$
    }

    public String getMenuIDFromName(String menuName) {
	Map<String, String> menuSectionMap = getSection(MENU);
	for (Entry<String, String> curOption : menuSectionMap.entrySet()) {
	    if (curOption.getValue().equals(menuName)) {
		return curOption.getKey();
	    }
	}
	return MENU + Messages.ArduinoBoards_name + menuName + Messages.ArduinoBoards_not_found;
    }

    public String getMenuItemIDFromName(String boardID, String menuID, String menuItemName) {
	// look in the pre 1.5.4 way "menu".menuid.boardid.menuitemid=name
	Map<String, String> menuSectionMap = getSection(MENU);
	for (Entry<String, String> curOption : menuSectionMap.entrySet()) {
	    if (curOption.getValue().equals(menuItemName)) {
		String[] keySplit = curOption.getKey().split("\\."); //$NON-NLS-1$
		if (keySplit.length == 3 && keySplit[0].equals(menuID) && keySplit[1].equals(boardID))
		    return keySplit[2];
	    }
	}
	// nothing found so look in the post 1.5.4 way boardid."menu".menuid.menuitemid=name
	// TODO implement in 1.5.4 case
	return "getMenuItemIDFromName not yet implemented in 1.5.4 way"; //$NON-NLS-1$
    }

    public String getMenuItemNameFromID(String boardID, String menuID, String menuItemID) {
	// look in the pre 1.5.4 way "menu".menuid.boardid.menuitemid=name
	Map<String, String> menuSectionMap = getSection(MENU);
	String lookupValue = menuID + DOT + boardID + DOT + menuItemID;
	for (Entry<String, String> curOption : menuSectionMap.entrySet()) {
	    if (curOption.getKey().equalsIgnoreCase(lookupValue))
		return curOption.getValue();
	}
	// nothing found so look in the post 1.5.4 way boardid."menu".menuid.menuitemid=name
	Map<String, String> BoardIDSectionMap = getSection(boardID);
	String loopupValue = MENU + DOT + menuID + DOT + menuItemID;
	for (Entry<String, String> curOption : BoardIDSectionMap.entrySet()) {
	    if (curOption.getKey().equalsIgnoreCase(loopupValue))
		return curOption.getValue();
	}
	// TODO implement 1.5.4 way
	return Messages.ArduinoBoards_Get_menu_item_name_from_id_did_not_find + boardID + ' ' + menuID + ' ' + menuItemID;
    }

}
