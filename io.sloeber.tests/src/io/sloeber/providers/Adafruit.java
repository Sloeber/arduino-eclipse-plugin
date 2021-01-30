package io.sloeber.providers;

import static org.junit.Assert.*;

import io.sloeber.core.api.PackageManager;
@SuppressWarnings("nls")
public class Adafruit extends MCUBoard {
	public final static String packageURL = "https://adafruit.github.io/arduino-board-index/package_adafruit_index.json";
    private static final String AVRArchitectureName = "avr";
    private static final String SAMDArchitectureName = "samd";
	//private static final String SAMPlatformName = "Arduino SAM Boards (32-bits ARM Cortex-M3)";
    private static final String NFR52ArchitectureName = "nRF52";
	//private static final String XICEDPlatformName = "Adafruit WICED";
	public static final String metroM4SlangName="metroM4";
	public static final String metroM4ID="adafruit_metro_m4";



	public  Adafruit( String architectureName, String boardName) {

		this.myBoardDescriptor = PackageManager.getBoardDescriptor( "package_adafruit_index.json","adafruit",architectureName ,
				boardName, null);
		if (this.myBoardDescriptor == null) {
			fail(boardName + " Board not found");
		}
		this.myBoardDescriptor.setUploadPort("none");
	}


	public static MCUBoard feather() {
		MCUBoard ret= new Adafruit(NFR52ArchitectureName,"feather52832");
		ret.mySlangName="feather";
		return ret;
	}


	public static MCUBoard trinket8MH() {
        MCUBoard ret = new Adafruit(AVRArchitectureName, "trinket3");
		ret.mySlangName="trinket";
		return ret;
	}
	
	public static MCUBoard featherMO() {
        MCUBoard ret = new Adafruit(SAMDArchitectureName, "adafruit_feather_m0");
		ret.mySlangName="FeatherM0";
		ret.myAttributes.mo_mcu=true;
		return ret;
	}

	public static MCUBoard metroM4() {
        MCUBoard ret = new Adafruit(SAMDArchitectureName, metroM4ID);
		ret.mySlangName=metroM4SlangName;
		ret.myAttributes.mo_mcu=true;
		return ret;
	}
	
}
