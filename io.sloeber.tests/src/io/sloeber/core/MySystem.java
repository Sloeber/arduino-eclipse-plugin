package io.sloeber.core;

import io.sloeber.core.api.Other;

/*
 * Set system specific info here.
 * For now this is only the teensy installlocation
 */
@SuppressWarnings("nls")
public class MySystem {
	private static String jantjesWindowsMachine = "D:\\arduino\\arduino-1.8.2Teensy1.38beta2\\hardware\\teensy";
	private static String jantjesVirtualLinuxMachine = "/home/jantje/programs/arduino-1.8.0/hardware/teensy";

	public static String getTeensyPlatform() {
		switch (Other.getSystemHash()) {
		case "1248215851":
			return jantjesWindowsMachine;
		case "still need to gett the key":
			return jantjesVirtualLinuxMachine;
		}
		return new String();
	}
}
