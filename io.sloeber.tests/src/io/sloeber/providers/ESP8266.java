package io.sloeber.providers;


import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;
import java.util.TreeMap;

import io.sloeber.arduinoFramework.api.BoardDescription;
import io.sloeber.arduinoFramework.api.BoardsManager;

@SuppressWarnings("nls")
public class ESP8266 extends MCUBoard {
    private static final String provider = "esp8266";
    private static final String architectureName = "esp8266";
    public static final String packageURL = "https://arduino.esp8266.com/stable/package_esp8266com_index.json";

    public static MCUBoard wemosD1() {
        Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        options.put("CpuFrequency", "80");
        options.put("UploadSpeed", "115200");
        options.put("FlashSize", "4M1M");
        return new ESP8266("d1_mini", options);
    }

    public static MCUBoard nodeMCU() {
        return new ESP8266("nodemcu", null);
    }

    public static MCUBoard ESPressoLite() {
        return new ESP8266("espresso_lite_v2", null);
    }

    public ESP8266(String boardName, Map<String, String> options) {
        myBoardDescriptor = BoardsManager.getBoardDescription(packageURL, provider, architectureName, boardName,
                options);
        if (this.myBoardDescriptor == null) {
            fail(boardName + " Board not found");
        }

    }

    public ESP8266(BoardDescription boardDesc) {
        myBoardDescriptor = boardDesc;
        myBoardDescriptor.setUploadPort("none");
        setAttributes();
    }

    public static void installLatest() {
        BoardsManager.installLatestPlatform(packageURL, provider, architectureName);
    }

    @Override
    protected void setAttributes() {
        myAttributes.myArchitecture=myBoardDescriptor.getArchitecture();

    }

    @Override
    public MCUBoard createMCUBoard(BoardDescription boardDesc) {
        return new ESP8266(boardDesc);
    }

}