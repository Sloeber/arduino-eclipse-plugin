package io.sloeber.providers;


import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;
import java.util.TreeMap;

import io.sloeber.arduinoFramework.api.BoardDescription;
import io.sloeber.arduinoFramework.api.BoardsManager;

@SuppressWarnings("nls")
public class ESP32 extends MCUBoard {
    private static final String provider = "esp32";
    private static final String architectureName = "esp32";
    public static final String packageURL = "https://espressif.github.io/arduino-esp32/package_esp32_index.json";
    public static final String esp32ID = "esp32";

    public static MCUBoard esp32() {
        Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        //		options.put("CpuFrequency", "80");
        //		options.put("UploadSpeed", "115200");
        //		options.put("FlashSize", "4M1M");
        ESP32 ret = new ESP32(esp32ID, options);
        //	ret.mySlangName="wemos";
        return ret;
    }

    public ESP32(String boardName, Map<String, String> options) {
        myBoardDescriptor = BoardsManager.getBoardDescription(packageURL, provider, architectureName, boardName,
                options);
        if (myBoardDescriptor == null) {
            fail(boardName + " Board not found");
        }
        setAttributes();
    }

    private ESP32(String providerName, String architectureName, String boardID) {
        this.myBoardDescriptor = BoardsManager.getBoardDescription(packageURL, providerName, architectureName,
                boardID, null);
        if (this.myBoardDescriptor == null) {
            fail(boardID + " Board not found");
        }
        this.myBoardDescriptor.setUploadPort("none");
        setAttributes();
    }

    public ESP32(BoardDescription boardDesc) {
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
        // nothing to set

    }

    @Override
    public MCUBoard createMCUBoard(BoardDescription boardDesc) {
        return new ESP32(boardDesc);
    }

	public static Object ESP32S3() {
		return new ESP32(provider, architectureName, "esp32s3");
	}

}