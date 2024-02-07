package io.sloeber.core.natures;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import io.sloeber.core.api.Const;

public class ArduinoNature implements IProjectNature {
    public static final String NATURE_ID = Const.SLOEBER_NATURE_ID;
    private static final String BUILDER_ID = "io.sloeber.core.inoToCpp"; //$NON-NLS-1$

    public static void addNature(IProject project, IProgressMonitor inMonitor) throws CoreException {
        IProgressMonitor monitor = inMonitor;
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        IProjectDescription description = project.getDescription();
        String[] prevNatures = description.getNatureIds();
        for (String prevNature : prevNatures) {
            if (NATURE_ID.equals(prevNature))
                return;
        }
        String[] newNatures = new String[prevNatures.length + 1];
        System.arraycopy(prevNatures, 0, newNatures, 0, prevNatures.length);
        newNatures[prevNatures.length] = NATURE_ID;
        description.setNatureIds(newNatures);
        project.setDescription(description, monitor);
        monitor.done();
    }

    private IProject myProject = null;

    @Override
    public void configure() throws CoreException {
        IProjectDescription description = myProject.getDescription();
        // add builder to project
        ICommand command = description.newCommand();
        ICommand[] commands = description.getBuildSpec();
        command.setBuilderName(BUILDER_ID);
        ICommand[] newCommands = new ICommand[commands.length + 1];

        // Add it before other builders.
        System.arraycopy(commands, 0, newCommands, 1, commands.length);
        newCommands[0] = command;
        description.setBuildSpec(newCommands);

        myProject.setDescription(description, new NullProgressMonitor());
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
