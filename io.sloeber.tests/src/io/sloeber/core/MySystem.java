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
	private static final String jantjesLinuxMachineHashKey = "88937904";
	private static final String jantjeWindowsMachineHashkeyAfterSecondUpdate="-441525448";

	public static String getTeensyPlatform() {
		switch (Other.getSystemHash()) {
		case jantjesWindowsMachineHashKey:
		case jantjesWindowsMachineHashkeyAfterUpdate:
		case jantjeWindowsMachineHashkeyAfterSecondUpdate:
			return "E:\\arduino\\arduino-1.8.12 - teensy\\hardware\\teensy";
		case jantjesLinuxMachineHashKey:
			return "/home/jan/tensyduino/arduino-1.8.12/hardware/teensy";
		default:
			return new String();
		}
	}

	public static String getTeensyBoard_txt() {
		return getTeensyPlatform() + "/avr/boards.txt";
	}
	public static MCUBoard[] getUploadBoards()  {
		switch (Other.getSystemHash()) {
		case jantjesLinuxMachineHashKey: {
			MCUBoard[] boards = {   
					Teensy.teensypp2("/dev/ttyACM0"),  
					Arduino.leonardo("/dev/ttyS0"),   //werkt niet
					Arduino.fried2016("/dev/ttyS0"), //werkt niet
					Arduino.zeroNatviePort("/dev/ttyS0"), //werkt niet
					Arduino.yun("COM20"), 
					ESP8266.wemosD1("/dev/ttyUSB0"),
					Arduino.arduino_101("COM15"),
					Arduino.zeroProgrammingPort("COM14"), 
					Arduino.getMega2560Board("COM11"), 
					Arduino.dueprogramming("COM8"),
					Arduino.uno("COM6"),
					};
			return boards;
		}
		case jantjesWindowsMachineHashKey: 
		case jantjeWindowsMachineHashkeyAfterSecondUpdate:
		case jantjesWindowsMachineHashkeyAfterUpdate:{
			//due native upload gives to mutch trouble even in arduino IDE
			MCUBoard[] boards = {  
					Teensy.teensypp2("COM10"),
				//	Teensy.Teensy3_1("COM24"), 
					Arduino.leonardo("COM101"), 
					Arduino.fried2016("COM102"), 
					Arduino.zeroNatviePort("COM104"), //boardSponsor
					Arduino.yun("COM106"), 
					ESP8266.wemosD1("COM108"),
					Arduino.arduino_101("COM110"),
					Arduino.zeroProgrammingPort("COM111"), 
					Arduino.getMega2560Board("COM112"), 
					Arduino.dueprogramming("COM124"),
					Arduino.uno("COM126"),
					};
			return boards;
		}
		default:
			fail("Boards for the system with haskey " + Other.getSystemHash() + " are not found");
			return null;
		}
	}
}
