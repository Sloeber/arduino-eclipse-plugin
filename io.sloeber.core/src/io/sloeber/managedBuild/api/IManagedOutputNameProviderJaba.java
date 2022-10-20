package io.sloeber.managedBuild.api;

import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

public interface IManagedOutputNameProviderJaba {
    public IPath getOutputName(IProject project, IConfiguration cConf, ITool tool, IPath inputName);
}
