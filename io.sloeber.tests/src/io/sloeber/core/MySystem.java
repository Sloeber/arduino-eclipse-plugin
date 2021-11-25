package io.sloeber.core;

import static org.junit.Assert.*;

import io.sloeber.providers.Arduino;
import io.sloeber.providers.ESP8266;
import io.sloeber.providers.MCUBoard;
import io.sloeber.providers.Teensy;

/*
 * Set system specific info here.
 * For now this is only the teensy installlocation
 */
@SuppressWarnings("nls")
public class MySystem {
    private static final String jantjesWindowsMachine = "1248215851";
    //the one below is based on one mac address Fysiek adres (MAC):	C0-3F-D5-66-04-58 
    private static final String jantjesLinuxMachine = "88937904";
    private static final String currentMachine = jantjesWindowsMachine;

    public static String getTeensyPlatform() {
        switch (currentMachine) {
        case jantjesWindowsMachine:
            return "E:\\arduino\\arduino-1.8.12 - teensy\\hardware\\teensy";
        case jantjesLinuxMachine:
            return "/home/jan/tensyduino/arduino-1.8.12/hardware/teensy";
        default:
            return new String();
        }
    }

    public static String getTeensyBoard_txt() {
        return getTeensyPlatform() + "/avr/boards.txt";
    }

    public static MCUBoard[] getUploadBoards() {
        switch (currentMachine) {
        case jantjesLinuxMachine: {
            MCUBoard[] boards = { Teensy.teensypp2().setUploadPort("/dev/ttyACM0"),
                    Arduino.leonardo().setUploadPort("/dev/ttyS0"), //werkt niet
                    Arduino.fried2016().setUploadPort("/dev/ttyS0"), //werkt niet
                    Arduino.zeroNatviePort().setUploadPort("/dev/ttyS0"), //werkt niet
                    Arduino.yun().setUploadPort("COM20"), ESP8266.wemosD1().setUploadPort("/dev/ttyUSB0"),
                    Arduino.arduino_101().setUploadPort("COM15"), Arduino.zeroProgrammingPort().setUploadPort("COM14"),
                    Arduino.getMega2560Board().setUploadPort("COM11"), Arduino.dueprogramming().setUploadPort("COM8"),
                    Arduino.uno().setUploadPort("COM6"), };
            return boards;
        }
        case jantjesWindowsMachine:
            //due native upload gives to mutch trouble even in arduino IDE
            MCUBoard[] boards = { Teensy.teensypp2().setUploadPort("COM103"),
                    //	Teensy.Teensy3_1("COM24"), 
                    Arduino.leonardo().setUploadPort("COM101"), Arduino.fried2016().setUploadPort("COM102"),
                    Arduino.zeroNatviePort().setUploadPort("COM104"), //boardSponsor
                    Arduino.yun().setUploadPort("COM106"), ESP8266.wemosD1().setUploadPort("COM108"),
                    Arduino.arduino_101().setUploadPort("COM110"),
                    Arduino.zeroProgrammingPort().setUploadPort("COM111"),
                    Arduino.getMega2560Board().setUploadPort("COM112"),
                    Arduino.dueprogramming().setUploadPort("COM124"), Arduino.uno().setUploadPort("COM126"), };
            return boards;
        default:
            fail("Boards for the system with haskey " + currentMachine + " are not found");
            return null;
        }
    }
}
