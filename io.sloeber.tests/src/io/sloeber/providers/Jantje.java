package io.sloeber.providers;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.sloeber.core.Example;
import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class Jantje extends MCUBoard {

    private static final String provider = "Jantje";
    private static final String packageName = "Jantje";
    private static final String localDebugArchitectureName = "pc";
    private static final String jsonFileName = "package_jantje_index.json";
    // the below json url is need as esp8266 is a referenced platform
    public static final String additionalJsonURL = "http://arduino.esp8266.com/stable/package_esp8266com_index.json";

    public Jantje(String boardName) {
        Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        options.put("type", "debug");
        this.myBoardDescriptor = BoardsManager.getBoardDescription(jsonFileName, packageName,
                localDebugArchitectureName, boardName, options);
        if (this.myBoardDescriptor == null) {
            fail(boardName + " Board not found");
        }
        this.myBoardDescriptor.setUploadPort("none");

        myAttributes.serial = !Arduino.doesNotSupportSerialList().contains(boardName);
        myAttributes.serial1 = Arduino.supportSerial1List().contains(boardName);
        myAttributes.keyboard = Arduino.supportKeyboardList().contains(boardName);
    }

    @Override
    public boolean isExampleSupported(Example example) {
        LinkedList<String> notSupportedExamples = new LinkedList<>();
        notSupportedExamples.add("Example/09.USB/Keyboard/KeyboardLogout");
        notSupportedExamples.add("Example/09.USB/Keyboard/KeyboardMessage");
        notSupportedExamples.add("Example/09.USB/Keyboard/KeyboardReprogram");
        notSupportedExamples.add("Example/09.USB/Keyboard/KeyboardSerial");
        notSupportedExamples.add("Example/09.USB/KeyboardAndMouseControl");
        notSupportedExamples.add("Example/09.USB/Mouse/ButtonMouseControl");
        notSupportedExamples.add("Example/09.USB/Mouse/JoystickMouseControl");
        notSupportedExamples.add("Example/10.StarterKit_BasicKit/p13_TouchSensorLamp");
        if (notSupportedExamples.contains(example.getFQN())) {
            return false;
        }
        return super.isExampleSupported(example);
    }

    public static List<MCUBoard> getAllBoards() {
        return getAllBoards(provider, Arduino.uno());
    }

    @Override
    public MCUBoard createMCUBoard(BoardDescription boardDescriptor) {
        return new Arduino(boardDescriptor);

    }

    public Jantje(BoardDescription boardDescriptor) {
        myBoardDescriptor = boardDescriptor;
        myBoardDescriptor.setUploadPort("none");
        String boardID = myBoardDescriptor.getBoardID();
        myAttributes.serial = !Arduino.doesNotSupportSerialList().contains(boardID);
        myAttributes.serial1 = Arduino.supportSerial1List().contains(boardID);
        myAttributes.keyboard = Arduino.supportKeyboardList().contains(boardID);
    }

    public static void installLatestLocalDebugBoards() {
        BoardsManager.installLatestPlatform(jsonFileName, provider, localDebugArchitectureName);
    }

}
