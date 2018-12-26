package io.sloeber.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import io.sloeber.providers.Adafruit;
import io.sloeber.providers.Arduino;
import io.sloeber.providers.ESP32;
import io.sloeber.providers.Teensy;

@SuppressWarnings("nls")
public class Libraries {

	public static boolean doNotUseThisLib(String libName) {
		String skipLibs[] = new String[] { "ACROBOTIC_SSD1306", "XLR8Servo", "Adafruit_CC3000_Library",
				"Adafruit_HX8340B", "Adafruit_IO_Arduino", "Adafruit_MQTT_Library", "Adafruit_SPIFlash",
				"Adafruit_SSD1325", "ArdBitmap", "ArdOSC", "Arduino-Websocket-Fast", "ArduinoFacil",
				"ArduinoMenu_library", "ArduinoSensors", "ArduinoSerialToTCPBridgeClient", "ArduinoUnit", "arduinoVNC",
				"ArduZ80", "AS3935", "AzureIoTHubMQTTClient", "BigCrystal", "Babelduino", "Blynk", "Brief", "Brzo_I2C",
				"BTLE", "Cayenne", "CayenneMQTT", "Chronos", "CoAP_simple_library", "Comp6DOF_n0m1", "Constellation",
				"CRC_Simula_Library", "Cytron_3A_Motor_Driver_Shield", "DoubleResetDetector", "DCF77", "dcf77_xtal",
				"DW1000", "EDB", "eBtn", "AJSP", "EducationShield", "ArduinoMqtt", "Embedded_Template_Library",
				"Embedis", "EMoRo_2560", "Adafruit_microbit_Library", "GPRSbee", "hd44780",
				/*
				 * AceButton uses namespace in ino files with is not compatible with sloeber ino
				 * implementation tomake it work remove using namespace and add AceButton:: as
				 * needed
				 */
				"AceButton",
				// at the time of testing seesaw is not supported on gnu
				"seesaw" ,
				//Needs a library not provided by default jsons
				"AGirs",
				"AlertMe",
				//Defines need to be changed in the lib
				"Adafruit_SSD1306_Wemos_Mini_OLED",
				//uses unknown libraries tc.h and tc_interrupt.h
				"Adafruit_ZeroTimer_Library",
				"avdweb_SAMDtimer",
				//uses unknown lib CurieBLE
				"Andee101",
				//uses unknonw lib ThingerESP8266
				"ClimaStick",

				//this fails in the combiner on windows for some strange reason.
				//I think it is a windows command line limitation again
				"ANTPLUS-Arduino",
				//post parser is 2 times in the code
				"Arduino_POST_HTTP_Parser",
				//no websocket client
				"ArduinoArcherPanelClient",
				//missing httpclient
				"ArduinoFritzApi",
				//uncomment something in my own library to make it work... are you kidding?
				"DCCpp",
				"Commanders",
				//Macro defenitions of setup and loop confusing the ino.cpp converter
				"ArduinoLang",
				//all kind of issue
				"ArduinoOSC",
				//usage of macro's confusing ino to cpp converter
				"AUnit"	,
				//contains REPLACE_ME
				"CayenneLPP",
				//uses unknown lib powerbank
				"Charge_n_Boost",
				//uses mbino that uses SPI but has SPI included so
				//indexer thinks SPI is defines but utilities is not in include path
				"CarreraDigitalControlUnit",
				//I don't know which hardware to take for this lib
				"CoDrone",
				//uses a define that is not defined
				"DcDccNanoController",
				//requires Paul Stoffregens time lib and Sloeber takes arduino time lib
				"DS3232RTC",
				//Cloud4RPi.cpp:179:5: error: 'DynamicJsonBuffer' was not declared in this scope
				"cloud4rpi-esp-arduino",
				//not sure what is wrong. Don't feel like looking
				"AceRoutine",
				//source is in scr instead of src
				"ADC_SEQR",
				//uses StaticJsonBuffer I do not have
				"AmazonDRS",
				//to complicated board selection
				"Adafruit_DAP_library",
				//fails as if wrong board tried zero and feater MO
				"Adafruit_Zero_I2S_Library",
				//to hard to assign boards correctly
				"ArduinoBearSSL",
				// call of overloaded 'requestFrom(uint8_t, size_t, bool)' is ambiguous
				"ArduinoECCX08",
				//getRequest.ino:29:45: error: no matching function for call to 'ESPAT::get(const char [16])'
				"ArduinoESPAT",
				//ESPAsyncE131.h:23:25: fatal error: ESPAsyncUDP.h: No such file or directory
				"ESP_Async_E1.31",
				//doesn't work in arduino ide
				"AIOModule",
				//ned to fix issue
				"Blynk_For_Chinese",
				};
		return Arrays.asList(skipLibs).contains(libName);
	}

	public static BoardAttributes getRequiredBoardAttributes(String libName) {
		BoardAttributes ret = new BoardAttributes();
		// ret.serial = serial && andd.serial;
		// ret.serial1 = serial1 && andd.serial1;
		// ret.keyboard = keyboard && andd.keyboard;
		// ret.flightSim = flightSim && andd.flightSim;
		// ret.joyStick = joyStick && andd.joyStick;
		// ret.midi = midi && andd.midi;
		// ret.mouse = mouse && andd.mouse;
		// ret.wire1 = wire1 && andd.wire1;
		// ret.inputPullDown = inputPullDown && andd.inputPullDown;
		// ret.teensy = teensy && andd.teensy;
		ret.worksOutOfTheBox =! doNotUseThisLib(libName);
		ret.mo_mcu = mo_mcu(libName);
		ret.esp8266_mcu = esp8266_mcu(libName);
		ret.boardName = getRequiredBoard(libName);
		return ret;
	}

	/*
	 * M0 microcontrollers (Feather M0, Arduino Zero, etc.)
	 */
	private static boolean mo_mcu(String libName) {
		String theLibs[] = new String[] { "Adafruit_composite_video_Library", "Adafruit_DMA_neopixel_library",
			"Adafruit_FreeTouch_Library" };
		return Arrays.asList(theLibs).contains(libName);
	}

	/*
	 * esp8266 libraries
	 */
	private static boolean esp8266_mcu(String libName) {
		String theLibs[] = new String[] { "Adafruit_ESP8266" ,"BoodskapTransceiver","AutoConnect","ClimaStick","CMMC_Interval","ConfigManager","CoogleIOT","CTBot"};
		return Arrays.asList(theLibs).contains(libName);
	}

	private static String getRequiredBoard(String libName) {
		if(libName==null)return null;

		Map<String, String[]> runLibOnBoard = new HashMap<>();

		runLibOnBoard.put("no board",
				new String[] { "Arduino_Commander", "ALog", "BPLib", "ESP8266_Microgear", "ESP8266_Weather_Station",
						"Gamebuino", "Gadgetron_Libraries", "GadgetBox", "ESP8266_Oled_Driver_for_SSD1306_display",
						"FrequencyTimer2", "Gamer", "ghaemShopSmSim" });
		runLibOnBoard.put("fix issue first",
				new String[] { "DS1307RTC", "ESPMail", "EspSaveCrash", "FlashStorage", "Fram", "Geometry" });
		runLibOnBoard.put("macro's confuse indexer", new String[] { "EnableInterrupt", "Eventually" });

		runLibOnBoard.put(Arduino.unoID,
				new String[] { "A4963", "Adafruit_Motor_Shield_library", "Adafruit_Motor_Shield_library_V2",
						"AccelStepper", "Arduino_Uno_WiFi_Dev_Ed_Library", "ardyno", "AVR_Standard_C_Time_Library",
						"DDS", "EtherSia", "ExodeCore", "FingerLib", "HamShield" });
		runLibOnBoard.put("esplora", new String[] { "Esplora" });
		runLibOnBoard.put(Arduino.circuitplay32ID,
				new String[] { "Adafruit_Circuit_Playground", "Adafruit_BluefruitLE_nRF51", "Adafruit_GPS_Library","Adafruit_composite_video_Library" });
		runLibOnBoard.put("nodemcu",
				new String[] { "Adafruit_IO_Arduino", "CMMC_Easy", "CMMC_MQTT_Connector", "CMMC_OTA",
						"CMMC_WiFi_Connector", "EasyUI", "EasyDDNS", "CoinMarketCapApi", "ArduinoIHC", "AsciiMassage",
						"ESPiLight", "HaLakeKit", "HaLakeKitFirst","AFArray" ,"AIOModule","AlertMe","Bleeper","Blinker"});
		runLibOnBoard.put("feather52", new String[] { "Firmata" ,"Codec2"});
		runLibOnBoard.put("primo", new String[] { "Adafruit_BluefruitLE_nRF51", "arduino-NVM" });
		runLibOnBoard.put("mega", new String[] { "Adafruit_GPS_Library", "Dynamixel_Servo", "EnergyBoard" ,"DFL168A_Async"});
		runLibOnBoard.put("zero",
				new String[] { "Arduino_Low_Power", "ArduinoSound", "AudioZero", "Dimmer_class_for_SAMD21",
						"AzureIoTHub", "AzureIoTProtocol_HTTP", "AzureIoTProtocol_MQTT", "Adafruit_AM_radio_library" });
		runLibOnBoard.put("mkrfox1200", new String[] { "Arduino_SigFox_for_MKRFox1200" });
		runLibOnBoard.put("due",
				new String[] { "Audio", "AutoAnalogAudio", "dcf77_xtal", "due_can", "DueFlashStorage", "DueTimer"});
		runLibOnBoard.put("espresso_lite_v2", new String[] { "ESPert", "ESPectro" });
		runLibOnBoard.put(ESP32.esp32ID, new String[] { "EasyBuzzer_Beep_leonardo", "ESPUI","Basecamp" });
		runLibOnBoard.put(Teensy.Teensy3_6_ID, new String[] { "ACAN","ACAN2515","ACAN2517" });
		
		runLibOnBoard.put(Adafruit.metroM4ID, new String[] { "Adafruit_QSPI" ,"Adafruit_mp3"});
		runLibOnBoard.put(Arduino.ethernetID, new String[] { "Blynk_For_Chinese" });
		 
		
		for (Entry<String, String[]> curEntry : runLibOnBoard.entrySet()) {
			if (Arrays.asList(curEntry.getValue()).contains(libName)) {
				return curEntry.getKey();
			}
		}
		return null;

	}
}
