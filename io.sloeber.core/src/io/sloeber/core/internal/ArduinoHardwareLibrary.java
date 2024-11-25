package io.sloeber.core.internal;

import static io.sloeber.core.api.Const.*;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.arduinoFramework.api.BoardDescription;
import io.sloeber.arduinoFramework.api.IArduinoLibrary;
import io.sloeber.arduinoFramework.api.IArduinoLibraryVersion;
import io.sloeber.core.api.Common;
import io.sloeber.core.api.VersionNumber;

public class ArduinoHardwareLibrary implements IArduinoLibraryVersion {
	private IPath myInstallPath;
	private String myName;
	private IPath myFQN;

	public ArduinoHardwareLibrary(IPath installPath) {
		myInstallPath = installPath;
		myName = myInstallPath.lastSegment();
		myFQN=calculateFQN(getName());
	}

	public ArduinoHardwareLibrary(String curSaveString, BoardDescription boardDesc) {
		String[] parts=curSaveString.split(SEMI_COLON);
		myName=parts[parts.length-1];
		myInstallPath=boardDesc.getReferencingLibraryPath().append(myName);
		if(!myInstallPath.toFile().exists()) {
			myInstallPath=boardDesc.getReferencedCoreLibraryPath().append(myName);
		}
		myFQN=calculateFQN(getName());
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


	public static IPath calculateFQN(String libName) {
		return  Path.fromPortableString(SLOEBER_LIBRARY_FQN).append(BOARD).append(libName);
	}

	@Override
	public String[] getBreadCrumbs() {
		return myFQN.segments();
	}

	@Override
	public IPath getFQN() {
		return myFQN;
	}

	@Override
	public boolean equals(IArduinoLibraryVersion other) {
		return myFQN.equals(other.getFQN());
	}

	@Override
	public int compareTo(IArduinoLibraryVersion o) {
		return 0;
	}

	@Override
	public IArduinoLibrary getLibrary() {
		return null;
	}

	@Override
	public VersionNumber getVersion() {
		return null;
	}

	@Override
	public boolean isInstalled() {
		return true;
	}

	@Override
	public List<String> getArchitectures() {
		return null;
	}

	@Override
	public String getParagraph() {
		return EMPTY_STRING;
	}

	@Override
	public String getSentence() {
		return EMPTY_STRING;
	}

	@Override
	public String getMaintainer() {
		return "Maintainer is board provider"; //$NON-NLS-1$
	}

	@Override
	public String getAuthor() {
		// TODO Auto-generated method stub
		return "Author is board provider"; //$NON-NLS-1$
	}

}
