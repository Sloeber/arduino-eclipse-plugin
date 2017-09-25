package io.sloeber.core.boards;

import static org.junit.Assert.fail;

import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class AdafruitnRF52idBoard extends IBoard {
	public AdafruitnRF52idBoard() {
		this.myBoardDescriptor = BoardsManager.getBoardDescriptor("package_adafruit_index.json", "adafruit",
				"Adafruit nRF52", "feather52", null);
		if (this.myBoardDescriptor == null) {
			fail("Adafruit nRF52 Board not found");
		}
		this.myBoardDescriptor.setUploadPort("none");
	}

}