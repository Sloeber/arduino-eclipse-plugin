package it.baeyens.arduino.managers;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

import it.baeyens.arduino.common.ConfigurationPreferences;
import it.baeyens.arduino.ui.Activator;

public class ArduinoPreferences {

    private static final String BOARD_URLS = "boardUrls"; //$NON-NLS-1$

    private static final String defaultBoardUrls = "http://downloads.arduino.cc/packages/package_index.json" //$NON-NLS-1$
	    + "\nhttp://arduino.esp8266.com/stable/package_esp8266com_index.json"; //$NON-NLS-1$

    private static IEclipsePreferences getPrefs() {
	return InstanceScope.INSTANCE.getNode(Activator.getId());
    }

    public static Path getArduinoHome() {
	return Paths.get(ConfigurationPreferences.getInstallationPath().toString());
    }

    public static String getBoardUrls() {
	return getPrefs().get(BOARD_URLS, defaultBoardUrls);
    }

    public static void setBoardUrls(String boardUrls) {
	IEclipsePreferences prefs = getPrefs();
	prefs.put(BOARD_URLS, boardUrls);
	try {
	    prefs.flush();
	} catch (BackingStoreException e) {
	    Activator.log(e);
	}
    }

    public static String getDefaultBoardUrls() {
	return defaultBoardUrls;
    }
}
