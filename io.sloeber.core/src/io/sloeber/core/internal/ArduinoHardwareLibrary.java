package io.sloeber.core.internal;

import static io.sloeber.core.api.Const.*;

import org.eclipse.core.runtime.IPath;

import io.sloeber.core.api.BoardDescription;
import io.sloeber.core.api.Common;
import io.sloeber.core.api.IArduinoLibraryVersion;

public class ArduinoHardwareLibrary implements IArduinoLibraryVersion {
	private IPath myInstallPath;
	private String myName;

	public ArduinoHardwareLibrary(IPath installPath) {
		myInstallPath = installPath;
		myName = myInstallPath.lastSegment();
	}

	public ArduinoHardwareLibrary(String curSaveString, BoardDescription boardDesc) {
		String[] parts=curSaveString.split(SEMI_COLON);
		myName=parts[parts.length-1];
		myInstallPath=boardDesc.getReferencingLibraryPath().append(myName);
		if(!myInstallPath.toFile().exists()) {
			myInstallPath=boardDesc.getReferencedCoreLibraryPath().append(myName);
		}
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
		return !Common.sloeberHomePath.isPrefixOf(myInstallPath);
	}

	@Override
	public IPath getExamplePath() {
		IPath Lib_examples = getInstallPath().append(eXAMPLES_FODER);
		if (Lib_examples.toFile().exists()) {
			return Lib_examples;
		}
		return getInstallPath().append(EXAMPLES_FOLDER);
	}

	@Override
	public String[] getBreadCrumbs() {
		String[] ret = new String[2];
		ret[0]=BOARD;
		ret[1]=getName();
		return ret;
	}

}
