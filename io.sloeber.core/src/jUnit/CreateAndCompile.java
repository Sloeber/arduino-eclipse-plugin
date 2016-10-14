package jUnit;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.api.BoardsManager;
import io.sloeber.core.api.CodeDescriptor;
import io.sloeber.core.api.ConfigurationDescriptor;

@SuppressWarnings("nls")
@RunWith(Parameterized.class)
public class CreateAndCompile {
	private String mBoardID;
	private Map<String, String> myOptions = new HashMap<>();
	private String mPackageName;
	private String mPlatform;
	private String mJsonFileName;
	private static int mCounter = 0;
	private static String teensyInstall = "D:/arduino/arduino-1.6.9 - Teensy 1.29/hardware";
	private static String teensyBoards_txt = teensyInstall + "/teensy/avr/boards.txt";

	public CreateAndCompile(String jsonFileName, String packageName, String platform, String boardID, String options) {
		this.mBoardID = boardID;
		this.mPackageName = packageName;
		this.mPlatform = platform;
		this.mJsonFileName = jsonFileName;
		String[] lines = options.split("\n"); //$NON-NLS-1$
		for (String curLine : lines) {
			String[] values = curLine.split("=", 2); //$NON-NLS-1$
			if (values.length == 2) {
				this.myOptions.put(values[0], values[1]);
			}
		}

	}

	@SuppressWarnings("rawtypes")
	@Parameterized.Parameters
	public static Collection boards() {
		return Arrays.asList(new Object[][] {
				// STM32
				{ "package_stm_index.json", "STM32", "STM32 F1 Boards", "NUCLEO-F103RB", "" }, //
				{ "package_stm_index.json", "STM32", "STM32 L4 Boards", "NUCLEO-L476RG", "" }, //

				// mighty core
				{ "package_MCUdude_MightyCore_index.json", "MightyCore", "MightyCore", "1284",
						"clock=16MHz_external\npinout=bobuino\nBOD=2v7\nvariant=modelP\nLTO=Os" }, //
				{ "package_MCUdude_MightyCore_index.json", "MightyCore", "MightyCore", "644",
						"clock=20MHz_external\npinout=standard\nBOD=2v7\nvariant=modelNonP\nLTO=Os_flto" }, //
				{ "package_MCUdude_MightyCore_index.json", "MightyCore", "MightyCore", "324",
						"clock=12MHz_external\npinout=bobuino\nBOD=1v8\nvariant=modelP\nLTO=Os" }, //
				{ "package_MCUdude_MightyCore_index.json", "MightyCore", "MightyCore", "164",
						"clock=8MHz_external\npinout=bobuino\nBOD=Disabled\nvariant=modelA\nLTO=Os" }, //
				{ "package_MCUdude_MightyCore_index.json", "MightyCore", "MightyCore", "32",
						"clock=12MHz_external\npinout=standard\nLTO=Os" }, //
				{ "package_MCUdude_MightyCore_index.json", "MightyCore", "MightyCore", "16",
						"clock=8MHz_internal\npinout=bobuino\nLTO=Os" }, //
				{ "package_MCUdude_MightyCore_index.json", "MightyCore", "MightyCore", "8535",
						"clock=1MHz_internal\npinout=bobuino\nLTO=Os" }, //

				// digistump sam
				{ "package_digistump_index.json", "digistump", "Digistump SAM Boards (32-bits ARM Cortex-M3)", "digix",
						"" }, //

				// redbear
				/*
				 * problems with linking see
				 * https://github.com/jantje/arduino-eclipse-plugin/issues/546
				 */
				{ "package_redbear_index.json", "RedBear", "RedBear Duo (32-bits ARM Cortex-M3)", "RedBear_Duo_native",
						"" }, //
				{ "package_redbear_index.json", "RedBear", "RedBear Duo (32-bits ARM Cortex-M3)", "RedBear_Duo", "" }, //

				// digistump AVR
				{ "package_digistump_index.json", "digistump", "Digistump AVR Boards", "digispark-tiny", "" }, //
				{ "package_digistump_index.json", "digistump", "Digistump AVR Boards", "digispark-pro", "" }, //
				{ "package_digistump_index.json", "digistump", "Digistump AVR Boards", "digispark-pro32", "" }, //
				{ "package_digistump_index.json", "digistump", "Digistump AVR Boards", "digispark-pro64", "" }, //
				{ "package_digistump_index.json", "digistump", "Digistump AVR Boards", "digispark-tiny16", "" }, //
				{ "package_digistump_index.json", "digistump", "Digistump AVR Boards", "digispark-tiny8", "" }, //
				{ "package_digistump_index.json", "digistump", "Digistump AVR Boards", "digispark-tiny1", "" }, //

				// digistump oak (needs MSVCR100.dll to be added to tool
				// esptool2 folder
				{ "package_digistump_index.json", "digistump", "Oak by Digistump", "oak1",
						"CpuFrequency=80\nUploadTool=oak\nFlashSize=OAK\nRomConfig=Full" }, //
				{ "package_digistump_index.json", "digistump", "Oak by Digistump", "oak1_noauto",
						"CpuFrequency=80\nUploadTool=oak_ota\nFlashSize=OAK\nRomConfig=Full" },
				{ "package_digistump_index.json", "digistump", "Oak by Digistump", "oak",
						"CpuFrequency=160\nUploadTool=oak\nFlashSize=OAK\nRomConfig=Half1" }, //

				// package_damellis_attiny_index
				{ "package_damellis_attiny_index.json", "attiny", "attiny", "ATtinyX5",
						"cpu=attiny25\nclock=internal1" }, //
				{ "package_damellis_attiny_index.json", "attiny", "attiny", "ATtinyX4",
						"cpu=attiny44\nclock=internal1" }, //

				// package_akafugu_index.json
				{ "package_akafugu_index.json", "akafugu", "Akafugu Boards", "akafugubread", "" }, //
				{ "package_akafugu_index.json", "akafugu", "Akafugu Boards", "akafugubread16", "" }, //
				{ "package_akafugu_index.json", "akafugu", "Akafugu Boards", "akafuinol", "" }, //
				{ "package_akafugu_index.json", "akafugu", "Akafugu Boards", "simpleclock", "" }, //
				{ "package_akafugu_index.json", "akafugu", "Akafugu Boards", "nixieclock", "" }, //

				// Teensy
				{ "local", teensyBoards_txt, "", "teensy31", "usb=serial\nspeed=96\nkeys=en-us" }, //
				{ "local", teensyBoards_txt, "", "teensy30", "usb=serial\nspeed=96\nkeys=en-us" }, //
				{ "local", teensyBoards_txt, "", "teensyLC", "usb=serial\nl\nspeed=48\nkeys=en-us" }, //
				{ "local", teensyBoards_txt, "", "teensypp2", "usb=serial\nspeed=16\nkeys=en-us" }, //
				{ "local", teensyBoards_txt, "", "teensy2", "usb=serial\nspeed=16\nkeys=en-us" }, //

				// Adafruit AVR
				{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "flora8", "" }, //
				{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "bluefruitmicro", "" }, //
				{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "gemma", "" }, //
				{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "feather32u4", "" }, //
				{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "trinket3", "" }, //
				{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "trinket5", "" }, //
				{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "protrinket5", "" }, //
				{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "protrinket3", "" }, //
				{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "protrinket5ftdi", "" }, //
				{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "protrinket3ftdi", "" }, //
				{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "adafruit32u4", "" }, //
				{ "package_adafruit_index.json", "adafruit", "Adafruit AVR Boards", "circuitplay32u4cat", "" }, //
				// Adafruit SAMD
				{ "package_adafruit_index.json", "adafruit", "Adafruit SAMD Boards", "adafruit_feather_m0", "" }, //

				// alorium
				// not yet adopted to LTO
				// { "package_aloriumtech_index.json", "alorium", "Alorium XLR8
				// Boards", "xlr8rev2", "xbs=disable_none" }, //

				// amel samd
				{ "package_index.json", "AMEL", "AMEL-Tech Boards", "AMEL_SmartEverything_atmel_ice", "" }, //
				{ "package_index.json", "AMEL", "AMEL-Tech Boards", "AMEL_SmartEverything_sam_ice", "" }, //
				{ "package_index.json", "AMEL", "AMEL-Tech Boards", "AMEL_SmartEverything_native", "" }, //
				{ "package_index.json", "AMEL", "AMEL-Tech Boards", "AMEL_SmartEverything_native", "" }, //

				// acore avr
				{ "package_adafruit_index.json", "arcore", "Leonardo & Micro MIDI-USB (arcore)", "leonardo", "" }, //
				{ "package_adafruit_index.json", "arcore", "Leonardo & Micro MIDI-USB (arcore)", "leonardo2", "" }, //
				{ "package_adafruit_index.json", "arcore", "Leonardo & Micro MIDI-USB (arcore)", "micro", "" }, //

				// ardhat
				// not yet adopted to LTO
				// { "package_ardhat_index.json", "ardhat", "Ardhat AVR Boards",
				// "ardhat", "" }, //

				// arduboy
				{ "package_arduboy_index.json", "arduboy", "Arduboy", "arduboy", "" }, //
				{ "package_arduboy_index.json", "arduboy", "Arduboy", "arduboy_devkit", "" }, //

				// Arduino.cc AVR boards
				{ "package_index.json", "arduino", "Arduino AVR Boards", "yun", "" }, //
				{ "package_index.json", "arduino", "Arduino AVR Boards", "uno", "" }, //
				{ "package_index.json", "arduino", "Arduino AVR Boards", "diecimila", "cpu=atmega328" },
				{ "package_index.json", "arduino", "Arduino AVR Boards", "nano", "cpu=atmega328" },
				{ "package_index.json", "arduino", "Arduino AVR Boards", "mega", "cpu=atmega2560" }, // comment
				{ "package_index.json", "arduino", "Arduino AVR Boards", "megaADK", "" }, //
				{ "package_index.json", "arduino", "Arduino AVR Boards", "leonardo", "" }, //
				{ "package_index.json", "arduino", "Arduino AVR Boards", "micro", "" }, //
				{ "package_index.json", "arduino", "Arduino AVR Boards", "esplora", "" }, //
				{ "package_index.json", "arduino", "Arduino AVR Boards", "mini", "cpu=atmega328" }, // comment
				{ "package_index.json", "arduino", "Arduino AVR Boards", "ethernet", "" }, //
				{ "package_index.json", "arduino", "Arduino AVR Boards", "fio", "" }, //
				{ "package_index.json", "arduino", "Arduino AVR Boards", "bt", "cpu=atmega328" }, // comment
				{ "package_index.json", "arduino", "Arduino AVR Boards", "LilyPadUSB", "" }, // comment
				{ "package_index.json", "arduino", "Arduino AVR Boards", "lilypad", "cpu=atmega328" }, // comment
				{ "package_index.json", "arduino", "Arduino AVR Boards", "pro", "cpu=16MHzatmega328" }, // comment
				{ "package_index.json", "arduino", "Arduino AVR Boards", "atmegang", "cpu=atmega8" }, //
				{ "package_index.json", "arduino", "Arduino AVR Boards", "robotControl", "" }, //
				{ "package_index.json", "arduino", "Arduino AVR Boards", "robotMotor", "" }, //
				{ "package_index.json", "arduino", "Arduino AVR Boards", "gemma", "" },

				// Arduino SAM
				{ "package_index.json", "arduino", "Arduino SAM Boards (32-bits ARM Cortex-M3)", "arduino_due_x_dbg",
						"" }, //
				{ "package_index.json", "arduino", "Arduino SAM Boards (32-bits ARM Cortex-M3)", "arduino_due_x", "" }, //

				// Arduino SAMD
				{ "package_index.json", "arduino", "Arduino SAMD Boards (32-bits ARM Cortex-M0+)", "arduino_zero_edbg",
						"" }, //
				{ "package_index.json", "arduino", "Arduino SAMD Boards (32-bits ARM Cortex-M0+)",
						"arduino_zero_native", "" }, //
				{ "package_index.json", "arduino", "Arduino SAMD Boards (32-bits ARM Cortex-M0+)", "mkr1000", "" }, //

				// arduino-tiny-841 depricated so not added

				// arrow SAMD
				{ "package_index.json", "Arrow", "Arrow Boards", "SmartEverything_Fox_atmel_ice", "" }, //
				{ "package_index.json", "Arrow", "Arrow Boards", "SmartEverything_Fox_sam_ice", "" }, //
				{ "package_index.json", "Arrow", "Arrow Boards", "SmartEverything_Fox_native", "" }, //
				{ "package_index.json", "Arrow", "Arrow Boards", "NetTrotter_atmel_ice", "" }, //
				{ "package_index.json", "Arrow", "Arrow Boards", "NetTrotter_sam_ice", "" }, //
				{ "package_index.json", "Arrow", "Arrow Boards", "NetTrotter_native", "" }, //

				// atmel-avr-xminis
				{ "package_index.json", "atmel-avr-xminis", "Atmel AVR Xplained-minis", "atmega328p_xplained_mini",
						"" }, //
				{ "package_index.json", "atmel-avr-xminis", "Atmel AVR Xplained-minis", "atmega168pb_xplained_mini",
						"" }, //
				{ "package_index.json", "emoro", "EMORO 2560", "emoro2560", "" }, //

				// ATTinyCore
				{ "package_drazzy.com_index.json", "ATTinyCore", "ATTinyCore", "attinyx41",
						"chip=841\nclock=8internal\n=bod=disable)" }, //
				{ "package_drazzy.com_index.json", "ATTinyCore", "ATTinyCore", "attiny841opti",
						"clock=8internal\n=bod=1v8" }, //
				{ "package_drazzy.com_index.json", "ATTinyCore", "ATTinyCore", "attiny1634",
						"clock=8internal\n=bod=1v8" }, //
				{ "package_drazzy.com_index.json", "ATTinyCore", "ATTinyCore", "attiny1634opti",
						"clock=8internal\nbod=2v7" }, //
				{ "package_drazzy.com_index.json", "ATTinyCore", "ATTinyCore", "attiny828",
						"clock=8internal\n=bod=2v7" }, //
				{ "package_drazzy.com_index.json", "ATTinyCore", "ATTinyCore", "attiny828opti", "bod=2v7\nvcc=3v3" }, //
				{ "package_drazzy.com_index.json", "ATTinyCore", "ATTinyCore", "attinyx5",
						"chip=85\nclock=20external\nbod=2v7\nTimerClockSource=default" }, //
				{ "package_drazzy.com_index.json", "ATTinyCore", "ATTinyCore", "attinyx4",
						"chip=44\nclock=6external\nbod=2v7\npinmapping=new" }, //
				{ "package_drazzy.com_index.json", "ATTinyCore", "ATTinyCore", "attinyx61",
						"chip=261\nclock=1internal\nbod=2v7\nTimerClockSource=pll" }, //
				{ "package_drazzy.com_index.json", "ATTinyCore", "ATTinyCore", "attiny167opti",
						"clock=16external\nbod=2v7\npinmapping=new" }, //
				{ "package_drazzy.com_index.json", "ATTinyCore", "ATTinyCore", "attiny87opti",
						"clock=16external\nbod=2v7\npinmapping=new" }, //
				{ "package_drazzy.com_index.json", "ATTinyCore", "ATTinyCore", "attinyx7",
						"chip=87\nclock=12external\nbod=2v7\npinmapping=new" }, //
				{ "package_drazzy.com_index.json", "ATTinyCore", "ATTinyCore", "attinyx8",
						"chip=88\nclock=1internal\nbod=2v7" }, //
				{ "package_drazzy.com_index.json", "ATTinyCore", "ATTinyCore", "attinyx313",
						"chip=4313\nclock=5internal\nbod=2v7\nINITIALIZE_SECONDARY_TIMERS=1" }, //

				// chipKIT
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "cerebot32mx4", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "cerebot32mx7", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "cerebot_mx3ck", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "chipkit_mx3", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "cerebot_mx3ck_512", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "cerebot_mx4ck", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "chipkit_pro_mx4", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "cerebot_mx7ck", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "chipkit_pro_mx7", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "chipkit_Pi", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "chipkit_Pi_USB_Serial", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "cmod", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "CUI32stem", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "ubw32_mx460", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "ubw32_mx795", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "cui32", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "usbono_pic32", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "chipkit_DP32", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "fubarino_mini_dev", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "fubarino_mini", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "fubarino_sd_seeed", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "fubarino_sd", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "Fubarino_SDZ", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "mega_pic32", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "mega_usb_pic32", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "Olimex_Pinguino32", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "picadillo_35t", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "quick240_usb_pic32", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "chipkit_uc32", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "uc32_pmod", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "uno_pic32", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "uno_pmod", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "chipkit_WF32", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "chipkit_WiFire", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "chipkit_WiFire_AB", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "chipkit_WiFire_80MHz", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "OpenScope", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "openbci", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "lenny", "" }, //
				{ "package_chipkit_index.json", "chipKIT", "chipKIT", "clicker2", "" }, //

				// Cosa
				{ "package_cosa_index.json", "Cosa", "Cosa", "diecimila", "" }, //
				{ "package_cosa_index.json", "Cosa", "Cosa", "duemilanove", "" }, //
				{ "package_cosa_index.json", "Cosa", "Cosa", "leonardo", "" }, //
				{ "package_cosa_index.json", "Cosa", "Cosa", "mega1280", "" }, //
				{ "package_cosa_index.json", "Cosa", "Cosa", "mega2560", "" }, //
				{ "package_cosa_index.json", "Cosa", "Cosa", "micro", "" }, //
				{ "package_cosa_index.json", "Cosa", "Cosa", "nano", "" }, //
				{ "package_cosa_index.json", "Cosa", "Cosa", "pro-micro", "" }, //
				{ "package_cosa_index.json", "Cosa", "Cosa", "pro-micro-8", "" }, //
				{ "package_cosa_index.json", "Cosa", "Cosa", "pro-mini", "" }, //
				{ "package_cosa_index.json", "Cosa", "Cosa", "pro-mini-8", "" }, //
				{ "package_cosa_index.json", "Cosa", "Cosa", "uno", "" }, //
				{ "package_cosa_index.json", "Cosa", "Cosa", "attiny84-8", "" }, //
				{ "package_cosa_index.json", "Cosa", "Cosa", "attiny85-8", "" }, //
				{ "package_cosa_index.json", "Cosa", "Cosa", "attiny861-8", "" }, //
				{ "package_cosa_index.json", "Cosa", "Cosa", "atmega328-8", "" }, //
				{ "package_cosa_index.json", "Cosa", "Cosa", "mighty", "" }, //
				{ "package_cosa_index.json", "Cosa", "Cosa", "mighty-opt", "" }, //
				{ "package_cosa_index.json", "Cosa", "Cosa", "lilypad", "" }, //
				{ "package_cosa_index.json", "Cosa", "Cosa", "lilypad-usb", "" }, //

				// emoro AVR
				{ "package_index.json", "emoro", "EMORO 2560", "emoro2560", "" }, //

				// ESP8266 boards
				{ "package_esp8266com_index.json", "esp8266", "esp8266", "generic",
						".CpuFrequency=80\nFlashFreq=40\nFlashMode=dio\nUploadSpeed=115200\nFlashSize=512K64\nResetMethod=ck\nDebug=Disabled\nDebugLevel=None" },
				{ "package_esp8266com_index.json", "esp8266", "esp8266", "esp8285",
						"CpuFrequency=80\nUploadSpeed=115200\nFlashSize=1M512" },
				{ "package_esp8266com_index.json", "esp8266", "esp8266", "espduino",
						"CpuFrequency=80\nUploadSpeed=115200\nFlashSize=4M3M" },
				{ "package_esp8266com_index.json", "esp8266", "esp8266", "huzzah",
						"CpuFrequency=80\nUploadSpeed=115200\nFlashSize=4M3M" },
				{ "package_esp8266com_index.json", "esp8266", "esp8266", "espresso_lite_v1",
						"CpuFrequency=80\nUploadSpeed=115200\nFlashSize=4M3M\nResetMethod=nodemcu\nDebug=Disabled\nDebugLevel=None" },
				{ "package_esp8266com_index.json", "esp8266", "esp8266", "espresso_lite_v2",
						"CpuFrequency=80\nUploadSpeed=115200\nFlashSize=4M3M\nResetMethod=nodemcu\nDebug=Disabled\nDebugLevel=None" },
				{ "package_esp8266com_index.json", "esp8266", "esp8266", "phoenix_v1",
						"CpuFrequency=80\nUploadSpeed=115200\nFlashSize=4M3M\nResetMethod=nodemcu\nDebug=Disabled\nDebugLevel=None" },
				{ "package_esp8266com_index.json", "esp8266", "esp8266", "phoenix_v2",
						"CpuFrequency=80\nUploadSpeed=115200\nFlashSize=4M3M\nResetMethod=nodemcu\nDebug=Disabled\nDebugLevel=None" },
				{ "package_esp8266com_index.json", "esp8266", "esp8266", "nodemcu",
						"CpuFrequency=80\nUploadSpeed=115200\nFlashSize=4M3M" },
				{ "package_esp8266com_index.json", "esp8266", "esp8266", "nodemcuv2",
						"CpuFrequency=80\nUploadSpeed=115200\nFlashSize=4M3M" },
				{ "package_esp8266com_index.json", "esp8266", "esp8266", "modwifi",
						"CpuFrequency=80\nUploadSpeed=115200" },
				{ "package_esp8266com_index.json", "esp8266", "esp8266", "thing",
						"CpuFrequency=80\nUploadSpeed=115200" },
				{ "package_esp8266com_index.json", "esp8266", "esp8266", "thingdev",
						"CpuFrequency=80\nUploadSpeed=115200" },
				{ "package_esp8266com_index.json", "esp8266", "esp8266", "esp210",
						"CpuFrequency=80\nUploadSpeed=115200\nFlashSize=4M3M" },
				{ "package_esp8266com_index.json", "esp8266", "esp8266", "d1_mini",
						"CpuFrequency=80\nUploadSpeed=115200\nFlashSize=4M3M" },
				{ "package_esp8266com_index.json", "esp8266", "esp8266", "d1",
						"CpuFrequency=80\nUploadSpeed=115200\nFlashSize=4M3M" },
				{ "package_esp8266com_index.json", "esp8266", "esp8266", "espino",
						"CpuFrequency=80\nUploadSpeed=115200\nFlashSize=4M3M\nFlashMode=dio\nResetMethod=ck" },
				{ "package_esp8266com_index.json", "esp8266", "esp8266", "espinotee",
						"CpuFrequency=80\nUploadSpeed=115200\nFlashSize=4M3M" },
				{ "package_esp8266com_index.json", "esp8266", "esp8266", "wifinfo",
						"CpuFrequency=80\nUploadSpeed=115200\nESPModule=ESP07192\nResetMethod=nodemcu\nDebug=Disabled\nDebugLevel=None\nFlashFreq=40\nFlashMode=qio" },
				{ "package_esp8266com_index.json", "esp8266", "esp8266", "coredev",
						"CpuFrequency=80\nUploadSpeed=115200\nFlashSize=4M3M\nResetMethod=nodemcu\nDebug=Disabled\nDebugLevel=None\nFlashFreq=40\nFlashMode=qio\nLwIPVariant=Espressif" },

				// intel
				{ "package_index.json", "Intel", "Intel Curie Boards", "arduino_101", "" }, //
				{ "package_index.json", "Intel", "Intel i586 Boards", "izmir_fd", "" }, //
				{ "package_index.json", "Intel", "Intel i586 Boards", "izmir_fg", "" }, //
				{ "package_index.json", "Intel", "Intel i686 Boards", "izmir_ec", "" }, //

				// littleBits AVR
				{ "package_index.json", "littleBits", "littleBits Arduino AVR Modules", "w6_arduino", "" }, //

				// Microsoft win10
				// { "package_index.json", "Microsoft", "Windows 10 Iot Core",
				// "w10iotcore", "Processor=arm" },
				//

				// nucdino
				{ "package_dfrobot_index.json", "nucDuino", "Bluno M0 MainBoard & DFRduino M0 MainBoard", "DFRDuino",
						"" }, //
				{ "package_dfrobot_index.json", "nucDuino", "Bluno M0 MainBoard & DFRduino M0 MainBoard", "BlunoM0",
						"" }, //

				// RedBearLab
				{ "package_redbearlab_index.json", "RedBearLab", "RedBearLab AVR Boards", "blend", "" }, //
				{ "package_redbearlab_index.json", "RedBearLab", "RedBearLab AVR Boards", "blendmicro8", "" }, //
				{ "package_redbearlab_index.json", "RedBearLab", "RedBearLab AVR Boards", "blendmicro16", "" }, //
				{ "package_redbearlab_index.json", "RedBearLab", "RedBearLab nRF51822 Boards (32-bits ARM Cortex-M0)",
						"nRF51822", "" }, //
				{ "package_redbearlab_index.json", "RedBearLab", "RedBearLab nRF51822 Boards (32-bits ARM Cortex-M0)",
						"nRF51822_NANO", "" }, //
				{ "package_redbearlab_index.json", "RedBearLab", "RedBearLab nRF51822 Boards (32-bits ARM Cortex-M0)",
						"nRF51822_32KB", "" }, //
				{ "package_redbearlab_index.json", "RedBearLab", "RedBearLab nRF51822 Boards (32-bits ARM Cortex-M0)",
						"nRF51822_NANO_32KB", "" }, //

				// Sparkfun AVR
				{ "package_sparkfun_index.json", "SparkFun", "SparkFun AVR Boards", "RedBoard", "" }, //
				{ "package_sparkfun_index.json", "SparkFun", "SparkFun AVR Boards", "makeymakey", "" }, //
				{ "package_sparkfun_index.json", "SparkFun", "SparkFun AVR Boards", "promicro", "cpu=8MHzatmega32U4" }, //
				{ "package_sparkfun_index.json", "SparkFun", "SparkFun AVR Boards", "fiov3", "" }, //
				{ "package_sparkfun_index.json", "SparkFun", "SparkFun AVR Boards", "qduinomini", "" }, //
				{ "package_sparkfun_index.json", "SparkFun", "SparkFun AVR Boards", "digitalsandbox", "" }, //
				{ "package_sparkfun_index.json", "SparkFun", "SparkFun AVR Boards", "megapro", "cpu=atmega25603V3" }, //
				{ "package_sparkfun_index.json", "SparkFun", "SparkFun AVR Boards", "RedBot", "" }, //
				{ "package_sparkfun_index.json", "SparkFun", "SparkFun AVR Boards", "Serial7Seg", "" }, //
				{ "package_sparkfun_index.json", "SparkFun", "SparkFun AVR Boards", "atmega128rfa1", "" }, //

				// TeeOnArdu avr

				{ "package_adafruit_index.json", "TeeOnArdu", "Adafruit TeeOnArdu", "TeeOnArdu",
						"UsbType=serial\nKeyLayout=en-us" }, //
				{ "package_adafruit_index.json", "TeeOnArdu", "Adafruit TeeOnArdu", "FloraTeensyCore",
						"UsbType=midi\nKeyLayout=fr-ca" }, //
				{ "package_adafruit_index.json", "TeeOnArdu", "Adafruit TeeOnArdu", "CirPlayTeensyCore",
						"UsbType=serial\nKeyLayout=usint" }, //
																// Talk2
				{ "package_talk2.wisen.com_index.json", "Talk2", "Talk2 AVR Boards", "whispernode", "mhz=16MHz" }, //

		});
	}

	/*
	 * In new new installations (of the Sloeber development environment) the
	 * installer job will trigger downloads These mmust have finished before we
	 * can start testing
	 */
	@BeforeClass
	public static void WaitForInstallerToFinish() {
		installAdditionalBoards();
		waitForAllJobsToFinish();
	}

	public static void waitForAllJobsToFinish() {
		try {
			Thread.sleep(10000);

			IJobManager jobMan = Job.getJobManager();

			while (!jobMan.isIdle()) {
				Thread.sleep(5000);
			}
			// As nothing is running now we can start installing

		} catch (InterruptedException e) {
			e.printStackTrace();
			fail("can not find installerjob");
		}
	}

	public static void installAdditionalBoards() {
		String[] packageUrlsToAdd = { "https://adafruit.github.io/arduino-board-index/package_adafruit_index.json",
				"https://mcudude.github.io/MightyCore/package_MCUdude_MightyCore_index.json",
				"https://raw.githubusercontent.com/mikaelpatel/Cosa/master/package_cosa_index.json",
				"https://raw.githubusercontent.com/sparkfun/Arduino_Boards/master/IDE_Board_Manager/package_sparkfun_index.json",
				"https://redbearlab.github.io/arduino/package_redbearlab_index.json",
				"https://github.com/chipKIT32/chipKIT-core/raw/master/package_chipkit_index.json",
				"http://talk2arduino.wisen.com.au/master/package_talk2.wisen.com_index.json",
				"https://raw.githubusercontent.com/ThamesValleyReprapUserGroup/Beta-TVRRUG-Mendel90/master/Added-Documents/OMC/package_omc_index.json",
				"https://raw.githubusercontent.com/AloriumTechnology/Arduino_Boards/master/package_aloriumtech_index.json",
				"https://raw.githubusercontent.com/geolink/opentracker-arduino-board/master/package_opentracker_index.json",
				"https://raw.githubusercontent.com/ElektorLabs/arduino/master/package_elektor-labs.com_ide-1.6.6_index.json",
				"https://ardhat.github.io/ardhat-board-support/arduino/package_ardhat_index.json",
				"https://s3.amazonaws.com/quirkbot-downloads-production/downloads/package_quirkbot.com_index.json",
				"https://redbearlab.github.io/arduino/package_redbear_index.json",
				"https://per1234.github.io/Ariadne-Bootloader/package_codebendercc_ariadne-bootloader_index.json",
				"http://panstamp.org/arduino/package_panstamp_index.json",
				"https://raw.githubusercontent.com/TKJElectronics/Balanduino/master/package_tkj_balanduino_index.json",
				"https://raw.githubusercontent.com/Lauszus/Sanguino/master/package_lauszus_sanguino_index.json",
				"https://raw.githubusercontent.com/damellis/attiny/ide-1.6.x-boards-manager/package_damellis_attiny_index.json",
				"http://drazzy.com/package_drazzy.com_index.json",
				"https://raw.githubusercontent.com/carlosefr/atmega/master/package_carlosefr_atmega_index.json",
				"https://dl.dropboxusercontent.com/u/2807353/femtoCore/package_femtocow_attiny_index.json",
				"https://raw.githubusercontent.com/RiddleAndCode/RnCAtmega256RFR2/master/Board_Manager/package_rnc_index.json",
				"http://rfduino.com/package_rfduino_index.json",
				"https://raw.githubusercontent.com/akafugu/akafugu_core/master/package_akafugu_index.json",
				"https://github.com/Ameba8195/Arduino/raw/master/release/package_realtek.com_ameba_index.json",
				"https://raw.githubusercontent.com/Seeed-Studio/Seeeduino-Boards/master/package_seeeduino_index.json",
				"http://download.labs.mediatek.com/package_mtk_linkit_index.json",
				"http://download.labs.mediatek.com/package_mtk_linkit_smart_7688_index.json",
				"https://raw.githubusercontent.com/NicoHood/HoodLoader2/master/package_NicoHood_HoodLoader2_index.json",
				"http://digistump.com/package_digistump_index.json",
				"https://raw.githubusercontent.com/OLIMEX/Arduino_configurations/master/AVR/package_olimex_avr_index.json",
				"https://raw.githubusercontent.com/OLIMEX/Arduino_configurations/master/PIC/package_olimex_pic_index.json",
				"https://raw.githubusercontent.com/OLIMEX/Arduino_configurations/master/STM/package_olimex_stm_index.json",
				"http://navspark.mybigcommerce.com/content/package_navspark_index.json",
				"https://raw.githubusercontent.com/CytronTechnologies/Cytron-Arduino-URL/master/package_cytron_index.json",
				"https://www.mattairtech.com/software/arduino/package_MattairTech_index.json",
				"http://www.dwengo.org/sites/default/files/package_dwengo.org_dwenguino_index.json",
				"http://downloads.sodaq.net/package_sodaq_index.json",
				"https://zevero.github.io/avr_boot/package_zevero_avr_boot_index.json",
				"https://raw.githubusercontent.com/eightdog/laika_arduino/master/IDE_Board_Manager/package_project_laika.com_index.json",
				"https://lowpowerlab.github.io/MoteinoCore/package_LowPowerLab_index.json",
				"https://rawgit.com/hunianhang/nufront_arduino_json/master/package_tl7788_index.json",
				"http://downloads.konekt.io/arduino/package_konekt_index.json",
				"https://raw.githubusercontent.com/oshlab/Breadboard-Arduino/master/avr/boardsmanager/package_oshlab_breadboard_index.json",
				"https://raw.githubusercontent.com/feilipu/feilipu.github.io/master/package_goldilocks_index.json",
				"http://www.leonardomiliani.com/repository/package_leonardomiliani.com_index.json",
				"https://thomasonw.github.io/ATmegaxxM1-C1/package_thomasonw_ATmegaxxM1-C1_index.json",
				"http://hidnseek.github.io/hidnseek/package_hidnseek_boot_index.json",
				"http://clkdiv8.com/download/package_clkdiv8_index.json",
				"https://mcudude.github.io/MegaCore/package_MCUdude_MegaCore_index.json",
				"https://arduboy.github.io/board-support/package_arduboy_index.json",
				"https://github.com/ms-iot/iot-utilities/raw/master/IotCoreAppDeployment/ArduinoIde/package_iotcore_ide-1.6.6_index.json",
				// sourceforge download stuff
				// "https://sourceforge.net/projects/simba-arduino/files/avr/package_simba_avr_index.json",
				// sourceforge download stuff
				// "https://sourceforge.net/projects/simba-arduino/files/sam/package_simba_sam_index.json",
				// sourceforge download stuff
				// "https://sourceforge.net/projects/simba-arduino/files/esp/package_simba_esp_index.json",
				"https://mcudude.github.io/MiniCore/package_MCUdude_MiniCore_index.json",
				"https://raw.githubusercontent.com/DFRobot/DFRobotDuinoBoard/master/package_dfrobot_index.json",
				"https://github.com/IntoRobot/IntoRobotPackages-ArduinoIDE/releases/download/1.0.0/package_intorobot_index.json",
				"https://raw.githubusercontent.com/stm32duino/BoardManagerFiles/master/STM32/package_stm_index.json" };
		BoardsManager.addPackageURLs(new HashSet<>(Arrays.asList(packageUrlsToAdd)), true);
		BoardsManager.installAllLatestPlatforms();
		BoardsManager.referenceLocallInstallation(teensyInstall);
	}

	@Test
	public void testBoard() {
		BoardDescriptor boardid = BoardsManager.getBoardID(this.mJsonFileName, this.mPackageName, this.mPlatform,
				this.mBoardID, this.myOptions);
		if (boardid == null) {
			fail("Board " + this.mJsonFileName + " " + " " + this.mPackageName + " " + this.mPlatform + " "
					+ this.mBoardID + " not found");
			return;
		}
		BuildAndVerify(boardid);

	}

	public static void BuildAndVerify(BoardDescriptor boardid) {

		IProject theTestProject = null;
		CodeDescriptor codeDescriptor = CodeDescriptor.createDefaultIno();
		NullProgressMonitor monitor = new NullProgressMonitor();
		String projectName = String.format("%03d_", new Integer(mCounter++)) + boardid.getBoardID();
		try {

			theTestProject = boardid.createProject(projectName, null, ConfigurationDescriptor.getDefaultDescriptors(),
					codeDescriptor, monitor);
			waitForAllJobsToFinish(); // for the indexer
		} catch (Exception e) {
			e.printStackTrace();
			fail("Failed to create the project:" + projectName);
			return;
		}
		try {
			theTestProject.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
			if (hasBuildErrors(theTestProject)) {
				fail("Failed to compile the project:" + projectName + " build errors");
			}
		} catch (CoreException e) {
			e.printStackTrace();
			fail("Failed to compile the project:" + boardid.getBoardName() + " exception");
		}
	}

	private static boolean hasBuildErrors(IProject project) throws CoreException {
		IMarker[] markers = project.findMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
		for (IMarker marker : markers) {
			if (marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO) == IMarker.SEVERITY_ERROR) {
				return true;
			}
		}
		return false;
	}
}
