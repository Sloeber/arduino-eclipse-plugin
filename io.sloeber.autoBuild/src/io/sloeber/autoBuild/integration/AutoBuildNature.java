package io.sloeber.autoBuild.integration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

public class AutoBuildNature implements IProjectNature {

    private static final String BUILDER_ID = "io.sloeber.autoBuild.AutoMakeBuilder"; //$NON-NLS-1$
    private static final String NATURE_ID = "io.sloeber.autoBuildNature"; //$NON-NLS-1$
    private IProject myProject;

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

    @Override
    public void configure() throws CoreException {
        // Add the builder to the project
        IProjectDescription description = myProject.getDescription();
        ICommand[] commands = description.getBuildSpec();

        // TODO Remove this when the new StandardBuild nature adds the cbuilder
        for (int i = 0; i < commands.length; i++) {
            ICommand command = commands[i];
            if (command.getBuilderName().equals("org.eclipse.cdt.core.cbuilder")) { //$NON-NLS-1$
                // Remove the command
                Vector<ICommand> vec = new Vector<>(Arrays.asList(commands));
                vec.removeElementAt(i);
                vec.trimToSize();
                ICommand[] tempCommands = vec.toArray(new ICommand[commands.length - 1]);
                description.setBuildSpec(tempCommands);
                break;
            }
        }

        commands = description.getBuildSpec();
        boolean found = false;
        // See if the builder is already there
        for (int i = 0; i < commands.length; ++i) {
            if (commands[i].getBuilderName().equals(BUILDER_ID)) {
                found = true;
                break;
            }
        }
        if (!found) {
            //add builder to project
            ICommand command = description.newCommand();
            command.setBuilderName(BUILDER_ID);
            ICommand[] newCommands = new ICommand[commands.length + 1];
            // Add it before other builders.
            System.arraycopy(commands, 0, newCommands, 1, commands.length);
            newCommands[0] = command;
            description.setBuildSpec(newCommands);
            myProject.setDescription(description, null);
        }

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

    //TODO check whether the stuff below can be removed

    /**
     * Utility method for removing a project nature from a project.
     *
     * @param project
     *            the project to remove the nature from
     * @param natureId
     *            the nature id to remove
     * @param monitor
     *            a progress monitor to indicate the duration of the operation, or
     *            <code>null</code> if progress reporting is not required.
     */
    public static void removeNature(IProject project, String natureId, IProgressMonitor monitor) throws CoreException {
        IProjectDescription description = project.getDescription();
        String[] prevNatures = description.getNatureIds();
        List<String> newNatures = new ArrayList<>(Arrays.asList(prevNatures));
        newNatures.remove(natureId);
        description.setNatureIds(newNatures.toArray(new String[newNatures.size()]));
        project.setDescription(description, monitor);
    }

    /**
     * Update the Java command in the build spec (replace existing one if present,
     * add one first if none).
     */
    public static IProjectDescription setBuildSpec(IProjectDescription description, ICommand newCommand) {

        ICommand[] oldCommands = description.getBuildSpec();
        ICommand oldCommand = getBuildSpec(description, newCommand.getBuilderName());
        ICommand[] newCommands;

        if (oldCommand == null) {
            // Add a Java build spec before other builders (1FWJK7I)
            newCommands = new ICommand[oldCommands.length + 1];
            System.arraycopy(oldCommands, 0, newCommands, 1, oldCommands.length);
            newCommands[0] = newCommand;
        } else {
            for (int i = 0, max = oldCommands.length; i < max; i++) {
                if (oldCommands[i].getBuilderName().equals(oldCommand.getBuilderName())) {
                    oldCommands[i] = newCommand;
                    break;
                }
            }
            newCommands = oldCommands;
        }

        // Commit the spec change into the project
        description.setBuildSpec(newCommands);
        return description;
    }

    public static ICommand getBuildSpec(IProjectDescription description, String builderID) {
        ICommand[] commands = description.getBuildSpec();
        for (int i = 0; i < commands.length; ++i) {
            if (commands[i].getBuilderName().equals(builderID)) {
                return commands[i];
            }
        }
        return null;
    }
}
