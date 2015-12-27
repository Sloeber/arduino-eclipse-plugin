package it.baeyens.arduino.natures;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

public class ArduinoNature implements IProjectNature {
    private IProject myProject = null;

    @Override
    public void configure() throws CoreException {
	// Jaba is not going to write this code
    }

    @Override
    public void deconfigure() throws CoreException {
	// Jaba is not going to write this code
    }

    @Override
    public IProject getProject() {
	return this.myProject;
    }

    @Override
    public void setProject(IProject Project) {
	this.myProject = Project;

    }

}
