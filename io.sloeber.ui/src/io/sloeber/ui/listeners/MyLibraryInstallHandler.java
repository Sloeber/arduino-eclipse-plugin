package io.sloeber.ui.listeners;

import java.util.Map;

import io.sloeber.core.api.IInstallLibraryHandler;
import io.sloeber.core.api.LibraryDescriptor;

public class MyLibraryInstallHandler implements IInstallLibraryHandler {

	@Override
	public boolean autoInstall() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public Map<String, LibraryDescriptor> selectLibrariesToInstall(Map<String, LibraryDescriptor> proposedLibsToInstall) {
		// TODO Auto-generated method stub
		return proposedLibsToInstall;
	}

}
