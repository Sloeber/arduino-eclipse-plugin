package io.sloeber.ui.listeners;

import java.util.Map;

import io.sloeber.core.api.IInstallLibraryHandler;
import io.sloeber.core.api.Json.ArduinoLibraryVersion;
import io.sloeber.ui.helpers.MyPreferences;

public class MyLibraryInstallHandler implements IInstallLibraryHandler {

	@Override
	public boolean autoInstall() {
		return MyPreferences.getAutomaticallyInstallLibrariesOption();
	}

	@Override
	public Map<String, ArduinoLibraryVersion> selectLibrariesToInstall(Map<String, ArduinoLibraryVersion> proposedLibsToInstall) {
		return proposedLibsToInstall;
	}

}
