package io.sloeber.core.api;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.core.common.Const;

@SuppressWarnings("nls")
public class Defaults {
	public static final String EXAMPLE_PACKAGE = "examples_Arduino_1_6_7.zip";
	public static final String EXAMPLES_URL = "https://eclipse.baeyens.it/download/" + EXAMPLE_PACKAGE;
	public static final String PLATFORM_NAME = "Arduino AVR Boards";
	public static final String[] INSTALLED_LIBRARIES = new String[] { "Ethernet", "Firmata", "GSM", "Keyboard",
			"LiquidCrystal", "Mouse", "SD", "Servo", "Stepper", "TFT", "WiFi", "CapacitiveSensor" };
	public static final String DEFAULT = "Default";
	private static final String LIBRARY_PATH_SUFFIX = "libraries";

	/**
	 * Arduino has the default libraries in the user home directory in subfolder
	 * Arduino/libraries. As the home directory is platform dependent getting
	 * the value is resolved by this method
	 *
	 * @return the folder where Arduino puts the libraries by default.
	 */
	public static String getPrivateLibraryPath() {
		IPath homPath = new Path(System.getProperty("user.home"));
		return homPath.append("Arduino").append(LIBRARY_PATH_SUFFIX).toString();
	}

	public static String getPrivateHardwarePath() {
		IPath homPath = new Path(System.getProperty("user.home"));
		return homPath.append("Arduino").append(Const.ARDUINO_HARDWARE_FOLDER_NAME).toString();
	}

	public static String getDefaultUploadProtocol() {
		return DEFAULT;
	}

	public static boolean  getUseArduinoToolSelection() {
		return true;
	}
}
