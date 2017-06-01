package jUnit.boards;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import io.sloeber.core.api.BoardDescriptor;

@SuppressWarnings("nls")
public abstract class IBoard {

	protected BoardDescriptor myBoardDescriptor = null;
	protected List<String> doNotTestTheseSketches;
	protected List<String> doNotTestTheseLibs;
	/*
	 * TOFIX Needs a list "needs to run on board" and a list of
	 * "Does not run on board" maybe a list board example needs/doesn't work
	 */

	final String[] doNotTestSketch = { "AD7193 examples?AD7193_VoltageMeasurePsuedoDifferential_Example",
			"bunny_cuberotate?cuberotate", "XPT2046_Touchscreen?ILI9341Test", "Adafruit_AHRS examples?ahrs_mahony",
			"Adafruit_BLEFirmata examples?StandardFirmata", "Adafruit_BNO055 examples? bunny? processing?cuberotate",
			"Adafruit_GPS_Library examples?due_shield_sdlog",
			"Adafruit_Graphic_VFD_Display_Library examples?GraphicVFDtest", "Adafruit_GPS_Library examples?locus_erase",
			"Adafruit_GPS_Library examples?shield_sdlog", "Adafruit_HX8357_Library examples?breakouttouchpaint",
			"Adafruit_ILI9341 examples?breakouttouchpaint", "Adafruit_ILI9341 examples?onoffbutton_breakout",
			"Adafruit_GPS_Library examples?echo", "Adafruit_LED_Backpack_Library examples?wavface",
			"Adafruit_SSD1306 examples?ssd1306_128x64_i2c", "Adafruit_SSD1306 examples?ssd1306_128x64_spi",
			"Adafruit_ST7735_Library examples?soft_spitftbitmap",
			"Adafruit_TCS34725 examples? colorview? processing?colorview",
			"Adafruit_TinyRGBLCDShield examples?TinyHelloWorld",
			"Akafugu_TWILiquidCrystal_Library examples?change_address", "Akafugu_WireRtc_Library examples?alarm",
			"ALA examples?RgbStripButton", "APA102 examples?GameOfLife" };
	final String[] doNotTestLib = { "ACROBOTIC_SSD1306", "XLR8Servo", "Adafruit_CC3000_Library", "Adafruit_HX8340B",
			"Adafruit_IO_Arduino", "Adafruit_MQTT_Library", "Adafruit_SPIFlash", "Adafruit_SSD1325" };

	final protected String[] testSketchOnLeonardo = {};
	final protected String[] testLibOnLeonardo = {};
	final protected String[] testSketchOnUno = {};
	final protected String[] testLibOnUno = { "A4963", "Adafruit_Motor_Shield_library",
			"Adafruit_Motor_Shield_library_V2", "AccelStepper" };
	final protected String[] testSketchOnEsplora = {};
	final protected String[] testLibOnEsplora = { "Esplora" };
	final protected String[] testLibOncircuitplay32u4cat = { "Adafruit_Circuit_Playground",
			"Adafruit_BluefruitLE_nRF51", "Adafruit_GPS_Library" };
	final protected String[] testSketchOncircuitplay32u4cat = {};
	final protected String[] testSketchOnNodeMCU = { "YouMadeIt examples?basic_example" };
	final protected String[] testLibOnNodeMCU = { "Adafruit_IO_Arduino", "anto_esp8266_arduino" };
	final protected String[] testLibOnfeather52 = { "Firmata" };
	final protected String[] testSketchOnfeather52 = {};
	final protected String[] testLibOnPrimo = { "Adafruit_BluefruitLE_nRF51" };
	final protected String[] testSketchOnPrimo = {};
	final protected String[] testLibOnMega = { "Adafruit_GPS_Library" };
	final protected String[] testSketchOnMega = {};
	final protected String[] testLibOnGemma = {};
	final protected String[] testSketchOnGemma = { "Adafruit_MiniMLX90614 examples?templight" };
	final protected String[] testLibOnTrinket = {};
	final protected String[] testSketchOnTrinket = { "Adafruit_SoftServo examples?TrinketKnob",
			"Adafruit_TiCoServo examples?TiCoServo_Test_Trinket_Gemma_leonardo",
			"Adafruit_TinyFlash examples?TrinketPlayer" };

	public BoardDescriptor getBoardDescriptor() {
		return this.myBoardDescriptor;
	}

	public boolean isExampleOk(String inoName, String libName) {
		if (this.myBoardDescriptor == null) {
			return false;
		}
		if (this.doNotTestTheseSketches == null) {
			this.doNotTestTheseSketches = new LinkedList<>();
			this.doNotTestTheseSketches.addAll(Arrays.asList(this.doNotTestSketch));
			this.doNotTestTheseSketches.addAll(Arrays.asList(this.testSketchOnLeonardo));
			this.doNotTestTheseSketches.addAll(Arrays.asList(this.testSketchOnUno));
			this.doNotTestTheseSketches.addAll(Arrays.asList(this.testSketchOnEsplora));
			this.doNotTestTheseSketches.addAll(Arrays.asList(this.testSketchOncircuitplay32u4cat));
			this.doNotTestTheseSketches.addAll(Arrays.asList(this.testSketchOnfeather52));
			this.doNotTestTheseSketches.addAll(Arrays.asList(this.testSketchOnNodeMCU));
			this.doNotTestTheseSketches.addAll(Arrays.asList(this.testSketchOnPrimo));
			this.doNotTestTheseSketches.addAll(Arrays.asList(this.testSketchOnMega));
			this.doNotTestTheseSketches.addAll(Arrays.asList(this.testSketchOnGemma));
			this.doNotTestTheseSketches.addAll(Arrays.asList(this.testSketchOnTrinket));

			this.doNotTestTheseLibs = new LinkedList<>();
			this.doNotTestTheseLibs.addAll(Arrays.asList(this.doNotTestLib));
			this.doNotTestTheseLibs.addAll(Arrays.asList(this.testLibOnLeonardo));
			this.doNotTestTheseLibs.addAll(Arrays.asList(this.testLibOnUno));
			this.doNotTestTheseLibs.addAll(Arrays.asList(this.testLibOnEsplora));
			this.doNotTestTheseLibs.addAll(Arrays.asList(this.testLibOncircuitplay32u4cat));
			this.doNotTestTheseLibs.addAll(Arrays.asList(this.testLibOnfeather52));
			this.doNotTestTheseLibs.addAll(Arrays.asList(this.testLibOnNodeMCU));
			this.doNotTestTheseLibs.addAll(Arrays.asList(this.testLibOnPrimo));
			this.doNotTestTheseLibs.addAll(Arrays.asList(this.testLibOnMega));
			this.doNotTestTheseLibs.addAll(Arrays.asList(this.testLibOnGemma));
			this.doNotTestTheseLibs.addAll(Arrays.asList(this.testLibOnTrinket));

			switch (getName()) {
			case "leonardo": {
				this.doNotTestTheseSketches.removeAll(Arrays.asList(this.testSketchOnLeonardo));
				this.doNotTestTheseLibs.removeAll(Arrays.asList(this.testLibOnLeonardo));
				break;
			}
			case "uno": {
				this.doNotTestTheseSketches.removeAll(Arrays.asList(this.testSketchOnUno));
				this.doNotTestTheseLibs.removeAll(Arrays.asList(this.testLibOnUno));
				break;
			}
			case "esplora": {
				this.doNotTestTheseSketches.removeAll(Arrays.asList(this.testSketchOnEsplora));
				this.doNotTestTheseLibs.removeAll(Arrays.asList(this.testLibOnEsplora));
				break;
			}
			case "feather52": {
				this.doNotTestTheseSketches.removeAll(Arrays.asList(this.testSketchOnfeather52));
				this.doNotTestTheseLibs.removeAll(Arrays.asList(this.testLibOnfeather52));
				break;
			}
			case "circuitplay32u4cat": {
				this.doNotTestTheseSketches.removeAll(Arrays.asList(this.testSketchOncircuitplay32u4cat));
				this.doNotTestTheseLibs.removeAll(Arrays.asList(this.testLibOncircuitplay32u4cat));
				break;
			}
			case "primo": {
				this.doNotTestTheseSketches.removeAll(Arrays.asList(this.testSketchOnPrimo));
				this.doNotTestTheseLibs.removeAll(Arrays.asList(this.testLibOnPrimo));
				break;
			}
			case "mega": {
				this.doNotTestTheseSketches.removeAll(Arrays.asList(this.testSketchOnMega));
				this.doNotTestTheseLibs.removeAll(Arrays.asList(this.testLibOnMega));
				break;
			}
			case "gemma": {
				this.doNotTestTheseSketches.removeAll(Arrays.asList(this.testSketchOnGemma));
				this.doNotTestTheseLibs.removeAll(Arrays.asList(this.testLibOnGemma));
				break;
			}
			}
		}
		if (this.doNotTestTheseLibs.contains(libName)) {
			return false;
		}
		if (this.doNotTestTheseSketches.contains(inoName)) {
			return false;
		}
		return true;
	}

	public String getName() {
		if (this.myBoardDescriptor == null) {
			return null;
		}
		return this.myBoardDescriptor.getBoardID();
	}

}