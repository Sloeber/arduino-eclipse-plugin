package io.sloeber.core.api;

import cc.arduino.packages.discoverers.SloeberNetworkDiscovery;
import io.sloeber.core.common.ConfigurationPreferences;
import io.sloeber.core.common.InstancePreferences;

/**
 * This is a wrapper class to make internal configuration settings externally available
 * There should not be any logic in this class only redirections to internal methods
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

	public static void setUpdateJsonFiles(boolean flag) {
		ConfigurationPreferences.setUpdateJasonFilesFlag(flag);
	}
	public static boolean getUpdateJsonFiles() {
		return ConfigurationPreferences.getUpdateJasonFilesFlag();
	}
	
	/**
	 *wrapper for ConfigurationPreferences.useBonjour();
	 */
	public static boolean useBonjour() {
		return InstancePreferences.useBonjour();
	}

	/**
	 *wrapper for ConfigurationPreferences.setUseBonjour(newFlag);
	 */
	public static void setUseBonjour(boolean newFlag) {
		InstancePreferences.setUseBonjour(newFlag);
		if(newFlag) {
			SloeberNetworkDiscovery.start();
		}else {
			SloeberNetworkDiscovery.stop();
		}
	}

}
