package io.sloeber.core;

import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("nls")
public class AttributesCode extends Attributes {
	public boolean worksOutOfTheBox = true;
	public final Set<String> myCompatibleArchitectures = new HashSet<>();
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
		digitalPinToPCICR=false;
		RX_TX_PIN=false;
		SD=false;
	}

	public AttributesCode(String fqn) {
		this();
		if (fqn.contains("ChipKIT")) {
			myCompatibleArchitectures.add("pic32");
		}
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
		// ret = ret && matches(example.teensy, teensy);
		ret = ret && matches(board.inputPullDown, inputPullDown);
		// ret = ret && matches(example.mo_mcu, mo_mcu);
		// ret = ret && matches(example.esp8266_mcu, esp8266_mcu);
		ret = ret && matches(board.buildInLed, buildInLed);
		ret = ret && matches(board.tone, tone);
		ret = ret && matches(board.directMode, directMode);
		ret = ret && matches(board.digitalPinToPCICR, digitalPinToPCICR);
		ret = ret && matches(board.USBCON, USBCON);
		ret = ret && matches(board.RX_TX_PIN, RX_TX_PIN);
		ret = ret && matches(board.SD, SD);

		if (myCompatibleArchitectures.size() > 0) {
			ret = ret && myCompatibleArchitectures.contains(board.myArchitecture);
		}

		ret = ret && board.myNumAD >= myNumAD;

		if (myCompatibleBoardIDs.size() > 0) {
			ret = ret && myCompatibleBoardIDs.contains(board.boardID);
		}

		return ret;

	}

	private static boolean matches(boolean has, boolean needs) {
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
		if (myCompatibleArchitectures.size() > 0 && or.myCompatibleArchitectures.size() > 0) {
			ret.myCompatibleArchitectures.addAll(myCompatibleArchitectures);
			ret.myCompatibleArchitectures.retainAll(or.myCompatibleArchitectures);
		} else {
			ret.myCompatibleArchitectures.addAll(myCompatibleArchitectures);
			ret.myCompatibleArchitectures.addAll(or.myCompatibleArchitectures);
		}

		if (myCompatibleBoardIDs.size() > 0 && or.myCompatibleBoardIDs.size() > 0) {
			ret.myCompatibleBoardIDs.addAll(myCompatibleBoardIDs);
			ret.myCompatibleBoardIDs.retainAll(or.myCompatibleBoardIDs);
		} else {
			ret.myCompatibleBoardIDs.addAll(myCompatibleBoardIDs);
			ret.myCompatibleBoardIDs.addAll(or.myCompatibleBoardIDs);
		}

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
		ret.digitalPinToPCICR = digitalPinToPCICR || or.digitalPinToPCICR;
		ret.USBCON = USBCON || or.USBCON;
		ret.RX_TX_PIN = RX_TX_PIN || or.RX_TX_PIN;
		ret.SD = SD || or.SD;

		return ret;
	}
}
