package jUnit.boards;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Map;

import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class EsploraBoard extends IBoard {
	public EsploraBoard(Map<String, String> options) {
		this.myBoardDescriptor = BoardsManager.getBoardDescriptor("package_index.json", "arduino", "Arduino AVR Boards",
				"esplora", options);
		if (this.myBoardDescriptor == null) {
			fail("Esplora Board not found");
		}
		this.myBoardDescriptor.setUploadPort("none");
	}

	@Override
	public boolean isExampleOk(String inoName, String libName) {
		final String[] inoNotOk = { "Firmataexamples?StandardFirmataBLE", "Firmataexamples?StandardFirmataChipKIT",
				"Firmataexamples?StandardFirmataEthernet", "Firmataexamples?StandardFirmataWiFi",
				"cplay_neopixel_picker" };
		final String[] libNotOk = { "Adafruit_BluefruitLE_nRF51" };
		if (Arrays.asList(libNotOk).contains(libName)) {
			return false;
		}
		if (inoName.replace(" ", "").startsWith("TFTexamples?Esplora?Esplora"))
			return false;
		if (Arrays.asList(inoNotOk).contains(inoName.replace(" ", "")))
			return false;
		return true; // default everything is fine

	}
}
