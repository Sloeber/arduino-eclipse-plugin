package io.sloeber.core.core;

import java.util.Map;

import io.sloeber.core.api.IInstallLibraryHandler;
import io.sloeber.core.managers.Library;

public class DefaultInstallHandler implements IInstallLibraryHandler {

	@Override
	public boolean autoInstall() {
		return false;
	}


	@Override
	public Map<String, Library> selectLibrariesToInstall(Map<String, Library> proposedLibsToInstall) {

		return proposedLibsToInstall;
	}

}
