package jUnit.boards;

import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("nls")
public class megaBoard extends GenericArduinoAvrBoard {
	public megaBoard() {
		super("mega");
		this.myBoardDescriptor.setUploadPort("COM11");

		Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		options.put("cpu", "atmega2560");
		this.myBoardDescriptor.setOptions(options);
	}



}
