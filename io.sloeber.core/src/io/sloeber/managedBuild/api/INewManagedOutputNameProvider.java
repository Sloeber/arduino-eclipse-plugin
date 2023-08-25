package io.sloeber.managedBuild.api;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

import io.sloeber.schema.api.IConfiguration;
import io.sloeber.schema.api.ITool;

public interface INewManagedOutputNameProvider {
    public IPath getOutputName(IProject project, IConfiguration cConf, ITool tool, IPath inputName);
}
