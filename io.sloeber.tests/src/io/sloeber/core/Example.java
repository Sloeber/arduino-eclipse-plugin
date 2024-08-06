package io.sloeber.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.core.api.LibraryManager;
import io.sloeber.providers.ESP32;
import io.sloeber.providers.MCUBoard;
import io.sloeber.providers.Teensy;

@SuppressWarnings({ "nls" })
public class Example extends io.sloeber.core.internal.Example{
    private AttributesCode myRequiredBoardAttributes;
    private static int noBoardFoundCount = 0;


    public AttributesCode getRequiredBoardAttributes() {
        return myRequiredBoardAttributes;
    }

    public Example(String fqn, IPath path) throws Exception {
        myFQN =  Path.fromPortableString(fqn);
        myExampleLocation = path;
        String libName=calcLibName();
        if(libName!=null &&(!libName.isBlank())) {
        	Set<String>libNames=new HashSet<>();
        	libNames.add(libName);
        	myLibs=LibraryManager.getLatestInstallableLibraries(libNames);
        }
        myRequiredBoardAttributes = new AttributesCode(fqn);
        myRequiredBoardAttributes.serial = examplesUsingSerial().contains(fqn);
        myRequiredBoardAttributes.serial1 = examplesUsingSerial1().contains(fqn);
        myRequiredBoardAttributes.serialUSB = examplesUsingSerialUSB().contains(fqn);
        myRequiredBoardAttributes.keyboard = examplesUsingKeyboard().contains(fqn);
        myRequiredBoardAttributes.flightSim = examplesUsingFlightSim().contains(fqn);
        myRequiredBoardAttributes.joyStick = examplesUsingJoyStick().contains(fqn);
        myRequiredBoardAttributes.mouse = examplesUsingMouse().contains(fqn);
        myRequiredBoardAttributes.tone = examplesUsingTone().contains(fqn);
        myRequiredBoardAttributes.wire1 = examplesUsingWire1().contains(fqn);
        myRequiredBoardAttributes.buildInLed = fqn.contains("Blink")||examplesUsingBuildInLed().contains(fqn);
        myRequiredBoardAttributes.midi = examplesUsingMidi().contains(fqn) || fqn.contains("USB_MIDI");
        //        myRequiredBoardAttributes.teensy = fqn.startsWith("Example/Teensy");
        myRequiredBoardAttributes.worksOutOfTheBox = !failingExamples().contains(fqn);
        myRequiredBoardAttributes.myCompatibleBoardIDs.add( getRequiredBoardID(fqn));
        //        myRequiredBoardAttributes.mo_mcu = examplesUsingMCUmo().contains(fqn);
        myRequiredBoardAttributes.rawHID = fqn.contains("USB_RawHID");

        myRequiredBoardAttributes.myNumAD = getNumADCUsedInExample(fqn);
        myRequiredBoardAttributes.directMode = examplesUsingDirectMode().contains(fqn);

        myRequiredBoardAttributes.myCompatibleBoardIDs.remove(null);
        myRequiredBoardAttributes = myRequiredBoardAttributes.or(Libraries.getCodeAttributes(myExampleLocation));
    }


    private static int getNumADCUsedInExample(String myFQN2) {
        switch (myFQN2) {
        case "Example/04.Communication/VirtualColorMixer":
        case "Example/10.StarterKit_BasicKit/p04_ColorMixingLamp":
            return 3;
        case "Example/06.Sensors/ADXL3xx":
            return 4;
        case "Example/08.Strings/StringComparisonOperators":
            return 6;
        default:
            return 0;
        }
    }

    String calcLibName() {
        if (myFQN.segmentCount() == 4) {
            if ("Library".equals(myFQN.segment(0))) {
                return myFQN.segment(2);
            }
        }
        return null;
    }



    public String getFQN() {
        return myFQN.toString();
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

    private static LinkedList<String> examplesUsingSerialUSB() {
        LinkedList<String> ret = new LinkedList<>();
        ret.add("Example/11.ArduinoISP/ArduinoISP");
        return ret;
    }

    private static LinkedList<String> examplesUsingDirectMode() {
        LinkedList<String> ret = new LinkedList<>();
        ret.add("Example/10.StarterKit_BasicKit/p13_TouchSensorLamp");
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


        ret.add("Library/Managed/LiquidCrystal/SerialDisplay");
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

    private static LinkedList<String> examplesUsingTone() {
        LinkedList<String> ret = new LinkedList<>();
        ret.add("Example/10.StarterKit_BasicKit/p06_LightTheremin");
        ret.add("Example/02.Digital/toneMultiple");
        ret.add("Example/02.Digital/toneMelody");
        ret.add("Example/02.Digital/toneKeyboard");
        ret.add("Example/02.Digital/tonePitchFollower");
        ret.add("Example/10.StarterKit_BasicKit/p07_Keyboard");
        return ret;
    }

    private static LinkedList<String> examplesUsingWire1() {
        LinkedList<String> ret = new LinkedList<>();
        ret.add("Example/Teensy/USB_Joystick/Basic");
        ret.add("Example/Teensy/USB_Joystick/Buttons");
        ret.add("Example/Teensy/USB_Joystick/Complete");
        ret.add("Example/Teensy/USB_RawHID/Basic");
        ret.add("Library/Managed/Adafruit_BME280_Library/advancedsettings");
        ret.add("Library/Managed/AS3935MI/AS3935MI_LightningDetector_otherInterfaces");
        return ret;
    }

    private static LinkedList<String> examplesUsingBuildInLed() {
        LinkedList<String> ret = new LinkedList<>();
        ret.add("Library/Managed/SD/NonBlockingWrite");
        return ret;
    }

    /*
     * These examples that are known to fail
     */
    private static LinkedList<String> failingExamples() {
        LinkedList<String> ret = new LinkedList<>();
        /*
         * Because you can not define a enum 2 times Sloeber can not add the enum in
         * Sloeber.ino.cpp as a result the function declarations using these enums
         * generate a error because the enum is not defined. The examples below fail due
         * to this
         */
        ret.add("Library/Managed/_2020Bot_Library/_2020Bot_Demo");
        // These examples are the processing part and are not a deal of sloeber
        ret.add("Library/Managed/Adafruit_BNO055/bunny/processing/cuberotate");
        // manual action is needed for following examples
        ret.add("Library/Managed/AbsoluteMouse/DevKit");
        ret.add("Library/Managed/Accessories/CANCommander");
        ret.add("Library/Managed/Accessories/Demo");
        ret.add("Library/Managed/Accessories/Full");
        ret.add("Library/Managed/Accessories/Group");
        ret.add("Library/Managed/Accessories/LightFading");
        ret.add("Library/Managed/Accessories/LMD18200");
        ret.add("Library/Managed/Accessories/Servos");
        ret.add("Library/Managed/Accessories/SignalFrench");
        ret.add("Library/Managed/Accessories/Signals4x3");
        ret.add("Library/Managed/Accessories/SimpleLed");
        ret.add("Library/Managed/Accessories/SimpleLedMulti");
        ret.add("Library/Managed/Accessories/Stepper");
        ret.add("Library/Managed/Accessory/Shield/OLED/example/Adafruit");
        ret.add("Library/Managed/Accessory/Shield/temp/humidity/oled");
        // at the time of testing there were case sensetivity issues
        ret.add("Library/Managed/AutoAnalogAudio/SDAudio/SdAudioRecording");
        ret.add("Library/Managed/AutoAnalogAudio/SDAudio/SdAudioWavPlayer");
        ret.add("Library/Managed/AutoAnalogAudio/AudioRadioRelay");
        ret.add("Library/Managed/AutoAnalogAudio/WirelessMicrophone");
        ret.add("Library/Managed/AutoAnalogAudio/WirelessSpeaker");
        ret.add("Library/Managed/AutoAnalogAudio/WirelessTx_RPi");
        // needs to get the defines and includes right
        ret.add("Library/Managed/Accessories/Locoduino.org/Programme4");
        ret.add("Library/Managed/Accessories/Locoduino.org/Programme5");
        // Should be build on a stm32 (nucleo as far as I get)
        // but these bards require #927 (and probably more :-(
        ret.add("Library/Managed/ADCTouchSensor/CapacitiveController");
        ret.add("Library/Managed/ADCTouchSensor/CapacitivePiano");
        // failed in SPI :-(
        ret.add("Library/Managed/Adafruit_TinyFlash/TrinketPlayer");
        // not sure how this could work
        ret.add("Library/Managed/Adafruit_VS1053_Library/feather_midi");
        // all kinds of incompatibility issues I guess
        ret.add("Library/Managed/Andee/Lesson_08_Miscellaneous/Lesson_8b_Use_Bluetooth_Signal_Strength_to_Control_Things");
        ret.add("Library/Managed/Andee/Lesson_08_Miscellaneous/Lesson_8c_Change_Andee_Bluetooth_Device_Name");
        ret.add("Library/Managed/Andee/Lesson_08_Miscellaneous/Lesson_8d_How_to_Read_and_Write_to_SD_Card");
        ret.add("Library/Managed/Andee/Lesson_08_Miscellaneous/Lesson_8f_Using_Andee_IO_Pins_as_OUTPUT_Pins");
        ret.add("Library/Managed/Andee/Lesson_08_Miscellaneous/Lesson_8g_Getting_Inputs_from_Andee_IO_Pins");
        ret.add("Library/Managed/Andee/Lesson_09_SmartDevice_Control/Lesson_9a_Get_Device_Bluetooth_MAC_Address_ANDROID_Only");
        ret.add("Library/Managed/Andee/Lesson_09_SmartDevice_Control/Lesson_9b_Filter_Devices_by_Bluetooth_MAC_Address_ANDROID_Only");
        // doc says not finished
        ret.add("Library/Managed/ANT-Arduino/NativeAnt");
        // uses missing library MobileBLE
        ret.add("Library/Managed/ArduinoBlue/differentialDriveCar");
        // defining struct/enum in ino file
        ret.add("Library/Managed/ArduinoJson/JsonConfigFile");
        ret.add("Library/Managed/Arduboy2/RGBled");
        // error: no matching function for call to 'aREST_UI::addToBuffer(const char
        ret.add("Library/Managed/aREST_UI/ESP8266");
        ret.add("Library/Managed/aREST_UI/WiFi_CC3000");
        ret.add("Library/Managed/aREST_UI/WildFire");
        // uses arduinoWIFI
        ret.add("Library/Managed/Braccio/braccioOfUnoWiFi");
        // uses lib that does not has lib folder= include-.h
        ret.add("Library/Managed/ArduinoCloud/SimpleCloudButtonYun");
        // usi!ng non exsisting methods
        ret.add("Library/Managed/CAN-BUS_Shield/gpioRead");
        ret.add("Library/Managed/CAN-BUS_Shield/gpioWrite");
        // using defines inino file to generate functions (not supported in Sloeber)
        ret.add("Library/Managed/Adafruit_VEML6070_Library/unittests");
        // uses a non existing header
        ret.add("Library/Managed/AESLib/complex");
        // using wrong sdfat librarie
        ret.add("Library/Managed/Arduino_OPL2_SimpleTone");
        ret.add("Library/Managed/Arduino_OPL2_Teensy_PlayDRO");
        ret.add("Library/Managed/Arduino_OPL2_Teensy_PlayIMF");
        // I don't recall why following examples didn't work
        ret.add("Library/Managed/arduino_ess_ess_yun");
        ret.add("Library/Managed/arduino_ess_linkit_one_dweet");
        ret.add("Library/Managed/AD7193/AD7193_VoltageMeasurePsuedoDifferential_Example");
        ret.add("Library/Managed/bunny_cuberotate/cuberotate");
        ret.add("Library/Managed/XPT2046_Touchscreen/ILI9341Test");
        ret.add("Library/Managed/Adafruit_AHRS/ahrs_mahony");
        ret.add("Library/Managed/Adafruit_BLEFirmata/StandardFirmata");
        ret.add("Library/Managed/Adafruit_BNO055/bunny/processing/cuberotate");
        ret.add("Library/Managed/Adafruit_GPS_Library/due_shield_sdlog");
        ret.add("Library/Managed/Adafruit_Graphic_VFD_Display_Library/GraphicVFDtest");
        ret.add("Library/Managed/Adafruit_GPS_Library/locus_erase");
        ret.add("Library/Managed/Adafruit_GPS_Library/shield_sdlog");
        ret.add("Library/Managed/Adafruit_HX8357_Library/breakouttouchpaint");
        ret.add("Library/Managed/Adafruit_ILI9341/breakouttouchpaint");
        ret.add("Library/Managed/Adafruit_ILI9341/onoffbutton_breakout");
        ret.add("Library/Managed/Adafruit_GPS_Library/echo");
        ret.add("Library/Managed/Adafruit_LED_Backpack_Library/wavface");
        ret.add("Library/Managed/Adafruit_SSD1306/ssd1306_128x64_i2c");
        ret.add("Library/Managed/Adafruit_SSD1306/ssd1306_128x64_spi");
        ret.add("Library/Managed/Adafruit_ST7735_Library/soft_spitftbitmap");
        ret.add("Library/Managed/Adafruit_TCS34725/colorview/processing/colorview");
        ret.add("Library/Managed/Adafruit_TinyRGBLCDShield/TinyHelloWorld");
        ret.add("Library/Managed/Akafugu_TWILiquidCrystal_Library/change_address");
        ret.add("Library/Managed/Akafugu_WireRtc_Library/alarm");
        ret.add("Library/Managed/ALA/RgbStripButton");
        ret.add("Library/Managed/APA102/GameOfLife");
        ret.add("Library/Managed/arduino-menusystem/led_matrix");
        ret.add("Library/Managed/arduino-menusystem/led_matrix_animated");
        ret.add("Library/Managed/Arduino_Low_Power/TianStandby");
        ret.add("Library/Managed/aREST/BLE");
        ret.add("Library/Managed/aREST/ESP32");
        ret.add("Library/Managed/aREST/ESP32_cloud");
        ret.add("Library/Managed/ArduinoHttpClient/DweetGet");
        ret.add("Library/Managed/ArduinoMenu_library/adafruitGfx/lcdMono/lcdMono");
        ret.add("Library/Managed/ArduinoMenu_library/adafruitGfx/tft/tft");
        ret.add("Library/Managed/ArdVoice/Sample2-Complex");
        ret.add("Library/Managed/Aspen_SIM800/Access_HTTP");
        ret.add("Library/Managed/Awesome/advanced/how_fast");
        ret.add("Library/Managed/Awesome/advanced/lie_detector");
        ret.add("Library/Managed/AzureIoTUtility/simplesample_http");
        ret.add("Library/Managed/BLEPeripheral/ir_bridge");
        ret.add("Library/Managed/BLEPeripheral/temp_sensor");
        ret.add("Library/Managed/Brasilino/Basicos/controleGradual");
        ret.add("Library/Managed/ClosedCube_HDC1010/hdc1010demo");
        ret.add("Library/Managed/Chrono/Resolutions");
        ret.add("Library/Managed/Chrono/StopResume");
        ret.add("Library/Managed/ConfigurableFirmata/ConfigurableFirmataWiFi");
        ret.add("Library/Managed/ControleForno/configuravel");
        ret.add("Library/Managed/CopyThreads/c");
        ret.add("Library/Managed/ArduinoCloud/SimpleCloudButtoBrzo_I2C");
        ret.add("Library/Managed/CopyThreads/FromReadme");
        ret.add("Library/Managed/DallasTemperature/Multibus_simple");
        ret.add("Library/Managed/DecodeIR/InfraredDecode");
        ret.add("Library/Managed/AutoAnalogAudio/SimpleSine");
        ret.add("Library/Managed/DimSwitch/DimSwitchTester-ESP-MQTT");
        ret.add("Library/Managed/DS3231/echo_time");
        ret.add("Library/Managed/Easy_NeoPixels");
        ret.add("Library/Managed/DallasTemperature/AlarmHandler");
        ret.add("Library/Managed/AmazonDRS/amazonDashNfc");
        ret.add("Library/Managed/Andee/Lesson_02_Buttons/Lesson_2h_Using_Buttons_to_Control_Servos");
        ret.add("Library/Managed/Andee/Project_Christmas_Lights_and_Annoying_Music");
        ret.add("Library/Managed/Andee/Project_Rubber_Band_Launcher");
        ret.add("Library/Managed/Andee/Project_Time_Automated_Data_Logger");
        ret.add("Library/Managed/ANT-Arduino_library/NativeAnt");
        ret.add("Library/Managed/arduino-fsm/timed_switchoff");
        ret.add("Library/Managed/BME280/BME_280_BRZO_I2C_Test");
        ret.add("Library/Managed/Adafruit_seesaw_Library/DAP");
        // uses unknown NO_ERROR
        ret.add("Library/Managed/ClosedCube_TCA9546A/tca9546a_sht31d");
        // uses dht.h from dht_sensor_lib
        ret.add("Library/Managed/CMMC_MQTT_Connector/basic_dht");
        ret.add("Library/Managed/ArduinoLearningKitStarter/boardTest");
        // uses altsoftserial and then Serial2????
        ret.add("Library/Managed/CMMC_NB-IoT/example1");
        // some bu!g I guess
        ret.add("Library/Managed/CopyThreads/ExamplesFromReadme");
        ret.add("Library/Managed/CRC_Simula_Arduino_IDE_Library/Simula_BehaviorTree");
        // empty sketch??
        ret.add("Library/Managed/DFW/ProvisionController");
        // error: 'mapSensor' was not declared in this scope
        ret.add("Library/Managed/AD_Sensors/ConstrainAnalogSensor");
        // error: 'sensor' was not declared in this scope
        ret.add("Library/Managed/AD_Sensors/MapAndConstrainAnalogSensor");
        // ess-yun.ino:17:12: error: no matching function for call to
        // 'HttpClient::HttpClient()'
        ret.add("Library/Managed/arduino-ess/ess-yun");
        // I have no linket board in test setup
        ret.add("Library/Managed/arduino-ess/linkit-one-dweet");
        //cores\arduino/WString.h:38:74: error: statement-expressions
        ret.add("Library/Managed/ArduinoTrace/TraceFromGlobalScope");
        // no matching function for call to 'aREST::handle(EthernetClient&)'
        ret.add("Library/Managed/aREST/Ethernet");
        // AsciiMassage_servos.ino:75:37: error: no matching function for call to
        // 'AsciiMassagePacker::streamEmpty(const char [6])'
        ret.add("Library/Managed/AsciiMassage/AsciiMassage_servos");
        // \BH1750FVI_Simple.ino:33:10: error: 'class Serial_' has no member named
        // 'printf'
        ret.add("Library/Managed/BH1750FVI/BH1750FVI_Simple");
        // * This example not complete at all.
        ret.add("Library/Managed/Blinker/Blinker_AUTO/AUTO_MQTT");
        // 3: error: 'StaticJsonBuffer' was not declared in this scope
        ret.add("Library/Managed/Boodskap_Message_library/SimpleMessageUsage");
        //uses #include <ArduinoDebug.hpp>
        ret.add("Library/Managed/107-Arduino-BoostUnits/Basic");
     // needs web lib
        ret.add("Library/Managed/FreeRTOS/GoldilocksAnalogueTestSuite");
        // needs config
        ret.add("Library/Managed/Firmata/StandardFirmataWiFi");
        // needs web lib
        ret.add("Library/Managed/Firmata/StandardFirmataBLE");


        return ret;
    }

    /**
     * Give a list of boards pick the board that is best to test this code Boards in
     * the beginning of the array are prefered (first found ok algorithm)
     *
     * returns null if this code should not be tested return null if myBoards is
     * empty returns the best known boarddescriptor to run this example
     */
    public static MCUBoard pickBestBoard(Example example, MCUBoard myBoards[]) {
        String libName = example.calcLibName();
        String fqn = example.getFQN();
        if (myBoards.length == 0) {
            //No boards =>no match
            System.out.println("No boards to select from found for ");
            return null;
        }
        //examples using DHT_sensor_library libraries are not found as the include is
        // DHT.h
        if (!libName.equals("DHT_sensor_library") && fqn.contains("DHT")) {
            System.out.println("Ignore Lib as it is known to fail " + libName);
            return null;
        }
        if (!example.getRequiredBoardAttributes().worksOutOfTheBox) {
            System.out.println("Example is known to fail " + example.getFQN());
            return null;
        }

        // if example states which board it wants use that board
        if (example.getRequiredBoardAttributes().myCompatibleBoardIDs.size() >0) {
            Set<String> compatibleBoardName = example.getRequiredBoardAttributes().myCompatibleBoardIDs;
            for (MCUBoard curBoard : myBoards) {
                if (compatibleBoardName.contains( curBoard.getID())) {
                    return curBoard;
                }
            }
            System.out.println(
                    "Example " + example.getFQN() + " requires boards " + compatibleBoardName + " that is not listed");
            return null;
        }

        // if the boardname is in the libname or ino name pick this one
        for (MCUBoard curBoard : myBoards) {
            String curBoardName = curBoard.getName();
            List<String> curBoardExampleNames = getSlangNames(curBoardName);
            for (String curBoardExampleName : curBoardExampleNames) {
                if (libName.toLowerCase().contains(curBoardName) || fqn.toLowerCase().contains(curBoardExampleName)) {
                    if (example.worksOnBoard(curBoard)) {
                        return curBoard;
                    }
                }
            }
        }
        // If the architecture is in the libname or boardname pick this one
        for (MCUBoard curBoard : myBoards) {
            String curArchitectureName = curBoard.getBoardDescriptor().getArchitecture().toLowerCase();
            if (libName.toLowerCase().contains(curArchitectureName)
                    || fqn.toLowerCase().contains(curArchitectureName)) {
                if (example.worksOnBoard(curBoard)) {
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
            if (example.worksOnBoard(curBoard)) {
                return curBoard;
            }
        }
        System.out.println(
                "No board found for " + Integer.toString(++noBoardFoundCount) + " " + example.getCodeLocation().toOSString());
        return null;
    }

    private static List<String> getSlangNames(String curBoardName) {
        Map<String, String> singleNames = new HashMap<>();
        singleNames.put("adafruit_metro_m4", "metroM4");
        singleNames.put("feather52832", "feather");
        singleNames.put("trinket3", "trinket");
        singleNames.put("adafruit_feather_m0", "FeatherM0");
        singleNames.put("arduino_zero_edbg", "zero");
        singleNames.put("arduino_101", "101");
        singleNames.put("arduino_zero_native", "zero Native");
        singleNames.put("d1_mini", "wemos");
        singleNames.put("teensy36", "teensy3");
        singleNames.put("adafruit_metro_m4", "metroM4");

        List<String> ret = new ArrayList<>();
        String singleName = singleNames.get(curBoardName);
        if (singleName != null) {
            ret.add(singleName);
        }
        ret.add(curBoardName);
        return ret;
    }

    private static String getRequiredBoardID(String fqn) {
        switch (fqn) {
        case "Library/Managed/Accessory_Shield/OLED_example_Adafruit":
        case "Library/Managed/Accessory_Shield/temp_humidity_oled":
            return "uno";
        case "Library/Managed/Adafruit_Circuit_Playground/Infrared_Demos/Infrared_NeoPixel":
        case "Library/Managed/Adafruit_Circuit_Playground/Infrared_Demos/Infrared_Read":
        case "Library/Managed/Adafruit_Circuit_Playground/Infrared_Demos/Infrared_Record":
        case "Library/Managed/Adafruit_Circuit_Playground/Infrared_Demos/Infrared_Send":
        case "Library/Managed/Adafruit_Circuit_Playground/Infrared_Demos/Infrared_Testpattern":
        case "Library/Managed/Adafruit_Zero_FFT_Library/CircuitPlayground":
            return "adafruit_circuitplayground_m0";
        case "Library/Managed/Adafruit_MiniMLX90614/templight":
            return "gemma";
        case "Library/Managed/ArduinoThread/SensorThread":
            return "due";
        case "Library/Managed/AudioFrequencyMeter/SimpleAudioFrequencyMeter":
        case "Library/Managed/Adafruit_NeoPXL8/strandtest":
            return "zero";
        case "Library/Managed/BLEPeripheral/iBeacon":
            return "feather52";
        default:
            return null;
        }

    }

	public boolean worksOnBoard(MCUBoard board) {
	        if (board.getBoardDescriptor() == null) {
	            return false;
	        }
	        /*
	         * There is one know Teensy example that does not
	         * run on all teensy boards
	         */
	        if ("Teensy".equalsIgnoreCase(getID())) {
	            if (getFQN().contains("Teensy/USB_Mouse/Buttons")) {
	                String boardID = board.getBoardDescriptor().getBoardID();
	                if ("teensypp2".equals(boardID) || "teensy2".equals(boardID)) {
	                    return false;
	                }
	            }
	        }
	        /*
	         * the servo lib does not work on gemma
	         */
	        if ("Servo".equalsIgnoreCase(calcLibName())) {
	                if ("gemma".equals( board.getBoardDescriptor().getBoardID()) ) {
	                    return false;
	            }
	        }

	        return getRequiredBoardAttributes().compatibleWithBoardAttributes(board.myAttributes);
	}
}
