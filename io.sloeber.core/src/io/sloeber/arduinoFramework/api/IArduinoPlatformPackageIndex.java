package io.sloeber.arduinoFramework.api;

import java.io.File;
import java.util.List;


public interface IArduinoPlatformPackageIndex {

	List<IArduinoPackage> getPackages();

	IArduinoPackage getPackage(String packageName);

	File getJsonFile();

	boolean isInstalled();

	String getID();

	String getName();

}