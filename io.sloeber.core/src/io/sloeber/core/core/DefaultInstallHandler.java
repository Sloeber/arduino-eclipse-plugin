package io.sloeber.core.core;

import java.util.Map;

import io.sloeber.core.api.IInstallLibraryHandler;
import io.sloeber.core.api.Json.library.LibraryJson;

public class DefaultInstallHandler implements IInstallLibraryHandler {

    @Override
    public boolean autoInstall() {
        return false;
    }

    @Override
    public Map<String, LibraryJson> selectLibrariesToInstall(Map<String, LibraryJson> proposedLibsToInstall) {

        return proposedLibsToInstall;
    }

}
