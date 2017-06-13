package jUnit.boards;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import io.sloeber.core.api.BoardDescriptor;

@SuppressWarnings("nls")
public abstract class IBoard {

	protected BoardDescriptor myBoardDescriptor = null;
	protected List<String> doNotTestTheseSketches;
	protected List<String> doNotTestTheseLibs;

	public BoardDescriptor getBoardDescriptor() {
		return this.myBoardDescriptor;
	}

	public boolean isExampleOk(String inoName, String libName) {
		if (this.myBoardDescriptor == null) {
			return false;
		}
		if (this.doNotTestTheseSketches == null) {
			createDoNotTestTheseSketches();
			createDoNotTestTheseLibs();

		}
		if (this.doNotTestTheseLibs.contains(libName)) {
			return false;
		}
		if (this.doNotTestTheseSketches.contains(inoName)) {
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
				"Adafruit_TinyRGBLCDShield examples?TinyHelloWorld",
				"Akafugu_TWILiquidCrystal_Library examples?change_address", "Akafugu_WireRtc_Library examples?alarm",
				"ALA examples?RgbStripButton", "APA102 examples?GameOfLife", "arduino-menusystem examples?led_matrix",
				"arduino-menusystem examples?led_matrix_animated", "Arduino_Low_Power examples?TianStandby",
				"aREST examples?BLE", "aREST examples?ESP32", "aREST examples?ESP32_cloud",
				"ArduinoHttpClient examples?DweetGet", "ArduinoMenu_library examples? adafruitGfx? lcdMono?lcdMono",
				"ArduinoMenu_library examples? adafruitGfx? tft?tft", "ArduinoMqtt examples?ConnectEthernetClient",
				"ArduinoMqtt examples?PubSub", "ArdVoice examples?Sample2-Complex", "Aspen_SIM800 examples?Access_HTTP",
				"Awesome examples? advanced?how_fast", "Awesome examples? advanced?lie_detector",
				"AzureIoTUtility examples?simplesample_http", "BLEPeripheral examples?ir_bridge",
				"BLEPeripheral examples?temp_sensor", "Brasilino examples? Basicos?controleGradual",
				"ClosedCube_HDC1010 examples?hdc1010demo", "Chrono examples?Resolutions", "Chrono examples?StopResume",
				"ConfigurableFirmata examples?ConfigurableFirmataWiFi", "ControleForno examples?configuravel",
				"CopyThreads examples?c" });
		runSketchOnBoard.put("fix case Sensitive include first",
				new String[] { "AutoAnalogAudio examples? SDAudio?SdAudioRecording",
						"AutoAnalogAudio examples? SDAudio?SdAudioWavPlayer",
						"AutoAnalogAudio examples?AudioRadioRelay", "AutoAnalogAudio examples?WirelessMicrophone",
						"AutoAnalogAudio examples?WirelessSpeaker", "AutoAnalogAudio examples?WirelessTx_RPi" });

		runSketchOnBoard.put("nodeMCU",
				new String[] { "YouMadeIt examples?basic_example", "ArduinoIRC examples?BasicESP8266",
						"ArduinoIRC examples?BasicESP8266Reply",
						"ArduinoMenu_library examples? esp8266? XmlServer?XmlServer",
						"ArduinoMqtt examples?ConnectEsp8266WiFiClient", "aREST examples?ESP8266",
						"aREST examples?ESP8266_cloud", "aREST examples?ESP8266_cloud_and_local",
						"aREST examples?ESP8266_cloud_pro", "aREST examples?ESP8266_softAP",
						"aREST_UI examples?ESP8266", "ConfigManagerexamples"

				});
		runSketchOnBoard.put("gemma",
				new String[] { "Adafruit_MiniMLX90614 examples?templight",
						"Adafruit_TiCoServo examples?TiCoServo_Test_Trinket_Gemma",
						"Arduino_Low_Power examples?PrimoDeepSleep" });
		runSketchOnBoard.put("primo",
				new String[] { "Arduino_Low_Power examples?PrimoDeepSleep", "BLEPeripheral examples?iBeacon" });
		runSketchOnBoard.put("trinket",
				new String[] { "Adafruit_SoftServo examples?TrinketKnob",
						"Adafruit_TiCoServo examples?TiCoServo_Test_Trinket_Gemma",
						"Adafruit_TinyFlash examples?TrinketPlayer" });
		runSketchOnBoard.put("zero", new String[] { "ArduinoCloud examples?ReadAndWrite",
				"ArduinoCloud examples?SimpleCloudButton", "AudioFrequencyMeter examples?SimpleAudioFrequencyMeter" });
		runSketchOnBoard.put("due", new String[] { "ArduinoThread examples?SensorThread" });
		runSketchOnBoard.put("mega", new String[] { "aREST_UI examples?WiFi_CC3000" });
		runSketchOnBoard.put("wildfire", new String[] { "aREST_UI examples?WildFire" });
		runSketchOnBoard.put("circuitplay32u4catexpress",
				new String[] { "Adafruit_Circuit_Playground examples?Infrared_NeoPixel",
						"Adafruit_Circuit_Playground examples?Infrared_Read",
						"Adafruit_Circuit_Playground examples?Infrared_Record",
						"Adafruit_Circuit_Playground examples?Infrared_Testpattern" });
		runSketchOnBoard.put("unowifi", new String[] { "Braccio examples?braccioOfUnoWiFi" });

		for (Entry<String, String[]> curEntry : runSketchOnBoard.entrySet()) {
			if (!getName().equals(curEntry.getKey())) {
				this.doNotTestTheseSketches.addAll(Arrays.asList(curEntry.getValue()));
			}
		}

	}

	private void createDoNotTestTheseLibs() {
		this.doNotTestTheseLibs = new ArrayList<>();
		Map<String, String[]> runLibOnBoard = new HashMap<>();
		runLibOnBoard.put("no Board", new String[] { "ACROBOTIC_SSD1306", "XLR8Servo", "Adafruit_CC3000_Library",
				"Adafruit_HX8340B", "Adafruit_IO_Arduino", "Adafruit_MQTT_Library", "Adafruit_SPIFlash",
				"Adafruit_SSD1325", "ArdBitmap", "ArdOSC", "Arduino-Websocket-Fast", "ArduinoFacil",
				"ArduinoMenu_library", "ArduinoSensors", "ArduinoSerialToTCPBridgeClient", "ArduinoUnit", "arduinoVNC",
				"ArduZ80", "AS3935", "AzureIoTHubMQTTClient", "BigCrystal", "Babelduino", "Blynk", "Brief", "Brzo_I2C",
				"BTLE", "Cayenne", "CayenneMQTT", "Chronos", "CoAP_simple_library", "Comp6DOF_n0m1", "Constellation" });
		runLibOnBoard.put("uno",
				new String[] { "A4963", "Adafruit_Motor_Shield_library", "Adafruit_Motor_Shield_library_V2",
						"AccelStepper", "Arduino_Uno_WiFi_Dev_Ed_Library", "ardyno", "AVR_Standard_C_Time_Library" });
		runLibOnBoard.put("esplora", new String[] { "Esplora" });
		runLibOnBoard.put("circuitplay32u4cat",
				new String[] { "Adafruit_Circuit_Playground", "Adafruit_BluefruitLE_nRF51", "Adafruit_GPS_Library" });
		runLibOnBoard.put("nodeMCU",
				new String[] { "Adafruit_IO_Arduino", "anto-esp8266-arduino", "Automaton-Esp8266", "AzureIoTHub",
						"AzureIoTProtocol_HTTP", "AzureIoTProtocol_MQTT", "CMMC_Easy", "CMMC_MQTT_Connector",
						"CMMC_OTA", "CMMC_WiFi_Connector" });
		runLibOnBoard.put("feather52", new String[] { "Firmata" });
		runLibOnBoard.put("primo", new String[] { "Adafruit_BluefruitLE_nRF51", "arduino-NVM" });
		runLibOnBoard.put("mega", new String[] { "Adafruit_GPS_Library" });
		runLibOnBoard.put("zero", new String[] { "Arduino_Low_Power", "ArduinoSound", "AudioZero" });
		runLibOnBoard.put("mkrfox1200", new String[] { "Arduino_SigFox_for_MKRFox1200" });
		runLibOnBoard.put("due", new String[] { "Audio", "AutoAnalogAudio" });

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

}