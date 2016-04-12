package it.baeyens.arduino.common;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

@SuppressWarnings("nls")
public class Defaults {
    public static final boolean OPEN_SERIAL_WITH_MONITOR = true;
    public static final boolean AUTO_IMPORT_LIBRARIES = true;
    public static final boolean PRIVATE_OVER_HARDWARE_LIBRARIES = true;
    public static final String PLATFORM_URLS = "http://downloads.arduino.cc/packages/package_index.json"
	    + System.lineSeparator() + "http://arduino.esp8266.com/stable/package_esp8266com_index.json";
    public static final String LIBRARIES_URL = "http://downloads.arduino.cc/libraries/library_index.json";
    public static final String EXAMPLE_PACKAGE = "examples_Arduino_1_6_7.zip";
    public static final String EXAMPLES_URL = "http://eclipse.baeyens.it/download/" + EXAMPLE_PACKAGE;
    public static final String PLATFORM_NAME = "Arduino AVR Boards";
    public static final String[] INSTALLED_LIBRARIES = new String[] { "Ethernet", "Firmata", "GSM", "Keyboard",
	    "LiquidCrystal", "Mouse", "SD", "Servo", "Stepper", "TFT", "WiFi" };

    /**
     * Arduino has the default libraries in the user home directory in subfolder
     * Arduino/libraries. As the home directory is platform dependent getting
     * the value is resolved by this method
     * 
     * @return the folder where Arduino puts the libraries by default.
     */
    public static String getPrivateLibraryPath() {
	IPath homPath = new Path(System.getProperty("user.home"));
	return homPath.append("Arduino").append(Const.LIBRARY_PATH_SUFFIX).toString();
    }

    public static String getPrivateHardwarePath() {
	IPath homPath = new Path(System.getProperty("user.home"));
	return homPath.append("Arduino").append(Const.ARDUINO_HARDWARE_FOLDER_NAME).toString();
    }
}
