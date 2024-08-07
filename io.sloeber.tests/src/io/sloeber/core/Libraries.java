package io.sloeber.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.IPath;

import io.sloeber.providers.Adafruit;
import io.sloeber.providers.Arduino;
import io.sloeber.providers.ESP32;
import io.sloeber.providers.Teensy;

@SuppressWarnings("nls")
public class Libraries {

	public static LinkedList<String> doNotUseThisLib() {
		LinkedList<String> ret = new LinkedList<>();
		// the Wifi lib uses SPI but holds a spi.h file in the extras
		// though the file does not match case and it is excluded
		// the indexer is fine with it resulting
		// that SPI.h is not a unresolved exclude
		ret.add("WiFi");

		ret.add("ACROBOTIC_SSD1306");
		ret.add("XLR8Servo");
		ret.add("Adafruit_CC3000_Library");
		ret.add("Adafruit_HX8340B");
		ret.add("Adafruit_IO_Arduino");
		ret.add("Adafruit_MQTT_Library");
		ret.add("Adafruit_SPIFlash");
		ret.add("Adafruit_SSD1325");
		ret.add("ArdBitmap");
		ret.add("ArdOSC");
		ret.add("Arduino-Websocket-Fast");
		ret.add("ArduinoFacil");
		ret.add("ArduinoMenu_library");
		ret.add("ArduinoSensors");
		ret.add("ArduinoSerialToTCPBridgeClient");
		ret.add("ArduinoUnit");
		ret.add("arduinoVNC");
		ret.add("ArduZ80");
		ret.add("AS3935");
		ret.add("AzureIoTHubMQTTClient");
		ret.add("BigCrystal");
		ret.add("Babelduino");
		ret.add("Blynk");
		ret.add("Brief");
		ret.add("Brzo_I2C");
		ret.add("BTLE");
		ret.add("Cayenne");
		ret.add("CayenneMQTT");
		ret.add("Chronos");
		ret.add("CoAP_simple_library");
		ret.add("Comp6DOF_n0m1");
		ret.add("Constellation");
		ret.add("CRC_Simula_Library");
		ret.add("Cytron_3A_Motor_Driver_Shield");
		ret.add("DoubleResetDetector");
		ret.add("DCF77");
		ret.add("dcf77_xtal");
		ret.add("DW1000");
		ret.add("EDB");
		ret.add("eBtn");
		ret.add("AJSP");
		ret.add("EducationShield");
		ret.add("ArduinoMqtt");
		ret.add("Embedded_Template_Library");
		ret.add("Embedis");
		ret.add("EMoRo_2560");
		ret.add("Adafruit_microbit_Library");
		ret.add("GPRSbee");
		ret.add("hd44780");
		ret.add("RTC");
		/*
		 * AceButton uses namespace in ino files with is not compatible with sloeber ino
		 * implementation tomake it work remove using namespace and add AceButton:: as
		 * needed
		 */
		ret.add("AceButton");
		// at the time of testing seesaw is not supported on gnu
		ret.add("seesaw");
		// Needs a library not provided by default jsons
		ret.add("AGirs");
		ret.add("AlertMe");
		// Defines need to be changed in the lib
		ret.add("Adafruit_SSD1306_Wemos_Mini_OLED");
		// uses unknown libraries tc.h and tc_interrupt.h
		ret.add("Adafruit_ZeroTimer_Library");
		ret.add("avdweb_SAMDtimer");
		// uses unknown lib CurieBLE
		ret.add("Andee101");
		// uses unknonw lib ThingerESP826
		ret.add("ClimaStick");

		// this fails in the combiner on windows for some strange reason.
		// I think it is a windows command line limitation again
		ret.add("ANTPLUS-Arduino");
		// post parser is 2 times in the code
		ret.add("Arduino_POST_HTTP_Parser");
		// no websocket client
		ret.add("ArduinoArcherPanelClient");
		// missing httpclient
		ret.add("ArduinoFritzApi");
		// uncomment something in my own library to make it work... are you kidding?
		ret.add("DCCpp");
		ret.add("Commanders");
		// Macro defenitions of setup and loop confusing the ino.cpp converter
		ret.add("ArduinoLang");
		// all kind of issue
		ret.add("ArduinoOSC");
		// usage of macro's confusing ino to cpp converter
		ret.add("AUnit");
		// contains REPLACE_ME
		ret.add("CayenneLPP");
		// uses unknown lib powerbank
		ret.add("Charge_n_Boost");
		// uses mbino that uses SPI but has SPI included so
		// indexer thinks SPI is defines but utilities is not in include path
		ret.add("CarreraDigitalControlUnit");
		// I don't know which hardware to take for this lib
		ret.add("CoDrone");
		// uses a define that is not defined
		ret.add("DcDccNanoController");
		// requires Paul Stoffregens time lib and Sloeber takes arduino time lib
		ret.add("DS3232RTC");
		// Cloud4RPi.cpp:179:5: error: 'DynamicJsonBuffer' was not declared in this
		// scope
		ret.add("cloud4rpi-esp-arduino");
		// not sure what is wrong. Don't feel like looking
		ret.add("AceRoutine");
		ret.add("Arduino_Low_Power");
		// source is in scr instead of src
		ret.add("ADC_SEQR");
		// uses StaticJsonBuffer I do not have
		ret.add("AmazonDRS");
		// to complicated board selection
		ret.add("Adafruit_DAP_library");
		// fails as if wrong board tried zero and feater MO
		ret.add("Adafruit_Zero_I2S_Library");
		// to hard to assign boards correctly
		ret.add("ArduinoBearSSL");
		// call of overloaded 'requestFrom(uint8_t, size_t, bool)' is ambiguous
		ret.add("ArduinoECCX08");
		// getRequest.ino:29:45: error: no matching function for call to
		// 'ESPAT::get(const char [16])'
		ret.add("ArduinoESPAT");
		// ESPAsyncE131.h:23:25: fatal error: ESPAsyncUDP.h: No such file or directory
		ret.add("ESP_Async_E1.31");
		// I think this needs a can bus
		ret.add("Adafruit_CAN");
		// Needs special board
		ret.add("Adafruit_composite_video_Library");
		ret.add("Arduboy");
		ret.add("Arduboy2");
		// needs special setting
		ret.add("Adafruit_CPFS");

		ret.add("Adafruit_InternalFlash");
		// uses unknown lib
		ret.add("Adafruit_LittlevGL_Glue_Library");
		// doesn't work in arduino ide
		ret.add("AIOModule");
		// need to fix issue
		ret.add("Blynk_For_Chinese");
		ret.add("AcksenUtils");
		ret.add("AD7173");
		ret.add("Adafruit_Arcada_GifDecoder");
		return ret;

	}

	private static LinkedList<String> libsUsingSerial() {
		LinkedList<String> ret = new LinkedList<>();
		ret.add("FreeRTOS");
		ret.add("GSM");
		ret.add("CapacitiveSensor");
		ret.add("Ethernet");
		ret.add("Stepper");
		return ret;
	}

	private static LinkedList<String> libsUsingBuildInLed() {
		LinkedList<String> ret = new LinkedList<>();
		ret.add("FreeRTOS");
		ret.add("GSM");
		return ret;
	}

	private static LinkedList<String> libsUsingSD() {
		LinkedList<String> ret = new LinkedList<>();
		ret.add("SD");
		return ret;
	}

	private static LinkedList<String> libsUsingRX_TX_PIN() {
		LinkedList<String> ret = new LinkedList<>();
		ret.add("GSM");
		return ret;
	}

	private static LinkedList<String> libsUsingPinToPCICR() {
		LinkedList<String> ret = new LinkedList<>();
		ret.add("Firmata");
		ret.add("SoftwareSerial");
		return ret;
	}

	private static LinkedList<String> libsUsingUSBCON() {
		LinkedList<String> ret = new LinkedList<>();
		ret.add("HID-Project");
		ret.add("Keyboard");
		return ret;
	}

	public static AttributesCode getCodeAttributes(IPath path) throws Exception {
		AttributesCode ret = new AttributesCode();

		ret.myCompatibleBoardIDs.add(getRequiredBoardID(path.lastSegment()));
		ret.myCompatibleArchitectures.addAll(getSupportedArchitectures(path));
		String libName = path.segment(path.segmentCount() - 4);
		if ("libraries".equals(libName)) {
			libName = path.segment(path.segmentCount() - 3);
		}

		ret.worksOutOfTheBox = !doNotUseThisLib().contains(libName);
		ret.serial = libsUsingSerial().contains(libName);
		ret.buildInLed = libsUsingBuildInLed().contains(libName);
		ret.SD = libsUsingSD().contains(libName);
		ret.RX_TX_PIN = libsUsingRX_TX_PIN().contains(libName);
		ret.digitalPinToPCICR = libsUsingPinToPCICR().contains(libName);
		ret.USBCON = libsUsingUSBCON().contains(libName);

		ret.myCompatibleArchitectures.remove(null);
		ret.myCompatibleBoardIDs.remove(null);
		ret.myCompatibleArchitectures.remove("*");
		return ret;
	}

	private static Set<String> getSupportedArchitectures(IPath path) throws Exception {
		Set<String> ret = new HashSet<>();
		File libFile = path.append("library.properties").toFile();
		if (libFile.exists()) {
			try (FileReader fileReader = new FileReader(libFile)) {
				try (BufferedReader bufferedReader = new BufferedReader(fileReader)) {
					String line = null;
					while ((line = bufferedReader.readLine()) != null) {
						if (line.startsWith("architectures=")) {
							for (String curArch : line.split("=", 2)[1].split(",")) {
								ret.add(curArch);
							}
						}
					}
					bufferedReader.close();
				}
			}
		}
		return ret;
	}

	// /*
	// * M0 microcontrollers (Feather M0, Arduino Zero, etc.)
	// */
	// private static boolean mo_mcu(String libName) {
	// String theLibs[] = new String[] { "Adafruit_composite_video_Library",
	// "Adafruit_DMA_neopixel_library",
	// "Adafruit_FreeTouch_Library" };
	// return Arrays.asList(theLibs).contains(libName);
	// }

	// /*
	// * esp8266 libraries
	// */
	// private static boolean esp8266_mcu(String libName) {
	// String theLibs[] = new String[] { "Adafruit_ESP8266", "BoodskapTransceiver",
	// "AutoConnect", "ClimaStick",
	// "CMMC_Interval", "ConfigManager", "CoogleIOT", "CTBot" };
	// return Arrays.asList(theLibs).contains(libName);
	// }

	private static String getRequiredBoardID(String libName) {
		if (libName == null)
			return null;

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
		runLibOnBoard.put(Arduino.circuitplay32ID, new String[] { "Adafruit_Circuit_Playground",
				"Adafruit_BluefruitLE_nRF51", "Adafruit_GPS_Library", "Adafruit_composite_video_Library" });
		runLibOnBoard.put("nodemcu",
				new String[] { "Adafruit_IO_Arduino", "CMMC_Easy", "CMMC_MQTT_Connector", "CMMC_OTA",
						"CMMC_WiFi_Connector", "EasyUI", "EasyDDNS", "CoinMarketCapApi", "ArduinoIHC", "AsciiMassage",
						"ESPiLight", "HaLakeKit", "HaLakeKitFirst", "AFArray", "AIOModule", "AlertMe", "Bleeper",
						"Blinker" });
		runLibOnBoard.put("feather52", new String[] { "Firmata", "Codec2" });
		runLibOnBoard.put("primo", new String[] { "Adafruit_BluefruitLE_nRF51", "arduino-NVM" });
		runLibOnBoard.put("mega",
				new String[] { "Adafruit_GPS_Library", "Dynamixel_Servo", "EnergyBoard", "DFL168A_Async" });
		runLibOnBoard.put("zero",
				new String[] { "Arduino_Low_Power", "ArduinoSound", "AudioZero", "Dimmer_class_for_SAMD21",
						"AzureIoTHub", "AzureIoTProtocol_HTTP", "AzureIoTProtocol_MQTT", "Adafruit_AM_radio_library" });
		runLibOnBoard.put("mkrfox1200", new String[] { "Arduino_SigFox_for_MKRFox1200" });
		runLibOnBoard.put("due",
				new String[] { "Audio", "AutoAnalogAudio", "dcf77_xtal", "due_can", "DueFlashStorage", "DueTimer" });
		runLibOnBoard.put("espresso_lite_v2", new String[] { "ESPert", "ESPectro" });
		runLibOnBoard.put(ESP32.esp32ID, new String[] { "EasyBuzzer_Beep_leonardo", "ESPUI", "Basecamp" });
		runLibOnBoard.put(Teensy.Teensy3_6_ID, new String[] { "ACAN", "ACAN2515", "ACAN2517" });

		runLibOnBoard.put(Adafruit.metroM4ID, new String[] { "Adafruit_QSPI", "Adafruit_mp3" });
		runLibOnBoard.put(Arduino.ethernetID, new String[] { "Blynk_For_Chinese" });

		for (Entry<String, String[]> curEntry : runLibOnBoard.entrySet()) {
			if (Arrays.asList(curEntry.getValue()).contains(libName)) {
				return curEntry.getKey();
			}
		}
		return null;

	}
}
