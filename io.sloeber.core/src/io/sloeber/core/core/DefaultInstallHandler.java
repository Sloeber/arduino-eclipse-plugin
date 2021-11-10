package io.sloeber.core.core;

import java.util.Map;

import io.sloeber.core.api.IInstallLibraryHandler;
import io.sloeber.core.api.Json.ArduinoLibraryVersion;

public class DefaultInstallHandler implements IInstallLibraryHandler {

    @Override
    public boolean autoInstall() {
        return false;
    }

    @Override
    public Map<String, ArduinoLibraryVersion> selectLibrariesToInstall(Map<String, ArduinoLibraryVersion> proposedLibsToInstall) {

        return proposedLibsToInstall;
    }

}
