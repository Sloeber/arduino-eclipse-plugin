package io.sloeber.core.api;

import io.sloeber.core.common.InstancePreferences;

/**
 * A class to controll the preferences at the workspace level
 * @author jan
 *
 */
public class Preferences {
	public static void setAutoImportLibraries(boolean booleanValue) {
		InstancePreferences.setAutomaticallyImportLibraries(booleanValue);

	}

	public static void setPragmaOnceHeaders(boolean booleanValue) {
		InstancePreferences.setPragmaOnceHeaders(booleanValue);

	}

	public static boolean getPragmaOnceHeaders() {
		return InstancePreferences.getPragmaOnceHeaders();
	}

	public static boolean getAutoImportLibraries() {
		return InstancePreferences.getAutomaticallyImportLibraries();
	}

	public static void setUseArduinoToolSelection(boolean booleanValue) {
		InstancePreferences.setUseArduinoToolSelection(booleanValue);

	}

	public static boolean getUseArduinoToolSelection() {
		return InstancePreferences.getUseArduinoToolSelection();
	}


}
