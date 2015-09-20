package it.baeyens.arduino.common.test;

import static org.junit.Assert.assertEquals;
import it.baeyens.arduino.common.ArduinoInstancePreferences;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

public class ArduinoIDEVersionNameParsing {

    private Map<String, String> VersionList;

    @Test
    public void testAllArduinoVersions() {
	VersionList = new LinkedHashMap<String, String>();
	// The tests below have been deactivated as they fail
	// and the plugin does not support these versions
	// VersionList.put("1.0.1", "101");
	// VersionList.put("1.0.2", "102");
	// VersionList.put("1.0.3", "103");
	// VersionList.put("1.0.4", "104");
	VersionList.put("1.5.1", "151");
	VersionList.put("1.5.2", "152");
	VersionList.put("1.5.3", "153");
	VersionList.put("1.5.4", "154");
	VersionList.put("1.5.5", "155");
	VersionList.put("1.5.6", "156");
	VersionList.put("1.5.6-r2", "156");
	VersionList.put("1.5.7", "157");
	VersionList.put("1.5.8", "158");
	VersionList.put("1.6.0", "10600");
	VersionList.put("1.6.1", "10601");
	VersionList.put("1.6.2", "10602");
	VersionList.put("1.6.3", "10603");
	VersionList.put("1.6.4", "10604");
	VersionList.put("1.6.5", "10605");
	VersionList.put("1.6.5-r2", "10605");
	VersionList.put("1.6.5-r3", "10605");
	VersionList.put("1.6.5-r4", "10605");
	VersionList.put("1.6.5-r5", "10605");
	for (Entry<String, String> currentVersion : VersionList.entrySet()) {
	    assertEquals(currentVersion.getValue(), ArduinoInstancePreferences.GetArduinoDefineValueInternal(currentVersion.getKey()));
	}
    }
}
