package jUnit.boards;

import static org.junit.Assert.fail;

import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class NodeMCUBoard extends IBoard {
	public NodeMCUBoard() {
		this.myBoardDescriptor = BoardsManager.getBoardDescriptor("package_esp8266com_index.json", "esp8266", "esp8266",
				"nodemcu", null);
		if (this.myBoardDescriptor == null) {
			fail("nodemcu Board not found");
		}
		this.myBoardDescriptor.setUploadPort("none");
	}

}