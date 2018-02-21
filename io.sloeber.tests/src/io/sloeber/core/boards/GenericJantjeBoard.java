package io.sloeber.core.boards;

import static org.junit.Assert.fail;

import java.util.Map;
import java.util.TreeMap;

import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class GenericJantjeBoard extends IBoard {



	public static String getJsonFileName() {
		return  "package_jantje_index.json";
	}
	public static String getPackageName() {
		return "Jantje";
	}
	public static String getPlatformName() {
		return "Arduino avr Boards (local debug)";
	}
	public GenericJantjeBoard(String boardName) {
		Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		options.put("type", "debug");
		this.myBoardDescriptor = BoardsManager.getBoardDescriptor(getJsonFileName(),getPackageName(),getPlatformName() ,
				boardName, options);
		if (this.myBoardDescriptor == null) {
			fail(boardName + " Board not found");
		}
		this.myBoardDescriptor.setUploadPort("none");

		setSupportSerial(!ArduinoBoards.doesNotSupportSerialList().contains(boardName));
		setSupportSerial1(ArduinoBoards.supportSerial1List().contains(boardName));
		setSupportKeyboard(ArduinoBoards.supportKeyboardList().contains(boardName));
	}



}
