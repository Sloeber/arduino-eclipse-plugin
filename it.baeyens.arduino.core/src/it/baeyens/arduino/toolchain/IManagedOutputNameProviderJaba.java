package it.baeyens.arduino.toolchain;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IManagedOutputNameProvider;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

public interface IManagedOutputNameProviderJaba extends IManagedOutputNameProvider {
	public IPath[] getOutputNames(IProject project, IConfiguration cConf, ITool tool, IPath[] primaryInputNames);
}
