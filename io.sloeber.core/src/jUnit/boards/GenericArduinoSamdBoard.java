package jUnit.boards;

import static org.junit.Assert.fail;

import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class GenericArduinoSamdBoard extends IBoard {

	public GenericArduinoSamdBoard(String boardName) {

		this.myBoardDescriptor = BoardsManager.getBoardDescriptor("package_index.json", "arduino",
				"Arduino SAMD Boards (32-bits ARM Cortex-M0+)", boardName, null);
		if (this.myBoardDescriptor == null) {
			fail(boardName + " Board not found");
		}
		this.myBoardDescriptor.setUploadPort("none");

	}

}
