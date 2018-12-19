package io.sloeber.core;

public class BoardAttributes {
    public boolean serial = false;
    public boolean serial1 = false;
    public boolean keyboard = false;
    public boolean flightSim = false;
    public boolean joyStick = false;
    public boolean midi = false;
    public boolean mouse = false;
    public boolean wire1 = false;
    public boolean rawHID = false;
    /*
     * Only a very rara selection of boards supports input_pulldown as pin mode
     */
    public boolean inputPullDown = false;
    public boolean teensy = false;// Teensy specific hardware or software
    /*
     * No board is out of the box compatible with code that needs a change
     */
    public boolean worksOutOfTheBox = true;
    /*
     * Composite video output from M0 microcontrollers: Circuit Playground Express
     * (not 'classic'), Feather M0, Arduino Zero
     */
    public boolean mo_mcu = false;
    public boolean esp8266_mcu = false;
    public String boardName = null;

    public boolean compatibleWithExampleRequirements(BoardAttributes example) {
        boolean ret = worksOutOfTheBox;
        ret = ret && matches(example.serial, serial);
        ret = ret && matches(example.rawHID, rawHID);
        ret = ret && matches(example.serial1, serial1);
        ret = ret && matches(example.keyboard, keyboard);
        ret = ret && matches(example.flightSim, flightSim);
        ret = ret && matches(example.joyStick, joyStick);
        ret = ret && matches(example.midi, midi);
        ret = ret && matches(example.mouse, mouse);
        ret = ret && matches(example.wire1, wire1);
        ret = ret && matches(example.teensy, teensy);
        ret = ret && matches(example.inputPullDown, inputPullDown);
        ret = ret && matches(example.mo_mcu, mo_mcu);
        ret = ret && matches(example.esp8266_mcu, esp8266_mcu);
        if (example.boardName != null) {
            ret = ret && example.boardName.equals(boardName);
        }

        return ret;

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
        // fields that need a binary and
        ret.worksOutOfTheBox = worksOutOfTheBox && or.worksOutOfTheBox;
        // fields that can do with or
        ret.serial = serial || or.serial;
        ret.rawHID = rawHID || or.rawHID;
        ret.serial1 = serial1 || or.serial1;
        ret.keyboard = keyboard || or.keyboard;
        ret.flightSim = flightSim || or.flightSim;
        ret.joyStick = joyStick || or.joyStick;
        ret.midi = midi || or.midi;
        ret.mouse = mouse || or.mouse;
        ret.wire1 = wire1 || or.wire1;
        ret.inputPullDown = inputPullDown || or.inputPullDown;
        ret.teensy = teensy || or.teensy;
        ret.mo_mcu = mo_mcu || or.mo_mcu;
        ret.esp8266_mcu = esp8266_mcu || or.esp8266_mcu;
        // other special fields
        if (boardName == null) {
            ret.boardName = or.boardName;
        } else {
            if (or.boardName == null) {
                ret.boardName = boardName;
            } else {
                if (or.boardName.equals(boardName)) {
                    ret.boardName = boardName;
                } else {
                    ret.worksOutOfTheBox = false;
                }
            }
        }

        return ret;
    }
}
