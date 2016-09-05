package io.sloeber.core.natures;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import io.sloeber.common.Const;

public class ArduinoNature implements IProjectNature {
    public static final String NATURE_ID = Const.ARDUINO_NATURE_ID;
    private static final String BUILDER_ID = "io.sloeber.core.inoToCpp"; //$NON-NLS-1$

    private IProject myProject = null;

    @Override
    public void configure() throws CoreException {
	IProjectDescription description = this.myProject.getDescription();
	// add builder to project
	ICommand command = description.newCommand();
	ICommand[] commands = description.getBuildSpec();
	command.setBuilderName(BUILDER_ID);
	ICommand[] newCommands = new ICommand[commands.length + 1];

	// Add it before other builders.
	System.arraycopy(commands, 0, newCommands, 1, commands.length);
	newCommands[0] = command;
	description.setBuildSpec(newCommands);

	this.myProject.setDescription(description, new NullProgressMonitor());
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
