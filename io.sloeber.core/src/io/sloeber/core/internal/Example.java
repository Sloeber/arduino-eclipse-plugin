package io.sloeber.core.internal;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import static io.sloeber.core.api.Const.*;

import java.util.ArrayList;
import java.util.Arrays;
import io.sloeber.core.api.IArduinoLibraryVersion;
import io.sloeber.core.api.IExample;

public class Example implements IExample {
	private IArduinoLibraryVersion myLib;
	private IPath myExampleLocation;

	@Override
	public String toSaveString() {
		return String.join(SEMI_COLON, getBreadCrumbs());
	}


	public Example(IArduinoLibraryVersion lib, Path path) {
		myLib = lib;
		myExampleLocation = path;
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
		return myExampleLocation.lastSegment();
	}

	@Override
	public String getID() {
		if (myLib == null) {
			return EXAMPLES_FOLDER + COLON + getName();
		}
		return myLib.getName() + COLON + getName();
	}

	@Override
	public String[] getBreadCrumbs() {
		ArrayList<String> ret = new ArrayList<>();
		if (myLib == null) {
			ret.add(EXAMPLES_FOLDER);
			//remmove arduinoplugin/examples
			IPath filteredPath=myExampleLocation.removeFirstSegments(2);
			ret.addAll(Arrays.asList( filteredPath.segments()));
		} else {
			ret.addAll(  Arrays.asList( myLib.getBreadCrumbs()));
			ret.add(getName());
		}

		return ret.toArray(new String[ret.size()]);
	}

}
