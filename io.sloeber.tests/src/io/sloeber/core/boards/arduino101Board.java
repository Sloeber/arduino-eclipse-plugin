package io.sloeber.core.boards;

import static org.junit.Assert.fail;

import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class arduino101Board extends IBoard {
	public arduino101Board() {
		this.myBoardDescriptor = BoardsManager.getBoardDescriptor("package_index.json", "Intel", "Intel Curie Boards",
				"arduino_101", null);
		if (this.myBoardDescriptor == null) {
			fail("Arduino 101 Board not found");
		}
		this.myBoardDescriptor.setUploadPort("COM9");
	}

}