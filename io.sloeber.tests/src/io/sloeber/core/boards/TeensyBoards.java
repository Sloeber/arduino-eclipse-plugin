package io.sloeber.core.boards;

import static org.junit.Assert.fail;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.sloeber.core.Shared;
import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class TeensyBoards extends IBoard {

	private static List<String> mySupportSerialList = null;
	private static List<String> mySupportSerial1List = null;
	private static List<String> mySupportKeyboardList = null;
	private final static String Teensy3_0_ID = "teensy30";
	private final static String Teensy3_1_ID = "teensy31";
	private final static String Teensy_PP2_ID = "teensypp2";
	private final static String Teensy_2_ID = "teensy2";
	private final static String Teensy3_5_ID = "teensy35";
	private final static String Teensy3_6_ID = "teensy36";
	private final static String Teensy_LC_ID = "teensyLC";
	public static IBoard Teensy_LC() {
		return new TeensyBoards(Teensy_LC_ID);
	}
	public static IBoard Teensy3_5() {
		return new TeensyBoards(Teensy3_5_ID);
	}
	public static IBoard Teensy3_6() {
		return new TeensyBoards(Teensy3_6_ID);
	}
	public static IBoard Teensy3_1() {
		return new TeensyBoards(Teensy3_1_ID);
	}
	public static IBoard Teensy3_0() {
		return new TeensyBoards(Teensy3_0_ID);
	}
	public static IBoard teensypp2() {
		return new TeensyBoards(Teensy_PP2_ID);
	}
	public static IBoard teensy2() {
		return new TeensyBoards(Teensy_2_ID);
	}

	private TeensyBoards(String boardName) {
		Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		options.put("usb", "serialhid");
		options.put("speed", "48");
		options.put("opt", "o2std");
		options.put("keys", "en-us");
		switch (boardName) {
		case Teensy_PP2_ID:
			options.put("speed", "8");
			options.put("usb", "serialhid");
			break;
		case Teensy_2_ID:
			options.put("speed", "8");
			break;
		case Teensy3_1_ID:
			// to avoid the os to install keyboard and other drives I specify the options to
			// something not needing drivers
			options.put("usb", "serial");
			options.put("speed", "72");
			options.put("opt", "o2std");
			options.put("keys", "en-us");
			break;
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