package io.sloeber.core;

import static org.junit.Assert.fail;

import io.sloeber.core.api.Other;
import io.sloeber.providers.Arduino;
import io.sloeber.providers.ESP8266;
import io.sloeber.providers.MCUBoard;
import io.sloeber.providers.Teensy;

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
	public static MCUBoard[] getUploadBoards()  {
		switch (Other.getSystemHash()) {
		case jantjesLinuxMachineHashKey: {
			MCUBoard[] boards = {   Teensy.teensypp2(),  ESP8266.wemosD1("COM34"),
					Arduino.fried(""), Arduino.yun(""), Arduino.uno(""),
					Arduino.getMega2560Board(""), Arduino.zero("COM14"), Arduino.due(""),
					Arduino.leonardo(""), Arduino.arduino_101("") };
			return boards;
		}
		case jantjesWindowsMachineHashKey: {
			MCUBoard[] boards = {  Teensy.teensypp2(), ESP8266.wemosD1("COM34"),
					Arduino.fried("COM5"), Arduino.yun("COM17"), Arduino.uno("COM6"),
					Arduino.getMega2560Board("COM11"), Arduino.zero("COM14"), Arduino.due("COM3"),Arduino.dueprogramming("COM8"),
					Arduino.leonardo("COM19"), Arduino.arduino_101("") };
			return boards;
		}
		}
		fail("Boards for the system with haskey " + Other.getSystemHash() + "are not found");
		return null;
	}
}
