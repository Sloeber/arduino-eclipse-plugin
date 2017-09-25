package io.sloeber.core.boards;

import static org.junit.Assert.fail;

import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class Due extends IBoard {

	public Due() {
		this.myBoardDescriptor = BoardsManager.getBoardDescriptor("package_index.json", "arduino",
				"Arduino SAM Boards (32-bits ARM Cortex-M3)", "arduino_due_x", null);
		if (this.myBoardDescriptor == null) {
			fail("Due Board not found");
		}
		this.myBoardDescriptor.setUploadPort("COM8");
	}

	@Override
	public String getName() {
		return "due";
	}

}
