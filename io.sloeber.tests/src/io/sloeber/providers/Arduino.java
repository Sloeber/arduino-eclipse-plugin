package io.sloeber.providers;

import static org.junit.Assert.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.Json.ArduinoPackage;
import io.sloeber.core.api.Json.ArduinoPlatform;
import io.sloeber.core.api.Json.ArduinoPlatformVersion;

@SuppressWarnings("nls")
public class Arduino extends MCUBoard {

    private static final String providerArduino = "arduino";
    private static final String providerIntel = "Intel";
    private static final String AVRArchitectureName = "avr";
    private static final String SAMDArchitectureName = "samd";
    private static final String SAMArchitectureName = "sam";
    private static final String NFRArchitectureName = "nrf52";
    private static final String intelCurieArchitectureName = "arc32";
    private static final String jsonFileName = "package_index.json";

    public static final String circuitplay32ID = "circuitplay32u4cat";
    public static final String unoID = "uno";
    public static final String ethernetID = "ethernet";

    public static MCUBoard gemma() {
        MCUBoard ret = new Arduino(providerArduino, AVRArchitectureName, "gemma");
        ret.mySlangName = "gemma";
        return ret;
    }

    public static MCUBoard MegaADK() {
        return new Arduino(providerArduino, AVRArchitectureName, "megaADK");
    }

    public static MCUBoard esplora() {
        return new Arduino(providerArduino, AVRArchitectureName, "esplora");
    }

    public static MCUBoard adafruitnCirquitPlayground() {
        return new Arduino(providerArduino, AVRArchitectureName, circuitplay32ID);
    }

    public static MCUBoard cirquitPlaygroundExpress() {
        return new Arduino(providerArduino, SAMDArchitectureName, "adafruit_circuitplayground_m0");
    }

    public static MCUBoard getAvrBoard(String boardID) {
        return new Arduino(providerArduino, AVRArchitectureName, boardID);
    }

    public static MCUBoard fried2016() {
        return new Arduino(providerArduino, AVRArchitectureName, "LilyPadUSB");
    }

    public static MCUBoard fried2016(String uploadPort) {
        MCUBoard fried = fried2016();
        fried.myBoardDescriptor.setUploadPort(uploadPort);
        return fried;
    }

    public static MCUBoard getMega2560Board() {
        MCUBoard mega = new Arduino(providerArduino, AVRArchitectureName, "mega");
        Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        options.put("cpu", "atmega2560");
        mega.myBoardDescriptor.setOptions(options);
        return mega;
    }

    public static MCUBoard getMega2560Board(String uploadPort) {
        MCUBoard mega = getMega2560Board();
        mega.myBoardDescriptor.setUploadPort(uploadPort);
        return mega;
    }

    public static MCUBoard leonardo() {
        MCUBoard leonardo = new Arduino(providerArduino, AVRArchitectureName, "leonardo");
        return leonardo;
    }

    public static MCUBoard leonardo(String uploadPort) {
        MCUBoard leonardo = leonardo();
        leonardo.myBoardDescriptor.setUploadPort(uploadPort);
        return leonardo;
    }

    public static MCUBoard yun() {
        MCUBoard yun = new Arduino(providerArduino, AVRArchitectureName, "yun");
        return yun;
    }

    public static MCUBoard yun(String uploadPort) {
        MCUBoard yun = yun();
        yun.myBoardDescriptor.setUploadPort(uploadPort);
        return yun;
    }

    public static MCUBoard zeroProgrammingPort() {
        MCUBoard zero = new Arduino(providerArduino, SAMDArchitectureName, "arduino_zero_edbg");
        zero.mySlangName = "zero";
        return zero;
    }

    public static MCUBoard zeroProgrammingPort(String uploadPort) {
        MCUBoard zero = zeroProgrammingPort();
        zero.myBoardDescriptor.setUploadPort(uploadPort);
        return zero;
    }

    public static MCUBoard due() {
        return new Arduino(providerArduino, SAMArchitectureName, "arduino_due_x");
    }

    public static MCUBoard due(String uploadPort) {
        MCUBoard board = due();
        board.myBoardDescriptor.setUploadPort(uploadPort);
        return board;
    }

    public static MCUBoard dueprogramming() {
        return new Arduino(providerArduino, SAMArchitectureName, "arduino_due_x_dbg");
    }

    public static MCUBoard dueprogramming(String uploadPort) {
        MCUBoard board = dueprogramming();
        board.myBoardDescriptor.setUploadPort(uploadPort);
        return board;
    }

    public static MCUBoard mkrfox1200() {
        return new Arduino(providerArduino, SAMDArchitectureName, "mkrfox1200");
    }

    public static MCUBoard primo() {
        return new Arduino(providerArduino, NFRArchitectureName, "primo");
    }

    public static MCUBoard uno() {
        MCUBoard uno = new Arduino(providerArduino, AVRArchitectureName, unoID);
        uno.mySlangName = "uno";
        return uno;
    }

    public static MCUBoard ethernet() {
        MCUBoard uno = new Arduino(providerArduino, AVRArchitectureName, ethernetID);
        return uno;
    }

    public static MCUBoard uno(String uploadPort) {
        MCUBoard uno = uno();
        uno.myBoardDescriptor.setUploadPort(uploadPort);
        return uno;
    }

    public static MCUBoard arduino_101() {
        MCUBoard arduino_101 = new Arduino(providerIntel, intelCurieArchitectureName, "arduino_101");
        arduino_101.mySlangName = "101";
        return arduino_101;
    }

    public static MCUBoard arduino_101(String uploadPort) {
        MCUBoard arduino_101 = arduino_101();
        arduino_101.myBoardDescriptor.setUploadPort(uploadPort);
        return arduino_101;
    }

    private Arduino(String providerName, String architectureName, String boardID) {
        this.myBoardDescriptor = BoardsManager.getBoardDescription(jsonFileName, providerName, architectureName,
                boardID, null);
        if (this.myBoardDescriptor == null) {
            fail(boardID + " Board not found");
        }
        this.myBoardDescriptor.setUploadPort("none");

        myAttributes.serial = !doesNotSupportSerialList().contains(boardID);
        myAttributes.serial1 = supportSerial1List().contains(boardID);
        myAttributes.keyboard = supportKeyboardList().contains(boardID);
        myAttributes.wire1 = supportWire1List().contains(boardID);
        myAttributes.buildInLed = !doesNotSupportbuildInLed().contains(boardID);

    }

    private Arduino(BoardDescription boardDescriptor) {
        myBoardDescriptor = boardDescriptor;
        myBoardDescriptor.setUploadPort("none");
        String boardID = myBoardDescriptor.getBoardID();
        myAttributes.serial = !doesNotSupportSerialList().contains(boardID);
        myAttributes.serial1 = supportSerial1List().contains(boardID);
        myAttributes.keyboard = supportKeyboardList().contains(boardID);
        myAttributes.wire1 = supportWire1List().contains(boardID);
        myAttributes.buildInLed = !doesNotSupportbuildInLed().contains(boardID);

    }

    static List<String> supportWire1List() {
        List<String> ret = new LinkedList<>();
        ret.add("zero");
        return ret;
    }

    static List<String> doesNotSupportbuildInLed() {
        List<String> ret = new LinkedList<>();
        ret.add("robotControl");
        ret.add("robotMotor");
        return ret;
    }

    static List<String> supportSerial1List() {
        List<String> ret = new LinkedList<>();
        ret.add("circuitplay32u4cat");
        ret.add("LilyPadUSB");
        ret.add("Micro");
        ret.add("yunMini");
        ret.add("robotControl");
        ret.add("Esplora");
        ret.add("mega");
        ret.add("chiwawa");
        ret.add("yun");
        ret.add("one");
        ret.add("leonardo");
        ret.add("robotMotor");
        ret.add("leonardoEth");
        ret.add("megaADK");

        return ret;
    }

    static List<String> doesNotSupportSerialList() {
        List<String> ret = new LinkedList<>();
        ret.add("gemma");

        return ret;
    }

    static List<String> supportKeyboardList() {
        List<String> ret = new LinkedList<>();
        ret.add("circuitplay32u4cat");
        ret.add("LilyPadUSB");
        ret.add("Micro");
        ret.add("yunMini");
        ret.add("robotControl");
        ret.add("Esplora");
        ret.add("chiwawa");
        ret.add("yun");
        // mySupportKeyboardList.add("one");
        // mySupportKeyboardList.add("Leonardo");
        // mySupportKeyboardList.add("robotMotor");
        // mySupportKeyboardList.add("LeonardoEth");
        // mySupportKeyboardList.add("MegaADK");

        return ret;
    }

    public static void installLatestAVRBoards() {
        BoardsManager.installLatestPlatform(jsonFileName, providerArduino, AVRArchitectureName);
    }

    public static void installLatestSamDBoards() {
        BoardsManager.installLatestPlatform(jsonFileName, providerArduino, SAMDArchitectureName);
    }

    public static void installLatestSamBoards() {
        BoardsManager.installLatestPlatform(jsonFileName, providerArduino, SAMArchitectureName);
    }

    public static void installLatestIntellCurieBoards() {
        BoardsManager.installLatestPlatform(jsonFileName, providerIntel, intelCurieArchitectureName);
    }

    public static List<MCUBoard> getAllBoards() {
        List<MCUBoard> ret = new LinkedList<>();
        Map<String, String> options = null;
        ArduinoPackage arduinoPkg = BoardsManager.getPackageByProvider(providerArduino);
        for (ArduinoPlatform curPlatform : arduinoPkg.getPlatforms()) {
            ArduinoPlatformVersion curPlatformVersion = curPlatform.getNewestInstalled();
            if (curPlatformVersion != null) {
                List<BoardDescription> boardDescriptions = BoardDescription
                        .makeBoardDescriptors(curPlatformVersion.getBoardsFile(), options);
                for (BoardDescription curBoardDesc : boardDescriptions) {
                    ret.add(new Arduino(curBoardDesc));
                }
            }
        }
        return ret;

    }

    public static MCUBoard zeroNatviePort() {
        MCUBoard zero = new Arduino(providerArduino, SAMDArchitectureName, "arduino_zero_native");
        zero.mySlangName = "zero Native";
        zero.mySerialPort = "SerialUSB";
        return zero;
    }

    public static MCUBoard zeroNatviePort(String uploadPort) {
        MCUBoard zero = zeroNatviePort();
        zero.myBoardDescriptor.setUploadPort(uploadPort);
        return zero;
    }

}