package io.sloeber.core.api;

import org.eclipse.core.runtime.IPath;

public interface IExample  {
	public IArduinoLibraryVersion getArduinoLibrary();
	public IPath getCodeLocation();
	public String getName();
	public String getID();
	public String[] getBreadCrumbs();
}
