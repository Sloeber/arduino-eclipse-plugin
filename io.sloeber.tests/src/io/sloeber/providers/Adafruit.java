package io.sloeber.providers;

import static org.junit.Assert.*;

import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class Adafruit extends MCUBoard {
    public final static String packageURL = "https://adafruit.github.io/arduino-board-index/package_adafruit_index.json";
    private static final String AVRArchitectureName = "avr";
    private static final String SAMDArchitectureName = "samd";
    //private static final String SAMPlatformName = "Arduino SAM Boards (32-bits ARM Cortex-M3)";
    private static final String NFR52ArchitectureName = "nRF52";
    //private static final String XICEDPlatformName = "Adafruit WICED";
    public static final String metroM4ID = "adafruit_metro_m4";

    public Adafruit(String architectureName, String boardName) {

        myBoardDescriptor = BoardsManager.getBoardDescription("package_adafruit_index.json", "adafruit",
                architectureName, boardName, null);
        if (myBoardDescriptor == null) {
            fail(boardName + " Board not found");
        }
        myBoardDescriptor.setUploadPort("none");
        setAttributes();
    }

    public Adafruit(BoardDescription boardDesc) {
        myBoardDescriptor = boardDesc;
        myBoardDescriptor.setUploadPort("none");
        setAttributes();
    }

    public static MCUBoard feather() {
        return new Adafruit(NFR52ArchitectureName, "feather52832");
    }

    public static MCUBoard trinket8MH() {
        return new Adafruit(AVRArchitectureName, "trinket3");
    }

    public static MCUBoard featherMO() {
        return new Adafruit(SAMDArchitectureName, "adafruit_feather_m0");
    }

    public static MCUBoard metroM4() {
        return new Adafruit(SAMDArchitectureName, metroM4ID);
    }

    @Override
    protected void setAttributes() {
        String BoardID = myBoardDescriptor.getBoardID();
        switch (BoardID) {
        case metroM4ID:
        case "adafruit_feather_m0":
            myAttributes.mo_mcu = true;
            return;
        }

    }

    @Override
    public MCUBoard createMCUBoard(BoardDescription boardDesc) {
        return new Adafruit(boardDesc);
    }

}
