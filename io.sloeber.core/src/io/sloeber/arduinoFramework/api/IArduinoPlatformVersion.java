package io.sloeber.arduinoFramework.api;

import java.io.File;
import java.util.List;

import io.sloeber.arduinoFramework.internal.ArduinoPlatformTooldDependency;
import io.sloeber.core.api.VersionNumber;

public abstract class IArduinoPlatformVersion extends ArduinoInstallable implements Comparable<IArduinoPlatformVersion>{

	public abstract IArduinoPlatform getParent();

	public abstract  String getArchitecture();

	public abstract  VersionNumber getVersion();

	public abstract  String getCategory();

	public abstract  List<ArduinoPlatformTooldDependency> getToolsDependencies();

	public abstract boolean isInstalled();

	public abstract  File getBoardsFile();

	public abstract  File getPlatformFile();

	public abstract  List<String> getBoardNames();

	public abstract  String getID();

	public abstract  String getConcattenatedBoardNames();


}