package io.sloeber.core.internal;

import org.eclipse.core.runtime.IPath;

import static io.sloeber.core.api.Const.*;

import java.util.ArrayList;
import java.util.Arrays;

import io.sloeber.core.api.IArduinoLibraryVersion;
import io.sloeber.core.api.IExample;

public class Example implements IExample {
	private IArduinoLibraryVersion myLib;
	private IPath myExampleLocation;

	public Example(IArduinoLibraryVersion lib, IPath exampleLocation) {
		myLib = lib;
		myExampleLocation = exampleLocation;
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
			return EXAMPLES_FODER + COLON + getName();
		}
		return myLib.getName() + COLON + getName();
	}

	@Override
	public String[] getBreadCrumbs() {
		ArrayList<String> ret = new ArrayList<>();
		if (myLib == null) {
			ret.add(EXAMPLES_FODER);
		} else {
			ret.addAll(  Arrays.asList( myLib.getBreadCrumbs()));
		}
		ret.add(getName());
		return ret.toArray(new String[ret.size()]);
	}

}
