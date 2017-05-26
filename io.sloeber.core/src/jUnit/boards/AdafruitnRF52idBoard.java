package jUnit.boards;

import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Map;

import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class AdafruitnRF52idBoard extends IBoard {
	public AdafruitnRF52idBoard(Map<String, String> options) {
		this.myBoardDescriptor = BoardsManager.getBoardDescriptor("package_adafruit_index.json", "adafruit",
				"Adafruit nRF52", "feather52", options);
		if (this.myBoardDescriptor == null) {
			fail("Adafruit nRF52 Board not found");
		}
		this.myBoardDescriptor.setUploadPort("none");
	}

	@Override
	public boolean isExampleOk(String inoName, String libName) {
		final String[] sketchIsNotOk = { "cplay_neopixel_picker" };
		if (Arrays.asList(sketchIsNotOk).contains(inoName.replace(" ", ""))) {
			return false;
		}
		return true; // default everything is fine
	}
}