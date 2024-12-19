package io.sloeber.core.internal;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import static io.sloeber.core.api.Const.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import io.sloeber.arduinoFramework.api.IArduinoLibraryVersion;
import io.sloeber.arduinoFramework.api.IExample;
import io.sloeber.core.api.ConfigurationPreferences;

public class Example implements IExample {
	protected IPath myFQN;
	protected Map<String, IArduinoLibraryVersion> myLibs=new HashMap<>();
	protected IPath myExampleLocation;

	@Override
	public String toSaveString() {
		return String.join(SEMI_COLON, getBreadCrumbs());
	}

	protected Example() {

	}
	public Example(IArduinoLibraryVersion lib, Path path) {
		if (lib != null) {
			myLibs.put(lib.getFQN().toString(), lib);
		}
		myExampleLocation = path;
		calculateFQN();
	}

	private void calculateFQN() {
		if (myLibs.size() == 0) {
			myFQN = Path.fromPortableString(EXAMPLES_FOLDER);
			IPath exampleFolder = ConfigurationPreferences.getInstallationPathExamples();
			if (exampleFolder.isPrefixOf(myExampleLocation)) {
				myFQN = myFQN.append(myExampleLocation.makeRelativeTo(exampleFolder));
			} else {
				myFQN = myFQN.append(myExampleLocation);
			}
		} else {
			for (IArduinoLibraryVersion myLib : myLibs.values()) {
				myFQN = myLib.getFQN().append(  myExampleLocation.makeRelativeTo(myLib.getExamplePath()));
			}
		}
	}

	@Override
	public Collection<IArduinoLibraryVersion> getArduinoLibraries() {
		return myLibs.values();
	}

	@Override
	public IPath getCodeLocation() {
		return myExampleLocation;
	}

	@Override
	public String getName() {
		return myExampleLocation.lastSegment();
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
