package io.sloeber.core.core;

import java.util.Map;

import io.sloeber.core.api.IArduinoLibraryVersion;
import io.sloeber.core.api.IInstallLibraryHandler;

public class DefaultInstallHandler implements IInstallLibraryHandler {

    @Override
    public boolean autoInstall() {
        return false;
    }

    @Override
    public Map<String, IArduinoLibraryVersion> selectLibrariesToInstall(Map<String, IArduinoLibraryVersion> proposedLibsToInstall) {

        return proposedLibsToInstall;
    }

}
