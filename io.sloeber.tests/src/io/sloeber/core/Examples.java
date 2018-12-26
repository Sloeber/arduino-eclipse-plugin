package io.sloeber.core;

import java.util.LinkedList;

import org.eclipse.core.runtime.IPath;

import io.sloeber.providers.ESP32;
import io.sloeber.providers.MCUBoard;
import io.sloeber.providers.Teensy;

@SuppressWarnings("nls")
public class Examples {
	private String myFQN;
	private String myLibName;
	private IPath myPath;
	private BoardAttributes myRequiredBoardAttributes;
	private static int noBoardFoundCount = 0;

	public BoardAttributes getRequiredBoardAttributes() {
		return myRequiredBoardAttributes;
	}

	public Examples(String fqn, IPath path) {
		myFQN = fqn;
		myPath = path;
		getLibNameFromPath();
		myRequiredBoardAttributes = new BoardAttributes();
		myRequiredBoardAttributes.serial = examplesUsingSerial().contains(myFQN);
		myRequiredBoardAttributes.serial1 = examplesUsingSerial1().contains(myFQN);
		myRequiredBoardAttributes.keyboard = examplesUsingKeyboard().contains(myFQN);
		myRequiredBoardAttributes.flightSim = examplesUsingFlightSim().contains(myFQN);
		myRequiredBoardAttributes.joyStick = examplesUsingJoyStick().contains(myFQN);
		myRequiredBoardAttributes.mouse = examplesUsingMouse().contains(myFQN);
		myRequiredBoardAttributes.wire1 = examplesUsingWire1().contains(myFQN);
		myRequiredBoardAttributes.midi = examplesUsingMidi().contains(myFQN) || myFQN.contains("USB_MIDI");
		myRequiredBoardAttributes.teensy = myFQN.startsWith("Example/Teensy");
		myRequiredBoardAttributes.worksOutOfTheBox = !failingExamples().contains(myFQN);
		myRequiredBoardAttributes.boardName = getRequiredBoardID(myFQN);
		myRequiredBoardAttributes.mo_mcu = examplesUsingMCUmo().contains(fqn);
		myRequiredBoardAttributes.rawHID = myFQN.contains("USB_RawHID");
		myRequiredBoardAttributes = myRequiredBoardAttributes.or(Libraries.getRequiredBoardAttributes(getLibName()));
	}

	private void getLibNameFromPath() {
		myLibName = new String();
		String[] splits = myFQN.split("/");
		if (splits.length >= 2) {
			if ("Library".equals(splits[0])) {
				myLibName = splits[1];
			}
		}
	}

	public IPath getPath() {
		return myPath;
	}

	public String getLibName() {
		return myLibName;
	}

	public String getFQN() {
		return myFQN;
	}

	public String getInoName() {
		return myPath.lastSegment();
	}

	private static LinkedList<String> examplesUsingMidi() {
		LinkedList<String> myUsesMidiExampleList = new LinkedList<>();
		// myUsesMidiExampleList.add("Example/Teensy/USB_FlightSim/ThrottleServo");
		return myUsesMidiExampleList;
	}

	private static LinkedList<String> examplesUsingFlightSim() {
		LinkedList<String> ret = new LinkedList<>();
		ret.add("Example/Teensy/USB_FlightSim/BlinkTransponder");
		ret.add("Example/Teensy/USB_FlightSim/FrameRateDisplay");
		ret.add("Example/Teensy/USB_FlightSim/NavFrequency");
		ret.add("Example/Teensy/USB_FlightSim/ThrottleServo");
		return ret;
	}

	private static LinkedList<String> examplesUsingSerial1() {
		LinkedList<String> ret = new LinkedList<>();
		ret.add("Example/04.Communication/MultiSerial");
		ret.add("Example/04.Communication/SerialPassthrough");
		ret.add("Example/Teensy/Serial/EchoBoth");
		return ret;
	}

	private static LinkedList<String> examplesUsingKeyboard() {
		LinkedList<String> ret = new LinkedList<>();
		ret.add("Example/09.USB/Keyboard/KeyboardLogout");
		ret.add("Example/09.USB/Keyboard/KeyboardMessage");
		ret.add("Example/09.USB/Keyboard/KeyboardReprogram");
		ret.add("Example/09.USB/Keyboard/KeyboardSerial");
		ret.add("Example/09.USB/Mouse/ButtonMouseControl");
		ret.add("Example/09.USB/Mouse/JoystickMouseControl");
		ret.add("Example/09.USB/KeyboardAndMouseControl");
		return ret;
	}

	private static LinkedList<String> examplesUsingSerial() {
		LinkedList<String> ret = new LinkedList<>();
		ret.add("Example/01.Basics/AnalogReadSerial");
		ret.add("Example/01.Basics/DigitalReadSerial");
		ret.add("Example/01.Basics/ReadAnalogVoltage");
		ret.add("Example/02.Digital/DigitalInputPullup");
		ret.add("Example/02.Digital/StateChangeDetection");
		ret.add("Example/02.Digital/tonePitchFollower");
		ret.add("Example/03.Analog/AnalogInOutSerial");
		ret.add("Example/03.Analog/Smoothing");
		ret.add("Example/04.Communication/ASCIITable");
		ret.add("Example/04.Communication/Dimmer");
		ret.add("Example/04.Communication/Graph");
		ret.add("Example/04.Communication/Midi");
		ret.add("Example/04.Communication/PhysicalPixel");
		ret.add("Example/04.Communication/ReadASCIIString");
		ret.add("Example/04.Communication/SerialCallResponse");
		ret.add("Example/04.Communication/SerialCallResponseASCII");
		ret.add("Example/04.Communication/SerialEvent");
		ret.add("Example/04.Communication/VirtualColorMixer");
		ret.add("Example/05.Control/IfStatementConditional");
		ret.add("Example/05.Control/switchCase");
		ret.add("Example/05.Control/switchCase2");
		ret.add("Example/06.Sensors/ADXL3xx");
		ret.add("Example/06.Sensors/Knock");
		ret.add("Example/06.Sensors/Memsic2125");
		ret.add("Example/06.Sensors/Ping");
		ret.add("Example/08.Strings/CharacterAnalysis");
		ret.add("Example/08.Strings/StringAdditionOperator");
		ret.add("Example/08.Strings/StringAppendOperator");
		ret.add("Example/08.Strings/StringCaseChanges");
		ret.add("Example/08.Strings/StringCharacters");
		ret.add("Example/08.Strings/StringComparisonOperators");
		ret.add("Example/08.Strings/StringConstructors");
		ret.add("Example/08.Strings/StringIndexOf");
		ret.add("Example/08.Strings/StringLength");
		ret.add("Example/08.Strings/StringLengthTrim");
		ret.add("Example/08.Strings/StringReplace");
		ret.add("Example/08.Strings/StringStartsWithEndsWith");
		ret.add("Example/08.Strings/StringSubstring");
		ret.add("Example/08.Strings/StringToInt");
		ret.add("Example/10.StarterKit_BasicKit/p03_LoveOMeter");
		ret.add("Example/10.StarterKit_BasicKit/p04_ColorMixingLamp");
		ret.add("Example/10.StarterKit_BasicKit/p05_ServoMoodIndicator");
		ret.add("Example/10.StarterKit_BasicKit/p07_Keyboard");
		ret.add("Example/10.StarterKit_BasicKit/p12_KnockLock");
		ret.add("Example/10.StarterKit_BasicKit/p13_TouchSensorLamp");
		ret.add("Example/10.StarterKit_BasicKit/p14_TweakTheArduinoLogo");
		ret.add("Example/11.ArduinoISP/ArduinoISP");
		ret.add("Example/Teensy/Tutorial3/HelloSerialMonitor");
		ret.add("Example/Teensy/Tutorial3/Pushbutton");
		ret.add("Example/Teensy/Tutorial3/PushbuttonPullup");
		ret.add("Example/Teensy/Tutorial3/PushbuttonRGBcolor");
		ret.add("Example/Teensy/Tutorial4/AnalogInput");
		ret.add("Example/Teensy/Tutorial4/TemperatureNumberOnly");
		ret.add("Example/Teensy/Tutorial4/TemperatureScaled");
		ret.add("Example/Teensy/Tutorial4/TemperatureScaledMulti");
		return ret;
	}

	private static LinkedList<String> examplesUsingJoyStick() {
		LinkedList<String> ret = new LinkedList<>();
		ret.add("Example/Teensy/USB_Joystick/Basic");
		ret.add("Example/Teensy/USB_Joystick/Buttons");
		ret.add("Example/Teensy/USB_Joystick/Complete");
		return ret;
	}

	private static LinkedList<String> examplesUsingMouse() {
		LinkedList<String> ret = new LinkedList<>();
		ret.add("Example/Teensy/USB_Joystick/Basic");
		ret.add("Example/Teensy/USB_Joystick/Buttons");
		ret.add("Example/Teensy/USB_Joystick/Complete");
		ret.add("Example/Teensy/USB_RawHID/Basic");
		return ret;
	}

	private static LinkedList<String> examplesUsingWire1() {
		LinkedList<String> ret = new LinkedList<>();
		ret.add("Example/Teensy/USB_Joystick/Basic");
		ret.add("Example/Teensy/USB_Joystick/Buttons");
		ret.add("Example/Teensy/USB_Joystick/Complete");
		ret.add("Example/Teensy/USB_RawHID/Basic");
		ret.add("Library/Adafruit_BME280_Library/advancedsettings");
		ret.add("Library/AS3935MI/AS3935MI_LightningDetector_otherInterfaces");
		return ret;
	}

	private static LinkedList<String> examplesUsingMCUmo() {
		LinkedList<String> ret = new LinkedList<>();
		ret.add("Library/Adafruit_Circuit_Playground/CircuitPlaygroundFirmata_Express_CodeOrg");
		return ret;
	}

	/*
	 * These examples that are known to fail
	 */
	private static LinkedList<String> failingExamples() {
		LinkedList<String> ret = new LinkedList<>();
		/*
		 * Because you can not define a enum 2 times Sloeber can not add the enum in
		 * Slober.ino.cpp as a result the function declarations using these enums
		 * generate a error because the enum is not defined. The examples below fail due
		 * to this
		 */
		ret.add("Library/_2020Bot_Library/_2020Bot_Demo");
		// These examples are the processing part and are not a deal of sloeber
		ret.add("Library/Adafruit_BNO055/bunny/processing/cuberotate");
		// manual action is needed for following examples
		ret.add("Library/Accessories/CANCommander");
		ret.add("Library/Accessories_CANCommander");
		ret.add("Library/Accessories/Demo");
		ret.add("Library/Accessories/Full");
		ret.add("Library/Accessories/Group");
		ret.add("Library/Accessories/LightFading");
		ret.add("Library/Accessories/LMD18200");
		ret.add("Library/Accessories/Servos");
		ret.add("Library/Accessories/SignalFrench");
		ret.add("Library/Accessories/Signals4x3");
		ret.add("Library/Accessories/SimpleLed");
		ret.add("Library/Accessories/SimpleLedMulti");
		ret.add("Library/Accessories/Stepper");
		ret.add("Library/Accessory/Shield/OLED/example/Adafruit");
		ret.add("Library/Accessory/Shield/temp/humidity/oled");
		// at the time of testing there were case sensetivity issues
		ret.add("Library/AutoAnalogAudio/SDAudio/SdAudioRecording");
		ret.add("Library/AutoAnalogAudio/SDAudio/SdAudioWavPlayer");
		ret.add("Library/AutoAnalogAudio/AudioRadioRelay");
		ret.add("Library/AutoAnalogAudio/WirelessMicrophone");
		ret.add("Library/AutoAnalogAudio/WirelessSpeaker");
		ret.add("Library/AutoAnalogAudio/WirelessTx_RPi");
		// needs to get the defines and includes right
		ret.add("Library/Accessories/Locoduino.org/Programme4");
		ret.add("Library/Accessories/Locoduino.org/Programme5");
		// Should be build on a stm32 (nucleo as far as I get)
		// but these bards require #927 (and probably more :-(
		ret.add("Library/ADCTouchSensor/CapacitiveController");
		ret.add("Library/ADCTouchSensor/CapacitivePiano");
		// failed in SPI :-(
		ret.add("Library/Adafruit_TinyFlash/TrinketPlayer");
		// not sure how this could work
		ret.add("Library/Adafruit_VS1053_Library/feather_midi");
		// all kinds of incompatibility issues I guess
		ret.add("Library/Andee/Lesson_08_Miscellaneous/Lesson_8b_Use_Bluetooth_Signal_Strength_to_Control_Things");
		ret.add("Library/Andee/Lesson_08_Miscellaneous/Lesson_8c_Change_Andee_Bluetooth_Device_Name");
		ret.add("Library/Andee/Lesson_08_Miscellaneous/Lesson_8d_How_to_Read_and_Write_to_SD_Card");
		ret.add("Library/Andee/Lesson_08_Miscellaneous/Lesson_8f_Using_Andee_IO_Pins_as_OUTPUT_Pins");
		ret.add("Library/Andee/Lesson_08_Miscellaneous/Lesson_8g_Getting_Inputs_from_Andee_IO_Pins");
		ret.add("Library/Andee/Lesson_09_SmartDevice_Control/Lesson_9a_Get_Device_Bluetooth_MAC_Address_ANDROID_Only");
		ret.add("Library/Andee/Lesson_09_SmartDevice_Control/Lesson_9b_Filter_Devices_by_Bluetooth_MAC_Address_ANDROID_Only");
		// doc says not finished
		ret.add("Library/ANT-Arduino/NativeAnt");
		// uses missing library MobileBLE
		ret.add("Library/ArduinoBlue/differentialDriveCar");
		// defining struct/enum in ino file
		ret.add("Library/ArduinoJson/JsonConfigFile");
		ret.add("Library/Arduboy2/RGBled");
		// error: no matching function for call to 'aREST_UI::addToBuffer(const char
		ret.add("Library/aREST_UI/ESP8266");
		ret.add("Library/aREST_UI/WiFi_CC3000");
		ret.add("Library/aREST_UI/WildFire");
		// uses arduinoWIFI
		ret.add("Library/Braccio/braccioOfUnoWiFi");
		// uses lib that does not has lib folder= include-.h
		ret.add("Library/ArduinoCloud/SimpleCloudButtonYun");
		// usi!ng non exsisting methods
		ret.add("Library/CAN-BUS_Shield/gpioRead");
		ret.add("Library/CAN-BUS_Shield/gpioWrite");
		// using defines inino file to generate functions (not supported in Sloeber)
		ret.add("Library/Adafruit_VEML6070_Library/unittests");
		// uses a non existing header
		ret.add("Library/AESLib/complex");
		// using wrong sdfat librarie
		ret.add("Library/Arduino_OPL2_SimpleTone");
		ret.add("Library/Arduino_OPL2_Teensy_PlayDRO");
		ret.add("Library/Arduino_OPL2_Teensy_PlayIMF");
		// I don't recall why following examples didn't work
		ret.add("Library/arduino_ess_ess_yun");
		ret.add("Library/arduino_ess_linkit_one_dweet");
		ret.add("Library/AD7193/AD7193_VoltageMeasurePsuedoDifferential_Example");
		ret.add("Library/bunny_cuberotate/cuberotate");
		ret.add("Library/XPT2046_Touchscreen/ILI9341Test");
		ret.add("Library/Adafruit_AHRS/ahrs_mahony");
		ret.add("Library/Adafruit_BLEFirmata/StandardFirmata");
		ret.add("Library/Adafruit_BNO055/bunny/processing/cuberotate");
		ret.add("Library/Adafruit_GPS_Library/due_shield_sdlog");
		ret.add("Library/Adafruit_Graphic_VFD_Display_Library/GraphicVFDtest");
		ret.add("Library/Adafruit_GPS_Library/locus_erase");
		ret.add("Library/Adafruit_GPS_Library/shield_sdlog");
		ret.add("Library/Adafruit_HX8357_Library/breakouttouchpaint");
		ret.add("Library/Adafruit_ILI9341/breakouttouchpaint");
		ret.add("Library/Adafruit_ILI9341/onoffbutton_breakout");
		ret.add("Library/Adafruit_GPS_Library/echo");
		ret.add("Library/Adafruit_LED_Backpack_Library/wavface");
		ret.add("Library/Adafruit_SSD1306/ssd1306_128x64_i2c");
		ret.add("Library/Adafruit_SSD1306/ssd1306_128x64_spi");
		ret.add("Library/Adafruit_ST7735_Library/soft_spitftbitmap");
		ret.add("Library/Adafruit_TCS34725/colorview/processing/colorview");
		ret.add("Library/Adafruit_TinyRGBLCDShield/TinyHelloWorld");
		ret.add("Library/Akafugu_TWILiquidCrystal_Library/change_address");
		ret.add("Library/Akafugu_WireRtc_Library/alarm");
		ret.add("Library/ALA/RgbStripButton");
		ret.add("Library/APA102/GameOfLife");
		ret.add("Library/arduino-menusystem/led_matrix");
		ret.add("Library/arduino-menusystem/led_matrix_animated");
		ret.add("Library/Arduino_Low_Power/TianStandby");
		ret.add("Library/aREST/BLE");
		ret.add("Library/aREST/ESP32");
		ret.add("Library/aREST/ESP32_cloud");
		ret.add("Library/ArduinoHttpClient/DweetGet");
		ret.add("Library/ArduinoMenu_library/adafruitGfx/lcdMono/lcdMono");
		ret.add("Library/ArduinoMenu_library/adafruitGfx/tft/tft");
		ret.add("Library/ArdVoice/Sample2-Complex");
		ret.add("Library/Aspen_SIM800/Access_HTTP");
		ret.add("Library/Awesome/advanced/how_fast");
		ret.add("Library/Awesome/advanced/lie_detector");
		ret.add("Library/AzureIoTUtility/simplesample_http");
		ret.add("Library/BLEPeripheral/ir_bridge");
		ret.add("Library/BLEPeripheral/temp_sensor");
		ret.add("Library/Brasilino/Basicos/controleGradual");
		ret.add("Library/ClosedCube_HDC1010/hdc1010demo");
		ret.add("Library/Chrono/Resolutions");
		ret.add("Library/Chrono/StopResume");
		ret.add("Library/ConfigurableFirmata/ConfigurableFirmataWiFi");
		ret.add("Library/ControleForno/configuravel");
		ret.add("Library/CopyThreads/c");
		ret.add("Library/ArduinoCloud/SimpleCloudButtoBrzo_I2C");
		ret.add("Library/CopyThreads/FromReadme");
		ret.add("Library/DallasTemperature/Multibus_simple");
		ret.add("Library/DecodeIR/InfraredDecode");
		ret.add("Library/AutoAnalogAudio/SimpleSine");
		ret.add("Library/DimSwitch/DimSwitchTester-ESP-MQTT");
		ret.add("Library/DS3231/echo_time");
		ret.add("Library/Easy_NeoPixels");
		ret.add("Library/DallasTemperature/AlarmHandler");
		ret.add("Library/AmazonDRS/amazonDashNfc");
		ret.add("Library/Andee/Lesson_02_Buttons/Lesson_2h_Using_Buttons_to_Control_Servos");
		ret.add("Library/Andee/Project_Christmas_Lights_and_Annoying_Music");
		ret.add("Library/Andee/Project_Rubber_Band_Launcher");
		ret.add("Library/Andee/Project_Time_Automated_Data_Logger");
		ret.add("Library/ANT-Arduino_library/NativeAnt");
		ret.add("Library/arduino-fsm/timed_switchoff");
		ret.add("Library/BME280/BME_280_BRZO_I2C_Test");
		ret.add("Library/Adafruit_seesaw_Library/DAP");
		// uses unknown NO_ERROR
		ret.add("Library/ClosedCube_TCA9546A/tca9546a_sht31d");
		// uses dht.h from dht_sensor_lib
		ret.add("Library/CMMC_MQTT_Connector/basic_dht");
		ret.add("Library/ArduinoLearningKitStarter/boardTest");
		// uses altsoftserial and then Serial2????
		ret.add("Library/CMMC_NB-IoT/example1");
		// some bu!g I guess
		ret.add("Library/CopyThreads/ExamplesFromReadme");
		ret.add("Library/CRC_Simula_Arduino_IDE_Library/Simula_BehaviorTree");
		// empty sketch??
		ret.add("Library/DFW/ProvisionController");
		// error: 'mapSensor' was not declared in this scope
		ret.add("Library/AD_Sensors/ConstrainAnalogSensor");
		// error: 'sensor' was not declared in this scope
		ret.add("Library/AD_Sensors/MapAndConstrainAnalogSensor");
		// ess-yun.ino:17:12: error: no matching function for call to
		// 'HttpClient::HttpClient()'
		ret.add("Library/arduino-ess/ess-yun");
		// I have no linket board in test setup
		ret.add("Library/arduino-ess/linkit-one-dweet");
        //cores\arduino/WString.h:38:74: error: statement-expressions 
		ret.add("Library/ArduinoTrace/TraceFromGlobalScope");
		// no matching function for call to 'aREST::handle(EthernetClient&)'
		ret.add("Library/aREST/Ethernet");
		// AsciiMassage_servos.ino:75:37: error: no matching function for call to
		// 'AsciiMassagePacker::streamEmpty(const char [6])'
		ret.add("Library/AsciiMassage/AsciiMassage_servos");
		// \BH1750FVI_Simple.ino:33:10: error: 'class Serial_' has no member named
		// 'printf'
		ret.add("Library/BH1750FVI/BH1750FVI_Simple");
		// * This example not complete at all, TODO.
		ret.add("Library/Blinker/Blinker_AUTO/AUTO_MQTT");
		// 3: error: 'StaticJsonBuffer' was not declared in this scope
		ret.add("Library/Boodskap_Message_library/SimpleMessageUsage");
		return ret;
	}

	/**
	 * Give a list of boards pick the board that is best to test this code Boards in
	 * the beginning of the array are prefered (first found ok algorithm)
	 *
	 * returns null if this code should not be tested return null if myBoards is
	 * empty returns the best known boarddescriptor to run this example
	 */
	public static MCUBoard pickBestBoard(Examples example, MCUBoard myBoards[]) {
		String libName = example.getLibName();
		String fqn = example.getFQN();
		if (myBoards.length == 0) {
			return null;
		}

		if (example.getRequiredBoardAttributes().worksOutOfTheBox) {

			// if example states which board it wants use that board
			if (example.getRequiredBoardAttributes().boardName != null) {
				String wantedBoardName = example.getRequiredBoardAttributes().boardName;
				for (MCUBoard curBoard : myBoards) {
					if (curBoard.getID().equals(wantedBoardName)) {
						return curBoard;
					}
				}
			} else {
				// examples using DHT_sensor_library libraries are not found as the include is
				// DHT.h
				if (!libName.equals("DHT_sensor_library") && fqn.contains("DHT")) {
					return null;
				}
				// if the boardname is in the libname or ino name pick this one
				for (MCUBoard curBoard : myBoards) {
					String curBoardName = curBoard.getSlangName().toLowerCase();
					if (libName.toLowerCase().contains(curBoardName) || fqn.toLowerCase().contains(curBoardName)) {
						if (curBoard.isExampleSupported(example)) {
							return curBoard;
						}
					}
				}
				// If the architecture is in the libname or boardname pick this one
				for (MCUBoard curBoard : myBoards) {
					String curArchitectureName = curBoard.getBoardDescriptor().getArchitecture().toLowerCase();
					if (libName.toLowerCase().contains(curArchitectureName)
							|| fqn.toLowerCase().contains(curArchitectureName)) {
						if (curBoard.isExampleSupported(example)) {
							return curBoard;
						}
					}
				}
				// if the example name contains teensy try teensy board
				if (example.getFQN().toLowerCase().contains("teensy")) {
					for (MCUBoard curBoard : myBoards) {
						if (Teensy.class.isInstance(curBoard)) {
							return curBoard;
						}
					}
				}
				// if the example name contains ESP32 try ESP32 board
				if (example.getFQN().toLowerCase().contains("esp32")) {
					for (MCUBoard curBoard : myBoards) {
						if (ESP32.class.isInstance(curBoard)) {
							return curBoard;
						}
					}
				}
				// if the example name contains ESP try ESP8266 board
//				if (example.getFQN().toLowerCase().contains("esp")) {
//					for (MCUBoard curBoard : myBoards) {
//						if (ESP8266.class.isInstance(curBoard)) {
//							return curBoard;
//						}
//					}
//				}
//causes issues with response
				
				// Out of guesses based on the name. Take the first ok one
				for (MCUBoard curBoard : myBoards) {
					if (curBoard.isExampleSupported(example)) {
						return curBoard;
					}
				}
			}
		}
		System.out.println("No board found for " + Integer.toString(++noBoardFoundCount) + " " + example.getFQN());
		return null;
	}

	private static String getRequiredBoardID(String fqn) {
		switch (fqn) {
		case "Library/Accessory_Shield/OLED_example_Adafruit":
		case "Library/Accessory_Shield/temp_humidity_oled":
			return "uno";
		case "Library/Adafruit_Circuit_Playground/Infrared_Demos/Infrared_NeoPixel":
		case "Library/Adafruit_Circuit_Playground/Infrared_Demos/Infrared_Read":
		case "Library/Adafruit_Circuit_Playground/Infrared_Demos/Infrared_Record":
		case "Library/Adafruit_Circuit_Playground/Infrared_Demos/Infrared_Send":
		case "Library/Adafruit_Circuit_Playground/Infrared_Demos/Infrared_Testpattern":
		case "Library/Adafruit_Zero_FFT_Library/CircuitPlayground":
			return "adafruit_circuitplayground_m0";
		case "Library/Adafruit_MiniMLX90614/templight":
			return "gemma";
		case "Library/ArduinoThread/SensorThread":
			return "due";
		case "Library/AudioFrequencyMeter/SimpleAudioFrequencyMeter":
		case "Library/Adafruit_NeoPXL8/strandtest":
			return "zero";
		case "Library/BLEPeripheral/iBeacon":
			return "feather52";
		}
		return null;
	}
}
