package io.sloeber.providers;

import static org.junit.Assert.fail;

import java.util.Map;
import java.util.TreeMap;

import io.sloeber.core.api.PackageManager;

@SuppressWarnings("nls")
public class ESP8266 extends MCUBoard {

	public static MCUBoard wemosD1() {
		Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		options.put("CpuFrequency", "80");
		options.put("UploadSpeed", "115200");
		options.put("FlashSize", "4M1M");
		ESP8266 ret= new ESP8266("d1_mini", options);
		ret.mySlangName="wemos";
		return ret;
	}

	public static MCUBoard wemosD1(String uploadPort) {
		MCUBoard board = wemosD1();
		board.myBoardDescriptor.setUploadPort(uploadPort);
		return board;
	}


	public static MCUBoard nodeMCU() {
		return new ESP8266("nodemcu", null);
	}

	public static MCUBoard NodeMCUBoard(String uploadPort) {
		MCUBoard board = wemosD1();
		board.myBoardDescriptor.setUploadPort(uploadPort);
		return board;
	}

	public static MCUBoard  ESPressoLite() {
		return new ESP8266("espresso_lite_v2", null);
	}


	public ESP8266(String boardName, Map<String, String> options) {
		this.myBoardDescriptor = PackageManager.getBoardDescriptor("package_esp8266com_index.json", "esp8266", "esp8266",
				boardName, options);
		if (this.myBoardDescriptor == null) {
			fail(boardName + " Board not found");
		}

	}

}