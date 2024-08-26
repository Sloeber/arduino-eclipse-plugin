package io.sloeber.arduinoFramework.api;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import io.sloeber.arduinoFramework.internal.Node;
import io.sloeber.core.api.VersionNumber;

public interface IArduinoLibrary extends Comparable<IArduinoLibrary> {

	Collection<IArduinoLibraryVersion> getVersions();

	String getAuthor();

	String getMaintainer();

	String getWebsite();

	String getCategory();

	List<String> getArchitectures();

	List<String> getTypes();

	/**
	 * Get the newest version of this library
	 *
	 * @return the newest version of this library
	 */
	IArduinoLibraryVersion getNewestVersion();

	/**
	 * Get the version that is installed
	 * If no version is installed return NULL
	 *
	 * @return
	 */
	IArduinoLibraryVersion getInstalledVersion();

	/**
	 * checks if a version of this library is installed.
	 *
	 * @return true if a version is installed. false in case no version is installed
	 */
	boolean isInstalled();

	//Below are the Node overrides
	String getNodeName();

	Node[] getChildren();

	String getID();

	IPath getInstallPath();

	IArduinoLibraryVersion getVersion(VersionNumber versionNumber);

}