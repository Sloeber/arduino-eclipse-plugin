package io.sloeber.core.tools;

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
import java.util.TreeMap;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

import io.sloeber.common.Common;
import io.sloeber.common.Const;

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
	private File mLastLoadedTxtFile = null;
	private static final String DOT = Const.DOT;

	private static final String TXT_NAME_KEY_TAG = "name"; //$NON-NLS-1$
	private static final String MENU = Const.MENU;
	private static final String MENUITEMID = "\\$\\{MENUITEMID}"; //$NON-NLS-1$
	private static final String MENUID = "\\$\\{MENUID}"; //$NON-NLS-1$
	private static final String BOARDID = "\\$\\{BOARDID}"; //$NON-NLS-1$
	private static final String MENUITEMNAME = "\\$\\{MENUITEMNAME}";//$NON-NLS-1$
	Map<String, String> settings = null;
	private LinkedHashMap<String, Map<String, String>> fileContent = new LinkedHashMap<>(); // all
	// the
	// data

	public TxtFile(File boardsFileName) {
		LoadBoardsFile(boardsFileName);
	}

	public TxtFile() {
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
		return this.fileContent.get(SectionKey);
	}

	/**
	 * Get all the menu option names in the .txt file
	 *
	 * @return a list of all the menu option names
	 */
	public Set<String> getMenuNames() {
		HashSet<String> ret = new HashSet<>();
		for (Entry<String, Map<String, String>> entry : this.fileContent.entrySet()) {
			if (entry.getKey().equals(MENU)) {
				for (Entry<String, String> e2 : entry.getValue().entrySet()) {
					if (!e2.getKey().contains(Const.DOT)) {
						if (!ret.contains(e2.getValue())) {
							ret.add(e2.getValue());
						}
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Get all the menu option names in the .txt file
	 *
	 * @return a list of all the menu option key value pairs
	 */
	public TreeMap<String, String> getMenus() {
		TreeMap<String, String> ret = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		for (Entry<String, Map<String, String>> entry : this.fileContent.entrySet()) {
			if (entry.getKey().equals(MENU)) {
				for (Entry<String, String> e2 : entry.getValue().entrySet()) {
					if (!e2.getKey().contains(Const.DOT)) {
						ret.put(e2.getKey().toUpperCase(), e2.getValue());
					}
				}
			}
		}
		return ret;
	}

	/**
	 * Get all the acceptable values for a option for a board The outcome of
	 * this method can be used to fill a
	 *
	 * @param menu
	 *            the id of a menu not the name
	 * @param boardID
	 *            the id of a board not the name
	 * @return The nice names that are the possible selections
	 */
	public String[] getMenuItemNamesFromMenuID(String menuID, String boardID) {
		HashSet<String> ret = new HashSet<>();
		Map<String, String> menuInfo = this.fileContent.get(MENU);
		if (menuInfo == null) {
			return new String[0];
		}

		String SearchKey = menuID + DOT + boardID + DOT;
		SearchKey = SearchKey.toUpperCase();
		for (Entry<String, String> e2 : menuInfo.entrySet()) {
			int numsubkeys = e2.getKey().split("\\.").length; //$NON-NLS-1$
			boolean startOk = e2.getKey().toUpperCase().startsWith(SearchKey);
			if ((numsubkeys == 3) && (startOk))
				ret.add(e2.getValue());
		}
		// from Arduino IDE 1.5.4 menu is subset of the board. The previous code
		// will not return a result
		Map<String, String> boardInfo = this.fileContent.get(boardID);
		if (boardInfo != null) {
			SearchKey = MENU + DOT + menuID + DOT;
			SearchKey = SearchKey.toUpperCase();
			for (Entry<String, String> e2 : boardInfo.entrySet()) {
				int numsubkeys = e2.getKey().split("\\.").length; //$NON-NLS-1$
				boolean startOk = e2.getKey().toUpperCase().startsWith(SearchKey);
				if ((numsubkeys == 3) && (startOk))
					ret.add(e2.getValue());
			}
		}
		return ret.toArray(new String[ret.size()]);
	}

	/**
	 * this is public String[] getAllNames(String[] toaddNames) with a empty
	 * toaddnames
	 *
	 * @return
	 */
	public String[] getAllNames() {
		return getAllNames(new String[0]);
	}

	/**
	 * getAllNames returns all the "names" that are in the currently loaded
	 * *.txt file. The toaddNames are added to the end result toaddNames should
	 * be a string array and can not be null
	 *
	 * For a boards.txt file that means all the board names. For a
	 * programmers.txt file that means all the programmers For platform.txt the
	 * outcome is not defined
	 *
	 * @return an empty list if no board file is loaded. In all other cases it
	 *         returns the list of boards found in the file
	 * @author Trump
	 *
	 */
	public String[] getAllNames(String[] toaddNames) {
		if (this.mLastLoadedTxtFile.equals(Const.EMPTY_STRING)) {
			return toaddNames;
		}
		HashSet<String> allNames = new HashSet<>();
		for (String curName : toaddNames) {
			allNames.add(curName);
		}
		for (String s : this.fileContent.keySet()) {
			if (s != null) {
				String theName = this.fileContent.get(s).get(TXT_NAME_KEY_TAG);
				if (theName != null) {
					allNames.add(theName);
				}
			}
		}
		String[] sBoards = new String[allNames.size()];
		allNames.toArray(sBoards);
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

		if ((this.mLastLoadedTxtFile != null) && (this.mLastLoadedTxtFile.equals(boardsFile)))
			return true; // do nothing when value didn't change
		this.mLastLoadedTxtFile = boardsFile;
		return LoadBoardsFile();
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
		if (!this.mLastLoadedTxtFile.exists())
			return false;

		this.fileContent.clear();

		try {
			Map<String, String> boardPreferences = new LinkedHashMap<>();
			load(this.mLastLoadedTxtFile, boardPreferences);
			for (Object k : boardPreferences.keySet()) {
				String key = (String) k;
				String board = key.substring(0, key.indexOf('.'));
				if (!this.fileContent.containsKey(board))
					this.fileContent.put(board, new HashMap<String, String>());
				(this.fileContent.get(board)).put(key.substring(key.indexOf('.') + 1), boardPreferences.get(key));
			}

		} catch (Exception e) {
			Common.log(new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID,
					Messages.Boards_Failed_to_read_boards + this.mLastLoadedTxtFile.getName(), e));
		}
		return true;
	}

	/**
	 * Given a nice name look for the ID The assumption is that the txt file
	 * contains a line like ID.name=[nice name] Given this this method returns
	 * ID when given [nice name]
	 */
	public String getBoardIDFromBoardName(String name) {
		if ((name == null) || name.isEmpty()) {
			return null;
		}
		for (Entry<String, Map<String, String>> entry : this.fileContent.entrySet()) {
			for (Entry<String, String> e2 : entry.getValue().entrySet()) {
				if (e2.getValue().equals(name))
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

	/**
	 * Loads an input stream into an array of strings representing each line of
	 * the input stream
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
			IStatus status = new Status(IStatus.WARNING, Const.CORE_PLUGIN_ID, "Failed to read stream ", e); //$NON-NLS-1$
			Common.log(status);
		}
		return null;
	}

	/**
	 *
	 * @return the file name that is currently loaded
	 */
	public File getTxtFile() {
		return this.mLastLoadedTxtFile;
	}

	public String getMenuNameFromID(String menuID) {
		Map<String, String> menuSectionMap = getSection(MENU);
		for (Entry<String, String> curOption : menuSectionMap.entrySet()) {
			if (curOption.getKey().equalsIgnoreCase(menuID)) {
				return curOption.getValue();
			}
		}
		return MENU + " ID " + menuID + Messages.Boards_not_found; //$NON-NLS-1$
	}

	public String getMenuItemNameFromMenuItemID(String boardID, String menuID, String menuItemID) {
		// look in the pre 1.5.4 way "menu".menuid.boardid.menuitemid=name
		Map<String, String> menuSectionMap = getSection(MENU);
		String lookupValue = menuID + DOT + boardID + DOT + menuItemID;
		for (Entry<String, String> curOption : menuSectionMap.entrySet()) {
			if (curOption.getKey().equalsIgnoreCase(lookupValue))
				return curOption.getValue();
		}
		// nothing found so look in the post 1.5.4 way
		// boardid."menu".menuid.menuitemid=name
		Map<String, String> BoardIDSectionMap = getSection(boardID);
		lookupValue = MENU + DOT + menuID + DOT + menuItemID;
		for (Entry<String, String> curOption : BoardIDSectionMap.entrySet()) {
			if (curOption.getKey().equalsIgnoreCase(lookupValue))
				return curOption.getValue();
		}
		return Messages.Boards_Get_menu_item_name_from_id_did_not_find.replaceAll(MENUITEMID, menuItemID)
				.replaceAll(MENUID, menuID).replaceAll(BOARDID, boardID);
	}

	public String getNameFromID(String myBoardID) {
		Map<String, String> boardSection = getSection(myBoardID);
		if (boardSection == null) {
			return Const.EMPTY_STRING;
		}
		return boardSection.get("name"); //$NON-NLS-1$
	}

	/*
	 * Returns the package name based on the boardsfile name Caters for the
	 * packages (with version number and for the old way
	 */
	public String getPackage() {
		IPath platformFile = new Path(this.mLastLoadedTxtFile.toString().trim());
		String architecture = platformFile.removeLastSegments(1).lastSegment();
		if (architecture.contains(Const.DOT)) { // This is a version number so
			// package
			return platformFile.removeLastSegments(4).lastSegment();
		}
		return platformFile.removeLastSegments(2).lastSegment();
	}

	/*
	 * Returns the architecture based on the platform file name Caters for the
	 * packages (with version number and for the old way
	 */
	public String getArchitecture() {

		IPath platformFile = new Path(this.mLastLoadedTxtFile.toString().trim());
		String architecture = platformFile.removeLastSegments(1).lastSegment();
		if (architecture.contains(Const.DOT)) { // This is a version number so
			// package
			architecture = platformFile.removeLastSegments(2).lastSegment();
		}
		return architecture;
	}

	public String getMenuIDFromMenuName(String menuName) {
		Map<String, String> menuSectionMap = getSection(MENU);
		if (menuSectionMap != null) {
			for (Entry<String, String> curOption : menuSectionMap.entrySet()) {
				if (curOption.getValue().equals(menuName)) {
					return curOption.getKey().toUpperCase();
				}
			}
		}
		return MENU + Messages.Boards_name + menuName + Messages.Boards_not_found;
	}

	public String getMenuItemIDFromMenuItemName(String boardID, String menuID, String menuItemName) {
		// look in the pre 1.5.4 way "menu".menuid.boardid.menuitemid=name
		Map<String, String> menuSectionMap = getSection(MENU);
		String lookupValue = menuID + DOT + boardID + DOT;
		lookupValue = lookupValue.toUpperCase();
		for (Entry<String, String> curOption : menuSectionMap.entrySet()) {
			if (curOption.getValue().equals(menuItemName)) {
				if (curOption.getKey().toUpperCase().startsWith(lookupValue))
					return curOption.getKey().substring(lookupValue.length()).toUpperCase();
			}
		}
		// nothing found so look in the post 1.5.4 way
		// boardid."menu".menuid.menuitemid=name
		Map<String, String> BoardIDSectionMap = getSection(boardID);
		lookupValue = MENU + DOT + menuID + DOT;
		lookupValue = lookupValue.toUpperCase();
		for (Entry<String, String> curOption : BoardIDSectionMap.entrySet()) {
			if (curOption.getValue().equals(menuItemName)) {
				if (curOption.getKey().toUpperCase().startsWith(lookupValue))
					return curOption.getKey().substring(lookupValue.length()).toUpperCase();
			}
		}
		return Messages.getMenuItemIDFromMenuItemName.replaceAll(MENUITEMNAME, menuItemName).replaceAll(MENUID, menuID)
				.replaceAll(BOARDID, boardID);
	}

}
