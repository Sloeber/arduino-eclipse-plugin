package io.sloeber.providers;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.sloeber.core.AttributesCode;
import io.sloeber.core.Example;
import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class Teensy extends MCUBoard {

    public final static String Teensy3_0_ID = "teensy30";
    public final static String Teensy3_1_ID = "teensy31";
    public final static String Teensy_PP2_ID = "teensypp2";
    public final static String Teensy_2_ID = "teensy2";
    public final static String Teensy3_5_ID = "teensy35";
    public final static String Teensy3_6_ID = "teensy36";
    public final static String Teensy_LC_ID = "teensyLC";
    public final static String TEENSY_PLATFORM_ID = "avr";
    public final static String TEENSY_PROVIDER= "teensy";
	private static final String jsonFileName = "package_teensy_index.json";
	public static final String packageURL = "https://www.pjrc.com/teensy/package_teensy_index.json";

    public static MCUBoard Teensy_LC() {
        return new Teensy(Teensy_LC_ID);
    }

    public static MCUBoard Teensy3_5() {
        return new Teensy(Teensy3_5_ID);
    }

    public static MCUBoard Teensy3_6() {
        return new Teensy(Teensy3_6_ID);
    }

    public static MCUBoard Teensy3_1() {
        return new Teensy(Teensy3_1_ID);
    }

    public static MCUBoard Teensy3_0() {
        return new Teensy(Teensy3_0_ID);
    }

    public static MCUBoard teensypp2() {
        return new Teensy(Teensy_PP2_ID);
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
        default:
            break;
        }
        myBoardDescriptor = BoardsManager.getBoardDescription(jsonFileName, TEENSY_PROVIDER, TEENSY_PLATFORM_ID,boardName,options);

        setUploadPort("none");
        setAttributes();
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
    public Map<String, String> getBoardOptions(Example example) {
        Map<String, String> ret = super.getBoardOptions(example);
        switch (myBoardDescriptor.getBoardID()) {
        case Teensy3_5_ID:
        case Teensy3_6_ID:
            ret.put("usb", "everything");
            break;
        default:
            AttributesCode attribs = example.getRequiredBoardAttributes();
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


    @Override
    public MCUBoard createMCUBoard(BoardDescription boardDescriptor) {
        return new Teensy(boardDescriptor);

    }

    public Teensy(BoardDescription boardDesc) {
        myBoardDescriptor = boardDesc;
        myBoardDescriptor.setUploadPort("none");
        setAttributes();

    }

    @Override
    protected void setAttributes() {
        myAttributes.flightSim = true;
        myAttributes.joyStick = true;
        myAttributes.keyboard = true;
        myAttributes.midi = true;
        myAttributes.mouse = true;
        myAttributes.serial = true;
        myAttributes.serial1 = true;
        myAttributes.wire1 = true;
        myAttributes.myArchitecture=myBoardDescriptor.getArchitecture();
    }

	public static void installLatest() {
	        BoardsManager.installLatestPlatform(jsonFileName, TEENSY_PROVIDER, TEENSY_PLATFORM_ID);
	}

	public static List<MCUBoard> getAllBoards() {
		return getAllBoards(TEENSY_PROVIDER, Teensy3_0());
	}
}