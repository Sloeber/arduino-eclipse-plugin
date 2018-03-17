package io.sloeber.providers;

import static org.junit.Assert.fail;

import java.util.Map;
import java.util.TreeMap;

import io.sloeber.core.api.PackageManager;

@SuppressWarnings("nls")
public class Jantje extends MCUBoard {



	public static String getJsonFileName() {
		return  "package_jantje_index.json";
	}
	public static String getPackageName() {
		return "Jantje";
	}
	public static String getPlatformName() {
		return "Arduino avr Boards (local debug)";
	}
	public Jantje(String boardName) {
		Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		options.put("type", "debug");
		this.myBoardDescriptor = PackageManager.getBoardDescriptor(getJsonFileName(),getPackageName(),getPlatformName() ,
				boardName, options);
		if (this.myBoardDescriptor == null) {
			fail(boardName + " Board not found");
		}
		this.myBoardDescriptor.setUploadPort("none");

		myAttributes.serial=!Arduino.doesNotSupportSerialList().contains(boardName);
		myAttributes.serial1=Arduino.supportSerial1List().contains(boardName);
		myAttributes.keyboard=Arduino.supportKeyboardList().contains(boardName);
	}



}
