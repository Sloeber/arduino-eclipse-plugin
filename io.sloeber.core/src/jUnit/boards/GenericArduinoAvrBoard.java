package jUnit.boards;

import static org.junit.Assert.fail;

import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class GenericArduinoAvrBoard extends IBoard {

	public GenericArduinoAvrBoard(String boardName) {

		this.myBoardDescriptor = BoardsManager.getBoardDescriptor("package_index.json", "arduino", "Arduino AVR Boards",
				boardName, null);
		if (this.myBoardDescriptor == null) {
			fail(boardName + " Board not found");
		}
		this.myBoardDescriptor.setUploadPort("none");

	}

}