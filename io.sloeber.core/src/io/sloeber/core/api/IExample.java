package io.sloeber.core.api;

import java.util.Collection;

import org.eclipse.core.runtime.IPath;

public interface IExample  {
	public Collection<IArduinoLibraryVersion> getArduinoLibraries();
	public IPath getCodeLocation();
	public String getName();
	public String getID();
	public String[] getBreadCrumbs();
	String toSaveString();
}
