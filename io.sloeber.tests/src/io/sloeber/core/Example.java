package io.sloeber.core;

import java.util.LinkedList;

import org.eclipse.core.runtime.IPath;

@SuppressWarnings("nls")
public class Example {
	private static LinkedList<String> usesSerialExampleList = null;
	private static LinkedList<String> usesSerial1ExampleList = null;
	private static LinkedList<String> usesKeyboardExampleList = null;
	private static LinkedList<String> usesFlightSimExampleList = null;
	private static LinkedList<String> usesMidiExampleList = null;

	private String myExampleName;
	private boolean myUsesSerial;
	private boolean usesSerial1;
	private boolean usesKeyboard;
	private boolean usesFlightSim;
	private boolean usesMidi;
	private String myLibName;
	private IPath myPath;



	public Example(String exampleName,String libName, IPath path) {
		myExampleName = exampleName;
		myUsesSerial = getUsesSerialExamples().contains(myExampleName);
		usesSerial1 = getUsesSerial1Examples().contains(myExampleName);
		usesKeyboard = getUsesKeyboardExamples().contains(myExampleName);
		usesFlightSim = getUsesusesFlightSimExamples().contains(myExampleName);
		usesMidi = getUsesusesMidiExamples().contains(myExampleName) || myExampleName.contains("USB_MIDI");
		myLibName=libName;
		myPath=path;
	}
	public IPath getPath() {
		return myPath;
	}
	public String getName() {
		return myExampleName;
	}

	public boolean UsesSerial() {
		return myUsesSerial;
	}

	public boolean UsesSerial1() {
		return usesSerial1;
	}

	public boolean UsesKeyboard() {
		return usesKeyboard;
	}

	public boolean UsesFlightSim() {
		return usesFlightSim;
	}

	public boolean UsesMidi() {
		return usesMidi;
	}

	public String getLibName() {
		return myLibName;
	}

	public String getInoName() {
		return myPath.lastSegment();
	}

	private static LinkedList<String> getUsesusesMidiExamples() {
		if (usesMidiExampleList != null) {
			return usesMidiExampleList;
		}
		usesMidiExampleList = new LinkedList<>();
		usesMidiExampleList.add("examples? Teensy? USB_FlightSim?ThrottleServo");

		return usesMidiExampleList;
	}

	private static LinkedList<String> getUsesusesFlightSimExamples() {
		if (usesFlightSimExampleList != null) {
			return usesFlightSimExampleList;
		}
		usesFlightSimExampleList = new LinkedList<>();
		usesFlightSimExampleList.add("examples? Teensy? USB_FlightSim?BlinkTransponder");
		usesFlightSimExampleList.add("examples? Teensy? USB_FlightSim?FrameRateDisplay");
		usesFlightSimExampleList.add("examples? Teensy? USB_FlightSim?NavFrequencyr");
		usesFlightSimExampleList.add("examples? Teensy? USB_FlightSim?ThrottleServo");

		return usesFlightSimExampleList;
	}

	private static LinkedList<String> getUsesSerial1Examples() {
		if (usesSerial1ExampleList != null) {
			return usesSerial1ExampleList;
		}
		usesSerial1ExampleList = new LinkedList<>();
		usesSerial1ExampleList.add("examples? 04.Communication?MultiSerial");
		usesSerial1ExampleList.add("examples? 04.Communication?SerialPassthrough");
		usesSerial1ExampleList.add("examples? Teensy? Serial?EchoBoth");
		return usesSerial1ExampleList;
	}

	private static LinkedList<String> getUsesKeyboardExamples() {
		if (usesKeyboardExampleList != null) {
			return usesKeyboardExampleList;
		}
		usesKeyboardExampleList = new LinkedList<>();
		usesKeyboardExampleList.add("examples? 09.USB? Keyboard?KeyboardLogout");
		usesKeyboardExampleList.add("examples? 09.USB? Keyboard?KeyboardMessage");
		usesKeyboardExampleList.add("examples? 09.USB? Keyboard?KeyboardReprogram");
		usesKeyboardExampleList.add("examples? 09.USB? Keyboard?KeyboardSerial");
		usesKeyboardExampleList.add("examples? 09.USB? Mouse?ButtonMouseControl");
		usesKeyboardExampleList.add("examples? 09.USB? Mouse?JoystickMouseControl");
		usesKeyboardExampleList.add("examples? 09.USB?KeyboardAndMouseControl");
		return usesKeyboardExampleList;
	}

	private static LinkedList<String> getUsesSerialExamples() {
		if (usesSerialExampleList != null) {
			return usesSerialExampleList;
		}
		usesSerialExampleList = new LinkedList<>();
		usesSerialExampleList.add("examples? 01.Basics?AnalogReadSerial");
		usesSerialExampleList.add("examples? 01.Basics?DigitalReadSerial");
		usesSerialExampleList.add("examples? 01.Basics?ReadAnalogVoltage");
		usesSerialExampleList.add("examples? 02.Digital?DigitalInputPullup");
		usesSerialExampleList.add("examples? 02.Digital?StateChangeDetection");
		usesSerialExampleList.add("examples? 02.Digital?tonePitchFollower");
		usesSerialExampleList.add("examples? 03.Analog?AnalogInOutSerial");
		usesSerialExampleList.add("examples? 03.Analog?Smoothing");
		usesSerialExampleList.add("examples? 04.Communication?ASCIITable");
		usesSerialExampleList.add("examples? 04.Communication?Dimmer");
		usesSerialExampleList.add("examples? 04.Communication?Graph");
		usesSerialExampleList.add("examples? 04.Communication?Midi");
		usesSerialExampleList.add("examples? 04.Communication?PhysicalPixel");
		usesSerialExampleList.add("examples? 04.Communication?ReadASCIIString");
		usesSerialExampleList.add("examples? 04.Communication?SerialCallResponse");
		usesSerialExampleList.add("examples? 04.Communication?SerialCallResponseASCII");
		usesSerialExampleList.add("examples? 04.Communication?SerialEvent");
		usesSerialExampleList.add("examples? 04.Communication?VirtualColorMixer");
		usesSerialExampleList.add("examples? 05.Control?IfStatementConditional");
		usesSerialExampleList.add("examples? 05.Control?switchCase");
		usesSerialExampleList.add("examples? 05.Control?switchCase2");
		usesSerialExampleList.add("examples? 06.Sensors?ADXL3xx");
		usesSerialExampleList.add("examples? 06.Sensors?Knock");
		usesSerialExampleList.add("examples? 06.Sensors?Memsic2125");
		usesSerialExampleList.add("examples? 06.Sensors?Ping");
		usesSerialExampleList.add("examples? 08.Strings?CharacterAnalysis");
		usesSerialExampleList.add("examples? 08.Strings?StringAdditionOperator");
		usesSerialExampleList.add("examples? 08.Strings?StringAppendOperator");
		usesSerialExampleList.add("examples? 08.Strings?StringCaseChanges");
		usesSerialExampleList.add("examples? 08.Strings?StringCharacters");
		usesSerialExampleList.add("examples? 08.Strings?StringComparisonOperators");
		usesSerialExampleList.add("examples? 08.Strings?StringConstructors");
		usesSerialExampleList.add("examples? 08.Strings?StringIndexOf");
		usesSerialExampleList.add("examples? 08.Strings?StringLength");
		usesSerialExampleList.add("examples? 08.Strings?StringLengthTrim");
		usesSerialExampleList.add("examples? 08.Strings?StringReplace");
		usesSerialExampleList.add("examples? 08.Strings?StringStartsWithEndsWith");
		usesSerialExampleList.add("examples? 08.Strings?StringSubstring");
		usesSerialExampleList.add("examples? 08.Strings?StringToInt");
		usesSerialExampleList.add("examples? 10.StarterKit_BasicKit?p03_LoveOMeter");
		usesSerialExampleList.add("examples? 10.StarterKit_BasicKit?p04_ColorMixingLamp");
		usesSerialExampleList.add("examples? 10.StarterKit_BasicKit?p05_ServoMoodIndicator");
		usesSerialExampleList.add("examples? 10.StarterKit_BasicKit?p07_Keyboard");
		usesSerialExampleList.add("examples? 10.StarterKit_BasicKit?p12_KnockLock");
		usesSerialExampleList.add("examples? 10.StarterKit_BasicKit?p13_TouchSensorLamp");
		usesSerialExampleList.add("examples? 10.StarterKit_BasicKit?p14_TweakTheArduinoLogo");
		usesSerialExampleList.add("examples? 11.ArduinoISP?ArduinoISP");
		usesSerialExampleList.add("examples? Teensy? Tutorial3?HelloSerialMonitor");
		usesSerialExampleList.add("examples? Teensy? Tutorial3?Pushbutton");
		usesSerialExampleList.add("examples? Teensy? Tutorial3?PushbuttonPullup");
		usesSerialExampleList.add("examples? Teensy? Tutorial3?PushbuttonRGBcolor");
		usesSerialExampleList.add("examples? Teensy? Tutorial4?AnalogInput");
		usesSerialExampleList.add("examples? Teensy? Tutorial4?TemperatureNumberOnly");
		usesSerialExampleList.add("examples? Teensy? Tutorial4?TemperatureScaled");
		usesSerialExampleList.add("examples? Teensy? Tutorial4?TemperatureScaledMulti");
		return usesSerialExampleList;
	}



}
