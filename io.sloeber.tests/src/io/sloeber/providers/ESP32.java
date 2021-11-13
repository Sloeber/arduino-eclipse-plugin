package io.sloeber.providers;

import static org.junit.Assert.*;

import java.util.Map;
import java.util.TreeMap;

import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class ESP32 extends MCUBoard {
	private static final String provider = "esp32";
    private static final String architectureName = "esp32";
    private static final String jsonFileName ="package_esp32_index.json";
    public static final String packageURL ="https://dl.espressif.com/dl/package_esp32_index.json";
    public static final String esp32ID ="esp32";
    

	public static MCUBoard esp32() {
		Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
//		options.put("CpuFrequency", "80");
//		options.put("UploadSpeed", "115200");
//		options.put("FlashSize", "4M1M");
		ESP32 ret= new ESP32(esp32ID, options);
	//	ret.mySlangName="wemos";
		return ret;
	}



	public ESP32(String boardName, Map<String, String> options) {
		this.myBoardDescriptor = BoardsManager.getBoardDescription(jsonFileName, provider, architectureName,
				boardName, options);
		if (this.myBoardDescriptor == null) {
			fail(boardName + " Board not found");
		}

	}
	
	public static void installLatest() {
	    BoardsManager.installLatestPlatform(jsonFileName,provider, architectureName);
	}

}