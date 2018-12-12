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
	private static final String jantjeWindowsMachineHashkeyAfterSecondUpdate="-441525448";

	public static String getTeensyPlatform() {
		switch (Other.getSystemHash()) {
		case jantjesWindowsMachineHashKey:
		case jantjesWindowsMachineHashkeyAfterUpdate:
		case jantjeWindowsMachineHashkeyAfterSecondUpdate:
			return "C:\\test\\teensy-latest\\hardware\\teensy";
		case jantjesLinuxMachineHashKey:
			return "/home/jan/nas/linux/arduino/arduino-1.8.8-linux64/arduino-1.8.8/hardware/teensy";
		}
		return new String();
	}

	public static String getTeensyBoard_txt() {
		return getTeensyPlatform() + "/avr/boards.txt";
	}
	public static MCUBoard[] getUploadBoards()  {
		switch (Other.getSystemHash()) {
		case jantjesLinuxMachineHashKey: {
			MCUBoard[] boards = {   Teensy.teensypp2(),  ESP8266.wemosD1("COM21"),
					Arduino.fried2016("COM26"), Arduino.yun("COM20"), Arduino.uno("COM6"),
					Arduino.getMega2560Board("COM11"), Arduino.zeroProgrammingPort("COM14"), Arduino.due("COM8"),
					Arduino.leonardo("COM31"), Arduino.arduino_101("COM15") };
			return boards;
		}
		case jantjesWindowsMachineHashKey: 
		case jantjeWindowsMachineHashkeyAfterSecondUpdate:
		case jantjesWindowsMachineHashkeyAfterUpdate:{
			//due native upload gives to mutch trouble even in arduino IDE
			MCUBoard[] boards = {  
					Teensy.teensypp2("COM10"),
				//	Teensy.Teensy3_1("COM24"), 
					ESP8266.wemosD1("COM41"),
					Arduino.fried2016("COM36"), 
					Arduino.yun("COM40"), 
					Arduino.uno("COM6"),
					Arduino.getMega2560Board("COM11"), 
					Arduino.zeroProgrammingPort("COM14"), 
					//Arduino.dueprogramming("COM"),no final cable yet
					Arduino.leonardo("COM37"), 
					Arduino.arduino_101("COM15"),
					Arduino.zeroNatviePort("COM38"), //boardSponsor
					};
			
		 
			return boards;
		}
		}
		fail("Boards for the system with haskey " + Other.getSystemHash() + "are not found");
		return null;
	}
}
