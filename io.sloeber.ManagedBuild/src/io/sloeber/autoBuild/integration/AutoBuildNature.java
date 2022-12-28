package io.sloeber.autoBuild.integration;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class AutoBuildNature implements IProjectNature {

    private IProject myProject;

    @Override
    public void configure() throws CoreException {
        // TODO Auto-generated method stub

    }

    @Override
    public void deconfigure() throws CoreException {
        // TODO Auto-generated method stub

    }

    @Override
    public IProject getProject() {
        return myProject;
    }

    @Override
    public void setProject(IProject project) {
        myProject = project;
    }

}
