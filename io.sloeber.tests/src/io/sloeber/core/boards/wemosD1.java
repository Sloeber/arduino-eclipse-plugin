package io.sloeber.core.boards;

import java.util.Map;
import java.util.TreeMap;

@SuppressWarnings("nls")
public class wemosD1 extends GenericESP8266Board {
	public wemosD1() {
		super("d1_mini", defaultOptions());
		this.myBoardDescriptor.setUploadPort("COM21");
	}
private static Map<String, String> defaultOptions() {
	Map<String, String> options = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	options.put("CpuFrequency", "80");
	options.put("UploadSpeed", "115200");
	options.put("FlashSize", "4M1M");
	return options;
}
}