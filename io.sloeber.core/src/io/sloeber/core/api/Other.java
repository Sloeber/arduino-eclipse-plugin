package io.sloeber.core.api;

import io.sloeber.common.ConfigurationPreferences;

public class Other {
    public static String getSystemHash() {
	return ConfigurationPreferences.getSystemHash();
    }
}
