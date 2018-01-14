package io.sloeber.core;

import static org.junit.Assert.fail;

import io.sloeber.core.api.Other;
import io.sloeber.core.boards.ArduinoBoards;
import io.sloeber.core.boards.ESP8266Boards;
import io.sloeber.core.boards.IBoard;
import io.sloeber.core.boards.TeensyBoards;

/*
 * Set system specific info here.
 * For now this is only the teensy installlocation
 */
@SuppressWarnings("nls")
public class MySystem {
	private static String jantjesWindowsMachine = "D:\\arduino\\arduino-1.8.2Teensy1.38beta2\\hardware\\teensy";
	private static String jantjesVirtualLinuxMachine = "/home/jantje/programs/arduino-1.8.0/hardware/teensy";

	public static String getTeensyPlatform() {
		switch (Other.getSystemHash()) {
		case "1248215851":
			return jantjesWindowsMachine;
		case "still need to get the key":
			return jantjesVirtualLinuxMachine;
		}
		return new String();
	}

	public static IBoard[] getUploadBoards()  {
		switch (Other.getSystemHash()) {
		case "still need to get the key": {
			IBoard[] boards = {   TeensyBoards.teensypp2(),  ESP8266Boards.wemosD1("COM34"),
					ArduinoBoards.fried(""), ArduinoBoards.yun(""), ArduinoBoards.uno(""),
					ArduinoBoards.getMega2560Board(""), ArduinoBoards.zero("COM14"), ArduinoBoards.due(""),
					ArduinoBoards.leonardo(""), ArduinoBoards.arduino_101("") };
			return boards;
		}
		case "1248215851": {
			IBoard[] boards = {  TeensyBoards.teensypp2(), ESP8266Boards.wemosD1("COM34"),
					ArduinoBoards.fried("COM5"), ArduinoBoards.yun("COM17"), ArduinoBoards.uno("COM6"),
					ArduinoBoards.getMega2560Board("COM11"), ArduinoBoards.zero("COM14"), ArduinoBoards.due("COM3"),ArduinoBoards.dueprogramming("COM8"),
					ArduinoBoards.leonardo("COM19"), ArduinoBoards.arduino_101("") };
			return boards;
		}
		}
		fail("Boards for the system with haskey " + Other.getSystemHash() + "are not found");
		return null;
	}
}
