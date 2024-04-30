package io.sloeber.core;

import java.util.HashSet;
import java.util.Set;

public class AttributesCode extends Attributes{
    public boolean worksOutOfTheBox = true;
    public final Set<String> myArchitectures = new HashSet<>();
    public final Set<String> myCompatibleBoardIDs = new HashSet<>();

    public AttributesCode() {
         serial = false;
         serial1 = false;
         serialUSB = false;
         keyboard = false;
         flightSim = false;
         joyStick = false;
         midi = false;
         mouse = false;
         wire1 = false;
         rawHID = false;
         buildInLed = false;
         tone = false;
         myNumAD = 0;
         directMode = false;
         inputPullDown = false;
    }

    public boolean compatibleWithBoardAttributes(AttributesBoard board) {
        boolean ret = worksOutOfTheBox;
        ret = ret && matches(board.serial, serial);
        ret = ret && matches(board.serial1, serial1);
        ret = ret && matches(board.serialUSB, serialUSB);
        ret = ret && matches(board.rawHID, rawHID);
        ret = ret && matches(board.keyboard, keyboard);
        ret = ret && matches(board.flightSim, flightSim);
        ret = ret && matches(board.joyStick, joyStick);
        ret = ret && matches(board.midi, midi);
        ret = ret && matches(board.mouse, mouse);
        ret = ret && matches(board.wire1, wire1);
        //        ret = ret && matches(example.teensy, teensy);
        ret = ret && matches(board.inputPullDown, inputPullDown);
        //        ret = ret && matches(example.mo_mcu, mo_mcu);
        //        ret = ret && matches(example.esp8266_mcu, esp8266_mcu);
        ret = ret && matches(board.buildInLed, buildInLed);
        ret = ret && matches(board.tone, tone);
        ret = ret && matches(board.directMode, directMode);

        if(myArchitectures.size()>0) {
        ret = ret &&  myArchitectures.contains(board.myArchitecture);
        }

        ret = ret && board.myNumAD >= myNumAD;

        if (myCompatibleBoardIDs.size()>0) {
            ret = ret && myCompatibleBoardIDs.contains( board.boardID);
        }

        return ret;

    }


    private static boolean matches(boolean has , boolean needs) {
        if (!needs)
            return true;
        return has;
    }

    /*
     * This method adds two attributes together. The result is more restrictive than
     * the 2 inputs
     */
    public AttributesCode or(AttributesCode or) {
        AttributesCode ret = new AttributesCode();
        ret.myArchitectures.addAll(myArchitectures);
        ret.myArchitectures.retainAll(or.myArchitectures);

        ret.myCompatibleBoardIDs.addAll(myCompatibleBoardIDs);
        ret.myCompatibleBoardIDs.retainAll(or.myCompatibleBoardIDs);

        ret.worksOutOfTheBox = worksOutOfTheBox && or.worksOutOfTheBox;
        ret.buildInLed = buildInLed || or.buildInLed;
        ret.tone = tone || or.tone;
        ret.directMode = directMode || or.directMode;
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

        return ret;
    }
}
