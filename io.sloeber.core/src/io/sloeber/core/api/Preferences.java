package io.sloeber.core.api;

import static io.sloeber.core.common.ConfigurationPreferences.*;
import static io.sloeber.core.common.Const.*;

import java.util.HashSet;

import org.eclipse.cdt.core.parser.util.StringUtil;

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
    private static String stringSplitter = "\n";//$NON-NLS-1$
    private static final String KEY_DISCONNECT_SERIAL_TAGETS = "Target names that require serial disconnect to run";//$NON-NLS-1$
    private static final String DEFAULT_DISCONNECT_SERIAL_TARGETS = "BurnBootLoader\nuploadWithBuild\nuploadWithoutBuild\nuploadWithProgrammerWithBuild\nuploadWithProgrammerWithoutBuild"; //$NON-NLS-1$

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

    public static String getDefaultDisconnectSerialTargets() {
        return DEFAULT_DISCONNECT_SERIAL_TARGETS;
    }

    public static String[] getDisconnectSerialTargetsList() {
        return getDisconnectSerialTargets().split(stringSplitter);
    }

    public static String getDisconnectSerialTargets() {
        return getString(KEY_DISCONNECT_SERIAL_TAGETS, DEFAULT_DISCONNECT_SERIAL_TARGETS).replace("\r", EMPTY);//$NON-NLS-1$
    }

    public static void setDisconnectSerialTargets(String targets) {
        setString(KEY_DISCONNECT_SERIAL_TAGETS, targets);
    }

    public static void setDisconnectSerialTargets(String targets[]) {
        setString(KEY_DISCONNECT_SERIAL_TAGETS, StringUtil.join(targets, stringSplitter));
    }

    public static void setDisconnectSerialTargets(HashSet<String> targets) {
        setString(KEY_DISCONNECT_SERIAL_TAGETS, StringUtil.join(targets, stringSplitter));
    }

}
