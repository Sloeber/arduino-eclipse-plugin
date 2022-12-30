package io.sloeber.autoBuild.extensionPoint;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;

import io.sloeber.autoBuild.api.IConfiguration;
import io.sloeber.autoBuild.api.ITool;

public interface IOutputNameProvider {
    public IPath getOutputName(IProject project, IConfiguration cConf, ITool tool, IPath inputName);
}
