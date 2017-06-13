package io.sloeber.core.tools;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

public class KeyValue {
	private static String ENTRY_SEPARATOR = "\n"; //$NON-NLS-1$
	private static String KEY_VALUE_SEPARATOR = "="; //$NON-NLS-1$

	/**
	 * convert a Map<string,String> to a string so it can be stored The strings
	 * can not contain '\n' or '='
	 * 
	 * @return a string representation of the map
	 */
	static public String makeString(Map<String, String> map) {
		String ret = new String();
		String concat = new String();
		if (map != null) {
			for (Entry<String, String> curOption : map.entrySet()) {
				ret += concat + curOption.getKey() + KEY_VALUE_SEPARATOR + curOption.getValue();
				concat = ENTRY_SEPARATOR;
			}
		}
		return ret;
	}

	/**
	 * convert a string to a Map<String, String> so it can be read from a string
	 * based storage
	 *
	 * @param options
	 */
	static public Map<String, String> makeMap(String options) {
		Map<String, String> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
		if (options != null) {
			String[] lines = options.split(ENTRY_SEPARATOR);
			for (String curLine : lines) {
				String[] values = curLine.split(KEY_VALUE_SEPARATOR, 2);
				if (values.length == 2) {
					map.put(values[0], values[1]);
				}
			}
		}
		return map;
	}
}
