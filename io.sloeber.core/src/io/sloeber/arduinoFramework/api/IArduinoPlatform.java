package io.sloeber.arduinoFramework.api;

import java.util.Collection;

import org.eclipse.core.runtime.IPath;

import io.sloeber.core.api.VersionNumber;

public interface IArduinoPlatform extends Comparable<IArduinoPlatform>{

	boolean isInstalled();

	IPath getInstallPath();

	/**
	 * Get the newest version of this platform
	 *
	 * @return the newest version of this platform
	 */
	IArduinoPlatformVersion getNewestVersion();

	Collection<IArduinoPlatformVersion> getVersions();

	IArduinoPlatformVersion getVersion(VersionNumber refVersion);

	/**
	 * return the installed version with the newest version number
	 * Null if no version is installed
	 *
	 * @return
	 */
	IArduinoPlatformVersion getNewestInstalled();

	String getName();

	String getArchitecture();

	IArduinoPackage getParent();

	String getID();

}