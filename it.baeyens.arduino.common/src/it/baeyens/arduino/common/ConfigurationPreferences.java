package it.baeyens.arduino.common;

import java.io.File;

import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

public class ConfigurationPreferences {
    private static final String ARDUINO_HOME = "arduinoHome"; //$NON-NLS-1$
    private static final String defaultHome = new Path(System.getProperty("user.home")).append("arduinoPlugin").toString(); //$NON-NLS-1$ //$NON-NLS-2$

    private static String getGlobalString(String key, String defaultValue) {
	IEclipsePreferences myScope = ConfigurationScope.INSTANCE.getNode(ArduinoConst.NODE_ARDUINO);
	return myScope.get(key, defaultValue);
    }

    public static Path getInstallationPath() {
	return new Path(getGlobalString(ARDUINO_HOME, defaultHome));
    }

    /**
     * Get the file that contains the preprocessing platform content
     * 
     * @return
     */
    public static File getPreProcessingPlatformFile() {
	return getInstallationPath().append(ArduinoConst.PRE_PROCESSING_PLATFORM_TXT).toFile();
    }

    /**
     * Get the file that contains the post processing platform content
     * 
     * @return
     */
    public static File getPostProcessingPlatformFile() {
	return getInstallationPath().append(ArduinoConst.POST_PROCESSING_PLATFORM_TXT).toFile();
    }

    public static File getPreProcessingBoardsFile() {
	return getInstallationPath().append(ArduinoConst.PRE_PROCESSING_BOARDS_TXT).toFile();
    }

    public static File getPostProcessingBoardsFile() {
	return getInstallationPath().append(ArduinoConst.POST_PROCESSING_BOARDS_TXT).toFile();
    }

}
