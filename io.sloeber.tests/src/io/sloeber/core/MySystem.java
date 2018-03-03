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
	private static final String jantjesWindowsMachineHashKey = "1248215851";
	private static final String jantjesLinuxMachineHashKey = "-784776710";

	public static String getTeensyPlatform() {
		switch (Other.getSystemHash()) {
		case jantjesWindowsMachineHashKey:
			return "D:\\arduino\\arduino-1.8.5\\hardware\\teensy";
		case jantjesLinuxMachineHashKey:
			return "/home/jan/arduino-1.8.5/hardware/teensy";
		}
		return new String();
	}

	public static String getTeensyBoard_txt() {
		return getTeensyPlatform() + "/avr/boards.txt";
	}
	public static IBoard[] getUploadBoards()  {
		switch (Other.getSystemHash()) {
		case jantjesLinuxMachineHashKey: {
			IBoard[] boards = {   TeensyBoards.teensypp2(),  ESP8266Boards.wemosD1("COM34"),
					ArduinoBoards.fried(""), ArduinoBoards.yun(""), ArduinoBoards.uno(""),
					ArduinoBoards.getMega2560Board(""), ArduinoBoards.zero("COM14"), ArduinoBoards.due(""),
					ArduinoBoards.leonardo(""), ArduinoBoards.arduino_101("") };
			return boards;
		}
		case jantjesWindowsMachineHashKey: {
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
