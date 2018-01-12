package io.sloeber.core.boards;

import static org.junit.Assert.fail;

import java.util.Map;
import java.util.TreeMap;

import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class ESP8266Boards extends IBoard {
	private static final String WEMOSD1_ID = "d1_mini";
	private static final String NODE_MCU_ID = "nodemcu";

	public static IBoard wemosD1() {
		Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		options.put("CpuFrequency", "80");
		options.put("UploadSpeed", "115200");
		options.put("FlashSize", "4M1M");
		return new ESP8266Boards(WEMOSD1_ID, options);
	}

	public static IBoard wemosD1(String uploadPort) {
		IBoard board = wemosD1();
		board.myBoardDescriptor.setUploadPort(uploadPort);
		return board;
	}


	public static IBoard NodeMCUBoard() {
		return new ESP8266Boards(NODE_MCU_ID, null);
	}

	public static IBoard NodeMCUBoard(String uploadPort) {
		IBoard board = wemosD1();
		board.myBoardDescriptor.setUploadPort(uploadPort);
		return board;
	}


	public ESP8266Boards(String boardName, Map<String, String> options) {
		this.myBoardDescriptor = BoardsManager.getBoardDescriptor("package_esp8266com_index.json", "esp8266", "esp8266",
				boardName, options);
		if (this.myBoardDescriptor == null) {
			fail(boardName + " Board not found");
		}

	}

}