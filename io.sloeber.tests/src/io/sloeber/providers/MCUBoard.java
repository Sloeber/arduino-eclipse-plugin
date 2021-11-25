package io.sloeber.providers;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.sloeber.core.BoardAttributes;
import io.sloeber.core.Example;
import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.Json.ArduinoPackage;
import io.sloeber.core.api.Json.ArduinoPlatform;
import io.sloeber.core.api.Json.ArduinoPlatformVersion;

@SuppressWarnings("nls")
public abstract class MCUBoard {

    protected BoardDescription myBoardDescriptor = null;
    public BoardAttributes myAttributes = new BoardAttributes();
    public String mySerialPort = "Serial";

    public abstract MCUBoard createMCUBoard(BoardDescription boardDesc);

    protected abstract void setAttributes();

    public static List<MCUBoard> getAllBoards(String provider, MCUBoard creator) {
        List<MCUBoard> ret = new LinkedList<>();
        ArduinoPackage arduinoPkg = BoardsManager.getPackageByProvider(provider);
        for (ArduinoPlatform curPlatform : arduinoPkg.getPlatforms()) {
            ArduinoPlatformVersion curPlatformVersion = curPlatform.getNewestInstalled();
            if (curPlatformVersion != null) {
                List<BoardDescription> boardDescriptions = BoardDescription
                        .makeBoardDescriptors(curPlatformVersion.getBoardsFile());
                for (BoardDescription curBoardDesc : boardDescriptions) {
                    ret.add(creator.createMCUBoard(curBoardDesc));
                }
            }
        }
        return ret;

    }

    public BoardDescription getBoardDescriptor() {
        return myBoardDescriptor;
    }

    public boolean isExampleSupported(Example example) {
        if (myBoardDescriptor == null) {
            return false;
        }

        /*
         * There is one know Teensy example that does not
         * run on all teensy boards
         */
        if ("Teensy".equalsIgnoreCase(getID())) {
            if (example.getFQN().contains("Teensy/USB_Mouse/Buttons")) {
                String boardID = myBoardDescriptor.getBoardID();
                if ("teensypp2".equals(boardID) || "teensy2".equals(boardID)) {
                    return false;
                }
            }
        }
        myAttributes.boardName = myBoardDescriptor.getBoardID();
        return myAttributes.compatibleWithExampleRequirements(example.getRequiredBoardAttributes());
    }

    /**
     * give the ID of the board as it appears in boards.txt
     * 
     * @return the ID or null
     */
    public String getID() {
        if (myBoardDescriptor == null) {
            return null;
        }
        return myBoardDescriptor.getBoardID();
    }

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