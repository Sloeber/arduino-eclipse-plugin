package io.sloeber.core.internal;

import static io.sloeber.core.api.Const.EXAMPLES_FODER;
import static io.sloeber.core.api.Const.eXAMPLES_FODER;

import org.eclipse.core.runtime.IPath;

import io.sloeber.core.api.IArduinoLibraryVersion;

public class ArduinoHardwareLibrary implements IArduinoLibraryVersion {
	private IPath myInstallPath;
	private String myName;
	private boolean myIsPrivate;

	public ArduinoHardwareLibrary(IPath installPath,boolean isPrivate) {
		myInstallPath = installPath;
		myName = myInstallPath.lastSegment();
		myIsPrivate=isPrivate;
	}

	@Override
	public String getName() {
		return myName;
	}

	@Override
	public IPath getInstallPath() {
		return myInstallPath;
	}

	@Override
	public boolean isHardwareLib() {
		return true;
	}

	@Override
	public boolean isPrivateLib() {
		return myIsPrivate;
	}

	@Override
	public IPath getExamplePath() {
		IPath Lib_examples = getInstallPath().append(eXAMPLES_FODER);
		if (Lib_examples.toFile().exists()) {
			return Lib_examples;
		}
		return getInstallPath().append(EXAMPLES_FODER);
	}

}
