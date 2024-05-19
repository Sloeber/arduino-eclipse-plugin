package io.sloeber.core.internal;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import static io.sloeber.core.api.Const.*;

import io.sloeber.core.api.ConfigurationPreferences;
import io.sloeber.core.api.IArduinoLibraryVersion;
import io.sloeber.core.api.IExample;

public class Example implements IExample {
	private IArduinoLibraryVersion myLib;
	private IPath myExampleLocation;
	private IPath myFQN;
	private String myName;

	@Override
	public String toSaveString() {
		return String.join(SEMI_COLON, getBreadCrumbs());
	}

	public Example(IArduinoLibraryVersion lib, Path path) {
		myLib = lib;
		myExampleLocation = path;
		myName = path.lastSegment();
		calculateFQN();
	}

	private void calculateFQN() {
		if (myLib == null) {
			myFQN = Path.fromPortableString(EXAMPLES_FOLDER );
			IPath exampleFolder = ConfigurationPreferences.getInstallationPathExamples();
			if (exampleFolder.isPrefixOf(myExampleLocation)) {
				myFQN = myFQN.append(myExampleLocation.makeRelativeTo(exampleFolder));
			} else {
				myFQN = myFQN.append(myExampleLocation);
			}
		} else {
			myFQN = myLib.getFQN().append(myName);
		}
	}

	@Override
	public IArduinoLibraryVersion getArduinoLibrary() {
		return myLib;
	}

	@Override
	public IPath getCodeLocation() {
		return myExampleLocation;
	}

	@Override
	public String getName() {
		return myName;
	}

	@Override
	public String getID() {
		return myFQN.toString();
	}

	@Override
	public String[] getBreadCrumbs() {
		return myFQN.segments();

	}

}
