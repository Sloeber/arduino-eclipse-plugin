package io.sloeber.core;

import static org.junit.Assert.*;

import io.sloeber.core.common.Common;
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
    private static final String currentMachine = getMachine();

    private static String getMachine() {

        if ("jan".equals(System.getProperty("user.name"))) {
            if (Common.isWindows) {
                return jantjesWindowsMachine;
            }
            return jantjesLinuxMachine;
        }
        return new String();
    }

    public static String getTeensyPlatform() {
        switch (currentMachine) {
        case jantjesWindowsMachine:
            return "E:\\arduino\\arduino-1.8.12 - teensy\\hardware\\teensy";
        case jantjesLinuxMachine:
            return "/home/jan/teensyduino/arduino-1.8.12/hardware/teensy";
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
            /* using this udev file thins work for most but not all boards (fried 101 )
              KERNEL=="ttyACM*", ATTRS{idVendor}=="2341", ATTRS{idProduct}=="003d", SYMLINK+="ttyDueProg"
            KERNEL=="ttyACM*", ATTRS{idVendor}=="2341", ATTRS{idProduct}=="0041", SYMLINK+="ttyYun"
            KERNEL=="ttyACM*", ATTRS{idVendor}=="2341", ATTRS{idProduct}=="8041", SYMLINK+="ttyYun"
            KERNEL=="ttyACM*", ATTRS{idVendor}=="2341", ATTRS{idProduct}=="0043", SYMLINK+="ttyUno"
            KERNEL=="ttyACM*", ATTRS{idVendor}=="2341", ATTRS{idProduct}=="0042", SYMLINK+="ttyMega2560"
            KERNEL=="ttyACM*", ATTRS{idVendor}=="2341", ATTRS{idProduct}=="0010", SYMLINK+="ttyMega2560"
            KERNEL=="ttyACM*", ATTRS{idVendor}=="2341", ATTRS{idProduct}=="8036", SYMLINK+="ttyLeonardo"
            KERNEL=="ttyACM*", ATTRS{idVendor}=="2341", ATTRS{idProduct}=="0036", SYMLINK+="ttyLeonardo"
            KERNEL=="ttyACM*", ATTRS{idVendor}=="1B4F", ATTRS{idProduct}=="9207", SYMLINK+="ttyFried"
            KERNEL=="ttyACM*", ATTRS{idVendor}=="1B4F", ATTRS{idProduct}=="9208", SYMLINK+="ttyFried"
            KERNEL=="ttyACM*", ATTRS{idVendor}=="03eb", ATTRS{idProduct}=="2157", SYMLINK+="ttyZeroProg"
            KERNEL=="ttyACM*", ATTRS{idVendor}=="2341", ATTRS{idProduct}=="804d", SYMLINK+="ttyZeroNa"
            KERNEL=="ttyACM*", ATTRS{idVendor}=="2341", ATTRS{idProduct}=="004d", SYMLINK+="ttyZeroNa"
            
             */
            MCUBoard[] boards = { Teensy.teensypp2().setUploadPort("/dev/ttyACM0"),
                    Arduino.leonardo().setUploadPort("/dev/ttyLeonardo"), //werkt niet
                    Arduino.fried2016().setUploadPort("/dev/ttyFried"), //werkt niet
                    Arduino.zeroNatviePort().setUploadPort("/dev/ttyZeroNa"), //werkt niet
                    Arduino.yun().setUploadPort("/dev/ttyYun"), ESP8266.wemosD1().setUploadPort("/dev/ttyUSB0"),
                    Arduino.arduino_101().setUploadPort("/dev/ttyArduino_101"),
                    Arduino.zeroProgrammingPort().setUploadPort("/dev/ttyZeroProg"),
                    Arduino.mega2560Board().setUploadPort("/dev/ttyMega2560"),
                    Arduino.dueprogramming().setUploadPort("/dev/ttyDueProg"),
                    Arduino.uno().setUploadPort("/dev/ttyUno"), };
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
                    Arduino.mega2560Board().setUploadPort("COM112"), Arduino.dueprogramming().setUploadPort("COM124"),
                    Arduino.uno().setUploadPort("COM126"), };
            return boards;
        default:
            fail("Boards for the system with haskey " + currentMachine + " are not found");
            return null;
        }
    }
}
