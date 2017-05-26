package jUnit.boards;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Map;

import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class UnoBoard extends IBoard {
	public UnoBoard(Map<String, String> options) {
		this.myBoardDescriptor = BoardsManager.getBoardDescriptor("package_index.json", "arduino", "Arduino AVR Boards",
				"uno", options);
		if (this.myBoardDescriptor == null) {
			fail("uno Board not found");
		}
		this.myBoardDescriptor.setUploadPort("none");
	}

	@Override
	public boolean isExampleOk(String inoName, String libName) {
		final String[] notOkForUno = { "Firmataexamples?StandardFirmataWiFi", "examples?04.Communication?MultiSerial",
				"examples?09.USB?Keyboard?KeyboardLogout", "examples?09.USB?Keyboard?KeyboardMessage",
				"examples?09.USB?Keyboard?KeyboardReprogram", "examples?09.USB?Keyboard?KeyboardSerial",
				"examples?09.USB?KeyboardAndMouseControl", "examples?09.USB?Mouse?ButtonMouseControl",
				"examples?09.USB?Mouse?JoystickMouseControl", "cplay_neopixel_picker" };
		final String[] libNotOk = { "Adafruit_BluefruitLE_nRF51" };
		if (Arrays.asList(libNotOk).contains(libName)) {
			return false;
		}
		if (inoName.startsWith("Esploraexamples"))
			return false;
		if (inoName.replace(" ", "").startsWith("TFTexamples?Esplora?Esplora"))
			return false;

		if (inoName.contains("Firmata"))
			return false;
		if (Arrays.asList(notOkForUno).contains(inoName.replace(" ", "")))
			return false;
		return true; // default everything is fine
	}

}