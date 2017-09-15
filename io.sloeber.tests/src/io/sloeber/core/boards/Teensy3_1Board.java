package io.sloeber.core.boards;

import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("nls")
public class Teensy3_1Board extends GenericTeensyBoard {
	public Teensy3_1Board() {
		super("teensy31");
		Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		// to avoid the os to install keyboard and other drives I specify the options to
		// something not needing drivers
		options.put("usb", "serial");
		options.put("speed", "72");
		options.put("opt", "o2std");
		options.put("keys", "en-us");
		this.myBoardDescriptor.setOptions(options);
		this.myBoardDescriptor.setUploadPort("COM15");
	}
}