package it.baeyens.arduino.managers;
/**
 * This is a wrapper class to route the copied arduino manager code to the way the arduino plugin works
 * I assume this class will disappear over time
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import it.baeyens.arduino.common.ConfigurationPreferences;

public class ArduinoPreferences {

    public static Path getArduinoHome() {
	return Paths.get(ConfigurationPreferences.getInstallationPath().toString());
    }

    public static String getBoardUrls() {
	return ConfigurationPreferences.getBoardURLs();
    }

    public static void setBoardUrls(String boardUrls) {
	ConfigurationPreferences.setBoardURLs(boardUrls);
    }

}
