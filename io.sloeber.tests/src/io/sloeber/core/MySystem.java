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
	//the one below is based on one mac address Fysiek adres (MAC):	C0-3F-D5-66-04-58 
	private static final String jantjesWindowsMachineHashkeyAfterUpdate="139705674";
	private static final String jantjesLinuxMachineHashKey = "-784776710";

	public static String getTeensyPlatform() {
		switch (Other.getSystemHash()) {
		case jantjesWindowsMachineHashKey:
		case jantjesWindowsMachineHashkeyAfterUpdate:
			return "D:\\arduino\\teensy-latest\\hardware\\teensy";
		case jantjesLinuxMachineHashKey:
			return "/home/jan/arduino-1.8.5/hardware/teensy-latest";
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
		case jantjesWindowsMachineHashKey: 
		case jantjesWindowsMachineHashkeyAfterUpdate:{
			//due native upload gives to mutch trouble even in arduino IDE
			MCUBoard[] boards = {  
					Teensy.teensypp2("COM10"),
					Teensy.Teensy3_1("COM24"), 
					ESP8266.wemosD1("COM23"),
					Arduino.fried("COM22"), 
					Arduino.yun("COM5"), 
					Arduino.uno("COM6"),
					Arduino.getMega2560Board("COM11"), 
					Arduino.zero("COM14"), 
					Arduino.dueprogramming("COM8"),
					Arduino.leonardo("COM30"), 
					Arduino.arduino_101("COM15") };
			return boards;
		}
		}
		fail("Boards for the system with haskey " + Other.getSystemHash() + "are not found");
		return null;
	}
}
