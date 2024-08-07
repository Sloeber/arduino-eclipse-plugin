package io.sloeber.core.natures;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import io.sloeber.core.api.Const;

public class SloeberNature implements IProjectNature {
    public static final String NATURE_ID = Const.SLOEBER_NATURE_ID;
    private IProject myProject = null;

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
    	//JABA nothing to do here
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
