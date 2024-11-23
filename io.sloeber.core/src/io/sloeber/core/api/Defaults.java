package io.sloeber.core.api;

import static io.sloeber.core.api.Const.*;

import java.time.Duration;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

@SuppressWarnings("nls")
public class Defaults {
	public static final String DEFAULT_INSTALL_JSON = "https://downloads.arduino.cc/packages/package_index.json";
	public static final String JANTJE_BOARD_JSON_URL = "https://raw.githubusercontent.com/jantje/hardware/master/package_jantje_index.json";
	public static final String DEFAULT_INSTALL_ARCHITECTURE = "avr";
    public static final String DEFAULT_INSTALL_MAINTAINER = "arduino";
    private static final String DEFAULT_DISCONNECT_SERIAL_TARGETS = "BurnBootLoader\nuploadWithBuild\nuploadWithoutBuild\nuploadWithProgrammerWithBuild\nuploadWithProgrammerWithoutBuild"; //$NON-NLS-1$

	public static final String[] DEFAULT_JSON_URLS = {DEFAULT_INSTALL_JSON,
			JANTJE_BOARD_JSON_URL,
            "https://raw.githubusercontent.com/jantje/ArduinoLibraries/master/library_jantje_index.json",
            "https://arduino.esp8266.com/stable/package_esp8266com_index.json",
            "https://www.pjrc.com/teensy/package_teensy_index.json",
            "https://downloads.arduino.cc/libraries/library_index.json"};



    public static final String EXAMPLE_PACKAGE = "examples_Arduino_1_8_10.zip";
    public static final String EXAMPLES_URL = "https://github.com/Sloeber/arduino-eclipse-plugin/releases/download/V4_3_2/"
            + EXAMPLE_PACKAGE;


    public static final String[] DEFAULT_INSTALLED_LIBRARIES = new String[] { "Ethernet", "Firmata", "GSM", "Keyboard",
            "LiquidCrystal", "Mouse", "SD", "Servo", "Stepper", "TFT", "WiFi", "CapacitiveSensor" };
    public static final String DEFAULT = "Default";

    public static final boolean useBonjour = true;
    public static final boolean autoInstallLibraries = true;
    public static final boolean useArduinoToolSelection = true;

    /**
     * Arduino has the default libraries in the user home directory in subfolder
     * Arduino/libraries. As the home directory is platform dependent getting the
     * value is resolved by this method
     *
     * @return the folder where Arduino puts the libraries by default.
     */
    public static String getPrivateLibraryPath() {
        IPath homPath = new Path(System.getProperty("user.home"));
        if (isMac || isWindows) {
            homPath = homPath.append("Documents");
        }
        return homPath.append("Arduino").append(ARDUINO_LIBRARY_FOLDER_NAME).toOSString();
    }

    public static String getPrivateHardwarePath() {
        IPath homPath = new Path(System.getProperty("user.home"));
        if (isMac || isWindows) {
            homPath = homPath.append("Documents");
        }
        return homPath.append("Arduino").append(ARDUINO_HARDWARE_FOLDER_NAME).toOSString();
    }

    public static String getDefaultDisconnectSerialTargets() {
        return DEFAULT_DISCONNECT_SERIAL_TARGETS;
    }

	public static Duration getJsonUpdateDuration() {
		return  Duration.ofDays(7);
	}

}
