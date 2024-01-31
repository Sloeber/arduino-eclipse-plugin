package io.sloeber.core;

import java.util.HashSet;
import java.util.Set;

public class BoardAttributes {
    public boolean serial = false;
    public boolean serial1 = false;
    public boolean serialUSB = false;
    public boolean keyboard = false;
    public boolean flightSim = false;
    public boolean joyStick = false;
    public boolean midi = false;
    public boolean mouse = false;
    public boolean wire1 = false;
    public boolean rawHID = false;
    public boolean buildInLed = true;
    public boolean tone = true;
    //the number of ADC ports needed/available
    //default is ridiculous high so boards should 
    // 1)default to test 
    // 2 fail if insufficient ADC's are available
    public int myNumAD = 20;
    //directmode is something from the capacitiveSensor library
    //but as the arduino examples contain examples using this libraries
    //and mbed boards do not support it I added it as a boardAttribute
    public boolean directMode = true;

    /*
     * Only a very rare selection of boards supports input_pulldown as pin mode
     */
    public boolean inputPullDown = false;
    /*
     * No board is out of the box compatible with code that needs a change
     */
    public boolean worksOutOfTheBox = true;
    public String boardID = null;
    public final Set<String> myArchitectures = new HashSet<>(); //Bit wierd to have multiple architectures I know

    public boolean compatibleWithExampleRequirements(BoardAttributes example) {
        boolean ret = worksOutOfTheBox;
        ret = ret && matches(example.serial, serial);
        ret = ret && matches(example.serial1, serial1);
        ret = ret && matches(example.serialUSB, serialUSB);
        ret = ret && matches(example.rawHID, rawHID);
        ret = ret && matches(example.keyboard, keyboard);
        ret = ret && matches(example.flightSim, flightSim);
        ret = ret && matches(example.joyStick, joyStick);
        ret = ret && matches(example.midi, midi);
        ret = ret && matches(example.mouse, mouse);
        ret = ret && matches(example.wire1, wire1);
        //        ret = ret && matches(example.teensy, teensy);
        ret = ret && matches(example.inputPullDown, inputPullDown);
        //        ret = ret && matches(example.mo_mcu, mo_mcu);
        //        ret = ret && matches(example.esp8266_mcu, esp8266_mcu);
        ret = ret && matches(example.buildInLed, buildInLed);
        ret = ret && matches(example.tone, tone);
        ret = ret && matches(example.directMode, directMode);

        ret = ret && matches(example.myArchitectures, myArchitectures);

        ret = ret && example.myNumAD <= myNumAD;

        if (example.boardID != null) {
            ret = ret && example.boardID.equals(boardID);
        }

        return ret;

    }

    private static boolean matches(Set<String> example_Archs, Set<String> board_archs) {
        if (example_Archs.size() == 0) {
            //no requirements for example
            return true;
        }
        Set<String> result = new HashSet<>(example_Archs);

        result.retainAll(board_archs);

        return result.size() > 0;
    }

    private static boolean matches(boolean needs, boolean has) {
        if (!needs)
            return true;
        return has;
    }

    /*
     * This method adds two attributes together. The result is more restrictive than
     * the 2 inputs
     */
    public BoardAttributes or(BoardAttributes or) {
        BoardAttributes ret = new BoardAttributes();
        ret.myArchitectures.addAll(myArchitectures);
        ret.myArchitectures.addAll(or.myArchitectures);
        // fields that need a binary and
        ret.worksOutOfTheBox = worksOutOfTheBox && or.worksOutOfTheBox;
        ret.buildInLed = buildInLed && or.buildInLed;
        ret.tone = tone && or.tone;
        ret.directMode = directMode && or.directMode;
        // fields that can do with or
        ret.serial = serial || or.serial;
        ret.serial1 = serial1 || or.serial1;
        ret.serialUSB = serialUSB || or.serialUSB;
        ret.rawHID = rawHID || or.rawHID;
        ret.keyboard = keyboard || or.keyboard;
        ret.flightSim = flightSim || or.flightSim;
        ret.joyStick = joyStick || or.joyStick;
        ret.midi = midi || or.midi;
        ret.mouse = mouse || or.mouse;
        ret.wire1 = wire1 || or.wire1;
        ret.inputPullDown = inputPullDown || or.inputPullDown;
        //        ret.teensy = teensy || or.teensy;
        //        ret.mo_mcu = mo_mcu || or.mo_mcu;
        //        ret.esp8266_mcu = esp8266_mcu || or.esp8266_mcu;

        // other special fields
        if (boardID == null) {
            ret.boardID = or.boardID;
        } else {
            if (or.boardID == null) {
                ret.boardID = boardID;
            } else {
                if (or.boardID.equals(boardID)) {
                    ret.boardID = boardID;
                } else {
                    ret.worksOutOfTheBox = false;
                }
            }
        }

        return ret;
    }
}
