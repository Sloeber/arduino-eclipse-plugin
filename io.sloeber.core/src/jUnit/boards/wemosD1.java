package jUnit.boards;

import static org.junit.Assert.fail;

import java.util.Map;
import java.util.TreeMap;

import io.sloeber.core.api.BoardsManager;

@SuppressWarnings("nls")
public class wemosD1 extends IBoard {
	public wemosD1() {
		Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		options.put("CpuFrequency", "80");
		options.put("UploadSpeed", "115200");
		options.put("FlashSize", "4M1M");
		this.myBoardDescriptor = BoardsManager.getBoardDescriptor("package_esp8266com_index.json", "esp8266", "esp8266",
				"d1_mini", options);
		if (this.myBoardDescriptor == null) {
			fail("wemos Board not found");
		}
		this.myBoardDescriptor.setUploadPort("none");
	}

}