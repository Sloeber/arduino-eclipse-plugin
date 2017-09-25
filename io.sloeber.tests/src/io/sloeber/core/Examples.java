package io.sloeber.core;

import java.util.LinkedList;

@SuppressWarnings("nls")
public class Examples {
	private static LinkedList<String> usesSerialExampleList = null;
	private static LinkedList<String> usesSerial1ExampleList = null;
	private static LinkedList<String> usesKeyboardExampleList = null;

	public static LinkedList<String> getUsesSerial1Examples() {
		if (usesSerial1ExampleList != null) {
			return usesSerial1ExampleList;
		}
		usesSerial1ExampleList = new LinkedList<>();
		usesSerial1ExampleList.add("examples? 04.Communication?MultiSerial");
		return usesSerial1ExampleList;
	}

	public static LinkedList<String> getUsesKeyboardExamples() {
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

	public static LinkedList<String> getUsesSerialExamples() {
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
		return usesSerialExampleList;
	}

}
