package io.sloeber.arduinoFramework.api;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import io.sloeber.core.api.VersionNumber;

public abstract class IArduinoLibrary  extends Node implements  Comparable<IArduinoLibrary>{

	public abstract Collection<IArduinoLibraryVersion> getVersions();

	public abstract String getAuthor();

	public abstract String getMaintainer();

	public abstract String getWebsite();

	public abstract String getCategory();

	public abstract List<String> getArchitectures();

	public abstract List<String> getTypes();

	/**
	 * Get the newest version of this library
	 *
	 * @return the newest version of this library
	 */
	public abstract IArduinoLibraryVersion getNewestVersion();

	/**
	 * Get the version that is installed
	 * If no version is installed return NULL
	 *
	 * @return
	 */
	public abstract IArduinoLibraryVersion getInstalledVersion();

	/**
	 * checks if a version of this library is installed.
	 *
	 * @return true if a version is installed. false in case no version is installed
	 */
	public abstract boolean isInstalled();


	public abstract IArduinoLibraryVersion getVersion(VersionNumber versionNumber);

	public abstract IPath getInstallPath();

}