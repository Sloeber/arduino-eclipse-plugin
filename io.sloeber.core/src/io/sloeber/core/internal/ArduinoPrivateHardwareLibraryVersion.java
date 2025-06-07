package io.sloeber.core.internal;

import static io.sloeber.core.api.Const.*;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import io.sloeber.arduinoFramework.api.IArduinoLibrary;
import io.sloeber.arduinoFramework.api.IArduinoLibraryVersion;
import io.sloeber.core.api.VersionNumber;
import io.sloeber.core.common.InstancePreferences;

public class ArduinoPrivateHardwareLibraryVersion implements IArduinoLibraryVersion {
	private IPath myInstallPath;
	private String myName;
	private IPath myFQN;

	public ArduinoPrivateHardwareLibraryVersion(IPath installPath) {
		myInstallPath = installPath;
		myName = myInstallPath.lastSegment();
		myFQN= calculateFQN(getName());
	}

	public ArduinoPrivateHardwareLibraryVersion(String curSaveString) {
		String[] parts=curSaveString.split(SEMI_COLON);
		myName=parts[parts.length-1];
		myFQN= calculateFQN(getName());
		String privateLibPaths[] = InstancePreferences.getPrivateLibraryPaths();
		for (String curLibPath : privateLibPaths) {
			Path curPrivPath=new Path(curLibPath);
			if(curPrivPath.append(myName).toFile().exists()) {
				myInstallPath=curPrivPath.append(myName);
				return;
			}
		}
		//This should not happen
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
		return false;
	}

	@Override
	public boolean isPrivateLib() {
		return true;
	}

	@Override
	public IPath getExamplePath() {
		IPath Lib_examples = getInstallPath().append(eXAMPLES_FODER);
		if (Lib_examples.toFile().exists()) {
			return Lib_examples;
		}
		return getInstallPath().append(EXAMPLES_FOLDER);
	}

	static public IPath calculateFQN(String libName) {
		return  Path.fromPortableString(SLOEBER_LIBRARY_FQN).append(PRIVATE).append(libName);
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
		return "unknown maintainer (private lib)"; //$NON-NLS-1$
	}

	@Override
	public String getAuthor() {
		return "unknown author (private lib)"; //$NON-NLS-1$
	}

}
