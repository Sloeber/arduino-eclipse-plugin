package io.sloeber.core.txt;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.sloeber.core.Messages;

public class BoardTxtFile extends TxtFile {

    private static final String MENUITEMNAME = Messages.MENUITEMNAME;
    public static final String MENUITEMID = Messages.MENUITEMID;
    public static final String MENUID = Messages.MENUID;
    public static final String BOARDID = Messages.BOARDID;
    private File providedTxtFile = null;

    public BoardTxtFile(File boardsFile) {
        super(getActualTxtFile(boardsFile));
        providedTxtFile = boardsFile;
    }

    private static File getActualTxtFile(File boardsFile) {
        if (BOARDS_FILE_NAME.equals(boardsFile.getName())) {
            return WorkAround.MakeBoardsSloeberTxt(boardsFile);
        }
        return boardsFile;
    }

    public String getMenuIDFromMenuName(String menuName) {
        Map<String, String> menuSectionMap = getSection(MENU);
        if (menuSectionMap != null) {
            for (Entry<String, String> curOption : menuSectionMap.entrySet()) {
                if (curOption.getValue().equals(menuName)) {
                    return curOption.getKey();
                }
            }
        }
        return Messages.Boards_menu_name_not_found.replace(Messages.NAME, menuName);
    }

    public String getMenuItemIDFromMenuItemName(String boardID, String menuID, String menuItemName) {

        // boardid."menu".menuid.menuitemid=name
        KeyValueTree BoardIDSectionMap = myData.getChild(boardID + DOT + MENU + DOT + menuID);
        for (KeyValueTree curOption : BoardIDSectionMap.getChildren().values()) {
            if (menuItemName.equals(curOption.getValue())) {
                return curOption.getKey();
            }
        }
        return Messages.Boards_Get_menu_item_id_from_name_failed.replace(MENUITEMNAME, menuItemName)
                .replace(MENUID, menuID).replace(BOARDID, boardID);
    }

    /**
     * Get all the acceptable values for a option for a board The outcome of this
     * method can be used to fill a
     *
     * @param menu
     *            the id of a menu not the name
     * @param boardID
     *            the id of a board not the name
     * @return The nice names that are the possible selections
     */
    public String[] getMenuItemNamesFromMenuID(String menuID, String boardID) {
        HashSet<String> ret = new HashSet<>();

        KeyValueTree boardMenuInfo = myData.getChild(boardID + DOT + MENU + DOT + menuID);
        for (KeyValueTree menuData : boardMenuInfo.getChildren().values()) {
            if (null != menuData.getValue()) {
                ret.add(menuData.getValue());
            }
        }
        return ret.toArray(new String[ret.size()]);
    }

    /**
     * Get all the acceptable values for a option for a board The outcome of this
     * method can be used to fill the menu options combobox
     * 
     * The result is ordered because the first item is the default
     *
     * @param menu
     *            the id of a menu not the name
     * @param boardID
     *            the id of a board not the name
     * @return The IDs that are the possible selections
     */
    public ArrayList<String> getMenuItemIDsFromMenuID(String menuID, String boardID) {
        ArrayList<String> ret = new ArrayList<>();

        KeyValueTree boardMenuInfo = myData.getChild(boardID + DOT + MENU + DOT + menuID);
        for (KeyValueTree menuData : boardMenuInfo.getChildren().values()) {
            ret.add(menuData.getKey());
        }
        return ret;
    }

    public String getMenuItemNameFromMenuItemID(String boardID, String menuID, String menuItemID) {
        // // look in the pre 1.5.4 way "menu".menuid.boardid.menuitemid=name
        // Map<String, String> menuSectionMap = getSection(MENU);
        // String lookupValue = menuID + DOT + boardID + DOT + menuItemID;
        // for (Entry<String, String> curOption : menuSectionMap.entrySet()) {
        // if (curOption.getKey().equalsIgnoreCase(lookupValue))
        // return curOption.getValue();
        // }
        // // nothing found so look in the post 1.5.4 way
        // // boardid."menu".menuid.menuitemid=name
        String lookupValue = boardID + DOT + MENU + DOT + menuID + DOT + menuItemID;
        String ret = myData.getValue(lookupValue);
        if (ret != null) {
            return ret;
        }
        // Map<String, String> BoardIDSectionMap = getSection(boardID);
        //
        // for (Entry<String, String> curOption : BoardIDSectionMap.entrySet()) {
        // if (curOption.getKey().equalsIgnoreCase(lookupValue))
        // return curOption.getValue();
        // }
        return Messages.Boards_Get_menu_item_name_from_id_did_not_find.replace(MENUITEMID, menuItemID)
                .replace(MENUID, menuID).replace(BOARDID, boardID);
    }

    public String getMenuNameFromID(String menuID) {
        Map<String, String> menuSectionMap = getSection(MENU);
        if (menuSectionMap != null) {
            for (Entry<String, String> curOption : menuSectionMap.entrySet()) {
                if (curOption.getKey().equalsIgnoreCase(menuID)) {
                    return curOption.getValue();
                }
            }
        }
        return Messages.Boards_menu_ID_not_found.replace(ID, menuID);
    }

    /**
     * Get all the menu option names in the .txt file
     *
     * @return a list of all the menu option names
     */
    public Set<String> getMenuNames() {
        HashSet<String> ret = new HashSet<>();

        KeyValueTree menuInfo = myData.getChild(MENU);
        for (KeyValueTree menuData : menuInfo.getChildren().values()) {
            if (null != menuData.getValue()) {
                ret.add(menuData.getValue());
            }
        }
        return ret;
    }

    /**
     * Get all the menu option names in the .txt file
     *
     * @return a list of all the menu option key value pairs
     */
    public Map<String, String> getMenus() {
        return getSection(MENU);
    }

    @Override
    public File getTxtFile() {
        return providedTxtFile;
    }

    /**
     * this is public String[] getAllSectionNames (String[] toaddNames) with a empty
     * toaddnames
     *
     * @return
     */
    public String[] getAllSectionNames() {
        return getAllSectionNames(new String[0]);
    }

    /**
     * getAllSectionNames returns all the "names" that are in the currently loaded
     * *.txt file. The toaddNames are added to the end result toaddNames should be a
     * string array and can not be null
     *
     * For a boards.txt file that means all the board names. For a programmers.txt
     * file that means all the programmers
     *
     * @return an empty list if no board file is loaded. In all other cases it
     *         returns the list of boards found in the file
     * @author Trump
     *
     */
    public String[] getAllSectionNames(String[] toaddNames) {

        HashSet<String> allNames = new HashSet<>();
        for (String curName : toaddNames) {
            allNames.add(curName);
        }
        for (String curKey : myData.getChildren().keySet()) {
            if ((curKey != null) && (!curKey.isEmpty())) {
                String theName = myData.getValue(curKey + DOT + NAME);
                if ((theName != null) && (!theName.isEmpty())) {
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
     * Get all the key value pairs that need to be added to the environment
     * variables
     * 
     * prefix something to add at the beginning of each key name
     */
    public Map<String, String> getAllBoardEnvironVars(String boardID) {
        return myData.getChild(boardID).toKeyValues(ERASE_START, false);
    }
}
