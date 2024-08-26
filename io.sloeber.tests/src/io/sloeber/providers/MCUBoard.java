package io.sloeber.providers;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.sloeber.arduinoFramework.api.BoardDescription;
import io.sloeber.arduinoFramework.api.BoardsManager;
import io.sloeber.arduinoFramework.api.IArduinoPackage;
import io.sloeber.arduinoFramework.api.IArduinoPlatform;
import io.sloeber.arduinoFramework.api.IArduinoPlatformVersion;
import io.sloeber.core.AttributesBoard;
import io.sloeber.core.Example;

@SuppressWarnings("nls")
public abstract class MCUBoard {

    protected BoardDescription myBoardDescriptor = null;
    public AttributesBoard myAttributes = new AttributesBoard();
    public String mySerialPort = "Serial";

    public abstract MCUBoard createMCUBoard(BoardDescription boardDesc);

    protected abstract void setAttributes();

    public static List<MCUBoard> getAllBoards(String provider, MCUBoard board) {
    	return getAllBoards( provider, null ,board) ;
//        List<MCUBoard> ret = new LinkedList<>();
//        ret.add(board);//make the board provided the first in the list
//        ArduinoPackage arduinoPkg = BoardsManager.getPackageByProvider(provider);
//        for (ArduinoPlatform curPlatform : arduinoPkg.getPlatforms()) {
//            ArduinoPlatformVersion curPlatformVersion = curPlatform.getNewestInstalled();
//            if (curPlatformVersion != null) {
//                List<BoardDescription> boardDescriptions = BoardDescription
//                        .makeBoardDescriptors(curPlatformVersion.getBoardsFile());
//                for (BoardDescription curBoardDesc : boardDescriptions) {
//                    MCUBoard curBoard = board.createMCUBoard(curBoardDesc);
//                    curBoard.myAttributes.boardID = curBoardDesc.getBoardID();
//                    ret.add(curBoard);
//                }
//            }
//        }
//        return ret;
    }

    public static List<MCUBoard> getAllBoards(String provider, String architecture, MCUBoard board) {
        List<MCUBoard> ret = new LinkedList<>();
        ret.add(board);//make the board provided the first in the list
        IArduinoPackage arduinoPkg = BoardsManager.getPackageByProvider(provider);
        for (IArduinoPlatform curPlatform : arduinoPkg.getPlatforms()) {
        	if(architecture!=null && !architecture.equals( curPlatform.getArchitecture())) {
        		continue;
        	}
            IArduinoPlatformVersion curPlatformVersion = curPlatform.getNewestInstalled();
            if (curPlatformVersion != null) {
                List<BoardDescription> boardDescriptions = BoardDescription
                        .makeBoardDescriptors(curPlatformVersion.getBoardsFile());
                for (BoardDescription curBoardDesc : boardDescriptions) {
                    MCUBoard curBoard = board.createMCUBoard(curBoardDesc);
                    curBoard.myAttributes.boardID = curBoardDesc.getBoardID();
                    ret.add(curBoard);
                }
            }
        }
        return ret;
    }

    public BoardDescription getBoardDescriptor() {
        return myBoardDescriptor;
    }



    /**
     * give the name of the board as it appears in boards.txt
     *
     * @return the name of the board as shown in the gui
     */
    public String getID() {
        if (myBoardDescriptor == null) {
            return null;
        }
        return myBoardDescriptor.getBoardID();
    }

    /**
	 * @param example not used here but used in overloaded methods
	 */
    @SuppressWarnings({ "static-method" })
    public Map<String, String> getBoardOptions(Example example) {
        Map<String, String> ret = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        return ret;
    }

    /**
     * give the name of the board as it appears in boards.txt
     *
     * @return the name of the board as shown in the gui or null
     */
    public String getName() {
        if (myBoardDescriptor == null) {
            return null;
        }
        return myBoardDescriptor.getBoardName();
    }

    public MCUBoard setUploadPort(String uploadPort) {
        myBoardDescriptor.setUploadPort(uploadPort);
        return this;

    }

}