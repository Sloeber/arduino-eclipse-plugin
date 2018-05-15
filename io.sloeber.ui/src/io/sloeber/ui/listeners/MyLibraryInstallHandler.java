package io.sloeber.ui.listeners;

import java.util.Map;

import io.sloeber.core.api.IInstallLibraryHandler;
import io.sloeber.core.api.LibraryDescriptor;
import io.sloeber.ui.preferences.PreferencePage;

public class MyLibraryInstallHandler implements IInstallLibraryHandler {

	@Override
	public boolean autoInstall() {
		return PreferencePage.getAutomaticallyInstallLibrariesOption();
	}

	@Override
	public Map<String, LibraryDescriptor> selectLibrariesToInstall(Map<String, LibraryDescriptor> proposedLibsToInstall) {
		return proposedLibsToInstall;
	}

}
