package io.sloeber.core.api;

import org.eclipse.core.runtime.IPath;

public interface IArduinoLibraryVersion {

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


}
