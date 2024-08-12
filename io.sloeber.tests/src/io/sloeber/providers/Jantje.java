package io.sloeber.providers;


import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class Jantje extends MCUBoard {

    private static final String provider = "Jantje";
    private static final String packageName = "Jantje";
    private static final String localDebugArchitectureName = "pc";
    private static final String jsonFileName = "package_jantje_index.json";
    // the below json url is need as esp8266 is a referenced platform
    public static final String additionalJsonURL = "https://arduino.esp8266.com/stable/package_esp8266com_index.json";


    public static  List<MCUBoard> getAllBoards() {
        return getAllBoards(provider, uno());
    }

    @Override
    public MCUBoard createMCUBoard(BoardDescription boardDescriptor) {
        return new Jantje(boardDescriptor);

    }

    public Jantje(BoardDescription boardDesc) {
        myBoardDescriptor = boardDesc;
        setAttributes();
    }

    public static MCUBoard uno() {
        return new Jantje( Arduino.unoID);
    }

    public Jantje(String boardName) {
        Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        options.put("type", "debug");
        myBoardDescriptor = BoardsManager.getBoardDescription(jsonFileName, packageName, localDebugArchitectureName,
                boardName, options);
        if (myBoardDescriptor == null) {
            fail(boardName + " Board not found");
        }
        setAttributes();
    }

    public static void installLatestLocalDebugBoards() {
        BoardsManager.installLatestPlatform(jsonFileName, provider, localDebugArchitectureName);
    }

    @Override
    protected void setAttributes() {
       // boardID = myBoardDescriptor.getBoardID();
        //Arduino.sharedsetAttributes(boardID, myAttributes);
        setUploadPort("none");
        myAttributes.myArchitecture=myBoardDescriptor.getArchitecture();

    }

}
