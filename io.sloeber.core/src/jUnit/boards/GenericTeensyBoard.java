package jUnit.boards;

import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.sloeber.core.api.BoardsManager;
import jUnit.Shared;

@SuppressWarnings("nls")
public class GenericTeensyBoard extends IBoard {

	private static List<String> mySupportSerialList = null;
	private static List<String> mySupportSerial1List = null;
	private static List<String> mySupportKeyboardList = null;

	public GenericTeensyBoard(String boardName) {
		Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		options.put("usb", "serialhid");
		options.put("speed", "48");
		options.put("opt", "o2std");
		options.put("keys", "en-us");
		if ("teensypp2".equals(boardName)) {
			options.put("speed", "8");
			options.put("usb", "serialhid");
		}
		if ("teensy2".equals(boardName)) {
			options.put("speed", "8");
		}
		this.myBoardDescriptor = BoardsManager.getBoardDescriptor("local", Shared.getTeensyBoard_txt(), "ignored",
				boardName, options);
		if (this.myBoardDescriptor == null) {
			fail(boardName + " Board not found");
		}
		this.myBoardDescriptor.setUploadPort("none");

		setSupportSerial(supportSerialList().contains(boardName));
		setSupportSerial1(supportSerial1List().contains(boardName));
		setSupportKeyboard(supportKeyboardList().contains(boardName));
	}

	private static List<String> supportSerial1List() {
		if (mySupportSerial1List == null) {
			mySupportSerial1List = getAllBoards();

		}
		return mySupportSerial1List;
	}

	private static List<String> supportSerialList() {
		if (mySupportSerialList == null) {
			mySupportSerialList = getAllBoards();
		}
		return mySupportSerialList;
	}

	private static List<String> supportKeyboardList() {
		if (mySupportKeyboardList == null) {
			mySupportKeyboardList = getAllBoards();
		}
		return mySupportKeyboardList;
	}

	private static List<String> getAllBoards() {
		List<String> allBoards = new LinkedList<>();
		String[] boards = BoardsManager.getBoardNames(Shared.getTeensyBoard_txt());
		for (String curBoard : boards) {
			allBoards.add(curBoard);
		}

		return allBoards;
	}

}