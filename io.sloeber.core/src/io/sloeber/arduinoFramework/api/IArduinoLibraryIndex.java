package io.sloeber.arduinoFramework.api;

import java.util.Collection;
import java.util.Map;
import java.util.Set;


public interface IArduinoLibraryIndex extends Comparable<IArduinoLibraryIndex>{

	/**
	 * given a library name provide the library
	 *
	 * @param libraryName
	 * @return the library or null if not found
	 */
	IArduinoLibrary getLibrary(String libraryName);

	/**
	 * get all the latest versions of all the libraries provided that can be
	 * installed but are not yet installed To do so I find all latest libraries and
	 * I remove the once that are installed.
	 *
	 * @return
	 */
	Map<String, IArduinoLibraryVersion> getLatestInstallableLibraries(Set<String> libNames);

	Collection<IArduinoLibrary> getLibraries();

	String getID();

}