package io.sloeber.core.boards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import io.sloeber.core.Example;
import io.sloeber.core.api.BoardDescriptor;

@SuppressWarnings("nls")
public abstract class IBoard {

	protected BoardDescriptor myBoardDescriptor = null;
	protected List<String> doNotTestTheseSketches;
	protected List<String> doNotTestTheseLibs;
	private boolean mySupportSerial = true;
	private boolean mySupportSerial1 = false;
	private boolean mySupportKeyboard = false;
	private boolean mySupportFlightSim = false;
	private boolean mySupportMidi = false;
	private static final boolean log = true;

	void setSupportSerial(boolean supportSerial) {
		this.mySupportSerial = supportSerial;
	}

	void setSupportSerial1(boolean supportSerial1) {
		this.mySupportSerial1 = supportSerial1;
	}

	void setSupportKeyboard(boolean supportKeyboard) {
		this.mySupportKeyboard = supportKeyboard;
	}

	public BoardDescriptor getBoardDescriptor() {
		return this.myBoardDescriptor;
	}

	/**
	 * Does this board support Serial object This method is made final to disable
	 * overloading To change use the set method The only board I know that doesn't
	 * is the gemma
	 *
	 * @return true if it does otherwise false
	 */
	public final boolean supportsSerial() {
		return this.mySupportSerial;
	}

	/**
	 * Does this board support Serial1 object This method is made final to disable
	 * overloading To change use the set method
	 *
	 * @return true if it does otherwise false
	 */
	public final boolean supportsSerial1() {
		return this.mySupportSerial1;
	}

	/**
	 * Does this board support keyboard.h This method is made final to disable
	 * overloading To change use the set method
	 *
	 * @return true if inclusion of keyboard.h compiles otherwise false
	 */
	public final boolean supportsKeyboard() {
		return this.mySupportKeyboard;
	}

	public boolean supportsFlightSim() {
		return this.mySupportFlightSim;
	}

	public boolean supportsusesMidi() {
		return this.mySupportMidi;
	}

	public boolean isExampleSupported(Example example) {
		if (this.myBoardDescriptor == null) {
			return false;
		}
		// make the' lists for this boards once
		if (this.doNotTestTheseSketches == null) {
			createDoNotTestTheseSketches();
			createDoNotTestTheseLibs();

		}
		if (this.doNotTestTheseLibs.contains(example.getLibName())) {
			return false;
		}

		if (this.doNotTestTheseSketches.contains(example.getInoName())) {
			return false;
		}
		if (example.getLibName()!=null&&example.getLibName().toLowerCase().contains("seesaw")) {
			// at the time of testing seesaw is not supported on gnu
			return false;
		}

		boolean ret = matches(example.getFQN(), getName(), "usesSerial", example.UsesSerial(), supportsSerial());
		ret = ret && matches(example.getFQN(), getName(), "usesSerial1", example.UsesSerial1(), supportsSerial1());
		ret = ret && matches(example.getFQN(), getName(), "usesKeyboard", example.UsesKeyboard(), supportsKeyboard());
		ret = ret
				&& matches(example.getFQN(), getName(), "usesFlightSim", example.UsesFlightSim(), supportsFlightSim());
		ret = ret && matches(example.getFQN(), getName(), "usesMidi", example.UsesMidi(), supportsusesMidi());
		if ("Teensy".equalsIgnoreCase(getName())) {
			if (example.getFQN().contains("Teensy? USB_Mouse?Buttons")) {
				String boardID = myBoardDescriptor.getBoardID();
				if ("teensypp2".equals(boardID) || "teensy2".equals(boardID)) {
					return false;
				}
			}
		}
		return ret;
	}

	private static boolean matches(String exampleName, String boardName, String fieldName, boolean val1, boolean val2) {
		if (val1 && !val2) {
			if (log) {
				System.out.println("!Example " + exampleName + " SKIPPED on " + boardName + " due to " + fieldName);
			}
			return false;
		}
		return true;
	}

	private void createDoNotTestTheseSketches() {
		this.doNotTestTheseSketches = new ArrayList<>();
		Map<String, String[]> runSketchOnBoard = new HashMap<>();

		runSketchOnBoard.put("no Board", new String[] {
				"AD7193 examples?AD7193_VoltageMeasurePsuedoDifferential_Example", "bunny_cuberotate?cuberotate",
				"XPT2046_Touchscreen?ILI9341Test", "Adafruit_AHRS examples?ahrs_mahony",
				"Adafruit_BLEFirmata examples?StandardFirmata",
				"Adafruit_BNO055 examples? bunny? processing?cuberotate",
				"Adafruit_GPS_Library examples?due_shield_sdlog",
				"Adafruit_Graphic_VFD_Display_Library examples?GraphicVFDtest",
				"Adafruit_GPS_Library examples?locus_erase", "Adafruit_GPS_Library examples?shield_sdlog",
				"Adafruit_HX8357_Library examples?breakouttouchpaint", "Adafruit_ILI9341 examples?breakouttouchpaint",
				"Adafruit_ILI9341 examples?onoffbutton_breakout", "Adafruit_GPS_Library examples?echo",
				"Adafruit_LED_Backpack_Library examples?wavface", "Adafruit_SSD1306 examples?ssd1306_128x64_i2c",
				"Adafruit_SSD1306 examples?ssd1306_128x64_spi", "Adafruit_ST7735_Library examples?soft_spitftbitmap",
				"Adafruit_TCS34725 examples? colorview? processing?colorview",
				"Adafruit_Circuit_Playground examples?Infrared_Send",
				"Adafruit_TinyRGBLCDShield examples?TinyHelloWorld",
				"Akafugu_TWILiquidCrystal_Library examples?change_address", "Akafugu_WireRtc_Library examples?alarm",
				"ALA examples?RgbStripButton", "APA102 examples?GameOfLife", "arduino-menusystem examples?led_matrix",
				"arduino-menusystem examples?led_matrix_animated", "Arduino_Low_Power examples?TianStandby",
				"aREST examples?BLE", "aREST examples?ESP32", "aREST examples?ESP32_cloud",
				"ArduinoHttpClient examples?DweetGet", "ArduinoMenu_library examples? adafruitGfx? lcdMono?lcdMono",
				"ArduinoMenu_library examples? adafruitGfx? tft?tft", "ArdVoice examples?Sample2-Complex",
				"Aspen_SIM800 examples?Access_HTTP", "Awesome examples? advanced?how_fast",
				"Awesome examples? advanced?lie_detector", "AzureIoTUtility examples?simplesample_http",
				"BLEPeripheral examples?ir_bridge", "BLEPeripheral examples?temp_sensor",
				"Brasilino examples? Basicos?controleGradual", "ClosedCube_HDC1010 examples?hdc1010demo",
				"Chrono examples?Resolutions", "Chrono examples?StopResume",
				"ConfigurableFirmata examples?ConfigurableFirmataWiFi", "ControleForno examples?configuravel",
				"CopyThreads examples?c", "ArduinoCloud examples?SimpleCloudButtonYun", "Brzo_I2Cexamples",
				"CopyThreads examples?ExamplesFromReadme", "DallasTemperature examples?Multibus_simple",
				"DecodeIR examples?InfraredDecode", "AutoAnalogAudio examples?SimpleSine",
				"DimSwitch examples?DimSwitchTester-ESP-MQTT", "DS3231 examples?echo_time", "Easy_NeoPixelsexamples",
				"DallasTemperature examples?AlarmHandler", "ADCTouchSensor examples?CapacitiveController",
				"AmazonDRS examples?amazonDashNfc",
				"Andee examples? Lesson_02_Buttons?Lesson_2h_Using_Buttons_to_Control_Servos",
				"Andee examples?Project_Christmas_Lights_and_Annoying_Music",
				"Andee examples?Project_Rubber_Band_Launcher", "Andee examples?Project_Time_Automated_Data_Logger",
				"ANT-Arduino_library examples?NativeAnt", "arduino-fsm examples?timed_switchoff",
				"BME280 examples?BME_280_BRZO_I2C_Test"

		});
		runSketchOnBoard.put("fix case Sensitive include first",
				new String[] { "AutoAnalogAudio examples? SDAudio?SdAudioRecording",
						"AutoAnalogAudio examples? SDAudio?SdAudioWavPlayer",
						"AutoAnalogAudio examples?AudioRadioRelay", "AutoAnalogAudio examples?WirelessMicrophone",
						"AutoAnalogAudio examples?WirelessSpeaker", "AutoAnalogAudio examples?WirelessTx_RPi" });

		runSketchOnBoard.put("nodemcu", new String[] { "YouMadeIt examples?basic_example",
				// "ConfigManagerexamples",
				"DimSwitch examples?DimSwitchTester-ESP", "AIOModule examples?handling-module",
				"AIOModule examples?mobility-module", "AlertMe examples?email_and_sms"

		});
		runSketchOnBoard.put("gemma", new String[] { "Adafruit_MiniMLX90614 examples?templight",
				"Adafruit_TiCoServo examples?TiCoServo_Test_Trinket_Gemma" });
		runSketchOnBoard.put("primo",
				new String[] { "Arduino_Low_Power examples?PrimoDeepSleep", "BLEPeripheral examples?iBeacon" });
		// runSketchOnBoard.put("trinket",
		// new String[] { "Adafruit_SoftServo examples?TrinketKnob",
		// "Adafruit_TiCoServo examples?TiCoServo_Test_Trinket_Gemma",
		// "Adafruit_TinyFlash examples?TrinketPlayer" });
		runSketchOnBoard.put("zero", new String[] { "ArduinoCloud examples?ReadAndWrite",
				"ArduinoCloud examples?SimpleCloudButton", "AudioFrequencyMeter examples?SimpleAudioFrequencyMeter" });
		runSketchOnBoard.put("due", new String[] { "ArduinoThread examples?SensorThread",
				"Adafruit_BME280_Library examples?advancedsettings" });
		runSketchOnBoard.put("mega", new String[] { "aREST_UI examples?WiFi_CC3000" });
		runSketchOnBoard.put("wildfire", new String[] { "aREST_UI examples?WildFire" });
		runSketchOnBoard.put("circuitplay32u4catexpress",
				new String[] { "Adafruit_Circuit_Playground examples?Infrared_NeoPixel",
						"Adafruit_Circuit_Playground examples?Infrared_Read",
						"Adafruit_Circuit_Playground examples?Infrared_Record",
						"Adafruit_Circuit_Playground examples?Infrared_Testpattern" });
		runSketchOnBoard.put("unowifi", new String[] { "Braccio examples?braccioOfUnoWiFi" });
		runSketchOnBoard.put("uno",
				new String[] { "Accessory_Shield examples?OLED_example_Adafruit",
						"Accessory_Shield examples?temp_humidity_oled", "AFArray examples?GetFromIndex",
						"AFArray examples?ImplodeExplode", "Adafruit_ESP8266 examples?webclient" });

		for (Entry<String, String[]> curEntry : runSketchOnBoard.entrySet()) {
			if (!getName().equals(curEntry.getKey())) {
				this.doNotTestTheseSketches.addAll(Arrays.asList(curEntry.getValue()));
			}
		}

	}

	private void createDoNotTestTheseLibs() {
		this.doNotTestTheseLibs = new ArrayList<>();
		Map<String, String[]> runLibOnBoard = new HashMap<>();

		runLibOnBoard.put("no board",
				new String[] { "Arduino_Commanderexamples", "ALog", "BPLib", "ESP8266_Microgear",
						"ESP8266_Weather_Station", "Gamebuino", "Gadgetron_Libraries", "GadgetBox",
						"ESP8266_Oled_Driver_for_SSD1306_display", "FrequencyTimer2", "Gamer", "ghaemShopSmSim" });
		runLibOnBoard.put("fix issue first",
				new String[] { "DS1307RTC", "ESPMail", "EspSaveCrash", "FlashStorage", "Fram", "Geometry" });
		runLibOnBoard.put("macro's confuse indexer", new String[] { "EnableInterrupt", "Eventually" });

		runLibOnBoard.put("uno",
				new String[] { "A4963", "Adafruit_Motor_Shield_library", "Adafruit_Motor_Shield_library_V2",
						"AccelStepper", "Arduino_Uno_WiFi_Dev_Ed_Library", "ardyno", "AVR_Standard_C_Time_Library",
						"DDS", "EtherSia", "ExodeCore", "FingerLib", "HamShield" });
		runLibOnBoard.put("esplora", new String[] { "Esplora" });
		runLibOnBoard.put("circuitplay32u4cat",
				new String[] { "Adafruit_Circuit_Playground", "Adafruit_BluefruitLE_nRF51", "Adafruit_GPS_Library" });
		runLibOnBoard.put("nodemcu",
				new String[] { "Adafruit_IO_Arduino", "CMMC_Easy", "CMMC_MQTT_Connector", "CMMC_OTA",
						"CMMC_WiFi_Connector", "EasyUI", "EasyDDNS", "CoinMarketCapApi", "ArduinoIHC", "AsciiMassage",
						"ESPiLight", "HaLakeKit", "HaLakeKitFirst" });
		runLibOnBoard.put("feather52", new String[] { "Firmata" });
		runLibOnBoard.put("primo", new String[] { "Adafruit_BluefruitLE_nRF51", "arduino-NVM" });
		runLibOnBoard.put("mega", new String[] { "Adafruit_GPS_Library", "Dynamixel_Servo", "EnergyBoard" });
		runLibOnBoard.put("zero", new String[] { "Arduino_Low_Power", "ArduinoSound", "AudioZero",
				"Dimmer_class_for_SAMD21", "AzureIoTHub", "AzureIoTProtocol_HTTP", "AzureIoTProtocol_MQTT" });
		runLibOnBoard.put("mkrfox1200", new String[] { "Arduino_SigFox_for_MKRFox1200" });
		runLibOnBoard.put("due",
				new String[] { "Audio", "AutoAnalogAudio", "dcf77_xtal", "due_can", "DueFlashStorage", "DueTimer" });
		runLibOnBoard.put("espresso_lite_v2", new String[] { "ESPert", "ESPectro" });
		runLibOnBoard.put("esp32", new String[] { "EasyBuzzer_Beep_leonardo", "ESPUI" });

		for (Entry<String, String[]> curEntry : runLibOnBoard.entrySet()) {
			if (!getName().equals(curEntry.getKey())) {
				this.doNotTestTheseLibs.addAll(Arrays.asList(curEntry.getValue()));
			}
		}
	}

	public String getName() {
		if (this.myBoardDescriptor == null) {
			return null;
		}
		return this.myBoardDescriptor.getBoardID();
	}

	/**
	 * Give a list of boards pick the board that is best to test this code Boards in
	 * the beginning of the array are prefered (first found ok algorithm)
	 *
	 * returns null if this code should not be tested return null if myBoards is
	 * empty returns the best known boarddescriptor to run this example
	 */

	public static BoardDescriptor pickBestBoard(Example example, IBoard myBoards[]) {
		String libName=example.getLibName();
		String inoName=example.getInoName();
		if (myBoards.length == 0) {
			return null;
		}
		if (neverTestThisLib(libName)) {
			return null;
		}
		if (neverTestThisExample(example)) {
			return null;
		}

		// if the boardname is in the libname or ino name pick this one
		for (IBoard curBoard : myBoards) {
			String filter = curBoard.getName().toLowerCase();
			if (libName.toLowerCase().contains(filter) || inoName.toLowerCase().contains(filter)) {
				if (curBoard.isExampleSupported(example)) {
					return curBoard.getBoardDescriptor();
				}
			}
		}
		// If the archtecture is in the libname or boardname pick this one
		for (IBoard curBoard : myBoards) {
			String filter = curBoard.getBoardDescriptor().getArchitecture().toLowerCase();
			if (libName.toLowerCase().contains(filter) || inoName.toLowerCase().contains(filter)) {
				if (curBoard.isExampleSupported(example)) {
					return curBoard.getBoardDescriptor();
				}
			}
		}
		/*
		 * look for well known names that identify the board that should be used in the
		 * lib and ino name to skip in case these boards are not in the list of boards
		 */
		String commonlyUsedIDNames[] = new String[] { "trinket", "esp8266", "esp32", "Goldilocks" };
		for (String curBoardName : commonlyUsedIDNames) {
			if (libName.toLowerCase().contains(curBoardName) || inoName.toLowerCase().contains(curBoardName)) {
				/*
				 * This examplename or libname contains the name of a
				 * board/techology/architecture commonly used in lib- and example names and such
				 * a board is not provided
				 */
				return null;

			}
		}
		// Out of guesses based on the name. Take the first ok one
		for (IBoard curBoard : myBoards) {
			if (curBoard.isExampleSupported(example)) {
				return curBoard.getBoardDescriptor();
			}
		}
		return null;

	}

	private static boolean neverTestThisLib(String libName) {
		String skipLibs[] = new String[] { "ACROBOTIC_SSD1306", "XLR8Servo", "Adafruit_CC3000_Library",
				"Adafruit_HX8340B", "Adafruit_IO_Arduino", "Adafruit_MQTT_Library", "Adafruit_SPIFlash",
				"Adafruit_SSD1325", "ArdBitmap", "ArdOSC", "Arduino-Websocket-Fast", "ArduinoFacil",
				"ArduinoMenu_library", "ArduinoSensors", "ArduinoSerialToTCPBridgeClient", "ArduinoUnit", "arduinoVNC",
				"ArduZ80", "AS3935", "AzureIoTHubMQTTClient", "BigCrystal", "Babelduino", "Blynk", "Brief", "Brzo_I2C",
				"BTLE", "Cayenne", "CayenneMQTT", "Chronos", "CoAP_simple_library", "Comp6DOF_n0m1", "Constellation",
				"CRC_Simula_Library", "Cytron_3A_Motor_Driver_Shield", "DoubleResetDetector", "DCF77", "dcf77_xtal",
				"DW1000", "EDB", "eBtn", "AJSP", "EducationShield", "ArduinoMqtt", "Embedded_Template_Library",
				"Embedis", "EMoRo_2560", "Adafruit_microbit_Library", "GPRSbee", "hd44780" };
		return Arrays.asList(skipLibs).contains(libName);
	}

	/**
	 * if the example fails it is known not to compile(under sloeber?)
	 *
	 * @param inoName
	 * @param libName
	 * @return
	 */
	private static boolean neverTestThisExample(Example example) {
		final String[] skipIno = { "AD7193_VoltageMeasurePsuedoDifferential_Example", "bunny_cuberotate?cuberotate",
				"XPT2046_Touchscreen?ILI9341Test" };
		String inoName = example.getInoName();
		if (Arrays.asList(skipIno).contains(inoName.replace(" ", "")))
			return true;
		if (inoName.endsWith("AD7193_VoltageMeasurePsuedoDifferential_Example"))
			return true;
		// following examples also fail in Arduino IDE at the time of writing
		// these unit tests
		if (inoName.endsWith("ahrs_mahony")
				|| ("Adafruit_BLEFirmata".equals(example.getLibName()) && inoName.endsWith("StandardFirmata"))) {
			return true;
		}
		return false; // default everything is fine so don't skip
	}

	@SuppressWarnings("static-method")
	public Map<String, String> getBoardOptions(Example example) {
		 Map<String, String>  ret =new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		 if(example.getFQN().contains("Teensy? USB_FlightSim")) {
			 // it is a teensy and you need to set the usb to flightsim
			 ret.put("usb", "flightsim");
		 }
		 if(example.getFQN().contains("Teensy? USB_Mouse")) {
			 // it is a teensy and you need to set the usb to flightsim
			 ret.put("usb", "serialhid");
		 }
		 if(example.getFQN().contains("Teensy? USB_RawHID")) {
			 // it is a teensy and you need to set the usb to flightsim
			 ret.put("usb", "rawhid");
		 }
		return ret;
	}

}