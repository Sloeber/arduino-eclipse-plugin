package io.sloeber.core;

import static org.junit.Assert.*;
import static io.sloeber.core.api.Const.*;

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
    private static final String jantjesLinuxMachine = "88937904";
    private static final String currentMachine = getMachine();

    private static String getMachine() {

        if ("jan".equals(System.getProperty("user.name"))) {
            if (isWindows) {
                return jantjesWindowsMachine;
            }
            return jantjesLinuxMachine;
        }
        return new String();
    }



    public static MCUBoard[] getUploadBoards() {
        switch (currentMachine) {
        case jantjesLinuxMachine: {
            /* using this udev file thins work for most but not all boards (fried 101 )
KERNEL=="ttyACM*", ACTION=="add", ATTRS{idVendor}=="2341", ATTRS{idProduct}=="003d", SYMLINK+="s_DueProg"
KERNEL=="ttyACM*", ACTION=="add", ATTRS{idVendor}=="2341", ATTRS{idProduct}=="0041", SYMLINK+="s_Yun"
KERNEL=="ttyACM*", ACTION=="add", ATTRS{idVendor}=="2341", ATTRS{idProduct}=="8041", SYMLINK+="s_Yun"
KERNEL=="ttyACM*", ACTION=="add", ATTRS{idVendor}=="2341", ATTRS{idProduct}=="0043", SYMLINK+="s_Uno"
KERNEL=="ttyACM*", ACTION=="add", ATTRS{idVendor}=="2341", ATTRS{idProduct}=="0042", SYMLINK+="s_Mega2560"
KERNEL=="ttyACM*", ACTION=="add", ATTRS{idVendor}=="2341", ATTRS{idProduct}=="0010", SYMLINK+="s_Mega2560"
KERNEL=="ttyACM*", ACTION=="add", ATTRS{idVendor}=="2341", ATTRS{idProduct}=="8036", SYMLINK+="s_Leonardo"
KERNEL=="ttyACM*", ACTION=="add", ATTRS{idVendor}=="2341", ATTRS{idProduct}=="0036", SYMLINK+="s_Leonardo"
KERNEL=="ttyACM*", ACTION=="add", ATTRS{idVendor}=="1B4F", ATTRS{idProduct}=="9207", SYMLINK+="s_Fried"
KERNEL=="ttyACM*", ACTION=="add", ATTRS{idVendor}=="1B4F", ATTRS{idProduct}=="9208", SYMLINK+="s_Fried"
KERNEL=="ttyACM*", ACTION=="add", ATTRS{idVendor}=="03eb", ATTRS{idProduct}=="2157", SYMLINK+="s_ZeroProg"
KERNEL=="ttyACM*", ACTION=="add", ATTRS{idVendor}=="2341", ATTRS{idProduct}=="804d", SYMLINK+="s_ZeroNa"
KERNEL=="ttyACM*", ACTION=="add", ATTRS{idVendor}=="2341", ATTRS{idProduct}=="004d", SYMLINK+="s_ZeroNa"
KERNEL=="ttyACM*", ACTION=="add", ATTRS{idVendor}=="16c0", ATTRS{idProduct}=="0487", SYMLINK+="s_teensy"

KERNEL=="ttyUSB*", ACTION=="add", ATTRS{idVendor}=="1a86", ATTRS{idProduct}=="7523", SYMLINK+="s_NanoFakeChina"


             */
            MCUBoard[] boards = { Teensy.teensypp2().setUploadPort("/dev/s_teensy"),
                    Arduino.leonardo().setUploadPort("/dev/s_Leonardo"),
                    Arduino.fried2016().setUploadPort("/dev/s_Fried"), //werkt niet
                    Arduino.zeroNatviePort().setUploadPort("/dev/s_ZeroNa"),
                    Arduino.yun().setUploadPort("/dev/s_Yun"), ESP8266.wemosD1().setUploadPort("/dev/ttyUSB0"),
                    Arduino.arduino_101().setUploadPort("/dev/s_Arduino_101"),
                    Arduino.zeroProgrammingPort().setUploadPort("/dev/s_ZeroProg"),
                    Arduino.mega2560Board().setUploadPort("/dev/s_Mega2560"),
                    Arduino.dueprogramming().setUploadPort("/dev/s_DueProg"),
                    Arduino.uno().setUploadPort("/dev/s_Uno"), };
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
