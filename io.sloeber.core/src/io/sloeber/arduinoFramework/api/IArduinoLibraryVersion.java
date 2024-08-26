package io.sloeber.arduinoFramework.api;

import java.util.List;

import org.eclipse.core.runtime.IPath;

import io.sloeber.core.api.VersionNumber;

public interface IArduinoLibraryVersion extends  Comparable<IArduinoLibraryVersion>{

	String getName();

	IPath getInstallPath();

	IPath getExamplePath();

	/**
	 *
	 * @return true if this is a library linked to a platform private or managed by
	 *         boards manager
	 */
	boolean isHardwareLib();

	/**
	 *
	 * @return true if the library is part of the private library or private
	 *         hardware
	 */
	boolean isPrivateLib();

	/**
	 *
	 * @return getFQN().segments()
	 */
	String[] getBreadCrumbs();

	/**
	 * returns a path that identifies this object in a path form
	 *
	 * @return
	 */
	IPath getFQN();

	public boolean equals(IArduinoLibraryVersion other);

	IArduinoLibrary getLibrary();

	VersionNumber getVersion();

	boolean isInstalled();

	List<String> getArchitectures();

	String getParagraph();

	String getSentence();

	String getMaintainer();

	String getAuthor();


}
