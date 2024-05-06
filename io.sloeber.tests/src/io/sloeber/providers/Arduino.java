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
    private static final String MBEDArchitectureName = "mbed";
    private static final String intelCurieArchitectureName = "arc32";
    private static final String jsonFileName = "package_index.json";

    public static final String circuitplay32ID = "circuitplay32u4cat";
    public static final String unoID = "uno";
    public static final String robotControlID ="robotControl";
    public static final String ethernetID = "ethernet";
    public static final List<String> mbedBoards = getAllmBedBoardNames();

    public static MCUBoard gemma() {
        return new Arduino(providerArduino, AVRArchitectureName, "gemma");
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

    public static MCUBoard fried2016() {
        return new Arduino(providerArduino, AVRArchitectureName, "LilyPadUSB");
    }

    public static MCUBoard mega2560Board() {
        MCUBoard mega = new Arduino(providerArduino, AVRArchitectureName, "mega");
        Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        options.put("cpu", "atmega2560");
        mega.myBoardDescriptor.setOptions(options);
        return mega;
    }

    public static MCUBoard leonardo() {
        return new Arduino(providerArduino, AVRArchitectureName, "leonardo");
    }

    public static MCUBoard yun() {
        return new Arduino(providerArduino, AVRArchitectureName, "yun");
    }

    public static MCUBoard zeroProgrammingPort() {
        return new Arduino(providerArduino, SAMDArchitectureName, "arduino_zero_edbg");
    }

    public static MCUBoard zeroNatviePort() {
        MCUBoard zero = new Arduino(providerArduino, SAMDArchitectureName, "arduino_zero_native");
        zero.mySerialPort = "SerialUSB";
        return zero;
    }

    public static MCUBoard due() {
        return new Arduino(providerArduino, SAMArchitectureName, "arduino_due_x");
    }

    public static MCUBoard dueprogramming() {
        return new Arduino(providerArduino, SAMArchitectureName, "arduino_due_x_dbg");
    }

    public static MCUBoard mkrfox1200() {
        return new Arduino(providerArduino, SAMDArchitectureName, "mkrfox1200");
    }

    public static MCUBoard primo() {
        return new Arduino(providerArduino, NFRArchitectureName, "primo");
    }

    public static MCUBoard uno() {
        return new Arduino(providerArduino, AVRArchitectureName, unoID);
    }

    public static MCUBoard robotControl() {
        return new Arduino(providerArduino, AVRArchitectureName, robotControlID);
    }

    public static MCUBoard ethernet() {
        return new Arduino(providerArduino, AVRArchitectureName, ethernetID);
    }

    public static MCUBoard arduino_101() {
        return new Arduino(providerIntel, intelCurieArchitectureName, "arduino_101");
    }

    private Arduino(String providerName, String architectureName, String boardID) {
        this.myBoardDescriptor = BoardsManager.getBoardDescription(jsonFileName, providerName, architectureName,
                boardID, null);
        if (this.myBoardDescriptor == null) {
            fail(boardID + " Board not found");
        }
        this.myBoardDescriptor.setUploadPort("none");
        setAttributes();
    }

    @Override
    public MCUBoard createMCUBoard(BoardDescription boardDescriptor) {
        return new Arduino(boardDescriptor);

    }

    public Arduino(BoardDescription boardDesc) {
        myBoardDescriptor = boardDesc;
        myBoardDescriptor.setUploadPort("none");
        setAttributes();
    }

    @Override
    protected void setAttributes() {
        String boardID = myBoardDescriptor.getBoardID();
        myAttributes.serial = !doesNotSupportSerialList().contains(boardID);
        myAttributes.SD=!doesNotSupportSD().contains(boardID);
        myAttributes.serial1 = supportSerial1List().contains(boardID);
        myAttributes.keyboard = supportKeyboardList().contains(boardID);
        myAttributes.wire1 = supportWire1List().contains(boardID);
        myAttributes.buildInLed = !doesNotSupportbuildInLed().contains(boardID);
        myAttributes.tone = !doesNotSupportTone().contains(boardID);
        myAttributes.myNumAD = getNumADCsAvailable(boardID, myAttributes.myNumAD);
        myAttributes.directMode = !doesNotSupportDirectModeList().contains(boardID);
        myAttributes.serialUSB = !doesNotSupportSerialUSBList().contains(boardID);
        myAttributes.digitalPinToPCICR =!doesNotSupportDigitalPinToPCICR().contains(boardID);
        myAttributes.RX_TX_PIN =!doesNotSupportRX_TX_Pin().contains(boardID);

        myAttributes.myArchitecture=myBoardDescriptor.getArchitecture();
    }


    private static int getNumADCsAvailable(String boardID, int standard) {
        switch (boardID) {
        case "nicla_sense":
            return 2;
        case "pico":
        case "nanorp2040connect":
            return 4;
        default:
            //don't change default
            return standard;
        }
    }

    private static List<String> supportWire1List() {
        List<String> ret = new LinkedList<>();
        ret.add("zero");
        return ret;
    }

    private static List<String> doesNotSupportbuildInLed() {
        List<String> ret = new LinkedList<>();
        ret.add("robotControl");
        ret.add("robotMotor");
        ret.add("edge_control");
        ret.add("nicla_sense");
        return ret;
    }

    private static List<String> doesNotSupportDigitalPinToPCICR() {
        List<String> ret = new LinkedList<>();
        ret.add("robotControl");
        ret.add("robotMotor");
        ret.add("gemma");
        return ret;
    }

    private static List<String> doesNotSupportRX_TX_Pin() {
        List<String> ret = new LinkedList<>();
        ret.add("atmegang");
        return ret;
    }

    private static List<String> supportSerial1List() {
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

    private static List<String> doesNotSupportSerialList() {
        List<String> ret = new LinkedList<>();
        ret.add("gemma");
        return ret;
    }

    private static List<String> doesNotSupportSD() {
        List<String> ret = new LinkedList<>();
        ret.add("gemma");
        return ret;
    }

    private static List<String> doesNotSupportSerialUSBList() {
        List<String> ret = new LinkedList<>();
        ret.addAll(mbedBoards);
        return ret;
    }

    private static List<String> doesNotSupportDirectModeList() {
        List<String> ret = new LinkedList<>();
        ret.addAll(mbedBoards);
        return ret;
    }

    private static List<String> doesNotSupportTone() {
        List<String> ret = new LinkedList<>();
        ret.add("arduino_due_x");
        ret.add("arduino_due_x_dbg");
        return ret;
    }

    private static List<String> supportKeyboardList() {
        List<String> ret = new LinkedList<>();
        ret.add("circuitplay32u4cat");
        ret.add("LilyPadUSB");
        ret.add("Micro");
        ret.add("yunMini");
        ret.add("robotControl");
        ret.add("Esplora");
        ret.add("chiwawa");
        ret.add("yun");
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
        return getAllBoards(providerArduino, uno());
    }

    public static List<MCUBoard> getAllAvrBoards() {
        return getAllBoards(providerArduino,AVRArchitectureName, uno());
    }

    private static List<String> getAllmBedBoardNames() {
        List<String> ret = new LinkedList<>();
        ArduinoPackage arduinoPkg = BoardsManager.getPackageByProvider(providerArduino);
        for (ArduinoPlatform curPlatform : arduinoPkg.getPlatforms()) {
            if (curPlatform.getArchitecture().equals(MBEDArchitectureName)) {
                ArduinoPlatformVersion curPlatformVersion = curPlatform.getNewestInstalled();
                if (curPlatformVersion != null) {
                    List<BoardDescription> boardDescriptions = BoardDescription
                            .makeBoardDescriptors(curPlatformVersion.getBoardsFile());
                    for (BoardDescription curBoardDesc : boardDescriptions) {
                        ret.add(curBoardDesc.getBoardName());
                    }
                }
            }
        }
        return ret;
    }
}
