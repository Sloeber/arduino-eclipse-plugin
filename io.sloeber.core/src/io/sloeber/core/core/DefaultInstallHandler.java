package io.sloeber.core.core;

import java.util.Map;

import io.sloeber.core.api.IInstallLibraryHandler;
import io.sloeber.core.api.LibraryDescriptor;

public class DefaultInstallHandler implements IInstallLibraryHandler {

	@Override
	public boolean autoInstall() {
		return false;
	}


	@Override
	public Map<String, LibraryDescriptor> selectLibrariesToInstall(Map<String, LibraryDescriptor> proposedLibsToInstall) {

		return proposedLibsToInstall;
	}

}
