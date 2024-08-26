package io.sloeber.arduinoFramework.api;

import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IPath;

import io.sloeber.arduinoFramework.internal.ArduinoPlatformTool;
import io.sloeber.arduinoFramework.internal.ArduinoPlatformToolVersion;
import io.sloeber.core.api.VersionNumber;

public interface IArduinoPackage extends Comparable<IArduinoPackage>{

	IArduinoPlatformPackageIndex getPackageIndex();

	String getMaintainer();

	String getWebsiteURL();

	String getEmail();

	String getHelp();

	Collection<IArduinoPlatform> getPlatforms();

	/**
	 * This method looks up the installed platforms So if you have 2 arduino avr
	 * platform versions installed you will get back 2.
	 *
	 * @return all the installed platforms
	 */
	List<IArduinoPlatformVersion> getInstalledPlatforms();

	/**
	 * get tyhe platform based on the platform ID
	 * The platform ID is the architecture
	 *
	 * @param platformID
	 * @return return the platfiorm or null if not found
	 */
	IArduinoPlatform getPlatform(String platformID);

	Collection<ArduinoPlatformTool> getTools();

	ArduinoPlatformToolVersion getTool(String toolName, VersionNumber version);

	ArduinoPlatformTool getTool(String toolName);

	ArduinoPlatformToolVersion getNewestInstalled(String toolName);

	void onlyKeepLatestPlatforms();

	boolean isInstalled();

	/**
	 * Is any version of the platform installed
	 *
	 * @param platformName
	 * @return if a platform with this name is installed
	 */
	boolean isAVersionOfThisPlatformInstalled(String platformName);

	String getID();

	IPath getInstallPath();

	String getName();

}