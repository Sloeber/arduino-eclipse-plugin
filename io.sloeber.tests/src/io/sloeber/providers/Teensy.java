package io.sloeber.providers;

import static org.junit.Assert.fail;

import java.util.Map;
import java.util.TreeMap;

import io.sloeber.core.BoardAttributes;
import io.sloeber.core.Examples;
import io.sloeber.core.MySystem;
import io.sloeber.core.api.PackageManager;

@SuppressWarnings("nls")
public class Teensy extends MCUBoard {

	public final static String Teensy3_0_ID = "teensy30";
	public final static String Teensy3_1_ID = "teensy31";
	public final static String Teensy_PP2_ID = "teensypp2";
	public final static String Teensy_2_ID = "teensy2";
	public final static String Teensy3_5_ID = "teensy35";
	public final static String Teensy3_6_ID = "teensy36";
	public final static String Teensy_LC_ID = "teensyLC";

	public static MCUBoard Teensy_LC() {
		return new Teensy(Teensy_LC_ID);
	}

	public static MCUBoard Teensy3_5() {
		return new Teensy(Teensy3_5_ID);
	}

	public static MCUBoard Teensy3_6() {
		MCUBoard board =  new Teensy(Teensy3_6_ID);
		board.mySlangName="teensy3";
		return board;
	}

	public static MCUBoard Teensy3_1() {
		return new Teensy(Teensy3_1_ID);
	}
	public static MCUBoard Teensy3_1(String uploadPort) {
		MCUBoard board = Teensy3_1();
		board.myBoardDescriptor.setUploadPort(uploadPort);
		return board;
	}

	public static MCUBoard Teensy3_0() {
		return new Teensy(Teensy3_0_ID);
	}

	public static MCUBoard teensypp2() {
		return new Teensy(Teensy_PP2_ID);
	}
	public static MCUBoard teensypp2(String uploadPort) {
		MCUBoard board = teensypp2();
		board.myBoardDescriptor.setUploadPort(uploadPort);
		return board;
	}

	public static MCUBoard teensy2() {
		return new Teensy(Teensy_2_ID);
	}

	private Teensy(String boardName) {
		Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		options.put("usb", "serialhid");
		options.put("speed", "48");
		options.put("opt", "o2std");
		options.put("keys", "en-us");
		switch (boardName) {
		case Teensy_PP2_ID:
			options.put("speed", "8");
			break;
		case Teensy_2_ID:
			options.put("speed", "8");
			break;
		case Teensy3_1_ID:
			options.put("speed", "72");
			break;
		}

		this.myBoardDescriptor = PackageManager.getBoardDescriptor("local", MySystem.getTeensyBoard_txt(), "ignored",
				boardName, options);
		if (this.myBoardDescriptor == null) {
			fail(boardName + " Board not found");
		}
		this.myBoardDescriptor.setUploadPort("none");

		myAttributes.serial = true;
		myAttributes.serial1 = true;
		myAttributes.keyboard = true;
		myAttributes.joyStick = true;
		myAttributes.mouse = true;
		myAttributes.flightSim = true;
		myAttributes.midi = true;
		myAttributes.wire1 = true;
		myAttributes.teensy = true;
	}



	/*
	 * For teensy any menu option is ok except for menu.usb There the main options
	 * are: serial keyboard touch hidtouch hid serialhid midi serialmidi audio
	 * serialmidiaudio rawhid flightsim flightsimjoystick everything disable
	 * unfortunately not all boards support the everything option
	 *
	 * @see
	 * io.sloeber.core.boards.MCUBoard#getBoardOptions(io.sloeber.core.Examples)
	 */
	@Override
	public Map<String, String> getBoardOptions(Examples example) {
		Map<String, String> ret = super.getBoardOptions(example);
		switch (myBoardDescriptor.getBoardID()) {
		case Teensy3_5_ID:
		case Teensy3_6_ID:
			ret.put("usb", "everything");
			break;
		default:
			BoardAttributes attribs = example.getRequiredBoardAttributes();
			if (attribs.flightSim) {
				ret.put("usb", "flightsim");
			}
			if (attribs.mouse || attribs.keyboard || attribs.serial || attribs.joyStick) {
				ret.put("usb", "serialhid");
			}
			if (attribs.midi) {
				ret.put("usb", "serialmidiaudio");
			}
	         if (attribs.rawHID) {
	                ret.put("usb", "rawhid");
	            }
		}
		return ret;
	}
	
	public static MCUBoard[] getAllBoards() {
		// hardcode this stuff now because I want to release 4.3.1
		// shoulds be something like
		// return
		// PackageManager.getAllBoardDescriptors(getJsonFileName(),getPackageName(),getPlatformName()
		// , options);
		MCUBoard[] boards = { Teensy.Teensy3_6(), Teensy.Teensy3_5(), Teensy.Teensy3_1(), Teensy.Teensy3_0(),
				Teensy.Teensy_LC(), Teensy.teensypp2(), Teensy.teensy2() };
		return boards;


    }
}