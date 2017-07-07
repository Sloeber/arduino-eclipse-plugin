package jUnit.boards;

import static org.junit.Assert.fail;

import java.util.Map;
import java.util.TreeMap;

import io.sloeber.core.api.BoardsManager;
import jUnit.Shared;

@SuppressWarnings("nls")
public class Teensy3_1Board extends IBoard {
	public Teensy3_1Board() {
		// to avoid the os to install keyboard and other drives I specify the options to
		// something not needing drivers
		Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		options.put("usb", "serial");
		options.put("speed", "72");
		options.put("opt", "o2std");
		options.put("keys", "en-us");


		this.myBoardDescriptor = BoardsManager.getBoardDescriptor("local", Shared.getTeensyBoard_txt(),
				"ignored", "teensy31", options);
		if (this.myBoardDescriptor == null) {
			fail("teensy 3.1 Board not found");
		}
		this.myBoardDescriptor.setUploadPort("COM15");
	}

}