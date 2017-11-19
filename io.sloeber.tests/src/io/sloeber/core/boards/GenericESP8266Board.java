package io.sloeber.core.boards;

import static org.junit.Assert.fail;

import java.util.Map;

import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class GenericESP8266Board extends IBoard {



		public GenericESP8266Board(String boardName,Map<String, String> options) {
			this.myBoardDescriptor = BoardsManager.getBoardDescriptor("package_esp8266com_index.json", "esp8266", "esp8266",
					boardName, options);
			if (this.myBoardDescriptor == null) {
				fail(boardName + " Board not found");
			}

		}


}