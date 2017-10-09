package io.sloeber.core.boards;

import static org.junit.Assert.fail;

import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class Primo extends IBoard {

	public Primo() {
		this.myBoardDescriptor = BoardsManager.getBoardDescriptor("package_index.json", "arduino",
				"Arduino NRF52 Boards", "primo", null);
		if (this.myBoardDescriptor == null) {
			fail("Primo Board not found");
		}
		this.myBoardDescriptor.setUploadPort("none");
	}
}
