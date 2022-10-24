package io.sloeber.managedBuild.api;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

public interface INewManagedOutputNameProvider {
    public IPath getOutputName(IProject project, IConfiguration cConf, ITool tool, IPath inputName);
}
