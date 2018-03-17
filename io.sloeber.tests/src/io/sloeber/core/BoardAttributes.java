package io.sloeber.core;

public class BoardAttributes {
	public boolean serial = true;
	public boolean serial1 = false;
	public boolean keyboard = false;
	public boolean flightSim = false;
	public boolean joyStick = false;
	public boolean midi = false;
	public boolean mouse = false;
	public boolean wire1 = false;
	/*
	 * Only a very rara selection of boards supports input_pulldown as pin mode
	 */
	public boolean inputPullDown = false;
	public boolean teensy = false;// Teensy specific hardware or software
	/*
	 * No board is out of the box compatible with code that needs a change
	 */
	public boolean worksOutOfTheBox = false;
	/*
	 * Composite video output from M0 microcontrollers: Circuit Playground Express (not 'classic'), Feather M0, Arduino Zero
	 */
	public boolean mo_mcu = false;
	public boolean esp8266_mcu=false;
	public String boardID=null;


	public boolean compatibleWithExampleRequirements(BoardAttributes example) {
		boolean ret = !worksOutOfTheBox;
		ret = ret && matches(example.serial, serial);
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
		if (example.boardID!=null) {
			ret=ret&&example.boardID.equals(boardID);
		}

		return ret;

	}

	private static boolean matches(boolean needs, boolean has) {
		if (!needs)
			return true;
		return has;
	}

	/*
	 * This method adds two attributes together.
	 * The result is more restrictive than the 2 inputs
	 */
	public BoardAttributes and(BoardAttributes or) {
		BoardAttributes ret = new BoardAttributes();
		ret.serial = serial || or.serial;
		ret.serial1 = serial1 || or.serial1;
		ret.keyboard = keyboard || or.keyboard;
		ret.flightSim = flightSim || or.flightSim;
		ret.joyStick = joyStick || or.joyStick;
		ret.midi = midi || or.midi;
		ret.mouse = mouse || or.mouse;
		ret.wire1 = wire1 || or.wire1;
		ret.inputPullDown = inputPullDown || or.inputPullDown;
		ret.teensy = teensy || or.teensy;
		ret.worksOutOfTheBox=worksOutOfTheBox||or.worksOutOfTheBox;
		ret.mo_mcu=mo_mcu||or.mo_mcu;
		ret.esp8266_mcu=esp8266_mcu||or.esp8266_mcu;
		if (boardID==null) {
			ret.boardID=or.boardID;
		}else {
			if (or.boardID==null) {
				ret.boardID=boardID;
			}else {
				if(or.boardID.equals(boardID)) {
					ret.boardID=boardID;
				}else {
					ret.worksOutOfTheBox=true;
				}
			}
		}

		return ret;
	}
}
