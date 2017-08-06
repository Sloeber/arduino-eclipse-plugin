package io.sloeber.core.api;

import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.managers.Manager;

public class JsonManager {
	public static void setURL(String[] newUrls) {
		Manager.setJsonURL(newUrls);
	}
	
	public static String[] getURLList() {
		return Manager.getJsonURLList();
	}
	
	public static void setUpdateJsonFilesFlag(boolean flag) {
		ConfigurationPreferences.setUpdateJasonFilesFlag(flag);
	}
	
	public static String getDefaultURLs() {
		return ConfigurationPreferences.getDefaultJsonURLs();
	}
}
