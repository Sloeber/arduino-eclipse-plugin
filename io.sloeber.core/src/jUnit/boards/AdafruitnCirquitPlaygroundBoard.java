package jUnit.boards;

import static org.junit.Assert.fail;

import java.util.Map;

import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class AdafruitnCirquitPlaygroundBoard extends IBoard {
	public AdafruitnCirquitPlaygroundBoard(Map<String, String> options) {
		this.myBoardDescriptor = BoardsManager.getBoardDescriptor("package_index.json", "arduino", "Arduino AVR Boards",
				"circuitplay32u4cat", options);
		if (this.myBoardDescriptor == null) {
			fail("Adafruit Cirquit Playground Board not found");
		}
		this.myBoardDescriptor.setUploadPort("none");
	}

	@Override
	public boolean isExampleOk(String inoName, String libName) {
		return true; // default everything is fine
	}
}